// AUD-W. The hard case is not "is this reading wrong" — the boot gate already
// answers that — but "is this wrong reading a dying device or a machine waking
// up". Both measured series are here as regressions.

CQAudioClockWatchTest : UnitTest {

	prFeed { |watch, rates, period = 2.5, start = 0|
		var time = start;
		rates.do { |rate|
			watch.push(rate, time);
			time = time + period;
		};
		^watch.state
	}

	test_boundsAreTheBootGatesOwnDefinition {
		this.assertEquals(CQAudioClockWatch.ratioBounds,
			Cocoquantus.audioClockRatioBounds,
			"one bound serves the boot gate and the runtime guard");
		this.assert(
			Cocoquantus.prAudioClockHealthy(48000, 47986.317)
				== CQAudioClockWatch.healthy(48000, 47986.317),
			"the facade predicate delegates rather than duplicating");
		this.assert(CQAudioClockWatch.healthy(48000, 47986.317),
			"ordinary hardware-clock drift is healthy");
		this.assert(CQAudioClockWatch.healthy(48000, 2610.783).not,
			"the measured responsive system-default clock is unhealthy");
		this.assert(CQAudioClockWatch.healthy(48000, nil).not,
			"a missing reading is not a healthy reading");
		this.assertEquals(CQAudioClockWatch.errorOf(48000, nil), 1.0,
			"no reading counts as maximally wrong, not slightly wrong");
	}

	test_healthyClockNeverLeavesOk {
		var watch = CQAudioClockWatch(48000);
		var rates = 40.collect { 47986.317 + 30.0.rand2 };
		this.assertEquals(this.prFeed(watch, rates), \ok,
			"100 s of ordinary drift raises nothing");
		this.assertEquals(watch.streakCount, 0, "a healthy run holds no streak");
	}

	// Measured 2026-07-30: a Modern Standby wake travels outside BOTH
	// bounds before it settles. A naive three-sample check called this a fault
	// twice, in both directions, while nothing was broken.
	test_measuredStandbyWakeIsNotAFault {
		var watch = CQAudioClockWatch(48000);
		// Every reading here is outside the gate's bounds — 52 kHz would already
		// be healthy, and a healthy reading would end the streak for the wrong
		// reason and make this test prove nothing.
		var rates = [7419, 12000, 21000, 30000, 36000, 69184, 65000, 62000];
		this.assertEquals(this.prFeed(watch, rates, 11.25), \watching,
			"a converging transient stays watching however long it runs");
		this.assert(watch.streakSeconds > CQAudioClockWatch.dangerWindow,
			"and it does so well past the danger window, not by being short");
		watch.push(47986.317, 100);
		this.assertEquals(watch.state, \ok,
			"one healthy reading ends a transient that never became danger");
	}

	test_theThreeMeasuredSamplesAlone {
		var watch = CQAudioClockWatch(48000);
		this.assertEquals(this.prFeed(watch, [7419, 69184, 47986.317], 45), \ok,
			"the exact measured 7419 -> 69184 -> 47986 series ends healthy");
		this.assert(Cocoquantus.prAudioClockHealthy(48000, 7419).not
			and: { Cocoquantus.prAudioClockHealthy(48000, 69184).not },
			"both transient readings really are outside the boot gate's bounds");
	}

	test_stuckClockBecomesDanger {
		var watch = CQAudioClockWatch(48000);
		var rates = 30.collect { 2610.783 };
		this.assertEquals(this.prFeed(watch, rates), \danger,
			"the measured stuck system-default clock is finally said out loud");
		this.assert(watch.streakSeconds >= CQAudioClockWatch.dangerWindow,
			"danger is never declared before the whole window has passed");
	}

	test_oneBadReadingIsNotAFault {
		var watch = CQAudioClockWatch(48000);
		watch.push(48000, 0);
		this.assertEquals(watch.push(2610.783, 2.5), \watching,
			"a single unhealthy reading only starts watching");
		this.assertEquals(watch.push(47986.317, 5), \ok,
			"and a healthy reading clears it immediately");
	}

	test_shortStuckWindowIsNotYetDanger {
		var watch = CQAudioClockWatch(48000);
		var rates = 8.collect { 2610.783 };
		this.assertEquals(this.prFeed(watch, rates, 2.5), \watching,
			"20 s of a stuck clock is not yet the 60 s the contract requires");
	}

	test_tooFewReadingsCannotDeclareDanger {
		var watch = CQAudioClockWatch(48000);
		this.assertEquals(this.prFeed(watch, [2610.783, 2610.783, 2610.783], 40),
			\watching,
			"a long span of very few readings is not evidence of a stuck clock");
	}

	test_divergingClockIsDanger {
		var watch = CQAudioClockWatch(48000);
		var rates = [30000, 26000, 21000, 16000, 11000, 6000, 3000, 1500, 800, 400];
		this.assertEquals(this.prFeed(watch, rates, 8), \danger,
			"a clock running away from the requested rate is not converging");
	}

	test_missingReadingsAreDangerNotSilence {
		var watch = CQAudioClockWatch(48000);
		var rates = 30.collect { nil };
		this.assertEquals(this.prFeed(watch, rates), \danger,
			"a server that reports no rate at all for a minute is not healthy");
	}

	test_dangerNeedsSettledHealthToClear {
		var watch = CQAudioClockWatch(48000);
		this.prFeed(watch, 30.collect { 2610.783 });
		this.assertEquals(watch.state, \danger, "precondition: danger is set");
		watch.push(47986.317, 100);
		this.assertEquals(watch.state, \danger,
			"one good reading cannot clear danger — the boot gate's settle rule");
		watch.push(47986.317, 102.5);
		this.assertEquals(watch.state, \danger, "nor can two");
		watch.push(47986.317, 105);
		this.assertEquals(watch.state, \ok,
			"three settled healthy readings clear it");
	}

	test_noRequestedRateJudgesNothing {
		var watch = CQAudioClockWatch(nil);
		this.assertEquals(this.prFeed(watch, 40.collect { 2610.783 }), \ok,
			"without a requested rate there is nothing to compare against");
	}

	// ---- the facade's runtime guard (AUD-W lifecycle + announcement laws) ----

	// User review 2026-07-31: restarting the guard left the toolbar mark lit,
	// because the stop path cleared the state without saying so. A state that
	// can be entered but not left is a defect however rare the route out is.
	test_stoppingTheGuardRetractsItsDanger {
		var server = CQTestServer();
		var events = List.new;
		var listener = SimpleController(Cocoquantus);
		listener.put(\audioClock, { |obj, what, alert| events.add(alert) });
		Cocoquantus.prStartAudioClockWatch(server, Cocoquantus.sessionGeneration,
			48000, 0);
		server.actualSampleRate = 2610.783;
		30.do { |i| Cocoquantus.prAudioClockTick(server, i * 2.5) };
		this.assert(Cocoquantus.audioClockDanger, "precondition: danger is published");
		Cocoquantus.prStartAudioClockWatch(server, Cocoquantus.sessionGeneration,
			48000, 0);
		this.assert(Cocoquantus.audioClockDanger.not,
			"a restarted guard starts from ok");
		this.assertEquals(events.size, 2,
			"and it retracts the danger it published, so no surface stays lit");
		this.assertEquals(events.last, \none, "the retraction says the fault is over");
		Cocoquantus.prStopAudioClockWatch;
		this.assertEquals(events.size, 2,
			"stopping an already-ok guard announces nothing");
		listener.remove;
	}

	test_guardDoesNotOutliveTheSession {
		var server = CQTestServer();
		Cocoquantus.prStartAudioClockWatch(server, Cocoquantus.sessionGeneration);
		// AppClock schedules the routine; it is not running until it has ticked.
		0.2.wait;
		this.assert(Cocoquantus.prAudioClockWatchRunning,
			"the guard runs while the session does");
		Cocoquantus.prUnwatchServer;
		this.assert(Cocoquantus.prAudioClockWatchRunning.not,
			"every lifecycle exit runs through prUnwatchServer, so none leaks it");
		this.assertEquals(Cocoquantus.audioClockState, \ok,
			"a torn-down guard cannot keep claiming danger");
	}

	test_guardAnnouncesOnlyWhenTheVisibleTruthChanges {
		var server = CQTestServer();
		var events = List.new;
		var listener = SimpleController(Cocoquantus);
		listener.put(\audioClock, { |obj, what, alert| events.add(alert) });
		Cocoquantus.prStartAudioClockWatch(server, Cocoquantus.sessionGeneration,
			48000, 0);
		this.assert(Cocoquantus.prAudioClockWatchRunning.not,
			"period 0 leaves the tick to the test, with no clock of its own");
		server.actualSampleRate = 2610.783;
		30.do { |i| Cocoquantus.prAudioClockTick(server, i * 2.5) };
		this.assert(Cocoquantus.audioClockDanger,
			"a minute of a stuck clock reaches danger through the facade");
		this.assertEquals(events.size, 1,
			"danger is announced once, not on every tick — a state is not a stream");
		this.assertEquals(events[0], \clockStuck, "and it is announced by its own name");
		server.actualSampleRate = 47986.317;
		3.do { |i| Cocoquantus.prAudioClockTick(server, 100 + (i * 2.5)) };
		this.assertEquals(events.size, 2, "recovery is announced exactly once too");
		this.assertEquals(events[1], \none, "and it clears the mark");
		listener.remove;
		Cocoquantus.prStopAudioClockWatch;
	}

	// ---- SOAK-1 finding B: a stale reading is not a healthy one ----

	// The measured case, 2026-07-31: the server answered none of four probes and
	// its audio thread was dead (CPU frozen, threads 9 -> 7), while
	// actualSampleRate still read 48003.47925608 from before the sleep. The old
	// guard called that `ok`, because a ratio test on a frozen healthy value can
	// never fail.
	test_frozenReadingFromASilentServerIsNotHealth {
		var watch = CQAudioClockWatch(48000);
		this.assertEquals(watch.push(48003.47925608, 0), \ok,
			"precondition: while the server answers, this reading is healthy");
		20.do { |i| watch.push(48003.47925608, (i + 1) * 2.5, false) };
		this.assertEquals(watch.state, \stale,
			"the same number, no longer fresh, is not evidence of anything");
		this.assertEquals(watch.lastRate, nil,
			"and the stale value is forgotten, so no surface can draw it as measured");
		this.assertEquals(watch.lastRatio, nil, "nor its ratio");
	}

	test_silenceBreaksTheStuckClaimRatherThanFeedingIt {
		var watch = CQAudioClockWatch(48000);
		10.do { |i| watch.push(2610.783, i * 2.5) };
		this.assertEquals(watch.state, \watching,
			"precondition: an unhealthy series is building, not yet danger");
		watch.push(2610.783, 25, false);
		this.assertEquals(watch.state, \stale, "silence replaces the claim");
		watch.push(2610.783, 27.5);
		this.assertEquals(watch.state, \watching,
			"and the 60 s window restarts: a gap in evidence cannot be counted as evidence");
	}

	test_answeringAgainLeavesStaleAtOnce {
		var watch = CQAudioClockWatch(48000);
		watch.push(48003.479, 0, false);
		this.assertEquals(watch.state, \stale, "precondition");
		this.assertEquals(watch.push(47986.317, 2.5), \ok,
			"a server that answers is itself the recovery evidence — no settle delay");
	}

	// The facade half: the same case must reach the always-visible mark with its
	// own words (user decision 2026-07-31), and must not claim the clock is stuck.
	test_facadeCallsASilentServerByItsOwnName {
		var server = CQTestServer();
		var events = List.new;
		var listener = SimpleController(Cocoquantus);
		listener.put(\audioClock, { |obj, what, alert| events.add(alert) });
		Cocoquantus.prStartAudioClockWatch(server, Cocoquantus.sessionGeneration,
			48000, 0);
		server.actualSampleRate = 48003.47925608;
		Cocoquantus.prAudioClockTick(server, 0);
		this.assertEquals(Cocoquantus.audioClockAlert, \none,
			"precondition: a healthy answering server lights nothing");
		server.unresponsive = true;
		4.do { |i| Cocoquantus.prAudioClockTick(server, (i + 1) * 2.5) };
		this.assertEquals(Cocoquantus.audioClockState, \stale,
			"the guard stops asserting health the moment the answers stop");
		this.assertEquals(Cocoquantus.audioClockAlert, \serverDown,
			"and the mark says what actually happened");
		this.assert(Cocoquantus.audioClockDanger.not,
			"a silent server is not a stuck clock — two faults, two sentences");
		this.assertEquals(Cocoquantus.audioClockRate, nil,
			"the frozen reading is never reported as a measurement");
		this.assertEquals(events.size, 1,
			"announced once: a state is not a stream");
		this.assertEquals(events[0], \serverDown, "by name");
		server.unresponsive = false;
		server.actualSampleRate = 47986.317;
		Cocoquantus.prAudioClockTick(server, 20);
		this.assertEquals(Cocoquantus.audioClockAlert, \none,
			"and it is retracted as soon as the server answers again");
		this.assertEquals(events.last, \none, "with its own announcement");
		listener.remove;
		Cocoquantus.prStopAudioClockWatch;
	}

	// User review 2026-07-31: the guard printed its warning, the model said
	// \serverDown, and the toolbar mark never appeared. `CQ : Cocoquantus {}` is
	// an alias subclass, so a session booted through `CQ.boot` runs every class
	// method with CQ as the receiver — and `this.changed(...)` then published on
	// CQ while every listener subscribes to Cocoquantus. AUD-W's part 1 had
	// passed because the checklist drove the guard through
	// `Cocoquantus.prStartAudioClockWatch`, i.e. by the other name: **the test
	// agreed with the code only because it used the same spelling the code did.**
	// This one deliberately uses the alias, which is what a player uses.
	test_theAliasPublishesWhereListenersActuallyListen {
		var server = CQTestServer();
		var heard = List.new;
		var listener = SimpleController(Cocoquantus);
		listener.put(\audioClock, { |obj, what, alert| heard.add(alert) });
		CQ.prStartAudioClockWatch(server, CQ.sessionGeneration, 48000, 0);
		server.unresponsive = true;
		2.do { |i| CQ.prAudioClockTick(server, i * 2.5) };
		this.assertEquals(Cocoquantus.audioClockAlert, \serverDown,
			"precondition: the model reached the fault through the alias");
		this.assertEquals(heard.size, 1,
			"a guard driven through CQ still announces where CQGui listens");
		this.assertEquals(heard[0], \serverDown, "and by name");
		server.unresponsive = false;
		CQ.prAudioClockTick(server, 10);
		this.assertEquals(heard.last, \none, "its retraction arrives there too");
		listener.remove;
		CQ.prStopAudioClockWatch;
	}

	test_stoppingTheGuardRetractsAServerDownToo {
		var server = CQTestServer();
		var events = List.new;
		var listener = SimpleController(Cocoquantus);
		listener.put(\audioClock, { |obj, what, alert| events.add(alert) });
		Cocoquantus.prStartAudioClockWatch(server, Cocoquantus.sessionGeneration,
			48000, 0);
		server.unresponsive = true;
		2.do { |i| Cocoquantus.prAudioClockTick(server, i * 2.5) };
		this.assertEquals(Cocoquantus.audioClockAlert, \serverDown, "precondition");
		Cocoquantus.prStopAudioClockWatch;
		this.assertEquals(Cocoquantus.audioClockAlert, \none,
			"the entered-but-not-left defect covers the new mark as well");
		this.assertEquals(events.last, \none, "and the retraction is announced");
		listener.remove;
	}

	// A double that answers neither question is not evidence of a dead server:
	// absent telemetry is not the same as bad telemetry.
	test_aServerThatSaysNothingAboutItselfIsNotDeclaredDown {
		var quiet = Object.new;   // answers no lifecycle question at all
		var watch;
		Cocoquantus.prStartAudioClockWatch(CQTestServer(),
			Cocoquantus.sessionGeneration, 48000, 0);
		watch = Cocoquantus.prServerAnswering(quiet);
		this.assert(watch, "silence about the question is not an answer of 'dead'");
		Cocoquantus.prStopAudioClockWatch;
	}

	test_convergingTransientMovesNoInk {
		var server = CQTestServer();
		var events = List.new;
		var listener = SimpleController(Cocoquantus);
		var rates = [7419, 12000, 21000, 30000, 36000, 69184, 65000, 62000];
		listener.put(\audioClock, { |obj, what, alert| events.add(alert) });
		Cocoquantus.prStartAudioClockWatch(server, Cocoquantus.sessionGeneration,
			48000, 0);
		rates.do { |rate, i|
			server.actualSampleRate = rate;
			Cocoquantus.prAudioClockTick(server, i * 11.25);
		};
		this.assert(Cocoquantus.audioClockDanger.not,
			"a converging wake-up transient is never danger");
		this.assert(events.isEmpty,
			"and it announces nothing at all — the watching state stays invisible");
		listener.remove;
		Cocoquantus.prStopAudioClockWatch;
	}

	test_healthyClockAnnouncesNothing {
		var server = CQTestServer();
		var events = List.new;
		var listener = SimpleController(Cocoquantus);
		listener.put(\audioClock, { |obj, what, alert| events.add(alert) });
		Cocoquantus.prStartAudioClockWatch(server, Cocoquantus.sessionGeneration,
			48000, 0);
		server.actualSampleRate = 47986.317;
		40.do { |i| Cocoquantus.prAudioClockTick(server, i * 2.5) };
		this.assert(events.isEmpty, "a healthy clock is silent");
		this.assertEquals(Cocoquantus.audioClockTickCount > 0, true,
			"even though the guard really did read the clock");
		listener.remove;
		Cocoquantus.prStopAudioClockWatch;
	}

	test_resetForgetsTheStreak {
		var watch = CQAudioClockWatch(48000);
		this.prFeed(watch, 30.collect { 2610.783 });
		watch.reset;
		this.assertEquals(watch.state, \ok, "reset returns to ok");
		this.assertEquals(watch.streakCount, 0, "and drops the streak with it");
	}
}
