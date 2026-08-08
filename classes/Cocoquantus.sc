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
	classvar <audioPreferredConfig, <audioFallbackActive = false;
	classvar <audioConfigModel, <audioReconfigureState = \idle;
	classvar <audioReconfigureError, <audioReconfigureResult;
	classvar audioTransaction, audioReopenPending = false;
	classvar pendingBoot, serverQuitAction, serverBootAction;
	classvar <audioDeviceCatalog, <audioDeviceCatalogState = \unavailable;
	classvar <audioDeviceCatalogError, <audioDeviceScanCount = 0;
	classvar audioClockWatch, audioClockRoutine;
	classvar <audioClockState = \ok, <audioClockTickCount = 0;
	classvar <audioBootRecovery;

	*boot { |path, onReady, preparedConfig, onFailure, beforeGui,
		preparedStartupRecovery|
		var generation, sessionConfig, sessionServer, sessionGraph, sessionState;
		var sessionMatrix, startupData, startupRecovery;
		var sessionBeforeGui = beforeGui, sessionOnFailure = onFailure;
		var freshDeviceCapture = false;
		startupRecovery = preparedStartupRecovery;
		if(lifecycleState == \running) {
			"Yönpuoli: already booted — CQ.free first.".warn;
			^this
		};
		if(lifecycleState == \booting) {
			"Yönpuoli: boot already in progress.".warn;
			^this
		};
		if(lifecycleState == \stopping) {
			pendingBoot = [path, onReady, preparedConfig, onFailure, beforeGui,
				preparedStartupRecovery];
			"Yönpuoli: boot queued until teardown completes.".postln;
			^this
		};
		// A killed/failed session may retain a dead graph solely so teardown can
		// release language resources. Queue the new boot behind that teardown.
		if(graph.notNil) {
			pendingBoot = [path, onReady, preparedConfig, onFailure, beforeGui,
				preparedStartupRecovery];
			this.free;
			^this
		};
		if(preparedConfig.isNil) {
			audioConfigModel = nil;
			audioTransaction = nil;
			audioBootRecovery = nil;
			audioReconfigureState = \idle;
			audioReconfigureError = nil;
			audioReconfigureResult = nil;
			audioReopenPending = false;
			audioFallbackActive = false;
		};
		configPath = path ?? { this.repoDir +/+ "config.scd" };
		lifecycleError = nil;
		lifecycleState = \booting;
		sessionGeneration = sessionGeneration + 1;
		generation = sessionGeneration;
		trimDb = 0;                            // watchdog trim resets each boot (SPEC §5.6)
		try {
			sessionConfig = if(preparedConfig.isNil) {
				startupData = this.prLoadStartupAudio(configPath);
				startupRecovery = startupData[\recovery];
				audioBootRecovery = startupRecovery;
				startupData[\config]
			} {
				audioBootRecovery = startupRecovery;
				CQConfig.validate(preparedConfig).copy
			};
		} { |err|
			lifecycleState = \failed;
			lifecycleError = err.errorString;
			("Yönpuoli: config failed — " ++ lifecycleError).warn;
			onFailure.value(err);
			^this
		};
		sessionServer = Server.default;
		// AIO-5: this method is a no-op if an existing server still owns its
		// endpoints. A fresh local boot captures devices before scsynth starts.
		freshDeviceCapture = this.prCaptureAudioDeviceCatalog(sessionServer);
		startupRecovery !? {
			sessionConfig = startupRecovery.selectInitial(
				audioDeviceCatalog,
				if(freshDeviceCapture) { audioDeviceCatalogState } { \unavailable });
			audioFallbackActive = startupRecovery.fallbackActive;
			if(startupRecovery.preflightFailed) {
				this.prStartupAudioPreflightFailed(startupRecovery, onFailure);
				^this
			};
			if(startupRecovery.fallbackActive) {
				sessionBeforeGui = { |facade|
					this.prCompleteStartupAudioRecovery(startupRecovery);
					beforeGui.value(facade)
				};
				sessionOnFailure = { |failure|
					this.prStartupAudioFallbackFailed(
						startupRecovery, failure, onFailure)
				}
			};
		};
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
				sessionOnFailure, sessionBeforeGui, startupRecovery);
		}, onFailure: {
			var failure = Error("server failed to boot");
			if(this.prTryStartupAudioRecovery(generation, sessionServer,
				sessionGraph, failure, \serverBoot, startupRecovery,
				onReady, sessionOnFailure, beforeGui).not) {
				this.prBootFailed(generation, sessionGraph, failure, sessionOnFailure)
			};
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

	// A responsive process is not yet usable audio. The physical Windows
	// system-default path once answered every /status probe while advancing its
	// nominal 48 kHz stream at only ~2.61 kHz. Reject that catastrophic clock
	// mismatch before allocating or fading in the graph, so an Audio candidate
	// reaches its existing one-rollback path and can never replace last-working.
	// AUD-W: the bound and the predicate now live in CQAudioClockWatch so the
	// boot gate and the runtime guard can never drift apart. Behaviour here is
	// unchanged — these are pass-throughs, not a second definition.
	*audioClockRatioBounds { ^CQAudioClockWatch.ratioBounds }
	*audioClockProbeCount { ^3 }
	*audioClockProbePeriod { ^0.75 }

	*prAudioClockHealthy { |requestedRate, actualRate|
		^CQAudioClockWatch.healthy(requestedRate, actualRate)
	}

	*prRequireAudioClock { |targetServer, requestedRate|
		var actualRate = targetServer.tryPerform(\actualSampleRate);
		if(this.prAudioClockHealthy(requestedRate, actualRate).not) {
			Error(
				"audio clock unhealthy — requested % Hz, server reports % Hz"
				.format(requestedRate, actualRate ? "unavailable")
			).throw;
		};
		^this
	}

	// ServerStatusWatcher's values survive Server.reboot until a reply from the
	// replacement process overwrites them. One immediate read can therefore be
	// the previous healthy server's stale truth. Require three readings across
	// more than two watcher periods; every reading must be healthy. Test doubles
	// opt into a zero interval without weakening the production boundary.
	*prRequireSettledAudioClock { |generation, targetServer, requestedRate|
		var readings = List.new;
		var period = targetServer.tryPerform(\cqAudioClockProbePeriod)
			? this.audioClockProbePeriod;
		this.audioClockProbeCount.do { |index|
			if(index > 0 and: { period > 0 }) { period.wait };
			this.prRequireSession(generation);
			readings.add(targetServer.tryPerform(\actualSampleRate));
		};
		if(readings.every { |rate|
			this.prAudioClockHealthy(requestedRate, rate)
		}.not) {
			Error(
				"audio clock unhealthy — requested % Hz, server reports %"
				.format(requestedRate, readings.asArray.asCompileString)
			).throw;
		};
		^this
	}

	*set { |name, value|
		if(state.isNil) { "Yönpuoli: not booted.".warn; ^this };
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
		if(this.isBooted.not) { "Yönpuoli: not booted.".warn; ^this };
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
			"Yönpuoli: bad spesal state % ignored.".format(state).warn;
			^this
		};
		synth !? {
			synth.set(("state" ++ k).asSymbol,
				state.round.asInteger.mod(8),          // entry-point clamp (safety #4)
				("t_set" ++ k).asSymbol, 1);
		};
	}

	*deckA {
		if(deckAPlayer.isNil) { "Yönpuoli: not booted — no deck A.".warn };
		^deckAPlayer
	}

	*deckB {
		if(deckBPlayer.isNil) { "Yönpuoli: not booted — no deck B.".warn };
		^deckBPlayer
	}

	// SPEC §5.1 linked-pair load: deck A takes L, deck B takes R of a stereo
	// file (a mono file plays on both — CQDeck warns and falls back to ch 0)
	*loadStereo { |filePath|
		if(deckPair.isNil) {
			"Yönpuoli: not booted — no decks.".warn;
			^this
		};
		deckPair.load(filePath);
	}

	// snapshots (SPEC §6.3): save stores micGain as 0 unless includeMic — see
	// CQSnapshots header for the full mic/panic policy
	*snap { |name, includeMic = false, note = ""|
		if(snapshotMgr.isNil) { "Yönpuoli: not booted.".warn; ^nil };
		^snapshotMgr.save(name, includeMic, note)
	}

	*recall { |name|
		if(snapshotMgr.isNil) { "Yönpuoli: not booted.".warn; ^false };
		^snapshotMgr.recall(name)
	}

	*morph { |from, to, dur = 10|
		if(snapshotMgr.isNil) { "Yönpuoli: not booted.".warn; ^this };
		snapshotMgr.morph(from, to, dur);
	}

	*snapshots { ^snapshotMgr }

	// 16n layer (SPEC §6.1); midiRescan re-inits MIDI after replugging mid-session
	*midi { ^midi16n }

	*midiRescan {
		if(midi16n.isNil) { "Yönpuoli: not booted.".warn; ^this };
		midi16n.rescan;
	}

	// last spesal states reported by the server ('/cq_spesal', SPEC §5.4)
	*spesalStates {
		^(spesalCache ?? { [0, 0, 0, 0, 0] }).copy
	}

	// routing matrix facade (SPEC §5.5): all four go through CQMatrix's own
	// funnel (clamp -1..1, mirror to the wDest bus, notify dependants)
	*patch { |src, dest, w = 1.0|
		if(matrix.isNil) { "Yönpuoli: not booted.".warn; ^this };
		matrix.patch(src, dest, w);
	}

	*unpatch { |src, dest|
		if(matrix.isNil) { "Yönpuoli: not booted.".warn; ^this };
		matrix.unpatch(src, dest);
	}

	*clearPatch {
		if(matrix.isNil) { "Yönpuoli: not booted.".warn; ^this };
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
		if(matrix.isNil) { "Yönpuoli: not booted.".warn; ^this };
		data = CQMatrix.presets[name];
		if(data.isNil) {
			"Yönpuoli: unknown preset '%' ignored.".format(name).warn;
			^this
		};
		matrix.preset(name, origin: origin, committed: true, operation: \preset);
		data[\params].keysValuesDo { |k, v| state.set(k, v) };
	}

	// GUI (SPEC §6.2): opens at boot unless headless; reopens a closed window.
	// A window the user closed leaves the instrument fully playable (SPEC §6)
	*gui {
		if(this.isBooted.not) { "Yönpuoli: not booted.".warn; ^nil };
		if(guiPanel.isNil or: { guiPanel.isClosed }) {
			guiPanel = CQGui(state, matrix, config);
		};
		^guiPanel
	}

	// recorder (SPEC §5.7): timestamped post-safety master WAV, never overwrites
	*rec {
		if(recorder.isNil) { "Yönpuoli: not booted.".warn; ^this };
		recorder.start;
	}

	*recStop {
		if(recorder.isNil) { "Yönpuoli: not booted.".warn; ^this };
		recorder.stop;
	}

	*recorder { ^recorder }

	// SPEC §5.5 shake: randomize matrix weights (±amount, sparse) + spesal
	// states — never levels/feedback (safety+musicality rule; CQMatrix.shake
	// only reaches weights by construction). seed makes the throw reproducible
	// `density` = how much random decoration is sown (SPEC §5.5; the night's
	// sowing moon wears it as its phase). Every non-empty throw also plants one
	// audible ecology and prepares only its explicitly safe musical controls.
	*shake { |amount = 0.3, seed, origin = \code, density|
		var spesal, context, plan;
		if(matrix.isNil) { "Yönpuoli: not booted.".warn; ^this };
		context = if(state.isNil) { () } { state.asEvent };
		spesal = matrix.shake(amount, seed, origin: origin,
			committed: true, operation: \shake, density: density,
			context: context);
		plan = matrix.lastShakePlan;
		if(plan.notNil and: { state.notNil }) {
			state.setAll(plan[\params])
		};
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
		"Yönpuoli: PANIC — output muted. CQ.resume to unmute.".postln;
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
		("Yönpuoli: testWatchdog — sent a simulated /cq_hot. Expect a warning + "
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
		CmdPeriod.remove(Cocoquantus);   // never `this`: see prPublish's law
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
				"Yönpuoli: freed.".postln;
				onDone.value(this);
				queued = pendingBoot;
				pendingBoot = nil;
				queued !? {
					this.boot(queued[0], queued[1], queued[2], queued[3], queued[4],
						queued[5])
				};
			};
		}.fork(AppClock);
	}

	*reboot {
		var path = configPath, targetServer = server ?? { Server.default };
		audioConfigModel = nil;
		audioTransaction = nil;
		audioBootRecovery = nil;
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
			("Yönpuoli: Cmd-. killed the server graph (output is silent). "
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

	// A running GUI receives memory data only. Exact configured endpoints are
	// merged even when the last stopped-server scan is absent, stale or no
	// longer reports them; no caller of this method can reach ServerOptions.
	*audioDeviceCatalogForGui { |device|
		var result = if(audioDeviceCatalog.notNil) {
			CQAudioDeviceCatalog.withConfigured(audioDeviceCatalog, device)
		} {
			CQAudioDeviceCatalog.configured(device)
		};
		result[\source] = if(audioDeviceCatalog.notNil) {
			audioDeviceCatalogState
		} {
			\configured
		};
		if(audioDeviceCatalogError.notNil) {
			result[\error] = audioDeviceCatalogError
		};
		result[\fallbackActive] = audioFallbackActive;
		result[\preferredDevice] = audioPreferredConfig !? { |preferred|
			preferred[\device]
		};
		^result
	}

	*preferredAudioConfig {
		^audioPreferredConfig !? { |preferred| preferred.copy }
	}

	// Tests reset the process-lifetime memory cache explicitly. Ordinary
	// CQ.free must not clear it: free leaves Server.default running, where a new
	// discovery call would be unsafe.
	*prResetAudioDeviceCatalog {
		audioDeviceCatalog = nil;
		audioDeviceCatalogState = \unavailable;
		audioDeviceCatalogError = nil;
		audioDeviceScanCount = 0;
		^this
	}

	// The one production entry into audio-device discovery. On Windows its
	// default is CQAudioDeviceProbe's bounded fresh sclang process, which renews
	// PortAudio itself after hot-plug; other platforms retain the direct stopped-
	// server primitive. A provider is accepted only for hermetic lifecycle tests;
	// fake servers never hit real discovery. Failure is diagnostic data, not an
	// audio-boot failure.
	*prCaptureAudioDeviceCatalog { |targetServer, provider|
		var success = false;
		if(targetServer.isNil) { ^false };
		if(targetServer.applicationRunning or: { targetServer.serverRunning }) {
			^false
		};
		if(provider.isNil and: { targetServer.isKindOf(Server).not }) {
			^false
		};
		audioDeviceScanCount = audioDeviceScanCount + 1;
		try {
			audioDeviceCatalog = if(provider.isNil) {
				CQAudioDeviceProbe.scan
			} {
				CQAudioDeviceCatalog.scan(provider)
			};
			audioDeviceCatalogState = \scanned;
			audioDeviceCatalogError = nil;
			success = true;
			("Yönpuoli: cached % input / % output audio devices "
				++ "from a stopped-server discovery.").format(
					audioDeviceCatalog[\inputs].size,
					audioDeviceCatalog[\outputs].size).postln;
		} { |error|
			audioDeviceCatalogState = if(audioDeviceCatalog.isNil) {
				\failed
			} {
				\stale
			};
			audioDeviceCatalogError = error.errorString;
			("Yönpuoli: device scan failed while server was stopped — "
				++ "continuing with configured/cached devices: "
				++ audioDeviceCatalogError).warn;
		};
		^success
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

	// A clean Audio page still needs a recovery operation. Normally it restarts
	// the exact active configuration. If that session is a temporary base
	// fallback, it reloads and retries the preserved preferred profile instead;
	// the stopped-boundary fresh-process scan decides whether the device has
	// returned. This remains separate from CQAudioReconfigure: there is no new
	// draft candidate to commit.
	*restartCurrentAudio {
		var preparedConfig, base, startupRecovery, startupData;
		var path = this.configFilePath;
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
			if(audioFallbackActive) {
				startupData = this.prLoadStartupAudio(path);
				preparedConfig = startupData[\config];
				base = startupData[\base];
				startupRecovery = startupData[\recovery];
			} {
				preparedConfig = CQConfig.validate(config).copy;
				base = this.prLoadBaseConfig(path);
				startupRecovery = CQAudioBootRecovery(
					base, preparedConfig, CQAudioConfig.profilePathFor(path));
			};
		} { |failure|
			audioReconfigureState = \failed;
			audioReconfigureError = failure.errorString;
			^false
		};
		audioTransaction = nil;
		audioConfigModel = nil;
		audioBootRecovery = startupRecovery;
		audioReconfigureState = \restarting;
		audioReconfigureError = nil;
		audioReconfigureResult = (
			status: \restarting,
			error: nil,
			cleanRestart: true
		);
		this.prRestartAudioConfig(preparedConfig, {
			if(audioBootRecovery.notNil and: {
				#[fallbackReady, rollbackReady, notSaved].includes(
					audioBootRecovery.state)
			}) {
				// Startup recovery already published its more specific truth.
				audioReopenPending = true;
			} {
				audioReconfigureState = \ready;
				audioReconfigureError = nil;
				audioReconfigureResult = (
					status: \ready,
					error: nil,
					cleanRestart: true
				);
				// This callback runs before the rebuilt GUI is constructed.
				audioReopenPending = true;
			};
		}, { |failure|
			if(audioBootRecovery.isNil or: {
				audioBootRecovery.state != \failed
			}) {
				audioReconfigureState = \failed;
				audioReconfigureError = failure.errorString;
				audioReconfigureResult = (
					status: \failed,
					error: failure.errorString,
					cleanRestart: true
				);
				("Yönpuoli: Audio restart failed — "
					++ audioReconfigureError).warn;
			};
		}, startupRecovery);
		^true
	}

	// UI wording names discovery, while lifecycle ownership remains exactly the
	// same confirmed strong restart as the clean Audio path above.
	*refreshAudioDevices { ^this.restartCurrentAudio }

	*retryAudioProfileSave {
		if(audioTransaction.notNil) { ^audioTransaction.retrySave };
		if(audioBootRecovery.notNil and: {
			audioBootRecovery.state == \notSaved
		}) {
			if(audioBootRecovery.retrySave) {
				audioProfilePresent = true;
				this.prPublishAudioReconfigure(audioBootRecovery.result);
				("Yönpuoli: recovered base Audio profile saved; rejected startup "
					++ "profile remains in the .bak sibling.").postln;
				^true
			};
			this.prPublishAudioReconfigure(audioBootRecovery.result);
		};
		^false
	}

	// ---- private ----

	*prLoadBaseConfig { |path|
		^if(File.exists(path.asString)) {
			CQConfig.load(path)
		} {
			("Yönpuoli: % not found — using defaults "
				++ "(copy config.example.scd to config.scd)").format(path).warn;
			CQConfig.validate(())
		}
	}

	// Initial startup treats a generated profile as a recoverable candidate, not
	// as permission to brick the only UI which can edit it. A malformed generated
	// file and a valid-but-different audio route both retain the hand-maintained
	// base as their one fallback. With no different generated profile, the same
	// coordinator still provides the fresh-catalog fail-fast preflight. Prepared
	// reconfigure boots bypass this loader: CQAudioReconfigure already owns their
	// explicit candidate/rollback pair.
	*prLoadStartupAudio { |path|
		var base = this.prLoadBaseConfig(path);
		var profilePath = CQAudioConfig.profilePathFor(path);
		var profile, candidate = base, recovery;
		audioProfilePresent = CQAudioProfileStore.profileExists(profilePath);
		if(audioProfilePresent) {
			try {
				profile = CQAudioProfileStore.loadProfile(profilePath);
				candidate = CQAudioConfig.merge(base, profile);
				if(CQAudioBootRecovery.audioDiffers(base, candidate)) {
					recovery = CQAudioBootRecovery(base, candidate, profilePath)
				};
			} { |failure|
				// The generated sibling is replaceable; config.scd is not. Boot the
				// base and replace the broken sibling only after that route is ready.
				recovery = CQAudioBootRecovery(base, base, profilePath,
					forceFallback: true);
				recovery.beginFallback(failure, false);
				candidate = base;
			};
		};
		recovery = recovery ?? {
			CQAudioBootRecovery(base, candidate, profilePath)
		};
		// This is the last explicit valid preference, not necessarily the route
		// selected for the current session after device preflight.
		audioPreferredConfig = candidate.copy;
		^(config: candidate.copy, base: base.copy, recovery: recovery)
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
		sessionState, sessionMatrix, onReady, onFailure, beforeGui,
		startupRecovery|
		var completed = false, stage = \audioClock;
		try {
			this.prRequireSession(generation);
			this.prRequireSettledAudioClock(generation, sessionServer,
				sessionConfig[\sampleRate]);
			this.prRequireSession(generation);
			stage = \graphBuild;
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
			// Registered under the class, not the receiver: a session booted
			// through the CQ alias and freed through Cocoquantus (or the other
			// way round) would otherwise leave this registration behind.
			CmdPeriod.add(Cocoquantus);
			"Yönpuoli: ready — output fading in over 2 s.".postln;
			completed = true;
		} { |err|
			if((sessionGeneration == generation) and: {
				lifecycleState == \booting
			}) {
				if(this.prTryStartupAudioRecovery(generation, sessionServer,
					sessionGraph, err, stage, startupRecovery,
					onReady, onFailure, beforeGui).not) {
					this.prBootFailed(generation, sessionGraph, err, onFailure)
				};
			} {
				try { sessionGraph.free } { |cleanupError|
					("Yönpuoli: stale-session cleanup failed — "
						++ cleanupError.errorString).warn;
				};
			};
		};
		if(completed) { onReady.value(this) };
	}

	*prRequireSession { |generation|
		if((sessionGeneration != generation) or: { lifecycleState != \booting }) {
			Error("Yönpuoli: stale session generation %".format(generation)).throw;
		};
	}

	*prRollbackFailedBoot { |generation, sessionGraph, err|
		if(sessionGeneration != generation) { ^false };
		sessionGeneration = sessionGeneration + 1;
		lifecycleState = \failed;
		lifecycleError = CQAudioBootRecovery.errorText(err);
		CmdPeriod.remove(Cocoquantus);   // never `this`: see prPublish's law
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
			("Yönpuoli: boot rollback cleanup failed — "
				++ cleanupError.errorString).warn;
		};
		graph = nil;
		state = nil;
		matrix = nil;
		config = nil;
		^true
	}

	*prBootFailed { |generation, sessionGraph, err, onFailure|
		if(this.prRollbackFailedBoot(generation, sessionGraph, err).not) { ^this };
		("Yönpuoli: boot failed and rolled back — " ++ lifecycleError).warn;
		onFailure.value(err);
	}

	// A generated startup profile is a candidate, not a trap. Only device-open
	// and pre-graph clock failures may enter this one-shot route; once the base
	// attempt begins, any further failure is terminal and retains both reasons.
	*prTryStartupAudioRecovery { |generation, sessionServer, sessionGraph,
		failure, stage, recovery, onReady, onFailure, beforeGui|
		var fallbackConfig, path, result;
		if((sessionGeneration != generation) or: {
			lifecycleState != \booting
		}) { ^false };
		if(recovery.isNil or: { recovery.canFallback(stage).not }) { ^false };
		path = this.configFilePath;
		fallbackConfig = recovery.beginFallback(failure, true);
		audioFallbackActive = true;
		if(this.prRollbackFailedBoot(generation, sessionGraph, failure).not) {
			^false
		};
		audioBootRecovery = recovery;
		result = recovery.result;
		this.prPublishAudioReconfigure(result);
		("Yönpuoli: saved Audio profile rejected — %; restarting once with "
			++ "the base config device %.").format(
				recovery.candidateError,
				fallbackConfig[\device].asCompileString).warn;
		this.prRebootPrepared(sessionServer, path, fallbackConfig,
			{ |facade|
				this.prCompleteStartupAudioRecovery(recovery);
				beforeGui.value(facade)
			},
			{ |fallbackFailure|
				this.prStartupAudioFallbackFailed(
					recovery, fallbackFailure, onFailure)
			},
			nil,
			onReady);
		^true
	}

	*prStartupAudioFallbackFailed { |recovery, failure, onFailure|
		var failed = recovery.fallbackFailed(failure);
		audioFallbackActive = true;
		audioBootRecovery = recovery;
		lifecycleError = failed[\error];
		this.prPublishAudioReconfigure(failed);
		("Yönpuoli: startup Audio fallback failed — "
			++ failed[\error]).warn;
		onFailure.value(Error(failed[\error]));
		^failed
	}

	*prStartupAudioPreflightFailed { |recovery, onFailure|
		lifecycleState = \failed;
		audioFallbackActive = recovery.fallbackActive;
		lifecycleError = recovery.error;
		audioBootRecovery = recovery;
		config = nil;
		this.prRemoveServerBootAction;
		this.prPublishAudioReconfigure(recovery.result);
		("Yönpuoli: startup Audio preflight failed — %; "
			++ "the server was not started.").format(lifecycleError).warn;
		onFailure.value(Error(lifecycleError));
		^false
	}

	*prCompleteStartupAudioRecovery { |recovery|
		var result = recovery.complete;
		audioBootRecovery = recovery;
		audioConfigModel = nil;
		audioTransaction = nil;
		audioFallbackActive = result[\status] == \fallbackReady;
		audioProfilePresent = CQAudioProfileStore.profileExists(
			recovery.profilePath);
		this.prPublishAudioReconfigure(result);
		if(result[\status] == \fallbackReady) {
			("Yönpuoli: preferred Audio device is unavailable; base Audio is "
				++ "running temporarily and the preferred profile remains at %.")
				.format(recovery.profilePath).postln;
		} {
			if(result[\status] == \rollbackReady) {
				("Yönpuoli: malformed Audio profile repaired after base Audio "
					++ "reached ready; the rejected file is preserved at %.bak.")
					.format(recovery.profilePath).postln;
			} {
				("Yönpuoli: base Audio recovered, but the corrected profile was not "
					++ "saved — %").format(result[\error]).warn;
			};
		};
		^result
	}

	// CQ.reboot is deliberately the strong operation: it restarts Server.default
	// so boot-only options take effect. Because that also stops unrelated synths,
	// callers must invoke it explicitly; normal CQ.free/CQ.boot never quits scsynth.
	*prRebootServer { |targetServer, path, onReady, onFailure|
		var startupData, startupRecovery;
		path = path ?? { this.repoDir +/+ "config.scd" };
		try {
			startupData = this.prLoadStartupAudio(path);
			startupRecovery = startupData[\recovery];
		} { |err|
			lifecycleState = \failed;
			lifecycleError = err.errorString;
			("Yönpuoli: reboot config failed — " ++ lifecycleError).warn;
			onFailure.value(err);
			^this
		};
		audioBootRecovery = startupRecovery;
		this.prRebootPrepared(targetServer, path, startupData[\config],
			onReady, onFailure, nil, nil, startupRecovery);
	}

	*prRestartAudioConfig { |preparedConfig, onReady, onFailure,
		startupRecovery|
		var targetServer = server ?? { Server.default };
		var path = this.configFilePath;
		this.free({
			this.prRebootPrepared(targetServer, path, preparedConfig,
				onReady, onFailure, nil, nil, startupRecovery)
		});
	}

	*prServerPort { |targetServer|
		^(targetServer.addr !? { |addr| addr.port }) ? 57110
	}

	// Degrades to the pre-2026-07-31 behaviour if the query itself fails (no
	// netstat, another platform): the boot then simply attempts as it always
	// did. Silently skipping the check would be the lie; saying so is not.
	*prClearServerPort { |targetServer|
		var port = this.prServerPort(targetServer);
		// A test double must never reach the operating system through here. The
		// port it names is just a number, and if it ever matched a real one the
		// query would answer about the player's live server — a unit test could
		// then stop the instrument it is testing. Doubles opt in explicitly by
		// installing CQServerProcess.commandRunner, which answers from fixtures.
		if(targetServer.isKindOf(Server).not
			and: { CQServerProcess.commandRunner.isNil }) {
			^(status: \free)
		};
		^try {
			CQServerProcess.clearPort(port)
		} { |err|
			("Yönpuoli: could not ask who holds udp %".format(port)
				++ " — " ++ err.errorString
				++ " (booting without that evidence)").warn;
			(status: \free)
		}
	}

	*prRebootPrepared { |targetServer, path, preparedConfig, onReady, onFailure,
		deviceProvider, afterReady, startupRecovery|
		var optionGraph, generation, action, fail, bootAfterForcedQuit, handoffBoot;
		var forcedQuitPolls = 0, prepareLaunch, launchBoot, pollForcedQuit;
		var forcedClearance, handoffPolls = 0;
		path = path ?? { this.repoDir +/+ "config.scd" };
		fail = { |failure|
			this.prRemoveServerBootAction;
			lifecycleState = \failed;
			lifecycleError = failure.errorString;
			("Yönpuoli: server reboot failed — " ++ lifecycleError).warn;
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
		handoffBoot = {
			AppClock.sched(0.01, {
				if((sessionGeneration != generation) or: {
					lifecycleState != \booting
				}) {
					nil
				} {
					if(targetServer.serverRunning) {
						lifecycleState = \stopped;
						this.boot(path, afterReady, preparedConfig,
							onFailure, onReady, startupRecovery);
						nil
					} {
						handoffPolls = handoffPolls + 1;
						if(handoffPolls >= 300) {
							fail.value(Error(
								"server did not finish boot finalization"));
							nil
						} {
							0.01
						}
					}
				}
			});
		};
		action = { |bootedServer|
			if(bootedServer === targetServer) {
				ServerBoot.remove(action, targetServer);
				serverBootAction = nil;
				if((sessionGeneration == generation) and: {
					lifecycleState == \booting
				}) {
					// ServerBoot precedes ServerStatus's notified/running truth.
					// Wait for that finalization before waitForBoot is entered, or
					// Server would log a redundant "already booting" attempt.
					handoffBoot.value;
				};
			};
		};
		serverBootAction = action;
		ServerBoot.add(action, targetServer);
		prepareLaunch = {
			var freshCapture = this.prCaptureAudioDeviceCatalog(
				targetServer, deviceProvider);
			var accepted = true;
			startupRecovery !? {
				preparedConfig = startupRecovery.selectInitial(
					audioDeviceCatalog,
					if(freshCapture) { audioDeviceCatalogState } { \unavailable });
				audioFallbackActive = startupRecovery.fallbackActive;
				audioBootRecovery = startupRecovery;
				if(startupRecovery.preflightFailed) {
					this.prStartupAudioPreflightFailed(
						startupRecovery, onFailure);
					accepted = false;
				} {
					try {
						preparedConfig = CQConfig.validate(preparedConfig).copy;
						optionGraph = CQGraph(targetServer, preparedConfig);
						optionGraph.applyOptions;
						config = preparedConfig;
					} { |failure|
						fail.value(failure);
						accepted = false;
					};
				};
			};
			accepted
		};
		launchBoot = {
			if((sessionGeneration == generation) and: {
				lifecycleState == \booting
			}) {
				if(prepareLaunch.value) {
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
		};
		// SOAK-1 finding C (measured 2026-07-31): the language's bookkeeping is
		// not evidence that the process died. Server:quit's forced stop killed
		// the pid it had recorded — 6012 — while the real server, 17716, kept
		// UDP 57110 and every following boot had no port to bind. The socket is
		// the only witness, so the port is polled here and a surviving holder is
		// stopped through CQServerProcess, which verifies the image first.
		pollForcedQuit = {
			AppClock.sched(0.05, {
				var clearance;
				if((sessionGeneration != generation) or: {
					lifecycleState != \booting
				}) {
					nil
				} {
					if(targetServer.applicationRunning
						or: { targetServer.serverRunning }
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
						clearance = this.prClearServerPort(targetServer);
						switch(clearance[\status],
							\free, {
								launchBoot.value;
								nil
							},
							\stopped, {
								// the OS releases the socket, not us: poll on
								forcedQuitPolls = forcedQuitPolls + 1;
								if(forcedQuitPolls >= 60) {
									fail.value(Error(
										"forced-stopped server kept its port"));
									nil
								} {
									0.05
								}
							},
							{
								fail.value(Error(CQServerProcess.refusalMessage(
									this.prServerPort(targetServer), clearance)));
								nil
							}
						)
					}
				}
			});
		};
		bootAfterForcedQuit = { pollForcedQuit.value };
		try {
			if(targetServer.unresponsive) {
				// Server.reboot skips quit when statusWatcher.unresponsive is
				// true, then starts a second process on the occupied port, so the
				// forced path is taken here — but not Server:quit's blind kill.
				//
				// SOAK-1 finding C: that kill aims at the pid `unixCmd` returned
				// when the server was launched (Server.sc:1042), which on Windows
				// is the launching shell. Measured 2026-07-31: it reported
				// "SUCCESS: The process with PID 6012 has been terminated" while
				// the server was 17716, which kept UDP 57110 and left every later
				// boot without a port. So we stop the verified holder of the port
				// ourselves first, and then clear the recorded pid so the library
				// can no longer fire a second, unverified kill at whatever now
				// owns that number. Nil-ing it also makes Server:applicationRunning
				// and its pid-release wait answer honestly instead of about a
				// process nobody owns.
				forcedClearance = this.prClearServerPort(targetServer);
				if(#[foreign, unverified].includes(forcedClearance[\status])) {
					fail.value(Error(CQServerProcess.refusalMessage(
						this.prServerPort(targetServer), forcedClearance)));
				} {
					if(targetServer.respondsTo(\pid)) {
						targetServer.instVarPut(\pid, nil);
					};
					targetServer.quit(bootAfterForcedQuit, {
						if(sessionGeneration == generation) {
							fail.value(Error(
								"unresponsive server forced quit failed"));
						};
					}, false);
				};
			} {
				if(startupRecovery.notNil) {
					if(targetServer.applicationRunning or: {
						targetServer.serverRunning
					}) {
						targetServer.quit(launchBoot, {
							if(sessionGeneration == generation) {
								fail.value(Error(
									"server quit before startup recovery failed"));
							};
						});
					} {
						launchBoot.value;
					};
				} {
					targetServer.reboot({ prepareLaunch.value }, {
						if(sessionGeneration == generation) {
							fail.value(Error("server reboot failed"));
						};
					});
				};
			};
		} { |err|
			if(sessionGeneration == generation) { fail.value(err) };
		};
	}

	*prPublishAudioReconfigure { |result|
		audioReconfigureResult = result.copy;
		audioReconfigureState = result[\status];
		audioReconfigureError = result[\error];
		if(audioReconfigureState == \ready and: { config.notNil }) {
			audioPreferredConfig = config.copy;
			audioFallbackActive = false;
		};
		if(audioReconfigureState == \fallbackReady) {
			audioFallbackActive = true;
		};
		if(audioReconfigureState == \rollbackReady and: {
			result[\startupFallback] == true
		}) {
			// This is the corrupt-profile repair path, not temporary absence.
			audioFallbackActive = false;
			audioPreferredConfig = config !? { |current| current.copy };
		};
		if(#[ready, notSaved, fallbackReady, rollbackReady].includes(
			audioReconfigureState)) {
			audioConfigModel !? {
				audioProfilePresent = audioConfigModel.profilePresent
			};
			if(guiPanel.isNil) { audioReopenPending = true };
		};
		if(audioReconfigureState == \failed
			and: { result[\startupFallback] != true }) {
			if(result[\rollbackError].notNil) {
				("Yönpuoli: Audio candidate failed: %; rollback failed: %. "
					++ "Base/profile: % / %. Output remains silent. Recovery: CQ.reboot")
					.format(result[\candidateError], result[\rollbackError],
						this.configFilePath,
						CQAudioConfig.profilePathFor(this.configFilePath)).warn;
			} {
				("Yönpuoli: Audio reconfigure failed — "
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
		this.prStartAudioClockWatch(sessionServer, generation);
	}

	*prUnwatchServer {
		if(serverQuitAction.notNil and: { server.notNil }) {
			ServerQuit.remove(serverQuitAction, server);
		};
		serverQuitAction = nil;
		this.prStopAudioClockWatch;
	}

	// AUD-W (PLAN.md "AUD-W:n sopimus"). A read-only runtime guard: it reads the
	// value the existing status watcher already fills from /status replies, so it
	// adds no server traffic, and it must NEVER reach for a device listing — that
	// is the isolated 2026-07-29 wedge. Low frequency by design; a healthy clock
	// changes nothing and moves no ink. No automatic recovery: an instrument that
	// reboots itself mid-performance is worse than an honest error state.
	*audioClockWatchPeriod { ^CQAudioClockWatch.watchPeriod }

	// `requestedRate`/`period` exist for hermetic tests only: a test drives the
	// tick itself with period 0 rather than waiting on a real clock. Production
	// passes neither and gets the session's own rate and the 2.5 s period.
	*prStartAudioClockWatch { |sessionServer, generation, requestedRate, period|
		period = period ?? {
			sessionServer.tryPerform(\cqAudioClockWatchPeriod)
				? this.audioClockWatchPeriod
		};
		requestedRate = requestedRate ?? { config !? { |cfg| cfg[\sampleRate] } };
		this.prStopAudioClockWatch;
		audioClockWatch = CQAudioClockWatch(requestedRate);
		if(period <= 0) { ^this }; // hermetic tests drive prAudioClockTick directly
		audioClockRoutine = Routine {
			var live = true;
			while { live } {
				period.wait;
				if((sessionGeneration != generation)
					or: { lifecycleState != \running }
					or: { audioClockWatch.isNil }) {
					live = false;
				} {
					this.prAudioClockTick(sessionServer, Main.elapsedTime);
				};
			};
		}.play(AppClock);
		^this
	}

	// Stopping the guard must retract a danger it published. The first version
	// cleared the state silently, on the assumption that a stop only ever
	// happens while the GUI is going away too — false for a restarted guard,
	// which left the toolbar claiming a fault the model no longer claimed (user
	// review 2026-07-31). A state that can be entered but not left is a defect
	// however rare the route out is.
	*prStopAudioClockWatch {
		var wasAlert = this.audioClockAlert;
		audioClockRoutine !? { |routine| routine.stop };
		audioClockRoutine = nil;
		audioClockWatch = nil;
		audioClockState = \ok;
		// The same law now covers both marks: whatever was published must be
		// retracted, or the toolbar keeps claiming a fault the model dropped.
		if(wasAlert != \none) { this.prPublish(\audioClock, \none) };
		^this
	}

	// Only a change of the *visible* truth is announced. `\watching` is the
	// converging-transient state and must stay invisible, or the guard becomes
	// the false alarm it exists to avoid.
	*prAudioClockTick { |targetServer, time|
		var rate, next, responding, wasAlert = this.audioClockAlert;
		if(audioClockWatch.isNil) { ^\ok };
		audioClockTickCount = audioClockTickCount + 1;
		responding = this.prServerAnswering(targetServer);
		rate = targetServer.tryPerform(\actualSampleRate);
		next = audioClockWatch.push(rate, time ? Main.elapsedTime, responding);
		audioClockState = next;
		if(this.audioClockAlert != wasAlert) {
			switch(this.audioClockAlert,
				\clockStuck, {
					("Yönpuoli: audio clock stuck — requested % Hz, server reports %."
						++ " The sound cannot be trusted; restart audio from the Audio"
						++ " page. No automatic recovery by design.").format(
							audioClockWatch.requestedRate, rate ? "nothing").warn;
				},
				\serverDown, {
					("Yönpuoli: the server has stopped answering — its last"
						++ " reading is not evidence of anything. Restart audio from"
						++ " the Audio page. No automatic recovery by design.").warn;
				},
				{ "Yönpuoli: audio recovered.".postln }
			);
			this.prPublish(\audioClock, this.audioClockAlert);
		};
		^next
	}

	// `CQ : Cocoquantus {}` is an alias subclass, so inside any class method
	// reached through `CQ.boot` the receiver is CQ — and `this.changed(...)`
	// then publishes on CQ while every listener subscribes to Cocoquantus. The
	// notification simply vanished (user review 2026-07-31: the guard printed
	// its warning, the model said \serverDown, and the toolbar never heard).
	// AUD-W's part 1 had passed because the checklist drove it through
	// `Cocoquantus.prStartAudioClockWatch`, i.e. by the other name.
	//
	// **A class-side notification must name the class, never `this`.** One door
	// so it cannot drift back.
	*prPublish { |what, value|
		Cocoquantus.changed(what, value);
	}

	// SOAK-1 finding B: a stale reading is not a healthy one. A double that
	// answers neither question is not evidence of a dead server either — absent
	// telemetry is not the same as bad telemetry (GUI_SPEC §4.5), so only an
	// explicit "not running" or "unresponsive" counts as silence.
	*prServerAnswering { |targetServer|
		var running = targetServer.tryPerform(\serverRunning);
		var mute = targetServer.tryPerform(\unresponsive);
		if(running.isNil and: { mute.isNil }) { ^true };
		^(running != false) and: { mute != true }
	}

	// One symbol for the one always-visible surface. The words differ because
	// the faults differ (user decision 2026-07-31): a stuck clock and a silent
	// server are not the same thing, and a mark that said "audio clock stuck"
	// about a dead server would be the next lie.
	*audioClockAlert {
		if(audioClockState == \stale) { ^\serverDown };
		if(audioClockState == \danger) { ^\clockStuck };
		^\none
	}

	*audioClockDanger { ^(audioClockState == \danger) }
	*audioClockRatio { ^audioClockWatch !? { |watch| watch.lastRatio } }
	*audioClockRate { ^audioClockWatch !? { |watch| watch.lastRate } }
	*prAudioClockWatchRunning {
		^audioClockRoutine.notNil and: { audioClockRoutine.isPlaying }
	}

	*prUnexpectedServerQuit {
		sessionGeneration = sessionGeneration + 1;
		lifecycleState = \failed;
		lifecycleError = "scsynth quit unexpectedly";
		CmdPeriod.remove(Cocoquantus);   // never `this`: see prPublish's law
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
		("Yönpuoli: scsynth quit unexpectedly; session invalidated. "
			++ "Run CQ.reboot to rebuild.").warn;
	}

	*prCocoParam { |n, suffix|
		if(#[1, 2].includes(n).not) {
			"Yönpuoli: coco index must be 1 or 2 (got %).".format(n).warn;
			^nil
		};
		^("coco" ++ n ++ suffix).asSymbol
	}

	*prCastleSynth { |k|
		if(#[1, 2, 3, 4, 5].includes(k).not) {
			"Yönpuoli: castle index must be 1..5 (got %).".format(k).warn;
			^nil
		};
		if(this.isBooted.not) { "Yönpuoli: not booted.".warn; ^nil };
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
				("Yönpuoli: WATCHDOG — driving hard > 5 s, auto-trim now % dB "
					++ "(CQ.resetTrim to restore).").format(trimDb).warn;
			} {
				// notice-only (default): the limiter already keeps it safe; auto-trim
				// is opt-in (config autoTrim: true) — a comfort ease-off, not safety
				("Yönpuoli: watchdog — driving the master hard (> 5 s). Limiter has "
					++ "it safe; config autoTrim: true adds an automatic −6 dB ease-off.").postln;
			};
		}, '/cq_hot');
	}

	// SPEC §7 mic warning (headless part; the GUI adds color in phase 6):
	// warn once per 0 → live transition, through any entry point
	*prInitMicWatch {
		micWatcher = SimpleController(state);
		// A bulk write must not be able to smuggle a live mic past the warning:
		// both doors into CQState reach this watcher (SOAK-1 finding A).
		micWatcher.put(\params, { |obj, what, event|
			event[\micGain] !? { |val| this.prMicGainChanged(val) };
		});
		micWatcher.put(\param, { |obj, what, name, val|
			if(name == \micGain) { this.prMicGainChanged(val) };
		});
	}

	*prMicGainChanged { |val|
		if(val.isNil) { ^this };
		if((val > 0) and: { micWasLive.not }) {
			micWasLive = true;
			("Yönpuoli: micGain > 0 — the mic is LIVE. Feedback risk "
				++ "with builtin speakers; CQ.set(\\micGain, 0) to mute.").warn;
		};
		if(val <= 0) { micWasLive = false };
		^this
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
			{ "Yönpuoli: \\cq event — unknown action '%' ignored.".format(action).warn }
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
				{ "Yönpuoli: \\cq deck op '%' ignored.".format(ev[\do]).warn }
			);
		};
	}
}

// short alias: CQ.boot, CQ.set(...), CQ.panic — classvars shared with Cocoquantus
CQ : Cocoquantus {}
