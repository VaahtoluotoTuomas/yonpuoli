// Runs only in the bounded fresh sclang owned by run_audio_device_probe.ps1.
// It uses standard SuperCollider classes and never boots an audio server.
(
var outputPath = "CQ_AUDIO_PROBE_OUTPUT".getenv;
var result;

try {
	if(outputPath.isNil or: { outputPath.isEmpty }) {
		Error("CQ_AUDIO_PROBE_OUTPUT is missing").throw
	};
	result = (
		inputs: ServerOptions.inDevices ? [],
		outputs: ServerOptions.outDevices ? []
	);
	File.use(outputPath, "w", { |file|
		file.write(result.asCompileString ++ Char.nl)
	});
	"CQ_AUDIO_DEVICE_PROBE_OK".postln;
	0.exit;
} { |error|
	error.reportError;
	"CQ_AUDIO_DEVICE_PROBE_FAILED".postln;
	1.exit;
};
)
