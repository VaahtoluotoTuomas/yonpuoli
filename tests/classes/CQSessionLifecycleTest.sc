// CQ-AUD-003/004 reproducers. Server.default is replaced by a hermetic callback
// double: no scsynth, GUI, MIDI device or hardware output is involved.

CQSessionLifecycleTest : UnitTest {
	var server, previousServer, configPath, tmpDir;

	setUp {
		previousServer = Server.default;
		server = CQTestServer();
		Server.default = server;
		tmpDir = this.class.filenameSymbol.asString.dirname.dirname
			+/+ "tmp" +/+ "cqsession_lifecycle_test";
		if(File.exists(tmpDir).not) { File.mkdir(tmpDir) };
		configPath = tmpDir +/+ "config.scd";
		File.use(configPath, "w", { |file|
			file.write("(headless: true, sixteenN: (enabled: false))");
		});
	}

	tearDown {
		Cocoquantus.free;
		100.do {
			if(Cocoquantus.lifecycleState == \stopped) { nil } { 0.001.wait };
		};
		Server.default = previousServer;
		if(File.exists(configPath)) { File.delete(configPath) };
	}

	prWaitUntilFreed {
		var spins = 0;
		while { (Cocoquantus.lifecycleState != \stopped) and: { spins < 1000 } } {
			0.001.wait;
			spins = spins + 1;
		};
		this.assertEquals(Cocoquantus.lifecycleState, \stopped,
			"session teardown completed");
		this.assert(Cocoquantus.graph.isNil, "stopped session owns no graph");
	}

	test_bootThenFreeInvalidatesLateCallback {
		var ready = false;
		Cocoquantus.boot(configPath, { ready = true });
		Cocoquantus.free;
		this.prWaitUntilFreed;
		this.assertNoException({ server.runNextBoot },
			"CQ-AUD-004: a stale waitForBoot callback is cancelled after free");
		this.assert(ready.not, "cancelled boot never calls onReady");
		this.assert(Cocoquantus.graph.isNil,
			"late callback cannot resurrect or partially rebuild the session");
	}

	test_bootFailureRollsBackToFailedState {
		if(Cocoquantus.respondsTo(\lifecycleState).not) {
			this.assert(false,
				"CQ-AUD-004: facade exposes stopped/booting/running/stopping/failed state");
			^this
		};
		Cocoquantus.boot(configPath);
		this.assertEquals(Cocoquantus.lifecycleState, \booting,
			"accepted boot publishes booting before async work starts");
		this.assertNoException({ server.runNextBoot },
			"boot-stage failure is contained by lifecycle rollback");
		this.assertEquals(Cocoquantus.lifecycleState, \failed,
			"boot-stage failure reaches explicit failed state");
		this.assert(Cocoquantus.graph.isNil,
			"failed boot rolls back its partially owned graph");
	}

	test_rebootInvokesActualServerReboot {
		Cocoquantus.boot(configPath);
		Cocoquantus.reboot;
		1.0.wait;
		this.assertEquals(server.rebootCount, 1,
			"CQ-AUD-003: CQ.reboot invokes Server.reboot, not only graph free/boot");
	}

	test_preparedAudioRebootUsesCapturedConfigAndReturnsOneFailure {
		var ready = 0, failures = 0, failureText;
		var prepared = CQConfig.validate((
			headless: true,
			sixteenN: (enabled: false),
			sampleRate: 96000,
			outChans: [0, 1]
		));
		Cocoquantus.prRebootPrepared(server, configPath, prepared,
			{ ready = ready + 1 },
			{ |failure|
				failures = failures + 1;
				failureText = failure.errorString
			});
		this.assertEquals(server.rebootCount, 1,
			"prepared Audio candidate crosses the real facade reboot boundary once");
		server.runServerBoot;
		this.assertEquals(Cocoquantus.config[\sampleRate], 96000,
			"post-reboot boot uses captured data rather than rereading the profile");
		server.runNextBoot;
		this.assertEquals(failures, 1,
			"candidate graph failure returns exactly one facade failure callback");
		this.assertEquals(ready, 0,
			"failed candidate cannot publish ready");
		this.assert(failureText.notNil
			and: { Cocoquantus.lifecycleState == \failed },
			"facade failure leaves an exact diagnostic and failed lifecycle");
	}

	test_audioResultRequestsOneSymbolicAudioReopen {
		Cocoquantus.prPublishAudioReconfigure((
			status: \rollbackReady,
			error: "candidate failed",
			candidateError: "candidate failed",
			rollbackError: nil,
			attemptCount: 2
		));
		this.assert(Cocoquantus.audioReopenRequested,
			"rollback-ready data requests the symbolic Audio page");
		this.assert(Cocoquantus.consumeAudioReopen,
			"the rebuilt GUI consumes the reopen request once");
		this.assert(Cocoquantus.consumeAudioReopen.not,
			"no stale page-open callback survives after consumption");
	}
}
