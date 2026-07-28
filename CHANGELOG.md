# Changelog

This file records release-level changes. Detailed implementation journals remain
under `docs/history/`; the active work order is `PLAN.md`.

The project follows semantic versioning once the first Yölahti public beta is
tagged. Earlier tags describe internal development baselines.

## Unreleased — Yölahti / Nocturne

### Changed

- Adopted **Yölahti** as the independent product identity while retaining the
  `CQ` SuperCollider API and class namespace.
- Reframed the project from a Cocoquantus emulation to an original feedback
  instrument with explicit lineage and non-affiliation.
- Rebuilt the public README around users, architecture, safety, testing,
  authorship, and current status.
- Made Night the sole runtime instrument surface in the accepted N9 cut. The
  Day stage, toolbar destination, four-column builders and its
  two read-only overview drawings are removed; Master and the four-page Drawer
  remain persistent. Night's under-hand coupling chord replaces the permanent
  spesal constellation, while the playable shells plus Workshop replace the
  static Coco memory-path diagram.

### Added

- Source and interpretation bibliography.
- Portfolio case study and AI-assisted development/ownership policy.
- Contribution guide, GPL-3.0-or-later license, and Windows CI gates.
- Playable, captionless castle range ripples on the Night landscape, with
  audible infra/balcium/audio stepping and cable-first interaction.
- Captionless Dolby oysters beneath the Night input/feedback crescents, with
  posture-led off/gate/duck stepping and unchanged cable-first interaction.
- Captionless Night deck-bird gestures: tap to fetch from the configured
  samples folder, wheel to walk the loaded folder, and a short out-and-back
  ruffle for a random sibling, all sharing the filename truth and 120 ms
  successful-load flash. The complete bird interaction passed live review on
  2026-07-28.
- Second-review Night intake bodies: five wide, seeded-irregular source strands
  select or deactivate the CQSpec source directly; an axis-locked 240 px knot
  gesture plays drive vertically and bias horizontally. Drive now weighs the
  Cocoon-bound audio vein at 1.5/2/3 px, while pre/deck slot gains inform route
  visibility. Transient bird flights reveal the direct deck-to-Cocoon path and
  any current bird-to-preamp provenance without hard-wiring a false landscape
  relation. `envA/B` remains the smaller patchable post-preamp branch. The
  complete intake body passed live review on 2026-07-28.
- Recorded preamp loudness compensation as a later equal-loudness DSP
  experiment; N7 deliberately changes only interaction and route drawing.
- A fourth persistent Drawer page, **workshop**: the bay's quiet measurement
  table, carrying the 43 parameters Night and the Master strip do not own so
  the whole instrument is reachable without the Day panel. One centred 1128 px
  rail holds a shallow service band (`deck a · input trims · space · deck b`)
  over a `coco 1 · quantussy · coco 2` body, with no scroll view at the
  1280×720 minimum and surrounding slate — not stretched controls — in a
  larger window. Accepted after five visual and playing review rounds on
  2026-07-28.
- A new Ink **ledger row**: label, one hairline value rail and an exact mono
  value, with unipolar, centre-tick bipolar, integer, toggle-seat and
  enum-seat variants whose ranges, steps and enum order all come from
  `CQSpec`. Both skins draw it from one primitive.
- A machine-checkable reachability gate: Night (31), Master (5) and Workshop
  (43) declare their interactive parameters independently, and a unit test
  proves the three sets disjoint with their union equal to `CQSpec.names`
  itself. Workshop ownership is an explicit manifest, never a complement, so a
  future parameter fails the gate instead of silently counting as covered.

- Two reviewed size steps for the whole Drawer (`CQGuiScale`): `compact` for a
  windowed 1280×720 and `roomy` once a maximized window gives the page room.
  Type, row height, gutters, the shared content rail and the Inspector loom's
  cells all grow at the bigger step, so a large window buys legibility rather
  than empty slate, while a margin to the page edge is always kept. The step
  follows the window through Qt's own resize callback, and every page
  re-measures instead of rebuilding.

### Changed (Nocturne)

- Widened the playable middle of the Quantussy Chaos hand with a `+2.0`
  control curve while preserving direct values, snapshots and the 0–1 DSP
  semantics. Night's storm iris now reveals four planted broken weather rings
  at ¼, ½, ¾ and full, with a living front through them and a proper
  cable-priority hover cue. The curve's listening pass and the rings' visual
  review were accepted on 2026-07-28.
- Gave all four Drawer pages one shared page frame: the same air between the
  tab row and the first ink, the same centred rail, and one content inset so
  the Workshop's labels, the loom's row names and the seed bank's boxes begin
  on the same vertical line. The Inspector had been on a 4 px margin while the
  others used 12, and the Workshop's ink started 8 px further right than
  anything else. Memory's snapshot column and patch fingerprint now reach down
  to the morph bridge instead of stopping at a fixed height — with a bottom
  margin, so the morph bar no longer crowds the Master strip — and the
  fingerprint's cells and row labels grow with the room.
- Raised the persistent chrome to the workbench's floor: the toolbar title and
  destinations, the Drawer's page tabs, and the Master strip's knob labels,
  notice line and elapsed time. The Master strip keeps its 118 px height and
  REC/PANIC their reviewed hit areas — only the ink inside grew. Buttons across
  the chrome and the Drawer are now measured from their own captions.
- Enlarged the Inspector's rotated destination headings and the Memory page's
  box titles and morph readout, which had stayed at the old 9–10 px while
  everything around them grew.
- Aligned the toolbar title to the rule beneath it, gave the Master strip's
  knob labels clearance from their value arcs, moved the loom's column
  headings clear of the preset row, and unpacked the Memory fingerprint's
  crowded top — its box title, the snapshot name and the explanatory line now
  occupy their own rows, and that line is set one rank smaller because it is
  information rather than a heading.
- Finished the Drawer selection cues: the Inspector matrix now gives the exact
  hovered knot a light spark wash and outline, while the Inspector preset and
  Memory Morph Bridge choice boxes use the same hover. Their opened lists are
  drawn in the Ink palette instead of falling back to native Qt chrome.
  Memory's fingerprint comparison note is now the page's tiny type rank with
  explicit clearance from the `decks` heading.
- Corrected the final N8 hover event path after live review: Inspector cells
  and every selectable row in an opened Ink choice list now respond to Qt's
  ordinary no-button mouse-over event, not only pressed mouse movement. The
  matrix cue and list-row wash/outline were strengthened for Day visibility.
  Short text bands now derive vertical ascent/descender clearance from
  `CQGuiScale`, preventing the Memory comparison note from clipping.
- Removed the remaining vertical clipping source from that comparison note:
  its layout inset is horizontal-only instead of consuming 8 px above and
  below the text. Memory's saved-snapshot list now highlights the hovered row,
  and all three Memory tool titles share a slightly deeper step-aware top
  inset.
- Replaced both platform-dependent remnants after live review. The Memory
  comparison note is now Pen-drawn in a measured, roomy band, so native label
  ascent/descent clipping cannot cut its glyphs. Saved snapshots now use an
  Ink-painted persistent list whose drawing, hover and click share one exact
  row height; the highlight can no longer land one native row above the
  pointer.
- Hardened every headless SuperCollider gate against a recurring Windows hang:
  the shared runner uses daemon mode, a child-only watchdog, expected-marker
  validation and per-run diagnostic logs; agents and CI now use that same
  entry point.
- Raised the Drawer's type above the Day panel's fixed 10 px and gave all four
  pages one shared content rail — the Spellbook previously used neither.
  Subgroup headings gained their own rank (line ink, a lighter rule from the
  end of the name to the island's edge) instead of being a fainter whisper.
  Day and Night keep §16.10's fixed px sizes unchanged.

- **Moved** the interactive `micGain` control and its warning owner from the
  Day sources column to the Workshop page, keeping SPEC §6.2's one-mic-path
  law while making the mic reachable without Day. Day retains only a passive
  `mic · drawer / workshop` pointer, the Night mic sprout stays read-only, and
  the persistent `MIC LIVE` notice still cannot be silenced by closing the
  Drawer.

### In progress

- Nocturne: complete the Night playing surface, evaluate Night-only sessions, and
  decide the future of Day.
- Public-beta screenshots, short performance video, and audio examples.
- Live stem verification on an interface with at least four outputs.

### Fixed

- Made the Drawer's bigger size step actually reachable on a real laptop. Its
  thresholds had been written by hand instead of derived from the step's own
  metrics, and a 1080p screen at Windows' common 125 % scaling reports a
  logical page about two pixels too short to qualify — so maximizing only
  widened the margins and never grew the type. The floors are now derived, the
  roomy rows are tightened to match, a wide-but-short page spends its width on
  the rail rather than on slate, and the step is re-read from two resize hooks
  so a window resized while the Drawer was closed cannot leave a stale size.
- Kept the Night provenance label's ground erasure off the intake strands it
  names: the erasure now measures the seeded delta inside its own band and
  gives way outward, instead of trusting a constant inset that cut the tops
  off two strands at the minimum stage size.
- Kept Night bird/root provenance flights continuous across the landscape by
  moving them and their high-contrast transient labels to one hit-transparent
  layer above all local Qt drawing rectangles.
- Smoothed each preamp's green output side-growth into one cubic under a
  transparent `envA/B` endpoint, removing the midpoint repair seam, and spread
  the five source mouths farther apart for clearer individual roots.
- Made the Night bird ruffle reliable in the live Windows/Qt window: the bird
  now claims its drag, follows the global cursor while held, and keeps a clear
  reversal valid even after repeated or broad movement.
- Preserved cable hover and cable-first presses where a planted cable crosses
  a Night Dolby oyster.
- Repainted the real shell spiral and radial stem beneath each opaque yellow
  organ-output view, removing the invisible rectangular break in the playhead
  spiral.

## archipelago-1.0 — 2026-07-22

- Completed and accepted the Archipelago G1–G3 GUI epic.
- Added the Ink Day panel, cable grammar, Drawer/Inspector/Spellbook/Memory,
  telemetry-driven surfaces, the Night landscape, compass, tide, and weather.
- Closed the full interaction, listening, scaling, CPU, and resource review gates
  documented in `docs/history/manual_checklist_archipelago.md`.

## v1.0 — 2026-07-19

- Completed the initial sound engine, code API, GUI/controller layer, file decks,
  snapshots, recording, optional stems, Space reverb, and automated/manual gates.
- Preserved as the pre-Archipelago baseline. Hardware stem listening remained
  explicitly deferred.
