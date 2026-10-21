// Click-to-inspect notes inside Night. Inspection consumes a whole mouse
// gesture; only the playable guides restore playing. Confirmed fresh-session
// action delegates to the owning facade; reading never writes music. One clock breathes
// the small info button and subject brackets together.
CQGuiInfo {
	var gui, matrix, map, <model, <card, <ink, <masterInk, bindings, shellCtl, matrixCtl;
	var <note, <buttons, <lines, <fontSize = 12, <paintErrors = 0;
	var <anchor;
	var sourceView, sourceFraction, freed = false, hoverButton;
	var refreshQueued = false;
	var <shown = false;
	var <markerViews, <pulse = 0, <pulseTask;
	var pulseStarted, masterRect;
	var <hoverTarget, hoverSource, <hoverMaster, hoverMasterRect;
	var <illustrations = #[], <focusSymbol;
	var <regions, <rows = #[], <focusRow, <diagramRect, <diagramSource;
	var <protectedRects = #[], <groupHover;
	var <freshArmed = false, freshFailed = false, <>freshAction;

	*new { |gui, matrix| ^super.newCopyArgs(gui, matrix).prBuild }

	*wrap { |text, width, measure|
		var result = List.new, line = "";
		text.split($ ).do { |word|
			var next = if(line.isEmpty) { word } { line ++ " " ++ word };
			if(measure.value(next) > width and: { line.notEmpty }) {
				result.add(line); line = word;
			} { line = next };
		};
		if(line.notEmpty) { result.add(line) };
		^result.asArray
	}

	// Pure layout: the card stays inside Night, above the permanent Master.
	// A pinned note occupies the opposite top corner to its original target.
	*place { |size, point, width, height, pinned = false|
		var gap = 18, x, y;
		width = width.min(size.x - 16).max(1);
		height = height.min(size.y - 16).max(1);
		x = if(point.x < (size.x * 0.5)) { point.x + gap } { point.x - width - gap };
		y = if(point.y + height + gap <= (size.y - 8)) { point.y + gap } { point.y - height - gap };
		if(pinned) {
			x = if(point.x < (size.x * 0.5)) { size.x - width - 16 } { 16 };
			y = 16;
		};
		^Rect(x.clip(8, (size.x - width - 8).max(8)),
			y.clip(8, (size.y - height - 8).max(8)), width, height)
	}

	prBuild {
		var parent, priorResize, priorDraw, buttonView, routeDraw;
		model = CQInfoState(); bindings = List.new; buttons = [];
		freshAction = { gui.freshSession };
		map = gui.nightMap; parent = map.stageView;
		regions = map.infoHitRegions;
		// The outer stage has no layout; its inner host owns the instrument's
		// stack. These children can never become full-stage stack pages.
		markerViews = 8.collect {
			var mark = UserView(parent, Rect(0, 0, 40, 40))
				.background_(CQInk.palette(\night)[\clear]).acceptsMouse_(false).canFocus_(false);
			mark.visible = false;
			mark.drawFunc = { this.prSafeDraw {
				if(focusRow.notNil) {
					CQInkInfoMap.locator(20@20, rows.indexOf(focusRow) + 1, 11)
				} {
					CQInk.drawInfoBrackets(mark.bounds.moveTo(0, 0), this.cuePulse, model.playing)
				}
			} };
			mark
		};
		ink = markerViews[0];
		card = UserView(parent, Rect(0, 0, 360, 200))
			.background_(CQInk.palette(\night)[\recess]).canFocus_(false);
		card.visible = false;
		// Reuse Master's existing paint surfaces. Adding an overlay to its box
		// layout changes its geometry; nested transparent UserViews can also
		// re-enter Qt painting. Text targets use brackets just outside their
		// bounds on the strip; drawn controls paint their own local brackets.
		(gui.infoBindings.collect { |b| b[0] }.select { |v| v.isKindOf(UserView) }
			++ [gui.shell.masterView, gui.masterControls]).do { |view|
			var original = view.drawFunc;
			var binding = gui.infoBindings.detect { |b| b[0] === view };
			bindings.add([view, \drawFunc, original]);
			view.drawFunc = { |v|
				original.value(v);
				if(view === hoverMaster) {
					this.prSafeDraw { CQInk.drawInfoHover(hoverMasterRect) };
				};
				if(view === masterInk) {
					this.prSafeDraw { CQInk.drawInfoBrackets(masterRect, this.cuePulse, model.playing) };
				};
				if(binding.notNil and: { binding[1] != [\master, \panic] }
					and: { focusRow.notNil and: { focusRow[\refs].includesEqual(binding[1]) } }) {
					this.prSafeDraw { CQInk.drawInfoBrackets(view.bounds.moveTo(0, 0), 0.6) };
				};
				if(binding.notNil and: { this.symbolRefs.includesEqual(binding[1]) }) {
					this.prSafeDraw { CQInk.drawInfoHover(view.bounds.moveTo(0, 0)) };
				};
				if(view === gui.shell.masterView) { this.prSafeDraw { this.drawMasterFocus } };
			};
		};
		buttonView = gui.shell.infoButton.view;
		[gui.shell.drawerButton.view].do { |view|
			var original = view.drawFunc;
			bindings.add([view, \drawFunc, original]);
			view.drawFunc = { |v|
				original.value(v);
				if(this.symbolRefs.includesEqual([\ui, \drawer])) {
					this.prSafeDraw { CQInk.drawInfoHover(v.bounds.moveTo(0, 0)) };
				};
			};
		};
		priorDraw = buttonView.drawFunc;
		bindings.add([buttonView, \drawFunc, priorDraw]);
		buttonView.drawFunc = { |v|
			if(model.enabled) {
				this.prSafeDraw {
					CQInk.drawInfoButton(v.bounds.moveTo(0, 0), pulse, gui.shell.infoButton.hover)
				};
			} { priorDraw.value(v) };
		};
		card.drawFunc = { this.prSafeDraw { this.drawCard } };
		card.mouseLeaveAction = { hoverButton = nil; this.focus(nil); this.focusIllustration(nil); card.refresh };
		card.acceptsMouseOver = true;
		card.mouseOverAction = { |v, x, y|
			var at = Point(x, y);
			var next = buttons.detect { |item| item[\enabled] and: { item[\rect].containsPoint(at) } };
			var row = rows.detect { |item|
				item[\refs].notEmpty and: { item[\rect].containsPoint(at)
					or: { (item[\dots] ? []).any { |p| CQNightGeometry.distance(p, at) < 10 } } }
			};
			this.hover(nil);
			this.focus(row);
			this.focusIllustration(if(model.isGuide) {
				illustrations.detect { |item| item[\rect].containsPoint(at) }
			} { nil });
			if(next != hoverButton) { hoverButton = next; card.refresh };
		};
		card.mouseDownAction = { |v, x, y, mods, button|
			if((button ? 0) == 0) { this.activateAt(Point(x, y)) };
		};
		this.bindTree(map.container, { |v, x, y|
			CQInfoGroups.at(map.prStagePoint(v, x, y), map.geometry, map.infoTarget(v, x, y))
		});
		gui.infoBindings.do { |binding|
			this.bindTree(binding[0], { binding[1] })
		};
		this.bindTree(gui.shell.masterView, { nil });
		gui.shell.infoOutsideViews.do { |view| this.bind(view, { nil }) };
		// The inner container is the last layout pass and owns the geometry.
		// The outer stage can resize before that geometry has caught up.
		priorResize = map.container.onResize;
		map.container.onResize = { |v|
			priorResize.value(v);
			if(freed.not) { { if(freed.not) { this.relayout } }.defer };
		};
		bindings.add([map.container, \onResize, priorResize]);
		shellCtl = SimpleController(gui.shellState);
		shellCtl.put(\drawer, {
			// The card belongs to Night. Close it on either stage transition;
			// a hidden note never returns above a different interaction.
			this.dismiss;
			if(gui.shellState.drawerOpen) { this.enabled = false };
		});
		gui.shell.infoAction = { this.enabled = model.enabled.not };
		gui.shell.infoGuideAction = { this.activate(\tour) };
		gui.shell.infoAdvancedAction = { this.activate(\advanced) };
		gui.shell.infoShortcutsAction = { this.activate(\shortcuts) };
		routeDraw = map.routeView.drawFunc;
		bindings.add([map.routeView, \drawFunc, routeDraw]);
		map.routeView.drawFunc = { |v|
			routeDraw.value(v);
			this.prSafeDraw {
				Pen.use {
					if(shown and: { model.target[0] == \group }) {
						CQInkInfoMap.outline(model.target, map.geometry, 0.35);
					};
					groupHover !? { CQInkInfoMap.outline(groupHover, map.geometry, 0.85) };
					this.symbolRefs.do { |ref|
						if(#[group, bird, preamp, eye, moon, castle].includes(ref[0])) {
							CQInkInfoMap.outline(CQInfoGroups.owner(ref), map.geometry, 1, 2.2)
						};
						if(ref[0] == \cables) { this.drawCableFocus(ref[1]) };
						if(ref[0] == \cable and: { matrix.getPatch(ref[1], ref[2]) != 0 }) {
							CQInk.drawCableFocus(map.geometry[\endpoints][ref[1]], map.geometry[\endpoints][ref[2]], \night)
						};
					};
				};
			};
		};
		matrixCtl = SimpleController(matrix);
		#[patch, matrix, clear].do { |key|
			matrixCtl.put(key, {
				if(freed.not and: { model.enabled } and: { refreshQueued.not }) {
					refreshQueued = true;
					AppClock.sched(0, {
						refreshQueued = false;
						if(freed.not) {
							this.hover(nil);
							if(shown and: { model.target[0] == \cable }) { this.render };
						};
						nil
					});
				};
			});
		};
		^this
	}

	bindTree { |view, resolver|
		if(bindings.any { |b|
			b[0] == view and: { b[1] == \mouseDownAction }
		}) { ^this };
		this.bind(view, resolver);
		view.children.do { |child| this.bindTree(child, resolver) };
		^this
	}
	bind { |view, resolver|
		var over = view.mouseOverAction, down = view.mouseDownAction, move = view.mouseMoveAction;
		var up = view.mouseUpAction, wheel = view.mouseWheelAction;
		var canDrop = view.canReceiveDragHandler, drop = view.receiveDragHandler;
		var leave = view.mouseLeaveAction, acceptsOver = view.acceptsMouseOver;
		var swallowed = false;
		bindings.addAll([
			[view, \acceptsMouseOver, acceptsOver],
			[view, \mouseOverAction, over], [view, \mouseLeaveAction, leave],
			[view, \mouseDownAction, down], [view, \mouseMoveAction, move],
			[view, \mouseUpAction, up], [view, \mouseWheelAction, wheel],
			[view, \canReceiveDragHandler, canDrop], [view, \receiveDragHandler, drop]
		]);
		view.acceptsMouseOver = true;
		view.mouseOverAction = { |v, x, y|
			// Keep each shape's own hover ink and its actual hit resolution.
			over.value(v, x, y);
			if(freed.not and: { model.inspecting }) {
				this.hover(resolver.value(v, x, y), v);
			};
		};
		view.mouseLeaveAction = { |v|
			leave.value(v);
			if(hoverSource === v) { this.hover(nil) };
		};
		view.mouseDownAction = { |v, x, y, mods, button, clicks|
			var target;
			swallowed = false;
			if(freed.not and: { model.inspecting }) {
				// Resolve at the press coordinates, even without a preceding hover.
				// PANIC remains a real silence control in every mode.
				over.value(v, x, y); target = resolver.value(v, x, y);
				this.hover(target, v);
				if(target != [\master, \panic]) {
					swallowed = true;
					// A target replaces the note immediately. Empty background
					// closes only the card; neither gesture can play through.
					if((button ? 0) == 0 and: { target.notNil }) {
						this.select(target, v, Point(x, y));
					} { this.dismiss };
				} {
					if(shown) { this.dismiss };
				};
			};
			if(swallowed) { true } { down.value(v, x, y, mods, button, clicks) }
		};
		view.mouseMoveAction = { |v, x, y, mods|
			if(swallowed or: { model.inspecting }) { true } { move.value(v, x, y, mods) }
		};
		view.mouseUpAction = { |v, x, y, mods, button|
			// An Esc or toolbar exit during a held inspection must not deliver
			// an orphan release to the musical gesture that never received down.
			if(swallowed) { swallowed = false; true } { up.value(v, x, y, mods, button) }
		};
		view.mouseWheelAction = { |v, x, y, mods, xd, yd|
			if(model.inspecting) { true } { wheel.value(v, x, y, mods, xd, yd) }
		};
		if(canDrop.notNil) {
			view.canReceiveDragHandler = { |v| if(model.inspecting) { false } { canDrop.value(v) } };
		};
		if(drop.notNil) {
			view.receiveDragHandler = { |v| if(model.inspecting.not) { drop.value(v) } };
		};
		^this
	}

	enabled_ { |flag|
		freshArmed = false; freshFailed = false;
		if(freed) { ^this };
		if(flag == true and: { gui.shellState.drawerOpen }) { gui.shell.toggleDrawer };
		if(flag == true) { gui.cancelAudition };
		model.enabled = flag;
		this.hover(nil);
		this.updatePulse;
		anchor = Point(map.stageView.bounds.width * 0.5, 32);
		sourceView = nil;
		this.render;
		^this
	}

	updatePulse {
		if(model.enabled and: { freed.not }) {
			if(pulseTask.isNil) {
				pulseStarted = Main.elapsedTime;
				pulseTask = Routine {
					while { freed.not and: { model.enabled } } {
						pulse = CQInk.infoPulse(Main.elapsedTime - pulseStarted);
						gui.shell.infoButton.view.refresh;
						markerViews.do { |view|
							// Native close can invalidate a child before the
							// owner's close callback stops this routine.
							if(view.visible == true) { view.refresh };
						};
						masterInk !? { if(masterInk.visible == true) { masterInk.refresh } };
						(1 / 15).wait;
					};
				}.play(AppClock);
			};
		} {
			pulseTask !? { pulseTask.stop }; pulseTask = nil; pulse = 0;
		};
		^this
	}

	select { |target, view, point|
		var next = CQInfoGroups.owner(target);
		if(freed or: { next.isNil }) { ^this };
		if(next == model.target) { ^this };
		if(model.select(next).not) { ^this };
		sourceView = view;
		sourceFraction = Point(point.x / view.bounds.width.max(1), point.y / view.bounds.height.max(1));
		this.render;
		^this
	}
	dismiss {
		freshArmed = false; freshFailed = false;
		model.dismiss; sourceView = nil;
		this.render;
		^this
	}

	activateAt { |point|
		var button = buttons.detect { |item| item[\enabled] and: { item[\rect].containsPoint(point) } };
		button !? { this.activate(button[\action]) };
		^this
	}
	activate { |action|
		if(freed or: { model.enabled.not }) { ^this };
		if(action == \back and: { model.canPage(-1).not }) { ^this };
		if(action == \next and: { model.canPage(1).not }) { ^this };
		if(action != \fresh) { freshArmed = false; freshFailed = false };
		switch(action,
			\fresh, {
				if(model.isGuide and: { model.pageIndex == 0 }) {
					if(freshArmed) {
						freshArmed = false;
						freshFailed = freshAction.value != true;
					} { freshArmed = true; freshFailed = false };
				};
			},
			\more, { model.more },
			\tour, { model.tour(0); sourceView = nil },
			\advanced, { model.advanced(0); sourceView = nil },
			\shortcuts, { model.shortcuts(0); sourceView = nil; this.hover(nil) },
			\back, { model.page(-1) },
			\next, { model.page(1) },
			\close, { this.dismiss });
		this.render;
		^this
	}

	render {
		var size, width, height, step, font, y, footer;
		if(freed) { ^this };
		illustrations = [];
		this.focusIllustration(nil);
		rows = []; diagramRect = nil; diagramSource = nil; protectedRects = [];
		this.focus(nil);
		gui.shell.infoStatus(model.enabled, model.playing);
		gui.hoverHelp !? { gui.hoverHelp.infoStatus(model.enabled, model.playing, model.guideKind) };
		if(model.inspecting.not) { this.hover(nil) };
		shown = model.enabled and: { model.target.notNil } and: { gui.shellState.drawerOpen.not };
		if(shown.not) {
			// Queued visibility avoids Qt's synchronous mouse-leave reentry.
			card.setProperty(\visible, false, false);
			this.layoutInk;
			map.routeView.refresh;
			^this
		};
		if(#[group, shortcuts].includes(model.target[0])) { ^this.renderPage };
		note = CQInfoText.topic(model.target, matrix);
		if(note.isNil) { model.dismiss; ^this.render };
		if(model.isGuide and: { model.pageIndex == 0 }) {
			if(freshArmed) {
				note[\detail] = "Reset controls, cables, loaded files and loop contents? Recording stops. Saved files and audio setup stay. Click reset now to start again; next or close cancels.";
			};
			if(freshFailed) { note[\detail] = "Fresh session needs a running audio engine. Open Drawer > audio to recover it, then return here." };
		};
		size = map.stageView.bounds.size;
		step = CQGuiScale.stepFor(size.width, size.height);
		fontSize = CQGuiScale.metrics(step)[\label];
		font = CQInk.font(\label, fontSize);
		width = if(step == \roomy) { 460 } { 400 };
		lines = List.new; y = 16;
		#[title, gesture, body, detail].do { |part|
			if(part == \detail and: { note[\detailItems].notNil }) {
				var extra = CQInkInfo.layoutItems(note[\detailItems], 16, y, width - 32,
					fontSize, 2, fontSize * 5);
				illustrations = illustrations ++ extra;
				y = extra.last[\rect].bottom + 10;
			};
			if(part != \detail or: { model.expanded } or: { model.pageCount > 0 }) {
				var text = note[part] ? "";
				if(text.notEmpty) {
					this.class.wrap(text, width - 32, { |s| s.bounds(font).width }).do { |line|
						lines.add((text: line, y: y, part: part));
						y = y + CQGuiScale.textBandHeight(fontSize);
					};
					y = y + 8;
				};
			};
			if(part == \gesture and: { model.isGuide }) {
				illustrations = CQInkInfo.tourLayout(model.target[1], 16, y, width - 32, fontSize, model.guideClass);
				y = illustrations.last[\rect].bottom + 12;
			};
		};
		footer = y + 4; height = footer + 40;
		this.layoutFooter(footer, width);
		if(model.isGuide) { anchor = this.tourPoint(model.target[1]) } {
			// Keep the original point on its owning view as that view is laid out
			// again. An annotation must not stay at the old stage pixels.
			if(sourceView.notNil and: { sourceView.isClosed.not } and: { sourceFraction.notNil }) {
				anchor = sourceView.mapToGlobal(Point(sourceFraction.x * sourceView.bounds.width,
					sourceFraction.y * sourceView.bounds.height)) - map.stageView.mapToGlobal(0@0);
			};
		};
		anchor = anchor ? Point(size.width * 0.5, 32);
		card.setProperty(\geometry, if(model.isGuide and: { note[\side] == 2 }) {
			Rect((size.width - width) * 0.5, 16, width, height)
		} {
			this.class.place(Point(size.width, size.height), anchor, width, height, model.isGuide)
		}, false);
		this.layoutInk;
		card.setProperty(\visible, true, false);
		card.invokeMethod(\raise, synchronous: false);
		hoverButton = nil;
		card.refresh;
		map.routeView.refresh;
		^this
	}

	renderPage {
		var size = map.stageView.bounds.size, group = model.target[0] == \group;
		var step = CQGuiScale.stepFor(size.width, size.height), placed, candidates, height;
		var preferRight, page = model.pageIndex;
		note = if(group) { CQInfoPages.page(model.target, page) } {
			CQInfoPages.shortcuts(page)
		};
		fontSize = CQGuiScale.metrics(step)[\label];
		if(group) {
			protectedRects = CQInfoGroups.protected(model.target, map.geometry, regions);
		};
		preferRight = protectedRects.notEmpty and: {
			CQInfoGroups.union(protectedRects).center.x < (size.width * 0.5)
		};
		candidates = if(step == \roomy) { [500, 440, 380, 340, 310] } { [440, 400, 360, 320, 290] };
		if(group and: { model.target[1] == \coco }) { candidates = [960, 880, 800, 760, 720] };
		candidates.do { |width|
			if(placed.isNil) {
				height = this.layoutPage(width);
				placed = CQInfoGroups.place(Point(size.width, size.height), width, height, protectedRects, preferRight);
			};
		};
		if(placed.isNil) {
			// At a narrow side slot, keep the compact type step rather than
			// hiding lines or laying the card over its protected subject.
			fontSize = CQGuiScale.metrics(\compact)[\label];
			if(group and: { model.target[1] == \coco }) {
				[960, 880, 800, 760, 720]
			} { [440, 400, 360, 320, 290, 260] }.do { |width|
				if(placed.isNil) {
					height = this.layoutPage(width);
					placed = CQInfoGroups.place(Point(size.width, size.height), width, height, protectedRects, preferRight);
				};
			};
		};
		if(placed.isNil) {
			// Supported sizes are covered by the layout gate. Fail closed for
			// an unsupported native geometry instead of obscuring the subject.
			card.setProperty(\visible, false, false);
			^this
		};
		anchor = if(protectedRects.isEmpty) { 0@0 } { CQInfoGroups.union(protectedRects).center };
		card.setProperty(\geometry, placed, false);
		this.layoutInk;
		card.setProperty(\visible, true, false);
		card.invokeMethod(\raise, synchronous: false);
		card.refresh; map.routeView.refresh;
		^this
	}

	layoutPage { |width|
		var y = 16, font = CQInk.font(\label, fontSize), band = CQGuiScale.textBandHeight(fontSize);
		var addText, footer, dots, occupied = List.new;
		var group = model.target[0] == \group;
		var coco = group and: { model.target[1] == \coco };
		var tide = group and: { model.target[1] == \tide };
		var bodyTop, columnX, columnWidth, leftBottom, pictureWidth, split;
		lines = List.new; rows = []; illustrations = []; diagramRect = nil; diagramSource = nil;
		addText = { |text, part, atX = 16, textWidth|
			textWidth = textWidth ? (width - 32);
			this.class.wrap(text, textWidth, { |s| s.bounds(font).width }).do { |line|
				lines.add((text: line, x: atX, y: y, width: textWidth, part: part)); y = y + band;
			};
		};
		addText.value(note[\title] ++ if(model.pageCount > 1 and: { group }) {
			" / " ++ (model.pageIndex + 1) ++ " of " ++ model.pageCount
		} { "" }, \title);
		y = y + 8; addText.value(note[\intro], \body); y = y + 8;
		if(group and: { #[master, tide].includes(model.target[1]).not }) {
			diagramSource = CQInfoGroups.diagramBounds(model.target, model.pageIndex, map.geometry, regions);
			if(coco) {
				// Spend width on a readable drawing and two independent prose
				// columns. Controls and their inputs stay together on one page.
				addText.value("Hover a number or paragraph to locate its parts.", \hint); y = y + 10;
				bodyTop = y;
				pictureWidth = ((width - 32) * 0.44).floor;
				diagramRect = Rect(16, bodyTop, pictureWidth, if(fontSize > 12) { 440 } { 360 });
				columnX = diagramRect.right + 16;
				columnWidth = (width - columnX - 32) * 0.5;
			} {
				diagramRect = Rect(20, y, width - 40, if(model.target[1] == \castles) {
					if(model.pageIndex == 0) { if(fontSize > 12) { 320 } { 250 } } {
						if(fontSize > 12) { 240 } { 190 }
					}
				} { if(fontSize > 12) { 128 } { 100 } });
				y = diagramRect.bottom + 6;
				addText.value("Hover a line or picture number to locate it.", \hint);
				y = y + 6;
			};
		};
		if(tide) {
			addText.value("Hover a picture or row to locate it on Night.", \hint); y = y + 8;
		};
		rows = note[\rows].collect { |row, i|
			var top, result = row.copy, atX = 16, textWidth = width - 32;
			if(coco) {
				split = (note[\rows].size / 2).ceil.asInteger;
				if(i == split) { leftBottom = y; y = bodyTop };
				atX = columnX + if(i < split) { 0 } { columnWidth + 16 };
				textWidth = columnWidth;
			};
			if(tide) { atX = 140; textWidth = width - atX - 16 };
			top = y;
			addText.value(if(group) { (i + 1).asString ++ ". " } { "" }
				++ row[\label] ++ ": " ++ row[\text], \body, atX, textWidth);
			if(tide) {
				var symbol = [\seedStates, \tideFocus, \tideBlend, \tideBreed][i];
				var item = (symbol: symbol, label: "", picture: Rect(20, top + 2, 104, 64));
				y = max(y, top + 72);
				item[\rect] = Rect(16, top, 112, y - top);
				illustrations = illustrations.add(item);
			};
			result[\rect] = Rect(atX - 4, top, textWidth + 8, y - top);
			if(tide) { result[\rect] = Rect(16, top, width - 32, y - top) };
			if(diagramSource.notNil) {
				dots = CQInfoGroups.points(row[\refs], regions).select { |p| diagramSource.containsPoint(p) };
				// One mark per row in the miniature; all referenced parts light
				// on the real surface, including the five equivalent castles.
				result[\dots] = [];
				if(dots.notEmpty) {
					var at = CQInfoGroups.project(dots.first, diagramSource, diagramRect);
					var label = CQInfoGroups.labelPoint(at, diagramRect, occupied, row[\axis]);
					result[\picturePoint] = at; result[\dots] = [label];
					occupied.add(label);
				};
			};
			y = y + if(coco) { 10 } { 7 };
			result
		};
		if(coco) { y = max(max(y, leftBottom), diagramRect.bottom + 7) };
		footer = y + 3;
		this.layoutFooter(footer, width);
		^footer + 40
	}
	layoutFooter { |top, width|
		buttons = model.footerItems.collect { |item, i|
			(label: item[0], action: item[1], enabled: item[2], rect: Rect(16 + (i * 76), top, 68, 28))
		};
		if(model.isGuide and: { model.pageIndex == 0 }) {
			buttons = buttons.add((label: if(freshArmed) { "reset now" } { "fresh session" },
				action: \fresh, enabled: true, rect: Rect(width - 140, top, 124, 28)));
		};
		hoverButton = nil;
	}
	// Arrow keys are local to an open multipage card. Text entry, Drawer and
	// the rest of the performance key map retain their existing owners.
	pageKey { |key|
		if(shown.not or: { model.pageCount < 2 }) { ^false };
		if(key == 16777234) { this.activate(\back); ^true };
		if(key == 16777236) { this.activate(\next); ^true };
		^false
	}
	cuePulse { ^if(model.playing) { CQInk.infoPulse((Main.elapsedTime - pulseStarted) * 2) } { pulse } }
	symbolRefs { ^focusSymbol !? { focusSymbol[\refs] } ? [] }
	focusIllustration { |item|
		if(item === focusSymbol) { ^this };
		focusSymbol = item;
		this.layoutInk;
		map.routeView.refresh; card.refresh;
		map.endpoints.values.do { |e| e[\view].refresh };
		map.crescentViews.values.do(_.refresh);
		map.shellViews.do(_.refresh); map.dustViews.do(_.refresh);
		gui.infoBindings.do { |binding| if(binding[0].isKindOf(UserView)) { binding[0].refresh } };
		gui.shell.drawerButton.view.refresh;
		^this
	}
	drawCableFocus { |polarity|
		matrix.asArray.do { |patch|
			var weight = patch[2], a = map.geometry[\endpoints][patch[0]], b = map.geometry[\endpoints][patch[1]];
			if(a.notNil and: { b.notNil } and: {
				polarity == \all or: { (polarity == \positive) == (weight > 0) }
			}) { CQInk.drawCableFocus(a, b, \night) };
		};
	}

	focus { |row|
		if(row === focusRow) { ^this };
		focusRow = row;
		this.layoutInk;
		card.refresh; gui.shell.masterView.refresh;
		gui.infoBindings.do { |binding| if(binding[0].isKindOf(UserView)) { binding[0].refresh } };
		map.endpoints.values.do { |e| e[\view].refresh };
		^this
	}
	endpointHighlighted { |name|
		^model.enabled and: {
			(focusRow.notNil and: { focusRow[\refs].includesEqual([\endpoint, name]) })
				or: { this.symbolRefs.includesEqual([\endpoint, name]) }
				or: { this.groupHighlighted([\endpoint, name]) }
		}
	}
	groupHighlighted { |target|
		^model.enabled and: {
			(groupHover.notNil and: { CQInfoGroups.owner(target) == groupHover })
				or: { this.symbolRefs.includesEqual(target) }
				or: { this.symbolRefs.includesEqual(CQInfoGroups.owner(target)) }
		}
	}
	drawMasterFocus {
		var refs;
		if(focusRow.isNil) { ^this };
		refs = focusRow[\refs].select { |r| r[0] == \master };
		gui.infoBindings.do { |binding|
			var view = binding[0], rect;
			if(refs.includesEqual(binding[1]) and: { view.isKindOf(UserView).not }) {
				rect = view.bounds.moveToPoint(view.mapToGlobal(0@0) - gui.shell.masterView.mapToGlobal(0@0));
				CQInk.drawInfoBrackets(rect.insetBy(-9, -9), 0.6);
			};
		};
	}

	relayout {
		if(freed) { ^this };
		regions = map.infoHitRegions;
		this.hover(nil);
		// Selected notes follow their view and reflow inside the resized stage.
		this.render;
		^this
	}

	// Night already paints its shape/cable hover from the shared hit resolver.
	// Group hover also reaches sibling crescents, sockets and Master fields.
	// Never create an overview of every target or change the selected card.
	hover { |target, view|
		var priorSource = hoverSource, priorMaster = hoverMaster;
		if(freed) { ^this };
		if(model.inspecting.not or: { target == [\master, \panic] }) { target = nil };
		if(target.isNil) { view = nil };
		if(target == hoverTarget and: { view === hoverSource }) { ^this };
		hoverTarget = target; hoverSource = view;
		groupHover = CQInfoGroups.owner(target);
		if(groupHover.notNil and: { groupHover[0] != \group }) { groupHover = nil };
		hoverMaster = nil; hoverMasterRect = nil;
		if(groupHover == [\group, \master, 0]) {
			hoverMaster = gui.masterControls;
			hoverMasterRect = Rect(8, 3, hoverMaster.bounds.width - 16, 16);
		};
		[priorSource, hoverSource, priorMaster, hoverMaster].reject(_.isNil).asSet.do(_.refresh);
		map.routeView.refresh;
		map.endpoints.values.do { |e| e[\view].refresh };
		map.crescentViews.values.do(_.refresh);
		^this
	}

	tourPoint { |step|
		var geo = map.geometry, guide = model.guideClass ? CQInfoTour;
		if(geo.isNil) { ^Point(50, 50) };
		if(guide.step(step)[\side] == 2) { ^Point(geo[\width] * 0.5, 32) };
		// The card stays opposite the active Coco, including both patching
		// steps. Right-Coco lessons deliberately move it to the left.
		^if(guide.step(step)[\id] == \time) { CQNightGeometry.spiralPoint(geo[\cocos][0], 0.8) } {
			geo[\cocos][guide.step(step)[\side]][\center]
		}
	}

	drawCard {
		var p = CQInk.palette(\night), font = CQInk.font(\label, fontSize);
		var bounds = card.bounds.moveTo(0, 0);
		// Always occlude surface ink, including during queued marker relayout.
		Pen.fillColor = p[\recess]; Pen.fillRect(bounds);
		Pen.strokeColor = p[\line]; Pen.width = CQInk.strokes[\hairline];
		Pen.strokeRect(bounds.insetBy(0.5, 0.5));
		illustrations.do { |item| CQInkInfo.draw(item, map.geometry, fontSize) };
		if(diagramSource.notNil) {
			CQInkInfoMap.diagram(model.target, model.pageIndex, map.geometry, regions, diagramSource, diagramRect);
		};
		rows.do { |row, i|
			(row[\dots] ? []).do { |point|
				Pen.strokeColor = p[\whisper]; Pen.width = 0.7;
				Pen.line(row[\picturePoint], point); Pen.stroke;
				if(row === focusRow) { CQInkInfoMap.locator(point, i + 1, 7) } {
					Pen.fillColor = p[\recess]; Pen.addOval(Rect.aboutPoint(point, 7, 7)); Pen.fill;
					Pen.stringCenteredIn((i + 1).asString, Rect.aboutPoint(point, 8, 8), CQInk.font(\label, 10), p[\spark]);
				};
			};
		};
		lines.do { |line|
			Pen.stringAtPoint(line[\text], Point(line[\x] ? 16, line[\y]), font,
				p[if(#[title, label].includes(line[\part])) { \spark } { \line }]);
		};
		buttons.do { |button|
			if(button[\action] == \fresh) {
				Pen.fillColor = CQInk.infoColor(if(button == hoverButton) { 0.8 } { 0.4 }).copy.alpha_(0.2);
				Pen.fillRect(button[\rect]);
			};
			Pen.strokeColor = p[if(button == hoverButton) { \spark } { \whisper }]
				.copy.alpha_(if(button[\enabled]) { 1 } { 0.4 });
			Pen.strokeRect(button[\rect]);
			Pen.stringCenteredIn(button[\label], button[\rect], font,
				p[if(button[\enabled]) { \spark } { \whisper }]);
		};
	}

	layoutInk {
		var view, priorMaster = masterInk, points = this.annotationPoints;
		markerViews.do { |mark, i|
			var at = points.at(i);
			if(at.notNil) {
				mark.setProperty(\geometry, this.markerRect(at, i), false);
				mark.invokeMethod(\raise, synchronous: false);
			};
			mark.setProperty(\visible, at.notNil, false);
			if(at.notNil) { mark.refresh };
		};
		if(shown) {
			view = if(model.playing and: { model.guideClass.cueRefs(model.pageIndex).includesEqual([\master, \masterDry]) }) {
				gui.infoBindings.detect { |binding| binding[1] == [\master, \masterDry] }[0]
			} {
				if(model.target[0] == \master) { sourceView } { nil }
			};
		};
		masterInk = nil; masterRect = nil;
		view !? {
			masterInk = if(view.isKindOf(UserView)) { view } { gui.shell.masterView };
			masterRect = Rect.fromPoints(view.mapToGlobal(0@0) - masterInk.mapToGlobal(0@0),
				view.mapToGlobal(Point(view.bounds.width, view.bounds.height))
					- masterInk.mapToGlobal(0@0));
			if(masterInk !== view) { masterRect = masterRect.insetBy(-9, -9) };
		};
		priorMaster !? { priorMaster.refresh };
		masterInk !? { masterInk.refresh };
		if(shown) { card.invokeMethod(\raise, synchronous: false) };
		^this
	}
	markerRect { |at, index|
		var refs, ref, name, radius = 20;
		if(model.playing) {
			refs = model.guideClass.cueRefs(model.pageIndex).reject { |r| r[0] == \master };
			ref = refs.at(index);
			if(ref.notNil and: { ref[0] == \param }) {
				name = ref[1].asString;
				if(["In", "Fb", "Spaf"].any { |suffix| name.endsWith(suffix) }) { radius = 36 };
				if(name.endsWith("Organ")) { radius = 36 };
			};
		};
		^Rect.aboutPoint(at, radius, radius)
	}

	annotationPoints {
		var target = model.target, geo = map.geometry;
		var points = List.new, name, suffix, n, point;
		if(shown.not or: { target.isNil } or: { geo.isNil }) { ^[] };
		if(focusRow.notNil) { ^CQInfoGroups.points(focusRow[\refs], regions) };
		if(#[group, shortcuts].includes(target[0])) { ^[] };
		if(model.isGuide) {
			^if(model.playing) { model.guideClass.cueRefs(target[1]).collect { |ref|
				if(ref[0] == \cable) {
					var a = geo[\endpoints][ref[1]], b = geo[\endpoints][ref[2]];
					CQInk.cablePoints(a, b)[12]
				} {
					if(ref[0] == \param and: { ref[1].asString.endsWith("Speed") }) {
						CQNightGeometry.spiralPoint(geo[\cocos][ref[1].asString[4].digit - 1], 0.8)
					} { CQInfoGroups.points([ref], regions).first }
				}
			}.reject(_.isNil) } { [] }
		};
		if(target[0] == \master) { ^[] };
		point = anchor;
		point !? { points.add(point) };
		if(target[0] == \cable) {
			points = [geo[\endpoints][target[1]], geo[\endpoints][target[2]]].asList;
		};
		if(target[0] == \param) {
			name = target[1].asString;
			if(name.beginsWith("coco")) {
				n = name[4].digit - 1;
				suffix = (Spaf: \spaf, In: \inVca, Fb: \fbVca, Organ: \organ,
					SlotQ: \slotQ, SlotX: \slotX, Dust: \dust, Hold: \record)[name.drop(5).asSymbol];
				suffix !? { points.add(geo[\endpoints][("coco" ++ (n + 1) ++ "_" ++ suffix).asSymbol]) };
				if(name.endsWith("SlotX")) { points.add(geo[\cocos][1 - n][\mouth]) };
				if(name.endsWith("SlotQ")) { points.add(geo[\eye]) };
			};
		};
		if(target[0] == \castle) {
			["tri", "cas"].do { |prefix| points.add(geo[\endpoints][(prefix ++ (target[1] + 1)).asSymbol]) };
		};
		^points.reject(_.isNil).asArray
	}
	prSafeDraw { |func|
		try { if(freed.not) { func.value } } { |error|
			paintErrors = paintErrors + 1; error.reportError
		};
	}
	free {
		if(freed) { ^this };
		freed = true; model.enabled = false;
		freshAction = nil;
		illustrations = [];
		this.updatePulse;
		rows = []; focusRow = nil; focusSymbol = nil; groupHover = nil; regions = [];
		hoverTarget = nil; hoverSource = nil; hoverMaster = nil; hoverMasterRect = nil;
		shellCtl !? { shellCtl.remove }; shellCtl = nil;
		matrixCtl !? { matrixCtl.remove }; matrixCtl = nil;
		bindings.reverseDo { |binding|
			if(binding[0].isClosed.not) { binding[0].perform(binding[1].asSetter, binding[2]) };
		};
		bindings.clear;
		gui.shell.infoAction = nil;
		gui.shell.infoGuideAction = nil;
		gui.shell.infoAdvancedAction = nil;
		gui.shell.infoShortcutsAction = nil;
		// Native children close with their owner. Do not remove a Qt child in
		// an onClose callback while the native parent is already being destroyed.
		card.drawFunc = nil;
		markerViews.do { |view| view.drawFunc = nil };
		^this
	}
}
