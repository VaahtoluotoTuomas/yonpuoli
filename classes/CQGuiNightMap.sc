// CQGuiNightMap — the Night stage (GUI_SPEC §6, §16.6): a topology-true
// archipelago carrying the same 16 jacks, 22 sockets, cable overlay and banana
// grammar as Day. Pure view: geometry from CQNightGeometry, pixels from
// CQInk/CQInkNight, every write through the shared grammar and funnels. The
// layers are opaque-backed (ground → ring → castles → endpoints → labels, the
// hit-transparent overlay on top) so an ambient repaint touches only the views
// whose information actually moved (§10 — a drawing's price is its call count).

CQGuiNightMap {
	var gui, state, matrix, config;
	var <stageView, <container, <overlay, <controller, <endpoints;
	var <groundView, ringView, <castleViews, labels;
	var <geometry;
	var <fireflyPhase = 0.0, lastFireflyTime, castlePainted;
	var <holdStates, <flipStates, <rangeLetters, <sproutState;
	var freed = false;

	*new { |gui, state, matrix, config|
		^super.newCopyArgs(gui, state, matrix, config).prBuild
	}

	// same families, glyphs and lamps as the Day skin — one learned shape
	// (§16.7); decorations are the night homes of §3.1 (leaf, crown, mouth,
	// fruit stem). Pure lookup, unit-testable without Qt.
	*endpointSpec { |name|
		var string = name.asString, prefix = string.keep(3).asSymbol;
		if(CQMatrix.srcIndex(name).notNil) {
			^(kind: \jack,
				family: (tri: \tri, cas: \cas, env: \input,
					coc: \cas, org: \organ)[prefix] ? \line,
				decoration: (tri: \leaf, cas: \crown, coc: \mouth,
					org: \stem)[prefix])
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

	// One /cq_aura frame that actually moved (CQGui gates the stream and the
	// visible view). Fireflies ride exactly these frames: continuous flow under
	// the transparent cable layer would cost O(cables) per frame at rest, so a
	// truly still instrument keeps a still river until the offscreen cable
	// cache lands after G2 (see PLAN "Myöhemmin").
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
		var night = CQInk.palette(\night);
		endpoints = IdentityDictionary.new;
		castlePainted = Array.newClear(5);
		holdStates = [false, false];
		flipStates = [false, false];
		rangeLetters = ["", "", "", "", ""];
		sproutState = CQNightGeometry.sproutState(config[\micChan].notNil, 0);
		container = View().background_(night[\ground]);
		groundView = UserView(container, Rect(0, 0, 4, 4))
			.background_(night[\ground]);
		groundView.drawFunc = { this.prSafeDraw { this.prDrawGround } };
		ringView = UserView(container, Rect(0, 0, 4, 4))
			.background_(night[\ground]);
		ringView.drawFunc = { this.prSafeDraw { this.prDrawRing } };
		castleViews = 5.collect { |k|
			var view = UserView(container, Rect(0, 0, 4, 4))
				.background_(night[\ground]);
			view.drawFunc = { this.prSafeDraw { this.prDrawCastle(k) } };
			view
		};
		(CQMatrix.sourceNames ++ CQMatrix.destNames).do { |name|
			this.prMakeEndpoint(name)
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
		controller = CQGuiBananaController(gui.bananaGrammar, endpoints,
			IdentityDictionary.new, overlay, List.new);
		controller.bindGround(container);
		stageView = View().background_(night[\ground]).layout_(
			StackLayout(overlay.view, container).mode_(\stackAll));
		container.onResize = { this.prLayout };
		this.prRegisterUpdaters;
		{ this.prLayout }.defer;
		^this
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

	prLayout {
		var bounds;
		if(freed) { ^this };
		bounds = container.bounds;
		if(bounds.width < 200 or: { bounds.height < 200 }) { ^this };
		geometry = CQNightGeometry.compute(bounds.width, bounds.height);
		groundView.bounds = Rect(0, 0, bounds.width, bounds.height);
		ringView.bounds = geometry[\ringBox];
		castleViews.do { |view, k| view.bounds = geometry[\castles][k][\box] };
		endpoints.keysValuesDo { |name, endpoint|
			var center = geometry[\endpoints][name];
			endpoint[\view].bounds = Rect(center.x - endpoint[\anchor].x,
				center.y - endpoint[\anchor].y,
				endpoint[\extent].width, endpoint[\extent].height);
		};
		labels.do { |label, i| label.bounds = geometry[\birds][i][\labelRect] };
		container.refresh;
	}

	prRegisterUpdaters {
		5.do { |k|
			gui.widgets.register(("c" ++ (k + 1) ++ "Range").asSymbol, { |value|
				rangeLetters[k] = CQNightGeometry.rangeLetter(value);
				this.prRefreshCastle(k);
			});
		};
		2.do { |i|
			var n = i + 1;
			gui.widgets.register(("coco" ++ n ++ "Hold").asSymbol, { |value|
				holdStates[i] = (value ? 0) > 0.5;
				if(freed.not) { groundView.refresh };
			});
			gui.widgets.register(("coco" ++ n ++ "Flip").asSymbol, { |value|
				flipStates[i] = (value ? 0) > 0.5;
				if(freed.not) {
					endpoints[("coco" ++ n).asSymbol][\view].refresh;
				};
			});
		};
		gui.widgets.register(\micGain, { |value|
			sproutState = CQNightGeometry.sproutState(
				config[\micChan].notNil, value);
			if(freed.not) { groundView.refresh };
		});
	}

	prRefreshCastle { |k|
		if(freed.not) { castleViews[k].refresh };
	}

	prSafeDraw { |func|
		try { func.value } { |error| CQInkControl.noteDrawError(error) }
	}

	prDrawGround {
		if(geometry.isNil) { ^this };
		geometry[\cocos].do { |coco, i|
			CQInkNight.drawSpiral(coco, holdStates[i], \night)
		};
		geometry[\roots].do { |root| CQInkNight.drawRoot(root, \night) };
		CQInkNight.drawSprout(geometry[\sprout], sproutState, \night);
		geometry[\birds].do { |bird| CQInkNight.drawBird(bird, \night) };
	}

	prDrawRing {
		if(geometry.isNil) { ^this };
		CQInkNight.drawRing(geometry, fireflyPhase, \night);
	}

	prDrawCastle { |k|
		var castle;
		if(geometry.isNil) { ^this };
		castle = geometry[\castles][k];
		CQInkNight.drawCastle(castle, castle[\box].leftTop,
			gui.auraFor(("cas" ++ (k + 1)).asSymbol),
			(gui.spesalStates[k] ? 0).asInteger, rangeLetters[k], \night);
	}

	prDrawDecoration { |name, spec, anchor|
		switch(spec[\decoration],
			\leaf, { CQInkNight.drawLeaf(anchor, \night) },
			\crown, { CQInkNight.drawCrown(anchor, \night) },
			\stem, { CQInkNight.drawStemStub(anchor, \night) },
			\mouth, {
				CQInkNight.drawMouthChevron(anchor,
					flipStates[if(name == \coco1) { 0 } { 1 }], \night)
			}
		);
	}
}
