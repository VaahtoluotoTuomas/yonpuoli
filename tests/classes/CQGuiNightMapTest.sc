// CQGuiNightMapTest — the serverless, windowless contracts of the Night map
// class: the endpoint look table mirrors the canonical matrix inventory with
// the same families, glyphs and lamps the Day skin teaches (GUI_SPEC §16.7).

CQGuiNightMapTest : UnitTest {

	test_endpointSpecInventory {
		CQMatrix.sourceNames.do { |name|
			var spec = CQGuiNightMap.endpointSpec(name);
			this.assert(spec.notNil and: { spec[\kind] == \jack },
				"% is a source jack".format(name));
		};
		CQMatrix.destNames.do { |name|
			var spec = CQGuiNightMap.endpointSpec(name);
			this.assert(spec.notNil and: { spec[\kind] == \socket },
				"% is a destination socket".format(name));
		};
		this.assert(CQGuiNightMap.endpointSpec(\bogus).isNil,
			"an unknown name has no night home");
	}

	// The struck head wears its trigger rate as weight, never as motion: above
	// ~10 Hz the eye fuses a flicker, and this surface gets 15 Hz, so any
	// blink would be a rate the GUI invented — slower than the truth. The
	// mapping is logarithmic (a rate is a pitch to the ear) and quantized to
	// the steps the drawing can show, so a drifting rate cannot repaint the
	// read-head layer every frame for a difference nobody can see (§10, §16.6).
	test_struckRateReadsAsWeightOnALogScale {
		var steps = CQGuiNightMap.struckSteps;
		this.assertEquals(CQGuiNightMap.struckFrames, 4,
			"four 15 Hz frames preserve the old roughly 0.2 s struck threshold");
		this.assertEquals(CQGuiNightMap.struckNorm(nil), 0,
			"no telemetry weighs nothing — the mark cannot invent a rate");
		this.assertEquals(CQGuiNightMap.struckNorm(8), 0,
			"the bottom of the scale is where a flicker stops being countable");
		this.assertEquals(CQGuiNightMap.struckNorm(2048), 1,
			"the top of the scale is eight octaves up");
		this.assertEquals(CQGuiNightMap.struckNorm(100000), 1,
			"a mad rate clamps instead of growing the mark off the shell");
		this.assertEquals(CQGuiNightMap.struckNorm(0), 0,
			"a zero rate clamps too (log2 of zero is not a size)");
		this.assert(CQGuiNightMap.struckNorm(128) > CQGuiNightMap.struckNorm(32)
			and: { CQGuiNightMap.struckNorm(32) > CQGuiNightMap.struckNorm(16) },
			"the weight rises with the rate");
		this.assertEquals(CQGuiNightMap.struckNorm(128),
			CQGuiNightMap.struckNorm(132),
			"a rate that drifts inside one step never repaints the read-head layer");
		this.assert((CQGuiNightMap.struckNorm(512) * steps).frac < 1e-9,
			"every weight lands on one of the drawing's own steps");
	}

	test_lampsExactlyOnGateTrigs {
		var lamped = CQMatrix.destNames.select { |name|
			CQGuiNightMap.endpointSpec(name)[\lamp] == true
		};
		this.assertEquals(lamped,
			[\coco1_flip, \coco1_skip, \coco1_record,
				\coco2_flip, \coco2_skip, \coco2_record,
				\castle1_spesal, \castle2_spesal, \castle3_spesal,
				\castle4_spesal, \castle5_spesal],
			"charge lamps sit on all eleven gate/trig sockets in /cq_charge order");
	}

	test_familiesMatchDaySkin {
		this.assertEquals(CQGuiNightMap.endpointSpec(\tri3)[\family], \tri,
			"triangle buds keep the tri family");
		this.assertEquals(CQGuiNightMap.endpointSpec(\coco1)[\family], \cas,
			"the spiral's mouth keeps the voice gray");
		this.assertEquals(CQGuiNightMap.endpointSpec(\org2)[\family], \organ,
			"org fruit keeps the organ yellow");
		this.assertEquals(CQGuiNightMap.endpointSpec(\envB)[\family], \input,
			"root tips keep the input green");
		this.assertEquals(CQGuiNightMap.endpointSpec(\coco2_spaf)[\family], \spaf,
			"spaf moons keep the purple ring");
		this.assertEquals(CQGuiNightMap.endpointSpec(\castle4_fm)[\family], \fm,
			"fm moons keep the blue ring");
		this.assertEquals(CQGuiNightMap.endpointSpec(\castle4_fm)[\glyph], \wave,
			"fm moons carry the wave glyph");
		this.assertEquals(CQGuiNightMap.endpointSpec(\deckA_rate)[\glyph], \pendulum,
			"the wing socket carries the pendulum glyph");
		this.assertEquals(CQGuiNightMap.endpointSpec(\coco1_inVca)[\glyph], \wedge,
			"vca moons carry the open wedge");
		this.assertEquals(CQGuiNightMap.endpointSpec(\coco1_organ)[\family], \organ,
			"the independent organ input inherits the calyx's yellow family");
		this.assert(CQGuiNightMap.endpointSpec(\coco1_organ)[\glyph].isNil,
			"organ proximity makes a redundant direction wedge unnecessary");
		this.assertEquals(CQGuiNightMap.endpointSpec(\coco2_slotQ)[\family], \tri,
			"the independent SlotQ input inherits the Quantussy tributary family");
		this.assert(CQGuiNightMap.endpointSpec(\coco2_slotQ)[\glyph].isNil,
			"the Q gill and its adjacent socket need no direction wedge");
		this.assertEquals(CQGuiNightMap.endpointSpec(\coco1_record)[\family], \danger,
			"the Record toggle belongs to the red Hold/REC family, not green input");
		this.assertEquals(CQGuiNightMap.endpointSpec(\coco1_record)[\glyph], \rec,
			"the matrix writer input has an unambiguous record seal glyph");
	}

	test_sourceDecorations {
		this.assertEquals(CQGuiNightMap.endpointSpec(\tri1)[\decoration], \leaf,
			"tri jacks grow the leaf");
		this.assertEquals(CQGuiNightMap.endpointSpec(\cas5)[\decoration], \crown,
			"cas jacks wear the crown teeth");
		this.assert(CQGuiNightMap.endpointSpec(\coco1)[\decoration].isNil,
			"coco jacks stay bare — the chevron is the shell's flip touch");
		this.assertEquals(CQGuiNightMap.endpointSpec(\org1)[\decoration], \stem,
			"org jacks keep their stem tip");
		this.assertEquals(CQGuiNightMap.endpointSpec(\envA)[\decoration], \root,
			"a root tip keeps its semantic while transparent paper preserves its curve");
	}

	test_preampProvenanceComesFromCQSpec {
		var values = CQSpec.enumValues(\preASrc);
		this.assertEquals(CQGuiNightMap.preampSourceValues(0), values,
			"preamp A reveals CQSpec's source order verbatim");
		this.assertEquals(CQGuiNightMap.preampSourceValues(1),
			CQSpec.enumValues(\preBSrc),
			"preamp B reveals its own CQSpec vocabulary");
		values.reject { |value| value == \none }.do { |value, i|
			this.assertEquals(CQGuiNightMap.preampSourceIndex(0, value), i,
				"% selects its one unlabelled intake strand".format(value));
		};
		this.assert(CQGuiNightMap.preampSourceIndex(0, \none).isNil,
			"`none` honestly lights no intake strand");
		this.assertEquals(CQGuiNightMap.preampSourceLabel(\liveL), "live l",
			"camel-case live provenance is readable only when revealed");
		this.assertEquals(CQGuiNightMap.preampSourceLabel(\deckB), "deck b",
			"deck provenance uses the bird's learned name");
	}

	// The night has no presets (decided with the user 2026-07-22, superseding
	// the sower constellation): a preset is a recipe, not a body, and it was
	// the night's only control that could not be used without reading text
	// (§16.4). The Inspector's own menu is one toolbar click away in either
	// view, so nothing was lost but a menu in costume.
	test_theNightCarriesNoPresets {
		this.assert(CQGuiNightMap.respondsTo(\presetNames).not,
			"the night map lists no recipes of its own");
		this.assert(CQGuiNightMap.instVarNames.every { |name|
			name.asString.containsi("preset").not
		}, "and keeps no preset state to draw");
		this.assert(CQGuiNightMap.methods.every { |method|
			#[sow, flashStar].includes(method.name).not
		}, "no sowing door survives on the night map");
	}

	test_theNightCarriesNoLegacyRangeLetters {
		this.assert(CQGuiNightMap.instVarNames.includes(\rangeLetters).not,
			"the playable ripple needs no i/b/a state beside its regime");
		this.assert(CQNightGeometry.respondsTo(\rangeLetter).not,
			"night geometry no longer translates current state into legacy text");
	}

	test_crescentInventory {
		this.assertEquals(CQGuiNightMap.crescentNames(0),
			[\coco1Spaf, \coco1In, \coco1Fb],
			"coco 1 hangs spaf/in/fb tide crescents under its moons");
		this.assertEquals(CQGuiNightMap.crescentNames(1),
			[\coco2Spaf, \coco2In, \coco2Fb],
			"coco 2 mirrors the same three crescents");
		this.assertEquals(CQGuiNightMap.crescentNames(nil),
			[\deckALevel, \deckBLevel],
			"each bird carries its deck-level crescent");
		(CQGuiNightMap.crescentNames(0) ++ CQGuiNightMap.crescentNames(1)
			++ CQGuiNightMap.crescentNames(nil)).do { |name|
			this.assert(CQSpec.at(name).notNil,
				"crescent % maps a real CQSpec parameter".format(name));
		};
	}

	test_dronePerformanceInventoryLivesOnItsSourceBodies {
		this.assertEquals(CQGuiNightMap.shellFlowNames(0),
			[\coco1Level, \coco1Organ, \coco1SlotQ, \coco1SlotX],
			"coco 1 owns level, audible organ, q intake and incoming x flow");
		this.assertEquals(CQGuiNightMap.shellFlowNames(1),
			[\coco2Level, \coco2Organ, \coco2SlotQ, \coco2SlotX],
			"coco 2 mirrors the same four performance organs");
		#[\level, \organ, \slotQ, \slotX].do { |target, i|
			this.assertEquals(CQGuiNightMap.shellFlowParam(0, target),
				CQGuiNightMap.shellFlowNames(0)[i],
				"the % hit resolves to one real funnel parameter".format(target));
		};
		this.assert(CQGuiNightMap.shellFlowParam(0, \bogus).isNil,
			"unknown shell flesh owns no parameter");
		[1, 2].do { |n|
			#[\Level, \Organ, \SlotQ, \SlotX].do { |suffix|
				this.assert(CQGuiNightMap.reachableParamNames.includes(
					("coco" ++ n ++ suffix).asSymbol),
					"Night coverage includes coco% %".format(n, suffix));
			};
		};
		(1..5).do { |k|
			this.assert(CQGuiNightMap.reachableParamNames.includes(
				("c" ++ k ++ "Send").asSymbol),
				"castle % owns its qMix send".format(k));
		};
		this.assertFloatEquals(CQSpec.map(\coco1Level,
			CQSpec.referenceNorm(\coco1Level, \unity)), 1.0,
			"the shoreline's unity notch comes from CQSpec", 1e-9);
	}

	test_crescentScalesNameOnlyHonestSemanticBoundaries {
		var spaf = CQGuiNightMap.crescentScaleFor(\coco1Spaf);
		var input = CQGuiNightMap.crescentScaleFor(\coco1In);
		var feedback = CQGuiNightMap.crescentScaleFor(\coco1Fb);
		var level = CQGuiNightMap.crescentScaleFor(\deckALevel);
		this.assertEquals(spaf[\kind], \spaf,
			"SP.AF names signed direction rather than danger");
		this.assertFloatEquals(spaf[\center], 0.5,
			"SP.AF zero is derived from its bipolar parameter map");
		this.assertEquals(spaf[\fine], [0.375, 0.625],
			"SP.AF's inner harbour is the CQSpec-owned +/-0.25 octave country");
		this.assertEquals(spaf[\octaves], [0.25, 0.75],
			"SP.AF's paired pinpricks land on exact +/-1 octave");
		this.assertEquals(input[\kind], \input,
			"input names normal gain and overdrive");
		this.assertFloatEquals(input[\unity], 0.5,
			"input unity is derived from CQSpec");
		this.assertEquals(feedback[\kind], \feedback,
			"feedback retains its audible three-region scale");
		this.assertEquals(feedback[\edges], [0.0, 0.5, 0.9, 1.0],
			"feedback scale follows its exact taper breakpoints");
		this.assertEquals(level[\kind], \level,
			"bird level exposes its one honest nominal/boost boundary");
		this.assertFloatEquals(CQSpec.map(\deckALevel, level[\unity]), 1.0,
			"the bird reference lands on exact unity despite its curved map");
		this.assert(CQGuiNightMap.crescentScaleFor(\qFreq).isNil,
			"a continuous control with no semantic boundary gets no scale");
		this.assert(CQGuiNightMap.crescentScaleFor(\coco1DolbyIn).isNil,
			"a similarly suffixed non-crescent cannot acquire a decorative scale");
	}

	test_crescentModulationSeparatesSlowTruthFromFastWake {
		var quiet = CQGuiNightMap.crescentModState(\coco1Spaf, 1, 0.25, 0);
		var spaf = CQGuiNightMap.crescentModState(\coco1Spaf, 0, 0.25, 0.60);
		var other = CQGuiNightMap.crescentModState(\coco2Spaf, 1, 0, 0.60);
		var clipped = CQGuiNightMap.crescentModState(\coco1Fb, 2, 0, 0.60);
		var fast = CQGuiNightMap.crescentModState(\coco1Spaf, 3.5, 2, 0.60);
		// PLAY-5 organ-source conditioning widens the former ~0.015-octave wake
		// by about ×1.6 while keeping its slow centre at zero.
		var organ = CQGuiNightMap.crescentModState(
			\coco1Spaf, 0, 0.024, 0.20);
		var organInput = CQGuiNightMap.crescentModState(
			\coco1Organ, 0.62, 0.08, 0.30);
		var slotQInput = CQGuiNightMap.crescentModState(
			\coco2SlotQ, 0.9, 0.12, 0.30);
		this.assert(quiet.isNil,
			"no connected matrix weight draws no invented live pearl");
		this.assertFloatEquals(spaf[\center], 0.5,
			"a centred applied pitch contribution sits at SP.AF zero", 1e-9);
		this.assertEquals([spaf[\lower], spaf[\upper]], [0.375, 0.625],
			"an unresolved +/-0.25 octave excursion spans the signed-square harbour");
		this.assertFloatEquals(other[\center], 0.75,
			"the same DSP octave maps identically on coco 2", 1e-9);
		this.assert(clipped[\center] == 1 and: { clipped[\clippedHigh] },
			"effective feedback beyond the hand range lands at a capped endpoint");
		this.assertFloatEquals(fast[\center],
			((((fast[\lower] + fast[\upper]) * 0.5)
				/ CQGuiNightMap.crescentModStep).round
				* CQGuiNightMap.crescentModStep),
			"a wide unresolved wake parks the satellite at its stable midpoint", 1e-9);
		this.assert(fast[\center] != CQSpec.unmap(\coco1Spaf, 3.5),
			"a clipped audio-rate wake cannot saw at its sampled instantaneous value");
		this.assert((organ[\upper] - organ[\lower]) >= 0.05,
			"the tutorial organ self-route survives quantization as a visible wake");
		this.assert(organInput.notNil and: { slotQInput.notNil },
			"the shell calyx and q gill accept the same honest DSP modulation frame");
		this.assert(
			CQGuiNightMap.crescentModStateChanged(spaf, spaf.copy).not,
			"an unchanged quantized wake costs no repaint");
		this.assert(CQGuiNightMap.crescentModStateChanged(spaf, other),
			"a visible satellite move requests its small view repaint");
	}

	test_castleMotionUsesPearlOnlyBelowTheDisplayCeiling {
		var slow = CQGuiNightMap.castleMotionState(-0.5, 1);
		var fastA = CQGuiNightMap.castleMotionState(-0.9, 120);
		var fastB = CQGuiNightMap.castleMotionState(0.9, 120);
		this.assertEquals(slow[\mode], \resolved,
			"an infra triangle is slow enough for a moving pearl");
		this.assertFloatEquals(slow[\position], 0.25,
			"the slow pearl maps the signed raw triangle onto its reed", 1e-9);
		this.assertEquals(fastA, (mode: \blur, position: 0.5, strength: 1),
			"an audio-rate triangle becomes one stable full-reed blur");
		this.assert(CQGuiNightMap.castleMotionStateChanged(fastA, fastB).not,
			"opposite audio samples cannot alias into a false two-position saw");
		this.assert(CQGuiNightMap.castleMotionState(nil, 1).isNil,
			"no oscillator telemetry invents no live pearl");
	}

	test_onlyCocoChannelCrescentsCarryDolby {
		#[coco1In, coco1Fb, coco2In, coco2Fb].do { |name|
			var dolby = CQGuiNightMap.dolbyNameForCrescent(name);
			this.assert(CQGuiNightMap.hasDolbyCrescent(name),
				"% owns an oyster".format(name));
			this.assert(CQSpec.at(dolby).notNil,
				"% maps to the real CQSpec enum %".format(name, dolby));
		};
		#[coco1Spaf, coco2Spaf, deckALevel, deckBLevel].do { |name|
			this.assert(CQGuiNightMap.hasDolbyCrescent(name).not,
				"% keeps its plain crescent".format(name));
			this.assert(CQGuiNightMap.dolbyNameForCrescent(name).isNil,
				"% invents no dolby parameter".format(name));
		};
		this.assertEquals(CQGuiNightMap.dolbyNameForCrescent(\coco1In),
			\coco1DolbyIn, "input oyster controls input dolby");
		this.assertEquals(CQGuiNightMap.dolbyNameForCrescent(\coco2Fb),
			\coco2DolbyFb, "feedback oyster controls feedback dolby");
	}

	// GUI_SPEC §6: only useful multi-endpoint Night bodies carry group solo.
	test_soloBodiesAreTheNightAnatomy {
		this.assertEquals(CQGuiNightMap.bodyNames,
			#[coco1, coco2, quantussy],
			"the two shells and whole ring keep useful multi-organ solos");
		this.assert(CQGuiNightMap.bodyEndpointNames(\castle3).isEmpty,
			"a castle's four precise endpoints need no redundant flesh solo");
		this.assertEquals(CQGuiNightMap.bodyEndpointNames(\coco2),
			#[coco2, org2, coco2_spaf, coco2_inVca, coco2_fbVca,
				coco2_flip, coco2_skip, coco2_organ, coco2_slotQ, coco2_record],
			"a shell owns its two sources and eight sockets");
		this.assertEquals(CQGuiNightMap.bodyEndpointNames(\quantussy),
			CQMatrix.sourceNames.copyRange(0, 9)
				++ CQMatrix.destNames.select { |dest|
					dest.asString.beginsWith("castle")
				},
			"the ring owns every castle source and destination");
		this.assert(CQGuiNightMap.bodyEndpointNames(\bogus).isEmpty,
			"an unknown body claims no endpoints");
	}

	test_everyBodyMemberIsACanonicalEndpoint {
		CQGuiNightMap.bodyNames.do { |name|
			var members = CQGuiNightMap.bodyEndpointNames(name);
			this.assert(members.notEmpty, "% is a real body".format(name));
			members.do { |member|
				this.assert(CQMatrix.srcIndex(member).notNil
					or: { CQMatrix.destIndex(member).notNil },
					"body member % is a canonical jack or socket".format(member));
			};
		};
		// roots and birds hold one endpoint each: an own body would only be a
		// second name for the jack/socket solo that already lights those cables
		this.assert(CQGuiNightMap.bodyNames.every { |name|
			CQGuiNightMap.bodyEndpointNames(name).size > 1
		}, "no body duplicates a single endpoint's solo");
	}
}
