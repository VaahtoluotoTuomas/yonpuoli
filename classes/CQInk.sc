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
			lamp: Color.new255(232, 201, 122)
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
			lamp: Color.new255(255, 233, 168)
		);
		palettes.values.do { |palette|
			palette[\hold] = palette[\danger];
			palette[\warn] = palette[\danger];
		};
		glyphNames = #[wedge, flip, spark, wave, tickRing, pendulum,
			hold, skip, spesal, panic, rec, drawer, day, night];
	}

	*palette { |skin = \day| ^palettes[skin] ?? { palettes[\day] } }
	*strokes { ^(hairline: 1.0, normal: 1.5, strong: 2.0, state: 3.0) }
	*metrics {
		^(grid: 8, labelSize: 10, numberSize: 10, noticeSize: 11,
			rangeSize: 9, knobDiameter: 26, featuredKnobDiameter: 32,
			primaryKnobDiameter: 36, buttonRadius: 3,
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

	*font { |role = \label|
		var m = this.metrics;
		^switch(role,
			\number, { Font.monospace(m[\numberSize]) },
			\notice, { Font.sansSerif(m[\noticeSize]) },
			\spellbook, { Font.monospace(m[\noticeSize]) },
			\range, { Font.sansSerif(m[\rangeSize]) },
			{ Font.sansSerif(m[\labelSize]) }
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

	*drawJack { |center, family, skin = \day, state = \idle, aura = 0|
		var palette = this.palette(skin), radius = if(state == \inHand) { 5 } { 4 };
		if(aura > 0) {
			Pen.fillColor = palette[family].copy.alpha_((aura * 0.22).clip(0, 0.22));
			Pen.addOval(Rect.aboutPoint(center, 8 + (aura * 5), 8 + (aura * 5)));
			Pen.fill;
		};
		Pen.fillColor = palette[family];
		Pen.addOval(Rect.aboutPoint(center, radius, radius));
		Pen.fill;
		Pen.strokeColor = palette[\line];
		Pen.width = this.strokes[\hairline];
		Pen.addOval(Rect.aboutPoint(center, radius + 1, radius + 1));
		Pen.stroke;
	}

	*drawSocket { |center, family, skin = \day, state = \idle|
		var palette = this.palette(skin), radius = if(state == \candidate) { 7.2 } { 6 };
		if(state == \candidate) {
			Pen.fillColor = palette[family].copy.alpha_(0.18);
			Pen.addOval(Rect.aboutPoint(center, 11, 11));
			Pen.fill;
		};
		Pen.strokeColor = palette[family];
		Pen.width = if(state == \occupied) { this.strokes[\strong] } { this.strokes[\normal] };
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

	*drawLamp { |center, charge = 0, on = false, skin = \day|
		var palette = this.palette(skin), fill = if(on) { 1 } { (charge / 0.5).clip(0, 1) };
		// An idle charge lamp is a recessed bead, not another hollow patch socket.
		Pen.fillColor = palette[\recess];
		Pen.addOval(Rect.aboutPoint(center, 4, 4));
		Pen.fill;
		Pen.fillColor = palette[\lamp].copy.alpha_(fill);
		Pen.addOval(Rect.aboutPoint(center, 4, 4));
		Pen.fill;
		Pen.strokeColor = palette[\whisper];
		Pen.width = this.strokes[\hairline];
		Pen.addOval(Rect.aboutPoint(center, 4, 4));
		Pen.stroke;
	}

	*drawCable { |start, end, weight, skin = \day, alpha = 1|
		var palette = this.palette(skin), controls = this.cableControls(start, end);
		var color = if(weight < 0) { palette[\inverso] } { palette[\verso] };
		var points, distance, stepCount;
		Pen.strokeColor = color.copy.alpha_(alpha.clip(0, 1));
		Pen.width = 1.5 + (weight.abs.clip(0, 1) * 1.5);
		Pen.moveTo(start);
		Pen.curveTo(end, controls[0], controls[1]);
		Pen.stroke;
		if(weight < 0) {
			distance = sqrt(((end.x - start.x).squared) + ((end.y - start.y).squared));
			stepCount = max(8, (distance / 12).ceil.asInteger);
			points = this.cablePoints(start, end, stepCount);
			points.do { |point, i|
				var tangent, magnitude, normal;
				if(i.odd and: { i > 0 } and: { i < (points.size - 1) }) {
					tangent = points[i + 1] - points[i - 1];
					magnitude = sqrt(tangent.x.squared + tangent.y.squared).max(0.001);
					normal = Point(tangent.y.neg / magnitude,
						tangent.x / magnitude) * 3;
					Pen.strokeColor = color.copy.alpha_(alpha.clip(0, 1));
					Pen.width = this.strokes[\hairline];
					Pen.line(point - normal, point + normal);
					Pen.stroke;
				};
			};
		};
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
		skin = \day, active = false, diameter|
		var palette = this.palette(skin), m = this.metrics;
		var radius = (diameter ? m[\knobDiameter]) / 2;
		var center = Point(rect.center.x, rect.top + 14 + radius);
		var start = 0.75pi, sweep = 1.5pi, angle, pointer, accent;
		value = value.clip(0, 1);
		accent = palette[family] ?? { palette[\whisper] };
		Pen.stringCenteredIn(label.asString, Rect(rect.left, rect.top, rect.width, 11),
			this.font(\label), palette[\whisper]);
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
			Rect(rect.left, rect.bottom - 11, rect.width, 10),
			this.font(\number), palette[\whisper]);
	}

	*drawButton { |rect, label, skin = \day, active = false, pressed = false,
		accent = \line, glyph, surface = \ground|
		var palette = this.palette(skin), body = rect.insetBy(1, 2), ink = palette[accent];
		var fill = if(active or: { pressed }) { ink } { palette[surface] };
		var text = if(active or: { pressed }) { palette[surface] } { ink };
		Pen.fillColor = fill;
		Pen.strokeColor = ink;
		Pen.width = this.strokes[\normal];
		Pen.addRoundedRect(body, this.metrics[\buttonRadius], this.metrics[\buttonRadius]);
		Pen.fillStroke;
		glyph !? {
			this.drawGlyph(glyph, Rect(body.left + 6, body.center.y - 4, 8, 8), text);
		};
		Pen.stringCenteredIn(label.asString,
			Rect(body.left + if(glyph.isNil) { 2 } { 14 }, body.top,
				body.width - if(glyph.isNil) { 4 } { 16 }, body.height),
			this.font(\label), text);
	}

	*drawSlider { |rect, value = 0, skin = \day, family|
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
		Pen.fillColor = palette[\ground];
		Pen.strokeColor = palette[\line];
		Pen.addOval(Rect.aboutPoint(Point(x, rail.center.y), 4, 4));
		Pen.fillStroke;
	}

	*drawSwitch { |rect, label, options, index = 0, skin = \day, family|
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

	*drawToolHeader { |rect, title, skin = \day|
		var palette = this.palette(skin), y = rect.top + 18;
		Pen.strokeColor = palette[\whisper];
		Pen.width = this.strokes[\hairline];
		Pen.line(Point(rect.left + 8, y), Point(rect.right - 8, y));
		Pen.stroke;
		Pen.stringLeftJustIn(title.asString, Rect(rect.left + 8, rect.top + 3, 90, 11),
			this.font(\label), palette[\line]);
	}

	*drawToolBox { |rect, title, skin = \day, active = false|
		var palette = this.palette(skin), body = rect.insetBy(1, 1);
		Pen.fillColor = palette[\ground];
		Pen.strokeColor = if(active) { palette[\line] } { palette[\whisper] };
		Pen.width = this.strokes[\hairline];
		Pen.addRoundedRect(body, this.metrics[\buttonRadius], this.metrics[\buttonRadius]);
		Pen.fillStroke;
		Pen.stringLeftJustIn(title.asString, Rect(body.left + 6, body.top + 3, 80, 11),
			this.font(\label), palette[\line]);
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
