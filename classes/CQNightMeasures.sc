// CQNightMeasures — the night's measures, in one table (NIGHT_STANDALONE_PLAN §8).
// Numbers only: no math, no Qt, no state. CQNightGeometry composes them into the
// archipelago, CQGuiNightMap sizes its views from them and CQInkNight draws its
// bodies at them, so the spring flood is one readable table instead of thirty
// literals scattered through three files.
//
// Two kinds of number live here and they behave differently (GUI_SPEC §16.10):
//
//   BODY measures are multiplied by the stage's `scale` — the landscape grows
//   with the paper.
//   PX measures are not — hit tolerances, opaque view boxes and drawn ink stay
//   the size of a hand and a pen however big the window gets. "Ink does not
//   thicken when the paper grows."
//
// Each method says which it is. Changing a number here is the whole flood;
// CQNightGeometryTest's collision matrix is what says whether it fits.

CQNightMeasures {

	// ---- the stage ----

	// the minimum stage (1280x720 window under the toolbar and Master strip)
	// is scale 1; below half scale the night stops shrinking and starts clipping
	*refWidth { ^1280 }                     // px
	*refHeight { ^570 }                     // px
	*minScale { ^0.5 }
	*ringHeightFraction { ^0.52 }           // the river's altitude on the stage

	// ---- the river and its castles ----

	*ringRadius { ^191 }                    // body
	*ringBoxPad { ^14 }                     // px, the ring layer's own slack

	*castleRx { ^37 }                       // body — the drawn blob's ellipse
	*castleRy { ^33 }                       // body
	*castleBoxReach { ^68 }                 // body — transparent castle paper
	*castleBoxSlack { ^5 }                  // px, hairline room inside that box
	*clockRadius { ^18 }                    // body — the inner spesal dial
	// The ripple follows each seeded blob's actual bottom, not its mean ellipse,
	// so every castle gives the control the same visual breath.
	*rangeBodyGap { ^17 }                   // px — blob edge to playable ripple

	// the four castle endpoints on their clock face (body radii from the centre)
	*castleJackOrbit { ^52 }                // tri at 1:30, cas at 10:30
	*castleFmOrbit { ^45 }                  // the fm socket at 4 o'clock
	// the lamped spesal view is the widest box a castle carries; this orbit is
	// what keeps it off the inner clock at every scale (CQNightGeometryTest)
	*castleSpesalOrbit { ^61 }

	*bodyMargin { ^12 }                     // px — the blob's hit/hover margin
	*clockHit { ^12 }                       // px — the dial annulus' tolerance

	// ---- the shells ----

	// The gap is the flood's tightest number at the minimum stage: it is squeezed
	// from the inside by the castle boxes the shell may not cover and from the
	// outside by the almanac's water. CQNightGeometryTest holds both ends.
	*shellGap { ^198 }                      // body — ring rim to shell centre
	*shellRise { ^20 }                      // body — the shells sit above the eye
	*shellOuterR { ^114 }                   // body — the spiral's outer rim
	// the shell layer's slack past the rim. It only has to hold what the shell
	// itself draws — the seal ring, the notch's hover reach, the struck head and
	// the skip glyph, ~16 px past outerR — so trimming it is what buys the
	// castle boxes and the almanac their margins at the minimum stage.
	*shellBoxPad { ^28 }                    // body
	*spiralInnerR { ^14 }                   // body — where the winding starts
	*orgStemReach { ^14 }                   // body — spiral to the hanging fruit
	*moonArc { ^136 }                       // body — the five moons' arc
	*crescentArc { ^176 }                   // body — the tide crescents' arc

	// The mouth's two stationary transport marks hang below the jack, and the
	// flood pushed them further out than they have ever hung: the grown jack
	// view (34) and the grown mark boxes (16) set the floor, since a mark whose
	// box lies under the jack's opaque view is half unreachable. They are held
	// AT that floor on purpose — the water below the mouth is the lane cables
	// from the ring to this shell's moons sag through (§8 review note).
	*chevronDrop { ^38 }                    // body
	*chevronDropFloor { ^38 }               // px
	*skipDrop { ^34 }                       // body, below the chevron
	*skipDropFloor { ^34 }                  // px

	*heartHit { ^22 }                       // px — hold's toggle and the punch
	*markHitReach { ^16 }                   // px — chevron and skip boxes
	*rimMargin { ^12 }                      // px — the shell rim's body handle
	// how near the drawn groove a press takes the winding gesture. It must stay
	// under half the spiral's radial pitch or two turns would answer one press.
	*windGrab { ^13 }                       // px

	// ---- the eye ----
	// The two-knob macro of the whole instrument: qFreq and qChaos live nowhere
	// else at night, so §8 gave the iris the mass its job deserves. Its position
	// did not move a pixel.

	*eyeLayerR { ^62 }                      // px — the storm iris' own layer
	*eyeMaskR { ^54 }                       // px — round ground under the actual iris
	*eyeHit { ^54 }                         // px — the pupil plus four weather rings

	// ---- the birds ----

	*birdFractions { ^#[0.21, 0.79] }
	*birdAltitude { ^56 }                   // body
	*birdSpan { ^64 }                       // body — wing tip to wing tip
	*birdBoxRx { ^44 }                      // body
	*birdBoxRy { ^31 }                      // body
	*birdRateOffset { ^Point(22, 30) }      // body — the rate moon under the wing
	*birdCrescentOffset { ^Point(-37, 27) } // body — the deck level crescent
	*birdLabelDrop { ^43 }                  // body
	// fixed in px so a resize never re-cuts a name that already fits, and tall
	// enough to hold a 10 px mono face whole (§16.4 fixes the type, not the box)
	*birdLabelExtent { ^Point(200, 20) }    // px
	*birdDropPad { ^6 }                     // px — §16.7's drop target slack

	// ---- the roots ----

	*rootFractions { ^#[0.12, 0.88] }
	*rootKnotReach { ^74 }                  // body — intake knot above the waterline
	// A root is a delta, not a comb: the seeded geometry perturbs this wide
	// nominal spacing independently on both sides.
	*rootSourceSpacing { ^34 }              // body — five unlabelled intake strands
	*rootSourceShoulder { ^48 }             // body — first bend above the waterline
	*rootSourceGather { ^26 }               // body — final bend before the knot
	*rootSourceHit { ^13 }                  // px — nearest strand chooses provenance
	*rootVeinOutset { ^52 }                 // body — route around the Coco crescents
	*rootVeinLift { ^38 }                   // body — first audio-vein control point
	*rootVeinDrop { ^64 }                   // body — final control point below the shell
	*rootVeinJoinNorm { ^0.8 }              // body fraction — a real outer spiral point
	*rootBudX { ^44 }                       // body — env follower side-growth
	*rootBudY { ^-4 }                       // body
	*rootKnotR { ^18 }                      // body — drawn two-lobed preamp knot
	*rootKnotHit { ^27 }                    // px — axis-locked drive / bias gesture
	*rootBoxPad { ^14 }                     // px — transparent local repaint layer
	*rootLabelExtent { ^Point(72, 14) }      // px — transient provenance only
	// "Beside, not over, the delta" is a promise a fixed inset cannot keep: the
	// strands are seeded, so this is only the nominal rest, and the geometry
	// pushes further inward until the ground erasure clears the actual curves.
	*rootLabelInward { ^60 }                // body — nominal, minimum inward rest
	*rootLabelDrop { ^5 }                   // body — slight settling below knot
	*rootLabelClear { ^6 }                  // px — erasure never touches a strand
	*rootLabelWashPad { ^Point(3, 1) }      // px — CQInkNight's ground erasure
	// The mic sprout still grows beside envA, but on root A's far side: the
	// tide pool took the water it used to stand in (§9), and a reed rising from
	// the waterline at the bay's left edge is where it belongs anyway.
	*sproutOffset { ^-50 }                  // body — the mic sprout beside envA
	*sproutTipOffset { ^-58 }               // body
	*sproutReach { ^54 }                    // body

	// ---- endpoint view extents (px, rake 12) ----
	// Most of these remain opaque boxes that eat lines and marks beneath them.
	// The four castle details are deliberately clear so the irregular castle
	// flesh, not a rectangular pad, decides what masks the river. Growing an
	// opaque owner re-runs the courtesy law; clear castle details must not add
	// synthetic repair stubs.

	*jackExtent { ^34 }                     // px, square
	*socketExtent { ^Point(28, 28) }        // px
	*lampedWidth { ^46 }                    // px — a socket that carries a lamp
	*lampOffset { ^18 }                     // px — the lamp bead's own home
	*endpointInkScale { ^1.2 }              // visible jack/socket/lamp ink
	*endpointGlyphExtent { ^12 }            // px — wave/wedge/trigger symbol
	*crescentExtent { ^44 }                 // px, square
	// Only the two in/fb crescents grow downward to carry their Dolby mark.
	// The crescent itself keeps the common 44 px square and exact anchor; the
	// extra paper is below it, away from the moons.
	*dolbyCrescentHeight { ^72 }            // px — grown in/fb owner view
	// CQInk.drawJack's Night-scaled idle dot plus its outline. The shared
	// primitive keeps Day at scale 1 while Night supplies endpointInkScale;
	// any local underlay has to stop at this visible rim.
	*jackDotRim { ^5.8 }                    // px

	// ---- the solitary moon and the bay-wide tide (§16.6) ----
	// The moon is a remote body beyond bird B, not the heading of an almanac
	// panel. Its vertical gesture briefly wears rain or sun; those glyphs have
	// geometry only so their active ink cannot clip, never as resting targets.
	*moonFraction { ^0.925 }                // of width — beyond bird B
	*moonAltitude { ^50 }                   // body — slightly above the birds
	*moonRadius { ^35 }                     // body (Ø 70)
	*moonBoxSlack { ^4 }                    // px, hairline room past all ink
	*moonHalo { ^10 }                       // px — the 120 ms shake flash
	// [name, dx, dy] from the moon; deliberately asymmetric, never a row.
	*moonWeatherPlaces { ^[[\grow, -25, 39], [\wither, 29, 35]] }

	// There is no pool body. Four memory seeds in open winds imply one large
	// rectangular bilinear field passing behind the organism. At rest the
	// rectangle itself is never drawn; these fractions place only its seeds.
	*tideLeftFraction { ^0.31 }
	*tideRightFraction { ^0.69 }
	*tideTopFraction { ^0.18 }
	*tideBottomFraction { ^0.96 }
	*stampRadius { ^14 }                    // px touch body around a tiny seed
	// The bay-wide focus is opened by a small gill below the two lower
	// castles. It is an organ of the same landscape, not a toolbar toggle:
	// closed = ordinary cable grammar, open = cables submerged + tide playable.
	*tideGateFraction { ^0.94 }             // of stage height
	*tideGateHit { ^24 }                    // px, one compact touch body
	*tideGateBoxSlack { ^2 }                // px past the hover echo
	*seedLength { ^11 }                     // px, inward-pointing kernel
	*seedWidth { ^7 }                       // px
	*seedHalo { ^15 }                       // px, focus / breed target bloom
	*tideGateSpan { ^20 }                   // px, half-width of the gill strokes
	*tideGateRise { ^11 }                   // px, opening breath

	// ---- drawn ink bodies (px — CQInkNight draws at these) ----
	// Not stroke widths and not type: those are frozen (§16.3, §16.4). These are
	// the sizes of the small drawn bodies themselves, and §8's law for them is
	// that information must not shrink relative to the landscape around it.

	*heartDotR { ^5.5 }                     // the hold heart at the spiral's eye
	*heartSealR { ^11 }                     // its danger ring while sealed
	*notchReach { ^8 }                      // the reel notch across the groove
	*notchHoverReach { ^12 }                // the grab cue
	*reelMaxNorm { ^0.99 }                  // keep full speed visible before mouth jack
	*crescentArcR { ^18 }                   // the tide crescent's own arc
	*dolbyOysterDrop { ^36 }                // px — centre-to-centre from crescent
	*dolbyOysterWidth { ^17 }               // px — half-width of the paired arcs
	*dolbyOysterOpenGap { ^6 }              // px — off posture
	*dolbyOysterGateGap { ^1.6 }            // px — gate's hairline opening
	*dolbyOysterHitX { ^17 }                // px — specific tap before crescent
	*dolbyOysterHitY { ^10 }                // px
	*clockBeadR { ^4.2 }                    // the spesal state's lit bead
	*clockBeadHaloR { ^6 }
	*clockTickReach { ^4.5 }                // the eight tick marks' half-length
	*rangeRippleWidth { ^22 }               // px — drawn slow/mid/fast mark
	*rangeRippleHeight { ^11 }              // px
	*rangeHitX { ^15 }                      // px — cue and act around the ripple
	*rangeHitY { ^12 }                      // px
	*cometR { ^2.75 }                       // the read head
	*fireflyR { ^1.6 }                      // the river's direction carriers
	*chordLightR { ^2.2 }                   // the coupling's travelling light
	*pupilMin { ^5.5 }                      // the iris at qFreq 0
	*pupilRange { ^10.5 }                   // ...and how far it opens
	*irisGap { ^6.5 }                       // pupil rim to the rays' inner ends
	*irisRayBase { ^5.5 }                   // a calm corona's ray
	*irisRayChaos { ^7 }                    // ...and how far the storm reaches
	*irisRayJitter { ^8 }
	*irisWeatherInner { ^20 }               // calm front, outside the largest pupil
	*irisWeatherSpan { ^32 }                // four 8 px seasons: ¼, ½, ¾, full
	*birdBodyR { ^3.6 }
	*skipGlyphR { ^8 }                      // the mouth's stationary skip mark
	*chevronReach { ^9 }                    // the flip chevron's half-width
	*chevronRise { ^6 }                     // its head's depth
	*chevronShuttleRise { ^7.5 }            // ...opposed, while shuttling
	*struckBase { ^7 }                      // the hammered head's arms
	*struckRange { ^10 }
}
