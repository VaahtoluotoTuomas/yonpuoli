// CQAudioConfig — pure Audio I/O profile model (SPEC §7.1, GUI_SPEC §8).
//
// This class owns the boundary between the currently running config and the
// Audio Drawer's draft. It has no Qt, file or server dependencies. Atomic
// persistence lives in CQAudioProfileStore and the later restart transaction
// stays behind the Cocoquantus facade.

CQAudioConfig {
	var <active, <draft, <candidate;
	var <activeProfile, <draftProfile;
	var <dirty = false, <valid = true, <validationError;
	var <configPath, <profilePath, <profilePresent = false;

	*new { |activeConfig, path, hasProfile = false|
		^super.new.prInit(activeConfig, path, hasProfile)
	}

	// The generated overlay may contain exactly these boot/config keys. They
	// are deliberately outside CQSpec/CQState and every musical snapshot.
	*allowedKeys {
		^#[device, sampleRate, blockSize, hardwareBufferSize, inChans, micChan,
			outChans, stemOuts, recStems]
	}

	// Event cannot retain a key whose value is nil. Generated profiles therefore
	// encode an explicit nil as \default under the same allowed key; the marker
	// is private profile syntax and is decoded before CQConfig/CQGraph see it.
	*nilableKeys { ^#[device, hardwareBufferSize, micChan, stemOuts] }
	*defaultMarker { ^\default }

	*profilePathFor { |path|
		var string = path.asString;
		if(string.endsWith(".scd")) {
			^string.copyRange(0, string.size - 5) ++ ".audio.scd"
		};
		^string ++ ".audio.scd"
	}

	// Config indices remain zero-based. Only this pair of helpers crosses the
	// one-based display boundary used by the Audio page.
	*channelToDisplay { |index|
		if(index.isInteger.not or: { index < 0 }) {
			Error("CQAudioConfig: channel index must be an integer >= 0 (got %)"
				.format(index)).throw;
		};
		^index + 1
	}

	*displayToChannel { |number|
		if(number.isInteger.not or: { number < 1 }) {
			Error("CQAudioConfig: displayed channel must be an integer >= 1 (got %)"
				.format(number)).throw;
		};
		^number - 1
	}

	*deviceMode { |device|
		^case
		{ device.isNil } { \default }
		{ device.isString } { \single }
		{ \split }
	}

	*deviceReported { |device, devices|
		var hasExact;
		devices = devices ? [];
		// Collection.includes compares String identity in sclang. Device names
		// arrive as fresh Strings, so exact textual equality is the truthful
		// test; substring/host-API parsing remains forbidden.
		hasExact = { |name| devices.any { |item| item.asString == name.asString } };
		^case
		{ device.isNil } { true }
		{ device.isString } { hasExact.value(device) }
		{
			device.every { |item|
				item.isNil or: { hasExact.value(item) }
			}
		}
	}

	// ServerOptions may report the same exact endpoint more than once. Preserve
	// driver order while removing only textually identical duplicates; parsing
	// host APIs or fuzzy device names would risk hiding a real route.
	*uniqueDeviceNames { |devices|
		var result = List.new;
		(devices ? []).do { |item|
			var text = item.asString;
			if(result.any { |seen| seen.asString == text }.not) {
				result.add(text)
			};
		};
		^result.asArray
	}

	// A single ServerOptions.device must serve both directions. Split mode uses
	// the two directional lists independently.
	*bidirectionalDeviceNames { |inputs, outputs|
		var ins = this.uniqueDeviceNames(inputs);
		var outs = this.uniqueDeviceNames(outputs);
		^ins.select { |name|
			outs.any { |candidate| candidate.asString == name.asString }
		}
	}

	// A complete generated profile. Nil-valued config truths become the private
	// marker above so the overlay can later override a non-nil base value.
	*profileFor { |config|
		var validated = CQConfig.validate(config), profile = ();
		this.allowedKeys.do { |key|
			profile[key] = this.prEncode(key, validated[key]);
		};
		^profile
	}

	// Merge order is CQConfig.defaults < base < overlay. The base is validated
	// into a fresh Event first and never mutated. Unknown generated keys are a
	// hard error (unlike hand-maintained config.scd, whose unknown keys warn).
	*merge { |base, overlay|
		var raw, validated, resets = List.new;
		if(overlay.isKindOf(Event).not) {
			Error("CQAudioConfig: audio overlay must be an Event").throw;
		};
		overlay.keysValuesDo { |key, value|
			if(this.allowedKeys.includes(key).not) {
				Error("CQAudioConfig: unknown audio overlay key '%'".format(key)).throw;
			};
			if((value == this.defaultMarker)
				and: { this.nilableKeys.includes(key).not }) {
				Error("CQAudioConfig: '%' cannot use the default marker".format(key))
					.throw;
			};
		};
		raw = CQConfig.validate(base).copy;
		overlay.keysValuesDo { |key, value|
			if(value == this.defaultMarker) {
				raw.removeAt(key);
				resets.add(key);
			} {
				raw[key] = value;
			};
		};
		// hardwareBufferSize has a non-nil CQConfig default. Validate the whole
		// candidate once, then remove explicit nils; every reset key is known to
		// accept nil by CQConfig's own contract.
		validated = CQConfig.validate(raw);
		resets.do { |key| validated.removeAt(key) };
		^validated
	}

	activeAt { |key|
		this.prRequireAllowed(key);
		^active[key]
	}

	draftAt { |key|
		this.prRequireAllowed(key);
		^this.class.prDecode(draftProfile[key])
	}

	dirtyAt { |key|
		this.prRequireAllowed(key);
		^draftProfile[key] != activeProfile[key]
	}

	// AIO-2's Qt rows call this; it still changes only the detached profile and
	// candidate, never the active session.
	setDraft { |key, value|
		this.prRequireAllowed(key);
		draftProfile[key] = this.class.prEncode(key, value);
		this.prRefresh;
		^this
	}

	discard {
		draftProfile = activeProfile.copy;
		this.prRefresh;
		^this
	}

	// AIO-3 calls this only after a complete candidate boot reaches \running.
	// The coordinator passes its captured data so a late external draft edit
	// can never be mistaken for the candidate that actually reached running.
	acceptCandidate { |preparedProfile, preparedCandidate|
		preparedProfile = preparedProfile ? draftProfile;
		preparedCandidate = preparedCandidate ? candidate;
		if(preparedCandidate.isNil) {
			Error("CQAudioConfig: cannot accept an invalid candidate").throw;
		};
		active = CQConfig.validate(preparedCandidate).copy;
		activeProfile = preparedProfile.copy;
		draftProfile = preparedProfile.copy;
		this.prRefresh;
		^this
	}

	profilePresent_ { |flag|
		profilePresent = flag == true;
		^this
	}

	// ---- private ----

	prInit { |activeConfig, path, hasProfile|
		configPath = (path ? (Cocoquantus.repoDir +/+ "config.scd")).asString;
		profilePath = this.class.profilePathFor(configPath);
		profilePresent = hasProfile == true;
		active = CQConfig.validate(activeConfig);
		activeProfile = this.class.profileFor(active);
		draftProfile = activeProfile.copy;
		this.prRefresh;
		^this
	}

	prRefresh {
		var nextDraft = ();
		this.class.allowedKeys.do { |key|
			var value = this.class.prDecode(draftProfile[key]);
			if(value.notNil) { nextDraft[key] = value };
		};
		draft = nextDraft;
		dirty = this.class.allowedKeys.any { |key|
			draftProfile[key] != activeProfile[key]
		};
		try {
			candidate = this.class.merge(active, draftProfile);
			valid = true;
			validationError = nil;
		} { |error|
			candidate = nil;
			valid = false;
			validationError = error.errorString;
		};
		^this
	}

	prRequireAllowed { |key|
		if(this.class.allowedKeys.includes(key).not) {
			Error("CQAudioConfig: '%' is not an Audio I/O field".format(key)).throw;
		};
	}

	*prEncode { |key, value|
		if(value.isNil) {
			if(this.nilableKeys.includes(key).not) {
				Error("CQAudioConfig: '%' may not be nil".format(key)).throw;
			};
			^this.defaultMarker
		};
		^if(value.isKindOf(SequenceableCollection) and: { value.isString.not }) {
			value.copy
		} {
			value
		}
	}

	*prDecode { |value|
		^if(value == this.defaultMarker) { nil } {
			if(value.isKindOf(SequenceableCollection) and: { value.isString.not }) {
				value.copy
			} {
				value
			}
		}
	}
}
