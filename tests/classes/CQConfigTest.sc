// CQConfigTest — config validation (SPEC §7, A2): bad configs hard-fail with a
// readable message, unknown keys are skipped, defaults fill in.

CQConfigTest : UnitTest {
	var repoRoot;

	setUp {
		CQConfig.postWarnings = false;
		// this file lives at <repo>/tests/classes/CQConfigTest.sc
		repoRoot = this.class.filenameSymbol.asString.dirname.dirname.dirname;
	}

	tearDown { CQConfig.postWarnings = true }

	test_defaultsValidate {
		var cfg = CQConfig.validate(());
		this.assertEquals(cfg[\sampleRate], 48000, "default sampleRate");
		this.assertEquals(cfg[\outChans], [0, 1], "default outChans");
		this.assertEquals(cfg[\bufFrames], 131072, "default bufFrames");
		this.assertEquals(cfg[\sixteenN][\ccBase], 32, "default 16n ccBase");
		this.assert(cfg[\device].isNil, "device defaults to nil");
		this.assert(cfg[\micChan].isNil, "micChan defaults to nil (mic disabled)");
		this.assert(cfg[\stemOuts].isNil, "stemOuts defaults to nil");
	}

	test_exampleConfigLoads {
		var cfg = CQConfig.load(repoRoot +/+ "config.example.scd");
		this.assertEquals(cfg[\bufFrames], 131072, "config.example.scd validates");
		this.assertEquals(cfg[\sixteenN][\takeover], \jump, "example 16n takeover");
	}

	test_partialConfigMerges {
		var cfg = CQConfig.validate((sixteenN: (enabled: false)));
		this.assertEquals(cfg[\sixteenN][\enabled], false, "user subkey applied");
		this.assertEquals(cfg[\sixteenN][\ccBase], 32, "missing subkeys filled from defaults");
	}

	test_badValuesRejected {
		[
			(sampleRate: 12345),
			(blockSize: 63),
			(outChans: [0]),
			(outChans: "stereo"),
			(bufFrames: 1024),
			(bufFrames: 1048576),
			(micChan: -1),
			(sixteenN: (takeover: \teleport)),
			(autoTrim: 3),
			(folders: (samples: 42)),
			(device: 7)
		].do { |bad|
			this.assertException({ CQConfig.validate(bad) }, Error,
				"rejects %".format(bad));
		};
	}

	test_notAnEventRejected {
		this.assertException({ CQConfig.validate([1, 2, 3]) }, Error,
			"rejects a non-Event config");
		this.assertException({ CQConfig.load(repoRoot +/+ "no_such_config.scd") }, Error,
			"rejects a missing file");
	}

	test_unknownKeysSkipped {
		var cfg = CQConfig.validate((megaBass: true));
		this.assert(cfg.includesKey(\megaBass).not, "unknown key skipped, not applied");
	}

	test_nilableKeysAccepted {
		var cfg = CQConfig.validate((device: "ASIO : Fireface", micChan: 2, stemOuts: [2, 3]));
		this.assertEquals(cfg[\device], "ASIO : Fireface", "device string accepted");
		this.assertEquals(cfg[\micChan], 2, "micChan accepted");
		this.assertEquals(cfg[\stemOuts], [2, 3], "stemOuts accepted");
	}
}
