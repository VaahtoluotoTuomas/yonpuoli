// CQGuiShell — Qt composition for GUI_SPEC §2 and the §8 workbench experiment.
// Day/Night and Drawer share one central stage; the toolbar and Master remain
// persistent. The shell never touches CQState, CQMatrix or the server.

CQGuiShell {
	var state;
	var <view, <dayView, <nightView, <masterView, <drawerView;
	var <workspaceStack, <drawerButton;
	var contentStack, drawerStack, drawerPages, toolbarView, toolbarTitle;
	var modeButton, tabButtons, skinAction;

	*new { |state, dayView, nightView, masterView, pages, skinAction|
		^super.new.prInit(state, dayView, nightView, masterView,
			pages, skinAction)
	}

	prInit { |shellState, dayContent, nightContent, masterContent,
		pages, applySkin|
		var day = CQInk.palette(\day), tabRow, instrumentView;
		state = shellState;
		skinAction = applySkin;
		dayView = dayContent;
		nightView = nightContent;
		masterView = masterContent;
		modeButton = CQInkControl.button("night", 66).surface_(\recess)
			.action_({ this.toggleView(false) });
		drawerButton = CQInkControl.button("drawer", 92, \day, false,
			false, \line, \collapse).surface_(\recess)
			.action_({ this.toggleDrawer });
		toolbarTitle = StaticText().string_("cocoquantus · archipelago")
			.font_(CQInk.font(\label)).stringColor_(day[\line])
			.align_(\left).fixedWidth_(180);
		toolbarView = UserView().fixedHeight_(32).background_(day[\recess]);
		toolbarView.drawFunc = {
			var palette = CQInk.palette(state.viewMode);
			Pen.strokeColor = palette[\whisper];
			Pen.width = CQInk.strokes[\hairline];
			Pen.line(Point(8, 31), Point(toolbarView.bounds.width - 8, 31));
			Pen.stroke;
		};
		toolbarView.layout = HLayout(toolbarTitle, nil, modeButton.view, drawerButton.view)
			.margins_(8).spacing_(6);

		contentStack = StackLayout(dayView, nightView).mode_(\stackOne);
		instrumentView = View().layout_(contentStack);
		tabButtons = IdentityDictionary.new;
		CQGuiDrawer.tabs.do { |tab|
			tabButtons[tab] = CQInkControl.button(tab.asString, 86, \day, true)
				.surface_(\recess).action_({ this.selectDrawer(tab) });
		};
		tabRow = HLayout(
			*(CQGuiDrawer.tabs.collect { |tab| tabButtons[tab].view } ++ [nil])
		).margins_(4).spacing_(4);
		drawerPages = pages;
		drawerStack = StackLayout(*drawerPages).mode_(\stackOne);
		drawerView = UserView().background_(day[\recess]);
		drawerView.layout = VLayout(tabRow, drawerStack).margins_(4).spacing_(4);
		workspaceStack = StackLayout(instrumentView, drawerView).mode_(\stackOne);

		view = View().background_(day[\ground]);
		view.layout = VLayout(toolbarView, View().layout_(workspaceStack), masterView)
			.margins_(0).spacing_(0);
		this.applyState;
		^this
	}

	toggleView { |textHasFocus = false|
		state.toggleView(textHasFocus);
		this.applyState;
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
		var skin = state.viewMode, palette = CQInk.palette(skin), tabIndex;
		tabIndex = CQGuiDrawer.tabs.indexOf(state.drawerTab) ? 0;
		view.background = palette[\ground];
		toolbarView.background = palette[\recess];
		drawerView.background = palette[\recess];
		drawerPages.do { |page| page.background = palette[\recess] };
		masterView.background = palette[\recess];
		toolbarTitle.stringColor = palette[\line];
		modeButton.skin = skin;
		modeButton.label = if(skin == \day) { "night" } { "day" };
		drawerButton.skin = skin;
		// The global destination keeps one name; the inner page tabs carry the
		// Inspector/Spellbook/Memory labels. Down opens, up returns.
		drawerButton.label = "drawer";
		drawerButton.glyph = if(state.drawerOpen) { \expand } { \collapse };
		drawerButton.value = state.drawerOpen.binaryValue;
		tabButtons.keysValuesDo { |tab, button|
			button.skin = skin;
			button.value = (tab == state.drawerTab).binaryValue;
		};
		skinAction !? { skinAction.value(skin) };
		toolbarView.refresh;
		drawerView.refresh;
		masterView.refresh;
		// last, and only after every blocking write above has returned
		this.prSwapStage(if(skin == \day) { 0 } { 1 },
			if(state.drawerOpen) { 1 } { 0 }, tabIndex);
		^this
	}

	// QtCollider deadlock guard: hiding a view under the mouse pointer makes Qt
	// deliver enter/leave back into the language, and a *direct* property write
	// blocks the language thread inside the same Qt call — Tab with the pointer
	// over the Day panel froze the whole window (found 2026-07-21). Queued
	// writes (direct: false) return immediately, so the swap and its hover
	// events happen while nothing is blocking. The stack index therefore
	// settles one GUI tick later; CQGuiShellState stays the synchronous truth.
	prSwapStage { |contentIndex, workspaceIndex, drawerIndex|
		contentStack.setProperty(\currentIndex, contentIndex, false);
		workspaceStack.setProperty(\currentIndex, workspaceIndex, false);
		drawerStack.setProperty(\currentIndex, drawerIndex, false);
	}
}
