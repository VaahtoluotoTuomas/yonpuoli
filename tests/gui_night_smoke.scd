// Night Qt smoke: construct the real Night-only shell and assert the
// archipelago carries the full banana grammar — endpoints,
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
var gui, ok = true, abort, compassChecks, weatherAsync, punchChecks, birdChecks;
var preampChecks;
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

// ---- §11 deck birds: native-dialog routing is injected because a headless
// smoke must never open an OS panel. The deck dispatcher is injected too, so
// the real view handlers prove which deck act they requested while unbooted.
birdChecks = { |map|
	var view = map.birdViews[0], bird = map.geometry[\birds][0];
	var local = bird[\center] - bird[\box].leftTop;
	var routes = List.new, dialogCount = 0, dialogPath;
	var before, cable, cablePoint, savedMatrix;
	var check = { |condition, message|
		if(condition) { ("ok:   " ++ message).postln } {
			ok = false;
			("FAIL: " ++ message).postln;
		};
	};
	map.deckAction = { |which, action, value|
		routes.add([which, action, value])
	};
	map.fileDialog = { |callback, path|
		dialogCount = dialogCount + 1;
		dialogPath = path;
		callback.value("stub-bird.wav")
	};
	view.mouseOverAction.value(view, local.x, local.y);
	check.value(map.birdHover[0],
		"hovering a clear bird brightens the act it promises");
	view.mouseDownAction.value(view, local.x, local.y, 0, 0, 1);
	view.mouseUpAction.value(view, local.x, local.y, 0, 0);
	check.value(dialogCount == 1
		and: { routes.last == [\deckA, \load, "stub-bird.wav"] },
		"a bird tap routes the async dialog result to its own deck");
	check.value(dialogPath.tr($\\, $/) ==
		(Cocoquantus.repoDir +/+ "samples").tr($\\, $/),
		"the native fetch dialog starts in the configured project samples folder");
	map.fileDialog = { |callback, path|
		dialogCount = dialogCount + 1;
		dialogPath = path
	};
	before = routes.size;
	view.mouseDownAction.value(view, local.x, local.y, 0, 0, 1);
	view.mouseUpAction.value(view, local.x, local.y, 0, 0);
	check.value(dialogCount == 2 and: { routes.size == before },
		"cancelling the file dialog is a harmless no-op");
	view.mouseWheelAction.value(view, local.x, local.y, 0, 0, -1);
	view.mouseWheelAction.value(view, local.x, local.y, 0, 0, -1);
	check.value(routes.last == [\deckA, \next, nil]
		and: { routes.size == (before + 1) },
		"wheel-down routes one next step and suppresses immediate momentum");
	before = routes.size;
	check.value(view.mouseDownAction.value(view,
			local.x, local.y, 0, 0, 1) == true
		and: { view.mouseMoveAction.value(view,
			local.x + 7, local.y, 0) == true }
		and: { view.mouseMoveAction.value(view,
			local.x, local.y, 0) == true }
		and: { view.mouseUpAction.value(view,
			local.x, local.y, 0, 0) == true },
		"the bird claims the complete Qt drag instead of leaking it to the stage");
	check.value(routes.size == (before + 1)
		and: { routes.last == [\deckA, \random, nil] },
		"one natural out-and-back ruffle routes random on release");
	before = routes.size;
	view.mouseDownAction.value(view, local.x, local.y, 0, 0, 1);
	view.mouseMoveAction.value(view, local.x + 10, local.y, 0);
	view.mouseUpAction.value(view, local.x + 10, local.y, 0, 0);
	check.value(routes.size == before,
		"a plain non-tap, non-ruffle drag does nothing");
	view.mouseDownAction.value(view, local.x, local.y, 0, 0, 1);
	check.value(map.touch.birdGestureActive,
		"a pressed bird owns transient gesture state");
	map.cancelGestures;
	view.mouseUpAction.value(view, local.x, local.y, 0, 0);
	check.value(map.touch.birdGestureActive.not and: { routes.size == before },
		"the shared exit choke point cancels a half-finished bird gesture");
	map.deckLoaded(\deckA);
	check.value(map.birdFlash[0] and: { map.birdFlash[1].not },
		"a successful deck notification flashes only its own bird");

	// Plant a deterministic cable into the wing socket and feed one of its
	// exact path points through the bird handler. (At this reviewed layout the
	// endpoint's higher sibling view owns the final 6 px below the bird, so a
	// literal on-screen overlap is not guaranteed.) The handler must still
	// preserve §4.5 if a future cable/layout crosses its paper.
	savedMatrix = matrix.asArray;
	matrix.patch(\tri1, \deckA_rate, 0.5);
	map.overlay.invalidate;
	cable = map.overlay.cableRecords.detect { |record|
		record[\src] == \tri1 and: { record[\dest] == \deckA_rate }
	};
	cablePoint = cable !? { |record| record[\points].at(record[\points].size div: 2) };
	check.value(cablePoint.notNil,
		"the bird priority fixture has an exact planted cable point");
	if(cablePoint.notNil) {
		local = cablePoint - bird[\box].leftTop;
		view.mouseOverAction.value(view, local.x, local.y);
		check.value(map.birdHover[0].not
			and: { gui.bananaGrammar.hoverCable.notNil },
			"a crossing cable takes hover before the bird");
		before = routes.size;
		view.mouseDownAction.value(view, local.x, local.y, 0, 0, 1);
		check.value(gui.bananaGrammar.isInHand and: { routes.size == before },
			"a crossing cable takes the press before fetch/ruffle");
		gui.bananaGrammar.escape;
	};
	matrix.load(savedMatrix);
	map.overlay.invalidate;
};

// ---- §10 intake roots: selectable provenance · drive/bias knot · audio body ----
preampChecks = { |map|
	var view = map.rootViews[0], root = map.geometry[\roots][0];
	var local = root[\knot] - root[\box].leftTop;
	var before, after, sourceIndex, sourcePoint, savedMatrix, cable, cablePoint;
	var check = { |condition, message|
		if(condition) { ("ok:   " ++ message).postln } {
			ok = false;
			("FAIL: " ++ message).postln;
		};
	};
	view.mouseOverAction.value(view, local.x, local.y);
	check.value(map.preampHover[0],
		"hovering the knot reveals the current preamp provenance");
	check.value(CQGuiNightMap.preampSourceIndex(0, state.get(\preASrc)) == 0,
		"the revealed live-l strand follows CQSpec's source order");
	before = state.get(\preADrive);
	view.mouseDownAction.value(view, local.x, local.y, 0, 0, 1);
	check.value(map.touch.preampGestureActive and: { map.preampActive[0] },
		"the preamp knot owns one bodily drive gesture");
	view.mouseMoveAction.value(view, local.x, local.y - 60, 0);
	after = state.get(\preADrive);
	check.value(after > before
		and: { map.preampDriveNorms[0]
			== CQSpec.unmap(\preADrive, after) },
		"pulling the knot upward raises exponential drive through CQState");
	view.mouseUpAction.value(view, local.x, local.y - 60, 0, 0);
	check.value(map.touch.preampGestureActive.not
		and: { map.preampActive[0].not },
		"release rests the knot without reverting its continuous setting");
	before = state.get(\preABias);
	view.mouseDownAction.value(view, local.x, local.y, 0, 0, 1);
	view.mouseMoveAction.value(view, local.x + 60, local.y + 2, 0);
	after = state.get(\preABias);
	view.mouseUpAction.value(view, local.x + 60, local.y + 2, 0, 0);
	check.value(after > before
		and: { map.preampBiasNorms[0] == CQSpec.unmap(\preABias, after) },
		"a rightward knot knead raises bias through the same state funnel");
	view.mouseDownAction.value(view, local.x, local.y, 0, 0, 1);
	map.cancelGestures;
	check.value(map.touch.preampGestureActive.not
		and: { map.preampActive[0].not },
		"the shared Esc/Tab/panic/free choke point releases a held drive knot");
	view.mouseLeaveAction.value(view);
	check.value(map.preampHover[0].not,
		"leaving the root hides its transient provenance");
	sourceIndex = root[\sourceValues].indexOf(\deckA);
	sourcePoint = CQNightGeometry.cubicPoint(
		root[\sourceStrands][sourceIndex], 0.12) - root[\box].leftTop;
	view.mouseOverAction.value(view, sourcePoint.x, sourcePoint.y);
	check.value(map.preampSourceHover[0] == sourceIndex,
		"hovering an intake strand reveals that candidate before selection");
	view.mouseDownAction.value(view, sourcePoint.x, sourcePoint.y, 0, 0, 1);
	check.value(state.get(\preASrc) == \deckA,
		"clicking a source strand selects its exact CQSpec enum value");
	view.mouseDownAction.value(view, sourcePoint.x, sourcePoint.y, 0, 0, 1);
	check.value(state.get(\preASrc) == \none,
		"clicking the selected strand again closes the preamp intake");
	state.set(\preASrc, \deckA);
	check.value(map.deckSlotNorms[0] > 0
		and: { map.preampSlotNorms[0] > 0 },
		"the route painting knows both default deck and preamp slot gains");

	// As with the bird fixture, feed an exact planted path point through this
	// body handler. It proves §4.5 independently of where a review patch happens
	// to cross the knot in this particular layout.
	savedMatrix = matrix.asArray;
	matrix.patch(\tri1, \coco1_spaf, 0.5);
	map.overlay.invalidate;
	cable = map.overlay.cableRecords.detect { |record|
		record[\src] == \tri1 and: { record[\dest] == \coco1_spaf }
	};
	cablePoint = cable !? { |record|
		record[\points].at(record[\points].size div: 2)
	};
	check.value(cablePoint.notNil,
		"the preamp priority fixture has an exact planted cable point");
	if(cablePoint.notNil) {
		local = cablePoint - root[\box].leftTop;
		view.mouseOverAction.value(view, local.x, local.y);
		check.value(map.preampHover[0].not
			and: { gui.bananaGrammar.hoverCable.notNil },
			"a crossing cable takes hover before the preamp provenance");
		before = state.get(\preADrive);
		view.mouseDownAction.value(view, local.x, local.y, 0, 0, 1);
		check.value(gui.bananaGrammar.isInHand
			and: { state.get(\preADrive) == before },
			"a crossing cable takes the press before the drive knot");
		gui.bananaGrammar.escape;
	};
	matrix.load(savedMatrix);
	map.overlay.invalidate;
};

// ---- §7.1 the compass water (G3): seeds, voyages, weather — the synchronous
// acts. Defined here at sane indentation; called from the night sequence.
compassChecks = { |map|
	var almanac = map.geometry[\almanac];
	var cView = map.compassView;
	var boxOrigin = almanac[\compassBox].leftTop;
	var at = { |stagePoint| stagePoint - boxOrigin };
	// One geometry owns both the four seeds and the active cross.
	var atNorm = { |nx, ny|
		CQNightGeometry.compassPointAt(almanac, Point(nx, ny))
	};
	var press = { |stagePoint, button = 0, modifiers = 0|
		var p = at.value(stagePoint);
		cView.mouseDownAction.value(cView, p.x, p.y, modifiers, button, 1);
		cView.mouseUpAction.value(cView, p.x, p.y, modifiers, button);
		if(modifiers.isShift) {
			gui.window.view.keyModifiersChangedAction.value(gui.window.view, 0)
		};
	};
	var check = { |condition, message|
		if(condition) { ("ok:   " ++ message).postln } {
			ok = false;
			("FAIL: " ++ message).postln;
		};
	};
	var undoBefore, local, local2;
	var di, si, mark, xEnd;
	var savedRand, expected, countBefore;
	var record, cablePoint, crossing, savedMatrix;
	check.value(cView.notNil
		and: { (cView.bounds.left - almanac[\compassBox].left).abs < 1 },
		"the tide is the full-stage ground, not a pool layer");
	check.value(gui.compass.stampedMask == [false, false, false, false],
		"the pad opens with four vacant memory husks");
	// two different jungles into TL and TR
	matrix.load([[\tri1, \coco1_spaf, 0.55], [\envB, \castle3_fm, -0.45]]);
	press.value(almanac[\stamps][0]);
	check.value(gui.compass.stampedMask == [false, false, false, false]
		and: { matrix.asArray.size == 2 },
		"an ordinary tap cannot accidentally write a vacant seed");
	press.value(almanac[\stamps][0], 0, 131072);
	check.value(gui.compass.stampedMask == [true, false, false, false],
		"Shift-tap writes the live matrix into a vacant seed");
	check.value(File.exists(compassDir +/+ CQCompass.fileName),
		"a stamp auto-saves the memories file");
	matrix.load([[\cas2, \coco2_fbVca, 0.4]]);
	map.tideFocusLatched_(true);
	press.value(almanac[\stamps][1]);
	map.tideFocusLatched_(false);
	check.value(gui.compass.stampedMask == [true, true, false, false],
		"an open gill lets a plain tap plant a vacant seed without a keyboard");
	// A filled seed recalls exactly. Unfocused open water remains inert even
	// past the shared 8 px threshold: a tide voyage requires the deliberate
	// Tab/gill latch or momentary Shift focus.
	matrix.clearPatch;
	undoBefore = gui.bananaGrammar.undoCount;
	press.value(atNorm.value(0, 0));
	check.value(matrix.getPatch(\tri1, \coco1_spaf) == 0.55
		and: { matrix.getPatch(\envB, \castle3_fm) == -0.45 }
		and: { matrix.asArray.size == 2 },
		"a tap at a pure corner recalls that stamp bit-exact");
	check.value(gui.bananaGrammar.undoCount == (undoBefore + 1),
		"one filled-seed recall is one undo op");
	gui.window.view.keyDownAction.value(gui.window.view, nil, 262144, 26, 0, 90);
	check.value(matrix.asArray.size == 0,
		"Ctrl+Z returns the bay the seed recall replaced");
	press.value(almanac[\stamps][2], 0, 131072);
	check.value(gui.compass.stampedMask == [true, true, true, false]
		and: { gui.compass.memoryDensities[2] == 0 },
		"a stored silent patch is occupied but remains a dormant dark kernel");
	press.value(almanac[\stamps][2], 1);
	check.value(gui.compass.stampedMask == [true, true, false, false],
		"right press returns a dormant kernel to a vacant husk");
	undoBefore = gui.bananaGrammar.undoCount;
	press.value(atNorm.value(0.5, 0.5));
	check.value(matrix.asArray.isEmpty
		and: { gui.bananaGrammar.undoCount == undoBefore },
		"an open-water tap is a no-op, never an accidental jump");
	local = at.value(atNorm.value(0.42, 0.42));
	local2 = at.value(atNorm.value(0.58, 0.58));
	cView.mouseDownAction.value(cView, local.x, local.y, 0, 0, 1);
	cView.mouseMoveAction.value(cView, local2.x, local2.y, 0);
	check.value(map.touch.tideActive.not
		and: { map.overlay.stormMode.not }
		and: { matrix.asArray.isEmpty },
		"an unfocused left sweep cannot accidentally activate morph");
	cView.mouseUpAction.value(cView, local2.x, local2.y, 0, 0);
	// Tab latches the water without changing the instrument stage. The gill is the exact
	// same state in the landscape and can close and reopen it.
	gui.window.view.keyDownAction.value(gui.window.view, 9.asAscii, 0);
	gui.window.view.keyDownAction.value(gui.window.view, 9.asAscii, 0);
	gui.window.view.keyUpAction.value(gui.window.view, 9.asAscii, 0);
	check.value(map.tideFocus and: { map.tideFocusLatched }
		and: { map.overlay.backgrounded }
		and: { gui.shellState.viewMode == \night },
		"one held Tab opens focus once, without autorepeat or a view flip");
	local = map.geometry[\tideGate] - map.geometry[\tideGateBox].leftTop;
	map.tideGateView.mouseDownAction.value(map.tideGateView,
		local.x, local.y, 0, 0, 1);
	check.value(map.tideFocus.not and: { map.tideFocusLatched.not },
		"the closed gill toggles the same persistent focus off");
	// The gill is the one deliberate exception to cable-first priority. Find a
	// real endpoint pair whose rendered curve crosses its exact hit shape, then
	// prove hover and press still belong to the visible gill.
	CQMatrix.sourceNames.do { |src|
		CQMatrix.destNames.do { |dest|
			var start, finish, points, segments, hit;
			if(crossing.isNil) {
				start = map.overlay.pointFor(src);
				finish = map.overlay.pointFor(dest);
				segments = max(8,
					(CQNightGeometry.distance(start, finish) / 12).ceil.asInteger);
				points = CQInk.cablePoints(start, finish, segments);
				hit = points.detect { |point|
					CQNightGeometry.tideGateHit(map.geometry, point)
				};
				if(hit.notNil) { crossing = [src, dest, hit] };
			};
		};
	};
	check.value(crossing.notNil,
		"the canonical endpoint field contains a cable that crosses the gill");
	savedMatrix = matrix.asArray;
	matrix.patch(crossing[0], crossing[1], 0.7);
	map.overlay.invalidate;
	check.value(map.overlay.patchAt(crossing[2]).notNil,
		"the regression point really belongs to the crossing cable");
	local = crossing[2] - map.geometry[\tideGateBox].leftTop;
	map.tideGateView.mouseOverAction.value(map.tideGateView, local.x, local.y);
	check.value(map.tideGateHover
		and: { gui.bananaGrammar.hoverCable.isNil },
		"the gill owns hover and clears cable focus at their crossing");
	map.tideGateView.mouseDownAction.value(map.tideGateView,
		local.x, local.y, 0, 0, 1);
	check.value(map.tideFocus and: { map.tideFocusLatched },
		"the gill owns press through a crossing cable and opens the water");
	matrix.load(savedMatrix);
	map.overlay.invalidate;
	// a voyage: pour through the field, commit on release. The end position is
	// derived from the cell's own tide mark, so the assertion is
	// deterministic geography, not luck.
	di = CQMatrix.destIndex(\coco1_spaf);
	si = CQMatrix.srcIndex(\tri1);
	mark = CQCompass.tideMarks[di][si];
	xEnd = (1 - mark) * 0.5;
	undoBefore = gui.bananaGrammar.undoCount;
	local = at.value(atNorm.value(xEnd, 0.32));
	local2 = at.value(atNorm.value(xEnd, 0.02));
	cView.mouseDownAction.value(cView, local.x, local.y, 0, 0, 1);
	cView.mouseMoveAction.value(cView, local2.x, local2.y, 0);
	check.value(map.overlay.stormMode,
		"a pour holds the night overlay in storm form");
	check.value(map.touch.tideActive,
		"the voyage captures the hand while its paced cross catches up");
	cView.mouseUpAction.value(cView, local2.x, local2.y, 0, 0);
	check.value(map.overlay.stormMode.not,
		"release returns the bay to full ink");
	check.value(map.compassDragState.isNil, "and takes the cross with it");
	check.value((matrix.getPatch(\tri1, \coco1_spaf) - 0.55).abs < 1e-9,
		"a cable standing above its tide mark keeps its stamped weight");
	check.value(gui.bananaGrammar.undoCount == (undoBefore + 1),
		"the whole voyage is one undo op");
	check.value(map.tideFocus and: { map.tideFocusLatched },
		"persistent water focus stays open after the voyage");
	gui.window.view.keyDownAction.value(gui.window.view, 9.asAscii, 0);
	gui.window.view.keyUpAction.value(gui.window.view, 9.asAscii, 0);
	// Shift still offers the temporary version without erasing patch topology:
	// spans recede and
	// stop hit-testing, while their seated socket plugs still answer.
	record = map.overlay.cableRecords.first;
	cablePoint = record[\points][record[\points].size div: 2];
	check.value(map.overlay.patchAt(cablePoint).notNil,
		"a planted span answers before water focus");
	gui.window.view.keyModifiersChangedAction.value(gui.window.view, 131072);
	check.value(map.tideFocus and: { map.overlay.backgrounded }
		and: { map.overlay.patchAt(cablePoint).isNil },
		"Shift sends cable spans behind the playable water");
	check.value(map.overlay.patchAtSocket(record[\dest], record[\finish]).notNil,
		"water focus keeps endpoint plugs interactive");
	gui.window.view.keyModifiersChangedAction.value(gui.window.view, 0);
	check.value(map.tideFocus.not and: { map.overlay.backgrounded.not }
		and: { map.overlay.patchAt(cablePoint).notNil },
		"releasing Shift restores the ordinary cable grammar");
	// Breed is no longer a graft tool. A filled seed dragged directly onto a
	// second filled seed draws one temporary relation and plants the offspring
	// as one undoable act. Shift keeps dense cable spans behind the gesture.
	matrix.clearPatch;
	map.overlay.invalidate;
	savedRand = thisThread.randData;
	thisThread.randSeed = 777;
	expected = CQCompass.breed(gui.compass.corner(0), gui.compass.corner(1));
	thisThread.randSeed = 777;
	undoBefore = gui.bananaGrammar.undoCount;
	local = at.value(almanac[\stamps][0]);
	local2 = at.value(almanac[\stamps][1]);
	cView.mouseDownAction.value(cView, local.x, local.y, 131072, 0, 1);
	cView.mouseMoveAction.value(cView, local2.x, local2.y, 131072);
	check.value(map.compassDragState.notNil
		and: { map.compassDragState[\breedFrom] == 0 }
		and: { map.compassDragState[\breedTarget] == 1 }
		and: { map.tideGestureView.acceptsMouse.not },
		"a valid breed target catches the top-layer relation and blooms");
	cView.mouseUpAction.value(cView, local2.x, local2.y, 131072, 0);
	map.modifierTideFocus_(false);
	thisThread.randData = savedRand;
	check.value(matrix.fullMatrix == expected,
		"releasing on a second seed breeds the exact §7.3 offspring");
	check.value(gui.bananaGrammar.undoCount == (undoBefore + 1),
		"one breeding is one undo op");
	// a right press on a seed clears that memory, and it stays cleared on disk.
	matrix.clearPatch;
	map.overlay.invalidate;
	check.value(CQNightGeometry.stampHit(almanac, almanac[\stamps][1]) == 1
		and: { map.overlay.patchAt(almanac[\stamps][1]).isNil },
		"precondition: seed 1 is free water before the clear");
	press.value(almanac[\stamps][1], 1);
	check.value(gui.compass.stampedMask == [true, false, false, false],
		"a right press on a seed clears its memory");
	check.value(CQCompass(compassDir).stampedMask
		== [true, false, false, false],
		"stamps and clears survive on disk (versioned file)");
	// Outside the four winds the ground keeps its ordinary grammar.
	countBefore = matrix.asArray.size;
	// the full-stage layer's own lower-left corner lies outside the tide
	press.value(almanac[\compassBox].leftTop
		+ Point(3, almanac[\compassBox].height - 3));
	check.value(CQNightGeometry.compassHit(almanac,
		almanac[\compassBox].leftTop
			+ Point(3, almanac[\compassBox].height - 3)).not
		and: { CQNightGeometry.weatherHit(almanac,
			almanac[\compassBox].leftTop
				+ Point(3, almanac[\compassBox].height - 3)).isNil },
		"precondition: the stage corner is outside the tide");
	check.value(matrix.asArray.size == countBefore
		and: { gui.bananaGrammar.isInHand.not },
		"water outside the four winds acts on nothing");
};

// ---- the elastic and the offscreen cache need real Qt ticks, so they run as
// their own async step just before teardown; `done` continues the sequence.
weatherAsync = { |map, done|
	var almanac = map.geometry[\almanac];
	var cView = map.compassView;
	var boxOrigin = almanac[\compassBox].leftTop;
	var atNorm = { |nx, ny|
		CQNightGeometry.compassPointAt(almanac, Point(nx, ny))
	};
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
	local = atNorm.value(0.5, 0.16) - boxOrigin;
	local2 = atNorm.value(0.5, 0.9) - boxOrigin;
	cView.mouseDownAction.value(cView, local.x, local.y, 131072, 1, 1);
	cView.mouseMoveAction.value(cView, local2.x, local2.y, 131072);
	check.value(map.overlay.stormMode, "the elastic pours in storm form too");
	cView.mouseUpAction.value(cView, local2.x, local2.y, 131072, 1);
	map.modifierTideFocus_(false);
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
				check.value(map.overlay.skin == \night
					and: { map.overlay.view.acceptsMouse.not },
					"night owns a hit-transparent phosphor cable overlay");
				check.value(map.routeView.acceptsMouse.not,
					"route provenance owns one hit-transparent topological overlay");
				check.value(#[envA, envB].every { |name|
					map.endpoints[name][\view].background.alpha == 0
				}, "env output dots keep transparent paper over the exact curved branch");
				check.value(map.eyeView.background.alpha == 0,
					"the storm iris masks a circle, not an invisible square");
				check.value(map.readHeadViews.size == 2
					and: { map.readHeadViews.every { |view|
						view.background.alpha == 0 and: { view.acceptsMouse.not }
					} },
					"Coco read heads own transparent hit-through animation layers");
				check.value(#[coco1, coco2].every { |name|
					map.endpoints[name][\view].background.alpha == 0
				}, "coco mouth outputs cannot erase the full-speed reel mark");
				check.value(map.tideGestureView.acceptsMouse.not
					and: { CQNightGeometry.compute(1280, 570)[\castles]
						.every { |castle|
						castle[\stubs].isNil } },
					"tide relations own the top transparent layer and castles grow no fake stubs");
				check.value(map.castleViews.every { |view|
					view.background.alpha == 0
				} and: {
					(1..5).every { |n|
						#[tri, cas].every { |prefix|
							map.endpoints[(prefix.asString ++ n).asSymbol]
								[\view].background.alpha == 0
						} and: {
							#[fm, spesal].every { |suffix|
								map.endpoints[("castle" ++ n ++ "_" ++ suffix).asSymbol]
									[\view].background.alpha == 0
							}
						}
					}
				}, "castle flesh and its endpoint details have no rectangular occlusion");
				check.value(map.endpoints.values.every { |endpoint|
					endpoint[\view].mouseDownAction.notNil
				}, "the shared grammar controller binds every night endpoint");
				gui.bananaGrammar.hoverEndpoint = \deckA_rate;
				check.value(gui.endpointInkState(\deckA_rate, \socket) == \idle,
					"an inert destination hover promises no unavailable action");
				gui.bananaGrammar.startSource(\tri1);
				gui.bananaGrammar.hoverEndpoint = nil;
				check.value(CQMatrix.destNames.every { |name|
					gui.endpointInkState(name, \socket) == \available
				}, "a banana in hand gives every legal destination a quiet halo");
				gui.bananaGrammar.hoverEndpoint = \deckA_rate;
				check.value(gui.endpointInkState(\deckA_rate, \socket) == \hover,
					"the destination under the held banana answers more strongly");
				gui.bananaGrammar.releaseVoid;
				gui.bananaGrammar.hoverEndpoint = nil;
				check.value(map.holdStates[0] == (state.get(\coco1Hold) > 0.5),
					"hold seal state mirrors the power-on param");
				check.value(map.sproutState == \hidden,
					"no mic configured: the sprout is not drawn at all");
				check.value(gui.shellState.viewMode == \night,
					"the shell opens directly on the night map");
				AppClock.sched(0.2, {
					try {
						var geo = map.geometry, expected, view, anchor;
						var geometryProbe, reusedProbe;
						var sourceView, destView, destPoint, dragPoint, sourceAnchor;
						var before, triView, target, movePoint, triAnchor;
						check.value(geo.notNil, "night geometry is laid out");
						check.value(map.tideGesturePaintCount > 0,
							"the top tide layer paints the resting gill before any hover");
						expected = geo[\endpoints][\tri1];
						view = map.endpoints[\tri1][\view];
						// the anchor is read, never assumed: a flooded view size
						// must move this assert's expectation with it (rake 11)
						anchor = map.endpoints[\tri1][\anchor];
						check.value(
							(view.bounds.left - (expected.x - anchor.x)).abs < 1
							and: { (view.bounds.top - (expected.y - anchor.y)).abs < 1 },
							"a castle leaf jack sits exactly on its computed home");
						check.value(map.endpoints.keys.every { |name|
							var endpoint = map.endpoints[name];
							var center = geo[\endpoints][name];
							var actual = endpoint[\view].bounds.leftTop
								+ endpoint[\anchor];
							CQNightGeometry.distance(center, actual) < 1
						},
							"all 38 endpoint views keep their computed cable anchor");
						map.overlay.invalidate;
						check.value(map.overlay.cableRecords.every { |record|
							CQNightGeometry.distance(record[\start],
								geo[\endpoints][record[\src]]) < 1
							and: { CQNightGeometry.distance(record[\finish],
								geo[\endpoints][record[\dest]]) < 1 }
						}, "every planted cable lands on both moved endpoint centres");
					// SOAK-1 finding A: a weight-only write must not rebuild a
					// single curve. Measured 2026-07-31, rebuilding them cost
					// 122.9 ms at 352 cables and starved the event loop, so this
					// pins the geometry as shared objects: a morph frame is
					// allowed to change what a cable weighs, never where it runs.
					geometryProbe = map.overlay.cableRecords
						.detect { |r| r[\src] == \tri1 };
					matrix.patch(\tri1, \coco1_spaf, 0.31);
					map.overlay.invalidate;
					reusedProbe = map.overlay.cableRecords
						.detect { |r| r[\src] == \tri1 };
					check.value(reusedProbe.notNil
						and: { reusedProbe[\points] === geometryProbe[\points] }
						and: { reusedProbe[\weight] != geometryProbe[\weight] },
						"a weight change reuses the cable's curve and only its weight moves");
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
						// §4.1: opening the workbench hides Night and must revert
						// the audition before the queued stage swap.
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
						gui.shell.toggleDrawer;
						check.value(gui.bananaGrammar.isInHand.not
							and: { matrix.fullMatrix.asCompileString == before },
							"opening Drawer drops the night hand and reverts — no leak");
						gui.shell.toggleDrawer;
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
														check.value(map.rangeStates[1] == \infra,
															"the castle range ripple follows its param");
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
															var shell, coco, local, crescent, dolbyView;
															var speedBefore, freqBefore, chaosBefore;
															var samples, cablePoint, cableRecord, heartPoint;
															var castleTwo, castleView, dialPoint;
															var rangePoint, rangeBefore, rangeBox, rangeCable;
															var matrixBeforeRange;
															var dolbyY, dolbyBefore, dolbyBox, dolbyCable;
															var matrixBeforeDolby, inBefore;
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
															// N5: the in/fb crescent keeps that same drag,
															// while its smaller oyster owns one tap below.
															dolbyView = map.crescentViews[\coco1In];
															dolbyY = mid
																+ CQNightMeasures.dolbyOysterDrop;
															state.set(\coco1DolbyIn, \off);
															gui.prFlushParams;
															dolbyView.mouseOverAction.value(
																dolbyView, mid, dolbyY);
															check.value(map.dolbyHover.includes(\coco1In),
																"the input oyster owns its static hover cue");
															dolbyView.mouseDownAction.value(
																dolbyView, mid, dolbyY, 0, 0, 1);
															check.value(state.get(\coco1DolbyIn) == \gate,
																"tapping the oyster steps off to gate through CQState");
															gui.prFlushParams;
															check.value(map.dolbyStates[\coco1DolbyIn] == \gate,
																"the oyster repaints from funnel truth");
															dolbyView.mouseDownAction.value(
																dolbyView, mid, dolbyY, 0, 0, 1);
															check.value(state.get(\coco1DolbyIn) == \duck,
																"the second posture is cross-keyed duck");
															dolbyView.mouseDownAction.value(
																dolbyView, mid, dolbyY, 0, 0, 1);
															check.value(state.get(\coco1DolbyIn) == \off,
																"three oyster taps wrap through CQSpec choices");
															#[coco1Fb, coco2In, coco2Fb].do { |crescentName|
																var view = map.crescentViews[crescentName];
																var param = CQGuiNightMap
																	.dolbyNameForCrescent(crescentName);
																state.set(param, \off);
																gui.prFlushParams;
																view.mouseDownAction.value(
																	view, mid, dolbyY, 0, 0, 1);
																check.value(state.get(param) == \gate,
																	"% oyster has the same funnel step"
																		.format(crescentName));
															};
															inBefore = state.get(\coco1In);
															dolbyView.mouseMoveAction.value(
																dolbyView, mid, dolbyY - 20, 0);
															dolbyView.mouseUpAction.value(
																dolbyView, mid, dolbyY - 20, 0, 0);
															check.value(state.get(\coco1In) == inBefore,
																"a drag starting on the oyster never pours the crescent");
															dolbyView.mouseOverAction.value(
																dolbyView, mid, mid);
															check.value(map.dolbyHover.includes(\coco1In).not,
																"crescent and oyster hover cues are exclusive");

															// Plant a real cable across the oyster and call the
															// covered view directly: §4.5 must still win in logic.
															dolbyBox = CQNightGeometry.dolbyOysterBox(
																map.geometry[\cocos][0][\crescents][1]);
															CQMatrix.sourceNames.do { |src|
																CQMatrix.destNames.do { |dest|
																	if(dolbyCable.isNil) {
																		var start = map.overlay.pointFor(src);
																		var finish = map.overlay.pointFor(dest);
																		var points = CQInk.cablePoints(start,
																			finish, 400);
																		var crossing = points.detect { |p|
																			dolbyBox.containsPoint(p)
																		};
																		crossing !? {
																			dolbyCable = (src: src,
																				dest: dest, point: crossing)
																		};
																	};
																};
															};
															check.value(dolbyCable.notNil,
																"precondition: endpoint geometry offers a cable across the oyster");
															if(dolbyCable.notNil) {
																matrixBeforeDolby = matrix.fullMatrix;
																matrix.patch(dolbyCable[\src],
																	dolbyCable[\dest], 0.5,
																	\test, false, \dolbyPriority);
																map.overlay.invalidate;
																dolbyBefore = state.get(\coco1DolbyIn);
																local = dolbyCable[\point]
																	- dolbyView.bounds.leftTop;
																dolbyView.mouseOverAction.value(
																	dolbyView, local.x, local.y);
																check.value(map.dolbyHover
																	.includes(\coco1In).not,
																	"a cable suppresses the oyster hover cue");
																check.value(gui.bananaGrammar.hoverCable.notNil,
																	"the crossing cable receives the hover the oyster yielded");
																dolbyView.mouseDownAction.value(dolbyView,
																	local.x, local.y, 0, 0, 1);
																check.value(state.get(\coco1DolbyIn)
																	== dolbyBefore,
																	"a cable crossing the oyster wins; Dolby does not step");
																dolbyView.mouseUpAction.value(dolbyView,
																	local.x, local.y, 0, 0);
																matrix.writeMatrix(matrixBeforeDolby,
																	\test, false, \dolbyPriorityRestore);
																map.overlay.invalidate;
															};
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
															check.value(map.heartHover[0].not,
																"and the heart stays dark out on the groove");
															shell.mouseLeaveAction.value(shell);
															check.value(map.shellHover[0].not,
																"leaving the shell rests the cue");
															// The slow end of the reel lies inside the
															// heart's disc. Found in the user's hands
															// 2026-07-29: the cue promised a wind there
															// and the press toggled the loop instead.
															// One resolver now answers both, so the
															// promise and the act are tested together.
															heartPoint = CQNightGeometry
																.spiralPoint(coco, 0);
															check.value(CQNightGeometry.distance(
																heartPoint, coco[\center])
																	<= coco[\heartRadius],
																"precondition: the slowest reel pearl really "
																"sits inside the heart");
															shell.mouseOverAction.value(shell,
																heartPoint.x - local.x,
																heartPoint.y - local.y);
															check.value(map.heartHover[0],
																"the heart owns the cue where it owns the press");
															check.value(map.shellHover[0].not,
																"and the wind cue no longer lies over the heart");
															speedBefore = state.get(\coco1Speed);
															holdBefore = state.get(\coco1Hold);
															shell.mouseDownAction.value(shell,
																heartPoint.x - local.x,
																heartPoint.y - local.y, 0, 0, 1);
															shell.mouseMoveAction.value(shell,
																samples[34].x - local.x,
																samples[34].y - local.y, 0);
															shell.mouseUpAction.value(shell,
																samples[34].x - local.x,
																samples[34].y - local.y, 0, 0);
															check.value(state.get(\coco1Hold)
																!= holdBefore,
																"a press there does what the cue promised: hold");
															check.value(state.get(\coco1Speed)
																== speedBefore,
																"and dragging off it never winds the reel behind "
																"the cue's back");
															state.set(\coco1Hold, holdBefore);
															shell.mouseLeaveAction.value(shell);
															check.value(map.heartHover[0].not,
																"leaving rests the heart's ring too");
															// the chevron was the last pressable night
															// control with no cue at all (§4.5)
															shell.mouseOverAction.value(shell,
																coco[\chevron].x - local.x,
																coco[\chevron].y - local.y);
															check.value(map.chevronHover[0],
																"hovering the mouth chevron answers in weight");
															check.value(map.shellHover[0].not
																and: { map.heartHover[0].not }
																and: { map.skipHover[0].not }
																and: { map.rimHover[0].not },
																"and it is the only shell cue lit");
															shell.mouseLeaveAction.value(shell);
															check.value(map.chevronHover[0].not,
																"leaving rests the chevron");
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
																- map.eyeView.bounds.leftTop;
															map.eyeView.mouseOverAction.value(
																map.eyeView, local.x, local.y);
															check.value(map.eyeHover,
																"hovering the eye reveals its weather seasons");
															map.eyeView.mouseLeaveAction.value(map.eyeView);
															check.value(map.eyeHover.not,
																"leaving the eye rests its weather seasons");
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
																"the eye keeps its own local storm layer");
															check.value((map.eyeFreqNorm
																- CQSpec.unmap(\qFreq,
																	state.get(\qFreq))).abs < 1e-6
																and: { (map.eyeChaosNorm
																	- CQSpec.unmap(\qChaos,
																		state.get(\qChaos))).abs < 1e-6 },
																"the storm iris paints the funnel truth mid-drag");
															// ---- the read head: '/cq_phase' spark, display only ----
															check.value(map.sparkPoint(0).isNil,
																"before telemetry the read-head layer invents no spark");
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
															birdChecks.value(map);
															preampChecks.value(map);
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
															gui.prPhaseFrame(1, 0.025, 1, 12);
															check.value(map.struckOn[0].not,
																"three 15 Hz frames remain below the old time threshold");
															gui.prPhaseFrame(1, 0.02, 1, 12);
															check.value(map.struckOn[0],
																"four lit frames across about 0.2 s mark the head struck");
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
															rangePoint = castleTwo[\rangeMark]
																- castleTwo[\box].leftTop;
															check.value(map.overlay.patchAt(
																castleTwo[\rangeMark]).isNil,
																"precondition: no cable crosses castle 2's range ripple");
															state.set(\c2Range, \infra);
															gui.prFlushParams;
															rangeBefore = state.get(\c2Range);
															castleView.mouseDownAction.value(castleView,
																rangePoint.x, rangePoint.y, 0, 0, 1);
															check.value(state.get(\c2Range) == \balcium,
																"tapping the range ripple steps infra to balcium through CQState");
															gui.prFlushParams;
															check.value(map.rangeStates[1] == \balcium,
																"the ripple repaints from funnel truth without a legacy caption");
															castleView.mouseDownAction.value(castleView,
																rangePoint.x, rangePoint.y, 0, 0, 1);
															castleView.mouseDownAction.value(castleView,
																rangePoint.x, rangePoint.y, 0, 0, 1);
															check.value(state.get(\c2Range) == rangeBefore,
																"three ripple taps wrap through all CQSpec choices to the start");
															castleView.mouseOverAction.value(castleView,
																rangePoint.x, rangePoint.y);
															check.value(map.rangeHover[1]
																and: { map.dialHover[1].not }
																and: { map.bodyHover[1].not },
																"the range ripple owns one exclusive hover cue");
															castleView.mouseLeaveAction.value(castleView);

															// Put one real cable across the ripple. The route is
															// chosen from the live endpoint geometry, never assumed,
															// so future layout changes keep testing §4.5 itself.
															rangeBox = CQNightGeometry.rangeBox(castleTwo);
															CQMatrix.sourceNames.do { |src|
																CQMatrix.destNames.do { |dest|
																	if(rangeCable.isNil) {
																		var start = map.overlay.pointFor(src);
																		var finish = map.overlay.pointFor(dest);
																		var points = CQInk.cablePoints(start,
																			finish, 400);
																		var crossing = points.detect { |p|
																			rangeBox.containsPoint(p)
																		};
																		crossing !? {
																			rangeCable = (src: src,
																				dest: dest, point: crossing)
																		};
																	};
																};
															};
															check.value(rangeCable.notNil,
																"precondition: endpoint geometry offers a cable across the ripple");
															if(rangeCable.notNil) {
																matrixBeforeRange = matrix.fullMatrix;
																matrix.patch(rangeCable[\src],
																	rangeCable[\dest], 0.5,
																	\test, false, \rangePriority);
																map.overlay.invalidate;
																check.value(map.overlay.patchAt(
																	rangeCable[\point]).notNil,
																	"precondition: the planted crossing is inside cable hit width");
																rangeBefore = state.get(\c2Range);
																local = rangeCable[\point]
																	- castleTwo[\box].leftTop;
																castleView.mouseOverAction.value(castleView,
																	local.x, local.y);
																check.value(map.rangeHover[1].not
																	and: { map.dialHover[1].not }
																	and: { map.bodyHover[1].not },
																	"a cable crossing suppresses every castle hover cue");
																castleView.mouseDownAction.value(castleView,
																	local.x, local.y, 0, 0, 1);
																check.value(state.get(\c2Range) == rangeBefore,
																	"a cable crossing the ripple wins the press; range does not step");
																castleView.mouseUpAction.value(castleView,
																	local.x, local.y, 0, 0);
																matrix.writeMatrix(matrixBeforeRange,
																	\test, false, \rangePriorityRestore);
																map.overlay.invalidate;
															};
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
																and: { map.dialHover[1].not }
																and: { map.rangeHover[1].not },
																"hovering a castle's flesh lights the body's press cue");
															castleView.mouseOverAction.value(castleView,
																dialPoint.x, dialPoint.y);
															check.value(map.dialHover[1]
																and: { map.rangeHover[1].not }
																and: { map.bodyHover[1].not },
																"the clock, ripple and body cues never light together");
															castleView.mouseLeaveAction.value(castleView);
															check.value(map.bodyHover[1].not
																and: { map.dialHover[1].not }
																and: { map.rangeHover[1].not },
																"leaving the castle rests all three cues");
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
																	"the chord starts at the shared spesal partner, in its mode");
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
															map.ringView.mouseUpAction.value(
																map.ringView, 6, 6, 0, 0);
															block { |tideDone|
																var stageFrom = map.geometry[\eye]
																	+ Point(62, 0);
																var stageTo = stageFrom + Point(18, 14);
																var ringOrigin = map.geometry[\ringBox].leftTop;
																var from = stageFrom - ringOrigin;
																var to = stageTo - ringOrigin;
																var tideBefore = matrix.fullMatrix;
																map.ringView.mouseDownAction.value(
																	map.ringView, from.x, from.y,
																	131072, 0, 1);
																map.ringView.mouseMoveAction.value(
																	map.ringView, to.x, to.y, 131072);
																check.value(map.compassDragState.notNil
																	and: { map.overlay.stormMode },
																	"free central water starts a small Shift morph through the ring view");
																map.ringView.mouseUpAction.value(
																	map.ringView, to.x, to.y,
																	131072, 0);
																matrix.writeMatrix(tideBefore,
																	origin: \test, committed: false,
																	operation: \centralTideSmokeRestore);
																map.modifierTideFocus_(false);
																tideDone.value;
															};
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
															var from, to, undoBefore, growWeight;
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
															check.value(almanac[\skyMarks].isNil
																and: { almanac[\pool].isNil },
																"the moon has no panel companions and the bay has no pool body");
															check.value(map.shakeAmount == gui.shakeAmount
																and: { gui.shakeAmount
																	== CQGui.defaultShakeAmount },
																"the moon opens on the shared small crescent");
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
															// The vertical axis now belongs to the moon too:
															// down = rain/grow, up = sun/wither. The glyph
															// exists only while that axis-locked hold is live.
															matrix.clearPatch;
															undoBefore = gui.bananaGrammar.undoCount;
															from = almanac[\moon] - almanac[\box].leftTop;
															to = from + (0 @ 40);
															view.mouseDownAction.value(view,
																from.x, from.y, 0, 0, 1);
															view.mouseMoveAction.value(view,
																to.x, to.y, 0);
															check.value(map.weatherActive == \grow
																and: { map.weatherIntensity == 1 }
																and: { matrix.asArray.size == 1 }
																and: { gui.shakeAmount == 0 },
																"a deep downward pull reveals hard rain and lands one grow seed");
															growWeight = matrix.asArray.first[2];
															view.mouseUpAction.value(view,
																to.x, to.y, 0, 0);
															check.value(map.weatherActive.isNil
																and: { gui.bananaGrammar.undoCount
																	== (undoBefore + 1) },
																"releasing the moon hides rain and commits one weather hold");
															matrix.load([[\tri1, \coco1_spaf, growWeight]]);
															undoBefore = gui.bananaGrammar.undoCount;
															to = from + (0 @ -40);
															view.mouseDownAction.value(view,
																from.x, from.y, 0, 0, 1);
															view.mouseMoveAction.value(view,
																to.x, to.y, 0);
															check.value(map.weatherActive == \wither
																and: { map.weatherIntensity == 1 }
																and: { (matrix.asArray.first[2].abs
																	- (growWeight.abs * 0.7)).abs < 1e-9 },
																"a deep upward pull reveals full sun and withers ×0.7");
															view.mouseUpAction.value(view,
																to.x, to.y, 0, 0);
															check.value(map.weatherActive.isNil
																and: { gui.bananaGrammar.undoCount
																	== (undoBefore + 1) },
																"releasing the moon hides sun and commits one drought");
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
																and: { CQGui.sowDensity(0.25)
																	== 0.0125 }
																and: { CQGui.sowDensity(0.5)
																	== 0.05 }
																and: { CQGui.sowDensity(1)
																	== CQGui.fullMoonDensity },
																"the crescent sows sparsely, then the full moon gathers force");
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
														// Tab focus entry is also a punch exit path (§2):
														// begin in night, enter the tide, and assert that the
														// loop was restored without changing view
														state.set(\coco1Hold, 1);
														tabBefore = state.get(\coco1Hold);
														hShell.mouseDownAction.value(hShell,
															heartLocal.x, heartLocal.y, 0, 1, 1);
														check.value(state.get(\coco1Hold) == 0
															and: { map.touch.punchActive },
															"precondition: a punch is held before Tab enters tide focus");
														gui.window.view.keyDownAction.value(
															gui.window.view, 9.asAscii, 0);
														gui.window.view.keyUpAction.value(
															gui.window.view, 9.asAscii, 0);
														check.value(state.get(\coco1Hold) == tabBefore
															and: { map.touch.punchActive.not }
															and: { map.tideFocusLatched }
															and: { gui.shellState.viewMode == \night },
															"Tab restores a held punch before latching water focus");
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
