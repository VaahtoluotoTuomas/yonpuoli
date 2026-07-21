// CQGuiCableOverlay — hit-transparent planted-banana renderer for the Day panel.
// Endpoint geometry comes from the actual laid-out jack/socket views, while all
// matrix reads and redraw notifications stay on CQMatrix's public contracts.

CQGuiCableOverlay {
	var matrix, endpoints, grammar, auraFunc, ctl, gestureCtl;
	var <view, <skin = \day;
	var recordCache, cacheExtent, cacheHover;
	var refreshScheduled = false, lastRefresh = 0;

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

	auraPulses { ^this.cableCount <= this.class.auraCableLimit }

	skin_ { |newSkin|
		skin = newSkin;
		view.background = CQInk.palette(skin)[\clear];
		view.refresh;
		^this
	}

	free {
		ctl !? { ctl.remove };
		gestureCtl !? { gestureCtl.remove };
		ctl = nil;
		gestureCtl = nil;
		refreshScheduled = false;
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
	invalidate { recordCache = nil; ^this }

	// Matrix notifications carry no rate limit: a snapshot morph writes every changed
	// cell 30 times a second, i.e. twice the §10 ambient ceiling, and each of those
	// asked for a full re-stroke of the cable layer. Plant, rip and harvest are single
	// acts and still repaint at once — only a stream is paced down to the ceiling, and
	// the trailing repaint guarantees the last state is the one left on paper.
	prRefresh {
		var wait;
		if(refreshScheduled) { ^this };
		wait = CQGui.paramFlushPeriod - (Main.elapsedTime - lastRefresh);
		if(wait <= 0) {
			lastRefresh = Main.elapsedTime;
			view.refresh;
			^this
		};
		refreshScheduled = true;
		AppClock.sched(wait, {
			refreshScheduled = false;
			lastRefresh = Main.elapsedTime;
			if(view.isClosed.not) { view.refresh };
			nil
		});
	}

	cableRecords {
		var extent = view.bounds.extent, hover = grammar.hoverEndpoint;
		if(recordCache.isNil
			or: { cacheExtent != extent }
			or: { cacheHover != hover }) {
			cacheExtent = extent;
			cacheHover = hover;
			recordCache = this.prBuildRecords;
		};
		^recordCache
	}

	prBuildRecords {
		var patches;
		patches = matrix.asArray;
		^patches.collect { |patch|
			var sameDest, index, count, spread, start, finish, distance, segments;
			var points;
			sameDest = patches.select { |other| other[1] == patch[1] };
			index = sameDest.indexOf(patch) ? 0;
			count = sameDest.size;
			spread = grammar.hoverEndpoint == patch[1] and: { count > 1 };
			start = this.pointFor(patch[0]);
			finish = this.pointFor(patch[1]);
			if(spread) {
				finish = finish + Point((index - ((count - 1) * 0.5)) * 5, 0)
			};
			distance = this.prDistance(start, finish);
			segments = max(8, (distance / 12).ceil.asInteger);
			points = CQInk.cablePoints(start, finish, segments);
			(src: patch[0], dest: patch[1], weight: patch[2], start: start,
				finish: finish, points: points, bounds: this.prBoundsOf(points))
		}
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
		bestDistance = grammar.auditionRadius;
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
			if(currentDistance <= grammar.auditionRadius
				and: { (best[\distance] + 6) >= currentDistance }) {
				best = (dest: current, distance: currentDistance)
			};
		};
		^best
	}

	patchAt { |point, threshold = 7|
		var best, bestDistance;
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
		var start, finish, weight, alpha, lifted, focused, hasFocus, pulses;
		hasFocus = grammar.hoverCable.notNil;
		pulses = this.auraPulses;
		this.cableRecords.do { |record|
			if(grammar.isGrabbed(record[\src], record[\dest]).not
				and: { grammar.isTransient(record[\src], record[\dest]).not }) {
				focused = grammar.isCableHovered(record[\src], record[\dest]);
				alpha = if(focused) { 1.0 } {
					if(grammar.isCableDim(record[\src], record[\dest])) { 0.22 } {
						if(hasFocus) { 0.46 } {
							this.class.plantedAlpha(
								if(pulses) {
									auraFunc !? { auraFunc.value(record[\src]) }
								})
						}
					}
				};
				lifted = if(grammar.isHarvested(record[\src], record[\dest])) { 2 } {
					if(focused) { 1 } { 0 }
				};
				if(lifted > 0) {
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
		if(grammar.isInHand and: { grammar.cursor.notNil }) {
			start = this.pointFor(grammar.handSource);
			finish = if(grammar.candidateDest.notNil) {
				this.pointFor(grammar.candidateDest)
			} {
				grammar.cursor
			};
			weight = grammar.handWeight ? 0.5;
			alpha = if(grammar.candidateDest.notNil) { 0.6 } { 1.0 };
			CQInk.drawCable(start, finish, weight, skin, alpha);
			CQInk.drawPlug(start, weight, skin);
			CQInk.drawPlug(finish, weight, skin,
				if(grammar.candidateDest.notNil) { 3 } { 0 });
		};
		grammar.lassoPath !? { |path| CQInk.drawLasso(path, skin) };
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
