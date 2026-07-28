// SPEC §6.2's completeness obligation, made machine-checkable
// (NIGHT_STANDALONE_PLAN §12.1.1/§12.1.5): a player who starts in Night and
// uses only Night, the Drawer and the persistent Master strip must reach every
// CQSpec parameter. Day, code and the 16n do not count.
//
// The three interactive owner sets are declared independently — Night from its
// own bodies, Master from the persistent strip, Workshop from its drawn
// manifest — and this class proves they are disjoint and that their union is
// exactly `CQSpec.names`. Equality with `CQSpec.names` is the future-proof
// gate: a parameter added tomorrow fails here until some surface draws it.

CQGuiCoverageTest : UnitTest {
	prNight { ^CQGuiNightMap.reachableParamNames }
	prMaster { ^CQGuiPanels.masterParamNames }
	prWorkshop { ^CQGuiWorkshop.paramNames }

	// an inventory may legitimately be a literal `#[...]`, which is immutable —
	// never sort one in place
	prSorted { |names| ^names.asArray.copy.sort { |a, b| a.asString <= b.asString } }

	prAssertUnique { |names, who|
		this.assertEquals(names.asArray.size, names.as(IdentitySet).size,
			"% names each parameter exactly once".format(who));
		names.do { |name|
			this.assert(CQSpec.includes(name),
				"% owns a real CQSpec parameter: %".format(who, name));
		};
	}

	// ---- the partition ----

	test_theThreeSurfacesDeclareTheirIntendedCounts {
		this.assertEquals(this.prNight.size, 31,
			"Night plays 31 interactive parameters");
		this.assertEquals(this.prMaster.size, 5,
			"the persistent Master strip owns 5");
		this.assertEquals(this.prWorkshop.size, 43,
			"the Workshop ledger owns the 43 remaining parameters");
	}

	test_eachSurfaceNamesRealParametersOnce {
		this.prAssertUnique(this.prNight, "Night");
		this.prAssertUnique(this.prMaster, "Master");
		this.prAssertUnique(this.prWorkshop, "Workshop");
	}

	test_interactiveOwnershipIsDisjoint {
		var surfaces = [
			[\Night, this.prNight], [\Master, this.prMaster],
			[\Workshop, this.prWorkshop]
		];
		surfaces.do { |a, i|
			surfaces.copyRange(i + 1, surfaces.size - 1).do { |b|
				var shared = a[1].asArray.select { |name| b[1].includes(name) };
				this.assert(shared.isEmpty,
					"% and % share no interactive parameter (found %)"
						.format(a[0], b[0], shared));
			};
		};
	}

	// The gate itself. Not "79" — that number is only a diagnostic; the contract
	// is that the union IS the registry, so the audit survives a future
	// parameter without being edited.
	test_nightDrawerAndMasterReachEveryCQSpecParameter {
		var union = this.prNight ++ this.prMaster ++ this.prWorkshop;
		var missing = CQSpec.names.reject { |name| union.includes(name) };
		var strangers = union.reject { |name| CQSpec.includes(name) };
		this.assertEquals(union.size, CQSpec.names.size,
			"the three surfaces name each parameter exactly once, with no duplicate owner");
		this.assert(missing.isEmpty,
			"every CQSpec parameter is reachable without code or Day (unreached: %)"
				.format(missing));
		this.assert(strangers.isEmpty,
			"no surface claims a parameter the registry does not have (%)"
				.format(strangers));
		this.assertEquals(this.prSorted(union), CQSpec.names,
			"the interactive union is exactly CQSpec.names");
	}

	test_thePartitionIsCurrentlySeventyNine {
		// a diagnostic, deliberately separate from the gate above: it tells a
		// future reader what the registry held when the contract was written
		this.assertEquals(CQSpec.names.size, 79,
			"the registry holds the 79 parameters the N8 contract was audited against");
	}

	// ---- the intended split, named surface by surface ----

	test_nightOwnsThePerformanceBodies {
		var night = this.prNight;
		#[deckALevel, deckBLevel, preASrc, preBSrc, preADrive, preBDrive,
			preABias, preBBias, qFreq, qChaos].do { |name|
			this.assert(night.includes(name),
				"the night's own bodies play %".format(name));
		};
		[1, 2].do { |n|
			#[\In, \Fb, \Speed, \Spaf, \DolbyIn, \DolbyFb, \Hold, \Flip].do { |suffix|
				var name = ("coco" ++ n ++ suffix).asSymbol;
				this.assert(night.includes(name),
					"the shell plays %".format(name));
			};
		};
		(1..5).do { |k|
			this.assert(night.includes(("c" ++ k ++ "Range").asSymbol),
				"castle %'s range ripple is playable".format(k));
		};
	}

	// A read-only mark is not an owner (§12.1.1). The night draws the mic sprout
	// and reads its roots' slot gains, but neither makes it an interactive owner
	// — otherwise the union would double-count and the Workshop would be free
	// not to draw them.
	test_nightsReadOnlyMarksAreNotOwnership {
		var night = this.prNight;
		this.assert(night.includes(\micGain).not,
			"the night's mic sprout is display-only, never a mic control");
		#[coco1SlotPre, coco2SlotPre, coco1SlotDeck, coco2SlotDeck].do { |name|
			this.assert(night.includes(name).not,
				"the roots read % to draw their vein without owning it".format(name));
		};
		// and a matrix destination is not its base parameter
		#[deckARate, deckBRate].do { |name|
			this.assert(night.includes(name).not,
				"the bird's rate socket is a matrix destination, not a % control"
					.format(name));
		};
	}

	test_masterKeepsPerformanceOnlyAndPanic {
		var master = this.prMaster;
		this.assert(master.includes(\monitorMute),
			"PANIC's parameter stays on the persistent strip");
		this.assert(master.includes(\spaceMix),
			"the strip keeps performance space mix");
		#[spaceSize, spaceDamp].do { |name|
			this.assert(master.includes(name).not,
				"% is a Workshop service control, not a performance one".format(name));
		};
	}

	test_workshopOwnsTheExactInventoryFromTheContract {
		var workshop = this.prWorkshop;
		var expected = #[
			liveGain, micGain, deckARate, deckBRate, deckALoop, deckBLoop,
			spaceSize, spaceDamp,
			coco1Bits, coco1Wrap, coco1Interp, coco1Dust, coco1Organ,
			coco1Level, coco1Pan, coco1Send,
			coco1SlotPre, coco1SlotDeck, coco1SlotQ, coco1SlotX,
			qShLag, c1Off, c2Off, c3Off, c4Off, c5Off,
			c1Send, c2Send, c3Send, c4Send, c5Send,
			coco2Bits, coco2Wrap, coco2Interp, coco2Dust, coco2Organ,
			coco2Level, coco2Pan, coco2Send,
			coco2SlotPre, coco2SlotDeck, coco2SlotQ, coco2SlotX
		];
		// `sort` is in place and a `#[...]` literal is immutable: copy first
		this.assertEquals(this.prSorted(workshop), this.prSorted(expected),
			"the Workshop manifest is the contract's exact 43 names");
	}

	// SPEC §6.2: the GUI never grows a second interactive micGain path. The
	// Workshop is now that one path, so Day must not also own one.
	test_exactlyOneInteractiveMicPath {
		var owners = [
			[\Night, this.prNight], [\Master, this.prMaster],
			[\Workshop, this.prWorkshop]
		].select { |pair| pair[1].includes(\micGain) };
		this.assertEquals(owners.size, 1,
			"exactly one surface owns micGain interactively");
		this.assertEquals(owners.first[0], \Workshop,
			"that one surface is the Workshop ledger");
	}

	// ---- the page's other SPEC §6.2 obligations still have named owners ----

	// A CQSpec-only audit is necessary but not sufficient (§12.1.1): the other
	// §6.2 obligations must still have a named owner after the page lands.
	// The Drawer's own four-page order is asserted where the pages are
	// published, in CQGuiVisualContractTest.
	test_theRemainingObligationsAreOwnedElsewhere {
		this.assertEquals(CQGuiMatrix.cellSize(\compact, 1272), [52, 24],
			"the Inspector still owns the full 16x22 signed matrix");
		#[snapshotName, snapshotSave, snapshotRecall, morph].do { |item|
			this.assert(CQGuiPanels.memoryItems.includes(item),
				"Memory still owns %".format(item));
		};
	}

	// The manifest is the drawn page, not a table beside it: every island's ink
	// must fit the room the page actually has, at BOTH size steps, or the audit
	// would be proving controls nobody can see.
	test_theManifestFitsTheRoomThePageActuallyHas {
		CQGuiScale.steps.do { |step|
			// the page rect this step is chosen for, at its own floor
			var page = if(step == \roomy) { Size(1400, 640) } { Size(1272, 526) };
			var rail = CQGuiScale.railWidth(step, page.width);
			var service = CQGuiWorkshop.serviceWidths(rail, step);
			var main = CQGuiWorkshop.mainWidths(rail, step);
			var gutter = CQGuiScale.metrics(step)[\gutter];
			this.assertEquals(service.sum + (3 * gutter), rail,
				"% : four service modules and three gutters make the rail"
					.format(step));
			this.assertEquals(main.sum + (2 * gutter), rail,
				"% : the three main ledgers and two gutters make the same rail"
					.format(step));
			this.assertEquals(main[0], main[2],
				"% : the two Coco ledgers are exactly the same width".format(step));
			this.assertEquals(rail, CQGuiMatrix.loomSize(step, page.width).width,
				"% : the Workshop rail is the Inspector loom's rail".format(step));
			this.assert(rail <= (page.width - (2 * CQGuiScale.metrics(step)[\pageMargin])),
				"% : the rail keeps its page margin".format(step));
			CQGuiWorkshop.serviceIslands.do { |island|
				this.assertEquals(CQGuiWorkshop.islandHeight(island, step),
					CQGuiScale.serviceBandHeight(step),
					"% : service module '%' is exactly the band"
						.format(step, island[\title]));
			};
			CQGuiWorkshop.mainIslands.do { |island|
				this.assert(CQGuiWorkshop.islandHeight(island, step)
					<= CQGuiScale.tallestIslandHeight(step),
					"% : ledger '%' fits the tallest-island budget"
						.format(step, island[\title]));
			};
			this.assertEquals(
				CQGuiWorkshop.islandHeight(CQGuiWorkshop.cocoIsland(1), step),
				CQGuiWorkshop.islandHeight(CQGuiWorkshop.cocoIsland(2), step),
				"% : both Coco ledgers are one helper and exactly as tall"
					.format(step));
			this.assert(CQGuiScale.minimumHeight(step) <= page.height,
				"% : the whole page fits its own floor without a scroll view (% <= %)"
					.format(step, CQGuiScale.minimumHeight(step), page.height));
		};
	}
}
