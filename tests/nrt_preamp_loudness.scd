// §13.8 preamp equal-loudness listening lab.
//
// Uses three local listening sources already present under samples/:
// speech, a drum loop and a sustained organ. Each source is peak-aligned to
// 0.5 exactly as CQDeck's -6 dBFS normalization intends, then rendered through
// the production asymmetric waveshaper in three fixed variants:
//
//   legacy_uncompensated no drive-dependent attenuation
//   accepted_1db_oct    the production helper accepted by ear
//   stronger_1_5db_oct  the rejected stronger listening reference
//
// The curves are conservative partial attenuation, not automatic RMS
// normalization. Every file repeats one identical excerpt in this order:
// drive 1/2/6/16/40 at bias 0.15, then bias 0/0.15/0.6 at drive 12.
// A constant 0.4 audition trim keeps the standalone files safe without
// changing any within- or between-variant comparison.
//
// The accepted variant calls the same CQSynthDefs helper as production. The
// other two remain fixed listening references. The gate prints RMS/peak
// measurements and writes nine mono WAV files so the accepted relation stays
// reproducible without introducing a runtime character parameter.
//
// Run only through tests/run_sclang_gate.ps1.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var tmpDir = repo +/+ "tests" +/+ "tmp";
var sr = 48000;
var segmentDur = 2.5;
var gapDur = 0.25;
var drives = #[1, 2, 6, 16, 40];
var biases = #[0.0, 0.15, 0.6];
var settings = drives.collect { |drive|
	(drive: drive, bias: 0.15, label: "drive % / bias 0.15".format(drive))
} ++ biases.collect { |bias|
	(drive: 12, bias: bias, label: "drive 12 / bias %".format(bias))
};
var variants = [
	(name: \legacy_uncompensated, mode: 0),
	(name: \accepted_1db_oct, mode: 1),
	(name: \stronger_1_5db_oct, mode: 2)
];
var sources = [
	(
		name: \speech,
		path: repo +/+ "samples"
			+/+ "203907__s9ames__bologna-speech-english.wav",
		startSeconds: 0.0
	),
	(
		name: \drums,
		path: repo +/+ "samples"
			+/+ "431874__esares__106-bpm-drum-loop-1.wav",
		startSeconds: 0.0
	),
	(
		name: \organ,
		path: repo +/+ "samples"
			+/+ "150858__freezeman__keyorgan5_d3_sustain.wav",
		startSeconds: 1.0
	)
];
var totalDur = settings.size * (segmentDur + gapDur);
var opts = ServerOptions.new;
var ok = true;
var results = List.new;
var measurements = IdentityDictionary.new;
var check = { |condition, message|
	if(condition) {
		results.add("ok:   " ++ message)
	} {
		ok = false;
		results.add("FAIL: " ++ message)
	};
};
var preampDef = SynthDef(\cqTestPreampLoudness, {
	var drive = \drive.ir(1);
	var bias = \bias.ir(0.15);
	var mode = \mode.ir(0);
	var source = PlayBuf.ar(1, \buf.ir(0), BufRateScale.kr(\buf.ir(0)),
		doneAction: 0) * \sourceGain.ir(1);
	var shaped = ((source * drive) + bias).tanh - bias.tanh;
	var attenuation = Select.kr(mode, [
		DC.kr(1),
		CQSynthDefs.prPreampMakeup(drive),
		(drive.log2 * -1.5).dbamp
	]);
	var envelope = EnvGen.kr(
		Env.linen(0.01, segmentDur - 0.02, 0.01),
		doneAction: 2);
	// Exact production order, followed only by the lab's constant safe trim.
	Out.ar(0, HPF.ar(shaped * attenuation, 20) * envelope * 0.4);
});
var inspectSource = { |source|
	var sf = SoundFile.openRead(source[\path]);
	var startFrame, frames, data, peak = 0.0, gain, inspected;
	if(sf.isNil) { Error("missing listening source: " ++ source[\path]).throw };
	startFrame = (source[\startSeconds] * sf.sampleRate).round.asInteger;
	frames = min((segmentDur * sf.sampleRate).ceil.asInteger,
		sf.numFrames - startFrame);
	if(frames < (segmentDur * sf.sampleRate * 0.95)) {
		sf.close;
		Error("listening source is too short: " ++ source[\path]).throw;
	};
	data = FloatArray.newClear(frames * sf.numChannels);
	sf.seek(startFrame);
	sf.readData(data);
	frames.do { |i|
		peak = max(peak, data[i * sf.numChannels].abs)
	};
	gain = 0.5 / max(peak, 1e-12);
	inspected = (
		startFrame: startFrame,
		frames: frames,
		peak: peak,
		gain: gain,
		sampleRate: sf.sampleRate,
		channels: sf.numChannels
	);
	sf.close;
	inspected
};
var makeScore = { |source, inspected, variant|
	var score = Score.new;
	score.add([0.0, [\d_recv, preampDef.asBytes]]);
	score.add([0.0, [
		\b_allocReadChannel, 0, source[\path],
		inspected[\startFrame], inspected[\frames], 0
	]]);
	settings.do { |setting, i|
		var start = 0.1 + (i * (segmentDur + gapDur));
		score.add([start, [
			\s_new, \cqTestPreampLoudness, 1000 + i, 0, 0,
			\buf, 0,
			\drive, setting[\drive],
			\bias, setting[\bias],
			\mode, variant[\mode],
			\sourceGain, inspected[\gain]
		]]);
	};
	score.add([totalDur, [\c_set, 0, 0]]);
	score
};
var render = { |score, path|
	var done = false, waited = 0.0;
	score.recordNRT(nil, path, nil, sr, "WAV", "float", opts,
		duration: totalDur, action: { done = true });
	while { done.not and: { waited < 180 } } {
		0.2.wait;
		waited = waited + 0.2;
	};
	if(done.not) { Error("preamp NRT render timed out: " ++ path).throw };
};
var analyze = { |path|
	var sf = SoundFile.openRead(path);
	var data, rmsValues = Array.newClear(settings.size);
	var peakValues = Array.newClear(settings.size);
	if(sf.isNil) { Error("cannot open preamp render: " ++ path).throw };
	data = FloatArray.newClear(sf.numFrames * sf.numChannels);
	sf.readData(data);
	settings.do { |setting, i|
		var from = ((0.1 + (i * (segmentDur + gapDur)) + 0.05) * sf.sampleRate)
			.asInteger;
		var to = ((0.1 + (i * (segmentDur + gapDur)) + segmentDur - 0.05)
			* sf.sampleRate).asInteger.min(sf.numFrames - 1);
		var count = max(1, to - from);
		var sum = 0.0, peak = 0.0;
		(from..to).do { |frame|
			var sample = data[frame * sf.numChannels];
			sum = sum + sample.squared;
			peak = max(peak, sample.abs);
		};
		rmsValues[i] = (sum / count).sqrt;
		peakValues[i] = peak;
	};
	sf.close;
	(rms: rmsValues, peak: peakValues)
};

opts.numOutputBusChannels = 1;
opts.numInputBusChannels = 0;
opts.blockSize = 64;
File.mkdir(tmpDir);

{
	try {
		sources.do { |source|
			var inspected = inspectSource.value(source);
			("% source: channel-0 peak %, audition gain %, start % s").format(
				source[\name], inspected[\peak].round(0.0001),
				inspected[\gain].round(0.0001),
				source[\startSeconds]).postln;
			check.value(inspected[\peak] >= 0.05,
				"% excerpt carries useful signal".format(source[\name]));

			variants.do { |variant|
				var path = tmpDir +/+
					("preamp_%_%.wav".format(source[\name], variant[\name]));
				var measured, baseRms, rel;
				("§13.8 rendering % / %...").format(
					source[\name], variant[\name]).postln;
				render.value(makeScore.value(source, inspected, variant), path);
				measured = analyze.value(path);
				measurements[
					(source[\name].asString ++ "_" ++ variant[\name].asString)
						.asSymbol
				] = measured;
				baseRms = max(measured[\rms][0], 1e-12);
				rel = measured[\rms].collect { |value|
					(value / baseRms).ampdb.round(0.01)
				};
				("% / % RMS dB vs drive 1: %").format(
					source[\name], variant[\name], rel).postln;
				("% / % peaks: %").format(
					source[\name], variant[\name],
					measured[\peak].collect(_.round(0.0001))).postln;
				check.value(measured[\rms].every { |value|
					value.isNaN.not and: { value.abs != inf } and: { value > 1e-5 }
				}, "% / % is finite and non-silent".format(
					source[\name], variant[\name]));
				check.value(measured[\peak].maxItem <= 0.9,
					"% / % standalone audition stays below -0.9 dBFS".format(
						source[\name], variant[\name]));
				("§13.8 listening file: " ++ path).postln;
			};

			// At drive 1 every curve is exactly unity. At drive 40 the two
			// candidates must be ordered by their declared dB/oct slope.
			{
				var legacy = measurements[
					(source[\name].asString ++ "_legacy_uncompensated")
						.asSymbol][\rms];
				var accepted = measurements[
					(source[\name].asString ++ "_accepted_1db_oct")
						.asSymbol][\rms];
				var stronger = measurements[
					(source[\name].asString ++ "_stronger_1_5db_oct")
						.asSymbol][\rms];
				check.value((legacy[0] - accepted[0]).abs <= 1e-6
					and: { (accepted[0] - stronger[0]).abs <= 1e-6 },
					"% candidates are anchored exactly at drive 1".format(
						source[\name]));
				check.value(legacy[4] > accepted[4]
					and: { accepted[4] > stronger[4] },
					"% drive-40 levels follow legacy > accepted > stronger"
						.format(source[\name]));
			}.value;
		};
	} { |error|
		error.reportError;
		ok = false;
		results.add("FAIL: uncaught error: " ++ error.errorString);
	};

	results.do(_.postln);
	if(ok) {
		"CQ_PREAMP_OK".postln;
		0.exit
	} {
		"CQ_PREAMP_LAB_FAILED".postln;
		1.exit
	};
}.fork(AppClock);
)
