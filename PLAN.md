# PLAN.md — CocoquantusSC toteutussuunnitelma

Vaiheistettu checklist. Toimii CLAUDE.md:n milestone-checklistinä (TODO.md:n roolissa).
Normatiiviset lähteet: SPEC.md (käyttäytyminen), ARCHITECTURE.md (rakenne).
Milestone-mappaus: **M1 = vaiheet 0–4 · M2 = vaiheet 5–6 · M3 = vaihe 7**.

**Tilanne 2026-07-16:** vaiheet 0–3 valmiit ja hyväksytty (`b03692b`, `a7c278b`,
`b7cb33a` + palautekorjaukset, `7f890d4`). **Vaihe 3 (Quantussy) hyväksytty
2026-07-16** (KUUNTELU 3 läpi, `tests/manual_checklist.md` M3 ruksattu).
**Vaihe 4 (reititysmatriisi + presetit) koodi valmis 2026-07-16**, kaikki neljä
markeria vihreää (A1–A5, ks. testitulokset alla). **KUUNTELU 4 -gate odottaa** —
skripti on `manual_checklist.md`:n M6-osio (vaatii Ctrl+Shift+L, deck A -lataus,
`CQ.patchPreset(\classic)`). M1 valmistuu vasta käyttäjän KUUNTELU 4 -hyväksynnän
jälkeen.

## Läpileikkaavat säännöt

- Checkboxin saa ruksata vain kun vaiheen kaikki testimarkerit ovat vihreitä **ja**
  mahdollinen kuuntelu-gate on käyttäjän hyväksymä.
- Jokaisen `classes/`-muutoksen jälkeen ajetaan CLAUDE.md:n testikomennot järjestyksessä
  (aina lipulla `-l tests/sclang_conf_dev.yaml`, jotta include-polut eivät riipu
  käyttäjän globaalista confista):
  1. `"<sclang.exe>" -l tests/sclang_conf_dev.yaml -i none tests/compile_check.scd` → `CQ_COMPILE_OK`
  2. `"<sclang.exe>" -l tests/sclang_conf_dev.yaml -i none tests/run_all.scd` → `CQ_TESTS_PASSED`
  3. `"<sclang.exe>" -l tests/sclang_conf_dev.yaml -i none tests/nrt_safety.scd` → `CQ_SAFETY_PASSED`
  4. DSP-muutoksissa lisäksi `tests/nrt_hold_integrity.scd` → `CQ_HOLD_OK`
- Ennen jokaista kuuntelu-gatea: muistutus käyttäjälle **Ctrl+Shift+L** (class-lib
  recompile IDE:ssä), kun `classes/` on muuttunut.
- Kuuntelu-gate: Claude antaa täsmällisen 3–5 askeleen skriptin (mitä ajetaan, mitä
  kuunnellaan) → käyttäjä raportoi → OK tai palaute-iteraatio (hypoteesi → muutos →
  testit → uusi kuunteluskripti, **yksi muuttuja kerrallaan**) ennen seuraavaa vaihetta.
- Git-commit jokaisen hyväksytyn vaiheen päätteeksi.
- Jos käyttäjälle näkyvä käyttäytyminen muuttuu palautteen perusteella → SPEC.md
  päivitetään samassa yhteydessä (SPEC ↔ CQSpec eivät saa erkaantua).

### Kirjatut toteutuspäätökset (vaiheet 0–1)
- Testiluokat asuvat `tests/classes/*.sc`-tiedostoissa (sclang ei voi määritellä
  luokkia .scd-skripteissä); ei `logic_tests.scd`:tä. ARCHITECTURE §2/§8 korjattu.
- Headless-ajoissa markerin jälkeinen `cleaning up OSC` -rivi on sclangin normaali
  exit-tuloste — markerin läsnäolo + exit code 0 ratkaisevat.
- **Enum-parametrit kulkevat serverille indeksinä** (CQSpecin values-listan järjestys);
  CQState tekee muunnoksen — tulevat GUI/MIDI-kerrokset kirjoittavat symboleina
  CQStaten kautta, eivät koskaan indeksejä suoraan.
- `aDry`-väylä (2 ch): \cqInput kirjoittaa käsittelemättömän monitorisyötteen
  (live L/R + mic), \cqMaster lukee `masterDry`-tasolla; deckit lisäävät itsensä
  vaiheessa 2. Kirjattu ARCHITECTURE §3.2:een.
- `config.device` hyväksyy myös `[inDevice, outDevice]`-parin (sama host-API!) —
  tarvitaan läppäreillä, joissa mikki ja kuulokkeet ovat eri laitteita (SPEC §7).
- `CQ.resume` lisätty panicin pariksi (SPEC §6.3); fade-in-verhokäyrä ei laukea
  uudelleen resumessa (mute on kerroin, EnvGen pysyy arvossa 1).

---

## Vaihe 0 — Perusta ja testi-infra (ei ääntä)

### Toteutus
- [x] Kysy käyttäjältä SuperColliderin asennuspolku → `tests/dev_paths.scd` (gitignored)
- [x] `.gitignore` — `config.scd`, `tests/dev_paths.scd`, `tests/sclang_conf_dev.yaml`,
      `samples/`, `recordings/`, `tests/tmp/`
- [x] `config.example.scd` — SPEC §7:n template
- [x] `config.scd` — luotu oletuksilla; laite asetettu vaiheessa 1 (tämä läppäri:
      WASAPI-pari AMD-mikki sisään / Realtek-kuulokkeet ulos; pöytäkoneelle tehdään
      myöhemmin oma config.scd ulkoisen äänikortin ASIO-nimellä)
- [x] `classes/CQSpec.sc` — **kaikki** SPEC §5:n 79 parametria (nimi/range/curve/default/
      lag/16n-slot) + `*clamp`, `*default`; ainoa paikka jossa rangeja on
- [x] `classes/CQConfig.sc` — configin lataus, oletusten täyttö, validointi,
      informatiivinen hard-fail
- [x] Headless-ajojen includePaths-järjestely: generoitu `tests/sclang_conf_dev.yaml`
      (gitignored), ajot `-l tests/sclang_conf_dev.yaml` -lipulla (ARCHITECTURE §10)
- [x] `tests/compile_check.scd` — luokat + (myöhemmin) SynthDefit kääntyvät ilman
      serveriä → `CQ_COMPILE_OK`
- [x] `tests/classes/CQSpecTest.sc` + `tests/classes/CQConfigTest.sc` — UnitTest-luokat
      (clamp-fuzzaus: NaN/inf/nil/Symbol/string; config-validointi). Huom: sclang-luokat
      voivat asua vain include-polun `.sc`-tiedostoissa, siksi ei `logic_tests.scd`:tä
- [x] `tests/run_all.scd` — UnitTest-runner → `CQ_TESTS_PASSED` / `CQ_TESTS_FAILED`

### Testit
- [x] `CQ_COMPILE_OK` (2026-07-15)
- [x] `CQ_TESTS_PASSED` (884 assertiota, 0 virhettä, 2026-07-15)

### Käyttäjä-gate (ei kuuntelua)
- [x] Käyttäjä lisää `classes/` JA `tests/classes/` IDE:n `sclang_conf.yaml`:n
      includePathsiin, Ctrl+Shift+L → luokat kääntyvät IDE:ssä ilman virheitä
      (OK 2026-07-15: `CQSpec.specs.size` → 79 IDE:ssä; vaati SC:n uudelleenkäynnistyksen)

---

## Vaihe 1 — Boot, palvelingraafi ja master-turvaketju (ensimmäinen ääni)

### Toteutus
- [x] `classes/CQGraph.sc` — ryhmät (ARCH §3.1), väylät (ARCH §3.2), bufferit,
      ServerOptions configista, `s.sync`-vaiheistus, teardown, CmdPeriod-hook
- [x] `classes/CQState.sc` — funnel: `set` → clamp (CQSpec) → server → `changed`
      (+ `tests/classes/CQStateTest.sc`, toimii ilman serveriä)
- [x] `classes/Cocoquantus.sc` + `CQ`-alias — `*boot` (ARCH §6: hiljainen kunnes graafi
      verifioitu → 2 s fade), `*free`, `*set/get`, `*panic`/`*resume`, `*reboot`
- [x] `classes/CQSynthDefs.sc` alku:
      `\cqMaster` — **koko turvaketju** (LeakDC → tanh −6 dB → Limiter 0.89/0.01 →
      bad-value squelch → boot-fade; ARCH §4.6) + meterit;
      `\cqInput` — live-tulot, preamp A/B (fuzz + bias), env followerit → aModSrc
      (ARCH §4.2)
- [x] `boot.scd` — käyttäjän entry point; `live.scd` — stub
- [x] `tests/nrt_safety.scd` v1 — NRT-infra (Score + `d_recv`, ei audiolaitetta):
      master-ketjun kidutus (4× fullscale-kohina + DC ±1.0 sisään) → peak ≤ −0.75 dBFS,
      ei NaN, |DC| ≤ 0.02; erillinen NaN-injektiorender → squelch pitää

### Testit
- [x] `CQ_COMPILE_OK` · `CQ_TESTS_PASSED` (894) · `CQ_SAFETY_PASSED`
      (torture-peak tasan 0.89 = limitterin katto; DC ~1e-4; 2026-07-15)

### KUUNTELU 1 (lyhyt) — boot & turvallisuus
- [x] Käyttäjä (Ctrl+Shift+L ensin): `CQ.boot` → täysi hiljaisuus, sitten 2 s fade-in
- [x] `CQ.set(\masterDry, 0.5)` + live-signaali sisään (puhe/modulaari) → kuuluu puhtaana
      (vaati `device: [inDevice, outDevice]` -tuen: läppärin mikki on AMD-laite,
      kuulokkeet Realtek — WASAPI-pari configiin; tuki lisätty CQConfig/CQGraph + SPEC §7)
- [x] `CQ.panic` → välitön mute; uudelleen → palaa
- [x] `masterVol`-liikkeet eivät klikkaa
- [x] Käyttäjän OK → vaihe hyväksytty (2026-07-16)

---

## Vaihe 2 — Deck A + Coco-pari (sydän)

### Toteutus
- [x] `classes/CQDeck.sc` + `\cqDeck` — deck A: lataus, −6 dBFS-normalisointi,
      rate/loop, bufferinvaihto crossfadella (ARCH §4.1)
- [x] `\cqCoco` ×2 (ARCH §4.4) — **BufRd ENNEN BufWr:ää** (load-bearing!);
      converter-järjestys SPEC §5.3: input mix → dolby(in) → ×inVCA → +feedback
      (dolby(fb) → ×fbVCA) → +dust → `clip2(1)`/wrap → kvantisointi → RAM;
      hold (identity rewrite = lossless), flip, skip, dolby-helper, organ,
      bits/wrap/interp, slot-mikseri, InFeedback-ristisyöttö, speed-clamp 1500 Hz–SR
- [x] Power-on-tila: bufferit kohinatäytteisiä (8-bit-kvantisoitu, −12 dBFS),
      `hold = 1` (SPEC §4)
- [x] Facade: `CQ.hold/flip/skip` (arvotta = toggle), `CQ.deckA`
      (load/rate/loop/level/restart)
- [x] `tests/nrt_hold_integrity.scd` — SPEC A4 (hold 60 s + speed-sweep ±2 okt,
      korrelaatio ≥ 0.999) ja A5 (unity looper, energia ±1 dB / 30 s) → `CQ_HOLD_OK`
- [x] `tests/nrt_safety.scd` laajennus: coco fb 1.25 + drive 40 + wrap + dust max
      + bits 3 + ristisyöttö 1.5 molempiin suuntiin
- [x] `tests/manual_checklist.md` luotu (M1, M2, M5-osat + dust; kasvaa vaiheittain)

### Testit
- [x] `CQ_COMPILE_OK` · `CQ_TESTS_PASSED` (897) · `CQ_SAFETY_PASSED`
      (coco-pahin peak 0.868 ≤ 0.89) · `CQ_HOLD_OK` (A4-korrelaatio 1.0 =
      bittitarkka; A5-drift 0.0 dB) (2026-07-16)

### KUUNTELU 2 (iso, ~15 min) — Coco-identiteetti
- [x] (a) Kenttä-äänite deck A:han, `coco1Hold 0` → nauhoita → `coco1Hold 1` →
      `coco1Speed`-sweep: tape-repitch; hidas = tumma/rouhea, nopea = kirkas (M1)
- [x] (b) Feedback-ride: 0.9 ≈ kaiut, 1.0 ≈ looper, 1.15 = eskaloituva crunch,
      **taso pysyy vakaana** (M2). Fb-pudotuksen portaittainen tyhjennys
      (askel/loop-kierto + bittilattian katkaisu) todettu autenttiseksi
- [x] (c) Flip kääntää suunnan, skip resetoi loopin alkuun (M5-osa)
- [x] (d) Dolby duck → "time-layer"-artikulaatio (M5-osa; testattu puheella
      mikistä, preADrive 6 — tasainen tekstuuri ei artikuloi duckia kuuluvasti)
- [x] (e) Input 0, fb 1.1, dust ylös → "digital dust" kertyy ja jää looppiin
      (kertyy purskeina: LFNoise2-moduloitu dust + bittilattia, by design)
- [x] Käyttäjän OK → vaihe hyväksytty (2026-07-16)

### Kirjatut toteutuspäätökset (vaihe 2)
- Power-on-kohinan täyttötaso laskettu −12 → **−26 dBFS** kuuntelupalautteen
  perusteella (peitti ohjelmamateriaalin; taso on yksi luku
  `CQGraph.prNoiseBuffer`:ssa).
- `\cqCoco`n matriisikiinnityspisteet ovat defissä nollapaikkoina kommentein
  merkittyinä (spaf-summa, in/fb-VCA-summat; flip-gate ja skip-trig tulevat
  parametrin rinnalle) — **vaihe 4 korvaa nollat wDest-väylien lukemisella**.
- Deck-synth on olemassa vasta latauksen jälkeen (ainoa sallittu synth churn):
  `CQState.prSend` ohittaa puuttuvan synthin hiljaa, `CQDeck.prSwap` spawnaa
  uuden nykyisillä state-arvoilla ja rekisteröi serveriobjektit CQGraphiin
  (`registerSynth`/`registerBuffer`) — teardown pysyy CQGraphissa.
- `cocoNSend`/space-parametrit eivät reititä mihinkään ennen vaihetta 7
  (`\cqSpace`); `cocoNPan` reitittyy masteriin (`pan1`/`pan2`).
- sclang-stringeissä kenoviiva on escape → Windows-polut kauttaviivoilla
  (muistutus live.scd:ssä ja manual_checklist.md:ssä); suhteelliset polut
  ratkeavat repon `samples/`-kansiosta (luotu, gitignored).
- `CQ.set(\param, ...)` hyppää 30 ms lagilla → pitkät tape-sweepit tehdään
  liu'uttamalla arvoja (16n-fader vaiheessa 5 on varsinainen interpolaattori).
- Kuunteluhavainnot, jotka ovat autenttista käytöstä (ei bugeja): fb-pudotus
  tyhjenee portaittain (askel = loop-kierto + bittilattian katkaisu), dust
  kertyy purskeina (LFNoise2-modulaatio + kvantisointikynnys), duck vaatii
  taukoisen syötteen kuuluakseen, hold-looppi on bittitarkan staattinen.

---

## Vaihe 3 — Quantussy

### Toteutus
- [x] `\cqQuantussy` (ARCH §4.3) — 5 castlea audio-ratella, LocalIn/LocalOut-rinki,
      S&H-parit (partner = state mod 4, mode = state div 4), spesal 8 tilaa,
      taajuuskaava `qFreq × range × 2^(off + qChaos·5·sh2 + fmExt)` clip 0.01–4000 Hz,
      qShLag, qMix (per-castle sendit), SendReply spesal-tiloista (`/cq_spesal`)
- [x] Boot-oletukset: ranget `[\infra, \infra, \balcium, \infra, \audio]`,
      off-detunet SPEC §5.4:n mukaan, c5Send 0.5 (CQSpec-defaultit + routet →
      pushAll hoitaa)
- [x] Facade: `CQ.spesal(k)`, `spesalSet`
- [x] `tests/nrt_safety.scd` lisäys: qMix-render ei-hiljainen ja rajattu
      (kaikki castlet audio-rangella, chaos 1, sendit 1 → coco1 slotQ 1.5
      pahimmillaan: peak 0.618, ei NaN, DC ok)
- [x] `tests/manual_checklist.md`: M3-osio (KUUNTELU 3 -skripti)

### Testit
- [x] `CQ_COMPILE_OK` · `CQ_TESTS_PASSED` (897) · `CQ_SAFETY_PASSED` ·
      `CQ_HOLD_OK` (varmuudeksi, coco-DSP ennallaan) (2026-07-16)

### KUUNTELU 3 — Quantussy-tuntuma
- [x] `CQ.set(\coco1SlotQ, 1)` → castle 5 kuuluu coco1:n läpi
- [x] (a) qChaos-sweep 0→1 qFreq 4:llä: stable → vibrato → stepped wander →
      hyperchaos (purskeet, hiljaisuudet) (M3)
- [x] (b) qFreq-sweep: kaikki viisi skaalautuvat yhdessä, LFO↔audio ristiin pehmeästi
- [x] (c) Range-kytkimet siirtävät castlen infra/balcium/audio-alueelle
- [x] (d) `CQ.spesal(k)` -askeleet muuttavat tekstuuria kuultavasti
- [x] (e) qShLag: 4 ms ≈ autenttiset askeleet, isompi kesyttää
- [x] Käyttäjän OK → vaihe hyväksytty (2026-07-16)

### Kirjatut toteutuspäätökset (vaihe 3)
- ARCH §4.3:n sketsissä castlen oma trig oli kehämääritelmä (trig → sh2 → hz →
  tri → trig). Ratkaisu: castlen **oma kello johdetaan feedback-ringin
  (LocalIn) edellisen blokin kolmiosta** — yhden blokin viive on sama jota
  koko rinki jo käyttää (analogisilmukan stand-in, by design, ei "korjata").
- Spesal-laskuri on **kokonaan serverillä**: `Stepper.ar(0..7, wrap)` per
  castle; `\t_stepN` askeltaa (vaihe 4 summaa matriisitrigin samaan
  inputtiin), `\t_setN` lataa `\stateN`-arvon (resetval), `Impulse.ar(0)`
  pakottaa boot-tilan. Tilat raportoidaan `SendReply`llä `/cq_spesal`
  (kuuntelija tulee GUI:n myötä vaiheessa 6). `CQ.spesal/spesalSet` ovat
  aktioita kuten skip — eivät CQSpec-parametreja; spesalSet clampaa mod 8.
- Range-kertoimet (×0.02/×1/×40) ja taajuusclampit (0.01–4000 Hz) ovat
  defissä SPEC §5.4 -viittein — enum-arvojen merkityksiä, eivät rangeja.
- qShLag-lagattu `cas` kiertää myös feedback-ringissä (sketsin mukaisesti),
  eli isompi qShLag kesyttää nimenomaan koko verkkoa, ei vain ulostuloa.
- Synth-argit: `\qFreq \qChaos \qShLag`, per castle `\rangeN \offN \sendN
  \stateN \t_stepN \t_setN` (CQState reitittää `cNRange→rangeN` jne.,
  enumit indekseinä kuten ennenkin).

---

## Vaihe 4 — Reititysmatriisi + presetit → **M1 valmis**

### Toteutus
- [x] `classes/CQMatrix.sc` — name↔index (16 src / 22 dest, kanoniset järjestykset
      ARCH §3.2–3.3), painot −1…1 + clamp, `patch/unpatch/clearPatch/getPatch`,
      preset-data, `wDest`-väylät `setn`:llä
- [x] `CQGraph`: 22 × 16-kanavaista control-väylää; kuluttajaluvut kaikkiin defseihin:
      coco `_spaf/_inVca/_fbVca/_flip/_skip`, castle `_fm/_spesal`, deck `_rate`;
      Schmitt-kynnys gate/trig-desteille; `patchDepth`-makro (lag 50 ms)
- [x] Facade: `CQ.patch(\src, \dest, w)` jne.
- [x] Presetit: `\empty`, `\classic` (+ `\mayhem`-data A3-turvatestiä varten)
- [x] `CQMatrixTest` logic_testsiin (name↔index, painorajat)
- [x] **Täysi A3** `tests/nrt_safety.scd`: `\mayhem` + fb 1.25 + drive 40 + wrap +
      dust max + chaos 1 + qFreq 500 + spaf ±4, 90 s, ei inputtia
- [x] Auditointilisäys (2026-07-16): `nrt_safety.scd` matrix-FM-regressiotesti —
      tri1 → castle5_fm painolla 1, castle 5 audio-alueella; renderin puoliskojen
      nollaylitysmäärien on erottava (ylös-/alas-sweep) → todistaa että
      matriisireititys quantussyn sisällä on elossa, ei vain rajattu

### Testit — SPEC A1–A5 kaikki vihreitä = M1 exit
- [x] `CQ_COMPILE_OK` (A1) · `CQ_TESTS_PASSED` (A2, 1641) · `CQ_SAFETY_PASSED`
      (A3, mayhem-peak 0.8806) · `CQ_HOLD_OK` (A4 korrelaatio 1.0, A5 drift 0.0 dB)
      (2026-07-16)

### KUUNTELU 4 — M1-hyväksyntä
- [ ] Kenttä-äänite + `CQ.patchPreset(\classic)` oletuksilla → heti "Cocoquantus-like":
      huojuva, aliasoiva, elävä, koskematta mihinkään (M6)
- [ ] Inverso-banaani: `CQ.patch(\envA, \coco2_fbVca, -0.5)` → ducking kuuluu
- [ ] `patchDepth` 1→0 kesyttää kaiken pehmeästi
- [ ] ~20 min code-only-sessio (`live.scd`-esimerkeillä)
- [ ] Käyttäjän M1-hyväksyntä → milestone valmis

### Kirjatut toteutuspäätökset (vaihe 4)
- `wDest`-väylät asuvat `CQGraph.buses`-sanakirjassa suoraan destinaationimellä
  avaimena (esim. `\coco1_spaf`) — ei erillistä `w`-etuliitettä, koska nimet eivät
  törmää audio-väylien nimiin.
- Matriisin summat luetaan **audio-rate**illa (`mods = In.ar/InFeedback.ar(...)`,
  `Mix(mods*w)`), ARCH §3.3:n pseudokoodin mukaisesti — sama "audio ylettyy
  kaikkialle" -periaate kuin Quantussyn castleissa. `clockHz`/`dir`/`phase`
  muuttuvat siksi audio-ratesta lähtien coco-defissä; tämä on tarkoituksellista,
  ei regressio.
- InFeedback-sääntö on **lukijakohtainen** (kirjoittajat ryhmäjärjestyksessä:
  ch 0–9 quantussy, ch 10–11 input, ch 12–15 cocot): kanava luetaan `In.ar`illa
  vain jos sen kirjoittaja on jo ajettu tällä blokilla, muuten `InFeedback.ar`
  (koskematon audioväylä lukisi `In.ar`illa nollia, ei edellistä blokkia).
  → Cocot: 0–11 `In.ar`, 12–15 `InFeedback` (sis. oman tapin). **Quantussy: 0–9
  `InFeedback`** — se lukee omia ulostulojaan ennen kuin on kirjoittanut ne
  tällä blokilla; yhden blokin viive on sama kuin LocalIn-ringissä — 10–11
  `In.ar`, 12–15 `InFeedback`. Deckit: kaikki 16 `InFeedback`illä, koska deck
  ajetaan `gSources`issa ensimmäisenä (`\addToHead`, ennen `\cqInput`iakin).
  (Auditointikorjaus 2026-07-16: ensimmäinen toteutus luki quantussyn sisällä
  ch 0–9 `In.ar`illa → tri/cas-lähteiset castle_fm/spesal-patchit — mm.
  `\classic`in FM-rinki — olivat hiljaa kuolleita.)
- `patchDepth` on ensimmäinen CQState-reitti, joka haarautuu useaan synthiin
  (`coco1/coco2/quantussy/deckA/deckB`) — `CQState.prSend` yleistettiin
  tunnistamaan `route[0]`:n tyypistä (Symbol = yksi pari, Array = pari-lista),
  vanhat yksittäisreitit toimivat muuttumattomina.
- Flip on **gate** (matriisi ORataan param-arvon kanssa, `max(...)`), skip ja
  castleN_spesal ovat **trig**ejä (→ `Trig1.ar`), spaf/inVca/fbVca/castleN_fm
  ovat jatkuvia summia — SPEC §5.5:n gate/trig-jaon mukaisesti. **Kaikki**
  gate/trig-destit kulkevat Schmitt-kynnyksen läpi (`Schmidt.ar(sum, 0.3,
  0.5)`: päälle nousten +0.5:n yli, pois alle +0.3:n) — paljas komparaattori
  värisisi kohinaisilla summilla (SPEC §5.5 "Schmitt threshold").
- `\mayhem`-preset on tässä vaiheessa **deterministinen** tiheä patch (jokainen
  16×22-solu ±0.6, etumerkki vuorottelee) — ei vielä satunnaisuutta (shake tulee
  vaiheessa 6). Tämä sama data-aineisto ajaa myös täyden A3-turvatestin.
- `\classic`-preset: `tri1 → coco1_spaf`/`coco2_spaf` (0.35, hidas drift) +
  `tri3 → spaf` (0.12, ~3 Hz flutter balcium-castlesta) + 5-castlen FM-rinki
  `cas_k → castle_(k+1)_fm` (0.3) — SPEC §5.5:n kuvauksen ("tri→spaf both
  Cocos, castle→castle FM ring") toteutus.
- **Preset = painot + nupit** (KUUNTELU 4 -löydös 2026-07-17): pelkkä
  `tri→spaf`-banaani on kuuroa dataa, koska spaf-syvyysnuppi (`cocoNSpaf`,
  SPEC §5.3: "spaf sum scaled by cocoNSpaf") on oletuksena 0 — M6:n
  "warbling at defaults" ei voi toteutua ilman että preset nostaa nupin.
  Preset-data on nyt `(matrix: [[src, dest, w]...], params: (nimi: arvo...))`;
  `CQMatrix.preset` soveltaa vain matriisiosan (ei tunne CQStatea),
  `Cocoquantus.patchPreset` soveltaa lisäksi params-osan CQState-suppilon
  läpi (clampattu). `\classic` params: `coco1Spaf/coco2Spaf` — ensin 0.8,
  kuuntelukalibroitu **0.12**:een 2026-07-17 (käyttäjän data: 0.1 "vain
  elävää huojuntaa" mutta puheessa hädin tuskin kuuluva, 0.15 paikoin jo
  merisairas, 0.2 melodisissa äänissä selvä vibrato; 0.12 = drift ±50 c,
  flutter ±17 c). Drift-kuvion kvasiperiodisuus (samanmuotoiset ~6–21 s
  kolmioaallot, taajuus askeltaa kerran per sykli) todettu autenttiseksi —
  arvaamattomuus kuuluu qChaos-nupille, ei presetille.
- KUUNTELU 4 -havainto (2026-07-17): **inVca/fbVca-duckit kuuluvat loop-
  kierron viiveellä** (~5.5 s oletuksilla), koska VCA:t istuvat kierrätyksen
  kirjoituspolussa ennen RAMia (SPEC §5.3:n normatiivinen järjestys; sama
  kuin hardwaressa) — monitoroitava ääni on aina RAMista luettua. Välitön
  kuultava env-reititys demotaan spaf-destillä (kello muuttuu heti);
  fbVca-duck kuunnellaan yhtäjaksoisella ≥ loop-kierron puheella.
  manual_checklist M6 kohta 2 jaettu 2a/2b tämän mukaisesti.
  Sama rakenne on välttämätön vaiheen 6 preseteille (SPEC: `\selfNoise`
  tarvitsee dust/fb-tasot, `\organMode` organ-tasot). CQMatrixTest varmistaa,
  että preset-paramit ovat olemassa olevia CQSpec-nimiä ja selviävät omasta
  clampistaan.
- `tests/nrt_safety.scd`: mayhem-testin control-väylät alkavat offsetista 100,
  jotta ne eivät koskaan törmää muiden testitapausten `c_set 0 0`
  -loppumerkkiin. `render`-apufunktio hyväksyy nyt valinnaisen kesto-parametrin
  (maxWait skaalautuu renderin pituuden mukaan) — vanhat 2-argumenttiset kutsut
  toimivat muuttumattomina.

---

## Vaihe 5 — Snapshotit + 16n (M2 alku)

### Toteutus
- [ ] `classes/CQSnapshots.sc` — save/recall/morph (ARCH §7:n tiedostoformaatti,
      version-gate, clamp latauksessa, **micGain ei koskaan auto-recallina > 0**,
      SPEC §7)
- [ ] `classes/CQMidi16n.sc` — CC 32–47 ch 1 configista, 30 ms smoothing,
      `\jump`/`\pickup`-takeover, hot-plug, `MIDIClient.init` guarded,
      `MIDIdef`-avaimet `\cq_*`; kaikki kirjoitukset CQStaten kautta
- [ ] Deck B + kansionavigointi (next/prev/random) + linked stereo -tila
- [ ] Mic-polku (`micGain`, oletus 0) + varoituslogiikka (headless-osa)
- [ ] `presets/`-esimerkkisnapshot
- [ ] `CQSnapshotTest` — roundtrip-yhtäsuuruus, malformed-tiedoston hylkäys

### Testit
- [ ] `CQ_COMPILE_OK` · `CQ_TESTS_PASSED` · `CQ_SAFETY_PASSED`

### KUUNTELU/KÄYTTÖ 5 — 16n & muisti
- [ ] Kaikki 16 faderia oikeissa parametreissa, liike pehmeää (≤ 20 ms tuntuma) (M8)
- [ ] 16n irti ja takaisin kesken session → toipuu (M8)
- [ ] `CQ.snap(\a)` → säädä → `CQ.recall(\a)` → tila palaa korvakuulolta;
      `CQ.morph(\a, \b, 30)` liukuu musikaalisesti
- [ ] Jump vs pickup -takeover käyttäytyy odotetusti recallin jälkeen
- [ ] Käyttäjän OK → vaihe hyväksytty

---

## Vaihe 6 — GUI, shake, loput presetit, perusäänitys → **M2 valmis**

### Toteutus
- [ ] `classes/CQGui.sc` — SPEC §6.2: neljä kolumnia + master-strip, banaaniväri-
      koodi, meterit/LEDit OSCdefeillä, matriisigrid, file drop, näppäinkartta;
      puhdas view (dependants, `{ }.defer`, ei server-pollausta)
- [ ] `CQ.shake(amount, seed)` — vain painot + spesal, ei koskaan level/feedback;
      determinismitesti seedillä logic_testsiin
- [ ] Presetit: `\selfNoise`, `\quantumDrone`, `\organMode` + `\mayhem`in
      musiikillinen viritys
- [ ] `classes/CQRecorder.sc` — master 2ch 24-bit wav, timestamped
      `recordings/cq_YYMMDD_HHMMSS.wav`, ei koskaan ylikirjoita → GUI:n REC
- [ ] Headless-tila varmistettu: instrumentti toimii ilman GUI:ta ja ilman 16n:ää

### Testit
- [ ] `CQ_COMPILE_OK` · `CQ_TESTS_PASSED` · `CQ_SAFETY_PASSED`

### KUUNTELU 6 — M2-hyväksyntä
- [ ] GUI-läpikäynti: meterit, hold-LEDit, spesal-näyttö, matriisigrid, file drop,
      näppäinkartta (`1/2 q/w a/s 3..7 r space p`)
- [ ] `\selfNoise`-preset toistaa no-input digital-dust -workflow'n (M4)
- [ ] M5 kokonaan: flip/skip/dolby GUI:sta
- [ ] REC → tiedosto syntyy, aikaleimattu, ei ylikirjoitusta
- [ ] 30 min sessio kirjoittamatta muuta kuin patch-rivejä → **M1–M8-checklist läpi**
- [ ] Käyttäjän M2-hyväksyntä → milestone valmis

---

## Vaihe 7 — Space, stemit, watchdog, patternit, viimeistely → **v1.0**

### Toteutus
- [ ] `\cqSpace` — 4-linjainen FDN (DelayC, Householder, damping; feedback ≤ 0.98;
      ARCH §4.5), spaceMix/Size/Damp, oletus OFF
- [ ] Stem-ulostulot (`stemOuts`) + stem-äänitys — **identtinen turvaketju joka
      stemiin** (safety-invariantti #1)
- [ ] Watchdog auto-trim: RMS > −6 dBFS 5 s → −6 dB trim + GUI-notice (`autoTrim`)
- [ ] Event type `\cq` — Pbind-sekvensointi diskreeteille aktioille (SPEC §6.4)
- [ ] `README.md` — asennus, includePaths-ohjeet Windowsille, pikastartti
- [ ] Suorituskykytarkistus: koko instrumentti ≤ 25 % / core @ 48 kHz / block 64

### Testit — täysi re-run
- [ ] `CQ_COMPILE_OK` · `CQ_TESTS_PASSED` · `CQ_SAFETY_PASSED` · `CQ_HOLD_OK`
      (A1–A5 kaikki)

### KUUNTELU 7 — v1.0-hyväksyntä
- [ ] M7 abuse-sessio: 30 min kuulokkeilla tahallista väärinkäyttöä —
      **mikään ei koskaan satu**
- [ ] Space: laadukas, oletuksena pois, mix/size/damp toimivat
- [ ] Stemit modulaariin: post-safety coco1/coco2 oikeissa ulostuloissa
- [ ] Stem-äänitys: tiedostot syntyvät configin mukaan
- [ ] Käyttäjän hyväksyntä → **v1.0-tag**
