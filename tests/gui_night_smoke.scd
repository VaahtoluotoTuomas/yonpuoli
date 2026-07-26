// G2 Qt smoke: construct the real shell, cross the hard cut into the Night
// map, and assert the archipelago carries the full banana grammar — endpoints,
// plant, undo, harvest, the §4.1 view-flip audition revert — plus the
// telemetry laws (fireflies ride only moving aura frames; no telemetry = the
// static reviewed ink). Prints CQ_GUI_NIGHT_PASSED / CQ_GUI_NIGHT_FAILED.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
// the §7.1 compass memories go to a gitignored tmp home so a smoke stamp can
// never overwrite the player's own corners (config gui.compassDir)
var compassDir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "cqcompass_smoke";
var config;
var gui, ok = true, abort, compassChecks, weatherAsync, punchChecks;
var check = { |condition, message|
	if(condition) { ("ok:   " ++ message).postln } {
		ok = false;
		("FAIL: " ++ message).postln;
	};
};
var auraMsg = { |casValue|
	['/cq_aura', 0, 0, 0.2, 0, 0, 0, 0, casValue, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
};
(compassDir +/+ "*").pathMatch.do { |p| File.delete(p) };
config = CQConfig.validate((headless: true, gui: (compassDir: compassDir)));
CQInkControl.resetDrawErrors;
// a hung continuation must still exit: sclang's stdout is block-buffered when
// redirected, so only an exit flushes the evidence of how far the smoke got
SystemClock.sched(90, {
	"FAIL: smoke watchdog — a continuation never completed".postln;
	"CQ_GUI_NIGHT_FAILED".postln;
	1.exit;
});
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

// ---- §7.1 the compass water (G3): seeds, voyages, weather — the synchronous
// acts. Defined here at sane indentation; called from the night sequence.
compassChecks = { |map|
	var almanac = map.geometry[\almanac];
	var cView = map.compassView;
	var boxOrigin = almanac[\compassBox].leftTop;
	var pad = almanac[\compass];
	var at = { |stagePoint| stagePoint - boxOrigin };
	var press = { |stagePoint, button = 0|
		var p = at.value(stagePoint);
		cView.mouseDownAction.value(cView, p.x, p.y, 0, button, 1);
		cView.mouseUpAction.value(cView, p.x, p.y, 0, button);
	};
	var check = { |condition, message|
		if(condition) { ("ok:   " ++ message).postln } {
			ok = false;
			("FAIL: " ++ message).postln;
		};
	};
	var undoBefore, local, local2;
	var di, si, mark, xEnd, growWeight;
	var savedRand, expected, countBefore;
	check.value(cView.notNil
		and: { (cView.bounds.left - almanac[\compassBox].left).abs < 1 },
		"the compass water is laid out on its own layer");
	check.value(gui.compass.stampedMask == [false, false, false, false],
		"the pad opens with empty water in all four corners");
	// two different jungles into TL and TR
	matrix.load([[\tri1, \coco1_spaf, 0.55], [\envB, \castle3_fm, -0.45]]);
	press.value(almanac[\stamps][0]);
	check.value(gui.compass.stampedMask == [true, false, false, false],
		"a tap on a corner seed stamps the live matrix");
	check.value(File.exists(compassDir +/+ CQCompass.fileName),
		"a stamp auto-saves the memories file");
	matrix.load([[\cas2, \coco2_fbVca, 0.4]]);
	press.value(almanac[\stamps][1]);
	check.value(gui.compass.stampedMask == [true, true, false, false],
		"the second corner holds its own patch");
	// a tap on the pad is a jump: one committed step to that weather
	matrix.clearPatch;
	undoBefore = gui.bananaGrammar.undoCount;
	press.value(pad.leftTop);
	check.value(matrix.getPatch(\tri1, \coco1_spaf) == 0.55
		and: { matrix.getPatch(\envB, \castle3_fm) == -0.45 }
		and: { matrix.asArray.size == 2 },
		"a tap at a pure corner recalls that stamp bit-exact");
	check.value(gui.bananaGrammar.undoCount == (undoBefore + 1),
		"the jump to a place on the pad is one undo op");
	gui.window.view.keyDownAction.value(gui.window.view, nil, 262144, 26, 0, 90);
	check.value(matrix.asArray.size == 0,
		"Ctrl+Z returns the bay the jump replaced");
	// a voyage: pour along the pad, commit on release. The end position is
	// derived from the cell's own tide mark, so the assertion is
	// deterministic geography, not luck.
	di = CQMatrix.destIndex(\coco1_spaf);
	si = CQMatrix.srcIndex(\tri1);
	mark = CQCompass.tideMarks[di][si];
	xEnd = (1 - mark) * 0.5;
	undoBefore = gui.bananaGrammar.undoCount;
	local = at.value(Point(pad.left + (xEnd * pad.width), pad.top + 30));
	local2 = at.value(Point(pad.left + (xEnd * pad.width), pad.top + 2));
	cView.mouseDownAction.value(cView, local.x, local.y, 0, 0, 1);
	cView.mouseMoveAction.value(cView, local2.x, local2.y, 0);
	check.value(map.overlay.stormMode,
		"a pour holds the night overlay in storm form");
	check.value(map.compassDragState.notNil,
		"the voyage paints its cross and its pulls");
	cView.mouseUpAction.value(cView, local2.x, local2.y, 0, 0);
	check.value(map.overlay.stormMode.not,
		"release returns the bay to full ink");
	check.value(map.compassDragState.isNil, "and takes the cross with it");
	check.value((matrix.getPatch(\tri1, \coco1_spaf) - 0.55).abs < 1e-9,
		"a cable standing above its tide mark keeps its stamped weight");
	check.value(gui.bananaGrammar.undoCount == (undoBefore + 1),
		"the whole voyage is one undo op");
	// grow: the first raindrop lands on the press itself
	matrix.clearPatch;
	undoBefore = gui.bananaGrammar.undoCount;
	local = at.value(almanac[\weather][\grow]);
	cView.mouseDownAction.value(cView, local.x, local.y, 0, 0, 1);
	check.value(matrix.asArray.size == 1,
		"grow plants one banana on the press itself");
	growWeight = matrix.asArray.first[2];
	check.value(growWeight.abs >= 0.15 and: { growWeight.abs <= 0.4 },
		"the raindrop weighs §7.2's ±U(0.15, 0.4)");
	cView.mouseUpAction.value(cView, local.x, local.y, 0, 0);
	check.value(gui.bananaGrammar.undoCount == (undoBefore + 1),
		"one rain hold is one undo op");
	// wither thins what stands
	undoBefore = gui.bananaGrammar.undoCount;
	local = at.value(almanac[\weather][\wither]);
	cView.mouseDownAction.value(cView, local.x, local.y, 0, 0, 1);
	cView.mouseUpAction.value(cView, local.x, local.y, 0, 0);
	check.value(matrix.asArray.size == 1
		and: { (matrix.asArray.first[2].abs - (growWeight.abs * 0.7)).abs < 1e-9 },
		"the sun decays its pick ×0.7");
	check.value(gui.bananaGrammar.undoCount == (undoBefore + 1),
		"one drought hold is one undo op");
	// breed: arm the graft, pick two parents — seeded here so the offspring
	// is checkable bit-exact against §7.3's own math
	savedRand = thisThread.randData;
	thisThread.randSeed = 777;
	expected = CQCompass.breed(gui.compass.corner(0), gui.compass.corner(1));
	undoBefore = gui.bananaGrammar.undoCount;
	press.value(almanac[\weather][\breed]);
	check.value(map.breedArmed, "pressing the graft arms the breeding");
	press.value(almanac[\stamps][0]);
	check.value(map.breedFirst == 0, "the first parent is chosen and held");
	thisThread.randSeed = 777;
	press.value(almanac[\stamps][1]);
	thisThread.randData = savedRand;
	check.value(map.breedArmed.not and: { map.breedFirst.isNil },
		"the second parent completes the graft and disarms it");
	check.value(matrix.fullMatrix == expected,
		"the offspring becomes the bay, exactly as §7.3 breeds it");
	check.value(gui.bananaGrammar.undoCount == (undoBefore + 1),
		"one breeding is one undo op");
	// Esc forgets an armed graft (cancelWeather)
	press.value(almanac[\weather][\breed]);
	check.value(map.breedArmed, "the graft re-arms");
	gui.window.view.keyDownAction.value(gui.window.view, 27.asAscii, 0);
	check.value(map.breedArmed.not, "Esc forgets the armed graft");
	// a right press on a seed clears that memory, and it stays cleared on disk
	press.value(almanac[\stamps][1], 1);
	check.value(gui.compass.stampedMask == [true, false, false, false],
		"a right press on a seed clears its memory");
	check.value(CQCompass(compassDir).stampedMask
		== [true, false, false, false],
		"stamps and clears survive on disk (versioned file)");
	// the water around the marks acts on nothing
	countBefore = matrix.asArray.size;
	press.value(Point(pad.left + (pad.width * 0.05),
		almanac[\compassBox].bottom - 2));
	check.value(matrix.asArray.size == countBefore
		and: { gui.bananaGrammar.isInHand.not },
		"the open water around the marks acts on nothing");
};

// ---- the elastic and the offscreen cache need real Qt ticks, so they run as
// their own async step just before teardown; `done` continues the sequence.
weatherAsync = { |map, done|
	var almanac = map.geometry[\almanac];
	var cView = map.compassView;
	var pad = almanac[\compass];
	var boxOrigin = almanac[\compassBox].leftTop;
	var check = { |condition, message|
		if(condition) { ("ok:   " ++ message).postln } {
			ok = false;
			("FAIL: " ++ message).postln;
		};
	};
	var beforeString, undoBefore, local, local2;
	matrix.load([[\tri1, \coco1_spaf, 0.55]]);
	beforeString = matrix.fullMatrix.asCompileString;
	undoBefore = gui.bananaGrammar.undoCount;
	local = Point(pad.center.x, pad.top + 30) - boxOrigin;
	local2 = Point(pad.center.x, pad.bottom - 10) - boxOrigin;
	cView.mouseDownAction.value(cView, local.x, local.y, 0, 1, 1);
	cView.mouseMoveAction.value(cView, local2.x, local2.y, 0);
	check.value(map.overlay.stormMode, "the elastic pours in storm form too");
	cView.mouseUpAction.value(cView, local2.x, local2.y, 0, 1);
	AppClock.sched(0.3, {
		try {
			check.value(matrix.fullMatrix.asCompileString == beforeString,
				"the elastic snaps the whole bay home");
			check.value(gui.bananaGrammar.undoCount == undoBefore,
				"and never enters the undo ring");
			check.value(map.overlay.stormMode.not,
				"the storm lifts with the hand");
			check.value(map.overlay.renderCount > 0,
				"the cable layer actually renders through the offscreen image");
			map.overlay.view.refresh;
			AppClock.sched(0.12, {
				try {
					var renders = map.overlay.renderCount;
					var draws = map.overlay.drawCount;
					map.overlay.view.refresh;
					AppClock.sched(0.12, {
						try {
							check.value(map.overlay.drawCount > draws,
								"a damage repaint still paints");
							check.value(map.overlay.renderCount == renders,
								"but unchanged ink is a blit, not a re-stroke");
							done.value;
						} { |error| abort.value(error) };
						nil
					});
				} { |error| abort.value(error) };
				nil
			});
		} { |error| abort.value(error) };
		nil
	});
};

// ---- §2 the punch (momentary hold): a right press held on the shell's heart
// inverts cocoNHold, restored on mouse-up and on every exit path. Synchronous
// (state.set is the funnel; the shell repaints from the already-registered hold
// updater), night-view, no cable dependency. Called from the §6.1 touch block;
// Tab and the free/close/CmdPeriod path are exercised in the teardown tail,
// where a view flip and a real gui.free are affordable.
punchChecks = { |map|
	var shell = map.shellViews[0];
	var coco = map.geometry[\cocos][0];
	var touch = map.touch;
	var local = coco[\center] - coco[\shellBox].leftTop;
	var before;
	var punch = { shell.mouseDownAction.value(shell, local.x, local.y, 0, 1, 1) };
	var release = { shell.mouseUpAction.value(shell, local.x, local.y, 0, 1) };
	// a right press held on the heart inverts the hold from its power-on seal
	state.set(\coco1Hold, 1);
	before = state.get(\coco1Hold);
	punch.value;
	check.value(state.get(\coco1Hold) == (1 - before)
		and: { touch.punchActive },
		"a right press on the heart punches — the hold inverts while held");
	// mouse-up is the natural release: it restores the exact prior value
	release.value;
	check.value(state.get(\coco1Hold) == before
		and: { touch.punchActive.not },
		"releasing the punch restores the hold to its pre-punch value");
	// the same invert works from the open state (crack -> seal -> crack)
	state.set(\coco1Hold, 0);
	before = state.get(\coco1Hold);
	punch.value;
	check.value(state.get(\coco1Hold) == 1 and: { touch.punchActive },
		"the punch seals an open loop while held");
	release.value;
	check.value(state.get(\coco1Hold) == 0 and: { touch.punchActive.not },
		"and gives the open loop back on release");
	// ---- exit paths: a stuck punch must never be left recording (§2) ----
	// Esc (CQGui.prKey ascii 27 -> cancelGestures)
	state.set(\coco1Hold, 1);
	before = state.get(\coco1Hold);
	punch.value;
	check.value(state.get(\coco1Hold) == 0 and: { touch.punchActive },
		"precondition: the Esc test starts punched");
	gui.window.view.keyDownAction.value(gui.window.view, 27.asAscii, 0);
	check.value(state.get(\coco1Hold) == before and: { touch.punchActive.not },
		"Esc restores a held punch");
	// panic: the exact path CQ.panic runs on the gui panel (cancelAudition)
	punch.value;
	check.value(state.get(\coco1Hold) == 0 and: { touch.punchActive },
		"precondition: the panic test starts punched");
	gui.cancelAudition;
	check.value(state.get(\coco1Hold) == before and: { touch.punchActive.not },
		"panic (CQGui.cancelAudition) restores a held punch");
	// §4.5: a right press on a cable crossing the shell rips, never punches — the
	// cable check precedes the heart in shellDown. Uses a live cable point inside
	// the shell box (the coco1_spaf cable is still planted at this point).
	block { |cableDone|
		var record = map.overlay.cableRecords.detect { |r| r[\dest] == \coco1_spaf };
		var cablePoint = record !? {
			record[\points].detect { |p| coco[\shellBox].containsPoint(p) }
		};
		var cLocal, holdWas;
		if(cablePoint.isNil) {
			check.value(false,
				"precondition: a coco1_spaf cable point lies inside the shell box");
			cableDone.value;
		} {
			holdWas = state.get(\coco1Hold);
			cLocal = cablePoint - coco[\shellBox].leftTop;
			shell.mouseDownAction.value(shell, cLocal.x, cLocal.y, 0, 1, 1);
			check.value(touch.punchActive.not
				and: { state.get(\coco1Hold) == holdWas },
				"a right press on a cable crossing the shell rips, it does not punch");
			gui.bananaGrammar.escape;
			shell.mouseUpAction.value(shell, cLocal.x, cLocal.y, 0, 1);
			cableDone.value;
		};
	};
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
						var geo = map.geometry, expected, view, anchor;
						var sourceView, destView, destPoint, dragPoint, sourceAnchor;
						var before, triView, target, movePoint, triAnchor;
						check.value(geo.notNil, "night geometry is laid out");
						expected = geo[\endpoints][\tri1];
						view = map.endpoints[\tri1][\view];
						// the anchor is read, never assumed: a flooded view size
						// must move this assert's expectation with it (rake 11)
						anchor = map.endpoints[\tri1][\anchor];
						check.value(
							(view.bounds.left - (expected.x - anchor.x)).abs < 1
							and: { (view.bounds.top - (expected.y - anchor.y)).abs < 1 },
							"a castle leaf jack sits exactly on its computed home");
						check.value(map.overlay.cableCount == 2,
							"night overlay renders every planted matrix cable");
						sourceView = map.endpoints[\org2][\view];
						sourceAnchor = map.endpoints[\org2][\anchor];
						destView = map.endpoints[\deckB_rate][\view];
						destPoint = destView.mapToGlobal(
							map.endpoints[\deckB_rate][\anchor]);
						dragPoint = destPoint - sourceView.mapToGlobal(0@0);
						sourceView.mouseDownAction.value(sourceView,
							sourceAnchor.x, sourceAnchor.y, 0, 0, 1);
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
						triAnchor = map.endpoints[\tri3][\anchor];
						target = map.endpoints[\coco1_spaf];
						movePoint = target[\view].mapToGlobal(target[\anchor])
							- triView.mapToGlobal(0@0);
						triView.mouseDownAction.value(triView,
							triAnchor.x, triAnchor.y, 0, 0, 1);
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
															var sparkPoint, holdBefore, flipBefore, blinkBefore;
															var savedPatches;
																// the crescent view's own centre, so a flooded
																// view keeps this exactly a 30 px pull (rake 11)
																var mid = CQNightMeasures.crescentExtent * 0.5;
															shell = map.shellViews[0];
															coco = map.geometry[\cocos][0];
															crescent = map.crescentViews[\coco1Spaf];
															crescent.mouseDownAction.value(
																crescent, mid, mid, 0, 0, 1);
															crescent.mouseMoveAction.value(
																crescent, mid, mid - 30, 0);
															crescent.mouseUpAction.value(
																crescent, mid, mid - 30, 0, 0);
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
															// ---- the read head: '/cq_phase' spark, display only ----
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
															// the comet stopped being a target at the G2 gate: pressing
															// it must now fall through to the winding surface it rides
															local = sparkPoint - coco[\shellBox].leftTop;
															shell.mouseDownAction.value(shell,
																local.x, local.y, 0, 0, 1);
															shell.mouseUpAction.value(shell,
																local.x, local.y, 0, 0);
															check.value(state.get(\coco1Hold) == holdBefore
																and: { state.get(\coco1Flip) == flipBefore },
																"a press on the read head toggles nothing — it is display only");
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
																"the comet's home is winding surface like any other");
															// ---- skip's stationary mark above the mouth jack ----
															// §4.5 gives a cable under the pointer the
															// press, and after the spring flood the
															// baseline tri1 -> coco1_spaf cable sags
															// straight across this mark (2.5 px, measured;
															// 17.5 px before the flood — recorded for the
															// §8 composition review). That is the grammar
															// working, not a defect, but it is not what
															// this block tests: the mark is exercised on
															// its own free water and the patch goes back
															// afterwards. `invalidate` is called by hand
															// because the overlay's own listener defers,
															// and this block is already inside AppClock.
															savedPatches = matrix.asArray;
															matrix.load([]);
															map.overlay.invalidate;
															local = coco[\skipMark] - coco[\shellBox].leftTop;
															speedBefore = state.get(\coco1Speed);
															shell.mouseOverAction.value(shell,
																local.x, local.y);
															check.value(map.skipHover[0],
																"hovering the skip mark lights it");
															check.value(map.rimHover[0].not,
																"the mark's cue wins over the rim it hangs beside");
															shell.mouseDownAction.value(shell,
																local.x, local.y, 0, 0, 1);
															shell.mouseUpAction.value(shell,
																local.x, local.y, 0, 0);
															check.value(state.get(\coco1Speed) == speedBefore
																and: { state.get(\coco1Hold) == holdBefore }
																and: { state.get(\coco1Flip) == flipBefore },
																"pressing skip winds nothing and toggles nothing");
															map.flashSkip(1);
															check.value(map.skipFlash[0],
																"any skip flashes the mouth mark (§16.7 momentary)");
															shell.mouseLeaveAction.value(shell);
															check.value(map.skipHover[0].not,
																"leaving rests the mark");
															matrix.load(savedPatches);
															map.overlay.invalidate;
															check.value(map.overlay.cableRecords.any { |r|
																r[\dest] == \coco1_spaf },
																"the mark's block put the patch back");
															check.value(map.struckOn[0].not,
																"a quiet skip leaves the comet a plain point");
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
															// ---- §16.7: the hovered bird wears the border ----
															check.value(gui.dropTarget.isNil,
																"no drag, no dashed border anywhere");
															View.prSetCurrentDrag("amen.wav");
															map.birdViews[0].canReceiveDragHandler
																.value(map.birdViews[0]);
															check.value(gui.dropTargetIs(
																CQGuiNightMap.dropTargetName(\deckA)),
																"a hovering file marks the bird under it");
															map.birdViews[1].canReceiveDragHandler
																.value(map.birdViews[1]);
															check.value(gui.dropTargetIs(
																CQGuiNightMap.dropTargetName(\deckB))
																and: { gui.dropTargetIs(CQGuiNightMap
																	.dropTargetName(\deckA)).not },
																"only ever one target is marked");
															gui.dropTargetClear;
															map.endpoints[\deckA_rate][\view]
																.canReceiveDragHandler.value;
															check.value(gui.dropTargetIs(
																CQGuiNightMap.dropTargetName(\deckA)),
																"the wing socket marks the bird, not itself");
															map.birdViews[0].receiveDragHandler.value;
															check.value(gui.dropTarget.isNil,
																"the drop clears the border at once");
															View.prClearCurrentDrag;
															map.birdViews[0].canReceiveDragHandler
																.value(map.birdViews[0]);
															check.value(gui.dropTarget.isNil,
																"something that is not a file marks nothing");
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
															// ---- the comet's regime: a hammered loop never
															// leaves the eye, and a bead crawling near zero
															// would read as "almost stopped" — the opposite
															// of the truth. The peak-held skip charge says so
															// already, so no new telemetry and no timer.
															// The mark is STATE, not extent: the reach out of
															// the eye collapses to nothing exactly when the
															// regime is clearest (G2 gate, 2026-07-22).
															gui.prChargeFrame(1, [0.62, 1.0]);
															gui.prPhaseFrame(1, 0.04, 1, 12);
															check.value(map.struckOn[0].not,
																"one lit frame is a skip, not yet a regime");
															gui.prPhaseFrame(1, 0.03, 1, 12);
															check.value(map.struckOn[0],
																"two lit frames running mark the head struck");
															check.value(map.struckOn[1].not,
																"the other shell's head is left alone");
															// the rate is worn as weight, never as motion
															check.value(map.struckNorms[0] > 0
																and: { map.struckNorms[0] < 0.5 },
																"a countable rate wears a light mark");
															gui.prPhaseFrame(1, 0.03, 1, 1500);
															check.value(map.struckNorms[0] > 0.9,
																"an audio-rate hammering wears the heaviest mark");
															// the twinkle rides the frames, so a stalled
															// server freezes the mark instead of lying
															blinkBefore = map.struckBlink[0];
															gui.prPhaseFrame(1, 0.03, 1, 1500);
															check.value(map.struckBlink[0] != blinkBefore,
																"every telemetry frame twinkles the struck mark");
															gui.prPhaseFrame(1, 0.03, 1, 1500);
															check.value(map.struckBlink[0] == blinkBefore,
																"the twinkle alternates, it does not drift");
															gui.prChargeFrame(1, [0.62, 0]);
															gui.prPhaseFrame(1, 0.5, 1, 0);
															check.value(map.struckOn[0].not
																and: { map.struckBlink[0].not },
																"a released skip returns the comet to a still point");
															// ---- the chevron reads the real travel ----
															// \cqCoco takes max(param, matrix gate), so a
															// patched flip reverses the loop without ever
															// touching the param — chevron and comet must
															// still agree (§16.6)
															state.set(\coco1Flip, 0);
															gui.prFlushParams;
															check.value(map.flipStates[0].not,
																"precondition: the FLIP param is released");
															gui.prPhaseFrame(1, 0.5, 1.0);
															gui.prPhaseFrame(1, 0.56, 1.0);
															check.value(map.flipMode(0) == \forward
																and: { map.sparkDirs[0] == 1 },
																"a settled forward loop reads forward");
															gui.prPhaseFrame(1, 0.5, -1.0);
															check.value(map.flipMode(0) == \flipped
																and: { map.sparkDirs[0] == -1 },
																"a matrix flip gate turns the chevron though the param never moved");
															// the answer to a gate too fast to draw as an
															// arrow: show the regime, not the value
															gui.prPhaseFrame(1, 0.5, 0.1);
															check.value(map.flipMode(0) == \shuttle,
																"a shuttled loop reads as the opposed chevron, never a frozen arrow");
															gui.prPhaseFrame(1, 0.5, -0.2);
															check.value(map.flipMode(0) == \shuttle
																and: { map.sparkDirs[0] == -1 },
																"the tail keeps its last settled direction while shuttling");
															state.set(\coco1Flip, 1);
															gui.prFlushParams;
															gui.prPhaseFrame(1, 0.56, 0.0);
															check.value(map.flipMode(0) == \flipped,
																"the param alone still turns the chevron (max, like the DSP)");
															// ---- §6 body solo-light ----
															castleView = map.castleViews[1];
															castleTwo = map.geometry[\castles][1];
															local = castleTwo[\center]
																- castleTwo[\box].leftTop;
															check.value(map.overlay.patchAt(
																castleTwo[\center]).isNil,
																"precondition: no cable crosses the castle's flesh");
															castleView.mouseDownAction.value(castleView,
																4, 4, 0, 0, 2);
															check.value(gui.bananaGrammar.soloTarget.isNil,
																"the layer box's corner is water: no solo where no cue promised one");
															castleView.mouseDownAction.value(castleView,
																local.x, local.y, 0, 0, 2);
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
																local.x, local.y, 0, 0, 2);
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
															// ---- §16.6 the spesal coupling chords ----
															// structure, so it is drawn only where the hand
															// asks: nothing at rest, one chord under a
															// castle, all five under the river
															check.value(map.chordCastles.isEmpty,
																"a resting bay draws no coupling at all");
															castleView.mouseOverAction.value(castleView,
																local.x, local.y);
															block { |chordDone|
																var chords = map.spesalChords, path;
																var state, partner, nearest = 0;
																check.value(map.chordCastles == [1]
																	and: { chords.size == 1 },
																	"hovering a castle draws its own coupling, and only its own");
																path = chords[0][\path];
																state = gui.spesalStates[1].asInteger;
																partner = CQGuiCastleConstellation
																	.partnerIndex(1, state);
																(0..4).do { |k|
																	if(CQNightGeometry.distance(path[0],
																		map.geometry[\castles][k][\center])
																		< CQNightGeometry.distance(path[0],
																			map.geometry[\castles][nearest]
																				[\center])) { nearest = k };
																};
																check.value(nearest == partner
																	and: { chords[0][\track]
																		== CQGuiCastleConstellation
																			.trackMode(state) },
																	"the chord starts at the partner the Day constellation names, in its mode");
																// and it runs the way the signal runs:
																// the partner is sampled INTO this castle
																nearest = 0;
																(0..4).do { |k|
																	if(CQNightGeometry.distance(path[3],
																		map.geometry[\castles][k][\center])
																		< CQNightGeometry.distance(path[3],
																			map.geometry[\castles][nearest]
																				[\center])) { nearest = k };
																};
																check.value(nearest == 1,
																	"and it ends on the castle doing the reading — the signal's own direction");
																check.value(CQNightGeometry.distance(
																	CQNightGeometry.chordPoint(path, 0),
																	path[0]) < 0.001
																	and: { CQNightGeometry.distance(
																		CQNightGeometry.chordPoint(path, 1),
																		path[3]) < 0.001 },
																	"the travelling light starts at the source and arrives at the reader");
																check.value(CQNightGeometry.distance(path[1],
																	map.geometry[\eye])
																	>= ((map.geometry[\ringR]
																		* CQNightGeometry.chordClearance)
																		- 0.001),
																	"and it bows inward without ever crowding the storm iris");
																chordDone.value;
															};
															castleView.mouseLeaveAction.value(castleView);
															check.value(map.chordCastles.isEmpty,
																"leaving the castle rests its coupling too");
															// the shell is taken by its rim, and the rim
															// says so under the pointer (§16.6)
															shell.mouseDownAction.value(shell, 3, 3, 0, 0, 2);
															check.value(gui.bananaGrammar.soloTarget.isNil,
																"the shell box's corner is open water: no solo, no cue");
															// opposite the mouth, where the outermost
															// turn sits well inside the rim: nearer the
															// mouth the winding surface rightly wins (§4.5)
															sparkPoint = CQNightGeometry.polar(coco[\center],
																coco[\outerR], pi);
															check.value(map.overlay.patchAt(sparkPoint).isNil
																and: { CQNightGeometry.spiralDistance(coco,
																	sparkPoint) > 8 },
																"precondition: the far rim is neither cable nor winding surface");
															local = sparkPoint - coco[\shellBox].leftTop;
															shell.mouseOverAction.value(shell,
																local.x, local.y);
															check.value(map.rimHover[0]
																and: { map.shellHover[0].not },
																"hovering the shell's rim draws the outline the press acts on");
															shell.mouseDownAction.value(shell,
																local.x, local.y, 0, 0, 2);
															check.value(
																gui.bananaGrammar.soloTarget.notNil
																and: { gui.bananaGrammar.soloTarget[\name]
																	== \coco1 },
																"a double-click on the rim solos the whole shell");
															gui.bananaGrammar.escape;
															shell.mouseLeaveAction.value(shell);
															check.value(map.rimHover[0].not,
																"leaving rests the rim cue");
															// the ring is taken by its own line
															local = map.geometry[\ringSamples][7]
																- map.geometry[\ringBox].leftTop;
															map.ringView.mouseDownAction.value(
																map.ringView, 6, 6, 0, 0, 2);
															check.value(gui.bananaGrammar.soloTarget.isNil,
																"still water inside the ring is not the ring's body");
															map.ringView.mouseOverAction.value(
																map.ringView, local.x, local.y);
															check.value(map.ringHover,
																"hovering the river brightens the line you can take");
															check.value(map.chordCastles.size == 5
																and: { map.spesalChords.size == 5 },
																"and asks the whole ring for its coupling at once");
															map.ringView.mouseDownAction.value(
																map.ringView, local.x, local.y, 0, 0, 2);
															check.value(
																gui.bananaGrammar.soloTarget.notNil
																and: { gui.bananaGrammar.soloTarget[\name]
																	== \quantussy },
																"a double-click on the river solos the whole Quantussy body");
															gui.bananaGrammar.escape;
															map.ringView.mouseLeaveAction.value(map.ringView);
															check.value(map.ringHover.not,
																"leaving rests the river's cue");
															// ---- §2 the punch (the heart), while cables are live ----
															punchChecks.value(map);
															touchDone.value;
														};
														// ---- §6.1/§16.6 the almanac corner ----
														// the sowing moon: tap = throw, horizontal drag =
														// pour the phase. The spy counts grammar notices,
														// which is exactly §4.3's line: a throw is a matrix
														// op, a poured phase is a GUI constant and touches
														// nothing undoable.
														block { |almanacDone|
															var almanac, local, before, amount;
															var notices = 0, spy, press, view;
															almanac = map.geometry[\almanac];
															view = map.almanacView;
															spy = SimpleController(gui.bananaGrammar);
															spy.put(\gesture, { notices = notices + 1 });
															press = { |point, travel|
																var from = point - almanac[\box].leftTop;
																var to = from + (travel ?? { 0@0 });
																view.mouseDownAction.value(view,
																	from.x, from.y, 0, 0, 1);
																travel !? {
																	view.mouseMoveAction.value(view,
																		to.x, to.y, 0)
																};
																view.mouseUpAction.value(view,
																	to.x, to.y, 0, 0);
															};
															check.value(almanac[\stars].isNil,
																"the sky carries the moon alone — no preset stars");
															check.value(map.shakeAmount == gui.shakeAmount,
																"the moon wears the one shake amount, the Day knob's own number");
															// the moon's own drawn rim is its cue (§4.5)
															local = almanac[\moon] - almanac[\box].leftTop;
															view.mouseOverAction.value(view, local.x, local.y);
															check.value(map.moonHover,
																"hovering the moon lights the rim the press acts on");
															view.mouseLeaveAction.value(view);
															check.value(map.moonHover.not,
																"leaving the corner rests the cue");
															// a tap throws at the phase the moon shows
															amount = gui.shakeAmount;
															notices = 0;
															press.value(almanac[\moon]);
															check.value(notices > 0
																and: { gui.shakeAmount == amount },
																"a tap on the moon throws a shake and never nudges the phase it threw");
															// pouring is not an op (§4.3): no undo, no throw
															before = matrix.fullMatrix.asCompileString;
															notices = 0;
															press.value(almanac[\moon], 60 @ 0);
															check.value(gui.shakeAmount > amount
																and: { notices == 0 }
																and: { matrix.fullMatrix.asCompileString
																	== before },
																"dragging right pours the moon fuller — no throw, no undo op");
															press.value(almanac[\moon], -200 @ 0);
															check.value(gui.shakeAmount == 0,
																"pouring past empty clamps at the new moon (every entry clamps)");
															notices = 0;
															press.value(almanac[\moon], 0 @ 40);
															check.value(notices == 0
																and: { gui.shakeAmount == 0 },
																"a press that travelled vertically is a drag, never a throw");
															gui.shakeAmount = 0.5;
															check.value(map.shakeAmount == 0.5,
																"the moon reads the amount back from its one owner");
															// the rest of the corner is open water
															amount = gui.shakeAmount;
															before = matrix.fullMatrix.asCompileString;
															press.value(almanac[\box].leftTop + Point(6, 6));
															check.value(gui.shakeAmount == amount
																and: { matrix.fullMatrix.asCompileString
																	== before }
																and: { gui.bananaGrammar.isInHand.not },
																"the corner's empty water throws nothing");
															// the phase governs how MUCH is sown (SPEC §5.5)
															check.value(CQGui.sowDensity(0) == 0
																and: { CQGui.sowDensity(0.5)
																	== CQMatrix.shakeDensity }
																and: { CQGui.sowDensity(1)
																	== CQGui.fullMoonDensity },
																"the half moon is exactly the v1.0 handful, the full one twice it");
															spy.remove;
															almanacDone.value;
														};
														compassChecks.value(map);
														// any shake, from any layer, flashes the moon —
														// through CQGui's own \action handler, so it lands
														// on the next GUI tick
														state.changed(\action, \shake, 0.4);
														AppClock.sched(0.05, {
														try {
														check.value(map.moonFlash,
															"a shake from any layer flashes the sowing moon");
														check.value(CQInkControl.drawErrors == 0,
															"night surfaces paint without callback errors");
														weatherAsync.value(map, {
														var hShell = map.shellViews[0];
														var hCoco = map.geometry[\cocos][0];
														var heartLocal = hCoco[\center] - hCoco[\shellBox].leftTop;
														var tabBefore, freeBefore;
														// Tab view flip is a punch exit path (§2): begin in night
														// (clean geometry), flip away, assert the loop was not
														// left inverted
														state.set(\coco1Hold, 1);
														tabBefore = state.get(\coco1Hold);
														hShell.mouseDownAction.value(hShell,
															heartLocal.x, heartLocal.y, 0, 1, 1);
														check.value(state.get(\coco1Hold) == 0
															and: { map.touch.punchActive },
															"precondition: a punch is held before the Tab flip");
														gui.window.view.keyDownAction.value(
															gui.window.view, 9.asAscii, 0);
														check.value(state.get(\coco1Hold) == tabBefore
															and: { map.touch.punchActive.not },
															"a Tab view flip restores a held punch");
														// window close / GUI free / CmdPeriod all reach
														// nightMap.free -> cancelGestures. Establish the active
														// punch directly (the press routing is proven above) so
														// the teardown restore needs no post-flip geometry.
														state.set(\coco1Hold, 1);
														freeBefore = state.get(\coco1Hold);
														map.touch.prPunchDown(0);
														check.value(state.get(\coco1Hold) == 0
															and: { map.touch.punchActive },
															"precondition: a punch is held before GUI free");
														gui.free;
														AppClock.sched(0.25, {
															try {
																check.value(gui.shellState
																	.lifecycleState == \closed,
																	"GUI free reaches closed");
																	check.value(state.get(\coco1Hold) == freeBefore,
																		"GUI free / window close / CmdPeriod restores a held punch");
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
			} { |error| abort.value(error) };
			nil
		});
	} { |error|
		abort.value(error);
	};
}.defer;
)
