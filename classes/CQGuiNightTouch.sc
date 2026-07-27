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
	var wind, punch, eyeDrag, crescentDrag, moonDrag;
	var compassDrag, weatherHold;

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

	// The moon chooses one act only after the shared 8 px threshold. Once the
	// caller stores this answer it never asks again, so diagonal drift cannot
	// switch rain into phase halfway through a gesture.
	*moonAxis { |delta|
		if(CQNightGeometry.distance(Point(0, 0), delta) <= this.tapTravel) {
			^nil
		};
		if(delta.x.abs >= delta.y.abs) { ^\phase };
		^if(delta.y > 0) { \grow } { \wither }
	}

	// Open paper stays inert until the bay is deliberately focused by Shift,
	// Tab or its gill. Inside focus left pours and right is elastic; outside
	// focus right remains the learned harvest lasso.
	*tideMode { |button, focused = false|
		if(focused and: { button == 0 }) { ^\blend };
		if(focused and: { button == 1 }) { ^\elastic };
		^nil
	}

	// §7.1 compass writes: the law's ceiling is 60 Hz; 30 is chosen because the
	// weights are unlagged control buses (§15 — a banana contact is a step too),
	// so faster stepping buys no audible smoothness, only server traffic
	*compassWritePeriod { ^1/30 }

	// §7.2 grow/wither repeat while held
	// The first 8 px choose the act; the next 32 px tighten it from a drizzle
	// or mild sun to its deliberate performance rate. Rain is slower because
	// every drop births a whole cable. Sun runs faster because one ray only
	// thins a random cable by x0.7.
	*weatherRateTravel { ^32 }
	*rainRateMin { ^0.6 }
	*rainRateMax { ^1.6 }
	*sunRateMin { ^2.5 }
	*sunRateMax { ^6.0 }
	*weatherClockPeriod { ^1/20 }

	*weatherIntensity { |delta|
		^((delta.y.abs - this.tapTravel) / this.weatherRateTravel).clip(0, 1)
	}

	*weatherRate { |mark, intensity = 0|
		var t = (intensity ? 0).asFloat.clip(0, 1);
		var lo = if(mark == \grow) { this.rainRateMin } { this.sunRateMin };
		var hi = if(mark == \grow) { this.rainRateMax } { this.sunRateMax };
		^lo + ((hi - lo) * t)
	}

	// One enum stepper for the range ripples and the later dolby oysters.
	// Membership and order come only from CQSpec; malformed current values are
	// clamped by the same authority before the next choice is selected.
	*nextEnum { |name, current|
		var values = CQSpec.enumValues(name), clamped, index;
		if(values.isNil or: { values.isEmpty }) { ^nil };
		clamped = CQSpec.clamp(name, current);
		index = values.indexOf(clamped) ? 0;
		^values.wrapAt(index + 1)
	}

	// §16.9 the elastic return's short glide, ~3 frames
	*elasticFrames { ^3 }

	// ---- shell: wind (speed) · heart (hold) · chevron (flip) · mark (skip) ----

	shellDown { |index, view, x, y, modifiers = 0, button = 0, clicks = 1|
		var point = this.prPoint(view, x, y), geo = map.geometry, coco, n;
		var body = ("coco" ++ (index + 1)).asSymbol;
		if(geo.isNil) { ^this };
		coco = geo[\cocos][index];
		n = index + 1;
		// §4.5 controls first, at every click count: a cable inside its hit
		// target is grabbed (and a right press on it rips), and Alt is the
		// ground grammar's polarity flip. This precedes the punch on purpose —
		// a right press on a cable crossing the heart must still rip (§2), never
		// punch. Shell paper between the turns carries a double-click to the body.
		if(modifiers.isAlt or: { overlay.patchAt(point).notNil }) {
			^this.forwardDown(view, x, y, modifiers, button, clicks, body)
		};
		// the shell's heart. Left tap: seal / crack the loop (hold toggle).
		// Right press held: the PUNCH — momentarily invert the hold, restored on
		// release and on every exit path (§2). Checked before the generic
		// non-left forward so a right press on the heart can never fall through
		// to a harvest/rip on the shell body.
		if(CQNightGeometry.distance(point, coco[\center])
			<= coco[\heartRadius]) {
			if(button == 0) { ^this.prToggle(("coco" ++ n ++ "Hold").asSymbol) };
			if(button == 1) { ^this.prPunchDown(index) };
		};
		// every other non-left button falls to the ground grammar exactly as
		// before: harvest on empty shell paper, rip, solo — carrying the body's
		// §6 solo identity down
		if(button != 0) {
			if(button == 1 and: { modifiers.isShift }
				and: { CQNightGeometry.compassHit(geo[\almanac], point) }
				and: { CQNightGeometry.shellRimHit(coco, point).not }) {
				^this.compassDown(view, x, y, modifiers, button, clicks)
			};
			^this.forwardDown(view, x, y, modifiers, button, clicks, body)
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
		if(CQNightGeometry.spiralDistance(coco, point)
			<= CQNightMeasures.windGrab) {
			wind = (index: index,
				angle: this.class.angleAt(coco[\center], point));
			^this
		};
		// The drawn rim is body. Empty paper inside this large opaque shell
		// view is still bay water, so it must reach the tide instead of being
		// lost merely because Qt's rectangle is larger than the organism.
		if(CQNightGeometry.shellRimHit(coco, point)) {
			^this.forwardDown(view, x, y, modifiers, button, clicks, body)
		};
		^this.compassDown(view, x, y, modifiers, button, clicks)
	}

	shellMove { |index, view, x, y, modifiers = 0|
		var point, coco, angle, delta, name;
		if(compassDrag.notNil) { ^this.compassMove(view, x, y, modifiers) };
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
		if(compassDrag.notNil) {
			^this.compassUp(view, x, y, modifiers, button)
		};
		// mouse-up is the punch's natural release. Qt grabs the press, so the up
		// always arrives at the shell view that began it — but restore on *any*
		// shell up regardless of index/button, so a stray up can never leave a
		// loop recording (§2). It is the first thing checked, ahead of the wind.
		if(punch.notNil) { ^this.cancelPunch };
		if(wind.notNil) {
			wind = nil;
			^this
		};
		^this.forwardUp(view, x, y, modifiers, button)
	}

	// ---- the punch (§2): a momentary hold invert, held while the right button
	// is down on the heart. Capture the exact prior value and invert it; every
	// exit path restores precisely that captured value (never a re-toggle, which
	// could drift under multi-button chording) — see cancelPunch.
	prPunchDown { |index|
		var name = ("coco" ++ (index + 1) ++ "Hold").asSymbol;
		var before = (state.get(name) ? 0);
		punch = (index: index, name: name, before: before);
		state.set(name, 1 - before);
		^this
	}

	// idempotent: a nil punch is a no-op, so calling this from several exit paths
	// (the mouse-up, plus the shared cancel choke point reached by Esc, Tab,
	// panic, window close, GUI free and CmdPeriod) can never double-restore
	cancelPunch {
		var held = punch;
		if(held.isNil) { ^this };
		punch = nil;
		state.set(held[\name], held[\before]);
		^this
	}

	// the night smoke's window onto the transient punch state (§5)
	punchActive { ^punch.notNil }

	// ---- ring: the eye (qFreq vertical, qChaos horizontal) ----

	ringDown { |view, x, y, modifiers = 0, button = 0, clicks = 1|
		var point = this.prPoint(view, x, y), geo = map.geometry;
		if(geo.isNil) { ^this };
		// the eye owns its own area at every click count (§4.5); the water
		// inside the ring belongs to the ring body — the night home of the Day
		// panel's own `quantussy` solo, so both stages share one solo state
		if(button == 0 and: { modifiers.isAlt.not }
			and: { overlay.patchAt(point).isNil }
			and: { CQNightGeometry.distance(point, geo[\eye])
				<= CQNightMeasures.eyeHit }) {
			eyeDrag = (start: point,
				freq: this.prNorm(\qFreq), chaos: this.prNorm(\qChaos));
			map.eyeActive = true;
			^this
		};
		if(CQNightGeometry.ringBodyHit(geo, point)) {
			^this.forwardDown(view, x, y, modifiers, button, clicks, \quantussy)
		};
		^this.compassDown(view, x, y, modifiers, button, clicks)
	}

	ringMove { |view, x, y, modifiers = 0|
		var point, sens;
		if(compassDrag.notNil) { ^this.compassMove(view, x, y, modifiers) };
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
		if(compassDrag.notNil) {
			^this.compassUp(view, x, y, modifiers, button)
		};
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
		var body = ("castle" ++ (k + 1)).asSymbol;
		var rangeName = ("c" ++ (k + 1) ++ "Range").asSymbol;
		if(geo.isNil) { ^this };
		// The ordinary priority law: cable, then the body's small controls,
		// then body solo / open-water ground.
		if(overlay.patchAt(point).notNil) {
			^this.forwardDown(view, x, y, modifiers, button, clicks, body)
		};
		// Alt belongs to the ground grammar's cable polarity, never to a small
		// control. Preserve the body's identity only where its drawn cue owns
		// the point; transparent castle paper remains ordinary water.
		if(modifiers.isAlt) {
			if(CQNightGeometry.bodyHit(geo[\castles][k], point)) {
				^this.forwardDown(view, x, y, modifiers, button, clicks, body)
			};
			^this.compassDown(view, x, y, modifiers, button, clicks)
		};
		if(button == 0
			and: { CQNightGeometry.rangeHit(geo[\castles][k], point) }) {
			^this.prCycleEnum(rangeName)
		};
		// the inner clock owns its area at every click count (§4.5), so two fast
		// taps are two steps — never one step plus a body solo behind it
		if(button == 0
			and: { CQNightGeometry.dialHit(geo[\castles][k], point) }) {
			Cocoquantus.spesal(k + 1);
			^this
		};
		// the body solo answers exactly where the hover echo promised it would
		// (§4.5): the flesh plus its margin, not the whole layer box
		if(CQNightGeometry.bodyHit(geo[\castles][k], point)) {
			^this.forwardDown(view, x, y, modifiers, button, clicks,
				body)
		};
		^this.compassDown(view, x, y, modifiers, button, clicks)
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
			moonDrag = (start: point, amount: map.shakeAmount,
				travel: 0, axis: nil);
			^this
		};
		^this.compassDown(view, x, y, modifiers, button, clicks)
	}

	almanacMove { |view, x, y, modifiers = 0|
		var point, delta, axis;
		if(compassDrag.notNil) { ^this.compassMove(view, x, y, modifiers) };
		if(moonDrag.isNil) { ^this.forwardMove(view, x, y, modifiers) };
		point = this.prPoint(view, x, y);
		delta = point - moonDrag[\start];
		moonDrag[\travel] = max(moonDrag[\travel],
			CQNightGeometry.distance(moonDrag[\start], point));
		if(moonDrag[\axis].isNil) {
			axis = this.class.moonAxis(delta);
			axis !? {
				moonDrag[\axis] = axis;
				if(#[grow, wither].includes(axis)) {
					this.beginWeatherHold(axis, delta)
				};
			};
		};
		if(weatherHold.notNil) { this.updateWeatherHold(delta) };
		if(moonDrag[\axis] == \phase) {
			map.pourShake(moonDrag[\amount]
				+ (this.class.deadzone(delta.x,
					this.class.tapTravel) * this.class.crescentSensitivity));
		};
		^this
	}

	almanacUp { |view, x, y, modifiers = 0, button = 0|
		if(compassDrag.notNil) {
			^this.compassUp(view, x, y, modifiers, button)
		};
		if(weatherHold.notNil) {
			this.endWeatherHold;
			moonDrag = nil;
			^this
		};
		if(moonDrag.isNil) { ^this.forwardUp(view, x, y, modifiers, button) };
		if(moonDrag[\travel] <= this.class.tapTravel) { map.throwShake };
		moonDrag = nil;
		^this
	}

	// ---- the compass water (§7.1): voyages, seeds, weather ----

	compassDown { |view, x, y, modifiers = 0, button = 0, clicks = 1|
		var point = this.prPoint(view, x, y), geo = map.geometry, almanac, seed;
		var mode;
		if(geo.isNil) { ^this };
		almanac = geo[\almanac];
		if(modifiers.isShift) { map.tideFocusGesture_(true) };
		// §4.5: a cable crossing the water is grabbed, never blended through
		if(modifiers.isAlt or: { overlay.patchAt(point).notNil }) {
			map.finishTideFocus(modifiers.isShift);
			^this.forwardDown(view, x, y, modifiers, button, clicks, nil)
		};
		seed = CQNightGeometry.stampHit(almanac, point);
		if(seed.notNil) {
			if(button == 1) { ^map.clearCorner(seed) };
			if(button != 0) { ^this };
			compassDrag = (mode: \seed, start: point, travel: 0, seed: seed,
				overwrite: modifiers.isShift,
				focusWrite: map.tideFocusLatched);
			^this
		};
		if(CQNightGeometry.compassHit(almanac, point)) {
			mode = this.class.tideMode(button, map.tideFocus);
			if(mode.isNil) {
				map.finishTideFocus(modifiers.isShift);
				^this.forwardDown(view, x, y, modifiers, button, clicks, nil)
			};
			compassDrag = (mode: mode,
				start: point, travel: 0, before: map.matrixCopy);
			^this
		};
		map.finishTideFocus(modifiers.isShift);
		^this.forwardDown(view, x, y, modifiers, button, clicks, nil)
	}

	// The gill and Tab toggle one persistent focus state. While closed, a cable
	// crossing the organ keeps §4.5 priority; while open, cable spans are below
	// the water and the gill remains available to close the field.
	tideGateDown { |view, x, y, modifiers = 0, button = 0, clicks = 1|
		var point = this.prPoint(view, x, y);
		if(button == 0 and: { clicks == 1 }
			and: { CQNightGeometry.tideGateHit(map.geometry, point) }
			and: { overlay.patchAt(point).isNil }) {
			map.toggleTideFocus;
			^this
		};
		^this.forwardDown(view, x, y, modifiers, button, clicks, nil)
	}

	compassMove { |view, x, y, modifiers = 0|
		var point;
		if(compassDrag.isNil) { ^this.forwardMove(view, x, y, modifiers) };
		point = this.prPoint(view, x, y);
		compassDrag[\travel] = max(compassDrag[\travel],
			CQNightGeometry.distance(compassDrag[\start], point));
		if(compassDrag[\mode] == \seed) {
			if(compassDrag[\travel] > this.class.tapTravel
				and: { map.cornerStamped(compassDrag[\seed]) }) {
				map.previewBreed(compassDrag[\seed], point)
			};
			^this
		};
		if(compassDrag[\travel] > this.class.tapTravel) {
			map.pourBlend(
				CQNightGeometry.compassNorm(map.geometry[\almanac], point));
		};
		^this
	}

	compassUp { |view, x, y, modifiers = 0, button = 0|
		var drag = compassDrag, point, norm, seed;
		if(weatherHold.notNil) { ^this.endWeatherHold };
		if(drag.isNil) { ^this.forwardUp(view, x, y, modifiers, button) };
		compassDrag = nil;
		point = this.prPoint(view, x, y);
		if(drag[\mode] == \seed) {
			if(drag[\travel] <= this.class.tapTravel) {
				map.seedTap(drag[\seed], drag[\overwrite],
					drag[\focusWrite]);
			} {
				seed = CQNightGeometry.stampHit(map.geometry[\almanac], point);
				if(seed.notNil and: { seed != drag[\seed] }
					and: { map.cornerStamped(seed) }) {
					map.breedCorners(drag[\seed], seed)
				} {
					map.clearVoyage
				};
			};
			map.finishTideFocus(modifiers.isShift);
			^this
		};
		norm = CQNightGeometry.compassNorm(map.geometry[\almanac], point);
		if(drag[\travel] <= this.class.tapTravel) {
			map.clearVoyage;
			map.finishTideFocus(modifiers.isShift);
			^this
		};
		if(drag[\mode] == \blend) {
			map.commitBlend(norm, drag[\before])
		} {
			map.elasticReturn(drag[\before])
		};
		map.finishTideFocus(modifiers.isShift);
		^this
	}

	// Public aliases name the new landscape without breaking scripts that still
	// address the former compass surface during this branch.
	tideDown { |view, x, y, modifiers = 0, button = 0, clicks = 1|
		^this.compassDown(view, x, y, modifiers, button, clicks)
	}

	tideMove { |view, x, y, modifiers = 0|
		^this.compassMove(view, x, y, modifiers)
	}

	tideUp { |view, x, y, modifiers = 0, button = 0|
		^this.compassUp(view, x, y, modifiers, button)
	}

	tideActive { ^compassDrag.notNil }

	landscapeMove { |view, x, y, modifiers = 0|
		if(compassDrag.notNil) { ^this.compassMove(view, x, y, modifiers) };
		^this.forwardMove(view, x, y, modifiers)
	}

	landscapeUp { |view, x, y, modifiers = 0, button = 0|
		if(compassDrag.notNil) {
			^this.compassUp(view, x, y, modifiers, button)
		};
		^this.forwardUp(view, x, y, modifiers, button)
	}

	// grow/wither: press = the first act lands at once; the hold repeats at the
	// pull's visible weather tension. One hold = one undo op at release.
	beginWeatherHold { |mark, delta|
		var intensity = this.class.weatherIntensity(delta ? Point(0, 0));
		weatherHold = (mark: mark, before: map.matrixCopy, landed: false,
			intensity: intensity, phase: 0.0);
		map.weatherGesture(mark, intensity);
		this.prWeatherStep;
		this.prScheduleWeather;
		^this
	}

	updateWeatherHold { |delta|
		weatherHold !? { |hold|
			var next = this.class.weatherIntensity(delta);
			if((next - hold[\intensity]).abs > 0.01) {
				hold[\intensity] = next;
				map.weatherGesture(hold[\mark], next);
			};
		};
		^this
	}

	prScheduleWeather {
		var hold = weatherHold;
		var period = this.class.weatherClockPeriod;
		AppClock.sched(period, {
			var rate, phase;
			if(weatherHold === hold) {
				rate = this.class.weatherRate(hold[\mark], hold[\intensity]);
				phase = hold[\phase] + (rate * period);
				if(phase >= 1) {
					phase = phase - 1;
					this.prWeatherStep;
				};
				hold[\phase] = phase;
				this.prScheduleWeather;
			};
			nil
		});
		^this
	}

	prWeatherStep {
		weatherHold !? { |hold|
			if(map.weatherStep(hold[\mark])) { hold[\landed] = true };
		};
		^this
	}

	endWeatherHold {
		var hold = weatherHold;
		weatherHold = nil;
		map.weatherGesture(nil, 0);
		hold !? { map.commitWeather(hold[\mark], hold[\before], hold[\landed]) };
		^this
	}

	// every path that takes the hand away mid-gesture (Esc, Tab, panic, free):
	// an uncommitted voyage goes home like the elastic, a held rain commits
	// what already landed (the seeds fell audibly; Ctrl+Z takes them back as
	// one op), and an armed graft is forgotten
	cancelWeather {
		var drag = compassDrag;
		compassDrag = nil;
		moonDrag = nil;
		if(drag.notNil
			and: { #[blend, elastic].includes(drag[\mode]) }
			and: { drag[\travel] > this.class.tapTravel }) {
			map.elasticReturn(drag[\before]);
		} {
			if(drag.notNil) { map.clearVoyage };
		};
		if(weatherHold.notNil) { this.endWeatherHold };
		map.finishTideFocus(false);
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

	prCycleEnum { |name|
		var next = this.class.nextEnum(name, state.get(name));
		next !? { state.set(name, next) };
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
