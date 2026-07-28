// CQGuiScale — the Drawer's two reviewed size steps (GUI_SPEC §8, §16.4).
//
// The workbench is the one place in this instrument that is *only* text, rules
// and rails, so legibility is its whole job. A windowed 1280×720 gives a Drawer
// page 1272×526 px; a maximized window gives far more, and leaving that as
// empty slate wastes the readability it could buy. So the pages have two
// reviewed compositions rather than one stretched or one scaled continuously:
//
//   compact  — the 1280×720 minimum and any small window
//   roomy    — a maximized/large window: bigger type, taller rows, a wider rail
//
// Two discrete steps, never a continuous zoom: type stays at sizes that were
// actually looked at, coordinates stay stable inside a step, and a resize can
// only ever move between two known layouts. §16.10's "font sizes stay in px"
// still binds the Day panel and the Night landscape — the ink of the organism
// does not thicken when the paper grows. This class governs the Drawer alone.
//
// Everything here is pure arithmetic: no Qt, no state, fully unit-testable.

CQGuiScale {
	*steps { ^#[compact, roomy] }

	// A Drawer page's own rect is the drawer stage minus its tab row and
	// margins — measured on the real Qt composition 2026-07-28 at 1280×720
	// (stage 1280×570 → page 1272×526) and again on a maximized window
	// (stage 1442×730 → page 1434×686). Both agree, so the inset is a constant.
	*pageInset { ^Point(8, 44) }

	*pageSizeFor { |stageWidth, stageHeight|
		var inset = this.pageInset;
		^Size((stageWidth - inset.x).max(0), (stageHeight - inset.y).max(0))
	}

	// The roomy step must never be chosen for a page that cannot hold it: the
	// height floor is `minimumHeight(\roomy)` plus a margin of comfort.
	*stepFor { |pageWidth, pageHeight|
		^if(pageWidth >= 1400 and: { pageHeight >= 640 }) { \roomy } { \compact }
	}

	*metrics { |step|
		^switch(step, \roomy, { this.prRoomy }, { this.prCompact })
	}

	// ---- the two reviewed records ----

	*prCompact {
		^(
			step: \compact,
			// page frame
			pageMargin: 24, vMargin: 12, gutter: 16, bandGap: 16,
			// type (§16.4's 10/11/9 were the Day panel's laptop-legibility
			// values; this page is read at arm's length and is nothing but
			// text, so it starts where Day ends)
			title: 13, label: 12, value: 12, subgroup: 11, notice: 12,
			mono: 12, tiny: 9,
			// ledger row
			rowHeight: 24, titleHeight: 22, groupHeight: 18,
			ledgerLabel: 96, ledgerValue: 58, ledgerInset: 12, ledgerGap: 10,
			// inspector loom: the compact page has no spare height, so the
			// header and cell height stay exactly as reviewed and only the
			// row labels and status text grow
			loomLabel: 80, cellMin: 44, cellMax: 52, cellHeight: 24,
			headerHeight: 64, loomLabelSize: 11, loomDestSize: 9,
			// list rows and small boxes
			listRow: 20, boxHeight: 24, barHeight: 26
		)
	}

	*prRoomy {
		^(
			step: \roomy,
			pageMargin: 32, vMargin: 16, gutter: 20, bandGap: 20,
			title: 16, label: 15, value: 15, subgroup: 13, notice: 15,
			mono: 15, tiny: 11,
			rowHeight: 30, titleHeight: 28, groupHeight: 24,
			ledgerLabel: 120, ledgerValue: 72, ledgerInset: 16, ledgerGap: 12,
			loomLabel: 104, cellMin: 54, cellMax: 72, cellHeight: 30,
			headerHeight: 78, loomLabelSize: 13, loomDestSize: 11,
			listRow: 26, boxHeight: 28, barHeight: 32
		)
	}

	// ---- the shared content rail ----

	// One rail width serves every Drawer page, so the four pages line up as one
	// workbench. Its width is the Inspector loom's own: the loom is the widest
	// fixed grid in the app, so letting it set the rail keeps the ledger, the
	// journal and the seed bank on the same edge. The cell grows with the page
	// but is clamped — past the clamp the extra becomes slate, because a knot
	// grid spread over two thousand pixels stops reading as a grid.
	// The floor keeps a grid from collapsing into illegibility, but it may never
	// push the rail off its own page: a minimum that overflows would silently
	// break the margin this class exists to guarantee. So the floor applies only
	// where the page can afford it.
	*loomCell { |step, pageWidth|
		var m = this.metrics(step);
		var usable = pageWidth - (2 * m[\pageMargin]) - m[\loomLabel];
		var fit = (usable / CQMatrix.destNames.size).floor.asInteger;
		^fit.min(m[\cellMax]).max(m[\cellMin].min(fit)).max(8)
	}

	*railWidth { |step, pageWidth|
		var m = this.metrics(step);
		^m[\loomLabel] + (this.loomCell(step, pageWidth) * CQMatrix.destNames.size)
	}

	*loomHeight { |step|
		^this.metrics(step)[\cellHeight] * CQMatrix.sourceNames.size
	}

	// Split a rail into `count` columns and `count - 1` gutters, summing to the
	// rail exactly. An odd count gives its remainder to the middle, so the two
	// Coco ledgers stay literally the same width — their symmetry is structural,
	// not a coincidence of arithmetic.
	*columns { |total, count = 3, gutter = 16|
		var span = total - (gutter * (count - 1));
		var base = span div: count, rest = span % count;
		var widths = Array.fill(count, base), middle = count div: 2, slots;
		// An odd split hands its whole remainder to the middle, so the outer
		// pair stays identical — that is what keeps the two Coco ledgers the
		// same width by construction. Only up to two pixels can land there;
		// a larger remainder is spread outward instead of bulging the centre.
		if(count.odd and: { rest <= 2 }) {
			widths[middle] = widths[middle] + rest;
		} {
			slots = Array.fill(count, { |i|
				if(i.even) { i div: 2 } { count - 1 - (i div: 2) }
			});
			rest.do { |i| widths[slots[i]] = widths[slots[i]] + 1 };
		};
		^widths
	}

	// ---- text measurement (used for buttons, which must not clip at any step) ----

	// `String.bounds` needs a live font; the fallback keeps serverless and
	// headless builds honest rather than letting a button collapse to nothing.
	*textWidth { |string, size = 12, mono = false|
		var font = if(mono) { Font.monospace(size) } { Font.sansSerif(size) };
		^try { string.asString.bounds(font).width }
			{ string.asString.size * size * if(mono) { 0.62 } { 0.55 } }
	}

	// A Drawer button is sized to its own label at the current step, so growing
	// the type can never crop a caption (the old fixed widths were measured for
	// 10 px text and had no headroom at all).
	*buttonWidth { |step, label, glyph = false|
		var m = this.metrics(step);
		var pad = if(glyph) { 34 } { 22 };
		^(this.textWidth(label, m[\label]) + pad).ceil.asInteger.max(48)
	}

	// ---- vertical budgets (asserted against the measured page height) ----

	*serviceBandHeight { |step|
		var m = this.metrics(step);
		^m[\titleHeight] + (2 * m[\rowHeight])
	}

	// the tallest Workshop island — a twelve-row Coco with four subgroups
	*tallestIslandHeight { |step|
		var m = this.metrics(step);
		^m[\titleHeight] + (4 * m[\groupHeight]) + (12 * m[\rowHeight])
	}

	// what the Workshop page must have before it may use this step at all
	*minimumHeight { |step|
		var m = this.metrics(step);
		^(2 * m[\vMargin]) + this.serviceBandHeight(step) + m[\bandGap]
			+ this.tallestIslandHeight(step)
	}
}
