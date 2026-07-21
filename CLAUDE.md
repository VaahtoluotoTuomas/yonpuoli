# CLAUDE.md — CocoquantusSC

SuperCollider emulation of the Ciat-Lonbarde Cocoquantus 2. Read `PLAN.md` for the
current handoff, then `SPEC.md` (behavior, parameter tables, safety — normative) and
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
- **G1 is complete and accepted, §13 end gate included (2026-07-21, `295d14d`).**
- **G2's whole built night surface is accepted and its §13 gate passed (2026-07-22).
  G2 itself is NOT signed off**: §13's G2 scope still contains one unbuilt part, the
  **Night toolbar** (§6). That is the next step. **Do not rebuild or "improve"
  accepted G1 or G2 surface on your own initiative.** `PLAN.md` holds the detail.
  - §16.7's dashed drop-target border landed 2026-07-22 for both skins from one
    `CQInk.drawDropTarget`. Qt hands sclang **no drag-leave callback** — only the
    `canReceiveDrag` predicate and the drop — so the border is armed by the predicate
    and released by a watchdog each further call re-arms (`CQGui.dropTargetHold`).
    Whether Qt polls the predicate per drag-move or only per enter is **unmeasured**;
    both worlds are tolerable by design. Do not "fix" this without a real file drag.
  - The Night toolbar was deliberately left to simmer by the user (2026-07-22). A
    discussion of its placement and look was held and **deliberately not recorded** —
    decide it again with the user when the toolbar actually starts.
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
- Latest full gate on `archipelago` (2026-07-22): 4,947 unit passes / 0 failures,
  `CQ_COMPILE_OK`, `CQ_SAFETY_PASSED`, `CQ_HOLD_OK`, `CQ_GUI_SHELL_PASSED`,
  `CQ_GUI_DRAWER_PASSED`, `CQ_GUI_DRAWER_REENTRY_PASSED`, `CQ_GUI_NIGHT_PASSED`.
  Measured on the user's machine: server 19.8–21.5 % against SPEC §9's 25 % ceiling,
  GUI 2.8–3.8 % at rest with 20 planted cables, and 3–6.8 % with `mayhem`'s 352
  cables — seven times `auraCableLimit`, so inside the documented band rather than a
  regression. Which repaint actually triggers that has **not** been measured in the
  real Qt paint event; do it with the offscreen buffer, do not guess (G1.3's lesson).
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
