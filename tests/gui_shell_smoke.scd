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
				var master = gui.shell.masterView;
				check.value(gui.window.notNil and: { gui.window.isClosed.not },
					"real Qt window constructed");
				check.value(gui.shellState.lifecycleState == \open,
					"shell lifecycle reached open");
				check.value(gui.shellState.minSize == [1280, 720],
					"minimum contract is 1280x720");
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
