// Cocoquantus — facade singleton (SPEC §6.3). Owns the boot lifecycle; all
// state flows through CQState, all server objects live in CQGraph.
// CQ (bottom of file) is the short alias — classvars are shared.

Cocoquantus {
	classvar <config, <graph, <state, <matrix, <server;
	classvar configPath, deckAPlayer, deckBPlayer, deckPair, snapshotMgr, midi16n, recorder;
	classvar guiPanel;
	classvar spesalCache, micWatcher, micWasLive = false, trimDb = 0;
	classvar <lifecycleState = \stopped, <sessionGeneration = 0, <lifecycleError;
	classvar pendingBoot, serverQuitAction, serverBootAction;

	*boot { |path, onReady|
		var generation, sessionConfig, sessionServer, sessionGraph, sessionState;
		var sessionMatrix;
		if(lifecycleState == \running) {
			"Cocoquantus: already booted — CQ.free first.".warn;
			^this
		};
		if(lifecycleState == \booting) {
			"Cocoquantus: boot already in progress.".warn;
			^this
		};
		if(lifecycleState == \stopping) {
			pendingBoot = [path, onReady];
			"Cocoquantus: boot queued until teardown completes.".postln;
			^this
		};
		// A killed/failed session may retain a dead graph solely so teardown can
		// release language resources. Queue the new boot behind that teardown.
		if(graph.notNil) {
			pendingBoot = [path, onReady];
			this.free;
			^this
		};
		configPath = path ?? { this.repoDir +/+ "config.scd" };
		lifecycleError = nil;
		lifecycleState = \booting;
		sessionGeneration = sessionGeneration + 1;
		generation = sessionGeneration;
		trimDb = 0;                            // watchdog trim resets each boot (SPEC §5.6)
		try {
			sessionConfig = this.prLoadConfig(configPath);
		} { |err|
			lifecycleState = \failed;
			lifecycleError = err.errorString;
			("Cocoquantus: config failed — " ++ lifecycleError).warn;
			^this
		};
		sessionServer = Server.default;
		sessionGraph = CQGraph(sessionServer, sessionConfig);
		sessionGraph.applyOptions;
		sessionState = CQState(sessionGraph);
		sessionMatrix = CQMatrix(sessionGraph);
		config = sessionConfig;
		server = sessionServer;
		graph = sessionGraph;
		state = sessionState;
		matrix = sessionMatrix;
		this.prRegisterCqEvent; // \cq pattern event type (SPEC §6.4)
		sessionServer.waitForBoot({
			this.prBuildSession(generation, sessionConfig, sessionServer,
				sessionGraph, sessionState, sessionMatrix, onReady);
		}, onFailure: {
			this.prBootFailed(generation, sessionGraph,
				Error("server failed to boot"));
		});
	}

	*isBooted {
		^(lifecycleState == \running) and: {
			graph.notNil and: { graph.isRunning }
		}
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
		synth !? {
			synth.set(\t_skip, 1);
			// skip is momentary (no state to display) — notify the GUI for a
			// brief button flash so code/pattern-driven skips are visible too
			state !? { state.changed(\action, \skip, n) };
		};
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
		if(deckPair.isNil) {
			"Cocoquantus: not booted — no decks.".warn;
			^this
		};
		deckPair.load(filePath);
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
		guiPanel !? { guiPanel.cancelAudition };
		this.set(\monitorMute, 1);
		"Cocoquantus: PANIC — output muted. CQ.resume to unmute.".postln;
	}

	*resume {
		this.set(\monitorMute, 0);
	}

	// clear a watchdog auto-trim back to 0 dB (SPEC §5.6): the operator's "I've
	// fixed the patch, restore level" after the output ran hot. A (re)boot also
	// resets it — the fresh master synth starts at unity trim.
	*resetTrim {
		trimDb = 0;
		if(this.isBooted) { graph.synths[\master] !? { |m| m.set(\trim, 1) } };
	}

	*trimDb { ^trimDb }

	// diagnostic (SPEC §5.6): simulate a watchdog trip so the notice + auto-trim path
	// can be verified without driving the master genuinely hot. Sends a real '/cq_hot'
	// to the language, exercising the exact OSCdefs the server's SendReply would hit.
	*testWatchdog {
		NetAddr("127.0.0.1", NetAddr.langPort).sendMsg('/cq_hot', 0, -1);
		("Cocoquantus: testWatchdog — sent a simulated /cq_hot. Expect a warning + "
			++ "(if autoTrim) CQ.trimDb -6, and the GUI notice line.").postln;
	}

	*free { |onDone|
		var oldGraph = graph, oldState = state, oldGui = guiPanel;
		var oldSnapshots = snapshotMgr, oldMidi = midi16n, oldMicWatcher = micWatcher;
		var oldDeckA = deckAPlayer, oldDeckB = deckBPlayer, oldPair = deckPair;
		var oldRecorder = recorder;
		var generation;
		if(graph.isNil and: { lifecycleState == \stopped }) {
			this.prRemoveServerBootAction;
			onDone.value(this);
			^this
		};
		sessionGeneration = sessionGeneration + 1; // invalidate every pending callback now
		generation = sessionGeneration;
		lifecycleState = \stopping;
		CmdPeriod.remove(this);
		this.prUnwatchServer;
		this.prRemoveServerBootAction;
		graph = nil;
		state = nil;
		matrix = nil;
		config = nil;
		guiPanel = nil;
		snapshotMgr = nil;
		midi16n = nil;
		micWatcher = nil;
		deckAPlayer = nil;
		deckBPlayer = nil;
		deckPair = nil;
		recorder = nil;
		{
			var queued, recorderDone;
			oldRecorder !? {
				recorderDone = Condition.new;
				oldRecorder.free({ recorderDone.unhang });
				if(oldRecorder.state != \idle) { recorderDone.hang };
			}; // close the file before graph teardown (ARCH §6)
			if(oldGraph.notNil and: { oldGraph.isRunning }) {
				oldState !? { oldState.set(\monitorMute, 1) }; // 30 ms fade before teardown
				0.15.wait;
			};
			oldGui !? { oldGui.free };              // closes window, frees \cq_gui_* defs
			oldSnapshots !? { oldSnapshots.free };  // stops a running morph
			oldMidi !? { oldMidi.free };
			oldMicWatcher !? { oldMicWatcher.remove };
			micWasLive = false;
			OSCdef.all[\cq_spesal] !? { |d| d.free };
			OSCdef.all[\cq_hot] !? { |d| d.free };
			spesalCache = nil;
			oldPair !? { oldPair.free };
			oldDeckA !? { oldDeckA.free };
			oldDeckB !? { oldDeckB.free };
			oldGraph !? { oldGraph.free };
			if(sessionGeneration == generation) {
				lifecycleState = \stopped;
				lifecycleError = nil;
				"Cocoquantus: freed.".postln;
				onDone.value(this);
				queued = pendingBoot;
				pendingBoot = nil;
				queued !? { this.boot(queued[0], queued[1]) };
			};
		}.fork(AppClock);
	}

	*reboot {
		var path = configPath, targetServer = server ?? { Server.default };
		this.free({ this.prRebootServer(targetServer, path) });
	}

	// CmdPeriod protocol: Cmd-. kills all nodes — that is silence, which is safe;
	// the graph cannot be trusted afterwards, so require an explicit reboot (ARCH §6.9)
	*cmdPeriod {
		if(graph.notNil and: { graph.isRunning }) {
			sessionGeneration = sessionGeneration + 1;
			lifecycleState = \failed;
			lifecycleError = "CmdPeriod killed the server graph";
			this.prUnwatchServer;
			graph.markKilled;
			guiPanel !? { guiPanel.free };   // the window is dead (server + its OSCdefs
			guiPanel = nil;                  // gone) — close it now so a reboot/boot
			                                 // opens a fresh one, not a frozen ghost
			("Cocoquantus: Cmd-. killed the server graph (output is silent). "
				++ "Run CQ.reboot to rebuild.").postln;
		};
	}

	*repoDir {
		^this.filenameSymbol.asString.dirname.dirname
	}

	// ---- private ----

	*prLoadConfig { |path|
		^if(File.exists(path.asString)) {
			CQConfig.load(path)
		} {
			("Cocoquantus: % not found — using defaults "
				++ "(copy config.example.scd to config.scd)").format(path).warn;
			CQConfig.validate(())
		}
	}

	// Every blocking boot stage is bracketed by a generation check. A free,
	// reboot, CmdPeriod or failed session increments the generation immediately,
	// so stale async work can only enter the local cleanup path (CQ-AUD-004).
	*prBuildSession { |generation, sessionConfig, sessionServer, sessionGraph,
		sessionState, sessionMatrix, onReady|
		try {
			this.prRequireSession(generation);
			sessionGraph.build;                // buses + groups, synced
			this.prRequireSession(generation);
			CQSynthDefs.send(sessionConfig, sessionServer);
			sessionServer.sync;
			this.prRequireSession(generation);
			this.prInitOsc;                     // catch the boot-time spesal report
			this.prInitMicWatch;
			sessionGraph.spawn;                // synths in group order, synced
			this.prRequireSession(generation);
			sessionState.pushAll;              // defaults + power-on state
			sessionMatrix.pushAll;             // matrix empty (SPEC boot sequence)
			sessionServer.sync;
			this.prRequireSession(generation);
			deckAPlayer = CQDeck(sessionGraph, sessionState, \deckA, this.prSamplesDir);
			deckBPlayer = CQDeck(sessionGraph, sessionState, \deckB, this.prSamplesDir);
			deckPair = CQDeckPair(deckAPlayer, deckBPlayer, sessionServer);
			snapshotMgr = CQSnapshots(sessionState, sessionMatrix, this.prPresetsDir,
				{ this.spesalStates },
				{ |states| states.do { |v, i| this.spesalSet(i + 1, v) } });
			midi16n = CQMidi16n(sessionState, sessionConfig[\sixteenN]);
			recorder = CQRecorder(sessionGraph, sessionConfig, this.prFolder(\recordings));
			this.prRequireSession(generation);
			lifecycleState = \running;
			this.prWatchServer(sessionServer, generation);
			if(sessionConfig[\headless] != true) { this.gui };
			sessionGraph.fadeIn;               // silent until verified → 2 s ramp
			CmdPeriod.add(this);
			"Cocoquantus: ready — output fading in over 2 s.".postln;
			onReady.value(this);
		} { |err|
			if((sessionGeneration == generation) and: {
				lifecycleState == \booting
			}) {
				this.prBootFailed(generation, sessionGraph, err);
			} {
				try { sessionGraph.free } { |cleanupError|
					("Cocoquantus: stale-session cleanup failed — "
						++ cleanupError.errorString).warn;
				};
			};
		};
	}

	*prRequireSession { |generation|
		if((sessionGeneration != generation) or: { lifecycleState != \booting }) {
			Error("Cocoquantus: stale session generation %".format(generation)).throw;
		};
	}

	*prBootFailed { |generation, sessionGraph, err|
		if(sessionGeneration != generation) { ^this };
		sessionGeneration = sessionGeneration + 1;
		lifecycleState = \failed;
		lifecycleError = err.errorString;
		CmdPeriod.remove(this);
		this.prUnwatchServer;
		guiPanel !? { guiPanel.free };
		guiPanel = nil;
		snapshotMgr !? { snapshotMgr.free };
		snapshotMgr = nil;
		midi16n !? { midi16n.free };
		midi16n = nil;
		micWatcher !? { micWatcher.remove };
		micWatcher = nil;
		deckAPlayer !? { deckAPlayer.free };
		deckAPlayer = nil;
		deckBPlayer !? { deckBPlayer.free };
		deckBPlayer = nil;
		deckPair !? { deckPair.free };
		deckPair = nil;
		recorder !? { recorder.free };
		recorder = nil;
		OSCdef.all[\cq_spesal] !? { |def| def.free };
		OSCdef.all[\cq_hot] !? { |def| def.free };
		try { sessionGraph.free } { |cleanupError|
			("Cocoquantus: boot rollback cleanup failed — "
				++ cleanupError.errorString).warn;
		};
		graph = nil;
		state = nil;
		matrix = nil;
		config = nil;
		("Cocoquantus: boot failed and rolled back — " ++ lifecycleError).warn;
	}

	// CQ.reboot is deliberately the strong operation: it restarts Server.default
	// so boot-only options take effect. Because that also stops unrelated synths,
	// callers must invoke it explicitly; normal CQ.free/CQ.boot never quits scsynth.
	*prRebootServer { |targetServer, path|
		var preparedConfig, optionGraph, generation, action;
		path = path ?? { this.repoDir +/+ "config.scd" };
		try {
			preparedConfig = this.prLoadConfig(path);
			optionGraph = CQGraph(targetServer, preparedConfig);
			optionGraph.applyOptions;
		} { |err|
			lifecycleState = \failed;
			lifecycleError = err.errorString;
			("Cocoquantus: reboot config failed — " ++ lifecycleError).warn;
			^this
		};
		configPath = path;
		config = preparedConfig;
		server = targetServer;
		lifecycleState = \booting;
		lifecycleError = nil;
		sessionGeneration = sessionGeneration + 1;
		generation = sessionGeneration;
		action = { |bootedServer|
			if(bootedServer === targetServer) {
				ServerBoot.remove(action, targetServer);
				serverBootAction = nil;
				if((sessionGeneration == generation) and: {
					lifecycleState == \booting
				}) {
					lifecycleState = \stopped;
					this.boot(path);
				};
			};
		};
		serverBootAction = action;
		ServerBoot.add(action, targetServer);
		try {
			targetServer.reboot({}, {
				if(sessionGeneration == generation) {
					this.prRemoveServerBootAction;
					lifecycleState = \failed;
					lifecycleError = "server reboot failed";
				};
			});
		} { |err|
			this.prRemoveServerBootAction;
			lifecycleState = \failed;
			lifecycleError = err.errorString;
			("Cocoquantus: server reboot failed — " ++ lifecycleError).warn;
		};
	}

	*prRemoveServerBootAction {
		if(serverBootAction.notNil and: { server.notNil }) {
			ServerBoot.remove(serverBootAction, server);
		};
		serverBootAction = nil;
	}

	*prWatchServer { |sessionServer, generation|
		this.prUnwatchServer;
		serverQuitAction = { |quitServer|
			if((quitServer === sessionServer) and: {
				sessionGeneration == generation
			} and: { lifecycleState == \running }) {
				this.prUnexpectedServerQuit;
			};
		};
		ServerQuit.add(serverQuitAction, sessionServer);
	}

	*prUnwatchServer {
		if(serverQuitAction.notNil and: { server.notNil }) {
			ServerQuit.remove(serverQuitAction, server);
		};
		serverQuitAction = nil;
	}

	*prUnexpectedServerQuit {
		sessionGeneration = sessionGeneration + 1;
		lifecycleState = \failed;
		lifecycleError = "scsynth quit unexpectedly";
		CmdPeriod.remove(this);
		this.prUnwatchServer;
		graph !? { graph.markKilled };
		guiPanel !? { guiPanel.free };
		guiPanel = nil;
		snapshotMgr !? { snapshotMgr.free };
		snapshotMgr = nil;
		midi16n !? { midi16n.free };
		midi16n = nil;
		micWatcher !? { micWatcher.remove };
		micWatcher = nil;
		deckAPlayer !? { deckAPlayer.free };
		deckAPlayer = nil;
		deckBPlayer !? { deckBPlayer.free };
		deckBPlayer = nil;
		recorder = nil; // the server-side writer is already gone
		OSCdef.all[\cq_spesal] !? { |def| def.free };
		OSCdef.all[\cq_hot] !? { |def| def.free };
		state = nil;
		matrix = nil;
		("Cocoquantus: scsynth quit unexpectedly; session invalidated. "
			++ "Run CQ.reboot to rebuild.").warn;
	}

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
		// watchdog (SPEC §5.6): the server flags sustained-hot output on '/cq_hot'.
		// With autoTrim on, step the master trim down −6 dB (floor −18 dB); otherwise
		// just warn. The GUI shows its own notice (CQGui listens to the same address).
		OSCdef(\cq_hot, {
			if(config.notNil and: { config[\autoTrim] == true }) {
				trimDb = max(trimDb - 6, -18);
				graph !? { graph.synths[\master] !? { |m| m.set(\trim, trimDb.dbamp) } };
				("Cocoquantus: WATCHDOG — driving hard > 5 s, auto-trim now % dB "
					++ "(CQ.resetTrim to restore).").format(trimDb).warn;
			} {
				// notice-only (default): the limiter already keeps it safe; auto-trim
				// is opt-in (config autoTrim: true) — a comfort ease-off, not safety
				("Cocoquantus: watchdog — driving the master hard (> 5 s). Limiter has "
					++ "it safe; config autoTrim: true adds an automatic −6 dB ease-off.").postln;
			};
		}, '/cq_hot');
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

	// SPEC §6.4 — Pbind sequencing of discrete actions on a TempoClock: a
	// generative "player piano". Event type \cq dispatches ONE action per event
	// through the facade, so every write still passes the funnels. Registered once;
	// re-registering on reboot is harmless. A missing/\rest action or an unbooted
	// instrument is a silent no-op, so a pattern may carry Rests and survive a
	// free/reboot. Examples:
	//   Pbind(\type, \cq, \action, \skip,   \coco, 1,             \dur, 1).play
	//   Pbind(\type, \cq, \action, \spesal, \castle, 3,           \dur, Pexprand(0.5, 4)).play
	//   Pbind(\type, \cq, \action, \hold,   \coco, 2, \val, 1,    \dur, 4).play
	//   Pbind(\type, \cq, \action, \deck,   \deck, \a, \do, \next, \dur, 8).play
	//   Pbind(\type, \cq, \action, \set,    \param, \coco1Fb, \value, 1.1, \dur, 2).play
	*prRegisterCqEvent {
		Event.addEventType(\cq, { Cocoquantus.prCqAction(currentEnvironment) });
	}

	// dispatch one \cq event (kept out of the event-type closure so it is unit-
	// testable and degrades gracefully when the instrument is down)
	*prCqAction { |ev|
		var action = ev[\action];
		if(action.isNil or: { action == \rest }) { ^this };   // Rest / no-op
		if(this.isBooted.not) { ^this };                       // silent when down
		switch(action,
			\hold,   { this.hold(ev[\coco] ? 1, ev[\val]) },
			\flip,   { this.flip(ev[\coco] ? 1, ev[\val]) },
			\skip,   { this.skip(ev[\coco] ? 1) },
			\spesal, {
				if(ev[\state].notNil)
					{ this.spesalSet(ev[\castle] ? 1, ev[\state]) }
					{ this.spesal(ev[\castle] ? 1) };
			},
			\set,    { ev[\param] !? { this.set(ev[\param], ev[\value]) } },
			\patch,  { this.patch(ev[\src], ev[\dest], ev[\weight] ? 1.0) },
			\deck,   { this.prCqDeck(ev) },
			\shake,  { this.shake(ev[\amount] ? 0.3, ev[\seed]) },
			{ "Cocoquantus: \\cq event — unknown action '%' ignored.".format(action).warn }
		);
	}

	*prCqDeck { |ev|
		var deck = if((ev[\deck] ? \a) == \b) { this.deckB } { this.deckA };
		deck !? {
			switch(ev[\do] ? \next,
				\next,    { deck.next },
				\prev,    { deck.prev },
				\random,  { deck.random },
				\restart, { deck.restart },
				{ "Cocoquantus: \\cq deck op '%' ignored.".format(ev[\do]).warn }
			);
		};
	}
}

// short alias: CQ.boot, CQ.set(...), CQ.panic — classvars shared with Cocoquantus
CQ : Cocoquantus {}
