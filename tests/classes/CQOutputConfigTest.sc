// CQ-AUD-001 reproducer: no accepted hardware-output configuration may alias
// writes after their independent limiter chains.

CQOutputConfigTest : UnitTest {
	setUp { CQConfig.postWarnings = false }

	tearDown { CQConfig.postWarnings = true }

	test_duplicateMainChannelsRejected {
		this.assertException({ CQConfig.validate((outChans: [0, 0])) }, Error,
			"CQ-AUD-001: duplicate main output indices are rejected");
	}

	test_duplicateStemChannelsRejected {
		this.assertException({ CQConfig.validate((stemOuts: [2, 2])) }, Error,
			"CQ-AUD-001: duplicate stem output indices are rejected");
	}

	test_mainStemOverlapRejected {
		[
			(outChans: [0, 1], stemOuts: [1, 2]),
			(outChans: [2, 3], stemOuts: [0, 2]),
			(outChans: [0, 1], stemOuts: [0, 1])
		].do { |bad|
			this.assertException({ CQConfig.validate(bad) }, Error,
				"CQ-AUD-001: main/stem overlap is rejected: %".format(bad));
		};
	}

	test_disjointOutputsAccepted {
		var cfg;
		this.assertNoException({
			cfg = CQConfig.validate((outChans: [0, 1], stemOuts: [2, 3]));
		}, "disjoint main/stem outputs remain valid");
		this.assertEquals(cfg[\stemOuts], [2, 3], "safe output indices are preserved");
	}
}
