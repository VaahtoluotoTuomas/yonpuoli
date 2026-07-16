// NRT safety render (SPEC §10 A3 — phase-1 subset). Renders the REAL SynthDefs
// offline (scsynth -N, no audio device) under worst-case input and asserts:
// peak <= -0.75 dBFS, no NaN/inf, |DC| <= 0.02. Thresholds may NEVER be raised
// (CLAUDE.md safety #6). Phase 4 extends this to the full \mayhem 90 s case.
// Prints CQ_SAFETY_PASSED / CQ_SAFETY_FAILED as its last line.
// Run: "<sclang.exe>" -l tests/sclang_conf_dev.yaml -i none tests/nrt_safety.scd

(
var repo = thisProcess.nowExecutingPath.dirname.dirname;
var tmpDir = repo +/+ "tests" +/+ "tmp";
var cfg = CQConfig.validate(());       // deterministic defaults, never the user's config
var defs = CQSynthDefs.all(cfg);
var dur = 10.0;
var sr = 48000;
var opts = ServerOptions.new;
var results = List.new;
var ok = true;
var check = { |bool, msg|
	if(bool) { results.add("ok:   " ++ msg) } { ok = false; results.add("FAIL: " ++ msg) };
};

// NRT bus plan (in = 2, out = 2 -> private audio buses start at index 4)
var bCoco1 = 4, bCoco2 = 5, bSpaceRet = 6, bDry = 8;
var bDeckA = 10, bDeckB = 11, bPreA = 12, bPreB = 13, bModSrc = 16;

// what a runaway patch could hand the master: 4x full scale + DC offsets
var tortureDef = SynthDef(\cqTestTorture, {
	Out.ar(\c1.ir(4), WhiteNoise.ar(4) + DC.ar(0.5));
	Out.ar(\c2.ir(5), WhiteNoise.ar(4) - DC.ar(0.5));
	Out.ar(\dry.ir(8), PinkNoise.ar([2, 2]) + DC.ar([1, -1]));
});
var nanDef = SynthDef(\cqTestNan, {
	var bad = DC.ar(0) / DC.ar(0);     // NaN generator (0/0)
	Out.ar(\c1.ir(4), bad);
	Out.ar(\c2.ir(5), bad);
});

var makeScore = { |testDefName|
	var score = Score.new;
	defs.do { |d| score.add([0.0, [\d_recv, d.asBytes]]) };
	score.add([0.0, [\d_recv, tortureDef.asBytes]]);
	score.add([0.0, [\d_recv, nanDef.asBytes]]);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqInput, 1001, 1, 1000,
		\deckABus, bDeckA, \deckBBus, bDeckB, \modSrcBus, bModSrc,
		\preABus, bPreA, \preBBus, bPreB, \dryBus, bDry]]);
	score.add([0.0, [\s_new, testDefName, 1002, 1, 1000,
		\c1, bCoco1, \c2, bCoco2, \dry, bDry]]);
	score.add([0.0, [\s_new, \cqMaster, 1003, 1, 1000,
		\coco1Bus, bCoco1, \coco2Bus, bCoco2, \spaceRetBus, bSpaceRet, \dryBus, bDry,
		\masterVol, 0, \masterDry, 1, \fadeIn, 1]]);   // vol maxed = worst case
	score.add([dur, [\c_set, 0, 0]]);
	score
};

var render = { |score, outPath|
	var done = false, waited = 0;
	score.recordNRT(nil, outPath, nil, sr, "WAV", "float", opts,
		duration: dur, action: { done = true });
	while { done.not and: { waited < 120 } } { 0.2.wait; waited = waited + 0.2 };
	if(done.not) { Error("CQ NRT: render timed out: " ++ outPath).throw };
};

var analyze = { |path|
	var sf = SoundFile.openRead(path);
	var data, total, peak, sumL = 0.0, sumR = 0.0, i, n;
	if(sf.isNil) { Error("CQ NRT: cannot open render: " ++ path).throw };
	data = FloatArray.newClear(sf.numFrames * sf.numChannels);
	sf.readData(data);
	sf.close;
	n = max(1, sf.numFrames);
	total = data.sum;                       // NaN/inf poisons the sum -> cheap bad check
	peak = data.abs.maxItem ? 0;
	i = 0;
	while { i < data.size } { sumL = sumL + data[i]; i = i + 2 };
	i = 1;
	while { i < data.size } { sumR = sumR + data[i]; i = i + 2 };
	(peak: peak, dcL: sumL / n, dcR: sumR / n,
		bad: total.isNaN or: { total.abs == inf }, frames: sf.numFrames)
};

opts.numOutputBusChannels = 2;
opts.numInputBusChannels = 2;
opts.blockSize = cfg[\blockSize];

File.mkdir(tmpDir);

{
	var r1, r2;
	var ceiling = (-0.75).dbamp;
	var p1 = tmpDir +/+ "nrt_safety_torture.wav";
	var p2 = tmpDir +/+ "nrt_safety_nan.wav";

	try {
		"CQ NRT: rendering torture case (% s)...".format(dur).postln;
		render.value(makeScore.value(\cqTestTorture), p1);
		r1 = analyze.value(p1);
		"torture: peak %  dcL %  dcR %  frames %".format(
			r1[\peak].round(0.0001), r1[\dcL].round(0.00001),
			r1[\dcR].round(0.00001), r1[\frames]).postln;
		check.value(r1[\frames] > 0, "torture render produced audio");
		check.value(r1[\bad].not, "torture render has no NaN/inf");
		check.value(r1[\peak] <= ceiling,
			"peak % <= -0.75 dBFS".format(r1[\peak].round(0.0001)));
		check.value(r1[\peak] >= 0.25,
			"graph carried signal (peak % >= 0.25, not failed-to-silence)"
				.format(r1[\peak].round(0.0001)));
		check.value(r1[\dcL].abs <= 0.02, "|DC left| <= 0.02");
		check.value(r1[\dcR].abs <= 0.02, "|DC right| <= 0.02");

		"CQ NRT: rendering NaN-injection case...".postln;
		render.value(makeScore.value(\cqTestNan), p2);
		r2 = analyze.value(p2);
		check.value(r2[\bad].not, "injected NaN never reaches the output");
		check.value(r2[\peak] <= ceiling, "NaN case stays under the ceiling");
	} { |err|
		ok = false;
		results.add("FAIL: uncaught error: " ++ err.errorString);
	};

	results.do(_.postln);
	if(ok) { "CQ_SAFETY_PASSED".postln; 0.exit } { "CQ_SAFETY_FAILED".postln; 1.exit };
}.fork(AppClock);
)
