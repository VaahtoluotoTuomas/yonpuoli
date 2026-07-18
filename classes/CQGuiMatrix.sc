// CQGuiMatrix — the banana-bay grid view (SPEC §6.2): 16 sources x 22
// destinations in one UserView (352 child views would crawl). Cell color =
// signed weight (orange verso, blue inverso, like the banana polarity code);
// drag vertically on a cell to set its weight, right-click to unpatch.
// Pure view: every write goes through matrix.patch (clamped there), every
// repaint comes from the matrix's dependant notifications.

CQGuiMatrix {
	classvar cellW = 26, cellH = 13, labW = 46, labH = 56;

	var matrix, <view, grid, status, ctl;
	var dragSi, dragDi, dragW, dragY;

	*new { |matrix| ^super.newCopyArgs(matrix).prBuild }

	free { ctl !? { ctl.remove }; ctl = nil }

	// ---- private ----

	prBuild {
		var srcs = CQMatrix.sourceNames, dests = CQMatrix.destNames;
		var w = labW + (dests.size * cellW);
		var h = labH + (srcs.size * cellH);
		grid = UserView().fixedSize_(Size(w, h)).background_(Color.gray(0.92));
		grid.drawFunc = { this.prDraw(srcs, dests) };
		grid.mouseDownAction = { |v, x, y, mod, btn|
			this.prMouseDown(x, y, btn, srcs, dests)
		};
		grid.mouseMoveAction = { |v, x, y| this.prMouseMove(y, srcs, dests) };
		grid.mouseUpAction = { dragSi = nil };
		status = StaticText().string_("banana bay — drag a cell, right-click clears")
			.font_(Font.sansSerif(9)).stringColor_(Color.gray(0.4));
		view = View().layout_(
			VLayout(grid, status).spacing_(1).margins_(0));
		ctl = SimpleController(matrix);
		ctl.put(\patch, { |obj, what, src, dest, wght|
			{ grid.refresh; this.prShow(src, dest, wght) }.defer
		});
		ctl.put(\clear, { { grid.refresh }.defer });
	}

	prDraw { |srcs, dests|
		var colors = CQGuiWidgets.colors;
		Pen.font = Font.sansSerif(8);
		dests.do { |dest, di|                       // rotated column headers
			Pen.use {
				Pen.translate(labW + (di * cellW) + (cellW / 2) + 3, labH - 4);
				Pen.rotate(-0.5pi);
				Pen.stringAtPoint(dest.asString, 0@0, Font.sansSerif(8),
					Color.gray(0.25));
			};
		};
		srcs.do { |src, si|                         // row labels
			Pen.stringAtPoint(src.asString, 2@(labH + (si * cellH) + 2),
				Font.sansSerif(8), Color.gray(0.25));
		};
		dests.do { |dest, di|
			srcs.do { |src, si|
				var wght = matrix.getPatch(src, dest);
				var r = Rect(labW + (di * cellW), labH + (si * cellH),
					cellW - 1, cellH - 1);
				Pen.fillColor = case
					{ wght > 0 } { colors[\tri].alpha_(wght.abs.max(0.15)) }
					{ wght < 0 } { colors[\fm].alpha_(wght.abs.max(0.15)) }
					{ Color.gray(0.97) };
				Pen.fillRect(r);
			};
		};
	}

	prCellAt { |x, y, srcs, dests|
		var di = ((x - labW) / cellW).floor.asInteger;
		var si = ((y - labH) / cellH).floor.asInteger;
		if(di < 0 or: { di >= dests.size } or: { si < 0 } or: { si >= srcs.size }) {
			^nil
		};
		^[si, di]
	}

	prMouseDown { |x, y, btn, srcs, dests|
		var cell = this.prCellAt(x, y, srcs, dests);
		if(cell.isNil) { ^this };
		if(btn == 1) {                              // right-click = pull the banana
			matrix.unpatch(srcs[cell[0]], dests[cell[1]]);
			dragSi = nil;
			^this
		};
		dragSi = cell[0];
		dragDi = cell[1];
		dragW = matrix.getPatch(srcs[dragSi], dests[dragDi]);
		dragY = y;
		this.prShow(srcs[dragSi], dests[dragDi], dragW);
	}

	// drag up = verso, down = inverso; 100 px covers the full -1..1 throw
	prMouseMove { |y, srcs, dests|
		if(dragSi.isNil) { ^this };
		matrix.patch(srcs[dragSi], dests[dragDi],
			dragW + ((dragY - y) * 0.02));
	}

	prShow { |src, dest, wght|
		status.string = "% -> %   %".format(src, dest,
			if(wght >= 0) { "+" ++ wght.round(0.01) } { wght.round(0.01) });
	}
}
