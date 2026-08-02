// CQBananaGrammar — serverless mouse-banana behavior (GUI_SPEC §4).
// It owns only GUI-local gesture/undo/solo state. A held cable is an armed,
// view-only gesture: the matrix changes exactly once, when a plug is planted.

CQBananaGrammar {
	var <matrix;
	var <hand, <candidateDest, <candidateWeight, <cursor;
	var <hoverEndpoint, <hoverCable, <soloTarget, <lassoPath, caught;
	var undoRing, handStrength, <dropRadius, undoDepth;

	// PLAN playability measurement 2026-08-01: an exact 0.5 banana only
	// touches the production Schmitt's +0.5 boundary and never fires it. 0.55
	// already crossed in the gate fixture; 0.60 keeps margin above that measured
	// floor while easing the first continuous modulation requested in PLAY-2.
	*defaultHandStrength { ^0.60 }
	// The socket's larger magnetic reach is forgiving, but never a strength
	// control. Once inside it, release means plant at the cable's armed weight.
	*defaultDropRadius { ^40 }

	*new { |matrix, handStrength, dropRadius, undoDepth = 32|
		handStrength = handStrength ?? { this.defaultHandStrength };
		dropRadius = dropRadius ?? { this.defaultDropRadius };
		^super.newCopyArgs(matrix).prInit(handStrength, dropRadius, undoDepth)
	}

	prInit { |strength, radius, depth|
		handStrength = strength.clip(0, 1);
		dropRadius = radius.max(1);
		undoDepth = depth.asInteger.max(1);
		undoRing = List.new;
		caught = [];
		^this
	}

	isInHand { ^hand.notNil }
	isStickySource {
		^hand.notNil and: { hand[\kind] == \source } and: { hand[\sticky] == true }
	}
	isSourceInHand { |src| ^hand.notNil and: { hand[\src] == src } }
	isGrabbed { |src, dest|
		^hand.notNil and: { hand[\kind] == \grab }
			and: { hand[\src] == src } and: { hand[\originDest] == dest }
	}
	isCandidate { |dest| ^candidateDest == dest }
	isHarvesting { ^lassoPath.notNil }
	// §4.4: a press that travels less than this is a no-op, never a lasso. The
	// almanac corner's taps borrow the same number (§6.1), so the hand learns
	// one threshold for "I pressed" versus "I dragged" across the whole night.
	*lassoTravel { ^8 }
	undoCount { ^undoRing.size }
	handSource { ^hand !? { hand[\src] } }
	handWeight {
		^candidateWeight ?? { hand !? { hand[\weight] } }
	}

	cursor_ { |point|
		cursor = point;
		this.changed(\gesture);
		^this
	}

	hoverEndpoint_ { |name|
		if(hoverEndpoint == name) { ^this };
		hoverEndpoint = name;
		this.changed(\gesture);
		^this
	}

	hoverCable_ { |record|
		var next, same;
		next = if(record.isNil) { nil } {
			(src: record[\src], dest: record[\dest])
		};
		same = hoverCable.isNil and: { next.isNil };
		if(hoverCable.notNil and: { next.notNil }) {
			same = hoverCable[\src] == next[\src]
				and: { hoverCable[\dest] == next[\dest] }
		};
		if(same) { ^this };
		hoverCable = next;
		this.changed(\gesture);
		^this
	}

	isCableHovered { |src, dest|
		^hoverCable.notNil and: { hoverCable[\src] == src }
			and: { hoverCable[\dest] == dest }
	}

	startSource { |src, polarity = 1|
		if(CQMatrix.srcIndex(src).isNil) { ^this };
		this.prDropHand;
		hoverCable = nil;
		hand = (
			kind: \source, src: src, polarity: if(polarity < 0) { -1 } { 1 },
			weight: if(polarity < 0) { handStrength.neg } { handStrength },
			before: matrix.fullMatrix, sticky: false
		);
		this.changed(\gesture);
		^this
	}

	grab { |src, dest|
		var weight;
		weight = matrix.getPatch(src, dest);
		if(weight == 0) { ^this };
		this.prDropHand;
		hoverCable = nil;
		hand = (
			kind: \grab, src: src, originDest: dest, carried: weight,
			weight: weight,
			before: matrix.fullMatrix, sticky: false
		);
		this.changed(\gesture);
		^this
	}

	updateCandidate { |dest, distance = 0|
		if(hand.isNil or: { CQMatrix.destIndex(dest).isNil }
			or: { distance > dropRadius }) {
			this.leaveRadius;
			^this
		};
		candidateDest = dest;
		candidateWeight = hand[\weight];
		this.changed(\gesture);
		^this
	}

	leaveRadius {
		candidateDest = nil;
		candidateWeight = nil;
		this.changed(\gesture);
		^this
	}

	releaseCandidate { |keepSource = false|
		var before, dense, src, origin, target, desired, polarity;
		if(hand.isNil or: { candidateDest.isNil }) { ^this.releaseVoid };
		before = hand[\before];
		src = hand[\src];
		target = candidateDest;
		desired = candidateWeight;
		if(hand[\kind] == \source) {
			polarity = hand[\polarity];
			matrix.patch(src, target, desired, origin: \gui, committed: true,
				operation: \plant);
			this.prRemember(before);
			if(keepSource) {
				hand = (kind: \source, src: src, polarity: polarity,
					weight: desired, before: matrix.fullMatrix, sticky: true)
			} {
				hand = nil
			};
		} {
			origin = hand[\originDest];
			if(target != origin
				or: { matrix.getPatch(src, origin) != desired }) {
				dense = matrix.fullMatrix;
				if(target != origin) { this.prSetDense(dense, src, origin, 0) };
				this.prSetDense(dense, src, target, desired);
				matrix.writeMatrix(dense, origin: \gui, committed: true,
					operation: \move);
				this.prRemember(before);
			};
			hand = nil;
		};
		candidateDest = nil;
		candidateWeight = nil;
		this.changed(\gesture);
		^this
	}

	releaseVoid {
		var before, src, dest;
		if(hand.isNil) { ^this };
		before = hand[\before];
		candidateDest = nil;
		candidateWeight = nil;
		if(hand[\kind] == \grab) {
			src = hand[\src];
			dest = hand[\originDest];
			matrix.patch(src, dest, 0, origin: \gui, committed: true,
				operation: \rip);
			this.prRemember(before);
			hand = nil;
		} {
			if(hand[\sticky].not) { hand = nil };
		};
		this.changed(\gesture);
		^this
	}

	instantRip { |src, dest|
		var before;
		if(matrix.getPatch(src, dest) == 0) { ^this };
		before = matrix.fullMatrix;
		matrix.patch(src, dest, 0, origin: \gui, committed: true,
			operation: \rip);
		this.prRemember(before);
		this.changed(\gesture);
		^this
	}

	polarityFlip { |src, dest|
		var before, weight;
		weight = matrix.getPatch(src, dest);
		if(weight == 0) { ^this };
		before = matrix.fullMatrix;
		matrix.patch(src, dest, weight.neg, origin: \gui, committed: true,
			operation: \polarity);
		this.prRemember(before);
		this.changed(\gesture);
		^this
	}

	wheel { |src, dest, direction = 1, fine = false|
		var weight, magnitude, step;
		weight = matrix.getPatch(src, dest);
		if(weight == 0 or: { direction == 0 }) { ^this };
		step = if(fine) { 0.01 } { 0.05 };
		magnitude = (weight.abs + (direction.sign * step)).clip(0.05, 1);
		matrix.patch(src, dest, magnitude * weight.sign, origin: \gui,
			committed: true, operation: \wheel);
		this.changed(\gesture);
		^this
	}

	// The unobtrusive attenuator for a new route: while the banana is in hand,
	// the same wheel language as a planted cable changes only its armed value.
	// No DSP or matrix write occurs until release over a magnetic destination.
	wheelHand { |direction = 1, fine = false|
		var weight, magnitude, step, next;
		if(hand.isNil or: { direction == 0 }) { ^this };
		weight = hand[\weight];
		step = if(fine) { 0.01 } { 0.05 };
		magnitude = (weight.abs + (direction.sign * step)).clip(0.05, 1);
		next = magnitude * weight.sign;
		hand[\weight] = next;
		if(hand[\kind] == \grab) { hand[\carried] = next };
		if(candidateDest.notNil) { candidateWeight = next };
		this.changed(\gesture);
		^this
	}

	performUndoable { |action|
		var before;
		this.prDropHand;
		before = matrix.fullMatrix;
		action.value;
		this.prRemember(before);
		this.changed(\gesture);
		^this
	}

	// Inspector fine edits publish transient frame writes, then one committed
	// release.  The gesture captured its own pre-drag matrix before those frames,
	// so it hands that copy back here to join the same GUI-local undo ring.
	rememberMatrix { |before|
		if(before.notNil) { this.prRemember(before) };
		this.changed(\gesture);
		^this
	}

	undo {
		var previous;
		this.prDropHand;
		if(undoRing.isEmpty) { ^this };
		previous = undoRing.removeAt(undoRing.size - 1);
		matrix.writeMatrix(previous, origin: \gui, committed: true,
			operation: \undo);
		this.changed(\gesture);
		^this
	}

	beginHarvest { |point|
		if(hand.notNil) { ^this };
		hoverCable = nil;
		lassoPath = [point];
		caught = [];
		cursor = point;
		this.changed(\gesture);
		^this
	}

	updateHarvest { |point, cables|
		var last;
		if(lassoPath.isNil) { ^this };
		last = lassoPath.last;
		if(this.prDistance(last, point) >= 2) { lassoPath = lassoPath.add(point) };
		cursor = point;
		caught = if(this.prHarvestTravel >= this.class.lassoTravel) {
			this.prCaught(cables)
		} { [] };
		this.changed(\gesture);
		^this
	}

	finishHarvest { |cables|
		var before, dense;
		if(lassoPath.isNil) { ^this };
		caught = if(this.prHarvestTravel >= this.class.lassoTravel) {
			this.prCaught(cables)
		} { [] };
		if(caught.notEmpty) {
			before = matrix.fullMatrix;
			dense = matrix.fullMatrix;
			caught.do { |patch|
				this.prSetDense(dense, patch[\src], patch[\dest], 0)
			};
			matrix.writeMatrix(dense, origin: \gui, committed: true,
				operation: \harvest);
			this.prRemember(before);
		};
		lassoPath = nil;
		caught = [];
		this.changed(\gesture);
		^this
	}

	cancelHarvest {
		lassoPath = nil;
		caught = [];
		this.changed(\gesture);
		^this
	}

	isHarvested { |src, dest|
		^caught.any { |patch| patch[\src] == src and: { patch[\dest] == dest } }
	}

	// the current catch as comparable pairs — the cable layer's offscreen cache
	// keys its planted-ink signature on this, so membership changes re-render
	// while a mere lasso-path move stays a blit
	harvestKey {
		^caught.collect { |patch| [patch[\src], patch[\dest]] }
	}

	toggleSoloEndpoint { |kind, name|
		var same;
		same = soloTarget.notNil and: { soloTarget[\kind] == kind }
			and: { soloTarget[\name] == name };
		soloTarget = if(same) { nil } { (kind: kind, name: name) };
		this.changed(\gesture);
		^this
	}

	toggleSoloCable { |src, dest|
		var same;
		same = soloTarget.notNil and: { soloTarget[\kind] == \cable }
			and: { soloTarget[\src] == src } and: { soloTarget[\dest] == dest };
		soloTarget = if(same) { nil } { (kind: \cable, src: src, dest: dest) };
		this.changed(\gesture);
		^this
	}

	toggleSoloBody { |name, members|
		var same;
		same = soloTarget.notNil and: { soloTarget[\kind] == \body }
			and: { soloTarget[\name] == name };
		soloTarget = if(same) { nil } {
			(kind: \body, name: name, members: members.copy)
		};
		this.changed(\gesture);
		^this
	}

	isCableDim { |src, dest|
		var target, members;
		target = soloTarget;
		if(target.isNil) { ^false };
		^switch(target[\kind],
			\jack, { src != target[\name] },
			\socket, { dest != target[\name] },
			\cable, { src != target[\src] or: { dest != target[\dest] } },
			\body, {
				members = target[\members];
				members.includes(src).not and: { members.includes(dest).not }
			},
			{ false }
		)
	}

	escape {
		this.prDropHand;
		this.cancelHarvest;
		soloTarget = nil;
		this.changed(\gesture);
		^this
	}

	viewToggle { this.prDropHand; this.cancelHarvest; ^this }
	windowClose { this.prDropHand; this.cancelHarvest; ^this }
	cmdPeriod { this.prDropHand; this.cancelHarvest; ^this }
	panic { this.prDropHand; this.cancelHarvest; ^this }
	free { this.prDropHand; this.cancelHarvest; ^this }

	// ---- private ----

	prDropHand {
		hand = nil;
		candidateDest = nil;
		candidateWeight = nil;
		cursor = nil;
		this.changed(\gesture);
		^this
	}

	prRemember { |before|
		if(this.prMatricesEqual(before, matrix.fullMatrix).not) {
			undoRing.add(before.collect { |row| row.copy });
			while { undoRing.size > undoDepth } { undoRing.removeAt(0) };
		};
		^this
	}

	prMatricesEqual { |a, b|
		if(a.size != b.size) { ^false };
		^a.every { |row, i|
			row.size == b[i].size and: {
				row.every { |value, j| value == b[i][j] }
			}
		}
	}

	prSetDense { |dense, src, dest, value|
		var si, di;
		si = CQMatrix.srcIndex(src);
		di = CQMatrix.destIndex(dest);
		if(si.notNil and: { di.notNil }) { dense[di][si] = value };
		^dense
	}

	prHarvestTravel {
		var start;
		if(lassoPath.isNil or: { lassoPath.size < 2 }) { ^0 };
		start = lassoPath.first;
		^lassoPath.collect { |point| this.prDistance(start, point) }.maxItem
	}

	prCaught { |cables|
		var polygon;
		polygon = lassoPath;
		if(polygon.isNil or: { polygon.size < 3 }) { ^[] };
		^cables.select { |cable|
			cable[\points].any { |point| this.prPointInPolygon(point, polygon) }
		}.collect { |cable| (src: cable[\src], dest: cable[\dest]) }
	}

	prPointInPolygon { |point, polygon|
		var inside = false, previous, current, crosses, intersectionX;
		previous = polygon.last;
		polygon.do { |vertex|
			current = vertex;
			if(this.prPointOnSegment(point, previous, current)) { ^true };
			crosses = (current.y > point.y) != (previous.y > point.y);
			if(crosses) {
				intersectionX = (((previous.x - current.x) * (point.y - current.y))
					/ (previous.y - current.y)) + current.x;
				crosses = point.x < intersectionX;
			};
			if(crosses) { inside = inside.not };
			previous = current;
		};
		^inside
	}

	prPointOnSegment { |point, a, b|
		var cross, dot, lengthSq;
		cross = ((point.y - a.y) * (b.x - a.x))
			- ((point.x - a.x) * (b.y - a.y));
		if(cross.abs > 0.01) { ^false };
		dot = ((point.x - a.x) * (b.x - a.x))
			+ ((point.y - a.y) * (b.y - a.y));
		if(dot < 0) { ^false };
		lengthSq = (b.x - a.x).squared + (b.y - a.y).squared;
		^dot <= lengthSq
	}

	prDistance { |a, b|
		^sqrt((b.x - a.x).squared + (b.y - a.y).squared)
	}
}
