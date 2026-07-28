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
		this.assertEquals(CQSpec.map16n[0], [\coco1In], "F1 = coco1In");
		this.assertEquals(CQSpec.map16n[8], [\qFreq], "F9 = qFreq");
		this.assertEquals(CQSpec.map16n[10], [\preADrive, \preBDrive],
			"F11 = linked preamp drives");
		this.assertEquals(CQSpec.map16n[13], [], "F14 is intentionally unassigned");
		this.assertEquals(CQSpec.map16n[15], [\masterVol], "F16 = masterVol");
		CQSpec.map16n.do { |slot|
			slot.do { |p|
				this.assert(CQSpec.includes(p),
					"slot param '%' exists in CQSpec".format(p));
			};
		};
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
		midi.handleCC(42, 127);
		this.assertFloatEquals(state.get(\preADrive), 40.0, "F11 writes preADrive");
		this.assertFloatEquals(state.get(\preBDrive), 40.0, "F11 writes preBDrive too");
	}

	test_retiredSpaceSlotIsInert {
		var before = state.asEvent;
		this.assertNoException({ midi.handleCC(45, 127) },
			"the retired F14 slot accepts physical movement safely");
		midi.takeover = \pickup;
		this.assertNoException({
			midi.handleCC(45, 0);
			midi.handleCC(45, 64);
		}, "the empty slot is safe in pickup mode too");
		this.assertEquals(state.asEvent, before,
			"F14 cannot activate a hidden Space route or change another parameter");
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
