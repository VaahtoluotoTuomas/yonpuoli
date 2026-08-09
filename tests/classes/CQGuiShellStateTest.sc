// N9 serverless shell contract: one Night instrument, a click-only Drawer,
// persistent Master and cancellable window lifecycle.

CQGuiShellStateTest : UnitTest {
	var shellClass, matrix, audition, shell;

	setUp {
		shellClass = \CQGuiShellState.asClass;
		matrix = CQMatrix(nil);
		audition = CQAudition(matrix);
		shell = shellClass !? { shellClass.new(audition) };
	}

	prRequireShell {
		if(shell.isNil) {
			this.assert(false,
				"N9: CQGuiShellState owns Night/Drawer/window lifecycle");
			^false
		};
		^true
	}

	test_minimumAndPersistentRegions {
		if(this.prRequireShell.not) { ^this };
		this.assertEquals(shell.minSize, [1280, 720],
			"minimum window is 1280x720");
		this.assertEquals(shell.viewMode, \night,
			"the one instrument destination is Night");
		this.assert(shell.masterVisible,
			"Master is persistent beside Night and Drawer");
	}

	test_drawerIsClickStateAndNeverCoversMaster {
		if(this.prRequireShell.not) { ^this };
		this.assert(shell.drawerOpen.not, "Drawer begins closed");
		shell.toggleDrawer;
		this.assert(shell.drawerOpen, "click action opens Drawer");
		shell.selectDrawer(\spellbook);
		this.assertEquals(shell.drawerTab, \spellbook,
			"Drawer selects a normative tab");
		this.assert(shell.masterVisible, "open Drawer never covers Master");
		shell.selectDrawer(\bogus);
		this.assertEquals(shell.drawerTab, \spellbook,
			"unknown Drawer tab is ignored");
	}

	test_drawerWorkbenchPersistsItsPageAcrossClose {
		if(this.prRequireShell.not) { ^this };
		shell.selectDrawer(\memory);
		shell.toggleDrawer;
		shell.toggleDrawer;
		this.assertEquals(shell.drawerTab, \memory,
			"collapsed tab reopens the last stable workbench page");
		this.assertEquals(shell.viewMode, \night,
			"workbench navigation never invents a second instrument mode");
	}

	test_closeRevertsAnActiveAudition {
		if(this.prRequireShell.not) { ^this };
		matrix.patch(\tri1, \coco1_spaf, 0.2);
		audition.preview(\tri1, \coco1_spaf, 0.8);
		shell.close;
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.2,
			"window close reverts the live audition");
	}

	test_buildLifecycleCanCancelBeforeOpen {
		if(this.prRequireShell.not) { ^this };
		this.assertEquals(shell.lifecycleState, \building, "new shell is building");
		this.assert(shell.canBuild, "building shell may construct Qt");
		shell.close;
		this.assertEquals(shell.lifecycleState, \closed,
			"close-before-build reaches closed without resurrection");
		this.assert(shell.canBuild.not, "closed shell rejects queued Qt construction");
		shell.markOpen;
		this.assertEquals(shell.lifecycleState, \closed,
			"late build callback cannot reopen");
	}
}
