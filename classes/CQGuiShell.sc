// CQGuiShell — Qt composition for Yölahti's Night-only instrument and the
// Drawer workbench. Night and Drawer share the central stage; the toolbar and
// Master remain persistent. The shell never touches musical state or server
// objects.

CQGuiShell {
	var state;
	var <view, <nightView, <masterView, <drawerView;
	var <workspaceStack, <drawerButton;
	var drawerStack, drawerPages, toolbarView, toolbarTitle;
	var tabButtons, skinAction, shownAction;
	var toolbarInset = 6;
	var <audioClockDanger = false;

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
			// sits on the drawer button, which is the only route to the only
			// remedy — cue and action share one geometry (GUI_SPEC §4.5). It is
			// static ink: no blink, no pulse, and it exists only in danger.
			if(audioClockDanger) { this.prDrawClockDanger };
		};
		toolbarView.layout = HLayout(toolbarTitle, nil, drawerButton.view)
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

	// One write door for the guard's visible truth. A repaint costs a Pen round
	// trip (§10), so an unchanged state must not ask for one — the facade
	// already announces only on change, and this refuses a second time.
	audioClockDanger_ { |flag|
		flag = (flag == true);
		if(flag == audioClockDanger) { ^this };
		audioClockDanger = flag;
		toolbarView !? { toolbarView.refresh };
		^this
	}

	*clockDangerLabel { ^"audio clock stuck" }
	*clockDangerDotRadius { ^4 }
	*clockDangerGap { ^8 }

	// Pure geometry, so the mark's placement can be tested without a paint
	// event. `titleRight` is the floor it may never cross: the toolbar's title
	// owns the left end, and a mark that slides under it would be unreadable
	// exactly when it matters most. When the room runs out the words drop and
	// the dot alone remains — the dot is the state, the words are the courtesy.
	*clockDangerLayout { |toolbarHeight, buttonLeft, titleRight = 0, labelSize = 11|
		var dotR = this.clockDangerDotRadius;
		var gap = this.clockDangerGap;
		var label = this.clockDangerLabel;
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

	prDrawClockDanger {
		var palette = CQInk.palette(\night);
		var chrome = CQGuiScale.chrome;
		var size = chrome[\label];
		var layout = this.class.clockDangerLayout(toolbarView.bounds.height,
			drawerButton.view.bounds.left, toolbarTitle.bounds.right, size);
		// The words are the ones the Audio page repeats, so the two surfaces
		// cannot disagree about what happened.
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
