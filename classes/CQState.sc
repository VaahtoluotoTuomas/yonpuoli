// CQState — the single state funnel (ARCHITECTURE §1.1). Every write path —
// code, MIDI, GUI, snapshots, shake — calls set(), which clamps via CQSpec,
// forwards to the owning synth, and notifies dependants. No side channels.
// Works with graph = nil (pure value store) so the funnel is unit-testable.

CQState {
	var <graph;
	var values;    // IdentityDictionary: paramName -> current (clamped) value
	var routes;    // paramName -> [synthKey, argName]; unrouted params are stored only

	*new { |graph|
		^super.newCopyArgs(graph).prInit
	}

	prInit {
		values = IdentityDictionary.new;
		CQSpec.names.do { |name| values[name] = CQSpec.default(name) };
		routes = this.prBuildRoutes;
	}

	set { |name, value|
		var clamped;
		if(CQSpec.includes(name).not) {
			("CQState: unknown parameter '" ++ name ++ "' ignored").warn;
			^this
		};
		clamped = CQSpec.clamp(name, value);
		values[name] = clamped;
		this.prSend(name, clamped);
		this.changed(\param, name, clamped);
	}

	get { |name| ^values[name] }

	asEvent {
		var e = Event.new;
		values.keysValuesDo { |k, v| e[k] = v };
		^e
	}

	// push every routed param's current value to the server (used once at boot)
	pushAll {
		routes.keysDo { |name| this.prSend(name, values[name]) };
	}

	// ---- private ----

	// route[name] is either one [synthKey, argName] pair or a list of pairs
	// (patchDepth fans out to every matrix-consumer synth) — pairs missing their
	// synth (not yet spawned, e.g. an unloaded deck) are skipped silently
	prSend { |name, value|
		var route = routes[name], pairs;
		if(route.isNil) { ^this };
		if(graph.isNil or: { graph.isRunning.not }) { ^this };
		if(CQSpec.kind(name) == \enum) {                 // enums travel as indices
			value = CQSpec.at(name)[\values].indexOf(value);
		};
		pairs = if(route[0].isKindOf(Symbol)) { [route] } { route };
		pairs.do { |pair|
			var synth = graph.synths[pair[0]];
			synth !? { synth.set(pair[1], value) };
		};
	}

	// param -> [synthKey, synthArgName]; grows phase by phase
	prBuildRoutes {
		var r = IdentityDictionary.new;
		[\liveGain, \preASrc, \preBSrc, \preADrive, \preBDrive,
			\preABias, \preBBias].do { |n| r[n] = [\input, n] };
		if(graph.notNil and: { graph.config[\micChan].notNil }) {
			r[\micGain] = [\input, \micGain];   // control exists only when mic is configured
		};
		[\masterDry, \masterVol].do { |n| r[n] = [\master, n] };
		// matrix macro (ARCH §3.3): fans out to every matrix-consumer synth
		r[\patchDepth] = [[\coco1, \patchDepth], [\coco2, \patchDepth],
			[\quantussy, \patchDepth], [\deckA, \patchDepth], [\deckB, \patchDepth]];
		r[\monitorMute] = [\master, \mute];
		r[\coco1Pan] = [\master, \pan1];
		r[\coco2Pan] = [\master, \pan2];
		// cocos (phase 2)
		[1, 2].do { |n|
			var synthKey = ("coco" ++ n).asSymbol;
			[
				[\In, \inLevel], [\Fb, \fbLevel], [\Speed, \speed], [\Spaf, \spafKnob],
				[\DolbyIn, \dolbyIn], [\DolbyFb, \dolbyFb], [\Hold, \hold],
				[\Flip, \flip], [\Bits, \bits], [\Wrap, \wrap], [\Interp, \interp],
				[\Dust, \dust], [\Organ, \organLvl], [\Level, \level],
				[\SlotPre, \slotPre], [\SlotDeck, \slotDeck], [\SlotQ, \slotQ],
				[\SlotX, \slotX]
			].do { |pair|
				r[("coco" ++ n ++ pair[0]).asSymbol] = [synthKey, pair[1]];
			};
		};
		// quantussy (phase 3); spesal is a trigger action, not a param (Cocoquantus)
		[\qFreq, \qChaos, \qShLag].do { |n| r[n] = [\quantussy, n] };
		(1..5).do { |k|
			r[("c" ++ k ++ "Range").asSymbol] = [\quantussy, ("range" ++ k).asSymbol];
			r[("c" ++ k ++ "Off").asSymbol] = [\quantussy, ("off" ++ k).asSymbol];
			r[("c" ++ k ++ "Send").asSymbol] = [\quantussy, ("send" ++ k).asSymbol];
		};
		// decks: the synth exists only after a load — prSend skips silently until
		// then, CQDeck spawns the synth with current values
		[\deckA, \deckB].do { |d|
			r[(d ++ "Rate").asSymbol] = [d, \rate];
			r[(d ++ "Level").asSymbol] = [d, \level];
			r[(d ++ "Loop").asSymbol] = [d, \loop];
		};
		^r
	}
}
