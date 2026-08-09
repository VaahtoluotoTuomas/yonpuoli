// Production entry point for YONPUOLI.bat. The launcher provides an isolated
// class library and owns this sclang process. Closing the application window
// therefore frees YONPUOLI, quits its audio server and exits the interpreter.

(
var validateOnly = "YONPUOLI_APP_VALIDATE_ONLY".getenv == "1";
var readyPath = "YONPUOLI_READY_FILE".getenv;
var serverPort;
var watchedGui, watchedGeneration = -1, monitor;
var finished = false, finish, publishReady, beginMonitor;

if(validateOnly) {
	CQStandaloneApp.validationMarker.postln;
	0.exit;
} {
	finish = { |exitCode = 0, error|
		if(finished.not) {
			finished = true;
			error !? {
				("YONPUOLI application failed: " ++ error.errorString).warn
			};
			Cocoquantus.free({
				Server.default.quit;
				AppClock.sched(1.0, {
					exitCode.asInteger.exit;
					nil
				});
			});
		};
	};

	try {
		serverPort = CQStandaloneApp.serverPortFrom(
			"YONPUOLI_SERVER_PORT".getenv);
		// A private Server.default keeps every existing IDE or stale 57110 server
		// outside this application's ownership and boot handshake.
		Server.default = Server(\yonpuoli,
			NetAddr("127.0.0.1", serverPort));
		("YONPUOLI audio server port: " ++ serverPort).postln;
	} { |error|
		error.reportError;
		1.exit;
	};

	publishReady = {
		if(readyPath.isNil or: { readyPath.isEmpty }) {
			Error("YONPUOLI_READY_FILE is missing").throw
		};
		File.use(readyPath, "w", { |file|
			file.write(CQStandaloneApp.readyMarker ++ Char.nl)
		});
		CQStandaloneApp.readyMarker.postln;
	};

	beginMonitor = {
		monitor = Routine({
			while({ finished.not }, {
				var action, closed;
				0.2.wait;
				closed = watchedGui.notNil and: { watchedGui.isClosed };
				action = CQStandaloneApp.pollAction(
					Cocoquantus.lifecycleState,
					Cocoquantus.sessionGeneration,
					watchedGeneration,
					closed);
				switch(action,
					\adoptGui, {
						watchedGeneration = Cocoquantus.sessionGeneration;
						watchedGui = Cocoquantus.gui;
					},
					\exitClean, { finish.value(0) },
					\exitFailed, {
						finish.value(1,
							Error(Cocoquantus.lifecycleError
								? "audio graph failed"))
					}
				);
			});
		}).play(AppClock);
	};

	Cocoquantus.boot(nil, { |facade|
		watchedGeneration = facade.sessionGeneration;
		watchedGui = facade.gui;
		try {
			publishReady.value;
			beginMonitor.value;
		} { |error| finish.value(1, error) };
	}, nil, { |error| finish.value(1, error) });
};
)
