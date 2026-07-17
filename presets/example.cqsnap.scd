// Example snapshot (SPEC §6.3 / ARCHITECTURE §7). Load with CQ.recall(\example);
// CQ.snap(\name) writes complete files like this next to it. Hand-written files
// may be sparse: parameters not listed keep their current values. Every value
// is clamped on load; micGain and monitorMute can never sneak in loud (SPEC §7).
(
version: 1,
params: (
	coco1Spaf: 0.12, coco2Spaf: 0.12,
	coco1Fb: 0.9, coco2Fb: 0.9,
	qChaos: 0.25
),
matrix: [
	[\tri1, \coco1_spaf, 0.35], [\tri1, \coco2_spaf, 0.35],
	[\tri3, \coco1_spaf, 0.12], [\tri3, \coco2_spaf, 0.12],
	[\cas1, \castle2_fm, 0.3], [\cas2, \castle3_fm, 0.3],
	[\cas3, \castle4_fm, 0.3], [\cas4, \castle5_fm, 0.3],
	[\cas5, \castle1_fm, 0.3]
],
spesal: [0, 0, 0, 0, 0],
meta: (name: "example", date: "2026-07-17",
	note: "classic bananas with more feedback and a livelier Quantussy")
)
