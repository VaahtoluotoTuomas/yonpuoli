// CQGuiNightTouchTest — the pure gesture math of the §6.1 night touch
// grammar: winding angles wrap correctly and the sensitivities keep their
// promised sweep (two winds = the full speed range).

CQGuiNightTouchTest : UnitTest {

	test_angleDelta {
		this.assert((CQGuiNightTouch.angleDelta(0.1, 0.4) - 0.3).abs < 1e-9,
			"a small clockwise turn is a positive delta");
		this.assert((CQGuiNightTouch.angleDelta(0.4, 0.1) + 0.3).abs < 1e-9,
			"a small counter-clockwise turn is negative");
		this.assert(
			(CQGuiNightTouch.angleDelta(pi - 0.1, 0.1 - pi) - 0.2).abs < 1e-6,
			"crossing the pi seam stays a small step, never a full turn");
		this.assert(CQGuiNightTouch.angleDelta(0.5, 0.5) == 0,
			"no motion, no delta");
	}

	test_angleAt {
		var center = Point(100, 100);
		this.assert(CQGuiNightTouch.angleAt(center, Point(110, 100)).abs < 1e-9,
			"east is angle zero");
		this.assert((CQGuiNightTouch.angleAt(center, Point(100, 110))
			- 0.5pi).abs < 1e-9, "screen-down is +pi/2 (clockwise positive)");
	}

	// Skip left the read-head comet at the G2 gate. The comet is the only
	// target in the night that moves, and past a crawl a hand cannot land on
	// it at all; any telemetry frame draws it slightly stale on top of that. Its home is now
	// a stationary mark mirroring the flip chevron across the mouth jack.
	test_skipHasAStationaryHome {
		this.assert(CQGuiNightTouch.respondsTo(\sparkRadius).not,
			"the read head keeps no landing radius — it is display only");
		// 1024x460 is scale 0.8, below the minimum stage: the mark carries px
		// floors so the mouth's column survives a window smaller than the
		// layout was drawn for
		[[1280, 570], [1920, 855], [1024, 460]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			geo[\cocos].do { |coco, i|
				var n = i + 1, mark = coco[\skipMark];
				this.assert(CQNightGeometry.markHit(mark, mark),
					"coco % skip mark answers at its own centre (%x%)"
						.format(n, *size));
				this.assert(CQNightGeometry.markHit(mark, coco[\chevron]).not
					and: { CQNightGeometry.markHit(coco[\chevron], mark).not },
					"coco % skip and flip never claim each other (%x%)"
						.format(n, *size));
				this.assert(CQNightGeometry.markHit(mark, coco[\mouth]).not,
					"coco % skip mark clears the mouth jack (%x%)"
						.format(n, *size));
				// the spiral ENDS at the mouth and arrives from above, so the
				// symmetrical-looking home up there is the groove itself —
				// this assert is what caught that
				this.assert(CQNightGeometry.spiralDistance(coco, mark) > 8,
					"coco % skip mark sits off the winding surface (%x%)"
						.format(n, *size));
				coco[\moons].do { |moon, m|
					this.assert(CQNightGeometry.distance(mark, moon) >= 24,
						"coco % skip mark keeps spacing from moon % (%x%)"
							.format(n, m, *size));
				};
				this.assert(coco[\shellBox].contains(
					Rect.aboutPoint(mark, 10, 10)),
					"coco % skip touch box stays inside the shell (%x%)"
						.format(n, *size));
			};
		};
	}

	// The sowing moon resolves tap versus drag at the shared threshold, but its
	// phase preview is continuous from mouse-down. A released tap restores its
	// exact start before throwing; a phase drag therefore has no sticky quarter.
	test_almanacTapAndPour {
		this.assertEquals(CQGuiNightTouch.tapTravel, CQBananaGrammar.lassoTravel,
			"the tap threshold IS the lasso's, never a second number");
		this.assertEquals(CQGuiNightTouch.deadzone(3, 8), 0,
			"a wobbling tap pours nothing");
		this.assertEquals(CQGuiNightTouch.deadzone(-8, 8), 0,
			"and neither does one that wandered exactly to the edge");
		this.assertFloatEquals(CQGuiNightTouch.deadzone(20, 8), 12,
			"past the threshold the pour starts from zero", 1e-9);
		this.assertFloatEquals(CQGuiNightTouch.deadzone(-20, 8), -12,
			"a downward pour is the same distance the other way", 1e-9);
		this.assertFloatEquals(CQGuiNightTouch.moonPhaseNorm(0.25, 3), 0.275,
			"even a three-pixel phase move follows continuously", 1e-9);
		this.assertFloatEquals(CQGuiNightTouch.moonPhaseNorm(0.25, -60), 0,
			"continuous moon motion still clamps at the new moon", 1e-9);
	}

	// A bird has three acts but no new glyphs: a still press fetches, one
	// deliberate horizontal reversal chooses random, and an ordinary drag is
	// inert. Recognition is distance-based, never timer-based.
	test_birdTapRuffleAndInertDrag {
		var tap = CQGuiNightTouch.newRuffle(Point(0, 0));
		var edge = CQGuiNightTouch.newRuffle(Point(0, 0));
		var ruffle = CQGuiNightTouch.newRuffle(Point(0, 0));
		var repeated = CQGuiNightTouch.newRuffle(Point(0, 0));
		CQGuiNightTouch.advanceRuffle(tap, Point(7, 0));
		this.assertEquals(CQGuiNightTouch.birdGestureAction(tap), \fetch,
			"less than the shared 8 px threshold is a fetch tap");
		CQGuiNightTouch.advanceRuffle(edge, Point(8, 0));
		this.assert(CQGuiNightTouch.birdGestureAction(edge).isNil,
			"the exact boundary is already a drag, not a tap");
		[Point(7, 0), Point(0, 0)].do { |point|
			CQGuiNightTouch.advanceRuffle(ruffle, point)
		};
		this.assertEquals(ruffle[\reversals], 1,
			"one 6 px out-and-back is counted as the natural ruffle");
		this.assertEquals(CQGuiNightTouch.birdGestureAction(ruffle), \random,
			"the ruffle wins even when its compact shape stays inside the tap radius");
		this.assert(CQGuiNightTouch.birdPollPeriod > 0
			and: { CQGuiNightTouch.birdPollPeriod <= (1/30) },
			"the live cursor fallback samples quickly enough to catch a hand reversal");
		[Point(80, 0), Point(-80, 0), Point(80, 0), Point(-80, 0)].do { |point|
			CQGuiNightTouch.advanceRuffle(repeated, point)
		};
		this.assertEquals(CQGuiNightTouch.birdGestureAction(repeated), \random,
			"repeated or wide ruffling stays recognized once the reversal is clear");
	}

	test_birdWheelDirectionAndMomentumGate {
		this.assertEquals(CQGuiNightTouch.birdWheelAction(-1), \next,
			"wheel down walks to the next file");
		this.assertEquals(CQGuiNightTouch.birdWheelAction(1), \prev,
			"wheel up walks to the previous file");
		this.assert(CQGuiNightTouch.birdWheelAction(0).isNil,
			"a zero wheel delta has no deck action");
		this.assert(CQGuiNightTouch.birdWheelReady(nil, 12),
			"the first wheel event always lands");
		this.assert(CQGuiNightTouch.birdWheelReady(12, 12.1).not,
			"trackpad momentum inside 150 ms cannot skip another file");
		this.assert(CQGuiNightTouch.birdWheelReady(12, 12.16),
			"a later deliberate step is accepted");
	}

	// One moon, three acts. The first 8 px still belong to a tap; after that
	// the dominant axis locks for the rest of the gesture. Horizontal pours
	// phase, down brings rain/grow and up brings sun/wither.
	test_moonAxisLocksWeatherIntoTheBody {
		this.assertEquals(CQGuiNightTouch.moonAxis(Point(4, 3)), nil,
			"a tap-sized moon wobble chooses no act");
		this.assertEquals(CQGuiNightTouch.moonAxis(Point(20, 6)), \phase,
			"a horizontal pull pours the moon phase");
		this.assertEquals(CQGuiNightTouch.moonAxis(Point(-20, 6)), \phase,
			"phase is the same act in either horizontal direction");
		this.assertEquals(CQGuiNightTouch.moonAxis(Point(5, 20)), \grow,
			"a downward pull calls rain / grow");
		this.assertEquals(CQGuiNightTouch.moonAxis(Point(5, -20)), \wither,
			"an upward pull calls sun / wither");
	}

	test_weatherTensionIsVisibleAndPerceptuallyBalanced {
		this.assertFloatEquals(
			CQGuiNightTouch.weatherIntensity(Point(0, 8)), 0,
			"the axis-lock distance has no hidden weather jump", 1e-9);
		this.assertFloatEquals(
			CQGuiNightTouch.weatherIntensity(Point(0, 24)), 0.5,
			"a half pull carries half the visible weather tension", 1e-9);
		this.assertFloatEquals(
			CQGuiNightTouch.weatherIntensity(Point(0, -40)), 1,
			"sun reaches full tension in the same travel as rain", 1e-9);
		this.assert(
			CQGuiNightTouch.weatherRate(\grow, 0)
				< CQGuiNightTouch.weatherRate(\grow, 1),
			"pulling rain farther makes it denser");
		this.assert(
			CQGuiNightTouch.weatherRate(\wither, 0)
				< CQGuiNightTouch.weatherRate(\wither, 1),
			"pulling sun farther makes it stronger");
		this.assert(
			CQGuiNightTouch.weatherRate(\grow, 1)
				< CQGuiNightTouch.weatherRate(\wither, 0),
			"even hard rain stays slower than mild sun because their steps differ");
		this.assert(
			CQGuiNightTouch.weatherClockPeriod
				< (1 / CQGuiNightTouch.sunRateMax),
			"the tension integrator follows the hand faster than its fastest act");
	}

	// Open paper is inert until the bay is deliberately focused. Shift is the
	// momentary entrance and Tab/the gill is the persistent entrance; once
	// focused, left pours and right becomes the elastic voyage. Outside focus,
	// right remains the learned harvest lasso and an accidental left sweep
	// changes nothing.
	test_tideGestureRequiresWaterFocus {
		this.assertEquals(CQGuiNightTouch.tideMode(0, false), nil,
			"plain left on unfocused water is inert");
		this.assertEquals(CQGuiNightTouch.tideMode(1, false), nil,
			"plain right on unfocused water remains the harvest lasso");
		this.assertEquals(CQGuiNightTouch.tideMode(0, true), \blend,
			"focused left pours the bay");
		this.assertEquals(CQGuiNightTouch.tideMode(1, true), \elastic,
			"focused right is the explicit elastic voyage");
	}

	test_enumCycleReadsCQSpecAndWraps {
		this.assertEquals(CQGuiNightTouch.nextEnum(\c1Range, \infra), \balcium,
			"the range ripple advances to CQSpec's next regime");
		this.assertEquals(CQGuiNightTouch.nextEnum(\c1Range, \balcium), \audio,
			"the middle regime advances to audio");
		this.assertEquals(CQGuiNightTouch.nextEnum(\c1Range, \audio), \infra,
			"the final regime wraps to CQSpec's first choice");
		this.assertEquals(CQGuiNightTouch.nextEnum(\c1Range, \broken), \balcium,
			"a malformed current value clamps to the spec default before stepping");
		this.assert(CQGuiNightTouch.nextEnum(\qFreq, 4).isNil,
			"a non-enum cannot be stepped");
		this.assertEquals(CQGuiNightTouch.nextEnum(\coco1DolbyIn, \off), \gate,
			"the input oyster follows CQSpec's dolby order");
		this.assertEquals(CQGuiNightTouch.nextEnum(\coco1DolbyIn, \gate), \duck,
			"the oyster reaches duck without a literal mode list");
		this.assertEquals(CQGuiNightTouch.nextEnum(\coco1DolbyIn, \duck), \off,
			"the oyster wraps to off");
	}

	test_sensitivities {
		this.assert(
			((4pi * CQGuiNightTouch.windSensitivity) - 1).abs < 1e-9,
			"two full winds sweep the whole speed range");
		this.assert(CQGuiNightTouch.crescentSensitivity > 0
			and: { CQGuiNightTouch.eyeSensitivity > 0 },
			"crescent and eye drags move their params upward for upward pulls");
		this.assert(CQGuiNightTouch.eyeSensitivity
			< CQGuiNightTouch.crescentSensitivity,
			"the eye moves the macro pair more gently than a crescent");
	}

	test_rightClickZerosOnlyTheTwoSpafCrescents {
		#[coco1Spaf, coco2Spaf].do { |name|
			this.assert(CQGuiNightTouch.spafZeroReset(name, 1),
				"right-click gives % an exact zero home".format(name));
			this.assert(CQGuiNightTouch.spafZeroReset(name, 0).not,
				"left drag remains continuous on %".format(name));
		};
		#[coco1In, coco1Fb, deckALevel, qFreq].do { |name|
			this.assert(CQGuiNightTouch.spafZeroReset(name, 1).not,
				"% keeps its existing right-button grammar".format(name));
		};
	}

	test_preampKnotLocksDriveAndBiasIntoTwoGentleAxes {
		this.assert(CQGuiNightTouch.preampSensitivity > 0,
			"the preamp knot increases drive when the hand pulls upward");
		this.assertEquals(CQGuiNightTouch.preampAxis(Point(6, 4)), nil,
			"a tap-sized wobble changes neither preamp parameter");
		this.assertEquals(CQGuiNightTouch.preampAxis(Point(20, 4)), \bias,
			"a horizontal pull locks bias");
		this.assertEquals(CQGuiNightTouch.preampAxis(Point(4, -20)), \drive,
			"a vertical pull locks drive");
		this.assertFloatEquals(
			CQGuiNightTouch.preampDriveNorm(0.25, 100, 40), 0.5,
			"a 60 px upward pull adds one quarter of the drive range", 1e-9);
		this.assertFloatEquals(
			CQGuiNightTouch.preampDriveNorm(0.25, 100, 160), 0,
			"a downward pull reduces drive and clamps at the CQSpec floor", 1e-9);
		this.assertFloatEquals(
			CQGuiNightTouch.preampDriveNorm(0.9, 100, -200), 1,
			"a hard upward pull clamps at the CQSpec ceiling", 1e-9);
		this.assertFloatEquals(
			CQGuiNightTouch.preampBiasNorm(0.25, 100, 160), 0.5,
			"a 60 px rightward knead adds one quarter of the bias range", 1e-9);
		this.assertFloatEquals(
			CQGuiNightTouch.preampBiasNorm(0.1, 100, 0), 0,
			"a leftward bias knead clamps at symmetry", 1e-9);
	}

	test_castleSendUsesAnInwardDeadzoneAndTheWholeRange {
		var inward = Point(0, 1);
		this.assertFloatEquals(CQGuiNightTouch.castleSendNorm(
			0.5, Point(0, 7), inward), 0.5,
			"sub-tap travel still belongs to the spesal step", 1e-9);
		this.assertFloatEquals(CQGuiNightTouch.castleSendNorm(
			0.5, Point(0, 58), inward), 1.0,
			"an inward radial pull opens the qMix tributary", 1e-9);
		this.assertFloatEquals(CQGuiNightTouch.castleSendNorm(
			0.5, Point(0, -58), inward), 0.0,
			"an outward radial pull closes it", 1e-9);
		this.assertFloatEquals(CQGuiNightTouch.castleSendNorm(
			0.5, Point(40, 0), inward), 0.5,
			"tangential travel invents no send change", 1e-9);
	}

	test_dustVeilSeparatesRadialAmountFromTangentialGrain {
		var axis = Point(1, 0), tangent = Point(0, 1), mixed, radial, across;
		mixed = CQGuiNightTouch.dustAxisNorms(
			0.2, 0.5, Point(60, -30), axis, tangent);
		this.assertFloatEquals(mixed[0], 0.7,
			"60 px outward adds half of the Dust amount range", 1e-9);
		this.assertFloatEquals(mixed[1], 0.25,
			"30 px across toward mist removes one quarter of grain", 1e-9);
		radial = CQGuiNightTouch.dustAxisNorms(
			0.2, 0.5, Point(120, 0), axis, tangent);
		this.assertFloatEquals(radial[0], 1,
			"a full radial span clamps amount at its ceiling", 1e-9);
		this.assertFloatEquals(radial[1], 0.5,
			"a purely radial pull leaves grain exactly at home", 1e-9);
		across = CQGuiNightTouch.dustAxisNorms(
			0.2, 0.5, Point(0, 120), axis, tangent);
		this.assertFloatEquals(across[0], 0.2,
			"a purely tangential pull leaves amount untouched", 1e-9);
		this.assertFloatEquals(across[1], 1,
			"one tangential span reaches the chip end", 1e-9);
	}
}
