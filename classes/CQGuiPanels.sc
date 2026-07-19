// CQGuiPanels — the panel columns of CQGui (SPEC §6.2): [Sources+Preamps]
// [Coco1] [Quantussy] [Coco2] + master strip. Pure view builders: continuous
// controls come from the CQGuiWidgets factory (funnel-bound), discrete actions
// call the Cocoquantus facade (which itself only writes through the funnels).
// Meters/LEDs register setter functions with CQGui; its OSCdefs drive them.

CQGuiPanels {
	*dayRegions { ^#[sources, coco1, quantussy, coco2] }
	*masterItems { ^#[masterVol, masterDry, spaceMix, patchDepth, gr, notice, rec, panic] }
	*memoryItems { ^#[snapshotName, snapshotSave, snapshotRecall, morph] }
	*sourceEndpointNames { ^CQMatrix.sourceNames }
	*destEndpointNames { ^CQMatrix.destNames }
	*cocoIslands { ^#[identity, performance, converter, output] }
	*sourcesIslands { ^#[inputs, decks] }
	*quantussyIslands { ^#[macro, castles, topology] }

	*coco { |gui, n|
		var w = gui.widgets;
		var p = { |suffix| ("coco" ++ n ++ suffix).asSymbol };
		var d = { |suffix| ("coco" ++ n ++ suffix).asSymbol };
		var inMeter = this.prMeter(gui), outMeter = this.prMeter(gui);
		var skip = this.prButton(gui, "skip", 52, false, true, \input, \spark);
		var title = "coco " ++ n;
		var identity, performance, converter, output, signalPath;
		var bitsControl, wrapControl;
		skip.action = { Cocoquantus.skip(n) };
		gui.setSkipButton(n, skip);   // brief flash on any skip (click/code/pattern)
		gui.setMeter(("coco" ++ n).asSymbol, { |inPk, outPk|
			inMeter.value = this.ampMeter(inPk);
			outMeter.value = this.ampMeter(outPk);
		});
		bitsControl = w.intBox(p.value("Bits"));
		wrapControl = this.prLowerButton(
			w.toggle(p.value("Wrap"), "wrap", CQInk.palette(\day)[\fm]), 58);
		identity = VLayout(
			HLayout(nil, this.prEndpoint(gui, d.value(""), \jack, \cas),
				this.prLabel("voice", \cas),
				this.prEndpoint(gui, ("org" ++ n).asSymbol, \jack, \organ),
				this.prLabel("organ", \organ), nil).spacing_(3),
			HLayout(
				VLayout(inMeter.view, this.prLabel("in", \whisper, \center)),
				VLayout(w.knob(p.value("In"), "in", nil),
					this.prEndpoint(gui, d.value("_inVca"), \socket, \line, \wedge)).spacing_(0),
				w.knob(p.value("Speed"), "speed", nil, \featured),
				VLayout(w.knob(p.value("Spaf"), "spaf", CQInk.palette(\day)[\spaf], \featured),
					this.prEndpoint(gui, d.value("_spaf"), \socket, \spaf)).spacing_(0),
				VLayout(w.knob(p.value("Fb"), "fb", nil),
					this.prEndpoint(gui, d.value("_fbVca"), \socket, \line, \wedge)).spacing_(0),
				VLayout(outMeter.view, this.prLabel("out", \whisper, \center))
			),
			// input-mixer slots (SPEC §5.3): what feeds the loop — preamp, deck,
			// quantussy mix, cross-feed from the other coco
			HLayout(
				this.prLabel("slots"),
				*([["SlotPre", "pre"], ["SlotDeck", "deck"],
					["SlotQ", "q"], ["SlotX", "x"]].collect { |pair|
					VLayout(
						this.prLabel(pair[1], \whisper, \center, \range),
						w.hslider(p.value(pair[0]), 40)
					).spacing_(0)
				})
			)
		).spacing_(4);
		performance = HLayout(
			w.toggle(p.value("Hold"), "HOLD", CQInk.palette(\day)[\hold]),
				w.toggle(p.value("Flip"), "flip", CQInk.palette(\day)[\input]),
				this.prEndpoint(gui, d.value("_flip"), \socket, \line, \flip, true),
				skip.view,
				this.prEndpoint(gui, d.value("_skip"), \socket, \line, \spark, true));
		converter = VLayout(
			HLayout(
				w.enumSel(p.value("DolbyIn"), nil, "dolby in"),
				w.enumSel(p.value("DolbyFb"), nil, "dolby fb")),
			HLayout(
				bitsControl,
				wrapControl,
				w.enumSel(p.value("Interp"), nil, "read"))
		).spacing_(4);
		output = HLayout(w.knob(p.value("Dust"), "dust", CQInk.palette(\day)[\cas]),
				w.knob(p.value("Organ"), "organ", CQInk.palette(\day)[\organ]),
				w.knob(p.value("Level"), "level"),
				w.knob(p.value("Pan"), "pan"),
				w.knob(p.value("Send"), "send"));
		signalPath = this.prCocoSignalPath(410 + n);
		^this.prPanel(title, \cas, 110 + n, VLayout(
			View().fixedHeight_(22),
			identity,
			this.prGap(12),
			performance,
			this.prGap(12),
			converter,
			signalPath,
			output
		).spacing_(6))
	}

	*quantussy { |gui|
		var w = gui.widgets, c = CQGuiWidgets.colors;
		var leds, spesalView, rows, shakeKnob, shakeButton, macro, castles;
		// shake amplitude: unbound knob (action argument, not a CQSpec param —
		// see CQGui.shakeAmount); 0..1 knob range = the |amount| range, and
		// CQMatrix.shake clamps again on every throw
		shakeKnob = gui.registerInkControl(CQInkControl.knob("amount", \spaf));
		shakeKnob.value = gui.shakeAmount;
		shakeKnob.display = gui.shakeAmount.round(0.01).asString;
		shakeKnob.action = { |k|
			gui.shakeAmount = k.value;
			k.display = k.value.round(0.01).asString;
		};
		shakeButton = this.prActionButton(gui, "shake", 54, \spaf,
			{ Cocoquantus.shake(gui.shakeAmount) });
		leds = 5.collect {
			this.prLamp(gui)
		};
		gui.castleLeds_(leds);
		// the 15-dot spesal display (SPEC §6.2): 5 castles x 3 bits of state 0..7
		spesalView = CQGuiCastleConstellation(gui).view;
		gui.spesalView_(spesalView);
		rows = 5.collect { |k|
			var step = this.prButton(gui, "step", 42, false, true, \input, \spark);
			step.action = { Cocoquantus.spesal(k + 1) };
			HLayout(
				this.prLabel((k + 1).asString, \tri, \center),
				this.prEndpoint(gui, ("tri" ++ (k + 1)).asSymbol, \jack, \tri),
				this.prEndpoint(gui, ("cas" ++ (k + 1)).asSymbol, \jack, \cas),
				leds[k].view,
				w.enumSel(("c" ++ (k + 1) ++ "Range").asSymbol, c[\cas], "range"),
				this.prEndpoint(gui, ("castle" ++ (k + 1) ++ "_fm").asSymbol,
					\socket, \fm, \wave),
				step.view,
				this.prEndpoint(gui, ("castle" ++ (k + 1) ++ "_spesal").asSymbol,
					\socket, \input, \spark, true),
				w.hslider(("c" ++ (k + 1) ++ "Send").asSymbol)
			)
		};
		macro = View().fixedHeight_(108).background_(c[\ground]).layout_(
			HLayout(w.knob(\qFreq, "q freq", c[\tri], \primary),
				w.knob(\qChaos, "chaos", c[\tri], \primary),
				shakeKnob.view,
				VLayout(nil, shakeButton, nil)).margins_(0)
		);
		castles = View().fixedHeight_(196).background_(c[\ground]).layout_(
			VLayout(*rows).margins_(0).spacing_(4)
		);
		^this.prPanel("quantussy", \tri, 220, VLayout(
			View().fixedHeight_(22),
			macro,
			castles,
			this.prGap(12),
			spesalView
		).spacing_(6))
	}

	*sources { |gui|
		var w = gui.widgets, c = CQGuiWidgets.colors;
		var micLive = false;
		var micRow = UserView().background_(c[\ground]).layout_(
			HLayout(w.knob(\micGain, "mic", c[\warn]),
				w.knob(\liveGain, "live", c[\input])).margins_(2));
		micRow.drawFunc = {
			if(micLive) {
				Pen.strokeColor = c[\danger].copy.alpha_(0.7);
				Pen.width = CQInk.strokes[\normal];
				Pen.addRoundedRect(micRow.bounds.moveTo(0, 0).insetBy(1, 1), 3, 3);
				Pen.stroke;
			};
		};
		gui.widgets.register(\micGain, { |v|
			micLive = v > 0;
			micRow.refresh;
			gui.micNotice(v > 0);
		});
		^this.prPanel("sources", \input, 330, VLayout(
			View().fixedHeight_(22),
			VLayout(
				HLayout(
					this.prLabel("pre a"), this.prEndpoint(gui, \envA, \jack, \input),
					w.enumSel(\preASrc, nil, "source"),
					w.knob(\preADrive, "drive", c[\cas]), w.knob(\preABias, "bias", c[\cas])),
				HLayout(
					this.prLabel("pre b"), this.prEndpoint(gui, \envB, \jack, \input),
					w.enumSel(\preBSrc, nil, "source"),
					w.knob(\preBDrive, "drive", c[\cas]), w.knob(\preBBias, "bias", c[\cas])),
				micRow
			).spacing_(6),
			this.prGap(16),
			this.prDeck(gui, \deckA),
			this.prGap(8),
			this.prDeck(gui, \deckB),
			nil
		).spacing_(6))
	}

	*master { |gui|
		var w = gui.widgets, c = CQGuiWidgets.colors;
		var meterL = this.prMeter(gui, false, false, \recess);
		var meterR = this.prMeter(gui, false, false, \recess);
		var grMeter = this.prMeter(gui, true, true, \recess);
		var notice = this.prLabel("", \whisper, \left, \notice)
			.fixedSize_(Size(310, 20));
		var recBtn = this.prButton(gui, "REC", 58, true, false, \danger, \rec, \recess);
		var recTime = this.prLabel("", \danger, \left, \number).fixedWidth_(48);
		var panicBtn = this.prButton(gui, "PANIC", 82, true, false,
			\danger, \panic, \recess);
		var panel;
		gui.setMeter(\master, { |pkL, pkR|
			meterL.value = this.ampMeter(pkL);
			meterR.value = this.ampMeter(pkR);
		});
		gui.setMeter(\gr, { |db| grMeter.value = (db / 12).clip(0, 1) });
		gui.notice_(notice);
		gui.recButton_(recBtn);
		gui.recTime_(recTime);
		recBtn.view.fixedSize = Size(88, 34);
		panicBtn.view.fixedSize = Size(112, 34);
		recBtn.action = { |b|
			if(b.value == 1) { Cocoquantus.rec } { Cocoquantus.recStop };
		};
		panicBtn.action = {
			if((Cocoquantus.get(\monitorMute) ? 0) > 0)
				{ Cocoquantus.resume } { Cocoquantus.panic };
		};
		gui.widgets.register(\monitorMute, { |v| panicBtn.value = (v ? 0).clip(0, 1) });
		panel = UserView().fixedHeight_(118).background_(c[\recess]);
		panel.drawFunc = {
			CQInk.drawToolHeader(panel.bounds.moveTo(0, 0), "master", gui.currentSkin)
		};
		panel.layout = VLayout(
			View().fixedHeight_(18),
			HLayout(
				VLayout(HLayout(meterL.view, meterR.view).spacing_(2),
					this.prLabel("out", \whisper, \center, \range)).spacing_(0),
				VLayout(grMeter.view,
					this.prLabel("gr", \whisper, \center, \range)).spacing_(0),
				w.knob(\masterVol, "vol", nil, \primary, \recess),
				w.knob(\masterDry, "dry", nil, \standard, \recess),
				w.knob(\spaceMix, "space", nil, \standard, \recess),
				w.knob(\patchDepth, "depth", c[\spaf], \standard, \recess),
				nil,
				VLayout(notice,
					HLayout(recBtn.view, recTime, panicBtn.view, nil).spacing_(8)).spacing_(6)
			).margins_(8).spacing_(8)
		).margins_(0).spacing_(0);
		^panel
	}

	*memory { |gui|
		var c = CQGuiWidgets.colors;
		var nameField = TextField().string_("snapshot").font_(CQInk.font(\number))
			.stringColor_(c[\line]).background_(c[\ground]).fixedSize_(Size(120, 22))
			.action_({ |tf| tf.focus(false) });
		var snapMenus = [PopUpMenu(), PopUpMenu()];
		var durBox = NumberBox().value_(10).clipLo_(0.1).clipHi_(600)
			.font_(CQInk.font(\number)).stringColor_(c[\line]).background_(c[\ground])
			.fixedSize_(Size(48, 22)).action_({ |b| b.focus(false) });
		var refresh, save, recall, morph;
		snapMenus.do { |menu|
			menu.font = CQInk.font(\number);
			menu.stringColor = c[\line];
			menu.background = c[\ground];
			menu.fixedSize = Size(142, 22);
		};
		refresh = {
			var items = (Cocoquantus.snapshots !? { Cocoquantus.snapshots.list }) ? [];
			snapMenus.do { |m| m.items = items };
		};
		save = this.prActionButton(gui, "save", 54, \line, {
			Cocoquantus.snap(nameField.string);
			refresh.value;
		});
		recall = this.prActionButton(gui, "recall", 60, \line, {
			snapMenus[0].item !? { |i| Cocoquantus.recall(i.asSymbol) };
		});
		morph = this.prActionButton(gui, "morph", 58, \spaf, {
			if(snapMenus[0].item.notNil and: { snapMenus[1].item.notNil }) {
				Cocoquantus.morph(snapMenus[0].item.asSymbol,
					snapMenus[1].item.asSymbol, durBox.value);
			};
		});
		refresh.value;
		^View().background_(c[\recess]).layout_(VLayout(
			this.prLabel("memory · snapshots and morph", \whisper),
			HLayout(nameField, save, nil).spacing_(6),
			HLayout(snapMenus[0], this.prLabel("to"), snapMenus[1], nil).spacing_(6),
			HLayout(recall, morph, durBox, this.prLabel("seconds"), nil).spacing_(6),
			nil
		).margins_(8).spacing_(6))
	}

	// -60..0 dBFS onto 0..1 for the LevelIndicators
	*ampMeter { |amp|
		^(((amp ? 0).max(1e-5).ampdb + 60) / 60).clip(0, 1)
	}

	// ---- private ----

	// deck transport row (SPEC §6.2): load/next/prev/random, rate, loop, level;
	// the whole box is the file-drop target, and the label shows the loaded file
	*prDeck { |gui, which|
		var w = gui.widgets, c = CQGuiWidgets.colors;
		var deck = { if(which == \deckA) { Cocoquantus.deckA } { Cocoquantus.deckB } };
		var canDrop, doDrop, bindDrop;
		var fileLabel = StaticText().string_("drop file here")
			.font_(CQInk.font(\number)).align_(\left)
			.stringColor_(c[\whisper]).fixedSize_(Size(118, 14));
		var title = if(which == \deckA) { "deck a" } { "deck b" };
		var row = UserView().fixedHeight_(126).background_(c[\ground]);
		row.drawFunc = { CQInk.drawToolBox(row.bounds.moveTo(0, 0), title, \day) };
		row.layout = VLayout(
			View().fixedHeight_(16),
			HLayout(
				this.prActionButton(gui, "load", 46, \line,
					{ Dialog.openPanel({ |path| deck.value.load(path) }) }),
				this.prActionButton(gui, "prev", 42, \line, { deck.value.prev }),
				this.prActionButton(gui, "next", 42, \line, { deck.value.next }),
				this.prActionButton(gui, "random", 58, \line, { deck.value.random }), nil),
			HLayout(
				VLayout(w.knob((which ++ "Rate").asSymbol, "rate"),
					this.prEndpoint(gui, (which ++ "_rate").asSymbol,
						\socket, \line, \pendulum)).spacing_(0),
				w.knob((which ++ "Level").asSymbol, "level"),
				w.toggle((which ++ "Loop").asSymbol, "loop"),
				VLayout(this.prLabel("file", \whisper, \left, \range), fileLabel).spacing_(0), nil)
		).margins_(6).spacing_(2);
		gui.setDeckLabel(which, fileLabel);
		canDrop = {
			View.currentDrag.isString
				or: { View.currentDrag.isKindOf(SequenceableCollection) }
		};
		doDrop = {
			var drag = View.currentDrag;
			var path = if(drag.isString) { drag } { drag.first.asString };
			deck.value.load(path);
		};
		// a drop lands on the deepest widget under the cursor, and QView's
		// dragEnterEvent always accepts — so a child without handlers swallows
		// the drag and then refuses it (defaultCanReceiveDrag ^false), leaving
		// only the row's margin pixels droppable. Bind every descendant.
		bindDrop = { |view|
			view.canReceiveDragHandler = canDrop;
			view.receiveDragHandler = doDrop;
			view.children.do { |ch| bindDrop.value(ch) };
		};
		bindDrop.value(row);
		^row
	}

	*prLabel { |text, color = \whisper, align = \left, role = \label|
		var palette = CQInk.palette(\day), label;
		label = StaticText().string_(text).font_(CQInk.font(role)).align_(align)
			.stringColor_(palette[color] ?? { palette[\whisper] }).fixedHeight_(12);
		^label
	}

	*prGap { |height = 12| ^View().fixedHeight_(height) }

	*prLowerButton { |view, width = 58|
		var holder = View().fixedSize_(Size(width, 36))
			.background_(CQInk.palette(\day)[\ground]);
		holder.layout = VLayout(this.prGap(10), view, this.prGap(2))
			.margins_(0).spacing_(0);
		^holder
	}

	*prCocoSignalPath { |seed|
		var view = UserView().minHeight_(96).background_(CQInk.palette(\day)[\ground]);
		view.drawFunc = {
			CQInk.drawCocoSignalPath(view.bounds.moveTo(0, 0), \day, seed)
		};
		^view
	}

	*prPanel { |title, family, seed, layout|
		var palette = CQInk.palette(\day), panel;
		panel = UserView().background_(palette[\ground]);
		panel.drawFunc = {
			CQInk.drawOrganFrame(panel.bounds.moveTo(0, 0), title, family, \day, seed)
		};
		panel.layout = layout.margins_(8);
		^panel
	}

	*prEndpoint { |gui, name, kind, family = \line, glyph, lamp = false|
		var palette = CQInk.palette(\day), width = if(lamp) { 34 } { 20 }, endpoint;
		endpoint = UserView().fixedSize_(Size(width, 20)).background_(palette[\ground]);
		endpoint.drawFunc = {
			var center = Point(9, 10);
			if(kind == \jack) {
				CQInk.drawJack(center, family, \day)
			} {
				CQInk.drawSocket(center, family, \day);
				glyph !? {
					CQInk.drawGlyph(glyph, Rect(5, 6, 8, 8), palette[family])
				};
				if(lamp) { CQInk.drawLamp(Point(25, 10), 0, false, \day) };
			};
		};
		// Lamp-bearing gate/trig views are wider than the socket itself.  Keep the
		// cable anchor on the socket glyph at x=9, never midway to its charge lamp.
		^gui.registerEndpoint(name, kind, endpoint, Point(9, 10))
	}

	*prButton { |gui, label, width = 58, toggle = false, momentary = false,
		accent = \line, glyph, surface = \ground|
		var button = CQInkControl.button(label, width, \day, toggle, momentary,
			accent, glyph);
		button.surface = surface;
		gui.registerInkControl(button);
		^button
	}

	*prActionButton { |gui, label, width = 58, accent = \line, action|
		var button = this.prButton(gui, label, width, false, true, accent);
		button.action = action;
		^button.view
	}

	*prMeter { |gui, danger = false, downward = false, surface = \ground|
		var meter = CQInkControl.meter(\day, danger, downward);
		meter.surface = surface;
		gui.registerInkControl(meter);
		^meter
	}

	*prLamp { |gui|
		var lamp = CQInkControl.lamp;
		gui.registerInkControl(lamp);
		^lamp
	}
}
