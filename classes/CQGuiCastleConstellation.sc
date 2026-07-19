// CQGuiCastleConstellation — semantic adapter for the Quantussy topology.
// It derives partner and sample/track mode from the normative spesal state;
// all pixels remain centralized in CQInk.

CQGuiCastleConstellation {
	var gui, <view;

	*new { |gui| ^super.new.prInit(gui) }

	*normalizedPositions {
		^[Point(0.13, 0.28), Point(0.48, 0.06), Point(0.86, 0.30),
			Point(0.72, 0.80), Point(0.22, 0.83)]
	}

	*partnerIndex { |castleIndex, state|
		^(0..4).removing(castleIndex.asInteger)[state.asInteger.mod(4)]
	}

	*trackMode { |state| ^state.asInteger >= 4 }

	prInit { |owner|
		gui = owner;
		view = UserView().minSize_(Size(220, 150))
			.background_(CQInk.palette(\day)[\ground]);
		view.drawFunc = {
			var states = gui.spesalStates;
			CQInk.drawCastleConstellation(view.bounds.moveTo(0, 0),
				this.class.normalizedPositions,
				(0..4).collect { |i| this.class.partnerIndex(i, states[i]) },
				states.collect { |state| this.class.trackMode(state) },
				states, gui.castleValues, gui.currentSkin, 5205);
		};
		^this
	}
}
