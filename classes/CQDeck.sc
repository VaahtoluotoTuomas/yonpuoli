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
	classvar <>postWarnings = true, <>loadTimeout = 15;
	var graph, state, <which, samplesDir;
	var <buffer, <path, synth;
	var <files, <fileIndex;
	var <loadState = \idle, <error, requestGeneration = 0, activeAttempt;
	var alive = true;

	*initClass {
		// symbols, not strings: Collection:includes compares identity (===),
		// and two equal String instances are never identical
		audioExtensions = #[\wav, \aif, \aiff, \aifc, \flac];
	}

	*new { |graph, state, which = \deckA, samplesDir|
		^super.newCopyArgs(graph, state, which, samplesDir)
	}

	load { |filePath, onDone, channel = 0|
		this.prepare(filePath, channel, { |attempt|
			this.commitPrepared(attempt, -1, onDone);
		});
		^this
	}

	// Start an owned, cancellable buffer load without publishing it. CQDeckPair
	// uses this split phase so L/R can be committed in one server bundle.
	prepare { |filePath, channel = 0, onReady, onFailure|
		var p = this.prResolve(filePath), ch, attempt;
		if(p.isNil) {
			onFailure.value(Error("CQDeck: file not found"));
			^nil
		};
		if(alive.not or: { graph.isNil or: { graph.isRunning.not } }) {
			this.class.prWarn("CQDeck: not booted — CQ.boot first.");
			onFailure.value(Error("CQDeck: deck is not active"));
			^nil
		};
		ch = this.prCheckChannel(p, channel);
		this.prCancelActive;
		requestGeneration = requestGeneration + 1;
		attempt = (
			generation: requestGeneration,
			path: p,
			channel: ch,
			buffer: nil,
			cancelled: false,
			onFailure: onFailure
		);
		activeAttempt = attempt;
		error = nil;
		this.prSetLoadState(\loading);
		{
			try {
				attempt[\buffer] = this.prReadBuffer(p, ch);
				if(this.prCanCommit(attempt)) {
					if(attempt[\buffer].numFrames.isNil or: {
						attempt[\buffer].numFrames <= 0
					}) {
						Error("CQDeck: could not read '" ++ p ++ "'").throw;
					};
					attempt[\buffer].normalize(0.5); // −6 dBFS (SPEC §5.1)
					graph.server.sync;
					if(this.prCanCommit(attempt)) {
						onReady.value(attempt);
					} {
						this.prDisposeAttempt(attempt);
					};
				} {
					this.prDisposeAttempt(attempt);
				};
			} { |failure|
				this.prAttemptFailed(attempt, failure);
			};
		}.fork;
		^attempt
	}

	canCommit { |attempt| ^this.prCanCommit(attempt) }

	commitPrepared { |attempt, dryChannel = -1, onDone|
		if(this.prCanCommit(attempt).not) {
			this.cancelPrepared(attempt);
			^false
		};
		try {
			this.prSwap(attempt[\buffer], attempt[\path], dryChannel);
			attempt[\buffer] = nil; // ownership moved to CQGraph
			activeAttempt = nil;
			this.prScanFolder(path);
			this.prSetLoadState(\idle);
			("CQDeck %: loaded % (% s)".format(which, path,
				(buffer.numFrames / buffer.sampleRate).round(0.1))).postln;
			onDone.value(this);
		} { |failure|
			this.prAttemptFailed(attempt, failure);
			^false
		};
		^true
	}

	cancelPrepared { |attempt|
		if(attempt.isNil) { ^this };
		attempt[\cancelled] = true;
		this.prDisposeAttempt(attempt);
		if(activeAttempt === attempt) {
			activeAttempt = nil;
			if(alive) { this.prSetLoadState(\idle) };
		};
		^this
	}

	// scan a folder and start on its first file (SPEC §5.1 "load a folder")
	loadFolder { |dirPath|
		var d = this.prResolveDir(dirPath);
		if(d.isNil) { ^this };
		this.prScanDir(d);
		if(files.isEmpty) {
			this.class.prWarn("CQDeck %: no audio files in '%'".format(which, d));
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
		alive = false;
		requestGeneration = requestGeneration + 1;
		this.prCancelActive;
		synth = nil;
		buffer = nil;
		path = nil;
		files = nil;
		fileIndex = nil;
		error = nil;
		loadState = \idle;
	}

	// ---- private ----

	prReadBuffer { |filePath, channel|
		var cond = Condition.new, completed = false, timedOut = false;
		var newBuffer = Buffer.readChannel(graph.server, filePath, channels: [channel],
			action: {
				completed = true;
				cond.unhang;
			});
		SystemClock.sched(this.class.loadTimeout, {
			if(completed.not) {
				timedOut = true;
				cond.unhang;
			};
			nil
		});
		cond.hang;
		if(timedOut) {
			newBuffer.free;
			Error("CQDeck: load timed out for '" ++ filePath ++ "'").throw;
		};
		^newBuffer
	}

	prCanCommit { |attempt|
		^(alive == true)
			and: { activeAttempt === attempt }
			and: { attempt[\cancelled] != true }
			and: { attempt[\generation] == requestGeneration }
			and: { graph.notNil and: { graph.isRunning } }
	}

	prCancelActive {
		var previous = activeAttempt;
		previous !? {
			previous[\cancelled] = true;
			this.prDisposeAttempt(previous);
		};
		activeAttempt = nil;
	}

	prDisposeAttempt { |attempt|
		attempt[\buffer] !? { |ownedBuffer|
			try { ownedBuffer.free } { |cleanupError|
				("CQDeck: buffer cleanup warning — "
					++ cleanupError.errorString).warn;
			};
			attempt[\buffer] = nil;
		};
	}

	prAttemptFailed { |attempt, failure|
		this.prDisposeAttempt(attempt);
		if(activeAttempt === attempt) {
			activeAttempt = nil;
			error = failure.errorString;
			this.prSetLoadState(\error);
			attempt[\onFailure].value(failure);
			this.class.prWarn("CQDeck %: %".format(which, error));
		};
	}

	prSetLoadState { |next|
		loadState = next;
		this.changed(\state, loadState, error);
	}

	prParam { |suffix| ^(which ++ suffix).asSymbol }

	prResolve { |filePath|
		var p = filePath.asString.standardizePath;
		var inSamples;
		if(File.exists(p)) { ^p };
		inSamples = samplesDir +/+ filePath.asString;
		if(File.exists(inSamples)) { ^inSamples };
		this.class.prWarn("CQDeck: file not found: '" ++ filePath ++ "'");
		^nil
	}

	prResolveDir { |dirPath|
		var p = dirPath.asString.standardizePath;
		var inSamples, sibling;
		if(File.exists(p)) { ^p };
		inSamples = samplesDir +/+ dirPath.asString;
		if(File.exists(inSamples)) { ^inSamples };
		// sibling of the samples folder — covers loadFolder("samples") itself
		// (any repo-root folder in the default config); the live IDE's cwd is
		// rarely the repo, so the bare-relative check above misses these
		sibling = samplesDir.dirname +/+ dirPath.asString;
		if(File.exists(sibling)) { ^sibling };
		this.class.prWarn("CQDeck: folder not found: '" ++ dirPath ++ "'");
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
			this.class.prWarn("CQDeck %: file has only % channel(s) — using channel 0"
				.format(which, n));
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
			this.class.prWarn("CQDeck %: no audio files found to navigate".format(which));
		};
	}

	prScanDir { |d|
		files = (d +/+ "*").pathMatch.select { |f|
			audioExtensions.includes(f.splitext[1].asString.toLower.asSymbol)
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
	prSwap { |newBuf, newPath, dryChannel = -1|
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
			\wRateBus, graph.buses[(which ++ "_rate").asSymbol].index,
			\dryChannel, dryChannel
		], graph.groups[\sources], \addToHead);   // decks run before \cqInput
		graph.registerSynth(which, synth);
		graph.registerBuffer(which, newBuf);
		buffer = newBuf;
		path = newPath;
		this.changed(\file, newPath);   // GUI file label (any load path: drop/button/nav/code)
		old !? { old.set(\gate, 0) };
		oldBuf !? { SystemClock.sched(0.3, { oldBuf.free; nil }) };
	}

	*prWarn { |msg| if(postWarnings) { msg.warn } }
}
