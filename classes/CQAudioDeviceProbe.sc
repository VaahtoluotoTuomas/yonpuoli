// CQAudioDeviceProbe — fresh-process audio discovery for Windows hot-plug.
//
// SuperCollider's Windows device primitive lives in sclang's PortAudio process.
// A stopped scsynth is necessary but not sufficient after an endpoint or driver
// appeared later in the same language session. The production Windows path
// therefore runs the tiny support/audio_device_probe.scd in a bounded, fresh
// sclang process, then loads its exact Event result from a private temp file.
// Other platforms retain the ordinary stopped-server in-process enumeration.

CQAudioDeviceProbe {
	classvar <>probeRunner;

	*timeoutSeconds { ^20 }

	*runnerPath {
		^Cocoquantus.repoDir +/+ "support" +/+ "run_audio_device_probe.ps1"
	}

	*probePath {
		^Cocoquantus.repoDir +/+ "support" +/+ "audio_device_probe.scd"
	}

	*sclangPath {
		var path;
		if(thisProcess.platform.name != \windows) { ^nil };
		path = Platform.resourceDir +/+ "sclang.exe";
		^if(File.exists(path)) { path } { nil }
	}

	*outputPath {
		var serial = (Main.elapsedTime * 1000).asInteger;
		^Platform.defaultTempDir +/+
			"cq-audio-devices-%-%-%.scd".format(
				thisProcess.pid, serial, 1000000000.rand)
	}

	*settledPasses { ^3 }
	*settleMilliseconds { ^750 }

	*commandFor { |outputPath, sclangPath, passes = 1|
		var command = "powershell.exe -NoProfile -NonInteractive "
			++ "-ExecutionPolicy Bypass -File " ++ this.runnerPath.quote
			++ " -OutputPath " ++ outputPath.asString.quote
			++ " -TimeoutSeconds " ++ this.timeoutSeconds.asString
			++ " -Passes " ++ passes.asInteger.max(1).asString
			++ " -SettleMilliseconds " ++ this.settleMilliseconds.asString;
		sclangPath !? {
			command = command ++ " -Sclang " ++ sclangPath.asString.quote
		};
		^command
	}

	*scan { |passes = 1|
		var runner = probeRunner, outputPath, command, code, result;
		if(thisProcess.platform.name != \windows) {
			^CQAudioDeviceCatalog.scan
		};
		outputPath = this.outputPath;
		command = this.commandFor(outputPath, this.sclangPath, passes);
		if(runner.notNil) {
			^CQAudioDeviceCatalog.normalize(
				runner.value(command, outputPath, this.probePath))
		};
		protect {
			code = command.systemCmd;
			if(code != 0) {
				Error("fresh audio-device probe failed (exit %)".format(code)).throw
			};
			if(File.exists(outputPath).not) {
				Error("fresh audio-device probe returned no result").throw
			};
			result = outputPath.load;
			if(result.isKindOf(Event).not) {
				Error("fresh audio-device probe returned invalid data").throw
			};
			result = CQAudioDeviceCatalog.normalize(result);
		} {
			if(File.exists(outputPath)) { File.delete(outputPath) };
		};
		^result
	}

	// A player-triggered hot-plug scan intentionally costs a few extra seconds:
	// Windows can publish the jack endpoint after the first fresh PortAudio
	// process has already enumerated. Each pass is a new bounded interpreter and
	// the final pass is the truth used by the rebuilt GUI. Ordinary startup keeps
	// the single-pass path above.
	*scanSettled { ^this.scan(this.settledPasses) }
}
