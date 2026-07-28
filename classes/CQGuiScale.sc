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

	// The persistent chrome — the toolbar and the Master strip — is a *frame*,
	// not content: its height is fixed (SPEC §6.2 keeps PANIC and the notice
	// line permanently visible, and the strip's layout is muscle memory in both
	// skins), so it does not follow the Drawer's steps. It does share their
	// floor, because the same eye reads both and 10 px beside a 12 px workbench
	// reads as an oversight rather than a hierarchy.
	*chrome {
		^(label: 12, mono: 12, notice: 12, buttonHeight: 26, tabHeight: 26)
	}

	// A Drawer page's own rect is the drawer stage minus its tab row and
	// margins — measured on the real Qt composition 2026-07-28 at 1280×720
	// (stage 1280×570 → page 1272×526) and again on a maximized window
	// (stage 1442×730 → page 1434×686). Both agree, so the inset is a constant.
	// Derived from the chrome it actually sits under (the drawer stage's own
	// 4 px margins plus the tab row), so a change to the tab height cannot
	// silently shift which step a window qualifies for.
	*pageInset {
		^Point(8, 4 + this.chrome[\tabHeight] + 8 + 4)
	}

	*pageSizeFor { |stageWidth, stageHeight|
		var inset = this.pageInset;
		^Size((stageWidth - inset.x).max(0), (stageHeight - inset.y).max(0))
	}

	// A little room to breathe above the bare minimum, so the bigger step is
	// never chosen for a page that would be full to its last pixel.
	*comfort { ^16 }

	// The narrowest and shortest page a step can actually be drawn on. Both are
	// DERIVED from the step's own metrics rather than written down beside them:
	// a magic threshold silently stops matching the moment a metric moves, and
	// this one decides whether a maximized window gets bigger type at all.
	*minimumWidth { |step|
		var m = this.metrics(step);
		^m[\loomLabel] + (m[\cellMin] * CQMatrix.destNames.size)
			+ (2 * m[\pageMargin])
	}

	*stepFor { |pageWidth, pageHeight|
		^if(pageWidth >= this.minimumWidth(\roomy)
			and: { pageHeight >= (this.minimumHeight(\roomy) + this.comfort) })
			{ \roomy } { \compact }
	}

	*metrics { |step|
		^switch(step, \roomy, { this.prRoomy }, { this.prCompact })
	}

	// Qt text views need room for the face's ascent/descent AND their own
	// vertical bearings. A font-size-only fixedHeight clips the tops and
	// descenders on Windows; every deliberately short text band derives its
	// height here and keeps reviewed clearance above and below the glyphs.
	*textBandHeight { |fontSize|
		^(fontSize + 8).ceil.asInteger
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
			// one inset for every page's first ink, so the Workshop's labels,
			// the loom's row names and the seed bank's boxes all begin on the
			// same vertical line inside the shared rail
			contentInset: 4, rowGap: 6,
			ledgerLabel: 96, ledgerValue: 58, ledgerGap: 10,
			// inspector loom: the compact page has no spare height, so the
			// header and cell height stay exactly as reviewed and only the
			// row labels and status text grow. The cell cap is deliberately
			// well above the minimum window's own 52, so a page that is wide
			// but too short for the roomy step still spends its width on the
			// rail instead of turning all of it into slate.
			loomLabel: 80, cellMin: 44, cellMax: 64, cellHeight: 24,
			// the rotated destination names are the loom's own column headings
			// and were the smallest type on the page; the header band has the
			// room for them because they hang from its bottom edge
			headerHeight: 64, loomLabelSize: 11, loomDestSize: 11,
			// list rows and small boxes
			listRow: 20, boxHeight: 24, barHeight: 26, toolTitleTop: 5
		)
	}

	*prRoomy {
		^(
			step: \roomy,
			pageMargin: 32, vMargin: 16, gutter: 20, bandGap: 20,
			title: 16, label: 15, value: 15, subgroup: 13, notice: 15,
			mono: 15, tiny: 11,
			// A 1080p laptop at 125 % Windows scaling reports a maximized page
			// around 1528x630 logical px, and that page must reach this step —
			// otherwise the one window size the instrument is actually played
			// at would never get the bigger type. The row and band heights are
			// therefore kept as tight as 15 px type allows.
			rowHeight: 28, titleHeight: 26, groupHeight: 22,
			contentInset: 6, rowGap: 8,
			ledgerLabel: 120, ledgerValue: 72, ledgerGap: 12,
			loomLabel: 104, cellMin: 54, cellMax: 76, cellHeight: 28,
			headerHeight: 74, loomLabelSize: 13, loomDestSize: 13,
			listRow: 26, boxHeight: 28, barHeight: 30, toolTitleTop: 6
		)
	}

	// ---- the shared page frame ----

	// Every Drawer page opens with exactly this much air between the tab row
	// and its first ink, and centres the same rail, so the four pages read as
	// one workbench rather than four layouts that happen to be adjacent. The
	// bottom is deliberately free: the pages hold different amounts of content.
	*pageFrame { |step, railWidth, pageWidth|
		var m = this.metrics(step);
		^(top: m[\vMargin], side: ((pageWidth - railWidth) / 2).max(m[\pageMargin]),
			gap: m[\bandGap])
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
