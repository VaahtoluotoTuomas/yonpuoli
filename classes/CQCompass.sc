// CQCompass — the weather machine's four matrix memories and the tide that
// blends them (GUI_SPEC §7.1, amended at G3). Pure model: no Qt, no server —
// corner storage, the blend/breed/grow/wither math and the versioned
// `compass.cqcompass.scd` persistence live here, unit-testable with dir = nil.
// Every loaded value re-enters CQMatrix.clampWeight; a hand-edited or
// malformed file degrades to warnings, never to raw values (SPEC §8.3 —
// CQSnapshots' safe-load manners, deliberately copied).
//
// **The tide** (decided at G3, superseding §7.1's first bilinear-amplitude
// sketch): a voyage across the pad does NOT scale every weight toward mush.
// Cells the corners SHARE crossfade their weights continuously — the common
// skeleton glides — while cells only some corners carry keep their full
// playing weight until the position's pull on them falls below that cell's
// own fixed tide mark, and then they lift, one banana at a time. Three
// reasons, all musical:
//   1. a patch is discrete bananas, and half-way between two patches should
//      be half the bananas of each at full voice — not all of them at half
//      voice (an amplitude blend of four sparse patches quadruples the cable
//      count and quarters every weight: mush by construction);
//   2. gate/trig cables live behind a +0.5 Schmitt (SPEC §5.5): an amplitude
//      blend silences every flip/skip/spesal banana almost everywhere on the
//      pad, while the tide keeps them firing through the whole voyage;
//   3. the tide marks are a fixed hash of each cell's coordinates, so the
//      same position always grows the same patch: the pad is learnable
//      geography, not a slot machine.
// Traveling toward an empty corner is the documented wither-by-travel, and
// under the tide it is literally that: cables dropping off one by one.

CQCompass {
	classvar <>postWarnings = true;
	classvar tideCache;

	var <dir;        // nil = memory only (serverless previews, unit tests)
	var corners;     // 4 slots (TL, TR, BL, BR): nil = empty water, or dense [22][16]

	*new { |dir| ^super.newCopyArgs(dir !? { dir.asString }).prInit }

	*version { ^1 }
	*fileName { ^"compass.cqcompass.scd" }

	// wither's own floor (GUI_SPEC §10) is also the tide's and breed's: a cable
	// below it is a ghost hair no hand could have planted, so it rounds to gone
	*weightFloor { ^0.05 }

	prInit {
		corners = Array.newClear(4);
		this.prLoad;
		^this
	}

	// ---- corners ----

	corner { |i| ^corners[i !? { i.asInteger.clip(0, 3) }] }
	stampedAt { |i| ^corners[i.asInteger.clip(0, 3)].notNil }
	stampedMask { ^4.collect { |i| corners[i].notNil } }

	// capture a dense matrix into a corner (weights only, ever — the landscape
	// law: no params, no spesal). Clamped cell by cell on the way in, so a
	// stamp can never hold what the funnel would not.
	stamp { |i, dense|
		var slot = i.asInteger.clip(0, 3);
		if(this.prDenseShaped(dense).not) {
			this.class.prWarn("stamp % rejected: not a %x% dense matrix"
				.format(i, CQMatrix.destNames.size, CQMatrix.sourceNames.size));
			^this
		};
		corners[slot] = dense.collect { |row|
			row.collect { |w| CQMatrix.clampWeight(w) }
		};
		this.prSave;
		^this
	}

	clear { |i|
		corners[i.asInteger.clip(0, 3)] = nil;
		this.prSave;
		^this
	}

	blendAt { |x, y| ^this.class.blend(corners, x, y) }

	// ---- the tide (pure) ----

	// bilinear pull of a pad position on the four corners, order TL TR BL BR
	// (y grows downward, as on the drawn pad)
	*bilinear { |x, y|
		var u = x.clip(0, 1), v = y.clip(0, 1);
		^[(1 - u) * (1 - v), u * (1 - v), (1 - u) * v, u * v]
	}

	// each cell's fixed tide mark: a deterministic hash of its coordinates,
	// clipped off the exact ends so no cable is unconditionally eternal or
	// unconditionally drowned. NOT session-seeded on purpose — the pad's
	// geography must survive a restart, or nothing learned on it is real.
	*tideMarks {
		^tideCache ?? {
			tideCache = CQMatrix.destNames.collect { |dest, di|
				CQMatrix.sourceNames.collect { |src, si|
					(sin((si * 12.9898) + (di * 78.233)) * 43758.5453)
						.frac.abs.clip(0.03, 0.97)
				}
			};
			tideCache
		}
	}

	// the blend: for every cell, `presence` is how hard this position pulls on
	// the corners that actually carry the cell. Above the cell's tide mark the
	// cable stands, at the pull-weighted average of the weights those corners
	// gave it — exactly its stamped weight wherever only one corner has it —
	// and at a pure corner the stamp comes back bit-exact. Below the mark the
	// water takes it. An empty corner is the zero matrix, so pulling toward it
	// sinks cables one by one (wither-by-travel).
	*blend { |corners, x, y|
		var u = this.bilinear(x, y);
		var marks = this.tideMarks;
		var floor = this.weightFloor;
		^CQMatrix.destNames.collect { |dest, di|
			CQMatrix.sourceNames.collect { |src, si|
				var presence = 0.0, sum = 0.0, w;
				4.do { |c|
					var m = corners[c];
					if(m.notNil and: { m[di][si] != 0.0 }) {
						presence = presence + u[c];
						sum = sum + (u[c] * m[di][si]);
					};
				};
				if(presence > marks[di][si]) {
					w = sum / presence;
					if(w.abs < floor) { 0.0 } { w }
				} { 0.0 }
			}
		}
	}

	// ---- breed (GUI_SPEC §7.3, exact constants) ----
	// per cell: coin-pick a parent's weight; with p = 0.08 mutate by ±U(0, 0.15);
	// clamp. Children differ but rhyme. Language-side rand on purpose — seeded
	// chaos stays in the code layer (CQ.shake(amt, seed:)).
	*breedMutateProb { ^0.08 }
	*breedMutateAmount { ^0.15 }

	*breed { |a, b|
		var floor = this.weightFloor;
		^CQMatrix.destNames.collect { |dest, di|
			CQMatrix.sourceNames.collect { |src, si|
				var w = if(0.5.coin) {
					a !? { a[di][si] } ? 0.0
				} {
					b !? { b[di][si] } ? 0.0
				};
				if(this.breedMutateProb.coin) {
					w = w + this.breedMutateAmount.rand2;
				};
				w = CQMatrix.clampWeight(w);
				if(w.abs < floor) { 0.0 } { w }
			}
		}
	}

	// ---- grow / wither (GUI_SPEC §7.2, exact constants) ----
	// One step each; the GUI repeats them at growRate while the mark is held.
	// Both return [di, si, w] — the single cell they touched — or nil when the
	// bay has nothing left to give (grow on a full table, wither on an empty).

	*growStep { |dense|
		var open = List.new;
		dense.do { |row, di|
			row.do { |w, si| if(w == 0.0) { open.add([di, si]) } };
		};
		if(open.isEmpty) { ^nil };
		^open.choose ++ [rrand(0.15, 0.4) * #[-1, 1].choose]
	}

	*witherStep { |dense|
		var held = List.new, cell, w;
		dense.do { |row, di|
			row.do { |weight, si| if(weight != 0.0) { held.add([di, si]) } };
		};
		if(held.isEmpty) { ^nil };
		cell = held.choose;
		w = dense[cell[0]][cell[1]] * 0.7;
		if(w.abs < this.weightFloor) { w = 0.0 };
		^cell ++ [w]
	}

	// ---- persistence (the CQSnapshots manners) ----

	path { ^dir !? { dir +/+ this.class.fileName } }

	// sparse triples per corner — the snapshot matrix format, one more time,
	// so a hand can read and edit the file in the language it already knows
	prSparse { |dense|
		var out = List.new;
		dense.do { |row, di|
			row.do { |w, si|
				if(w != 0.0) {
					out.add([CQMatrix.sourceNames[si], CQMatrix.destNames[di], w])
				};
			};
		};
		^out.asArray
	}

	prSave {
		var path = this.path, data;
		if(path.isNil) { ^this };
		data = (
			version: this.class.version,
			corners: corners.collect { |dense|
				dense !? { this.prSparse(dense) }
			}
		);
		try {
			if(File.exists(dir).not) { File.mkdir(dir) };
			File.use(path, "w", { |f| f.write(data.asCompileString) });
		} { |err|
			this.class.prWarn("could not save %: %".format(path, err.errorString));
		};
		^this
	}

	prLoad {
		var path = this.path, data;
		if(path.isNil or: { File.exists(path).not }) { ^this };
		data = try { path.load } { |err|
			this.class.prWarn("could not read %: %".format(path, err.errorString));
			nil
		};
		this.prValidate(data) !? { |loaded| corners = loaded };
		^this
	}

	// structural gate: wrong shape or a newer version rejects the whole file
	// (empty corners stay empty water); per-cell junk is skipped with a warning
	// and every surviving weight re-enters the clamp
	prValidate { |data|
		var raw;
		if(data.isKindOf(Event).not) {
			this.class.prWarn("compass file is not an Event — rejected");
			^nil
		};
		if(data[\version].isInteger.not) {
			this.class.prWarn("compass file has no integer version — rejected");
			^nil
		};
		if(data[\version] > this.class.version) {
			this.class.prWarn("compass file is version % — newer than this build"
				.format(data[\version]));
			^nil
		};
		raw = data[\corners];
		if(raw.isKindOf(SequenceableCollection).not or: { raw.isString }
			or: { raw.size != 4 }) {
			this.class.prWarn("compass file needs 4 corners — rejected");
			^nil
		};
		^4.collect { |i| this.prValidCorner(raw[i], i) }
	}

	prValidCorner { |sparse, i|
		var dense;
		if(sparse.isNil) { ^nil };
		if(sparse.isKindOf(SequenceableCollection).not or: { sparse.isString }) {
			this.class.prWarn("corner % is not a triple list — left empty".format(i));
			^nil
		};
		dense = CQMatrix.destNames.collect { CQMatrix.sourceNames.collect { 0.0 } };
		sparse.do { |t|
			var si, di;
			if(t.isKindOf(SequenceableCollection) and: { t.isString.not }
				and: { t.size == 3 }) {
				si = CQMatrix.srcIndex(t[0].asSymbol);
				di = CQMatrix.destIndex(t[1].asSymbol);
				if(si.notNil and: { di.notNil }) {
					dense[di][si] = CQMatrix.clampWeight(t[2]);
				} {
					this.class.prWarn("unknown patch % -> % in corner % skipped"
						.format(t[0], t[1], i));
				};
			} {
				this.class.prWarn("malformed entry % in corner % skipped".format(t, i));
			};
		};
		^dense
	}

	prDenseShaped { |dense|
		^dense.isKindOf(SequenceableCollection)
			and: { dense.size == CQMatrix.destNames.size }
			and: { dense.every { |row|
				row.isKindOf(SequenceableCollection)
					and: { row.size == CQMatrix.sourceNames.size }
			} }
	}

	*prWarn { |msg| if(postWarnings) { ("CQCompass: " ++ msg).warn } }
}
