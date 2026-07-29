// GUI_SPEC §2/§8/§16: pure visual and ownership contracts shared by the
// Night-only instrument, Drawer and persistent Master.

CQGuiVisualContractTest : UnitTest {
	test_sowingMoonStartsAsACrescentAndWaxesSlowly {
		this.assertFloatEquals(CQGui.defaultShakeAmount, 0.22,
			"the instrument opens on an iconic small crescent", 1e-9);
		this.assertFloatEquals(CQGui.sowDensity(0), 0,
			"the new moon still clears the bay", 1e-9);
		this.assertFloatEquals(CQGui.sowDensity(0.25), 0.0125,
			"a quarter moon sows a genuinely small patch", 1e-9);
		this.assertFloatEquals(CQGui.sowDensity(0.5), 0.05,
			"the first half waxes slowly", 1e-9);
		this.assertFloatEquals(CQGui.sowDensity(1), CQGui.fullMoonDensity,
			"the full moon keeps the whole former ceiling", 1e-9);
		this.assert(CQGui.sowDensity(0.75) > CQGui.sowDensity(0.5),
			"the second half gathers fertility");
	}

	test_masterIsPersistentPerformanceOnly {
		var items = CQGuiPanels.masterItems;
		#[masterVol, masterDry, patchDepth, gr, notice, rec, panic].do { |item|
			this.assert(items.includes(item), "Master keeps %".format(item));
		};
		this.assert(items.includes(\spaceMix).not,
			"the retired Space control cannot return through the persistent strip");
		#[snapshotName, snapshotSave, snapshotRecall, morph].do { |item|
			this.assert(items.includes(item).not,
				"Master never contains Drawer-memory item %".format(item));
		};
	}

	test_snapshotToolsLiveInMemoryDrawer {
		var items = CQGuiMemory.ownedItems;
		#[snapshotName, snapshotSave, snapshotRecall, morph].do { |item|
			this.assert(items.includes(item), "Memory Drawer owns %".format(item));
		};
	}

	// AIO-1 adds Audio without replacing a musical page; it remains last because
	// it is the technical machine/engine boundary.
	test_drawerPublishesTheFiveNormativePages {
		this.assertEquals(CQGuiDrawer.tabs,
			#[inspector, workshop, spellbook, memory, audio],
			"Drawer exposes Inspector, Workshop, Spellbook, Memory and Audio in order");
	}

	// A page the shell state would refuse is a page the toolbar cannot reach:
	// the whitelist and the published list are one list.
	test_everyPublishedPageIsSelectable {
		var shell = CQGuiShellState(CQAudition(CQMatrix(nil)));
		CQGuiDrawer.tabs.do { |tab|
			shell.selectDrawer(tab);
			this.assertEquals(shell.drawerTab, tab,
				"the shell accepts the published page %".format(tab));
		};
		shell.selectDrawer(\bogus);
		this.assertEquals(shell.drawerTab, CQGuiDrawer.tabs.last,
			"and still refuses a page that does not exist");
	}

	test_audioPresenceUsesCoarseStableDisplaySteps {
		this.assert(CQGuiAudioSetup.presenceNorm(0).isNumber
			and: { CQGuiAudioSetup.presenceNorm(0) == 0 },
			"silence sits at the start of the presence rail");
		this.assertEquals(
			CQGuiAudioSetup.presenceNorm(0.1),
			CQGuiAudioSetup.presenceNorm(0.1001),
			"sub-pixel amplitude noise does not request a new Qt frame");
		this.assert(CQGuiAudioSetup.presenceThreshold > 0
			and: { CQGuiAudioSetup.presenceThreshold < 0.01 },
			"the signal/quiet threshold remains a low raw-port presence test");
		this.assertEquals(CQGui.audioPresencePeriod, 1 / 15,
			"combined input/master presence is paced to 15 Hz");
	}

	// The loom keeps large playable knots at every Drawer size step; its cell
	// width is what sets the rail all five pages align to (CQGuiScale).
	test_inspectorUsesPlayablePatchLoomCellsAtEveryStep {
		CQGuiScale.steps.do { |step|
			var page = if(step == \roomy) { 1434 } { 1272 };
			var cell = CQGuiMatrix.cellSize(step, page);
			var loom = CQGuiMatrix.loomSize(step, page);
			this.assert(cell[0] >= 48 and: { cell[1] >= 24 },
				"% : knots stay at least as large as the reviewed 48x24"
					.format(step));
			this.assertEquals(loom.height,
				CQMatrix.sourceNames.size * cell[1],
				"% : the loom is exactly its 16 source rows".format(step));
			this.assertEquals(loom.width, CQGuiScale.railWidth(step, page),
				"% : the loom's width IS the shared content rail".format(step));
		};
		this.assert(CQGuiMatrix.cellSize(\roomy, 1434)[0]
			> CQGuiMatrix.cellSize(\compact, 1272)[0],
			"a maximized window gives the loom bigger cells, not just more slate");
	}

	test_memoryFillsTheSharedRailAtEveryStep {
		CQGuiScale.steps.do { |step|
			var page = if(step == \roomy) { 1434 } { 1272 };
			var rail = CQGuiScale.railWidth(step, page);
			var print = CQGuiMemory.fingerprintSize(step, rail);
			var column = CQGuiMemory.snapshotColumnWidth(step);
			this.assertEquals(print.width + column + 8, rail,
				"% : the saved-snapshot column and the fingerprint fill the rail"
					.format(step));
			this.assert(print.width >= 840,
				"% : the fingerprint keeps at least its reviewed width".format(step));
		};
		this.assert(
			CQGuiMemory.fingerprintSize(\roomy, CQGuiScale.railWidth(\roomy, 1434))
				.height
			> CQGuiMemory.fingerprintSize(\compact,
				CQGuiScale.railWidth(\compact, 1272)).height,
			"a wider window buys patch detail rather than empty slate");
	}

	// GUI_SPEC §9: the aura is the spec's only server-side addition, and §16.8
	// binds it to cable alpha. Silence about a source is not the same as a
	// source at zero — the first keeps the reviewed ink, the second reads quiet.
	test_auraTelemetryShipsAsItsOwnTelemetryDef {
		var names = CQSynthDefs.all(CQConfig.validate(())).collect { |def| def.name };
		this.assert(names.includes('cqAura'),
			"the graph ships a dedicated /cq_aura telemetry def");
	}

	// GUI_SPEC §9 `/cq_charge`: nine gate/trig sums in matrix destination order.
	// The channel list is derived from CQMatrix, never re-typed, so the lamps
	// cannot drift from the routing truth they report on.
	test_chargeChannelsAreTheNineGateTrigDestinations {
		this.assertEquals(CQGui.chargeNames,
			[\coco1_flip, \coco1_skip, \coco2_flip, \coco2_skip,
				\castle1_spesal, \castle2_spesal, \castle3_spesal,
				\castle4_spesal, \castle5_spesal],
			"charge channels = the matrix destination order filtered to gate/trigs");
		this.assertEquals(CQGui.chargeBlock(1), 0, "coco 1 reports slots 0-1");
		this.assertEquals(CQGui.chargeBlock(2), 2, "coco 2 reports slots 2-3");
		this.assertEquals(CQGui.chargeBlock(3), 4, "the castle row reports slots 4-8");
		this.assert(CQGui.chargeBlock(9).isNil,
			"an unknown replyID claims no lamp slot");
	}

	// The charge is sent by the synths that already compute the sums, so the
	// lamp reads exactly what the Schmitt reads (no second, drifting copy).
	test_chargeIsSentBySynthsThatOwnTheSums {
		var config = CQConfig.validate(());
		var coco = this.prReplies(config, \cqCoco);
		var quantussy = this.prReplies(config, \cqQuantussy);
		var cocoCharge = coco.detect { |reply| reply[\name] == '/cq_charge' };
		var castleCharge = quantussy.detect { |reply| reply[\name] == '/cq_charge' };
		this.assert(cocoCharge.notNil and: { castleCharge.notNil },
			"both the coco def and the quantussy def report their charges");
		this.assertEquals(cocoCharge[\values], 2,
			"each coco instance reports its own flip and skip sum");
		this.assertEquals(castleCharge[\values], 5,
			"the quantussy reports all five spesal sums at once");
		this.assertEquals((2 * cocoCharge[\values]) + castleCharge[\values],
			CQGui.chargeNames.size,
			"the two coco instances and the castle row cover all nine lamps");
		this.assertEquals(castleCharge[\replyID], 3,
			"the castle block claims replyID 3 — the cocos own 1 and 2");
		// the sums the lamps report on must not have cost the DSP a second copy
		this.assert(coco.collect { |reply| reply[\name] }.includes('/cq_phase'),
			"extracting the sums left the read-head telemetry intact");
		// address · direction mean · skip rate — the rate rides the message
		// that already exists rather than opening a fourth stream (§15 leaves
		// message merging free, and §10 counts messages, not floats)
		this.assertEquals(
			coco.detect { |reply| reply[\name] == '/cq_phase' }[\values], 3,
			"the phase message carries address, direction and skip rate");
		this.assertEquals(CQSynthDefs.phaseReplyHz, 15,
			"the read head shares the 15 Hz ambient ceiling with castle motion");
		this.assert(quantussy.collect { |reply| reply[\name] }
			.includes('/cq_castle'),
			"extracting the sums left the castle activity telemetry intact");
	}

	// SendReply's UGen inputs are [trig, replyID, nameLength, name..., values...].
	// Names come back as Symbols: `includes` compares identity (CLAUDE.md).
	prReplies { |config, defName|
		var def = CQSynthDefs.all(config).detect { |d| d.name == defName };
		if(def.isNil) { ^[] };
		^def.children.select { |ugen| ugen.isKindOf(SendReply) }
			.collect { |ugen|
				var size = ugen.inputs[2].asInteger;
				(name: String.newFrom(ugen.inputs.copyRange(3, 2 + size)
						.collect { |code| code.asInteger.asAscii }).asSymbol,
					replyID: ugen.inputs[1],
					values: ugen.inputs.size - 3 - size)
			}
	}

	// A deck filename is centered under its bird, so an overflow would eat both
	// ends. §16.4 fixes the type size — the string gives way, from its tail only.
	test_deckFilenameTrimsFromItsTailOnly {
		var measure = { |string| string.size * 6 };
		var short = "amen.wav";
		var long = "a_very_long_field_recording_name_take_seventeen_final.wav";
		var fitted = CQGui.fitLabel(long, 200, measure);
		this.assertEquals(CQGui.fitLabel(short, 200, measure), short,
			"a name that fits is never touched");
		this.assert(fitted.size < long.size,
			"an overlong name is cut");
		this.assert(long.beginsWith(fitted.drop(-3)),
			"what remains is the name's own head — the cut is at the tail");
		this.assert(measure.value(fitted) <= (200 - 6),
			"the cut name fits its box");
		this.assert(measure.value(long.keep(fitted.size - 2) ++ "…") > (200 - 6),
			"the cut keeps as much of the name as the box can hold");
		this.assertEquals(CQGui.fitLabel(long, 0, measure), long,
			"before layout the box has no width and nothing is cut away");
	}

	test_nightBirdsNameTheirDropTargetsApart {
		this.assert(CQGuiNightMap.dropTargetName(\deckA)
			!= CQGuiNightMap.dropTargetName(\deckB),
			"the two birds are distinct targets");
	}

	// Measured on a real file drag (2026-07-22): Qt asks `canReceiveDrag` once
	// per dragEnter, never per move, so the predicate can arm the border but
	// can never release it. A timed release put the border out while the hand
	// was still holding the file over the box. The border is held by where the
	// cursor actually is, with two independent exits so it cannot stick.
	test_dropBorderIsHeldByTheCursorNotByAClock {
		var rect = Rect(100, 100, 60, 40);
		this.assert(CQGui.dropTargetKeeps(rect, Point(130, 120), true, 99),
			"a cursor inside the target holds the border however long it rests");
		this.assert(CQGui.dropTargetKeeps(rect, Point(300, 120), true, 0).not,
			"the border releases the moment the cursor leaves the target");
		this.assert(CQGui.dropTargetKeeps(rect, Point(130, 120), false, 0).not,
			"a drag that has ended releases it too — the second exit");
		this.assert(CQGui.dropTargetKeeps(nil, Point(130, 120), true, 0).not,
			"a target with no rect to test cannot hold anything");
		// a platform with no cursor to report falls back to the blind hold
		this.assert(CQGui.dropTargetKeeps(rect, nil, true, 0),
			"no cursor: the border keeps its blind hold rather than flickering");
		this.assert(CQGui.dropTargetKeeps(rect, nil, true,
			CQGui.dropTargetHold + 0.1).not,
			"no cursor: the blind hold still expires, so it cannot stick");
	}

	test_cableAlphaPulsesWithSourceAuraOnly {
		this.assertEquals(CQGuiCableOverlay.plantedAlpha(nil), 0.88,
			"no telemetry leaves the planted cable at its reviewed static alpha");
		this.assertFloatEquals(CQGuiCableOverlay.plantedAlpha(0), 0.62,
			"a silent source reads quieter than a reported one");
		this.assertFloatEquals(CQGuiCableOverlay.plantedAlpha(1), 0.92,
			"a hot source lifts its cable toward full ink");
		this.assertFloatEquals(CQGuiCableOverlay.plantedAlpha(4.2),
			CQGuiCableOverlay.plantedAlpha(1),
			"an out-of-range magnitude clamps instead of overdriving alpha");
	}

}
