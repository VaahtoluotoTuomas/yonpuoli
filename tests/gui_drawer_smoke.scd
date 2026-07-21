// G1 Drawer Qt smoke: hold the real Inspector workbench visibly rendered for
// one Qt turn, then exercise patch loom, Spellbook, Memory and toolbar tab.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));
var gui, ok = true, abort;
var check = { |condition, message|
	if(condition) { ("ok:   " ++ message).postln } {
		ok = false;
		("FAIL: " ++ message).postln;
	};
};
abort = { |error|
	error !? { error.reportError };
	("FAIL: Drawer smoke aborted"
		++ (error !? { ": " ++ error.errorString } ? "")).postln;
	gui !? { gui.free };
	"CQ_GUI_DRAWER_FAILED".postln;
	1.exit;
};
CQInkControl.resetDrawErrors;

{
	try {
		gui = CQGui(state, matrix, config);
		AppClock.sched(0.2, {
			// This is the user's exact path: open the toolbar tab onto Inspector,
			// then yield so native Qt must lay out and paint the complete loom.
			gui.shell.selectDrawer(\inspector);
			gui.shell.toggleDrawer;
			AppClock.sched(0.2, {
				try {
					var drawer = gui.drawer, grid = gui.drawer.grid;
					var spellbook = gui.drawer.spellbook;
					var spellbookGui = gui.drawer.spellbookGui;
					var beforePreset;
					check.value(drawer.notNil
						and: { CQGuiDrawer.tabs == #[inspector, spellbook, memory] },
						"real Drawer owns the three normative pages");
					check.value(gui.shellState.drawerOpen
						and: { gui.shell.workspaceStack.index == 1 }
						and: { drawer.inspectorView.visible },
						"toolbar Drawer tab visibly renders the Inspector workbench");
					check.value(CQInkControl.drawErrors == 0,
						"Inspector header and patch loom paint without callback errors");
					check.value(grid.header !== grid.grid
						and: { grid.header.drawFunc.notNil }
						and: { grid.status.parent !== grid.grid },
						"Inspector keeps grouped headers and exact route detail outside the loom");
					check.value(CQGuiMatrix.cellSize == [48, 24]
						and: { grid.grid.bounds.width == 1128 }
						and: { grid.grid.bounds.height == 384 }
						and: { grid.header.bounds.height == 64 },
						"workbench exposes the full 16x22 patch loom with large targets");
					check.value(grid.status.absoluteBounds.left == grid.grid.absoluteBounds.left
						and: { grid.status.parent.absoluteBounds.right
							== grid.grid.absoluteBounds.right },
						"route ribbon and preset tools share the exact loom content rail");
					check.value(grid.grid.absoluteBounds.bottom
						<= (drawer.inspectorView.absoluteBounds.bottom + 1),
						"complete loom stays inside the visible Inspector workspace");
					check.value(grid.grid.background == CQInk.palette(\day)[\recess],
						"patch loom owns the Ink recess instead of native Qt blue");
					check.value(grid.grid.mouseDownAction.notNil
						and: { grid.grid.mouseUpAction.notNil },
						"Inspector grid exposes touch-and-listen mouse actions");
					grid.grid.mouseDownAction.value(grid.grid, 96, 12, 0, 0, 1);
					check.value(matrix.getPatch(\tri1, \coco1_spaf) == 0.3,
						"large knot press auditions +0.3 through the shared funnel");
					check.value(grid.selectedSrc == \tri1
						and: { grid.selectedDest == \coco1_spaf },
						"selected knot persists as the route-ribbon context");
					grid.grid.mouseUpAction.value(grid.grid, 96, 12, 0, 0);
					check.value(matrix.getPatch(\tri1, \coco1_spaf) == 0.0,
						"knot release reverts Inspector audition");

					matrix.patch(\envA, \deckA_rate, -0.4);
					check.value(spellbook.lines.last.find("envA").notNil,
						"Spellbook receives an external code-layer notification");
					spellbookGui.listView.items = spellbook.lines;
					spellbookGui.listView.value = spellbook.lines.size - 1;
					check.value(spellbookGui.listView.selectionMode == \single
						and: { spellbookGui.selectedLine == spellbook.lines.last },
						"clicked Spellbook row remains visibly and semantically selected");
					check.value((spellbookGui.listView.colors.isNil
						or: { spellbookGui.listView.colors.isEmpty })
						and: { spellbookGui.listView.background
							!= spellbookGui.listView.stringColor },
						"Spellbook uses one readable paper/text pair, not Qt row backgrounds");
					beforePreset = matrix.fullMatrix;
					drawer.applyPreset(\classic);
					check.value(spellbook.lastEntry[\lines].first == "// preset",
						"Inspector preset apply is one named Spellbook batch");
					drawer.spellbookGui.copySelectedLine;
					drawer.spellbookGui.copyPresetData;
					check.value(true,
						"Spellbook copy-line and preset-data clipboard paths execute");
					gui.bananaGrammar.undo;
					check.value(matrix.fullMatrix == beforePreset,
						"preset apply joins the shared matrix undo ring");

					gui.shell.selectDrawer(\memory);
					AppClock.sched(0.15, {
						try {
							check.value(gui.shellState.drawerTab == \memory
								and: { gui.shell.workspaceStack.index == 1 },
								"open workbench visibly selects the Memory seed bank");
							check.value(CQInkControl.drawErrors == 0,
								"Memory fingerprint and morph bridge paint without callback errors");
							check.value(gui.shell.drawerButton.label == "drawer"
								and: { gui.shell.drawerButton.view.mouseDownAction.notNil },
								"toolbar action keeps the stable global Drawer name");
							check.value(drawer.memoryGui.snapshotList.items.first == "live patch · now",
								"Memory always exposes the live patch above the saved snapshots");
							check.value(drawer.memoryGui.fingerprint.bounds.width == 840
								and: { drawer.memoryGui.fingerprint.bounds.height == 300 },
								"Memory gives the live/selected patch a substantial fingerprint");
							gui.shell.selectDrawer(\spellbook);
							check.value(gui.shellState.drawerTab == \spellbook,
								"open workbench returns to the live Spellbook page");
							gui.shell.drawerButton.view.mouseDownAction.value(
								gui.shell.drawerButton.view, 4, 4);
							check.value(gui.shellState.drawerOpen.not,
								"same toolbar tab collapses workbench back to the instrument");
							gui.shell.drawerButton.view.mouseDownAction.value(
								gui.shell.drawerButton.view, 4, 4);
							"CQ_DRAWER_BEFORE_VIEW_TOGGLE".postln;
							gui.shell.toggleView(false);
							"CQ_DRAWER_AFTER_VIEW_TOGGLE".postln;
							check.value(gui.shellState.viewMode == \night,
								"open workbench re-inks across the Day/Night hard cut");
							check.value(gui.shellState.drawerOpen.not,
								"Day/Night destination returns from Drawer to the instrument");
							// The stage swap is queued so hiding a view under the
							// mouse can never deadlock Qt (CQGuiShell.prSwapStage):
							// read the native stack on the next GUI tick.
							AppClock.sched(0.15, {
								try {
									check.value(gui.shell.workspaceStack.index == 0,
										"queued stage swap settles back on the instrument");
									gui.free;
									AppClock.sched(0.2, {
										if(ok) { "CQ_GUI_DRAWER_PASSED".postln; 0.exit } {
											"CQ_GUI_DRAWER_FAILED".postln; 1.exit
										};
										nil
									});
								} { |error| abort.value(error) };
								nil
							});
						} { |error| abort.value(error) };
						nil
					});
				} { |error| abort.value(error) };
				nil
			});
			nil
		});
	} { |error| abort.value(error) };
}.defer;
)
