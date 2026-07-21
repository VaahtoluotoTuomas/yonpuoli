// CQGui — the panel window (SPEC §6.2, ARCH §5): one window, four columns
// mirroring the hardware layout + master strip + banana-bay grid + keyboard
// map. Pure view: it renders from CQState/CQMatrix dependant notifications
// and server reports (OSCdefs \cq_gui_*, freed here), and every write goes
// through the funnels (CQGuiWidgets -> CQState, grid -> CQMatrix, actions ->
// Cocoquantus facade). All UI work happens inside { }.defer; no server polling
// — the only language-side timer is the 2 Hz REC elapsed-time display.

CQGui {
	var state, matrix, config;
	var <window, <widgets, updaters, <shellState, <shell, <drawer;
	var stateCtl, gridPanel, meterFuncs, <audition;
	var <bananaGrammar, <bananaController, <bananaBodies;
	var <endpoints, <cableOverlay;
	var <inkControls;
	var castleLeds, spesalView, <spesalStates, <castleValues;
	// GUI_SPEC §9: 16 source magnitudes, nil until the server has actually
	// reported. No telemetry = no pulse, so serverless and headless surfaces
	// keep exactly the reviewed static Day look.
	var <auraValues, auraPainted;
	var deckLabels, deckCtls, skipButtons;
	var noticeView, recBtn, recTimeView, recTask, hotTask;
	var micLive = false, hotLive = false, cleaned = false;
	var paramPending, paramFlushScheduled = false, lastParamFlush = 0;
	// shake throw amplitude shared by the SHAKE button and the p key; GUI-local
	// because shake is an action argument, not a CQSpec param (SPEC §5.5 —
	// the weight clamp lives in CQMatrix.shake). 0.3 = the facade default.
	var <>shakeAmount = 0.3;

	*new { |state, matrix, config|
		^super.newCopyArgs(state, matrix, config).prInit
	}

	isClosed {
		^shellState.lifecycleState == \closed
			or: { window.notNil and: { window.isClosed } }
	}

	free {
		shellState.beginClose;
		this.prCleanup;
		{ window !? { if(window.isClosed.not) { window.close } } }.defer;
	}

	cancelAudition {
		if(drawer.notNil) { drawer.panic } { bananaGrammar !? { bananaGrammar.panic } }
	}
	shake {
		bananaGrammar.performUndoable({
			Cocoquantus.shake(shakeAmount, origin: \gui)
		});
		^this
	}

	// ---- panel registration API (used by CQGuiPanels while building) ----

	setMeter { |key, func| meterFuncs[key] = func }
	setDeckLabel { |which, view| deckLabels[which] = view }
	deckLabel { |which| ^deckLabels[which] }
	setSkipButton { |n, btn| skipButtons[n] = btn }
	castleLeds_ { |views| castleLeds = views }
	spesalView_ { |view| spesalView = view }
	notice_ { |view| noticeView = view }
	recButton_ { |btn| recBtn = btn }
	recTime_ { |view| recTimeView = view }
	registerInkControl { |control| inkControls.add(control); ^control }
	registerEndpoint { |name, kind, endpointView, anchor|
		endpoints[name] = (name: name, kind: kind, view: endpointView, anchor: anchor);
		^endpointView
	}
	registerBananaBody { |name, bodyView, members|
		bananaBodies[name] = (name: name, view: bodyView, members: members);
		^bodyView
	}
	drawEndpoint { |name, kind, center, family, glyph, lamp = false|
		var palette = CQInk.palette(\day), endpointState;
		endpointState = if(kind == \jack) {
			if(bananaGrammar.isSourceInHand(name)) { \inHand } {
				if(bananaGrammar.hoverEndpoint == name) { \hover } { \idle }
			}
		} {
			if(bananaGrammar.isCandidate(name)) { \candidate } {
				if(bananaGrammar.hoverEndpoint == name) { \hover } {
					if(matrix.asArray.any { |patch| patch[1] == name }) {
						\occupied
					} {
						\idle
					}
				}
			}
		};
		if(kind == \jack) {
			CQInk.drawJack(center, family, \day, endpointState, this.auraFor(name) ? 0)
		} {
			CQInk.drawSocket(center, family, \day, endpointState,
				bananaGrammar.handWeight);
			glyph !? { CQInk.drawGlyph(glyph, Rect(5, 6, 8, 8), palette[family]) };
			if(lamp) { CQInk.drawLamp(Point(25, 10), 0, false, \day) };
		};
		^this
	}
	currentSkin { ^shellState.viewMode }

	// One source's live magnitude, or nil while the instrument is silent about
	// it (headless, serverless preview, unbooted). Callers decide what "no
	// information" looks like; they must never invent a value for it.
	auraFor { |name|
		var index = CQMatrix.srcIndex(name);
		if(auraValues.isNil or: { index.isNil }) { ^nil };
		^auraValues[index]
	}

	// SPEC §7: persistent warning color while the mic is live (M2 GUI part)
	micNotice { |live|
		micLive = live;
		this.prUpdateNotice;
	}

	// ---- private ----

	prInit {
		audition = CQAudition(matrix);
		bananaGrammar = CQBananaGrammar(matrix, audition);
		shellState = CQGuiShellState(bananaGrammar);
		updaters = IdentityDictionary.new;
		meterFuncs = IdentityDictionary.new;
		deckLabels = IdentityDictionary.new;
		skipButtons = IdentityDictionary.new;
		spesalStates = Cocoquantus.spesalStates;
		castleValues = [0, 0, 0, 0, 0];
		inkControls = List.new;
		endpoints = IdentityDictionary.new;
		bananaBodies = IdentityDictionary.new;
		widgets = CQGuiWidgets(state, updaters, this);
		{
			if(cleaned.not and: { shellState.canBuild }) {
				this.prBuild;
				this.prListen;
				this.prStartRecTask;
				shellState.markOpen;
			};
		}.defer;
	}

	prBuild {
		var dayContent, dayView, nightView, masterView;
		var inspectorView, spellbookView, memoryView;
		var night = CQInk.palette(\night), minimum = shellState.minSize;
		dayContent = View().background_(CQInk.palette(\day)[\ground]).layout_(
			HLayout(
				CQGuiPanels.sources(this),
				CQGuiPanels.coco(this, 1),
				CQGuiPanels.quantussy(this),
				CQGuiPanels.coco(this, 2)
			).margins_(8).spacing_(8));
		cableOverlay = CQGuiCableOverlay(matrix, endpoints, bananaGrammar,
			{ |src| this.auraFor(src) });
		bananaController = CQGuiBananaController(
			bananaGrammar, endpoints, bananaBodies, cableOverlay, inkControls);
		bananaController.bindGround(dayContent);
		dayView = View().background_(CQInk.palette(\day)[\ground]).layout_(
			StackLayout(cableOverlay.view, dayContent).mode_(\stackAll));
		nightView = UserView().background_(night[\ground]);
		nightView.drawFunc = {
			var center = nightView.bounds.center, horizon = center.y + 34;
			Pen.strokeColor = night[\whisper];
			Pen.width = CQInk.strokes[\hairline];
			Pen.addArc(center - Point(0, 34), 82, 0.15pi, 0.7pi);
			Pen.stroke;
			Pen.stringCenteredIn("night map waits for the day grammar",
				Rect.aboutPoint(Point(center.x, horizon + 40), 220, 12),
				CQInk.font(\label), night[\whisper]);
		};
		masterView = CQGuiPanels.master(this);
		drawer = CQGuiDrawer(this, state, matrix, audition, bananaGrammar);
		shellState.audition = drawer;
		gridPanel = drawer.grid;
		inspectorView = drawer.inspectorView;
		spellbookView = drawer.spellbookView;
		memoryView = drawer.memoryView;
		shell = CQGuiShell(shellState, dayView, nightView, masterView,
			inspectorView, spellbookView, memoryView,
			{ |skin| this.prApplySkin(skin) });
		this.prApplySkin(shellState.viewMode);
		window = Window("Cocoquantus · Archipelago",
			Rect(50, 50, minimum[0], minimum[1]));
		// SuperCollider's Qt backend suppresses child mouseOverAction callbacks
		// unless the containing window explicitly enables mouse-over delivery.
		window.acceptsMouseOver = true;
		window.view.minSize = Size(minimum[0], minimum[1]);
		window.view.layout = VLayout(shell.view).margins_(0).spacing_(0);
		window.view.keyDownAction = { |v, char, modifiers, unicode, keycode, key|
			this.prKey(char, modifiers ? 0, unicode, keycode, key)
		};
		window.onClose = { shellState.close; this.prCleanup };
		window.view.children.do { |ch| this.prDefocus(ch) };
		window.front;
	}

	prDrawerPlaceholder { |label|
		var palette = CQInk.palette(\day);
		^StaticText().string_(label).align_(\center)
			.font_(CQInk.font(\notice)).stringColor_(palette[\whisper])
	}

	// Qt hands keyboard focus to any clicked widget, and space/tab then act on
	// that widget instead of the SPEC §6.2 key map (space would re-toggle a
	// clicked REC rather than panic). The panel is mouse-only: strip focus from
	// everything except text entry, which gets Qt::ClickFocus only — typing
	// works after a click, but they stay out of the tab chain so the window
	// never hands them initial focus at boot. Enter releases (see widgets).
	prDefocus { |view|
		if(view.isKindOf(TextField) or: { view.isKindOf(NumberBox) }) {
			view.setProperty(\focusPolicy, 2);   // Qt::ClickFocus
		} {
			view.canFocus = false;
		};
		view.children.do { |ch| this.prDefocus(ch) };
	}

	// one SimpleController fans \param notifications out to the widget
	// updaters; OSCdefs carry the server reports (meters, LEDs, spesal)
	prListen {
		stateCtl = SimpleController(state);
		stateCtl.put(\param, { |obj, what, name, val|
			{ this.prQueueParam(name, val) }.defer;
		});
		// momentary actions with no persistent state: skip fires a brief button flash
		stateCtl.put(\action, { |obj, what, action, n|
			if(action == \skip) { { this.prFlashSkip(n) }.defer };
		});
		OSCdef(\cq_gui_meters, { |msg|
			{ meterFuncs[\master].value(msg[3], msg[5]) }.defer;
		}, '/cq_meters');
		OSCdef(\cq_gui_gr, { |msg|
			{ meterFuncs[\gr].value(max(msg[3] ? 0, msg[4] ? 0)) }.defer;
		}, '/cq_gr');
		OSCdef(\cq_gui_coco, { |msg|
			var key = ("coco" ++ msg[2].asInteger).asSymbol;
			{ meterFuncs[key].value(msg[3], msg[5]) }.defer;
		}, '/cq_cocometers');
		OSCdef(\cq_gui_castle, { |msg|
			{
				castleValues = msg.copyRange(3, 7).collect { |value|
					(value ? 0).clip(-1, 1)
				};
				castleLeds !? {
					castleLeds.do { |led, i|
						led.value = ((castleValues[i] + 1) * 0.5).clip(0, 1);
					};
				};
				spesalView !? { spesalView.refresh };
			}.defer;
		}, '/cq_castle');
		// GUI_SPEC §9 `/cq_aura`: the server's own 15 Hz rate IS the §10 ambient
		// ceiling — no extra timer, no polling. The repaint touches only the 16
		// small jack views and the cable layer, never the whole window.
		OSCdef(\cq_gui_aura, { |msg|
			{
				auraValues = msg.copyRange(3, 18).collect { |value|
					(value ? 0).clip(0, 1)
				};
				this.prPaintAura;
			}.defer;
		}, '/cq_aura');
		OSCdef(\cq_gui_spesal, { |msg|
			{
				spesalStates = msg.copyRange(3, 7).collect { |v| v.asInteger };
				spesalView !? { spesalView.refresh };
			}.defer;
		}, '/cq_spesal');
		// watchdog notice (SPEC §5.6): same '/cq_hot' the facade acts on
		OSCdef(\cq_gui_hot, { { this.prHotNotice }.defer }, '/cq_hot');
		// deck file labels: dependants of the deck players (nil when unbooted,
		// e.g. the serverless smoke build — the labels then just keep their hint)
		#[\deckA, \deckB].do { |which|
			this.prDeckPlayer(which) !? { |deck|
				var ctl = SimpleController(deck);
				deck.path !? { this.prPaintDeckLabel(which, deck.path) };
				ctl.put(\file, { |obj, what, path|
					{ this.prPaintDeckLabel(which, path) }.defer;
				});
				deckCtls = deckCtls.add(ctl);
			};
		};
	}

	// GUI_SPEC §10 caps ambient redraw at 15 Hz, but a state stream carries no rate
	// limit of its own: a snapshot morph re-sets all 57 continuous parameters 30
	// times a second (CQSnapshots), repainting every bound control twice per allowed
	// frame. Paced here rather than at the source, because the morph's audio rate is
	// none of the GUI's business. The first change after a quiet moment still lands
	// at once, so a knob turn, a 16n fader and a code set stay instant; only a
	// continuous stream is paced, and it is paced by dropping intermediate frames,
	// never by dropping the last value.
	*paramFlushPeriod { ^1/15 }

	prQueueParam { |name, val|
		var wait;
		paramPending ?? { paramPending = IdentityDictionary.new };
		paramPending[name] = val;
		if(paramFlushScheduled) { ^this };
		wait = this.class.paramFlushPeriod - (Main.elapsedTime - lastParamFlush);
		if(wait <= 0) { ^this.prFlushParams };
		paramFlushScheduled = true;
		AppClock.sched(wait, { this.prFlushParams; nil });
	}

	prFlushParams {
		var pending = paramPending;
		paramFlushScheduled = false;
		lastParamFlush = Main.elapsedTime;
		paramPending = nil;
		if(cleaned or: { pending.isNil }) { ^this };
		pending.keysValuesDo { |name, val|
			updaters[name].do { |f| f.value(val) };
		};
	}

	// A still instrument must not cost redraws: repaint only when some source
	// actually moved (GUI_SPEC §16.9 — ambient motion is information, and there
	// is none in silence). Cables repaint only when there are cables to pulse.
	prPaintAura {
		if(cleaned) { ^this };
		if(auraPainted.notNil and: {
			auraValues.every { |value, i| (value - auraPainted[i]).abs < 0.01 }
		}) { ^this };
		auraPainted = auraValues;
		CQMatrix.sourceNames.do { |src|
			endpoints[src] !? { |endpoint| endpoint[\view].refresh };
		};
		cableOverlay !? { |overlay|
			// a patch too dense to pulse inside the frame budget draws static ink,
			// so an ambient frame must not repaint it either (auraCableLimit)
			if(overlay.cableCount > 0 and: { overlay.auraPulses }) {
				overlay.view.refresh
			};
		};
	}

	prDeckPlayer { |which|
		if(Cocoquantus.graph.isNil) { ^nil };
		^if(which == \deckA) { Cocoquantus.deckA } { Cocoquantus.deckB }
	}

	prPaintDeckLabel { |which, path|
		deckLabels[which] !? { |lab|
			lab.string = path.asString.basename;
			lab.stringColor = CQInk.palette(shellState.viewMode)[\line];
		};
	}

	// 2 Hz language-side tick for the REC button + elapsed display (SPEC §6.2)
	prStartRecTask {
		recTask = Task {
			loop {
				var r = Cocoquantus.recorder;
				var on = r.notNil and: { r.isRecording };
				recBtn !? { recBtn.value = on.binaryValue };
				recTimeView !? {
					recTimeView.string = if(on) {
						var secs = r.elapsed.asInteger;
						"%:%".format(secs div: 60, (secs % 60).asString.padLeft(2, "0"))
					} { "" };
				};
				0.5.wait;
			};
		}.play(AppClock);
	}

	// SPEC §6.2 keyboard map
	prKey { |char, modifiers = 0, unicode, keycode, key|
		var ascii, undoKey;
		ascii = char !? { char.ascii };
		undoKey = #[26, 90, 122].includes(ascii)
			or: { #[26, 90, 122].includes(unicode) }
			or: { key == 90 };
		if(modifiers.isCtrl and: { undoKey }) {
			bananaGrammar.undo;
			^this
		};
		if(char.notNil and: { char.ascii == 27 }) {
			drawer.escape;
			^this
		};
		if(char.notNil and: { char.ascii == 9 }) {
			shell.toggleView(this.prTextHasFocus);
			^this
		};
		switch(char,
			$1, { Cocoquantus.hold(1) },
			$2, { Cocoquantus.hold(2) },
			$q, { Cocoquantus.flip(1) },
			$w, { Cocoquantus.flip(2) },
			$a, { Cocoquantus.skip(1) },
			$s, { Cocoquantus.skip(2) },
			$r, {
				var r = Cocoquantus.recorder;
				if(r.notNil and: { r.isRecording })
					{ Cocoquantus.recStop } { Cocoquantus.rec };
			},
			$p, { this.shake },
			$ , {
				if((Cocoquantus.get(\monitorMute) ? 0) > 0)
					{ Cocoquantus.resume } { Cocoquantus.panic };
			},
			{
				if(char.isDecDigit and: { char.digit.inclusivelyBetween(3, 7) }) {
					Cocoquantus.spesal(char.digit - 2);
				};
			}
		);
	}

	prTextHasFocus {
		var focused = GUI.focusView;
		^focused.notNil and: {
			focused.isKindOf(TextField) or: { focused.isKindOf(NumberBox) }
		}
	}

	// composes the persistent mic warning (SPEC §7) and the transient watchdog
	// notice (SPEC §5.6); the hot notice self-clears ~12 s after the last event
	prUpdateNotice {
		var parts = [];
		if(micLive) { parts = parts.add("MIC LIVE — feedback risk with builtin speakers") };
		if(hotLive) {
			parts = parts.add(if(config[\autoTrim] == true) {
				"OUTPUT HOT — auto-trimmed −6 dB"
			} {
				"OUTPUT HOT (> 5 s)"
			});
		};
		noticeView !? { noticeView.string = parts.join(" · ") };
	}

	prHotNotice {
		hotLive = true;
		this.prUpdateNotice;
		hotTask !? { hotTask.stop };
		hotTask = Task { 12.wait; hotLive = false; this.prUpdateNotice }.play(AppClock);
	}

	// brief highlight on the SKIP button so code/pattern-driven skips are visible
	// (skip is momentary — no lasting state like flip's toggle)
	prFlashSkip { |n|
		var b = skipButtons[n];
		if(cleaned) { ^this };
		b !? { b.flash };
	}

	prApplySkin { |skin|
		var palette = CQInk.palette(skin);
		inkControls.do { |control| control.skin = skin };
		cableOverlay !? { cableOverlay.skin = skin };
		drawer !? { drawer.applySkin(skin) };
		noticeView !? { noticeView.stringColor = palette[\whisper] };
		recTimeView !? { recTimeView.stringColor = palette[\danger] };
		shell !? {
			this.prReinkText(shell.masterView, palette);
			this.prReinkText(shell.drawerView, palette);
		};
		^this
	}

	prReinkText { |view, palette|
		if(view.isKindOf(StaticText)) { view.stringColor = palette[\whisper] };
		if(view.isKindOf(TextField) or: { view.isKindOf(NumberBox) }
			or: { view.isKindOf(PopUpMenu) }) {
			view.stringColor = palette[\line];
			view.background = palette[\ground];
		};
		view.children.do { |child| this.prReinkText(child, palette) };
	}

	// idempotent teardown shared by free and the window's close box: language-
	// side listeners die here so a closed window leaves no dangling \cq_gui_*
	prCleanup {
		bananaGrammar !? { bananaGrammar.free };
		if(cleaned) { ^this };
		cleaned = true;
		shellState.close;
		stateCtl !? { stateCtl.remove };
		stateCtl = nil;
		deckCtls.do { |c| c.remove };
		deckCtls = nil;
		drawer !? { drawer.free };
		drawer = nil;
		gridPanel = nil;
		bananaController !? { bananaController.free };
		bananaController = nil;
		cableOverlay !? { cableOverlay.free };
		cableOverlay = nil;
		recTask !? { recTask.stop };
		recTask = nil;
		hotTask !? { hotTask.stop };
		hotTask = nil;
		#[cq_gui_meters, cq_gui_gr, cq_gui_coco, cq_gui_castle,
			cq_gui_spesal, cq_gui_hot, cq_gui_aura].do { |k|
			OSCdef.all[k] !? { |d| d.free }
		};
	}
}
