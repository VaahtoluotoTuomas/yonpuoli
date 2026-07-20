// Serverless visual-review harness for the first G1 banana-grammar surface.
// Close the window to exit sclang. Audio audition uses manual_checklist.md.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));

matrix.patch(\tri1, \coco1_spaf, 0.58);
matrix.patch(\tri3, \coco1_spaf, 0.28);
matrix.patch(\cas2, \coco1_spaf, -0.36);
matrix.patch(\envA, \coco1_inVca, 0.44);
matrix.patch(\cas4, \coco1_fbVca, -0.52);
matrix.patch(\org1, \castle2_fm, 0.32);
matrix.patch(\tri5, \castle4_spesal, 0.66);
matrix.patch(\coco1, \coco2_fbVca, 0.38);
matrix.patch(\coco2, \coco1_fbVca, -0.34);
matrix.patch(\envB, \deckB_rate, -0.48);

{
	~cqBananaReview = CQGui(state, matrix, config);
	AppClock.sched(0.25, {
		if(~cqBananaReview.isClosed) {
			"CQ_BANANA_REVIEW_CLOSED".postln;
			0.exit;
			nil
		} {
			~cqBananaReview.deckLabel(\deckA).string =
				"123681__aidenfry__cricket-and-bird-field-recording.wav";
			0.25
		}
	});
	"CQ_BANANA_REVIEW_READY".postln;
}.defer;
)
