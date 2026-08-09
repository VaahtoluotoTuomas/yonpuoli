# Yönpuoli — manual smoke test

This is the short repeatable hands-on pass for the current instrument.
Automation does not check a box in this file on behalf of a listener.

Use a low monitoring level. Recompile the class library after changing
`classes/*.sc`, then start from a clean interpreter unless the step explicitly
tests recovery.

## 1. Windows one-click launch

Use a clean unpacked copy, or back up any existing local `config.scd` before
testing first-run creation. Do not start SuperCollider or run `CQ.boot` first.

- [ ] Double-click `YONPUOLI.bat`; no IDE preference, code evaluation or manual
      class-path setup is required.
- [ ] On a genuinely fresh copy, `config.scd` and the runtime folders are
      prepared without replacing an existing local config.
- [ ] The diagnostic console stays open while the instrument is running and the
      names distinct private language/audio ports while the application reaches
      the Night window with the Drawer closed. Initial
      profile fallback/repair must not open the Drawer or redirect to Audio.
- [ ] A responsive SuperCollider IDE server on port 57110 does not get adopted,
      stopped or waited on by the one-click application.
- [ ] A confirmed Drawer → Audio restart closes the old GUI, opens its
      replacement and leaves the standalone application alive.
- [x] Closing the replacement GUI frees audio and closes the diagnostic console
      without leaving `sclang` or `scsynth` running.
- [ ] An intentionally invalid `-Sclang` path produces a readable, paused error
      instead of a flashing or silent console.

## 2. First sound, silence and panic

```supercollider
// Start with mic gain at zero and a stereo route you can hear safely.
CQ.boot;
CQ.isBooted;             // -> true only after verified graph + fade-in
Cocoquantus.audioClockState; // -> \ok

// The blank instrument must not produce an unexpected loud signal.
// Space must mute immediately and resume without a second graph.
CQ.panic;
CQ.resume;
```

- [ ] Boot reaches ready without a hidden device/default-route substitution.
- [ ] Blank/rest level is safe and PANIC is immediate.
- [ ] Resume returns the same session without duplicate nodes or GUI.

## 3. Release presets

Use the same modest master level for all six. `classic` benefits from a loaded
deck or live input; the others can speak from their own sources.

```supercollider
CQ.patchPreset(\classic);
CQ.patchPreset(\selfNoise);
CQ.patchPreset(\quantumDrone);
CQ.patchPreset(\organMode);
CQ.patchPreset(\crossCurrent);
CQ.patchPreset(\dustGarden);
```

- [ ] Each preset has a recognisable purpose matching `README.md`.
- [ ] No preset produces a dangerous jump, stuck gesture or silent dead end.
- [ ] `empty` clears usefully and `mayhem` remains a stress field, not a promoted
      artistic starting point.

## 4. Release snapshots and morphs

```supercollider
CQ.recall(\undertow);
CQ.recall(\dust_bloom);
CQ.recall(\glass_tide);
CQ.recall(\organ_lanterns);

CQ.morph(\undertow, \glass_tide, 12);
CQ.morph(\dust_bloom, \organ_lanterns, 12);
```

- [ ] All four scenes start meaningfully from empty Coco RAM.
- [ ] The two morph pairs remain responsive and musically distinct.
- [ ] Recalled mic gain stays zero and PANIC remains reachable during morph.

## 5. Night and Drawer interaction

- [ ] Follow the README path without code: load one bird, preview it if needed,
      write a phrase into the matching Coco, seal Hold, lower dry and play the
      memory.
- [ ] Organ, Quantussy drones and self-noise read as optional voices after the
      loop path, not as an equal or preceding first lesson.
- [ ] Plant, lift, move, attenuate, invert and remove a cable without a stuck
      audition or gesture.
- [ ] Play both Coco Levels, Organ, SlotQ, SlotX and Dust/Grain bodies from Night.
- [ ] Change an exact value in Workshop and verify Night/state agree.
- [ ] Inspector's 16×32 headers, seams and short `slotX`/`dust` labels align.
- [ ] Save/recall a private snapshot; it remains local and does not join the
      release bank.
- [ ] Compact and roomy layouts contain all controls and remain responsive.

## 6. Audio-device recovery

```supercollider
// With a clean Audio draft, connect headphones or an interface while Yönpuoli
// is open. Drawer -> audio -> find connected devices.
// First click: only the static impact confirmation appears.
// Confirm scan & restart and wait for the rebuilt Audio page to report scan
// completion. Choose the exact route, then apply & restart audio.
Cocoquantus.audioClockState;       // -> \ok
Cocoquantus.audioFallbackActive;   // -> false on the preferred route
Cocoquantus.preferredAudioConfig;  // -> exact chosen route
```

- [ ] `find connected devices` becomes `confirmation below ↓` before stopping.
- [ ] The replacement page reports `device scan complete · choose route above`
      and fresh input/output counts after the settled three-pass Windows scan.
- [ ] Between the old server's exit and the replacement boot, the launcher
      console prints a new `Yönpuoli: cached … from a stopped-server discovery.`
      line.
- [ ] Pending changes show the host cue, highlighted restart gate and filled CTA.
- [ ] Every host control aligns with its own painted row: device mode, device or
      split input/output, sample rate, server block and hardware buffer.
- [ ] A mixed recognized split pair such as ASIO input plus WASAPI output is
      invalid and cannot be applied or saved.
- [ ] A newly connected wired `Windows WASAPI : Headphones (...)` endpoint
      appears in the output list without rebooting the main interpreter. In
      split mode it can be paired only with a WASAPI input.
- [ ] Success, cancel and discard retract all pending emphasis.
- [ ] Removing the preferred device uses an explicit temporary base fallback
      without overwriting the preference. Startup remains on Night and Master
      reads `TEMPORARY AUDIO · drawer → audio`.
- [ ] Reconnecting and `find preferred device` restores it in one confirmed
      restart; Night moves and the clock returns to `\ok`.
- [x] With the preference still absent, `keep current audio` first shows
      `keep & restart`; confirmation makes the working route preferred. Close
      and relaunch through `YONPUOLI.bat`: the old fallback warning does not
      repeat and the application still opens on Night.

## 7. Recording and optional hardware

```supercollider
CQ.rec;
// Play both Cocos for at least ten seconds.
CQ.recStop;
```

- [ ] A new timestamped file is created and contains the expected stereo mix.
- [ ] REC blocks Audio restart until recording has stopped.
- [ ] If testing a multichannel interface, stems 1/2 are independent of main
      1/2 and PANIC silences every physical output.
- [ ] If testing a 16n, all 16 faders follow `../docs/16N_MAP.md`; unplug/replug plus
      `CQ.midiRescan` restores control without affecting GUI-only use.
