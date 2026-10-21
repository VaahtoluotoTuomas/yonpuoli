// Serverless native bird gesture/animation lifecycle. The injected dispatcher
// counts requests; it never opens a dialog, boots audio or loads an actual file.
// Human gesture/visual acceptance remains in manual_checklist.md.
(
var gui, map, ok = true, requests = List.new, dialogs = 0;
var check, down, move, up, home, capture, beforeImage, pulledImage, settledImage;
check = { |condition, message|
	if(condition) { ("ok: " ++ message).postln } { ok = false; ("FAIL: " ++ message).postln };
};
home = {
	map.birdOffsets.every { |p| p == (0@0) } and: { map.birdTilts.every { |a| a == 0 } }
		and: { map.birdHeld.every(_.not) } and: { map.birdReturnTasks.every(_.isNil) }
		and: { map.touch.birdGestureActive.not }
};
down = { |index|
	var v = map.birdViews[index], p = v.bounds.moveTo(0, 0).center;
	v.mouseDownAction.value(v, p.x, p.y, 0, 0, 1);
};
move = { |index, delta|
	var v = map.birdViews[index], p = v.bounds.moveTo(0, 0).center + delta;
	v.mouseMoveAction.value(v, p.x, p.y, 0);
};
up = { |index, delta|
	var v = map.birdViews[index], p = v.bounds.moveTo(0, 0).center + delta;
	v.mouseUpAction.value(v, p.x, p.y, 0, 0);
};
capture = { |index, name|
	var view = map.birdViews[index], extent = view.bounds.extent + (8@8);
	var image = Image((extent.x * 3).asInteger, (extent.y * 3).asInteger);
	// Reuse the production draw callback at 3x for review. Synchronous native
	// window grabs can deadlock Qt's painter after a 60 Hz return; rendering
	// the same ink into an Image avoids that unrelated capture dependency.
	image.draw {
		Pen.fillColor = CQInk.palette(\night)[\ground];
		Pen.fillRect(Rect(0, 0, image.width, image.height));
		Pen.scale(3, 3); Pen.translate(4, 4);
		Pen.use { view.drawFunc.value(view) };
	};
	image.write(Cocoquantus.repoDir +/+ "tests/tmp" +/+ name ++ ".png");
	image
};
{
	try {
		var before;
		CQInkControl.resetDrawErrors;
		gui = CQGui(CQState(nil), CQMatrix(nil), CQConfig.validate((headless: true)));
		gui.window.bounds = Rect(-100000, -100000, 1280, 720); 0.5.wait;
		gui.slotAt(\recTask).stop;
		map = gui.nightMap;
		map.deckAction = { |which, action| requests.add([which, action]) };
		map.fileDialog = { dialogs = dialogs + 1 };
		2.do { |index|
			down.value(index); move.value(index, 0 @ -40);
			check.value(requests.isEmpty, "held pull does not request a file");
			check.value(map.birdHeld[index] and: { map.birdOffsets[index].y < 0 },
				"held bird activates and follows an upward pull");
			map.cancelGestures;
		};
		2.do { |index|
			var expected = if(index == 0) { \deckA } { \deckB };
			before = requests.size;
			down.value(index); move.value(index, 50@35); up.value(index, 50@35);
			check.value(requests.size == (before + 1) and: { requests.last == [expected, \random] },
				"release outside the bird requests exactly one random file for its own deck");
			check.value(map.birdReturnTasks[index].notNil, "release owns a finite return task");
			0.65.wait;
			check.value(home.value and: { requests.size == (before + 1) },
				"return settles exactly, releases its task, and never dispatches a second load");
		};
		// Compare the production ink for a deterministic held pose without a
		// cursor poll moving the synthetic hand during the review render.
		beforeImage = capture.value(0, "bird-home");
		map.beginBirdPull(0); map.moveBirdPull(0, 100 @ -60);
		pulledImage = capture.value(0, "bird-pulled");
		check.value(beforeImage.pixels != pulledImage.pixels, "production bird ink visibly responds to the pull");
		map.releaseBirdPull(0, true); 0.65.wait;
		settledImage = capture.value(0, "bird-settled");
		check.value(beforeImage.pixels == settledImage.pixels, "production bird ink returns to exactly its resting pose");
		[beforeImage, pulledImage, settledImage].do(_.free);
		before = requests.size;
		down.value(0); move.value(0, 50@0);
		gui.info.enabled = true; up.value(0, 50@0);
		check.value(home.value and: { requests.size == before }, "entering info cancels a held pull");
		down.value(1); move.value(1, 0@50); up.value(1, 0@50);
		check.value(home.value and: { requests.size == before } and: { dialogs == 0 },
			"info consumes the bird's complete drag without animation or loading");
		gui.info.enabled = false;
		down.value(0); move.value(0, 50@0);
		gui.shell.toggleDrawer; 0.1.wait;
		check.value(home.value and: { requests.size == before }, "Drawer entry cancels hidden bird work");
		gui.shell.toggleDrawer; 0.2.wait;
		down.value(0); move.value(0, 50@0);
		gui.window.bounds = Rect(-100000, -100000, 1400, 800); 0.2.wait;
		check.value(home.value and: { requests.size == before }, "resize cancels held pull and its cursor tracking");
		down.value(0); up.value(0, 50@0); 0.06.wait;
		gui.window.view.keyDownAction.value(gui.window.view, 27.asAscii, 0, 27);
		check.value(home.value, "Esc stops an in-flight spring return");
		before = requests.size;
		down.value(0); move.value(0, 50@0);
		gui.window.close; 0.2.wait;
		check.value(home.value and: { requests.size == before }, "native close cancels held work without loading");
		check.value(CQInkControl.drawErrors == 0,
			"all bird animation frames draw without errors");
	} { |error| ok = false; error.reportError; gui !? { gui.free } };
	if(ok) { "CQ_GUI_BIRD_PASSED".postln; 0.exit } { "CQ_GUI_BIRD_FAILED".postln; 1.exit };
}.fork(AppClock);
)
