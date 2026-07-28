// CQGuiWorkshop — the Drawer's fourth page: the bay's quiet measurement table
// (NIGHT_STANDALONE_PLAN §12.1, GUI_SPEC §8). Night is open water, asymmetric
// bodies and gesture; this is the same instrument in the opposite grammar —
// recessed slate, straight rules, exact names and values, stable coordinates
// and no uninvited motion. Same Ink kit, palette, type and hover language; no
// transplanted shells, roots, birds, weather or organism wobble, and no
// shrunken Day panel.
//
// One manifest below drives BOTH the built layout and `paramNames`, so the
// coverage audit can never test a second hand-copied list. The 43 names are
// written out explicitly and are never computed as `CQSpec.names` minus the
// other surfaces: a newly added parameter must fail the gate rather than
// silently count as covered without a drawn control.
//
// The page carries no height constants: every island is a stack of fixed-height
// rows over a trailing stretch, so the rail asks for exactly the height its
// content needs and spare room collects below it as slate. Width and type come
// from CQGuiScale's two reviewed steps.

CQGuiWorkshop {
	var gui, state;
	var <view, <rail, <step, <metrics, skin = \day, pageWidth = 1272;
	var <rows, rowsByParam, <islandRecords, <micLive = false;
	var bandView, mainView, bandLayout, mainLayout, railLayout;
	var topPad, gapPad, bottomPad;

	*new { |gui, state| ^super.newCopyArgs(gui, state).prBuild }

	// every drawn row, by the parameter it writes
	rowFor { |param| ^rowsByParam[param] }
	panels { ^islandRecords.collect { |record| record[\panel] } }
	serviceModules { ^islandRecords.copyRange(0, 3).collect { |r| r[\panel] } }
	mainLedgers { ^islandRecords.copyRange(4, 6).collect { |r| r[\panel] } }
	railWidth { ^CQGuiScale.railWidth(step, pageWidth) }

	// ---- the composition (pure, testable without Qt) ----

	// One island's vertical score: a title, then each subgroup's label and its
	// rows, each item carrying the y it starts at. Both the Qt layout and the
	// island's own drawFunc consume THIS list, so a painted subgroup label and
	// the rows under it can never drift apart, and both Coco ledgers share their
	// subgroup baselines by construction (§12.1.3).
	*islandLayout { |island, step = \compact|
		var m = CQGuiScale.metrics(step), items = List.new, y = 0;
		items.add((kind: \title, y: y, height: m[\titleHeight],
			label: island[\title], family: island[\family] ? \line));
		y = y + m[\titleHeight];
		this.islandGroups(island).do { |group|
			group[\label] !? { |text|
				items.add((kind: \group, y: y, height: m[\groupHeight],
					label: text));
				y = y + m[\groupHeight];
			};
			(group[\rows] ? []).do { |row|
				items.add((kind: \row, y: y, height: m[\rowHeight], row: row));
				y = y + m[\rowHeight];
			};
		};
		^items.asArray
	}

	*islandGroups { |island|
		^island[\groups] ?? { [(label: nil, rows: island[\rows] ? [])] }
	}

	*islandHeight { |island, step = \compact|
		var items = this.islandLayout(island, step), last = items.last;
		^last[\y] + last[\height]
	}

	// the four service modules and the three main ledgers, both summing to the
	// rail exactly (CQGuiScale.columns gives an odd count's remainder to the
	// middle, so the two Coco ledgers stay literally the same width)
	*serviceWidths { |railWidth, step = \compact|
		^CQGuiScale.columns(railWidth, 4, CQGuiScale.metrics(step)[\gutter])
	}

	*mainWidths { |railWidth, step = \compact|
		^CQGuiScale.columns(railWidth, 3, CQGuiScale.metrics(step)[\gutter])
	}

	// ---- the manifest (§12.1.1's exact 43) ----

	// The service band's four modules, in the wireframe's order. Space stays a
	// neutral service module and grows no invented Night body; its `mix` lives
	// on the persistent Master strip and is deliberately not repeated here.
	*serviceIslands {
		^[
			(title: "deck a", family: \line, rows: [
				(param: \deckARate, label: "rate"),
				(param: \deckALoop, label: "loop")]),
			(title: "input trims", family: \line, rows: [
				(param: \liveGain, label: "live gain", family: \input),
				// the one deliberate red exception on the page (SPEC §7)
				(param: \micGain, label: "mic gain", family: \danger)]),
			(title: "space", family: \line, rows: [
				(param: \spaceSize, label: "size"),
				(param: \spaceDamp, label: "damping")]),
			(title: "deck b", family: \line, rows: [
				(param: \deckBRate, label: "rate"),
				(param: \deckBLoop, label: "loop")])
		]
	}

	// One helper builds both Coco ledgers with only the index changed, so the
	// pair's symmetry is structural rather than transcribed. Rows are grouped by
	// signal meaning, in the normative converter order (SPEC §5.3), never
	// alphabetically.
	*cocoIsland { |n|
		var p = { |suffix| ("coco" ++ n ++ suffix).asSymbol };
		^(title: "coco " ++ n, family: \cas, groups: [
			(label: "converter", rows: [
				(param: p.value("Bits"), label: "bits"),
				(param: p.value("Wrap"), label: "wrap"),
				(param: p.value("Interp"), label: "read")]),
			(label: "material", rows: [
				(param: p.value("Dust"), label: "dust", family: \cas),
				(param: p.value("Organ"), label: "organ", family: \organ)]),
			(label: "output", rows: [
				(param: p.value("Level"), label: "level"),
				(param: p.value("Pan"), label: "pan"),
				(param: p.value("Send"), label: "send")]),
			// the input mixer's four slots (SPEC §5.3). These labels are short
			// only because their group already names them.
			(label: "input mix", rows: [
				(param: p.value("SlotPre"), label: "pre"),
				(param: p.value("SlotDeck"), label: "deck"),
				(param: p.value("SlotQ"), label: "q"),
				(param: p.value("SlotX"), label: "x")])
		])
	}

	// Castle numbers run 1→5 in both groups so the two lists can be compared
	// without hunting (§12.1.3).
	*quantussyIsland {
		^(title: "quantussy", family: \tri, groups: [
			(label: "s&h", rows: [(param: \qShLag, label: "slew")]),
			(label: "detune", rows: (1..5).collect { |k|
				(param: ("c" ++ k ++ "Off").asSymbol, label: "castle " ++ k)
			}),
			(label: "audio sends", rows: (1..5).collect { |k|
				(param: ("c" ++ k ++ "Send").asSymbol, label: "castle " ++ k)
			})
		])
	}

	*mainIslands { ^[this.cocoIsland(1), this.quantussyIsland, this.cocoIsland(2)] }

	*islands { ^this.serviceIslands ++ this.mainIslands }

	*islandRows { |island|
		^this.islandGroups(island).inject([]) { |all, group|
			all ++ (group[\rows] ? [])
		}
	}

	// the audited inventory — exactly the rows this page draws
	*paramNames { ^this.islands.inject([]) { |all, island|
		all ++ this.islandRows(island).collect { |row| row[\param] }
	} }

	// ---- lifecycle ----

	free { ^this }

	// The rows themselves re-ink with every other Ink control through CQGui's
	// own pass; only the page's slate and the islands' painted titles are ours.
	applySkin { |next|
		var palette;
		skin = next;
		palette = CQInk.palette(skin);
		view.background = palette[\recess];
		rail !? { rail.background = palette[\recess] };
		this.panels.do { |panel|
			panel.background = palette[\recess];
			panel.refresh;
		};
		^this
	}

	// The Drawer's size step moved (CQGuiScale). Nothing is rebuilt: the rows
	// keep their values, actions and updater registrations, and only their
	// measurements change — so a maximize can never re-register a funnel
	// binding or resurrect a stale control.
	applyScale { |nextStep, width|
		var railW, serviceWidths, mainWidths;
		nextStep = nextStep ? step;
		width = width ? pageWidth;
		if(nextStep == step and: { width == pageWidth }) { ^this };
		step = nextStep;
		pageWidth = width;
		metrics = CQGuiScale.metrics(step);
		railW = CQGuiScale.railWidth(step, pageWidth);
		serviceWidths = this.class.serviceWidths(railW, step);
		mainWidths = this.class.mainWidths(railW, step);
		rail.fixedWidth = railW;
		topPad.fixedHeight = metrics[\vMargin];
		bottomPad.fixedHeight = metrics[\vMargin];
		gapPad.fixedHeight = metrics[\bandGap];
		bandView.fixedHeight = CQGuiScale.serviceBandHeight(step);
		bandLayout.spacing = metrics[\gutter];
		mainLayout.spacing = metrics[\gutter];
		islandRecords.do { |record, i|
			this.prScaleIsland(record,
				if(i < 4) { serviceWidths[i] } { mainWidths[i - 4] });
		};
		^this
	}

	// ---- private ----

	prBuild {
		var palette = CQInk.palette(\day), railW, serviceWidths, mainWidths;
		step = \compact;
		metrics = CQGuiScale.metrics(step);
		railW = CQGuiScale.railWidth(step, pageWidth);
		serviceWidths = this.class.serviceWidths(railW, step);
		mainWidths = this.class.mainWidths(railW, step);
		rows = List.new;
		rowsByParam = IdentityDictionary.new;
		islandRecords = this.class.serviceIslands.collect { |island, i|
			this.prIsland(island, serviceWidths[i])
		} ++ this.class.mainIslands.collect { |island, i|
			this.prIsland(island, mainWidths[i])
		};
		bandLayout = HLayout(*this.serviceModules).margins_(0)
			.spacing_(metrics[\gutter]);
		bandView = View().fixedHeight_(CQGuiScale.serviceBandHeight(step))
			.layout_(bandLayout);
		mainLayout = HLayout(*this.mainLedgers).margins_(0)
			.spacing_(metrics[\gutter]);
		mainView = View().layout_(mainLayout);
		gapPad = View().fixedHeight_(metrics[\bandGap]);
		bottomPad = View().fixedHeight_(metrics[\vMargin]);
		// no height constant anywhere: the rail asks for exactly what its
		// islands need, and the page's trailing stretch turns the rest into slate
		railLayout = VLayout(bandView, gapPad, mainView, bottomPad)
			.margins_(0).spacing_(0);
		rail = View().fixedWidth_(railW).background_(palette[\recess])
			.layout_(railLayout);
		// the shared page frame: every Drawer page opens with the same air
		// under the tab row and centres the same rail (CQGuiScale.pageFrame)
		topPad = View().fixedHeight_(metrics[\vMargin]);
		view = View().background_(palette[\recess]);
		view.layout = VLayout(
			topPad,
			HLayout(nil, rail, nil).margins_(0).spacing_(0),
			nil
		).margins_(0).spacing_(0);
		^this
	}

	prIsland { |island, width|
		var palette = CQInk.palette(\day);
		var record = (island: island, items: this.class.islandLayout(island, step));
		var panel = UserView().fixedWidth_(width).background_(palette[\recess]);
		var children = List.new, stack = List.new;
		panel.drawFunc = {
			try {
				CQInk.drawLedgerIsland(panel.bounds.moveTo(0, 0),
					record[\items], skin, metrics, island[\family] ? \line)
			} { |error| CQInkControl.noteDrawError(error) };
		};
		record[\items].do { |item|
			if(item[\kind] == \row) {
				var control = this.prRow(item[\row], width);
				children.add(control);
				stack.add(control.view);
			} {
				var spacer = View().fixedHeight_(item[\height]);
				children.add(spacer);
				stack.add(spacer);
			};
		};
		// the island's own bottom air; the rows never stretch to fill it
		stack.add(nil);
		panel.layout = VLayout(*stack.asArray).margins_(0).spacing_(0);
		record[\panel] = panel;
		record[\children] = children.asArray;
		^record
	}

	prScaleIsland { |record, width|
		var items = this.class.islandLayout(record[\island], step);
		record[\items] = items;
		record[\panel].fixedWidth = width;
		items.do { |item, i|
			var child = record[\children][i];
			if(item[\kind] == \row) {
				child.scale_(metrics, width);
			} {
				child.fixedHeight = item[\height];
			};
		};
		record[\panel].refresh;
		^this
	}

	prRow { |row, width|
		var control = gui.widgets.ledger(row[\param], row[\label],
			row[\family], width, \recess, metrics);
		rows.add(control);
		rowsByParam[row[\param]] = control;
		// SPEC §7 / §12.1.3: the one interactive mic path in the whole GUI. Its
		// warning subscription lives here for as long as the GUI does — the page
		// is built at GUI open, so closing the Drawer cannot silence the notice.
		if(row[\param] == \micGain) {
			gui.widgets.register(\micGain, { |value|
				micLive = (value ? 0) > 0;
				control.danger = micLive;
				gui.micNotice(micLive);
			});
		};
		^control
	}
}
