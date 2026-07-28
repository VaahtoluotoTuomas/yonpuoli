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

CQGuiWorkshop {
	// §12.1.3's fixed composition. The rail matches the accepted Inspector loom
	// exactly; larger windows buy surrounding slate, never stretched controls.
	classvar railW = 1128, serviceW = 270, gutterW = 16, serviceH = 72;
	classvar mainH = 392, pageMargin = 12, bandGap = 16;
	classvar titleH = 20, groupLabelH = 16, rowH = 24;

	var gui, state;
	var <view, <rail, skin = \day;
	var <rows, rowsByParam, <serviceModules, <mainLedgers, <micLive = false;

	*new { |gui, state| ^super.newCopyArgs(gui, state).prBuild }

	// every drawn row, by the parameter it writes
	rowFor { |param| ^rowsByParam[param] }
	panels { ^(serviceModules ++ mainLedgers) }

	// ---- the fixed composition (pure, testable without Qt) ----

	*railWidth { ^railW }
	*serviceWidth { ^serviceW }
	*serviceHeight { ^serviceH }
	*gutter { ^gutterW }
	*pageMargin { ^pageMargin }
	*bandGap { ^bandGap }
	*rowHeight { ^rowH }
	*titleHeight { ^titleH }
	*groupLabelHeight { ^groupLabelH }
	*mainHeight { ^mainH }

	// 365 · 366 · 365 with two 16 px gutters — the odd middle pixel goes to the
	// Quantussy so the two Coco ledgers stay literally identical (§12.1.3's pair
	// symmetry: the same helper, only the index changed).
	*mainWidths { ^[365, 366, 365] }

	// service band: four equal modules, three gutters
	*serviceSpan { ^(4 * serviceW) + (3 * gutterW) }
	*mainSpan { ^this.mainWidths.sum + (2 * gutterW) }

	// 12 + 72 + 16 + 392 + 12 — the whole page inside the minimum Drawer
	// workspace with no scroll view (§12.1.3)
	*railHeight { ^(2 * pageMargin) + serviceH + bandGap + mainH }

	// One island's vertical score: a title, then each subgroup's whisper and its
	// rows, each item carrying the y it starts at. Both the Qt layout and the
	// island's own drawFunc consume THIS list, so a painted subgroup label and
	// the rows under it can never drift apart, and both Coco ledgers share their
	// subgroup baselines by construction (§12.1.3).
	*islandLayout { |island|
		var items = List.new, y = 0;
		items.add((kind: \title, y: y, height: titleH, label: island[\title],
			family: island[\family] ? \line));
		y = y + titleH;
		this.islandGroups(island).do { |group|
			group[\label] !? { |text|
				items.add((kind: \group, y: y, height: groupLabelH, label: text));
				y = y + groupLabelH;
			};
			(group[\rows] ? []).do { |row|
				items.add((kind: \row, y: y, height: rowH, row: row));
				y = y + rowH;
			};
		};
		^items.asArray
	}

	*islandGroups { |island|
		^island[\groups] ?? { [(label: nil, rows: island[\rows] ? [])] }
	}

	// the tallest island's own ink: a title, four subgroup labels and twelve
	// rows must fit inside `mainHeight` before any bottom air
	*islandHeight { |island|
		var items = this.islandLayout(island), last = items.last;
		^last[\y] + last[\height]
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

	// ---- private ----

	prBuild {
		var palette = CQInk.palette(\day), band, main;
		rows = List.new;
		rowsByParam = IdentityDictionary.new;
		serviceModules = this.class.serviceIslands.collect { |island|
			this.prIsland(island, serviceW, serviceH)
		};
		mainLedgers = this.class.mainIslands.collect { |island, i|
			this.prIsland(island, this.class.mainWidths[i], mainH)
		};
		band = View().fixedHeight_(serviceH).layout_(
			HLayout(*serviceModules).margins_(0).spacing_(gutterW));
		main = View().fixedHeight_(mainH).layout_(
			HLayout(*mainLedgers).margins_(0).spacing_(gutterW));
		rail = View().fixedSize_(Size(railW, this.class.railHeight))
			.background_(palette[\recess]);
		rail.layout = VLayout(
			View().fixedHeight_(pageMargin),
			band,
			View().fixedHeight_(bandGap),
			main,
			View().fixedHeight_(pageMargin)
		).margins_(0).spacing_(0);
		// The rail is fixed: a larger window buys surrounding slate, never
		// stretched controls, and spare height collects below as slate rather
		// than pulling the rows apart (§12.1.3).
		view = View().background_(palette[\recess]);
		view.layout = VLayout(
			HLayout(nil, rail, nil).margins_(0).spacing_(0),
			nil
		).margins_(0).spacing_(0);
		^this
	}

	prIsland { |island, width, height|
		var palette = CQInk.palette(\day);
		var items = this.class.islandLayout(island);
		var panel = UserView().fixedSize_(Size(width, height))
			.background_(palette[\recess]);
		var stack = List.new;
		panel.drawFunc = {
			try {
				CQInk.drawLedgerIsland(panel.bounds.moveTo(0, 0), items, skin)
			} { |error| CQInkControl.noteDrawError(error) };
		};
		items.do { |item|
			if(item[\kind] == \row) {
				stack.add(this.prRow(item[\row], width).view);
			} {
				stack.add(View().fixedHeight_(item[\height]));
			};
		};
		// the island's own bottom air; the rows never stretch to fill it
		stack.add(nil);
		panel.layout = VLayout(*stack.asArray).margins_(0).spacing_(0);
		^panel
	}

	prRow { |row, width|
		var control = gui.widgets.ledger(row[\param], row[\label],
			row[\family], width);
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
