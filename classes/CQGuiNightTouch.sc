// CQGuiNightTouch — the §6.1 night touch grammar: winding the shells, the
// hold hearts, the mouth's flip chevron and skip mark, the tide
// crescents, the ring's eye and the spesal dial taps. Owns only transient
// gesture state; every write goes through the CQState funnel (params) or the
// Cocoquantus facade (spesal step, skip), exactly like the Day widgets. On every press the cable layer is
// consulted first (§4.5) and everything unclaimed falls through to the shared
// banana controller's ground grammar.

CQGuiNightTouch {
	var map, state, overlay;
	var <>controller;   // set after construction — the controller binds later
	var wind, eyeDrag, crescentDrag, moonDrag;

	*new { |map, state, overlay| ^super.newCopyArgs(map, state, overlay) }

	// ---- pure gesture math ----

	*angleAt { |center, point|
		^(point.y - center.y).atan2(point.x - center.x)
	}

	// signed minimal angle difference, positive = screen-clockwise
	*angleDelta { |from, to| ^(to - from).wrap(-pi, pi) }

	// two full winds sweep the whole speed range
	*windSensitivity { ^1 / 4pi }
	// px of drag for the full range of a crescent / the eye's axes
	*crescentSensitivity { ^1 / 120 }
	*eyeSensitivity { ^1 / 220 }

	// the sowing moon's one threshold (§6.1): a press that travels less than the
	// harvest lasso's own 8 px is a **tap** — the throw — and anything longer
	// pours the phase instead. One body, two acts, told apart by the hand alone,
	// and told apart by the same number the lasso already teaches.
	*tapTravel { ^CQBananaGrammar.lassoTravel }

	// the tap's px belong to the tap: a throw must never nudge the phase, and a
	// pour must not jump the moment it engages
	*deadzone { |delta, dead|
		if(delta.abs <= dead) { ^0 };
		^delta - (dead * delta.sign)
	}

	// ---- shell: wind (speed) · heart (hold) · chevron (flip) · mark (skip) ----

	shellDown { |index, view, x, y, modifiers = 0, button = 0, clicks = 1|
		var point = this.prPoint(view, x, y), geo = map.geometry, coco, n;
		var body = ("coco" ++ (index + 1)).asSymbol;
		if(geo.isNil) { ^this };
		// §4.5 controls first, at every click count: a fast second tap belongs to
		// the touch point under it, never to the body solo behind it. Shell paper
		// between the turns is what carries the double-click to the body.
		if(button != 0 or: { modifiers.isAlt }
			or: { overlay.patchAt(point).notNil }) {
			^this.forwardDown(view, x, y, modifiers, button, clicks, body)
		};
		coco = geo[\cocos][index];
		n = index + 1;
		if(CQNightGeometry.distance(point, coco[\center])
			<= coco[\heartRadius]) {
			^this.prToggle(("coco" ++ n ++ "Hold").asSymbol)
		};
		if(CQNightGeometry.markHit(coco[\chevron], point)) {
			^this.prToggle(("coco" ++ n ++ "Flip").asSymbol)
		};
		// skip's own stationary mark above the mouth jack. It replaced the tap
		// on the read-head comet at the G2 gate: the comet is the only target
		// in the night that moves, and past a crawl it cannot be hit at all.
		if(CQNightGeometry.markHit(coco[\skipMark], point)) {
			^Cocoquantus.skip(n)
		};
		if(CQNightGeometry.spiralDistance(coco, point) <= 8) {
			wind = (index: index,
				angle: this.class.angleAt(coco[\center], point));
			^this
		};
		// the body solo answers on the rim the hover cue drew, nowhere else
		^this.forwardDown(view, x, y, modifiers, button, clicks,
			if(CQNightGeometry.shellRimHit(coco, point)) { body })
	}

	shellMove { |index, view, x, y, modifiers = 0|
		var point, coco, angle, delta, name;
		if(wind.isNil) { ^this.forwardMove(view, x, y, modifiers) };
		point = this.prPoint(view, x, y);
		coco = map.geometry[\cocos][wind[\index]];
		angle = this.class.angleAt(coco[\center], point);
		delta = this.class.angleDelta(wind[\angle], angle);
		wind[\angle] = angle;
		name = ("coco" ++ (wind[\index] + 1) ++ "Speed").asSymbol;
		this.prNudge(name, delta * coco[\winding] * this.class.windSensitivity);
		// gesture-rate feedback — the reel pearl slides under the finger,
		// painted from the funnel truth ahead of the paced notification
		map.paintSpeed(wind[\index], this.prNorm(name));
		^this
	}

	shellUp { |index, view, x, y, modifiers = 0, button = 0|
		if(wind.notNil) {
			wind = nil;
			^this
		};
		^this.forwardUp(view, x, y, modifiers, button)
	}

	// ---- ring: the eye (qFreq vertical, qChaos horizontal) ----

	ringDown { |view, x, y, modifiers = 0, button = 0, clicks = 1|
		var point = this.prPoint(view, x, y), geo = map.geometry;
		if(geo.isNil) { ^this };
		// the eye owns its own area at every click count (§4.5); the water
		// inside the ring belongs to the ring body — the night home of the Day
		// panel's own `quantussy` solo, so both stages share one solo state
		if(button == 0 and: { modifiers.isAlt.not }
			and: { overlay.patchAt(point).isNil }
			and: { CQNightGeometry.distance(point, geo[\eye]) <= 12 }) {
			eyeDrag = (start: point,
				freq: this.prNorm(\qFreq), chaos: this.prNorm(\qChaos));
			map.eyeActive = true;
			^this
		};
		^this.forwardDown(view, x, y, modifiers, button, clicks,
			if(CQNightGeometry.ringBodyHit(geo, point)) { \quantussy })
	}

	ringMove { |view, x, y, modifiers = 0|
		var point, sens;
		if(eyeDrag.isNil) { ^this.forwardMove(view, x, y, modifiers) };
		point = this.prPoint(view, x, y);
		sens = this.class.eyeSensitivity;
		this.prSet(\qFreq,
			eyeDrag[\freq] + ((eyeDrag[\start].y - point.y) * sens));
		this.prSet(\qChaos,
			eyeDrag[\chaos] + ((point.x - eyeDrag[\start].x) * sens));
		// gesture-rate feedback: the storm iris answers the hand directly,
		// painted from the funnel truth ahead of the paced notification
		map.paintEye(this.prNorm(\qFreq), this.prNorm(\qChaos));
		^this
	}

	ringUp { |view, x, y, modifiers = 0, button = 0|
		if(eyeDrag.notNil) {
			eyeDrag = nil;
			map.eyeActive = false;
			^this
		};
		^this.forwardUp(view, x, y, modifiers, button)
	}

	// ---- castle: a tap on the spesal dial = one step ----

	castleDown { |k, view, x, y, modifiers = 0, button = 0, clicks = 1|
		var point = this.prPoint(view, x, y), geo = map.geometry;
		if(geo.isNil) { ^this };
		// the inner clock owns its area at every click count (§4.5), so two fast
		// taps are two steps — never one step plus a body solo behind it
		if(button == 0 and: { modifiers.isAlt.not }
			and: { overlay.patchAt(point).isNil }
			and: { CQNightGeometry.dialHit(geo[\castles][k], point) }) {
			Cocoquantus.spesal(k + 1);
			^this
		};
		// the body solo answers exactly where the hover echo promised it would
		// (§4.5): the flesh plus its margin, not the whole layer box
		^this.forwardDown(view, x, y, modifiers, button, clicks,
			if(CQNightGeometry.bodyHit(geo[\castles][k], point)) {
				("castle" ++ (k + 1)).asSymbol
			})
	}

	// ---- the almanac corner: the sowing moon ----

	almanacDown { |view, x, y, modifiers = 0, button = 0, clicks = 1|
		var point = this.prPoint(view, x, y), geo = map.geometry;
		if(geo.isNil) { ^this };
		// §4.5 again: a cable crossing the sky is grabbed, never sown through
		if(button != 0 or: { modifiers.isAlt }
			or: { overlay.patchAt(point).notNil }) {
			^this.forwardDown(view, x, y, modifiers, button, clicks, nil)
		};
		if(CQNightGeometry.moonHit(geo[\almanac], point)) {
			moonDrag = (start: point, amount: map.shakeAmount, travel: 0);
			^this
		};
		// the rest of the corner is open water: harvest, solo and hover behave
		// exactly as they do on any other night paper
		^this.forwardDown(view, x, y, modifiers, button, clicks, nil)
	}

	almanacMove { |view, x, y, modifiers = 0|
		var point;
		if(moonDrag.isNil) { ^this.forwardMove(view, x, y, modifiers) };
		point = this.prPoint(view, x, y);
		moonDrag[\travel] = max(moonDrag[\travel],
			CQNightGeometry.distance(moonDrag[\start], point));
		if(moonDrag[\travel] > this.class.tapTravel) {
			// pouring the phase: the terminator follows the hand, so the drag is
			// horizontal — right for a fuller moon — at the tide crescents' own
			// sensitivity (§16.6, decided 2026-07-22)
			map.pourShake(moonDrag[\amount]
				+ (this.class.deadzone(point.x - moonDrag[\start].x,
					this.class.tapTravel) * this.class.crescentSensitivity));
		};
		^this
	}

	almanacUp { |view, x, y, modifiers = 0, button = 0|
		if(moonDrag.isNil) { ^this.forwardUp(view, x, y, modifiers, button) };
		if(moonDrag[\travel] <= this.class.tapTravel) { map.throwShake };
		moonDrag = nil;
		^this
	}

	// ---- crescents: dedicated small controls, vertical drag = value ----

	crescentDown { |name, view, x, y, modifiers = 0, button = 0, clicks = 1|
		if(button != 0) { ^this };
		crescentDrag = (name: name, startY: y, start: this.prNorm(name));
		map.crescentActive(name, true);
		^this
	}

	crescentMove { |name, view, x, y, modifiers = 0|
		if(crescentDrag.isNil or: { crescentDrag[\name] != name }) { ^this };
		this.prSet(name, crescentDrag[\start]
			+ ((crescentDrag[\startY] - y) * this.class.crescentSensitivity));
		^this
	}

	crescentUp { |name, view, x, y, modifiers = 0, button = 0|
		if(crescentDrag.notNil) {
			crescentDrag = nil;
			map.crescentActive(name, false);
		};
		^this
	}

	// ---- private ----

	prPoint { |view, x, y|
		^view.mapToGlobal(Point(x, y)) - map.container.mapToGlobal(0@0)
	}

	prNorm { |name| ^CQSpec.unmap(name, state.get(name)) }

	prSet { |name, norm|
		state.set(name, CQSpec.map(name, norm.clip(0, 1)));
		^this
	}

	prNudge { |name, deltaNorm|
		^this.prSet(name, this.prNorm(name) + deltaNorm)
	}

	prToggle { |name|
		state.set(name, 1 - ((state.get(name) ? 0) > 0.5).binaryValue);
		^this
	}

	// bodyName carries the pressed body's §6 solo identity down to the shared
	// ground grammar; everything else about the press behaves as on Day paper
	forwardDown { |view, x, y, modifiers, button, clicks, bodyName|
		controller !? {
			controller.groundDown(view, x, y, modifiers, button, clicks, bodyName)
		};
		^this
	}

	forwardMove { |view, x, y, modifiers|
		controller !? { controller.groundMove(view, x, y, modifiers) };
		^this
	}

	forwardUp { |view, x, y, modifiers, button|
		controller !? { controller.groundUp(view, x, y, modifiers, button) };
		^this
	}

	forwardWheel { |view, x, y, modifiers, delta|
		controller !? { controller.groundWheel(view, x, y, modifiers, delta) };
		^this
	}
}
