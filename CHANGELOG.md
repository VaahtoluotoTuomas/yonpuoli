# Changelog

This file records public releases and the earlier milestones that shaped
Yönpuoli. The Git history carries the detailed implementation story.

## Unreleased

### Added

- An `organ & advanced` guide beside `basics & first loop`: 27 illustrated,
  player-driven pages build an Organ-and-memory patch from scratch, introduce
  roots/envelopes, dynamics, punch/skip, castle audio, Q and cross-feed, then
  seed memories, weather and Drawer/saving. Its two-click fresh session returns
  to the same guide. Picture hover also locates Master depth and Drawer.
- Always-on hover help in the lower-left footer: concise titles and instructions
  for individual Night controls, sockets, cables, Master, toolbar and Workshop.
  It works while playing with info off; empty background clears the area.
  Info mode replaces these with one stable instruction pointing to component
  cards, the upper-left guides and exit actions. The combined learning guide
  distinguishes playing from cable-entry reading. Hovering leaves playing gestures unchanged.
- Optional `info` inspection mode on Night and Master: clicking a component
  opens a persistent illustrated explanation of its controls and connections.
  Enabling leaves the instrument exposed; inspection clicks do not play it.
  Coco has one complete page with a large picture and two text columns.
  Castles have two illustrated pages: the shared ring and one castle's anatomy.
  Other components use one page. Picture numbers and text rows locate the corresponding parts
  on hover. Cards avoid the complete subject, including all five castles.
  Clicking another component replaces
  the card immediately. Empty background closes only the card and has no note.
  Every info target reacts to hover, including empty input sockets and read-only
  Master fields; available targets have no persistent square marks.
  The info button breathes gently over four seconds while enabled.
- One 23-page `basics & first loop` guide combines the overview with practical
  exercises. Sources and Coco anatomy precede recording; castles and ports
  precede making, hearing and editing a cable. The path continues through flip
  triggers, right-bird capture, separate Input and Feedback lessons, dust,
  cross-Coco modulation, record gating and the moon.
  Input and Feedback each show all three lower crescents, highlighting only the
  middle or rightmost one. Concept pages use native picture hover without brackets;
  practical steps retain action cues. Cable editing covers polarity, weight,
  moving/removing and undo/redo without a second overlapping guide.
  Its card moves left for right-side lessons and centrally for cross-Coco lessons;
  stronger subject brackets pulse
  smoothly every two seconds. It stays in the upper-left toolbar.
  Reading notes never changes sound;
  Master and PANIC remain exposed. PANIC and performance keys stay live.
- Labelled native guide illustrations locate their matching parts on hover.
  They include the actual midpoint weight wheel, shoreline level, speed notch
  and separate playhead.
- Fixed back/next/close positions on multipage cards, grey unavailable directions,
  and left/right arrow navigation. Paragraphs no longer gain a hover background.
- A `shortcuts` info button after both playing guides, with two pages for performance
  and contextual patch/navigation keys. It distinguishes Coco write/hold
  (`1/2`) from recording the stereo output (`R`).
- Native symbols and action arrows on the guide's practical steps, including
  hollow-write/red-hold hearts and the direction for changing dry level.
- The guide's distinct `fresh session` button, with an inline `reset now`
  confirmation. It resets unsaved instrument state through the owned session
  lifecycle, keeps audio setup and saved files, and returns to step one.
- Ctrl+X redo for GUI patch edits and moon sowing, paired with Ctrl+Z undo.
  Redo restores the exact operation, and text entry retains its normal cut key.

### Fixed

- The first cable lesson explains strength/cancel before releasing, and defines
  SP.AF's zero-depth effect. The next lesson has separate local and global
  picture bands. Flip introduces its input first and describes a forward/reverse
  swap without the ambiguous hand-set wording.
- Flip input now toggles direction on each rising trigger, including when the
  hand Flip is already on. Falling/held signals retain direction; the chevron
  follows actual playback and the input lamp flashes only when a trigger fires.
- Guide symbol hover uses native brightness without extra square locators;
  the bridge lights along its full paths and both ends.
  Cards occlude all surface highlights, crescent brackets leave adjustments
  visible, and the speed cue sits at nine o'clock.
- Rapidly toggling info on and off no longer leaves its reading buttons visible.
- Tide's unreadable whole-landscape thumbnail is replaced by enlarged native
  seed/gill pictures and blend/breed diagrams beside their corresponding rows.
  The explanation follows store/recall, focus, blend and breed.
- The first info card could stretch over Night through Qt's stack layout.
- The Coco cross-feed bridge now has its own number 10, separate from the Q
  inlet at 9, with its valve and hollow modulation socket located together.
- Coco info numbers 2–4 now identify SP.AF, input and feedback separately,
  left-to-right. The overview starts with heart/spiral basics; castle anatomy
  reads its outputs and inputs in spatial order, with clear position cues.
- Coco inspection hover now includes all three lower crescents and their
  gate/duck arcs. Component cards sit beside their subject instead of at the
  opposite screen edge, while keeping that subject's controls and ports clear.
  Coco and castle explanations have larger illustrations. Moon and eye diagram leaders
  follow the described horizontal/vertical gestures.
- Closing the native window during an info pulse no longer tests a destroyed
  marker's missing visibility value as a Boolean.

### Changed

- Both Dust veils now use up/down for amount and left/right for the mist/chip
  blend, independently of their mirrored drawing. All help and diagram arrows
  use these axes. Grain is explained as a blend of steady noise and sparse
  crackles, with an open-memory listening comparison; the DSP blend is unchanged.
- Master meters and controls move to the lower right, after REC and PANIC.
  A bounded help area replaces their former left-hand position without reducing
  the playing surface or transport hit areas. Status text and REC/PANIC share
  aligned edges in a compact group, with reserved notice/elapsed-time space
  and a clear gap before Master. A matching `session` heading and rule unify the
  transport area. The duplicated upper-left info-mode hint is removed.
- Basics and patching now live inside `basics & first loop`; their separate
  buttons are removed. Cable clicks enter the port introduction in inspection,
  and paging keeps that reading mode. The combined guide button starts at the
  beginning with playing enabled.
- Pull either bird in any direction and release to load one random sample,
  replacing the horizontal back-and-forth gesture. The bird brightens, yields
  to the pull and springs home with a short wiggle. Tap, wheel and file drop
  keep their existing actions. Cancelling a held pull loads nothing.

## yonpuoli-1.0.0 — 2026-08-09

The first release under the Yönpuoli name.

### Added

- The Night performance landscape: two playable Coco shells, five castles,
  birds and live-input roots, directional cross-feed, dust veils, signed cable
  patching, ecological shake, and a persistent Master strip.
- A five-page Drawer with an exact 16×32 Inspector, Workshop parameters,
  patch-history Spellbook, snapshot Memory page, and Audio setup.
- Six authored starting presets: `classic`, `selfNoise`, `quantumDrone`,
  `organMode`, `crossCurrent`, and `dustGarden`.
- Five factory snapshots: `example`, `undertow`, `dust_bloom`, `glass_tide`, and
  `organ_lanterns`.
- File decks, live inputs, stereo recording, optional four-channel stems, and an
  optional 16n fader map.
- `YONPUOLI.bat`, a no-code Windows launcher that finds an installed
  SuperCollider, prepares local runtime files, and owns a private audio-server
  session until the window closes.
- A compact Night field guide, current interface screenshots, source and lineage
  notes, and a candid account of the AI-assisted development process.

### Changed

- Reframed the project as an independent feedback instrument with explicit
  Cocoquantus lineage rather than an emulation promise.
- Made Night the sole maintained performance surface and retired the earlier Day
  panel.
- Expanded the signed matrix to 16×32 with independent SlotX and Dust
  destinations for both Cocos.
- Rebuilt the 16n layout as two mirrored Coco strips plus shared performance
  controls.
- Revoiced digital dust and self-noise, widened the playable Quantussy chaos
  region, and removed the rejected Space/FDN chain after comparative listening.
- Reduced the public documentation to the instrument guide, release material,
  sources, and genuine technical references; private work journals remain in
  development history rather than the current project surface.

### Fixed

- Fresh Windows audio-device discovery now runs only after the owned server has
  stopped, using a bounded isolated helper and a settled multi-pass scan. Newly
  connected wired headphones and interfaces can appear without restarting the
  main language process.
- Saved audio preferences survive a temporarily absent device. The player can
  retry the preferred route or deliberately keep a working fallback.
- Standalone launches use a private free server port and no longer attach to an
  IDE server or stale process at the conventional port.
- Confirmed Audio restarts replace the GUI without terminating the standalone
  application, while ordinary startup still opens directly on Night.
- Audio changes, device scans, and fallback decisions now point clearly to the
  highlighted action required to continue.
- Inspector group headings and seams derive from the canonical matrix order;
  Coco headings now span through `record`, and local labels read `slotX` and
  `dust`.
- Night layers repaint with current geometry after startup and window resizing,
  preventing missing streams and temporarily misplaced cables.

## Earlier development milestones

### archipelago-1.0 — 2026-07-22

- Established the Ink visual language, cable grammar, Drawer, telemetry-driven
  surfaces, and the first complete Night landscape.

### v1.0 — 2026-07-19

- Completed the initial sound engine, code API, parameter and routing layer,
  file decks, snapshots, recording, controller support, and safety tests.
- This tag predates the independent Yönpuoli identity and is not the public 1.0
  release.
