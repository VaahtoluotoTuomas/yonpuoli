// Cocoquantus — facade singleton (SPEC §6.3). Owns the boot lifecycle; all
// state flows through CQState, all server objects live in CQGraph.
// CQ (bottom of file) is the short alias — classvars are shared.

Cocoquantus {
	classvar <config, <graph, <state, <server;
	classvar configPath;

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
		server.waitForBoot {
			graph.build;                       // buses + groups, synced
			CQSynthDefs.send(config, server);
			server.sync;
			graph.spawn;                       // synths in group order, synced
			state.pushAll;                     // defaults + power-on state
			server.sync;
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
			graph.free;
			graph = nil;
			state = nil;
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
}

// short alias: CQ.boot, CQ.set(...), CQ.panic — classvars shared with Cocoquantus
CQ : Cocoquantus {}
