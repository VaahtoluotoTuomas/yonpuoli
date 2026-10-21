CQInfoTest : UnitTest {
	test_componentOwnershipAndBoundedNavigation {
		var model = CQInfoState(), entry = CQInfoTour.indexOf(\ports);
		(CQMatrix.sourceNames ++ CQMatrix.destNames).do { |name|
			this.assert(CQInfoGroups.owner([\endpoint, name]).notNil, "every port has one component owner");
		};
		this.assertEquals(CQInfoGroups.owner([\param, \coco1Hold]), [\group, \coco, 0], "heart belongs to Coco 1");
		this.assertEquals(CQInfoGroups.owner([\endpoint, \coco2_record]), [\group, \coco, 1], "record socket belongs to Coco 2");
		this.assertEquals(CQInfoGroups.owner([\source, 1, \deckA]), [\group, \root, 1], "source strand belongs to its root");
		this.assertEquals(CQInfoGroups.owner([\castle, 4]), [\group, \castles, 0], "all castles share one explanation");
		this.assertEquals(CQInfoGroups.owner([\cable, \tri1, \deckA_rate]), [\tour, entry], "cable enters the shared guide at its port introduction");
		this.assert(CQInfoGroups.owner([\water]).isNil, "background is still empty");
		model.tour;
		this.assert(model.target.isNil, "the combined guide cannot enable info");
		model.enabled = true; model.select([\tour, entry]);
		model.page(100);
		this.assertEquals(model.target, [\tour, CQInfoTour.count - 1], "cable entry has bounded forward navigation");
		this.assert(model.inspecting, "paging an inspected cable never enables performance");
		model.page(-100);
		this.assertEquals(model.target, [\tour, 0], "cable entry can go back to the overview");
		model.select([\group, \coco, 0]); model.page(100);
		this.assertEquals(model.pageIndex, 0, "Coco stays on its complete overview");
		this.assertEquals(model.pageCount, 1, "Coco has one page");
		model.select([\group, \castles, 0]); model.page(100);
		this.assertEquals(model.pageIndex, 1, "castles keep overview and anatomy pages");
		model.select([\group, \moon, 0]); model.page(100);
		this.assertEquals(model.pageIndex, 0, "a single-page component cannot turn into a guide");
		this.assert(model.inspecting, "component navigation stays read-only");
	}
	test_cardPlacementProtectsSubjects {
		var size = Point(1280, 570), subject = Rect(60, 70, 400, 450), rect;
		rect = CQInfoGroups.place(size, 440, 520, [subject], true);
		this.assert(rect.notNil and: { rect.intersects(subject).not }, "card clears the whole subject");
		this.assert(Rect(0, 0, size.x, size.y).contains(rect), "card stays above Master");
		this.assertEquals(rect.left, subject.right + 12, "left Coco card sits beside its subject, not at the far edge");
		subject = Rect(820, 70, 400, 450);
		rect = CQInfoGroups.place(size, 440, 520, [subject]);
		this.assertEquals(rect.right, subject.left - 12, "right Coco card uses the adjacent left side");
		subject = Rect(100, 300, 1000, 200);
		rect = CQInfoGroups.place(size, 440, 200, [subject]);
		this.assertEquals(rect.bottom, subject.top - 12, "an adjacent vertical slot works when neither side fits");
		this.assertEquals(rect.center.x, subject.center.x, "vertical fallback aligns with the subject");
		subject = Rect(900.9, 190, 446, 406);
		rect = CQInfoGroups.place(1442@724, 880, 601, [subject]);
		this.assert((subject.left - rect.right - 12).abs < 0.001,
			"a wide card keeps its subject gap even when the stage-edge candidate is closer");
		this.assert(CQInfoGroups.place(size, 440, 900, [subject]).isNil, "oversize content is never silently clipped");
		this.assert(CQInfoGroups.place(size, 440, 500, [Rect(0, 0, 1280, 570)]).isNil,
			"no position is preferred to covering the subject");
	}
	test_gestureLeadersKeepTheirAxes {
		var rect = Rect(20, 70, 400, 100), at = rect.center, occupied = List.new;
		[\moon, \eye].do { |kind|
			var rows = CQInfoPages.page([\group, kind, 0])[\rows];
			this.assertEquals(rows.count { |r| r[\axis] == \horizontal }, 1, "one horizontal gesture in " ++ kind);
			this.assertEquals(rows.count { |r| r[\axis] == \vertical }, 1, "one vertical gesture in " ++ kind);
			rows.do { |row|
				if(row[\refs].notEmpty) {
					var label = CQInfoGroups.labelPoint(at, rect, occupied, row[\axis]);
					if(row[\axis] == \horizontal) { this.assertEquals(label.y, at.y, "sideways has a horizontal leader") };
					if(row[\axis] == \vertical) { this.assertEquals(label.x, at.x, "up/down has a vertical leader") };
					this.assert(CQNightGeometry.distance(at, label) >= 16, "number clears its anchor");
					this.assert(occupied.every { |p| CQNightGeometry.distance(p, label) >= 20 }, "number clears other labels");
					occupied.add(label);
				};
			};
			occupied.clear;
		};
	}
	test_cocoOverviewKeepsControlsAndPorts {
		2.do { |i|
			var group = [\group, \coco, i], rows = CQInfoPages.page(group)[\rows];
			var refs = rows.collect { |row| row[\refs] }.flatten;
			var prefix = "coco" ++ (i + 1);
			[\Spaf, \In, \Fb].do { |suffix, n|
				this.assertEquals(rows[n + 1][\refs].first, [\param, (prefix ++ suffix).asSymbol],
					"numbers 2, 3, 4 anchor the three lower crescents separately");
			};
			this.assertEquals(rows[8][\refs], [[\param, (prefix ++ "SlotQ").asSymbol], [\endpoint, (prefix ++ "_slotQ").asSymbol]],
				"Q inlet has its own control/socket row");
			this.assertEquals(rows[9][\refs], [[\param, (prefix ++ "SlotX").asSymbol], [\endpoint, (prefix ++ "_slotX").asSymbol]],
				"bridge has its own numbered valve and hollow modulation ring");
			this.assert(rows[9][\text].contains("Coco " ++ (2 - i)), "bridge names the opposite memory it receives");
			(CQMatrix.sourceNames ++ CQMatrix.destNames).do { |name|
				if(CQInfoGroups.owner([\endpoint, name]) == group) {
					this.assert(refs.includesEqual([\endpoint, name]), "single Coco page retains port " ++ name);
				};
			};
			[\Hold, \Speed, \Spaf, \In, \Fb, \Level, \Organ, \Flip, \DolbyIn, \DolbyFb, \SlotQ, \SlotX].do { |suffix|
				this.assert(refs.includesEqual([\param, (prefix ++ suffix).asSymbol]), "single Coco page retains control " ++ suffix);
			};
			this.assert(refs.includesEqual([\dust, i]) and: { refs.includesEqual([\skip, i]) }, "dust and skip remain located");
		};
	}
	test_allComponentPagesHaveUsefulLinks {
		var geometry = CQNightGeometry.compute(1280, 570);
		CQInfoGroups.all.do { |group|
			CQInfoPages.pageCount(group).do { |page|
				var note = CQInfoPages.page(group, page);
				this.assert(note[\title].notEmpty and: { note[\intro].notEmpty }, "component has a short introduction");
					this.assert(note[\rows].size.inclusivelyBetween(3, if(group[1] == \coco) { 10 } { 6 }), "a page has a bounded number of concise rows");
				note[\rows].do { |row|
					this.assert(row[\label].notEmpty and: { row[\text].notEmpty }, "each row has a named explanation");
					row[\refs].do { |ref| this.assert(CQInfoText.topic(ref).notNil, "links refer to existing instrument identities") };
				};
			};
		};
		this.assertEquals(CQInfoGroups.at(geometry[\cocos][0][\center], geometry, [\cable, \tri1, \coco1_spaf]),
			[\group, \coco, 0], "component wins over a crossing cable in inspection");
		this.assertEquals(CQInfoGroups.at(geometry[\eye], geometry, [\body, \quantussy]),
			[\group, \eye, 0], "central eye keeps its own subject");
	}
	test_enablingOnlyArmsInspection {
		var model = CQInfoState();
		this.assert(model.select([\param, \coco1Hold]).not, "disabled info ignores selection");
		model.enabled = true;
		this.assert(model.inspecting and: { model.target.isNil },
			"enabling leaves the instrument exposed, without a welcome card");
		model.select([\param, \coco1Hold]); model.more;
		this.assert(model.expanded, "a selected note can be expanded");
		model.select([\bird, 1]);
		this.assertEquals(model.target, [\bird, 1], "explicit selection replaces the note");
		this.assert(model.expanded.not, "a new selection starts compact");
		model.enabled = false; model.enabled = true;
		this.assert(model.target.isNil, "re-enabling cannot revive the previous card");
	}
	test_onlyTheGuideEnablesPlaying {
		var model = CQInfoState();
		model.enabled = true; model.select([\param, \coco1Spaf]); model.more;
		this.assert(model.inspecting, "reading an expanded note keeps inspection armed");
		model.tour(50);
		this.assertEquals(model.target, [\tour, CQInfoTour.count - 1], "tour navigation stays bounded");
		this.assert(model.playing, "the guide lets the player perform its steps");
		this.assert(model.select([\moon]).not, "playing cannot replace the guide");
		model.dismiss;
		this.assert(model.target.isNil and: { model.inspecting },
			"close returns to an unobstructed inspection mode");
	}
	test_infoBreathIsSlowSmoothAndBounded {
		this.assertEquals(CQInk.infoPulse(0), 0, "the breath starts at its quiet end");
		this.assert((CQInk.infoPulse(2) - 1).abs < 0.00001, "two seconds to the bright end");
		this.assert(CQInk.infoPulse(4).abs < 0.00001, "four seconds for a complete breath");
		120.do { |i|
			var at = i / 15, value = CQInk.infoPulse(at);
			this.assert(value >= 0 and: { value <= 1 }, "the breath stays bounded");
			this.assert((CQInk.infoPulse(at + (1 / 15)) - value).abs < 0.06,
				"adjacent frames do not flash or jump");
		};
	}
	test_conceptsIntroduceActionsWithinOneGuide {
		var ids = CQInfoTour.steps.collect { |step| step[\id] };
		this.assertEquals(ids.asSet.size, ids.size, "lessons have unique stable identities");
		[[\overview, \load], [\coco, \write], [\castles, \connect], [\ports, \connect],
			[\connect, \depth], [\depth, \edit], [\input, \feedback], [\cross, \crossListen]].do { |pair|
			this.assert(CQInfoTour.indexOf(pair[0]) < CQInfoTour.indexOf(pair[1]), "introduction precedes use: " ++ pair);
		};
		this.assertEquals(CQInfoTour.indexOf(\feedback), CQInfoTour.indexOf(\input) + 1,
			"Input and Feedback occupy consecutive, separate lessons");
		[\input, \feedback].do { |id|
			var step = CQInfoTour.step(CQInfoTour.indexOf(id));
			this.assertEquals(step[\items].size, 1, "one crescent subject at a time: " ++ id);
			this.assertEquals(step[\items][0][3], [[\param, if(id == \input) { \coco2In } { \coco2Fb }]],
				"the illustration locates only the named control");
		};
		CQInfoTour.count.do { |page|
			var note = CQInfoTour.step(page), refs = CQInfoTour.cueRefs(page);
			this.assert(note[\gesture].notEmpty and: { note[\body].notEmpty }, "every lesson explains its purpose");
			this.assertEquals(refs.isEmpty, note[\concept] == true, "concepts have hover references without action brackets");
		};
	}
	test_shortcutsRemainReadingAndUseContext {
		var model = CQInfoState(), rows = CQInfoPages.shortcuts(0)[\rows];
		model.shortcuts;
		this.assert(model.target.isNil, "shortcuts cannot enable info");
		model.enabled = true; model.tour(2); model.shortcuts;
		this.assert(model.inspecting and: { model.target == [\shortcuts, 0] },
			"shortcuts leaves the playable guide for inspection");
		model.page(100);
		this.assertEquals(model.target, [\shortcuts, 1], "shortcuts has two bounded pages");
		model.page(-100);
		this.assertEquals(model.target, [\shortcuts, 0], "back stops at page one");
		this.assert(rows[0][\text].contains("toggle") and: { rows[0][\text].contains("write") },
			"1/2 are write/hold toggles, not the output recorder");
		this.assert(rows[5][\text].contains("stereo output"), "R records the output to a file");
		this.assert(CQInfoPages.shortcuts(1)[\rows].last[\text].contains("While holding a cable"),
			"Z/X only apply to the cable in hand");
		model.dismiss;
		this.assert(model.inspecting and: { model.target.isNil }, "closing shortcuts keeps info armed");
	}
	test_learningGuideKeepsItsPicturesAndCableVocabulary {
		var note = CQInfoTour.step(CQInfoTour.indexOf(\ports));
		this.assert(note[\gesture].contains("built-in paths") and: { note[\body].contains("hollow input") }, "sound/control distinction precedes patching");
		CQInfoTour.count.do { |step|
			var items = CQInkInfo.tourItems(step);
			this.assert(items.notEmpty and: { items.every { |item| item[3].notEmpty } }, "every picture locates its real subject");
		};
		this.assertEquals(CQInkInfo.tourItems(CQInfoTour.indexOf(\write)).first[0], \heartOpen, "write step shows a hollow heart");
		this.assertEquals(CQInkInfo.tourItems(CQInfoTour.indexOf(\hold)).first[0], \heartClosed, "hold step shows a sealed heart");
		this.assertEquals(CQInkInfo.tourItems(CQInfoTour.indexOf(\hold)).last[2], \down, "capture lowers dry");
		this.assert(CQInfoTour.step(CQInfoTour.indexOf(\connect))[\body].contains("Z / X"), "pre-plant strength remains taught before release");
		this.assert(CQInfoTour.step(CQInfoTour.indexOf(\edit))[\body].contains("seated end"), "moving a cable remains taught");
		this.assert(CQInfoTour.step(CQInfoTour.indexOf(\cross))[\detail].contains("influences add"), "fan-out and summing remain taught");
	}
	test_stableFooterAndBoundaries {
		var model = CQInfoState(), actions;
		model.enabled = true;
		[\shortcuts, \tour, \advanced].do { |kind|
			model.perform(kind); actions = model.footerItems.collect { |b| b[1] };
			this.assertEquals(actions, #[back, next, close], "all guides share stable actions");
			this.assert(model.footerItems[0][2].not and: { model.footerItems[1][2] }, "first page disables only back");
			model.page(100);
			this.assertEquals(model.footerItems.collect { |b| b[1] }, actions, "last page does not replace or move buttons");
			this.assert(model.footerItems[0][2] and: { model.footerItems[1][2].not }, "last page disables only next");
		};
		model.select([\group, \castles, 0]); // Guide is playing: leave it first.
		model.dismiss; model.select([\group, \castles, 0]);
		this.assertEquals(model.footerItems.collect { |b| b[1] }, #[back, next, close], "component pagination uses same footer");
		model.dismiss;
		this.assert(model.canPage(1).not, "no invisible-card navigation");
	}
	test_advancedBuildsOnePatchInDependencyOrder {
		var guide = CQInfoAdvanced, ids = guide.steps.collect { |step| step[\id] }, model = CQInfoState();
		this.assertEquals(ids.asSet.size, ids.size, "advanced lessons have unique identities");
		[[\prepare, \clear], [\clear, \organ], [\organ, \breathe], [\steps, \spesal],
			[\rootSource, \rootCapture], [\rootCapture, \rootTone], [\rootTone, \envelope],
			[\castleSound, \qCapture], [\qCapture, \organFm], [\store, \variant],
			[\variant, \blend], [\blend, \breed], [\weather, \drawer]].do { |pair|
			this.assert(guide.indexOf(pair[0]) < guide.indexOf(pair[1]), "advanced prerequisite precedes use: " ++ pair);
		};
		guide.count.do { |page|
			guide.items(page).do { |item|
				this.assert(item[3].notEmpty, "advanced picture has real semantic references");
				item[3].do { |ref|
					if(ref[0] == \param) { this.assert(CQSpec.names.includes(ref[1]), "advanced parameter exists: " ++ ref) };
					if(ref[0] == \endpoint) { this.assert((CQMatrix.sourceNames ++ CQMatrix.destNames).includes(ref[1]), "advanced port exists: " ++ ref) };
				};
			};
		};
		model.advanced;
		this.assert(model.target.isNil, "advanced cannot enable info itself");
		model.enabled = true; model.advanced; model.page(1000);
		this.assertEquals(model.target, [\advanced, guide.count - 1], "advanced has its own upper boundary");
		this.assertEquals(model.guideClass, guide, "navigation retains the advanced guide identity");
		model.page(-1000);
		this.assertEquals(model.target, [\advanced, 0], "advanced back stops at its own fresh-session page");
		model.tour;
		this.assert(model.guideClass == CQInfoTour and: { model.playing }, "switching guides starts the selected path");
		model.shortcuts;
		this.assert(model.guideClass.isNil and: { model.inspecting }, "shortcuts leaves either playable guide");
	}
	test_depthLessonKeepsLocalAndGlobalControlsSeparate {
		var note = CQInfoTour.step(CQInfoTour.indexOf(\depth));
		this.assertEquals(note[\items].collect { |item| item[0] }, #[spaf, weights], "first band teaches local depth and cable weight");
		this.assertEquals(note[\detailItems].collect { |item| item[0] }, #[depth, eye], "second band teaches Master depth before the eye");
		this.assert(CQInfoTour.refs(CQInfoTour.indexOf(\depth)).includesEqual([\master, \patchDepth]), "Master depth picture locates its actual control");
		this.assertEquals(CQInfoTour.items(CQInfoTour.indexOf(\flip)).first[0], \flipInput, "flip input is introduced before reusing the triangle");
	}
	test_basicSymbolsReferenceAllMatchingParts {
		this.assertEquals(CQInkInfo.symbolRefs(\bird), [[\group, \bird, 0], [\group, \bird, 1]], "bird key locates both birds");
		this.assertEquals(CQInkInfo.symbolRefs(\crescents).size, 4, "both Input and Feedback pairs are located");
		this.assertEquals(CQInkInfo.symbolRefs(\output).collect { |ref| ref[1] }, CQMatrix.sourceNames, "output picture locates every source");
		this.assertEquals(CQInkInfo.symbolRefs(\input).collect { |ref| ref[1] }, CQMatrix.destNames, "input picture locates every destination");
		CQInfoTour.count.do { |page| CQInkInfo.tourItems(page).do { |item|
			this.assert(item[3].notEmpty, "every guide symbol has semantic references");
		} };
	}
	test_tourBuildsFromCaptureToTwoVoices {
		var refs = { |id| CQInfoTour.refs(CQInfoTour.indexOf(id)) };
		this.assertEquals(CQInfoTour.count, 23, "one bounded path replaces both separate guides");
		this.assert(refs.value(\connect).includesEqual([\endpoint, \tri1])
			and: { refs.value(\connect).includesEqual([\endpoint, \coco1_spaf]) }, "first patch names actual ports");
		this.assert(refs.value(\flip).includesEqual([\endpoint, \coco1_flip]), "direction trigger remains taught");
		this.assert(refs.value(\rightOpen).includesEqual([\param, \coco2Hold]), "right memory opens before loading its bird");
		this.assert(refs.value(\cross).includesEqual([\endpoint, \coco1])
			and: { refs.value(\crossListen).includesEqual([\param, \coco1Speed]) }, "cross-Coco cable is used in the next step");
		this.assert(refs.value(\record).includesEqual([\endpoint, \coco2_record]), "record gate follows the two-voice lesson");
		this.assert(CQInfoTour.step(CQInfoTour.indexOf(\moon))[\gesture].contains("random patch"), "moon is introduced before the sow action");
		this.assert(CQInfoTour.step(CQInfoTour.indexOf(\explore))[\body].contains("Organ"), "Organ and Drawer remain in the overview");
	}
	test_tideExplainsMemoryBeforeMovement {
		var rows = CQInfoPages.page([\group, \tide, 0])[\rows];
		this.assertEquals(rows.collect { |r| r[\label] },
			["store / recall", "open water focus", "blend", "breed"], "Tide follows the order of use");
		this.assertEquals(rows[1][\refs], [[\tideGate]], "focus picture locates the actual gill");
		this.assert(rows[3][\text].contains("Both memories stay intact"),
			"breeding changes the live patch, not either stored memory");
	}
	test_completePublicSurfaceHasAuthoredNotes {
		(CQGuiNightMap.reachableParamNames ++ CQGuiPanels.masterParamNames).do { |name|
			var note = CQInfoText.param(name);
			this.assert(note.notNil and: { note[\body].size > 30 }
				and: { note[\gesture].notEmpty }, "authored explanation and gesture for " ++ name);
		};
		(CQMatrix.sourceNames ++ CQMatrix.destNames).do { |name|
			this.assert(CQInfoText.endpoint(name).notNil, "authored endpoint explanation for " ++ name);
		};
		CQGuiPanels.masterItems.do { |name|
			this.assert(CQInfoText.master(name).notNil, "Master note for " ++ name);
		};
		this.assert(CQInfoText.topic([\water]).isNil, "empty water no longer has a card");
		this.assert(CQInfoText.topic([\tideGate]).notNil, "the actual tide gill still explains water focus");
	}
	test_directionAndRecordSemantics {
		var matrix = CQMatrix(nil), before, note;
		matrix.patch(\tri1, \coco1_record, -0.6); before = matrix.asArray;
		note = CQInfoText.topic([\cable, \tri1, \coco1_record], matrix);
		this.assert(note[\body].contains("Negative"), "cable polarity comes from the actual matrix");
		this.assert(note[\detail].contains("inverts"), "record is an inversion, not an unconditional record command");
		this.assert(CQInfoText.endpoint(\coco1_flip)[\body].contains("rising trigger")
			and: { CQInfoText.endpoint(\coco1_flip)[\body].contains("falling low leaves it") },
			"flip toggles on a rise and retains direction on a fall, distinct from record's gate");
		this.assertEquals(matrix.asArray, before, "reading notes never mutates a route");
		this.assert(CQInfoText.param(\coco1SlotX)[\body].contains("Coco 2 into Coco 1"),
			"left bridge note states the receiving direction");
		this.assert(CQInfoText.param(\coco2SlotX)[\body].contains("Coco 1 into Coco 2"),
			"right bridge note states the opposite receiving direction");
	}
	test_cardStaysInsideTheStage {
		[Point(1280, 570), Point(1528, 730)].do { |size|
			[Point(0, 0), Point(size.x, 0), size, Point(0, size.y), size * 0.5].do { |point|
				[false, true].do { |pinned|
					var rect = CQGuiInfo.place(size, point, 370, 400, pinned);
					this.assert(Rect(0, 0, size.x, size.y).contains(rect),
						"edge and pinned cards stay within Night; never reach Master");
				};
			};
		};
	}
}
