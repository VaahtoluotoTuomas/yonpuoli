// CQMidi16nTest — the 16n layer's logic with no MIDI device and no server:
// handleCC is driven directly (CQState with graph = nil), MIDI wiring stays
// off via enabled: false. Covers the CC→param mapping, linked slots, the
// jump/pickup takeover and external-change disengagement (SPEC §6.1, A2).

CQMidi16nTest : UnitTest {
	var state, midi;

	setUp {
		CQSpec.postWarnings = false;
		CQMidi16n.postWarnings = false;
		state = CQState(nil);
		midi = CQMidi16n(state,
			(enabled: false, ccBase: 32, chan: 0, takeover: \jump));
	}

	tearDown {
		midi.free;
		CQSpec.postWarnings = true;
		CQMidi16n.postWarnings = true;
	}

	test_slotTableMatchesSpec {
		this.assertEquals(CQSpec.map16n.size, 16, "16 fader slots (SPEC §6.1)");
		this.assertEquals(CQSpec.map16n[10], [\qFreq], "F11 = qFreq");
		this.assertEquals(CQSpec.map16n[12], [\preADrive, \preBDrive],
			"F13 = linked preamp drives");
		this.assertEquals(CQSpec.map16n[13], [\coco1SlotQ, \coco2SlotQ],
			"F14 = the linked ring audio gate");
		this.assertEquals(CQSpec.map16n[14], [\patchDepth], "F15 = patchDepth");
		this.assertEquals(CQSpec.map16n[15], [\masterVol], "F16 = masterVol");
		CQSpec.map16n.do { |slot|
			slot.do { |p|
				this.assert(CQSpec.includes(p),
					"slot param '%' exists in CQSpec".format(p));
			};
		};
	}

	// SPEC §6.1's channel-strip law as structure, not convention: the same
	// finger position must mean the same thing on either voice, and Level ends
	// the strip because it is its output stage.
	test_16nCocoStripsAreIdenticalAndLevelIsLast {
		// backslashes are required: bare `In` in a literal array parses as the
		// In UGen class, not a Symbol (same reason CQGuiNightMap writes \Src)
		var order = #[\In, \Speed, \Spaf, \Fb, \Level];
		2.do { |coco|
			order.do { |suffix, i|
				var fader = (coco * 5) + i;
				var expected = ("coco" ++ (coco + 1) ++ suffix).asSymbol;
				this.assertEquals(CQSpec.map16n[fader], [expected],
					"F% = %".format(fader + 1, expected));
			};
		};
		this.assertEquals(CQSpec.map16n[4].first, \coco1Level,
			"Level ends strip 1, mirroring a mixer's channel fader");
		this.assertEquals(CQSpec.map16n[9].first, \coco2Level,
			"Level ends strip 2 at the same offset");
	}

	test_jumpMapsFullRange {
		midi.handleCC(32, 127);
		this.assertFloatEquals(state.get(\coco1In), 2.0, "F1 top = coco1In max");
		midi.handleCC(32, 0);
		this.assertFloatEquals(state.get(\coco1In), 0.0, "F1 bottom = coco1In min");
		midi.handleCC(47, 127);
		this.assertFloatEquals(state.get(\masterVol), 0.0, "F16 top = 0 dB");
		midi.handleCC(47, 0);
		this.assertFloatEquals(state.get(\masterVol), -60.0, "F16 bottom = -60 dB");
	}

	test_linkedSlotsWriteBoth {
		midi.handleCC(44, 127);
		this.assertFloatEquals(state.get(\preADrive), 40.0, "F13 writes preADrive");
		this.assertFloatEquals(state.get(\preBDrive), 40.0, "F13 writes preBDrive too");
	}

	// F14/CC 45 carried the retired spaceMix. It now opens the Quantussy audio
	// gate on both Cocos -- one concept, so linking is right here, unlike the
	// two Level faders whose whole purpose is balancing them against each other.
	test_ringGateSlotWritesBothCocos {
		this.assertFloatEquals(state.get(\coco1SlotQ), 0.0,
			"the ring gate is shut at boot (SPEC §5.4)");
		midi.handleCC(45, 127);
		this.assertFloatEquals(state.get(\coco1SlotQ), 1.5, "F14 opens coco1SlotQ");
		this.assertFloatEquals(state.get(\coco2SlotQ), 1.5, "F14 opens coco2SlotQ too");
		midi.handleCC(45, 0);
		this.assertFloatEquals(state.get(\coco1SlotQ), 0.0, "and shuts it again");
		this.assert(CQSpec.includes(\spaceMix).not,
			"the retired Space route cannot return through this slot");
	}

	test_everySlotIsAssigned {
		CQSpec.map16n.do { |slot, i|
			this.assert(slot.size > 0,
				"F% carries a parameter: 16 faders, 74 params, none wasted"
					.format(i + 1));
		};
	}

	test_outOfRangeCCIgnored {
		var before = state.asEvent;
		this.assertNoException({
			midi.handleCC(31, 127);
			midi.handleCC(48, 127);
		}, "CCs outside ccBase..ccBase+15 never throw");
		this.assertEquals(state.asEvent, before, "and they change nothing");
	}

	test_pickupWaitsForCross {
		midi.takeover = \pickup;
		state.set(\coco1In, 0.8);              // norm 0.4 target
		midi.handleCC(32, 127);                // far touch: above the target
		this.assertFloatEquals(state.get(\coco1In), 0.8,
			"a far fader is ignored until it crosses the value");
		midi.handleCC(32, 30);                 // 0.236 — moved through 0.4
		this.assertFloatEquals(state.get(\coco1In), CQSpec.map(\coco1In, 30 / 127),
			"crossing the value engages the fader and writes");
		midi.handleCC(32, 100);
		this.assertFloatEquals(state.get(\coco1In), CQSpec.map(\coco1In, 100 / 127),
			"an engaged fader keeps writing");
	}

	test_pickupEngagesNearTarget {
		midi.takeover = \pickup;
		state.set(\coco1In, 1.0);              // norm 0.5
		midi.handleCC(32, 64);                 // 0.504 — within one CC step
		this.assertFloatEquals(state.get(\coco1In), CQSpec.map(\coco1In, 64 / 127),
			"landing within ±1 CC step of the value engages");
	}

	test_externalChangeDisengages {
		midi.takeover = \pickup;
		state.set(\coco1In, 0.8);
		midi.handleCC(32, 127);
		midi.handleCC(32, 30);                 // engaged now
		state.set(\coco1In, 0.2);              // external write (e.g. recall)
		midi.handleCC(32, 90);                 // 0.709: target 0.1, no cross from 0.236
		this.assertFloatEquals(state.get(\coco1In), 0.2,
			"an external change disengages the fader again");
	}

	test_takeoverSetterValidates {
		midi.takeover = \pickup;
		midi.takeover = \bogus;
		this.assertEquals(midi.takeover, \pickup, "bad takeover mode is ignored");
	}
}
