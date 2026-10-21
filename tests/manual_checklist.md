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

### Always-on hover help (manual visual and playing acceptance)

Recompile/relaunch, keep **info** off, and check at 1280x720 and maximized.
`gui_hover_smoke.scd` writes `tests/tmp/hover-*.png` for review; automated layout
and callback checks do not replace this hands-on acceptance.

- [ ] Move between a Coco's heart, spiral, each lower crescent, gate/duck arcs,
      Organ, Q inlet and bridge, then their input/output sockets. The lower-left
      title and gesture describe the individual part, without opening a card.
- [ ] Check birds/level, roots/source strands, castles/ranges, eye, moon,
      dust, seeds, tide gate and a planted cable. Text fits comfortably, remains
      readable and does not flicker when staying on one element.
- [ ] Cross empty gaps, the footer's blank space and the window edge: both title
      and explanation disappear. Resize and open/close Drawer without leaving
      a stale note. Check a Workshop ledger and toolbar/Master controls too.
- [ ] Master meters, vol, dry and depth are to the right of REC/PANIC. At both
      sizes the 400 px help area stays modest, notes/status do not collide and
      no controls are clipped. Status begins on REC's left edge; REC and PANIC
      share a baseline, with a clear gap between PANIC and Master. Review the
      `footer-status-*` and `footer-recording-*` captures for combined notices
      and elapsed time. Neither warnings nor recording move the buttons.
      Session and Master use matching headings/rules. Session buttons sit below
      the heading and status below the buttons, sharing their content edges.
- [ ] Play while notes change: turn a crescent, wind a spiral, patch/edit a cable,
      toggle a heart and use its right-button punch. Confirm the original
      actions still work. Start/stop REC and check PANIC and Space.
- [ ] Turn info on: the lower-left area immediately becomes a stable **info
      mode** instruction. Move over shapes, a card, its text and empty space:
      no competing element note appears. Clicking still opens the usual card
      and its row locators work. The combined guide shows **play** instructions;
      close returns to inspection instructions, Esc/info restores hover help.
      Check the guide's dry locator at Master's new position. Toggle info
      on/off quickly and confirm its guide buttons disappear when off.

### Info field notes (manual visual and playing acceptance)

Maintainer checkpoint, 2026-10-21: the Dust axis change and latest
**basics & first loop** updates were accepted. **Organ & advanced** has not yet
been read or played through by the maintainer; that review is deferred to a
later session. Its automated checks are complete; the human checks below remain open.

Start a fresh session at 1280x720, then repeat the layout checks maximized.
The automated `gui_info_smoke.scd` produces native-window review PNGs under
`tests/tmp/`; these do not make the human visual or listening decision.

- [ ] Enable **info**: Night stays exposed, with **basics & first loop**, then
      **organ & advanced**, then **shortcuts**. No separate Basics/Patching button, automatic card or grid of
      boxes appears. The info button breathes gently without flashing or moving.
      No repeated click/Esc text appears before the guide; the footer holds it.
- [ ] Hover a Coco's heart, winding, input crescent and an empty socket. They
      reveal the same Coco group; click each and confirm it opens the same
      component card. Moving to another part of that group retains its page.
      Repeat on both sides, both birds/roots, all five castles, eye, moon and Master.
      Hovering either Coco's spiral also brightens all three lower crescents
      and their gate/duck arcs; leaving or exiting info clears that group ink.
- [ ] Check both Cocos and both castle pages at 1280x720 and maximized.
      The whole described component, including its controls/ports, remains
      outside the card. All five castles stay exposed together. Text, picture,
      page count and footer fit with no clipping; Master and PANIC remain visible.
      Coco, root, bird, moon and eye cards appear beside their own component.
      Covering a different component is allowed. Each Coco shows its entire
      explanation at once: one large picture beside two readable text columns,
      with only close in the footer. Match lower controls, gate/duck arcs,
      gills and ports between the picture and actual surface.
      Number 1 teaches heart/spiral first; 2, 3, 4 each point to a different
      lower crescent (SP.AF, input, feedback), from left to right on both Cocos.
      Q inlet (9) and the upper cross-feed bridge (10) have separate numbers.
      Hover 10: the bridge valve and its hollow modulation ring are both located.
      Confirm the text names the other Coco as the incoming source.
      Castle page one shows a large shared ring, example links and central eye;
      page two enlarges one castle and covers its clock, range and four ports.
      Its order is clock, upper outputs left-to-right, lower inputs left-to-right,
      then the bottom range ripple; each leader reaches the named symbol.
      No tiny type or scrolling is needed to fit the enlarged pictures.
- [ ] Hover each card row and its picture number. Matching numbered rings locate
      the actual part(s); sockets brighten without a cable in hand. Picture
      numbers and leader lines leave the illustrated symbols recognisable.
      Castle connection rows locate the same port on all five castles. Master
      rows identify the corresponding controls. Leaving clears the part locators.
      No paragraph gains a hover background; rows without references stay inert.
- [ ] In moon and eye cards, sideways leaders run horizontally and up/down
      leaders vertically, with separated numbers; tap remains distinguishable.
- [ ] Click another component: its card replaces the previous one in that click.
      Background, blank Master space or **close** closes only the card. Empty
      background never opens an open-water topic. The lower gill and four seeds
      share the Tide explanation. Esc/info exits and restores normal playing.
- [ ] In inspection, a cable crossing a Coco/castle body does not steal that
      component's click. A cable in open space opens **basics & first loop** at
      **sound paths & control cables** (page 9), with **reading** in the footer.
      Paging keeps inspection active; clicking/dragging does not perform the
      described actions. Back reaches the overview; the guide button reopens page
      one with **play** in the footer and controls available. With info off,
      confirm ordinary cable-first playing priority is unchanged.
- [ ] Read the combined guide with back/next: all 23 pages remain readable.
      Its overview introduces birds/roots/memories before loading; **meet the
      Coco** precedes writing, with heart/spiral/playhead/shore pictures. Castles,
      the eye and solid/hollow ports are explained before their first cable.
      Cable depth/editing follows the first audible connection; fan-out and
      summing accompany the cross-Coco exercise. No separate Basics recap repeats
      these lessons. Check the weight, moving-end and SP.AF pictures.
      Reopening either guide or Shortcuts starts at page one. All three buttons disappear
      when info is off. Picture hover brightens both members of overview pairs
      and all matching ports, clearing on leave; prose stays inert. The weight
      picture includes the actual little wheel at its midpoint.
- [ ] On castles, Shortcuts and both guides, use left/right arrow keys
      and back/next. All three footer buttons keep their horizontal slots;
      first-page back and last-page next are grey and do nothing when clicked.
      Close keeps info armed; arrows with no card do nothing. Drawer/text-entry
      arrow keys retain their normal behavior.
- [ ] Open **tide / patch memories**. Read store/recall, focus, blend, breed in
      order. Empty and stored seeds and the gill are recognisable large pictures,
      not dots. Hover each picture/row: the correct seeds or lower gill light up.
      Check both 1280x720 and maximized. Outside info, store two different patches,
      recall each, enable focus and blend, then breed. Verify breeding changes
      the live patch while both stored patches can still be recalled.
- [ ] Read both **shortcuts** pages. Distinguish 1/2 write/hold from R output
      recording, and identify the contexts for Tab, Shift and held-cable Z/X.
      Left/right also describe info-page navigation. After a safe boot, verify the listed
      performance keys with a low listening level; Space remains immediate.
- [ ] Inspecting a bird never opens a chooser; inspecting a port never starts a
      cable. Click, drag, right-click, wheel and file drop do not alter sound or
      patch. Exit during a held press: releasing does not play through.
- [ ] PANIC and Space remain usable while reading; Master/REC/notices stay exposed.
      Use **basics & first loop** from a fresh session with a familiar file,
      following all 23 pages yourself. Introductions explain the next actions
      before asking you to try them. Practical steps show matching symbols and
      action cues; concept pages only use native picture hover. The write heart is
      hollow; the held heart is red. Dry's arrow points up for preview and down
      after capture. Match the symbols to the highlighted parts of the surface.
      Back/next do not change music. Opening Shortcuts or closing the guide
      restores inspection; switching back permits playing again.
      Brackets are clearly legible against the full surface with a smooth
      two-second pulse. **Keep the loop** shows the shoreline; **reshape time**
      shows a speed notch and separate playhead bead. Patch the top triangle to speed, raise
      SP.AF and adjust weight; slow it with the eye. Add the positive flip route
      at full weight/depth and hear one reversal per rising trigger, from both
      initial directions. High and falling signals retain direction; tap the
      chevron between triggers and verify it still turns the current direction.
      The card moves left for Coco 2: open its heart for a few laps at default
      feedback; startup noise fades, with a faint dust bed allowed. Leave it open.
      Pull/release the right bird: its sample writes directly, without raising dry.
      **New sound / Input** and **returning sound / Feedback** (pages 16/17) each
      have one large three-crescent picture. The middle vs rightmost active arc
      and its surface cue must be unambiguous, with no hidden setting marks.
      Adjust incoming sound first; restore it, then explore returning memory and
      Feedback distortion on the next page. Try dust, then the grey Coco 1 output into
      Coco 2 speed. The central card leaves both memories exposed; wind Coco 1
      and compare Coco 2 SP.AF settings using the same cable. Add right Record
      gating: it pauses/resumes writing while flip retains trigger behavior.
      Try moon fullness/random sow, Ctrl+Z and Ctrl+X; redo restores exactly the
      same patch and settings. A new edit discards redo. Text fields keep cut.
      Check instructions against actual sound;
      automated captures cannot accept their musical or visual effectiveness.
- [ ] Basics page 10 explains wheel/Z/X and Esc before planting the cable, then
      identifies SP.AF as the speed input's depth, still zero. Page 11 separates
      SP.AF/cable weight from a second picture row: Master depth, then eye.
      Hovering Master depth locates the actual footer knob. Page 13 shows the
      flip input before the reused triangle; its direction explanation means
      forward-to-reverse and reverse-to-forward, not cable polarity.
- [ ] On **both** Dust veils, a straight upward/downward drag changes only
      amount; a straight left/right drag changes only Grain. A diagonal can
      change both. For the page-18 comparison, open the heart, lower Input and
      Feedback, and raise Dust at a comfortable listening level. Compare Grain
      fully left (continuous noise) with fully right (sparse crackles), allowing
      a lap to replace the previous texture. Restore Input and lower Dust.
      The diagrams, hover help and component card all describe the same axes.
- [ ] Follow all 27 **organ & advanced** pages from its fresh-session button.
      Muting the right shore and clearing the left memory makes Organ audible
      alone. Its orange level route and grey pitch route remain useful as the
      patch grows. Each new control is introduced before its exercise; the
      practical brackets leave the calyx and gill fills visible.
      Load a rhythmic file into the left bird, select deck a on the right root,
      then capture it in Coco 2. Compare Drive and Bias on separate drags. The
      right root envelope replaces the triangle's Organ-level route and makes
      the left voice follow file hits. Check the light bird-rate experiment,
      negative Feedback duck, paired dynamics arcs, momentary heart and skip.
      Return temporary controls/routes as instructed before the next exercise.
- [ ] Continue the advanced patch: castle 5's audio range/send becomes audible
      through the right Q gill; left Organ's yellow output into castle 5's blue
      wave-marked FM input changes that sound. The right bridge valve captures
      the audible left voice. Seal the right memory, store the two instructed
      cable-weight variations in the left seeds, then blend and breed while both remain
      reachable outside the card. Recall a seed after moon weather. Judge the
      resulting patch's musical usefulness by listening, not automated tests.
      On the last two pages, Drawer and REC pictures locate the real controls.
      Opening Drawer intentionally closes the guide; seeds recall cables only,
      Memory snapshots restore settings/routes, and REC preserves the stereo sound. Neither seeds nor
      snapshots restore loop-buffer audio.
- [ ] Hover every guide picture. Corresponding native shapes brighten;
      concept pages never add `[]`. Hover bridge: both whole curved paths,
      valves and ends respond. No surface ink shows through a card. Practical
      brackets remain visible but clear the Input/Feedback/SP.AF arcs and their
      moving setting marks. The wind cue is at nine o'clock by the speed notch.
- [ ] In a disposable session, start recording, change controls/cables, load
      files and fill both loops. Combined guide page one: **fresh session** first
      changes to **reset now** with an explanation. Next/close/exit cancels
      without changing music. Confirm: recording closes cleanly, old loops and
      loaded decks are replaced by startup defaults, micGain is zero, the patch
      is empty and step one reopens. Repeat from **organ & advanced**: reset must
      reopen that guide's own first page. Device/config and saved audio/snapshots stay
      intact. Repeat once while a snapshot morph is running: no old writers,
      windows or callbacks survive. Standalone stays open through the rebuild.
- [ ] Resize while a component row is highlighted: the card still clears its
      subject and transient locators clear. Open Drawer, then info: return to
      Night without a stale card. Close/relaunch: info starts off.

### Bird pull / release (manual gesture and visual acceptance)

The serverless `gui_bird_smoke.scd` checks release dispatch, settling and
cancellation, and renders the production bird ink to `tests/tmp/bird-*.png`.
Use the live surface below to judge the gesture's feel and animation.

- [ ] With info off, pull each bird about 30–80 px horizontally, vertically
      and diagonally, including beyond its view. It brightens and moves slightly
      toward the hand without clipping. No sample changes while held.
      Release: exactly one random sample request, a quick spring back and small
      settling wiggle; the filename and success flash follow the actual load.
- [ ] A tap still opens the chooser; cancel leaves the file alone. Wheel walks
      the folder and dropping a file still works. The rate socket, level crescent
      and a cable crossing the bird retain their own actions.
- [ ] Cancel a held pull with Esc, Tab, PANIC, info or Drawer entry, and resize.
      Nothing loads and the bird returns home. Repeat during the return, then
      close/reopen the window: no lingering motion, stale tilt or callback error.
- [ ] In info mode, pull and release a bird: only its card opens. No spring,
      file chooser or sample change. Exit info and verify the gesture works.

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
