# CocoquantusSC — ajantasainen kehityssuunnitelma

Päivitetty 2026-07-21. Tämä tiedosto kertoo, mitä tehdään seuraavaksi. Valmiin v1.0-
toteutuksen yksityiskohtainen työpäiväkirja ja alkuperäiset checkboxit on jäädytetty
tiedostoon [`docs/history/PLAN_v1.0.md`](docs/history/PLAN_v1.0.md).

**Tilanne yhdellä rivillä:** foundation F0–F2 ja **koko G1 ovat valmiita ja käyttäjän
hyväksymiä**, §13:n loppuportti mukaan lukien (hyväksytty 2026-07-21, commit `295d14d`);
G2 Night mapin ensimmäinen ajettava versio on rakennettu 2026-07-21 ja **odottaa
visuaalista katselmointia**; sen jälkeen G2:n loput osat, sitten G3 Weather machine.

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
visuaalisesti hyväksyttyjä eikä avoimia Drawer-suunnittelupäätöksiä ole.

`/cq_aura` hyväksyttiin visuaalisesti 2026-07-21 sellaisenaan. **Kaikki G1:n
rakennuspalikat ovat siis tehty ja katselmoitu.** Jäljellä on vain §13:n
loppuportti: kuuntelugate, 100/125/150 % skaalaustarkastus ja CPU/resurssisoak.
Uutta G1-pintaa ei enää rakenneta — jos loppuportti paljastaa puutteita, ne
korjataan olemassa olevaan pintaan, ei uusina osina.

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
- [x] `/cq_aura`: 16 kanonista source-magnitudea noin 15 Hz; ambient redraw enintään
  15 Hz. Hyväksytty visuaalisesti 2026-07-21 sellaisenaan (hehkun voimakkuus ja
  kaapelin sykkeen syvyys). Tarkka GUI CPU ≤ 3 % mitataan G1:n loppuportissa.
  Toteutus: oma `\cqAura`-telemetriadef gMasterissa lukee valmiin modSrc-väylän,
  tasasuuntaa ja lagittaa audio-rate-tasolla ennen `A2K`:ta (audio-alueen castle ei
  siis alias oidu välkynnäksi) ja lähettää 15 Hz. GUI lukee sen `\cq_gui_aura`-
  OSCdefillä; viestin oma taajuus on ambient-katto, eikä ajastimia lisätty.
  Piirto koskee vain 16:ta pientä jack-näkymää ja kaapelikerrosta, ja se ohitetaan
  kokonaan jos mikään lähde ei liikkunut. Hiljaisuus lähteestä (headless,
  serveritön preview) ei ole sama kuin nolla: ilman telemetriaa kaapeli säilyttää
  hyväksytyn staattisen musteen. Hover/fokus ei sytytä auraa (§4.5).
Loppuportin eteneminen: G1.1 kuuntelugate hyväksytty 2026-07-21. G1.2 skaalaus
hyväksytty 2026-07-21 (100/125/150 % ja 200 %). G1.2 paljasti yhden vian: maksimoidussa
ikkunassa koko pystylöysä valui kahteen ainoaan min-kokoiseen näkymään — Cocon
muistipolkuun ja spesal-topologiaan — jotka venyivät tyhjiksi. Syy oli rakenteellinen,
ei piirtokoodin: `sources`-sarakkeessa oli `nil`-venytys nielemässä löysän, coco- ja
quantussy-sarakkeissa ei. Korjaus pysyi olemassa olevassa pinnassa: molemmat kaaviot
ankkuroitiin mitattuun hyväksyttyyn korkeuteensa (150 / 176 px 1280×720:ssa) ja
sarakkeisiin lisättiin päättävä `nil`, joten lisätila kerääntyy paperiksi eivätkä
säätimet siirry ikkunan koon mukana. Periaate: elastisuus on suunnittelupäätös —
vain se kasvaa, jonka informaatiosisältö kasvaa koon mukana.

G1.3 paljasti toisen vian, tällä kertaa GUI:n omassa CPU-osuudessa: serveri pysyi
`mayhem`-patchilla noin 20–21,5 %:ssa eli SPEC §9:n 25 %:n katon alla, mutta sclangin
osuus nousi noin 12–15 johdon patchilla lepotilassa 3,5–4,8 %:iin ja hiirtä
liikuteltaessa 5,5–6,8 %:iin — §13:n tavoite on noin ≤ 3 %. `mayhem`-patchilla GUI
lagasi näkyvästi. Korjaus vaati kolme kierrosta, ja **ensimmäinen niistä osui väärään
pullonkaulaan**: `cableRecords` rakensi kaikkien johtojen bezier-geometrian uudelleen
joka piirrossa (3,25 ms / 12 johtoa) ja `patchAt` maksoi saman jokaisella hiiren
liikkeellä, joten ne välimuistitettiin (3,25 → 0,007 ms; `patchAt` 3,47 → 0,21 ms) —
mutta käyttäjän mittaama lepotaso ei liikkunut lainkaan. Vasta Qt:n paint-eventin
sisältä mitattu profilointi kertoi totuuden: kustannus on **piirtokutsuissa**, ei
geometriassa. Yksi `Pen`-kutsu maksaa noin 53 µs kielen ja Qt:n rajapinnassa ja yksi
johto tarvitsee niitä ~11, joten kaapelikerroksen hinta on suoraan O(johdot):
12 johtoa 6–11 ms/ruutu, `mayhem`in 352 johtoa (kaikki 16×22 solua) **246 ms/ruutu**.

Toinen kierros poisti kutsuja pikselintarkasti: inverso-tikit piirretään yhtenä polkuna
yhdellä `Pen.stroke`-kutsulla per johto aiemman ~30 sijaan, ja `drawCable` saa valmiin
polylinen välimuistista sen sijaan että laskisi bezierin uudelleen. Tulos: negatiivinen
johto 11,37 → 7,34 ms, `mayhem` 246 → 164 ms. Kolmas kierros on käyttäjän 2026-07-21
tekemä linjapäätös, koska loppuosa on rakenteellinen eikä optimoitavissa:
`CQGuiCableOverlay.auraCableLimit` = 48 johtoa, jonka yli kaapelit pitävät §16.8:n
staattisen musteen eivätkä syki, kun taas 16 lähdejakkia sykkivät yhä (1,4 ms kaikille).
Kirjattu normatiivisesti `GUI_SPEC.md` §10:een. Mikään käsin rakennettu patch ei osu
rajaan; `mayhem` osuu. Suojattu pysyvillä smoke-tarkistuksilla: välimuistin tuoreus
(uusi patch, rippaus, painon muutos, ei turhaa uudelleenrakennusta) ja rajan molemmat
puolet (syke päällä, syke pois 352 johdolla, syke palaa harvennettaessa).

Neljäs kierros tuli käyttäjän havainnosta, että snapshot-morph nauhoituksen aikana vie
6–7 %. Syy oli aito speksirike: `CQSnapshots.morph` ajaa 30 Hz ja kirjoittaa joka
askeleella kaikki 57 jatkuvaa parametria sekä jokaisen muuttuneen matriisisolun, eli
kaksinkertaisella nopeudella §10:n 15 Hz ambient-kattoon nähden. Sekä parametrien
fan-out (`CQGui.paramFlushPeriod`) että kaapelikerroksen matriisipohjainen piirto
tahdistetaan nyt 15 Hz:iin etureunalla: ensimmäinen muutos hiljaisen hetken jälkeen
menee heti läpi, joten nupin käännös, 16n-faderi, koodi-`set`, istutus ja rippaus
pysyvät välittöminä, ja vain jatkuva virta harvennetaan — viimeistä arvoa ei koskaan
pudoteta. Mitattu: nuppien piirrot 4 420 → 2 312 kolmen sekunnin morphissa (22,9 % →
13,9 % seinäkellosta). Kirjattu `GUI_SPEC.md` §10:een.

Loppuun jäi tietoinen, kirjattu rakenteellinen rajoite: overlayn piirtomäärä ei
liikkunut tahdistuksesta lainkaan, mikä on itsessään todiste siitä, ettei sitä piirretä
meidän pyynnöistämme vaan sen alta — läpinäkyvä kaapelikerros vetää kaikki johdot aina
kun mikä tahansa säädin sen alla piirtyy. Käyttäjän päätös 2026-07-21: morph on
tahallinen ja ohimenevä operaatio, lepotaso palaa heti ~3 %:iin, joten G1 suljetaan ja
offscreen-välimuisti harkitaan G2:n jälkeen (ks. "Myöhemmin").

G1.3 hyväksytty 2026-07-21: serveri 19,8–21,5 % (SPEC §9:n katto 25 %), GUI lepotilassa
2,8–3,8 % 20 johdolla, hiiriliike +2 % välittömällä paluulla, ei muistin kasvua eikä
hidastumista ~20 min ajossa, inverso-johdot ja koko pinta visuaalisesti ennallaan.

- [x] `GUI_SPEC.md` §13:n automaatio, kuuntelu ja käyttäjän visuaalinen hyväksyntä.
  **Hyväksytty 2026-07-21 — G1 on valmis.** Sisältö: (1) neljä markkeria + kolme
  GUI-smokea, (2) §13:n kuuntelugate — istuta `tri3 → coco1_spaf` korvalla niin
  että swell kuuluu ennen commitia, siirrä banaani, rippaa nykäisemällä,
  wheel-trimmaa, lasso-harvestaa muutama ja peru koko saalis, ja tarkista että
  Spellbookin rivit vastaavat tekoja, (3) ikkuna 100/125/150 % Windows-skaalauksilla
  1280×720:stä ylöspäin, (4) CPU/resurssisoak GUI auki: SPEC §9:n 25 % kokonaiskatto
  pitää ja GUI:n oma osuus on noin ≤ 3 %, (5) sinun visuaalinen hyväksyntäsi.

### G2 — Night map

**Nykytila 2026-07-21: anatomia (G2.1) ja yön kielioppi (G2.2) on katselmoitu ja
hyväksytty; suunta todettiin oikeaksi.** Toteutus: `CQNightGeometry` (puhdas
layout-matematiikka — §15:n topologiatotuudet yksikkötestattuja ilman ikkunaa),
`CQInkNight` (Ink-kitin yökappaleet) ja `CQGuiNightMap` (Qt-kompositio). Yö
kantaa samat 16 jackia / 22 socketia, jaetun `CQBananaGrammar`in, oman
night-skin `CQGuiCableOverlay`n ja oman controllerin; §4.1:n view-flip-revert,
WYHIWYG-istutus, harvest ja undo on savutestattu yöllä
(`tests/gui_night_smoke.scd` → `CQ_GUI_NIGHT_PASSED`). Kerrokset ovat
opaakkitaustaisia, joten ambient-kehys piirtää vain liikkuneen informaation;
castle-hehku on delta-portitettu (0.04) ja piilotettu stage ohittaa
gesture-refreshit (`CQGuiBananaController.refreshGate`).

Katselmoinnissa käyttäjä pyysi yöstä itsenäisen soittimen: olennaiset
core-toiminnot on voitava tehdä yöllä, ilman että yö clutteroituu Dayn
kopioksi. Ratkaistu Blasserin hengessä **night touch grammarilla** (normatiivinen
`GUI_SPEC.md` §6.1, päätetty 2026-07-21): kehot kasvattavat kosketuspisteensä —
kuorta kelataan (speed), spiraalin sydän sinetöi loopin (hold), suun chevron
kääntää lukusuunnan (flip), vuorovesisirpit kuiden alla kaatavat spaf/in/fb:n
ja deck-levelit, ringin silmä vetää qFreq+qChaosia 2D-eleenä, kellotaulun
napautus askeltaa spesalia, ja tiedoston pudotus lintuun lataa deckin
(aikaistettu askeleesta 3, koska lataus on core). Toteutus: `CQGuiNightTouch`
(elekone + puhdas kulmamatematiikka), julkiset ground-delegaatit
`CQGuiBananaController`iin (kaapeli voittaa jokaisen painalluksen — §4.5).
Loput yöltä tavoitettavat: pyhä näppäinkartta (hold/flip/skip/spesal/rec/shake)
ja pysyvä Master-strip. Skip odottaa read-head-kipinää (§6.1:n kirjaus);
slotit/dolby/bitit/ranget jäävät Dayn ääniverstaaksi (slot-oletukset tekevät
pudotetusta tiedostosta kuuluvan).

**Tulikärpästen hiljaisuuden laki ratkaistu samassa hengessä** (`GUI_SPEC.md`
§16.6, pysyvä): joki virtaa kun saari soi; täysin hiljainen soitin pitää tyynen
veden. Puulaatikossa valot liikkuvat signaalista, eivät seinäkellosta — tämä ei
ole budjettihätäratkaisu vaan lopullinen käytös, myös offscreen-välimuistin
jälkeen. Ensimmäisestä versiosta yhä lykätty: spiraalin alfa-hengitys
coco-ulostulosta ja lintujen flutter.

- [x] Topology-true Archipelago: viiden castlen oikeansuuntainen ring, vasen/oikea
  Coco-spiraali, roots below ja birds above `GUI_SPEC.md` §16.6:n anatomialla.
  *(Hyväksytty visuaalisesti 2026-07-21, manuaalitestit G2.1–G2.2.)*
- [ ] Night touch grammar (`GUI_SPEC.md` §6.1): toteutettu ja savutestattu
  2026-07-21; G2.3-kuuntelukierros 1 käyty samana päivänä (kaikki eleet
  toimivat, palaute alla korjattu), odottaa uusintakatselmointia (G2.3b).
- [ ] Loop phase/read-head spark ja käyttäjän vahvistaman payloadin `/cq_charge`.
- [ ] Solo-light (body), deck-bird drag-over-korostus ja täsmällinen Night
  toolbar. *(Endpoint- ja kaapelisolo sekä bird-drop toimivat jo.)*
- [ ] Ei numeroita, Space-bodya tai dekoratiivista motionia; Master pysyy näkyvänä.
- [ ] `GUI_SPEC.md` §13:n automaatio, kuuntelu ja käyttäjän visuaalinen hyväksyntä.

**G2.3-kuuntelukierros 1 (2026-07-21):** kaikki seitsemän kosketuspistettä
toimivat korvalla; palaute käsitelty samana päivänä (`GUI_SPEC.md` §6.1 ja
§16.6 päivitetty):

- *Chevron-bugi:* vanha koti (suusta ulospäin + glyyfi ankkurista oikealle)
  leikkaantui shell-laatikon reunaan vasemmalla ja jäi coco-jakin
  läpinäkymättömän 26 px näkymän alle oikealla. Nyt suun alla (`mouth +
  (0, 24·scale)`), glyyfi keskitetty ankkuriin; väljyys naulattu
  `CQNightGeometryTest.test_inkClearances`iin.
- *Kellotaulun z-glitch + affordanssi:* dial r 26·scale kulki endpoint-
  näkymien (r 18–46) alta ja spesalin 34 px lamppulaatikko ylsi lähes
  keskustaan. Nyt **sisäkello**: haalea kellotaulukehä r 10·scale rungon
  sisällä, tila spark-helmenä + halona, staattinen hover kirkastaa; spesal-kuu
  siirtyi r 34 → 40, jotta sen laatikko väistää kellon joka skaalalla
  (testattu kaikilla 8 helmi-kodilla molemmilla lavakooilla).
- *Kelauspalaute:* **reel pearl** — spark-helmi spiraaliviivalla paikassa
  `unmap(cocoNSpeed)` (sisäsilmä = hidas, suu = nopea); liukuu eleessä
  suppilon totuudesta maalattuna (`CQGuiNightMap.paintSpeed`), lepotilassa
  näyttää arvon. Spiraalin hover kirkastaa viivan ja halottaa helmen.
  Samalla korjautui aito aukko: `sampleSegments` päättyi t = 5/6:een eli
  spiraalin viimeinen kuudennes ei kuulunut kelauspintaan — nyt suu päättää
  näytejonon.
- *Silmän palaute:* **myrsky-iiris** — pupillin aukko = qFreq, 12 säteen
  hajonta ja pituus = qChaos (käsin kylvetty sirontataulukko, ei rand-kutsuja
  piirrossa). Silmä sai oman läpinäkymättömän kerroksen (`eyeView`), joten
  iiriksen piirto ei koskaan maalaa rinkiä uudelleen (§10).

Portti 2026-07-21 (G2.3-korjausten jälkeen): `CQ_COMPILE_OK`, 4 747
unit-passia / 0 failurea, `CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`,
`CQ_GUI_DRAWER_PASSED`, `CQ_GUI_DRAWER_REENTRY_PASSED` ja
`CQ_GUI_NIGHT_PASSED` (sis. §6.1-eleet, §4.5:n kaapeli-ensin-tarkistuksen,
helmen/iiriksen suppilototuuden ja hover-vihjeet). (`CQ_HOLD_OK` ajettiin
session alussa; DSP-koodia ei muutettu.)

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

**G2:n anatomia on hyväksytty ja §6.1:n night touch grammar toteutettu
2026-07-21; touch grammar odottaa kuuntelukatselmointia (manuaalitesti G2.3).
G1 on suljettu eikä sitä avata uudelleen.**

1. Varmista `archipelago`-haara ja puhdas työpuu, ja lue `CLAUDE.md`, `PLAN.md` sekä
   `GUI_SPEC.md` kokonaan — erityisesti §16.6 (Night-anatomia) ja §10 (piirtobudjetti).
   Dayn muistipolun koko ja kaapelikerroksen offscreen-välimuisti ovat kirjattuja,
   tietoisesti siirrettyjä kysymyksiä — ne eivät ole G2:n scopea.
2. Aja automaatio ensin ja raportoi markerit: neljä markkeria (`compile_check`,
   `run_all`, `nrt_safety`, `nrt_hold_integrity`) ja **neljä** GUI-smokea
   (`gui_shell_smoke`, `gui_drawer_smoke`, `gui_drawer_preview_reentry_smoke`,
   `gui_night_smoke`). Lähtötaso 2026-07-21: `CQ_COMPILE_OK`, 4 747 passia /
   0 failurea, `CQ_SAFETY_PASSED`, `CQ_HOLD_OK` ja kaikki neljä GUI-markkeria.
   **Huom:** GUI-smoket ajetaan omana prosessinaan; jos ne jumittuvat, tarkista ensin
   hiiren osoittimen sijainti (ks. Qt-jäätymiskorjaus yllä) eikä koodia.
3. G2.3-kierros 1 on käyty ja korjattu (chevron, sisäkello, reel pearl,
   myrsky-iiris, hoverit — ks. G2-osio). Odota käyttäjän uusintakatselmointi
   (manuaalitesti G2.3b) ja tee viimeistely sen pohjalta. Vasta palautteen
   jälkeen jatka G2:n seuraaviin osiin: loop-phase/read-head spark (tuo
   samalla skipin yökodin, §6.1), `/cq_charge`-lamput, body-solo,
   drag-over-korostus ja Night toolbar.
4. Muista G1:n opetus piirtobudjetista: `GUI_SPEC.md` §10 sitoo myös Nightin. Ambient
   enintään 15 Hz, ilmoitusvirrat tahdistetaan etureunalla, eikä kustannusta arvata —
   se mitataan Qt:n paint-eventin sisältä. Kielen ja Qt:n rajapinnan `Pen`-kutsu
   maksaa noin 53 µs, joten piirron hinta on kutsujen määrä, ei matematiikka.

Weather Machinea ei piirretä ennen G2:n hyväksyntää.

## Myöhemmin, ei tämän vaiheen scopea

- **Snapshotit omaan kansioonsa.** Snapshotit kirjoitetaan yhä `presets/`-kansioon
  (`config.folders.presets`), mikä on suurin yksittäinen syy sekoittaa preset ja
  snapshot keskenään. Päätös 2026-07-21: lajitellaan myöhemmin omiin kansioihinsa —
  `snapshots/` käyttäjän `.cqsnap.scd`-tiedostoille, `presets/` koodin ja käsin
  kirjoitettujen patch-reseptien kodiksi. Vaatii `config.folders`-lisäyksen,
  SPEC §7 -päivityksen ja vanhan polun lukemisen taaksepäin yhteensopivasti.
  Ei tehdä kesken G1:n GUI-työn.
- **Kaapelikerroksen offscreen-välimuisti.** Kaapelikerros on läpinäkyvä koko
  Day-alueen kokoinen taso säätimien päällä, joten kun mikä tahansa nuppi sen alla
  piirtyy, Qt kutsuu overlayn `drawFunc`ia ja se vetää *kaikki* johdot uudelleen,
  vaikka Qt näyttää niistä vain nupin kokoisen palan (SC:n `drawFunc` ei tiedä
  leikkausalueesta). Tämä on ainoa jäljellä oleva syy siihen, että snapshot-morph
  maksaa ~5–6 % lepotason ~3 %:n sijaan. Oikea korjaus on piirtää kaapelikerros
  Image-puskuriin ja blitata se, kunnes geometria tai alpha oikeasti muuttuu.
  Päätös 2026-07-21: ei tehdä G1:n lopussa, koska se on iso muutos hyväksyttyyn
  renderöintiin ja vaatii oman visuaalisen katselmuksensa — harkitaan G2:n jälkeen.
- **Dayn staattisen muistipolun visuaalinen paino.** Käyttäjän havainto 2026-07-21:
  "signal memory path" on Dayn vähiten elävä osa — se näyttää reitin muttei tilaa —
  joten sen nykyinen tila-osuus on iso suhteessa informaatioarvoon. G1:n loppuportissa
  korjattiin vain venyminen (ankkurointi); kysymys siitä, pitäisikö kaaviota madaltaa,
  hiljentää vai poistaa, ratkaistaan omana suunnittelupäätöksenään G2:n jälkeen.
  Sama arviointi koskee "spesal topology" -osiota, joka sen sijaan **on** elävä.
- **Kosketusnäyttö-optimointi Night-modeen.** Käyttäjän havainto G2.3-kierroksella
  (2026-07-21): kelaus ja muut §6.1:n eleet vetävät kättä läppärin kosketusnäytölle,
  ja Nightia halutaan soittaa sormin. Oma työvaihe myöhemmin: kosketuskohteiden
  koot, multi-touch (esim. kelaus + sirppi yhtä aikaa), ele-erottelu (Qt:n
  touch-eventit vs. hiiriemulaatio) ja se, mitkä eleet hyötyvät kahdesta sormesta.
  Ei aloiteta ennen G2:n hyväksyntää.
- **Lintu-dropin vihje vai Night-infonappi.** Käyttäjän kysymys G2.3-kierroksella
  (2026-07-21): pitäisikö linnulla olla visuaalinen vihje pudotettavuudesta, vai
  pieni "i"-nappi, joka avaa Night-eleiden selitykset. Suunnittelupäätös tehdään
  käyttäjän kanssa myöhemmin (drag-over-korostus §16.7:n drop target -lain mukaan
  tulee joka tapauksessa G2:n loppuaskelissa).
- Space-body ja Space-estetiikka odottavat erillistä konseptipäätöstä.
- Norns + grid + 16n + crow -portti on v2-visio; alkuperäinen tekninen muistio on
  arkistoidun suunnitelman lopussa.
- Stem-ulostulojen live-kuuntelu vaatii vähintään nelikanavaisen audiointerfacen ja
  säilyy erillisenä laitteistogatena.
