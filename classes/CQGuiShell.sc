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
