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

	*ringRadius { ^158 }                    // body
	*ringBoxPad { ^12 }                     // px, the ring layer's own slack

	*castleRx { ^29 }                       // body — the drawn blob's ellipse
	*castleRy { ^25 }                       // body
	*castleBoxReach { ^45 }                 // body — transparent castle paper
	*castleBoxSlack { ^4 }                  // px, hairline room inside that box
	*clockRadius { ^14 }                    // body — the inner spesal dial
	// The ripple follows each seeded blob's actual bottom, not its mean ellipse,
	// so every castle gives the control the same visual breath.
	*rangeBodyGap { ^13 }                   // px — blob edge to playable ripple

	// the four castle endpoints on their clock face (body radii from the centre)
	*castleJackOrbit { ^40 }                // tri at 1:30, cas at 10:30
	*castleFmOrbit { ^34 }                  // the fm socket at 4 o'clock
	// the lamped spesal view is the widest box a castle carries; this orbit is
	// what keeps it off the inner clock at every scale (CQNightGeometryTest)
	*castleSpesalOrbit { ^48 }

	*bodyMargin { ^10 }                     // px — the blob's hit/hover margin
	*clockHit { ^10 }                       // px — the dial annulus' tolerance

	// ---- the shells ----

	// The gap is the flood's tightest number at the minimum stage: it is squeezed
	// from the inside by the castle boxes the shell may not cover and from the
	// outside by the almanac's water. CQNightGeometryTest holds both ends.
	*shellGap { ^184 }                      // body — ring rim to shell centre
	*shellRise { ^26 }                      // body — the shells sit above the eye
	*shellOuterR { ^100 }                   // body — the spiral's outer rim
	// the shell layer's slack past the rim. It only has to hold what the shell
	// itself draws — the seal ring, the notch's hover reach, the struck head and
	// the skip glyph, ~13 px past outerR — so trimming it is what buys the
	// castle boxes and the almanac their margins at the minimum stage.
	*shellBoxPad { ^24 }                    // body
	*spiralInnerR { ^12 }                   // body — where the winding starts
	*orgStemReach { ^12 }                   // body — spiral to the hanging fruit
	*moonArc { ^122 }                       // body — the five moons' arc
	*crescentArc { ^152 }                   // body — the tide crescents' arc

	// The mouth's two stationary transport marks hang below the jack, and the
	// flood pushed them further out than they have ever hung: the grown jack
	// view (30) and the grown mark boxes (13) set the floor, since a mark whose
	// box lies under the jack's opaque view is half unreachable. They are held
	// AT that floor on purpose — the water below the mouth is the lane cables
	// from the ring to this shell's moons sag through (§8 review note).
	*chevronDrop { ^30 }                    // body
	*chevronDropFloor { ^30 }               // px
	*skipDrop { ^26 }                       // body, below the chevron
	*skipDropFloor { ^26 }                  // px

	*heartHit { ^18 }                       // px — hold's toggle and the punch
	*markHitReach { ^13 }                   // px — chevron and skip boxes
	*rimMargin { ^10 }                      // px — the shell rim's body handle
	// how near the drawn groove a press takes the winding gesture. It must stay
	// under half the spiral's radial pitch or two turns would answer one press.
	*windGrab { ^11 }                       // px

	// ---- the eye ----
	// The two-knob macro of the whole instrument: qFreq and qChaos live nowhere
	// else at night, so §8 gave the iris the mass its job deserves. Its position
	// did not move a pixel.

	*eyeLayerR { ^48 }                      // px — the storm iris' own layer
	*eyeHit { ^24 }                         // px — the qFreq/qChaos pupil

	// ---- the birds ----

	*birdAltitude { ^48 }                   // body
	*birdSpan { ^46 }                       // body — wing tip to wing tip
	*birdBoxRx { ^32 }                      // body
	*birdBoxRy { ^22 }                      // body
	*birdRateOffset { ^Point(17, 28) }      // body — the rate moon under the wing
	*birdCrescentOffset { ^Point(-27, 24) } // body — the deck level crescent
	*birdLabelDrop { ^46 }                  // body
	// fixed in px so a resize never re-cuts a name that already fits, and tall
	// enough to hold a 10 px mono face whole (§16.4 fixes the type, not the box)
	*birdLabelExtent { ^Point(200, 20) }    // px
	*birdDropPad { ^6 }                     // px — §16.7's drop target slack

	// ---- the roots ----

	*rootFractions { ^#[0.17, 0.83] }
	*rootKnotReach { ^110 }                 // body — intake knot above the waterline
	// A root is a delta, not a comb: the seeded geometry perturbs this wide
	// nominal spacing independently on both sides.
	*rootSourceSpacing { ^27 }              // body — five unlabelled intake strands
	*rootSourceShoulder { ^54 }             // body — first bend above the waterline
	*rootSourceGather { ^32 }               // body — final bend before the knot
	*rootSourceHit { ^11 }                  // px — nearest strand chooses provenance
	*rootVeinOutset { ^46 }                 // body — route around the Coco crescents
	*rootVeinLift { ^34 }                   // body — first audio-vein control point
	*rootVeinDrop { ^58 }                   // body — final control point below the shell
	*rootVeinJoinNorm { ^0.8 }              // body fraction — a real outer spiral point
	*rootBudX { ^38 }                       // body — env follower side-growth
	*rootBudY { ^-4 }                       // body
	*rootKnotR { ^13 }                      // body — drawn two-lobed preamp knot
	*rootKnotHit { ^20 }                    // px — axis-locked drive / bias gesture
	*rootBoxPad { ^12 }                     // px — transparent local repaint layer
	*rootLabelExtent { ^Point(72, 14) }      // px — transient provenance only
	*rootLabelDrop { ^18 }                  // body — below the knot
	// The mic sprout still grows beside envA, but on root A's far side: the
	// tide pool took the water it used to stand in (§9), and a reed rising from
	// the waterline at the bay's left edge is where it belongs anyway.
	*sproutOffset { ^-58 }                  // body — the mic sprout beside envA
	*sproutTipOffset { ^-66 }               // body
	*sproutReach { ^60 }                    // body

	// ---- endpoint view extents (px, rake 12) ----
	// Most of these remain opaque boxes that eat lines and marks beneath them.
	// The four castle details are deliberately clear so the irregular castle
	// flesh, not a rectangular pad, decides what masks the river. Growing an
	// opaque owner re-runs the courtesy law; clear castle details must not add
	// synthetic repair stubs.

	*jackExtent { ^30 }                     // px, square
	*socketExtent { ^Point(24, 24) }        // px
	*lampedWidth { ^40 }                    // px — a socket that carries a lamp
	*lampOffset { ^16 }                     // px — the lamp bead's own home
	*crescentExtent { ^30 }                 // px, square
	// Only the two in/fb crescents grow downward to carry their Dolby mark.
	// The crescent itself keeps the old 30 px square and exact anchor; the
	// extra paper is below it, away from the moons.
	*dolbyCrescentHeight { ^50 }            // px — grown in/fb owner view
	// CQInk.drawJack's idle dot plus its outline. Day and night share that one
	// primitive (§16.11), so it does NOT move with the flood — but the stubs
	// drawn inside a night view have to stop at it.
	*jackDotRim { ^5 }                      // px

	// ---- the solitary moon and the bay-wide tide (§16.6) ----
	// The moon is a remote body beyond bird B, not the heading of an almanac
	// panel. Its vertical gesture briefly wears rain or sun; those glyphs have
	// geometry only so their active ink cannot clip, never as resting targets.
	*moonFraction { ^0.925 }                // of width — beyond bird B
	*moonAltitude { ^42 }                   // body — slightly above the birds
	*moonRadius { ^28 }                     // body (Ø 56)
	*moonBoxSlack { ^4 }                    // px, hairline room past all ink
	*moonHalo { ^9 }                        // px — the 120 ms shake flash
	// [name, dx, dy] from the moon; deliberately asymmetric, never a row.
	*moonWeatherPlaces { ^[[\grow, -25, 39], [\wither, 29, 35]] }

	// There is no pool body. Four memory seeds in open winds imply one large
	// rectangular bilinear field passing behind the organism. At rest the
	// rectangle itself is never drawn; these fractions place only its seeds.
	*tideLeftFraction { ^0.31 }
	*tideRightFraction { ^0.69 }
	*tideTopFraction { ^0.22 }
	*tideBottomFraction { ^0.91 }
	*stampRadius { ^9 }                     // px touch body around a tiny seed
	// The bay-wide focus is opened by a small gill below the two lower
	// castles. It is an organ of the same landscape, not a toolbar toggle:
	// closed = ordinary cable grammar, open = cables submerged + tide playable.
	*tideGateFraction { ^0.86 }             // of stage height
	*tideGateHit { ^17 }                    // px, one compact touch body
	*tideGateBoxSlack { ^2 }                // px past the hover echo
	*seedLength { ^7 }                      // px, inward-pointing kernel
	*seedWidth { ^4.5 }                     // px
	*seedHalo { ^10 }                       // px, focus / breed target bloom
	*tideGateSpan { ^14 }                   // px, half-width of the gill strokes
	*tideGateRise { ^8 }                    // px, opening breath

	// ---- drawn ink bodies (px — CQInkNight draws at these) ----
	// Not stroke widths and not type: those are frozen (§16.3, §16.4). These are
	// the sizes of the small drawn bodies themselves, and §8's law for them is
	// that information must not shrink relative to the landscape around it.

	*heartDotR { ^4 }                       // the hold heart at the spiral's eye
	*heartSealR { ^8.5 }                    // its danger ring while sealed
	*notchReach { ^6 }                      // the reel notch across the groove
	*notchHoverReach { ^9 }                 // the grab cue
	*crescentArcR { ^12 }                   // the tide crescent's own arc
	*dolbyOysterDrop { ^24 }                // px — centre-to-centre from crescent
	*dolbyOysterWidth { ^13 }               // px — half-width of the paired arcs
	*dolbyOysterOpenGap { ^4.5 }            // px — off posture
	*dolbyOysterGateGap { ^1.2 }            // px — gate's hairline opening
	*dolbyOysterHitX { ^13 }                // px — specific tap before crescent
	*dolbyOysterHitY { ^8 }                 // px
	*clockBeadR { ^3.2 }                    // the spesal state's lit bead
	*clockBeadHaloR { ^4.6 }
	*clockTickReach { ^3.5 }                // the eight tick marks' half-length
	*rangeRippleWidth { ^14 }               // px — drawn slow/mid/fast mark
	*rangeRippleHeight { ^8 }               // px
	*rangeHitX { ^10 }                      // px — cue and act around the ripple
	*rangeHitY { ^8 }                       // px
	*cometR { ^2.75 }                       // the read head
	*fireflyR { ^1.6 }                      // the river's direction carriers
	*chordLightR { ^2.2 }                   // the coupling's travelling light
	*pupilMin { ^4 }                        // the iris at qFreq 0
	*pupilRange { ^8 }                      // ...and how far it opens
	*irisGap { ^5 }                         // pupil rim to the rays' inner ends
	*irisRayBase { ^4 }                     // a calm corona's ray
	*irisRayChaos { ^5 }                    // ...and how far the storm reaches
	*irisRayJitter { ^6 }
	*birdBodyR { ^2.6 }
	*skipGlyphR { ^6 }                      // the mouth's stationary skip mark
	*chevronReach { ^6.5 }                  // the flip chevron's half-width
	*chevronRise { ^4.5 }                   // its head's depth
	*chevronShuttleRise { ^5.5 }            // ...opposed, while shuttling
	*struckBase { ^5 }                      // the hammered head's arms
	*struckRange { ^8 }
}
