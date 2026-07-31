// N9 Qt smoke: construct the real Night-only shell, verify its one instrument
// destination, persistent Master and Drawer stage, then close without leaving
// GUI listeners behind. Prints CQ_GUI_SHELL_PASSED / _FAILED as final marker.

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
		AppClock.sched(0.3, {
			try {
				var master = gui.shell.masterView;
				var volKnob, recControl, panicControl, oldVol;
				var map = gui.nightMap;
				var clockLayout, downLayout;
				check.value(gui.window.notNil and: { gui.window.isClosed.not },
					"real Qt window constructed");
				check.value(gui.window.acceptsMouseOver,
					"window enables real Qt mouse-over delivery");
				check.value(gui.shellState.lifecycleState == \open,
					"shell lifecycle reached open");
				check.value(gui.shellState.minSize == [1280, 720],
					"minimum contract is 1280x720");
				check.value(gui.shellState.viewMode == \night
					and: { gui.shell.nightView === map.stageView },
					"the shell opens directly on its one Night instrument");
				check.value(CQGuiShell.respondsTo(\toggleView).not
					and: { gui.respondsTo(\cableOverlay).not },
					"no Day destination or hidden Day cable layer remains");
				check.value(map.endpoints.size == 38
					and: { CQMatrix.sourceNames.every {
						|name| map.endpoints[name].notNil
					} }
					and: { CQMatrix.destNames.every {
						|name| map.endpoints[name].notNil
					} },
					"Night registers the canonical 16 jacks and 22 sockets");
				check.value(map.overlay.skin == \night
					and: { map.overlay.view.acceptsMouse.not }
					and: { map.overlay.cableCount == 2 },
					"one hit-transparent phosphor overlay renders the patch");
				volKnob = gui.inkControls.detect { |control|
					control.kind == \knob and: { control.label == "vol" }
				};
				oldVol = state.get(\masterVol);
				volKnob !? {
					volKnob.view.mouseDownAction.value(volKnob.view, 24, 30);
					volKnob.view.mouseMoveAction.value(volKnob.view, 24, 20);
					volKnob.view.mouseUpAction.value(volKnob.view, 24, 20);
				};
				check.value(volKnob.notNil
					and: { state.get(\masterVol) != oldVol },
					"persistent Master writes through the CQState funnel");
				recControl = gui.inkControls.detect { |control|
					control.kind == \button and: { control.label == "REC" }
				};
				panicControl = gui.inkControls.detect { |control|
					control.kind == \button and: { control.label == "PANIC" }
				};
				check.value(recControl.notNil and: { panicControl.notNil }
					and: { recControl.view.bounds.width >= 88 }
					and: { panicControl.view.bounds.width >= 112 },
					"REC and PANIC keep their deliberate hit areas");
				check.value(gui.shell.masterView === master
					and: { gui.shellState.masterVisible },
					"the same Master is persistent beside the instrument");
				check.value(gui.auraValues.isNil
					and: { gui.auraFor(\tri1).isNil }
					and: { gui.chargeFor(\coco1_flip).isNil },
					"missing telemetry never invents aura or charge");
				// AUD-W's always-visible surface. The mark is static ink drawn
				// on the toolbar beside the drawer button — the route to the
				// only remedy — and it may exist only in danger.
				check.value(gui.shell.audioClockDanger.not,
					"a healthy clock leaves no danger mark on the toolbar");
				gui.shell.audioClockDanger = true;
				check.value(gui.shell.audioClockDanger,
					"the guard's danger reaches the always-visible chrome");
				clockLayout = CQGuiShell.clockDangerLayout(34,
					gui.shell.drawerButton.view.bounds.left,
					0, CQGuiScale.chrome[\label]);
				check.value(clockLayout[\showText]
					and: { clockLayout[\textRect].left > 0 }
					and: { clockLayout[\textRect].right
						< gui.shell.drawerButton.view.bounds.left }
					and: { clockLayout[\dotCenter].x
						< gui.shell.drawerButton.view.bounds.left },
					"at the 1280 minimum the mark fits left of the drawer button");
				check.value(CQGuiShell.clockDangerLayout(34, 120, 100,
					CQGuiScale.chrome[\label])[\showText].not,
					"and where the title leaves no room the dot survives alone");
				// SOAK-1 finding B: the same mark, its own sentence. The longer
				// words must be measured, not assumed to fit where the clock's
				// shorter ones did.
				gui.shell.audioAlert = \serverDown;
				check.value(gui.shell.audioAlert == \serverDown
					and: { gui.shell.audioClockDanger.not },
					"a silent server lights the mark without claiming a stuck clock");
				downLayout = CQGuiShell.clockDangerLayout(34,
					gui.shell.drawerButton.view.bounds.left,
					0, CQGuiScale.chrome[\label], \serverDown);
				check.value(downLayout[\label] == "server not responding"
					and: { downLayout[\label] != clockLayout[\label] },
					"two faults, two sentences, one mark");
				check.value(downLayout[\showText]
					and: { downLayout[\textRect].left > 0 }
					and: { downLayout[\textRect].width > clockLayout[\textRect].width }
					and: { downLayout[\textRect].right
						< gui.shell.drawerButton.view.bounds.left }
					and: { downLayout[\dotCenter] == clockLayout[\dotCenter] },
					"the longer sentence still fits at 1280 and the dot never moves");
				gui.shell.audioAlert = \none;
				check.value(gui.shell.audioAlert == \none,
					"and every mark can be left, not only entered");
				gui.shell.toggleDrawer;
				AppClock.sched(0.15, {
					try {
						check.value(gui.shellState.drawerOpen
							and: { gui.shell.workspaceStack.index == 1 }
							and: { gui.shell.drawerButton.label == "drawer" },
							"Drawer replaces only the central Night stage");
						check.value(gui.shell.masterView === master
							and: { gui.shell.masterView.visible },
							"Drawer never covers or rebuilds Master");
						check.value(CQInkControl.drawErrors == 0,
							"Night, chrome and Drawer paint without callback errors");
						gui.free;
						AppClock.sched(0.25, {
							try {
								check.value(gui.shellState.lifecycleState == \closed,
									"GUI free reaches closed");
								#[cq_gui_meters, cq_gui_gr, cq_gui_coco,
									cq_gui_spesal, cq_gui_hot, cq_gui_aura,
									cq_gui_phase, cq_gui_charge,
									cq_gui_io_inputs].do { |key|
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
	} { |error| abort.value(error) };
}.defer;
)
