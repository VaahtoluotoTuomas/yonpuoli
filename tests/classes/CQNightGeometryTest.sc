// CQNightGeometryTest — the §15 topology truths of the Night map, asserted
// without a window: ring order and direction, cocos left/right, roots below,
// birds above, the full 38-endpoint inventory and the §16.10 hit spacing.

CQNightGeometryTest : UnitTest {

	prDistance { |a, b|
		^sqrt((b.x - a.x).squared + (b.y - a.y).squared)
	}

	// the very box CQGuiNightMap.prMakeEndpoint builds — one table, no mirror,
	// so a flooded view size can never pass a clearance test it would fail live
	prEndpointBox { |name, center|
		var spec = CQGuiNightMap.endpointSpec(name);
		^CQNightGeometry.endpointBox(center, spec[\kind], spec[\lamp])
	}

	// how far out a point lies in the pool's own elliptical frame: 1 is exactly
	// on the mean shore, so every reach assert reads the same at any scale
	prPoolReach { |pool, point|
		var u = (point.x - pool[\center].x) / pool[\rx];
		var v = (point.y - pool[\center].y) / pool[\ry];
		^sqrt((u * u) + (v * v))
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
				+ CQNightMeasures.bodyMargin + 4, 0)).not,
			"past the margin it is water again");
		this.assert(CQNightGeometry.bodyHit(geo[\castles][2],
			geo[\castles][2][\center], 0),
			"an explicit zero margin still finds the flesh");
		this.assert(CQNightGeometry.bodyHit(geo[\castles][2],
			geo[\castles][2][\box].leftTop + Point(2, 2)).not,
			"the layer box's corner is water, not flesh");
		geo[\castles].do { |castle, k|
			// the hover echo is drawn on that same boundary — it must fit inside
			// the castle's transparent view bounds or it would still be clipped
			this.assert(castle[\box].width
				>= (2 * (castle[\bodyRx] + CQNightMeasures.bodyMargin))
				and: { castle[\box].height
					>= (2 * (castle[\bodyRy] + CQNightMeasures.bodyMargin)) },
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
			var tipBox = CQNightGeometry.endpointBox(root[\points].last, \jack);
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
				var jack = CQNightGeometry.endpointBox(coco[\mouth], \jack);
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
	// halo included, or the layer would clip its own light. Since §9 the moon
	// rides the birds' own altitude band and the tide pool hangs below it, so
	// the two are a composition rather than a column, and their layers still
	// have to keep off each other: a moon flash may never re-ink the water.
	legacy_almanacCorner {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var almanac = geo[\almanac];
			var box = almanac[\box], compass = almanac[\compassBox];
			var moon = almanac[\moon], moonR = almanac[\moonR];
			var pool = almanac[\pool];
			var names = CQMatrix.sourceNames ++ CQMatrix.destNames;
			[[\sky, box], [\water, compass]].do { |pair|
				var rect = pair[1];
				this.assert(rect.left >= 0 and: { rect.right <= size[0] }
					and: { rect.top >= 0 } and: { rect.bottom <= size[1] },
					"the almanac % stays on the stage (%x%)"
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
			this.assert(box.intersects(compass).not,
				"the sky and the water are two places, not one corner (%x%)"
					.format(*size));
			this.assertFloatEquals(pool[\rx],
				CQNightMeasures.poolRx * geo[\scale],
				"the pool carries §7.1's water at this scale (%x%)"
					.format(*size), 0.001);
			// the pool is TANGENT to the stage's lower edge — that edge is the
			// waterline the roots already run past, and it is what says the
			// plants and the pool are in one water without drawing a thing
			this.assertFloatEquals(pool[\center].y + pool[\ry], size[1],
				"the pool sits on the waterline (%x%)".format(*size), 0.001);
			// ...and the shore's own wobble crosses it, so the water reads as
			// continuing under the frame rather than stopping at a rim
			this.assert(CQNightGeometry.sampleSegments(pool[\segments], 8)
				.any { |p| p.y > size[1] },
				"the shore runs past the waterline (%x%)".format(*size));
			// the bodies are far apart, which is the whole point of §9's move
			this.assert(CQNightGeometry.distance(moon, pool[\center])
				> (size[1] * 0.5),
				"the moon and the pool are two features, not a cluster (%x%)"
					.format(*size));
			this.assertFloatEquals(moon.y,
				CQNightMeasures.birdAltitude * geo[\scale],
				"the moon rides the birds' own altitude band (%x%)"
					.format(*size), 0.001);
			geo[\birds].do { |bird, i|
				this.assertFloatEquals(moon.y, bird[\center].y,
					"the sky reads bird — bird — moon (bird %, %x%)"
						.format(i + 1, *size), 0.001);
			};
			// the moon is a body: the disc it draws is the disc it answers on,
			// and its own layer holds the 120 ms flash halo whole
			this.assert(box.contains(Rect.aboutPoint(moon,
				moonR + CQNightMeasures.moonHalo,
				moonR + CQNightMeasures.moonHalo)),
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

	// The tide pool since §9: the §7.1 water is a seeded, wobbled shore with
	// its four memories seated on it at the diagonal winds and the weather
	// scattered around it — all inside their own layer box, all on water no
	// body reaches, at both reference stage sizes.
	legacy_compassWater {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var almanac = geo[\almanac];
			var pool = almanac[\pool], box = almanac[\compassBox];
			var names = CQMatrix.sourceNames ++ CQMatrix.destNames;
			var marks = #[grow, breed, wither].collect { |n| almanac[\weather][n] };
			var shore = CQNightGeometry.sampleSegments(pool[\segments], 8);
			this.assert(box.left >= 0 and: { box.right <= size[0] }
				and: { box.top >= 0 } and: { box.bottom <= size[1] },
				"the compass layer stays on the stage (%x%)".format(*size));
			// the layer holds every stretch of shore that is ON the stage; the
			// lowest stretch runs past the waterline on purpose and Qt cutting
			// it there IS the drawing (§9)
			this.assert(shore.every { |point|
				(point.y > size[1]) or: { box.containsPoint(point) } },
				"the layer holds the shore down to the waterline (%x%)"
					.format(*size));
			// the pool's real neighbours in the bay's bottom pocket — every one
			// of these is an opaque layer or an opaque view, so touching any of
			// them means one body eating another's ink
			this.assert(box.left > CQNightGeometry
				.endpointBox(geo[\endpoints][\envA], \jack).right,
				"the pool opens right of envA's own jack (%x%)".format(*size));
			this.assert(box.right < geo[\ringBox].left,
				"the pool stops before the river's own layer (%x%)".format(*size));
			this.assert(box.top > geo[\cocos][0][\shellBox].bottom,
				"the pool lies below coco 1's shell (%x%)".format(*size));
			geo[\cocos][0][\crescents].do { |point, m|
				var half = CQNightMeasures.crescentExtent * 0.5;
				this.assert(box.intersects(
					Rect.aboutPoint(point, half, half)).not,
					"the pool clears coco 1's tide crescent % (%x%)"
						.format(m, *size));
			};
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
			// the four memories are seated ON the shore at the diagonal winds,
			// in bilinear order TL TR BL BR, and each answers at its own point
			almanac[\stamps].do { |stamp, c|
				var reach = this.prPoolReach(pool, stamp);
				this.assert(reach < 1,
					"seed % is seated inside its own shore (%x%)".format(c, *size));
				this.assert(reach > 0.6,
					"seed % is ON the shore, not adrift in the pool (%x%)"
						.format(c, *size));
				this.assertEquals(CQNightGeometry.stampHit(almanac, stamp), c,
					"seed % answers at its own point (%x%)".format(c, *size));
			};
			this.assert(almanac[\stamps][0].x < almanac[\stamps][1].x
				and: { almanac[\stamps][0].y < almanac[\stamps][2].y },
				"seed order is TL TR BL BR — the bilinear's own frame (%x%)"
					.format(*size));
			// The weather row is gone and the marks did not stay together: they
			// went where each one ACTS (§9). Rain and sun grow and wither the
			// live bay, so they are sky weather and they keep the moon company.
			// The graft breeds two stamped memories and takes its parents as
			// seed taps, so it stays on the water where those parents are.
			#[grow, wither].do { |name|
				var mark = almanac[\weather][name];
				this.assertEquals(almanac[\skyMarks][name], mark,
					"% is sky weather, on the moon's own layer (%x%)"
						.format(name, *size));
				this.assert(almanac[\box].containsPoint(mark),
					"% lives inside the sky layer (%x%)".format(name, *size));
				this.assert(CQNightGeometry.distance(mark, almanac[\moon])
					< (almanac[\moonR] * 3),
					"% keeps the moon company rather than drifting (%x%)"
						.format(name, *size));
				this.assert(this.prPoolReach(pool, mark) > 3,
					"% is nowhere near the water (%x%)".format(name, *size));
			};
			this.assert(almanac[\skyMarks][\breed].isNil,
				"the graft did not follow them into the sky (%x%)".format(*size));
			// the graft stands off the DRAWN shore, not the mean ellipse: the
			// shore wobbles ±6 %, so a mark placed off the average can still
			// land on the line it is supposed to stand beside
			block { |done|
				var mark = almanac[\weather][\breed];
				var reach = shore.collect { |point|
					CQNightGeometry.distance(mark, point) }.minItem;
				this.assert(this.prPoolReach(pool, mark) > 1,
					"the graft stands off the shore (%x%)".format(*size));
				this.assert(reach >= 8,
					"the graft keeps clear of the drawn shore (%x%)".format(*size));
				this.assert(reach <= (40 * geo[\scale]),
					"...but still belongs to this pool (%x%)".format(*size));
				this.assert(box.containsPoint(mark),
					"the graft lives inside the water's layer (%x%)".format(*size));
				this.assert(mark.y > pool[\center].y
					and: { mark.x > almanac[\stamps][2].x }
					and: { mark.x > almanac[\stamps][3].x },
					"the graft waits at the lower shore past its parents (%x%)"
						.format(*size));
			};
			marks.do { |a, i|
				marks.drop(i + 1).do { |b|
					this.assert(CQNightGeometry.distance(a, b) >= 24,
						"weather marks keep the 24 px hit law apart (%x%)"
							.format(*size));
				};
			};
			// A row is a row whatever you call it: leaving it means no weather
			// mark may line up with another mark, with a seed or with the moon
			// on either axis. This is the whole reason §9 scattered them.
			#[grow, breed, wither].do { |name|
				var mark = almanac[\weather][name];
				(almanac[\stamps] ++ [almanac[\moon]]
					++ marks.reject { |m| m == mark }).do { |other|
					this.assert((mark.x - other.x).abs >= 6
						or: { (mark.y - other.y).abs >= 6 },
						"% shares no axis with % (%x%)"
							.format(name, other, *size));
				};
			};
			this.assertEquals(
				CQNightGeometry.weatherHit(almanac, almanac[\weather][\breed]),
				\breed, "the graft answers at its own mark (%x%)".format(*size));
			// The tide is bilinear in a square poured on a round body: the shore
			// at a diagonal wind IS a corner, and every corner is reachable —
			// bit-exact, which is what makes a stamp recallable (rake 13).
			4.do { |c|
				var corner = Point(c % 2, c div: 2);
				var place = CQNightGeometry.compassPointAt(almanac, corner);
				var beyond = pool[\center]
					+ ((place - pool[\center]) * 1.08);   // in the catch band
				this.assertFloatEquals(this.prPoolReach(pool, place), 1.0,
					"corner % lives on the shore (%x%)".format(c, *size), 0.001);
				this.assertEquals(CQNightGeometry.compassNorm(almanac, place),
					corner, "the shore at corner % is that corner (%x%)"
						.format(c, *size));
				this.assert(CQNightGeometry.compassHit(almanac, beyond),
					"the catch band answers past corner % (%x%)".format(c, *size));
				this.assertEquals(CQNightGeometry.compassNorm(almanac, beyond),
					corner, "...and holds exactly at that corner (%x%)"
						.format(c, *size));
			};
			this.assertEquals(
				CQNightGeometry.compassNorm(almanac, pool[\center]),
				Point(0.5, 0.5), "the pool's middle is the tide's middle (%x%)"
					.format(*size));
			// the moon's sky layer and the water's layer never overlap: a moon
			// flash may not re-ink the pool, nor a voyage the moon
			this.assert(box.intersects(almanac[\box]).not,
				"the sky and the water are separate layers (%x%)".format(*size));
		};
	}

	// The moon is no longer one item in an almanac panel. It is a solitary
	// weather body beyond bird B: slightly higher than the birds, clear of the
	// name/drop body, and large enough to play without becoming a second sun.
	// Rain and sun are transient faces of its vertical gesture, so their
	// anchors fit the moon layer but no static weather target survives.
	test_moonIsASolitaryWeatherBody {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var moon = geo[\almanac][\moon];
			var moonR = geo[\almanac][\moonR];
			var box = geo[\almanac][\box];
			var bird = geo[\birds][1];
			var previews = geo[\almanac][\weatherPreview];
			this.assert(box.left >= 0 and: { box.right <= size[0] }
				and: { box.top >= 0 } and: { box.bottom <= size[1] },
				"the remote moon stays inside the sky (%x%)".format(*size));
			this.assert(moon.x > bird[\dropRect].right
				and: { box.intersects(bird[\dropRect]).not },
				"the moon lives beyond bird B, never beside the centred organism (%x%)"
					.format(*size));
			this.assert(moon.y < bird[\center].y,
				"the moon is slightly above the birds, not aligned into a row (%x%)"
					.format(*size));
			this.assertFloatEquals(moonR,
				CQNightMeasures.moonRadius * geo[\scale],
				"the moon keeps the reduced playable body at this scale (%x%)"
					.format(*size), 0.001);
			this.assert(box.contains(Rect.aboutPoint(moon,
				moonR + CQNightMeasures.moonHalo,
				moonR + CQNightMeasures.moonHalo)),
				"the moon and its flash halo fit the sky layer (%x%)"
					.format(*size));
			#[grow, wither].do { |name|
				this.assert(box.contains(Rect.aboutPoint(previews[name],
					CQNightMeasures.markHitReach,
					CQNightMeasures.markHitReach)),
					"the transient % face cannot clip at the moon (%x%)"
						.format(name, *size));
			};
			this.assert(geo[\almanac][\skyMarks].isNil
				and: { geo[\almanac][\weather].isNil }
				and: { geo[\almanac][\pool].isNil },
				"no almanac panel, static weather mark or pool body survives (%x%)"
					.format(*size));
			this.assert(CQNightGeometry.moonHit(geo[\almanac], moon)
				and: { CQNightGeometry.moonHit(geo[\almanac],
					moon + Point(moonR + 3, 0)).not },
				"the moon answers on its drawn body only (%x%)".format(*size));
		};
	}

	// The compass is now the bay's large invisible tide field. Its four memory
	// seeds are the only resting marks: no shore, outline or graft. The field
	// deliberately passes under the organism, while each seed itself lands in
	// an open wind and maps directly to one exact bilinear corner.
	test_tideFieldBelongsToTheWholeBay {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var tide = geo[\almanac][\compass];
			var stamps = geo[\almanac][\stamps];
			var gate = geo[\tideGate], gateBox = geo[\tideGateBox];
			var endpointNames = CQMatrix.sourceNames ++ CQMatrix.destNames;
			this.assert(tide.left >= 0 and: { tide.right <= size[0] }
				and: { tide.top >= 0 } and: { tide.bottom <= size[1] },
				"the tide's four winds stay on the stage (%x%)".format(*size));
			this.assert(tide.width > (size[0] * 0.35)
				and: { tide.height > (size[1] * 0.6) },
				"the tide is a landscape field, not a disguised pad (%x%)"
					.format(*size));
			this.assertEquals(stamps,
				[tide.leftTop, tide.rightTop, tide.leftBottom, tide.rightBottom],
				"the four seeds ARE the tide's TL TR BL BR corners (%x%)"
					.format(*size));
			stamps.do { |stamp, c|
				this.assertEquals(CQNightGeometry.stampHit(
					geo[\almanac], stamp), c,
					"seed % answers at its own open wind (%x%)"
						.format(c, *size));
				geo[\castles].do { |castle, k|
					this.assert(castle[\box].containsPoint(stamp).not,
						"seed % clears castle % (%x%)".format(c, k + 1, *size));
				};
				geo[\cocos].do { |coco, i|
					this.assert(coco[\shellBox].containsPoint(stamp).not,
						"seed % clears cocoon % (%x%)".format(c, i + 1, *size));
				};
				geo[\birds].do { |bird, i|
					this.assert(bird[\dropRect].containsPoint(stamp).not,
						"seed % clears bird % (%x%)".format(c, i + 1, *size));
				};
				endpointNames.do { |name|
					var spec = CQGuiNightMap.endpointSpec(name);
					this.assert(CQNightGeometry.endpointBox(
						geo[\endpoints][name], spec[\kind], spec[\lamp])
						.containsPoint(stamp).not,
						"seed % clears endpoint % (%x%)".format(c, name, *size));
				};
				this.assertEquals(CQNightGeometry.compassNorm(
					geo[\almanac], stamp), Point(c % 2, c div: 2),
					"seed % recalls its corner bit-exact (%x%)"
						.format(c, *size));
			};
			this.assert(CQNightGeometry.compassHit(geo[\almanac], tide.center),
				"open water inside the four winds answers");
			this.assert(CQNightGeometry.compassHit(geo[\almanac],
				tide.leftTop - Point(2, 2)).not,
				"water outside the four winds remains ordinary ground");
			block { |done|
				var middle = CQNightGeometry.compassNorm(
					geo[\almanac], tide.center);
				this.assertFloatEquals(middle.x, 0.5,
					"the landscape's middle is the bilinear middle (x)", 1e-9);
				this.assertFloatEquals(middle.y, 0.5,
					"the landscape's middle is the bilinear middle (y)", 1e-9);
			};
			this.assertEquals(CQNightGeometry.compassPointAt(
				geo[\almanac], Point(0.25, 0.75)),
				Point(tide.left + (tide.width * 0.25),
					tide.top + (tide.height * 0.75)),
				"field and tide use one direct coordinate frame");
			this.assertFloatEquals(gate.x, geo[\ringCenter].x,
				"the tide gill grows on the organism's centre line", 0.001);
			this.assert(tide.containsPoint(gate)
				and: { Rect(0, 0, size[0], size[1]).contains(gateBox) },
				"the tide gill belongs to the field and stays on the stage (%x%)"
					.format(*size));
			this.assert(CQNightGeometry.tideGateHit(geo, gate)
				and: { CQNightGeometry.tideGateHit(geo,
					gate + Point(CQNightMeasures.tideGateHit + 2, 0)).not },
				"the gill answers on its one deliberate touch body (%x%)"
					.format(*size));
			geo[\castles].do { |castle, k|
				this.assert(gateBox.intersects(castle[\box]).not,
					"the tide gill clears castle % (%x%)".format(k + 1, *size));
			};
			geo[\cocos].do { |coco, i|
				this.assert(gateBox.intersects(coco[\shellBox]).not,
					"the tide gill clears cocoon % (%x%)".format(i + 1, *size));
			};
			endpointNames.do { |name|
				var spec = CQGuiNightMap.endpointSpec(name);
				this.assert(gateBox.intersects(CQNightGeometry.endpointBox(
					geo[\endpoints][name], spec[\kind], spec[\lamp])).not,
					"the tide gill clears endpoint % (%x%)".format(name, *size));
			};
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
					this.assertEquals(path[0], geo[\castles][k][\center],
						"chord % -> % begins beneath the source flesh (%x%)"
							.format(k, partner, *size));
					this.assertEquals(path[3], geo[\castles][partner][\center],
						"chord % -> % ends beneath the reader flesh (%x%)"
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

	// The tide is bilinear in a square; the pool it is poured on is round (§9).
	// The elliptical grid mapping carries one into the other, and the whole
	// reason to use it instead of a radial squash is that **the rim at a
	// diagonal is exactly a corner** — a stamped memory that cannot be reached
	// bit-exact is a memory the pad has lost (rake 13). These fixed points are
	// asserted on the pure math, with no window and no almanac anywhere near.
	legacy_poolMappingKeepsItsCorners {
		var root = 1 / 2.sqrt;
		var corners = [Point(-1, -1), Point(1, -1), Point(-1, 1), Point(1, 1)];
		this.assertEquals(CQNightGeometry.squareToDisc(Point(0, 0)), Point(0, 0),
			"the pool's centre is the square's centre");
		this.assertEquals(CQNightGeometry.discToSquare(Point(0, 0)), Point(0, 0),
			"...and back again");
		corners.do { |corner|
			// the corner leaves the square at the diagonal rim of the disc
			var rim = CQNightGeometry.squareToDisc(corner);
			var back = CQNightGeometry.discToSquare(rim);
			this.assertFloatEquals(rim.x, corner.x * root,
				"corner % sits on the rim at its own diagonal (x)"
					.format(corner), 1e-9);
			this.assertFloatEquals(rim.y, corner.y * root,
				"corner % sits on the rim at its own diagonal (y)"
					.format(corner), 1e-9);
			this.assertFloatEquals(sqrt(rim.x.squared + rim.y.squared), 1.0,
				"corner % lands exactly ON the shore, not inside it"
					.format(corner), 1e-9);
			// and the round trip is exact, which is what makes a corner recall
			// bit-exact rather than nearly
			this.assertFloatEquals(back.x, corner.x,
				"the diagonal rim maps back to corner % (x)".format(corner), 1e-9);
			this.assertFloatEquals(back.y, corner.y,
				"the diagonal rim maps back to corner % (y)".format(corner), 1e-9);
		};
		// the axes are fixed points too: the rim on an axis is an edge midpoint
		[[Point(1, 0), Point(1, 0)], [Point(-1, 0), Point(-1, 0)],
			[Point(0, 1), Point(0, 1)], [Point(0, -1), Point(0, -1)]].do { |pair|
			var square = CQNightGeometry.discToSquare(pair[0]);
			this.assertFloatEquals(square.x, pair[1].x,
				"the rim at % is an edge, not a corner (x)".format(pair[0]), 1e-9);
			this.assertFloatEquals(square.y, pair[1].y,
				"the rim at % is an edge, not a corner (y)".format(pair[0]), 1e-9);
		};
		// interior round trip: the geography has to be one-to-one, or the same
		// place on the pool would grow two different patches
		[-0.9, -0.4, 0, 0.3, 0.75, 1].do { |x|
			[-1, -0.6, -0.1, 0.5, 0.95].do { |y|
				var square = Point(x, y);
				var back = CQNightGeometry.discToSquare(
					CQNightGeometry.squareToDisc(square));
				this.assertFloatEquals(back.x, x,
					"(%, %) survives the round trip (x)".format(x, y), 1e-6);
				this.assertFloatEquals(back.y, y,
					"(%, %) survives the round trip (y)".format(x, y), 1e-6);
			};
		};
		// nothing inside the disc may map outside the square, at any angle
		36.do { |i|
			var angle = i / 36 * 2pi;
			[0.25, 0.6, 0.99, 1.0].do { |r|
				var square = CQNightGeometry.discToSquare(
					Point(cos(angle) * r, sin(angle) * r));
				this.assert(square.x.abs <= (1 + 1e-9)
					and: { square.y.abs <= (1 + 1e-9) },
					"the disc at angle % radius % stays in the square"
						.format(i, r));
			};
		};
	}

	// ---- §8's collision matrix (NIGHT_STANDALONE_PLAN §8) ----
	// The spring flood re-bases the night's measures, and a measure is safe only
	// if the bodies it grows still miss each other. These asserts were written
	// BEFORE any constant moved, at both reference stage sizes, so a measure
	// that stops fitting fails here instead of in the user's hands. Every castle
	// irregularity is seeded inside `compute`, so all of it is deterministic.

	// every truly opaque body layer the night paints on, in z order
	prOpaqueLayers { |geo|
		var layers = List.new;
		layers.add([\ring, geo[\ringBox]]);
		layers.add([\eye, Rect.aboutPoint(geo[\eye],
			CQNightMeasures.eyeLayerR, CQNightMeasures.eyeLayerR)]);
		geo[\cocos].do { |coco, i|
			layers.add([("shell" ++ (i + 1)).asSymbol, coco[\shellBox]])
		};
		geo[\birds].do { |bird, i|
			layers.add([("bird" ++ (i + 1)).asSymbol, bird[\box]])
		};
		layers.add([\almanac, geo[\almanac][\box]]);
		^layers.asArray
	}

	// An opaque layer covers whatever is under it, so two of them overlapping
	// means one body is quietly eating another's ink. Castles are intentionally
	// absent: their paper is clear and only their irregular painted flesh masks
	// the river. The storm iris keeps its own layer over the ring's centre so a
	// storm repaint never re-inks the river.
	test_floodOpaqueLayersMissEachOther {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var stage = Rect(0, 0, size[0], size[1]);
			var layers = this.prOpaqueLayers(geo);
			layers.do { |a, i|
				this.assert(stage.contains(a[1]),
					"the % layer stays on the stage (%x%)".format(a[0], *size));
				layers.drop(i + 1).do { |b|
					var pair = [a[0], b[0]];
					var nested = pair.includes(\ring) and: { pair.includes(\eye) };
					if(nested.not) {
						this.assert(a[1].intersects(b[1]).not,
							"opaque layers % and % keep off each other (%x%)"
								.format(a[0], b[0], *size));
					};
				};
			};
		};
	}

	// §16.7's drop border is drawn around the bird AND its name, so that whole
	// rect has to be free water: over a castle or the river it would ring ink
	// that does not accept the file.
	test_floodBirdsClearTheLandscape {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var stage = Rect(0, 0, size[0], size[1]);
			geo[\birds].do { |bird, i|
				var drop = bird[\dropRect];
				this.assert(stage.contains(drop),
					"bird %'s drop target stays on the stage (%x%)"
						.format(i + 1, *size));
				this.assert(drop.intersects(geo[\ringBox]).not,
					"bird % flies clear of the river's own layer (%x%)"
						.format(i + 1, *size));
				geo[\castles].do { |castle, k|
					this.assert(drop.intersects(castle[\box]).not,
						"bird % keeps off castle %'s box (%x%)"
							.format(i + 1, k + 1, *size));
				};
				geo[\cocos].do { |coco, c|
					this.assert(drop.intersects(coco[\shellBox]).not,
						"bird % keeps off coco %'s shell (%x%)"
							.format(i + 1, c + 1, *size));
				};
			};
		};
	}

	// The small opaque boxes are allowed to graze each other — 24 px of hit
	// spacing is less than two grown views — but never so far that one swallows
	// the other's drawn dot. A centre under a neighbour's box is a jack the eye
	// can see and the hand cannot reach.
	test_floodSmallBoxesNeverSwallowACentre {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var names = CQMatrix.sourceNames ++ CQMatrix.destNames;
			var stage = Rect(0, 0, size[0], size[1]);
			var crescents = List.new;
			var boxes = names.collect { |name|
				[name, this.prEndpointBox(name, geo[\endpoints][name]),
					geo[\endpoints][name]]
			};
			var half = CQNightMeasures.crescentExtent * 0.5;
			geo[\cocos].do { |coco, i|
				coco[\crescents].do { |point, m|
					crescents.add([("coco" ++ (i + 1) ++ "c" ++ m).asSymbol, point])
				};
			};
			geo[\birds].do { |bird, i|
				crescents.add([("deck" ++ (i + 1) ++ "level").asSymbol,
					bird[\levelCrescent]])
			};
			boxes.do { |a|
				this.assert(stage.contains(a[1]),
					"% keeps its whole view on the stage (%x%)"
						.format(a[0], *size));
			};
			boxes.do { |a, i|
				boxes.drop(i + 1).do { |b|
					this.assert(a[1].containsPoint(b[2]).not
						and: { b[1].containsPoint(a[2]).not },
						"neither % nor % swallows the other's centre (%x%)"
							.format(a[0], b[0], *size));
				};
			};
			// a tide crescent and the moon beside it are two different hands on
			// two different values. Their opaque views are allowed to graze at a
			// corner — the crescent is drawn last and the sliver it takes is
			// empty paper — but never far enough to reach a drawn mark, which is
			// what §16.10's 24 px between centres buys.
			crescents.do { |c|
				var box = Rect.aboutPoint(c[1], half, half);
				this.assert(stage.contains(box),
					"crescent % keeps its whole view on the stage (%x%)"
						.format(c[0], *size));
				boxes.do { |b|
					this.assert(box.containsPoint(b[2]).not
						and: { b[1].containsPoint(c[1]).not },
						"neither crescent % nor % swallows the other's centre (%x%)"
							.format(c[0], b[0], *size));
					this.assert(CQNightGeometry.distance(c[1], b[2]) >= 24,
						"crescent % keeps the 24 px hit law from % (%x%)"
							.format(c[0], b[0], *size));
				};
			};
			crescents.do { |a, i|
				crescents.drop(i + 1).do { |b|
					this.assert(CQNightGeometry.distance(a[1], b[1])
						>= CQNightMeasures.crescentExtent,
						"crescents % and % keep their views apart (%x%)"
							.format(a[0], b[0], *size));
				};
			};
		};
	}

	// The roots climb into the same lower water the shells hang their tide
	// crescents in, and their tips carry jacks. Nothing down there may cross.
	test_floodRootsClearTheLowerWater {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var stage = Rect(0, 0, size[0], size[1]);
			var half = CQNightMeasures.crescentExtent * 0.5;
			geo[\roots].do { |root, i|
				var tipBox = CQNightGeometry.endpointBox(root[\points].last, \jack);
				this.assert(stage.contains(tipBox),
					"root %'s tip jack stays on the stage (%x%)"
						.format(i + 1, *size));
				geo[\cocos].do { |coco, c|
					coco[\crescents].do { |point, m|
						var box = Rect.aboutPoint(point, half, half);
						(root[\points] ++ root[\branch]).doAdjacentPairs { |a, b|
							this.assert(box.containsPoint(a).not
								and: { box.containsPoint(b).not },
								"root % misses coco % crescent % (%x%)"
									.format(i + 1, c + 1, m, *size));
						};
					};
					this.assert(root[\points].every { |point|
						coco[\shellBox].containsPoint(point).not },
						"root % grows clear of coco %'s shell (%x%)"
							.format(i + 1, c + 1, *size));
				};
				this.assert(root[\points].every { |point|
					geo[\ringBox].containsPoint(point).not },
					"root % stops below the river's layer (%x%)"
						.format(i + 1, *size));
			};
		};
	}

	// §16.10's hit law is not a minimum-stage courtesy: it holds wherever the
	// window is dragged to.
	test_floodHitSpacingAtBothSizes {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			var names = CQMatrix.sourceNames ++ CQMatrix.destNames;
			names.do { |a, i|
				names.drop(i + 1).do { |b|
					this.assert(this.prDistance(geo[\endpoints][a],
						geo[\endpoints][b]) >= 24,
						"% and % keep the 24 px hit spacing (%x%)"
							.format(a, b, *size));
				};
			};
		};
	}

	// The night's own playing targets, measured as the hand meets them: each
	// one has to be findable without aiming, and none may reach into another.
	test_floodTouchTargetsAreReachable {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			geo[\cocos].do { |coco, i|
				var heart = coco[\heartRadius];
				var mark = CQNightMeasures.markHitReach;
				var scale = geo[\scale];
				var inner = CQNightMeasures.spiralInnerR * scale;
				// radial distance between two turns of the groove
				var pitch = (coco[\outerR] - inner) / CQNightGeometry.turns;
				var inkReach = [CQNightMeasures.notchHoverReach,
					CQNightMeasures.struckBase + CQNightMeasures.struckRange,
					CQNightMeasures.skipGlyphR].maxItem + 2;
				this.assert(CQNightGeometry.distance(coco[\chevron],
					coco[\skipMark]) >= (2 * mark),
					"coco %'s flip and skip marks never share a press (%x%)"
						.format(i + 1, *size));
				this.assert(CQNightGeometry.distance(coco[\mouth],
					coco[\chevron]) > (mark + (CQNightMeasures.jackExtent * 0.5)),
					"coco %'s chevron clears the mouth jack's own view (%x%)"
						.format(i + 1, *size));
				// The heart is checked BEFORE the winding surface (§4.5's chain
				// in CQGuiNightTouch.shellDown), so its disc quietly takes the
				// innermost turn of the groove with it — which is right, since a
				// speed grab there sits at norm 0 anyway. What it may never do is
				// swallow a SECOND turn: past that, a visible stretch of groove
				// would stop answering the winding gesture at all.
				this.assert(heart < (inner + pitch),
					"coco %'s heart takes the innermost turn and no more (%x%)"
						.format(i + 1, *size));
				// ...and the grab band has to stay inside one turn for the same
				// reason, from the other side
				this.assert(CQNightMeasures.windGrab < (pitch * 0.5),
					"coco %'s winding grab answers one turn, not two (%x%)"
						.format(i + 1, *size));
				this.assert(coco[\shellBox].contains(
					Rect.aboutPoint(coco[\skipMark], mark, mark)),
					"coco %'s skip box stays inside the shell layer (%x%)"
						.format(i + 1, *size));
				// The layer's slack past the rim is trimmed to what the shell
				// actually draws, because those pixels are what the castles and
				// the almanac live on. So it has to be pinned: the widest mark
				// leaving the rim is the hammered read head's arms, then the
				// notch's hover reach, then the skip glyph — plus the seal ring's
				// own stroke. If a future mark grows, this fails before it clips.
				this.assert(coco[\shellBox].contains(Rect.aboutPoint(coco[\center],
					coco[\outerR] + inkReach, coco[\outerR] + inkReach)),
					"coco %'s layer holds every mark its rim carries (%x%)"
						.format(i + 1, *size));
			};
			geo[\castles].do { |castle, k|
				var flesh = min(castle[\bodyRx], castle[\bodyRy]);
				// the inner clock and the body around it are two different taps.
				// What is drawn — the face, its ticks and the lit state bead with
				// its halo — has to sit inside the drawn blob, or the clock stops
				// reading as the castle's own inside.
				this.assert((castle[\dialR] + max(CQNightMeasures.clockTickReach,
					CQNightMeasures.clockBeadHaloR)) < flesh,
					"castle %'s clock is drawn inside its own flesh (%x%)"
						.format(k + 1, *size));
				// and its annulus may not answer a press the eye reads as water:
				// the dial's reach stays inside the body's own (§4.5 — a cue that
				// covers more than its action is a cue that lies)
				this.assert((castle[\dialR] + CQNightMeasures.clockHit)
					<= (flesh + CQNightMeasures.bodyMargin),
					"castle %'s clock steals no press from the water (%x%)"
						.format(k + 1, *size));
			};
			this.assert(CQNightMeasures.eyeHit
				<= CQNightMeasures.eyeLayerR,
				"the eye answers no further than its own layer draws (%x%)"
					.format(*size));
		};
	}

	test_rangeRippleGeometryAndClearance {
		[[1280, 570], [1920, 855]].do { |size|
			var geo = CQNightGeometry.compute(size[0], size[1]);
			geo[\castles].do { |castle, k|
				var n = k + 1;
				var hit = CQNightGeometry.rangeBox(castle);
				var drawn = Rect.aboutPoint(castle[\rangeMark],
					CQNightMeasures.rangeRippleWidth * 0.5,
					CQNightMeasures.rangeRippleHeight);
				var fmName = ("castle" ++ n ++ "_fm").asSymbol;
				var spesalName = ("castle" ++ n ++ "_spesal").asSymbol;
				var fm = this.prEndpointBox(fmName, geo[\endpoints][fmName]);
				var spesal = this.prEndpointBox(spesalName,
					geo[\endpoints][spesalName]);
				this.assert(CQNightGeometry.rangeHit(castle, castle[\rangeMark]),
					"castle % ripple answers on its drawn centre (%x%)"
						.format(n, *size));
				this.assert(CQNightGeometry.rangeHit(castle,
					castle[\rangeMark]
						+ Point(CQNightMeasures.rangeHitX + 1, 0)).not,
					"castle % ripple answers no wider than its cue (%x%)"
						.format(n, *size));
				this.assert(hit.contains(drawn),
					"castle % ripple ink fits inside its touch cue (%x%)"
						.format(n, *size));
				this.assert(castle[\box].contains(hit),
					"castle % ripple fits its transparent paper (%x%)"
						.format(n, *size));
				this.assertFloatEquals(castle[\rangeMark].y
					- castle[\blobBottom], CQNightMeasures.rangeBodyGap,
					"castle % ripple breathes from its actual seeded edge (%x%)"
						.format(n, *size), 0.0001);
				this.assert(hit.intersects(fm).not,
					"castle % ripple clears the fm socket (%x%)"
						.format(n, *size));
				this.assert(hit.intersects(spesal).not,
					"castle % ripple clears the lamped spesal socket (%x%)"
						.format(n, *size));
				this.assert(
					CQNightGeometry.distance(castle[\center], castle[\rangeMark])
						- CQNightMeasures.rangeHitY
						> (castle[\dialR] + CQNightMeasures.clockHit),
					"castle % ripple clears the clock annulus (%x%)"
						.format(n, *size));
				this.assert(CQNightGeometry.bodyHit(castle,
					castle[\rangeMark] - Point(0, CQNightMeasures.rangeHitY)),
					"castle % ripple overlaps the body's cue and therefore needs priority (%x%)"
						.format(n, *size));
			};
		};
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
