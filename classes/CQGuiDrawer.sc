// CQGuiDrawer — the Drawer composition: precision Inspector, the Workshop
// ledger page, notification-stream Spellbook, snapshot/morph Memory and the
// technical Audio I/O setup (GUI_SPEC §8).

CQGuiDrawer {
	var gui, state, matrix, grammar, audioConfig;
	var <spellbookGui, <memoryGui, <workshop, <audioSetup, <presetMenu;
	var <inspector, <grid, <spellbook;
	var <inspectorView, <workshopView, <audioView, <spellbookView, <memoryView;
	// the Drawer's shared size step (CQGuiScale) and the Inspector chrome that
	// follows the loom's rail
	var <step = \compact, <pageSize, applyButton, gestureHelp, topRail, footerRail;
	var inspectorTop, inspectorGap;

	*new { |gui, state, matrix, audition, grammar, audioModel|
		^super.newCopyArgs(gui, state, matrix, grammar, audioModel).prBuild(audition)
	}

	*tabs { ^#[inspector, workshop, spellbook, memory, audio] }

	// always in `tabs` order — the shell indexes one against the other
	pages { ^[inspectorView, workshopView, spellbookView, memoryView, audioView] }

	viewToggle { inspector.cancel; grammar.viewToggle; ^this }
	windowClose { inspector.cancel; grammar.windowClose; ^this }
	cmdPeriod { inspector.cancel; grammar.cmdPeriod; ^this }
	panic { inspector.cancel; grammar.panic; ^this }
	escape { inspector.cancel; grammar.escape; ^this }

	free {
		presetMenu !? { presetMenu.free };
		grid !? { grid.free };
		spellbookGui !? { spellbookGui.free };
		memoryGui !? { memoryGui.free };
		workshop !? { workshop.free };
		audioSetup !? { audioSetup.free };
		spellbook !? { spellbook.free };
		inspector !? { inspector.free };
	}

	// CQGui.drawerShown: the workbench became visible, or switched page. Pages
	// that skip drawing while nobody can see them catch up here.
	pageShown {
		spellbookGui !? { spellbookGui.pageShown };
		^this
	}

	applySkin { |skin|
		var palette = CQInk.palette(skin);
		grid.skin = skin;
		spellbookGui.applySkin(skin);
		memoryGui.applySkin(skin);
		workshop.applySkin(skin);
		audioSetup.applySkin(skin);
		presetMenu.skin = skin;
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

	// One size step for the whole workbench, so the five pages read as one
	// surface (GUI_SPEC §8). The stage's own rect decides it; every page then
	// re-measures rather than rebuilding, so a resize can never re-register a
	// funnel binding or leave a stale control behind.
	// Returns true when something actually moved, so the caller can settle a
	// layout that arrives in more than one pass instead of keeping a stale size.
	applyScale { |stageWidth, stageHeight|
		var size = CQGuiScale.pageSizeFor(stageWidth, stageHeight);
		var next = CQGuiScale.stepFor(size.width, size.height);
		if(pageSize.notNil and: { next == step } and: { size == pageSize }) {
			^false
		};
		step = next;
		pageSize = size;
		grid.applyScale(step, size.width);
		this.prScaleInspectorChrome;
		workshop.applyScale(step, size.width);
		audioSetup.applyScale(step, size.width);
		spellbookGui.applyScale(step, grid.loomSize.width);
		memoryGui.applyScale(step, grid.loomSize.width);
		^true
	}

	prScaleInspectorChrome {
		var m = CQGuiScale.metrics(step), width = grid.loomSize.width;
		inspectorTop.fixedHeight = m[\vMargin];
		inspectorGap.fixedHeight = m[\rowGap];
		topRail.fixedWidth = width;
		footerRail.fixedWidth = width;
		gestureHelp.font = CQInk.font(\label, m[\label]);
		gestureHelp.fixedSize = Size(width, m[\barHeight]);
		presetMenu.font = CQInk.font(\number, m[\mono]);
		presetMenu.fixedSize = Size(
			(CQGuiScale.textWidth("apply preset", m[\mono], true) + 40)
				.ceil.asInteger,
			m[\boxHeight]);
		applyButton.scale_(m);
		applyButton.resizeTo(CQGuiScale.buttonWidth(step, applyButton.label),
			m[\boxHeight]);
		^this
	}

	prBuild { |audition|
		var palette = CQInk.palette(\night);
		var loomWidth, presetNames = CQMatrix.presets.keys.asArray.sort;
		var m = CQGuiScale.metrics(step);
		inspector = CQInspector(matrix, audition, grammar);
		grid = CQGuiMatrix(matrix, inspector, step);
		loomWidth = grid.loomSize.width;
		spellbook = CQSpellbook(matrix);
		spellbookGui = CQGuiSpellbook(gui, spellbook);
		spellbookView = spellbookGui.view;
		memoryGui = CQGuiMemory(gui, state, matrix, Cocoquantus.snapshots);
		memoryView = memoryGui.view;
		// the bay's measurement table. Built with the GUI, never on demand: its
		// mic row owns the persistent MIC LIVE warning, and closing the Drawer
		// may not silence that (NIGHT_STANDALONE_PLAN §12.1.3).
		workshop = CQGuiWorkshop(gui, state);
		workshopView = workshop.view;
		// The complete Audio page is built once with the GUI. Its AIO-3
		// transaction keeps only the non-Qt model across a confirmed reboot.
		audioSetup = CQGuiAudioSetup(gui, audioConfig);
		audioView = audioSetup.view;
		presetMenu = CQInkMenu(presetNames.collect { |name| name.asString }, \night)
			.font_(CQInk.font(\number, m[\mono]))
			.fixedSize_(Size(
				(CQGuiScale.textWidth("apply preset", m[\mono], true) + 40)
					.ceil.asInteger,
				m[\boxHeight]));
		applyButton = this.prButton("apply preset",
			CQGuiScale.buttonWidth(step, "apply preset"), {
				presetMenu.item !? { |item| this.applyPreset(item.asSymbol) }
			});
		gestureHelp = StaticText().string_(
			"press listens · double-click plants · vertical drag trims · right-click clears")
			.font_(CQInk.font(\label, m[\label])).stringColor_(palette[\whisper])
			.align_(\right).fixedSize_(Size(loomWidth, m[\barHeight]));
		topRail = View().fixedWidth_(loomWidth).layout_(
			HLayout(grid.status, nil, presetMenu.view, applyButton.view)
				.margins_(0).spacing_(6));
		footerRail = View().fixedWidth_(loomWidth).layout_(
			HLayout(gestureHelp).margins_(0).spacing_(0));
		// the shared page frame (CQGuiScale.pageFrame): the same air under the
		// tab row as every other Drawer page, and the same centred rail
		inspectorTop = View().fixedHeight_(m[\vMargin]);
		// the loom's column headings belong to the grid, not to the preset row
		// above them, so the air goes here and the header stays tight to its grid
		inspectorGap = View().fixedHeight_(m[\rowGap]);
		// spacing 0 so the shared top margin is exactly the pad, as on every
		// other page; the gaps that matter are explicit spacers
		inspectorView = View().background_(palette[\recess]).layout_(VLayout(
			inspectorTop,
			HLayout(nil, topRail, nil),
			inspectorGap,
			HLayout(nil, grid.header, nil),
			HLayout(nil, grid.view, nil),
			View().fixedHeight_(4),
			HLayout(nil, footerRail, nil),
			nil
		).margins_(0).spacing_(0));
		^this
	}

	// returns the control, not its view: the chrome has to re-measure it when
	// the Drawer's size step changes
	prButton { |label, width, action|
		var button = CQInkControl.button(label, width, \night, false, true, \spaf);
		button.surface = \recess;
		button.action = action;
		button.scale = CQGuiScale.metrics(step);
		gui.registerInkControl(button);
		^button
	}
}
