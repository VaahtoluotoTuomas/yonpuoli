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
