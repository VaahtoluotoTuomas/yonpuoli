// NRT safety render (SPEC §10 A3). Renders the REAL SynthDefs offline (scsynth
// -N, no audio device) under worst-case input and asserts: peak <= -0.75 dBFS,
// no NaN/inf, |DC| <= 0.02. Thresholds may NEVER be raised (CLAUDE.md safety #6).
// Cases: master-chain torture, coco worst case (fb 1.25 + drive 40 + wrap +
// dust max + max cross-feed), quantussy worst case (all castles audio range,
// chaos 1, all sends up, through coco1's converter — qMix must be audible AND
// bounded), the full \mayhem matrix worst case (SPEC A3, 90 s), a matrix
// castle-FM routing-liveness regression (tri1 -> castle5_fm), NaN injection.
// Prints CQ_SAFETY_PASSED / CQ_SAFETY_FAILED as its last line.
// Run: "<sclang.exe>" -l tests/sclang_conf_dev.yaml -i none tests/nrt_safety.scd

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
// ^ realpath: nowExecutingPath is relative when sclang gets a relative script
// argument — bare dirname would then write renders to tests/tests/tmp
var tmpDir = repo +/+ "tests" +/+ "tmp";
var cfg = CQConfig.validate(());       // deterministic defaults, never the user's config
var defs = CQSynthDefs.all(cfg);
var stemCfg = CQConfig.validate((stemOuts: [2, 3]));   // phase 7: stem-out master
var stemMasterDef = CQSynthDefs.master(stemCfg);       // same \cqMaster def, stemOuts baked
var dur = 10.0;
var sr = 48000;
var opts = ServerOptions.new;
var opts4 = ServerOptions.new;         // 4-ch out for the stem render (mix + 2 stems)
var results = List.new;
var ok = true;
var check = { |bool, msg|
	if(bool) { results.add("ok:   " ++ msg) } { ok = false; results.add("FAIL: " ++ msg) };
};

// NRT bus plan (in = 2, out = 2 -> private audio buses start at index 4)
var bCoco1 = 4, bCoco2 = 5, bDry = 8;
var bDeckA = 10, bDeckB = 11, bPreA = 12, bPreB = 13, bQMix = 14, bModSrc = 16;
// Phase 7: the master's post-safety stem taps live past bModSrc's 16 channels,
// 16..31. Every \cqMaster below is given the stem
// buses so its always-on stem writes never land on hardware out 0 (they carry the
// full post-safety Coco signal, not silence — leaking them would double the mix).
var bStem1 = 32, bStem2 = 33;
var cocoBufFrames = 32768;   // short loop -> many recirculation passes in 10 s
var mayhemDur = 90.0;        // SPEC A3 duration for the full-matrix worst case

// what a runaway patch could hand the master: 4x full scale + DC offsets
var tortureDef = SynthDef(\cqTestTorture, {
	Out.ar(\c1.ir(4), WhiteNoise.ar(4) + DC.ar(0.5));
	Out.ar(\c2.ir(5), WhiteNoise.ar(4) - DC.ar(0.5));
	Out.ar(\dry.ir(8), PinkNoise.ar([2, 2]) + DC.ar([1, -1]));
});
// same abuse, but into the deck buses = through the cocos' converter chain
var feedDef = SynthDef(\cqTestFeed, {
	Out.ar(\a.ir(10), WhiteNoise.ar(4) + DC.ar(0.5));
	Out.ar(\b.ir(11), WhiteNoise.ar(4) - DC.ar(0.5));
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
		\coco1Bus, bCoco1, \coco2Bus, bCoco2, \dryBus, bDry,
		\stem1Bus, bStem1, \stem2Bus, bStem2,
		\masterVol, 0, \masterDry, 1, \fadeIn, 1]]);   // vol maxed = worst case
	score.add([dur, [\c_set, 0, 0]]);
	score
};

// phase-2 worst case through the REAL coco pair: preamp drive/bias maxed, both
// converter chains at inLevel 2 / fb 1.25 / dust 0.5 / wrap on / bits 3, all
// slots (incl. cross-feed) at 1.5, recording (hold 0) — clip2/wrap2 inside the
// recirculation is the invariant under test (SPEC §8.2)
var makeCocoScore = {
	var score = Score.new;
	var cocoArgs = { |buf, pre, deck, cross, out, tap, org|
		[\buf, buf, \preBus, pre, \deckBus, deck, \qMixBus, bQMix,
			\crossBus, cross, \outBus, out, \modSrcBus, bModSrc,
			\modTapIdx, tap, \modOrganIdx, org,
			\cocoIndex, tap - 12,
			\inLevel, 2, \fbLevel, 1.25, \dust, 0.5, \wrap, 1, \bits, 3,
			\hold, 0, \organLvl, 1, \level, 1.5, \speed, 24000,
			\slotPre, 1.5, \slotDeck, 1.5, \slotQ, 0, \slotX, 1.5]
	};
	defs.do { |d| score.add([0.0, [\d_recv, d.asBytes]]) };
	score.add([0.0, [\d_recv, feedDef.asBytes]]);
	score.add([0.0, [\b_alloc, 0, cocoBufFrames, 1]]);
	score.add([0.0, [\b_alloc, 1, cocoBufFrames, 1]]);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqTestFeed, 1001, 1, 1000, \a, bDeckA, \b, bDeckB]]);
	score.add([0.0, [\s_new, \cqInput, 1002, 1, 1000,
		\deckABus, bDeckA, \deckBBus, bDeckB, \modSrcBus, bModSrc,
		\preABus, bPreA, \preBBus, bPreB, \dryBus, bDry,
		\preASrc, 3, \preBSrc, 4,          // enum indices: deckA, deckB
		\preADrive, 40, \preBDrive, 40, \preABias, 0.6, \preBBias, 0.6]]);
	score.add([0.0, ([\s_new, \cqCoco, 1003, 1, 1000]
		++ cocoArgs.value(0, bPreA, bDeckA, bCoco2, bCoco1, 12, 14))]);
	score.add([0.0, ([\s_new, \cqCoco, 1004, 1, 1000]
		++ cocoArgs.value(1, bPreB, bDeckB, bCoco1, bCoco2, 13, 15))]);
	score.add([0.0, [\s_new, \cqMaster, 1005, 1, 1000,
		\coco1Bus, bCoco1, \coco2Bus, bCoco2, \dryBus, bDry,
		\stem1Bus, bStem1, \stem2Bus, bStem2,
		\masterVol, 0, \masterDry, 1, \fadeIn, 1]]);
	score.add([dur, [\c_set, 0, 0]]);
	score
};

// phase-3 case: the full quantussy at its wildest (every castle on audio range,
// qFreq maxed, chaos 1) feeding coco1's converter via slotQ with the converter
// itself at its phase-2 worst case — asserts the qMix path is non-silent and
// the recirculation stays bounded (PLAN vaihe 3)
var makeQuantussyScore = {
	var score = Score.new;
	defs.do { |d| score.add([0.0, [\d_recv, d.asBytes]]) };
	score.add([0.0, [\b_alloc, 0, cocoBufFrames, 1]]);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqQuantussy, 1001, 1, 1000,
		\modSrcBus, bModSrc, \qMixBus, bQMix,
		\qFreq, 500, \qChaos, 1,
		\range1, 2, \range2, 2, \range3, 2, \range4, 2, \range5, 2,
		\send1, 1, \send2, 1, \send3, 1, \send4, 1, \send5, 1]]);
	score.add([0.0, [\s_new, \cqCoco, 1002, 1, 1000,
		\buf, 0, \preBus, bPreA, \deckBus, bDeckA, \qMixBus, bQMix,
		\crossBus, bCoco2, \outBus, bCoco1, \modSrcBus, bModSrc,
		\modTapIdx, 12, \modOrganIdx, 14,
		\cocoIndex, 0,
		\inLevel, 2, \fbLevel, 1.25, \dust, 0.5, \wrap, 1, \bits, 3,
		\hold, 0, \level, 1.5, \speed, 24000,
		\slotPre, 0, \slotDeck, 0, \slotQ, 1.5, \slotX, 0]]);
	score.add([0.0, [\s_new, \cqMaster, 1003, 1, 1000,
		\coco1Bus, bCoco1, \coco2Bus, bCoco2, \dryBus, bDry,
		\stem1Bus, bStem1, \stem2Bus, bStem2,
		\masterVol, 0, \masterDry, 0, \fadeIn, 1]]);
	score.add([dur, [\c_set, 0, 0]]);
	score
};

// SPEC A3: \mayhem + fb 1.25 + drive 40 + wrap on + dust max + chaos 1 + qFreq 500 +
// spaf ±4, 90 s, no input — the full graph (input + quantussy + both cocos + master)
// wired through the REAL matrix control buses with every cell patched (deterministic
// \mayhem preset, CQMatrix §5.5). Matrix control buses are placed at offset 100 so
// they never collide with the `c_set 0 0` end-of-score marker every case here uses.
var makeMayhemScore = {
	var score = Score.new;
	var destNames = CQMatrix.destNames;
	var srcNames = CQMatrix.sourceNames;
	var busOffset = 100;
	var wArr = destNames.collect { srcNames.collect { 0.0 } };
	var destBus = { |name| busOffset + (destNames.indexOf(name) * 16) };
	var cocoArgs = { |buf, pre, deck, cross, out, tap, org, coco|
		[\buf, buf, \preBus, pre, \deckBus, deck, \qMixBus, bQMix,
			\crossBus, cross, \outBus, out, \modSrcBus, bModSrc,
			\modTapIdx, tap, \modOrganIdx, org,
			\cocoIndex, tap - 12,
			\inLevel, 2, \fbLevel, 1.25, \dust, 0.5, \wrap, 1, \bits, 3,
			\hold, 0, \organLvl, 1, \level, 1.5, \speed, 24000, \spafKnob, 4.0,
			\slotPre, 1.5, \slotDeck, 1.5, \slotQ, 1.5, \slotX, 1.5,
			\patchDepth, 1.0,
			\wSpafBus, destBus.value(("coco" ++ coco ++ "_spaf").asSymbol),
			\wInVcaBus, destBus.value(("coco" ++ coco ++ "_inVca").asSymbol),
			\wFbVcaBus, destBus.value(("coco" ++ coco ++ "_fbVca").asSymbol),
			\wFlipBus, destBus.value(("coco" ++ coco ++ "_flip").asSymbol),
			\wSkipBus, destBus.value(("coco" ++ coco ++ "_skip").asSymbol)]
	};
	CQMatrix.presets[\mayhem][\matrix].do { |t|
		var si = srcNames.indexOf(t[0]), di = destNames.indexOf(t[1]);
		wArr[di][si] = t[2];
	};
	defs.do { |d| score.add([0.0, [\d_recv, d.asBytes]]) };
	score.add([0.0, [\b_alloc, 0, cocoBufFrames, 1]]);
	score.add([0.0, [\b_alloc, 1, cocoBufFrames, 1]]);
	destNames.do { |name, di| score.add([0.0, [\c_setn, busOffset + (di * 16), 16] ++ wArr[di]]) };
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqInput, 1001, 1, 1000,
		\deckABus, bDeckA, \deckBBus, bDeckB, \modSrcBus, bModSrc,
		\preABus, bPreA, \preBBus, bPreB, \dryBus, bDry,
		\preADrive, 40, \preBDrive, 40, \preABias, 0.6, \preBBias, 0.6]]);
	score.add([0.0, ([\s_new, \cqQuantussy, 1002, 1, 1000,
		\modSrcBus, bModSrc, \qMixBus, bQMix,
		\qFreq, 500, \qChaos, 1, \patchDepth, 1.0,
		\range1, 2, \range2, 2, \range3, 2, \range4, 2, \range5, 2,
		\send1, 1, \send2, 1, \send3, 1, \send4, 1, \send5, 1]
		++ (1..5).collect { |k|
			[("wFm" ++ k).asSymbol, destBus.value(("castle" ++ k ++ "_fm").asSymbol),
			 ("wSpesal" ++ k).asSymbol, destBus.value(("castle" ++ k ++ "_spesal").asSymbol)]
		}.flatten)]);
	score.add([0.0, ([\s_new, \cqCoco, 1003, 1, 1000]
		++ cocoArgs.value(0, bPreA, bDeckA, bCoco2, bCoco1, 12, 14, 1))]);
	score.add([0.0, ([\s_new, \cqCoco, 1004, 1, 1000]
		++ cocoArgs.value(1, bPreB, bDeckB, bCoco1, bCoco2, 13, 15, 2))]);
	score.add([0.0, [\s_new, \cqMaster, 1005, 1, 1000,
		\coco1Bus, bCoco1, \coco2Bus, bCoco2, \dryBus, bDry,
		\stem1Bus, bStem1, \stem2Bus, bStem2,
		\masterVol, 0, \masterDry, 0, \fadeIn, 1]]);
	score.add([mayhemDur, [\c_set, 0, 0]]);
	score
};

// matrix-routing regression (audit 2026-07-16): tri1 (infra, 0.08 Hz triangle from
// phase 0) patched at weight 1 into castle5_fm; castle 5 on audio range at 160 Hz,
// chaos 0, off 0 -> its pitch must sweep UP in the render's first half (tri1 > 0)
// and DOWN in the second (tri1 < 0). Asserted via zero-crossing counts per half.
// Guards the quantussy-reads-its-own-outputs-via-InFeedback rule: with a plain
// In.ar the tri/cas sources are silently zero inside the quantussy and the halves
// come out identical (the \classic castle-FM ring would be dead).
var makeMatrixFmScore = {
	var score = Score.new;
	var wFm5Bus = 100;                          // clear of the c_set 0 0 end marker
	var w = 0.0 ! 16;
	w[0] = 1.0;                                 // source index 0 = tri1 (ARCH §3.2)
	defs.do { |d| score.add([0.0, [\d_recv, d.asBytes]]) };
	score.add([0.0, ([\c_setn, wFm5Bus, 16] ++ w)]);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqQuantussy, 1001, 1, 1000,
		\modSrcBus, bModSrc, \qMixBus, bQMix,
		\qFreq, 4, \qChaos, 0, \off5, 0,
		\range5, 2, \send5, 1,                  // castle 5: audio range, 4*40 = 160 Hz
		\wFm5, wFm5Bus]]);
	score.add([0.0, [\s_new, \cqMaster, 1002, 1, 1000,
		\coco1Bus, bQMix, \coco2Bus, bPreA,     // qMix straight into the master; the
		\dryBus, bDry, \stem1Bus, bStem1, \stem2Bus, bStem2, // other inputs read silent buses
		\masterVol, 0, \masterDry, 0, \fadeIn, 1]]);
	score.add([dur, [\c_set, 0, 0]]);
	score
};

// phase 7: stem outputs (SPEC §5.7, safety #1 "identical chain on each stem"). The
// master is built with stemOuts [2,3]; the coco worst case drives it and the render
// is 4-ch. Channels 2 & 3 are the post-safety Coco1/Coco2 stems — asserted bounded
// AND non-silent, proving the safety chain holds on the stems, not just the main mix.
var makeStemScore = {
	var score = Score.new;
	var cocoArgs = { |buf, pre, deck, cross, out, tap, org|
		[\buf, buf, \preBus, pre, \deckBus, deck, \qMixBus, bQMix,
			\crossBus, cross, \outBus, out, \modSrcBus, bModSrc,
			\modTapIdx, tap, \modOrganIdx, org,
			\cocoIndex, tap - 12,
			\inLevel, 2, \fbLevel, 1.25, \dust, 0.5, \wrap, 1, \bits, 3,
			\hold, 0, \organLvl, 1, \level, 1.5, \speed, 24000,
			\slotPre, 1.5, \slotDeck, 1.5, \slotQ, 0, \slotX, 1.5]
	};
	defs.do { |d| score.add([0.0, [\d_recv, d.asBytes]]) };
	score.add([0.0, [\d_recv, feedDef.asBytes]]);
	score.add([0.0, [\d_recv, stemMasterDef.asBytes]]);   // \cqMaster rebuilt with stemOuts
	score.add([0.0, [\b_alloc, 0, cocoBufFrames, 1]]);
	score.add([0.0, [\b_alloc, 1, cocoBufFrames, 1]]);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqTestFeed, 1001, 1, 1000, \a, bDeckA, \b, bDeckB]]);
	score.add([0.0, [\s_new, \cqInput, 1002, 1, 1000,
		\deckABus, bDeckA, \deckBBus, bDeckB, \modSrcBus, bModSrc,
		\preABus, bPreA, \preBBus, bPreB, \dryBus, bDry,
		\preASrc, 3, \preBSrc, 4, \preADrive, 40, \preBDrive, 40,
		\preABias, 0.6, \preBBias, 0.6]]);
	score.add([0.0, ([\s_new, \cqCoco, 1003, 1, 1000]
		++ cocoArgs.value(0, bPreA, bDeckA, bCoco2, bCoco1, 12, 14))]);
	score.add([0.0, ([\s_new, \cqCoco, 1004, 1, 1000]
		++ cocoArgs.value(1, bPreB, bDeckB, bCoco1, bCoco2, 13, 15))]);
	score.add([0.0, [\s_new, \cqMaster, 1005, 1, 1000,
		\coco1Bus, bCoco1, \coco2Bus, bCoco2,
		\dryBus, bDry, \stem1Bus, bStem1, \stem2Bus, bStem2,
		\masterVol, 0, \masterDry, 0, \fadeIn, 1]]);
	score.add([dur, [\c_set, 0, 0]]);
	score
};

var render = { |score, outPath, renderDur, renderOpts|
	var done = false, waited = 0, maxWait = max(120, (renderDur ? dur) * 2);
	score.recordNRT(nil, outPath, nil, sr, "WAV", "float", renderOpts ? opts,
		duration: (renderDur ? dur), action: { done = true });
	while { done.not and: { waited < maxWait } } { 0.2.wait; waited = waited + 0.2 };
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

// upward zero crossings of channel 0 over [startFrame, endFrame) — a cheap,
// deterministic pitch-activity proxy for the matrix-FM regression case
var countCrossings = { |path, half|
	var sf = SoundFile.openRead(path);
	var data, numCh, start, end, count = 0, prev = 0.0, i, v;
	if(sf.isNil) { Error("CQ NRT: cannot open render: " ++ path).throw };
	data = FloatArray.newClear(sf.numFrames * sf.numChannels);
	sf.readData(data);
	sf.close;
	numCh = sf.numChannels;
	start = if(half == 0) { 0 } { sf.numFrames div: 2 };
	end = if(half == 0) { sf.numFrames div: 2 } { sf.numFrames };
	i = start;
	while { i < end } {
		v = data[i * numCh];
		if((v > 0) and: { prev <= 0 }) { count = count + 1 };
		prev = v;
		i = i + 1;
	};
	count
};

// max abs over one channel of a multichannel render — per-stem peak (phase 7)
var chanPeak = { |path, ch|
	var sf = SoundFile.openRead(path);
	var data, numCh, peak = 0.0, i;
	if(sf.isNil) { Error("CQ NRT: cannot open render: " ++ path).throw };
	data = FloatArray.newClear(sf.numFrames * sf.numChannels);
	sf.readData(data);
	sf.close;
	numCh = sf.numChannels;
	i = ch;
	while { i < data.size } { peak = max(peak, data[i].abs); i = i + numCh };
	peak
};

opts.numOutputBusChannels = 2;
opts.numInputBusChannels = 2;
opts.blockSize = cfg[\blockSize];
opts4.numOutputBusChannels = 4;        // phase 7 stem render: mix (0,1) + stems (2,3)
opts4.numInputBusChannels = 2;
opts4.blockSize = cfg[\blockSize];

File.mkdir(tmpDir);

{
	var r1, r2, r3, r4, r5, r6, rStem, xUp, xDown, stem1Pk, stem2Pk;
	var ceiling = (-0.75).dbamp;
	var p1 = tmpDir +/+ "nrt_safety_torture.wav";
	var p2 = tmpDir +/+ "nrt_safety_nan.wav";
	var p3 = tmpDir +/+ "nrt_safety_coco.wav";
	var p4 = tmpDir +/+ "nrt_safety_quantussy.wav";
	var p5 = tmpDir +/+ "nrt_safety_mayhem.wav";
	var p6 = tmpDir +/+ "nrt_safety_matrixfm.wav";
	var pStem = tmpDir +/+ "nrt_safety_stem.wav";

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

		"CQ NRT: rendering coco worst case (% s)...".format(dur).postln;
		render.value(makeCocoScore.value, p3);
		r3 = analyze.value(p3);
		"coco: peak %  dcL %  dcR %  frames %".format(
			r3[\peak].round(0.0001), r3[\dcL].round(0.00001),
			r3[\dcR].round(0.00001), r3[\frames]).postln;
		check.value(r3[\frames] > 0, "coco render produced audio");
		check.value(r3[\bad].not, "coco render has no NaN/inf");
		check.value(r3[\peak] <= ceiling,
			"coco peak % <= -0.75 dBFS".format(r3[\peak].round(0.0001)));
		check.value(r3[\peak] >= 0.25,
			"coco pair carried signal (peak % >= 0.25, not failed-to-silence)"
				.format(r3[\peak].round(0.0001)));
		check.value(r3[\dcL].abs <= 0.02, "coco |DC left| <= 0.02");
		check.value(r3[\dcR].abs <= 0.02, "coco |DC right| <= 0.02");

		"CQ NRT: rendering quantussy worst case (% s)...".format(dur).postln;
		render.value(makeQuantussyScore.value, p4);
		r4 = analyze.value(p4);
		"quantussy: peak %  dcL %  dcR %  frames %".format(
			r4[\peak].round(0.0001), r4[\dcL].round(0.00001),
			r4[\dcR].round(0.00001), r4[\frames]).postln;
		check.value(r4[\frames] > 0, "quantussy render produced audio");
		check.value(r4[\bad].not, "quantussy render has no NaN/inf");
		check.value(r4[\peak] <= ceiling,
			"quantussy peak % <= -0.75 dBFS".format(r4[\peak].round(0.0001)));
		check.value(r4[\peak] >= 0.25,
			"qMix carried signal (peak % >= 0.25, not failed-to-silence)"
				.format(r4[\peak].round(0.0001)));
		check.value(r4[\dcL].abs <= 0.02, "quantussy |DC left| <= 0.02");
		check.value(r4[\dcR].abs <= 0.02, "quantussy |DC right| <= 0.02");

		"CQ NRT: rendering \\mayhem full-matrix worst case (% s)...".format(mayhemDur).postln;
		render.value(makeMayhemScore.value, p5, mayhemDur);
		r5 = analyze.value(p5);
		"mayhem: peak %  dcL %  dcR %  frames %".format(
			r5[\peak].round(0.0001), r5[\dcL].round(0.00001),
			r5[\dcR].round(0.00001), r5[\frames]).postln;
		check.value(r5[\frames] > 0, "mayhem render produced audio");
		check.value(r5[\bad].not, "mayhem render has no NaN/inf");
		check.value(r5[\peak] <= ceiling,
			"mayhem peak % <= -0.75 dBFS".format(r5[\peak].round(0.0001)));
		check.value(r5[\peak] >= 0.25,
			"mayhem carried signal (peak % >= 0.25, not failed-to-silence)"
				.format(r5[\peak].round(0.0001)));
		check.value(r5[\dcL].abs <= 0.02, "mayhem |DC left| <= 0.02");
		check.value(r5[\dcR].abs <= 0.02, "mayhem |DC right| <= 0.02");

		"CQ NRT: rendering matrix castle-FM routing check (% s)...".format(dur).postln;
		render.value(makeMatrixFmScore.value, p6);
		r6 = analyze.value(p6);
		xUp = countCrossings.value(p6, 0);      // tri1 > 0: castle 5 swept upward
		xDown = countCrossings.value(p6, 1);    // tri1 < 0: swept downward
		"matrixfm: peak %  crossings up-half %  down-half %".format(
			r6[\peak].round(0.0001), xUp, xDown).postln;
		check.value(r6[\peak] >= 0.05, "matrix-FM render is not silent");
		check.value(r6[\bad].not, "matrix-FM render has no NaN/inf");
		check.value(xUp > (xDown * 1.3),
			"matrix castle-FM routing is alive: up-half crossings % > 1.3 x down-half %"
				.format(xUp, xDown));

		"CQ NRT: rendering stem outputs (4-ch, % s)...".format(dur).postln;
		render.value(makeStemScore.value, pStem, dur, opts4);
		rStem = analyze.value(pStem);
		stem1Pk = chanPeak.value(pStem, 2);
		stem2Pk = chanPeak.value(pStem, 3);
		"stem: peak %  coco1(ch2) %  coco2(ch3) %  frames %".format(
			rStem[\peak].round(0.0001), stem1Pk.round(0.0001),
			stem2Pk.round(0.0001), rStem[\frames]).postln;
		check.value(rStem[\frames] > 0, "stem render produced audio");
		check.value(rStem[\bad].not, "stem render has no NaN/inf");
		check.value(rStem[\peak] <= ceiling,
			"stem: all channels peak % <= -0.75 dBFS".format(rStem[\peak].round(0.0001)));
		check.value(stem1Pk <= ceiling,
			"coco1 stem (ch2) % <= -0.75 dBFS".format(stem1Pk.round(0.0001)));
		check.value(stem2Pk <= ceiling,
			"coco2 stem (ch3) % <= -0.75 dBFS".format(stem2Pk.round(0.0001)));
		check.value(stem1Pk >= 0.05,
			"coco1 stem carries signal (identical safety chain on stems, #1)");
		check.value(stem2Pk >= 0.05,
			"coco2 stem carries signal (identical safety chain on stems, #1)");

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
