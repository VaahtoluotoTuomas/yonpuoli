// CQGuiNightMap — the Night stage (GUI_SPEC §6, §16.6): a topology-true
// archipelago carrying the same 16 jacks, 22 sockets, cable overlay and banana
// grammar as Day, plus the §6.1 night touch grammar (CQGuiNightTouch). Pure
// view: geometry from CQNightGeometry, pixels from CQInk/CQInkNight, every
// write through the shared grammar and funnels. The layers are opaque-backed
// (ground → ring → eye → castles → shells → birds → endpoints → crescents →
// labels, the hit-transparent overlay on top) so an ambient repaint touches
// only the views whose information actually moved (§10) — the storm iris in
// particular repaints without re-inking the ring.

CQGuiNightMap {
	var gui, state, matrix, config;
	var <stageView, <container, <overlay, <controller, <touch, <endpoints, <bodies;
	var <groundView, <ringView, <eyeView, <castleViews, <shellViews, <birdViews;
	var <crescentViews, crescentNorms, activeCrescents, hoverCrescents, labels;
	var <geometry, <eyeActive = false;
	var <eyeFreqNorm = 0.5, <eyeChaosNorm = 0.15;
	var <speedNorms, <shellHover, <dialHover, <bodyHover, <rimHover, <ringHover = false;
	var <sparkDirs, <sparkHover, sparkPainted, flipPainted;
	var <fireflyPhase = 0.0, lastFireflyTime, castlePainted;
	var <holdStates, <flipStates, <rangeLetters, <sproutState;
	var freed = false;

	*new { |gui, state, matrix, config|
		^super.newCopyArgs(gui, state, matrix, config).prBuild
	}

	// same families, glyphs and lamps as the Day skin — one learned shape
	// (§16.7); decorations are the night homes of §3.1 (leaf, crown, fruit
	// stem — the mouth chevron lives on the shell as a §6.1 touch).
	*endpointSpec { |name|
		var string = name.asString, prefix = string.keep(3).asSymbol;
		if(CQMatrix.srcIndex(name).notNil) {
			^(kind: \jack,
				family: (tri: \tri, cas: \cas, env: \input,
					coc: \cas, org: \organ)[prefix] ? \line,
				decoration: (tri: \leaf, cas: \crown, org: \stem,
					env: \root)[prefix])
		};
		if(CQMatrix.destIndex(name).isNil) { ^nil };
		if(string.endsWith("_spaf")) { ^(kind: \socket, family: \spaf) };
		if(string.endsWith("Vca")) { ^(kind: \socket, family: \line, glyph: \wedge) };
		if(string.endsWith("_flip")) {
			^(kind: \socket, family: \line, glyph: \flip, lamp: true)
		};
		if(string.endsWith("_skip")) {
			^(kind: \socket, family: \line, glyph: \spark, lamp: true)
		};
		if(string.endsWith("_fm")) { ^(kind: \socket, family: \fm, glyph: \wave) };
		if(string.endsWith("_spesal")) {
			^(kind: \socket, family: \input, glyph: \spark, lamp: true)
		};
		^(kind: \socket, family: \line, glyph: \pendulum)
	}

	// §6 solo-light bodies at night — the anatomy's own groupings (§16.6), and
	// deliberately the Day panel's own body names where the grouping is the
	// same organ, so one shared grammar cannot hold two solos for one body.
	// The castles are the night's own finer bodies. Roots and birds carry
	// exactly one endpoint each: their jack/socket solo already lights the very
	// same cables, so a body there would be a second name for one act.
	*bodyNames {
		^#[castle1, castle2, castle3, castle4, castle5, coco1, coco2, quantussy]
	}

	*bodyEndpointNames { |name|
		var string = name.asString, n;
		if(name == \quantussy) {
			^CQMatrix.sourceNames.copyRange(0, 9)
				++ CQMatrix.destNames.copyRange(10, 19)
		};
		n = string.last.asString;
		if(string.beginsWith("castle")) {
			^[("tri" ++ n).asSymbol, ("cas" ++ n).asSymbol,
				("castle" ++ n ++ "_fm").asSymbol,
				("castle" ++ n ++ "_spesal").asSymbol]
		};
		if(string.beginsWith("coco")) {
			^[("coco" ++ n).asSymbol, ("org" ++ n).asSymbol,
				("coco" ++ n ++ "_spaf").asSymbol,
				("coco" ++ n ++ "_inVca").asSymbol,
				("coco" ++ n ++ "_fbVca").asSymbol,
				("coco" ++ n ++ "_flip").asSymbol,
				("coco" ++ n ++ "_skip").asSymbol]
		};
		^[]
	}

	// §6.1 crescent inventory: three continuous coco moons per shell plus one
	// deck level under each wing. Pure lookup, unit-testable without Qt.
	*crescentNames { |cocoIndex|
		^switch(cocoIndex,
			0, { #[coco1Spaf, coco1In, coco1Fb] },
			1, { #[coco2Spaf, coco2In, coco2Fb] },
			{ #[deckALevel, deckBLevel] }
		)
	}

	free {
		if(freed) { ^this };
		freed = true;
		controller !? { controller.free };
		overlay !? { overlay.free };
	}

	// castles read the spesal dial state from CQGui's /cq_spesal cache
	spesalChanged {
		if(freed) { ^this };
		castleViews.do { |view| view.refresh };
	}

	eyeActive_ { |flag|
		eyeActive = flag == true;
		if(freed.not) { eyeView.refresh };
	}

	// gesture-time paint-cache pushes (§10: 60 Hz only during the drag): the
	// touch machine reads the funnel truth back and paints it directly, ahead
	// of the paced notification that confirms the same value at flush time
	paintSpeed { |i, norm|
		speedNorms[i] = norm;
		if(freed.not) { shellViews[i].refresh };
	}

	paintEye { |freqNorm, chaosNorm|
		eyeFreqNorm = freqNorm;
		eyeChaosNorm = chaosNorm;
		if(freed.not) { eyeView.refresh };
	}

	// one '/cq_phase' frame (CQGui caches the funnel truth in phaseNorms; the
	// 5 Hz send rate is the repaint ceiling, §10). The comet's direction is
	// read from the phase's actual travel — wrap-aware — so a matrix-driven
	// flip shows truthfully even when the chevron's param says otherwise.
	// A move under ~1.5 px along the line keeps the frame.
	paintPhase { |i|
		var norm = gui.phaseNorms[i], mean = gui.phaseDirs[i];
		var delta, mode, repaint = false;
		if(freed) { ^this };
		// the chevron's regime can change while the address barely moves — a
		// shuttled loop goes nowhere — so it gets its own gate
		mode = this.flipMode(i);
		if(mode != flipPainted[i]) { flipPainted[i] = mode; repaint = true };
		// the tail shares the chevron's one truth wherever the server reports a
		// settled direction; the wrap-aware travel stays the fallback
		if(mean.notNil and: { mean.abs > this.class.shuttleFloor }) {
			sparkDirs[i] = mean.sign
		};
		if(norm.notNil) {
			if(sparkPainted[i].isNil) {
				sparkPainted[i] = norm;
				repaint = true;
			} {
				delta = norm - sparkPainted[i];
				if(delta.abs > 0.5) { delta = delta - delta.sign };  // the mouth seam
				if(delta.abs >= 0.002) {
					if(mean.isNil and: { delta != 0 }) { sparkDirs[i] = delta.sign };
					sparkPainted[i] = norm;
					repaint = true;
				};
			};
		};
		if(repaint) { shellViews[i].refresh };
	}

	// the read head's stage-space home — nil until the server has reported
	sparkPoint { |i|
		var norm = gui.phaseNorms[i];
		if(geometry.isNil or: { norm.isNil }) { ^nil };
		^CQNightGeometry.spiralPoint(geometry[\cocos][i], norm)
	}

	// one gate/trig lamp whose charge actually moved (CQGui gates the §9 stream
	// and picks the visible stage; the hidden twin repaints at the hard cut)
	chargeChanged { |name|
		if(freed) { ^this };
		endpoints[name] !? { |endpoint| endpoint[\view].refresh };
	}

	crescentActive { |name, flag|
		if(flag == true) { activeCrescents.add(name) } {
			activeCrescents.remove(name)
		};
		if(freed.not) { crescentViews[name] !? { |view| view.refresh } };
	}

	// One /cq_aura frame that actually moved (CQGui gates the stream and the
	// visible view). Fireflies ride exactly these frames — §16.6 stillness
	// law: the river flows while the island sounds.
	auraFrame { |auraValues|
		var now;
		if(freed or: { geometry.isNil } or: { auraValues.isNil }) { ^this };
		now = Main.elapsedTime;
		fireflyPhase = (fireflyPhase
			+ (min(now - (lastFireflyTime ? now), 0.34) / 10)).mod(1);
		lastFireflyTime = now;
		ringView.refresh;
		CQMatrix.sourceNames.do { |src|
			endpoints[src] !? { |endpoint| endpoint[\view].refresh };
		};
		5.do { |k|
			var value = auraValues[5 + k] ? 0;   // cas1..cas5 canonical slots
			if(castlePainted[k].isNil
				or: { (value - castlePainted[k]).abs >= 0.04 }) {
				castlePainted[k] = value;
				castleViews[k].refresh;
			};
		};
		if(overlay.cableCount > 0 and: { overlay.auraPulses }) {
			overlay.view.refresh
		};
	}

	// ---- private ----

	prBuild {
		var night = CQInk.palette(\night), crescentControls;
		endpoints = IdentityDictionary.new;
		crescentViews = IdentityDictionary.new;
		crescentNorms = IdentityDictionary.new;
		activeCrescents = IdentitySet.new;
		hoverCrescents = IdentitySet.new;
		castlePainted = Array.newClear(5);
		holdStates = [false, false];
		flipStates = [false, false];
		speedNorms = [0.5, 0.5];
		shellHover = [false, false];
		rimHover = [false, false];
		dialHover = Array.fill(5, false);
		bodyHover = Array.fill(5, false);
		sparkDirs = [1, 1];
		sparkHover = [false, false];
		sparkPainted = Array.newClear(2);
		flipPainted = Array.newClear(2);
		rangeLetters = ["", "", "", "", ""];
		sproutState = CQNightGeometry.sproutState(config[\micChan].notNil, 0);
		container = View().background_(night[\ground]);
		groundView = this.prLayer;
		groundView.drawFunc = { this.prSafeDraw { this.prDrawGround } };
		ringView = this.prLayer;
		ringView.drawFunc = { this.prSafeDraw { this.prDrawRing } };
		eyeView = this.prLayer;
		eyeView.drawFunc = { this.prSafeDraw { this.prDrawEye } };
		castleViews = 5.collect { |k|
			var view = this.prLayer;
			view.drawFunc = { this.prSafeDraw { this.prDrawCastle(k) } };
			view
		};
		shellViews = 2.collect { |i|
			var view = this.prLayer;
			view.drawFunc = { this.prSafeDraw { this.prDrawShell(i) } };
			view
		};
		birdViews = 2.collect { |i|
			var view = this.prLayer;
			view.drawFunc = { this.prSafeDraw { this.prDrawBird(i) } };
			view
		};
		(CQMatrix.sourceNames ++ CQMatrix.destNames).do { |name|
			this.prMakeEndpoint(name)
		};
		(this.class.crescentNames(0) ++ this.class.crescentNames(1)
			++ this.class.crescentNames(nil)).do { |name|
			this.prMakeCrescent(name)
		};
		labels = #[deckA, deckB].collect { |which|
			var label = StaticText(container, Rect(0, 0, 160, 14))
				.string_("").align_(\center)
				.font_(CQInk.font(\number)).stringColor_(night[\whisper]);
			gui.setDeckLabel(which, label, night[\whisper]);
			label
		};
		overlay = CQGuiCableOverlay(matrix, endpoints, gui.bananaGrammar,
			{ |src| gui.auraFor(src) }).skin_(\night);
		touch = CQGuiNightTouch(this, state, overlay);
		this.prBindTouch;
		crescentControls = List.new;
		crescentViews.values.do { |view| crescentControls.add((view: view)) };
		// The night bodies own their own first gesture pass (§6.1), so they
		// register members only — no view for the controller to re-bind, or it
		// would overwrite the touch handlers installed just above.
		bodies = IdentityDictionary.new;
		this.class.bodyNames.do { |name|
			bodies[name] = (name: name, members: this.class.bodyEndpointNames(name))
		};
		controller = CQGuiBananaController(gui.bananaGrammar, endpoints,
			bodies, overlay, crescentControls);
		controller.bindGround(container);
		touch.controller = controller;
		this.prBindDrops;
		stageView = View().background_(night[\ground]).layout_(
			StackLayout(overlay.view, container).mode_(\stackAll));
		container.onResize = { this.prLayout };
		this.prRegisterUpdaters;
		{ this.prLayout }.defer;
		^this
	}

	prLayer {
		^UserView(container, Rect(0, 0, 4, 4))
			.background_(CQInk.palette(\night)[\ground])
	}

	prMakeEndpoint { |name|
		var spec = this.class.endpointSpec(name);
		var jack = spec[\kind] == \jack;
		var width = if(spec[\lamp] == true) { 34 } { if(jack) { 26 } { 20 } };
		var height = if(jack) { 26 } { 20 };
		var anchor = if(jack) { Point(13, 13) } { Point(9, 10) };
		var view = UserView(container, Rect(0, 0, width, height))
			.background_(CQInk.palette(\night)[\ground]);
		view.drawFunc = {
			this.prSafeDraw {
				spec[\decoration] !? { this.prDrawDecoration(name, spec, anchor) };
				gui.drawEndpoint(name, spec[\kind], anchor, spec[\family],
					spec[\glyph], spec[\lamp] == true, \night);
			}
		};
		endpoints[name] = (name: name, kind: spec[\kind], view: view,
			anchor: anchor, extent: Size(width, height));
	}

	prMakeCrescent { |name|
		var view = UserView(container, Rect(0, 0, 24, 24))
			.background_(CQInk.palette(\night)[\ground]);
		var family = if(name.asString.endsWith("Spaf")) { \spaf } { nil };
		view.drawFunc = {
			this.prSafeDraw {
				CQInkNight.drawCrescent(Point(12, 12), crescentNorms[name] ? 0,
					family, activeCrescents.includes(name)
						or: { hoverCrescents.includes(name) }, \night);
			}
		};
		view.mouseEnterAction = {
			hoverCrescents.add(name);
			view.refresh;
		};
		view.mouseLeaveAction = {
			hoverCrescents.remove(name);
			view.refresh;
		};
		crescentViews[name] = view;
	}

	// the §6.1 gesture pass sits on the touch bodies before the controller
	// binds the remaining paper; everything unclaimed falls through
	prBindTouch {
		shellViews.do { |view, i|
			view.mouseDownAction = { |v, x, y, mods, button, clicks|
				touch.shellDown(i, v, x, y, mods ? 0, button ? 0, clicks ? 1)
			};
			view.mouseMoveAction = { |v, x, y, mods|
				touch.shellMove(i, v, x, y, mods ? 0)
			};
			view.mouseUpAction = { |v, x, y, mods, button|
				touch.shellUp(i, v, x, y, mods ? 0, button ? 0)
			};
			view.mouseWheelAction = { |v, x, y, mods, xd, yd|
				touch.forwardWheel(v, x, y, mods ? 0, yd ? 0)
			};
		};
		// the eye's own opaque layer sits over the ring's center, so both
		// views speak the same ring grammar (prPoint maps via the container)
		[ringView, eyeView].do { |view|
			view.mouseDownAction = { |v, x, y, mods, button, clicks|
				touch.ringDown(v, x, y, mods ? 0, button ? 0, clicks ? 1)
			};
			view.mouseMoveAction = { |v, x, y, mods|
				touch.ringMove(v, x, y, mods ? 0)
			};
			view.mouseUpAction = { |v, x, y, mods, button|
				touch.ringUp(v, x, y, mods ? 0, button ? 0)
			};
			view.mouseWheelAction = { |v, x, y, mods, xd, yd|
				touch.forwardWheel(v, x, y, mods ? 0, yd ? 0)
			};
		};
		castleViews.do { |view, k|
			view.mouseDownAction = { |v, x, y, mods, button, clicks|
				touch.castleDown(k, v, x, y, mods ? 0, button ? 0, clicks ? 1)
			};
			view.mouseMoveAction = { |v, x, y, mods|
				touch.forwardMove(v, x, y, mods ? 0)
			};
			view.mouseUpAction = { |v, x, y, mods, button|
				touch.forwardUp(v, x, y, mods ? 0, button ? 0)
			};
			view.mouseWheelAction = { |v, x, y, mods, xd, yd|
				touch.forwardWheel(v, x, y, mods ? 0, yd ? 0)
			};
		};
		crescentViews.keysValuesDo { |name, view|
			view.mouseDownAction = { |v, x, y, mods, button, clicks|
				touch.crescentDown(name, v, x, y, mods ? 0, button ? 0, clicks ? 1)
			};
			view.mouseMoveAction = { |v, x, y, mods|
				touch.crescentMove(name, v, x, y, mods ? 0)
			};
			view.mouseUpAction = { |v, x, y, mods, button|
				touch.crescentUp(name, v, x, y, mods ? 0, button ? 0)
			};
		};
		// static hover cues (§4.5: hover never pulses — ink only, and never
		// where a cable would win the press). Cheap test first, patchAt only
		// on a candidate; a repaint only when the boolean flips.
		shellViews.do { |view, i|
			view.acceptsMouseOver = true;
			view.mouseOverAction = { |v, x, y| this.prHoverShell(i, v, x, y) };
			view.mouseLeaveAction = { this.prHoverShell(i, nil, nil, nil) };
		};
		castleViews.do { |view, k|
			view.acceptsMouseOver = true;
			view.mouseOverAction = { |v, x, y| this.prHoverCastle(k, v, x, y) };
			view.mouseLeaveAction = { this.prHoverCastle(k, nil, nil, nil) };
		};
		[ringView, eyeView].do { |view|
			view.acceptsMouseOver = true;
			view.mouseOverAction = { |v, x, y| this.prHoverRing(v, x, y) };
			view.mouseLeaveAction = { this.prHoverRing(nil, nil, nil) };
		};
	}

	prStagePoint { |view, x, y|
		^view.mapToGlobal(Point(x, y)) - container.mapToGlobal(0@0)
	}

	prHoverShell { |i, view, x, y|
		var near = false, headNear = false, rim = false, point, spark, free = true;
		if(geometry.notNil and: { view.notNil }) {
			point = this.prStagePoint(view, x, y);
			free = overlay.patchAt(point).isNil;
			near = (CQNightGeometry.spiralDistance(geometry[\cocos][i], point)
				<= 8) and: { free };
			spark = this.sparkPoint(i);
			headNear = near and: { spark.notNil } and: {
				CQNightGeometry.distance(point, spark)
					<= CQGuiNightTouch.sparkRadius };
			// the winding surface is consulted first, so the rim cue never
			// advertises a body solo where the next press would wind instead
			rim = free and: { near.not }
				and: { CQNightGeometry.shellRimHit(geometry[\cocos][i], point) };
		};
		if((near != shellHover[i]) or: { headNear != sparkHover[i] }
			or: { rim != rimHover[i] }) {
			shellHover[i] = near;
			sparkHover[i] = headNear;
			rimHover[i] = rim;
			if(freed.not) { shellViews[i].refresh };
		};
	}

	// the ring's own line, hovered from either of the two views that speak the
	// ring's grammar (the eye keeps its own opaque layer, §10)
	prHoverRing { |view, x, y|
		var near = false, point;
		if(geometry.notNil and: { view.notNil }) {
			point = this.prStagePoint(view, x, y);
			near = CQNightGeometry.ringBodyHit(geometry, point)
				and: { CQNightGeometry.distance(point, geometry[\eye]) > 12 }
				and: { overlay.patchAt(point).isNil };
		};
		if(near != ringHover) {
			ringHover = near;
			if(freed.not) { ringView.refresh };
		};
	}

	// two cues on one body, told apart by where the hand is: the inner clock's
	// face brightens for the step tap, the body wears the §4.5 outer echo for
	// its §6 solo. A cable under the pointer wins both (§4.5), and a repaint
	// happens only when a boolean actually flips.
	prHoverCastle { |k, view, x, y|
		var near = false, body = false, point, free = true;
		if(geometry.notNil and: { view.notNil }) {
			point = this.prStagePoint(view, x, y);
			free = overlay.patchAt(point).isNil;
			near = CQNightGeometry.dialHit(geometry[\castles][k], point) and: { free };
			body = free and: { near.not }
				and: { CQNightGeometry.bodyHit(geometry[\castles][k], point) };
		};
		if((near != dialHover[k]) or: { body != bodyHover[k] }) {
			dialHover[k] = near;
			bodyHover[k] = body;
			if(freed.not) { castleViews[k].refresh };
		};
	}

	// §6.1 bird drop: same plumbing as the Day deck box — every view of the
	// bird accepts the file, and the deck guards its own unbooted state
	prBindDrops {
		var canDrop = {
			View.currentDrag.isString
				or: { View.currentDrag.isKindOf(SequenceableCollection) }
		};
		#[deckA, deckB].do { |which, i|
			var doDrop = {
				var drag = View.currentDrag;
				var path = if(drag.isString) { drag } { drag.first.asString };
				this.prDeckFor(which) !? { |deck| deck.load(path) };
			};
			[birdViews[i], labels[i],
				endpoints[if(i == 0) { \deckA_rate } { \deckB_rate }][\view]]
			.do { |view|
				view.canReceiveDragHandler = canDrop;
				view.receiveDragHandler = doDrop;
			};
		};
	}

	prDeckFor { |which|
		if(Cocoquantus.graph.isNil) { ^nil };
		^if(which == \deckA) { Cocoquantus.deckA } { Cocoquantus.deckB }
	}

	prLayout {
		var bounds;
		if(freed) { ^this };
		bounds = container.bounds;
		if(bounds.width < 200 or: { bounds.height < 200 }) { ^this };
		geometry = CQNightGeometry.compute(bounds.width, bounds.height);
		groundView.bounds = Rect(0, 0, bounds.width, bounds.height);
		ringView.bounds = geometry[\ringBox];
		eyeView.bounds = Rect.aboutPoint(geometry[\eye], 22, 22);
		castleViews.do { |view, k| view.bounds = geometry[\castles][k][\box] };
		shellViews.do { |view, i|
			view.bounds = geometry[\cocos][i][\shellBox] };
		birdViews.do { |view, i| view.bounds = geometry[\birds][i][\box] };
		endpoints.keysValuesDo { |name, endpoint|
			var center = geometry[\endpoints][name];
			endpoint[\view].bounds = Rect(center.x - endpoint[\anchor].x,
				center.y - endpoint[\anchor].y,
				endpoint[\extent].width, endpoint[\extent].height);
		};
		2.do { |i|
			this.class.crescentNames(i).do { |name, m|
				this.prPlaceCrescent(name, geometry[\cocos][i][\crescents][m])
			};
		};
		this.class.crescentNames(nil).do { |name, i|
			this.prPlaceCrescent(name, geometry[\birds][i][\levelCrescent])
		};
		labels.do { |label, i| label.bounds = geometry[\birds][i][\labelRect] };
		gui.refreshDeckLabels;   // the boxes just changed: re-fit the names
		container.refresh;
	}

	prPlaceCrescent { |name, center|
		crescentViews[name].bounds =
			Rect(center.x - 12, center.y - 12, 24, 24);
	}

	prRegisterUpdaters {
		5.do { |k|
			gui.widgets.register(("c" ++ (k + 1) ++ "Range").asSymbol, { |value|
				rangeLetters[k] = CQNightGeometry.rangeLetter(value);
				if(freed.not) { castleViews[k].refresh };
			});
		};
		2.do { |i|
			var n = i + 1;
			var speedName = ("coco" ++ n ++ "Speed").asSymbol;
			gui.widgets.register(("coco" ++ n ++ "Hold").asSymbol, { |value|
				holdStates[i] = (value ? 0) > 0.5;
				if(freed.not) { shellViews[i].refresh };
			});
			gui.widgets.register(("coco" ++ n ++ "Flip").asSymbol, { |value|
				flipStates[i] = (value ? 0) > 0.5;
				if(freed.not) { shellViews[i].refresh };
			});
			gui.widgets.register(speedName, { |value|
				speedNorms[i] = CQSpec.unmap(speedName, value);
				if(freed.not) { shellViews[i].refresh };
			});
		};
		gui.widgets.register(\qFreq, { |value|
			eyeFreqNorm = CQSpec.unmap(\qFreq, value);
			if(freed.not) { eyeView.refresh };
		});
		gui.widgets.register(\qChaos, { |value|
			eyeChaosNorm = CQSpec.unmap(\qChaos, value);
			if(freed.not) { eyeView.refresh };
		});
		crescentViews.keysValuesDo { |name, view|
			gui.widgets.register(name, { |value|
				crescentNorms[name] = CQSpec.unmap(name, value);
				if(freed.not) { view.refresh };
			});
		};
		gui.widgets.register(\micGain, { |value|
			sproutState = CQNightGeometry.sproutState(
				config[\micChan].notNil, value);
			if(freed.not) { groundView.refresh };
		});
	}

	prSafeDraw { |func|
		try { func.value } { |error| CQInkControl.noteDrawError(error) }
	}

	prDrawGround {
		if(geometry.isNil) { ^this };
		geometry[\roots].do { |root| CQInkNight.drawRoot(root, \night) };
		CQInkNight.drawSprout(geometry[\sprout], sproutState, \night);
	}

	prDrawRing {
		if(geometry.isNil) { ^this };
		CQInkNight.drawRing(geometry, fireflyPhase, ringHover, \night);
	}

	prDrawEye {
		var origin;
		if(geometry.isNil) { ^this };
		origin = Rect.aboutPoint(geometry[\eye], 22, 22).leftTop;
		Pen.translate(origin.x.neg, origin.y.neg);
		CQInkNight.drawEye(geometry[\eye], eyeActive,
			eyeFreqNorm, eyeChaosNorm, \night);
	}

	prDrawCastle { |k|
		var castle;
		if(geometry.isNil) { ^this };
		castle = geometry[\castles][k];
		CQInkNight.drawCastle(castle, castle[\box].leftTop,
			gui.auraFor(("cas" ++ (k + 1)).asSymbol),
			(gui.spesalStates[k] ? 0).asInteger, rangeLetters[k],
			dialHover[k], bodyHover[k], \night);
	}

	// The read direction is what the loop is actually doing, and the FLIP param
	// is only one of its two causes: `\cqCoco` takes `max(param, matrix gate)`,
	// so a flip banana reverses the loop without ever touching the param.
	//
	// Three readings, because two would have to lie (§16.6, amended after the
	// G2.5 review). The param is instant and exact, so it decides on its own.
	// Otherwise the direction comes from `/cq_phase`'s audio-rate mean — never
	// from client-side guessing, which aliased badly once the gate fired faster
	// than the 5 Hz frame: a settled loop reads ±1, and a loop being shuttled
	// faster than an arrow can be read collapses toward 0, where the honest
	// mark is the opposed chevron rather than a frozen or lying arrowhead.
	*shuttleFloor { ^0.5 }

	flipMode { |i|
		var mean;
		if(flipStates[i]) { ^\flipped };
		mean = gui.phaseDirs[i];
		if(mean.isNil) { ^\forward };   // no telemetry: the param is all we know
		if(mean > this.class.shuttleFloor) { ^\forward };
		if(mean < this.class.shuttleFloor.neg) { ^\flipped };
		^\shuttle
	}

	prDrawShell { |i|
		var coco;
		if(geometry.isNil) { ^this };
		coco = geometry[\cocos][i];
		CQInkNight.drawShell(coco, coco[\shellBox].leftTop,
			holdStates[i], this.flipMode(i), speedNorms[i], shellHover[i],
			gui.phaseNorms[i], sparkDirs[i], sparkHover[i], rimHover[i], \night);
	}

	prDrawBird { |i|
		var bird;
		if(geometry.isNil) { ^this };
		bird = geometry[\birds][i];
		CQInkNight.drawBird(bird, bird[\box].leftTop, \night);
	}

	prDrawDecoration { |name, spec, anchor|
		switch(spec[\decoration],
			\leaf, { CQInkNight.drawLeaf(anchor, \night) },
			\crown, { CQInkNight.drawCrown(anchor, \night) },
			\stem, { CQInkNight.drawStemStub(anchor, \night) },
			\root, {
				geometry !? { |geo|
					CQInkNight.drawRootStub(anchor,
						geo[\roots][if(name == \envA) { 0 } { 1 }][\arrival], \night)
				}
			}
		);
	}
}
