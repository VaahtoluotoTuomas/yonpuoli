// Cocoquantus — facade singleton (SPEC §6.3). Owns the boot lifecycle; all
// state flows through CQState, all server objects live in CQGraph.
// CQ (bottom of file) is the short alias — classvars are shared.

Cocoquantus {
	classvar <config, <graph, <state, <matrix, <server;
	classvar configPath, deckAPlayer, deckBPlayer, deckPair, snapshotMgr, midi16n, recorder;
	classvar guiPanel;
	classvar spesalCache, micWatcher, micWasLive = false, trimDb = 0;
	classvar <lifecycleState = \stopped, <sessionGeneration = 0, <lifecycleError;
	classvar <audioProfilePresent = false;
	classvar <audioConfigModel, <audioReconfigureState = \idle;
	classvar <audioReconfigureError, <audioReconfigureResult;
	classvar audioTransaction, audioReopenPending = false;
	classvar pendingBoot, serverQuitAction, serverBootAction;

	*boot { |path, onReady, preparedConfig, onFailure, beforeGui|
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
			pendingBoot = [path, onReady, preparedConfig, onFailure, beforeGui];
			"Cocoquantus: boot queued until teardown completes.".postln;
			^this
		};
		// A killed/failed session may retain a dead graph solely so teardown can
		// release language resources. Queue the new boot behind that teardown.
		if(graph.notNil) {
			pendingBoot = [path, onReady, preparedConfig, onFailure, beforeGui];
			this.free;
			^this
		};
		if(preparedConfig.isNil) {
			audioConfigModel = nil;
			audioTransaction = nil;
			audioReconfigureState = \idle;
			audioReconfigureError = nil;
			audioReconfigureResult = nil;
			audioReopenPending = false;
		};
		configPath = path ?? { this.repoDir +/+ "config.scd" };
		lifecycleError = nil;
		lifecycleState = \booting;
		sessionGeneration = sessionGeneration + 1;
		generation = sessionGeneration;
		trimDb = 0;                            // watchdog trim resets each boot (SPEC §5.6)
		try {
			sessionConfig = if(preparedConfig.isNil) {
				this.prLoadConfig(configPath)
			} {
				CQConfig.validate(preparedConfig).copy
			};
		} { |err|
			lifecycleState = \failed;
			lifecycleError = err.errorString;
			("Cocoquantus: config failed — " ++ lifecycleError).warn;
			onFailure.value(err);
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
				sessionGraph, sessionState, sessionMatrix, onReady,
				onFailure, beforeGui);
		}, onFailure: {
			this.prBootFailed(generation, sessionGraph,
				Error("server failed to boot"), onFailure);
		});
	}

	*isBooted {
		^(lifecycleState == \running) and: {
			graph.notNil and: {
				graph.isRunning and: { this.serverResponsive }
			}
		}
	}

	// `serverRunning` alone is not enough on Windows: the status watcher keeps
	// the process registered while repeated /status probes establish that it is
	// alive but no longer answering. Keep that state distinct so the GUI can
	// offer recovery instead of presenting a stale "running" session.
	*serverResponsive {
		var targetServer = server ?? { Server.default };
		^targetServer.notNil
			and: { targetServer.serverRunning }
			and: { targetServer.unresponsive.not }
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
	*patchPreset { |name, origin = \code|
		var data;
		if(matrix.isNil) { "Cocoquantus: not booted.".warn; ^this };
		data = CQMatrix.presets[name];
		if(data.isNil) {
			"Cocoquantus: unknown preset '%' ignored.".format(name).warn;
			^this
		};
		matrix.preset(name, origin: origin, committed: true, operation: \preset);
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
	// `density` = how much is sown (SPEC §5.5; the night's sowing moon wears it
	// as its phase). Omitted, it stays the historical ~10% handful.
	*shake { |amount = 0.3, seed, origin = \code, density|
		var spesal;
		if(matrix.isNil) { "Cocoquantus: not booted.".warn; ^this };
		spesal = matrix.shake(amount, seed, origin: origin,
			committed: true, operation: \shake, density: density);
		spesal.do { |v, i| this.spesalSet(i + 1, v) };
		// like skip, a shake is momentary and leaves no state of its own to
		// display — so it is announced once here, and every layer's light hangs
		// off that one notice (GUI_SPEC §6.1: the Night's sowing moon flashes
		// on a throw from the p key, the moon, code or a pattern alike)
		state !? { state.changed(\action, \shake, amount) };
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
				queued !? {
					this.boot(queued[0], queued[1], queued[2], queued[3], queued[4])
				};
			};
		}.fork(AppClock);
	}

	*reboot {
		var path = configPath, targetServer = server ?? { Server.default };
		audioConfigModel = nil;
		audioTransaction = nil;
		audioReconfigureState = \idle;
		audioReconfigureError = nil;
		audioReconfigureResult = nil;
		audioReopenPending = false;
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

	// The Audio Drawer needs the base path only to derive its generated sibling
	// profile. It never parses or writes config.scd itself.
	*configFilePath {
		^(configPath ?? { this.repoDir +/+ "config.scd" }).asString
	}

	// The non-Qt Audio model survives only the two boot attempts of one AIO-3
	// transaction. A rebuilt GUI asks for it so rollback can reopen the rejected
	// draft instead of constructing a stale page callback.
	*audioConfigForGui { |activeConfig|
		if(audioConfigModel.isNil or: { audioTransaction.isNil }) {
			audioConfigModel = CQAudioConfig(activeConfig ? config,
				this.configFilePath, audioProfilePresent);
		};
		^audioConfigModel
	}

	*audioReopenRequested { ^audioReopenPending }

	*consumeAudioReopen {
		var answer = audioReopenPending;
		audioReopenPending = false;
		^answer
	}

	// Confirmed Audio I/O restart. The GUI passes only its non-Qt model; all
	// pending-file, server and rollback ownership remains below the facade.
	*reconfigureAudio { |model|
		var path = this.configFilePath;
		var base, store, runner, transaction;
		if(model.isNil) {
			audioReconfigureState = \invalid;
			audioReconfigureError = "missing Audio configuration model";
			^false
		};
		try {
			base = this.prLoadBaseConfig(path);
		} { |failure|
			audioReconfigureState = \failed;
			audioReconfigureError = failure.errorString;
			^false
		};
		store = CQAudioProfileStore(model.profilePath);
		runner = (
			lifecycle: { lifecycleState },
			recorderState: {
				if(recorder.isNil) { \idle } { recorder.state }
			},
			restart: { |candidate, onReady, onFailure|
				this.prRestartAudioConfig(candidate, onReady, onFailure)
			}
		);
		audioConfigModel = model;
		transaction = CQAudioReconfigure(model, base, store, runner,
			{ |result|
				if(audioTransaction === transaction) {
					this.prPublishAudioReconfigure(result)
				}
			});
		audioTransaction = transaction;
		^audioTransaction.start
	}

	// A clean Audio page still needs a recovery operation. This restarts the
	// exact active configuration without creating or rewriting an audio profile.
	// It is deliberately separate from CQAudioReconfigure: there is no candidate
	// to commit and retrying the same config as a rollback would add no safety.
	*restartCurrentAudio {
		var preparedConfig;
		if(lifecycleState != \running) {
			audioReconfigureState = \failed;
			audioReconfigureError = "audio session is not running";
			^false
		};
		if(recorder.notNil and: {
			#[starting, recording, stopping].includes(recorder.state)
		}) {
			audioReconfigureState = \blocked;
			audioReconfigureError = "stop REC before restarting audio";
			^false
		};
		try {
			preparedConfig = CQConfig.validate(config).copy;
		} { |failure|
			audioReconfigureState = \failed;
			audioReconfigureError = failure.errorString;
			^false
		};
		audioTransaction = nil;
		audioConfigModel = nil;
		audioReconfigureState = \restarting;
		audioReconfigureError = nil;
		audioReconfigureResult = (
			status: \restarting,
			error: nil,
			cleanRestart: true
		);
		this.prRestartAudioConfig(preparedConfig, {
			audioReconfigureState = \ready;
			audioReconfigureError = nil;
			audioReconfigureResult = (
				status: \ready,
				error: nil,
				cleanRestart: true
			);
			// This callback runs before the rebuilt GUI is constructed.
			audioReopenPending = true;
		}, { |failure|
			audioReconfigureState = \failed;
			audioReconfigureError = failure.errorString;
			audioReconfigureResult = (
				status: \failed,
				error: failure.errorString,
				cleanRestart: true
			);
			("Cocoquantus: Audio restart failed — "
				++ audioReconfigureError).warn;
		});
		^true
	}

	*retryAudioProfileSave {
		if(audioTransaction.isNil) { ^false };
		^audioTransaction.retrySave
	}

	// ---- private ----

	*prLoadBaseConfig { |path|
		^if(File.exists(path.asString)) {
			CQConfig.load(path)
		} {
			("Cocoquantus: % not found — using defaults "
				++ "(copy config.example.scd to config.scd)").format(path).warn;
			CQConfig.validate(())
		}
	}

	*prLoadConfig { |path|
		var base, profilePath;
		base = this.prLoadBaseConfig(path);
		profilePath = CQAudioConfig.profilePathFor(path);
		audioProfilePresent = CQAudioProfileStore.profileExists(profilePath);
		^if(audioProfilePresent) {
			CQAudioConfig.merge(base,
				CQAudioProfileStore.loadProfile(profilePath))
		} {
			base
		}
	}

	// Every blocking boot stage is bracketed by a generation check. A free,
	// reboot, CmdPeriod or failed session increments the generation immediately,
	// so stale async work can only enter the local cleanup path (CQ-AUD-004).
	*prBuildSession { |generation, sessionConfig, sessionServer, sessionGraph,
		sessionState, sessionMatrix, onReady, onFailure, beforeGui|
		var completed = false;
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
			snapshotMgr = CQSnapshots(sessionState, sessionMatrix, this.prSnapshotsDir,
				{ this.spesalStates },
				{ |states| states.do { |v, i| this.spesalSet(i + 1, v) } },
				this.prPresetsDir);
			midi16n = CQMidi16n(sessionState, sessionConfig[\sixteenN]);
			recorder = CQRecorder(sessionGraph, sessionConfig, this.prFolder(\recordings));
			this.prRequireSession(generation);
			lifecycleState = \running;
			this.prWatchServer(sessionServer, generation);
			sessionGraph.fadeIn;               // silent until verified → 2 s ramp
			// AIO-3 publishes the exact captured candidate only here: every
			// server/graph/state/matrix stage has completed and fade-in is armed.
			beforeGui.value(this);
			if(sessionConfig[\headless] != true) { this.gui };
			CmdPeriod.add(this);
			"Cocoquantus: ready — output fading in over 2 s.".postln;
			completed = true;
		} { |err|
			if((sessionGeneration == generation) and: {
				lifecycleState == \booting
			}) {
				this.prBootFailed(generation, sessionGraph, err, onFailure);
			} {
				try { sessionGraph.free } { |cleanupError|
					("Cocoquantus: stale-session cleanup failed — "
						++ cleanupError.errorString).warn;
				};
			};
		};
		if(completed) { onReady.value(this) };
	}

	*prRequireSession { |generation|
		if((sessionGeneration != generation) or: { lifecycleState != \booting }) {
			Error("Cocoquantus: stale session generation %".format(generation)).throw;
		};
	}

	*prBootFailed { |generation, sessionGraph, err, onFailure|
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
		onFailure.value(err);
	}

	// CQ.reboot is deliberately the strong operation: it restarts Server.default
	// so boot-only options take effect. Because that also stops unrelated synths,
	// callers must invoke it explicitly; normal CQ.free/CQ.boot never quits scsynth.
	*prRebootServer { |targetServer, path|
		var preparedConfig;
		path = path ?? { this.repoDir +/+ "config.scd" };
		try {
			preparedConfig = this.prLoadConfig(path);
		} { |err|
			lifecycleState = \failed;
			lifecycleError = err.errorString;
			("Cocoquantus: reboot config failed — " ++ lifecycleError).warn;
			^this
		};
		this.prRebootPrepared(targetServer, path, preparedConfig);
	}

	*prRestartAudioConfig { |preparedConfig, onReady, onFailure|
		var targetServer = server ?? { Server.default };
		var path = this.configFilePath;
		this.free({
			this.prRebootPrepared(targetServer, path, preparedConfig,
				onReady, onFailure)
		});
	}

	*prRebootPrepared { |targetServer, path, preparedConfig, onReady, onFailure|
		var optionGraph, generation, action, fail, bootAfterForcedQuit;
		var forcedQuitPolls = 0, launchBoot, pollForcedQuit;
		path = path ?? { this.repoDir +/+ "config.scd" };
		fail = { |failure|
			this.prRemoveServerBootAction;
			lifecycleState = \failed;
			lifecycleError = failure.errorString;
			("Cocoquantus: server reboot failed — " ++ lifecycleError).warn;
			onFailure.value(failure);
		};
		try {
			preparedConfig = CQConfig.validate(preparedConfig).copy;
			optionGraph = CQGraph(targetServer, preparedConfig);
			optionGraph.applyOptions;
		} { |failure|
			fail.value(failure);
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
					this.boot(path, nil, preparedConfig, onFailure, onReady);
				};
			};
		};
		serverBootAction = action;
		ServerBoot.add(action, targetServer);
		launchBoot = {
			if((sessionGeneration == generation) and: {
				lifecycleState == \booting
			}) {
				try {
					targetServer.boot(onFailure: {
						if(sessionGeneration == generation) {
							fail.value(Error(
								"server boot after forced quit failed"));
						};
					});
				} { |err|
					if(sessionGeneration == generation) { fail.value(err) };
				};
			};
		};
		pollForcedQuit = {
			AppClock.sched(0.05, {
				if((sessionGeneration != generation) or: {
					lifecycleState != \booting
				}) {
					nil
				} {
					if(targetServer.applicationRunning
						or: { targetServer.unresponsive }) {
						forcedQuitPolls = forcedQuitPolls + 1;
						if(forcedQuitPolls >= 60) {
							fail.value(Error(
								"unresponsive server process did not stop"));
							nil
						} {
							0.05
						}
					} {
						launchBoot.value;
						nil
					}
				}
			});
		};
		bootAfterForcedQuit = { pollForcedQuit.value };
		try {
			if(targetServer.unresponsive) {
				// Server.reboot skips quit when statusWatcher.unresponsive is
				// true, then starts a second process on the occupied port. Use
				// Server.quit's exact-PID forced stop before booting.
				targetServer.quit(bootAfterForcedQuit, {
					if(sessionGeneration == generation) {
						fail.value(Error(
							"unresponsive server forced quit failed"));
					};
				}, false);
			} {
				targetServer.reboot({}, {
					if(sessionGeneration == generation) {
						fail.value(Error("server reboot failed"));
					};
				});
			};
		} { |err|
			if(sessionGeneration == generation) { fail.value(err) };
		};
	}

	*prPublishAudioReconfigure { |result|
		audioReconfigureResult = result.copy;
		audioReconfigureState = result[\status];
		audioReconfigureError = result[\error];
		if(#[ready, notSaved, rollbackReady].includes(audioReconfigureState)) {
			audioConfigModel !? {
				audioProfilePresent = audioConfigModel.profilePresent
			};
			if(guiPanel.isNil) { audioReopenPending = true };
		};
		if(audioReconfigureState == \failed) {
			if(result[\rollbackError].notNil) {
				("Cocoquantus: Audio candidate failed: %; rollback failed: %. "
					++ "Base/profile: % / %. Output remains silent. Recovery: CQ.reboot")
					.format(result[\candidateError], result[\rollbackError],
						this.configFilePath,
						CQAudioConfig.profilePathFor(this.configFilePath)).warn;
			} {
				("Cocoquantus: Audio reconfigure failed — "
					++ audioReconfigureError).warn;
			};
		};
		^result
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
	*prSnapshotsDir { ^this.prFolder(\snapshots) }

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
			\shake,  { this.shake(ev[\amount] ? 0.3, ev[\seed],
				density: ev[\density]) },
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
