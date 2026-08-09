// CQSnapshots — save/recall/morph (ARCHITECTURE §5/§7, SPEC §6.3/§7). File-backed
// memory: one plain-text .cqsnap.scd per snapshot in the snapshots folder. Every
// value loaded from disk re-enters the funnels (CQState.set / CQMatrix clamp) —
// a hand-edited or malformed file degrades to warnings, never to raw values
// (SPEC safety #3). Spesal states travel through injected get/set functions so
// this class needs no server knowledge (unit-testable with graph = nil).
//
// micGain policy (SPEC §7): save() stores micGain as 0 unless includeMic: true —
// recalling an auto-saved snapshot can never open the mic. An explicitly saved
// (or hand-edited) micGain > 0 is applied clamped, with a warning.
// monitorMute is never saved or recalled: recall must not defeat panic
// (SPEC safety #3, "panic always reachable").

CQSnapshots {
	classvar <>postWarnings = true;
	var state, matrix, <dir, getSpesal, setSpesal, <legacyDir;
	var morphRoutine;
	var <morphProgress = 0.0, <morphFrom, <morphTo;

	*new { |state, matrix, dir, getSpesal, setSpesal, legacyDir|
		^super.newCopyArgs(state, matrix, dir, getSpesal, setSpesal, legacyDir)
	}

	// Versioned discovery material. The directory may also contain any number of
	// gitignored player memories; this manifest names only the deliberately
	// shipped files and lets tests audit them without touching personal sessions.
	*releaseNames {
		^#[example, undertow, dust_bloom, glass_tide, organ_lanterns]
	}

	save { |name, includeMic = false, note = ""|
		var safe = this.prSafeName(name);
		var path, params, data, text, d;
		if(safe.isNil) { ^nil };
		params = state.asEvent;
		params.removeAt(\monitorMute);                 // panic is not memory
		if(includeMic.not) { params[\micGain] = 0 };   // SPEC §7 mic safety
		d = Date.getDate;
		data = (
			version: 1,
			params: params,
			matrix: matrix.asArray,
			spesal: getSpesal.value ?? { [0, 0, 0, 0, 0] },
			meta: (name: safe, note: note.asString,
				date: "%-%-%".format(d.year,
					d.month.asString.padLeft(2, "0"), d.day.asString.padLeft(2, "0")))
		);
		if(File.exists(dir).not) { File.mkdir(dir) };
		path = this.prPath(safe);
		// Object's pretty-printer wraps long Events as "comma + space + newline".
		// The space is not data; remove it so versioned snapshots stay diff-clean.
		text = data.asCompileString.replace(" \n", "\n");
		File.use(path, "w", { |f| f.write(text) });
		("CQSnapshots: saved '" ++ safe ++ "' -> " ++ path).postln;
		^path
	}

	recall { |name|
		var data = this.read(name);
		if(data.isNil) { ^false };
		this.stopMorph;
		this.prApply(data, \snapshot, true, \recall);
		^true
	}

	// interpolate snapshot 'from' -> 'to': continuous params in normalized
	// (curve) space + matrix weights at 30 Hz, toggles/enums/spesal switch at the
	// midpoint, final tick applies 'to' exactly (== recall(to), incl. mic policy).
	// Cells patched in neither snapshot are cleared only by that final apply.
	// MOR-1 (measured 2026-07-30): morph frames redraw the cable overlay, so the
	// frame rate is GUI_SPEC §10's 15 Hz ambient ceiling, not 30 Hz. Matrix
	// weights are already smoothed 50 ms server-side and every gain-affecting
	// arg is lagged >= 30 ms, so a 66 ms frame is inaudible as a step.
	*morphTickPeriod { ^1/15 }
	// A frame that overruns still yields this much, so the Qt event loop drains
	// and panic stays reachable no matter how dense the pair is.
	*morphMinYield { ^0.004 }
	*morphMinDur { ^0.001 }

	morph { |from, to, dur = 10|
		var a = this.read(from), b = this.read(to);
		var contNames, aNorm, bNorm, cells, cellIndex, startTime, moving, still;
		if(a.isNil or: { b.isNil }) { ^this };
		this.stopMorph;
		morphFrom = from.asSymbol;
		morphTo = to.asSymbol;
		morphProgress = 0.0;
		this.changed(\morph, morphProgress, morphFrom, morphTo, \running);
		contNames = CQSpec.names.select { |n|
			#[num, int].includes(CQSpec.kind(n)) and: { n != \micGain }
		};
		// sparse files interpolate from/to the current value (unmap clamps)
		aNorm = contNames.collect { |n| CQSpec.unmap(n, a[\params][n] ?? { state.get(n) }) };
		bNorm = contNames.collect { |n| CQSpec.unmap(n, b[\params][n] ?? { state.get(n) }) };
		// SOAK-1 finding A: a morph writes what moves. A parameter that reads the
		// same in both snapshots is not interpolating — re-sending its unchanged
		// value 15 times a second is pure cost, and for a pair that differs only
		// in its matrix (the soak's own case) it was the entire per-frame param
		// load. The still ones are written once, below, and then left alone.
		moving = List.new;
		still = Event.new;
		contNames.do { |n, j|
			if(aNorm[j] == bNorm[j]) {
				still[n] = CQSpec.map(n, aNorm[j]);
			} {
				moving.add([n, aNorm[j], bNorm[j]]);
			};
		};
		moving = moving.asArray;
		// The still ones land once, at the start, exactly where both snapshots
		// already agree. A hand that moves one of them mid-morph now keeps it,
		// where the old per-frame rewrite overruled the player 15 times a second.
		if(still.size > 0) { state.setAll(still) };
		// unknown src/dest names are dropped here (not each tick) — patch() would
		// warn 30×/s otherwise; prApply's matrix.load still warns once at the end
		// MOR-1: keep the union as resolved [dest, src] indices so a frame writes
		// one dense matrix instead of looking a name pair up per cell per tick.
		cells = List.new;
		cellIndex = Dictionary.new;
		a[\matrix].do { |t|
			var si = CQMatrix.srcIndex(t[0]), di = CQMatrix.destIndex(t[1]);
			if(si.notNil and: { di.notNil }) {
				cellIndex[[di, si]] = cells.size;
				cells.add([di, si, CQMatrix.clampWeight(t[2]), 0.0]);
			};
		};
		b[\matrix].do { |t|
			var si = CQMatrix.srcIndex(t[0]), di = CQMatrix.destIndex(t[1]);
			var at;
			if(si.notNil and: { di.notNil }) {
				at = cellIndex[[di, si]];
				if(at.isNil) {
					cellIndex[[di, si]] = cells.size;
					cells.add([di, si, 0.0, CQMatrix.clampWeight(t[2])]);
				} {
					cells[at][3] = CQMatrix.clampWeight(t[2]);
				};
			};
		};
		cells = cells.asArray;
		dur = dur.max(this.class.morphMinDur);
		// MOR-1: a morph is a function of elapsed time, not of a fixed tick count.
		// Under load it drops frames and still lands on the requested duration,
		// instead of falling behind for minutes with the language thread pinned.
		startTime = Main.elapsedTime;
		morphRoutine = Routine {
			var switched = false, t = 0, tickStart, dense, frame;
			while { t < 1 } {
				tickStart = Main.elapsedTime;
				t = ((tickStart - startTime) / dur).clip(0, 1);
				morphProgress = t;
				this.changed(\morph, morphProgress, morphFrom, morphTo, \running);
				if(moving.size > 0) {
					frame = Event.new;
					moving.do { |m|
						frame[m[0]] = CQSpec.map(m[0], m[1] + ((m[2] - m[1]) * t));
					};
					state.setAll(frame);
				};
				// One dense write per frame, not one per cell. Per-cell patching
				// cost a server push AND a dependant notification each, so a dense
				// pair drove tens of thousands of deferred GUI closures per second
				// into a Qt queue that filled faster than it drained: the window
				// froze and PANIC could not be reached (safety invariant #3).
				// Reading the live matrix each frame keeps cells outside both
				// snapshots exactly as untouched as per-cell patching left them.
				if(cells.size > 0) {
					dense = matrix.fullMatrix;
					cells.do { |cell|
						dense[cell[0]][cell[1]] =
							cell[2] + ((cell[3] - cell[2]) * t);
					};
					matrix.writeMatrix(dense, origin: \morph, committed: false,
						operation: \morphFrame);
				};
				if(switched.not and: { t >= 0.5 }) {
					switched = true;
					this.prApplyDiscrete(b);
				};
				// Always hand the event loop a slice, however late this frame is.
				// SystemClock would otherwise re-run an overdue routine at once.
				(this.class.morphTickPeriod - (Main.elapsedTime - tickStart))
					.max(this.class.morphMinYield).wait;
			};
			morphRoutine = nil;
			this.prApply(b, \snapshot, true, \morph);
			this.changed(\morph, morphProgress, morphFrom, morphTo, \complete);
		}.play(SystemClock);
		("CQSnapshots: morphing '%' -> '%' over % s".format(from, to, dur)).postln;
	}

	isMorphing { ^morphRoutine.notNil }

	stopMorph {
		var wasMorphing = morphRoutine.notNil;
		morphRoutine !? { morphRoutine.stop };
		morphRoutine = nil;
		if(wasMorphing) {
			this.changed(\morph, morphProgress, morphFrom, morphTo, \stopped)
		};
	}

	// snapshot names available on disk, sorted
	list {
		var paths = (dir +/+ "*.cqsnap.scd").pathMatch;
		if(legacyDir.notNil and: { legacyDir != dir }) {
			paths = paths ++ (legacyDir +/+ "*.cqsnap.scd").pathMatch;
		};
		^paths.collect { |p| p.basename.replace(".cqsnap.scd", "") }
			.asSet.asArray.collect(_.asString).sort
	}

	// load + structurally validate one snapshot file -> data Event or nil
	read { |name|
		var safe = this.prSafeName(name);
		var path, legacyPath, data;
		if(safe.isNil) { ^nil };
		path = this.prPath(safe);
		if(File.exists(path).not
			and: { legacyDir.notNil and: { legacyDir != dir } }) {
			legacyPath = this.prPathIn(legacyDir, safe);
			if(File.exists(legacyPath)) { path = legacyPath };
		};
		if(File.exists(path).not) {
			this.class.prWarn("no snapshot '%' (%)".format(safe, path));
			^nil
		};
		data = try { path.load } { |err|
			this.class.prWarn("could not read %: %".format(path, err.errorString));
			nil
		};
		^this.prValidate(data, safe)
	}

	free { this.stopMorph }

	// ---- private ----

	prPath { |safe| ^this.prPathIn(dir, safe) }
	prPathIn { |base, safe| ^base +/+ (safe ++ ".cqsnap.scd") }

	// filenames stay portable: letters/digits/_/- pass, anything else becomes _
	prSafeName { |name|
		var s = name.asString;
		if(s.isEmpty) {
			this.class.prWarn("empty snapshot name");
			^nil
		};
		^s.collect { |ch| if(ch.isAlphaNum or: { "_-".includes(ch) }) { ch } { $_ } }
	}

	// structural gate (ARCH §7): wrong shape or a newer version rejects the file;
	// per-value clamping happens later in the funnels
	prValidate { |data, name|
		var sp;
		if(data.isKindOf(Event).not) {
			this.class.prWarn("'%' is not a snapshot Event — rejected".format(name));
			^nil
		};
		if(data[\version].isInteger.not) {
			this.class.prWarn("'%' has no integer version — rejected".format(name));
			^nil
		};
		if(data[\version] > 1) {
			this.class.prWarn("'%' is version % — newer than this build understands"
				.format(name, data[\version]));
			^nil
		};
		if(data[\params].isKindOf(Event).not) {
			this.class.prWarn("'%' has no params Event — rejected".format(name));
			^nil
		};
		data[\matrix] = (data[\matrix] ? []).asArray.select { |t|
			var ok = t.isKindOf(SequenceableCollection)
				and: { t.isString.not } and: { t.size == 3 };
			if(ok.not) {
				this.class.prWarn("malformed matrix entry % skipped".format(t));
			};
			ok
		};
		sp = data[\spesal];
		if(sp.isKindOf(SequenceableCollection) and: { sp.isString.not }
			and: { sp.size == 5 }
			and: { sp.every { |v|
				v.isNumber and: { v.asFloat.isNaN.not } and: { v.abs != inf } } }) {
			data[\spesal] = sp.collect { |v| v.round.asInteger.mod(8) };
		} {
			if(sp.notNil) {
				this.class.prWarn("bad spesal data % ignored".format(sp));
			};
			data[\spesal] = nil;
		};
		^data
	}

	// apply everything through the funnels; sparse params leave the rest untouched
	// A recall is one act, so it is one write and one announcement — not 76 of
	// each. The filtering (panic is not memory, unknown names are skipped) still
	// happens here, before anything reaches the funnel (SOAK-1 finding A).
	prApply { |data, origin = \snapshot, committed = true, operation = \recall|
		var wanted = Event.new;
		data[\params].keysValuesDo { |k, v|
			case
			{ k == \monitorMute } {
				this.class.prWarn("snapshot monitorMute ignored (panic is not memory)");
			}
			{ CQSpec.includes(k).not } {
				this.class.prWarn("unknown param '%' skipped".format(k));
			}
			{ wanted[k] = v };
		};
		state.setAll(wanted);
		if(data[\params].includesKey(\micGain) and: { (state.get(\micGain) ? 0) > 0 }) {
			this.class.prWarn("this snapshot OPENS THE MIC (micGain = "
				++ state.get(\micGain) ++ ") — CQ.set(\\micGain, 0) to mute");
		};
		matrix.load(data[\matrix], origin, committed, operation);
		data[\spesal] !? { |st| setSpesal.value(st) };
	}

	// toggles + enums + spesal only — the morph midpoint switch
	prApplyDiscrete { |data|
		var wanted = Event.new;
		data[\params].keysValuesDo { |k, v|
			if(#[toggle, enum].includes(CQSpec.kind(k)) and: { k != \monitorMute }) {
				wanted[k] = v;
			};
		};
		state.setAll(wanted);
		data[\spesal] !? { |st| setSpesal.value(st) };
	}

	*prWarn { |msg| if(postWarnings) { ("CQSnapshots: " ++ msg).warn } }
}
