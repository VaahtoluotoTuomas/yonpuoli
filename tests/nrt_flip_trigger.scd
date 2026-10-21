// Production cqCoco regression: rising edges toggle once, falling edges retain
// direction, and manual reversal composes with trigger history on both Cocos.
// Offline only; invoke through run_sclang_gate.ps1.
(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var path = repo +/+ "tests/tmp/nrt_flip_trigger.wav";
var cfg = CQConfig.validate(()), sr = 48000, dur = 4;
var opts = ServerOptions.new.numOutputBusChannels_(2).numInputBusChannels_(0);
var defs = CQSynthDefs.all(cfg), score = Score.new, ok = true;
var check = { |value, message|
	if(value) { ("ok: " ++ message).postln } { ok = false; ("FAIL: " ++ message).postln };
};
var feed = SynthDef(\cqTestFlipGate, {
	var signal = K2A.ar(\gate.kr(0));
	Out.ar(32, [signal] ++ (0 ! 15)); Out.ar(64, [signal] ++ (0 ! 15));
});
var ramp = Array.fill(1024, { |i| 0.1 + (i / 1024 * 0.1) });
defs.do { |def| score.add([0, [\d_recv, def.asBytes]]) };
score.add([0, [\d_recv, feed.asBytes]]);
score.add([0, [\c_setn, 0, 16] ++ (0 ! 16)]);
score.add([0, [\c_setn, 16, 16, 1] ++ (0 ! 15)]);
2.do { |i|
	score.add([0, [\b_alloc, i, 1024, 1]]);
	score.add([0.02, [\b_setn, i, 0, 1024] ++ ramp]);
};
score.add([0.04, [\g_new, 1000, 0, 0]]);
score.add([0.05, [\s_new, \cqTestFlipGate, 1001, 1, 1000]]);
2.do { |i|
	score.add([0.06, [\s_new, \cqCoco, 1010 + i, 1, 1000,
		\buf, i, \outBus, i, \modSrcBus, [32, 64][i], \cocoIndex, i,
		\modTapIdx, 12 + i, \modOrganIdx, 14 + i,
		\preBus, 128, \deckBus, 128, \qMixBus, 128, \crossBus, 128,
		\wSpafBus, 0, \wInVcaBus, 0, \wFbVcaBus, 0, \wFlipBus, 16,
		\wSkipBus, 0, \wOrganBus, 0, \wSlotQBus, 0, \wSlotXBus, 0,
		\wDustBus, 0, \wRecordBus, 0, \patchDepth, 1,
		\hold, 1, \flip, i, \speed, sr, \dust, 0, \organLvl, 0, \level, 1]]);
};
[[0.5, 1], [1, 0], [1.5, 1], [2, 0], [3, 1], [3.5, 0]].do { |edge|
	score.add([edge[0], [\n_set, 1001, \gate, edge[1]]]);
};
2.do { |i| score.add([2.5, [\n_set, 1010 + i, \flip, 1 - i]]) };
score.add([dur, [\c_set, 100, 0]]);
{
	var done = false, waited = 0, sound, data;
	var windows = [0.2, 0.7, 1.2, 1.7, 2.2, 2.7, 3.2, 3.7];
	var signs = [1, -1, -1, 1, 1, -1, 1, 1];
	try {
		score.recordNRT(nil, path, nil, sr, "WAV", "float", opts,
			duration: dur, action: { done = true });
		while { done.not and: { waited < 90 } } { 0.2.wait; waited = waited + 0.2 };
		if(done.not) { Error("flip-trigger render timed out").throw };
		sound = SoundFile.openRead(path);
		if(sound.isNil) { Error("flip-trigger render missing").throw };
		check.value(sound.numChannels == 2 and: { sound.numFrames >= (dur * sr) }, "complete two-Coco render");
		data = FloatArray.newClear(sound.numFrames * 2); sound.readData(data); sound.close;
		check.value(data.every { |v| v.isNaN.not and: { v.abs < 1 } }, "finite bounded output");
		windows.do { |start, n| 2.do { |i|
			var positive = 0, negative = 0, expected = signs[n] * if(i == 0) { 1 } { -1 };
			var first = (start * sr).asInteger, last = ((start + 0.1) * sr).asInteger;
			(first .. last).do { |frame|
				var delta = data[(frame * 2) + i] - data[((frame - 1) * 2) + i];
				// Exclude the sawtooth's wrap; the remaining slope reveals read direction.
				if(delta.abs < 0.001 and: { delta.abs > 0.000001 }) {
					if(delta > 0) { positive = positive + 1 } { negative = negative + 1 };
				};
			};
			check.value((positive + negative) > 4000 and: {
				if(expected > 0) { positive > (negative * 100) } { negative > (positive * 100) }
			}, "Coco " ++ (i + 1) ++ " direction at " ++ start ++ " s = " ++ expected);
		} };
	} { |error| ok = false; error.reportError };
	if(ok) { "CQ_FLIP_TRIGGER_PASSED".postln; 0.exit } { "CQ_FLIP_TRIGGER_FAILED".postln; 1.exit };
}.fork(AppClock);
)
