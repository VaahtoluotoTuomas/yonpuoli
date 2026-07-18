# PLAN.md — CocoquantusSC toteutussuunnitelma

Vaiheistettu checklist. Toimii CLAUDE.md:n milestone-checklistinä (TODO.md:n roolissa).
Normatiiviset lähteet: SPEC.md (käyttäytyminen), ARCHITECTURE.md (rakenne).
Milestone-mappaus: **M1 = vaiheet 0–4 · M2 = vaiheet 5–6 · M3 = vaihe 7**.

**Tilanne 2026-07-18: vaiheet 0–6 valmiit ja hyväksytty — M1 + M2 valmiit;
seuraavaksi vaihe 7 → v1.0.** (M1 hyväksytty 2026-07-17; vaihe 5 2026-07-18:
`885cb24` + `38653ab` + gate-korjaus `56ab8a7`. Vaihe 6: toteutus `5ab3255` —
CQGui + CQ.shake + presetit \selfNoise/\quantumDrone/\organMode +
\mayhem-viritys + CQRecorder — gate-korjaukset `81be310`, ja **KUUNTELU 6
hyväksytty kokonaan 2026-07-18**; gaten aikana GUI:hin lisätty
shake-amplitudinuppi, master-stripin depth-nuppi ja coco-strippien
slots-rivi.) Markerit vihreät: A1 + A2 (3080 assertiota) + A3 (mayhem-peak
0.89 = limitterin katto) + A4/A5 (korrelaatio 1.0, drift 0.0 dB) +
GUI-savutesti (dev-skripti). Post-v1.0-suunta (norns-portti) kirjattu tämän
tiedoston loppuun — se ohjaa vaiheen 7 rajapintavalintoja.

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
- [x] Kenttä-äänite + `CQ.patchPreset(\classic)` oletuksilla → heti "Cocoquantus-like":
      huojuva, aliasoiva, elävä, koskematta mihinkään (M6) — spaf-syvyys
      kuuntelukalibroitu 0.12:een (ks. kirjaukset)
- [x] Inverso-banaani → ducking kuuluu (toteutettu coco1:een; jaettu 2a
      spaf-välitodistus + 2b fbVca-duck loop-kierron yli, ks. kirjaukset)
- [x] `patchDepth` 1→0 kesyttää kaiken pehmeästi (ja 0→1 palauttaa)
- [x] ~20 min code-only-sessio (`live.scd`-reseptit R1–R6; R5:n oktaavikerros
      ja R6:n rajattu kasautuminen todettu odotetuiksi)
- [x] Käyttäjän M1-hyväksyntä → **milestone valmis 2026-07-17**

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
- KUUNTELU 4 -havainto (2026-07-17, mitattu 120 s NRT:llä): flip on SPECin
  mukainen **taso-gate** (Schmidt >0.5 on / <0.3 off) — infra-castlen cas
  pitää suunnan käännettynä kymmeniä sekunteja (chaos 0.01: 2 vaihtoa/120 s,
  43 % ajasta reverse), ja ääri-chaosissa castlen oma kello voi jäätyä
  lähes nollaan jolloin cas lakkaa päivittymästä (chaos 1.0: gate jumissa,
  90 %/120 s) — hyperkaaoksen "silences"-päätä, autenttista; spesal-askel
  herättää verkon. Tiheämpi flip-tanssi: balcium-range tai tri-lähde;
  painon suuruus säätää kuinka usein kiinteä kynnys ylittyy.
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
- [x] `classes/CQSnapshots.sc` — save/recall/morph (ARCH §7:n tiedostoformaatti,
      version-gate, clamp latauksessa, **micGain ei koskaan auto-recallina > 0**,
      SPEC §7)
- [x] `classes/CQMidi16n.sc` — CC 32–47 ch 1 configista, 30 ms smoothing,
      `\jump`/`\pickup`-takeover, hot-plug (`CQ.midiRescan`), `MIDIClient.init`
      guarded, `MIDIdef`-avain `\cq_16n`; kaikki kirjoitukset CQStaten kautta
- [x] Deck B + kansionavigointi (next/prev/random/loadFolder) + linked stereo
      (`CQ.loadStereo`: L→A, R→B)
- [x] Mic-polku (`micGain`, oletus 0) + varoituslogiikka (headless-osa:
      varoitus kerran per 0 → live -siirtymä, mistä tahansa entry pointista)
- [x] `presets/example.cqsnap.scd` — harva käsinkirjoitettu esimerkki
- [x] `CQSnapshotTest` — roundtrip-yhtäsuuruus, malformed-tiedoston hylkäys,
      clamp latauksessa, mic/panic-politiikka, morph päättyy kohteeseen;
      lisäksi `CQMidi16nTest` — CC→param-mappaus, linked-slotit, takeover-
      logiikka ilman MIDI-laitetta

### Testit
- [x] `CQ_COMPILE_OK` · `CQ_TESTS_PASSED` (1728) · `CQ_SAFETY_PASSED`
      (mayhem-peak 0.8807) · `CQ_HOLD_OK` varmuudeksi (DSP ennallaan;
      korrelaatio 1.0, drift 0.0 dB) (2026-07-17)

### KUUNTELU/KÄYTTÖ 5 — 16n & muisti
- [x] Kaikki 16 faderia oikeissa parametreissa, liike pehmeää (≤ 20 ms tuntuma) (M8)
      (2026-07-18; F2:n repitch-transientti todettu speksin mukaiseksi — RAM-kello,
      ei pitch-nuppi)
- [x] 16n irti ja takaisin kesken session → toipuu (M8) (2026-07-18, `CQ.midiRescan`
      toimi, ei virheryöppyä eikä katkosta)
- [x] `CQ.snap(\a)` → säädä → `CQ.recall(\a)` → tila palaa korvakuulolta;
      `CQ.morph(\a, \b, 30)` liukuu musikaalisesti (2026-07-18; recallin
      hetkellinen repitch = oletettu RAM-kellokäytös, mic-politiikka todennettu)
- [x] Jump vs pickup -takeover käyttäytyy odotetusti recallin jälkeen
      (2026-07-18; \jump jää oletukseksi käyttäjän valinnasta)
- [x] Deck B + kansionavigointi + linked stereo toimivat; mic-varoitus näkyy
      (2026-07-18; gatessa löytyi navigointibugi — korjattu `56ab8a7`,
      regressiotestit CQDeckNavTest — jonka jälkeen next/prev/random/loadFolder
      ok. Stereo: L→A/R→B todennettu, monon fallback + varoitus ok)
- [x] Käyttäjän OK → vaihe hyväksytty (2026-07-18)

### Kirjatut toteutuspäätökset (vaihe 5)
- **micGain-politiikka** (SPEC §7, päivitetty sinne): `save` tallettaa micGainin
  nollana ellei `includeMic: true` (eksplisiittinen opt-in) — auto-tallennettu
  snapshot ei voi koskaan avata mikkiä recallissa. Recall soveltaa tiedoston
  arvon clampattuna ja varoittaa kovaan ääneen jos se avaa mikin (käsin
  editoitu tiedosto = käyttäjän eksplisiittinen valinta). **monitorMute ei
  koskaan snapshotiin** kumpaankaan suuntaan: recall ei saa kumota panicia
  (safety #3). Lisäksi mistä tahansa entry pointista tuleva micGain-nosto
  varoittaa kerran per 0 → live -siirtymä (SimpleController CQStateen).
- **Spesal-tilat kielipuolelle**: `OSCdef(\cq_spesal)` cachettaa serverin
  SendReply-raportit (`Cocoquantus.spesalStates`); CQSnapshots saa spesalin
  get/set-**funktioina** konstruktorissa — luokka ei tunne serveriä eikä
  facadea, joten se testataan graph = nil (sama kuvio kuin CQState/CQMatrix).
  Sama cache syöttää vaiheen 6 LED-näytön.
- **Morph**: jatkuvat parametrit interpoloidaan **normalisoidussa (curve-)
  avaruudessa** (exp-parametrit kuten speed liukuvat musikaalisesti, eivät
  lineaarisesti Hz:einä), painot lineaarisesti; toggle/enum/spesal vaihtuvat
  puolivälissä; viimeinen askel on täsmälleen `prApply(kohde)` → morphin
  loppu ≡ `recall(kohde)` mic-politiikkaa myöten. micGain ei interpoloidu.
  Harvan tiedoston puuttuva parametri interpoloituu nykyarvosta/-arvoon.
- **Harvat snapshot-tiedostot sallittuja** (käsin kirjoitetut "osapresetit"):
  listaamattomat parametrit säilyttävät nykyarvonsa. Rakenteellisesti
  rikkinäinen tiedosto (ei Event / ei int-versiota / versio > 1 / params ei
  Event) hylätään varoituksella tilaan koskematta; ruma matriisirivi tai
  spesal-lista pudotetaan riveittäin. `.load`-ajon runtime-virhe napataan.
- **CQMidi16n**: `handleCC` on julkinen → takeover-logiikka testataan ilman
  MIDI-laitetta. Pickup: mikä tahansa ulkoinen kirjoitus (recall, koodi,
  preset) disengagee ko. faderin (`writing`-lippu erottaa omat kirjoitukset);
  engage kun fader ylittää nykyarvon tai osuu ±1 CC-askeleen sisään.
  Linked-slotit seuraavat parin ensimmäistä parametria. Smoothing = CQSpecin
  serveripuolen lagit (≥ 30 ms), ei kielipuolen suodatusta.
- **Hot-plug**: portmidi/Windows ei tunnista laitteita kesken ajon →
  `CQ.midiRescan` (disposeClient + init + connectAll; MIDIdefit säilyvät
  re-initin yli). 16n:n puuttuminen bootissa on notice, ei virhe; MIDI:n
  täysi puuttuminen degradoituu varoitukseksi (try-suojattu init).
- **Mitattu KÄYTTÖ 5:ssä (2026-07-17, MIDIFunc.trace)**: atovprojectin 16n
  lähettää **joka faderin omalla MIDI-kanavallaan** (F1 = ch 1 … F16 = ch 16),
  CC:t silti 32–47 — alkuperäinen `chan: 0` -suodatin päästi vain F1:n läpi.
  Korjaus: sidonta CC-numerolla, `sixteenN.chan` on nyt **nil = mikä tahansa
  kanava (oletus)**; kokonaisluku 0–15 lukitsee yksikanavaisen firmiksen.
  SPEC §6.1/§7, CLAUDE.md-ympäristörivi ja configit päivitetty.
- **Deck-navigointi**: jokainen load skannaa ladatun tiedoston kansion
  (wav/aif/aiff/aifc/flac), next/prev/random kulkevat sitä; ennen
  ensimmäistä latausta fallback config-`samples/`-kansioon. `loadStereo`
  lataa kanavan 0 → deck A ja 1 → deck B; monotiedostolla SoundFile-
  tarkistus pudottaa kanavan nollaan varoituksella (molemmat soittavat L:ää).

---

## Vaihe 6 — GUI, shake, loput presetit, perusäänitys → **M2 valmis**

### Toteutus
- [x] `classes/CQGui.sc` — SPEC §6.2: neljä kolumnia + master-strip, banaaniväri-
      koodi, meterit/LEDit OSCdefeillä, matriisigrid, file drop, näppäinkartta;
      puhdas view (dependants, `{ }.defer`, ei server-pollausta). Jaettu kokorajan
      takia: `CQGuiWidgets.sc` (param-sidottu widget-tehdas + väripaletti),
      `CQGuiPanels.sc` (kolumnit + master-strip), `CQGuiMatrix.sc` (grid)
- [x] `CQ.shake(amount, seed)` — vain painot + spesal, ei koskaan level/feedback;
      determinismitesti seedillä CQMatrixTestiin (+ rajat, harvuus, roska-amount)
- [x] Presetit: `\selfNoise`, `\quantumDrone`, `\organMode` + `\mayhem`in
      musiikillinen viritys (sin-hash-painot; pysyy tiheänä = A3-data ennallaan)
- [x] `classes/CQRecorder.sc` — master 2ch 24-bit wav, timestamped
      `recordings/cq_YYMMDD_HHMMSS.wav`, ei koskaan ylikirjoita → GUI:n REC;
      `CQRecorderTest` (aikaleimaformaatti + collision-suffiksi)
- [x] Headless-tila varmistettu: boot avaa GUIn vain jos `headless: false`;
      kaikki toiminnot facade-API:ssa ilman GUIta/16n:ää (testiajot ajavat
      koko kirjaston GUIttä; 16n-degradaatio todettu M8:ssa)

### Testit
- [x] `CQ_COMPILE_OK` · `CQ_TESTS_PASSED` (3080) · `CQ_SAFETY_PASSED`
      (mayhem-peak 0.89) · `CQ_HOLD_OK` varmuudeksi (meteri-tapit defeissä;
      korrelaatio 1.0, drift 0.0 dB) (2026-07-18)

### KUUNTELU 6 — M2-hyväksyntä
- [x] GUI-läpikäynti: meterit, hold-LEDit, spesal-näyttö, matriisigrid, file drop,
      näppäinkartta (`1/2 q/w a/s 3..7 r space p`) (2026-07-18: hyväksytty;
      matkalla korjattu file-drop- ja näppäimistöfokus-bugit, ks. kirjaukset)
- [x] `\selfNoise`-preset toistaa no-input digital-dust -workflow'n (M4)
      (2026-07-18: dust-purskeet, wobble, ristisyötön cas-portit ja
      hold-lukitus + repitch todettu kuuntelussa ok)
- [x] M5 kokonaan: flip/skip/dolby GUI:sta (2026-07-18: duck kuultavissa
      joskin hienovarainen, gate ei erotu hiljaisessa huoneessa = odotettu
      tulos; hold/flip/skip ok. Checklistin 6.2-asetelma täsmennetty: deck
      level 0:aan testin ajaksi — duck-avain kuuntelee koko input-mixiä,
      jatkuva deck-tekstuuri peittää artikulaation; dry-monitorointi ~0.4
      jotta puheen kuulee heti; duckin syvyys ~ in-meterin taso)
- [x] REC → tiedosto syntyy, aikaleimattu, ei ylikirjoitusta (2026-07-18;
      myös headless-boot todennettu — matkalla löytyi käyttäjän config.scd:hen
      eksynyt CQ.boot-rivi, joka kaatoi latauksen: config on puhdas
      data-lauseke, boot ajetaan boot.scd:stä)
- [x] 30 min sessio kirjoittamatta muuta kuin patch-rivejä → **M1–M8-checklist
      läpi** (2026-07-18)
- [x] Käyttäjän M2-hyväksyntä → **milestone valmis 2026-07-18**

### Kirjatut toteutuspäätökset (vaihe 6)
- **Shake on harva heitto**, ei tiheä: jokainen solu patchataan tn. 0.1:llä
  (~35 banaania) painolla ±amount — tiheä satunnaisuus on jo \mayhemin rooli.
  `CQMatrix.shake` palauttaa 5 spesal-arvontaa (luokka ei tunne serverin
  laskureita); `Cocoquantus.shake` soveltaa ne `spesalSet`-reittiä. Seedattu
  ajo tallettaa ja palauttaa kielisäikeen rand-streamin — ei sivuvaikutuksia.
- **\mayhem-viritys säilyttää tiheyden**: painot sin-hashista
  (`0.55·sin(5.7·si + 2.3·di)`, pyöristys 0.01, nollat → 0.05), koska sama
  data ajaa A3-turvarenderin — vaihteleva magnitudi rikkoo vaiheen 4
  tasaruudukon kuolleen symmetrian (gatet/trigit eivät enää laukea tahdissa),
  mutta joka solu pysyy patchattuna (CQMatrixTest vartioi).
- **Preset-nupit**: \selfNoise sulkee pre/deck-slotit ja avaa dustin/fb:n
  (M4-workflow); \quantumDrone nostaa 4 castlea audiolle ja ajaa ne cocojen
  läpi eri kelloilla; \organMode virittää coco-kellot 3:2-suhteeseen (urku-
  kvintti) — kaikki KUUNTELU 4:n "preset = painot + nupit" -opin mukaisesti.
- **CQRecorder** lukee hardware-out-väylät `gMaster`-hännässä → tiedosto on
  täsmälleen post-safety-signaali (invariantti #1 rakenteesta). Synth/buffer-
  churn luovutetaan CQGraphille nauhoituksen ajaksi ja rekisteröidään nil:ksi
  stopissa (IdentityDictionaryn nil-put poistaa avaimen) — teardown-omistus
  pysyy yhdessä paikassa. `CQ.free` sulkee tiedoston ennen graafin purkua.
- **Meteri-/LED-raportit defeissä**: '/cq_cocometers' (in/out-peakit,
  replyID 1/2 erottaa instanssit), '/cq_castle' (trien hetkisarvot 15 Hz —
  infra näkyy sykkeenä, audio välkkeenä), '/cq_gr' (limitterivaiheen reduktio
  dB:nä, mitattu ennen fade/mute-kerrointa ettei panic näytä valereduktiota).
  Lähetykset ovat ehdottomia (~20 Hz control-rate, kuulumattomia NRT:ssäkin).
- **GUI:n OSCdefit ovat GUIn omia** (`\cq_gui_*`) — Cocoquantuksen
  `\cq_spesal`-cache säilyy koskemattomana; molemmat kuuntelevat samaa
  osoitetta. Ikkunan sulkeminen (myös käyttäjän X) ajaa idempotentin
  `prCleanup`in: dependantit, OSCdefit ja REC-tick vapautuvat aina.
- **Morph GUI:ssa**: A/B-snapshot-popupit + kesto + MORPH-nappi (SPEC §6.2
  päivitetty; "morph-slider"-scrubbaus vaatisi CQSnapshotsiin
  interpolaatio-API:n — lisätään jos gate kaipaa).
- **GUI-savutesti** on dev-skripti (scratchpad, ei repo-suitea koska avaa
  oikean ikkunan): rakentaa CQGuin serverittömillä CQState/CQMatrix-
  instansseilla, ajaa koko parametripyyhkäisyn updatereiden läpi, mic-
  varoituksen, shake-redraw'n ja freen. Ajettu vihreänä 2026-07-18.
- GUI:n jatkuvat säätimet sitoutuvat parametreihin CQSpecin map/unmapilla
  (ei literaalirangeja); diskreetit aktiot (hold/flip/skip/spesal/shake/rec/
  panic/presetit) kutsuvat Cocoquantus-facadea, joka kirjoittaa suppiloiden
  läpi — GUI ei koskaan puhu serverille suoraan.
- KUUNTELU 6 -löydös (2026-07-18, CC-lokilla todennettu): 16n-faderin hidas
  liike voi laukaista **laitteen itsensä lähettämän glitch-ekskursion**
  (F16: 112 → 72 → 113 ~150 ms:ssä, eventit 1 ms välein, sulava ramppi =
  firmiksen suodattama ADC-kontaktihäiriö; masterVolilla ~19 dB dippi).
  Ei softabugi: funnel seuraa syötettä uskollisesti ja GUI:n "heittely" oli
  saman datan piirtoa. Oire korostuu hitaassa liikkeessä ja lievenee
  käytössä → hapettunut liukurata todennäköisin. Softasuodatinta EI lisätty:
  glitch on paikallisesti sileä eli erottamaton aidosta nopeasta eleestä
  ilman ≥100 ms latenssia; valinnainen slew-rajoitin config-lippuna
  harkitaan vain jos laitepuolen korjaus ei riitä. Erottelutesti tukee
  hapettumisdiagnoosia: myös F14 (käyttämätön) dippailee, F2–F6 (paljon
  käytetyt) eivät — laite seissyt ~vuoden kaapissa, käyttö puhdistaa radan.
  **AVOIN (myöhempään tarkasteluun): 16n:n syvätesti** — fadereiden ajelu/
  puhdistus, USB-portti/kaapeli-varmistus, kaikkien 16 faderin CC-lokiajo.
  Sivuhavainto: laite lähetti nyt kaiken MIDI-kanavalla 1, vaikka vaihe 5
  mittasi per-fader-kanavat eikä käyttäjä ole konfiguroinut/päivittänyt
  laitetta välissä — ristiriita selvittämättä (tarkista syvätestissä
  MIDIFunc.tracella); harmiton, sidonta on CC-numerolla.
- KUUNTELU 6 -bugi (2026-07-18): file drop toimi vain satunnaisesti ("kerran,
  sitten ei") — juurisyy QView'n drag-mallissa: `dragEnterEvent` hyväksyy
  dragin aina, joten kursorin alla oleva lapsiwidget nappaa sen ja hylkää
  dropin (`defaultCanReceiveDrag ^false`), eikä Qt propagoi rivi-View'lle;
  droppikelpoisia olivat vain rivin ~2 px marginaalipikselit. Korjaus:
  drop-handlerit sidotaan rekursiivisesti deck-rivin kaikkiin jälkeläisiin
  (koko laatikko on kohde). Samalla lisätty visuaalinen kuittaus: deck-rivin
  tiedostonimi-label, jota ajaa `CQDeck.prSwap`in `changed(\file)` →
  CQGui:n SimpleControllerit (katetaan kaikki latauspolut: drop/nappi/
  navigointi/koodi; nil-turvallinen serverless-buildissa). SPEC §6.2 ja
  manual_checklist 6.1 päivitetty (toistuvat pudotukset molempiin deckeihin).
  Käyttäjä vahvisti 2026-07-18: pudotus toimii nyt.
- KUUNTELU 6 -bugi (2026-07-18): hiirellä klikattu nappi (esim. REC) jäi
  Qt-näppäimistöfokukseen → space aktivoi fokusoidun napin (Qt:n oletus)
  eikä panicia, TAB kierrätti fokusta; ikkunan `keyDownAction` ei saanut
  näppäintä, koska fokusoitu widget kulutti sen. Korjaus: paneeli on
  hiiripelkkä — `CQGui.prDefocus` kävelee ikkunan view-puun rekursiivisesti
  ja asettaa `canFocus = false` kaikkeen paitsi tekstinsyöttöön (TextField/
  NumberBox), jotka vapauttavat fokuksen Enterillä (`focus(false)`
  action-käsittelijässä: snap-nimikenttä, morph-durBox, bits-intBox).
  Näppäinkartta (space panic mukaan lukien) toimii nyt klikkailun jälkeenkin.
  Jatkohavainto (käyttäjä): bootissa Qt antoi alkufokuksen tab-ketjun
  ensimmäiselle fokusoituvalle widgetille = coco1:n bits-boksille, jolloin
  näppäimet eivät toimineet heti. Korjaus: tekstinsyöttö saa vain
  Qt::ClickFocus-politiikan (`setProperty(\focusPolicy, 2)`) — kirjoitus
  onnistuu klikkauksen jälkeen, mutta widget ei ole tab-ketjussa eikä saa
  alkufokusta. SPEC §6.2 ja manual_checklist 6.1 päivitetty (fokustesti).
- KUUNTELU 6 -löydös (2026-07-18, shake-kuuluvuus): käyttäjä ei kuullut
  heitosta suurta eroa — mekaaninen selitys, ei bugi. Shake heittää vain
  painot + spesalit (SPEC §5.5), joten "preset = painot + nupit" -oppi pätee
  heittoonkin: oletusnuppitilassa spaf-banaanit ovat kuuroja (`cocoNSpaf`
  oletus 0), quantussya ei kuule suoraan (`cocoNSlotQ` oletus 0 → myös
  spesal-arvonta jää epäsuoraksi), ja gate/trig-rivit (flip/skip/
  castleN_spesal = 11/22 destistä) eivät voi laueta yksittäisestä banaanista
  oletusamplitudilla 0.3: Schmitt-kynnys on +0.5 ja lähteet huiputtavat
  ~±1 → summa ≤ 0.3. Checklist 6.4:ään kirjattu valmisteluohje (deck
  soimaan, spaf ~0.4, slotQ ~0.8, amplitudi 0.6–0.8 → trig-rivit heräävät).
  Oletusamplitudi pidetään SPEC:n 0.3:ssa — käyttäjä vahvisti samana
  päivänä: valmistellulla pöydällä heitot heräsivät henkiin ("alkaa tulla
  outoa kamaa"), oletusta ei muuteta; 6.3 ja 6.4 kuitattu.
- GUI-lisäykset kesken gaten (2026-07-18, käyttäjän pyyntö): shake-
  amplitudinuppi SHAKE-napin viereen (GUI-lokaali `CQGui.shakeAmount`,
  oletus 0.3 = facade-default; ei CQSpec-parametri koska kyse on aktion
  argumentista — clamp asuu `CQMatrix.shake`ssa; p-näppäin jakaa saman
  arvon) ja `patchDepth`-nuppi ("depth") master-strippiin vol/dry/space-
  rivin jatkoksi (CQSpec-sidottu → 16n F15 ja GUI-nuppi seuraavat
  toisiaan). Lisäksi coco-strippiin slots-rivi (pre/deck/q/x-minisliders =
  `cocoNSlotPre/Deck/Q/X`, CQSpec-sidotut) — sisäänottomikseri oli aiemmin
  vain koodi/preset-reitti, mikä teki "valmistele pöytä" -workflow'sta
  GUI:ssa mahdottoman. SPEC §6.2 päivitetty.

---

## Vaihe 7 — Space, stemit, watchdog, patternit, viimeistely → **v1.0**

### Toteutus
- [ ] `\cqSpace` — 4-linjainen FDN (DelayC, Householder, damping; feedback ≤ 0.98;
      ARCH §4.5), spaceMix/Size/Damp, oletus OFF
- [ ] Stem-ulostulot (`stemOuts`) + stem-äänitys — **identtinen turvaketju joka
      stemiin** (safety-invariantti #1)
- [ ] Watchdog auto-trim: RMS > −6 dBFS 5 s → −6 dB trim + GUI-notice (`autoTrim`)
- [ ] Event type `\cq` — Pbind-sekvensointi diskreeteille aktioille (SPEC §6.4)
- [x] `README.md` — asennus, includePaths-ohjeet Windowsille, pikastartti
      (aikaistettu 2026-07-18: projekti jaettiin testaajalle; suomeksi —
      viilataan/käännetään v1.0-viimeistelyssä jos tarpeen)
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

---

## Post-v1.0 — v2-visio: norns-portti (sovittu 2026-07-17)

Tavoiteympäristö: **norns + grid + 16n + crow-synkattu modulaari** (käyttäjällä
on kaikki paitsi arc). Ei kuulu v1.0:n scopeen, mutta ohjaa valintoja jo nyt:

- Nornsin crone-enginet ovat sclangia → SynthDefit siirtyvät lähes sellaisinaan
  (vanilla-UGens-only pätee jo). scsynth ajaa CM3+:lla yhdellä ytimellä →
  nykyinen graafi on rajatapaus; diettivivut (mikään ei ole identiteetti-
  kriittinen): harva matriisi (rebuild patchin muuttuessa — tiheä 22×16
  ar-summaus kertoo enimmäkseen nollia), Lag-pankit → yksi Lag per desti,
  block 128/256, deckit softcutiin (cocoiksi softcut EI käy: ei kierrätyksen
  sisäistä clip+kvantisointia), FDN pois (nornsin oma reverb).
- **Spike v1.0:n jälkeen** (1–2 pv): coco-pari + quantussy CroneEnginenä,
  mitataan CPU. Fallback ilman DSP-porttausta: norns pelkkänä kontrolli-
  keskuksena (grid + crow + enkooderit → OSC → läppärin SC) — sama soittopinta.
- Crow toimii jo läppäriltä (SerialPort, druid-protokolla): 2 CV-sisääntuloa
  "CV-fadereina" CQState-suppilon läpi, 4 ulostuloa infra/balcium-castleille
  (audio-rate ei kulje sarjalinkin yli) — mahdollinen pieni lisäluokka ennen
  norns-porttia.
- **Vaiheissa 5–7 vältettävä**: laptop-only-oletukset ytimessä; OSC-nimiavaruudet
  ja kontrolliabstraktiot muotoillaan niin, että ne sopivat myös norns/Lua-
  kerrokselle ja OSC-hub-fallbackille. SPEC.md pysyy porttauksen totuutena.
- Kontrolleripohdinnan muut suunnat (MPE-pinta "elävänä banaanipöytänä",
  Midi Fighter Twister 16n:n rinnalle nappi/valokerrokseksi — täydentää, ei
  korvaa) kirjattu muistiin; eivät vaadi arkkitehtuurimuutoksia.
