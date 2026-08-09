# Changelog

This file records public releases and the earlier milestones that shaped
Yönpuoli. The Git history carries the detailed implementation story.

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
