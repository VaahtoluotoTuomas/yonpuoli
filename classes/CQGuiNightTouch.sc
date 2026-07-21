// CQGuiNightTouch — the §6.1 night touch grammar: winding the shells, the
// hold hearts, the flip chevrons, the read-head skip taps, the tide
// crescents, the ring's eye and the spesal dial taps. Owns only transient
// gesture state; every write goes through the CQState funnel (params) or the
// Cocoquantus facade (spesal step, skip), exactly like the Day widgets. On every press the cable layer is
// consulted first (§4.5) and everything unclaimed falls through to the shared
// banana controller's ground grammar.

CQGuiNightTouch {
	var map, state, overlay;
	var <>controller;   // set after construction — the controller binds later
	var wind, eyeDrag, crescentDrag;

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
	// a press lands on the read head inside this radius of its drawn home;
	// a drag past the slop stops being a tap and becomes a wind
	*sparkRadius { ^9 }
	*skipSlop { ^4 }

	// ---- shell: wind (speed) · heart (hold) · chevron (flip) ----

	shellDown { |index, view, x, y, modifiers = 0, button = 0, clicks = 1|
		var point = this.prPoint(view, x, y), geo = map.geometry, coco, n, spark;
		if(geo.isNil) { ^this };
		if(button != 0 or: { clicks > 1 } or: { modifiers.isAlt }
			or: { overlay.patchAt(point).notNil }) {
			^this.forwardDown(view, x, y, modifiers, button, clicks)
		};
		coco = geo[\cocos][index];
		n = index + 1;
		if(CQNightGeometry.distance(point, coco[\center])
			<= coco[\heartRadius]) {
			^this.prToggle(("coco" ++ n ++ "Hold").asSymbol)
		};
		if(Rect.aboutPoint(coco[\chevron], 10, 10).containsPoint(point)) {
			^this.prToggle(("coco" ++ n ++ "Flip").asSymbol)
		};
		// a press on the read head arms a skip tap; the same press still
		// becomes a wind the moment the hand truly moves (skipSlop)
		spark = map.sparkPoint(index);
		if(spark.notNil and: {
			CQNightGeometry.distance(point, spark) <= this.class.sparkRadius
		}) {
			wind = (index: index, skipArm: point,
				angle: this.class.angleAt(coco[\center], point));
			^this
		};
		if(CQNightGeometry.spiralDistance(coco, point) <= 8) {
			wind = (index: index,
				angle: this.class.angleAt(coco[\center], point));
			^this
		};
		^this.forwardDown(view, x, y, modifiers, button, clicks)
	}

	shellMove { |index, view, x, y, modifiers = 0|
		var point, coco, angle, delta, name;
		if(wind.isNil) { ^this.forwardMove(view, x, y, modifiers) };
		point = this.prPoint(view, x, y);
		coco = map.geometry[\cocos][wind[\index]];
		if(wind[\skipArm].notNil) {
			// still a tap inside the slop — a tap must never nudge the speed
			if(CQNightGeometry.distance(point, wind[\skipArm])
				<= this.class.skipSlop) { ^this };
			wind[\skipArm] = nil;
			wind[\angle] = this.class.angleAt(coco[\center], point);
			^this
		};
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
			if(wind[\skipArm].notNil) {
				// the tap on the read head — skip's night home (§6.1)
				Cocoquantus.skip(wind[\index] + 1);
			};
			wind = nil;
			^this
		};
		^this.forwardUp(view, x, y, modifiers, button)
	}

	// ---- ring: the eye (qFreq vertical, qChaos horizontal) ----

	ringDown { |view, x, y, modifiers = 0, button = 0, clicks = 1|
		var point = this.prPoint(view, x, y), geo = map.geometry;
		if(geo.isNil) { ^this };
		if(button == 0 and: { clicks == 1 } and: { modifiers.isAlt.not }
			and: { overlay.patchAt(point).isNil }
			and: { CQNightGeometry.distance(point, geo[\eye]) <= 12 }) {
			eyeDrag = (start: point,
				freq: this.prNorm(\qFreq), chaos: this.prNorm(\qChaos));
			map.eyeActive = true;
			^this
		};
		^this.forwardDown(view, x, y, modifiers, button, clicks)
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
		if(button == 0 and: { clicks == 1 } and: { modifiers.isAlt.not }
			and: { overlay.patchAt(point).isNil }
			and: { CQNightGeometry.dialHit(geo[\castles][k], point) }) {
			Cocoquantus.spesal(k + 1);
			^this
		};
		^this.forwardDown(view, x, y, modifiers, button, clicks)
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

	forwardDown { |view, x, y, modifiers, button, clicks|
		controller !? {
			controller.groundDown(view, x, y, modifiers, button, clicks, nil)
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
