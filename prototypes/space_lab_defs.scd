// Yölahti Space Lab — four disposable listening SynthDefs.
//
// This file returns an Array of SynthDefs. It allocates nothing, starts no node,
// and never writes to hardware outputs. space_lab.scd owns the listening
// lifecycle and routes one selected prototype through Yölahti's existing
// aSpaceRet -> cqMaster safety/recording path.
//
// The four candidates deliberately answer different questions:
//   utility     — what if Space is simply a good, dark studio reverb?
//   clockField  — can the actual Coco clock relation become a room?
//   memoryHalo  — can read-only heads in the two Coco memories imply space?
//   confluence  — what new voice appears when each Coco samples the other?

[
	SynthDef(\cqProtoSpaceUtility, {
		var input, reach, tone, scale, cutoff, feedback;
		var prediff, previous, cleanPrevious, damped, reflected, injection;
		var times, delays, wet, bad;

		input = LeakDC.ar(In.ar(\inBus.ir(0), 2));
		reach = \reach.kr(0.68, 0.12).clip(0, 1);
		tone = \tone.kr(0.46, 0.12).clip(0, 1);
		scale = reach.linlin(0, 1, 0.72, 1.28);
		cutoff = tone.linexp(0, 1, 1800, 10500);
		feedback = reach.linlin(0, 1, 0.68, 0.965);

		// Two input diffusers erase the obvious left/right injection without
		// introducing a perceptible predelay.
		prediff = AllpassC.ar(input, 0.05, [0.0117, 0.0131], 0.34);
		prediff = AllpassC.ar(prediff, 0.05, [0.0179, 0.0193], 0.41);

		previous = LocalIn.ar(8);
		// A bad-value transient may never poison the recursive network for the
		// rest of a session (the production FDN's known CQ-AUD-006 weakness).
		cleanPrevious = previous.collect { |sig|
			Select.ar(CheckBadValues.ar(sig, post: 0) > 0, [sig, DC.ar(0)])
		};
		damped = LPF.ar(cleanPrevious, cutoff);
		// Eight-line Householder reflection: orthogonal before the explicit
		// feedback scalar, therefore bounded while preserving a dense tail.
		reflected = damped - (damped.sum * 0.25);
		injection = [
			prediff[0], prediff[1], prediff[0].neg, prediff[1].neg,
			prediff[1], prediff[0].neg, prediff[1].neg, prediff[0]
		] * 0.13;
		times = [0.0371, 0.0411, 0.0473, 0.0531, 0.0617, 0.0713, 0.0839, 0.0971]
			* scale;
		delays = 8.collect { |i|
			DelayC.ar((reflected[i] * feedback) + injection[i], 0.14, times[i])
		};
		LocalOut.ar(delays);

		wet = [
			delays[0] + delays[2] + delays[5] + delays[7],
			delays[1] + delays[3] + delays[4] + delays[6]
		] * 0.38;
		wet = LeakDC.ar(LPF.ar(wet, cutoff * 1.25));
		bad = CheckBadValues.ar(wet, post: 0);
		wet = wet.collect { |sig, i|
			Select.ar(bad[i] > 0, [sig, DC.ar(0)])
		};
		// Calibrated against a unity return through cqMaster. The recursive
		// injection above is deliberately quiet for stability; make that loss
		// up only after the feedback network.
		Out.ar(\outBus.ir(0), wet * \level.kr(0.72, 0.08) * 16.0);
	}),

	SynthDef(\cqProtoSpaceClockField, {
		var input, mods, organs, clockTrace, period, reach, motion, tone;
		var previous, cleanPrevious, weather, damped, reflected, injection;
		var feedback, cutoff, times, delays, wet, bad;

		input = LeakDC.ar(In.ar(\inBus.ir(0), 2));
		mods = In.ar(\modSrcBus.ir(0), 16);
		// The organ taps are published even when their audible organ levels are
		// zero. ZeroCrossing therefore follows the two real, post-SP.AF Coco
		// address clocks without adding a production telemetry bus.
		organs = [mods[14], mods[15]];
		clockTrace = organs.collect { |sig|
			Lag.ar(ZeroCrossing.ar(sig).clip(1.5, 1200), 0.38)
		};
		period = clockTrace.collect { |hz| hz.reciprocal.clip(0.0025, 0.24) };
		reach = \reach.kr(0.62, 0.15).clip(0, 1);
		motion = \motion.kr(0.38, 0.15).clip(0, 1);
		tone = \tone.kr(0.42, 0.12).clip(0, 1);
		feedback = reach.linlin(0, 1, 0.58, 0.945);
		cutoff = tone.linexp(0, 1, 1400, 9000);

		previous = LocalIn.ar(6);
		cleanPrevious = previous.collect { |sig|
			Select.ar(CheckBadValues.ar(sig, post: 0) > 0, [sig, DC.ar(0)])
		};
		weather = [
			Lag.ar(mods[0], 0.18), Lag.ar(mods[2], 0.23),
			Lag.ar(mods[4], 0.31), Lag.ar(mods[1], 0.27),
			Lag.ar(mods[3], 0.37), Lag.ar(mods[5], 0.21)
		] * motion;
		damped = LPF.ar(cleanPrevious, cutoff);
		reflected = damped - (damped.sum / 3);
		injection = [
			input[0], input[1], input.sum * 0.5,
			input[0].neg, input[1].neg, (input[0] - input[1]) * 0.5
		] * 0.16;
		// Each delay is a bounded irrational-looking view of one clock period or
		// their sum. A small, smoothed Quantussy weather term bends the field;
		// it never gets enough depth to turn every patch into a pitch shifter.
		times = [
			period[0] * 0.73,
			period[1] * 0.89,
			period[0] * 1.31,
			period[1] * 1.47,
			(period[0] + period[1]) * 0.61,
			(period[0] + period[1]) * 0.97
		].collect { |time, i|
			(time * reach.linlin(0, 1, 0.68, 1.42)
				* (1 + (weather[i] * 0.035))).clip(0.002, 0.34)
		};
		delays = 6.collect { |i|
			DelayC.ar((reflected[i] * feedback) + injection[i], 0.36, times[i])
		};
		LocalOut.ar(delays);

		wet = [
			delays[0] + delays[2] + delays[5],
			delays[1] + delays[3] + delays[4]
		] * 0.15;
		wet = LeakDC.ar(LPF.ar(wet, cutoff * 1.2));
		bad = CheckBadValues.ar(wet, post: 0);
		wet = wet.collect { |sig, i|
			Select.ar(bad[i] > 0, [sig, DC.ar(0)])
		};
		Out.ar(\outBus.ir(0), wet * \level.kr(0.68, 0.08) * 13.0);
	}),

	SynthDef(\cqProtoSpaceMemoryHalo, {
		var buf1, buf2, frames1, frames2, speed1, speed2, phase1, phase2;
		var reach, motion, tone, offsets, pans1, pans2, levels;
		var taps1, taps2, wet1, wet2, wet, cutoff, bad;

		buf1 = \buf1.ir(0);
		buf2 = \buf2.ir(1);
		frames1 = BufFrames.ir(buf1);
		frames2 = BufFrames.ir(buf2);
		speed1 = \speed1.kr(24000, 0.12).clip(1500, SampleRate.ir);
		speed2 = \speed2.kr(24000, 0.12).clip(1500, SampleRate.ir);
		phase1 = Phasor.ar(0, speed1 / SampleRate.ir, 0, frames1, 0);
		phase2 = Phasor.ar(0, speed2 / SampleRate.ir, 0, frames2, 0);
		reach = \reach.kr(0.58, 0.18).clip(0, 1);
		motion = \motion.kr(0.24, 0.18).clip(0, 1);
		tone = \tone.kr(0.48, 0.12).clip(0, 1);
		offsets = [0.071, 0.193, 0.337, 0.563]
			* reach.linlin(0, 1, 0.24, 1);
		pans1 = [-0.92, -0.34, 0.18, 0.76];
		pans2 = [0.92, 0.34, -0.18, -0.76];
		levels = [0.46, 0.34, 0.26, 0.19];

		// These heads only read: hold integrity and the two production write
		// loops remain untouched. Tiny independent drifts stop the four copies
		// from collapsing into a static comb while preserving the Coco speed.
		taps1 = 4.collect { |i|
			var drift, position;
			drift = SinOsc.ar([0.019, 0.027, 0.037, 0.053][i], i * 0.7,
				frames1 * 0.00045 * motion);
			position = (phase1 + (frames1 * offsets[i]) + drift)
				.wrap(0, frames1 - 2);
			BufRd.ar(1, buf1, position, loop: 1, interpolation: 4)
		};
		taps2 = 4.collect { |i|
			var drift, position;
			drift = SinOsc.ar([0.023, 0.031, 0.043, 0.059][i], i * 0.9,
				frames2 * 0.00045 * motion);
			position = (phase2 + (frames2 * offsets.wrapAt(i + 1)) + drift)
				.wrap(0, frames2 - 2);
			BufRd.ar(1, buf2, position, loop: 1, interpolation: 4)
		};
		wet1 = Mix(taps1.collect { |sig, i|
			Pan2.ar(sig, pans1[i], levels[i])
		}) * \feed1.kr(0.56, 0.08);
		wet2 = Mix(taps2.collect { |sig, i|
			Pan2.ar(sig, pans2[i], levels[i])
		}) * \feed2.kr(0.56, 0.08);
		cutoff = tone.linexp(0, 1, 1700, 12000);
		wet = LeakDC.ar(HPF.ar(LPF.ar((wet1 + wet2) * 0.62, cutoff), 35));
		bad = CheckBadValues.ar(wet, post: 0);
		wet = wet.collect { |sig, i|
			Select.ar(bad[i] > 0, [sig, DC.ar(0)])
		};
		Out.ar(\outBus.ir(0), wet * \level.kr(0.72, 0.08) * 12.5);
	}),

	SynthDef(\cqProtoSpaceConfluence, {
		var coco1, coco2, mods, organ1, organ2, trig1, trig2;
		var held1, held2, relation, motion, tone;
		var linear, braid, intermod, weightLinear, weightBraid, weightIntermod;
		var weather, wet, cutoff, bad;

		coco1 = LeakDC.ar(In.ar(\coco1Bus.ir(0)))
			* \feed1.kr(0.56, 0.08);
		coco2 = LeakDC.ar(In.ar(\coco2Bus.ir(0)))
			* \feed2.kr(0.56, 0.08);
		mods = In.ar(\modSrcBus.ir(0), 16);
		organ1 = mods[14];
		organ2 = mods[15];
		trig1 = HPZ1.ar(organ1 > 0).max(0);
		trig2 = HPZ1.ar(organ2 > 0).max(0);
		// Reciprocal resampling: Coco 1 is latched by Coco 2's address-derived
		// organ and vice versa. Their clock ratio, not an added oscillator,
		// determines the new sidebands and rhythmic locking.
		held1 = Latch.ar(coco1, trig2);
		held2 = Latch.ar(coco2, trig1);
		relation = \relation.kr(0.56, 0.15).clip(0, 1);
		motion = \motion.kr(0.32, 0.15).clip(0, 1);
		tone = \tone.kr(0.5, 0.12).clip(0, 1);

		// One macro moves through three views of the same pair:
		// sum/difference -> reciprocal sample-and-hold -> multiplication.
		linear = [(coco1 + coco2) * 0.5, (coco1 - coco2) * 0.5];
		braid = [
			held1 - (held2 * 0.36),
			held2 - (held1 * 0.36)
		];
		intermod = [
			(coco1 * held2 * 2.2) + (coco1 * coco2 * 0.7),
			(coco2 * held1 * 2.2) - (coco1 * coco2 * 0.7)
		].tanh;
		weightLinear = (1 - (relation * 2)).clip(0, 1);
		weightIntermod = ((relation - 0.5) * 2).clip(0, 1);
		weightBraid = 1 - weightLinear - weightIntermod;
		wet = (linear * weightLinear) + (braid * weightBraid)
			+ (intermod * weightIntermod);

		// The sampled castle relation slowly tilts the stereo shoreline. It
		// changes perspective, not the dry Coco state or its memory.
		weather = Lag.ar((mods[5] + mods[8]) * 0.5, 0.08)
			.clip(-1, 1) * motion;
		wet = Balance2.ar(wet[0], wet[1], weather, 1);
		cutoff = tone.linexp(0, 1, 1200, 13500);
		wet = LeakDC.ar(HPF.ar(RLPF.ar(wet, cutoff, 0.55), 35));
		wet = (wet * 0.9).tanh * 0.25;
		bad = CheckBadValues.ar(wet, post: 0);
		wet = wet.collect { |sig, i|
			Select.ar(bad[i] > 0, [sig, DC.ar(0)])
		};
		Out.ar(\outBus.ir(0), wet * \level.kr(0.72, 0.08) * 15.0);
	})
]
