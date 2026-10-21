# Yönpuoli 1.1.0

Learn the instrument while playing it. This release adds contextual help,
illustrated component cards and two guided patch-building paths to the Night
surface, together with clearer performance gestures.

## Learn on the playing surface

- Hover a control for a concise title and explanation in the lower-left corner.
- Turn on **info** and click a component for its illustrated anatomy. Cards sit
  beside their subject; clicking another component switches directly to it.
- **Basics & first loop** has 23 illustrated pages covering sources, recording,
  time, patching, modulation, two-voice playing and the moon.
- **Organ & advanced** has 27 pages building an Organ-and-memory patch through
  roots, envelopes, castle audio, cross-feed, patch memories and saving.
- Hover guide pictures to locate their real controls. Use fixed back/next/close
  buttons or the left/right arrow keys. **Shortcuts** collects the performance keys.
- Each guide starts with **fresh session → reset now**: an explicit reset of
  unsaved controls, cables, loaded decks and loop audio. Audio setup and saved
  files remain, and the same guide reopens at page one.

Ordinary info cards are for inspection; the two guides enable playing their
exercises. Master and PANIC remain accessible. Master now sits at the lower right,
beside a compact session area with REC, PANIC and audio notices.

## Playing changes

- Pull a bird in any direction and release to load a random sample. It springs
  back with a short wiggle; tap, wheel and file drop retain their actions.
- Dust amount uses up/down; Grain uses left/right on both sides. Grain blends
  steady noise and sparse crackles. The underlying noise blend is unchanged.
- Flip inputs reverse direction once per rising trigger, whether playback was
  already forward or reversed. Held-high and falling signals retain direction.
  This replaces the previous gate behavior, so patches using Flip can evolve
  differently. Record inputs remain gates.
- **Ctrl+X** redoes GUI patch edits, paired with **Ctrl+Z** undo. Neither restores
  overwritten loop audio; text fields keep their ordinary cut shortcut.

## Start or update

1. Install SuperCollider 3.13 or newer on Windows 10/11.
2. Download the release's **Source code (zip)** and extract it.
3. Double-click `YONPUOLI.bat`, then choose **info → basics & first loop**.

Keep your existing configuration, samples and recordings when updating an
existing installation. No Quarks or third-party plugins are required. Snapshots
store settings and routing, not Coco loop-buffer audio.

See the [quick reference](https://github.com/VaahtoluotoTuomas/yonpuoli/blob/yonpuoli-1.1.0/docs/QUICK_REFERENCE.md)
and [full changelog](https://github.com/VaahtoluotoTuomas/yonpuoli/blob/yonpuoli-1.1.0/CHANGELOG.md).
