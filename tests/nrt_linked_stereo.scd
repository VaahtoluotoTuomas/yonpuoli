// Offline linked-stereo identity/start test (roadmap 3.1, CQ-AUD-008).
// The REAL \cqDeck SynthDef receives two mono impulse buffers. The left impulse
// occurs earlier than the right, so dual-mono routing is immediately observable;
// equal corrected offsets prove both players began on the same score timetag.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var tmpDir = repo +/+ "tests" +/+ "tmp";
var outPath = tmpDir +/+ "cq_nrt_linked_stereo.wav";
var cfg = CQConfig.validate(());
var deckDef = CQSynthDefs.deck(cfg);
var score = Score.new;
var opts = ServerOptions.new;
var sr = 48000, frames = 8192, duration = 0.25;
var leftAt = 2400, rightAt = 4800;
var left = FloatArray.newClear(frames), right = FloatArray.newClear(frames);
var done = false, waited = 0, ok = true;
var sf, data, channels, firstLeft, firstRight, peakLeft = 0.0, peakRight = 0.0;
var i, leftOffset, rightOffset;
var check = { |condition, message|
	if(condition) { ("ok:   " ++ message).postln } {
		ok = false;
		("FAIL: " ++ message).postln;
	};
};

if(File.exists(tmpDir).not) { File.mkdir(tmpDir) };
if(File.exists(outPath)) { File.delete(outPath) };
left[leftAt] = 0.5;
right[rightAt] = -0.5;
opts.numOutputBusChannels = 2;
opts.numInputBusChannels = 0;

score.add([0.0, [\d_recv, deckDef.asBytes]]);
score.add([0.0, [\b_alloc, 0, frames, 1]]);
score.add([0.0, [\b_setn, 0, 0, frames] ++ left]);
score.add([0.0, [\b_alloc, 1, frames, 1]]);
score.add([0.0, [\b_setn, 1, 0, frames] ++ right]);
score.add([0.0, [\g_new, 1000, 0, 0]]);
// Both /s_new messages occupy the same score timestamp, exactly as one
// Server.bind bundle from CQDeckPair does in the live graph.
score.add([0.0, [\s_new, \cqDeck, 1001, 1, 1000,
	\buf, 0, \out, 2, \dryBus, 0, \modSrcBus, 4, \wRateBus, 0,
	\rate, 1, \loop, 0, \level, 1, \patchDepth, 0, \dryChannel, 0]]);
score.add([0.0, [\s_new, \cqDeck, 1002, 1, 1000,
	\buf, 1, \out, 3, \dryBus, 0, \modSrcBus, 4, \wRateBus, 16,
	\rate, 1, \loop, 0, \level, 1, \patchDepth, 0, \dryChannel, 1]]);
score.add([duration, [\c_set, 0, 0]]);

{
	try {
		score.recordNRT(nil, outPath, nil, sr, "WAV", "float", opts,
			duration: duration, action: { done = true });
		while { done.not and: { waited < 30 } } { 0.1.wait; waited = waited + 0.1 };
		if(done.not) { Error("linked-stereo NRT render timed out").throw };
		sf = SoundFile.openRead(outPath);
		if(sf.isNil) { Error("cannot open linked-stereo render").throw };
		channels = sf.numChannels;
		data = FloatArray.newClear(sf.numFrames * channels);
		sf.readData(data);
		sf.close;
		i = 0;
		while { i < sf.numFrames } {
			if(data[i * channels].abs > 1e-6) {
				firstLeft = firstLeft ?? { i };
				peakLeft = max(peakLeft, data[i * channels].abs);
			};
			if(data[(i * channels) + 1].abs > 1e-6) {
				firstRight = firstRight ?? { i };
				peakRight = max(peakRight, data[(i * channels) + 1].abs);
			};
			i = i + 1;
		};
		check.value(channels == 2, "render is stereo");
		check.value(firstLeft.notNil and: { firstRight.notNil },
			"both linked channels carry their own impulse");
		if(firstLeft.notNil and: { firstRight.notNil }) {
			leftOffset = firstLeft - leftAt;
			rightOffset = firstRight - rightAt;
			check.value(firstLeft < firstRight,
				"dry L contains the early L impulse; dry R does not (not dual mono)");
			check.value(leftOffset == rightOffset,
				"linked starts are sample-aligned (corrected offsets % / %)"
					.format(leftOffset, rightOffset));
		};
		check.value(peakLeft > 0.01 and: { peakRight > 0.01 },
			"both dry channels remain audible");
	} { |error|
		ok = false;
		("FAIL: uncaught linked-stereo NRT error: " ++ error.errorString).postln;
	};
	if(File.exists(outPath)) { File.delete(outPath) };
	if(ok) { "CQ_LINKED_STEREO_PASSED".postln; 0.exit } {
		"CQ_LINKED_STEREO_FAILED".postln; 1.exit
	};
}.fork(AppClock);
)
