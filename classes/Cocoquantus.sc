// Cocoquantus — facade singleton (SPEC §6.3). Owns the boot lifecycle; all
// state flows through CQState, all server objects live in CQGraph.
// CQ (bottom of file) is the short alias — classvars are shared.

Cocoquantus {
	classvar <config, <graph, <state, <matrix, <server;
	classvar configPath, deckAPlayer;

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
		server.waitForBoot {
			graph.build;                       // buses + groups, synced
			CQSynthDefs.send(config, server);
			server.sync;
			graph.spawn;                       // synths in group order, synced
			state.pushAll;                     // defaults + power-on state
			matrix.pushAll;                    // matrix empty (SPEC boot sequence)
			server.sync;
			deckAPlayer = CQDeck(graph, state, \deckA, this.prSamplesDir);
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

	*patchPreset { |name|
		if(matrix.isNil) { "Cocoquantus: not booted.".warn; ^this };
		matrix.preset(name);
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
			if(graph.isRunning) {
				state.set(\monitorMute, 1);    // 30 ms fade before teardown
				0.15.wait;
			};
			deckAPlayer !? { deckAPlayer.free };
			deckAPlayer = nil;
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

	*prSamplesDir {
		var dir = config[\folders][\samples].asString;
		^if(PathName(dir).isAbsolutePath) { dir } { this.repoDir +/+ dir }
	}
}

// short alias: CQ.boot, CQ.set(...), CQ.panic — classvars shared with Cocoquantus
CQ : Cocoquantus {}
