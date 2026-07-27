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

1. `"<sclang.exe>" -i none tests/compile_check.scd` → must print `CQ_COMPILE_OK`
2. `"<sclang.exe>" -i none tests/run_all.scd` → must print `CQ_TESTS_PASSED`
3. `"<sclang.exe>" -i none tests/nrt_safety.scd` → must print `CQ_SAFETY_PASSED`
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
  `main` (PR #1, tag `archipelago-1.0`). **The active branch is now `nocturne`**
  and the active roadmap is `docs/NIGHT_STANDALONE_PLAN.md` (§12 build order),
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
- **Build-order step 3, the almanac becomes landscape (§9), is built and green
  but NOT accepted (2026-07-27). It is not on `nocturne`.** `nocturne` stands at
  `a5e4b17` (the flood) and the §9 work lives on branch `nocturne-nightscape`;
  the user is thinking it over and said so plainly ("en ole ihan varma tästä,
  tämä ei oikein toimi"). Review = checklist **N3**. Do not merge it, do not
  build step 4 on top of it, and do not treat its `GUI_SPEC.md` block as
  accepted surface — that block carries its own provisional status note, and if
  N3 is rejected the block is deleted rather than amended. If work continues
  while the user thinks, prefer §13's ear pass, which touches none of this.
  What the two attempts taught, all of it now in the plan:
  1. **Version 1 was built exactly as `NIGHT_STANDALONE_PLAN.md` said and was
     rejected on sight.** The plan text was wrong, not the implementation: §7.2
     said "the fix is not to move them somewhere else … break the axis", which
     keeps all three parts in one reserved patch of right-hand sky. **A plan
     paragraph can be the defect.** §7.2 and §9 are now corrected in place with
     the old text kept as a warning; read the corrected §9 before touching this.
  2. The real fault was **grouping by function instead of by body** — a shake
     control, a matrix blend and three matrix ops sitting together touch no body
     around them, which is what a toolbar is, and re-composing a toolbar leaves
     a toolbar. Version 2 breaks the cluster: moon + rain + sun to the sky at
     the birds' altitude (they act on the *live bay*), pool + graft to the bay's
     bottom water tangent to the lower edge (the graft takes its parents as seed
     taps, so it may not live a bay away from them), right corner left as empty
     water. The tide's math survives via an elliptical disc↔square mapping whose
     inverse singularity is snapped at the source (`cornerSnap`), so a stamped
     corner still recalls bit-exact.
  3. Two costs, both measured, neither hidden — and the first was **my wrong
     claim, corrected**: the move buys *belonging, not size* (18 535 px² vs the
     G3 pad's 19 600 — a Qt layer is a **rectangle**, so free-*disc* comparisons
     overstate room), and the pool now lies under the cable field, so §4.5 gives
     cables the press on its bay-facing seeds (17 % / 35 % of random 35-cable
     bays, 0 % for the landward seeds and the whole sky). That last one is an
     **open user decision** in N3 — accept, a local "inside the shore the pool
     wins" exception, or move the pool back to free water. **Do not settle it in
     code.**
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
  - The Night toolbar is the **almanac corner** in the top-right sky
    (`GUI_SPEC.md` §6/§6.1/§16.6): the **sowing moon**, and beneath it G3's
    reserved **empty water** — pinned by clearance tests, never drawn, because
    a dead control breaks §4.5 and §16.12 at once. **Accepted 2026-07-22
    (manual block G2.11)** after three correction rounds, whose decisions are
    all normative now:
    1. **The moon's phase is how MUCH is sown, not how hard.** `shake` gained a
       `density` argument (SPEC §5.5, default 0.1 = the historical throw, so the
       code layer and patterns are untouched); `CQGui.sowDensity` maps the phase
       linearly onto it, so the **half moon is exactly the v1.0 handful** (10 %
       ≈ 35 cables) and the full moon is twice that. The old weight-ceiling
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
    `shakeAmount` fans out through `CQGui.onShakeAmount` (default now 0.5 = the
    half moon), so the Day knob and the moon are two views of one number, and
    `Cocoquantus.shake` announces `\action, \shake` exactly as skip does, which
    is what lets one light serve three layers. The moon is Ø 52 after round two.
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
    of its own), because an arrowhead there read as a flowchart. Day keeps a
    static arrow — paper is a calm tool and grows no ambient motion. **It is shown only where
    the hand asks** — nothing at rest, one chord under a hovered castle, five
    under the hovered river — because the coupling is structure, not a quantity,
    and the ring's interior belongs to the iris, the fireflies, the cables and
    the lasso. The chord bows toward the eye (0.4, clamped at 0.35 × ringR) so
    an adjacent pair never doubles the river, and both ends stop at the castle
    boxes via `CQNightGeometry.boxExit` — half a square box's *width* along a
    diagonal ray is still inside the box, which the clearance test caught.
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
  resting figure; an offscreen buffer is the real fix and waits until after G2. The
  static Day memory-path map's visual weight versus its information value is parked
  in the same place. Both are recorded in `PLAN.md` "Myöhemmin".
- Accepted G1 decisions that stay binding: the banana grammar (audition/WYHIWYG,
  plant/move/rip/harvest, polarity/wheel, solo-light, 32-op undo including GUI SHAKE,
  40 px capture radius, controls-first hit priority, static hover); no `cable opacity`
  control; the 184 px resizable bottom-Drawer was visually **rejected** — the accepted
  replacement is the toolbar `drawer` tab that swaps the central Day/Night stage for a
  stable workbench, with no resize, instrument scroll or second window; Inspector is a
  48×24 px-cell patch loom whose header, route ribbon, grid and footer share one
  1128 px rail; Spellbook is a read-only journal using Ink paper rows; Memory is the
  snapshot bank with truthful `CQSnapshots` morph progress. Day keeps compact islands,
  26/34/44 px knobs, the static Coco memory-path map and the live spesal constellation.
- Two memory words, no third (decided 2026-07-21, `GUI_SPEC.md` §8): a **preset** is a
  patch recipe in code, a **snapshot** is saved instrument state on disk. "Seed bank"
  survives only as the Memory page's header metaphor. Splitting snapshots out of
  `presets/` into their own folder is recorded in `PLAN.md` as later work.
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
