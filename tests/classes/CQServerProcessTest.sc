// CQServerProcessTest — SOAK-1 finding C. The measured 2026-07-31 case is the
// first test: a hung scsynth keeping UDP 57110 while the language's recorded
// pid (6012) belonged to something else entirely.

CQServerProcessTest : UnitTest {

	// The exact shape `netstat -ano -p UDP` printed during the incident, with
	// the language's own port on the neighbouring row so the match has to be
	// by port and not by "the first UDP line".
	*measuredNetstat {
		^"\nAktiiviset yhteydet\n\n"
		"  Yhteys  Paikallinen osoite     Ulkoinen osoite        PID\r\n"
		"  UDP     0.0.0.0:57120          *:*                    2696\r\n"
		"  UDP     127.0.0.1:57110        *:*                    17716\r\n"
	}

	*measuredTasklist {
		^"\"scsynth.exe\",\"17716\",\"Console\",\"1\",\"92,480 K\"\r\n"
	}

	test_parsePortHolder_finds_the_owner_of_that_exact_port {
		this.assertEquals(
			CQServerProcess.parsePortHolder(this.class.measuredNetstat, 57110),
			17716,
			"the holder of 57110 is read from the measured netstat output"
		);
		this.assertEquals(
			CQServerProcess.parsePortHolder(this.class.measuredNetstat, 57120),
			2696,
			"a different port matches a different row, not the first UDP row"
		);
	}

	test_parsePortHolder_is_nil_when_the_port_is_free {
		this.assertEquals(
			CQServerProcess.parsePortHolder(this.class.measuredNetstat, 57111),
			nil,
			"an unheld port has no owner"
		);
		this.assertEquals(
			CQServerProcess.parsePortHolder("", 57110), nil,
			"empty output is not an owner"
		);
		this.assertEquals(
			CQServerProcess.parsePortHolder(nil, 57110), nil,
			"a failed query is not an owner"
		);
	}

	test_parsePortHolder_does_not_match_a_port_that_is_only_a_suffix {
		var text = "  UDP     127.0.0.1:157110       *:*                    999\r\n";
		this.assertEquals(
			CQServerProcess.parsePortHolder(text, 57110), nil,
			"157110 is not 57110 — the suffix must follow the colon"
		);
	}

	test_parseImageName_reads_the_row_for_that_pid {
		this.assertEquals(
			CQServerProcess.parseImageName(this.class.measuredTasklist, 17716),
			"scsynth.exe",
			"the image name comes from the row whose pid matches"
		);
		this.assertEquals(
			CQServerProcess.parseImageName(this.class.measuredTasklist, 6012),
			nil,
			"a row for another pid never answers for this one"
		);
		this.assertEquals(
			CQServerProcess.parseImageName(
				"INFO: No tasks are running which match the specified criteria.", 17716),
			nil,
			"tasklist's empty answer is not an image name"
		);
	}

	test_isServerImage_accepts_only_a_server_binary {
		this.assert(CQServerProcess.isServerImage("scsynth.exe"),
			"scsynth is a server");
		this.assert(CQServerProcess.isServerImage("C:\\SC\\scsynth.exe"),
			"a full path is still scsynth");
		this.assert(CQServerProcess.isServerImage("SCSYNTH.EXE"),
			"the comparison is case-insensitive");
		this.assert(CQServerProcess.isServerImage("supernova.exe"),
			"supernova is a server too");
		this.assert(CQServerProcess.isServerImage("sclang.exe").not,
			"the interpreter is not a server");
		this.assert(CQServerProcess.isServerImage("chrome.exe").not,
			"an unrelated program is not a server");
		this.assert(CQServerProcess.isServerImage(nil).not,
			"an unknown image is not a server");
	}

	// The whole point of the class: what it does, and what it refuses to do.
	test_clearPort_stops_a_verified_server_and_reports_it {
		var commands = List.new;
		var previous = CQServerProcess.commandRunner;
		var clearance;
		CQServerProcess.commandRunner = { |cmd|
			commands.add(cmd);
			case
			{ cmd.contains("netstat") } { this.class.measuredNetstat }
			{ cmd.contains("tasklist") } { this.class.measuredTasklist }
			{ true } { "" }
		};
		clearance = CQServerProcess.clearPort(57110);
		CQServerProcess.commandRunner = previous;
		this.assertEquals(clearance[\status], \stopped,
			"a verified scsynth holding the port is stopped");
		this.assertEquals(clearance[\pid], 17716,
			"the pid stopped is the port's owner, not the recorded one");
		this.assert(commands.any { |c| c.contains("taskkill") and: {
			c.contains("17716")
		} }, "the kill names the verified pid");
		this.assert(commands.any { |c| c.contains("6012") }.not,
			"the language's recorded pid is never involved");
	}

	test_clearPort_refuses_to_kill_a_process_that_is_not_a_server {
		var commands = List.new;
		var previous = CQServerProcess.commandRunner;
		var clearance;
		CQServerProcess.commandRunner = { |cmd|
			commands.add(cmd);
			case
			{ cmd.contains("netstat") } { this.class.measuredNetstat }
			{ cmd.contains("tasklist") } {
				"\"chrome.exe\",\"17716\",\"Console\",\"1\",\"92,480 K\"\r\n"
			}
			{ true } { "" }
		};
		clearance = CQServerProcess.clearPort(57110);
		CQServerProcess.commandRunner = previous;
		this.assertEquals(clearance[\status], \foreign,
			"a recycled pid belonging to another program is refused");
		this.assert(commands.any { |c| c.contains("taskkill") }.not,
			"nothing is killed when the image is not a server");
		this.assert(
			CQServerProcess.refusalMessage(57110, clearance).contains("chrome.exe"),
			"the refusal names the holder"
		);
	}

	test_clearPort_refuses_when_the_holder_cannot_be_identified {
		var commands = List.new;
		var previous = CQServerProcess.commandRunner;
		var clearance;
		CQServerProcess.commandRunner = { |cmd|
			commands.add(cmd);
			if(cmd.contains("netstat")) { this.class.measuredNetstat } { "" }
		};
		clearance = CQServerProcess.clearPort(57110);
		CQServerProcess.commandRunner = previous;
		this.assertEquals(clearance[\status], \unverified,
			"an unidentifiable holder is never a valid target");
		this.assert(commands.any { |c| c.contains("taskkill") }.not,
			"an unverified pid is not killed");
	}

	test_clearPort_is_free_when_nobody_holds_the_port {
		var previous = CQServerProcess.commandRunner;
		var clearance;
		CQServerProcess.commandRunner = { |cmd|
			if(cmd.contains("netstat")) { "  UDP  0.0.0.0:57120  *:*  2696\r\n" } { "" }
		};
		clearance = CQServerProcess.clearPort(57110);
		CQServerProcess.commandRunner = previous;
		this.assertEquals(clearance[\status], \free,
			"a free port lets the boot proceed");
	}

	test_commands_name_the_pid_they_target {
		this.assert(CQServerProcess.imageQueryCommand(17716).contains("17716"),
			"the image query asks about that pid");
		this.assert(CQServerProcess.stopCommand(17716).contains("17716"),
			"the stop command names that pid");
		this.assert(CQServerProcess.stopCommand(17716).contains("/F"),
			"the stop is forced — the process is hung, not asleep");
	}
}
