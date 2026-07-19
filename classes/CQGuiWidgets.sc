// CQGuiWidgets — param-bound widget factory for CQGui (ARCH §5: the GUI is a
// pure view). Every control writes through the CQState funnel (clamped) and
// registers an updater in the shared registry so dependant notifications move
// the widget; setting a view's .value never fires its action, so a funnel echo
// cannot loop. No literal ranges here: everything maps through CQSpec.

CQGuiWidgets {
	var state, updaters, owner;

	*new { |state, updaters, owner| ^super.newCopyArgs(state, updaters, owner) }

	// updater registry: paramName -> List of { |val| ... } view painters;
	// CQGui's single SimpleController fans notifications out to these
	register { |name, func|
		updaters[name] = (updaters[name] ?? { List.new }).add(func);
		func.value(state.get(name));            // paint the initial value
	}

	// labeled knob for continuous params; color = banana accent on the label
	knob { |name, label, color, size = 34, surface = \ground|
		var family = this.prFamily(color), k = this.prOwn(CQInkControl.knob(label, family));
		k.surface = surface;
		k.action = { |v| state.set(name, CQSpec.map(name, v.value)) };
		this.register(name, { |v|
			k.value = CQSpec.unmap(name, v);
			k.display = this.class.format(v);
		});
		^k.view
	}

	// two-state toggle; lights up in onColor when the param is 1
	toggle { |name, label, onColor, surface = \ground|
		var family = this.prFamily(onColor), b = this.prOwn(
			CQInkControl.button(label, 58, \day, true, false, family)
		);
		b.surface = surface;
		b.action = { |bt| state.set(name, bt.value) };
		this.register(name, { |v| b.value = (v ? 0).clip(0, 1) });
		^b.view
	}

	// enum popup (dolby modes, ranges, preamp sources, interp)
	enumSel { |name, color, label = ""|
		var values = CQSpec.at(name)[\values];
		var family = this.prFamily(color), m = this.prOwn(
			CQInkControl.switch(label, values, if(values.size > 3) { 112 } { 82 }, family)
		);
		m.action = { |menu| state.set(name, values[menu.value]) };
		this.register(name, { |v| m.value = values.indexOf(v) ? 0 });
		^m.view
	}

	// small integer box (bits)
	intBox { |name|
		var b = this.prOwn(CQInkControl.number("bits"));
		b.action = { |box| state.set(name, CQSpec.map(name, box.value).round.asInteger) };
		this.register(name, { |v|
			b.value = CQSpec.unmap(name, v);
			b.display = v.asInteger.asString;
		});
		^b.view
	}

	// small horizontal slider (castle sends, deck levels)
	hslider { |name, width = 56|
		var s = this.prOwn(CQInkControl.slider(width));
		s.action = { |sl| state.set(name, CQSpec.map(name, sl.value)) };
		this.register(name, { |v| s.value = CQSpec.unmap(name, v) });
		^s.view
	}

	// Keep every semantic color lookup in CQInk while letting older panel code
	// pass palette colors during the migration.
	prFamily { |color|
		var palette = CQInk.palette(\day), found;
		if(color.isNil) { ^\line };
		palette.keysValuesDo { |key, value| if(value == color) { found = key } };
		^found ? \line
	}

	prOwn { |control| owner.registerInkControl(control); ^control }

	// static helper: compact value display
	*format { |v|
		var clean;
		if(v.isNumber.not) { ^v.asString };
		if(v.isInteger) { ^v.asString };
		clean = if(v.abs < 0.005) { 0.0 } { v };
		^if(clean.abs >= 100) {
			clean.round(1).asInteger.asString
		} {
			clean.round(0.01).asString
		}
	}

	// shared banana color code (SPEC §6.2)
	*colors {
		^CQInk.palette(\day)
	}
}
