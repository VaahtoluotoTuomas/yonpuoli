// CQNightGeometryTest — the §15 topology truths of the Night map, asserted
// without a window: ring order and direction, cocos left/right, roots below,
// birds above, the full 38-endpoint inventory and the §16.10 hit spacing.

CQNightGeometryTest : UnitTest {

	prDistance { |a, b|
		^sqrt((b.x - a.x).squared + (b.y - a.y).squared)
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
