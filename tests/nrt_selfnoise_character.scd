// NRT \selfNoise ecology gate against the REAL Quantussy + Coco pair.
// The preset must grow from dust, make a genuine clearing and bloom again;
// a merely periodic or monotonically saturating noise sea fails.
// Prints CQ_SELFNOISE_OK / CQ_SELFNOISE_FAILED as its last line.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var tmpDir = repo +/+ "tests" +/+ "tmp";
var cfg = CQConfig.validate(());
var defs = CQSynthDefs.all(cfg);
var preset = CQMatrix.presets[\selfNoise];
var p = preset[\params];
var sr = 48000, dur = 120.0, frames = 131072;
var opts = ServerOptions.new;
var bC1 = 4, bC2 = 5, bSilent = 8, bQMix = 14, bModSrc = 16;
var busOffset = 100;
var results = List.new, ok = true;
var monitorDef, makeScore, render, analyze, check;

check = { |bool, msg|
	if(bool) { results.add("ok:   " ++ msg) } { ok = false; results.add("FAIL: " ++ msg) };
};

opts.numOutputBusChannels = 2;
opts.numInputBusChannels = 2;
opts.blockSize = cfg[\blockSize];
File.mkdir(tmpDir);

// Raw Coco stems, one per file channel. Master safety is already covered by
// nrt_safety; this gate needs to see whether the converter ecology itself rails.
monitorDef = SynthDef(\cqTestSelfNoiseMonitor, {
	Out.ar(0, [In.ar(\c1.ir(bC1)), In.ar(\c2.ir(bC2))]);
});

makeScore = {
	var score = Score.new;
	var destNames = CQMatrix.destNames, srcNames = CQMatrix.sourceNames;
	var wArr = destNames.collect { srcNames.collect { 0.0 } };
	var destBus = { |name| busOffset + (destNames.indexOf(name) * 16) };
	var rangeIndex = { |name| #[infra, balcium, audio].indexOf(p[name]) };
	var cocoArgs = { |buf, cross, out, tap, org, coco, speed|
		[\buf, buf, \preBus, bSilent, \deckBus, bSilent, \qMixBus, bQMix,
			\crossBus, cross, \outBus, out, \modSrcBus, bModSrc,
			\modTapIdx, tap, \modOrganIdx, org,
			\inLevel, p[("coco" ++ coco ++ "In").asSymbol],
			\fbLevel, p[("coco" ++ coco ++ "Fb").asSymbol],
			\dust, p[("coco" ++ coco ++ "Dust").asSymbol],
			\hold, 0, \organLvl, 0, \level, 0.8, \speed, speed,
			\slotPre, 0, \slotDeck, 0, \slotQ, 0,
			\slotX, p[("coco" ++ coco ++ "SlotX").asSymbol],
			\spafKnob, p[("coco" ++ coco ++ "Spaf").asSymbol],
			\bits, 8, \wrap, 0, \interp, 0, \patchDepth, 1.0,
			\wSpafBus, destBus.value(("coco" ++ coco ++ "_spaf").asSymbol),
			\wInVcaBus, destBus.value(("coco" ++ coco ++ "_inVca").asSymbol),
			\wFbVcaBus, destBus.value(("coco" ++ coco ++ "_fbVca").asSymbol),
			\wFlipBus, destBus.value(("coco" ++ coco ++ "_flip").asSymbol),
			\wSkipBus, destBus.value(("coco" ++ coco ++ "_skip").asSymbol)]
	};

	preset[\matrix].do { |t|
		var si = srcNames.indexOf(t[0]), di = destNames.indexOf(t[1]);
		wArr[di][si] = t[2];
	};
	defs.do { |d| score.add([0.0, [\d_recv, d.asBytes]]) };
	score.add([0.0, [\d_recv, monitorDef.asBytes]]);
	score.add([0.0, [\b_alloc, 0, frames, 1]]);
	score.add([0.0, [\b_alloc, 1, frames, 1]]);
	destNames.do { |name, di|
		score.add([0.0, [\c_setn, busOffset + (di * 16), 16] ++ wArr[di]])
	};
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqQuantussy, 1001, 1, 1000,
		\modSrcBus, bModSrc, \qMixBus, bQMix,
		\qFreq, p[\qFreq], \qChaos, p[\qChaos], \patchDepth, 1.0,
		\range1, rangeIndex.value(\c1Range),
		\range2, rangeIndex.value(\c2Range),
		\range3, rangeIndex.value(\c3Range),
		\range4, rangeIndex.value(\c4Range),
		\range5, 2,
		\send1, 0, \send2, 0, \send3, 0, \send4, 0, \send5, 0]]);
	score.add([0.0, ([\s_new, \cqCoco, 1002, 1, 1000]
		++ cocoArgs.value(0, bC2, bC1, 12, 14, 1, p[\coco1Speed]))]);
	score.add([0.0, ([\s_new, \cqCoco, 1003, 1, 1000]
		++ cocoArgs.value(1, bC1, bC2, 13, 15, 2, p[\coco2Speed]))]);
	score.add([0.0, [\s_new, \cqTestSelfNoiseMonitor, 1004, 1, 1000,
		\c1, bC1, \c2, bC2]]);
	score.add([dur, [\c_set, 0, 0]]);
	score
};

render = { |score, path|
	var done = false, waited = 0;
	score.recordNRT(nil, path, nil, sr, "WAV", "float", opts,
		duration: dur, action: { done = true });
	while { done.not and: { waited < 240 } } { 0.2.wait; waited = waited + 0.2 };
	if(done.not) { Error("selfNoise NRT render timed out: " ++ path).throw };
};

analyze = { |path|
	var sf = SoundFile.openRead(path), data, channels, winFrames = sr * 2;
	var windows = List.new, nearRail = 0, samples = 0, peak = 0.0, bad = false;
	var startFrame = sr * 10, endFrame, pos, sorted, early, late, growth, range;
	var reversals = 0, lastSign = 0, i, d, sign;
	var runningMax, drawdown = 0.0, clearingIndex = 0, rebloom, corrAt;
	if(sf.isNil) { Error("cannot read selfNoise NRT render: " ++ path).throw };
	channels = sf.numChannels;
	data = FloatArray.newClear(sf.numFrames * channels);
	sf.readData(data);
	endFrame = sf.numFrames;
	sf.close;

	pos = startFrame;
	while { (pos + winFrames) <= endFrame } {
		var sum2 = 0.0, j = 0, l, r, v;
		while { j < winFrames } {
			l = data[((pos + j) * channels)];
			r = data[((pos + j) * channels) + 1];
			bad = bad or: { l.isNaN or: { r.isNaN
				or: { l.abs == inf or: { r.abs == inf } } } };
			v = ((l.squared + r.squared) * 0.5).sqrt;
			sum2 = sum2 + v.squared;
			peak = max(peak, max(l.abs, r.abs));
			if(max(l.abs, r.abs) >= 0.72) { nearRail = nearRail + 1 };
			samples = samples + 1;
			j = j + 1;
		};
		windows.add((sum2 / winFrames).sqrt.ampdb);
		pos = pos + winFrames;
	};

	sorted = windows.asArray.sort;
	early = windows.copyRange(0, 9).mean;
	late = windows.copyRange(windows.size - 15, windows.size - 1).mean;
	growth = late - early;
	range = sorted[(sorted.size * 0.9).floor] - sorted[(sorted.size * 0.1).floor];
	runningMax = windows[0];
	windows.do { |value, index|
		if((runningMax - value) > drawdown) {
			drawdown = runningMax - value;
			clearingIndex = index;
		};
		runningMax = max(runningMax, value);
	};
	rebloom = windows.copyRange(clearingIndex, windows.size - 1).maxItem
		- windows[clearingIndex];

	i = 1;
	while { i < windows.size } {
		d = windows[i] - windows[i - 1];
		sign = if(d > 0.25) { 1 } { if(d < -0.25) { -1 } { 0 } };
		if((sign != 0) and: { lastSign != 0 } and: { sign != lastSign }) {
			reversals = reversals + 1
		};
		if(sign != 0) { lastSign = sign };
		i = i + 1;
	};

	corrAt = { |lag|
		var n = windows.size - lag, ma = 0.0, mb = 0.0;
		var num = 0.0, da = 0.0, db = 0.0, j = 0, a, b;
		n.do { |k| ma = ma + windows[k]; mb = mb + windows[k + lag] };
		ma = ma / n; mb = mb / n;
		while { j < n } {
			a = windows[j] - ma; b = windows[j + lag] - mb;
			num = num + (a * b); da = da + a.squared; db = db + b.squared;
			j = j + 1;
		};
		num / max((da * db).sqrt, 1e-12)
	};

	(
		windows: windows,
		early: early,
		late: late,
		growth: growth,
		range: range,
		drawdown: drawdown,
		rebloom: rebloom,
		reversals: reversals,
		corr10: corrAt.value(5),
		peak: peak,
		railFraction: nearRail / samples,
		bad: bad
	)
};

{
	var path = tmpDir +/+ "nrt_selfnoise_character.wav";
	var stats;
	try {
		"CQ NRT: rendering 120 s selfNoise ecology...".postln;
		render.value(makeScore.value, path);
		stats = analyze.value(path);
		("selfNoise: early % dB late % dB growth % dB range % dB clearing % dB rebloom % dB reversals % corr10 % peak % rail %")
			.format(
				stats[\early].round(0.01), stats[\late].round(0.01),
				stats[\growth].round(0.01), stats[\range].round(0.01),
				stats[\drawdown].round(0.01), stats[\rebloom].round(0.01),
				stats[\reversals], stats[\corr10].round(0.01),
				stats[\peak].round(0.001), stats[\railFraction].round(0.0001)
			).postln;
		check.value(stats[\bad].not, "selfNoise render has no NaN/inf");
		check.value(stats[\late] > -40, "late selfNoise ecology is not silent");
		check.value(stats[\growth].inclusivelyBetween(-3, 8),
			"120 s ecology neither dies nor climbs monotonically into saturation");
		check.value(stats[\range].inclusivelyBetween(4, 12),
			"2 s loudness windows retain useful 4..12 dB weather");
		check.value(stats[\drawdown] >= 4,
			"an established bloom makes a genuine >= 4 dB clearing");
		check.value(stats[\rebloom] >= 4,
			"the ecology blooms >= 4 dB again after its deepest clearing");
		check.value(stats[\reversals] >= 4,
			"the envelope changes direction repeatedly");
		check.value(stats[\corr10] <= 0.8,
			"10 s envelope correlation stays below ocean-swell regularity");
		check.value(stats[\peak] <= 0.8001,
			"raw Coco stems remain converter-bounded");
		check.value(stats[\railFraction] <= 0.005,
			"selfNoise spends <= 0.5% of samples near the converter rail");
	} { |err|
		ok = false;
		results.add("FAIL: uncaught selfNoise NRT error: " ++ err.errorString);
	};

	results.do(_.postln);
	if(ok) { "CQ_SELFNOISE_OK".postln; 0.exit } {
		"CQ_SELFNOISE_FAILED".postln; 1.exit
	};
}.fork(AppClock);
)
