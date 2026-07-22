// CQSynthDefs — every SynthDef, pure server-side DSP (ARCHITECTURE §4).
// Defs are parameterized by the validated config (channel indices bake in at
// build time); bus indices arrive as \*.ir synth args — nothing hardcodes buses.
// checkAll builds every def to bytes with a default config — no server (A1).
// All gain-class controls are lagged via CQSpec.lag (SPEC safety #4).

CQSynthDefs {

	*all { |config|
		^[this.input(config), this.deck(config), this.quantussy(config),
			this.coco(config), this.space(config), this.master(config),
			this.aura, this.record(config), this.recordStems(config)]
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
			var patchDepth = \patchDepth.kr(CQSpec.default(\patchDepth), CQSpec.lag(\patchDepth));
			var mods = CQSynthDefs.prModsDecks(\modSrcBus.ir(0));
			var wRate = CQSynthDefs.prMatrixWeight(\wRateBus.ir(0), patchDepth);
			var totalRate = (rate + Mix(mods * wRate)).clip(-2, 2);  // matrix deckX_rate sum
			var env = EnvGen.kr(Env.asr(0.025, 1, 0.025), \gate.kr(1), doneAction: 2);
			var sig = PlayBuf.ar(1, buf, totalRate * BufRateScale.kr(buf), \t_trig.tr(0), 0, loop);
			var dryChannel = \dryChannel.ir(-1); // -1 mono, 0/1 linked L/R
			sig = sig * level * env;
			Out.ar(\out.ir(0), sig);
			Out.ar(\dryBus.ir(0), [
				sig * (dryChannel < 0.5),
				sig * (dryChannel.abs > 0.5)
			] * 0.7);
		})
	}

	// ARCHITECTURE §4.3 — five triangle castles in one synth, all at audio rate
	// (ranges genuinely reach audio). Cross-castle signals travel through a
	// one-block LocalIn/LocalOut ring — the accepted stand-in for the analog
	// loop, do not "fix". Castle mechanism (SPEC §5.4, normative): castle k's
	// square clocks its S&H pair once per cycle; state 0..7 picks partner
	// (state mod 4 of the other four — the CD4052) and mode (state div 4:
	// 0 = sample&hold, 1 = track&hold). S&H1 samples partner's triangle →
	// castle out; S&H2 samples partner's castle out → internal FM:
	// hz = qFreq × range × 2^(off + qChaos·5·sh2 + fmExt), clipped 0.01–4000 Hz.
	// The spesal counter is server-side (Stepper 0..7, wraps): \t_stepN steps,
	// \t_setN loads \stateN; states are reported on '/cq_spesal' for the LEDs.
	*quantussy { |config|
		var rangeIdx = { |name|
			CQSpec.at(name)[\values].indexOf(CQSpec.default(name))
		};
		^SynthDef(\cqQuantussy, {
			var fb, fbTris, fbCass, qFreq, qChaos, qShLag, patchDepth, mods;
			var castles, tris, cass, stateKr, spesalSums, sends;
			fb = LocalIn.ar(10);            // [tri1..5, cas1..5] from previous block
			fbTris = fb[0..4];
			fbCass = fb[5..9];
			qFreq = \qFreq.kr(CQSpec.default(\qFreq), CQSpec.lag(\qFreq));
			qChaos = \qChaos.kr(CQSpec.default(\qChaos), CQSpec.lag(\qChaos));
			qShLag = \qShLag.kr(CQSpec.default(\qShLag), CQSpec.lag(\qShLag));
			patchDepth = \patchDepth.kr(CQSpec.default(\patchDepth), CQSpec.lag(\patchDepth));
			mods = CQSynthDefs.prModsQuantussy(\modSrcBus.ir(0));
			castles = 5.collect { |k|
				var n = k + 1;
				var others = (0..4).removing(k);
				var stateArg = ("state" ++ n).asSymbol.kr(0);
				var wFm = CQSynthDefs.prMatrixWeight(("wFm" ++ n).asSymbol.ir(0), patchDepth);
				var wSpesal = CQSynthDefs.prMatrixWeight(
					("wSpesal" ++ n).asSymbol.ir(0), patchDepth);
				var fmExt = Mix(mods * wFm);           // matrix castleN_fm sum (octaves)
				var spesalSum = Mix(mods * wSpesal);   // pre-Schmitt = its §9 charge
				var spesalGate = Schmidt.ar(spesalSum, 0.3, 0.5);
				var spesalTrig = Trig1.ar(spesalGate, SampleDur.ir); // matrix castleN_spesal
				var state = Stepper.ar(
					T2A.ar(("t_step" ++ n).asSymbol.tr(0)) + spesalTrig,
					T2A.ar(("t_set" ++ n).asSymbol.tr(0))
						+ Impulse.ar(0),               // boot: state := stateArg
					0, 7, 1, stateArg);
				var partner = state % 4;               // which of the other four
				var thMode = state > 3.5;              // = state div 4
				var sqr = fbTris[k] > 0;               // own clock, one block late by design
				var trig = Trig1.ar(sqr, SampleDur.ir); // S&H pair clock, once per cycle
				var sh2 = Latch.ar(
					Select.ar(partner, others.collect { |i| fbCass[i] }), trig);
				var range = Select.kr(
					("range" ++ n).asSymbol.kr(rangeIdx.value(("c" ++ n ++ "Range").asSymbol)),
					#[0.02, 1, 40]);                   // SPEC §5.4 range multipliers
				var off = ("off" ++ n).asSymbol
					.kr(CQSpec.default(("c" ++ n ++ "Off").asSymbol));
				var hz = (qFreq * range * (2 ** (off + (qChaos * 5 * sh2) + fmExt)))
					.clip(0.01, 4000);
				var tri = LFTri.ar(hz);                // phase-accumulating, FM-safe
				var sampled = Select.ar(partner, others.collect { |i| fbTris[i] });
				var cas = Select.ar(thMode, [
					Latch.ar(sampled, trig),           // sample & hold
					Gate.ar(sampled, sqr)]);           // track & hold
				// qShLag: declick at 4 ms default
				[tri, Lag.ar(cas, qShLag), state, spesalSum]
			};
			tris = castles.collect { |c| c[0] };
			cass = castles.collect { |c| c[1] };
			stateKr = castles.collect { |c| A2K.kr(c[2]) };
			spesalSums = castles.collect { |c| c[3] };
			sends = 5.collect { |k|
				var name = ("c" ++ (k + 1) ++ "Send").asSymbol;
				("send" ++ (k + 1)).asSymbol.kr(CQSpec.default(name), CQSpec.lag(name))
			};
			LocalOut.ar(tris ++ cass);
			Out.ar(\modSrcBus.ir(0), tris ++ cass);    // canonical ch 0..9 (ARCH §3.2)
			Out.ar(\qMixBus.ir(0), Mix(tris * sends));
			SendReply.kr(Impulse.kr(0) + Mix(stateKr.collect { |s| Changed.kr(s) }),
				'/cq_spesal', stateKr);                // initial states + every change
			// GUI activity LEDs (SPEC §6.2): instantaneous tri values at 15 Hz —
			// infra castles visibly oscillate, audio-rate ones read as flicker
			SendReply.kr(Impulse.kr(15), '/cq_castle', tris.collect { |t| A2K.kr(t) });
			// GUI_SPEC §9 `/cq_charge`, castle block: the five spesal sums the
			// Schmitts above already read, peak-held like the coco pair (see
			// \cqCoco) so a single-sample crossing still reaches the lamp.
			// replyID 3 — the coco instances own 1 and 2.
			SendReply.kr(Impulse.kr(15), '/cq_charge',
				A2K.kr(LagUD.ar(spesalSums, 0, 0.15)), 3);
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
			var inCh, fbCh, record, write, outTap, organ, voice, lvl;
			var patchDepth, mods, wSpaf, wInVca, wFbVca, wFlip, wSkip;
			var spafSum, inVcaSum, fbVcaSum, flipSum, skipSum, flipGate, skipTrig;
			buf = \buf.ir(0);
			frames = BufFrames.ir(buf);
			speed = \speed.kr(CQSpec.default(\coco1Speed), CQSpec.lag(\coco1Speed));
			spafKnob = \spafKnob.kr(CQSpec.default(\coco1Spaf), CQSpec.lag(\coco1Spaf));
			patchDepth = \patchDepth.kr(CQSpec.default(\patchDepth), CQSpec.lag(\patchDepth));
			mods = CQSynthDefs.prModsCocos(\modSrcBus.ir(0));
			wSpaf = CQSynthDefs.prMatrixWeight(\wSpafBus.ir(0), patchDepth);
			wInVca = CQSynthDefs.prMatrixWeight(\wInVcaBus.ir(0), patchDepth);
			wFbVca = CQSynthDefs.prMatrixWeight(\wFbVcaBus.ir(0), patchDepth);
			wFlip = CQSynthDefs.prMatrixWeight(\wFlipBus.ir(0), patchDepth);
			wSkip = CQSynthDefs.prMatrixWeight(\wSkipBus.ir(0), patchDepth);
			spafSum = Mix(mods * wSpaf);
			inVcaSum = Mix(mods * wInVca);
			fbVcaSum = Mix(mods * wFbVca);
			// Schmitt threshold on gate/trig dests (SPEC §5.5): on rising through
			// +0.5, off below +0.3 — a bare comparator would chatter on noisy sums
			flipSum = Mix(mods * wFlip);
			skipSum = Mix(mods * wSkip);
			flipGate = max(\flip.kr(CQSpec.default(\coco1Flip)),
				Schmidt.ar(flipSum, 0.3, 0.5));            // flip = matrix gate OR param
			skipTrig = Trig1.ar(Schmidt.ar(skipSum, 0.3, 0.5), SampleDur.ir);
			// hard clamp 1500 Hz..SR is post-modulation by design (SPEC §5.3)
			clockHz = (speed * (2 ** (spafSum * spafKnob))).clip(1500, SampleRate.ir);
			ratio = clockHz / SampleRate.ir;
			dir = 1 - (flipGate * 2);                  // flip = matrix gate OR param
			phase = Phasor.ar(\t_skip.tr(0) + skipTrig, ratio * dir, 0, frames, 0); // address counter
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
			inVca = (\inLevel.kr(CQSpec.default(\coco1In), CQSpec.lag(\coco1In)) + inVcaSum)
				.clip(0, 2);
			fbVca = (\fbLevel.kr(CQSpec.default(\coco1Fb), CQSpec.lag(\coco1Fb)) + fbVcaSum)
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
			lvl = \level.kr(CQSpec.default(\coco1Level), CQSpec.lag(\coco1Level));
			Out.ar(\outBus.ir(0), voice * lvl);
			// pan-placed post-fader send to the FDN (SPEC §5.3, cocoNSend). gCocos
			// runs before gFx, so the send lands the same block the space processes
			// it and the master reads the return — no extra latency (ARCH §3.1/§4.4)
			Out.ar(\spaceBus.ir(0), Pan2.ar(voice * lvl,
				\pan.kr(CQSpec.default(\coco1Pan), CQSpec.lag(\coco1Pan)),
				\spaceSend.kr(CQSpec.default(\coco1Send), CQSpec.lag(\coco1Send))));
			Out.ar(\modSrcBus.ir(0) + \modTapIdx.ir(12), outTap);  // canonical ch 12/13
			Out.ar(\modSrcBus.ir(0) + \modOrganIdx.ir(14), organ); // canonical ch 14/15
			// GUI in/out meters (SPEC §6.2); replyID tells the two instances apart
			SendPeakRMS.kr([inSig, voice * lvl], 20, 3, '/cq_cocometers', \meterId.ir(0));
			// GUI_SPEC §9 loop phase → the night read-head spark. Its own message
			// at 5 Hz on purpose, not the meters' 15: the send rate IS the two
			// shells' repaint ceiling (§10), and one shell repaint costs ~30 Pen
			// calls (~1.6 ms), so 15 Hz would spend the GUI budget on two dots.
			// ...plus the read DIRECTION as an audio-rate mean, not a sample.
			// `dir` is what the chevron and the comet's tail must agree on, and
			// it is driven by max(flip param, matrix gate) — so a flip banana can
			// reverse it far faster than 5 Hz can sample. Averaging at audio rate
			// before A2K (the same law as \cqAura's lag and the charge peak-hold)
			// makes the value alias-free and gives the GUI a third, honest
			// reading: near ±1 = settled forward/flipped, near 0 = shuttling too
			// fast to draw as an arrow (GUI_SPEC §16.6).
			// third value: how often the head is being thrown back, in Hz —
			// the *combined* reset (matrix trig + \t_skip), i.e. exactly what
			// Phasor obeys. Impulses of height SampleRate through a one-pole
			// settle at their own rate, because a Lag's DC gain is 1: the
			// output IS triggers per second, smoothed over 0.2 s and self-
			// clearing when they stop. It exists because a rate this fast
			// cannot be drawn as motion (the eye fuses above ~10 Hz and this
			// message is 5 Hz), so §16.6 draws it as weight instead.
			SendReply.kr(Impulse.kr(5), '/cq_phase',
				[A2K.kr(phase / frames), A2K.kr(Lag.ar(dir, 0.15)),
					A2K.kr(Lag.ar((\t_skip.tr(0) + skipTrig) * SampleRate.ir,
						0.2))],
				\meterId.ir(0));
			// GUI_SPEC §9 `/cq_charge` — this coco's two gate/trig sums exactly as
			// the Schmitt above sees them (post-weight, pre-threshold), so the lamp
			// cannot disagree with the DSP. Peak-held at audio rate before A2K
			// (instant rise, 0.15 s fall): a trigger crossing can last a single
			// sample and a plain control-rate sample would miss it — a lamp that
			// answers "why is my flip banana silent" may not lie by omission, and
			// the fall doubles as §16.8's fired bloom. replyID = the coco's meterId.
			SendReply.kr(Impulse.kr(15), '/cq_charge',
				A2K.kr(LagUD.ar([flipSum, skipSum], 0, 0.15)), \meterId.ir(0));
		})
	}

	// ARCHITECTURE §4.5 — small 4-line FDN reverb, vanilla UGens only, default
	// OFF (spaceMix 0). Four mutually-prime DelayC lines scaled by spaceSize, a
	// Householder feedback matrix (orthogonal → norm-preserving), one-pole high
	// damping at spaceDamp in the feedback path, LeakDC on the input so no DC can
	// accumulate in the recirculation. The feedback gain is hard-capped at 0.98
	// (< 1 ⇒ always decays, never diverges); the master safety chain bounds the
	// return regardless (safety #1). in ← aSpace (cocoNSend), out → aSpaceRet.
	*space { |config|
		^SynthDef(\cqSpace, {
			var in = LeakDC.ar(In.ar(\inBus.ir(0), 2));
			var size = \spaceSize.kr(CQSpec.default(\spaceSize), CQSpec.lag(\spaceSize));
			var damp = \spaceDamp.kr(CQSpec.default(\spaceDamp), CQSpec.lag(\spaceDamp));
			var g = size.min(0.98);                        // decay/size; hard cap 0.98
			var dampCoef = exp(-2pi * damp / SampleRate.ir).clip(0, 0.999);
			var times = [0.0293, 0.0373, 0.0437, 0.0531]; // s, ~29/37/44/53 ms (ARCH §4.5)
			var prev = LocalIn.ar(4);                      // delay outs, previous block
			var damped = OnePole.ar(prev, dampCoef);       // one-pole damping in the loop
			var reflected = damped - (damped.sum * 0.5);   // Householder H = I − (2/N)11ᵀ
			var node = (reflected * g) + ([in[0], in[1], in[0], in[1]] * 0.3);
			var delays = 4.collect { |i| DelayC.ar(node[i], 0.07, times[i] * size) };
			LocalOut.ar(delays);
			Out.ar(\outBus.ir(0), [delays[0] + delays[3], delays[1] + delays[2]]);
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
			var mix, driven, preLim, grDb, stem1, stem2, hotRms, hotTimer;
			mix = Pan2.ar(coco1, pan1) + Pan2.ar(coco2, pan2)
				+ (spaceRet * spaceMix) + (dry * masterDry);
			mix = mix * vol * mute;
			driven = mix;                              // pre-safety "how hard is the
			                                           // master driven" — the watchdog
			                                           // measures THIS, not the tamed output
			mix = LeakDC.ar(mix);
			mix = (mix * 0.5).tanh;                    // −6 dB headroom glue
			preLim = mix;
			mix = Limiter.ar(mix, 0.89, 0.01);         // brickwall ≈ −1 dBFS
			// GUI limiter GR meter (SPEC §6.2): dB the Limiter stage is taking off,
			// measured pre-fade so panic/boot-fade cannot fake a reduction
			grDb = (Amplitude.kr(preLim, 0.01, 0.3).max(1e-6).ampdb
				- Amplitude.kr(mix, 0.01, 0.3).max(1e-6).ampdb).max(0);
			SendReply.kr(Impulse.kr(20), '/cq_gr', grDb);
			mix = mix.collect { |ch|                   // NaN/inf → silence (SPEC §8.6)
				Select.ar(CheckBadValues.ar(ch, post: 0) > 0, [ch, DC.ar(0)])
			};
			mix = mix * fade;
			// watchdog auto-trim (SPEC §5.6): a −6 dB step applied POST-limiter (so it
			// genuinely lowers output RMS, not just what the limiter re-pins), lagged
			// 100 ms so it never zippers. Language engages it on '/cq_hot' when config
			// autoTrim is on; default 1 = 0 dB, reset to 1 on every (re)boot.
			mix = mix * \trim.kr(1, 0.1);
			// sustained-hot detector (SPEC §5.6): the DRIVEN master mix (per channel,
			// louder wins) above −6 dBFS RMS held a continuous 5 s → one '/cq_hot'.
			// Measured on `driven` (pre-safety), NOT the output: the tanh+limiter chain
			// tames the output to ≤ ~−6 dBFS RMS regardless of drive, so measuring the
			// output never reliably crossed a threshold; the driven mix is a wide range
			// (dense material at vol 0 measured +1.6 dBFS) that faithfully reflects how
			// hard the master is pushed. Sweep resets on every cool→hot edge, so brief
			// peaks never trip it — only sustained hard driving. (Verified: the detector
			// fires at 5.1 s once a sustained signal crosses the threshold.)
			hotRms = max(Lag.ar(driven[0].squared, 0.3), Lag.ar(driven[1].squared, 0.3)).sqrt;
			hotTimer = Sweep.ar(hotRms > (-6.0).dbamp, 1) * (hotRms > (-6.0).dbamp);
			SendReply.kr(Trig1.kr(A2K.kr(hotTimer > 5), ControlDur.ir), '/cq_hot');
			SendPeakRMS.kr(mix, 20, 3, '/cq_meters');
			config[\outChans].do { |hw, i| Out.ar(hw, mix[i]) };
			// stems (SPEC §5.7): each post-safety Coco through the IDENTICAL safety
			// chain (invariant #1), plus the same mute + boot-fade as the master but
			// never the master vol — an independent line-level feed to the modular.
			// Always published to private buses (stem recorder reads them); also to
			// hardware stemOuts when configured.
			stem1 = CQSynthDefs.prSafetyChain(coco1 * mute) * fade;
			stem2 = CQSynthDefs.prSafetyChain(coco2 * mute) * fade;
			Out.ar(\stem1Bus.ir(0), stem1);
			Out.ar(\stem2Bus.ir(0), stem2);
			config[\stemOuts] !? { |outs|
				Out.ar(outs[0], stem1);
				Out.ar(outs[1] ? outs[0], stem2);
			};
		})
	}

	// GUI_SPEC §9 `/cq_aura` — a §9 telemetry addition (its sibling '/cq_phase'
	// lives inside \cqCoco), and telemetry alone: it reads the finished
	// mod-source bus and writes no audio
	// anywhere, so no safety invariant is in its path. Spawned in gMaster, i.e.
	// after every aModSrc writer, so plain In.ar sees this block's values (the
	// InFeedback convention above applies only to readers that run first).
	// Rectify and smooth at AUDIO rate before A2K: an audio-rate castle would
	// otherwise alias into the control stream and glow as random flicker. One
	// lag serves both regimes — an audio source settles at its mean magnitude,
	// an infra one keeps breathing its own shape well below the 2 Hz corner.
	*aura {
		^SynthDef(\cqAura, {
			var sources = In.ar(\modSrcBus.ir(0), 16);
			SendReply.kr(Impulse.kr(15), '/cq_aura',
				A2K.kr(Lag.ar(sources.abs, 0.05)));
		})
	}

	// SPEC §5.7 recorder tap (spawned by CQRecorder at the tail of gMaster,
	// only while recording): reads the HARDWARE output buses after \cqMaster
	// has written them, so the file is exactly the post-safety signal the
	// speakers got — the safety chain is inherited by construction (safety #1)
	*record { |config|
		^SynthDef(\cqRecord, {
			DiskOut.ar(\buf.ir(0), [In.ar(\outL.ir(0)), In.ar(\outR.ir(1))]);
		})
	}

	// SPEC §5.7 stem recorder: master L/R plus the two post-safety Coco stems the
	// \cqMaster synth publishes to private buses (each already through the identical
	// safety chain, invariant #1). 4-ch file; same gMaster-tail tap as \cqRecord.
	*recordStems { |config|
		^SynthDef(\cqRecordStems, {
			DiskOut.ar(\buf.ir(0), [
				In.ar(\outL.ir(0)), In.ar(\outR.ir(1)),
				In.ar(\stem1.ir(0)), In.ar(\stem2.ir(0))
			]);
		})
	}

	// the sacred safety chain (SPEC §8.1, ARCH §4.6), factored so every stem is
	// provably identical to it (safety #1: "identical chain on each stem"). The
	// master's own main path keeps the chain inline because it interleaves the
	// GR-meter tap between the tanh and the Limiter. Mono in, mono out.
	// LeakDC → tanh (−6 dB headroom) → Limiter(0.89, 0.01) → NaN/inf squelch.
	*prSafetyChain { |sig|
		var m = LeakDC.ar(sig);
		m = (m * 0.5).tanh;
		m = Limiter.ar(m, 0.89, 0.01);
		^Select.ar(CheckBadValues.ar(m, post: 0) > 0, [m, DC.ar(0)])
	}

	// ---- private (routing-matrix helpers, ARCHITECTURE §3.2/§3.3) ----
	// aModSrc writers per group order (ARCH §3.1): ch 0-9 quantussy (gQuantussy),
	// ch 10-11 input (gSources), ch 12-15 cocos (gCocos). A channel is read with
	// In.ar only when its writer has already run this block; otherwise InFeedback
	// (one block late — In.ar on an untouched bus would read silence, not old data).

	// cocos run after both writers above them: only the coco taps (12-15, which
	// include the reader's own output) are one block late
	*prModsCocos { |modSrcBus|
		^In.ar(modSrcBus, 12) ++ InFeedback.ar(modSrcBus + 12, 4)
	}

	// the quantussy reads its OWN outputs (0-9) — it hasn't written them yet this
	// block, so they come via InFeedback, matching the LocalIn ring's one-block
	// convention; env followers (10-11, gSources) are current
	*prModsQuantussy { |modSrcBus|
		^InFeedback.ar(modSrcBus, 10) ++ In.ar(modSrcBus + 10, 2)
			++ InFeedback.ar(modSrcBus + 12, 4)
	}

	// decks run before every aModSrc writer (\addToHead in gSources, ahead even
	// of \cqInput) — all 16 channels are one block late
	*prModsDecks { |modSrcBus|
		^InFeedback.ar(modSrcBus, 16)
	}

	// matrix weight-bus read: 50 ms smoothing + global patchDepth scaler (ARCH §3.3)
	*prMatrixWeight { |busArg, patchDepth|
		^Lag.kr(In.kr(busArg, 16), 0.05) * patchDepth
	}
}
