# Yölahti

[![SuperCollider checks](https://github.com/VaahtoluotoTuomas/sc-cocoquantus/actions/workflows/ci.yml/badge.svg)](https://github.com/VaahtoluotoTuomas/sc-cocoquantus/actions/workflows/ci.yml)

**An experimental feedback instrument for SuperCollider, built around unstable
8-bit memory, chaotic modulation, and a playable night-map interface.**

Yölahti treats feedback, aliasing, noise, and cross-modulation as playing
materials. Two clocked delay/loopers circulate sound through an intentionally
coarse converter, while a five-oscillator chaotic network and a signed patching
matrix make the instrument behave less like an effect and more like a small
electronic ecosystem.

The instrument is currently developed and tested on Windows. The v1.0 sound
engine and the complete Archipelago GUI are implemented, audited, and accepted.
The active `nocturne` phase has made the Night surface the whole instrument:
Night, the persistent Master strip and the Drawer now cover every parameter
without maintaining a second performance interface. The Night-only instrument
passed its live review on 2026-07-28; see [`PLAN.md`](PLAN.md) for current work
rather than reading historical checklists as open tasks. The accepted Audio
page now includes the AIO-3 transaction: it edits and validates an isolated
draft, confirms the disruptive `Server.default` restart inline, commits the
generated sibling profile only after a complete candidate boot, and restores
the previous working Audio config once if that candidate fails. AIO-4 adds raw
pre-gain live/mic presence and shared post-safety master presence without
painting a hidden Audio page; its isolated physical recheck and the wider
hardware-I/O review remain the next gates. The earlier WASAPI hang was isolated
to live device enumeration, which is no longer performed while the server is
running. The Audio restart gate also remains available with a clean draft: it
reports an unresponsive engine explicitly and can rebuild the exact active
configuration without writing a profile.

## What it offers

- Two recirculating 8-bit Coco-inspired delay/loopers: clock-rate repitching,
  feedback above unity, hold, flip, skip, Dolby-style articulation, digital
  dust, and square-stack organ voices.
- A five-castle Quantussy-inspired modulation network that moves from slow
  stepped wandering to audio-rate chaos.
- A 16×22 signed routing matrix with cable-like audition, planting, moving,
  ripping, harvesting, undo, snapshots, and morphing.
- The Ink & Phosphor GUI: one playable Night landscape, a persistent Master
  strip, and a Drawer for precise patching, tuning, audio I/O status, history,
  and memory.
- File decks, live inputs, optional 16n faderbank control, recording, optional
  stems, snapshots, and morphing.
- A fixed output-safety chain and offline render tests designed for unpredictable
  feedback patches.

## Requirements

- Windows 10/11 (tested). Other platforms may work but are not yet release-tested.
- [SuperCollider](https://supercollider.github.io/) 3.13 or newer (3.14.1 is the
  current recommended release).
- Headphones or an audio interface. Start at a low monitoring level.
- Optional: an atovproject 16n faderbank. The instrument is fully usable without
  it.

Yölahti uses only SuperCollider's standard UGens; no Quarks or third-party
plugins are required.

## Installation

1. Clone the repository and install SuperCollider.

2. Copy `config.example.scd` to `config.scd`. The copy is ignored by Git and
   holds machine-specific audio, MIDI, and path settings. During the current
   Windows stability gate, the running Audio page exposes only the exact
   configured endpoint(s): live `_ListAudioDevices` discovery was withdrawn
   because it reproducibly wedged an already-running WASAPI server. If input
   and output are separate Windows devices, use a pair from the same host API:

   ```supercollider
   device: ["Windows WASAPI : Microphone (...)", "Windows WASAPI : Speakers (...)"]
   ```

   Audio-page edits are stored in the ignored sibling `config.audio.scd`; the
   hand-maintained `config.scd` is never rewritten. Apply first explains that
   audio, unrelated synths on `Server.default`, and the unsaved Yölahti patch
   will reset. An active recorder blocks the restart.

3. In the SuperCollider IDE, open **Edit → Preferences → Interpreter → Include
   paths** and add `<repo>/classes`. Recompile the class library with
   **Ctrl+Shift+L**. `CQSpec.specs.size` should return `74`. Developers running the
   unit suite also add `<repo>/tests/classes`.

4. Put a few `.wav`, `.aiff`, or `.flac` files in the ignored `samples/`
   directory. Field recordings and modular material work especially well.

5. Evaluate `boot.scd`, or run `CQ.boot`. Boot is deliberately silent while the
   graph is verified, followed by a two-second fade-in.

## Five-minute start

```supercollider
CQ.boot;
CQ.deckA.load("my_recording.wav"); // use forward slashes in Windows paths
CQ.set(\masterDry, 0.3);
CQ.hold(1, 0);                     // record the deck into Coco 1
CQ.set(\coco1Fb, 1.0);            // ~0.9 echo, 1.0 loop, >1 blooming crunch
CQ.set(\coco1Speed, 12000);        // clock-rate repitch, roughly an octave down
CQ.patchPreset(\classic);
CQ.shake;                          // sow a random handful of routes
CQ.rec; CQ.recStop;                // recordings/cq_YYMMDD_HHMMSS.wav
CQ.panic;                          // emergency mute; CQ.resume to return
```

GUI shortcuts: `1/2` hold · `q/w` flip · `a/s` skip · `3..7` spesal ·
`r` record · `space` panic · `p` shake. More live-coding examples are in
[`live.scd`](live.scd).

## Architecture and safety

Every control surface writes through the same validated state funnel:

```text
16n ──┐
GUI ──┼──> CQState / CQMatrix ──> CQSpec clamp ──> scsynth graph
code ─┘              │                            │
snapshots ───────────┘                            └──> state notifications ──> GUI
```

The server graph stays static while playing. Parameter definitions live in one
registry, async deck and lifecycle work use cancellable transactions, and GUI
layers remain clients of the state rather than alternate control paths. The
detailed design is documented in [`ARCHITECTURE.md`](ARCHITECTURE.md).

Every hardware output passes through a non-bypassable chain:

```text
LeakDC -> bounded tanh headroom -> Limiter(0.89) -> bad-value squelch
```

The looper's feedback is clipped or wrapped *before* re-quantization inside the
recirculation loop. This keeps feedback above unity a sound-design gesture rather
than an unbounded level path. Microphone gain defaults to zero, boot fails toward
silence, and `CQ.panic` is always available.

## Testing

The repository uses several different kinds of evidence rather than treating one
large test count as a release claim:

- serverless unit tests for parameter, routing, transaction, lifecycle, geometry,
  and GUI contracts;
- non-realtime renders of the real SynthDefs for peak, DC, NaN, feedback, and
  hold-integrity checks;
- GUI interaction smokes and measured paint-performance tests;
- hands-on listening, visual, mouse, and controller reviews;
- a separate audit that produced reproducible safety and lifecycle findings before
  remediation.

Headless developer gates:

```text
powershell -File tests/run_sclang_gate.ps1 tests/compile_check.scd      -> CQ_COMPILE_OK
powershell -File tests/run_sclang_gate.ps1 tests/run_all.scd            -> CQ_TESTS_PASSED
powershell -File tests/run_sclang_gate.ps1 tests/nrt_safety.scd         -> CQ_SAFETY_PASSED
powershell -File tests/run_sclang_gate.ps1 tests/nrt_hold_integrity.scd -> CQ_HOLD_OK
powershell -File tests/run_sclang_gate.ps1 tests/nrt_dust_character.scd -> CQ_DUST_OK
powershell -File tests/run_sclang_gate.ps1 tests/nrt_selfnoise_character.scd -> CQ_SELFNOISE_OK
powershell -File tests/run_sclang_gate.ps1 tests/gui_audio_setup_smoke.scd -> CQ_GUI_AUDIO_PASSED
```

Focused measurement/listening gates (they render reproducible evidence; the
human verdict is recorded separately in `tests/manual_checklist.md`):

```text
powershell -File tests/run_sclang_gate.ps1 tests/nrt_zipper_measure.scd  -> CQ_ZIPPER_MEASURED
powershell -File tests/run_sclang_gate.ps1 tests/nrt_preamp_loudness.scd -> CQ_PREAMP_OK
```

The wrapper owns the headless process, timeout and success marker; its default
test config includes `classes/` and `tests/classes/`. The CI workflow generates
its own isolated version. Current
hands-on checks are in [`tests/manual_checklist.md`](tests/manual_checklist.md);
accepted historical evidence is archived under [`docs/history/`](docs/history/).

## Lineage and acknowledgements

Yölahti began as a study of Peter Blasser's Ciat-Lonbarde Cocoquantus,
particularly its pair of clocked 8-bit delays and five-oscillator Quantussy
network. It is not a circuit model or a faithful software emulation. Its Night
interface, signed routing matrix, safety system, file decks, snapshots, compass,
and weather gestures take the instrument in an independent direction.

This independent project is not affiliated with or endorsed by Peter Blasser or
Ciat-Lonbarde. The technical sources, interpretations, and deliberate deviations
are listed in [`SOURCES.md`](SOURCES.md) and [`SPEC.md`](SPEC.md). Cocoquantus and
Ciat-Lonbarde are referenced only to identify the instrument's lineage.

## Development process and authorship

Yölahti was conceived and directed by **Tuomas Vaahtoluoto**, including its sonic
and visual design, requirements, acceptance criteria, and hands-on testing.
[Claude Code](https://claude.com/claude-code) and
[OpenAI Codex](https://openai.com/codex/) have been used extensively as coding
agents for implementation, documentation, tests, and audits. Tuomas made the
final product decisions and accepted changes through automated gates, targeted
reproduction tests, and iterative listening and interaction reviews. This is a
human-directed, AI-assisted project; there is no claim that every source line was
manually typed by its maintainer.

The working method, its limitations, and the current technical-ownership pass are
described in [`docs/DEVELOPMENT_PROCESS.md`](docs/DEVELOPMENT_PROCESS.md). A
portfolio-oriented account of the project is in
[`docs/CASE_STUDY.md`](docs/CASE_STUDY.md).

## Documentation

| Document | Purpose |
|---|---|
| [`SPEC.md`](SPEC.md) | Normative behavior, parameters, DSP, and safety |
| [`GUI_SPEC.md`](GUI_SPEC.md) | Normative interactions and visual language |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Code structure, ownership, and test architecture |
| [`PLAN.md`](PLAN.md) | Current Nocturne work order and status |
| [`SOURCES.md`](SOURCES.md) | Technical lineage, sources, and interpretations |
| [`docs/CASE_STUDY.md`](docs/CASE_STUDY.md) | Portfolio case study and engineering decisions |
| [`docs/DEVELOPMENT_PROCESS.md`](docs/DEVELOPMENT_PROCESS.md) | AI-assisted workflow and ownership policy |
| [`audit/README.md`](audit/README.md) | Audit authority and reading order |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution and verification workflow |
| [`CHANGELOG.md`](CHANGELOG.md) | Release-level history |

## Known limits

- Live stem routing still needs final verification on an interface with at least
  four hardware outputs; NRT stem safety is covered.
- Windows is the only currently tested release platform.
- Nocturne is active development, so the final Night-only product surface and
  public-beta media are not released yet.

## License

Yölahti is licensed under the
[GNU General Public License v3.0 or later](LICENSE). See
[`CONTRIBUTING.md`](CONTRIBUTING.md) for contribution terms.
