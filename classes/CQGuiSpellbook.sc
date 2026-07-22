// CQGuiSpellbook — Ink presentation for CQSpellbook (GUI_SPEC §8).
// The read-only journal keeps one persistent native selection so the line to
// be copied is always unambiguous. Batch ownership is shown below the journal.

CQGuiSpellbook {
	var gui, model, ctl, copyHelper, operationStatus;
	var <listView;
	var <view;

	*new { |gui, model| ^super.newCopyArgs(gui, model).prBuild }

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
		var palette = CQInk.palette(\day), copyLine, copyPreset, header;
		listView = ListView().selectionMode_(\single)
			.font_(CQInk.font(\spellbook)).stringColor_(palette[\line])
			.selectedStringColor_(palette[\line])
			.hiliteColor_(palette[\spaf].copy.alpha_(0.28))
			.background_(palette[\ground]).items_(model.lines);
		listView.action = { this.prShowSelection };
		listView.selectionAction = { this.prShowSelection };
		copyHelper = TextView().fixedSize_(Size(1, 1)).visible_(false);
		copyLine = this.prButton("copy line", 78, { this.copySelectedLine });
		copyPreset = this.prButton("copy all as preset data", 162,
			{ this.copyPresetData });
		header = StaticText().string_("read-only patch journal · committed operations · every layer")
			.font_(CQInk.font(\notice)).stringColor_(palette[\whisper]);
		operationStatus = StaticText().string_("no committed operation selected")
			.font_(CQInk.font(\label)).stringColor_(palette[\whisper])
			.fixedHeight_(18);
		view = View().background_(palette[\recess]).layout_(VLayout(
			HLayout(header, nil, copyLine, copyPreset).spacing_(6),
			listView,
			operationStatus,
			copyHelper
		).margins_(4).spacing_(4));
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

	prButton { |label, width, action|
		var button = CQInkControl.button(label, width, \day, false, true, \line);
		button.surface = \recess;
		button.action = action;
		gui.registerInkControl(button);
		^button.view
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
