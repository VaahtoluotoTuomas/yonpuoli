// CQ-AUD-002 reproducer: the recorder must own one cancellable attempt. The fake
// server exposes each sync barrier, making double-start/immediate-stop deterministic.

CQRecorderLifecycleTest : UnitTest {
	var server, graph, recorder, tmpDir, config;

	setUp {
		server = CQTestServer();
		graph = CQTestGraph(server);
		tmpDir = this.class.filenameSymbol.asString.dirname.dirname
			+/+ "tmp" +/+ "cqrec_lifecycle_test";
		if(File.exists(tmpDir).not) { File.mkdir(tmpDir) };
		config = (outChans: [0, 1], recStems: false);
		recorder = CQRecorder(graph, config, tmpDir);
		CQRecorder.postWarnings = false;
	}

	tearDown {
		recorder !? { recorder.free };
		Buffer.clearServerCaches(server);
		(tmpDir +/+ "*").pathMatch.do { |p| File.delete(p) };
		CQRecorder.postWarnings = true;
	}

	prWaitForPendingSync {
		var spins = 0;
		while { (server.pendingSyncCount < 1) and: { spins < 1000 } } {
			0.001.wait;
			spins = spins + 1;
		};
		this.assert(server.pendingSyncCount > 0,
			"fake server reached the expected explicit sync barrier");
	}

	prDrainTransition {
		var spins = 0, transitioning;
		if(recorder.respondsTo(\state).not) {
			2.do {
				this.prWaitForPendingSync;
				server.releasePendingSyncs;
			};
			0.01.wait;
			^this
		};
		transitioning = { #[starting, stopping].includes(recorder.state) };
		while { transitioning.value and: { spins < 2000 } } {
			if(server.pendingSyncCount > 0) { server.releasePendingSyncs };
			0.001.wait;
			spins = spins + 1;
		};
		this.assert(transitioning.value.not,
			"recorder transition completes through explicit fake-server barriers");
	}

	prMessages { |command|
		^server.messages.select { |msg| msg[0] == command }
	}

	test_stateContractExists {
		if(recorder.respondsTo(\state).not) {
			this.assert(false,
				"CQ-AUD-002: recorder exposes idle/starting/recording/stopping/error state");
			^this
		};
		this.assertEquals(recorder.state, \idle, "recorder begins idle");
	}

	test_doubleStartOwnsExactlyOneNode {
		var made, freed;
		recorder.start;
		recorder.start;
		this.prDrainTransition;
		made = this.prMessages(9).collect { |msg| msg[2] };
		this.assertEquals(made.size, 1,
			"CQ-AUD-002: double-start creates exactly one DiskOut synth");
		recorder.stop;
		if(recorder.respondsTo(\state)) { this.prDrainTransition };
		freed = this.prMessages(11).collect { |msg| msg[1] };
		this.assertEquals(made.reject { |id| freed.includes(id) }.size, 0,
			"CQ-AUD-002: one stop leaves no orphan recorder node");
	}

	test_immediateStopCannotBeOvertakenByStart {
		recorder.start;
		recorder.stop;
		this.prDrainTransition;
		this.assert(recorder.isRecording.not,
			"CQ-AUD-002: stop during starting prevents later recording resurrection");
	}

	test_asyncFailureEntersErrorAndNextPressAcknowledges {
		if(recorder.respondsTo(\state).not) {
			this.assert(false, "CQ-AUD-002: recorder exposes an error state");
			^this
		};
		server.failSyncAt = 1;
		recorder.start;
		100.do {
			if(recorder.state == \error) { nil } { 0.001.wait };
		};
		this.assertEquals(recorder.state, \error,
			"async allocation/open failure enters error state");
		this.assert(recorder.error.notNil, "error state retains a displayable reason");
		recorder.start;
		this.assertEquals(recorder.state, \idle,
			"the next REC press acknowledges error without starting another attempt");
	}
}
