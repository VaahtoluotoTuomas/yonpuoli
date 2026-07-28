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

	// One point on a cubic [p0, c1, c2, p1]. Root anatomy uses this to grow
	// the env follower from the actual post-preamp audio vein, not from a
	// nearby decorative approximation.
	*cubicPoint { |segment, t|
		var u = 1 - t;
		^((segment[0] * (u * u * u))
			+ (segment[1] * (3 * u * u * t))
			+ (segment[2] * (3 * u * t * t))
			+ (segment[3] * (t * t * t)))
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

	// The winding value still reaches exact 1, but its setting tick stops one
	// sampled breath before the mouth jack. At the literal endpoint the jack
	// and the tick would claim the same pixels; a maximum setting must remain
	// legible rather than disappear under its neighbouring source dot.
	*reelNotchNorm { |norm|
		^norm.clip(0, CQNightMeasures.reelMaxNorm)
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

	// The castle's 6-o'clock rate ripple: its fixed-pixel touch rectangle is
	// also the hover cue's geometry. The drawn 22x11 mark fits wholly inside it.
	*rangeBox { |castle|
		^Rect.aboutPoint(castle[\rangeMark],
			CQNightMeasures.rangeHitX, CQNightMeasures.rangeHitY)
	}

	*rangeHit { |castle, point|
		^this.rangeBox(castle).containsPoint(point)
	}

	// The in/fb owner grows only below its unchanged crescent anchor. Keeping
	// this pure lets the collision matrix assert the real opaque rectangle.
	*crescentViewBox { |center, carriesDolby = false|
		var extent = CQNightMeasures.crescentExtent;
		var height = if(carriesDolby) {
			CQNightMeasures.dolbyCrescentHeight
		} {
			extent
		};
		^Rect(center.x - (extent * 0.5), center.y - (extent * 0.5),
			extent, height)
	}

	*dolbyOysterAnchor { |crescentCenter|
		^crescentCenter + Point(0, CQNightMeasures.dolbyOysterDrop)
	}

	*dolbyOysterBox { |crescentCenter|
		^Rect.aboutPoint(this.dolbyOysterAnchor(crescentCenter),
			CQNightMeasures.dolbyOysterHitX,
			CQNightMeasures.dolbyOysterHitY)
	}

	*dolbyOysterHit { |crescentCenter, point|
		^this.dolbyOysterBox(crescentCenter).containsPoint(point)
	}

	*crescentControlHit { |crescentCenter, point|
		var half = CQNightMeasures.crescentExtent * 0.5;
		^Rect.aboutPoint(crescentCenter, half, half).containsPoint(point)
	}

	// the spesal dial annulus around a castle (a tap = one step)
	*dialHit { |castle, point|
		^(this.distance(point, castle[\center]) - castle[\dialR]).abs
			<= CQNightMeasures.clockHit
	}

	// ---- endpoint view extents (rake 12) ----
	// One source for the boxes the map builds and the clearance tests assert
	// against: a jack is a square, and a socket is a little wider than tall
	// once it carries a lamp. Most owners paint opaque paper; castle details
	// are the clear exception. The socket's anchor sits a pixel left of centre,
	// which is the pixel the lamp bead borrows on its side.

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
		var moon = Point(width * CQNightMeasures.moonFraction,
			CQNightMeasures.moonAltitude * scale);
		var weatherPreview = ();
		var tide = Rect(width * CQNightMeasures.tideLeftFraction,
			height * CQNightMeasures.tideTopFraction,
			width * (CQNightMeasures.tideRightFraction
				- CQNightMeasures.tideLeftFraction),
			height * (CQNightMeasures.tideBottomFraction
				- CQNightMeasures.tideTopFraction));
		var stamps = [tide.leftTop, tide.rightTop,
			tide.leftBottom, tide.rightBottom];
		var box;
		CQNightMeasures.moonWeatherPlaces.do { |place|
			weatherPreview[place[0]] = moon
				+ Point(place[1] * scale, place[2] * scale)
		};
		box = this.prBoundsOf(
			[moon + Point(halo, halo), moon - Point(halo, halo)]
			++ weatherPreview.values.collect { |p| p + Point(mark, mark) }
			++ weatherPreview.values.collect { |p| p - Point(mark, mark) },
			CQNightMeasures.moonBoxSlack);
		^(box: box, moon: moon, moonR: moonR,
			weatherPreview: weatherPreview,
			compass: tide, stamps: stamps,
			// The tide is painted with the full-stage ground. This compatibility
			// key now describes that transparent field layer, never an opaque pad.
			compassBox: Rect(0, 0, width, height))
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

	*compassHit { |almanac, point|
		^almanac[\compass].containsPoint(point)
	}

	// A direct field frame: the four seed winds are the bilinear's four exact
	// corners. The hand may travel outside after capture, so both axes clamp.
	*compassNorm { |almanac, point|
		var tide = almanac[\compass];
		^Point(
			this.prSnapEdge(((point.x - tide.left)
				/ max(1, tide.width)).clip(0, 1)),
			this.prSnapEdge(((point.y - tide.top)
				/ max(1, tide.height)).clip(0, 1)))
	}

	// ...and back, used both by the active cross and exact corner recall.
	*compassPointAt { |almanac, norm|
		var tide = almanac[\compass];
		^Point(tide.left + (norm.x.clip(0, 1) * tide.width),
			tide.top + (norm.y.clip(0, 1) * tide.height))
	}

	// which corner seed the point lands on, or nil — consulted before the pad
	// itself, since the seeds live inside the water. A stamp answers under the
	// 8 px tap threshold, so it is a tap target and never a drag start.
	*stampHit { |almanac, point|
		^almanac[\stamps].detectIndex { |stamp|
			this.distance(point, stamp) <= CQNightMeasures.stampRadius
		}
	}

	// The tide gate is the one explicit entrance into playable water. Its
	// visual is a low gill rather than a button, but cue and action still share
	// one compact body.
	*tideGateHit { |geometry, point|
		^this.distance(point, geometry[\tideGate])
			<= CQNightMeasures.tideGateHit
	}

	// Weather is no longer a set of static targets. Kept as a compatibility
	// query for callers crossing this commit: it deliberately answers nowhere.
	*weatherHit { |almanac, point|
		^nil
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

	*spesalChord { |geometry, from, to|
		var castles = geometry[\castles];
		var a = castles[from][\center], b = castles[to][\center];
		var eye = geometry[\ringCenter];
		var reach = geometry[\ringR] * this.chordClearance;
		// Transparent castle paper lets the chord reach each centre. The
		// irregular ground-filled flesh above masks only the part behind it.
		var start = a;
		var end = b;
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

	// §4.5 made structural: ONE answer for what a point on a shell belongs to,
	// asked by the hover cue and by the press alike. The two chains used to be
	// written out separately and had drifted — the press consulted the heart,
	// the cue did not — so at slow speeds the groove lit up under the hand and
	// the press toggled the loop instead of winding. Measured before the fix:
	// the norm-0 reel pearl sits 14 px from centre at the minimum stage and
	// 21 px at 1920x855, both inside the heart's 22 px.
	//
	// The heart wins its own disc on purpose. It is small, central and fixed,
	// and its act is consequential (a stray toggle starts or ends a recording),
	// while winding is an ANGULAR gesture available along the whole five-turn
	// groove — so giving the spiral's eye to the heart costs the reel nothing
	// that cannot be done one turn out.
	//
	// Geometry only: cables, modifiers and buttons are NOT decided here.
	// CQGuiNightTouch keeps §4.5's cable-first check ahead of this call.
	*shellTargetAt { |coco, point|
		if(this.distance(point, coco[\center]) <= coco[\heartRadius]) {
			^\heart
		};
		if(this.markHit(coco[\chevron], point)) { ^\chevron };
		if(this.markHit(coco[\skipMark], point)) { ^\skip };
		if(this.spiralDistance(coco, point) <= CQNightMeasures.windGrab) {
			^\wind
		};
		if(this.shellRimHit(coco, point)) { ^\rim };
		^nil
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

	// The visible two-lobed outline fits inside this generous hand target.
	*preampKnotHit { |root, point|
		^this.distance(root[\knot], point) <= CQNightMeasures.rootKnotHit
	}

	// Five irregular strands remain individually playable without acquiring
	// permanent labels. Sample the exact cubic and return only the nearest
	// provenance inside one hand-sized reach. The caller consults the knot
	// first, so the five curves may gather naturally without stealing its drag.
	*preampSourceHit { |root, point, reach|
		var best = inf, found;
		root[\sourceStrands].do { |segment, i|
			var samples = this.sampleSegments([segment], 24) ++ [segment.last];
			samples.doAdjacentPairs { |a, b|
				var distance = this.prPointSegmentDistance(point, a, b);
				if(distance < best) {
					best = distance;
					found = i;
				};
			};
		};
		^if(best <= (reach ? CQNightMeasures.rootSourceHit)) { found }
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
		var sprout, almanac, tideGate, tideGateBox, xs, ys;

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
				var blobBottom = blobAnchors.collect { |point| point.y }.maxItem;
				// the spesal dial is the castle's inner clock: it lives inside
				// the body, where the opaque endpoint views can never cover its
				// hand — pinned by test_inkClearances, not by eye
				var reach = (CQNightMeasures.castleBoxReach * scale)
					+ CQNightMeasures.castleBoxSlack;
				(center: center, bodyRx: bodyRx, bodyRy: bodyRy,
					blobBottom: blobBottom,
					blobSegments: this.smoothClosedSegments(blobAnchors),
					dialR: CQNightMeasures.clockRadius * scale,
					rangeMark: Point(center.x,
						blobBottom + CQNightMeasures.rangeBodyGap),
					box: Rect.aboutPoint(center, reach, reach))
			}
		});
		// The ring runs all the way through the castle centres. Each transparent
		// castle view later masks it with the irregular flesh itself.
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
				// mouth — clear of the mouth jack's 34 px interaction view and
				// inside the shell box at every scale — plus the hold heart at
				// the spiral's innermost eye, the shell's own opaque layer box
				// and the tide crescents under the three continuous moons
				chevron: mouth + Point(0, max(CQNightMeasures.chevronDrop * scale,
					CQNightMeasures.chevronDropFloor)),
				// skip's night home, landed at the G2 gate: a stationary mark
				// under the flip chevron, hanging in the one direction that is
				// empty. The read-head
				// comet lost the gesture because a moving target is not a
				// control: past a crawl it cannot be hit at all, and any
				// telemetry frame is slightly stale. Above the mouth looked symmetrical and
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

		// The intake body (§10): five unlabelled, seeded-wild source strands
		// converge on a drive/bias knot, an audio vein joins the corresponding
		// Cocoon's real spiral, and envA/B grows as the one patchable side bud.
		// The source count comes from CQSpec's enum; `none` is the honest
		// absence of a selected strand, never a sixth decorative root.
		//
		// The audio vein bows outward before it reaches the 0.8 point of the
		// spiral. That is the one lane around the three tide crescents below
		// each shell; the collision matrix pins it at both reference scales.
		roots = this.prSeeded(4407, {
			CQNightMeasures.rootFractions.collect { |fraction, i|
				var side = if(i == 0) { -1 } { 1 }; // outward from the bay
				var srcName = if(i == 0) { \preASrc } { \preBSrc };
				var sourceValues = CQSpec.enumValues(srcName)
					.reject { |value| value == \none };
				var knot = Point(width * fraction,
					height - (CQNightMeasures.rootKnotReach * scale));
				var spacing = CQNightMeasures.rootSourceSpacing * scale;
				var middle = (sourceValues.size - 1) * 0.5;
				var strands = sourceValues.collect { |value, s|
					var offset = ((s - middle) * spacing)
						+ (5.0.rand2 * scale);
					var base = Point(knot.x + offset, height + 2);
					var c1 = Point(base.x + (14.0.rand2 * scale),
						height - (CQNightMeasures.rootSourceShoulder * scale
							* (0.72 + 0.46.rand)));
					var c2 = Point(knot.x + (offset * 0.18)
						+ (14.0.rand2 * scale),
						knot.y + (CQNightMeasures.rootSourceGather * scale
							* (0.62 + 0.70.rand)));
					[base, c1, c2, knot]
				};
				var join = this.spiralPoint(cocos[i],
					CQNightMeasures.rootVeinJoinNorm);
				var vein = [
					knot,
					knot + Point(side * CQNightMeasures.rootVeinOutset * scale,
						CQNightMeasures.rootVeinLift.neg * scale),
					join + Point(side * CQNightMeasures.rootVeinOutset * scale,
						CQNightMeasures.rootVeinDrop * scale),
					join
				];
				var budBase = this.cubicPoint(vein, 0.2);
				var budAhead = this.cubicPoint(vein, 0.24);
				var envTip = budBase + Point(
					side * CQNightMeasures.rootBudX * scale,
					CQNightMeasures.rootBudY * scale);
				var budTangentReach = max(0.001,
					this.distance(budBase, budAhead));
				var budTangent = (budAhead - budBase) / budTangentReach;
				var envBranch = [
					budBase,
					budBase + (budTangent
						* (CQNightMeasures.rootBudX * scale * 0.34)),
					envTip - Point(side
						* (CQNightMeasures.rootBudX * scale * 0.28), 0),
					envTip
				];
				var envReach = max(0.001,
					this.distance(envBranch[2], envTip));
				var labelExtent = CQNightMeasures.rootLabelExtent;
				var labelTop = knot.y + (CQNightMeasures.rootLabelDrop * scale);
				// The provenance rests inward, and its ground erasure has to
				// stay off the delta — but the delta is seeded, so a constant
				// inset cannot promise that (at 1280x570 root B's leftmost
				// strand ran straight through the wash). Measure the strands
				// inside the erasure's own band and give way to them.
				var washPad = CQNightMeasures.rootLabelWashPad;
				var inward = side.neg;
				// The band is the erasure's own height grown by the clearance,
				// so a strand is either pushed aside horizontally or already
				// that far above or below it: the gap holds in every direction.
				var bandTop = labelTop - washPad.y
					- CQNightMeasures.rootLabelClear;
				var bandBottom = labelTop + labelExtent.y + washPad.y
					+ CQNightMeasures.rootLabelClear;
				var reach = strands.collect { |strand|
					(0, 0.01 .. 1).collect { |t| this.cubicPoint(strand, t) }
						.select { |p|
							(p.y >= bandTop) and: { p.y <= bandBottom }
						}
						.collect { |p| p.x * inward }
				}.flat;
				var restU = (knot.x * inward)
					+ (CQNightMeasures.rootLabelInward * scale);
				var clearU = if(reach.isEmpty) { restU } {
					reach.maxItem + CQNightMeasures.rootLabelClear
						+ washPad.x + (labelExtent.x * 0.5)
				};
				var labelCenterX = max(restU, clearU) * inward;
				var labelRect = Rect(labelCenterX - (labelExtent.x * 0.5),
					labelTop, labelExtent.x, labelExtent.y);
				var allPoints = strands.flat
					++ vein ++ envBranch ++ [labelRect.leftTop,
						labelRect.rightBottom];
				(side: side, sourceValues: sourceValues,
					sourceStrands: strands, knot: knot,
					audioVein: vein, audioJoin: join,
					envBranch: envBranch,
					// The env jack uses transparent paper, so this exact cubic
					// remains continuous beneath its dot without a repair seam.
					arrival: (envTip - envBranch[2]) / envReach,
					labelRect: labelRect,
					box: this.prBoundsOf(allPoints,
						CQNightMeasures.rootBoxPad))
			}
		});
		endpoints[\envA] = roots[0][\envBranch].last;
		endpoints[\envB] = roots[1][\envBranch].last;
		sprout = (
			base: Point((width * CQNightMeasures.rootFractions[0])
				+ (CQNightMeasures.sproutOffset * scale), height + 2),
			tip: Point((width * CQNightMeasures.rootFractions[0])
				+ (CQNightMeasures.sproutTipOffset * scale),
				height - (CQNightMeasures.sproutReach * scale)));

		// decks as birds at the top; the rate moon under the wing with the
		// deck-level tide crescent beside it, filename (10 px mono) beneath
		birds = CQNightMeasures.birdFractions.collect { |fraction|
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
		// Route revelations are geometry, not improvised paint. A bird has one
		// direct deck-slot flight to its own Cocoon and may feed either preamp;
		// the latter two paths are revealed only when state says so.
		birds.do { |bird, i|
			var side = if(i == 0) { -1 } { 1 };
			var directJoin = this.spiralPoint(cocos[i], 0.62);
			bird[\directRoute] = [
				bird[\center],
				bird[\center] + Point(side * 34 * scale, 76 * scale),
				directJoin + Point(side * 42 * scale, -72 * scale),
				directJoin
			];
			bird[\rootRoutes] = roots.collect { |root, r|
				var bow = if(r == i) { side * 74 } {
					if(r == 0) { -1 } { 1 } * 118
				};
				[
					bird[\center],
					bird[\center] + Point(bow * scale, 92 * scale),
					root[\knot] + Point(root[\side] * 70 * scale,
						-98 * scale),
					root[\knot]
				]
			};
		};
		endpoints[\deckA_rate] = birds[0][\rateSocket];
		endpoints[\deckB_rate] = birds[1][\rateSocket];

		// the moon in the sky, the tide pool in the water — two features of the
		// landscape rather than one corner of tools (§9, decided 2026-07-26)
		almanac = this.almanac(width, height, scale);
		tideGate = Point(ringCenter.x,
			height * CQNightMeasures.tideGateFraction);
		tideGateBox = Rect.aboutPoint(tideGate,
			CQNightMeasures.tideGateHit + CQNightMeasures.tideGateBoxSlack,
			CQNightMeasures.tideGateHit + CQNightMeasures.tideGateBoxSlack);

		^(scale: scale, width: width, height: height,
			ringCenter: ringCenter, ringR: ringR,
			eye: ringCenter,           // §6.1: the ring's qFreq/qChaos pupil
			tideGate: tideGate, tideGateBox: tideGateBox,
			castles: castles, ringSegments: ringSegments,
			ringSamples: ringSamples, ringBox: ringBox,
			cocos: cocos, roots: roots, sprout: sprout, birds: birds,
			almanac: almanac, endpoints: endpoints)
	}
}
