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
				var render;
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
						render.value(controlOutput);
						gui.free;
						("CQ_NIGHT_CAPTURE: " ++ output ++ " · " ++ cableOutput
							++ " · " ++ couplingOutput ++ " · " ++ targetOutput
							++ " · " ++ controlOutput).postln;
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
}.defer;
)
