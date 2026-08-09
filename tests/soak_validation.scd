// soak_validation.scd -- production-path validation for the replacement SOAK-1
// memory criterion. This script is launched only through run_sclang_gate.ps1.
// tests/run_soak_validation.ps1 owns its request, ready and stop files.

(
var root = Cocoquantus.repoDir;
var tmpDir = root +/+ "tests" +/+ "tmp";
var requestPath = tmpDir +/+ "soak_validation_request.txt";
var readyPath = tmpDir +/+ "soak_validation.ready";
var stopPath = tmpDir +/+ "soak_validation.stop";
var snapDir = tmpDir +/+ "soak_validation_snapshots";
var compassDir = tmpDir +/+ "soak_validation_compass";
var request, duration, warmup, runConfig;
var activity, stopWatcher, finished = false, finish, postMemory;

request = if(File.exists(requestPath)) {
	File.use(requestPath, "r", { |f| f.readAllString }).stripWhiteSpace.split($ )
} { ["5880", "3600"] };
duration = (request[0] ? "5880").asFloat.max(60);
warmup = (request[1] ? "3600").asFloat.clip(0, duration - 10);

File.mkdir(tmpDir);
File.mkdir(snapDir);
File.mkdir(compassDir);
if(File.exists(readyPath)) { File.delete(readyPath) };
if(File.exists(stopPath)) { File.delete(stopPath) };
(snapDir +/+ "*.cqsnap.scd").pathMatch.do { |path| File.delete(path) };

postMemory = { |label|
	var panel, book;
	panel = Cocoquantus.gui;
	book = panel !? { panel.drawer.spellbook };
	("CQ_SOAK_MEMORY % spellbookEntries % spellbookLines %"
		.format(label, book !? { book.entryCount }, book !? { book.lineCount })).postln;
	Object.gcInfo;
};

finish = {
	if(finished.not) {
		finished = true;
		activity !? { activity.stop };
		// finish may be called by stopWatcher itself. `finished` ends that
		// routine naturally; stopping the current Routine here aborts teardown.
		postMemory.value(\measuredWindowEnd);
		CQ.free({
			Server.default.quit;
			AppClock.sched(1.0, {
				"CQ soak validation: clean teardown".postln;
				"CQ_SOAK_VALIDATION_DONE".postln;
				AppClock.sched(0.3, { 0.exit; nil });
				nil
			});
		});
	};
};

runConfig = Cocoquantus.prLoadConfig(root +/+ "config.scd").copy;
runConfig[\sixteenN] = runConfig[\sixteenN].copy;
runConfig[\sixteenN][\enabled] = false;
runConfig[\folders] = runConfig[\folders].copy;
// Cocoquantus.prFolder resolves this relative to repoDir. A mixed-separator
// Windows absolute path is not accepted by PathName.isAbsolutePath.
runConfig[\folders][\snapshots] = "tests/tmp/soak_validation_snapshots";
runConfig[\gui] = (runConfig[\gui] ? ()).copy;
runConfig[\gui][\compassDir] = compassDir;
runConfig[\headless] = false;

CQ.boot(nil, {
	var startedAt = Main.elapsedTime;
	CQ.panic;
	CQ.patchPreset(\selfNoise);
	CQ.patchPreset(\mayhem);
	CQ.snap(\soak_validation_a);
	CQ.shake(0.3, density: 0.35);
	CQ.snap(\soak_validation_b);

	activity = Routine({
		var step = 0;
		while({ finished.not }, {
			step = step + 1;
			CQ.shake(0.3, density: 1.0);
			switch(step % 8,
				1, { CQ.set(\coco1Speed, rrand(6000, 30000)) },
				2, { CQ.set(\coco2Speed, rrand(6000, 30000)) },
				3, { CQ.set(\coco1Fb, rrand(0.2, 0.95)) },
				4, { CQ.hold(1, step.div(8) % 2) },
				5, { if(step.div(8).odd) { CQ.deckA.random } { CQ.deckB.random } },
				6, { CQ.flip(1, [0, 1].choose); CQ.skip(2) },
				7, { CQ.shake(0.25, density: 0.2) },
				0, { CQ.morph(\soak_validation_a, \soak_validation_b, 20) }
			);
			60.wait;
		});
	}).play(SystemClock);

	stopWatcher = Routine({
		while({ finished.not }, {
			if(File.exists(stopPath) or: {
				(Main.elapsedTime - startedAt) >= duration
			}) {
				finish.value;
			};
			1.wait;
		});
	}).play(SystemClock);

	AppClock.sched(warmup, {
		if(finished.not) { postMemory.value(\measuredWindowStart) };
		nil
	});
	File.use(readyPath, "w", { |file| file.write("ready") });
	("CQ_SOAK_VALIDATION_READY duration % warmup %".format(duration, warmup)).postln;
}, runConfig, { |failure|
	("ERROR: soak validation boot failed: " ++ failure.errorString).postln;
	AppClock.sched(0.3, { 1.exit; nil });
}, { CQ.panic });
)
