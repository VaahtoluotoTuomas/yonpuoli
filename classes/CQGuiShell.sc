// CQGuiShell — Qt composition for Yönpuoli's Night-only instrument and the
// Drawer workbench. Night and Drawer share the central stage; the toolbar and
// Master remain persistent. The shell never touches musical state or server
// objects.

CQGuiShell {
	var state;
	var <view, <nightView, <masterView, <drawerView;
	var <workspaceStack, <drawerButton;
	var <infoButton, <>infoAction;
	var <infoGuideButton, <>infoGuideAction;
	var <infoAdvancedButton, <>infoAdvancedAction;
	var <infoShortcutsButton, <>infoShortcutsAction;
	var drawerStack, drawerPages, toolbarView, toolbarTitle;
	var tabButtons, skinAction, shownAction;
	var toolbarInset = 6;
	var infoControlsVisible = false;
	var <audioAlert = \none;

	*new { |state, nightView, masterView, pages, skinAction, shownAction|
		^super.new.prInit(state, nightView, masterView, pages,
			skinAction, shownAction)
	}

	prInit { |shellState, nightContent, masterContent, pages,
		applySkin, onDrawerShown|
		var night = CQInk.palette(\night), tabRow;
		// The toolbar is persistent chrome: one type size, not the Drawer's
		// steps, because its height and muscle-memory layout remain fixed.
		var chrome = CQGuiScale.chrome;
		state = shellState;
		skinAction = applySkin;
		shownAction = onDrawerShown;
		nightView = nightContent;
		masterView = masterContent;
		drawerButton = CQInkControl.button("drawer",
			CQGuiScale.buttonWidth(\compact, "drawer", true), \night, false,
			false, \line, \collapse).surface_(\recess)
			.action_({ this.toggleDrawer });
		drawerButton.scale = chrome;
		drawerButton.view.fixedSize = Size(
			CQGuiScale.buttonWidth(\compact, "drawer", true), chrome[\buttonHeight]);
		infoButton = CQInkControl.button("info",
			CQGuiScale.buttonWidth(\compact, "info"), \night, true,
			false, \line).surface_(\recess).action_({ infoAction.value });
		infoButton.scale = chrome;
		infoButton.view.fixedSize = Size(
			CQGuiScale.buttonWidth(\compact, "info"), chrome[\buttonHeight]);
		infoGuideButton = CQInkControl.button(CQInfoTour.label,
			CQGuiScale.buttonWidth(\compact, CQInfoTour.label), \night)
			.surface_(\recess).action_({ infoGuideAction.value });
		infoGuideButton.scale = chrome;
		infoGuideButton.view.fixedSize = Size(
			CQGuiScale.buttonWidth(\compact, CQInfoTour.label), chrome[\buttonHeight]);
		infoGuideButton.view.visible = false;
		infoAdvancedButton = CQInkControl.button(CQInfoAdvanced.label,
			CQGuiScale.buttonWidth(\compact, CQInfoAdvanced.label), \night)
			.surface_(\recess).action_({ infoAdvancedAction.value });
		infoAdvancedButton.scale = chrome;
		infoAdvancedButton.view.fixedSize = Size(
			CQGuiScale.buttonWidth(\compact, CQInfoAdvanced.label), chrome[\buttonHeight]);
		infoAdvancedButton.view.visible = false;
		infoShortcutsButton = CQInkControl.button("shortcuts",
			CQGuiScale.buttonWidth(\compact, "shortcuts"), \night)
			.surface_(\recess).action_({ infoShortcutsAction.value });
		infoShortcutsButton.scale = chrome;
		infoShortcutsButton.view.fixedSize = Size(
			CQGuiScale.buttonWidth(\compact, "shortcuts"), chrome[\buttonHeight]);
		infoShortcutsButton.view.visible = false;
		toolbarTitle = StaticText().string_("yölahti")
			.font_(CQInk.font(\label, chrome[\label])).stringColor_(night[\line])
			.align_(\left).fixedWidth_(
				(CQGuiScale.textWidth("yölahti", chrome[\label]) + 16)
					.ceil.asInteger);
		toolbarView = UserView()
			.fixedHeight_(chrome[\buttonHeight] + (2 * toolbarInset))
			.background_(night[\recess]);
		toolbarView.drawFunc = {
			var y = toolbarView.bounds.height - 1, inset = toolbarInset;
			Pen.strokeColor = CQInk.palette(\night)[\whisper];
			Pen.width = CQInk.strokes[\hairline];
			Pen.line(Point(inset, y), Point(toolbarView.bounds.width - inset, y));
			Pen.stroke;
			// AUD-W's always-visible surface (user decision 2026-07-31). The mark
			// sits beside the info/drawer group, clear of both buttons. Drawer
			// remains the route to the Audio remedy (GUI_SPEC §4.5). It is
			// static ink: no blink, no pulse, and it exists only in danger.
			if(audioAlert != \none) { this.prDrawClockDanger };
		};
		toolbarView.layout = HLayout(toolbarTitle, infoGuideButton.view, infoAdvancedButton.view, infoShortcutsButton.view,
			nil, infoButton.view, drawerButton.view)
			.margins_(toolbarInset).spacing_(6);

		tabButtons = IdentityDictionary.new;
		CQGuiDrawer.tabs.do { |tab|
			tabButtons[tab] = CQInkControl.button(tab.asString,
				CQGuiScale.buttonWidth(\compact, tab.asString), \night, true)
				.surface_(\recess).action_({ this.selectDrawer(tab) });
			tabButtons[tab].scale = chrome;
			tabButtons[tab].view.fixedSize = Size(
				CQGuiScale.buttonWidth(\compact, tab.asString) + 16,
				chrome[\tabHeight]);
		};
		tabRow = HLayout(
			*(CQGuiDrawer.tabs.collect { |tab| tabButtons[tab].view } ++ [nil])
		).margins_(4).spacing_(4);
		drawerPages = pages;
		drawerStack = StackLayout(*drawerPages).mode_(\stackOne);
		drawerView = UserView().background_(night[\recess]);
		drawerView.layout = VLayout(tabRow, drawerStack).margins_(4).spacing_(4);
		workspaceStack = StackLayout(nightView, drawerView).mode_(\stackOne);

		view = View().background_(night[\ground]);
		view.layout = VLayout(toolbarView, View().layout_(workspaceStack), masterView)
			.margins_(0).spacing_(0);
		this.applyState;
		^this
	}

	infoOutsideViews { ^[toolbarView, toolbarTitle] }
	hoverBindings {
		^[[infoButton.view, [\ui, \info]], [drawerButton.view, [\ui, \drawer]],
			[infoGuideButton.view, [\ui, \tour]],
			[infoAdvancedButton.view, [\ui, \advanced]],
			[infoShortcutsButton.view, [\ui, \shortcuts]]]
			++ CQGuiDrawer.tabs.collect { |tab| [tabButtons[tab].view, [\ui, tab]] }
	}

	infoStatus { |enabled, playing|
		infoButton.value = enabled.binaryValue;
		// Changing toolbar visibility can move a live pointer; use queued Qt writes.
		// Compare the requested state, not Qt's not-yet-applied visibility.
		// A quick on/off must still queue the final hide after the pending show.
		if(infoControlsVisible != enabled) {
			infoControlsVisible = enabled;
			[infoGuideButton.view, infoAdvancedButton.view, infoShortcutsButton.view].do { |v|
				v.setProperty(\visible, enabled, false)
			};
		};
		^this
	}

	// One write door for the guard's visible truth. A repaint costs a Pen round
	// trip (§10), so an unchanged state must not ask for one — the facade
	// already announces only on change, and this refuses a second time.
	audioClockDanger_ { |flag|
		^this.audioAlert_(if(flag == true) { \clockStuck } { \none })
	}

	audioClockDanger { ^(audioAlert == \clockStuck) }

	// SOAK-1 finding B (user decision 2026-07-31): one mark, one ink, one route
	// to the only remedy — but two faults, so two sentences. A dead server told
	// with the clock's words would be the next lie.
	audioAlert_ { |kind|
		kind = this.class.alertKind(kind);
		if(kind == audioAlert) { ^this };
		audioAlert = kind;
		toolbarView !? { toolbarView.refresh };
		^this
	}

	*alertKind { |kind|
		^if(this.alertLabels.includesKey(kind)) { kind } { \none }
	}

	*alertLabels {
		^(
			clockStuck: "audio clock stuck",
			serverDown: "server not responding"
		)
	}

	// The words the Audio page uses for the same fault, so the two surfaces
	// cannot disagree about what happened.
	*alertLabel { |kind| ^this.alertLabels[this.alertKind(kind)] }

	*clockDangerLabel { ^this.alertLabel(\clockStuck) }
	*clockDangerDotRadius { ^4 }
	*clockDangerGap { ^8 }

	// Pure geometry, so the mark's placement can be tested without a paint
	// event. `titleRight` is the floor it may never cross: the toolbar's title
	// owns the left end, and a mark that slides under it would be unreadable
	// exactly when it matters most. When the room runs out the words drop and
	// the dot alone remains — the dot is the state, the words are the courtesy.
	// `kind` decides the words, and the words decide the geometry: the longer
	// "server not responding" must be measured, not assumed to fit where the
	// clock's shorter sentence did.
	*clockDangerLayout { |toolbarHeight, buttonLeft, titleRight = 0, labelSize = 11,
		kind = \clockStuck|
		var dotR = this.clockDangerDotRadius;
		var gap = this.clockDangerGap;
		var label = this.alertLabel(kind) ? this.alertLabel(\clockStuck);
		var textW = CQGuiScale.textWidth(label, labelSize);
		var cy = (toolbarHeight - 1) / 2;
		var dotX = buttonLeft - gap - dotR;
		var textLeft = dotX - dotR - gap - textW;
		var showText = textLeft > (titleRight + gap);
		^(
			dotCenter: Point(dotX, cy),
			dotRadius: dotR,
			showText: showText,
			textRect: Rect(textLeft, cy - (labelSize * 0.75), textW,
				labelSize * 1.5),
			label: label
		)
	}

	audioAlertLayout {
		var contentRight = if(infoShortcutsButton.view.visible) {
			infoShortcutsButton.view.bounds.right
		} { toolbarTitle.bounds.right };
		^this.class.clockDangerLayout(toolbarView.bounds.height,
			infoButton.view.bounds.left, contentRight, CQGuiScale.chrome[\label],
			audioAlert);
	}

	prDrawClockDanger {
		var palette = CQInk.palette(\night);
		var size = CQGuiScale.chrome[\label];
		var layout = this.audioAlertLayout;
		if(layout[\showText]) {
			Pen.stringRightJustIn(layout[\label], layout[\textRect],
				CQInk.font(\label, size), palette[\danger]);
		};
		Pen.fillColor = palette[\danger];
		Pen.addArc(layout[\dotCenter], layout[\dotRadius], 0, 2pi);
		Pen.fill;
		^this
	}

	toggleDrawer {
		state.toggleDrawer;
		this.applyState;
	}

	selectDrawer { |tab|
		state.selectDrawer(tab);
		this.applyState;
	}

	applyState {
		var palette = CQInk.palette(\night), tabIndex;
		tabIndex = CQGuiDrawer.tabs.indexOf(state.drawerTab) ? 0;
		view.background = palette[\ground];
		toolbarView.background = palette[\recess];
		drawerView.background = palette[\recess];
		drawerPages.do { |page| page.background = palette[\recess] };
		masterView.background = palette[\recess];
		toolbarTitle.stringColor = palette[\line];
		drawerButton.skin = \night;
		drawerButton.label = "drawer";
		drawerButton.glyph = if(state.drawerOpen) { \expand } { \collapse };
		drawerButton.value = state.drawerOpen.binaryValue;
		tabButtons.keysValuesDo { |tab, button|
			button.skin = \night;
			button.value = (tab == state.drawerTab).binaryValue;
		};
		skinAction !? { skinAction.value(\night) };
		if(state.drawerOpen) { shownAction !? { shownAction.value } };
		toolbarView.refresh;
		drawerView.refresh;
		masterView.refresh;
		// Only queued swaps are safe under a live pointer (GUI_SPEC §11).
		this.prSwapStage(if(state.drawerOpen) { 1 } { 0 }, tabIndex);
		^this
	}

	// QtCollider deadlock guard: hiding a view under the mouse pointer can
	// synchronously deliver enter/leave back into the language. Queued writes
	// return before those callbacks, so CQGuiShellState remains the synchronous
	// truth and the native indices settle one GUI tick later.
	prSwapStage { |workspaceIndex, drawerIndex|
		workspaceStack.setProperty(\currentIndex, workspaceIndex, false);
		drawerStack.setProperty(\currentIndex, drawerIndex, false);
	}
}
