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
var n10HeartOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "n10-heart-hover.png";
var n10HeartRestOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "n10-heart-rest.png";
var n10ChevronRestOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "n10-chevron-rest.png";
var n10ChevronHoverOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "n10-chevron-hover.png";
var gui;

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
				var render, renderCrop;
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
				render = { |path|
					var image = Image(1280, 570);
					image.draw {
						Pen.fillColor = CQInk.palette(\night)[\ground];
						Pen.addRect(Rect(0, 0, 1280, 570));
						Pen.fill;
						paintView.value(map.groundView);
						map.rootViews.do(paintView);
						paintView.value(map.ringView);
						paintView.value(map.eyeView);
						map.castleViews.do(paintView);
						map.shellViews.do(paintView);
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
								gui.free;
								("CQ_NIGHT_CAPTURE: " ++ output ++ " | "
									++ cableOutput ++ " | " ++ couplingOutput
									++ " | " ++ targetOutput ++ " | "
									++ controlOutput ++ " | " ++ n10HeartOutput
									++ " | " ++ n10HeartRestOutput ++ " | "
									++ n10ChevronRestOutput ++ " | "
									++ n10ChevronHoverOutput).postln;
								"CQ_NIGHT_CAPTURE_DONE".postln;
								0.exit;
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
