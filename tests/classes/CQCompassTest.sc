// CQCompassTest — the §7.1 weather machine's pure model: the tide, breed,
// grow/wither and the versioned compass.cqcompass.scd persistence. Serverless
// and windowless throughout.

CQCompassTest : UnitTest {

	var tmpDir;

	setUp {
		CQCompass.postWarnings = false;
		CQMatrix.postWarnings = false;
		// tests/tmp/ is gitignored — the CQSnapshotTest home for file fixtures
		tmpDir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "cqcompass_test";
		(tmpDir +/+ "*").pathMatch.do { |p| File.delete(p) };
	}

	tearDown {
		CQCompass.postWarnings = true;
		CQMatrix.postWarnings = true;
		(tmpDir +/+ "*").pathMatch.do { |p| File.delete(p) };
	}

	prDense { |triples|
		var dense = CQMatrix.destNames.collect {
			CQMatrix.sourceNames.collect { 0.0 }
		};
		triples.do { |t|
			dense[CQMatrix.destIndex(t[1])][CQMatrix.srcIndex(t[0])] = t[2];
		};
		^dense
	}

	prCount { |dense|
		^dense.sum { |row| row.count { |w| w != 0.0 } }
	}

	// ---- the tide ----

	test_cornersReproduceExactly {
		var a = this.prDense([[\tri1, \coco1_spaf, 0.35], [\cas3, \castle2_fm, -0.6]]);
		var b = this.prDense([[\envA, \coco2_fbVca, 0.5]]);
		var corners = [a, b, nil, nil];
		// at a pure corner the stamp comes back bit-exact — the pad's promise
		this.assertEquals(CQCompass.blend(corners, 0, 0), a,
			"corner TL reproduces its stamp exactly");
		this.assertEquals(CQCompass.blend(corners, 1, 0), b,
			"corner TR reproduces its stamp exactly");
		this.assertEquals(this.prCount(CQCompass.blend(corners, 0, 1)), 0,
			"an empty corner is the zero matrix");
	}

	test_sharedCellsGlide {
		var a = this.prDense([[\tri1, \coco1_spaf, 0.8]]);
		var b = this.prDense([[\tri1, \coco1_spaf, 0.2]]);
		var corners = [a, b, nil, nil];
		var mid = CQCompass.blend(corners, 0.5, 0);
		// a cell both corners carry has presence 1 everywhere on their edge:
		// it never lifts, and its weight crossfades linearly
		this.assertFloatEquals(mid[CQMatrix.destIndex(\coco1_spaf)]
			[CQMatrix.srcIndex(\tri1)], 0.5,
			"a shared cell glides to the pull-weighted average", 0.0001);
	}

	test_disjointCellsKeepFullVoice {
		var a = this.prDense([[\tri1, \coco1_spaf, 0.8]]);
		var corners = [a, nil, nil, nil];
		var di = CQMatrix.destIndex(\coco1_spaf), si = CQMatrix.srcIndex(\tri1);
		var mark = CQCompass.tideMarks[di][si];
		// walking toward empty water, an A-only cable stands at FULL weight
		// while presence (1-x) clears its tide mark, then lifts to exactly 0 —
		// never to a whisper of itself. This is the tide against the mush.
		var standing = CQCompass.blend(corners, (1 - mark) * 0.5, 0);
		var lifted = CQCompass.blend(corners, 1 - (mark * 0.5), 0);
		this.assertFloatEquals(standing[di][si], 0.8,
			"a standing cable keeps its stamped weight", 0.0001);
		this.assertEquals(lifted[di][si], 0.0,
			"past its tide mark the cable is gone, not faded");
	}

	test_witherByTravel {
		var triples = CQMatrix.destNames.collect { |dest, di|
			[CQMatrix.sourceNames[di % 16], dest, 0.4]
		};
		var a = this.prDense(triples);
		var corners = [a, nil, nil, nil];
		var counts = (0, 0.25 .. 1.0).collect { |x|
			this.prCount(CQCompass.blend(corners, x, 0))
		};
		// toward an empty corner the cable count only ever falls — cables drop
		// one by one at their own marks (the documented wither-by-travel)
		counts.doAdjacentPairs { |now, further|
			this.assert(further <= now,
				"the tide only takes cables on the way to empty water");
		};
		this.assertEquals(counts.first, triples.size,
			"the full stamp stands at its own corner");
		this.assertEquals(counts.last, 0, "empty water is empty");
	}

	test_tideMarksAreStableGeography {
		// NOT session-seeded: the same position must grow the same patch after
		// a restart, or nothing learned on the pad is real
		this.assertEquals(CQCompass.tideMarks, CQCompass.tideMarks,
			"tide marks are deterministic");
		this.assert(CQCompass.tideMarks.every { |row|
			row.every { |m| m > 0.0 and: { m < 1.0 } } },
			"no cell is unconditionally eternal or unconditionally drowned");
	}

	test_gatesSurviveTheVoyage {
		// the amplitude blend's failure case: a 0.9 flip banana must still
		// clear the +0.5 Schmitt at the pad's midpoint if it stands at all
		var a = this.prDense([[\cas3, \coco1_flip, 0.9]]);
		var b = this.prDense([[\tri2, \coco2_spaf, 0.3]]);
		var corners = [a, b, nil, nil];
		var di = CQMatrix.destIndex(\coco1_flip), si = CQMatrix.srcIndex(\cas3);
		var mark = CQCompass.tideMarks[di][si];
		var mid = CQCompass.blend(corners, min(0.45, (1 - mark) * 0.9), 0);
		this.assertFloatEquals(mid[di][si], 0.9,
			"a standing gate banana keeps its firing weight mid-voyage", 0.0001);
	}

	// ---- breed / grow / wither ----

	test_breedConstants {
		var a = this.prDense([[\tri1, \coco1_spaf, 0.6], [\cas2, \castle3_fm, -0.4]]);
		var b = this.prDense([[\tri1, \coco1_spaf, -0.2], [\envB, \deckA_rate, 0.7]]);
		var saved = thisThread.randData;
		var child;
		thisThread.randSeed = 4711;
		child = CQCompass.breed(a, b);
		thisThread.randData = saved;
		child.do { |row, di|
			row.do { |w, si|
				var fromA = a[di][si], fromB = b[di][si];
				this.assert(w == 0.0
					or: { (w - fromA).abs <= (CQCompass.breedMutateAmount + 0.0001) }
					or: { (w - fromB).abs <= (CQCompass.breedMutateAmount + 0.0001) },
					"every child cell is a parent's weight within one mutation");
				this.assert(w.abs <= 1.0, "children clamp");
				this.assert(w == 0.0 or: { w.abs >= CQCompass.weightFloor },
					"no ghost hairs below the floor");
			};
		};
	}

	test_growStep {
		var dense = this.prDense([[\tri1, \coco1_spaf, 0.5]]);
		var saved = thisThread.randData;
		var change;
		thisThread.randSeed = 99;
		change = CQCompass.growStep(dense);
		thisThread.randData = saved;
		this.assert(change.notNil, "grow finds an open cell");
		this.assertEquals(dense[change[0]][change[1]], 0.0,
			"grow only plants on open water");
		this.assert(change[2].abs.inclusivelyBetween(0.15, 0.4),
			"grow plants ±U(0.15, 0.4)");
	}

	test_witherStep {
		var dense = this.prDense([[\tri1, \coco1_spaf, 0.5]]);
		var change = CQCompass.witherStep(dense);
		this.assertEquals([change[0], change[1]],
			[CQMatrix.destIndex(\coco1_spaf), CQMatrix.srcIndex(\tri1)],
			"wither picks a patched cell");
		this.assertFloatEquals(change[2], 0.35, "wither decays ×0.7", 0.0001);
		this.assertEquals(
			CQCompass.witherStep(this.prDense([[\tri1, \coco1_spaf, 0.06]]))[2],
			0.0, "wither floors below 0.05 to gone");
		this.assertEquals(CQCompass.witherStep(this.prDense([])), nil,
			"an empty bay has nothing to wither");
	}

	// ---- persistence (the CQSnapshots manners) ----

	test_roundtrip {
		var compass = CQCompass(tmpDir);
		var dense = this.prDense([[\tri1, \coco1_spaf, 0.35],
			[\envA, \castle4_spesal, -0.8]]);
		var reloaded;
		compass.stamp(1, dense);
		this.assert(File.exists(tmpDir +/+ CQCompass.fileName),
			"a stamp auto-saves the memories file");
		reloaded = CQCompass(tmpDir);
		this.assertEquals(reloaded.stampedMask, [false, true, false, false],
			"the stamped mask survives the round trip");
		this.assertEquals(reloaded.corner(1), dense,
			"a corner survives the round trip exactly");
		compass.clear(1);
		this.assertEquals(CQCompass(tmpDir).stampedMask,
			[false, false, false, false], "a clear auto-saves too");
	}

	test_malformedFileRejected {
		var path;
		if(File.exists(tmpDir).not) { File.mkdir(tmpDir) };
		path = tmpDir +/+ CQCompass.fileName;
		File.use(path, "w", { |f| f.write("(version: 99, corners: [nil, nil, nil, nil])") });
		this.assertEquals(CQCompass(tmpDir).stampedMask,
			[false, false, false, false],
			"a newer version is rejected whole, leaving empty water");
		File.use(path, "w", { |f| f.write("[1, 2, 3]") });
		this.assertEquals(CQCompass(tmpDir).stampedMask,
			[false, false, false, false], "a non-Event file is rejected");
		File.use(path, "w", { |f| f.write(
			"(version: 1, corners: [[[\\tri1, \\coco1_spaf, 9.9], "
			++ "[\\bogus, \\coco1_spaf, 0.5], \"junk\"], nil, nil, nil])") });
		this.assertEquals(CQCompass(tmpDir).corner(0)
			[CQMatrix.destIndex(\coco1_spaf)][CQMatrix.srcIndex(\tri1)], 1.0,
			"a wild weight re-enters the clamp on load");
		this.assertEquals(this.prCount(CQCompass(tmpDir).corner(0)), 1,
			"unknown names and junk entries are skipped, the rest survives");
	}

	test_memoryOnlyWithoutDir {
		var compass = CQCompass(nil);
		compass.stamp(0, this.prDense([[\tri1, \coco1_spaf, 0.5]]));
		this.assertEquals(compass.stampedMask, [true, false, false, false],
			"dir = nil stamps in memory");
		this.assertEquals(compass.path, nil, "dir = nil never touches disk");
	}
}
