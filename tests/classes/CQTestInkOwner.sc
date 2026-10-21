// Test double for CQGui's Ink-control registry. CQGuiWidgets hands every
// control it builds to its owner so one skin pass can re-ink them all; a unit
// test only needs to know that each control arrived exactly once.

CQTestInkOwner {
	var <controls, <notices, <hoverBindings;

	*new { ^super.new.prInit }

	prInit {
		controls = List.new;
		notices = List.new;
		hoverBindings = List.new;
		^this
	}

	registerInkControl { |control| controls.add(control); ^control }
	registerHover { |control, target| hoverBindings.add([control, target]); ^control }

	// the persistent MIC LIVE composition (SPEC §7)
	micNotice { |live| notices.add(live); ^this }

	micLive { ^notices.isEmpty.not and: { notices.last == true } }
}
