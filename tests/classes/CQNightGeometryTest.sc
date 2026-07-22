// CQNightGeometryTest — the §15 topology truths of the Night map, asserted
// without a window: ring order and direction, cocos left/right, roots below,
// birds above, the full 38-endpoint inventory and the §16.10 hit spacing.

CQNightGeometryTest : UnitTest {

	prDistance { |a, b|
		^sqrt((b.x - a.x).squared + (b.y - a.y).squared)
	}

	// mirrors CQGuiNightMap.prMakeEndpoint: jack 26x26 anchored (13,13),
	// socket 20x20 anchored (9,10), a lamped socket widens to 34
	prEndpointBox { |name, center|
		var spec = CQGuiNightMap.endpointSpec(name);
		var jack = spec[\kind] == \jack;
		var width = if(spec[\lamp] == true) { 34 } { if(jack) { 26 } { 20 } };
		var height = if(jack) { 26 } { 20 };
		var anchor = if(jack) { Point(13, 13) } { Point(9, 10) };
		^Rect(center.x - anchor.x, center.y - anchor.y, width, height)
	}

	prRectDistance { |point, rect|
		var dx = max(max(rect.left - point.x, point.x - rect.right), 0);
		var dy = max(max(rect.top - point.y, point.y - rect.bottom), 0);
		^sqrt(dx.squared + dy.squared)
	}

	test_topologyTruths {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var centers = geo[\castles].collect { |c| c[\center] };
			var ringCenter = geo[\ringCenter];
			var angles = centers.collect { |c|
				(c.y - ringCenter.y).atan2(c.x - ringCenter.x)
			};
			this.assert(centers[0].y < centers.drop(1).collect { |c| c.y }.minItem,
				"castle 1 sits at the top of the ring (%x%)".format(*size));
			5.do { |k|
				var step = (angles.wrapAt(k + 1) - angles[k]).wrap(-pi, pi);
				this.assert(step > 0.9 and: { step < 1.62 },
					"castle % -> % advances ~72 deg clockwise, the LocalIn flow"
						.format(k + 1, (k % 5) + 2));
			};
			this.assert(geo[\cocos][0][\center].x < (ringCenter.x - geo[\ringR]),
				"coco 1 spiral lies left of the ring");
			this.assert(geo[\cocos][1][\center].x > (ringCenter.x + geo[\ringR]),
				"coco 2 spiral lies right of the ring");
			this.assert(geo[\endpoints][\envA].y > (size[1] * 0.75)
				and: { geo[\endpoints][\envB].y > (size[1] * 0.75) },
				"env roots grow at the bottom");
			this.assert(geo[\endpoints][\envA].x < ringCenter.x
				and: { geo[\endpoints][\envB].x > ringCenter.x },
				"envA roots left, envB right");
			this.assert(geo[\endpoints][\deckA_rate].y < (size[1] * 0.2)
				and: { geo[\endpoints][\deckB_rate].y < (size[1] * 0.2) },
				"deck birds fly at the top");
			this.assert(geo[\endpoints][\deckA_rate].x < ringCenter.x
				and: { geo[\endpoints][\deckB_rate].x > ringCenter.x },
				"bird A left, bird B right");
		};
	}

	test_endpointInventoryAndSpacing {
		var geo = CQNightGeometry.compute(1280, 570);
		var points = geo[\endpoints];
		var names = CQMatrix.sourceNames ++ CQMatrix.destNames;
		var stage = Rect(0, 0, 1280, 570);
		this.assertEquals(points.size, 38,
			"the night map places exactly 16 jacks and 22 sockets");
		names.do { |name|
			this.assert(points[name].notNil,
				"canonical endpoint % has a night home".format(name));
			this.assert(stage.containsPoint(points[name]),
				"% lands inside the minimum stage".format(name));
		};
		names.do { |a, i|
			names.drop(i + 1).do { |b|
				this.assert(this.prDistance(points[a], points[b]) >= 24,
					"% and % keep the 24 px hit spacing (16.10)".format(a, b));
			};
		};
	}

	test_determinism {
		var one = CQNightGeometry.compute(1280, 570);
		var two = CQNightGeometry.compute(1280, 570);
		var names = CQMatrix.sourceNames ++ CQMatrix.destNames;
		this.assert(names.every { |name|
			one[\endpoints][name] == two[\endpoints][name]
		}, "the seeded archipelago is pixel-identical across computations");
	}

	test_fireflyDirection {
		var geo = CQNightGeometry.compute(1280, 570);
		var castle2 = geo[\castles][1][\center];
		var near = CQNightGeometry.fireflyPoint(geo, 0.05);
		var farther = CQNightGeometry.fireflyPoint(geo, 0.12);
		this.assert(this.prDistance(farther, castle2)
			< this.prDistance(near, castle2),
			"a growing phase carries the firefly from castle 1 toward castle 2");
	}

	test_spiralShape {
		var geo = CQNightGeometry.compute(1280, 570);
		var coco = geo[\cocos][0];
		var mouthR = this.prDistance(coco[\mouth], coco[\center]);
		var orgR = this.prDistance(coco[\orgSpiralPoint], coco[\center]);
		this.assertEquals(coco[\segments].size, 10,
			"2.5 turns draw as ten quarter-turn cubics");
		this.assert((mouthR - coco[\outerR]).abs < 1,
			"the mouth opens at the spiral's outer rim");
		this.assert(orgR > (coco[\outerR] * 0.3)
			and: { orgR < (coco[\outerR] * 0.8) },
			"the org fruit hangs from an inner turn");
		this.assert(geo[\cocos][0][\winding] == (geo[\cocos][1][\winding].neg),
			"coco 2 mirrors coco 1's winding");
	}

	test_touchHomes {
		var geo = CQNightGeometry.compute(1280, 570);
		var coco = geo[\cocos][0];
		var onSpiral = coco[\spiralSamples][30];
		[0, 1].do { |i|
			var shell = geo[\cocos][i];
			this.assert(shell[\shellBox].containsPoint(shell[\center])
				and: { shell[\shellBox].containsPoint(shell[\mouth]) }
				and: { shell[\shellBox].containsPoint(shell[\chevron]) },
				"coco % shell box holds its heart, mouth and chevron".format(i + 1));
			shell[\crescents].do { |crescent, m|
				this.assert(CQNightGeometry.distance(crescent,
					shell[\moons][m]) >= 24,
					"coco % crescent % keeps hit spacing from its moon"
						.format(i + 1, m));
			};
		};
		this.assert(CQNightGeometry.spiralDistance(coco, onSpiral) < 1,
			"a spiral sample lies on the winding surface");
		this.assert(CQNightGeometry.spiralDistance(coco,
			coco[\center] + Point(0, 4)) > 8,
			"the heart region is not the winding surface");
		this.assert(CQNightGeometry.dialHit(geo[\castles][2],
			CQNightGeometry.polar(geo[\castles][2][\center],
				geo[\castles][2][\dialR], 0.3)),
			"a tap on the dial radius hits the dial");
		this.assert(CQNightGeometry.dialHit(geo[\castles][2],
			geo[\castles][2][\center]).not,
			"the castle's center is not the dial");
		// the body is the §6 solo target and wears the §4.5 hover echo — one
		// geometry for both, reaching `bodyMargin` px outside the drawn blob so
		// it answers the approaching hand
		this.assert(CQNightGeometry.bodyHit(geo[\castles][2],
			geo[\castles][2][\center]),
			"the castle's center is inside its own body");
		this.assert(CQNightGeometry.bodyHit(geo[\castles][2],
			geo[\castles][2][\center]
				+ Point(geo[\castles][2][\bodyRx] + 4, 0)),
			"the body answers just outside the drawn blob (the margin)");
		this.assert(CQNightGeometry.bodyHit(geo[\castles][2],
			geo[\castles][2][\center] + Point(geo[\castles][2][\bodyRx]
				+ CQNightGeometry.bodyMargin + 4, 0)).not,
			"past the margin it is water again");
		this.assert(CQNightGeometry.bodyHit(geo[\castles][2],
			geo[\castles][2][\center], 0),
			"an explicit zero margin still finds the flesh");
		this.assert(CQNightGeometry.bodyHit(geo[\castles][2],
			geo[\castles][2][\box].leftTop + Point(2, 2)).not,
			"the layer box's corner is water, not flesh");
		geo[\castles].do { |castle, k|
			// the hover echo is drawn on that same boundary — it must fit inside
			// the castle's own opaque layer or it would be clipped
			this.assert(castle[\box].width
				>= (2 * (castle[\bodyRx] + CQNightGeometry.bodyMargin))
				and: { castle[\box].height
					>= (2 * (castle[\bodyRy] + CQNightGeometry.bodyMargin)) },
				"castle %'s hover echo fits inside its own layer".format(k + 1));
		};
		// each body is taken by its own drawn outline (§16.6) — the cue and the
		// action share one geometry on all three
		this.assert(CQNightGeometry.shellRimHit(coco,
			coco[\center] + Point(coco[\outerR], 0)),
			"the shell's rim is its body handle");
		this.assert(CQNightGeometry.shellRimHit(coco,
			coco[\center] + Point(coco[\outerR] * 0.5, 0)).not,
			"the shell's inside is winding surface and paper, not the rim");
		this.assert(CQNightGeometry.shellRimHit(coco,
			coco[\shellBox].leftTop + Point(2, 2)).not,
			"the shell box's corner is open water");
		this.assert(CQNightGeometry.ringBodyHit(geo, geo[\ringSamples][7]),
			"a point on the river is the ring's body handle");
		this.assert(CQNightGeometry.ringBodyHit(geo, geo[\eye]).not,
			"the water inside the ring is not the ring's line");
		this.assert(CQNightGeometry.ringDistance(geo, geo[\eye]) == inf,
			"the radial reject spares the language a polyline scan mid-water");
		// the bird filename needs a line box a 10 px mono face fits into
		geo[\birds].do { |bird, i|
			this.assert(bird[\labelRect].height >= 18,
				"bird %'s filename has room for its ascenders".format(i + 1));
			this.assert(bird[\labelRect].width >= 200,
				"bird %'s filename box holds a normal name whole".format(i + 1));
			this.assert(bird[\labelRect].top > (bird[\rateSocket].y + 10),
				"bird %'s filename clears the rate socket's own view".format(i + 1));
		};
		// the root arrives on a unit vector, so its tip jack can redraw the
		// passage that its opaque view would otherwise swallow — and the side
		// twig must not disappear under that same box
		geo[\roots].do { |root, i|
			var arrival = root[\arrival];
			var tipBox = Rect.aboutPoint(root[\points].last, 13, 13);
			this.assertFloatEquals(
				sqrt(arrival.x.squared + arrival.y.squared), 1.0,
				"root %'s arrival is a unit direction".format(i + 1), 0.0001);
			this.assert(arrival.y < 0,
				"root % arrives climbing toward its tip".format(i + 1));
			this.assert(tipBox.containsPoint(root[\branch][0]).not
				and: { tipBox.containsPoint(root[\branch][1]).not }
				and: { (root[\branch][0].y > tipBox.bottom)
					and: { root[\branch][1].y > tipBox.bottom } },
				"root %'s twig stays clear of its tip jack's opaque box"
					.format(i + 1));
		};
		this.assertEquals(geo[\eye], geo[\ringCenter],
			"the ring's eye sits at the ring's center");
		this.assert(geo[\birds][0][\box].containsPoint(geo[\birds][0][\center])
			and: { CQNightGeometry.distance(geo[\birds][0][\levelCrescent],
				geo[\endpoints][\deckA_rate]) >= 24 },
			"the bird box holds the bird; its crescent clears the rate moon");
	}

	// The reel mark crosses the groove instead of riding it (G2 gate): against
	// the read head's bead it read as the same object with two functions.
	// A tick needs the line's own direction, which the shell's radius only
	// matches a quarter turn at a time.
	test_reelNotchStandsAcrossItsLine {
		var geo = CQNightGeometry.compute(1280, 570);
		var coco = geo[\cocos][0];
		[0.1, 0.35, 0.6, 0.9].do { |norm|
			var normal = CQNightGeometry.spiralNormal(coco, norm);
			var point = CQNightGeometry.spiralPoint(coco, norm);
			var probe = CQNightGeometry.normalProbe;
			var ahead = CQNightGeometry.spiralPoint(coco, norm + probe);
			var behind = CQNightGeometry.spiralPoint(coco, norm - probe);
			var length = max(0.001, CQNightGeometry.distance(behind, ahead));
			var radius = max(0.001,
				CQNightGeometry.distance(point, coco[\center]));
			var dot = (((ahead.x - behind.x) * normal.x)
				+ ((ahead.y - behind.y) * normal.y)) / length;
			var radial = (((point.x - coco[\center].x) * normal.x)
				+ ((point.y - coco[\center].y) * normal.y)) / radius;
			var tip = CQNightGeometry.spiralDistance(coco,
				point + (normal * 6));
			this.assert((sqrt(normal.x.squared + normal.y.squared) - 1).abs < 1e-6,
				"the notch's normal is a unit vector at %".format(norm));
			this.assert(dot.abs < 0.05,
				"the notch stands across the line, not along it (at %)"
					.format(norm));
			// a groove climbing 30 px per turn leans ~8° off the circle at
			// the inner turns, and a norm landing exactly on a polyline
			// vertex (0.35 is one) can add half a segment's 15° — so the
			// tick must read as nearly radial, the independent check that
			// this is not the tangent by accident
			this.assert(radial.abs > 0.95,
				"the notch points out of the shell, not around it (at %)"
					.format(norm));
			// 2.5 turns across 75 px of radius = 30 px between grooves, so a
			// hovered 6 px half-reach leaves its own line without touching
			// the neighbouring one
			this.assert(tip > 3 and: { tip < 15 },
				"a hovered notch tip clears its line but not the next (at %)"
					.format(norm));
		};
	}

	// the G2.3 clip fixes, pinned: no opaque endpoint view may cover the
	// chevron glyph, the inner clock's face or any of its eight bead homes
	test_inkClearances {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			geo[\cocos].do { |coco, i|
				var glyph = Rect.aboutPoint(coco[\chevron], 6, 5);
				var jack = Rect.aboutPoint(coco[\mouth], 13, 13);
				this.assert((glyph.intersects(jack)).not,
					"coco % chevron glyph clears the mouth jack's view (%x%)"
						.format(i + 1, *size));
				this.assert(coco[\shellBox].contains(
					Rect.aboutPoint(coco[\chevron], 10, 10)),
					"coco % chevron touch box stays inside the shell (%x%)"
						.format(i + 1, *size));
			};
			geo[\castles].do { |castle, k|
				var n = k + 1, center = castle[\center];
				var names = [("tri" ++ n).asSymbol, ("cas" ++ n).asSymbol,
					("castle" ++ n ++ "_fm").asSymbol,
					("castle" ++ n ++ "_spesal").asSymbol];
				names.do { |name|
					var box = this.prEndpointBox(name, geo[\endpoints][name]);
					this.assert(this.prRectDistance(center, box)
						>= (castle[\dialR] + 1),
						"castle % clock face clears the % view (%x%)"
							.format(n, name, *size));
					8.do { |i|
						var bead = CQNightGeometry.polar(center,
							castle[\dialR], (22.5 + (i * 45)).degrad);
						this.assert(this.prRectDistance(bead, box) >= 4,
							"castle % bead % + halo clears the % view (%x%)"
								.format(n, i, name, *size));
					};
				};
			};
		};
	}

	test_reelPearl {
		var geo = CQNightGeometry.compute(1280, 570);
		[0, 1].do { |i|
			var coco = geo[\cocos][i];
			var monotonic = true;
			var radii = [0, 0.25, 0.5, 0.75, 1].collect { |norm|
				this.prDistance(CQNightGeometry.spiralPoint(coco, norm),
					coco[\center])
			};
			radii.doAdjacentPairs { |a, b| if(a >= b) { monotonic = false } };
			this.assert(CQNightGeometry.spiralPoint(coco, 0)
				== coco[\spiralSamples].first,
				"coco % pearl at norm 0 rests on the innermost eye".format(i + 1));
			this.assert(this.prDistance(
				CQNightGeometry.spiralPoint(coco, 1), coco[\mouth]) < 0.001,
				"coco % pearl at norm 1 reaches the mouth".format(i + 1));
			this.assert(monotonic,
				"coco % pearl rides outward monotonically".format(i + 1));
		};
	}

	// The almanac corner (§16.6) lives on water the bodies leave free: right of
	// coco 2's own box, past bird B's name box, inside the stage at every
	// scale. The sowing moon must be takeable by the same rim it draws — flash
	// halo included, or the layer would clip its own light — and G3's compass
	// water must already be empty and clear: the home is pinned now so the
	// corner cannot grow into it later, but nothing is drawn there, because a
	// dead control breaks §4.5 and §16.12 at once.
	test_almanacCorner {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var almanac = geo[\almanac];
			var box = almanac[\box], compass = almanac[\compass];
			var moon = almanac[\moon], moonR = almanac[\moonR];
			var names = CQMatrix.sourceNames ++ CQMatrix.destNames;
			[[\corner, box], [\water, compass]].do { |pair|
				var rect = pair[1];
				this.assert(rect.left >= 0 and: { rect.right <= size[0] }
					and: { rect.top >= 0 } and: { rect.bottom <= size[1] },
					"the almanac % stays on the stage (%x%)"
						.format(pair[0], *size));
				this.assert(rect.left > geo[\cocos][1][\shellBox].right,
					"the almanac % opens right of coco 2's own box (%x%)"
						.format(pair[0], *size));
				geo[\birds].do { |bird, i|
					this.assert(rect.intersects(bird[\dropRect]).not,
						"bird %'s drop rect keeps clear of the almanac % (%x%)"
							.format(i + 1, pair[0], *size));
				};
				names.do { |name|
					this.assert(rect.containsPoint(geo[\endpoints][name]).not,
						"no jack or socket lands in the almanac % (% at %x%)"
							.format(pair[0], name, *size));
				};
				geo[\roots].do { |root, i|
					(root[\points] ++ root[\branch]).do { |point|
						this.assert(rect.containsPoint(point).not,
							"root % keeps out of the almanac % (%x%)"
								.format(i + 1, pair[0], *size));
					};
				};
			};
			this.assert(box.intersects(compass).not
				and: { compass.top >= box.bottom },
				"G3's compass water waits below the constellation (%x%)"
					.format(*size));
			this.assertFloatEquals(compass.width, 140 * geo[\scale],
				"the reserved pad is §7.1's own 140 px (%x%)".format(*size), 0.001);
			// the moon is a body: the disc it draws is the disc it answers on,
			// and its own layer holds the 120 ms flash halo whole
			this.assert(box.contains(Rect.aboutPoint(moon,
				moonR + CQNightGeometry.moonHalo,
				moonR + CQNightGeometry.moonHalo)),
				"the moon and its flash halo fit inside their layer (%x%)"
					.format(*size));
			this.assert(CQNightGeometry.moonHit(almanac, moon)
				and: { CQNightGeometry.moonHit(almanac,
					moon + Point(moonR - 2, 0)) }
				and: { CQNightGeometry.moonHit(almanac,
					moon + Point(moonR + 4, 0)).not },
				"the moon answers inside its drawn rim and nowhere else (%x%)"
					.format(*size));
			// the sower constellation was removed on 2026-07-22: a preset is a
			// recipe, not a body, and the Inspector is one toolbar click away in
			// either view. Nothing but the moon lives in this corner.
			this.assert(almanac[\stars].isNil
				and: { CQNightGeometry.respondsTo(\starAt).not },
				"no preset stars remain in the sky (%x%)".format(*size));
		};
	}

	// The compass water at G3: the §7.1 pad is drawn now, with its corner
	// seeds and the weather row beneath — all inside their own layer box, all
	// on water no body reaches, at both reference stage sizes.
	test_compassWater {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var almanac = geo[\almanac];
			var pad = almanac[\compass], box = almanac[\compassBox];
			var names = CQMatrix.sourceNames ++ CQMatrix.destNames;
			var marks = #[grow, breed, wither].collect { |n| almanac[\weather][n] };
			this.assert(box.left >= 0 and: { box.right <= size[0] }
				and: { box.top >= 0 } and: { box.bottom <= size[1] },
				"the compass layer stays on the stage (%x%)".format(*size));
			this.assert(box.contains(pad),
				"the layer holds the whole pad (%x%)".format(*size));
			this.assert(box.left > geo[\cocos][1][\shellBox].right,
				"the compass water opens right of coco 2's box (%x%)".format(*size));
			geo[\birds].do { |bird, i|
				this.assert(box.intersects(bird[\dropRect]).not,
					"bird %'s drop rect keeps clear of the compass (%x%)"
						.format(i + 1, *size));
			};
			names.do { |name|
				this.assert(box.containsPoint(geo[\endpoints][name]).not,
					"no jack or socket lands in the compass water (% at %x%)"
						.format(name, *size));
			};
			geo[\roots].do { |root, i|
				(root[\points] ++ root[\branch]).do { |point|
					this.assert(box.containsPoint(point).not,
						"root % keeps out of the compass water (%x%)"
							.format(i + 1, *size));
				};
			};
			// the four seeds sit inside the pad in bilinear order TL TR BL BR,
			// and each seed answers at its own point
			almanac[\stamps].do { |stamp, c|
				this.assert(pad.containsPoint(stamp),
					"seed % sits inside the pad (%x%)".format(c, *size));
				this.assertEquals(CQNightGeometry.stampHit(almanac, stamp), c,
					"seed % answers at its own point (%x%)".format(c, *size));
			};
			this.assert(almanac[\stamps][0].x < almanac[\stamps][1].x
				and: { almanac[\stamps][0].y < almanac[\stamps][2].y },
				"seed order is TL TR BL BR — the bilinear's own frame (%x%)"
					.format(*size));
			// the weather marks hang below the field, inside the layer, apart
			// (the 24 px hit law), and answer as themselves
			marks.do { |mark, i|
				this.assert(mark.y > pad.bottom,
					"weather mark % hangs below the field (%x%)".format(i, *size));
				this.assert(box.containsPoint(mark),
					"weather mark % lives inside the layer (%x%)".format(i, *size));
			};
			marks.doAdjacentPairs { |a, b|
				this.assert(CQNightGeometry.distance(a, b) >= 24,
					"weather marks keep the 24 px hit law apart (%x%)".format(*size));
			};
			this.assertEquals(
				CQNightGeometry.weatherHit(almanac, almanac[\weather][\breed]),
				\breed, "the graft answers at its own mark (%x%)".format(*size));
			// pad coordinates are the bilinear's 0..1 frame
			this.assertEquals(
				CQNightGeometry.compassNorm(almanac, pad.leftTop), Point(0, 0),
				"pad TL is the norm origin (%x%)".format(*size));
			this.assertEquals(
				CQNightGeometry.compassNorm(almanac,
					Point(pad.right, pad.bottom)), Point(1, 1),
				"pad BR is norm (1,1) (%x%)".format(*size));
			// the moon's sky layer and the compass layer never overlap: a moon
			// flash may not re-ink the field, nor a voyage the moon
			this.assert(box.intersects(almanac[\box]).not,
				"the sky and the water are separate layers (%x%)".format(*size));
		};
	}

	// The spesal coupling chord (§16.6): both ends stop outside their castle's
	// own opaque box — a line under an opaque layer is a line nobody sees — it
	// bows inward far enough that an *adjacent* pair can never be mistaken for
	// the river itself, and it never crowds the storm iris at the centre.
	test_spesalChordsBowInsideTheRiver {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var eye = geo[\eye], ringR = geo[\ringR];
			var floor = ringR * CQNightGeometry.chordClearance;
			5.do { |k|
				(0..4).removing(k).do { |partner|
					var path = CQNightGeometry.spesalChord(geo, k, partner);
					var curveMid = (path[0] * 0.125) + (path[1] * 0.375)
						+ (path[2] * 0.375) + (path[3] * 0.125);
					this.assert(geo[\castles][k][\box].containsPoint(path[0]).not
						and: { geo[\castles][partner][\box]
							.containsPoint(path[3]).not },
						"chord % -> % leaves both opaque boxes (%x%)"
							.format(k, partner, *size));
					this.assert(this.prDistance(path[1], eye) >= (floor - 0.001),
						"chord % -> % keeps its distance from the eye (%x%)"
							.format(k, partner, *size));
					// the river is the outer line; an inner chord must read as a
					// different mark even when it joins two neighbours
					this.assert(CQNightGeometry.ringDistance(geo, curveMid) > 12,
						"chord % -> % never doubles the river's own line (%x%)"
							.format(k, partner, *size));
					// the storm iris draws out to roughly 20 px at full chaos
					this.assert(this.prDistance(curveMid, eye) > 20,
						"chord % -> % passes clear of the iris itself (%x%)"
							.format(k, partner, *size));
				};
			};
		};
	}

	// the travelling light rides the chord from the castle being read to the
	// one reading it, and it is a function of the phase alone — no clock of its
	// own anywhere in the geometry (§16.9)
	test_spesalChordCarriesItsLight {
		var geo = CQNightGeometry.compute(1280, 570);
		var path = CQNightGeometry.spesalChord(geo, 3, 0);
		var early = CQNightGeometry.chordPoint(path, 0.2);
		var late = CQNightGeometry.chordPoint(path, 0.8);
		this.assertEquals(CQNightGeometry.chordPoint(path, 0), path[0],
			"the light leaves the castle being read");
		this.assertEquals(CQNightGeometry.chordPoint(path, 1), path[3],
			"and arrives at the one reading it");
		this.assert(this.prDistance(early, path[0])
			< this.prDistance(late, path[0]),
			"it travels one way along the chord, never back");
		this.assertEquals(CQNightGeometry.chordPoint(path, 2), path[3],
			"a phase past the end clamps instead of running off the chord");
		this.assert(CQNightGeometry.chordLaps > 0,
			"a chord is crossed a whole number of times per river lap");
	}

	// one spesal state may never mean two different couplings in two skins:
	// the night reads the Day constellation's own adapter, and defines no
	// partner rule of its own anywhere
	test_oneCouplingRuleForBothSkins {
		this.assert(CQNightGeometry.respondsTo(\partnerIndex).not
			and: { CQGuiNightMap.respondsTo(\partnerIndex).not }
			and: { CQNightGeometry.respondsTo(\trackMode).not },
			"the night derives no partner or mode rule of its own");
		this.assertEquals(CQGuiCastleConstellation.partnerIndex(2, 5),
			(0..4).removing(2)[1],
			"state 5 on castle 3 reads its second remaining neighbour");
		this.assert(CQGuiCastleConstellation.trackMode(5)
			and: { CQGuiCastleConstellation.trackMode(1).not },
			"states 4..7 are track-and-hold, 0..3 sample-and-hold");
	}

	test_rangeLetter {
		this.assertEquals(CQNightGeometry.rangeLetter(\infra), "i", "infra = i");
		this.assertEquals(CQNightGeometry.rangeLetter(\balcium), "b", "balcium = b");
		this.assertEquals(CQNightGeometry.rangeLetter(\audio), "a", "audio = a");
		this.assertEquals(CQNightGeometry.rangeLetter(nil), "",
			"an unknown range stays silent");
	}

	test_sproutState {
		this.assertEquals(CQNightGeometry.sproutState(false, 0), \hidden,
			"no mic configured: no sprout at all");
		this.assertEquals(CQNightGeometry.sproutState(true, 0), \dormant,
			"micChan set with micGain 0: dormant whisper bud");
		this.assertEquals(CQNightGeometry.sproutState(true, 0.5), \live,
			"micChan set and micGain up: danger-red sprout");
		this.assertEquals(CQNightGeometry.sproutState(false, 1), \hidden,
			"gain without a configured mic still draws nothing");
	}
}
