// CQSpellbook — notification-stream matrix history for GUI_SPEC §8.
// It listens below every client at CQMatrix, keeps the last committed image,
// and therefore sees code, GUI, preset, snapshot and weather operations while
// excluding audition/morph/compass frame writes marked committed:false.

CQSpellbook {
	var matrix, ctl, committedMatrix;
	var <entries;
	var <lineCount;

	// SOAK-1 criterion 5, measured 2026-07-31: this journal had no cap, and it
	// was the sclang memory ramp the soak saw twice (~20 MB/h, never settling).
	// Every committed act appends one line per changed cell, and a dense act — a
	// snapshot morph, a recall, a preset, a shake — changes hundreds at once, so
	// an evening of playing retained hundreds of megabytes that nothing read
	// again. The probe isolated it here rather than anywhere else in the GUI
	// because prObserve is the only place in the app that tells a committed write
	// from an uncommitted one: the same notification stream at 15 Hz, uncommitted,
	// was flat for the whole measurement.
	//
	// The bound is counted in LINES because the page is a line list — the limit is
	// in the same unit as the thing a player reads. Whole oldest entries leave,
	// never part of one, so every operation that remains stays legible as a block.
	// It does not touch export: presetDataString reads committedMatrix.
	*lineLimit { ^20000 }

	*new { |matrix| ^super.newCopyArgs(matrix).prInit }

	prInit {
		entries = List.new;
		lineCount = 0;
		committedMatrix = this.prCopyMatrix(matrix.fullMatrix);
		ctl = SimpleController(matrix);
		ctl.put(\patch, { |obj, what, src, dest, weight,
			origin, committed, operation|
			this.prObserve(\patch, origin ? \code, committed ? true,
				operation ? \patch, src, dest)
		});
		ctl.put(\matrix, { |obj, what, origin, committed, operation|
			this.prObserve(\matrix, origin ? \code, committed ? true,
				operation ? \writeMatrix)
		});
		ctl.put(\clear, { |obj, what, origin, committed, operation|
			this.prObserve(\clear, origin ? \code, committed ? true,
				operation ? \clear)
		});
		^this
	}

	free {
		ctl !? { ctl.remove };
		ctl = nil;
	}

	entryCount { ^entries.size }
	lastEntry { ^entries.last }

	lines {
		var result = List.new;
		entries.do { |entry| entry[\lines].do { |line| result.add(line) } };
		^result.asArray
	}

	// One record per visible journal row. Keeping the owning entry makes a
	// selected code line legible as part of its committed batch operation.
	lineRecords {
		var result = List.new;
		entries.do { |entry, entryIndex|
			entry[\lines].do { |line, lineIndex|
				result.add((
					text: line,
					entryIndex: entryIndex,
					lineIndex: lineIndex,
					lineCount: entry[\lines].size,
					operation: entry[\operation]
				));
			};
		};
		^result.asArray
	}

	// One row's ownership, without building a record for every other row first.
	// The page asks this on every selection change, and lineRecords answered it
	// by allocating an Event per line in the whole journal — 20 000 of them at
	// the line limit, to read one.
	recordAt { |index|
		var seen = 0;
		if(index.isNil or: { index < 0 }) { ^nil };
		entries.do { |entry, entryIndex|
			var size = entry[\lines].size;
			if(index < (seen + size)) {
				^(
					text: entry[\lines][index - seen],
					entryIndex: entryIndex,
					lineIndex: index - seen,
					lineCount: size,
					operation: entry[\operation]
				)
			};
			seen = seen + size;
		};
		^nil
	}

	text { ^this.lines.join(Char.nl.asString) }

	// Export the committed image, never a banana that happens to be auditioning.
	presetDataString {
		var triples = this.prTriples(committedMatrix);
		var body = triples.collect { |triple|
			"[%, %, %]".format(
				this.prSymbolString(triple[0]),
				this.prSymbolString(triple[1]),
				triple[2].round(0.0001).asCompileString)
		}.join(", ");
		^"(matrix: [" ++ body ++ "])"
	}

	// ---- private ----

	prObserve { |signal, origin, committed, operation, src, dest|
		var after, diff, lines, entry;
		if(committed != true) { ^this };
		after = matrix.fullMatrix;
		diff = if(signal == \patch) {
			this.prCellDiff(committedMatrix, after, src, dest)
		} {
			this.prDiff(committedMatrix, after)
		};
		if(diff.notEmpty) {
			lines = diff.collect { |cell| this.prPatchLine(cell) };
			if(signal != \patch) {
				lines = ["// " ++ operation.asString] ++ lines;
			};
			entry = (
				origin: origin,
				operation: operation,
				lines: lines.asArray,
				diff: diff.asArray
			);
			entries.add(entry);
			lineCount = lineCount + entry[\lines].size;
			this.prTrim;
			this.changed(\entry, entry);
		};
		if(signal == \patch) {
			this.prCommitCell(after, src, dest)
		} {
			committedMatrix = this.prCopyMatrix(after)
		};
		^this
	}

	// The newest entry is never dropped, however long it is: a journal that
	// answered "nothing happened" to the act a hand just made would be a worse
	// defect than a long one.
	prTrim {
		var limit = this.class.lineLimit;
		while { (lineCount > limit) and: { entries.size > 1 } } {
			lineCount = lineCount - entries.removeAt(0)[\lines].size;
		};
		^this
	}

	prCellDiff { |before, after, src, dest|
		var si = CQMatrix.srcIndex(src), di = CQMatrix.destIndex(dest);
		if(si.isNil or: { di.isNil } or: { before[di][si] == after[di][si] }) {
			^List.new
		};
		^List[(src: src, dest: dest, weight: after[di][si])]
	}

	prCommitCell { |after, src, dest|
		var si = CQMatrix.srcIndex(src), di = CQMatrix.destIndex(dest);
		if(si.notNil and: { di.notNil }) {
			committedMatrix[di][si] = after[di][si]
		};
		^this
	}

	prDiff { |before, after|
		var result = List.new;
		CQMatrix.destNames.do { |dest, di|
			CQMatrix.sourceNames.do { |src, si|
				if(before[di][si] != after[di][si]) {
					result.add((src: src, dest: dest, weight: after[di][si]));
				};
			};
		};
		^result
	}

	prPatchLine { |cell|
		var value = cell[\weight];
		value = if(value == 0) { "0" } { value.round(0.0001).asCompileString };
		^"CQ.patch(%, %, %)".format(
			this.prSymbolString(cell[\src]),
			this.prSymbolString(cell[\dest]),
			value)
	}

	prSymbolString { |symbol| ^"\\" ++ symbol.asString }

	prTriples { |dense|
		var result = List.new;
		CQMatrix.destNames.do { |dest, di|
			CQMatrix.sourceNames.do { |src, si|
				if(dense[di][si] != 0) {
					result.add([src, dest, dense[di][si]]);
				};
			};
		};
		^result.asArray
	}

	prCopyMatrix { |dense| ^dense.collect { |row| row.copy } }
}
