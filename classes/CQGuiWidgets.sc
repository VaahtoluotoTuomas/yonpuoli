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
	knob { |name, label, color, role = \standard, surface = \ground, scale|
		var family = this.prFamily(color), diameter = CQInk.knobDiameter(role);
		// The persistent Master strip reads at the chrome's fixed type size.
		var k = this.prOwn(CQInkControl.knob(label, family, \night, diameter,
			scale !? { |m| m[\label] }));
		k.surface = surface;
		scale !? { k.scale = scale };
		k.action = { |v| state.set(name, CQSpec.map(name, v.value)) };
		this.register(name, { |v|
			k.value = CQSpec.unmap(name, v);
			k.display = this.class.format(v);
		});
		if(CQGuiPanels.masterParamNames.includes(name)) {
			owner !? { owner.registerInfo(k.view, [\master, name]) };
		};
		owner !? { owner.registerHover(k, [\param, name]) };
		^k.view
	}

	// two-state toggle; lights up in onColor when the param is 1
	toggle { |name, label, onColor, surface = \ground|
		var family = this.prFamily(onColor), b = this.prOwn(
			CQInkControl.button(label, 58, \night, true, false, family)
		);
		b.surface = surface;
		b.action = { |bt| state.set(name, bt.value) };
		this.register(name, { |v| b.value = (v ? 0).clip(0, 1) });
		owner !? { owner.registerHover(b, [\param, name]) };
		^b.view
	}

	// enum popup (dolby modes, ranges, preamp sources, interp)
	enumSel { |name, color, label = ""|
		var values = CQSpec.at(name)[\values];
		var family = this.prFamily(color), m = this.prOwn(
			CQInkControl.switch(label, values,
				if(values.size > 3) { 112 } { 82 }, family, \night)
		);
		m.action = { |menu| state.set(name, values[menu.value]) };
		this.register(name, { |v| m.value = values.indexOf(v) ? 0 });
		owner !? { owner.registerHover(m, [\param, name]) };
		^m.view
	}

	// small integer box (bits)
	intBox { |name|
		var b = this.prOwn(CQInkControl.number("bits", \night));
		b.action = { |box| state.set(name, CQSpec.map(name, box.value).round.asInteger) };
		this.register(name, { |v|
			b.value = CQSpec.unmap(name, v);
			b.display = v.asInteger.asString;
		});
		owner !? { owner.registerHover(b, [\param, name]) };
		^b.view
	}

	// The Workshop's ledger row (GUI_SPEC §16.7): one 24 px rail carrying an
	// exact value. Everything shape-related — seats, their order, where zero
	// sits — is read from CQSpec here and handed to the control, so the drawn
	// row still owns no range, step, default or enum of its own. Returns the
	// control (not its view): the page needs the row itself for the mic's
	// danger treatment, and the view is one accessor away.
	ledger { |name, label, color, width = 200, surface = \recess, scale|
		var kind = CQSpec.kind(name), values = CQSpec.enumValues(name);
		var family = this.prFamily(color);
		var seats = switch(kind,
			\enum, { values.collect { |value| value.asString } },
			\toggle, { ["off", "on"] },
			{ nil }
		);
		var mode = switch(kind, \enum, { \enum }, \toggle, { \toggle },
			\int, { \int }, { \num });
		var row = this.prOwn(CQInkControl.ledger(label, mode, family, \night,
			width, seats, CQSpec.centerNorm(name), scale));
		row.surface = surface;
		switch(kind,
			\enum, {
				row.action = { |c| state.set(name, values[c.value.asInteger]) };
				this.register(name, { |v|
					row.value = values.indexOf(v) ? 0;
					row.display = this.class.ledgerFormat(name, v);
				});
			},
			\toggle, {
				row.action = { |c| state.set(name, c.value.asInteger) };
				this.register(name, { |v|
					row.value = (v ? 0).asFloat.clip(0, 1).round.asInteger;
					row.display = this.class.ledgerFormat(name, v);
				});
			},
			{
				row.action = { |c| state.set(name, CQSpec.map(name, c.value)) };
				this.register(name, { |v|
					row.value = CQSpec.unmap(name, v);
					row.display = this.class.ledgerFormat(name, v);
				});
			}
		);
		// Register identity here; native hover hooks wait for GUI composition.
		owner !? { owner.registerHover(row, [\param, name]) };
		^row
	}

	// How many decimals a parameter deserves on the ledger: enough for the
	// printed number to resolve about a thousandth of its own range, so
	// qShLag's 4 ms and ordinary gain values are both readable at a glance and
	// neither is padded with meaningless digits. Derived from CQSpec's range,
	// never from a per-parameter table.
	*ledgerDecimals { |name|
		var e = CQSpec.at(name), span;
		if(e.isNil or: { e[\kind] != \num }) { ^0 };
		span = (e[\spec].maxval - e[\spec].minval).abs;
		if(span <= 0) { ^2 };
		^((span / 1000).log10.neg).ceil.asInteger.clip(0, 4)
	}

	// the ledger's exact-value reading; a rail with no value is not enough for
	// this precision page (GUI_SPEC)
	*ledgerFormat { |name, value|
		var e = CQSpec.at(name);
		if(e.isNil) { ^(value ? "").asString };
		^switch(e[\kind],
			\enum, { (value ? e[\default]).asString },
			\toggle, { if((value ? 0).asFloat > 0.5) { "on" } { "off" } },
			\int, { (value ? 0).asFloat.round.asInteger.asString },
			{ this.prFixed((value ? 0).asFloat, this.ledgerDecimals(name)) }
		)
	}

	// fixed-decimal text, so a column of values keeps one width and a trailing
	// zero does not disappear (Float.asString would print 0.8 for 0.800)
	*prFixed { |value, decimals = 2|
		var scale = (10 ** decimals).asInteger, negative = value < 0, scaled, text;
		scaled = (value.abs * scale).round.asInteger;
		text = (scaled div: scale).asString;
		if(decimals > 0) {
			text = text ++ "." ++ (scaled % scale).asString.padLeft(decimals, "0");
		};
		^if(negative and: { scaled > 0 }) { "-" ++ text } { text }
	}

	// small horizontal slider (castle sends, deck levels)
	hslider { |name, width = 56|
		var s = this.prOwn(CQInkControl.slider(width, nil, \night));
		s.action = { |sl| state.set(name, CQSpec.map(name, sl.value)) };
		this.register(name, { |v| s.value = CQSpec.unmap(name, v) });
		owner !? { owner.registerHover(s, [\param, name]) };
		^s.view
	}

	// Keep every semantic color lookup in CQInk while letting older panel code
	// pass palette colors during the migration. Newer surfaces (the Workshop
	// manifest) name their family directly, which is what §16.2 asks for: no
	// color is inlined at a draw site.
	prFamily { |color|
		var palette = CQInk.palette(\night), found;
		if(color.isNil) { ^\line };
		if(color.isKindOf(Symbol)) {
			^if(palette[color].notNil) { color } { \line }
		};
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
		^CQInk.palette(\night)
	}
}
