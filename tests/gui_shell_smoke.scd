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
				var master = gui.shell.masterView, volKnob, qKnob, speedKnob, trimKnob;
				var recControl, panicControl, sliders, ranges, oldVol;
				var sourceView, destView, destPoint, sourceOrigin, dragPoint, cableHit;
				var groundView, harvestRecord, harvestCenter, harvestPoints, localPoints;
				var beforeHarvest, afterHarvest, hoverPoint, fileLabel, loopControl;
				var trackedView, trackedControl;
				check.value(gui.window.notNil and: { gui.window.isClosed.not },
					"real Qt window constructed");
				check.value(gui.window.acceptsMouseOver,
					"window enables real Qt mouse-over delivery to child cable hit tests");
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
				check.value(gui.bananaController.notNil
					and: { gui.bananaBodies.size == 4 }
					and: { gui.endpoints.values.every { |endpoint|
						endpoint[\view].mouseDownAction.notNil
					} },
					"banana controller binds every endpoint and four organ bodies");
				cableHit = gui.cableOverlay.cableRecords.first;
				cableHit = gui.cableOverlay.patchAt(cableHit[\points]
					[cableHit[\points].size div: 2]);
				check.value(cableHit.notNil and: { cableHit[\src] == \tri1 }
					and: { cableHit[\dest] == \coco1_spaf },
					"drawn cable path exposes its narrow semantic hit target");
				groundView = gui.bananaBodies[\sources][\view];
				trackedControl = gui.inkControls.detect { |control| control.isInteractive };
				trackedView = trackedControl.view;
				check.value(trackedView.mouseOverAction.notNil and: {
					trackedView.getProperty(\mouseTracking) == true
				}, "a Day child control passes real Qt hover moves to cable hit-testing");
				trackedView.mouseEnterAction.value(trackedView);
				check.value(trackedControl.hover,
					"interactive Ink control enters its immediate visual hover state");
				gui.bananaGrammar.hoverCable = cableHit;
				hoverPoint = gui.cableOverlay.view.mapToGlobal(cableHit[\points]
					[cableHit[\points].size div: 2]) - trackedView.mapToGlobal(0@0);
				trackedView.mouseOverAction.value(trackedView, hoverPoint.x, hoverPoint.y);
				check.value(gui.bananaGrammar.hoverCable.isNil,
					"controls-first priority suppresses a cable target over a control");
				trackedView.mouseLeaveAction.value(trackedView);
				check.value(trackedControl.hover.not,
					"interactive Ink control clears its hover state on leave");
				hoverPoint = gui.cableOverlay.view.mapToGlobal(cableHit[\points]
					[cableHit[\points].size div: 2]) - groundView.mapToGlobal(0@0);
				groundView.mouseOverAction.value(groundView, hoverPoint.x, hoverPoint.y);
				check.value(gui.bananaGrammar.isCableHovered(\tri1, \coco1_spaf),
					"open-ground hover exposes one precise cable target");
				gui.bananaGrammar.hoverCable = nil;
				sourceView = gui.endpoints[\org2][\view];
				destView = gui.endpoints[\deckB_rate][\view];
				destPoint = destView.mapToGlobal(gui.endpoints[\deckB_rate][\anchor]);
				sourceOrigin = sourceView.mapToGlobal(0@0);
				dragPoint = destPoint - sourceOrigin;
				sourceView.mouseDownAction.value(sourceView, 9, 10, 0, 0, 1);
				sourceView.mouseMoveAction.value(sourceView, dragPoint.x, dragPoint.y, 0);
				sourceView.mouseUpAction.value(sourceView, dragPoint.x, dragPoint.y, 0, 0);
				check.value(matrix.getPatch(\org2, \deckB_rate) == 0.5,
					"real endpoint drag auditions and plants WYHIWYG through the funnel");
				gui.window.view.keyDownAction.value(gui.window.view, $z, 262144);
				check.value(matrix.getPatch(\org2, \deckB_rate) == 0.0,
					"window-level Ctrl+Z restores the endpoint plant");
				harvestRecord = gui.cableOverlay.cableRecords.first;
				harvestCenter = harvestRecord[\points]
					[harvestRecord[\points].size div: 2];
				harvestPoints = [harvestCenter + (-20@(-20)),
					harvestCenter + (20@(-20)), harvestCenter + (20@20),
					harvestCenter + (-20@20)];
				localPoints = harvestPoints.collect { |point|
					gui.cableOverlay.view.mapToGlobal(point) - groundView.mapToGlobal(0@0)
				};
				beforeHarvest = matrix.fullMatrix.asCompileString;
				groundView.mouseDownAction.value(groundView,
					localPoints[0].x, localPoints[0].y, 0, 1, 1);
				groundView.mouseMoveAction.value(groundView,
					localPoints[1].x, localPoints[1].y, 0);
				groundView.mouseMoveAction.value(groundView,
					localPoints[2].x, localPoints[2].y, 0);
				groundView.mouseUpAction.value(groundView,
					localPoints[3].x, localPoints[3].y, 0, 1);
				afterHarvest = matrix.fullMatrix.asCompileString;
				check.value(afterHarvest != beforeHarvest,
					"real ground right-drag harvest removes its caught cable");
				gui.window.view.keyDownAction.value(gui.window.view,
					nil, 262144, 26, 0, 90);
				check.value(matrix.fullMatrix.asCompileString == beforeHarvest,
					"native Windows Ctrl+Z restores the complete harvest batch");
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
				speedKnob = gui.inkControls.detect { |control|
					control.kind == \knob and: { control.label == "speed" }
				};
				check.value(qKnob.notNil and: { speedKnob.notNil }
					and: { trimKnob.notNil }
					and: { [trimKnob.diameter, speedKnob.diameter, qKnob.diameter]
						== [26, 34, 44] },
					"Day renders the experimental 26/34/44 knob hierarchy");
				recControl = gui.inkControls.detect { |control|
					control.kind == \button and: { control.label == "REC" }
				};
				panicControl = gui.inkControls.detect { |control|
					control.kind == \button and: { control.label == "PANIC" }
				};
				check.value(recControl.notNil and: { panicControl.notNil }
					and: { recControl.view.bounds.width >= 88 }
					and: { recControl.view.bounds.height >= 34 }
					and: { panicControl.view.bounds.width >= 112 }
					and: { panicControl.view.bounds.height >= 34 },
					"REC and PANIC expose deliberate large hit areas");
				sliders = gui.inkControls.select { |control| control.kind == \slider };
				ranges = gui.inkControls.select { |control|
					control.kind == \switch and: { control.label == "range" }
				};
				check.value(sliders.notEmpty and: { ranges.notEmpty }
					and: { (sliders[0].view.absoluteBounds.center.y
						- (ranges[0].view.absoluteBounds.top + 22)).abs <= 1 },
					"Quantussy castle 1 range rail aligns with the Coco 1 slot rail");
				ranges[0].view.mouseDownAction.value(ranges[0].view, 1, 18);
				ranges[0].view.mouseMoveAction.value(ranges[0].view,
					ranges[0].view.bounds.width - 1, 18);
				ranges[0].view.mouseUpAction.value(ranges[0].view,
					ranges[0].view.bounds.width - 1, 18);
				check.value(state.get(\c1Range) == \audio,
					"range switch supports click-drag selection across its rail");
				fileLabel = gui.deckLabel(\deckA);
				loopControl = gui.inkControls.detect { |control|
					control.kind == \button and: { control.label == "loop" }
				};
				check.value(fileLabel.bounds.width >= 144
					and: { fileLabel.bounds.height >= 36 }
					and: { (fileLabel.absoluteBounds.left
						- loopControl.view.absoluteBounds.right) >= 6 },
					"deck filename has full glyph height and breathing room after loop");
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
				NetAddr.localAddr.sendMsg('/cq_spesal', 0, 0, 0, 1, 4, 6, 7);
				NetAddr.localAddr.sendMsg('/cq_castle', 0, 0,
					-0.8, -0.25, 0.35, 0.9, 0.1);
				gui.shell.toggleDrawer;
				// QView.visible_ crosses the Qt event queue; observe it on the
				// next GUI tick rather than reading the stale native property.
				AppClock.sched(0.05, {
					try {
						check.value(gui.spesalStates == [0, 1, 4, 6, 7],
							"spesal OSC drives the constellation partner and mode state");
						check.value(gui.castleValues.collect { |value, i|
							(value - [-0.8, -0.25, 0.35, 0.9, 0.1][i]).abs < 0.001
						}.every { |value| value },
							"castle OSC drives the five constellation activity marks");
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
