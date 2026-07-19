// Roadmap 3.1 / CQ-AUD-008: linked L/R is prepared independently but committed
// once, in one Server.bind bundle, with channel-preserving dry routing.

CQDeckPairTest : UnitTest {
	var server, graph, state, deckA, deckB, pair, pairClass, dir, path;

	setUp {
		server = CQTestServer();
		graph = CQTestGraph(server);
		state = CQState(nil);
		dir = this.class.filenameSymbol.asString.dirname.dirname
			+/+ "tmp" +/+ "cqdeck_pair_test";
		if(File.exists(dir).not) { File.mkdir(dir) };
		path = dir +/+ "pair.wav";
		File.use(path, "w", { |file| file.write("test") });
		deckA = CQTestDeck(graph, state, \deckA, dir);
		deckB = CQTestDeck(graph, state, \deckB, dir);
		pairClass = \CQDeckPair.asClass;
		pair = pairClass !? { pairClass.new(deckA, deckB, server) };
		CQDeck.postWarnings = false;
	}

	tearDown {
		pair !? { pair.free };
		deckA.free;
		deckB.free;
		if(File.exists(path)) { File.delete(path) };
		File.delete(dir);
		CQDeck.postWarnings = true;
	}

	prRequireContract {
		if(pair.isNil) {
			this.assert(false,
				"roadmap 3.1: CQDeckPair coordinates atomic linked-stereo loads");
			^false
		};
		^true
	}

	prWaitForRead { |deck|
		var spins = 0;
		while { deck.readRequests.isEmpty and: { spins < 1000 } } {
			0.001.wait;
			spins = spins + 1;
		};
		this.assert(deck.readRequests.notEmpty, "linked read request appeared");
	}

	prReady { |deck|
		var spins = 0;
		deck.completeRead(0);
		while { (server.pendingSyncCount < 1) and: { spins < 1000 } } {
			0.001.wait;
			spins = spins + 1;
		};
		server.releasePendingSyncs;
		0.01.wait;
	}

	prSynthMessages {
		^server.messages.select { |message| message[0] == 9 }
	}

	prArg { |message, name|
		var index = message.indexOf(name);
		^if(index.isNil) { nil } { message[index + 1] }
	}

	test_linkedPairCommitsOnceAfterBothChannelsReady {
		var messages;
		if(this.prRequireContract.not) { ^this };
		pair.load(path);
		this.prWaitForRead(deckA);
		this.prWaitForRead(deckB);
		this.prReady(deckA);
		this.assert(deckA.path.isNil and: { deckB.path.isNil },
			"neither side publishes before the pair is ready");
		this.prReady(deckB);
		this.assertEquals(deckA.path, path, "left path committed");
		this.assertEquals(deckB.path, path, "right path committed");
		this.assertEquals(server.bindCount, 1,
			"linked synth starts share exactly one server bundle/timetag");
		messages = this.prSynthMessages;
		this.assertEquals(this.prArg(messages[0], \dryChannel), 0,
			"linked left remains left in the dry monitor");
		this.assertEquals(this.prArg(messages[1], \dryChannel), 1,
			"linked right remains right in the dry monitor");
	}

	test_oneSideFailureRollsBackWholePair {
		if(this.prRequireContract.not) { ^this };
		pair.load(path);
		this.prWaitForRead(deckA);
		this.prWaitForRead(deckB);
		this.prReady(deckA);
		deckB.failRead(0);
		0.02.wait;
		this.assert(deckA.path.isNil and: { deckB.path.isNil },
			"linked load publishes neither side after one read fails");
		this.assert(deckA.readRequests[0][\buffer].freed,
			"prepared sibling buffer is released on pair rollback");
	}

	test_pairFreeInvalidatesBothPendingReads {
		if(this.prRequireContract.not) { ^this };
		pair.load(path);
		this.prWaitForRead(deckA);
		this.prWaitForRead(deckB);
		pair.free;
		deckA.free;
		deckB.free;
		deckA.completeRead(0);
		deckB.completeRead(0);
		0.02.wait;
		this.assert(deckA.path.isNil and: { deckB.path.isNil },
			"free cannot resurrect either member of a linked pair");
		this.assert(deckA.readRequests[0][\buffer].freed
			and: { deckB.readRequests[0][\buffer].freed },
			"both stale linked buffers are released");
	}
}
