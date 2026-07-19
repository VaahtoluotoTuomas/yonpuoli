// CQGuiShell — Qt composition for GUI_SPEC §2. The serverless CQGuiShellState
// owns behavior; this class only applies that state to Day/Night, Drawer and the
// persistent Master region. It never touches CQState, CQMatrix or the server.

CQGuiShell {
	var state;
	var <view, <dayView, <nightView, <masterView, <drawerView;
	var contentStack, drawerStack, toolbarView, modeButton, drawerButton, tabButtons;

	*new { |state, dayView, nightView, masterView, inspectorView, spellbookView, memoryView|
		^super.new.prInit(state, dayView, nightView, masterView,
			inspectorView, spellbookView, memoryView)
	}

	prInit { |shellState, dayContent, nightContent, masterContent,
		inspectorView, spellbookView, memoryView|
		var day = CQInk.palette(\day), tabRow;
		state = shellState;
		dayView = dayContent;
		nightView = nightContent;
		masterView = masterContent;
		modeButton = Button().fixedWidth_(92).action_({ this.toggleView(false) });
		drawerButton = Button().fixedWidth_(92).action_({ this.toggleDrawer });
		toolbarView = View().fixedHeight_(32).background_(day[\recess]);
		toolbarView.layout = HLayout(
			StaticText().string_("COCOQUANTUS · ARCHIPELAGO")
				.font_(Font.sansSerif(11, true)).stringColor_(day[\line]),
			nil, modeButton, drawerButton
		).margins_(8).spacing_(6);
		contentStack = StackLayout(dayView, nightView).mode_(\stackOne);
		tabButtons = IdentityDictionary.new;
		#[inspector, spellbook, memory].do { |tab|
			tabButtons[tab] = Button().fixedWidth_(92).states_([[tab.asString]])
				.action_({ this.selectDrawer(tab) });
		};
		tabRow = HLayout(tabButtons[\inspector], tabButtons[\spellbook],
			tabButtons[\memory], nil).margins_(4).spacing_(4);
		drawerStack = StackLayout(inspectorView, spellbookView, memoryView)
			.mode_(\stackOne);
		drawerView = View().fixedHeight_(270).background_(day[\recess]);
		drawerView.layout = VLayout(tabRow, drawerStack).margins_(6).spacing_(4);
		view = View().background_(day[\ground]);
		view.layout = VLayout(toolbarView, View().layout_(contentStack), drawerView,
			masterView).margins_(0).spacing_(0);
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
		contentStack.index = if(skin == \day) { 0 } { 1 };
		drawerView.visible = state.drawerOpen;
		tabIndex = #[inspector, spellbook, memory].indexOf(state.drawerTab) ? 0;
		drawerStack.index = tabIndex;
		view.background = palette[\ground];
		toolbarView.background = palette[\recess];
		drawerView.background = palette[\recess];
		masterView.background = palette[\recess];
		modeButton.states = [[if(skin == \day) { "night" } { "day" },
			palette[\line], palette[\recess]]];
		drawerButton.states = [[if(state.drawerOpen) { "close drawer" } { "drawer" },
			palette[\line], palette[\recess]]];
		tabButtons.keysValuesDo { |tab, button|
			button.value = (tab == state.drawerTab).binaryValue;
		};
		^this
	}
}
