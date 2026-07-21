// CQInkNight — the Ink & Phosphor kit's night-body vocabulary (GUI_SPEC §16.6).
// Pen recipes for the archipelago: castle blobs, the ring and its fireflies,
// coco spirals, roots, the mic sprout, deck birds and the small source-jack
// decorations. All color through CQInk.palette (§16.11 single source); geometry
// arrives precomputed (CQNightGeometry) so a drawFunc's cost stays its
// Pen-call count (§10).

CQInkNight {

	// one smooth stroke through precomputed cubic segments
	*strokeSegments { |segments|
		Pen.moveTo(segments.first[0]);
		segments.do { |seg| Pen.curveTo(seg[3], seg[1], seg[2]) };
		Pen.stroke;
	}

	// castle body: seeded hollow blob with the aura as a quiet radial gradient
	// inside (§16.6); dial ticks sit rotated 22.5° so no tick hides exactly
	// under the tri leaf or cas crown grown on the rim. Origin = the castle
	// layer's own box corner: the layer is opaque, so an ambient glow repaint
	// never re-runs the whole landscape underneath.
	*drawCastle { |castle, origin, aura, spesalState = 0, rangeLetter = "",
		skin = \night|
		var palette = CQInk.palette(skin), center = castle[\center], tickAngle;
		Pen.translate(origin.x.neg, origin.y.neg);
		// the river's short passage under the body
		Pen.strokeColor = palette[\whisper].copy.alpha_(0.55);
		Pen.width = CQInk.strokes[\hairline];
		castle[\stubs].do { |stub| Pen.line(stub[0], stub[1]) };
		Pen.stroke;
		if((aura ? 0) > 0.01) {
			Pen.addOval(Rect.aboutPoint(center,
				castle[\bodyRx] + 6, castle[\bodyRy] + 6));
			Pen.fillRadialGradient(center, center, 0, castle[\bodyRx] + 6,
				palette[\cas].copy.alpha_((aura.clip(0, 1) * 0.3)),
				palette[\cas].copy.alpha_(0));
		};
		Pen.strokeColor = palette[\line];
		Pen.width = CQInk.strokes[\normal];
		this.strokeSegments(castle[\blobSegments]);
		Pen.strokeColor = palette[\whisper];
		Pen.width = CQInk.strokes[\hairline];
		8.do { |i|
			var angle = (22.5 + (i * 45)).degrad;
			Pen.line(CQNightGeometry.polar(center, castle[\dialR] - 3, angle),
				CQNightGeometry.polar(center, castle[\dialR] + 3, angle));
		};
		Pen.stroke;
		// current spesal state lit — a mark, never a number (§16.4)
		tickAngle = (22.5 + ((spesalState.asInteger.clip(0, 7)) * 45)).degrad;
		Pen.strokeColor = palette[\spark];
		Pen.width = CQInk.strokes[\strong];
		Pen.line(CQNightGeometry.polar(center, castle[\dialR] - 4, tickAngle),
			CQNightGeometry.polar(center, castle[\dialR] + 4, tickAngle));
		Pen.stroke;
		Pen.stringCenteredIn(rangeLetter.asString,
			Rect.aboutPoint(Point(center.x, center.y + castle[\dialR] + 7), 7, 6),
			CQInk.font(\range), palette[\whisper]);
	}

	// the irregular hairline circle through the five castles, with the
	// fireflies that carry the loop's direction (k → k+1). Phase advances only
	// when telemetry moves — a still bay keeps a still river.
	*drawRing { |geometry, phase = 0, eyeActive = false, skin = \night|
		var palette = CQInk.palette(skin), origin = geometry[\ringBox].leftTop;
		Pen.translate(origin.x.neg, origin.y.neg);
		Pen.strokeColor = palette[\whisper].copy.alpha_(0.7);
		Pen.width = CQInk.strokes[\hairline];
		this.strokeSegments(geometry[\ringSegments]);
		Pen.fillColor = palette[\spark];
		10.do { |i|
			Pen.addOval(Rect.aboutPoint(
				CQNightGeometry.fireflyPoint(geometry, phase + (i / 10)), 1.2, 1.2));
		};
		Pen.fill;
		this.drawEye(geometry[\eye], eyeActive, skin);
	}

	// the delay RAM as a coconut cross-section, drawn by the shell's own opaque
	// layer: spiral (the §6.1 winding surface), hold seal, the hold heart at
	// the innermost eye, the press-to-flip mouth chevron and the org stem
	*drawShell { |coco, origin, holdOn = false, flipped = false, skin = \night|
		var palette = CQInk.palette(skin);
		Pen.translate(origin.x.neg, origin.y.neg);
		Pen.strokeColor = palette[\line].copy.alpha_(0.8);
		Pen.width = CQInk.strokes[\normal];
		this.strokeSegments(coco[\segments]);
		if(holdOn) {
			Pen.strokeColor = palette[\danger].copy.alpha_(0.6);
			Pen.width = CQInk.strokes[\strong];
			Pen.addOval(Rect.aboutPoint(coco[\center], coco[\outerR], coco[\outerR]));
			Pen.stroke;
		};
		// the heart: hollow whisper eye while open, danger-red while sealed
		if(holdOn) {
			Pen.fillColor = palette[\danger];
			Pen.addOval(Rect.aboutPoint(coco[\center], 2.5, 2.5));
			Pen.fill;
			Pen.strokeColor = palette[\danger].copy.alpha_(0.4);
			Pen.addOval(Rect.aboutPoint(coco[\center], 5.5, 5.5));
			Pen.stroke;
		} {
			Pen.strokeColor = palette[\whisper];
			Pen.width = CQInk.strokes[\hairline];
			Pen.addOval(Rect.aboutPoint(coco[\center], 2.5, 2.5));
			Pen.stroke;
		};
		this.drawMouthChevron(coco[\chevron], flipped, skin);
		// org stem from the second turn out to the hanging fruit jack
		Pen.strokeColor = palette[\organ].copy.alpha_(0.8);
		Pen.width = CQInk.strokes[\hairline];
		Pen.line(coco[\orgSpiralPoint], coco[\orgCenter]);
		Pen.stroke;
	}

	// §6.1 tide crescent: an open arc whose luminous fill is the value —
	// the night's only continuous hand, never a knob, never a number
	*drawCrescent { |center, norm = 0, family = \line, active = false,
		skin = \night|
		var palette = CQInk.palette(skin);
		var accent = palette[family] ?? { palette[\whisper] };
		var start = 0.75pi, sweep = 1.5pi;
		Pen.strokeColor = palette[\whisper].copy
			.alpha_(if(active) { 0.9 } { 0.55 });
		Pen.width = CQInk.strokes[\hairline];
		Pen.addArc(center, 9, start, sweep);
		Pen.stroke;
		if(norm > 0.001) {
			Pen.strokeColor = accent.copy.alpha_(if(active) { 1.0 } { 0.85 });
			Pen.width = CQInk.strokes[\strong];
			Pen.addArc(center, 9, start, sweep * norm.clip(0, 1));
			Pen.stroke;
		};
	}

	// §6.1 the ring's eye: the qFreq/qChaos pupil at the ring's center
	*drawEye { |center, active = false, skin = \night|
		var palette = CQInk.palette(skin);
		Pen.fillColor = palette[\spark].copy.alpha_(if(active) { 0.55 } { 0.25 });
		Pen.addOval(Rect.aboutPoint(center, 4, 4));
		Pen.fill;
		Pen.strokeColor = palette[\spark].copy.alpha_(if(active) { 1.0 } { 0.7 });
		Pen.width = CQInk.strokes[\hairline];
		Pen.addOval(Rect.aboutPoint(center, if(active) { 6 } { 5 },
			if(active) { 6 } { 5 }));
		Pen.stroke;
	}

	// input root: wobbled three-segment polyline with one side branch;
	// the green tip is the env jack itself (drawn by its endpoint view)
	*drawRoot { |root, skin = \night|
		var palette = CQInk.palette(skin), points = root[\points];
		Pen.strokeColor = palette[\line].copy.alpha_(0.75);
		Pen.width = CQInk.strokes[\normal];
		Pen.moveTo(points.first);
		points.drop(1).do { |point| Pen.lineTo(point) };
		Pen.stroke;
		Pen.width = CQInk.strokes[\hairline];
		Pen.line(root[\branch][0], root[\branch][1]);
		Pen.stroke;
	}

	// mic sprout, 3 states (§16.6): hidden / dormant whisper bud / danger-red
	*drawSprout { |sprout, state = \hidden, skin = \night|
		var palette = CQInk.palette(skin), color, base, tip;
		if(state == \hidden) { ^this };
		color = if(state == \live) { palette[\danger] } { palette[\whisper] };
		base = sprout[\base];
		tip = sprout[\tip];
		Pen.strokeColor = color;
		Pen.width = CQInk.strokes[\hairline];
		Pen.moveTo(base);
		Pen.curveTo(tip, Point(base.x + 6, base.y - ((base.y - tip.y) * 0.4)),
			Point(tip.x + 4, tip.y + ((base.y - tip.y) * 0.3)));
		Pen.stroke;
		Pen.addOval(Rect.aboutPoint(tip, 3, 4.5));
		Pen.stroke;
		if(state == \live) {
			Pen.strokeColor = palette[\danger].copy.alpha_(0.6);
			Pen.addOval(Rect.aboutPoint(tip, 7, 8));
			Pen.stroke;
		};
	}

	// deck bird: two wing béziers and a body dot (§16.6), span ≈ 30 px,
	// drawn by the bird's own opaque layer (origin = its box corner)
	*drawBird { |bird, origin, skin = \night|
		var palette = CQInk.palette(skin), c = bird[\center], span = bird[\span];
		origin !? { Pen.translate(origin.x.neg, origin.y.neg) };
		Pen.strokeColor = palette[\line];
		Pen.width = CQInk.strokes[\normal];
		Pen.moveTo(Point(c.x - (span * 0.5), c.y - (span * 0.12)));
		Pen.curveTo(c, Point(c.x - (span * 0.34), c.y - (span * 0.4)),
			Point(c.x - (span * 0.1), c.y - (span * 0.28)));
		Pen.stroke;
		Pen.moveTo(Point(c.x + (span * 0.5), c.y - (span * 0.12)));
		Pen.curveTo(c, Point(c.x + (span * 0.34), c.y - (span * 0.4)),
			Point(c.x + (span * 0.1), c.y - (span * 0.28)));
		Pen.stroke;
		Pen.fillColor = palette[\line];
		Pen.addOval(Rect.aboutPoint(c, 1.7, 1.7));
		Pen.fill;
	}

	// ---- source-jack decorations (drawn inside the opaque endpoint views,
	// under the family dot, so an aura repaint stays local) ----

	// the tri leaf: 8 px triangular leaf on a stem, the orange dot at its base
	*drawLeaf { |anchor, skin = \night|
		var palette = CQInk.palette(skin);
		Pen.strokeColor = palette[\tri].copy.alpha_(0.75);
		Pen.width = CQInk.strokes[\hairline];
		Pen.line(anchor + Point(2, -2), anchor + Point(7, -7));
		Pen.stroke;
		Pen.fillColor = palette[\tri].copy.alpha_(0.45);
		Pen.moveTo(anchor + Point(7, -7));
		Pen.lineTo(anchor + Point(12, -9));
		Pen.lineTo(anchor + Point(8, -12));
		Pen.fill;
	}

	// the cas crown: three 4 px square teeth over the gray dot
	*drawCrown { |anchor, skin = \night|
		var palette = CQInk.palette(skin);
		Pen.strokeColor = palette[\cas].copy.alpha_(0.85);
		Pen.width = CQInk.strokes[\hairline];
		3.do { |i|
			Pen.addRect(Rect(anchor.x - 7 + (i * 5), anchor.y - 12, 4, 4));
		};
		Pen.stroke;
	}

	// the coco mouth chevron: read direction at the spiral's mouth, reversed
	// by flip (down = forward, up = flipped — mirrored the same on both cocos)
	*drawMouthChevron { |anchor, flipped = false, skin = \night|
		var palette = CQInk.palette(skin), dy = if(flipped) { -3 } { 3 };
		Pen.strokeColor = palette[\cas];
		Pen.width = CQInk.strokes[\hairline];
		Pen.line(anchor + Point(8, dy.neg), anchor + Point(11, dy));
		Pen.line(anchor + Point(11, dy), anchor + Point(14, dy.neg));
		Pen.stroke;
	}

	// the org fruit's stem tip entering its view from the spiral side
	*drawStemStub { |anchor, skin = \night|
		var palette = CQInk.palette(skin);
		Pen.strokeColor = palette[\organ].copy.alpha_(0.8);
		Pen.width = CQInk.strokes[\hairline];
		Pen.line(anchor + Point(0, -12), anchor + Point(0, -5));
		Pen.stroke;
	}
}
