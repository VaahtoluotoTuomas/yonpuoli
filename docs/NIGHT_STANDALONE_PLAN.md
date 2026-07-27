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

---

## 0. Context — the day-mode question

The user is considering whether the instrument would be more coherent and
more itself as a **night-only instrument**, with the drawer holding the
menu-like remainder. This document does not decide that. It matters here for
one reason: **these three additions are prerequisites for even evaluating
night-only.** Ranges, dolby and the punch are the last musically load-bearing
things a player cannot do at night. Once they land, night-only can be
evaluated by simply *not pressing Tab* for a few sessions — no code, no
feature flag (PLAN.md forbids runtime flags), no deletion. If the verdict is
night-only, the follow-up work (its own gate, not this one) is:

- a drawer **workshop page** carrying every remaining CQSpec parameter with
  honest ledger-style controls (the drawer is where "the ledger look is
  honest" — §16.7): coco slots, dust, organ level, bits/wrap/interp,
  level/pan/send, preamp src/drive/bias, castle sends and detunes, qShLag,
  deck rate/loop, micGain with its warning color. SPEC §6.2's obligation —
  *every* §5 parameter reachable without code — must be re-verified against
  night + drawer + master strip alone before any Day deletion;
- a new home (or a recorded death) for Day's two overview surfaces: the
  spesal constellation (the whole-network coupling view; night shows chords
  only under the hand) and the static coco memory-path map (already on the
  "Myöhemmin" list as questioned);
- only then the deletion of the Day panel, as reversible structural commits.

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
4. **Opaque boxes eat lines and marks.** Every endpoint view is an opaque
   box (26 px jacks, 34 px lamped sockets, 24 px crescents). A mark drawn on
   a body layer near one of those boxes can be silently covered — this bug
   has been fixed twice already (roots, inner clock). Either draw the mark
   inside a view that owns it, or assert box-vs-mark clearance in the
   geometry test.
5. **The §4.5 priority chain, identically in hover and press.** Order inside
   every `Down` handler: cable under the pointer wins (`overlay.patchAt`),
   then the new touch point, then the existing touch points, then
   body-solo/ground fallthrough. The hover cue must be computed with the
   *same* priority — a cue that advertises an action the next press will not
   receive is a lying cue.
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
press, so it always arrives at the shell view), **Esc**, **Tab view flip**,
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

**What it is musically.** The castle range switch (\infra ×0.02 / \balcium
×1 / \audio ×40) is how a castle stops being a modulator and starts being a
voice. On the hardware it is a front-panel switch; at night it is currently
display-only (the i/b/a whisper letter). This is the single biggest gap in
night-as-instrument.

**Design — a drawn rate, not a widget and not a letter.** §6 forbids
anything that looks like a knob or transplanted widget; §16.4 forbids a
control that requires reading text (the sower constellation died of this).
So the control is a **rate ripple**: a small drawn mark (~14×8 px box, §16.5
style) at the castle's 6 o'clock anchor (the current `letterAnchor`,
r ≈ 33·scale), whose *form is the regime*:

- `\infra` — one long shallow swell (a single half-wave);
- `\balcium` — two half-waves (one full cycle);
- `\audio` — five tight half-waves (a dense ripple).

Slow/mid/fast reads pre-verbally — it is §16.6's "show the regime" law made
into a control. The existing i/b/a letter stays as the whisper caption
*under* the ripple (approved night text, §16.4) — the mark carries the
control, the letter only confirms. Note the deliberate distance from the
§16.5 `wave` glyph (castle-fm sockets): the fm wave is always exactly one
cycle inside a socket ring; the ripple varies its density and lives on the
body. If review finds them confusable, thicken the distinction by form
(e.g. give the ripple a baseline stroke), never by hue alone (§16.2).

**Interaction.** Tap cycles `\infra → \balcium → \audio → \infra` — the
night's established idiom for stepping an enum is the tap (the spesal dial).
Hover brightens the ripple to line ink (static, §4.5). No flash: a range is
a setting, and the mark itself redraws to the new density instantly via the
already-registered `cNRange` updater in `CQGuiNightMap.prRegisterUpdaters`
(extend the callback that today only updates `rangeLetters`).

**Geometry and its traps.**
- Anchor exists (`letterAnchor`); add a `rangeMark` anchor + a
  `rangeHit(castle, point)` class method (markHit-style 10 px box, but
  verify it against the ripple's drawn extent — cue and act, one geometry).
- **Clearance asserts needed at both sizes**: the mark's hit box versus
  (a) the spesal endpoint's opaque 34 px lamped box at 8 o'clock r = 40 —
  this is the crowded neighbor; (b) the fm socket box at 4 o'clock r = 28;
  (c) the dial annulus (r 10 ± 8); (d) the body ellipse with its ±8 %
  jitter and 10 px `bodyMargin` — at maximum jitter the body's cue edge
  comes within a couple of px of r 33, so `castleDown` must consult
  `rangeHit` **before** `bodyHit`, and `prHoverCastle` must do the same
  (three exclusive cues on one castle now: clock, ripple, body — never two
  lit at once).
- The ripple is drawn by the castle's own opaque layer (`prDrawCastle` →
  `CQInkNight`), so its repaint stays local; it must also *fit inside the
  castle's layer box* (`box` = ±(36·scale + 4)) — at scale 1 the letter
  anchor sits at +33 with the caption below it: measure, and if the caption
  clips, grow the box and re-run every clearance test, because the box is
  also the layer that covers the river stubs (§16.6's courtesy law).

**Funnel.** `state.set(\cNRange, next)` via the shared enum-cycle helper
(law 1.2). CQState/CQSpec clamp malformed enums already; the GUI never
stores the symbol list itself.

**Day parity.** Day's 3-state switches are untouched. (If day later dies,
the drawer workshop inherits a switch — not this gate's problem.)

**Spec/docs to edit:** GUI_SPEC §6 (strike "display only; ranges are set in
day"), §6.1 table row, §16.5 (ripple recipe), §16.6 castle anatomy, manual
checklist block (tap each castle through all six steps *by ear*: an infra
castle stepping to audio must be immediately audible in `\classic`), PLAN.

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
12. **(flood)** Growing an endpoint view grows an **opaque box**: every
    grown box re-triggers the courtesy law (§16.6 — a line under an opaque
    layer must be redrawn by that layer or routed clear). The river stubs,
    the root arrival stubs and the org stems all have lengths tuned to the
    *current* boxes. Re-derive them from the box sizes, don't re-tune by
    hand.
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

Strokes, fonts, palette, alphas: **unchanged** (§16.3, §16.4, §16.10 —
"ink does not thicken when the paper grows"). Pen-call counts are size-
independent, so the flood costs the frame budget nothing; only the opaque
boxes grow, which is rake 12's business.

If the matrix proves the minimum stage genuinely cannot hold the new
measures, raising the documented minimum window is a **user decision** to
surface at the review, not something the implementer decides.

---

## 9. The almanac becomes landscape — the cluster is broken

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

---

## 10. The roots — mangrove, not thread

Two lonely polylines read as fragile because they are drawn as *lines*
carrying a jack, not as *bodies* carrying the instrument's blood in. The
fix adds visual mass without inventing information:

- Each root becomes a **mangrove foot**: 3–4 seeded, wobbled strands
  leaving the bottom edge over a widened base (~40–60 px of shoreline) and
  **converging to the single tip jack** — one endpoint, exactly as today,
  because there is exactly one envelope per side and extra tips would be
  lying marks. Reach grows ~64 → ~110 px.
- The tip jack view grows with §8's table and keeps its aura glow; the
  arrival-stub courtesy (the root's last stretch redrawn inside the jack's
  box) now applies per-strand — derive the stub from the box size (rake
  12), and keep the side twigs growing *away* from the tip (the recorded
  2026-07-21 fix; do not re-learn it).
- The mic sprout stays exactly as it is — display only, three states,
  beside envA.
- Roots stay bodies-without-solo (single endpoint — the recorded law).

---

## 11. The birds fetch — loading without a single arrow

The decks need in-GUI loading and folder navigation, and the night forbids
buttons, arrows and question marks. The bird itself is the control, in the
app's own gesture grammar — three acts, told apart by the hand, no new
glyphs at all:

- **Tap the bird (< 8 px travel — the lasso's own threshold) = go fetch.**
  Opens the system file dialog (async `Dialog.openPanel` →
  `CQ.deckX.load`; rake 14). The bird is the obvious "this is the deck"
  body, it already brightens for a file drag, and a cancelled dialog is a
  harmless no-op.
- **Wheel over the bird = the next perch.** Wheel down = next file, wheel
  up = previous (`CQ.deckX.next/prev` — the folder must be loaded; the
  facade's own guards answer otherwise). The wheel already means "step
  through an ordered neighborhood" everywhere in the app (weight nudges);
  here the neighborhood is the folder. Rake 15 (momentum) applies.
- **Ruffle the bird = bring me anything.** A drag that starts on the bird
  and reverses horizontal direction at least twice (swings ≥ 6 px, total
  travel ≤ ~300 px) is a *ruffle* — on release the startled bird returns
  with a random file (`CQ.deckX.random`). Travel-based, no timer, and
  surreal in exactly the way this instrument means it: you shake the tree,
  the bird brings what it brings. A drag that is neither a tap nor a
  ruffle does nothing.
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
3. **The almanac becomes landscape** (§9) — the cluster breaks: moon + rain
   + sun rise into the sky, the pool goes to the bay's bottom water with the
   graft, the corner empties. (Kept separate from the flood so a disagreement
   about the pool's form cannot hold the size pass hostage — which is exactly
   what happened: the first build of this step was rejected and rebuilt, and
   §8 was already safe on `main`'s line.) The **size re-check of the whole bay
   rides this step's review**, because sizes are relational (§8).
4. **Range ripples** (§3) and 5. **dolby oysters** (§4) — into the grown
   landscape, so their clearances are done once.
6. **The birds fetch** (§11).
7. **The roots** (§10) — pure drawing, can ride along with 6 if reviews
   allow.
8. **The drawer workshop page** (§0) — its own gate; completeness audit
   against SPEC §6.2 (every CQSpec parameter reachable) is its exit
   criterion.
9. **The day verdict** (§0) — only after 1–8 have been *played* for real
   sessions. Deletion, if chosen, is its own reversible gate.

After steps 1–7 the night map is, deliberately, a complete instrument:
every hardware-panel gesture (speed, in, fb, spaf, hold+punch, flip, skip,
dolby, ranges, master freq, chaos, spesal, patching) plus the machine's own
gifts (the moon, the pool, the weather, the birds) — playable from one
landscape, with the drawer as its workshop and the master strip as its
spine.

The **ear pass** (§13) is independent of this GUI order and can interleave
with it — each of its items is its own listening iteration. Its natural
start is right after the G3.1 review, whose script already carries the
zipper measurement (§13.6).

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
drift). Day knob arcs and night crescents follow automatically — they draw
from `unmap`.

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
// 2. Nosta organ (Day/workshop: coco1Organ 0.5) ja kelaa pohjilla:
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

**Proposal.** In `\cqCoco`, replace the dust source (verify its current
form first) with a blend, injected at the same pre-converter point so
safety invariant 2 (clip before quantize) is structurally untouched:

```
dustSig = (LPF.ar(WhiteNoise.ar, 9000) * 0.35
    + Dust2.ar(clockHz * 0.02) * 1.2) * dustLevel
```

- `clockHz * 0.02` ≈ 30 grains/s at the new 1 500 Hz floor, ~960/s at
  48 k — crackle when slow, dense sizzle when fast, and dust *audibly
  follows the speed knob* (the poetic coupling, free of charge).
- Calibrate the two mix constants by ear so the default `dust = 0.02`
  keeps roughly the old loudness — old snapshots must not get louder.
- No new parameters (the norns diet): the blend constants are fixed
  interpretation, commented with a reference in the SynthDef.

**Consequences to check.** DSP change: full battery incl. A3 (its stress
case runs dust at max — impulses hit the quantizer at full scale, bounded
by clip2; the peak assert is the honest judge) and A4. M4's checklist
figure (selfNoise) is the acceptance ear.

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
curve factor ≈ **+2.5**, try 2–3 at the review). Value semantics — the
internal 0–5 octave FM depth — are untouched, so snapshots, patterns and
the DSP stay bit-identical; only the hand's mapping stretches. With
+2.5: fader noon ↦ qChaos ≈ 0.27 (deep wander), 3/4 ↦ ≈ 0.55, and the
hyperchaos region 0.6–1.0 compresses into the last quarter.

**Technique.** One curve field in CQSpec + SPEC §5.4 row note. The eye's
horizontal axis at night and the Day knob both read through `unmap` —
they follow. M3's sweep is the acceptance ear.

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

**Hypothesis.** The matrix in/fb-VCAs sum linearly; an analog verso/
inverso VCA breathes more exponentially. A gentle power shaping gives
duckings and tremolos a "valve opens / valve shuts" body.

**Proposal.** In `\cqCoco`, where the inVca/fbVca sums become gains
(verify the current form and any existing sum clamp first — see the rake
below), replace the linear law with:

```
gain = max(0, 1 + vcaSum) ** p
```

- Try **p = 2 first** (it is free: `g = max(0, 1 + x); g = g * g`), then
  p = 1.5 if 2 is too dramatic. p is a fixed constant, commented — not a
  parameter.
- Full closure at sum = −1 is preserved (tremolo reaches silence);
  unity at 0 is preserved; sum = +1 now gives +12 dB (p = 2) into the
  converter instead of +6 — hotter overdrive, still bounded by clip2.

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

**Listening protocol:**

```supercollider
// 13.5 — VCA-lain korvakoe
CQ.boot; CQ.patchPreset(\selfNoise);
// 1. \selfNoise hengittää fb-VCA:ita infra-triangeleilla: vertaa muistiin
//    - aukeaako ja sulkeutuuko kuori nyt "venttiilinä" (nopea sulku,
//    pyöreä avaus) tasaisen liu'un sijaan?
CQ.patchPreset(\empty);
CQ.deckA.load("<rytmikäs äänite>"); CQ.set(\coco1SlotDeck, 1);
CQ.set(\coco1In, 0.8);
CQ.patch(\cas1, \coco1_inVca, -0.8);   // inverso chop
// 2. cas1:n portaat leikkaavat sisääntuloa: chopin pitää PURRA - pohja
//    kiinni asti, avaus muhkea. Käännä paino +0.8:aan: ylipaine puskee
//    konvertteriin kuuluvasti mutta rajatusti.
CQ.patch(\envA, \coco2_fbVca, -0.5);
// 3. SPEC:n oma duck-esimerkki: puhu/soita liveen - feedbackin pitää
//    väistää artikuloiden, ei liukuen.
```

### 13.6 The zipper seam — measured in G3.1, remedy ladder ready

**Not a change — a measurement with a prepared answer.** The compass now
pours weights as a 30 Hz stream into unlagged control buses (§15's open
seam, finally exercised for real). The tide makes this *mostly* moot —
disjoint cables step whole, which is the banana idiom — but **shared
cells glide**, and a glide in 30 Hz steps on a VCA cable over sustained
material is where a zipper would live.

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
1. Nothing — 30 Hz already is the "thin the writes" answer; a faint step
   *is* the hardware's banana-contact truth (§15).
2. Client-side glide: the touch layer splits each paced blend write into
   2–3 sub-writes ~10 ms apart, interpolating only the **shared** cells
   (the tide already knows which those are — disjoint cells must keep
   stepping whole, that is the design).
3. Server-side `Lag.kr` ≈ 15–20 ms on the **continuous destinations
   only** (spaf, inVca, fbVca, fm, deck rates) — within the norns-diet
   ceiling (one Lag per dest). **Never on gate/trig destinations**: a
   lagged flip/skip/spesal sum reshapes Schmitt crossings and charge-lamp
   truth. This rung is a SPEC §15/§5.5 edit and needs the user's yes.

### 13.7 The punch — already planned

The momentary hold is §2 of this document (right-press held on the
shell's heart; exhaustive exit paths; optional Shift+1/2 with the
autorepeat rake). It is listed here only so the ear pass reads complete:
it is the hardware player's punch-in, and it ships with the GUI plan, not
with the DSP experiments.

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
| coco pair, in-loop clip→quantize, dust, organ, dolby | Space FDN → norns system reverb | stems |
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
