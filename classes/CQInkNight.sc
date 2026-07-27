// CQInkNight — the Ink & Phosphor kit's night-body vocabulary (GUI_SPEC §16.6).
// Pen recipes for the archipelago: castle blobs, the ring and its fireflies,
// coco spirals, roots, the mic sprout, deck birds and the small source-jack
// decorations. All color through CQInk.palette (§16.11 single source); geometry
// arrives precomputed (CQNightGeometry) so a drawFunc's cost stays its
// Pen-call count (§10).

CQInkNight {

	// one smooth path through precomputed cubic segments
	*addSegments { |segments|
		Pen.moveTo(segments.first[0]);
		segments.do { |seg| Pen.curveTo(seg[3], seg[1], seg[2]) };
	}

	*strokeSegments { |segments|
		this.addSegments(segments);
		Pen.stroke;
	}

	// castle body: seeded hollow blob with the aura as a quiet radial gradient
	// inside (§16.6). The spesal dial is the castle's inner clock: a whisper
	// face inside the body — where no opaque endpoint view can cover it —
	// with the current state as a lit spark bead, and a static hover
	// brightening (§4.5) so the face reads as pressable. Origin = the castle
	// layer's own box corner. The paper is transparent and the blob fills only
	// its irregular flesh with ground, so no rectangular edge can cut the river.
	*drawCastle { |castle, origin, aura, spesalState = 0, range,
		dialHover = false, rangeHover = false,
		bodyHover = false, skin = \night|
		var palette = CQInk.palette(skin), center = castle[\center];
		var tickAngle, bead;
		Pen.translate(origin.x.neg, origin.y.neg);
		// The castle view is transparent. Only the irregular flesh masks the
		// river below it; the old opaque rectangle needed two artificial ring
		// stubs, which read as unexplained short lines outside every castle.
		Pen.fillColor = palette[\ground];
		this.addSegments(castle[\blobSegments]);
		Pen.fill;
		if((aura ? 0) > 0.01) {
			Pen.addOval(Rect.aboutPoint(center,
				castle[\bodyRx] + 6, castle[\bodyRy] + 6));
			Pen.fillRadialGradient(center, center, 0, castle[\bodyRx] + 6,
				palette[\cas].copy.alpha_((aura.clip(0, 1) * 0.3)),
				palette[\cas].copy.alpha_(0));
		};
		// the body's own hover echo: the quiet outer ring §4.5 gives every
		// pressable Ink control, here saying "this whole body answers a
		// double-click" (the §6 solo). Static ink, never a pulse, and the
		// resting drawing is untouched — two Pen calls only under the pointer.
		if(bodyHover) {
			Pen.strokeColor = palette[\whisper].copy.alpha_(0.55);
			Pen.width = CQInk.strokes[\hairline];
			Pen.addOval(Rect.aboutPoint(center,
				castle[\bodyRx] + CQNightMeasures.bodyMargin,
				castle[\bodyRy] + CQNightMeasures.bodyMargin));
			Pen.stroke;
		};
		Pen.strokeColor = palette[\line];
		Pen.width = CQInk.strokes[\normal];
		this.strokeSegments(castle[\blobSegments]);
		// the inner clock's face — the tap affordance
		Pen.strokeColor = palette[\whisper].copy
			.alpha_(if(dialHover) { 0.85 } { 0.4 });
		Pen.width = CQInk.strokes[\hairline];
		Pen.addOval(Rect.aboutPoint(center, castle[\dialR], castle[\dialR]));
		Pen.stroke;
		Pen.strokeColor = palette[\whisper];
		8.do { |i|
			var angle = (22.5 + (i * 45)).degrad;
			var tick = CQNightMeasures.clockTickReach;
			Pen.line(CQNightGeometry.polar(center, castle[\dialR] - tick, angle),
				CQNightGeometry.polar(center, castle[\dialR] + tick, angle));
		};
		Pen.stroke;
		// current spesal state lit as the clock's spark hand — a mark, never
		// a number (§16.4); bead + halo stay inside the endpoint clearance
		tickAngle = (22.5 + ((spesalState.asInteger.clip(0, 7)) * 45)).degrad;
		bead = CQNightGeometry.polar(center, castle[\dialR], tickAngle);
		Pen.fillColor = palette[\spark];
		Pen.addOval(Rect.aboutPoint(bead, CQNightMeasures.clockBeadR,
			CQNightMeasures.clockBeadR));
		Pen.fill;
		Pen.strokeColor = palette[\spark]
			.copy.alpha_(if(dialHover) { 0.8 } { 0.5 });
		Pen.width = CQInk.strokes[\hairline];
		Pen.addOval(Rect.aboutPoint(bead, CQNightMeasures.clockBeadHaloR,
			CQNightMeasures.clockBeadHaloR));
		Pen.stroke;
		this.drawRangeRipple(castle[\rangeMark], range, rangeHover, skin);
	}

	// A regime drawn as rate, not a transplanted switch: one long half-wave for
	// infra, one full cycle for balcium, five tight half-waves for audio. The
	// fm socket's glyph is always one cycle inside a ring; this mark changes
	// density on open flesh and needs no legacy letter caption.
	*drawRangeRipple { |anchor, range, hover = false, skin = \night|
		var palette = CQInk.palette(skin);
		var halves = switch(range,
			\infra, { 1 }, \balcium, { 2 }, \audio, { 5 }, { 0 });
		if(halves <= 0) { ^this };
		// A narrow vector knockout follows only the control's own marks. It
		// separates the ripple from the river where those paths cross without
		// restoring the rectangular castle-paper erasure removed in §9.
		Pen.strokeColor = palette[\ground];
		Pen.width = CQInk.strokes[\state];
		this.addRangeRipple(anchor, halves);
		Pen.stroke;
		Pen.strokeColor = palette[if(hover) { \line } { \whisper }]
			.copy.alpha_(if(hover) { 1.0 } { 0.72 });
		Pen.width = CQInk.strokes[\hairline];
		this.addRangeRipple(anchor, halves);
		Pen.stroke;
	}

	// One shared keel distinguishes this regime mark from the fm moon's free
	// single-cycle wave. The waveform crosses the keel; density alone carries
	// infra/balcium/audio, with no legacy i/b/a caption.
	*addRangeRipple { |anchor, halves|
		var width = CQNightMeasures.rangeRippleWidth;
		var height = CQNightMeasures.rangeRippleHeight;
		var left = anchor.x - (width * 0.5), base = anchor.y;
		var segment = width / halves;
		Pen.moveTo(Point(left, base));
		Pen.lineTo(Point(left + width, base));
		Pen.moveTo(Point(left, base));
		halves.do { |i|
			var x0 = left + (i * segment);
			var x1 = x0 + segment;
			var crest = base + (if(i.even) { -1 } { 1 } * height);
			Pen.curveTo(Point(x1, base),
				Point(x0 + (segment * 0.35), crest),
				Point(x0 + (segment * 0.65), crest));
		};
	}

	// the irregular hairline circle through the five castles, with the
	// fireflies that carry the loop's direction (k → k+1). Phase advances only
	// when telemetry moves — a still bay keeps a still river. The eye lives on
	// its own opaque layer (drawEye) so a storm repaint never re-inks the ring.
	*drawRing { |geometry, phase = 0, hover = false, skin = \night, chords|
		var palette = CQInk.palette(skin), origin = geometry[\ringBox].leftTop;
		Pen.translate(origin.x.neg, origin.y.neg);
		// the spesal coupling rides this layer because a castle's own 80 px box
		// cannot paint a chord to a body 300 px away — and its travelling lights
		// ride the river's own phase, so every moving mark in the ring advances
		// on the same telemetry frames (§16.9)
		this.drawSpesalChords(chords, phase, skin);
		// the river's own line is the §6 body handle: hovering it brightens it,
		// exactly as hovering the spiral brightens the winding surface (§4.5)
		Pen.strokeColor = palette[\whisper].copy
			.alpha_(if(hover) { 1.0 } { 0.7 });
		Pen.width = CQInk.strokes[\hairline];
		this.strokeSegments(geometry[\ringSegments]);
		Pen.fillColor = palette[\spark];
		10.do { |i|
			var r = CQNightMeasures.fireflyR;
			Pen.addOval(Rect.aboutPoint(
				CQNightGeometry.fireflyPoint(geometry, phase + (i / 10)), r, r));
		};
		Pen.fill;
	}

	// §16.6 the spesal coupling: which castle each castle reads, drawn on the
	// ring's own layer as inner chords in spesal green. Same fact as the Day
	// constellation and the same direction — the chord runs the way the signal
	// runs, from the castle being read into the one reading it (SPEC §2.2) —
	// but told in the night's own language:
	//
	// **direction is a travelling light, not an arrowhead.** A dot with a short
	// tail crosses the chord, exactly as the fireflies cross the river two
	// hundred pixels away, and for exactly the same reason: the sampling never
	// stops. An arrowhead would be the only mark in the bay that points instead
	// of moves, and it read as a flowchart. The light rides the **telemetry
	// frames** (§16.9), never a clock of its own, so a silent instrument holds
	// still water here too.
	//
	// **Track-and-hold is drawn broken, not doubled**: it follows its input
	// while the clock is high and freezes while it is low, so a link that is
	// connected half the time IS the mechanism. (Cross-ticks were unavailable —
	// barbs already mean inverso polarity on the cables, §16.8 — and the old
	// parallel line collided with everything at the far end.)
	//
	// Structure, not a quantity: nothing is drawn at rest, so this costs a
	// resting bay exactly zero Pen calls, and four strokes under the hand
	// however many chords are showing.
	*drawSpesalChords { |chords, phase = 0, skin = \night|
		var palette = CQInk.palette(skin), heads;
		var t = (phase * CQNightGeometry.chordLaps).mod(1);
		if(chords.isNil or: { chords.isEmpty }) { ^this };
		Pen.width = CQInk.strokes[\hairline];
		[false, true].do { |track|
			var group = chords.select { |chord| chord[\track] == track };
			if(group.notEmpty) {
				Pen.strokeColor = palette[\input].copy.alpha_(0.8);
				if(track) { Pen.lineDash = FloatArray[5, 5] };
				group.do { |chord|
					var path = chord[\path];
					Pen.moveTo(path[0]);
					Pen.curveTo(path[3], path[1], path[2]);
				};
				Pen.stroke;
				if(track) { Pen.lineDash = FloatArray[1, 0] };
			};
		};
		heads = chords.collect { |chord|
			CQNightGeometry.chordPoint(chord[\path], t)
		};
		Pen.strokeColor = palette[\input].copy.alpha_(0.5);
		chords.do { |chord, i|
			Pen.line(CQNightGeometry.chordPoint(chord[\path], (t - 0.12).max(0)),
				heads[i]);
		};
		Pen.stroke;
		Pen.fillColor = palette[\input];
		heads.do { |head|
			Pen.addOval(Rect.aboutPoint(head, CQNightMeasures.chordLightR,
				CQNightMeasures.chordLightR))
		};
		Pen.fill;
	}

	// the delay RAM as a coconut cross-section, drawn by the shell's own opaque
	// layer: spiral (the §6.1 winding surface), hold seal, the hold heart at
	// the innermost eye, the press-to-flip mouth chevron, the org stem and the
	// reel notch — a tick ACROSS the line at the speed's norm (inner eye =
	// slowest, mouth = fastest), the winding gesture's live hand; a static
	// hover (§4.5) lengthens and thickens it: the grab cue. It was a bead
	// until the G2 gate, and against the read head's bead it read as the same
	// object with two functions — 0.2 px of radius is not a difference a hand
	// can use. A speed is a *setting on a scale*, so it is drawn as a scale
	// mark in line ink; the only travelling body on this shell is the read
	// head, and bone spark ink now belongs to events alone (§16.2: the
	// difference is form first).
	// The read head is the comet: a bone dot at the loop's true address
	// (sparkNorm, nil until '/cq_phase' has reported — no telemetry is not
	// zero, §9) with a short tail trailing where it came from. Display only
	// since the G2 gate; the hand's skip lives on the mouth's own mark.
	*drawShell { |coco, origin, holdOn = false, flipMode = \forward,
		speedNorm = 0.5, hover = false, sparkNorm, sparkDir = 1,
		skipHover = false, skipFlash = false, struckNorm, struckTwinkle = false,
		rimHover = false, skin = \night|
		var palette = CQInk.palette(skin), notch, normal, reach, spark, tail, heart;
		Pen.translate(origin.x.neg, origin.y.neg);
		// the shell's rim is its §6 body handle: hovering it draws the outline
		// the double-click acts on, in whisper hairline so the danger-red hold
		// seal on the very same circle can never be mistaken for it
		if(rimHover) {
			Pen.strokeColor = palette[\whisper].copy.alpha_(0.55);
			Pen.width = CQInk.strokes[\hairline];
			Pen.addOval(Rect.aboutPoint(coco[\center],
				coco[\outerR], coco[\outerR]));
			Pen.stroke;
		};
		Pen.strokeColor = palette[\line].copy
			.alpha_(if(hover) { 1.0 } { 0.8 });
		Pen.width = CQInk.strokes[\normal];
		this.strokeSegments(coco[\segments]);
		if(holdOn) {
			Pen.strokeColor = palette[\danger].copy.alpha_(0.6);
			Pen.width = CQInk.strokes[\strong];
			Pen.addOval(Rect.aboutPoint(coco[\center], coco[\outerR], coco[\outerR]));
			Pen.stroke;
		};
		// the heart: hollow whisper eye while open, danger-red while sealed
		heart = CQNightMeasures.heartDotR;
		if(holdOn) {
			Pen.fillColor = palette[\danger];
			Pen.addOval(Rect.aboutPoint(coco[\center], heart, heart));
			Pen.fill;
			Pen.strokeColor = palette[\danger].copy.alpha_(0.4);
			Pen.addOval(Rect.aboutPoint(coco[\center], CQNightMeasures.heartSealR,
				CQNightMeasures.heartSealR));
			Pen.stroke;
		} {
			Pen.strokeColor = palette[\whisper];
			Pen.width = CQInk.strokes[\hairline];
			Pen.addOval(Rect.aboutPoint(coco[\center], heart, heart));
			Pen.stroke;
		};
		this.drawMouthChevron(coco[\chevron], flipMode, skin);
		// org stem from the second turn out to the hanging fruit jack
		Pen.strokeColor = palette[\organ].copy.alpha_(0.8);
		Pen.width = CQInk.strokes[\hairline];
		Pen.line(coco[\orgSpiralPoint], coco[\orgCenter]);
		Pen.stroke;
		// skip's stationary home above the mouth jack, the chevron's mirror
		// (§6.1). Momentary, so it carries §16.7's 120 ms ink flash — fired by
		// ANY skip (mark, `a/s`, code, pattern, matrix gate), exactly like the
		// Day button it mirrors, since both hang off the one `\action` notice.
		CQInk.drawGlyph(\skip, Rect.aboutPoint(coco[\skipMark],
			CQNightMeasures.skipGlyphR, CQNightMeasures.skipGlyphR),
			if(skipFlash) { palette[\spark] } {
				palette[if(skipHover) { \line } { \whisper }]
			});
		// the reel notch: a tick across the groove, never a bead
		notch = CQNightGeometry.spiralPoint(coco, speedNorm ? 0.5);
		normal = CQNightGeometry.spiralNormal(coco, speedNorm ? 0.5);
		reach = if(hover) { CQNightMeasures.notchHoverReach }
			{ CQNightMeasures.notchReach };
		Pen.strokeColor = palette[\line];
		Pen.width = CQInk.strokes[if(hover) { \strong } { \normal }];
		Pen.line(notch - (normal * reach), notch + (normal * reach));
		Pen.stroke;
		// the read head, drawn last so the moving comet passes over the notch
		sparkNorm !? {
			spark = CQNightGeometry.spiralPoint(coco, sparkNorm);
			// struckNorm is nil unless the head is being hammered — the same
			// nil-or-value idiom the whole night uses for "state not in force"
			if(struckNorm.notNil) {
				this.drawStruck(spark, struckNorm, struckTwinkle, skin);
			} {
				tail = CQNightGeometry.spiralPoint(coco,
					(sparkNorm - (sparkDir.sign * 0.01)).clip(0, 1));
				Pen.strokeColor = palette[\spark].copy.alpha_(0.45);
				Pen.width = CQInk.strokes[\hairline];
				Pen.line(tail, spark);
				Pen.stroke;
			};
			Pen.fillColor = palette[\spark];
			Pen.addOval(Rect.aboutPoint(spark, CQNightMeasures.cometR,
				CQNightMeasures.cometR));
			Pen.fill;
		};
	}

	// The read head while it is being hammered (§16.6). It wears the skip
	// glyph itself — one vocabulary, and the cause drawn where the effect is
	// (§1's proximity law). A mark of STATE, never of extent: the first try
	// drew the reach the head manages out of the eye, and that reach collapses
	// to nothing exactly when the regime is clearest, so the mark vanished
	// when it was most needed. The tail goes with it — a direction means
	// nothing under hammering.
	//
	// The trigger RATE is drawn as weight, not as motion. Above ~10 Hz the eye
	// fuses a flicker into a steady light (a real panel LED does the same), and
	// this surface only gets 5 Hz of telemetry (§10) — so any blink would be a
	// rate we invented, slower than the truth and decorative besides (§16.12).
	// A tachometer, not a strobe: the arms grow with the rate and a second,
	// diagonal pair opens past the midpoint. Four Pen lines, one stroke.
	// The mark also **twinkles**, alternating on every telemetry frame (§16.9's
	// read-head spark, ambient at the telemetry rate). That says "this state is
	// live", which the rate's weight alone cannot: a heavy static star and a
	// heavy frozen one look the same. It is driven BY the frames rather than by
	// a clock of its own, so a stalled server freezes the mark instead of
	// animating a lie about liveness — the moving version of §9's law that no
	// telemetry is not zero. It is not the trigger rate and never claims to be.
	*drawStruck { |center, rateNorm = 0, twinkle = false, skin = \night|
		var palette = CQInk.palette(skin);
		var norm = (rateNorm ? 0).clip(0, 1);
		var reach = CQNightMeasures.struckBase + (CQNightMeasures.struckRange * norm);
		var cross = reach * if(twinkle) { 0.74 } { 1.0 };
		Pen.strokeColor = palette[\spark].copy.alpha_(
			(0.55 + (0.35 * norm)) * if(twinkle) { 0.78 } { 1.0 });
		Pen.width = CQInk.strokes[\normal];
		Pen.line(center + Point(cross.neg, 0), center + Point(cross, 0));
		Pen.line(center + Point(0, cross.neg), center + Point(0, cross));
		if(norm > 0.5) {
			// past the midpoint the two pairs trade emphasis instead of merely
			// pulsing: the star scintillates, which reads as alive at a glance
			var diagonal = reach * 0.7 * if(twinkle) { 1.0 } { 0.72 };
			Pen.line(center + Point(diagonal.neg, diagonal.neg),
				center + Point(diagonal, diagonal));
			Pen.line(center + Point(diagonal, diagonal.neg),
				center + Point(diagonal.neg, diagonal));
		};
		Pen.stroke;
	}

	// §6.1 tide crescent: an open arc whose luminous fill is the value —
	// the night's only continuous hand, never a knob, never a number
	*drawCrescent { |center, norm = 0, family = \line, active = false,
		skin = \night|
		var palette = CQInk.palette(skin);
		var accent = palette[family] ?? { palette[\whisper] };
		var start = 0.75pi, sweep = 1.5pi, arcR = CQNightMeasures.crescentArcR;
		Pen.strokeColor = palette[\whisper].copy
			.alpha_(if(active) { 0.9 } { 0.55 });
		Pen.width = CQInk.strokes[\hairline];
		Pen.addArc(center, arcR, start, sweep);
		Pen.stroke;
		if(norm > 0.001) {
			Pen.strokeColor = accent.copy.alpha_(if(active) { 1.0 } { 0.85 });
			Pen.width = CQInk.strokes[\strong];
			Pen.addArc(center, arcR, start, sweep * norm.clip(0, 1));
			Pen.stroke;
		};
	}

	// §16.6 the sowing moon: shake and how much it sows, in ONE body. The lit
	// phase is the value — the tide crescents' law carried into the sky — so
	// there is no number and nothing that looks like a knob. A fuller moon sows
	// a denser patch, which is the mark teaching its own consequence.
	//
	// The moon fills from its LEFT limb and the terminator sweeps right, so a
	// rightward drag pushes the light ahead of the hand (decided with the user
	// 2026-07-22: the value's axis is horizontal, so the gesture is too, and
	// cue and action share one geometry — §4.5). Astronomically a waxing moon
	// lights from the right; this is an instrument, not an ephemeris.
	// The lit region is the left limb plus a half-ellipse whose x-radius runs
	// -R (new, nothing lit) through 0 (half) to +R (full disc): two quarter-arcs
	// each side, one fill — six Pen calls for any phase (§10).
	*drawSowingMoon { |center, radius, litNorm = 0, hover = false, flash = false,
		skin = \night|
		var palette = CQInk.palette(skin);
		var lit = (litNorm ? 0).clip(0, 1);
		var k = radius * 0.5523;
		var terminator = radius * ((2 * lit) - 1);
		var tk = terminator * 0.5523;
		var top = Point(center.x, center.y - radius);
		var bottom = Point(center.x, center.y + radius);
		if(flash) {
			Pen.fillColor = palette[\spark].copy.alpha_(0.28);
			Pen.addOval(Rect.aboutPoint(center,
				radius + CQNightMeasures.moonHalo,
				radius + CQNightMeasures.moonHalo));
			Pen.fill;
		};
		if(lit > 0.001) {
			Pen.fillColor = palette[\spark].copy
				.alpha_(if(flash) { 1.0 } { 0.82 });
			Pen.moveTo(top);
			Pen.curveTo(Point(center.x - radius, center.y),
				Point(center.x - k, center.y - radius),
				Point(center.x - radius, center.y - k));
			Pen.curveTo(bottom,
				Point(center.x - radius, center.y + k),
				Point(center.x - k, center.y + radius));
			Pen.curveTo(Point(center.x + terminator, center.y),
				Point(center.x + tk, center.y + radius),
				Point(center.x + terminator, center.y + k));
			Pen.curveTo(top,
				Point(center.x + terminator, center.y - k),
				Point(center.x + tk, center.y - radius));
			Pen.fill;
		};
		// the drawn rim is the whole body: what the press takes and what the
		// hover brightens are one geometry (§4.5), so a dark new moon is still
		// as findable as a full one
		Pen.strokeColor = palette[if(hover) { \line } { \whisper }].copy
			.alpha_(if(hover) { 1.0 } { 0.7 });
		Pen.width = CQInk.strokes[if(hover) { \normal } { \hairline }];
		Pen.addOval(Rect.aboutPoint(center, radius, radius));
		Pen.stroke;
	}

	// ---- the tide pool (§7.1, §9) ----
	// The pool body was removed in the landscape pass of 2026-07-27. The tide
	// now belongs to the whole bay: four memory seeds are its only resting ink.
	// Shift focus gives those seeds a quiet breath; an actual voyage reveals
	// four ≤8 % pulls and the hand's cross. No rectangle, shore, ambient wave or
	// sparkle is ever drawn. A seed-to-seed breed shows one temporary relation
	// line, then disappears with the release.
	*legacyDrawTideField { |almanac, stamped, drag, focused = false, hover,
		skin = \night|
		var palette = CQInk.palette(skin), tide = almanac[\compass];
		var stampHover = hover.isKindOf(SequenceableCollection);
		var reach = min(tide.width, tide.height) * 0.56;
		var cross, target;
		if(drag.notNil and: { drag[\pos].notNil }) {
			cross = CQNightGeometry.compassPointAt(almanac, drag[\pos])
		};
		if(drag.notNil and: { drag[\pull].notNil }) {
			almanac[\stamps].do { |corner, c|
				var strength = (drag[\pull][c] ? 0).clip(0, 1);
				if(strength > 0.02) {
					var angle = CQGuiNightTouch.angleAt(corner, cross);
					4.do { |i|
						var fraction = 0.22 + (i * 0.18);
						Pen.strokeColor = palette[\spark].copy.alpha_(
							strength * 0.08 * (1 - (i * 0.18)));
						Pen.width = CQInk.strokes[\hairline];
						Pen.addArc(corner, reach * fraction,
							angle - 0.44, 0.88);
						Pen.stroke;
					};
				};
			};
		};
		almanac[\stamps].do { |corner, c|
			var isHover = stampHover and: { hover[1] == c };
			var angle = CQGuiNightTouch.angleAt(corner, tide.center);
			var inward = CQNightGeometry.polar(corner, 4.2, angle);
			var outward = CQNightGeometry.polar(corner, 4.2, angle + pi);
			var sideA = CQNightGeometry.polar(corner, 3.0, angle + (0.5pi));
			var sideB = CQNightGeometry.polar(corner, 3.0, angle - (0.5pi));
			var seedInk;
			Pen.width = CQInk.strokes[\hairline];
			Pen.moveTo(inward);
			Pen.curveTo(outward, sideA, sideA);
			Pen.curveTo(inward, sideB, sideB);
			if(stamped[c] == true) {
				seedInk = palette[\line].copy.alpha_(
					if(isHover or: { focused }) { 1.0 } { 0.82 });
				Pen.fillColor = seedInk;
				Pen.strokeColor = seedInk;
				Pen.fillStroke;
			} {
				Pen.strokeColor = palette[\whisper].copy.alpha_(
					if(isHover or: { focused }) { 0.9 } { 0.48 });
				Pen.stroke;
			};
			if(isHover or: { focused }) {
				Pen.strokeColor = palette[\line].copy
					.alpha_(if(isHover) { 0.58 } { 0.24 });
				Pen.width = CQInk.strokes[\hairline];
				Pen.addOval(Rect.aboutPoint(corner,
					if(isHover) { 6 } { 5 }, if(isHover) { 6 } { 5 }));
				Pen.stroke;
			};
		};
		if(drag.notNil and: { drag[\pos].notNil }) {
			Pen.strokeColor = palette[\line];
			Pen.width = CQInk.strokes[\hairline];
			Pen.line(cross + Point(-4, 0), cross + Point(4, 0));
			Pen.line(cross + Point(0, -4), cross + Point(0, 4));
			Pen.stroke;
		};
		if(drag.notNil and: { drag[\breedFrom].notNil }) {
			cross = almanac[\stamps][drag[\breedFrom]];
			target = drag[\cursor];
			Pen.strokeColor = palette[\line].copy.alpha_(0.42);
			Pen.width = CQInk.strokes[\hairline];
			Pen.moveTo(cross);
			Pen.curveTo(target,
				cross + ((target - cross) * 0.36) + Point(0, -8),
				cross + ((target - cross) * 0.72) + Point(0, 8));
			Pen.stroke;
			Pen.addOval(Rect.aboutPoint(cross, 6, 6));
			Pen.addOval(Rect.aboutPoint(target, 4, 4));
			Pen.stroke;
		};
	}

	// §6.1 Dolby oyster: posture, never colour, is the enum. Off breathes
	// open; gate nearly closes; duck closes and receives a short stroke from
	// the partner channel that keys it. This is setting ink, not invented
	// telemetry, so it never blinks.
	*drawDolbyOyster { |center, mode = \off, partnerDirection = 1,
		hover = false, skin = \night|
		var palette = CQInk.palette(skin);
		var width = CQNightMeasures.dolbyOysterWidth;
		var gap = switch(mode,
			\gate, { CQNightMeasures.dolbyOysterGateGap },
			\duck, { 0 },
			{ CQNightMeasures.dolbyOysterOpenGap });
		var bow = if(mode == \off) { 3.5 } { 2.2 };
		var left = center + Point(width.neg, 0);
		var right = center + Point(width, 0);
		Pen.strokeColor = palette[\whisper].copy.alpha_(if(hover) { 0.95 } { 0.62 });
		Pen.width = if(hover) { CQInk.strokes[\normal] } {
			CQInk.strokes[\hairline]
		};
		Pen.moveTo(left + Point(0, gap.neg));
		Pen.curveTo(right + Point(0, gap.neg),
			center + Point(width * -0.45, gap.neg - bow),
			center + Point(width * 0.45, gap.neg - bow));
		Pen.moveTo(left + Point(0, gap));
		Pen.curveTo(right + Point(0, gap),
			center + Point(width * -0.45, gap + bow),
			center + Point(width * 0.45, gap + bow));
		if(mode == \duck) {
			var direction = if(partnerDirection < 0) { -1 } { 1 };
			// The arrival lives inside the 30 px owner, entering across the
			// partner-facing lip instead of clipping outside opaque paper.
			var outer = center + Point(direction * (width - 1), 0);
			var inner = center + Point(direction * (width - 6), 0);
			Pen.moveTo(outer);
			Pen.lineTo(inner);
			Pen.moveTo(inner + Point(direction * 3, -2.5));
			Pen.lineTo(inner);
			Pen.lineTo(inner + Point(direction * 3, 2.5));
		};
		Pen.stroke;
	}

	// G3 drew this as a round-cornered hairline square — the app's one
	// widget-shaped body at night, and it always looked like one. §9 made it a
	// **pool**: a seeded, wobbled closed shore (the castle blob's cousin,
	// §16.3's own organism language) hanging below the sowing moon like its
	// widening reflection on the water. Nothing about the tide changed: the
	// same four memories, the same corner seeds — now seated on the shore at
	// the four diagonal winds, where the disc→square mapping puts the
	// bilinear's corners — the same cross while a voyage is poured, the same
	// four faint pulls (≤ 8 % alpha) that exist only under the hand, and the
	// same weather glyphs, scattered onto the shores instead of lined up in a
	// row. No numbers, no labels: a stamped memory is a seated seed and empty
	// water is a hollow ring. At rest this layer asks for no repaints at all —
	// nothing on it moves without the hand.
	//
	// drag = nil, or (pos: Point(0..1), pull: [4 bilinear weights])
	// hover = nil | \pad | [\stamp, i] | \grow | \breed | \wither
	// held  = nil | \grow | \wither      flash = the same, for 120 ms
	*drawCompass { |almanac, stamped, drag, breedArmed = false, breedFirst,
		hover, held, flash, skin = \night|
		var palette = CQInk.palette(skin), pool = almanac[\pool];
		var stampHover = hover.isKindOf(SequenceableCollection);
		var reach = max(pool[\rx], pool[\ry]) * 0.9;
		var cross;
		// the four pulls, only while the hand is pouring, radiating from the
		// shores the memories sit on (§16.6: if a gradient is visible as a
		// gradient, it is too strong)
		drag !? {
			almanac[\stamps].do { |corner, c|
				var strength = (drag[\pull][c] ? 0).clip(0, 1);
				if(strength > 0.02) {
					Pen.addOval(Rect.aboutPoint(corner, reach, reach));
					Pen.fillRadialGradient(corner, corner, 0, reach,
						palette[\spark].copy.alpha_(strength * 0.08),
						palette[\spark].copy.alpha_(0));
				};
			};
		};
		Pen.strokeColor = palette[\whisper].copy
			.alpha_(if(hover == \pad or: { drag.notNil }) { 0.9 } { 0.55 });
		Pen.width = CQInk.strokes[\hairline];
		this.strokeSegments(pool[\segments]);
		// corner seeds: a stamped memory is a seated seed in line ink, empty
		// water a hollow whisper ring — settings, so never spark ink (§16.6)
		almanac[\stamps].do { |corner, c|
			var isHover = stampHover and: { hover[1] == c };
			if(stamped[c] == true) {
				Pen.fillColor = palette[\line].copy
					.alpha_(if(isHover) { 1.0 } { 0.85 });
				Pen.addOval(Rect.aboutPoint(corner, 3, 3));
				Pen.fill;
			} {
				Pen.strokeColor = palette[\whisper].copy
					.alpha_(if(isHover) { 0.9 } { 0.5 });
				Pen.width = CQInk.strokes[\hairline];
				Pen.addOval(Rect.aboutPoint(corner, 2.5, 2.5));
				Pen.stroke;
			};
			// the chosen parent while breeding wears a quiet ring; the hover
			// echo is the same ring fainter — cue and act share one geometry
			if((breedFirst == c) or: { isHover }) {
				Pen.strokeColor = palette[\line].copy
					.alpha_(if(breedFirst == c) { 0.9 } { 0.5 });
				Pen.width = CQInk.strokes[\hairline];
				Pen.addOval(Rect.aboutPoint(corner, 6, 6));
				Pen.stroke;
			};
		};
		// the voyage's own cross, at the place on the pool being poured — read
		// through the same mapping the pour itself speaks, never re-derived
		drag !? {
			cross = CQNightGeometry.compassPointAt(almanac, drag[\pos]);
			Pen.strokeColor = palette[\line];
			Pen.width = CQInk.strokes[\hairline];
			Pen.line(cross + Point(-4, 0), cross + Point(4, 0));
			Pen.line(cross + Point(0, -4), cross + Point(0, 4));
			Pen.stroke;
		};
		// only the graft lives on the water: it takes its two parents as seed
		// taps, so it may not stand a bay away from them. The rain and the sun
		// went to the sky with the moon, where whole-bay weather belongs (§9).
		this.drawWeatherMark(\breed, almanac[\weather][\breed],
			hover == \breed, breedArmed, flash == \breed, breedArmed, skin);
	}

	// §7.2/§7.3 in the almanac's own pictures: the rain (grow), the graft
	// (breed — two parents sharing flesh) and the sun (wither: in a farmer's
	// almanac the sun is the drought). Whisper at rest, line ink under the
	// hand or while held; the 120 ms flash is each seed actually landing —
	// §16.7's momentary law at the pull's current rate, so a held rain visibly
	// thickens as the hand moves away. An armed graft keeps a halo until its
	// parents are chosen.
	*drawWeatherMark { |name, anchor, hover = false, active = false,
		flash = false, halo = false, skin = \night, intensity = 0|
		var palette = CQInk.palette(skin);
		var force = (intensity ? 0).asFloat.clip(0, 1);
		var ink = if(flash) { palette[\spark] } {
			palette[if(hover or: { active }) { \line } { \whisper }]
		};
		Pen.strokeColor = ink;
		Pen.width = CQInk.strokes[\hairline];
		switch(name,
			\grow, {
				var drops = 1 + (force * 3).round.asInteger;
				var span = 5 + (force * 2);
				var slant = 1 + force;
				drops.do { |i|
					var x = if(drops == 1) { 0 } {
						(((i / (drops - 1)) * 2) - 1) * span
					};
					Pen.line(anchor + Point(x, -4 - force),
						anchor + Point(x - slant, 3 + (force * 2)));
				};
			},
			\breed, {
				Pen.addOval(Rect.aboutPoint(anchor + Point(-2.5, 0), 3.5, 3.5));
				Pen.addOval(Rect.aboutPoint(anchor + Point(2.5, 0), 3.5, 3.5));
			},
			\wither, {
				var rays = 4 + (force * 4).round.asInteger;
				Pen.addOval(Rect.aboutPoint(anchor,
					2.5 + (force * 0.5), 2.5 + (force * 0.5)));
				rays.do { |i|
					var angle = (i / rays * 2pi) + 0.25pi;
					Pen.line(CQNightGeometry.polar(anchor, 4, angle),
						CQNightGeometry.polar(anchor, 6.5 + (force * 1.5), angle));
				};
			}
		);
		Pen.stroke;
		if(halo) {
			Pen.strokeColor = ink.copy.alpha_(0.45);
			Pen.addOval(Rect.aboutPoint(anchor, 9, 9));
			Pen.stroke;
		};
	}

	// fixed ray scatter for the storm iris — seeded once by hand, never
	// randomized at draw time, so a still storm keeps a still eye (§16.11)
	*irisJitters {
		^#[0.34, -0.62, 0.11, 0.83, -0.27, 0.55,
			-0.91, 0.07, -0.44, 0.68, -0.15, 0.39]
	}

	// §6.1 the ring's eye — the storm iris. Its ink is the actual weather:
	// the pupil's aperture is qFreq, and the twelve rays scatter and reach
	// with qChaos (even, short corona = calm; ragged reach = storm). Drawn by
	// its own opaque layer so a storm repaint never re-inks the ring (§10).
	*drawEye { |center, active = false, freqNorm = 0.5, chaosNorm = 0.15,
		skin = \night|
		var palette = CQInk.palette(skin), jitters = this.irisJitters;
		var pupil = CQNightMeasures.pupilMin
			+ (freqNorm.clip(0, 1) * CQNightMeasures.pupilRange);
		var chaos = chaosNorm.clip(0, 1);
		Pen.fillColor = palette[\spark].copy.alpha_(if(active) { 0.55 } { 0.25 });
		Pen.addOval(Rect.aboutPoint(center, pupil, pupil));
		Pen.fill;
		Pen.strokeColor = palette[\spark].copy.alpha_(if(active) { 1.0 } { 0.7 });
		Pen.width = CQInk.strokes[\hairline];
		Pen.addOval(Rect.aboutPoint(center, pupil + 1.5, pupil + 1.5));
		Pen.stroke;
		Pen.strokeColor = palette[\spark].copy.alpha_(if(active) { 0.8 } { 0.5 });
		12.do { |i|
			var angle = (i / 12 * 2pi) + (jitters[i] * chaos * 0.45);
			var innerR = pupil + CQNightMeasures.irisGap;
			var length = CQNightMeasures.irisRayBase
				+ (chaos * (CQNightMeasures.irisRayChaos
					+ (jitters.wrapAt(i + 5).abs * CQNightMeasures.irisRayJitter)));
			Pen.line(CQNightGeometry.polar(center, innerR, angle),
				CQNightGeometry.polar(center, innerR + length, angle));
		};
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
	*drawBird { |bird, origin, active = false, flash = false, skin = \night|
		var palette = CQInk.palette(skin), c = bird[\center], span = bird[\span];
		var color = if(active or: { flash }) { palette[\spark] } { palette[\line] };
		origin !? { Pen.translate(origin.x.neg, origin.y.neg) };
		if(flash) {
			Pen.fillColor = palette[\spark].copy.alpha_(0.13);
			Pen.addOval(Rect.aboutPoint(c, CQNightMeasures.birdBodyR + 7,
				CQNightMeasures.birdBodyR + 7));
			Pen.fill;
		};
		Pen.strokeColor = color;
		Pen.width = CQInk.strokes[
			if(flash) { \state } { if(active) { \strong } { \normal } }];
		Pen.moveTo(Point(c.x - (span * 0.5), c.y - (span * 0.12)));
		Pen.curveTo(c, Point(c.x - (span * 0.34), c.y - (span * 0.4)),
			Point(c.x - (span * 0.1), c.y - (span * 0.28)));
		Pen.stroke;
		Pen.moveTo(Point(c.x + (span * 0.5), c.y - (span * 0.12)));
		Pen.curveTo(c, Point(c.x + (span * 0.34), c.y - (span * 0.4)),
			Point(c.x + (span * 0.1), c.y - (span * 0.28)));
		Pen.stroke;
		Pen.fillColor = color;
		Pen.addOval(Rect.aboutPoint(c, CQNightMeasures.birdBodyR,
			CQNightMeasures.birdBodyR));
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

	// the coco mouth chevron: read direction below the spiral's mouth,
	// reversed by flip (down = forward, up = flipped — mirrored the same on
	// both cocos). Centered on its anchor so neither the shell box edge nor
	// the mouth jack's opaque view ever swallows it.
	// Three modes, not two (amended 2026-07-21 after the G2.5 review): once a
	// flip banana drives the gate faster than an arrow can be read, a single
	// arrowhead can only freeze or lie, so the mark becomes §16.5's opposed
	// chevrons — *shuttling*, the regime instead of the value. Which regime is
	// decided on the server, at audio rate (§9), never by guessing here.
	*drawMouthChevron { |anchor, mode = \forward, skin = \night|
		var palette = CQInk.palette(skin);
		var reach = CQNightMeasures.chevronReach;
		var rise = CQNightMeasures.chevronRise;
		var shuttleRise = CQNightMeasures.chevronShuttleRise;
		var head = { |dy|
			Pen.line(anchor + Point(reach.neg, dy.neg), anchor + Point(0, dy));
			Pen.line(anchor + Point(0, dy), anchor + Point(reach, dy.neg));
		};
		Pen.strokeColor = palette[\cas];
		Pen.width = CQInk.strokes[\hairline];
		switch(mode,
			\shuttle, { head.value(shuttleRise); head.value(shuttleRise.neg) },
			\flipped, { head.value(rise.neg) },
			{ head.value(rise) }
		);
		Pen.stroke;
	}

	// the root's last stretch, redrawn inside the tip jack's own opaque view so
	// the line reaches its dot instead of vanishing under the box (§16.6).
	// The reach is the box's own half-diagonal, so however big the view grows
	// the stub still crosses it from any arrival angle and Qt clips the rest
	// (rake 12 — derived from the box, never re-tuned by hand).
	// Four resting seeds imply the full field. Vacancy is an open husk; stored
	// silence is a closed dark kernel; a living memory is luminous in proportion
	// to its cable density. Occupancy and musical emptiness remain distinct.
	*drawTideField { |almanac, memories, focused = false, hover, skin = \night|
		var palette = CQInk.palette(skin), tide = almanac[\compass];
		var stampHover = hover.isKindOf(SequenceableCollection);
		almanac[\stamps].do { |corner, c|
			var isHover = stampHover and: { hover[1] == c };
			var angle = CQGuiNightTouch.angleAt(corner, tide.center);
			var density = memories[c];
			var fullness, seedInk;
			if(density.isNil) {
				// Vacant memory: an unmistakably open inward-facing husk.
				Pen.strokeColor = palette[\whisper].copy.alpha_(
					if(isHover or: { focused }) { 0.98 } { 0.72 });
				Pen.width = CQInk.strokes[
					if(isHover or: { focused }) { \normal } { \hairline }];
				this.prAddSeed(corner, angle, false);
				Pen.stroke;
			} {
				if(density <= 0) {
					// Occupied silence: closed, dark, with one dormant pale vein.
					Pen.fillColor = palette[\ground];
					Pen.strokeColor = palette[\line].copy.alpha_(
						if(isHover or: { focused }) { 0.98 } { 0.7 });
					Pen.width = CQInk.strokes[
						if(isHover or: { focused }) { \normal } { \hairline }];
					this.prAddSeed(corner, angle, true);
					Pen.fillStroke;
					Pen.strokeColor = palette[\whisper].copy.alpha_(
						if(isHover or: { focused }) { 0.72 } { 0.42 });
					Pen.width = CQInk.strokes[\hairline];
					Pen.line(CQNightGeometry.polar(corner,
						CQNightMeasures.seedLength * 0.34, angle + pi),
						CQNightGeometry.polar(corner,
							CQNightMeasures.seedLength * 0.34, angle));
					Pen.stroke;
				} {
					// Living memory: density changes the kernel's stored light,
					// while even a sparse patch remains visibly occupied.
					fullness = (density / CQGui.fullMoonDensity)
						.clip(0, 1).sqrt;
					seedInk = palette[\line].copy.alpha_(
						(0.55 + (fullness * 0.4)
							+ (if(isHover or: { focused }) { 0.08 } { 0 }))
							.clip(0, 1));
					Pen.fillColor = seedInk;
					Pen.strokeColor = seedInk;
					Pen.width = CQInk.strokes[\hairline];
					this.prAddSeed(corner, angle, true);
					Pen.fillStroke;
					Pen.strokeColor = palette[\ground].copy.alpha_(0.82);
					Pen.line(CQNightGeometry.polar(corner,
						CQNightMeasures.seedLength * 0.58, angle + pi),
						CQNightGeometry.polar(corner,
							CQNightMeasures.seedLength * 0.58, angle));
					Pen.stroke;
				};
			};
			if(isHover or: { focused }) {
				Pen.strokeColor = palette[\line].copy
					.alpha_(if(isHover) { 0.58 } { 0.24 });
				Pen.width = CQInk.strokes[\hairline];
				Pen.addOval(Rect.aboutPoint(corner,
					if(isHover) { CQNightMeasures.seedHalo + 2 }
						{ CQNightMeasures.seedHalo },
					if(isHover) { CQNightMeasures.seedHalo + 2 }
						{ CQNightMeasures.seedHalo }));
				Pen.stroke;
			};
		};
	}

	*prAddSeed { |center, angle, closed = true|
		var forward = Point(cos(angle), sin(angle));
		var normal = Point(forward.y.neg, forward.x);
		var length = CQNightMeasures.seedLength;
		var width = CQNightMeasures.seedWidth;
		var inward = center + (forward * length);
		var outward = center - (forward * (length * 0.72));
		var sideA = center + (normal * width);
		var sideB = center - (normal * width);
		if(closed) {
			Pen.moveTo(inward);
			Pen.curveTo(outward, sideA, sideA);
			Pen.curveTo(inward, sideB, sideB);
		} {
			Pen.moveTo(inward - (forward * 1.8) + (normal * 1.3));
			Pen.curveTo(outward, sideA, sideA);
			Pen.curveTo(inward - (forward * 1.8) - (normal * 1.3),
				sideB, sideB);
		};
	}

	// Voyage and breeding ink live on the topmost hit-transparent stage layer.
	// A valid breed target catches the thread at its centre and opens a double
	// bloom: an explicit visual promise that releasing here will make a child.
	*drawTideGesture { |almanac, drag, skin = \night|
		var palette = CQInk.palette(skin), tide = almanac[\compass];
		var reach = min(tide.width, tide.height) * 0.56;
		var cross, target, targetIndex, source;
		if(drag.isNil) { ^this };
		if(drag[\pos].notNil) {
			cross = CQNightGeometry.compassPointAt(almanac, drag[\pos])
		};
		if(drag[\pull].notNil and: { cross.notNil }) {
			almanac[\stamps].do { |corner, c|
				var strength = (drag[\pull][c] ? 0).clip(0, 1);
				if(strength > 0.02) {
					var angle = CQGuiNightTouch.angleAt(corner, cross);
					4.do { |i|
						var fraction = 0.22 + (i * 0.18);
						Pen.strokeColor = palette[\spark].copy.alpha_(
							strength * 0.08 * (1 - (i * 0.18)));
						Pen.width = CQInk.strokes[\hairline];
						Pen.addArc(corner, reach * fraction,
							angle - 0.44, 0.88);
						Pen.stroke;
					};
				};
			};
		};
		if(cross.notNil) {
			Pen.strokeColor = palette[\line];
			Pen.width = CQInk.strokes[\hairline];
			Pen.line(cross + Point(-4, 0), cross + Point(4, 0));
			Pen.line(cross + Point(0, -4), cross + Point(0, 4));
			Pen.stroke;
		};
		if(drag[\breedFrom].notNil) {
			source = almanac[\stamps][drag[\breedFrom]];
			targetIndex = drag[\breedTarget];
			target = if(targetIndex.notNil) {
				almanac[\stamps][targetIndex]
			} {
				drag[\cursor]
			};
			Pen.strokeColor = palette[\line].copy
				.alpha_(if(targetIndex.notNil) { 0.88 } { 0.42 });
			Pen.width = CQInk.strokes[
				if(targetIndex.notNil) { \normal } { \hairline }];
			Pen.moveTo(source);
			Pen.curveTo(target,
				source + ((target - source) * 0.36) + Point(0, -8),
				source + ((target - source) * 0.72) + Point(0, 8));
			Pen.stroke;
			Pen.strokeColor = palette[\line].copy.alpha_(0.7);
			Pen.width = CQInk.strokes[\hairline];
			Pen.addOval(Rect.aboutPoint(source, 8, 8));
			Pen.stroke;
			if(targetIndex.notNil) {
				Pen.fillColor = palette[\fm].copy.alpha_(0.12);
				Pen.addOval(Rect.aboutPoint(target,
					CQNightMeasures.seedHalo + 5,
					CQNightMeasures.seedHalo + 5));
				Pen.fill;
				Pen.strokeColor = palette[\fm].copy.alpha_(0.9);
				Pen.width = CQInk.strokes[\normal];
				Pen.addOval(Rect.aboutPoint(target,
					CQNightMeasures.seedHalo,
					CQNightMeasures.seedHalo));
				Pen.addOval(Rect.aboutPoint(target,
					CQNightMeasures.seedHalo + 5,
					CQNightMeasures.seedHalo + 5));
				Pen.stroke;
			} {
				Pen.addOval(Rect.aboutPoint(target, 4, 4));
				Pen.stroke;
			};
		};
	}

	// Closed gill = ordinary cable grammar. Open, ribbed gill = the cords sink
	// behind the field and the water becomes playable. The bay appears to take
	// a breath; this is not a disguised checkbox.
	*drawTideGate { |center, origin, active = false, hover = false, skin = \night|
		var palette = CQInk.palette(skin);
		var span = CQNightMeasures.tideGateSpan;
		var rise = if(active) { CQNightMeasures.tideGateRise } { 3.5 };
		var left = center + Point(span.neg, 0);
		var right = center + Point(span, 0);
		Pen.translate(origin.x.neg, origin.y.neg);
		if(hover or: { active }) {
			Pen.strokeColor = palette[if(active) { \fm } { \line }]
				.copy.alpha_(if(hover) { 0.55 } { 0.28 });
			Pen.width = CQInk.strokes[\hairline];
			Pen.addOval(Rect.aboutPoint(center,
				CQNightMeasures.tideGateHit,
				CQNightMeasures.tideGateHit * 0.72));
			Pen.stroke;
		};
		Pen.strokeColor = palette[if(active) { \fm } {
			if(hover) { \line } { \whisper }
		}].copy.alpha_(if(active or: { hover }) { 0.95 } { 0.66 });
		Pen.width = CQInk.strokes[if(active) { \normal } { \hairline }];
		Pen.moveTo(left);
		Pen.curveTo(right,
			center + Point(span * -0.45, rise.neg),
			center + Point(span * 0.45, rise.neg));
		Pen.moveTo(left);
		Pen.curveTo(right,
			center + Point(span * -0.45, rise),
			center + Point(span * 0.45, rise));
		Pen.stroke;
		Pen.strokeColor = palette[if(active) { \fm } { \whisper }]
			.copy.alpha_(if(active) { 0.62 } { 0.36 });
		Pen.width = CQInk.strokes[\hairline];
		[-7, 0, 7].do { |x|
			var rib = if(active) { 4.2 } { 1.8 };
			Pen.line(center + Point(x, rib.neg),
				center + Point(x, rib));
		};
		Pen.stroke;
	}

	*drawRootStub { |anchor, arrival, skin = \night|
		var palette = CQInk.palette(skin), direction = arrival ? Point(0, -1);
		var reach = (CQNightMeasures.jackExtent * 0.5 * 2.sqrt) + 1;
		Pen.strokeColor = palette[\line].copy.alpha_(0.75);
		Pen.width = CQInk.strokes[\normal];
		Pen.line(anchor - (direction * reach), anchor);
		Pen.stroke;
	}

	// The org jack owns an opaque 30 px box over the shell. Repaint the exact
	// spiral curve and its radial fruit stem inside that clipped view before
	// the yellow jack is drawn; a generic vertical stub lies about both.
	*drawOrganUnderlay { |coco, anchor, skin = \night|
		var palette = CQInk.palette(skin);
		var delta = anchor - coco[\orgCenter];
		var segments = coco[\segments].collect { |segment|
			segment.collect { |point| point + delta }
		};
		Pen.strokeColor = palette[\line].copy.alpha_(0.8);
		Pen.width = CQInk.strokes[\normal];
		this.strokeSegments(segments);
		Pen.strokeColor = palette[\organ].copy.alpha_(0.8);
		Pen.width = CQInk.strokes[\hairline];
		Pen.line(coco[\orgSpiralPoint] + delta, anchor);
		Pen.stroke;
	}
}
