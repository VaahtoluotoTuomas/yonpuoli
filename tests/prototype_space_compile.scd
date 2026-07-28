// Builds the four disposable Space Lab SynthDefs to bytes without a server.
// This catches sclang/UGen graph errors while keeping the prototypes outside
// the production class library.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var path = repo +/+ "prototypes" +/+ "space_lab_defs.scd";
var harnessPath = repo +/+ "prototypes" +/+ "space_lab.scd";
var expected = #[
	cqProtoSpaceUtility,
	cqProtoSpaceClockField,
	cqProtoSpaceMemoryHalo,
	cqProtoSpaceConfluence
];
var defs, probe, probeResult, ok = true;

defs = try {
	thisProcess.interpreter.executeFile(path)
} { |err|
	err.errorString.postln;
	ok = false;
	[]
};

if(defs.isArray.not or: { defs.size != 4 }) {
	"Space Lab: expected four SynthDefs, got %".format(defs).postln;
	ok = false;
} {
	defs.do { |def, i|
		if(def.isKindOf(SynthDef).not) {
			"Space Lab: item % is not a SynthDef".format(i).postln;
			ok = false;
		} {
			if(def.name != expected[i]) {
				"Space Lab: def % should be %, got %"
					.format(i, expected[i], def.name).postln;
				ok = false;
			};
			try { def.asBytes } { |err|
				("%: %".format(def.name, err.errorString)).postln;
				ok = false;
			};
		};
	};
};

// CQ is intentionally down in this headless check. Executing the harness still
// parses the complete script and takes its explicit "boot first" no-op branch.
try {
	thisProcess.interpreter.executeFile(harnessPath)
} { |err|
	("Space Lab harness: " ++ err.errorString).postln;
	ok = false;
};

// The listening harness must be a parentless controller, not an Event: an
// absent \synth key on Event falls through to the default event's synth
// Function and makes the first free attempt treat a Function as a node.
probe = IdentityDictionary.new(8, nil, nil, true).putAll((
	playVariant: { |self, which| [self, which] }
));
probeResult = try { probe.playVariant(\clockField) } { |err|
	err.errorString.postln;
	ok = false;
	nil
};
if(probeResult.isNil or: {
	probeResult[0] !== probe or: { probeResult[1] != \clockField }
}) {
	"Space Lab: Event controller dispatch does not pass self/arguments".postln;
	ok = false;
};
if(probe.synth.notNil) {
	"Space Lab: an absent synth field must stay nil (no Event fallback)".postln;
	ok = false;
};

if(ok) {
	"CQ_SPACE_LAB_COMPILE_OK".postln;
	0.exit;
} {
	"CQ_SPACE_LAB_COMPILE_FAILED".postln;
	1.exit;
};
)
