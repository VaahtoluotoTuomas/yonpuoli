// GUI_SPEC §2/§5/§8/§16: pure layout inventory for the first Day skin.
// The test prevents the old administrative controls from leaking back into
// the persistent Master strip while the Qt composition evolves.

CQGuiVisualContractTest : UnitTest {
	test_dayKeepsTheFourOrganOrder {
		this.assertEquals(CQGuiPanels.dayRegions,
			#[sources, coco1, quantussy, coco2],
			"Day retains the four learned organ columns");
	}

	test_masterIsPersistentPerformanceOnly {
		var items = CQGuiPanels.masterItems;
		#[masterVol, masterDry, spaceMix, patchDepth, gr, notice, rec, panic].do { |item|
			this.assert(items.includes(item), "Master keeps %".format(item));
		};
		#[snapshotName, snapshotSave, snapshotRecall, morph].do { |item|
			this.assert(items.includes(item).not,
				"Master never contains Drawer-memory item %".format(item));
		};
	}

	test_snapshotToolsLiveInMemoryDrawer {
		var items = CQGuiPanels.memoryItems;
		#[snapshotName, snapshotSave, snapshotRecall, morph].do { |item|
			this.assert(items.includes(item), "Memory Drawer owns %".format(item));
		};
	}

	test_drawerPublishesTheThreeNormativePages {
		this.assertEquals(CQGuiDrawer.tabs, #[inspector, spellbook, memory],
			"Drawer exposes Inspector, Spellbook and Memory in normative order");
	}

	test_inspectorPrototypeUsesPlayablePatchLoomCells {
		this.assertEquals(CQGuiMatrix.cellSize, [48, 24],
			"workbench prototype replaces the tiny ledger targets with playable knots");
		this.assertEquals(CQGuiMatrix.loomSize, Size(1128, 384),
			"the exact 16x22 loom fits the 1280x720 central workspace without scrolling");
	}

	test_memoryPrototypeUsesTheLoomContentRail {
		this.assertEquals(CQGuiMemory.contentWidth, CQGuiMatrix.loomSize.width,
			"Memory seed bank aligns with the Inspector patch-loom rail");
		this.assertEquals(CQGuiMemory.fingerprintSize, Size(840, 300),
			"Memory gives the selected seed a substantial patch fingerprint");
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

	test_dayEndpointInventoryMatchesTheMatrixExactly {
		if(CQGuiPanels.respondsTo(\sourceEndpointNames).not
			or: { CQGuiPanels.respondsTo(\destEndpointNames).not }) {
			this.assert(false, "Day publishes its semantic cable endpoint inventory");
			^this
		};
		this.assertEquals(CQGuiPanels.sourceEndpointNames, CQMatrix.sourceNames,
			"Day has exactly the canonical 16 source jacks");
		this.assertEquals(CQGuiPanels.destEndpointNames, CQMatrix.destNames,
			"Day has exactly the canonical 22 destination sockets");
	}

	test_columnsPublishFunctionalIslands {
		this.assertEquals(CQGuiPanels.cocoIslands,
			#[identity, performance, converter, output],
			"Coco groups controls by signal and performance role");
		this.assertEquals(CQGuiPanels.sourcesIslands, #[inputs, decks],
			"Sources distinguishes live inputs from file decks");
		this.assertEquals(CQGuiPanels.quantussyIslands,
			#[macro, castles, topology],
			"Quantussy gives its functional topology a deliberate region");
	}
}
