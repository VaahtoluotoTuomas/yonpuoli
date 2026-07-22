// Roadmap 3.1 / CQ-AUD-005: async deck loads are cancellable last-request-wins
// transactions. Reads and syncs are controlled by hermetic Conditions.

CQDeckTransactionTest : UnitTest {
	var server, graph, state, deck, dir, firstPath, secondPath;

	setUp {
		server = CQTestServer();
		graph = CQTestGraph(server);
		state = CQState(nil);
		dir = this.class.filenameSymbol.asString.dirname.dirname
			+/+ "tmp" +/+ "cqdeck_transaction_test";
		if(File.exists(dir).not) { File.mkdir(dir) };
		firstPath = dir +/+ "first.wav";
		secondPath = dir +/+ "second.wav";
		File.use(firstPath, "w", { |file| file.write("test") });
		File.use(secondPath, "w", { |file| file.write("test") });
		deck = CQTestDeck(graph, state, \deckA, dir);
		CQDeck.postWarnings = false;
	}

	tearDown {
		deck !? { deck.free };
		[firstPath, secondPath].do { |path| if(File.exists(path)) { File.delete(path) } };
		File.delete(dir);
		CQDeck.postWarnings = true;
	}

	prRequireContract {
		if(deck.respondsTo(\loadState).not or: { deck.respondsTo(\prepare).not }) {
			this.assert(false,
				"roadmap 3.1: CQDeck exposes a cancellable prepare/commit transaction");
			^false
		};
		^true
	}

	prWaitForRequests { |count|
		var spins = 0;
		while { (deck.readRequests.size < count) and: { spins < 1000 } } {
			0.001.wait;
			spins = spins + 1;
		};
		this.assert(deck.readRequests.size >= count, "controlled read request appeared");
	}

	prFinishRead { |index|
		var spins = 0;
		deck.completeRead(index);
		while { (server.pendingSyncCount < 1) and: {
			(deck.readRequests[index][\buffer].freed.not) and: { spins < 1000 }
		} } {
			0.001.wait;
			spins = spins + 1;
		};
		server.releasePendingSyncs;
		0.01.wait;
	}

	test_latestRequestWinsWhenItCompletesFirst {
		if(this.prRequireContract.not) { ^this };
		deck.load(firstPath);
		deck.load(secondPath);
		this.prWaitForRequests(2);
		this.prFinishRead(1);
		this.assertEquals(deck.path, secondPath, "latest completed request publishes its path");
		this.prFinishRead(0);
		this.assertEquals(deck.path, secondPath,
			"CQ-AUD-005: stale late completion cannot replace the latest request");
		this.assert(deck.readRequests[0][\buffer].freed,
			"stale request frees its locally owned buffer");
	}

	test_latestRequestWinsWhenItCompletesLast {
		if(this.prRequireContract.not) { ^this };
		deck.load(firstPath);
		deck.load(secondPath);
		this.prWaitForRequests(2);
		this.prFinishRead(0);
		this.assert(deck.path.isNil, "superseded completion is never published");
		this.prFinishRead(1);
		this.assertEquals(deck.path, secondPath, "latest request publishes when ready");
	}

	test_freeDuringReadCannotResurrectDeck {
		if(this.prRequireContract.not) { ^this };
		deck.load(firstPath);
		this.prWaitForRequests(1);
		deck.free;
		this.prFinishRead(0);
		this.assert(deck.path.isNil and: { deck.isLoaded.not },
			"CQ-AUD-005: free invalidates an in-flight load permanently");
		this.assert(deck.readRequests[0][\buffer].freed,
			"free-invalidated read releases its buffer on completion");
	}

	test_readFailureEntersOwnedErrorState {
		if(this.prRequireContract.not) { ^this };
		deck.load(firstPath);
		this.prWaitForRequests(1);
		deck.failRead(0);
		0.01.wait;
		this.assertEquals(deck.loadState, \error, "read failure is explicit and contained");
		this.assert(deck.error.notNil, "error state retains a displayable reason");
	}
}
