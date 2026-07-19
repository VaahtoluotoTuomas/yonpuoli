// Deterministic number allocator used by the hermetic async-resource fixtures.

CQTestAllocator {
	var next = 0;

	alloc { |n = 1|
		var first = next;
		next = next + n;
		^first
	}

	free { |index| ^index }
}
