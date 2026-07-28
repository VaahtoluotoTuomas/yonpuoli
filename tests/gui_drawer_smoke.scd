// G1 Drawer Qt smoke: hold the real Inspector workbench visibly rendered for
// one Qt turn, then exercise patch loom, Spellbook, Memory and toolbar tab.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));
var gui, ok = true, abort;
var workshop, rowOf, railOf, pressRow, centred;
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
	CQInk.ledgerRailRect(Rect(0, 0, view.bounds.width, view.bounds.height))
};
pressRow = { |name, x|
	var view = rowOf.value(name).view;
	view.mouseDownAction.value(view, x, CQInk.ledgerMetrics[\height] / 2);
	view.mouseUpAction.value(view, x, CQInk.ledgerMetrics[\height] / 2, 0, 0);
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
											check.value(workshop.rail.bounds.width
												== CQGuiWorkshop.railWidth
												and: { workshop.rail.bounds.height
													== CQGuiWorkshop.railHeight }
												and: { centred.value(workshop.rail, workshop.view) },
												"the fixed 1128 x 504 px content rail is centred");
											check.value(workshop.serviceModules.size == 4
												and: { workshop.serviceModules.every { |module|
													module.bounds.width == CQGuiWorkshop.serviceWidth
														and: { module.bounds.height
															== CQGuiWorkshop.serviceHeight }
												} },
												"four equal 270 px modules make the shallow service band");
											check.value(workshop.mainLedgers
												.collect { |ledger| ledger.bounds.width }
												== CQGuiWorkshop.mainWidths
												and: { workshop.mainLedgers.every { |ledger|
													ledger.bounds.height == CQGuiWorkshop.mainHeight
												} },
												"the body is the 365 · 366 · 365 coco / quantussy / coco triptych");
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
													// a bigger window buys slate, not
													// stretched controls (§12.1.3)
													gui.window.setInnerExtent(1920, 1005);
													AppClock.sched(0.35, {
														try {
															check.value(workshop.micLive.not
																and: { (gui.noticeText ? "").find("MIC LIVE").isNil },
																"muting the mic clears the row and the notice together");
															check.value(workshop.rail.bounds.width
																== CQGuiWorkshop.railWidth
																and: { centred.value(workshop.rail, workshop.view) },
																"the rail stays 1128 px and centred in a 1920x1005 window");
															check.value(workshop.view.bounds.width
																> CQGuiWorkshop.railWidth,
																"the extra width becomes surrounding slate");
															check.value(workshop.mainLedgers
																.collect { |ledger| ledger.bounds.width }
																== CQGuiWorkshop.mainWidths
																and: { rowOf.value(\c5Send).view.bounds.width
																	== CQGuiWorkshop.mainWidths[1] },
																"no control is stretched into a different hierarchy");
															check.value(CQInkControl.drawErrors == 0,
																"both skins and both window sizes paint cleanly");
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
