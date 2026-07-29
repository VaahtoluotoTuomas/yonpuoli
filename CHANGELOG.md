# Changelog

This file records release-level changes. Detailed implementation journals remain
under `docs/history/`; the active work order is `PLAN.md`.

The project follows semantic versioning once the first Yölahti public beta is
tagged. Earlier tags describe internal development baselines.

## Unreleased — Yölahti / Nocturne

### Changed

- Audio now distinguishes a responding engine from an unresponsive registered
  server. A clean page exposes `restart audio` (with `no changes` in place of a
  misleading inert discard), and recovery forces the exact hung local server
  process down, waits for process/status teardown, then boots instead of racing
  a second process onto its port.
- Root-caused the apparent AIO-4 audio regression to live
  `_ListAudioDevices` enumeration, not the raw-input telemetry: the hang
  reproduced after `SendPeakRMS` was withdrawn, while removing enumeration
  restored sound, Night motion and repeated `/status` replies. The reinstated
  raw tap passed its isolated split-WASAPI recheck: both presence rows reacted
  while the same server PID kept processing audio and answering three external
  status probes.
- Moved directional audio-device discovery to a confirmed stopped-server
  boundary before a fresh boot or inside `Server.reboot`'s server-off callback.
  The running Audio page now consumes only that memory snapshot and never calls
  `_ListAudioDevices`. Its local recommended/all toggle hides exact legacy
  Windows host prefixes by default while retaining every exact active/draft
  endpoint; scan failure remains nonfatal and falls back to configured or stale
  cached devices. The split-WASAPI physical gate passed with sound, Night
  motion and presence uninterrupted while toggling both views, and a clean
  Audio restart rebuilt the GUI and restored the same working audio session.
- Moved the technical Audio page to the Drawer's rightmost tab, after the
  Inspector, Workshop, Spellbook and Memory musical/workflow pages.
- Adopted **Yölahti** as the independent product identity while retaining the
  `CQ` SuperCollider API and class namespace.
- Reframed the project from a Cocoquantus emulation to an original feedback
  instrument with explicit lineage and non-affiliation.
- Rebuilt the public README around users, architecture, safety, testing,
  authorship, and current status.
- Made Night the sole runtime instrument surface in the accepted N9 cut. The
  Day stage, toolbar destination, four-column builders and its
  two read-only overview drawings are removed; Master and the Drawer
  remain persistent. Night's under-hand coupling chord replaces the permanent
  spesal constellation, while the playable shells plus Workshop replace the
  static Coco memory-path diagram.
- Retired the rejected production Space/FDN chain after comparative listening:
  its five parameters, Coco sends, F14 mapping, GUI controls, buses, node and
  master return are removed. F14 is intentionally unassigned, old snapshot
  fields are ignored safely, and the Master keeps open air for the final
  instrument-mode phase.

### Added

- AIO-4 signal presence: `\cqInput` reports canonical raw live L/R/mic peaks
  before gain and preamp selection at 15 Hz, while Audio reuses the existing
  post-safety master meter. `CQGui` caches both streams, coalesces their
  combined 20/15 Hz arrivals to at most 15 visible page notifications, skips
  hidden Audio paints, primes from cache on open and releases the owner plus
  namespaced OSCdef on teardown.
- AIO-3 confirmed Audio restart: inline impact confirmation, active-recorder
  gate, facade-owned generation-safe candidate reboot, exactly one automatic
  last-working rollback, terminal silent failure diagnostics and symbolic
  Audio-tab reopen. Successful audio is promoted only after the full graph
  reaches running; a failed final rename remains `running · not saved` and can
  be retried without another reboot. The physical split-WASAPI candidate
  roundtrip also passed at server block 64→128→64: both values persisted
  exactly, both rebuilt GUIs restored sound and Night motion, and the final
  server remained externally responsive.
- `CQAudioReconfigure`, a serverless candidate/rollback coordinator with
  explicit lifecycle callbacks and hermetic success, rollback, stale-callback,
  recorder-block and retry tests.
- AIO-2 draft editing for all nine Audio startup fields, with one-based channel
  presentation, exact invalid/dirty states and discard that never touches the
  running server. The injected serverless discovery adapter uses directional `inDevices`/
  `outDevices`, removes only exact duplicates, limits long menus to sixteen
  scrollable one-line rows and pre-ellipsizes long names so row highlight and
  text cannot disagree. Embedded CR/LF/tab/control whitespace from Windows
  endpoint names is collapsed only in display text; exact model data survives.
- `CQAudioProfileStore`: validated `.pending` roundtrip, last-working
  preservation, same-directory atomic promotion with one `.bak`, retryable
  `notSaved` state, and boot-time merge of the ignored sibling Audio profile.
- AIO-1's fifth persistent Drawer page, **audio**, as a deliberately read-only
  visual gate before any config write or server restart. It shows exact active
  engine/device/channel state on the shared compact/roomy rail, caches the
  device list only at build/explicit refresh, keeps mic/stem-off rows stable,
  and exposes Ink-painted mode/device lists while discard/apply remain inert.
- `CQAudioConfig`, a serverless startup-profile model for the exact nine Audio
  keys: defaults/base/overlay precedence, active/draft/dirty/valid separation,
  single-owner 0↔1 channel display conversion and sibling profile paths.
  Generated explicit nils use a private `\default` marker because
  SuperCollider Events drop nil-valued keys; unknown generated keys hard-fail.
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
- Added reproducible §13.6/§13.8 listening labs: a 30 Hz shared-cell zipper
  render against continuous and deliberately unlagged controls, plus
  peak-aligned speech, drum and sustained-organ comparisons for the legacy
  preamp and two conservative drive-derived attenuation curves. The user could
  not distinguish the measured zipper reliably, so its DSP remains unchanged.
- Accepted the middle preamp curve for production: fixed partial attenuation of
  −1 dB per actual Drive octave after the asymmetric waveshaper. It is unity at
  Drive 1, leaves substantial material-dependent pressure, and feeds the
  envelope follower from the same compensated signal that reaches the Coco.
  This adds no parameter, GUI control or snapshot migration. The selected
  production path also passed its final live-input confirmation.
- Completed the N10 shell-cue review: the heart's true 22 px hover disc and
  the mouth chevron's weight cue passed visual acceptance on 2026-07-29.
- A fourth persistent Drawer page, **workshop**: the bay's quiet measurement
  table, now carrying the 39 parameters Night and the Master strip do not own
  after Space retirement, so the whole instrument is reachable without the
  Day panel. One centred responsive rail holds a shallow service band
  (`deck a · input trims · deck b`)
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

- Revoiced Coco digital dust from a breathing white-noise bed into a quieter
  9 kHz-filtered floor plus sparse bipolar chip errors. Clocked RAM now thins
  the errors into roughly 30–960 stored grains/s across the 1.5–48 kHz Speed
  range, keeping slow crackle distinct and the fast end drier than legacy
  hiss. A dedicated NRT character gate guards loudness, crest, density and
  high-frequency balance alongside the existing maximum-dust safety render.
  Rebalanced `selfNoise` around below-unity rest gains, irregular held-castle
  blooms and fully closing cross-feed valves, so it can clear and regrow
  instead of settling into a periodic saturated sea; a 120-second ecology gate
  now guards clearing, rebloom, envelope irregularity and rail residence. The
  new dust source and the revised preset were accepted by ear on 2026-07-29.
- Widened the playable middle of the Quantussy Chaos hand with a `+2.0`
  control curve while preserving direct values, snapshots and the 0–1 DSP
  semantics. Night's storm iris now reveals four planted broken weather rings
  at ¼, ½, ¾ and full, with a living front through them and a proper
  cable-priority hover cue. The curve's listening pass and the rings' visual
  review were accepted on 2026-07-28.
- Gave the then-current four Drawer pages one shared page frame: the same air between the
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
- Raised the Drawer's type above the Day panel's fixed 10 px and gave the then-current four
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
