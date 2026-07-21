# CocoquantusSC — ajantasainen kehityssuunnitelma

Päivitetty 2026-07-20. Tämä tiedosto kertoo, mitä tehdään seuraavaksi. Valmiin v1.0-
toteutuksen yksityiskohtainen työpäiväkirja ja alkuperäiset checkboxit on jäädytetty
tiedostoon [`docs/history/PLAN_v1.0.md`](docs/history/PLAN_v1.0.md).

Nykyinen runtime on toimiva v1.0-pohja, mutta riippumaton auditointi varmisti yhden
P0-julkaisun estäjän ja kolme P1-vakausvikaa. Seuraava kehitysvaihe on Archipelago-GUI;
sitä ei nimetä vakaaksi releaseksi ennen alla olevia foundation-portteja.

**Aktiivinen työhaara:** `archipelago`. GUI_SPECin neljä aiempaa määrittelysaumaa
ratkaistiin käyttäjän päätöksellä 2026-07-19; avoimia GUI-käyttäytymispäätöksiä ei ole.

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

- [x] Aja compile-, unit-, NRT safety- ja hold-integrity-baseline ja tallenna markerit.
- [x] Lisää hermettiset reproducerit CQ-AUD-001–004:lle ennen niiden korjauksia.
- [x] Lukitse `CQMatrix`-sopimus testeillä: full read, clampattu batch write, täsmälleen
  yksi notification sekä committed/transient-raja.
- [x] Toteuta serveritön audition-tilakonetesti jokaiselle `GUI_SPEC.md` §4.1:n
  plant/revert/cancel-polulle.

Baseline 2026-07-19 ennen toteutusmuutoksia: `CQ_COMPILE_OK`, 3 082 unit-passia / 0
failurea ja `CQ_TESTS_PASSED`, `CQ_SAFETY_PASSED`, `CQ_HOLD_OK`.

### F1 — auditin julkaisun estäjät

- [x] **P0 / CQ-AUD-001:** validoi hardware-output-indeksit globaalisti uniikeiksi ja
  stem/master-joukot erillisiksi ennen serverin käynnistystä.
- [x] **P1 / CQ-AUD-003–004:** tee todellinen server-reboot, session generation,
  rollback ja peruutettava GUI-init eksplisiittiseksi lifecycleksi.
- [x] **P1 / CQ-AUD-002:** tee recorderista omistettu
  idle/starting/recording/stopping/error-tilakone paikallisella resurssiomistuksella.
- [x] **Roadmap 3.1 / CQ-AUD-005 ja -008:** toteuta varmennettujen löydösten vaatima
  deck-load-transaction ja linked stereo -korjaus roadmapin rajauksen mukaan.
- [x] Aja kunkin korjauksen kohdetesti ja koko baseline uudelleen.

### F2 — Archipelagon käyttäytymisperusta

- [x] Lisää pienin testien vaatima `CQMatrix` full-read/batch-write-API muuttamatta
  nykyistä `patch`- tai snapshot triple -formaattia.
- [x] Toteuta audition-tilakone ensin ilman uusia piirtoeleitä.
- [x] Varmista, että GUI pysyy `CQState`/`CQMatrix`-tilan asiakkaana eikä kirjoita
  serverille ohi nykyisten suppiloiden.
- [x] Ratkaise tämän tiedoston avoimet GUI-spec-saumat ennen niitä käyttävää pintaa.

Foundation-portti 2026-07-19: `CQ_COMPILE_OK`, 3 176 unit-passia / 0 failurea ja
`CQ_TESTS_PASSED`, `CQ_SAFETY_PASSED`; viimeisen DSP-muutoksen jälkeen lisäksi
`CQ_LINKED_STEREO_PASSED` (L/R-offsetit 0/0) ja `CQ_HOLD_OK`.

Foundation hyväksyttiin 2026-07-19. Se ja G1:n ensimmäinen shell/Ink-versio
commitoitiin `archipelago`-haaraan commitissa `1f4a210`; G1-versio on visuaalista
katselmointia odottava tarkistuspiste, ei hyväksytty ulkoasu. Foundation-vaihetta ei
aloiteta seuraavassa sessiossa uudelleen.

## Archipelago-portit

### G1 — Day skin

**Nykytila 2026-07-19:** ensimmäinen Day skin ja planted-cable-overlay on katselmoitu.
Neljä organ-pylvästä, omalla Ink-kielellä piirretyt toimivat säätimet, täsmälleen 16
source jackia / 22 destination socketia ja hit-transparent overlay ovat olemassa.
Käyttäjä hyväksyi yleisen kuvallisen suunnan, kaapelien kaaret/paksuudet/värit sekä
10/10/11/9 px typografian ja vahvemmat neutraalit. Gate/trig-kaapelit ankkuroidaan
glyph-socketiin eikä viereiseen charge-lamppuun; passiivinen lamp bead erottuu
patch-kohteesta.

Ensimmäinen versio on palautuspisteessä `3d631dd` ja sen väljempi 26/32/36-versio
checkpointissa `b3d0096`. Käyttäjä hyväksyi seuraavaksi uuden ensisijaisen Day-suunnan:
kompaktit toiminnalliset saarekkeet, 26/34/44 px knob-hierarkian, Cocon normatiivista
muistipolkua näyttävät kaksitasoiset signaalikaaviot ja Quantussyn spesal-tilasta sekä
`/cq_castle`-aktiivisuudesta elävän viiden linnakkeen topologian. Visualisoinnit näyttävät
olemassa olevaa rakennetta/tilaa eivätkä täytä paperia koristeella. Hyväksytyn checkpointin
portti on `CQ_COMPILE_OK`, 3 361 unit-passia / 0 failurea, `CQ_GUI_SHELL_PASSED`,
`CQ_SAFETY_PASSED` ja `CQ_HOLD_OK` (A4-korrelaatio 1.0, A5-drifti 0.0 dB).

Seuraava layout-katselmointi hyväksyttiin myös: Quantussyn ensimmäinen range-rail ja
Coco-slot-rail ovat samassa globaalissa Y-koordinaatissa, topologia käyttää laajemman
alakentän, `dust`/`rail` väistävät kaavioviivaa, bits/wrap ovat linjassa ja Masterin
REC/PANIC-osuma-alueet ovat 88×34 / 112×34 px. Detaljitason hienosäätöön palataan vasta,
kun muutkin G1-osiot ovat valmiimpia.

Mouse banana grammar + audition hyväksyttiin 2026-07-20. Mukana ovat WYHIWYG
plant/move/rip, polarity/wheel, harvest-lasso, solo-light, 32 operaation undo,
GUI-SHAKEn yhden operaation undo, 40 px audition-tartunta, controls-first-hit-laki
sekä käyttäjän hyväksymä yhtenäinen hover-kielioppi. Portti: `CQ_COMPILE_OK`,
3 411 unit-passia / 0 failurea, `CQ_GUI_SHELL_PASSED` ja `CQ_SAFETY_PASSED`;
viimeinen DSP-portti säilyi `CQ_HOLD_OK` (A4 1.0, A5 0.0 dB).

Drawerin käyttäytymistestit 1–5 hyväksyttiin 2026-07-20; tiheän patchin perusteella
`cable opacity` -säädintä ei tarvita. Inspector jakaa saman audition-kirjanpidon,
tekee release-vaiheessa yhden fine-edit-commitin/undon ja sisältää patch-preset-menun.
Spellbook on read-only session patch journal, ei komentokonsoli: se diffaa kaikkien
kerrosten committed `CQMatrix`-ilmoitukset kopioitavaksi `CQ.patch(...)`-kieleksi,
jättää transientit pois ja näyttää pysyvästi valitun rivin sekä sen batch-kontekstin.
Memoryssa ovat snapshot-save/recall ja A→B morph; niiden ympärille rakennettu seed bank
näyttää live-patchin ja valitun snapshotin sormenjäljen, reittitilastot, erot nykyiseen
matriisiin sekä todelliseen `CQSnapshots`-etenemään sidotun morph-mittarin.

Ensimmäinen 184 px resizable bottom-Drawer katselmoitiin ja hylättiin tilamallina:
se jätti soittimen puoliksi tavoittamattomaksi, resize nyki ja pieni Excel-mäinen matriisi
ei kutsunut soittamaan. Käyttäjä hyväksyi 2026-07-20 uuden, `GUI_SPEC.md` §8:aan
ei-normatiivisena kokeiluna kirjatun suunnan. Workbench + patch loom -ydin hyväksyttiin
visuaalisesti 2026-07-20. Drawer avattuna korvaa vakaan keskialueen workbenchinä
Day/Night-pinnan; Master pysyy toimivana eikä resizea, instrumenttiscrollia, click-awayta
tai toista ikkunaa ole.

Inspector-prototyyppi on täysleveä patch loom: tarkka 16×22-järjestys, 48×24 px solut
ilman sisäscrollia, ryhmitellyt lyhyet diagonaaliotsikot, source-family-merkit,
hiljaiset railit/risteykset, painoa ja polariteettia sekä värillä että geometrialla
näyttävät solmut, hover-rivi+kolumni, pysyvä soluvalinta ja tarkka route-ribbon.
`/cq_aura` jätettiin tarkoituksella pois tästä staattisesta katselmuksesta. Portti:
`CQ_COMPILE_OK`, 3 462 unit-passia / 0 failurea, `CQ_GUI_SHELL_PASSED`,
`CQ_GUI_DRAWER_PASSED`, `CQ_GUI_DRAWER_REENTRY_PASSED` ja `CQ_SAFETY_PASSED`.
Serveritön visuaaliharness on
`tests/gui_drawer_preview.scd`. Hyväksytyn ytimen jälkeinen viimeistelyprototyyppi siirtää
aina `drawer`-nimisen tabin Nightin viereen, linjaa Inspectorin kaikki tiedot 1128 px
loom-railiin, korjaa Spellbookin Qt-rivivärit ja korvaa tyhjän Memory-lomakkeen seed
bankilla.

Viimeistelykierros hyväksyttiin visuaalisesti 2026-07-21 (manuaalitesti 7) ja
commitoitiin `7eadd6f`. Samalla käyttäjä hyväksyi kaksi asiaa. **Muistisanasto:** soittimessa on täsmälleen kaksi muistisanaa —
*preset* on koodissa elävä patch-resepti (painot + ne nupit, joita ilman banaanit ovat
kuuroja) ja *snapshot* on levylle tallennettu soittimen tila. Memory-sivu saa käyttää
"seed bank" -metaforaa otsikkonaan, mutta sen kohteet ovat snapshotteja; GUI:n
kolmas sana poistettiin. **Qt-jäätymiskorjaus:** näkymän piilotus hiiren osoittimen
alta lukitsi koko ikkunan (Tab Day-paneelin päällä), koska Qt kutsui kieltä samalla
kun kielisäie odotti Qt:ta. Day/Night- ja Drawer-vaihdot menevät nyt jonoon
(`CQGuiShell.prSwapStage`), ja shell state on synkroninen totuus. Korjaus
todennettiin manuaalitestillä 9 sekä ajamalla GUI-smoket osoitin ikkunan päällä —
juuri se tila, joka ennen jumitti joka kerta.

Drawerin rakennustyö on tämän jälkeen valmis: Inspector, Spellbook ja Memory ovat
visuaalisesti hyväksyttyjä eikä avoimia Drawer-suunnittelupäätöksiä ole. G1 ei silti
ole valmis: `/cq_aura`, koko G1:n soitettavuus- ja kuunteluportti sekä
100/125/150 % skaalaus- ja resurssitarkastus puuttuvat.

- [x] Yksi Day/Night-ikkunakuori, pysyvä Master-strip ja Drawer vähintään 1280×720:ssa.
- [x] Yhteinen Ink kit: normatiivinen paletti, viiva, glyphit ja session-seedattu
  epäsymmetria ilman kuva-assetteja tai icon-fontteja.
- [x] Dayn 16 source jackia ja 22 destination socketia oikeilla organ skin -koodeilla.
- [x] Hit-safe cable overlay ja koko yhteinen banana grammar: audition, WYHIWYG plant,
  move, rip, harvest-lasso (§4.4), wheel, polarity, solo-light, Esc ja 32 operaation
  Ctrl+Z.
- [x] Drawer: Inspector, notification-streamiin perustuva Spellbook ja snapshot/morph
  Memory. Manuaalitestit 1–7 ja 9 hyväksytty (2026-07-21); `cable opacity` päätetty
  jättää pois. Drawerin oma soitettavuus- ja kuuntelukoe ajetaan osana alla olevaa
  G1:n yhteistä §13-porttia, ei erillisenä Drawer-gattina.
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

## Ratkaistut GUI_SPEC-päätökset

Nämä ovat nyt normatiivisia eivätkä enää toteutusta estäviä saumoja:

- [x] `/cq_charge`: speksin 11 summaa vs inventaarion 9 gate/trig-kohdetta.
  — Päätös 2026-07-19: 9 kanavaa; kanoninen järjestys = matriisin destinaatio-
  järjestys suodatettuna gate/trigeihin (GUI_SPEC §9).
- [x] `harvest`: nimi undo-luettelossa ilman määriteltyä elettä tai semantiikkaa.
  — Päätös 2026-07-19: määritelty eleeksi (GUI_SPEC §4.4): tyhjältä pohjalta
  aloitettu oikean napin vapaa lasso, molemmat näkymät, endpoint-säännöllä
  "kierrä elin = riisu se", yksi commit/undo-op/spellbook-merkintä, Esc peruu;
  kuuluu G1-kielioppiin.
- [x] `mic root`: varoituksen visuaalinen koti ilman uutta 17. matriisilähdettä.
  — Päätös 2026-07-19: ei-patchattava mic-verso juurten vieressä, 3 tilaa:
  ei piirretä (ei konfiguroitu) / lepäävä whisper-silmu (micChan asetettu,
  micGain 0) / danger-punainen (micChan asetettu JA micGain > 0) (GUI_SPEC §16.6).
- [x] REC: starting/stopping/error-tilojen normatiivinen look.
  — Päätös 2026-07-19: staattiset välitilat, ei uutta animaatiota: starting…/
  stopping… okratekstinä elapsed-paikalla + nappi ei reagoi; error = ontto
  rengas + piirretty vinoviiva + syy notice-rivillä, kuittautuu seuraavasta
  painalluksesta (GUI_SPEC §16.8).
- [x] Tiheän patchin hit-prioriteetti ja jatkopolku.
  — Päätös 2026-07-20: säätimen näkyvä hit-alue voittaa sen yli kulkevan johdon;
  kaapeli kohdistuu tyhjällä paperilla tai endpointista ja muut kaapelit
  himmenevät. Drawer `cable opacity` ja hetkellinen banana-focus arvioidaan
  myöhemmin omalla käytettävyysgatella. Pysyvä edit/performance-mode säilyy
  viimeisenä varavaihtoehtona (GUI_SPEC §4.5).

Päätökset on kirjattu ensin `GUI_SPEC.md`:ään ja synkronoitu tähän sekä GUI-
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

### GUI-osien välitarkastukset

Jokainen käyttäjälle näkyvä osa — esimerkiksi Day skin, banana-kaapelit, Drawer-paneeli,
Night map ja Weather Machine — tehdään seuraavalla tavalla:

1. Toteuta pienin ajettava ensimmäinen versio ja sen käyttäytymistestit.
2. Aja pakolliset compile/unit/NRT-portit sekä osan oma GUI-smoke.
3. Näytä versio käyttäjälle kuvana tai ajettavana näkymänä ja kerro myös keskeneräiset
   kohdat. Pysähdy visuaalista palautetta varten.
4. Tee visuaalinen viimeistely palautteen perusteella. Älä aloita seuraavaa näkyvää
   kokonaisuutta ennen palautetta.
5. Viimeistään hyväksytyn milestone-tarkastuksen jälkeen tee commit ja push
   `archipelago`-haaraan.

## Seuraavan toteutussession ensimmäiset askeleet

1. Varmista `archipelago`-haara, puhdas työpuu ja lue `CLAUDE.md`, `PLAN.md` sekä
   `GUI_SPEC.md` kokonaan. F0–F2, hyväksytty Day/Ink-layout, banana grammar +
   audition ja koko Drawer ovat valmiit; älä toista niitä.
2. Toteuta `/cq_aura` (GUI_SPEC §9): 16 kanonista source-magnitudia noin 15 Hz
   yhtenä ehdottomana control-rate SendReplynä, kuten olemassa olevat meterit.
   Kieli lukee sen namespacoidulla OSCdefillä; ambient redraw ≤ 15 Hz ja GUI CPU
   ≈ ≤ 3 % (§10/§11). Aura ruokkii jack glow'n, kaapelipulssin ja body glow'n —
   ei mitään muuta liikettä. NRT-turvakynnykset eivät muutu.
3. Aja G1:n lopullinen §13-portti: neljä markkeria, GUI-smoket, kuuntelugate
   (istuta `tri3 → coco1_spaf` korvalla, siirrä, rippaa, wheel-trimmaa,
   lasso-harvestaa ja peru), 100/125/150 % skaalaustarkastus ja CPU/resurssisoak.
4. Pysähdy jokaisen ensimmäisen ajettavan GUI-osan kohdalla yllä olevaan visuaaliseen
   välitarkastukseen. Älä aloita varsinaista G2 Night mapia ennen valmista G1-porttia.

Night-organismeja tai Weather Machinea ei piirretä ennen G1:n behavior foundationia.

## Myöhemmin, ei tämän vaiheen scopea

- **Snapshotit omaan kansioonsa.** Snapshotit kirjoitetaan yhä `presets/`-kansioon
  (`config.folders.presets`), mikä on suurin yksittäinen syy sekoittaa preset ja
  snapshot keskenään. Päätös 2026-07-21: lajitellaan myöhemmin omiin kansioihinsa —
  `snapshots/` käyttäjän `.cqsnap.scd`-tiedostoille, `presets/` koodin ja käsin
  kirjoitettujen patch-reseptien kodiksi. Vaatii `config.folders`-lisäyksen,
  SPEC §7 -päivityksen ja vanhan polun lukemisen taaksepäin yhteensopivasti.
  Ei tehdä kesken G1:n GUI-työn.
- Space-body ja Space-estetiikka odottavat erillistä konseptipäätöstä.
- Norns + grid + 16n + crow -portti on v2-visio; alkuperäinen tekninen muistio on
  arkistoidun suunnitelman lopussa.
- Stem-ulostulojen live-kuuntelu vaatii vähintään nelikanavaisen audiointerfacen ja
  säilyy erillisenä laitteistogatena.
