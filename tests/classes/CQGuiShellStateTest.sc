// G1.1 serverless window-shell contract. It locks behavior, not Qt widget count:
// cancellable lifecycle, Day/Night hard cut, persistent Master and stable workbench.

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
				"G1.1: CQGuiShellState owns Day/Night/Drawer/window lifecycle");
			^false
		};
		^true
	}

	test_minimumAndPersistentRegions {
		if(this.prRequireShell.not) { ^this };
		this.assertEquals(shell.minSize, [1280, 720], "minimum window is 1280x720");
		this.assertEquals(shell.viewMode, \day, "shell opens in Day");
		this.assert(shell.masterVisible, "Master is persistent in Day");
		shell.toggleView(false);
		this.assertEquals(shell.viewMode, \night,
			"the toolbar destination hard-cuts to Night");
		this.assert(shell.masterVisible, "Master remains persistent in Night");
	}

	test_textFocusBlocksDestinationAndToggleRevertsAudition {
		if(this.prRequireShell.not) { ^this };
		matrix.patch(\tri1, \coco1_spaf, 0.2);
		audition.preview(\tri1, \coco1_spaf, 0.8);
		shell.toggleView(true);
		this.assertEquals(shell.viewMode, \day,
			"the destination switch is ignored while text has focus");
		this.assert(audition.isActive,
			"a blocked destination switch does not cancel the current gesture");
		shell.toggleView(false);
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.2,
			"accepted view toggle reverts audition before the hard cut");
	}

	test_drawerIsClickStateAndNeverCoversMaster {
		if(this.prRequireShell.not) { ^this };
		this.assert(shell.drawerOpen.not, "Drawer begins closed");
		shell.toggleDrawer;
		this.assert(shell.drawerOpen, "click action opens Drawer");
		shell.selectDrawer(\spellbook);
		this.assertEquals(shell.drawerTab, \spellbook, "Drawer selects a normative tab");
		this.assert(shell.masterVisible, "open Drawer never covers Master");
		shell.selectDrawer(\bogus);
		this.assertEquals(shell.drawerTab, \spellbook, "unknown Drawer tab is ignored");
	}

	test_drawerWorkbenchPersistsItsPageAcrossClose {
		if(this.prRequireShell.not) { ^this };
		shell.selectDrawer(\memory);
		shell.toggleDrawer;
		shell.toggleDrawer;
		this.assertEquals(shell.drawerTab, \memory,
			"collapsed tab reopens the last stable workbench page");
	}

	test_viewDestinationClosesTheWorkbench {
		if(this.prRequireShell.not) { ^this };
		shell.toggleDrawer;
		this.assert(shell.drawerOpen, "workbench begins open for destination switch");
		shell.toggleView(false);
		this.assertEquals(shell.viewMode, \night, "view destination reaches Night");
		this.assert(shell.drawerOpen.not,
			"choosing Day/Night returns from the workbench to the instrument");
	}

	test_buildLifecycleCanCancelBeforeOpen {
		if(this.prRequireShell.not) { ^this };
		this.assertEquals(shell.lifecycleState, \building, "new shell is building");
		this.assert(shell.canBuild, "building shell may construct Qt");
		shell.close;
		this.assertEquals(shell.lifecycleState, \closed,
			"close-before-build reaches closed without an intermediate resurrection");
		this.assert(shell.canBuild.not, "closed shell rejects queued Qt construction");
		shell.markOpen;
		this.assertEquals(shell.lifecycleState, \closed, "late build callback cannot reopen");
	}
}
