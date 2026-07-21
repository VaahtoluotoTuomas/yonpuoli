// CQMatrix — the routing matrix (banana bay), SPEC §5.5 / ARCHITECTURE §3.2–3.3.
// Same funnel shape as CQState: patch() clamps the weight, stores it, mirrors it
// to the destination's 16-channel wDest control bus via setn, and notifies
// dependants. Works with graph = nil (pure value store) so it is unit-testable.
// Weight range -1.0–1.0 is owned here (SPEC §5.5), not in CQSpec: matrix cells
// are name-pair-indexed, not a CQSpec scalar parameter.

CQMatrix {
	classvar <sourceNames, <destNames;
	classvar <>postWarnings = true;

	var <graph;
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
			\coco2_spaf, \coco2_inVca, \coco2_fbVca, \coco2_flip, \coco2_skip,
			\castle1_fm, \castle2_fm, \castle3_fm, \castle4_fm, \castle5_fm,
			\castle1_spesal, \castle2_spesal, \castle3_spesal, \castle4_spesal,
			\castle5_spesal, \deckA_rate, \deckB_rate
		];
	}

	*new { |graph| ^super.newCopyArgs(graph).prInit }

	prInit {
		weights = destNames.collect { sourceNames.collect { 0.0 } };
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

	// SPEC §5.5 shake: throw a fresh handful of bananas on the bay — a sparse
	// random patch (each cell patched with prob. shakeDensity, weight ±amount)
	// plus 5 random spesal states, returned for the caller to apply
	// (Cocoquantus.shake; this class knows no server counters). Never touches
	// levels/feedback by construction: only weights live here. A seed makes the
	// throw reproducible (SPEC: shake(0.4, seed: 1234)); the language-side rand
	// stream is restored afterwards so a seeded shake has no side effects.
	shake { |amount = 0.3, seed, origin = \code, committed = true,
		operation = \shake|
		var amt = this.class.clampWeight(amount).abs;
		var saved = thisThread.randData;
		var spesal;
		seed !? { thisThread.randSeed = seed.asInteger };
		this.prInit;
		weights.do { |row, di|
			row.size.do { |si|
				if(this.class.shakeDensity.coin) { weights[di][si] = amt.rand2 };
			};
		};
		spesal = 5.collect { 8.rand };
		seed !? { thisThread.randData = saved };
		this.prPushAll;
		this.changed(\clear, origin, committed, operation);
		// bulk notification: dependants re-read everything
		^spesal
	}

	// ~10% of 16x22 cells = ~35 bananas — a full but still readable table
	*shakeDensity { ^0.1 }

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

			// "dust looping + VCA wobble" (SPEC §5.5, M4): the no-input digital-dust
			// workflow as one gesture — external slots closed, both converters
			// recording (hold 0) over unity feedback so their own dust accumulates;
			// slotX cross-feed makes the two loops chew on each other. Infra tris
			// breathe the feedback VCAs (dust-density waves), infra/balcium cas
			// steps gate the cross-feed, slow spaf drift keeps the grain shifting.
			// Different speeds = different loop lengths -> the pair never phase-locks
			selfNoise: (
				matrix: [
					[\tri2, \coco1_fbVca, 0.15], [\tri4, \coco2_fbVca, 0.15],
					[\cas1, \coco1_inVca, 0.3], [\cas3, \coco2_inVca, 0.3],
					[\tri1, \coco1_spaf, 0.25], [\tri3, \coco2_spaf, 0.25]
				],
				params: (
					coco1Hold: 0, coco2Hold: 0,
					coco1Fb: 1.1, coco2Fb: 1.1,
					coco1Dust: 0.3, coco2Dust: 0.25,
					coco1SlotPre: 0, coco2SlotPre: 0,
					coco1SlotDeck: 0, coco2SlotDeck: 0,
					coco1SlotX: 0.3, coco2SlotX: 0.3,
					coco1Spaf: 0.4, coco2Spaf: 0.4,
					coco2Speed: 18000
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
