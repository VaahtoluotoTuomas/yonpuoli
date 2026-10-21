// Native vector miniatures and quiet shape-following inspection outlines.
CQInkInfoMap {
	*outline { |group, g, strength = 0.7, lineWidth = 1.5|
		var kind = group[1], i = group[2], c;
		Pen.strokeColor = CQInk.infoColor(0.6).copy.alpha_(strength);
		Pen.width = lineWidth;
		switch(kind,
			\coco, {
				c = g[\cocos][i];
				Pen.addOval(Rect.aboutPoint(c[\center], c[\outerR] + 5, c[\outerR] + 5)); Pen.stroke;
				CQInkNight.strokeSegments(g[\dusts][i][\segments]);
			},
			\castles, {
				CQInkNight.strokeSegments(g[\ringSegments]);
				g[\castles].do { |b| CQInkNight.strokeSegments(b[\blobSegments]) };
			},
			\root, {
				c = g[\roots][i];
				c[\sourceStrands].do { |s| CQInkNight.strokeSegments([s]) };
				CQNightGeometry.preampLobeBoxes(c[\knot], 0).do { |r| Pen.addOval(r.insetBy(-3, -3)) }; Pen.stroke;
			},
			\bird, { CQInkNight.drawBird(g[\birds][i], 0@0, true) },
			\eye, { Pen.addOval(Rect.aboutPoint(g[\eye], 32, 32)); Pen.stroke },
			\moon, {
				c = g[\almanac]; Pen.addOval(Rect.aboutPoint(c[\moon], c[\moonR] + 5, c[\moonR] + 5)); Pen.stroke;
			},
			\tide, {
				(g[\almanac][\stamps] ++ [g[\tideGate]]).do { |at| Pen.addOval(Rect.aboutPoint(at, 13, 13)) };
				Pen.stroke;
			}
		);
	}
	*diagram { |group, page, g, regions, source, destination|
		var kind = group[1], i = group[2], c;
		var scale = min(destination.width / source.width, destination.height / source.height);
		var crescents = CQGuiNightMap.crescentNames(0) ++ CQGuiNightMap.crescentNames(1)
			++ #[deckALevel, deckBLevel];
		CQInkInfo.fit(source, destination, {
			switch(kind,
				\coco, {
					c = g[\cocos][i];
					CQInkNight.drawShell(c, 0@0, true, performance: (level: 1, organ: 0.4));
					CQInkNight.drawLevelShore(c, 0.6);
					CQInkNight.drawDust(g[\dusts][i], 0@0, 0.7, 0.6);
					// Projected outline keeps a miniature winding legible.
					Pen.strokeColor = CQInk.palette(\night)[\line]; Pen.width = 0.8 / scale;
					CQInkNight.strokeSegments(c[\segments]);
					#[slotQPaths, slotXPaths].do { |key, n|
						var path = g[\drone][key][i];
						CQInkNight.drawFlowGill(path.last, path.last - path[2], 0.5,
							if(n == 0) { \tri } { \cas }, palette: CQInk.palette(\night));
					};
				},
				\castles, {
					if(page == 0) {
						Pen.strokeColor = CQInk.palette(\night)[\whisper]; Pen.width = 0.7 / scale;
						CQInkNight.strokeSegments(g[\ringSegments]);
						// A fixed example, labelled as such in the prose. Native
						// paths and symbols explain the network without telemetry.
						CQInkNight.drawSpesalChords(#[0, 1, 4, 2, 6].collect { |state, k|
							(path: CQNightGeometry.spesalChord(g,
								CQGuiCastleConstellation.partnerIndex(k, state), k),
								track: CQGuiCastleConstellation.trackMode(state))
						}, 0.2);
						CQInkNight.drawEye(g[\eye]);
					};
					g[\castles].do { |castle, n|
						if(page == 0 or: { n == 0 }) {
							CQInkNight.drawCastle(castle, 0@0, nil,
								if(page == 0) { #[0, 1, 4, 2, 6][n] } { 0 }, \infra);
							Pen.strokeColor = CQInk.palette(\night)[\line]; Pen.width = 0.8 / scale;
							CQInkNight.strokeSegments(castle[\blobSegments]);
						};
					};
				},
				\bird, { CQInkNight.drawBird(g[\birds][i], 0@0) },
				\root, { CQInkNight.drawPreampRoot(g[\roots][i], 0@0, sourceIndex: 0) },
				\eye, { CQInkNight.drawEye(g[\eye]) },
				\moon, { CQInkNight.drawSowingMoon(g[\almanac][\moon], g[\almanac][\moonR], 0.35) }
			);
			CQInfoGroups.regions(group, regions).do { |r|
				var ref = r[\target], name = ref[1], spec;
				if(ref[0] == \endpoint and: { source.containsPoint(r[\point]) }) {
					spec = CQGuiNightMap.endpointSpec(name);
					if(spec[\kind] == \jack) {
						switch(spec[\decoration],
							\leaf, { CQInkNight.drawLeaf(r[\point]) },
							\crown, { CQInkNight.drawCrown(r[\point]) });
						CQInk.drawJack(r[\point], spec[\family], \night);
					} {
						CQInk.drawSocket(r[\point], spec[\family], \night);
						spec[\glyph] !? { |glyph|
							CQInk.drawGlyph(glyph, Rect.aboutPoint(r[\point], 6, 6), CQInk.palette(\night)[spec[\family]]);
						};
					};
				};
				if(ref[0] == \param and: { crescents.includes(name) }) {
					CQInkNight.drawCrescent(r[\point], 0.45,
						if(name.asString.endsWith("Spaf")) { \spaf },
						scale: CQGuiNightMap.crescentScaleFor(name));
					if(CQGuiNightMap.hasDolbyCrescent(name)) {
						CQInkNight.drawDolbyOyster(CQNightGeometry.dolbyOysterAnchor(r[\point]),
							\off, if(name.asString.endsWith("In")) { 1 } { -1 });
					};
				};
			};
		});
	}
	*locator { |point, number, radius = 13|
		var color = CQInk.infoColor(0.8);
		Pen.strokeColor = color; Pen.width = 1.5;
		Pen.addOval(Rect.aboutPoint(point, radius, radius)); Pen.stroke;
		Pen.fillColor = CQInk.palette(\night)[\recess];
		Pen.addOval(Rect.aboutPoint(point + Point(radius, radius.neg), 7, 7)); Pen.fill;
		Pen.stringCenteredIn(number.asString,
			Rect.aboutPoint(point + Point(radius, radius.neg), 8, 8), CQInk.font(\label, 10), color);
	}
}
