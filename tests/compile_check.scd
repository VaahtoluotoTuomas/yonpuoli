// Headless compile check (A1). If the class library fails to compile, sclang never
// executes this file and no marker is printed — a missing marker counts as failure.
// Run: "<sclang.exe>" -l tests/sclang_conf_dev.yaml -i none tests/compile_check.scd

(
var ok = true, count, defsClass;

count = try { CQSpec.specs.size } { |err| err.errorString.postln; -1 };
if(count != 79) {
	"CQSpec: expected 79 parameters, found %".format(count).postln;
	ok = false;
};

try { CQConfig.validate(CQConfig.defaults) } { |err|
	err.errorString.postln;
	ok = false;
};

// from phase 1 on: build every SynthDef to bytes (no server needed)
defsClass = \CQSynthDefs.asClass;
if(defsClass.notNil) {
	try { defsClass.perform(\checkAll) } { |err|
		err.errorString.postln;
		ok = false;
	};
};

if(ok) {
	"CQ_COMPILE_OK".postln;
	0.exit;
} {
	"CQ_COMPILE_FAILED".postln;
	1.exit;
};
)
