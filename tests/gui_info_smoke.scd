// Serverless Qt integration. Capture native composition AFTER show/layout have
// settled, starting with the very first card; offscreen painting can hide a
// QStackLayout takeover. Human visual/playing acceptance: manual_checklist.md.
(
var gui, info, map, ok = true, state = CQState(nil), matrix = CQMatrix(nil);
var config = CQConfig.validate((headless: true));
var check, click, localPoint, capture, captureView, retained;
check = { |condition, text|
	if(condition) { ("ok: " ++ text).postln } { ok = false; ("FAIL: " ++ text).postln };
};
localPoint = { |view, point| map.container.mapToGlobal(point) - view.mapToGlobal(0@0) };
click = { |view, point, button = 0, mods = 0, clicks = 1|
	var p = localPoint.value(view, point);
	view.mouseDownAction.value(view, p.x, p.y, mods, button, clicks);
	view.mouseUpAction.value(view, p.x, p.y, mods, button);
};
capture = { |name, expectCard = true|
	var image, ratio, animations = [info.pulseTask, gui.slotAt(\recTask)].reject(_.isNil);
	var pixelAt = { |view, point|
		var p = view.mapToGlobal(point) - gui.window.view.mapToGlobal(0@0);
		image.getColor((p.x * ratio).asInteger, (p.y * ratio).asInteger)
	};
	var near = { |a, b|
		(a.red - b.red).abs < 0.01 and: { (a.green - b.green).abs < 0.01 }
			and: { (a.blue - b.blue).abs < 0.01 }
	};
	// Qt's synchronous window grab shares the global Pen painter with view
	// callbacks. Quiesce both the info pulse and the 2 Hz REC refresh, then let
	// outstanding paints drain. Reading the private REC task is test-only: the
	// production GUI has no pause switch for its recorder indicator.
	// Animation and input behavior are exercised separately throughout this run.
	animations.do(_.stop); 0.2.wait;
	image = Image.fromWindow(gui.window, gui.window.view.bounds.moveTo(0, 0));
	ratio = image.width / gui.window.view.bounds.width;
	check.value(near.value(pixelAt.value(map.stageView, 4@4), CQInk.palette(\night)[\ground]),
		"native capture leaves the Night landscape exposed: " ++ name);
	if(expectCard) {
		check.value(info.card.bounds.width <= 960
			and: { info.card.bounds.height < (map.stageView.bounds.height - 16) }
			and: { near.value(pixelAt.value(info.card, 6@6), CQInk.palette(\night)[\recess]) },
			"native card is visible and small after show/layout: " ++ name);
		info.illustrations.do { |item|
			var rect = item[\picture], ink = 0;
			(rect.width div: 3).do { |x|
				(rect.height div: 3).do { |y|
					if(near.value(pixelAt.value(info.card, rect.leftTop + Point(1 + (x * 3), 1 + (y * 3))),
						CQInk.palette(\night)[\recess]).not) { ink = ink + 1 };
				};
			};
			check.value(ink > 3, "native illustration contains visible ink: " ++ item[\label] ++ " / " ++ name);
		};
	};
	image.write(Cocoquantus.repoDir +/+ "tests/tmp" +/+ name ++ ".png"); image.free;
	animations.do { |animation| animation.play(AppClock) };
};
CQInkControl.resetDrawErrors;
captureView = { |view|
	var image, point = view.mapToGlobal(0@0) - gui.window.view.mapToGlobal(0@0);
	var animations = [info.pulseTask, gui.slotAt(\recTask)].reject(_.isNil);
	animations.do(_.stop); 0.2.wait;
	image = Image.fromWindow(gui.window, Rect(point.x, point.y, view.bounds.width, view.bounds.height));
	animations.do { |animation| animation.play(AppClock) };
	image
};
{
	try {
		var beforeState, beforeMatrix, v, p, xy, before, task, missed, located = 0;
		var probe, downs = 0, ups = 0, moves = 0, wheels = 0, drops = 0, panics = 0, panic, action;
		var originalOver, originalLeave, imageA, imageB, resets = 0;
		gui = CQGui(state, matrix, config); 0.8.wait;
		gui.window.bounds = Rect(-100000, -100000, 1280, 720); 0.2.wait;
		info = gui.info; map = gui.nightMap;
		beforeState = state.asEvent.copy; beforeMatrix = matrix.asArray;
		check.value(info.model.enabled.not and: { info.card.visible.not }, "info starts off without a card");
		check.value(gui.shell.infoGuideButton.view.visible.not
			and: { gui.shell.infoAdvancedButton.view.visible.not }
			and: { gui.shell.infoShortcutsButton.view.visible.not }, "reading buttons start hidden");
		info.enabled = true; 0.3.wait;
		check.value(info.model.inspecting and: { info.card.visible.not }, "enable only arms inspection");
		check.value(gui.shell.infoGuideButton.label == "basics & first loop"
			and: { gui.shell.infoAdvancedButton.label == "organ & advanced" }
			and: { gui.shell.infoGuideButton.view.bounds.right < gui.shell.infoAdvancedButton.view.bounds.left }
			and: { gui.shell.infoAdvancedButton.view.bounds.right < gui.shell.infoShortcutsButton.view.bounds.left }
			and: { gui.shell.respondsTo(\infoBasicsButton).not }, "both playable guides precede shortcuts");
		check.value(gui.shell.respondsTo(\infoPatchingButton).not, "no separate Patching button remains");
		gui.shell.audioAlert = \serverDown;
		check.value(gui.shell.audioAlertLayout[\textRect].left > gui.shell.infoShortcutsButton.view.bounds.right,
			"audio warning clears all three guide buttons");
		gui.shell.audioAlert = \none;
		before = info.pulse; task = info.pulseTask; 0.3.wait;
		check.value((info.pulse - before).abs > 0.02, "active info keeps breathing");
		capture.value("info-window-ready", false);
		missed = info.regions.reject { |r| CQInfoGroups.owner(r[\target]).notNil };
		check.value(missed.isEmpty, "every landmark and port belongs to a component");
		if(missed.notEmpty) { missed.collect { |r| r[\target] }.postln };
		v = map.shellViews[0]; p = map.geometry[\cocos][0][\center];
		xy = localPoint.value(v, p); imageA = captureView.value(map.routeView);
		v.mouseOverAction.value(v, xy.x, xy.y); 0.1.wait; imageB = captureView.value(map.routeView);
		check.value(info.groupHover == [\group, \coco, 0] and: { imageA.pixels != imageB.pixels }
			and: { info.shown.not }, "hover reveals the whole Coco without opening a card");
		imageA.free; imageB.free;
		2.do { |i|
			var shell = map.shellViews[i], center = map.geometry[\cocos][i][\center];
			var point = localPoint.value(shell, center);
			CQGuiNightMap.crescentNames(i).do { |name|
				var crescent = map.crescentViews[name];
				info.hover(nil); 0.02.wait; imageA = captureView.value(crescent);
				shell.mouseOverAction.value(shell, point.x, point.y); 0.02.wait;
				imageB = captureView.value(crescent);
				check.value(imageA.pixels != imageB.pixels, "whole Coco hover visibly brightens its crescent: " ++ name);
				imageB.free;
				info.hover(nil); 0.02.wait; imageB = captureView.value(crescent);
				check.value(imageA.pixels == imageB.pixels, "leaving Coco restores its crescent: " ++ name);
				imageA.free; imageB.free;
			};
			shell.mouseOverAction.value(shell, point.x, point.y); 0.02.wait;
			capture.value("info-hover-coco-" ++ i, false);
			shell.mouseLeaveAction.value(shell);
		};
		click.value(v, p); 0.1.wait;
		check.value(info.model.target == [\group, \coco, 0], "heart opens the Coco component");
		info.activate(\next); 0.1.wait;
		click.value(map.endpoints[\coco1_spaf][\view], map.geometry[\endpoints][\coco1_spaf]);
		check.value(info.model.target == [\group, \coco, 0] and: { info.model.pageCount == 1 }
			and: { info.model.pageIndex == 0 }, "Coco stays on one complete page");
		click.value(map.castleViews[0], map.geometry[\castles][0][\center]); info.activate(\next);
		click.value(map.castleViews[1], map.geometry[\castles][1][\center]);
		check.value(info.model.target == [\group, \castles, 0] and: { info.model.pageIndex == 1 }, "another castle preserves the anatomy page");
		click.value(map.birdViews[1], map.geometry[\birds][1][\center]); 0.1.wait;
		check.value(info.model.target == [\group, \bird, 1], "one click replaces the component");
		click.value(map.groundView, 20@20); 0.1.wait;
		check.value(info.shown.not and: { info.model.inspecting } and: { info.pulseTask === task }, "background closes only the card");
		[Size(1280, 720), Size(1528, 900)].do { |size|
			gui.window.bounds = Rect(-100000, -100000, size.width, size.height); 0.3.wait;
			CQInfoGroups.all.do { |target|
				info.model.select(target); info.render;
				info.model.pageCount.do { |page|
					var caption = target.asString ++ " / " ++ page ++ " / " ++ size;
					0.06.wait;
					check.value(info.card.visible
						and: { Rect(0, 0, map.stageView.bounds.width, map.stageView.bounds.height).contains(info.card.bounds) }
						and: { info.buttons.last[\rect].bottom <= info.card.bounds.height }
						and: { info.lines.every { |line| line[\y] + CQGuiScale.textBandHeight(info.fontSize) < info.buttons.first[\rect].top } },
						"full card and footer fit: " ++ caption);
					check.value(info.protectedRects.every { |r| info.card.bounds.intersects(r).not }, "subject stays exposed: " ++ caption);
					if(target[1] == \coco) {
						var subject = CQInfoGroups.union(info.protectedRects);
						var gap = if(target[2] == 0) { info.card.bounds.left - subject.right } { subject.left - info.card.bounds.right };
						check.value((gap - 12).abs < 1.1, "Coco card sits directly beside the complete component: " ++ caption);
						check.value(info.diagramRect.width >= 300 and: { info.diagramRect.height >= 360 }, "Coco picture has room to read its controls: " ++ caption);
						check.value(info.rows.size == 10 and: { info.buttons.size == 1 }
							and: { info.buttons.first[\action] == \close }, "all Coco notes share one page without next/back");
						check.value(info.rows[..4].every { |r| r[\rect].left == info.rows.first[\rect].left }
							and: { info.rows[5..].every { |r| r[\rect].left == info.rows[5][\rect].left } }
							and: { info.rows.first[\rect].right < info.rows[5][\rect].left }, "Coco text uses two separate columns");
						check.value(info.rows[1][\picturePoint].x < info.rows[2][\picturePoint].x
							and: { info.rows[2][\picturePoint].x < info.rows[3][\picturePoint].x }
							and: { info.rows[1..3].every { |r| r[\dots].size == 1 } },
							"all three crescents have distinct numbered anchors from left to right");
						check.value(info.rows.every { |r| r[\rect].intersects(info.diagramRect).not }, "Coco prose leaves the whole picture clear");
						check.value(info.rows[8][\dots].notEmpty and: { info.rows[9][\dots].notEmpty }
							and: { info.rows[8][\picturePoint] != info.rows[9][\picturePoint] },
							"Q inlet and bridge have separate numbered controls");
						check.value(CQNightGeometry.distance(info.rows[9][\dots].first,
							CQInfoGroups.project(map.geometry[\endpoints][("coco" ++ (target[2] + 1) ++ "_slotX").asSymbol],
								info.diagramSource, info.diagramRect)) > 14,
							"bridge number leaves its hollow socket visible");
					};
					if(target[1] == \castles) {
						check.value(info.diagramRect.height >= if(page == 0) { 250 } { 190 }, "castle overview and anatomy have enlarged pictures");
					};
					if(target[1] == \tide) {
						check.value(info.diagramSource.isNil and: { info.illustrations.size == 4 },
							"Tide uses four close-ups instead of a shrunken landscape");
						info.illustrations.do { |item, i|
							check.value(item[\picture].width >= 100 and: { item[\picture].height >= 64 }
								and: { info.rows[i][\rect].contains(item[\picture]) },
								"each Tide close-up stays large and belongs to its hover row");
						};
					};
					check.value(info.lines.every { |line| line[\text].bounds(CQInk.font(\label, info.fontSize)).width <= (line[\width] ? (info.card.bounds.width - 32))
						and: { (line[\x] ? 16) + (line[\width] ? (info.card.bounds.width - 32)) <= (info.card.bounds.width - 16) } }, "text fits its own column: " ++ caption);
					info.rows.do { |row|
						var xy = row[\rect].center;
						if(target[1] == \master and: { row === info.rows.first }) {
							imageA = captureView.value(gui.shell.masterView);
						};
						info.card.mouseOverAction.value(info.card, xy.x, xy.y); 0.01.wait;
						check.value(if(row[\refs].notEmpty) { info.focusRow === row } { info.focusRow.isNil },
							"row hover only resolves actual references: " ++ row[\label]);
						if(target[1] == \tide) {
							var at = info.illustrations[info.rows.indexOf(row)][\picture].center;
							info.card.mouseOverAction.value(info.card, at.x, at.y);
							check.value(info.focusRow === row
								and: { info.annotationPoints.size == row[\refs].size },
								"Tide picture hover locates its gill or all four seeds");
						};
						if(target[1] == \master and: { row === info.rows.first }) {
							imageB = captureView.value(gui.shell.masterView);
							check.value(imageA.pixels != imageB.pixels, "Master row visibly locates native controls");
							imageA.free; imageB.free;
						};
						(row[\dots] ? []).do { |dot|
							info.card.mouseOverAction.value(info.card, dot.x, dot.y);
							check.value(info.focusRow === row, "picture number locates the same row");
							check.value(CQNightGeometry.distance(dot, row[\picturePoint]) >= 16,
								"picture number leaves its symbol visible");
							if(row[\axis] == \horizontal) {
								check.value((dot.y - row[\picturePoint].y).abs < 0.01, "sideways leader is horizontal: " ++ caption);
							};
							if(row[\axis] == \vertical) {
								check.value((dot.x - row[\picturePoint].x).abs < 0.01, "up/down leader is vertical: " ++ caption);
							};
						};
						if(row[\refs].notEmpty and: { target[1] != \master }) {
							check.value(info.annotationPoints.notEmpty, "row locates a real part: " ++ row[\label]); located = located + 1;
						};
					};
					info.card.mouseLeaveAction.value(info.card); 0.02.wait;
					check.value(info.focusRow.isNil and: { info.annotationPoints.isEmpty }, "leave clears part locators");
					if(size.width == 1280 or: { [\coco, \castles, \tide].includes(target[1]) }) {
						capture.value("info-group-" ++ target[1] ++ "-" ++ target[2] ++ "-" ++ page ++ "-" ++ size.width);
					};
					if(page < (info.model.pageCount - 1)) { info.activate(\next) };
				};
			};
			[[\shortcuts, 2], [\tour, CQInfoTour.count], [\advanced, CQInfoAdvanced.count]].do { |guide|
				var guideClass = CQInfoState.guideClass(guide[0]);
				info.activate(guide[0]); 0.1.wait;
				guide[1].do { |page|
					check.value(info.card.visible
						and: { info.buttons.last[\rect].bottom <= info.card.bounds.height }
						and: { info.lines.every { |line| line[\y] + CQGuiScale.textBandHeight(info.fontSize) < info.buttons.first[\rect].top } },
						"guide prose and footer fit: " ++ guide[0] ++ page ++ size);
					check.value(info.buttons[..2].collect { |button| button[\action] } == #[back, next, close], "fixed back/next/close slots");
					if(page == 0) {
						info.activateAt(info.buttons[0][\rect].center);
						check.value(info.model.pageIndex == 0, "unavailable back is inert");
					};
					if(page == (guide[1] - 1)) {
						info.activateAt(info.buttons[1][\rect].center);
						check.value(info.model.pageIndex == page, "unavailable next is inert");
					};

					check.value(info.model.playing == guideClass.notNil,
						"toolbar guide entry enables playing; shortcuts stays inspection");
					if(guideClass.notNil) {
						check.value(if(guideClass.step(page)[\side] == 2) {
							(info.card.bounds.center.x - (map.stageView.bounds.width * 0.5)).abs < 1
						} {
							(info.card.bounds.center.x < (map.stageView.bounds.width * 0.5)) == (guideClass.step(page)[\side] == 1)
						}, "guide moves beside the active side or between both Cocos");
						check.value(info.annotationPoints.size == guideClass.cueRefs(page).reject { |ref| ref[0] == \master }.size,
							"every guided target gets a surface cue");
						check.value(info.illustrations.notEmpty and: { info.illustrations.every { |item|
							item[\rect].bottom < info.buttons.first[\rect].top
						} }, "each lesson has visible symbols and semantic references");
						if(guideClass.step(page)[\detailItems].notNil) {
							check.value(info.illustrations[2][\rect].top > info.lines.select { |line| line[\part] == \body }.last[\y]
								and: { info.illustrations[3][\rect].bottom < info.lines.detect { |line| line[\part] == \detail }[\y] },
								"Master depth and eye form a second band above their paragraph");
						};
						info.illustrations.do { |item|
							var at = item[\picture].center, cues = info.annotationPoints, stillCard, litCard;
							check.value(item[\label].bounds(CQInk.font(\label, info.fontSize)).width <= item[\caption].width,
								"picture caption fits without clipping: " ++ item[\label]);
							if(guideClass.step(page)[\concept] == true) { stillCard = captureView.value(info.card) };
							info.card.mouseOverAction.value(info.card, at.x, at.y);
							check.value(info.focusSymbol === item and: { info.symbolRefs.notEmpty }
								and: { info.annotationPoints == cues }, "guide symbol brightens native parts without replacing action cues");
							if(guideClass.step(page)[\concept] == true) {
								check.value(info.annotationPoints.isEmpty, "concept pages add no action brackets");
								litCard = captureView.value(info.card);
								check.value(stillCard.pixels == litCard.pixels, "native symbol highlights never bleed through the guide");
								stillCard.free; litCard.free;
							};
							info.symbolRefs.do { |ref|
								if(ref[0] == \endpoint) { check.value(info.endpointHighlighted(ref[1]), "all matching ports highlight") };
							};
							if(item[\symbol] == \bridge) { capture.value("info-guide-bridge-hover-" ++ size.width) };

						};
						info.card.mouseLeaveAction.value;
						if(guideClass.step(page)[\concept] != true) {
							guideClass.cueRefs(page).reject { |r| r[0] == \master }.do { |ref, i|
								if(ref[0] == \param and: { ["In", "Fb", "Spaf"].any { |suffix| ref[1].asString.endsWith(suffix) } }) {
									check.value(info.markerRect(info.annotationPoints[i], i).width >= 72,
										"action brackets leave crescent adjustment visible");
								};
							};
						};
						if(guideClass.step(page)[\id] == \time) {
							check.value(CQNightGeometry.distance(info.tourPoint(page),
								map.geometry[\cocos][0][\center]) > 40
								and: { CQNightGeometry.distance(info.tourPoint(page),
									map.geometry[\endpoints][\org1]) > 40 },
								"winding cue clears the heart and Organ");
						};
					};
					capture.value("info-" ++ guide[0] ++ "-" ++ page ++ "-" ++ size.width);
					if(page < (guide[1] - 1)) {
						gui.window.view.keyDownAction.value(gui.window.view, 0.asAscii, 0, 0, 0, 16777236); 0.1.wait;
					};
				};
			};
			info.activate(\shortcuts);
		};
		check.value(located > 50, "both layouts exercise part locators");
		info.freshAction = { resets = resets + 1; true };
		info.activate(\tour); info.activate(\fresh);
		check.value(resets == 0 and: { info.freshArmed }, "fresh session first asks within the card without changing music");
		check.value(info.buttons.last[\label] == "reset now", "armed reset has an explicit label");
		info.pageKey(16777234);
		check.value(info.freshArmed and: { info.buttons.last[\label] == "reset now" },
			"unavailable back is inert even while reset confirmation is open");
		info.activate(\next); info.activate(\back);
		check.value(info.freshArmed.not, "navigation cancels reset confirmation");
		info.activate(\fresh); info.activate(\fresh);
		check.value(resets == 1 and: { info.freshArmed.not }, "confirmed reset dispatches exactly once");
		info.activate(\next); info.activate(\fresh);
		check.value(resets == 1, "fresh session exists only on the first step");
		info.activate(\tour); info.activate(\fresh); info.enabled = false; info.enabled = true;
		check.value(info.freshArmed.not, "exiting info cancels reset confirmation");
		info.activate(\advanced); info.activate(\fresh);
		check.value(resets == 1 and: { info.freshArmed }, "advanced fresh session also requires its second click");
		info.activate(\fresh);
		check.value(resets == 2 and: { info.model.guideKind == \advanced }, "advanced reset dispatch keeps its guide identity");
		info.activate(\next); info.activate(\fresh);
		check.value(resets == 2 and: { info.freshArmed.not }, "later advanced pages cannot reset");
		check.value(state.asEvent == beforeState and: { matrix.asArray == beforeMatrix }, "reading and locating never changes music");
		info.enabled = false;
		gui.bananaGrammar.startSource(\tri2, 1);
		gui.bananaGrammar.updateCandidate(\coco2_spaf, 0); gui.bananaGrammar.releaseCandidate;
		gui.window.view.keyDownAction.value(gui.window.view, 26.asAscii, 262144, 26, 0, 90);
		check.value(matrix.getPatch(\tri2, \coco2_spaf) == 0, "Ctrl+Z dispatches undo through the window");
		gui.window.view.keyDownAction.value(gui.window.view, 24.asAscii, 262144, 24, 0, 88);
		check.value(matrix.getPatch(\tri2, \coco2_spaf) == CQBananaGrammar.defaultHandStrength,
			"Ctrl+X control character dispatches redo through the window");
		gui.bananaGrammar.undo;
		probe = TextField(gui.window.view, Rect(10, 10, 100, 24)).string_("text");
		probe.focus; 0.05.wait;
		check.value(gui.prTextHasFocus, "text focus fixture is active");
		gui.bananaGrammar.startSource(\tri3, 1); before = gui.bananaGrammar.handWeight;
		gui.window.view.keyDownAction.value(gui.window.view, $x, 262144);
		check.value(matrix.getPatch(\tri2, \coco2_spaf) == 0
			and: { gui.bananaGrammar.handWeight == before }, "text Ctrl+X neither redoes nor changes a held cable");
		gui.bananaGrammar.escape; probe.focus(false); probe.remove; 0.1.wait;
		check.value(gui.prTextHasFocus.not, "removed text field releases native focus before playing keys");
		gui.window.view.keyDownAction.value(gui.window.view, $x, 262144);
		check.value(matrix.getPatch(\tri2, \coco2_spaf) == CQBananaGrammar.defaultHandStrength,
			"Ctrl+X printable key dispatches redo too");
		gui.bananaGrammar.undo; info.enabled = true;
		matrix.patch(\tri1, \deckA_rate, -0.6); map.overlay.invalidate; 0.1.wait;
		info.select([\cable, \tri1, \deckA_rate], map.groundView, 20@20); 0.1.wait;
		check.value(info.model.target == [\tour, CQInfoTour.indexOf(\ports)] and: { info.model.inspecting }, "cable enters the combined guide in reading mode");
		info.activate(\back);
		check.value(info.model.target == [\tour, CQInfoTour.indexOf(\ports) - 1] and: { info.model.inspecting }, "cable navigation stays read-only");
		gui.shell.infoGuideAction.value;
		check.value(info.model.target == [\tour, 0] and: { info.model.playing }, "combined guide button reopens at the beginning in play mode");
		gui.shell.infoGuideAction.value; 0.1.wait;
		before = state.get(\coco1Hold); click.value(map.shellViews[0], map.geometry[\cocos][0][\center]);
		check.value(state.get(\coco1Hold) != before and: { info.model.target == [\tour, 0] }, "the combined guide remains playable");
		gui.shell.infoGuideAction.value; gui.shell.infoShortcutsAction.value;
		check.value(info.model.inspecting, "shortcuts exits the playable guide");
		probe = UserView(map.stageView, Rect(0, 0, 5, 5)).visible_(false);
		probe.mouseDownAction = { downs = downs + 1 }; probe.mouseUpAction = { ups = ups + 1 };
		probe.mouseMoveAction = { moves = moves + 1 }; probe.mouseWheelAction = { wheels = wheels + 1 };
		probe.canReceiveDragHandler = { true }; probe.receiveDragHandler = { drops = drops + 1 };
		originalOver = {}; originalLeave = {};
		probe.mouseOverAction = originalOver; probe.mouseLeaveAction = originalLeave;
		info.bind(probe, { [\bird, 0] });
		probe.mouseDownAction.value(probe, 1, 1, 0, 0, 1); probe.mouseMoveAction.value(probe, 2, 2, 0);
		probe.mouseWheelAction.value(probe, 1, 1, 0, 0, 1);
		check.value(probe.canReceiveDragHandler.value(probe).not, "inspection declines drops");
		probe.receiveDragHandler.value(probe); info.enabled = false;
		probe.mouseUpAction.value(probe, 2, 2, 0, 0);
		check.value([downs, ups, moves, wheels, drops] == [0, 0, 0, 0, 0], "inspection consumes the whole gesture through exit");
		probe.mouseDownAction.value(probe, 1, 1, 0, 0, 1); probe.mouseUpAction.value(probe, 2, 2, 0, 0);
		check.value([downs, ups] == [1, 1], "normal handlers resume");
		info.enabled = true;
		panic = gui.inkControls.detect { |c| c.label == "PANIC" }; action = panic.action; panic.action = { panics = panics + 1 };
		panic.view.mouseDownAction.value(panic.view, 10, 10, 0, 0, 1);
		panic.view.mouseUpAction.value(panic.view, 10, 10, 0, 0); panic.action = action;
		check.value(panics == 1, "PANIC remains live");
		gui.shell.toggleDrawer; 0.1.wait;
		check.value(info.model.enabled.not and: { info.pulseTask.isNil }, "Drawer cancels info and its clock");
		info.enabled = true; 0.1.wait; check.value(gui.shellState.drawerOpen.not, "info returns to Night");
		gui.window.view.keyDownAction.value(gui.window.view, 27.asAscii, 0, 27); 0.1.wait;
		check.value(info.model.enabled.not and: { info.pulseTask.isNil } and: { gui.shell.infoGuideButton.view.visible.not }
			and: { gui.shell.infoShortcutsButton.view.visible.not }, "Esc exits info");
		check.value(info.paintErrors == 0 and: { CQInkControl.drawErrors == 0 }, "all drawings are error-free");
		info.free;
		check.value(gui.shell.infoGuideAction.isNil and: { gui.shell.infoShortcutsAction.isNil } and: { probe.mouseOverAction === originalOver }
			and: { probe.mouseLeaveAction === originalLeave }, "teardown restores callbacks");
		retained = info; gui.free; 0.2.wait; check.value(retained.pulseTask.isNil, "free stops the clock");
		gui = CQGui(state, matrix, config); 0.4.wait; gui.info.enabled = true;
		retained = gui.info; gui.window.close; 0.2.wait;
		check.value(gui.isClosed and: { retained.pulseTask.isNil }, "native close cleans up");
	} { |error| ok = false; error.reportError; gui !? { gui.free } };
	if(ok) { "CQ_GUI_INFO_PASSED".postln; 0.exit } { "CQ_GUI_INFO_FAILED".postln; 1.exit };
}.fork(AppClock);
)
