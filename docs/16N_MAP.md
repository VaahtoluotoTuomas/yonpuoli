# 16n fader map

Quick reference for the atovproject 16n faderbank. Normative source is
[`SPEC.md` §6.1](../SPEC.md); this page only makes it glanceable. The map is
remappable in `config.scd`, and the instrument plays fully without the 16n.
The current feature set was accepted on 2026-08-08; the longer physical exercise
in `tests/manual_checklist.md` remains optional regression guidance, not a 1.0
release blocker.

```
 ── COCO 1 ─────────────────────  ── COCO 2 ─────────────────────  ── SHARED ────────────────────────
  F1     F2     F3    F4    F5     F6     F7     F8    F9   F10    F11    F12    F13    F14   F15   F16
  In   Speed   Spaf   Fb  Level    In   Speed   Spaf   Fb  Level   Freq  Chaos  Drive   Ring Depth  Vol
  ▯      ▯      ▯     ▯     ▯      ▯      ▯      ▯     ▯     ▯      ▯      ▯      ▯      ▯     ▯     ▯
  32     33     34    35    36     37     38     39    40    41     42     43     44     45    46    47
                                                                                  ⇄      ⇄
```

Two identical Coco strips in signal order, then six shared faders. The same
finger position means the same thing on either voice, and **Level ends the strip**
exactly as a channel fader ends a mixer strip. `⇄` = one fader writes both
channels.

## Coco strips (F1–F5 = Coco 1, F6–F10 = Coco 2)

| # | Param | Range (default) | What it does |
|---|---|---|---|
| 1 | `In` | 0–2 lin (0.8) | Level into the 8-bit converter. Unity at 1.0; **above that it overdrives** — this is character, not just gain. |
| 2 | `Speed` | 1.5–48 kHz exp (24 k) | The RAM clock. Re-pitches what is already in the buffer *and* changes loop length and fidelity together. Bottom ≈ 87 s loop, top ≈ 2.7 s. |
| 3 | `Spaf` | ±4 oct (0) | The `^` knob: how deep and which direction the matrix modulates Speed. Does nothing until something is patched to `cocoN_spaf`. |
| 4 | `Fb` | 0–1.25, tapered (0.35) | Loop feedback. **Half-way = 0.85, unity ≈ 74 % of travel, top 1.25.** Above unity it grows into saturated, quantized mayhem that stays loudness-stable — the clip inside the loop bounds it. |
| 5 | `Level` | 0–1.5 exp (0.8) | This voice's output into the mix. **Not linked to the other Coco** — balancing the two against each other is the point. |

## Shared section

| # | Param | Range (default) | What it does |
|---|---|---|---|
| 11 | `qFreq` | 0.05–500 Hz exp (4.0) | Quantussy master frequency: scales all five castle rates at once. |
| 12 | `qChaos` | 0–1, curve +2 (0.15) | Depth of the internal castle→FM feedback. 0 = stable tones and LFOs, mid = drunken stepped wander, top = hyperchaos. The hand map is deliberately non-linear: mid-fader ≈ 0.27. |
| 13 | `preADrive` + `preBDrive` ⇄ | 1–40 exp (2.0) | Preamp fuzz into both Cocos. Unity-ish ≤ 2. Loudness is partly compensated (−1 dB per Drive octave), so this reads as timbre rather than a volume ramp. |
| 14 | `coco1SlotQ` + `coco2SlotQ` ⇄ | 0–1.5 lin (**0.0**) | **The ring's audio gate.** How much of the Quantussy audio mix enters both Cocos. See the note below — this is the one fader that starts fully shut. |
| 15 | `patchDepth` | 0–1 lin (1.0) | Global scaler on every matrix weight: how alive the patch is. Fans out to both Cocos, the Quantussy and both decks. |
| 16 | `masterVol` | −60–0 dB (−12) | Output. Lagged 100 ms. |

## Three things worth knowing

**F14 starts at zero, and at boot it opens one voice, not five.** `qMix` has
exactly one writer (Σ castle triangles × their `cKSend`) and exactly one reader
(this slot gain). At boot only castle 5 sends — it is the single castle in
`\audio` range with a non-zero send. So F14 is the *performance gate* on whatever
the ring is currently sending; which castles send is a `cKSend` setup decision in
the Workshop page. With F14 shut, F11 and F12 shape a voice you cannot hear.

**Speed's floor is the knob's floor, not the clock's.** The fader bottoms out at
1500 Hz, but matrix modulation can push the clock an octave lower, to 750 Hz —
the hardware's "ground jack" trick, deliberately out of reach of the hand alone.

**Takeover.** `\jump` (default) writes the moment a fader moves. `\pickup`
re-engages a fader only after it crosses the parameter's current value — worth
switching to when you recall snapshots mid-set, since a recall moves values out
from under the faders.

## What the faders deliberately do not reach

A fader is worth most where no other hand reaches, so anything already playable
elsewhere stayed off the map:

| Not on the faders | Where it lives instead |
|---|---|
| `deckALevel`, `deckBLevel` | crescents under the birds' wings in the Night map |
| `deckARate`, `deckBRate` | matrix destinations (`deckA_rate`, `deckB_rate`) + Workshop |
| `Hold`, `Flip`, skip, spesal, dolby | Night map bodies + the keyboard (`1/2`, `q/w`, `a/s`, `3–7`) |
| `preASrc`/`preBSrc`, `preABias`/`preBBias` | the preamp roots in the Night map |
| `cKRange` | the castles' range ripples in the Night map |
| `Organ`, `SlotX`, `Dust`/`DustGrain`, `cKSend` | each Cocon organ calyx, the two-way bridge valves, the inner-lower Dust veils and the castles' radial tributaries in Night (`SlotQ` also has a Night gill) |
| `masterDry`, `monitorMute` | the Master strip |
| `Pan`, `SlotPre`, `SlotDeck`, `cKOff`, `liveGain`, `micGain`, `Bits`, `Wrap`, `Interp` | the Drawer's Workshop page |

All 76 parameters are also reachable from code (`CQ.set`) and from snapshots.
