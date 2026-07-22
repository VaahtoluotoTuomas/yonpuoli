// GUI_SPEC §8: append-only matrix language learned from CQMatrix notifications.

CQSpellbookTest : UnitTest {
	var matrix, spellbook;

	setUp {
		CQMatrix.postWarnings = false;
		matrix = CQMatrix(nil);
		spellbook = CQSpellbook(matrix);
	}

	tearDown {
		spellbook.free;
		CQMatrix.postWarnings = true;
	}

	test_codePatchAppearsAsCopyableLanguage {
		matrix.patch(\tri1, \coco1_spaf, 0.4);
		this.assertEquals(spellbook.entryCount, 1,
			"code-layer commits reach the notification-stream spellbook");
		this.assertEquals(spellbook.lines.first,
			"CQ.patch(\\tri1, \\coco1_spaf, 0.4)",
			"single-cell entry uses the code layer's own tongue");
	}

	test_transientWritesNeverLogButFinalCommitDiffsFromCommittedState {
		var audition = CQAudition(matrix);
		audition.preview(\tri3, \coco1_spaf, 0.27);
		this.assertEquals(spellbook.entryCount, 0,
			"audition preview is excluded");
		audition.revert;
		this.assertEquals(spellbook.entryCount, 0,
			"audition revert is excluded");
		audition.preview(\tri3, \coco1_spaf, 0.31);
		audition.plant;
		this.assertEquals(spellbook.entryCount, 1,
			"plant logs once even though the audible transient already held its value");
		this.assert(spellbook.lines.first.find("0.31").notNil,
			"plant line records the exact heard weight");
	}

	test_codePatchDuringAnotherCellsAuditionLogsOnlyItsOwnCell {
		var audition = CQAudition(matrix);
		audition.preview(\tri3, \coco1_spaf, 0.27);
		matrix.patch(\envA, \deckA_rate, -0.4);
		this.assertEquals(spellbook.entryCount, 1,
			"concurrent code commit still creates one entry");
		this.assertEquals(spellbook.lastEntry[\diff].size, 1,
			"single-cell commit never sweeps an unrelated transient into its diff");
		this.assertEquals(spellbook.lastEntry[\diff].first[\src], \envA,
			"the code cell, not the audition cell, is remembered");
		audition.revert;
	}

	test_batchCommitIsOneToolBlockWithDiffedCells {
		var dense = matrix.fullMatrix;
		dense[0][0] = 0.5;
		dense[1][1] = -0.25;
		matrix.writeMatrix(dense, origin: \gui, committed: true,
			operation: \harvest);
		this.assertEquals(spellbook.entryCount, 1,
			"one batch notification produces one spellbook operation");
		this.assertEquals(spellbook.lastEntry[\lines].first, "// harvest",
			"batch operation begins with its tool comment");
		this.assertEquals(spellbook.lastEntry[\diff].size, 2,
			"batch block contains only changed cells");
		this.assertEquals(spellbook.lineRecords[1][\operation], \harvest,
			"every visible code row retains its owning batch operation");
		this.assertEquals(spellbook.lineRecords[1][\lineCount], 3,
			"row metadata makes the whole operation boundary visible");
	}

	test_clearPresetAndShakeCarryOperationMetadata {
		matrix.patch(\tri1, \coco1_spaf, 0.4);
		matrix.clearPatch(origin: \code, operation: \clear);
		this.assertEquals(spellbook.lastEntry[\lines].first, "// clear",
			"clear is a named batch operation");
		matrix.preset(\classic, origin: \gui, operation: \preset);
		this.assertEquals(spellbook.lastEntry[\lines].first, "// preset",
			"preset apply is one named batch operation");
		matrix.shake(0.3, 1234, origin: \gui, operation: \shake);
		this.assertEquals(spellbook.lastEntry[\lines].first, "// shake",
			"shake is one named batch operation");
	}

	test_presetExportUsesCommittedImageOnly {
		var audition = CQAudition(matrix), exported;
		matrix.patch(\envA, \deckA_rate, -0.4);
		audition.preview(\tri1, \coco1_spaf, 0.3);
		exported = spellbook.presetDataString;
		this.assert(exported.find("envA").notNil
			and: { exported.find("deckA_rate").notNil },
			"preset export includes committed cells");
		this.assert(exported.find("tri1").isNil,
			"preset export never captures a live audition");
		audition.revert;
	}
}
