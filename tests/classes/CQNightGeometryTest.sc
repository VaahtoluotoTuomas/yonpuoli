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
		// the bird filename needs a line box a 10 px mono face fits into
		geo[\birds].do { |bird, i|
			this.assert(bird[\labelRect].height >= 18,
				"bird %'s filename has room for its ascenders".format(i + 1));
			this.assert(bird[\labelRect].top
				> (bird[\rateSocket].y + 10),
				"the filename clears the rate socket's own view".format(i + 1));
		};
		// the root arrives on a unit vector, so its tip jack can redraw the
		// passage that its opaque view would otherwise swallow
		geo[\roots].do { |root, i|
			var arrival = root[\arrival];
			this.assertFloatEquals(
				sqrt(arrival.x.squared + arrival.y.squared), 1.0,
				"root %'s arrival is a unit direction".format(i + 1), 0.0001);
			this.assert(arrival.y < 0,
				"root % arrives climbing toward its tip".format(i + 1));
		};
		this.assertEquals(geo[\eye], geo[\ringCenter],
			"the ring's eye sits at the ring's center");
		this.assert(geo[\birds][0][\box].containsPoint(geo[\birds][0][\center])
			and: { CQNightGeometry.distance(geo[\birds][0][\levelCrescent],
				geo[\endpoints][\deckA_rate]) >= 24 },
			"the bird box holds the bird; its crescent clears the rate moon");
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
