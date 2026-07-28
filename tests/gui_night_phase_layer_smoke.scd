// Focused Qt gate for NIGHT's lightweight 15 Hz Coco read heads. The moving
// comet must repaint its own transparent, hit-through layer without repainting
// the static shell. Prints CQ_GUI_PHASE_LAYER_PASSED / _FAILED as final marker.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));
var gui, ok = true, abort;
var check = { |condition, message|
	if(condition) { ("ok:   " ++ message).postln } {
		ok = false;
		("FAIL: " ++ message).postln;
	};
};

CQInkControl.resetDrawErrors;
SystemClock.sched(20, {
	"FAIL: phase-layer smoke watchdog".postln;
	"CQ_GUI_PHASE_LAYER_FAILED".postln;
	1.exit;
});

abort = { |error|
	error !? { error.reportError };
	("FAIL: GUI phase-layer smoke aborted"
		++ (error !? { ": " ++ error.errorString } ? "")).postln;
	gui !? { gui.free };
	"CQ_GUI_PHASE_LAYER_FAILED".postln;
	1.exit;
};

{
	try {
		gui = CQGui(state, matrix, config);
		AppClock.sched(0.35, {
			try {
				var map = gui.nightMap;
				var shellIndex, headIndex, endpointIndex;
				check.value(map.readHeadViews.size == 2,
					"each Coco owns one dedicated read-head layer");
				check.value(map.readHeadViews.every { |view|
					view.background.alpha == 0 and: { view.acceptsMouse.not }
				}, "read-head paper is transparent and hit-through");
				check.value((0..1).every { |i|
					map.readHeadViews[i].bounds == map.shellViews[i].bounds
				}, "each read-head layer follows its shell bounds exactly");
				shellIndex = map.container.children.indexOf(map.shellViews[0]);
				headIndex = map.container.children.indexOf(map.readHeadViews[0]);
				endpointIndex = map.container.children
					.indexOf(map.endpoints[\coco1][\view]);
				check.value(shellIndex < headIndex and: { headIndex < endpointIndex },
					"the comet paints above shell ink but below endpoint ink");

				// Prime direction/chevron state; the first truth frame is allowed
				// to establish both layers.
				gui.prPhaseFrame(1, 0.2, 1, 0);
				AppClock.sched(0.12, {
					try {
						var shellBefore = map.shellPaintCounts[0];
						var headBefore = map.readHeadPaintCounts[0];
						gui.prPhaseFrame(1, 0.38, 1, 0);
						AppClock.sched(0.12, {
							try {
								var stillShell = map.shellPaintCounts[0];
								var movedHead = map.readHeadPaintCounts[0];
								check.value(stillShell == shellBefore
									and: { movedHead > headBefore },
									"ordinary phase motion repaints only the cheap comet layer");
								gui.prPhaseFrame(1, 0.38, 1, 0);
								AppClock.sched(0.12, {
									try {
										var quietShell = map.shellPaintCounts[0];
										var quietHead = map.readHeadPaintCounts[0];
										check.value(quietShell == stillShell
											and: { quietHead == movedHead },
											"an unchanged phase frame costs no repaint");
										gui.prPhaseFrame(1, 0.48, -1, 0);
										AppClock.sched(0.12, {
											try {
												check.value(
													map.shellPaintCounts[0] > quietShell
													and: {
														map.readHeadPaintCounts[0] > quietHead
													},
													"a true direction-regime change updates chevron and comet");
												check.value(CQInkControl.drawErrors == 0,
													"isolated read heads paint without callback errors");
												gui.free;
												AppClock.sched(0.2, {
													if(ok) {
														"CQ_GUI_PHASE_LAYER_PASSED".postln;
														0.exit
													} {
														"CQ_GUI_PHASE_LAYER_FAILED".postln;
														1.exit
													};
													nil
												});
											} { |error| abort.value(error) };
											nil
										});
									} { |error| abort.value(error) };
									nil
								});
							} { |error| abort.value(error) };
							nil
						});
					} { |error| abort.value(error) };
					nil
				});
			} { |error| abort.value(error) };
			nil
		});
	} { |error| abort.value(error) };
}.defer;
)
