# CocoquantusSC — ajantasainen kehityssuunnitelma

Päivitetty 2026-07-22. Tämä tiedosto kertoo, mitä tehdään seuraavaksi. Valmiin v1.0-
toteutuksen yksityiskohtainen työpäiväkirja ja alkuperäiset checkboxit on jäädytetty
tiedostoon [`docs/history/PLAN_v1.0.md`](PLAN_v1.0.md).

**Tilanne yhdellä rivillä (2026-07-22): G1, G2 ja G3 ovat kaikki rakennettu ja
käyttäjän hyväksymiä.** Foundation F0–F2 ja **koko G1** §13:n loppuportteineen
(2026-07-21, `295d14d`); **koko G2:n yöpinta** (G2.1–G2.5 rakennuspalikat,
loppuportti G2.6–G2.10, almanakkakulma G2.11 + spesal-jänteet G2.11b, ja §13:n
täydennys uudelle pinnalle G2.12 skaalaus + G2.13 soak — kaikki hyväksytty);
ja **G3 Weather machine** (kompassi + vuorovesi-sekoitus + sade/aurinko/oksastus
+ kaapelikerroksen offscreen-välimuisti + myrskymuoto), jonka kuuntelu- ja
visuaalikatselmointi lohkoissa G3.1a/G3.1b hyväksyttiin — **vuorovesi soi**,
suunnitelman ainoa aito tuntematon ratkesi korvalla.

**Seuraava askel konkreettisesti:** tämä (archipelago G1–G3) on
kokonaisuudessaan valmis ja hyväksytty; luontevin seuraava vaihe on
**mergata `archipelago` → `main`** (Draft-PR #1 valmiiksi), **arkistoida tämä
PLAN.md** `docs/history/`-kansioon ja **aloittaa uusi vaihe**
`docs/NIGHT_STANDALONE_PLAN.md`:n pohjalta (night-first-soitin: punch, ranget,
dolby, kevättulva, almanakan maisemointi, linnut, juuret → drawer-workshop →
day-tuomio; sitten §13 korvapassi ja §14 norns-portti). Ks. sen §12
rakennusjärjestys ja §13/§14.

**Almanakkakulma ja spesal-jänteet hyväksyttiin 2026-07-22** (lohkot G2.11 ja
G2.11b) neljän kierroksen jälkeen. Kulmassa on **kylvökuu** yläoikean taivaan
vapaassa vedessä (napautus < 8 px = shake; vaakaveto kaataa vaiheen; 120 ms
välähdys jokaisesta shake-reitistä — p, Dayn nappi, koodi) ja sen alla G3:n
työkaluille **tyhjänä varattu kompassivesi** (clearance-testeihin naulattu,
ei piirretä ennen G3:a — kuollut säädin rikkoisi §4.5:n ja §16.12:n).

Katselmoinnin kolme päätöstä (kirjattu `GUI_SPEC.md` §5/§6/§6.1/§10/§13/§16.6
ja `SPEC.md` §5.5):

1. **Kuun vaihe = kylvön tiheys, ei painojen katto.** Vanha mappaus oli sekä
   päällekkäinen (`patchDepth` on jo voimakkuusmakro) että kuulumaton: jokainen
   heitto kirjoitti samat ~35 solua, joten ohut sirppi luki "kaikki hiljeni"
   eikä "vähän kylvettiin". `CQ.shake` sai `density`-argumentin (oletus 0.1 =
   entinen käytös, joten koodikerros ja patternit ovat ennallaan). Lineaarinen:
   **puolikuu = tasan v1.0:n kourallinen** (10 % ≈ 35), täysikuu tuplat (~70,
   viidesosa mayhemista). **Uusikuu tyhjentää kaapelit** (kierros 3): tyhjä
   vaihe = tyhjä pelto, ja Ctrl+Z tuo sen takaisin. Ensimmäinen versio teki
   siitä no-opin, jottei pimein vaihe olisi tuhoisin teko — kädessä se luki
   rikkinäisenä säätimenä. Spesal-tiloja clear ei koske.
2. **Veto vaakaan** ja kuu peilattiin: valo kasvaa vasemmalta, terminaattori
   kulkee käden mukana oikealle. Arvon akseli on vaakasuora, joten eleen pitää
   olla myös (§4.5: vihje ja teko jakavat saman geometrian).
3. **Kylväjän tähdistö poistettiin.** Preset on resepti, ei keho, ja se oli yön
   ainoa säädin jota ei voinut käyttää lukematta tekstiä (§16.4) — kuusi
   identtistä glyfiä, joista piti hoveroida jokainen. Presetit ovat
   Inspectorissa, yhden toolbar-klikin päässä kummastakin näkymästä. G3 ei
   riipu niistä: kompassi leimaa kulmansa elävästä matriisista.

Kolmannella kierroksella lisättiin lisäksi **spesal-kytkentäjänteet** (lohko
G2.11b, `GUI_SPEC.md` §6.1/§16.6): yön sisäkello näytti *tilan* muttei sitä
mitä tila tarkoittaa — kenet linnake lukee. Nyt kytkentä piirtyy ringin
sisäjänteenä, mutta **vain siellä missä käsi kysyy**: ei mitään lepotilassa,
yksi jänne hoveroidun linnakkeen alla, kaikki viisi hoveroidun joen alla.
Neljäs kierros (käyttäjän palaute) toi kaksi lakia **molempiin nahkoihin**:
**linkki kulkee signaalin suuntaan** (linnake näytteistää kumppaninsa *itseensä*
— vanha nuoli osoitti luettavaan, mikä teki siitä sovelluksen ainoan suuntamerkin
joka kulki omaa virtaustaan vastaan) ja **track-and-hold on katkoviiva, ei
kaksoisviiva** (T&H seuraa tuloaan puolet kellojaksosta ja jäätyy toisen puolen,
joten katko *on* mekanismi; kaksoisviivan toinen juova leikkasi myös nuolenkärjen).
Poikkiviivat eivät olleet vapaana: väkäset tarkoittavat kaapeleissa inverso-
polariteettia. Yössä nuoli poistui kokonaan — suunnan kertoo **kulkeva valo**
jänteellä, joka ratsastaa joen omalla tulikärpäsvaiheella (telemetriakehykset,
ei omaa kelloa), koska nuolenkärki luki kaaviona. Day pitää staattisen nuolensa:
paperi on tyyni työkalu eikä kasvata ambient-liikettä. Kytkentä on rakenne eikä
suure — se muuttuu vain askellettaessa, ja ringin sisus kuuluu iirikselle,
tulikärpäsille, kaapeleille ja lassolle. Partneri- ja moodisääntö luetaan Dayn
`CQGuiCastleConstellation`-adapterista, joten yksi spesal-tila ei voi tarkoittaa
kahta eri kytkentää kahdessa nahassa.

Molemmat lohkot hyväksyttiin käyttäjän ajamina 2026-07-22; §13:n täydennyksen
(skaalaus + soak) jälkeen G2 kuitataan ja siirrytään G3 Weather machineen.

**Aktiivinen työhaara:** `archipelago`. GUI_SPECin neljä aiempaa määrittelysaumaa
ratkaistiin käyttäjän päätöksellä 2026-07-19; avoimia GUI-käyttäytymispäätöksiä ei ole.

## Auktoriteettijärjestys

1. [`SPEC.md`](../../SPEC.md), erityisesti §8, määrää parametrit, DSP-käyttäytymisen ja
   turvallisuuden.
2. [`GUI_SPEC.md`](../../GUI_SPEC.md) määrää GUI:n rakenteen, eleet, tilat, mitat ja Ink &
   Phosphor -visuaalikielen. GUI-kompromissi hyväksytetään käyttäjällä etukäteen.
3. [`ARCHITECTURE.md`](../../ARCHITECTURE.md) määrää toteutusrajat, omistuksen ja pure-view /
   yhden kirjoitussuppilon periaatteet.
4. [`audit/VERIFIED_FINDINGS.md`](../../audit/VERIFIED_FINDINGS.md) on varmennettujen
   audit-havaintojen totuus; [`audit/REMEDIATION_ROADMAP.md`](../../audit/REMEDIATION_ROADMAP.md)
   ja [`audit/GUI_DEVELOPMENT_PLAN.md`](../../audit/GUI_DEVELOPMENT_PLAN.md) pilkkovat työn.
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
- [x] Night touch grammar (`GUI_SPEC.md` §6.1): toteutettu ja savutestattu
  2026-07-21; G2.3-kuuntelukierros 1 käyty samana päivänä (kaikki eleet
  toimivat, palaute alla korjattu) ja **uusintakatselmointi G2.3b hyväksytty
  2026-07-21** (chevron, sisäkello, reel pearl, myrsky-iiris ja hoverit
  silmällä ja korvalla; manuaalitesti G2.3b).
- [x] Loop phase/read-head spark. **Hyväksytty 2026-07-21 (manuaalitesti G2.4)
  ja uudelleen G2.9:n kolmen muutoksen jälkeen 2026-07-22.**
  `/cq_phase`-telemetria 5 Hz `\cqCoco`-defissä (oma viesti — lähetystaajuus on
  kahden shell-kerroksen uudelleenpiirron katto, §10), luunvaalea lukupää-
  komeetta suuntahäntineen (suunta luetaan serverin audio-tason keskiarvosta,
  joten matriisi-flip näkyy totuudenmukaisesti eikä aliasoi), staattinen
  hover-halo, delta-portti (~1,5 px) harventaa lähes paikallaan olevan kelan
  piirrot. Ei telemetriaa = ei komeettaa. **Komeetan napautus = skip poistui
  2026-07-22** (ks. G2.9): komeetta on nyt pelkkä näyttö ja skipillä on oma
  paikallaan pysyvä merkki suun chevronin alla. Viesti kantaa nyt kolme arvoa:
  osoite, suunnan keskiarvo ja skipin taajuus hertseinä.
- [x] `/cq_charge`-latauslamput (9 kanavaa, GUI_SPEC §9). **Hyväksytty
  2026-07-21 (manuaalitesti G2.5).** Lataus lähetetään
  niistä samoista synteistä, jotka summat jo laskevat — coco 1 (replyID 1),
  coco 2 (replyID 2) ja quantussy (replyID 3), kukin 15 Hz — joten lamppu ei
  voi olla eri mieltä kuin oma Schmittinsä eikä grafiin synny toista kopiota
  summista. Summa huippupidätetään audio-tasolla ennen `A2K`:ta (nousu heti,
  lasku 0,15 s): yhden näytteen mittainen liipaisu jäisi muuten kokonaan
  näkemättä, ja sama lasku on §16.8:n kukinta. Piirto: lamppu kerrallaan,
  vain kun sen oma lataus liikkui (≥ 0.02) — kytkemätön gate summautuu ikuisesti
  tarkalleen nollaan eikä maksa mitään. Ei telemetriaa = himmeä lepohelmi.
- [x] Solo-light (body). **Hyväksytty 2026-07-21 (manuaalitesti G2.5).** Yön
  rungot ovat viisi linnaketta, kaksi kuorta ja rinki — kolme jälkimmäistä
  Dayn omilla nimillä `coco1`/`coco2`/`quantussy`, joten yksi jaettu kielioppi
  ei pidä kahta soloa samasta elimestä. Juuret ja linnut eivät saa runkoa:
  niillä on yksi endpoint, jonka oma solo sytyttää täsmälleen samat johdot.
  Kolmen kierroksen jälkeen syntyi yksi normatiivinen laki (`GUI_SPEC.md`
  §16.6): **runko otetaan kiinni sen omasta piirretystä ääriviivasta, ja se
  viiva on myös hover-vihje** — linnakkeen kehä, kuoren ulkokehä, joen viiva.
  Vihje ja teko jakavat aina saman geometrian. §4.5 sitoo joka
  klikkausmäärällä: nopea toinen napautus sisäkellolla, sydämellä,
  chevronilla, lukupäässä tai silmässä kuuluu sille säätimelle, ja siellä
  missä kelauspinta kulkee lähellä kuoren kehää, kelaus voittaa omalla
  vihjeellään.
- [x] **Deck-bird drag-over-korostus (§16.7)** — **hyväksytty 2026-07-22
  (manuaalitesti G2.10, kahden kierroksen jälkeen).** Toteutettu
  molemmille nahoille yhdestä `CQInk.drawDropTarget`-primitiivistä (koko appin
  ainoa katkoviivasuorakaide). Merkki on Dayssa deck-laatikossa ja yöllä
  linnussa; myös nimikyltti ja siipisoketti sytyttävät *linnun* reunan, koska
  §16.7 merkitsee kehon eikä sen osia. Yksi kohde kerrallaan, jaettuna
  `CQGui`:ssä (nahat eivät ole koskaan yhtä aikaa näkyvissä), ja Day/Night
  nimeävät kohteensa erikseen (`day_deckA` / `night_deckA`), mikä on
  yksikkötestattu.
  **Mitattu 2026-07-22 oikealla tiedostovedolla:** Qt kysyy `canReceiveDrag`in
  **vain kerran per dragEnter, ei joka liikkeellä**, eikä drag-leave-callbackia
  ole lainkaan. Ensimmäinen versio purki korostuksen ajastimella, ja reuna
  sammui kesken vedon vaikka tiedosto oli yhä laatikon päällä — affordanssi
  petti juuri sillä hetkellä kun sitä luettiin. **Korostusta pitää nyt yllä se,
  missä kursori on**: kevyt `QtGUI.cursorPosition`-kysely kohteen omaa globaalia
  suorakaidetta vasten, samaa jolle reuna piirretään (vihje ja teko jakavat
  saman geometrian, §4.5). Kaksi toisistaan riippumatonta ulospääsyä — kursori
  poistuu tai veto päättyy — joten se ei voi jäädä jumiin. Kysely käy vain
  silloin kun tiedostoa oikeasti raahataan soittimen päällä.
  Käyttäjän toinen havainto samalla korjattu: **yön kohde oli liian pieni** ja
  jäi Windowsin raahauskuvakkeen alle. Reuna piirtyy nyt linnun JA sen nimen
  ympärille (`birds[i][\dropRect]`) — se on sekä se, minkä silmä lukee decksi,
  että se, mikä oikeasti ottaa tiedoston vastaan. Piirto siirtyi linnun omasta
  44×32 kerroksesta koko lavan groundille, koska näkymä ei voi maalata omien
  rajojensa ulkopuolelle.
- [x] **Night toolbar = almanakkakulma** (päätetty käyttäjän kanssa 2026-07-22,
  kirjattu `GUI_SPEC.md` §6/§6.1/§16.6:een; vanha sanarivi/mininuppi-luonnos
  kumottu): kylvökuu — shake ja *kuinka paljon se kylvää* yhtenä kehona.
  Grow/wither/breed/kompassilevy ovat G3:n osia — niiden koti varataan
  **tyhjänä vetenä** geometriassa ja clearance-testeissä; kuollutta säädintä ei
  piirretä.
  **Hyväksytty 2026-07-22 (manuaalilohkot G2.11 ja G2.11b) kolmen
  korjauskierroksen jälkeen.** Toteutus:
  `CQNightGeometry.almanac` (kulma alkaa coco 2:n oman laatikon oikealta
  reunalta, joten se ei voi puristua kasaan millään lavakoolla —
  clearance-testit naulaavat kuun, sen välähdyshalon ja kompassiveden
  molemmilla lavakooilla), `CQInkNight.drawSowingMoon` (vaihe yhtenä
  bezier-terminaattorina: kuusi Pen-kutsua millä tahansa arvolla; valo kasvaa
  vasemmalta niin että terminaattori kulkee käden mukana),
  `CQGuiNightTouch.almanacDown/Move/Up` (napautus/veto erotetaan lasson omalla
  8 px:n kynnyksellä — `CQBananaGrammar.lassoTravel`, nyt yksi vakio kahdessa
  paikassa; kynnyksen pikselit ovat kuolleita, joten heitto ei nyhtäise
  vaihetta eikä kaato hypähdä alkaessaan) sekä oma opaakki
  `almanacView`-kerros. Uusi suppilonpätkä: `CQMatrix.shake`in ja
  `Cocoquantus.shake`in `density` (+`clampDensity`, SPEC §5.5) ja
  `CQGui.sowDensity`, joka kartoittaa vaiheen sille. `shakeAmount` sai
  `CQGui.onShakeAmount`-fanoutin, joten Dayn nuppi ja kuu ovat kaksi näkymää
  yhteen lukuun; oletus on nyt **0.5 = puolikuu = v1.0:n kourallinen**. Shake
  ilmoitetaan `state.changed(\action, \shake, amount)` -notisella (kuten skip),
  joten kuu välähtää `p`:stä, Dayn napista ja koodista samalla valolla.
- [x] **Kaksi aiemmista vaiheista löytynyttä piirtovikaa korjattu** (käyttäjä
  2026-07-21, G2.5-kierros 2): (1) **juuret leikkaantuivat yläpäästä** — syy
  oli juuri se epäilty: juuren viimeinen pätkä jäi kärkijakin läpinäkymättömän
  26 px näkymän alle. Korjaus on sama kuvio, jonka linnake jo käyttää joelle:
  kärkijakki piirtää oman laatikkonsa alta kulkevan pätkän itse
  (`roots[i][\arrival]` = saapumissuunnan yksikkövektori). Yleistetty
  `GUI_SPEC.md` §16.6:een: läpinäkymätön endpoint-näkymä on velkaa saman
  kohteliaisuuden jokaiselle viivalle, jonka päälle se asetetaan.
  (2) **Lintujen tiedostonimet leikkaantuivat** — `labelRect` 14 → 20 px.
- [x] **Dokumenttivelka maksettu 2026-07-21:** `SPEC.md` §6.2 kuvasi yhä v1.0:n
  neljää saraketta. Se on nyt kirjoitettu uusiksi osoittimeksi `GUI_SPEC.md`:hen
  + luetteloksi siitä, mitä GUI on skinistä riippumatta velkaa SPECille (mitä
  sen on näytettävä, mikä on saavutettava ilman koodia, mikropolitiikka, pyhä
  näppäinkartta, PANICin pysyvä näkyvyys, headless-vaatimus). `GUI_SPEC.md` §1:n
  maininta "edit deferred until adoption" päivitetty vastaavasti.
- [x] Ei numeroita, Space-bodya tai dekoratiivista motionia; Master pysyy
  näkyvänä. *(Todettu loppuportissa 2026-07-21/22. Ainoa liike yössä on
  §16.9:n valkoisella listalla ja telemetrian ajamaa — ei yhtään omaa kelloa.)*
- [x] **Loppuportin G2.6–G2.8 hyväksytty** (käyttäjä 2026-07-21): kuuntelugate,
  skaalaus 100/125/150 % ja CPU-soak. Soakissa sclang liikkui `mayhem`illa
  3–6,8 %; se osuu dokumentoituun haarukkaan (SPEC §9:n ~3 % koskee *soitetun
  kokoista* patchia, ja 352 johtoa on 7× `auraCableLimit`) eikä ole regressio.
  Laukaisijaa ei ole mitattu Qt:n paint-eventistä — se tehdään offscreen-
  puskurin yhteydessä, ei arvaamalla (G1.3:n oppi).
- [x] **G2.9: lukupään neljä havaintoa ratkaistu** (käyttäjä 2026-07-22).
  Kolme päätöstä, kaikki `GUI_SPEC.md` §6.1/§16.6:een normatiivisina:
  1. **Skip sai paikallaan pysyvän kodin** suun chevronin alta (§16.5:n
     spark-glyyfi, 10 px osumalaatikko, 120 ms mustevälähdys millä tahansa
     skipillä). Komeetta on nyt pelkkä näyttö. Perustelu: *liikkuva kohde ei
     ole säädin* — komeetta oli yön ainoa liikkuva kosketuspiste, ja siihen
     ehti osua vain nopeusasteikon sisimmällä kierroksella. Ei suun
     yläpuolelle, vaikka se näytti symmetriseltä: spiraali **päättyy** suuhun
     ja saapuu sinne ylhäältä, joten se puoli on itse ura (testi nappasi tämän).
  2. **Reel pearl → reel notch**: nopeus piirtyy nyt lovena uran poikki
     line-musteella. Vanha helmi oli 0,2 px komeettaa isompi ja samanvärinen —
     ei ero, jota käsi voi käyttää. Spark-muste varattiin tapahtumille.
  3. **Komeetalle kolmas tila**: kun `cocoN_skip`-lataus on palanut kahdella
     peräkkäisellä `/cq_phase`-kehyksellä, lukupää on *iskun alla* ja saa
     hännän tilalle §16.5:n kipinäglyfin. Sama laki kuin chevronin
     sukkulointitilalla: näytä regime, älä arvoa. Ei uutta telemetriaa eikä
     ajastinta — huippupidätetty lataus kertoo sen jo.
     **Toinen yritys** (2026-07-22): ensimmäinen versio piirsi silmästä ulos
     sen kantaman, jonka pää ehtii, ja käyttäjä ei nähnyt siitä mitään. Syy ei
     ollut piirrossa vaan suunnittelussa: kantama = nopeus × skipien väli,
     eli se kutistuu alle pikselin juuri silloin kun regime on selvin. Merkki
     katosi silloin kun sitä eniten tarvittiin. Yleistettiin `GUI_SPEC.md`
     §16.6:een: **regime-näyttö mittaa ehtoa, ei suuretta jonka ehto tuhoaa.**
     **Kolmas yritys** (2026-07-22): staattinen merkki luki *rauhallisena*,
     vaikka liipaisu kävi satoja kertoja sekunnissa. Vilkutus ei ole ratkaisu
     eikä edes mahdollinen — silmä sulauttaa yli ~10 Hz:n (oikea LED myös), ja
     tämä pinta saa 5 Hz telemetriaa, joten nopein piirrettävä välähdys olisi
     *hitaampi* kuin totuus ja keksityllä taajuudella. Nopeus piirretään siis
     **painona**: kipinän sakarat kasvavat 4 → 10 px ja toinen, vino pari
     avautuu puolivälin jälkeen, logaritmisella 8 Hz → 2 kHz asteikolla
     kahdeksaan askeleeseen kvantisoituna. Nopeus ratsastaa `/cq_phase`:n
     kolmantena arvona (§15 sallii viestien yhdistämisen; §10 laskee viestejä
     ja Pen-kutsuja, ei liukulukuja). Laki `GUI_SPEC.md` §16.6:een:
     **liike kantaa vain sen suureen, jonka silmä ehtii integroida.**
     **Neljäs kierros** (2026-07-22, käyttäjän pyyntö): merkki **tuikkii**
     joka telemetriakehyksellä, jotta tilan näkee olevan *päällä* — sakarat
     lyhenevät 74 %:iin ja himmenevät, ja nopeuden puolivälin jälkeen parit
     vaihtavat painotusta, jolloin tähti tuikkii eikä vain sykähtele. Tämä on
     §16.9:n valkoisella listalla oleva read-head spark. Olennaista:
     **kehykset ajavat tuiketta, ei oma kello** — pysähtynyt serveri jähmettää
     merkin sen sijaan että se animoisi väitettä elossaolosta. Kirjattu
     §16.9:ään yleisenä lakina kaikelle ambient-liikkeelle.
- [x] `GUI_SPEC.md` §13:n käyttäjän visuaalinen hyväksyntä G2.9:n korjauksille
  (lohko G2.9b) — **hyväksytty 2026-07-22**, tuike mukaan lukien.

**G2:n tila 2026-07-22: kaikki on rakennettu; hyväksymättä on enää Night
toolbar.** §13:n G2-scope on "archipelago bodies + spiral read-head + charge
lamps + solo-light + **toolbar presets/shake** + **deck-bird file drop**".
Deck-bird drop ja sen §16.7-korostus ovat valmiit ja hyväksytyt (G2.10).
**Night toolbar (almanakkakulma) hyväksyttiin 2026-07-22 lohkoissa G2.11 ja
G2.11b, spesal-kytkentäjänteet mukaan lukien.** Näin koko G2:n pinta on
rakennettu ja hyväksytty: G2.1–G2.5 rakennuspalikat, loppuportti G2.6–G2.10 ja
almanakkakulma G2.11/G2.11b. Kuittaamatta on enää §13:n täydennys uudelle
pinnalle (skaalaus 100/125/150 % + CPU-soak).

Almanakkakulman portti 2026-07-22 (korjauskierrosten ja spesal-jänteiden
jälkeen): `CQ_COMPILE_OK`,
**5 371 unit-passia / 0 failurea**, `CQ_SAFETY_PASSED`, `CQ_HOLD_OK` sekä
kaikki neljä GUI-markkeria (`CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED`, `CQ_GUI_NIGHT_PASSED` — viimeinen sisältää
kulman omat väitteet: hover, napautus vs. vaakaveto, tiheysasteikon ankkurit ja
välähdys mistä tahansa kerroksesta). `CQMatrixTest` sai `density`n omat testit:
tiheämpi kylvö on tiheämpi, jätetty pois = entinen heitto, nolla = aito no-op,
ja rikkinäinen arvo kaatuu dokumentoituun oletukseen.

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

**Nykytila 2026-07-22: rakennettu, testattu JA hyväksytty.** Kuuntelu- ja
visuaalikatselmointi (lohkot G3.1a/G3.1b) hyväksyttiin 2026-07-22; vuorovesi soi.
Kolme session alussa nimettyä haastetta ratkaistiin näin:

1. **Suorituskyky mitattiin ennen kuin mitään päätettiin** (G1.3:n laki).
   Uusi harness `tests/gui_paint_profile.scd` mittaa oikean Qt-paint-eventin
   sisältä: täysi muste maksaa dev-koneella 2,1 ms / 12 johtoa ja 82,7 ms /
   352 johtoa per repaint (referenssiläppäri ~2× hitaampi) — kompassiveto
   täydellä musteella olisi ollut mahdoton. Ratkaisu on kaksiosainen:
   **(a) kaapelikerroksen offscreen-Image-välimuisti** ("Myöhemmin"-listan
   kohta, nyt tehty — kompassin esiehto, kuten arveltiin, mutta *leposyistä*:
   kompassi kutsuu isoja patcheja pysyvästi lavalle, ja jokainen damage-
   repaint niiden alla oli O(johdot)); istutettu muste renderöidään Imageen
   ja kaikki repaintit joita muste ei itse pyytänyt (nuppi kerroksen alla,
   morphin paneelipiirrot, 60 Hz käsikaapeli ja lasso) maksavat yhden blitin.
   **(b) myrskymuoto**: itse vedon aikana matriisi muuttuu joka kehyksellä
   eikä välimuisti voi auttaa, joten lahti piirretään sateen läpi —
   polariteettiväriset vakioleveät rungot ilman tulppia ja väkäsiä, jokainen
   johto omana pienenä strokenaan — ja piirto tapahtuu **vain kun johtojen
   joukko muuttuu**: vuorovedessä repaint on banaanin laskeutuminen tai
   nousu, ei jatkuva kustannus. Ilmeinen "yksi jättipolku per polariteetti"
   rakennettiin ensin ja mitattiin **täyttä mustetta huonommaksi** (505 ms
   vs 177 ms / 352 johtoa — Qt:n stroker on superlineaarinen yhden polun
   alipolkujen määrässä); G1.3:n laki iski kolmannenkin kerran. Blitti
   maksaa ~4,5 ms johtomäärästä riippumatta. Mittarit vastasivat samalla
   PLANin vanhaan arvailukiellettyyn kysymykseen: **naapurinäkymän repaint
   ei laukaise overlayn piirtoa lainkaan** StackLayout-lavalla (10 castle-
   tai kompassirefreshiä = +0 overlay-piirtoa; `drawCount`-laskurit), joten
   välimuistin todelliset asiakkaat ovat 60 Hz elekerrokset ja koko ikkunan
   damage (resize, hard cut). Kaksi toteutuslakia kirjattiin `GUI_SPEC.md`
   §11:een: **Imagea ei koskaan renderöidä paint-eventin sisällä**
   (QtColliderin Pen on yksi globaali maalari — sisäkkäinen maalari jumittaa
   koko GUI-säikeen; renderit ajetaan AppClockilla ennen refreshiä, ja
   vanhentunut kuva piirtyy suoraan entiseen hintaan) ja välimuisti on
   **pikselintarkasti identtinen** (kuvan piirtää sama Pen-koodi; alusta
   ilman toimivaa Image-APIa putoaa äänekkäästi suoraan piirtoon, ei koskaan
   hiljaa puuttuvaan musteeseen).
   Mitatut mediaanit per oikea Qt-repaint (dev-kone; referenssiläppäri ~2×;
   `tests/gui_paint_profile.scd` toistaa nämä milloin vain):

   | johtoja | täysi muste | blit | myrsky/event |
   |---|---|---|---|
   | 12 | 3,9 ms | 4,4 ms | 1,2 ms |
   | 35 | 12,4 ms | 4,4 ms | 3,5 ms |
   | 70 | 23,0 ms | 4,3 ms | 6,7 ms |
   | 140 | 57,7 ms | 4,5 ms | 19,8 ms |
   | 352 | 174,9 ms | 4,8 ms | 49,4 ms |
2. **`compass.cqcompass.scd` kopioi CQSnapshotsin tavat** täsmälleen:
   kokonaislukuversio, rakenteellinen portti (väärä muoto tai uudempi versio
   hylkää koko tiedoston varoituksella), solukohtainen roskien ohitus,
   jokainen paino `CQMatrix.clampWeight`in läpi, autosave jokaisesta
   leimasta ja tyhjennyksestä. `config.scd` sai GUI_SPEC §10:n mukaisen
   `gui: (compassDir: ...)` -alieventin, jolla smoket osoittavat tiedoston
   `tests/tmp/`-kansioon — testileima ei voi koskaan ylikirjoittaa soittajan
   omia kulmia.
3. **Musikaalisuus (suunnitelman ainoa aito tuntematon) hoidettiin
   rakenteella, ei toivolla**: spekin bilineaarinen *amplitudi*sekoitus
   korvattiin **vuorovedellä** (`GUI_SPEC.md` §7.1, normatiivinen):
   kulmien jakamat solut liukuvat painojaan jatkuvasti (yhteinen runko
   morphaa), mutta eriävät johdot seisovat täydessä leimapainossaan kunnes
   aseman veto alittaa solun oman kiinteän vuorovesirajan — ja nousevat
   silloin kokonaan, banaani kerrallaan. Kolme perustetta: patch on
   diskreettejä banaaneja (puolimatka = puolet kummankin banaaneista
   täydellä äänellä, ei kaikki puoliteholla = puuroa); gate/trig-johdot
   elävät +0.5-Schmittin takana (amplitudisekoitus mykistäisi jokaisen
   flip/skip/spesal-banaanin lähes koko padolla, vuorovesi pitää ne
   laukeavina koko matkan); ja rajat ovat istunnoista riippumaton hash,
   joten padosta tulee **opittavaa maantiedettä** — sama kohta kasvattaa
   aina saman patchin. Tyhjää kulmaa kohti matkaaminen on kirjaimellisesti
   wither-by-travel. Lopullinen tuomio annetaan silti korvalla: G3.1a on
   ennen kaikkea kuuntelukoe, ja jos vuorovesi ei soi, palataan
   suunnittelupöytään tämä kirjaus edellä.

Toteutus: `CQCompass` (puhdas malli: kulmat, vuorovesi, breed/grow/wither,
tiedosto — yksikkötestattu `CQCompassTest`illä), `CQNightGeometry.almanac`
laajeni (pato + siemenkulmat + säärivi + `compassBox`, clearance-testit
molemmilla lavakooilla), `CQInkNight.drawCompass/drawWeatherMark` (pato,
siemenet, risti, ≤ 8 % kulmavedot; sade/oksastus/aurinko §16.5:n hengessä),
`CQGuiNightTouch` (matka/hyppy/elastinen/leima/sää — tap vs. veto lasson
8 px:llä kuten kuussa; Esc/Tab/panic → `cancelWeather`), `CQGuiNightMap`
(oma opaakki compassView-kerros; kaikki kirjoitukset writeMatrix-suppilon
läpi, transientit vedossa, yksi committi + yksi undo + yksi spellbook-lohko
per teko), `CQGuiCableOverlay` (välimuisti + myrskymuoto + piirtolaskurit
`drawCount`/`renderCount`/`lastDrawMs` soakia varten).

- [x] Compass: neljä weights-only-matriisimuistia, left commit, right elastic,
  ≤ 60 Hz batch writes (toteutettu 30 Hz — painot ovat lagittomia
  kontrollibusseja, nopeampi askellus ei osta kuuluvaa) ja yksi
  undo-operaatio. *(Sekoituslaki on nyt §7.1:n vuorovesi, ks. yllä.)*
- [x] Versioitu, validoitu ja clampattu `presets/compass.cqcompass.scd`
  autosave/load.
- [x] Grow/wither 2,5 Hz hold-operaatioina ja Breed täsmälleen §7:n vakioilla
  (breed lisäksi siivoaa < 0.05-haamut — witherin oma lattia).
- [x] Weather-operaatiot eivät muuta `CQSpec`-parametreja; `patchDepth` säilyy.
  *(Rakenteellisesti tosi: kaikki kirjoitukset kulkevat `CQMatrix`in kautta,
  jossa ei ole parametreja.)*
- [x] `GUI_SPEC.md` §13:n kuuntelu ja käyttäjän visuaalinen hyväksyntä
  (checklist-lohkot G3.1a/G3.1b hyväksytty 2026-07-22; vuorovesi soi).

G3:n ensimmäisen version portti 2026-07-22: `CQ_COMPILE_OK`, **6 611
unit-passia / 0 failurea**, `CQ_SAFETY_PASSED`, kaikki neljä GUI-markeria
(`CQ_GUI_NIGHT_PASSED` sisältää kompassin omat väitteet: leima/clear +
levypysyvyys, kulmatarkka recall, vuorovesirajasta johdettu deterministinen
matka, myrskymuoto päällä vain vedon ajan, grow/wither/breed §7:n vakioilla
ja siemenillä bittitarkasti, Esc-disarm, elastisen täsmäpaluu ilman undoa,
sekä välimuistin blit-vs-render-laskurit). DSP-koodiin ei koskettu, joten
`CQ_HOLD_OK` on edellisen portin arvo. Kaksi sitkeää opetusta matkalta:
sclangin stdout **puskuroituu uudelleenohjattuna** — jumittunut smoke näytti
tyhjältä, ja vasta skriptin oma watchdog-exit (nyt pysyvästi
`gui_night_smoke.scd`:ssä) huuhtoi evidenssin näkyviin; ja `Rect.extent`
palauttaa **Pointin**, jolla ei ole `width`iä — virhe joka kytki välimuistin
hiljaa pois ja paljastui vain jäljitysajolla.

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

**G3:n ensimmäinen ajettava versio on rakennettu ja automaatio on vihreä
(2026-07-22). Seuraava sessio alkaa käyttäjän palautteesta, ei uudesta
pinnasta.**

1. Varmista `archipelago`-haara ja puhdas työpuu; lue `CLAUDE.md`, `PLAN.md`
   ja `GUI_SPEC.md` — erityisesti §7 (weather machine + vuorovesi) ja §11:n
   uusi offscreen-välimuistilaki.
2. Aja automaatio ja raportoi markerit: `compile_check`, `run_all`,
   `nrt_safety` (+ `nrt_hold_integrity` jos DSP:hen kosketaan) ja neljä
   GUI-smokea. Lähtötaso 2026-07-22: `CQ_COMPILE_OK`, 6 611 passia /
   0 failurea, `CQ_SAFETY_PASSED` ja kaikki neljä GUI-markkeria.
   **Huom:** GUI-smoket omana prosessinaan; jos smoke näyttää jumittuneelta,
   muista että sclangin stdout puskuroituu uudelleenohjattuna — yösmoken
   watchdog pakottaa exitin 90 s:ssa ja huuhtoo evidenssin. Tarkista myös
   hiiren osoittimen sijainti (Qt-jäätymislaki) ennen kuin epäilet koodia.
3. Käyttäjän kanssa, tässä järjestyksessä:
   1. **G2:n kuittaus**: checklist-lohkot G2.12 (skaalaus) ja G2.13 (soak).
   2. **G3.1a/G3.1b**: kompassin, vuoroveden ja säärivin visuaalinen ja
      ennen kaikkea kuuntelukatselmointi. Vuoroveden musikaalisuus on
      suunnitelman ainoa aito tuntematon — jos se ei soi, päätös §7.1:n
      sekoituslaista avataan uudelleen kirjatun perustelun pohjalta.
   3. Palautteen viimeistelykierrokset olemassa olevaan pintaan; sitten
      §13:n G3-portti kokonaisuudessaan (skaalaus + soak myös padolle).
4. G1.3:n laki pätee yhä ja iski tänäkin sessiona kolmesti: kustannus
   mitataan Qt:n paint-eventin sisältä (`tests/gui_paint_profile.scd`), ei
   arvata. Uudet mittarit: `CQGuiCableOverlay.drawCount/renderCount/lastDrawMs`.

## Myöhemmin, ei tämän vaiheen scopea

- **Snapshotit omaan kansioonsa.** Snapshotit kirjoitetaan yhä `presets/`-kansioon
  (`config.folders.presets`), mikä on suurin yksittäinen syy sekoittaa preset ja
  snapshot keskenään. Päätös 2026-07-21: lajitellaan myöhemmin omiin kansioihinsa —
  `snapshots/` käyttäjän `.cqsnap.scd`-tiedostoille, `presets/` koodin ja käsin
  kirjoitettujen patch-reseptien kodiksi. Vaatii `config.folders`-lisäyksen,
  SPEC §7 -päivityksen ja vanhan polun lukemisen taaksepäin yhteensopivasti.
  Ei tehdä kesken G1:n GUI-työn.
- **Kaapelikerroksen offscreen-välimuisti — TEHTY G3:n alussa 2026-07-22**
  (ks. G3-osio). Samalla mitattiin `drawCount`-laskureilla se, mitä tässä
  kohdassa aiemmin oletettiin: naapurinäkymän repaint EI laukaise overlayn
  piirtoa StackLayout-lavalla lainkaan — morphin ylimääräinen kustannus tuli
  matriisin transienttien notifikaatioiden ajamista overlay-piirroista (jotka
  välimuisti nyt muuttaa blitiksi aina kun solut eivät oikeasti muuttuneet) ja
  nuppien omista piirroista, ei damage-vuodosta. Blitti ~4,5 ms johtomäärästä
  riippumatta; morph-luvut kannattaa mitata uudelleen G3:n soakissa.
- **Dayn staattisen muistipolun visuaalinen paino.** Käyttäjän havainto 2026-07-21:
  "signal memory path" on Dayn vähiten elävä osa — se näyttää reitin muttei tilaa —
  joten sen nykyinen tila-osuus on iso suhteessa informaatioarvoon. G1:n loppuportissa
  korjattiin vain venyminen (ankkurointi); kysymys siitä, pitäisikö kaaviota madaltaa,
  hiljentää vai poistaa, ratkaistaan omana suunnittelupäätöksenään G2:n jälkeen.
  Sama arviointi koskee "spesal topology" -osiota, joka sen sijaan **on** elävä.
- **Koko Night-näkymän mittakaava.** Käyttäjän havainto 2026-07-21 (G2.5):
  loppukatselmuksessa voisi harkita **melkein kaikkien näkyvien elementtien
  suurentamista** koko yökartalla — bodyt, kuut, sirpit, helmet ja tekstit.
  Tämä ei ole yksittäinen bugi vaan yksi mittakaavapäätös, joka koskee
  `CQNightGeometry.compute`n `scale`a ja px-vakioita (§16.10: viivanpaksuudet
  ja fontit pysyvät px:issä, bodyt skaalautuvat). Liittyy suoraan
  kosketusnäyttötyöhön alla — tehdään mieluiten samalla kertaa, ei ennen G2:n
  muiden osien valmistumista.
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
