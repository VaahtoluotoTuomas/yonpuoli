// CQSynthDefs — every SynthDef, pure server-side DSP (ARCHITECTURE §4).
// Defs are parameterized by the validated config (channel indices bake in at
// build time); bus indices arrive as \*.ir synth args — nothing hardcodes buses.
// checkAll builds every def to bytes with a default config — no server (A1).
// All gain-class controls are lagged via CQSpec.lag (SPEC safety #4).

CQSynthDefs {

	*all { |config|
		^[this.input(config), this.deck(config), this.coco(config), this.master(config)]
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

	// ARCHITECTURE §4.1 — file player. One def for both decks; deck synths are the
	// only allowed synth churn: CQDeck spawns a new instance per loaded file (25 ms
	// attack) and releases the old one via \gate — a 50 ms crossfade swap. Decks
	// also add themselves to the untreated monitor bus aDry (ARCHITECTURE §3.2).
	*deck { |config|
		^SynthDef(\cqDeck, {
			var buf = \buf.ir(0);
			var rate = \rate.kr(CQSpec.default(\deckARate), CQSpec.lag(\deckARate));
			var loop = \loop.kr(CQSpec.default(\deckALoop));
			var level = \level.kr(CQSpec.default(\deckALevel), CQSpec.lag(\deckALevel));
			var env = EnvGen.kr(Env.asr(0.025, 1, 0.025), \gate.kr(1), doneAction: 2);
			var sig = PlayBuf.ar(1, buf,
				rate.clip(-2, 2) * BufRateScale.kr(buf),   // matrix deck*_rate adds here (phase 4)
				\t_trig.tr(0), 0, loop);
			sig = sig * level * env;
			Out.ar(\out.ir(0), sig);
			Out.ar(\dryBus.ir(0), sig * 0.7 ! 2);
		})
	}

	// ARCHITECTURE §4.4 — the heart. One def, two instances (buffer + buses per
	// synth). BufRd MUST be instantiated before BufWr: the pre-write read is both
	// the full-buffer delay tap and what makes hold an identity rewrite (bit-exact
	// lossless) — see ARCHITECTURE §4.4. Do not reorder.
	// Converter order (SPEC §5.3, normative): input mix → dolby(in) → ×inVCA →
	// + feedback (dolby(fb) → ×fbVCA) → + dust → clip2/wrap2 → quantize → RAM.
	// clip2(1)/wrap2(1) before quantization INSIDE the recirculation is safety
	// invariant #2 — it is what bounds all feedback. Never move or condition it.
	*coco { |config|
		var enumIdx = { |name|
			CQSpec.at(name)[\values].indexOf(CQSpec.default(name))
		};
		^SynthDef(\cqCoco, {
			var buf, frames, speed, spafKnob, clockHz, ratio, dir, phase;
			var here, hereSmooth, dolby, inSig, inVca, fbVca, inEnv, fbEnv;
			var inCh, fbCh, record, write, outTap, organ, voice;
			buf = \buf.ir(0);
			frames = BufFrames.ir(buf);
			speed = \speed.kr(CQSpec.default(\coco1Speed), CQSpec.lag(\coco1Speed));
			spafKnob = \spafKnob.kr(CQSpec.default(\coco1Spaf), CQSpec.lag(\coco1Spaf));
			// matrix dest coco*_spaf replaces the 0 in phase 4 (sum in octaves);
			// hard clamp 1500 Hz..SR is post-modulation by design (SPEC §5.3)
			clockHz = (speed * (2 ** (0 * spafKnob))).clip(1500, SampleRate.ir);
			ratio = clockHz / SampleRate.ir;
			dir = 1 - (\flip.kr(CQSpec.default(\coco1Flip)) * 2);
			phase = Phasor.ar(\t_skip.tr(0), ratio * dir, 0, frames, 0); // address counter
			here = BufRd.ar(1, buf, phase, loop: 1, interpolation: 1);   // ZOH DAC out
			hereSmooth = BufRd.ar(1, buf, phase, loop: 1, interpolation: 4);
			// ^ both reads sit BEFORE the write on purpose (see header comment)
			dolby = { |sig, mode, ownEnv, keyEnv|
				Select.ar(mode, [
					sig,
					sig * LagUD.ar(ownEnv > 0.02, 0.005, 0.05),   // \gate: self-expander
					sig * (1 - (keyEnv * 0.9)).clip(0, 1)         // \duck: "other-puncher"
				])
			};
			inSig = (In.ar(\preBus.ir(0))
					* \slotPre.kr(CQSpec.default(\coco1SlotPre), CQSpec.lag(\coco1SlotPre)))
				+ (In.ar(\deckBus.ir(0))
					* \slotDeck.kr(CQSpec.default(\coco1SlotDeck), CQSpec.lag(\coco1SlotDeck)))
				+ (In.ar(\qMixBus.ir(0))
					* \slotQ.kr(CQSpec.default(\coco1SlotQ), CQSpec.lag(\coco1SlotQ)))
				+ (InFeedback.ar(\crossBus.ir(0))   // other coco, one block late by design
					* \slotX.kr(CQSpec.default(\coco1SlotX), CQSpec.lag(\coco1SlotX)));
			// matrix VCA sums (coco*_inVca / coco*_fbVca) join these in phase 4
			inVca = \inLevel.kr(CQSpec.default(\coco1In), CQSpec.lag(\coco1In))
				.clip(0, 2);
			fbVca = \fbLevel.kr(CQSpec.default(\coco1Fb), CQSpec.lag(\coco1Fb))
				.clip(0, 2.5);
			inEnv = Amplitude.ar(inSig, 0.003, 0.08);
			fbEnv = Amplitude.ar(here, 0.003, 0.08);
			inCh = dolby.value(inSig, \dolbyIn.kr(enumIdx.value(\coco1DolbyIn)),
				inEnv, fbEnv);
			fbCh = dolby.value(here, \dolbyFb.kr(enumIdx.value(\coco1DolbyFb)),
				fbEnv, inEnv);
			record = (inCh * inVca) + (fbCh * fbVca)
				+ (WhiteNoise.ar(\dust.kr(CQSpec.default(\coco1Dust), CQSpec.lag(\coco1Dust)))
					* LFNoise2.ar(3));
			record = Select.ar(\wrap.kr(CQSpec.default(\coco1Wrap)),
				[record.clip2(1), record.wrap2(1)]);       // ADC rails — safety #2
			record = record.round(2 ** (1 - \bits.kr(CQSpec.default(\coco1Bits))));
			write = Select.ar(\hold.kr(CQSpec.default(\coco1Hold)), [record, here]);
			BufWr.ar(write, buf, phase, loop: 1);          // hold = identity rewrite
			outTap = Select.ar(\interp.kr(enumIdx.value(\coco1Interp)),
				[here, hereSmooth]);
			organ = Mix(Pulse.ar(clockHz / [128, 256, 512], 0.5, [0.5, 0.3, 0.2]));
			// ^ square stack off the address-counter bits [interpretation, SPEC §3.9]
			voice = outTap
				+ (organ * \organLvl.kr(CQSpec.default(\coco1Organ), CQSpec.lag(\coco1Organ))
					* 0.3);
			Out.ar(\outBus.ir(0),
				voice * \level.kr(CQSpec.default(\coco1Level), CQSpec.lag(\coco1Level)));
			Out.ar(\modSrcBus.ir(0) + \modTapIdx.ir(12), outTap);  // canonical ch 12/13
			Out.ar(\modSrcBus.ir(0) + \modOrganIdx.ir(14), organ); // canonical ch 14/15
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
