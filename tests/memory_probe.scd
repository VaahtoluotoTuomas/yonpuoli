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
var plantDense, denseFrom;

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
		snaps = CQSnapshots(state, matrix, snapDir, { [0, 0, 0, 0, 0] }, { },
			snapDir);
		state.set(\coco1Fb, 0.2);
		snaps.save("probe_a");
		matrix.writeMatrix(denseFrom.value(1.7));
		state.set(\coco1Fb, 1.1);
		snaps.save("probe_b");
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

	{
		("CQ memory probe: unknown scenario '%'".format(scenario)).warn;
	}
);

AppClock.sched(seconds, { finish.value; nil });
)
