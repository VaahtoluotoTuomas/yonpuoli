// CQRecorder — disk recording (ARCHITECTURE §5, SPEC §5.7). Master 2 ch
// 24-bit WAV, timestamped cq_YYMMDD_HHMMSS.wav in the recordings folder,
// never overwrites (safety #7: collisions get a _2/_3... suffix). The
// \cqRecord DiskOut synth sits at the tail of gMaster and taps the hardware
// output buses AFTER \cqMaster has written them — the file is exactly the
// post-safety signal the speakers got. Recorder synth/buffer churn is the
// allowed exception (ARCH §1.3); ownership is handed to CQGraph while live
// and handed back (registered nil) on stop. Stem recording arrives in phase 7.

CQRecorder {
	classvar <>postWarnings = true;
	var graph, config, <dir;
	var buffer, synth, <path, startTime;

	*new { |graph, config, dir| ^super.newCopyArgs(graph, config, dir) }

	isRecording { ^synth.notNil }

	// seconds since start — the GUI's elapsed-time display reads this
	elapsed {
		^if(startTime.isNil) { 0 } { Main.elapsedTime - startTime }
	}

	start {
		var outChans, useStems, def, numCh, stemArgs;
		if(this.isRecording) {
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
		{
			if(File.exists(dir).not) { File.mkdir(dir) };
			buffer = Buffer.alloc(graph.server, 65536, numCh);
			graph.server.sync;
			buffer.write(path, "wav", "int24", 0, 0, true);   // leaveOpen for DiskOut
			graph.server.sync;
			synth = Synth(def, [
				\buf, buffer.bufnum,
				\outL, outChans[0],
				\outR, outChans[1] ? outChans[0]
			] ++ stemArgs, graph.groups[\master], \addToTail);
			graph.registerSynth(\recorder, synth);
			graph.registerBuffer(\recorder, buffer);
			startTime = Main.elapsedTime;
			("CQRecorder: recording % -> %".format(
				if(useStems) { "4-ch stems" } { "master" }, path)).postln;
		}.fork;
	}

	stop {
		var s = synth, b = buffer, secs = this.elapsed;
		if(this.isRecording.not) { ^this };
		synth = nil;
		buffer = nil;
		startTime = nil;
		s.free;
		b.close({ |buf| buf.freeMsg });
		graph !? {                            // hand ownership back (nil removes)
			graph.registerSynth(\recorder, nil);
			graph.registerBuffer(\recorder, nil);
		};
		("CQRecorder: stopped (% s) -> %".format(secs.round(0.1), path)).postln;
	}

	free { this.stop }

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
