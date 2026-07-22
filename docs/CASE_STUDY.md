# Yölahti — portfolio case study

## The project

Yölahti is a standalone experimental feedback instrument built in SuperCollider.
It combines two deliberately low-fidelity delay/loopers, a five-oscillator chaotic
modulation network, a signed routing matrix, file and live inputs, recording, and
an original graphical playing surface.

The project began as a way to study and experience ideas from Peter Blasser's
Ciat-Lonbarde Cocoquantus without owning the physical instrument. It gradually
became a separate software instrument. The current Night surface no longer copies
the hardware panel: it turns the signal topology into a landscape of shells,
castles, birds, roots, tides, and weather gestures.

## My role

I conceived the project and directed its sonic and visual design. I defined the
requirements and acceptance criteria, made the final product decisions, played
every release candidate, and conducted the listening, visual, mouse, and hardware
controller reviews.

Claude Code and OpenAI Codex were used extensively as coding agents. They proposed
and implemented much of the source code, documentation, and automated tests. I do
not present the repository as code that I manually typed alone. My contribution is
the combination of product direction, sound and interaction design, specification,
agent orchestration, acceptance testing, and ongoing technical ownership. The
working agreement is documented in
[`DEVELOPMENT_PROCESS.md`](DEVELOPMENT_PROCESS.md).

## Constraints that shaped the design

- **Feedback must remain playable.** Signals may become noisy, aliased, saturated,
  and chaotic, but no patch may create an unbounded hardware-output path.
- **The converter is part of the instrument.** Clipping and quantization happen
  inside the feedback loop, so every repeat can acquire more digital wear.
- **Code, MIDI, GUI, snapshots, and generative gestures must agree.** Every write
  uses the same parameter or routing funnel and the same limits.
- **The interface must show real state.** Ambient motion is derived from server
  telemetry; decorative animation cannot imply activity that is not happening.
- **The system must remain usable without optional hardware.** GUI, 16n, file
  decks, microphone, and stems degrade independently.
- **Listening is part of verification.** A headless test cannot decide whether a
  feedback taper, chaos range, gesture, or visual hierarchy is musically useful.

## Architecture

The language side and audio server have deliberately different responsibilities:

```text
controllers and GUI
        │
        v
CQState / CQMatrix ----> CQSpec validation
        │
        v
fixed scsynth graph ----> output safety chain
        │
        └--------------> state and telemetry notifications ----> GUI
```

Three decisions are especially important:

1. **One state funnel.** All parameter paths clamp through `CQSpec`; matrix writes
   have an equivalent transaction funnel. A new UI gesture cannot silently invent
   a second interpretation of a parameter.
2. **A static playing graph.** Repatching changes control-bus weights rather than
   rebuilding synth nodes. Async deck swaps and recording are explicit,
   cancellable transactions.
3. **Fail toward silence.** Boot, teardown, malformed state, server loss, and bad
   sample values converge on silence rather than an unlimited output.

The detailed structure is in [`../ARCHITECTURE.md`](../ARCHITECTURE.md).

## A design decision: feedback above unity

Ordinary audio software often treats feedback above 1.0 as an error. Here it is an
important playing region: echoes become loops and loops become dense, quantized
crunch. The design makes that region available while bounding it structurally.

The recirculating signal is clipped or wrapped before every quantization pass. A
separate, non-bypassable output chain removes DC, supplies bounded headroom,
limits peaks, and squelches NaN/invalid values. Offline renders exercise the real
SynthDefs under worst-case patches and inspect the resulting audio.

## An engineering decision: test the risks, not just the functions

The test suite includes ordinary unit tests, but the release gates are organized
around failure modes:

- malformed parameter and snapshot values;
- overlapping or invalid output configurations;
- stale async deck loads and linked-stereo partial failure;
- rapid recorder operations and session teardown races;
- lossless hold and unity-feedback energy;
- NaN, DC, and peak safety under worst-case DSP;
- GUI gesture cancellation, hit priority, geometry, and repaint cost.

The independent audit was valuable because it challenged the tests themselves. It
found a release-blocking output-configuration safety hole and several lifecycle
risks despite the existing green suite. The findings were reproduced before the
remediation plan was accepted. That experience changed the project rule from
“many tests pass” to “the important oracle is demonstrated and known failures make
it turn red.” The preserved reports are indexed in [`../audit/README.md`](../audit/README.md).

## Human evaluation

Each visible or audible increment stops at a runnable version. I then test it with
the actual window, mouse, ears, samples, and controller before the next increment
begins. Reviews have rejected plausible implementations: a resizable drawer,
decorative animation, a preset constellation that required reading labels, and
several visually attractive indicators that represented the wrong signal fact.

The full historical checklist records these decisions rather than presenting them
as automated proof. The current checklist is intentionally shorter and contains
only live regressions and the active Nocturne gates.

## Current result

- v1.0 audio engine and code/MIDI control layer complete;
- Archipelago Day, Night, Drawer, compass, tide, and weather surfaces accepted;
- 28 UnitTest classes and 209 test methods, currently reporting 6,611 passing
  assertions on the reference environment;
- non-realtime safety and hold-integrity gates;
- measured server and GUI performance within the project's budgets on the
  reference laptop;
- active Nocturne phase focused on making Night the only necessary playing surface.

The main outstanding hardware-specific acceptance item is live verification of
separate stems on an interface with at least four outputs.

## What I learned

- A detailed specification is useful only when implementation, tests, and current
  status can be traced back to it.
- AI-generated tests can share the implementation's mistaken assumptions; an
  adversarial reproducer and mutation check are stronger than a pass count.
- GUI performance must be measured inside the real paint event. Several intuitive
  optimizations were slower or deadlocked Qt.
- A musical interface is not accepted when it merely works. The cue, gesture,
  audible response, and displayed state must describe the same event.
- Technical ownership does not come from who typed a line. It comes from being able
  to explain, test, change, and support the behavior safely.

## Next steps

The active roadmap is [`../PLAN.md`](../PLAN.md). In parallel with Nocturne, I am
taking three vertical slices into line-level ownership: the state funnel, the
feedback/safety DSP path, and the Night gesture path. The first Nocturne feature,
the momentary hold punch, is the first complete review exercise under the policy in
[`DEVELOPMENT_PROCESS.md`](DEVELOPMENT_PROCESS.md).
