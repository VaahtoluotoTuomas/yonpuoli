# GUI_SPEC.md — the Archipelago (Yölahti GUI)

> *A matrix is what a patch looks like to an accountant.*
> *This document describes what it looks like to a hand.*

Drafted 2026-07-19 in a design session held in the voice of the instrument's
original builder. It is a **vision spec**: normative for GUI behavior once
adopted, written from the instrument-builder's chair. Frontend architecture
(classes, layout engines, drawing strategy) is deliberately left to the
implementers — but every behavior, law, constant, contract and safety edge they
need is here.

Relationship to the other documents:

- **SPEC.md stays normative** for parameters, ranges, curves, and safety (§8
  wins any conflict with this file). SPEC §6.2 was rewritten on 2026-07-21
  (during G2): it points here for GUI structure and keeps only the
  skin-independent obligations — what the GUI must show, what it must make
  reachable without code, the mic policy, the sacred keyboard map, and PANIC's
  permanent visibility. The v1.0 panel it used to describe is history.
- **ARCHITECTURE.md stays normative** for code shape (pure-view GUI, funnels,
  ownership).
- Nothing in this document changes DSP behavior. The only server-side changes
  are the read-only telemetry additions in §9.
- Design target: **PC alone** — mouse + QWERTY, no controllers. The 16n stays
  first-class when present; nothing here depends on it. Headless operation is
  untouched.

---

## 1. Philosophy

On the hardware, patching is a gesture: banana between two fingers, a stab, you
hear it *now*, you rip it out. In the v1.0 GUI, patching is a form: find the
coordinate, drag by millimeters, read a number. Nobody plays a form. The v1.0
grid is a fine *ledger* — it stays, demoted to inspector — but the playing
surface must be rebuilt around five laws:

1. **Proximity is meaning.** A socket lives beside the knob it feeds, on the
   body of the organ it modulates — never in a separate bureau.
2. **The hand listens.** Every patch gesture is audible *while it happens*,
   not after the form is filed.
3. **Anarchy over administration.** Fast, slightly sloppy, always reversible.
   There are no illegal bananas: any source into any destination, always.
   Mess is music.
4. **Software is not a photograph of hardware.** The mouse has two superpowers
   a banana never had — hover, and the single continuous 2D gesture. Build on
   those instead of imitating cables apologetically.
5. **What breathes gets touched.** The panel moves with the real signals
   (telemetry already streams); a surface that visibly lives invites the hand.

Layer creed for PC-only use (updates the spirit of SPEC §6, text edit
deferred): **GUI = the eyes + the hands. Code = the memory + the rituals.
16n, when present, = the continuous hands.**

---

## 2. Structure — one window, two views, one strip, one drawer

| region | role |
|---|---|
| **Day panel** | the current four columns + master strip, with sockets moved onto the organs' skin (§5). Knob work, switches, meters. |
| **Night map** | the archipelago (§6): a topology-true landscape for patching and listening only — no knobs, no numbers. |
| **Master strip** | persistent across BOTH views, never covered: vol, dry, space, depth, GR meter, notice line, REC + elapsed, PANIC. |
| **Drawer** | slides open on demand (click, no key), never over the master strip. Tabs: **inspector** (the old grid), **workshop** (§8 — the ledger page that completes SPEC §6.2's reachability without Day), **spellbook** (§8), **memory** (snapshots + morph, moved here). |

Keyboard: the sacred map is unchanged and global in both views —
`1/2` hold · `q/w` flip · `a/s` skip · `3..7` spesal · `r` rec · `space` panic
· `p` shake. Night adds **Tab** = latch/unlatch water focus (§7.1; ignored
while text entry or the Drawer has focus). Day/Night is now a deliberate
toolbar destination only. **Esc** = drop the banana in hand / release solo-light,
**Ctrl+Z** = matrix undo. Widgets stay non-focusable (existing focus law);
Esc and Ctrl+Z are window-level keys.

---

## 3. Bodies, jacks, and the color law

Two color systems coexist on the hardware; the software separates them with
one law so implementers never have to decide case by case:

> **Jack color = signal family. Cable color = polarity. Insertion depth = |weight|.**

- **Source jacks** are solid dots in their family color (SPEC §6.2 accents).
- **Destination sockets** are ink rings; family-colored where SPEC assigns a
  family (spaf purple, fm blue, spesal green); gate/trig sockets carry a small
  glyph and a **charge lamp** (§9).
- **Cables** are verso **amber** / inverso **blue** (the existing grid
  convention), drawn as gravity-sagged arcs; width and how deep the banana
  sits in its socket ∝ |w|; alpha pulses with the source's aura (§9).
- Multiple bananas in one socket = a stack (hardware-true). A stack spreads on
  hover; every banana in it must be individually grabbable.

### 3.1 Source inventory (16)

| source | family · color | day home | night home |
|---|---|---|---|
| `tri1`–`tri5` | triangle · orange | castle row *k*, Quantussy column | triangle bud on castle *k* |
| `cas1`–`cas5` | castle out · gray | castle row *k* | crenellated bud on castle *k* |
| `envA`, `envB` | input env · green | preamp A/B rows, sources column | root tips, bottom left/right |
| `coco1`, `coco2` | delay voice · gray | coco strip header | the spiral's mouth |
| `org1`, `org2` | organ · yellow | coco strip organ row | fruit hanging on the spiral |

### 3.2 Destination inventory (22)

| dest | kind | socket | day home | night home |
|---|---|---|---|---|
| `cocoN_spaf` | continuous | purple ring | beside the spaf knob | spiral moon |
| `cocoN_inVca` | continuous | ink ring ▷ | beside the in knob | spiral moon |
| `cocoN_fbVca` | continuous | ink ring ▷ | beside the fb knob | spiral moon |
| `cocoN_flip` | gate | ink ring ⇋ + lamp | beside FLIP | spiral moon + lamp |
| `cocoN_skip` | trig | ink ring ✦ + lamp | beside SKIP | spiral moon + lamp |
| `castleK_fm` | continuous | blue ring | beside castle *k* range switch | inner moon of castle *k* |
| `castleK_spesal` | trig | green ring + lamp | beside castle *k* step button | outer moon of castle *k* + lamp |
| `deckA/B_rate` | continuous | ink ring | beside deck rate control | socket under the bird's wing |

Gate/trig reminder for players and implementers alike: these pass the Schmitt
threshold (+0.5 on, +0.3 off; SPEC §5.5). A lone banana at modest strength may
never fire — that is why the charge lamps exist (§9): the panel shows the
charge instead of leaving silence mysterious.

---

## 4. Interaction grammar (identical in both views)

| gesture | meaning | state path |
|---|---|---|
| press a source jack | banana in hand; polarity = button (left = verso, right = inverso) | — |
| drag near a socket (≤ auditionRadius) | **audition**: live weight = handStrength × closeness², swelling as you approach | `matrix.patch` (funnel, clamped) |
| release on/near a socket | **plant** at exactly the audible weight (WYHIWYG) | commit; undo op |
| release in the void | nothing persists | audition reverted |
| Shift held at release | plant AND keep the source in hand (click-click mode: each click plants, Esc drops) | commits per plant |
| press a planted banana (socket end or cable) | back in hand, carrying its weight; release in void = **rip** (w → 0); release on another socket = **move** | commit; undo op |
| right-click socket/cable | instant rip (matches inspector) | commit; undo op |
| right-drag starting on empty ground | **harvest**: freehand lasso — on release, every planted banana caught inside rips together (§4.4) | commit; one undo op |
| wheel over socket/cable | |w| nudge ± 0.05 (Ctrl: ± 0.01), sign preserved, floor 0.05 | funnel |
| Alt+click socket/cable | polarity flip (verso ↔ inverso) | funnel; undo op |
| double-click any jack/socket/body | **solo-light**: its bananas bright, everything else dim; repeat or Esc releases | view-only |
| Ctrl+Z | undo the last committed matrix op | batched funnel write |

### 4.1 Audition protocol (the sacred bookkeeping)

- At most **one live audition** at any moment. Bookkeeping is a single record
  `{src, dest, prevW}`; revert = `patch(src, dest, prevW)`.
- Moving to a new candidate socket reverts the previous audition first.
- **Exhaustive revert paths — all normative:** cursor leaves the radius; new
  candidate socket; plant (candidate commits, any other preview reverts); Esc;
  release in void; view toggle; window close; GUI free; CmdPeriod; `CQ.panic`
  (state must not leak even while muted). No timers, no leaks; the smoke test
  asserts matrix equality after every exit path (§13).
- Audition writes flow through the normal funnel at mouse-move rate (see §15
  on zipper measurement).

### 4.2 WYHIWYG law

Planting commits exactly the weight being heard — no hidden defaults on the
plant path. The inspector's double-click plant (fixed 0.3, = shake's default
feel) is the only fixed-default entry.

### 4.3 Undo

GUI-local ring (default 32) of full-matrix copies. One op = plant / rip / move
/ polarity flip / harvest / shake / preset apply via GUI / breed / one
grow-or-wither hold / compass left-release. Elastic compass gestures (§7.1)
and auditions never enter the ring. Code-layer writes (`live.scd`) are not in
this undo — the spellbook is their memory. Undo writes go through the batched
funnel like any other client.

### 4.4 Harvest — the lasso

- **Start**: a right-button drag beginning on empty ground — no jack, socket,
  cable or widget under the press — in either view. A press that travels
  < 8 px is a no-op, never a lasso.
- **The loop**: the drag draws a freehand path; the region is that path
  closed by a chord back to its start (virtually while drawing, actually on
  release).
- **Membership**: sample every *planted* cable's drawn path, endpoints
  included, ~every 12 px; a banana is caught if any sample falls inside the
  region. Because endpoints count, circling an organ strips it bare — every
  socket and jack inside brings all its bananas. The audition banana is never
  harvestable: it belongs to the hand and reverts by §4.1.
- **Preview**: while the lasso is open, every currently-caught banana lifts
  its plug 2 px and gains a danger-red under-glow (polarity color unchanged —
  the hue-alone law, §16.2). Leaving the region drops the mark.
- **Release**: the whole catch rips together through the batched write — one
  commit, one undo op, one spellbook entry (§8). An empty catch is a silent
  no-op with no undo entry.
- **Esc** cancels mid-draw; marks drop, nothing changes.

### 4.5 Hover and dense-patch hit priority

- **Controls first.** A visible interactive control owns its rectangular hit
  area even when one or more cables cross it. Its own hover cue appears and
  cable focus clears; click/drag/wheel operate the control. The overlay may
  never advertise a cable action that the next mouse gesture will not receive.
- **Cables on paper.** On non-interactive paper, labels and organism body, the
  nearest cable inside its narrow path target owns the pointer. It reaches
  full alpha with a soft, static ink under-stroke while unrelated planted
  cables recede to 46 % alpha. No pulse or telemetry aura is used for focus.
- **Endpoints are cable territory.** Jack/socket/plug hover wins in its own
  geometry. A socket stack still fans out (§3.2), providing the precision path
  when several bananas share an endpoint. At a control crossing, select the
  cable from an exposed span or its endpoint instead.
- **One hover language.** Every interactive Ink control responds immediately:
  knobs gain a quiet outer ring, slider/switch handles a small halo, and
  buttons a light wash plus stronger outline. Read-only meters and lamps do
  not pretend to be interactive. Hover is static and never changes state.
  An opened choice list is still an interactive Ink surface: **every selectable
  row under the pointer** gets the same light wash and stronger outline before
  click. Styling only the collapsed box or only the currently selected row is
  incomplete. Persistent selectable lists, including Memory's saved snapshots,
  likewise highlight the row under the pointer without changing selection.
  Row paint, hover and click must resolve through the same geometry. If a
  platform-native list does not expose its real row bounds, do not estimate
  them from a Drawer metric: use an Ink-painted list whose row height is owned
  by the application.
  In Qt this binds ordinary `mouseOverAction`; `mouseMoveAction` alone covers
  only movement while a button is held.

Deferred dense-patch escape hatches, recorded but **not current gestures**:

1. A Drawer `cable opacity` control, with the focused cable remaining fully
   legible, is evaluated with the completed G1 Drawer.
2. If controls-first plus endpoint fan-out is insufficient, add a transient
   **banana-focus** hold mode that temporarily gives the cable layer priority;
   its key/modifier and visible mode notice require a separate usability gate.
3. A persistent edit/performance mode is the fallback only if the transient
   model fails. It must not be introduced merely to solve visual clutter.

---

## 5. Day panel

The four columns stay — muscle memory, hardware mirror, nothing wrong with the
body. What changes:

- **Skin sockets.** Every destination socket sits beside the control it feeds
  (per §3.2): spaf socket at the spaf knob, inVca at the in knob, fbVca at fb,
  flip at FLIP, skip at SKIP, `castleK_fm` at castle *k*'s range switch,
  `castleK_spesal` at its step button, deck rate at the rate control. Source
  dots sit at their organ's edge (§3.1). Proximity is meaning.
- **Cable overlay.** A window-spanning layer draws bananas across columns. It
  must never steal mouse events from knobs and buttons — only jacks, sockets
  and cables are hit targets (targets ≥ 12 px, magnetic halo marks the current
  candidate). The controls-first law and cable focus are defined by §4.5.
- **Declutter.** Number boxes, snapshot save/recall and morph A/B/duration
  move to the drawer's memory tab. Deck file-drop + filename label stay.
  Meters, LEDs, spesal display, shake button + amount knob stay. **`amount` is
  how much a shake sows**, not how hard (SPEC §5.5's `density`; strength is
  `patchDepth`'s job) — one state shared with the night's sowing moon and the
  `p` key: three layers, one number, one light.
- The old matrix pane leaves the main window → drawer inspector (§8). The
  patch-preset menu moves to the inspector header, **and that is its only
  home**: the night carries no presets (decided 2026-07-22, superseding the
  sower constellation — see §16.6). The Drawer tab sits in the persistent
  toolbar, so the recipes are one click away from either view.

---

## 6. Night map — the archipelago

The toolbar flips the same window to night: a patch-first landscape. No transplanted
panel widgets and no numbers — only bodies, jacks, bananas, weather, and the
bodies' own touch points (§6.1). The master strip stays below.

> Amended 2026-07-21 at the user's direction: the night must stand alone as an
> instrument. On the wooden box there is no day mode — the knobs are always
> under your fingers — so the night bodies grow their own touch points instead
> of borrowing the day panel's widgets. The original "no knobs" line survives
> as "no transplanted widgets": nothing on the night map looks like a knob,
> but everything essential can be played.

Topology-true layout (coordinates are a starting sketch; implementers tune by
eye but must keep the topology truths):

- **Five castles in a true ring** (pentagon, castle 1 at top, flow arrows
  k → k+1 — the ring *is* the LocalIn loop, and matches the `\classic` FM
  ring). Each castle: triangle bud (orange source), crenellated bud (gray
  source), inner moon `fm` (blue ring), outer moon `spesal` (green ring +
  charge lamp), an **8-notch spesal dial** with the current state lit (data:
  the existing `/cq_spesal` cache), body glow = its aura (§9). A playable rate
  ripple at the castle's 6 o'clock carries range without text:
  one/two/five half-waves mean infra/balcium/audio, and a tap steps the regime
  (§6.1, §16.6).
- **Coco 1 left, Coco 2 right: spiral shells** (the delay RAM as a coconut
  cross-section). A **read-head spark travels the spiral** (loop-phase
  telemetry, §9); hold = the spiral ring solidifies red; flip = the spark
  reverses. On the shell: org fruit (yellow source), the spiral's mouth (gray
  source), and moons spaf / inVca / fbVca / flip / skip (lamps on the
  gate/trigs).
- **Inputs as roots** at the bottom: envA left, envB right; root tips glow
  with follower level (aura).
- **Decks as birds** at the top: deckA / deckB. The bird itself fetches:
  **tap opens the system file dialog in the configured samples folder**, wheel
  down/up walks to the next/previous file in the loaded folder, and a short
  horizontal out-and-back ruffle chooses a random file. Dropping an audio file
  on the bird remains an equal loading
  path; the wing socket is the rate moon, and the filename is written small
  under the bird.
  **Accepted 2026-07-28 (checklist N6):** the complete tap, wheel and
  Windows/Qt-safe ruffle grammar passed live playing review.
- Cables follow the same grammar (§4), arcing around bodies, pulsing with
  their source's aura. Solo-light on a body lights everything touching it.
  The night's bodies (landed 2026-07-21): each **castle** (its two buds, its
  fm and spesal moons), each **shell** (mouth, fruit and its five moons) and
  the **ring** — the last three under the Day panel's own body names
  `coco1` / `coco2` / `quantussy`, so one shared grammar never holds two
  solos for one organ, while the castles are the night's finer bodies. Roots
  and birds get no body: each carries a single endpoint whose own jack/socket
  solo already lights the very same cables.

  **A body is taken by its own drawn outline** (decided 2026-07-21): the
  castle's blob, the shell's outer rim, the ring's line — each a mark the eye
  already reads as *the whole organism*, each narrow enough to leave the water
  around it free for the harvest lasso, and each its own hover cue (§4.5:
  the castle wears the quiet outer ring, the shell draws its rim, the river
  brightens). Cue and action always share one geometry; a cue that covers more
  or less ground than its action is a cue that lies. Controls-first still binds
  at every click count — a fast second tap on the inner clock, the heart, the
  chevron, the skip mark or the eye belongs to that touch point, and where the
  winding surface runs close to the shell's rim it is the wind that answers,
  with the wind's own cue.
- **Night toolbar → the almanac corner** (decided with the user 2026-07-22,
  superseding the earlier lowercase word-row sketch): no bar, no box, no
  menu. The top-right sky carries the **sowing moon** — shake and how much it
  sows in one body, the lit phase *is* the value — and beneath it the §7
  weather tools' G3 home, reserved as empty water (§16.6). The old "amount
  mini-knob" is superseded: nothing on the night map looks like a knob.
  A **sower constellation** of preset stars stood here for one review round
  and was removed the same day at the user's direction: a preset is a recipe,
  not a body, and it was the night's only control that could not be used
  without reading text (§16.4) — six identical glyphs you had to hover one by
  one, which is a menu in costume. The night's grammar is bodies, bananas and
  weather; recipes live in the Inspector, one toolbar click away.

### 6.1 Night touch grammar (normative, decided 2026-07-21)

The essential playing controls live on the bodies, in each body's own logic.
All writes go through the normal funnels; parameter gestures have no undo
(§14); gesture redraw ≤ 60 Hz only during the drag (§10). Except for an
explicitly named exception, on every press the cable layer is consulted first:
a cable inside its narrow hit target is grabbed, never wound or tapped through
(§4.5 — the overlay may never advertise an action the next gesture will not
receive). The one approved future exception is the tide-focus gill recorded
below; it does not silently grant priority to any other control.

| touch | gesture | writes |
|---|---|---|
| **wind the shell** | left-drag along the spiral's drawn line (≤ 8 px); turning toward the mouth winds faster, against it slower — the two shells mirror like their spirals. The reel notch rides the line as the live hand, and hover brightens the grab (§16.6) | `cocoN speed` (relative, ~2 turns = full range) |
| **the shell's heart** | **left tap** the small mark at the spiral's innermost eye: seal / crack the loop; red while sealed (one law with the outer seal and HOLD) | `cocoN hold` toggle |
| **the heart's punch** | **right press held** on the same heart mark: momentarily *invert* the seal for as long as it is held — a looper's punch-in (sealed → material drops into the ring; open → a fragment freezes), restored on release. The exact prior value is captured and restored on **every** exit path — mouse-up, Esc, Tab, panic, window close, GUI free, CmdPeriod — because a stuck punch leaves a loop recording over itself (a right press on a *cable* crossing the heart still rips, §4.5). No timer: the split from the left tap is the button, not the clock | `cocoN hold` (momentary invert, no undo) |
| **the mouth chevron** | press the read-direction chevron at the mouth | `cocoN flip` toggle |
| **the mouth's skip mark** | press the spark glyph hanging under the chevron (10 px box, stationary). Momentary: 120 ms ink flash on *any* skip, the Day button's law (§16.7) | `skip(n)` — the loop restarts from the eye |
| **tide crescents** | a small crescent below each spaf / inVca / fbVca moon, luminous fill = value; vertical drag pours more or less. Socket beside its control — §1's proximity law, reborn at night. spaf wears purple | `cocoN spaf / in / fb` |
| **Dolby oysters** | beneath each in/fb tide crescent, tap the captionless paired arcs: open = off, nearly shut = gate, shut with a partner-facing arrival stroke = duck. Oyster hover and crescent hover are exclusive; a drag may begin only on the crescent's original square | `cocoN dolbyIn / dolbyFb`: `off → gate → duck → off` (CQSpec enum order) |
| **the ring's eye** | the storm iris at the ring's center (§16.6); grab and drag — vertical = pitch, horizontal = chaos, and the iris itself answers on both axes. The weather of five castles from one fingertip | `qFreq` (y) + `qChaos` (x), relative |
| **dial tap** | press a castle's inner clock — the dial face inside the body (§16.6): one step (same act as the day step button and keys 3..7). Hovering the castle first shows **which castle it reads** as an inner chord (§16.6's coupling chords), so the step's consequence is visible in the gesture that makes it | `spesal(k)` step |
| **range ripple** | tap the drawn rate at a castle's 6 o'clock: one slow swell → one full cycle → five tight half-waves → one slow swell. The form is the whole regime display; no legacy `i/b/a` caption remains. Cable-first remains absolute, and ripple/clock/body hover cues are exclusive | `cNRange`: `infra → balcium → audio → infra` (CQSpec enum order) |
| **bird fetch + drop + wing crescent** | tap the bird with < 8 px travel to open the asynchronous system file dialog at the configured samples folder; cancelling is inert. Dropping an audio file remains an equal load path, and the crescent under the wing remains the deck's level | deck load · `deckN level` |
| **bird folder walk** | wheel down = next file, wheel up = previous. Qt momentum is gated to one sign-step per 150 ms so one flick cannot skip a handful of perches; before a load, the deck's own guard scans the samples folder | `deckN.next / prev` |
| **bird ruffle** | horizontal drag travels ≥ 6 px and reverses once by ≥ 6 px — one natural out-and-back; release chooses a random file. Repeated or broad ruffling remains valid once that reversal is clear. A drag that is neither a tap nor a ruffle is inert | `deckN.random` |
| **the sowing moon** | the bone moon in the top-right sky (§16.6). It opens as a small 0.22 crescent. A press that travels < 8 px — the lasso's own threshold — throws a shake at the moon's current phase, with a 120 ms bone flash on *any* shake from any layer (`p` key, Day button, code: one state, three layers, one light). A **horizontal** drag pours the phase fuller (right) or darker (left), the way the terminator itself travels; the lit fraction is **how much gets sown**, through the slow-first quadratic fertility curve in §10, no number | `shake` (matrix op — one undo, one spellbook entry) · `shakeAmount` (GUI constant, no undo) |

Skip's night home moved at the G2 gate (2026-07-22, replacing the read-head
tap that landed 2026-07-21; the `a/s` keys remain). **A moving target is not a
control.** The comet was the only touch point in the night that travels, and
the hand could only land on it while the loop crawled — past the innermost
turn of the speed scale it was unhittable, and a 5 Hz frame draws it stale on
top of that. Its home is now a stationary mark under the flip chevron: the Day
panel's own FLIP/SKIP pair, in the one direction around the mouth that is
empty. Not above the jack, which looks symmetrical and is wrong — the spiral
*ends* at the mouth and arrives there from above, so that side is the groove
itself. The comet keeps its whole job, which was always the honest one: saying
where the loop is reading. The loop's finer diet — slots, bits and
interpolation — remains day work: the slot defaults (pre 1,
deck 1) make a dropped file audible at night with `in` up. Body solo-light
and the night toolbar arrive with the later G2 steps as planned.

---

## 7. The weather machine

The mouse's weakness is 352 small targets; its strength is one continuous
gesture. The weather machine gives the matrix a macro plane. **The landscape
law binds everything in this section: weather tools write weights only.** No
CQSpec parameter is ever touched by compass, grow, wither, breed or shake
(same law as SPEC §5.5 shake). Knobs belong to hands. Corollary, recorded in
`docs/history/PLAN_v1.0.md` (KUUNTELU 4): a landscape can be *deaf* if its depth-knobs are down —
aura and charge lamps make that visible instead of mysterious.

### 7.1 Compass — the bay-wide tide field (§16.6)

- Four corners = four **matrix memories** (weights only; never params, never
  spesal). The night has no buttons, so each corner is a **seed** (§16.6).
  A filled tap recalls bit-exact; Shift+tap writes or overwrites; with
  Tab/gill focus latched, tapping a vacant seed also writes, so the complete
  grammar remains available without a keyboard. An ordinary unfocused tap on
  a vacant seed is inert; right press clears. Drag one filled seed onto
  another to breed them. The ink has three truths: vacant = open husk,
  occupied silence = closed dark dormant kernel, occupied patch = luminous
  veined kernel whose light follows stored cable density.
  An empty corner = the zero matrix (all unpatched) — traveling toward it is
  a wither-by-travel, documented behavior. There is **no pad or pool body**:
  the four seeds stand in open winds around the central organism and imply
  one large rectangular bilinear field passing behind it. The field has no
  resting outline, fill, wave or sparkle.
- **The tide** (amended at G3, superseding the first bilinear-amplitude
  sketch — this is the blend law): a position on the pad pulls on the four
  corners bilinearly, but the pull decides **which cables stand, not how loud
  everything is**. Cells the corners *share* crossfade their weights
  continuously — the common skeleton glides — while cells only some corners
  carry keep their **full stamped weight** until the position's pull on them
  falls below that cell's own fixed **tide mark** (a deterministic hash of
  its coordinates, deliberately not session-seeded), and then they lift —
  one banana at a time, to exactly zero. At a pure corner the stamp comes
  back bit-exact. Three reasons, all musical: (1) a patch is discrete
  bananas, and half-way between two patches should be half the bananas of
  each *at full voice* — an amplitude blend of four sparse patches
  quadruples the cable count and quarters every weight, mush by
  construction; (2) gate/trig cables live behind the +0.5 Schmitt, so an
  amplitude blend silences every flip/skip/spesal banana almost everywhere
  on the pad, while the tide keeps them firing through the whole voyage;
  (3) fixed tide marks make the pad **learnable geography** — the same
  position always grows the same patch, across sessions. Weights below the
  wither floor (0.05) round to gone. Traveling toward an empty corner is
  therefore *literally* wither-by-travel: cables dropping off one by one.
- **Focused left-drag** on genuinely open water inside the four winds: the
  tide pours continuously after the lasso's own 8 px threshold (batched funnel
  write, one transient notification per frame, ≤ 60 Hz — implemented at
  30 Hz, since the weights are unlagged control buses and faster stepping
  buys no audible smoothness). Release **commits** (one undo op, one
  `// compass` spellbook batch). Outside water focus both a tap and an
  arbitrarily long left sweep are a **no-op**: exploratory movement cannot
  replace the patch accidentally.
- **Water focus** has two entrances into one state. **Tab** latches it until
  the next Tab, and the small clickable **gill** below the two lower castles
  opens/closes the exact same latch. Native key repeat is suppressed: one
  physical Tab press toggles once. **Shift** with no banana in hand remains
  its momentary form. In focus, planted cable
  spans dim to background alpha and stop hit-testing, while bodies, jacks,
  sockets and seated plugs keep priority. The four seeds breathe and the
  gill opens in blue, so the mode is visible without a label.
- **Accepted polish debt (2026-07-27; not yet implemented):** the gill is the
  persistent entrance to a whole-stage playing mode, so it must ultimately be
  consulted **before** a cable span in both hover and press. A crossing cable
  must not erase its discoverability: its cue needs a top-layer echo, local
  knockout or another reviewed form that remains legible through the cable
  field. This is a narrow exception for the Tab-equivalent gill only; the
  general cable-first law remains intact everywhere else.
- **Focused right-drag**: **elastic** — the same pour, but release snaps back to
  the pre-gesture matrix (short glide, ~3 frames; no undo entry, no
  spellbook entry — transient all the way). The whole matrix becomes a
  wobble you can flick, and under the tide the wobble has *texture*:
  crossing a cable's tide mark lands and lifts a real banana. Outside focus,
  right-drag remains the learned harvest lasso. Once a tide gesture crosses
  its threshold, pointer capture continues across bodies and cables until
  release.
- **Storm form** (the §10/§11 measured consequence): while a voyage is being
  poured the matrix changes every frame, so the cable layer's offscreen
  cache cannot help — the bay is instead drawn *through rain*:
  polarity-colored fixed-width cores only, no plugs, no barbs, each cable
  its own small stroke (the obvious "one giant path per polarity" was built
  first and measured **worse than full ink** — 505 ms against 177 at 352
  cables; Qt's stroker goes superlinear in one path's subpath count), and a
  repaint happens **only when the planted set actually changes** (a banana
  landing or lifting is an event; a glide of shared weights is drawn as
  nothing and costs nothing). Release returns full ink at once.
- `patchDepth` stays the global tamer on top; the pad never touches it.
- **Persistence:** corners auto-save to `presets/compass.cqcompass.scd` (pure
  data expression, integer version field, validated and clamped on load,
  malformed file rejected with a warning — CQSnapshots' safe-load manners) and
  auto-load at GUI open. `config.scd` may override the home with
  `gui: (compassDir: ...)` — the smoke tests point it at a tmp dir so a test
  stamp can never overwrite the player's corners.

### 7.2 Grow / wither (the moon's tensioned vertical weather)

- **grow**: pick a random unpatched cell, plant weight ± U(0.15, 0.4), random
  sign. — **wither**: pick a random patched cell, w ×= 0.7; |w| < 0.05 → 0.
- On the sowing moon, the dominant axis locks after 8 px: horizontal pours
  phase, vertical down holds rain/grow and vertical up holds sun/wither.
  The next 32 px of vertical travel are **weather tension**. Near the moon,
  rain drizzles at 0.6 Hz and sun thins at 2.5 Hz; at full pull they reach
  1.6 Hz and 6 Hz. Rain is deliberately slower because each step births one
  complete cable, while one sun step only multiplies a random cable by 0.7.
  The rain gains drops and the sun gains rays with the same normalized
  tension, so the symbol shows the current rate. Rain or sun is drawn only
  while its gesture is active.
- One hold = one undo op. Language-side rand, no seed UI (seeded chaos stays
  in the code layer: `CQ.shake(amt, seed:)`).
- Shake is the lightning; these are the rain and the drought.

**Deferred musical-design pass — ecological shake.** The current shake still
chooses matrix cells uniformly. A later, separately reviewed pass may generate
“sweet spot” patches from weighted source/destination affinities, guaranteed
audible paths and bounded feedback/gate populations. It changes the musical
grammar, not this moon layout, and was explicitly deferred on 2026-07-27.

### 7.3 Breed

Drag one filled corner seed directly onto another → the offspring becomes
the current matrix (one undo op). There is no graft button or armed mode.
Per cell: coin-pick a parent's weight; then with
p = 0.08 mutate by ± U(0, 0.15); clamp. Children differ but rhyme.

---

## 8. Drawer — inspector, workshop, spellbook, memory

> **G1 feedback experiment, 2026-07-20 — core direction visually accepted;
> the navigation/Memory polish below remains provisional until review.**
> Prototype the Drawer as a stable **workbench**, not a resizable bottom dock.
> A compact, always-labelled `drawer` tab sits beside the Day/Night control in the
> persistent top toolbar: its down chevron opens the workbench into the central
> area and its up chevron returns to the instrument. Inspector/Spellbook/Memory
> remain page names only inside the workbench. Choosing Day/Night while the Drawer
> is open also returns to that instrument destination. Master remains visible and
> live; there is no Drawer resize, whole-instrument scroll, click-away close or
> second window. The selected Drawer page persists across close/reopen.
>
> In that workbench, prototype Inspector as a full-width **patch loom** while
> preserving the exact canonical 16×22 matrix and every gesture below. Target cell
> size is approximately 48–52×24 px at the 1280×720 minimum. Destination symbols
> are grouped into meaningful islands (`coco 1`, `coco 2`, `castle · fm`,
> `castle · spesal`, `decks`) with short diagonal child labels; source rows retain
> canonical order but expose their signal families. Replace the spreadsheet field
> of filled rectangles with quiet rails/intersections: an occupied cell becomes a
> seated knot/plug whose size shows `|weight|`, while polarity uses both the normal
> amber/blue law and distinct geometry. Hover emphasizes one source row and one
> destination column; selection persists as an outlined cell plus a fixed exact
> route ribbon. The route ribbon, preset controls, header, grid and gesture footer
> share the same exact loom-width content rail; the footer cannot clip and the
> grouped column headers sit close to their diagonal child labels. Presets live in
> the Inspector header. Informational `/cq_aura` and
> charge animation may inhabit these marks later, but are deliberately excluded
> from the first static prototype. Chaos belongs to the planted pattern, never to
> unstable coordinates or decorative motion.

- **Workshop** (added at N8, 2026-07-28) = the bay's quiet **measurement table**:
  the same instrument in the opposite grammar to Night. Night is open water,
  asymmetric bodies, topology, gesture, almost no text and no numbers; the
  Workshop is recessed slate, straight rules, exact names and values, stable
  coordinates and no uninvited motion. The relation comes from the shared Ink
  kit, palette, type, line weights, family colors and hover language — never
  from decorative biology. **Do not** transplant Night's shells, roots, birds,
  wobble or weather into it, and do not shrink the Day panel into it. In Night
  skin it reads as a phosphor-lit field ledger beside the bay; in Day skin the
  identical geometry becomes ink on recessed paper. The hard cut changes the
  light, never the information architecture. Night's lights stay informational
  here too: no aura, glowing borders, cable animation, texture, decorative
  gradients or organism wobble — only a changing parameter, hover/press,
  `micGain` danger or another real state may brighten a mark.
  - It owns the **43 parameters** Night and Master do not, so that a player who
    starts in Night and uses only Night, the Drawer and the Master strip reaches
    every `CQSpec` parameter (SPEC §6.2). The interactive partition is
    **Night 31 · Master 5 · Workshop 43**, disjoint, and its union is asserted
    against `CQSpec.names` itself rather than a copied count. Workshop ownership
    is an **explicit manifest**, never `CQSpec.names` minus the others: a newly
    added parameter must fail the audit instead of silently counting as covered.
  - **Composition.** One centred **content rail**, shared by all four Drawer
    pages and always exactly the Inspector loom's own width, so the workbench
    reads as one surface. One continuous ground recess, not a board of rounded
    cards; hairline separators and gutters on the 8 px grid; the Drawer's own
    soft edge stays the app's one shadow. A shallow service band (four equal
    modules: `deck a · input trims · space · deck b`) sits over a
    `coco 1 · quantussy · coco 2` body, whose three columns and two gutters sum
    back to the rail exactly — an odd split gives its remainder to the middle so
    the two Coco ledgers stay literally the same width. The page carries **no
    height constants**: each island is a stack of fixed-height rows over a
    trailing stretch, so the rail asks for exactly the height its content needs,
    spare room collects below as slate, and there is **no scroll view** at any
    size. Space stays a neutral service module and gains no invented Night body.
  - **One shared page frame.** All four Drawer pages open with exactly the same
    air between the tab row and their first ink, centre exactly the same rail,
    and begin their first ink at one **content inset** inside it — the
    Workshop's labels, the loom's row names and the seed bank's boxes all start
    on the same vertical line. The workbench reads as one surface rather than
    four adjacent layouts. The bottom is deliberately free — the pages hold different amounts
    of content — and a page whose body can use extra height (Memory's
    snapshot column and patch fingerprint) takes it rather than stopping at a
    constant. Added 2026-07-28 after the N8 review found the Inspector on a
    4 px margin and the others on 12.
  - **Two reviewed size steps, never a continuous zoom** (added 2026-07-28 at
    the user's N8 review; `CQGuiScale`). This page is *only* text, rules and
    rails, so legibility is its whole job, and a maximized window that spends
    its extra room on empty slate has wasted it. The Drawer therefore has two
    measured compositions — **compact** for the 1280×720 minimum (page
    1272×526) and **roomy** for anything large enough to hold it — differing in
    type size, row height, gutters and rail width. Both thresholds are
    **derived from the roomy step's own metrics**, never written down beside
    them: a fixed threshold silently stops matching the moment a metric moves,
    and this one decides whether a maximized window gets bigger type at all.
    Windows laptops are commonly run at 125 % or 150 % scaling and sclang works
    in logical px, so a 1080p panel can report a page of only ~1528×630 — the
    step floors must clear that, or the one window size the instrument is
    actually played at never grows. At 150 % the logical window *is* the
    1280×720 minimum, and staying compact there is correct rather than a fault.
    Qt reports a new rect into the language, so the step follows the window with
    no timer and no polling, and every page **re-measures rather than
    rebuilding**: a resize can never re-register a funnel binding or resurrect a
    stale control. It needs **two hooks, not one** — the drawer stage is a
    StackLayout page and a hidden page is not laid out, so resizing with the
    Drawer closed would otherwise leave a stale size behind; the window's own
    view always resizes, and reopening the Drawer re-asks. Two
    discrete steps keep coordinates stable inside a step and keep type at sizes
    that were actually looked at. The rail grows with the page until the loom
    cell reaches its cap, after which the surplus becomes slate — a knot grid
    spread over two thousand pixels stops reading as a grid — but a **page
    margin is always kept**: the rail may never reach the edge. §16.10's "font
    sizes stay in px" still binds the Day panel and the Night landscape, whose
    ink must not thicken when the paper grows; this applies to the Drawer alone.
  - Rows are grouped by **signal meaning**, never alphabetically: each Coco as
    *converter* (`bits`, `wrap`, `interp`) · *material* (`dust`, `organ`) ·
    *output* (`level`, `pan`, `send`) · *input mix* (`pre`, `deck`, `q`, `x`);
    the Quantussy as *s&h* (`qShLag`) · *detune* (`c1Off`…`c5Off`) · *audio
    sends* (`c1Send`…`c5Send`), castles running 1→5 in both so the lists compare
    without hunting. **Pair symmetry is structural**: both Coco ledgers are built
    by one helper with only the index changed, so their subgroup baselines match
    by construction.
  - **Three ranks, told by rule length rather than by shouting** (amended
    2026-07-28: the first version's 9 px whisper subgroups did not separate the
    groups enough to be read at a glance). §16.4 keeps everything lowercase but
    PANIC/REC/HOLD, so emphasis comes from *rank*, not from caps or a new hue:
    an island **title** carries a rule across the whole island; a **subgroup**
    carries a lighter rule from the end of its own name to the island's edge and
    is set in line ink one step below the title; a **row** carries only its value
    rail, and its label is the whisper. Labels sit close to their rail and are
    never boxed or rendered as pills. Spare vertical room collects below the
    ledgers as slate rather than stretching rows apart.
  - Semantic phosphor is used **sparingly**: slate is `recess`, rules and values
    are bone/whisper, and only `dust`, `organ`, the live input and the mic carry
    a family accent. Neutral slots, levels, pan, sends and Space get no family —
    do not rainbow a column, and inline no color at the draw site.
  - **`micGain` lives here and nowhere else.** SPEC §6.2 forbids a second
    interactive mic path, and the night-first completeness gate needs the mic
    without Day, so N8 **moved** the control and its warning owner out of
    Day/Sources; Day keeps only a passive `mic · drawer / workshop` pointer and
    the night's read-only mic sprout is unchanged. The row uses the warning
    family, gains the danger outline while live and drives the existing
    persistent `MIC LIVE` notice. That subscription exists for as long as the
    GUI does: **closing the Drawer may not silence the warning**, and the row
    may brighten red while its surrounding service module does not glow.
  - The page is rejected if it reads as any of: four rounded settings cards; a
    dense native control panel; a miniature Day; Night scenery used as
    decoration; independently glowing sections; controls spread evenly to fill
    every pixel. The squint read must be **service band above · coco / quantussy
    / coco below · one red mic exception**; the close read must give every exact
    value.
- **Inspector** = the existing 16×22 grid, demoted but upgraded with
  **touch-and-listen**: press a cell = audition ± 0.3 (mouse button = sign)
  until release, same bookkeeping and revert law as §4.1; double-click =
  plant 0.3; vertical drag = fine edit (current behavior); right-click =
  clear (current behavior). This is the precision path: everything the
  gestural surfaces do must remain exactly doable here. Hovering a valid cell
  gives that exact cell a light `spark` wash and outline, in addition to the
  quiet row/column focus, so the selectable target is visible before press.
  The preset choice box uses the same cue. Its opened choices are Ink-painted
  ground/line rows with a `spark` hover, not an unstyled native Qt menu.
- **Spellbook**: an append-only log of *committed* matrix ops from **all
  layers** (GUI gestures, code, presets, shake, compass commits — never
  auditions or per-frame compass writes), each as a copyable line in the
  code layer's own tongue: `CQ.patch(\tri3, \coco1_spaf, 0.4)`. Batch
  commits (harvest, breed, compass release, preset, shake) log as one
  `// tool` comment line followed by the diffed cell lines — a harvest reads
  as `// harvest` plus its `CQ.patch(\src, \dest, 0)` lines. Buttons:
  copy line · **copy all as preset data** (`(matrix: [[src, dest, w], ...])`
  to clipboard). The journal is read-only, uses one Ink paper/text pair instead
  of native per-row background colors, and keeps the clicked line visibly selected.
  The GUI teaches the language instead of competing with it.
- **Memory**: a **seed bank** around the unchanged snapshot laws. The live patch is
  always the first truthful seed; saved names remain explicit save/recall targets.
  **Two memory words, no third.** A *preset* is a patch recipe that lives in code
  (weights plus the few knobs without which those bananas are deaf); a *snapshot*
  is saved instrument state on disk (every param, the matrix, spesal). "Seed bank"
  may name this page's metaphor, but every item in it is called a snapshot, and
  the Inspector's recipes are called presets. Decided 2026-07-21.
  Selecting a seed previews a read-only mini patch fingerprint, route/polarity
  counts and outlines every knot that differs from the live matrix. A and B remain
  explicit saved seeds joined by the duration-controlled morph bridge; its meter
  follows real `CQSnapshots` progress. Selection never recalls by itself, and this
  experiment adds no delete control or implicit write/recall gesture. The
  “outlined knots differ…” line is a tiny side note with two row gaps of
  clearance above the destination headings, never heading-sized or attached
  to the `decks` heading below it. It is Pen-drawn inside an explicitly roomy
  band rather than entrusted to a native label's opaque content rectangle.
  The saved-snapshot list is likewise an Ink-painted field: row drawing,
  pointer hover and selection use one exact row height. Both Morph Bridge
  choice boxes share the Inspector preset box's closed and opened Ink hover
  language.

---

## 9. Telemetry (the only server-side changes in this spec)

Existing v1.0 streams: `/cq_cocometers` (in/out peaks, id 1/2), `/cq_castle`
(5 tri values, 15 Hz), `/cq_gr`, `/cq_spesal`, `/cq_hot`.

Additions — unconditional control-rate SendReply at ~15 Hz (`/cq_phase`
deliberately slower, see its row), NRT-harmless, same pattern as the existing
meters:

| phase | message | payload | drives |
|---|---|---|---|
| G1 | `/cq_aura` | 16 source magnitudes (lagged \|amp\|, canonical source order) | jack glow, cable pulse, root/castle/body glow |
| G2 | `/cq_phase` | coco read-head position 0..1, **the read direction as an audio-rate mean** (`Lag.ar(dir, 0.15)`) and **the skip rate in Hz** (added 2026-07-22: impulses of height `SampleRate` through a one-pole settle at their own rate, because a `Lag`'s DC gain is 1 — so the value *is* triggers/second, over the combined reset `\t_skip + skipTrig` that Phasor actually obeys, smoothed 0.2 s and self-clearing. It exists because a rate this fast cannot be drawn as motion — see §16.6), replyID 1/2 (landed 2026-07-21 as its own message at **5 Hz** on purpose: the send rate is the two shells' repaint ceiling — §10, one shell repaint ≈ 30 Pen calls, so 15 Hz would spend the whole ~3 % GUI budget on two dots). The direction is a *mean*, not a sample, because a flip banana can reverse the loop far faster than 5 Hz can sample it: ±1 = settled, near 0 = shuttling. Deriving direction client-side from consecutive positions aliased and was replaced | the spiral spark, the comet's tail and the mouth chevron; the rate is the struck mark's weight |
| G2 | `/cq_charge` | the 9 gate/trig destination sums (post-weight, pre-Schmitt); landed 2026-07-21 as **three 15 Hz blocks** sent by the synths that already compute those sums — coco 1 (replyID 1) and coco 2 (replyID 2) report their own flip/skip pair, the quantussy (replyID 3) all five spesal sums. Each sum is **peak-held at audio rate** (instant rise, 0.15 s fall) before `A2K`, because a trigger crossing can last a single sample and a plain control-rate sample would miss it; the fall is also §16.8's fired bloom. No second copy of the sums exists in the graph, so a lamp can never disagree with its Schmitt | charge lamps — the answer to "why is my flip banana silent" — and, for `cocoN_skip`, the read head's struck mark (§16.6) |

`/cq_charge` canonical channel order = the matrix destination order filtered
to gates/trigs, nine channels: `coco1_flip, coco1_skip, coco2_flip,
coco2_skip, castle1_spesal, castle2_spesal, castle3_spesal, castle4_spesal,
castle5_spesal`.

No safety thresholds change; NRT tests keep their caps; message merging is
the implementers' choice. Added sends must fit the §11 performance budget.

---

## 10. GUI constants

GUI-local (not CQSpec parameters — they are gesture properties, like
`shakeAmount`). Defaults below; optionally overridable under a `gui: (...)`
sub-event in `config.scd`, clamped on load.

| constant | default | meaning |
|---|---|---|
| `handStrength` | 0.5 (0..1) | audition & plant ceiling |
| `auditionRadius` | 40 px | socket capture distance |
| audition curve | closeness² | swell late, near the socket |
| `inspectorPlant` | 0.3 | double-click plant weight (= shake default) |
| `shakeAmount` | 0.22 | the sowing moon's opening phase = the Day amount knob; an iconic sparse crescent (§16.6) |
| `fullMoonDensity` | 0.2 | the full moon's `density` (SPEC §5.5); intermediate density = `0.2 × phase²` (quarter ≈ 1.25 %, half = 5 %) |
| wheel step | 0.05 / Ctrl 0.01 | weight nudge |
| rain rate | 0.6…1.6 Hz | vertical pull tension; one new cable per step |
| sun rate | 2.5…6 Hz | vertical pull tension; one random cable ×0.7 per step |
| grow weight | ± U(0.15, 0.4) | new banana strength |
| wither | ×0.7, floor 0.05 | decay per step |
| breed | p 0.08, amt ≤ 0.15 | mutation odds / size |
| compass write rate | ≤ 60 Hz | batched writes during drag |
| `undoDepth` | 32 | matrix-op ring |
| ambient redraw | ≤ 15 Hz | telemetry-driven animation |
| gesture redraw | ≤ 60 Hz | only during an active drag |

---

## 11. Contracts for implementers (behavior-level; the architecture is yours)

- **Pure view law** (existing, restated): every write goes through the
  CQState/CQMatrix funnels; clamps live there; the GUI never talks to the
  server; `{ }.defer` everywhere; dependants for updates; OSCdefs namespaced
  `\cq_gui_*`; cleanup idempotent on close, including the user's X.
- **Two small state-API additions are required** (naming and shape yours):
  1. a **batched full-matrix write** that clamps every cell and emits **one**
     change notification (compass, breed, undo need it — 352 per-cell
     notifications per frame would melt the redraw);
  2. a **full-matrix read** (undo copies, corner stamps, spellbook export).
- **View swaps must never block the language thread.** Hiding a view under the
  mouse pointer makes Qt deliver its enter/leave events back into the language;
  if the language thread is at that moment blocked inside the same Qt write, both
  threads wait forever (Tab over the Day panel froze the window, 2026-07-21).
  Day/Night and Drawer stage swaps are therefore queued, not direct, and the
  serverless shell state — not the native widget index — is the synchronous truth.
- **Redraw discipline**: ambient animation at telemetry rate (≤ 15 Hz); 60 Hz
  only during an active drag; avoid whole-window repaints; GUI CPU ≤ ~3 % on
  the reference laptop — SPEC §9's 25 % envelope must hold with the GUI open.
- **Paced notification streams**: the ambient ceiling binds *every* repaint source,
  not just telemetry. A snapshot morph re-sets all 57 continuous parameters and
  rewrites its changed cells 30 times a second, i.e. twice the ceiling. Both the
  parameter fan-out (`CQGui.paramFlushPeriod`) and the cable layer's matrix-driven
  repaint are therefore paced to 15 Hz with a leading edge: the first change after a
  quiet moment lands at once, so a knob turn, a 16n fader, a code `set`, a plant and
  a rip stay instant, and only a continuous stream is thinned. Pacing may drop
  intermediate frames but never the last value.
- **Charge lamps repaint one at a time, on change only** (landed 2026-07-21): a
  `/cq_charge` block refreshes just those lamp views whose own charge moved by
  ≥ 0.02. An unpatched gate sums to exactly zero forever and costs nothing at
  all; a steadily held gate is steady ink. This is §16.6's stillness law applied
  to the panel, and it is what keeps nine 15 Hz channels inside the budget.
- **Cable pulse density limit**: a planted cable costs about eleven language-to-Qt
  Pen calls, so the cable layer's ambient repaint is O(cables). Past
  `CQGuiCableOverlay.auraCableLimit` (48) planted cables the cables stop pulsing
  and keep the static reviewed ink of §16.8's no-telemetry case, while the 16
  source jacks go on pulsing; below the limit nothing changes. A pulse that cannot
  be attributed to one cable is not information, and a late pulse is worse than
  none. `\mayhem` patches all 352 cells and is the case this exists for
  (measured 2026-07-21: 164 ms per repaint at 352 cables, 1.4 ms for all 16 jacks).
- **The cable layer's offscreen cache** (landed at G3; measured 2026-07-22
  inside the real Qt paint event: full ink costs 2.1 ms at 12 cables and
  82.7 ms at 352 on the dev machine, roughly half the reference laptop's
  figures — and a blit is ~4.5 ms flat at any cable count): the planted
  pass renders into an `Image` and every repaint the planted ink did not
  ask for — the 60 Hz hand cable and lasso, a resize, the hard cut's
  full expose — pays one blit. The image re-renders only when its pixels
  can actually have moved: the patch set, the hover/solo/harvest marks, an
  aura frame. Pixel-identical by construction (the image is drawn by the
  same Pen code), and a platform without a working Image API falls back to
  direct drawing loudly, never to silently absent ink. Two things were
  measured on the way (drawCount counters, 2026-07-22), both correcting
  older guesses: **a sibling view's repaint does not damage the overlay at
  all** in the StackLayout stage (ten castle or compass repaints = zero
  overlay draws), so the cache's real customers are the gesture layers and
  whole-window damage; and **the render must never run inside a paint
  event** — QtCollider's Pen is one global painter, and opening the image's
  painter while a drawFunc's own is active deadlocks the whole GUI thread
  (the same family as the queued-view-swap law). Renders happen on the
  AppClock before the refresh they prepare; a paint that finds its image
  stale draws direct at today's price and schedules the render for the
  next damage.
- **Overlay hit-testing**: the cable layer must not steal knob/button
  interactions; only jacks/sockets/cables are targets (≥ 12 px, magnetic
  candidate halo). The one exception is the harvest lasso's right-drag
  (§4.4), which may claim only empty ground.
- **Focus law**: widgets are non-focusable; text entry is click-focus only
  and releases on Enter. Tab toggles Night water focus only when neither text
  entry nor the Drawer owns the interaction; Esc and Ctrl+Z remain
  window-level.
- **Headless**: boot never constructs the GUI (existing flag); all GUI
  classes must build serverless (the smoke-script law).
- **Spellbook hooks the matrix notification stream**, not the gesture layer —
  that is how code-layer writes appear in it for free.
- **Style**: governed by §16 (the visual language — normative for look and
  motion): single palette source, seeded wobble, the ink kit, the state table.

---

## 12. Safety appendix (nothing weakened, restated for the new surfaces)

- All SPEC §8 invariants untouched: master chain, coco clip-before-quantize,
  silent boot, clamp funnels, mic policy, NRT caps, recording files.
- **`space` = panic works in every view, always.** The master strip (with
  PANIC and the notice line) is persistent and may never be covered — not by
  the drawer, not by the night map, not by anything.
- **Audition can never leak**: the revert-path list in §4.1 is normative and
  the smoke test asserts it, including the panic and CmdPeriod paths.
- Weather tools and compass write **weights only**; `micGain` has exactly **one**
  interactive GUI path with its warning color — since N8 that path is the
  Workshop ledger row, *moved* from Day and never duplicated there (§8), while
  the night's mic sprout stays read-only; relocating snapshot controls to the
  drawer changes zero snapshot policy (SPEC §7).
- Undo is a funnel client like everything else — it can never bypass clamps.

---

## 13. Phases, tests, gates

Standard project law applies per phase: full test battery after every
`classes/` change, Ctrl+Shift+L reminder before gates, commit per approved
gate, manual_checklist blocks in the established copy-pasteable format.

- **G1 — the day skin**: skin sockets + cable overlay + full banana grammar
  (§4) + audition + spellbook + inspector demotion + `/cq_aura`.
  Tests: the usual four markers; smoke-script extension — serverless GUI
  build, sweep, and **audition-leak assertions** (simulate every §4.1 exit
  path, assert matrix equality); unit test for the batched write (clamps,
  single notification). Listening gate: plant `tri3 → coco1_spaf` *by ear*
  (the swell audible before commit); move a banana; rip by yank; wheel-trim;
  lasso-harvest a few and Ctrl+Z the whole catch back; spellbook lines match
  the actions.
- **G2 — the night map**: archipelago bodies + spiral read-head (+ phase
  telemetry) + charge lamps (`/cq_charge`) + solo-light + the almanac
  corner's sowing moon (presets were struck from this scope on 2026-07-22 —
  §16.6) + deck-bird file drop. Listening gate: patch a flip banana while
  watching its lamp charge and fire through the Schmitt; confirm the ring
  river's direction matches the `\classic` FM ring audibly; and sow a thin
  crescent against a full moon — the difference must be a difference in
  *how much*, not in volume.
- **G3 — the weather**: compass + persistence file + grow/wither + breed +
  elastic right-drag + undo polish. Listening gate: stamp two jungles in
  opposite corners and crossfade ecosystems with one finger; right-drag
  wobble snaps back cleanly; breed offspring differs but rhymes.
- **N8 — the Workshop** (§8): the fourth Drawer page and the ledger row.
  Tests: a **dynamic coverage gate** proving the Night 31 / Master 5 /
  Workshop 43 interactive sets disjoint and their union equal to
  `CQSpec.names` — the three inventories are declared independently and none
  is a complement of the others; ownership tests for the four-tab order, page
  persistence and the single `micGain` path; binding tests for num, bipolar
  num, int, toggle and enum rows writing clamped through `CQState` and
  following an external write without echoing; and the Drawer Qt smoke opening
  Night → Drawer → Workshop at 1280×720 and again at 1920×1005 in both skins,
  asserting the centred 1128 px rail, the module and ledger widths, the last
  slot/send rows contained, Master visible and `CQInkControl.drawErrors == 0`.
  The coverage test may not be satisfied by a test-only array, hidden or
  disabled controls, clipped rows or a computed complement: the rendered
  controls and the audited manifest must be the same data.

---

## 14. Non-goals

- No DAW-isms: no automation lanes, no MIDI-learn UI, no theming engine.
- No photorealistic wood; this is ink, not veneer.
- No parameter-knob undo (matrix ops only). No second window.
- No touchscreen/tablet assumptions — hover is load-bearing by design.
- No changes to the 16n or code layers beyond the spellbook bridge.
- Space (`\cqSpace`) controls stay minimal pending the post-v1.0 space-concept
  decision (`docs/history/PLAN_v1.0.md`, vaihe 7 kirjaukset) — the night map gives space no body
  until it knows what space *is*.

---

## 15. Open seams (measure before polishing)

- **Weight-write zipper**: matrix weights are unlagged control buses read at
  audio rate; mouse-rate stepping is probably fine (a hardware banana contact
  is a step too). If audible on sustained sources, thin GUI writes to ~30 Hz
  or add a small client-side glide. Do not add server-side lag without
  checking the norns diet note (`docs/history/PLAN_v1.0.md` post-v1.0: one Lag per dest is the
  ceiling).
- **Overlay performance** on the reference laptop: if the full-window cable
  layer runs hot, split static and animated layers — implementers' craft.
- **Night-map coordinates**: tune by eye; keep the topology truths (ring
  order and direction, cocos left/right, roots below, birds above).
- **Message merging** (`/cq_aura` + `/cq_charge` as one packet) — free choice.

---

## 16. Visual language — ink & phosphor (normative for look and motion)

The software must not be a visually cheap digital picture of the wooden box.
We draw no wood, no screws, no photographed jacks, no panel replica. The
archipelago is the same animal in a new body: **a field notebook by day, a
bioluminescent bay by night.** Everything in this section decomposes into
vector primitives — strokes, fills, arcs, béziers, radial/axial gradients,
dash patterns, alpha, transforms, text — and nothing else. No image files, no
icon fonts, no external assets, ever.

### 16.1 The two skins

- **Day = ink on paper.** Warm cream ground, warm near-black ink, family
  pigments. A calm tool. The paper is *flat color* — no fake grain, no
  texture noise (simulated paper reads cheap); warmth comes from hue.
- **Night = phosphor on deep water.** Blue-black ground, pale bone linework,
  the family colors turned luminous. **Every light is information** — nothing
  glows, twinkles or drifts decoratively. No starfields.
- The master strip keeps one identical layout in both skins (muscle memory),
  only re-inked: paper band by day, dark slate by night; PANIC is vermillion
  in both.
- The Day/Night toolbar destination is a **hard cut**, not a transition.
  Turning your head, not a cutscene.

Rough night geography (day keeps the v1.0 columns):

```
        bird A                          bird B
   coco1 spiral      ring of five       coco2 spiral
   (+moons)            castles          (+moons)
        roots envA ──┘        └── roots envB
   [ master strip ──────────────────────── PANIC ]
```

### 16.2 Palette

Single source of truth in code: the existing CQGuiWidgets color table,
extended — every color anywhere in the GUI is looked up there, none are
inlined at draw sites.

Grounds & neutrals:

| name | day | night |
|---|---|---|
| ground | paper `#F4EDE0` | sky `#12141C` |
| ground recess (strip, drawer) | `#E7DECB` | slate `#1A1D26` |
| line | ink `#2A251E` | bone-ink `#A9AEB8` |
| whisper (labels, idle values) | `#62594D` | `#7A808C` |
| spark / highlight | (use ink) | bone `#EDE6D2` |

Families (jack dots, socket rings, knob arcs) — pigment by day, phosphor by
night:

| family | pigment | phosphor |
|---|---|---|
| triangle (tri) | `#D98324` | `#FFB13D` |
| castle out (cas) | `#797267` | `#C1BAAD` |
| fm | `#3D6FA5` | `#64A7FF` |
| inputs / spesal | `#4F8A50` | `#62D06A` |
| spaf | `#7C58A8` | `#B287FF` |
| organ | `#D4AF37` | `#FFDE59` |
| preamp | `#FBF7ED` (ink-outlined) | `#F2EFE4` |

Signals:

| signal | day | night |
|---|---|---|
| verso cable | `#E39A2D` | `#FFAE42` |
| inverso cable | `#4A7CC7` | `#62A9FF` |
| danger (hold, rec, panic, mic) | `#C43D2B` | `#FF6B54` |
| warning text (watchdog, notices) | `#A87B2F` | `#FFD98A` |
| lamp fill (charge lamps) | `#B57A16` | `#FFE9A8` |

Laws:

- **On paper a lit lamp is more ink, not more light** (amended 2026-07-21 after
  the G2.5 review): the first day lamp fill was a pale amber that measured
  1.2:1 against cream and disappeared. Cream ground admits no glow, so the day
  lamp is a deep amber bead at 3.15:1; the night lamp stays luminous.
- **No meaning by hue alone.** Every meaning carries a second channel:
  inverso cables are barbed, gates have lamps, commitment is seatedness,
  families have homes and glyphs. (This also resolves the verso≈tri and
  inverso≈fm hue kinships — heritage from the v1.0 grid, kept on purpose.)
- Contrast: informational text ≥ 4.5:1 against its ground, large marks
  ≥ 3:1. Tune the exact values by eye, but never below these.
- **Squint test**: from two meters you must still read — what is patched and
  roughly how hard, what is hot, and the hold / panic / rec states.

### 16.3 Line

- Stroke widths in px, never scaled with the window: **1** hairline (dials,
  ticks, rails) · **1.5** body (organisms, sockets, knobs) · **2** emphasis
  (cable cores, active states) · **3** heavy (panic rule, hold ring — rare).
- Round caps and joins everywhere. No miters.
- **The wobble**: organism paths get hand-drawn irregularity — control-point
  jitter ≈ 0.8 px, roughly one perturbation per 40 px of path, from one
  session seed (§16.11). Redraws must not shimmer: it is *a* drawing, not a
  boiling one. Widgets (knobs, sliders, meters, drawer) are drawn **clean** —
  tools are steady, creatures are alive.
- **Asymmetry law**: organism geometry is never perfect — pentagon angles
  ±6°, ring radii ±5 %, castle sizes ±8 % (all seeded). Perfect symmetry
  smells of CAD; the original was cut by hand.
- **One shadow in the whole app**: the open drawer's soft edge. Nothing else
  casts shadows; depth elsewhere is alpha and seatedness.

### 16.4 Type

- Two faces only, via generic constructors (code-only, platform-safe):
  `Font.sansSerif` for labels, `Font.monospace` for numbers, the spellbook
  and filenames.
- Sizes: **10 px labels and values, 11 px spellbook/notice line, 9 px dense-map
  annotations**. These are the laptop-legibility values approved after the first
  real Day-skin review; do not shrink them back to the earlier 9/10/8 sketch.
  Lowercase everywhere; only PANIC, REC and HOLD may shout.
- **The Drawer is the one exception, and it goes up, never down** (added
  2026-07-28). Day and Night are read as *shapes* — a knob's arc, a castle's
  ripple — with text as annotation, and their px sizes are fixed by §16.10. The
  workbench is read as **prose and numbers**, so the same 10 px that works
  beside a knob is too small for forty-three rows of exact values. Its two
  reviewed steps (§8, `CQGuiScale`) therefore start above the Day floor and step
  up again when the window has the room. Every Drawer size is still a whole
  number of px chosen at a reviewed step — never a scale factor applied to a
  base size, which is how type turns to mush.
- **The persistent chrome shares that floor but not those steps** (added
  2026-07-28 at the user's request: beside a 12 px workbench the toolbar and
  Master strip read as an oversight). The toolbar title, the Day/Night and
  Drawer destinations, the Drawer's page tabs, and the Master strip's knob
  labels, notice line and elapsed time all take **one chrome size**
  (`CQGuiScale.chrome`), matching the Drawer's compact step. It does not follow
  the window: the strip's height is fixed by §12's permanent-visibility rule and
  its layout is muscle memory in both skins, so only the ink inside it grew —
  the strip is still 118 px and REC/PANIC keep their §16.7 hit areas. Buttons in
  the chrome and the Drawer are **measured from their own captions**, so growing
  the type can never crop one.
- **A nominal font size is not a view height.** Every fixed-height text band
  must reserve explicit ascent/descent clearance above and below the face
  (`CQGuiScale.textBandHeight` in the Drawer). After any type-rank or font-size
  change, the visual and Qt-smoke review checks both horizontal fit and that
  glyph tops/descenders are not clipped. A smaller side note may use a smaller
  face; it may not use a guessed face-size-plus-a-few-pixels box. If native
  label metrics still crop a short, read-only annotation, draw it with Pen in
  an explicit band and gate the measured glyph bounds against that band.
- Tool-box titles keep a small reviewed top inset between the border and the
  glyphs. Memory's `saved snapshots`, `patch fingerprint` and `morph bridge`
  use the same step-aware inset; none sits directly against the top stroke.
- Labels are whispers: whisper color, close to their object, never boxed.
  The project's own words — spaf, spesal, verso, balcium — are decoration
  enough.
- The night map carries almost no text: filenames under the birds are its only
  resting labels. **Numbers never appear at night** — weights are read as depth
  and width; precision lives in the inspector. The range ripples deliberately
  carry their three regimes by form alone; the old `i/b/a` vocabulary was
  removed at the user's N4 review.

### 16.5 Glyphs — drawn, never typed

All glyphs are Pen paths in a nominal 8×8 box. No unicode symbols, no icon
fonts (their rendering varies across platforms; paths don't).

| glyph | marks | recipe |
|---|---|---|
| open wedge | inVca / fbVca sockets | open triangle pointing into the socket |
| opposed chevrons | flip | mirrored ‹ › pair |
| spark | skip; fired lamps | 4-point star: two crossed strokes, long/short arms |
| wave | castle fm | one sine cycle from two béziers |
| rate ripple | castle range | a short common keel plus one long half-wave (`infra`), two half-waves / one full cycle (`balcium`), or five tight half-waves (`audio`), in a nominal 14×8 px box. A 3 px ground under-stroke follows the vector paths exactly where the river crosses; never a rectangular knockout |
| oyster / eyelid | coco in/fb Dolby | paired short arcs: relaxed apart = `off`; hairline gap = `gate`; closed plus a short stroke entering from the partner channel = `duck`. Posture carries the state before hue; the mark never fakes gate telemetry |
| tick-ring | spesal dial | 8 radial ticks, current one lit |
| pendulum | deck rate | small wedge + off-vertical line |

### 16.6 Bodies (night-map anatomy, in primitives)

Positions on bodies are given as clock angles — more precise than pictures.

**Castle (×5).** Body = closed blob: 6 anchors on a 46×40 px ellipse with
seeded ±8 % radial jitter, cubic curve through them; 1.5 px stroke, hollow
fill. Aura = radial gradient inside the body (phosphor → transparent),
intensity from `/cq_aura`. At 10–11 o'clock, a crown of three 4 px square
teeth — this *is* the cas source (gray dot at its base). At 1–2 o'clock, an
8 px triangular leaf on a 6 px stem — the tri source (orange dot). fm moon =
blue ring Ø 12 orbiting at 4 o'clock, r ≈ 28; spesal moon = green ring +
lamp at 8 o'clock, r ≈ 40 (moved out from 34 on 2026-07-21: the lamped view
is 34 px wide and its detail/hit box must clear the inner clock). Spesal dial =
the castle's **inner clock** (amended 2026-07-21 after the G2.3 review): a
whisper face at r ≈ 10 *inside* the body — where no endpoint mark can
ever cover it — with 8 ticks and the current state as a lit spark bead with a
halo; a static hover brightens the face so it reads as pressable (§4.5).
The **flesh around the clock is the body's own §6 solo target**, and hovering
it lights the quiet outer ring §4.5 gives every pressable control (added
2026-07-21: a double-click affordance the hand could not otherwise guess).
The two cues are exclusive — clock or body, never both. Cue and action share
one geometry: the drawn blob grown by `bodyMargin` (10 px, so the body answers
the approaching hand), and the echo is stroked exactly on that boundary. A cue
that covers more or less ground than its action is a cue that lies.
At 6 o'clock, a **rate ripple** is the playable range switch. Its form is the
regime: one long shallow half-wave = `infra`, two half-waves / one full cycle =
`balcium`, five tight half-waves = `audio`. Tap cycles in CQSpec's enum order;
**this captionless keel/ripple v2 was accepted 2026-07-27 (checklist N4).**
static hover brightens the ripple to line ink. **No `i/b/a` caption remains:**
the user judged the shape sufficient and the letters unnecessary inherited
Cocoquantus vocabulary at the N4 review. A short common horizontal keel makes
the regime family distinct from the fm moon's free single-cycle wave. Where a
ripple crosses the river, a 3 px ground under-stroke follows only the keel and
wave paths before the visible hairline is drawn; it is a local vector
separation, never the rectangular paper erasure §9 removed. Each anchor sits
13 px below its seeded blob's actual lowest point, so irregular castles breathe
equally. The ripple, inner clock and body have exclusive cues and press priority
in that order after the cable layer: a cable under the pointer still wins. The
14×8 px drawn mark, its fixed-pixel touch rectangle and all endpoint/clock
clearances are pinned at both reference sizes by `CQNightGeometryTest`.

**Layer amendment, 2026-07-27.** Castle paper and its four endpoint-detail
views are transparent. The blob first fills only its own irregular flesh with
Night ground, then draws aura and outline; therefore the ring and coupling
chords can run beneath each castle centre and emerge at the true body edge.
No rectangular blank area may erase castle ink, and no synthetic “river
stub” may protrude from a castle to repair such an erasure.

**The ring.** Irregular circle R ≈ 150 px through the five castles, 1 px
hairline. Flow k → k+1 shown by two **firefly dots** per arc segment
(Ø 2 px bone, ≈ 2 s per segment) — permitted ambient motion, because it
depicts the always-running LocalIn loop: it *is* information. **Stillness law
(decided 2026-07-21):** the fireflies advance only on telemetry frames that
already repaint — *the river flows while the island sounds; a silent
instrument keeps still water.* On the wood, lights move with signal, not with
the wall clock; this is the permanent behavior, not a budget stopgap, and it
stays even after the post-G2 offscreen cable cache. At the ring's center sits
its **eye**, the §6.1 handle for qFreq/qChaos, drawn as the **storm iris**
(amended 2026-07-21): the pupil's aperture is qFreq (Ø ≈ 5..14) and twelve
rays scatter and reach with qChaos — an even, short corona is calm weather, a
ragged reach is storm. The ink is the actual state, never decoration. The eye
lives on its own opaque layer so a storm repaint never re-inks the ring
(§10); it brightens while held and never displays a value.

**The spesal coupling chords** (added 2026-07-22 — the night's answer to
"which castle is this one attached to?"). The inner clock shows a castle's
*state*; the chord shows what that state **means**: which of the other four
it reads. Drawn on the ring's own layer in spesal green, carrying the same fact and the
same direction as the Day constellation (§16.7) so one state can never mean
two couplings in two skins — **the chord runs the way the signal runs**, from
the castle being read into the one reading it — but told in the night's own
language:

**Direction is a travelling light, never an arrowhead.** A dot with a short
tail crosses the chord, exactly as the fireflies cross the river two hundred
pixels away and for exactly the same reason: the sampling never stops. An
arrowhead here was the only mark in the bay that pointed instead of moved,
and it read as a flowchart. The light rides the river's own phase — the
telemetry frames, never a clock of its own (§16.9) — so a silent instrument
keeps still water on the chords too.

**Track-and-hold is a broken chord**, sample-and-hold a solid one: a
track-and-hold follows its input while the clock is high and freezes while it
is low, so a link connected half the time is the mechanism itself. No S/T
letters — the night carries almost no text (§16.4) — and no barbs, because
those already mean inverso polarity on the cables (§16.8).

**Shown only where the hand is asking**: nothing at rest, one chord under a
hovered castle, all five under the hovered river. This is a deliberate
refusal of the obvious version — five permanent chords through the ring's
interior — because the coupling is **structure, not a quantity**. It changes
only when spesal steps; it is read at the moment a hand is on a castle
deciding whether to step it; and that interior is where the storm iris, the
fireflies, the cables and the harvest lasso all live. A mark read two
percent of the time has not earned permanent ink, and the whole-network
overview already exists one hard cut away in Day's constellation. Stepping
the clock swings the chord under the same pointer, so the act shows its own
consequence.

Geometry: the chord bows toward the eye by 0.4 of the way from its own
midpoint, so an *adjacent* pair reads as an inner chord instead of doubling
the river's line — clamped at 0.35 × ringR so it can never crowd the iris.
Both ends stop where the castle's own opaque box begins (§16.6's law about
lines laid under opaque layers), or the arrowhead would hide beneath the
very body it points at.

**Coco (×2).** Spiral = Archimedean, 2.5 turns, outer R ≈ 85 px, 1.5 px
stroke; the mouth at the outer end carries the gray coco-voice source dot;
the direction chevron (a §6.1 touch: press = flip) hangs ≈ 24 px below the
mouth, centered on its anchor — clear of the mouth jack's opaque view and the
shell box edge at every scale (down = forward, up = flipped; amended
2026-07-21 after the G2.3 review found the outward home clipped).
**The chevron has three states, not two** (amended 2026-07-21 after the G2.5
review): forward, flipped, and — when the flip gate is driven faster than an
arrow can be read — §16.5's **opposed chevrons**, meaning *shuttling*. The
FLIP param decides on its own when it is on (instant and exact); otherwise the
state comes from `/cq_phase`'s audio-rate direction mean, settled past ±0.5.
This is the general law for any mark whose value outruns the eye: **show the
regime, not the value.** A frozen arrow and a guessed arrow are both lies.
**Read-head spark** = Ø 4 px bone **comet** at the loop's true address
(`/cq_phase`, §9 — 5 Hz tick; not drawn until the server has reported — no
telemetry is not zero): a dot with a short tail trailing where it came from, the
tail's side read from the phase's actual travel (wrap-aware), so a
matrix-driven flip shows truthfully — and the **mouth chevron reads that same
travel OR-ed with the FLIP param**, exactly as `\cqCoco` takes
`max(param, matrix gate)` (amended 2026-07-21: the chevron used to read the
param alone, so a patched flip banana turned the loop and the comet while the
chevron kept pointing the old way; §16.6 binds the two to one direction).
The comet is **display only** (2026-07-22, G2 gate — see §6.1): it is the only
mark in the night that travels, and a moving target is not a control.
It draws over the reel notch and carries the night's bone-spark ink, which now
belongs to **events** alone — the read head, fired lamps — while settings are
drawn in line ink. The comet has a third state of its own, by the same regime
law as the chevron: while the `cocoN_skip` charge is still lit on two
consecutive `/cq_phase` frames — i.e. the loop is being restarted faster than
5 Hz can show — the head is **struck**, and wears the §16.5 spark glyph
instead of its tail. A bead crawling near zero would otherwise read as "almost
stopped", the exact opposite of the truth, and a direction means nothing under
hammering. The condition itself needs no new telemetry and no client-side
timer: the server's 0.15 s peak-hold on the charge already says it.
**The struck mark is a state, never an extent** (2026-07-22, second attempt):
the first version drew the reach the head manages out of the eye, and that
reach collapses to sub-pixel exactly when the regime is clearest — a mark that
disappears when it is most needed. Any regime display owes this: measure the
*condition*, not a quantity the condition destroys.

The **trigger rate is drawn as weight, never as motion** (2026-07-22, third
attempt — the static mark read as *calm* while the loop was being hammered
hundreds of times a second). This is the general law for any rate on this
surface: above ~10 Hz the eye fuses a flicker into a steady light — a real
panel LED does exactly the same — and this map only receives 5 Hz, so the
fastest blink it could draw would be **slower than the truth**, at a rate the
GUI invented. That is decoration (§16.12) and a lie at once. A tachometer, not
a strobe: the spark's arms grow 4 → 10 px and a second diagonal pair opens
past the midpoint, alpha 0.55 → 0.9, on a **logarithmic** 8 Hz → 2 kHz scale
(a rate is a pitch to the ear, so it is an octave axis to the eye), quantized
to the eight steps the drawing can distinguish so a drifting rate cannot
repaint the shell for a difference nobody can see. The rate rides `/cq_phase`
as a third value rather than opening a fourth stream (§15 leaves message
merging free; §10 counts messages and Pen calls, not floats).

The struck mark **twinkles**, alternating on every telemetry frame — §16.9's
whitelisted read-head spark, ambient at the telemetry rate: the arms shorten
to 74 % and dim to 78 %, and past the rate's midpoint the two pairs trade
emphasis so the star scintillates rather than merely pulses. This says *the
state is live*, which weight alone cannot — a heavy static star and a heavy
frozen one look identical. It is driven **by the frames, never by a clock of
its own**, so a stalled server freezes the mark instead of animating a claim
about liveness it can no longer make: §9's "no telemetry is not zero", in
motion. It is not the trigger rate and never claims to be. Flip reverses comet
and chevron.
**Hold**: the outermost turn
closes into a full circle and tints danger-red at 60 % — the loop is sealed;
the **heart** at the spiral's innermost eye (small Ø ≈ 5 mark, red while
sealed) is the §6.1 hold touch — a left tap toggles the seal, and a **right
press held on it punches**: the seal inverts for as long as it is held and is
restored on release and on every exit path (§6.1's punch row), so the same
crack/close the display already draws animates the momentary hold for free. The **skip mark** hangs under the mouth
chevron: the §16.5 spark glyph in a 9 px box, whisper at rest, line ink on
hover, a 120 ms bone-spark flash on any skip (§16.7's momentary law, one
`CQInk.momentaryFlash` constant shared with the Day button). The spiral's line
is the §6.1 winding surface; the **reel notch** (amended 2026-07-22) rides it
at the speed's normalized position — inner eye = slowest, mouth = fastest — as
the winding gesture's live hand and the resting speed display. It is a **tick
drawn across the groove** in line ink, ±4 px, ±6 px and heavier on hover: a
speed is a setting on a scale, so it wears a scale mark, and the only
travelling body on the shell keeps the bead. It was a bone bead until the G2
gate, 0.2 px larger than the comet and the same hue — which is no difference a
hand can use. §16.2's "no meaning by hue alone" is satisfied by **form first**;
where hue can help too, let it. On spiral hover the line
brightens and the notch lengthens (static ink, §4.5). Org fruit =
yellow Ø 7 dot on a 6 px stem from the second turn. Moons spaf / inVca /
fbVca / flip / skip sit on an arc under the spiral, glyphs per §16.5, lamps
on flip/skip; below the three continuous moons hang their **tide crescents**
(§6.1): open arcs ~Ø 18, luminous fill = value, family-colored, no numbers.
The in/fb crescent owners grow only downward and carry a **Dolby oyster**
24 px below the unchanged crescent centre. The oyster is consulted first on
press; its tap box and the crescent's original drag square are disjoint.
`off / gate / duck` are readable as open / nearly shut / partner-pressed shut,
and the duck arrival stroke enters from the other channel's direction. Spaf
and deck crescents carry no oyster. Cable-first priority remains absolute.
Because these crescents are native controls, the shared controller must publish
the crossing cable's hover *after* the oyster/crescent cue yields; clearing both
cues is forbidden. The same cable must then receive the press.
The yellow org fruit jack keeps its opaque 30×30 owner, but that owner repaints
the shell's exact curved spiral segment and radial stem beneath the jack. A
generic vertical stub is not the same path and may not replace it.
The spiral's stroke alpha breathes 0.55 → 1.0 with the coco's out level.

**Roots (inputs; accepted 2026-07-28, checklist N7).** Each side is the whole
intake body, not an envelope on a stick:

- Five unlabelled source strands grow as a wide, seeded-irregular delta from
  the lower waterline into one
  preamp knot. Their order comes from that side's `preASrc` / `preBSrc`
  `CQSpec` enum with `none` omitted. The selected strand remains subtly
  stronger at rest. Hovering a strand brightens that candidate and shows one
  transient 10 px mono provenance (`live l`, `live r`, `mic`, `deck a`,
  `deck b`); left-click selects it, and clicking the selected strand again
  writes `none`. Hovering or holding the knot reveals the current selection;
  `none` shows its name but lights no strand. There are no permanent labels.
  The transient name sits inward beside the knot in bone ink over a small
  ground wash, so a highlighted green strand cannot swallow its text. That
  wash erases whatever it covers, so it may never reach the delta: the label
  keeps a measured clearance from the strands' own curves and gives way
  outward wherever the seeded delta needs the room. **A constant inset cannot
  promise clearance from a seeded body** — the geometry has to measure it
  (the accepted N7 review found the erasure cutting the tops off root B's two
  leftmost strands at 1280×570).
- The two-lobed knot is an axis-locked drive/bias body. The first 8 px choose
  one act for the whole drag: vertical up/down traverses normalized
  `preADrive` / `preBDrive`, while horizontal right/left traverses normalized
  `preABias` / `preBBias`. Both use 240 px for the full range and map only
  afterwards through their CQSpec curves. Drive deepens the living centre;
  bias swells the outgoing lobe and pinches the intake lobe, so zero is
  symmetric and increasing asymmetry is shape-first. A tap changes neither.
  Non-left presses are inert. Cable/Alt priority remains absolute.
- A preamp-colored audio vein leaves the knot, bends outside the
  Coco's tide crescents and terminates on an actual outer turn of the
  corresponding spiral. It is anatomy, never a cable or hit target. The
  Cocoon's opaque view redraws the exact final cubic under its own spiral;
  no rectangular layer edge may cut the vein. Drive gives it quantized
  1.5 / 2 / 3 px pressure, so the default rests below cable weight and a hard
  preamp visibly thickens. Its alpha combines the corresponding
  `cocoNSlotPre` opening with the existing post-preamp env aura, in a local
  transparent root layer.
- The canonical green `envA` / `envB` jack is a smaller outward side-growth
  from that same post-preamp vein. It remains the only patchable endpoint;
  the inventory is still exactly 16×22. The side-growth is one tangent-led
  cubic all the way to the dot; the endpoint view uses transparent paper, so
  no straight repair stub or rectangular seam interrupts it.

Beside side A grows the existing small **mic sprout** — display only, never a
jack: the mic is not one of the 16 sources but an optional input feeding the
preamps, so nothing on the sprout can be grabbed or patched. Three states:
not drawn at all when no mic is configured; a dormant whisper-colored bud
when configured (`micChan` set) with `micGain` = 0; bud and tip ring danger-red
while `micChan` is set **and** `micGain` > 0. The day panel's mic control keeps
its own warning-color law unchanged.

**Birds (decks).** Three strokes each — two wing béziers + a body dot,
span ≈ 30 px; filename in 10 px mono whisper beneath, in a line box of at
least 200 × 20 px (fixed 2026-07-21: 14 px clipped the ascenders and
descenders, and a 160 px box ate both ends of a long name — §16.4 fixes the
type size, so the box gives way first). A name too long for even that box is
trimmed **from its tail only** and marked with an ellipsis: it is centered, so
an untrimmed overflow would lose the head and the extension both; the rate socket under
the wing, and beside it the deck-level tide crescent (§6.1). Dropping an
audio file on the bird loads its deck (§6.1). The same body now carries three
captionless hand acts: tap (< 8 px) opens `Dialog.openPanel` at the configured
samples folder; wheel sign walks the loaded folder with a 150 ms momentum gate;
one ≥ 6 px horizontal out-and-back ruffles to random; repeated or broad
ruffling remains valid once that reversal is clear. While held, the bird owns
the Qt drag and a 60 Hz global-cursor fallback preserves motion samples if the
pointer leaves its small UserView on Windows.
A different drag is inert. Cable
hit-testing remains first inside the bird view, while the rate socket and level
crescent keep their higher sibling views and own geometry. Any successful
`\file` notification — dialog, wheel, ruffle, drop or code — flashes that bird
for the shared 120 ms momentary interval and updates the one filename truth.
Flutter = 1 px wing bob at ambient rate while the deck's level > 0.05
(client-side state, no polling). Ordinary hover statically brightens the bird;
file-drag hover adds the existing whole bird+name drop-target border (§16.7).
Ordinary bird hover also reveals a faint, hit-transparent flight from that deck
to its own Cocoon, weighted by `cocoNSlotDeck` and absent at zero. If either
preamp currently selects that deck, the same hover additionally reveals a
warmer flight to the true preamp knot. Conversely, hovering a deck source
strand or a deck-fed knot reveals that bird-to-root provenance. These paths
exist only under the hand: deck A/B may feed either preamp, so a permanent
bird-root line would falsely hard-code a relationship the DSP does not have.
All transient flights and their provenance names share one hit-transparent
topological overlay above every landscape body and below actual patch cables.
They therefore remain continuous across otherwise invisible Qt view
rectangles instead of being repaired separately inside each object.

**The moon and the tide field** (accepted for the next visual iteration with
the user 2026-07-27). The weather machine is integrated by *gesture and
relation*, not collected into a panel:

- The sowing moon is a solitary Ø ≈ 56 body in the far upper-right, beyond
  bird B and slightly above its flight line. It opens as a 0.22 crescent;
  sowing density is `0.2 × phase²`, so quarter/half/full average roughly
  4/18/70 cables rather than crowding the first half. Tap throws shake;
  horizontal axis-locked drag pours phase. Vertical down holds rain/grow and
  vertical up holds sun/wither. The dominant axis locks only after the shared
  8 px threshold; the next 32 px control weather tension (rain 0.6…1.6 Hz,
  sun 2.5…6 Hz) and visibly add drops/rays. Rain and sun are transient faces
  beside the moon and exist only while held — no static satellites remain.
- There is no pond, shore or pad. Four inward-pointing memory seeds in the
  NW/NE/SW/SE winds imply a large bilinear tide field through the bay. At
  rest only those seeds are inked. During a voyage, a small cross and four
  sparse wavefront pulls (≤ 8 % alpha) reveal the relation; there is no
  ambient animation and no rectangular boundary.
- Filled seed tap recalls, Shift+tap stores/overwrites, and a latched Tab/gill
  focus lets a plain tap store into a vacant seed. A vacant unfocused tap is
  inert; right clears. Vacancy is an **open husk**, stored silence a closed
  dark dormant kernel, and a stored living patch a luminous veined kernel
  whose light follows its cable density. Dragging one filled seed onto another
  breeds. Its temporary curved relation is drawn on the stage's topmost
  hit-transparent layer; a valid filled target catches the thread at its
  centre and opens a blue double bloom. No graft glyph or armed state exists.
- Open-field left drag does nothing until water focus is open. Tab and the
  clickable gill below the lower castles latch the mode; Shift is its
  momentary form. In focus, cable spans go behind the water and out of
  hit-testing while endpoints and bodies keep priority; left commits and
  right is elastic. Outside focus, right remains harvest. A captured voyage
  continues across the organism until release.

The N3 landscape review deliberately left the two roots in their then-existing
form. N7 has since replaced them with the normative intake bodies in the
**Roots (inputs)** paragraph above: actual preamp→Coco audio anatomy plus its
envelope side-growth, never multiplied decorative roots.

<details>
<summary><b>Superseded almanac/pool experiments (2026-07-22—27)</b></summary>

**The almanac corner** (decided with the user 2026-07-22 — the night
toolbar's body; the earlier lowercase word-row is superseded). It lives on
the top-right sky, on the water the bodies leave free — beyond bird B's
name box and the shell's box at every scale; the geometry owns the exact
rects and the clearance tests pin them (tune by eye, §15). The farmer's
almanac holds exactly what this corner holds: the weather, and the sowing
calendar read from the moon. Nothing else — the recipes are not almanac
material, and they left with the sower constellation below.

> **Status — accepted 2026-07-27 (checklist N3).** Everything from here to the
> end of the weather paragraph describes the accepted §9 v3.2 rebuild on the
> `nocturne-nightscape` branch. It supersedes the 2026-07-22 almanac corner;
> `nocturne` itself still carries the older surface until the accepted branch
> is explicitly merged. One later polish debt does not reopen this gate: the
> Tab-equivalent gill must gain the narrow cable-priority/discoverability
> exception specified in §7.1.

**There is no almanac corner** (proposed 2026-07-27). Keeping the three parts
together in one patch of right-hand sky was tried twice — first as a column,
then re-composed to break the axis — and neither fixed what was wrong, because
the fault was not the arrangement. They were a **cluster of unlike things**
(a shake control, a matrix blend, three matrix ops) grouped by FUNCTION and
touching no body around them, which is what a toolbar is. Everything else at
night is grouped by body: this castle's controls are on this castle.

So the parts were separated and each sent where it **acts**:

- **The moon goes to the sky**, at the birds' own altitude, between castle 1's
  box and bird B's name — the thing the birds fly under.
- **The rain and the sun keep it company there.** They grow and wither the
  *live bay*, not the pool's memories, so they are whole-bay weather exactly as
  the moon's sowing is, and an almanac's sky is where weather belongs.
- **The pool goes to the water** at the bottom of the bay (below), **tangent to
  the stage's lower edge**. The roots already run past that edge, so it reads
  as the waterline, and nothing has to be drawn to say the plants and the pool
  share one water.
- **The graft stays on the pool**, because it takes its two parents as seed
  taps: a mark whose gesture ends on the seeds may not live a bay away from them.

No mark may line up with another mark, with a corner seed or with the moon on
either axis, and the clearance tests hold all of it at both reference stage
sizes.

**The sowing moon**: a bone-ink circle Ø ≈ 64 (44 in the first build, 52 at
the second review, 64 when §9 raised it into the sky), its lit phase =
`shakeAmount` — the terminator drawn as one arc, the lit fraction is the
value, no number (the tide crescents' law in the sky).

**The phase is how MUCH is sown, not how hard** (decided with the user
2026-07-22 after the first review round, and carried into SPEC §5.5 as
`shake`'s `density` argument). Mapping the lit area onto the weight ceiling
was both redundant — `patchDepth` is already the how-alive macro — and
inaudible: every throw rewrote the same ~35 cells, so a thin crescent read
as "everything went quiet" rather than "a little was sown". Density is
reachable from nowhere else, an area reads as a quantity, and an almanac's
moon has always said how much seed to throw. The map is linear, so the
**half moon is exactly the v1.0 handful** (10 % ≈ 35 bananas) and the full
moon is twice that (~70, dense but a fifth of `\mayhem`). Consequence worth
knowing: a full sowing passes `auraCableLimit`, so its cables keep §16.8's
static ink — a big throw costs the pulse, and that is honest.
**The new moon clears the bay** (amended 2026-07-22 after the second review
round): the phase *is* the field, and an empty field is empty, so a tap at
new moon pulls every cable. The first version made it a no-op instead, to
keep the darkest phase from being the most destructive act — but in the
hand that read as a broken control rather than a careful one, and a clear
is one Ctrl+Z away like any other throw. The spesal states are left alone:
a clear is a clear, not a scramble in disguise.

Tap (< 8 px travel, the lasso's own threshold) = shake at the current
phase; **horizontal** drag = pour the phase, at the tide crescents' own
rate. The moon fills from its left limb so the terminator travels *with* a
rightward drag — the hand pushes the light ahead of it, and cue and action
share one geometry (§4.5). Astronomically a waxing moon lights from the
right; this is an instrument, not an ephemeris, and the first version's
vertical drag lost that argument the moment it was tried by hand.
**The tap's own px are dead**: the pour starts counting past the threshold,
so a throw can never nudge the phase it is throwing and a pour cannot jump
the moment it engages — one body, two acts, told apart by the hand alone.
The moon flashes 120 ms bone on *any* shake from any layer (`p` key, Day
button, code) — §16.7's momentary law, one `CQInk.momentaryFlash` constant,
hung off one action notice so the three layers cannot drift. Hover
brightens the moon's own drawn rim.

**No presets at night.** A sower constellation of preset stars stood under
the moon for one review round (2026-07-22) and was removed the same day:
one star per recipe, hover-whispered names, tap to sow. It failed on its
own terms — six identical glyphs that had to be hovered one at a time to be
told apart, i.e. the night's only control that could not be used without
reading text (§16.4), which is a menu in costume. A preset is a recipe, not
a body; the night's grammar is bodies, bananas and weather, and the
Inspector's menu is one toolbar click away from either view. Nothing about
G3 depended on it: the compass stamps its corners from the live matrix.

**The tide pool** (drawn since G3, on its own opaque layer so a moon flash
never re-inks the water and a voyage never re-inks the moon). G3 drew the
§7.1 pad as a 140 px round-cornered hairline square — the app's one
widget-shaped body at night, and it always read as one. §9 made it a
**pool**: a seeded, wobbled closed shore (the castle blob's cousin, §16.3's
own organism language) lying in the bay's bottom water, **tangent to the
waterline**, with its own wobble kissing and crossing that edge so the water
reads as continuing under the frame. A tide pool is literally the memory of a
tide, which is what this control is.

It is an **ellipse** (≈ 200 × 118 at the minimum stage), and that is not
taste either. A Qt layer is a rectangle, so what the pool may occupy is the
largest free *rect*, not the largest free disc, and the bottom pocket is
bounded by coco 1's tide crescents above, the river's own layer to the right,
envA's jack to the left and the waterline below. An ellipse uses that pocket;
a disc would waste half of it. Measured honestly, the move to the water buys
about the area the G3 pad already had: it buys **belonging, not size**. Size
has one real lever left and it is the minimum window (§15).

The four **corner seeds** sit *on the shore at the diagonal winds*
(NW NE SW SE = bilinear TL TR BL BR) — a stamped memory is a seated line-ink
seed, empty water a hollow whisper ring, and the drawn mark is the tap
target (§4.5, cue and act one geometry). While a voyage is being poured, a
small line-ink cross rides the position and four faint gradients ≤ 8 % alpha
radiate from the shores to show the pull; at rest the water carries no
moving ink at all, so the layer costs a still bay zero repaints. No numbers,
no labels — the pool's geography is read by ear and by the bay.

**The tide's frame stays square on a round body.** The blend has four
corners whatever it is poured on (§7.1), so a stage point becomes a 0..1
position through the **elliptical grid mapping** between the disc and the
square. That mapping is chosen over a cheap radial squash for one reason
that is normative, not aesthetic: **the shore at a diagonal maps to exactly
a corner**, so a stamped memory can still be recalled bit-exact. Its inverse
has a square-root singularity at those corners, so the implementation snaps
edge values within 1e-6 — a deviation ~2.4e-13 of the radius, float dust and
never a hand. The pool also answers a little past its drawn shore (the same
idiom as a castle body's margin), and a press in that band holds at the
nearest place *on* the shore: that is what makes the pure corners reachable
by aiming past a seed rather than at a single pixel.

**The weather left the row, and did not stay together.** Three §16.5-style
drawn marks, each sent where it acts. The **rain** (three slanted strokes,
grow) and the **sun** (disc and four rays — in a farmer's almanac the sun is
the drought, wither) act on the live bay, so they are sky weather beside the
moon. The **graft** (two overlapping rings — two parents sharing flesh,
breed) acts on two stamped memories and takes its parents as seed taps, so it
stands off the pool's lower-right shore, clear of the drawn line rather than
on it. On the water a mark is consulted **before** the water it stands on
(§4.5's most-specific-mark-first rule, the same order a corner seed already
had) — which the row never needed and a shore does.
Whisper at rest, line ink under the hand or while held. Grow and wither
are hold-marks at §7.2's 2.5 Hz, and the mark flashes 120 ms bone on
**each seed that actually lands** — §16.7's momentary law at the weather's
own rate, so a held rain visibly rains and a hold that lands nothing (a
full or empty bay) stays honest. The graft arms with a halo, takes its two
parents as seed taps, and is forgotten by Esc, by a stray pad press, or by
pressing it again. One hold, one jump, one voyage, one breeding = one undo
op each; stamping and clearing corners touch no cable, so they are not ops
at all.

</details>

### 16.7 Widgets (day-panel language)

- **Knob**: three reviewed body sizes express breadth of consequence, never
  current value: standard Ø 26 for trims/mix/routing; featured Ø 34 for the
  two Coco identity controls `speed` / `spaf`; primary Ø 44 for `q freq`,
  `chaos` and master `vol`. All use 1.5 px ink, wedge pointer and a 2 px value
  arc from min-angle to current — whisper-colored, family-accented where the
  param has a family (the spaf knob wears a purple arc). Drag brightens pointer
  and arc to full ink. No 3D, no gloss.
- **Slider** (slots, sends): hairline rail, Ø 8 thumb, whisper value fill.
- **Ledger row** (added at N8 for the Workshop, §8; both skins from one
  primitive): a **24 px row owning one rectangular hit area** — label at left,
  one hairline value rail in the middle, the exact value in 10 px mono at right,
  and the value never overlaps the rail. Forty-three miniature Day knobs would
  be the cheap answer; a precision page needs a row that *states* its value.
  - A unipolar numeric fill grows from the rail's left end; a **bipolar** fill
    grows away from a visible centre tick, so zero is a place and not a guess.
    Where that tick sits comes from `CQSpec` (`centerNorm`), never from a bound
    compared at the draw site. Integer rails snap and print an integer.
  - A **toggle** is two stable `off · on` seats and an **enum** one labelled seat
    per value, in `CQSpec.enumValues` order. A seat is a bead plus its own
    caption, and the drawn cell is the pressed cell.
  - Press/drag maps the **rail's own x-coordinate** to normalized 0…1 and then
    calls `CQSpec.map`; the wheel is the existing normalized fine nudge, and a
    seated row has no continuum to nudge along. No literal range, step, default
    or enum lives in the control. A press outside the rail — on the label or the
    value column — is deliberately **inert**: a mis-aimed click on a ledger may
    not slam a parameter to the floor of its range.
  - The handle is a **tick drawn across the rail**, the shell reel-notch's own
    idiom (§16.6): a setting on a scale wears a scale mark. Hover brightens only
    that row's rail, handle and value; press adds the stronger outline. Cue and
    action share one geometry — the rail is published once, by the kit, and both
    the drawing and the hit test ask for it (§4.5). The whole row, including its
    grab band, seat beads and handle, is measured from the current size step, so
    it grows as one thing rather than as a bigger box holding the same small
    marks.
  - External state and snapshot morphs move handle and value through the
    existing paced updater stream (§10). That real change is the only motion.
- **Meters**: 1 px ink outline, whisper fill, 1 px peak tick; the GR meter
  fills downward in danger-red.
- **Buttons**: rounded rect (r 3), 1.5 px ink outline, paper fill; latched
  on = ink fill with paper text; momentary = 120 ms ink flash (the existing
  skip flash, generalized). Persistent safety/transport actions carry larger
  hit areas in the Master strip: REC 88×34 px, PANIC 112×34 px.
- **HOLD LED**: Ø 8; off = hollow ring; on = danger-red fill with a soft
  radial glow — the only glowing thing on paper.
- **3-state switches** (range, dolby): three tick positions on a short rail,
  ink dot at the current one.
- **Skin sockets & jacks**: exactly the night recipes, inked — same geometry
  everywhere, so the hand learns one shape.
- **Spesal topology**: five asymmetric castle nodes replace the administrative
  v1.0 dot ledger without discarding its information. Each node retains three
  state beads and the 0–7 number. **The link runs the way the signal runs**
  (amended 2026-07-22): castle *k* samples its partner's triangle into its own
  S&H (SPEC §2.2), so the arrow leaves the partner selected by `state % 4` and
  lands on *k*. It pointed the other way — at the one being read — for as long
  as G1 stood; that made it the only directional mark in the app running
  against its own flow, and it was reversed in both skins at once so the two
  can never disagree. **Track-and-hold is a broken link** (`state >= 4`): it
  follows its input while the clock is high and freezes while it is low, so a
  dash pattern *is* the mechanism, and it is still reinforced by the S/T
  letters. The old parallel second line said the same thing twice and ran into
  the arrowhead; cross-ticks were not available, because barbs already mean
  inverso polarity on the cables (§16.8). The small mark inside a node follows
  its instantaneous `/cq_castle` sample and is described only as activity,
  never as a waveform.
- **Drawer & inspector**: the ledger look is honest here — rectangles, grids
  and right angles allowed; the inspector heatmap recolors to verso amber /
  inverso blue on paper; 9 px mono row/column labels.
- **Drop targets**: on drag-over, the deck box (day) or bird (night) gains a
  2 px dashed ink border — the only dashed rectangle in the app. One target is
  marked at a time; every view that accepts the file marks the **body** it
  belongs to, never itself (the night's filename label and wing socket light
  the bird). At night the border rings the bird **and its name**: that is what
  the eye reads as the deck and what actually accepts the file, and the bird's
  own 44×32 box was narrower than the file icon the OS drags under the cursor,
  so the border hid beneath the very thing it was answering.
  Implementation law (measured 2026-07-22): sclang receives no drag-*leave*
  callback from Qt, and `canReceiveDrag` is asked **once per dragEnter, never
  per move** — so the predicate can arm the border but can never release it,
  and a timed release puts it out while the hand is still holding the file
  over the box. **The border is therefore held by where the cursor is**: a
  light poll of `QtGUI.cursorPosition` against the target's own global rect —
  the same rect the border is drawn on, so cue and action share one geometry
  (§4.5). Two independent exits, the cursor leaving and the drag ending, so it
  can never stick. The poll runs only while a file is actually being dragged
  over the instrument; it is hover tracking, not ambient motion (§16.9).

### 16.8 State table (normative)

| element | state | look |
|---|---|---|
| source jack | idle | family dot Ø 8, 1 px ink ring |
| | hover | ring 1.5 px, dot brightens |
| | in hand | dot pulses; cable follows cursor |
| | hot | radial glow ∝ aura magnitude |
| socket | idle | ring per §3.2 |
| | candidate | magnetic halo: ring ×1.2 + soft glow in the held cable's polarity color |
| | auditioning | plug floats 3 px above the rim, cable at 60 % alpha |
| | occupied | plug seated, insertion depth ∝ \|w\| |
| gate/trig lamp | no telemetry | the same dark recessed bead as a zero charge: a lamp has nothing to overstate, so silence and zero may look alike here. `chargeFor` still returns `nil`, so no other surface can mistake one for the other (§9) |
| | charging | lamp fill alpha = charge / 0.5, lamp color |
| | Schmitt on (gate) | lit steady + a Ø 14 bloom at 28 % (landed 2026-07-21: one mechanism, `charge ≥ 0.5`, drawn only while lit) |
| | fired (trig) | the same bloom, decaying with the server's 0.15 s peak-hold — no client-side timer |
| cable | planted | width 1.5–3 px ∝ \|w\|, alpha pulses with source aura |
| | hover target | full alpha + soft 6 px ink under-stroke at 22 %; unrelated planted cables recede to 46 % |
| | grabbed | full alpha, follows cursor |
| | inverso | barbs: 3 px cross-ticks every 24 px along the path |
| | solo-dim | everything un-solo'd at 25 % alpha |
| | marked for harvest | plug lifts 2 px + danger-red under-glow; polarity color unchanged (§16.2) |
| harvest lasso | drawing | 1 px hairline, danger-red at 60 % alpha — exactly the hand's path plus the closing chord; no wobble (the player's own line is drawn true) |
| interactive control | hover | static semantic focus per §4.5; no value/state change |
| PANIC | unmuted | danger-red outline word-button |
| | muted | filled danger-red + 3 px red rule across the master strip top; **the panel is never dimmed** — state must stay readable |
| REC | idle | hollow danger-red ring |
| | starting / stopping | static: lowercase `starting…` / `stopping…` in warning ochre where the elapsed sits; the button ignores presses; no animation |
| | recording | filled ring + mono elapsed, colon blinking at 1 Hz |
| | error | hollow ring with a drawn slash (path, not a typed ✕); the reason in the notice line in danger red; clears on the next REC press |
| notice line | info / warning / danger | whisper / warning ochre / danger-red text |

### 16.9 Motion

| motion | duration / rate |
|---|---|
| hover feedback | immediate |
| press feedback | ≤ 50 ms |
| momentary flash, bloom | 120 ms |
| solo-light fade | 150 ms |
| plant wobble (optional garnish) | ≤ 300 ms damped |
| compass elastic return | ~3 frames glide |
| Day/Night toolbar destination | hard cut |
| tide focus (Tab / gill) | immediate state change |
| ambient (aura, fireflies, sparks, flutter) | telemetry rate, ≤ 15 Hz |

Easing: linear or single-pole decay only — no bounces, no overshoot. The one
comedy allowed is the damped cable wobble on plant. Ambient motion whitelist:
aura breathing, ring fireflies, **spesal coupling lights**, read-head sparks,
deck flutter — all of them information. The coupling lights joined the list on
2026-07-22 for the fireflies' own reason: the S&H sampling they depict never
stops, and they ride the same phase, so every moving mark inside the ring
advances on one set of telemetry frames. Nothing else moves uninvited.

Every ambient motion is driven **by the telemetry frames themselves, never by
a clock of its own** (made explicit 2026-07-22 with the struck read head).
A mark that animates on its own timer keeps promising liveness after the
server has stopped speaking, which is exactly what §9 forbids in stillness.
This also fixes what any ambient motion may mean: it can say *this is live*,
and it can carry a quantity slow enough for the eye to integrate — but it can
never carry a rate the frames cannot resolve. Above roughly 10 Hz a rate is
drawn as weight (§16.6's struck mark), because the fastest blink this surface
could draw would be slower than the truth.

### 16.10 Space & scale

Day keeps the v1.0 column geometry; widgets align to an 8 px spacing grid.
Controls form compact functional islands rather than being spread evenly down
the page. Spare paper belongs to an informative instrument view: each Coco has
a static two-row map of the normative mix → dolby → VCA/feedback → dust → rail
→ bits → RAM → output memory path, while Quantussy devotes its lower field to
the live spesal topology above. These are state/signal maps, not decorative
filler or invented waveform displays. The first Quantussy range rail shares
the exact horizontal baseline of the Coco slot rails; the remaining castle
rows descend from it. Diagram labels must clear their paths (`dust` / `rail`
sit beside the turn, never on its vertical stroke), and adjacent `bits` /
`wrap` bodies share a top edge. Skin sockets sit 4–6 px from
their control's bounding circle. Night bodies
scale with the window (minimum 1280×720) but **stroke widths and font sizes
stay in px** — ink does not thicken when the paper grows. Sockets never
closer than 24 px center-to-center (hit law, §11).

### 16.11 The ink kit (code-only implementation notes)

- One shared drawing kit holds: the palette table (§16.2 — the single color
  source), stroke constants, the seeded wobble-path helper, blob, socket,
  jack, banana plug, lamp, the glyphs, and the cable renderer. **Both views
  and the drawer draw only through the kit** — that is how the organism stays
  one species. The kit's shape is the implementers' choice; its existence is
  normative.
- **Session seed**: one integer drawn at GUI open feeds every wobble and
  asymmetry call; a redraw within a session is pixel-identical.
- Gradients are quiet tools (auras, lamp warmth) — if a gradient is visible
  *as a gradient*, it is too strong.

### 16.12 Cheapness smells (forbidden)

Wood-photo or brushed-metal fills · bevels and gloss · drop shadows beyond
the drawer's one · decorative particles or starfields · emoji or icon-font
icons · uppercase outside PANIC/REC/HOLD · bouncing easings · perfectly
regular polygons for organisms · drawing the hardware's box, screws or panel
· glow on anything that carries no signal · numbers on the night map.

When in doubt, ask: would this mark look at home both in a field notebook and
on a mixing desk at night? If not, redraw it.

---

*Plant by ear. Rip without guilt. Let the weather do some of the playing.*
