// CQGuiBananaController — Qt adapter for CQBananaGrammar (GUI_SPEC §4/§11).
// Endpoint and cable/ground hit tests live here; the visual cable layer remains
// acceptsMouse=false, so knobs and buttons underneath keep their own gestures.

CQGuiBananaController {
	var grammar, endpoints, bodies, overlay, controls, controlViews;
	var gestureCtl, matrixCtl;
	// With one grammar feeding two stages (Day and Night), the hidden stage's
	// per-gesture endpoint refreshes are pure language-to-Qt bridge waste: Qt
	// repaints the whole stage anyway when the hard cut shows it. nil = always.
	var <>refreshGate;

	*new { |grammar, endpoints, bodies, overlay, controls|
		^super.newCopyArgs(grammar, endpoints, bodies, overlay, controls).prInit
	}

	free {
		gestureCtl !? { gestureCtl.remove };
		matrixCtl !? { matrixCtl.remove };
		gestureCtl = nil;
		matrixCtl = nil;
	}

	bindGround { |root|
		var previousLeave = root.mouseLeaveAction;
		this.prBindGround(root, nil);
		root.mouseLeaveAction = { |view|
			previousLeave.value(view);
			grammar.hoverCable = nil
		};
		^this
	}

	// Public ground delegates for views that own their first gesture pass (the
	// §6.1 night touch bodies): whatever the touch grammar does not claim falls
	// through here, so cables, harvest, solo and hover behave identically on
	// every kind of paper.
	groundDown { |view, x, y, modifiers = 0, button = 0, clicks = 1, bodyName|
		^this.prGroundDown(view, x, y, modifiers, button, clicks, bodyName)
	}
	groundMove { |view, x, y, modifiers = 0| ^this.prMove(view, x, y, modifiers) }
	groundUp { |view, x, y, modifiers = 0, button = 0|
		^this.prUp(view, x, y, modifiers, button)
	}
	groundWheel { |view, x, y, modifiers = 0, delta = 0|
		^this.prGroundWheel(view, x, y, modifiers, delta)
	}

	prInit {
		controlViews = IdentitySet.new;
		controls.do { |control| controlViews.add(control.view) };
		this.prBindEndpoints;
		this.prBindBodies;
		gestureCtl = SimpleController(grammar);
		gestureCtl.put(\gesture, { { this.prRefresh }.defer });
		matrixCtl = SimpleController(grammar.matrix);
		#[patch, clear, matrix].do { |key|
			matrixCtl.put(key, { { this.prRefresh }.defer })
		};
		^this
	}

	prBindEndpoints {
		endpoints.keysValuesDo { |name, endpoint|
			var view = endpoint[\view];
			view.mouseEnterAction = {
				grammar.hoverCable = nil;
				grammar.hoverEndpoint = name;
				view.refresh;
			};
			view.mouseLeaveAction = {
				// Keep a spread stack open briefly while the pointer travels from
				// the socket center onto one of its offset cable/plug hit paths.
				AppClock.sched(0.18, {
					if(grammar.hoverEndpoint == name) { grammar.hoverEndpoint = nil };
					view.refresh;
					nil
				});
			};
			view.mouseDownAction = { |v, x, y, modifiers, button, clicks|
				this.prEndpointDown(name, endpoint, v, x, y,
					modifiers ? 0, button ? 0, clicks ? 1)
			};
			view.mouseMoveAction = { |v, x, y, modifiers|
				this.prMove(v, x, y, modifiers ? 0)
			};
			view.mouseUpAction = { |v, x, y, modifiers, button|
				this.prUp(v, x, y, modifiers ? 0, button ? 0)
			};
			view.mouseWheelAction = { |v, x, y, modifiers, xDelta, yDelta|
				this.prEndpointWheel(name, endpoint, v, x, y,
					modifiers ? 0, yDelta ? 0)
			};
		};
		^this
	}

	// A body may register members without a view: the night bodies own their own
	// first gesture pass (§6.1) and hand the body name in through groundDown, so
	// binding them here would overwrite the touch grammar they installed.
	prBindBodies {
		bodies.keysValuesDo { |name, body|
			body[\view] !? { |view| this.prBindGroundView(view, name) }
		};
		^this
	}

	prBindGround { |view, bodyName|
		var localBody;
		localBody = this.prBodyName(view) ? bodyName;
		if((view.isKindOf(UserView) or: { view.isKindOf(StaticText) })
			and: { this.prEndpoint(view).isNil }
			and: { controlViews.includes(view).not }
			and: { view.mouseDownAction.isNil }) {
			this.prBindGroundView(view, localBody)
		};
		this.prBindHoverView(view);
		view.children.do { |child| this.prBindGround(child, localBody) };
		^this
	}

	prBindGroundView { |view, bodyName|
		view.mouseDownAction = { |v, x, y, modifiers, button, clicks|
			this.prGroundDown(v, x, y, modifiers ? 0, button ? 0,
				clicks ? 1, bodyName)
		};
		view.mouseMoveAction = { |v, x, y, modifiers|
			this.prMove(v, x, y, modifiers ? 0)
		};
		view.mouseUpAction = { |v, x, y, modifiers, button|
			this.prUp(v, x, y, modifiers ? 0, button ? 0)
		};
		view.mouseWheelAction = { |v, x, y, modifiers, xDelta, yDelta|
			this.prGroundWheel(v, x, y, modifiers ? 0, yDelta ? 0)
		};
		^this
	}

	// Qt does not propagate no-button mouse moves through child widgets. Every
	// Day descendant therefore opts into tracking while retaining its own hover
	// behavior; otherwise a cable crossing a knob or label can never be focused.
	prBindHoverView { |view|
		var previousOver = view.mouseOverAction, previousEnter = view.mouseEnterAction;
		if(controlViews.includes(view)) {
			view.mouseEnterAction = { |v|
				previousEnter.value(v);
				grammar.hoverCable = nil
			}
		};
		view.mouseOverAction = { |v, x, y|
			previousOver.value(v, x, y);
			if(controlViews.includes(v)) {
				grammar.hoverCable = nil
			} {
				this.prHover(v, x, y)
			}
		};
		^this
	}

	prHover { |view, x, y|
		if(grammar.isInHand.not and: { grammar.isHarvesting.not }) {
			grammar.hoverCable = overlay.patchAt(this.prPoint(view, x, y))
		};
		^this
	}

	prEndpointDown { |name, endpoint, view, x, y, modifiers, button, clicks|
		var point, patch;
		point = this.prPoint(view, x, y);
		grammar.cursor = point;
		if(clicks > 1) {
			grammar.toggleSoloEndpoint(endpoint[\kind], name);
			^this
		};
		if(endpoint[\kind] == \jack) {
			grammar.startSource(name, if(button == 1) { -1 } { 1 });
			^this
		};
		if(grammar.isStickySource and: { button == 0 }) {
			grammar.updateCandidate(name, 0);
			^this
		};
		patch = overlay.patchAtSocket(name, point);
		if(patch.isNil) { ^this };
		if(button == 1) {
			grammar.instantRip(patch[\src], patch[\dest])
		} {
			if(modifiers.isAlt) {
				grammar.polarityFlip(patch[\src], patch[\dest])
			} {
				grammar.grab(patch[\src], patch[\dest])
			}
		};
		^this
	}

	prEndpointWheel { |name, endpoint, view, x, y, modifiers, delta|
		var patch, point;
		if(endpoint[\kind] != \socket or: { delta == 0 }) { ^this };
		point = this.prPoint(view, x, y);
		patch = overlay.patchAtSocket(name, point);
		patch !? {
			grammar.wheel(patch[\src], patch[\dest], delta.sign, modifiers.isCtrl)
		};
		^this
	}

	prGroundDown { |view, x, y, modifiers, button, clicks, bodyName|
		var point, patch, body;
		point = this.prPoint(view, x, y);
		grammar.cursor = point;
		patch = overlay.patchAt(point);
		if(clicks > 1) {
			if(patch.notNil) {
				grammar.toggleSoloCable(patch[\src], patch[\dest])
			} {
				body = bodies[bodyName];
				body !? { grammar.toggleSoloBody(bodyName, body[\members]) };
			};
			^this
		};
		if(button == 1) {
			if(patch.notNil) {
				grammar.instantRip(patch[\src], patch[\dest])
			} {
				grammar.beginHarvest(point)
			};
			^this
		};
		patch !? {
			if(modifiers.isAlt) {
				grammar.polarityFlip(patch[\src], patch[\dest])
			} {
				grammar.grab(patch[\src], patch[\dest])
			}
		};
		^this
	}

	prGroundWheel { |view, x, y, modifiers, delta|
		var patch;
		if(delta == 0) { ^this };
		patch = overlay.patchAt(this.prPoint(view, x, y));
		patch !? {
			grammar.wheel(patch[\src], patch[\dest], delta.sign, modifiers.isCtrl)
		};
		^this
	}

	prMove { |view, x, y, modifiers|
		var point, nearest;
		point = this.prPoint(view, x, y);
		grammar.cursor = point;
		if(grammar.isHarvesting) {
			grammar.updateHarvest(point, overlay.cableRecords);
			^this
		};
		if(grammar.isInHand) {
			nearest = overlay.nearestDestination(point);
			if(nearest.isNil) {
				grammar.leaveRadius
			} {
				grammar.updateCandidate(nearest[\dest], nearest[\distance])
			};
		};
		^this
	}

	prUp { |view, x, y, modifiers, button|
		var point, nearest;
		point = this.prPoint(view, x, y);
		grammar.cursor = point;
		if(grammar.isHarvesting) {
			grammar.updateHarvest(point, overlay.cableRecords);
			grammar.finishHarvest(overlay.cableRecords);
			^this
		};
		if(grammar.isInHand) {
			nearest = overlay.nearestDestination(point);
			if(nearest.notNil) {
				grammar.updateCandidate(nearest[\dest], nearest[\distance])
			} {
				grammar.leaveRadius
			};
			if(grammar.candidateDest.notNil) {
				grammar.releaseCandidate(modifiers.isShift)
			} {
				grammar.releaseVoid
			};
		};
		^this
	}

	prRefresh {
		if(refreshGate.notNil and: { refreshGate.value.not }) { ^this };
		endpoints.values.do { |endpoint| endpoint[\view].refresh };
		overlay.view.refresh;
		^this
	}

	prPoint { |view, x, y|
		^view.mapToGlobal(Point(x, y)) - overlay.view.mapToGlobal(0@0)
	}

	prEndpoint { |view|
		^endpoints.values.detect { |endpoint| endpoint[\view] === view }
	}

	prBodyName { |view|
		if(view.isNil) { ^nil };
		^bodies.keys.detect { |name| bodies[name][\view] === view }
	}
}
