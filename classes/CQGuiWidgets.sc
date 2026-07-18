// CQGuiWidgets — param-bound widget factory for CQGui (ARCH §5: the GUI is a
// pure view). Every control writes through the CQState funnel (clamped) and
// registers an updater in the shared registry so dependant notifications move
// the widget; setting a view's .value never fires its action, so a funnel echo
// cannot loop. No literal ranges here: everything maps through CQSpec.

CQGuiWidgets {
	var state, updaters;

	*new { |state, updaters| ^super.newCopyArgs(state, updaters) }

	// updater registry: paramName -> List of { |val| ... } view painters;
	// CQGui's single SimpleController fans notifications out to these
	register { |name, func|
		updaters[name] = (updaters[name] ?? { List.new }).add(func);
		func.value(state.get(name));            // paint the initial value
	}

	// labeled knob for continuous params; color = banana accent on the label
	knob { |name, label, color, size = 34|
		var k = Knob().mode_(\vert).fixedSize_(Size(size, size));
		var val = StaticText().align_(\center).font_(Font.sansSerif(8));
		var lab = StaticText().string_(label).align_(\center)
			.font_(Font.sansSerif(9, true));
		color !? { lab.stringColor_(color) };
		k.action = { |v| state.set(name, CQSpec.map(name, v.value)) };
		this.register(name, { |v|
			k.value = CQSpec.unmap(name, v);
			val.string = this.class.format(v);
		});
		^View().layout_(
			VLayout(lab, k, val).spacing_(0).margins_(0))
			.fixedWidth_(max(size + 10, 44))
	}

	// two-state toggle; lights up in onColor when the param is 1
	toggle { |name, label, onColor|
		var b = Button().states_([
			[label, Color.gray(0.3), Color.gray(0.85)],
			[label, Color.white, onColor ?? { Color.grey(0.4) }]
		]);
		b.action = { |bt| state.set(name, bt.value) };
		this.register(name, { |v| b.value = (v ? 0).clip(0, 1) });
		^b
	}

	// enum popup (dolby modes, ranges, preamp sources, interp)
	enumSel { |name, color|
		var values = CQSpec.at(name)[\values];
		var m = PopUpMenu().items_(values.collect(_.asString))
			.font_(Font.sansSerif(9));
		color !? { m.stringColor_(color) };
		m.action = { |menu| state.set(name, values[menu.value]) };
		this.register(name, { |v| m.value = values.indexOf(v) ? 0 });
		^m
	}

	// small integer box (bits)
	intBox { |name|
		var spec = CQSpec.at(name)[\spec];
		var b = NumberBox().clipLo_(spec.minval).clipHi_(spec.maxval)
			.step_(1).scroll_step_(1).font_(Font.sansSerif(9));
		// Enter commits, then hands keyboard focus back to the key map (CQGui)
		b.action = { |box| state.set(name, box.value); box.focus(false) };
		this.register(name, { |v| b.value = v });
		^b
	}

	// small horizontal slider (castle sends, deck levels)
	hslider { |name, width = 56|
		var s = Slider().orientation_(\horizontal)
			.fixedSize_(Size(width, 12));
		s.action = { |sl| state.set(name, CQSpec.map(name, sl.value)) };
		this.register(name, { |v| s.value = CQSpec.unmap(name, v) });
		^s
	}

	// static helper: compact value display
	*format { |v|
		if(v.isNumber.not) { ^v.asString };
		if(v.isInteger) { ^v.asString };
		^if(v.abs >= 100) { v.round(1).asInteger.asString } { v.round(0.01).asString }
	}

	// shared banana color code (SPEC §6.2)
	*colors {
		^(
			tri: Color.new255(230, 130, 20),      // orange = triangle
			cas: Color.gray(0.45),                // gray = castle/delay out
			fm: Color.new255(60, 110, 220),       // blue = FM
			input: Color.new255(30, 150, 70),     // green = inputs/spesal
			spaf: Color.new255(150, 60, 200),     // purple = SP.AF
			organ: Color.new255(200, 170, 0),     // yellow = organ
			pre: Color.gray(0.95),                // white = preamp
			hold: Color.new255(200, 30, 30),      // HOLD LED red
			warn: Color.new255(220, 40, 40)
		)
	}
}
