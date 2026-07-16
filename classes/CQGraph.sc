// CQGraph — owns every server object: buses, groups, buffers, synths.
// Nothing else allocates on the server (CLAUDE.md). The server instance is
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
	// the highest configured index — never assume more channels exist (CLAUDE.md)
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
		buses[\space] = Bus.audio(server, 2);
		buses[\spaceRet] = Bus.audio(server, 2);
		buses[\dry] = Bus.audio(server, 2);
		groups[\cq] = Group(server);
		groups[\sources] = Group(groups[\cq], \addToTail);
		groups[\quantussy] = Group(groups[\cq], \addToTail);
		groups[\cocos] = Group(groups[\cq], \addToTail);
		groups[\fx] = Group(groups[\cq], \addToTail);
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
		synths[\master] = Synth(\cqMaster, [
			\coco1Bus, buses[\coco1].index,
			\coco2Bus, buses[\coco2].index,
			\spaceRetBus, buses[\spaceRet].index,
			\dryBus, buses[\dry].index
		], groups[\master]);
		server.sync;
		isRunning = true;
	}

	// stage 3: release the boot fade — 2 s ramp to nominal (SPEC safety #5)
	fadeIn {
		synths[\master].set(\fadeIn, 1);
	}

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
}
