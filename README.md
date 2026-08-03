# Yönpuoli

[![SuperCollider checks](https://github.com/VaahtoluotoTuomas/yonpuoli/actions/workflows/ci.yml/badge.svg)](https://github.com/VaahtoluotoTuomas/yonpuoli/actions/workflows/ci.yml)

**A nocturnal feedback instrument for SuperCollider.**

*Memory circulates until it becomes weather.*

> **Lineage, not emulation.** Yönpuoli grew from a software study of Peter
> Blasser's Ciat-Lonbarde Cocoquantus. It interprets the dual clocked 8-bit
> memories, five-castle Quantussy network, and part of the original vocabulary,
> but it is not a circuit model, faithful emulation, official port, or affiliated
> Ciat-Lonbarde product.

Yönpuoli treats feedback, aliasing, noise, and cross-modulation as playing
materials. Two clocked delay/loopers circulate sound through an intentionally
coarse converter, while a five-oscillator chaotic network and a signed patching
matrix make the instrument behave less like an effect and more like a small
electronic ecosystem.

The sound engine, Night-only performance surface, hardware-I/O path, and
long-duration resource soak have passed their current acceptance gates. Work is
now focused on final 1.0 packaging: repository privacy and history review,
controller mapping, first-touch playability, release media, and the final merge
and tag. [`PLAN.md`](PLAN.md) is the current status authority.

## What it offers

- Two recirculating 8-bit Coco-inspired delay/loopers: clock-rate repitching,
  feedback above unity, hold, flip, skip, dolby-style articulation, digital
  dust, and square-stack organ voices.
- A five-castle Quantussy-inspired modulation network that moves from slow
  stepped wandering to audio-rate chaos.
- A 16×28 signed routing matrix with binary magnetic planting, held-cable attenuation, moving,
  ripping, harvesting, undo, snapshots, and morphing. Six software-native Coco
  inputs let the Quantussy breathe the organ, feed its own audio into memory,
  and momentarily invert recording/hold without changing the hand-set Hold state.
- The Ink & Phosphor GUI: one playable Night landscape whose shell shores,
  organ calyces, Quantussy tributaries and directional cross-feed bridge open
  drones without a separate mode; a persistent Master strip; and a Drawer for
  precise patching, setup, audio I/O status, history, and memory.
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
  it. Its default layout is in [`docs/16N_MAP.md`](docs/16N_MAP.md).

Yönpuoli uses only SuperCollider's standard UGens; no Quarks or third-party
plugins are required.

## Installation

1. Clone the repository and install SuperCollider.

2. Copy `config.example.scd` to `config.scd`. The copy is ignored by Git and
   holds machine-specific audio, MIDI, and path settings. Device discovery is
   captured only while the local audio server is stopped; the running Audio
   page reads that memory snapshot and never invokes `_ListAudioDevices`. Its
   default Windows view keeps WASAPI/ASIO choices and exact configured
   endpoints concise, while `show all drivers` reveals cached MME, DirectSound
   and WDM-KS alternatives without another scan. If input and output are
   separate Windows devices, use a pair from the same host API:

   ```supercollider
   device: ["Windows WASAPI : Microphone (...)", "Windows WASAPI : Speakers (...)"]
   ```

   Audio-page edits are stored in the ignored sibling `config.audio.scd`; the
   hand-maintained `config.scd` is never rewritten. Apply first explains that
   audio, unrelated synths on `Server.default`, and the unsaved Yönpuoli patch
   will reset. An active recorder blocks the restart. On a later ordinary
   startup, `CQ.reboot`, or the Audio page's clean restart, an absent saved
   device or an unhealthy actual sample rate triggers at most one automatic
   retry with `config.scd`. The recovered route must reach full ready before it
   replaces `config.audio.scd`; the rejected profile is kept as
   `config.audio.scd.bak`. If a fresh scan finds neither configured route, the
   operation stops before launching the audio server instead of silently
   accepting the driver's default device.

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
CQ.shake;                          // grow one audible ecology plus random detail
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

Yönpuoli began as a study of Peter Blasser's Ciat-Lonbarde Cocoquantus,
particularly its pair of clocked 8-bit delays and five-oscillator Quantussy
network. It is not a circuit model or a faithful software emulation. Its Night
interface, signed routing matrix, safety system, file decks, snapshots, compass,
and weather gestures take the instrument in an independent direction.

This independent project is not affiliated with or endorsed by Peter Blasser or
Ciat-Lonbarde. The technical sources, interpretations, and deliberate deviations
are listed in [`SOURCES.md`](SOURCES.md) and [`SPEC.md`](SPEC.md). The lowercase
term `dolby` follows the inherited Cocoquantus vocabulary for Yönpuoli's
gate/duck articulation; it does not identify or implement Dolby Laboratories
technology. Familiar terms such as Coco, Quantussy, castle, Spesal, SP.AF, flip,
skip, and verso/inverso are retained deliberately as an honest map of that
lineage, not as a fidelity claim. The existing `CQ*` / `Cocoquantus` technical
namespace is kept as a stable legacy API.

## Development process and authorship

Yönpuoli was conceived and directed by **Tuomas Vaahtoluoto**, including its sonic
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
| [`docs/PLAYABILITY_GUIDE.md`](docs/PLAYABILITY_GUIDE.md) | Finnish hands-on tutorial: Coco memory, castle/Quantussy, organ, cables, shake, and Workshop dependencies |
| [`docs/CASE_STUDY.md`](docs/CASE_STUDY.md) | Portfolio case study and engineering decisions |
| [`docs/DEVELOPMENT_PROCESS.md`](docs/DEVELOPMENT_PROCESS.md) | AI-assisted workflow and ownership policy |
| [`audit/README.md`](audit/README.md) | Audit authority and reading order |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Contribution and verification workflow |
| [`CHANGELOG.md`](CHANGELOG.md) | Release-level history |

## Known limits

- On the current Windows laptop, SuperCollider's `system default` route can
  start responsively yet advance a requested 48 kHz stream at only about
  2.61 kHz. Yönpuoli now requires three healthy post-boot clock readings before
  building the graph, because the first watcher value can still belong to the
  previous server. The tested split-WASAPI input/output route remains the local
  baseline. An unplugged saved ASIO route is rejected during startup and gets
  the one base-config retry described above. Unplugging a device after a
  session is already ready remains an explicit Audio-page restart: the runtime
  watchdog does not restart audio automatically.
- Main 1/2, independent Coco stems on physical outputs 3/4, raw inputs 1/2 and
  PANIC were verified with a four-output USB interface; NRT covers the shared
  stem safety ceiling.
- Windows is the only currently tested release platform.
- Nocturne is active development, so the final Night-only product surface and
  public-beta media are not released yet.

## License

Yönpuoli is licensed under the
[GNU General Public License v3.0 or later](LICENSE). See
[`CONTRIBUTING.md`](CONTRIBUTING.md) for contribution terms.
