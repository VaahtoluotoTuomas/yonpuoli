// CQStandaloneApp -- pure decisions shared by the one-click application script
// and its serverless tests. The script owns the sclang/scsynth process pair;
// ordinary IDE use keeps Cocoquantus's intentionally independent GUI lifecycle.

CQStandaloneApp {
	*readyMarker { ^"YONPUOLI_APP_READY" }
	*validationMarker { ^"CQ_YONPUOLI_APP_SCRIPT_OK" }

	// The one-click application must not attach to an IDE session or an orphaned
	// scsynth on the conventional 57110 port. Its launcher reserves a free port
	// and hands it over as plain environment data before this class is loaded.
	*serverPortFrom { |value|
		var port;
		if(value.isNil or: { value.asString.isEmpty }) {
			Error("YONPUOLI_SERVER_PORT is missing").throw
		};
		port = value.asString.asInteger;
		if(port.inclusivelyBetween(1024, 65535).not) {
			Error("YONPUOLI_SERVER_PORT is invalid: %".format(value)).throw
		};
		^port
	}

	// A confirmed Audio restart closes one GUI while changing generation. Adopt
	// the replacement instead of treating that internal rebuild as an app quit.
	// Only a closed GUI in the same running generation means the user closed the
	// standalone application.
	*pollAction { |lifecycle, generation, watchedGeneration, guiClosed = false|
		if(lifecycle == \failed) { ^\exitFailed };
		if(lifecycle != \running) { ^\wait };
		if(generation != watchedGeneration) { ^\adoptGui };
		if(guiClosed) { ^\exitClean };
		^\wait
	}
}
