// CQGuiNightMapTest — the serverless, windowless contracts of the Night map
// class: the endpoint look table mirrors the canonical matrix inventory with
// the same families, glyphs and lamps the Day skin teaches (GUI_SPEC §16.7).

CQGuiNightMapTest : UnitTest {

	test_endpointSpecInventory {
		CQMatrix.sourceNames.do { |name|
			var spec = CQGuiNightMap.endpointSpec(name);
			this.assert(spec.notNil and: { spec[\kind] == \jack },
				"% is a source jack".format(name));
		};
		CQMatrix.destNames.do { |name|
			var spec = CQGuiNightMap.endpointSpec(name);
			this.assert(spec.notNil and: { spec[\kind] == \socket },
				"% is a destination socket".format(name));
		};
		this.assert(CQGuiNightMap.endpointSpec(\bogus).isNil,
			"an unknown name has no night home");
	}

	test_lampsExactlyOnGateTrigs {
		var lamped = CQMatrix.destNames.select { |name|
			CQGuiNightMap.endpointSpec(name)[\lamp] == true
		};
		this.assertEquals(lamped,
			[\coco1_flip, \coco1_skip, \coco2_flip, \coco2_skip,
				\castle1_spesal, \castle2_spesal, \castle3_spesal,
				\castle4_spesal, \castle5_spesal],
			"charge lamps sit on the nine gate/trig sockets in /cq_charge order");
	}

	test_familiesMatchDaySkin {
		this.assertEquals(CQGuiNightMap.endpointSpec(\tri3)[\family], \tri,
			"triangle buds keep the tri family");
		this.assertEquals(CQGuiNightMap.endpointSpec(\coco1)[\family], \cas,
			"the spiral's mouth keeps the voice gray");
		this.assertEquals(CQGuiNightMap.endpointSpec(\org2)[\family], \organ,
			"org fruit keeps the organ yellow");
		this.assertEquals(CQGuiNightMap.endpointSpec(\envB)[\family], \input,
			"root tips keep the input green");
		this.assertEquals(CQGuiNightMap.endpointSpec(\coco2_spaf)[\family], \spaf,
			"spaf moons keep the purple ring");
		this.assertEquals(CQGuiNightMap.endpointSpec(\castle4_fm)[\family], \fm,
			"fm moons keep the blue ring");
		this.assertEquals(CQGuiNightMap.endpointSpec(\castle4_fm)[\glyph], \wave,
			"fm moons carry the wave glyph");
		this.assertEquals(CQGuiNightMap.endpointSpec(\deckA_rate)[\glyph], \pendulum,
			"the wing socket carries the pendulum glyph");
		this.assertEquals(CQGuiNightMap.endpointSpec(\coco1_inVca)[\glyph], \wedge,
			"vca moons carry the open wedge");
	}

	test_sourceDecorations {
		this.assertEquals(CQGuiNightMap.endpointSpec(\tri1)[\decoration], \leaf,
			"tri jacks grow the leaf");
		this.assertEquals(CQGuiNightMap.endpointSpec(\cas5)[\decoration], \crown,
			"cas jacks wear the crown teeth");
		this.assert(CQGuiNightMap.endpointSpec(\coco1)[\decoration].isNil,
			"coco jacks stay bare — the chevron is the shell's flip touch");
		this.assertEquals(CQGuiNightMap.endpointSpec(\org1)[\decoration], \stem,
			"org jacks keep their stem tip");
		this.assertEquals(CQGuiNightMap.endpointSpec(\envA)[\decoration], \root,
			"a root tip redraws its own arrival under its opaque view");
	}

	test_crescentInventory {
		this.assertEquals(CQGuiNightMap.crescentNames(0),
			[\coco1Spaf, \coco1In, \coco1Fb],
			"coco 1 hangs spaf/in/fb tide crescents under its moons");
		this.assertEquals(CQGuiNightMap.crescentNames(1),
			[\coco2Spaf, \coco2In, \coco2Fb],
			"coco 2 mirrors the same three crescents");
		this.assertEquals(CQGuiNightMap.crescentNames(nil),
			[\deckALevel, \deckBLevel],
			"each bird carries its deck-level crescent");
		(CQGuiNightMap.crescentNames(0) ++ CQGuiNightMap.crescentNames(1)
			++ CQGuiNightMap.crescentNames(nil)).do { |name|
			this.assert(CQSpec.at(name).notNil,
				"crescent % maps a real CQSpec parameter".format(name));
		};
	}

	// GUI_SPEC §6: solo-light on a body lights everything touching it. The
	// night's bodies are its own anatomy, but they keep the Day panel's names
	// where the organ is the same — one grammar must not hold two solos for it.
	test_soloBodiesAreTheNightAnatomy {
		this.assertEquals(CQGuiNightMap.bodyNames,
			#[castle1, castle2, castle3, castle4, castle5, coco1, coco2, quantussy],
			"five castles, two shells and the ring carry the night's body solo");
		this.assertEquals(CQGuiNightMap.bodyEndpointNames(\castle3),
			#[tri3, cas3, castle3_fm, castle3_spesal],
			"a castle body owns its two buds and its two moons");
		this.assertEquals(CQGuiNightMap.bodyEndpointNames(\coco2),
			CQGuiPanels.bodyEndpointNames(\coco2),
			"a shell is the Day coco panel's own body, so one solo serves both");
		this.assertEquals(CQGuiNightMap.bodyEndpointNames(\quantussy),
			CQGuiPanels.bodyEndpointNames(\quantussy),
			"the ring is the Day quantussy panel's own body");
		this.assert(CQGuiNightMap.bodyEndpointNames(\bogus).isEmpty,
			"an unknown body claims no endpoints");
	}

	test_everyBodyMemberIsACanonicalEndpoint {
		CQGuiNightMap.bodyNames.do { |name|
			var members = CQGuiNightMap.bodyEndpointNames(name);
			this.assert(members.notEmpty, "% is a real body".format(name));
			members.do { |member|
				this.assert(CQMatrix.srcIndex(member).notNil
					or: { CQMatrix.destIndex(member).notNil },
					"body member % is a canonical jack or socket".format(member));
			};
		};
		// roots and birds hold one endpoint each: an own body would only be a
		// second name for the jack/socket solo that already lights those cables
		this.assert(CQGuiNightMap.bodyNames.every { |name|
			CQGuiNightMap.bodyEndpointNames(name).size > 1
		}, "no body duplicates a single endpoint's solo");
	}
}
