// Semantic contract retained for Night's under-the-hand spesal chords after
// N9 removed Day's permanent constellation overview.

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
}
