// CQDeck — file player control (ARCHITECTURE §4.1). Loads a file (mono, or the
// left channel of a stereo file — linked-pair mode arrives in phase 5), peak-
// normalizes the buffer to −6 dBFS (SPEC §5.1), and swaps the \cqDeck synth with
// a 50 ms crossfade. Server objects are handed to CQGraph (registerSynth/
// registerBuffer) so teardown ownership stays in one place. Continuous params
// (rate/loop/level) go through the CQState funnel like every other write path.

CQDeck {
	var graph, state, <which, samplesDir;
	var <buffer, <path, synth;

	*new { |graph, state, which = \deckA, samplesDir|
		^super.newCopyArgs(graph, state, which, samplesDir)
	}

	load { |filePath, onDone|
		var p = this.prResolve(filePath);
		if(p.isNil) { ^this };
		if(graph.isNil or: { graph.isRunning.not }) {
			"CQDeck: not booted — CQ.boot first.".warn;
			^this
		};
		{
			var cond = Condition.new;
			var newBuf = Buffer.readChannel(graph.server, p, channels: [0],
				action: { cond.unhang });
			cond.hang;
			if(newBuf.numFrames.isNil or: { newBuf.numFrames <= 0 }) {
				("CQDeck: could not read '" ++ p ++ "'").warn;
				newBuf.free;
			} {
				newBuf.normalize(0.5);         // peak −6 dBFS on load (SPEC §5.1)
				graph.server.sync;
				this.prSwap(newBuf, p);
				("CQDeck %: loaded % (% s)".format(which, p,
					(newBuf.numFrames / newBuf.sampleRate).round(0.1))).postln;
				onDone.value(this);
			};
		}.fork;
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
