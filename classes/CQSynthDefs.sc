// CQSynthDefs — every SynthDef, pure server-side DSP (ARCHITECTURE §4).
// Defs are parameterized by the validated config (channel indices bake in at
// build time); bus indices arrive as \*.ir synth args — nothing hardcodes buses.
// checkAll builds every def to bytes with a default config — no server (A1).
// All gain-class controls are lagged via CQSpec.lag (SPEC safety #4).

CQSynthDefs {

	*all { |config|
		^[this.input(config), this.deck(config), this.quantussy(config),
			this.coco(config), this.master(config),
			this.aura, this.record(config), this.recordStems(config)]
	}

	*send { |config, server|
		this.all(config).do { |def| def.send(server) };
	}

	*checkAll {
		this.all(CQConfig.validate(())).do { |def| def.asBytes };
	}

	// NIGHT's read head shares the ambient animation ceiling with the castle
	// fireflies. Its GUI ink lives on a dedicated lightweight layer, so this
	// rate no longer implies repainting either complete Coco shell.
	*phaseReplyHz { ^15 }
	*ioPresenceReplyHz { ^15 }

	// §13.8's accepted partial preamp compensation. Drive still raises level
	// substantially; one dB per doubling merely stops loudness from masking the
	// saturation gesture in longer playing sessions. The curve is derived from
	// the actual drive value (never its GUI norm) and is anchored at drive = 1.
	*preampMakeupDbPerOct { ^1.0 }
	*gateOffThreshold { ^0.3 }
	*gateOnThreshold { ^0.5 }
	// The address-bit organ used to enter the audible mix at x0.3. PLAY-8 found
	// it almost hidden even with the hand fully open, while its modulation jack
	// was already healthy. x0.55 is a +5.3 dB correction in the voice branch
	// only: the published organ source and every matrix threshold stay unchanged.
	*organAudibleGain { ^0.55 }
	// The software-only VCA destinations add to the matching Night hand and
	// stop at that hand's own documented range. Matrix modulation can therefore
	// open a closed Organ/SlotQ path without inventing a hidden boost country.
	*organVcaCeiling { ^1.0 }
	*slotQVcaCeiling { ^1.5 }
	*slotXVcaCeiling { ^this.slotQVcaCeiling }
	*dustCeiling { ^0.5 }
	*dustMatrixRange { ^0.18 }
	// The weighted band-limited square stack reaches only about +0.56 at the
	// default 24 kHz clock. Its patch jack must inhabit the same useful ±1
	// source country as tri/cas: ×1.6 then clip gives a default 0.60 banana
	// measured headroom above the +0.5 Schmitt threshold. The audible post-RAM
	// organ voice deliberately keeps the unconditioned stack and its old gain.
	*organModGain { ^1.6 }

	// The Record banana is a momentary inversion of the hand-set Hold state:
	// held -> write while the gate is high, writing -> lossless hold. Subtraction
	// is XOR for the two 0/1 states and works identically for scalars and UGens.
	// Nothing on this path writes the persistent CQState parameter.
	*recordToggledHold { |manualHold, matrixGate|
		^(manualHold - matrixGate).abs
	}

	*prPreampMakeup { |drive|
		^((drive.log2 * this.preampMakeupDbPerOct.neg).dbamp)
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
			// AIO-4 presence taps the hardware ports before either trim and before
			// the preamp source selector. The audio expressions below are otherwise
			// unchanged: these names only expose the already-read SoundIn signals.
			var rawLiveL = SoundIn.ar(inChans[0]);
			var rawLiveR = SoundIn.ar(inChans[1] ? inChans[0]);
			var rawMic = if(micChan.isNil) { DC.ar(0) } {
				SoundIn.ar(micChan)
			};
			var liveL = rawLiveL * liveGain;
			var liveR = rawLiveR * liveGain;
			var mic = if(micChan.isNil) { DC.ar(0) } {
				rawMic
					* \micGain.kr(CQSpec.default(\micGain), CQSpec.lag(\micGain))
			};
			var deckA = In.ar(\deckABus.ir(0));
			var deckB = In.ar(\deckBBus.ir(0));
			var sources = [liveL, liveR, mic, deckA, deckB, DC.ar(0)];
			var preamp = { |srcCtl, driveCtl, biasCtl|
				var sig = Select.ar(srcCtl, sources);
				var shaped = ((sig * driveCtl) + biasCtl).tanh - biasCtl.tanh;
				// §13.8: accepted -1 dB per actual Drive octave, after the
				// asymmetric waveshaper. Envelope followers below therefore
				// publish the audible compensated signal, not its hotter shadow.
				HPF.ar(shaped * CQSynthDefs.prPreampMakeup(driveCtl), 20)
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
			// Telemetry only, canonical [live L, live R, mic]. A missing mic is
			// the rawMic DC zero above, so the message shape never changes.
			SendPeakRMS.kr([rawLiveL, rawLiveR, rawMic],
				this.ioPresenceReplyHz, 3, '/cq_io_inputs');
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
			var castles, tris, cass, stateKr, spesalSums, spesalTrigs, castleHz, sends;
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
				var spesalGate = Schmidt.ar(spesalSum,
					CQSynthDefs.gateOffThreshold, CQSynthDefs.gateOnThreshold);
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
				[tri, Lag.ar(cas, qShLag), state, spesalSum, spesalTrig, hz]
			};
			tris = castles.collect { |c| c[0] };
			cass = castles.collect { |c| c[1] };
			stateKr = castles.collect { |c| A2K.kr(c[2]) };
			spesalSums = castles.collect { |c| c[3] };
			spesalTrigs = castles.collect { |c| c[4] };
			castleHz = castles.collect { |c| c[5] };
			sends = 5.collect { |k|
				var name = ("c" ++ (k + 1) ++ "Send").asSymbol;
				("send" ++ (k + 1)).asSymbol.kr(CQSpec.default(name), CQSpec.lag(name))
			};
			LocalOut.ar(tris ++ cass);
			Out.ar(\modSrcBus.ir(0), tris ++ cass);    // canonical ch 0..9 (ARCH §3.2)
			Out.ar(\qMixBus.ir(0), Mix(tris * sends));
			SendReply.kr(Impulse.kr(0) + Mix(stateKr.collect { |s| Changed.kr(s) }),
				'/cq_spesal', stateKr);                // initial states + every change
			// The inner reeds receive raw triangle position plus the realized,
			// gently averaged rate. At <=3 Hz the GUI can move a pearl honestly;
			// above that it ignores the aliased sample and draws a stable blur.
			SendReply.kr(Impulse.kr(15), '/cq_castle',
				A2K.kr(tris) ++ A2K.kr(Lag.ar(castleHz, 0.15)));
			// GUI_SPEC §9 `/cq_charge`, castle block: first the five spesal sums
			// the Schmitts above read, then their five exact triggers. Both halves
			// are peak-held like the coco pair (see \cqCoco), so a one-sample event
			// still reaches the lamp without a GUI-side threshold guess.
			// replyID 3 — the coco instances own 1 and 2.
			SendReply.kr(Impulse.kr(15), '/cq_charge',
				A2K.kr(LagUD.ar(spesalSums, 0, 0.15))
					++ A2K.kr(LagUD.ar(spesalTrigs, 0, 0.15)), 3);
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
			var inCh, fbCh, dustLevel, dustGrain, dustGains, dustSig, record, write, outTap;
			var organ, organMod, voice, lvl, organVca, slotQVca, slotXVca;
			var patchDepth, mods, wSpaf, wInVca, wFbVca, wFlip, wSkip;
			var wOrgan, wSlotQ, wSlotX, wDust, wRecord;
			var selfModSources, cocoIndex;
			var spafSum, inVcaSum, fbVcaSum, flipSum, skipSum;
			var organSum, slotQSum, slotXSum, dustSum, recordSum;
			var flipMatrixGate, flipGate, skipTrig, recordMatrixGate;
			var manualHold, effectiveHold;
			var spafApplied, spafEffective, modActual, modSlow, modSpread, modActivity;
			var phaseTelemetry;
			buf = \buf.ir(0);
			frames = BufFrames.ir(buf);
			speed = \speed.kr(CQSpec.default(\coco1Speed), CQSpec.lag(\coco1Speed));
			spafKnob = \spafKnob.kr(CQSpec.default(\coco1Spaf), CQSpec.lag(\coco1Spaf));
			patchDepth = \patchDepth.kr(CQSpec.default(\patchDepth), CQSpec.lag(\patchDepth));
			// A Coco is both consumer and writer of its own loop/organ sources.
			// Local feedback makes that one-block self-route explicit. Relying only
			// on a four-channel shared InFeedback span made the same org jack work in
			// the earlier Quantussy node while its own Coco could read silence.
			selfModSources = LocalIn.ar(2);             // prior [outTap, organ]
			cocoIndex = \cocoIndex.ir(0);               // 0 = coco1, 1 = coco2
			mods = CQSynthDefs.prModsCocos(
				\modSrcBus.ir(0), selfModSources, cocoIndex);
			wSpaf = CQSynthDefs.prMatrixWeight(\wSpafBus.ir(0), patchDepth);
			wInVca = CQSynthDefs.prMatrixWeight(\wInVcaBus.ir(0), patchDepth);
			wFbVca = CQSynthDefs.prMatrixWeight(\wFbVcaBus.ir(0), patchDepth);
			wFlip = CQSynthDefs.prMatrixWeight(\wFlipBus.ir(0), patchDepth);
			wSkip = CQSynthDefs.prMatrixWeight(\wSkipBus.ir(0), patchDepth);
			wOrgan = CQSynthDefs.prMatrixWeight(\wOrganBus.ir(0), patchDepth);
			wSlotQ = CQSynthDefs.prMatrixWeight(\wSlotQBus.ir(0), patchDepth);
			wSlotX = CQSynthDefs.prMatrixWeight(\wSlotXBus.ir(0), patchDepth);
			wDust = CQSynthDefs.prMatrixWeight(\wDustBus.ir(0), patchDepth);
			wRecord = CQSynthDefs.prMatrixWeight(\wRecordBus.ir(0), patchDepth);
			spafSum = Mix(mods * wSpaf);
			inVcaSum = Mix(mods * wInVca);
			fbVcaSum = Mix(mods * wFbVca);
			organSum = Mix(mods * wOrgan);
			slotQSum = Mix(mods * wSlotQ);
			slotXSum = Mix(mods * wSlotX);
			dustSum = Mix(mods * wDust);
			recordSum = Mix(mods * wRecord);
			// Schmitt threshold on gate/trig dests (SPEC §5.5): on rising through
			// +0.5, off below +0.3 — a bare comparator would chatter on noisy sums
			flipSum = Mix(mods * wFlip);
			skipSum = Mix(mods * wSkip);
			flipMatrixGate = Schmidt.ar(flipSum, CQSynthDefs.gateOffThreshold,
				CQSynthDefs.gateOnThreshold);
			flipGate = max(\flip.kr(CQSpec.default(\coco1Flip)), flipMatrixGate);
			skipTrig = Trig1.ar(Schmidt.ar(skipSum,
				CQSynthDefs.gateOffThreshold, CQSynthDefs.gateOnThreshold), SampleDur.ir);
			recordMatrixGate = Schmidt.ar(recordSum,
				CQSynthDefs.gateOffThreshold, CQSynthDefs.gateOnThreshold);
			organVca = (\organLvl.kr(CQSpec.default(\coco1Organ),
				CQSpec.lag(\coco1Organ)) + CQSynthDefs.prVcaLaw(organSum))
				.clip(0, CQSynthDefs.organVcaCeiling);
			slotQVca = (\slotQ.kr(CQSpec.default(\coco1SlotQ),
				CQSpec.lag(\coco1SlotQ)) + CQSynthDefs.prVcaLaw(slotQSum))
				.clip(0, CQSynthDefs.slotQVcaCeiling);
			slotXVca = (\slotX.kr(CQSpec.default(\coco1SlotX),
				CQSpec.lag(\coco1SlotX)) + CQSynthDefs.prVcaLaw(slotXSum))
				.clip(0, CQSynthDefs.slotXVcaCeiling);
			// hard clamp floor..SR is post-modulation by design (SPEC §5.3): the
			// floor sits an octave under the knob's own, so a banana into _spaf
			// still reaches a country the hand alone cannot ("ground jack" trick)
			spafApplied = spafSum * spafKnob;
			clockHz = (speed * (2 ** spafApplied))
				.clip(CQSpec.speedClampFloor, SampleRate.ir);
			spafEffective = (clockHz / speed).log2;
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
				+ (In.ar(\qMixBus.ir(0)) * slotQVca)
				+ (InFeedback.ar(\crossBus.ir(0))   // other coco, one block late by design
					* slotXVca);
			// The knob is the valve's rest and the matrix opens and shuts it.
			// §13.5's ear pass accepted one fixed signed p=2 contribution law:
			// low cable weights still shade continuously, medium weights phrase,
			// and only deliberate high weights cut full holes.
			inVca = (\inLevel.kr(CQSpec.default(\coco1In), CQSpec.lag(\coco1In))
				+ CQSynthDefs.prVcaLaw(inVcaSum)).clip(0, 2);
			fbVca = (\fbLevel.kr(CQSpec.default(\coco1Fb), CQSpec.lag(\coco1Fb))
				+ CQSynthDefs.prVcaLaw(fbVcaSum)).clip(0, 2.5);
			inEnv = Amplitude.ar(inSig, 0.003, 0.08);
			fbEnv = Amplitude.ar(here, 0.003, 0.08);
			inCh = dolby.value(inSig, \dolbyIn.kr(enumIdx.value(\coco1DolbyIn)),
				inEnv, fbEnv);
			fbCh = dolby.value(here, \dolbyFb.kr(enumIdx.value(\coco1DolbyFb)),
				fbEnv, inEnv);
			// §13.3: chip debris, not a tape-hiss control. The quiet filtered bed
			// keeps low dust settings continuous; sparse bipolar errors give the
			// converter its own rough grain. Dust2's source rate is 2% of SR rather
			// than 2% of clockHz on purpose: at slow clocks BufWr revisits one RAM
			// cell for many server samples and only the last error survives. That
			// natural thinning makes the STORED crackle density follow clockHz
			// linearly (~30..960 grains/s at 1.5k..48k), instead of quadratically.
			dustLevel = CQSynthDefs.dustEffective(
				\dust.kr(CQSpec.default(\coco1Dust), CQSpec.lag(\coco1Dust)),
				dustSum);
			dustGrain = \dustGrain.kr(CQSpec.default(\coco1DustGrain),
				CQSpec.lag(\coco1DustGrain));
			dustGains = CQSynthDefs.dustGrainGains(dustGrain);
			dustSig = (LPF.ar(WhiteNoise.ar, 9000) * 0.35 * dustGains[0])
				+ (Dust2.ar(SampleRate.ir * 0.02) * 1.2 * dustGains[1]);
			record = (inCh * inVca) + (fbCh * fbVca) + (dustSig * dustLevel);
			record = Select.ar(\wrap.kr(CQSpec.default(\coco1Wrap)),
				[record.clip2(1), record.wrap2(1)]);       // ADC rails — safety #2
			record = record.round(2 ** (1 - \bits.kr(CQSpec.default(\coco1Bits))));
			// Hold is the manual seal. A positive matrix Record gate momentarily
			// inverts it in either direction, then returns exactly to the hand-set
			// state; it never mutates the persistent Hold parameter.
			manualHold = \hold.kr(CQSpec.default(\coco1Hold));
			effectiveHold = CQSynthDefs.recordToggledHold(
				manualHold, recordMatrixGate);
			write = Select.ar(effectiveHold, [record, here]);
			BufWr.ar(write, buf, phase, loop: 1);          // hold = identity rewrite
			outTap = Select.ar(\interp.kr(enumIdx.value(\coco1Interp)),
				[here, hereSmooth]);
			organ = Mix(Pulse.ar(clockHz / [128, 256, 512], 0.5, [0.5, 0.3, 0.2]));
			// ^ square stack off the address-counter bits [interpretation, SPEC §3.9]
			organMod = (organ * CQSynthDefs.organModGain).clip2(1);
			// Own Coco/conditioned-organ matrix sources return on the next block without
			// depending on the shared bus timestamp. Out below still publishes them
			// for the other Coco, Quantussy, decks and GUI telemetry.
			LocalOut.ar([outTap, organMod]);
			voice = outTap + (organ * organVca * CQSynthDefs.organAudibleGain);
			lvl = \level.kr(CQSpec.default(\coco1Level), CQSpec.lag(\coco1Level));
			Out.ar(\outBus.ir(0), voice * lvl);
			Out.ar(\modSrcBus.ir(0) + \modTapIdx.ir(12), outTap);  // canonical ch 12/13
			Out.ar(\modSrcBus.ir(0) + \modOrganIdx.ir(14), organMod); // canonical ch 14/15
			// GUI in/out meters (SPEC §6.2); replyID tells the two instances apart
			SendPeakRMS.kr([inSig, voice * lvl], 20, 3, '/cq_cocometers', \meterId.ir(0));
			// GUI_SPEC §9 loop phase → the night read-head spark. Its own message
			// shares the 15 Hz ambient ceiling with the castle fireflies. The GUI
			// repaints only a clear dot-and-tail layer, never either complete shell.
			// ...plus the read DIRECTION as an audio-rate mean, not a sample.
			// `dir` is what the chevron and the comet's tail must agree on, and
			// it is driven by max(flip param, matrix gate) — so a flip banana can
			// reverse it far faster than 15 Hz can sample. Averaging at audio rate
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
			// message is 15 Hz), so §16.6 draws it as weight instead.
			//
			// The same frame also carries the seven continuous matrix destinations
			// as DSP truth: slow centre, unresolved excursion and connected weight.
			// The first is allowed to move a small satellite pearl. Audio-rate motion
			// is reduced *before* A2K to a stable peak wake, never sampled into a
			// false 15 Hz wobble. Activity hides the wake when no planted banana can
			// affect the destination. SP.AF reports the realized octave shift after
			// the clock clamp (relative to the unmodulated speed), not an inaudible
			// request beyond the rail;
			// input/feedback/organ/SlotQ/SlotX report their post-law, post-clip
			// effective VCAs; Dust reports its signed-square, post-clip level.
			modActual = [spafEffective, inVca, fbVca, organVca, slotQVca,
				slotXVca, dustLevel];
			modSlow = Lag.ar(modActual, 0.035);
			modSpread = LagUD.ar((modActual - modSlow).abs, 0, 0.18);
			modActivity = [Mix(wSpaf.abs), Mix(wInVca.abs), Mix(wFbVca.abs),
				Mix(wOrgan.abs), Mix(wSlotQ.abs), Mix(wSlotX.abs), Mix(wDust.abs)]
				.clip(0, 1);
			phaseTelemetry = [A2K.kr(phase / frames),
				A2K.kr(Lag.ar(dir, 0.15)),
				A2K.kr(Lag.ar((\t_skip.tr(0) + skipTrig) * SampleRate.ir, 0.2))]
				++ A2K.kr(modSlow) ++ A2K.kr(modSpread) ++ modActivity;
			SendReply.kr(Impulse.kr(this.phaseReplyHz), '/cq_phase', phaseTelemetry,
				\meterId.ir(0));
			// GUI_SPEC §9 `/cq_charge` — this coco's three gate/trig sums exactly as
			// the Schmitt above sees them (post-weight, pre-threshold), so the lamp
			// cannot disagree with the DSP. Peak-held at audio rate before A2K
			// (instant rise, 0.15 s fall): a trigger crossing can last a single
			// sample and a plain control-rate sample would miss it — a lamp that
			// answers "why is my flip banana silent" may not lie by omission. The
			// second half separately holds the exact post-Schmitt gate/trigger that
			// drives §16.8's fire diamond. replyID = the coco's meterId.
			SendReply.kr(Impulse.kr(15), '/cq_charge',
				A2K.kr(LagUD.ar([flipSum, skipSum, recordSum], 0, 0.15))
					++ A2K.kr(LagUD.ar(
						[flipMatrixGate, skipTrig, recordMatrixGate], 0, 0.15)),
				\meterId.ir(0));
		})
	}

	// ARCHITECTURE §4.5 — the sacred chain (SPEC §8.1): LeakDC → tanh headroom →
	// Limiter(0.89, 0.01) → bad-value squelch → boot fade. Fixed, non-bypassable.
	// NEVER weaken; every hardware output must pass through this.
	*master { |config|
		^SynthDef(\cqMaster, {
			var coco1 = In.ar(\coco1Bus.ir(0));
			var coco2 = In.ar(\coco2Bus.ir(0));
			var dry = In.ar(\dryBus.ir(0), 2);
			var pan1 = \pan1.kr(CQSpec.default(\coco1Pan), CQSpec.lag(\coco1Pan));
			var pan2 = \pan2.kr(CQSpec.default(\coco2Pan), CQSpec.lag(\coco2Pan));
			var masterDry = \masterDry.kr(CQSpec.default(\masterDry), CQSpec.lag(\masterDry));
			var vol = \masterVol.kr(CQSpec.default(\masterVol), CQSpec.lag(\masterVol)).dbamp;
			var mute = 1 - \mute.kr(0, CQSpec.lag(\monitorMute));
			var fade = EnvGen.kr(Env([0, 1], [2]), \fadeIn.kr(0)); // silent boot → 2 s ramp
			var mix, driven, preLim, grDb, stem1, stem2, hotRms, hotTimer;
			mix = Pan2.ar(coco1, pan1) + Pan2.ar(coco2, pan2)
				+ (dry * masterDry);
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

	// the sacred safety chain (SPEC §8.1, ARCH §4.5), factored so every stem is
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

	// Cocos run after both writers above them. Coco-derived channels are read
	// separately so a current write on one channel cannot decide the timestamp
	// of its neighbours. The reader's own loop/conditioned-organ pair comes from an
	// explicit LocalIn/LocalOut block delay; the other Coco remains on the shared
	// feedback bus. This is the actual circular signal graph, not a copied source.
	*prModsCocos { |modSrcBus, selfSources, cocoIndex = 0|
		var delayed = 4.collect { |i| InFeedback.ar(modSrcBus + 12 + i) };
		var own = selfSources ?? { [delayed[0], delayed[2]] };
		var derived = [
			Select.ar(cocoIndex, [own[0], delayed[0]]),
			Select.ar(cocoIndex, [delayed[1], own[0]]),
			Select.ar(cocoIndex, [own[1], delayed[2]]),
			Select.ar(cocoIndex, [delayed[3], own[1]])
		];
		^In.ar(modSrcBus, 12) ++ derived
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

	// ---- the VCA law (SPEC §5.5, interpretation — see ARCHITECTURE §4.4) ----
	//
	// §13.5's hypothesis: a summed LINEAR matrix VCA slides; an analog
	// verso/inverso VCA breathes, so duckings and tremolos should have a
	// "valve opens / valve shuts" body. The law is (1 + sum) ** 2.
	//
	// The plan sketched that as the gain itself. It cannot be, because in this
	// instrument the knob is the VCA's base and the matrix ADDS to it
	// (`knob + sum`, SPEC §5.5) — shaping the total would silently recalibrate
	// every knob/default and a banana could no longer open a shut VCA, which is
	// a documented patching move. So the shaping is applied to the modulation
	// alone, as the contribution it makes:
	//
	//     shaped = (1 + sum) ** 2 - 1
	//
	// At knob=unity this is the plan's law BEFORE the real VCA ceiling
	// (1 + shaped = (1 + sum)**2); input then clips to 2 and feedback to 2.5.
	// Away from unity the knob keeps its zero-sum meaning. The contribution's
	// anchors are: sum 0 -> 0, sum -1 -> -1 (therefore it closes a knob at or
	// below unity, but only subtracts one above unity), sum +1 -> +3 before the
	// ceiling. clip2 inside the loop remains safety invariant #2.
	//
	// `g.abs * g` is g**2 * g.sign in two ops: signed, so the law stays
	// monotonic below sum = -1 instead of folding back up.
	// The sum is clamped BEFORE shaping (§13.5's rake): \mayhem swings a VCA
	// sum far past ±1 and squaring an unclamped swing would scorch the tone
	// long before the clip caught it. Continuous VCA paths only — gate/trig
	// destinations keep their Schmitt semantics and never come here.
	*vcaSumClamp { ^2 }

	*prVcaLaw { |sum|
		var g = 1 + sum.clip(this.vcaSumClamp.neg, this.vcaSumClamp);
		^(g.abs * g) - 1
	}

	// Dust is material, not a gain stage. A signed-square ±0.18 contribution
	// gives the hand a quiet centre and lets positive/negative bananas grow or
	// clear debris without allowing the converter injection above its 0.5 rail.
	*dustMatrixContribution { |sum|
		var m = sum.clip(-1, 1);
		^(m.abs * m) * this.dustMatrixRange
	}

	*dustEffective { |hand, sum|
		^(hand + this.dustMatrixContribution(sum)).clip(0, this.dustCeiling)
	}

	// Equal-power mist↔chip crossfade, normalized so the midpoint is exactly
	// [1, 1]: DustGrain=0.5 is bit-for-formula compatible with the old sound.
	*dustGrainGains { |grain|
		var g = grain.clip(0, 1);
		^[(2 * (1 - g)).sqrt, (2 * g).sqrt]
	}
}
