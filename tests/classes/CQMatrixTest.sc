// CQMatrixTest — the routing-matrix funnel with no server (graph = nil): name<->index
// mapping, weight clamping, patch/unpatch/clear/getPatch, asArray/load roundtrip,
// preset data sanity (SPEC §5.5, A2).

CQMatrixTest : UnitTest {
	var matrix;

	setUp {
		CQMatrix.postWarnings = false;
		matrix = CQMatrix(nil);
	}

	tearDown {
		CQMatrix.postWarnings = true;
	}

	test_nameIndexMapping {
		this.assertEquals(CQMatrix.sourceNames.size, 16, "16 sources (SPEC §5.5)");
		this.assertEquals(CQMatrix.destNames.size, 22, "22 destinations (SPEC §5.5)");
		this.assertEquals(CQMatrix.srcIndex(\tri1), 0, "tri1 is source 0");
		this.assertEquals(CQMatrix.srcIndex(\org2), 15, "org2 is the last source");
		this.assertEquals(CQMatrix.destIndex(\coco1_spaf), 0, "coco1_spaf is dest 0");
		this.assertEquals(CQMatrix.destIndex(\deckB_rate), 21, "deckB_rate is the last dest");
		this.assert(CQMatrix.srcIndex(\bogus).isNil, "unknown source has no index");
		this.assert(CQMatrix.destIndex(\bogus).isNil, "unknown dest has no index");
	}

	test_weightClampFuzz {
		var garbage = [0.0 / 0.0, inf, inf.neg, nil, \banana, "noise", [1, 2], (a: 1), $x];
		this.assertEquals(CQMatrix.clampWeight(0.5), 0.5, "in-range passes");
		this.assertEquals(CQMatrix.clampWeight(99), 1.0, "over-range clips to 1.0");
		this.assertEquals(CQMatrix.clampWeight(-99), -1.0, "under-range clips to -1.0");
		garbage.do { |g|
			var v = CQMatrix.clampWeight(g);
			this.assert(
				v.isNumber and: { v.asFloat.isNaN.not } and: { v.inclusivelyBetween(-1.0, 1.0) },
				"garbage % clamps to an in-range number (got %)".format(g, v));
		};
	}

	test_patchUnpatchRoundtrip {
		matrix.patch(\tri1, \coco1_spaf, 0.5);
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.5, "patch stores the weight");
		matrix.unpatch(\tri1, \coco1_spaf);
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.0, "unpatch zeroes the cell");
	}

	test_patchClampsThroughFunnel {
		matrix.patch(\cas3, \coco2_fbVca, 99);
		this.assertEquals(matrix.getPatch(\cas3, \coco2_fbVca), 1.0, "patch clamps over-range");
		matrix.patch(\cas3, \coco2_fbVca, 0.0 / 0.0);
		this.assertEquals(matrix.getPatch(\cas3, \coco2_fbVca), 0.0, "NaN weight -> 0.0");
	}

	test_unknownPatchSafe {
		this.assertNoException({ matrix.patch(\bogus, \coco1_spaf, 1) },
			"unknown source never throws");
		this.assertNoException({ matrix.patch(\tri1, \bogus, 1) },
			"unknown dest never throws");
		this.assertEquals(matrix.getPatch(\bogus, \coco1_spaf), 0.0,
			"unknown patch reads back as 0.0");
	}

	test_clearPatch {
		matrix.patch(\tri1, \coco1_spaf, 0.7);
		matrix.patch(\cas5, \castle1_fm, -0.4);
		matrix.clearPatch;
		this.assertEquals(matrix.asArray, [], "clearPatch empties every cell");
	}

	test_asArrayLoadRoundtrip {
		var loaded;
		matrix.patch(\tri1, \coco1_spaf, 0.5);
		matrix.patch(\envA, \coco2_fbVca, -0.5);
		loaded = CQMatrix(nil);
		loaded.load(matrix.asArray);
		this.assertEquals(loaded.getPatch(\tri1, \coco1_spaf), 0.5, "load restores cell 1");
		this.assertEquals(loaded.getPatch(\envA, \coco2_fbVca), -0.5, "load restores cell 2");
		this.assertEquals(loaded.asArray.size, 2, "load carries only the patched cells");
	}

	test_presetEmpty {
		matrix.preset(\empty);
		this.assertEquals(matrix.asArray, [], "\\empty ships with no connections");
	}

	test_presetClassic {
		matrix.preset(\classic);
		this.assert(matrix.asArray.size > 0, "\\classic patches something");
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.35,
			"\\classic: tri1 -> coco1_spaf");
		this.assertEquals(matrix.getPatch(\tri1, \coco2_spaf), 0.35,
			"\\classic: tri1 -> coco2_spaf (both Cocos)");
		this.assertEquals(matrix.getPatch(\cas1, \castle2_fm), 0.3,
			"\\classic: castle FM ring segment cas1 -> castle2_fm");
	}

	test_presetClassicParams {
		var params = CQMatrix.presets[\classic][\params];
		this.assert(params[\coco1Spaf] > 0,
			"\\classic raises the spaf depth knob — the tri->spaf banana is dead "
			++ "without it (SPEC §5.3: spaf sum is scaled by cocoNSpaf)");
		this.assert(params[\coco2Spaf] > 0, "\\classic raises coco2's spaf knob too");
	}

	test_presetParamsAreValidSpecNames {
		CQMatrix.presets.keysValuesDo { |name, data|
			data[\params].keysValuesDo { |k, v|
				this.assert(CQSpec.includes(k),
					"preset % param '%' exists in CQSpec".format(name, k));
				this.assertEquals(CQSpec.clamp(k, v), v,
					"preset % param % = % survives its own clamp".format(name, k, v));
			};
		};
	}

	test_presetMayhemDense {
		matrix.preset(\mayhem);
		this.assertEquals(matrix.asArray.size, 16 * 22, "\\mayhem patches every cell");
		matrix.asArray.do { |t|
			this.assert(t[2].abs <= 1.0, "\\mayhem weight % stays in range".format(t[2]));
			this.assert(t[2] != 0.0, "\\mayhem weight is never zero");
		};
	}

	test_unknownPresetSafe {
		matrix.patch(\tri1, \coco1_spaf, 0.5);
		this.assertNoException({ matrix.preset(\bogus) }, "unknown preset never throws");
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.5,
			"unknown preset leaves existing patches untouched");
	}

	test_dependantsNotified {
		var got;
		var ctl = SimpleController(matrix);
		ctl.put(\patch, { |obj, what, src, dest, w| got = [src, dest, w] });
		matrix.patch(\tri1, \coco1_spaf, 0.6);
		ctl.remove;
		this.assertEquals(got, [\tri1, \coco1_spaf, 0.6], "dependants hear clamped patches");
	}
}
