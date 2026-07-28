// CQSnapshotTest — file-backed snapshot memory with no server (graph = nil):
// roundtrip equality, malformed-file rejection, clamp-on-load, the micGain
// recall policy and the monitorMute exclusion (SPEC §7 / A2). Files go to
// tests/tmp/ (gitignored), cleaned before and after every test.

CQSnapshotTest : UnitTest {
	var state, matrix, snaps, dir, legacyDir, spesal;

	setUp {
		CQSpec.postWarnings = false;
		CQMatrix.postWarnings = false;
		CQSnapshots.postWarnings = false;
		state = CQState(nil);
		matrix = CQMatrix(nil);
		spesal = [0, 0, 0, 0, 0];
		dir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "cqsnap_test";
		legacyDir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
			+/+ "cqsnap_legacy_test";
		this.prClean;
		snaps = CQSnapshots(state, matrix, dir,
			{ spesal.copy }, { |st| spesal = st.copy }, legacyDir);
	}

	tearDown {
		snaps.free;
		this.prClean;
		CQSpec.postWarnings = true;
		CQMatrix.postWarnings = true;
		CQSnapshots.postWarnings = true;
	}

	prClean {
		(dir +/+ "*").pathMatch.do { |p| File.delete(p) };
		(legacyDir +/+ "*").pathMatch.do { |p| File.delete(p) };
	}

	prWrite { |name, content|
		this.prWriteAt(dir, name, content)
	}

	prWriteAt { |base, name, content|
		if(File.exists(base).not) { File.mkdir(base) };
		File.use(base +/+ (name ++ ".cqsnap.scd"), "w", { |f| f.write(content) });
	}

	test_roundtripEquality {
		state.set(\coco1Fb, 1.02);
		state.set(\qChaos, 0.4);
		state.set(\preASrc, \mic);
		state.set(\coco1Bits, 5);
		state.set(\coco2Hold, 0);
		matrix.patch(\cas3, \coco1_spaf, 0.7);
		matrix.patch(\envA, \coco2_fbVca, -0.5);
		spesal = [1, 2, 3, 4, 5];
		snaps.save(\rt);
		state.set(\coco1Fb, 0.1);
		state.set(\qChaos, 0.9);
		state.set(\preASrc, \deckB);
		state.set(\coco1Bits, 12);
		state.set(\coco2Hold, 1);
		matrix.clearPatch;
		matrix.patch(\tri1, \deckA_rate, 0.2);
		spesal = [7, 7, 7, 7, 7];
		this.assert(snaps.recall(\rt), "recall of a saved snapshot succeeds");
		this.assertFloatEquals(state.get(\coco1Fb), 1.02, "num param restored");
		this.assertFloatEquals(state.get(\qChaos), 0.4, "second num param restored");
		this.assertEquals(state.get(\preASrc), \mic, "enum param restored");
		this.assertEquals(state.get(\coco1Bits), 5, "int param restored");
		this.assertEquals(state.get(\coco2Hold), 0, "toggle param restored");
		this.assertFloatEquals(matrix.getPatch(\cas3, \coco1_spaf), 0.7,
			"matrix cell restored");
		this.assertFloatEquals(matrix.getPatch(\envA, \coco2_fbVca), -0.5,
			"negative (inverso) weight restored");
		this.assertEquals(matrix.getPatch(\tri1, \deckA_rate), 0.0,
			"cells not in the snapshot are cleared");
		this.assertEquals(spesal, [1, 2, 3, 4, 5], "spesal states restored");
	}

	test_micGainNeverAutoRecalled {
		state.set(\micGain, 2);
		snaps.save(\mic0);                     // default save: micGain stored as 0
		state.set(\micGain, 1);
		snaps.recall(\mic0);
		this.assertEquals(state.get(\micGain), 0,
			"auto-saved snapshot recalls micGain as 0 (SPEC §7)");
	}

	test_micGainExplicitSaveRecalls {
		state.set(\micGain, 2);
		snaps.save(\micX, includeMic: true);   // the user's explicit choice
		state.set(\micGain, 0);
		snaps.recall(\micX);
		this.assertFloatEquals(state.get(\micGain), 2.0,
			"includeMic: true stores and recalls the real value");
	}

	test_monitorMuteNeverRecalled {
		snaps.save(\mm);                       // saved while unmuted
		state.set(\monitorMute, 1);            // panic
		snaps.recall(\mm);
		this.assertEquals(state.get(\monitorMute), 1,
			"recall never defeats panic (monitorMute untouched)");
	}

	test_malformedFilesRejected {
		var before = state.get(\coco1Fb);
		this.prWrite("bad_runtime", "nil.thisMethodDoesNotExist");
		this.prWrite("bad_notevent", "[1, 2, 3]");
		this.prWrite("bad_noversion", "(params: (coco1Fb: 0.9))");
		this.prWrite("bad_futureversion", "(version: 99, params: (coco1Fb: 0.9))");
		this.prWrite("bad_noparams", "(version: 1, params: \"nope\")");
		#["bad_runtime", "bad_notevent", "bad_noversion", "bad_futureversion",
			"bad_noparams"].do { |n|
			this.assertNoException({
				this.assertEquals(snaps.recall(n), false,
					"'%' is rejected".format(n));
			}, "'%' never throws".format(n));
		};
		this.assertEquals(state.get(\coco1Fb), before,
			"rejected files leave the state untouched");
	}

	test_missingSnapshotSafe {
		this.assertNoException({
			this.assertEquals(snaps.recall(\doesNotExist), false,
				"missing snapshot recalls as false");
		}, "missing snapshot never throws");
	}

	test_valuesClampedOnLoad {
		this.prWrite("dirty", "(version: 1,"
			++ "params: (coco1Fb: 99, qChaos: \"garbage\", bogusParam: 1),"
			++ "matrix: [[\\tri1, \\coco1_spaf, 42], [\\nope, \\coco1_spaf, 1],"
			++ " [\\tri1], \"junk\"],"
			++ "spesal: [99, -3, 2, 800, 1])");
		this.assert(snaps.recall(\dirty), "structurally valid file loads");
		this.assertFloatEquals(state.get(\coco1Fb), 1.25,
			"over-range param clamps to the CQSpec max");
		this.assertFloatEquals(state.get(\qChaos), CQSpec.default(\qChaos),
			"garbage value falls back to the default");
		this.assertFloatEquals(matrix.getPatch(\tri1, \coco1_spaf), 1.0,
			"over-range weight clamps to 1.0");
		this.assertEquals(spesal, [3, 5, 2, 0, 1],
			"spesal states wrap into 0..7");
	}

	test_sparseParamsLeaveRestUntouched {
		state.set(\qChaos, 0.6);
		this.prWrite("sparse", "(version: 1, params: (coco1Fb: 0.5))");
		snaps.recall(\sparse);
		this.assertFloatEquals(state.get(\coco1Fb), 0.5, "listed param applied");
		this.assertFloatEquals(state.get(\qChaos), 0.6,
			"unlisted params keep their current values");
	}

	test_saveSanitizesName {
		var path = snaps.save("we ird/na:me");
		this.assert(path.notNil and: { File.exists(path) },
			"unsafe characters sanitize into a writable filename");
		this.assert(snaps.recall("we ird/na:me"),
			"recall sanitizes identically and finds the file");
	}

	test_list {
		snaps.save(\alpha);
		snaps.save(\beta);
		this.assertEquals(snaps.list, ["alpha", "beta"], "list returns sorted names");
	}

	test_legacyPresetFolderIsReadOnlyFallback {
		this.prWriteAt(legacyDir, "old_bank",
			"(version: 1, params: (coco1Fb: 0.91), matrix: [])");
		this.assertEquals(snaps.list, ["old_bank"],
			"legacy presets-folder snapshots remain discoverable");
		this.assert(snaps.recall(\old_bank),
			"legacy presets-folder snapshots remain recallable");
		this.assertFloatEquals(state.get(\coco1Fb), 0.91,
			"legacy recall applies the old file");
		state.set(\coco1Fb, 0.44);
		snaps.save(\old_bank);
		this.assert(File.exists(dir +/+ "old_bank.cqsnap.scd"),
			"a same-name save writes only to the new snapshots folder");
		state.set(\coco1Fb, 0.1);
		snaps.recall(\old_bank);
		this.assertFloatEquals(state.get(\coco1Fb), 0.44,
			"the new snapshots folder wins over a same-name legacy file");
	}

	test_morphEndsAtTarget {
		state.set(\coco1Fb, 0.2);
		state.set(\coco1Hold, 0);
		matrix.patch(\tri1, \coco1_spaf, 0.8);
		snaps.save(\ma);
		state.set(\coco1Fb, 1.1);
		state.set(\coco1Hold, 1);
		state.set(\micGain, 3);
		matrix.patch(\tri1, \coco1_spaf, 0.0);
		matrix.patch(\cas2, \castle3_fm, 0.5);
		snaps.save(\mb);
		snaps.recall(\ma);
		snaps.morph(\ma, \mb, 0.2);
		this.assert(snaps.isMorphing, "morph routine is running");
		this.assertEquals(snaps.morphFrom, \ma, "morph publishes its A seed");
		this.assertEquals(snaps.morphTo, \mb, "morph publishes its B seed");
		this.assert(snaps.morphProgress >= 0 and: { snaps.morphProgress < 1 },
			"morph publishes a truthful in-flight progress value");
		0.8.wait;
		this.assert(snaps.isMorphing.not, "morph routine ended");
		this.assertFloatEquals(snaps.morphProgress, 1.0,
			"completed morph publishes arrival at the target");
		this.assertFloatEquals(state.get(\coco1Fb), 1.1,
			"continuous param lands exactly on the target");
		this.assertEquals(state.get(\coco1Hold), 1, "toggle switched to the target");
		this.assertFloatEquals(matrix.getPatch(\cas2, \castle3_fm), 0.5,
			"matrix weight lands on the target");
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.0,
			"weight morphs down to 0");
		this.assertEquals(state.get(\micGain), 0,
			"morph target was auto-saved — mic stays closed");
	}
}
