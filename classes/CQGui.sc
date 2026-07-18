// CQGui — the panel window (SPEC §6.2, ARCH §5): one window, four columns
// mirroring the hardware layout + master strip + banana-bay grid + keyboard
// map. Pure view: it renders from CQState/CQMatrix dependant notifications
// and server reports (OSCdefs \cq_gui_*, freed here), and every write goes
// through the funnels (CQGuiWidgets -> CQState, grid -> CQMatrix, actions ->
// Cocoquantus facade). All UI work happens inside { }.defer; no server polling
// — the only language-side timer is the 2 Hz REC elapsed-time display.

CQGui {
	var state, matrix, config;
	var <window, <widgets, updaters;
	var stateCtl, gridPanel, meterFuncs;
	var castleLeds, spesalView, <spesalStates;
	var noticeView, recBtn, recTimeView, recTask;
	var micLive = false, cleaned = false;

	*new { |state, matrix, config|
		^super.newCopyArgs(state, matrix, config).prInit
	}

	isClosed { ^window.isNil or: { window.isClosed } }

	free {
		this.prCleanup;
		{ window !? { if(window.isClosed.not) { window.close } } }.defer;
	}

	// ---- panel registration API (used by CQGuiPanels while building) ----

	setMeter { |key, func| meterFuncs[key] = func }
	castleLeds_ { |views| castleLeds = views }
	spesalView_ { |view| spesalView = view }
	notice_ { |view| noticeView = view }
	recButton_ { |btn| recBtn = btn }
	recTime_ { |view| recTimeView = view }

	// SPEC §7: persistent warning color while the mic is live (M2 GUI part)
	micNotice { |live|
		micLive = live;
		this.prUpdateNotice;
	}

	// ---- private ----

	prInit {
		updaters = IdentityDictionary.new;
		meterFuncs = IdentityDictionary.new;
		spesalStates = Cocoquantus.spesalStates;
		widgets = CQGuiWidgets(state, updaters);
		{
			this.prBuild;
			this.prListen;
			this.prStartRecTask;
		}.defer;
	}

	prBuild {
		var presetMenu = PopUpMenu()
			.items_(["preset…"] ++ CQMatrix.presets.keys.asArray
				.collect(_.asString).sort);
		presetMenu.action = { |m|
			if(m.value > 0) { Cocoquantus.patchPreset(m.item.asSymbol) };
		};
		gridPanel = CQGuiMatrix(matrix);
		window = Window("Cocoquantus", Rect(50, 50, 1120, 740));
		window.view.layout = VLayout(
			HLayout(
				CQGuiPanels.sources(this),
				CQGuiPanels.coco(this, 1),
				CQGuiPanels.quantussy(this),
				CQGuiPanels.coco(this, 2),
				CQGuiPanels.master(this)
			),
			HLayout(
				StaticText().string_("MATRIX").font_(Font.sansSerif(11, true)),
				presetMenu,
				StaticText()
					.string_("keys: 1/2 hold · q/w flip · a/s skip · "
						++ "3..7 spesal · r rec · space panic · p shake")
					.font_(Font.sansSerif(9)).stringColor_(Color.gray(0.45)),
				nil),
			HLayout(gridPanel.view, nil)
		);
		window.view.keyDownAction = { |v, char| this.prKey(char) };
		window.onClose = { this.prCleanup };
		window.front;
	}

	// one SimpleController fans \param notifications out to the widget
	// updaters; OSCdefs carry the server reports (meters, LEDs, spesal)
	prListen {
		var colors = CQGuiWidgets.colors;
		stateCtl = SimpleController(state);
		stateCtl.put(\param, { |obj, what, name, val|
			{ updaters[name].do { |f| f.value(val) } }.defer;
		});
		OSCdef(\cq_gui_meters, { |msg|
			{ meterFuncs[\master].value(msg[3], msg[5]) }.defer;
		}, '/cq_meters');
		OSCdef(\cq_gui_gr, { |msg|
			{ meterFuncs[\gr].value(max(msg[3] ? 0, msg[4] ? 0)) }.defer;
		}, '/cq_gr');
		OSCdef(\cq_gui_coco, { |msg|
			var key = ("coco" ++ msg[2].asInteger).asSymbol;
			{ meterFuncs[key].value(msg[3], msg[5]) }.defer;
		}, '/cq_cocometers');
		OSCdef(\cq_gui_castle, { |msg|
			{
				castleLeds !? {
					castleLeds.do { |led, i|
						led.background = Color.gray(0.85)
							.blend(colors[\tri], (((msg[3 + i] ? 0) + 1) * 0.5).clip(0, 1));
					};
				};
			}.defer;
		}, '/cq_castle');
		OSCdef(\cq_gui_spesal, { |msg|
			{
				spesalStates = msg.copyRange(3, 7).collect { |v| v.asInteger };
				spesalView !? { spesalView.refresh };
			}.defer;
		}, '/cq_spesal');
	}

	// 2 Hz language-side tick for the REC button + elapsed display (SPEC §6.2)
	prStartRecTask {
		recTask = Task {
			loop {
				var r = Cocoquantus.recorder;
				var on = r.notNil and: { r.isRecording };
				recBtn !? { recBtn.value = on.binaryValue };
				recTimeView !? {
					recTimeView.string = if(on) {
						var secs = r.elapsed.asInteger;
						"%:%".format(secs div: 60, (secs % 60).asString.padLeft(2, "0"))
					} { "" };
				};
				0.5.wait;
			};
		}.play(AppClock);
	}

	// SPEC §6.2 keyboard map
	prKey { |char|
		switch(char,
			$1, { Cocoquantus.hold(1) },
			$2, { Cocoquantus.hold(2) },
			$q, { Cocoquantus.flip(1) },
			$w, { Cocoquantus.flip(2) },
			$a, { Cocoquantus.skip(1) },
			$s, { Cocoquantus.skip(2) },
			$r, {
				var r = Cocoquantus.recorder;
				if(r.notNil and: { r.isRecording })
					{ Cocoquantus.recStop } { Cocoquantus.rec };
			},
			$p, { Cocoquantus.shake },
			$ , {
				if((Cocoquantus.get(\monitorMute) ? 0) > 0)
					{ Cocoquantus.resume } { Cocoquantus.panic };
			},
			{
				if(char.isDecDigit and: { char.digit.inclusivelyBetween(3, 7) }) {
					Cocoquantus.spesal(char.digit - 2);
				};
			}
		);
	}

	prUpdateNotice {
		noticeView !? {
			noticeView.string = if(micLive) {
				"MIC LIVE — feedback risk with builtin speakers"
			} { "" };
		};
	}

	// idempotent teardown shared by free and the window's close box: language-
	// side listeners die here so a closed window leaves no dangling \cq_gui_*
	prCleanup {
		if(cleaned) { ^this };
		cleaned = true;
		stateCtl !? { stateCtl.remove };
		stateCtl = nil;
		gridPanel !? { gridPanel.free };
		recTask !? { recTask.stop };
		recTask = nil;
		#[cq_gui_meters, cq_gui_gr, cq_gui_coco, cq_gui_castle,
			cq_gui_spesal].do { |k| OSCdef.all[k] !? { |d| d.free } };
	}
}
