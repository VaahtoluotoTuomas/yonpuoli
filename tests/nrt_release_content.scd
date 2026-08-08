// Release-content proof from completely empty Coco RAM. The two new authored
// presets and all four artistic release snapshots run through production
// Quantussy/Coco SynthDefs. Every scene must speak without private samples,
// stay finite/bounded, and remain materially distinct from its peers.
// Run only through tests/run_sclang_gate.ps1.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var tmpDir = repo +/+ "tests" +/+ "tmp";
var cfg = CQConfig.validate(()), defs = CQSynthDefs.all(cfg);
var defaults = CQState(nil).asEvent;
var names = #[crossCurrent, dustGarden, undertow, dust_bloom, glass_tide, organ_lanterns];
var scenes = List.new, releaseSnaps, dummyState, dummyMatrix, dummySpesal;
var sr = 48000, dur = 18.0, analysisStart = 3.0;
var opts = ServerOptions.new, results = List.new, ok = true;
var branches, sinkDef, makeScore, render, statsFor, check;

check = { |condition, message|
	if(condition) { results.add("ok:   " ++ message) } {
		ok = false;
		results.add("FAIL: " ++ message)
	};
};

// Preset overlays are evaluated against a clean boot state.
#[crossCurrent, dustGarden].do { |name|
	var data = CQMatrix.presets[name], params = defaults.copy;
	var matrix = CQMatrix(nil);
	data[\params].keysValuesDo { |key, value| params[key] = CQSpec.clamp(key, value) };
	matrix.load(data[\matrix]);
	scenes.add((name: name, params: params, weights: matrix.fullMatrix,
		spesal: [0, 0, 0, 0, 0]));
};

// Read only the four exact manifest names; personal gitignored memories are
// neither listed nor touched.
dummyState = CQState(nil);
dummyMatrix = CQMatrix(nil);
dummySpesal = [0, 0, 0, 0, 0];
releaseSnaps = CQSnapshots(dummyState, dummyMatrix, repo +/+ "snapshots",
	{ dummySpesal.copy }, { |values| dummySpesal = values.copy }, repo +/+ "snapshots");
#[undertow, dust_bloom, glass_tide, organ_lanterns].do { |name|
	var data = releaseSnaps.read(name), matrix = CQMatrix(nil);
	if(data.isNil) { Error("missing release snapshot " ++ name).throw };
	matrix.load(data[\matrix]);
	scenes.add((name: name, params: data[\params], weights: matrix.fullMatrix,
		spesal: data[\spesal]));
};
releaseSnaps.free;

branches = scenes.size.collect { |i|
	(mod: 32 + (i * 32), qMix: 48 + (i * 32), coco1: 49 + (i * 32),
		coco2: 50 + (i * 32), silent: 51 + (i * 32),
		ctl: 256 + (i * 600), buf1: i * 2, buf2: (i * 2) + 1,
		out: i, node: 2100 + (i * 20))
};

sinkDef = SynthDef(\cqTestReleaseContentSink, {
	Out.ar(\out.ir(0), (In.ar(\coco1.ir(49)) + In.ar(\coco2.ir(50)))
		* 0.5 * \gain.ir(1))
});

makeScore = {
	var score = Score.new;
	defs.do { |def| score.add([0.0, [\d_recv, def.asBytes]]) };
	score.add([0.0, [\d_recv, sinkDef.asBytes]]);
	scenes.do { |scene, i|
		var branch = branches[i], params = scene[\params];
		var value = { |name| params[name] ? defaults[name] };
		var enum = { |name| CQSpec.enumValues(name).indexOf(value.value(name)) };
		var destBus = { |name| branch[\ctl] + (CQMatrix.destIndex(name) * 16) };
		var cocoArgs = { |n|
			var prefix = { |suffix| ("coco" ++ n ++ suffix).asSymbol };
			var other = if(n == 1) { branch[\coco2] } { branch[\coco1] };
			var tap = if(n == 1) { 12 } { 13 };
			var organTap = if(n == 1) { 14 } { 15 };
			[
				\buf, if(n == 1) { branch[\buf1] } { branch[\buf2] },
				\preBus, branch[\silent], \deckBus, branch[\silent],
				\qMixBus, branch[\qMix], \crossBus, other,
				\outBus, if(n == 1) { branch[\coco1] } { branch[\coco2] },
				\modSrcBus, branch[\mod], \modTapIdx, tap,
				\modOrganIdx, organTap, \cocoIndex, n - 1,
				\wSpafBus, destBus.value(("coco" ++ n ++ "_spaf").asSymbol),
				\wInVcaBus, destBus.value(("coco" ++ n ++ "_inVca").asSymbol),
				\wFbVcaBus, destBus.value(("coco" ++ n ++ "_fbVca").asSymbol),
				\wFlipBus, destBus.value(("coco" ++ n ++ "_flip").asSymbol),
				\wSkipBus, destBus.value(("coco" ++ n ++ "_skip").asSymbol),
				\wOrganBus, destBus.value(("coco" ++ n ++ "_organ").asSymbol),
				\wSlotQBus, destBus.value(("coco" ++ n ++ "_slotQ").asSymbol),
				\wSlotXBus, destBus.value(("coco" ++ n ++ "_slotX").asSymbol),
				\wDustBus, destBus.value(("coco" ++ n ++ "_dust").asSymbol),
				\wRecordBus, destBus.value(("coco" ++ n ++ "_record").asSymbol),
				\patchDepth, value.value(\patchDepth),
				\speed, value.value(prefix.value("Speed")),
				\spafKnob, value.value(prefix.value("Spaf")),
				\inLevel, value.value(prefix.value("In")),
				\fbLevel, value.value(prefix.value("Fb")),
				\dust, value.value(prefix.value("Dust")),
				\dustGrain, value.value(prefix.value("DustGrain")),
				\hold, value.value(prefix.value("Hold")),
				\flip, value.value(prefix.value("Flip")),
				\dolbyIn, enum.value(prefix.value("DolbyIn")),
				\dolbyFb, enum.value(prefix.value("DolbyFb")),
				\bits, value.value(prefix.value("Bits")),
				\wrap, value.value(prefix.value("Wrap")),
				\interp, enum.value(prefix.value("Interp")),
				\organLvl, value.value(prefix.value("Organ")),
				\level, value.value(prefix.value("Level")),
				// The proof is intentionally independent of local audio files/devices.
				\slotPre, 0, \slotDeck, 0,
				\slotQ, value.value(prefix.value("SlotQ")),
				\slotX, value.value(prefix.value("SlotX"))
			]
		};
		var quantussyArgs = [
			\modSrcBus, branch[\mod], \qMixBus, branch[\qMix],
			\qFreq, value.value(\qFreq), \qChaos, value.value(\qChaos),
			\qShLag, value.value(\qShLag), \patchDepth, value.value(\patchDepth)
		];

		score.add([0.0, [\b_alloc, branch[\buf1], 32768, 1]]);
		score.add([0.0, [\b_alloc, branch[\buf2], 32768, 1]]);
		CQMatrix.destNames.do { |dest, di|
			score.add([0.0, [\c_setn, branch[\ctl] + (di * 16), 16]
				++ scene[\weights][di]])
		};
		(1..5).do { |k|
			quantussyArgs = quantussyArgs ++ [
				("state" ++ k).asSymbol, scene[\spesal][k - 1],
				("range" ++ k).asSymbol, enum.value(("c" ++ k ++ "Range").asSymbol),
				("off" ++ k).asSymbol, value.value(("c" ++ k ++ "Off").asSymbol),
				("send" ++ k).asSymbol, value.value(("c" ++ k ++ "Send").asSymbol),
				("wFm" ++ k).asSymbol, destBus.value(("castle" ++ k ++ "_fm").asSymbol),
				("wSpesal" ++ k).asSymbol,
					destBus.value(("castle" ++ k ++ "_spesal").asSymbol)
			]
		};
		score.add([0.0, [\g_new, branch[\node], 0, 0]]);
		score.add([0.0, [\s_new, \cqQuantussy, branch[\node] + 1, 1,
			branch[\node]] ++ quantussyArgs]);
		score.add([0.0, [\s_new, \cqCoco, branch[\node] + 2, 1,
			branch[\node]] ++ cocoArgs.value(1)]);
		score.add([0.0, [\s_new, \cqCoco, branch[\node] + 3, 1,
			branch[\node]] ++ cocoArgs.value(2)]);
		score.add([0.0, [\s_new, \cqTestReleaseContentSink, branch[\node] + 4,
			1, branch[\node], \coco1, branch[\coco1], \coco2, branch[\coco2],
			\out, branch[\out], \gain, value.value(\masterVol).dbamp]]);
	};
	score.add([dur, [\c_set, 0, 0]]);
	score
};

render = { |score, path|
	var done = false, waited = 0.0;
	score.recordNRT(nil, path, nil, sr, "WAV", "float", opts,
		duration: dur, action: { done = true });
	while { done.not and: { waited < 180 } } { 0.2.wait; waited = waited + 0.2 };
	if(done.not) { Error("release-content NRT timed out: " ++ path).throw };
};

statsFor = { |path|
	var sound = SoundFile.openRead(path), data, channels, frames, start, count;
	var sums, peaks, finite = true, diffs;
	if(sound.isNil) { Error("cannot read release-content NRT: " ++ path).throw };
	channels = sound.numChannels;
	frames = sound.numFrames;
	data = FloatArray.newClear(frames * channels);
	sound.readData(data);
	sound.close;
	start = (analysisStart * sr).asInteger.min(frames - 1);
	count = max(1, frames - start);
	sums = 0.0 ! channels;
	peaks = 0.0 ! channels;
	diffs = Array.fill(channels, { 0.0 ! channels });
	(start .. (frames - 1)).do { |frame|
		channels.do { |channel|
			var sample = data[(frame * channels) + channel];
			if(sample.isNaN or: { sample.abs == inf }) { finite = false };
			sums[channel] = sums[channel] + sample.squared;
			peaks[channel] = max(peaks[channel], sample.abs);
			if(channel < (channels - 1)) {
				(channel + 1 .. (channels - 1)).do { |other|
					var delta = sample - data[(frame * channels) + other];
					diffs[channel][other] = diffs[channel][other] + delta.squared;
				};
			};
		};
	};
	(channels: channels, frames: frames, finite: finite,
		rms: sums.collect { |sum| (sum / count).sqrt }, peaks: peaks,
		diffRms: diffs.collect { |row| row.collect { |sum| (sum / count).sqrt } })
};

opts.numOutputBusChannels = scenes.size;
opts.numInputBusChannels = 0;
opts.blockSize = cfg[\blockSize];
opts.verbosity = -1;
File.mkdir(tmpDir);

{
	var path = tmpDir +/+ "nrt_release_content.wav", stats;
	try {
		render.value(makeScore.value, path);
		stats = statsFor.value(path);
		("release content post-master RMS % | peaks %").format(
			stats[\rms].collect(_.round(0.000001)),
			stats[\peaks].collect(_.round(0.000001))).postln;
		check.value(stats[\channels] == scenes.size,
			"one render carries the two new presets and four artistic snapshots");
		check.value(stats[\finite], "every release scene remains finite");
		names.do { |name, i|
			check.value(stats[\rms][i] >= 0.001,
				"% speaks from empty RAM (RMS %)"
					.format(name, stats[\rms][i].round(0.000001)));
			check.value(stats[\peaks][i] <= 1.5,
				"% remains converter/output bounded (peak %)"
					.format(name, stats[\peaks][i].round(0.000001)));
		};
		// The two recipes must not be cosmetic variants of each other.
		check.value((stats[\diffRms][0][1]
			/ max((stats[\rms][0] + stats[\rms][1]) * 0.5, 1e-12)).ampdb >= -18,
			"crossCurrent and dustGarden have materially different production output");
		// Every pair among the four shipped scenes must justify a Memory slot.
		(2..4).do { |i|
			(i + 1 .. 5).do { |j|
				var reference = max((stats[\rms][i] + stats[\rms][j]) * 0.5, 1e-12);
				var relativeDb = (stats[\diffRms][i][j] / reference).ampdb;
				check.value(relativeDb >= -18,
					"% and % are materially distinct (% dB relative difference)"
						.format(names[i], names[j], relativeDb.round(0.01)));
			};
		};
		{
			var snapshotRms = stats[\rms].copyRange(2, 5);
			var spreadDb = (snapshotRms.maxItem / snapshotRms.minItem.max(1e-12)).ampdb;
			check.value(spreadDb <= 12,
				"the four recalled scenes stay within a musical 12 dB loudness country (% dB)"
					.format(spreadDb.round(0.01)));
		}.value;
	} { |error|
		ok = false;
		results.add("FAIL: uncaught release-content NRT error: " ++ error.errorString);
	};
	results.do(_.postln);
	if(ok) { "CQ_RELEASE_CONTENT_OK".postln; 0.exit } {
		"CQ_RELEASE_CONTENT_FAILED".postln; 1.exit
	};
}.fork(AppClock);
)
