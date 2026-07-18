// CQGuiPanels — the panel columns of CQGui (SPEC §6.2): [Sources+Preamps]
// [Coco1] [Quantussy] [Coco2] + master strip. Pure view builders: continuous
// controls come from the CQGuiWidgets factory (funnel-bound), discrete actions
// call the Cocoquantus facade (which itself only writes through the funnels).
// Meters/LEDs register setter functions with CQGui; its OSCdefs drive them.

CQGuiPanels {

	*coco { |gui, n|
		var w = gui.widgets, c = CQGuiWidgets.colors;
		var p = { |suffix| ("coco" ++ n ++ suffix).asSymbol };
		var inMeter = LevelIndicator().fixedSize_(Size(10, 68));
		var outMeter = LevelIndicator().fixedSize_(Size(10, 68));
		var skip = Button().states_([["SKIP"]])
			.action_({ Cocoquantus.skip(n) });
		gui.setMeter(("coco" ++ n).asSymbol, { |inPk, outPk|
			inMeter.value = this.ampMeter(inPk);
			outMeter.value = this.ampMeter(outPk);
		});
		^View().layout_(VLayout(
			StaticText().string_("COCO " ++ n).align_(\center)
				.font_(Font.sansSerif(12, true)),
			HLayout(
				VLayout(inMeter, StaticText().string_("in").font_(Font.sansSerif(8))),
				w.knob(p.value("In"), "in", c[\input]),
				w.knob(p.value("Speed"), "speed", c[\cas]),
				w.knob(p.value("Spaf"), "^ spaf", c[\spaf]),
				w.knob(p.value("Fb"), "fb", c[\input]),
				VLayout(outMeter, StaticText().string_("out").font_(Font.sansSerif(8)))
			),
			HLayout(w.toggle(p.value("Hold"), "HOLD", c[\hold]),
				w.toggle(p.value("Flip"), "FLIP", c[\input]), skip),
			HLayout(
				StaticText().string_("dolby in/fb").font_(Font.sansSerif(9)),
				w.enumSel(p.value("DolbyIn")), w.enumSel(p.value("DolbyFb"))),
			HLayout(
				StaticText().string_("bits").font_(Font.sansSerif(9)),
				w.intBox(p.value("Bits")),
				w.toggle(p.value("Wrap"), "wrap", c[\fm]),
				w.enumSel(p.value("Interp"))),
			HLayout(w.knob(p.value("Dust"), "dust", c[\cas]),
				w.knob(p.value("Organ"), "organ", c[\organ]),
				w.knob(p.value("Level"), "level"),
				w.knob(p.value("Pan"), "pan"),
				w.knob(p.value("Send"), "send")),
			nil
		))
	}

	*quantussy { |gui|
		var w = gui.widgets, c = CQGuiWidgets.colors;
		var leds, spesalView, rows;
		leds = 5.collect {
			View().fixedSize_(Size(12, 12)).background_(Color.gray(0.8))
		};
		gui.castleLeds_(leds);
		// the 15-dot spesal display (SPEC §6.2): 5 castles x 3 bits of state 0..7
		spesalView = UserView().fixedSize_(Size(64, 62)).drawFunc_({
			gui.spesalStates.do { |st, i|
				3.do { |bit|
					Pen.fillColor = if(st.asInteger.bitTest(2 - bit)) {
						c[\input] } { Color.gray(0.85) };
					Pen.fillOval(Rect(bit * 14 + 2, i * 12 + 2, 9, 9));
				};
				Pen.stringAtPoint(st.asString, 46@(i * 12 + 1),
					Font.sansSerif(8), Color.gray(0.3));
			};
		});
		gui.spesalView_(spesalView);
		rows = 5.collect { |k|
			HLayout(
				StaticText().string_("C" ++ (k + 1)).font_(Font.sansSerif(9, true))
					.stringColor_(c[\tri]),
				leds[k],
				w.enumSel(("c" ++ (k + 1) ++ "Range").asSymbol, c[\cas]),
				Button().states_([["S", Color.white, c[\input]]])
					.fixedSize_(Size(18, 18))
					.action_({ Cocoquantus.spesal(k + 1) }),
				w.hslider(("c" ++ (k + 1) ++ "Send").asSymbol)
			)
		};
		^View().layout_(VLayout(
			StaticText().string_("QUANTUSSY").align_(\center)
				.font_(Font.sansSerif(12, true)),
			HLayout(w.knob(\qFreq, "qFreq", c[\tri], 44),
				w.knob(\qChaos, "qChaos", c[\tri], 44)),
			*(rows ++ [
				HLayout(
					StaticText().string_("spesal").font_(Font.sansSerif(9))
						.stringColor_(c[\input]),
					spesalView,
					Button().states_([["SHAKE", Color.white, c[\spaf]]])
						.action_({ Cocoquantus.shake })),
				nil
			])
		))
	}

	*sources { |gui|
		var w = gui.widgets, c = CQGuiWidgets.colors;
		var micRow = View().layout_(
			HLayout(w.knob(\micGain, "mic", c[\warn]),
				w.knob(\liveGain, "live", c[\input])).margins_(2));
		gui.widgets.register(\micGain, { |v|
			micRow.background = if(v > 0) { c[\warn].alpha_(0.35) } { nil };
			gui.micNotice(v > 0);
		});
		^View().layout_(VLayout(
			StaticText().string_("SOURCES").align_(\center)
				.font_(Font.sansSerif(12, true)),
			HLayout(
				StaticText().string_("pre A").font_(Font.sansSerif(9, true)),
				w.enumSel(\preASrc),
				w.knob(\preADrive, "drive", c[\cas]),
				w.knob(\preABias, "bias", c[\cas])),
			HLayout(
				StaticText().string_("pre B").font_(Font.sansSerif(9, true)),
				w.enumSel(\preBSrc),
				w.knob(\preBDrive, "drive", c[\cas]),
				w.knob(\preBBias, "bias", c[\cas])),
			micRow,
			this.prDeck(gui, \deckA), this.prDeck(gui, \deckB),
			nil
		))
	}

	*master { |gui|
		var w = gui.widgets, c = CQGuiWidgets.colors;
		var meterL = LevelIndicator().fixedSize_(Size(10, 68));
		var meterR = LevelIndicator().fixedSize_(Size(10, 68));
		var grMeter = LevelIndicator().fixedSize_(Size(10, 68));
		var notice = StaticText().font_(Font.sansSerif(9))
			.stringColor_(c[\warn]).align_(\center);
		var recBtn = Button().states_([
			["REC", Color.white, Color.gray(0.35)],
			["REC", Color.white, c[\warn]]]);
		var recTime = StaticText().font_(Font.sansSerif(9, true))
			.stringColor_(c[\warn]).fixedWidth_(34);
		var panicBtn = Button().states_([
			["PANIC", Color.white, Color.gray(0.35)],
			["MUTED — resume", Color.white, c[\warn]]]);
		// text entry holds focus while typing; Enter returns it to the key map
		var nameField = TextField().string_("snap")
			.action_({ |tf| tf.focus(false) });
		var snapMenus = [PopUpMenu(), PopUpMenu()];
		var durBox = NumberBox().value_(10).clipLo_(0.1).clipHi_(600)
			.fixedWidth_(40).action_({ |b| b.focus(false) });
		var refresh = {
			var items = (Cocoquantus.snapshots !? { Cocoquantus.snapshots.list }) ? [];
			snapMenus.do { |m| m.items = items };
		};
		gui.setMeter(\master, { |pkL, pkR|
			meterL.value = this.ampMeter(pkL);
			meterR.value = this.ampMeter(pkR);
		});
		gui.setMeter(\gr, { |db| grMeter.value = (db / 12).clip(0, 1) });
		gui.notice_(notice);
		gui.recButton_(recBtn);
		gui.recTime_(recTime);
		recBtn.action = { |b|
			if(b.value == 1) { Cocoquantus.rec } { Cocoquantus.recStop };
		};
		panicBtn.action = {
			if((Cocoquantus.get(\monitorMute) ? 0) > 0)
				{ Cocoquantus.resume } { Cocoquantus.panic };
		};
		gui.widgets.register(\monitorMute, { |v| panicBtn.value = (v ? 0).clip(0, 1) });
		refresh.value;
		^View().layout_(VLayout(
			StaticText().string_("MASTER").align_(\center)
				.font_(Font.sansSerif(12, true)),
			HLayout(
				VLayout(HLayout(meterL, meterR),
					StaticText().string_("out").font_(Font.sansSerif(8)).align_(\center)),
				VLayout(grMeter,
					StaticText().string_("GR").font_(Font.sansSerif(8)).align_(\center)),
				w.knob(\masterVol, "vol", nil, 44),
				w.knob(\masterDry, "dry"),
				w.knob(\spaceMix, "space")),
			notice,
			HLayout(recBtn, recTime, panicBtn),
			HLayout(nameField,
				Button().states_([["SNAP"]]).action_({
					Cocoquantus.snap(nameField.string);
					refresh.value;
				})),
			HLayout(snapMenus[0], snapMenus[1],
				Button().states_([["↻"]]).fixedWidth_(24).action_(refresh)),
			HLayout(
				Button().states_([["RECALL"]]).action_({
					snapMenus[0].item !? { |i| Cocoquantus.recall(i.asSymbol) };
				}),
				Button().states_([["MORPH →"]]).action_({
					if(snapMenus[0].item.notNil and: { snapMenus[1].item.notNil }) {
						Cocoquantus.morph(snapMenus[0].item.asSymbol,
							snapMenus[1].item.asSymbol, durBox.value);
					};
				}),
				durBox, StaticText().string_("s").font_(Font.sansSerif(9))),
			nil
		))
	}

	// -60..0 dBFS onto 0..1 for the LevelIndicators
	*ampMeter { |amp|
		^(((amp ? 0).max(1e-5).ampdb + 60) / 60).clip(0, 1)
	}

	// ---- private ----

	// deck transport row (SPEC §6.2): load/next/prev/random, rate, loop, level;
	// the whole box is the file-drop target, and the label shows the loaded file
	*prDeck { |gui, which|
		var w = gui.widgets;
		var deck = { if(which == \deckA) { Cocoquantus.deckA } { Cocoquantus.deckB } };
		var canDrop, doDrop, bindDrop;
		var fileLabel = StaticText().string_("drop file here")
			.font_(Font.sansSerif(8)).align_(\center)
			.stringColor_(Color.gray(0.5)).fixedWidth_(86);
		var row = View().background_(Color.gray(0.9)).layout_(VLayout(
			HLayout(
				StaticText().string_(which.asString.toUpper)
					.font_(Font.sansSerif(9, true)),
				Button().states_([["load"]]).action_({
					Dialog.openPanel({ |path| deck.value.load(path) });
				}),
				Button().states_([["<"]]).fixedWidth_(22).action_({ deck.value.prev }),
				Button().states_([[">"]]).fixedWidth_(22).action_({ deck.value.next }),
				Button().states_([["?"]]).fixedWidth_(22).action_({ deck.value.random })),
			HLayout(
				w.knob((which ++ "Rate").asSymbol, "rate"),
				w.knob((which ++ "Level").asSymbol, "level"),
				w.toggle((which ++ "Loop").asSymbol, "loop"),
				fileLabel)
		).margins_(2).spacing_(1));
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
}
