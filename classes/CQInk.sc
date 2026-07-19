// CQInk — the single Ink & Phosphor vector kit (GUI_SPEC §16.2/§16.11).
// All palette, stroke, seeded-wobble and primitive drawing decisions live here;
// views provide semantic state and geometry only.

CQInk {
	classvar palettes, <glyphNames;

	*initClass {
		palettes = IdentityDictionary.new;
		palettes[\day] = (
			ground: Color.new255(244, 237, 224), recess: Color.new255(231, 222, 203),
			line: Color.new255(42, 37, 30), whisper: Color.new255(111, 101, 87),
			spark: Color.new255(42, 37, 30), tri: Color.new255(217, 131, 36),
			cas: Color.new255(133, 127, 115), fm: Color.new255(61, 111, 165),
			input: Color.new255(79, 138, 80), spaf: Color.new255(124, 88, 168),
			organ: Color.new255(212, 175, 55), pre: Color.new255(251, 247, 237),
			verso: Color.new255(227, 154, 45), inverso: Color.new255(74, 124, 199),
			danger: Color.new255(196, 61, 43), warning: Color.new255(168, 123, 47),
			lamp: Color.new255(232, 201, 122)
		);
		palettes[\night] = (
			ground: Color.new255(18, 20, 28), recess: Color.new255(26, 29, 38),
			line: Color.new255(169, 174, 184), whisper: Color.new255(106, 111, 123),
			spark: Color.new255(237, 230, 210), tri: Color.new255(255, 177, 61),
			cas: Color.new255(183, 177, 163), fm: Color.new255(100, 167, 255),
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
		glyphNames = #[hold, flip, skip, spesal, panic, rec, drawer, day, night];
	}

	*palette { |skin = \day| ^palettes[skin] ?? { palettes[\day] } }
	*strokes { ^(hairline: 1.0, normal: 1.5, strong: 2.0, state: 3.0) }

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
			\hold, { [[1, 1], [1, 7], nil, [7, 1], [7, 7]] },
			\flip, { [[7, 1], [2, 4], [7, 7], nil, [1, 1], [1, 7]] },
			\skip, { [[1, 1], [1, 7], nil, [2, 4], [7, 1], [7, 7], [2, 4]] },
			\spesal, { [[4, 0], [5, 3], [8, 4], [5, 5], [4, 8], [3, 5], [0, 4], [3, 3], [4, 0]] },
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
			Pen.fillColor = palette[family].alpha_((aura * 0.22).clip(0, 0.22));
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
			Pen.fillColor = palette[family].alpha_(0.18);
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
		Pen.fillColor = palette[\lamp].alpha_(fill);
		Pen.addOval(Rect.aboutPoint(center, 4, 4));
		Pen.fill;
		Pen.strokeColor = palette[\line];
		Pen.width = this.strokes[\hairline];
		Pen.addOval(Rect.aboutPoint(center, 4, 4));
		Pen.stroke;
	}

	*drawCable { |start, end, weight, skin = \day, alpha = 1|
		var palette = this.palette(skin), sag = max(18, (end.x - start.x).abs * 0.12);
		var color = if(weight < 0) { palette[\inverso] } { palette[\verso] };
		Pen.strokeColor = color.alpha_(alpha.clip(0, 1));
		Pen.width = 1.5 + (weight.abs.clip(0, 1) * 1.5);
		Pen.moveTo(start);
		Pen.curveTo(end, start + Point(0, sag), end + Point(0, sag));
		Pen.stroke;
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
