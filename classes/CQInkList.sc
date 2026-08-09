// CQInkList — a small Ink-painted persistent single-selection list.
//
// Native ListView owns an undocumented platform row height, so a language-side
// hover computed from the Drawer's reviewed row metric can land one item away.
// This view makes row drawing, hover hit testing and click selection share the
// exact same `rowHeight`.

CQInkList {
	var <view, <items = #[], <value, <hoverIndex, <skin = \day;
	var listFont, rowHeight = 20, action, selectionAction, scrollIndex = 0;

	*new { |items = #[], skin = \day, height = 20|
		^super.new.prInit(items, skin, height)
	}

	prInit { |initialItems, initialSkin, initialHeight|
		skin = initialSkin;
		rowHeight = initialHeight;
		listFont = CQInk.font(\spellbook);
		view = UserView().background_(CQInk.palette(skin)[\ground]);
		view.drawFunc = {
			try { this.prDraw } { |error| CQInkControl.noteDrawError(error) }
		};
		view.mouseOverAction = { |v, x, y| this.prHoverAt(y) };
		view.mouseMoveAction = view.mouseOverAction;
		view.mouseLeaveAction = {
			if(hoverIndex.notNil) { hoverIndex = nil; view.refresh }
		};
		view.mouseDownAction = { |v, x, y| this.prChooseAt(y) };
		view.mouseWheelAction = { |v, x, y, modifiers, xDelta, yDelta|
			var maxScroll = this.prMaxScroll;
			scrollIndex = (scrollIndex - yDelta.sign).clip(0, maxScroll);
			hoverIndex = nil;
			view.refresh
		};
		this.items = initialItems;
		^this
	}

	items_ { |next|
		items = next ? [];
		if(value.notNil and: { value >= items.size }) { value = nil };
		scrollIndex = scrollIndex.clip(0, this.prMaxScroll);
		if(hoverIndex.notNil and: { hoverIndex >= items.size }) { hoverIndex = nil };
		view.refresh;
		^this
	}

	value_ { |index|
		value = if(index.isNil or: { items.isEmpty }) { nil } {
			index.asInteger.clip(0, items.size - 1)
		};
		this.prRevealValue;
		view.refresh;
		^this
	}

	action_ { |function| action = function; ^this }
	selectionAction_ { |function| selectionAction = function; ^this }
	selectionMode { ^\single }

	font_ { |font| listFont = font; view.refresh; ^this }
	rowHeight_ { |height| rowHeight = height.max(1); view.refresh; ^this }
	fixedWidth_ { |width| view.fixedWidth = width; ^this }

	skin_ { |next|
		skin = next;
		view.background = CQInk.palette(skin)[\ground];
		view.refresh;
		^this
	}

	prVisibleRows {
		^(view.bounds.height / rowHeight).ceil.asInteger.max(1)
	}

	prMaxScroll { ^(items.size - this.prVisibleRows).max(0) }

	prIndexAt { |y|
		var index;
		if(y < 0 or: { y >= view.bounds.height }) { ^nil };
		index = scrollIndex + (y / rowHeight).floor.asInteger;
		if(index < 0 or: { index >= items.size }) { ^nil };
		^index
	}

	prHoverAt { |y|
		var next = this.prIndexAt(y);
		if(next != hoverIndex) {
			hoverIndex = next;
			view.refresh
		};
		^this
	}

	prChooseAt { |y|
		var next = this.prIndexAt(y);
		if(next.isNil) { ^this };
		value = next;
		view.refresh;
		action.value(this);
		if(selectionAction.notNil and: { selectionAction !== action }) {
			selectionAction.value(this)
		};
		^this
	}

	prRevealValue {
		var visible = this.prVisibleRows;
		value !? {
			if(value < scrollIndex) { scrollIndex = value };
			if(value >= (scrollIndex + visible)) {
				scrollIndex = value - visible + 1
			};
			scrollIndex = scrollIndex.clip(0, this.prMaxScroll)
		};
		^this
	}

	prDraw {
		var palette = CQInk.palette(skin), rect = view.bounds.moveTo(0, 0);
		var visible = this.prVisibleRows, maxScroll = this.prMaxScroll;
		Pen.fillColor = palette[\ground];
		Pen.fillRect(rect);
		visible.do { |slot|
			var index = scrollIndex + slot, row, selected, hovered;
			if(index < items.size) {
				row = Rect(0, slot * rowHeight, rect.width, rowHeight);
				selected = index == value;
				hovered = index == hoverIndex;
				if(selected) {
					Pen.fillColor = palette[\spaf].copy.alpha_(0.24);
					Pen.fillRect(row.insetBy(1, 1));
				};
				if(hovered) {
					Pen.fillColor = palette[\spark].copy.alpha_(0.14);
					Pen.addRoundedRect(row.insetBy(2, 2),
						CQInk.metrics[\buttonRadius], CQInk.metrics[\buttonRadius]);
					Pen.fill;
					Pen.strokeColor = palette[\spark].copy.alpha_(0.78);
					Pen.width = CQInk.strokes[\normal];
					Pen.addRoundedRect(row.insetBy(2, 2),
						CQInk.metrics[\buttonRadius], CQInk.metrics[\buttonRadius]);
					Pen.stroke;
				};
				Pen.stringLeftJustIn(items[index].asString, row.insetBy(7, 0),
					listFont, if(hovered) { palette[\spark] } { palette[\line] });
			};
		};
		if(maxScroll > 0) {
			var track = Rect(rect.right - 4, 3, 1, rect.height - 6);
			var thumbH = (track.height * (visible / items.size)).max(8);
			var thumbY = track.top
				+ ((track.height - thumbH) * (scrollIndex / maxScroll));
			Pen.fillColor = palette[\whisper].copy.alpha_(0.42);
			Pen.fillRect(Rect(track.left, thumbY, 1, thumbH));
		};
	}
}
