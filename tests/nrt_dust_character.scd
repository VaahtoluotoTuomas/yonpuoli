// NRT digital-dust character gate against the REAL \cqCoco SynthDef.
// Compares the current source with the pre-§13.3 WhiteNoise*LFNoise2 source at
// default dust level, then checks the intended musical invariants:
//   - no extra default noise-floor loudness;
//   - a crestier, genuinely granular waveform rather than continuous hiss;
//   - stored event density rises strongly with Coco clock speed;
//   - usable activity remains at the 1500 Hz hand floor.
//   - the 48 kHz end keeps some dry air without returning to white-noise glare.
// Prints CQ_DUST_OK / CQ_DUST_FAILED as its last line.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var tmpDir = repo +/+ "tests" +/+ "tmp";
var cfg = CQConfig.validate(());
var defs = CQSynthDefs.all(cfg);
var sr = 48000, dur = 8.0, frames = 4096;
var speeds = [1500, 24000, 48000];
var opts = ServerOptions.new;
var results = List.new, ok = true;
var newStats = List.new;
var legacyDef, makeScore, render, readStats, check;

check = { |bool, msg|
	if(bool) { results.add("ok:   " ++ msg) } { ok = false; results.add("FAIL: " ++ msg) };
};

// Exact pre-§13.3 record source, kept only as an in-test loudness reference.
// Its buffer/read/write order and 8-bit converter match \cqCoco's relevant path.
legacyDef = SynthDef(\cqTestLegacyDust, {
	var buf = \buf.ir(1), speed = \speed.ir(24000);
	var phase = Phasor.ar(0, speed / SampleRate.ir, 0, BufFrames.ir(buf), 0);
	var here = BufRd.ar(1, buf, phase, loop: 1, interpolation: 1);
	var record;
	RandSeed.ir(1, \seed.ir(9000));
	record = WhiteNoise.ar(\dust.ir(0.02)) * LFNoise2.ar(3);
	record = record.clip2(1).round(2 ** -7);
	BufWr.ar(record, buf, phase, loop: 1);
	Out.ar(1, here);
});

makeScore = { |speed|
	var score = Score.new;
	defs.do { |d| score.add([0.0, [\d_recv, d.asBytes]]) };
	score.add([0.0, [\d_recv, legacyDef.asBytes]]);
	score.add([0.0, [\b_alloc, 0, frames, 1]]);
	score.add([0.0, [\b_alloc, 1, frames, 1]]);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqCoco, 1001, 1, 1000,
		\buf, 0, \preBus, 8, \deckBus, 8, \qMixBus, 8, \crossBus, 8,
		\outBus, 0, \modSrcBus, 16, \modTapIdx, 12, \modOrganIdx, 14,
		\speed, speed, \inLevel, 0, \fbLevel, 0, \dust, 0.02,
		\hold, 0, \organLvl, 0, \level, 1,
		\slotPre, 0, \slotDeck, 0, \slotQ, 0, \slotX, 0]]);
	score.add([0.0, [\s_new, \cqTestLegacyDust, 1002, 1, 1000,
		\buf, 1, \speed, speed, \dust, 0.02, \seed, 9000 + speed]]);
	score.add([dur, [\c_set, 0, 0]]);
	score
};

render = { |score, path|
	var done = false, waited = 0;
	score.recordNRT(nil, path, nil, sr, "WAV", "float", opts,
		duration: dur, action: { done = true });
	while { done.not and: { waited < 120 } } { 0.2.wait; waited = waited + 0.2 };
	if(done.not) { Error("dust NRT render timed out: " ++ path).throw };
};

// Analyze the last three seconds, after even the 1500 Hz case has completed
// its first 4096-cell buffer pass. "changes" is audible stored-grain activity,
// not the raw Dust2 trigger count before the clocked RAM has thinned it.
readStats = { |path, channel|
	var sf = SoundFile.openRead(path), data, channels, start, count;
	var sum2 = 0.0, sum4 = 0.0, peak = 0.0;
	var active = 0, changes = 0, i = 0, v, prev = 0.0, rms;
	var fftSize = 65536, real, imag, spectrum, bin = 1;
	var spectralPower = 0.0, highPower = 0.0, centroidNumerator = 0.0, power, hz;
	if(sf.isNil) { Error("cannot read dust NRT render: " ++ path).throw };
	channels = sf.numChannels;
	data = FloatArray.newClear(sf.numFrames * channels);
	sf.readData(data);
	sf.close;
	start = (5.0 * sr).asInteger;
	count = ((dur * sr).asInteger - start);
	while { i < count } {
		v = data[((start + i) * channels) + channel];
		sum2 = sum2 + v.squared;
		sum4 = sum4 + v.squared.squared;
		peak = max(peak, v.abs);
		if(v.abs >= (2 ** -8)) { active = active + 1 };
		if((v - prev).abs >= (2 ** -8)) { changes = changes + 1 };
		prev = v;
		i = i + 1;
	};
	rms = (sum2 / count).sqrt;
	real = Signal.fill(fftSize, { |j|
		data[((start + j) * channels) + channel]
	}) * Signal.hanningWindow(fftSize);
	imag = Signal.newClear(fftSize);
	spectrum = fft(real, imag, Signal.fftCosTable(fftSize));
	while { bin < (fftSize / 2) } {
		power = spectrum.real[bin].squared + spectrum.imag[bin].squared;
		hz = bin * sr / fftSize;
		spectralPower = spectralPower + power;
		centroidNumerator = centroidNumerator + (power * hz);
		if(hz >= 10000) { highPower = highPower + power };
		bin = bin + 1;
	};
	(
		rms: rms,
		peak: peak,
		active: active / count,
		changes: changes / (count / sr),
		crest: peak / max(rms, 1e-12),
		kurtosis: (sum4 / count) / max((sum2 / count).squared, 1e-20),
		centroid: centroidNumerator / max(spectralPower, 1e-20),
		highPower: highPower,
		highShare: highPower / max(spectralPower, 1e-20)
	)
};

opts.numOutputBusChannels = 2;
opts.numInputBusChannels = 2;
opts.blockSize = cfg[\blockSize];
File.mkdir(tmpDir);

{
	try {
		speeds.do { |speed|
			var path = tmpDir +/+ "nrt_dust_%.wav".format(speed);
			var current, legacy, levelRatio;
			"CQ NRT: rendering dust character at % Hz...".format(speed).postln;
			render.value(makeScore.value(speed), path);
			current = readStats.value(path, 0);
			legacy = readStats.value(path, 1);
			newStats.add(current);
			levelRatio = current[\rms] / max(legacy[\rms], 1e-12);
			("% Hz: new rms % peak % active % changeHz % crest % kurt % centroid % high % | old rms % centroid % high % | ratio % dB")
				.format(
					speed,
					current[\rms].round(0.000001),
					current[\peak].round(0.000001),
					current[\active].round(0.0001),
					current[\changes].round(0.1),
					current[\crest].round(0.01),
					current[\kurtosis].round(0.01),
					current[\centroid].round(1),
					current[\highShare].round(0.001),
					legacy[\rms].round(0.000001),
					legacy[\centroid].round(1),
					legacy[\highShare].round(0.001),
					levelRatio.ampdb.round(0.01)
				).postln;

			check.value(current[\rms] <= (legacy[\rms] * 1.05),
				"% Hz default dust does not raise the legacy noise floor".format(speed));
			check.value(current[\rms] >= (legacy[\rms] * 0.45),
				"% Hz default dust remains present (within -6.94 dB of legacy)".format(speed));
			check.value(current[\crest] >= 5,
				"% Hz dust is transient/granular (crest >= 5)".format(speed));
			check.value(current[\kurtosis] >= 8,
				"% Hz dust is not Gaussian tape hiss (kurtosis >= 8)".format(speed));
			check.value(current[\active].inclusivelyBetween(0.05, 0.25),
				"% Hz keeps a sparse but continuous 8-bit footprint".format(speed));
			check.value(current[\peak] <= 0.08,
				"% Hz default crackle peaks stay <= 0.08".format(speed));
			check.value(current[\highPower] <= (legacy[\highPower] * 1.05),
				"% Hz does not add absolute energy above 10 kHz".format(speed));
			check.value(current[\centroid] <= (legacy[\centroid] * 1.10),
				"% Hz spectral centroid does not brighten beyond legacy".format(speed));
		};

		check.value(newStats[0][\changes] < newStats[1][\changes],
			"stored grain activity rises from 1500 to 24000 Hz");
		check.value(newStats[1][\changes] < newStats[2][\changes],
			"stored grain activity rises from 24000 to 48000 Hz");
		check.value((newStats[2][\changes] / newStats[0][\changes])
				.inclusivelyBetween(12, 32),
			"fast/slow grain-activity ratio is strongly clock-coupled (12..32x)");
		check.value((newStats.collect(_[\rms]).maxItem
			/ newStats.collect(_[\rms]).minItem).ampdb <= 3,
			"dust loudness stays within 3 dB across the speed range");
		check.value(newStats[2][\centroid].inclusivelyBetween(8000, 10000),
			"48 kHz centroid keeps dry air without brittle glare (8..10 kHz)");
		check.value(newStats[2][\highShare].inclusivelyBetween(0.30, 0.45),
			"48 kHz energy above 10 kHz is present but not white-hiss dominant");
	} { |err|
		ok = false;
		results.add("FAIL: uncaught dust NRT error: " ++ err.errorString);
	};

	results.do(_.postln);
	if(ok) { "CQ_DUST_OK".postln; 0.exit } { "CQ_DUST_FAILED".postln; 1.exit };
}.fork(AppClock);
)
