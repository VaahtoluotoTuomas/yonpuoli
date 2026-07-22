# Sources, lineage, and interpretation

Yölahti is an independent software instrument. It began as a study of Peter
Blasser's Ciat-Lonbarde Cocoquantus but is not presented as a faithful emulator,
circuit model, official port, or substitute for the physical instrument.

This project is not affiliated with or endorsed by Peter Blasser or Ciat-Lonbarde.
Names associated with the original instrument are used to identify technical and
artistic lineage.

## Primary sources

- Peter Blasser / Ciat-Lonbarde,
  [Cocoquantus description and benchmarks](https://www.ciat-lonbarde.net/ciat-lonbarde/cocoquantus/index.html).
  This is the primary source for the dual 8-bit delay architecture, panel
  functions, Quantussy, ranges, Spesal state, Dolby behavior, and the instrument's
  own conceptual vocabulary.
- Peter Blasser / Ciat-Lonbarde,
  [Cocoquantus view document](https://www.ciat-lonbarde.net/ciat-lonbarde/cocoquantus/view.pdf).
- Peter Blasser / Ciat-Lonbarde,
  [published Cocoquantus schematic](https://www.ciat-lonbarde.net/ciat-lonbarde/cocoquantus/cocoquantuschematic.pdf).
- Peter Blasser,
  [current document and project index](https://pblasser.github.io/).

The repository links to these sources; it does not redistribute their images,
documents, recordings, panel graphics, or schematics.

## Secondary technical sources

- Richard Brewster,
  [Explaining the Quantussy Schematic](https://pugix.com/synth/explaining-the-quantussy-schematic/).
  Used to understand the five oscillators, paired sample-and-holds, partner
  selection, and the relationship between triangle, castle, and FM signals.
- Perfect Circuit,
  [Beyond Conventional Synthesis: Worlds Within Cocoquantus](https://www.perfectcircuit.com/signal/ciat-lonbarde-cocoquantus-explained).
  Used as a practical overview and cross-check of the playing model. It is not
  treated as a circuit-level source.

## Project interpretations

The public material does not determine every software behavior. The following are
explicit Yölahti interpretations or software-native designs rather than verified
claims about the hardware:

- the exact eight-state Spesal mapping as four partners × two hold modes;
- the square-stack organ derivation from address-counter bits;
- clock, feedback, converter, dust, preamp, and VCA response curves;
- the 16×22 signed-weight routing matrix and its Schmitt-trigger destinations;
- presets, snapshots, morphing, file decks, recording, stems, Space reverb, and
  all computer/headphone safety layers;
- the Day, Night, Drawer, compass, tide, and weather interfaces;
- performance and CPU tradeoffs required by SuperCollider and Qt.

The authoritative list of deliberate deviations is
[`SPEC.md` §12](SPEC.md#12-deliberate-deviations-from-the-hardware). When a source
is ambiguous, `SPEC.md` labels the software behavior as an interpretation.

## Code and assets

The tracked repository currently contains SuperCollider source and project-written
documentation; it does not contain copied Ciat-Lonbarde images, audio, fonts, or
schematic files. Contributors must document the origin and license of any future
third-party code or asset in this file before it is accepted.
