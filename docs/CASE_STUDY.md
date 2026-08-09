# Yönpuoli — a personal instrument built with AI

## Building something I wanted to play

I began Yönpuoli because I wanted to explore ideas from Peter Blasser's
Ciat-Lonbarde Cocoquantus without owning the physical instrument. The early
project was a software study: two deliberately coarse clocked memories, a
five-oscillator Quantussy network, and patchable feedback.

It gradually stopped being an emulator project. File decks, snapshots, a signed
routing matrix, output safety, and the Night landscape pulled it toward its own
identity. Yönpuoli is now a standalone SuperCollider instrument whose central
act is simple: let an outside sound enter a Coco memory, close the loop, and
play what the memory becomes.

The interface follows that idea. Birds carry samples, shells hold memories,
castles disturb them, roots bring in live sound, and cables grow across the
landscape. The imagery is not decoration placed over a conventional panel. It
is my attempt to make signal flow readable without making the instrument lose
its mystery.

## My role and the AI collaboration

I conceived the project and directed its musical and visual design. I decided
what the instrument should do, wrote and refined its specifications, played the
iterations, and made the final choices about sound, interaction, safety, and
release scope.

Claude Code and OpenAI Codex were used extensively as coding agents. They
proposed and implemented a large part of the source code, tests, documentation,
and debugging work. I do not describe Yönpuoli as a repository I hand-coded
alone. My work has been closer to instrument design and technical direction:
turning musical intentions into explicit behavior, examining implementations,
finding failures through use, and continuing the iteration until the result
felt coherent under the hand and ear.

That process was not automatic. Plausible-looking solutions were often wrong in
practice. I rejected interactions that hid state, visual effects that suggested
signal activity that was not real, a resizable Drawer that weakened the playing
surface, and sounds that passed technical tests but did not belong to the
instrument.

## A few decisions that shaped the result

### The loop comes first

Yönpuoli can make sound from its organ, noise, and chaotic network, but the
sample-to-memory path is the instrument's centre. This changed both the release
presets and the documentation: the first lesson is loading a bird, writing it
into a Coco, sealing Hold, and reshaping the loop. Self-running voices are
presented as secondary entrances rather than a shortcut around the main idea.

### Feedback above unity is playable territory

Feedback greater than 1.0 is not treated as an invalid value. It is where echoes
become loops and loops become dense converter weather. The recirculating signal
is therefore clipped or wrapped before every re-quantization pass, while every
hardware output crosses a separate DC-removal, bounded-headroom, limiter, and
bad-value-squelch chain.

This keeps dangerous level growth out of the hardware path without sanding away
the unstable part of the sound. Offline renders exercise the real SynthDefs
under hostile patches, but listening still decides whether the result is
musically useful.

### Every control tells the same truth

GUI gestures, MIDI, code, snapshots, presets, and generative shakes all write
through the same validated state and matrix funnels:

```text
hands / code / MIDI
         │
         v
 CQState / CQMatrix ──> CQSpec validation ──> fixed audio graph
         ^                                        │
         └──────── state and telemetry ───────────┘
```

This matters most in a strange interface. A shell, fader, recalled snapshot, and
live-coding command must not develop four subtly different meanings. Night's
ambient motion also comes from real server telemetry rather than an unrelated
decorative clock.

### Windows audio recovery became part of the instrument

Connecting headphones after startup exposed a surprisingly deep platform
problem: the long-lived SuperCollider interpreter could keep a stale PortAudio
device list. Restarting the audio server alone did not guarantee fresh device
discovery.

The final path stops the owned server, launches a small bounded helper
interpreter with an isolated class path, samples the Windows device list more
than once, validates the result, and only then rebuilds the audio session. The
player sees one explicit scan/restart gesture and a highlighted next action.
Solving the lifecycle problem and explaining it clearly in the interface were
equally important parts of the fix.

## Working by playing

Automated checks cover parameter limits, routing, lifecycle transactions,
snapshot migration, output safety, hold integrity, GUI geometry, gesture
cancellation, and paint cost. They are useful because feedback software has
failure modes that are difficult to notice in casual use.

They are not the final musical judge. Audible and visible increments were tested
in the real application with samples, headphones, an audio interface, a mouse,
and optional controller hardware. Several changes went through many small rounds
because a cue could be technically functional and still feel misplaced.

## What I learned

- A specification is most useful when every control surface shares it, not when
  it merely describes the code afterward.
- AI-generated implementation and AI-generated tests can share the same wrong
  assumption. A small reproducer that fails for the right reason is more useful
  than a large pass count.
- GUI performance and interaction have to be measured in the real Qt event
  path. Intuitive fixes can be slower or can introduce deadlocks.
- Experimental does not have to mean careless. The interface can remain obscure
  and discoverable while lifecycle, output safety, and displayed state remain
  explicit.
- Supporting software is an ongoing practice: being able to navigate, explain,
  test, change, and maintain the behavior matters more than manual keystroke
  count.

## Where it is now

Yönpuoli 1.0 contains the complete Night surface, six authored presets, five
factory snapshots, recording, optional 16n control, and a no-code Windows
launcher. The first release uses current interface screenshots; performance
video and audio excerpts will follow when they represent the instrument well.

The exact implementation is described in [`../ARCHITECTURE.md`](../ARCHITECTURE.md),
the signal and behavior contract in [`../SPEC.md`](../SPEC.md), and the playing
surface in the [`Night field guide`](QUICK_REFERENCE.md).
