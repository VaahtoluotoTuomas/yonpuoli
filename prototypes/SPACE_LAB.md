# Yönpuoli Space Lab

> Listening decision, 2026-07-28: this effect line is closed. `utility`,
> `clockField` and `confluence` were rejected. `memoryHalo` remains only as
> research material for a future Coco-integrated memory-read mode; none of the
> four is a production Space candidate. Keep this lab disposable and outside
> `CQSpec`, the GUI and the production graph.

This is an archived disposable listening experiment. It does not change
`CQSpec`, snapshots, MIDI mappings or the GUI. The production `\cqSpace`
chain was removed after the listening decision, so `space_lab.scd` now exits
with an archive notice instead of trying to recreate a hidden production
return. `space_lab_defs.scd` and the offline comparison remain runnable;
`memoryHalo` is the retained research artifact.

The original live harness required the pre-retirement placeholder Space node
and return bus. Use the definitions or the offline test for future research;
do not reconnect the archived harness to the current production graph.

## Four candidates

1. **utility** — an eight-line, dark and dense FDN. This is the deliberately
   ordinary reference: stable, legible and useful.
2. **clockField** — a six-line echo field whose delay constellation follows the
   two actual address-derived organ clocks. Quantussy adds only a small,
   smoothed weather term.
3. **memoryHalo** — four read-only heads per Coco buffer. It has no recursive
   reverb tail: held memory becomes a spatial constellation, while recording
   memory produces displaced moving shadows.
4. **confluence** — not a reverb. Each Coco is sampled by the other Coco's
   address-derived organ clock. One relationship macro moves from sum/difference
   through reciprocal sample-and-hold to multiplication, producing a third
   voice from the two clock ratios.

Only one candidate runs at a time.

## Controls

```supercollider
~cqSpaceLab.playVariant(\utility);
~cqSpaceLab.playVariant(\clockField);
~cqSpaceLab.playVariant(\memoryHalo);
~cqSpaceLab.playVariant(\confluence);

~cqSpaceLab.nextVariant;          // manual cycle
~cqSpaceLab.compareAll(25, 5);    // 5 s dry + 25 s wet for all four

~cqSpaceLab.setWet(0.35);          // exact dry reference at 0
~cqSpaceLab.setSends(0.65, 0.65);  // Coco 1 / Coco 2
~cqSpaceLab.setShape(0.6);         // reach; candidate 4: relationship
~cqSpaceLab.setMotion(0.3);
~cqSpaceLab.setTone(0.46);
~cqSpaceLab.setLevel(0.72);         // calibrated return trim
~cqSpaceLab.showStatus;
```

The four returns are loudness-calibrated against a unity Space return at
`level 0.72`. With the default `wet 0.35`, the return is roughly 9 dB below
that reference and should be plainly audible beside the dry Cocos. `wet 0.9`
is intentionally a strong, near-unity effect level. Use `level` only as a
shared final trim when comparing; changing it for every candidate defeats the
calibration.

The source requirements differ. `utility` should react to any audible Coco
loop, so use it first as the routing check. `clockField` also needs the Coco
address clocks to be moving. `memoryHalo` becomes clearest when the buffers
contain distinct material, and `confluence` is most revealing when both Cocos
are audible and run at contrasting speeds.

`~cqSpaceLab.freeLab` removes the lab and restores the Space mix and both Coco
sends that were active before the lab started. `CQ.panic`, the master volume
and the recorder continue to work normally throughout the experiment.

## One-session comparison

Use the same source and do not tune a candidate for longer than the others.
Keep `wet`, sends and master volume fixed while comparing identities.

1. Start with a held, clearly articulated field-recording loop in Coco 1 and a
   contrasting sustained or noisy loop in Coco 2.
2. Run `~cqSpaceLab.compareAll(25, 5)` for a first blind-ish pass. It gives each
   candidate the same five-second dry reference followed by 25 seconds wet.
3. For every candidate, try `setShape(0.25)`, `setShape(0.6)` and
   `setShape(0.9)`.
4. Sweep one Coco speed slowly while the other remains fixed. Then exchange
   their roles.
5. Open both Cocos for recording and ride feedback around the echo/loop/bloom
   region. Check whether the candidate clarifies or smears that transition.
6. Raise Quantussy chaos and patch depth. A candidate should respond without
   making every patch sound like the same effect.
7. Return to `setWet(0)` after each candidate. Judge identity and loss of
   clarity, not loudness.

Suggested note headings:

- new musical act made possible;
- still sounds like Yönpuoli / sounds pasted on;
- useful range of `shape`;
- response to speed and Quantussy;
- stereo image and mono centre;
- tail or persistence after the source changes;
- fatigue after one minute;
- keep / hybridize / reject.
