# ARCHITECTURE.md — Yönpuoli

How the code is organized. `SPEC.md` is normative for behavior, names, ranges and
safety; this file is normative for structure. Code snippets here are **sketches of the
intended approach** — they show the trick, not final compilable code.

---

## 1. Principles

1. **One state funnel.** Every parameter change — code, MIDI, GUI, snapshot, shake —
   goes through `CQState.set`, which clamps via `CQSpec`, forwards to the server, and
   notifies dependants (GUI, etc.). No side channels.
2. **`CQSpec` is the single source of truth** for parameter names/ranges/defaults and
   must mirror SPEC §5 exactly. Anything reading a range reads it from `CQSpec`.
3. **Server graph is static after boot.** Fixed groups, buses and long-lived synths;
   no synth churn while playing (exception: deck buffer swaps, recorder). Re-patching
   = writing weights to control buses, never re-wiring nodes.
4. **Fail toward silence.** Bad values squelch, missing devices degrade features
   (no 16n → still playable), errors never leave an unlimited path to the outputs.
5. **Language-side is replaceable, server-side is sacred.** GUI and MIDI layers can be
   rewritten freely; SynthDef signal chains follow SPEC exactly.

---

## 2. Repository layout

```
yonpuoli/
├── SPEC.md  ARCHITECTURE.md  AGENTS.md  README.md
├── YONPUOLI.bat                // double-click Windows application launcher
├── boot.scd                    // user entry point: loads config, Cocoquantus.boot
├── live.scd                    // performance scratchpad: example blocks, patch recipes
├── config.example.scd          // template → user copies to config.scd (gitignored)
├── config.scd                  // machine-specific (gitignored)
├── classes/                    // sclang class library (add to includePaths, see §10)
│   ├── Cocoquantus.sc          // facade singleton (+ CQ alias subclass)
│   ├── CQSpec.sc               // parameter registry: ControlSpecs, defaults, 16n map
│   ├── CQConfig.sc             // config load + validation + defaults
│   ├── CQAudioConfig.sc        // pure active/draft Audio profile model
│   ├── CQAudioDeviceProbe.sc   // bounded fresh-process Windows discovery
│   ├── CQAudioProfileStore.sc  // pending roundtrip + atomic sibling profile
│   ├── CQAudioBootRecovery.sc  // startup preflight + one profile fallback
│   ├── CQAudioReconfigure.sc   // one candidate + at most one rollback
│   ├── CQGraph.sc              // buses, groups, synth spawning, boot/teardown
│   ├── CQSynthDefs.sc          // all SynthDefs (pure server-side DSP)
│   ├── CQState.sc              // value store, clamped setters, dependant notify
│   ├── CQMatrix.sc             // routing matrix: name↔index, weights, presets, shake
│   ├── CQAudition.sc           // transient matrix-preview bookkeeping
│   ├── CQInspector.sc          // Drawer grid touch/listen + fine-edit transaction
│   ├── CQSpellbook.sc          // committed matrix notification log + code export
│   ├── CQMidi16n.sc            // 16n binding, smoothing, takeover
│   ├── CQGui.sc                // panel window (view-only client of CQState)
│   ├── CQGuiDrawer.sc          // Inspector / Workshop / Spellbook / Memory / Audio
│   ├── CQGuiWorkshop.sc        // Drawer ledger page: 24 setup parameters
│   ├── CQGuiAudioSetup.sc      // Drawer Audio I/O draft page
│   ├── CQGuiScale.sc           // the Drawer's two reviewed size steps (pure arithmetic)
│   ├── CQInkMenu.sc            // Ink-painted Drawer choice box + opened list
│   ├── CQInkList.sc            // Ink-painted persistent list with exact row geometry
│   ├── CQSnapshots.sc          // save/recall/morph
│   ├── CQRecorder.sc           // disk recording (master + stems)
│   ├── CQDeck.sc               // transactional file player control
│   ├── CQDeckPair.sc           // atomic linked-stereo coordinator
│   └── CQStandaloneApp.sc      // pure lifecycle decisions for one-click mode
├── support/                    // isolated app/audio-probe scripts and class overlays
├── presets/                    // patch recipes + legacy snapshot read fallback
├── snapshots/                  // *.cqsnap.scd snapshot files (text, versioned)
├── samples/                    // user field recordings (gitignored)
├── recordings/                 // output (gitignored)
└── tests/
    ├── compile_check.scd       // headless: prints CQ_COMPILE_OK, exit code
    ├── run_all.scd             // UnitTest runner: prints CQ_TESTS_PASSED / _FAILED
    ├── classes/                // UnitTest classes (CQSpecTest, CQMatrixTest, CQSnapshotTest,
    │                           //   CQConfigTest) — sclang can only define classes in .sc
    │                           //   files on an include path, not in .scd scripts
    ├── nrt_safety.scd          // NRT worst-case render + analysis: CQ_SAFETY_PASSED
    ├── nrt_hold_integrity.scd  // SPEC A4/A5 renders
    ├── nrt_matrix_inputs.scd   // six software-native Coco destinations in real DSP
    ├── nrt_shake_ecology.scd   // four audible blank-RAM shake families
    ├── nrt_linked_stereo.scd   // linked L/R identity + sample-aligned start
    └── manual_checklist.md     // SPEC M1–M8 listening tests
```

---

## 3. Server graph

### 3.1 Groups (execution order is the architecture)

```
RootNode
 └─ gCQ
     ├─ gSources    // \cqDeck ×2, \cqInput (preamps + env followers)
     ├─ gQuantussy  // \cqQuantussy (single synth: 5 castles + spesal + qMix)
     ├─ gCocos      // \cqCoco ×2
     └─ gMaster     // \cqMaster (mix, safety, meters, stems), \cqAura (telemetry),
                    // \cqRecorder when active
```

`\cqAura` (GUI_SPEC §9) sits here purely because gMaster runs last: it reads the
finished `aModSrc` bus with plain `In.ar` — no InFeedback needed — rectifies and
lags at audio rate so audio-range castles cannot alias into the control stream,
and reports 16 magnitudes at 15 Hz. It writes to no bus and no output, so it is
outside every safety path; a headless session simply has no listener.

`/cq_charge` (GUI_SPEC §9) needed no such def: the eleven gate/trig sums and their
post-Schmitt decisions already exist inside `\cqQuantussy` and `\cqCoco`, so
each synth reports its own paired halves — coco 1 replyID 1, coco 2 replyID 2,
the five castles replyID 3. Each Coco block is flip/skip/record; the castle
block remains five spesals. The graph never holds a second, drifting copy
of either the threshold input or its result. Both halves are peak-held
(`LagUD.ar(value, 0, 0.15)`) before `A2K`, because a trigger may last one sample
and a control-rate sample would miss it.

Rationale: modulation sources are computed before their consumers within the same
block. Intentional one-block delays are Coco↔Coco cross-signals (`InFeedback`),
the Quantussy's internal ring (`LocalIn/LocalOut`), and a Coco's explicit
self-modulation return for its own raw RAM tap and conditioned organ source
(`LocalIn/LocalOut`).

### 3.2 Buses

| Name | Rate | Ch | Writer → Reader | Purpose |
|---|---|---|---|---|
| `aModSrc` | audio | 16 | quantussy, input, cocos → cocos, quantussy, decks, master | mod sources, canonical index order below |
| `aPreA`, `aPreB` | audio | 1 | input → cocos | preamp outputs |
| `aDeckA`, `aDeckB` | audio | 1 | decks → input, cocos | deck audio |
| `aQMix` | audio | 1 | quantussy → cocos | castle audio mix (per-castle sends) |
| `aCoco1`, `aCoco2` | audio | 1 | cocos → master, cocos (cross via InFeedback) | post-converter, post-level, pre-pan taps |
| `aDry` | audio | 2 | input, decks → master | untreated source monitor feed (`masterDry`, SPEC §5.6) |
| `wDest[dest]` | control | 16 | language (CQMatrix) → consumer synth | one 16-ch weight bus per matrix destination (32 total) |
| (params) | — | — | language → synth args via `.set` | everything in CQSpec that isn't matrix |

**Canonical `aModSrc` index order (fixed, used by CQMatrix and all synths):**
`0–4 tri1..5 · 5–9 cas1..5 · 10 envA · 11 envB · 12 coco1 · 13 coco2 · 14 org1 · 15 org2`.
Channels 12–15 are written by the Coco synths themselves. Earlier consumers and
the other Coco read each relevant channel separately through `InFeedback.ar`.
Inside each `cqCoco`, its own `cocoN`/`orgN` pair returns through a two-channel
`LocalIn/LocalOut` instead of depending on a partially current four-channel bus;
both paths are exactly one block late. The organ jack is the raw stack conditioned
by `×1.6` and `clip2(1)`; the audible Organ mix remains unconditioned.

### 3.3 Matrix destination list (fixed order, 32)

`coco1_spaf, coco1_inVca, coco1_fbVca, coco1_flip, coco1_skip,`
`coco1_organ, coco1_slotQ, coco1_slotX, coco1_dust, coco1_record,`
`coco2_spaf, coco2_inVca, coco2_fbVca, coco2_flip, coco2_skip,`
`coco2_organ, coco2_slotQ, coco2_slotX, coco2_dust, coco2_record,`
`castle1_fm..castle5_fm, castle1_spesal..castle5_spesal, deckA_rate, deckB_rate`

Consumer synths read their own dest buses:
```supercollider
var w    = Lag.kr(In.kr(wBusIndex, 16), 0.05) * patchDepth;  // patchDepth lagged too
var mods = In.ar(modSrcBus, 16);        // (channels 12..15 via InFeedback where needed)
var sum  = Mix(mods * w);               // signed weights = verso/inverso
```
Gate/trig dests: `var trig = Trig1.ar(sum > 0.5, SampleDur.ir);` (Schmitt via `> 0.5`
with hysteresis handled by `Schmidt.ar(sum, 0.3, 0.5)` if chatter appears).

---

## 4. SynthDefs (`CQSynthDefs.sc`)

All defs take `out`/bus-index args; nothing hardcodes bus numbers. All level-ish args
are wrapped in `Lag.kr(x, 0.03)` minimum (SPEC safety #4).

### 4.1 `\cqDeck` (×2 instances)

`PlayBuf`-based, `rate = deckRate + matrixSum(deckX_rate)`, clamped ±2, loop switch,
`t_trig` restart, out → `aDeckA/B`. Buffer swap on load: new Buffer, 50 ms crossfade
via a short env on synth replace (only allowed synth churn). Each asynchronous load owns
its buffer locally under a monotonically increasing request generation; stale, failed and
freed-session completions release that buffer and cannot publish a synth or path.
`CQDeckPair` prepares linked L/R independently, then commits both synth starts in one
`Server.bind` bundle. Linked dry monitoring preserves L on the left and R on the right;
ordinary one-deck loads remain dual mono.

### 4.2 `\cqInput`

- Reads `SoundIn` channels from config (live L/R, mic), applies `liveGain`/`micGain`.
- Preamp A/B: source select (`Select.ar` over [liveL, liveR, mic, deckA, deckB, 0]),
  drive → asymmetric fuzz → fixed partial makeup (−1 dB per actual Drive
  octave) → gentle HPF 20 Hz:
  ```supercollider
  var shaped = ((x * drive) + bias).tanh - bias.tanh;   // asymmetric soft clip
  var makeup = (drive.log2.neg).dbamp;                   // unity at Drive 1
  var pre = HPF.ar(shaped * makeup, 20);
  ```
- Envelope followers read that audible compensated `pre` signal:
  `Amplitude.ar(pre, 0.003, 0.15)` → `aModSrc[10..11]`.
- AIO-4 names the three hardware reads `rawLiveL/rawLiveR/rawMic` before any
  gain or source selection and publishes exactly that order through one
  15 Hz `/cq_io_inputs` `SendPeakRMS`; missing mic is a DC zero. The telemetry
  writes no audio bus and the downstream gain/preamp expressions are unchanged.
  It was temporarily withdrawn during the WASAPI investigation, then restored
  after the same hang reproduced without it and stopped once live
  `_ListAudioDevices` enumeration was removed. The isolated split-WASAPI
  recheck then kept sound and Night motion alive, moved both raw presence rows
  and returned three consecutive external `/status.reply` packets.
- Outputs `aPreA`, `aPreB`.

### 4.3 `\cqQuantussy` (one synth, five castles)

```supercollider
// sketch — inside one SynthDef, arrays of 5
var fb      = LocalIn.ar(10);                  // [tri1..5, cas1..5] from previous block
var tris    = fb[0..4]; var cass = fb[5..9];
var castle  = { |k|
    var others   = (0..4).removing(k);
    var state    = states[k];                          // 0..7, set from language (spesal)
    var partner  = Select.kr(state.mod(4), others);    // which of the other 4
    var thMode   = state.div(4);                       // 0 = S&H, 1 = track&hold
    var fmExt    = Mix(In.ar(modSrcBus, 16) * wCastleFm[k]);      // octaves
    var sh2      = Latch.ar(SelectCas(partner, cass), trig[k]);   // internal FM source
    var hz       = (qFreq * rangeMul[k] * (2 ** (off[k] + (qChaos * 5 * sh2) + fmExt)))
                     .clip(0.01, 4000);
    var tri      = LFTri.ar(hz);                       // phase-accumulating, FM-safe
    var sqr      = tri > 0;
    var trigK    = Trig1.ar(sqr, SampleDur.ir);        // one sample per cycle
    var sampled  = SelectTri(partner, tris);
    var cas      = Select.ar(thMode, [
                     Latch.ar(sampled, trigK),                    // sample & hold
                     Gate.ar(sampled, sqr) ]);                    // track & hold
    [tri, Lag.ar(cas, qShLag)]
};
// collect 5 castles, LocalOut.ar(tris ++ cass)
// spesal: states settable via \state1..\state5 args; matrix castleK_spesal trig
//   increments server-side: Stepper.ar(trigSum, min:0, max:7) merged with language state
//   (implementation choice: keep counter server-side, language sends t_step triggers;
//    report state to language via SendReply for the LED display)
// outs: aModSrc[0..9] = tris ++ cass;  aQMix = Mix(tris * cSends)
```

Notes: `Select.ar` with array indexing over `others` is the partner selector (the
CD4052). One-block feedback via LocalIn/LocalOut is the accepted stand-in for the
analog ring; do not "fix". All castle work at audio rate — ranges reach audio.

### 4.4 `\cqCoco` (×2 instances) — the heart

```supercollider
// sketch of the RAM loop core
var clockHz = (speed * (2 ** (spafSum * spafKnob)))
    .clip(CQSpec.speedClampFloor, SampleRate.ir);       // 750 Hz, SPEC §5.3
var ratio   = clockHz / SampleRate.ir;               // ≤ 1.0
var flipTrig = Trig1.ar(Schmidt.ar(flipSum, CQSynthDefs.gateOffThreshold,
    CQSynthDefs.gateOnThreshold), SampleDur.ir);
var reverse = (handFlip - ToggleFF.ar(flipTrig)).abs; // hand XOR latched trigger parity
var dir     = 1 - (reverse * 2);
var phase   = Phasor.ar(skipTrig, ratio * dir, 0, BufFrames.ir(buf)); // address counter
var here    = BufRd.ar(1, buf, phase, loop: 1, interpolation: 1);
// ^ MUST be created before BufWr: reads the cell value BEFORE overwrite = the DAC out

var organVca = (organLvl + prVcaLaw(organSum)).clip(0, 1);
var slotQVca = (slotQ + prVcaLaw(slotQSum)).clip(0, 1.5);
var inSig   = Mix([aPreX * slotPre, deck * slotDeck, qmix * slotQVca, cross * slotX]);
// verso/inverso via signed weights. The knob is the valve's rest; the matrix
// contribution is SHAPED, not linear — (1+sum)^2 - 1, sum clamped +-2 first
// (SPEC §5.3/§5.5, interpretation §13.5, exactly like the organ derivation).
// Shaping the contribution rather than the total keeps every knob/default at
// its zero-sum value and keeps a banana able to open a shut VCA. Snapshot VCA
// weights keep their numbers but are sonically revoiced. See prVcaLaw.
var inVca   = (inLevel + CQSynthDefs.prVcaLaw(inVcaSum)).clip(0, 2);
var fbVca   = (fbLevel + CQSynthDefs.prVcaLaw(fbVcaSum)).clip(0, 2.5);
var inCh    = dolby(inSig, dolbyInMode, keyEnv: fbEnv);   // 3-mode gate/duck helper
var fbCh    = dolby(here,  dolbyFbMode, keyEnv: inEnv);
var dustSig = (LPF.ar(WhiteNoise.ar, 9000) * 0.35)
    + (Dust2.ar(SampleRate.ir * 0.02) * 1.2);
var record  = (inCh * inVca) + (fbCh * fbVca) + (dustSig * dust);
record      = Select.ar(wrap, [record.clip2(1), record.wrap2(1)]);   // ADC rails
record      = record.round(2 ** (1 - bits));                        // requantize EVERY pass
var recordGate   = Schmidt.ar(recordSum, 0.3, 0.5);
var effectiveHold = (hold - recordGate).abs;          // momentary XOR; never manual-mutates
var write   = Select.ar(effectiveHold, [record, here]); // hold = identity rewrite (lossless)
BufWr.ar(write, buf, phase, loop: 1);

var outTap  = Select.ar(interpSmooth, [
                here,                                              // \zoh — authentic
                BufRd.ar(1, buf, phase, loop: 1, interpolation: 4) // \smooth
              ]);
var organ   = Mix(Pulse.ar(clockHz / [128, 256, 512], 0.5, [0.5, 0.3, 0.2]));
var voice   = (outTap + (organ * organVca * CQSynthDefs.organAudibleGain)); // 0.55
// out: aCocoN = voice * level (lagged); aModSrc[12/13] = normalized tap;
//      aModSrc[14/15] = organ
```

Why this reproduces SPEC §3: last-write-wins into each cell = ZOH ADC at `clockHz`
with no AA filter (aliases in); non-interpolating read = ZOH DAC (aliases out);
`here` read pre-write = the cell about to be overwritten = full-buffer delay;
changing `ratio` moves both heads together = tape-style re-pitch of held content;
`hold` writes back the exact value just read (interpolation 1 at the same integer
phase) = bit-exact lossless hold at any speed; clip→quantize inside the loop =
recirculating degradation and bounded feedback; `flip` reverses the counter direction
(reverse loops when holding, head-crossing reverse-delay chaos when recording —
authentic-adjacent, keep); `skip` resets the Phasor. Organ and SlotQ matrix sums
use the same shaped additive VCA contribution as Input/Feedback and retain their
manual zero-sum values. Record uses its own 0.3/0.5 Schmitt and momentarily
inverts the Hold seal in either direction: a held writer opens and an open writer
freezes for the gate duration. It never mutates the stored Hold parameter.

`dolby(sig, mode, keyEnv)` helper (function in the def):
```supercollider
var env = Amplitude.ar(sig, 0.003, 0.08);
Select.ar(mode, [ sig,
    sig * Lag.ar(env > 0.02, 0.005, 0.05),        // \gate: self-expander
    sig * (1 - (keyEnv * 0.9)).clip(0, 1) ]);     // \duck: "other-puncher"
```

### 4.5 `\cqMaster` — the sacred chain

```supercollider
var mix = (pan-placed cocos) + (drySources * masterDry);
mix     = mix * masterAmp.lag(0.1) * (1 - mute.lag(0.03));
mix     = LeakDC.ar(mix);
mix     = (mix * 0.5).tanh * 2 * 0.5;                    // −6 dB headroom glue
mix     = Limiter.ar(mix, 0.89, 0.01);                   // ceiling −1 dBFS
mix     = Select.ar(CheckBadValues.ar(mix, post: 0) > 0, [mix, DC.ar(0)]);
mix     = mix * EnvGen.kr(Env([0,0,1],[0.5,2]));         // boot silence + fade
Out.ar(outChans, mix);
// stems: same LeakDC→Limiter→squelch per stem before Out.ar(stemOuts, ...)
// meters: SendPeakRMS 20 Hz → \cq_meters; GUI reuses this post-safety master
// presence instead of adding a second output tap
// watchdog: RMS>-6dBFS for 5s → SendReply \cq_hot
```

Language side on `\cq_hot`: engage −6 dB trim (a lagged synth arg), GUI notice
(SPEC §5.6, config `autoTrim`).

---

## 5. Class layer (language side)

`CQGuiHover` owns the always-on lower-left help view and its passive callbacks.
`CQHoverText` authors short notes for existing semantic identities; Night resolves
the original individual playing hit after its native hover handler, while Master,
widgets and shell controls register their existing views. This is a read-only
observer, with no new setter, server resource, hit layer or timer. It wraps only
hover/leave and resize callbacks, suppresses repeated text layout for an unchanged
target and clears ordinary hover on stage changes, background or leave.
`CQGuiInfo.render` synchronizes only the presentation mode: inspection and
the combined guide replace individual hover notes with stable instructions,
including distinct play and cable-entry reading modes. `CQGuiHover` retains the latest pointer target separately, so
leaving info restores it without a timer or an extra musical listener.
`CQGuiPanels` keeps the 118 px footer and its 400 px help area. A 280 px transport
group aligns the status area's edges to REC and PANIC, reserves 52 px for combined
notices and a fixed elapsed-time field, then leaves 40 px before the rightmost
Master controls. Status changes do not change button geometry.
`CQGui` constructs hover help before `CQGuiInfo` and frees them in reverse order,
so nested callbacks restore first the observer, then the original native handlers.
Teardown releases every hook and the shell-state listener. The hover smoke checks
native semantic coverage, text fit, footer order and callback restoration at both
supported sizes.

The optional info layer is GUI-owned; reading and navigation are read-only. `CQInfoGroups` maps existing
semantic identities into component topics, supplies protected subject bounds,
and chooses a non-overlapping card position. It gives whole Coco/castle bodies
inspection priority over crossing cables, without changing playing hit tests.
`CQInfoPages` authors concise rows with references to those same identities;
`CQInfoText` retains individual reference text and delegates the combined guide to
`CQInfoTour` / `CQInfoAdvanced`, whose read-only steps own stable lesson IDs, prose, symbols, semantic
references, concept/action cue policy and placement side together. Cable entry
and special cue placement use lesson IDs instead of ordinal page numbers. No parallel
parameter registry, setter or server resource is introduced.
Each guide's first page alone offers a two-click session reset. `CQGuiInfo` owns
confirmation; `CQGui` delegates to `Cocoquantus.freshSession`, which uses the
existing owned free/boot lifecycle with a copy of the active audio config and
the same running server. Recorder, morph, deck loads, graph buffers, listeners
and GUI histories are torn down before defaults are rebuilt. Saved files remain.
The GUI captures the guide identity before teardown and reopens its page one.


`CQInfoState` bounds navigation: one page for Coco, two for castles and Shortcuts,
23 for basics & first loop and 27 for organ & advanced. A shared guide-class
lookup supplies counts, lesson data and references without parallel controllers.
It owns footer availability and
bounded navigation; `CQGui.prKey` delegates left/right to an open multipage card
outside text entry and Drawer. Only toolbar entry to either learning guide enables
playing; `page` preserves its entry mode. `CQGuiInfo`
wraps existing mouse/drop callbacks, consumes complete inspection gestures,
and owns all listeners, callbacks and one enabled-only 15 Hz mode-breath clock.
PANIC bypasses inspection. Background closes the card; another component replaces
it directly; another control within the same component retains the current page.
The shell owns both guide buttons and Shortcuts; the controller releases all
three actions on teardown. A cable's semantic owner enters the first guide
at its port introduction in inspection; the toolbar resets it to page one in play
mode. Shortcuts describes the existing
`CQGui.prKey` map, including the context-limited info page keys and Ctrl+X redo.
`CQBananaGrammar` owns bounded undo/redo rings. It captures exact inverse matrix
states and only the changed parameter keys of compound operations; all restores
use the same CQMatrix/CQState funnels. New commits clear redo; view-only work does not.
Guide illustration hover reuses native ink brightness, including Drawer and
Master controls. Optional `detailItems` form a second picture band immediately
before the corresponding paragraph; they join the same reference/cue inventory.
Concept pages have only
hover references; practical pages also supply action cue references. The card is raised
after guide locators and fills an opaque background; no surface cue bleeds through.

Component geometry samples come from `CQGuiNightMap.infoHitRegions`, refreshed
after native layout. The card tries bounded widths and adjacent subject edges,
scoring subject distance before stage-edge preference and keeping centre alignment
where space permits. Other components may be covered. Coco spends a wider card on
one large picture and two independently wrapped text columns. Each line carries
its own x coordinate and width; paragraph hover bounds follow those columns.
Castles use enlarged ring/network and single-castle pictures on separate pages.
Their native diagram includes the central eye and a labelled, fixed example of
sampling links; no telemetry or new animation is introduced.
Moon/eye rows carry an explicit gesture axis,
so their miniature leaders cannot rotate into a misleading diagonal. The layout
protects the whole subject and keeps the footer above Master. No subject overlap
or silent text clipping is accepted. A native-window smoke audits every page at both
supported sizes. `CQInkInfoMap` draws miniatures using the existing geometry and
Pen recipes; numbered leader lines connect the picture to row hover locators.
The first semantic reference is the miniature anchor: Coco's SP.AF, input and
feedback each own a row anchored to their crescent, while related ports remain
in that row's hover references. Q and cross-feed now also have separate gill/socket
rows. Castle ports read upper outputs then lower inputs,
left-to-right; its range ripple finishes the anatomy page.
`CQInkInfo` lays out the combined guide's lesson-specific symbols/action arrows
and maps general overview symbols to all matching surface parts. It reuses native
bird, knob, heart, crescent, winding and chevron recipes as static examples.
Input and Feedback lessons each enlarge a three-crescent context picture and
brighten only the named middle/rightmost control. Neither drawing class allocates
image resources or extra clocks. The guide introduces fixed audio paths versus
control cables before its connection/editing lessons, authored in `CQInfoTour`.
`CQInfoAdvanced` inherits only the common read-only lesson helpers and authors
the second progression. Its port pictures use `CQGuiNightMap.endpointSpec`, so
their family colours and event glyphs cannot diverge from the playing surface.
Tide uses four row-bound close-ups through `CQInkInfo`, with native
`drawTideField` / `drawTideGate` recipes rather than shrinking the landscape or
substituting source-jack leaves. Hovering these pictures shares the row locator.

Dust gestures use screen-space deltas in `CQGuiNightTouch`: negative y raises
normalized amount, positive x raises grain. Both use the existing sensitivity,
clamp to 0..1 and write through CQSpec/CQState. The mirrored visual anatomy does
not rotate these input axes. Grain's DSP blend and all parameter maps stay unchanged.

Bird pull recognition and bounded return-pose math live in `CQGuiNightTouch`.
Mouse-up alone dispatches the existing deck random action; the global-cursor
fallback only tracks the held hand. `CQGuiNightMap` owns the per-bird finite
return Routines and pose arrays. Its shared gesture cancellation, resize and
free stop them and restore home. `CQInkNight.drawBird` applies the small pose
inside the existing bird layer without moving hit targets or touching audio.

Shape-following group outlines append to the existing route paint surface.
Endpoint hover is derived from the inspected group or referenced row, via the
same endpoint ink function. Native crescent and gate/duck painting also reads
group hover; its existing sibling views refresh when the group changes.
Up to eight small, hit-transparent locator views
mark the referenced parts. Master reuses its existing paint surfaces rather
than adding children to its box layout. The selected component outline is static;
the clock redraws only the info button and visible small annotations. Card rows,
miniatures and landscape do not repaint on every tick. Disable, Drawer, leave,
resize and teardown clear transient locators/hover; teardown restores original
handlers, paint callbacks and mouse tracking.
Night's outer stage has no layout. An inner host owns the instrument's
StackLayout; the card and annotation layer are siblings of that host. This
prevents Qt from stretching the first shown card over the instrument. Resize
follows the inner container's completed geometry pass.

| Class | Role | Key methods / notes |
|---|---|---|
| `Cocoquantus` | facade singleton; owns boot lifecycle and all subsystems | Explicit `stopped/booting/running/stopping/failed` state plus monotonically increasing session generation; stale callbacks self-cancel. `*boot(configPath)`, waitable `*free(onDone)`, strong `*reboot`, confirmed `*reconfigureAudio(model)` and profile-neutral `*restartCurrentAudio`. `*serverResponsive` distinguishes a status-watcher process record from a server that still answers; the pre-graph audio-clock gate additionally requires three reported actual-sample-rate readings, 0.75 s apart, each within 80–120% of the request before a session can become ready. An unresponsive local server is quit through its owned PID before a fresh boot, avoiding a second process on the occupied port. `CQ : Cocoquantus {}` empty subclass = short alias (classvars shared) |
| `CQSpec` | parameter registry | `*specs` (IdentityDictionary name → ControlSpec+default+lag+16n slot), `*clamp(name, val)`, `*default(name)`, exact invertible hand maps and semantic reference landmarks; mirrors SPEC §5 tables 1:1. SP.AF's signed-square hand taper changes normalized control resolution only — stored octave values and DSP remain linear/direct |
| `CQConfig` | config load/validate | reads `config.scd`, fills defaults, validates channels/paths, hard-fails informatively |
| `CQStandaloneApp` | pure one-click lifecycle policy | Validates the launcher's private audio-server port and distinguishes a user-closed same-generation GUI from the old GUI closed during a confirmed Audio restart. The standalone script can therefore avoid the conventional shared port and adopt the replacement GUI, while ordinary IDE use retains the facade's independent GUI lifecycle |
| `CQAudioConfig` | pure Audio profile model | Owns the exact nine Audio keys, `defaults < base < overlay` merge, active/draft/dirty/valid separation, 0↔1 channel display boundary and sibling profile path. Split device Strings remain exact endpoint data, while a narrow complete-prefix check rejects recognized cross-host pairs such as ASIO input plus WASAPI output. Generated explicit nils use private `\default` because an Event drops nil-valued keys; the marker is decoded before the candidate reaches `CQGraph`. No Qt, file, server, CQSpec or snapshot dependency |
| `CQAudioDeviceCatalog` | pure normalized device snapshot/filter boundary | Calls an injected provider once, exact-deduplicates directional names, merges exact active/draft endpoints, derives the single-device intersection and produces reversible recommended/all cached views. The Windows recommendation recognizes only complete legacy host prefixes; it never fuzzily parses endpoint names, touches Qt, persists machine names or scans by itself after construction |
| `CQAudioDeviceProbe` | stopped-boundary device discovery | On Windows launches `support/audio_device_probe.scd` in a fresh daemon `sclang` through a 20-second PowerShell watchdog, an isolated standard-library class path and suppressed user startup files. Ordinary boot uses one pass; explicit hot-plug discovery runs three fresh processes 750 ms apart and trusts the final snapshot, allowing a newly published jack endpoint to settle without making every launch slower. Data crosses back only as an exact compile-string Event in a random private temp file, which is validated and deleted. The injected runner makes unit tests hermetic. Other platforms retain direct stopped-process enumeration |
| `CQAudioProfileStore` | generated Audio overlay persistence | Writes only the sibling `.audio.scd.pending`, loads it back, hard-gates its keys and validates the merged candidate before exposure. Promotion is a same-directory atomic replacement, preserves one `.bak`, never writes `config.scd`, and keeps a validated pending file retryable after rename failure |
| `CQAudioBootRecovery` | pure startup preflight/recovery coordinator | Treats the generated profile as a candidate and the base config as its sole fallback. Fresh device absence can preselect the base, or fail before scsynth when every configured route is absent; server-open or pre-graph clock failure can restart the base exactly once. A valid temporarily unavailable preference is never overwritten: ready base Audio becomes `fallbackReady` for that session, and a later strong restart retries the profile. Only the forced malformed-profile path repairs the generated sibling and preserves its rejected data as `.bak`; no server, Qt or lifecycle side effects live in this class |
| `CQAudioReconfigure` | serverless audio-restart transaction coordinator | Gates lifecycle and recorder state, captures previous/candidate data, drives one candidate restart and at most one rollback through an injected facade runner, ignores stale generations, promotes only after ready and keeps rename failure retryable without a reboot. Clean drafts are rejected unless the explicit temporary-fallback adoption path requests the same proven transaction. No Qt or server dependency |
| `CQGraph` | server plumbing | allocates buses/groups/buffers, builds ServerOptions from config, spawn order, `s.sync` between stages, teardown, CmdPeriod re-arm hook |
| `CQState` | value store + funnel | `set(name, val)` → clamp → `synth.set` / bus write → `changed(\param, name, val)`. `get`, `asEvent` (for snapshots), `load(event)`. Enum params travel to the server as indices into the CQSpec value list; every other layer writes symbols through the funnel |
| `CQMatrix` | routing weights | name↔index maps (source 16, dest 32 — canonical orders §3.2/3.3), metadata-bearing single-cell writes plus deep `fullMatrix`/clamped `writeMatrix` batch transactions, weight cache mirrored to `wDest` buses, existing triple `asArray/load`, presets and ecological shake. `releasePresetNames` identifies the six authored release starts while `empty` remains a utility and `mayhem` a deterministic stress edge. A seeded shake produces one of four 3–5-cable audible skeletons plus density-controlled decoration, and publishes only its explicit safe musical-parameter preparation as `lastShakePlan`; the model itself never writes `CQState`. All 32 destinations participate in decoration; `dustGrowth` owns both Dust rows and `crossfeed` both SlotX rows |
| `CQAudition` | Inspector-only transient patch preview | Serverless one-candidate `{src,dest,prevW,audibleW}` state machine used by the Drawer's touch-and-listen precision editor; candidate switches and cancellation restore through `CQMatrix.patch(... committed:false)`. Night cable planting no longer calls it |
| `CQBananaGrammar` | Night cable gesture + GUI-local undo | A held cable and its ≤40 px magnetic destination are view-only; drop distance never scales weight. The held weight moon starts at ±0.60 and wheel-arms ±0.05 / Ctrl ±0.01 before the first and only matrix write on release. Lifted routes remain at their audible origin until one atomic move/rip; lifecycle exits cannot leak a preview because none was written. `performUndoableState` bundles an ecological shake's dense matrix and exact safe parameter snapshot into one undo entry; ordinary banana operations remain matrix-only |
| `CQGuiCableOverlay` | planted/held banana ink and span hit-testing | Caches exact endpoint-keyed Bézier records and a pixel-identical offscreen planted pass. Dense morphs may blit one stale frame to keep Qt responsive, but every newly rendered replacement requests its own presentation refresh; hover can therefore never remain one cable behind. `patchAt` tests the same sampled curves the ink draws |
| `CQGuiBananaController` | shared endpoint/body/control binding | Applies the common cable-first grammar and accepts only explicit per-control priority predicates. Night uses that narrow hook for the exact SlotQ/SlotX bridge-end gills; surrounding shell points still resolve to a crossing cable |
| `CQInspector` | Drawer precision gesture state | Shares `CQAudition`; press previews the shared first-touch strength (currently ±0.60), vertical frames are transient, and release publishes one committed fine-edit plus one shared GUI-undo entry; lifecycle cancellation restores the pre-gesture cell |
| `CQGuiScale` | the Drawer's two size steps | Pure arithmetic, no Qt and no state: the measured page inset, the `compact`/`roomy` thresholds, both type/metric records, ascent/descent-safe short text-band height, the shared rail width (derived from the Inspector loom's clamped cell), the column split that keeps the Coco pair equal, and caption measurement for buttons. Every Drawer page asks it rather than holding a literal; `CQGui` feeds it the drawer stage's own resize callback |
| `CQGuiWorkshop` | Drawer ledger page (GUI_SPEC §8) | One explicit manifest of the 24 setup parameters Night and Master do not own drives **both** the built layout and `paramNames`, so the coverage audit and the rendered page are the same data — Workshop ownership is never computed as `CQSpec.names` minus the other surfaces. Level, Organ, SlotQ, SlotX, Dust/DustGrain and the five castle sends have one interactive GUI home in Night. The shared responsive rail carries three source-service modules over a coco/quantussy/coco body, with one `islandLayout` score feeding both the Qt layout and the island's painted titles. Owns the GUI's single interactive `micGain` row and its persistent warning subscription; builds with the GUI, not on demand. Pure view: every row writes through `CQGuiWidgets` → `CQState` |
| `CQGuiAudioSetup` | fifth/rightmost Drawer page (SPEC §7.1 / GUI_SPEC §8) | Three-zone Ink composition on the shared rail: host/engine, stable input/output islands and restart gate. One eight-row host score aligns the painted engine/device-list/mode/device/rate/buffer labels with their exact controls in both single and split modes. The running page consumes the facade's stopped-server snapshot and never calls `_ListAudioDevices`; one local button toggles recommended/all cached drivers and a separate confirmed `find connected devices` action crosses the strong stopped-server discovery boundary. Its replacement page explicitly reports scan completion and fresh directional counts before asking the player to choose a route. Dirty drafts, recorder activity and in-flight restarts block discovery. A temporary fallback is named honestly and exposes `find preferred device` / `retry preferred audio` plus the separately confirmed `keep current audio` adoption path. Edits only `CQAudioConfig`'s draft; first Apply enters the static inline confirmation, while confirmed restart passes only the non-Qt model to the facade. One testable `restartEmphasis` state drives the persistent warning/danger gate, filled available CTA and host-side downward copy; clean, success, cancel and discard retract all three together. A page-owned `CQRecorder` controller updates the restart lock on every state transition and is removed on free. Renders ready/not-saved/fallback/rollback/failed truth. AIO-4 draws coarse raw-input tracks plus shared post-safety master L/R presence from a GUI-owned cache. Quantized deltas and the visible-tab gate prevent hidden or sub-pixel repaint work. The page unregisters its listeners on free and never retains a view callback across reboot |
| `CQGuiMatrix` | experimental Inspector patch loom | Exact canonical 16×32 single-`UserView` field with ≥35×24 px hit cells at the supported compact size, grouped destination islands, family-marked rows, weighted/polarity-shaped knots, persistent route selection, quiet hover row/column focus and an exact-cell spark cue; no inner scroll or new state funnel. One published `destGroups` contract drives both header rules and strong seams: Coco 1 = 0…9, Coco 2 = 10…19, FM = 20…24, Spesal = 25…29, decks = 30…31. `destLabel` shortens only the displayed Coco-local SlotX/Dust names; canonical symbols stay intact |
| `CQInkMenu` | Drawer choice box | Replaces Qt's only-partly-styleable `PopUpMenu` for Inspector presets, Memory's Morph Bridge and Audio setup fields. The collapsed box and temporary borderless choice list are both painted from the active Ink palette; CR/LF/tab/control whitespace is collapsed only in display text, long labels are ellipsized before drawing, disabled choices keep the same subdued geometry, and large lists are capped at 16 wheel-scrollable rows. The popup enables mouse-over on its own top-level window, opens upward near the screen edge, and teardown owns the temporary window |
| `CQInkList` | Drawer persistent single-selection list | Replaces native `ListView` where selectable row geometry must be exact. One application-owned `rowHeight` drives paint, ordinary no-button hover and click selection, preventing platform frame/content metrics from moving the highlight to an adjacent row; Memory's saved snapshots are its first client |
| `CQSpellbook` | read-only committed patch journal | Hooks `CQMatrix` notifications below every client, diffs against the last committed full matrix, excludes `committed:false` frames, exposes row→batch metadata for persistent GUI selection, and exports copyable `CQ.patch(...)` lines or preset data; it does not execute or edit code |
| `CQMidi16n` | controller layer | `MIDIdef.cc` per fader from config map, 0–127 → spec map, optional `\pickup` takeover, hot-plug via `MIDIClient` device watch; all writes via `CQState` |
| `CQGui` | panel | pure view: renders from `CQState`/`CQMatrix` dependant notifications, sends via funnel; composes the sole Night instrument stage, persistent Master strip and `CQGuiDrawer`; meters/LEDs come from namespaced OSCdefs; `/cq_aura` magnitudes stay `nil` until the server reports, repaint only Night's relevant views and cable layer, and are skipped entirely when nothing moved; `/cq_charge`'s eleven paired charge/fire slots follow the same law and repaint one Night lamp at a time, only when that lamp's own reading moved. `/cq_phase` carries seven continuous destination triples per Coco — SP.AF, Input, Feedback, Organ, SlotQ, SlotX and Dust — without another stream. `/cq_castle` gives each inner clock a raw-triangle sample plus realized rate so slow motion resolves and audio-rate motion becomes a stable reed. AIO-4 centrally caches `/cq_io_inputs` and the existing `/cq_meters` master pair, coalesces their combined arrival rate to 15 Hz, notifies owned listeners only while Audio is the visible Drawer page and clears the scheduled generation/listeners/OSCdefs on teardown. Everything UI runs inside `{ }.defer`; closes cleanly on `*free`. `CQGuiShellState` owns only the Night↔Drawer presentation swap — no skin or instrument-mode state. The Drawer's page list is published once by `CQGuiDrawer.tabs` and consumed by the shell's tab row, its stack index and the state's whitelist, so a page cannot exist in one place and be refused in another |
| `CQSnapshots` | memory | `save(name)` → `(version: 1, params: state.asEvent, matrix: matrix.asArray, spesal: [..], meta: (...))` `.asCompileString` → `snapshots/name.cqsnap.scd`; reads prefer that folder and fall back to legacy `presets/*.cqsnap.scd`; `releaseNames` is the exact five-file versioned bank (`example` plus four complete artistic scenes), while arbitrary player memories remain ignored; `recall` clamps everything on load; `morph(a, b, dur)` = Routine interpolating continuous params + weights, discrete switch at midpoint; mic gain never auto-recalled above 0 (SPEC §7). **Frames are driven by elapsed time, not by a tick count** (a slow frame is dropped, so the morph still lands on `dur`), run at `CQSnapshots.morphTickPeriod` = GUI_SPEC §10's 15 Hz ceiling, always yield at least `morphMinYield` so the Qt event loop drains and panic stays reachable, and write the union of both matrices as **one `CQMatrix.writeMatrix` per frame** — never one `patch` per cell (MOR-1, 2026-07-30) |
| `CQRecorder` | disk | Explicit `idle/starting/recording/stopping/error` state machine; attempt-local path/buffer/synth ownership; master 2ch 24-bit wav timestamped or optional 4ch master+stems; never overwrite; `DiskOut` synth in `gMaster` tail |
| `CQDeck` | file players | Cancellable last-request-wins buffer transaction, folder scan/next/prev/random and peak-normalize on load; stale/error paths always release their local buffer |
| `CQDeckPair` | linked stereo | Two-phase L/R prepare and one bundled commit; rolls both sides back if either side fails or is superseded |

### Control/data flow

```
16n ──▶ CQMidi16n ─┐
GUI  ─▶ CQGui ─────┼──▶ CQState.set(name, val) ──▶ clamp (CQSpec) ──▶ server (.set / bus)
code ─▶ Cocoquantus┘            │
snapshots/morph ────────────────┘            └──▶ .changed ──▶ CQGui, (anything else)
CQMatrix: same shape, its own funnel (patch → clamp → wDest bus + .changed)
```

Audio startup state is a separate, slower funnel by design:

```
Audio page ──▶ CQAudioConfig draft/validate ──▶ Cocoquantus.reconfigureAudio
                    │                                      │
                    └── generated sibling profile          └──▶ CQGraph.applyOptions
```

It never passes through `CQState`/`CQSpec`, because a device or hardware channel
is boot configuration rather than a musical parameter. AIO-3 implements the
whole confirmed arrow: pending roundtrip, candidate boot, three-reading
pre-graph actual-sample-rate health gate, commit-after-running or exactly one last-working
rollback. A clean draft can instead restart the exact active config without
creating a profile. The one clean-draft exception is an explicit
`keep current audio` decision during temporary fallback; it deliberately sends
the already-running base route through the same pending/restart/promote proof
before replacing the preference. AIO-4 adds only signal-presence telemetry.

The Windows application path is a deliberately separate owner boundary:

```
YONPUOLI.bat → support/launch_yonpuoli.ps1 → isolated daemon sclang
             → support/yonpuoli_app.scd → Cocoquantus.boot
```

The launcher discovers SuperCollider, creates an ephemeral class configuration
that excludes user startup files, reserves a private free audio-server port and
owns the interpreter until the application window closes. The standalone app
therefore never attaches to an IDE server or a stale process on the conventional
57110 port. Its language process shares the diagnostic console rather than
becoming a detached child. The first GUI always presents Night with the Drawer
closed, even
when startup selected or repaired a fallback route. Its monitor follows session
generations so an Audio restart adopts the replacement GUI instead of exiting;
only that player-initiated restart asks the replacement to reopen Audio. A
same-generation window close frees the facade, quits the owned audio server and
exits cleanly. This ownership applies only to the one-click path; `CQ.boot` in
the IDE keeps its existing lifecycle semantics.

---

## 6. Boot sequence (`Cocoquantus.boot`)

The facade publishes an explicit lifecycle state. Every accepted boot captures a fresh
session generation and local graph/state/matrix references; every async stage checks the
generation after blocking work. `free`, reboot, CmdPeriod and boot failure invalidate it
immediately, so a late callback can only clean its own resources. A stage failure rolls
back GUI/controllers/OSCdefs/decks/recorder and the partial graph, then enters `failed`.

1. Load and validate the hand-maintained config (`CQConfig`), then merge an
   existing generated sibling Audio overlay through `CQAudioConfig`. A malformed
   generated sibling starts the forced base-repair path; the hand-maintained
   config or final base candidate still fails hard with a readable message.
2. If the local server is confirmed stopped, read directional devices once into
   the facade's memory-only `CQAudioDeviceCatalog`. Windows delegates this to
   `CQAudioDeviceProbe`'s bounded fresh interpreter so the main process's stale
   PortAudio list cannot hide a hot-plugged endpoint or newly installed driver.
   If the server is already running, skip discovery and retain the previous or
   configured fallback catalog.
3. Build `ServerOptions` (device, SR, block, hardwareBufferSize, channel counts).
4. `server.waitForBoot` → read the reported actual sample rate three times over
   1.5 seconds and require every value to be within 80–120% of the requested
   rate. The sequence prevents a previous server's stale healthy watcher value
   from hiding a new clock collapse. Failure happens before graph allocation
   and follows the ordinary boot/candidate failure path.
5. Allocate buffers (2 coco RAM, deck placeholders) and buses; create groups in
   order; `s.sync`.
6. Add SynthDefs (`CQSynthDefs.add(config)` — defs parameterized by config where
   needed); `s.sync`.
7. Spawn synths in group order; write CQSpec defaults + power-on state (dust-filled
   buffers via one-shot noise-fill routine, `hold = 1`, matrix empty); `s.sync`.
8. Init MIDI (guarded — absence of 16n is not an error), OSCdefs, recorder standby.
9. Open GUI (unless `headless: true` in config).
10. Release master fade → 2 s ramp to nominal (SPEC safety #5).
11. Install `CmdPeriod` and server-quit hooks. Either invalidates the session; CmdPeriod
   marks the killed graph, while an unexpected scsynth quit also tears down language-side
   clients. Both fail toward silence and require an explicit rebuild.

Teardown (`*free`): recorder stop → master fade 100 ms → free groups/buses/buffers →
close GUI/MIDI/OSC defs. It is idempotent, invalidates pending work before the fade, and
accepts an `onDone` callback; a boot requested during stopping is queued behind it.

`CQ.reboot` is intentionally stronger than `free` + `boot`: it performs a
controlled `Server.quit` → stopped-device scan/profile preflight → `Server.boot`, so
boot-only options take effect without launching a known-missing configured device.
The facade does not treat the `Server.quit` completion callback as process exit:
on Windows that callback may arrive while the old scsynth process still owns its
endpoints, so both `applicationRunning` and `serverRunning` must clear before the
stopped-device scan or replacement boot begins.
The project uses `Server.default`, so this explicit command also terminates unrelated
synths on that shared server; ordinary `CQ.free` never quits it.
If SuperCollider's status watcher marks the local server unresponsive, the facade
does not use stock `Server.reboot` (which would attempt a second boot on the occupied
port): it invokes `Server.quit`'s exact-PID forced stop, then waits until both the
process is gone and the status watcher has cleared `unresponsive` before booting.
Both the responsive controlled restart and forced-quit recovery capture
directional devices only after that stopped boundary and before launching the
replacement process. The Audio page's separately confirmed `find connected
devices` action enters this same lifecycle path. Discovery failure alone never
prevents the audio boot.

The Audio page's later `reconfigureAudio` operation reuses this ownership rather
than duplicating it in Qt: validate and persist pending profile → generation-
guarded free/reboot/build with the candidate → publish/atomically promote only
after the three-reading actual-sample-rate gate and `running`. Candidate failure performs
exactly one generation-guarded last-working rollback; rollback failure stays
`failed` and silent. Recorder
`starting/recording/stopping` blocks the transaction. `CQGraph.applyOptions`
remains the only writer of `ServerOptions`. With no draft changes the page labels
discard as `no changes` and offers `restart audio`; that path first targets the
exact active config, but shares the stopped-scan base fallback when its device
has disappeared. A valid missing-device profile stays untouched while the ready
base route is marked as a temporary fallback; later `find preferred device`,
clean restart or `CQ.reboot` retries it. The player may instead confirm
`keep current audio`; the current base route is then promoted through the same
transaction and becomes the next launch preference. Only malformed-profile
recovery rewrites the generated sibling automatically after full readiness.

---

## 7. Snapshot file format

```supercollider
// snapshots/lakeDrone.cqsnap.scd — plain text, git-friendly
(
  version: 1,
  params: ( coco1Fb: 1.02, qChaos: 0.4, /* every CQSpec param */ ),
  matrix: [ [\cas3, \coco1_spaf, 0.7], [\tri1, \coco2_fbVca, -0.5] ],
  spesal: [0, 3, 1, 7, 2],
  meta: (name: "lakeDrone", date: "2026-07-15", note: "")
)
```
Loading: unknown keys warn + skip; every value passes `CQSpec.clamp` / matrix clamp;
`version` gates future migrations.

The versioned release bank is deliberately bounded to `example`, `undertow`,
`dust_bloom`, `glass_tide` and `organ_lanterns`. `example` teaches the readable
format; the other four are complete, self-starting scenes and useful morph
endpoints. They do not contain Coco buffer audio. Any other snapshot in the
directory is personal session memory and remains gitignored.

---

## 8. Testing architecture

All are runnable headless from a local shell, CI, or a coding agent through
`tests/run_sclang_gate.ps1`; never invoke `sclang` directly. The wrapper uses
daemon mode, owns and times out only its child PID, validates the expected
machine-readable marker, and leaves a diagnostic log under
`tests/tmp/gate-logs/`, including for failed runs.

| Script | What | Marker |
|---|---|---|
| `tests/compile_check.scd` | class lib compiles, all SynthDefs `.add` without error (no server boot — `SynthDef(...).asBytes` suffices) | `CQ_COMPILE_OK` |
| `tests/run_all.scd` | UnitTests (classes in `tests/classes/*.sc`), including the pure Audio draft/validation contract and the pending-profile roundtrip, atomic promotion, backup and failure adapters | `CQ_TESTS_PASSED` |
| `tests/nrt_safety.scd` | NRT render (scsynth non-realtime, no audio device): worst-case preset (SPEC A3) 90 s → read file, assert peak ≤ −0.75 dBFS, no NaN, |DC| ≤ 0.02 | `CQ_SAFETY_PASSED` |
| `tests/nrt_hold_integrity.scd` | SPEC A4 (lossless hold under speed sweep, correlation ≥ 0.999) and A5 (unity looper energy) | `CQ_HOLD_OK` |
| `tests/manual_checklist.md` | SPEC M1–M8 — human ears only | — |

NRT renders use `Score` + `Server.renderNRT`; they exercise the *real* SynthDefs and
the real preset data — the safety proof covers the actual shipped graph.

---

## 9. Performance notes

- Whole instrument ≈ 1 quantussy synth (5 oscs + S&Hs), 2 coco synths (few BufRd/BufWr,
  gates, amplitude followers), decks, input telemetry, and master. Well under SPEC's 25% budget; no
  demand-rate UGens on hot paths; meters at 20 Hz.
- `bufFrames` ≤ 2^19: Phasor phase stays integer-exact in float32 (< 2^24).
- Block size 64 default. LocalIn (Quantussy ring and each Coco's own matrix-source
  return) and InFeedback (Coco cross) are one block late by design — at 48 k/64
  that is 1.3 ms; document, don't fight.
- GUI updates coalesced: meters via one OSCdef; param echoes via dependants, deferred.

---

## 10. Development environment notes

- Class lib install: add `<repo>/classes` to `sclang_conf.yaml` `includePaths`
  (document exact steps in README for Windows: `%LOCALAPPDATA%\SuperCollider\sclang_conf.yaml`).
  After editing `classes/*.sc`, the IDE needs a class-lib recompile (Ctrl+Shift+L);
  headless test runs always compile fresh.
- Headless runs (local shell, CI, or coding agent):
  `powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 <script>`
  — the dev conf
  (gitignored, machine-specific) adds `<repo>/classes` and `<repo>/tests/classes` to
  includePaths so runs never depend on the user's global conf; sclang path stored in
  a dev-local file (`tests/dev_paths.scd`, gitignored) after first session with the user.
- NRT needs `scsynth.exe` (same install dir); no audio device required.

## 11. Extension points (explicitly out of v1)

- OSC remote control, MIDI clock → spesal sync, >2 Cocos (bus layout already
  generalizes via index arrays), multi-instance via additional servers, per-castle
  range CV (hardware has a switch; a `\rangeMorph` continuous crossfade could be a v2
  experiment). (The pattern event type `\cq` shipped in M3 — see SPEC §6.4 and the
  `Cocoquantus.prCqAction` dispatcher.)
