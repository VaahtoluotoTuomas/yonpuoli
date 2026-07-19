// CQGuiShell — Qt composition for GUI_SPEC §2. The serverless CQGuiShellState
// owns behavior; this class only applies that state to Day/Night, Drawer and the
// persistent Master region. It never touches CQState, CQMatrix or the server.

CQGuiShell {
	var state;
	var <view, <dayView, <nightView, <masterView, <drawerView;
	var contentStack, drawerStack, drawerPages, toolbarView, toolbarTitle;
	var modeButton, drawerButton, tabButtons, skinAction;

	*new { |state, dayView, nightView, masterView, inspectorView, spellbookView,
		memoryView, skinAction|
		^super.new.prInit(state, dayView, nightView, masterView,
			inspectorView, spellbookView, memoryView, skinAction)
	}

	prInit { |shellState, dayContent, nightContent, masterContent,
		inspectorView, spellbookView, memoryView, applySkin|
		var day = CQInk.palette(\day), tabRow;
		state = shellState;
		skinAction = applySkin;
		dayView = dayContent;
		nightView = nightContent;
		masterView = masterContent;
		modeButton = CQInkControl.button("night", 66).surface_(\recess)
			.action_({ this.toggleView(false) });
		drawerButton = CQInkControl.button("drawer", 82).surface_(\recess)
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
		toolbarView.layout = HLayout(
			toolbarTitle, nil, modeButton.view, drawerButton.view
		).margins_(8).spacing_(6);
		contentStack = StackLayout(dayView, nightView).mode_(\stackOne);
		tabButtons = IdentityDictionary.new;
		#[inspector, spellbook, memory].do { |tab|
			tabButtons[tab] = CQInkControl.button(tab.asString, 86, \day, true)
				.surface_(\recess).action_({ this.selectDrawer(tab) });
		};
		tabRow = HLayout(tabButtons[\inspector].view, tabButtons[\spellbook].view,
			tabButtons[\memory].view, nil).margins_(4).spacing_(4);
		drawerPages = [inspectorView, spellbookView, memoryView];
		drawerStack = StackLayout(*drawerPages)
			.mode_(\stackOne);
		drawerView = UserView().fixedHeight_(250).background_(day[\recess]);
		drawerView.drawFunc = {
			var palette = CQInk.palette(state.viewMode);
			Pen.strokeColor = palette[\line].copy.alpha_(0.35);
			Pen.width = CQInk.strokes[\hairline];
			Pen.line(Point(0, 1), Point(drawerView.bounds.width, 1));
			Pen.stroke;
		};
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
		drawerPages.do { |page| page.background = palette[\recess] };
		masterView.background = palette[\recess];
		toolbarTitle.stringColor = palette[\line];
		modeButton.skin = skin;
		modeButton.label = if(skin == \day) { "night" } { "day" };
		drawerButton.skin = skin;
		drawerButton.label = if(state.drawerOpen) { "close drawer" } { "drawer" };
		tabButtons.keysValuesDo { |tab, button|
			button.skin = skin;
			button.value = (tab == state.drawerTab).binaryValue;
		};
		skinAction !? { skinAction.value(skin) };
		toolbarView.refresh;
		drawerView.refresh;
		masterView.refresh;
		^this
	}
}
