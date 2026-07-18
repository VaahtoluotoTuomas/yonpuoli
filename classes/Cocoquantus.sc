// Cocoquantus — facade singleton (SPEC §6.3). Owns the boot lifecycle; all
// state flows through CQState, all server objects live in CQGraph.
// CQ (bottom of file) is the short alias — classvars are shared.

Cocoquantus {
	classvar <config, <graph, <state, <matrix, <server;
	classvar configPath, deckAPlayer, deckBPlayer, snapshotMgr, midi16n, recorder;
	classvar guiPanel;
	classvar spesalCache, micWatcher, micWasLive = false;

	*boot { |path, onReady|
		if(this.isBooted) {
			"Cocoquantus: already booted — CQ.free first.".warn;
			^this
		};
		configPath = path ?? { this.repoDir +/+ "config.scd" };
		config = if(File.exists(configPath.asString)) {
			CQConfig.load(configPath)
		} {
			("Cocoquantus: % not found — using defaults "
				++ "(copy config.example.scd to config.scd)").format(configPath).warn;
			CQConfig.validate(())
		};
		server = Server.default;
		graph = CQGraph(server, config);
		graph.applyOptions;
		state = CQState(graph);
		matrix = CQMatrix(graph);
		this.prInitOsc;        // before spawn: catch the boot-time spesal report
		this.prInitMicWatch;
		server.waitForBoot {
			graph.build;                       // buses + groups, synced
			CQSynthDefs.send(config, server);
			server.sync;
			graph.spawn;                       // synths in group order, synced
			state.pushAll;                     // defaults + power-on state
			matrix.pushAll;                    // matrix empty (SPEC boot sequence)
			server.sync;
			deckAPlayer = CQDeck(graph, state, \deckA, this.prSamplesDir);
			deckBPlayer = CQDeck(graph, state, \deckB, this.prSamplesDir);
			snapshotMgr = CQSnapshots(state, matrix, this.prPresetsDir,
				{ this.spesalStates },
				{ |states| states.do { |v, i| this.spesalSet(i + 1, v) } });
			midi16n = CQMidi16n(state, config[\sixteenN]);  // no 16n = notice, not error
			recorder = CQRecorder(graph, config, this.prFolder(\recordings));
			if(config[\headless] != true) { this.gui };  // ARCH §6 step 7; the
			                                   // instrument is complete without it
			graph.fadeIn;                      // silent until verified → 2 s ramp
			CmdPeriod.add(this);
			"Cocoquantus: ready — output fading in over 2 s.".postln;
			onReady.value(this);
		};
	}

	*isBooted {
		^graph.notNil and: { graph.isRunning }
	}

	*set { |name, value|
		if(state.isNil) { "Cocoquantus: not booted.".warn; ^this };
		state.set(name, value);
	}

	*get { |name|
		if(state.isNil) { ^CQSpec.default(name) };
		^state.get(name)
	}

	// coco performance actions (SPEC §6.3): hold/flip toggle when called without
	// a value; both go through the state funnel. skip is a trigger, not a param —
	// it goes straight to the synth (no CQSpec entry, nothing to clamp)
	*hold { |n = 1, val|
		var name = this.prCocoParam(n, "Hold");
		name !? { this.set(name, val ?? { 1 - (this.get(name) ? 0) }) };
	}

	*flip { |n = 1, val|
		var name = this.prCocoParam(n, "Flip");
		name !? { this.set(name, val ?? { 1 - (this.get(name) ? 0) }) };
	}

	*skip { |n = 1|
		var synth;
		if(this.prCocoParam(n, "Hold").isNil) { ^this };
		if(this.isBooted.not) { "Cocoquantus: not booted.".warn; ^this };
		synth = graph.synths[("coco" ++ n).asSymbol];
		synth !? { synth.set(\t_skip, 1) };
	}

	// quantussy spesal (SPEC §5.4): like skip, these are actions, not CQSpec
	// params — the 0..7 counter lives server-side (Stepper in \cqQuantussy,
	// states reported on '/cq_spesal'). spesal(k) steps castle k's partner/mode
	// state; spesalSet forces an absolute state.
	*spesal { |k = 1|
		var synth = this.prCastleSynth(k);
		synth !? { synth.set(("t_step" ++ k).asSymbol, 1) };
	}

	*spesalSet { |k = 1, state = 0|
		var synth = this.prCastleSynth(k);
		if(state.isNumber.not or: { state.asFloat.isNaN or: { state.abs == inf } }) {
			"Cocoquantus: bad spesal state % ignored.".format(state).warn;
			^this
		};
		synth !? {
			synth.set(("state" ++ k).asSymbol,
				state.round.asInteger.mod(8),          // entry-point clamp (safety #4)
				("t_set" ++ k).asSymbol, 1);
		};
	}

	*deckA {
		if(deckAPlayer.isNil) { "Cocoquantus: not booted — no deck A.".warn };
		^deckAPlayer
	}

	*deckB {
		if(deckBPlayer.isNil) { "Cocoquantus: not booted — no deck B.".warn };
		^deckBPlayer
	}

	// SPEC §5.1 linked-pair load: deck A takes L, deck B takes R of a stereo
	// file (a mono file plays on both — CQDeck warns and falls back to ch 0)
	*loadStereo { |filePath|
		if(deckAPlayer.isNil or: { deckBPlayer.isNil }) {
			"Cocoquantus: not booted — no decks.".warn;
			^this
		};
		deckAPlayer.load(filePath, channel: 0);
		deckBPlayer.load(filePath, channel: 1);
	}

	// snapshots (SPEC §6.3): save stores micGain as 0 unless includeMic — see
	// CQSnapshots header for the full mic/panic policy
	*snap { |name, includeMic = false, note = ""|
		if(snapshotMgr.isNil) { "Cocoquantus: not booted.".warn; ^nil };
		^snapshotMgr.save(name, includeMic, note)
	}

	*recall { |name|
		if(snapshotMgr.isNil) { "Cocoquantus: not booted.".warn; ^false };
		^snapshotMgr.recall(name)
	}

	*morph { |from, to, dur = 10|
		if(snapshotMgr.isNil) { "Cocoquantus: not booted.".warn; ^this };
		snapshotMgr.morph(from, to, dur);
	}

	*snapshots { ^snapshotMgr }

	// 16n layer (SPEC §6.1); midiRescan re-inits MIDI after replugging mid-session
	*midi { ^midi16n }

	*midiRescan {
		if(midi16n.isNil) { "Cocoquantus: not booted.".warn; ^this };
		midi16n.rescan;
	}

	// last spesal states reported by the server ('/cq_spesal', SPEC §5.4)
	*spesalStates {
		^(spesalCache ?? { [0, 0, 0, 0, 0] }).copy
	}

	// routing matrix facade (SPEC §5.5): all four go through CQMatrix's own
	// funnel (clamp -1..1, mirror to the wDest bus, notify dependants)
	*patch { |src, dest, w = 1.0|
		if(matrix.isNil) { "Cocoquantus: not booted.".warn; ^this };
		matrix.patch(src, dest, w);
	}

	*unpatch { |src, dest|
		if(matrix.isNil) { "Cocoquantus: not booted.".warn; ^this };
		matrix.unpatch(src, dest);
	}

	*clearPatch {
		if(matrix.isNil) { "Cocoquantus: not booted.".warn; ^this };
		matrix.clearPatch;
	}

	*getPatch { |src, dest|
		^matrix !? { matrix.getPatch(src, dest) }
	}

	// a preset is weights + knob positions (see CQMatrix preset data): the
	// matrix part goes through CQMatrix's funnel, the params part through
	// CQState's — both clamped, both notify dependants
	*patchPreset { |name|
		var data;
		if(matrix.isNil) { "Cocoquantus: not booted.".warn; ^this };
		data = CQMatrix.presets[name];
		if(data.isNil) {
			"Cocoquantus: unknown preset '%' ignored.".format(name).warn;
			^this
		};
		matrix.preset(name);
		data[\params].keysValuesDo { |k, v| state.set(k, v) };
	}

	// GUI (SPEC §6.2): opens at boot unless headless; reopens a closed window.
	// A window the user closed leaves the instrument fully playable (SPEC §6)
	*gui {
		if(this.isBooted.not) { "Cocoquantus: not booted.".warn; ^nil };
		if(guiPanel.isNil or: { guiPanel.isClosed }) {
			guiPanel = CQGui(state, matrix, config);
		};
		^guiPanel
	}

	// recorder (SPEC §5.7): timestamped post-safety master WAV, never overwrites
	*rec {
		if(recorder.isNil) { "Cocoquantus: not booted.".warn; ^this };
		recorder.start;
	}

	*recStop {
		if(recorder.isNil) { "Cocoquantus: not booted.".warn; ^this };
		recorder.stop;
	}

	*recorder { ^recorder }

	// SPEC §5.5 shake: randomize matrix weights (±amount, sparse) + spesal
	// states — never levels/feedback (safety+musicality rule; CQMatrix.shake
	// only reaches weights by construction). seed makes the throw reproducible
	*shake { |amount = 0.3, seed|
		var spesal;
		if(matrix.isNil) { "Cocoquantus: not booted.".warn; ^this };
		spesal = matrix.shake(amount, seed);
		spesal.do { |v, i| this.spesalSet(i + 1, v) };
	}

	// panic is always reachable and preserves state (SPEC safety #3)
	*panic {
		this.set(\monitorMute, 1);
		"Cocoquantus: PANIC — output muted. CQ.resume to unmute.".postln;
	}

	*resume {
		this.set(\monitorMute, 0);
	}

	*free {
		if(graph.isNil) { ^this };
		CmdPeriod.remove(this);
		{
			recorder !? { recorder.stop };     // close the file before teardown (ARCH §6)
			recorder = nil;
			if(graph.isRunning) {
				state.set(\monitorMute, 1);    // 30 ms fade before teardown
				0.15.wait;
			};
			guiPanel !? { guiPanel.free };         // closes window, frees \cq_gui_* defs
			guiPanel = nil;
			snapshotMgr !? { snapshotMgr.free };   // stops a running morph
			snapshotMgr = nil;
			midi16n !? { midi16n.free };
			midi16n = nil;
			micWatcher !? { micWatcher.remove };
			micWatcher = nil;
			micWasLive = false;
			OSCdef.all[\cq_spesal] !? { |d| d.free };
			spesalCache = nil;
			deckAPlayer !? { deckAPlayer.free };
			deckAPlayer = nil;
			deckBPlayer !? { deckBPlayer.free };
			deckBPlayer = nil;
			graph.free;
			graph = nil;
			state = nil;
			matrix = nil;
			config = nil;
			"Cocoquantus: freed.".postln;
		}.fork(AppClock);
	}

	*reboot {
		var path = configPath;
		this.free;
		AppClock.sched(0.8, { this.boot(path); nil });
	}

	// CmdPeriod protocol: Cmd-. kills all nodes — that is silence, which is safe;
	// the graph cannot be trusted afterwards, so require an explicit reboot (ARCH §6.9)
	*cmdPeriod {
		if(graph.notNil and: { graph.isRunning }) {
			graph.markKilled;
			("Cocoquantus: Cmd-. killed the server graph (output is silent). "
				++ "Run CQ.reboot to rebuild.").postln;
		};
	}

	*repoDir {
		^this.filenameSymbol.asString.dirname.dirname
	}

	// ---- private ----

	*prCocoParam { |n, suffix|
		if(#[1, 2].includes(n).not) {
			"Cocoquantus: coco index must be 1 or 2 (got %).".format(n).warn;
			^nil
		};
		^("coco" ++ n ++ suffix).asSymbol
	}

	*prCastleSynth { |k|
		if(#[1, 2, 3, 4, 5].includes(k).not) {
			"Cocoquantus: castle index must be 1..5 (got %).".format(k).warn;
			^nil
		};
		if(this.isBooted.not) { "Cocoquantus: not booted.".warn; ^nil };
		^graph.synths[\quantussy]
	}

	*prSamplesDir { ^this.prFolder(\samples) }
	*prPresetsDir { ^this.prFolder(\presets) }

	*prFolder { |key|
		var dir = config[\folders][key].asString;
		^if(PathName(dir).isAbsolutePath) { dir } { this.repoDir +/+ dir }
	}

	// cache the server-side spesal counters ('/cq_spesal' SendReply) so
	// snapshots and the phase-6 LED display read the same source
	*prInitOsc {
		OSCdef(\cq_spesal, { |msg|
			spesalCache = msg.copyRange(3, 7).collect { |v| v.asInteger };
		}, '/cq_spesal');
	}

	// SPEC §7 mic warning (headless part; the GUI adds color in phase 6):
	// warn once per 0 → live transition, through any entry point
	*prInitMicWatch {
		micWatcher = SimpleController(state);
		micWatcher.put(\param, { |obj, what, name, val|
			if(name == \micGain) {
				if((val > 0) and: { micWasLive.not }) {
					micWasLive = true;
					("Cocoquantus: micGain > 0 — the mic is LIVE. Feedback risk "
						++ "with builtin speakers; CQ.set(\\micGain, 0) to mute.").warn;
				};
				if(val <= 0) { micWasLive = false };
			};
		});
	}
}

// short alias: CQ.boot, CQ.set(...), CQ.panic — classvars shared with Cocoquantus
CQ : Cocoquantus {}
