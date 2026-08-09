// CQServerProcess — the only door that force-stops a server process, and the
// only place that asks the operating system who holds the server's port.
//
// Why it exists (measured 2026-07-31, SOAK-1 finding C). After a sleep the
// scsynth process was alive but its audio thread was dead and it answered no
// /status at all, so `restart audio` took Server:quit's forced path. That path
// kills the pid the language recorded when it launched the server — and on
// Windows that pid is not necessarily scsynth. The run reported
// "SUCCESS: The process with PID 6012 has been terminated" while the live
// server was 17716, which kept UDP 57110 open; every following boot had no
// port to bind, and the instrument never came back.
//
// Two laws follow, and they are structure here rather than convention:
//
//   1. **The port is the evidence.** A server is stopped when its socket is
//      free, not when a kill command printed SUCCESS. `applicationRunning`,
//      `serverRunning` and `unresponsive` are the language's bookkeeping about
//      a process it may not even own; the socket is the operating system's.
//   2. **Nothing is force-killed before its image name is verified.** A pid the
//      language remembers can have been recycled by an unrelated program, and
//      terminating that is a far worse failure than refusing to restart.
//
// Everything except `prRun` is pure and unit-tested; the OS is touched only
// from the unresponsive-restart path, never while the instrument plays.

CQServerProcess {

	// Injection door for tests: a Function of one String (the command) that
	// answers the command's stdout. nil = talk to the real operating system.
	classvar <>commandRunner;

	// Symbols, never Strings: Collection:includes compares identity, so a table
	// of Strings would silently never match (AGENTS.md, durable rules).
	*serverImageNames { ^#[scsynth, supernova] }

	// UDP because that is the port scsynth binds for OSC; the language's own
	// port shows up in the same table and is filtered out by the port match.
	*portQueryCommand { ^"netstat -ano -p UDP" }

	*imageQueryCommand { |pid|
		^"tasklist /NH /FO CSV /FI \"PID eq %\"".format(pid.asInteger)
	}

	*stopCommand { |pid|
		^"taskkill /F /PID %".format(pid.asInteger)
	}

	// ---- pure parsing ----

	// A netstat UDP row is `UDP  127.0.0.1:57110  *:*  17716`. Matching by the
	// address suffix and taking the last token keeps this working when the
	// headers are localised, which they are on a Finnish Windows.
	*parsePortHolder { |text, port|
		var suffix = ":" ++ port.asInteger.asString;
		var found = nil;
		if(text.isNil) { ^nil };
		text.asString.split($\n).do { |rawLine|
			var line, words, pid;
			if(found.isNil) {
				line = rawLine.replace("\r", "");
				words = line.split($ ).reject { |w| w.isEmpty };
				if((words.size >= 4) and: { words[0].toUpper == "UDP" }) {
					if(words[1].endsWith(suffix)) {
						pid = words.last.asInteger;
						if(pid > 0) { found = pid };
					};
				};
			};
		};
		^found
	}

	// tasklist CSV: `"scsynth.exe","17716","Console","1","92,480 K"`. Only the
	// first two fields are read and neither can contain a comma, so a plain
	// split is safe here (the memory field's comma sits past both).
	*parseImageName { |text, pid|
		var target = pid.asInteger.asString;
		var found = nil;
		if(text.isNil) { ^nil };
		text.asString.split($\n).do { |rawLine|
			var line, fields, name, listed;
			if(found.isNil) {
				line = rawLine.replace("\r", "");
				if(line.beginsWith("\"")) {
					fields = line.split($,);
					if(fields.size >= 2) {
						name = fields[0].replace("\"", "").stripWhiteSpace;
						listed = fields[1].replace("\"", "").stripWhiteSpace;
						if(listed == target and: { name.isEmpty.not }) {
							found = name;
						};
					};
				};
			};
		};
		^found
	}

	// "C:\\path\\scsynth.exe" and "scsynth.exe" both answer \scsynth.
	*normalizeImage { |name|
		var base;
		if(name.isNil) { ^nil };
		base = name.asString.replace("\\", "/").basename.toLower;
		if(base.contains(".")) { base = base.split($.)[0] };
		if(base.isEmpty) { ^nil };
		^base.asSymbol
	}

	*isServerImage { |name|
		var symbol = this.normalizeImage(name);
		if(symbol.isNil) { ^false };
		^this.serverImageNames.includes(symbol)
	}

	// ---- the operating system ----

	*prRun { |command|
		var runner = commandRunner;
		if(runner.notNil) { ^runner.value(command) };
		^command.unixCmdGetStdOut
	}

	// nil when the port is free; otherwise (pid:, image:) where image may be nil
	// if the process could not be identified — an unidentified holder is never
	// a valid target (law 2).
	*portHolder { |port|
		var pid = this.parsePortHolder(this.prRun(this.portQueryCommand), port);
		if(pid.isNil) { ^nil };
		^(
			pid: pid,
			image: this.parseImageName(this.prRun(this.imageQueryCommand(pid)), pid)
		)
	}

	// The whole decision in one place, so the lifecycle call site stays a
	// switch over four honest outcomes:
	//   \free       — nobody holds the port, boot may proceed
	//   \stopped    — a verified server process was force-stopped; poll again,
	//                 because the socket is released by the OS, not by us
	//   \foreign    — something that is not a server holds it; refuse to kill
	//   \unverified — a holder exists but could not be identified; refuse too
	*clearPort { |port|
		var holder = this.portHolder(port);
		if(holder.isNil) { ^(status: \free) };
		if(holder[\image].isNil) {
			^(status: \unverified, pid: holder[\pid], image: nil)
		};
		if(this.isServerImage(holder[\image]).not) {
			^(status: \foreign, pid: holder[\pid], image: holder[\image])
		};
		this.prRun(this.stopCommand(holder[\pid]));
		^(status: \stopped, pid: holder[\pid], image: holder[\image])
	}

	// The message the player reads when a restart refuses to proceed. It names
	// the holder, because "restart failed" without a name is exactly the kind
	// of surface this whole finding was about.
	*refusalMessage { |port, clearance|
		^switch(clearance[\status],
			\foreign, {
				"udp port % is held by % (pid %), which is not a server process"
					.format(port, clearance[\image], clearance[\pid])
			},
			\unverified, {
				"udp port % is held by pid %, whose process could not be identified"
					.format(port, clearance[\pid])
			},
			{ "udp port % could not be cleared".format(port) }
		)
	}
}
