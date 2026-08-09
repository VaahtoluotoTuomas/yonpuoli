// Minimal running graph double for recorder and matrix ownership tests.

CQTestGraph {
	var <server, <config, <groups, <buses, <synths, <buffers;
	var <registeredSynths, <registeredBuffers;

	*new { |server, config| ^super.newCopyArgs(server, config).prInit }

	prInit {
		config = config ?? { CQConfig.validate(()) };
		groups = IdentityDictionary.new;
		buses = IdentityDictionary.new;
		synths = IdentityDictionary.new;
		buffers = IdentityDictionary.new;
		registeredSynths = List.new;
		registeredBuffers = List.new;
		groups[\master] = Group.basicNew(server, 900);
		groups[\sources] = Group.basicNew(server, 901);
		buses[\deckA] = CQTestBus(10);
		buses[\deckB] = CQTestBus(11);
		buses[\dry] = CQTestBus(12);
		buses[\modSrc] = CQTestBus(13);
		buses[\deckA_rate] = CQTestBus(14);
		buses[\deckB_rate] = CQTestBus(15);
		buses[\stem1] = CQTestBus(1);
		buses[\stem2] = CQTestBus(2);
		CQMatrix.destNames.do { |dest, i| buses[dest] = CQTestBus(i) };
	}

	isRunning { ^true }

	registerSynth { |key, object|
		synths[key] = object;
		registeredSynths.add([key, object]);
	}

	registerBuffer { |key, object|
		buffers[key] = object;
		registeredBuffers.add([key, object]);
	}
}
