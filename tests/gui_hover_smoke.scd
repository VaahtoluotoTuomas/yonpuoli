// Serverless native layout/input audit. Human readability/playing acceptance
// remains in manual_checklist.md. Run only through run_sclang_gate.ps1.
(
var gui, state = CQState(nil), matrix = CQMatrix(nil), ok = true, check, capture;
check = { |condition, message|
	if(condition) { ("ok: " ++ message).postln } { ok = false; ("FAIL: " ++ message).postln };
};
capture = { |name|
	var image, tasks = [gui.slotAt(\recTask), gui.info.pulseTask].reject(_.isNil);
	tasks.do(_.stop); 0.2.wait;
	image = Image.fromWindow(gui.window, gui.window.view.bounds.moveTo(0, 0));
	image.write(Cocoquantus.repoDir +/+ "tests/tmp" +/+ name ++ ".png"); image.free;
	tasks.do { |t| t.play(AppClock) };
};
CQInkControl.resetDrawErrors;
{
	try {
		var map, help, before, beforeMatrix, targets, misses = List.new, source, point;
		var local, rec, panic, notice, recTime, probe, oldOver, oldLeave, downs = 0, wheels = 0, actionControl;
		gui = CQGui(state, matrix, CQConfig.validate((headless: true)));
		0.6.wait;
		gui.window.bounds = Rect(-100000, -100000, 1280, 720); 0.3.wait;
		map = gui.nightMap; help = gui.hoverHelp;
		before = state.asEvent.copy; beforeMatrix = matrix.asArray;
		local = { |region|
			var point = region[\point];
			// Info's annotation points to the centre; the actual Organ hand
			// is the annulus around the output jack, not that jack's centre.
			if(region[\target][0] == \param and: { region[\target][1].asString.endsWith("Organ") }) {
				point = point + Point((CQNightMeasures.organPodInner + CQNightMeasures.organPodOuter) * 0.5, 0);
			};
			point + map.container.mapToGlobal(0@0) - region[\view].mapToGlobal(0@0)
		};
		check.value(gui.info.model.enabled.not and: { help.note.isNil }, "help starts empty without enabling info");
		map.infoHitRegions.do { |r|
			var at = local.value(r), v = r[\view];
			v.mouseOverAction.value(v, at.x, at.y);
			if(help.note.isNil) { misses.add(r[\target]) };
			v.mouseLeaveAction.value(v);
		};
		check.value(misses.isEmpty, "every native functional hover has a note");
		if(misses.notEmpty) { misses.postln };
		source = map.infoHitRegions.detect { |r| r[\target] == [\param, \coco1In] };
		point = local.value(source);
		source[\view].mouseOverAction.value(source[\view], point.x, point.y);
		check.value(help.target == [\param, \coco1In], "ordinary hover identifies input, not the whole Coco");
		gui.info.enabled = true;
		check.value(help.target == [\ui, \inspectInfo], "enabling info immediately replaces the old hover note");
		source[\view].mouseOverAction.value(source[\view], point.x, point.y);
		check.value(help.target == [\ui, \inspectInfo] and: { gui.info.model.target.isNil },
			"info shows one stable instruction instead of a second element explanation");
		help.clear;
		check.value(help.target == [\ui, \inspectInfo], "empty space keeps the info-mode instruction visible");
		gui.info.activate(\tour);
		check.value(help.target == [\ui, \guidedInfo], "the combined guide explains playing instead of inspection");
		gui.info.activate(\advanced);
		check.value(help.target == [\ui, \advancedInfo] and: { gui.info.model.playing }, "advanced has its own stable play instruction");
		gui.info.activate(\shortcuts);
		check.value(help.target == [\ui, \inspectInfo], "reading Shortcuts restores the inspection instruction");
		gui.info.dismiss;
		check.value(help.target == [\ui, \inspectInfo], "closing a card keeps the mode instruction");
		gui.info.select([\cable, \tri1, \coco1_spaf], map.groundView, 20@20);
		check.value(help.target == [\ui, \readingGuide] and: { gui.info.model.inspecting },
			"cable entry explains reading rather than promising playable controls");
		gui.info.activate(\next); help.clear;
		check.value(help.target == [\ui, \readingGuide] and: { gui.info.model.inspecting },
			"paging and empty space keep the cable-entry reading instruction");
		gui.info.activate(\tour);
		check.value(help.target == [\ui, \guidedInfo] and: { gui.info.model.playing },
			"toolbar entry restores playing and its matching instruction");
		gui.info.dismiss;
		source[\view].mouseOverAction.value(source[\view], point.x, point.y);
		gui.info.enabled = false;
		check.value(help.target == [\param, \coco1In], "leaving info restores the current ordinary hover");
		gui.info.enabled = true; gui.info.enabled = false;
		0.1.wait;
		check.value(gui.shell.infoGuideButton.view.visible.not and: { gui.shell.infoAdvancedButton.view.visible.not }, "quick info on/off leaves both guide buttons hidden");
		map.groundView.mouseOverAction.value(map.groundView, 20, 20);
		check.value(help.note.isNil and: { help.lines.isEmpty }, "empty background clears title and text");
		check.value(state.asEvent == before and: { matrix.asArray == beforeMatrix }, "hover never changes music");
		rec = gui.infoBindings.detect { |b| b[1] == [\master, \rec] }[0];
		panic = gui.infoBindings.detect { |b| b[1] == [\master, \panic] }[0];
		notice = gui.infoBindings.detect { |b| b[1] == [\master, \notice] }[0];
		recTime = gui.infoBindings.select { |b| b[1] == [\master, \rec] }[1][0];
		targets = map.infoHitRegions.collect { |r| r[\target] }
			++ gui.infoBindings.collect { |b| b[1] }
			++ gui.hoverBindings.collect { |b| b[1] }
			++ gui.shell.hoverBindings.collect { |b| b[1] }
			++ gui.inkControls.select { |c| CQHoverText.ui(c.label.asSymbol).notNil }
				.collect { |c| [\ui, c.label.asSymbol] }
			++ [[\ui, \inspectInfo], [\ui, \guidedInfo], [\ui, \advancedInfo], [\ui, \readingGuide]];
		[Size(1280, 720), Size(1528, 900)].do { |size|
			gui.window.bounds = Rect(-100000, -100000, size.width, size.height); 0.3.wait;
			check.value(gui.hoverView.bounds.width == 400 and: { gui.shell.masterView.bounds.height == 118 },
				"help has a bounded width and does not steal stage height");
			check.value(gui.hoverView.mapToGlobal(400@0).x < rec.mapToGlobal(0@0).x
				and: { rec.mapToGlobal(Point(rec.bounds.width, 0)).x < panic.mapToGlobal(0@0).x }
				and: { panic.mapToGlobal(Point(panic.bounds.width, 0)).x < gui.masterControls.mapToGlobal(0@0).x },
				"footer order is help, REC, PANIC, then all Master controls");
			check.value(rec.bounds.size == Size(88, 34) and: { panic.bounds.size == Size(112, 34) },
				"REC and PANIC retain full hit areas");
			check.value(notice.mapToGlobal(0@0).x == rec.mapToGlobal(0@0).x
				and: { rec.mapToGlobal(0@0).y == panic.mapToGlobal(0@0).y }
				and: { notice.mapToGlobal(Point(notice.bounds.width, 0)).x
					== panic.mapToGlobal(Point(panic.bounds.width, 0)).x },
				"status and transport share exact left/right edges and buttons share a baseline");
			check.value((gui.masterControls.mapToGlobal(0@0).x
				- panic.mapToGlobal(Point(panic.bounds.width, 0)).x) >= 40,
				"transport has a deliberate breathing gap before Master");
			targets.do { |target|
				help.show(target);
				check.value(help.note.notNil and: { help.lines.size <= 3 }
					and: { help.note[\title].bounds(CQInk.font(\label, 13)).width <= 376 }
					and: { help.lines.every { |line| line.bounds(CQInk.font(\label, 12)).width <= 376 } },
					"whole hover note fits: " ++ target);
			};
			help.show([\param, \coco1In]); capture.value("hover-input-" ++ size.width);
			help.show([\bird, 0]); capture.value("hover-bird-" ++ size.width);
			help.show([\endpoint, \coco1_slotX]); capture.value("hover-bridge-" ++ size.width);
			help.clear; capture.value("hover-empty-" ++ size.width);
			notice.string = "TEMPORARY AUDIO · drawer → audio";
			capture.value("footer-status-" ++ size.width);
			notice.string = "MIC LIVE — feedback risk with builtin speakers · OUTPUT HOT — auto-trimmed −6 dB · TEMPORARY AUDIO · drawer → audio";
			check.value(CQGuiInfo.wrap(notice.string, notice.bounds.width - 4,
				{ |s| s.bounds(notice.font).width }).join(Char.nl).bounds(notice.font).height
				<= notice.bounds.height, "combined safety notices fit the reserved status area");
			// Keep a visible elapsed-time example while the recorder painter is paused.
			gui.slotAt(\recTask).stop; recTime.string = "02:34";
			capture.value("footer-recording-" ++ size.width);
			recTime.string = "";
			notice.string = "";
			gui.info.enabled = true;
			capture.value("hover-info-mode-" ++ size.width);
			gui.info.activate(\tour);
			capture.value("hover-guided-mode-" ++ size.width);
			gui.info.enabled = false;
		};
		gui.shell.toggleDrawer; 0.2.wait;
		check.value(help.note.isNil, "stage change clears a stale hint");
		gui.shell.selectDrawer(\workshop); 0.1.wait;
		source = gui.drawer.workshop.rowFor(\coco1Bits).view;
		source.mouseOverAction.value(source, 10, 10);
		check.value(help.target == [\param, \coco1Bits], "Workshop uses the same passive help");
		capture.value("hover-workshop");
		actionControl = gui.inkControls.detect { |c| c.label == "apply preset" };
		actionControl.label = "cancel";
		actionControl.view.mouseOverAction.value(actionControl.view, 10, 10);
		check.value(help.note[\title] == "cancel", "a relabelled action describes its current purpose");
		actionControl.label = "apply preset";
		gui.shell.toggleDrawer; 0.2.wait;
		// The observer wraps only over/leave: all playing callbacks survive.
		probe = UserView(gui.window.view, Rect(0, 0, 2, 2));
		oldOver = { }; oldLeave = { };
		probe.mouseOverAction = oldOver; probe.mouseLeaveAction = oldLeave;
		probe.mouseDownAction = { downs = downs + 1 };
		probe.mouseWheelAction = { wheels = wheels + 1 };
		help.bindTree(probe, { [\bird, 0] });
		probe.mouseDownAction.value(probe, 0, 0, 0, 0, 1);
		probe.mouseWheelAction.value(probe, 0, 0, 0, 0, 1);
		check.value(downs == 1 and: { wheels == 1 }, "passive help leaves clicks and wheel untouched");
		check.value(help.paintErrors == 0 and: { CQInkControl.drawErrors == 0 }, "all hover and footer paint is error-free");
		gui.info.free; help.free;
		check.value(probe.mouseOverAction === oldOver and: { probe.mouseLeaveAction === oldLeave },
			"teardown restores hover callbacks");
		gui.free; 0.2.wait;
		gui = CQGui(CQState(nil), CQMatrix(nil), CQConfig.validate((headless: true))); 0.6.wait;
		gui.hoverHelp.show([\bird, 0]); gui.window.close; 0.2.wait;
		check.value(gui.hoverHelp.isNil, "native close releases the help observer");
		if(ok) { "CQ_GUI_HOVER_PASSED".postln; 0.exit } { "CQ_GUI_HOVER_FAILED".postln; 1.exit };
	} { |error| error.reportError; gui !? { gui.free }; "CQ_GUI_HOVER_FAILED".postln; 1.exit };
}.fork(AppClock);
)
