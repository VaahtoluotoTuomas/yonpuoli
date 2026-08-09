// CQMatrix — the routing matrix (banana bay), SPEC §5.5 / ARCHITECTURE §3.2–3.3.
// Same funnel shape as CQState: patch() clamps the weight, stores it, mirrors it
// to the destination's 16-channel wDest control bus via setn, and notifies
// dependants. Works with graph = nil (pure value store) so it is unit-testable.
// Weight range -1.0–1.0 is owned here (SPEC §5.5), not in CQSpec: matrix cells
// are name-pair-indexed, not a CQSpec scalar parameter.

CQMatrix {
	classvar <sourceNames, <destNames;
	classvar <>postWarnings = true;

	var <graph, <lastShakePlan;
	var weights;   // [destIndex][sourceIndex] -> Float, -1..1, 0 = unpatched

	*initClass {
		// ARCHITECTURE §3.2 — canonical aModSrc index order
		sourceNames = #[
			\tri1, \tri2, \tri3, \tri4, \tri5, \cas1, \cas2, \cas3, \cas4, \cas5,
			\envA, \envB, \coco1, \coco2, \org1, \org2
		];
		// ARCHITECTURE §3.3 — canonical matrix destination order
		destNames = #[
			\coco1_spaf, \coco1_inVca, \coco1_fbVca, \coco1_flip, \coco1_skip,
			\coco1_organ, \coco1_slotQ, \coco1_slotX, \coco1_dust, \coco1_record,
			\coco2_spaf, \coco2_inVca, \coco2_fbVca, \coco2_flip, \coco2_skip,
			\coco2_organ, \coco2_slotQ, \coco2_slotX, \coco2_dust, \coco2_record,
			\castle1_fm, \castle2_fm, \castle3_fm, \castle4_fm, \castle5_fm,
			\castle1_spesal, \castle2_spesal, \castle3_spesal, \castle4_spesal,
			\castle5_spesal, \deckA_rate, \deckB_rate
		];
	}

	*new { |graph| ^super.newCopyArgs(graph).prInit }

	prInit {
		weights = destNames.collect { sourceNames.collect { 0.0 } };
		lastShakePlan = nil;
	}

	*srcIndex { |name| ^sourceNames.indexOf(name) }
	*destIndex { |name| ^destNames.indexOf(name) }

	// the funnel: every write path (code, GUI, snapshots, shake, presets) clamps
	// here — malformed values warn and become 0 (= unpatched), never applied raw
	// (SPEC §8.3)
	*clampWeight { |w|
		var v = w;
		if(v.isKindOf(Boolean)) { v = v.binaryValue.asFloat };
		if(v.isNumber.not or: { v.asFloat.isNaN } or: { v.abs == inf }) {
			this.prWarn("bad weight % clamped to 0".format(w));
			^0.0
		};
		^v.asFloat.clip(-1.0, 1.0)
	}

	patch { |src, dest, w = 1.0, origin = \code, committed = true, operation = \patch|
		var si = sourceNames.indexOf(src), di = destNames.indexOf(dest);
		if(si.isNil or: { di.isNil }) {
			this.class.prWarn("unknown patch % -> %".format(src, dest));
			^this
		};
		weights[di][si] = this.class.clampWeight(w);
		this.prPush(di);
		this.changed(\patch, src, dest, weights[di][si],
			origin, committed, operation);
	}

	unpatch { |src, dest, origin = \code, committed = true, operation = \rip|
		this.patch(src, dest, 0.0, origin, committed, operation)
	}

	clearPatch { |origin = \code, committed = true, operation = \clear|
		this.prInit;
		this.prPushAll;
		this.changed(\clear, origin, committed, operation);
	}

	getPatch { |src, dest|
		var si = sourceNames.indexOf(src), di = destNames.indexOf(dest);
		^if(si.isNil or: { di.isNil }) { 0.0 } { weights[di][si] }
	}

	// Dense [destination][source] transaction API for Archipelago surfaces.
	// Reads are deep copies; writes validate the complete shape before replacing
	// state, clamp every cell, push each destination once, and notify once.
	fullMatrix {
		^weights.collect { |row| row.copy }
	}

	writeMatrix { |dense, origin = \code, committed = true, operation = \writeMatrix|
		var valid, next;
		valid = dense.isKindOf(SequenceableCollection)
			and: { dense.size == destNames.size }
			and: { dense.every { |row|
				row.isKindOf(SequenceableCollection)
					and: { row.size == sourceNames.size }
			} };
		if(valid.not) {
			this.class.prWarn("dense matrix must be % destinations x % sources"
				.format(destNames.size, sourceNames.size));
			^this
		};
		next = dense.collect { |row|
			row.collect { |value| this.class.clampWeight(value) }
		};
		weights = next;
		this.prPushAll;
		this.changed(\matrix, origin, committed, operation);
		^this
	}

	// every non-zero cell as [src, dest, weight] — the snapshot matrix format
	// (ARCHITECTURE §7)
	asArray {
		var out = List.new;
		destNames.do { |dest, di|
			sourceNames.do { |src, si|
				if(weights[di][si] != 0.0) { out.add([src, dest, weights[di][si]]) };
			};
		};
		^out.asArray
	}

	load { |triples, origin = \code, committed = true, operation = \load|
		this.prInit;
		triples.do { |t|
			var si = sourceNames.indexOf(t[0]), di = destNames.indexOf(t[1]);
			if(si.notNil and: { di.notNil }) {
				weights[di][si] = this.class.clampWeight(t[2]);
			} {
				this.class.prWarn("unknown patch % -> % in loaded data".format(t[0], t[1]));
			};
		};
		this.prPushAll;
		this.changed(\clear, origin, committed, operation);
		// one bulk notification instead of one per cell
	}

	// applies the preset's MATRIX part only; the params part goes through the
	// CQState funnel and is applied by Cocoquantus.patchPreset (CQMatrix must
	// not know about CQState — ARCH §5 separation)
	preset { |name, origin = \code, committed = true, operation = \preset|
		var data = this.class.presets[name];
		if(data.isNil) {
			this.class.prWarn("unknown preset '" ++ name ++ "'");
			^this
		};
		this.load(data[\matrix], origin, committed, operation);
	}

	// SPEC §5.5 shake: first plant one audible 3–5-cable ecology, then sow the
	// moon-density random field around it.  `amount` remains the decoration
	// ceiling for continuous destinations. Gate/trig cables have their own
	// threshold-aware 0.58..0.82 country, and the guaranteed ecology has small,
	// destination-specific continuous ranges; otherwise a default 0.3 throw can
	// never open a +0.5 Schmitt and often draws many cables that do nothing.
	//
	// The returned value remains the five spesal states for API compatibility.
	// `lastShakePlan` exposes the selected family, its exact skeleton and the
	// small CQState preparation Event that Cocoquantus applies through the normal
	// state funnel. This model still writes no parameter itself. A seed makes the
	// complete throw reproducible (and the setup too from the same context) and
	// leaves the caller's rand stream untouched.
	//
	// `density` is how MUCH is sown, and it is what the night's sowing moon
	// wears as its phase (GUI_SPEC §16.6): strength already has a macro of its
	// own in patchDepth, while density is reachable from nowhere else.
	shake { |amount = 0.3, seed, origin = \code, committed = true,
		operation = \shake, density, context|
		var amt = this.class.clampWeight(amount).abs;
		var sow = this.class.clampDensity(density);
		var saved = thisThread.randData;
		var spesal, plan, family;
		// an empty moon **clears the bay**: nothing is sown, so nothing is left
		// standing — the phase is the field, and an empty field is empty. The
		// spesal states are deliberately left alone: a clear is a clear, not a
		// scramble in disguise (decided with the user 2026-07-22, replacing the
		// first version's no-op, which read as a broken control).
		if(sow <= 0) {
			this.clearPatch(origin, committed, operation);
			^[]
		};
		seed !? { thisThread.randSeed = seed.asInteger };
		this.prInit;
		if(amt > 0) {
			// Select the ecology and its spesal posture before density consumes any
			// random draws. The same seed therefore keeps the same musical core from
			// crescent to full moon; density only grows decoration around it.
			family = if(seed.notNil) {
				this.class.shakeFamilyForSeed(seed)
			} { nil };
			plan = this.prShakePlan(context ? (), family);
			spesal = 5.collect { 8.rand };
			this.prSowDecoration(amt, sow);
			// Guaranteed ecology last: a random draw can never weaken or replace
			// the deliberate route that makes this throw worth hearing.
			plan[\skeleton].do { |triple|
				this.prPlace(triple[0], triple[1], triple[2])
			};
		} {
			// Preserve the old malformed/zero-amount safety behaviour: a field of
			// zero-weight "patches" is simply empty, while spesal still scrambles.
			plan = (family: \silent, params: (), skeleton: []);
			spesal = 5.collect { 8.rand };
		};
		lastShakePlan = plan;
		seed !? { thisThread.randData = saved };
		this.prPushAll;
		this.changed(\clear, origin, committed, operation);
		// bulk notification: dependants re-read everything
		^spesal
	}

	// The only state controls an ecological throw may prepare. Keeping this list
	// explicit lets the GUI capture one matching undo bundle without rolling back
	// unrelated mixer, mic, master or safety state.
	*shakeParamNames {
		var names = List[\patchDepth, \qFreq, \qChaos];
		[1, 2].do { |n|
			#[\Level, \Organ, \SlotQ, \Hold, \In, \Fb, \Dust, \SlotX, \Speed].do { |suffix|
				names.add(("coco" ++ n ++ suffix).asSymbol)
			};
		};
		(1..5).do { |k|
			names.add(("c" ++ k ++ "Send").asSymbol);
			names.add(("c" ++ k ++ "Range").asSymbol);
		};
		^names.asArray
	}

	prSowDecoration { |amt, sow|
		weights.do { |row, di|
			var dest = destNames[di];
			// Gate populations grow more slowly than continuous colour. At full
			// moon this is about nine chance gates, not thirty-five simultaneous
			// Schmitts; the family skeleton supplies the deliberate pulse.
			var chance = if(this.class.isGateDest(dest)) { sow * 0.22 } { sow };
			// Every accepted canonical destination participates. SlotX and Dust were
			// initially skipped as a four-row audition quarantine; the user opened
			// them after their sound, gesture and UI gates on 2026-08-08.
			if(this.class.isShakeTarget(dest)) {
				row.size.do { |si|
					if(chance.coin) {
						weights[di][si] = this.class.decorationWeight(dest, amt)
					};
				};
			};
		};
	}

	*isShakeTarget { |dest|
		^destNames.includes(dest)
	}

	*isGateDest { |dest|
		var text = dest.asString;
		^text.endsWith("_flip") or: { text.endsWith("_skip") }
			or: { text.endsWith("_record") }
			or: { text.endsWith("_spesal") }
	}

	*isNewVcaDest { |dest|
		var text = dest.asString;
		^text.endsWith("_organ") or: { text.endsWith("_slotQ") }
			or: { text.endsWith("_slotX") }
	}

	// The four documented PLAY-8 fixture seeds retain their identities, while
	// an unseeded throw still chooses freely in prShakePlan. Explicit seeds cycle
	// the four families predictably; this is also independent of moon density.
	*shakeFamilyForSeed { |seed|
		var n = if(seed.isNumber) { seed.asInteger.abs } { 0 };
		^#[crossfeed, memoryPulse, dustGrowth, organDrone].wrapAt(n % 4)
	}

	// Random continuous cables no longer cluster around zero: the lower bound is
	// a fraction of the requested ceiling, so every drawn cable has a useful say.
	// Gates instead live above their DSP threshold; sign still chooses phase.
	*decorationWeight { |dest, amount = 0.3|
		var amt = this.clampWeight(amount).abs, lo, hi, magnitude;
		if(amt <= 0) { ^0.0 };
		if(this.isGateDest(dest)) {
			lo = 0.58;
			hi = (0.5 + amt).clip(0.62, 0.82);
		} {
			hi = if(this.isNewVcaDest(dest)) { amt.min(0.45) } { amt };
			lo = if(this.isNewVcaDest(dest)) {
				min(hi, 0.16)
			} {
				min(hi, max(0.05, hi * 0.45))
			};
		};
		magnitude = rrand(lo, hi);
		^magnitude * #[-1, 1].choose
	}

	prPlace { |src, dest, weight|
		var si = this.class.srcIndex(src), di = this.class.destIndex(dest);
		if(si.notNil and: { di.notNil }) {
			weights[di][si] = this.class.clampWeight(weight)
		};
	}

	// Four contrasting ecologies. Every one can speak with empty/held Cocos:
	// organ is its own source; q-memory prepares qMix and punches RAM; dust grows
	// converter material; crossfeed makes the two memories rewrite one another.
	// All families touch both Cocos and plant exactly 3–5 deliberate cables.
	prShakePlan { |context, requestedFamily|
		var family = requestedFamily
			? #[organDrone, memoryPulse, dustGrowth, crossfeed].choose;
		var params = Event.new, skeleton = List.new;
		var speed1, active, sources;
		context = context ? ();
		params[\patchDepth] = this.prAtLeast(context, \patchDepth, 0.9);
		[1, 2].do { |n|
			var level = ("coco" ++ n ++ "Level").asSymbol;
			params[level] = this.prAtLeast(context, level, 0.90);
		};
		// Every ecology chooses the three castles that animate its own skeleton:
		// one balcium source and two audio-rate sources. This keeps a first shake
		// immediate even from empty RAM; an arbitrary infra source could otherwise
		// postpone the first record punch for much of its cycle.
		active = this.prPrepareQ(params);
		sources = (active.collect { |i| ("tri" ++ (i + 1)).asSymbol }
			++ active.collect { |i| ("cas" ++ (i + 1)).asSymbol }).scramble;
		// A family is a complete ecology rather than a layer of stale setup left
		// by the previous moon throw. RAM content survives, but every safe control
		// that distinguishes the four families gets a quiet shared ground first.
		[1, 2].do { |n|
			params[("coco" ++ n ++ "Organ").asSymbol] = 0.0;
			params[("coco" ++ n ++ "SlotQ").asSymbol] = 0.0;
			params[("coco" ++ n ++ "Hold").asSymbol] = 1;
			params[("coco" ++ n ++ "In").asSymbol] = 0.8;
			params[("coco" ++ n ++ "Fb").asSymbol] = 0.35;
			params[("coco" ++ n ++ "Dust").asSymbol] = 0.02;
			params[("coco" ++ n ++ "SlotX").asSymbol] = 0.0;
			params[("coco" ++ n ++ "Speed").asSymbol] = 24000;
		};
		switch(family,
			\organDrone, {
				params[\coco1Organ] = 0.06;
				params[\coco2Organ] = 0.06;
				params[\coco1Dust] = 0.0;
				params[\coco2Dust] = 0.0;
				speed1 = #[12000, 16000, 22000, 30000].choose;
				params[\coco1Speed] = speed1;
				params[\coco2Speed] = (speed1 * #[0.5, 0.6666667, 0.75, 1.5].choose)
					.clip(CQSpec.at(\coco2Speed)[\spec].minval,
						CQSpec.at(\coco2Speed)[\spec].maxval);
				skeleton.add([sources[0], \coco1_organ, rrand(0.26, 0.38)]);
				skeleton.add([sources[1], \coco2_organ, rrand(0.26, 0.38)]);
				skeleton.add([sources[2], #[coco1_organ, coco2_organ].choose,
					rrand(0.20, 0.30) * #[-1, 1].choose]);
			},
			\memoryPulse, {
				[1, 2].do { |n|
					params[("coco" ++ n ++ "SlotQ").asSymbol] = 0.04;
					params[("coco" ++ n ++ "Hold").asSymbol] = 1;
					params[("coco" ++ n ++ "In").asSymbol] = 0.9;
					params[("coco" ++ n ++ "Fb").asSymbol] = 0.88;
				};
				params[\coco1Dust] = 0.01;
				params[\coco2Dust] = 0.01;
				params[\coco1Speed] = #[14000, 18000, 22000].choose;
				params[\coco2Speed] = #[20000, 26000, 32000].choose;
				skeleton.add([sources[0], \coco1_slotQ, rrand(0.30, 0.42)]);
				skeleton.add([sources[1], \coco2_slotQ, rrand(0.30, 0.42)]);
				skeleton.add([sources[2], \coco1_record, rrand(0.68, 0.82)]);
				skeleton.add([sources[3], \coco2_record, rrand(0.68, 0.82)]);
			},
			\dustGrowth, {
				params[\qFreq] = rrand(3.0, 10.0);
				params[\qChaos] = rrand(0.38, 0.68);
				params[\coco1Dust] = rrand(0.07, 0.14);
				params[\coco2Dust] = rrand(0.15, 0.25);
				params[\coco1Fb] = rrand(0.74, 0.88);
				params[\coco2Fb] = rrand(0.80, 0.92);
				params[\coco1SlotQ] = 0.04;
				params[\coco2SlotQ] = 0.04;
				params[\coco1Speed] = #[5000, 7000, 10000].choose;
				params[\coco2Speed] = #[22000, 28000, 36000].choose;
				skeleton.add([sources[0], \coco1_record, rrand(0.68, 0.82)]);
				skeleton.add([sources[1], \coco2_record, rrand(0.68, 0.82)]);
				skeleton.add([sources[2], #[coco1_slotQ, coco2_slotQ].choose,
					rrand(0.28, 0.38)]);
				// Accepted Dust sockets now make the family's converter weather
				// breathe explicitly instead of leaving both new columns decorative.
				skeleton.add([sources[3], \coco1_dust,
					rrand(0.46, 0.62) * #[-1, 1].choose]);
				skeleton.add([sources[4], \coco2_dust,
					rrand(0.46, 0.62) * #[-1, 1].choose]);
			},
			{ // crossfeed
				[1, 2].do { |n|
					params[("coco" ++ n ++ "Hold").asSymbol] = 1;
					params[("coco" ++ n ++ "In").asSymbol] = 0.86;
					params[("coco" ++ n ++ "Fb").asSymbol] = 0.86;
					params[("coco" ++ n ++ "SlotX").asSymbol] = rrand(0.20, 0.36);
					params[("coco" ++ n ++ "SlotQ").asSymbol] = 0.04;
				};
				params[\coco1Dust] = 0.01;
				params[\coco2Dust] = 0.01;
				params[\coco1Speed] = #[9000, 12000, 16000].choose;
				params[\coco2Speed] = #[18000, 24000, 30000].choose;
				params[\coco2Organ] = 0.03;
				skeleton.add([sources[0], \coco1_record, rrand(0.68, 0.82)]);
				skeleton.add([sources[1], \coco2_record, rrand(0.68, 0.82)]);
				// The crossfeed family finally plants the bridge it is named for.
				skeleton.add([sources[2], \coco1_slotX,
					rrand(0.18, 0.30) * #[-1, 1].choose]);
				skeleton.add([sources[3], \coco2_slotX,
					rrand(0.18, 0.30) * #[-1, 1].choose]);
				skeleton.add([sources[4], \coco2_organ, rrand(0.22, 0.34)]);
			}
		);
		^(family: family, params: params, skeleton: skeleton.asArray)
	}

	prAtLeast { |context, name, floor|
		var current = context[name] ? CQSpec.default(name);
		if(current.isNumber.not) { current = CQSpec.default(name) };
		^max(current.asFloat, floor)
	}

	prPrepareQ { |params|
		var active = (0..4).scramble.keep(3);
		params[\qFreq] = rrand(2.0, 6.5);
		params[\qChaos] = rrand(0.18, 0.48);
		5.do { |i|
			var n = i + 1;
			params[("c" ++ n ++ "Send").asSymbol] = if(active.includes(i)) {
				rrand(0.22, 0.42)
			} { 0.0 };
			params[("c" ++ n ++ "Range").asSymbol] = if(active.includes(i)) {
				if(i == active.first) { \balcium } { \audio }
			} { \infra };
		};
		^active
	}

	// Ten percent of the random field, plus the 3–5-cable audible skeleton: a
	// full but still readable default throw. Gate rows use their deliberately
	// lower sowing probability, so this is no longer a fixed percentage of all
	// 512 cells. All 32 accepted destination rows now participate.
	*shakeDensity { ^0.1 }

	// same manners as clampWeight: a malformed density warns and falls back to
	// the documented default rather than being applied raw (safety invariant 4)
	*clampDensity { |d|
		if(d.isNil) { ^this.shakeDensity };
		if(d.isNumber.not or: { d.asFloat.isNaN }) {
			this.prWarn("bad density % ignored".format(d));
			^this.shakeDensity
		};
		^d.asFloat.clip(0, 1)
	}

	// push every current weight to the server (used once at boot and after clear/load)
	pushAll { this.prPushAll }

	// ---- private ----

	prPush { |di|
		var bus;
		if(graph.isNil or: { graph.isRunning.not }) { ^this };
		bus = graph.buses[destNames[di]];
		bus !? { bus.setn(weights[di]) };
	}

	prPushAll { destNames.do { |d, di| this.prPush(di) } }

	*prWarn { |msg| if(postWarnings) { ("CQMatrix: " ++ msg).warn } }

	// ---- preset data (SPEC §5.5) ----
	// A preset is (matrix: [[src, dest, w] ...], params: (name: value ...)) —
	// hardware patches include knob positions, not just bananas (e.g. \classic
	// is dead without the spaf depth knob up), and SPEC's phase-6 presets
	// (\selfNoise dust levels, \organMode organ levels) need params anyway.
	// Params are CQSpec names, applied through the CQState funnel (clamped).
	// `empty` is a clearing utility and `mayhem` is the deterministic stress edge;
	// the named release set below contains the six musically authored starts.

	*releasePresetNames {
		^#[classic, selfNoise, quantumDrone, organMode, crossCurrent, dustGarden]
	}

	*presets {
		^(
			// no connections — the hardware's default, unpatched state
			empty: (matrix: [], params: ()),

			// "tri→spaf both Cocos, castle→castle FM ring" (SPEC §5.5): tri1
			// (infra, ~0.08 Hz) is the slow tape-drift on both loops, tri3
			// (balcium, ~3 Hz) adds audible flutter on top; the ^ knobs must be
			// up for either to bite (SPEC §5.3: spaf sum is scaled by cocoNSpaf).
			// A 5-castle FM ring layers patchable cross-modulation over the
			// Quantussy's own internal ring
			classic: (
				matrix: [
					[\tri1, \coco1_spaf, 0.35], [\tri1, \coco2_spaf, 0.35],
					[\tri3, \coco1_spaf, 0.12], [\tri3, \coco2_spaf, 0.12],
					[\cas1, \castle2_fm, 0.3], [\cas2, \castle3_fm, 0.3],
					[\cas3, \castle4_fm, 0.3], [\cas4, \castle5_fm, 0.3],
					[\cas5, \castle1_fm, 0.3]
				],
				params: (coco1Spaf: 0.12, coco2Spaf: 0.12)
				// ^ ear-calibrated (KUUNTELU 4): drift ±50 c, flutter ±17 c —
				// "worn but working tape"; 0.15 already tipped into seasick
			),

			// "dust looping + broken weather" (SPEC §5.5, M4): no external input;
			// both converters record their own dust and a gated fraction of the
			// other loop. Small opposite-polarity tris keep an undertow under the
			// feedback VCAs, but held castle values carry most of the gain: growth
			// arrives as unequal plateaus, not two regular ocean swells. The input
			// valves rest low enough that cas1/cas3 can fully cut cross-feed, making
			// real clearings. All four modulation castles are explicitly infra so
			// the preset cannot inherit an audio-rate gate from another overlay.
			// Different speeds and opposing spaf drift keep the pair from phase-lock.
			selfNoise: (
				matrix: [
					[\tri2, \coco1_fbVca, 0.045], [\cas1, \coco1_fbVca, 0.19],
					[\tri4, \coco2_fbVca, -0.04], [\cas3, \coco2_fbVca, -0.21],
					[\cas2, \coco1_inVca, 0.36], [\cas4, \coco2_inVca, 0.36],
					[\tri1, \coco1_spaf, 0.2], [\tri3, \coco2_spaf, -0.24]
				],
				params: (
					coco1Hold: 0, coco2Hold: 0,
					coco1In: 0.42, coco2In: 0.4,
					coco1Fb: 0.86, coco2Fb: 0.82,
					coco1Dust: 0.22, coco2Dust: 0.18,
					coco1SlotPre: 0, coco2SlotPre: 0,
					coco1SlotDeck: 0, coco2SlotDeck: 0,
					coco1SlotQ: 0, coco2SlotQ: 0,
					coco1SlotX: 0.24, coco2SlotX: 0.2,
					coco1Spaf: 0.4, coco2Spaf: 0.4,
					coco1Speed: 24000, coco2Speed: 18000,
					coco1DolbyIn: \off, coco2DolbyIn: \off,
					coco1DolbyFb: \off, coco2DolbyFb: \off,
					coco1Bits: 8, coco2Bits: 8,
					coco1Wrap: 0, coco2Wrap: 0,
					coco1Interp: \zoh, coco2Interp: \zoh,
					coco1Organ: 0, coco2Organ: 0,
					qFreq: 4, qChaos: 0.36,
					c1Range: \infra, c2Range: \infra,
					c3Range: \infra, c4Range: \infra
				)
			),

			// "castles as audio, skip/flip animation" (SPEC §5.5): four castles up
			// on audio range as the drone cluster (castle 3 stays balcium = the
			// animator), both cocos record the qMix smear (fb just under unity).
			// cas3 gates the flips with OPPOSITE polarity — the pair reverses
			// alternately; tri1 fires an occasional skip (phrase reset ~12 s);
			// a mutual castle-FM pair thickens the cluster; slow anti-phase spaf
			// makes the two loops breathe apart. Coco2 an octave down for weight
			quantumDrone: (
				matrix: [
					[\cas3, \coco1_flip, 0.6], [\cas3, \coco2_flip, -0.6],
					[\tri1, \coco2_skip, 0.55],
					[\cas1, \castle2_fm, 0.25], [\cas2, \castle1_fm, 0.25],
					[\tri1, \coco1_spaf, 0.3], [\tri1, \coco2_spaf, -0.3]
				],
				params: (
					c1Range: \audio, c2Range: \audio, c3Range: \balcium,
					c4Range: \audio, c5Range: \audio,
					c1Send: 0.4, c2Send: 0.4, c3Send: 0, c4Send: 0.4, c5Send: 0.5,
					qChaos: 0.25,
					coco1SlotQ: 1.0, coco2SlotQ: 1.0,
					coco1SlotPre: 0, coco2SlotPre: 0,
					coco1SlotDeck: 0, coco2SlotDeck: 0,
					coco1Hold: 0, coco2Hold: 0,
					coco1Fb: 0.95, coco2Fb: 0.95,
					coco1Spaf: 0.25, coco2Spaf: 0.25,
					coco2Speed: 12000
				)
			),

			// "organ outs as voices, cas->VCAs, tri->spaf light" (SPEC §5.5): the
			// address-counter squares up on both cocos as sustained voices — speeds
			// in a 3:2 ratio put organ 2 a fifth below organ 1 (play melodies with
			// the speed faders F2/F6). cas steps pump the loop bed's VCAs under the
			// organs; tri3 (~3 Hz) through a light spaf knob is the shared vibrato
			// (spaf moves the RAM clock -> organ pitch follows, SPEC §5.3)
			organMode: (
				matrix: [
					[\cas1, \coco1_inVca, 0.4], [\cas4, \coco2_inVca, 0.4],
					[\cas2, \coco1_fbVca, 0.3], [\cas5, \coco2_fbVca, 0.3],
					[\tri3, \coco1_spaf, 0.2], [\tri3, \coco2_spaf, 0.2]
				],
				params: (
					coco1Organ: 0.7, coco2Organ: 0.7,
					coco1Spaf: 0.1, coco2Spaf: 0.1,
					coco1Hold: 0, coco2Hold: 0,
					coco1Fb: 0.9, coco2Fb: 0.9,
					coco2Speed: 16000
				)
			),

			// "two memories trading weather": the newly accepted SlotX columns are
			// the form, not decoration. Opposed slow bridge pulls keep either Coco
			// becoming a static copy of the other; two castle gates open held RAM in
			// alternating phrases. Quiet organ and qMix layers make the state speak
			// from empty buffers, while unequal clocks turn each exchange into new
			// material rather than a louder feedback loop.
			crossCurrent: (
				matrix: [
					[\tri1, \coco1_slotX, 0.26], [\tri2, \coco2_slotX, -0.28],
					[\cas2, \coco1_record, 0.72], [\cas4, \coco2_record, 0.74],
					[\tri3, \coco1_spaf, 0.15], [\tri3, \coco2_spaf, -0.17],
					[\cas1, \coco1_organ, 0.24], [\cas5, \coco2_organ, 0.23],
					[\cas3, \castle4_fm, 0.22], [\cas4, \castle2_fm, -0.20]
				],
				params: (
					coco1Hold: 1, coco2Hold: 1,
					coco1SlotPre: 0, coco2SlotPre: 0,
					coco1SlotDeck: 0, coco2SlotDeck: 0,
					coco1In: 0.82, coco2In: 0.82,
					coco1Fb: 0.84, coco2Fb: 0.88,
					coco1Dust: 0.025, coco2Dust: 0.035,
					coco1DustGrain: 0.38, coco2DustGrain: 0.66,
					coco1Organ: 0.055, coco2Organ: 0.05,
					coco1SlotQ: 0.10, coco2SlotQ: 0.08,
					coco1SlotX: 0.18, coco2SlotX: 0.22,
					coco1Spaf: 0.18, coco2Spaf: 0.18,
					coco1Speed: 12000, coco2Speed: 27000,
					coco1Level: 0.90, coco2Level: 0.90,
					coco1Pan: -0.32, coco2Pan: 0.32,
					qFreq: 3.4, qChaos: 0.32,
					c1Range: \infra, c2Range: \audio, c3Range: \balcium,
					c4Range: \audio, c5Range: \audio,
					c1Send: 0.12, c2Send: 0.22, c3Send: 0,
					c4Send: 0.18, c5Send: 0.25
				)
			),

			// "mist on one shore, chips on the other": both accepted Dust columns
			// breathe under different clocks and opposite grain biases. Record gates
			// close holes in an otherwise writing pair, while small SlotX pulls let
			// fragments migrate between the soft and brittle converters. This is the
			// release set's quietest self-starting texture, not a second selfNoise.
			dustGarden: (
				matrix: [
					[\tri1, \coco1_dust, 0.56], [\tri4, \coco2_dust, -0.60],
					[\cas2, \coco1_fbVca, -0.20], [\cas5, \coco2_fbVca, 0.22],
					[\tri3, \coco1_slotX, 0.20], [\tri2, \coco2_slotX, -0.18],
					[\cas3, \coco1_record, 0.70], [\cas1, \coco2_record, 0.72],
					[\cas4, \castle5_fm, 0.22], [\cas5, \castle2_fm, -0.18]
				],
				params: (
					coco1Hold: 0, coco2Hold: 0,
					coco1SlotPre: 0, coco2SlotPre: 0,
					coco1SlotDeck: 0, coco2SlotDeck: 0,
					coco1In: 0.62, coco2In: 0.68,
					coco1Fb: 0.78, coco2Fb: 0.84,
					coco1Dust: 0.12, coco2Dust: 0.20,
					coco1DustGrain: 0.16, coco2DustGrain: 0.84,
					coco1Organ: 0.015, coco2Organ: 0.025,
					coco1SlotQ: 0.08, coco2SlotQ: 0.12,
					coco1SlotX: 0.08, coco2SlotX: 0.10,
					coco1Spaf: 0.22, coco2Spaf: 0.22,
					coco1Speed: 6500, coco2Speed: 34000,
					coco1Bits: 7, coco2Bits: 9,
					coco1DolbyFb: \gate, coco2DolbyFb: \off,
					coco1Level: 0.95, coco2Level: 0.85,
					coco1Pan: -0.42, coco2Pan: 0.42,
					qFreq: 2.7, qChaos: 0.54,
					c1Range: \infra, c2Range: \balcium, c3Range: \audio,
					c4Range: \infra, c5Range: \audio,
					c1Send: 0.08, c2Send: 0.12, c3Send: 0.18,
					c4Send: 0, c5Send: 0.16
				)
			),

			// "dense random, capped depths" (SPEC §5.5): every cell patched, but at
			// deterministic pseudo-random depths (sin hash, |w| <= 0.55) instead of
			// phase 4's rigid ±0.6 checkerboard — varied magnitudes decorrelate the
			// mod sums, so gates/trigs fire irregularly instead of in lockstep
			// (phase-6 musical tuning). Deterministic on purpose: this same data is
			// the SPEC A3 worst-case stress patch (nrt_safety.scd) and must render
			// reproducibly. Zero-rounding cells are floored to 0.05 to stay dense
			mayhem: (
				matrix: destNames.collect { |dest, di|
					sourceNames.collect { |src, si|
						var w = (sin((si * 5.7) + (di * 2.3)) * 0.55).round(0.01);
						[src, dest, if(w == 0.0) { 0.05 } { w }]
					}
				}.flatten(1),
				params: ()
			)
		)
	}
}
