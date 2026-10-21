// Read-only illustrations for both learning guides and Tide. Reuse actual
// Pen recipes; examples are still drawings, never controls or live telemetry.
CQInkInfo {
	*symbolRefs { |symbol|
		var pair = { |suffix| 2.collect { |i| [\param, ("coco" ++ (i + 1) ++ suffix).asSymbol] } };
		^switch(symbol,
			\coco, { [[\group, \coco, 0], [\group, \coco, 1]] },
			\bird, { [[\group, \bird, 0], [\group, \bird, 1]] },
			\root, { [[\group, \root, 0], [\group, \root, 1]] },
			\castle, { [[\group, \castles, 0]] },
			\crescents, { pair.value("In") ++ pair.value("Fb") },
			\shore, { pair.value("Level") }, \organ, { pair.value("Organ") },
			\dust, { [[\dust, 0], [\dust, 1]] },
			\eye, { [[\group, \eye, 0]] }, \moon, { [[\group, \moon, 0]] },
			\bridge, { pair.value("SlotX") ++ [[\endpoint, \coco1], [\endpoint, \coco2],
				[\endpoint, \coco1_slotX], [\endpoint, \coco2_slotX]] },
			\gill, { pair.value("SlotQ") },
			\spaf, { pair.value("Spaf") },
			\output, { CQMatrix.sourceNames.collect { |name| [\endpoint, name] } },
			\input, { CQMatrix.destNames.collect { |name| [\endpoint, name] } },
			\connect, { (CQMatrix.sourceNames ++ CQMatrix.destNames).collect { |name| [\endpoint, name] } },
			\positive, { [[\cables, \positive]] }, \negative, { [[\cables, \negative]] },
			{ [[\cables, \all]] })
	}

	*tourItems { |step, guide|
		guide = guide ? CQInfoTour;
		^guide.items(step)
	}

	*tourLayout { |step, left, top, width, fontSize, guide|
		var note = (guide ? CQInfoTour).step(step);
		^this.layoutItems(note[\items], left, top, width, fontSize,
			if(note[\concept] == true) { 4 } { 3 },
			fontSize * if(#[input, feedback].includes(note[\id])) { 9 } { if(note[\detailItems].notNil) { 5 } { 6 } })
	}

	*layoutItems { |items, left, top, width, fontSize, columns = 4, pictureHeight|
		var gap = 8, cellWidth;
		var labelHeight = CQGuiScale.textBandHeight(fontSize);
		var cellHeight;
		columns = min(columns, items.size);
		cellWidth = (width - (gap * (columns - 1))) / columns;
		pictureHeight = pictureHeight ? (fontSize * 4);
		cellHeight = pictureHeight + labelHeight + 4;
		^items.collect { |item, i|
			var x = left + ((i % columns) * (cellWidth + gap));
			var y = top + ((i div: columns) * (cellHeight + gap));
			(symbol: item[0], label: item[1], gesture: item[2], refs: item[3] ? [], side: item[4] ? 0,
				rect: Rect(x, y, cellWidth, cellHeight),
				picture: Rect(x + 3, y, cellWidth - 6, pictureHeight),
				caption: Rect(x, y + pictureHeight + 4, cellWidth, labelHeight))
		}
	}

	*fit { |source, destination, draw|
		var scale = min(destination.width / source.width, destination.height / source.height);
		Pen.use {
			Pen.addRect(destination); Pen.clip;
			Pen.translate(destination.center.x, destination.center.y);
			Pen.scale(scale, scale);
			Pen.translate(source.center.x.neg, source.center.y.neg);
			draw.value;
		};
	}

	*draw { |item, geometry, fontSize|
		if(geometry.isNil) { ^this };
		Pen.use { this.drawSymbol(item[\symbol], item[\picture], geometry, item[\side] ? 0) };
		item[\gesture] !? { |gesture| this.drawAction(gesture, item[\picture]) };
		if(item[\caption].notNil) {
			Pen.stringCenteredIn(item[\label], item[\caption], CQInk.font(\label, fontSize),
				CQInk.palette(\night)[\spark]);
		};
	}

	*drawSymbol { |symbol, rect, geometry, side = 0|
		var p = CQInk.palette(\night), coco = geometry[\cocos][side];
		var source, body, endpoint = (organInput: \coco1_organ, organOutput: \org1,
			castleOutput: \cas1, envelope: \envB, feedbackInput: \coco1_fbVca,
			skipInput: \coco1_skip, rateInput: \deckA_rate, fmInput: \castle5_fm)[symbol];
		if(endpoint.notNil) { ^this.drawEndpoint(endpoint, rect) };
		switch(symbol,
			\drawer, { this.fit(Rect(0, 0, 96, 36), rect, {
				CQInk.drawButton(Rect(0, 0, 96, 36), "drawer", \night,
					glyph: \collapse, surface: \recess, labelSize: 14);
			}) },
			\rec, { this.fit(Rect(0, 0, 96, 36), rect, {
				CQInk.drawButton(Rect(0, 0, 96, 36), "REC", \night,
					accent: \danger, glyph: \rec, surface: \recess, labelSize: 14);
			}) },
			\skip, { this.fit(Rect(-12, -12, 24, 24), rect, {
				CQInk.drawGlyph(\skip, Rect(-5, -5, 10, 10), p[\line]);
			}) },
			\range, { this.fit(Rect(-28, -16, 56, 32), rect, {
				CQInkNight.drawRangeRipple(0@0, \audio, true);
			}) },
			\dolby, { this.fit(Rect(-30, -30, 60, 76), rect, {
				CQInkNight.drawCrescent(0@(-8), 0.4, \input);
				CQInkNight.drawDolbyOyster(0@28, \off, hover: true);
			}) },
			\rootKnot, {
				body = geometry[\roots][side];
				this.fit(Rect.aboutPoint(body[\knot], 30, 30), rect, {
					CQInkNight.drawPreampRoot(body, 0@0, 0.4, 0.35, active: true);
				});
			},
			\bridgeValve, { this.fit(Rect(-29, -18, 58, 36), rect, {
				CQInkNight.drawFlowPath([-27@0, -20@0, -12@0, 12@0], 0.6, \cas, palette: p);
				CQInkNight.drawFlowGill(12@0, 1@0, 0.6, \cas, palette: p);
			}) },
			\seedStates, {
				this.drawSeed(Rect(rect.left, rect.top, rect.width * 0.44, 44), nil);
				this.drawSeed(Rect(rect.right - (rect.width * 0.44), rect.top, rect.width * 0.44, 44), 0.4);
				Pen.stringCenteredIn("empty", Rect(rect.left, rect.bottom - 16, rect.width * 0.5, 16),
					CQInk.font(\label, 10), p[\line]);
				Pen.stringCenteredIn("stored", Rect(rect.center.x, rect.bottom - 16, rect.width * 0.5, 16),
					CQInk.font(\label, 10), p[\line]);
			},
			\tideFocus, {
				this.fit(Rect(-32, -20, 64, 40), rect, {
					CQInkNight.drawTideGate(0@0, 0@0, true);
				});
			},
			\tideBlend, { this.drawTideExample(rect, false) },
			\tideBreed, { this.drawTideExample(rect, true) },
			\heartOpen, { this.drawHeart(coco, rect, false) },
			\heartClosed, { this.drawHeart(coco, rect, true) },
			\inputBank, { this.drawCrescentBank(rect, 1) },
			\feedbackBank, { this.drawCrescentBank(rect, 2) },
			\inputCrescent, {
				this.fit(Rect(-24, -24, 48, 48), rect, {
					CQInkNight.drawCrescent(0@0, 0.55, \input,
						scale: CQGuiNightMap.crescentScaleFor(\coco1In));
				});
			},
			\feedback, {
				this.fit(Rect(-24, -24, 48, 48), rect, {
					CQInkNight.drawCrescent(0@0, 0.55, \input,
						scale: CQGuiNightMap.crescentScaleFor(\coco1Fb));
				});
			},
			\spaf, {
				this.fit(Rect(-24, -24, 48, 48), rect, {
					CQInkNight.drawCrescent(0@0, 0.65, \spaf,
						scale: CQGuiNightMap.crescentScaleFor(\coco1Spaf));
				});
			},
			\dry, {
				this.fit(Rect(0, 0, 66, 80), rect, {
					CQInk.drawKnob(Rect(0, 0, 66, 80), 0.4, "dry", "",
						skin: \night, diameter: 40, labelSize: 12);
				});
			},
			\depth, {
				this.fit(Rect(0, 0, 66, 80), rect, {
					CQInk.drawKnob(Rect(0, 0, 66, 80), 1, "depth", "",
						family: \spaf, skin: \night, diameter: 40, labelSize: 12);
				});
			},
			\flip, {
				this.fit(Rect(-20, -20, 40, 40), rect, { CQInkNight.drawMouthChevron(0@0) });
			},
			\bird, {
				body = geometry[\birds][side];
				source = Rect.aboutPoint(body[\center] - (0@(body[\span] * 0.15)),
					body[\span] * 0.55, body[\span] * 0.30);
				this.fit(source, rect, { CQInkNight.drawBird(body, 0@0) });
			},
			\coco, {
				this.drawCoco(coco, rect);
			},
			\wind, { this.drawCoco(coco, rect, wind: true) },
			\root, {
				body = geometry[\roots][side];
				// Show the knot and intake strands, not its long vein into Coco.
				source = Rect(body[\knot].x - 58, body[\knot].y - 22, 116, 100);
				this.fit(source, rect, {
					CQInkNight.drawPreampRoot(body, 0@0,
						sourceIndex: CQGuiNightMap.preampSourceIndex(side, \deckA));
				});
			},
			\castle, {
				body = geometry[\castles][side];
				source = Rect.aboutPoint(body[\center], body[\bodyRx] + 10, body[\bodyRy] + 22);
				this.fit(source, rect, {
					CQInkNight.drawCastle(body, 0@0, nil, 0, if(side == 4) { \audio } { \infra });
				});
			},
			\crescents, {
				this.fit(Rect(-46, -23, 92, 46), rect, {
					CQInkNight.drawCrescent(-24@0, 0.6, \input, scale: CQGuiNightMap.crescentScaleFor(\coco1In));
					CQInkNight.drawCrescent(24@0, 0.7, \input, scale: CQGuiNightMap.crescentScaleFor(\coco1Fb));
				});
			},
			\shore, {
				this.drawCoco(coco, rect, true);
			},
			\organ, {
				this.fit(Rect(-31, -31, 62, 62), rect, {
					CQInkNight.drawOrganPod(0@0, 0.5);
					CQInk.drawJack(0@0, \organ, \night);
				});
			},
			\dust, {
				body = geometry[\dusts][side];
				this.fit(body[\box], rect, { CQInkNight.drawDust(body, 0@0, 1, 0.75, hover: true) });
			},
			\bridge, {
				// Project the real bridge paths into a small key, preserving line
				// weight so the long, narrow landscape routes remain readable.
				body = geometry[\drone][\slotXPaths];
				source = CQNightGeometry.prBoundsOf(body.flat, 1);
				body.do { |path|
					var points = path.collect { |at|
						Point(rect.left + ((at.x - source.left) / source.width * rect.width),
							rect.top + 8 + ((at.y - source.top) / source.height * (rect.height - 16)))
					};
					CQInkNight.drawFlowPath(points, 0.7, \cas, palette: p);
				};
			},
			\gill, {
				this.fit(Rect(-29, -18, 58, 36), rect, {
					CQInkNight.drawFlowPath([-27@0, -20@0, -12@0, 12@0], 0.6, \tri, palette: p);
					CQInkNight.drawFlowGill(12@0, 1@0, 0.6, \tri, palette: p);
				});
			},
			\eye, {
				this.fit(Rect(-34, -34, 68, 68), rect, {
					Pen.addOval(Rect(-32, -32, 64, 64)); Pen.clip;
					CQInkNight.drawEye(0@0);
				});
			},
			\moon, {
				this.fit(Rect(-25, -25, 50, 50), rect, { CQInkNight.drawSowingMoon(0@0, 22, 0.25) });
			},
			\output, {
				this.fit(Rect(-16, -16, 32, 32), rect, {
					CQInkNight.drawLeaf(0@0);
					CQInk.drawJack(0@0, \tri, \night);
				});
			},
			\input, {
				this.fit(Rect(-16, -16, 32, 32), rect, { CQInk.drawSocket(0@0, \spaf, \night) });
			},
			\flipInput, {
				this.fit(Rect(-16, -16, 32, 32), rect, {
					CQInk.drawSocket(0@0, \line, \night);
					CQInk.drawGlyph(\flip, Rect(-4, -4, 8, 8), p[\line]);
				});
			},
			\recordInput, {
				this.fit(Rect(-16, -16, 32, 32), rect, {
					CQInk.drawSocket(0@0, \hold, \night);
					CQInk.drawGlyph(\hold, Rect(-4, -4, 8, 8), p[\hold]);
				});
			},
			\cocoOutput, {
				this.fit(Rect(-16, -16, 32, 32), rect, { CQInk.drawJack(0@0, \cas, \night) });
			},
			\positive, { this.drawConnection(rect, 0.7) },
			\negative, { this.drawConnection(rect, -0.7) },
			\connect, {
				var left = Point(rect.left + 8, rect.center.y), right = Point(rect.right - 8, rect.center.y);
				CQInk.drawJack(left, \tri, \night); CQInk.drawSocket(right, \spaf, \night);
				Pen.strokeColor = p[\line]; Pen.width = 1.2;
				this.arrow(left + (13@0), right - (12@0));
			},
			\weights, { this.drawWeight(rect, 0.65) },
			\fullWeight, { this.drawWeight(rect, 1) },
			\moveCable, {
				var a = Point(rect.left + 8, rect.top + 8), b = Point(rect.right - 8, rect.top + 8);
				var c = Point(b.x, rect.bottom - 8);
				CQInk.drawCable(a, b, 0.6, \night); CQInk.drawJack(a, \tri, \night);
				CQInk.drawSocket(b, \spaf, \night); CQInk.drawPlug(b, 0.6, \night);
				CQInk.drawSocket(c, \spaf, \night);
				Pen.strokeColor = p[\line]; Pen.width = 1.2;
				this.arrow(b + (-10@9), c + (-10@ -3));
			}
		);
	}
	*drawEndpoint { |name, rect|
		var spec = CQGuiNightMap.endpointSpec(name), p = CQInk.palette(\night);
		this.fit(Rect(-16, -16, 32, 32), rect, {
			if(spec[\kind] == \jack) {
				if(spec[\decoration] == \crown) { CQInkNight.drawCrown(0@0) };
				CQInk.drawJack(0@0, spec[\family], \night);
			} {
				CQInk.drawSocket(0@0, spec[\family], \night);
				spec[\glyph] !? { |glyph| CQInk.drawGlyph(glyph, Rect(-4, -4, 8, 8), p[spec[\family]]) };
			};
		});
	}

	*drawWeight { |rect, weight|
		this.fit(Rect(0, 0, 100, 46), rect, {
			var a = 8@20, b = 92@20;
			this.drawConnection(Rect(1, 8, 98, 30), weight);
			CQInk.drawCableWeightMoon(CQInk.cablePoints(a, b), weight, \night);
		});
	}

	*drawSeed { |rect, density|
		this.fit(Rect(-18, -18, 36, 36), rect, {
			CQInkNight.drawTideField((compass: Rect(10, 10, 20, 20), stamps: [0@0]),
				[density], true);
		});
	}

	*drawTideExample { |rect, breed|
		var p = CQInk.palette(\night);
		// A schematic of the four corners, not a tiny copy of the whole
		// landscape. Native seed silhouettes retain a readable fixed size.
		this.fit(Rect(-17, -17, 114, 74), rect, {
			var stamps = if(breed) { [0@20, 80@20] } { [0@0, 80@0, 0@40, 80@40] };
			CQInkNight.drawTideField((compass: Rect(0, 0, 80, 40), stamps: stamps),
				Array.fill(stamps.size, 0.4), true);
			Pen.strokeColor = CQInk.infoColor(0.8); Pen.width = 1.5;
			if(breed) {
				this.arrow(17@20, 62@20);
			} {
				Pen.addOval(Rect.aboutPoint(30@28, 4, 4)); Pen.stroke;
				this.arrow(34@26, 54@13);
			};
		});
	}

	*drawHeart { |coco, rect, sealed|
		// Crop the real heart and inner winding, preserving the native
		// hollow-write / red-hold states instead of inventing a new icon.
		this.fit(Rect.aboutPoint(coco[\center], 32, 32), rect, {
			CQInkNight.drawShell(coco, 0@0, sealed);
		});
	}

	*drawAction { |action, rect|
		var x = rect.right - 8, start, end, angle = 1.7pi, radius;
		Pen.use {
			Pen.strokeColor = CQInk.infoColor(0.65); Pen.width = 1.3;
			if(action == \wind) {
				radius = min(rect.width, rect.height) * 0.44;
				Pen.addArc(rect.center, radius, 0.2pi, 1.5pi); Pen.stroke;
				end = rect.center + (Point(cos(angle), sin(angle)) * radius);
				this.arrow(end - (Point(sin(angle).neg, cos(angle)) * 8), end);
			} {
				start = Point(x, rect.bottom - 12); end = Point(x, rect.top + 10);
				if(action == \down) { start = Point(x, rect.top + 10); end = Point(x, rect.bottom - 12) };
				if(action == \tap) { start = Point(x, rect.top + 10); end = rect.center + (16@0) };
				if(#[across, inward].includes(action)) {
					start = Point(rect.left + 8, rect.bottom - 8); end = Point(rect.right - 8, rect.bottom - 8);
					if(action == \inward) { start = Point(rect.center.x, rect.bottom - 8); end = rect.center };
				};
				if(action == \wheel) { start = Point(x, rect.center.y + 12); end = Point(x, rect.center.y - 12) };
				if(action == \outward) { start = rect.center; end = Point(rect.left + 8, rect.bottom - 8) };
				this.arrow(start, end);
			};
		};
	}

	// Retain the left-to-right, descending bank shape so two similar arcs
	// can be recognized by position. Only the lesson's control carries full ink.
	*drawCrescentBank { |rect, selected|
		var centers = [-58@(-16), 0@10, 58@18];
		var names = #[coco1Spaf, coco1In, coco1Fb];
		var labels = ["SP.AF", "Input", "Feedback"];
		var p = CQInk.palette(\night);
		this.fit(Rect(-86, -44, 172, 98), rect, {
			centers.do { |at, i|
				CQInkNight.drawCrescent(at, if(i == 0) { 0.6 } { 0.55 },
					if(i == 0) { \spaf } { \input }, active: i == selected,
					scale: CQGuiNightMap.crescentScaleFor(names[i]));
				if(i != selected) {
					Pen.fillColor = p[\recess].copy.alpha_(0.75);
					Pen.fillRect(Rect.aboutPoint(at, 27, 27));
				};
				Pen.stringCenteredIn(labels[i], Rect(at.x - 28, at.y + 23, 56, 14),
					CQInk.font(\label, 10), p[if(i == selected) { \spark } { \whisper }]);
			};
		});
	}

	*arrow { |start, end|
		var delta = end - start, length = CQNightGeometry.distance(start, end).max(0.001);
		var unit = delta / length, normal = Point(unit.y.neg, unit.x), base = end - (unit * 6);
		Pen.line(start, end); Pen.line(base + (normal * 3), end);
		Pen.line(base - (normal * 3), end); Pen.stroke;
	}

	*drawCoco { |coco, rect, shore = false, wind = false|
		var p = CQInk.palette(\night), center = rect.center;
		var radius = if(shore) { coco[\levelR] } { coco[\outerR] };
		var scale = min(rect.width, rect.height) / ((radius + 12) * 2);
		var project = { |point| center + ((point - coco[\center]) * scale) };
		// Keep the real winding, but project its points instead of scaling Pen:
		// subpixel hairlines disappear when a whole shell is shrunk to a key.
		var segments = coco[\segments].collect { |segment| segment.collect(project) };
		Pen.strokeColor = p[\line].copy.alpha_(if(shore) { 0.38 } { 0.95 });
		Pen.width = 1;
		CQInkNight.strokeSegments(segments);
		if(shore) {
			CQInkNight.drawLevelShore((center: center, levelR: radius * scale,
				levelStart: coco[\levelStart], levelSweep: coco[\levelSweep]), 0.7, active: true);
		} {
			Pen.strokeColor = p[\danger].copy.alpha_(0.65); Pen.width = 1;
			Pen.addOval(Rect.aboutPoint(center, radius * scale, radius * scale)); Pen.stroke;
			Pen.fillColor = p[\danger];
			Pen.addOval(Rect.aboutPoint(center, 2, 2)); Pen.fill;
		};
		if(wind) {
			var norm = CQNightGeometry.reelNotchNorm(0.6);
			var at = project.value(CQNightGeometry.spiralPoint(coco, norm));
			var normal = CQNightGeometry.spiralNormal(coco, norm);
			var bead = project.value(CQNightGeometry.spiralPoint(coco, 0.77));
			Pen.strokeColor = p[\spark]; Pen.width = 2;
			Pen.line(at - (normal * 7), at + (normal * 7)); Pen.stroke;
			Pen.fillColor = p[\spark]; Pen.addOval(Rect.aboutPoint(bead, 3, 3)); Pen.fill;
		};
	}

	*drawConnection { |rect, weight|
		var left = Point(rect.left + 7, rect.top + 12);
		var right = Point(rect.right - 7, rect.top + 12);
		CQInk.drawCable(left, right, weight, \night);
		CQInk.drawJack(left, \tri, \night);
		CQInk.drawSocket(right, \spaf, \night);
		CQInk.drawPlug(right, weight, \night);
	}
}
