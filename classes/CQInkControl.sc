// CQInkControl — functional UserView control painted only through CQInk.
// The wrapper deliberately exposes a tiny value/action API so the GUI keeps
// CQState as its sole write funnel while avoiding platform-native widget chrome.

CQInkControl {
	classvar <drawErrors = 0;
	var <view, <kind, <value = 0, <label, <skin, <family, <options, <diameter = 26;
	var <action, <display = "", pressed = false, <hover = false, <enabled = true;
	var toggle = false, momentary = false, glyph, danger = false, downward = false;
	var surface = \ground, startX = 0, startY = 0, startValue = 0;
	// the Workshop ledger row's own shape channel: \num · \int · \toggle · \enum,
	// its seat captions and where its zero sits on the rail (nil = unipolar)
	var <ledgerMode = \num, <seats, <center;
	// a CQGuiScale metrics Event for controls that live in the Drawer, whose
	// type steps between two reviewed sizes; nil keeps the kit's fixed px
	var <scale;

	*knob { |label, family, skin = \day, knobDiameter, labelSize|
		var control, m = CQInk.metrics;
		knobDiameter = (knobDiameter ? m[\knobDiameter])
			.clip(m[\knobDiameter], m[\primaryKnobDiameter]);
		control = super.new.prInit(\knob, label, family, skin,
			Size(knobDiameter + 22,
				CQInk.knobBoxHeight(knobDiameter, labelSize)));
		control.prKnobDiameter(knobDiameter);
		^control
	}

	*slider { |width = 56, family, skin = \day|
		^super.new.prInit(\slider, "", family, skin, Size(width, 16))
	}

	*button { |label, width = 58, skin = \day, toggle = false,
		momentary = false, accent = \line, glyph|
		var control = super.new.prInit(\button, label, accent, skin, Size(width, 24));
		control.prButtonMode(toggle, momentary, glyph);
		^control
	}

	*switch { |label, options, width = 86, family, skin = \day|
		var control = super.new.prInit(\switch, label, family, skin, Size(width, 36));
		control.prOptions(options);
		^control
	}

	*number { |label = "", skin = \day|
		^super.new.prInit(\number, label, nil, skin, Size(44, 36))
	}

	// the Workshop's exact-value row (GUI_SPEC §16.7). Width is given by the
	// island that owns it, so the page keeps stable coordinates; the height is
	// the kit's own 24 px. No range, step, default or enum lives here: the
	// factory hands over seats and centre already read from CQSpec.
	*ledger { |label, mode = \num, family, skin = \day, width = 200,
		seats, center, scale|
		var control = super.new.prInit(\ledger, label, family, skin,
			Size(width, CQInk.ledgerMetrics(scale)[\height]));
		control.prLedgerMode(mode, seats, center);
		control.scale = scale;
		^control
	}

	*meter { |skin = \day, danger = false, downward = false|
		var control = super.new.prInit(\meter, "", nil, skin, Size(16, 72));
		control.prMeterMode(danger, downward);
		^control
	}

	*lamp { |family = \lamp, skin = \day|
		^super.new.prInit(\lamp, "", family, skin, Size(14, 14))
	}

	*resetDrawErrors { drawErrors = 0 }
	*noteDrawError { |error| drawErrors = drawErrors + 1; error.reportError }

	prInit { |controlKind, controlLabel, controlFamily, controlSkin, size|
		kind = controlKind;
		label = controlLabel;
		family = controlFamily;
		skin = controlSkin;
		view = UserView().fixedSize_(size).background_(CQInk.palette(skin)[surface]);
		view.drawFunc = {
			try { this.prDraw } { |error| this.class.noteDrawError(error) }
		};
		this.prBind;
		^this
	}

	isInteractive { ^#[knob, slider, button, switch, number, ledger].includes(kind) }

	// a ledger row whose value moves on stable seats rather than along the rail
	isSeated { ^#[toggle, enum].includes(ledgerMode) }

	prButtonMode { |isToggle, isMomentary, icon|
		toggle = isToggle;
		momentary = isMomentary;
		glyph = icon;
		^this
	}

	prOptions { |items| options = items; ^this }
	prMeterMode { |isDanger, fillsDown| danger = isDanger; downward = fillsDown; ^this }
	prKnobDiameter { |newDiameter| diameter = newDiameter; ^this }
	prLedgerMode { |mode, seatLabels, centerNorm|
		ledgerMode = mode ? \num;
		seats = seatLabels;
		center = centerNorm;
		^this
	}

	// SPEC §7's mic row: the control brightens, its module never does
	danger_ { |flag| danger = flag; view.refresh; ^this }

	// The Drawer's size step changed under a live control: take the new type
	// metrics and the width its island now gives it, and re-fix the box. The
	// control keeps its value, action and updater registration — only its
	// measurements move, so a resize never rebuilds the funnel bindings.
	scale_ { |metrics, width|
		scale = metrics;
		if(kind == \ledger) {
			view.fixedSize = Size(width ? view.bounds.width,
				CQInk.ledgerMetrics(scale)[\height]);
		};
		view.refresh;
		^this
	}

	// a Drawer button re-measured for the current step, so growing the type can
	// never crop a caption
	resizeTo { |width, height|
		view.fixedSize = Size(width, height ? view.bounds.height);
		view.refresh;
		^this
	}

	action_ { |func| action = func; ^this }
	value_ { |newValue| value = newValue ? 0; view.refresh; ^this }
	display_ { |string| display = string ? ""; view.refresh; ^this }
	label_ { |string| label = string; view.refresh; ^this }
	family_ { |newFamily| family = newFamily; view.refresh; ^this }
	glyph_ { |icon| glyph = icon; view.refresh; ^this }
	enabled_ { |flag|
		enabled = flag == true;
		if(enabled.not) {
			hover = false;
			pressed = false;
		};
		view.refresh;
		^this
	}

	skin_ { |newSkin|
		skin = newSkin;
		view.background = CQInk.palette(skin)[surface];
		view.refresh;
		^this
	}

	surface_ { |newSurface|
		surface = newSurface;
		view.background = CQInk.palette(skin)[surface];
		view.refresh;
		^this
	}

	flash {
		value = 1;
		view.refresh;
		AppClock.sched(CQInk.momentaryFlash, {
			value = 0;
			view.refresh;
			nil
		});
		^this
	}

	prBind {
		view.mouseEnterAction = { hover = enabled; view.refresh };
		view.mouseLeaveAction = { hover = false; view.refresh };
		view.mouseDownAction = { |v, x, y|
			var index;
			// a ledger answers only over its own rail: pressing the label or the
			// value column must never slam the parameter to an end of its range
			var accepts = enabled and: {
				(kind != \ledger) or: { this.prLedgerOverRail(v, x) }
			};
			if(accepts) {
				pressed = true;
				startX = x;
				startY = y;
				startValue = value;
				switch(kind,
					\button, {
						if(toggle) { value = 1 - value };
						if(momentary) { value = 1 };
						action.value(this);
					},
					\switch, {
						index = ((x / v.bounds.width) * options.size).floor.asInteger;
						value = index.clip(0, options.size - 1);
						action.value(this);
					},
					\ledger, { this.prLedgerTouch(v, x) }
				);
				view.refresh;
			};
		};
		view.mouseMoveAction = { |v, x, y|
			var index;
			if(pressed and: { enabled }) {
				switch(kind,
					\knob, { value = (startValue + ((startY - y) / 100)).clip(0, 1) },
					\number, { value = (startValue + ((startY - y) / 100)).clip(0, 1) },
					\slider, { value = (x / v.bounds.width).clip(0, 1) },
					\switch, {
						index = (((x / v.bounds.width) * options.size).floor.asInteger)
							.clip(0, options.size - 1);
						if(index != value) {
							value = index;
							action.value(this)
						}
					},
					\ledger, { this.prLedgerTouch(v, x) }
				);
				if(#[knob, number, slider].includes(kind)) { action.value(this) };
				view.refresh;
			};
		};
		view.mouseUpAction = {
			pressed = false;
			if(momentary) { value = 0 };
			view.refresh;
		};
		view.mouseWheelAction = { |v, x, y, modifiers, xDelta, yDelta|
			// the existing normalized fine nudge; a seated ledger has no
			// continuum to nudge along, so the wheel leaves it alone
			var nudges = #[knob, number, slider].includes(kind)
				or: { kind == \ledger and: { this.isSeated.not } };
			if(enabled and: { nudges }) {
				value = (value + (yDelta.sign * 0.01)).clip(0, 1);
				action.value(this);
				view.refresh;
			};
		};
	}

	// one geometry for cue and action (§4.5): both ask CQInk for the same rail
	prLedgerRect { |v| ^Rect(0, 0, v.bounds.width, v.bounds.height) }

	prLedgerOverRail { |v, x|
		^CQInk.ledgerHitsRail(this.prLedgerRect(v), x, scale)
	}

	prLedgerTouch { |v, x|
		var rect = this.prLedgerRect(v), next;
		next = if(this.isSeated) {
			CQInk.ledgerSeatAt(rect, x, (seats ? []).size.max(1), scale)
		} {
			CQInk.ledgerNormAt(rect, x, scale)
		};
		if(next.isNil or: { next == value }) { ^this };
		value = next;
		action.value(this);
		^this
	}

	prDraw {
		var bounds = view.bounds.moveTo(0, 0);
		var activeHover = enabled and: { hover };
		var activePressed = enabled and: { pressed };
		switch(kind,
			\knob, {
				CQInk.drawKnob(bounds, value, label, display, family, skin,
					activePressed or: { activeHover }, diameter,
					scale !? { |m| m[\label] })
			},
			\slider, {
				CQInk.drawSlider(bounds, value, skin, family,
					activePressed or: { activeHover })
			},
			\button, {
				CQInk.drawButton(bounds, label, skin, value > 0, activePressed,
					family, glyph, surface, activeHover,
					scale !? { |m| m[\label] })
			},
			\switch, {
				CQInk.drawSwitch(bounds, label, options, value.asInteger, skin,
					family, activePressed or: { activeHover })
			},
			\number, {
				CQInk.drawNumber(bounds, label, display, skin,
					activePressed or: { activeHover }, surface)
			},
			\ledger, {
				CQInk.drawLedgerRow(bounds, ledgerMode, value, label, display,
					seats, family, skin, activeHover, activePressed,
					center, danger, scale)
			},
			\meter, { CQInk.drawMeter(bounds, value, skin, danger, downward) },
			\lamp, { CQInk.drawLamp(bounds.center, value, value >= 0.5, skin) }
		);
	}
}
