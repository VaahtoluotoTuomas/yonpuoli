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
