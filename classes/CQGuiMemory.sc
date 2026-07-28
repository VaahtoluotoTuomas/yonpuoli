// CQGuiMemory — the Drawer Memory page as a truthful seed bank. Saved snapshots
// remain explicit recall/morph data; the live matrix and selected snapshot are
// rendered as small patch fingerprints instead of an empty form (GUI_SPEC §8).
//
// Naming law (GUI_SPEC §8): "seed bank" names the page metaphor, but every item
// in it is a *snapshot* — the instrument has exactly two memory words, presets
// (patch recipes in code) and snapshots (saved instrument state on disk). The
// word seed appears in exactly one place, the page header; labels, hints and
// identifiers all say snapshot.

CQGuiMemory {
	classvar contentW = 1128;

	var gui, state, matrix, snapshots, matrixCtl, morphCtl;
	var skin = \day, snapshotNames, selectedData, labels, menus, panels;
	var nameField, aMenu, bMenu, durationBox, detailTitle, detailHint;
	var <view, <snapshotList, <fingerprint, <morphMeter, <selectedName;
	// the seed bank shares the workbench's content rail and size step
	var <step = \compact, <railWidth, buttons, body, snapshotPanel, morphPanel;
	var topPad;

	*new { |gui, state, matrix, snapshots|
		^super.newCopyArgs(gui, state, matrix, snapshots).prBuild
	}

	// the historical reference width, kept so older contracts still read; the
	// live rail is `railWidth` and follows CQGuiScale
	*contentWidth { ^contentW }

	// The saved-snapshot column is a fixed reading width; the fingerprint takes
	// whatever the rail has left, so a wider window buys patch detail rather
	// than empty slate.
	*snapshotColumnWidth { |step = \compact|
		^if(step == \roomy) { 340 } { 280 }
	}

	*fingerprintSize { |step = \compact, rail = 1128|
		var column = this.snapshotColumnWidth(step);
		var height = if(step == \roomy) { 380 } { 300 };
		^Size(rail - column - 8, height)
	}

	applyScale { |nextStep, width|
		var m, column, print;
		step = nextStep ? step;
		railWidth = width ? railWidth;
		m = CQGuiScale.metrics(step);
		column = this.class.snapshotColumnWidth(step);
		print = this.class.fingerprintSize(step, railWidth);
		labels.do { |record|
			record[\view].font = CQInk.font(record[\role],
				m[if(record[\role] == \notice) { \notice } { \label }]);
			record[\view].fixedHeight = m[\barHeight];
		};
		snapshotList.font = CQInk.font(\spellbook, m[\mono]);
		snapshotList.fixedWidth = column - 14;
		nameField.font = CQInk.font(\number, m[\mono]);
		nameField.fixedSize = Size(column - 14 - CQGuiScale.buttonWidth(step,
			"save snapshot") - 6, m[\boxHeight]);
		durationBox.font = CQInk.font(\number, m[\mono]);
		durationBox.fixedSize = Size(64, m[\boxHeight]);
		menus.do { |menu|
			menu.font = CQInk.font(\number, m[\mono]);
			menu.fixedSize = Size(if(step == \roomy) { 210 } { 170 }, m[\boxHeight]);
		};
		buttons.do { |button|
			button.scale = m;
			button.resizeTo(CQGuiScale.buttonWidth(step, button.label),
				m[\boxHeight]);
		};
		snapshotPanel.fixedWidth = column;
		fingerprint.fixedWidth = print.width;
		body.fixedWidth = railWidth;
		morphPanel.fixedSize = Size(railWidth, m[\boxHeight] + 62);
		morphMeter.fixedSize = Size(if(step == \roomy) { 380 } { 310 },
			m[\boxHeight]);
		topPad.fixedHeight = m[\vMargin];
		panels.do { |panel| panel.refresh };
		^this
	}

	free {
		matrixCtl !? { matrixCtl.remove };
		morphCtl !? { morphCtl.remove };
		matrixCtl = nil;
		morphCtl = nil;
	}

	applySkin { |next|
		var palette;
		skin = next;
		palette = CQInk.palette(skin);
		view.background = palette[\recess];
		panels.do { |panel| panel.background = palette[\recess]; panel.refresh };
		labels.do { |record|
			record[\view].stringColor = palette[record[\color]]
				?? { palette[\whisper] };
		};
		snapshotList.background = palette[\ground];
		snapshotList.stringColor = palette[\line];
		snapshotList.selectedStringColor = palette[\line];
		snapshotList.hiliteColor = palette[\spaf].copy.alpha_(0.28);
		menus.do { |menu|
			menu.background = palette[\ground];
			menu.stringColor = palette[\line];
		};
		nameField.background = palette[\ground];
		nameField.stringColor = palette[\line];
		durationBox.background = palette[\ground];
		durationBox.stringColor = palette[\line];
		fingerprint.background = palette[\ground];
		morphMeter.background = palette[\ground];
		fingerprint.refresh;
		morphMeter.refresh;
		^this
	}

	refresh {
		var index;
		snapshotNames = if(snapshots.isNil) { [] } { snapshots.list };
		snapshotList.items = ["live patch · now"] ++ snapshotNames;
		menus.do { |menu| menu.items = snapshotNames };
		index = if(selectedName.isNil) { 0 } {
			snapshotNames.indexOf(selectedName.asString)
		};
		if(index.isNil) { selectedName = nil; index = 0 } { index = index + 1 };
		snapshotList.value = index;
		this.prSelectSnapshot;
		^this
	}

	// ---- private ----

	prBuild {
		var palette = CQInk.palette(\day), header;
		var saveButton, recallButton, refreshButton, morphButton;
		var m = CQGuiScale.metrics(step), column, print, saveW;
		labels = List.new;
		menus = List.new;
		panels = List.new;
		buttons = List.new;
		railWidth = CQGuiScale.railWidth(step, 1272);
		column = this.class.snapshotColumnWidth(step);
		print = this.class.fingerprintSize(step, railWidth);
		saveW = CQGuiScale.buttonWidth(step, "save snapshot");

		header = this.prLabel("memory · seed bank", \line, \notice)
			.fixedWidth_(railWidth);
		nameField = TextField().string_("snapshot")
			.font_(CQInk.font(\number, m[\mono]))
			.stringColor_(palette[\line]).background_(palette[\ground])
			.fixedSize_(Size(column - 14 - saveW - 6, m[\boxHeight]))
			.action_({ |field| field.focus(false) });
		snapshotList = ListView().selectionMode_(\single)
			.font_(CQInk.font(\spellbook, m[\mono]))
			.stringColor_(palette[\line]).selectedStringColor_(palette[\line])
			.hiliteColor_(palette[\spaf].copy.alpha_(0.28))
			.background_(palette[\ground])
			.fixedWidth_(column - 14);
		snapshotList.action = { this.prSelectSnapshot };
		snapshotList.selectionAction = { this.prSelectSnapshot };
		saveButton = this.prButton("save snapshot", saveW, \line, { this.prSave });
		recallButton = this.prButton("recall",
			CQGuiScale.buttonWidth(step, "recall"), \line, { this.prRecall });
		refreshButton = this.prButton("refresh",
			CQGuiScale.buttonWidth(step, "refresh"), \line, { this.refresh });

		snapshotPanel = UserView().fixedWidth_(column)
			.background_(palette[\recess]);
		snapshotPanel.drawFunc = {
			CQInk.drawToolBox(snapshotPanel.bounds.moveTo(0, 0), "saved snapshots", skin)
		};
		snapshotPanel.layout = VLayout(
			View().fixedHeight_(m[\barHeight] - 4),
			HLayout(nameField, saveButton.view).spacing_(6),
			snapshotList,
			HLayout(recallButton.view, refreshButton.view, nil).spacing_(6)
		).margins_(7).spacing_(6);

		detailTitle = this.prLabel("live patch · now", \line, \notice);
		detailHint = this.prLabel(
			"select a saved snapshot to compare it with the live patch", \whisper, \label);
		fingerprint = UserView().fixedWidth_(print.width)
			.background_(palette[\ground]);
		fingerprint.drawFunc = {
			try { this.prDrawFingerprint } { |error| CQInkControl.noteDrawError(error) }
		};
		fingerprint.layout = VLayout(
			HLayout(detailTitle, nil, detailHint).margins_(8).spacing_(6), nil
		).margins_(0).spacing_(0);

		// width fixed to the rail, height free: the body takes the page's
		// remaining room instead of a constant
		body = View().fixedWidth_(railWidth).layout_(
			HLayout(snapshotPanel, fingerprint).margins_(0).spacing_(8));

		aMenu = this.prMenu;
		bMenu = this.prMenu;
		menus = [aMenu, bMenu];
		durationBox = NumberBox().value_(10).clipLo_(0.1).clipHi_(600)
			.font_(CQInk.font(\number, m[\mono])).stringColor_(palette[\line])
			.background_(palette[\ground]).fixedSize_(Size(64, m[\boxHeight]))
			.action_({ |box| box.focus(false) });
		morphButton = this.prButton("morph",
			CQGuiScale.buttonWidth(step, "morph"), \spaf, { this.prMorph });
		morphMeter = UserView().fixedSize_(Size(310, m[\boxHeight]))
			.background_(palette[\ground]);
		morphMeter.drawFunc = {
			try { this.prDrawMorphMeter } { |error| CQInkControl.noteDrawError(error) }
		};
		morphPanel = UserView().fixedSize_(Size(railWidth, m[\boxHeight] + 62))
			.background_(palette[\recess]);
		morphPanel.drawFunc = {
			CQInk.drawToolBox(morphPanel.bounds.moveTo(0, 0), "morph bridge", skin)
		};
		morphPanel.layout = VLayout(
			View().fixedHeight_(m[\barHeight] - 4),
			HLayout(this.prLabel("a", \whisper, \label).fixedWidth_(10), aMenu,
				this.prLabel("→", \whisper, \label).fixedWidth_(12), bMenu,
				durationBox,
				this.prLabel("seconds", \whisper, \label).fixedWidth_(58),
				morphButton.view,
				nil, morphMeter).spacing_(6)
		).margins_(7).spacing_(4);

		panels = [snapshotPanel, fingerprint, morphPanel, morphMeter];
		// the shared page frame (CQGuiScale.pageFrame). The body carries the
		// stretch, so the saved-snapshot column and the fingerprint reach down
		// to the morph bridge instead of stopping at a fixed height — the
		// fingerprint's own cells and type then grow with the room it gets.
		topPad = View().fixedHeight_(m[\vMargin]);
		view = View().background_(palette[\recess]).layout_(VLayout(
			topPad,
			HLayout(nil, header, nil),
			HLayout(nil, body, nil),
			HLayout(nil, morphPanel, nil)
		).margins_(0).spacing_(8));

		matrixCtl = SimpleController(matrix);
		#[patch, matrix, clear].do { |what|
			matrixCtl.put(what, { { fingerprint.refresh }.defer })
		};
		if(snapshots.notNil) {
			morphCtl = SimpleController(snapshots);
			morphCtl.put(\morph, { { morphMeter.refresh }.defer });
		};
		this.refresh;
		^this
	}

	prMenu {
		var palette = CQInk.palette(skin), m = CQGuiScale.metrics(step);
		^PopUpMenu().font_(CQInk.font(\number, m[\mono]))
			.stringColor_(palette[\line])
			.background_(palette[\ground])
			.fixedSize_(Size(if(step == \roomy) { 210 } { 170 }, m[\boxHeight]))
	}

	prLabel { |text, color = \whisper, role = \label|
		var palette = CQInk.palette(skin), label;
		var m = CQGuiScale.metrics(step);
		label = StaticText().string_(text)
			.font_(CQInk.font(role, m[if(role == \notice) { \notice } { \label }]))
			.align_(\left)
			.stringColor_(palette[color] ?? { palette[\whisper] })
			.fixedHeight_(m[\barHeight]);
		labels.add((view: label, role: role, color: color));
		^label
	}

	// returns the control: the page re-measures it when the size step changes
	prButton { |label, width, accent, action|
		var button = CQInkControl.button(label, width, \day, false, true, accent);
		button.surface = \recess;
		button.action = action;
		button.scale = CQGuiScale.metrics(step);
		gui.registerInkControl(button);
		buttons.add(button);
		^button
	}

	prSelectSnapshot {
		var index = snapshotList.value ? 0;
		if(index <= 0) {
			selectedName = nil;
			selectedData = nil;
			detailTitle.string = "live patch · now";
			detailHint.string = "select a saved snapshot to compare it with the live patch";
		} {
			selectedName = snapshotNames.at(index - 1);
			selectedData = snapshots !? { snapshots.read(selectedName.asSymbol) };
			detailTitle.string = selectedName.asString;
			detailHint.string = "outlined knots differ from the live patch";
		};
		fingerprint.refresh;
		^this
	}

	prSave {
		if(snapshots.isNil) {
			detailHint.string = "start the instrument before saving a snapshot";
			^this
		};
		snapshots.save(nameField.string);
		selectedName = snapshots.list.detect { |name| name == nameField.string };
		this.refresh;
	}

	prRecall {
		if(snapshots.notNil and: { selectedName.notNil }) {
			snapshots.recall(selectedName.asSymbol)
		} {
			detailHint.string = "select a saved snapshot before recall";
		};
		^this
	}

	prMorph {
		if(snapshots.notNil and: { aMenu.item.notNil } and: { bMenu.item.notNil }) {
			snapshots.morph(aMenu.item.asSymbol, bMenu.item.asSymbol, durationBox.value)
		} {
			detailHint.string = "choose two saved snapshots before morph";
		};
		^this
	}

	prDataWeight { |data, src, dest|
		var entry;
		if(data.isNil) { ^matrix.getPatch(src, dest) };
		entry = (data[\matrix] ? []).detect { |triplet|
			triplet[0] == src and: { triplet[1] == dest }
		};
		^if(entry.isNil) { 0.0 } { CQMatrix.clampWeight(entry[2]) }
	}

	prDrawFingerprint {
		var palette = CQInk.palette(skin), data = selectedData;
		var box = fingerprint.bounds.moveTo(0, 0);
		var m = CQGuiScale.metrics(step);
		var left = if(step == \roomy) { 112 } { 92 };
		var top = if(step == \roomy) { 62 } { 50 };
		// the fingerprint fills whatever the rail leaves it, so a wider window
		// buys patch detail rather than a small picture in a big box
		var cellW = ((box.width - left - 24) / CQMatrix.destNames.size)
			.floor.clip(24, 48);
		var cellH = ((box.height - top - 34) / CQMatrix.sourceNames.size)
			.floor.clip(11, 26);
		var width = CQMatrix.destNames.size * cellW;
		var height = CQMatrix.sourceNames.size * cellH;
		var active = 0, verso = 0, inverso = 0, changed = 0;
		// the row labels are the fingerprint's own readable text, so they grow
		// with the cells rather than staying at the page's smallest size
		var rowSize = (cellH - 4).clip(m[\tiny], m[\label]);
		var groups;
		CQInk.drawToolBox(box, "patch fingerprint", skin, selectedName.notNil);
		groups = [(start: 0, count: 5, title: "coco 1"),
			(start: 5, count: 5, title: "coco 2"),
			(start: 10, count: 5, title: "castle fm"),
			(start: 15, count: 5, title: "spesal"),
			(start: 20, count: 2, title: "decks")];
		groups.do { |group|
			var x = left + (group[\start] * cellW);
			Pen.stringCenteredIn(group[\title],
				Rect(x, top - m[\tiny] - 8, group[\count] * cellW, m[\tiny] + 4),
				CQInk.font(\range, m[\tiny]), palette[\line]);
		};
		Pen.strokeColor = palette[\whisper].copy.alpha_(0.16);
		Pen.width = CQInk.strokes[\hairline];
		CQMatrix.sourceNames.do { |src, si|
			var y = top + (si * cellH) + (cellH / 2);
			Pen.line(Point(left, y), Point(left + width, y));
		};
		CQMatrix.destNames.do { |dest, di|
			var x = left + (di * cellW) + (cellW / 2);
			Pen.line(Point(x, top), Point(x, top + height));
		};
		Pen.stroke;
		CQMatrix.sourceNames.do { |src, si|
			var y = top + (si * cellH) + (cellH / 2);
			var family = this.prSourceFamily(src);
			Pen.stringAtPoint(this.prSourceLabel(src),
				8@(y - (rowSize / 2) - 1),
				CQInk.font(\range, rowSize), palette[\whisper]);
			Pen.fillColor = palette[family];
			Pen.fillOval(Rect(left - 12, y - 2.5, 5, 5));
			CQMatrix.destNames.do { |dest, di|
				var weight = this.prDataWeight(data, src, dest);
				var live = matrix.getPatch(src, dest);
				var x = left + (di * cellW) + (cellW / 2), radius, color;
				if(weight != 0) {
					active = active + 1;
					if(weight > 0) { verso = verso + 1 } { inverso = inverso + 1 };
					radius = 2.5 + (weight.abs * 4.5);
					color = if(weight > 0) { palette[\verso] } { palette[\inverso] };
					Pen.fillColor = color.copy.alpha_(0.52 + (weight.abs * 0.48));
					Pen.fillOval(Rect(x - radius, y - radius, radius * 2, radius * 2));
				};
				if(selectedName.notNil and: { (weight - live).abs > 0.005 }) {
					changed = changed + 1;
					Pen.strokeColor = palette[\line].copy.alpha_(0.72);
					Pen.width = CQInk.strokes[\hairline];
					Pen.strokeOval(Rect(x - 6, y - 6, 12, 12));
				};
			};
		};
		Pen.stringRightJustIn("% routes · % verso · % inverso · % changed"
			.format(active, verso, inverso, changed),
			Rect(box.width - 396, box.height - (m[\mono] + 12), 384, m[\mono] + 4),
			CQInk.font(\number, m[\mono]), palette[\line]);
	}

	prDrawMorphMeter {
		var palette = CQInk.palette(skin), progress, label, rail;
		progress = if(snapshots.isNil) { 0.0 } { snapshots.morphProgress ? 0.0 };
		rail = Rect(5, 7, morphMeter.bounds.width - 70, 10);
		Pen.fillColor = palette[\recess];
		Pen.fillRect(rail);
		Pen.fillColor = palette[\spaf].copy.alpha_(0.72);
		Pen.fillRect(Rect(rail.left, rail.top, rail.width * progress.clip(0, 1), rail.height));
		Pen.strokeColor = palette[\whisper];
		Pen.width = CQInk.strokes[\hairline];
		Pen.strokeRect(rail);
		label = if(snapshots.notNil and: { snapshots.isMorphing }) {
			(progress * 100).round.asInteger.asString ++ "%"
		} {
			if(progress >= 1) { "arrived" } { "ready" }
		};
		Pen.stringRightJustIn(label, Rect(rail.right + 6, 4, 54, 14),
			CQInk.font(\number), palette[\line]);
	}

	prSourceLabel { |src|
		^switch(src,
			\envA, { "env a" }, \envB, { "env b" },
			\coco1, { "coco 1" }, \coco2, { "coco 2" },
			\org1, { "org 1" }, \org2, { "org 2" },
			{ src.asString })
	}

	prSourceFamily { |src|
		var name = src.asString;
		^case
			{ name.beginsWith("tri") } { \tri }
			{ name.beginsWith("env") } { \input }
			{ name.beginsWith("org") } { \organ }
			{ \cas }
	}
}
