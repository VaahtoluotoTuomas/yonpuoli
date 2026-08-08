# Yönpuoli — concise release checklist

This is the current human release pass. The complete accepted Nocturne, Audio,
SOAK, 16n, playability and release-content scripts are preserved in
[`../docs/history/manual_checklist_nocturne.md`](../docs/history/manual_checklist_nocturne.md).
Automation does not check a box in this file on behalf of a listener.

Use a low monitoring level. Recompile the class library after changing
`classes/*.sc`, then start from a clean interpreter unless the step explicitly
tests recovery.

## 1. First launch, silence and panic

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

## 2. Release presets

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

## 3. Release snapshots and morphs

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

## 4. Night and Drawer interaction

- [ ] Plant, lift, move, attenuate, invert and remove a cable without a stuck
      audition or gesture.
- [ ] Play both Coco Levels, Organ, SlotQ, SlotX and Dust/Grain bodies from Night.
- [ ] Change an exact value in Workshop and verify Night/state agree.
- [ ] Inspector's 16×32 headers, seams and short `slotX`/`dust` labels align.
- [ ] Save/recall a private snapshot; it remains local and does not join the
      release bank.
- [ ] Compact and roomy layouts contain all controls and remain responsive.

## 5. Audio-device recovery

```supercollider
// With a clean Audio draft, connect headphones or an interface while Yönpuoli
// is open. Drawer -> audio -> find connected devices.
// First click: only the static impact confirmation appears.
// Confirm scan & restart, choose the exact route, then apply & restart audio.
Cocoquantus.audioClockState;       // -> \ok
Cocoquantus.audioFallbackActive;   // -> false on the preferred route
Cocoquantus.preferredAudioConfig;  // -> exact chosen route
```

- [ ] `find connected devices` becomes `confirmation below ↓` before stopping.
- [ ] Pending changes show the host cue, highlighted restart gate and filled CTA.
- [ ] The connected route appears without rebooting the main interpreter.
- [ ] Success, cancel and discard retract all pending emphasis.
- [ ] Removing the preferred device uses an explicit temporary base fallback
      without overwriting the preference.
- [ ] Reconnecting and `find preferred device` restores it in one confirmed
      restart; Night moves and the clock returns to `\ok`.

## 6. Recording and optional hardware

```supercollider
CQ.rec;
// Play both Cocos for at least ten seconds.
CQ.recStop;
```

- [ ] A new timestamped file is created and contains the expected stereo mix.
- [ ] REC blocks Audio restart until recording has stopped.
- [ ] If testing a multichannel interface, stems 1/2 are independent of main
      1/2 and PANIC silences every physical output.
- [ ] If testing a 16n, all 16 faders follow `docs/16N_MAP.md`; unplug/replug plus
      `CQ.midiRescan` restores control without affecting GUI-only use.

## 7. Release media and sign-off

- [ ] Hero screenshot is generated from the exact release candidate.
- [ ] Short video shows direct Night playing, cable editing, Drawer precision and
      PANIC without cuts that misrepresent response.
- [ ] Audio excerpts are listened to on headphones and speakers, use only
      redistributable/original source material, and name their preset/snapshot.
- [ ] README, CASE_STUDY, CHANGELOG and release notes describe the same version,
      limitations and AI-assisted authorship.
- [ ] Remote CI is green on the commit that will be tagged.
- [ ] `nocturne` is merged to `main`; release tag is `yonpuoli-1.0.0`.
