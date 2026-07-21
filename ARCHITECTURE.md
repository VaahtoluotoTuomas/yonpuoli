# ARCHITECTURE.md — CocoquantusSC

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
cocoquantus-sc/
├── SPEC.md  ARCHITECTURE.md  CLAUDE.md  README.md
├── boot.scd                    // user entry point: loads config, Cocoquantus.boot
├── live.scd                    // performance scratchpad: example blocks, patch recipes
├── config.example.scd          // template → user copies to config.scd (gitignored)
├── config.scd                  // machine-specific (gitignored)
├── classes/                    // sclang class library (add to includePaths, see §10)
│   ├── Cocoquantus.sc          // facade singleton (+ CQ alias subclass)
│   ├── CQSpec.sc               // parameter registry: ControlSpecs, defaults, 16n map
│   ├── CQConfig.sc             // config load + validation + defaults
│   ├── CQGraph.sc              // buses, groups, synth spawning, boot/teardown
│   ├── CQSynthDefs.sc          // all SynthDefs (pure server-side DSP)
│   ├── CQState.sc              // value store, clamped setters, dependant notify
│   ├── CQMatrix.sc             // routing matrix: name↔index, weights, presets, shake
│   ├── CQAudition.sc           // transient matrix-preview bookkeeping
│   ├── CQInspector.sc          // Drawer grid touch/listen + fine-edit transaction
│   ├── CQSpellbook.sc          // committed matrix notification log + code export
│   ├── CQMidi16n.sc            // 16n binding, smoothing, takeover
│   ├── CQGui.sc                // panel window (view-only client of CQState)
│   ├── CQGuiDrawer.sc          // Inspector / Spellbook / Memory composition
│   ├── CQSnapshots.sc          // save/recall/morph
│   ├── CQRecorder.sc           // disk recording (master + stems)
│   ├── CQDeck.sc               // transactional file player control
│   └── CQDeckPair.sc           // atomic linked-stereo coordinator
├── presets/                    // *.cqsnap.scd snapshot files (text, versioned)
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
     ├─ gFx         // \cqSpace
     └─ gMaster     // \cqMaster (mix, safety, meters, stems), \cqAura (telemetry),
                    // \cqRecorder when active
```

`\cqAura` (GUI_SPEC §9) sits here purely because gMaster runs last: it reads the
finished `aModSrc` bus with plain `In.ar` — no InFeedback needed — rectifies and
lags at audio rate so audio-range castles cannot alias into the control stream,
and reports 16 magnitudes at 15 Hz. It writes to no bus and no output, so it is
outside every safety path; a headless session simply has no listener.

Rationale: modulation sources are computed before their consumers within the same
block; the only intentional one-block delays are Coco↔Coco cross-signals (InFeedback)
and the Quantussy's internal ring (LocalIn/LocalOut).

### 3.2 Buses

| Name | Rate | Ch | Writer → Reader | Purpose |
|---|---|---|---|---|
| `aModSrc` | audio | 16 | quantussy, input, cocos → cocos, quantussy, decks, master | mod sources, canonical index order below |
| `aPreA`, `aPreB` | audio | 1 | input → cocos | preamp outputs |
| `aDeckA`, `aDeckB` | audio | 1 | decks → input, cocos | deck audio |
| `aQMix` | audio | 1 | quantussy → cocos | castle audio mix (per-castle sends) |
| `aCoco1`, `aCoco2` | audio | 1 | cocos → master, cocos (cross via InFeedback) | post-converter, post-level, pre-pan taps |
| `aSpace` | audio | 2 | cocos → space | space send |
| `aSpaceRet` | audio | 2 | space → master | space return |
| `aDry` | audio | 2 | input, decks → master | untreated source monitor feed (`masterDry`, SPEC §5.6) |
| `wDest[dest]` | control | 16 | language (CQMatrix) → consumer synth | one 16-ch weight bus per matrix destination (22 total) |
| (params) | — | — | language → synth args via `.set` | everything in CQSpec that isn't matrix |

**Canonical `aModSrc` index order (fixed, used by CQMatrix and all synths):**
`0–4 tri1..5 · 5–9 cas1..5 · 10 envA · 11 envB · 12 coco1 · 13 coco2 · 14 org1 · 15 org2`.
Channels 12–15 are written by the Coco synths themselves; any consumer in `gCocos` or
earlier reads those four via `InFeedback.ar` (one block late — accepted and documented).

### 3.3 Matrix destination list (fixed order, 22)

`coco1_spaf, coco1_inVca, coco1_fbVca, coco1_flip, coco1_skip,`
`coco2_spaf, coco2_inVca, coco2_fbVca, coco2_flip, coco2_skip,`
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
  drive → asymmetric fuzz, gentle HPF 20 Hz:
  ```supercollider
  var shaped = ((x * drive) + bias).tanh - bias.tanh;   // asymmetric soft clip
  ```
- Envelope followers: `Amplitude.ar(pre, 0.003, 0.15)` → `aModSrc[10..11]`.
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
var clockHz = (speed * (2 ** (spafSum * spafKnob))).clip(1500, SampleRate.ir);
var ratio   = clockHz / SampleRate.ir;               // ≤ 1.0
var dir     = Select.ar(flipGate, [DC.ar(1), DC.ar(-1)]);   // flip = matrix gate OR param
var phase   = Phasor.ar(skipTrig, ratio * dir, 0, BufFrames.ir(buf)); // address counter
var here    = BufRd.ar(1, buf, phase, loop: 1, interpolation: 1);
// ^ MUST be created before BufWr: reads the cell value BEFORE overwrite = the DAC out

var inSig   = Mix([aPreX * slotPre, deck * slotDeck, qmix * slotQ, cross * slotX]);
var inVca   = (inLevel + inVcaSum).clip(0, 2);       // verso/inverso via signed weights
var fbVca   = (fbLevel + fbVcaSum).clip(0, 2.5);
var inCh    = dolby(inSig, dolbyInMode, keyEnv: fbEnv);   // 3-mode gate/duck helper
var fbCh    = dolby(here,  dolbyFbMode, keyEnv: inEnv);
var record  = (inCh * inVca) + (fbCh * fbVca) + (WhiteNoise.ar(dust) * LFNoise2.ar(3));
record      = Select.ar(wrap, [record.clip2(1), record.wrap2(1)]);   // ADC rails
record      = record.round(2 ** (1 - bits));                        // requantize EVERY pass
var write   = Select.ar(hold, [record, here]);       // hold = identity rewrite (lossless)
BufWr.ar(write, buf, phase, loop: 1);

var outTap  = Select.ar(interpSmooth, [
                here,                                              // \zoh — authentic
                BufRd.ar(1, buf, phase, loop: 1, interpolation: 4) // \smooth
              ]);
var organ   = Mix(Pulse.ar(clockHz / [128, 256, 512], 0.5, [0.5, 0.3, 0.2]));
var voice   = (outTap + (organ * organLvl * 0.3));
// out: aCocoN = voice * level (lagged); aModSrc[12/13] = normalized tap;
//      aModSrc[14/15] = organ; aSpace += pan-placed send
```

Why this reproduces SPEC §3: last-write-wins into each cell = ZOH ADC at `clockHz`
with no AA filter (aliases in); non-interpolating read = ZOH DAC (aliases out);
`here` read pre-write = the cell about to be overwritten = full-buffer delay;
changing `ratio` moves both heads together = tape-style re-pitch of held content;
`hold` writes back the exact value just read (interpolation 1 at the same integer
phase) = bit-exact lossless hold at any speed; clip→quantize inside the loop =
recirculating degradation and bounded feedback; `flip` reverses the counter direction
(reverse loops when holding, head-crossing reverse-delay chaos when recording —
authentic-adjacent, keep); `skip` resets the Phasor.

`dolby(sig, mode, keyEnv)` helper (function in the def):
```supercollider
var env = Amplitude.ar(sig, 0.003, 0.08);
Select.ar(mode, [ sig,
    sig * Lag.ar(env > 0.02, 0.005, 0.05),        // \gate: self-expander
    sig * (1 - (keyEnv * 0.9)).clip(0, 1) ]);     // \duck: "other-puncher"
```

### 4.5 `\cqSpace` — small FDN (vanilla, no plugins)

4 delay lines (`DelayC`, ~29/37/44/53 ms × size), Householder feedback matrix,
one-pole damping at `spaceDamp`, in from `aSpace`, out `aSpaceRet`. ~20 lines,
tasteful and dependency-free. Internal feedback gain ≤ 0.98 hard cap.

### 4.6 `\cqMaster` — the sacred chain

```supercollider
var mix = (pan-placed cocos) + (aSpaceRet * spaceMix) + (drySources * masterDry);
mix     = mix * masterAmp.lag(0.1) * (1 - mute.lag(0.03));
mix     = LeakDC.ar(mix);
mix     = (mix * 0.5).tanh * 2 * 0.5;                    // −6 dB headroom glue
mix     = Limiter.ar(mix, 0.89, 0.01);                   // ceiling −1 dBFS
mix     = Select.ar(CheckBadValues.ar(mix, post: 0) > 0, [mix, DC.ar(0)]);
mix     = mix * EnvGen.kr(Env([0,0,1],[0.5,2]));         // boot silence + fade
Out.ar(outChans, mix);
// stems: same LeakDC→Limiter→squelch per stem before Out.ar(stemOuts, ...)
// meters: SendPeakRMS 20 Hz → \cq_meters; watchdog: RMS>-6dBFS for 5s → SendReply \cq_hot
```

Language side on `\cq_hot`: engage −6 dB trim (a lagged synth arg), GUI notice
(SPEC §5.6, config `autoTrim`).

---

## 5. Class layer (language side)

| Class | Role | Key methods / notes |
|---|---|---|
| `Cocoquantus` | facade singleton; owns boot lifecycle and all subsystems | Explicit `stopped/booting/running/stopping/failed` state plus monotonically increasing session generation; stale callbacks self-cancel. `*boot(configPath)`, waitable `*free(onDone)`, strong `*reboot`, and the existing control API. `CQ : Cocoquantus {}` empty subclass = short alias (classvars shared) |
| `CQSpec` | parameter registry | `*specs` (IdentityDictionary name → ControlSpec+default+lag+16n slot), `*clamp(name, val)`, `*default(name)`, mirrors SPEC §5 tables 1:1 |
| `CQConfig` | config load/validate | reads `config.scd`, fills defaults, validates channels/paths, hard-fails informatively |
| `CQGraph` | server plumbing | allocates buses/groups/buffers, builds ServerOptions from config, spawn order, `s.sync` between stages, teardown, CmdPeriod re-arm hook |
| `CQState` | value store + funnel | `set(name, val)` → clamp → `synth.set` / bus write → `changed(\param, name, val)`. `get`, `asEvent` (for snapshots), `load(event)`. Enum params travel to the server as indices into the CQSpec value list; every other layer writes symbols through the funnel |
| `CQMatrix` | routing weights | name↔index maps (source 16, dest 22 — canonical orders §3.2/3.3), metadata-bearing single-cell writes plus deep `fullMatrix`/clamped `writeMatrix` batch transactions, weight cache mirrored to `wDest` buses, existing triple `asArray/load`, presets and shake |
| `CQAudition` | transient patch preview | Serverless one-candidate `{src,dest,prevW,audibleW}` state machine; candidate switches and every §4.1 cancellation path restore through `CQMatrix.patch(... committed:false)`, while plant commits exactly the audible weight |
| `CQInspector` | Drawer precision gesture state | Shares `CQAudition`; press previews ±0.3, vertical frames are transient, and release publishes one committed fine-edit plus one shared GUI-undo entry; lifecycle cancellation restores the pre-gesture cell |
| `CQGuiMatrix` | experimental Inspector patch loom | Exact canonical 16×22 single-`UserView` field with 48×24 px hit cells, grouped destination islands, family-marked rows, weighted/polarity-shaped knots, persistent route selection and hover row/column focus; no inner scroll or new state funnel |
| `CQSpellbook` | read-only committed patch journal | Hooks `CQMatrix` notifications below every client, diffs against the last committed full matrix, excludes `committed:false` frames, exposes row→batch metadata for persistent GUI selection, and exports copyable `CQ.patch(...)` lines or preset data; it does not execute or edit code |
| `CQMidi16n` | controller layer | `MIDIdef.cc` per fader from config map, 0–127 → spec map, optional `\pickup` takeover, hot-plug via `MIDIClient` device watch; all writes via `CQState` |
| `CQGui` | panel | pure view: renders from `CQState`/`CQMatrix` dependant notifications, sends via funnel; composes the Day/Night shell and `CQGuiDrawer`; meters/LEDs come from namespaced OSCdefs; `/cq_aura` magnitudes stay `nil` until the server reports, repaint only the jack views and cable layer, and are skipped entirely when nothing moved; everything UI inside `{ }.defer`; closes cleanly on `*free` |
| `CQSnapshots` | memory | `save(name)` → `(version: 1, params: state.asEvent, matrix: matrix.asArray, spesal: [..], meta: (...))` `.asCompileString` → `presets/name.cqsnap.scd`; `recall` clamps everything on load; `morph(a, b, dur)` = Routine at 30 Hz interpolating continuous params + weights, discrete switch at midpoint; mic gain never auto-recalled above 0 (SPEC §7) |
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

---

## 6. Boot sequence (`Cocoquantus.boot`)

The facade publishes an explicit lifecycle state. Every accepted boot captures a fresh
session generation and local graph/state/matrix references; every async stage checks the
generation after blocking work. `free`, reboot, CmdPeriod and boot failure invalidate it
immediately, so a late callback can only clean its own resources. A stage failure rolls
back GUI/controllers/OSCdefs/decks/recorder and the partial graph, then enters `failed`.

1. Load + validate config (`CQConfig`). Fail hard with a readable message if invalid.
2. Build `ServerOptions` (device, SR, block, hardwareBufferSize, channel counts).
3. `server.waitForBoot` → allocate buffers (2 coco RAM, deck placeholders) and buses;
   create groups in order; `s.sync`.
4. Add SynthDefs (`CQSynthDefs.add(config)` — defs parameterized by config where
   needed); `s.sync`.
5. Spawn synths in group order; write CQSpec defaults + power-on state (dust-filled
   buffers via one-shot noise-fill routine, `hold = 1`, matrix empty); `s.sync`.
6. Init MIDI (guarded — absence of 16n is not an error), OSCdefs, recorder standby.
7. Open GUI (unless `headless: true` in config).
8. Release master fade → 2 s ramp to nominal (SPEC safety #5).
9. Install `CmdPeriod` and server-quit hooks. Either invalidates the session; CmdPeriod
   marks the killed graph, while an unexpected scsynth quit also tears down language-side
   clients. Both fail toward silence and require an explicit rebuild.

Teardown (`*free`): recorder stop → master fade 100 ms → free groups/buses/buffers →
close GUI/MIDI/OSC defs. It is idempotent, invalidates pending work before the fade, and
accepts an `onDone` callback; a boot requested during stopping is queued behind it.

`CQ.reboot` is intentionally stronger than `free` + `boot`: it calls `Server.reboot` so
boot-only options take effect. The project uses `Server.default`, so this explicit command
also terminates unrelated synths on that shared server; ordinary `CQ.free` never quits it.

---

## 7. Snapshot file format

```supercollider
// presets/lakeDrone.cqsnap.scd — plain text, git-friendly
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

---

## 8. Testing architecture

All runnable headless by Claude Code; each prints a single machine-readable marker as
its last line and sets exit code.

| Script | What | Marker |
|---|---|---|
| `tests/compile_check.scd` | class lib compiles, all SynthDefs `.add` without error (no server boot — `SynthDef(...).asBytes` suffices) | `CQ_COMPILE_OK` |
| `tests/run_all.scd` | UnitTests (classes in `tests/classes/*.sc`): `CQSpecTest` (clamp fuzzing incl. NaN/inf/nil/Symbol), `CQMatrixTest` (name↔index, weight bounds, shake determinism w/ seed), `CQSnapshotTest` (roundtrip equality, malformed-file rejection), `CQConfigTest` | `CQ_TESTS_PASSED` |
| `tests/nrt_safety.scd` | NRT render (scsynth non-realtime, no audio device): worst-case preset (SPEC A3) 90 s → read file, assert peak ≤ −0.75 dBFS, no NaN, |DC| ≤ 0.02 | `CQ_SAFETY_PASSED` |
| `tests/nrt_hold_integrity.scd` | SPEC A4 (lossless hold under speed sweep, correlation ≥ 0.999) and A5 (unity looper energy) | `CQ_HOLD_OK` |
| `tests/manual_checklist.md` | SPEC M1–M8 — human ears only | — |

NRT renders use `Score` + `Server.renderNRT`; they exercise the *real* SynthDefs and
the real preset data — the safety proof covers the actual shipped graph.

---

## 9. Performance notes

- Whole instrument ≈ 1 quantussy synth (5 oscs + S&Hs), 2 coco synths (few BufRd/BufWr,
  gates, amplitude followers), decks, FDN, master. Well under SPEC's 25% budget; no
  demand-rate UGens on hot paths; meters at 20 Hz.
- `bufFrames` ≤ 2^19: Phasor phase stays integer-exact in float32 (< 2^24).
- Block size 64 default. LocalIn (quantussy ring) and InFeedback (coco cross) are one
  block late by design — at 48 k/64 that is 1.3 ms; document, don't fight.
- GUI updates coalesced: meters via one OSCdef; param echoes via dependants, deferred.

---

## 10. Development environment notes

- Class lib install: add `<repo>/classes` to `sclang_conf.yaml` `includePaths`
  (document exact steps in README for Windows: `%LOCALAPPDATA%\SuperCollider\sclang_conf.yaml`).
  After editing `classes/*.sc`, the IDE needs a class-lib recompile (Ctrl+Shift+L);
  headless test runs always compile fresh.
- Headless runs (Claude Code):
  `"<sclang.exe path>" -l tests/sclang_conf_dev.yaml -i none <script>` — the dev conf
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
