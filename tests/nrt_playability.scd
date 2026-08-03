// Playability measurement for PLAN.md -> "Tie 1.0:aan", item 8.
//
// This is deliberately a measurement, not a taste gate. It uses the real
// \cqQuantussy and \cqCoco SynthDefs. It retains the pre-PLAY-7 first-touch and
// rejected-candidate probes as diagnostics, but every shake below uses today's
// ecological matrix law. `nrt_shake_ecology.scd` is the authoritative blank-RAM
// proof that also applies each family's safe control preparation:
//
//   1. How audible was one former first-touch 0.5 banana as the existing matrix gets
//      denser? A/B differs only by tri5 -> coco1_inVca at weight 0.5.
//   2. How different are two complete shakes at the Night full-moon density
//      (0.2) and at the pathological all-cells density (1.0)?
//   3. Does a normalized bipolar source behind an exact 0.5 banana cross the
//      production Schmitt on-threshold, or merely touch it?
//   4. Can a Coco's own organ return through both a continuous and a discrete
//      destination, using the production one-block self-source path?
//
// The pair renders report signal RMS, difference RMS relative to signal,
// correlation, both spectral centroids and the difference-signal centroid.
// Listening A/B files stay under tests/tmp/. Assertions cover only render
// validity (finite, complete, non-silent where the fixture promises sound) and
// never turn a dB number into an audibility verdict.
//
// Run only through tests/run_sclang_gate.ps1.

(
var repo, tmpDir, cfg, defs;
var sr, dur, analysisStart, analysisEnd;
var bufFrames, amount, baselineWeight, densitySteps;
var opts, results, ok;
var feedDef, sinkDef, schmittDef;
var check, render, makeShake, addCable, matrixCount, branchPlan;
var addMatrix, quantussyArgs, cocoArgs, makePairScore, makeCrossOrganScore;
var readPairStats;
var centroidFor, readSchmitt, schmittScore;

repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
tmpDir = repo +/+ "tests" +/+ "tmp";
cfg = CQConfig.validate(());
defs = CQSynthDefs.all(cfg);
sr = 48000;
dur = 10.0;
analysisStart = 3.0;
analysisEnd = 9.5;
bufFrames = 32768;
amount = 0.3;
baselineWeight = 0.5;
densitySteps = [0.0, 0.05, 0.1, 0.2, 0.5, 1.0];
opts = ServerOptions.new;
results = List.new;
ok = true;

check = { |condition, message|
	if(condition) {
		results.add("ok:   " ++ message)
	} {
		ok = false;
		results.add("FAIL: " ++ message)
	};
};

// A deterministic, pitched and articulated program source. Both branches read
// this exact signal; it contains no random UGen and therefore cannot create an
// A/B difference of its own.
feedDef = SynthDef(\cqTestPlayabilityFeed, {
	var pulse = Decay2.ar(Impulse.ar(3), 0.003, 0.18);
	var body = Mix(SinOsc.ar(
		#[110, 220, 333, 555, 889],
		#[0.0, 0.6, 1.2, 1.8, 2.4],
		#[0.16, 0.10, 0.07, 0.045, 0.025]));
	var sig = body * (0.32 + pulse);
	Out.ar(\preA.ir(33), sig);
	Out.ar(\preB.ir(65), sig);
});

// Same fixed safety law as the production master. One mono branch per output
// channel avoids pan/loudness confounds while retaining tanh + limiter effects.
sinkDef = SynthDef(\cqTestPlayabilitySink, {
	var a = CQSynthDefs.prSafetyChain(In.ar(\a.ir(34)));
	var b = CQSynthDefs.prSafetyChain(In.ar(\b.ir(66)));
	Out.ar(0, [a, b]);
});

// Exact production Schmitt thresholds, isolated so the boundary can be read
// without guessing at UGen equality semantics.
schmittDef = SynthDef(\cqTestPlayabilitySchmitt, {
	var src = LFTri.ar(2);
	Out.ar(0, #[0.49, 0.5, 0.55, 0.60, 0.65].collect { |weight|
		Schmidt.ar(src * weight,
			CQSynthDefs.gateOffThreshold, CQSynthDefs.gateOnThreshold)
	});
});

render = { |score, path, duration|
	var done = false, waited = 0.0, oscPath = path ++ ".osc";
	score.recordNRT(oscPath, path, nil, sr, "WAV", "float", opts,
		duration: duration, action: { done = true });
	while { done.not and: { waited < 120 } } {
		0.2.wait;
		waited = waited + 0.2;
	};
	if(done.not) { Error("playability NRT render timed out: " ++ path).throw };
};

// Use CQMatrix itself, not a copied random algorithm: these are exactly the
// current shake weights and spesal states for a given seed and density.
makeShake = { |seed, density, strength|
	var matrix = CQMatrix(nil);
	var spesal = matrix.shake(strength, seed, density: density);
	(weights: matrix.fullMatrix, spesal: spesal, seed: seed, density: density)
};

addCable = { |shake, src, dest, weight|
	var weights = shake[\weights].collect(_.copy);
	weights[CQMatrix.destIndex(dest)][CQMatrix.srcIndex(src)] = weight;
	(weights: weights, spesal: shake[\spesal].copy,
		seed: shake[\seed], density: shake[\density])
};

matrixCount = { |shake|
	shake[\weights].sum { |row| row.count { |weight| weight != 0.0 } }
};

// Audio/control bus plan for two isolated copies of the real graph fragment.
// Matrix control buses are in a separate namespace, so 100/500 are safely
// independent of the audio-bus spans.
branchPlan = [
	(mod: 16, qMix: 32, pre: 33, out: 34, silent: 35, ctl: 100,
		buf: 0, quantussy: 1100, coco: 1101),
	(mod: 48, qMix: 64, pre: 65, out: 66, silent: 67, ctl: 500,
		buf: 1, quantussy: 1200, coco: 1201)
];

addMatrix = { |score, branch, shake|
	CQMatrix.destNames.do { |dest, di|
		score.add([0.0, [\c_setn, branch[\ctl] + (di * 16), 16]
			++ shake[\weights][di]]);
	};
};

quantussyArgs = { |branch, shake, prepared|
	var args = [
		\modSrcBus, branch[\mod], \qMixBus, branch[\qMix],
		\qFreq, CQSpec.default(\qFreq), \qChaos, CQSpec.default(\qChaos),
		\qShLag, CQSpec.default(\qShLag), \patchDepth, 1,
		\range1, 0, \range2, 0, \range3, 1, \range4, 0, \range5, 2,
		\send1, 0, \send2, 0, \send3, 0, \send4, 0,
		\send5, if(prepared) { 0.5 } { CQSpec.default(\c5Send) }
	];
	(1..5).do { |k|
		args = args ++ [
			("state" ++ k).asSymbol, shake[\spesal][k - 1],
			("wFm" ++ k).asSymbol,
				branch[\ctl] + (CQMatrix.destIndex(
					("castle" ++ k ++ "_fm").asSymbol) * 16),
			("wSpesal" ++ k).asSymbol,
				branch[\ctl] + (CQMatrix.destIndex(
					("castle" ++ k ++ "_spesal").asSymbol) * 16)
		];
	};
	args
};

cocoArgs = { |branch, prepared, bootSpaf|
	[
		\buf, branch[\buf], \preBus, branch[\pre],
		\deckBus, branch[\silent], \qMixBus, branch[\qMix],
		\crossBus, branch[\silent], \outBus, branch[\out],
		\modSrcBus, branch[\mod], \modTapIdx, 12, \modOrganIdx, 14,
		\cocoIndex, 0,
		\wSpafBus, branch[\ctl] + (CQMatrix.destIndex(\coco1_spaf) * 16),
		\wInVcaBus, branch[\ctl] + (CQMatrix.destIndex(\coco1_inVca) * 16),
		\wFbVcaBus, branch[\ctl] + (CQMatrix.destIndex(\coco1_fbVca) * 16),
		\wFlipBus, branch[\ctl] + (CQMatrix.destIndex(\coco1_flip) * 16),
		\wSkipBus, branch[\ctl] + (CQMatrix.destIndex(\coco1_skip) * 16),
		\wOrganBus, branch[\ctl] + (CQMatrix.destIndex(\coco1_organ) * 16),
		\wSlotQBus, branch[\ctl] + (CQMatrix.destIndex(\coco1_slotQ) * 16),
		\wRecordBus, branch[\ctl] + (CQMatrix.destIndex(\coco1_record) * 16),
		\patchDepth, 1, \speed, CQSpec.default(\coco1Speed),
		\spafKnob, if(prepared) { 0.35 } { bootSpaf },
		\inLevel, CQSpec.default(\coco1In),
		\fbLevel, if(prepared) { 0.9 } { CQSpec.default(\coco1Fb) },
		\dust, 0, \hold, if(prepared) { 0 } { CQSpec.default(\coco1Hold) },
		\organLvl, 0, \level, 1,
		\slotPre, 1, \slotDeck, 0,
		\slotQ, if(prepared) { 0.45 } { CQSpec.default(\coco1SlotQ) },
		\slotX, 0
	]
};

makePairScore = { |shakeA, shakeB, prepared, bootSpaf|
	var score = Score.new;
	defs.do { |def| score.add([0.0, [\d_recv, def.asBytes]]) };
	score.add([0.0, [\d_recv, feedDef.asBytes]]);
	score.add([0.0, [\d_recv, sinkDef.asBytes]]);
	branchPlan.do { |branch|
		score.add([0.0, [\b_alloc, branch[\buf], bufFrames, 1]]);
		// A deterministic held waveform makes the exact boot-default case audible
		// without relying on CQGraph's intentionally random power-on RAM.
		if(prepared.not) {
			score.add([0.0, [\b_gen, branch[\buf], \sine1, 7,
				1.0, 0.45, 0.22, 0.11, 0.06, 0.03]]);
		};
	};
	addMatrix.value(score, branchPlan[0], shakeA);
	addMatrix.value(score, branchPlan[1], shakeB);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqTestPlayabilityFeed, 1001, 1, 1000,
		\preA, branchPlan[0][\pre], \preB, branchPlan[1][\pre]]]);
	[shakeA, shakeB].do { |shake, i|
		var branch = branchPlan[i];
		score.add([0.0, [\s_new, \cqQuantussy, branch[\quantussy], 1, 1000]
			++ quantussyArgs.value(branch, shake, prepared)]);
		score.add([0.0, [\s_new, \cqCoco, branch[\coco], 1, 1000]
			++ cocoArgs.value(branch, prepared, bootSpaf)]);
	};
	score.add([0.0, [\s_new, \cqTestPlayabilitySink, 1300, 1, 1000,
		\a, branchPlan[0][\out], \b, branchPlan[1][\out]]]);
	score.add([dur, [\c_set, 0, 0]]);
	score
};

// Production-order cross-Coco fixture. One Coco 1 publishes org1 on canonical
// channel 14. Two otherwise identical Coco 2 consumers read that same source;
// only the right consumer has org1 -> coco2_skip at 0.60. Their own source
// publications are moved to unused channels so they cannot mix back into the
// canonical four-channel field under test.
makeCrossOrganScore = {
	var score = Score.new;
	var noWeights = 0.0 ! 16, skipWeights = 0.0 ! 16;
	var common = [
		\preBus, 8, \deckBus, 8, \qMixBus, 8, \crossBus, 8,
		\modSrcBus, 16, \speed, 24000, \spafKnob, 0,
		\inLevel, 0, \fbLevel, 0, \dust, 0, \hold, 1,
		\organLvl, 0, \level, 1, \slotPre, 0, \slotDeck, 0,
		\slotQ, 0, \slotX, 0, \patchDepth, 1
	];
	skipWeights[CQMatrix.srcIndex(\org1)] = 0.60;
	defs.do { |def| score.add([0.0, [\d_recv, def.asBytes]]) };
	score.add([0.0, [\d_recv, sinkDef.asBytes]]);
	3.do { |buf|
		score.add([0.0, [\b_alloc, buf, bufFrames, 1]]);
		score.add([0.0, [\b_gen, buf, \sine1, 7,
			1.0, 0.45, 0.22, 0.11, 0.06, 0.03]]);
	};
	score.add([0.0, [\c_setn, 100, 16] ++ noWeights]);
	score.add([0.0, [\c_setn, 116, 16] ++ skipWeights]);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	// Source: canonical coco1/org1 writers at modSrc + 12/+14.
	score.add([0.0, [\s_new, \cqCoco, 1100, 1, 1000]
		++ common ++ [\buf, 0, \outBus, 40, \cocoIndex, 0,
			\modTapIdx, 12, \modOrganIdx, 14]]);
	// Baseline and routed Coco 2 consumers. Each sees canonical org1 at +14.
	score.add([0.0, [\s_new, \cqCoco, 1101, 1, 1000]
		++ common ++ [\buf, 1, \outBus, 41, \cocoIndex, 1,
			\modTapIdx, 20, \modOrganIdx, 22, \wSkipBus, 100]]);
	score.add([0.0, [\s_new, \cqCoco, 1102, 1, 1000]
		++ common ++ [\buf, 2, \outBus, 42, \cocoIndex, 1,
			\modTapIdx, 21, \modOrganIdx, 23, \wSkipBus, 116]]);
	score.add([0.0, [\s_new, \cqTestPlayabilitySink, 1300, 1, 1000,
		\a, 41, \b, 42]]);
	score.add([dur, [\c_set, 0, 0]]);
	score
};

centroidFor = { |data, channels, channel, startFrame, difference = false|
	var fftSize = 65536;
	var real = Signal.fill(fftSize, { |j|
		var frame = startFrame + j;
		var a = data[(frame * channels) + channel];
		if(difference) { a - data[(frame * channels) + 1] } { a }
	}) * Signal.hanningWindow(fftSize);
	var spectrum = fft(real, Signal.newClear(fftSize), Signal.fftCosTable(fftSize));
	var numerator = 0.0, powerSum = 0.0, bin = 1;
	while { bin < (fftSize / 2) } {
		var power = spectrum.real[bin].squared + spectrum.imag[bin].squared;
		var hz = bin * sr / fftSize;
		numerator = numerator + (power * hz);
		powerSum = powerSum + power;
		bin = bin + 1;
	};
	numerator / max(powerSum, 1e-20)
};

readPairStats = { |path|
	var sf = SoundFile.openRead(path), data, channels, frames;
	var startFrame, endFrame, count, i;
	var a2 = 0.0, b2 = 0.0, d2 = 0.0, cross = 0.0, peak = 0.0;
	var a, b, d, rmsA, rmsB, diffRms, signalRms, finite = true;
	var centroidStart, centroidA, centroidB, centroidDiff;
	if(sf.isNil) { Error("cannot read playability NRT render: " ++ path).throw };
	channels = sf.numChannels;
	frames = sf.numFrames;
	data = FloatArray.newClear(frames * channels);
	sf.readData(data);
	sf.close;
	startFrame = (analysisStart * sr).asInteger;
	endFrame = min((analysisEnd * sr).asInteger, frames - 1);
	count = max(1, endFrame - startFrame + 1);
	i = startFrame;
	while { i <= endFrame } {
		a = data[i * channels];
		b = data[(i * channels) + 1];
		d = a - b;
		if(a.isNaN or: { b.isNaN } or: { a.abs == inf } or: { b.abs == inf }) {
			finite = false
		};
		a2 = a2 + a.squared;
		b2 = b2 + b.squared;
		d2 = d2 + d.squared;
		cross = cross + (a * b);
		peak = max(peak, max(a.abs, b.abs));
		i = i + 1;
	};
	rmsA = (a2 / count).sqrt;
	rmsB = (b2 / count).sqrt;
	diffRms = (d2 / count).sqrt;
	signalRms = (rmsA + rmsB) * 0.5;
	centroidStart = min(startFrame + (0.5 * sr).asInteger,
		frames - 65536 - 1);
	centroidA = centroidFor.value(data, channels, 0, centroidStart, false);
	centroidB = centroidFor.value(data, channels, 1, centroidStart, false);
	centroidDiff = centroidFor.value(data, channels, 0, centroidStart, true);
	(
		channels: channels, frames: frames, finite: finite, peak: peak,
		rmsA: rmsA, rmsB: rmsB, diffRms: diffRms,
		diffDb: (diffRms / max(signalRms, 1e-12)).ampdb,
		correlation: cross / max((a2 * b2).sqrt, 1e-20),
		centroidA: centroidA, centroidB: centroidB,
		centroidDelta: (centroidA - centroidB).abs,
		centroidDiff: centroidDiff
	)
};

readSchmitt = { |path|
	var sf = SoundFile.openRead(path), data, channels, frames;
	var highCounts, rises, previous;
	if(sf.isNil) { Error("cannot read Schmitt render: " ++ path).throw };
	channels = sf.numChannels;
	frames = sf.numFrames;
	data = FloatArray.newClear(frames * channels);
	sf.readData(data);
	sf.close;
	highCounts = 0 ! channels;
	rises = 0 ! channels;
	previous = 0.0 ! channels;
	frames.do { |frame|
		channels.do { |channel|
			var value = data[(frame * channels) + channel];
			if(value > 0.5) { highCounts[channel] = highCounts[channel] + 1 };
			if(value > 0.5 and: { previous[channel] <= 0.5 }) {
				rises[channel] = rises[channel] + 1
			};
			previous[channel] = value;
		};
	};
	(duty: highCounts.collect { |count| count / frames }, rises: rises,
		channels: channels, frames: frames)
};

schmittScore = Score([
	[0.0, [\d_recv, schmittDef.asBytes]],
	[0.0, [\s_new, \cqTestPlayabilitySchmitt, 1000, 0, 0]],
	[3.0, [\c_set, 0, 0]]
]);

opts.numInputBusChannels = 0;
opts.blockSize = cfg[\blockSize];
opts.verbosity = -1;
File.mkdir(tmpDir);

{
	try {
		var schmittPath = tmpDir +/+ "nrt_playability_schmitt.wav";
		var gateStats;
		opts.numOutputBusChannels = 5;
		render.value(schmittScore, schmittPath, 3.0);
		gateStats = readSchmitt.value(schmittPath);
		("Schmitt weights 0.49/0.50/0.55/0.60/0.65: duty % | rises %")
			.format(gateStats[\duty].collect(_.round(0.0001)),
				gateStats[\rises]).postln;
		check.value(gateStats[\channels] == 5, "Schmitt boundary render has five probes");
		check.value(gateStats[\rises][0] == 0,
			"0.49 cannot cross the production +0.5 on-threshold");
		check.value(gateStats[\rises][2] > 0,
			"0.55 establishes the measured gate-capable floor");
		check.value(gateStats[\rises][3] > 0,
			"the lighter 0.60 production candidate keeps gate headroom");

		opts.numOutputBusChannels = 2;
		"Former first-touch cable: tri5 -> coco1_inVca, weight 0.5".postln;
		densitySteps.do { |density, index|
			var base = makeShake.value(7000 + index, density, amount);
			var without = addCable.value(base, \tri5, \coco1_inVca, 0.0);
			var with = addCable.value(base, \tri5, \coco1_inVca, baselineWeight);
			var path = tmpDir +/+ "nrt_playability_cable_density_%.wav"
				.format((density * 100).round.asInteger);
			var stats;
			render.value(makePairScore.value(without, with, true, 0), path, dur);
			stats = readPairStats.value(path);
			("density % (% cells): rms % / %, diff % (% dB rel), corr %, "
				++ "centroid % / % (delta %, diff % Hz)")
				.format(
					density, matrixCount.value(without),
					stats[\rmsA].round(0.000001), stats[\rmsB].round(0.000001),
					stats[\diffRms].round(0.000001), stats[\diffDb].round(0.01),
					stats[\correlation].round(0.0001),
					stats[\centroidA].round(1), stats[\centroidB].round(1),
					stats[\centroidDelta].round(1), stats[\centroidDiff].round(1))
				.postln;
			check.value(stats[\channels] == 2 and: {
				stats[\frames] >= (dur * sr * 0.99) },
				"density % render is complete stereo A/B".format(density));
			check.value(stats[\finite] and: { stats[\peak] <= 1.0 },
				"density % render is finite and bounded".format(density));
			check.value(max(stats[\rmsA], stats[\rmsB]) >= 0.005,
				"density % prepared fixture carries sound".format(density));
		};

		// PLAY-5 regression probe: the organ jack is written by the same Coco
		// that may read it back through the matrix. A/B uses the exact tutorial
		// values and otherwise identical deterministic held RAM.
		{
			var base = makeShake.value(5050, 0, amount);
			var with = addCable.value(base, \org1, \coco1_spaf, 0.2);
			var path = tmpDir +/+ "nrt_playability_org1_self_spaf.wav";
			var stats;
			render.value(makePairScore.value(base, with, false, 0.12), path, dur);
			stats = readPairStats.value(path);
			("org1 -> own SP.AF 0.2 at depth 0.12: rms % / %, diff % (% dB rel), "
				++ "corr %, centroid delta % Hz")
				.format(stats[\rmsA].round(0.000001), stats[\rmsB].round(0.000001),
					stats[\diffRms].round(0.000001), stats[\diffDb].round(0.01),
					stats[\correlation].round(0.0001),
					stats[\centroidDelta].round(1)).postln;
			check.value(stats[\finite] and: { stats[\peak] <= 1.0 },
				"org1 self-SP.AF probe is finite and bounded");
			check.value(stats[\diffRms] > 0.005,
				"org1 self-SP.AF route changes the production Coco path");
		}.value;

		// The live regression that exposed the shared-bus timing hole: the same
		// source could step a castle but appeared dead at every destination inside
		// its own Coco. A skip is deliberately used here because it bypasses all
		// questions of SP.AF depth or VCA audibility: crossing the production
		// Schmitt must reset the held read head and change the rendered loop.
		{
			var base = makeShake.value(5051, 0, amount);
			var with = addCable.value(base, \org1, \coco1_skip, 0.60);
			var path = tmpDir +/+ "nrt_playability_org1_self_skip.wav";
			var stats;
			render.value(makePairScore.value(base, with, false, 0), path, dur);
			stats = readPairStats.value(path);
			("org1 -> own SKIP 0.60: rms % / %, diff % (% dB rel), corr %")
				.format(stats[\rmsA].round(0.000001), stats[\rmsB].round(0.000001),
					stats[\diffRms].round(0.000001), stats[\diffDb].round(0.01),
					stats[\correlation].round(0.0001)).postln;
			check.value(stats[\finite] and: { stats[\peak] <= 1.0 },
				"org1 self-SKIP probe is finite and bounded");
			check.value(stats[\diffRms] > 0.005,
				"org1 self-SKIP crosses the Coco's production trigger path");
		}.value;

		{
			var path = tmpDir +/+ "nrt_playability_org1_cross_coco2_skip.wav";
			var stats;
			render.value(makeCrossOrganScore.value, path, dur);
			stats = readPairStats.value(path);
			("org1 -> coco2 SKIP 0.60: rms % / %, diff % (% dB rel), corr %")
				.format(stats[\rmsA].round(0.000001), stats[\rmsB].round(0.000001),
					stats[\diffRms].round(0.000001), stats[\diffDb].round(0.01),
					stats[\correlation].round(0.0001)).postln;
			check.value(stats[\finite] and: { stats[\peak] <= 1.0 },
				"org1 cross-Coco SKIP probe is finite and bounded");
			check.value(stats[\diffRms] > 0.005,
				"org1 crosses the other Coco's production trigger path");
		}.value;

		[
			[\fullMoon, 0.2, 8101, 9202, true, 0.3, 0],
			[\allCells, 1.0, 8101, 9202, true, 0.3, 0],
			[\bareEcology, 0.2, 8101, 9202, false, 0.3, 0],
			[\historicalSpaf012WithEcology, 0.2, 8101, 9202, false, 0.3, 0.12],
			[\historicalAmount065WithEcology, 0.2, 8101, 9202, false, 0.65, 0]
		].do { |row|
			var label = row[0], density = row[1];
			var shakeA = makeShake.value(row[2], density, row[5]);
			var shakeB = makeShake.value(row[3], density, row[5]);
			var prepared = row[4];
			var path = tmpDir +/+ "nrt_playability_shakes_%.wav".format(label);
			var stats;
			render.value(makePairScore.value(shakeA, shakeB, prepared, row[6]), path, dur);
			stats = readPairStats.value(path);
			("two % shakes (% / % cells): rms % / %, diff % (% dB rel), corr %, "
				++ "centroid % / % (delta %, diff % Hz)")
				.format(
					label, matrixCount.value(shakeA), matrixCount.value(shakeB),
					stats[\rmsA].round(0.000001), stats[\rmsB].round(0.000001),
					stats[\diffRms].round(0.000001), stats[\diffDb].round(0.01),
					stats[\correlation].round(0.0001),
					stats[\centroidA].round(1), stats[\centroidB].round(1),
					stats[\centroidDelta].round(1), stats[\centroidDiff].round(1))
				.postln;
			check.value(stats[\finite] and: { stats[\peak] <= 1.0 },
				"% shake pair is finite and bounded".format(label));
			check.value(max(stats[\rmsA], stats[\rmsB]) >= 0.005,
				"% shake pair carries sound".format(label));
		};
	} { |error|
		ok = false;
		results.add("FAIL: uncaught playability NRT error: " ++ error.errorString);
	};

	results.do(_.postln);
	if(ok) {
		"CQ_PLAYABILITY_MEASURED".postln;
		0.exit
	} {
		"CQ_PLAYABILITY_MEASURE_FAILED".postln;
		1.exit
	};
}.fork(AppClock);
)
