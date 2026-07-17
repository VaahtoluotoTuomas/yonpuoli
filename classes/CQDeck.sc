// CQDeck — file player control (ARCHITECTURE §4.1). Loads one channel of a file
// (default 0; Cocoquantus.loadStereo pairs channel 0 → deck A, 1 → deck B),
// peak-normalizes the buffer to −6 dBFS (SPEC §5.1), and swaps the \cqDeck synth
// with a 50 ms crossfade. Folder navigation (SPEC §5.1): every load scans the
// loaded file's folder, next/prev/random walk it; before any load they fall
// back to the samples folder. Server objects are handed to CQGraph
// (registerSynth/registerBuffer) so teardown ownership stays in one place.
// Continuous params (rate/loop/level) go through the CQState funnel.

CQDeck {
	classvar <audioExtensions;
	var graph, state, <which, samplesDir;
	var <buffer, <path, synth;
	var <files, <fileIndex;

	*initClass {
		audioExtensions = #["wav", "aif", "aiff", "aifc", "flac"];
	}

	*new { |graph, state, which = \deckA, samplesDir|
		^super.newCopyArgs(graph, state, which, samplesDir)
	}

	load { |filePath, onDone, channel = 0|
		var p = this.prResolve(filePath);
		var ch;
		if(p.isNil) { ^this };
		if(graph.isNil or: { graph.isRunning.not }) {
			"CQDeck: not booted — CQ.boot first.".warn;
			^this
		};
		ch = this.prCheckChannel(p, channel);
		{
			var cond = Condition.new;
			var newBuf = Buffer.readChannel(graph.server, p, channels: [ch],
				action: { cond.unhang });
			cond.hang;
			if(newBuf.numFrames.isNil or: { newBuf.numFrames <= 0 }) {
				("CQDeck: could not read '" ++ p ++ "'").warn;
				newBuf.free;
			} {
				newBuf.normalize(0.5);         // peak −6 dBFS on load (SPEC §5.1)
				graph.server.sync;
				this.prSwap(newBuf, p);
				this.prScanFolder(p);
				("CQDeck %: loaded % (% s)".format(which, p,
					(newBuf.numFrames / newBuf.sampleRate).round(0.1))).postln;
				onDone.value(this);
			};
		}.fork;
	}

	// scan a folder and start on its first file (SPEC §5.1 "load a folder")
	loadFolder { |dirPath|
		var d = this.prResolveDir(dirPath);
		if(d.isNil) { ^this };
		this.prScanDir(d);
		if(files.isEmpty) {
			("CQDeck %: no audio files in '%'".format(which, d)).warn;
			^this
		};
		this.load(files.first);
	}

	next { this.prStep(1) }
	prev { this.prStep(-1) }

	random {
		this.prEnsureFiles;
		if(files.isEmpty) { ^this };
		this.load(files.choose);
	}

	rate_ { |v| state.set(this.prParam("Rate"), v) }
	loop_ { |v| state.set(this.prParam("Loop"), v) }
	level_ { |v| state.set(this.prParam("Level"), v) }
	rate { ^state.get(this.prParam("Rate")) }
	loop { ^state.get(this.prParam("Loop")) }
	level { ^state.get(this.prParam("Level")) }

	restart {
		synth !? { synth.set(\t_trig, 1) };
	}

	isLoaded { ^buffer.notNil }

	duration {
		^buffer !? { buffer.numFrames / buffer.sampleRate }
	}

	// language-side detach only; the server objects belong to CQGraph
	free {
		synth = nil;
		buffer = nil;
		path = nil;
		files = nil;
		fileIndex = nil;
	}

	// ---- private ----

	prParam { |suffix| ^(which ++ suffix).asSymbol }

	prResolve { |filePath|
		var p = filePath.asString.standardizePath;
		var inSamples;
		if(File.exists(p)) { ^p };
		inSamples = samplesDir +/+ filePath.asString;
		if(File.exists(inSamples)) { ^inSamples };
		("CQDeck: file not found: '" ++ filePath ++ "'").warn;
		^nil
	}

	prResolveDir { |dirPath|
		var p = dirPath.asString.standardizePath;
		var inSamples;
		if(File.exists(p)) { ^p };
		inSamples = samplesDir +/+ dirPath.asString;
		if(File.exists(inSamples)) { ^inSamples };
		("CQDeck: folder not found: '" ++ dirPath ++ "'").warn;
		^nil
	}

	// a requested channel beyond the file's channel count falls back to 0
	// (loadStereo of a mono file plays it on both decks)
	prCheckChannel { |p, channel|
		var sf, n;
		if(channel == 0) { ^0 };
		sf = SoundFile.openRead(p);
		if(sf.isNil) { ^0 };
		n = sf.numChannels;
		sf.close;
		if(channel >= n) {
			("CQDeck %: file has only % channel(s) — using channel 0"
				.format(which, n)).warn;
			^0
		};
		^channel
	}

	prStep { |delta|
		this.prEnsureFiles;
		if(files.isEmpty) { ^this };
		this.load(files.wrapAt(
			(fileIndex ?? { if(delta > 0) { -1 } { 0 } }) + delta));
	}

	prEnsureFiles {
		if(files.isNil) { this.prScanDir(samplesDir) };
		if(files.isEmpty) {
			("CQDeck %: no audio files found to navigate".format(which)).warn;
		};
	}

	prScanDir { |d|
		files = (d +/+ "*").pathMatch.select { |f|
			audioExtensions.includes(f.splitext[1].asString.toLower)
		}.sort;
		fileIndex = nil;
	}

	// after a load: remember siblings, so next/prev continue from this file
	prScanFolder { |loadedPath|
		var norm = loadedPath.tr($\\, $/);
		this.prScanDir(loadedPath.dirname);
		fileIndex = files.detectIndex { |f| f.tr($\\, $/) == norm };
	}

	// the only allowed synth churn (ARCH §1.3): new synth fades in over 25 ms,
	// the old one releases over 25 ms — a click-free buffer swap
	prSwap { |newBuf, newPath|
		var old = synth, oldBuf = buffer;
		synth = Synth(\cqDeck, [
			\out, graph.buses[which].index,
			\dryBus, graph.buses[\dry].index,
			\buf, newBuf.bufnum,
			\rate, state.get(this.prParam("Rate")),
			\loop, state.get(this.prParam("Loop")),
			\level, state.get(this.prParam("Level")),
			\patchDepth, state.get(\patchDepth),   // deck may load after a depth change
			\modSrcBus, graph.buses[\modSrc].index,
			\wRateBus, graph.buses[(which ++ "_rate").asSymbol].index
		], graph.groups[\sources], \addToHead);   // decks run before \cqInput
		graph.registerSynth(which, synth);
		graph.registerBuffer(which, newBuf);
		buffer = newBuf;
		path = newPath;
		old !? { old.set(\gate, 0) };
		oldBuf !? { SystemClock.sched(0.3, { oldBuf.free; nil }) };
	}
}
