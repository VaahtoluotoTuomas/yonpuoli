// CQSynthDefsTest — the pure, language-side DSP laws that can be checked
// without a server. The SynthDefs themselves are proven by the NRT renders
// (nrt_safety, nrt_hold_integrity); what lives here are the small algebraic
// rules those graphs are built from, where a sign or a fixed point going
// wrong would be inaudible in a unit run but wrong in the instrument.

CQSynthDefsTest : UnitTest {

	test_ioPresenceNeverExceedsTheAmbientCeiling {
		this.assertEquals(CQSynthDefs.ioPresenceReplyHz, 15,
			"raw input presence reports at the shared 15 Hz ambient ceiling");
	}

	// §13.5's VCA law. It is applied to the matrix modulation alone, because
	// in this instrument the knob is the VCA's base and the matrix adds to it
	// (SPEC §5.5). The anchors keep the zero-sum knob/default calibration;
	// stored VCA cable weights keep their numbers but are sonically revoiced.
	test_vcaLawFixedPoints {
		this.assertEquals(CQSynthDefs.prVcaLaw(0), 0.0,
			"no modulation contributes nothing — every knob keeps its meaning");
		this.assertEquals(CQSynthDefs.prVcaLaw(-1), -1.0,
			"sum -1 subtracts one (closing a knob at or below unity)");
		this.assertEquals(CQSynthDefs.prVcaLaw(1), 3.0,
			"sum +1 pushes +3 where linear pushed +1 (§13.5's hotter overdrive)");
	}

	// At unity the shaped contribution reproduces the plan's own law BEFORE
	// the real VCA's 2.0/2.5 ceiling: 1 + shaped(x) == (1 + x) ** 2.
	test_vcaLawIsThePlansPreClipLawAtUnity {
		[-1, -0.5, -0.25, 0, 0.25, 0.5, 1, 1.5, 2].do { |sum|
			var shaped = 1 + CQSynthDefs.prVcaLaw(sum);
			var plan = (1 + sum) ** 2;
			this.assert((shaped - plan).abs < 1e-9,
				"at unity, sum % gives the plan's (1+x)**2 = % (got %)"
					.format(sum, plan, shaped));
		};
	}

	// The final gain ceiling is musically important: the accepted p=2 law reaches
	// the rail sooner, but it never realizes the sketch's unbounded x4 gain.
	// At knob=1, sum=1, input is x2 and feedback x2.5 after their real clips.
	test_vcaLawKeepsTheRealGainCeilings {
		var preClip = 1 + CQSynthDefs.prVcaLaw(1);
		this.assertEquals(preClip, 4.0, "the helper produces the pre-clip x4 value");
		this.assertEquals(preClip.clip(0, 2), 2.0,
			"the actual input VCA remains capped at x2");
		this.assertEquals(preClip.clip(0, 2.5), 2.5,
			"the actual feedback VCA remains capped at x2.5");
	}

	test_softwareNativeCocoInputsUseTheirOwnMusicalCeilings {
		var organAtPointThree = CQSynthDefs.prVcaLaw(0.3)
			.clip(0, CQSynthDefs.organVcaCeiling);
		var slotAtPointFour = CQSynthDefs.prVcaLaw(0.4)
			.clip(0, CQSynthDefs.slotQVcaCeiling);
		this.assertEquals(CQSynthDefs.organVcaCeiling, 1.0,
			"matrix Organ cannot exceed the hand calyx's full-open value");
		this.assertEquals(CQSynthDefs.slotQVcaCeiling, 1.5,
			"matrix SlotQ shares the hand gill's documented gain country");
		this.assertEquals(CQSynthDefs.slotXVcaCeiling, CQSynthDefs.slotQVcaCeiling,
			"matrix SlotX is the mirrored gill and uses the same bounded rail");
		this.assert(organAtPointThree.inclusivelyBetween(0.68, 0.70),
			"a tuned 0.30 Organ cable is clearly audible without rail pinning");
		this.assert(slotAtPointFour.inclusivelyBetween(0.95, 0.97),
			"a tuned 0.40 SlotQ cable opens most of the Quantussy tributary");
		this.assertFloatEquals(CQSynthDefs.organAudibleGain, 0.55,
			"PLAY-8 lifts the audible organ branch by 5.3 dB without moving its source", 1e-9);
	}

	test_recordMatrixGateMomentarilyInvertsManualHold {
		this.assertEquals(CQSynthDefs.recordToggledHold(1, 0), 1,
			"a sealed writer stays sealed with no Record gate");
		this.assertEquals(CQSynthDefs.recordToggledHold(1, 1), 0,
			"a Record gate opens a sealed writer");
		this.assertEquals(CQSynthDefs.recordToggledHold(0, 0), 0,
			"an open writer stays open with no Record gate");
		this.assertEquals(CQSynthDefs.recordToggledHold(0, 1), 1,
			"the same Record gate freezes an open writer");
	}

	test_cocoSynthOwnsAllFiveIndependentSoftwareInputBuses {
		var def = CQSynthDefs.all(CQConfig.validate(())).detect { |item|
			item.name == \cqCoco
		};
		var controls = def.allControlNames.collect { |control| control.name.asSymbol };
		#[wOrganBus, wSlotQBus, wSlotXBus, wDustBus, wRecordBus].do { |name|
			this.assert(controls.includes(name),
				"cqCoco exposes the independent % matrix bus".format(name));
		};
		this.assert(CQSynthDefs.gateOnThreshold < 0.58,
			"the ecological record floor has measured Schmitt headroom");
	}

	test_dustMatrixLawHasSignedQuietGrowthAndHardMaterialRails {
		this.assertFloatEquals(CQSynthDefs.dustMatrixContribution(0), 0,
			"an unpatched Dust socket leaves the hand untouched", 1e-12);
		this.assertFloatEquals(CQSynthDefs.dustMatrixContribution(0.5), 0.045,
			"a half banana shades Dust gently through signed-square shaping", 1e-12);
		this.assertFloatEquals(CQSynthDefs.dustMatrixContribution(-0.5), -0.045,
			"negative phase clears by the same amount", 1e-12);
		this.assertFloatEquals(CQSynthDefs.dustMatrixContribution(1), 0.18,
			"full positive modulation reaches the documented +0.18 range", 1e-12);
		this.assertFloatEquals(CQSynthDefs.dustMatrixContribution(-1), -0.18,
			"full negative modulation reaches the documented -0.18 range", 1e-12);
		this.assertFloatEquals(CQSynthDefs.dustMatrixContribution(99), 0.18,
			"a mayhem sum clamps before shaping", 1e-12);
		this.assertFloatEquals(CQSynthDefs.dustEffective(0.2, 0), 0.2,
			"zero matrix sum is exactly backward compatible", 1e-12);
		this.assertFloatEquals(CQSynthDefs.dustEffective(0.1, -1), 0,
			"negative modulation cannot drive material below silence", 1e-12);
		this.assertFloatEquals(CQSynthDefs.dustEffective(0.4, 1), 0.5,
			"positive modulation cannot exceed the converter's Dust rail", 1e-12);
	}

	test_dustGrainMidpointExactlyPreservesTheOldTwoSourceFormula {
		var mist = CQSynthDefs.dustGrainGains(0);
		var legacy = CQSynthDefs.dustGrainGains(0.5);
		var chip = CQSynthDefs.dustGrainGains(1);
		this.assertFloatEquals(legacy[0], 1, "legacy mist gain stays exactly one", 1e-12);
		this.assertFloatEquals(legacy[1], 1, "legacy chip gain stays exactly one", 1e-12);
		this.assertFloatEquals(mist[0], 2.sqrt,
			"the mist end keeps equal-power loudness", 1e-12);
		this.assertFloatEquals(mist[1], 0, "the mist end removes chips", 1e-12);
		this.assertFloatEquals(chip[0], 0, "the chip end removes mist", 1e-12);
		this.assertFloatEquals(chip[1], 2.sqrt,
			"the chip end keeps equal-power loudness", 1e-12);
	}

	// A VCA that folds back up as its control drives further negative would
	// re-open a valve the hand is closing. `g.abs * g` is signed precisely so
	// the law stays monotonic on both sides of g = 0.
	test_vcaLawIsMonotonic {
		var previous = -inf;
		41.do { |k|
			var sum = (k / 10) - 2.5;             // -2.5 .. +1.5, across both clamps
			var value = CQSynthDefs.prVcaLaw(sum);
			this.assert(value >= previous,
				"the law never turns back at sum % (% after %)"
					.format(sum, value, previous));
			previous = value;
		};
	}

	// §13.5's rake: \mayhem swings a VCA sum far past ±1, and squaring an
	// unclamped swing would scorch the tone long before the output clip caught
	// it. The clamp is on the SUM, before the shaping.
	test_vcaLawClampsBeforeShaping {
		var ceiling = CQSynthDefs.prVcaLaw(CQSynthDefs.vcaSumClamp);
		var floor = CQSynthDefs.prVcaLaw(CQSynthDefs.vcaSumClamp.neg);
		this.assertEquals(CQSynthDefs.prVcaLaw(40), ceiling,
			"an absurd positive sum saturates at the clamp, it does not square");
		this.assertEquals(CQSynthDefs.prVcaLaw(-40), floor,
			"and an absurd negative one saturates too");
		this.assert(ceiling.isNumber and: { ceiling < 10 },
			"the clamped ceiling stays a sane gain contribution (got %)"
				.format(ceiling));
	}

	// §13.8 accepted the conservative middle curve: one dB of attenuation per
	// doubling of the actual Drive value. It is a fixed interpretation, not a
	// GUI-normalized map or a new snapshot parameter.
	test_preampMakeupUsesActualDriveOctaves {
		this.assertFloatEquals(CQSynthDefs.prPreampMakeup(1), 1.0,
			"Drive 1 is the exact unity anchor", 1e-9);
		this.assertFloatEquals(CQSynthDefs.prPreampMakeup(2), (-1).dbamp,
			"one Drive octave attenuates exactly 1 dB", 1e-9);
		this.assertFloatEquals(CQSynthDefs.prPreampMakeup(4), (-2).dbamp,
			"two Drive octaves attenuate exactly 2 dB", 1e-9);
		this.assertFloatEquals(CQSynthDefs.prPreampMakeup(40),
			(CQSynthDefs.preampMakeupDbPerOct.neg * 40.log2).dbamp,
			"Drive 40 follows the same actual-value curve", 1e-9);
		this.assert(CQSynthDefs.prPreampMakeup(40)
			< CQSynthDefs.prPreampMakeup(16),
			"attenuation remains monotonic across the full CQSpec range");
	}

}
