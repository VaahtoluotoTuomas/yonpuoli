// CocoquantusSC configuration template (SPEC §7).
// Copy to config.scd (gitignored) and edit for your machine.
(
	sampleRate: 48000,          // 44100 / 48000 / 96000; coco clock is capped at SR
	blockSize: 64,
	device: nil,                // "ASIO : Fireface" | nil = system default |
	                            // [inDev, outDev] pair (same host API), e.g. WASAPI
	                            // laptop mic in + headphones out; list: ServerOptions.devices
	hardwareBufferSize: 256,    // latency vs. stability for live modular processing
	inChans: [0, 1],            // live stereo pair from modular/mixer
	micChan: nil,               // nil = mic path disabled entirely
	outChans: [0, 1],
	stemOuts: nil,              // e.g. [2, 3] -> coco1, coco2 post-safety
	bufFrames: 131072,          // Coco RAM, 2^16..2^19 (Phasor float32 precision limit)
	sixteenN: (enabled: true, ccBase: 32, chan: 0, takeover: \jump),
	folders: (samples: "samples", recordings: "recordings", presets: "presets"),
	autoTrim: true,             // watchdog -6 dB trim on sustained hot output
	headless: false             // true = no GUI (instrument stays fully playable)
)
