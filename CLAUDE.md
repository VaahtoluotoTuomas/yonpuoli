# CLAUDE.md — Yölahti

Independent SuperCollider feedback instrument descended from a study of the
Ciat-Lonbarde Cocoquantus 2. Read `PLAN.md` for the current handoff, then `SPEC.md`
(behavior, parameter tables, safety — normative) and
`ARCHITECTURE.md` (structure, SynthDef sketches, tests — normative for code shape).
For any GUI work, read `GUI_SPEC.md` completely: it is authoritative for structure,
interaction, constants, visual language and motion, while SPEC §8 wins any safety
conflict. Do not invent parameters, ranges, bus layouts or ambiguous GUI behavior.

## Environment

- User's machine: Windows, SuperCollider ≥ 3.13 (IDE, `sclang.exe`, `scsynth.exe`),
  audio interface (ASIO), atovproject 16n faderbank (USB MIDI, CC 32–47; each
  fader on its own MIDI channel F1 = ch 1 … F16 = ch 16 — bind by CC, not chan).
- **You cannot hear audio.** Never claim something "sounds right". Verify with the
  automated tests, then ask the user to run specific steps from
  `tests/manual_checklist.md` and report back (give exact instructions: which preset,
  which fader, what to listen for).
- Never hardcode absolute paths; everything user-specific lives in `config.scd` or
  `tests/dev_paths.scd` (both gitignored). Build paths with `+/+`.

## Commands

On the first session, ask the user for their SuperCollider install path and store it
in `tests/dev_paths.scd`. Then, after **every** change to `classes/` or SynthDef code,
run in order (each prints a marker as its last line):

Use `tests/run_sclang_gate.ps1` for every headless gate. It preserves output and
terminates only the child it started if sclang falls back to its interactive
prompt after a class-library compile error.

1. `powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/compile_check.scd` → must print `CQ_COMPILE_OK`
2. `powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/run_all.scd` → must print `CQ_TESTS_PASSED`
3. `powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_safety.scd` → must print `CQ_SAFETY_PASSED`
   (NRT render, can take ~1 min; needs scsynth, no audio device)
4. DSP-behavior changes additionally: `tests/nrt_hold_integrity.scd` → `CQ_HOLD_OK`

A missing marker or nonzero exit = failure, even if output looks plausible. Fix before
moving on. Remind the user that after you edit `classes/*.sc`, their open IDE needs a
class-lib recompile (Ctrl+Shift+L) before live testing.

## SuperCollider style

- One class per file in `classes/`, `CQ` prefix, filename = class name.
- `var` declarations first in every function body (sclang syntax requires it).
- Parameters exist exactly once: in `CQSpec`, mirroring SPEC §5. No literal ranges,
  defaults or curves anywhere else. Every write path clamps through `CQSpec`.
- Every gain-affecting synth arg is lagged ≥ 30 ms server-side.
- Server objects (buses/groups/buffers) are owned by `CQGraph` only; pass the server
  in, never assume `Server.default` inside classes; `s.sync` between boot stages.
- GUI: view-only client of `CQState`/`CQMatrix` dependants; all UI code in `{ }.defer`;
  no server polling loops. A technical compromise against `GUI_SPEC.md` requires the
  user's approval before implementation.
- `MIDIdef`/`OSCdef` keys namespaced `\cq_*`; all freed in `Cocoquantus.free`.
- No `.play`/scratch code in `classes/`; demos go in `live.scd`.
- No Quarks or extension plugins in the core (vanilla UGens only).
- Comment the non-obvious DSP tricks with a reference (e.g. `// see ARCHITECTURE §4.4`).
- Keep files under ~400 lines; split classes rather than growing them.

## Safety invariants — NEVER weaken (and never weaken their tests)

1. Master chain on every hardware output (incl. stems): `LeakDC → tanh headroom →
   Limiter(0.89, 0.01) → bad-value squelch`. Fixed, non-bypassable.
2. Coco loop: `clip2(1)` (or wrap2) **before** quantization, inside the recirculation,
   unconditionally — this is what bounds all feedback.
3. Boot is silent until the graph is verified, then 2 s fade-in. Panic/mute always
   reachable (`CQ.panic`).
4. Every entry point clamps (code, MIDI, GUI, snapshot load, config, shake, matrix).
5. `micGain` defaults to 0; snapshots never auto-recall it above 0 (see SPEC §7).
6. NRT safety thresholds (peak ≤ −0.75 dBFS, no NaN, |DC| ≤ 0.02) may not be raised.
7. Recordings always create new timestamped files.

## Current handoff

- **Public product identity is Yölahti.** Cocoquantus is the explicitly credited
  lineage, not the product name or a fidelity promise. Keep the existing `CQ*` /
  `Cocoquantus` class and API namespace during Nocturne; do not start a mass rename.
  `README.md`, `SOURCES.md`, `docs/CASE_STUDY.md`, and
  `docs/DEVELOPMENT_PROCESS.md` define the public authorship and portfolio framing.
- The detailed v1.0 implementation log is frozen at `docs/history/PLAN_v1.0.md`.
- The verified audit result is indexed by `audit/README.md`; use
  `audit/VERIFIED_FINDINGS.md` over historical severities in the first audit reports.
- Archipelago work belongs on the `archipelago` integration branch. F0–F2 were
  accepted on 2026-07-19; the foundation and first runnable G1 shell/Ink version were
  committed as `1f4a210`. Do not repeat the foundation implementation.
- The accepted Day rollback chain is `3d631dd` (tight first complete skin/cables) then
  `b3d0096` (spaced 26/32/36 hierarchy). The preferred reviewed Day baseline now uses
  compact functional islands, 26 px standard / 34 px featured / 44 px primary knobs,
  a static truthful Coco memory-path map and a live Quantussy spesal constellation.
  The constellation maps real state: partner = state mod 4 among the other castles,
  single/double link = sample/track, three beads + number = state, inner mark = raw
  `/cq_castle` activity. Do not call that instantaneous sample a waveform.
- The Madrona/Sumu reference contributed only the principle that a visualization must
  expose actual sound/state structure. Do not add decorative filler or animation.
- **The Archipelago GUI epic (G1 Day · G2 Night · G3 Weather) is complete,
  accepted and merged (2026-07-22).** All of G1/G2/G3 were signed off — the
  tide sings, the plan's one honest unknown resolved by ear — and merged to
  `main` (PR #1, tag `archipelago-1.0`). **The integration branch is `nocturne`;
  the current review branch is `nocturne-night-only`.** The active roadmap is
  `docs/NIGHT_STANDALONE_PLAN.md` (§12 build order),
  with `PLAN.md` as its status view. The archipelago detail is frozen at
  `docs/history/PLAN_archipelago.md` and its acceptance checklist at
  `docs/history/manual_checklist_archipelago.md`; `tests/manual_checklist.md`
  is now the lean live list (SPEC M1–M8 regression + Nocturne blocks as they
  land). **Nocturne build-order step 1, the punch (§2), is done and accepted
  (2026-07-23, checklist N1)** — a right-press held on the shell's heart
  momentarily inverts `cocoNHold`, restored on release and on every exit path
  through one choke point (`CQGuiNightMap.cancelGestures`; mouse-up via
  `CQGuiNightTouch.shellUp`), captured-and-restored so a stuck punch can never
  leave a loop recording; the night smoke asserts each exit path. Step 2, the spring
  flood (§8), **is done and accepted (2026-07-26, checklist N2)**: the night's
  measures now live in one table, `CQNightMeasures`, and `CQNightGeometry` is
  the math that composes them, holding no size literal. The collision matrix was
  written first against the OLD constants, at both reference stage sizes, and
  only then did the numbers move — that order is the method, not a formality.
  Strokes, type, palette and alphas did not move (§16.3/§16.4/§16.10). Three
  things the flood taught: the shell layer's slack past the rim is what the
  castle boxes and the almanac live on (trimmed 30 → 24 px, now pinned by a test
  that names every mark the rim carries); the heart's disc deliberately swallows
  the innermost turn of the groove, because §4.5 checks it before the winding
  surface; and **cables are not static geometry, so no clearance test can cover
  them** — the grown transport marks now sit in the lane where cables from the
  ring sag toward this shell's moons (measured 6.0 px on `tri1 → coco1_spaf`,
  17.5 px before the flood). The user accepted that as the grammar working
  (cable wins under the pointer) and the mark's G2-gated home did not move. The
  user's own size touch-up rides §9's review, because sizes are relational and
  are judged in the new composition rather than one at a time. **Do not rebuild or
  "improve" accepted G1/G2/G3 surface on your own initiative.** The bullets
  below record what that accepted surface is and the laws it taught — historical
  reference, not a to-do list.
- **Build-order step 3, the almanac becomes landscape (§9), reached v3.2 on
  `nocturne-nightscape` and was accepted by the user on 2026-07-27 (checklist
  N3).** It is now part of the accepted N3→N8 chain merged locally into
  `nocturne`. **Build-order step 4, the castle range
  ripples (§3), was accepted on `nocturne-range-ripples` on 2026-07-27
  (checklist N4). Build-order step 5, the Dolby oysters (§4), was accepted on
  `nocturne-dolby-oysters` the same day (checklist N5). Build-order step 6,
  the fetching birds (§11), was accepted on `nocturne-birds-fetch` on
  2026-07-28 (checklist N6), build-order step 7, the preamp roots (§10), was
  accepted on `nocturne-preamp-roots` the same day (checklist N7), and
  **build-order step 8, the Drawer workshop (§12.1), was accepted on
  `nocturne-drawer-workshop` on 2026-07-28 (checklist N8).** Its tested
  Space retirement leaves a Night 31 + Master 4 + Workshop 39 partition that
  covers all 74 `CQSpec`
  parameters without Day. The user then chose Night as the sole interface and
  asked to merge the accepted chain: local `nocturne` now points at `dc95ced`.
  **N9's reversible Night-only cut was accepted on `nocturne-night-only`
  (2026-07-28, checklist N9) and merged locally into `nocturne`.** The
  permanent spesal constellation is recorded dead
  in favor of Night's under-hand coupling chord; the static Coco memory-path
  diagram is recorded dead in favor of the playable shells plus Workshop.
  N7's review found one defect worth remembering: the transient provenance
  label's ground erasure was cutting the tops off root B's two leftmost
  intake strands. **A constant inset cannot promise clearance from a seeded
  body** — the geometry now measures the strands inside the erasure's own
  band and gives way outward, and `rootLabelInward` is documented as a
  nominal rest rather than a promise. The clearance matrix was written
  against the old constants first: it failed on exactly the two strands the
  user saw, plus a 0.8 px mirror case on root A that no eye had caught.
  The review confirmed the sound and GUI write path with `\quantumDrone`.
  The user removed the inherited i/b/a captions; the accepted v2 gives every
  ripple a common keel, a path-only river knockout and an anchor 13 px below
  the actual seeded blob edge.
  The accepted N3 increment stops at tide+moon; the functional root/preamp body
  remains its own later step.
  What the three attempts taught, all of it now in the plan:
  1. **Version 1 was built exactly as `NIGHT_STANDALONE_PLAN.md` said and was
     rejected on sight.** The plan text was wrong, not the implementation: §7.2
     said "the fix is not to move them somewhere else … break the axis", which
     keeps all three parts in one reserved patch of right-hand sky. **A plan
     paragraph can be the defect.** §7.2 and §9 are now corrected in place with
     the old text kept as a warning; read the corrected §9 before touching this.
  2. The real fault was **grouping by function instead of by body** — a shake
     control, a matrix blend and three matrix ops sitting together touch no body
     around them, which is what a toolbar is. Version 2 separated the pieces,
     but the bottom ellipse still read as a placed control and its seeds were
     measurably shadowed by cables.
  3. **Version 3 removes the pool body.** Four inward-pointing memory seeds at
     the open NW/NE/SW/SE winds imply one direct bilinear field behind the
     organism; no shore, pad, outline or ambient water ink remains. After the
     first review, unfocused water became inert even under a long left sweep.
     Tab and the clickable lower-centre gill latch water focus; Shift is its
     momentary form. Focus backgrounds cable spans and removes their hit paths,
     but endpoints and bodies retain priority; left commits, right is elastic,
     and outside focus right stays harvest. Seeds have three truths: vacant
     open husk, stored-silence dark closed kernel, living luminous kernel.
     Shift writes; a latched Tab/gill focus also lets a vacant tap write;
     filled tap recalls and right clears. Filled-seed→filled-seed breeds
     directly; a valid target catches the top-layer thread and blooms.
  4. The moon is one Ø56 body beyond bird B and opens as a 0.22 crescent.
     Tap = shake, horizontal = phase; sowing density is `0.2 × phase²`.
     Vertical down/up = rain/grow and sun/wither after a dominant-axis 8 px
     lock. The next 32 px visibly tension rain 0.6…1.6 Hz and sun 2.5…6 Hz;
     weather glyphs exist only during the hold.
  5. Castle paper and its endpoint detail views are transparent; irregular
     ground-filled flesh masks the ring/chords at their real body edge. The
     old rectangle-repair ring stubs are forbidden.
  6. **Gill polish completed 2026-07-28:** the Tab-equivalent lower-centre
     gill is the one deliberate exception to cable-first priority. Its exact
     hit shape owns hover and press, clears cable hover, and its cue is drawn
     on the top hit-transparent gesture layer. Do not generalize this exception
     to other controls.
  7. Roots are not a decoration pass. The actual DSP is source selection →
     drive/bias preamp → Coco audio, with `envA/B` only the post-preamp
     Amplitude side-growth. When the root increment arrives it must draw and
     play that anatomy; extra decorative root lines alone are not authorized.
- `tests/run_all.scd` gained a `completed`/`aborted` guard (on the same branch,
  its own commit, cherry-pickable): an uncaught error inside a test class used
  to print `CQ_TESTS_FAILED` and then let the run reach `CQ_TESTS_PASSED`,
  because sclang's `try` does not survive the routine's `0.1.wait` yield. The
  summary now reports completed/total classes and the pass marker requires all
  of them. **The marker is the gate — treat a gate that can lie as a defect.**
- G3's built surface (accepted 2026-07-22): the §7.1
  compass in the almanac's water (corner seeds stamp/clear, tap = jump,
  left-drag = voyage, right-drag = elastic), the weather row beneath it
  (rain = grow, graft = breed, sun = wither, §7.2/§7.3 exact constants), a
  versioned/validated/clamped `presets/compass.cqcompass.scd` in CQSnapshots'
  manners (config `gui: (compassDir:)` redirects it — smokes use tests/tmp),
  and `CQCompass` as the pure model. **The blend is the tide, not the spec's
  first bilinear-amplitude sketch** (GUI_SPEC §7.1, normative now): shared
  cells glide, disjoint cables stand at full stamped weight until the pull
  drops below their fixed per-cell tide mark, then lift whole — so a midpoint
  is a handful of full-voice bananas instead of mush, Schmitt gates keep
  firing across the pad, and the pad is stable, learnable geography. The tide
  was confirmed musical at the G3.1a review (2026-07-22).
- G3 also landed the cable layer's offscreen Image cache + the weather
  gestures' storm form, both measured inside the real Qt paint event
  (`tests/gui_paint_profile.scd`; table in PLAN.md): blit ≈ 4.5 ms at any
  cable count vs 175 ms full ink at 352. Three hard-won laws, all in
  GUI_SPEC §7.1/§11: never render an Image inside a paint event
  (QtCollider's Pen is one global painter — it deadlocks the GUI thread);
  never batch cables into one giant path (Qt's stroker is superlinear in
  subpath count — 505 ms vs 177 at 352); and sibling-view repaints do NOT
  damage the overlay in a StackLayout stage (measured +0 draws), so the
  cache serves the 60 Hz gesture layers and whole-window damage. Also:
  sclang's stdout is block-buffered when redirected — a hung smoke shows
  nothing; the night smoke now carries a 90 s watchdog exit that flushes
  the evidence. `Rect.extent` returns a Point (no `.width`).
  - §16.7's dashed drop-target border is done and accepted (2026-07-22, checklist
    G2.10), both skins from one `CQInk.drawDropTarget`. Qt hands sclang **no
    drag-leave callback**, and
    **`canReceiveDrag` is asked once per dragEnter, never per move** (measured on a
    real file drag) — so the predicate can arm the border but can never release it.
    A timed release was tried first and failed in the user's hands: the border went
    out while the file was still held over the box. The border is now held by
    **where the cursor is** (`QtGUI.cursorPosition` polled against the target's own
    global rect), with two exits — cursor leaves, or drag ends. Any future drag
    affordance in this codebase inherits that: Qt's drag events cannot track hover
    for you, the cursor can.
  - The **sowing moon's musical laws** were accepted 2026-07-22 (manual block
    G2.11) after three correction rounds. Its old almanac-corner home is now
    historical; on the §9 v3 branch the same moon is a solitary weather body
    beyond bird B. These decisions remain normative:
    1. **The moon's phase is how MUCH is sown, not how hard.** `shake` gained a
       `density` argument (SPEC §5.5, default 0.1 = the historical throw, so the
       code layer and patterns are untouched); `CQGui.sowDensity` now maps the
       phase quadratically (`0.2 × phase²`), so a quarter moon averages ~4
       cables, half ~18 and full ~70. The old weight-ceiling
       mapping was redundant (`patchDepth` is already the how-alive macro) and
       inaudible — every throw rewrote the same ~35 cells, so a thin crescent
       read as "everything went quiet". **The new moon clears the bay** (round
       three): the phase is the field, so an empty field is empty. It was a
       no-op first, to keep the darkest phase from being the most destructive
       act, but in the hand that read as a broken control — and a clear is one
       Ctrl+Z away. Spesal states are left alone either way.
    2. **The pour is horizontal** and the moon fills from its left limb, so the
       terminator travels with the hand (§4.5 again: cue and action share one
       geometry). The tap threshold is `CQBananaGrammar.lassoTravel` — now one
       constant in both places — and its px are **dead**, so a throw cannot
       nudge the phase it throws.
    3. **No presets at night.** The sower constellation lived for one round and
       was cut: a preset is a recipe, not a body, and it was the night's only
       control that could not be used without reading text (§16.4). The
       Inspector's menu is one toolbar click away from either view, and G3 never
       depended on it (the compass stamps its corners from the live matrix).
    `shakeAmount` fans out through `CQGui.onShakeAmount` (default now 0.22, a
    small crescent), and `Cocoquantus.shake` announces `\action, \shake` exactly
    as skip does, which lets the Night moon report shakes from the GUI, key,
    code or patterns. The current §9 body is Ø 56.
    A topology-aware “sweet spot” / ecological shake was explicitly deferred
    on 2026-07-27; do not fold that musical redesign into GUI polish.
  - **Spesal coupling chords** (accepted 2026-07-22, manual block G2.11b): the
    night's inner clock showed a castle's *state* but not
    what the state means — which castle it reads. The chord now draws that on
    the ring's layer, reading `CQGuiCastleConstellation.partnerIndex/trackMode`
    so one state cannot mean two couplings in two skins. Two laws came out of
    the review and were applied to **both** skins:
    **the link runs with the signal** (castle k samples its partner *into*
    itself, SPEC §2.2 — the old arrow pointed at the one being read, which made
    it the only directional mark in the app running against its own flow), and
    **track-and-hold is a broken link, not a doubled one** (it follows its input
    half the clock and freezes the other half, so a dash *is* the mechanism; the
    parallel second line also collided with the arrowhead). Cross-ticks were not
    available — barbs mean inverso polarity on the cables. In the night the
    arrowhead is gone entirely: direction is a **travelling light** on the
    chord, riding the river's own firefly phase (telemetry frames, never a clock
    of its own), because an arrowhead there read as a flowchart. The retired Day
    record used a static arrow. **The Night chord is shown only where
    the hand asks** — nothing at rest, one chord under a hovered castle, five
    under the hovered river — because the coupling is structure, not a quantity,
    and the ring's interior belongs to the iris, the fireflies, the cables and
    the lasso. The chord bows toward the eye (0.4, clamped at 0.35 × ringR) so
    an adjacent pair never doubles the river. Since the 2026-07-27 layer fix,
    both ends run to the castle centres on the ring layer; transparent castle
    paper masks them with the irregular ground-filled flesh, so they emerge at
    the true body edge without a rectangular gap.
  - The night's read head cost four review rounds and produced laws worth keeping:
    **a moving target is not a control** (skip's tap on the comet was replaced by a
    stationary mark under the mouth chevron); **a regime display measures the
    condition, not a quantity the condition destroys** (the first struck mark drew a
    reach that collapses to sub-pixel exactly when the regime is clearest);
    **motion carries only a quantity the eye can integrate** — above ~10 Hz a rate is
    drawn as weight, because the fastest blink a 5 Hz surface can draw is slower than
    the truth; and **ambient motion is driven by the telemetry frames, never by a
    clock of its own**, so a stalled server freezes the mark instead of animating a
    claim about liveness (§9's law, in motion). All four are in `GUI_SPEC.md`.
  - Below the 1280×570 minimum stage the castles' endpoint views already cover their
    inner clocks. Known, out of scope, and the reason the minimum is a floor rather
    than a suggestion — remember it if the night's scale-up is ever done.
- The G1 gate exposed three real defects, all fixed in existing surface: Day's
  two min-sized diagrams absorbed all vertical slack on maximize (now anchored at
  150/176 px with the slack collecting as paper), the cable layer rebuilt bezier
  geometry every frame and paid ~30 `Pen.stroke` calls per inverso cable, and a
  snapshot morph drove the GUI at 30 Hz — twice §10's ambient ceiling.
- GUI redraw law learned the hard way in G1.3, now normative in `GUI_SPEC.md` §10:
  a `Pen` call costs ~53 µs across the language/Qt boundary, so a drawing's price is
  its **call count**, not its math. Notification streams are paced to 15 Hz with a
  leading edge (single acts stay instant, streams thin, the last value is never
  dropped), and past `CQGuiCableOverlay.auraCableLimit` (48) cables stop pulsing and
  keep §16.8's static ink. Never assume where a repaint costs — measure inside the
  real Qt paint event. Two plausible language-side hypotheses were wrong before that.
- Known and deliberately deferred, not bugs to fix on sight: the transparent cable
  overlay re-strokes every cable whenever any control beneath it repaints (Qt damage
  + no clip info in `drawFunc`), which is why a morph costs ~5–6 % against a ~3 %
  resting figure; an offscreen buffer is the real fix and waits until after G2.
  The former static Day memory-path map is no longer deferred: N9 records its death.
- Accepted G1 interaction decisions that stay binding: the banana grammar (audition/WYHIWYG,
  plant/move/rip/harvest, polarity/wheel, solo-light, 32-op undo including GUI SHAKE,
  40 px capture radius, controls-first hit priority, static hover); no `cable opacity`
  control; the 184 px resizable bottom-Drawer was visually **rejected** — the accepted
  replacement is the toolbar `drawer` tab that swaps the central Night stage for a
  stable workbench, with no resize, instrument scroll or second window; Inspector is a
  48×24 px-cell patch loom whose header, route ribbon, grid and footer share one
  1128 px rail; Spellbook is a read-only journal using Ink paper rows; Memory is the
  snapshot bank with truthful `CQSnapshots` morph progress. N9 retired the Day-specific
  compact islands, static Coco memory-path map and live spesal constellation.
- Two memory words, no third (decided 2026-07-21, `GUI_SPEC.md` §8): a **preset** is a
  patch recipe in code, a **snapshot** is saved instrument state on disk. "Seed bank"
  survives only as the Memory page's header metaphor. New snapshots live in
  `snapshots/`; `presets/*.cqsnap.scd` remains a read-only compatibility fallback
  for older personal files (completed 2026-07-28).
- `/cq_aura` law: no telemetry is not the same as zero. `CQGui.auraFor` returns `nil`
  until the server reports, and every drawing surface must keep its static reviewed
  look in that case. Focus, solo-dim and hover never pulse (`GUI_SPEC.md` §4.5).
- Latest full gate on `archipelago` (2026-07-22, G3 first version): **6,611
  unit passes / 0 failures**, `CQ_COMPILE_OK`, `CQ_SAFETY_PASSED`,
  `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
  `CQ_GUI_DRAWER_REENTRY_PASSED` and `CQ_GUI_NIGHT_PASSED` (now including
  the compass water's own assertions and the cache's blit-vs-render
  counters); `CQ_HOLD_OK` stands from the previous gate (no DSP change).
  The CPU soak figures are still the pre-almanac ones: server 19.8–21.5 %
  against SPEC §9's 25 % ceiling, GUI 2.8–3.8 % at rest with 20 planted
  cables, 3–6.8 % with `mayhem`'s 352. The repaint-trigger question is now
  measured (see the G3 bullet above); the user's own soak runs as G2.13.
- Accepted N3 v3.2 gate on `nocturne-nightscape` (2026-07-27):
  `CQ_COMPILE_OK`, **11,269 unit passes / 0 failures / 28/28 classes**,
  `CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
  `CQ_GUI_DRAWER_REENTRY_PASSED` and `CQ_GUI_NIGHT_PASSED`; `CQ_HOLD_OK`
  carries because the increment did not touch DSP.
- Accepted §3 range-ripple v2 gate on `nocturne-range-ripples` (2026-07-27,
  checklist N4):
  `CQ_COMPILE_OK`, **11,365 unit passes / 0 failures / 28/28 classes**,
  `CQ_SAFETY_PASSED`,
  `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
  `CQ_GUI_DRAWER_REENTRY_PASSED` and `CQ_GUI_NIGHT_PASSED`; `CQ_HOLD_OK`
  carries because the increment did not touch DSP.
- Accepted N7 preamp-root gate on `nocturne-preamp-roots` (2026-07-28,
  checklist N7): `CQ_COMPILE_OK`, **12,219 unit passes / 0 failures / 28/28
  classes**, `CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`,
  `CQ_GUI_DRAWER_PASSED`, `CQ_GUI_DRAWER_REENTRY_PASSED` and
  `CQ_GUI_NIGHT_PASSED`; `CQ_HOLD_OK` carries because the accepted increment
  did not touch DSP.
- Accepted N8 Drawer-workshop gate on `nocturne-drawer-workshop` (2026-07-28,
  checklist N8): `CQ_COMPILE_OK`, **13,035 unit passes / 0 failures / 31/31
  classes**, `CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`,
  `CQ_GUI_DRAWER_PASSED`, `CQ_GUI_DRAWER_REENTRY_PASSED` and
  `CQ_GUI_NIGHT_PASSED`; `CQ_HOLD_OK` carries because the accepted increment
  did not touch DSP.
- Accepted N9 Night-only gate on `nocturne-night-only` (2026-07-28,
  checklist N9): `CQ_COMPILE_OK`, **13,033 unit passes / 0 failures /
  32/32 classes**, `CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
  `CQ_GUI_DRAWER_REENTRY_PASSED` and `CQ_GUI_NIGHT_PASSED`. The final safety
  marker and the changed Night overlay's `CQ_PAINT_PROFILE_DONE` are green;
  DSP is unchanged and `CQ_HOLD_OK` carries from N8.
- The four former `GUI_SPEC.md` seams were resolved by the user on 2026-07-19 and are
  recorded in the spec and `PLAN.md`: nine charge channels, the G1 harvest lasso,
  display-only mic sprout and static REC transition/error states. Do not reopen or
  reinterpret them without the user's approval.
- `Cocoquantus.sc` is currently about 645 lines after the lifecycle work. The user
  explicitly accepts that size for now; split it when ownership becomes clearer or
  further growth would make the class harder to maintain, not as a risky handoff-only
  refactor.

## Workflow

- Work in `PLAN.md` order and tick an item only when its relevant automated and manual
  gates pass; do not create competing current-status checklists.
- For every new visible GUI part (for example Day skin, Drawer, Night map, Compass),
  stop after its first runnable and tested version, show it to the user, explain what
  is still provisional, and wait for visual feedback before deeper styling or the
  next visible part. Commit and push no later than each user-approved milestone.
- Small, focused changes; after green tests, update SPEC.md if user-visible behavior
  changed (SPEC and CQSpec must never drift apart).
- Listening iterations: propose a hypothesis → make the change → run tests → give the
  user a 3–5 step listening script → adjust from their description. One variable at a
  time.
- `tests/manual_checklist.md` format: every test = one copy-pasteable fenced
  ```` ```supercollider ```` block, instructions as `//` comments inside it (the user
  runs lines straight from the file).
- Don't refactor the sacred parts (master chain, coco core, clamp funnel) casually;
  if you must, state the invariant impact explicitly first.

## Known pitfalls

- `BufRd` must be instantiated before `BufWr` in `\cqCoco` — the pre-write read is
  load-bearing (lossless hold, correct delay tap). See ARCHITECTURE §4.4.
- `LocalIn`/`InFeedback` one-block delays in the Quantussy ring and coco cross-feed
  are by design; do not "fix".
- `bufFrames` must stay ≤ 2^19 (Phasor float32 integer precision).
- `MIDIClient.init` once, guarded; the instrument must run fully without the 16n and
  without the GUI (headless config).
- Windows: device names in `ServerOptions.device` ("ASIO : ..."); don't assume more
  than 2 in/out channels exist — read config.
- NRT scripts: remember `Score` needs the SynthDefs written into the score
  (`d_recv`) — they run in a fresh scsynth.
- `Collection:includes` compares identity (`===`), not equality: membership
  tables must hold Symbols/Integers/Chars (immediates), never Strings.
- Repo files contain UTF-8 (Finnish text, `±`, `→`): never edit them with PowerShell
  text cmdlets (`Get-Content`/`Set-Content` mangle the encoding) — use the agent's
  file tools or an editor.
- **`.ps1` files must stay pure ASCII.** Windows PowerShell 5.1 reads a script
  without a BOM as ANSI, so a UTF-8 em dash (`E2 80 94`) decodes in CP1252 as
  `â€”` whose `0x94` is a *smart closing quote* — it terminates whatever string it
  lands in and the parse then fails hundreds of lines later, pointing at innocent
  code. Cost one debugging round in `tests/soak_monitor.ps1` on 2026-07-30.
  Comments survive it; strings do not. Verify with
  `grep -n -P '[^\x00-\x7F]' <file>`.
- `CQGuiWidgets.register` is the **CQSpec param** registry: it primes the updater
  immediately with `state.get(name)`, so registering a GUI-only constant there
  paints the widget with `nil` on the very first build (it took down every GUI
  smoke when `shakeAmount` was tried that way). Gesture constants get their own
  watcher door — `CQGui.onShakeAmount` is the pattern.
- `config.scd` must stay a pure data expression (the parenthesized Event) — no
  trailing code; `CQConfig` uses the file's return value, and boot lives in `boot.scd`.
- Qt deadlock: hiding a view under the mouse pointer with a *direct* property write
  freezes the whole window — Qt delivers the hidden child's enter/leave back into the
  language while the language thread is still blocked inside that same write. Swap
  views with queued writes only (`setProperty(\currentIndex, i, false)`, see
  `CQGuiShell.prSwapStage`), after every blocking write, and read the native index
  only on a later GUI tick; `CQGuiShellState` is the synchronous truth. The same law
  binds the coming Night map and Compass. If a GUI smoke hangs, check where the
  physical cursor is before you suspect the code.
