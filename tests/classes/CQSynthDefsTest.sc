// CQSynthDefsTest — the pure, language-side DSP laws that can be checked
// without a server. The SynthDefs themselves are proven by the NRT renders
// (nrt_safety, nrt_hold_integrity); what lives here are the small algebraic
// rules those graphs are built from, where a sign or a fixed point going
// wrong would be inaudible in a unit run but wrong in the instrument.

CQSynthDefsTest : UnitTest {

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
