// CQGui — Yönpuoli's Night-only window (SPEC §6.2, ARCH §5): the Night
// landscape, Drawer workbench, persistent Master strip and global keyboard
// map. Pure view: CQState/CQMatrix notifications and namespaced OSC reports
// drive it; every write returns through the funnels. All UI work happens
// inside { }.defer; the only idle language timer is REC elapsed time.

CQGui {
	var state, matrix, config;
	var <window, <widgets, updaters, <shellState, <shell, <drawer;
	var <audioConfig;
	var stateCtl, serverCtl, clockCtl, gridPanel, meterFuncs, <audition;
	var <bananaGrammar, <nightMap;
	var <inkControls;
	var <spesalStates;
	// `/cq_phase`: read-head address, audio-rate direction mean and skip rate,
	// followed by the three continuous Coco destinations' reduced live truth
	// (§9). The first three remain nil until the server reports; the tiny
	// crescent views own the appended state themselves.
	var <phaseNorms, <phaseDirs, <phaseSkipHz;
	// AIO-4 caches raw hardware-input and post-safety master presence even while
	// its Drawer page is hidden. Listeners are paced and notified only while the
	// Audio tab is the visible workbench page.
	var <ioInputPresence, <masterPresence, audioPresenceListeners;
	var <audioPresenceNotifyCount = 0;
	var audioPresencePending = false, audioPresenceGeneration = 0;
	var lastAudioPresenceNotify = 0;
	// GUI_SPEC §9: 16 source magnitudes, nil until the server has actually
	// reported. No telemetry = no pulse, so serverless and headless surfaces
	// keep exactly the reviewed static Night look.
	var <auraValues, auraPainted;
	// GUI_SPEC §9 `/cq_charge`: nine paired pre-Schmitt charges and exact
	// post-Schmitt gate/trig fires, in three owner blocks. Same law as the aura:
	// each half of a slot stays nil until its synth has spoken.
	var <chargeValues, chargePainted, <fireValues, firePainted;
	var <chargeNames, chargeIndex;
	var deckLabels, deckNames, deckCtls;
	var noticeView, recBtn, recTimeView, recTask, hotTask;
	// §16.7: which drop target a file drag is hovering, the view that draws it,
	// and a function returning its global rect. One target at a time.
	var <dropTarget, dropTargetView, dropTargetRect;
	var dropTargetPolling = false, dropTargetArmed = 0;
	var micLive = false, hotLive = false, cleaned = false;
	var tabHeld = false;
	var paramPending, paramFlushScheduled = false, lastParamFlush = 0;
	// how much a shake sows (0..1), shared by the SHAKE button, the p key and the
	// night's sowing moon; GUI-local because shake is an action argument, not a
	// CQSpec param (SPEC §5.5 — the density clamp lives in CQMatrix.shake).
	// The default is a small crescent. Its visible phase remains linear, but
	// sowDensity waxes quadratically so early light can make a tiny patch.
	var <shakeAmount = 0.22;
	// ...and its own watcher list, deliberately NOT the param registry: that one
	// is keyed by CQSpec names and primes itself from CQState, which has no
	// opinion about a gesture constant (GUI_SPEC §10).
	var shakeWatchers;
	// the §7.1 weather machine's four matrix memories, loaded from
	// presets/compass.cqcompass.scd at GUI open (config gui.compassDir
	// overrides the home — the smokes point it at a tmp dir)
	var <compass;

	*new { |state, matrix, config|
		^super.newCopyArgs(state, matrix, config).prInit
	}

	// GUI_SPEC §9: the canonical `/cq_charge` channel order is the matrix
	// destination order filtered to gates and trigs — derived here rather than
	// re-typed, so the lamps can never drift from CQMatrix's own truth.
	*chargeNames {
		^CQMatrix.destNames.select { |name|
			var tail = name.asString;
			tail.endsWith("_flip") or: { tail.endsWith("_skip") }
				or: { tail.endsWith("_spesal") }
		}
	}

	*audioPresencePeriod { ^1 / 15 }

	// which slots of the nine a given SendReply block carries (§9 order):
	// the cocos report their own pair, the quantussy its five castles
	*chargeBlock { |replyId|
		^switch(replyId.asInteger, 1, { 0 }, 2, { 2 }, 3, { 4 }, { nil })
	}

	*chargeBlockSize { |replyId|
		^switch(replyId.asInteger, 1, { 2 }, 2, { 2 }, 3, { 5 }, { nil })
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
		if(drawer.notNil) { drawer.panic } { bananaGrammar !? { bananaGrammar.panic } };
		// Night's view-only cable hand and bodily gestures come off on the same
		// lifecycle paths as the Drawer's precision audition (§4.1): a held punch
		// is restored (a stuck one leaves a loop
		// recording, §2) and an uncommitted weather voyage goes home. This is the
		// path CQ.panic runs on the gui panel.
		nightMap !? { nightMap.cancelGestures };
	}
	// GUI_SPEC §16.6, decided with the user 2026-07-22: the shared shake amount
	// is **how much is sown**, not how hard. Strength already has a macro of its
	// own (`patchDepth`), so mapping the moon's lit area onto the weight ceiling
	// was both redundant and inaudible; density is reachable from nowhere else.
	// Quadratic fertility: quarter ≈ 1.25 %, half = 5 %, full = 20 %. The first
	// half stays sparse enough to play; the second half gathers force.
	*fullMoonDensity { ^0.2 }
	*defaultShakeAmount { ^0.22 }
	*sowDensity { |norm|
		^(norm ? 0).clip(0, 1).squared * this.fullMoonDensity
	}

	shake {
		bananaGrammar.performUndoable({
			// the weight stays the facade's documented default: the phase says
			// how much, patchDepth says how alive
			Cocoquantus.shake(origin: \gui,
				density: this.class.sowDensity(shakeAmount))
		});
		^this
	}

	// One amount for every shake entrance: the Night moon shows it, while the
	// p key and code may trigger it. Every entry clamps, here as everywhere.
	shakeAmount_ { |value|
		var next = (value ? 0).asFloat.clip(0, 1);
		if(next == shakeAmount) { ^this };
		shakeAmount = next;
		shakeWatchers.do { |func| func.value(next) };
		^this
	}

	// a watcher is painted at once with the value it is joining, exactly as the
	// param registry primes its widgets
	onShakeAmount { |func|
		shakeWatchers.add(func);
		func.value(shakeAmount);
		^func
	}

	// ---- view registration API ----

	setMeter { |key, func| meterFuncs[key] = func }
	addAudioPresenceListener { |owner, func|
		audioPresenceListeners[owner] = func;
		if(this.audioPageVisible) { this.prQueueAudioPresence };
		^func
	}
	removeAudioPresenceListener { |owner|
		audioPresenceListeners.removeAt(owner);
		^this
	}
	audioPresenceListenerCount { ^audioPresenceListeners.size }
	// One definition of "the hand can see this page", so a page cannot count as
	// hidden for one purpose and visible for another.
	drawerPageVisible { |tab|
		^shellState.drawerOpen and: { shellState.drawerTab == tab }
	}
	audioPageVisible { ^this.drawerPageVisible(\audio) }
	audioInputPresenceFrame { |values|
		ioInputPresence = this.prPresenceValues(values, 3);
		this.prQueueAudioPresence;
		^this
	}
	masterPresenceFrame { |values|
		masterPresence = this.prPresenceValues(values, 2);
		this.prQueueAudioPresence;
		^this
	}
	setDeckLabel { |which, view, ink|
		deckLabels[which] = (deckLabels[which] ?? { List.new }).add(
			(view: view, ink: ink ?? { CQInk.palette(\night)[\line] }));
	}
	deckLabel { |which| ^deckLabels[which] !? { |list| list.first[\view] } }
	notice_ { |view| noticeView = view }
	recButton_ { |btn| recBtn = btn }
	recTime_ { |view| recTimeView = view }
	registerInkControl { |control| inkControls.add(control); ^control }
	endpointInkState { |name, kind|
		if(kind == \jack) {
			^if(bananaGrammar.isSourceInHand(name)) { \inHand } {
				if(bananaGrammar.hoverEndpoint == name) { \hover } { \idle }
			}
		};
		if(bananaGrammar.isCandidate(name)) { ^\candidate };
		// A destination that cannot act does not promise an action on hover.
		// Once a banana is in hand, every legal destination answers with one
		// quiet static halo; the nearest/hovered candidate still grows stronger.
		if(bananaGrammar.isInHand) {
			^if(bananaGrammar.hoverEndpoint == name) { \hover } { \available }
		};
		^if(matrix.asArray.any { |patch| patch[1] == name }) {
			\occupied
		} {
			\idle
		}
	}

	drawEndpoint { |name, kind, center, family, glyph, lamp = false, skin = \night|
		var palette = CQInk.palette(skin);
		var endpointState = this.endpointInkState(name, kind);
		var inkScale = if(skin == \night) {
			CQNightMeasures.endpointInkScale
		} {
			1
		};
		if(kind == \jack) {
			CQInk.drawJack(center, family, skin, endpointState,
				this.auraFor(name) ? 0, inkScale)
		} {
			CQInk.drawSocket(center, family, skin, endpointState,
				bananaGrammar.handWeight, inkScale);
			glyph !? {
				var glyphHalf = if(skin == \night) {
					CQNightMeasures.endpointGlyphExtent * 0.5
				} {
					4
				};
				CQInk.drawGlyph(glyph,
					Rect.aboutPoint(center, glyphHalf, glyphHalf), palette[family])
			};
			if(lamp) {
				var charge = this.chargeFor(name) ? 0;
				var fired = this.fireFor(name) ? 0;
				// Amber fill is post-weight, pre-Schmitt charge. The separate pale
				// flare is held from the exact gate/trig the DSP actually obeyed.
				CQInk.drawLamp(center + Point(
					if(skin == \night) { CQNightMeasures.lampOffset } { 16 }, 0),
					charge, fired > 0.01, skin, inkScale, fired)
			};
		};
		^this
	}
	currentSkin { ^\night }

	// One source's live magnitude, or nil while the instrument is silent about
	// it (headless, serverless preview, unbooted). Callers decide what "no
	// information" looks like; they must never invent a value for it.
	auraFor { |name|
		var index = CQMatrix.srcIndex(name);
		if(auraValues.isNil or: { index.isNil }) { ^nil };
		^auraValues[index]
	}

	// §16.7 drop target. Qt hands sclang no drag-LEAVE callback and — measured
	// on a real file drag, 2026-07-22 — asks `canReceiveDrag` only once per
	// dragEnter, never per move. So the predicate can arm the border but can
	// never release it, and a timed release put the border out while the hand
	// was still holding the file over the box: the affordance failed exactly
	// while it was being read. The border is therefore held by *where the
	// cursor is*: a light poll hit-tests `QtGUI.cursorPosition` against the
	// target's own global rect — the same rect the border is drawn on, so cue
	// and action share one geometry (§4.5, again). Two independent exits, so
	// it can never stick: the cursor leaving, or the drag itself ending.
	// The poll exists only while a file is actually being dragged over the
	// instrument; it is hover tracking, not ambient motion (§16.9).
	*dropTargetPoll { ^0.08 }
	// only the fallback if the platform has no cursor to give us
	*dropTargetHold { ^0.4 }

	// pure: does the border stay up? nil cursor = the platform would not say,
	// and then the border keeps its blind hold rather than flickering off
	*dropTargetKeeps { |rect, cursor, dragging = true, held = 0|
		if(dragging.not or: { rect.isNil }) { ^false };
		if(cursor.isNil) { ^held < this.dropTargetHold };
		^rect.containsPoint(cursor)
	}

	dropTargetIs { |name| ^dropTarget == name }

	dropTargetHover { |name, view, rectFunc|
		if(cleaned) { ^this };
		if(dropTarget != name) {
			var previous = dropTargetView;
			dropTarget = name;
			dropTargetView = view;
			this.prRefreshDropTarget(previous);
			this.prRefreshDropTarget(view);
		};
		dropTargetRect = rectFunc;
		dropTargetArmed = Main.elapsedTime;
		this.prDropWatch;
		^this
	}

	dropTargetClear {
		var previous = dropTargetView;
		if(dropTarget.notNil) {
			dropTarget = nil;
			dropTargetView = nil;
			dropTargetRect = nil;
			if(cleaned.not) { this.prRefreshDropTarget(previous) };
		};
		^this
	}

	prRefreshDropTarget { |target|
		if(target.isKindOf(SequenceableCollection)) {
			target.do { |view| view !? { |v| v.refresh } };
		} {
			target !? { |view| view.refresh };
		};
		^this
	}

	prDropWatch {
		if(dropTargetPolling) { ^this };
		dropTargetPolling = true;
		AppClock.sched(this.class.dropTargetPoll, {
			var keep = false;
			if(cleaned.not and: { dropTarget.notNil }) {
				keep = this.class.dropTargetKeeps(
					try { dropTargetRect.value },
					try { QtGUI.cursorPosition },
					View.currentDrag.notNil,
					Main.elapsedTime - dropTargetArmed);
			};
			if(keep) {
				this.class.dropTargetPoll
			} {
				dropTargetPolling = false;
				this.dropTargetClear;
				nil
			}
		});
		^this
	}

	// One gate/trig destination's live charge (post-weight, pre-Schmitt), or nil
	// while its synth has said nothing — the §9 law again: a dark lamp because
	// nobody reported is not a lamp reporting zero.
	chargeFor { |dest|
		var index = chargeIndex[dest];
		if(index.isNil) { ^nil };
		^chargeValues[index]
	}

	// The exact post-Schmitt gate or trigger, peak-held on the server so a
	// single-sample event reaches the 15 Hz eye. This is intentionally separate
	// from `chargeFor`: threshold proximity and an event are different facts.
	fireFor { |dest|
		var index = chargeIndex[dest];
		if(index.isNil) { ^nil };
		^fireValues[index]
	}

	// SPEC §7: persistent warning color while the mic is live (M2 GUI part).
	// Since N8 the one interactive mic control lives on the Workshop page, but
	// this composition stays central: the notice belongs to the always-visible
	// Master strip, so closing the Drawer can never silence the warning.
	micNotice { |live|
		micLive = live;
		this.prUpdateNotice;
	}

	noticeText { ^noticeView !? { |view| view.string } }

	// ---- private ----

	prInit {
		audioConfig = Cocoquantus.audioConfigForGui(config);
		audition = CQAudition(matrix);
		bananaGrammar = CQBananaGrammar(matrix);
		shellState = CQGuiShellState(bananaGrammar);
		updaters = IdentityDictionary.new;
		meterFuncs = IdentityDictionary.new;
		deckLabels = IdentityDictionary.new;
		deckNames = IdentityDictionary.new;
		spesalStates = Cocoquantus.spesalStates;
		phaseNorms = [nil, nil];   // nil until '/cq_phase' reports (§9: no telemetry ≠ 0)
		phaseDirs = [nil, nil];
		phaseSkipHz = [nil, nil];
		ioInputPresence = [nil, nil, nil];
		masterPresence = [nil, nil];
		audioPresenceListeners = IdentityDictionary.new;
		chargeNames = this.class.chargeNames;
		chargeIndex = IdentityDictionary.new;
		chargeNames.do { |name, i| chargeIndex[name] = i };
		chargeValues = Array.newClear(chargeNames.size);   // same law as the aura
		chargePainted = Array.newClear(chargeNames.size);
		fireValues = Array.newClear(chargeNames.size);
		firePainted = Array.newClear(chargeNames.size);
		inkControls = List.new;
		shakeWatchers = List.new;
		compass = CQCompass(this.prCompassDir);
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
		var masterView;
		var minimum = shellState.minSize;
		nightMap = CQGuiNightMap(this, state, matrix, config);
		masterView = CQGuiPanels.master(this);
		drawer = CQGuiDrawer(this, state, matrix, audition, bananaGrammar,
			audioConfig);
		shellState.audition = drawer;
		gridPanel = drawer.grid;
		shell = CQGuiShell(shellState, nightMap.stageView, masterView,
			drawer.pages, { |skin| this.prApplySkin(skin) },
			{ this.drawerShown });
		// The workbench reads at two reviewed sizes (CQGuiScale): a windowed
		// 1280x720 gives a Drawer page 1272x526, a maximized window far more,
		// and a page that is nothing but text should spend that on legibility
		// rather than on slate. Qt reports a new rect straight into the
		// language, so the step follows the window with no timer and no polling.
		//
		// Two hooks, not one, because either alone has a hole: the drawer stage
		// is a StackLayout page, and a hidden page is not laid out — maximizing
		// with the Drawer closed would otherwise leave a stale size behind. The
		// window's own view always resizes, and reopening the Drawer re-asks.
		shell.drawerView.onResize = { this.prSyncDrawerScale };
		// A GUI built while the clock is already stuck must not open clean: the
		// guard's state is older than this window.
		shell.audioAlert = Cocoquantus.audioClockAlert;
		this.prApplySkin(\night);
		window = Window("Yönpuoli",
			Rect(50, 50, minimum[0], minimum[1]));
		// SuperCollider's Qt backend suppresses child mouseOverAction callbacks
		// unless the containing window explicitly enables mouse-over delivery.
		window.acceptsMouseOver = true;
		window.view.minSize = Size(minimum[0], minimum[1]);
		window.view.layout = VLayout(shell.view).margins_(0).spacing_(0);
		window.view.keyDownAction = { |v, char, modifiers, unicode, keycode, key|
			this.prKey(char, modifiers ? 0, unicode, keycode, key)
		};
		window.view.keyUpAction = { |v, char, modifiers, unicode, keycode, key|
			var ascii = char !? { char.ascii };
			if(ascii == 9 or: { unicode == 9 }) { tabHeld = false };
		};
		// the window's own view always resizes, even when the drawer stage is
		// the hidden StackLayout page (see the two-hook note above)
		window.view.onResize = { this.prSyncDrawerScale };
		window.view.keyModifiersChangedAction = { |v, modifiers|
			nightMap !? { |map|
				map.modifierTideFocus_((modifiers ? 0).isShift)
			}
		};
		window.onClose = { shellState.close; this.prCleanup };
		window.view.children.do { |ch| this.prDefocus(ch) };
		window.front;
		if(Cocoquantus.consumeAudioReopen) {
			shell.selectDrawer(\audio);
			if(shellState.drawerOpen.not) { shell.toggleDrawer };
		};
	}

	// Re-ask the Drawer which size step its stage now deserves. Deferred by one
	// GUI tick because a resize callback can arrive before Qt has finished
	// laying the stage out, and the stale bounds would pick the wrong step.
	// `applyScale` early-outs when nothing moved, so calling this often is free.
	prSyncDrawerScale {
		if(cleaned or: { drawer.isNil } or: { shell.isNil }) { ^this };
		AppClock.sched(0, {
			var stage, moved = false;
			if(cleaned.not and: { drawer.notNil } and: { shell.notNil }) {
				stage = shell.drawerView.bounds;
				moved = drawer.applyScale(stage.width, stage.height);
			};
			// Qt can settle a layout over more than one pass — re-measuring the
			// stage may itself change it — so a step that moved asks once more
			// and stops as soon as nothing does. It converges by construction:
			// the second pass returns false unless the size really changed again.
			if(moved) { this.prSyncDrawerScale };
			nil
		});
		^this
	}

	// the Drawer became visible (or changed page): its stage may have been
	// resized while it was hidden and therefore never laid out
	drawerShown {
		this.prSyncDrawerScale;
		this.prQueueAudioPresence;
		// a page that skipped drawing while it was hidden catches up here
		drawer !? { |page| page.pageShown };
		^this
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
	prPresenceValues { |values, size|
		values = values ? [];
		^Array.fill(size, { |index|
			values[index] !? { |value| value.asFloat.abs }
		})
	}

	// `/cq_meters` arrives at 20 Hz and `/cq_io_inputs` at 15 Hz. Coalesce both
	// caches onto one page notification stream so their sum can never make the
	// Audio workbench paint faster than its 15 Hz ambient ceiling.
	prQueueAudioPresence {
		var wait, generation;
		if(cleaned or: { this.audioPageVisible.not }) { ^this };
		if(audioPresencePending) { ^this };
		wait = (this.class.audioPresencePeriod
			- (Main.elapsedTime - lastAudioPresenceNotify)).max(0);
		audioPresencePending = true;
		audioPresenceGeneration = audioPresenceGeneration + 1;
		generation = audioPresenceGeneration;
		AppClock.sched(wait, {
			if(generation == audioPresenceGeneration) {
				audioPresencePending = false;
				if(cleaned.not and: { this.audioPageVisible }) {
					lastAudioPresenceNotify = Main.elapsedTime;
					this.prNotifyAudioPresence;
				};
			};
			nil
		});
		^this
	}

	prNotifyAudioPresence {
		var inputs = ioInputPresence.copy, master = masterPresence.copy;
		var gains = [state.get(\liveGain), state.get(\micGain)];
		audioPresenceNotifyCount = audioPresenceNotifyCount + 1;
		audioPresenceListeners.keysValuesDo { |owner, func|
			func.value(inputs, master, gains)
		};
		^this
	}

	prListen {
		stateCtl = SimpleController(state);
		stateCtl.put(\param, { |obj, what, name, val|
			{
				this.prQueueParam(name, val);
				if(#[liveGain, micGain].includes(name)) {
					this.prQueueAudioPresence
				};
			}.defer;
		});
		// SOAK-1 finding A: the dense write's single announcement. One deferred
		// closure for the whole frame instead of one per parameter — the queue
		// below is the same, so widgets still repaint at §10's paced rate.
		stateCtl.put(\params, { |obj, what, event|
			{
				var presence = false;
				event.keysValuesDo { |name, val|
					this.prQueueParam(name, val);
					if(#[liveGain, micGain].includes(name)) { presence = true };
				};
				if(presence) { this.prQueueAudioPresence };
			}.defer;
		});
		// Momentary actions with no persistent state light their Night marks.
		stateCtl.put(\action, { |obj, what, action, n|
			if(action == \skip) { { this.prFlashSkip(n) }.defer };
			// Any shake (`p`, code, pattern) flashes the sowing moon.
			if(action == \shake) { { this.prFlashShake }.defer };
		});
		// AUD-W: the runtime clock guard announces only when the *visible* truth
		// changes, so this listener costs nothing while the clock is healthy.
		clockCtl = SimpleController(Cocoquantus);
		clockCtl.put(\audioClock, { |obj, what, alert|
			{
				// The retraction can arrive from a session teardown, so this
				// deferred block can outlive the views it writes to.
				if(cleaned.not) {
					// The fault can strike with the Drawer closed, so the always
					// visible toolbar carries it too (AUD-W, user decision
					// 2026-07-31); the page states the measured rate.
					shell !? { |current| current.audioAlert = alert };
					drawer !? {
						drawer.audioSetup !? { |page| page.serverHealthChanged }
					};
				};
			}.defer;
		});
		Cocoquantus.server !? { |currentServer|
			serverCtl = SimpleController(currentServer);
			serverCtl.put(\serverRunning, {
				{
					drawer !? {
						drawer.audioSetup !? { |page| page.serverHealthChanged }
					}
				}.defer;
			});
		};
		OSCdef(\cq_gui_meters, { |msg|
			{
				this.masterPresenceFrame([msg[3], msg[5]]);
				meterFuncs[\master].value(msg[3], msg[5]);
			}.defer;
		}, '/cq_meters');
		OSCdef(\cq_gui_io_inputs, { |msg|
			{ this.audioInputPresenceFrame([msg[3], msg[5], msg[7]]) }.defer;
		}, '/cq_io_inputs');
		OSCdef(\cq_gui_gr, { |msg|
			{ meterFuncs[\gr].value(max(msg[3] ? 0, msg[4] ? 0)) }.defer;
		}, '/cq_gr');
		OSCdef(\cq_gui_coco, { |msg|
			var key = ("coco" ++ msg[2].asInteger).asSymbol;
			{ meterFuncs[key].value(msg[3], msg[5]) }.defer;
		}, '/cq_cocometers');
		OSCdef(\cq_gui_castle, { |msg|
			var rates = if(msg.size >= 13) { msg.copyRange(8, 12) } { nil };
			{ nightMap !? { |map| map.castleFrame(msg.copyRange(3, 7), rates) } }.defer;
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
		// GUI_SPEC §9 `/cq_charge`: three 15 Hz blocks (coco 1, coco 2, the five
		// castles) fill paired nine-slot charge/fire caches. The send rate is the
		// §10 ambient ceiling; the repaint gate below keeps a still panel free.
		OSCdef(\cq_gui_charge, { |msg|
			{ this.prChargeFrame(msg[2], msg.copyRange(3, msg.size - 1)) }.defer;
		}, '/cq_charge');
		OSCdef(\cq_gui_spesal, { |msg|
			{
				spesalStates = msg.copyRange(3, 7).collect { |v| v.asInteger };
				nightMap !? { nightMap.spesalChanged };
			}.defer;
		}, '/cq_spesal');
		// GUI_SPEC §9 loop phase: the read head's own 15 Hz message. Night gives
		// the comet its own cheap layer. The appended continuous-destination
		// triples feed only their three tiny crescents, so no complete shell follows
		// this stream.
		OSCdef(\cq_gui_phase, { |msg|
			var values, spreads, activities;
			if(msg.size >= 15) {
				values = msg.copyRange(6, 8);
				spreads = msg.copyRange(9, 11);
				activities = msg.copyRange(12, 14);
			};
			{
				this.prPhaseFrame(msg[2], msg[3], msg[4], msg[5],
					values, spreads, activities)
			}.defer;
		}, '/cq_phase');
		// watchdog notice (SPEC §5.6): same '/cq_hot' the facade acts on
		OSCdef(\cq_gui_hot, { { this.prHotNotice }.defer }, '/cq_hot');
		// deck file labels: dependants of the deck players (nil when unbooted,
		// e.g. the serverless smoke build — the labels then just keep their hint)
		#[\deckA, \deckB].do { |which|
			this.prDeckPlayer(which) !? { |deck|
				var ctl = SimpleController(deck);
				deck.path !? { this.prPaintDeckLabel(which, deck.path) };
				ctl.put(\file, { |obj, what, path|
					{
						this.prPaintDeckLabel(which, path);
						nightMap !? { |map| map.deckLoaded(which) };
					}.defer;
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

	// A still instrument must not cost redraws: Night repaints only when some
	// source actually moved (GUI_SPEC §16.9).
	prPaintAura {
		if(cleaned) { ^this };
		if(auraPainted.notNil and: {
			auraValues.every { |value, i| (value - auraPainted[i]).abs < 0.01 }
		}) { ^this };
		auraPainted = auraValues;
		nightMap !? { nightMap.auraFrame(auraValues) };
		^this
	}

	// One '/cq_charge' block. Each Night lamp repaints only when its own charge
	// or exact fire reading moved; an unchanged held gate costs nothing.
	prChargeFrame { |replyId, values|
		var base, count, charges, fires;
		if(cleaned) { ^this };
		base = this.class.chargeBlock(replyId ? 0);
		count = this.class.chargeBlockSize(replyId ? 0);
		if(base.isNil or: { count.isNil }) { ^this };
		charges = values.keep(count);
		fires = if(values.size >= (count * 2)) {
			values.copyRange(count, (count * 2) - 1)
		} {
			Array.fill(count, 0)
		};
		count.do { |i|
			var slot = base + i, previousCharge, previousFire, charge, fire;
			if(slot < chargeValues.size) {
				charge = (charges.at(i) ? 0).asFloat.clip(0, 1);
				fire = (fires.at(i) ? 0).asFloat.clip(0, 1);
				previousCharge = chargePainted[slot];
				previousFire = firePainted[slot];
				chargeValues[slot] = charge;
				fireValues[slot] = fire;
				if(previousCharge.isNil or: { previousFire.isNil }
					or: { (charge - previousCharge).abs >= 0.02 }
					or: { (fire - previousFire).abs >= 0.02 }) {
					chargePainted[slot] = charge;
					firePainted[slot] = fire;
					this.prPaintLamp(chargeNames[slot]);
				};
			};
		};
	}

	prPaintLamp { |name|
		nightMap !? { |map| map.chargeChanged(name) };
		^this
	}

	// One '/cq_phase' frame: cache the clamped truth. The Night map's own
	// delta gate thins a near-still reel.
	prPhaseFrame { |n, value, dir, skipHz, modValues, modSpreads, modActivities|
		var i;
		if(cleaned) { ^this };
		i = (n ? 1).asInteger.clip(1, 2) - 1;
		phaseNorms[i] = (value ? 0).asFloat.clip(0, 1);
		// an older frame without the direction value leaves the last one alone
		dir !? { phaseDirs[i] = dir.asFloat.clip(-1, 1) };
		skipHz !? { phaseSkipHz[i] = (skipHz.asFloat.max(0)) };
		nightMap !? {
			nightMap.paintPhase(i);
			nightMap.crescentModulationFrame(i, modValues, modSpreads,
				modActivities);
		};
	}

	prDeckPlayer { |which|
		if(Cocoquantus.graph.isNil) { ^nil };
		^if(which == \deckA) { Cocoquantus.deckA } { Cocoquantus.deckB }
	}

	// A filename is centered under its bird (§16.6), so an overflow eats BOTH
	// ends and leaves neither the name nor the extension readable. §16.4 fixes
	// the type size, so the string gives way instead — trimmed from its tail
	// only, which is the expendable end, and marked with an ellipsis.
	*fitLabel { |string, width, measure|
		var text = string.asString, limit = width - 6, low, high, best;
		// before layout the box has no useful width yet: never trim to nothing
		if(width < 40 or: { measure.value(text) <= limit }) { ^text };
		low = 0;
		high = text.size;
		best = 0;
		while { low <= high } {
			var mid = (low + high) div: 2;
			if(measure.value(text.keep(mid) ++ "…") <= limit) {
				best = mid;
				low = mid + 1;
			} {
				high = mid - 1;
			};
		};
		^text.keep(best) ++ "…"
	}

	*prTextWidth { |string, font|
		^try { string.bounds(font).width } { string.size * font.size * 0.62 }
	}

	// re-fits every deck label from the raw name it was last given: a re-layout
	// can widen or narrow the box, and a name cut for the old one would stay cut
	refreshDeckLabels {
		deckNames.keysValuesDo { |which, name| this.prPaintDeckLabel(which, name) };
	}

	prPaintDeckLabel { |which, path|
		var name = path.asString.basename;
		var font = CQInk.font(\number);
		deckNames[which] = name;
		deckLabels[which].do { |record|
			var view = record[\view];
			view.string = this.class.fitLabel(name, view.bounds.width,
				{ |candidate| this.class.prTextWidth(candidate, font) });
			view.stringColor = record[\ink];
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
		var ascii, undoKey, handDownKey, handUpKey;
		ascii = char !? { char.ascii };
		undoKey = #[26, 90, 122].includes(ascii)
			or: { #[26, 90, 122].includes(unicode) }
			or: { key == 90 };
		handDownKey = #[90, 122].includes(ascii)
			or: { #[90, 122].includes(unicode) } or: { key == 90 };
		handUpKey = #[88, 120].includes(ascii)
			or: { #[88, 120].includes(unicode) } or: { key == 88 };
		if(modifiers.isCtrl and: { undoKey }) {
			bananaGrammar.undo;
			^this
		};
		// Laptop path for the held-cable attenuator. A touchpad commonly cannot
		// scroll while its click is held, so the leftmost Z/X pair mirrors wheel
		// down/up. Shift is the 0.01 fine step; plain keys use 0.05. Ctrl+Z keeps
		// its existing undo priority above this branch.
		if(bananaGrammar.isInHand and: { handDownKey or: { handUpKey } }) {
			bananaGrammar.wheelHand(if(handDownKey) { -1 } { 1 }, modifiers.isShift);
			^this
		};
		if(char.notNil and: { char.ascii == 27 }) {
			drawer.escape;
			nightMap !? { nightMap.cancelGestures };
			^this
		};
		if(ascii == 9 or: { unicode == 9 }) {
			// Native key repeat must not open/close a toggle several times
			// while one physical Tab press is held.
			if(tabHeld) { ^this };
			tabHeld = true;
			// Night owns Tab: it opens/closes the same persistent water focus
			// as the tide gill. Text entry and the Drawer keep it to themselves.
			if(this.prTextHasFocus.not
				and: { shellState.drawerOpen.not }) {
				nightMap !? {
					nightMap.cancelGestures;
					nightMap.toggleTideFocus
				};
			};
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

	// where the compass memories live: config gui.compassDir wins, otherwise the
	// presets folder (relative paths resolve against the repo, exactly as the
	// facade resolves them). nil = memory only, which is what a bare test build
	// without folders gets.
	prCompassDir {
		var override = config[\gui] !? { |g| g[\compassDir] };
		var folder;
		override !? { ^override.asString };
		folder = config[\folders] !? { |f| f[\presets].asString };
		if(folder.isNil) { ^nil };
		^if(PathName(folder).isAbsolutePath) { folder } {
			Cocoquantus.repoDir +/+ folder
		}
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

	// A skip from any layer lights the shell's stationary mouth mark.
	prFlashSkip { |n|
		if(cleaned) { ^this };
		nightMap !? { |map| map.flashSkip(n) };
	}

	// the sowing moon's own momentary light (§16.6): the amount it wears is
	// what was just thrown, so the flash reads as the throw itself
	prFlashShake {
		if(cleaned) { ^this };
		nightMap !? { |map| map.flashShake };
	}

	// Night is the only runtime skin. One hook still keeps the Drawer and
	// Master on the same palette choke point.
	prApplySkin { |skin|
		var palette = CQInk.palette(\night);
		inkControls.do { |control| control.skin = \night };
		drawer !? { drawer.applySkin(\night) };
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
		if(view.isKindOf(TextField) or: { view.isKindOf(NumberBox) }) {
			// one door for entry ink, so hover and selection colours survive a
			// re-ink instead of reverting to the OS default (2026-07-30).
			// Night is the only runtime skin, as prApplySkin declares.
			CQInk.applyTextEntryInk(view, \night)
		};
		if(view.isKindOf(PopUpMenu)) {
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
		serverCtl !? { serverCtl.remove };
		serverCtl = nil;
		clockCtl !? { clockCtl.remove };
		clockCtl = nil;
		deckCtls.do { |c| c.remove };
		deckCtls = nil;
		drawer !? { drawer.free };
		drawer = nil;
		gridPanel = nil;
		nightMap !? { nightMap.free };
		nightMap = nil;
		recTask !? { recTask.stop };
		recTask = nil;
		hotTask !? { hotTask.stop };
		hotTask = nil;
		audioPresenceGeneration = audioPresenceGeneration + 1;
		audioPresencePending = false;
		audioPresenceListeners.clear;
		#[cq_gui_meters, cq_gui_gr, cq_gui_coco, cq_gui_castle,
			cq_gui_spesal, cq_gui_hot, cq_gui_aura, cq_gui_phase,
			cq_gui_charge, cq_gui_io_inputs].do { |k|
			OSCdef.all[k] !? { |d| d.free }
		};
	}
}
