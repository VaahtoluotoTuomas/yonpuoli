// CQGuiPanels — the persistent Master strip of the Night-only Yönpuoli GUI.
// Continuous controls come from CQGuiWidgets (the CQState funnel); transport
// actions call the Cocoquantus facade. Meters register view setters with
// CQGui, whose OSC listeners drive them.

CQGuiPanels {
	*masterItems {
		^#[masterVol, masterDry, patchDepth, gr, notice, rec, panic]
	}

	// The strip's exact interactive CQSpec ownership. Read-only GR/notice and
	// recorder action REC do not count; PANIC owns monitorMute.
	*masterParamNames {
		^#[masterVol, masterDry, patchDepth, monitorMute]
	}

	*master { |gui|
		var w = gui.widgets, c = CQGuiWidgets.colors;
		var chrome = CQGuiScale.chrome;
		var meterL = this.prMeter(gui, false, false, \recess);
		var meterR = this.prMeter(gui, false, false, \recess);
		var grMeter = this.prMeter(gui, true, true, \recess);
		// One left edge for status and REC. Three lines leave room for combined
		// audio/mic/output notices without moving the transport buttons.
		var notice = this.prLabel("", \whisper, \topLeft, \notice, chrome[\notice])
			.fixedSize_(Size(280, 52));
		var recBtn = this.prButton(gui, "REC", 58, true, false,
			\danger, \rec, \recess);
		var recTime = this.prLabel("", \danger, \left, \number, chrome[\mono])
			.fixedWidth_(56);
		var panicBtn = this.prButton(gui, "PANIC", 82, true, false,
			\danger, \panic, \recess);
		var panel, controls, help, transport;
		[recBtn, panicBtn].do { |button| button.scale = chrome };
		gui.setMeter(\master, { |pkL, pkR|
			meterL.value = this.ampMeter(pkL);
			meterR.value = this.ampMeter(pkR);
		});
		gui.setMeter(\gr, { |db| grMeter.value = (db / 12).clip(0, 1) });
		gui.notice_(notice);
		gui.recButton_(recBtn);
		gui.recTime_(recTime);
		gui.registerInfo(meterL.view, [\master, \out]);
		gui.registerInfo(meterR.view, [\master, \out]);
		gui.registerInfo(grMeter.view, [\master, \gr]);
		gui.registerInfo(notice, [\master, \notice]);
		gui.registerInfo(recBtn.view, [\master, \rec]);
		gui.registerInfo(recTime, [\master, \rec]);
		gui.registerInfo(panicBtn.view, [\master, \panic]);
		recBtn.view.fixedSize = Size(88, 34);
		panicBtn.view.fixedSize = Size(112, 34);
		recBtn.action = { |b|
			if(b.value == 1) { Cocoquantus.rec } { Cocoquantus.recStop };
		};
		panicBtn.action = {
			if((Cocoquantus.get(\monitorMute) ? 0) > 0)
				{ Cocoquantus.resume } { Cocoquantus.panic };
		};
		gui.widgets.register(\monitorMute, {
			|v| panicBtn.value = (v ? 0).clip(0, 1)
		});
		help = UserView().fixedWidth_(400);
		gui.hoverView = help;
		controls = UserView().fixedWidth_(280);
		gui.masterControls = controls;
		controls.drawFunc = {
			CQInk.drawToolHeader(controls.bounds.moveTo(0, 0), "master", \night, chrome[\label])
		};
		panel = UserView().fixedHeight_(118)
			.background_(CQInk.palette(\night)[\recess]);
		panel.drawFunc = {
			Pen.strokeColor = CQInk.palette(\night)[\whisper];
			Pen.width = CQInk.strokes[\hairline];
			Pen.line(8@0, Point(panel.bounds.width - 8, 0)); Pen.stroke;
		};
		controls.layout = VLayout(
			View().fixedHeight_(chrome[\label] + 8),
			HLayout(
				VLayout(HLayout(meterL.view, meterR.view).spacing_(2),
					this.prLabel("out", \whisper, \center, \range,
						chrome[\label] - 2)).spacing_(0),
				VLayout(grMeter.view,
					this.prLabel("gr", \whisper, \center, \range,
						chrome[\label] - 2)).spacing_(0),
				w.knob(\masterVol, "vol", nil, \primary, \recess, chrome),
				w.knob(\masterDry, "dry", nil, \standard, \recess, chrome),
				w.knob(\patchDepth, "depth", c[\spaf], \standard, \recess, chrome)
			).margins_(6).spacing_(8)
		).margins_(0).spacing_(0);
		transport = UserView().fixedWidth_(296);
		transport.drawFunc = {
			CQInk.drawToolHeader(transport.bounds.moveTo(0, 0), "session", \night, chrome[\label])
		};
		transport.layout = VLayout(
			View().fixedHeight_(chrome[\label] + 8), View().fixedHeight_(6),
			HLayout(recBtn.view, recTime, nil, panicBtn.view).spacing_(8),
			View().fixedHeight_(4), notice, nil).margins_(8, 0, 8, 0).spacing_(0);
		// Transport is one compact group; the larger inter-group gap separates
		// PANIC from the meter bank without inserting another box or divider.
		panel.layout = HLayout(help, nil, HLayout(transport, controls).spacing_(40))
			.margins_(6, 0, 6, 0).spacing_(16);
		^panel
	}

	// -60..0 dBFS onto 0..1 for the meters.
	*ampMeter { |amp|
		^(((amp ? 0).max(1e-5).ampdb + 60) / 60).clip(0, 1)
	}

	*prLabel { |text, color = \whisper, align = \left, role = \label, size|
		var palette = CQInk.palette(\night), label;
		label = StaticText().string_(text).font_(CQInk.font(role, size))
			.align_(align)
			.stringColor_(palette[color] ?? { palette[\whisper] })
			.fixedHeight_((size ? 10) + 2);
		^label
	}

	*prButton { |gui, label, width = 58, toggle = false, momentary = false,
		accent = \line, glyph, surface = \ground|
		var button = CQInkControl.button(label, width, \night, toggle,
			momentary, accent, glyph);
		button.surface = surface;
		gui.registerInkControl(button);
		^button
	}

	*prMeter { |gui, danger = false, downward = false, surface = \ground|
		var meter = CQInkControl.meter(\night, danger, downward);
		meter.surface = surface;
		gui.registerInkControl(meter);
		^meter
	}
}
