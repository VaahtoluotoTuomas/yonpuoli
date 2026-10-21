// Read-only semantic ownership and geometry for the inspection layer.
// Musical hit testing and performance gestures keep their existing owners.
CQInfoGroups {
	*all {
		^[[\group, \coco, 0], [\group, \coco, 1],
			[\group, \bird, 0], [\group, \bird, 1],
			[\group, \root, 0], [\group, \root, 1],
			[\group, \castles, 0], [\group, \eye, 0], [\group, \moon, 0],
			[\group, \tide, 0], [\group, \master, 0]]
	}
	*owner { |target|
		var kind, key, s;
		if(target.isNil) { ^nil };
		kind = target[0]; key = target[1]; s = key.asString;
		if(#[group, tour, advanced, shortcuts].includes(kind)) { ^target };
		if(kind == \cable) { ^[\tour, CQInfoTour.indexOf(\ports)] };
		if(kind == \master) { ^[\group, \master, 0] };
		if(kind == \bird) { ^[\group, \bird, key] };
		if(#[preamp, source].includes(kind)) { ^[\group, \root, key] };
		if(#[dust, skip].includes(kind)) { ^[\group, \coco, key] };
		if(kind == \castle) { ^[\group, \castles, 0] };
		if(kind == \eye) { ^[\group, \eye, 0] };
		if(kind == \moon) { ^[\group, \moon, 0] };
		if(#[seed, tideGate].includes(kind)) { ^[\group, \tide, 0] };
		if(kind == \body and: { key == \quantussy }) { ^[\group, \castles, 0] };
		if(s.beginsWith("coco")) { ^[\group, \coco, s[4].digit - 1] };
		if(s.beginsWith("org")) { ^[\group, \coco, s[3].digit - 1] };
		if(s.beginsWith("deck")) { ^[\group, \bird, if(s[4] == $A) { 0 } { 1 }] };
		if(s.beginsWith("pre")) { ^[\group, \root, if(s[3] == $A) { 0 } { 1 }] };
		if(s.beginsWith("env")) { ^[\group, \root, if(s[3] == $A) { 0 } { 1 }] };
		if(s.beginsWith("tri") or: { s.beginsWith("cas") }
			or: { s.size > 1 and: { s[0] == $c and: { s[1].isDecDigit } } }) {
			^[\group, \castles, 0]
		};
		if(#[qFreq, qChaos].includes(key)) { ^[\group, \eye, 0] };
		if(CQGuiPanels.masterParamNames.includes(key)) { ^[\group, \master, 0] };
		^nil
	}

	// Whole bodies answer even between their small performance controls.
	// The eye stays independent of the surrounding castle ring. Components
	// win over crossing cables here; open-water cables enter the shared guide at its port introduction.
	*at { |point, geometry, raw|
		var g = geometry;
		if(g.isNil) { ^this.owner(raw) };
		if(CQNightGeometry.distance(point, g[\eye]) < 35) { ^[\group, \eye, 0] };
		g[\cocos].do { |c, i|
			if(CQNightGeometry.distance(point, c[\center]) <= c[\outerR]) { ^[\group, \coco, i] };
		};
		g[\castles].do { |c|
			if(CQNightGeometry.polygonHit(point, c[\blobSamples])) { ^[\group, \castles, 0] };
		};
		g[\birds].do { |b, i| if(b[\box].containsPoint(point)) { ^[\group, \bird, i] } };
		^this.owner(raw)
	}

	*union { |rects|
		var result;
		rects.do { |rect| result = if(result.isNil) { rect } { result.union(rect) } };
		^result
	}
	*regions { |group, regions|
		^regions.select { |region| this.owner(region[\target]) == group }
	}
	*protected { |group, geometry, regions|
		var kind = group[1], i = group[2], rects, c;
		if(kind == \master) { ^[] };
		rects = this.regions(group, regions).collect { |r|
			if(r[\path].notNil) { CQNightGeometry.prBoundsOf(r[\path], 10) } { r[\rect] }
		};
		switch(kind,
			\coco, {
				c = geometry[\cocos][i];
				rects = rects ++ [Rect.aboutPoint(c[\center], c[\levelR] + 8, c[\levelR] + 8),
					geometry[\dusts][i][\box]];
			},
			\castles, { rects = rects ++ geometry[\castles].collect { |b| b[\box] } },
			\bird, { rects = rects ++ [geometry[\birds][i][\box]] },
			\root, { rects = rects ++ [geometry[\roots][i][\knotHitBox]] }
		);
		// The tide's four seeds are disjoint: reserve each seed and the gill,
		// rather than pretending their entire background is one solid subject.
		^if(kind == \tide) { rects } { [this.union(rects)].reject(_.isNil) }
	}
	*points { |refs, regions|
		^refs.collect { |ref|
			var region = regions.detect { |r| r[\target] == ref };
			region !? { region[\point] }
		}.reject(_.isNil)
	}
	*diagramBounds { |group, page, geometry, regions|
		var rects;
		if(group[1] == \castles and: { page == 1 }) {
			^geometry[\castles][0][\box].insetBy(-12, -12)
		};
		rects = this.protected(group, geometry, regions);
		^this.union(rects)
	}
	*project { |point, source, destination|
		var scale = min(destination.width / source.width, destination.height / source.height);
		^destination.center + ((point - source.center) * scale)
	}
	*labelPoint { |at, rect, occupied, axis|
		var direction = atan2(at.y - rect.center.y, at.x - rect.center.x), result;
		// Gesture rows name an axis, so their leaders must depict that axis.
		// Try both directions without rotating a horizontal/vertical gesture.
		if(axis.notNil) {
			[36, 48, 60, 24, 18].do { |radius|
				[1, -1].do { |sign|
					var point = at + if(axis == \horizontal) { Point(radius * sign, 0) } { Point(0, radius * sign) };
					if(result.isNil and: { rect.insetBy(8, 8).containsPoint(point) }
						and: { occupied.every { |prior| CQNightGeometry.distance(point, prior) >= 20 } }) { result = point };
				};
			};
			^result ? at
		};
		[18, 30, 44, 58].do { |radius|
			16.do { |n|
				var point = CQNightGeometry.polar(at, radius, direction + (n * 2pi / 16));
				if(result.isNil and: { rect.insetBy(8, 8).containsPoint(point) }
					and: { occupied.every { |prior| CQNightGeometry.distance(point, prior) >= 20 } }) { result = point };
			};
		};
		^result ? at
	}
	// Prefer the nearest side of the subject, aligned with its centre. Other
	// components may sit behind the card; every part of its own subject stays
	// exposed. Stage edges are fallbacks, never the primary destination.
	*place { |size, width, height, protected, preferRight = false|
		var xs = [12, size.x - width - 12], ys = [12, size.y - height - 12], candidates;
		var subject = this.union(protected), score;
		if(height > (size.y - 24) or: { width > (size.x - 24) }) { ^nil };
		protected.do { |r|
			xs = xs ++ [r.right + 12, r.left - width - 12, (r.center.x - (width * 0.5)).clip(12, size.x - width - 12)];
			ys = ys ++ [r.bottom + 12, r.top - height - 12, (r.center.y - (height * 0.5)).clip(12, size.y - height - 12)];
		};
		candidates = xs.collect { |x| ys.collect { |y| Rect(x, y, width, height) } }.flat.select { |r|
			Rect(8, 8, size.x - 16, size.y - 16).contains(r)
				and: { protected.every { |subject| r.intersects(subject.insetBy(-6, -6)).not } }
		};
		score = { |r|
			if(subject.isNil) { r.left + (r.top * 0.2) } {
				var dx = max(0, max(subject.left - r.right, r.left - subject.right));
				var dy = max(0, max(subject.top - r.bottom, r.top - subject.bottom));
				(((dx.squared + dy.squared).sqrt - 12).abs * 1000)
					+ CQNightGeometry.distance(r.center, subject.center)
					+ if(dx > 0) { 0 } { 100 }
					+ if((r.center.x > subject.center.x) == preferRight) { 0 } { 1 }
			}
		};
		^candidates.sort { |a, b| score.value(a) < score.value(b) }.first
	}
}
