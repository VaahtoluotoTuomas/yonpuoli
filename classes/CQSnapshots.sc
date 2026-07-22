// CQSnapshots — save/recall/morph (ARCHITECTURE §5/§7, SPEC §6.3/§7). File-backed
// memory: one plain-text .cqsnap.scd per snapshot in the presets folder. Every
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
	var state, matrix, <dir, getSpesal, setSpesal;
	var morphRoutine;
	var <morphProgress = 0.0, <morphFrom, <morphTo;

	*new { |state, matrix, dir, getSpesal, setSpesal|
		^super.newCopyArgs(state, matrix, dir, getSpesal, setSpesal)
	}

	save { |name, includeMic = false, note = ""|
		var safe = this.prSafeName(name);
		var path, params, data, d;
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
		File.use(path, "w", { |f| f.write(data.asCompileString) });
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
	morph { |from, to, dur = 10|
		var a = this.read(from), b = this.read(to);
		var contNames, aNorm, bNorm, cells, nSteps;
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
		// unknown src/dest names are dropped here (not each tick) — patch() would
		// warn 30×/s otherwise; prApply's matrix.load still warns once at the end
		cells = Dictionary.new;
		a[\matrix].do { |t|
			if(CQMatrix.srcIndex(t[0]).notNil and: { CQMatrix.destIndex(t[1]).notNil }) {
				cells[[t[0], t[1]]] = [CQMatrix.clampWeight(t[2]), 0.0];
			};
		};
		b[\matrix].do { |t|
			var pair;
			if(CQMatrix.srcIndex(t[0]).notNil and: { CQMatrix.destIndex(t[1]).notNil }) {
				pair = cells[[t[0], t[1]]] ?? { [0.0, 0.0] };
				pair[1] = CQMatrix.clampWeight(t[2]);
				cells[[t[0], t[1]]] = pair;
			};
		};
		nSteps = max(1, (dur * 30).round.asInteger);
		morphRoutine = Routine {
			var switched = false;
			nSteps.do { |i|
				var t = (i + 1) / nSteps;
				morphProgress = t;
				this.changed(\morph, morphProgress, morphFrom, morphTo, \running);
				contNames.do { |n, j|
					state.set(n, CQSpec.map(n, aNorm[j] + ((bNorm[j] - aNorm[j]) * t)));
				};
				cells.keysValuesDo { |key, w|
					matrix.patch(key[0], key[1], w[0] + ((w[1] - w[0]) * t),
						origin: \morph, committed: false, operation: \morphFrame);
				};
				if(switched.not and: { t >= 0.5 }) {
					switched = true;
					this.prApplyDiscrete(b);
				};
				(1/30).wait;
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
		^(dir +/+ "*.cqsnap.scd").pathMatch
			.collect { |p| p.basename.replace(".cqsnap.scd", "") }.sort
	}

	// load + structurally validate one snapshot file -> data Event or nil
	read { |name|
		var safe = this.prSafeName(name);
		var path, data;
		if(safe.isNil) { ^nil };
		path = this.prPath(safe);
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

	prPath { |safe| ^dir +/+ (safe ++ ".cqsnap.scd") }

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
	prApply { |data, origin = \snapshot, committed = true, operation = \recall|
		data[\params].keysValuesDo { |k, v|
			case
			{ k == \monitorMute } {
				this.class.prWarn("snapshot monitorMute ignored (panic is not memory)");
			}
			{ CQSpec.includes(k).not } {
				this.class.prWarn("unknown param '%' skipped".format(k));
			}
			{ state.set(k, v) };
		};
		if(data[\params].includesKey(\micGain) and: { (state.get(\micGain) ? 0) > 0 }) {
			this.class.prWarn("this snapshot OPENS THE MIC (micGain = "
				++ state.get(\micGain) ++ ") — CQ.set(\\micGain, 0) to mute");
		};
		matrix.load(data[\matrix], origin, committed, operation);
		data[\spesal] !? { |st| setSpesal.value(st) };
	}

	// toggles + enums + spesal only — the morph midpoint switch
	prApplyDiscrete { |data|
		data[\params].keysValuesDo { |k, v|
			if(#[toggle, enum].includes(CQSpec.kind(k)) and: { k != \monitorMute }) {
				state.set(k, v);
			};
		};
		data[\spesal] !? { |st| setSpesal.value(st) };
	}

	*prWarn { |msg| if(postWarnings) { ("CQSnapshots: " ++ msg).warn } }
}
