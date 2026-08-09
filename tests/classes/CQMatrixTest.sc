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
		this.assertEquals(CQMatrix.destNames.size, 32, "32 destinations (SPEC §5.5)");
		this.assertEquals(CQMatrix.srcIndex(\tri1), 0, "tri1 is source 0");
		this.assertEquals(CQMatrix.srcIndex(\org2), 15, "org2 is the last source");
		this.assertEquals(CQMatrix.destIndex(\coco1_spaf), 0, "coco1_spaf is dest 0");
		this.assertEquals(CQMatrix.destIndex(\coco1_organ), 5,
			"coco 1 organ begins the software-native input family");
		this.assertEquals(CQMatrix.destIndex(\coco1_slotX), 7,
			"coco 1 SlotX follows its SlotQ counterpart");
		this.assertEquals(CQMatrix.destIndex(\coco1_dust), 8,
			"coco 1 Dust is a continuous material destination");
		this.assertEquals(CQMatrix.destIndex(\coco1_record), 9,
			"coco 1 record closes the first destination island");
		this.assertEquals(CQMatrix.destIndex(\coco2_organ), 15,
			"coco 2 mirrors the same independent organ input");
		this.assertEquals(CQMatrix.destIndex(\coco2_slotX), 17,
			"coco 2 mirrors the SlotX destination");
		this.assertEquals(CQMatrix.destIndex(\coco2_dust), 18,
			"coco 2 mirrors the Dust destination");
		this.assertEquals(CQMatrix.destIndex(\coco2_record), 19,
			"coco 2 mirrors the complete software-native family");
		this.assertEquals(CQMatrix.destIndex(\deckB_rate), 31, "deckB_rate is the last dest");
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

	test_releasePresetManifestNamesSixAuthoredStarts {
		this.assertEquals(CQMatrix.releasePresetNames,
			#[classic, selfNoise, quantumDrone, organMode, crossCurrent, dustGarden],
			"release presets keep six authored starts without counting clear or stress tools");
		CQMatrix.releasePresetNames.do { |name|
			this.assert(CQMatrix.presets[name].notNil,
				"release preset % exists".format(name));
		};
		this.assert(CQMatrix.releasePresetNames.includes(\empty).not,
			"the empty clearing utility is not advertised as an authored sound");
		this.assert(CQMatrix.releasePresetNames.includes(\mayhem).not,
			"the deterministic safety stress field is not advertised as an authored sound");
	}

	test_newReleasePresetsFeatureTheAcceptedMatrixMaterials {
		var cross = CQMatrix.presets[\crossCurrent];
		var dust = CQMatrix.presets[\dustGarden];
		#[coco1_slotX, coco2_slotX].do { |dest|
			this.assert(cross[\matrix].any { |triple| triple[1] == dest },
				"crossCurrent authors % into its form".format(dest));
		};
		#[coco1_dust, coco2_dust].do { |dest|
			this.assert(dust[\matrix].any { |triple| triple[1] == dest },
				"dustGarden authors % into its form".format(dest));
		};
		this.assert(cross[\params][\coco1Organ] > 0
			and: { cross[\params][\coco1SlotQ] > 0 },
			"crossCurrent speaks from blank RAM while its held memories begin trading");
		this.assert(dust[\params][\coco1Dust] >= 0.1
			and: { dust[\params][\coco2Dust] >= 0.1 },
			"dustGarden is a self-starting converter ecology");
		this.assert(dust[\params][\coco1DustGrain] < 0.5
			and: { dust[\params][\coco2DustGrain] > 0.5 },
			"dustGarden gives the two shores audibly opposed mist/chip roles");
	}

	// phase-6 presets carry their load-bearing knobs (preset = weights + knobs,
	// the KUUNTELU 4 lesson: a banana into a closed knob is dead data)
	test_presetPhase6Params {
		var sn = CQMatrix.presets[\selfNoise][\params];
		var qd = CQMatrix.presets[\quantumDrone][\params];
		var om = CQMatrix.presets[\organMode][\params];
		this.assertEquals(sn[\coco1Hold], 0,
			"\\selfNoise records (dust cannot accumulate under hold)");
		this.assert(sn[\coco1Fb].inclusivelyBetween(0.75, 0.95),
			"\\selfNoise rests below unity so only held CV plateaus bloom");
		this.assert(sn[\coco2Fb].inclusivelyBetween(0.75, 0.95),
			"\\selfNoise gives the second loop the same clearing room");
		this.assert(sn[\coco1Dust] > 0.1, "\\selfNoise raises the dust knob");
		this.assert(sn[\coco1In] <= 0.5,
			"\\selfNoise lowers the input valve so cas can cut cross-feed fully");
		this.assertEquals(sn[\coco1SlotPre], 0, "\\selfNoise is no-input (pre slot shut)");
		this.assertEquals(sn[\coco1SlotDeck], 0, "\\selfNoise is no-input (deck slot shut)");
		this.assertEquals(sn[\coco1SlotQ], 0, "\\selfNoise shuts inherited qMix audio");
		[\c1Range, \c2Range, \c3Range, \c4Range].do { |name|
			this.assertEquals(sn[name], \infra,
				"\\selfNoise keeps modulation source % slow".format(name));
		};
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
		this.assertEquals(matrix.asArray.size,
			CQMatrix.sourceNames.size * CQMatrix.destNames.size,
			"\\mayhem patches every cell");
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
		this.assertEquals(m1.lastShakePlan, m2.lastShakePlan,
			"same seed reproduces the audible family and its musical preparation");
		m2 = CQMatrix(nil);
		m2.shake(0.4, seed: 4321);
		this.assert(m1.asArray != m2.asArray, "a different seed throws differently");
	}

	test_shakeBoundsAndShape {
		var spesal = matrix.shake(0.4, seed: 99);
		var cells = matrix.asArray;
		this.assert(cells.size > 0, "shake patches something (seed 99)");
		this.assert(cells.size < (CQMatrix.sourceNames.size * CQMatrix.destNames.size),
			"shake stays sparse — it is a banana handful, not \\mayhem");
		cells.do { |t|
			this.assert(t[2].abs <= 0.82,
				"shake weight % stays inside the tuned ecological ceiling".format(t[2]));
			if(CQMatrix.isGateDest(t[1])) {
				this.assert(t[2].abs >= 0.58,
					"gate/trig % clears the +0.5 Schmitt instead of drawing dead ink"
						.format(t[2]));
			};
		};
		this.assertEquals(spesal.size, 5, "shake returns one state per castle");
		spesal.do { |v|
			this.assert(v.isInteger and: { v.inclusivelyBetween(0, 7) },
				"spesal state % is an integer 0..7".format(v));
		};
	}

	test_shakeDecorationWeightsStayInMusicalCountries {
		var saved = thisThread.randData;
		thisThread.randSeed = 404;
		32.do {
			var continuous = CQMatrix.decorationWeight(\coco1_spaf, 0.3);
			var newVca = CQMatrix.decorationWeight(\coco1_organ, 0.3);
			var gate = CQMatrix.decorationWeight(\coco1_record, 0.3);
			this.assert(continuous.abs.inclusivelyBetween(0.135, 0.3),
				"ordinary continuous decoration no longer clusters near zero");
			this.assert(newVca.abs.inclusivelyBetween(0.16, 0.3),
				"Organ/SlotQ/SlotX decoration stays useful but below its 0.45 ceiling");
			this.assert(gate.abs.inclusivelyBetween(0.58, 0.8),
				"default gate decoration has Schmitt headroom without a full-scale slam");
		};
		thisThread.randData = saved;
	}

	test_acceptedSlotXAndDustRowsParticipateInEcologicalShake {
		#[coco1_slotX, coco1_dust, coco2_slotX, coco2_dust].do { |dest|
			this.assert(CQMatrix.isShakeTarget(dest),
				"% is admitted to ecological shake after acceptance".format(dest));
		};
		this.assert(CQMatrix.isShakeTarget(\bogus).not,
			"an unknown destination never becomes a shake target");
		matrix.shake(0.8, seed: 3000, density: 1);
		#[coco1_slotX, coco1_dust, coco2_slotX, coco2_dust].do { |dest|
			this.assertEquals(matrix.asArray.count { |triple| triple[1] == dest },
				CQMatrix.sourceNames.size,
				"a full sow populates every source in accepted row %".format(dest));
		};
	}

	test_shakePlantsOneAudibleEcologyAndOnlyPreparesSafeMusicalControls {
		var families = IdentitySet.new;
		64.do { |i|
			var plan, destinations, activeSources;
			matrix.shake(0.3, seed: 1000 + i,
				context: (coco1Level: 0.95, coco2Level: 0.2));
			plan = matrix.lastShakePlan;
			families.add(plan[\family]);
			this.assert(plan[\skeleton].size.inclusivelyBetween(3, 5),
				"% plants one deliberate 3–5-cable skeleton".format(plan[\family]));
			destinations = plan[\skeleton].collect { |triple| triple[1] };
			this.assert(destinations.any { |dest| dest.asString.beginsWith("coco1") }
				and: { destinations.any { |dest| dest.asString.beginsWith("coco2") } },
				"% makes both Cocos participants".format(plan[\family]));
			this.assert(plan[\params].keys.every { |name|
				CQMatrix.shakeParamNames.includes(name)
			}, "% prepares only the explicit safe musical-control whitelist"
				.format(plan[\family]));
			this.assert(plan[\params][\patchDepth] >= 0.9
				and: { plan[\params][\coco1Level] >= 0.95 }
				and: { plan[\params][\coco2Level] >= 0.90 },
				"the throw preserves a louder existing level and supplies useful headroom");
			activeSources = (1..5).select { |n|
				plan[\params][("c" ++ n ++ "Range").asSymbol] != \infra
			}.collect { |n| [("tri" ++ n).asSymbol, ("cas" ++ n).asSymbol] }
				.flatten;
			this.assertEquals(activeSources.size, 6,
				"three prepared castles supply six immediate skeleton sources");
			plan[\skeleton].do { |triple|
				var weight = triple[2].abs, dest = triple[1].asString;
				this.assert(CQMatrix.srcIndex(triple[0]).notNil
					and: { CQMatrix.destIndex(triple[1]).notNil },
					"every skeleton route is canonical");
				this.assert(activeSources.includes(triple[0]),
					"the skeleton draws only from its prepared balcium/audio castles");
				if(dest.endsWith("_record")) {
					this.assert(weight.inclusivelyBetween(0.68, 0.82),
						"record punches live in their threshold-safe 0.68…0.82 country");
				};
				if(dest.endsWith("_organ")) {
					this.assert(weight.inclusivelyBetween(0.20, 0.38),
						"organ openings stay expressive without pinning the VCA rail");
				};
				if(dest.endsWith("_slotQ")) {
					this.assert(weight.inclusivelyBetween(0.28, 0.42),
						"SlotQ openings are large enough to expose the Quantussy mix");
				};
			};
		};
		this.assertEquals(families.size, 4,
			"the deterministic seed sweep reaches all four contrasting ecologies");
		matrix.shake(0.3, seed: 2);
		this.assert(matrix.lastShakePlan[\skeleton].collect { |triple| triple[1] }
			.includesAll(#[coco1_dust, coco2_dust]),
			"dustGrowth owns both accepted Dust destinations in its audible skeleton");
		matrix.shake(0.3, seed: 4);
		this.assert(matrix.lastShakePlan[\skeleton].collect { |triple| triple[1] }
			.includesAll(#[coco1_slotX, coco2_slotX]),
			"crossfeed owns both accepted SlotX destinations in its audible skeleton");
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

	// SPEC §5.5's `density` (added 2026-07-22 for the night's sowing moon,
	// GUI_SPEC §16.6): how MUCH decoration is sown, while `amount` is its ceiling
	// and patchDepth stays how hard. The default density remains stable, and the
	// empty phase clears the bay — the field
	// is exactly what the moon shows — without touching the spesal states,
	// because a clear is a clear and not a scramble in disguise.
	test_shakeDensitySowsMoreOrLess {
		var sparse = CQMatrix(nil), dense = CQMatrix(nil), fallback = CQMatrix(nil);
		var explicit = CQMatrix(nil), quiet = CQMatrix(nil), spesal;
		var sparseSpesal, denseSpesal;
		sparseSpesal = sparse.shake(0.4, seed: 11, density: 0.02);
		denseSpesal = dense.shake(0.4, seed: 11, density: 0.4);
		this.assert(dense.asArray.size > (sparse.asArray.size * 2),
			"a fuller moon sows a denser patch (% vs % cells)"
				.format(sparse.asArray.size, dense.asArray.size));
		this.assertEquals(sparse.lastShakePlan[\family], dense.lastShakePlan[\family],
			"moon density cannot change a seeded ecology's family");
		this.assertEquals(sparse.lastShakePlan[\skeleton], dense.lastShakePlan[\skeleton],
			"moon density cannot change a seeded ecology's audible skeleton");
		this.assertEquals(sparse.lastShakePlan[\params].asCompileString,
			dense.lastShakePlan[\params].asCompileString,
			"moon density cannot change a seeded ecology's prepared controls");
		this.assertEquals(sparseSpesal, denseSpesal,
			"moon density adds decoration without changing the seeded spesal posture");
		fallback.shake(0.4, seed: 11);
		explicit.shake(0.4, seed: 11, density: CQMatrix.shakeDensity);
		this.assertEquals(fallback.asArray, explicit.asArray,
			"an omitted density is exactly the historical ~10 % handful");
		// the empty phase: an empty field, i.e. a clear
		quiet.patch(\tri1, \coco1_spaf, 0.55);
		quiet.patch(\envB, \castle3_fm, -0.4);
		spesal = quiet.shake(0.4, seed: 11, density: 0);
		this.assertEquals(quiet.asArray, [],
			"density 0 sows nothing, so it leaves nothing standing — it clears");
		this.assertEquals(spesal, [],
			"but it scrambles no spesal state: a clear is a clear");
		this.assert(quiet.lastShakePlan.isNil,
			"an empty moon prepares no musical controls");
	}

	test_documentedShakeSeedsKeepTheirFamilies {
		[[3, \organDrone], [1, \memoryPulse], [2, \dustGrowth],
			[4, \crossfeed]].do { |pair|
			this.assertEquals(CQMatrix.shakeFamilyForSeed(pair[0]), pair[1],
				"PLAY-8 seed % remains % at every density".format(pair[0], pair[1]));
		};
	}

	test_shakeDensityClampFuzz {
		var wild = CQMatrix(nil);
		wild.shake(0.4, seed: 3, density: 9);
		this.assert(wild.asArray.size >= 280 and: {
			wild.asArray.size < (CQMatrix.sourceNames.size * CQMatrix.destNames.size)
		},
			"a density over 1 fills continuous colour but keeps gate rows ecological");
		wild = CQMatrix(nil);
		wild.patch(\tri1, \coco1_spaf, 0.5);
		wild.shake(0.4, seed: 3, density: -2);
		this.assertEquals(wild.asArray, [],
			"a negative density clamps to the empty field, never to raw values");
		wild = CQMatrix(nil);
		wild.shake(0.4, seed: 3, density: 0.0 / 0.0);
		this.assert(wild.asArray.size > 0,
			"a NaN density falls back to the documented default, never raw");
		this.assertEquals(CQMatrix.clampDensity(nil), CQMatrix.shakeDensity,
			"no density asked for = the historical throw");
		this.assertEquals(CQMatrix.clampDensity(\bogus), CQMatrix.shakeDensity,
			"a non-number density warns and falls back");
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
