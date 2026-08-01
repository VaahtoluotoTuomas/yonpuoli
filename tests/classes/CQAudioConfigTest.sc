// AIO-1/AIO-2 — pure Audio I/O config/profile model. No file, Qt, OS device or
// server is touched by this class (persistence has its own tests/tmp fixture).

CQAudioConfigTest : UnitTest {
	var repoRoot;

	setUp {
		CQConfig.postWarnings = false;
		repoRoot = this.class.filenameSymbol.asString.dirname.dirname.dirname;
	}

	tearDown { CQConfig.postWarnings = true }

	test_allowedKeysAreBootConfigNotMusicalState {
		this.assertEquals(CQAudioConfig.allowedKeys,
			#[device, sampleRate, blockSize, hardwareBufferSize, inChans, micChan,
				outChans, stemOuts, recStems],
			"Audio owns exactly the nine startup I/O fields");
		this.assert(CQAudioConfig.allowedKeys.every { |key|
			CQSpec.names.includes(key).not
		}, "no Audio startup field leaks into CQSpec");
	}

	test_profilePathFollowsTheBaseConfig {
		this.assertEquals(
			CQAudioConfig.profilePathFor(repoRoot +/+ "config.scd"),
			repoRoot +/+ "config.audio.scd",
			"the default config gets its sibling profile");
		this.assertEquals(
			CQAudioConfig.profilePathFor(repoRoot +/+ "rig.foo.scd"),
			repoRoot +/+ "rig.foo.audio.scd",
			"a custom base keeps its full stem");
		this.assertEquals(
			CQAudioConfig.profilePathFor(repoRoot +/+ "rig"),
			repoRoot +/+ "rig.audio.scd",
			"an extensionless custom base is still unambiguous");
	}

	test_channelDisplayRoundtripHasOneOwner {
		[0, 1, 7, 63].do { |index|
			this.assertEquals(
				CQAudioConfig.displayToChannel(
					CQAudioConfig.channelToDisplay(index)),
				index,
				"zero-based channel % roundtrips through the one-based page"
					.format(index));
		};
		this.assertEquals(CQAudioConfig.channelToDisplay(0), 1,
			"hardware channel zero is shown as ch 1");
		this.assertException({ CQAudioConfig.channelToDisplay(-1) }, Error,
			"negative config channels are rejected");
		this.assertException({ CQAudioConfig.displayToChannel(0) }, Error,
			"display channel zero does not exist");
	}

	test_nilSingleAndSplitDeviceModesAreExact {
		this.assertEquals(CQAudioConfig.deviceMode(nil), \default,
			"nil means the system default");
		this.assertEquals(CQAudioConfig.deviceMode("ASIO : Lake"), \single,
			"one exact String means one device");
		this.assertEquals(CQAudioConfig.deviceMode(
			["WASAPI : mic", "WASAPI : phones"]), \split,
			"a pair means split input/output");
		this.assert(CQAudioConfig.deviceReported("ASIO : Lake",
			["ASIO : Lake"]), "an exact reported String is present");
		this.assert(CQAudioConfig.deviceReported("Lake",
			["ASIO : Lake"]).not, "device presence never uses substring guessing");
		this.assert(CQAudioConfig.deviceReported(
			[nil, "WASAPI : phones"], ["WASAPI : phones"]),
			"a split side may intentionally use the system default");
	}

	test_deviceCatalogOnlyRemovesExactDuplicates {
		var inputs = [
			"WASAPI : Lake", "WASAPI : Lake", "ASIO : Fjord",
			"WASAPI : Lake (loopback)"
		];
		var outputs = [
			"ASIO : Fjord", "ASIO : Fjord", "WASAPI : Speakers"
		];
		this.assertEquals(CQAudioConfig.uniqueDeviceNames(inputs), [
			"WASAPI : Lake", "ASIO : Fjord", "WASAPI : Lake (loopback)"
		], "exact duplicates collapse while similar endpoints remain distinct");
		this.assertEquals(
			CQAudioConfig.bidirectionalDeviceNames(inputs, outputs),
			["ASIO : Fjord"],
			"single-device choices are exact input/output intersections");
	}

	test_mergePrecedenceIsDefaultsThenBaseThenOverlay {
		var base = (
			sampleRate: 44100,
			blockSize: 32,
			device: "ASIO : base",
			inChans: [2, 3],
			outChans: [0, 1]
		);
		var before = base.asCompileString;
		var merged = CQAudioConfig.merge(base, (
			sampleRate: 96000,
			device: "ASIO : overlay",
			outChans: [4, 5],
			stemOuts: [6, 7]
		));
		this.assertEquals(merged[\sampleRate], 96000,
			"overlay wins over base");
		this.assertEquals(merged[\blockSize], 32,
			"base wins over CQConfig defaults");
		this.assertEquals(merged[\bufFrames], CQConfig.defaults[\bufFrames],
			"ordinary defaults still fill non-Audio fields");
		this.assertEquals(merged[\device], "ASIO : overlay",
			"the exact overlay device survives");
		this.assertEquals(base.asCompileString, before,
			"merge never mutates the base Event");
	}

	test_generatedDefaultMarkerCanReallyClearABaseValue {
		var merged = CQAudioConfig.merge((
			device: "ASIO : base",
			hardwareBufferSize: 512,
			micChan: 2,
			outChans: [0, 1],
			stemOuts: [2, 3]
		), (
			device: \default,
			hardwareBufferSize: \default,
			micChan: \default,
			stemOuts: \default
		));
		this.assert(merged[\device].isNil, "system default clears a base device");
		this.assert(merged[\hardwareBufferSize].isNil,
			"driver default clears CQConfig's ordinary 256-frame default");
		this.assert(merged[\micChan].isNil, "mic off clears a base mic channel");
		this.assert(merged[\stemOuts].isNil, "stems off clears a base stem pair");
	}

	test_profilePreservesDifficultDeviceStringsExactly {
		var name = "ASIO : Öljyjärvi \"main\" \\ loopback";
		var profile = CQAudioConfig.profileFor((
			device: name,
			outChans: [0, 1]
		));
		var merged = CQAudioConfig.merge((), profile);
		this.assertEquals(profile[\device], name,
			"Unicode, quotes and backslash are data, not UI code");
		this.assertEquals(merged[\device], name,
			"the profile merge is byte-for-byte exact at the String level");
	}

	test_unknownOverlayKeysAndIllegalMarkersHardFail {
		this.assertException({
			CQAudioConfig.merge((), (megaBass: true))
		}, Error, "a generated overlay with an unknown key is corrupt");
		this.assertException({
			CQAudioConfig.merge((), (sampleRate: \default))
		}, Error, "a required non-nil field cannot use the private marker");
	}

	test_activeDraftDirtyValidAndDiscardAreSeparate {
		var model = CQAudioConfig((
			device: "ASIO : Lake",
			outChans: [0, 1]
		), repoRoot +/+ "custom.scd");
		this.assert(model.dirty.not and: { model.valid },
			"a fresh draft is the valid active config");
		model.setDraft(\sampleRate, 96000);
		this.assert(model.dirty and: { model.valid },
			"a structural edit becomes a valid dirty draft");
		this.assertEquals(model.activeAt(\sampleRate), 48000,
			"editing never changes active");
		this.assertEquals(model.candidate[\sampleRate], 96000,
			"the valid candidate carries the draft");
		model.setDraft(\outChans, [0, 0]);
		this.assert(model.dirty and: { model.valid.not }
			and: { model.candidate.isNil },
			"duplicate main outputs remain visible as an invalid draft");
		this.assert(model.validationError.find("unique").notNil,
			"the invalid draft keeps CQConfig's exact reason");
		model.discard;
		this.assert(model.dirty.not and: { model.valid }
			and: { model.draftAt(\sampleRate) == 48000 },
			"discard returns only the draft to active");
	}

	test_overlapAndRangeValidationStayInCQConfig {
		var model = CQAudioConfig((outChans: [0, 1]), repoRoot +/+ "config.scd");
		model.setDraft(\stemOuts, [1, 2]);
		this.assert(model.valid.not
			and: { model.validationError.find("disjoint").notNil },
			"main/stem overlap is never softened for the GUI");
		model.setDraft(\stemOuts, nil);
		model.setDraft(\hardwareBufferSize, 9000);
		this.assert(model.valid.not
			and: { model.validationError.find("16..8192").notNil },
			"hardware-buffer range remains CQConfig's law");
	}

	test_staticAudioPageFitsBothReviewedSteps {
		[
			[\compact, 526],
			[\roomy, 630]
		].do { |pair|
			var step = pair[0], height = pair[1];
			var m = CQGuiScale.metrics(step);
			var score = CQGuiAudioSetup.layoutFor(step);
			var used = (2 * m[\vMargin]) + score[\hostHeight]
				+ score[\ioHeight] + score[\restartHeight]
				+ (2 * m[\bandGap]);
			this.assert(used <= height,
				"% Audio composition fits without scrolling (% <= %)"
					.format(step, used, height));
		};
		this.assert(CQGuiAudioSetup.writable,
			"AIO-2 activates draft editing after the visual stop");
	}

	test_longDeviceNamesKeepTheirBeginning {
		var long = "ASIO : Very Long Interface Name With Loopback And Many Channels";
		var windows = "Windows WDM-KS : Headset Hands-Free%0"
			++ Char.ret ++ Char.nl ++ Char.tab ++ ";(Studio - AirPods Pro)";
		var short = CQGuiAudioSetup.ellipsize(long, 150, 12);
		this.assert(short.beginsWith("ASIO"),
			"tail ellipsis preserves the host API/name beginning");
		this.assert(short.endsWith("…"),
			"a clipped device name visibly declares the omission");
		this.assert(CQInkMenu.ellipsize(long, 150, 12).find(Char.nl).isNil,
			"the Ink menu gives Qt one shortened line, never wrapped text");
		this.assertEquals(CQInkMenu.displayLine(windows),
			"Windows WDM-KS : Headset Hands-Free%0 ;(Studio - AirPods Pro)",
			"embedded Windows endpoint whitespace collapses only for display");
		this.assertEquals(CQInkMenu.ellipsize(windows, 4096, 12),
			CQInkMenu.displayLine(windows),
			"even an unclipped endpoint String reaches Pen as exactly one line");
	}
}
