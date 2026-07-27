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
	*castleBoxReach { ^45 }                 // body — the opaque castle layer
	*castleBoxSlack { ^4 }                  // px, hairline room inside that box
	*clockRadius { ^14 }                    // body — the inner spesal dial
	*rangeLetterDrop { ^41 }                // body — the whisper letter at 6 o'clock

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
	*rootReach { ^110 }                     // body — how high the tip climbs
	*rootSpread { ^40 }                     // body — base offset from the tip
	*rootTwigX { ^28 }                      // body
	*rootTwigY { ^20 }                      // body
	// The mic sprout still grows beside envA, but on root A's far side: the
	// tide pool took the water it used to stand in (§9), and a reed rising from
	// the waterline at the bay's left edge is where it belongs anyway.
	*sproutOffset { ^-58 }                  // body — the mic sprout beside envA
	*sproutTipOffset { ^-66 }               // body
	*sproutReach { ^60 }                    // body

	// ---- the opaque endpoint views (px, rake 12) ----
	// Every one of these is an opaque box that eats the lines and marks beneath
	// it. Growing one re-runs the courtesy law: the stub that used to reach its
	// dot must be re-derived from the box, never re-tuned by hand.

	*jackExtent { ^30 }                     // px, square
	*socketExtent { ^Point(24, 24) }        // px
	*lampedWidth { ^40 }                    // px — a socket that carries a lamp
	*lampOffset { ^16 }                     // px — the lamp bead's own home
	*crescentExtent { ^30 }                 // px, square
	// CQInk.drawJack's idle dot plus its outline. Day and night share that one
	// primitive (§16.11), so it does NOT move with the flood — but the stubs
	// drawn inside a night view have to stop at it.
	*jackDotRim { ^5 }                      // px

	// ---- the moon and the tide pool (§16.6, §9) ----
	// The almanac corner is gone (decided with the user 2026-07-26). Keeping the
	// three parts together in the right-hand sky fixed nothing: they were a
	// cluster of unlike things — a shake control, a matrix blend, three matrix
	// ops — grouped by FUNCTION and touching no body around them, which is what
	// a toolbar is. Everything else at night is grouped by body: this castle's
	// controls are on this castle.
	//
	// So the parts were separated and each given its own element. The **moon**
	// goes into the sky at the birds' own altitude, between castle 1's box and
	// bird B's name — the thing the birds fly under. The **tide pool** goes into
	// the water at the bottom of the bay, tangent to the stage's lower edge. The
	// roots already run past that edge, so it reads as the waterline, and
	// nothing has to be drawn to say the plants and the pool share one water. A
	// tide pool is literally the memory of a tide, which is what this control is.
	//
	// It is an ELLIPSE, not a disc, and it is tangent rather than clipped. Both
	// are load-bearing. A Qt layer is a RECTANGLE, so what the pool may occupy is
	// not the largest free disc but the largest free rect, and the bottom pocket
	// is bounded by coco 1's tide crescents above (they hang to y 437 at the
	// minimum stage), the river's own layer on the right, envA's jack on the
	// left and the waterline below: 227 × 126 px. An ellipse uses that; a disc
	// would waste half of it. Measured honestly, the move to the water buys
	// roughly the area the G3 pad already had — it buys BELONGING, not size.
	// Size has only one real lever left and it is the minimum window (§8).
	//
	// A clipped pool would put two of the tide's four corners past the frame,
	// and an unreachable corner is a memory the pad has lost; tangent, the
	// corners sit at the diagonals 0.29 × ry above the waterline while the
	// shore's own wobble kisses and crosses it.

	*moonFraction { ^0.617 }                // of the width — the open sky
	*poolFraction { ^0.2742 }               // of the width — the bottom pocket
	*moonRadius { ^32 }                     // body (Ø 64)
	*moonBoxSlack { ^4 }                    // px, hairline room past the halo
	*moonHalo { ^9 }                        // px — the 120 ms flash reach
	*poolRx { ^100 }                        // body
	*poolRy { ^59 }                         // body — cy = height - ry (tangent)
	*poolWobble { ^0.06 }                   // seeded shore irregularity (§16.3)
	*poolAnchors { ^9 }                     // shore anchors the blob runs through
	// the pool answers a little past its drawn shore, exactly as a castle body
	// does (`bodyMargin`). That band is what makes the tide's pure corners
	// reachable: a press outside the shore holds at the nearest place ON it, and
	// the shore at a diagonal is exactly a bilinear corner.
	*poolCatch { ^10 }                      // px
	*stampInset { ^14 }                     // body — seeds inside the shore
	*stampRadius { ^9 }                     // px — under the 8 px tap threshold
	// The weather row is gone, and the three marks did not stay together — they
	// went where each one ACTS. Rain and sun grow and wither the live bay, not
	// the pool's memories, so they are sky weather and they join the moon, which
	// is also a whole-bay act (sowing). The graft breeds two stamped memories
	// and takes its two parents as seed taps, so it stays on the pool where its
	// parents are: a mark whose gesture ends on the seeds may not live a bay
	// away from them.
	//
	// Sky: [name, dx, dy] as body offsets from the moon. Pool: [name, degrees,
	// radial factor] in the ellipse's own frame, so > 1 stands off the shore.
	// Unequal on purpose — no mark may line up with another mark, with a seed or
	// with the moon on either axis. CQNightGeometryTest holds that.
	*skyWeatherPlaces { ^[[\grow, -74, -12], [\wither, -60, 46]] }
	*poolWeatherPlaces { ^[[\breed, 40, 1.13]] }
	*compassBoxPad { ^4 }                   // px

	// ---- drawn ink bodies (px — CQInkNight draws at these) ----
	// Not stroke widths and not type: those are frozen (§16.3, §16.4). These are
	// the sizes of the small drawn bodies themselves, and §8's law for them is
	// that information must not shrink relative to the landscape around it.

	*heartDotR { ^4 }                       // the hold heart at the spiral's eye
	*heartSealR { ^8.5 }                    // its danger ring while sealed
	*notchReach { ^6 }                      // the reel notch across the groove
	*notchHoverReach { ^9 }                 // the grab cue
	*crescentArcR { ^12 }                   // the tide crescent's own arc
	*clockBeadR { ^3.2 }                    // the spesal state's lit bead
	*clockBeadHaloR { ^4.6 }
	*clockTickReach { ^3.5 }                // the eight tick marks' half-length
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
