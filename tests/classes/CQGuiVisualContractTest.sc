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
}
