// NRT proof for ecological SHAKE from completely empty Coco RAM.
// The first deterministic seed for each family is rendered through a complete
// Quantussy + two-Coco production fragment with the family's own matrix AND the
// exact safe musical-control preparation returned by CQMatrix.lastShakePlan.
// Every family must speak, remain finite, and differ materially from every
// other family. Prints CQ_SHAKE_ECOLOGY_OK / _FAILED last.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var tmpDir = repo +/+ "tests" +/+ "tmp";
var cfg = CQConfig.validate(()), defs = CQSynthDefs.all(cfg);
var defaults = CQState(nil).asEvent;
var familyOrder = #[organDrone, memoryPulse, dustGrowth, crossfeed];
var found = IdentityDictionary.new, seed = 1;
var density = CQGui.sowDensity(CQGui.defaultShakeAmount);
var amount = 0.3, sr = 48000, dur = 12.0, analysisStart = 2.0;
var opts = ServerOptions.new, results = List.new, ok = true;
var branches, sinkDef, makeScore, render, statsFor, check;

check = { |condition, message|
	if(condition) { results.add("ok:   " ++ message) } {
		ok = false;
		results.add("FAIL: " ++ message)
	};
};

while { found.size < familyOrder.size and: { seed < 1000 } } {
	var matrix = CQMatrix(nil);
	var spesal = matrix.shake(amount, seed, density: density, context: defaults);
	var plan = matrix.lastShakePlan;
	if(found[plan[\family]].isNil) {
		found[plan[\family]] = (
			seed: seed, weights: matrix.fullMatrix, spesal: spesal.copy,
			plan: plan.deepCopy
		)
	};
	seed = seed + 1;
};

if(found.size != familyOrder.size) {
	Error("could not find all four deterministic shake families").throw
};

// Four disjoint production fragments. Matrix control buses need 448 slots per
// branch (28 destinations x 16 sources), so each gets a 500-bus country.
branches = 4.collect { |i|
	(mod: 16 + (i * 32), qMix: 32 + (i * 32), coco1: 33 + (i * 32),
		coco2: 34 + (i * 32), silent: 35 + (i * 32),
		ctl: 100 + (i * 500), buf1: i * 2, buf2: (i * 2) + 1,
		out: i, node: 1100 + (i * 20))
};

sinkDef = SynthDef(\cqTestShakeEcologySink, {
	Out.ar(\out.ir(0), (In.ar(\coco1.ir(33)) + In.ar(\coco2.ir(34))) * 0.5)
});

makeScore = {
	var score = Score.new;
	defs.do { |def| score.add([0.0, [\d_recv, def.asBytes]]) };
	score.add([0.0, [\d_recv, sinkDef.asBytes]]);
	familyOrder.do { |family, i|
		var branch = branches[i], patch = found[family], plan = patch[\plan];
		var params = plan[\params];
		var value = { |name| params[name] ? defaults[name] };
		var enum = { |name| CQSpec.enumValues(name).indexOf(value.value(name)) };
		var destBus = { |name|
			branch[\ctl] + (CQMatrix.destIndex(name) * 16)
		};
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
				\wRecordBus, destBus.value(("coco" ++ n ++ "_record").asSymbol),
				\patchDepth, value.value(\patchDepth),
				\speed, value.value(prefix.value("Speed")),
				\spafKnob, value.value(prefix.value("Spaf")),
				\inLevel, value.value(prefix.value("In")),
				\fbLevel, value.value(prefix.value("Fb")),
				\dust, value.value(prefix.value("Dust")),
				\hold, value.value(prefix.value("Hold")),
				\organLvl, value.value(prefix.value("Organ")),
				\level, value.value(prefix.value("Level")),
				// Empty-Coco challenge: no preamp or deck can seed the result.
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
				++ patch[\weights][di]])
		};
		(1..5).do { |k|
			quantussyArgs = quantussyArgs ++ [
				("state" ++ k).asSymbol, patch[\spesal][k - 1],
				("range" ++ k).asSymbol, enum.value(("c" ++ k ++ "Range").asSymbol),
				("off" ++ k).asSymbol, value.value(("c" ++ k ++ "Off").asSymbol),
				("send" ++ k).asSymbol, value.value(("c" ++ k ++ "Send").asSymbol),
				("wFm" ++ k).asSymbol,
					destBus.value(("castle" ++ k ++ "_fm").asSymbol),
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
		score.add([0.0, [\s_new, \cqTestShakeEcologySink, branch[\node] + 4,
			1, branch[\node], \coco1, branch[\coco1],
			\coco2, branch[\coco2], \out, branch[\out]]]);
	};
	score.add([dur, [\c_set, 0, 0]]);
	score
};

render = { |score, path|
	var done = false, waited = 0.0;
	score.recordNRT(nil, path, nil, sr, "WAV", "float", opts,
		duration: dur, action: { done = true });
	while { done.not and: { waited < 180 } } {
		0.2.wait;
		waited = waited + 0.2
	};
	if(done.not) { Error("shake ecology NRT timed out: " ++ path).throw };
};

statsFor = { |path|
	var sound = SoundFile.openRead(path), data, channels, frames, start, count;
	var sums, peaks, finite = true, diffs;
	if(sound.isNil) { Error("cannot read shake ecology NRT: " ++ path).throw };
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
			var value = data[(frame * channels) + channel];
			if(value.isNaN or: { value.abs == inf }) { finite = false };
			sums[channel] = sums[channel] + value.squared;
			peaks[channel] = max(peaks[channel], value.abs);
			if(channel < (channels - 1)) {
				(channel + 1 .. (channels - 1)).do { |other|
					var delta = value - data[(frame * channels) + other];
					diffs[channel][other] = diffs[channel][other] + delta.squared
				};
			};
		};
	};
	(channels: channels, frames: frames, finite: finite,
		rms: sums.collect { |sum| (sum / count).sqrt }, peaks: peaks,
		diffRms: diffs.collect { |row| row.collect { |sum| (sum / count).sqrt } })
};

opts.numOutputBusChannels = 4;
opts.numInputBusChannels = 0;
opts.blockSize = cfg[\blockSize];
opts.verbosity = -1;
File.mkdir(tmpDir);

{
	var path = tmpDir +/+ "nrt_shake_ecologies.wav", stats;
	try {
		familyOrder.do { |family|
			var patch = found[family];
			("shake family %: seed %, % cables, skeleton %")
				.format(family, patch[\seed],
					patch[\weights].sum { |row| row.count { |weight| weight != 0 } },
					patch[\plan][\skeleton]).postln
		};
		render.value(makeScore.value, path);
		stats = statsFor.value(path);
		("shake ecology RMS % | peaks %")
			.format(stats[\rms].collect(_.round(0.000001)),
				stats[\peaks].collect(_.round(0.000001))).postln;
		check.value(stats[\channels] == 4, "one render carries all four shake families");
		check.value(stats[\finite], "all four ecological shakes remain finite");
		familyOrder.do { |family, i|
			check.value(stats[\rms][i] >= 0.003,
				"% speaks from empty Coco RAM (RMS %)"
					.format(family, stats[\rms][i].round(0.000001)));
		};
		3.do { |i|
			(i + 1 .. 3).do { |j|
				var reference = max((stats[\rms][i] + stats[\rms][j]) * 0.5, 1e-12);
				var relativeDb = (stats[\diffRms][i][j] / reference).ampdb;
				check.value(relativeDb >= -12,
					"% and % differ materially (% dB relative difference)"
						.format(familyOrder[i], familyOrder[j], relativeDb.round(0.01)));
			};
		};
	} { |error|
		ok = false;
		results.add("FAIL: uncaught shake ecology NRT error: " ++ error.errorString);
	};
	results.do(_.postln);
	if(ok) { "CQ_SHAKE_ECOLOGY_OK".postln; 0.exit } {
		"CQ_SHAKE_ECOLOGY_FAILED".postln; 1.exit
	};
}.fork(AppClock);
)
