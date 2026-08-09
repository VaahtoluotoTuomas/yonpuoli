// CQMidi16n — the 16n faderbank layer (SPEC §6.1). One MIDIdef over the 16
// configured CCs; every write goes through the CQState funnel (0..127 →
// normalized → CQSpec.map → set), so values are clamped and server-lagged
// ≥ 30 ms — the spec'd fader smoothing. Fader→param slots come from
// CQSpec.map16n (linked slots write two params, e.g. preADrive + preBDrive).
// Takeover \jump writes immediately; \pickup re-engages a fader only after it
// crosses the parameter's current position (e.g. after a snapshot recall —
// any external write disengages that fader). MIDI is optional: absence of the
// 16n (or of MIDI entirely) degrades to a posted notice, never an error
// (ARCH §1.4); handleCC is public so logic tests run without a device.

CQMidi16n {
	classvar <>postWarnings = true;
	var state, <cfg;
	var <takeover;
	var engaged, lastNorm;   // per fader: pickup engagement + last seen position
	var writing = false;     // reentrancy: our own funnel writes must not disengage
	var watcher;             // external param changes disengage their fader (pickup)
	var paramFader;          // paramName -> fader index, for the watcher
	var <midiOK = false;

	*new { |state, cfg|
		^super.newCopyArgs(state, cfg).prInit
	}

	prInit {
		takeover = if(#[jump, pickup].includes(cfg[\takeover])) { cfg[\takeover] } { \jump };
		engaged = false ! 16;
		lastNorm = nil ! 16;
		paramFader = IdentityDictionary.new;
		CQSpec.map16n.do { |params, i|
			params.do { |p| paramFader[p] = i };
		};
		watcher = SimpleController(state);
		watcher.put(\param, { |obj, what, name, val|
			if(writing.not) {
				paramFader[name] !? { |i| engaged[i] = false };
			};
		});
		// A morph's dense frame arrives as one event, and takeover must break for
		// every fader it moved — otherwise a fader would keep writing values the
		// snapshot has since overwritten (SOAK-1 finding A).
		watcher.put(\params, { |obj, what, event|
			if(writing.not) {
				event.keysDo { |name|
					paramFader[name] !? { |i| engaged[i] = false };
				};
			};
		});
		if(cfg[\enabled] == true) { this.prInitMidi };
	}

	// one incoming CC through the funnel — the only write path of this layer
	handleCC { |ccNum, val|
		var fader = ccNum - cfg[\ccBase];
		var params, norm, target;
		if(fader < 0 or: { fader > 15 }) { ^this };
		params = CQSpec.map16n[fader];
		norm = (val / 127).clip(0, 1);
		if((takeover == \pickup) and: { engaged[fader].not }) {
			// linked slots track their first param (the pair moves together anyway)
			target = CQSpec.unmap(params[0], state.get(params[0])) ? 0;
			if(this.prCrossed(lastNorm[fader], norm, target)) { engaged[fader] = true };
		};
		if((takeover == \jump) or: { engaged[fader] }) {
			writing = true;
			params.do { |p| state.set(p, CQSpec.map(p, norm)) };
			writing = false;
			engaged[fader] = true;
		};
		lastNorm[fader] = norm;
	}

	takeover_ { |mode|
		if(#[jump, pickup].includes(mode).not) {
			this.class.prWarn("takeover must be \\jump or \\pickup (got %)".format(mode));
			^this
		};
		takeover = mode;
		if(mode == \pickup) { engaged = false ! 16 };
	}

	// re-scan after (re)plugging the 16n mid-session — portmidi on Windows does
	// not hot-plug by itself; MIDIdefs survive the client re-init untouched
	rescan {
		try {
			if(MIDIClient.initialized) { MIDIClient.disposeClient };
		} { |err|
			this.class.prWarn("MIDI dispose failed: %".format(err.errorString));
		};
		this.prInitMidi;
	}

	found16n {
		if(MIDIClient.initialized.not) { ^false };
		^MIDIClient.sources.any { |src|
			(src.device.asString ++ " " ++ src.name.asString).toLower.contains("16n")
		}
	}

	free {
		watcher !? { watcher.remove };
		watcher = nil;
		MIDIdef.all[\cq_16n] !? { |d| d.free };
	}

	// ---- private ----

	prInitMidi {
		try {
			// chan nil = any channel (default): the atovproject 16n sends each
			// fader on its own MIDI channel, CCs 32-47 — the CC number alone
			// identifies the fader. An integer chan pins single-channel firmware.
			var chanDesc = if(cfg[\chan].isNil) { "any chan" } {
				"chan " ++ (cfg[\chan] + 1) };
			if(MIDIClient.initialized.not) { MIDIClient.init };   // guarded (CLAUDE.md)
			MIDIIn.connectAll;
			MIDIdef.cc(\cq_16n, { |val, num|
				this.handleCC(num, val);
			}, (cfg[\ccBase]..(cfg[\ccBase] + 15)), cfg[\chan]);
			midiOK = true;
			if(this.found16n) {
				"CQMidi16n: 16n found — CC %..% (%) live (takeover: %)."
					.format(cfg[\ccBase], cfg[\ccBase] + 15, chanDesc, takeover).postln;
			} {
				("CQMidi16n: no 16n detected — plug it in and run CQ.midiRescan "
					++ "(any device sending CC %..% on % works).")
					.format(cfg[\ccBase], cfg[\ccBase] + 15, chanDesc).postln;
			};
		} { |err|
			midiOK = false;
			this.class.prWarn("MIDI unavailable (%) — running without the 16n."
				.format(err.errorString));
		};
	}

	// pickup: engage when the fader lands on the value (±1 CC step) or moves
	// through it — never on a far touch (SPEC §6.1 takeover)
	prCrossed { |prev, now, target|
		if((now - target).abs <= (1 / 127)) { ^true };
		if(prev.isNil) { ^false };
		^((prev - target) * (now - target)) < 0
	}

	*prWarn { |msg| if(postWarnings) { ("CQMidi16n: " ++ msg).warn } }
}
