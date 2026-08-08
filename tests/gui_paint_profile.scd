// gui_paint_profile.scd — measure the cable layer inside the REAL Qt paint
// event (G1.3's law: a Pen call costs ~53 µs at the language/Qt boundary, so
// a drawing's price is its call count — and the only honest place to read it
// is the drawFunc itself). Run headless through the project gate:
//   powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_paint_profile.scd
//
// It answers, with numbers:
//   1. does the Image offscreen API work on this platform (draw / clear / blit)?
//   2. what does one full-ink cable-layer repaint cost at N planted cables?
//   3. what does the same repaint cost as a blit from the offscreen cache?
//   4. what does a storm-form repaint (weather-gesture ink) cost?
// Prints CQ_PAINT_PROFILE_DONE last, or CQ_PAINT_PROFILE_FAILED.

(
var state = CQState(nil);
var matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));
var gui, ok = true, results = List.new;
var counts = #[12, 35, 48, 70, 140, 512];
var abort = { |error|
	error !? { error.reportError };
	("FAIL: paint profile aborted"
		++ (error !? { ": " ++ error.errorString } ? "")).postln;
	gui !? { gui.free };
	"CQ_PAINT_PROFILE_FAILED".postln;
	1.exit;
};
// a deterministic sparse patch of exactly n cables (mayhem's own sin-hash
// weights, so polarity and |w| spread like a real dense patch)
var plant = { |n|
	var triples = List.new;
	block { |break|
		CQMatrix.destNames.do { |dest, di|
			CQMatrix.sourceNames.do { |src, si|
				var w = (sin((si * 5.7) + (di * 2.3)) * 0.55).round(0.01);
				if(w == 0.0) { w = 0.05 };
				triples.add([src, dest, w]);
				if(triples.size >= n) { break.value };
			};
		};
	};
	matrix.load(triples.asArray);
};

// ---- 1. Image feature check, before anything depends on it ----
{
	try {
		var img, blitOk = false, drawOk = false, clearOk = false;
		try {
			img = Image(64, 48);
			drawOk = true;
			try { img.draw { Pen.fillColor = Color.red; Pen.addRect(Rect(0, 0, 8, 8)); Pen.fill };
			} { drawOk = false };
		} { drawOk = false };
		if(drawOk) {
			// does a fresh image start transparent, and did the draw actually
			// land its pixels synchronously (not deferred)?
			try {
				var c = img.getColor(30, 30);
				var inked = img.getColor(4, 4);
				clearOk = c.alpha < 0.01;
				drawOk = inked.red > 0.9 and: { inked.alpha > 0.9 };
				("Image(64,48) pixel (30,30) after draw: " ++ c).postln;
				("Image(64,48) pixel (4,4) after draw: " ++ inked).postln;
			} { |e| ("getColor not available: " ++ e.errorString).postln };
		};
		("IMAGE_DRAW_OK: " ++ drawOk).postln;
		("IMAGE_TRANSPARENT_OK: " ++ clearOk).postln;

		gui = CQGui(state, matrix, config);
		AppClock.sched(0.4, {
			try {
				var overlay = gui.nightMap.overlay;
				var view = overlay.view;
				var origDraw = view.drawFunc;
				var times, measure, step, finish;
				// wrap the REAL drawFunc: wall time of the language-side Pen
				// marshaling — the measured cost from G1.3
				var lastMs = 0;
				view.drawFunc = { |v|
					var t0 = Main.elapsedTime;
					origDraw.value(v);
					lastMs = (Main.elapsedTime - t0) * 1000;
				};
				// three regimes per cable count: full ink (invalidate before
				// every repaint — the pre-cache price), blit (the image is
				// fresh: the price of every repaint the ink did not ask for),
				// and storm form (the §7.1 weather-gesture drawing)
				measure = { |label, reps = 6, prep, done|
					var samples = List.new;
					var one = { |remaining|
						prep.value;
						view.refresh;
						AppClock.sched(0.06, {
							samples.add(lastMs);
							if(remaining > 1) { one.value(remaining - 1) } {
								var sorted = samples.asArray.sort;
								var median = sorted[sorted.size div: 2];
								results.add([label, median.round(0.01)]);
								("%: median % ms (samples: %)".format(
									label, median.round(0.01),
									samples.collect { |m| m.round(0.1) })).postln;
								done.value;
							};
							nil
						});
					};
					one.value(reps);
				};
				step = { |i|
					if(i >= counts.size) { finish.value } {
						var n = counts[i];
						plant.value(n);
						overlay.invalidate;
						AppClock.sched(0.1, {
							measure.value("full ink, % cables".format(n), 6,
								{ overlay.invalidate }, {
								// let the deferred render land, then time blits
								AppClock.sched(0.15, {
									measure.value("blit,     % cables".format(n), 6,
										{ }, {
										overlay.stormMode = true;
										measure.value("storm,    % cables".format(n), 6,
											{ }, {
											overlay.stormMode = false;
											step.value(i + 1);
										});
									});
									nil
								});
							});
							nil
						});
					};
				};
				finish = {
					"".postln;
					"---- summary (median ms per real Qt repaint) ----".postln;
					results.do { |row| ("  " ++ row[0] ++ ": " ++ row[1] ++ " ms").postln };
					gui.free;
					AppClock.sched(0.3, { "CQ_PAINT_PROFILE_DONE".postln; 0.exit; nil });
				};
				step.value(0);
			} { |e| abort.value(e) };
			nil
		});
	} { |e| abort.value(e) };
}.fork(AppClock);
)
