// NRT regression for the six software-native Coco matrix inputs.
// Two real \cqCoco instances per destination prove that:
//   - coco1/2_organ can open a closed audible Organ calyx;
//   - coco1/2_slotQ can open a closed Quantussy input gill;
//   - coco1/2_record invert either hand-set Hold state for the gate duration:
//     a sealed writer opens and an open writer freezes, without changing Hold.
// A matching all-zero render proves none of those signals leaks through the
// manual controls by itself. Prints CQ_MATRIX_INPUTS_OK / _FAILED last.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var tmpDir = repo +/+ "tests" +/+ "tmp";
var cfg = CQConfig.validate(());
var defs = CQSynthDefs.all(cfg);
var sr = 48000, dur = 3.0, analysisStart = 0.5;
var opts = ServerOptions.new;
var results = List.new, ok = true;
var check = { |condition, message|
	if(condition) { results.add("ok:   " ++ message) } {
		ok = false;
		results.add("FAIL: " ++ message)
	};
};

// Six disjoint 16-channel modulation fields. The production graph shares one
// field; isolation here makes a wrong per-Coco destination argument impossible
// to hide behind another test voice.
var modBuses = #[16, 32, 48, 64, 80, 96];
var qBus = 120, preBus = 121, silentBus = 122;
var zeroWeights = 100;
var routeBuses = #[116, 132, 148, 164, 180, 196];
var routeWeights = #[0.30, 0.34, 0.38, 0.42, 0.70, 0.76];
var bufFrames = 4096;

var sourceDef = SynthDef(\cqTestMatrixInputSources, {
	var slowA = LFTri.ar(1.7, 0);
	var slowB = LFTri.ar(2.3, 0.31);
	var high = DC.ar(1);
	Out.ar(\mod1.ir(16), [slowA] ++ (0 ! 15));
	Out.ar(\mod2.ir(32), [slowB] ++ (0 ! 15));
	Out.ar(\mod3.ir(48), [slowA] ++ (0 ! 15));
	Out.ar(\mod4.ir(64), [slowB] ++ (0 ! 15));
	Out.ar(\mod5.ir(80), [high] ++ (0 ! 15));
	Out.ar(\mod6.ir(96), [high] ++ (0 ! 15));
	Out.ar(\qBus.ir(120), SinOsc.ar(183, 0, 0.55));
	Out.ar(\preBus.ir(121), SinOsc.ar(271, 0, 0.50));
});

var commonArgs = { |buf, out, modBus, cocoIndex, wOrgan, wSlotQ, wRecord|
	[
		\buf, buf, \outBus, out, \modSrcBus, modBus,
		\modTapIdx, if(cocoIndex == 0) { 12 } { 13 },
		\modOrganIdx, if(cocoIndex == 0) { 14 } { 15 },
		\cocoIndex, cocoIndex,
		\deckBus, silentBus, \crossBus, silentBus,
		\wSpafBus, zeroWeights, \wInVcaBus, zeroWeights,
		\wFbVcaBus, zeroWeights, \wFlipBus, zeroWeights,
		\wSkipBus, zeroWeights, \wOrganBus, wOrgan,
		\wSlotQBus, wSlotQ, \wRecordBus, wRecord,
		\patchDepth, 1, \speed, if(cocoIndex == 0) { 18000 } { 24000 },
		\spafKnob, 0, \fbLevel, 0, \dust, 0,
		\organLvl, 0, \level, 1, \slotDeck, 0, \slotX, 0
	]
};

var makeScore = { |active = true|
	var score = Score.new;
	var zero = 0.0 ! 16;
	defs.do { |def| score.add([0.0, [\d_recv, def.asBytes]]) };
	score.add([0.0, [\d_recv, sourceDef.asBytes]]);
	8.do { |i| score.add([0.0, [\b_alloc, i, bufFrames, 1]]) };
	score.add([0.0, [\c_setn, zeroWeights, 16] ++ zero]);
	routeBuses.do { |bus, i|
		var weights = 0.0 ! 16;
		if(active) { weights[0] = routeWeights[i] };
		score.add([0.0, [\c_setn, bus, 16] ++ weights]);
	};
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqTestMatrixInputSources, 1001, 1, 1000,
		\mod1, modBuses[0], \mod2, modBuses[1],
		\mod3, modBuses[2], \mod4, modBuses[3],
		\mod5, modBuses[4], \mod6, modBuses[5],
		\qBus, qBus, \preBus, preBus]]);

	// Organ pair: empty held RAM and a closed manual calyx. Only wOrgan can speak.
	2.do { |i|
		score.add([0.0, [\s_new, \cqCoco, 1010 + i, 1, 1000]
			++ commonArgs.value(i, i, modBuses[i], i,
				routeBuses[i], zeroWeights, zeroWeights)
			++ [\preBus, silentBus, \qMixBus, silentBus,
				\inLevel, 0, \slotPre, 0, \slotQ, 0, \hold, 1]]);
	};

	// Record-toggle pair: with Hold=0 the same live feed normally fills RAM.
	// A high matrix gate must instead select identity rewrite and keep it silent.
	2.do { |i|
		score.add([0.0, [\s_new, \cqCoco, 1040 + i, 1, 1000]
			++ commonArgs.value(i + 6, i + 6, modBuses[i + 4], i,
				zeroWeights, zeroWeights, routeBuses[i + 4])
			++ [\preBus, preBus, \qMixBus, silentBus,
				\inLevel, 1, \slotPre, 1, \slotQ, 0, \hold, 0]]);
	};

	// SlotQ pair: qMix exists, but its manual gill is closed. Matrix VCA opens it.
	2.do { |i|
		score.add([0.0, [\s_new, \cqCoco, 1020 + i, 1, 1000]
			++ commonArgs.value(i + 2, i + 2, modBuses[i + 2], i,
				zeroWeights, routeBuses[i + 2], zeroWeights)
			++ [\preBus, silentBus, \qMixBus, qBus,
				\inLevel, 1, \slotPre, 0, \slotQ, 0, \hold, 0]]);
	};

	// Record pair: a live preamp feed meets Hold=1. Only the matrix punch may
	// crack the seal; the persistent control remains one for the whole score.
	2.do { |i|
		score.add([0.0, [\s_new, \cqCoco, 1030 + i, 1, 1000]
			++ commonArgs.value(i + 4, i + 4, modBuses[i + 4], i,
				zeroWeights, zeroWeights, routeBuses[i + 4])
			++ [\preBus, preBus, \qMixBus, silentBus,
				\inLevel, 1, \slotPre, 1, \slotQ, 0, \hold, 1]]);
	};
	score.add([dur, [\c_set, 0, 0]]);
	score
};

var render = { |score, path|
	var done = false, waited = 0.0;
	score.recordNRT(nil, path, nil, sr, "WAV", "float", opts,
		duration: dur, action: { done = true });
	while { done.not and: { waited < 120 } } {
		0.2.wait;
		waited = waited + 0.2;
	};
	if(done.not) { Error("matrix-input NRT timed out: " ++ path).throw };
};

var statsFor = { |path|
	var sound = SoundFile.openRead(path), data, channels, frames, start;
	var sums, peaks, finite = true, count;
	if(sound.isNil) { Error("cannot read matrix-input NRT: " ++ path).throw };
	channels = sound.numChannels;
	frames = sound.numFrames;
	data = FloatArray.newClear(frames * channels);
	sound.readData(data);
	sound.close;
	start = (analysisStart * sr).asInteger.min(frames - 1);
	count = max(1, frames - start);
	sums = 0.0 ! channels;
	peaks = 0.0 ! channels;
	(start .. (frames - 1)).do { |frame|
		channels.do { |channel|
			var value = data[(frame * channels) + channel];
			if(value.isNaN or: { value.abs == inf }) { finite = false };
			sums[channel] = sums[channel] + value.squared;
			peaks[channel] = max(peaks[channel], value.abs);
		};
	};
	(channels: channels, frames: frames, finite: finite,
		rms: sums.collect { |sum| (sum / count).sqrt }, peaks: peaks)
};

opts.numOutputBusChannels = 8;
opts.numInputBusChannels = 0;
opts.blockSize = cfg[\blockSize];
opts.verbosity = -1;
File.mkdir(tmpDir);

{
	var baselinePath = tmpDir +/+ "nrt_matrix_inputs_baseline.wav";
	var activePath = tmpDir +/+ "nrt_matrix_inputs_active.wav";
	var baseline, active;
	var labels = #[coco1_organ, coco2_organ, coco1_slotQ, coco2_slotQ,
		coco1_record_open, coco2_record_open,
		coco1_record_close, coco2_record_close];
	try {
		render.value(makeScore.value(false), baselinePath);
		render.value(makeScore.value(true), activePath);
		baseline = statsFor.value(baselinePath);
		active = statsFor.value(activePath);
		("matrix inputs RMS baseline % | active %")
			.format(baseline[\rms].collect(_.round(0.000001)),
				active[\rms].collect(_.round(0.000001))).postln;
		check.value(baseline[\channels] == 8 and: { active[\channels] == 8 },
			"both renders contain six destinations plus both Record directions");
		check.value(baseline[\finite] and: { active[\finite] },
			"all six destination renders are finite");
		labels.keep(6).do { |label, i|
			check.value(baseline[\rms][i] <= 1e-7,
				"% stays silent with its manual seal closed and no matrix cable"
					.format(label));
			check.value(active[\rms][i] >= 0.005,
				"% produces audible signal through its own matrix input (RMS %)"
					.format(label, active[\rms][i].round(0.000001)));
			check.value(active[\peaks][i] <= 1.01,
				"% remains bounded (peak %)"
					.format(label, active[\peaks][i].round(0.000001)));
		};
		labels.drop(6).do { |label, j|
			var i = j + 6;
			check.value(baseline[\rms][i] >= 0.005,
				"% writes audibly while manual Hold is open (RMS %)"
					.format(label, baseline[\rms][i].round(0.000001)));
			check.value(active[\rms][i] <= 1e-7,
				"% freezes losslessly while its Record gate is high"
					.format(label));
			check.value(active[\peaks][i] <= 1.01,
				"% remains bounded (peak %)"
					.format(label, active[\peaks][i].round(0.000001)));
		};
	} { |error|
		ok = false;
		results.add("FAIL: uncaught matrix-input NRT error: " ++ error.errorString);
	};
	results.do(_.postln);
	if(ok) { "CQ_MATRIX_INPUTS_OK".postln; 0.exit } {
		"CQ_MATRIX_INPUTS_FAILED".postln; 1.exit
	};
}.fork(AppClock);
)
