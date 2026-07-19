// Buffer protocol double for deterministic deck-load ownership tests.

CQTestBuffer {
	var <bufnum, <numFrames = 48000, <sampleRate = 48000;
	var <freed = false, <normalized = false;

	*new { |bufnum| ^super.newCopyArgs(bufnum) }

	normalize { |peak = 0.5|
		normalized = peak;
		^this
	}

	free {
		freed = true;
		^this
	}
}
