// CQNightMeasures — the night's measures, in one table (GUI_SPEC Night geometry).
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
	// A castle's audio send is a short inward tributary between its clock and
	// its inner shoreline. The whole visible island owns the radial gesture;
	// these two body measures only keep the resting ink out of the dial/rim.
	*castleSendClockGap { ^5 }              // body — clock rim to tributary
	*castleSendBeyondShore { ^7 }           // body — point into the central river

	// the four castle endpoints on their clock face (body radii from the centre)
	*castleJackOrbit { ^52 }                // tri at 1:30, cas at 10:30
	*castleFmOrbit { ^45 }                  // the fm socket at 4 o'clock
	// the lamped spesal view is the widest box a castle carries; this orbit is
	// what keeps it off the inner clock at every scale (CQNightGeometryTest)
	*castleSpesalOrbit { ^61 }

	*bodyMargin { ^12 }                     // px — island-ground margin beyond the playable blob

	// ---- the shells ----

	// PLAY-8b moves each complete shell organism 30 body px outward. At the
	// reference stage this opens a 114 px clear shore between ring and shell,
	// while the mirrored Record papers still retain >60 px of outer margin.
	// CQNightGeometryTest holds both the inner breath and the stage edges.
	*shellGap { ^228 }                      // body — ring rim to shell centre
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
	// The promoted performance mix grows on the shell instead of in Master.
	// Level hugs only the exterior shore; q/x valves occupy the two clear inner
	// quadrants. All body offsets scale, while the hand reaches below stay px.
	*levelShoreOutset { ^10 }               // body — outside the hold/seal rim
	*levelShoreSweep { ^0.86pi }            // body angle — exterior half, with breath
	*flowValveOutset { ^8 }                 // body — just outside the shell rim
	// Raised 28 -> 40 in the PLAY-6.1 review. At 28 the right q stream ran
	// through castle 2's tri leaf (measured -0.2 px, i.e. touching), so that
	// output looked joined to the stream instead of to its own castle; the left
	// side had the same fault latent at 5.4 px from cas 5's crown. Both valves
	// moved together, because two shells are one organ drawn twice. The raise
	// also lets the q bow relax back to its nominal lift: the chord no longer
	// has to arc over a jack it used to run straight through.
	*slotQValveAngle { ^40 }                // degrees above the inward axis
	*slotXValveAngle { ^62 }                // degrees above the inward axis
	*levelShoreHit { ^10 }                  // px — distance from drawn shoreline
	*flowValveHit { ^17 }                   // px — q/x gill hand
	// PLAY-8 anatomical socket clearances. Endpoint papers stay fixed in px, so
	// these do too: each socket clears its control's hit body without drifting
	// away from the organ it belongs to as the stage grows.
	*organInputOutset { ^48 }               // px — outward from the organ fruit jack
	*slotQInputGap { ^34 }                  // px — beside the Q gill toward the ring
	*slotXInputGap { ^32 }                  // px — above/outward from the X gill
	*slotXInputTilt { ^15 }                 // degrees outward from vertical
	*recordInputOutset { ^38 }              // px — just beyond the upper outer rim
	*modInputStop { ^7 }                    // px — semantic root stops at socket ink
	*organPodInner { ^18 }                  // px — jack keeps the fruit's seed
	*organPodOuter { ^32 }                  // px — annular audible-organ hand

	// Each converter exhales into its own inner-lower veil. The body grows from
	// the shell toward the bay; a transverse seed carries the mist↔chip axis.
	// Its ownership root is deliberately detached from the crowded shell: the
	// mirrored body and its inward-pointing breath say which Coco owns it.
	*dustBodyReach { ^240 }                  // body — Coco centre to veil centre
	*dustBodyAngle { ^32 }                   // degrees below the inward axis
	*dustRx { ^53 }                          // body — along the radial amount axis
	*dustRy { ^36 }                          // body — across the grain axis
	*dustSocketInset { ^48 }                 // body — Coco-side rim from veil centre
	*dustTailLength { ^24 }                  // body — detached root back toward Coco
	*dustBodyClear { ^12 }                   // px — contour breath from other bodies
	*dustTailClear { ^10 }                   // px — detached root breath from controls
	*dustParticleCount { ^36 }               // static seeded specks at full amount
	*dustParticleHalfMin { ^0.7 }            // px — mist-end half-length
	*dustParticleHalfGrain { ^1.4 }          // px — added at the chip end
	*dustParticleLengthJitter { ^0.16 }      // proportion, seeded once
	*dustParticleAlphaJitter { ^0.16 }       // proportion, seeded once
	*dustParticleSlant { ^0.16 }             // radians around the grain axis
	*dustBoxPad { ^8 }                       // px — clear paper around veil ink
	*dustHitSlack { ^6 }                     // px — irregular veil hand tolerance
	*dustSensitivity { ^120 }                // px for one normalized axis span

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

	// ---- the internal audio topology (PLAY-6.1) ----
	// The drone streams are landscape, not placed lines. qMix leaves the river
	// at the two empty gaps beside castle 1, and the two cross-feed lanes share
	// one arch OVER castle 1 instead of cutting through the ring's interior —
	// that interior belongs to the iris, the fireflies, the cables and the
	// lasso. The bow and the crown are body measures so both keep their
	// proportion to the landscape they span; the top floor is what holds the
	// arch on the stage when castle 1 has climbed close to the upper edge.
	*slotQLift { ^10 }                      // body — nominal bow of the q stream
	*slotXBridgeClearance { ^12 }           // body — crown above castle 1's bodies
	*slotXBridgeTopFloor { ^12 }            // body — nearest the stage's top edge
	*slotXBridgeLaneGap { ^10 }             // body — the two lanes' separation

	// Neither nominal above is a promise, because both streams run past seeded
	// and placed bodies: castle 2's tri jack sits within a pixel of the straight
	// chord out of its own gap, and at the minimum stage the birds' fixed 200 px
	// name plates overhang both x valves. A constant inset cannot clear a body
	// it did not measure (the law the preamp labels taught in N7), so each
	// stream gives ground — the q bow deepens, the x shoulders draw inward —
	// until this much clear paper stands between its ink and theirs. Hand-sized
	// px, so a grown stage spends the slack on the landscape instead.
	*flowBodyClear { ^8 }                   // px — a stream's breath past ink
	*flowGiveWayStep { ^2 }                 // px — how finely it gives ground
	*flowGiveWayMax { ^180 }                // px — cap: a cramped stage still draws
	// How far an endpoint's OWN ink actually reaches from its centre. The dot's
	// rim is not the answer: a `tri` jack wears a leaf out to (12, -9) = 15 px,
	// a `cas` jack a crown, `org` a stem. Measuring the dot alone is what let a
	// stream pass 14.8 px from a jack centre and still touch its leaf.
	*endpointInkReach { ^15 }               // px

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
	*rootVeinOutset { ^80 }                 // body — route around the Coco crescents
	*rootVeinLift { ^38 }                   // body — first audio-vein control point
	*rootVeinDrop { ^64 }                   // body — final control point below the shell
	*rootVeinJoinNorm { ^0.8 }              // body fraction — a real outer spiral point
	*rootBudX { ^44 }                       // body — env follower side-growth
	*rootBudY { ^-4 }                       // body
	*rootKnotR { ^18 }                      // body — drawn two-lobed preamp knot
	*rootKnotHit { ^27 }                    // px — axis-locked drive / bias gesture
	*rootLobeOffset { ^0.42 }               // × knot radius, above/below centre
	*rootLobeRx { ^0.72 }                   // × knot radius
	*rootLobeRy { ^0.48 }                   // × knot radius
	*rootBiasTopGrowth { ^0.48 }            // max outgoing-lobe swell
	*rootBiasBottomShrink { ^0.30 }         // max intake-lobe pinch
	*rootDolbyClear { ^12 }                 // px — full bias envelope below dolby
	*rootStageClear { ^8 }                  // px — full lobe envelope above waterline
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
	// Only the two in/fb crescents grow downward to carry their dolby mark.
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
	*tideTopFraction { ^0.13 }
	*tideBottomFraction { ^0.90 }
	*stampRadius { ^14 }                    // px touch body around a tiny seed
	*seedMoat { ^5 }                        // px ground wash around seed leaves
	// The bay-wide focus is opened by a small gill below the two lower
	// castles. It is an organ of the same landscape, not a toolbar toggle:
	// closed = ordinary cable grammar, open = cables submerged + tide playable.
	*tideGateFraction { ^0.94 }             // of stage height, below raised field
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
	*castleSendPearlR { ^2.1 }              // the tributary's stored amount
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
	*organPodR { ^27 }                      // the yellow flesh around its jack
	*flowGillHalf { ^9 }                    // q/x valve length along the stream
	*flowGillGap { ^5 }                     // opening at a fully raised slot
	*flowArrowR { ^4.5 }                    // directional bridge arrow
}
