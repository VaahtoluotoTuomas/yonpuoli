// CQStateTest — the state funnel with no server (graph = nil): clamps, stores,
// notifies dependants, never applies garbage raw, never throws.

CQStateTest : UnitTest {
	var state;

	setUp {
		CQSpec.postWarnings = false;
		state = CQState(nil);
	}

	tearDown {
		CQSpec.postWarnings = true;
	}

	test_setClampsAndStores {
		state.set(\coco1Fb, 99);
		this.assertEquals(state.get(\coco1Fb), 1.25, "set clamps through CQSpec");
	}

	test_defaultsPresent {
		this.assertEquals(state.get(\qFreq), 4.0, "defaults preloaded");
		this.assertEquals(state.get(\micGain), 0.0, "mic starts muted");
		this.assertEquals(state.get(\coco2Hold), 1, "power-on loop-locked");
	}

	test_garbageNeverRaw {
		state.set(\coco1Fb, 0.0 / 0.0);
		this.assertEquals(state.get(\coco1Fb), CQSpec.default(\coco1Fb),
			"NaN falls back to the default, never stored raw");
	}

	test_unknownParamSafe {
		this.assertNoException({ state.set(\bogus, 1) }, "unknown param never throws");
		this.assert(state.get(\bogus).isNil, "unknown param stays absent");
	}

	test_dependantsNotified {
		var got, ctl;
		ctl = SimpleController(state);
		ctl.put(\param, { |obj, what, name, val| got = [name, val] });
		state.set(\qChaos, 0.5);
		ctl.remove;
		this.assertEquals(got, [\qChaos, 0.5], "dependants hear clamped values");
	}

	test_asEventCarriesEverything {
		var e;
		state.set(\coco1Fb, 1.0);
		e = state.asEvent;
		this.assertEquals(e[\coco1Fb], 1.0, "asEvent reflects current values");
		this.assertEquals(e.size, 79, "asEvent carries every parameter");
	}
}
