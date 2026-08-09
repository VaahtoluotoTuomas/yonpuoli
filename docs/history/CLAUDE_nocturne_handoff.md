# Yönpuoli — Nocturne-vaiheen Claude-handoff (arkisto)

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

- **Where the work stands (2026-08-08).** Branch `nocturne`. Every Nocturne
  GUI increment (N1–N10), the whole ear pass (§13) and PLAY-1…PLAY-6.1 are
  accepted; so are the DAW bridge, the audio-clock guard, SOAK-1, the Yönpuoli
  identity, the privacy audit and the README. The dense-cable hover/Q-X
  priority fix was committed separately as `732741f`. The 16×28
  Organ/SlotQ/Record expansion and ecological shake is the separate prototype
  milestone `dfee7af`. Its focused Night correction — exact socket paper,
  coherent glyphs and wider ring-to-Coco spacing — plus bundled undo and free
  shake were accepted on 2026-08-03. PLAY-8 is complete.
  The subsequent SlotX/Dust implementation expands the matrix to 16×32 and
  CQSpec to 76 parameters: two bounded SlotX destinations, two independently
  bounded Dust destinations and one new DustGrain hand axis per Coco. Its full
  automated/NRT/GUI gate and rendered visual review are green. Sound, gesture
  and behavior were accepted on 2026-08-03; the repaired final UI was accepted
  in the real application on 2026-08-08. A subsequent explicit musical decision
  admitted those four matrix columns to ecological shake: every row receives
  decoration, `dustGrowth` owns both Dust rows and `crossfeed` both SlotX rows.
  The user explicitly accepted every pre-PLAY-7 listening test on 2026-08-02.
  **The content gate is closed; three release steps remain before 1.0**, in this order, and
  `PLAN.md`'s "Seuraava askel konkreettisesti" is the
  authority on them:
  1. [done] **PLAY-8 ecological-shake ear acceptance.** The user's 2026-08-02 PLAY-7 result
     is **neither A nor B**: `Spaf=0.12` remained very subtle, and weight ceiling
     0.65 produced only the small gate changes predicted by seeds 8101/9202.
     Neither global candidate became the fix. The working tree now has six
     independent destinations — `coco1_organ`,
     `coco2_organ`, `coco1_slotQ`, `coco2_slotQ`, `coco1_record`,
     `coco2_record` — plus a four-family audible skeleton and bounded safe
     musical-control preparation. Organ/SlotQ are continuous VCA contributions;
     Record is a momentary two-way Hold inversion that never mutates manual
     Hold. All six have Inspector columns and anatomical Night sockets/roots/
     telemetry. PLAY-8's first live pass confirmed four distinct blank-RAM
     identities, stable same-seed identity across density and working Organ,
     SlotQ and Record paths. It also found Organ too quiet, dust-growth too
     static, the first socket layout cluttered and the moon drag sticky. Those
     findings are corrected. The user's 2026-08-03 acceptance covers the
     Organ/SlotQ/Record behavior, the final visual correction, bundled undo and
     the free 8–10-shake first-use gate.
     Exact analysis and PLAY-8 seeds: `docs/PLAYABILITY_GUIDE.md` and
     `tests/manual_checklist.md`.
  2. [done] **SLOTX/DUST** — both matrix-fed X lanes, Dust's radial amount and
     transverse mist↔chip grain, Hold closure and the raised/moated morph seeds
     passed. The final Dust spacing, static-speck, root/dolby and tide-eye UI
     repair was accepted on 2026-08-08. Its separately decided shake admission
     is also complete.
  3. [done] **16N-1** — the user accepted the current 16n features on
     2026-08-08 and removed the extra hands-on exercise from the 1.0 quarantine.
     The checklist remains optional regression guidance.
  4. [done] **What ships** — six authored presets (`classic`, `selfNoise`,
     `quantumDrone`, `organMode`, `crossCurrent`, `dustGarden`) and exactly five
     versioned snapshots (`example` plus four complete artistic scenes).
     `empty` is a utility, `mayhem` a stress edge, and private memories stay ignored.
  5. Next: release media → remote CI green → merge to `main` and the 1.0 tag.
  The visual correction has three durable laws. Continuous Coco destinations
  (SP.AF, Input VCA, Feedback VCA, Organ, SlotQ, SlotX, Dust) are hollow; interior glyphs
  are reserved for Flip/Skip/Record acts. Software-native Organ/SlotQ/Record
  views use transparent paper plus a shape-local circular socket mask, never a
  rectangular eraser; Record lamps mirror outward. Complete Coco organisms sit
  30 body px farther outward at the reference stage (114 px ring shore, >60 px
  outer Record margin); no control is smaller. Tests pin all three.
  Deliberately deferred and non-blocking, in order after 1.0: 13.3b (isolated
  Bits 5/8/12 ear test), the three porous playing modes, and the Norns port
  (§14). Ecological shake is no longer in that bucket: its implementation is
  accepted in the current 1.0 candidate.
  SlotX/Dust geometry is now accepted: mirrored SlotX sockets sit above the X
  valves; each inner-lower Dust veil owns radial amount and transverse Grain at
  a 240 body-px reach and 32° angle, with a detached 24 body-px ownership breath
  and 36 static specks. Morph seeds sit at 13/90 % height with a 5 px ground
  moat; the separate tide eye uses 0.94. Grain 0.5 preserves the prior
  filtered-noise + Dust2 formula exactly.
  Inspector group geometry is one 10/10/5/5/2 contract shared by header rules
  and strong seams, so both Coco headings end at Record. Display labels shorten
  only `cocoN_slotX`/`cocoN_dust` to `slotX`/`dust`; canonical names stay stable.
- **Current delivery gate (2026-08-08):** `CQ_COMPILE_OK`, **21,660 unit
  passes / 0 failures / 40/40 classes**, all three GUI smokes,
  `CQ_MATRIX_INPUTS_OK`, `CQ_SHAKE_ECOLOGY_OK`, `CQ_PLAYABILITY_MEASURED`,
  `CQ_HOLD_OK`, `CQ_DUST_OK`, `CQ_SELFNOISE_OK`, `CQ_RELEASE_CONTENT_OK` and
  the full 90 s `CQ_SAFETY_PASSED`. The exact four-scene generator also exits
  with `CQ_RELEASE_SNAPSHOTS_WRITTEN`. Current shake-family RMS is
  0.041385 / 0.089769 / 0.062946 / 0.023180; every pair remains distinct.
  The six release-content branches have post-master RMS 0.008574 / 0.013069 /
  0.005382 / 0.004706 / 0.012155 / 0.006428, with an 8.24 dB snapshot spread.
  Rest/open captures were visually inspected. Active
  SlotX RMS is 0.119051/0.141106 and Dust RMS 0.018479/0.023670; all four held
  cases are exactly silent. The physical unplugged-K-Mix reboot from the prior
  delivery selected split WASAPI, reported about 47,969 Hz and advanced 100
  Night phase frames in three seconds.
- **Latest full DSP green gate (2026-08-02, committed PLAY-8 correction pass):**
  `CQ_COMPILE_OK`, **18,555 unit passes / 0 failures / 39/39 classes**;
  `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`, `CQ_GUI_NIGHT_PASSED`;
  `CQ_MATRIX_INPUTS_OK`, `CQ_SHAKE_ECOLOGY_OK`, `CQ_PLAYABILITY_MEASURED`,
  `CQ_HOLD_OK` and the full 90 s `CQ_SAFETY_PASSED`. The Night capture was
  also rendered and visually inspected at rest, with telemetry and with cables.
  The older gate figures below are historical records of their own increments,
  not the current baseline.
- **Public product identity is Yönpuoli.** Cocoquantus is the explicitly credited
  lineage, not the product name or a fidelity promise. Keep its recognizable
  vocabulary. Public prose and labels spell `dolby` with a lowercase initial.
  Keep the existing `CQ*` / `Cocoquantus` namespace and internal `...Dolby...`
  identifiers; do not start a mass rename.
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
  (checklist N4). Build-order step 5, the dolby oysters (§4), was accepted on
  `nocturne-dolby-oysters` the same day (checklist N5). Build-order step 6,
  the fetching birds (§11), was accepted on `nocturne-birds-fetch` on
  2026-07-28 (checklist N6), build-order step 7, the preamp roots (§10), was
  accepted on `nocturne-preamp-roots` the same day (checklist N7), and
  **build-order step 8, the Drawer workshop (§12.1), was accepted on
  `nocturne-drawer-workshop` on 2026-07-28 (checklist N8).** Its tested
  PLAY-6's drone promotion left, at that milestone, a Night 44 + Master 4 +
  Workshop 26 partition that covered all 74 `CQSpec`
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
  4. The moon is one Ø70 body beyond bird B and opens as a 0.22 crescent.
     Tap = shake, horizontal = continuous phase; sowing density is
     `0.2 × phase³`.
     Vertical down/up = rain/grow and sun/wither after a dominant-axis 8 px
     lock. The next 32 px visibly tension rain 0.6…1.6 Hz and sun 2.5…6 Hz;
     weather glyphs exist only during the hold.
  5. Castle paper and its endpoint detail views are transparent; irregular
     ground-filled flesh masks the ring/chords at their real body edge. The
     old rectangle-repair ring stubs are forbidden.
  6. **Gill polish completed 2026-07-28; reviewed amendment 2026-08-02:** the
     Tab-equivalent lower-centre gill is a deliberate exception to cable-first
     priority. Its exact hit shape owns hover and press, clears cable hover,
     and its cue is drawn on the top hit-transparent gesture layer. The later
     dense-patch review explicitly adds the four exact SlotQ/SlotX bridge-end
     circles for hover and plain left press only; surrounding shell flesh,
     right-click and Alt remain cable-first. Do not generalize beyond these
     five named shapes.
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
  The 2026-08-02 dense-hover review found a presentation corollary: a paint may
  blit one stale image for responsiveness, but the offscreen render that follows
  must call `view.refresh` when it actually creates the replacement. Without
  that, hover remains one cable behind until unrelated Qt damage and looks like
  a centimetre-scale hit-test error even though `patchAt` follows the exact
  Bézier. Night-smoke now samples fine drawn curves in a 512-cable field and
  asserts both render and subsequent presentation.
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
    1. **The moon's phase is how MUCH decoration is sown, not how hard.** `shake`
       has a `density` argument (SPEC §5.5, code default 0.1) and
       `CQGui.sowDensity` maps phase cubically (`0.2 × phase³`). Every
       non-empty throw now also owns a 3–5-cable ecological skeleton; gate rows
       receive only 22% of continuous-row decoration probability. Across the
       full 32-target field, a 1,024-seed probe gives quarter/half/full means
       5.40 / 13.38 / 78.39 total cables (documented as ≈5 / 13 / 78). The old weight-ceiling
       mapping was redundant (`patchDepth` is already the how-alive macro) and
       inaudible — every throw rewrote the same ~35 cells, so a thin crescent
       read as "everything went quiet". **The new moon clears the bay** (round
       three): the phase is the field, so an empty field is empty. It was a
       no-op first, to keep the darkest phase from being the most destructive
       act, but in the hand that read as a broken control — and a clear is one
       Ctrl+Z away. Spesal states are left alone and no parameters are prepared.
    2. **The pour is horizontal** and the moon fills from its left limb, so the
       terminator travels with the hand (§4.5 again: cue and action share one
       geometry). Phase preview follows from the first pixel, including inside
       `CQBananaGrammar.lassoTravel`; a release still classified as a tap first
       restores the exact starting phase and then throws, so a tap cannot leave
       an accidental nudge.
    3. **No presets at night.** The sower constellation lived for one round and
       was cut: a preset is a recipe, not a body, and it was the night's only
       control that could not be used without reading text (§16.4). The
       Inspector's menu is one toolbar click away from either view, and G3 never
       depended on it (the compass stamps its corners from the live matrix).
    `shakeAmount` fans out through `CQGui.onShakeAmount` (default now 0.22, a
    small crescent), and `Cocoquantus.shake` announces `\action, \shake` exactly
    as skip does, which lets the Night moon report shakes from the GUI, key,
    code or patterns. The current §9 body is Ø 70. The ecological redesign was
    deferred on 2026-07-27, reopened by PLAY-7 and implemented on 2026-08-02:
    four seeded audible families, destination-aware weights and an explicit safe
    musical-control preparation bundled with the matrix in GUI undo. PLAY-8 has
    accepted its distinct-family and density-identity laws, and the correction
    pass passed its focused ear/hand retest on 2026-08-03. SlotX/Dust were
    separately admitted on 2026-08-08. Do not dilute it into unrelated
    generic knob randomization.
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
    the hand asks** — nothing at rest, one chord under a hovered inner clock, five
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
- Accepted G1 interaction decisions that stay binding, as amended by PLAY-5:
  the banana grammar (view-only magnetic candidate + exact armed-weight plant,
  atomic move/rip/harvest, polarity/wheel, solo-light, 32-op undo including GUI SHAKE,
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
  recorded in the spec and `PLAN.md`: the charge contract (then nine, now eleven
  with the two Record gates), the G1 harvest lasso,
  display-only mic sprout and static REC transition/error states. Do not reopen or
  reinterpret them without the user's approval.
- **The ReaRoute DAW bridge (road-to-1.0 step 1) works in both directions as of
  2026-07-31** (checklist DAW-1): Yönpuoli opens `ASIO : ReaRoute ASIO (x64)` in
  single-device mode while REAPER keeps K-Mix ASIO and does the monitoring. The
  working recipe, its two REAPER traps (the hardware-output row's own `M`; input
  monitoring, without which an armed track still shows input on its meter but
  passes nothing) and the round-trip-latency debt are in PLAN.md.
- **AUD-W, the runtime audio-clock guard (step 2), is built and its part 1 was
  accepted 2026-07-31** (checklist AUD-W). `CQAudioClockWatch` is the pure model
  and now owns the bound and predicate the boot gate delegates to — one
  definition, guarded by a test. The fault condition is a *stuck* clock
  (unhealthy for 60 s AND not converging), never a mere out-of-bounds reading,
  because a Modern Standby wake measurably travels outside both bounds while
  nothing is broken. The guard adds no server traffic, never lists devices, has
  no automatic recovery, and is silent unless the *visible* truth changes; its
  `watching` state is deliberately invisible. Danger shows on the Audio page and
  as a static mark on the toolbar's drawer button — the route to the only remedy.
  Two review defects are recorded there as laws: **a visible state that can be
  entered but not left is a defect** (the stop path cleared danger silently), and
  a row that already overflows must be split rather than shortened. **AUD-W's
  part 2 was accepted 2026-07-31** in the soak's own phase 4.
- **Step 3, the long soak, is where the road to 1.0 stands, and run 3 is done.**
  Criterion 5's source was found by `tests/memory_probe.ps1` rather than a third
  night: `CQSpellbook`'s journal had no cap, and every committed act appended a
  line per changed cell forever. It is bounded now at `lineLimit` = 20 000 lines
  and the bound was measured holding. **Run 3 (2026-07-31 → 2026-08-01, 12.52 h,
  `tests/tmp/soak/soak-20260731-213509.csv`, 2250 samples) came back eight of nine
  criteria green — 0 status failures, one PID, 0 unhealthy clock samples, CPU
  13.11 % with no rising trend, handles and threads immobile for 12.5 h, 8 synths
  in every sample, and the user's own phase-3 sign-off on criterion 8 (PANIC
  immediate, no morph freeze, GUI at the evening's speed, no dropouts or level
  drift) — but criterion 5 still does not pass as written**: sclang's
  hour-2 mean 274.6 MB → hour-12 mean 305.1 MB (+11.1 % against a 5 % allowance),
  hours 1–12 monotone, Theil–Sen +3.45 MB/h barely decaying. The cap took ~83 %
  of the old ~20 MB/h ramp and the run's own peak (335.1 MB) was in its first
  hour, 40 MB above where it ended — but that is not settling, and the criterion
  asks for settling. Criterion 9 was skipped (AUD-W part 2 accepted 2026-07-31),
  so **criterion 5 is the single thing standing between the soak and its
  acceptance.**
  **The remaining +3.45 MB/h was named on 2026-08-01: it is working-set
  residency recovery after Windows trims, not retained memory.** Run 3 reached
  335.1 MB at 1.03 h, then lost 35.5 MB and 32.0 MB in two steps; the criterion's
  274.6 MB hour-2 baseline therefore sits in the post-trim trough. A clean gated
  `morphGuiFast` reproduction made the distinction direct: private bytes stayed
  at 365.5 MB (Theil-Sen 0.0 MB/h) and the live SC heap moved only +48,064 bytes,
  while working set repopulated 273.0 -> 280.0 MB (+134 MB/h if the short segment
  is naively extrapolated). Its warm-up also lost 56.4 MB of working set in one
  sample while private bytes rose 2.35 MB. The capped unique-random shake stayed
  at 56 entries / 19,768 lines after 480 acts, and production-boot deck probes
  executed 78 measured swaps with stable Buffer cache and heap. Thus deck loading,
  random journal text, morph frames, GUI painting and telemetry are all bounded.
  Criterion 5 still fails **as originally written**, and that failure remains in
  the record. The user approved the documented replacement on 2026-08-01 and
  **SOAK-1 is accepted**: after a 60-minute warm-up, five-minute private-byte means
  must have Theil-Sen <=5 MB/h once at least two post-warm-up hours exist, and the
  final 15-minute mean may exceed the first post-warm-up window by at most
  max(5%, 8 MB). Shorter tooling runs report slope as informational instead of
  extrapolating a high-water segment to an hourly leak. Working set is only a pressure
  bound (first-hour peak + max(25%, 64 MB)); >=5 MB trims are reported separately.
  `soak_monitor.ps1` now records both private bytes and working set, and the gated
  `run_soak_validation.ps1` production harness validates this instrumentation for
  95 minutes. That run passed from `soak-20260801-123248.csv`: private windows
  388.32 -> 391.06 MB, WS peak 320.2 / bound 400.2 MB, 0 status or clock failures,
  and clean gate teardown. Its short slope was +7.23 MB/h; the first summary
  correctly exposed that 35 minutes was too short for a slope gate. Direct GC
  evidence settled ownership: live cells fell 214,430 -> 199,939, free cells rose
  30,486 -> 68,484, and Spellbook stayed 56 entries / 19,768 lines. This is a
  tooling confirmation, not a fourth acceptance night.
- **The user decided 2026-07-31 that five more items are 1.0 conditions**, so the
  road is now nine steps. Name/identity (**Yönpuoli**), the privacy audit
  (`audit/PRIVACY_AUDIT.md`), README rewrite, playability, current **16n map**,
  ecological-shake admission and release-content curation are **done**. The
  16n hands-on block remains optional regression guidance, not a blocker.
  Every pre-PLAY-7 listening test was explicitly accepted (2026-08-02), PLAY-8
  closed on 2026-08-03 and SlotX/Dust closed on 2026-08-08. The exact release
  manifests now expose six authored presets and five snapshots; the four full
  snapshot scenes and two new presets pass a production-SynthDef blank-RAM NRT.
  The remaining order is release media, remote CI, then merge/tag.
  See PLAN.md → "Tie 1.0:aan".
- **Playability tests 1–5 are accepted as of 2026-08-02.** The planted-cable
  first-touch weight is 0.60. Night planting is binary and silent until release:
  every destination inside the 40 px magnetic catch receives the held moon's
  exact weight, distance never attenuates, and wheel while in hand arms 0.05
  steps (Ctrl 0.01) without a 0.60 DSP transient. `Z`/`X` is the laptop
  decrement/increment mirror and Shift makes it fine. The Drawer's precision
  Inspector alone retains `CQAudition`. SP.AF uses the accepted signed-square hand map and
  right-click zero, and the Coco crescents separate stored settings from DSP
  truth. For an audio-rate wake spanning at least 1/16 of the arc, the hollow
  satellite is pinned to the measured midpoint instead of displaying an aliased
  15 Hz sample. A castle's entire visible seeded blob, including the exact
  centre and outer flesh, is one spesal-step/hover surface; its central clock
  names the mechanism. Do not restore a separate flesh-wide hover ring/solo or
  the former `cas` aura: they were found redundant and removed. The centre now has a raw-`tri`
  reed (moving pearl at ≤3 Hz, stable full-reed blur above), while gate/trig
  sockets distinguish amber pre-Schmitt charge from an exact pale post-Schmitt
  fire diamond. `tri` versus `cas` and the organ source/output split passed.
  A later live probe found the organ source's real defect: the weighted
  band-limited stack peaked at only +0.5602 at Speed 24000, so a 0.60 cable
  could not cross a Coco gate/trig's +0.5 Schmitt. The patch source is now
  conditioned by ×1.6 and `clip2(1)` while the audible Organ layer is unchanged.
  Each Coco's own `cocoN/orgN` matrix return is an explicit two-channel
  LocalIn/LocalOut; cross-Coco channels remain separate InFeedback reads.
  NRT pins both `org1 → coco1_spaf` and `org1 → coco1_skip 0.60` (the latter
  changes held-loop RMS 0.1528 → 0.0414), so never remove this as redundant.
  Keep the spiral speed notch as the static stored setting; the SP.AF wake and
  read-head motion show effective modulation.
- **PLAY-6 is accepted, 2026-08-02:** Night is one continuous landscape, never a
  separate drone mode. Each shell owns a vertical-drag Level shoreline, an
  audible-Organ calyx, its own SlotQ tributary gill and its destination-side
  SlotX bridge valve. `coco1SlotX` means Coco 2 → Coco 1 and `coco2SlotX`
  means Coco 1 → Coco 2; the lanes are independent and never linked to SlotQ.
  Each visible castle owns one deferred tap/radial-pull decision: ≤8 px release
  steps spesal, a deliberate inward/outward pull opens/closes `c1Send…c5Send`
  without first rewiring the clock. These 13 controls moved out of Workshop;
  the exact tested ownership at that milestone was Night 44 · Master 4 ·
  Workshop 26; the later Dust/Grain promotion moves the current split to 48/4/24.
- **PLAY-6.1 is accepted (2026-08-02, checklist `PLAY-6.1`)** — the user
  reviewed the corrected version live and signed it off. All four parts landed: SlotQ's anchors are the exact
  `cubicPoint(ringSegments[4]/[0], 0.5)` gap midpoints, SlotX is one two-lane
  arch over castle 1, only Coco 2's Level sweep is sign-reversed, Level ink
  moved to `routeView`, and `undertow` is in both palettes. No DSP, range,
  gesture, ownership or SlotX-direction change. Gate: `CQ_COMPILE_OK`,
  **14,827 unit passes / 0 failures / 39/39 classes**, all four GUI markers and
  `CQ_NIGHT_CAPTURE_DONE`; `CQ_SAFETY_PASSED` / `CQ_HOLD_OK` carry.
  **Two clauses of the hand-off were defects and were measured out**, both the
  N7 law again — *a constant cannot clear a body it did not measure*:
  1. The prescribed q anchor drew the right stream **straight through castle 2's
     `tri` jack** (4.86 px from its centre at 1280×570, 7.28 at 1920×855,
     against ~5.8 px of drawn jack). The `-10 px` lift is now a minimum and the
     geometry deepens the bow until every drawn endpoint clears by
     `flowBodyClear` = 8 px.
  2. Both x lanes **grazed the birds' fixed 200 px name plates** (0.02 px at the
     minimum stage) because the valves sit directly under them. The gap
     x-coordinates are now nominal shoulders that draw inward as measured, and
     the crown is taken from **castle 1's own endpoint boxes**, not its
     transparent 146 px paper — clearing the paper would fly the arch so high it
     came down through the plates. The transparent castle papers are explicitly
     not bodies here; the river already runs through them. The clearance belongs
     to the arch's lowest ink, the outbound lane.
  The birds' 6 px **drop pad** still cannot be cleared at the minimum stage
  (1.0 px) without either splitting castle 1 or turning the arch gothic; it is
  drawn only during a file drag. Solving costs ~21 ms per layout at the minimum
  stage (6 ms when no give-way is needed).
  **The first visual review (2026-08-02) found five things, all fixed:**
  1. **Every `StackLayout` sibling above the container needs its own repaint
     after a layout.** `routeView`, `tideGestureView` and the cable overlay are
     siblings, so Qt resizes and repaints them independently of the container
     whose `onResize` recomputes the geometry — `prLayout` refreshed only the
     container and the gesture layer. One bug, two faces: the drone streams
     were invisible until first hovered after boot, and every line (including
     old patch cables) stood in its old place for seconds after a maximize.
     All three are repainted in `prLayout` now, and `CQGuiCableOverlay.relayout`
     drops the caches keyed on stage extent and endpoint positions. The night
     smoke compares the route layer's last painted stage size against the
     current one; with the fix removed it reports `painted nil`.
  2. **A jack's ink is its whole family mark, not its dot.** Measuring only
     `jackDotRim` passed the right q stream 14.8 px from castle 2's `tri`
     centre while running through that jack's *leaf* (reach 15 px, measured
     −0.2 px — touching), which made the output read as joined to the stream.
     Clearance now uses `endpointInkReach`, and both q valves were raised
     together 28° → 40°; that also let the q bow relax 28 → 14 px.
  3. **Hover answers on the whole control, not on its empty reach.** Level's
     shoreline brightened only the track, so a hand below the pearl lit the
     water above it. Fixed on Level and on the Organ calyx (same defect).
     Brightness is hover; weight stays press.
  4. **A separator nobody can find separates nothing.** The undertow's
     hand-off values (2 px, α 0.14) were invisible; it is `state` width at
     α 0.30 / 0.58 now.
  5. Known and left for the user's judgement: the undertow tints the Organ
     calyx's dim resting arcs olive. Removing it there is one line.
- **The 16n map is now two identical Coco channel strips plus a shared section**
  (SPEC §6.1, normative): F1–F5 `coco1` In·Speed·Spaf·Fb·**Level**, F6–F10
  `coco2` the same, F11–F16 `qFreq`·`qChaos`·`preADrive+preBDrive`·
  **`coco1SlotQ+coco2SlotQ`**·`patchDepth`·`masterVol`. Level ends each strip
  like a mixer's channel fader and the two Levels are deliberately **not**
  linked — balancing the voices against each other is their whole purpose. The
  deck faders left because `deckALevel`/`deckBLevel` already have Night crescents
  and `deckARate`/`deckBRate` are matrix destinations. The rule that decided it
  is normative: **a fader is worth most where no other hand reaches** — Night
  now plays 48 params, Master 4 and the matrix 32 destinations. Level, Organ,
  SlotQ, directional SlotX and Dust/DustGrain have Night bodies; Pan and the
  pre/deck setup slots retain exact Workshop rows. F14 is measured, not taste: `qMix` has exactly one writer and one
  reader (`cocoNSlotQ`, default 0), so without a hand there F11/F12 shape a voice
  that never reaches the output. Do not "restore" the deck faders or link the
  two Levels without the user's decision.
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
- `LocalIn`/`InFeedback` one-block delays in the Quantussy ring, each Coco's own
  `cocoN/orgN` matrix-source return, and Coco cross-feed are by design; do not
  "fix".
- `bufFrames` must stay ≤ 2^19 (Phasor float32 integer precision).
- `MIDIClient.init` once, guarded; the instrument must run fully without the 16n and
  without the GUI (headless config).
- Windows: device names in `ServerOptions.device` ("ASIO : ..."); don't assume more
  than 2 in/out channels exist — read config.
- **Windows device discovery needs both a stopped server and a fresh `sclang`.**
  `ServerOptions.inDevices`/`outDevices` → `_ListAudioDevices` → PortAudio: the
  main interpreter can retain its old list after endpoint hot-plug or driver install,
  while enumerating there with scsynth running can wedge an owned WASAPI endpoint.
  Measured on ReaRoute 2026-07-30: registry and REAPER were correct, a standalone
  sclang saw 20 in / 27 out, and the IDE session stayed one device short per
  direction through several server restarts. Production discovery therefore goes
  only through `CQAudioDeviceProbe`: after lifecycle has stopped scsynth, its
  PowerShell watchdog starts a fresh isolated daemon sclang for at most 20 seconds,
  suppresses user startup files and returns exact data through a private temp file.
  Never replace this with a live main-process call. The Audio page's confirmed
  `find connected devices` action and `CQ.reboot` both enter that boundary;
  `CQ.free` alone intentionally does not stop `Server.default`.
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
- **`powershell -File s.ps1 -Names a,b,c` passes ONE string, not an array** —
  `-File` does no PowerShell parsing of its arguments. Measured 2026-07-31 on
  `tests/memory_probe.ps1`: the whole scenario list arrived as a single unknown
  name, so the run did nothing at all and reported itself `ok`. Split comma lists
  inside the script, and make an unknown name refuse to print its success marker.
  **A flat line that measured nothing looks exactly like a flat line that measured
  something** — same law as the `run_all.scd` marker that could lie.
- **Windows trims a process's working set in one downward step**, and a
  least-squares fit reads that step as a ramp: the memory probe reported
  −939.6 MB/h for a process that had been flat within 1.8 MB for 400 s. Any
  growth measurement over a Windows working set needs a robust slope (the probe
  uses Theil–Sen) *and* must report the drops separately instead of averaging
  them into the answer.
- **A bound is verified by exceeding it, not by running the normal workload.**
  The first verification of `CQSpellbook.lineLimit` re-ran the ordinary morph
  cadence and measured the same ramp as before the fix — correctly, because
  ~57 dense acts are needed to reach the cap and the window held 53. The probe's
  `commitGuiFast` exists to pass the limit during its warm-up.
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
