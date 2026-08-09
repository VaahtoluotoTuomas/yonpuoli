// CQGuiCastleConstellation — the shared semantic adapter retained after N9
// removed Day's overview drawing. Night asks it what one spesal state means;
// it owns no pixels, view or second presentation of the topology.

CQGuiCastleConstellation {
	*partnerIndex { |castleIndex, state|
		^(0..4).removing(castleIndex.asInteger)[state.asInteger.mod(4)]
	}

	*trackMode { |state| ^state.asInteger >= 4 }
}
