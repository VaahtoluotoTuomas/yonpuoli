// CQRecorder — disk recording (ARCHITECTURE §5, SPEC §5.7). Master 2 ch
// 24-bit WAV, timestamped cq_YYMMDD_HHMMSS.wav in the recordings folder,
// never overwrites (safety #7: collisions get a _2/_3... suffix). The
// \cqRecord DiskOut synth sits at the tail of gMaster and taps the hardware
// output buses AFTER \cqMaster has written them — the file is exactly the
// post-safety signal the speakers got. Recorder synth/buffer churn is the
// allowed exception (ARCH §1.3); ownership is handed to CQGraph while live
// and handed back (registered nil) on stop. Each async start owns one local
// attempt record, so cancellation cannot be overtaken by a late callback.

CQRecorder {
	classvar <>postWarnings = true;
	var graph, config, <dir;
	var <state, <error, <path, startTime;
	var attemptSerial = 0, activeAttempt, doneCallbacks;

	*new { |graph, config, dir| ^super.newCopyArgs(graph, config, dir).prInit }

	prInit {
		state = \idle;
		doneCallbacks = List.new;
	}

	isRecording { ^state == \recording }

	// seconds since start — the GUI's elapsed-time display reads this
	elapsed {
		^if(startTime.isNil) { 0 } { Main.elapsedTime - startTime }
	}

	start {
		var outChans, useStems, def, numCh, stemArgs, attempt;
		if(state == \error) {
			this.prClearError;
			^this
		};
		if(state != \idle) {
			this.class.prWarn("already recording -> " ++ path);
			^this
		};
		if(graph.isNil or: { graph.isRunning.not }) {
			this.class.prWarn("not booted — CQ.boot first");
			^this
		};
		outChans = config[\outChans];
		// SPEC §5.7 config choice: recStems -> 4-ch file (master L/R + the two
		// post-safety Coco stems \cqMaster publishes to private buses); else 2-ch
		useStems = config[\recStems] == true;
		def = if(useStems) { \cqRecordStems } { \cqRecord };
		numCh = if(useStems) { 4 } { 2 };
		stemArgs = if(useStems) {
			[\stem1, graph.buses[\stem1].index, \stem2, graph.buses[\stem2].index]
		} { [] };
		path = this.class.nextFreePath(dir);
		attemptSerial = attemptSerial + 1;
		attempt = (
			id: attemptSerial,
			path: path,
			cancelled: false,
			fileOpen: false
		);
		activeAttempt = attempt;
		error = nil;
		this.prSetState(\starting);
		{
			var localBuffer, localSynth;
			try {
				if(File.exists(dir).not) { File.mkdir(dir) };
				localBuffer = Buffer.alloc(graph.server, 65536, numCh);
				attempt[\buffer] = localBuffer;
				graph.server.sync;
				if(this.prCanPublish(attempt)) {
					localBuffer.write(attempt[\path], "wav", "int24", 0, 0, true);
					attempt[\fileOpen] = true;
					graph.server.sync;
					if(this.prCanPublish(attempt)) {
						localSynth = Synth(def, [
							\buf, localBuffer.bufnum,
							\outL, outChans[0],
							\outR, outChans[1] ? outChans[0]
						] ++ stemArgs, graph.groups[\master], \addToTail);
						attempt[\synth] = localSynth;
						graph.registerSynth(\recorder, localSynth);
						graph.registerBuffer(\recorder, localBuffer);
						startTime = Main.elapsedTime;
						this.prSetState(\recording);
						("CQRecorder: recording % -> %".format(
							if(useStems) { "4-ch stems" } { "master" }, path)).postln;
					} {
						this.prReleaseAttempt(attempt);
					};
				} {
					this.prReleaseAttempt(attempt);
				};
			} { |err|
				this.prAttemptFailed(attempt, err);
			};
		}.fork;
	}

	stop { |onDone|
		var attempt = activeAttempt, secs = this.elapsed;
		this.prAddDone(onDone);
		switch(state,
			\idle, { this.prCompleteDone },
			\error, {
				this.prClearError;
				this.prCompleteDone;
			},
			\starting, {
				attempt[\cancelled] = true;
				this.prSetState(\stopping);
			},
			\recording, {
				attempt[\cancelled] = true;
				this.prSetState(\stopping);
				{
					this.prReleaseAttempt(attempt);
					("CQRecorder: stopped (% s) -> %"
						.format(secs.round(0.1), path)).postln;
				}.fork;
			},
			\stopping, { nil }
		);
	}

	free { |onDone| this.stop(onDone) }

	// ---- attempt lifecycle ----

	prCanPublish { |attempt|
		^(activeAttempt === attempt)
			and: { attempt[\cancelled] != true }
			and: { state == \starting }
			and: { graph.notNil and: { graph.isRunning } }
	}

	prReleaseAttempt { |attempt|
		this.prDisposeAttempt(attempt, true);
		if(activeAttempt === attempt) {
			activeAttempt = nil;
			startTime = nil;
			this.prSetState(\idle);
			this.prCompleteDone;
		};
	}

	prDisposeAttempt { |attempt, waitForServer = false|
		var ownedSynth = attempt[\synth], ownedBuffer = attempt[\buffer];
		try {
			ownedSynth !? { ownedSynth.free };
			if(waitForServer and: { ownedSynth.notNil }) { graph.server.sync };
			ownedBuffer !? {
				if(attempt[\fileOpen] == true) {
					ownedBuffer.close({ |buf| buf.freeMsg });
				} {
					ownedBuffer.free;
				};
			};
			if(waitForServer and: { ownedBuffer.notNil }) { graph.server.sync };
		} { |cleanupError|
			("CQRecorder: cleanup warning — " ++ cleanupError.errorString).warn;
		};
		attempt[\synth] = nil;
		attempt[\buffer] = nil;
		if(activeAttempt === attempt) {
			graph !? {
				graph.registerSynth(\recorder, nil);
				graph.registerBuffer(\recorder, nil);
			};
		};
	}

	prAttemptFailed { |attempt, failure|
		this.prDisposeAttempt(attempt, false);
		if(activeAttempt === attempt) {
			activeAttempt = nil;
			startTime = nil;
			error = failure.errorString;
			this.prSetState(\error);
			this.prCompleteDone;
			("CQRecorder: " ++ error).warn;
		};
	}

	prClearError {
		error = nil;
		this.prSetState(\idle);
	}

	prSetState { |next|
		state = next;
		this.changed(\state, state, error);
	}

	prAddDone { |func| func !? { doneCallbacks.add(func) } }

	prCompleteDone {
		var callbacks = doneCallbacks.copy;
		doneCallbacks.clear;
		callbacks.do { |func| func.value(this) };
	}

	// timestamped and collision-proof (safety #7: recordings never overwrite);
	// date is injectable so the naming is testable without a clock or server
	*nextFreePath { |dir, date|
		var d = date ?? { Date.getDate };
		var stamp = "%%%_%%%".format(
			(d.year % 100).asString.padLeft(2, "0"),
			d.month.asString.padLeft(2, "0"),
			d.day.asString.padLeft(2, "0"),
			d.hour.asString.padLeft(2, "0"),
			d.minute.asString.padLeft(2, "0"),
			d.second.asString.padLeft(2, "0"));
		var base = dir +/+ ("cq_" ++ stamp);
		var candidate = base ++ ".wav";
		var n = 2;
		while { File.exists(candidate) } {
			candidate = base ++ "_" ++ n ++ ".wav";
			n = n + 1;
		};
		^candidate
	}

	*prWarn { |msg| if(postWarnings) { ("CQRecorder: " ++ msg).warn } }
}
