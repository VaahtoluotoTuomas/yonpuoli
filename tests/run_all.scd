// Headless UnitTest runner (A2). Runs every CQ*Test class in the library
// (they live in tests/classes/, on the include path via sclang_conf_dev.yaml).
// Prints CQ_TESTS_PASSED / CQ_TESTS_FAILED as its last line.
// Run: "<sclang.exe>" -l tests/sclang_conf_dev.yaml -i none tests/run_all.scd

(
var runner;
UnitTest.reportPasses = false;   // failures still print in full

runner = {
	var classes, nFail;
	classes = UnitTest.allSubclasses
		.select { |c| c.name.asString.beginsWith("CQ") }
		.sort { |a, b| a.name.asString <= b.name.asString };
	if(classes.isEmpty) {
		"No CQ*Test classes found — check includePaths (sclang_conf_dev.yaml).".postln;
		"CQ_TESTS_FAILED".postln;
		1.exit;
	};
	try {
		UnitTest.reset;
		classes.do { |c|
			c.run(reset: false, report: true);
			0.1.wait;
		};
	} { |err|
		"Uncaught error during tests: %".format(err.errorString).postln;
		"CQ_TESTS_FAILED".postln;
		1.exit;
	};
	nFail = UnitTest.failures.size;
	"CQ test summary: % passes, % failures"
		.format(UnitTest.passes.size, nFail).postln;
	if(nFail == 0) {
		"CQ_TESTS_PASSED".postln;
		0.exit;
	} {
		"CQ_TESTS_FAILED".postln;
		1.exit;
	};
};
runner.fork(AppClock);
)
