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
	*sproutOffset { ^60 }                   // body — the mic sprout beside envA
	*sproutTipOffset { ^70 }                // body
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

	// ---- the almanac corner (§16.6; §9 gives it its landscape) ----

	*almanacInset { ^6 }                    // body — clear of coco 2's box
	*moonAxis { ^80 }                       // body — inset from the right edge
	*moonRadius { ^26 }                     // body
	*moonTop { ^18 }                        // body
	*moonBoxSlack { ^14 }                   // body
	*moonHalo { ^7 }                        // px — the 120 ms flash reach
	*padExtent { ^140 }                     // body — §7.1's compass water
	*padGap { ^14 }                         // body
	*weatherGap { ^16 }                     // body — the row below the field
	*compassBoxPad { ^8 }                   // px
	*compassBoxFoot { ^12 }                 // px
	*stampInset { ^13 }                     // body — seeds inside the shore
	*stampRadius { ^9 }                     // px — under the 8 px tap threshold

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
