// Offline liveness/safety comparison for all four disposable Space Lab
// candidates. A private lab monitor hears only the prototype return, so a
// silent candidate cannot hide behind the dry Cocos. The monitor mirrors the
// production safety chain but does not restore a production effect input.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var tmpDir = repo +/+ "tests" +/+ "tmp";
var defsPath = repo +/+ "prototypes" +/+ "space_lab_defs.scd";
var protoDefs = thisProcess.interpreter.executeFile(defsPath);
var cfg = CQConfig.validate(());
var sr = 48000, dur = 6.0, frames = 8192;
var opts = ServerOptions.new;
var variants = #[
	cqProtoSpaceUtility,
	cqProtoSpaceClockField,
	cqProtoSpaceMemoryHalo,
	cqProtoSpaceConfluence
];
var results = List.new, signatures = IdentityDictionary.new, ok = true;
var check, sourceDef, referenceDef, monitorDef, makeScore, render, analyze;
// NRT hardware buses 0/1; private audio buses begin at 4.
var bRaw1 = 4, bRaw2 = 5, bReturn = 8, bIn = 10, bMod = 16;

check = { |bool, msg|
	if(bool) {
		results.add("ok:   " ++ msg)
	} {
		ok = false;
		results.add("FAIL: " ++ msg)
	};
};

sourceDef = SynthDef(\cqProtoSpaceTestSource, {
	var hit1, hit2, coco1, coco2, organ1, organ2, mods;
	hit1 = Decay2.ar(Impulse.ar(1.7), 0.001, 0.09);
	hit2 = Decay2.ar(Impulse.ar(1.13, 0.37), 0.001, 0.14);
	coco1 = ((PinkNoise.ar(0.42) * hit1)
		+ (SinOsc.ar(173) * 0.16) + (Pulse.ar(61, 0.37) * 0.08)).tanh;
	coco2 = ((BrownNoise.ar(0.3) * hit2)
		+ (SinOsc.ar(257) * 0.14) + (LFTri.ar(97) * 0.09)).tanh;
	organ1 = Mix(Pulse.ar(24000 / [128, 256, 512], 0.5, [0.5, 0.3, 0.2]));
	organ2 = Mix(Pulse.ar(17300 / [128, 256, 512], 0.5, [0.5, 0.3, 0.2]));
	mods = [
		SinOsc.ar(0.17), SinOsc.ar(0.23), LFTri.ar(0.31),
		LFTri.ar(0.41), SinOsc.ar(0.53),
		Latch.ar(WhiteNoise.ar, Impulse.ar(2.1)),
		Latch.ar(WhiteNoise.ar, Impulse.ar(1.7)),
		Latch.ar(WhiteNoise.ar, Impulse.ar(1.3)),
		Latch.ar(WhiteNoise.ar, Impulse.ar(0.9)),
		Latch.ar(WhiteNoise.ar, Impulse.ar(0.7)),
		Amplitude.ar(coco1), Amplitude.ar(coco2),
		coco1, coco2, organ1, organ2
	];
	Out.ar(\raw1.ir(bRaw1), coco1);
	Out.ar(\raw2.ir(bRaw2), coco2);
	Out.ar(\spaceIn.ir(bIn), [coco1, coco2] * 0.72);
	Out.ar(\modBus.ir(bMod), mods);
});

// Unity return reference for gain calibration. A candidate at full wet should
// live in the same broad loudness region as this signal, so the performance
// knob has an actually useful 0..1 range beside the dry Cocos.
referenceDef = SynthDef(\cqProtoSpaceTestReference, {
	Out.ar(\outBus.ir(bReturn), In.ar(\inBus.ir(bIn), 2));
});

monitorDef = SynthDef(\cqProtoSpaceTestMonitor, {
	var signal = LeakDC.ar(In.ar(\inBus.ir(bReturn), 2));
	signal = (signal * 0.5).tanh;
	signal = Limiter.ar(signal, 0.89, 0.01);
	signal = signal.collect { |channel|
		Select.ar(CheckBadValues.ar(channel, post: 0) > 0,
			[channel, DC.ar(0)])
	};
	Out.ar(0, signal);
});

makeScore = { |variant|
	var score = Score.new, proto;
	proto = if(variant == \cqProtoSpaceTestReference) {
		referenceDef
	} {
		protoDefs.detect { |def| def.name == variant }
	};
	score.add([0.0, [\d_recv, sourceDef.asBytes]]);
	score.add([0.0, [\d_recv, proto.asBytes]]);
	score.add([0.0, [\d_recv, monitorDef.asBytes]]);
	score.add([0.0, [\b_alloc, 0, frames, 1]]);
	score.add([0.0, [\b_alloc, 1, frames, 1]]);
	score.add([0.0, [\b_gen, 0, \sine1, 7, 1, 0.42, 0.21, 0.11, 0.05]]);
	score.add([0.0, [\b_gen, 1, \sine1, 7, 1, 0.31, 0.17, 0.09, 0.04]]);
	score.add([0.0, [\g_new, 1000, 0, 0]]);
	score.add([0.0, [\s_new, \cqProtoSpaceTestSource, 1001, 1, 1000,
		\raw1, bRaw1, \raw2, bRaw2, \spaceIn, bIn, \modBus, bMod]]);
	score.add([0.0, [\s_new, variant, 1002, 1, 1000,
		\inBus, bIn, \outBus, bReturn, \modSrcBus, bMod,
		\coco1Bus, bRaw1, \coco2Bus, bRaw2,
		\buf1, 0, \buf2, 1, \speed1, 24000, \speed2, 17300,
		\feed1, 0.6, \feed2, 0.6, \reach, 0.66, \relation, 0.58,
		\motion, 0.38, \tone, 0.48, \level, 0.72]]);
	score.add([0.0, [\s_new, \cqProtoSpaceTestMonitor, 1003, 1, 1000,
		\inBus, bReturn]]);
	score.add([dur, [\c_set, 0, 0]]);
	score
};

render = { |score, path|
	var done = false, waited = 0;
	score.recordNRT(nil, path, nil, sr, "WAV", "float", opts,
		duration: dur, action: { done = true });
	while { done.not and: { waited < 120 } } {
		0.2.wait;
		waited = waited + 0.2;
	};
	if(done.not) { Error("Space Lab NRT timed out: " ++ path).throw };
};

analyze = { |path|
	var sf = SoundFile.openRead(path);
	var data, total, peak, squareSum = 0.0, sumL = 0.0, sumR = 0.0;
	var crossings = 0, previous = 0.0, i = 0, value, n;
	if(sf.isNil) { Error("Space Lab NRT cannot open: " ++ path).throw };
	data = FloatArray.newClear(sf.numFrames * sf.numChannels);
	sf.readData(data);
	sf.close;
	n = max(1, sf.numFrames);
	total = data.sum;
	peak = data.abs.maxItem ? 0;
	while { i < data.size } {
		value = data[i];
		sumL = sumL + value;
		sumR = sumR + data[i + 1];
		squareSum = squareSum + value.squared + data[i + 1].squared;
		if((value > 0) and: { previous <= 0 }) { crossings = crossings + 1 };
		previous = value;
		i = i + 2;
	};
	(
		peak: peak,
		rms: (squareSum / (n * 2)).sqrt,
		dcL: sumL / n,
		dcR: sumR / n,
		crossings: crossings,
		bad: total.isNaN or: { total.abs == inf },
		frames: sf.numFrames
	)
};

opts.numOutputBusChannels = 2;
opts.numInputBusChannels = 2;
opts.blockSize = cfg[\blockSize];
File.mkdir(tmpDir);

{
	var reference;
	try {
		var referencePath;
		referencePath = tmpDir +/+ "prototype_space_unity_reference.wav";
		"Space Lab NRT: rendering unity return reference...".postln;
		render.value(makeScore.value(\cqProtoSpaceTestReference), referencePath);
		reference = analyze.value(referencePath);
		"unity reference: peak % rms %".format(
			reference[\peak].round(0.0001),
			reference[\rms].round(0.00001)
		).postln;
		check.value(reference[\bad].not and: { reference[\rms] > 0.001 },
			"unity return reference is valid");

		variants.do { |variant|
			var path, result, relativeDb;
			path = tmpDir +/+ ("prototype_space_" ++ variant.asString ++ ".wav");
			"Space Lab NRT: rendering %...".format(variant).postln;
			render.value(makeScore.value(variant), path);
			result = analyze.value(path);
			signatures[variant] = result;
			relativeDb = (result[\rms] / reference[\rms]).max(1e-9).ampdb;
			"%: peak % rms % (% dB vs unity) dc %/% crossings %".format(
				variant,
				result[\peak].round(0.0001),
				result[\rms].round(0.00001),
				relativeDb.round(0.1),
				result[\dcL].round(0.00001),
				result[\dcR].round(0.00001),
				result[\crossings]
			).postln;
			check.value(result[\frames] > 0,
				"% produced frames".format(variant));
			check.value(result[\bad].not,
				"% has no NaN/inf".format(variant));
			check.value(result[\peak] <= (-0.75).dbamp,
				"% stays below the master ceiling".format(variant));
			check.value(result[\peak] >= 0.005,
				"% return is not silent".format(variant));
			check.value(result[\rms] >= 0.0005,
				"% carries sustained energy".format(variant));
			check.value(relativeDb >= -7 and: { relativeDb <= 4 },
				"% wet return is -7..+4 dB from unity (% dB)"
					.format(variant, relativeDb.round(0.1)));
			check.value(result[\dcL].abs <= 0.02
				and: { result[\dcR].abs <= 0.02 },
				"% has bounded DC".format(variant));
		};
		check.value(
			variants.collect { |variant| signatures[variant][\crossings] }.asSet.size > 1,
			"the four renders do not collapse to one crossing signature"
		);
	} { |err|
		ok = false;
		results.add("ERROR: " ++ err.errorString);
	};

	results.do(_.postln);
	if(ok) {
		"CQ_SPACE_LAB_NRT_PASSED".postln;
		0.exit;
	} {
		"CQ_SPACE_LAB_NRT_FAILED".postln;
		1.exit;
	};
}.fork;
)
