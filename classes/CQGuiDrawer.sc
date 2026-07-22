// CQGuiDrawer — G1 Drawer composition: precision Inspector, notification-
// stream Spellbook and existing snapshot/morph Memory (GUI_SPEC §8).

CQGuiDrawer {
	var gui, state, matrix, grammar;
	var <spellbookGui, <memoryGui, presetMenu;
	var <inspector, <grid, <spellbook;
	var <inspectorView, <spellbookView, <memoryView;

	*new { |gui, state, matrix, audition, grammar|
		^super.newCopyArgs(gui, state, matrix, grammar).prBuild(audition)
	}

	*tabs { ^#[inspector, spellbook, memory] }

	viewToggle { inspector.cancel; grammar.viewToggle; ^this }
	windowClose { inspector.cancel; grammar.windowClose; ^this }
	cmdPeriod { inspector.cancel; grammar.cmdPeriod; ^this }
	panic { inspector.cancel; grammar.panic; ^this }
	escape { inspector.cancel; grammar.escape; ^this }

	free {
		grid !? { grid.free };
		spellbookGui !? { spellbookGui.free };
		memoryGui !? { memoryGui.free };
		spellbook !? { spellbook.free };
		inspector !? { inspector.free };
	}

	applySkin { |skin|
		var palette = CQInk.palette(skin);
		grid.skin = skin;
		spellbookGui.applySkin(skin);
		memoryGui.applySkin(skin);
		inspectorView.background = palette[\recess];
		^this
	}

	applyPreset { |name|
		var data = CQMatrix.presets[name];
		if(data.isNil) { ^this };
		grammar.performUndoable({
			matrix.preset(name, origin: \gui, committed: true,
				operation: \preset);
			data[\params].keysValuesDo { |key, value| state.set(key, value) };
		});
		^this
	}

	// ---- private ----

	prBuild { |audition|
		var palette = CQInk.palette(\day), applyButton, gestureHelp;
		var loomWidth = CQGuiMatrix.loomSize.width, topRail, footerRail;
		var presetNames = CQMatrix.presets.keys.asArray.sort;
		inspector = CQInspector(matrix, audition, grammar);
		grid = CQGuiMatrix(matrix, inspector);
		spellbook = CQSpellbook(matrix);
		spellbookGui = CQGuiSpellbook(gui, spellbook);
		spellbookView = spellbookGui.view;
		memoryGui = CQGuiMemory(gui, state, matrix, Cocoquantus.snapshots);
		memoryView = memoryGui.view;
		presetMenu = PopUpMenu().items_(presetNames.collect { |name| name.asString })
			.font_(CQInk.font(\number)).stringColor_(palette[\line])
			.background_(palette[\ground]).fixedSize_(Size(148, 22));
		applyButton = this.prButton("apply preset", 96, {
			presetMenu.item !? { |item| this.applyPreset(item.asSymbol) }
		});
		gestureHelp = StaticText().string_(
			"press listens · double-click plants · vertical drag trims · right-click clears")
			.font_(CQInk.font(\label)).stringColor_(palette[\whisper])
			.align_(\right).fixedSize_(Size(loomWidth, 26));
		topRail = View().fixedWidth_(loomWidth).layout_(
			HLayout(grid.status, nil, presetMenu, applyButton).margins_(0).spacing_(6));
		footerRail = View().fixedWidth_(loomWidth).layout_(
			HLayout(gestureHelp).margins_(0).spacing_(0));
		inspectorView = View().background_(palette[\recess]).layout_(VLayout(
			HLayout(nil, topRail, nil),
			HLayout(nil, grid.header, nil),
			HLayout(nil, grid.view, nil),
			HLayout(nil, footerRail, nil),
			nil
		).margins_(4).spacing_(2));
		^this
	}

	prButton { |label, width, action|
		var button = CQInkControl.button(label, width, \day, false, true, \spaf);
		button.surface = \recess;
		button.action = action;
		gui.registerInkControl(button);
		^button.view
	}
}
