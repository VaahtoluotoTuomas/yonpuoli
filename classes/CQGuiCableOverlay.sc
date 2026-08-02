// CQGuiCableOverlay — hit-transparent planted-banana renderer used by Night.
// Endpoint geometry comes from the actual laid-out jack/socket views, while all
// matrix reads and redraw notifications stay on CQMatrix's public contracts.

CQGuiCableOverlay {
	var matrix, endpoints, grammar, auraFunc, ctl, gestureCtl;
	var <view, <skin = \night;
	var recordCache, cacheExtent, cacheHover, geometryCache, geometryPositions;
	var refreshScheduled = false, lastRefresh = 0, lastRenderCost = 0;
	// The offscreen cache (measured 2026-07-22 inside the real Qt paint event:
	// a full-ink repaint costs 2.1 ms at 12 cables and 82.7 ms at 352 on the
	// dev machine — roughly half the reference laptop's figures — so any repaint
	// the planted ink did not ask for must be a blit, not a re-stroke). The
	// planted pass renders into an Image whenever something that can move its
	// pixels changed — the patch set, the hover/solo/harvest marks, an aura
	// frame — and every other damage-driven repaint (a knob under the layer, a
	// morphing panel, the 60 Hz hand cable) pays one blit. Pixel-identical by
	// construction: the image is drawn by the very same Pen code.
	var image, imageExtent, imageSig, imageDirty = true, cacheOk = true;
	var renderScheduled = false;
	// §7.1 weather gestures: while a compass voyage is being poured the matrix
	// changes every frame, so the cache cannot help — instead the bay is drawn
	// in STORM FORM (one bezier pass per polarity, fixed width, no plugs or
	// barbs) and repainted only when the planted SET actually changes: under
	// the tide a redraw is a banana landing or lifting, not a continuous cost.
	var <stormMode = false, stormSig;
	// Momentary Shift water focus: planted spans recede and stop intercepting
	// the hand, while endpoint views and socket plug tests remain fully alive.
	var <backgrounded = false;
	// read by the paint-profile harness and the soak checklist: what actually
	// repaints, how often, and what one repaint cost (G1.3's law — measure
	// inside the paint event, never guess)
	var <drawCount = 0, <renderCount = 0, <lastDrawMs = 0;

	*new { |matrix, endpoints, grammar, auraFunc|
		^super.newCopyArgs(matrix, endpoints, grammar, auraFunc).prBuild
	}

	// GUI_SPEC §16.8: a planted cable's alpha pulses with its source's aura.
	// nil = the server has said nothing about that source, so the cable keeps the
	// reviewed static ink instead of pretending to be silent. Focus and solo-dim
	// never pulse — §4.5 keeps telemetry out of hover semantics.
	*plantedAlpha { |aura|
		^aura !? { 0.62 + (aura.clip(0, 1) * 0.3) } ?? 0.88
	}

	// Each cable costs ~11 language-to-Qt Pen calls, so past this many planted
	// cables one ambient repaint no longer fits the §10 frame budget — and a pulse
	// nobody can attribute to a single cable carries no information anyway. Denser
	// patches therefore keep the reviewed static ink, which is exactly the look
	// §16.8 already defines for "no telemetry", while the 16 source jacks go on
	// pulsing (all sixteen together cost about one cable). No hand-built patch
	// comes near the limit; `\mayhem` patches all 352 cells and does.
	*auraCableLimit { ^48 }
	*backgroundAlpha { ^0.14 }

	auraPulses { ^this.cableCount <= this.class.auraCableLimit }

	skin_ { |newSkin|
		skin = newSkin;
		view.background = CQInk.palette(skin)[\clear];
		imageDirty = true;
		view.refresh;
		^this
	}

	// an /cq_aura frame changes the planted alphas, so the cache re-renders —
	// HERE, on the AppClock, never inside the paint event (QtCollider's Pen
	// state is one global painter: opening a second on the image while the
	// view's own is active deadlocks the GUI thread). Callers that repaint for
	// any other reason keep calling view.refresh and get the blit.
	auraTick {
		imageDirty = true;
		this.prEnsureImage;
		view.refresh;
		^this
	}

	// the weather gestures hold this true for exactly the drag (set from
	// CQGuiNightMap); entering or leaving it re-inks once in the new form
	stormMode_ { |flag|
		var next = flag == true;
		if(next == stormMode) { ^this };
		stormMode = next;
		stormSig = nil;
		imageDirty = true;
		view.refresh;
		^this
	}

	backgrounded_ { |flag|
		var next = flag == true;
		if(next == backgrounded) { ^this };
		backgrounded = next;
		if(next) { grammar.hoverCable = nil };
		imageDirty = true;
		this.prEnsureImage;
		view.refresh;
		^this
	}

	free {
		ctl !? { ctl.remove };
		gestureCtl !? { gestureCtl.remove };
		ctl = nil;
		gestureCtl = nil;
		refreshScheduled = false;
		image !? { try { image.free } };
		image = nil;
		this.invalidate;
	}

	cableCount { ^matrix.asArray.size }
	endpointCount { ^endpoints.size }

	pointFor { |name|
		var endpoint = endpoints[name], localCenter, globalCenter, overlayOrigin;
		if(endpoint.isNil) { ^nil };
		localCenter = endpoint[\anchor] ?? {
			Point(endpoint[\view].bounds.width / 2,
				endpoint[\view].bounds.height / 2)
		};
		globalCenter = endpoint[\view].mapToGlobal(localCenter);
		overlayOrigin = view.mapToGlobal(0@0);
		^globalCenter - overlayOrigin
	}

	// An /cq_aura frame changes only each cable's alpha, never its geometry, yet a
	// rebuild costs ~3.3 ms with a 12-cable patch (bezier polylines dominate). At the
	// §10 ambient 15 Hz that alone burned ~5 % of a core, and every hover hit test
	// paid it again. Geometry is therefore cached and rebuilt only when something
	// that can actually move a cable changed: the patch set (invalidate, driven by
	// CQMatrix's own notifications), the overlay's size, or the hovered endpoint
	// whose fan-out spreads the plugs. Callers only read records (see prCaught).
	invalidate { recordCache = nil; imageDirty = true; ^this }

	// Matrix notifications carry no rate limit: a snapshot morph writes every changed
	// cell 30 times a second, i.e. twice the §10 ambient ceiling, and each of those
	// asked for a full re-stroke of the cable layer. Plant, rip and harvest are single
	// acts and still repaint at once — only a stream is paced down to the ceiling, and
	// the trailing repaint guarantees the last state is the one left on paper.
	// SOAK-1 finding A: 15 Hz was the wrong pace, not because §10's ceiling is
	// wrong but because a ceiling in Hz assumes the work fits inside the frame.
	// Measured 2026-07-31 at 352 cables: one re-ink costs ~41 ms, so the paced
	// stream asked for 62 % of the language thread and only 22 of 60 competing
	// AppClock ticks ran — PANIC could not be reached. **A stream is paced by
	// the cost of its own work, not by a fixed rate**: never spend more than one
	// slice in `duty` on redrawing, so a dense patch animates more coarsely
	// instead of freezing the instrument. A sparse patch is far under the
	// ceiling and keeps the full 15 Hz.
	*renderDuty { ^3 }

	prRefreshPeriod {
		^max(CQGui.paramFlushPeriod, lastRenderCost * this.class.renderDuty)
	}

	prRenderImage {
		var t0 = Main.elapsedTime;
		this.prEnsureImage;
		lastRenderCost = Main.elapsedTime - t0;
		^this
	}

	prRefresh {
		var wait;
		// under weather, only an actual change of the planted set (or a
		// polarity flip) is information — a glide of shared weights is drawn as
		// nothing in storm form, so it must not cost a repaint either
		if(stormMode) {
			var sig = matrix.asArray.collect { |t| [t[0], t[1], t[2] < 0] };
			if(sig == stormSig) { ^this };
			stormSig = sig;
		};
		if(refreshScheduled) { ^this };
		wait = this.prRefreshPeriod - (Main.elapsedTime - lastRefresh);
		if(wait <= 0) {
			lastRefresh = Main.elapsedTime;
			if(stormMode.not) { this.prRenderImage };
			view.refresh;
			^this
		};
		refreshScheduled = true;
		AppClock.sched(wait, {
			refreshScheduled = false;
			lastRefresh = Main.elapsedTime;
			if(view.isClosed.not) {
				this.prRenderImage;   // off the paint event, so the paint blits
				view.refresh;
			};
			nil
		});
	}

	cableRecords {
		var extent = view.bounds.extent, hover = grammar.hoverEndpoint;
		if(recordCache.isNil
			or: { cacheExtent != extent }
			or: { cacheHover != hover }) {
			// A cable's curve depends on the two endpoint positions and the fan
			// spread — that is, on the layout and on which endpoint is hovered.
			// It does not depend on the weight, so a morph must not pay for it.
			if((cacheExtent != extent) or: { cacheHover != hover }) {
				geometryCache = nil;
			};
			cacheExtent = extent;
			cacheHover = hover;
			recordCache = this.prBuildRecords;
		};
		^recordCache
	}

	// SOAK-1 finding A, measured 2026-07-31 with the real GUI: rebuilding these
	// records cost 122.9 ms at 352 cables — more than the full re-ink (42.2 ms)
	// it exists to avoid, and every matrix write invalidates them. A morph frame
	// therefore asked for 165 ms of work every 66 ms and the event loop starved:
	// only 5 of 60 competing AppClock ticks ran, which is why PANIC could not be
	// reached (safety invariant #3).
	//
	// The cost was one line: `patches.select` inside `patches.collect` is O(n²),
	// and `indexOf` on top of it walks the same list again. Grouping by
	// destination once makes the per-cable work O(1) and changes no output — a
	// fan's spread order is still the order the matrix reports.
	prBuildRecords {
		var patches, byDest, seen, positions;
		patches = matrix.asArray;
		byDest = IdentityDictionary.new;
		patches.do { |patch|
			byDest[patch[1]] = (byDest[patch[1]] ? 0) + 1;
		};
		seen = IdentityDictionary.new;
		// Ask each endpoint where it is exactly once — 38 of them instead of two
		// lookups per cable — and treat that set of positions as the geometry's
		// identity. If any endpoint moved, every curve is stale and the cache
		// goes; the night smoke's moved-endpoint assertion is what proves it,
		// and it is what caught the first version of this cache.
		positions = IdentityDictionary.new;
		endpoints.keysDo { |name| positions[name] = this.pointFor(name) };
		if(positions != geometryPositions) {
			geometryCache = Dictionary.new;
			geometryPositions = positions;
		};
		geometryCache ?? { geometryCache = Dictionary.new };
		^patches.collect { |patch|
			var index, count, key, geometry, start, finish;
			count = byDest[patch[1]] ? 1;
			index = seen[patch[1]] ? 0;
			seen[patch[1]] = index + 1;
			start = positions[patch[0]];
			finish = positions[patch[1]];
			// The fan's own offset is part of the geometry, so it belongs in the
			// key: two cables into the same socket curve differently.
			key = [patch[0], patch[1], index, count];
			geometry = geometryCache[key];
			if(geometry.isNil) {
				geometry = this.prGeometryFor(patch, index, count, start, finish);
				// A view that has not been laid out yet answers no position, and
				// that absence must never be cached: the old path recomputed
				// everything every frame and so healed itself silently, this one
				// would keep a hole forever (found by the profile, 2026-07-31).
				if(geometry[\start].notNil and: { geometry[\finish].notNil }) {
					geometryCache[key] = geometry;
				};
			};
			(src: patch[0], dest: patch[1], weight: patch[2],
				start: geometry[\start], finish: geometry[\finish],
				points: geometry[\points], bounds: geometry[\bounds])
		}
	}

	prGeometryFor { |patch, index, count, start, finish|
		var spread, distance, segments, points;
		spread = grammar.hoverEndpoint == patch[1] and: { count > 1 };
		if(spread) {
			finish = finish + Point((index - ((count - 1) * 0.5)) * 5, 0)
		};
		distance = this.prDistance(start, finish);
		segments = max(8, (distance / 12).ceil.asInteger);
		points = CQInk.cablePoints(start, finish, segments);
		^(start: start, finish: finish, points: points,
			bounds: this.prBoundsOf(points))
	}

	// Hover runs a hit test on every mouse move, and a cable that the pointer is
	// nowhere near should not cost one segment projection per bezier step.
	prBoundsOf { |points|
		var xs = points.collect { |p| p.x }, ys = points.collect { |p| p.y };
		^Rect(xs.minItem, ys.minItem,
			xs.maxItem - xs.minItem, ys.maxItem - ys.minItem)
	}

	nearestDestination { |point|
		var best, bestDistance, current, currentDistance;
		bestDistance = grammar.dropRadius;
		CQMatrix.destNames.do { |dest|
			var center, distance;
			center = this.pointFor(dest);
			distance = this.prDistance(point, center);
			if(distance <= bestDistance) {
				bestDistance = distance;
				best = (dest: dest, distance: distance);
			};
		};
		// A small hold bias prevents a cable from hopping between neighbouring
		// sockets while the pointer crosses their overlapping capture edges.
		current = grammar.candidateDest;
		if(current.notNil and: { best.notNil } and: { best[\dest] != current }) {
			currentDistance = this.prDistance(point, this.pointFor(current));
			if(currentDistance <= grammar.dropRadius
				and: { (best[\distance] + 6) >= currentDistance }) {
				best = (dest: current, distance: currentDistance)
			};
		};
		^best
	}

	patchAt { |point, threshold = 7|
		var best, bestDistance;
		if(backgrounded) { ^nil };
		bestDistance = threshold;
		this.cableRecords.do { |record|
			// cheap reject first: the pointer cannot be within `threshold` of this
			// cable if it is outside the cable's own box grown by that much
			if(record[\bounds].insetBy(threshold.neg, threshold.neg)
				.containsPoint(point)) {
				record[\points].doAdjacentPairs { |a, b|
					var distance;
					distance = this.prSegmentDistance(point, a, b);
					if(distance <= bestDistance) {
						bestDistance = distance;
						best = record;
					};
				};
			};
		};
		^best
	}

	patchAtSocket { |dest, point|
		var best, bestDistance = 12;
		this.cableRecords.do { |record|
			var distance;
			if(record[\dest] == dest) {
				distance = this.prDistance(point, record[\finish]);
				if(distance <= bestDistance) {
					bestDistance = distance;
					best = record;
				};
			};
		};
		^best
	}

	prBuild {
		view = UserView().background_(CQInk.palette(skin)[\clear]);
		view.acceptsMouse = false;
		view.drawFunc = {
			try { this.prDraw } { |error| CQInkControl.noteDrawError(error) }
		};
		ctl = SimpleController(matrix);
		ctl.put(\patch, { { this.invalidate; this.prRefresh }.defer });
		ctl.put(\clear, { { this.invalidate; this.prRefresh }.defer });
		ctl.put(\matrix, { { this.invalidate; this.prRefresh }.defer });
		// a live gesture may repaint at 60 Hz (GUI_SPEC §10); only the matrix
		// stream behind it is paced
		gestureCtl = SimpleController(grammar);
		gestureCtl.put(\gesture, { { view.refresh }.defer });
		^this
	}

	prDraw {
		var start, finish, weight, points, distance, segments;
		var t0 = Main.elapsedTime;
		if(stormMode) { this.prDrawStorm } { this.prBlitPlanted };
		// the hand cable and the lasso are always live: they are the gesture
		// itself, one cable's worth of Pen calls at 60 Hz over the blit
		if(grammar.isInHand and: { grammar.cursor.notNil }) {
			start = this.pointFor(grammar.handSource);
			finish = if(grammar.candidateDest.notNil) {
				this.pointFor(grammar.candidateDest)
			} {
				grammar.cursor
			};
			weight = grammar.handWeight ? CQBananaGrammar.defaultHandStrength;
			distance = this.prDistance(start, finish);
			segments = max(8, (distance / 12).ceil.asInteger);
			points = CQInk.cablePoints(start, finish, segments);
			CQInk.drawCable(start, finish, weight, skin, 1.0, points);
			CQInk.drawPlug(start, weight, skin);
			CQInk.drawPlug(finish, weight, skin,
				if(grammar.candidateDest.notNil) { 3 } { 0 });
			CQInk.drawCableWeightMoon(points, weight, skin);
		};
		grammar.lassoPath !? { |path| CQInk.drawLasso(path, skin) };
		drawCount = drawCount + 1;
		lastDrawMs = (Main.elapsedTime - t0) * 1000;
	}

	// planted ink via the offscreen image. The render itself NEVER happens in
	// here — a drawFunc runs inside Qt's paint event, where opening the image's
	// painter would nest painters on QtCollider's one global Pen and deadlock
	// the GUI thread (found the hard way, 2026-07-22). A stale image falls back
	// to the direct drawing — pixel-identical, today's price — and a render is
	// scheduled off the paint event so the NEXT damage repaint is a blit.
	prBlitPlanted {
		if(cacheOk.not) { ^this.prDrawPlanted };
		if(this.prImageFresh) { ^image.drawAtPoint(Point(0, 0)) };
		// SOAK-1 finding A, the last term. During a morph a write lands between
		// the render and the paint every single time, so the image was never
		// "fresh" and every paint took the direct route — 352 cables inked
		// inside the paint event, which is the 175 ms path §11 measured and the
		// reason PANIC could not be reached. **A stale blit beats a frozen
		// window**: one frame of slightly old cable ink is invisible mid-glide,
		// and the trailing render still puts the final state on paper. The
		// direct draw remains for the only case that needs it — no usable image
		// yet, or one built for a different size.
		if(image.notNil and: { imageExtent == view.bounds.extent }) {
			image.drawAtPoint(Point(0, 0));
			^this.prScheduleRender
		};
		this.prDrawPlanted;
		this.prScheduleRender;
	}

	prImageFresh {
		^image.notNil and: { imageDirty.not }
			and: { imageExtent == view.bounds.extent }
			and: { imageSig == this.prInkSignature }
	}

	// render the planted pass into the image — callable only OUTSIDE a paint
	// event (AppClock callbacks, notification handlers). A failed Image
	// round-trip (platform without the API) falls back to direct drawing,
	// permanently and loudly — never silently absent ink.
	prEnsureImage {
		var extent;
		if(cacheOk.not or: { this.prImageFresh }) { ^this };
		extent = view.bounds.extent;   // a Point — Rect:extent has no width
		try {
			image !? { image.free };
			image = Image(max(1, extent.x.asInteger),
				max(1, extent.y.asInteger));
			image.draw { this.prDrawPlanted };
			imageExtent = extent;
			imageSig = this.prInkSignature;
			imageDirty = false;
			renderCount = renderCount + 1;
		} { |error|
			cacheOk = false;
			image !? { try { image.free } };
			image = nil;
			("CQGuiCableOverlay: offscreen cache disabled (%)"
				.format(error.errorString)).warn;
		};
		^this
	}

	// after a fallback direct draw the screen is already right, so the deferred
	// render refreshes nothing — it only readies the blit for the next damage
	prScheduleRender {
		if(renderScheduled or: { cacheOk.not }) { ^this };
		renderScheduled = true;
		AppClock.sched(0, {
			renderScheduled = false;
			if(view.isClosed.not) { this.prEnsureImage };
			nil
		});
		^this
	}

	// everything that can change the planted pass's pixels without going
	// through invalidate or auraTick: hover focus, solo dim, the hand (a
	// grabbed cable is skipped), harvest marks
	prInkSignature {
		^[this.cableRecords, grammar.hoverCable, grammar.soloTarget,
			grammar.handSource, grammar.candidateDest, grammar.harvestKey,
			backgrounded]
	}

	prDrawPlanted {
		var alpha, lifted, focused, hasFocus, pulses, focusedRecord;
		hasFocus = grammar.hoverCable.notNil;
		pulses = this.auraPulses;
		this.cableRecords.do { |record|
			if(grammar.isGrabbed(record[\src], record[\dest]).not) {
				focused = grammar.isCableHovered(record[\src], record[\dest]);
				if(focused) { focusedRecord = record };
				alpha = if(backgrounded) { this.class.backgroundAlpha } {
					if(focused) { 1.0 } {
						if(grammar.isCableDim(record[\src], record[\dest])) { 0.22 } {
							if(hasFocus) { 0.46 } {
								this.class.plantedAlpha(
									if(pulses) {
										auraFunc !? { auraFunc.value(record[\src]) }
									})
							}
						}
					}
				};
				lifted = if(grammar.isHarvested(record[\src], record[\dest])) { 2 } {
					if(focused) { 1 } { 0 }
				};
				if(lifted > 0 and: { backgrounded.not }) {
					if(grammar.isHarvested(record[\src], record[\dest])) {
						CQInk.drawCableGlow(record[\start], record[\finish], skin)
					} {
						CQInk.drawCableFocus(record[\start], record[\finish], skin)
					}
				};
				CQInk.drawCable(record[\start], record[\finish], record[\weight], skin,
					alpha, record[\points]);
				CQInk.drawPlug(record[\start], record[\weight], skin);
				CQInk.drawPlug(record[\finish], record[\weight], skin, lifted);
			};
		};
		if(focusedRecord.notNil and: { backgrounded.not }) {
			CQInk.drawCableWeightMoon(focusedRecord[\points],
				focusedRecord[\weight], skin)
		};
	}

	// §7.1 storm form: the bay seen through rain — fixed-width polarity-colored
	// cores, no plugs, no barbs, and a repaint only when a banana lands or
	// lifts (prRefresh gates on the planted set). Each cable is its OWN small
	// stroke on purpose: the obvious "one giant path per polarity" was built
	// first and measured 505 ms at 352 cables against full ink's 177 — Qt's
	// stroker goes superlinear in a single path's subpath count, so batching
	// made it three times worse. Measured, not guessed (G1.3's law, again).
	prDrawStorm {
		var palette = CQInk.palette(skin), records = this.cableRecords;
		var alpha = if(backgrounded) { this.class.backgroundAlpha } { 0.7 };
		[[\verso, { |w| w >= 0 }], [\inverso, { |w| w < 0 }]].do { |pair|
			var group = records.select { |r| pair[1].value(r[\weight]) };
			if(group.notEmpty) {
				Pen.strokeColor = palette[pair[0]].copy.alpha_(alpha);
				Pen.width = CQInk.strokes[\normal];
				group.do { |r|
					var controls = CQInk.cableControls(r[\start], r[\finish]);
					Pen.moveTo(r[\start]);
					Pen.curveTo(r[\finish], controls[0], controls[1]);
					Pen.stroke;
				};
			};
		};
	}

	prSegmentDistance { |point, a, b|
		var dx, dy, lengthSq, t, projection;
		dx = b.x - a.x;
		dy = b.y - a.y;
		lengthSq = dx.squared + dy.squared;
		if(lengthSq <= 0.0001) { ^this.prDistance(point, a) };
		t = ((((point.x - a.x) * dx) + ((point.y - a.y) * dy)) / lengthSq)
			.clip(0, 1);
		projection = Point(a.x + (t * dx), a.y + (t * dy));
		^this.prDistance(point, projection)
	}

	prDistance { |a, b|
		if(a.isNil or: { b.isNil }) { ^inf };
		^sqrt((b.x - a.x).squared + (b.y - a.y).squared)
	}
}
