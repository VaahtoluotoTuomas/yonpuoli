// CQGuiCableOverlay — hit-transparent planted-banana renderer for the Day panel.
// Endpoint geometry comes from the actual laid-out jack/socket views, while all
// matrix reads and redraw notifications stay on CQMatrix's public contracts.

CQGuiCableOverlay {
	var matrix, endpoints, ctl;
	var <view, <skin = \day;

	*new { |matrix, endpoints| ^super.newCopyArgs(matrix, endpoints).prBuild }

	skin_ { |newSkin|
		skin = newSkin;
		view.background = CQInk.palette(skin)[\clear];
		view.refresh;
		^this
	}

	free {
		ctl !? { ctl.remove };
		ctl = nil;
	}

	cableCount { ^matrix.asArray.size }
	endpointCount { ^endpoints.size }

	prBuild {
		view = UserView().background_(CQInk.palette(skin)[\clear]);
		view.acceptsMouse = false;
		view.drawFunc = {
			try { this.prDraw } { |error| CQInkControl.noteDrawError(error) }
		};
		ctl = SimpleController(matrix);
		ctl.put(\patch, { { view.refresh }.defer });
		ctl.put(\clear, { { view.refresh }.defer });
		ctl.put(\matrix, { { view.refresh }.defer });
		^this
	}

	prPoint { |name|
		var endpoint = endpoints[name], localCenter, globalCenter, overlayOrigin;
		if(endpoint.isNil) { ^nil };
		localCenter = endpoint[\anchor] ?? {
			Point(endpoint[\view].bounds.width / 2,
				endpoint[\view].bounds.height / 2)
		};
		globalCenter = endpoint[\view].mapToGlobal(localCenter);
		overlayOrigin = view.mapToGlobal(0@0);
		^globalCenter - overlayOrigin
	}

	prDraw {
		matrix.asArray.do { |patch|
			var start = this.prPoint(patch[0]), finish = this.prPoint(patch[1]);
			var weight = patch[2];
			if(start.notNil and: { finish.notNil }) {
				CQInk.drawCable(start, finish, weight, skin, 0.88);
				CQInk.drawPlug(start, weight, skin);
				CQInk.drawPlug(finish, weight, skin);
			};
		};
	}
}
