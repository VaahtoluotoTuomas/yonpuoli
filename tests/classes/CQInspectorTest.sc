// GUI_SPEC §8: Inspector touch-and-listen, fine edit and shared undo behavior.

CQInspectorTest : UnitTest {
	var matrix, audition, grammar, inspector;

	setUp {
		CQMatrix.postWarnings = false;
		matrix = CQMatrix(nil);
		audition = CQAudition(matrix);
		grammar = CQBananaGrammar(matrix, audition);
		inspector = CQInspector(matrix, audition, grammar);
	}

	tearDown {
		inspector.free;
		grammar.free;
		CQMatrix.postWarnings = true;
	}

	test_pressAuditionsByMouseSignAndLeftReleaseReverts {
		inspector.press(\tri1, \coco1_spaf, 0, 1, 100);
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.3,
			"left press auditions verso +0.3");
		inspector.release(0);
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.0,
			"left release without a drag reverts the audition");
		this.assertEquals(grammar.undoCount, 0,
			"audition never enters GUI undo");
		inspector.press(\tri1, \coco1_spaf, 1, 1, 100);
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), -0.3,
			"right press auditions inverso -0.3");
		inspector.release(1);
		this.assertEquals(grammar.undoCount, 0,
			"right-clicking an empty cell remains a no-op");
	}

	test_rightClickClearsAndOneUndoRestores {
		matrix.patch(\tri1, \coco1_spaf, 0.6);
		inspector.press(\tri1, \coco1_spaf, 1, 1, 100);
		inspector.release(1);
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.0,
			"right-click clears the occupied precision cell");
		this.assertEquals(grammar.undoCount, 1, "clear is one GUI undo operation");
		grammar.undo;
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.6,
			"Ctrl+Z restores the cleared precision cell");
	}

	test_doubleClickPlantsFixedInspectorWeight {
		inspector.press(\cas2, \castle3_fm, 0, 2, 50);
		this.assertEquals(matrix.getPatch(\cas2, \castle3_fm), 0.3,
			"double-click plants the normative fixed 0.3");
		this.assertEquals(grammar.undoCount, 1,
			"double-click plant is one GUI undo operation");
	}

	test_fineEditFramesAreTransientAndReleaseCommitsOnce {
		var spellbook;
		matrix.patch(\tri2, \coco2_spaf, 0.2);
		spellbook = CQSpellbook(matrix);
		inspector.press(\tri2, \coco2_spaf, 0, 1, 100);
		inspector.drag(80);
		this.assertFloatEquals(matrix.getPatch(\tri2, \coco2_spaf), 0.6,
			"20 px upward fine edit raises weight by 0.4");
		this.assertEquals(spellbook.entryCount, 0,
			"fine-edit frames stay out of Spellbook");
		inspector.release(0);
		this.assertEquals(spellbook.entryCount, 1,
			"release publishes exactly one committed fine edit");
		this.assertEquals(grammar.undoCount, 1,
			"fine edit joins the shared GUI undo ring once");
		grammar.undo;
		this.assertEquals(matrix.getPatch(\tri2, \coco2_spaf), 0.2,
			"one undo restores the pre-drag precision weight");
		spellbook.free;
	}

	test_cancelRestoresFineEditWithoutCommit {
		matrix.patch(\envB, \deckB_rate, -0.2);
		inspector.press(\envB, \deckB_rate, 0, 1, 50);
		inspector.drag(30);
		inspector.cancel;
		this.assertEquals(matrix.getPatch(\envB, \deckB_rate), -0.2,
			"Esc/lifecycle cancel restores the pre-drag matrix");
		this.assertEquals(grammar.undoCount, 0,
			"cancelled fine edit creates no undo operation");
	}
}
