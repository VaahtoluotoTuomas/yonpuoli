// CQAudition — serverless transient matrix-preview state (GUI_SPEC §4.1).
// It owns exactly one {src, dest, prevW, audibleW} record. Every preview and
// revert uses CQMatrix's normal funnel with committed:false; plant republishes
// the exact heard value as one committed operation.

CQAudition {
	var matrix, candidate;

	*new { |matrix| ^super.newCopyArgs(matrix) }

	isActive { ^candidate.notNil }

	preview { |src, dest, weight|
		var same;
		if(CQMatrix.srcIndex(src).isNil or: { CQMatrix.destIndex(dest).isNil }) {
			^this
		};
		same = candidate.notNil
			and: { candidate[\src] == src }
			and: { candidate[\dest] == dest };
		if(same.not) {
			this.revert;
			candidate = (
				src: src,
				dest: dest,
				prevW: matrix.getPatch(src, dest),
				audibleW: nil
			);
		};
		matrix.patch(src, dest, weight,
			origin: \audition, committed: false, operation: \audition);
		candidate[\audibleW] = matrix.getPatch(src, dest);
		^this
	}

	plant {
		var current = candidate;
		if(current.isNil) { ^this };
		candidate = nil;
		matrix.patch(current[\src], current[\dest], current[\audibleW],
			origin: \gui, committed: true, operation: \plant);
		^this
	}

	revert {
		var current = candidate;
		if(current.isNil) { ^this };
		candidate = nil;
		matrix.patch(current[\src], current[\dest], current[\prevW],
			origin: \audition, committed: false, operation: \auditionRevert);
		^this
	}

	leaveRadius { ^this.revert }
	releaseVoid { ^this.revert }
	escape { ^this.revert }
	viewToggle { ^this.revert }
	windowClose { ^this.revert }
	cmdPeriod { ^this.revert }
	panic { ^this.revert }
	free { ^this.revert }
}
