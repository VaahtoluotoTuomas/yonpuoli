// NRT hold-integrity renders (SPEC §10 A4 + A5) against the REAL \cqCoco def.
// A4: capture a loop through the record path, hold for ~60 s while sweeping
//     speed ±2 octaves and back -> buffer dumps before/after must correlate
//     >= 0.999 (lossless hold; the identity rewrite must be bit-exact).
// A5: unity looper — fb = 1.0, one pass of an impulse train, then 30 s of
//     recirculation -> repeat energy stays within ±1 dB (no level drift).
// Prints CQ_HOLD_OK / CQ_HOLD_FAILED as its last line.
// Run: "<sclang.exe>" -l tests/sclang_conf_dev.yaml -i none tests/nrt_hold_integrity.scd

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
// ^ realpath: nowExecutingPath is relative when sclang gets a relative script
// argument — bare dirname would then write renders to tests/tests/tmp
var tmpDir = repo +/+ "tests" +/+ "tmp";
var cfg = CQConfig.validate(());
var defs = CQSynthDefs.all(cfg);
var sr = 48000;
var opts = ServerOptions.new;
var results = List.new;
var ok = true;
var check = { |bool, msg|
	if(bool) { results.add("ok:   " ++ msg) } { ok = false; results.add("FAIL: " ++ msg) };
};

// NRT bus plan (out = 2, in = 2 -> private buses start at 4)
var bSilent = 8, bSink = 9, bFeed = 10, bModSrc = 16;
var bufFrames = 65536;                       // small RAM for fast renders

var a4Clock = 24000;                         // capture pass = 65536/24000 = 2.73 s
var a4Dur = 60.0;
var a5Clock = 48000;                         // ratio exactly 1.0 at SR 48000
var a5LoopDur = bufFrames / a5Clock;         // = 65536 samples = 1.365 s
var a5Dur = 32.0;

// A4 capture source: a three-partial drone (deterministic, non-silent)
var srcDef = SynthDef(\cqTestHoldSrc, {
	Out.ar(\out.ir(10), SinOsc.ar([220, 330, 550], 0, [0.3, 0.2, 0.15]).sum);
});
// A5 source: impulse train gated to exactly one buffer pass; 0.5 sits exactly
// on the 8-bit grid, so unity feedback must recirculate it without drift
var impDef = SynthDef(\cqTestImpulse, {
	var gate = Sweep.ar(0, 1) < \gateDur.ir(1);
	Out.ar(\out.ir(10), Impulse.ar(50, 0, 0.5) * gate);
});

var cocoBase = { |extra|
	[\buf, 0, \preBus, bSilent, \deckBus, bFeed, \qMixBus, bSilent,
		\crossBus, bSilent, \modSrcBus, bModSrc, \modTapIdx, 12, \modOrganIdx, 14,
		\inLevel, 1.0, \dust, 0, \organLvl, 0, \level, 1.0,
		\slotPre, 0, \slotDeck, 1.0, \slotQ, 0, \slotX, 0] ++ extra
};

var dump1 = tmpDir +/+ "nrt_hold_a4_before.wav";
var dump2 = tmpDir +/+ "nrt_hold_a4_after.wav";

var makeA4Score = {
	var score = Score.new;
	defs.do { |d| score.add([0.0, [\d_recv, d.asBytes]]) };
	score.add([0.0, [\d_recv, srcDef.asBytes]]);
	score.add([0.0, [\b_alloc, 0, bufFrames, 1]]);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqTestHoldSrc, 1001, 1, 1000, \out, bFeed]]);
	score.add([0.0, ([\s_new, \cqCoco, 1002, 1, 1000] ++ cocoBase.value([
		\outBus, bSink, \speed, a4Clock, \fbLevel, 0, \hold, 0]))]);
	score.add([3.0, [\n_set, 1002, \hold, 1]]);      // > one full capture pass
	score.add([3.2, [\b_write, 0, dump1, "WAV", "float", -1, 0, 0]]);
	// ±2 oct sine-shaped sweep and back, one step per 0.5 s; the def's own
	// 1500 Hz..SR clamp is allowed to engage — hold must survive regardless
	108.do { |i|
		score.add([4.0 + (i * 0.5), [\n_set, 1002,
			\speed, (a4Clock * (2 ** (2 * sin(2pi * i / 108)))).round(1)]]);
	};
	score.add([59.0, [\b_write, 0, dump2, "WAV", "float", -1, 0, 0]]);
	score.add([a4Dur, [\c_set, 0, 0]]);
	score
};

var makeA5Score = {
	var score = Score.new;
	defs.do { |d| score.add([0.0, [\d_recv, d.asBytes]]) };
	score.add([0.0, [\d_recv, impDef.asBytes]]);
	score.add([0.0, [\b_alloc, 0, bufFrames, 1]]);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqTestImpulse, 1001, 1, 1000,
		\out, bFeed, \gateDur, a5LoopDur]]);
	score.add([0.0, ([\s_new, \cqCoco, 1002, 1, 1000] ++ cocoBase.value([
		\outBus, 0, \speed, a5Clock, \fbLevel, 1.0, \hold, 0]))]);
	score.add([a5Dur, [\c_set, 0, 0]]);
	score
};

var render = { |score, outPath, dur|
	var done = false, waited = 0;
	score.recordNRT(nil, outPath, nil, sr, "WAV", "float", opts,
		duration: dur, action: { done = true });
	while { done.not and: { waited < 240 } } { 0.2.wait; waited = waited + 0.2 };
	if(done.not) { Error("CQ NRT: render timed out: " ++ outPath).throw };
};

var readMono = { |path|
	var sf = SoundFile.openRead(path);
	var data;
	if(sf.isNil) { Error("CQ NRT: cannot open " ++ path).throw };
	data = FloatArray.newClear(sf.numFrames * sf.numChannels);
	sf.readData(data);
	sf.close;
	[data, sf.numChannels]
};

var pearson = { |a, b|
	var n = a.size, ma = a.sum / n, mb = b.sum / n;
	var num = 0.0, da = 0.0, db = 0.0, i = 0, xa, xb;
	while { i < n } {
		xa = a[i] - ma; xb = b[i] - mb;
		num = num + (xa * xb); da = da + (xa * xa); db = db + (xb * xb);
		i = i + 1;
	};
	if((da <= 0) or: { db <= 0 }) { 0.0 } { num / (da * db).sqrt }
};

// RMS of output channel 0 over one loop-pass window starting at pass k
var windowRms = { |data, numCh, k|
	var start = k * bufFrames, sum = 0.0, i = 0, v;
	while { i < bufFrames } {
		v = data[(start + i) * numCh];
		sum = sum + (v * v);
		i = i + 1;
	};
	(sum / bufFrames).sqrt
};

opts.numOutputBusChannels = 2;
opts.numInputBusChannels = 2;
opts.blockSize = cfg[\blockSize];

File.mkdir(tmpDir);

{
	var d1, d2, corr, peak1, peak2, a5, rmsFirst, rmsLast, dbDiff;
	var a4Out = tmpDir +/+ "nrt_hold_a4_render.wav";
	var a5Out = tmpDir +/+ "nrt_hold_a5_render.wav";

	try {
		"CQ NRT: rendering A4 lossless hold (% s, speed sweep +/-2 oct)..."
			.format(a4Dur).postln;
		render.value(makeA4Score.value, a4Out, a4Dur);
		d1 = readMono.value(dump1)[0];
		d2 = readMono.value(dump2)[0];
		peak1 = d1.abs.maxItem ? 0;
		peak2 = d2.abs.maxItem ? 0;
		corr = pearson.value(d1, d2);
		"A4: peak(before) %  peak(after) %  correlation %".format(
			peak1.round(0.001), peak2.round(0.001), corr.round(0.000001)).postln;
		check.value(d1.size == bufFrames, "A4 dump 1 has % frames".format(bufFrames));
		check.value(peak1 >= 0.05, "A4 captured loop is not silent");
		check.value(peak2 >= 0.05, "A4 held loop is not silent after the sweep");
		check.value(corr >= 0.999,
			"A4 hold is lossless: correlation % >= 0.999".format(corr.round(0.000001)));

		"CQ NRT: rendering A5 unity looper (% s)...".format(a5Dur).postln;
		render.value(makeA5Score.value, a5Out, a5Dur);
		a5 = readMono.value(a5Out);
		check.value((a5[0].sum.isNaN or: { a5[0].sum.abs == inf }).not,
			"A5 render has no NaN/inf");
		rmsFirst = windowRms.value(a5[0], a5[1], 1);    // first full repeat
		rmsLast = windowRms.value(a5[0], a5[1], 21);    // ~30 s of recirculation
		dbDiff = if((rmsFirst > 0) and: { rmsLast > 0 }) {
			(rmsLast / rmsFirst).ampdb
		} { inf };
		"A5: rms(first) %  rms(pass 21) %  drift % dB".format(
			rmsFirst.round(0.00001), rmsLast.round(0.00001),
			dbDiff.round(0.001)).postln;
		check.value(rmsFirst >= 1e-4, "A5 first repeat carries signal");
		check.value(dbDiff.abs <= 1.0,
			"A5 unity-looper energy drift % dB within +/-1 dB".format(dbDiff.round(0.001)));
	} { |err|
		ok = false;
		results.add("FAIL: uncaught error: " ++ err.errorString);
	};

	results.do(_.postln);
	if(ok) { "CQ_HOLD_OK".postln; 0.exit } { "CQ_HOLD_FAILED".postln; 1.exit };
}.fork(AppClock);
)
