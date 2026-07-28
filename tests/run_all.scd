// Headless UnitTest runner (A2). Runs every CQ*Test class in the library
// (they live in tests/classes/, on the include path via sclang_conf_dev.yaml).
// Prints CQ_TESTS_PASSED / CQ_TESTS_FAILED as its last line.
// Run only through tests/run_sclang_gate.ps1; direct sclang may strand a shell
// after a class-library parse error (AGENTS.md).

(
var runner;
UnitTest.reportPasses = false;   // failures still print in full

runner = {
	// `completed` and `aborted` exist because the marker is the gate (CLAUDE.md)
	// and it once lied: an uncaught error inside a test class printed
	// CQ_TESTS_FAILED and then the run carried on to a summary of zero failures
	// and CQ_TESTS_PASSED, because sclang's `try` does not survive the routine
	// yielding at `0.1.wait`. A run that did not finish every class is a failed
	// run whatever its failure count says.
	var classes, nFail, completed = 0, aborted = false;
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
			completed = completed + 1;
			0.1.wait;
		};
	} { |err|
		aborted = true;
		"Uncaught error during tests: %".format(err.errorString).postln;
	};
	nFail = UnitTest.failures.size;
	"CQ test summary: % passes, % failures, %/% classes completed"
		.format(UnitTest.passes.size, nFail, completed, classes.size).postln;
	if(nFail == 0 and: { aborted.not } and: { completed == classes.size }) {
		"CQ_TESTS_PASSED".postln;
		0.exit;
	} {
		if(completed != classes.size) {
			"Run stopped after % of % test classes — the remaining classes never ran."
				.format(completed, classes.size).postln;
		};
		"CQ_TESTS_FAILED".postln;
		1.exit;
	};
};
runner.fork(AppClock);
)
