# Yönpuoli 1.0.0

Yönpuoli is a nocturnal feedback instrument for SuperCollider: two coarse,
clocked loop memories circulate sound through a five-oscillator chaotic network
and a signed 16×32 patch matrix.

This is the first public Yönpuoli release. It is a lineage study inspired by
Peter Blasser's Ciat-Lonbarde Cocoquantus, not a circuit model, official port or
affiliated product.

## Highlights

- Two Coco-inspired 8-bit loopers with repitching, feedback, hold, flip, skip,
  articulation, dust and square-stack organ voices.
- A Night-first performance surface with cable patching, direct gestural playing
  and a five-page Drawer for Inspector, Workshop, Spellbook, Memory and Audio.
- Six authored presets and five release snapshots covering looping, Quantussy,
  organ, granular/noise and cross-modulated starting points.
- `YONPUOLI.bat` for a no-code Windows launch using an installed SuperCollider
  3.13 or newer, an isolated class path and a private audio-server port.
- Explicit connected-device discovery, saved preferred routes, safe temporary
  fallback and recoverable Audio restarts for Windows hardware changes.
- Master/stem safety limiting, immediate PANIC, recording, optional 16n control
  and fail-toward-silence lifecycle guards.

## Start

1. Install SuperCollider 3.13 or newer on Windows 10/11.
2. Download and unzip the release source archive.
3. Double-click `YONPUOLI.bat` and keep its diagnostic console open.
4. Put an audio file in `samples/` and follow README's **First loop without
   code** path.

No Quarks or third-party plugins are required.

## Release media

- [Night performance surface](https://raw.githubusercontent.com/VaahtoluotoTuomas/yonpuoli/yonpuoli-1.0.0/docs/media/yonpuoli-night.png)
- [Inspector routing matrix](https://raw.githubusercontent.com/VaahtoluotoTuomas/yonpuoli/yonpuoli-1.0.0/docs/media/yonpuoli-drawer-inspector.png)
- [Workshop parameter ledger](https://raw.githubusercontent.com/VaahtoluotoTuomas/yonpuoli/yonpuoli-1.0.0/docs/media/yonpuoli-drawer-workshop.png)

Audio excerpts and a performance video are intentionally later additions, not
missing 1.0 deliverables.

## Known limits

- Windows 10/11 is the only release-tested platform.
- Device hot-plug is an explicit confirmed Audio-page operation.
- Snapshots store instrument and routing state, not Coco buffer audio.
- Optional 16n and multichannel paths are not required for ordinary stereo GUI
  use.

See the [README](https://github.com/VaahtoluotoTuomas/yonpuoli/tree/yonpuoli-1.0.0#readme),
[quick reference](https://github.com/VaahtoluotoTuomas/yonpuoli/blob/yonpuoli-1.0.0/docs/QUICK_REFERENCE.md)
and [full changelog](https://github.com/VaahtoluotoTuomas/yonpuoli/blob/yonpuoli-1.0.0/CHANGELOG.md).
