// morph_profile.scd — SOAK-1 finding A. Measures what one morph frame costs the
// language thread, because "it feels lighter" is not evidence and the freeze it
// caused was safety invariant #3 (PANIC unreachable).
//
// Headless and serverless: CQTestGraph stands in for the server, so this measures
// exactly the half that froze — clamping, funnelling and notifying — with no
// audio device and no Qt. Run through tests/run_sclang_gate.ps1.
// Prints CQ_MORPH_PROFILE_DONE as its last line.

(
var runner;
runner = {
	var state, matrix, snaps, dir, graph;
	var listeners, deferCount = 0, notifyCount = 0;
	var frames, worst, total, report, scenario;

	dir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "morphprofile";
	File.mkdir(dir);
	(dir +/+ "*.cqsnap.scd").pathMatch.do { |p| File.delete(p) };

	// One listener per real subscriber, each doing what the real one does at the
	// coarsest level: count the announcements the GUI would have to defer.
	scenario = { |label, buildB, dur = 4|
		var a, b, watcher, ticks, start, last, gaps;
		state = CQState(nil);
		matrix = CQMatrix(nil, state);
		snaps = CQSnapshots(state, matrix, dir, { [0, 0, 0, 0, 0] }, { }, dir);
		notifyCount = 0;
		deferCount = 0;
		watcher = SimpleController(state);
		watcher.put(\param, { notifyCount = notifyCount + 1 });
		watcher.put(\params, { notifyCount = notifyCount + 1 });
		listeners = SimpleController(snaps);
		// One \morph notification per frame, so the count below is per frame and
		// not per morph — the number that decides whether the thread can breathe.
		listeners.put(\morph, { |obj, what, progress, from, to, status|
			if(status == \running) { deferCount = deferCount + 1 };
		});

		// A: a dense patch. B: whatever the scenario changes on top of it.
		matrix.preset(\mayhem);
		state.set(\coco1Fb, 0.35);
		snaps.save("profile_a");
		buildB.value(state, matrix);
		snaps.save("profile_b");

		gaps = List.new;
		ticks = 0;
		start = Main.elapsedTime;
		last = start;
		snaps.morph("profile_a", "profile_b", dur);
		// Drive the routine's own clock by yielding: SystemClock runs it while
		// this routine waits, so the gaps measured are the real frame spacing.
		while { snaps.isMorphing and: { (Main.elapsedTime - start) < (dur * 3) } } {
			0.01.wait;
			ticks = ticks + 1;
		};
		total = Main.elapsedTime - start;
		watcher.remove;
		listeners.remove;
		(
			label: label,
			seconds: total.round(0.01),
			requested: dur,
			frames: deferCount,
			notifications: notifyCount,
			// The recall at each end writes once too; the frames are what repeats.
			perFrame: ((notifyCount - 3).max(0) / deferCount.max(1)).round(0.01)
		)
	};

	report = List.new;

	// 1. The soak's own pair: two dense snapshots that differ ONLY in the matrix
	//    (mayhem + a shake). This is the case that pinned a core for 40-60 s.
	report.add(scenario.value("matrix only (the soak's pair)", { |st, mx|
		mx.load(CQMatrix.presets[\mayhem][\matrix].collect { |t|
			[t[0], t[1], (t[2] * -0.5)]
		});
	}));

	// 2. A pair whose parameters really do move, so the moving set is not empty
	//    and every frame still carries a dense parameter write.
	report.add(scenario.value("params move too", { |st, mx|
		st.set(\coco1Fb, 1.1);
		st.set(\coco2Fb, 0.9);
		st.set(\coco1Speed, 12000);
		st.set(\coco2Speed, 36000);
		st.set(\masterVol, -12);
		mx.load(CQMatrix.presets[\mayhem][\matrix].collect { |t|
			[t[0], t[1], (t[2] * -0.5)]
		});
	}));

	"".postln;
	"CQ morph profile — one language-side notification is one deferred GUI closure".postln;
	report.do { |r|
		("  %  |  %s requested, %s actual  |  % frames  |  % state notifications"
			++ "  |  % per frame")
			.format(r[\label].padRight(30), r[\requested], r[\seconds],
				r[\frames], r[\notifications], r[\perFrame]).postln;
	};
	"".postln;
	("  before this fix every frame announced once per continuous parameter:"
		++ " ~60 per frame, ~900/s at 15 Hz, and each one was deferred separately"
		++ " by three listeners").postln;
	"".postln;
	"CQ_MORPH_PROFILE_DONE".postln;
	0.exit;
};
runner.fork(SystemClock);
)
