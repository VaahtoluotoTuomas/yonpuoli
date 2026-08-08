// Deterministically rebuild the four curated release scenes. Run only through
// tests/run_sclang_gate.ps1. Personal snapshots are never enumerated, read,
// removed or overwritten; the four exact names below are the only write targets.

(
var repo = File.realpath(thisProcess.nowExecutingPath).dirname.dirname;
var out = repo +/+ "snapshots";
var common = (
	masterVol: -18, masterDry: 0, micGain: 0,
	coco1SlotPre: 0, coco2SlotPre: 0,
	coco1SlotDeck: 0, coco2SlotDeck: 0
);
var scenes = [
	(
		name: \undertow, preset: \crossCurrent,
		params: (
			masterVol: -12, coco1Pan: -0.55, coco2Pan: 0.55,
			qChaos: 0.40, coco1DustGrain: 0.32, coco2DustGrain: 0.72
		),
		matrix: [
			[\org1, \coco2_slotX, 0.16], [\org2, \coco1_slotX, -0.14]
		],
		spesal: [1, 3, 5, 7, 2],
		note: "Held cross-currents: unequal clocks exchange short organ and Quantussy phrases."
	),
	(
		name: \dust_bloom, preset: \dustGarden,
		params: (
			masterVol: -14, coco1Pan: -0.65, coco2Pan: 0.65,
			qChaos: 0.62, coco1DustGrain: 0.12, coco2DustGrain: 0.90,
			coco1Level: 0.92, coco2Level: 0.82
		),
		matrix: [
			[\org1, \coco2_dust, 0.38], [\org2, \coco1_dust, -0.34]
		],
		spesal: [6, 1, 4, 0, 3],
		note: "A slow mist shore and a fast chip shore bloom, clear and exchange debris."
	),
	(
		name: \glass_tide, preset: \quantumDrone,
		params: (
			masterVol: -30, qFreq: 6.8, qChaos: 0.18,
			coco1Speed: 9000, coco2Speed: 21000,
			coco1Organ: 0.04, coco2Organ: 0.035,
			coco1Dust: 0.015, coco2Dust: 0.025,
			coco1DustGrain: 0.74, coco2DustGrain: 0.26,
			coco1Bits: 10, coco2Bits: 8,
			coco1Pan: -0.28, coco2Pan: 0.28
		),
		matrix: [
			[\tri2, \coco1_slotX, 0.22], [\tri4, \coco2_slotX, -0.24],
			[\cas2, \coco1_dust, 0.48], [\cas4, \coco2_dust, -0.50]
		],
		spesal: [2, 5, 1, 6, 3],
		note: "A bright Quantussy tide fractures differently in two clocked memories."
	),
	(
		name: \organ_lanterns, preset: \organMode,
		params: (
			masterVol: -20, qChaos: 0.20,
			coco1Speed: 18000, coco2Speed: 12000,
			coco1Organ: 0.58, coco2Organ: 0.66,
			coco1Hold: 1, coco2Hold: 1,
			coco1Dust: 0, coco2Dust: 0,
			coco1SlotQ: 0.04, coco2SlotQ: 0.04,
			coco1Pan: -0.50, coco2Pan: 0.50
		),
		matrix: [
			[\tri1, \coco1_slotX, 0.14], [\tri2, \coco2_slotX, -0.14]
		],
		spesal: [0, 4, 2, 6, 1],
		note: "Two address-counter lanterns in a fifth, held clear for direct playing."
	)
];

File.mkdir(out);
scenes.do { |scene|
	var state = CQState(nil), matrix = CQMatrix(nil), spesal = scene[\spesal].copy;
	var data = CQMatrix.presets[scene[\preset]];
	var snaps;
	matrix.load(data[\matrix]);
	data[\params].keysValuesDo { |name, value| state.set(name, value) };
	common.keysValuesDo { |name, value| state.set(name, value) };
	scene[\params].keysValuesDo { |name, value| state.set(name, value) };
	scene[\matrix].do { |triple| matrix.patch(triple[0], triple[1], triple[2]) };
	snaps = CQSnapshots(state, matrix, out,
		{ spesal.copy }, { |values| spesal = values.copy }, out);
	snaps.save(scene[\name], note: scene[\note]);
	snaps.free;
};

"CQ_RELEASE_SNAPSHOTS_WRITTEN".postln;
0.exit;
)
