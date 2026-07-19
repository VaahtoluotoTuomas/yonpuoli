// CQEventTest — the \cq pattern event type (SPEC §6.4, ARCH §11 stub). Verifies
// registration and that dispatch degrades to a silent no-op with the instrument
// down (a pattern must survive a free/reboot without flooding warnings). The
// actual hold/skip/spesal/deck firing is verified by ear in KUUNTELU 7, where the
// facade is booted. This suite stays server-free like the rest of run_all.

CQEventTest : UnitTest {

	test_registersEventType {
		Cocoquantus.prRegisterCqEvent;
		this.assert(Event.eventTypes[\cq].notNil,
			"\\cq is a registered event type (SPEC §6.4)");
	}

	test_dispatchShapesNeverThrow {
		var ok = true;
		Cocoquantus.prRegisterCqEvent;
		try {
			[
				(action: \hold, coco: 1),
				(action: \hold, coco: 2, val: 1),
				(action: \flip, coco: 2, val: 0),
				(action: \skip, coco: 1),
				(action: \spesal, castle: 3),
				(action: \spesal, castle: 3, state: 5),
				(action: \set, param: \coco1Fb, value: 1.1),
				(action: \patch, src: \tri1, dest: \coco1_spaf, weight: 0.5),
				(action: \deck, deck: \a, do: \next),
				(action: \deck, deck: \b, do: \random),
				(action: \shake, amount: 0.4, seed: 1234),
				(action: \bogus),          // unknown action
				(action: \rest),           // Rest / no-op
				(action: nil),             // no action key
				()                         // empty event
			].do { |ev| Cocoquantus.prCqAction(ev) };
		} { |err| ok = false; err.errorString.postln };
		this.assert(ok,
			"prCqAction handles every action shape without error when unbooted "
			"(SPEC §6.4 stub)");
	}
}
