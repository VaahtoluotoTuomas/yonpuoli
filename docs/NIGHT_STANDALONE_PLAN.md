# NIGHT_STANDALONE_PLAN.md — the night as the whole instrument

> Status: **proposal for handoff, not yet normative.** Drafted 2026-07-22 at the
> user's direction, after G3's first runnable version landed (`c305a25`);
> extended the same day with the full night-layout and visual plan (§7–§12),
> the ear pass — the musical tuning experiments (§13) — and the norns-port
> waypoints (§14).
> It plans the road to a night-first instrument: three playing additions
> (range switching, dolby switching, a momentary hold), a re-composition of
> the night landscape (the spring flood, the almanac's integration, the
> roots, the birds' fetching), and the decision path for the Day panel — in
> enough detail that an implementer who has read `CLAUDE.md`, `GUI_SPEC.md`
> (all of it, especially §4.5, §6.1, §10, §16.4–§16.6, §16.12) and `PLAN.md`
> can build it without stepping on the project's known rakes. Nothing here
> changes DSP, CQSpec ranges, or safety (SPEC §8). **Its prerequisites are now
> met** (G2.12–G2.13 and the G3.1 review were all signed off 2026-07-22;
> archipelago is merged to `main`, tag `archipelago-1.0`), so this is the
> **active roadmap for the `nocturne` branch** — `PLAN.md` is its status view.
> Where this plan supersedes a GUI_SPEC recipe (it does, in §9), the spec edit
> is part of the increment that lands it, as always.
> The N8 Drawer-workshop handoff was coverage- and visual-audited on
> 2026-07-28 and is specified in §12.1. That section, rather than the old
> one-paragraph sketch below, is the implementation contract for N8.
> On 2026-07-28 the user made and accepted the §0 verdict: Night is the sole
> instrument surface. The reversible N9 cut passed its live visual/playing
> review on `nocturne-night-only`; this document records the decision and the
> two overview deaths below.

---

## 0. Context — the day-mode question

The user decided on 2026-07-28 that the instrument is more coherent and more
itself as a **night-only instrument**, with the Drawer holding the precise,
menu-like remainder. Day felt dull and uninspiring, added little that Night
and the Drawer could not already do, and would turn every future interaction
change into maintenance across two interfaces.

The prerequisites are met:

- the Drawer **workshop** carries the 26 setup CQSpec parameters with honest
  ledger controls. After the 2026-08-02 PLAY-6 drone promotion, Night 44 ·
  Master 4 · Workshop 26 are disjoint and their tested union is exactly all
  74 `CQSpec.names`; `micGain`
  has one interactive home and keeps its warning;
- Day's **spesal constellation is recorded dead**, not transplanted. Night's
  under-hand coupling chord carries the same partner, mode and signal direction
  exactly where the player hovers and steps the inner clock. A permanent
  whole-network diagram would spend ink continuously on information whose
  consequence is already clearer at the action point;
- Day's **static Coco memory-path map is recorded dead**, not transplanted. It
  was read-only and non-live; Night's shells expose the playable loop body and
  Workshop exposes the exact slot/converter controls. Keeping another diagram
  would add maintenance without adding an act.

N9 therefore removes the Day builders, its toolbar destination and its two
overview drawings in one reversible structural gate — no runtime flag and no
parallel product. This changes no DSP, CQSpec range, snapshot rule, keyboard
map or safety invariant. The automated gate and the required live
visual/playing review passed on 2026-07-28; N9 is accepted.

---

## 1. Laws that bind all three additions

These are restatements, not new rules. The implementer must not rediscover
them the hard way.

1. **One funnel.** Every write goes through `CQState.set` (params) exactly
   like `CQGuiNightTouch.prToggle` does today. No new write path, no undo
   (§14: parameter gestures have no undo), no spellbook entries (it hooks
   the matrix, not params).
2. **Parameters exist exactly once — in `CQSpec`.** The enum-cycling helper
   (needed by ranges and dolby) must read the enum's choices from CQSpec.
   If CQSpec has no public accessor for an enum's choice list, **add one
   there**; never write `[\infra, \balcium, \audio]` or `[\off, \gate,
   \duck]` at a call site. (CLAUDE.md style law.)
3. **Pure geometry, tested clearances.** Every new anchor and hit test goes
   into `CQNightGeometry` as pure math (anchors into `compute`'s castle/coco
   events, hit tests as class methods), and `CQNightGeometryTest` gets
   asserts **at both reference stage sizes** (1280×570 and 1920×855). The
   castle bodies carry seeded ±8 % jitter — the seed is fixed inside
   `compute`, so the asserts are deterministic. Do not eyeball clearances.
4. **Opaque boxes eat lines and marks.** Most endpoint views remain opaque
   boxes (jacks, lamped sockets, crescents). The four detail views surrounding
   each castle are the deliberate exception: transparent paper lets the
   irregular flesh, not a rectangle, decide what masks the river. A mark near
   any remaining opaque box can still be silently covered. Either draw it
   inside its owner, route it to the top gesture layer, or assert
   box-vs-mark clearance in the geometry test.
5. **The §4.5 priority chain, identically in hover and press.** Unless a
   reviewed exception is named, order inside every `Down` handler is: cable
   under the pointer wins (`overlay.patchAt`), then the new touch point, then
   the existing touch points, then body-solo/ground fallthrough. The hover cue
   must be computed with the *same* priority — a cue that advertises an action
   the next press will not receive is a lying cue. N3 closed with one
   deliberately deferred exception: the Tab-equivalent tide gill must
   eventually precede cable hit-testing and keep a discoverable top-layer cue.
   That was the only exception in this increment. The later PLAY-7 dense-patch
   review (2026-08-02) explicitly granted the four exact SlotQ/SlotX bridge-end
   gills the same hover/plain-left priority; no surrounding shell area changed.
6. **Redraw discipline (§10).** New marks live on the owning opaque layer
   (castle view, shell view, crescent view), repaint only that layer, only
   when their state changes. No timers, no telemetry additions, no ambient
   motion — all three additions are *settings*, drawn as static ink, and the
   drawn regime IS the display (§16.6's "show the regime" family).
7. **Workflow.** Separate small increments, each with its own manual
   checklist block, its own visual-review stop with the user, and the full
   battery (`compile_check` → `run_all` → `nrt_safety`, plus the four GUI
   smokes) after every `classes/` change. Remind the user about
   Ctrl+Shift+L before live testing. The build order is §12 — note that it
   deliberately puts the flood (§8) **before** ranges and dolby, so their
   clearance work is done once, in the final landscape.

---

## 2. Feature A — the punch (momentary hold)

**What it is musically.** Loopers punch: with the loop sealed, hold the
valve open for a moment and new material drops into the ring; with the loop
open, seal it for a moment and a fragment freezes. Both are one gesture:
*invert `cocoNHold` while pressed, restore on release.*

**Gesture.** A **right-button press held on the shell's heart**. Rationale:
the app's gesture discriminator idiom is *the hand, not the clock* — the
lasso and the moon split tap/drag by 8 px of travel, never by time. A
long-press timer would make a slow tap silently become a no-op
(punch+restore), which is exactly the "broken control" failure the new-moon
no-op taught. The right button is free at the heart: §4.4's lasso may only
start on empty ground, and the heart is a control.

- Left tap on the heart: unchanged (toggle, as today).
- Right press on the heart: `punch = (index: i, before: state.get(\cocoNHold))`,
  then `state.set(\cocoNHold, 1 - before)`. Release: `state.set` back to
  `before`, clear `punch`.
- The seal ring and heart already repaint from the param updater
  (`holdStates`), so the display is free and truthful — while punched, the
  spiral visibly seals/cracks.

**The one real trap — exit paths.** A stuck punch is destructive (a loop
left recording overwrites itself). The punch needs §4.1's discipline:
enumerate every exit and restore on all of them — mouse up (Qt grabs the
press, so it always arrives at the shell view), **Esc**, **Tab water-focus toggle**,
**window close / GUI free**, **panic**, **CmdPeriod**. The plumbing already
exists: `CQGui.prKey` (Esc and Tab branches) and `CQGui.cancelAudition`
already call `nightMap.cancelWeather`; extend that cancel path (or add a
sibling `cancelPunch` called from the same places, including
`CQGuiNightMap.free`) to restore and clear an active punch. The night smoke
must assert matrix-style equality of the hold param after *each* exit path,
exactly as the audition-leak asserts do.

**Code touch points.** `CQGuiNightTouch.shellDown/shellUp` — note the
current `shellDown` forwards **every** non-left button to the ground grammar
*before* checking any touch point. The punch requires reordering for
button 1 only: cable check first (a right press on a cable crossing the
heart must still rip, §4.5), then heart-hit → punch, then the existing
forward. Do not let button 1 reach the wind/chevron/skip checks.

**Optional, ask the user first:** Shift+1/Shift+2 as keyboard punch. The
sacred key map is frozen; additions need the user's blessing. If accepted,
beware **Qt key autorepeat**: keyDown repeats while held — track an
`isDown` flag and ignore repeats, restore on keyUp (`keyUpAction` at window
level, same place the map lives).

**Spec/docs to edit (after review, as usual):** GUI_SPEC §6.1 table (one
row: *the heart's punch*), §16.6 shell paragraph (one sentence), manual
checklist block, PLAN.md log.

---

## 3. Feature B — range marks (the ripple)

> **Status — accepted 2026-07-27 (checklist N4).** The final reviewed form is
> the captionless keel/ripple v2 described below.

**What it is musically.** The castle range switch (\infra ×0.02 / \balcium
×1 / \audio ×40) is how a castle stops being a modulator and starts being a
voice. On the hardware it is a front-panel switch; before N4 the night only
showed a display-only i/b/a whisper. This is the single biggest gap in
night-as-instrument.

**Design — a drawn rate, not a widget and not a letter.** §6 forbids
anything that looks like a knob or transplanted widget; §16.4 forbids a
control that requires reading text (the sower constellation died of this).
So the control is a **rate ripple**: a small drawn mark (~14×8 px box, §16.5
style) at the castle's 6 o'clock anchor, whose *form is the regime*:

- `\infra` — one long shallow swell (a single half-wave);
- `\balcium` — two half-waves (one full cycle);
- `\audio` — five tight half-waves (a dense ripple).

Slow/mid/fast reads pre-verbally — it is §16.6's "show the regime" law made
into a control. **N4 review amendment, 2026-07-27:** the user removed the
existing i/b/a caption as unnecessary inherited Cocoquantus vocabulary; form
alone carries the regime. The same review found balcium's one free cycle too
close to the §16.5 `wave` glyph in castle-fm sockets, so every ripple gained a
short common keel. The fm wave remains a free cycle inside a socket ring; the
rate ripple varies its density against the keel and lives on the body. The
distinction is form, never hue alone (§16.2).

**Interaction.** Tap cycles `\infra → \balcium → \audio → \infra` — the
night's established idiom for stepping an enum is the tap (the spesal dial).
Hover brightens the ripple to line ink (static, §4.5). No flash: a range is
a setting, and the mark itself redraws to the new density instantly via the
already-registered `cNRange` updater in `CQGuiNightMap.prRegisterUpdaters`.

**Geometry and its traps.**
- `rangeMark` follows the seeded blob's actual lowest point with a fixed
  13 px breath; `rangeHit(castle, point)` is a markHit-style 10 px box,
  verified against the ripple's drawn extent — cue and act, one geometry.
- **Clearance asserts needed at both sizes**: the mark's hit box versus
  (a) the spesal endpoint's opaque 34 px lamped box at 8 o'clock r = 40 —
  this is the crowded neighbor; (b) the fm socket box at 4 o'clock r = 28;
  (c) the dial annulus (r 10 ± 8); (d) the body ellipse with its ±8 %
  jitter and 10 px `bodyMargin` — at maximum jitter the body's cue edge
  comes within a couple of px of r 33, so `castleDown` must consult
  `rangeHit` **before** `bodyHit`, and `prHoverCastle` must do the same
  (three exclusive cues on one castle now: clock, ripple, body — never two
  lit at once).
- The ripple is drawn by the castle's own transparent view (`prDrawCastle` →
  `CQInkNight`), so its repaint stays local and must fit inside the view box.
  Two crossings were found within 1.55 px of the river in N4 review. A 3 px
  ground under-stroke now follows only the ripple's vector paths before its
  visible hairline; no rectangle, caption or synthetic river stub returns.

**Funnel.** `state.set(\cNRange, next)` via the shared enum-cycle helper
(law 1.2). CQState/CQSpec clamp malformed enums already; the GUI never
stores the symbol list itself.

**Day parity.** Day's 3-state switches are untouched. (If day later dies,
the drawer workshop inherits a switch — not this gate's problem.)

**Spec/docs to edit:** GUI_SPEC §6 (strike "display only; ranges are set in
day"), §6.1 table row, §16.5 (ripple recipe), §16.6 castle anatomy, manual
checklist block (tap each castle through all six steps *by ear*: an infra
castle stepping to audio must be immediately audible in the isolated
`\quantumDrone` listening setup), PLAN.

---

## 4. Feature C — the dolby oysters

**What it is musically.** Per coco channel (input, feedback): \off /
\gate (downward expander on its own signal) / \duck (attenuated by the
*other* channel's envelope) — the hardware's "monophonic articulations of
time-layers". Two independent 3-state switches per shell.

**Design — an oyster at the channel's own hand.** Proximity is meaning
(§1): the in and fb channels already have hands at night — their tide
crescents. The dolby control sits directly beneath each of those two
crescents (spaf gets none; deck crescents get none). The mark is a pair of
short arcs — an oyster, an eyelid — whose posture is the regime:

- `\off` — the two arcs relaxed apart, hollow whisper: the channel never
  blinks;
- `\gate` — the arcs drawn nearly shut with a hairline gap: it closes on
  its own silence;
- `\duck` — the arcs shut, plus a short arrival stroke entering **from the
  direction of the partner channel's crescent** (fb's direction for the in
  oyster, in's direction for the fb oyster): the other channel presses it
  shut. The stroke's direction is honest signal flow — the same law that
  reversed the spesal links (SPEC §5.3: dolby-in is keyed by the feedback
  envelope and vice versa). Never encode the difference by hue alone
  (§16.2): posture first.

**Interaction.** Tap cycles `\off → \gate → \duck`. Hover brightens the
oyster (static). No telemetry, no breathing: the mark shows the *setting*.
(A future ambient "the oyster actually blinking with the gate" would need
its own telemetry stream and its own §16.9 review — explicitly out of
scope; do not fake it client-side from meters.)

**Geometry and its traps.**
- The crescent views are **24×24 opaque boxes** placed center-anchored
  (`prPlaceCrescent`). The cleanest home for the oyster is *inside a grown
  crescent view* (e.g. 24×40, grown downward/outward only, so the crescent
  arc itself stays exactly on its geometry point): the mark then lives in
  its own opaque layer, repaints locally, and can never be covered by
  another box. This means per-name view sizes — only `cocoNIn` and
  `cocoNFb` grow; `cocoNSpaf` and the deck crescents keep 24×24. Grow
  **away from** the moons arc above (moons r = 104·scale, crescents
  r = 130·scale — outward is free water; assert it).
- Inside the grown view, split the hit: the crescent's drag surface on the
  arc's own half, the oyster's tap on a markHit box at its anchor.
  `crescentDown` must consult the oyster **first** (it is the smaller,
  more specific target), and a drag may never start from the oyster's box.
  The §6.1 crescent sensitivity and gesture must remain bit-identical —
  the night smoke already asserts the crescent pour; keep those asserts
  green untouched.
- 24 px center-to-center hit law (§16.10) between the oyster and the
  crescent arc and between neighboring crescents: assert, both sizes.
- The night carries no labels for these (§16.4): the oyster's posture must
  be readable at 100 % scale from two meters (§16.2 squint test). If the
  review says it is too small, grow the mark, not a label.

**Funnel.** `state.set(\cocoNDolbyIn / \cocoNDolbyFb, next)` via the shared
enum helper. Enums clamp in the funnel.

**Spec/docs to edit:** GUI_SPEC §6.1 (strike dolby from the "remains day
work" sentence; add the row), §16.5 (oyster recipe, 3 postures), §16.6
shell paragraph, manual checklist block (the M5 listening figure: patch a
loop, set fb-duck, hear the time-layers articulate; then gate on input and
hear the silence close), PLAN.

---

## 5. Test plan (all three)

- **Unit:** enum-cycle helper (wraps, clamps, reads choices from CQSpec —
  assert it has no literal symbol lists by construction); new
  `CQNightGeometry` hit tests; clearance asserts at both stage sizes for
  every new anchor/box (pattern: `test_almanacCorner` / `test_compassWater`).
- **Night smoke (`gui_night_smoke.scd`):** per feature — tap cycles the
  param through the funnel and back to start; cable-crossing wins the press
  at each new target; hover cues are exclusive (clock/ripple/body; oyster
  vs crescent); the punch inverts and restores on mouse-up **and on every
  §4.1-style exit path**; a right press on a cable over the heart rips
  instead of punching. Remember the smoke's stdout is block-buffered when
  redirected — the 90 s watchdog exit is what flushes evidence; keep it.
- **Battery:** the usual markers after every `classes/` change. No DSP is
  touched, so `CQ_HOLD_OK` carries over; say so in the gate note.
- **Manual:** one checklist block per feature, copy-pasteable SC fences,
  ear-first (a range step must be *heard*, dolby must articulate, a punch
  must drop material into a sealed loop and give it back).

---

## 6. The rakes, collected (read before coding)

1. `sclang` requires `var` declarations first in every function body.
2. `Rect.extent` returns a **Point** — `.x/.y`, not `.width/.height`.
3. Never render an `Image` inside a paint event; never batch cables into
   one giant Pen path (both measured fatal — GUI_SPEC §11). Not directly
   relevant here, but binding if any drawing is moved.
4. A hung GUI smoke: check the physical mouse cursor position first (Qt
   enter/leave deadlock), then remember stdout buffering (watchdog exit),
   then suspect the code.
5. `CQGuiWidgets.register` is the **CQSpec param** registry and primes from
   `CQState` — fine for `cNRange`/dolby/hold updaters (they are params),
   but never register a GUI-only constant there.
6. Repo files are UTF-8 with Finnish text and `§`/`→` — never edit with
   PowerShell text cmdlets; use proper file tools.
7. `Collection:includes` compares identity — membership tables hold
   Symbols/Integers, never Strings.
8. View swaps stay queued (`CQGuiShell.prSwapStage`); nothing here may hide
   a view under the pointer with a direct write.
9. The night smoke's nesting is deep; new checks go in as top-level
   functions called from the sequence (see `compassChecks` /
   `weatherAsync` in `gui_night_smoke.scd`) — do not add a 15-tab pyramid.
10. Every visible increment stops for the user's visual review before the
    next one starts, and lands as its own small commit on `archipelago`.
11. **(flood)** Some smoke steps press endpoints at hardcoded local points
    (`mouseDownAction.value(view, 13, 13, …)` — the center of a 26 px jack
    view). When view sizes change, those literals lie. Replace them with
    reads of `endpoint[\anchor]` / geometry anchors *before* touching the
    constants, or the smokes will fail for reasons that look like layout
    bugs.
12. **(flood)** Growing an opaque endpoint view re-triggers the courtesy law
    (§16.6 — a line under opaque paper must be redrawn by that owner or routed
    clear). Root arrivals and org stems remain derived from their current box.
    Castle details are transparent and must never regain synthetic
    rectangle-repair passages or protruding stubs.
13. **(pool)** The disc→square mapping in `compassNorm` must keep the four
    corners *reachable* (rim at the diagonals maps to exactly (0,0)…(1,1)),
    or a stamped corner can never be recalled bit-exact and the smoke's
    corner-recall assert fails. Unit-test the mapping's fixed points before
    wiring it to the view. Built: the elliptical grid mapping, whose
    **inverse has a square-root singularity at exactly those corners** — it
    returned 1±2e-8, not 1. Fix it at the source with an edge snap
    (`CQNightGeometry.cornerSnap`, 1e-6 ≈ 2.4e-13 of the radius), never by
    loosening the assert: the assert was measuring the real thing.
13b. **(pool)** **A Qt layer is a rectangle.** When judging how much room a
    new body has, measure the largest free *rect*, not the largest free
    disc — the disc overstates it, and an area claim made from one is
    wrong. This is what made "the bottom water gives the pool ~1.8× the
    area" false; the true figure was 0.95×.
13c. **(pool)** Moving an interactive body out of reserved water and into the
    bay puts it **under the cable field**, and §4.5 hands the cable the
    press. Before proposing such a move, measure the shadowing over many
    random bays at the real cable densities (see the N3 numbers) and put it
    to the user as a trade — do not discover it after the build, and do not
    invent a local priority exception to hide it.
14. **(birds)** `Dialog.openPanel` is asynchronous and cannot run in a
    headless smoke. The tap handler must route through an injectable
    function (default = the real dialog) so the smoke can stub it and
    assert the routing; the callback must guard an unbooted/freed deck
    exactly as the drop path does (`prDeckFor`), and the deck's own
    last-request-wins law (SPEC §5.1) already covers a stale callback.
15. **(birds)** Qt wheel events arrive with momentum on some devices —
    step the folder on the wheel's *sign with a per-gesture cooldown*
    (~150 ms between steps is a hand, not a timer-driven display; compare
    the existing wheel-step semantics on sockets), or one flick skips five
    files.

---

## 7. The layout verdict — re-composition, not redesign

The question asked: does the night layout need radical redesign or small
tuning? The answer this plan takes: **neither — it needs a re-composition.**
The topology is proven and stays untouched: castles in a ring at the center,
the eye in the ring, shells left and right, birds above, roots below. That
is the poem's structure, it was reviewed into shape over two gates, and no
musical gain justifies burning it. What is wrong is more subtle, and it is
two things:

1. **The measures are timid.** The landscape was composed fit-first for the
   1280×570 minimum stage, and every element took the smallest size that
   passed review. On a laptop the bay reads beautiful but distant — marks
   the hand must aim at (the inner clock, the heart, the crescents, the
   eye) are 10–20 px affairs swimming in open water. The emptiness itself
   is not the flaw — the lasso needs ground and the night needs quiet
   (§16.12 forbids filling it with decoration) — but the *information* is
   smaller than the water asks for. PLAN.md already records this
   observation ("Koko Night-näkymän mittakaava", G2.5) as one deliberate
   scale decision waiting to be made. §8 makes it.

2. **The later additions were composed as clearances, not as landscape.**
   The moon, the compass and the weather row are each individually right,
   but together they sit in a right-edge column — moon over pad over row,
   all on one axis — and a column of aligned tools is a toolbar wearing a
   costume, exactly the smell the sower constellation died of. §9 fixes it.

   **Corrected 2026-07-27 — this paragraph used to end "the fix is not to
   move them somewhere else (the sky is where an almanac lives, and the
   bottom band has no room at the minimum stage); it is to break the axis
   and let the sky compose them as bodies."** That was written from the
   wrong diagnosis and it cost a whole rejected build: an implementer who
   follows it keeps all three parts in the same patch of right-hand sky and
   only rearranges them, which was tried and does not work. The axis is a
   symptom. **The fault is that the three parts are a cluster of unlike
   things grouped by function** — a shake control, a matrix blend, three
   matrix ops — sitting in reserved water and touching no body around them.
   That is the definition of a toolbar, and re-composing a toolbar leaves a
   toolbar. Everything else at night is grouped by **body**: this castle's
   controls are on this castle. So the fix is to break the cluster and send
   each part where it *acts*, into the landscape, and to accept that the
   parts then live far apart. §9 (rewritten) does that.

Everything the reviews already fought for is **invariant** under this plan:
the touch grammar's semantics and thresholds, the cable grammar, the
telemetry laws (stillness, no-telemetry-is-not-zero, frames-drive-motion),
the drawn-outline-is-the-body law, every §16.2 color, every §16.3 stroke
width and §16.4 type size (ink and type do NOT scale — §16.10 is law), the
skip mark's stationary home, the reel notch, the struck twinkle, the chords
shown only under the hand. The flood changes *measures*; nothing changes
*meanings*.

---

## 8. The spring flood — the size pass

One coordinated increment that raises the base sizes of the bodies and
touch targets at the minimum stage, so the bay fills its own water. Not a
multiplier on `scale` — a re-basing of the geometry constants, tuned as one
composition.

**Method (this is the important part — the numbers below are starting
points, the method is binding):**

1. Name the constants. The sizes now scattered as literals through
   `CQNightGeometry.compute` become named class constants (`castleRx`,
   `clockR`, `eyeReach`, …) so the flood is one visible table, not thirty
   edits.
2. **Write the collision matrix first.** Extend `CQNightGeometryTest` with
   the pair asserts *before* touching any constant, at both reference
   sizes: bird (grown) + its label vs castle boxes and the ring's top;
   shell boxes vs stage edges; crescent arc vs moons arc; grown clock vs
   grown spesal box; pool vs bird B's dropRect and shell box; roots vs
   crescents and the master strip line; every §16.10 socket-distance pair.
   Then tune constants red→green. The castle jitter is seeded, so the
   asserts are deterministic.
3. Tune by eye with the user **at two sizes**: the 1280×720 minimum window
   and their laptop maximized — §15's own law. One review stop for the
   whole flood; element sizes are relational and cannot be approved one at
   a time.
4. The flood also serves the recorded touch-screen ambition ("tehdään
   mieluiten samalla kertaa"): the targets below move toward ~40 px-class
   hit areas without committing to multi-touch yet.

**Starting-point table (min stage, scale 1 — current → target):**

| element | now | target | why |
|---|---|---|---|
| ring radius | 148 | ~158–165 | bounded above by the bird band — the matrix decides |
| castle body | 46×40 | ~58×50 | the night's finest bodies; also makes room for the ripple |
| inner clock r / hit | 10 / ±8 | 14 / ±10 | a primary playing target |
| spesal moon orbit | r 40 | ~r 48 | the clock grew; keep the lamped box clear |
| the eye (layer / pupil / rays / hit) | 44 / Ø 5–14 / ~25 / r 12 | ~96 / Ø 8–24 / ~40 / r 24 | the two-knob macro of the whole instrument deserves mass; position stays |
| shell outerR | 85 | ~100–110 | the shells are the other half of the instrument |
| heart Ø / hit | 5 / r 14 | 8 / r 18 | hold + the new punch live here |
| chevron & skip boxes | 10 | ~13 | transport under fingers |
| reel notch reach | ±4/±6 | ±6/±9 | the speed hand |
| moons arc / crescents arc | r 104 / 130 | ~r 122 / 152 | follows outerR |
| crescent arc / view | Ø 18 / 24 px | Ø 24 / 30 px | in/fb views grow further for the oysters (§4) |
| jack / socket / lamped views | 26 / 20 / 34 | 30 / 24 / 40 | legibility + hit; every grown box re-runs the courtesy law (rake 12) |
| bird span / body | 30 / 1.7 | ~46 / 2.6 | see §11; label type stays 10 px mono |
| roots reach | ~64 | ~110 | see §10 |
| sowing moon Ø / halo | 52 / 7 | ~64 / 9 | see §9 |
| compass | 140 square | pool Ø ~156 | see §9 |
| weather marks / hit | ~10–13 / 10 | ~14–18 / 12–14 | see §9 |
| telemetry marks (comet, fireflies, chord lights) | Ø 4 / 1.2 / 1.8 | Ø 5–6 / 1.6 / 2.2 | information must not shrink relative to grown bodies |

The comet's accepted 2026-07-29 motion pass keeps that mark on one transparent,
mouse-pass-through layer per Coco. `/cq_phase` now shares the 15 Hz ambient
ceiling with the castle fireflies; ordinary address motion redraws only the
dot/tail layer, never the complete shell, and an unchanged frame redraws
nothing. A real direction-regime change still repaints the shell chevron.

Strokes, fonts, palette, alphas: **unchanged** (§16.3, §16.4, §16.10 —
"ink does not thicken when the paper grows"). Pen-call counts are size-
independent, so the flood costs the frame budget nothing; only the opaque
boxes grow, which is rake 12's business.

If the matrix proves the minimum stage genuinely cannot hold the new
measures, raising the documented minimum window is a **user decision** to
surface at the review, not something the implementer decides.

---

## 9. The almanac becomes landscape — the cluster is broken

**Third design, v3.2 accepted after visual/interaction review 2026-07-27
(checklist N3).** The cluster is dissolved one step further: weather becomes
an act of the moon and the compass becomes a property of the bay.

**Full-scale polish build, implemented 2026-07-28 and visually accepted
2026-07-29.**
The accepted interaction and topology stay unchanged while the moon grows
Ø 56 → 70, the seed rows move 22/91 → 18/96 % of stage height, each seed
gains a 28 px-diameter touch body, and the gill gains a 48 px-diameter exact
hit body. These measures do not reopen N3's grammar.

- The **solitary moon** is Ø 56 in the far upper-right, beyond bird B and
  slightly above it. It opens as a 0.22 crescent and sows through
  `0.2 × phase²` (quarter/half/full ≈ 4/18/70 cables). Tap throws and
  horizontal drag pours phase. Down holds rain/grow and up holds sun/wither:
  one 8 px dominant-axis lock chooses the act, then 32 px of travel tensions
  rain 0.6…1.6 Hz and sun 2.5…6 Hz while adding drops/rays to the transient
  glyph.
- The **pool is removed**, not relocated. Four memory seeds at 31/69 % width
  and 22/91 % height imply a large direct bilinear field behind the central
  organism. No shore, rectangle, ambient wave or sparkle is drawn at rest.
  A voyage reveals a cross and four sparse ≤8 % wavefront pulls.
- Filled/Shift/right seed taps mean recall/write-or-overwrite/clear. A latched
  Tab/gill focus also lets a vacant tap write without a keyboard; an ordinary
  unfocused vacant tap is inert. Vacancy is an open inward husk, stored silence
  a closed dark dormant kernel, and a living patch a closed luminous kernel
  whose light follows its cable density.
  Filled-seed → filled-seed drag breeds directly. Its curved chord lives on a
  topmost hit-transparent layer; a valid filled target catches it and opens a
  blue double bloom. This replaces the graft glyph and its armed mode.
- Unfocused open water is inert even under a long left sweep. Tab and the
  clickable gill below the lower castles latch water focus; Shift is its
  momentary form. Focus backgrounds planted cable spans and removes their hit
  paths while leaving bodies/endpoints/plugs alive; left commits and right is
  elastic. Outside focus, right remains harvest. Capture continues across
  bodies once the tide has begun.
- **Polish completed 2026-07-28:** the gill always wins a crossing cable in
  matching hover/press priority. Its exact hit clears cable hover and its mark
  is echoed on the top hit-transparent gesture layer. This is one narrow
  exception to cable-first, not a new general control law. A later reviewed
  amendment (2026-08-02) adds only the four exact SlotQ/SlotX bridge-end hit
  circles; right-click/Alt and the rest of each shell remain cable-first.
- Castle and castle-endpoint paper is transparent. The irregular flesh masks
  the river and coupling chords only inside its real shape, so no rectangular
  blank bites the body and no synthetic ring stub protrudes from it.
- This increment deliberately stops before the roots. Their next version
  must show the real source→preamp→Coco audio vein with the envelope as a
  side-growth; §10's decoration-only mangrove proposal is no longer enough.

<details>
<summary><b>Superseded almanac/pool designs v1–v2</b></summary>

**Rewritten 2026-07-27, after the first build of this section was rejected.**
The version below the line is the history; read it, because the way it was
wrong is the useful part.

The three parts are not one thing, so they do not go in one place. Each goes
where it **acts**, into the landscape, and they end up far apart. Same
functions, same math, same gestures — new homes and new bodies.

- **The moon rises into the sky**, Ø 52 → ~64, up to the birds' own
  altitude between castle 1's box and bird B's name: the thing the birds fly
  under. Behavior (phase = density, tap = throw, horizontal pour, flash from
  every layer) is untouched.
- **The rain and the sun follow it there.** This is the part the first build
  got wrong by leaving them beside the pad. They grow and wither the **live
  bay**, not the pool's stamped memories — they are whole-bay weather in
  exactly the way the moon's sowing is, and an almanac's sky is where weather
  belongs. Sky-side placement also costs nothing: measured, the sky is never
  under a cable.
- **The pad becomes the tide pool, and goes to the water** — the bay's own
  bottom pocket, **tangent to the stage's lower edge**, not a reflection
  hanging under the moon. The roots already run past that edge, so it reads
  as the waterline and nothing has to be drawn to say the plants and the pool
  share one water. Body: a seeded, wobbled closed shore (§16.3's organism
  language, the castle blob's cousin) whose wobble kisses and crosses the
  waterline. **It is an ellipse, ≈ 200 × 118**, because a Qt layer is a
  rectangle and the pocket is bounded by coco 1's crescents, the river's
  layer, envA's jack and the edge; an ellipse uses that pocket, a disc wastes
  half of it. The four **corner seeds sit on the shore at the diagonal
  winds** (NW NE SW SE = bilinear TL TR BL BR). Tide math is untouched:
  `compassNorm` gains an elliptical grid mapping between disc and square
  (shore at a diagonal = exactly a bilinear corner — rake 13; its inverse
  needs a corner snap), and stamp/tap/voyage/elastic/undo, the cross and the
  ≤ 8 % pull gradients (now radiating from the shores) carry over.
- **The graft stays on the pool**, off its lower-right shore. It takes its
  two parents as **seed taps**, so a mark whose gesture ends on the seeds may
  not live a bay away from them.
- On the water a mark is consulted **before** the water it stands on — §4.5's
  most-specific-first order, which the row never needed and a shore does.
- Clearance: the moon keeps its own opaque layer and the pool keeps its own
  (a flash never re-inks the other), and the asserts are rewritten for the new
  forms and the two new neighbourhoods — sky: castle 1's box, bird B's label
  and dropRect, the stage top; water: envA's jack, the river's layer, coco 1's
  crescents, the waterline.

**Two honest costs, both measured, neither hidden:**

1. **The move buys belonging, not size.** An early estimate of ~1.8× the pad's
   area was wrong: it compared free *discs* when a Qt layer is a *rect*. The
   pool ends at 18 535 px² against the G3 pad's 19 600. Size has one real
   lever left and it is the minimum window (§15).
2. **The pool is in the bay, so cables cross it**, and §4.5 gives the cable
   under the pointer the press. Over 60 random bays at the half moon's 35
   cables the two bay-facing seeds are shadowed **17 % and 35 %** of the time
   and the graft 17 %; at 70 cables 32 % / 48 % / 25 %; the two landward seeds
   and the entire sky are **0 %** at every density. This is a heavier trade
   than the skip mark's, because the seeds are the pad's whole interface. It
   is an open user decision, recorded in checklist N3: (a) accept, (b) a local
   §4.5 exception "inside the pool's shore the pool wins" (cost: a cable lying
   across the pool cannot be ripped at that spot), or (c) move the pool back
   to water no cable crosses. Do not decide it in code.

This section supersedes GUI_SPEC §16.6's almanac-corner and pad recipes and
§7.1's "140 px round-cornered square" line; those edits ride the increment.

---

<details>
<summary><b>Superseded 2026-07-22 version — "the right-sky column becomes a
composition"</b> (kept because the failure is instructive)</summary>

> The right-sky column becomes a composition. Same region, same functions,
> same math — different body language: the moon rises to the birds' band, the
> pad becomes a Ø 156 pool hanging below it "like its widening reflection",
> and the three weather marks scatter onto that pool's shore with seeded
> jitter so no two marks share an axis.
>
> **Why it failed.** It was built as written and rejected on sight. Breaking
> the *axis* while keeping the *region* leaves the three parts exactly as
> related as they were — a reserved patch of right-hand sky, touching no body,
> read by the user as "valikkomainen alue sivussa". Two further lessons came
> out of the wreck: the reflection metaphor forced the pool into the same
> corner it needed to leave, and scattering all three weather marks onto the
> pool quietly claimed that rain, sun and graft act on the same thing, which
> is false — only the graft touches the memories.

</details>

</details>

---

## 10. The roots — the intake body, not an envelope on a stick

> **Status — accepted 2026-07-28 on `nocturne-preamp-roots`
> (checklist N7).** The accepted design is direct strand selection/toggle, an
> 8 px axis lock into 240 px vertical drive or horizontal bias,
> drive-weighted 1.5/2/3 px audio anatomy, and transient, topology-true bird
> flights. The live review carried one correction: the transient provenance's
> ground erasure was cutting the tops off root B's two leftmost strands. The
> erasure now measures the seeded delta inside its own band and gives way
> outward — `rootLabelInward` is a nominal rest, not a promise. The clearance
> matrix was written against the old constants first and failed on exactly
> the two strands the user saw, plus a 0.8 px mirror case on root A that no
> eye had caught.

The root metaphor works only if it tells the whole signal story. In the DSP,
each side first chooses `preASrc` / `preBSrc` (`liveL`, `liveR`, `mic`,
`deckA`, `deckB`, `none`), passes through the drive+bias asymmetric tanh
preamp and HPF, and that **audio** feeds its Coco input. `envA` / `envB` is
only the post-preamp `Amplitude` side-growth used by the modulation matrix.
Each deck also has a parallel direct `SlotDeck` path to its own Cocoon, so a
deck selected into a preamp may be heard through both paths.

The root increment therefore does this:

- Five wide, seeded-irregular source strands enter one **preamp knot**.
  Hover previews one CQSpec provenance, click selects it and a second click
  writes `none`; no strand gets a permanent label.
- A drive-weighted **audio vein** leaves that knot and joins the
  corresponding Cocoon. This is anatomy, not a patch cable: the DSP relation
  already exists and must be drawn as such.
- The existing `envA` / `envB` jack becomes a smaller side bud off the
  post-preamp vein. It remains the one real patchable modulation endpoint.
- The knot gets one axis-locked body: vertical drive and horizontal bias;
  its outgoing/intake lobe imbalance reads bias without a number.
- Bird/root hover reveals the direct deck→Coco slot and any currently selected
  bird→preamp provenance. Nothing permanent links a bird to one root, because
  either deck may feed either preamp.
- Do not add decorative root count without information, and do not invent
  extra patchable audio jacks. Making `preA` / `preB` audio patchable would
  expand the canonical 16×22 matrix and is a separate DSP/interface epic.
- The mic sprout remains a truthful three-state provenance cue beside side A.

The second-review implementation contract is now precise:

- The five audible source values are read from `CQSpec`; `none` illuminates no
  strand. A nearest-strand 11 px hand reach selects the exact enum; selecting
  the active strand again writes `none`.
- The first 8 px lock one axis. A 240 px upward pull crosses normalized drive;
  a 240 px rightward pull crosses normalized bias. Values map through CQSpec,
  so neither the 1–40 exponential law nor the 0–0.6 linear law is duplicated.
  Right press is inert, and cable/Alt wins before strand or knot.
- The audio vein meets the real Cocoon spiral at norm 0.8, routes outside the
  crescent owners and is redrawn inside opaque Cocoon paper. Its 1.5/2/3 px
  weight follows drive; `SlotPre` and env aura shape alpha. `envA/B` grows
  outward as one smaller cubic branch under transparent endpoint paper, with
  no straight repair stub or midpoint seam.
- Bird hover reveals its direct route only while `SlotDeck` is nonzero, with
  weight derived from that gain. Bird hover also reveals every preamp selecting
  that deck; root hover makes the reciprocal revelation. All route ink and
  provenance text share one hit-transparent layer above the landscape and below
  patch cables, so Qt view rectangles cannot clip them. They disappear with the
  hand; bone text over a small ground wash stays legible beside a lit root.
- Drive/bias/aura repaint remains local to roots and their shell underlay.
  Transient cross-landscape provenance repaints only its shared route overlay;
  open paper inside the transparent root layer delegates to the tide/ground
  grammar.

---

## 11. The birds fetch — loading without a single arrow

> **Status — accepted 2026-07-28 (checklist N6).** The configured samples
> folder, cancellation, folder wheel, inert straight drag and final
> Windows/Qt-safe ruffle were exercised in the live instrument. The accepted
> implementation is commit `59ade79` on `nocturne-birds-fetch`.

The decks need in-GUI loading and folder navigation, and the night forbids
buttons, arrows and question marks. The bird itself is the control, in the
app's own gesture grammar — three acts, told apart by the hand, no new
glyphs at all:

- **Tap the bird (< 8 px travel — the lasso's own threshold) = go fetch.**
  Opens the system file dialog (async `Dialog.openPanel` →
  `CQ.deckX.load`; rake 14) at the configured samples folder. The bird is the
  obvious "this is the deck" body, it already brightens for a file drag, and a
  cancelled dialog is a harmless no-op.
- **Wheel over the bird = the next perch.** Wheel down = next file, wheel
  up = previous (`CQ.deckX.next/prev` — the folder must be loaded; the
  facade's own guards answer otherwise). The wheel already means "step
  through an ordered neighborhood" everywhere in the app (weight nudges);
  here the neighborhood is the folder. Rake 15 (momentum) applies.
- **Ruffle the bird = bring me anything.** A drag that starts on the bird,
  travels horizontally ≥ 6 px and returns ≥ 6 px in the opposite direction
  (one reversal) is a *ruffle* — on release the
  startled bird returns with a random file (`CQ.deckX.random`). The first
  implementation required two reversals; live review showed that a natural
  out-and-back contains one, so that stricter recognizer was corrected. A
  second live review found that Windows/Qt could lose move samples when the
  cursor left the bird's small UserView: the bird now claims the event and a
  60 Hz global-cursor fallback follows the held hand. The old 300 px path cap
  was also removed, so repeated or broad ruffling cannot invalidate an
  already-clear reversal.
  Travel-based, no timer, and surreal in exactly the way this instrument means
  it: you shake the tree, the bird brings what it brings. A drag that is neither
  a tap nor a ruffle does nothing.
- **Feedback is the existing truth**: the filename label updates from the
  deck's own `\file` notification on every path (dialog, wheel, ruffle,
  drop, code), and the bird gets the §16.7 120 ms momentary flash on any
  successful load — one light for five entry paths, the moon's own
  pattern. The wing flutter law and drop-target border are untouched.
- Hover: the bird brightens (static, §4.5) — the same cue it already wears
  for a file drag, now also promising the tap.
- Priority chain on the bird view: cable first (a cable can cross a bird),
  then these gestures; the rate socket and level crescent keep their own
  views and win their own geometry.

Spec edits: GUI_SPEC §6 (bird paragraph), §6.1 (three rows), §16.6 (birds),
checklist block (load by tap, walk a folder by wheel, ruffle for random —
by ear and by label).

---

## 12. Build order and review stops (supersedes §1.7's original order)

Each numbered step is one increment: full battery, its own checklist block,
its own visual/listening review stop, its own commit.

1. **The punch** (§2) — no geometry, immediately musical, independent of
   everything below.
2. **The spring flood** (§8) — sizes only, no new forms; collision matrix
   first, one composition review at two window sizes.
3. **The almanac becomes landscape** (§9) — the moon becomes one remote
   three-axis weather body; the pool disappears into a four-seed tide field
   behind the organism; Shift gives that water temporary priority over cable
   spans; breed becomes a direct seed relation. (Kept separate from the flood
   so the rejected v1/v2 compositions never held §8 hostage.) The **size
   re-check of the whole bay rides this step's review**, because sizes are
   relational (§8).
4. **Range ripples** (§3) and 5. **dolby oysters** (§4) — into the grown
   landscape, so their clearances are done once.
6. **The birds fetch** (§11).
7. **The roots** (§10) — a functional preamp/intake-body pass after the tide
   composition is accepted; it no longer rides as decoration with the birds.
8. **The drawer workshop page** (§12.1) — its own gate; completeness audit
   against SPEC §6.2 (every CQSpec parameter reachable) is its exit
   criterion.
9. **The day verdict** (§0) — only after 1–8 have been *played* for real
   sessions. Deletion, if chosen, is its own reversible gate.

After steps 1–7 the night map is, deliberately, a complete **performance
surface**:
every hardware-panel gesture (speed, in, fb, spaf, hold+punch, flip, skip,
dolby, ranges, master freq, chaos, spesal, patching) plus the machine's own
gifts (the moon, the tide field, the weather, the birds) — playable from one
landscape with the master strip as its spine. It is not yet the complete
code-free GUI promised by SPEC §6.2: step 8 supplies the remaining tuning and
configuration controls in the Drawer. Only after step 8 does night + drawer +
master form the complete GUI instrument whose Day verdict can be trusted.

The **ear pass** (§13) is independent of this GUI order and can interleave
with it — each of its items is its own listening iteration. Its natural
start is right after the G3.1 review, whose script already carries the
zipper measurement (§13.6).

---

## 12.1 The Drawer workshop — N8 implementation contract

### 12.1.1 Coverage-audit verdict

The old N8 sketch named examples but did not define “remaining”, a count, a
machine-checkable partition, the fourth-page shell work, or the one-GUI-path
rule for `micGain`. It was not sufficient handoff for implementation.

`CQSpec` currently contains **74** parameters. “Reachable” in this gate means
all of the following:

- the player starts in Night and uses only Night, the Drawer and the
  persistent Master strip — Day, code and 16n do not count;
- a persistent parameter has a visible, labelled, enabled control at the
  1280×720 minimum, and the control both writes through `CQState.set` and
  follows writes from code, MIDI, snapshots and the other GUI surface;
- a read-only mark does not count (the night's mic sprout is an indicator),
  and a matrix destination does not count as its base parameter (the bird's
  `deckA_rate` socket is not a `deckARate` control);
- numeric curves, ranges, integer steps and enum membership come from
  `CQSpec`; the page owns no copied ControlSpec, default or enum array;
- “reachable” includes an exact current-value reading. A rail with no value
  display is not enough for this ledger page.

The intended partition is exact and disjoint:

| Surface | Count | CQSpec parameters |
|---|---:|---|
| Night | 44 | `deckALevel`, `deckBLevel`; `preASrc`, `preBSrc`, `preADrive`, `preBDrive`, `preABias`, `preBBias`; for each Coco: `In`, `Fb`, `Speed`, `Spaf`, `DolbyIn`, `DolbyFb`, `Hold`, `Flip`, `Level`, `Organ`, `SlotQ`, `SlotX`; `qFreq`, `qChaos`; `c1Range`…`c5Range`; `c1Send`…`c5Send` |
| Master | 4 | `masterVol`, `masterDry`, `patchDepth`, `monitorMute` (the PANIC/resume control) |
| Workshop | 26 | the explicit inventory below |
| **Total** | **74** | must equal `CQSpec.names`, with no missing or duplicate owner |

This partition describes the night-first GUI, not every observer. For
example, Night may keep reading `micGain` to draw its sprout without becoming
an interactive owner.

The **26 Workshop parameters** are:

| Ledger island | Count | Exact names |
|---|---:|---|
| Inputs and decks | 6 | `liveGain`, `micGain`, `deckARate`, `deckBRate`, `deckALoop`, `deckBLoop` |
| Coco 1 remainder | 7 | `coco1Bits`, `coco1Wrap`, `coco1Interp`, `coco1Dust`, `coco1Pan`, `coco1SlotPre`, `coco1SlotDeck` |
| Quantussy remainder | 6 | `qShLag`, `c1Off`…`c5Off` |
| Coco 2 remainder | 7 | `coco2Bits`, `coco2Wrap`, `coco2Interp`, `coco2Dust`, `coco2Pan`, `coco2SlotPre`, `coco2SlotDeck` |

The other SPEC §6.2 obligations are already owned, but the N8 exit audit must
name them too: Night owns hold/flip/skip, spesal step, dolby, ranges, patching
and deck loading/navigation; Inspector owns patch presets and the full
16×22 signed matrix; Memory owns snapshots and A→B morph; Night/Master show
the required hold, spesal/activity, filename, meters, REC/elapsed and notice
state. A CQSpec-only test is therefore necessary but not sufficient.

### 12.1.2 Preconditions and non-goals

Do not begin N8 until N7 has the user's checklist acceptance. Build N8 on the
accepted tip of the current stacked Nocturne chain, not on the older
`nocturne` integration tip. Record a clean baseline from `compile_check`,
`run_all`, `nrt_safety` and all four GUI smokes before changing classes.

N8 changes presentation and GUI bindings only. It does **not**:

- change a SynthDef, DSP route, CQSpec name/range/default/curve/lag, matrix
  inventory, snapshot policy, keyboard map or safety threshold;
- remove Day, add a runtime feature flag, or decide the fate of Day's spesal
  constellation or memory-path diagrams (N9 owns that verdict);
- add parameter undo or parameter lines to Spellbook — those remain matrix
  facilities;
- duplicate performance controls already honest in Night or Master merely to
  make the Workshop look comprehensive.

There is one deliberate Day-side relocation. SPEC §6.2 says the GUI never
grows a second `micGain` path, while the night-first completeness gate
requires `micGain` without Day. Therefore N8 must **move**, not copy, the
interactive mic control and its warning owner from Day/Sources to Workshop.
Day may retain a passive pointer such as “mic · drawer/workshop” if the
composition needs it, but no second enabled mic control. The night's
read-only mic sprout remains valid.

### 12.1.3 Stable page, visual concept and layout contract

The global toolbar destination remains named `drawer`. Inside it the tab
order becomes:

`inspector · workshop · spellbook · memory`

`workshop` is a fourth persistent page, not a replacement for Inspector and
not a mode inside Memory. The existing close/reopen persistence law applies
to it. Selecting Day or Night while the Drawer is open still returns to that
instrument, and Master remains visible and live.

#### Artistic relation to Night

The Workshop is **the same instrument in the opposite grammar**:

- Night is the living bay: open water, asymmetric bodies, topology, gesture,
  almost no text and no numbers.
- Workshop is the bay's quiet measurement table: recessed slate, straight
  rules, exact names and values, stable coordinates and no uninvited motion.

This contrast is the point. Do not transplant Night's shells, roots, birds,
wobble or weather into the Drawer, and do not shrink the Day panel into it.
The relation comes from the shared Ink kit, palette, type, line weights,
semantic family colors and hover language — not from decorative biology. In
Night skin it should read as a phosphor-lit field ledger beside the bay, not
as a generic settings dashboard. In Day skin the identical geometry becomes
ink on recessed paper. The hard cut changes the light, never the information
architecture.

Night's lights remain informational here too. The Workshop has no aura,
glowing panel borders, cable animation, starfield, texture, gradients used as
decoration, or organism wobble. Only a changing parameter, hover/press,
`micGain` danger, or another real state may brighten a mark. The page is
otherwise perfectly still.

#### Fixed composition

At the 1280×720 minimum, center one **1128 px content rail**, exactly matching
the accepted Inspector loom rail. Keep that rail fixed at larger sizes:
larger windows buy surrounding slate, not stretched controls. The whole page
uses one continuous `ground recess`; it is not a board of rounded cards.
Inside the rail, use hairline separators and 16 px gutters on the established
8 px grid. The only shadow remains the Drawer stage's existing soft edge —
no island casts one.

The page has one shallow service band over a three-ledger body:

```text
┌──────────── deck a ────────────┬────────── input trims ──────────┬──────────── deck b ────────────┐
│ rate · loop                    │ live gain · mic gain             │ rate · loop                    │
└────────────────────────────────┴──────────────────────────────────┴────────────────────────────────┘

┌──────────────── coco 1 ────────────────┬──────────────── quantussy ───────────────┬──────────────── coco 2 ────────────────┐
│ converter · material · output · slots  │ s&h slew · detunes · castle sends       │ converter · material · output · slots  │
└────────────────────────────────────────┴───────────────────────────────────────────┴────────────────────────────────────────┘
```

This is a restrained echo of Night's real geography: deck A and B hold the
two outer ends, while the main body reads `coco 1 · quantussy · coco 2`.
It is topology as ordering, not a picture. The retired Space module has no
hidden or disabled placeholder.

Use three equal service modules with two gutters across the shared responsive
rail. Below the band, split the same rail into the three main columns with two
gutters. Pair symmetry is structural:
both Coco ledgers are built by the same helper with only the index changed.
The service band is 72 px high. After PLAY-6, each Coco ledger has seven rows
and Quantussy six; the page still derives height from its rows instead of a
fixed content constant. With 12 px top/bottom page margins, the derived rail
fits the minimum Drawer workspace without a scroll view.

Group rows by signal meaning, never alphabetically:

- each Coco: **converter** (`bits`, `wrap`, `interp`), **material** (`dust`),
  **output** (`pan`), **input mix** (`pre`, `deck`);
- Quantussy: **s&h** (`qShLag`), **detune** (`c1Off`…`c5Off`);
- the service band uses the exact pairs shown in the wireframe.

Island titles and ordinary row labels are lowercase 10 px sans; subgroup
labels are 9 px whisper; exact values are 10 px mono. Labels sit close to
their rail and are never boxed or rendered as pills. The main three titles
share one baseline, both Coco subgroup baselines match exactly, and spare
vertical room collects below the ledgers as slate rather than stretching
rows apart.

#### The ledger control

The final Workshop must not be forty-three miniature Day knobs. Add one clean
Ink **ledger-row** control through `CQInkControl` + `CQGuiWidgets`; the
existing slider does not carry this exact-value contract:

- one row is 24 px high and owns one rectangular hit area;
- label at left, a hairline value rail in the middle, exact mono value at
  right; the value never overlaps the rail;
- unipolar numeric fill grows from the left; bipolar fill grows away from a
  visible centre tick; integer rails snap and print an integer;
- a toggle uses two stable `off · on` seats; an enum uses one labelled seat
  per value, in `CQSpec.enumValues` order;
- press/drag on the rail maps its own x-coordinate to normalized 0…1 and then
  calls `CQSpec.map`; wheel uses the existing normalized fine nudge. No
  literal range, step, default or enum lives in the control;
- hover brightens only that row's rail, handle and value; press adds the
  existing stronger outline. Cue and action use the same row/rail geometry;
- external state and snapshot morphs move the handle/value through the
  existing paced updater stream. That real change is the only motion.

Use semantic phosphor sparingly. In Night skin, slate is `recess`, rules and
ordinary values are bone/whisper, and established families accent only their
own facts (`organ`, `dust`, input, Quantussy family, danger). Do not rainbow
whole columns or invent a family for neutral slots, levels or pan. Day uses
the same marks re-inked through the Day palette. No color is
inlined at the Workshop draw site.

`bits` uses the integer ledger; `wrap` and deck loops use the toggle ledger;
`interp` uses the enum ledger. All other Workshop parameters are numeric
ledgers, including slots and sends: an unlabeled slider with no exact value
is insufficient for this precision page. Labels may be shortened only inside
their already-named group (`pre`, `deck`, `q`, `x` under **input mix**);
the full parameter ownership remains in the manifest.

The `micGain` control uses the warning family, gains the existing danger
outline when live, and drives the existing persistent `MIC LIVE` notice. Its
row may brighten red, but its surrounding service module does not glow.
That warning subscription must exist while the GUI exists, even when the
Workshop page is hidden; closing the Drawer may not silence the warning.

Do not make the page scroll merely to avoid layout work. At the minimum size,
the service band, all three main ledgers, the last Coco slot row and the fifth
castle-send row must lie inside the Workshop page's actual bounds. A
1920×1005 content-window review (the
existing 1920×855 workspace reference) must add breathing room rather than
stretching controls into a different hierarchy.

Reject the first runnable visual if it reads as any of these: four rounded
settings cards; a dense native control panel; a miniature Day; Night scenery
used as decoration; independently glowing sections; evenly distributed
controls filling every available pixel. At the review stop, the squint read
must be **service band above · Coco / Quantussy / Coco below · one red mic
exception**, while the close read gives every exact value.

### 12.1.4 Code-shape handoff

Keep the new composition out of the already large `CQGui.sc`. The expected
shape is:

- add `classes/CQGuiWorkshop.sc` (under the project's ~400-line limit);
  its one explicit `parameterGroups`/manifest drives both layout construction
  and `paramNames`, so the audit never tests a second hand-copied list;
- make that manifest list the 26 names above explicitly. **Do not** compute
  Workshop ownership as `CQSpec.names - night - master`: that would silently
  make a newly added parameter “covered” without drawing a control;
- expose semantic interactive inventories for Night and Master (for example
  `CQGuiNightMap.reachableParamNames` and
  `CQGuiPanels.masterParamNames`). They may compose existing semantic helpers,
  but they must not derive from the Workshop complement;
- extend `CQGuiDrawer` to own, skin and free the Workshop and to publish
  `#[inspector, workshop, spellbook, memory]`;
- extend `CQGuiShellState`, `CQGuiShell`, and `CQGui.prBuild` through the same
  fourth-page order. Update every whitelist, page array and selected-index
  lookup together; the queued `prSwapStage` law is unchanged;
- move the Day `micGain` control/warning registration from
  `CQGuiPanels.sources` into `CQGuiWorkshop`; keep the existing central
  `CQGui.micNotice` composition and the Night sprout observer;
- reuse `CQGuiWidgets` for all parameter writes and updater registration;
  implement §12.1.3's ledger row in `CQInkControl` and bind its
  num/int/toggle/enum variants through `CQGuiWidgets`. Do not make a second
  state-binding factory inside Workshop, and do not ship standard Day knobs
  as the Workshop's final visual form.

Use this green implementation sequence:

1. Synchronize the proposed GUI contract in `GUI_SPEC.md`/`ARCHITECTURE.md`,
   add the three explicit owner inventories and their dynamic coverage unit
   test. No rendered layout is claimed yet.
2. Add `CQGuiWorkshop` and build every control directly from its manifest
   (39 at the original N8 gate; 26 after PLAY-6 moved 13 gestures to Night).
   Test it serverlessly before putting it in the live shell. Because it is
   not constructed by `CQGui` yet, its mic control is not a second live GUI
   path.
3. In one atomic, green change, wire the fourth page through Drawer/Shell and
   remove the Day mic control/warning owner. Never commit or hand off an
   intermediate live GUI with two `micGain` controls.
4. Extend the Qt smokes, run the full gate, add the N8 manual block, then
   present the first runnable Workshop at the review stop. Polish only from
   the user's feedback; do not drift into N9.

The implementation increment must update `GUI_SPEC.md` (§2 and §8 tab/page
contract, §13 gate, and the mic relocation), `ARCHITECTURE.md` (new class and
Drawer composition), `tests/manual_checklist.md` (N8 block), `PLAN.md` status
and, after acceptance, `CLAUDE.md`/`CHANGELOG.md`. SPEC §6.2 already states
the required behavior and needs no semantic weakening.

### 12.1.5 Test-first contract

Add the following contracts before calling the page complete:

1. **Dynamic coverage unit test** (`CQGuiVisualContractTest` or a narrowly
   named new unit class):
   - Workshop manifest has 26 unique real `CQSpec` names;
   - Night has 44, Master 4, and the three interactive-owner sets are
     disjoint;
   - their union equals `CQSpec.names` exactly and has the same size.
   Keep the current 74 as a diagnostic assertion if useful, but the equality
   to `CQSpec.names` is the future-proof gate.
2. **Ownership tests**:
   - all five Drawer tabs appear in the normative order;
   - `\workshop` is accepted, persists across close/reopen and rejects no
     global key behavior;
   - `micGain` occurs in Workshop's interactive manifest and not Day's,
     while Night's mic mark remains explicitly read-only.
3. **Binding tests**:
   - every manifest entry constructs exactly one enabled control;
   - representative num, bipolar num, int, toggle and enum controls write the
     expected clamped value through `CQState`;
   - an external `state.set` repaints representative controls without firing
     a write echo; enum order and numeric mapping come from `CQSpec`;
   - all 26 final controls are ledger rows, not native widgets or retired Day knobs,
     and bipolar/int/toggle/enum variants expose their required secondary
     shape channel.
4. **Drawer Qt smoke**:
   - open Night → Drawer → Workshop at 1280×720 and assert the real page is
     visible; the shared rail is centred; the three service modules and three
     main ledgers have their specified widths; the deepest retained setup rows are
     contained; Master is visible; and `CQInkControl.drawErrors == 0`;
   - repeat in Night and in a 1920×1005 content window
     (the 1920×855 workspace reference);
   - set `micGain` above zero and back to zero: the one Workshop control,
     danger treatment and persistent notice agree even after switching pages;
   - close/free and preview re-entry leave no duplicate controller, task or
     resurrected window.
5. **Regression gates after the class changes**:
   `CQ_COMPILE_OK`, `CQ_TESTS_PASSED`, `CQ_SAFETY_PASSED`,
   `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
   `CQ_GUI_DRAWER_REENTRY_PASSED`, and `CQ_GUI_NIGHT_PASSED`. Run them through
   `tests/run_sclang_gate.ps1`. N8 has no DSP change, so Hold NRT is not a new
   obligation; an unexpected audio/SynthDef edit expands the gate and is a
   scope alarm.

Do not satisfy the coverage test with a test-only array, hidden controls,
disabled controls, clipped rows or a complement computed from `CQSpec`. The
rendered controls and the audited manifest must be the same data.

### 12.1.6 Review stop and exit criteria

Add an N8 block to the live checklist with two passes:

- **visual/behavior pass:** Night → Drawer → Workshop at 1280×720 and the
  user's maximized laptop size. First squint: one quiet service band over the
  `coco 1 · quantussy · coco 2` triptych, same phosphor species as Night but
  no copied organisms, card dashboard or miniature Day. Then check Coco
  symmetry, exact values, semantic color restraint, hover, page persistence,
  no scroll/clipping, fixed-rail breathing room, Master/PANIC visibility,
  external state following, and the single mic warning path;
- **short listening pass:** with conservative master level, exercise one
  representative from every Workshop subgroup (deck rate/loop, Coco
  converter/material/output/slot, castle detune/send and qShLag), reset to a
  known snapshot, then play a real session without
  opening Day. This is a control-provenance check, not permission to retune
  DSP.

Stop for the user's visual and playing review before polish or N9. N8 exits
only when:

- the dynamic 44 + 4 + 26 partition proves all current CQSpec names reachable
  from Night + Drawer + Master and all other SPEC §6.2 obligations have a
  named owner;
- all 26 Workshop controls are visible, enabled, exact-value-following and
  funnel-bound at the minimum size;
- the accepted visual reads as Night's still measurement table — shared Ink
  species, deliberately different grammar — and passes §12.1.3's explicit
  cheapness/rejection list at both review sizes;
- there is exactly one interactive `micGain` GUI path, with warning and
  persistent notice intact;
- the five-page Drawer lifecycle, focus, queued swap, skin and cleanup laws
  hold;
- the full automated gate is green and the N8 checklist is accepted by the
  user.

Only then may N9 ask whether Day lives or dies.

---

## 13. The ear pass — musical tuning experiments

Seven ear-first experiments toward the target sound: Cocoquantus- and
ciat-lonbarde-like experimental sound design and looping. None are bug
fixes; all are "one notch more instrument-with-a-will" levers. **The
listening-iteration law governs every one of them** (CLAUDE.md): one
hypothesis → one change → full battery → a 3–5 step listening script → the
user's description → adjust or revert. **One variable at a time — never
two of these in one session.** Every accepted change updates SPEC.md's §5
tables in the same commit (SPEC and CQSpec must never drift), and every
DSP-touching item (13.3, 13.5, and 13.2's clamp option) re-runs
`nrt_safety` **and** `nrt_hold_integrity`, not just the compile/unit pair.

Values below are **starting points for the ear, not conclusions.**

Suggested order by payoff over risk: **13.1 → 13.4 → 13.2 → 13.5 → 13.3**,
with 13.6 running inside the G3.1 review and 13.7 already planned as §2.

### 13.1 The feedback taper — more fader where the music lives

**Implementation status (2026-07-28).** The first listening candidate is
implemented on `nocturne-feedback-taper` as the generic CQSpec-owned
breakpoint map described below. The automatic gate is green. The user
accepted the taper and its full visual follow-up on 2026-07-28: the audible
regimes land at their expected positions, and the feedback crescent draws the
same three CQSpec-owned regions discretely — echo green, unity/bloom amber,
mayhem red — with a current-position pearl. A continuous hue ramp was rejected
because it would claim a uniform continuum across a deliberately piecewise
musical map.

**Adjacent-control scale audit (implemented 2026-07-28).** Green→red was not
spread to every continuous hand: colour must name a consequence, not merely
magnitude. SP.AF now has a CQSpec-derived zero notch and two signed halves
(inverso blue / verso orange), with its live fill growing away from zero.
Input has a CQSpec-owned unity boundary: green normal gain, amber overdrive.
The bird level remains neutral through its exact unity tick; the actually
travelled gain above it and its pearl turn danger-red. This is one real
boundary (nominal level → boost), not a generic magnitude ramp. Speed, the
storm iris and the root gestures have no honest discrete region boundaries
today and intentionally remain continuous. dolby and range already state
their discrete modes by shape.

**Hypothesis.** The instrument's juiciest zone is fb ≈ 0.95–1.15
(echo → looper → blooming crunch), but the linear 0–1.25 spec gives it
~15 % of the fader. Densifying resolution around unity is likely the
single biggest playability win for feedback riding (M2's core gesture).

**Proposal.** A piecewise-linear warp for `coco1Fb`/`coco2Fb` — honest,
exactly invertible, no fake smoothness:

| normalized fader | value |
|---|---|
| 0.0 → 0.5 | 0.00 → 0.85 (the echo country, coarse) |
| 0.5 → 0.9 | 0.85 → 1.10 (the juice — 40 % of the fader for 0.25 of value) |
| 0.9 → 1.0 | 1.10 → 1.25 (the mayhem tail) |

On a 7-bit 16n fader that gives ~51 steps across 0.85–1.10 ≈ 0.005 per
step — fine enough to ride a bloom.

**Technique.** CQSpec owns its own map/unmap, so add a warp *kind* there
(e.g. `\fbTaper`, or a generic breakpoint warp whose data lives in CQSpec
only — never at a call site). Range, default (0.35) and every stored
snapshot value are unchanged: only the hand's mapping moves. Unit-test the
map/unmap roundtrip to ≤ 1e-9 across the breakpoints (the morph
interpolates in normalized space, so an inexact inverse would make morphs
drift). Night crescents, Workshop ledger rails and 16n pickup all follow
automatically — they draw or compare through `unmap`.

**Consequences to check.** NRT A5 (unity looper) sets fb by *value* — no
change. After a snapshot recall the 16n fader position no longer matches
the old muscle memory once; mention `takeover: \pickup` in the checklist
block for players who mind.

**Listening protocol (with the user):**

```supercollider
// 13.1 — feedback-taperin korvakoe
CQ.boot; CQ.patchPreset(\empty);
CQ.deckA.load("<perkussiivinen kenttä-äänite>");
CQ.set(\coco1SlotDeck, 1); CQ.set(\coco1In, 0.8);
// 1. Aja F4 hitaasti 0 -> ylös ja takaisin. Ennen: kaiku->luuppi->rouhe
//    ryöpsähti faderin viimeisessä kuudenneksessa. Nyt: rajan 0.85->1.10
//    pitää levittäytyä faderin leveälle keskiyläalueelle.
// 2. Etsi "ikuinen looppi" (~1.0) sormella: löytyykö se nyt ilman
//    mikrometriliikettä, ja pysyykö käsi siinä?
// 3. Ratsasta 0.95<->1.15 edestakaisin: kuuluuko bloom-kynnys aina samassa
//    kohdassa fader-matkaa (ennustettavuus = soitettavuus)?
```

### 13.2 The speed floor — darker honey

**Implementation status (2026-07-29).** Option A is implemented on
`nocturne-feedback-taper` and the full automated gate is green, including
both NRT renders (the DSP clamp moved, so `nrt_safety` and
`nrt_hold_integrity` are mandatory, not optional). `\mayhem`'s ±4 oct spaf
now really drives the clock onto the 750 Hz floor and the DC assert holds
with room to spare (8e-05 / 2.8e-04 against the 0.02 ceiling), so LeakDC
does what the proposal hoped and option B is not forced by measurement.
The clamp floor is no longer a literal inside `\cqCoco`: it lives in
`CQSpec.speedClampFloor`, because it is a range fact of the Speed
parameter, and a unit test pins it at exactly one octave under the knob
floor so the ground-jack trick cannot silently drift. **Not accepted —
this waits on the ear** (checklist 13.2). The honest risk the listening
block asks about first is not the new bottom but the cost everywhere else:
the fader now spans five octaves instead of four, so every centimetre of
travel covers more. If the middle got too twitchy, that is option B.

**Hypothesis.** Slower clocks = darker, more alias-shimmering, longer
loops — very CL. The knob floor (3 000 Hz) is shy; the post-mod clamp
already trusts 1 500.

**Proposal, two options (A first, B the fallback):**

- **A:** knob floor 3 000 → **1 500 Hz** (exp curve and 48 k ceiling
  unchanged), and the post-modulation clamp floor 1 500 → **750 Hz** — so
  the documented "ground jack trick" (modulation pushing an octave below
  the knob floor, SPEC §5.3) survives the widening.
- **B (if A's NRT or ear fails):** knob floor 2 000, clamp stays 1 500.

At 1 500 Hz with the default 2^17 buffer the loop is ~87 s; `bufFrames ≤
2^19` (Phasor precision law) is untouched.

**Technique.** Two numbers in CQSpec (`coco1Speed`/`coco2Speed` minval)
and the clamp constant in `\cqCoco` (SPEC §5.3's row changes with it, same
commit). Old snapshots hold values inside the new range — nothing to
migrate. The organ follows the clock by design, so it now reaches an
octave lower: that is a feature, listen to it.

**Consequences to check.** This touches the DSP clamp: full NRT battery
(A3's stress drives spaf ±4 into the new floor — watch the DC assert;
LeakDC should hold it, and if it does not, take option B). A4
hold-integrity re-run.

**Listening protocol:**

```supercollider
// 13.2 — speed-lattian korvakoe
CQ.boot; CQ.patchPreset(\classic);
CQ.deckA.load("<kirkas kenttä-äänite>");
CQ.set(\coco1SlotDeck, 1); CQ.set(\coco1In, 0.8); CQ.hold(1, 1);
// 1. Kelaa F2 pohjaan asti. Uusi pohja: tumma, rakeinen, ~87 s luuppi.
//    Kysymys korvalle: onko 1500-3000 Hz -kaista SOITETTAVAA pimeyttä vai
//    mutaa? Missä kohtaa siitä tulee mutaa?
// 2. Nosta organ (Nightin keltainen malto tai coco1Organ 0.5) ja kelaa pohjilla:
//    urkupilli laskeutuu oktaavin entistä alemmas - onko se ääni?
// 3. Istuta tri1 -> coco1_spaf painolla 0.8, spaf-syvyys ylös: modulaatio
//    saa nyt painaa kellon 750 Hz:iin asti. Kuuntele ground jack -tempun
//    syvin pää: kimmellys vai katastrofi?
```

### 13.3 The dust character — chip crackle, not tape hiss

**Hypothesis.** The hardware's "digital dust" is grainy chip debris;
plain noise reads as tape hiss. Sparse bipolar grain, whose density
follows the clock, would make the selfNoise workflow organic — and the
link is physical: fewer conversions per second, fewer error events.

**Implemented candidate (2026-07-29).** In `\cqCoco`, the dust source is a
blend injected at the same pre-converter point, so safety invariant 2
(clip before quantize) is structurally untouched:

```
dustSig = (LPF.ar(WhiteNoise.ar, 9000) * 0.35
    + Dust2.ar(SampleRate.ir * 0.02) * 1.2) * dustLevel
```

- The original draft's `clockHz * 0.02` was rejected after measuring the
  actual clocked write. At 1 500 Hz, BufWr revisits a RAM cell for about 32
  server samples and overwrites almost every one-sample impulse before the
  address advances; clocking the raw impulse source would therefore make
  surviving errors follow approximately clock squared.
- A constant two-percent-per-server-sample error stream lets the RAM clock
  do the physically meaningful thinning. Stored Dust2 errors are then
  approximately `clockHz * 0.02`: ~30 grains/s at 1 500 Hz and ~960/s at
  48 kHz. Dust still *audibly follows the speed knob*, now linearly.
- Calibrate the two mix constants by ear so the default `dust = 0.02`
  keeps roughly the old loudness — old snapshots must not get louder.
- No new parameters (the norns diet): the blend constants are fixed
  interpretation, commented with a reference in the SynthDef.

The automated character render (`tests/nrt_dust_character.scd`) measures
the real SynthDef at 1 500, 24 000 and 48 000 Hz. The candidate is 2.1–6.1
dB below the legacy source in RMS, but its crest factor is ~9.2–9.5 and
kurtosis ~15.5 instead of hiss-like values near 3. Stored change activity
rises about 22x from the slow to fast end while RMS stays within 0.3 dB.
At 48 kHz its spectral centroid is 9.43 kHz (legacy 11.97 kHz), with 40.3%
of energy above 10 kHz (legacy 57.7%). This deliberately spends less energy
on a masking, bright bed and more on audible events; the manual ear gate
below still owns final acceptance.

**Accepted 2026-07-29.** In the exact A/B the new source was consistently
preferred: a quieter, breathing rain bed with more internal levels, where the
legacy source broke into louder, flatter bursts and abrupt empty cuts. Live
Speed sweeps kept one recognizable material from dark 1.5 kHz crackle through
gravel and paper-dry chip dust to a pleasant 48 kHz hiss, without a piercing
tone. `dust=0.02` stayed deliberately subtle in program material, `0.2` was
already an obvious effect, maximum dust produced no hard spikes, and the source
worked in the user's own patches. No global level correction: raising the
source would spend the gained headroom on masking and accelerate selfNoise
saturation.

The slow-write follow-up also corrected the listening protocol's premise.
Recording at 48 kHz then playing at 1.5 kHz and recording directly at 1.5 kHz
should be statistically similar: in either case about two percent of stored
RAM cells carry a Dust2 error. Speed changes temporal density on playback;
write speed does not change that per-cell distribution.

**Deferred, non-blocking:** the 5/8/12-bit comparison was masked by program
material and insufficient recirculation time. Converter depth gets its own
isolated capture test with identical quiet material and at least three complete
write passes. It does not reopen the accepted dust source.

**selfNoise follow-up.** The source passed in ordinary patches, but the old
preset's regular triangle feedback accumulated into a nearly continuous
ocean-swell mass. Lowering only feedback or dust softened/slowed that attractor
without creating real clearings. The revised preset therefore rests below
unity, lets held castle values create irregular above-unity plateaus, lowers
the cross-feed valves far enough for castle steps to close them fully, and
forces its four modulation castles back to infra range. A real 120 s
Quantussy+Coco render changed from +9.6 dB monotonic growth to +3.4 dB,
made an 8.2 dB clearing followed by a 7.2 dB rebloom, reduced 10 s envelope
correlation from 0.87 to 0.49, and spent effectively no samples near the
converter rail. `tests/nrt_selfnoise_character.scd` preserves those facts;
the user accepted the revised preset on 2026-07-29 as musically better than
the old ocean-swell version. The final ear gate is closed.

**Consequences to check.** DSP change: full battery incl. A3 (its stress
case runs dust at max — impulses hit the quantizer hard and feedback can
reach the full-scale converter rails, bounded by clip2; the peak assert is
the honest judge) and A4. M4's checklist figure (selfNoise) is the
acceptance ear.

**Listening protocol:**

```supercollider
// 13.3 — dustin karaktäärin korvakoe
CQ.boot; CQ.patchPreset(\selfNoise);
// 1. Ei sisääntuloa: anna pölyn kertyä luuppiin. Ennen: nauhakohinaa.
//    Nyt: rakeista sälää, jossa yksittäiset "virhetapahtumat" erottuvat.
// 2. Kelaa speediä: rakeiden tiheyden pitää seurata kelloa - hidas =
//    harva rapina, nopea = tiheä kihinä.
// 3. Palauta oletus (dust 0.02) tavallisessa patchissa: pohjakohinan
//    pitää olla yhtä hiljainen kuin ennen - vain luonne muuttui.
CQ.patchPreset(\empty);
```

### 13.4 The chaos curve — widen the drunken country

**Hypothesis.** The stepped/sliding "drunken wander" is the Quantussy's
heart, and it deserves the widest band of the fader; hyperchaos can keep
the top quarter.

**Proposal.** Change only `qChaos`'s CQSpec curve (lin → an exp-like
curve factor **+2.0**, selected because it gives the intended review
landmarks). Value semantics — the
internal 0–5 octave FM depth — are untouched, so snapshots, patterns and
the DSP stay bit-identical; only the hand's mapping stretches. With
+2.0: fader noon ↦ qChaos ≈ 0.269 (deep wander), 3/4 ↦ ≈ 0.545, and the
hyperchaos region 0.6–1.0 compresses into the last quarter.

**Technique.** One curve field in CQSpec + SPEC §5.4 row note. The eye's
horizontal axis at Night, the Workshop ledger and 16n all read through `unmap` —
they follow. M3's sweep is the acceptance ear.

**Accepted 2026-07-28.** SuperCollider's curve **+2.0** produced the intended
hand landmarks (noon ≈ 0.269, 3/4 ≈ 0.545) more accurately than the draft's
+2.5 estimate. The first listening pass accepted the stable → wander →
hyperchaos dramaturgy. The visual pass then added four planted broken weather
rings at exact hand quarters, a living qChaos front and the missing
cable-priority eye hover. The user accepted both the curve and this poetic
position feedback.

**Listening protocol:**

```supercollider
// 13.4 — chaos-käyrän korvakoe (M3:n kuvio uudella kädellä)
CQ.boot; CQ.patchPreset(\classic); CQ.set(\c5Send, 0.5);
// 1. Aja F10 hitaasti 0 -> 1. Rajojen pitää nyt istua näin: vakaa ->
//    vibrato ~neljänneksellä, porras/liukuvaellus koko leveällä
//    keskialueella, hyperkaaos vasta viimeisellä neljänneksellä.
// 2. Pysäköi fader keskipäivään: tämän kohdan PITÄÄ olla se "humalainen
//    nauhuri" -makea kohta, johon käsi palaa aina.
// 3. Yökartalla: vedä silmää vaakaan ääripäästä toiseen - saman
//    dramaturgian pitää toistua eleessä.
```

### 13.5 The VCA law — the shell opens and closes

**Accepted 2026-07-29: one fixed signed p = 2 law.** A same-session A/B/C
compared the old linear contribution, signed p = 1.5 and signed p = 2 in the
real Cocos. p = 2 kept weights 0.05–0.10 subtle, made phrases at 0.20, stayed
deep but round-edged at 0.40 and cut complete holes only at 0.80. Its positive
arc touched the real VCA ceiling without becoming a static plateau; feedback
weights −0.20/−0.50 made two depths of recess without erasing empty blocks.
SP.AF and whole-preset controls passed.

p = 1.5 had no identity that cable depth could not supply. Linear retained a
recognizable sliding character, but the session found no necessary musical job
that justified another GUI/snapshot mode. No character switch ships. The
temporary hidden crossfader and p = 1.5 branch were removed after the choice;
`\cqCoco` contains only the accepted p = 2 graph.

**The sketch below did not match the code, and the rake is why it was
caught.** "Verify the current form first" was the right instruction: the
real form is `gain = (knob + sum).clip(...)` — the knob is the VCA's rest
and the matrix *adds* to it — not `gain = (1 + sum)`. Shaping the total
would have recalibrated every knob, default and stored snapshot
(`coco1In` 0.8 → 0.64) and would have destroyed a documented patching
move: a banana into a VCA whose knob is shut can currently open it, and
under a multiplicative law it never could. So the shaping is applied to
the modulation's **contribution**:

```
shaped = (1 + sum) ** 2 - 1        // sum clamped ±2 first (the rake)
inVca  = (knob + shaped).clip(0, 2)
```

At a knob of unity `1 + shaped = (1 + sum) ** 2` only **before the real
VCA ceiling**. Input is subsequently clipped to 0..2 and feedback to 0..2.5,
so the sketch's sum +1 → x4/+12 dB never reaches the converter; p = 2 reaches
the existing rail earlier and spends longer there. Away from unity the knob
keeps its zero-sum meaning, but sum −1 subtracts one rather than universally
closing a knob above unity. `g.abs * g` is `g² · sign(g)` in two ops, and the
sign keeps the pre-clip law monotonic below sum = −1 instead of folding up.

**One consequence the sketch does not mention and the ear will meet
first:** the p = 2 curve's slope at the origin is 2, so existing VCA
modulation becomes substantially deeper. Choosing that exponent was the
implementation's musical decision. This reaches `\selfNoise`, `\organMode`,
`\mayhem` and the user's snapshots: knob values remain numerically calibrated,
but a snapshot containing VCA bananas is **not sonically unchanged**. The
accepted 0.05–0.10 subtle region is how the patch keeps nuance; high weights
deliberately enter the instrument's experimental country. This law never
touches pitch or speed modulation, so a seasick `_spaf` patch must be tuned at
its own cable, SP.AF depth or `patchDepth`.

**Hypothesis.** The matrix in/fb-VCAs sum linearly; an analog verso/
inverso VCA breathes more exponentially. A gentle power shaping gives
duckings and tremolos a "valve opens / valve shuts" body.

**Accepted implementation.** In `\cqCoco`, the matrix contribution is:

```
shaped = sign(1 + clamp(vcaSum, -2, 2))
       * abs(1 + clamp(vcaSum, -2, 2)) ** 2 - 1
gain = clip(knob + shaped, 0, vcaCeiling)
```

- p = 2 is a fixed constant, not a parameter.
- Sum 0 leaves the knob unchanged; sum −1 subtracts one; sum +1 contributes
  +3 before the real input/feedback ceilings of 2/2.5.

**Rake.** Check how the sum is clamped before shaping: `\mayhem` can swing
a VCA sum far past ±1, and `(1 + x) ** 2` at x = +4 is a ×25 gain — safe
(the clip bounds it) but tonally scorched. If the sum is not already
clamped, clamp to ±2 **before** the shaping and say so in the SynthDef
comment. This applies to the continuous VCA path only — gate/trig
destinations have their own Schmitt semantics and are not touched.

**Consequences to check.** DSP change: full battery incl. A3 (VCA wobble
is in the stress patch — watch the peak margin) and A4. Document the
chosen law in SPEC §5.5's destination notes and ARCHITECTURE's coco
section (it is an interpretation, like the organ derivation).

**Listening evidence:** archived in `tests/manual_checklist.md` §13.5:
small-weight nuance, three-depth inverso chop, positive ceiling arc, two-depth
feedback duck, SP.AF isolation and whole-preset checks.

### 13.6 The zipper seam — measured and closed without a DSP change

**Not a change — a measurement with a prepared answer.** Factual correction
found while building the gate on 2026-07-29: the compass pours weights as a
30 Hz stream into control buses, but production readers have never been
unlagged. `CQSynthDefs.prMatrixWeight` has applied one 50 ms `Lag.kr` to every
destination since M1, before `patchDepth`. The tide makes the remaining seam
*mostly* moot — disjoint cables step whole, which is the banana idiom — but
**shared cells glide**, and their 30 Hz writes over sustained VCA material are
still the right place to verify the real implementation rather than trust its
topology.

The reproducible NRT gate `tests/nrt_zipper_measure.scd` now renders the
production 30 Hz + 50 ms path, a continuously ramped reference with the same
lag, and an intentionally unlagged positive control. On the slow 0.9→0.1
shared-cell glide, production residual was −55.87 dB RMS relative to the
signal; the positive control was −55.15 dB. The tiny 0.72 dB RMS difference
does not decide audibility. In the 2026-07-29 listening gate the user could
not distinguish zippering reliably and judged it probably inaudible.
Production therefore stays unchanged; the remedy ladder below is retained
only if later material exposes a repeatable problem.

**Measurement (fold into the G3.1a session):**

```supercollider
// 13.6 — zipper-mittaus: jaetun solun liuku sustainatulla lähteellä
CQ.boot; CQ.patchPreset(\empty);
CQ.deckA.load("<pitkä tasainen ääni, esim. urkupiste>");
CQ.set(\coco1SlotDeck, 1); CQ.set(\coco1In, 0.8); CQ.hold(1, 1);
CQ.patch(\cas1, \coco1_inVca, 0.9);
// Yö: leimaa tämä kulmaan A; muuta paino 0.1:ksi ja leimaa kulmaan B.
// Purjehdi HITAASTI A:sta B:hen: jaettu solu liukuu 0.9 -> 0.1.
// Kuuntele tarkasti: portaikkoa/rahinaa vai sileä liuku?
```

**Remedy ladder if it grates (stop at the first rung that cures it):**
1. Nothing — 30 Hz plus the inherited 50 ms reader smoothing may already be
   clean; a faint disjoint-cable step *is* the hardware's banana-contact truth
   (§15).
2. Client-side glide: the touch layer splits each paced blend write into
   2–3 sub-writes ~10 ms apart, interpolating only the **shared** cells
   (the tide already knows which those are — disjoint cells must keep
   stepping whole, that is the design).
3. Replace — do not stack — the inherited blanket 50 ms helper with a
   destination-aware smoother on the **continuous destinations only** (spaf,
   inVca, fbVca, fm, deck rates). **Never add lag to gate/trig destinations**:
   a lagged flip/skip/spesal sum reshapes Schmitt crossings and charge-lamp
   truth. The inherited helper currently does precede those Schmitt sums; its
   removal is a separate gate-timing audit, not a free side effect. This rung
   is a SPEC §15/§5.5 edit and needs the user's yes plus the full battery.

### 13.7 The punch — already planned

The momentary hold is §2 of this document (right-press held on the
shell's heart; exhaustive exit paths; optional Shift+1/2 with the
autorepeat rake). It is listed here only so the ear pass reads complete:
it is the hardware player's punch-in, and it ships with the GUI plan, not
with the DSP experiments.

### 13.8 Preamp loudness compensation — later listening experiment

**Recorded 2026-07-28; not part of N7 and not yet a DSP change.** The current
drive correctly raises level before the asymmetric `tanh`, so its lower half
can read more like gain than timbre. After the Night gesture has been played
in real sessions, compare bypass/processed RMS and peaks on live input, mic,
percussive decks and sustained decks at several drive/bias settings. If the
level jump obscures the musical gesture, audition a conservative,
drive-dependent **partial makeup attenuation** after the waveshaper rather
than automatic full loudness normalization. The goal is to expose saturation,
not erase the preamp's pressure.

Any accepted curve must be specified in `SPEC.md` (and in `CQSpec` only if it
introduces a parameter), keep peak safety
and stereo symmetry, place the envelope follower after the audible compensated
signal, pass the full NRT battery, and survive an equal-loudness listening
comparison. Do not infer a curve from the GUI's normalized value alone or
silently fold this experiment into a visual iteration.

**File A/B accepted and production curve implemented 2026-07-29.**
`tests/nrt_preamp_loudness.scd` peak-aligns the local speech, drum-loop and
sustained-organ sources to the Deck contract, then repeats the exact same
excerpt at drive 1/2/6/16/40 and at bias 0/0.15/0.6. It renders the current
production path beside the legacy uncompensated path and the rejected stronger
−1.5 dB per drive octave reference. The chosen production path is −1.0 dB per
actual drive octave. These are partial attenuation curves, not
material-dependent normalization, and remain anchored exactly at drive 1.
At drive 40, RMS rise relative to drive 1 measured:

| source | legacy uncompensated | accepted −1 dB/oct | stronger −1.5 dB/oct |
|---|---:|---:|---:|
| speech | +21.67 dB | +16.34 dB | +13.68 dB |
| drums | +22.17 dB | +16.84 dB | +14.18 dB |
| sustained organ | +16.09 dB | +10.77 dB | +8.11 dB |

The user heard the legacy level rise as excessive and selected the middle curve
as the most playable compromise: it limits runaway loudness without flattening
the saturation pressure. Production now applies
`dbamp(−log2(actualDrive))` after the asymmetric waveshaper and before the
20 Hz HPF. The post-HPF envelope follower therefore tracks the same compensated
audio that is sent to the Coco. No parameter, GUI control or snapshot migration
was added. Marker `CQ_PREAMP_OK`, 13,340 unit assertions, safety, hold, dust and
self-noise gates all pass. The selected production path passed its final
live-input confirmation on 2026-07-29, closing §13.8.

---

## 14. The norns port — waypoints and technical specs

The stage after the night-first instrument: **monome norns + grid (16×8
varibright) + 16n**, crow as an optional coda. The founding memo is the
v2 vision at the end of `docs/history/PLAN_v1.0.md` (agreed 2026-07-17);
this section builds on it and updates it with everything learned since —
it does not replace it. The memo's decisions stand: SynthDefs travel
nearly as-is (the vanilla-UGens law was written for this day), softcut can
never be a coco (no in-loop clip+quantize — the recirculating dirt IS the
instrument), the FDN goes (norns has its own reverb), and the **OSC-hub
fallback** (norns + grid as controllers → OSC → laptop SC) remains the
risk parachute if the on-device DSP budget refuses.

### 14.0 Ground truths of the device

- **crone** runs scsynth; engines are sclang (`CroneEngine` subclasses) —
  the DSP and the pure core classes port. **matron** runs Lua and owns the
  screen (128×64, 16 greys), the three encoders and keys, the grid, the
  clock, the params/PSET system **and all MIDI — the 16n arrives in Lua,
  not in sclang**.
- One effective core for scsynth (CM3+, A53) — plausibly 6–10× slower
  than one reference-laptop core, against our measured 19.8–21.5 % there.
  Naive arithmetic says over-budget; the diet (§14.2) plus the spike's
  *measurement* (§14.5, N1) decide. No number in this section outranks a
  number measured on the device — the paint-event law, reborn as the
  device law.
- 48 kHz fixed (SPEC's SR-independence law already covers it). ~1 GB
  shared RAM — the coco buffers (2 × 2^17 floats = 1 MB) are nothing;
  crone's server config (memSize, block size) is norns-owned, so **whether
  the block 128/256 diet lever is even available is a spike question**,
  not an assumption.
- norns sclang is built **without Qt**: `Window`, `UserView`, `Pen`,
  `Image` do not exist there. See rake N1 below — this is the port's
  first structural task, not a detail.

### 14.1 The shape of the port — one core, three faces

The split falls exactly on the seam the architecture has defended since
F0: **pure core vs view.**

- **Travels into the engine as-is:** `CQSpec`, `CQState`, `CQMatrix`,
  `CQSynthDefs`, `CQCompass` (pure sclang, no Qt anywhere in them), plus
  the safety chain and the graph. Engine *commands* land in the same
  funnels — `CQState.set` / `CQMatrix` — so every clamp stays on the SC
  side of the Lua/SC boundary and **a Lua bug can never bypass safety**.
  Engine *polls* replace the OSC telemetry (§14.2).
- **Replaced by Lua:** `CQMidi16n` (16n arrives in matron; keep SPEC
  §6.1's fader map *identical* — muscle memory is a feature that ports for
  free), the keyboard map (keys/encoders), snapshots' front (PSET as the
  norns-native face over the engine's state — decide at N3 whether
  `.cqsnap.scd` stays the wire format read engine-side, since Lua cannot
  read SC literals).
- **Does not travel:** every `CQGui*`/`CQInk*` class, the shell, the
  drawer. The night map remains the laptop's face; norns is a third face
  of the same instrument, not a small screenshot of the night.

Repo shape: monorepo — `classes/` stays the shared core, `norns/` gains
the engine wrapper + Lua script. The laptop test battery (unit + NRT
safety + hold integrity) keeps running against the shared core on every
change; the device gets a lightweight boot self-check, because running
NRT renders on a CM3 as a gate is not a workflow anyone survives.

### 14.2 The diet — musical, and taken as a gift

The framing is binding: the diet is not an apology. This instrument's
identity is 8-bit quantize, ZOH, aliasing, stepped S&H — all of which cost
*nothing*. The expensive parts are exactly the polite parts, and most of
them the hardware never had either. What ships / is replaced / is cut:

| ships (identity) | replaced (norns gives it) | cut (politeness) |
|---|---|---|
| coco pair, in-loop clip→quantize, dust, organ, dolby | optional ambience → norns system reverb | stems and the retired Space FDN |
| quantussy: 5 castles, spesal, chaos — count is identity | recorder → norns tape | watchdog auto-trim (notice may stay) |
| 16×22 matrix semantics, patchDepth, shake/tide/weather math | decks → **softcut as source**, its output feeding the coco input slot (softcut stays clean; the coco does the dirt) | `\smooth` interp (keep `\zoh` — the authentic one *is* the cheap one) |
| master safety chain — **non-negotiable on headphones** | mic path → norns inputs as liveL/R | GUI telemetry rates (polls at 10 Hz grid / ~12 fps screen) |

The load-bearing lever from the memo, now even more valuable: **the
sparse matrix**. The dense 22×16 audio-rate summing mostly multiplies
zeros; a played patch is 10–70 cables. Rebuild the mod-routing graph on
*topology* change only (weight changes stay bus writes). Consequence to
embrace, not hide: a cable insertion may click — **a banana contact
clicks on the wooden box too** (§15's own words). Bound it (short fade or
accept), listen at N1, decide by ear.

Gifts to receive consciously rather than fight:

- Block 128/256 (if crone allows) lengthens the `LocalIn` one-block
  delays — the Quantussy ring breathes at 2.7/5.3 ms instead of 1.3. That
  is a *different room*, not a bug: listen before judging (hyperchaos
  character will shift slightly).
- Coarser control/poll rates hide inside an instrument whose whole
  modulation language is already stepped sample-and-hold.
- If CPU forces `qShLag` toward its declick minimum everywhere: that IS
  the authentic hardware step.
- The screen's 16 greys are the night's whisper/line/spark palette born
  again; the grid's stillness is the fireflies' law with LEDs.

### 14.3 One truth for parameters — generated, never copied

The norns params system needs every CQSpec range/curve/default in Lua.
Hand-copying them creates the exact drift the SPEC↔CQSpec law exists to
forbid. Therefore: **a codegen script emits the Lua params table from
CQSpec** (names, ranges, curves incl. §13.1/§13.4's warps, defaults,
enums), and a repo test asserts the generated file is current. SPEC.md
remains the single truth of the port (the memo's own law). The same
generator can emit the 16n map and the poll list.

### 14.4 The grid and the screen — a new body, its own spec

The grid is not a port target; it is a **new body for the same grammar**,
and it gets its own design document and review rounds exactly as
GUI_SPEC got them (first runnable → review → iterate; no surface built on
guesses). Three directions to take into that document, with a
recommendation:

- **(a) Bay pages:** matrix cells on keys, dest-pages. Honest but
  administrative — the v1.0 grid ledger reborn at 16×8. Keep as the
  precision fallback.
- **(b) Two-finger bananas (recommended):** hold a source key, press a
  dest key — *the banana held between two fingers, literally*. Varibright
  swell while held = the audition's WYHIWYG light; release = plant;
  double-tap = rip. This is §1's whole philosophy with no screen at all.
- **(c) The performance shore:** a key row for spesal taps, hold/punch
  (momentary = held key — the punch's natural home!), shake, and **four
  corner keys as the compass seeds** with a 4×4 key region as the tide
  pool — the weather machine under the fingers.

Laws of light that transfer as written: every LED is information; LED
brightness = magnitude (aura/|weight|); **stillness — a silent instrument
is a still grid**; motion rides the polls, never a Lua metro of its own;
polarity needs a second channel beyond brightness (grid has no hue — form
it spatially or by key-pairing, never by blink-decoration).

The screen is a **periscope, not a window**: one focused body at a time
plus a thin constellation strip; regime marks, not values; lowercase; the
night's display laws (show the regime; no telemetry ≠ zero; no invented
motion) apply verbatim. Do not attempt to draw the archipelago on
128×64 — distill it.

### 14.5 Waypoints

- **N0 — decisions & audit** (before any device code): repo layout
  (`norns/` in monorepo), the Qt-reference audit (rake N1), the
  `.cqsnap`/compass wire-format decision, dev loop (maiden/ssh).
- **N1 — the spike** (the memo's 1–2 days, unchanged): coco pair +
  quantussy as a minimal CroneEngine on the device. Measure: CPU at the
  available block sizes, dense vs sparse matrix, temperature over 30 min
  (CM3 throttles — a soak that ignores heat lies). **Exit: a measured
  table and a go/no-go.** No-go → the OSC hub is the product for a while,
  and nothing is wasted: the control surfaces (§14.4) are the same.
- **N2 — the engine**: full DSP + safety chain + funnels as the engine
  lib; polls; boot self-check; the laptop NRT battery green against the
  shared core.
- **N3 — the Lua control core**: generated params (§14.3), 16n map,
  PSET/snapshot bridge, tape + system-reverb integration.
- **N4 — the grid grammar**: its own spec + review rounds (§14.4).
- **N5 — the screen + the device soak**: CPU *and thermals*, plus the ear
  pass revalidated on the device DAC (§13's accepted settings may sit
  differently on different converters — listen again, adjust nothing
  blindly).
- **N6 — crow (optional, from the memo)**: 2 CV ins as CV-faders through
  the CQState funnel, 4 outs for infra/balcium castles (audio-rate never
  crosses the serial link).

### 14.6 Norns rakes

1. **No Qt in norns sclang.** Any hard reference from core classes to a
   GUI class breaks compilation on the device. Audit `Cocoquantus.sc`
   (it constructs `CQGui`): the engine package must exclude all
   `CQGui*`/`CQInk*` files **and** the facade's GUI touchpoints must go
   soft (`\CQGui.asClass`-style lookup or a split facade). Do this on the
   laptop first — it is also what makes the headless law enforceable.
2. **16n lives in Lua on norns.** Do not port `CQMidi16n`; port its *map*
   (via the §14.3 generator) and its takeover semantics.
3. **Clamps stay engine-side.** Lua sends commands; CQState/CQMatrix
   clamp. Never re-implement a clamp in Lua "for responsiveness" — a
   display estimate may be Lua's, a written value never.
4. **crone owns the server config.** Block size, memSize, device — verify
   what is actually settable before promising the block-size lever.
5. **Sparse-matrix rebuilds click.** Decide the click's fate by ear at N1
   (bounded fade vs honest contact-click); never let a rebuild glitch the
   *safety chain* — it is one synth that never rebuilds.
6. **Polls are not OSCdefs.** The engine exposes named polls; rates are
   set Lua-side. Keep the §9-family laws: no poll = no light, silence is
   still, the last value is never invented.
7. **Thermals are part of the CPU budget.** A CM3 that throttles at
   minute 20 fails a soak that looked green at minute 5.
8. **The ear decides twice.** Every §13 tuning accepted on the laptop is
   re-listened on the device before it is called done there.
