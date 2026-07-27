// CQGuiNightMap — the Night stage (GUI_SPEC §6, §16.6): a topology-true
// archipelago carrying the same 16 jacks, 22 sockets, cable overlay and banana
// grammar as Day, plus the §6.1 night touch grammar (CQGuiNightTouch). Pure
// view: geometry from CQNightGeometry, pixels from CQInk/CQInkNight, every
// write through the shared grammar and funnels. Large stable bodies remain
// opaque-backed; castles and their endpoint details use transparent paper so
// only irregular flesh masks the river. Z order is ground → ring → eye →
// castles → shells → birds → endpoints → crescents → labels → cable overlay →
// hit-transparent tide gesture overlay, so an ambient repaint touches
// only the views whose information actually moved (§10) — the storm iris in
// particular repaints without re-inking the ring.

CQGuiNightMap {
	var gui, state, matrix, config;
	var <stageView, <container, <overlay, <controller, <touch, <endpoints, <bodies;
	var <groundView, <ringView, <eyeView, <castleViews, <shellViews, <birdViews;
	var <almanacView, <moonHover = false, <moonFlash = false;
	// The tide is the full ground itself: four seeds, no pool body or outline.
	var <compassView, <compassHover, <compassDragState;
	var <tideGestureView, <tideGateView, <tideGateHover = false;
	var <tideFocus = false, <tideFocusLatched = false;
	var <tideFocusRequested = false;
	var <weatherActive, <weatherFlash, <weatherIntensity = 0;
	var blendPending, blendScheduled = false, lastBlendWrite = 0;
	var <crescentViews, crescentNorms, activeCrescents, hoverCrescents, labels;
	var <geometry, <eyeActive = false;
	var <eyeFreqNorm = 0.5, <eyeChaosNorm = 0.15;
	var <speedNorms, <shellHover, <dialHover, <rangeHover, <bodyHover;
	var <rimHover, <ringHover = false;
	var <sparkDirs, <skipHover, <skipFlash, sparkPainted, flipPainted;
	var struckCount, <struckOn, <struckNorms, <struckBlink;
	var <fireflyPhase = 0.0, lastFireflyTime, castlePainted;
	var <holdStates, <flipStates, <rangeStates, <sproutState;
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
		// window close, GUI free and CmdPeriod all reach here — restore a held
		// punch and let go of any weather gesture through the one choke point
		this.cancelGestures;
		controller !? { controller.free };
		overlay !? { overlay.free };
	}

	// castles read the spesal dial state from CQGui's /cq_spesal cache; the ring
	// carries the coupling those states decide, so a step swings a shown chord
	// under the very hand that stepped it
	spesalChanged {
		if(freed) { ^this };
		castleViews.do { |view| view.refresh };
		ringView.refresh;
	}

	// §16.6: which castle each castle reads is *structure*, so it is drawn only
	// where the hand is asking — one chord under a hovered castle, all five
	// under the hovered river, nothing at rest. The partner and the hold mode
	// come from the Day constellation's own adapter, so one spesal state can
	// never mean two different couplings in two skins.
	chordCastles {
		if(ringHover) { ^(0..4) };
		^(0..4).select { |k| dialHover[k] or: { bodyHover[k] } }
	}

	spesalChords {
		var states = gui.spesalStates;
		if(geometry.isNil) { ^[] };
		^this.chordCastles.collect { |k|
			var state = (states[k] ? 0).asInteger;
			var partner = CQGuiCastleConstellation.partnerIndex(k, state);
			// the chord runs the way the signal runs: the partner's triangle is
			// sampled INTO this castle (SPEC §2.2), so the light travels
			// partner -> k, and the Day link now points the same way
			(path: CQNightGeometry.spesalChord(geometry, partner, k),
				track: CQGuiCastleConstellation.trackMode(state))
		}
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

	// ---- the almanac corner (§6.1, §16.6) ----
	// shakeAmount is a GUI constant, not a CQSpec param, and it is ONE state
	// worn by three layers: the Day knob, the `p` key and this moon. The write
	// goes to CQGui, which clamps it and fans the new phase back out to both
	// skins — so the moon can never disagree with the knob.
	shakeAmount { ^gui.shakeAmount }
	pourShake { |norm| gui.shakeAmount = norm; ^this }
	throwShake { gui.shake; ^this }

	// any shake flashes the moon, from any layer — `p`, the Day button and code
	// all hang off the one `\action` notice, exactly as the skip marks do, so
	// one state shows in three places with one light. It flashes even at the new
	// moon, where nothing is sown: the flash says the press landed, and the dark
	// disc says why nothing grew.
	flashShake {
		if(freed) { ^this };
		moonFlash = true;
		almanacView.refresh;
		AppClock.sched(CQInk.momentaryFlash, {
			if(freed.not) { moonFlash = false; almanacView.refresh };
			nil
		});
		^this
	}

	// ---- the compass water (§7.1) ----
	// All matrix work funnels through here so the touch machine owns gestures
	// only. The blend is CQCompass's tide; every write is a writeMatrix through
	// the batched funnel — clamped, one notification per frame.

	matrixCopy { ^matrix.fullMatrix }

	// a voyage frame: paced to the §7.1 write law with a leading edge, so the
	// first pour of a still hand lands at once and only the stream is thinned.
	// The overlay holds storm form for exactly as long as the pour runs.
	pourBlend { |norm|
		var wait;
		blendPending = norm;
		overlay.stormMode = true;
		if(blendScheduled) { ^this };
		wait = CQGuiNightTouch.compassWritePeriod
			- (Main.elapsedTime - lastBlendWrite);
		if(wait <= 0) { ^this.prFlushBlend };
		blendScheduled = true;
		AppClock.sched(wait, { this.prFlushBlend; nil });
		^this
	}

	prFlushBlend {
		var norm = blendPending;
		blendScheduled = false;
		lastBlendWrite = Main.elapsedTime;
		blendPending = nil;
		if(freed or: { norm.isNil }) { ^this };
		matrix.writeMatrix(gui.compass.blendAt(norm.x, norm.y),
			origin: \gui, committed: false, operation: \compassFrame);
		compassDragState = (pos: norm, pull: CQCompass.bilinear(norm.x, norm.y));
		tideGestureView.refresh;
		^this
	}

	// release (or a tap on open water): one committed write, one undo op, one
	// spellbook entry — `// compass` plus its diffed cells
	commitBlend { |norm, before|
		blendPending = nil;
		overlay.stormMode = false;
		if(freed) { ^this };
		matrix.writeMatrix(gui.compass.blendAt(norm.x, norm.y),
			origin: \gui, committed: true, operation: \compass);
		gui.bananaGrammar.rememberMatrix(before);
		compassDragState = nil;
		tideGestureView.refresh;
		^this
	}

	// the right-drag's snap home (§7.1): a short ~3-frame glide, transient all
	// the way — the elastic never enters the undo ring or the spellbook
	elasticReturn { |before|
		var steps = CQGuiNightTouch.elasticFrames, from;
		blendPending = nil;
		overlay.stormMode = false;
		compassDragState = nil;
		if(freed) { ^this };
		tideGestureView.refresh;
		from = matrix.fullMatrix;
		steps.do { |i|
			var t = (i + 1) / steps;
			AppClock.sched(0.016 * (i + 1), {
				if(freed.not) {
					if(i == (steps - 1)) {
						matrix.writeMatrix(before, origin: \gui,
							committed: false, operation: \compassElastic);
					} {
						matrix.writeMatrix(
							from.collect { |row, di|
								row.collect { |w, si|
									w + ((before[di][si] - w) * t)
								}
							},
							origin: \gui, committed: false,
							operation: \compassElastic);
					};
				};
				nil
			});
		};
		^this
	}

	// a voyage that never poured (cancelled tap) still clears its paint state
	clearVoyage {
		blendPending = nil;
		overlay.stormMode = false;
		compassDragState = nil;
		if(freed.not) { tideGestureView.refresh };
		^this
	}

	// Corner seeds are memories, not four identical buttons. A filled tap
	// recalls; Shift writes/overwrites; an open Tab/gill focus lets an empty tap
	// write without a keyboard; right clears. Dragging one filled seed onto
	// another breeds them without introducing a graft tool.
	cornerStamped { |i| ^gui.compass.corner(i).notNil }

	stampCorner { |i|
		gui.compass.stamp(i, matrix.fullMatrix);
		if(freed.not) { compassView.refresh };
		^this
	}

	seedTap { |i, overwrite = false, focusWrite = false|
		if(overwrite) { ^this.stampCorner(i) };
		if(this.cornerStamped(i)) {
			^this.commitBlend(Point(i % 2, i div: 2), matrix.fullMatrix)
		};
		if(focusWrite) { ^this.stampCorner(i) };
		// Reading an unplanted husk is intentionally inert. Writing is guarded
		// by Shift, or by opening the bay with Tab/the gill.
		^this
	}

	clearCorner { |i|
		gui.compass.clear(i);
		if(freed.not) { compassView.refresh };
		^this
	}

	previewBreed { |i, point|
		var target = CQNightGeometry.stampHit(geometry[\almanac], point);
		if(target == i or: { target.notNil and: {
			this.cornerStamped(target).not } }) {
			target = nil
		};
		compassDragState = (breedFrom: i, cursor: point,
			breedTarget: target);
		if(freed.not) { tideGestureView.refresh };
		^this
	}

	breedCorners { |a, b|
		var before, offspring;
		if(this.cornerStamped(a).not or: { this.cornerStamped(b).not }) {
			^this.clearVoyage
		};
		before = matrix.fullMatrix;
		offspring = CQCompass.breed(gui.compass.corner(a), gui.compass.corner(b));
		matrix.writeMatrix(offspring, origin: \gui, committed: true,
			operation: \breed);
		gui.bananaGrammar.rememberMatrix(before);
		compassDragState = nil;
		if(freed.not) { tideGestureView.refresh };
		^this
	}

	// Shift is a momentary water focus; Tab and the gill latch the same state.
	// Modifier release waits until a captured tide gesture ends, so the cable
	// field cannot jump back over the pointer mid-voyage. A latch survives Esc,
	// panic and view changes until Tab/the gill explicitly closes it.
	modifierTideFocus_ { |flag|
		tideFocusRequested = flag == true;
		if(tideFocusRequested) {
			if(gui.bananaGrammar.isInHand.not) { this.prSetTideFocus(true) };
		} {
			if(touch.isNil or: { touch.tideActive.not }) {
				this.prSetTideFocus(tideFocusLatched)
			};
		};
		^this
	}

	toggleTideFocus {
		if(gui.bananaGrammar.isInHand) { ^this };
		tideFocusLatched = tideFocusLatched.not;
		this.prSetTideFocus(tideFocusLatched or: { tideFocusRequested });
		^this
	}

	tideFocusLatched_ { |flag|
		tideFocusLatched = flag == true;
		if(gui.bananaGrammar.isInHand.not) {
			this.prSetTideFocus(tideFocusLatched or: { tideFocusRequested })
		};
		^this
	}

	tideFocusGesture_ { |flag|
		if(flag == true and: { gui.bananaGrammar.isInHand.not }) {
			this.prSetTideFocus(true)
		};
		^this
	}

	finishTideFocus { |shifted = false|
		tideFocusRequested = shifted == true;
		if(touch.isNil or: { touch.tideActive.not }) {
			this.prSetTideFocus((tideFocusLatched or: { tideFocusRequested })
				and: { gui.bananaGrammar.isInHand.not })
		};
		^this
	}

	prSetTideFocus { |flag|
		var next = flag == true;
		if(next == tideFocus) { ^this };
		tideFocus = next;
		overlay !? { overlay.backgrounded = next };
		if(freed.not) {
			compassView.refresh;
			tideGateView !? { tideGateView.refresh };
		};
		^this
	}

	// one §7.2 seed lands (grow) or one cable thins (wither) — audible at
	// once as a transient write; the mark flashes 120 ms per act, so a held
	// rain visibly rains at the pull's current tension
	weatherStep { |mark|
		var change;
		if(freed) { ^false };
		change = if(mark == \grow) {
			CQCompass.growStep(matrix.fullMatrix)
		} {
			CQCompass.witherStep(matrix.fullMatrix)
		};
		if(change.isNil) { ^false };
		matrix.patch(CQMatrix.sourceNames[change[1]],
			CQMatrix.destNames[change[0]], change[2],
			origin: \gui, committed: false,
			operation: if(mark == \grow) { \growFrame } { \witherFrame });
		this.prFlashWeather(mark);
		^true
	}

	// one hold = one committed op, one undo, one `// grow` / `// wither`
	// spellbook batch; a hold that landed nothing commits nothing
	commitWeather { |mark, before, landed = false|
		if(landed.not or: { freed }) { ^this };
		matrix.writeMatrix(matrix.fullMatrix, origin: \gui, committed: true,
			operation: mark);
		gui.bananaGrammar.rememberMatrix(before);
		^this
	}

	// Rain and sun are transient faces of the moon's vertical gesture. No
	// weather glyph exists on the resting tide.
	prRefreshWeather {
		if(freed.not) { almanacView.refresh };
	}

	weatherGesture { |mark, intensity = 0|
		var next = (intensity ? 0).asFloat.clip(0, 1);
		if(mark.isNil) { next = 0 };
		if(mark == weatherActive
			and: { (next - weatherIntensity).abs <= 0.001 }) { ^this };
		weatherActive = mark;
		weatherIntensity = next;
		this.prRefreshWeather;
		^this
	}

	weatherActive_ { |mark|
		^this.weatherGesture(mark,
			if(mark.isNil) { 0 } { weatherIntensity })
	}

	prFlashWeather { |mark|
		if(freed) { ^this };
		weatherFlash = mark;
		this.prRefreshWeather;
		AppClock.sched(CQInk.momentaryFlash, {
			if(freed.not and: { weatherFlash == mark }) {
				weatherFlash = nil;
				this.prRefreshWeather;
			};
			nil
		});
		^this
	}

	// The single "hand comes off every night gesture" choke point. Every
	// non-mouse-up exit path lands here — Esc and Tab (CQGui.prKey), panic
	// (CQGui.cancelAudition), and window close / GUI free / CmdPeriod (free,
	// below). Restoring the momentary punch FIRST is deliberate: a stuck punch
	// leaves a loop recording over itself (§2), so it is the destructive one.
	// The weather's uncommitted voyage then goes home like the elastic.
	cancelGestures {
		touch !? {
			touch.cancelPunch;
			touch.cancelWeather;
		};
		tideFocusRequested = false;
		this.prSetTideFocus(tideFocusLatched);
		^this
	}

	// one '/cq_phase' frame (CQGui caches the funnel truth in phaseNorms; the
	// 5 Hz send rate is the repaint ceiling, §10). The comet's direction is
	// read from the phase's actual travel — wrap-aware — so a matrix-driven
	// flip shows truthfully even when the chevron's param says otherwise.
	// A move under ~1.5 px along the line keeps the frame.
	paintPhase { |i|
		var norm = gui.phaseNorms[i], mean = gui.phaseDirs[i];
		var delta, mode, struck, rate, repaint = false;
		if(freed) { ^this };
		// the chevron's regime can change while the address barely moves — a
		// shuttled loop goes nowhere — so it gets its own gate
		mode = this.flipMode(i);
		if(mode != flipPainted[i]) { flipPainted[i] = mode; repaint = true };
		// so does the comet's: a hammered loop never leaves the eye, and a
		// bead crawling near zero would read as "almost stopped" — the exact
		// opposite of the truth
		struck = this.prStruck(i);
		if(struck != struckOn[i]) { struckOn[i] = struck; repaint = true };
		// the weight only moves in the steps the drawing can show
		rate = this.class.struckNorm(gui.phaseSkipHz[i]);
		if(struck and: { rate != struckNorms[i] }) {
			struckNorms[i] = rate;
			repaint = true;
		};
		// and the mark twinkles once per frame while the state is in force —
		// §16.9's read-head spark, ambient at the telemetry rate. The frames
		// drive it, so a stalled server freezes the mark rather than animating
		// a claim about liveness it can no longer make (§9's law, in motion)
		if(struck) {
			struckBlink[i] = struckBlink[i].not;
			repaint = true;
		} {
			struckBlink[i] = false;
		};
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

	// skip's charge is peak-held 0.15 s on the server (§9), so if it is still
	// lit when the next 5 Hz phase frame lands, the loop was restarted inside
	// that window. Two frames running means the head is being thrown back
	// faster than the eye can follow, and it wears the struck mark (§16.6).
	// No new telemetry and no client-side timer: the charge already says it.
	*struckFrames { ^2 }

	// The rate the struck mark wears as weight, 8 Hz → 2 kHz over a log scale
	// (rate is a pitch to the ear, so it is an octave axis to the eye too).
	// Quantized to the eight steps the drawing can actually distinguish, so a
	// drifting rate cannot repaint the shell on every 5 Hz frame for nothing.
	*struckSteps { ^8 }

	*struckNorm { |hz|
		if(hz.isNil) { ^0 };
		^((((hz.max(1).log2) - 3) / 8).clip(0, 1) * this.struckSteps).round
			/ this.struckSteps
	}

	prStruck { |i|
		var charge = gui.chargeFor(("coco" ++ (i + 1) ++ "_skip").asSymbol);
		if(charge.notNil and: { charge >= 0.5 }) {
			struckCount[i] = struckCount[i] + 1;
		} {
			struckCount[i] = 0;
		};
		^struckCount[i] >= this.class.struckFrames
	}

	// any skip flashes the mouth mark, exactly as it flashes the Day button —
	// both hang off the one `\action` notice, so code and patterns show too
	flashSkip { |n|
		var i = n.asInteger - 1;
		if(freed or: { i < 0 } or: { i > 1 }) { ^this };
		skipFlash[i] = true;
		shellViews[i].refresh;
		AppClock.sched(CQInk.momentaryFlash, {
			if(freed.not) { skipFlash[i] = false; shellViews[i].refresh };
			nil
		});
		^this
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
			overlay.auraTick   // the alphas moved: the offscreen cache re-renders
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
		rangeHover = Array.fill(5, false);
		bodyHover = Array.fill(5, false);
		sparkDirs = [1, 1];
		skipHover = [false, false];
		skipFlash = [false, false];
		struckCount = [0, 0];
		struckOn = [false, false];
		struckNorms = [0, 0];
		struckBlink = [false, false];
		sparkPainted = Array.newClear(2);
		flipPainted = Array.newClear(2);
		rangeStates = Array.newClear(5);
		sproutState = CQNightGeometry.sproutState(config[\micChan].notNil, 0);
		container = View().background_(night[\ground]);
		groundView = this.prLayer;
		groundView.drawFunc = { this.prSafeDraw { this.prDrawGround } };
		// Public compatibility name; the tide is now painted by, and receives
		// open-water gestures through, the full-stage ground.
		compassView = groundView;
		ringView = this.prLayer;
		ringView.drawFunc = { this.prSafeDraw { this.prDrawRing } };
		eyeView = this.prLayer;
		eyeView.drawFunc = { this.prSafeDraw { this.prDrawEye } };
		castleViews = 5.collect { |k|
			// Transparent paper lets the river remain continuous around the
			// irregular flesh. drawCastle masks only the flesh itself.
			var view = this.prLayer(false);
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
		// the almanac corner sits above the bodies in z but on water none of
		// them reach, so its opaque layer covers nothing (§16.6, pinned by the
		// geometry's clearance tests)
		almanacView = this.prLayer;
		almanacView.drawFunc = { this.prSafeDraw { this.prDrawAlmanac } };
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
		tideGateView = this.prLayer(false);
		tideGateView.drawFunc = { this.prSafeDraw { this.prDrawTideGate } };
		overlay = CQGuiCableOverlay(matrix, endpoints, gui.bananaGrammar,
			{ |src| gui.auraFor(src) }).skin_(\night);
		// Gesture ink must cross every opaque landscape rectangle. This view is
		// the topmost stage sibling and never accepts the mouse.
		tideGestureView = UserView()
			.background_(night[\clear])
			.acceptsMouse_(false);
		tideGestureView.drawFunc = {
			this.prSafeDraw { this.prDrawTideGesture }
		};
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
		this.prBindTideGround;
		this.prBindTideGate;
		this.prBindDrops;
		stageView = View().background_(night[\ground]).layout_(
			StackLayout(tideGestureView, overlay.view, container).mode_(\stackAll));
		container.onResize = { this.prLayout };
		this.prRegisterUpdaters;
		{ this.prLayout }.defer;
		^this
	}

	prLayer { |opaque = true|
		var palette = CQInk.palette(\night);
		^UserView(container, Rect(0, 0, 4, 4))
			.background_(palette[if(opaque) { \ground } { \clear }])
	}

	prMakeEndpoint { |name|
		var spec = this.class.endpointSpec(name);
		// the box and its anchor come from CQNightGeometry, so the map, the
		// clearance tests and the view itself size from one table (rake 12)
		var extent = CQNightGeometry.endpointExtent(spec[\kind], spec[\lamp]);
		var anchor = CQNightGeometry.endpointAnchor(spec[\kind]);
		var width = extent.x, height = extent.y;
		var castleDetail = name.asString.beginsWith("tri")
			or: { name.asString.beginsWith("cas") }
			or: { name.asString.beginsWith("castle") };
		var view = UserView(container, Rect(0, 0, width, height))
			.background_(CQInk.palette(\night)[
				if(castleDetail) { \clear } { \ground }]);
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
		var extent = CQNightMeasures.crescentExtent;
		var view = UserView(container, Rect(0, 0, extent, extent))
			.background_(CQInk.palette(\night)[\ground]);
		var family = if(name.asString.endsWith("Spaf")) { \spaf } { nil };
		view.drawFunc = {
			this.prSafeDraw {
				CQInkNight.drawCrescent(Point(extent * 0.5, extent * 0.5),
					crescentNorms[name] ? 0,
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
				touch.landscapeMove(v, x, y, mods ? 0)
			};
			view.mouseUpAction = { |v, x, y, mods, button|
				touch.landscapeUp(v, x, y, mods ? 0, button ? 0)
			};
			view.mouseWheelAction = { |v, x, y, mods, xd, yd|
				touch.forwardWheel(v, x, y, mods ? 0, yd ? 0)
			};
		};
		almanacView.mouseDownAction = { |v, x, y, mods, button, clicks|
			touch.almanacDown(v, x, y, mods ? 0, button ? 0, clicks ? 1)
		};
		almanacView.mouseMoveAction = { |v, x, y, mods|
			touch.almanacMove(v, x, y, mods ? 0)
		};
		almanacView.mouseUpAction = { |v, x, y, mods, button|
			touch.almanacUp(v, x, y, mods ? 0, button ? 0)
		};
		almanacView.mouseWheelAction = { |v, x, y, mods, xd, yd|
			touch.forwardWheel(v, x, y, mods ? 0, yd ? 0)
		};
		compassView.mouseDownAction = { |v, x, y, mods, button, clicks|
			touch.compassDown(v, x, y, mods ? 0, button ? 0, clicks ? 1)
		};
		compassView.mouseMoveAction = { |v, x, y, mods|
			touch.compassMove(v, x, y, mods ? 0)
		};
		compassView.mouseUpAction = { |v, x, y, mods, button|
			touch.compassUp(v, x, y, mods ? 0, button ? 0)
		};
		compassView.mouseWheelAction = { |v, x, y, mods, xd, yd|
			touch.forwardWheel(v, x, y, mods ? 0, yd ? 0)
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
		almanacView.acceptsMouseOver = true;
		almanacView.mouseOverAction = { |v, x, y| this.prHoverAlmanac(v, x, y) };
		almanacView.mouseLeaveAction = { this.prHoverAlmanac(nil, nil, nil) };
		compassView.acceptsMouseOver = true;
		compassView.mouseOverAction = { |v, x, y| this.prHoverCompass(v, x, y) };
		compassView.mouseLeaveAction = { this.prHoverCompass(nil, nil, nil) };
	}

	// The shared controller first teaches every piece of paper the ordinary
	// banana/harvest grammar. Open night ground then gets this one extra pass:
	// a left voyage inside the four winds, with every unclaimed act forwarded
	// straight back to that controller.
	prBindTideGround {
		groundView.mouseDownAction = { |v, x, y, mods, button, clicks|
			touch.tideDown(v, x, y, mods ? 0, button ? 0, clicks ? 1)
		};
		groundView.mouseMoveAction = { |v, x, y, mods|
			touch.tideMove(v, x, y, mods ? 0)
		};
		groundView.mouseUpAction = { |v, x, y, mods, button|
			touch.tideUp(v, x, y, mods ? 0, button ? 0)
		};
		groundView.mouseWheelAction = { |v, x, y, mods, xd, yd|
			touch.forwardWheel(v, x, y, mods ? 0, yd ? 0)
		};
		^this
	}

	// The gill is the GUI entrance to the same state Tab latches. A cable still
	// wins while the bay is closed; once open, spans are deliberately behind
	// the water and the organ itself remains available to close it again.
	prBindTideGate {
		tideGateView.mouseDownAction = { |v, x, y, mods, button, clicks|
			touch.tideGateDown(v, x, y, mods ? 0, button ? 0, clicks ? 1)
		};
		tideGateView.mouseMoveAction = { |v, x, y, mods|
			touch.landscapeMove(v, x, y, mods ? 0)
		};
		tideGateView.mouseUpAction = { |v, x, y, mods, button|
			touch.landscapeUp(v, x, y, mods ? 0, button ? 0)
		};
		tideGateView.mouseWheelAction = { |v, x, y, mods, xd, yd|
			touch.forwardWheel(v, x, y, mods ? 0, yd ? 0)
		};
		tideGateView.acceptsMouseOver = true;
		tideGateView.mouseOverAction = { |v, x, y|
			this.prHoverTideGate(v, x, y)
		};
		tideGateView.mouseLeaveAction = {
			this.prHoverTideGate(nil, nil, nil)
		};
		^this
	}

	prStagePoint { |view, x, y|
		^view.mapToGlobal(Point(x, y)) - container.mapToGlobal(0@0)
	}

	prHoverShell { |i, view, x, y|
		var near = false, mark = false, rim = false, point, free = true;
		if(geometry.notNil and: { view.notNil }) {
			point = this.prStagePoint(view, x, y);
			free = overlay.patchAt(point).isNil;
			near = (CQNightGeometry.spiralDistance(geometry[\cocos][i], point)
				<= CQNightMeasures.windGrab) and: { free };
			// the skip mark sits off the winding surface, above the mouth
			// jack, so it answers the hand on its own account
			mark = free and: {
				CQNightGeometry.markHit(geometry[\cocos][i][\skipMark], point)
			};
			// the winding surface and the mark are consulted first, so the rim
			// cue never advertises a body solo where the next press would wind
			rim = free and: { near.not } and: { mark.not }
				and: { CQNightGeometry.shellRimHit(geometry[\cocos][i], point) };
		};
		if((near != shellHover[i]) or: { mark != skipHover[i] }
			or: { rim != rimHover[i] }) {
			shellHover[i] = near;
			skipHover[i] = mark;
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
				and: { CQNightGeometry.distance(point, geometry[\eye])
					> CQNightMeasures.eyeHit }
				and: { overlay.patchAt(point).isNil };
		};
		if(near != ringHover) {
			ringHover = near;
			if(freed.not) { ringView.refresh };
		};
	}

	// Three exclusive cues on one body: inner clock, 6-o'clock rate ripple, or
	// flesh/body. Their order mirrors castleDown exactly after cable priority,
	// so the cue always promises the action the next press will receive.
	prHoverCastle { |k, view, x, y|
		var dial = false, range = false, body = false, point, free = true;
		if(geometry.notNil and: { view.notNil }) {
			point = this.prStagePoint(view, x, y);
			free = overlay.patchAt(point).isNil;
			range = CQNightGeometry.rangeHit(geometry[\castles][k], point)
				and: { free };
			dial = free and: { range.not }
				and: { CQNightGeometry.dialHit(geometry[\castles][k], point) };
			body = free and: { range.not } and: { dial.not }
				and: { CQNightGeometry.bodyHit(geometry[\castles][k], point) };
		};
		if((dial != dialHover[k]) or: { range != rangeHover[k] }
			or: { body != bodyHover[k] }) {
			dialHover[k] = dial;
			rangeHover[k] = range;
			bodyHover[k] = body;
			if(freed.not) {
				castleViews[k].refresh;
				// the coupling chord lives on the ring's layer (§16.6): a castle
				// cannot paint a line to a body three hundred pixels away
				ringView.refresh;
			};
		};
	}

	// the moon's own drawn rim is its cue, as everywhere else in the night. A
	// cable crossing the sky wins the press (§4.5), so it wins the cue too, and
	// a repaint happens only when the boolean actually flips.
	prHoverAlmanac { |view, x, y|
		var moon = false, point, free = false;
		if(geometry.notNil and: { view.notNil }) {
			point = this.prStagePoint(view, x, y);
			free = overlay.patchAt(point).isNil;
			moon = CQNightGeometry.moonHit(geometry[\almanac], point) and: { free };
		};
		if(moon != moonHover) {
			moonHover = moon;
			if(freed.not) { almanacView.refresh };
		};
	}

	// At rest only a seed advertises an action. The field itself becomes
	// perceptible under Shift focus or after the hand has crossed the 8 px
	// threshold; ordinary open water keeps no widget hover rectangle.
	prHoverCompass { |view, x, y|
		var next, point, almanac, seed;
		if(geometry.notNil and: { view.notNil }) {
			point = this.prStagePoint(view, x, y);
			almanac = geometry[\almanac];
			if(overlay.patchAt(point).isNil) {
				seed = CQNightGeometry.stampHit(almanac, point);
				next = if(seed.notNil) { [\stamp, seed] } {
					if(tideFocus
						and: { CQNightGeometry.compassHit(almanac, point) }) {
						\pad
					}
				};
			};
		};
		if(next != compassHover) {
			compassHover = next;
			if(freed.not) { compassView.refresh };
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
			var name = this.class.dropTargetName(which);
			var doDrop = {
				var drag = View.currentDrag;
				var path = if(drag.isString) { drag } { drag.first.asString };
				gui.dropTargetClear;
				this.prDeckFor(which) !? { |deck| deck.load(path) };
			};
			// the label and the rate moon accept the file too, but the border
			// is drawn on the bird — §16.7 marks the body, not its parts
			[birdViews[i], labels[i],
				endpoints[if(i == 0) { \deckA_rate } { \deckB_rate }][\view]]
			.do { |view|
				view.canReceiveDragHandler = { |v|
					var accepted = canDrop.value(v);
					// cue and action share one rect, in the coordinates the
					// cursor poll speaks (§16.7)
					if(accepted) {
						gui.dropTargetHover(name, groundView, {
							this.prGlobalRect(
								geometry !? { |geo| geo[\birds][i][\dropRect] })
						})
					};
					accepted
				};
				view.receiveDragHandler = doDrop;
			};
		};
	}

	// a stage rect in the screen coordinates QtGUI.cursorPosition reports
	prGlobalRect { |rect|
		var origin;
		if(rect.isNil or: { freed }) { ^nil };
		origin = container.mapToGlobal(0@0);
		^Rect(rect.left + origin.x, rect.top + origin.y, rect.width, rect.height)
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
		almanacView.bounds = geometry[\almanac][\box];
		tideGateView.bounds = geometry[\tideGateBox];
		ringView.bounds = geometry[\ringBox];
		eyeView.bounds = Rect.aboutPoint(geometry[\eye], CQNightMeasures.eyeLayerR,
			CQNightMeasures.eyeLayerR);
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
		var extent = CQNightMeasures.crescentExtent;
		crescentViews[name].bounds =
			Rect(center.x - (extent * 0.5), center.y - (extent * 0.5),
				extent, extent);
	}

	prRegisterUpdaters {
		5.do { |k|
			gui.widgets.register(("c" ++ (k + 1) ++ "Range").asSymbol, { |value|
				rangeStates[k] = value;
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
		// the sowing moon's phase: not a CQSpec param, so it rides CQGui's own
		// fan-out rather than the paced param stream (§6.1)
		gui.onShakeAmount({ if(freed.not) { almanacView.refresh } });
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
		this.prDrawCompass;
		geometry[\roots].do { |root| CQInkNight.drawRoot(root, \night) };
		CQInkNight.drawSprout(geometry[\sprout], sproutState, \night);
		// §16.7's border rings the bird AND its name, so it is drawn on the
		// full-stage ground rather than inside the bird's own 44x32 layer —
		// a view cannot paint outside its bounds, and this rect is bigger than
		// the bird on purpose (it is what accepts the file). Ground is the
		// bottom layer, but the border runs outside every opaque layer it
		// encloses, so nothing covers it.
		geometry[\birds].do { |bird, i|
			var which = if(i == 0) { \deckA } { \deckB };
			if(gui.dropTargetIs(this.class.dropTargetName(which))) {
				CQInk.drawDropTarget(bird[\dropRect], \night)
			};
		};
	}

	prDrawRing {
		if(geometry.isNil) { ^this };
		CQInkNight.drawRing(geometry, fireflyPhase, ringHover, \night,
			this.spesalChords);
	}

	// One remote weather body. Rain or sun exists only while the moon's locked
	// vertical gesture is active; at rest there is no satellite toolbar.
	prDrawAlmanac {
		var almanac;
		if(geometry.isNil) { ^this };
		almanac = geometry[\almanac];
		Pen.translate(almanac[\box].left.neg, almanac[\box].top.neg);
		CQInkNight.drawSowingMoon(almanac[\moon], almanac[\moonR],
			gui.shakeAmount, moonHover, moonFlash, \night);
		weatherActive !? { |name|
			CQInkNight.drawWeatherMark(name,
				almanac[\weatherPreview][name], false, true,
				weatherFlash == name, false, \night, weatherIntensity)
		};
	}

	prHoverTideGate { |view, x, y|
		var next = false, point;
		if(geometry.notNil and: { view.notNil }) {
			point = this.prStagePoint(view, x, y);
			next = CQNightGeometry.tideGateHit(geometry, point)
				and: { overlay.patchAt(point).isNil };
		};
		if(next != tideGateHover) {
			tideGateHover = next;
			if(freed.not) { tideGateView.refresh };
		};
	}

	// The tide is inferred by four seeds. No shore or rectangle is drawn at
	// rest; focus and a live voyage reveal only faint pulls through the bay.
	prDrawCompass {
		var almanac;
		if(geometry.isNil) { ^this };
		almanac = geometry[\almanac];
		CQInkNight.drawTideField(almanac, gui.compass.memoryDensities,
			tideFocus, compassHover, \night);
	}

	prDrawTideGesture {
		if(geometry.isNil) { ^this };
		CQInkNight.drawTideGesture(geometry[\almanac],
			compassDragState, \night);
	}

	prDrawTideGate {
		if(geometry.isNil) { ^this };
		CQInkNight.drawTideGate(geometry[\tideGate],
			geometry[\tideGateBox].leftTop,
			tideFocus, tideGateHover, \night);
	}

	prDrawEye {
		var origin;
		if(geometry.isNil) { ^this };
		origin = Rect.aboutPoint(geometry[\eye], CQNightMeasures.eyeLayerR,
			CQNightMeasures.eyeLayerR).leftTop;
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
			(gui.spesalStates[k] ? 0).asInteger, rangeStates[k],
			dialHover[k], rangeHover[k], bodyHover[k], \night);
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
			gui.phaseNorms[i], sparkDirs[i], skipHover[i], skipFlash[i],
			if(struckOn[i]) { struckNorms[i] }, struckBlink[i],
			rimHover[i], \night);
	}

	// the night half of §16.7's drop-target law; the Day deck box wears the
	// identical mark, from the same CQInk primitive
	*dropTargetName { |which| ^("night_" ++ which).asSymbol }

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
