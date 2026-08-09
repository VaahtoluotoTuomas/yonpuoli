// Fresh-process device discovery stays hermetic here: the injected runner never
// starts sclang, touches PortAudio or writes a temp file.

CQAudioDeviceProbeTest : UnitTest {
	var previousRunner;

	setUp {
		previousRunner = CQAudioDeviceProbe.probeRunner;
		Cocoquantus.prResetAudioDeviceCatalog;
	}

	tearDown {
		CQAudioDeviceProbe.probeRunner = previousRunner;
		Cocoquantus.prResetAudioDeviceCatalog;
	}

	test_windowsCommandNamesTheBoundedProbeAndPrivateOutput {
		var output = "C:/Temp/cq audio devices.scd";
		var command = CQAudioDeviceProbe.commandFor(output,
			"C:/Program Files/SuperCollider/sclang.exe");
		this.assert(command.contains("run_audio_device_probe.ps1"),
			"the production command crosses the watchdog wrapper");
		this.assert(command.contains("-OutputPath") and: {
			command.contains("cq audio devices.scd")
		}, "the helper writes only the caller-owned private result path");
		this.assert(command.contains("-TimeoutSeconds 20"),
			"the fresh interpreter has an explicit bounded lifetime");
		this.assert(command.contains("-Passes 1") and: {
			command.contains("-SettleMilliseconds 750")
		}, "ordinary startup requests one bounded probe pass");
		this.assert(command.contains("sclang.exe"),
			"the active SuperCollider installation is passed explicitly when known");
	}

	test_playerDiscoveryRequestsASettledFreshProcessSeries {
		var calls = 0, observedCommand, result;
		CQAudioDeviceProbe.probeRunner = { |command|
			calls = calls + 1;
			observedCommand = command;
			(inputs: ["Windows WASAPI : Microphone"],
				outputs: ["Windows WASAPI : Headphones"])
		};
		result = CQAudioDeviceProbe.scanSettled;
		this.assertEquals(calls, 1,
			"one watchdog owns the complete settled discovery series");
		this.assert(observedCommand.contains("-Passes 3"),
			"explicit hot-plug discovery asks for three fresh PortAudio passes");
		this.assertEquals(result[\outputs],
			["Windows WASAPI : Headphones"],
			"the final settled output remains exact");
	}

	test_injectedFreshProbePreservesExactDirectionalNames {
		var calls = 0, observedCommand, result;
		CQAudioDeviceProbe.probeRunner = { |command, outputPath, probePath|
			calls = calls + 1;
			observedCommand = command;
			this.assert(outputPath.endsWith(".scd"),
				"the injected boundary receives a private compile-string path");
			this.assert(probePath.endsWith("audio_device_probe.scd"),
				"the bounded child evaluates only the tracked probe script");
			(
				inputs: ["Windows WASAPI : Microphone", "ASIO : Öljyjärvi"],
				outputs: ["Windows WASAPI : Headphones", "ASIO : Öljyjärvi"]
			)
		};
		result = CQAudioDeviceProbe.scan;
		this.assertEquals(calls, 1,
			"one discovery request launches exactly one fresh provider");
		this.assert(observedCommand.contains("powershell.exe"),
			"Windows discovery is owned by the bounded process wrapper");
		this.assertEquals(result[\inputs][0], "Windows WASAPI : Microphone",
			"input endpoint spelling remains exact");
		this.assertEquals(result[\outputs][0], "Windows WASAPI : Headphones",
			"output endpoint spelling remains exact");
		this.assertEquals(result[\all].size, 3,
			"normalization exact-deduplicates the bidirectional ASIO name");
	}

	test_facadeProductionCaptureDelegatesToTheFreshProbe {
		var calls = 0, catalog;
		var stoppedServer = Server(\cqFreshProbeTest,
			NetAddr("127.0.0.1", 59997));
		CQAudioDeviceProbe.probeRunner = {
			calls = calls + 1;
			(inputs: ["fresh mic"], outputs: ["fresh headphones"])
		};
		this.assert(
			Cocoquantus.prCaptureAudioDeviceCatalog(stoppedServer),
			"the real facade path accepts a confirmed stopped Server");
		catalog = Cocoquantus.audioDeviceCatalogForGui(
			["fresh mic", "fresh headphones"]);
		this.assertEquals(calls, 1,
			"production capture delegates exactly once to CQAudioDeviceProbe");
		this.assertEquals(catalog[\source], \scanned,
			"fresh helper data receives stopped-scan provenance");
		this.assertEquals(catalog[\outputs], ["fresh headphones"],
			"the facade publishes the exact fresh output name from memory");
	}

	test_facadeSettledCaptureUsesTheHotPlugProbeMode {
		var command, stoppedServer = Server(\cqSettledProbeTest,
			NetAddr("127.0.0.1", 59996));
		CQAudioDeviceProbe.probeRunner = { |value|
			command = value;
			(inputs: ["fresh mic"], outputs: ["fresh headphones"])
		};
		this.assert(Cocoquantus.prCaptureAudioDeviceCatalog(
			stoppedServer, nil, true),
			"explicit discovery accepts the settled production capture");
		this.assert(command.contains("-Passes 3"),
			"the facade distinguishes player discovery from one-pass startup");
	}
}
