// G2 Qt smoke: construct the real shell, cross the hard cut into the Night
// map, and assert the archipelago carries the full banana grammar — endpoints,
// plant, undo, harvest, the §4.1 view-flip audition revert — plus the
// telemetry laws (fireflies ride only moving aura frames; no telemetry = the
// static reviewed ink). Prints CQ_GUI_NIGHT_PASSED / CQ_GUI_NIGHT_FAILED.

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
var auraMsg = { |casValue|
	['/cq_aura', 0, 0, 0.2, 0, 0, 0, 0, casValue, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
};
CQInkControl.resetDrawErrors;
matrix.patch(\tri1, \coco1_spaf, 0.55);
matrix.patch(\envB, \castle3_fm, -0.45);
abort = { |error|
	error !? { error.reportError };
	("FAIL: GUI night smoke aborted"
		++ (error !? { ": " ++ error.errorString } ? "")).postln;
	gui !? { gui.free };
	"CQ_GUI_NIGHT_FAILED".postln;
	1.exit;
};

{
	try {
		gui = CQGui(state, matrix, config);
		AppClock.sched(0.25, {
			try {
				var map = gui.nightMap;
				check.value(map.notNil, "night map is built with the shell");
				check.value(map.endpoints.size == 38
					and: { CQMatrix.sourceNames.every { |n| map.endpoints[n].notNil } }
					and: { CQMatrix.destNames.every { |n| map.endpoints[n].notNil } },
					"night registers its own canonical 16 jacks and 22 sockets");
				check.value(map.overlay !== gui.cableOverlay
					and: { map.overlay.skin == \night }
					and: { map.overlay.view.acceptsMouse.not },
					"night owns a hit-transparent phosphor cable overlay");
				check.value(gui.cableOverlay.skin == \day,
					"the day overlay keeps its own fixed ink");
				check.value(map.endpoints.values.every { |endpoint|
					endpoint[\view].mouseDownAction.notNil
				}, "the shared grammar controller binds every night endpoint");
				check.value(map.holdStates[0] == (state.get(\coco1Hold) > 0.5),
					"hold seal state mirrors the power-on param");
				check.value(map.sproutState == \hidden,
					"no mic configured: the sprout is not drawn at all");
				gui.shell.toggleView(false);
				check.value(gui.shellState.viewMode == \night,
					"hard cut reached the night map");
				AppClock.sched(0.2, {
					try {
						var geo = map.geometry, expected, view;
						var sourceView, destView, destPoint, dragPoint;
						var before, triView, target, movePoint;
						check.value(geo.notNil, "night geometry is laid out");
						expected = geo[\endpoints][\tri1];
						view = map.endpoints[\tri1][\view];
						check.value(
							(view.bounds.left - (expected.x - 13)).abs < 1
							and: { (view.bounds.top - (expected.y - 13)).abs < 1 },
							"a castle leaf jack sits exactly on its computed home");
						check.value(map.overlay.cableCount == 2,
							"night overlay renders every planted matrix cable");
						sourceView = map.endpoints[\org2][\view];
						destView = map.endpoints[\deckB_rate][\view];
						destPoint = destView.mapToGlobal(
							map.endpoints[\deckB_rate][\anchor]);
						dragPoint = destPoint - sourceView.mapToGlobal(0@0);
						sourceView.mouseDownAction.value(sourceView, 13, 13, 0, 0, 1);
						sourceView.mouseMoveAction.value(sourceView,
							dragPoint.x, dragPoint.y, 0);
						sourceView.mouseUpAction.value(sourceView,
							dragPoint.x, dragPoint.y, 0, 0);
						check.value(matrix.getPatch(\org2, \deckB_rate) == 0.5,
							"night endpoint drag auditions and plants WYHIWYG");
						gui.window.view.keyDownAction.value(gui.window.view,
							$z, 262144);
						check.value(matrix.getPatch(\org2, \deckB_rate) == 0.0,
							"window-level Ctrl+Z restores the night plant");
						// §4.1: the view flip is a normative audition revert path
						before = matrix.fullMatrix.asCompileString;
						triView = map.endpoints[\tri3][\view];
						target = map.endpoints[\coco1_spaf];
						movePoint = target[\view].mapToGlobal(target[\anchor])
							- triView.mapToGlobal(0@0);
						triView.mouseDownAction.value(triView, 13, 13, 0, 0, 1);
						triView.mouseMoveAction.value(triView,
							movePoint.x, movePoint.y, 0);
						check.value(gui.bananaGrammar.isInHand,
							"a night banana auditions in hand");
						gui.shell.toggleView(false);
						check.value(gui.bananaGrammar.isInHand.not
							and: { matrix.fullMatrix.asCompileString == before },
							"the view flip drops the hand and reverts — no leak");
						gui.shell.toggleView(false);
						AppClock.sched(0.15, {
							try {
								var records, mid, harvestPoints, localPoints;
								var beforeHarvest, ground;
								check.value(map.fireflyPhase == 0,
									"the river rests before any telemetry");
								NetAddr.localAddr.sendMsg(*auraMsg.value(0.5));
								AppClock.sched(0.1, {
									NetAddr.localAddr.sendMsg(*auraMsg.value(0.9));
									nil
								});
								AppClock.sched(0.25, {
									try {
										var phase = map.fireflyPhase;
										check.value(phase > 0,
											"moving aura frames carry the fireflies k -> k+1");
										NetAddr.localAddr.sendMsg(*auraMsg.value(0.9));
										AppClock.sched(0.15, {
											try {
												check.value(map.fireflyPhase == phase,
													"a still bay keeps a still river (early-out)");
												state.set(\coco1Hold, 0);
												state.set(\c2Range, \infra);
												NetAddr.localAddr.sendMsg('/cq_spesal',
													0, 0, 3, 0, 0, 0, 0);
												AppClock.sched(0.25, {
													try {
														check.value(map.holdStates[0] == false,
															"releasing hold unseals the spiral");
														check.value(map.rangeLetters[1] == "i",
															"the castle range letter follows its param");
														check.value(gui.spesalStates[0] == 3,
															"spesal OSC lights the night dial state");
														// harvest on night paper, one batch undo
														records = map.overlay.cableRecords;
														mid = records.first[\points]
															[records.first[\points].size div: 2];
														harvestPoints = [mid + (-20@(-20)),
															mid + (20@(-20)), mid + (20@20),
															mid + (-20@20)];
														ground = map.groundView;
														localPoints = harvestPoints.collect { |p|
															map.overlay.view.mapToGlobal(p)
																- ground.mapToGlobal(0@0)
														};
														beforeHarvest =
															matrix.fullMatrix.asCompileString;
														ground.mouseDownAction.value(ground,
															localPoints[0].x, localPoints[0].y,
															0, 1, 1);
														ground.mouseMoveAction.value(ground,
															localPoints[1].x, localPoints[1].y, 0);
														ground.mouseMoveAction.value(ground,
															localPoints[2].x, localPoints[2].y, 0);
														ground.mouseUpAction.value(ground,
															localPoints[3].x, localPoints[3].y,
															0, 1);
														check.value(matrix.fullMatrix
															.asCompileString != beforeHarvest,
															"a night lasso harvests its catch");
														gui.window.view.keyDownAction.value(
															gui.window.view, nil, 262144, 26, 0, 90);
														check.value(matrix.fullMatrix
															.asCompileString == beforeHarvest,
															"Ctrl+Z restores the whole night harvest");
														// ---- §6.1 night touch grammar ----
														block { |touchDone|
															var shell, coco, local, crescent;
															var speedBefore, freqBefore, chaosBefore;
															var samples, cablePoint, cableRecord;
															var castleTwo, castleView, dialPoint;
															var sparkPoint, holdBefore, flipBefore;
															shell = map.shellViews[0];
															coco = map.geometry[\cocos][0];
															crescent = map.crescentViews[\coco1Spaf];
															crescent.mouseDownAction.value(
																crescent, 12, 12, 0, 0, 1);
															crescent.mouseMoveAction.value(
																crescent, 12, -18, 0);
															crescent.mouseUpAction.value(
																crescent, 12, -18, 0, 0);
															check.value(
																(state.get(\coco1Spaf) - 2.0).abs < 0.02,
																"a tide-crescent pull pours spaf through the funnel");
															local = coco[\center]
																- coco[\shellBox].leftTop;
															shell.mouseDownAction.value(shell,
																local.x, local.y, 0, 0, 1);
															shell.mouseUpAction.value(shell,
																local.x, local.y, 0, 0);
															check.value(state.get(\coco1Hold) == 1,
																"pressing the shell's heart seals the loop (hold)");
															check.value(map.overlay
																.patchAt(coco[\chevron]).isNil,
																"precondition: no cable crosses the chevron home");
															local = coco[\chevron]
																- coco[\shellBox].leftTop;
															shell.mouseDownAction.value(shell,
																local.x, local.y, 0, 0, 1);
															shell.mouseUpAction.value(shell,
																local.x, local.y, 0, 0);
															check.value(state.get(\coco1Flip) == 1,
																"pressing the mouth chevron flips the read direction");
															speedBefore = state.get(\coco1Speed);
															samples = coco[\spiralSamples];
															local = coco[\shellBox].leftTop;
															shell.mouseDownAction.value(shell,
																samples[30].x - local.x,
																samples[30].y - local.y, 0, 0, 1);
															shell.mouseMoveAction.value(shell,
																samples[32].x - local.x,
																samples[32].y - local.y, 0);
															shell.mouseMoveAction.value(shell,
																samples[34].x - local.x,
																samples[34].y - local.y, 0);
															shell.mouseUpAction.value(shell,
																samples[34].x - local.x,
																samples[34].y - local.y, 0, 0);
															check.value(
																state.get(\coco1Speed) > speedBefore,
																"winding toward the mouth speeds the reel");
															check.value((map.speedNorms[0]
																- CQSpec.unmap(\coco1Speed,
																	state.get(\coco1Speed))).abs < 1e-6,
																"the reel pearl paints the funnel truth mid-wind");
															// static hover cues (§4.5: ink, never a pulse)
															shell.mouseOverAction.value(shell,
																samples[30].x - local.x,
																samples[30].y - local.y);
															check.value(map.shellHover[0],
																"hovering the spiral lights the wind cue");
															shell.mouseLeaveAction.value(shell);
															check.value(map.shellHover[0].not,
																"leaving the shell rests the cue");
															castleTwo = map.geometry[\castles][1];
															castleView = map.castleViews[1];
															dialPoint = CQNightGeometry.polar(
																castleTwo[\center], castleTwo[\dialR],
																0.25pi) - castleTwo[\box].leftTop;
															castleView.mouseOverAction.value(castleView,
																dialPoint.x, dialPoint.y);
															check.value(map.dialHover[1],
																"the inner clock lights under the pointer");
															castleView.mouseLeaveAction.value(castleView);
															check.value(map.dialHover[1].not,
																"the clock rests when the pointer leaves");
															freqBefore = state.get(\qFreq);
															chaosBefore = state.get(\qChaos);
															local = map.geometry[\eye]
																- map.geometry[\ringBox].leftTop;
															map.ringView.mouseDownAction.value(
																map.ringView, local.x, local.y, 0, 0, 1);
															map.ringView.mouseMoveAction.value(
																map.ringView, local.x + 30, local.y - 30, 0);
															map.ringView.mouseUpAction.value(
																map.ringView, local.x + 30, local.y - 30, 0, 0);
															check.value(
																state.get(\qFreq) > freqBefore
																and: { state.get(\qChaos) > chaosBefore },
																"the ring's eye drags pitch and chaos together");
															check.value(map.eyeView.notNil
																and: { map.eyeView.mouseDownAction.notNil },
																"the eye keeps its own opaque storm layer");
															check.value((map.eyeFreqNorm
																- CQSpec.unmap(\qFreq,
																	state.get(\qFreq))).abs < 1e-6
																and: { (map.eyeChaosNorm
																	- CQSpec.unmap(\qChaos,
																		state.get(\qChaos))).abs < 1e-6 },
																"the storm iris paints the funnel truth mid-drag");
															// ---- the read head: '/cq_phase' spark + skip tap ----
															check.value(map.sparkPoint(0).isNil,
																"before telemetry the shell keeps its reviewed ink (no spark)");
															gui.prPhaseFrame(1, 0.62);
															check.value((gui.phaseNorms[0] - 0.62).abs < 1e-9
																and: { map.sparkPoint(0).notNil },
																"a '/cq_phase' frame lands in the cache and births the spark");
															gui.prPhaseFrame(1, 2.5);
															check.value(gui.phaseNorms[0] == 1,
																"a mad phase clamps at the mouth (every entry clamps)");
															gui.prPhaseFrame(1, 0.62);
															sparkPoint = map.sparkPoint(0);
															speedBefore = state.get(\coco1Speed);
															holdBefore = state.get(\coco1Hold);
															flipBefore = state.get(\coco1Flip);
															local = sparkPoint - coco[\shellBox].leftTop;
															shell.mouseDownAction.value(shell,
																local.x, local.y, 0, 0, 1);
															shell.mouseMoveAction.value(shell,
																local.x + 2, local.y, 0);
															shell.mouseUpAction.value(shell,
																local.x + 2, local.y, 0, 0);
															check.value(state.get(\coco1Speed) == speedBefore
																and: { state.get(\coco1Hold) == holdBefore }
																and: { state.get(\coco1Flip) == flipBefore },
																"a tap on the read head never winds nor toggles (skip's home)");
															shell.mouseDownAction.value(shell,
																local.x, local.y, 0, 0, 1);
															local = CQNightGeometry.spiralPoint(coco, 0.66)
																- coco[\shellBox].leftTop;
															shell.mouseMoveAction.value(shell,
																local.x, local.y, 0);
															local = CQNightGeometry.spiralPoint(coco, 0.7)
																- coco[\shellBox].leftTop;
															shell.mouseMoveAction.value(shell,
																local.x, local.y, 0);
															shell.mouseUpAction.value(shell,
																local.x, local.y, 0, 0);
															check.value(state.get(\coco1Speed) > speedBefore,
																"a drag past the slop is a wind, never a skip");
															local = sparkPoint - coco[\shellBox].leftTop;
															shell.mouseOverAction.value(shell,
																local.x, local.y);
															check.value(map.sparkHover[0],
																"hovering the read head halos the comet");
															shell.mouseLeaveAction.value(shell);
															check.value(map.sparkHover[0].not,
																"leaving rests the comet's halo");
															gui.prPhaseFrame(1, 0.64);
															check.value(map.sparkDirs[0] == 1,
																"a forward frame turns the comet's tail forward");
															gui.prPhaseFrame(1, 0.6);
															check.value(map.sparkDirs[0] == -1,
																"the comet's tail reads the true travel direction");
															gui.prPhaseFrame(1, 0.02);
															check.value(map.sparkDirs[0] == 1,
																"a wrap past the mouth still reads as forward travel");
															cableRecord = map.overlay.cableRecords
																.detect { |record|
																record[\dest] == \coco1_spaf
															};
															cablePoint = cableRecord[\points]
																.detect { |point|
																coco[\shellBox].containsPoint(point)
															};
															speedBefore = state.get(\coco1Speed);
															local = cablePoint - coco[\shellBox].leftTop;
															shell.mouseDownAction.value(shell,
																local.x, local.y, 0, 0, 1);
															check.value(gui.bananaGrammar.isInHand
																and: { state.get(\coco1Speed)
																	== speedBefore },
																"a cable crossing the shell wins the press (4.5)");
															gui.bananaGrammar.escape;
															check.value(map.birdViews.every { |bird|
																bird.canReceiveDragHandler.notNil
															}, "both birds accept the file drop");
															// ---- §9 charge lamps ----
															check.value(gui.chargeFor(\coco1_flip).isNil,
																"before telemetry a lamp has no charge to draw");
															gui.prChargeFrame(1, [0.62, 0.0]);
															gui.prChargeFrame(3, [0, 0, 0.9, 0, 0]);
															check.value(
																(gui.chargeFor(\coco1_flip) - 0.62).abs < 1e-6
																and: { gui.chargeFor(\coco1_skip) == 0 }
																and: { (gui.chargeFor(\castle3_spesal) - 0.9)
																	.abs < 1e-6 },
																"night charge blocks land on their own lamps");
															check.value(gui.chargeFor(\coco2_flip).isNil,
																"a block that never reported stays unreported");
															// ---- the chevron reads the real travel ----
															// \cqCoco takes max(param, matrix gate), so a
															// patched flip reverses the loop without ever
															// touching the param — chevron and comet must
															// still agree (§16.6)
															state.set(\coco1Flip, 0);
															gui.prFlushParams;
															check.value(map.flipStates[0].not,
																"precondition: the FLIP param is released");
															gui.prPhaseFrame(1, 0.5);
															gui.prPhaseFrame(1, 0.56);
															check.value(map.flippedNow(0).not,
																"forward travel with a released param reads forward");
															gui.prPhaseFrame(1, 0.5);
															check.value(map.sparkDirs[0] == -1
																and: { map.flippedNow(0) },
																"a matrix flip gate turns the chevron though the param never moved");
															state.set(\coco1Flip, 1);
															gui.prFlushParams;
															gui.prPhaseFrame(1, 0.56);
															gui.prPhaseFrame(1, 0.62);
															check.value(map.sparkDirs[0] == 1
																and: { map.flippedNow(0) },
																"the param alone still turns the chevron (max, like the DSP)");
															// ---- §6 body solo-light ----
															castleView = map.castleViews[1];
															castleTwo = map.geometry[\castles][1];
															check.value(map.overlay.patchAt(
																castleTwo[\box].leftTop + (4@4)).isNil,
																"precondition: no cable crosses the castle's corner");
															castleView.mouseDownAction.value(castleView,
																4, 4, 0, 0, 2);
															check.value(
																gui.bananaGrammar.soloTarget.notNil
																and: { gui.bananaGrammar.soloTarget[\kind]
																	== \body }
																and: { gui.bananaGrammar.soloTarget[\name]
																	== \castle2 },
																"a double-click on a castle body solos that castle");
															check.value(gui.bananaGrammar
																.isCableDim(\envB, \castle3_fm),
																"an unrelated cable dims under the body solo");
															castleView.mouseDownAction.value(castleView,
																4, 4, 0, 0, 2);
															check.value(gui.bananaGrammar.soloTarget.isNil,
																"pressing the same body again releases the light");
															// the dial owns its area at every click count (§4.5)
															dialPoint = CQNightGeometry.polar(
																castleTwo[\center], castleTwo[\dialR],
																0.25pi) - castleTwo[\box].leftTop;
															castleView.mouseDownAction.value(castleView,
																dialPoint.x, dialPoint.y, 0, 0, 2);
															check.value(gui.bananaGrammar.soloTarget.isNil,
																"a fast second tap on the inner clock is a step, not a solo");
															// the body's own §4.5 hover echo, distinct from
															// the clock's: the hand must see the affordance
															local = castleTwo[\center]
																- castleTwo[\box].leftTop;
															castleView.mouseOverAction.value(castleView,
																local.x, local.y);
															check.value(map.bodyHover[1]
																and: { map.dialHover[1].not },
																"hovering a castle's flesh lights the body's press cue");
															castleView.mouseOverAction.value(castleView,
																dialPoint.x, dialPoint.y);
															check.value(map.dialHover[1]
																and: { map.bodyHover[1].not },
																"the clock's cue and the body's cue never light together");
															castleView.mouseLeaveAction.value(castleView);
															check.value(map.bodyHover[1].not
																and: { map.dialHover[1].not },
																"leaving the castle rests both cues");
															shell.mouseDownAction.value(shell, 3, 3, 0, 0, 2);
															check.value(
																gui.bananaGrammar.soloTarget.notNil
																and: { gui.bananaGrammar.soloTarget[\name]
																	== \coco1 },
																"shell paper carries the coco body solo");
															gui.bananaGrammar.escape;
															local = map.geometry[\ringBox].leftTop;
															map.ringView.mouseDownAction.value(
																map.ringView, 6, 6, 0, 0, 2);
															check.value(
																gui.bananaGrammar.soloTarget.notNil
																and: { gui.bananaGrammar.soloTarget[\name]
																	== \quantussy },
																"the ring's water solos the whole Quantussy body");
															gui.bananaGrammar.escape;
															touchDone.value;
														};
														check.value(CQInkControl.drawErrors == 0,
															"night surfaces paint without callback errors");
														gui.free;
														AppClock.sched(0.25, {
															try {
																check.value(gui.shellState
																	.lifecycleState == \closed,
																	"GUI free reaches closed");
																#[cq_gui_meters, cq_gui_gr,
																	cq_gui_coco, cq_gui_castle,
																	cq_gui_spesal, cq_gui_hot,
																	cq_gui_aura, cq_gui_phase,
																	cq_gui_charge].do { |key|
																	check.value(OSCdef.all[key].isNil,
																		"cleanup removed %".format(key));
																};
																if(ok) {
																	"CQ_GUI_NIGHT_PASSED".postln;
																	0.exit
																} {
																	"CQ_GUI_NIGHT_FAILED".postln;
																	1.exit
																};
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
			} { |error| abort.value(error) };
			nil
		});
	} { |error|
		abort.value(error);
	};
}.defer;
)
