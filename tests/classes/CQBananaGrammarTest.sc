// GUI_SPEC §4 behavior contract for the serverless banana grammar.

CQBananaGrammarTest : UnitTest {
	var matrix, grammar;

	setUp {
		CQMatrix.postWarnings = false;
		matrix = CQMatrix(nil);
		grammar = CQBananaGrammar(matrix);
	}

	tearDown {
		grammar !? { grammar.free };
		CQMatrix.postWarnings = true;
	}

	prAssertGestureExit { |selector, label|
		var before = matrix.fullMatrix;
		grammar.startSource(\tri2, 1);
		grammar.updateCandidate(\coco1_spaf, 10);
		grammar.perform(selector);
		this.assertEquals(matrix.fullMatrix, before,
			label ++ " restores exact matrix equality");
		this.assert(grammar.isInHand.not, label ++ " drops the banana in hand");
	}

	test_candidateIsSilentAndDropDistanceNeverAttenuates {
		var before = matrix.fullMatrix;
		grammar.startSource(\tri1, 1);
		grammar.updateCandidate(\coco1_spaf, 27);
		this.assertEquals(matrix.fullMatrix, before,
			"entering the magnetic socket radius never writes or auditions DSP");
		this.assertEquals(grammar.candidateWeight,
			CQBananaGrammar.defaultHandStrength,
			"candidate distance does not attenuate the armed cable");
		grammar.updateCandidate(\coco1_spaf, grammar.dropRadius - 1);
		this.assertEquals(matrix.fullMatrix, before,
			"the outermost legal candidate remains view-only");
		grammar.releaseCandidate;
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf),
			CQBananaGrammar.defaultHandStrength,
			"release plants the same fixed armed weight at every catch distance");
		this.assertEquals(grammar.undoCount, 1, "plant is one undo operation");
		grammar.undo;
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.0,
			"Ctrl+Z restores the pre-plant matrix");
	}

	test_hoverCableIsStableViewOnlyState {
		var before = matrix.fullMatrix;
		grammar.hoverCable = (src: \tri1, dest: \coco1_spaf, weight: 0.5);
		this.assert(grammar.isCableHovered(\tri1, \coco1_spaf),
			"hover identifies the cable targeted by mouse gestures");
		grammar.hoverCable = nil;
		this.assert(grammar.isCableHovered(\tri1, \coco1_spaf).not,
			"leaving the cable clears its targeting feedback");
		this.assertEquals(matrix.fullMatrix, before,
			"hover feedback never writes the matrix");
	}

	test_everyGuiLifecycleExitRevertsTheIntegratedGesture {
		#[viewToggle, windowClose, cmdPeriod, panic, free].do { |selector|
			this.prAssertGestureExit(selector, selector.asString)
		};
	}

	test_inversoAndShiftKeepSourceInHand {
		grammar.startSource(\cas2, -1);
		grammar.updateCandidate(\coco1_inVca, 0);
		grammar.releaseCandidate(true);
		this.assertEquals(matrix.getPatch(\cas2, \coco1_inVca), -0.60,
			"right-button source plants inverso");
		this.assert(grammar.isSourceInHand(\cas2),
			"Shift-release keeps the source in click-click mode");
		grammar.updateCandidate(\coco2_inVca, 0);
		grammar.releaseCandidate(false);
		this.assertEquals(matrix.getPatch(\cas2, \coco2_inVca), -0.60,
			"the kept source plants again");
		this.assert(grammar.isInHand.not, "ordinary release drops the kept source");
	}

	test_moveIsOneBatchAndUndoRestoresBothCells {
		var notifications = 0, ctl;
		matrix.patch(\tri3, \coco1_spaf, 0.42);
		ctl = SimpleController(matrix);
		ctl.put(\matrix, { notifications = notifications + 1 });
		grammar.grab(\tri3, \coco1_spaf);
		grammar.updateCandidate(\coco2_spaf, 20);
		this.assertEquals(matrix.getPatch(\tri3, \coco1_spaf), 0.42,
			"a lifted route stays live at its origin until the binary drop");
		this.assertEquals(matrix.getPatch(\tri3, \coco2_spaf), 0.0,
			"hovering the new socket never previews a second route");
		grammar.releaseCandidate;
		this.assertEquals(matrix.getPatch(\tri3, \coco1_spaf), 0.0,
			"move clears the original socket");
		this.assertEquals(matrix.getPatch(\tri3, \coco2_spaf), 0.42,
			"move carries the planted weight unchanged");
		this.assertEquals(notifications, 1, "move commits as one batch notification");
		grammar.undo;
		this.assertEquals(matrix.getPatch(\tri3, \coco1_spaf), 0.42,
			"move undo restores the origin");
		this.assertEquals(matrix.getPatch(\tri3, \coco2_spaf), 0.0,
			"move undo clears the destination");
		ctl.remove;
	}

	test_yankAndInstantRipAreUndoable {
		matrix.patch(\envA, \castle1_fm, 0.3);
		grammar.grab(\envA, \castle1_fm);
		grammar.releaseVoid;
		this.assertEquals(matrix.getPatch(\envA, \castle1_fm), 0.0,
			"releasing a grabbed banana in the void rips it");
		grammar.undo;
		this.assertEquals(matrix.getPatch(\envA, \castle1_fm), 0.3,
			"yank rip is undoable");
		grammar.instantRip(\envA, \castle1_fm);
		this.assertEquals(matrix.getPatch(\envA, \castle1_fm), 0.0,
			"right-click rips immediately");
	}

	test_polarityAndWheelPreserveTheWeightLaws {
		matrix.patch(\org1, \deckA_rate, 0.07);
		grammar.polarityFlip(\org1, \deckA_rate);
		this.assertEquals(matrix.getPatch(\org1, \deckA_rate), -0.07,
			"Alt-click flips polarity without changing magnitude");
		grammar.wheel(\org1, \deckA_rate, -1, false);
		this.assertEquals(matrix.getPatch(\org1, \deckA_rate), -0.05,
			"coarse wheel respects the 0.05 planted floor");
		grammar.wheel(\org1, \deckA_rate, 1, true);
		this.assert((matrix.getPatch(\org1, \deckA_rate) + 0.06).abs < 0.0001,
			"Ctrl-wheel nudges magnitude by 0.01 and preserves sign");
	}

	test_wheelInHandArmsAQuietInitialWeight {
		var before = matrix.fullMatrix;
		grammar.startSource(\org1, 1);
		grammar.updateCandidate(\coco1_spaf, grammar.dropRadius - 1);
		4.do { grammar.wheelHand(-1, false) };
		this.assert((grammar.handWeight - 0.40).abs < 0.0001,
			"four coarse wheel steps arm 0.40 before the cable lands");
		grammar.wheelHand(1, true);
		this.assert((grammar.handWeight - 0.41).abs < 0.0001,
			"Ctrl-wheel gives the held cable the same 0.01 fine step");
		this.assertEquals(matrix.fullMatrix, before,
			"arming a small starting value creates no 0.60 audio transient");
		grammar.releaseCandidate;
		this.assert((matrix.getPatch(\org1, \coco1_spaf) - 0.41).abs < 0.0001,
			"the first and only DSP write uses the selected held-cable weight");
	}

	test_harvestUsesCableSamplesAndOneUndoEntry {
		var cables, notifications = 0, ctl;
		matrix.patch(\tri1, \coco1_spaf, 0.4);
		matrix.patch(\cas1, \coco2_spaf, -0.3);
		cables = [
			(src: \tri1, dest: \coco1_spaf, points: [10@10, 20@20, 30@30]),
			(src: \cas1, dest: \coco2_spaf, points: [70@70, 80@80, 90@90])
		];
		ctl = SimpleController(matrix);
		ctl.put(\matrix, { notifications = notifications + 1 });
		grammar.beginHarvest(0@0);
		grammar.updateHarvest(40@0, cables);
		grammar.updateHarvest(40@40, cables);
		grammar.updateHarvest(0@40, cables);
		this.assert(grammar.isHarvested(\tri1, \coco1_spaf),
			"lasso preview marks a cable when any sampled point is inside");
		this.assert(grammar.isHarvested(\cas1, \coco2_spaf).not,
			"lasso preview leaves outside cables alone");
		grammar.finishHarvest(cables);
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.0,
			"harvest rips the entire catch");
		this.assertEquals(matrix.getPatch(\cas1, \coco2_spaf), -0.3,
			"harvest preserves cables outside the region");
		this.assertEquals(notifications, 1,
			"harvest is one batch notification and one operation");
		this.assertEquals(grammar.undoCount, 1, "harvest adds one undo entry");
		grammar.undo;
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.4,
			"one undo restores the whole harvest catch");
		ctl.remove;
	}

	test_guiMatrixActionIsOneUndoableOperation {
		var before = matrix.fullMatrix;
		grammar.performUndoable({ matrix.shake(0.3, 1234) });
		this.assert(matrix.fullMatrix != before,
			"GUI SHAKE changes the matrix through its existing funnel");
		this.assertEquals(grammar.undoCount, 1,
			"GUI SHAKE contributes one banana-grammar undo entry");
		grammar.undo;
		this.assertEquals(matrix.fullMatrix, before,
			"one Ctrl+Z restores the complete pre-SHAKE matrix");
	}

	test_shortHarvestAndEscapeAreNoOps {
		var before = matrix.fullMatrix;
		grammar.beginHarvest(0@0);
		grammar.updateHarvest(4@3, []);
		grammar.finishHarvest([]);
		this.assertEquals(matrix.fullMatrix, before,
			"right drag shorter than 8 px is a silent no-op");
		this.assertEquals(grammar.undoCount, 0, "empty short lasso adds no undo");
		grammar.beginHarvest(0@0);
		grammar.updateHarvest(20@20, []);
		grammar.escape;
		this.assert(grammar.isHarvesting.not, "Esc cancels an open harvest lasso");
	}

	test_soloLightIsViewOnlyAndEscReleasesIt {
		var before = matrix.fullMatrix;
		grammar.toggleSoloEndpoint(\jack, \tri1);
		this.assert(grammar.isCableDim(\tri1, \coco1_spaf).not,
			"solo jack keeps its own bananas bright");
		this.assert(grammar.isCableDim(\cas1, \coco1_spaf),
			"solo jack dims unrelated bananas");
		grammar.escape;
		this.assert(grammar.isCableDim(\cas1, \coco1_spaf).not,
			"Esc releases solo-light");
		this.assertEquals(matrix.fullMatrix, before, "solo-light never writes the matrix");
	}

	test_undoRingHonorsConfiguredDepth {
		grammar = CQBananaGrammar(matrix, 0.5, 80, 2);
		grammar.instantRip(\tri1, \coco1_spaf);
		matrix.patch(\tri1, \coco1_spaf, 0.2);
		grammar.polarityFlip(\tri1, \coco1_spaf);
		grammar.polarityFlip(\tri1, \coco1_spaf);
		grammar.polarityFlip(\tri1, \coco1_spaf);
		this.assertEquals(grammar.undoCount, 2, "undo ring drops entries beyond its depth");
		3.do { grammar.undo };
		this.assertEquals(grammar.redoCount, 2, "redo retains only the bounded undo history");
		3.do { grammar.redo };
		this.assertEquals(grammar.undoCount, 2, "redo returns entries to the same bounded history");
	}

	test_redoRestoresMovesAndANewEditReplacesTheBranch {
		var before, moved;
		matrix.patch(\tri1, \coco1_spaf, 0.42);
		before = matrix.fullMatrix;
		grammar.grab(\tri1, \coco1_spaf);
		grammar.updateCandidate(\coco2_spaf, 0); grammar.releaseCandidate;
		moved = matrix.fullMatrix;
		grammar.undo;
		this.assertEquals(matrix.fullMatrix, before, "undo restores both ends");
		grammar.instantRip(\tri5, \coco2_spaf);
		this.assertEquals(grammar.redoCount, 1, "an empty rip does not discard redo");
		grammar.startSource(\cas1, 1);
		grammar.redo;
		this.assertEquals(matrix.fullMatrix, moved, "redo restores the exact moved route and weight");
		this.assert(grammar.isInHand.not, "redo cancels an uncommitted held cable");
		grammar.undo;
		grammar.polarityFlip(\tri1, \coco1_spaf);
		before = matrix.fullMatrix;
		this.assertEquals(grammar.redoCount, 0, "a new committed edit discards the redo branch");
		grammar.redo;
		this.assertEquals(matrix.fullMatrix, before, "redo cannot resurrect the discarded branch");
	}

	test_guiShakeUndoRestoresItsMatrixAndPreparedControlsTogether {
		var state = CQState(nil), beforeMatrix, beforeParams, plan;
		var stableName, changedNames, handValue, afterMatrix, afterParams;
		grammar.free;
		state.setAll((patchDepth: 0.2, coco1Level: 0.1, coco2Level: 0.2,
			coco1Hold: 0, coco2Hold: 0));
		grammar = CQBananaGrammar(matrix, state: state);
		beforeMatrix = matrix.fullMatrix;
		beforeParams = Event.new;
		CQMatrix.shakeParamNames.do { |name| beforeParams[name] = state.get(name) };
		grammar.performUndoableState(CQMatrix.shakeParamNames, {
			matrix.shake(0.3, seed: 1, context: state.asEvent);
			state.setAll(matrix.lastShakePlan[\params]);
		});
		plan = matrix.lastShakePlan.deepCopy;
		afterMatrix = matrix.fullMatrix; afterParams = state.asEvent.copy;
		changedNames = plan[\params].keys.select { |name|
			beforeParams[name] != state.get(name)
		};
		this.assert(matrix.fullMatrix != beforeMatrix,
			"an ecological GUI shake replaces the matrix");
		this.assert(state.get(\patchDepth) >= 0.9
			and: { state.get(\coco1Level) >= 0.90 }
			and: { state.get(\coco2Level) >= 0.90 },
			"its safe musical preparation reaches the same state funnel");
		this.assertEquals(grammar.undoCount, 1,
			"the complete ecological throw is one undo operation");
		stableName = #[coco1Organ, coco2Organ, coco1SlotX, coco2SlotX].detect { |name|
			beforeParams[name] == state.get(name)
		};
		handValue = 0.27;
		this.assert(stableName.notNil,
			"the plan may name a stable value without pretending it changed");
		state.set(stableName, handValue);
		grammar.undo;
		this.assertEquals(matrix.fullMatrix, beforeMatrix,
			"one undo restores the pre-shake matrix");
		changedNames.do { |name|
			this.assertEquals(state.get(name), beforeParams[name],
				"one undo restores prepared musical control %".format(name));
		};
		this.assertEquals(state.get(stableName), CQSpec.clamp(stableName, handValue),
			"undo leaves a later hand edit to an unchanged planned control intact");
		state.set(\masterVol, -19);
		grammar.redo;
		this.assertEquals(matrix.fullMatrix, afterMatrix, "redo restores the exact sow, without randomizing again");
		changedNames.do { |name|
			this.assertEquals(state.get(name), afterParams[name], "redo restores prepared control " ++ name);
		};
		this.assertEquals(state.get(stableName), CQSpec.clamp(stableName, handValue), "redo preserves unchanged planned controls");
		this.assertEquals(state.get(\masterVol), CQSpec.clamp(\masterVol, -19), "redo preserves unrelated later hand edits");
	}

	test_visibleSocketCatchesTheFullGateCapablePlant {
		var before = matrix.fullMatrix;
		grammar.startSource(\tri1, 1);
		grammar.updateCandidate(\coco1_flip, grammar.dropRadius - 1);
		this.assertEquals(matrix.fullMatrix, before,
			"even the magnetic edge remains silent before release");
		this.assertEquals(grammar.candidateWeight,
			CQBananaGrammar.defaultHandStrength,
			"the magnetic edge arms full first-touch strength");
		grammar.releaseCandidate;
		this.assert(matrix.getPatch(\tri1, \coco1_flip)
			> CQSynthDefs.gateOnThreshold,
			"a normally seated raw-triangle cable can cross flip/skip/spesal");
	}

	test_defaultPlantHasSchmittHeadroom {
		this.assertEquals(CQBananaGrammar.defaultHandStrength, 0.60,
			"PLAY-2 keeps a slightly lighter continuous first touch");
		this.assert(CQBananaGrammar.defaultHandStrength
			> CQSynthDefs.gateOnThreshold,
			"a default cable crosses, rather than merely touches, the gate threshold");
	}
}
