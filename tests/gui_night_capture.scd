// Serverless offscreen NIGHT-stage capture for layout review.
// Run only through tests/run_sclang_gate.ps1.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));
var output = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "night-layout.png";
var cableOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "night-layout-cables.png";
var couplingOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "night-layout-couplings.png";
var targetOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "night-layout-targets.png";
var controlOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "night-layout-control-edges.png";
var play3Output = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "night-layout-play3.png";
var n10HeartOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "n10-heart-hover.png";
var n10HeartRestOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "n10-heart-rest.png";
var n10ChevronRestOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "n10-chevron-rest.png";
var n10ChevronHoverOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "n10-chevron-hover.png";
var dustOpenOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "dust-open.png";
var repairOutput = { |size, stateName|
	Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
		+/+ ("slotx-dust-ui-" ++ size ++ "-" ++ stateName ++ ".png")
};
// PLAY-6.1's own review pair, at both reference stages: the internal streams
// closed (the landscape at rest) and open, with real patch cables above them,
// so the undertow can be judged against the bananas it has to differ from.
var play61 = { |name|
	Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ ("play61-" ++ name ++ ".png")
};
var play61Names = #[coco1Level, coco2Level, coco1Organ, coco2Organ,
	coco1SlotQ, coco2SlotQ, coco1SlotX, coco2SlotX,
	c1Send, c2Send, c3Send, c4Send, c5Send];
// Every value below is written through CQState and reaches the views on a
// DEFERRED tick, so each render has to wait one. Rendering in the same call
// photographs the PREVIOUS values — which is exactly how the first pass
// produced two identical Level shores. A flat step chain keeps that wait
// explicit instead of burying it in another layer of nesting.
var play61Steps, runSteps;
var gui;

runSteps = { |steps, done|
	if(steps.isEmpty) { done.value } {
		AppClock.sched(steps.first[0], {
			try {
				steps.first[1].value;
				runSteps.value(steps.drop(1), done);
			} { |error|
				error.reportError;
				gui !? { gui.free };
				"CQ_NIGHT_CAPTURE_FAILED".postln;
				1.exit;
			};
			nil
		});
	};
};

matrix.preset(\classic, origin: \code, committed: true, operation: \preset);
matrix.patch(\envA, \deckA_rate, -0.42);
matrix.patch(\cas4, \coco2_fbVca, 0.37);

{
	try {
		gui = CQGui(state, matrix, config);
		AppClock.sched(1.0, {
			try {
				var map = gui.nightMap;
				var paintView;
				var render, renderCrop, repairOutputs, renderRepair;
				var setDustReview, setBiasReview, setDustTelemetry;
				var ringPoint, ringLocal;
				paintView = { |view|
					var bounds = view.bounds;
					Pen.use {
						Pen.translate(bounds.left, bounds.top);
						if(view.background.alpha > 0) {
							Pen.fillColor = view.background;
							Pen.addRect(Rect(0, 0, bounds.width, bounds.height));
							Pen.fill;
						};
						view.drawFunc.value(view);
					};
				};
				// The stage's own size, read from the geometry it laid out, so a
				// roomy fixture renders roomy paper instead of cropping the very
				// landscape it was asked to show.
				render = { |path|
					var w = map.geometry[\width].round.asInteger;
					var h = map.geometry[\height].round.asInteger;
					var image = Image(w, h);
					image.draw {
						Pen.fillColor = CQInk.palette(\night)[\ground];
						Pen.addRect(Rect(0, 0, w, h));
						Pen.fill;
						paintView.value(map.groundView);
						map.rootViews.do(paintView);
						paintView.value(map.ringView);
						paintView.value(map.eyeView);
						map.castleViews.do(paintView);
						map.shellViews.do(paintView);
						map.dustViews.do(paintView);
						map.readHeadViews.do(paintView);
						map.birdViews.do(paintView);
						paintView.value(map.almanacView);
						map.endpoints.values.do { |endpoint|
							paintView.value(endpoint[\view])
						};
						map.crescentViews.values.do(paintView);
						paintView.value(map.routeView);
						paintView.value(map.overlay.view);
						paintView.value(map.tideGestureView);
					};
					image.write(path);
					image.free;
				};
				renderCrop = { |path, crop, scale = 3|
					var image = Image(
						(crop.width * scale).round.asInteger,
						(crop.height * scale).round.asInteger);
					image.draw {
						Pen.fillColor = CQInk.palette(\night)[\ground];
						Pen.addRect(Rect(0, 0, image.width, image.height));
						Pen.fill;
						Pen.scale(scale, scale);
						Pen.translate(crop.left.neg, crop.top.neg);
						paintView.value(map.groundView);
						map.rootViews.do(paintView);
						paintView.value(map.ringView);
						paintView.value(map.eyeView);
						map.castleViews.do(paintView);
						map.shellViews.do(paintView);
						map.dustViews.do(paintView);
						map.readHeadViews.do(paintView);
						map.birdViews.do(paintView);
						paintView.value(map.almanacView);
						map.endpoints.values.do { |endpoint|
							paintView.value(endpoint[\view])
						};
						map.crescentViews.values.do(paintView);
						paintView.value(map.routeView);
						paintView.value(map.overlay.view);
						paintView.value(map.tideGestureView);
					};
					image.write(path);
					image.free;
				};
				repairOutputs = List.new;
				renderRepair = { |size, stateName|
					var path = repairOutput.value(size, stateName);
					render.value(path);
					repairOutputs.add(path);
				};
				setDustReview = { |amount, grain|
					2.do { |i|
						var amountName = CQGuiNightMap.dustParamName(i);
						var grainName = CQGuiNightMap.dustParamName(i, \DustGrain);
						state.set(amountName, CQSpec.map(amountName, amount));
						state.set(grainName, CQSpec.map(grainName, grain));
					};
				};
				setBiasReview = { |norm|
					#[preABias, preBBias].do { |name|
						state.set(name, CQSpec.map(name, norm))
					};
				};
				setDustTelemetry = { |active = true|
					var activity = if(active) { 0.72 } { 0 };
					gui.prPhaseFrame(1, 0.43, nil, nil,
						[0, 0, 0, 0, 0, 0, 0.36],
						[0, 0, 0, 0, 0, 0, 0.11],
						[0, 0, 0, 0, 0, 0, activity]);
					gui.prPhaseFrame(2, 0.57, nil, nil,
						[0, 0, 0, 0, 0, 0, 0.28],
						[0, 0, 0, 0, 0, 0, 0.08],
						[0, 0, 0, 0, 0, 0, activity]);
				};
				map.overlay.backgrounded_(false);
				map.overlay.invalidate;
				map.overlay.auraTick;
				render.value(cableOutput);
				map.overlay.backgrounded_(true);
				map.overlay.invalidate;
				map.overlay.auraTick;
				render.value(output);
				// Targeted polish fixtures: the complete coupling constellation
				// must cross the eye layer without square bites, and a banana in
				// hand must reveal every legal socket without ambient blinking.
				ringPoint = map.geometry[\ringSamples].detect { |point|
					map.overlay.patchAt(point).isNil
				} ?? { map.geometry[\ringSamples].first };
				ringLocal = ringPoint - map.ringView.bounds.leftTop;
				map.ringView.mouseOverAction.value(map.ringView,
					ringLocal.x, ringLocal.y);
				render.value(couplingOutput);
				map.ringView.mouseLeaveAction.value(map.ringView);
				gui.bananaGrammar.startSource(\tri1);
				render.value(targetOutput);
				gui.bananaGrammar.releaseVoid;
				// PLAY-3 fixture: two resolvable raw-triangle pearls followed by
				// three audio-rate stable reeds, plus charge-only and exact-fire lamps.
				map.castleFrame([-0.75, 0.4, -0.9, 0.2, 0.8],
					[0.8, 2.5, 3.1, 40, 400]);
				gui.prChargeFrame(1, [0.82, 0.34, 0.68, 1, 0, 1]);
				gui.prChargeFrame(2, [0.25, 0.72, 0.18, 0, 1, 0]);
				gui.prChargeFrame(3,
					[0.15, 0.42, 0.66, 0.91, 0.28, 0, 0, 1, 0, 1]);
				render.value(play3Output);
				state.set(\coco1Speed, 48000);
				state.set(\deckALevel, 2);
				AppClock.sched(0.15, {
					try {
						var coco = map.geometry[\cocos][0];
						var shell = map.shellViews[0];
						var shellOrigin = coco[\shellBox].leftTop;
						var heartPoint, crop;
						render.value(controlOutput);
						// N10 visual-review fixtures. Clear the bay so cable-first
						// priority cannot hide either cue, then place the slowest
						// reel pearl inside the heart's real 22 px hit disc.
						matrix.preset(\empty, origin: \test,
							committed: false, operation: \n10Capture);
						map.overlay.invalidate;
						state.set(\coco1Speed, 1500);
						state.set(\coco1Hold, 0);
						AppClock.sched(0.15, {
							try {
								heartPoint = CQNightGeometry.spiralPoint(coco, 0);
								crop = Rect.aboutPoint(coco[\center], 58, 58);
								renderCrop.value(n10HeartRestOutput, crop);
								shell.mouseOverAction.value(shell,
									heartPoint.x - shellOrigin.x,
									heartPoint.y - shellOrigin.y);
								renderCrop.value(n10HeartOutput, crop);
								shell.mouseLeaveAction.value(shell);
								crop = Rect.aboutPoint(coco[\chevron], 48, 38);
								renderCrop.value(n10ChevronRestOutput, crop);
								shell.mouseOverAction.value(shell,
									coco[\chevron].x - shellOrigin.x,
									coco[\chevron].y - shellOrigin.y);
								renderCrop.value(n10ChevronHoverOutput, crop);
								shell.mouseLeaveAction.value(shell);
								// ---- PLAY-6.1's own review pair ----
								// Closed first: at zero every internal stream is
								// only a hairline possibility, and both Level
								// shores must still read whole across the opaque
								// crescent papers they now cross. Then open, with
								// the classic patch back on top, so the undertow
								// is judged against real bananas, not empty sky.
								play61Steps = [
									[0.15, {
										render.value(play61.value("min-zero"));
										matrix.preset(\classic, origin: \test,
											committed: false,
											operation: \play61Capture);
										matrix.patch(\envA, \deckA_rate, -0.42);
										matrix.patch(\cas4, \coco2_fbVca, 0.37);
										map.overlay.invalidate;
										play61Names.do { |name|
											state.set(name, CQSpec.map(name, 0.7))
										};
									}],
									[0.15, {
										render.value(play61.value("min-open"));
										gui.window.setInnerExtent(1920, 1005);
									}],
									[0.4, {
										render.value(play61.value("roomy-open"));
										play61Names.do { |name|
											state.set(name, 0) };
										matrix.preset(\empty, origin: \test,
											committed: false,
											operation: \play61Capture);
										map.overlay.invalidate;
									}],
					[0.15, {
						render.value(play61.value("roomy-zero"));
						state.set(\coco1Dust, CQSpec.map(\coco1Dust, 0.72));
						state.set(\coco2Dust, CQSpec.map(\coco2Dust, 0.72));
						state.set(\coco1DustGrain,
							CQSpec.map(\coco1DustGrain, 0.12));
						state.set(\coco2DustGrain,
							CQSpec.map(\coco2DustGrain, 0.88));
					}],
					[0.15, {
						render.value(dustOpenOutput);
						setDustTelemetry.value(false);
						map.tideFocusLatched_(false);
						matrix.preset(\empty, origin: \test,
							committed: false, operation: \dustUiCapture);
						map.overlay.invalidate;
						setDustReview.value(0, 0.5);
						setBiasReview.value(0.5);
						gui.window.setInnerExtent(1280, 720);
					}],
					[0.4, {
						renderRepair.value("compact", "rest");
						setDustReview.value(1, 0);
					}],
					[0.15, {
						renderRepair.value("compact", "high-mist");
						setDustReview.value(1, 1);
					}],
					[0.15, {
						renderRepair.value("compact", "high-chip");
						setDustReview.value(0.78, 0.5);
						matrix.patch(\cas1, \coco1_dust, 0.65);
						matrix.patch(\tri4, \coco2_dust, -0.58);
						setDustTelemetry.value(true);
						map.overlay.invalidate;
					}],
					[0.15, {
						renderRepair.value("compact", "telemetry-cables");
						matrix.preset(\empty, origin: \test,
							committed: false, operation: \dustUiCapture);
						setDustTelemetry.value(false);
						map.overlay.invalidate;
						setDustReview.value(0, 0.5);
						setBiasReview.value(0);
					}],
					[0.15, {
						renderRepair.value("compact", "root-bias-0");
						setBiasReview.value(1);
					}],
					[0.15, {
						renderRepair.value("compact", "root-bias-1");
						setBiasReview.value(0.5);
						map.tideFocusLatched_(false);
					}],
					[0.15, {
						renderRepair.value("compact", "tide-focus-closed");
						map.tideFocusLatched_(true);
					}],
					[0.15, {
						renderRepair.value("compact", "tide-focus-open");
						map.tideFocusLatched_(false);
						setDustReview.value(0, 0.5);
						gui.window.setInnerExtent(1920, 1005);
					}],
					[0.4, {
						renderRepair.value("roomy", "rest");
						setDustReview.value(1, 0);
					}],
					[0.15, {
						renderRepair.value("roomy", "high-mist");
						setDustReview.value(1, 1);
					}],
					[0.15, {
						renderRepair.value("roomy", "high-chip");
						setDustReview.value(0.78, 0.5);
						matrix.patch(\cas1, \coco1_dust, 0.65);
						matrix.patch(\tri4, \coco2_dust, -0.58);
						setDustTelemetry.value(true);
						map.overlay.invalidate;
					}],
					[0.15, {
						renderRepair.value("roomy", "telemetry-cables");
						matrix.preset(\empty, origin: \test,
							committed: false, operation: \dustUiCapture);
						setDustTelemetry.value(false);
						map.overlay.invalidate;
						setDustReview.value(0, 0.5);
						setBiasReview.value(0);
					}],
					[0.15, {
						renderRepair.value("roomy", "root-bias-0");
						setBiasReview.value(1);
					}],
					[0.15, {
						renderRepair.value("roomy", "root-bias-1");
						setBiasReview.value(0.5);
						map.tideFocusLatched_(false);
					}],
					[0.15, {
						renderRepair.value("roomy", "tide-focus-closed");
						map.tideFocusLatched_(true);
					}],
					[0.15, {
						renderRepair.value("roomy", "tide-focus-open");
						map.tideFocusLatched_(false);
					}]
								];
								play61Names.do { |name| state.set(name, 0) };
								runSteps.value(play61Steps, {
									gui.free;
									("CQ_NIGHT_CAPTURE: " ++ output ++ " | "
										++ cableOutput ++ " | " ++ couplingOutput
										++ " | " ++ targetOutput ++ " | "
										++ controlOutput ++ " | " ++ play3Output
										++ " | " ++ n10HeartOutput
										++ " | " ++ n10HeartRestOutput ++ " | "
										++ n10ChevronRestOutput ++ " | "
									++ n10ChevronHoverOutput ++ " | "
									++ dustOpenOutput ++ " | "
									++ play61.value("min-zero") ++ " | "
									++ play61.value("min-open") ++ " | "
									++ play61.value("roomy-open") ++ " | "
									++ play61.value("roomy-zero") ++ " | "
									++ repairOutputs.asArray.join(" | ")).postln;
									"CQ_NIGHT_CAPTURE_DONE".postln;
									0.exit;
								});
							} { |error|
								error.reportError;
								gui !? { gui.free };
								"CQ_NIGHT_CAPTURE_FAILED".postln;
								1.exit;
							};
							nil
						});
					} { |error|
						error.reportError;
						gui !? { gui.free };
						"CQ_NIGHT_CAPTURE_FAILED".postln;
						1.exit;
					};
					nil
				});
			} { |error|
				error.reportError;
				gui !? { gui.free };
				"CQ_NIGHT_CAPTURE_FAILED".postln;
				1.exit;
			};
			nil
		});
	} { |error|
		error.reportError;
		gui !? { gui.free };
		"CQ_NIGHT_CAPTURE_FAILED".postln;
		1.exit;
	};
}.defer;
)
