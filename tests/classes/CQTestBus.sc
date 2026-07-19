// Control/audio bus double for funnel tests.

CQTestBus {
	var <index, <writes;

	*new { |index = 0| ^super.newCopyArgs(index).prInit }

	prInit { writes = List.new }

	setn { |values| writes.add(values.copy) }
}
