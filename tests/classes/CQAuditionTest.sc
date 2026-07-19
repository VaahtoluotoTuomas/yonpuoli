// GUI_SPEC §4.1 serverless audition state-machine contract. Every cancellation
// route restores exact matrix equality; planting commits exactly the heard weight.

CQAuditionTest : UnitTest {
	var matrix, modelClass;

	setUp {
		CQMatrix.postWarnings = false;
		matrix = CQMatrix(nil);
		matrix.patch(\tri1, \coco1_spaf, 0.25);
		modelClass = \CQAudition.asClass;
	}

	tearDown { CQMatrix.postWarnings = true }

	prModel {
		if(modelClass.isNil) {
			this.assert(false,
				"F0: a serverless CQAudition state machine implements GUI_SPEC §4.1");
			^nil
		};
		^modelClass.new(matrix)
	}

	prAssertRevert { |selector, label|
		var before = matrix.asArray.deepCopy;
		var model = this.prModel;
		if(model.isNil) { ^this };
		model.preview(\tri1, \coco1_spaf, 0.8);
		model.perform(selector);
		this.assertEquals(matrix.asArray, before, label ++ " restores exact matrix state");
		this.assert(model.isActive.not, label ++ " clears audition bookkeeping");
	}

	test_candidateRadiusExitReverts {
		this.prAssertRevert(\leaveRadius, "candidate-radius exit");
	}

	test_candidateSwitchRevertsPreviousFirst {
		var before = matrix.asArray.deepCopy;
		var model = this.prModel;
		if(model.isNil) { ^this };
		model.preview(\tri1, \coco1_spaf, 0.8);
		model.preview(\cas2, \coco2_fbVca, -0.6);
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.25,
			"new candidate reverts the previous candidate first");
		model.releaseVoid;
		this.assertEquals(matrix.asArray, before,
			"cancelling the new candidate restores the entire matrix");
	}

	test_plantCommitsExactlyAudibleWeight {
		var model = this.prModel;
		if(model.isNil) { ^this };
		model.preview(\tri1, \coco1_spaf, 0.73);
		model.plant;
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.73,
			"WYHIWYG plant commits exactly the auditioned weight");
		this.assert(model.isActive.not, "plant clears audition bookkeeping");
	}

	test_escapeReverts { this.prAssertRevert(\escape, "Esc") }

	test_releaseVoidReverts { this.prAssertRevert(\releaseVoid, "release in void") }

	test_viewToggleReverts { this.prAssertRevert(\viewToggle, "Day/Night toggle") }

	test_windowCloseReverts { this.prAssertRevert(\windowClose, "window close") }

	test_guiFreeReverts { this.prAssertRevert(\free, "GUI free") }

	test_cmdPeriodReverts { this.prAssertRevert(\cmdPeriod, "CmdPeriod") }

	test_panicReverts { this.prAssertRevert(\panic, "CQ.panic") }
}
