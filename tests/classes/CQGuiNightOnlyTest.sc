// N9 — Night is the instrument, not one half of a two-skin runtime.
// This contract keeps the removal structural and reversible: the live shell
// has one instrument destination, while the Drawer and persistent Master keep
// the complete Night 31 + Master 4 + Workshop 39 ownership proved elsewhere.

CQGuiNightOnlyTest : UnitTest {
	test_shellHasOneNightInstrumentDestination {
		var shell = CQGuiShellState(CQAudition(CQMatrix(nil)));
		this.assertEquals(shell.viewMode, \night,
			"the instrument opens in Night");
		this.assert(CQGuiShellState.respondsTo(\toggleView).not,
			"presentation state owns no Day/Night toggle");
		this.assert(CQGuiShell.respondsTo(\toggleView).not,
			"the Qt shell exposes no second instrument destination");
		this.assert(shell.masterVisible,
			"the persistent Master remains part of the one instrument");
	}

	test_dayPanelBuildersAreGone {
		#[dayRegions, sources, coco, quantussy, dropTargetName,
			sourceEndpointNames, destEndpointNames, cocoIslands,
			sourcesIslands, quantussyIslands].do { |selector|
			this.assert(CQGuiPanels.respondsTo(selector).not,
				"Day-only panel API is removed: %".format(selector));
		};
		this.assert(CQGuiPanels.respondsTo(\master),
			"the persistent Master builder remains");
		this.assert(CQInk.respondsTo(\drawCocoSignalPath).not,
			"the dead static Coco overview has no drawing path");
		this.assert(CQInk.respondsTo(\drawCastleConstellation).not
			and: { CQGuiCastleConstellation.respondsTo(\normalizedPositions).not },
			"the dead Day topology overview has no geometry or drawing path");
		this.assertEquals(CQGuiPanels.masterParamNames,
			#[masterVol, masterDry, patchDepth, monitorMute],
			"Master keeps its four exact CQSpec owners");
	}

	test_nightDrawerMasterStillCoverTheInstrument {
		var union = CQGuiNightMap.reachableParamNames
			++ CQGuiPanels.masterParamNames
			++ CQGuiWorkshop.paramNames;
		this.assertEquals(union.asArray.copy.sort { |a, b| a.asString <= b.asString },
			CQSpec.names,
			"Night + Drawer + Master still reaches every CQSpec parameter");
		this.assertEquals(CQGuiDrawer.tabs,
			#[inspector, workshop, spellbook, memory],
			"the complete four-page workbench remains");
	}
}
