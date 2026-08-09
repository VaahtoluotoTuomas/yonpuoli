# Yönpuoli — release roadmap

Updated 2026-08-09. This is the concise public status and release order. The
complete Nocturne design and acceptance journal is preserved in
[`docs/history/PLAN_nocturne.md`](docs/history/PLAN_nocturne.md); it is evidence,
not a current to-do list.

## Current status

Yönpuoli 1.0 is a Windows-tested SuperCollider feedback instrument. The sound
engine, Night performance surface, Master and five-page Drawer,
16×32 routing matrix, Audio I/O recovery, optional 16n map, release presets and
release snapshots are implemented. The candidate also has an installed-runtime
Windows launcher and a concise sample-loop-first README path. Automated compile, unit, NRT
safety/hold and GUI gates are maintained in the repository. Audible, visual and
hardware claims remain human acceptance decisions rather than conclusions
inferred from automation.

The user accepted the Nocturne interaction and sound passes, SlotX/Dust, the
current 16n feature set, curated release content, persistent Audio restart
guidance and release screenshots by 2026-08-08. The installed-runtime launcher,
private audio server, Night-first startup, replacement GUI after Audio restart
and settled wired-headphone discovery were accepted on 2026-08-09.

## Public-release gate

- [x] Product identity, lineage statement, license and source attribution.
- [x] Privacy/source audit over the working tree and full Git history.
- [x] Windows hardware I/O, recorder, panic, clock guard and long-resource soak.
- [x] Night-only performance interface and Drawer workflows.
- [x] Six authored release presets and five deliberate release snapshots.
- [x] Fresh-interpreter Audio device discovery and preferred-route fallback.
- [x] Stable pending-action guidance for Audio changes and device scans.
- [x] Human-approved Night hero plus Inspector and Workshop Drawer screenshots
      from the exact candidate UI.
- [x] Human-verify the implemented `YONPUOLI.bat` installed-runtime launch,
      private-server isolation, Night-first startup, replacement GUI after an
      Audio restart and settled wired-headphone discovery; automated
      resolver/config/script gates pass.
- [x] Final human check that **keep current audio** survives a relaunch and that
      closing the replacement window also closes the diagnostic console without
      leaving an owned `sclang` or `scsynth` process.
- [x] Remote CI green on the exact release candidate.
- [x] Merge `nocturne` into the default `main` branch.
- [x] Tag and publish `yonpuoli-1.0.0` with release notes and media links.

The old `v1.0` tag belongs to the pre-Yönpuoli Cocoquantus study. It is an
internal technical milestone, not the Yönpuoli public release. `archipelago-1.0`
is likewise a completed GUI-development milestone.

## Release evidence

The release candidate must pass, through `tests/run_sclang_gate.ps1`:

1. `tests/compile_check.scd` → `CQ_COMPILE_OK`
2. `tests/run_all.scd` → `CQ_TESTS_PASSED`
3. `tests/nrt_safety.scd` → `CQ_SAFETY_PASSED`
4. `tests/nrt_hold_integrity.scd` → `CQ_HOLD_OK`
5. relevant GUI smokes for every changed public surface

The concise hands-on release pass is
[`tests/manual_checklist.md`](tests/manual_checklist.md). Completed acceptance
scripts remain in
[`docs/history/manual_checklist_nocturne.md`](docs/history/manual_checklist_nocturne.md).

## Known release limits

- Windows 10/11 is the only release-tested platform.
- SuperCollider's system-default device may report a responsive but unhealthy
  audio clock on some Windows hosts; Yönpuoli detects this and requires an
  explicit restart rather than hiding it.
- Device hot-plug is an explicit confirmed Audio-page operation, never an
  unsolicited interruption of a performance.
- Snapshots store instrument state, not Coco buffer audio.
- Optional 16n and multichannel hardware paths remain optional; the instrument
  works from its GUI and a stereo device.
- Several mature GUI owners are large classes. They remain covered by geometry,
  interaction and paint gates; decomposition is post-release maintenance, not a
  risky last-minute rewrite.

## After 1.0

In order, subject to a new design decision:

1. honest performance video and three or four accepted audio excerpts;
2. isolated Bits 5/8/12 listening study;
3. three porous playing-mode experiments;
4. Norns port research.

These are not hidden release blockers and must not be presented as current
features.

## Authority and history

- [`SPEC.md`](SPEC.md): behavior, parameters, DSP and safety.
- [`GUI_SPEC.md`](GUI_SPEC.md): interaction and visual language.
- [`ARCHITECTURE.md`](ARCHITECTURE.md): ownership and code/test structure.
- [`CHANGELOG.md`](CHANGELOG.md): release-level changes.
- [`docs/DEVELOPMENT_PROCESS.md`](docs/DEVELOPMENT_PROCESS.md): human-directed,
  AI-assisted workflow and technical ownership.
- [`audit/README.md`](audit/README.md): audit decision layer and historical reports.
- [`docs/history/`](docs/history/README.md): frozen implementation journals.
