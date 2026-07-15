# CLAUDE.md — CocoquantusSC

SuperCollider emulation of the Ciat-Lonbarde Cocoquantus 2. Read `SPEC.md` first
(behavior, parameter tables, safety — normative), then `ARCHITECTURE.md` (structure,
SynthDef sketches, tests — normative for code shape). Do not invent parameters,
ranges or bus layouts: they are all defined there.

## Environment

- User's machine: Windows, SuperCollider ≥ 3.13 (IDE, `sclang.exe`, `scsynth.exe`),
  audio interface (ASIO), atovproject 16n faderbank (USB MIDI, CC 32–47 ch 1).
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
  no server polling loops.
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

## Workflow

- Work milestone by milestone (SPEC §11); keep a `TODO.md` checklist per milestone
  and tick items only when the relevant tests pass.
- Small, focused changes; after green tests, update SPEC.md if user-visible behavior
  changed (SPEC and CQSpec must never drift apart).
- Listening iterations: propose a hypothesis → make the change → run tests → give the
  user a 3–5 step listening script → adjust from their description. One variable at a
  time.
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
