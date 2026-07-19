// G1.1 Qt smoke: construct the real one-window shell, exercise hard-cut view and
// click-only Drawer state, then close without leaving GUI listeners behind.
// Prints CQ_GUI_SHELL_PASSED / CQ_GUI_SHELL_FAILED as its final marker.

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
matrix.patch(\tri1, \coco1_spaf, 0.55);
matrix.patch(\envB, \castle3_fm, -0.45);
abort = { |error|
	error !? { error.reportError };
	("FAIL: GUI shell smoke aborted"
		++ (error !? { ": " ++ error.errorString } ? "")).postln;
	gui !? { gui.free };
	"CQ_GUI_SHELL_FAILED".postln;
	1.exit;
};

{
	try {
		gui = CQGui(state, matrix, config);
		AppClock.sched(0.25, {
			try {
				var master = gui.shell.masterView, volKnob, qKnob, trimKnob, oldVol;
				check.value(gui.window.notNil and: { gui.window.isClosed.not },
					"real Qt window constructed");
				check.value(gui.shellState.lifecycleState == \open,
					"shell lifecycle reached open");
				check.value(gui.shellState.minSize == [1280, 720],
					"minimum contract is 1280x720");
				check.value(gui.endpoints.size == 38
					and: { CQMatrix.sourceNames.every { |name| gui.endpoints[name].notNil } }
					and: { CQMatrix.destNames.every { |name| gui.endpoints[name].notNil } },
					"Day registers the canonical 16 jacks and 22 sockets");
				check.value(gui.endpoints[\castle5_spesal][\anchor] == Point(9, 10),
					"gate cable anchors on its glyph socket, not between socket and lamp");
				check.value(gui.cableOverlay.view.acceptsMouse.not,
					"cable overlay is hit-transparent to the controls below");
				check.value(gui.cableOverlay.cableCount == 2,
					"overlay renders every planted matrix cable");
				volKnob = gui.inkControls.detect { |control|
					control.kind == \knob and: { control.label == "vol" }
				};
				oldVol = state.get(\masterVol);
				check.value(volKnob.notNil, "Ink master-vol knob exists as a functional control");
				qKnob = gui.inkControls.detect { |control|
					control.kind == \knob and: { control.label == "q freq" }
				};
				trimKnob = gui.inkControls.detect { |control|
					control.kind == \knob and: { control.label == "dust" }
				};
				check.value(qKnob.notNil and: { trimKnob.notNil }
					and: { qKnob.diameter > trimKnob.diameter },
					"Day uses functional knob-size hierarchy, not uniform control weight");
				volKnob !? {
					volKnob.view.mouseDownAction.value(volKnob.view, 24, 30);
					volKnob.view.mouseMoveAction.value(volKnob.view, 24, 20);
					volKnob.view.mouseUpAction.value(volKnob.view, 24, 20);
				};
				check.value(state.get(\masterVol) != oldVol,
					"Ink knob gesture writes through the CQState funnel");
				gui.shell.toggleView(false);
				check.value(gui.shellState.viewMode == \night,
					"Day/Night hard cut reached Night");
				check.value(gui.shell.masterView === master,
					"the same Master view persists across the hard cut");
				gui.shell.toggleDrawer;
				// QView.visible_ crosses the Qt event queue; observe it on the
				// next GUI tick rather than reading the stale native property.
				AppClock.sched(0.05, {
					try {
						check.value(CQInkControl.drawErrors == 0,
							"Ink controls paint without callback errors");
						check.value(gui.shellState.drawerOpen
							and: { gui.shell.drawerView.visible },
							"click action opens Drawer above Master");
						gui.free;
						AppClock.sched(0.25, {
							try {
								check.value(gui.shellState.lifecycleState == \closed,
									"GUI free reaches closed");
								#[cq_gui_meters, cq_gui_gr, cq_gui_coco, cq_gui_castle,
									cq_gui_spesal, cq_gui_hot].do { |key|
									check.value(OSCdef.all[key].isNil,
										"cleanup removed %".format(key));
								};
								if(ok) { "CQ_GUI_SHELL_PASSED".postln; 0.exit } {
									"CQ_GUI_SHELL_FAILED".postln; 1.exit
								};
							} { |error| abort.value(error) };
							nil
						});
					} { |error| abort.value(error) };
					nil
				});
			} { |error| abort.value(error) };
			nil
		});
	} { |error|
		abort.value(error);
	};
}.defer;
)
