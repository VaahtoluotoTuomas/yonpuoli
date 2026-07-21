// CQNightGeometry — pure layout math for the Night map (GUI_SPEC §16.6, §15).
// No Qt and no instrument state: a stage size goes in, every body and endpoint
// position comes out, so the topology truths — ring order and direction, cocos
// left/right, roots below, birds above — are unit-testable without a window.
// Every irregularity is seeded (§16.11): the same size always returns the same
// hand-drawn archipelago, and a redraw never boils.

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

	// the spesal dial annulus around a castle (a tap = one step)
	*dialHit { |castle, point|
		^(this.distance(point, castle[\center]) - castle[\dialR]).abs <= 8
	}

	// the castle's own flesh: the ellipse the blob is drawn around (§16.6). This
	// is the §6 body-solo target and, with dialHit excluded, the area that wears
	// the hover echo telling the hand the body is pressable (§4.5).
	*bodyHit { |castle, point|
		var dx = (point.x - castle[\center].x) / max(0.001, castle[\bodyRx]);
		var dy = (point.y - castle[\center].y) / max(0.001, castle[\bodyRy]);
		^(dx.squared + dy.squared) <= 1
	}

	// ---- the whole archipelago from one stage size ----
	// Bodies scale with the window (§16.10); stroke widths and text stay in px
	// at the draw sites. The minimum stage (1280x570 under the toolbar and the
	// Master strip) gives scale 1.
	*compute { |width, height|
		var scale = min(width / 1280, height / 570).max(0.5);
		var ringCenter = Point(width * 0.5, height * 0.52);
		var ringR = 148 * scale;
		var endpoints = IdentityDictionary.new;
		var castles, ringSegments, ringSamples, ringBox, cocos, roots, birds;
		var sprout, xs, ys;

		// five castles in a true ring, castle 1 at top, k -> k+1 clockwise —
		// pentagon ±6°, radius ±5 %, body ±8 % (§16.3 asymmetry law), seeded
		castles = this.prSeeded(9021, {
			5.collect { |k|
				var angle = -0.5pi + (k * 0.4pi) + 6.degrad.rand2;
				var center = this.polar(ringCenter, ringR * (1 + 0.05.rand2), angle);
				var bodyRx = 23 * scale * (1 + 0.08.rand2);
				var bodyRy = 20 * scale * (1 + 0.08.rand2);
				var blobAnchors = 6.collect { |a|
					var blobAngle = (a / 6 * 2pi) + 0.22.rand2;
					var jitter = 1 + 0.08.rand2;
					Point(center.x + (cos(blobAngle) * bodyRx * jitter),
						center.y + (sin(blobAngle) * bodyRy * jitter))
				};
				// the spesal dial is the castle's inner clock (r 10): it lives
				// inside the body where the opaque endpoint views (r >= ~14.6
				// to their nearest corner at scale 1) can never cover its hand
				(center: center, bodyRx: bodyRx, bodyRy: bodyRy,
					blobSegments: this.smoothClosedSegments(blobAnchors),
					dialR: 10 * scale,
					letterAnchor: Point(center.x, center.y + (33 * scale)),
					box: Rect.aboutPoint(center, (36 * scale) + 4, (36 * scale) + 4))
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
					[castle[\center] + (unit * rim),
						castle[\center] + (unit * (castle[\box].width * 0.5))]
				};
		};
		ringSegments = this.smoothClosedSegments(castles.collect { |c| c[\center] });
		ringSamples = this.sampleSegments(ringSegments, 24);
		xs = ringSamples.collect { |p| p.x };
		ys = ringSamples.collect { |p| p.y };
		ringBox = Rect(xs.minItem - 12, ys.minItem - 12,
			xs.maxItem - xs.minItem + 24, ys.maxItem - ys.minItem + 24);

		// coco 1 left, coco 2 right: spiral shells with their mouths toward the
		// ring, org fruit on the second turn, five moons on an arc below
		cocos = 2.collect { |i|
			var side = if(i == 0) { -1 } { 1 };
			var winding = if(i == 0) { 1 } { -1 };
			var mouthAngle = if(i == 0) { 0 } { pi };
			var center = Point(ringCenter.x + (side * (ringR + (190 * scale))),
				ringCenter.y - (26 * scale));
			var outerR = 85 * scale;
			var segments = this.spiralSegments(center, 10 * scale, outerR,
				mouthAngle, winding);
			var mouth = segments.last[3];
			var orgSpiralPoint = segments[5][0];
			var orgDelta = orgSpiralPoint - center;
			var orgLength = max(0.001, this.distance(center, orgSpiralPoint));
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
				chevron: mouth + Point(0, max(24 * scale, 22)),
				heartRadius: 14,
				shellBox: Rect.aboutPoint(center,
					outerR + (30 * scale), outerR + (30 * scale)),
				crescents: [126, 108, 90].collect { |degrees|
					this.polar(center, 130 * scale, degrees.degrad)
				},
				orgSpiralPoint: orgSpiralPoint,
				orgCenter: orgSpiralPoint + ((orgDelta / orgLength) * (10 * scale)),
				moons: [126, 108, 90, 72, 54].collect { |degrees|
					this.polar(center, 104 * scale, degrees.degrad)
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
			endpoints[("tri" ++ n).asSymbol] =
				this.polar(center, 33 * scale, this.clockAngle(1.5));
			endpoints[("cas" ++ n).asSymbol] =
				this.polar(center, 33 * scale, this.clockAngle(10.5));
			endpoints[("castle" ++ n ++ "_fm").asSymbol] =
				this.polar(center, 28 * scale, this.clockAngle(4));
			// r 40: the lamped spesal view is 34 px wide with the lamp
			// reaching 25 px toward the body — r 40 keeps its opaque box off
			// the inner clock at every scale (see CQNightGeometryTest)
			endpoints[("castle" ++ n ++ "_spesal").asSymbol] =
				this.polar(center, 40 * scale, this.clockAngle(8));
		};

		// inputs as roots at the bottom, envA left, envB right; the mic sprout
		// grows beside envA (display only — the mic is not one of the 16)
		roots = this.prSeeded(4407, {
			[0.17, 0.83].collect { |fraction, i|
				var side = if(i == 0) { -1 } { 1 };
				var tip = Point(width * fraction, height - (64 * scale));
				var base = Point(tip.x + (side * 30 * scale), height + 2);
				var mid1 = Point(base.x + ((tip.x - base.x) * 0.35) + 6.0.rand2,
					base.y + ((tip.y - base.y) * 0.4) + 4.0.rand2);
				var mid2 = Point(base.x + ((tip.x - base.x) * 0.7) + 6.0.rand2,
					base.y + ((tip.y - base.y) * 0.75) + 4.0.rand2);
				(points: [base, mid1, mid2, tip],
					branch: [mid2, mid2 + Point(side.neg * 20 * scale,
						(14 * scale).neg + 3.0.rand2)])
			}
		});
		endpoints[\envA] = roots[0][\points].last;
		endpoints[\envB] = roots[1][\points].last;
		sprout = (base: Point((width * 0.17) + (46 * scale), height + 2),
			tip: Point((width * 0.17) + (54 * scale), height - (36 * scale)));

		// decks as birds at the top; the rate moon under the wing with the
		// deck-level tide crescent beside it, filename (10 px mono) beneath
		birds = [0.24, 0.76].collect { |fraction|
			var center = Point(width * fraction, 40 * scale);
			(center: center, span: 30 * scale,
				box: Rect.aboutPoint(center, 22 * scale, 16 * scale),
				rateSocket: center + Point(11 * scale, 19 * scale),
				levelCrescent: center + Point(-18 * scale, 16 * scale),
				labelRect: Rect(center.x - 80, center.y + (32 * scale), 160, 14))
		};
		endpoints[\deckA_rate] = birds[0][\rateSocket];
		endpoints[\deckB_rate] = birds[1][\rateSocket];

		^(scale: scale, width: width, height: height,
			ringCenter: ringCenter, ringR: ringR,
			eye: ringCenter,           // §6.1: the ring's qFreq/qChaos pupil
			castles: castles, ringSegments: ringSegments,
			ringSamples: ringSamples, ringBox: ringBox,
			cocos: cocos, roots: roots, sprout: sprout, birds: birds,
			endpoints: endpoints)
	}
}
