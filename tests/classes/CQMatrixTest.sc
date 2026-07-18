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

	test_presetMatrixEntriesValid {
		CQMatrix.presets.keysValuesDo { |name, data|
			data[\matrix].do { |t|
				this.assert(CQMatrix.srcIndex(t[0]).notNil,
					"preset % source '%' exists".format(name, t[0]));
				this.assert(CQMatrix.destIndex(t[1]).notNil,
					"preset % dest '%' exists".format(name, t[1]));
				this.assert(t[2].abs <= 1.0,
					"preset % weight % in range".format(name, t[2]));
			};
		};
	}

	// phase-6 presets carry their load-bearing knobs (preset = weights + knobs,
	// the KUUNTELU 4 lesson: a banana into a closed knob is dead data)
	test_presetPhase6Params {
		var sn = CQMatrix.presets[\selfNoise][\params];
		var qd = CQMatrix.presets[\quantumDrone][\params];
		var om = CQMatrix.presets[\organMode][\params];
		this.assertEquals(sn[\coco1Hold], 0,
			"\\selfNoise records (dust cannot accumulate under hold)");
		this.assert(sn[\coco1Fb] >= 1.0, "\\selfNoise loops its dust (fb >= unity)");
		this.assert(sn[\coco1Dust] > 0.1, "\\selfNoise raises the dust knob");
		this.assertEquals(sn[\coco1SlotPre], 0, "\\selfNoise is no-input (pre slot shut)");
		this.assertEquals(sn[\coco1SlotDeck], 0, "\\selfNoise is no-input (deck slot shut)");
		this.assertEquals(qd[\c1Range], \audio, "\\quantumDrone lifts castles to audio");
		this.assert(qd[\coco1SlotQ] > 0, "\\quantumDrone opens the qMix slot");
		this.assert(qd[\c1Send] > 0, "\\quantumDrone sends the castles out");
		this.assert(om[\coco1Organ] > 0.5, "\\organMode raises organ 1");
		this.assert(om[\coco2Organ] > 0.5, "\\organMode raises organ 2");
		this.assert(om[\coco1Spaf] > 0,
			"\\organMode opens the spaf knob for the tri3 vibrato banana");
	}

	test_presetMayhemDense {
		matrix.preset(\mayhem);
		this.assertEquals(matrix.asArray.size, 16 * 22, "\\mayhem patches every cell");
		matrix.asArray.do { |t|
			this.assert(t[2].abs <= 1.0, "\\mayhem weight % stays in range".format(t[2]));
			this.assert(t[2] != 0.0, "\\mayhem weight is never zero");
		};
	}

	test_shakeSeedDeterminism {
		var m1 = CQMatrix(nil), m2 = CQMatrix(nil);
		var sp1 = m1.shake(0.4, seed: 1234);
		var sp2 = m2.shake(0.4, seed: 1234);
		this.assertEquals(m1.asArray, m2.asArray,
			"same seed reproduces the same weight throw (SPEC §5.5)");
		this.assertEquals(sp1, sp2, "same seed reproduces the same spesal states");
		m2 = CQMatrix(nil);
		m2.shake(0.4, seed: 4321);
		this.assert(m1.asArray != m2.asArray, "a different seed throws differently");
	}

	test_shakeBoundsAndShape {
		var spesal = matrix.shake(0.4, seed: 99);
		var cells = matrix.asArray;
		this.assert(cells.size > 0, "shake patches something (seed 99)");
		this.assert(cells.size < (16 * 22),
			"shake stays sparse — it is a banana handful, not \\mayhem");
		cells.do { |t|
			this.assert(t[2].abs <= 0.4,
				"shake weight % honors ±amount".format(t[2]));
		};
		this.assertEquals(spesal.size, 5, "shake returns one state per castle");
		spesal.do { |v|
			this.assert(v.isInteger and: { v.inclusivelyBetween(0, 7) },
				"spesal state % is an integer 0..7".format(v));
		};
	}

	test_shakeReplacesOldThrow {
		matrix.patch(\envA, \deckA_rate, 1.0);
		matrix.shake(0.3, seed: 7);
		this.assert(matrix.getPatch(\envA, \deckA_rate).abs <= 0.3,
			"shake clears the previous patch before throwing (old cell is gone "
			++ "or re-thrown within ±amount)");
	}

	test_shakeAmountClampFuzz {
		var spesal;
		matrix.shake(99, seed: 5);
		matrix.asArray.do { |t|
			this.assert(t[2].abs <= 1.0, "amount over 1 clamps to the weight range");
		};
		spesal = matrix.shake(0.0 / 0.0, seed: 5);
		this.assertEquals(matrix.asArray, [],
			"NaN amount degrades to an empty throw, never to raw values");
		this.assertEquals(spesal.size, 5, "spesal states still arrive on a bad amount");
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
