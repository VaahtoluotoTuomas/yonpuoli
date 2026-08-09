// CQGraph — owns every server object: buses, groups, buffers, synths.
// Nothing else allocates on the server (AGENTS.md). The server instance is
// passed in — never assumed. build/spawn must be called from a Routine
// (Cocoquantus.boot runs inside waitForBoot's Routine) because they sync.

CQGraph {
	var <server, <config;
	var <groups, <buses, <synths, <buffers;
	var <isRunning = false;

	*new { |server, config|
		^super.newCopyArgs(server, config).prInit
	}

	prInit {
		groups = IdentityDictionary.new;
		buses = IdentityDictionary.new;
		synths = IdentityDictionary.new;
		buffers = IdentityDictionary.new;
	}

	// build ServerOptions from config before boot; channel counts derive from
	// the highest configured index — never assume more channels exist (AGENTS.md)
	applyOptions {
		var o = server.options;
		var dev = config[\device];
		var inMax = (config[\inChans] ++ [config[\micChan]]).reject(_.isNil).maxItem;
		var outMax = (config[\outChans] ++ (config[\stemOuts] ? [])).maxItem;
		o.sampleRate = config[\sampleRate];
		o.blockSize = config[\blockSize];
		o.hardwareBufferSize = config[\hardwareBufferSize];
		if(dev.isNil or: { dev.isString }) {
			o.device = dev;
		} {                                   // [inDevice, outDevice] — e.g. a laptop
			o.inDevice = dev[0];              // whose mic and phones are separate
			o.outDevice = dev[1];             // devices (same host API on both!)
		};
		o.numInputBusChannels = inMax + 1;
		o.numOutputBusChannels = outMax + 1;
	}

	// stage 1: buses + groups (ARCHITECTURE §3.1–3.2); execution order = group order
	build {
		buses[\modSrc] = Bus.audio(server, 16);
		buses[\preA] = Bus.audio(server, 1);
		buses[\preB] = Bus.audio(server, 1);
		buses[\deckA] = Bus.audio(server, 1);
		buses[\deckB] = Bus.audio(server, 1);
		buses[\qMix] = Bus.audio(server, 1);
		buses[\coco1] = Bus.audio(server, 1);
		buses[\coco2] = Bus.audio(server, 1);
		buses[\dry] = Bus.audio(server, 2);
		// post-safety Coco stems (SPEC §5.7): \cqMaster publishes each Coco through
		// the identical safety chain here; hardware stem outs and the stem recorder
		// both read these (kept internal so stem recording needs no configured outs)
		buses[\stem1] = Bus.audio(server, 1);
		buses[\stem2] = Bus.audio(server, 1);
		// routing matrix (ARCH §3.2/§3.3): one 16-ch control bus per destination,
		// keyed by the destination name itself (CQMatrix.destNames) — matrix is
		// empty at boot (all zero), CQMatrix.pushAll after spawn makes that explicit
		CQMatrix.destNames.do { |dest| buses[dest] = Bus.control(server, 16) };
		// power-on RAM state (SPEC §4): buffers full of 8-bit-quantized noise,
		// loop-locked via the hold=1 default — "digital dust" is a documented,
		// intentional sound source. Fill peak −26 dBFS: present when levels are
		// pushed, but must not mask program material (KUUNTELU 2 feedback)
		buffers[\coco1] = this.prNoiseBuffer;
		buffers[\coco2] = this.prNoiseBuffer;
		groups[\cq] = Group(server);
		groups[\sources] = Group(groups[\cq], \addToTail);
		groups[\quantussy] = Group(groups[\cq], \addToTail);
		groups[\cocos] = Group(groups[\cq], \addToTail);
		groups[\master] = Group(groups[\cq], \addToTail);
		server.sync;
	}

	// stage 2: long-lived synths in group order; graph is static after this
	spawn {
		synths[\input] = Synth(\cqInput, [
			\deckABus, buses[\deckA].index,
			\deckBBus, buses[\deckB].index,
			\modSrcBus, buses[\modSrc].index,
			\preABus, buses[\preA].index,
			\preBBus, buses[\preB].index,
			\dryBus, buses[\dry].index
		], groups[\sources]);
		synths[\quantussy] = Synth(\cqQuantussy, [
			\modSrcBus, buses[\modSrc].index,
			\qMixBus, buses[\qMix].index
		] ++ (1..5).collect { |k|
			[("wFm" ++ k).asSymbol, buses[("castle" ++ k ++ "_fm").asSymbol].index,
			 ("wSpesal" ++ k).asSymbol, buses[("castle" ++ k ++ "_spesal").asSymbol].index]
		}.flatten, groups[\quantussy]);
		// canonical aModSrc channels: coco taps 12/13, organs 14/15 (ARCH §3.2)
		[[\coco1, \preA, \deckA, \coco2, 12, 14, 0],
		 [\coco2, \preB, \deckB, \coco1, 13, 15, 1]].do { |row|
			var n = row[0];
			synths[n] = Synth(\cqCoco, [
				\buf, buffers[n].bufnum,
				\preBus, buses[row[1]].index,
				\deckBus, buses[row[2]].index,
				\qMixBus, buses[\qMix].index,
				\crossBus, buses[row[3]].index,
				\outBus, buses[n].index,
				\modSrcBus, buses[\modSrc].index,
				\modTapIdx, row[4], \modOrganIdx, row[5],
				\cocoIndex, row[6],
				\meterId, if(n == \coco1) { 1 } { 2 },   // '/cq_cocometers' replyID
				\wSpafBus, buses[(n ++ "_spaf").asSymbol].index,
				\wInVcaBus, buses[(n ++ "_inVca").asSymbol].index,
				\wFbVcaBus, buses[(n ++ "_fbVca").asSymbol].index,
				\wFlipBus, buses[(n ++ "_flip").asSymbol].index,
				\wSkipBus, buses[(n ++ "_skip").asSymbol].index,
				\wOrganBus, buses[(n ++ "_organ").asSymbol].index,
				\wSlotQBus, buses[(n ++ "_slotQ").asSymbol].index,
				\wSlotXBus, buses[(n ++ "_slotX").asSymbol].index,
				\wDustBus, buses[(n ++ "_dust").asSymbol].index,
				\wRecordBus, buses[(n ++ "_record").asSymbol].index
			], groups[\cocos]);
		};
		// GUI_SPEC §9 telemetry: reads the completed 16-channel mod-source bus in
		// the last group and only reports — it writes to no bus and no output
		synths[\aura] = Synth(\cqAura, [
			\modSrcBus, buses[\modSrc].index
		], groups[\master]);
		synths[\master] = Synth(\cqMaster, [
			\coco1Bus, buses[\coco1].index,
			\coco2Bus, buses[\coco2].index,
			\dryBus, buses[\dry].index,
			\stem1Bus, buses[\stem1].index,
			\stem2Bus, buses[\stem2].index
		], groups[\master]);
		server.sync;
		isRunning = true;
	}

	// stage 3: release the boot fade — 2 s ramp to nominal (SPEC safety #5)
	fadeIn {
		synths[\master].set(\fadeIn, 1);
	}

	// registration API for the only allowed synth churn (deck loads, recorder):
	// callers hand ownership over so teardown stays in one place (AGENTS.md)
	registerSynth { |key, synth| synths[key] = synth }
	registerBuffer { |key, buffer| buffers[key] = buffer }

	// Cmd-. killed all nodes: forget them so free won't message dead IDs
	markKilled {
		isRunning = false;
		synths = IdentityDictionary.new;
	}

	free {
		if(isRunning and: { groups[\cq].notNil }) { groups[\cq].free };
		buses.do(_.free);
		buffers.do(_.free);
		this.prInit;
		isRunning = false;
	}

	// ---- private ----

	// must run inside a Routine (build is called from waitForBoot's routine)
	prNoiseBuffer {
		var cond = Condition.new;
		var data = FloatArray.fill(config[\bufFrames], { 0.05.rand2.round(2 ** -7) });
		var buf = Buffer.loadCollection(server, data, 1, { cond.unhang });
		cond.hang;
		^buf
	}
}
