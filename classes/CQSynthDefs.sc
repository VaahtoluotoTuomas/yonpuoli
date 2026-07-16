// CQSynthDefs — every SynthDef, pure server-side DSP (ARCHITECTURE §4).
// Defs are parameterized by the validated config (channel indices bake in at
// build time); bus indices arrive as \*.ir synth args — nothing hardcodes buses.
// checkAll builds every def to bytes with a default config — no server (A1).
// All gain-class controls are lagged via CQSpec.lag (SPEC safety #4).

CQSynthDefs {

	*all { |config|
		^[this.input(config), this.master(config)]
	}

	*send { |config, server|
		this.all(config).do { |def| def.send(server) };
	}

	*checkAll {
		this.all(CQConfig.validate(())).do { |def| def.asBytes };
	}

	// ARCHITECTURE §4.2 — live/mic trims, preamp fuzz pair, envelope followers.
	// Source order in Select MUST match CQSpec's preASrc enum order.
	*input { |config|
		var srcAIdx = CQSpec.at(\preASrc)[\values].indexOf(CQSpec.default(\preASrc));
		var srcBIdx = CQSpec.at(\preBSrc)[\values].indexOf(CQSpec.default(\preBSrc));
		^SynthDef(\cqInput, {
			var inChans = config[\inChans];
			var micChan = config[\micChan];
			var liveGain = \liveGain.kr(CQSpec.default(\liveGain), CQSpec.lag(\liveGain));
			var liveL = SoundIn.ar(inChans[0]) * liveGain;
			var liveR = SoundIn.ar(inChans[1] ? inChans[0]) * liveGain;
			var mic = if(micChan.isNil) { DC.ar(0) } {
				SoundIn.ar(micChan)
					* \micGain.kr(CQSpec.default(\micGain), CQSpec.lag(\micGain))
			};
			var deckA = In.ar(\deckABus.ir(0));
			var deckB = In.ar(\deckBBus.ir(0));
			var sources = [liveL, liveR, mic, deckA, deckB, DC.ar(0)];
			var preamp = { |srcCtl, driveCtl, biasCtl|
				var sig = Select.ar(srcCtl, sources);
				var shaped = ((sig * driveCtl) + biasCtl).tanh - biasCtl.tanh; // asym. fuzz
				HPF.ar(shaped, 20)
			};
			var preA = preamp.value(
				\preASrc.kr(srcAIdx),
				\preADrive.kr(CQSpec.default(\preADrive), CQSpec.lag(\preADrive)),
				\preABias.kr(CQSpec.default(\preABias), CQSpec.lag(\preABias)));
			var preB = preamp.value(
				\preBSrc.kr(srcBIdx),
				\preBDrive.kr(CQSpec.default(\preBDrive), CQSpec.lag(\preBDrive)),
				\preBBias.kr(CQSpec.default(\preBBias), CQSpec.lag(\preBBias)));
			var envA = Amplitude.ar(preA, 0.003, 0.15).clip(0, 1);
			var envB = Amplitude.ar(preB, 0.003, 0.15).clip(0, 1);
			Out.ar(\preABus.ir(0), preA);
			Out.ar(\preBBus.ir(0), preB);
			Out.ar(\modSrcBus.ir(0) + 10, [envA, envB]);   // canonical slots 10/11 (ARCH §3.2)
			Out.ar(\dryBus.ir(0), [liveL, liveR] + (mic * 0.7)); // untreated monitor feed
		})
	}

	// ARCHITECTURE §4.6 — the sacred chain (SPEC §8.1): LeakDC → tanh headroom →
	// Limiter(0.89, 0.01) → bad-value squelch → boot fade. Fixed, non-bypassable.
	// NEVER weaken; every hardware output must pass through this.
	*master { |config|
		^SynthDef(\cqMaster, {
			var coco1 = In.ar(\coco1Bus.ir(0));
			var coco2 = In.ar(\coco2Bus.ir(0));
			var spaceRet = In.ar(\spaceRetBus.ir(0), 2);
			var dry = In.ar(\dryBus.ir(0), 2);
			var pan1 = \pan1.kr(CQSpec.default(\coco1Pan), CQSpec.lag(\coco1Pan));
			var pan2 = \pan2.kr(CQSpec.default(\coco2Pan), CQSpec.lag(\coco2Pan));
			var spaceMix = \spaceMix.kr(CQSpec.default(\spaceMix), CQSpec.lag(\spaceMix));
			var masterDry = \masterDry.kr(CQSpec.default(\masterDry), CQSpec.lag(\masterDry));
			var vol = \masterVol.kr(CQSpec.default(\masterVol), CQSpec.lag(\masterVol)).dbamp;
			var mute = 1 - \mute.kr(0, CQSpec.lag(\monitorMute));
			var fade = EnvGen.kr(Env([0, 1], [2]), \fadeIn.kr(0)); // silent boot → 2 s ramp
			var mix;
			mix = Pan2.ar(coco1, pan1) + Pan2.ar(coco2, pan2)
				+ (spaceRet * spaceMix) + (dry * masterDry);
			mix = mix * vol * mute;
			mix = LeakDC.ar(mix);
			mix = (mix * 0.5).tanh;                    // −6 dB headroom glue
			mix = Limiter.ar(mix, 0.89, 0.01);         // brickwall ≈ −1 dBFS
			mix = mix.collect { |ch|                   // NaN/inf → silence (SPEC §8.6)
				Select.ar(CheckBadValues.ar(ch, post: 0) > 0, [ch, DC.ar(0)])
			};
			mix = mix * fade;
			SendPeakRMS.kr(mix, 20, 3, '/cq_meters');
			config[\outChans].do { |hw, i| Out.ar(hw, mix[i]) };
		})
	}
}
