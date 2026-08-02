// CQInspector — serverless touch-and-listen state for the Drawer grid
// (GUI_SPEC §8). Press is a shared CQAudition preview, drag is transient fine
// edit, and release publishes exactly one committed/undoable operation.

CQInspector {
	var matrix, audition, grammar, inspectorPlant;
	var <src, <dest, <button, <startY, <priorWeight, beforeMatrix;
	var <dragging = false, <currentWeight;

	// A newly planted precision-grid cable follows the same first-touch law as
	// a Night cable. Fine drag/wheel editing still reaches the full 0.05..1
	// range, but a double-click must not create a gate cable that cannot fire.
	*new { |matrix, audition, grammar, inspectorPlant|
		inspectorPlant = inspectorPlant ?? { CQBananaGrammar.defaultHandStrength };
		^super.newCopyArgs(matrix, audition, grammar).prInit(inspectorPlant)
	}

	prInit { |plant|
		inspectorPlant = plant.clip(0, 1);
		^this
	}

	isActive { ^src.notNil }

	press { |nextSrc, nextDest, nextButton = 0, clickCount = 1, y = 0|
		var polarity;
		this.cancel;
		if(CQMatrix.srcIndex(nextSrc).isNil
			or: { CQMatrix.destIndex(nextDest).isNil }) { ^this };
		grammar.panic;
		if(clickCount >= 2) {
			grammar.performUndoable({
				matrix.patch(nextSrc, nextDest, inspectorPlant,
					origin: \gui, committed: true, operation: \plant)
			});
			^this
		};
		src = nextSrc;
		dest = nextDest;
		button = nextButton;
		startY = y;
		priorWeight = matrix.getPatch(src, dest);
		currentWeight = priorWeight;
		beforeMatrix = matrix.fullMatrix;
		dragging = false;
		polarity = if(button == 0) { 1 } { -1 };
		audition.preview(src, dest, polarity * inspectorPlant);
		^this
	}

	drag { |y|
		if(this.isActive.not) { ^this };
		if(dragging.not and: { (y - startY).abs < 2 }) { ^this };
		if(dragging.not) {
			audition.revert;
			dragging = true;
		};
		currentWeight = CQMatrix.clampWeight(priorWeight + ((startY - y) * 0.02));
		matrix.patch(src, dest, currentWeight,
			origin: \inspector, committed: false, operation: \inspectorFrame);
		^this
	}

	release { |releaseButton|
		var wasSrc, wasDest, wasButton, before, finalWeight;
		if(this.isActive.not) { ^this };
		wasSrc = src;
		wasDest = dest;
		wasButton = releaseButton ? button;
		before = beforeMatrix;
		if(dragging) {
			finalWeight = matrix.getPatch(wasSrc, wasDest);
			matrix.patch(wasSrc, wasDest, finalWeight,
				origin: \gui, committed: true, operation: \inspectorEdit);
			grammar.rememberMatrix(before);
			this.prReset;
			^this
		};
		audition.revert;
		this.prReset;
		if(wasButton == 1 and: {
			matrix.getPatch(wasSrc, wasDest) != 0 }) {
			grammar.performUndoable({
				matrix.patch(wasSrc, wasDest, 0,
					origin: \gui, committed: true, operation: \rip)
			});
		};
		^this
	}

	cancel {
		if(this.isActive) {
			if(dragging) {
				matrix.patch(src, dest, priorWeight,
					origin: \inspector, committed: false,
					operation: \inspectorRevert)
			} {
				audition.revert
			};
		};
		this.prReset;
		^this
	}

	free { ^this.cancel }

	// ---- private ----

	prReset {
		src = nil;
		dest = nil;
		button = nil;
		startY = nil;
		priorWeight = nil;
		beforeMatrix = nil;
		dragging = false;
		currentWeight = nil;
		^this
	}
}
