// CQInkControl — functional UserView control painted only through CQInk.
// The wrapper deliberately exposes a tiny value/action API so the GUI keeps
// CQState as its sole write funnel while avoiding platform-native widget chrome.

CQInkControl {
	classvar <drawErrors = 0;
	var <view, <kind, <value = 0, <label, <skin, <family, <options;
	var action, display = "", pressed = false, hover = false, enabled = true;
	var toggle = false, momentary = false, glyph, danger = false, downward = false;
	var surface = \ground, startX = 0, startY = 0, startValue = 0;

	*knob { |label, family, skin = \day|
		^super.new.prInit(\knob, label, family, skin, Size(48, 56))
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

	prButtonMode { |isToggle, isMomentary, icon|
		toggle = isToggle;
		momentary = isMomentary;
		glyph = icon;
		^this
	}

	prOptions { |items| options = items; ^this }
	prMeterMode { |isDanger, fillsDown| danger = isDanger; downward = fillsDown; ^this }

	action_ { |func| action = func; ^this }
	value_ { |newValue| value = newValue ? 0; view.refresh; ^this }
	display_ { |string| display = string ? ""; view.refresh; ^this }
	label_ { |string| label = string; view.refresh; ^this }
	enabled_ { |flag| enabled = flag; view.refresh; ^this }

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
		AppClock.sched(0.12, {
			value = 0;
			view.refresh;
			nil
		});
		^this
	}

	prBind {
		view.mouseEnterAction = { hover = true; view.refresh };
		view.mouseLeaveAction = { hover = false; view.refresh };
		view.mouseDownAction = { |v, x, y|
			var index;
			if(enabled) {
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
					}
				);
				view.refresh;
			};
		};
		view.mouseMoveAction = { |v, x, y|
			if(pressed and: { enabled }) {
				switch(kind,
					\knob, { value = (startValue + ((startY - y) / 100)).clip(0, 1) },
					\number, { value = (startValue + ((startY - y) / 100)).clip(0, 1) },
					\slider, { value = (x / v.bounds.width).clip(0, 1) }
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
			if(enabled and: { #[knob, number, slider].includes(kind) }) {
				value = (value + (yDelta.sign * 0.01)).clip(0, 1);
				action.value(this);
				view.refresh;
			};
		};
	}

	prDraw {
		var bounds = view.bounds.moveTo(0, 0);
		switch(kind,
			\knob, {
				CQInk.drawKnob(bounds, value, label, display, family, skin, pressed or: { hover })
			},
			\slider, { CQInk.drawSlider(bounds, value, skin, family) },
			\button, {
				CQInk.drawButton(bounds, label, skin, value > 0, pressed, family, glyph, surface)
			},
			\switch, {
				CQInk.drawSwitch(bounds, label, options, value.asInteger, skin, family)
			},
			\number, {
				CQInk.drawNumber(bounds, label, display, skin, pressed or: { hover }, surface)
			},
			\meter, { CQInk.drawMeter(bounds, value, skin, danger, downward) },
			\lamp, { CQInk.drawLamp(bounds.center, value, value >= 0.5, skin) }
		);
	}
}
