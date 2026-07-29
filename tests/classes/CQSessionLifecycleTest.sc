// CQ-AUD-003/004 reproducers. Server.default is replaced by a hermetic callback
// double: no scsynth, GUI, MIDI device or hardware output is involved.

CQSessionLifecycleTest : UnitTest {
	var server, previousServer, configPath, tmpDir;

	setUp {
		previousServer = Server.default;
		server = CQTestServer();
		Server.default = server;
		Cocoquantus.prResetAudioDeviceCatalog;
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
		Cocoquantus.prResetAudioDeviceCatalog;
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

	test_deviceCaptureRefusesA_RUNNING_Server {
		var reads = 0;
		var accepted = Cocoquantus.prCaptureAudioDeviceCatalog(server, {
			reads = reads + 1;
			(inputs: ["input"], outputs: ["output"])
		});
		this.assert(accepted.not and: { reads == 0 },
			"AIO-5 never invokes its provider while the server owns endpoints");
		this.assertEquals(Cocoquantus.audioDeviceScanCount, 0,
			"a refused running scan does not increment the read count");
	}

	test_stoppedDeviceCaptureCachesExactDirections {
		var reads = 0, runningDuringScan;
		var catalog;
		var has = { |items, text|
			items.any { |item| item.asString == text.asString }
		};
		server.processRunning = false;
		this.assert(Cocoquantus.prCaptureAudioDeviceCatalog(server, {
			reads = reads + 1;
			runningDuringScan =
				server.applicationRunning or: { server.serverRunning };
			(
				inputs: ["Windows WASAPI : Mic", "ASIO : Duplex"],
				outputs: ["Windows WASAPI : Phones", "ASIO : Duplex"]
			)
		}), "a stopped server accepts one directional capture");
		catalog = Cocoquantus.audioDeviceCatalogForGui(
			["Windows WASAPI : Mic", "Windows WASAPI : Phones"]);
		this.assert(reads == 1 and: { runningDuringScan.not }
			and: { Cocoquantus.audioDeviceScanCount == 1 },
			"the provider runs once and observes a stopped boundary");
		this.assertEquals(catalog[\source], \scanned,
			"the runtime page receives explicit stopped-snapshot provenance");
		this.assert(has.value(catalog[\inputs], "Windows WASAPI : Mic")
			and: { has.value(
				catalog[\outputs], "Windows WASAPI : Phones") },
			"the memory cache preserves exact directional endpoints");
	}

	test_deviceScanFailureKeepsTheLastCacheAndDoesNotThrow {
		var catalog;
		var has = { |items, text|
			items.any { |item| item.asString == text.asString }
		};
		server.processRunning = false;
		Cocoquantus.prCaptureAudioDeviceCatalog(server, {
			(inputs: ["cached input"], outputs: ["cached output"])
		});
		this.assert(Cocoquantus.prCaptureAudioDeviceCatalog(server, {
			Error("injected device scan failure").throw
		}).not, "device discovery failure returns false instead of aborting audio boot");
		catalog = Cocoquantus.audioDeviceCatalogForGui(
			["configured input", "configured output"]);
		this.assertEquals(catalog[\source], \stale,
			"a failed refresh marks the previous successful memory cache stale");
		this.assert(has.value(catalog[\inputs], "cached input")
			and: { has.value(catalog[\outputs], "cached output") }
			and: { has.value(catalog[\inputs], "configured input") }
			and: { has.value(catalog[\outputs], "configured output") },
			"stale cache and exact configured fallback are both retained");
		this.assert(catalog[\error].find("injected device scan failure").notNil,
			"the runtime page receives an exact nonfatal scan diagnostic");
	}

	test_preparedRebootCapturesInsideServerOffCallback {
		var scanRunning, reads = 0;
		var prepared = CQConfig.validate((
			headless: true,
			sixteenN: (enabled: false)
		));
		Cocoquantus.prRebootPrepared(server, configPath, prepared, nil, nil, {
			reads = reads + 1;
			scanRunning =
				server.applicationRunning or: { server.serverRunning };
			(inputs: ["input"], outputs: ["output"])
		});
		this.assertEquals(server.rebootCount, 1,
			"responsive AIO-5 capture reuses the one real reboot boundary");
		this.assert(reads == 1 and: { scanRunning.not },
			"responsive reboot invokes discovery only in its server-off callback");
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

	test_unresponsivePreparedRebootForcesQuitBeforeBoot {
		var reads = 0, scanRunning;
		var prepared = CQConfig.validate((
			headless: true,
			sixteenN: (enabled: false)
		));
		server.unresponsive = true;
		Cocoquantus.prRebootPrepared(server, configPath, prepared, nil, nil, {
			reads = reads + 1;
			scanRunning =
				server.applicationRunning or: { server.serverRunning };
			(inputs: ["input"], outputs: ["output"])
		});
		0.15.wait;
		this.assertEquals(server.quitCount, 1,
			"an unresponsive server is explicitly stopped by its owned PID");
		this.assertEquals(server.rebootCount, 0,
			"the stock reboot path cannot boot over an unresponsive occupied port");
		this.assertEquals(server.bootCount, 1,
			"a fresh server boot follows the forced stop");
		this.assert(reads == 1 and: { scanRunning.not },
			"forced-quit recovery scans once after process and watcher teardown");
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
