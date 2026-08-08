// Startup Audio profile recovery is pure here: no server, Qt, MIDI or hardware.

CQAudioBootRecoveryTest : UnitTest {
	var base, candidate, profilePath;

	setUp {
		CQConfig.postWarnings = false;
		base = CQConfig.validate((
			headless: true,
			sixteenN: (enabled: false),
			device: [
				"Windows WASAPI : Microphone Array",
				"Windows WASAPI : Headphones"
			],
			outChans: [0, 1]
		));
		candidate = CQAudioConfig.merge(base, (
			device: "ASIO : K-MixUSBAudio ASIO Driver"
		));
		profilePath = "memory-config.audio.scd";
	}

	tearDown { CQConfig.postWarnings = true }

	prFixture { |promoteFailure = false, forceFallback = false|
		// Paths are value-equal Strings produced by several independent `++`
		// calls. Dictionary is intentional: IdentityDictionary would miss a
		// textually identical .bak key and turn the assertion into nil.interpret.
		var files = Dictionary.new, failPromote = promoteFailure;
		var adapter, store, recovery;
		files[profilePath] = CQAudioConfig.profileFor(candidate).asCompileString;
		adapter = (
			exists: { |path| files.includesKey(path.asString) },
			write: { |path, text| files[path.asString] = text },
			read: { |path| files[path.asString].interpret },
			remove: { |path| files.removeAt(path.asString) },
			promote: { |pending, profile, backup|
				if(failPromote) { Error("injected promote failure").throw };
				if(files.includesKey(profile.asString)) {
					files[backup.asString] = files[profile.asString]
				};
				files[profile.asString] = files[pending.asString];
				files.removeAt(pending.asString);
			}
		);
		store = CQAudioProfileStore(profilePath, adapter);
		recovery = CQAudioBootRecovery(base, candidate, profilePath, store,
			forceFallback);
		^(
			files: files,
			store: store,
			recovery: recovery,
			allowPromote: { failPromote = false }
		)
	}

	test_freshCatalogSkipsAnAbsentSavedDevice {
		var f = this.prFixture;
		var selected = f[\recovery].selectInitial((
			inputs: ["Windows WASAPI : Microphone Array"],
			outputs: ["Windows WASAPI : Headphones"]
		), \scanned);
		this.assertEquals(selected[\device], base[\device],
			"a positively absent saved K-Mix route yields to the reported base pair");
		this.assertEquals(f[\recovery].state, \rollback,
			"the skipped profile enters the one fallback attempt");
		this.assertEquals(f[\recovery].attemptCount, 1,
			"catalog rejection never pretends the bad server was launched");
		this.assert(f[\recovery].candidateError.find("not currently reported").notNil,
			"the reason names availability rather than an invented boot failure");
	}

	test_enumerableDriverStillFallsBackAtTheClockGateOnly {
		var f = this.prFixture;
		var selected = f[\recovery].selectInitial((
			inputs: ["Windows WASAPI : Microphone Array",
				"ASIO : K-MixUSBAudio ASIO Driver"],
			outputs: ["Windows WASAPI : Headphones",
				"ASIO : K-MixUSBAudio ASIO Driver"]
		), \scanned);
		this.assertEquals(selected[\device], candidate[\device],
			"an installed ASIO driver is allowed to prove its real clock");
		this.assert(f[\recovery].canFallback(\audioClock),
			"the actual-rate gate may repair an enumerable but unplugged driver");
		this.assert(f[\recovery].canFallback(\serverBoot),
			"a device-open failure has the same one fallback");
		this.assert(f[\recovery].canFallback(\graphBuild).not,
			"a SynthDef or graph defect can never be hidden as an audio route issue");
		f[\recovery].beginFallback(
			Error("audio clock unhealthy — requested 48000 Hz, server reports 8000"));
		this.assertEquals(f[\recovery].attemptCount, 2,
			"a launched profile plus base is exactly two attempts");
		this.assert(f[\recovery].canFallback(\audioClock).not,
			"the base attempt cannot recurse into a restart loop");
	}

	test_freshCatalogFailsBeforeServerWhenBothRoutesAreMissing {
		var f = this.prFixture, result;
		f[\recovery].selectInitial((inputs: [], outputs: []), \scanned);
		result = f[\recovery].result;
		this.assert(f[\recovery].preflightFailed,
			"a fresh scan which rejects both routes fails before server launch");
		this.assertEquals(result[\attemptCount], 0,
			"known absence does not expose either endpoint to scsynth");
		this.assert(result[\error].find("saved audio device").notNil
			and: { result[\error].find("base audio device").notNil },
			"the preflight diagnosis names both unavailable routes");
		this.assert(f[\recovery].canFallback(\audioClock).not,
			"known-missing hardware cannot cause a futile reboot");
	}

	test_freshCatalogRejectsOneMissingBaseOnlyRoute {
		var recovery = CQAudioBootRecovery(base, base, profilePath);
		recovery.selectInitial((inputs: [], outputs: []), \scanned);
		this.assert(recovery.preflightFailed,
			"a missing base-only route also fails before server launch");
		this.assertEquals(recovery.attemptCount, 0,
			"base-only preflight does not rely on the driver's silent default");
		this.assert(recovery.error.find("configured audio device").notNil,
			"base-only failure identifies the configured endpoint");
	}

	test_preselectedBaseFailureRetainsBothReasons {
		var f = this.prFixture, failed;
		f[\recovery].selectInitial((
			inputs: ["Windows WASAPI : Microphone Array"],
			outputs: ["Windows WASAPI : Headphones"]
		), \scanned);
		failed = f[\recovery].fallbackFailed(
			Error("base graph failed after device preselection"));
		this.assertEquals(failed[\status], \failed,
			"a preselected fallback has one terminal failure state");
		this.assert(failed[\error].find("not currently reported").notNil
			and: { failed[\error].find("base graph failed").notNil },
			"catalog rejection and fallback failure remain in one diagnostic");
	}

	test_readyTemporaryBaseKeepsThePreferredProfileUntouched {
		var f = this.prFixture, saved;
		f[\recovery].beginFallback("injected unhealthy clock");
		this.assertEquals(f[\recovery].complete[\status], \fallbackReady,
			"a fully ready base route is an explicit temporary fallback");
		saved = f[\files][profilePath].interpret;
		this.assertEquals(saved[\device], candidate[\device],
			"temporary absence never overwrites the explicit preferred route");
		this.assert(f[\files].includesKey(profilePath ++ ".bak").not,
			"a session-only fallback creates no misleading preference backup");
		this.assert(f[\store].hasPending.not,
			"a session-only fallback writes no pending startup state");
		this.assert(f[\recovery].result[\preferredRetained],
			"the result tells the facade that the preferred route remains valid");
	}

	test_readyBaseRepairsOnlyAForcedMalformedProfile {
		var f = this.prFixture(false, true), saved, backup;
		f[\recovery].beginFallback("injected malformed profile", false);
		this.assertEquals(f[\recovery].complete[\status], \rollbackReady,
			"a malformed profile is repaired only after base reaches ready");
		saved = f[\files][profilePath].interpret;
		backup = f[\files][profilePath ++ ".bak"].interpret;
		this.assertEquals(saved[\device], base[\device],
			"the repaired generated profile receives the working base route");
		this.assertEquals(backup[\device], candidate[\device],
			"the rejected generated data remains in the ordinary backup");
		this.assert(f[\recovery].result[\preferredRetained].not,
			"corrupt data is never advertised as a retained preference");
	}

	test_saveFailureKeepsRunningFallbackRetryable {
		var f = this.prFixture(true, true);
		f[\recovery].beginFallback("injected unhealthy clock");
		this.assertEquals(f[\recovery].complete[\status], \notSaved,
			"persistence failure does not misreport the running base as failed audio");
		this.assert(f[\store].hasPending,
			"the validated recovered profile survives for retry");
		f[\allowPromote].value;
		this.assert(f[\recovery].retrySave,
			"retry saves without another audio restart");
		this.assertEquals(f[\recovery].state, \rollbackReady,
			"successful retry returns to recovered-ready");
	}
}
