// CQAudioProfileStore — generated Audio I/O overlay persistence (SPEC §7.1).
//
// The hand-maintained base config is never opened for writing. A candidate is
// written to a sibling .pending file, loaded back as code, constrained to
// CQAudioConfig's nine allowed keys and merged/validated before it can be
// promoted. The final same-directory rename is delegated so tests can inject a
// memory adapter and AIO-3 can keep server lifecycle separate from file I/O.

CQAudioProfileStore {
	var <profilePath, <pendingPath, <backupPath, <adapter;
	var <state = \idle, <lastError, <preparedProfile, <preparedCandidate;

	*new { |path, adapter|
		^super.new.prInit(path, adapter)
	}

	*diskAdapter {
		^(
			exists: { |path| File.exists(path.asString) },
			read: { |path| path.asString.load },
			write: { |path, string|
				File.use(path.asString, "w", { |file| file.write(string) })
			},
			remove: { |path|
				if(File.exists(path.asString)) { File.delete(path.asString) }
			},
			promote: { |pending, profile, backup|
				this.prDiskPromote(pending, profile, backup)
			}
		)
	}

	*loadProfile { |path, adapter|
		var io = adapter ? this.diskAdapter;
		var profile = io[\read].value(path.asString);
		this.prValidateProfileShape(profile);
		^profile
	}

	*profileExists { |path, adapter|
		var io = adapter ? this.diskAdapter;
		^io[\exists].value(path.asString) == true
	}

	prepare { |base, profile|
		var loaded, candidate, text;
		state = \preparing;
		lastError = nil;
		try {
			this.class.prValidateProfileShape(profile);
			candidate = CQAudioConfig.merge(base, profile);
			text = profile.asCompileString ++ Char.nl;
			adapter[\write].value(pendingPath, text);
			loaded = adapter[\read].value(pendingPath);
			this.class.prValidateProfileShape(loaded);
			if(this.class.prSameProfile(profile, loaded).not) {
				Error("CQAudioProfileStore: pending profile changed during roundtrip")
					.throw;
			};
			if(this.class.prSameAudio(candidate,
				CQAudioConfig.merge(base, loaded)).not) {
				Error("CQAudioProfileStore: pending candidate changed during roundtrip")
					.throw;
			};
			preparedProfile = loaded.copy;
			preparedCandidate = candidate.copy;
			state = \prepared;
		} { |error|
			lastError = error.errorString;
			state = \failed;
			preparedProfile = nil;
			preparedCandidate = nil;
			try { adapter[\remove].value(pendingPath) } { };
			error.throw;
		};
		^preparedCandidate.copy
	}

	promote {
		if(state != \prepared and: { state != \notSaved }) {
			Error("CQAudioProfileStore: no validated pending profile to promote")
				.throw;
		};
		lastError = nil;
		try {
			adapter[\promote].value(pendingPath, profilePath, backupPath);
			if(adapter[\exists].value(profilePath) != true) {
				Error("CQAudioProfileStore: promote returned without a profile")
					.throw;
			};
			state = \saved;
		} { |error|
			lastError = error.errorString;
			state = \notSaved;
			error.throw;
		};
		^profilePath
	}

	discardPending {
		try { adapter[\remove].value(pendingPath) } { |error|
			lastError = error.errorString;
			state = \failed;
			error.throw;
		};
		preparedProfile = nil;
		preparedCandidate = nil;
		state = \idle;
		^this
	}

	hasProfile { ^adapter[\exists].value(profilePath) == true }
	hasPending { ^adapter[\exists].value(pendingPath) == true }

	// ---- private ----

	prInit { |path, injected|
		profilePath = path.asString;
		pendingPath = profilePath ++ ".pending";
		backupPath = profilePath ++ ".bak";
		adapter = injected ? this.class.diskAdapter;
		^this
	}

	*prValidateProfileShape { |profile|
		if(profile.isKindOf(Event).not) {
			Error("CQAudioProfileStore: generated profile must be an Event").throw;
		};
		profile.keysValuesDo { |key, value|
			if(CQAudioConfig.allowedKeys.includes(key).not) {
				Error("CQAudioProfileStore: unknown generated key '%'"
					.format(key)).throw;
			};
			if(value == CQAudioConfig.defaultMarker
				and: { CQAudioConfig.nilableKeys.includes(key).not }) {
				Error("CQAudioProfileStore: '%' cannot use the default marker"
					.format(key)).throw;
			};
		};
		^profile
	}

	*prSameProfile { |a, b|
		if(a.keys.asSet != b.keys.asSet) { ^false };
		^a.keys.every { |key|
			a[key].asCompileString == b[key].asCompileString
		}
	}

	*prSameAudio { |a, b|
		^CQAudioConfig.allowedKeys.every { |key|
			a[key].asCompileString == b[key].asCompileString
		}
	}

	// SuperCollider 3.13 exposes no File.rename primitive. Keep the actual final
	// replacement atomic on Windows with File.Replace (same-directory pending
	// and final), and with POSIX mv elsewhere. The old final becomes one .bak.
	*prDiskPromote { |pending, profile, backup|
		var command, code;
		if(thisProcess.platform.name == \windows) {
			command = "powershell.exe -NoProfile -NonInteractive "
				++ "-ExecutionPolicy Bypass -Command \"& { "
				++ "if (Test-Path -LiteralPath "
				++ this.prPowerShellLiteral(backup)
				++ ") { Remove-Item -LiteralPath "
				++ this.prPowerShellLiteral(backup) ++ " -Force }; "
				++ "if (Test-Path -LiteralPath "
				++ this.prPowerShellLiteral(profile) ++ ") { "
				++ "[System.IO.File]::Replace("
				++ this.prPowerShellLiteral(pending) ++ ", "
				++ this.prPowerShellLiteral(profile) ++ ", "
				++ this.prPowerShellLiteral(backup) ++ ", $true) "
				++ "} else { [System.IO.File]::Move("
				++ this.prPowerShellLiteral(pending) ++ ", "
				++ this.prPowerShellLiteral(profile) ++ ") } }\"";
		} {
			command = "if [ -e " ++ this.prShellLiteral(profile)
				++ " ]; then cp -f -- " ++ this.prShellLiteral(profile)
				++ " " ++ this.prShellLiteral(backup)
				++ "; fi; mv -f -- " ++ this.prShellLiteral(pending)
				++ " " ++ this.prShellLiteral(profile);
		};
		code = command.systemCmd;
		if(code != 0) {
			Error("CQAudioProfileStore: atomic promote failed (exit %)"
				.format(code)).throw;
		};
		^profile
	}

	*prPowerShellLiteral { |string|
		^"'" ++ string.asString.replace("'", "''") ++ "'"
	}

	*prShellLiteral { |string|
		^"'" ++ string.asString.replace("'", "'\\''") ++ "'"
	}
}
