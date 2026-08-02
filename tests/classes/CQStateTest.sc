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

	// ---- SOAK-1 finding A: the bulk door ----

	test_setAllClampsEveryValueThroughTheSameFunnel {
		var applied = state.setAll((coco1Fb: 99, micGain: -5, qFreq: 4.5));
		this.assertEquals(state.get(\coco1Fb), 1.25,
			"a bulk write clamps exactly as set does");
		this.assertEquals(state.get(\micGain), 0.0, "and clamps the low end too");
		this.assertEquals(state.get(\qFreq), 4.5, "a valid value lands unchanged");
		this.assertEquals(applied[\coco1Fb], 1.25,
			"the answer reports what was actually applied, not what was asked");
	}

	test_setAllAnnouncesOnceForTheWholeWrite {
		var events = List.new, singles = 0, ctl = SimpleController(state);
		ctl.put(\params, { |obj, what, event| events.add(event) });
		ctl.put(\param, { singles = singles + 1 });
		state.setAll((coco1Fb: 0.5, coco2Fb: 0.5, qFreq: 3, coco1Speed: 20000));
		this.assertEquals(events.size, 1,
			"one write, one announcement — this is the whole fix");
		this.assertEquals(singles, 0,
			"and it never also fires the per-parameter stream");
		this.assertEquals(events[0].size, 4, "the event carries every value written");
		this.assertEquals(events[0][\qFreq], 3.0, "clamped, as applied");
		ctl.remove;
	}

	test_setAllSkipsUnknownNamesWithoutThrowing {
		var events = List.new, ctl = SimpleController(state);
		ctl.put(\params, { |obj, what, event| events.add(event) });
		this.assertNoException({ state.setAll((bogus: 1, qFreq: 5)) },
			"an unknown name never throws");
		this.assert(state.get(\bogus).isNil, "and never enters the store");
		this.assertEquals(events[0].size, 1, "nor the announcement");
		this.assertEquals(state.get(\qFreq), 5.0, "the valid names still land");
		ctl.remove;
	}

	test_setAllOfNothingSaysNothing {
		var events = List.new, ctl = SimpleController(state);
		ctl.put(\params, { |obj, what, event| events.add(event) });
		state.setAll(Event.new);
		state.setAll(nil);
		this.assert(events.isEmpty,
			"an empty write is not an event — a state is not a stream");
		ctl.remove;
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
		this.assertEquals(e.size, 74, "asEvent carries every parameter");
	}

	// Playability audit (PLAN item 8): storage and GUI coverage are not enough.
	// Every parameter in a mic-enabled instrument must route to a control that
	// actually exists in its owning production SynthDef. This catches both an
	// omitted route (a dead knob) and a misspelled synth argument (a knob that
	// moves in state/GUI but is ignored by scsynth).
	test_everyParameterTerminatesAtARealSynthDefControl {
		var cfg = CQConfig.validate((micChan: 0));
		var graph = CQTestGraph(CQTestServer(), cfg);
		var routedState = CQState(graph);
		var manifest = routedState.routeManifest;
		var defs = IdentityDictionary.new;
		defs[\input] = CQSynthDefs.input(cfg);
		defs[\master] = CQSynthDefs.master(cfg);
		defs[\quantussy] = CQSynthDefs.quantussy(cfg);
		defs[\coco1] = CQSynthDefs.coco(cfg);
		defs[\coco2] = defs[\coco1];
		defs[\deckA] = CQSynthDefs.deck(cfg);
		defs[\deckB] = defs[\deckA];

		this.assertEquals(manifest.size, CQSpec.names.size,
			"a mic-enabled graph routes all 74 CQSpec parameters");
		CQSpec.names.do { |name|
			var route = manifest[name];
			var pairs;
			this.assert(route.notNil, "parameter % has a DSP route".format(name));
			if(route.notNil) {
				pairs = if(route[0].isKindOf(Symbol)) { [route] } { route };
				pairs.do { |pair|
					var def = defs[pair[0]];
					var controls = def !? {
						def.allControlNames.collect { |control| control.name.asSymbol }
					};
					this.assert(def.notNil,
						"% route names a production synth (%)".format(name, pair[0]));
					this.assert(controls.notNil and: { controls.includes(pair[1]) },
						"% reaches real %.% control".format(name, pair[0], pair[1]));
				};
			};
		};
	}

	test_micGainIsHonestlyDormantWithoutConfiguredHardware {
		var cfg = CQConfig.validate((micChan: nil));
		var graph = CQTestGraph(CQTestServer(), cfg);
		this.assert(CQState(graph).routeManifest[\micGain].isNil,
			"micGain has no fake DSP route when Audio I/O says mic off");
	}
}
