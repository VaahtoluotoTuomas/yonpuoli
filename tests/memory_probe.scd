// memory_probe.scd — SOAK-1 criterion 5. The soak measured sclang's working set
// climbing ~20 MB/h without settling, twice, on both the old and the fixed code.
// Where the growth comes from is a different question from whether it plateaus,
// and only the first one can be answered in minutes instead of a night.
//
// This runs ONE workload for a fixed time while tests/memory_probe.ps1 samples
// this process's working set from outside. No server, no audio device: the
// language side is what grows, and every scenario below is reproducible with a
// serverless GUI exactly as the smokes build one.
//
// The scenario and duration arrive in tests/tmp/mem_probe.txt ("<name> <secs>")
// because a .scd takes no arguments. Prints CQ_MEM_PROBE_DONE last.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var compassDir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "cqcompass_memprobe";
var snapDir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "memprobe_snaps";
var requestPath = Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "mem_probe.txt";
var request, scenario, seconds, config, gui, snaps, lang, workers, finish;
var plantDense, denseFrom, makeSnaps, triplesFrom, commitLoop, known = true;

request = if(File.exists(requestPath)) {
	File.use(requestPath, "r", { |f| f.readAllString }).stripWhiteSpace.split($ )
} { ["idleGui", "60"] };
scenario = request[0].asSymbol;
seconds = (request[1] ? "60").asFloat.max(10);

File.mkdir(compassDir);
File.mkdir(snapDir);
(snapDir +/+ "*.cqsnap.scd").pathMatch.do { |p| File.delete(p) };
config = CQConfig.validate((headless: true, gui: (compassDir: compassDir)));
lang = NetAddr("127.0.0.1", NetAddr.langPort);
workers = List.new;

// mayhem's density is the case every finding in this phase was measured at
plantDense = { CQMatrix.presets[\mayhem][\matrix].do { |t|
	matrix.patch(t[0], t[1], t[2])
} };

denseFrom = { |phase|
	CQMatrix.destNames.collect { |dest, di|
		CQMatrix.sourceNames.collect { |src, si|
			(sin((si * 5.7) + (di * 2.3) + phase) * 0.55).round(0.01)
		}
	}
};

// The morph pair every morph scenario runs: probe_a and probe_b differ in one
// parameter and in the whole matrix, which is the soak's own case.
makeSnaps = {
	snaps = CQSnapshots(state, matrix, snapDir, { [0, 0, 0, 0, 0] }, { }, snapDir);
	state.set(\coco1Fb, 0.2);
	snaps.save("probe_a");
	matrix.writeMatrix(denseFrom.value(1.7));
	state.set(\coco1Fb, 1.1);
	snaps.save("probe_b");
	snaps
};

// [src, dest, weight] triples, the shape a snapshot file stores and prApply loads
triplesFrom = { |phase|
	var dense = denseFrom.value(phase);
	var triples = List.new;
	CQMatrix.destNames.do { |dest, di|
		CQMatrix.sourceNames.do { |src, si|
			triples.add([src, dest, dense[di][si]]);
		};
	};
	triples.asArray
};

// Alternating dense committed loads: exactly what a morph, a recall or a preset
// ends with, at whatever cadence the scenario asks for.
commitLoop = { |period|
	var fields = [triplesFrom.value(1.7), triplesFrom.value(0.3)];
	plantDense.value;
	workers.add(Routine {
		var flip = false;
		loop {
			flip = flip.not;
			matrix.load(fields[if(flip) { 0 } { 1 }], \snapshot, true, \morph);
			period.wait;
		};
	}.play(SystemClock));
};

finish = {
	workers.do { |w| w.stop };
	gui !? { gui.free };
	("CQ memory probe: scenario % ran % s".format(scenario, seconds)).postln;
	"CQ_MEM_PROBE_DONE".postln;
	AppClock.sched(0.3, { 0.exit; nil });
};

// ---- scenarios ----
//
// Each one isolates a suspect. The telemetry case matters most: in a real
// session the server pushes aura, meters, charge and phase frames at 15-20 Hz
// for the whole night, and none of the other measurements in this phase ever
// included that stream.

switch(scenario,

	// GUI alive, absolutely nothing happening. Anything that grows here grows
	// on its own, which would be the worst and simplest case.
	\idleGui, {
		plantDense.value;
		gui = CQGui(state, matrix, config);
	},

	// The ambient telemetry stream, at its real rates, with no server.
	\telemetryGui, {
		plantDense.value;
		gui = CQGui(state, matrix, config);
		workers.add(Routine {
			var frame = 0, aura, charge, spesal;
			loop {
				frame = frame + 1;
				aura = 16.collect { |i| (sin((frame * 0.05) + i) * 0.5 + 0.5) };
				charge = 9.collect { |i| (sin((frame * 0.03) + i) * 0.5 + 0.5) };
				lang.sendMsg(*(['/cq_aura', 0, 0] ++ aura));
				lang.sendMsg(*(['/cq_charge', 0] ++ charge));
				lang.sendMsg('/cq_phase', 0, 1, (frame % 100) / 100, 1, 4.0);
				if(frame % 4 == 0) {
					spesal = 5.collect { frame % 5 };
					lang.sendMsg(*(['/cq_spesal', 0, 0] ++ spesal));
				};
				(1/15).wait;
			};
		}.play(SystemClock));
		workers.add(Routine {
			loop {
				lang.sendMsg('/cq_meters', 0, 0, 0.4, 0, 0.4);
				lang.sendMsg('/cq_gr', 0, 0, 0.1, 0.1);
				lang.sendMsg('/cq_cocometers', 0, 1, 0.3, 0, 0.3);
				lang.sendMsg('/cq_cocometers', 0, 2, 0.3, 0, 0.3);
				(1/20).wait;
			};
		}.play(SystemClock));
	},

	// A matrix stream at morph rate, with the GUI listening.
	\matrixGui, {
		plantDense.value;
		gui = CQGui(state, matrix, config);
		workers.add(Routine {
			var phase = 0;
			loop {
				phase = phase + 0.05;
				matrix.writeMatrix(denseFrom.value(phase), origin: \code,
					committed: false, operation: \morphFrame);
				(1/15).wait;
			};
		}.play(SystemClock));
	},

	// The same stream with no GUI at all: language core versus interface.
	\matrixNoGui, {
		plantDense.value;
		workers.add(Routine {
			var phase = 0;
			loop {
				phase = phase + 0.05;
				matrix.writeMatrix(denseFrom.value(phase), origin: \code,
					committed: false, operation: \morphFrame);
				(1/15).wait;
			};
		}.play(SystemClock));
	},

	// Parameter writes through the funnel, GUI listening.
	\paramGui, {
		plantDense.value;
		gui = CQGui(state, matrix, config);
		workers.add(Routine {
			var frame = 0;
			loop {
				frame = frame + 1;
				state.setAll((
					coco1Fb: (sin(frame * 0.02) * 0.5 + 0.6),
					coco2Fb: (sin(frame * 0.03) * 0.5 + 0.6),
					coco1Speed: 12000 + (sin(frame * 0.01) * 8000),
					coco2Speed: 18000 + (sin(frame * 0.015) * 8000),
					qFreq: 4 + sin(frame * 0.02)
				));
				(1/15).wait;
			};
		}.play(SystemClock));
	},

	// Repeated snapshot morphs, the soak loop's own heaviest act.
	\morphGui, {
		plantDense.value;
		gui = CQGui(state, matrix, config);
		makeSnaps.value;
		workers.add(Routine {
			var flip = false;
			loop {
				flip = flip.not;
				if(flip) { snaps.morph("probe_a", "probe_b", 8) }
					{ snaps.morph("probe_b", "probe_a", 8) };
				9.wait;
			};
		}.play(SystemClock));
	},

	// ---- second stage (2026-07-31): the six above found that ONLY morphGui
	// grows, ~670 MB/h, so these three split that one workload apart. Each runs
	// a piece of the morph at the morph's own rate, so their MB/h are directly
	// comparable to morphGui's and to each other.

	// The same morph loop with no GUI. CQSpellbook is built by CQGuiDrawer and
	// its entry list has no cap, so this scenario is the one that says whether
	// the retention needs an interface at all.
	\morphNoGui, {
		plantDense.value;
		makeSnaps.value;
		workers.add(Routine {
			var flip = false;
			loop {
				flip = flip.not;
				if(flip) { snaps.morph("probe_a", "probe_b", 8) }
					{ snaps.morph("probe_b", "probe_a", 8) };
				9.wait;
			};
		}.play(SystemClock));
	},

	// The same morph loop with its acts crowded together: a 1.5 s morph every 2 s
	// instead of an 8 s morph every 9.
	//
	// The FRAME rate is 15 Hz either way, so this scales only the per-morph acts
	// -- two file reads, prApply's whole-parameter setAll, prApplyDiscrete, one
	// committed load -- by about 4.5x. If the growth morphGui still showed after
	// the journal cap is per act, it scales with this; if it is per frame, it does
	// not move. And 8 minutes here covers what ~36 would at the slower cadence,
	// which is how a plateau can be found without a long run.
	\morphGuiFast, {
		plantDense.value;
		gui = CQGui(state, matrix, config);
		makeSnaps.value;
		workers.add(Routine {
			var flip = false;
			loop {
				flip = flip.not;
				if(flip) { snaps.morph("probe_a", "probe_b", 1.5) }
					{ snaps.morph("probe_b", "probe_a", 1.5) };
				2.wait;
			};
		}.play(SystemClock));
	},

	// Only the two file reads a morph opens with, and nothing else. `read` ends
	// in `path.load`, which COMPILES the file -- a fresh parse tree and fresh
	// literals per call, twice per morph.
	\readNoGui, {
		plantDense.value;
		makeSnaps.value;
		workers.add(Routine {
			loop {
				snaps.read("probe_a");
				snaps.read("probe_b");
				9.wait;
			};
		}.play(SystemClock));
	},

	// Only the morph's final act: one committed dense matrix load per morph,
	// with the GUI listening. matrixGui's 15 Hz stream was uncommitted, which
	// CQSpellbook drops (CQSpellbook.sc:82) -- so this is the first scenario in
	// the whole probe that feeds its journal.
	\commitGui, {
		gui = CQGui(state, matrix, config);
		commitLoop.value(9);
	},

	// The same act once a second.
	//
	// CQSpellbook.lineLimit is 20 000 lines and one dense act writes ~352, so the
	// cap is only reached after ~57 acts -- which a 9 s cadence cannot manage
	// inside one probe window. The first verification run of the cap measured a
	// journal still filling and was therefore no verification at all: it showed
	// the same ramp as before the fix, correctly. At 1 s the limit is passed
	// during the warm-up, so the whole measured window is life after the cap, and
	// a plateau here is the plateau the cap promises.
	\commitGuiFast, {
		gui = CQGui(state, matrix, config);
		commitLoop.value(1);
	},

	// A misspelled name used to warn and then sit idle for the full duration,
	// and the report still called that row `ok` — a flat line that measured
	// nothing is indistinguishable from a flat line that measured something.
	// Measured 2026-07-31: `powershell -File` passes `-Scenarios a,b,c` as ONE
	// string, so the whole list arrived here as a single name and the run looked
	// clean. Refuse, and let the missing marker say so.
	{
		("CQ memory probe: unknown scenario '%'".format(scenario)).warn;
		"CQ_MEM_PROBE_UNKNOWN".postln;
		known = false;
	}
);

if(known) {
	AppClock.sched(seconds, { finish.value; nil });
} {
	// stdout is block-buffered when redirected: give the marker its tick out
	AppClock.sched(0.3, { 0.exit; nil });
};
)
