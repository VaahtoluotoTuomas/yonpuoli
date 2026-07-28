// CQSpecTest — the clamp funnel (SPEC §8.3, A2): malformed values are never
// applied raw; ranges/defaults mirror SPEC §5; 16n map mirrors SPEC §6.1.

CQSpecTest : UnitTest {

	setUp { CQSpec.postWarnings = false }
	tearDown { CQSpec.postWarnings = true }

	test_registryMatchesSpec {
		this.assertEquals(CQSpec.specs.size, 74, "SPEC §5 defines 74 parameters");
		this.assertEquals(CQSpec.default(\coco1Fb), 0.35, "coco1Fb default");
		this.assertEquals(CQSpec.default(\coco1Pan), -0.3, "coco1Pan default");
		this.assertEquals(CQSpec.default(\coco2Pan), 0.3, "coco2Pan default");
		this.assertEquals(CQSpec.default(\micGain), 0.0,
			"micGain MUST default to 0 — mic feedback safety (SPEC §7)");
		this.assertEquals(CQSpec.default(\c3Range), \balcium, "c3Range default");
		this.assertEquals(CQSpec.default(\c5Send), 0.5, "c5Send default");
		this.assertEquals(CQSpec.default(\coco1Hold), 1,
			"power-on state is loop-locked (SPEC §5.3)");
	}

	test_defaultsSurviveClamp {
		CQSpec.names.do { |name|
			this.assertEquals(CQSpec.clamp(name, CQSpec.default(name)), CQSpec.default(name),
				"clamp(default) is identity for '%'".format(name));
		};
	}

	test_numericClamp {
		this.assertEquals(CQSpec.clamp(\coco1Fb, 99), 1.25, "over-range clips to max");
		this.assertEquals(CQSpec.clamp(\coco1Fb, -1), 0.0, "under-range clips to min");
		this.assertEquals(CQSpec.clamp(\masterVol, 20), 0.0, "masterVol tops at 0 dB");
		this.assertEquals(CQSpec.clamp(\coco1Speed, 100), 3000.0,
			"speed knob floor is 3000 Hz (SPEC §5.3)");
	}

	test_garbageFuzz {
		var garbage = [0.0 / 0.0, inf, inf.neg, nil, \banana, "noise", [1, 2, 3], (a: 1), $x];
		CQSpec.names.do { |name|
			var e = CQSpec.at(name);
			garbage.do { |g|
				var v = CQSpec.clamp(name, g);
				switch(e[\kind],
					\num, {
						this.assert(
							v.isNumber and: { v.asFloat.isNaN.not } and: {
								v.inclusivelyBetween(e[\spec].minval, e[\spec].maxval) },
							"garbage % on num '%' yields in-range number (got %)"
								.format(g, name, v));
					},
					\int, {
						this.assert(
							v.isInteger and: {
								v.inclusivelyBetween(e[\spec].minval, e[\spec].maxval) },
							"garbage % on int '%' yields in-range integer (got %)"
								.format(g, name, v));
					},
					\toggle, {
						this.assert([0, 1].includes(v),
							"garbage % on toggle '%' yields 0/1 (got %)".format(g, name, v));
					},
					\enum, {
						this.assert(e[\values].includes(v),
							"garbage % on enum '%' yields a valid symbol (got %)"
								.format(g, name, v));
					}
				);
			};
		};
	}

	test_toggleCoercion {
		this.assertEquals(CQSpec.clamp(\coco1Hold, true), 1, "true -> 1");
		this.assertEquals(CQSpec.clamp(\coco1Hold, false), 0, "false -> 0");
		this.assertEquals(CQSpec.clamp(\coco1Hold, 0.7), 1, "0.7 rounds to 1");
		this.assertEquals(CQSpec.clamp(\coco1Hold, -5), 0, "-5 clips to 0");
		this.assertEquals(CQSpec.clamp(\coco1Wrap, 2), 1, "2 clips to 1");
	}

	test_intClamp {
		this.assertEquals(CQSpec.clamp(\coco1Bits, 7.6), 8, "7.6 rounds to 8");
		this.assertEquals(CQSpec.clamp(\coco1Bits, 99), 12, "99 clips to 12");
		this.assertEquals(CQSpec.clamp(\coco2Bits, 1), 3, "1 clips to 3");
		this.assert(CQSpec.clamp(\coco1Bits, 9.2).isInteger, "bits clamps to Integer");
	}

	test_enumClamp {
		this.assertEquals(CQSpec.clamp(\coco1DolbyIn, \duck), \duck, "valid symbol passes");
		this.assertEquals(CQSpec.clamp(\coco1DolbyIn, \banana), \off, "invalid -> default");
		this.assertEquals(CQSpec.clamp(\preASrc, "mic"), \mic, "strings coerce to symbols");
		this.assertEquals(CQSpec.clamp(\c1Range, \audio), \audio, "range symbol passes");
	}

	test_enumValues {
		this.assertEquals(CQSpec.enumValues(\c1Range),
			#[infra, balcium, audio],
			"enum controls read their ordered vocabulary from CQSpec");
		this.assert(CQSpec.enumValues(\qFreq).isNil,
			"a numeric parameter advertises no enum vocabulary");
		this.assert(CQSpec.enumValues(\bogus).isNil,
			"an unknown parameter advertises no enum vocabulary");
	}

	test_unknownParam {
		this.assert(CQSpec.clamp(\notAParam, 1).isNil, "unknown parameter clamps to nil");
		this.assert(CQSpec.default(\notAParam).isNil, "unknown parameter has nil default");
	}

	test_16nMapMirrorsSpec {
		var expected = [
			[\coco1In], [\coco1Speed], [\coco1Spaf], [\coco1Fb],
			[\coco2In], [\coco2Speed], [\coco2Spaf], [\coco2Fb],
			[\qFreq], [\qChaos],
			[\preADrive, \preBDrive],
			[\deckARate, \deckBRate],
			[\deckALevel, \deckBLevel],
			[], [\patchDepth], [\masterVol]
		];
		this.assertEquals(CQSpec.map16n, expected, "16n default map mirrors SPEC §6.1");
		CQSpec.map16n.flatten.do { |name|
			this.assert(CQSpec.includes(name), "16n target '%' exists".format(name));
		};
	}

	test_gainLags {
		[\liveGain, \micGain, \deckALevel, \coco1In, \coco1Fb, \coco1Level,
			\coco1Dust, \coco1Organ, \masterDry].do { |name|
			this.assert(CQSpec.lag(name) >= 0.03,
				"gain param '%' lagged >= 30 ms (CLAUDE.md safety)".format(name));
		};
		this.assertFloatEquals(CQSpec.lag(\masterVol), 0.1, "masterVol lagged 100 ms");
		this.assertFloatEquals(CQSpec.lag(\patchDepth), 0.05, "patchDepth lagged 50 ms");
	}

	test_mapNormalized {
		this.assertFloatEquals(CQSpec.map(\coco1Fb, 0), 0.0, "map(0) = min");
		this.assertFloatEquals(CQSpec.map(\coco1Fb, 1), 1.25, "map(1) = max");
		this.assertFloatEquals(CQSpec.map(\coco1Fb, 2), 1.25, "norm input clipped to 0..1");
		this.assertFloatEquals(CQSpec.unmap(\coco1Fb, CQSpec.map(\coco1Fb, 0.42)), 0.42,
			"map/unmap roundtrip");
		this.assertEquals(CQSpec.map(\coco1Bits, 1), 12, "int map lands on integer max");
		this.assertEquals(CQSpec.map(\coco1Hold, 0.9), 1, "toggle map thresholds at 0.5");
	}

	test_feedbackTaperIsExactAndInvertible {
		var norms = [0, 0.1, 0.499999, 0.5, 0.7, 0.899999, 0.9, 0.95, 1];
		[\coco1Fb, \coco2Fb].do { |name|
			this.assertFloatEquals(CQSpec.map(name, 0.0), 0.0,
				"% taper starts at silence".format(name), 1e-12);
			this.assertFloatEquals(CQSpec.map(name, 0.5), 0.85,
				"% taper enters the riding band at mid-fader".format(name), 1e-12);
			this.assertFloatEquals(CQSpec.map(name, 0.9), 1.10,
				"% taper leaves the riding band at 90%".format(name), 1e-12);
			this.assertFloatEquals(CQSpec.map(name, 1.0), 1.25,
				"% taper keeps the old maximum".format(name), 1e-12);
			this.assertFloatEquals(CQSpec.map(name, 0.7), 0.975,
				"% taper is piecewise linear between its breakpoints".format(name), 1e-12);
			this.assertFloatEquals(CQSpec.unmap(name, 1.0), 0.74,
				"% puts unity at a broad, repeatable fader position".format(name), 1e-12);
			norms.do { |norm|
				var back = CQSpec.unmap(name, CQSpec.map(name, norm));
				this.assert((back - norm).abs <= 1e-9,
					"% map/unmap roundtrip at % has error %"
						.format(name, norm, (back - norm).abs));
			};
		};
	}

	test_feedbackTaperPublishesItsVisualRegionsWithoutLeakingOwnership {
		var points = CQSpec.mapBreakpoints(\coco1Fb);
		this.assertEquals(points,
			[[0.0, 0.0], [0.5, 0.85], [0.9, 1.10], [1.0, 1.25]],
			"the visual scale reads the exact registry-owned taper");
		this.assert(CQSpec.mapBreakpoints(\coco1In).isNil,
			"a linear control advertises no invented regions");
		points[1][0] = 0.25;
		this.assertEquals(CQSpec.mapBreakpoints(\coco1Fb)[1][0], 0.5,
			"a caller cannot mutate CQSpec's breakpoint table");
	}

	test_chaosCurveWidensTheDrunkenCountry {
		var norms = [0, 0.1, 0.25, 0.5, 0.75, 0.9, 1];
		this.assertFloatEquals(CQSpec.map(\qChaos, 0.0), 0.0,
			"chaos hand starts at the unchanged stable value", 1e-12);
		this.assertFloatEquals(CQSpec.map(\qChaos, 0.5), 0.2689414213699951,
			"chaos noon lands in the deep-wander country", 1e-12);
		this.assertFloatEquals(CQSpec.map(\qChaos, 0.75), 0.5449457660765887,
			"hyperchaos is compressed into the last quarter", 1e-12);
		this.assertFloatEquals(CQSpec.map(\qChaos, 1.0), 1.0,
			"chaos hand keeps the unchanged DSP maximum", 1e-12);
		norms.do { |norm|
			var back = CQSpec.unmap(\qChaos, CQSpec.map(\qChaos, norm));
			this.assert((back - norm).abs <= 1e-9,
				"qChaos map/unmap roundtrip at % has error %"
					.format(norm, (back - norm).abs));
		};
	}

	test_semanticReferenceMarksUseTheParametersOwnMap {
		var deckUnity = CQSpec.referenceNorm(\deckALevel, \unity);
		this.assertFloatEquals(CQSpec.referenceNorm(\coco1In, \unity), 0.5,
			"input unity sits at the midpoint of its own linear map");
		this.assertFloatEquals(CQSpec.map(\deckALevel, deckUnity), 1.0,
			"the bird mark roundtrips to exact unity through its curved map");
		this.assert(CQSpec.referenceNorm(\coco1Fb, \unity).isNil,
			"feedback does not invent a second scale beside its audible regions");
		this.assert(CQSpec.referenceNorm(\bogus, \unity).isNil,
			"an unknown parameter advertises no reference mark");
	}
}
