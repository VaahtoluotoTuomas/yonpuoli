// CQNightGeometry — pure layout math for the Night map (GUI_SPEC §16.6, §15).
// No Qt and no instrument state: a stage size goes in, every body and endpoint
// position comes out, so the topology truths — ring order and direction, cocos
// left/right, roots below, birds above — are unit-testable without a window.
// Every irregularity is seeded (§16.11): the same size always returns the same
// hand-drawn archipelago, and a redraw never boils.
//
// The measures themselves live next door in `CQNightMeasures` — this class is
// the math that composes them (NIGHT_STANDALONE_PLAN §8). Nothing here is a
// size literal; if a number belongs to a body or a hit target, it belongs there.

CQNightGeometry {

	// coco spiral turns (§16.6)
	*turns { ^2.5 }

	// range letters shown in whisper at the castle's 6 o'clock (§16.6)
	*rangeLetter { |range|
		^switch(range, \infra, { "i" }, \balcium, { "b" }, \audio, { "a" }, { "" })
	}

	// mic sprout (§16.6): display only, never a jack. \hidden when no mic is
	// configured, \dormant while micGain sits at its safe 0, \live once audible.
	*sproutState { |micConfigured = false, micGain = 0|
		if(micConfigured != true) { ^\hidden };
		^if((micGain ? 0) > 0) { \live } { \dormant }
	}

	// clock-face angle in radians (12 o'clock = up, clockwise — §16.6 positions)
	*clockAngle { |clock| ^((clock / 12) * 2pi) - 0.5pi }

	*polar { |center, radius, angle|
		^Point(center.x + (cos(angle) * radius), center.y + (sin(angle) * radius))
	}

	// closed Catmull-Rom through the given points as cubic segments
	// [[p0, c1, c2, p1], ...] — one smooth stroke through five castles or six
	// blob anchors, because a drawing's price is its Pen-call count (§10)
	*smoothClosedSegments { |points|
		^points.collect { |p1, i|
			var p0 = points.wrapAt(i - 1);
			var p2 = points.wrapAt(i + 1);
			var p3 = points.wrapAt(i + 2);
			[p1, p1 + ((p2 - p0) / 6), p2 - ((p3 - p1) / 6), p2]
		}
	}

	// sample a bezier segment list into a polyline (firefly travel)
	*sampleSegments { |segments, perSegment = 16|
		var points = List.new;
		segments.do { |seg|
			perSegment.do { |i|
				var t = i / perSegment, u = 1 - t;
				points.add((seg[0] * (u * u * u)) + (seg[1] * (3 * u * u * t))
					+ (seg[2] * (3 * u * t * t)) + (seg[3] * (t * t * t)));
			};
		};
		^points.asArray
	}

	// a point riding the ring: phase 0..1 travels castle 1 → 2 → 3 → 4 → 5 → 1,
	// the direction of the LocalIn loop and the \classic FM ring (cas k → k+1)
	*fireflyPoint { |geometry, phase|
		var samples = geometry[\ringSamples], scaled, index, frac, a, b;
		scaled = phase.mod(1) * samples.size;
		index = scaled.floor.asInteger;
		frac = scaled - index;
		a = samples.wrapAt(index);
		b = samples.wrapAt(index + 1);
		^Point(a.x + ((b.x - a.x) * frac), a.y + ((b.y - a.y) * frac))
	}

	// Archimedean spiral (§16.6) as quarter-turn cubics ending at the mouth on
	// the outer rim; winding -1 mirrors coco2 against coco1
	*spiralSegments { |center, innerR, outerR, mouthAngle = 0, winding = 1|
		var quarters = (this.turns * 4).round.asInteger;
		var radiusAt = { |i| innerR + ((outerR - innerR) * (i / quarters)) };
		var pointAt = { |i|
			this.polar(center, radiusAt.value(i),
				mouthAngle - (winding * (quarters - i) * 0.5pi))
		};
		var tangentAt = { |i|
			var angle = mouthAngle - (winding * (quarters - i) * 0.5pi);
			Point(sin(angle).neg, cos(angle)) * winding
		};
		^quarters.collect { |i|
			var p0 = pointAt.value(i), p1 = pointAt.value(i + 1);
			[p0, p0 + (tangentAt.value(i) * (radiusAt.value(i) * 0.5523)),
				p1 - (tangentAt.value(i + 1) * (radiusAt.value(i + 1) * 0.5523)), p1]
		}
	}

	*prSeeded { |seed, func|
		var saved = thisThread.randData, result;
		thisThread.randSeed = seed.asInteger;
		result = func.value;
		thisThread.randData = saved;
		^result
	}

	*distance { |a, b|
		^sqrt((b.x - a.x).squared + (b.y - a.y).squared)
	}

	*prPointSegmentDistance { |point, a, b|
		var dx, dy, lengthSq, t, projection;
		dx = b.x - a.x;
		dy = b.y - a.y;
		lengthSq = dx.squared + dy.squared;
		if(lengthSq <= 0.0001) { ^this.distance(point, a) };
		t = ((((point.x - a.x) * dx) + ((point.y - a.y) * dy)) / lengthSq)
			.clip(0, 1);
		projection = Point(a.x + (t * dx), a.y + (t * dy));
		^this.distance(point, projection)
	}

	// ---- §6.1 touch hit tests (pure) ----

	// distance from a point to the spiral's drawn line (the winding surface)
	*spiralDistance { |coco, point|
		var samples = coco[\spiralSamples], best = inf;
		samples.doAdjacentPairs { |a, b|
			best = min(best, this.prPointSegmentDistance(point, a, b));
		};
		^best
	}

	// a point riding the spiral's line: norm 0 = the innermost eye (slowest),
	// norm 1 = the mouth (fastest) — the reel pearl's home for a speed value
	*spiralPoint { |coco, norm|
		var samples = coco[\spiralSamples], scaled, index, frac, a, b;
		scaled = norm.clip(0, 1) * (samples.size - 1);
		index = scaled.floor.asInteger.min(samples.size - 2);
		frac = scaled - index;
		a = samples[index];
		b = samples[index + 1];
		^Point(a.x + ((b.x - a.x) * frac), a.y + ((b.y - a.y) * frac))
	}

	// the unit normal of the spiral's drawn line at `norm`. The reel notch
	// crosses the groove, so it needs the line's own direction — not the
	// shell's radius, which only coincides with it a quarter turn at a time.
	// The probe stays inside one polyline segment (60 of them span 2.5 turns,
	// so a wider chord would straddle a vertex and lean the tick by up to 7°).
	*normalProbe { ^0.002 }

	*spiralNormal { |coco, norm|
		var a = this.spiralPoint(coco, (norm - this.normalProbe).clip(0, 1));
		var b = this.spiralPoint(coco, (norm + this.normalProbe).clip(0, 1));
		var length = max(0.001, this.distance(a, b));
		^Point((b.y - a.y).neg, b.x - a.x) / length
	}

	// the mouth's two stationary transport marks (skip above the jack, flip
	// below it): one box for the cue and for the act, as everywhere in §4.5
	*markHit { |anchor, point|
		var reach = CQNightMeasures.markHitReach;
		^Rect.aboutPoint(anchor, reach, reach).containsPoint(point)
	}

	// the spesal dial annulus around a castle (a tap = one step)
	*dialHit { |castle, point|
		^(this.distance(point, castle[\center]) - castle[\dialR]).abs
			<= CQNightMeasures.clockHit
	}

	// ---- the opaque endpoint views (rake 12) ----
	// One source for the boxes the map builds and the clearance tests assert
	// against: a jack is a square, a socket is a little wider than tall once it
	// carries a lamp, and both are opaque, so anything drawn under one is gone.
	// The socket's anchor sits a pixel left of centre, which is the pixel the
	// lamp bead borrows on its side.

	*endpointExtent { |kind, lamp = false|
		var socket = CQNightMeasures.socketExtent;
		if(kind == \jack) {
			^Point(CQNightMeasures.jackExtent, CQNightMeasures.jackExtent)
		};
		^Point(if(lamp == true) { CQNightMeasures.lampedWidth } { socket.x },
			socket.y)
	}

	*endpointAnchor { |kind|
		var socket = CQNightMeasures.socketExtent;
		if(kind == \jack) {
			^Point(CQNightMeasures.jackExtent * 0.5, CQNightMeasures.jackExtent * 0.5)
		};
		^Point((socket.x * 0.5) - 1, socket.y * 0.5)
	}

	*endpointBox { |center, kind, lamp = false|
		var extent = this.endpointExtent(kind, lamp);
		var anchor = this.endpointAnchor(kind);
		^Rect(center.x - anchor.x, center.y - anchor.y, extent.x, extent.y)
	}

	// ---- the almanac corner (§16.6) ----
	// Free water in the top-right sky: the sowing moon, and under it the §7.1
	// compass — the weather machine's home, drawn since G3. The corner starts
	// where coco 2's own box ends, so no stage size can squeeze it; the
	// clearance tests pin every rect.
	//
	// The sower constellation that first stood here was removed on the user's
	// decision (2026-07-22): a preset is a recipe, not a body, and it was the
	// night's only control that could not be used without reading text. Presets
	// live in the Inspector, one toolbar click away in either view.

	// Ø 64 since §9 (the moon rose into the birds' altitude band, and a body
	// that reads as a moon at a glance has to carry a phase the eye can judge)
	*moonRadius { |scale = 1| ^CQNightMeasures.moonRadius * scale }

	// the pool's shore: a seeded, wobbled closed blob — the castle blob's
	// cousin (§16.3's organism language), never a circle and never a widget.
	// The wobble deliberately carries the lowest stretch past the waterline;
	// the layer clips it there, which is what reads as water continuing under
	// the frame, exactly as the roots already do.
	*poolShore { |center, rx, ry|
		^this.prSeeded(3311, {
			var count = CQNightMeasures.poolAnchors;
			this.smoothClosedSegments(count.collect { |i|
				var angle = (i / count * 2pi) + 0.10.rand2;
				var jitter = 1 + CQNightMeasures.poolWobble.rand2;
				Point(center.x + (cos(angle) * rx * jitter),
					center.y + (sin(angle) * ry * jitter))
			})
		})
	}

	*prBoundsOf { |points, pad = 0|
		var xs = points.collect { |p| p.x }, ys = points.collect { |p| p.y };
		^Rect(xs.minItem - pad, ys.minItem - pad,
			xs.maxItem - xs.minItem + (2 * pad),
			ys.maxItem - ys.minItem + (2 * pad))
	}

	*almanac { |width, height, scale = 1|
		var moonR = this.moonRadius(scale);
		var halo = moonR + CQNightMeasures.moonHalo;
		var mark = CQNightMeasures.markHitReach;
		// the moon in the open sky at the birds' own altitude, between castle
		// 1's box and bird B's name — the thing the birds fly under (§9)
		var moon = Point(width * CQNightMeasures.moonFraction,
			CQNightMeasures.birdAltitude * scale);
		// ...with the rain and the sun beside it, because they act on the whole
		// bay exactly as the moon's sowing does. The sky is the weather's place.
		var skyMarks = ();
		// ...and the pool in the water at the bottom of the bay, tangent to the
		// waterline the roots already run past. An ellipse: the pocket the bay
		// leaves free is wider than it is tall, and a disc would waste it.
		var rx = CQNightMeasures.poolRx * scale;
		var ry = CQNightMeasures.poolRy * scale;
		var pool = Point(width * CQNightMeasures.poolFraction, height - ry);
		var shore = this.poolShore(pool, rx, ry);
		var inset = 1 - (CQNightMeasures.stampInset * scale / min(rx, ry));
		var weather = (), extents, box;
		// corner seeds in bilinear order TL TR BL BR — the same order
		// CQCompass.bilinear speaks, so no view ever re-maps them — seated on
		// the shore at the four diagonal winds, which is where the ellipse→
		// square mapping puts the tide's own corners
		var stamps = [225, 315, 135, 45].collect { |degrees|
			var angle = degrees.degrad;
			Point(pool.x + (cos(angle) * rx * inset),
				pool.y + (sin(angle) * ry * inset))
		};
		CQNightMeasures.skyWeatherPlaces.do { |place|
			skyMarks[place[0]] = moon
				+ Point(place[1] * scale, place[2] * scale)
		};
		box = this.prBoundsOf([moon + Point(halo, halo), moon - Point(halo, halo)]
			++ skyMarks.values.collect { |p| p + Point(mark, mark) }
			++ skyMarks.values.collect { |p| p - Point(mark, mark) },
			CQNightMeasures.moonBoxSlack);
		CQNightMeasures.poolWeatherPlaces.do { |place|
			var angle = place[1].degrad, factor = place[2];
			weather[place[0]] = Point(pool.x + (cos(angle) * rx * factor),
				pool.y + (sin(angle) * ry * factor))
		};
		skyMarks.keysValuesDo { |name, point| weather[name] = point };
		// the layer holds the drawn shore, the seeds with their breed rings and
		// every weather mark's own touch box — read off the bodies themselves,
		// so moving one can never leave it clipping against the layer's edge.
		// It is then clipped to the stage: the shore's lowest stretch runs past
		// the waterline on purpose, and Qt cutting it there IS the drawing.
		extents = this.sampleSegments(shore, 8)
			++ stamps.collect { |p| p + Point(9, 9) }
			++ stamps.collect { |p| p - Point(9, 9) }
			++ CQNightMeasures.poolWeatherPlaces.collect { |place|
				weather[place[0]] + Point(mark, mark) }
			++ CQNightMeasures.poolWeatherPlaces.collect { |place|
				weather[place[0]] - Point(mark, mark) };
		^(box: box, moon: moon, moonR: moonR, skyMarks: skyMarks,
			// the tide's own frame stays a square: the bilinear has four
			// corners whatever body it is poured on (§7.1). Its half-sides are
			// the pool's radii, so the square's corners are the ellipse's
			// diagonals — see compassNorm.
			compass: Rect.aboutPoint(pool, rx, ry),
			pool: (center: pool, rx: rx, ry: ry, segments: shore),
			stamps: stamps,
			weather: weather,
			compassBox: this.prBoundsOf(extents, CQNightMeasures.compassBoxPad)
				.sect(Rect(0, 0, width, height)))
	}

	*moonHit { |almanac, point|
		^this.distance(point, almanac[\moon]) <= almanac[\moonR]
	}

	// ---- §7.1 the pool's coordinate frame (pure) ----
	// The tide's math is a bilinear square (four corners, §7.1) but the body it
	// is poured on is a round pool (§9). The elliptical grid mapping carries one
	// into the other, and it is chosen over the cheap radial squash for exactly
	// one reason: **the rim at a diagonal maps to exactly a corner**. A stamped
	// memory that cannot be reached bit-exact is a memory the pad has lost, so
	// the fixed points are unit-tested before either direction touches a view.
	//
	// square -> disc:  u = x·sqrt(1 - y²/2),  v = y·sqrt(1 - x²/2)
	// disc -> square:  the closed-form inverse below
	// Both speak the centred frame [-1, 1]², not the 0..1 bilinear frame.

	*squareToDisc { |point|
		^Point(point.x * sqrt(max(0, 1 - (point.y.squared * 0.5))),
			point.y * sqrt(max(0, 1 - (point.x.squared * 0.5))))
	}

	// On the rim the second root collapses to zero, so the inverse has a
	// square-root singularity exactly at the boundary: an error of ε in the
	// point becomes an error of ~2·sqrt(ε) in the square. Float dust of 1e-16
	// therefore lands a corner at 1 − 2e-8 instead of 1, and a corner that is
	// not exactly a corner is a stamp that comes back *nearly* — the tide would
	// crossfade shared cells by a hair instead of returning them (§7.1's
	// bit-exact recall, rake 13). The snap is tied to that square root: a
	// deviation big enough to matter is ~2.4e-13 of the radius, so this can
	// only ever catch the dust and never a hand.
	*cornerSnap { ^1e-6 }

	*prSnapEdge { |value|
		if((value - 1).abs < this.cornerSnap) { ^1.0 };
		if((value + 1).abs < this.cornerSnap) { ^-1.0 };
		^value
	}

	*discToSquare { |point|
		var u = point.x, v = point.y;
		var uu = u.squared, vv = v.squared;
		var root2 = 2.sqrt;
		// the max(0, ...) guards are that same dust at the rim, not a clamp:
		// the terms are non-negative for every point of the closed unit disc
		var x = (sqrt(max(0, 2 + uu - vv + (2 * root2 * u)))
			- sqrt(max(0, 2 + uu - vv - (2 * root2 * u)))) * 0.5;
		var y = (sqrt(max(0, 2 - uu + vv + (2 * root2 * v)))
			- sqrt(max(0, 2 - uu + vv - (2 * root2 * v)))) * 0.5;
		^Point(this.prSnapEdge(x).clip(-1, 1), this.prSnapEdge(y).clip(-1, 1))
	}

	// ---- §7.1 compass hit tests (pure) ----

	// The pool answers inside its drawn shore plus the catch band — the same
	// idiom the castle blob's `bodyMargin` uses, and here it earns its keep
	// twice: it answers the approaching hand, and it is what makes the tide's
	// pure corners reachable at all (see compassNorm). The mean radius stands
	// in for the wobbled shore exactly as the castle's ellipse stands in for
	// its blob — a few px of hand-drawn irregularity is not a hit boundary.
	*prPoolNorm { |almanac, point|
		var pool = almanac[\pool];
		^Point((point.x - pool[\center].x) / max(1, pool[\rx]),
			(point.y - pool[\center].y) / max(1, pool[\ry]))
	}

	*compassHit { |almanac, point|
		var pool = almanac[\pool];
		// the catch band in the ellipse's own normalized frame, measured off
		// the shorter radius so it is never thinner than `poolCatch` px
		var band = 1 + (CQNightMeasures.poolCatch / max(1, min(pool[\rx], pool[\ry])));
		var n = this.prPoolNorm(almanac, point);
		^((n.x * n.x) + (n.y * n.y)) <= (band * band)
	}

	// A stage point as the tide's own 0..1 coordinates (TL = origin, the
	// bilinear's frame). The body is round and the math is square, so the
	// elliptical grid mapping carries one into the other; a press outside the
	// shore holds at the nearest place ON it, which is why aiming past the
	// shore at a seed's diagonal lands exactly on that corner (rake 13).
	*compassNorm { |almanac, point|
		var n = this.prPoolNorm(almanac, point);
		var u = n.x, v = n.y;
		var length = sqrt((u * u) + (v * v)), square;
		if(length > 1) { u = u / length; v = v / length };
		square = this.discToSquare(Point(u, v));
		^Point(((square.x + 1) * 0.5).clip(0, 1),
			((square.y + 1) * 0.5).clip(0, 1))
	}

	// ...and back: where on the pool a given place in the tide lives. The cross
	// a voyage draws rides this, and so does anything that has to point AT a
	// corner rather than merely near it.
	*compassPointAt { |almanac, norm|
		var pool = almanac[\pool];
		var disc = this.squareToDisc(Point((norm.x.clip(0, 1) * 2) - 1,
			(norm.y.clip(0, 1) * 2) - 1));
		^Point(pool[\center].x + (disc.x * pool[\rx]),
			pool[\center].y + (disc.y * pool[\ry]))
	}

	// which corner seed the point lands on, or nil — consulted before the pad
	// itself, since the seeds live inside the water. A stamp answers under the
	// 8 px tap threshold, so it is a tap target and never a drag start.
	*stampHit { |almanac, point|
		^almanac[\stamps].detectIndex { |stamp|
			this.distance(point, stamp) <= CQNightMeasures.stampRadius
		}
	}

	// which weather mark the point lands on (\grow / \breed / \wither), or nil
	*weatherHit { |almanac, point|
		^#[grow, breed, wither].detect { |name|
			this.markHit(almanac[\weather][name], point)
		}
	}

	// ---- spesal coupling chords (§16.6, added 2026-07-22) ----
	// Which castle each castle reads is structure, not a quantity, so it is
	// shown only where the hand is asking (§4.5's hover grammar) — one chord
	// for a hovered castle, all five for the hovered river. The chord bows
	// toward the ring's eye so that even an *adjacent* pair reads as an inner
	// chord instead of doubling the river's own line, and it is clamped so it
	// can never crowd the storm iris sitting at that centre.

	*chordBow { ^0.4 }          // of the way from the chord's midpoint to the eye
	*chordClearance { ^0.35 }   // ...but never nearer the eye than this × ringR

	// where a ray leaves an axis-aligned box, plus a hair. A castle's layer is
	// square, so "half its width along the ray" is still *inside* it in every
	// direction but the four axes — the chord has to start where the opaque
	// layer actually stops covering it, or its arrowhead hides under the body
	// it is pointing at.
	*boxExit { |center, box, unit, margin = 2|
		var tx = if(unit.x.abs < 1e-9) { inf } { (box.width * 0.5) / unit.x.abs };
		var ty = if(unit.y.abs < 1e-9) { inf } { (box.height * 0.5) / unit.y.abs };
		^center + (unit * (min(tx, ty) + margin))
	}

	*spesalChord { |geometry, from, to|
		var castles = geometry[\castles];
		var a = castles[from][\center], b = castles[to][\center];
		var eye = geometry[\ringCenter];
		var reach = geometry[\ringR] * this.chordClearance;
		var length = max(0.001, this.distance(a, b));
		var unit = (b - a) / length;
		// both ends stop at their castle's own opaque box, exactly where the
		// river's visible stretch stops: a line under an opaque layer is a line
		// nobody sees (§16.6)
		var start = this.boxExit(a, castles[from][\box], unit);
		var end = this.boxExit(b, castles[to][\box], unit * -1);
		var mid = (start + end) * 0.5;
		var pull = mid + ((eye - mid) * this.chordBow);
		var pulled = this.distance(pull, eye);
		if(pulled < reach) {
			pull = if(pulled < 0.001) { mid } {
				eye + ((pull - eye) / pulled * reach)
			};
		};
		^[start, pull, pull, end]
	}

	// a point riding a chord: t = 0 at the castle being read, t = 1 at the one
	// reading it — the direction the sampled signal actually travels
	*chordPoint { |path, t|
		var v = t.clip(0, 1), u = 1 - v;
		^(path[0] * (u * u * u)) + (path[1] * (3 * u * u * v))
			+ (path[2] * (3 * u * v * v)) + (path[3] * (v * v * v))
	}

	// crossings per lap of the river's own firefly phase: the chords are short,
	// so a light crosses one about three times while a firefly rounds the ring
	*chordLaps { ^3 }

	// the castle's own flesh: the ellipse the blob is drawn around (§16.6), grown
	// by `margin` so the body answers the hand slightly before it arrives. This
	// is the §6 body-solo target AND, with dialHit excluded, the area that wears
	// the hover echo (§4.5) — one geometry for both, because a cue that covers
	// more or less than its action is a cue that lies.
	*bodyHit { |castle, point, margin|
		var m = margin ? CQNightMeasures.bodyMargin;
		var dx = (point.x - castle[\center].x) / max(0.001, castle[\bodyRx] + m);
		var dy = (point.y - castle[\center].y) / max(0.001, castle[\bodyRy] + m);
		^(dx.squared + dy.squared) <= 1
	}

	// One law for all three night bodies (decided 2026-07-21): **a body is
	// taken by its own drawn outline**, and hovering that outline is the cue.
	// The castle offers its blob, the shell its outer rim, the ring its line —
	// each a mark the eye already reads as "the whole organism", each narrow
	// enough that the water around it stays free for the harvest lasso.

	// the shell's rim: a band on the circle the hold seal draws. The spiral
	// itself never reaches past outerR except at the mouth, and the winding
	// surface is consulted first, so the two can never fight over a press.
	*shellRimHit { |coco, point, margin|
		^(this.distance(point, coco[\center]) - coco[\outerR]).abs
			<= (margin ? CQNightMeasures.rimMargin)
	}

	// distance from a point to the ring's drawn line (the river itself)
	*ringDistance { |geometry, point|
		var samples = geometry[\ringSamples], best = inf;
		// cheap radial reject first: most of the stage is nowhere near the river
		if((this.distance(point, geometry[\ringCenter]) - geometry[\ringR]).abs
			> (geometry[\ringR] * 0.45)) { ^inf };
		samples.doAdjacentPairs { |a, b|
			best = min(best, this.prPointSegmentDistance(point, a, b));
		};
		// the river is a closed loop: its last sample joins its first
		^min(best, this.prPointSegmentDistance(point, samples.last, samples.first))
	}

	*ringBodyHit { |geometry, point, margin|
		^this.ringDistance(geometry, point)
			<= (margin ? CQNightMeasures.rimMargin + 2)
	}

	// ---- the whole archipelago from one stage size ----
	// Bodies scale with the window (§16.10); stroke widths and text stay in px
	// at the draw sites. The minimum stage (1280x570 under the toolbar and the
	// Master strip) gives scale 1.
	*compute { |width, height|
		var scale = min(width / CQNightMeasures.refWidth,
			height / CQNightMeasures.refHeight).max(CQNightMeasures.minScale);
		var ringCenter = Point(width * 0.5,
			height * CQNightMeasures.ringHeightFraction);
		var ringR = CQNightMeasures.ringRadius * scale;
		var endpoints = IdentityDictionary.new;
		var castles, ringSegments, ringSamples, ringBox, cocos, roots, birds;
		var sprout, almanac, xs, ys;

		// five castles in a true ring, castle 1 at top, k -> k+1 clockwise —
		// pentagon ±6°, radius ±5 %, body ±8 % (§16.3 asymmetry law), seeded
		castles = this.prSeeded(9021, {
			5.collect { |k|
				var angle = -0.5pi + (k * 0.4pi) + 6.degrad.rand2;
				var center = this.polar(ringCenter, ringR * (1 + 0.05.rand2), angle);
				var bodyRx = CQNightMeasures.castleRx * scale * (1 + 0.08.rand2);
				var bodyRy = CQNightMeasures.castleRy * scale * (1 + 0.08.rand2);
				var blobAnchors = 6.collect { |a|
					var blobAngle = (a / 6 * 2pi) + 0.22.rand2;
					var jitter = 1 + 0.08.rand2;
					Point(center.x + (cos(blobAngle) * bodyRx * jitter),
						center.y + (sin(blobAngle) * bodyRy * jitter))
				};
				// the spesal dial is the castle's inner clock: it lives inside
				// the body, where the opaque endpoint views can never cover its
				// hand — pinned by test_inkClearances, not by eye
				var reach = (CQNightMeasures.castleBoxReach * scale)
					+ CQNightMeasures.castleBoxSlack;
				(center: center, bodyRx: bodyRx, bodyRy: bodyRy,
					blobSegments: this.smoothClosedSegments(blobAnchors),
					dialR: CQNightMeasures.clockRadius * scale,
					letterAnchor: Point(center.x,
						center.y + (CQNightMeasures.rangeLetterDrop * scale)),
					box: Rect.aboutPoint(center, reach, reach))
			}
		});
		// ring stubs: the opaque castle layer redraws the river's short passage
		// through its own box so the loop reads as continuous under the body
		castles.do { |castle, k|
			castle[\stubs] = [castles.wrapAt(k - 1), castles.wrapAt(k + 1)]
				.collect { |other|
					var delta = other[\center] - castle[\center];
					var length = max(0.001, this.distance(castle[\center], other[\center]));
					var unit = delta / length;
					var rim = max(castle[\bodyRx], castle[\bodyRy]) + 2;
					// the far end is read off the box itself, so a grown layer
					// carries its own stub with it (rake 12)
					[castle[\center] + (unit * rim),
						castle[\center] + (unit * (castle[\box].width * 0.5))]
				};
		};
		ringSegments = this.smoothClosedSegments(castles.collect { |c| c[\center] });
		ringSamples = this.sampleSegments(ringSegments, 24);
		xs = ringSamples.collect { |p| p.x };
		ys = ringSamples.collect { |p| p.y };
		ringBox = Rect(xs.minItem - CQNightMeasures.ringBoxPad,
			ys.minItem - CQNightMeasures.ringBoxPad,
			xs.maxItem - xs.minItem + (2 * CQNightMeasures.ringBoxPad),
			ys.maxItem - ys.minItem + (2 * CQNightMeasures.ringBoxPad));

		// coco 1 left, coco 2 right: spiral shells with their mouths toward the
		// ring, org fruit on the second turn, five moons on an arc below
		cocos = 2.collect { |i|
			var side = if(i == 0) { -1 } { 1 };
			var winding = if(i == 0) { 1 } { -1 };
			var mouthAngle = if(i == 0) { 0 } { pi };
			var center = Point(ringCenter.x
				+ (side * (ringR + (CQNightMeasures.shellGap * scale))),
				ringCenter.y - (CQNightMeasures.shellRise * scale));
			var outerR = CQNightMeasures.shellOuterR * scale;
			var segments = this.spiralSegments(center,
				CQNightMeasures.spiralInnerR * scale, outerR,
				mouthAngle, winding);
			var mouth = segments.last[3];
			var orgSpiralPoint = segments[5][0];
			var orgDelta = orgSpiralPoint - center;
			var orgLength = max(0.001, this.distance(center, orgSpiralPoint));
			var shellReach = outerR + (CQNightMeasures.shellBoxPad * scale);
			(center: center, outerR: outerR, winding: winding,
				mouthAngle: mouthAngle, segments: segments,
				mouth: mouth,
				// sampleSegments stops at t = 5/6 of the last cubic (an open
				// curve, unlike the ring) — the mouth closes the winding
				// surface so the reel pearl and the grab reach the outer rim
				spiralSamples: this.sampleSegments(segments, 6) ++ [mouth],
				// §6.1 touch homes: the press-to-flip chevron hangs below the
				// mouth — clear of the mouth jack's opaque 26 px view and
				// inside the shell box at every scale — plus the hold heart at
				// the spiral's innermost eye, the shell's own opaque layer box
				// and the tide crescents under the three continuous moons
				chevron: mouth + Point(0, max(CQNightMeasures.chevronDrop * scale,
					CQNightMeasures.chevronDropFloor)),
				// skip's night home, landed at the G2 gate: a stationary mark
				// under the flip chevron — the Day panel's own FLIP/SKIP pair,
				// hanging in the one direction that is empty. The read-head
				// comet lost the gesture because a moving target is not a
				// control: past a crawl it cannot be hit at all, and a 5 Hz
				// frame draws it stale. Above the mouth looked symmetrical and
				// is wrong: the spiral *ends* at the mouth and arrives there
				// from above, so that side is the groove itself (caught by the
				// winding-surface assert, CQGuiNightTouchTest).
				skipMark: mouth + Point(0,
					max(CQNightMeasures.chevronDrop * scale,
						CQNightMeasures.chevronDropFloor)
					+ max(CQNightMeasures.skipDrop * scale,
						CQNightMeasures.skipDropFloor)),
				heartRadius: CQNightMeasures.heartHit,
				shellBox: Rect.aboutPoint(center, shellReach, shellReach),
				crescents: [126, 108, 90].collect { |degrees|
					this.polar(center, CQNightMeasures.crescentArc * scale,
						degrees.degrad)
				},
				orgSpiralPoint: orgSpiralPoint,
				orgCenter: orgSpiralPoint + ((orgDelta / orgLength)
					* (CQNightMeasures.orgStemReach * scale)),
				moons: [126, 108, 90, 72, 54].collect { |degrees|
					this.polar(center, CQNightMeasures.moonArc * scale,
						degrees.degrad)
				})
		};
		cocos.do { |coco, i|
			var n = i + 1;
			endpoints[("coco" ++ n).asSymbol] = coco[\mouth];
			endpoints[("org" ++ n).asSymbol] = coco[\orgCenter];
			[\spaf, \inVca, \fbVca, \flip, \skip].do { |suffix, m|
				endpoints[("coco" ++ n ++ "_" ++ suffix).asSymbol] = coco[\moons][m];
			};
		};
		castles.do { |castle, k|
			var n = k + 1, center = castle[\center];
			var jackOrbit = CQNightMeasures.castleJackOrbit * scale;
			endpoints[("tri" ++ n).asSymbol] =
				this.polar(center, jackOrbit, this.clockAngle(1.5));
			endpoints[("cas" ++ n).asSymbol] =
				this.polar(center, jackOrbit, this.clockAngle(10.5));
			endpoints[("castle" ++ n ++ "_fm").asSymbol] =
				this.polar(center, CQNightMeasures.castleFmOrbit * scale,
					this.clockAngle(4));
			// the lamped spesal view is the widest box a castle carries, with
			// its lamp reaching further still toward the body — this orbit is
			// what keeps it off the inner clock (see CQNightGeometryTest)
			endpoints[("castle" ++ n ++ "_spesal").asSymbol] =
				this.polar(center, CQNightMeasures.castleSpesalOrbit * scale,
					this.clockAngle(8));
		};

		// inputs as roots at the bottom, envA left, envB right; the mic sprout
		// grows beside envA (display only — the mic is not one of the 16)
		roots = this.prSeeded(4407, {
			CQNightMeasures.rootFractions.collect { |fraction, i|
				var side = if(i == 0) { -1 } { 1 };
				var tip = Point(width * fraction,
					height - (CQNightMeasures.rootReach * scale));
				var base = Point(tip.x
					+ (side * CQNightMeasures.rootSpread * scale), height + 2);
				var mid1 = Point(base.x + ((tip.x - base.x) * 0.35) + 6.0.rand2,
					base.y + ((tip.y - base.y) * 0.4) + 4.0.rand2);
				var mid2 = Point(base.x + ((tip.x - base.x) * 0.7) + 6.0.rand2,
					base.y + ((tip.y - base.y) * 0.75) + 4.0.rand2);
				var reach = max(0.001, this.distance(mid2, tip));
				// the side twig grows AWAY from the tip and leaves low on the
				// stem. Aimed the other way it ran the length of the tip jack's
				// opaque box and all but vanished — visibly so on the left root,
				// whose twig lay almost exactly along the box (fixed 2026-07-21)
				(points: [base, mid1, mid2, tip],
					// the unit vector the root arrives on: the tip jack's opaque
					// view redraws this short passage under its own box, exactly
					// as a castle redraws the river's (§16.6) — otherwise the
					// root visibly stops a jack-radius short of its own tip
					arrival: (tip - mid2) / reach,
					branch: [mid1, mid1 + Point(
						side * CQNightMeasures.rootTwigX * scale,
						(CQNightMeasures.rootTwigY * scale).neg + 3.0.rand2)])
			}
		});
		endpoints[\envA] = roots[0][\points].last;
		endpoints[\envB] = roots[1][\points].last;
		sprout = (
			base: Point((width * CQNightMeasures.rootFractions[0])
				+ (CQNightMeasures.sproutOffset * scale), height + 2),
			tip: Point((width * CQNightMeasures.rootFractions[0])
				+ (CQNightMeasures.sproutTipOffset * scale),
				height - (CQNightMeasures.sproutReach * scale)));

		// decks as birds at the top; the rate moon under the wing with the
		// deck-level tide crescent beside it, filename (10 px mono) beneath
		birds = [0.24, 0.76].collect { |fraction|
			var center = Point(width * fraction,
				CQNightMeasures.birdAltitude * scale);
			(center: center, span: CQNightMeasures.birdSpan * scale,
				box: Rect.aboutPoint(center, CQNightMeasures.birdBoxRx * scale,
					CQNightMeasures.birdBoxRy * scale),
				rateSocket: center + (CQNightMeasures.birdRateOffset * scale),
				levelCrescent: center
					+ (CQNightMeasures.birdCrescentOffset * scale),
				// 20 px of line box for a 10 px mono face: the earlier 14 px
				// clipped the filename's ascenders and descenders (§16.4 fixes
				// the font size, so the box has to give way, not the type)
				// fixed in px so a resize never re-cuts a name that already fits;
				// the line box holds a 10 px mono face whole, and CQGui trims an
				// overlong name from its tail only (§16.4)
				labelRect: Rect(center.x - (CQNightMeasures.birdLabelExtent.x * 0.5),
					center.y + (CQNightMeasures.birdLabelDrop * scale),
					CQNightMeasures.birdLabelExtent.x,
					CQNightMeasures.birdLabelExtent.y))
		};
		// §16.7's drop target: the bird AND its name — both what the eye reads
		// as "this deck" and what actually accepts the file (label and wing
		// socket take drops too). The bird's own 44x32 box was smaller than the
		// file icon Windows drags under the cursor, so the border hid beneath
		// the very thing it was answering (measured 2026-07-22).
		birds.do { |bird|
			var pad = CQNightMeasures.birdDropPad.neg;
			bird[\dropRect] = bird[\box].union(bird[\labelRect]).insetBy(pad, pad);
		};
		endpoints[\deckA_rate] = birds[0][\rateSocket];
		endpoints[\deckB_rate] = birds[1][\rateSocket];

		// the moon in the sky, the tide pool in the water — two features of the
		// landscape rather than one corner of tools (§9, decided 2026-07-26)
		almanac = this.almanac(width, height, scale);

		^(scale: scale, width: width, height: height,
			ringCenter: ringCenter, ringR: ringR,
			eye: ringCenter,           // §6.1: the ring's qFreq/qChaos pupil
			castles: castles, ringSegments: ringSegments,
			ringSamples: ringSamples, ringBox: ringBox,
			cocos: cocos, roots: roots, sprout: sprout, birds: birds,
			almanac: almanac, endpoints: endpoints)
	}
}
