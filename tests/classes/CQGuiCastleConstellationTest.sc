// Functional contract for the Quantussy topology view. The drawing is allowed
// to show only relationships already present in the eight spesal states:
// state % 4 selects one of the four other castles, state div: 4 selects mode.

CQGuiCastleConstellationTest : UnitTest {
	var constellationClass;

	setUp { constellationClass = \CQGuiCastleConstellation.asClass }

	prRequireConstellation {
		if(constellationClass.isNil) {
			this.assert(false,
				"Quantussy has a dedicated functional castle constellation");
			^false
		};
		^true
	}

	test_eachStateSelectsEveryOtherCastleExactlyOnce {
		if(this.prRequireConstellation.not) { ^this };
		5.do { |castle|
			var partners = (0..3).collect { |state|
				constellationClass.partnerIndex(castle, state)
			};
			this.assert(partners.includes(castle).not,
				"castle % never selects itself".format(castle + 1));
			this.assertEquals(partners.asSet.size, 4,
				"castle % reaches all four partners".format(castle + 1));
		};
	}

	test_upperFourStatesKeepPartnerAndChangeMode {
		if(this.prRequireConstellation.not) { ^this };
		5.do { |castle|
			4.do { |state|
				this.assertEquals(
					constellationClass.partnerIndex(castle, state),
					constellationClass.partnerIndex(castle, state + 4),
					"mode bit does not change the selected partner");
				this.assert(constellationClass.trackMode(state).not,
					"states 0..3 are sample mode");
				this.assert(constellationClass.trackMode(state + 4),
					"states 4..7 are track mode");
			};
		};
	}

	test_constellationOwnsFiveStableNodePositions {
		var first, second;
		if(this.prRequireConstellation.not) { ^this };
		first = constellationClass.normalizedPositions;
		second = constellationClass.normalizedPositions;
		this.assertEquals(first.size, 5, "one topology node per castle");
		this.assertEquals(first, second, "topology does not jitter between redraws");
		this.assert(first.every { |point|
			point.x.inclusivelyBetween(0, 1)
				and: { point.y.inclusivelyBetween(0, 1) }
		}, "all normalized nodes remain inside the drawing field");
	}
}
