// CQSpellbook — notification-stream matrix history for GUI_SPEC §8.
// It listens below every client at CQMatrix, keeps the last committed image,
// and therefore sees code, GUI, preset, snapshot and weather operations while
// excluding audition/morph/compass frame writes marked committed:false.

CQSpellbook {
	var matrix, ctl, committedMatrix;
	var <entries;

	*new { |matrix| ^super.newCopyArgs(matrix).prInit }

	prInit {
		entries = List.new;
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
			this.changed(\entry, entry);
		};
		if(signal == \patch) {
			this.prCommitCell(after, src, dest)
		} {
			committedMatrix = this.prCopyMatrix(after)
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
