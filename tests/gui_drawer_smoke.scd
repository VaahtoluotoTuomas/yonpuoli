// G1 Drawer Qt smoke: hold the real Inspector workbench visibly rendered for
// one Qt turn, then exercise patch loom, Spellbook, Memory and toolbar tab.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));
var gui, ok = true, abort;
var workshop, rowOf, railOf, pressRow, centred, beforeRow, beforeRail;
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
// The Workshop's rail is the same rectangle its rows are drawn on and pressed
// through (§4.5): the smoke aims at CQInk's own geometry, never at a guess.
rowOf = { |name| workshop.rowFor(name) };
railOf = { |name|
	var view = rowOf.value(name).view;
	CQInk.ledgerRailRect(Rect(0, 0, view.bounds.width, view.bounds.height),
		CQGuiScale.metrics(gui.drawer.step))
};
pressRow = { |name, x|
	var view = rowOf.value(name).view, y = view.bounds.height / 2;
	view.mouseDownAction.value(view, x, y);
	view.mouseUpAction.value(view, x, y, 0, 0);
};
centred = { |inner, outer|
	(inner.absoluteBounds.center.x - outer.absoluteBounds.center.x).abs <= 1
};

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
						and: { CQGuiDrawer.tabs
							== #[inspector, workshop, spellbook, memory] }
						and: { drawer.pages.size == CQGuiDrawer.tabs.size }
						and: { drawer.pages.every { |page| page.notNil } },
						"real Drawer owns the four normative pages");
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
					check.value(grid.cellSize[0] >= 48
						and: { grid.cellSize[1] >= 24 }
						and: { grid.grid.bounds.width
							== CQGuiScale.railWidth(drawer.step, drawer.pageSize.width) }
						and: { grid.grid.bounds.height
							== (CQMatrix.sourceNames.size * grid.cellSize[1]) }
						and: { grid.header.bounds.height
							== CQGuiScale.metrics(drawer.step)[\headerHeight] },
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
							check.value(drawer.memoryGui.fingerprint.bounds
								== Rect(0, 0,
									CQGuiMemory.fingerprintSize(drawer.step,
										drawer.memoryGui.railWidth).width,
									CQGuiMemory.fingerprintSize(drawer.step,
										drawer.memoryGui.railWidth).height)
									.moveTo(drawer.memoryGui.fingerprint.bounds.left,
										drawer.memoryGui.fingerprint.bounds.top),
								"Memory gives the live/selected patch a substantial fingerprint");
							check.value(drawer.memoryGui.railWidth
								== CQGuiScale.railWidth(drawer.step,
									drawer.pageSize.width)
								and: { drawer.spellbookGui.rail.bounds.width
									== drawer.memoryGui.railWidth },
								"every Drawer page shares one content rail");
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
									// §12.1.5: the night-first path — Night, then
									// the Drawer, then the Workshop, at the 1280x720
									// minimum and in the Night skin.
									workshop = drawer.workshop;
									gui.shell.selectDrawer(\workshop);
									gui.shell.toggleDrawer;
									AppClock.sched(0.3, {
										try {
											var beforeLevel, interpRail, dustRail;
											check.value(gui.shellState.viewMode == \night
												and: { gui.shellState.drawerTab == \workshop }
												and: { gui.shell.workspaceStack.index == 1 }
												and: { workshop.view.visible },
												"Night opens the Drawer onto the visible Workshop page");
											check.value(gui.shell.masterView.visible,
												"the persistent Master strip stays visible beside it");
											check.value(drawer.step == \compact
												and: { drawer.pageSize == Size(1272, 526) },
												"the 1280x720 minimum is the compact step on a 1272x526 page");
											check.value(workshop.rail.bounds.width
												== CQGuiScale.railWidth(\compact, 1272)
												and: { workshop.rail.bounds.height
													>= CQGuiScale.minimumHeight(\compact) }
												and: { workshop.rail.bounds.height
													<= workshop.view.bounds.height }
												and: { centred.value(workshop.rail, workshop.view) },
												"the shared content rail is centred and fits its page");
											check.value(workshop.serviceModules.size == 4
												and: { workshop.serviceModules.collect { |module|
													module.bounds.width
												} == CQGuiWorkshop.serviceWidths(
													workshop.railWidth, drawer.step) }
												and: { workshop.serviceModules.every { |module|
													module.bounds.height
														== CQGuiScale.serviceBandHeight(drawer.step)
												} },
												"four equal modules make the shallow service band");
											check.value(workshop.mainLedgers
												.collect { |ledger| ledger.bounds.width }
												== CQGuiWorkshop.mainWidths(
													workshop.railWidth, drawer.step)
												and: { workshop.mainLedgers[0].bounds.width
													== workshop.mainLedgers[2].bounds.width },
												"the body is the coco / quantussy / coco triptych, the pair equal");
											check.value(workshop.rows.size == 43
												and: { workshop.rows.every { |row|
													row.kind == \ledger and: { row.isInteractive }
												} },
												"all 43 parameters render as enabled ledger rows");
											// no scroll view: the last row of the
											// deepest group must lie inside the page
											check.value(#[coco1SlotX, coco2SlotX, c5Send]
												.every { |name|
													rowOf.value(name).view.absoluteBounds.bottom
														<= (workshop.view.absoluteBounds.bottom + 1)
												},
												"the last Coco slot rows and the fifth castle send are contained");
											check.value(CQInkControl.drawErrors == 0,
												"the islands and 43 ledger rows paint without callback errors");
											// the rail writes through the funnel, and
											// only the rail does
											dustRail = railOf.value(\coco1Dust);
											pressRow.value(\coco1Dust, dustRail.right);
											check.value(state.get(\coco1Dust)
												== CQSpec.map(\coco1Dust, 1),
												"a press at the rail's end writes the parameter's own maximum");
											beforeLevel = state.get(\coco1Level);
											pressRow.value(\coco1Level, 4);
											check.value(state.get(\coco1Level) == beforeLevel,
												"a press on the label column writes nothing");
											interpRail = railOf.value(\coco1Interp);
											pressRow.value(\coco1Interp, interpRail.right - 2);
											check.value(state.get(\coco1Interp)
												== CQSpec.enumValues(\coco1Interp).last,
												"an enum seat writes CQSpec's own value");
											// the exact value follows through the §10
											// paced stream, so it is read after the
											// next flush rather than in the same frame
											// SPEC §7: raise the mic and leave it up
											// across a page switch
											state.set(\micGain, 2);
											gui.shell.selectDrawer(\inspector);
											gui.shell.selectDrawer(\workshop);
											gui.shell.toggleView(false);
											gui.shell.toggleDrawer;
											AppClock.sched(0.3, {
												try {
													check.value(gui.shellState.viewMode == \day
														and: { gui.shellState.drawerTab == \workshop }
														and: { workshop.view.visible },
														"the Workshop page persists across the hard cut and reopen");
													check.value(workshop.rail.background
														== CQInk.palette(\day)[\recess],
														"the same geometry re-inks as ink on recessed paper");
													check.value(rowOf.value(\coco1Interp).display
														== CQSpec.enumValues(\coco1Interp).last.asString
														and: { rowOf.value(\coco1Dust).display
															== CQGuiWidgets.ledgerFormat(\coco1Dust,
																state.get(\coco1Dust)) },
														"every row's exact value follows the paced state stream");
													state.set(\coco1Interp, CQSpec.default(\coco1Interp));
													state.set(\coco1Dust, CQSpec.default(\coco1Dust));
													check.value(workshop.micLive
														and: { rowOf.value(\micGain).display == "2.000" },
														"the one mic control follows the live value exactly");
													check.value((gui.noticeText ? "").find("MIC LIVE").notNil,
														"the persistent MIC LIVE notice survives the page switch");
													check.value(gui.inkControls.select { |control|
														control.label == "mic gain"
													}.size == 1,
														"exactly one interactive mic control exists in the whole GUI");
													check.value(gui.inkControls.select { |control|
														control.kind == \knob and: { control.label == "mic" }
													}.isEmpty,
														"Day no longer owns a second mic knob");
													state.set(\micGain, 0);
													// A maximized window is read at the
													// bigger step: the extra room buys
													// legibility, not empty slate
													// (the user's N8 review, 2026-07-28).
													// The screen may clamp the request,
													// so everything below is asserted
													// against the size actually achieved.
													beforeRow = rowOf.value(\c5Send).view.bounds.height;
													beforeRail = workshop.railWidth;
													gui.window.setInnerExtent(1920, 1005);
													AppClock.sched(0.4, {
														try {
															check.value(workshop.micLive.not
																and: { (gui.noticeText ? "").find("MIC LIVE").isNil },
																"muting the mic clears the row and the notice together");
															check.value(drawer.step == \roomy
																and: { drawer.pageSize.width >= 1400 },
																"a maximized window reaches the roomy step (page %)"
																	.format(drawer.pageSize));
															check.value(rowOf.value(\c5Send).view.bounds.height
																> beforeRow
																and: { workshop.railWidth > beforeRail },
																"rows and rail both grow — the room buys legibility");
															check.value(workshop.rail.bounds.width
																== CQGuiScale.railWidth(drawer.step,
																	drawer.pageSize.width)
																and: { centred.value(workshop.rail, workshop.view) },
																"the rail keeps its measured width and stays centred");
															check.value(workshop.view.bounds.width
																- workshop.rail.bounds.width
																>= (2 * CQGuiScale.metrics(drawer.step)[\pageMargin]),
																"and it always keeps its page margin of slate");
															check.value(workshop.mainLedgers
																.collect { |ledger| ledger.bounds.width }
																== CQGuiWorkshop.mainWidths(
																	workshop.railWidth, drawer.step)
																and: { rowOf.value(\c5Send).view.bounds.width
																	== CQGuiWorkshop.mainWidths(
																		workshop.railWidth, drawer.step)[1] },
																"every row is re-measured to its own island");
															check.value(#[coco1SlotX, coco2SlotX, c5Send]
																.every { |name|
																	rowOf.value(name).view.absoluteBounds.bottom
																		<= (workshop.view.absoluteBounds.bottom + 1)
																},
																"the deepest rows are still contained at the bigger step");
															check.value(grid.cellSize[1]
																> CQGuiScale.metrics(\compact)[\cellHeight]
																and: { drawer.spellbookGui.rail.bounds.width
																	== workshop.railWidth }
																and: { drawer.memoryGui.railWidth
																	== workshop.railWidth },
																"the loom grows too and all four pages keep one rail");
															check.value(CQInkControl.drawErrors == 0,
																"both skins and both size steps paint cleanly");
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
