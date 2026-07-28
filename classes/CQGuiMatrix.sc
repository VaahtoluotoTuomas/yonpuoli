// CQGuiMatrix — the 16×22 precision matrix as the §8 patch-loom experiment.
// Quiet rails preserve exact coordinates; planted cells become weighted knots.
// Header, loom and route ribbon are separate fixed regions in the workbench.

CQGuiMatrix {
	var matrix, inspector, <view, <grid, <header, <status, ctl;
	var <selectedSrc, <selectedDest, <hoverSi, <hoverDi;
	var skin = \day;
	// the loom's own measurements, taken from the Drawer's size step. The
	// cell width sets the shared content rail every Drawer page aligns to, so
	// the loom is where the workbench's width is decided (CQGuiScale).
	var <step = \compact, metrics, cellW, cellH, labW, headerH;

	*new { |matrix, inspector, step| ^super.newCopyArgs(matrix, inspector).prBuild(step) }

	*cellSize { |step = \compact, pageWidth = 1272|
		^[CQGuiScale.loomCell(step, pageWidth),
			CQGuiScale.metrics(step)[\cellHeight]]
	}

	*loomSize { |step = \compact, pageWidth = 1272|
		^Size(CQGuiScale.railWidth(step, pageWidth), CQGuiScale.loomHeight(step))
	}

	cellSize { ^[cellW, cellH] }
	loomSize { ^Size(labW + (CQMatrix.destNames.size * cellW), grid.bounds.height) }

	// Nothing is rebuilt on a step change: the loom is one UserView whose ink is
	// computed from these four numbers, so a resize is four assignments and a
	// repaint.
	applyScale { |nextStep, pageWidth = 1272|
		var size;
		step = nextStep ? step;
		metrics = CQGuiScale.metrics(step);
		cellW = CQGuiScale.loomCell(step, pageWidth);
		cellH = metrics[\cellHeight];
		labW = metrics[\loomLabel];
		headerH = metrics[\headerHeight];
		size = Size(labW + (CQMatrix.destNames.size * cellW),
			CQMatrix.sourceNames.size * cellH);
		header !? { header.fixedSize = Size(size.width, headerH) };
		grid !? { grid.fixedSize = size };
		status !? {
			status.font = CQInk.font(\notice, metrics[\notice]);
			status.fixedHeight = metrics[\barHeight];
		};
		header !? { header.refresh };
		grid !? { grid.refresh };
		^this
	}

	free {
		inspector !? { inspector.cancel };
		ctl !? { ctl.remove };
		ctl = nil;
	}

	skin_ { |next|
		var palette;
		skin = next;
		palette = CQInk.palette(skin);
		grid.background = palette[\recess];
		header.background = palette[\recess];
		status.stringColor = palette[\line];
		grid.refresh;
		header.refresh;
		^this
	}

	// ---- private ----

	prBuild { |initialStep|
		var srcs = CQMatrix.sourceNames, dests = CQMatrix.destNames;
		var palette = CQInk.palette(\day), size;
		step = initialStep ? \compact;
		metrics = CQGuiScale.metrics(step);
		cellW = CQGuiScale.loomCell(step, 1272);
		cellH = metrics[\cellHeight];
		labW = metrics[\loomLabel];
		headerH = metrics[\headerHeight];
		size = Size(labW + (dests.size * cellW), srcs.size * cellH);
		header = UserView().fixedSize_(Size(size.width, headerH))
			.background_(palette[\recess]);
		header.drawFunc = {
			try { this.prDrawHeader(dests) } { |error| CQInkControl.noteDrawError(error) }
		};
		grid = UserView().fixedSize_(size).background_(palette[\recess]);
		grid.drawFunc = {
			try { this.prDrawGrid(srcs, dests) } { |error| CQInkControl.noteDrawError(error) }
		};
		grid.mouseDownAction = { |v, x, y, mod, btn, clickCount|
			this.prMouseDown(x, y, btn, clickCount ? 1, srcs, dests)
		};
		// mouseMoveAction only runs while a button is held; ordinary hover is
		// mouseOverAction in Qt. Keep both paths on the same hit geometry.
		grid.mouseOverAction = { |v, x, y| this.prMouseMove(x, y, srcs, dests) };
		grid.mouseMoveAction = { |v, x, y| this.prMouseMove(x, y, srcs, dests) };
		grid.mouseUpAction = { |v, x, y, mod, btn|
			this.prMouseUp(btn, srcs, dests)
		};
		grid.mouseLeaveAction = {
			if(inspector.isActive.not) {
				hoverSi = nil;
				hoverDi = nil;
				grid.refresh;
				header.refresh;
			};
		};
		status = StaticText().string_("select a knot · exact route and weight appear here")
			.font_(CQInk.font(\notice, metrics[\notice])).stringColor_(palette[\line])
			.align_(\left).fixedHeight_(metrics[\barHeight]);
		view = grid;
		ctl = SimpleController(matrix);
		ctl.put(\patch, { |obj, what, src, dest, wght|
			{
				grid.refresh;
				if(src == selectedSrc and: { dest == selectedDest }) {
					this.prShow(src, dest, wght)
				};
			}.defer
		});
		ctl.put(\matrix, { { grid.refresh; this.prRefreshSelection }.defer });
		ctl.put(\clear, { { grid.refresh; this.prRefreshSelection }.defer });
	}

	prDrawHeader { |dests|
		var colors = CQInk.palette(skin), focusDi = hoverDi;
		var groups = this.prDestGroups, ruleY = metrics[\subgroup] + 8;
		focusDi = focusDi ? CQMatrix.destIndex(selectedDest);
		groups.do { |group|
			var left = labW + (group[\start] * cellW);
			var width = group[\count] * cellW;
			Pen.stringCenteredIn(group[\title],
				Rect(left, 2, width, metrics[\subgroup] + 4),
				CQInk.font(\label, metrics[\subgroup]), colors[\line]);
			Pen.strokeColor = colors[\whisper].copy.alpha_(0.45);
			Pen.width = CQInk.strokes[\hairline];
			Pen.line(Point(left + 5, ruleY), Point(left + width - 5, ruleY));
			Pen.stroke;
		};
		dests.do { |dest, di|
			var color = this.prDestColor(dest, colors);
			if(di != focusDi) { color = color.copy.alpha_(0.72) };
			Pen.use {
				Pen.translate(labW + (di * cellW) + (cellW / 5).max(8), headerH - 7);
				Pen.rotate(-0.25pi);
				Pen.stringAtPoint(this.prDestLabel(dest), 0@0,
					CQInk.font(\range, metrics[\loomDestSize]), color);
			};
		};
	}

	prDrawGrid { |srcs, dests|
		var colors = CQInk.palette(skin);
		var size = Size(labW + (dests.size * cellW), srcs.size * cellH);
		var focusSi = hoverSi ? CQMatrix.srcIndex(selectedSrc);
		var focusDi = hoverDi ? CQMatrix.destIndex(selectedDest);
		var selectedSi = CQMatrix.srcIndex(selectedSrc);
		var selectedDi = CQMatrix.destIndex(selectedDest);

		focusSi !? {
			Pen.fillColor = colors[\line].copy.alpha_(0.035);
			Pen.fillRect(Rect(labW, focusSi * cellH, size.width - labW, cellH));
		};
		focusDi !? {
			Pen.fillColor = colors[\line].copy.alpha_(0.035);
			Pen.fillRect(Rect(labW + (focusDi * cellW), 0, cellW, size.height));
		};

		Pen.strokeColor = colors[\whisper].copy.alpha_(0.18);
		Pen.width = CQInk.strokes[\hairline];
		srcs.do { |src, si|
			var y = (si * cellH) + (cellH / 2);
			Pen.line(Point(labW, y), Point(size.width, y));
		};
		dests.do { |dest, di|
			var x = labW + (di * cellW) + (cellW / 2);
			Pen.line(Point(x, 0), Point(x, size.height));
		};
		Pen.stroke;

		// Canonical groups stay exact; stronger seams make the flat ledger legible.
		Pen.strokeColor = colors[\whisper].copy.alpha_(0.48);
		#[5, 10, 12, 14].do { |row|
			Pen.line(Point(0, row * cellH), Point(size.width, row * cellH));
		};
		#[5, 10, 15, 20].do { |column|
			Pen.line(Point(labW + (column * cellW), 0),
				Point(labW + (column * cellW), size.height));
		};
		Pen.stroke;

		srcs.do { |src, si|
			var y = (si * cellH) + (cellH / 2), familyColor;
			var labelColor = if(si == focusSi) { colors[\line] } { colors[\whisper] };
			familyColor = colors[this.prSourceFamily(src)];
			// the shared content inset: the loom's row names begin on the same
			// vertical line as every other Drawer page's first ink
			Pen.stringAtPoint(this.prSourceLabel(src),
				metrics[\contentInset]@(y - (metrics[\loomLabelSize] / 2) - 1),
				CQInk.font(\label, metrics[\loomLabelSize]), labelColor);
			Pen.fillColor = familyColor;
			Pen.fillOval(Rect(labW - 15, y - 3, 6, 6));
		};

		dests.do { |dest, di|
			srcs.do { |src, si|
				var wght = matrix.getPatch(src, dest);
				var cx = labW + (di * cellW) + (cellW / 2);
				var cy = (si * cellH) + (cellH / 2);
				var selected = (si == selectedSi) and: { di == selectedDi };
				var hovered = (si == hoverSi) and: { di == hoverDi };
				var auditioning = inspector.isActive
					and: { inspector.src == src } and: { inspector.dest == dest };
				this.prDrawKnot(cx, cy, wght, selected, hovered, auditioning, colors);
			};
		};
	}

	prDrawKnot { |cx, cy, weight, selected, hovered, auditioning, colors|
		var radius, knotY = cy, color, rect;
		if(weight == 0) {
			Pen.fillColor = colors[\whisper].copy.alpha_(0.26);
			Pen.fillOval(Rect(cx - 1.25, cy - 1.25, 2.5, 2.5));
		} {
			radius = 3 + (weight.abs * 6);
			if(auditioning) { knotY = knotY - 2 };
			color = if(weight > 0) { colors[\verso] } { colors[\inverso] };
			color = color.copy.alpha_(0.48 + (weight.abs * 0.52));
			rect = Rect(cx - radius, knotY - radius, radius * 2, radius * 2);
			Pen.fillColor = color;
			Pen.fillOval(rect);
			Pen.strokeColor = colors[\line].copy.alpha_(0.72);
			Pen.width = CQInk.strokes[\hairline];
			Pen.strokeOval(rect);
			if(weight > 0) {
				Pen.line(Point(cx, knotY - radius - 4), Point(cx, knotY - radius));
			} {
				Pen.line(Point(cx, knotY + radius), Point(cx, knotY + radius + 4));
				Pen.line(Point(cx - 4, knotY + radius + 1),
					Point(cx + 4, knotY + radius + 4));
			};
			Pen.stroke;
		};
		if(selected or: { hovered }) {
			if(hovered) {
				Pen.fillColor = colors[\spark].copy.alpha_(0.16);
				Pen.addRoundedRect(Rect(cx - (cellW / 2) + 3,
					cy - (cellH / 2) + 2, cellW - 6, cellH - 4), 4, 4);
				Pen.fill;
			};
			Pen.strokeColor = if(hovered) {
				colors[\spark].copy.alpha_(0.92)
			} {
				colors[\line].copy.alpha_(0.9)
			};
			Pen.width = if(selected or: { hovered }) {
				CQInk.strokes[\strong]
			} {
				CQInk.strokes[\normal]
			};
			Pen.addRoundedRect(Rect(cx - (cellW / 2) + 4, cy - (cellH / 2) + 3,
				cellW - 8, cellH - 6), 3, 3);
			Pen.stroke;
		};
	}

	prCellAt { |x, y, srcs, dests|
		var di = ((x - labW) / cellW).floor.asInteger;
		var si = (y / cellH).floor.asInteger;
		if(di < 0 or: { di >= dests.size } or: { si < 0 } or: { si >= srcs.size }) {
			^nil
		};
		^[si, di]
	}

	prMouseDown { |x, y, btn, clickCount, srcs, dests|
		var cell = this.prCellAt(x, y, srcs, dests);
		if(cell.isNil) { ^this };
		selectedSrc = srcs[cell[0]];
		selectedDest = dests[cell[1]];
		inspector.press(selectedSrc, selectedDest, btn, clickCount, y);
		this.prShow(selectedSrc, selectedDest,
			matrix.getPatch(selectedSrc, selectedDest));
		grid.refresh;
		header.refresh;
	}

	prMouseMove { |x, y, srcs, dests|
		var cell;
		if(inspector.isActive) {
			inspector.drag(y);
			this.prShow(inspector.src, inspector.dest,
				matrix.getPatch(inspector.src, inspector.dest));
			grid.refresh;
			^this
		};
		cell = this.prCellAt(x, y, srcs, dests);
		if(cell.isNil) {
			if(hoverSi.notNil or: { hoverDi.notNil }) {
				hoverSi = nil;
				hoverDi = nil;
				grid.refresh;
				header.refresh;
			};
		} {
			if(cell[0] != hoverSi or: { cell[1] != hoverDi }) {
				hoverSi = cell[0];
				hoverDi = cell[1];
				grid.refresh;
				header.refresh;
			};
		};
	}

	prMouseUp { |btn, srcs, dests|
		var activeSrc = inspector.src, activeDest = inspector.dest;
		if(inspector.isActive.not) { ^this };
		inspector.release(btn);
		this.prShow(activeSrc, activeDest, matrix.getPatch(activeSrc, activeDest));
		grid.refresh;
	}

	prRefreshSelection {
		if(selectedSrc.notNil and: { selectedDest.notNil }) {
			this.prShow(selectedSrc, selectedDest,
				matrix.getPatch(selectedSrc, selectedDest))
		};
		^this
	}

	prShow { |src, dest, wght|
		var amount = wght.round(0.01);
		var signed = if(wght > 0) { "+" ++ amount } { amount.asString };
		var polarity = case
			{ wght > 0 } { "verso" }
			{ wght < 0 } { "inverso" }
			{ "unpatched" };
		status.string = "%  →  %     % · %".format(src, dest, signed, polarity);
	}

	prDestGroups {
		^[(start: 0, count: 5, title: "coco 1"),
			(start: 5, count: 5, title: "coco 2"),
			(start: 10, count: 5, title: "castle · fm"),
			(start: 15, count: 5, title: "castle · spesal"),
			(start: 20, count: 2, title: "decks")]
	}

	prDestLabel { |dest|
		^switch(dest,
			\coco1_spaf, { "spaf" }, \coco1_inVca, { "in" },
			\coco1_fbVca, { "fb" }, \coco1_flip, { "flip" },
			\coco1_skip, { "skip" }, \coco2_spaf, { "spaf" },
			\coco2_inVca, { "in" }, \coco2_fbVca, { "fb" },
			\coco2_flip, { "flip" }, \coco2_skip, { "skip" },
			\castle1_fm, { "1" }, \castle2_fm, { "2" },
			\castle3_fm, { "3" }, \castle4_fm, { "4" }, \castle5_fm, { "5" },
			\castle1_spesal, { "1" }, \castle2_spesal, { "2" },
			\castle3_spesal, { "3" }, \castle4_spesal, { "4" },
			\castle5_spesal, { "5" }, \deckA_rate, { "a · rate" },
			\deckB_rate, { "b · rate" }, { dest.asString })
	}

	prDestColor { |dest, colors|
		var name = dest.asString;
		^case
			{ name.find("spaf").notNil } { colors[\spaf] }
			{ name.find("_fm").notNil } { colors[\fm] }
			{ name.find("spesal").notNil } { colors[\input] }
			{ colors[\whisper] }
	}

	prSourceLabel { |src|
		^switch(src,
			\envA, { "env a" }, \envB, { "env b" },
			\coco1, { "coco 1" }, \coco2, { "coco 2" },
			\org1, { "org 1" }, \org2, { "org 2" },
			{ src.asString })
	}

	prSourceFamily { |src|
		var name = src.asString;
		^case
			{ name.beginsWith("tri") } { \tri }
			{ name.beginsWith("env") } { \input }
			{ name.beginsWith("org") } { \organ }
			{ \cas }
	}
}
