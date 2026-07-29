// Yölahti configuration template (SPEC §7).
// Copy to config.scd (gitignored) and edit for your machine.
(
	sampleRate: 48000,          // 44100 / 48000 / 96000; coco clock is capped at SR
	blockSize: 64,
	device: nil,                // "ASIO : Fireface" | nil = system default |
	                            // [inDev, outDev] pair (same host API), e.g. WASAPI
	                            // laptop mic in + headphones out. Drawer's Audio
	                            // page lists exact inDevices/outDevices and writes
	                            // only a generated sibling config.audio.scd overlay.
	hardwareBufferSize: 256,    // latency vs. stability for live modular processing
	inChans: [0, 1],            // live stereo pair from modular/mixer
	micChan: nil,               // nil = mic path disabled entirely
	outChans: [0, 1],           // main stereo; indices must be unique
	stemOuts: nil,              // e.g. [2, 3] -> unique + disjoint from outChans
	bufFrames: 131072,          // Coco RAM, 2^16..2^19 (Phasor float32 precision limit)
	sixteenN: (enabled: true, ccBase: 32, chan: nil, takeover: \jump),
	                            // chan nil = any MIDI channel (atovproject 16n
	                            // sends each fader on its own channel); 0..15 pins one
	folders: (samples: "samples", recordings: "recordings",
		presets: "presets", snapshots: "snapshots"),
	autoTrim: false,            // watchdog: notice always shows; true = also auto-trim
	                            // -6 dB on sustained hard driving (comfort, not safety —
	                            // the limiter is the safety). Opt-in.
	recStems: false,            // true = CQ.rec writes a 4-ch file (master L/R +
	                            // post-safety coco1 + coco2 stems) instead of stereo
	headless: false,            // true = no GUI (instrument stays fully playable)
	// gui: (compassDir: nil)   // optional GUI sub-event (GUI_SPEC §10). compassDir
	                            // overrides where the §7.1 weather-machine memories
	                            // (compass.cqcompass.scd) live; nil/absent = presets folder
)
