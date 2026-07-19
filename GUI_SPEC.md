# GUI_SPEC.md — the Archipelago (CocoquantusSC dream GUI)

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
  wins any conflict with this file). SPEC §6.2 describes the v1.0 panel; when
  this spec ships, §6.2 gets a one-line pointer here (that edit is deferred
  until adoption).
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
| **Drawer** | slides open on demand (click, no key), never over the master strip. Tabs: **inspector** (the old grid), **spellbook** (§8), **memory** (snapshots + morph, moved here). |

Keyboard: the sacred map is unchanged and global in both views —
`1/2` hold · `q/w` flip · `a/s` skip · `3..7` spesal · `r` rec · `space` panic
· `p` shake. New, non-colliding: **Tab** = day/night flip (only when no text
entry has focus), **Esc** = drop the banana in hand / release solo-light,
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
  candidate).
- **Declutter.** Number boxes, snapshot save/recall and morph A/B/duration
  move to the drawer's memory tab. Deck file-drop + filename label stay.
  Meters, LEDs, spesal display, shake button + amount knob stay (amount is
  shared with the night toolbar and the `p` key).
- The old matrix pane leaves the main window → drawer inspector (§8). The
  patch-preset menu moves to the night toolbar and the inspector header.

---

## 6. Night map — the archipelago

Tab flips the same window to night: a patch-first landscape. No knobs, no
numbers — only bodies, jacks, bananas, weather. The master strip stays below.

Topology-true layout (coordinates are a starting sketch; implementers tune by
eye but must keep the topology truths):

- **Five castles in a true ring** (pentagon, castle 1 at top, flow arrows
  k → k+1 — the ring *is* the LocalIn loop, and matches the `\classic` FM
  ring). Each castle: triangle bud (orange source), crenellated bud (gray
  source), inner moon `fm` (blue ring), outer moon `spesal` (green ring +
  charge lamp), an **8-notch spesal dial** with the current state lit (data:
  the existing `/cq_spesal` cache), body glow = its aura (§9). A tiny `i/b/a`
  glyph shows the range switch state — display only; ranges are set in day.
- **Coco 1 left, Coco 2 right: spiral shells** (the delay RAM as a coconut
  cross-section). A **read-head spark travels the spiral** (loop-phase
  telemetry, §9); hold = the spiral ring solidifies red; flip = the spark
  reverses. On the shell: org fruit (yellow source), the spiral's mouth (gray
  source), and moons spaf / inVca / fbVca / flip / skip (lamps on the
  gate/trigs).
- **Inputs as roots** at the bottom: envA left, envB right; root tips glow
  with follower level (aura).
- **Decks as birds** at the top: deckA / deckB. **Dropping an audio file on a
  bird loads it** (existing drop plumbing); the wing socket is the rate moon;
  the filename is written small under the bird.
- Cables follow the same grammar (§4), arcing around bodies, pulsing with
  their source's aura. Solo-light on a body lights everything touching it.
- **Night toolbar** (small, lowercase): patch-preset menu · shake · grow ·
  wither · breed · amount mini-knob · the compass pad (§7).

---

## 7. The weather machine

The mouse's weakness is 352 small targets; its strength is one continuous
gesture. The weather machine gives the matrix a macro plane. **The landscape
law binds everything in this section: weather tools write weights only.** No
CQSpec parameter is ever touched by compass, grow, wither, breed or shake
(same law as SPEC §5.5 shake). Knobs belong to hands. Corollary, recorded in
`docs/history/PLAN_v1.0.md` (KUUNTELU 4): a landscape can be *deaf* if its depth-knobs are down —
aura and charge lamps make that visible instead of mysterious.

### 7.1 Compass — the 2D patch-blend pad (~140 px, night toolbar)

- Four corners = four **matrix memories** (weights only; never params, never
  spesal). A stamp button at each corner captures the current matrix;
  right-click a corner clears it. An empty corner = the zero matrix (all
  unpatched) — traveling toward it is a wither-by-travel, documented behavior.
- **Left-drag** inside the pad: bilinear blend of the four corner matrices,
  written continuously (batched funnel write, one notification per frame,
  ≤ 60 Hz). Release **commits** (one undo op).
- **Right-drag**: **elastic** — same blending live, but release snaps back to
  the pre-gesture matrix (short glide, ~3 frames; no undo entry). The whole
  matrix becomes a wobble you can flick.
- `patchDepth` stays the global tamer on top; the pad never touches it.
- **Persistence:** corners auto-save to `presets/compass.cqcompass.scd` (pure
  data expression, integer version field, validated and clamped on load,
  malformed file rejected with a warning — CQSnapshots' safe-load manners) and
  auto-load at GUI open.

### 7.2 Grow / wither (hold-buttons, ~2.5 Hz while held)

- **grow**: pick a random unpatched cell, plant weight ± U(0.15, 0.4), random
  sign. — **wither**: pick a random patched cell, w ×= 0.7; |w| < 0.05 → 0.
- One hold = one undo op. Language-side rand, no seed UI (seeded chaos stays
  in the code layer: `CQ.shake(amt, seed:)`).
- Shake is the lightning; these are the rain and the drought.

### 7.3 Breed

Press **breed** → click two corners → the offspring becomes the current
matrix (one undo op). Per cell: coin-pick a parent's weight; then with
p = 0.08 mutate by ± U(0, 0.15); clamp. Children differ but rhyme.

---

## 8. Drawer — inspector, spellbook, memory

- **Inspector** = the existing 16×22 grid, demoted but upgraded with
  **touch-and-listen**: press a cell = audition ± 0.3 (mouse button = sign)
  until release, same bookkeeping and revert law as §4.1; double-click =
  plant 0.3; vertical drag = fine edit (current behavior); right-click =
  clear (current behavior). This is the precision path: everything the
  gestural surfaces do must remain exactly doable here.
- **Spellbook**: an append-only log of *committed* matrix ops from **all
  layers** (GUI gestures, code, presets, shake, compass commits — never
  auditions or per-frame compass writes), each as a copyable line in the
  code layer's own tongue: `CQ.patch(\tri3, \coco1_spaf, 0.4)`. Batch
  commits (harvest, breed, compass release, preset, shake) log as one
  `// tool` comment line followed by the diffed cell lines — a harvest reads
  as `// harvest` plus its `CQ.patch(\src, \dest, 0)` lines. Buttons:
  copy line · **copy all as preset data** (`(matrix: [[src, dest, w], ...])`
  to clipboard). The GUI teaches the language instead of competing with it.
- **Memory**: snapshot save/recall list + A→B morph controls (relocated from
  the main window; behavior unchanged, SPEC §7 laws untouched).

---

## 9. Telemetry (the only server-side changes in this spec)

Existing v1.0 streams: `/cq_cocometers` (in/out peaks, id 1/2), `/cq_castle`
(5 tri values, 15 Hz), `/cq_gr`, `/cq_spesal`, `/cq_hot`.

Additions — unconditional control-rate SendReply at ~15 Hz, NRT-harmless,
same pattern as the existing meters:

| phase | message | payload | drives |
|---|---|---|---|
| G1 | `/cq_aura` | 16 source magnitudes (lagged \|amp\|, canonical source order) | jack glow, cable pulse, root/castle/body glow |
| G2 | loop phase | coco read-head position 0..1, appended to `/cq_cocometers` or its own message (implementers' choice) | the spiral spark |
| G2 | `/cq_charge` | the 9 gate/trig destination sums (post-weight, pre-Schmitt) | charge lamps — the answer to "why is my flip banana silent" |

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
| `auditionRadius` | 80 px | socket capture distance |
| audition curve | closeness² | swell late, near the socket |
| `inspectorPlant` | 0.3 | double-click plant weight (= shake default) |
| wheel step | 0.05 / Ctrl 0.01 | weight nudge |
| `growRate` | 2.5 Hz | grow/wither repeat while held |
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
- **Redraw discipline**: ambient animation at telemetry rate (≤ 15 Hz); 60 Hz
  only during an active drag; avoid whole-window repaints; GUI CPU ≤ ~3 % on
  the reference laptop — SPEC §9's 25 % envelope must hold with the GUI open.
- **Overlay hit-testing**: the cable layer must not steal knob/button
  interactions; only jacks/sockets/cables are targets (≥ 12 px, magnetic
  candidate halo). The one exception is the harvest lasso's right-drag
  (§4.4), which may claim only empty ground.
- **Focus law unchanged**: widgets non-focusable; text entry is click-focus
  only and releases on Enter; Tab flips views only when no text entry has
  focus; Esc and Ctrl+Z handled at window level.
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
- Weather tools and compass write **weights only**; `micGain` has no new GUI
  path (only the existing preamp control with its warning color); relocating
  snapshot controls to the drawer changes zero snapshot policy (SPEC §7).
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
  telemetry) + charge lamps (`/cq_charge`) + solo-light + toolbar presets/
  shake + deck-bird file drop. Listening gate: patch a flip banana while
  watching its lamp charge and fire through the Schmitt; confirm the ring
  river's direction matches the `\classic` FM ring audibly.
- **G3 — the weather**: compass + persistence file + grow/wither + breed +
  elastic right-drag + undo polish. Listening gate: stamp two jungles in
  opposite corners and crossfade ecosystems with one finger; right-drag
  wobble snaps back cleanly; breed offspring differs but rhymes.

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
- Tab is a **hard cut**, not a transition. Turning your head, not a cutscene.

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
| lamp fill (charge lamps) | `#E8C97A` | `#FFE9A8` |

Laws:

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
- Sizes: **10 px labels and values, 11 px spellbook/notice line, 9 px range
  letters**. These are the laptop-legibility values approved after the first
  real Day-skin review; do not shrink them back to the earlier 9/10/8 sketch.
  Lowercase everywhere; only PANIC, REC and HOLD may shout.
- Labels are whispers: whisper color, close to their object, never boxed.
  The project's own words — spaf, spesal, verso, balcium — are decoration
  enough.
- The night map carries almost no text: toolbar words, filenames under the
  birds, i/b/a range letters. **Numbers never appear at night** — weights are
  read as depth and width; precision lives in the inspector.

### 16.5 Glyphs — drawn, never typed

All glyphs are Pen paths in a nominal 8×8 box. No unicode symbols, no icon
fonts (their rendering varies across platforms; paths don't).

| glyph | marks | recipe |
|---|---|---|
| open wedge | inVca / fbVca sockets | open triangle pointing into the socket |
| opposed chevrons | flip | mirrored ‹ › pair |
| spark | skip; fired lamps | 4-point star: two crossed strokes, long/short arms |
| wave | castle fm | one sine cycle from two béziers |
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
lamp at 8 o'clock, r ≈ 34. Spesal dial = 8 ticks at r ≈ 26 full circle,
current state's tick lit. Range letter i/b/a in whisper at 6 o'clock below.

**The ring.** Irregular circle R ≈ 150 px through the five castles, 1 px
hairline. Flow k → k+1 shown by two **firefly dots** per arc segment
(Ø 2 px bone, ≈ 2 s per segment) — permitted ambient motion, because it
depicts the always-running LocalIn loop: it *is* information.

**Coco (×2).** Spiral = Archimedean, 2.5 turns, outer R ≈ 85 px, 1.5 px
stroke; the mouth at the outer end carries the gray coco-voice source dot and
a 6 px direction chevron. **Read-head spark** = Ø 4 px bone dot traveling the
spiral (loop-phase telemetry, §9); flip reverses spark and chevron. **Hold**:
the outermost turn closes into a full circle and tints danger-red at 60 % —
the loop is sealed. Org fruit = yellow Ø 7 dot on a 6 px stem from the second
turn. Moons spaf / inVca / fbVca / flip / skip sit on an arc under the
spiral, glyphs per §16.5, lamps on flip/skip. The spiral's stroke alpha
breathes 0.55 → 1.0 with the coco's out level.

**Roots (inputs).** Two branching wobbled polylines (3 segments) growing from
the bottom edge, envA left, envB right; tips = green Ø 7 dots with radial
glow ∝ follower aura. Beside them grows a small **mic sprout** — display
only, never a jack: the mic is not one of the 16 sources but an optional
input feeding the preamps, so nothing on the sprout can be grabbed or
patched. Three states: not drawn at all when no mic is configured; a dormant
whisper-colored bud when configured (`micChan` set) with `micGain` = 0; bud
and tip ring danger-red while `micChan` is set **and** `micGain` > 0 — the
existing warning made flesh. The day panel's mic control keeps its own
warning-color law unchanged.

**Birds (decks).** Three strokes each — two wing béziers + a body dot,
span ≈ 30 px; filename in 10 px mono whisper beneath; the rate socket under
the wing. Flutter = 1 px wing bob at ambient rate while the deck's level
> 0.05 (client-side state, no polling). On file-drag hover the bird
brightens (drop target law, §16.7).

**Weather corner.** Compass pad = 140 px round-cornered square, hairline
border, stamp dots in the corners, small cross cursor; while dragging, four
faint corner gradients (≤ 8 % alpha) show the pull. grow · wither · breed ·
shake as lowercase word-buttons beside it.

### 16.7 Widgets (day-panel language)

- **Knob**: three reviewed body sizes express breadth of consequence, never
  current value: standard Ø 26 for trims/mix/routing; featured Ø 32 for the
  two Coco identity controls `speed` / `spaf`; primary Ø 36 for `q freq`,
  `chaos` and master `vol`. All use 1.5 px ink, wedge pointer and a 2 px value
  arc from min-angle to current — whisper-colored, family-accented where the
  param has a family (the spaf knob wears a purple arc). Drag brightens pointer
  and arc to full ink. No 3D, no gloss.
- **Slider** (slots, sends): hairline rail, Ø 8 thumb, whisper value fill.
- **Meters**: 1 px ink outline, whisper fill, 1 px peak tick; the GR meter
  fills downward in danger-red.
- **Buttons**: rounded rect (r 3), 1.5 px ink outline, paper fill; latched
  on = ink fill with paper text; momentary = 120 ms ink flash (the existing
  skip flash, generalized).
- **HOLD LED**: Ø 8; off = hollow ring; on = danger-red fill with a soft
  radial glow — the only glowing thing on paper.
- **3-state switches** (range, dolby): three tick positions on a short rail,
  ink dot at the current one.
- **Skin sockets & jacks**: exactly the night recipes, inked — same geometry
  everywhere, so the hand learns one shape.
- **Spesal display**: geometry kept from v1.0, re-inked.
- **Drawer & inspector**: the ledger look is honest here — rectangles, grids
  and right angles allowed; the inspector heatmap recolors to verso amber /
  inverso blue on paper; 9 px mono row/column labels.
- **Drop targets**: on drag-over, the deck box (day) or bird (night) gains a
  2 px dashed ink border — the only dashed rectangle in the app.

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
| gate/trig lamp | charging | lamp fill alpha = charge / 0.5, lamp color |
| | Schmitt on (gate) | lit steady |
| | fired (trig) | 120 ms spark bloom |
| cable | planted | width 1.5–3 px ∝ \|w\|, alpha pulses with source aura |
| | grabbed | full alpha, follows cursor |
| | inverso | barbs: 3 px cross-ticks every 24 px along the path |
| | solo-dim | everything un-solo'd at 25 % alpha |
| | marked for harvest | plug lifts 2 px + danger-red under-glow; polarity color unchanged (§16.2) |
| harvest lasso | drawing | 1 px hairline, danger-red at 60 % alpha — exactly the hand's path plus the closing chord; no wobble (the player's own line is drawn true) |
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
| view flip (Tab) | hard cut |
| ambient (aura, fireflies, sparks, flutter) | telemetry rate, ≤ 15 Hz |

Easing: linear or single-pole decay only — no bounces, no overshoot. The one
comedy allowed is the damped cable wobble on plant. Ambient motion whitelist:
aura breathing, ring fireflies, read-head sparks, deck flutter — all of them
information. Nothing else moves uninvited.

### 16.10 Space & scale

Day keeps the v1.0 column geometry; widgets align to an 8 px spacing grid.
At heights above the 720 px minimum, spare vertical paper is divided between
semantic control clusters (source / performance / codec / output), not pooled
as one empty basement below a top-packed form. Skin sockets sit 4–6 px from
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
