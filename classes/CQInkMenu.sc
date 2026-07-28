// CQInkMenu — a small Drawer-native choice box.
//
// Qt's PopUpMenu only lets SuperCollider colour the collapsed item; its opened
// list remains platform chrome. This control keeps both states in the Ink kit:
// one painted UserView for the closed box and a temporary, borderless painted
// list for the open choices. Hover uses the same spark cue as the instrument's
// other selectable controls.

CQInkMenu {
	var <view, <items = #[], <value, <skin = \day, <hover = false, <isOpen = false;
	var popupWindow, popupView, popupHover, menuFont, action;

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
		var absolute, rowH, height, popupTop, screenRect;
		if(isOpen or: { items.isEmpty }) { ^this };
		isOpen = true;
		popupHover = value;
		rowH = view.bounds.height.max(24);
		height = rowH * items.size;
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
		popupView = UserView(popupWindow, Rect(0, 0, absolute.width, height))
			.background_(CQInk.palette(skin)[\ground]);
		popupView.drawFunc = {
			try { this.prDrawPopup(rowH) } {
				|error| CQInkControl.noteDrawError(error)
			}
		};
		popupView.mouseMoveAction = { |v, x, y|
			var next = (y / rowH).floor.asInteger.clip(0, items.size - 1);
			if(next != popupHover) { popupHover = next; v.refresh };
		};
		popupView.mouseLeaveAction = { popupHover = nil; popupView.refresh };
		popupView.mouseDownAction = { |v, x, y|
			value = (y / rowH).floor.asInteger.clip(0, items.size - 1);
			action.value(this);
			this.close;
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
		if(hover or: { isOpen }) {
			Pen.fillColor = palette[\spark].copy.alpha_(if(isOpen) { 0.14 } { 0.08 });
			Pen.addRoundedRect(body, CQInk.metrics[\buttonRadius],
				CQInk.metrics[\buttonRadius]);
			Pen.fill;
		};
		Pen.strokeColor = if(hover or: { isOpen }) {
			palette[\spark].copy.alpha_(0.72)
		} {
			palette[\whisper]
		};
		Pen.width = if(isOpen) { CQInk.strokes[\normal] } {
			CQInk.strokes[\hairline]
		};
		Pen.addRoundedRect(body, CQInk.metrics[\buttonRadius],
			CQInk.metrics[\buttonRadius]);
		Pen.stroke;
		Pen.stringLeftJustIn((this.item ? "—").asString,
			Rect(body.left + 7, body.top, (body.width - 28).max(12), body.height),
			menuFont, palette[\line]);
		Pen.strokeColor = if(hover or: { isOpen }) { palette[\spark] } {
			palette[\line]
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
		Pen.fillColor = palette[\ground];
		Pen.fillRect(rect);
		items.do { |choice, index|
			var row = Rect(1, index * rowH, rect.width - 2, rowH);
			if(index == popupHover) {
				Pen.fillColor = palette[\spark].copy.alpha_(0.12);
				Pen.fillRect(row);
				Pen.strokeColor = palette[\spark].copy.alpha_(0.62);
				Pen.width = CQInk.strokes[\hairline];
				Pen.line(row.leftTop, row.leftBottom);
				Pen.stroke;
			};
			Pen.stringLeftJustIn(choice.asString, row.insetBy(7, 0),
				menuFont,
				if(index == popupHover) { palette[\spark] } { palette[\line] });
		};
		Pen.strokeColor = palette[\line].copy.alpha_(0.82);
		Pen.width = CQInk.strokes[\hairline];
		Pen.addRoundedRect(rect.insetBy(0.5, 0.5), CQInk.metrics[\buttonRadius],
			CQInk.metrics[\buttonRadius]);
		Pen.stroke;
	}
}
