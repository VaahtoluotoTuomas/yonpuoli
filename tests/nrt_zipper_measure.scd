// §13.6 matrix-weight zipper measurement.
//
// Renders one sustained harmonic source through three otherwise identical
// VCA paths:
//   ch 0 = the production 30 Hz bus writes through prMatrixWeight's 50 ms Lag
//   ch 1 = a continuous-ramp reference through the same 50 ms Lag
//   ch 2 = an intentionally unlagged 30 Hz positive control
//   ch 3 = that positive control's continuous-ramp reference
//
// The source holds the matrix modulator at +0.5 while a shared-cell weight
// glides 0.9 -> 0.1. This keeps the accepted p=2 VCA law out of its ceiling
// while making every control step audible if the seam is exposed. The test is
// a measurement, not an audibility verdict: it asserts valid renders and
// reports the production/positive-control residuals, then writes mono files
// for a human A/B. No dB threshold is allowed to impersonate a listening gate.
//
// Run only through tests/run_sclang_gate.ps1.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var tmpDir = repo +/+ "tests" +/+ "tmp";
var outPath = tmpDir +/+ "nrt_zipper_measure.wav";
var currentPath = tmpDir +/+ "zipper_current_30hz.wav";
var referencePath = tmpDir +/+ "zipper_reference_continuous.wav";
var positivePath = tmpDir +/+ "zipper_unlagged_positive_control.wav";
var positiveReferencePath = tmpDir +/+ "zipper_unlagged_reference.wav";
var sr = 48000;
var dur = 10.0;
var glideStart = 1.0;
var glideDur = 8.0;
var writeHz = 30;
var weightBus = 16;
var opts = ServerOptions.new;
var ok = true;
var results = List.new;
var check = { |condition, message|
	if(condition) {
		results.add("ok:   " ++ message)
	} {
		ok = false;
		results.add("FAIL: " ++ message)
	};
};
var def = SynthDef(\cqTestZipperMeasure, {
	var rawWeight = In.kr(\weightBus.ir(weightBus), 16)[0];
	var currentWeight = CQSynthDefs.prMatrixWeight(
		\weightBus.ir(weightBus), DC.kr(1))[0];
	var idealWeight = EnvGen.kr(Env(
		#[0.9, 0.9, 0.1, 0.1],
		[glideStart, glideDur, dur - glideStart - glideDur],
		\lin));
	var referenceWeight = Lag.kr(idealWeight, 0.05);
	var modulator = 0.5;
	var base = 0.8;
	var source = Mix(SinOsc.ar(
		#[110, 220, 330, 550],
		#[0.0, 0.7, 1.4, 2.1],
		#[0.12, 0.08, 0.055, 0.035]));
	var gainFor = { |weight|
		(base + CQSynthDefs.prVcaLaw(weight * modulator)).clip(0, 2)
	};
	Out.ar(0, [
		source * gainFor.value(currentWeight),
		source * gainFor.value(referenceWeight),
		source * gainFor.value(rawWeight),
		source * gainFor.value(idealWeight)
	]);
});
var score = Score.new;
var initialWeights = 0.0 ! 16;
var steps = (glideDur * writeHz).round.asInteger;
var render = { |aScore|
	var done = false, waited = 0.0;
	aScore.recordNRT(nil, outPath, nil, sr, "WAV", "float", opts,
		duration: dur, action: { done = true });
	while { done.not and: { waited < 120 } } {
		0.2.wait;
		waited = waited + 0.2;
	};
	if(done.not) { Error("zipper NRT render timed out").throw };
};
var read = {
	var sf = SoundFile.openRead(outPath);
	var data;
	if(sf.isNil) { Error("cannot open zipper render: " ++ outPath).throw };
	data = FloatArray.newClear(sf.numFrames * sf.numChannels);
	sf.readData(data);
	sf.close;
	[data, sf.numFrames, sf.numChannels]
};
var writeMono = { |data, frames, channels, channel, path|
	var mono = FloatArray.newClear(frames);
	var sf = SoundFile.new;
	frames.do { |i| mono[i] = data[(i * channels) + channel] };
	sf.numChannels = 1;
	sf.sampleRate = sr;
	sf.headerFormat = "WAV";
	sf.sampleFormat = "float";
	if(sf.openWrite(path).not) { Error("cannot write " ++ path).throw };
	sf.writeData(mono);
	sf.close;
};

opts.numOutputBusChannels = 4;
opts.numInputBusChannels = 0;
opts.blockSize = 64;
File.mkdir(tmpDir);

initialWeights[0] = 0.9;
score.add([0.0, [\d_recv, def.asBytes]]);
score.add([0.0, [\c_setn, weightBus, 16] ++ initialWeights]);
score.add([0.0, [\s_new, \cqTestZipperMeasure, 1000, 0, 0,
	\weightBus, weightBus]]);
(0..steps).do { |i|
	var t = i / steps;
	var weight = 0.9 + ((0.1 - 0.9) * t);
	score.add([glideStart + (i / writeHz), [\c_set, weightBus, weight]]);
};
score.add([dur, [\c_set, 0, 0]]);

{
	var rendered, data, frames, channels;
	var startFrame, endFrame, count;
	var currentSq = 0.0, currentPeak = 0.0;
	var smoothErrSq = 0.0, smoothErrPeak = 0.0;
	var rawErrSq = 0.0, rawErrPeak = 0.0;
	var smoothErrRms, rawErrRms, currentRms;
	var smoothRelDb, rawRelDb, improvementDb;

	try {
		"§13.6: rendering 30 Hz matrix glide and controls...".postln;
		render.value(score);
		rendered = read.value;
		data = rendered[0];
		frames = rendered[1];
		channels = rendered[2];
		check.value(channels == 4,
			"render has matched current/reference and positive-control/reference pairs");
		check.value(frames >= (dur * sr * 0.99), "render has the full measurement duration");

		startFrame = ((glideStart + 0.25) * sr).asInteger;
		endFrame = ((glideStart + glideDur - 0.1) * sr).asInteger.min(frames - 1);
		count = max(1, endFrame - startFrame + 1);
		(startFrame..endFrame).do { |i|
			var current = data[(i * channels)];
			var reference = data[(i * channels) + 1];
			var raw = data[(i * channels) + 2];
			var rawReference = data[(i * channels) + 3];
			var smoothErr = current - reference;
			var rawErr = raw - rawReference;
			currentSq = currentSq + current.squared;
			currentPeak = max(currentPeak, current.abs);
			smoothErrSq = smoothErrSq + smoothErr.squared;
			smoothErrPeak = max(smoothErrPeak, smoothErr.abs);
			rawErrSq = rawErrSq + rawErr.squared;
			rawErrPeak = max(rawErrPeak, rawErr.abs);
		};
		currentRms = (currentSq / count).sqrt;
		smoothErrRms = (smoothErrSq / count).sqrt;
		rawErrRms = (rawErrSq / count).sqrt;
		smoothRelDb = (smoothErrRms / max(currentRms, 1e-12)).ampdb;
		rawRelDb = (rawErrRms / max(currentRms, 1e-12)).ampdb;
		improvementDb = (smoothErrRms / max(rawErrRms, 1e-12)).ampdb.neg;

		("§13.6 current signal: rms %, peak %").format(
			currentRms.round(0.000001), currentPeak.round(0.000001)).postln;
		("§13.6 50 ms path residual: rms % (% dB rel), peak %").format(
			smoothErrRms.round(0.000001), smoothRelDb.round(0.01),
			smoothErrPeak.round(0.000001)).postln;
		("§13.6 unlagged positive control residual: rms % (% dB rel), peak %").format(
			rawErrRms.round(0.000001), rawRelDb.round(0.01),
			rawErrPeak.round(0.000001)).postln;
		("§13.6 production smoothing improvement: % dB").format(
			improvementDb.round(0.01)).postln;

		check.value(currentRms >= 0.03, "current path carries sustained signal");
		check.value(currentPeak <= 1.0, "isolated VCA measurement remains bounded");
		check.value(smoothErrRms < rawErrRms,
			"production 50 ms weight smoothing reduces 30 Hz staircase residual");

		writeMono.value(data, frames, channels, 0, currentPath);
		writeMono.value(data, frames, channels, 1, referencePath);
		writeMono.value(data, frames, channels, 2, positivePath);
		writeMono.value(data, frames, channels, 3, positiveReferencePath);
		("§13.6 listening files: % | % | % | %").format(
			currentPath, referencePath, positivePath, positiveReferencePath).postln;
	} { |error|
		ok = false;
		results.add("FAIL: uncaught error: " ++ error.errorString);
	};

	results.do(_.postln);
	if(ok) {
		"CQ_ZIPPER_MEASURED".postln;
		0.exit
	} {
		"CQ_ZIPPER_MEASURE_FAILED".postln;
		1.exit
	};
}.fork(AppClock);
)
