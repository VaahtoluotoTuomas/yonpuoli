# CocoquantusSC

SuperCollider-emulaatio Ciat-Lonbarden **Cocoquantus 2** -soittimesta: kaksi
8-bittistä coco-delay/looperia, viiden castlen Quantussy-kaaosverkko ja
banaanipatchausta vastaava 16×22-reititysmatriisi. Ohjaus kolmena kerroksena:
koodi (`CQ`-facade), GUI ja 16n-faderbank — kaikki valinnaisia, kaikki
kirjoittavat samaan tilaan.

**Tilanne: 🎉 v1.0 (2026-07-19).** M1 (ydinääni), M2 (GUI, 16n, snapshotit,
shake, recorder) ja M3 (space-kaiku, stemit, watchdog, `\cq`-patternit) valmiit ja
kuunneltu. CPU raskaimmillaan ~20–23 % (≤ 25 %). Ainoa live-todentamaton on
stem-ulostulot/-äänitys, jotka vaativat ≥4-kanavaisen äänikortin (toteutus + NRT-turva
valmiit; live-kuuntelu v1.0.1:ssä). Työn tila ja checkboxit: `PLAN.md`.

## Vaatimukset

- Windows (testattu; muut alustat todennäköisesti ok, ei testattu)
- [SuperCollider](https://supercollider.github.io/) **≥ 3.13** (IDE + sclang + scsynth)
- Kuulokkeet tai audiointerface
- Valinnaiset: atovproject **16n** faderbank (soitin toimii täysin ilmankin),
  [Claude Code](https://claude.com/claude-code) testien ajamiseen

## Asennus (uusi kone)

1. **Kloonaa repo** ja asenna SuperCollider.

2. **Kopioi `config.example.scd` → `config.scd`** (gitignored, konekohtainen)
   ja aseta äänilaite. Laitenimet saat SC:ssä komennolla
   `ServerOptions.devices`. Jos mikki ja kuulokkeet ovat eri laitteita
   (tyypillinen läppäri), käytä paria — molemmat samasta host-API:sta:

   ```supercollider
   device: ["Windows WASAPI : Mikrofoni (...)", "Windows WASAPI : Kaiuttimet (...)"],
   ```

3. **Lisää luokat include-polkuihin**: SC IDE → *Edit → Preferences →
   Interpreter → Include paths* → lisää `<repo>/classes` **ja**
   `<repo>/tests/classes`, sitten **Ctrl+Shift+L** (recompile class library).
   Tarkista: `CQSpec.specs.size` → pitää palauttaa `79`.

4. **Laita pari wav-tiedostoa `samples/`-kansioon** (gitignored) — kenttä-
   äänitteet toimivat parhaiten. Suhteelliset polut (`CQ.deckA.load("nimi.wav")`)
   etsitään tästä kansiosta.

5. **Boottaa**: avaa `boot.scd` ja evaluoi (tai aja `CQ.boot`). Boot on
   **täysin hiljainen ~2 s ja feidaa sitten sisään** — se on tarkoitus.
   GUI-ikkuna aukeaa itsekseen (`headless: true` configissa estää).

## Pikastartti

```supercollider
CQ.boot;
CQ.deckA.load("oma_aanite.wav");    // HUOM: Windows-polut kauttaviivoilla!
CQ.set(\masterDry, 0.3);            // kuule myös lähde
CQ.hold(1, 0);                      // coco1 record-tilaan -> deck kiertää looppiin
CQ.set(\coco1Fb, 1.0);              // looper; 0.9 = kaiut, 1.15 = crunch
CQ.set(\coco1Speed, 12000);         // tape-repitch oktaavi alas
CQ.patchPreset(\classic);           // banaanit: tri->spaf-huojunta + FM-rinki
CQ.shake;                           // satunnainen banaanikourallinen
CQ.rec; CQ.recStop;                 // recordings/cq_YYMMDD_HHMMSS.wav
CQ.panic;                           // hätämykistys (GUI: space) -> CQ.resume
```

Näppäimet GUI-ikkunassa: `1/2` hold · `q/w` flip · `a/s` skip · `3..7` spesal ·
`r` rec · `space` panic · `p` shake. Lisää reseptejä: `live.scd`.

## Testaus

**Claude Codella** (suositus): avaa terminaali repokansiossa, aja `claude` ja
sano *"lue PLAN.md ja jatka"*. Ensimmäisessä sessiossa Claude kysyy
SuperColliderin asennuspolun ja generoi kehitysconfit itse; sen jälkeen se
ajaa headless-testit ja ohjaa kuuntelutestit askel kerrallaan.

**Käsin** (headless, ei tarvitse äänilaitetta; `<conf>` =
`tests/sclang_conf_dev.yaml`, jonka Claude/sinä generoit — pelkät
include-polut repon `classes/`- ja `tests/classes/`-kansioihin):

```
sclang -l <conf> -i none tests/compile_check.scd      -> CQ_COMPILE_OK
sclang -l <conf> -i none tests/run_all.scd            -> CQ_TESTS_PASSED
sclang -l <conf> -i none tests/nrt_safety.scd         -> CQ_SAFETY_PASSED (~2 min)
sclang -l <conf> -i none tests/nrt_hold_integrity.scd -> CQ_HOLD_OK
```

**Korvilla**: `tests/manual_checklist.md` — kopioitavat lohkot, aja rivi
kerrallaan. KUUNTELU-gatet 1–6 (M1–M8) hyväksytty 2026-07-18; seuraava
kuuntelu on vaiheen 7 **KUUNTELU 7** (v1.0-hyväksyntä).

## Hyvä tietää

- **Turvaketju on kiinteä**: LeakDC → tanh → limitteri (−1 dBFS) → NaN-squelch
  joka ulostulossa, ja coco-kierrätys clipataan ennen kvantisointia — feedback
  yli unityn on soittotekniikka, ei vaara. Silti: älä kuuntele kuulokkeilla
  täysillä ensimmäistä boottia.
- **Mikki on oletuksena kiinni** (`micGain` 0, snapshotit eivät koskaan avaa
  sitä itsekseen). GUI näyttää punaisen varoituksen kun mikki on auki.
- sclang-merkkijonoissa kenoviiva on escape → **Windows-polut kauttaviivoilla**
  (`"C:/Users/..."`).
- 16n kytketään ennen boottia; jos irrotat/kytket kesken session:
  `CQ.midiRescan` (Windows/portmidi ei hot-plugaa itsekseen).
- Cmd-. (Ctrl-.) tappaa palvelingraafin — se on turvallista (hiljaisuus),
  mutta jatka sen jälkeen `CQ.reboot`illa.

## Dokumentit

| Tiedosto | Rooli |
|---|---|
| `SPEC.md` | Käyttäytyminen, parametrit, turvasäännöt (normatiivinen) |
| `ARCHITECTURE.md` | Koodin rakenne, väylät, testiarkkitehtuuri (normatiivinen) |
| `PLAN.md` | Vaiheistettu checklist — **työn ajantasainen tila** |
| `CLAUDE.md` | Claude Code -työohjeet (testikomennot, invariantit) |
| `tests/manual_checklist.md` | Kuuntelutestit (M1–M8), suomeksi |
