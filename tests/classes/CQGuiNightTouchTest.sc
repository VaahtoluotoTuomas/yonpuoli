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
	// it at all; a 5 Hz frame draws it stale on top of that. Its home is now
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

	// The sowing moon's one threshold (§6.1): under it a press is a tap — a
	// shake thrown at the moon's current phase — and past it the moon pours
	// that phase instead, horizontally, the way its own terminator travels. It
	// is the harvest lasso's own number, so the hand learns "pressed" versus
	// "dragged" once for the whole night, and the tap's own px are dead, so a
	// throw never nudges the phase it throws and a pour cannot jump when it
	// engages.
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
		this.assert(CQGuiNightTouch.tapTravel
			< (1 / CQGuiNightTouch.crescentSensitivity),
			"the dead tap costs a sliver of the moon's range, never a jump");
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
}
