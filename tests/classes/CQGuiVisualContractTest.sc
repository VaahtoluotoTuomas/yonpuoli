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
