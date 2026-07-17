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

	patch { |src, dest, w = 1.0|
		var si = sourceNames.indexOf(src), di = destNames.indexOf(dest);
		if(si.isNil or: { di.isNil }) {
			this.class.prWarn("unknown patch % -> %".format(src, dest));
			^this
		};
		weights[di][si] = this.class.clampWeight(w);
		this.prPush(di);
		this.changed(\patch, src, dest, weights[di][si]);
	}

	unpatch { |src, dest| this.patch(src, dest, 0.0) }

	clearPatch {
		this.prInit;
		this.prPushAll;
		this.changed(\clear);
	}

	getPatch { |src, dest|
		var si = sourceNames.indexOf(src), di = destNames.indexOf(dest);
		^if(si.isNil or: { di.isNil }) { 0.0 } { weights[di][si] }
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

	load { |triples|
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
		this.changed(\clear);   // one bulk notification instead of one per cell
	}

	// applies the preset's MATRIX part only; the params part goes through the
	// CQState funnel and is applied by Cocoquantus.patchPreset (CQMatrix must
	// not know about CQState — ARCH §5 separation)
	preset { |name|
		var data = this.class.presets[name];
		if(data.isNil) {
			this.class.prWarn("unknown preset '" ++ name ++ "'");
			^this
		};
		this.load(data[\matrix]);
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
				params: (coco1Spaf: 0.8, coco2Spaf: 0.8)
			),

			// "dense random, capped depths" (SPEC §5.5): every source patched to
			// every destination at a fixed, sub-maximal depth (deterministic, not
			// literally random, so the A3 safety render is reproducible) —
			// doubles as the SPEC A3 worst-case stress patch; musical tuning
			// happens in phase 6
			mayhem: (
				matrix: destNames.collect { |dest, di|
					sourceNames.collect { |src, si|
						[src, dest, 0.6 * (if((si + di).even) { 1 } { -1 })]
					}
				}.flatten(1),
				params: ()
			)
		)
	}
}
