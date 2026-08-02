// CQInk — the single Ink & Phosphor vector kit (GUI_SPEC §16.2/§16.11).
// All palette, stroke, seeded-wobble and primitive drawing decisions live here;
// views provide semantic state and geometry only.

CQInk {
	classvar palettes, <glyphNames;

	*initClass {
		palettes = IdentityDictionary.new;
		palettes[\day] = (
			clear: Color.new(0, 0, 0, 0),
			ground: Color.new255(244, 237, 224), recess: Color.new255(231, 222, 203),
			line: Color.new255(42, 37, 30), whisper: Color.new255(98, 89, 77),
			spark: Color.new255(42, 37, 30), tri: Color.new255(217, 131, 36),
			cas: Color.new255(121, 114, 103), fm: Color.new255(61, 111, 165),
			input: Color.new255(79, 138, 80), spaf: Color.new255(124, 88, 168),
			organ: Color.new255(212, 175, 55), pre: Color.new255(251, 247, 237),
			verso: Color.new255(227, 154, 45), inverso: Color.new255(74, 124, 199),
			danger: Color.new255(196, 61, 43), warning: Color.new255(168, 123, 47),
			// On paper a lit lamp is MORE ink, not more light: the earlier pale
			// amber sat at 1.2:1 against cream and simply vanished (user review
			// 2026-07-21). This deep amber reads 3.15:1 — §16.2's large-mark
			// floor — while the night lamp stays luminous.
			lamp: Color.new255(181, 122, 22),
			// PLAY-6.1: the instrument's own internal audio streams run under a
			// single low-chroma ink, so the drone topology separates from the
			// patch cables by DEPTH instead of by a sixth bright family. Never
			// a line's own colour — always the water beneath one.
			undertow: Color.new255(82, 111, 113)
		);
		palettes[\night] = (
			clear: Color.new(0, 0, 0, 0),
			ground: Color.new255(18, 20, 28), recess: Color.new255(26, 29, 38),
			line: Color.new255(169, 174, 184), whisper: Color.new255(122, 128, 140),
			spark: Color.new255(237, 230, 210), tri: Color.new255(255, 177, 61),
			cas: Color.new255(193, 186, 173), fm: Color.new255(100, 167, 255),
			input: Color.new255(98, 208, 106), spaf: Color.new255(178, 135, 255),
			organ: Color.new255(255, 222, 89), pre: Color.new255(242, 239, 228),
			verso: Color.new255(255, 174, 66), inverso: Color.new255(98, 169, 255),
			danger: Color.new255(255, 107, 84), warning: Color.new255(255, 217, 138),
			lamp: Color.new255(255, 233, 168),
			undertow: Color.new255(102, 144, 151)
		);
		palettes.values.do { |palette|
			palette[\hold] = palette[\danger];
			palette[\warn] = palette[\danger];
		};
		glyphNames = #[wedge, flip, spark, wave, tickRing, pendulum,
			hold, skip, spesal, panic, rec, drawer, collapse, expand, day, night];
	}

	*palette { |skin = \day| ^palettes[skin] ?? { palettes[\day] } }
	*strokes { ^(hairline: 1.0, normal: 1.5, strong: 2.0, state: 3.0) }

	// §16.7: a momentary control shows a 120 ms ink flash. One constant keeps
	// every action entrance in step.
	*momentaryFlash { ^0.12 }

	// A Qt text entry cannot be Pen-drawn, so it borrows the Ink hover and
	// selection language through its own Qt palette instead of showing the OS
	// default. Hover is `spark` at the same weight a menu box uses; selection is
	// `spaf`, the weight a list row uses -- never native highlight blue.
	// User review 2026-07-30 (the morph bridge's seconds box).
	*textEntryInk { |skin = \day, hovered = false|
		var p = this.palette(skin);
		^(
			text: p[\line],
			background: if(hovered) { p[\ground].blend(p[\spark], 0.10) } {
				p[\ground]
			},
			highlight: p[\ground].blend(p[\spaf], 0.34),
			highlightText: p[\line]
		)
	}

	*applyTextEntryInk { |view, skin = \day, hovered = false|
		var ink = this.textEntryInk(skin, hovered);
		view.palette = view.palette
			.base_(ink[\background]).baseText_(ink[\text])
			.highlight_(ink[\highlight]).highlightText_(ink[\highlightText]);
		if(view.isKindOf(NumberBox)) {
			view.normalColor_(ink[\text]);
			// Qt's default typing colour is red, which reads as an error. It is
			// not even a pending state any more: a box's live text is what its
			// action reads, so what you see is what happens.
			view.typingColor_(ink[\text]);
		};
		^view
	}
	*metrics {
		^(grid: 8, labelSize: 10, numberSize: 10, noticeSize: 11,
			rangeSize: 9, knobDiameter: 26, featuredKnobDiameter: 34,
			primaryKnobDiameter: 44, buttonRadius: 3,
			socketSpacing: 24)
	}
	*knobDiameter { |role = \standard|
		var m = this.metrics;
		^switch(role,
			\featured, { m[\featuredKnobDiameter] },
			\primary, { m[\primaryKnobDiameter] },
			{ m[\knobDiameter] }
		)
	}

	// A knob's box must hold its label band, the circle, the value arc's 3 px
	// and the value readout. The reviewed Day figure is `diameter + 30` at
	// 10 px type; a larger label needs the difference twice over, above and
	// below. Day and Night pass no size and are pixel-identical to before.
	*knobBoxHeight { |diameter, labelSize|
		var grown = if(labelSize.isNil) { 0 } {
			4 * (labelSize - this.metrics[\labelSize])
		};
		^diameter + 30 + grown
	}

	// The face is always the kit's (§16.4: two faces, generic constructors).
	// `size` overrides only the *size*, for the Drawer's two reviewed steps
	// (CQGuiScale) — the Night landscape keeps the fixed px sizes §16.10
	// requires and simply never passes one.
	*font { |role = \label, size|
		var m = this.metrics;
		^switch(role,
			\number, { Font.monospace(size ? m[\numberSize]) },
			\notice, { Font.sansSerif(size ? m[\noticeSize]) },
			\spellbook, { Font.monospace(size ? m[\noticeSize]) },
			\range, { Font.sansSerif(size ? m[\rangeSize]) },
			{ Font.sansSerif(size ? m[\labelSize]) }
		)
	}

	*wobble { |points, seed = 0, amount = 1.0|
		var saved = thisThread.randData, result;
		thisThread.randSeed = seed.asInteger;
		result = points.collect { |point|
			Point(point.x + amount.rand2, point.y + amount.rand2)
		};
		thisThread.randData = saved;
		^result
	}

	*glyphPath { |name|
		^switch(name,
			\wedge, { [[1, 1], [7, 4], [1, 7]] },
			\hold, { [[1, 1], [1, 7], nil, [7, 1], [7, 7]] },
			\flip, { [[1, 1], [4, 4], [1, 7], nil, [7, 1], [4, 4], [7, 7]] },
			\spark, { [[4, 0], [4, 8], nil, [1, 4], [7, 4]] },
			\skip, { this.glyphPath(\spark) },
			\spesal, { this.glyphPath(\spark) },
			\wave, { [[0, 4], [2, 1], [4, 4], [6, 7], [8, 4]] },
			\tickRing, { [[4, 0], [4, 2], nil, [8, 4], [6, 4], nil,
				[4, 8], [4, 6], nil, [0, 4], [2, 4]] },
			\pendulum, { [[3, 1], [5, 7], nil, [2, 6], [5, 7], [7, 5]] },
			\panic, { [[4, 0], [4, 5], nil, [4, 7], [4, 8]] },
			\rec, { [[4, 1], [6, 2], [7, 4], [6, 6], [4, 7], [2, 6], [1, 4], [2, 2], [4, 1]] },
			\drawer, { [[1, 2], [7, 2], [7, 6], [1, 6], [1, 2], nil, [3, 4], [5, 4]] },
			\collapse, { [[1, 3], [4, 6], [7, 3]] },
			\expand, { [[1, 6], [4, 3], [7, 6]] },
			\day, { [[4, 1], [4, 7], nil, [1, 4], [7, 4], nil, [2, 2], [6, 6], nil, [6, 2], [2, 6]] },
			\night, { [[6, 1], [3, 1], [1, 3], [1, 5], [3, 7], [6, 7], [4, 6], [3, 4], [4, 2], [6, 1]] },
			{ [] }
		)
	}

	*drawGlyph { |name, rect, color|
		var path = this.glyphPath(name), drawing = false;
		Pen.color = color;
		Pen.width = this.strokes[\normal];
		path.do { |point|
			if(point.isNil) {
				drawing = false;
			} {
				point = Point(rect.left + ((point[0] / 8) * rect.width),
					rect.top + ((point[1] / 8) * rect.height));
				if(drawing) { Pen.lineTo(point) } { Pen.moveTo(point); drawing = true };
			};
		};
		Pen.stroke;
	}

	*drawJack { |center, family, skin = \day, state = \idle, aura = 0,
		sizeScale = 1|
		var palette = this.palette(skin), radius;
		radius = switch(state, \inHand, { 5 }, \hover, { 4.5 }, { 4 })
			* sizeScale;
		if(aura > 0) {
			Pen.fillColor = palette[family].copy.alpha_((aura * 0.22).clip(0, 0.22));
			Pen.addOval(Rect.aboutPoint(center,
				(8 + (aura * 5)) * sizeScale,
				(8 + (aura * 5)) * sizeScale));
			Pen.fill;
		};
		if(state == \hover) {
			Pen.fillColor = palette[family].copy.alpha_(0.10);
			Pen.addOval(Rect.aboutPoint(center, 7 * sizeScale, 7 * sizeScale));
			Pen.fill;
		};
		Pen.fillColor = palette[family];
		Pen.addOval(Rect.aboutPoint(center, radius, radius));
		Pen.fill;
		Pen.strokeColor = palette[\line];
		Pen.width = if(state == \hover) { this.strokes[\normal] } { this.strokes[\hairline] };
		Pen.addOval(Rect.aboutPoint(center, radius + 1, radius + 1));
		Pen.stroke;
	}

	*drawSocket { |center, family, skin = \day, state = \idle, candidateWeight,
		sizeScale = 1|
		var palette = this.palette(skin), radius, halo;
		radius = if(state == \candidate) { 7.2 } { 6 } * sizeScale;
		// Static invitation while a cable is in hand. It never pulses: Night
		// reserves motion for telemetry, but every legal landing place should
		// become readable before the hand has to guess.
		if(state == \available) {
			Pen.fillColor = palette[family].copy.alpha_(0.10);
			Pen.addOval(Rect.aboutPoint(center, 10 * sizeScale, 10 * sizeScale));
			Pen.fill;
			Pen.strokeColor = palette[family].copy.alpha_(0.48);
			Pen.width = this.strokes[\hairline];
			Pen.addOval(Rect.aboutPoint(center, 10 * sizeScale, 10 * sizeScale));
			Pen.stroke;
		};
		if(state == \candidate) {
			halo = if((candidateWeight ? 1) < 0) {
				palette[\inverso]
			} {
				palette[\verso]
			};
			Pen.fillColor = halo.copy.alpha_(0.18);
			Pen.addOval(Rect.aboutPoint(center, 11 * sizeScale, 11 * sizeScale));
			Pen.fill;
		};
		if(state == \hover) {
			Pen.fillColor = palette[family].copy.alpha_(0.09);
			Pen.addOval(Rect.aboutPoint(center, 9 * sizeScale, 9 * sizeScale));
			Pen.fill;
		};
		Pen.strokeColor = palette[family];
		Pen.width = if(#[occupied, hover].includes(state)) {
			this.strokes[\strong]
		} {
			this.strokes[\normal]
		};
		Pen.addOval(Rect.aboutPoint(center, radius, radius));
		Pen.stroke;
	}

	*drawPlug { |center, weight, skin = \day, lifted = 0|
		var palette = this.palette(skin), radius = 3 + (weight.abs * 2);
		var color = if(weight < 0) { palette[\inverso] } { palette[\verso] };
		center = center - Point(0, lifted);
		Pen.fillColor = color;
		Pen.addOval(Rect.aboutPoint(center, radius, radius));
		Pen.fill;
	}

	*drawLamp { |center, charge = 0, on = false, skin = \day, sizeScale = 1, fired|
		var palette = this.palette(skin), fire, fill;
		fire = if(fired.isNil) { if(on) { 1 } { 0 } } {
			fired.asFloat.clip(0, 1)
		};
		fill = if(fired.isNil and: { on }) { 1 } { (charge / 0.5).clip(0, 1) };
		// An idle charge lamp is a recessed bead, not another hollow patch socket.
		Pen.fillColor = palette[\recess];
		Pen.addOval(Rect.aboutPoint(center, 4 * sizeScale, 4 * sizeScale));
		Pen.fill;
		// Charge remains amber inside the bead. An exact post-Schmitt event gets
		// a pale diamond flare as a second, form-first fact: a steady diamond is
		// an active gate; a trigger leaves the server-held flare to decay.
		if(fired.isNil and: { on }) {
			Pen.fillColor = palette[\lamp].copy.alpha_(0.28);
			Pen.addOval(Rect.aboutPoint(center, 7 * sizeScale, 7 * sizeScale));
			Pen.fill;
		};
		if(fired.notNil and: { fire > 0.01 }) {
			Pen.fillColor = palette[\spark].copy.alpha_(0.12 * fire);
			Pen.addOval(Rect.aboutPoint(center, 8 * sizeScale, 8 * sizeScale));
			Pen.fill;
			Pen.strokeColor = palette[\spark].copy.alpha_(0.35 + (0.6 * fire));
			Pen.width = this.strokes[\normal];
			Pen.moveTo(center + Point(0, -7 * sizeScale));
			Pen.lineTo(center + Point(7 * sizeScale, 0));
			Pen.lineTo(center + Point(0, 7 * sizeScale));
			Pen.lineTo(center + Point(-7 * sizeScale, 0));
			Pen.lineTo(center + Point(0, -7 * sizeScale));
			Pen.stroke;
		};
		Pen.fillColor = palette[\lamp].copy.alpha_(fill);
		Pen.addOval(Rect.aboutPoint(center, 4 * sizeScale, 4 * sizeScale));
		Pen.fill;
		Pen.strokeColor = palette[\whisper];
		Pen.width = this.strokes[\hairline];
		Pen.addOval(Rect.aboutPoint(center, 4 * sizeScale, 4 * sizeScale));
		Pen.stroke;
	}

	// `cached` takes the polyline already computed for this exact cable (the overlay
	// keeps one per planted cable), so an ambient repaint re-strokes ink instead of
	// re-deriving bezier geometry. The inverso ticks all share one colour and width,
	// so they are built as one path and stroked once: identical pixels, but one Qt
	// call instead of one per tick — at ~30 ticks per cable that dominated the frame.
	*drawCable { |start, end, weight, skin = \day, alpha = 1, cached|
		var palette = this.palette(skin), controls = this.cableControls(start, end);
		var color = if(weight < 0) { palette[\inverso] } { palette[\verso] };
		var points, distance, stepCount;
		Pen.strokeColor = color.copy.alpha_(alpha.clip(0, 1));
		Pen.width = 1.5 + (weight.abs.clip(0, 1) * 1.5);
		Pen.moveTo(start);
		Pen.curveTo(end, controls[0], controls[1]);
		Pen.stroke;
		if(weight < 0) {
			points = cached ?? {
				distance = sqrt(((end.x - start.x).squared) + ((end.y - start.y).squared));
				stepCount = max(8, (distance / 12).ceil.asInteger);
				this.cablePoints(start, end, stepCount)
			};
			Pen.strokeColor = color.copy.alpha_(alpha.clip(0, 1));
			Pen.width = this.strokes[\hairline];
			points.do { |point, i|
				var tangent, magnitude, normal;
				if(i.odd and: { i > 0 } and: { i < (points.size - 1) }) {
					tangent = points[i + 1] - points[i - 1];
					magnitude = sqrt(tangent.x.squared + tangent.y.squared).max(0.001);
					normal = Point(tangent.y.neg / magnitude,
						tangent.x / magnitude) * 3;
					Pen.line(point - normal, point + normal);
				};
			};
			Pen.stroke;
		};
	}

	*drawCableGlow { |start, end, skin = \day|
		var palette = this.palette(skin), controls = this.cableControls(start, end);
		Pen.strokeColor = palette[\danger].copy.alpha_(0.34);
		Pen.width = 6;
		Pen.moveTo(start);
		Pen.curveTo(end, controls[0], controls[1]);
		Pen.stroke;
	}

	// Static, hue-independent targeting cue. The animated/translucent aura is
	// reserved for signal telemetry; mouse focus gets a crisp ink under-stroke.
	*drawCableFocus { |start, end, skin = \day|
		var palette = this.palette(skin), controls = this.cableControls(start, end);
		Pen.strokeColor = palette[\line].copy.alpha_(0.22);
		Pen.width = 6;
		Pen.moveTo(start);
		Pen.curveTo(end, controls[0], controls[1]);
		Pen.stroke;
	}

	// A focused cable states its weight instead of asking the eye to estimate a
	// 1.5 px width interval. The orbit's travelled fraction is |weight|, while
	// its direction and hue repeat polarity. This data helper keeps the visual
	// promise unit-testable without turning the instrument into a numeric panel.
	*cableWeightReadout { |weight = 0|
		var clipped = weight.asFloat.clip(-1, 1);
		var direction = if(clipped < 0) { -1 } { if(clipped > 0) { 1 } { 0 } };
		^(magnitude: clipped.abs, direction: direction,
			family: if(direction < 0) { \inverso } { \verso })
	}

	// Hover-only midpoint moon. It is deliberately drawn after every planted
	// cable, so dense routes cannot bury the readout belonging to the pointer.
	*drawCableWeightMoon { |points, weight, skin = \day|
		var palette = this.palette(skin), data, center;
		var radius = 6.5, origin = -0.5pi, arcSweep, arcStart, pearlAngle, pearl;
		if(points.isNil or: { points.size < 1 }) { ^this };
		data = this.cableWeightReadout(weight);
		center = points[points.size div: 2];

		Pen.fillColor = palette[\recess].copy.alpha_(0.98);
		Pen.addOval(Rect.aboutPoint(center, radius, radius));
		Pen.fill;
		Pen.strokeColor = palette[\whisper].copy.alpha_(0.82);
		Pen.width = this.strokes[\hairline];
		Pen.addOval(Rect.aboutPoint(center, radius, radius));
		Pen.stroke;

		arcSweep = 2pi * data[\magnitude];
		if(arcSweep > 0) {
			arcStart = if(data[\direction] < 0) { origin - arcSweep } { origin };
			Pen.strokeColor = palette[data[\family]];
			Pen.width = this.strokes[\strong];
			Pen.addArc(center, radius - 1.2, arcStart, arcSweep);
			Pen.stroke;
			pearlAngle = if(data[\direction] < 0) { arcStart } {
				origin + arcSweep
			};
			pearl = center + (Point(cos(pearlAngle), sin(pearlAngle)) * (radius - 1.2));
			Pen.fillColor = palette[data[\family]];
			Pen.addOval(Rect.aboutPoint(pearl, 1.5, 1.5));
			Pen.fill;
		};
	}

	*drawLasso { |points, skin = \day|
		var palette = this.palette(skin);
		if(points.isNil or: { points.size < 2 }) { ^this };
		Pen.strokeColor = palette[\danger].copy.alpha_(0.6);
		Pen.width = this.strokes[\hairline];
		Pen.moveTo(points.first);
		points.copyToEnd(1).do { |point| Pen.lineTo(point) };
		Pen.lineTo(points.first);
		Pen.stroke;
	}

	*cableControls { |start, end|
		var sag = max(28, (end.x - start.x).abs * 0.20);
		^[start + Point(0, sag), end + Point(0, sag)]
	}

	*cablePoints { |start, end, segments = 24|
		var controls = this.cableControls(start, end), c1 = controls[0], c2 = controls[1];
		segments = segments.asInteger.max(2);
		^(0..segments).collect { |i|
			var t = i / segments, u = 1 - t;
			(start * (u * u * u)) + (c1 * (3 * u * u * t))
				+ (c2 * (3 * u * t * t)) + (end * (t * t * t))
		}
	}

	*drawKnob { |rect, value = 0, label = "", display = "", family,
		skin = \day, active = false, diameter, labelSize|
		var palette = this.palette(skin), m = this.metrics;
		var size = labelSize ? m[\labelSize];
		var radius = (diameter ? m[\knobDiameter]) / 2;
		// The value arc sits 3 px outside the circle and sweeps over the top, so
		// the label needs clearance from the ARC, not from the knob. The default
		// path keeps the reviewed 14 px exactly; a caller that grew the type
		// (the Master strip) gets a band measured from its own size.
		var band = if(labelSize.isNil) { 14 } { size + 8 };
		var center = Point(rect.center.x, rect.top + band + radius);
		var start = 0.75pi, sweep = 1.5pi, angle, pointer, accent;
		value = value.clip(0, 1);
		accent = palette[family] ?? { palette[\whisper] };
		if(active) {
			Pen.strokeColor = accent.copy.alpha_(0.32);
			Pen.width = this.strokes[\hairline];
			Pen.addOval(Rect.aboutPoint(center, radius + 6, radius + 6));
			Pen.stroke;
		};
		Pen.stringCenteredIn(label.asString,
			Rect(rect.left, rect.top, rect.width, size + 2),
			this.font(\label, labelSize), palette[\whisper]);
		Pen.strokeColor = palette[\line];
		Pen.width = this.strokes[\normal];
		Pen.addOval(Rect.aboutPoint(center, radius, radius));
		Pen.stroke;
		Pen.strokeColor = accent;
		Pen.width = this.strokes[\strong];
		Pen.addArc(center, radius + 3, start, sweep * value);
		Pen.stroke;
		angle = start + (sweep * value);
		pointer = center + (Point(cos(angle), sin(angle)) * (radius - 4));
		Pen.strokeColor = if(active) { palette[\line] } { palette[\whisper] };
		Pen.width = this.strokes[\normal];
		Pen.line(center, pointer);
		Pen.stroke;
		Pen.stringCenteredIn(if(skin == \night) { "" } { display.asString },
			Rect(rect.left, rect.bottom - size - 2, rect.width, size + 1),
			this.font(\number, labelSize), palette[\whisper]);
	}

	*drawButton { |rect, label, skin = \day, active = false, pressed = false,
		accent = \line, glyph, surface = \ground, hover = false, labelSize|
		var palette = this.palette(skin), body = rect.insetBy(1, 2), ink = palette[accent];
		var fill = if(active or: { pressed }) { ink } { palette[surface] };
		var text = if(active or: { pressed }) { palette[surface] } { ink };
		Pen.fillColor = fill;
		Pen.addRoundedRect(body, this.metrics[\buttonRadius], this.metrics[\buttonRadius]);
		Pen.fill;
		if(hover and: { active.not } and: { pressed.not }) {
			Pen.fillColor = ink.copy.alpha_(0.07);
			Pen.addRoundedRect(body, this.metrics[\buttonRadius], this.metrics[\buttonRadius]);
			Pen.fill;
		};
		Pen.strokeColor = ink;
		Pen.width = if(hover) { this.strokes[\strong] } { this.strokes[\normal] };
		Pen.addRoundedRect(body, this.metrics[\buttonRadius], this.metrics[\buttonRadius]);
		Pen.stroke;
		glyph !? {
			this.drawGlyph(glyph, Rect(body.left + 6, body.center.y - 4, 8, 8), text);
		};
		Pen.stringCenteredIn(label.asString,
			Rect(body.left + if(glyph.isNil) { 2 } { 14 }, body.top,
				body.width - if(glyph.isNil) { 4 } { 16 }, body.height),
			this.font(\label, labelSize), text);
	}

	*drawSlider { |rect, value = 0, skin = \day, family, hover = false|
		var palette = this.palette(skin), rail, x, accent;
		value = value.clip(0, 1);
		rail = Rect(rect.left + 5, rect.center.y - 1, rect.width - 10, 2);
		x = rail.left + (rail.width * value);
		accent = palette[family] ?? { palette[\whisper] };
		Pen.strokeColor = palette[\whisper];
		Pen.width = this.strokes[\hairline];
		Pen.line(Point(rail.left, rail.center.y), Point(rail.right, rail.center.y));
		Pen.stroke;
		Pen.strokeColor = accent;
		Pen.line(Point(rail.left, rail.center.y), Point(x, rail.center.y));
		Pen.stroke;
		if(hover) {
			Pen.fillColor = accent.copy.alpha_(0.11);
			Pen.addOval(Rect.aboutPoint(Point(x, rail.center.y), 7, 7));
			Pen.fill;
			Pen.strokeColor = accent.copy.alpha_(0.48);
			Pen.width = this.strokes[\hairline];
			Pen.addOval(Rect.aboutPoint(Point(x, rail.center.y), 6, 6));
			Pen.stroke;
		};
		Pen.fillColor = palette[\ground];
		Pen.strokeColor = palette[\line];
		Pen.addOval(Rect.aboutPoint(Point(x, rail.center.y), 4, 4));
		Pen.fillStroke;
	}

	*drawSwitch { |rect, label, options, index = 0, skin = \day, family, hover = false|
		var palette = this.palette(skin), y = rect.top + 22, left = rect.left + 7;
		var width = rect.width - 14, step, accent;
		accent = palette[family] ?? { palette[\line] };
		Pen.stringLeftJustIn(label.asString, Rect(rect.left + 2, rect.top, rect.width - 4, 10),
			this.font(\label), palette[\whisper]);
		step = if(options.size > 1) { width / (options.size - 1) } { 0 };
		Pen.strokeColor = palette[\whisper];
		Pen.width = this.strokes[\hairline];
		Pen.line(Point(left, y), Point(left + width, y));
		Pen.stroke;
		options.do { |option, i|
			var x = left + (step * i);
			Pen.strokeColor = palette[\whisper];
			Pen.line(Point(x, y - 3), Point(x, y + 3));
			Pen.stroke;
			if(i == index) {
				if(hover) {
					Pen.fillColor = accent.copy.alpha_(0.11);
					Pen.addOval(Rect.aboutPoint(Point(x, y), 7, 7));
					Pen.fill;
					Pen.strokeColor = accent.copy.alpha_(0.48);
					Pen.width = this.strokes[\hairline];
					Pen.addOval(Rect.aboutPoint(Point(x, y), 6, 6));
					Pen.stroke;
				};
				Pen.fillColor = accent;
				Pen.addOval(Rect.aboutPoint(Point(x, y), 3, 3));
				Pen.fill;
			};
		};
		Pen.stringRightJustIn((options[index] ? "").asString,
			Rect(rect.left, rect.bottom - 10, rect.width - 2, 9),
			this.font(\range), palette[\line]);
	}

	*drawMeter { |rect, value = 0, skin = \day, danger = false, downward = false|
		var palette = this.palette(skin), body = rect.insetBy(3, 3), fill;
		value = value.clip(0, 1);
		Pen.strokeColor = palette[\line];
		Pen.width = this.strokes[\hairline];
		Pen.addRect(body);
		Pen.stroke;
		fill = if(downward) {
			Rect(body.left + 2, body.top + 2, body.width - 4, (body.height - 4) * value)
		} {
			Rect(body.left + 2, body.bottom - 2 - ((body.height - 4) * value),
				body.width - 4, (body.height - 4) * value)
		};
		Pen.fillColor = if(danger) { palette[\danger] } { palette[\whisper] };
		Pen.fillRect(fill);
	}

	*drawNumber { |rect, label, display, skin = \day, active = false,
		surface = \ground|
		var palette = this.palette(skin), box = Rect(rect.center.x - 17,
			rect.top + 12, 34, 18);
		Pen.stringCenteredIn(label.asString, Rect(rect.left, rect.top, rect.width, 10),
			this.font(\label), palette[\whisper]);
		Pen.fillColor = palette[surface];
		Pen.strokeColor = if(active) { palette[\line] } { palette[\whisper] };
		Pen.width = this.strokes[\hairline];
		Pen.addRoundedRect(box, this.metrics[\buttonRadius], this.metrics[\buttonRadius]);
		Pen.fillStroke;
		Pen.stringCenteredIn(if(skin == \night) { "" } { display.asString }, box,
			this.font(\number), palette[\line]);
	}

	// ---- the Workshop ledger row (GUI_SPEC §16.7, NIGHT_STANDALONE_PLAN §12.1.3)
	// A precision page needs a control the Day kit does not have: label at left,
	// one hairline value rail in the middle, the exact value in mono at right,
	// and never a knob. The rail's geometry is published here so the drawing and
	// the hit test are literally the same rectangle (§4.5: cue and action share
	// one geometry) — no view may compute a second one.

	// `scale` is a CQGuiScale metrics Event; nil means the compact step, so a
	// caller that has no opinion still gets a reviewed geometry.
	*ledgerMetrics { |scale|
		var m = scale ?? { CQGuiScale.metrics(\compact) };
		^(height: m[\rowHeight], inset: m[\contentInset],
			labelWidth: m[\ledgerLabel], valueWidth: m[\ledgerValue],
			gap: m[\ledgerGap],
			// the grab band and the seat bead follow the row, so the whole
			// control grows as one thing rather than as a bigger box holding
			// the same small marks
			grab: (m[\rowHeight] / 6).round.asInteger,
			seatRadius: m[\rowHeight] / 6.8,
			labelSize: m[\label], valueSize: m[\value], seatSize: m[\subgroup])
	}

	*ledgerRailRect { |rect, scale|
		var m = this.ledgerMetrics(scale), body = rect.insetBy(m[\inset], 0);
		var left = body.left + m[\labelWidth];
		var right = body.right - m[\valueWidth] - m[\gap];
		^Rect(left, body.top, (right - left).max(16), body.height)
	}

	// a press is answered only over the rail (plus a small grab band at each
	// end): a mis-aimed click on the label may not slam a parameter to its floor
	*ledgerHitsRail { |rect, x, scale|
		var rail = this.ledgerRailRect(rect, scale);
		var grab = this.ledgerMetrics(scale)[\grab];
		^(x >= (rail.left - grab)) and: { x <= (rail.right + grab) }
	}

	*ledgerNormAt { |rect, x, scale|
		var rail = this.ledgerRailRect(rect, scale);
		if(rail.width <= 0) { ^nil };
		^((x - rail.left) / rail.width).clip(0, 1)
	}

	*ledgerSeatAt { |rect, x, count = 2, scale|
		var rail = this.ledgerRailRect(rect, scale);
		if(count < 1 or: { rail.width <= 0 }) { ^nil };
		^((((x - rail.left) / rail.width) * count).floor.asInteger)
			.clip(0, count - 1)
	}

	*drawLedgerRow { |rect, mode = \num, value = 0, label = "", display = "",
		seats, family, skin = \day, hover = false, pressed = false,
		center, danger = false, scale|
		var palette = this.palette(skin), m = this.ledgerMetrics(scale);
		var body = rect.insetBy(m[\inset], 0);
		var rail = this.ledgerRailRect(rect, scale);
		var accent = palette[family] ?? { palette[\line] };
		var textH = m[\labelSize] + 4;
		var y = rail.center.y, norm, handleX, originX, reach;
		Pen.stringLeftJustIn(label.asString,
			Rect(body.left, body.center.y - (textH / 2),
				m[\labelWidth] - m[\gap], textH),
			this.font(\label, m[\labelSize]),
			if(hover) { palette[\line] } { palette[\whisper] });
		Pen.strokeColor = if(hover) { palette[\line] } { palette[\whisper] };
		Pen.width = this.strokes[\hairline];
		Pen.line(Point(rail.left, y), Point(rail.right, y));
		Pen.stroke;
		if(#[\toggle, \enum].includes(mode)) {
			this.prLedgerSeats(rail, seats ? [], (value ? 0).asInteger,
				accent, palette, hover, m);
		} {
			// a unipolar rail fills from its left end; a bipolar one grows away
			// from a visible centre tick, so zero is a place and not a guess
			norm = (value ? 0).clip(0, 1);
			handleX = rail.left + (rail.width * norm);
			originX = rail.left + (rail.width * (center ? 0));
			center !? {
				Pen.strokeColor = palette[\whisper];
				Pen.width = this.strokes[\hairline];
				Pen.line(Point(originX, y - 5), Point(originX, y + 5));
				Pen.stroke;
			};
			Pen.strokeColor = accent;
			Pen.width = this.strokes[\strong];
			Pen.line(Point(originX, y), Point(handleX, y));
			Pen.stroke;
			// the handle is a tick drawn across the rail — a setting on a scale
			// wears a scale mark, exactly as the shell's reel notch does (§16.6)
			reach = m[\seatRadius] * if(hover) { 1.7 } { 1.3 };
			Pen.strokeColor = if(hover) { palette[\spark] } { accent };
			Pen.width = this.strokes[\normal];
			Pen.line(Point(handleX, y - reach), Point(handleX, y + reach));
			Pen.stroke;
		};
		Pen.stringRightJustIn(display.asString,
			Rect(body.right - m[\valueWidth], body.center.y - (textH / 2),
				m[\valueWidth], textH),
			this.font(\number, m[\valueSize]),
			if(hover) { palette[\spark] } { palette[\line] });
		if(pressed) {
			Pen.strokeColor = accent.copy.alpha_(0.55);
			Pen.width = this.strokes[\hairline];
			Pen.addRoundedRect(Rect(rail.left - m[\grab], rect.top + 2,
				rail.width + (2 * m[\grab]), rect.height - 4),
				this.metrics[\buttonRadius], this.metrics[\buttonRadius]);
			Pen.stroke;
		};
		// SPEC §7: the mic row may brighten red — its module may not
		if(danger) {
			Pen.strokeColor = palette[\danger].copy.alpha_(0.7);
			Pen.width = this.strokes[\normal];
			Pen.addRoundedRect(body.insetBy(0, 2),
				this.metrics[\buttonRadius], this.metrics[\buttonRadius]);
			Pen.stroke;
		};
	}

	// a toggle or enum is stable seats, never a sliding value: one labelled seat
	// per value, in the order CQSpec gave them
	*prLedgerSeats { |rail, seats, index, accent, palette, hover = false, m|
		var cell = rail.width / seats.size.max(1);
		var radius = m[\seatRadius], textH = m[\seatSize] + 4;
		seats.do { |seat, i|
			var left = rail.left + (cell * i);
			var seatCenter = Point(left + (radius * 2), rail.center.y);
			var lit = i == index;
			if(lit) {
				Pen.fillColor = accent;
				Pen.addOval(Rect.aboutPoint(seatCenter, radius, radius));
				Pen.fill;
			} {
				Pen.strokeColor = palette[\whisper];
				Pen.width = this.strokes[\hairline];
				Pen.addOval(Rect.aboutPoint(seatCenter, radius, radius));
				Pen.stroke;
			};
			Pen.stringLeftJustIn(seat.asString,
				Rect(seatCenter.x + radius + 4, rail.center.y - (textH / 2),
					(cell - (radius * 3) - 4).max(8), textH),
				this.font(\range, m[\seatSize]),
				if(lit) {
					if(hover) { palette[\spark] } { palette[\line] }
				} {
					palette[\whisper]
				});
		};
	}

	// One ledger island: its title over a full rule, then its subgroups. The y
	// offsets come from the caller's own layout list, so the painted labels and
	// the laid-out rows can never drift apart.
	//
	// Three ranks, told by rule length rather than by shouting (§16.4 keeps
	// everything lowercase but PANIC/REC/HOLD): the island title carries a rule
	// across the whole island, a subgroup carries a lighter rule from the end of
	// its own name to the island's edge, and a row carries only its value rail.
	// The subgroup label is line ink, not whisper — it names a region of the
	// signal, and the rows under it are the whispers.
	*drawLedgerIsland { |rect, items, skin = \day, scale, titleFamily = \line|
		var palette = this.palette(skin), m = this.ledgerMetrics(scale);
		var sizes = scale ?? { CQGuiScale.metrics(\compact) };
		var left = rect.left + m[\inset], right = rect.right - m[\inset];
		items.do { |item|
			var y = rect.top + item[\y], textH, ruleY, labelEnd;
			switch(item[\kind],
				\title, {
					textH = sizes[\title] + 4;
					Pen.stringLeftJustIn(item[\label].asString,
						Rect(left, y + 2, right - left, textH),
						this.font(\label, sizes[\title]),
						palette[item[\family] ? titleFamily]);
					Pen.strokeColor = palette[\whisper];
					Pen.width = this.strokes[\hairline];
					Pen.line(Point(left, y + item[\height] - 3),
						Point(right, y + item[\height] - 3));
					Pen.stroke;
				},
				\group, {
					textH = sizes[\subgroup] + 4;
					ruleY = y + (item[\height] / 2) + 1;
					labelEnd = left + CQGuiScale.textWidth(item[\label],
						sizes[\subgroup]) + 10;
					Pen.stringLeftJustIn(item[\label].asString,
						Rect(left, y + ((item[\height] - textH) / 2),
							right - left, textH),
						this.font(\label, sizes[\subgroup]), palette[\line]);
					if(labelEnd < (right - 12)) {
						Pen.strokeColor = palette[\whisper].copy.alpha_(0.4);
						Pen.width = this.strokes[\hairline];
						Pen.line(Point(labelEnd, ruleY), Point(right, ruleY));
						Pen.stroke;
					};
				}
			);
		};
	}

	*drawOrganFrame { |rect, title, family = \line, skin = \day, seed = 0|
		var palette = this.palette(skin), y = rect.top + 22, points;
		points = this.wobble([Point(rect.left + 8, y), Point(rect.center.x - 42, y),
			Point(rect.center.x + 42, y), Point(rect.right - 8, y)], seed, 0.8);
		Pen.strokeColor = palette[family] ?? { palette[\line] };
		Pen.width = this.strokes[\hairline];
		Pen.moveTo(points.first);
		points.drop(1).do { |point| Pen.lineTo(point) };
		Pen.stroke;
		Pen.stringCenteredIn(title.asString, Rect(rect.left, rect.top + 4, rect.width, 12),
			this.font(\label), palette[\line]);
	}

	// `labelSize` lets a surface with its own type step (the Drawer's pages, the
	// persistent chrome) title its boxes at the size the rest of that surface
	// reads at; nil keeps the kit's fixed px. The title rect is measured from
	// the box rather than fixed at 80/90 px, which clipped a longer caption the
	// moment the type grew.
	*drawToolHeader { |rect, title, skin = \day, labelSize|
		var palette = this.palette(skin), size = labelSize ? this.metrics[\labelSize];
		var y = rect.top + size + 8;
		Pen.strokeColor = palette[\whisper];
		Pen.width = this.strokes[\hairline];
		Pen.line(Point(rect.left + 8, y), Point(rect.right - 8, y));
		Pen.stroke;
		Pen.stringLeftJustIn(title.asString,
			Rect(rect.left + 8, rect.top + 3, (rect.width - 16).max(20), size + 3),
			this.font(\label, labelSize), palette[\line]);
	}

	*drawToolBox { |rect, title, skin = \day, active = false, labelSize,
		titleTop = 3|
		var palette = this.palette(skin), body = rect.insetBy(1, 1);
		var size = labelSize ? this.metrics[\labelSize];
		Pen.fillColor = palette[\ground];
		Pen.strokeColor = if(active) { palette[\line] } { palette[\whisper] };
		Pen.width = this.strokes[\hairline];
		Pen.addRoundedRect(body, this.metrics[\buttonRadius], this.metrics[\buttonRadius]);
		Pen.fillStroke;
		Pen.stringLeftJustIn(title.asString,
			Rect(body.left + 6, body.top + titleTop,
				(body.width - 12).max(20), size + 3),
			this.font(\label, labelSize), palette[\line]);
	}

	// §16.7's only dashed rectangle in the whole app: a file drag is hovering a
	// Night bird. Pen's dash
	// pattern is global state, so it is put back to solid here rather than
	// trusting every call site to remember.
	*drawDropTarget { |rect, skin = \day|
		var palette = this.palette(skin);
		Pen.strokeColor = palette[\line];
		Pen.width = this.strokes[\strong];
		Pen.lineDash = FloatArray[4, 3];
		Pen.addRect(rect.insetBy(2, 2));
		Pen.stroke;
		Pen.lineDash = FloatArray[1, 0];
	}

	*drawBlob { |rect, skin = \day, seed = 0|
		var palette = this.palette(skin);
		var points = this.wobble([rect.leftTop, rect.rightTop, rect.rightBottom,
			rect.leftBottom, rect.leftTop], seed, 1.5);
		Pen.fillColor = palette[\recess];
		Pen.moveTo(points.first);
		points.drop(1).do { |point| Pen.lineTo(point) };
		Pen.fill;
	}
}
