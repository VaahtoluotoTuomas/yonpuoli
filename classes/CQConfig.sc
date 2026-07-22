// CQConfig — loads and validates config.scd (SPEC §7). Structural errors hard-fail
// with a readable message; unknown keys warn and are skipped; missing keys get
// defaults. Returns a fresh, validated Event — never the raw user event.

CQConfig {
	classvar <>postWarnings = true;

	*defaults {
		// device, micChan, stemOuts and sixteenN.chan default to nil (nil-valued
		// keys cannot live in an Event, so they are simply absent here).
		// sixteenN.chan nil = accept any MIDI channel: the atovproject 16n sends
		// each fader on its own channel (F1 = ch 1 .. F16 = ch 16), CCs 32-47 —
		// the CC number alone identifies the fader. An integer pins one channel.
		^(
			sampleRate: 48000,
			blockSize: 64,
			hardwareBufferSize: 256,
			inChans: [0, 1],
			outChans: [0, 1],
			bufFrames: 131072,
			sixteenN: (enabled: true, ccBase: 32, takeover: \jump),
			folders: (samples: "samples", recordings: "recordings", presets: "presets"),
			autoTrim: false,    // watchdog: notice always; auto-trim is opt-in (SPEC §5.6)
			recStems: false,
			headless: false
		)
	}

	*knownKeys {
		^#[sampleRate, blockSize, device, hardwareBufferSize, inChans, micChan,
			outChans, stemOuts, bufFrames, sixteenN, folders, autoTrim, recStems,
			headless, gui]
	}

	*load { |path|
		var raw;
		path = path.asString;
		if(File.exists(path).not) {
			Error("CQConfig: config file not found: %".format(path)).throw;
		};
		raw = path.load;
		if(raw.isKindOf(Event).not) {
			Error("CQConfig: % must return an Event, i.e. ( key: value, ... )"
				.format(path)).throw;
		};
		^this.validate(raw)
	}

	*validate { |event|
		var cfg = this.defaults;
		if(event.isKindOf(Event).not) {
			Error("CQConfig: expected an Event, got %".format(event.class)).throw;
		};
		event.keysValuesDo { |key, val|
			case
			{ #[sixteenN, folders].includes(key) } {
				this.prMergeSub(cfg, key, val);
			}
			{ this.knownKeys.includes(key) } {
				cfg[key] = val;
			}
			{
				this.prWarn("unknown key '%' skipped".format(key));
			};
		};
		this.prCheck(cfg);
		^cfg
	}

	// ---- private ----

	*prMergeSub { |cfg, key, val|
		var allowed = switch(key,
			\sixteenN, #[enabled, ccBase, chan, takeover],
			\folders, #[samples, recordings, presets],
			// GUI_SPEC §10's optional gui: (...) sub-event. compassDir redirects
			// the §7.1 compass memories file — the smoke tests point it at a tmp
			// dir so a test stamp can never overwrite the player's own corners.
			\gui, #[compassDir]
		);
		if(val.isKindOf(Event).not) {
			Error("CQConfig: % must be an Event (got %)".format(key, val.class)).throw;
		};
		cfg[key] = cfg[key] ?? { () };   // gui has no default sub-event
		val.keysValuesDo { |k2, v2|
			if(allowed.includes(k2)) {
				cfg[key][k2] = v2;
			} {
				this.prWarn("unknown key '%.%' skipped".format(key, k2));
			};
		};
	}

	*prCheck { |cfg|
		var six, fol, overlaps;
		this.prAssert(#[44100, 48000, 96000].includes(cfg[\sampleRate]),
			"sampleRate must be 44100, 48000 or 96000 (got %)".format(cfg[\sampleRate]));
		this.prAssert(#[16, 32, 64, 128, 256, 512].includes(cfg[\blockSize]),
			"blockSize must be a power of two 16..512 (got %)".format(cfg[\blockSize]));
		this.prAssert(cfg[\device].isNil or: { cfg[\device].isString } or: {
				cfg[\device].isKindOf(SequenceableCollection)
				and: { cfg[\device].size == 2 }
				and: { cfg[\device].every { |d| d.isNil or: { d.isString } } } },
			"device must be nil, a String like \"ASIO : ...\", or [inDevice, outDevice] "
			++ "Strings (got %)".format(cfg[\device]));
		this.prAssert(cfg[\hardwareBufferSize].isNil or: {
				cfg[\hardwareBufferSize].isInteger
				and: { cfg[\hardwareBufferSize].inclusivelyBetween(16, 8192) } },
			"hardwareBufferSize must be nil or an integer 16..8192 (got %)"
				.format(cfg[\hardwareBufferSize]));
		this.prCheckChans(cfg[\inChans], \inChans, 1, 8);
		this.prCheckChans(cfg[\outChans], \outChans, 2, 2);
		this.prCheckUniqueChans(cfg[\outChans], \outChans);
		this.prAssert(cfg[\micChan].isNil or: {
				cfg[\micChan].isInteger and: { cfg[\micChan] >= 0 } },
			"micChan must be nil or a channel index >= 0 (got %)".format(cfg[\micChan]));
		if(cfg[\stemOuts].notNil) {
			this.prCheckChans(cfg[\stemOuts], \stemOuts, 2, 2);
			this.prCheckUniqueChans(cfg[\stemOuts], \stemOuts);
			overlaps = cfg[\outChans].select { |chan| cfg[\stemOuts].includes(chan) };
			this.prAssert(overlaps.isEmpty,
				"outChans and stemOuts must be disjoint; overlapping hardware indices: %"
					.format(overlaps));
		};
		this.prAssert(cfg[\bufFrames].isInteger and: {
				cfg[\bufFrames].inclusivelyBetween(65536, 524288) },
			"bufFrames must be an integer 2^16..2^19 (got %)".format(cfg[\bufFrames]));
		if(cfg[\bufFrames].isInteger and: { cfg[\bufFrames].isPowerOfTwo.not }) {
			this.prWarn("bufFrames % is not a power of two (allowed, but unusual)"
				.format(cfg[\bufFrames]));
		};

		six = cfg[\sixteenN];
		this.prAssert(six.isKindOf(Event), "sixteenN must be an Event");
		this.prAssert(six[\enabled].isKindOf(Boolean),
			"sixteenN.enabled must be true or false (got %)".format(six[\enabled]));
		this.prAssert(six[\ccBase].isInteger and: { six[\ccBase].inclusivelyBetween(0, 112) },
			"sixteenN.ccBase must be an integer 0..112 — 16 consecutive CCs must fit (got %)"
				.format(six[\ccBase]));
		this.prAssert(six[\chan].isNil or: {
				six[\chan].isInteger and: { six[\chan].inclusivelyBetween(0, 15) } },
			"sixteenN.chan must be nil (any channel) or a MIDI channel 0..15 (got %)"
				.format(six[\chan]));
		this.prAssert(#[jump, pickup].includes(six[\takeover]),
			"sixteenN.takeover must be \\jump or \\pickup (got %)".format(six[\takeover]));

		fol = cfg[\folders];
		this.prAssert(fol.isKindOf(Event), "folders must be an Event");
		#[samples, recordings, presets].do { |k|
			this.prAssert(fol[k].isString,
				"folders.% must be a String path (got %)".format(k, fol[k]));
		};

		cfg[\gui] !? { |g|
			this.prAssert(g[\compassDir].isNil or: { g[\compassDir].isString },
				"gui.compassDir must be nil or a String path (got %)"
					.format(g[\compassDir]));
		};

		this.prAssert(cfg[\autoTrim].isKindOf(Boolean),
			"autoTrim must be true or false (got %)".format(cfg[\autoTrim]));
		this.prAssert(cfg[\recStems].isKindOf(Boolean),
			"recStems must be true or false (got %)".format(cfg[\recStems]));
		this.prAssert(cfg[\headless].isKindOf(Boolean),
			"headless must be true or false (got %)".format(cfg[\headless]));
	}

	*prCheckChans { |arr, name, minSize, maxSize|
		this.prAssert(
			arr.isKindOf(SequenceableCollection)
			and: { arr.isString.not }
			and: { arr.size.inclusivelyBetween(minSize, maxSize) }
			and: { arr.every { |c| c.isInteger and: { c >= 0 } } },
			"% must be an array of %..% channel indices >= 0 (got %)"
				.format(name, minSize, maxSize, arr));
	}

	*prCheckUniqueChans { |arr, name|
		var seen = Set.new, duplicates = Set.new;
		arr.do { |chan|
			if(seen.includes(chan)) { duplicates.add(chan) } { seen.add(chan) };
		};
		this.prAssert(duplicates.isEmpty,
			"% must use unique hardware channel indices; duplicates: %"
				.format(name, duplicates.asArray.sort));
	}

	*prAssert { |bool, msg|
		if(bool.not) { Error("CQConfig: " ++ msg).throw };
	}

	*prWarn { |msg|
		if(postWarnings) { ("CQConfig: " ++ msg).warn };
	}
}
