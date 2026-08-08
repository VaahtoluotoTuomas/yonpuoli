// The Drawer Workshop's own contracts (NIGHT_STANDALONE_PLAN §12.1.3/§12.1.5):
// the ledger row's geometry, its exact-value reading, and the funnel bindings
// every one of the 24 rows is built with. Serverless and Qt-free — the rendered
// page is asserted by the Drawer smoke.

CQGuiWorkshopTest : UnitTest {
	var state, widgets, updaters, owner;

	setUp {
		state = CQState(nil);
		updaters = IdentityDictionary.new;
		owner = CQTestInkOwner.new;
		widgets = CQGuiWidgets(state, updaters, owner);
	}

	prRow { |name, label, family, width = 200|
		^widgets.ledger(name, label ? name.asString, family, width)
	}

	// ---- the composition ----

	test_theRailIsTheInspectorLoomsRailAtEveryStep {
		CQGuiScale.steps.do { |step|
			var page = if(step == \roomy) { 1434 } { 1272 };
			var rail = CQGuiScale.railWidth(step, page);
			var service = CQGuiWorkshop.serviceWidths(rail, step);
			var main = CQGuiWorkshop.mainWidths(rail, step);
			this.assertEquals(rail, CQGuiMatrix.loomSize(step, page).width,
				"% : one rail serves the loom and the ledger".format(step));
			this.assertEquals(service.size, 3, "% : three service modules".format(step));
			this.assertEquals(main.size, 3, "% : three main ledgers".format(step));
			this.assert(service.maxItem - service.minItem <= 1,
				"% : the service modules are equal to the pixel".format(step));
			this.assertEquals(main[0], main[2],
				"% : the Coco ledgers are the same width".format(step));
		};
		this.assertEquals(CQGuiScale.railWidth(\compact, 1272), 1200,
			"the 32-column windowed loom keeps more than its 24 px margin");
		this.assert(CQGuiScale.railWidth(\roomy, 1434)
			> CQGuiScale.railWidth(\compact, 1272),
			"a maximized window buys a wider rail, not only more slate");
	}

	// One score drives both the Qt layout and the painted labels, so a subgroup
	// label can never sit above the wrong rows.
	test_oneIslandScoreDrivesLayoutAndInk {
		var coco = CQGuiWorkshop.cocoIsland(1);
		var items = CQGuiWorkshop.islandLayout(coco);
		var rowItems = items.select { |item| item[\kind] == \row };
		var groups = items.select { |item| item[\kind] == \group };
		var y = 0;
		this.assertEquals(items.first[\kind], \title, "an island opens with its title");
		this.assertEquals(rowItems.size, 6, "coco 1 lays out its six setup rows");
		this.assertEquals(groups.collect { |item| item[\label] },
			["converter", "output", "input mix"],
			"rows are grouped by signal meaning, in the normative converter order");
		items.do { |item|
			this.assertEquals(item[\y], y,
				"item % starts exactly where the previous one ended".format(item[\kind]));
			y = y + item[\height];
		};
		this.assertEquals(y, CQGuiWorkshop.islandHeight(coco),
			"the score's end IS the island's height");
		this.assertEquals(CQGuiWorkshop.islandHeight(coco),
			CQGuiScale.tallestIslandHeight(\compact),
			"a six-parameter Coco is still the page's tallest island");
		this.assert(CQGuiWorkshop.islandHeight(coco, \roomy)
			> CQGuiWorkshop.islandHeight(coco, \compact),
			"the roomy step gives the same six rows more room");
	}

	test_bothCocoLedgersComeFromOneHelper {
		var a = CQGuiWorkshop.islandLayout(CQGuiWorkshop.cocoIsland(1));
		var b = CQGuiWorkshop.islandLayout(CQGuiWorkshop.cocoIsland(2));
		this.assertEquals(a.collect { |item| [item[\kind], item[\y], item[\height]] },
			b.collect { |item| [item[\kind], item[\y], item[\height]] },
			"the pair's rows and subgroup baselines match exactly");
		this.assertEquals(a.select { |i| i[\kind] == \row }
			.collect { |i| i[\row][\label] },
			b.select { |i| i[\kind] == \row }.collect { |i| i[\row][\label] },
			"only the index differs between the two Coco ledgers");
	}

	test_castleDetunesStayInWorkshopWhileSendsMovedToNight {
		var groups = CQGuiWorkshop.islandGroups(CQGuiWorkshop.quantussyIsland);
		var detune = groups.detect { |g| g[\label] == "detune" };
		var sends = groups.detect { |g| g[\label] == "audio sends" };
		this.assertEquals(detune[\rows].collect { |row| row[\param] },
			[\c1Off, \c2Off, \c3Off, \c4Off, \c5Off],
			"detunes run castle 1 to 5");
		this.assert(sends.isNil,
			"audio sends have one owner: the castles' radial Night gestures");
	}

	// §12.1.3: semantic phosphor sparingly. Neutral slots, levels and pan
	// get no family at all.
	test_semanticColourIsUsedSparingly {
		var accented = CQGuiWorkshop.islands.inject([]) { |all, island|
			all ++ CQGuiWorkshop.islandRows(island).select { |row|
				row[\family].notNil
			}
		};
		this.assertEquals(accented.collect { |row| row[\param] }.asArray.copy
			.sort { |a, b| a.asString <= b.asString },
			[\liveGain, \micGain],
			"only the live input and the mic carry a family accent here");
		accented.do { |row|
			this.assert(CQInk.palette(\day)[row[\family]].notNil,
				"% names a real palette family".format(row[\param]));
		};
		this.assert(CQGuiWorkshop.islandRows(CQGuiWorkshop.quantussyIsland)
			.every { |row| row[\family].isNil },
			"the Quantussy ledger does not rainbow its own column");
	}

	// ---- the ledger row's geometry ----

	// §4.5: cue and action share one geometry, so the rail the row draws is the
	// rail the press is measured against.
	test_theRailIsOneGeometryForCueAndAction {
		var rect = Rect(0, 0, 365, 24);
		var rail = CQInk.ledgerRailRect(rect);
		var m = CQInk.ledgerMetrics(nil);
		this.assertEquals(rail.left, m[\inset] + m[\labelWidth],
			"the rail begins after the label column");
		this.assertEquals(rail.right,
			365 - m[\inset] - m[\valueWidth] - m[\gap],
			"the rail stops before the value column, so the two never overlap");
		this.assertFloatEquals(CQInk.ledgerNormAt(rect, rail.left), 0,
			"the rail's left end is 0", 1e-9);
		this.assertFloatEquals(CQInk.ledgerNormAt(rect, rail.right), 1,
			"the rail's right end is 1", 1e-9);
		this.assertFloatEquals(CQInk.ledgerNormAt(rect, rail.center.x), 0.5,
			"its centre is the middle of the range", 1e-9);
		this.assertFloatEquals(CQInk.ledgerNormAt(rect, rail.left - 40), 0,
			"a press past an end clamps rather than wrapping", 1e-9);
	}

	test_aPressOffTheRailIsInert {
		var rect = Rect(0, 0, 365, 24);
		var rail = CQInk.ledgerRailRect(rect);
		this.assert(CQInk.ledgerHitsRail(rect, rail.center.x),
			"the rail itself answers");
		this.assert(CQInk.ledgerHitsRail(rect,
			rail.left - CQInk.ledgerMetrics(nil)[\grab]),
			"a small grab band makes the rail's own ends reachable");
		this.assert(CQInk.ledgerHitsRail(rect, 4).not,
			"a mis-aimed click on the label writes nothing");
		this.assert(CQInk.ledgerHitsRail(rect, 362).not,
			"nor does one on the value column");
	}

	// the rail follows its step, so a press measured at one size can never be
	// answered by geometry from the other
	test_theRailGrowsWithTheStep {
		var rect = Rect(0, 0, 520, 30);
		var compact = CQInk.ledgerRailRect(rect, CQGuiScale.metrics(\compact));
		var roomy = CQInk.ledgerRailRect(rect, CQGuiScale.metrics(\roomy));
		this.assert(roomy.left > compact.left,
			"the roomy label column is wider for its bigger type");
		this.assert(roomy.right < compact.right,
			"and so is its value column");
		this.assertFloatEquals(
			CQInk.ledgerNormAt(rect, roomy.right, CQGuiScale.metrics(\roomy)), 1,
			"the roomy rail's own right end is still exactly 1", 1e-9);
		this.assertEquals(CQInk.ledgerMetrics(CQGuiScale.metrics(\roomy))[\height],
			CQGuiScale.metrics(\roomy)[\rowHeight],
			"a row is exactly its step's row height");
	}

	test_seatsAreStableAndEvenlySpaced {
		var rect = Rect(0, 0, 270, 24);
		var rail = CQInk.ledgerRailRect(rect);
		this.assertEquals(CQInk.ledgerSeatAt(rect, rail.left + 1, 2), 0,
			"the left half of the rail is the first seat");
		this.assertEquals(CQInk.ledgerSeatAt(rect, rail.right - 1, 2), 1,
			"the right half is the second");
		this.assertEquals(CQInk.ledgerSeatAt(rect, rail.right + 99, 2), 1,
			"a press past the last seat stays on it");
		this.assertEquals(CQInk.ledgerSeatAt(rect, rail.left + (rail.width * 0.5), 3), 1,
			"three seats split the rail into three");
	}

	// ---- the exact-value reading ----

	test_everyParameterPrintsAValueItsOwnRangeCanResolve {
		// a rail with no value is not enough for this page (§12.1.1)
		this.assertEquals(CQGuiWidgets.ledgerFormat(\qShLag, 0.004), "0.0040",
			"a 4 ms slew is readable, not rounded away to zero");
		this.assertEquals(CQGuiWidgets.ledgerFormat(\c5Off, -0.17), "-0.170",
			"a detune offset keeps its sign and useful precision");
		this.assertEquals(CQGuiWidgets.ledgerFormat(\coco1Pan, -0.3), "-0.300",
			"a bipolar value keeps its sign and a stable width");
		this.assertEquals(CQGuiWidgets.ledgerFormat(\coco1Level, 0.8), "0.800",
			"a trailing zero survives, so a column of values keeps one width");
		this.assertEquals(CQGuiWidgets.ledgerFormat(\coco1Bits, 8), "8",
			"an integer rail prints an integer");
		this.assertEquals(CQGuiWidgets.ledgerFormat(\coco1Wrap, 0), "off",
			"a toggle reads as its state, never as 0");
		this.assertEquals(CQGuiWidgets.ledgerFormat(\coco1Wrap, 1), "on",
			"and as on when it is set");
		this.assertEquals(CQGuiWidgets.ledgerFormat(\coco1Interp, \smooth), "smooth",
			"an enum prints CQSpec's own symbol");
		this.assertEquals(CQGuiWidgets.ledgerFormat(\micGain, 0), "0.000",
			"a muted mic reads as an exact zero");
		// the decimals come from the parameter's range, never from a table
		CQGuiWorkshop.paramNames.do { |name|
			this.assert(CQGuiWidgets.ledgerFormat(name,
				CQSpec.default(name)).size > 0,
				"% prints its default".format(name));
		};
	}

	test_bipolarityIsAnswerredByTheRegistry {
		this.assertFloatEquals(CQSpec.centerNorm(\coco1Pan), 0.5,
			"a symmetric bipolar range puts zero at the rail's middle", 1e-9);
		this.assertFloatEquals(CQSpec.centerNorm(\deckARate), 0.5,
			"so does the deck rate", 1e-9);
		this.assertFloatEquals(CQSpec.centerNorm(\c3Off), 0.5,
			"and a castle detune", 1e-9);
		this.assert(CQSpec.centerNorm(\coco1Level).isNil,
			"a unipolar rail has no centre tick and fills from its left end");
		this.assert(CQSpec.centerNorm(\deckALevel).isNil,
			"a range that never crosses zero has no centre to mark");
		this.assert(CQSpec.centerNorm(\coco1Interp).isNil,
			"an enum has no rail at all");
	}

	// ---- the funnel bindings ----

	test_aNumericRowWritesTheMappedValueThroughTheFunnel {
		var row = this.prRow(\coco1Level, "level");
		this.assertEquals(row.kind, \ledger, "the control is a ledger row");
		this.assertEquals(row.ledgerMode, \num, "a continuous parameter gets a rail");
		row.value = 1;
		row.action.value(row);
		this.assertFloatEquals(state.get(\coco1Level), CQSpec.map(\coco1Level, 1),
			"the rail's right end writes the parameter's own maximum", 1e-6);
		row.value = 0.5;
		row.action.value(row);
		this.assertFloatEquals(state.get(\coco1Level), CQSpec.map(\coco1Level, 0.5),
			"the mapping is CQSpec's, curve included", 1e-6);
	}

	test_anIntegerRowSnapsAndPrintsAnInteger {
		var row = this.prRow(\coco1Bits, "bits");
		this.assertEquals(row.ledgerMode, \int, "bits uses the integer ledger");
		row.value = 0.5;
		row.action.value(row);
		this.assert(state.get(\coco1Bits).isInteger,
			"an integer parameter stays an integer through the funnel");
		this.assertEquals(state.get(\coco1Bits), CQSpec.map(\coco1Bits, 0.5),
			"and lands on CQSpec's own step");
	}

	test_aToggleRowUsesTwoStableSeats {
		var row = this.prRow(\coco1Wrap, "wrap");
		this.assertEquals(row.ledgerMode, \toggle, "wrap uses the toggle ledger");
		this.assertEquals(row.seats, ["off", "on"], "two stable seats");
		row.value = 1;
		row.action.value(row);
		this.assertEquals(state.get(\coco1Wrap), 1, "the second seat writes 1");
		row.value = 0;
		row.action.value(row);
		this.assertEquals(state.get(\coco1Wrap), 0, "the first writes 0");
	}

	test_anEnumRowSeatsCQSpecsOwnOrder {
		var row = this.prRow(\coco1Interp, "read");
		var values = CQSpec.enumValues(\coco1Interp);
		this.assertEquals(row.ledgerMode, \enum, "interp uses the enum ledger");
		this.assertEquals(row.seats, values.collect { |v| v.asString },
			"one labelled seat per value, in CQSpec's order — no copied array");
		values.do { |value, i|
			row.value = i;
			row.action.value(row);
			this.assertEquals(state.get(\coco1Interp), value,
				"seat % writes %".format(i, value));
		};
	}

	test_aBipolarRowGrowsFromItsCentreTick {
		var row = this.prRow(\coco1Pan, "pan");
		this.assertFloatEquals(row.center, 0.5,
			"the row carries the registry's own centre", 1e-9);
		row.value = 0;
		row.action.value(row);
		this.assertFloatEquals(state.get(\coco1Pan), -1.0,
			"the rail's left end is the parameter's minimum", 1e-6);
		this.assert(this.prRow(\coco1Level, "level").center.isNil,
			"a unipolar row carries no centre at all");
	}

	// A funnel echo must move the row without firing a write back (the widget
	// factory's standing law: setting .value never fires .action).
	test_anExternalWriteRepaintsTheRowWithoutEchoing {
		var row = this.prRow(\coco1Level, "level"), writes = 0;
		var previousAction = row.action;
		row.action = { |c| writes = writes + 1; previousAction.value(c) };
		state.set(\coco1Level, 0.75);
		updaters[\coco1Level].do { |func| func.value(state.get(\coco1Level)) };
		this.assertFloatEquals(row.value, CQSpec.unmap(\coco1Level, 0.75),
			"a code-layer write moves the handle", 1e-6);
		this.assertEquals(row.display, CQGuiWidgets.ledgerFormat(\coco1Level, 0.75),
			"and the exact value follows it");
		this.assertEquals(writes, 0,
			"following an external write never writes back");
	}

	test_everyManifestEntryBuildsExactlyOneEnabledLedgerRow {
		var built = CQGuiWorkshop.islands.inject([]) { |all, island|
			all ++ CQGuiWorkshop.islandRows(island).collect { |row|
				this.prRow(row[\param], row[\label], row[\family])
			}
		};
		this.assertEquals(built.size, 24,
			"the manifest builds 24 controls");
		this.assertEquals(built.collect { |row| row.kind }.as(IdentitySet).asArray,
			[\ledger],
			"all 24 are ledger rows — no native widgets and no Day knobs");
		built.do { |row|
			this.assert(row.isInteractive,
				"'%' is an enabled interactive control".format(row.label));
			this.assert(row.action.notNil, "'%' is funnel-bound".format(row.label));
		};
		this.assertEquals(owner.controls.size, 24,
			"every row is registered with the GUI's Ink control list");
		CQGuiWorkshop.paramNames.do { |name|
			this.assertEquals((updaters[name] ? []).size, 1,
				"% registers exactly one row updater".format(name));
		};
	}

	// §12.1.3: each variant must expose a secondary shape channel, so meaning
	// never rides on hue alone (§16.2).
	test_eachVariantExposesItsOwnShapeChannel {
		this.assertEquals(this.prRow(\liveGain, "live gain").ledgerMode, \num,
			"a plain trim is a rail filling from the left");
		this.assert(this.prRow(\deckARate, "rate").center.notNil,
			"a bipolar rate shows where its zero is");
		this.assertEquals(this.prRow(\coco2Bits, "bits").ledgerMode, \int,
			"an integer rail is its own mode");
		this.assert(this.prRow(\deckALoop, "loop").isSeated,
			"a toggle sits on seats rather than sliding");
		this.assert(this.prRow(\coco2Interp, "read").isSeated,
			"so does an enum");
		this.assert(this.prRow(\coco2Level, "level").isSeated.not,
			"a continuous parameter is not seated");
	}
}
