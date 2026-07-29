// AIO-4 Qt smoke: the raw-input/post-safety-master caches remain live while
// Audio is hidden, but only the visible Audio page receives a paced repaint
// stream. Free removes both its listener and the namespaced OSC receiver.
// Run only through tests/run_sclang_gate.ps1.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));
var gui, ok = true;
var check = { |condition, message|
	if(condition) { ("ok:   " ++ message).postln } {
		ok = false;
		("FAIL: " ++ message).postln;
	};
};
var abort = { |error|
	error !? { error.reportError };
	gui !? { gui.free };
	"CQ_GUI_AUDIO_PRESENCE_FAILED".postln;
	1.exit;
};

{
	try {
		gui = CQGui(state, matrix, config);
		AppClock.sched(0.35, {
			try {
				var page = gui.drawer.audioSetup;
				var hiddenFrames = page.presenceFrameCount;
				var hiddenPaints = page.presencePaintCount;
				check.value(gui.audioPresenceListenerCount == 1
					and: { OSCdef.all[\cq_gui_io_inputs].notNil },
					"one Audio owner listens through one namespaced input OSCdef");

				24.do { |index|
					var hot = index.even;
					gui.audioInputPresenceFrame(
						if(hot) { [0.2, 0.04, 0.01] } {
							[0.0001, 0.0002, 0.0003]
						});
					gui.masterPresenceFrame(
						if(hot) { [0.15, 0.03] } { [0.0001, 0.0002] });
				};
				AppClock.sched(0.12, {
					try {
						check.value(gui.ioInputPresence.size == 3
							and: { gui.masterPresence.size == 2 }
							and: { page.presenceFrameCount == hiddenFrames }
							and: { page.presencePaintCount == hiddenPaints },
							"hidden Audio caches every source without a page callback or paint");

						gui.shell.selectDrawer(\audio);
						gui.shell.toggleDrawer;
						AppClock.sched(0.2, {
							Routine({
								try {
									var notifyBefore, paintBefore, refreshBefore;
									var notifyDelta, paintDelta, refreshDelta;
									var hiddenFrameGate, hiddenPaintGate;
									check.value(gui.audioPageVisible
										and: { page.presenceFrameCount > hiddenFrames },
										"opening Audio primes it from the latest hidden cache");
									check.value(page.inputRows[3][\value]
										.find("quiet").notNil
										and: { page.masterPresenceText.find("quiet").notNil },
										"the primed cache reaches the input rows and master title");

									notifyBefore = gui.audioPresenceNotifyCount;
									paintBefore = page.presencePaintCount;
									refreshBefore = page.presenceRefreshCount;
									30.do { |index|
										var hot = index.even;
										gui.audioInputPresenceFrame(
											if(hot) { [0.35, 0.08, 0] } {
												[0.0001, 0.0002, 0]
											});
										gui.masterPresenceFrame(
											if(hot) { [0.22, 0.06] } {
												[0.0001, 0.0002]
											});
										0.025.wait;
									};
									0.2.wait;
									notifyDelta =
										gui.audioPresenceNotifyCount - notifyBefore;
									paintDelta = page.presencePaintCount - paintBefore;
									refreshDelta =
										page.presenceRefreshCount - refreshBefore;
									check.value(notifyDelta > 0 and: { notifyDelta <= 15 },
										"combined 40 Hz injections notify the visible page at <=15 Hz");
									check.value(paintDelta > 0
										and: { paintDelta <= notifyDelta }
										and: { refreshDelta <= notifyDelta },
										"Qt presence paint batches never outrun paced notifications");

									gui.shell.selectDrawer(\workshop);
									0.1.wait;
									hiddenFrameGate = page.presenceFrameCount;
									hiddenPaintGate = page.presencePaintCount;
									20.do { |index|
										gui.audioInputPresenceFrame(
											if(index.even) { [0.4, 0.3, 0.2] } {
												[0, 0, 0]
											});
										gui.masterPresenceFrame(
											if(index.even) { [0.3, 0.2] } { [0, 0] });
									};
									0.15.wait;
									check.value(page.presenceFrameCount
										== hiddenFrameGate
										and: { page.presencePaintCount
											== hiddenPaintGate },
										"switching away stops Audio callbacks and paints immediately");

									gui.free;
									0.25.wait;
									check.value(gui.audioPresenceListenerCount == 0
										and: { OSCdef.all[\cq_gui_io_inputs].isNil }
										and: { OSCdef.all[\cq_gui_meters].isNil },
										"free removes the page listener and both telemetry OSCdefs");
									if(ok) {
										"CQ_GUI_AUDIO_PRESENCE_PASSED".postln;
										0.exit;
									} {
										"CQ_GUI_AUDIO_PRESENCE_FAILED".postln;
										1.exit;
									};
								} { |error| abort.value(error) };
							}).play(AppClock);
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
