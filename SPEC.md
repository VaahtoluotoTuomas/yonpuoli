# SPEC.md — Yölahti

Yölahti is an independent SuperCollider feedback instrument whose starting point was
the **Ciat-Lonbarde Cocoquantus 2** (Peter Blasser): two 8-bit "Coco"
delay/loopers around a five-oscillator chaotic "Quantussy" brain. It is no longer
positioned as a faithful emulator; its software-native routing, safety architecture,
file and memory systems, and Night interface deliberately take it in its own direction.
This document defines **what** the instrument does. `ARCHITECTURE.md` defines how the code
is organized. `CLAUDE.md` defines working rules for implementation.

Status: **implemented normative specification.** The v1.0 engine and Archipelago GUI
baseline are complete; the active Nocturne phase is tracked in `PLAN.md`. Parameter
names, ranges, defaults and safety requirements here remain the single source of truth.

---

## 1. Goals and non-goals

### Goals
1. **Faithful in behavior and aesthetics, native to SuperCollider in interface.**
   Reproduce the *musical behaviors* of the Cocoquantus (lo-fi clocked-RAM delays,
   recirculating 8-bit degradation, chaotic stepped modulation, two-knob macro control
   of complexity), not its circuits.
2. **A processor-instrument.** Primary use case: feed in field recordings and modular
   synth material (audio interface, files, optionally laptop mic), mangle it, record the
   result to disk and/or send it back out to the modular.
3. **Safe to play.** It must be impossible for any patch, preset, modulation state,
   feedback setting or user error to produce a damaging output level. Surprising sound
   yes; surprising loudness no.
4. **Professional code.** Testable, documented, one source of truth for parameters,
   clean server/language separation. See CLAUDE.md invariants.

### Non-goals
- Circuit-level accuracy (no component modeling, no claim of null-test equivalence).
- On-screen patch-cable simulation. Patching is done through a named routing matrix.
- MIDI note polyphony / keyboard playing. This is a monolithic instrument, like the
  hardware: "polyphony" = 2 Cocos + 5 castles + 2 file decks running concurrently.
- Multi-instance operation (possible later via separate servers; out of scope for v1).

---

## 2. Hardware background (research summary)

Facts below are drawn from Peter Blasser's own description, the published Cocoquantus
schematic (as analyzed by Richard Brewster), and the Perfect Circuit in-depth article.
Where the sources are ambiguous, the SC interpretation is marked **[interpretation]**.

### 2.1 Coco (×2) — 8-bit delay/looper
- 8-bit digital delay around RAM, clocked by a variable-speed oscillator. The **Speed**
  knob sets the clock: fast = short, brighter delay; slow = long, darker, crunchier,
  noisier. Buffer length and effective fidelity are coupled.
- **Input** level knob (unity ≈ noon, beyond = overdrive into the fuzzy preamp/ADC) and
  **Feedback** ("FEEDBA") level knob (unity ≈ noon; just below = looper-like repeats;
  beyond = escalating mangled, saturating echo cascades).
- Both Input and Feedback levels have **verso/inverso VCA inputs**: patchable amplitude
  modulation, non-inverting (verso) or inverting (inverso) polarity.
- Both channels have a 3-position **Dolby** switch: *off* / *noise-gate on own signal*
  (self-expander) / *keyed by the other channel* ("other-puncher" — input and feedback
  duck/gate each other for "monophonic articulations of time-layers").
- **Loop button**: default state records continuously (delay); pressed = recording
  disengaged, buffer content loops unchanged (red LED). At power-on the buffers are
  full of noise and loop-locked — noise is a documented, intentional sound source
  ("digital dust": with no input and feedback up, chip self-noise accumulates).
- Patch points: **SP.AF** (purple) speed-modulation input with a bipolar depth knob
  (`^`), **Flip** (green) reverses buffer playback direction, **Skip** (green) resets
  the loop to its start, **gray** delayed-signal output, **yellow "organ"** output —
  a rich square derived from the internal digital circuitry whose pitch follows Speed
  (usable as a voice or modulator).

### 2.2 Quantussy — five castles
- Five triangle-core VCOs ("petals"/"castles") in a ring. Per castle: 3-position range
  switch — infrasonic (LFO) / audio / "Balcium" (low-mid) — plus banana I/O:
  **orange** triangle out, **gray castle out** (stepped S&H signal), **blue** FM in,
  **green Spesal** in.
- Internal mechanism (from the schematic): each castle's square wave clocks a **pair of
  sample-and-holds**. An analog selector picks ONE partner castle out of the other four;
  S&H #1 samples the partner's *triangle* → this castle's **castle output**; S&H #2
  samples the partner's *castle output* → this castle's **FM source**.
- **Two global knobs rule everything**: *Master Frequency* (scales all five rates) and
  *Chaos* (global depth of the castle→frequency FM feedback). Chaos from 0 up:
  stable → vibrato → stepped/sliding random → "hyperchaos" (noise bursts, silences).
- The **Spesal** input steps a per-castle counter (flip-flops + diode network) through
  **8 states**, re-selecting the partner pairing; 15 center LEDs (3 per castle) display
  the "Spesal State". Touching the node scrambles it. 8^5 = 32768 network configurations.
- **[interpretation]** The 8 states = 4 possible partners × 2 hold modes
  (sample-and-hold vs. track-and-hold). Implemented that way in SC.

### 2.3 Input section
- Preamp module: XLR mic in (dynamic) and Hi-Z piezo/instrument in, each with level
  knob, amplified output (white) and an **envelope follower** output (orange).
  The preamps saturate musically when pushed.
- Two stereo line inputs normalled directly to the Cocos (L→Coco1, R→Coco2).

### 2.4 Cocoquantus 1 vs 2
Same architecture and feature set (Flip/Skip/SP.AF, Dolby, verso/inverso existed in v1).
V2 refinements: banana jacks replace bare screw/alligator patch points, refined panel
and center display. **This port targets the Cocoquantus 2 feature set.**

---

## 3. Sound identity requirements

The implementation MUST reproduce these behaviors (these drive the acceptance tests):

1. **Clocked-RAM re-pitching.** Changing Speed re-pitches material already in the
   buffer (tape-like doppler), while also changing loop length and fidelity together.
2. **Aliasing as a voice.** No anti-alias filtering around the RAM loop. Slow clocks
   produce foldover shimmer and grit on the way in AND on the way out (zero-order hold
   both directions).
3. **Recirculating degradation.** The feedback path re-enters the 8-bit converter:
   every repeat is re-clipped and re-quantized, so long feedback tails get
   progressively crunchier and denser instead of just quieter/louder.
4. **Bounded chaos.** Feedback > unity must escalate into saturated, quantized mayhem
   that is *loudness-stable* (bounded by the converter clip), never a runaway level.
5. **Digital dust.** With no input, feedback raised collects a granular noise floor
   that can be caught into loops (self-noise source, level controllable in SC).
6. **Lossless hold.** Loop-hold plays the buffer indefinitely with zero degradation,
   and Speed changes during hold re-pitch the loop without destroying it.
7. **Quantussy feel.** Master Frequency + Chaos as the two-knob macro: 0 = stable
   tones/LFOs; mid = drunken stepped & gliding wander; max = hyperchaos including
   audio-rate screaming, bursts and silences. Castle rates cross smoothly between
   LFO, low-mid and audio ranges.
8. **Everything is signal.** Castle outputs are audible if routed to audio inputs;
   Coco outputs, organ squares and envelope followers are patchable as modulators.
   No artificial wall between CV and audio.
9. **Organ output.** A square-stack voice per Coco whose pitch follows that Coco's
   clock. **[interpretation: derived from address-counter bits]**

---

## 4. Signal flow overview

```
                    ┌────────────────────────────────────────────────┐
                    │                 MOD SOURCE BUS (16 ch, audio)  │
                    │  tri1..5  cas1..5  envA envB  coco1 coco2 org1 org2
                    └───────▲──────────────────────────▲─────────────┘
                            │                          │
   ┌──────────┐   ┌─────────┴──────────┐    ┌──────────┴──────────┐
   │ SOURCES  │   │  QUANTUSSY (5 x    │    │  ROUTING MATRIX     │
   │ live L/R │   │  castle: tri, sqr, │    │  weights: 16 srcs → │
   │ mic      │   │  S&H pair, spesal) │    │  22 dests, per-dest │
   │ deck A/B │   │  qFreq, qChaos     │    │  signed weights     │
   └────┬─────┘   └────────────────────┘    └──────────┬──────────┘
        │                                              │ (speed FM, VCAs,
        ▼                                              ▼  flip/skip/spesal,
   ┌──────────┐    ┌───────────────────────────────────────────┐  castle FM, deck rate)
   │ PREAMP A ├───▶│ COCO 1: dolby → [clip → 8-bit quant → RAM │
   │ (fuzz,   │    │ loop @ variable clock] → out + organ      │──┐
   │ env out) │    │ feedback recirculates through converter   │  │
   └──────────┘    └───────────────────────────────────────────┘  │
   ┌──────────┐    ┌───────────────────────────────────────────┐  │
   │ PREAMP B ├───▶│ COCO 2: (identical)                       │──┤
   └──────────┘    └───────────────────────────────────────────┘  │
                                                                  ▼
                              ┌──────────────┐   ┌────────────────────────────┐
                              │ SPACE (FDN   │◀──┤ MIX: levels, pans, sends   │
                              │ reverb, opt.)│──▶│ MASTER SAFETY: LeakDC →    │
                              └──────────────┘   │ tanh headroom → Limiter →  │
                                                 │ bad-value squelch → outs   │
                                                 │ (+ optional stem outs,     │
                                                 │  + disk recorder)          │
                                                 └────────────────────────────┘
```

Default (unpatched) state mirrors hardware: live L→Coco1, live R→Coco2, matrix empty,
buffers filled with dust and loop-locked, master faded in muted-to-nominal over 2 s.

---

## 5. Modules and parameters

Conventions: all parameters listed here exist in `CQSpec` with exactly these names,
ranges, curves, defaults. All setters clamp to range. `exp` = exponential mapping,
`lin` = linear, `bipolar` = linear centered on 0. `fb taper` is the exactly
invertible piecewise-linear control map
`(0.00→0.00, 0.50→0.85, 0.90→1.10, 1.00→1.25)`; it changes hand resolution,
not stored parameter values or DSP. Toggles are 0/1. Enums are Symbols.

### 5.1 Sources

| Param | Range | Default | Curve | Notes |
|---|---|---|---|---|
| `liveGain` | 0–4 (amp) | 1.0 | exp | trim for interface inputs (config: `inChans`) |
| `micGain` | 0–8 (amp) | 0.0 | exp | laptop/interface mic channel (config: `micChan`). **Default 0 (muted)** — feedback safety |
| `deckALevel`, `deckBLevel` | 0–2 | 1.0 | exp | file players |
| `deckARate`, `deckBRate` | −2.0–2.0 | 1.0 | bipolar | negative = reverse; matrix destination |
| `deckALoop`, `deckBLoop` | 0/1 | 1 | — | |

Decks: load single files or a folder (`samples/`); next/prev/random file navigation;
buffers are peak-normalized to −6 dBFS on load (consistent gain staging for field
recordings). Formats: anything libsndfile reads (wav/aiff/flac). Mono files play as
mono; stereo files: deck A takes L, deck B takes R when loaded as a linked pair. A
linked pair becomes audible only after both buffers are ready: its two players start in
one server bundle, and the dry monitor preserves L/R rather than summing them to dual
mono. Standalone deck loads remain dual mono in the dry monitor. Async loads are
last-request-wins and cannot publish after deck/session teardown.

### 5.2 Preamps A / B

Two channels; A feeds Coco1's `pre` slot, B feeds Coco2's by default.

| Param | Range | Default | Curve | Notes |
|---|---|---|---|---|
| `preASrc`, `preBSrc` | enum | `\liveL` / `\liveR` | — | `\liveL \liveR \mic \deckA \deckB \none` |
| `preADrive`, `preBDrive` | 1–40 (amp) | 2.0 | exp | into asymmetric soft clip (fuzz). Unity-ish ≤ 2 |
| `preABias`, `preBBias` | 0–0.6 | 0.15 | lin | waveshaper asymmetry (even harmonics) |

Each preamp publishes an **envelope follower** (`envA`, `envB`) to the mod source bus
(attack 3 ms, release 150 ms, 0–1 range).

### 5.3 Coco 1 / Coco 2

Per Coco `n` ∈ {1,2} (names shown for n=1):

| Param | Range | Default | Curve | Notes |
|---|---|---|---|---|
| `coco1In` | 0–2 | 0.8 | lin | input level into converter; >1 overdrives. Matrix VCA dest `coco1_inVca` |
| `coco1Fb` | 0–1.25 | 0.35 | fb taper | feedback/loop level; 1.0 = unity looper; >1 = saturating growth (bounded by clip). Matrix VCA dest `coco1_fbVca` |
| `coco1Speed` | 3000–48000 Hz | 24000 | exp | RAM clock. Loop length = bufFrames/clock (default buffer 2^17 ⇒ 2.7 s @48 k, 43.7 s @3 k). Post-modulation hard clamp: 1500 Hz – server SR (modulation may push an octave below the knob floor — the "ground jack" trick). Matrix dest `coco1_spaf` |
| `coco1Spaf` | −4.0–4.0 oct | 0 | bipolar | the `^` knob: depth/direction of speed modulation from the matrix |
| `coco1DolbyIn` | enum | `\off` | — | `\off \gate \duck` — gate = downward expander on input; duck = input attenuated by feedback-channel envelope |
| `coco1DolbyFb` | enum | `\off` | — | same, on feedback channel, keyed by input envelope |
| `coco1Hold` | 0/1 | 1 at boot | — | 0 = recording (delay), 1 = loop-locked. Boot state mirrors hardware power-on |
| `coco1Flip` | 0/1 | 0 | — | reverse head direction (works recording and holding). Matrix gate dest `coco1_flip` |
| — skip — | trigger | — | — | resets loop phase to 0. Method `.skip(n)` + matrix trig dest `coco1_skip` |
| `coco1Bits` | 3–12 | 8 | lin (int) | converter depth. 8 = authentic |
| `coco1Wrap` | 0/1 | 0 | — | 0 = converter clips (authentic-safe); 1 = digital wraparound overflow (evil mode, still level-bounded downstream) |
| `coco1Interp` | enum | `\zoh` | — | `\zoh` (authentic stair-step DAC) / `\smooth` (cubic read, tamer top end) |
| `coco1Dust` | 0–0.5 | 0.02 | exp | self-noise injected pre-converter; the "digital dust" source |
| `coco1Organ` | 0–1 | 0 | lin | level of the organ square-stack voice (pitch tracks Speed) into this Coco's output mix |
| `coco1Level` | 0–1.5 | 0.8 | exp | output level to mix |
| `coco1Pan` | −1–1 | −0.3 (coco2: +0.3) | bipolar | |
| slot gains: `coco1SlotPre`, `coco1SlotDeck`, `coco1SlotQ`, `coco1SlotX` | 0–1.5 | 1.0 / 1.0 / 0.0 / 0.0 | lin | input mixer slots: preamp A, deck A, Quantussy audio mix (`qMix`), cross-feed from Coco2. (Coco2 slots: preamp B, deck B, qMix, Coco1) |

**Converter chain (normative order):** input mix → dolby(in) → ×inVCA → + feedback
(dolby(fb) → ×fbVCA) → + dust → `clip2(1.0)` **(or wrap)** → quantize to `bits` →
RAM. This ordering is what bounds all feedback and produces recirculating crunch.

### 5.4 Quantussy

Global:

| Param | Range | Default | Curve | Notes |
|---|---|---|---|---|
| `qFreq` | 0.05–500 Hz | 4.0 | exp | Master Frequency — scales all castle rates |
| `qChaos` | 0–1 | 0.15 | curve +2 | global depth of internal castle→FM feedback; maps internally to 0–5 octaves of expo FM. The hand map widens stepped wander (fader 0.5 → ≈0.269, 0.75 → ≈0.545); direct values and snapshots retain linear 0–1 DSP semantics |
| `qShLag` | 0–0.2 s | 0.004 | exp | slew on castle (S&H) outputs. 4 ms = declick only ≈ authentic steps; larger tames the network (SC extension) |

Per castle `k` ∈ {1..5}:

| Param | Range | Default | Curve | Notes |
|---|---|---|---|---|
| `c1Range` … `c5Range` | enum | mixed (see below) | — | `\infra` ×0.02, `\balcium` ×1, `\audio` ×40 rate multiplier vs `qFreq` |
| `c1Off` … `c5Off` | −1.0–1.0 oct | fixed spread | bipolar | per-castle detune (stands in for hardware component variance): defaults [+0.00, +0.23, −0.31, +0.48, −0.17] |
| — spesal — | trigger | states 0–7 | — | steps partner/mode counter. Method `.spesal(k)`, `.spesalSet(k, state)`, matrix trig dest `castleK_spesal` |

Default ranges at boot: `[\infra, \infra, \balcium, \infra, \audio]` — modulation-heavy,
one audible voice, like a sensible hardware starting state.

Castle mechanism (normative): castle k's square clocks its S&H pair once per cycle.
State ∈ 0–7 ⇒ partner = other castle indexed by state mod 4, mode = state div 4
(0 = sample-and-hold, 1 = track-and-hold). S&H₁ samples partner's triangle → **castle
output** `casK`. S&H₂ samples partner's castle output → internal FM: castle k's
frequency = `qFreq × range × 2^(off + qChaos·5·sh2 + fmExt)` where `fmExt` is the
matrix `castleK_fm` destination sum (in octaves). Frequency clamped to 0.01–4000 Hz.

Outputs published to the mod source bus: `tri1..tri5` (±1 triangle), `cas1..cas5`
(±1 stepped). `qMix` audio bus = Σ castle triangles × per-castle audio sends
(`c1Send`..`c5Send`, 0–1, default 0 except castle 5 = 0.5).

### 5.5 Routing matrix (the banana bay)

- **Sources (16):** `tri1..tri5, cas1..cas5, envA, envB, coco1, coco2, org1, org2`.
- **Destinations (22):** per Coco: `_spaf` (speed FM, scaled by `cocoNSpaf`), `_inVca`,
  `_fbVca`, `_flip` (gate), `_skip` (trig); per castle: `_fm` (octaves), `_spesal`
  (trig); plus `deckA_rate`, `deckB_rate` (±1 rate offset).
- Weight range **−1.0–1.0** per (source, dest) pair. Negative weight = inverso
  (inverted CV) — this is how verso/inverso polarity is expressed.
- Gate/trig destinations use a Schmitt threshold: rising through +0.5 fires (gates:
  >+0.5 = on).
- `patchDepth` (0–1, default 1.0, lag 50 ms): global scaler over all matrix weights —
  a "how alive is it" macro and instant tamer.
- API: `patch(src, dest, w)`, `unpatch(src, dest)`, `clearPatch`, `getPatch`,
  `patchPreset(\name)`, `shake(amount: 0.3, seed: nil, density: 0.1)` (throw a sparse
  random patch — each cell with probability `density`, weights ±amount — plus random
  spesal states; never touches levels/feedback — safety+musicality rule; same seed =
  same throw). The throw **replaces** the patch rather than adding to it, so
  `density: 0` **clears the bay** — nothing sown, nothing left standing — while
  deliberately leaving the spesal states alone: a clear is a clear, not a scramble in
  disguise. `density` was added
  2026-07-22 for the night's sowing moon (GUI_SPEC §16.6), which maps its lit phase
  onto it — how *much* is sown, while `patchDepth` remains the how-*hard* macro.
  Existing callers are unaffected: the default is the historical ~10%.
- Named presets shipped in v1 (derived from documented hardware patches):
  `\empty`, `\classic` (tri→spaf both Cocos, castle→castle FM ring),
  `\selfNoise` (dust looping + VCA wobble), `\quantumDrone` (castles as audio, skip/flip
  animation), `\organMode` (organ outs as voices, cas→VCAs, tri→spaf light),
  `\mayhem` (dense random, capped depths).

### 5.6 Master

| Param | Range | Default | Curve | Notes |
|---|---|---|---|---|
| `masterDry` | 0–1 | 0.0 | lin | untreated source monitor mix (hardware has none; convenience) |
| `masterVol` | −60–0 dB | −12 | lin dB | lagged 100 ms |
| `monitorMute` | 0/1 | 0 | — | hard output mute (panic), 30 ms fade |

**Master safety chain (fixed, non-bypassable):** mix → `LeakDC` → soft saturation with
−6 dB headroom (`tanh`) → `Limiter.ar(ceiling: 0.89 ≈ −1 dBFS, dur: 0.01)` → bad-value
squelch (NaN/inf → 0) → boot fade envelope → hardware outs. **Identical chain on each
stem output.** A watchdog measures the **driven** master-mix RMS (post-vol, pre-safety-
chain; per channel, louder wins); if RMS > −6 dBFS sustained 5 s it posts a notice
(console + GUI). With `autoTrim: true` (opt-in, **default off**) it additionally engages an
automatic −6 dB ease-off, held until `CQ.resetTrim`/reboot. This is a **comfort feature,
not a safety guarantee**: the limiter (§8.1) and bounded recirculation (§8.2) are the
safety; the watchdog only eases sustained loud *driving*. (It measures the driven mix, not
the tamed output — the tanh + limiter chain holds the output near −6 dBFS RMS or below
regardless of drive, so an output threshold never reliably fires; the driven mix is a wide
range, +1.6 dBFS for dense material at vol 0. `CQ.testWatchdog` simulates a trip.)

### 5.7 Stems and recorder

- Main out: stereo to `outChans` (config, default [0,1]).
- Optional stems to additional interface outputs (config `stemOuts`, e.g. [2,3,4,5]):
  post-safety Coco1, Coco2 (mono each) — for re-processing in the modular in real time.
- Every configured hardware-output index is globally unique: `outChans` contains no
  duplicates, `stemOuts` contains no duplicates, and the two sets are disjoint. Invalid
  aliasing is rejected before server boot because separate post-limiter `Out` writes to
  one physical bus would otherwise sum after the safety ceiling.
- Recorder: `rec` / `recStop`. Writes 24-bit WAV, timestamped
  `recordings/cq_YYMMDD_HHMMSS.wav`, never overwrites. Optional stem recording
  (`recStems: true`, config choice): `rec` writes a 4-channel file — master L/R plus
  the post-safety Coco1 and Coco2 stems (same taps as `stemOuts`) — instead of stereo.
  Runtime state is explicitly `idle/starting/recording/stopping/error`; one start attempt
  owns its path, buffer and writer synth until stop/error cleanup completes. A second REC
  cannot create another writer, and stop/free during starting cancels that attempt.

---

## 6. Control design

Three layers, all writing through one state funnel (equal rights, always consistent,
each optional — the instrument is fully playable with any one layer alone):

1. **16n faderbank = the hands.** Continuous, absolute, multi-finger — carries the core
   performance gestures (riding Input/Feedback/Speed is *the* Coco technique).
2. **GUI = the eyes + the switches.** State display (meters, loop LEDs, Spesal LEDs,
   castle activity), toggles/buttons (hold, flip, skip, dolby, range switches, presets,
   rec), matrix grid, file drop. Mouse is never required for continuous gestures.
3. **Code = the bananas + the memory.** Patching, presets, snapshots, morphing,
   batch file loading, scripted automation. A patch is a few readable lines — closer
   to the hardware's banana anarchy than dragging virtual cables, and it's saveable.

Rationale: live-coding alone pulls attention from listening during two-hand gestures;
GUI alone is one-knob-at-a-time; 16n alone can't express routing. Layered, they map to
what each does best. (GUI-less and 16n-less operation must both remain possible.)

### 6.1 16n mapping (default; remappable in `config.scd`)

16n default: **CC 32–47** (fader 1 = CC 32). The atovproject firmware sends each
fader on its **own MIDI channel** (F1 = ch 1 … F16 = ch 16; measured 2026-07-17),
so binding is by CC number alone: config `sixteenN.chan` is `nil` = accept any
channel (default) or an integer 0–15 to pin single-channel firmware. 7-bit
smoothing with 30 ms lag on all faders.

| Fader | CC | Param | | Fader | CC | Param |
|---|---|---|---|---|---|---|
| F1 | 32 | `coco1In` | | F9 | 40 | `qFreq` |
| F2 | 33 | `coco1Speed` | | F10 | 41 | `qChaos` |
| F3 | 34 | `coco1Spaf` | | F11 | 42 | `preADrive` + `preBDrive` (linked) |
| F4 | 35 | `coco1Fb` | | F12 | 43 | `deckARate` + `deckBRate` (linked) |
| F5 | 36 | `coco2In` | | F13 | 44 | `deckALevel` + `deckBLevel` (linked) |
| F6 | 37 | `coco2Speed` | | F14 | 45 | unassigned (reserved) |
| F7 | 38 | `coco2Spaf` | | F15 | 46 | `patchDepth` |
| F8 | 39 | `coco2Fb` | | F16 | 47 | `masterVol` |

Left half = the two Cocos exactly as on the hardware panel; right half = Quantussy
macros, sources, global. Takeover mode configurable: `\jump` (default) or `\pickup`
(fader must cross current value after preset recall).

### 6.2 GUI panel

> **Version note (edited 2026-07-28, during N9):** GUI **structure, interaction
> grammar, visual language, motion and constants are normative in
> [`GUI_SPEC.md`](GUI_SPEC.md)** — one window whose sole instrument surface is
> the Night map, with a persistent Master strip and a Drawer
> (inspector · workshop · spellbook · memory). The former Day panel is retained
> only as design history; N9 removed its runtime stage after Night + Master +
> Workshop proved exact coverage of every `CQSpec` parameter. This subsection
> no longer describes a layout; it keeps only what the GUI owes *this* document.
> Parameters, ranges, curves, DSP and all §8 safety invariants remain SPEC's,
> and §8 wins any conflict. The retired v1.0 four-column panel is preserved in
> this section as it stood at the `v1.0` tag; its build log is
> [`docs/history/PLAN_v1.0.md`](docs/history/PLAN_v1.0.md).

Requirements on any GUI, independent of skin:

- **It must show**: in/out meters, limiter gain reduction, HOLD state, Spesal
  states and castle activity, loaded deck filenames (updating on *every* load
  path: drop, load button, folder navigation, code), REC state + elapsed time,
  and the notice line.
- **It must make reachable without code**: hold, flip, skip, Spesal step, dolby
  and range switches, every §5 `CQSpec` parameter, patch presets, snapshots +
  A→B morph, the full 16×22 signed-weight matrix, and file loading including
  drag-and-drop.
- **Mic**: the preamp's mic control carries a warning color when `micGain` > 0
  while a builtin output is active, and the GUI never grows a second `micGain`
  path (§7, §8.5).
- **Keyboard** (unchanged, global in every view): `1/2` hold Coco1/2 · `q/w`
  flip · `a/s` skip · `3..7` spesal castle 1–5 · `r` rec · `space` panic ·
  `p` shake. Widgets never take keyboard focus (mouse-only), so the key map —
  panic included — always works after any click; only text/number entry takes
  focus, click-to-focus only (never at boot, no tab chain), and releases it on
  Enter.
- **PANIC and the notice line are visible at all times** and may be covered by
  nothing — no view, no drawer, no overlay (§8.3).
- The instrument must stay fully playable with the GUI closed (§6 intro).

### 6.3 Live-coding API (facade `Cocoquantus`, singleton `CQ`)

```supercollider
CQ.boot;                            // reads config.scd, builds graph, opens GUI
CQ.set(\coco1Fb, 1.05);             // any param, always clamped
CQ.patch(\cas3, \coco1_spaf, 0.7);  // banana in one line
CQ.patch(\envA, \coco2_fbVca, -0.5); // inverso = negative weight
CQ.patchPreset(\quantumDrone);
CQ.hold(1); CQ.flip(2); CQ.skip(1); CQ.spesal(4);
CQ.deckA.load("~/field/lake.wav"); CQ.deckA.rate_(0.5);
CQ.deckA.next; CQ.deckB.random;     // folder navigation (scans the loaded folder)
CQ.loadStereo("~/field/pair.wav");  // linked pair: L -> deck A, R -> deck B
CQ.shake(0.4, seed: 1234);          // reproducible chaos
CQ.snap(\lakeDrone);  CQ.recall(\lakeDrone);  CQ.morph(\lakeDrone, \mayhem, 30);
CQ.rec; CQ.recStop;
CQ.panic;                           // instant mute, state preserved
CQ.resume;                          // unmute after panic (no fade-in re-trigger)
CQ.resetTrim;                       // restore level after a watchdog auto-trim (§5.6)
```

Lifecycle is explicit and readable as `CQ.lifecycleState`:
`\stopped/\booting/\running/\stopping/\failed`. `CQ.free` invalidates pending async
work immediately; boot failures roll back partial ownership. `CQ.reboot` performs a real
restart of `Server.default` so device/sample-rate/block-size/I/O options take effect; as
an explicitly strong operation, it also stops unrelated synths using that shared server.

### 6.4 Patterns (optional, M3)

Event type `\cq` sequences discrete actions on a TempoClock — generative "player
piano" sessions. Each event carries an `\action` plus its arguments and dispatches
through the facade (so every write still passes the funnels). A missing/`\rest`
action or an unbooted instrument is a silent no-op, so a pattern can carry Rests and
survive a `free`/reboot. Actions: `\hold`/`\flip` (`\coco`, optional `\val`),
`\skip` (`\coco`), `\spesal` (`\castle`, optional `\state`), `\set` (`\param`,
`\value`), `\patch` (`\src`, `\dest`, `\weight`), `\deck` (`\deck` = `\a`/`\b`,
`\do` = `\next`/`\prev`/`\random`/`\restart`), `\shake` (`\amount`, `\seed`,
`\density`). E.g.

```supercollider
Pbind(\type, \cq, \action, \spesal, \castle, 3, \dur, Pexprand(0.5, 4)).play;
```

Not core.

---

## 7. I/O and configuration

`config.scd` (an Event; template `config.example.scd` in repo; machine-specific,
gitignored):

```supercollider
(
  sampleRate: 48000,          // 44.1k/48k/96k supported; coco clock capped at SR
  blockSize: 64,
  device: nil,                // e.g. "ASIO : Fireface" on Windows; or an
                              // [inDevice, outDevice] pair (same host API) when
                              // in/out are separate devices (laptop mic + phones)
  hardwareBufferSize: 256,    // latency vs. stability for live modular processing
  inChans: [0, 1],            // live stereo pair from modular/mixer
  micChan: nil,               // nil = mic path disabled entirely
  outChans: [0, 1],           // unique physical indices
  stemOuts: nil,              // e.g. [2, 3] → unique + disjoint from outChans
  bufFrames: 131072,          // Coco RAM (2^16–2^19)
  sixteenN: (enabled: true, ccBase: 32, chan: nil, takeover: \jump),
                              // chan nil = any MIDI channel (per-fader channels,
                              // §6.1); an integer 0..15 pins one channel
  folders: (samples: "samples", recordings: "recordings",
            presets: "presets", snapshots: "snapshots"),
  autoTrim: false,            // watchdog notice always; true = opt-in −6 dB auto-ease
  recStems: false,            // true → CQ.rec writes 4-ch: master L/R + coco1/coco2 stems
)
```

Notes:
- **Mic feedback safety:** when `micChan` is set and output device is the laptop's
  speakers, GUI shows a persistent warning; `micGain` defaults to 0 and is never
  included in snapshots' recalled gains above 0 unless explicitly saved by the user.
  Mechanism: `CQ.snap(name)` stores `micGain` as 0; `CQ.snap(name, includeMic: true)`
  is the explicit opt-in. Recalling any snapshot whose file opens the mic posts a
  loud warning. Setting `micGain` above 0 through any entry point warns once per
  0 → live transition (headless; the GUI adds a persistent color in M2).
- **Snapshots never store or recall `monitorMute`:** recall must not defeat panic
  (§8.3 "panic always reachable"). Panic is a performance action, not memory.
- **To modular:** stems are line-level audio (post-limiter). Castle CV to hardware is
  possible only with a DC-coupled interface — document but don't special-case.
- 16n hot-plug: absence is fine; arrival auto-binds (device match on name "16n").

---

## 8. Safety requirements (hard, tested)

1. Every hardware output passes the fixed master safety chain (§5.6). No code path may
   route around it. Ceiling −1 dBFS, brickwall.
2. The Coco recirculation is bounded **inside** the loop: `clip2(1)` (or wrap) before
   quantization, unconditionally. Feedback of any setting cannot raise loop energy
   beyond 0 dBFS.
3. All parameter entry points clamp via `CQSpec` (code, MIDI, GUI, snapshots, shake,
   matrix). Malformed snapshot/config values are clamped or rejected with a warning,
   never applied raw.
4. All gain-class parameters are lagged ≥ 30 ms server-side (no zipper steps, no
   instant jumps to loud).
5. Boot sequence is silent until the graph is verified, then fades in over 2 s.
   `CmdPeriod`, stale async work, boot failure and unexpected server quit invalidate the
   session and leave silence (never a raw unprocessed path); the facade can rebuild.
6. NaN/inf anywhere in the chain is squelched to silence and reported, never emitted.
7. The NRT safety test (§10) gates every change: worst-case preset must render with
   peak ≤ −0.75 dBFS, no NaN, |DC| < 0.02.

Safety comes entirely from 1–7 above (bounded recirculation + brickwall limiter make a
damaging level impossible). The watchdog (§5.6) is **not** a safety requirement — it is
an optional comfort feature (sustained-loud notice; opt-in −6 dB auto-ease via
`autoTrim`), default off, and may be disabled or removed without weakening safety.

---

## 9. Performance requirements

- CPU: full instrument (2 Cocos, Quantussy, decks, space, GUI meters) ≤ 25% of one
  core on a mid-range 2020s laptop at 48 kHz / block 64.
- GUI CPU (language side) ≤ ~3% at rest with a played-sized patch. Measured
  2026-07-21 on the reference laptop: ~2.8–3.8% with 20 planted cables, rising a
  couple of points while the pointer crosses cables and falling back at once. A
  snapshot morph is a deliberate, temporary exception at ~5–6%: it drives the whole
  parameter set continuously, and the transparent cable layer re-strokes every cable
  whenever any control beneath it repaints. See `GUI_SPEC.md` §10 for the pacing and
  density rules that keep the resting figure.
- Control latency: 16n fader → audible ≤ 20 ms perceived (smoothing included).
- Live throughput latency: bounded by `hardwareBufferSize`; no added latency in the
  dry-to-coco path beyond one block.
- Sample-rate independence: identical musical behavior at 44.1/48/96 kHz (coco clock is
  absolute Hz; only the ultrasound ceiling differs).
- No allocations on the audio path after boot (buffers preallocated; deck loads use
  separate buffers with crossfade swap).

---

## 10. Acceptance criteria

Automated (must pass in CI/dev loop, see ARCHITECTURE §9):
- A1 Class library + all SynthDefs compile clean (`CQ_COMPILE_OK`).
- A2 Logic tests pass: spec clamping (incl. NaN/inf/string fuzzing), matrix name↔index
  mapping, snapshot save/load roundtrip equality, config validation (`CQ_TESTS_PASSED`).
- A3 NRT safety render: `\mayhem` + fb 1.25 + drive 40 + wrap on + dust max + chaos 1 +
  qFreq 500 + spaf ±4, 90 s, no input → peak ≤ −0.75 dBFS, no NaN, |DC| ≤ 0.02
  (`CQ_SAFETY_PASSED`).
- A4 NRT hold-integrity: capture a loop, hold 60 s with speed sweep ±2 oct and back →
  first vs. last pass correlation ≥ 0.999 (lossless hold).
- A5 NRT unity-looper: fb = 1.0, one impulse train pass → repeat energy decay within
  ±1 dB over 30 s (requant noise floor growth allowed, level drift not).

Manual listening checklist (user-verified, `tests/manual_checklist.md`):
- M1 Speed sweep re-pitches held loop smoothly; slow = dark/gritty, fast = bright.
- M2 Feedback ride: 0.9 ≈ echoes, 1.0 ≈ looper, 1.15 = escalating crunch, level stable.
- M3 Chaos sweep 0→1 traverses stable→vibrato→stepped wander→hyperchaos.
- M4 `\selfNoise` preset reproduces the no-input digital-dust workflow.
- M5 Flip/skip audibly reverse/restart; dolby duck creates "time-layer" articulation.
- M6 A field recording through `\classic` at defaults is immediately "Cocoquantus-like"
  (warbling, aliased, alive) without touching anything.
- M7 Full session (30 min, headphones) with deliberate abuse: nothing painful ever.
- M8 16n: all 16 faders correct and smooth; unplug/replug mid-session survives.

---

## 11. Milestones

**M1 — it sounds right (core DSP, code-only control).**
Config/boot, groups/buses, Coco pair (hold/flip/skip, dolby, dust, organ, bits/wrap),
Quantussy (spesal, chaos, ranges), matrix + presets `\empty \classic`, master safety,
live in + deck A, facade API, tests A1–A5 green. Exit: play a code-only session.

**M2 — it plays right (interfaces).**
16n layer, GUI panel complete, snapshots + morph, shake, deck B + folder navigation,
mic path + warnings, remaining presets, keyboard map. Exit: 30-min session without
typing anything but patch lines; M1–M8 checklist pass.

**M3 — it ships (polish).**
The v1.0 baseline included the later-retired Space FDN; stems + stem recording,
watchdog trim, pattern event type, README,
performance pass (§9 budget), full acceptance re-run, v1.0 tag.

---

## 12. Deliberate deviations from the hardware

Documented so fidelity questions have one answer:

1. Banana bay → named 16×22 signed-weight matrix with presets/shake/snapshots.
2. Global `patchDepth` macro and per-dest Schmitt thresholds (no hardware equivalent).
3. Added parameters: `bits`, `wrap`, `interp`, `dust` level, `qShLag`, castle `Off`
   detunes, castle audio sends — all default to authentic behavior.
4. File decks, snapshots, morphing, disk recorder: studio affordances, no hw analog.
5. Master limiter, watchdog, lags, clamps: safety layer the hardware never needed
   (it can't exceed line level; a computer + headphones can).
6. The v1.0 Space FDN was a software-only experiment and was retired after the
   2026-07-28 listening decision; current production has no built-in reverb.
7. Spesal 8-state semantics and organ derivation are marked interpretations (§2, §3).
8. Stereo image: hardware is dual-mono to a stereo out; SC adds pan/width control.
