# CocoquantusSC — ajantasainen kehityssuunnitelma

Päivitetty 2026-07-19. Tämä tiedosto kertoo, mitä tehdään seuraavaksi. Valmiin v1.0-
toteutuksen yksityiskohtainen työpäiväkirja ja alkuperäiset checkboxit on jäädytetty
tiedostoon [`docs/history/PLAN_v1.0.md`](docs/history/PLAN_v1.0.md).

Nykyinen runtime on toimiva v1.0-pohja, mutta riippumaton auditointi varmisti yhden
P0-julkaisun estäjän ja kolme P1-vakausvikaa. Seuraava kehitysvaihe on Archipelago-GUI;
sitä ei nimetä vakaaksi releaseksi ennen alla olevia foundation-portteja.

## Auktoriteettijärjestys

1. [`SPEC.md`](SPEC.md), erityisesti §8, määrää parametrit, DSP-käyttäytymisen ja
   turvallisuuden.
2. [`GUI_SPEC.md`](GUI_SPEC.md) määrää GUI:n rakenteen, eleet, tilat, mitat ja Ink &
   Phosphor -visuaalikielen. GUI-kompromissi hyväksytetään käyttäjällä etukäteen.
3. [`ARCHITECTURE.md`](ARCHITECTURE.md) määrää toteutusrajat, omistuksen ja pure-view /
   yhden kirjoitussuppilon periaatteet.
4. [`audit/VERIFIED_FINDINGS.md`](audit/VERIFIED_FINDINGS.md) on varmennettujen
   audit-havaintojen totuus; [`audit/REMEDIATION_ROADMAP.md`](audit/REMEDIATION_ROADMAP.md)
   ja [`audit/GUI_DEVELOPMENT_PLAN.md`](audit/GUI_DEVELOPMENT_PLAN.md) pilkkovat työn.
5. Tämä tiedosto määrää toteutusjärjestyksen ja ajantasaisen statuksen.

Ristiriidassa turvallisuus voittaa. Epäselvää GUI-käyttäytymistä ei arvata eikä
korvata teknisesti helpommalla ratkaisulla ilman käyttäjän päätöstä.

## Git- ja toimitusmalli

- `main` on vakaa integraatiopohja ja sisältää auditin sekä yhteiset suunnitelmat.
- `archipelago` on uuden GUI:n integraatiohaara. G1–G3 tehdään siihen pieninä,
  testattuina committeina ja pidetään Draft PR:ssä `main`-haaraa vasten.
- Vanhaa GUI:ta ei kopioida rinnakkaiseksi tuotteeksi eikä runtime-feature-flagia
  lisätä. Työ etenee peruutettavina rakenteellisina committeina.
- Erillisiä lyhytikäisiä työhaaroja tarvitaan vain todellisessa rinnakkaistyössä.
- Lähdekoodi, testit ja dokumentit pidetään samassa commitissa silloin, kun ne yhdessä
  muodostavat yhden todennettavan käyttäytymismuutoksen.

## Foundation ennen G1:tä

Toteutusjärjestys on sitova, ellei uusi evidenssi muuta riippuvuuksia.

### F0 — vihreä lähtötilanne ja sopimustestit

- [ ] Aja compile-, unit-, NRT safety- ja hold-integrity-baseline ja tallenna markerit.
- [ ] Lisää hermettiset reproducerit CQ-AUD-001–004:lle ennen niiden korjauksia.
- [ ] Lukitse `CQMatrix`-sopimus testeillä: full read, clampattu batch write, täsmälleen
  yksi notification sekä committed/transient-raja.
- [ ] Toteuta serveritön audition-tilakonetesti jokaiselle `GUI_SPEC.md` §4.1:n
  plant/revert/cancel-polulle.

### F1 — auditin julkaisun estäjät

- [ ] **P0 / CQ-AUD-001:** validoi hardware-output-indeksit globaalisti uniikeiksi ja
  stem/master-joukot erillisiksi ennen serverin käynnistystä.
- [ ] **P1 / CQ-AUD-004:** tee session generation, rollback, todellinen reboot ja
  peruutettava GUI-init eksplisiittiseksi lifecycleksi.
- [ ] **P1 / CQ-AUD-002:** tee recorderista omistettu
  idle/starting/recording/stopping/error-tilakone paikallisella resurssiomistuksella.
- [ ] **P1 / CQ-AUD-003:** toteuta varmennetun löydöksen vaatima deck-load- ja linked
  stereo -korjaus roadmapin rajauksen mukaan.
- [ ] Aja kunkin korjauksen kohdetesti ja koko baseline uudelleen.

### F2 — Archipelagon käyttäytymisperusta

- [ ] Lisää pienin testien vaatima `CQMatrix` full-read/batch-write-API muuttamatta
  nykyistä `patch`- tai snapshot triple -formaattia.
- [ ] Toteuta audition-tilakone ensin ilman uusia piirtoeleitä.
- [ ] Varmista, että GUI pysyy `CQState`/`CQMatrix`-tilan asiakkaana eikä kirjoita
  serverille ohi nykyisten suppiloiden.
- [ ] Ratkaise tämän tiedoston avoimet GUI-spec-saumat ennen niitä käyttävää pintaa.

## Archipelago-portit

### G1 — Day skin

- [ ] Yksi Day/Night-ikkunakuori, pysyvä Master-strip ja Drawer vähintään 1280×720:ssa.
- [ ] Yhteinen Ink kit: normatiivinen paletti, viiva, glyphit ja session-seedattu
  epäsymmetria ilman kuva-assetteja tai icon-fontteja.
- [ ] Dayn 16 source jackia ja 22 destination socketia oikeilla organ skin -koodeilla.
- [ ] Hit-safe cable overlay ja koko yhteinen banana grammar: audition, WYHIWYG plant,
  move, rip, wheel, polarity, solo-light, Esc ja 32 operaation Ctrl+Z.
- [ ] Drawer: Inspector, notification-streamiin perustuva Spellbook ja snapshot/morph
  Memory.
- [ ] `/cq_aura`: 16 kanonista source-magnitudea noin 15 Hz; ambient redraw enintään
  15 Hz ja GUI CPU noin enintään 3 %.
- [ ] `GUI_SPEC.md` §13:n automaatio, kuuntelu ja käyttäjän visuaalinen hyväksyntä.

### G2 — Night map

- [ ] Topology-true Archipelago: viiden castlen oikeansuuntainen ring, vasen/oikea
  Coco-spiraali, roots below ja birds above `GUI_SPEC.md` §16.6:n anatomialla.
- [ ] Loop phase/read-head spark ja käyttäjän vahvistaman payloadin `/cq_charge`.
- [ ] Solo-light, deck-bird recursive drop ja täsmällinen Night toolbar.
- [ ] Ei numeroita, Space-bodya tai dekoratiivista motionia; Master pysyy näkyvänä.
- [ ] `GUI_SPEC.md` §13:n automaatio, kuuntelu ja käyttäjän visuaalinen hyväksyntä.

### G3 — Weather machine

- [ ] Compass: neljä weights-only-matriisimuistia, bilinear left commit, right elastic,
  enintään 60 Hz batch writes ja yksi undo-operaatio.
- [ ] Versioitu, validoitu ja clampattu `presets/compass.cqcompass.scd` autosave/load.
- [ ] Grow/wither 2,5 Hz hold-operaatioina ja Breed täsmälleen §7:n vakioilla.
- [ ] Weather-operaatiot eivät muuta `CQSpec`-parametreja; `patchDepth` säilyy.
- [ ] `GUI_SPEC.md` §13:n automaatio, kuuntelu ja käyttäjän visuaalinen hyväksyntä.

## Avoimet GUI_SPEC-päätökset

Näitä ei toteuteta oman tulkinnan varassa:

- [ ] `/cq_charge`: speksin 11 summaa vs inventaarion 9 gate/trig-kohdetta.
- [ ] `harvest`: nimi undo-luettelossa ilman määriteltyä elettä tai semantiikkaa.
- [ ] `mic root`: varoituksen visuaalinen koti ilman uutta 17. matriisilähdettä.
- [ ] REC: starting/stopping/error-tilojen normatiivinen look.

Päätökset kirjataan ensin `GUI_SPEC.md`:ään ja sen jälkeen tähän sekä GUI-
toteutussuunnitelmaan.

## Jokaisen portin hyväksyntä

Automaatio ajetaan tässä järjestyksessä ja onnistuminen määräytyy viimeisestä
markerista, ei prosessin silmämääräisestä tulosteesta:

1. `tests/compile_check.scd` → `CQ_COMPILE_OK`
2. `tests/run_all.scd` → `CQ_TESTS_PASSED`
3. `tests/nrt_safety.scd` → `CQ_SAFETY_PASSED`
4. DSP-käyttäytymisen muutoksissa `tests/nrt_hold_integrity.scd` → `CQ_HOLD_OK`

Lisäksi relevantit `GUI_SPEC.md` §13:n serverless interaction-testit, manuaalinen
kuuntelu, visuaalinen tarkastus 100/125/150 % skaalauksilla sekä CPU/resource-soak on
hyväksyttävä. G-porttia ei merkitä valmiiksi pelkän toteutuscommitin perusteella.

## Seuraavan toteutussession ensimmäiset askeleet

1. Varmista `archipelago`-haara ja puhdas työpuu.
2. Aja neljän testin baseline muuttamatta lähdekoodia.
3. Lisää ensin CQ-AUD-001–004-reproducerit sekä matrix transaction- ja audition-
   revert-testit; varmista, että ne epäonnistuvat tarkoitetusta syystä.
4. Korjaa CQ-AUD-001 ja aja koko baseline.
5. Etene lifecycle/recorder/deck-perustaan ja vasta sitten `CQMatrix`-API:in.
6. Aloita Ink kit ja ikkunakuori vasta, kun F0–F2:n riippuvuudet ovat vihreitä.

Night-organismeja tai Weather Machinea ei piirretä ennen G1:n behavior foundationia.

## Myöhemmin, ei tämän vaiheen scopea

- Space-body ja Space-estetiikka odottavat erillistä konseptipäätöstä.
- Norns + grid + 16n + crow -portti on v2-visio; alkuperäinen tekninen muistio on
  arkistoidun suunnitelman lopussa.
- Stem-ulostulojen live-kuuntelu vaatii vähintään nelikanavaisen audiointerfacen ja
  säilyy erillisenä laitteistogatena.
