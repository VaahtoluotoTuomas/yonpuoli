// Serverless full-window release captures for the Drawer pages.
// Run only through tests/run_sclang_gate.ps1.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));
var outputDir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp";
var outputs = List.new;
var gui, capture, runSteps, steps;

capture = { |name|
	var path = outputDir +/+ ("release-drawer-" ++ name ++ ".png");
	var image = Image.fromWindow(gui.window);
	if(image.isNil) { Error("release capture returned no image").throw };
	image.write(path);
	image.free;
	outputs.add(path);
};

runSteps = { |remaining, done|
	if(remaining.isEmpty) { done.value } {
		AppClock.sched(remaining.first[0], {
			try {
				remaining.first[1].value;
				runSteps.value(remaining.drop(1), done);
			} { |error|
				error.reportError;
				gui !? { gui.free };
				"CQ_RELEASE_CAPTURE_FAILED".postln;
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
		steps = [
			[0.4, {
				gui.window.setInnerExtent(1600, 900);
				if(gui.shellState.drawerOpen.not) { gui.shell.toggleDrawer };
				gui.shell.selectDrawer(\inspector);
			}],
			[0.5, { capture.value("inspector") }],
			[0.1, { gui.shell.selectDrawer(\workshop) }],
			[0.35, { capture.value("workshop") }]
		];
		runSteps.value(steps, {
			gui.free;
			("CQ_RELEASE_CAPTURE: " ++ outputs.asArray.join(" | ")).postln;
			"CQ_RELEASE_CAPTURE_DONE".postln;
			0.exit;
		});
	} { |error|
		error.reportError;
		gui !? { gui.free };
		"CQ_RELEASE_CAPTURE_FAILED".postln;
		1.exit;
	};
}.defer;
)
