// morph_profile_gui.scd — SOAK-1 finding A, second attempt, measured where it
// actually fails: with the real Qt GUI alive. The first fix collapsed the
// language-side parameter stream (measured, 55x fewer notifications) and the
// freeze survived it, so the cost is somewhere else and this script finds out
// where instead of guessing a third time.
//
// The metric that matters is not milliseconds but REACHABILITY: a 20 Hz
// competitor runs on AppClock during the morph, exactly where a PANIC click
// would queue. If it gets its ticks, the button answers; if it starves, the
// instrument is frozen however fast the frames are.
//
// Prints CQ_MORPH_GUI_PROFILE_DONE as its last line.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var compassDir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "cqcompass_profile";
var snapDir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "morphprofile_gui";
var config, gui, snaps, report, runCase, densePair, finish, phaseCosts;

File.mkdir(compassDir);
File.mkdir(snapDir);
(snapDir +/+ "*.cqsnap.scd").pathMatch.do { |p| File.delete(p) };
config = CQConfig.validate((headless: true, gui: (compassDir: compassDir)));

SystemClock.sched(120, {
	"FAIL: profile watchdog — a continuation never completed".postln;
	"CQ_MORPH_GUI_PROFILE_DONE".postln;
	1.exit;
});

// mayhem's own density: every one of the 352 cells patched.
densePair = {
	CQMatrix.presets[\mayhem][\matrix].do { |t| matrix.patch(t[0], t[1], t[2]) };
	state.set(\coco1Fb, 0.2);
	snaps.save("gui_a");
	CQMatrix.presets[\mayhem][\matrix].do { |t| matrix.patch(t[0], t[1], t[2] * -0.6) };
	state.set(\coco1Fb, 1.1);
	snaps.save("gui_b");
};

// One case = one morph with a 20 Hz competitor counting its own ticks.
runCase = { |label, dur, onDone|
	var ticks = 0, started, competitor, expected;
	expected = (dur * 20).round.asInteger;
	started = Main.elapsedTime;
	competitor = Routine {
		while { (Main.elapsedTime - started) < dur } {
			ticks = ticks + 1;
			0.05.wait;
		};
	}.play(AppClock);
	snaps.morph("gui_a", "gui_b", dur);
	AppClock.sched(dur + 1.5, {
		var ran = Main.elapsedTime - started;
		competitor.stop;
		report.add((
			label: label,
			requested: dur,
			actual: ran.round(0.01),
			ticks: ticks,
			expected: expected,
			reachable: ((ticks / expected) * 100).round(1)
		));
		onDone.value;
		nil
	});
};

report = List.new;

// What one frame actually asks the cable layer to redo, timed separately, so the
// fix aims at the dominant term instead of at a plausible one.
phaseCosts = {
	var overlay = gui.nightMap !? { |map| map.overlay };
	var t0, build, ink, cables;
	overlay !? {
		cables = overlay.cableCount;
		overlay.invalidate;
		t0 = Main.elapsedTime;
		overlay.cableRecords;
		build = (Main.elapsedTime - t0) * 1000;
		overlay.invalidate;
		overlay.cableRecords;              // records warm, so this times the ink
		t0 = Main.elapsedTime;
		overlay.prEnsureImage;
		ink = (Main.elapsedTime - t0) * 1000;
		"".postln;
		("  one frame's cable work at % cables:  records % ms  |  ink % ms"
			++ "  |  15 Hz would need % ms of every 66")
			.format(cables, build.round(0.1), ink.round(0.1),
				(build + ink).round(0.1)).postln;
	};
};

finish = {
	phaseCosts.value;
	"".postln;
	"CQ morph GUI profile — AppClock reachability during a dense morph".postln;
	("  (ticks = 20 Hz competitor slices actually run; 100 % means a PANIC"
		++ " click would have been answered at once)").postln;
	"".postln;
	report.do { |r|
		("  %  |  %s morph  |  % / % ticks  |  % %% reachable")
			.format(r[\label].padRight(34), r[\requested], r[\ticks],
				r[\expected], r[\reachable]).postln;
	};
	"".postln;
	gui !? { gui.free };
	"CQ_MORPH_GUI_PROFILE_DONE".postln;
	AppClock.sched(0.2, { 0.exit; nil });
};

// 1. No GUI at all: the language funnel alone, the baseline the first profile
//    measured and pronounced fixed.
snaps = CQSnapshots(state, matrix, snapDir, { [0, 0, 0, 0, 0] }, { }, snapDir);
densePair.value;
runCase.value("no GUI (language only)", 3, {
	// 2. The real Night GUI, which is where it froze.
	gui = CQGui(state, matrix, config);
	AppClock.sched(0.6, {
		runCase.value("real GUI, Night visible", 3, {
			// 3. Same GUI, but the Drawer swapped over the Night stage — the
			//    user's own A/B, now measured instead of felt.
			gui.shell !? { |sh| sh.toggleDrawer };
			runCase.value("real GUI, Night hidden (Drawer)", 3, {
				finish.value;
			});
			nil
		});
		nil
	});
});
)
