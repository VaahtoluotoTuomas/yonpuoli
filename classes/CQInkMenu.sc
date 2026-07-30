// CQInkMenu — a small Drawer-native choice box.
//
// Qt's PopUpMenu only lets SuperCollider colour the collapsed item; its opened
// list remains platform chrome. This control keeps both states in the Ink kit:
// one painted UserView for the closed box and a temporary, borderless painted
// list for the open choices. Hover uses the same spark cue as the instrument's
// other selectable controls.

CQInkMenu {
	var <view, <items = #[], <value, <skin = \day, <hover = false, <isOpen = false;
	var popupWindow, <popupView, <popupHover, menuFont, <action;
	var <enabled = true, <maxVisibleRows = 16, <scrollOffset = 0;

	*new { |items = #[], skin = \day|
		^super.new.prInit(items, skin)
	}

	prInit { |choices, initialSkin|
		skin = initialSkin;
		menuFont = CQInk.font(\number, CQGuiScale.metrics(\compact)[\mono]);
		view = UserView().fixedSize_(Size(170, 24))
			.background_(CQInk.palette(skin)[\ground]);
		view.drawFunc = {
			try { this.prDrawClosed } { |error| CQInkControl.noteDrawError(error) }
		};
		view.mouseEnterAction = { hover = true; view.refresh };
		view.mouseLeaveAction = { hover = false; view.refresh };
		view.mouseDownAction = { this.open };
		this.items = choices;
		^this
	}

	items_ { |choices|
		items = choices ? [];
		if(items.isEmpty) {
			value = nil
		} {
			if(value.isNil or: { value >= items.size }) { value = 0 };
		};
		scrollOffset = scrollOffset.clip(0,
			(items.size - maxVisibleRows).max(0));
		view.refresh;
		^this
	}

	item {
		if(value.isNil) { ^nil };
		^items.at(value)
	}

	value_ { |index|
		value = if(index.isNil or: { items.isEmpty }) { nil } {
			index.asInteger.clip(0, items.size - 1)
		};
		view.refresh;
		^this
	}

	action_ { |function| action = function; ^this }

	enabled_ { |flag|
		enabled = flag == true;
		if(enabled.not) { this.close };
		view.refresh;
		^this
	}

	maxVisibleRows_ { |count|
		maxVisibleRows = count.asInteger.max(1);
		scrollOffset = scrollOffset.clip(0,
			(items.size - maxVisibleRows).max(0));
		^this
	}

	font_ { |font|
		menuFont = font;
		view.refresh;
		^this
	}

	fixedSize_ { |size|
		view.fixedSize = size;
		view.refresh;
		^this
	}

	skin_ { |next|
		skin = next;
		view.background = CQInk.palette(skin)[\ground];
		view.refresh;
		popupView !? {
			popupView.background = CQInk.palette(skin)[\ground];
			popupView.refresh
		};
		^this
	}

	free { this.close; ^this }

	open {
		var absolute, rowH, height, popupTop, screenRect, visibleRows, maxOffset;
		if(enabled.not or: { isOpen } or: { items.isEmpty }) { ^this };
		isOpen = true;
		popupHover = value;
		rowH = view.bounds.height.max(24);
		visibleRows = items.size.min(maxVisibleRows);
		height = rowH * visibleRows;
		maxOffset = (items.size - visibleRows).max(0);
		scrollOffset = ((value ? 0) - (visibleRows div: 2))
			.clip(0, maxOffset);
		absolute = view.absoluteBounds;
		popupTop = absolute.bottom;
		// Morph Bridge sits near the bottom of the workbench. Like a native
		// choice box, open upward when the complete list would leave the screen.
		if((popupTop + height) > Window.screenBounds.height) {
			popupTop = (absolute.top - height).max(0)
		};
		// absoluteBounds is in Qt's top-left global coordinates, while Window
		// accepts SuperCollider's flipped screen coordinates.
		screenRect = Window.flipY(
			Rect(absolute.left, popupTop, absolute.width, height));
		popupWindow = Window("", screenRect, false, false)
			.background_(CQInk.palette(skin)[\ground])
			.alwaysOnTop_(true);
		// This is a separate top-level window: the instrument window's
		// acceptsMouseOver flag does not propagate into it.
		popupWindow.acceptsMouseOver = true;
		popupView = UserView(popupWindow, Rect(0, 0, absolute.width, height))
			.background_(CQInk.palette(skin)[\ground]);
		popupView.drawFunc = {
			try { this.prDrawPopup(rowH) } {
				|error| CQInkControl.noteDrawError(error)
			}
		};
		// Ordinary pointer travel is mouseOverAction; mouseMoveAction is only
		// delivered while a button is held. Both keep the same row hot.
		popupView.mouseOverAction = { |v, x, y|
			var next = this.prPopupIndex(y, rowH);
			if(next != popupHover) { popupHover = next; v.refresh };
		};
		popupView.mouseMoveAction = popupView.mouseOverAction;
		popupView.mouseLeaveAction = { popupHover = nil; popupView.refresh };
		popupView.mouseDownAction = { |v, x, y|
			value = this.prPopupIndex(y, rowH);
			this.close;
			// Close the temporary top-level before the callback is allowed to
			// remeasure or swap the owning page's layouts.
			action.value(this);
		};
		popupView.mouseWheelAction = { |v, x, y, modifiers, xDelta, yDelta|
			var next = (scrollOffset - yDelta.sign)
				.clip(0, (items.size - visibleRows).max(0));
			if(next != scrollOffset) {
				scrollOffset = next;
				popupHover = this.prPopupIndex(y, rowH);
				v.refresh;
			};
		};
		popupView.keyDownAction = { |v, char, modifiers, unicode, keycode|
			if(keycode == 16777216) { this.close }; // Qt::Key_Escape
		};
		popupWindow.endFrontAction = { this.close };
		popupWindow.onClose = {
			popupWindow = nil;
			popupView = nil;
			isOpen = false;
			view.refresh
		};
		popupWindow.front;
		popupView.setProperty(\focusPolicy, 2);
		popupView.focus(true);
		view.refresh;
		^this
	}

	close {
		var window = popupWindow;
		popupWindow = nil;
		popupView = nil;
		isOpen = false;
		view.refresh;
		window !? { if(window.isClosed.not) { window.close } };
		^this
	}

	prDrawClosed {
		var palette = CQInk.palette(skin), rect = view.bounds.moveTo(0, 0);
		var body = rect.insetBy(1, 1), arrowX = body.right - 13;
		var activeHover = enabled and: { hover or: { isOpen } };
		var textColor = if(enabled) { palette[\line] } {
			palette[\whisper].copy.alpha_(0.46)
		};
		if(activeHover) {
			Pen.fillColor = palette[\spark].copy.alpha_(if(isOpen) { 0.14 } { 0.08 });
			Pen.addRoundedRect(body, CQInk.metrics[\buttonRadius],
				CQInk.metrics[\buttonRadius]);
			Pen.fill;
		};
		Pen.strokeColor = if(activeHover) {
			palette[\spark].copy.alpha_(0.72)
		} {
			if(enabled) { palette[\whisper] } {
				palette[\whisper].copy.alpha_(0.38)
			}
		};
		Pen.width = if(isOpen) { CQInk.strokes[\normal] } {
			CQInk.strokes[\hairline]
		};
		Pen.addRoundedRect(body, CQInk.metrics[\buttonRadius],
			CQInk.metrics[\buttonRadius]);
		Pen.stroke;
		Pen.stringLeftJustIn(
			this.class.ellipsize((this.item ? "—").asString,
				(body.width - 28).max(12), menuFont.size),
			Rect(body.left + 7, body.top, (body.width - 28).max(12), body.height),
			menuFont, textColor);
		Pen.strokeColor = if(activeHover) { palette[\spark] } {
			if(enabled) { palette[\line] } {
				palette[\whisper].copy.alpha_(0.46)
			}
		};
		Pen.width = CQInk.strokes[\hairline];
		Pen.line(Point(arrowX - 4, body.center.y - 2),
			Point(arrowX, body.center.y + 2));
		Pen.line(Point(arrowX, body.center.y + 2),
			Point(arrowX + 4, body.center.y - 2));
		Pen.stroke;
	}

	prDrawPopup { |rowH|
		var palette = CQInk.palette(skin);
		var rect = popupView.bounds.moveTo(0, 0);
		var visibleRows = items.size.min(maxVisibleRows);
		var textRight = if(items.size > visibleRows) { rect.right - 7 } {
			rect.right
		};
		Pen.fillColor = palette[\ground];
		Pen.fillRect(rect);
		visibleRows.do { |rowIndex|
			var index = scrollOffset + rowIndex;
			var choice = items[index];
			var row = Rect(1, rowIndex * rowH, rect.width - 2, rowH);
			if(index == popupHover) {
				Pen.fillColor = palette[\spark].copy.alpha_(0.18);
				Pen.addRoundedRect(row.insetBy(2, 2),
					CQInk.metrics[\buttonRadius], CQInk.metrics[\buttonRadius]);
				Pen.fill;
				Pen.strokeColor = palette[\spark].copy.alpha_(0.82);
				Pen.width = CQInk.strokes[\normal];
				Pen.addRoundedRect(row.insetBy(2, 2),
					CQInk.metrics[\buttonRadius], CQInk.metrics[\buttonRadius]);
				Pen.stroke;
			};
			Pen.stringLeftJustIn(
				this.class.ellipsize(choice.asString,
					(textRight - row.left - 14).max(12), menuFont.size),
				Rect(row.left + 7, row.top,
					(textRight - row.left - 14).max(12), row.height),
				menuFont,
				if(index == popupHover) { palette[\spark] } { palette[\line] });
		};
		if(items.size > visibleRows) {
			var track = Rect(rect.right - 4, 3, 2, rect.height - 6);
			var thumbH = (track.height * (visibleRows / items.size))
				.max(8);
			var travel = (track.height - thumbH).max(0);
			var maxOffset = (items.size - visibleRows).max(1);
			Pen.fillColor = palette[\whisper].copy.alpha_(0.22);
			Pen.fillRect(track);
			Pen.fillColor = palette[\spark].copy.alpha_(0.72);
			Pen.fillRect(Rect(track.left,
				track.top + (travel * (scrollOffset / maxOffset)),
				track.width, thumbH));
		};
		Pen.strokeColor = palette[\line].copy.alpha_(0.82);
		Pen.width = CQInk.strokes[\hairline];
		Pen.addRoundedRect(rect.insetBy(0.5, 0.5), CQInk.metrics[\buttonRadius],
			CQInk.metrics[\buttonRadius]);
		Pen.stroke;
	}

	prPopupIndex { |y, rowH|
		var visibleRows = items.size.min(maxVisibleRows);
		^scrollOffset + (y / rowH).floor.asInteger
			.clip(0, visibleRows - 1)
	}

	// Driver endpoint names can contain CR/LF, tabs and other control
	// whitespace. Keep the exact String in the model, but collapse its display
	// representation to one trimmed line before measuring or drawing it.
	*displayLine { |string|
		var output = "", pendingSpace = false;
		string.asString.do { |character|
			if(character.isSpace or: { character.isControl }) {
				if(output.size > 0) { pendingSpace = true };
			} {
				if(pendingSpace) {
					output = output ++ $ ;
					pendingSpace = false;
				};
				output = output ++ character;
			};
		};
		^output
	}

	// Menus are one-line controls. Normalizing, measuring and shortening before
	// Pen receives the String prevents Qt from wrapping a long device name
	// underneath a one-row hover rectangle.
	*ellipsize { |string, width, fontSize = 12|
		var source = this.displayLine(string), suffix = "…", count, candidate;
		if(CQGuiScale.textWidth(source, fontSize, true) <= width) { ^source };
		count = source.size - 1;
		while { count > 0 } {
			candidate = source.copyRange(0, count - 1) ++ suffix;
			if(CQGuiScale.textWidth(candidate, fontSize, true) <= width) {
				^candidate
			};
			count = count - 1;
		};
		^suffix
	}
}
