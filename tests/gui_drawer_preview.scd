// Serverless visual-review harness for the workbench + patch-loom experiment.
// It opens on the instrument. The Drawer tab beside Night opens the full central
// Inspector; click Workshop/Spellbook/Memory to inspect the other pages.
// Memory shows its truthful live-patch fingerprint here; saved-seed recall and
// A→B morph use Cocoquantus.snapshots and are exercised with the booted instrument.
// The Workshop is fully playable serverlessly: every row writes through CQState,
// so the page can be judged without an audio server (its *sound* cannot).
// Close the window to exit sclang. No audio server is started.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));
var previous = ~cqDrawerReview;
var review;

// Prepare the visible loom before GUI construction; no open-time preset burst
// or automatic Drawer toggle competes with the user's first toolbar-tab click.
matrix.preset(\classic, origin: \code, committed: true, operation: \preset);
matrix.patch(\envA, \deckA_rate, -0.42);
matrix.patch(\cas4, \coco2_fbVca, 0.37);

{
	// Re-evaluating this file in the IDE replaces, rather than leaks, its preview.
	previous !? { |old| if(old.isClosed.not) { old.free } };
	review = CQGui(state, matrix, config);
	~cqDrawerReview = review;
	AppClock.sched(0.25, {
		// Two small post-build commits populate the Spellbook for tab review.
		matrix.patch(\envA, \deckA_rate, -0.39);
		matrix.patch(\cas4, \coco2_fbVca, 0.33);
		nil
	});
	"CQ_DRAWER_REVIEW_READY · click drawer beside night".postln;
}.defer;
)
