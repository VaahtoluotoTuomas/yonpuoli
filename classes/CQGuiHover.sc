// Always-on, passive hover help. Observe the original hover callbacks, never
// intercept performance input. No timer, musical state setter or server resource.
CQGuiHover {
	var gui, <view, <target, <note, <lines = #[], <paintErrors = 0;
	var bindings, bound, sourceView, shellCtl, freed = false;
	var hoverTarget, infoMode = \off;
	*new { |gui, view| ^super.newCopyArgs(gui, view).prBuild }

	prBuild {
		var resize = gui.window.view.onResize;
		bindings = List.new; bound = IdentitySet.new;
		this.bindTree(gui.nightMap.container, { |v, x, y| gui.nightMap.infoTarget(v, x, y) });
		gui.infoBindings.do { |b| this.bindTree(b[0], { b[1] }) };
		gui.hoverBindings.do { |b| this.bindTree(b[0].view, { b[1] }) };
		gui.shell.hoverBindings.do { |b| this.bindTree(b[0], { b[1] }) };
		this.bindTree(gui.drawer.grid.view, { [\ui, \inspector] });
		this.bindTree(gui.drawer.presetMenu.view, { [\ui, \preset] });
		gui.inkControls.do { |control|
			// Some actions change label after confirmation/state changes.
			// Resolve the current action; unknown labels leave the area blank.
			this.bindTree(control.view, { [\ui, control.label.asSymbol] });
		};
		// Every remaining surface clears the hint: blank panels, labels,
		// headers and the help area itself must not retain the last control.
		this.bindTree(gui.window.view, { nil });
		bindings.add([gui.window.view, \onResize, resize]);
		gui.window.view.onResize = { |v| resize.value(v); this.clear };
		view.drawFunc = {
			try { if(freed.not) { this.draw } } {
				|error| paintErrors = paintErrors + 1; error.reportError
			};
		};
		shellCtl = SimpleController(gui.shellState);
		shellCtl.put(\drawer, { this.clear });
		^this
	}
	bindTree { |v, resolver|
		if(bound.includes(v)) { ^this };
		this.bind(v, resolver);
		v.children.do { |child| this.bindTree(child, resolver) };
		^this
	}
	bind { |v, resolver|
		var over = v.mouseOverAction, leave = v.mouseLeaveAction;
		bound.add(v);
		bindings.addAll([
			[v, \mouseOverAction, over], [v, \mouseLeaveAction, leave],
			[v, \acceptsMouseOver, v.acceptsMouseOver]
		]);
		v.acceptsMouseOver = true;
		v.mouseOverAction = { |sender, x, y|
			over.value(sender, x, y);
			if(freed.not) { this.show(resolver.value(sender, x, y), sender) };
		};
		v.mouseLeaveAction = { |sender|
			leave.value(sender);
			if(sourceView === sender or: { sender === gui.window.view }) { this.clear };
		};
		^this
	}
	show { |next, sender|
		if(freed) { ^this };
		sourceView = sender; hoverTarget = next;
		^this.prUpdateNote
	}
	infoStatus { |enabled, playing, guideKind|
		var next = if(enabled) {
			if(playing) { if(guideKind == \advanced) { \advancedInfo } { \guidedInfo } } {
				if(guideKind.notNil) { \readingGuide } { \inspectInfo }
			}
		} { \off };
		if(freed or: { next == infoMode }) { ^this };
		infoMode = next;
		^this.prUpdateNote
	}
	prUpdateNote {
		var next = if(infoMode == \off) { hoverTarget } { [\ui, infoMode] };
		if(next == target) { ^this };
		target = next; note = CQHoverText.topic(next);
		this.layout;
		if(view.isClosed.not) { view.refresh };
		^this
	}
	clear { ^this.show(nil, nil) }
	layout {
		var font = CQInk.font(\label, CQGuiScale.chrome[\label]);
		lines = if(note.isNil) { [] } {
			CQGuiInfo.wrap(note[\text], view.bounds.width - 24, { |s| s.bounds(font).width })
		};
		^this
	}
	draw {
		var p = CQInk.palette(\night), size = CQGuiScale.chrome[\label];
		if(note.isNil) { ^this };
		Pen.stringAtPoint(note[\title], 12@10, CQInk.font(\label, size + 1), CQInk.infoColor(0.65));
		lines.do { |line, i|
			Pen.stringAtPoint(line, Point(12, 38 + (i * CQGuiScale.textBandHeight(size))),
				CQInk.font(\label, size), p[\line]);
		};
	}
	free {
		if(freed) { ^this };
		infoMode = \off;
		this.clear; freed = true;
		shellCtl !? { shellCtl.remove }; shellCtl = nil;
		bindings.reverseDo { |b|
			if(b[0].isClosed.not) { b[0].perform(b[1].asSetter, b[2]) };
		};
		bindings.clear; bound.clear;
		view.drawFunc = nil;
		^this
	}
}
