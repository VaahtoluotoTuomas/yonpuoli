// CQSpec — the single source of truth for every parameter. Mirrors SPEC.md §5
// exactly: names, ranges, curves, defaults, server-side lags, 16n slots.
// Nothing else in the codebase may contain a literal range or default.
// All entry points (code, MIDI, GUI, snapshots, config, shake) clamp through here.
//
// SPEC curve 'exp' over a range whose minimum is 0 is implemented as CurveWarp(4)
// — exponential feel that still reaches 0 (ExponentialWarp cannot contain 0).

CQSpec {
	classvar all;              // IdentityDictionary: paramName -> Event
	classvar slots16n;         // Array[16] of Arrays of paramNames (SPEC §6.1)
	classvar <>postWarnings = true;

	*specs {
		if(all.isNil) { this.prBuild };
		^all
	}

	*map16n {
		if(all.isNil) { this.prBuild };
		^slots16n
	}

	*names {
		^this.specs.keys.asArray.sort { |a, b| a.asString <= b.asString }
	}

	*includes { |name| ^this.specs.includesKey(name) }

	*at { |name| ^this.specs[name] }

	// A semantic control may draw the same normalized regions its hand map
	// actually uses. Return a copy: CQSpec owns the table and no view may
	// mutate range truth while decorating itself.
	*mapBreakpoints { |name|
		var e = this.specs[name], points;
		if(e.isNil) { ^nil };
		points = e[\breakpoints];
		if(points.isNil) { ^nil };
		^points.collect { |point| point.copy }
	}

	// Named landmarks belong to the parameter just like its range and curve.
	// Views receive only the mapped normalized position: they never assume that
	// unity is halfway along a rail (which is false for a curved deck level).
	*referenceNorm { |name, reference|
		var e = this.specs[name], references, value;
		if(e.isNil) { ^nil };
		references = e[\references];
		if(references.isNil) { ^nil };
		value = references[reference];
		if(value.isNil) { ^nil };
		^this.unmap(name, value)
	}

	*default { |name|
		var e = this.specs[name];
		^if(e.isNil) { nil } { e[\default] }
	}

	*lag { |name|
		var e = this.specs[name];
		^if(e.isNil) { 0 } { e[\lag] }
	}

	*kind { |name|
		var e = this.specs[name];
		^if(e.isNil) { nil } { e[\kind] }
	}

	// Public enum vocabulary for controls that step rather than choose from a
	// menu. Callers never keep their own symbol list: CQSpec remains the one
	// source for both order and membership.
	*enumValues { |name|
		var e = this.specs[name];
		if(e.isNil or: { e[\kind] != \enum }) { ^nil };
		^e[\values]
	}

	// Where a parameter's zero sits on its own normalized 0..1 rail, or nil for
	// a range that never crosses zero. A drawn control asks this instead of
	// comparing literal bounds at its call site: bipolarity is a property of the
	// parameter, so it is answered here like every other range fact. Used by the
	// Workshop ledger row, whose bipolar fill grows away from a visible centre
	// tick (GUI_SPEC §16.7).
	*centerNorm { |name|
		var e = this.specs[name];
		if(e.isNil or: { #[num, int].includes(e[\kind]).not }) { ^nil };
		if(e[\spec].minval >= 0 or: { e[\spec].maxval <= 0 }) { ^nil };
		^this.unmap(name, 0)
	}

	// SPEC §5.3: the Coco clock is clamped a second time AFTER speed modulation,
	// which is what lets a banana into `_spaf` push the clock an octave below the
	// knob's own floor (the "ground jack" trick). It is a range fact of the Speed
	// parameter, so it lives here rather than as a literal inside \cqCoco — the
	// SynthDef, the tests and the spec then cannot drift apart. The knob floor
	// itself is the minval of `coco1Speed`/`coco2Speed`.
	*speedClampFloor { ^750 }

	// the funnel: every write path passes its value through here (SPEC §8.3)
	*clamp { |name, value|
		var e = this.specs[name];
		if(e.isNil) {
			this.prWarn("unknown parameter '%'".format(name));
			^nil
		};
		^switch(e[\kind],
			\num,    { this.prClampNum(e, value, name) },
			\int,    { this.prClampNum(e, value, name).round.asInteger },
			\toggle, { this.prClampToggle(e, value, name) },
			\enum,   { this.prClampEnum(e, value, name) }
		)
	}

	// map a normalized 0..1 control (fader, GUI knob) onto a parameter value
	*map { |name, norm|
		var e = this.specs[name];
		if(e.isNil) { ^nil };
		norm = (this.prNumOrNil(norm) ? 0).clip(0, 1);
		^switch(e[\kind],
			\num,    { this.prMapNum(e, norm) },
			\int,    { e[\spec].map(norm).round.asInteger },
			\toggle, { if(norm > 0.5) { 1 } { 0 } },
			\enum,   { e[\values].at((norm * (e[\values].size - 1)).round.asInteger) }
		)
	}

	*unmap { |name, value|
		var e = this.specs[name];
		if(e.isNil) { ^nil };
		value = this.clamp(name, value);
		^switch(e[\kind],
			\num,    { this.prUnmapNum(e, value) },
			\int,    { e[\spec].unmap(value) },
			\toggle, { value },
			\enum,   { e[\values].indexOf(value) / (e[\values].size - 1) }
		)
	}

	// ---- private ----

	*prWarn { |msg|
		if(postWarnings) { ("CQSpec: " ++ msg).warn };
	}

	// numbers and booleans pass (as Float), everything else (incl. NaN/inf) -> nil
	*prNumOrNil { |value|
		if(value.isKindOf(Boolean)) { ^value.binaryValue.asFloat };
		if(value.isNumber.not) { ^nil };
		value = value.asFloat;
		if(value.isNaN or: { value.abs == inf }) { ^nil };
		^value
	}

	*prClampNum { |e, value, name|
		var v = this.prNumOrNil(value);
		if(v.isNil) {
			this.prWarn("bad value % for '%', using default %"
				.format(value, name, e[\default]));
			^e[\default]
		};
		^e[\spec].constrain(v)
	}

	*prClampToggle { |e, value, name|
		var v = this.prNumOrNil(value);
		if(v.isNil) {
			this.prWarn("bad value % for '%', using default %"
				.format(value, name, e[\default]));
			^e[\default]
		};
		^v.clip(0, 1).round.asInteger
	}

	*prClampEnum { |e, value, name|
		if(value.isString) { value = value.asSymbol };
		if(e[\values].includes(value)) { ^value };
		this.prWarn("bad value % for '%', using default %"
			.format(value, name, e[\default]));
		^e[\default]
	}

	// An optional monotonic breakpoint table keeps a hand mapping honest and
	// exactly invertible while ControlSpec remains the owner of range/clamping.
	// Each point is [normalizedPosition, parameterValue]; the table itself lives
	// only in this registry, never in a GUI or MIDI call site.
	*prMapNum { |e, norm|
		var points = e[\breakpoints], handPower = e[\handPower], i;
		var signed, midpoint, halfRange;
		// A symmetric power taper widens the hand's country around the midpoint
		// without changing the stored value, endpoints or DSP range. Unlike a
		// GUI-local sensitivity tweak this remains one exact, invertible law for
		// Night, Workshop and MIDI.
		if(handPower.notNil) {
			signed = (norm * 2) - 1;
			midpoint = (e[\spec].minval + e[\spec].maxval) * 0.5;
			halfRange = (e[\spec].maxval - e[\spec].minval) * 0.5;
			^midpoint + (if(signed < 0) { -1 } { 1 }
				* (signed.abs ** handPower) * halfRange)
		};
		if(points.isNil) { ^e[\spec].map(norm) };
		i = (0 .. (points.size - 2)).detect { |j| norm <= points[j + 1][0] };
		^norm.linlin(
			points[i][0], points[i + 1][0],
			points[i][1], points[i + 1][1])
	}

	*prUnmapNum { |e, value|
		var points = e[\breakpoints], handPower = e[\handPower], i;
		var signed, midpoint, halfRange;
		if(handPower.notNil) {
			midpoint = (e[\spec].minval + e[\spec].maxval) * 0.5;
			halfRange = (e[\spec].maxval - e[\spec].minval) * 0.5;
			signed = ((value - midpoint) / halfRange).clip(-1, 1);
			^((if(signed < 0) { -1 } { 1 }
				* (signed.abs ** (1 / handPower))) + 1) * 0.5
		};
		if(points.isNil) { ^e[\spec].unmap(value) };
		i = (0 .. (points.size - 2)).detect { |j| value <= points[j + 1][1] };
		^value.linlin(
			points[i][1], points[i + 1][1],
			points[i][0], points[i + 1][0])
	}

	*prNum { |name, lo, hi, warp, default, lag = 0.03, breakpoints, references,
		handPower|
		all[name] = (kind: \num, spec: ControlSpec(lo, hi, warp, 0, default),
			default: default, lag: lag, breakpoints: breakpoints,
			references: references, handPower: handPower);
	}

	*prInt { |name, lo, hi, default|
		all[name] = (kind: \int, spec: ControlSpec(lo, hi, \lin, 1, default),
			default: default, lag: 0);
	}

	*prToggle { |name, default, lag = 0|
		all[name] = (kind: \toggle, default: default, lag: lag);
	}

	*prEnum { |name, values, default|
		all[name] = (kind: \enum, values: values, default: default, lag: 0);
	}

	*prBuild {
		all = IdentityDictionary.new;

		// SPEC §5.1 sources
		this.prNum(\liveGain, 0, 4, 4, 1.0);
		this.prNum(\micGain, 0, 8, 4, 0.0);       // default 0 = muted (SPEC §7 mic safety)
		this.prNum(\deckALevel, 0, 2, 4, 1.0, 0.03, nil, (unity: 1.0));
		this.prNum(\deckBLevel, 0, 2, 4, 1.0, 0.03, nil, (unity: 1.0));
		this.prNum(\deckARate, -2.0, 2.0, \lin, 1.0);
		this.prNum(\deckBRate, -2.0, 2.0, \lin, 1.0);
		this.prToggle(\deckALoop, 1);
		this.prToggle(\deckBLoop, 1);

		// SPEC §5.2 preamps
		this.prEnum(\preASrc, #[liveL, liveR, mic, deckA, deckB, none], \liveL);
		this.prEnum(\preBSrc, #[liveL, liveR, mic, deckA, deckB, none], \liveR);
		this.prNum(\preADrive, 1, 40, \exp, 2.0);
		this.prNum(\preBDrive, 1, 40, \exp, 2.0);
		this.prNum(\preABias, 0, 0.6, \lin, 0.15);
		this.prNum(\preBBias, 0, 0.6, \lin, 0.15);

		// SPEC §5.3 cocos (identical pair, only pan default differs)
		[1, 2].do { |n|
			var p = { |suffix| ("coco" ++ n ++ suffix).asSymbol };
			this.prNum(p.value("In"), 0, 2, \lin, 0.8, 0.03, nil, (unity: 1.0));
			this.prNum(p.value("Fb"), 0, 1.25, \lin, 0.35, 0.03,
				[[0.0, 0.0], [0.5, 0.85], [0.9, 1.10], [1.0, 1.25]]);
			// 13.2: the floor is 1500 Hz, not the shy 3000 — five octaves of clock
			// under the hand, and the darkest end is playable rather than a wall
			this.prNum(p.value("Speed"), 1500, 48000, \exp, 24000);
			// Signed-square hand taper: quarter travel from zero is only 0.25
			// octave; half travel is 1 octave; the unchanged endpoints are +/-4.
			// Named landmarks also let Night draw the calm water geometrically,
			// without assigning a third semantic colour to pitch depth.
			this.prNum(p.value("Spaf"), -4.0, 4.0, \lin, 0, 0.03, nil,
				(fineNeg: -0.25, finePos: 0.25,
					octaveNeg: -1.0, octavePos: 1.0), 2);
			this.prEnum(p.value("DolbyIn"), #[off, gate, duck], \off);
			this.prEnum(p.value("DolbyFb"), #[off, gate, duck], \off);
			this.prToggle(p.value("Hold"), 1);     // power-on mirrors hardware: loop-locked
			this.prToggle(p.value("Flip"), 0);
			this.prInt(p.value("Bits"), 3, 12, 8);
			this.prToggle(p.value("Wrap"), 0);
			this.prEnum(p.value("Interp"), #[zoh, smooth], \zoh);
			this.prNum(p.value("Dust"), 0, 0.5, 4, 0.02);
			this.prNum(p.value("Organ"), 0, 1, \lin, 0);
			this.prNum(p.value("Level"), 0, 1.5, 4, 0.8, 0.03,
				nil, (unity: 1.0));
			this.prNum(p.value("Pan"), -1, 1, \lin, if(n == 1) { -0.3 } { 0.3 });
			this.prNum(p.value("SlotPre"), 0, 1.5, \lin, 1.0);
			this.prNum(p.value("SlotDeck"), 0, 1.5, \lin, 1.0);
			this.prNum(p.value("SlotQ"), 0, 1.5, \lin, 0.0);
			this.prNum(p.value("SlotX"), 0, 1.5, \lin, 0.0);
		};

		// SPEC §5.4 quantussy
		this.prNum(\qFreq, 0.05, 500, \exp, 4.0);
		// Curve +2 keeps the stepped drunken-wander country broad under the hand:
		// noon maps to ~0.269 and 3/4 to ~0.545; DSP value semantics stay linear.
		this.prNum(\qChaos, 0, 1, 2, 0.15);
		this.prNum(\qShLag, 0, 0.2, 4, 0.004);
		[\infra, \infra, \balcium, \infra, \audio].do { |range, i|
			this.prEnum(("c" ++ (i + 1) ++ "Range").asSymbol, #[infra, balcium, audio], range);
		};
		[0.0, 0.23, -0.31, 0.48, -0.17].do { |off, i|
			this.prNum(("c" ++ (i + 1) ++ "Off").asSymbol, -1.0, 1.0, \lin, off);
		};
		[0, 0, 0, 0, 0.5].do { |send, i|
			this.prNum(("c" ++ (i + 1) ++ "Send").asSymbol, 0, 1, \lin, send);
		};

		// SPEC §5.5 matrix macro
		this.prNum(\patchDepth, 0, 1, \lin, 1.0, 0.05);

		// SPEC §5.6 master
		this.prNum(\masterDry, 0, 1, \lin, 0.0);
		this.prNum(\masterVol, -60, 0, \lin, -12, 0.1);   // lin dB, lag 100 ms
		this.prToggle(\monitorMute, 0, 0.03);             // 30 ms panic fade

		// SPEC §6.1 — 16n default map, fader index 0..15 (cc = ccBase + index).
		// Two identical five-fader Coco strips in signal order (in, clock, clock
		// modulation, feedback, output level), then a six-fader shared section.
		// Level is last because it is the strip's output stage, like a mixer's
		// channel fader; CQMidi16nTest pins both strips against that law rather
		// than against sixteen hand-written rows.
		slots16n = [
			[\coco1In], [\coco1Speed], [\coco1Spaf], [\coco1Fb], [\coco1Level],
			[\coco2In], [\coco2Speed], [\coco2Spaf], [\coco2Fb], [\coco2Level],
			[\qFreq], [\qChaos],
			[\preADrive, \preBDrive],
			// the ring's audio gate: qMix has exactly one writer (castle sends)
			// and exactly one reader (this slot gain), so without a hand here
			// F11/F12 shape a voice that never reaches the output (SPEC §5.4)
			[\coco1SlotQ, \coco2SlotQ],
			[\patchDepth], [\masterVol]
		];
	}
}
