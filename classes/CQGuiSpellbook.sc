// CQGuiSpellbook — Ink presentation for CQSpellbook (GUI_SPEC §8).
// The read-only journal keeps one persistent native selection so the line to
// be copied is always unambiguous. Batch ownership is shown below the journal.

CQGuiSpellbook {
	var gui, model, ctl, copyHelper, operationStatus;
	var <listView;
	var <view, <rail, header, copyLine, copyPreset, topPad;
	var <step = \compact;

	*new { |gui, model| ^super.newCopyArgs(gui, model).prBuild }

	// The journal shares the workbench's content rail and size step, so the
	// all Drawer pages line up on one edge and read at one size (GUI_SPEC §8).
	applyScale { |nextStep, width|
		var m;
		step = nextStep ? step;
		m = CQGuiScale.metrics(step);
		rail.fixedWidth = width;
		header.font = CQInk.font(\notice, m[\notice]);
		listView.font = CQInk.font(\spellbook, m[\mono]);
		operationStatus.font = CQInk.font(\label, m[\label]);
		operationStatus.fixedHeight = m[\barHeight];
		[copyLine, copyPreset].do { |button|
			button.scale = m;
			button.resizeTo(CQGuiScale.buttonWidth(step, button.label),
				m[\boxHeight]);
		};
		topPad.fixedHeight = m[\vMargin];
		^this
	}

	free {
		ctl !? { ctl.remove };
		ctl = nil;
	}

	applySkin { |skin|
		var palette = CQInk.palette(skin);
		view.background = palette[\recess];
		listView.background = palette[\ground];
		listView.stringColor = palette[\line];
		listView.selectedStringColor = palette[\line];
		listView.hiliteColor = palette[\spaf].copy.alpha_(0.28);
		operationStatus.stringColor = palette[\whisper];
		copyHelper.background = palette[\ground];
		copyHelper.stringColor = palette[\line];
		^this
	}

	selectedIndex { ^listView.value }
	selectedLine {
		var index = listView.value;
		if(index.isNil) { ^"" };
		^model.lines.at(index) ? ""
	}
	copySelectedLine { this.prCopy(this.selectedLine); ^this }
	copyPresetData { this.prCopy(model.presetDataString); ^this }

	// ---- private ----

	prBuild {
		var palette = CQInk.palette(\night);
		var m = CQGuiScale.metrics(step);
		listView = ListView().selectionMode_(\single)
			.font_(CQInk.font(\spellbook, m[\mono])).stringColor_(palette[\line])
			.selectedStringColor_(palette[\line])
			.hiliteColor_(palette[\spaf].copy.alpha_(0.28))
			.background_(palette[\ground]).items_(model.lines);
		listView.action = { this.prShowSelection };
		listView.selectionAction = { this.prShowSelection };
		copyHelper = TextView().fixedSize_(Size(1, 1)).visible_(false);
		copyLine = this.prButton("copy line",
			CQGuiScale.buttonWidth(step, "copy line"), { this.copySelectedLine });
		copyPreset = this.prButton("copy all as preset data",
			CQGuiScale.buttonWidth(step, "copy all as preset data"),
			{ this.copyPresetData });
		header = StaticText().string_("read-only patch journal · committed operations · every layer")
			.font_(CQInk.font(\notice, m[\notice])).stringColor_(palette[\whisper]);
		operationStatus = StaticText().string_("no committed operation selected")
			.font_(CQInk.font(\label, m[\label])).stringColor_(palette[\whisper])
			.fixedHeight_(m[\barHeight]);
		rail = View().fixedWidth_(CQGuiScale.railWidth(step, 1272)).layout_(VLayout(
			HLayout(header, nil, copyLine.view, copyPreset.view)
				.margins_(0).spacing_(6),
			listView,
			operationStatus,
			copyHelper
		).margins_(0).spacing_(6));
		// the shared page frame (CQGuiScale.pageFrame)
		topPad = View().fixedHeight_(m[\vMargin]);
		view = View().background_(palette[\recess]).layout_(VLayout(
			topPad,
			HLayout(nil, rail, nil).margins_(0).spacing_(0)
		).margins_(0).spacing_(0));
		ctl = SimpleController(model);
		ctl.put(\entry, {
			{
				listView.items = model.lines;
				listView.value = (model.lines.size - 1).max(0);
				this.prShowSelection;
			}.defer
		});
		^this
	}

	prShowSelection {
		var record, index = listView.value;
		if(index.isNil) {
			operationStatus.string = "no committed operation selected";
			^this
		};
		record = model.lineRecords.at(index);
		if(record.isNil) { ^this };
		operationStatus.string = "operation % · row %/%".format(
			record[\operation], record[\lineIndex] + 1, record[\lineCount]);
		^this
	}

	// returns the control: the page re-measures it when the size step changes
	prButton { |label, width, action|
		var button = CQInkControl.button(label, width, \night, false, true, \line);
		button.surface = \recess;
		button.action = action;
		button.scale = CQGuiScale.metrics(step);
		gui.registerInkControl(button);
		^button
	}

	prCopy { |string|
		var value = string ? "";
		if(value.isEmpty) { ^this };
		copyHelper.string = value;
		copyHelper.select(0, value.size);
		copyHelper.invokeMethod(\copy);
		^this
	}
}
