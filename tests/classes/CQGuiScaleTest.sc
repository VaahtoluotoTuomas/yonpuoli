// The Drawer's two reviewed size steps (GUI_SPEC §8, CQGuiScale).
//
// The workbench is the one surface in this instrument that is only text, rules
// and rails, so its type size is a functional property, not a taste. These
// contracts pin what the user asked for at the N8 review: bigger type than the
// Day panel's 10 px, a bigger step again when a maximized window has the room,
// a margin that is always kept, and a page that can never ask for more height
// than the step it was chosen for actually has.

CQGuiScaleTest : UnitTest {
	// the real Drawer page rect at both review sizes, measured on the Qt
	// composition 2026-07-28 (stage 1280x570 -> page 1272x526,
	// stage 1442x730 -> page 1434x686)
	prMinPage { ^Size(1272, 526) }
	prBigPage { ^Size(1434, 686) }

	test_thePageRectComesFromTheMeasuredStageInset {
		this.assertEquals(CQGuiScale.pageSizeFor(1280, 570), this.prMinPage,
			"a 1280x720 window gives the Drawer page 1272x526");
		this.assertEquals(CQGuiScale.pageSizeFor(1442, 730), this.prBigPage,
			"a maximized window gives it 1434x686");
	}

	test_theStepFollowsTheRoomThePageHas {
		this.assertEquals(
			CQGuiScale.stepFor(this.prMinPage.width, this.prMinPage.height),
			\compact, "the windowed minimum is the compact step");
		this.assertEquals(
			CQGuiScale.stepFor(this.prBigPage.width, this.prBigPage.height),
			\roomy, "a maximized window earns the roomy step");
		this.assertEquals(CQGuiScale.stepFor(1912, 846), \roomy,
			"so does a 1080p window maximized at 100 % scaling");
		this.assertEquals(CQGuiScale.stepFor(1600, 500), \compact,
			"a wide but short page stays compact — height is a real constraint");
		this.assertEquals(CQGuiScale.stepFor(1200, 900), \compact,
			"and so does a tall but narrow one");
	}

	// The case the first version got wrong (found by the user, 2026-07-28):
	// Windows laptops are commonly run at 125 % or 150 % scaling, and sclang
	// works in logical px, so a 1080p laptop maximized reports a far smaller
	// page than its panel suggests. A threshold that misses 125 % means the one
	// window size the instrument is actually played at never gets bigger type.
	test_aScaledLaptopStillReachesTheRoomyStep {
		// 1920x1080 at 125 % -> 1536x824 logical window -> 1528x630 page
		this.assertEquals(CQGuiScale.stepFor(1528, 630), \roomy,
			"a 1080p laptop at 125 % scaling reaches the roomy step");
		// at 150 % the logical window IS the 1280x720 minimum: there is
		// genuinely no room, and staying compact is correct, not a bug
		this.assertEquals(CQGuiScale.stepFor(1272, 526), \compact,
			"at 150 % scaling the page really is the minimum and stays compact");
	}

	// The whole point of the roomy step: it must never be chosen for a page that
	// cannot hold it, or the ledger would clip instead of growing. Both floors
	// are derived from the step's own metrics, so this cannot drift.
	test_neitherStepCanBeChosenForAPageItDoesNotFit {
		var floorW = CQGuiScale.minimumWidth(\roomy);
		var floorH = CQGuiScale.minimumHeight(\roomy) + CQGuiScale.comfort;
		this.assertEquals(CQGuiScale.stepFor(floorW, floorH), \roomy,
			"roomy is chosen at exactly its own derived floor");
		this.assertEquals(CQGuiScale.stepFor(floorW - 1, floorH), \compact,
			"and not one pixel narrower");
		this.assertEquals(CQGuiScale.stepFor(floorW, floorH - 1), \compact,
			"nor one pixel shorter");
		CQGuiScale.steps.do { |step|
			this.assert(CQGuiScale.minimumHeight(step)
				<= (if(step == \roomy) { floorH } { 526 }),
				"% fits the page it is chosen for".format(step));
		};
		// the compact step must fit the app's own minimum window, always
		this.assert(CQGuiScale.minimumHeight(\compact) <= this.prMinPage.height,
			"compact fits the 1280x720 minimum with room to spare (% <= %)"
				.format(CQGuiScale.minimumHeight(\compact), this.prMinPage.height));
	}

	test_theRoomyStepIsBiggerEverywhereThatMatters {
		var compact = CQGuiScale.metrics(\compact);
		var roomy = CQGuiScale.metrics(\roomy);
		#[\title, \label, \value, \subgroup, \notice, \mono, \tiny,
			\rowHeight, \titleHeight, \groupHeight, \ledgerLabel, \ledgerValue,
			\cellHeight, \headerHeight, \loomLabel, \loomLabelSize,
			\loomDestSize, \boxHeight, \barHeight].do { |key|
			this.assert(roomy[key] > compact[key],
				"the roomy step grows % (% -> %)"
					.format(key, compact[key], roomy[key]));
		};
	}

	// §16.4 fixed the Day panel at 10 px after a real review and forbade
	// shrinking below it. The Drawer is read as prose, so it starts above.
	test_everyDrawerTypeSizeClearsTheDayPanelsFloor {
		CQGuiScale.steps.do { |step|
			var m = CQGuiScale.metrics(step);
			#[\title, \label, \value, \subgroup, \notice, \mono].do { |key|
				this.assert(m[key] >= 11,
					"% : % is at least 11 px (got %)".format(step, key, m[key]));
			};
			this.assert(m[\label] > CQInk.metrics[\labelSize],
				"% : a ledger label is larger than the Day panel's 10 px"
					.format(step));
			this.assert(m[\subgroup] > CQInk.metrics[\rangeSize],
				"% : a subgroup label is larger than a dense-map annotation"
					.format(step));
		};
	}

	// A row must be taller than its own text, or the ledger clips
	test_everyRowCanHoldItsOwnType {
		CQGuiScale.steps.do { |step|
			var m = CQGuiScale.metrics(step);
			this.assert(m[\rowHeight] >= (m[\label] + 8),
				"% : a 24 px-class row holds its label".format(step));
			this.assert(m[\rowHeight] >= (m[\value] + 8),
				"% : and its exact value".format(step));
			this.assert(m[\groupHeight] >= (m[\subgroup] + 4),
				"% : a subgroup band holds its own name".format(step));
			this.assert(m[\titleHeight] >= (m[\title] + 6),
				"% : a title band holds its title and its rule".format(step));
			this.assert(m[\boxHeight] >= (m[\mono] + 8),
				"% : a text box holds mono digits".format(step));
		};
	}

	// ---- the shared rail ----

	test_theRailFillsThePageButAlwaysKeepsItsMargin {
		CQGuiScale.steps.do { |step|
			var m = CQGuiScale.metrics(step);
			// including widths below this step's own floor: a rail may never
			// overflow its page even if a step is applied where it would not
			// have been chosen
			[900, 1272, 1434, 1912, 2560].do { |pageWidth|
				var rail = CQGuiScale.railWidth(step, pageWidth);
				var slate = (pageWidth - rail) / 2;
				this.assert(rail <= (pageWidth - (2 * m[\pageMargin])),
					"% at % : the rail keeps its % px margin"
						.format(step, pageWidth, m[\pageMargin]));
				this.assert(slate >= m[\pageMargin],
					"% at % : slate on each side is at least the margin (%)"
						.format(step, pageWidth, slate));
				this.assertEquals(rail,
					m[\loomLabel] + (CQGuiScale.loomCell(step, pageWidth)
						* CQMatrix.destNames.size),
					"% at % : the rail is exactly the loom's own width"
						.format(step, pageWidth));
			};
		};
	}

	test_theRailGrowsWithThePageUntilTheLoomCellIsCapped {
		var narrow = CQGuiScale.railWidth(\roomy, 1434);
		var wide = CQGuiScale.railWidth(\roomy, 1912);
		var absurd = CQGuiScale.railWidth(\roomy, 3000);
		this.assert(wide > narrow,
			"a wider page buys a wider rail rather than only slate");
		this.assertEquals(wide, absurd,
			"past the cell cap the extra becomes slate — a knot grid spread over "
			"two thousand pixels stops reading as a grid");
		this.assertEquals(CQGuiScale.loomCell(\roomy, 3000),
			CQGuiScale.metrics(\roomy)[\cellMax],
			"the cap is the loom cell's own maximum");
		// a page that is wide but too short for the roomy step must still spend
		// its width on the rail rather than turning all of it into slate
		this.assert(CQGuiScale.railWidth(\compact, 1528)
			> CQGuiScale.railWidth(\compact, 1272),
			"a wide compact page widens its rail too");
		// below the app's own 1280 minimum the floor yields rather than pushing
		// the rail off the page — a minimum that overflows is not a minimum
		this.assert(CQGuiScale.loomCell(\compact, 900)
			< CQGuiScale.metrics(\compact)[\cellMin],
			"a narrower page yields the floor instead of breaking the margin");
		this.assert(CQGuiScale.loomCell(\compact, 900) >= 8,
			"but the cell never collapses to nothing");
	}

	// ---- column arithmetic ----

	test_columnsAlwaysSumBackToTheRail {
		[1224, 1336, 1400, 1688, 1224 + 1, 1224 + 2].do { |rail|
			[2, 3, 4, 5].do { |count|
				[16, 20].do { |gutter|
					var widths = CQGuiScale.columns(rail, count, gutter);
					this.assertEquals(widths.size, count,
						"% into % columns".format(rail, count));
					this.assertEquals(widths.sum + (gutter * (count - 1)), rail,
						"% into % columns with % gutters sums back exactly"
							.format(rail, count, gutter));
					// an odd split may put up to two pixels on the middle to keep
					// its outer pair identical; nothing may differ by more
					this.assert(widths.maxItem - widths.minItem <= 2,
						"no column is more than two pixels from its neighbours");
					this.assert(widths.every { |w| w > 0 },
						"no column collapses");
				};
			};
		};
	}

	// the Coco pair's symmetry is structural, not luck: an odd split gives its
	// remainder to the middle so the outer two stay identical
	test_anOddSplitKeepsItsOuterColumnsIdentical {
		(1220 .. 1240).do { |rail|
			var widths = CQGuiScale.columns(rail, 3, 16);
			this.assertEquals(widths[0], widths[2],
				"rail % : the two Coco ledgers are the same width".format(rail));
		};
	}

	// ---- the shared page frame ----

	// All four Drawer pages open with the same air under the tab row and centre
	// the same rail, so the workbench reads as one surface rather than four
	// adjacent layouts (the user's N8 feedback: Inspector and Workshop had
	// different margins from Spellbook and Memory).
	test_everyPageSharesOneFrame {
		CQGuiScale.steps.do { |step|
			var page = if(step == \roomy) { 1434 } { 1272 };
			var rail = CQGuiScale.railWidth(step, page);
			var frame = CQGuiScale.pageFrame(step, rail, page);
			this.assertEquals(frame[\top], CQGuiScale.metrics(step)[\vMargin],
				"% : the top margin is the step's own vertical margin"
					.format(step));
			this.assertFloatEquals(frame[\side], (page - rail) / 2,
				"% : the rail is centred".format(step), 1e-9);
			this.assert(frame[\side] >= CQGuiScale.metrics(step)[\pageMargin],
				"% : and never closer to the edge than the page margin"
					.format(step));
		};
		// a rail somehow wider than its page still reports a usable margin
		this.assert(CQGuiScale.pageFrame(\compact, 2000, 1272)[\side]
			>= CQGuiScale.metrics(\compact)[\pageMargin],
			"an over-wide rail cannot produce a negative margin");
	}

	// ---- buttons ----

	test_aButtonIsMeasuredFromItsOwnCaption {
		CQGuiScale.steps.do { |step|
			var short = CQGuiScale.buttonWidth(step, "recall");
			var long = CQGuiScale.buttonWidth(step, "copy all as preset data");
			this.assert(long > short,
				"% : a long caption gets a wider button".format(step));
			this.assert(short >= 48,
				"% : even a short caption keeps a usable target".format(step));
			this.assert(long >= CQGuiScale.textWidth("copy all as preset data",
				CQGuiScale.metrics(step)[\label]),
				"% : the caption always fits inside its button".format(step));
		};
		this.assert(CQGuiScale.buttonWidth(\roomy, "recall")
			> CQGuiScale.buttonWidth(\compact, "recall"),
			"the same caption gets a bigger button at the bigger step");
	}

	test_textMeasurementNeverReturnsNothing {
		this.assert(CQGuiScale.textWidth("copy line", 12) > 0,
			"a measured caption has a width");
		this.assertEquals(CQGuiScale.textWidth("", 12), 0,
			"an empty caption measures zero");
		this.assert(CQGuiScale.textWidth("0.0040", 15, true)
			> CQGuiScale.textWidth("0.0040", 12, true),
			"mono digits grow with their size");
	}
}
