// CQ-AUD-003/004 reproducers. Server.default is replaced by a hermetic callback
// double: no scsynth, GUI, MIDI device or hardware output is involved.

CQSessionLifecycleTest : UnitTest {
	var server, previousServer, configPath, profilePath, tmpDir;

	setUp {
		previousServer = Server.default;
		server = CQTestServer();
		Server.default = server;
		Cocoquantus.prResetAudioDeviceCatalog;
		tmpDir = this.class.filenameSymbol.asString.dirname.dirname
			+/+ "tmp" +/+ "cqsession_lifecycle_test";
		if(File.exists(tmpDir).not) { File.mkdir(tmpDir) };
		configPath = tmpDir +/+ "config.scd";
		profilePath = CQAudioConfig.profilePathFor(configPath);
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
		[configPath, profilePath, profilePath ++ ".pending",
			profilePath ++ ".bak"].do { |path|
			if(File.exists(path)) { File.delete(path) }
		};
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

	test_rebootStopsAndBootsTheActualServer {
		Cocoquantus.boot(configPath);
		Cocoquantus.reboot;
		1.0.wait;
		this.assert(server.quitCount == 1 and: { server.bootCount == 1 }
			and: { server.rebootCount == 0 },
			"CQ-AUD-003: startup-aware CQ.reboot stops, scans, then boots the server");
	}

	test_audioClockHealthRejectsResponsiveCatastrophicDrift {
		this.assert(
			Cocoquantus.prAudioClockHealthy(48000, 47986.317),
			"ordinary hardware-clock drift passes the Audio readiness gate");
		this.assert(
			Cocoquantus.prAudioClockHealthy(48000, 48003.479),
			"the measured recovered split-WASAPI clock passes");
		this.assert(
			Cocoquantus.prAudioClockHealthy(48000, 2610.783).not,
			"the measured responsive system-default clock is rejected");
		this.assert(
			Cocoquantus.prAudioClockHealthy(48000, nil).not,
			"missing clock truth cannot publish an Audio candidate");
		this.assert(
			[47986.317, 48003.479, 47986.317].every { |rate|
				Cocoquantus.prAudioClockHealthy(48000, rate)
			},
			"three recovered split-WASAPI readings all pass");
		this.assert(
			[48000, 7076.4, 7019.8].every { |rate|
				Cocoquantus.prAudioClockHealthy(48000, rate)
			}.not,
			"a stale healthy first reading cannot hide later clock collapse");
		server.actualSampleRate = 2610.783;
		this.assertException({
			Cocoquantus.prRequireAudioClock(server, 48000)
		}, Error,
			"catastrophic drift becomes a facade failure before graph allocation");
	}

	test_initialProfileClockFailureRetriesBaseExactlyOnce {
		var failures = 0, failureText;
		var baseDevice = [
			"Windows WASAPI : Microphone Array",
			"Windows WASAPI : Headphones"
		];
		File.use(configPath, "w", { |file|
			file.write((
				headless: true,
				sixteenN: (enabled: false),
				device: baseDevice
			).asCompileString)
		});
		File.use(profilePath, "w", { |file|
			file.write((device: "ASIO : K-MixUSBAudio ASIO Driver")
				.asCompileString)
		});
		server.actualSampleRateReadings = [8000, 8000, 8000];
		Cocoquantus.boot(configPath, nil, nil, { |failure|
			failures = failures + 1;
			failureText = failure.errorString
		});

		server.runNextBoot;
		this.assertEquals(server.rebootCount, 1,
			"an enumerable profile with a catastrophic clock restarts once");
		this.assertEquals(failures, 0,
			"the candidate failure is not terminal before the base attempt");
		this.assertEquals(Cocoquantus.audioReconfigureState, \rollback,
			"startup publishes the same explicit rollback truth as Audio apply");
		this.assert(Cocoquantus.audioBootRecovery.candidateError
			.find("audio clock unhealthy").notNil,
			"the recovery retains the measured clock diagnosis");

		server.runServerBoot;
		0.02.wait;
		this.assertEquals(Cocoquantus.config[\device], baseDevice,
			"the second boot uses config.scd data, never the rejected profile");
		server.runNextBoot;
		this.assertEquals(server.rebootCount, 1,
			"a failed base route cannot recurse into a reboot loop");
		this.assertEquals(failures, 1,
			"two failed routes produce one final caller failure");
		this.assert(failureText.find("saved profile:").notNil
			and: { failureText.find("base fallback:").notNil },
			"the terminal diagnostic retains both route failures");
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

	test_startupAwareRebootSkipsMissingProfileBeforeServerLaunch {
		var failures = 0, scanRunning;
		var baseDevice = [
			"Windows WASAPI : Microphone Array",
			"Windows WASAPI : Speakers"
		];
		var base = CQConfig.validate((
			headless: true,
			sixteenN: (enabled: false),
			device: baseDevice
		));
		var candidate = CQAudioConfig.merge(base, (
			device: "ASIO : K-MixUSBAudio ASIO Driver"
		));
		var recovery = CQAudioBootRecovery(
			base, candidate, profilePath);
		Cocoquantus.prRebootPrepared(server, configPath, candidate, nil, {
			failures = failures + 1
		}, {
			scanRunning = server.applicationRunning or: {
				server.serverRunning
			};
			(inputs: [baseDevice[0]], outputs: [baseDevice[1]])
		}, nil, recovery);

		this.assert(server.quitCount == 1 and: { server.bootCount == 1 }
			and: { server.rebootCount == 0 },
			"startup recovery scans between a real stop and the replacement boot");
		this.assert(scanRunning.not,
			"the K-Mix absence is observed only while endpoints are released");
		this.assertEquals(server.options.inDevice, baseDevice[0],
			"the replacement server receives the present base input");
		this.assertEquals(server.options.outDevice, baseDevice[1],
			"the replacement server never receives the absent K-Mix output");
		this.assert(recovery.fallbackActive
			and: { recovery.attemptCount == 1 }
			and: { failures == 0 },
			"catalog preselection records one base attempt and no candidate failure");
	}

	test_startupAwareRebootDoesNotLaunchWhenEveryRouteIsMissing {
		var failures = 0, failureText;
		var base = CQConfig.validate((
			headless: true,
			sixteenN: (enabled: false),
			device: ["missing base input", "missing base output"]
		));
		var candidate = CQAudioConfig.merge(base, (
			device: "ASIO : missing candidate"
		));
		var recovery = CQAudioBootRecovery(
			base, candidate, profilePath);
		Cocoquantus.prRebootPrepared(server, configPath, candidate, nil, {
			|failure|
			failures = failures + 1;
			failureText = failure.errorString
		}, { (inputs: [], outputs: []) }, nil, recovery);

		this.assert(server.quitCount == 1 and: { server.bootCount == 0 },
			"known-missing routes leave the stopped server stopped");
		this.assert(recovery.preflightFailed and: { failures == 1 },
			"the stopped scan produces one terminal preflight callback");
		this.assert(failureText.find("saved profile:").notNil
			and: { failureText.find("base fallback:").notNil },
			"the no-launch diagnosis retains both missing routes");
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
		0.02.wait;
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

	test_preparedAudioRebootRejectsResponsiveUnhealthyClock {
		var ready = 0, failures = 0, failureText;
		var prepared = CQConfig.validate((
			headless: true,
			sixteenN: (enabled: false),
			sampleRate: 48000,
			outChans: [0, 1]
		));
		server.actualSampleRateReadings = [48000, 7076.4, 7019.8];
		Cocoquantus.prRebootPrepared(server, configPath, prepared,
			{ ready = ready + 1 },
			{ |failure|
				failures = failures + 1;
				failureText = failure.errorString
			});
		server.runServerBoot;
		0.02.wait;
		server.runNextBoot;
		this.assertEquals(ready, 0,
			"a responsive catastrophic clock cannot publish candidate ready");
		this.assertEquals(failures, 1,
			"the unhealthy candidate returns exactly one facade failure");
		this.assert(failureText.notNil and: {
			failureText.find("audio clock unhealthy").notNil
		} and: {
			Cocoquantus.lifecycleState == \failed
		}, "stale-then-collapsed clock readings retain an actionable diagnostic");
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
			"an unresponsive server is explicitly stopped");
		this.assertEquals(server.rebootCount, 0,
			"the stock reboot path cannot boot over an unresponsive occupied port");
		this.assertEquals(server.bootCount, 1,
			"a fresh server boot follows the forced stop");
		this.assert(reads == 1 and: { scanRunning.not },
			"forced-quit recovery scans once after process and watcher teardown");
	}

	// ---- SOAK-1 finding C: the port is the evidence ----

	// The operating system releases the socket, not the kill command, so the
	// fixture only frees the port after taskkill has actually run — which is what
	// makes the poll's post-condition mean something.
	prMeasuredHolder { |image = "scsynth.exe"|
		var stopped = false;
		^{ |cmd|
			case
			{ cmd.contains("netstat") } {
				if(stopped) {
					"  UDP     0.0.0.0:57120          *:*                    2696\r\n"
				} {
					"  UDP     127.0.0.1:59999        *:*                    17716\r\n"
				}
			}
			{ cmd.contains("tasklist") } {
				"\"%\",\"17716\",\"Console\",\"1\",\"92,480 K\"\r\n".format(image)
			}
			{ cmd.contains("taskkill") } {
				stopped = true;
				"SUCCESS: The process with PID 17716 has been terminated.\r\n"
			}
			{ true } { "" }
		}
	}

	// Measured 2026-07-31: the restart force-killed the pid the library recorded
	// (6012) while the real server, 17716, kept its port and every later boot had
	// nowhere to bind. The instrument never came back.
	test_forcedRestartStopsTheProcessThatHoldsThePort {
		var commands = List.new, previous = CQServerProcess.commandRunner;
		var runner = this.prMeasuredHolder;
		var prepared = CQConfig.validate((headless: true, sixteenN: (enabled: false)));
		CQServerProcess.commandRunner = { |cmd| commands.add(cmd); runner.value(cmd) };
		server.unresponsive = true;
		Cocoquantus.prRebootPrepared(server, configPath, prepared, nil, nil, {
			(inputs: ["input"], outputs: ["output"])
		});
		0.25.wait;
		CQServerProcess.commandRunner = previous;
		this.assert(commands.any { |c|
			c.contains("taskkill") and: { c.contains("17716") }
		}, "the holder of the port is what gets stopped");
		this.assertEquals(server.bootCount, 1,
			"and the boot follows once the socket is actually free");
	}

	test_forcedRestartRefusesWhenThePortIsHeldByAStranger {
		var commands = List.new, previous = CQServerProcess.commandRunner;
		var runner = this.prMeasuredHolder("chrome.exe");
		var failure, prepared;
		prepared = CQConfig.validate((headless: true, sixteenN: (enabled: false)));
		CQServerProcess.commandRunner = { |cmd| commands.add(cmd); runner.value(cmd) };
		server.unresponsive = true;
		Cocoquantus.prRebootPrepared(server, configPath, prepared, nil,
			{ |err| failure = err }, {
				(inputs: ["input"], outputs: ["output"])
			});
		0.25.wait;
		CQServerProcess.commandRunner = previous;
		this.assert(commands.any { |c| c.contains("taskkill") }.not,
			"a pid that is not a server is never force-killed, however stuck we are");
		this.assertEquals(server.bootCount, 0,
			"and no boot is attempted into a port somebody else owns");
		this.assert(failure.notNil, "the restart fails out loud");
		this.assert(failure.errorString.contains("chrome.exe"),
			"naming the holder, because 'restart failed' alone is the old surface");
		this.assertEquals(Cocoquantus.lifecycleState, \failed,
			"the session says so too");
	}

	// A double must not be able to reach the operating system: the port it names
	// could be the player's live server, and a unit test would stop it.
	test_aDoubleWithoutAFixtureNeverAsksTheOperatingSystem {
		var clearance;
		CQServerProcess.commandRunner = nil;
		clearance = Cocoquantus.prClearServerPort(server);
		this.assertEquals(clearance[\status], \free,
			"no fixture, no query — the OS is reached only for a real Server");
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
