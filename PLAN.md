# Yölahti — ajantasainen kehityssuunnitelma (Nocturne-vaihe)

Päivitetty 2026-07-30. Tämä tiedosto kertoo, **mitä tehdään seuraavaksi** ja
**missä järjestyksessä**. Vaiheen yksityiskohtainen *miten* — jokaisen osan
suunnittelu, geometria, karikot, korvakokeet — on
[`docs/NIGHT_STANDALONE_PLAN.md`](docs/NIGHT_STANDALONE_PLAN.md):ssä; tämä
tiedosto on sen status- ja järjestysnäkymä. Aiempien vaiheiden jäädytetyt
työpäiväkirjat: v1.0 [`docs/history/PLAN_v1.0.md`](docs/history/PLAN_v1.0.md),
Archipelago-GUI G1–G3 [`docs/history/PLAN_archipelago.md`](docs/history/PLAN_archipelago.md).

**Tilanne yhdellä rivillä:** v1.0 (DSP + koodikerros) ja koko Archipelago-GUI
(G1 Day · G2 Night · G3 Weather machine) ovat valmiita ja käyttäjän hyväksymiä;
archipelago on sulautettu `main`iin (PR #1, tagi `archipelago-1.0`). **Uusi
vaihe: Nocturne — yö koko soittimena.** N1–N9 on hyväksytty: Night + Master +
Drawer on nyt soittimen ainoa ylläpidettävä käyttöliittymä, ja koko hyväksytty
Nocturne-GUI-ketju on sulautettu paikalliseen `nocturne`-integraatiohaaraan.

**Julkinen identiteetti:** soittimen nimi on nyt **Yölahti**. Cocoquantus on
lähtökohta ja avoimesti dokumentoitu sukulinja, ei tuotteen nimi eikä lupaus
uskollisesta emulaatiosta. `CQ`/`Cocoquantus` säilyvät toistaiseksi teknisenä
API- ja luokkanimiavaruutena; niitä ei massauudelleennimetä kesken Nocturnen.
Portfolio- ja julkaisupinta on `README.md`:ssä, `SOURCES.md`:ssä ja `docs/`-kansion
case study / development process -dokumenteissa. Etärepon uudelleennimeäminen,
public-vaihto ja oikeat kuva-/äänidemot ovat erillinen julkaisutoimi.

**Seuraava askel konkreettisesti:** **N9 hyväksyttiin 2026-07-28.** Day on
poistettu, koska Night on inspiroivampi varsinainen soitin ja kahden
käyttöliittymän ylläpito hajottaisi kehityksen. Space-eläköityksen jälkeen
Night 31 · Master 4 · Workshop 39 on erillinen, koneellisesti todistettu
`CQSpec.names`-peitto. Dayn
spesal-tähdistö ja staattinen muistipolkukartta saivat kirjatun kuoleman:
Nightin osoittimen alla ilmestyvä suuntatotuudellinen kytkentäsointu kertoo
spesalin seurauksen tekopaikassa, ja Workshopin täsmäkontrollit sekä Nightin
kuoret tekevät passiivisesta muistipolkukartasta ylläpitokustannustaan
heikomman. N9:n jälkeinen rajattu polish-portti — kiduksen
kaapeliprioriteetti ja snapshot-kansion erotus — valmistui 2026-07-28.
**13.1 feedback-taper sekä 13.4 chaos-käyrä ja sen sääkehät hyväksyttiin
käyttäjän kuuntelu- ja näkökatselmuksessa 2026-07-28. Space-kuuntelupäätös,
tuotantoketjun hallittu eläköitys ja Nightin täyden mittakaavan GUI-passi
hyväksyttiin 2026-07-28–29. 13.2 speed-lattia (vaihtoehto A) ja 13.5:n
kiinteä p = 2 VCA-laki sekä 13.3:n uusi dust-lähde ja sen uudelleen viritetty
`selfNoise`-preset hyväksyttiin 2026-07-29. §13.6:n zipperiä ei erotettu
luotettavasti, joten se suljettiin ilman DSP-muutosta. §13.8:ssa valittiin ja
toteutettiin maltillinen −1 dB / todellinen Drive-oktaavi -kompensaatio, joka
hyväksyttiin myös live-inputilla.** **AIO-1:n compact/roomy-sommittelu
hyväksyttiin käyttäjäkatselmuksessa 2026-07-29. Katselmuksen refresh-linjaus
ja Windows-nimien sisäiset rivinvaihdot korjattiin vain näyttötekstissä.
AIO-2:n draft, suuntakohtainen exact-dedup-laitelista ja atominen
sibling-overlay, AIO-3:n vahvistettu candidate/rollback-transaktio sekä
AIO-4:n raw-input/master-presence ja GUI:n pacing toteutettiin
`nocturne-audio-setup`-haaralle. Raw-tap palautettiin, kun kontrolloitu koe
toisti jumin ilman sitä ja eristi syyksi käynnissä tehdyn laitelistauksen;
eristetty split-WASAPI-uusintaportti hyväksyttiin 2026-07-29. AIO-4:n fyysinen
hardware-katselmus on 2026-07-30 tehty split-, USB-single-device-, mic-,
PANIC-, stem-3/4-, stereo/4ch-REC- ja 10 minuutin patch-kohdiltaan; loopbackin
näkyvyys kirjattiin samana päivänä, ja avoimeksi jäi vain kaksisuuntainen
VB-CABLE-DAW-portti.**
**Fyysinen AIO-hardware-katselmus hyväksyttiin 2026-07-30, joten koko AIO-vaihe
on valmis.** Järjestys tästä on **käyttäjän 2026-07-30 päättämä tie 1.0:aan:
~~MOR-1~~ (hyväksytty) → ~~DAW-silta ReaRoutella~~ (toimii molempiin suuntiin
2026-07-31) → ~~ajonaikainen audiokelloguard~~ (AUD-W osa 1 hyväksytty
2026-07-31; osa 2 kulkee soakin mukana) →
**pitkä laitteisto-/resource-soak ← tässä ollaan** → merge `main`iin ja
1.0-tagi** (oma osio "Tie 1.0:aan" alla).
Kolme huokoista soittotilaa **ei ole 1.0:n ehto** — se on oma haaransa 1.0:n
jälkeen — ja Norns-portti (§14) tulee vasta niiden jälkeen. Yksi musiikillinen
muuttuja käsitellään edelleen kerrallaan. **N10:n sydän/kela- ja chevron-vihjeet hyväksyttiin visuaalisesti
2026-07-29**, joten erillistä GUI-velkaa ei enää ole.

## Auktoriteettijärjestys

1. [`SPEC.md`](SPEC.md), erityisesti §8, määrää parametrit, DSP-käyttäytymisen ja
   turvallisuuden. SPEC ja `CQSpec` eivät koskaan erkane.
2. [`GUI_SPEC.md`](GUI_SPEC.md) määrää GUI:n rakenteen, eleet, tilat, mitat ja
   Ink & Phosphor -visuaalikielen. GUI-kompromissi hyväksytetään etukäteen.
3. [`ARCHITECTURE.md`](ARCHITECTURE.md) määrää toteutusrajat, omistuksen ja
   pure-view / yhden kirjoitussuppilon periaatteet.
4. [`docs/NIGHT_STANDALONE_PLAN.md`](docs/NIGHT_STANDALONE_PLAN.md) on tämän
   vaiheen suunnitteludokumentti (ei vielä normatiivinen — kun se korvaa
   GUI_SPECin reseptin, spec-muutos kulkee sen inkrementin mukana, kuten §9).
5. Tämä tiedosto määrää toteutusjärjestyksen ja ajantasaisen statuksen.

Ristiriidassa turvallisuus voittaa. Epäselvää GUI-käyttäytymistä ei arvata eikä
korvata teknisesti helpommalla ratkaisulla ilman käyttäjän päätöstä.

## Git- ja toimitusmalli

- `main` on vakaa integraatiopohja; Archipelago-GUI on siinä (PR #1 sulautettu).
- `nocturne` on tämän vaiheen integraatiohaara. Työ tehdään siihen pieninä,
  testattuina committeina ja pidetään Draft-PR:ssä `main`-haaraa vasten. PR
  merkitään valmiiksi (`gh pr ready`) vasta kun vaihe on ohi.
- Vanhaa GUI:ta ei kopioida rinnakkaiseksi tuotteeksi eikä runtime-feature-flagia
  lisätä. N9 poistaa Day-rakenteen yhdellä peruutettavalla commitilla omalla
  katselmointihaarallaan; palaaminen tarkoittaa commitin peruuttamista, ei
  kahden käyttöliittymän ylläpitämistä runtimessa.
- Erillisiä lyhytikäisiä työhaaroja tarvitaan vain todellisessa rinnakkaistyössä
  **tai kun valmis inkrementti jää käyttäjän harkintaan.** N9 käytti tätä mallia
  ja hyväksyttiin 2026-07-28; sen katselmointihaara jää audit trailiksi, mutta
  hyväksytty työ elää nyt `nocturne`ssa.
  **Testi-commit ei ole hyväksyntä** — hyväksyntä on aina checklist-rivin
  rasti käyttäjältä.
- Lähdekoodi, testit ja dokumentit pidetään samassa commitissa silloin, kun ne
  yhdessä muodostavat yhden todennettavan käyttäytymismuutoksen.
- Nocturnen N3→N4→N5→N6→N7→N8-haarat muodostivat **pinotun ketjun**, eivät
  kuutta toisistaan riippumatonta toteutusta. Käyttäjän 2026-07-28 pyynnöstä
  N3–N8-ketju sulautettiin ensin paikalliseen `nocturne`en (`dc95ced`) ja
  hyväksytty N9 sen päälle 2026-07-28. Testi-commit ei edelleenkään yksin ole
  hyväksyntä.

## Nocturne-vaiheen rakennusjärjestys

Kukin numero on yksi inkrementti: oma pieni toteutus, oma checklist-lohko, oma
katselmointipysähdys ja oma commit. Yksityiskohdat viitatussa §:ssä.
`NIGHT_STANDALONE_PLAN.md §12` on tämän järjestyksen lähde. Kaikkia inkrementtejä
sitovat lisäksi **§1:n lait** (yksi kirjoitussuppilo, enum-vaihtoehdot CQSpecistä,
puhdas testattu geometria, opaakkien laatikoiden peittolaki, §4.5:n
prioriteettiketju, piirtokuri) ja **§5:n testisuunnitelma** (unit + yösmoke +
patteri). Kohtien 2–3 (kevättulva, almanakan maisemointi) tausta ja perustelu on
**§7:n layout-tuomio: uudelleensommittelu, ei uudelleensuunnittelu** — topologia
säilyy, vain mitat ja sommittelu muuttuvat, merkitykset eivät.

- [x] **1. Punch — momentaarinen hold** (§2). **Valmis ja hyväksytty
  2026-07-23** (checklist N1). Oikean napin pito kuoren sydämellä invertoi
  holdin painalluksen ajaksi; vasen napautus = tavallinen hold-toggle. Ei uutta
  geometriaa. Kaikki exit-polut kulkevat yhden solmukohdan kautta:
  ei-hiiren-irrotus-polut (Esc, Tab, panic, ikkunan sulku, GUI free, CmdPeriod)
  osuvat `CQGuiNightMap.cancelGestures`iin (palauttaa punchin ja päästää irti
  sääeleestä), hiiren irrotus `CQGuiNightTouch.shellUp`iin. Punch kaappaa holdin
  täsmäarvon ja palauttaa juuri sen (ei uudelleentogglea); yösmoke varmistaa
  jokaisen polun erikseen. Valinnainen Shift+1/2-näppäinpunch jätettiin pois
  (§2 vaatisi siihen käyttäjän luvan; autorepeat-karikko odottaisi silloin).
- [x] **2. Kevättulva — kokopassi** (§8). **Valmis ja hyväksytty 2026-07-26**
  (checklist N2). Mitat asuvat nyt yhdessä taulukossa, `CQNightMeasures`, ja
  `CQNightGeometry` on pelkkää matematiikkaa; törmäysmatriisi kirjoitettiin
  ensin nykyisiä vakioita vastaan ja vasta sitten vakiot liikkuivat. Rengas
  148 → 158, linnan keho 46×40 → 58×50, sisäkello 10/±8 → 14/±10, kuoren
  outerR 85 → 100, sydän Ø 5/r14 → Ø 8/r18, silmä 44/r12 → 96/r24, jakki/kanta/
  lampullinen 26/20/34 → 30/24/40, siipiväli 30 → 46, juuret 64 → 110. Viivat,
  kirjasimet ja värit koskemattomat (§16.3/§16.4/§16.10). Kuoren kerroksen
  reunusta kavennettiin 30 → 24 px — se osti minimilavan marginaalit (kuori↔linna
  8.0/12.9 px, kuori↔kompassi 16.0 px) ja on nyt itse testillä pinnattu.
  Almanakan rivit (kylvökuu, kompassi, säämerkit) jätettiin §9:lle, joka
  siirtää ne joka tapauksessa. **Hyväksytty tieto:** skip-merkki painui suusta
  46 → 56 px (kasvaneet osumalaatikot pakottavat, pidetty alarajalla) ja
  renkaasta kuoren kuihin menevät kaapelit kulkevat sen kaistan läpi (mitattu
  `tri1 → coco1_spaf` 6.0 px, ennen tulvaa 17.5 px). §4.5 pätee: kaapeli
  voittaa osoittimen alla. Merkin kotia ei siirretty.
- [x] **3. Almanakka maisemaksi** (§9). **Versio 3.2 valmis ja hyväksytty
  `nocturne-nightscape`-haaralla 2026-07-27** (checklist N3):
  - Lampi, ranta ja padiraja poistettiin. Neljä sisäänpäin osoittavaa
    muistisiementä muodostavat koko lahden läpi kulkevan bilineaarisen
    vuorovesikentän; levossa kenttää ei muuten piirretä. Fokusoimaton avoin
    vesi on inertti myös pitkällä vasemmalla vedolla.
  - Tab ja alakeskellä oleva klikattava kidus lukitsevat saman vesifokuksen;
    Shift on sen hetkellinen muoto. Fokus vie kaapelijänteet taustalle ja pois
    osumatestistä, mutta kehot, liittimet ja istutetut plugit säilyvät elävinä.
    Fokuksessa vasen veto committoi ja oikea on elastinen; fokuksen ulkopuolella
    oikea säilyy harvest-lassona.
  - Kuu on yksin oikean linnun takaisessa ylätaivaassa (Ø 56) ja käynnistyy
    0,22-sirppinä. Tap = shake, vaaka = vaihe; kylvötiheys on `0,2 × vaihe²`,
    joten neljännes/puoli/täysi kuu tuottavat keskimäärin noin 4/18/70 johtoa.
    Alas = sade/grow ja ylös = aurinko/wither: 8 px lukitsee akselin ja seuraavat
    32 px kiristävät näkyvän sään (sade 0,6…1,6 Hz, aurinko 2,5…6 Hz).
  - Siemenellä on kolme totuutta: vapaa avoin kuori, tallennetun hiljaisuuden
    suljettu tumma ydin ja elävän patchin tiheyden mukaan hohtava suonellinen
    ydin. Täysi tap palauttaa; Shift+tap tallentaa/korvaa; avoimessa Tab/kidus-
    fokuksessa tyhjän kuoren tap tallentaa myös ilman näppäimistöä. Tavallinen
    fokusoimaton tyhjän kuoren tap on inertti ja oikea tyhjentää. Täydestä
    siemenestä toiseen vetäminen breedaa; kelvollinen kohde nappaa ylimmän
    kerroksen langan ja kukkii sinisellä kaksoiskehällä.
  - Castlejen ja niiden neljän endpointin paperi on läpinäkyvä. Castlen oma
    epäsäännöllinen liha peittää renkaan vain muotonsa kohdalta; suorakulmaiset
    aukot ja niitä paikanneet lyhyet river-stubit poistuivat.
  - **Tämä pysähdys koskee vuorovettä + kuuta.** Juurten funktionaalinen
    preamp-keho (audiojuoni Cocoon ja envelope sivukasvuna) tehdään vasta tämän
    sommittelupalautteen jälkeen; pelkkää koristejuurten lisäämistä ei tehdä.
  - **Myöhemmäksi jätetty musiikillinen työ:** topology-aware “sweet spot” /
    ekologinen shake, joka suosii eläviä signaalipolkuja ja rajaa feedback- ja
    gate-populaatioita. Käyttäjä siirsi tämän tarkoituksella pois N3:n GUI-
    viimeistelystä 2026-07-27.
  - **Hyväksynnän mukana kirjattu myöhempi GUI-viilaus:** Tabin kanssa saman
    vesifokuksen avaavan kiduksen pitää voittaa sen yli kulkeva kaapelijänne
    sekä hoverissa että painalluksessa ja pysyä visuaalisesti löydettävänä
    kaapelien alta. Tämä on tietoinen, vain kidusta koskeva poikkeus §4.5:n
    cable-first-kieleen; tarkka top-layer/knockout-muoto ratkaistaan omassa
    polish-passissa eikä avaa N3:a uudelleen.
- [x] **4. Range-väreet** (§3). **Valmis ja hyväksytty 2026-07-27 (checklist
  N4).** Castlen 6-suunnan piirretty väre, jonka *muoto
  on regime* (infra/balcium/audio); napautus askeltaa. Yön suurin
  soitettavuusaukko. Kasvaneeseen maisemaan, jotta clearance tehdään kerran.
  Muoto on 1/2/5 puoliaaltoa; hover-järjestys on kaapeli → väre → sisäkello →
  keho, ja enum-järjestys luetaan CQSpecistä. Vanhat i/b/a-kirjaimet
  poistettiin käyttäjän päätöksellä; yhteinen köli erottaa väreen fm-kuun
  vapaasta aallosta, 3 px vektoriura erottaa sen takana kulkevasta joesta ja
  ankkuri seuraa linnan todellista alareunaa 13 px etäisyydellä.
- [x] **5. Dolby-osterit** (§4). **Valmis ja hyväksytty
  `nocturne-dolby-oysters`-haaralla 2026-07-27 (checklist N5).**
  In/fb-kanavien dolby-kytkin on sirpin alla osterina, jonka asento on regime
  (off/gate/duck). Napautus askeltaa; osterin ja sirpin hit/hover ovat erilliset,
  kaapeli voittaa molemmat ja enum-järjestys tulee CQSpecistä. Hyväksyntäkierros
  löysi ja sulki kaksi kerrosvikaa: osterin väistäessä kaapelia yhteinen
  controller nollasi myös kaapelin hoverin, ja org-outputin opaakki 30×30-laatikko
  katkaisi kuoren spiraalin. Night-crescentit julkaisevat nyt risteävän kaapelin
  hoverin, ja org-laatikko piirtää sisäänsä aidon kaartuvan spiraalisegmentin
  sekä säteittäisen varren ennen keltaista jakkia.
- [x] **6. Linnut hakevat** (§11). **Valmis ja käyttäjän hyväksymä
  `nocturne-birds-fetch`-haaralla 2026-07-28 (checklist N6).** Deckin lataus ja
  kansionavigointi ilman ainuttakaan nuolta: napautus = hakee (asynkroninen
  tiedostodialogi avautuu konfiguroidussa samples-kansiossa, peruutus on no-op),
  rulla alas/ylös = seuraava/edellinen 150 ms momentum-portilla, vähintään 6 px
  vaakaveto ja 6 px paluu = satunnainen. Windows/Qt-livekatselmuksen
  hyväksytyssä versiossa lintu kaappaa vedon, 60 Hz kursorivarmistus seuraa
  kättä myös pienen UserView'n ulkopuolella ja jo tunnistettu pörhistys säilyy
  leveänä tai toistuvana. Muu veto on inertti. Onnistunut
  `\file`-ilmoitus päivittää yhden
  nimilähteen ja väläyttää oman linnun 120 ms kaikilla viidellä polulla
  (dialogi, rulla, pörhistys, drop, code); kaapeli säilyy lintuhandlerin
  ensimmäisenä prioriteettina, rate-socket ja level-sirppi omina ylempinä
  sisarnäkyminään.
- [x] **7. Juuret — preamp-keho** (§10). Lähdesäikeet → drive/bias-solmu →
  audiojuoni Cocoon; `envA/B` jää post-preamp-envelope-sivusilmuksi.
  **Hyväksytty 2026-07-28 `nocturne-preamp-roots`-haaralla (checklist
  N7).** Viisi audible-
  lähdettä luetaan `CQSpec`istä (`none` ei saa omaa säiettä); leveä,
  seedattu ja epäsymmetrinen säie on klikattava enum-valinta, jonka toinen
  klikkaus kirjoittaa `none`. Solmu lukitsee 8 px jälkeen pystysuunnan driveen
  ja vaakasuunnan biasiin; kumpikin käyttää 240 px normalisoitua matkaa.
  Audiojuoni kiertää sirpit, liittyy spiraalin todelliseen 0,8-kohtaan ja
  kasvaa driven mukana 1,5/2/3 px; `SlotPre` ja env-aura vaikuttavat alfaan.
  Lintu-hover paljastaa gain-toden suoran deck→Coco-lennon ja nykyiset
  bird→preamp-lennot, juuren hover saman provenienssin vastakkaiseen suuntaan.
  Vihreä `envA/B` kasvaa ulospäin sivusilmuna, eikä 16×22-inventaario muutu.
  Livekatselmus toi yhden korjauksen: provenienssitekstin ground-pyyhkäisy
  leikkasi juuren B kahta vasemmanpuoleisinta säiettä 1280×570:ssä. Nyt
  labelin sijainti mitataan säikeiden omista käyristä pyyhkäisyn kaistalla ja
  väistyy ulospäin, kunnes 6 px vapaa väli toteutuu; erasuren mitat elävät
  yhtenä `CQNightMeasures`-arvona sekä musteessa että geometriassa.
- [x] **8. Drawer-workshop** (§12.1). **Valmis ja hyväksytty
  `nocturne-drawer-workshop`-haaralla 2026-07-28 (checklist N8).**
  Toteutettu §12.1.4:n vihreässä järjestyksessä:
  1. Kolme omistajainventaariota julistettiin erikseen — Night 31
     (`CQGuiNightMap.reachableParamNames`, koottu samoista semanttisista
     apureista joilla kehot piirretään), nykyinen Master 4
     (`CQGuiPanels.masterParamNames`) ja Workshop 39 (`CQGuiWorkshop`in oma
     manifesti) — ja `CQGuiCoverageTest` todistaa ne erillisiksi ja niiden
     unionin **täsmälleen `CQSpec.names`**iksi. Workshopin lista on
     eksplisiittinen, ei komplementti: huomenna lisätty parametri kaataa
     portin sen sijaan että laskettaisiin katetuksi ilman piirrettyä
     kontrollia.
  2. Uusi Ink-**ledger-rivi** (`CQInk` + `CQInkControl` + `CQGuiWidgets`):
     24 px, nimi vasemmalla, hiusviivaraili keskellä, tarkka monoarvo
     oikealla; unipolaarinen täyttö vasemmalta, bipolaarinen keskimerkistä
     (`CQSpec.centerNorm`), int napsahtaa, toggle ja enum istuvat vakaille
     istuimille `CQSpec.enumValues`-järjestyksessä. Raili julkaistaan
     kerran kitistä, joten piirto ja osumatesti ovat sama suorakaide.
  3. Yksi atominen muutos vei neljännen sivun Drawerin ja shellin läpi **ja**
     poisti Dayn mic-kontrollin samassa commitissa; välivaihetta kahdella
     interaktiivisella mic-polulla ei ollut missään vaiheessa.
  4. Drawer-smoke avaa nyt Night → Drawer → Workshop 1280×720:ssä ja
     uudelleen 1920×1005:ssä molemmilla skineillä.
  Drawerin järjestys oli tässä N8-inkrementissä
  `inspector · workshop · spellbook · memory` (AIO lisää myöhemmin oikealle
  teknisen Audio-sivun), ja
  sivulista julkaistaan kerran (`CQGuiDrawer.tabs`) shellin välilehtiriville,
  pinoindeksille ja `CQGuiShellState`in valkolistalle. Ei DSP-, CQSpec-
  semantiikka-, Day-poisto- tai undo-muutoksia. **Valmis ja hyväksytty
  2026-07-28 (checklist N8):** viisi visuaalista palautekierrosta ja viimeinen
  hover-/valikko-/sivuhuomautuspassi ovat vihreitä.
- [x] **9. Day-tuomio** (§0). **Valmis ja käyttäjän hyväksymä
  `nocturne-night-only`-haaralla 2026-07-28 (checklist N9).** Dayn
  runtime-rakenne, toolbar-vaihto,
  neljän sarakkeen rakentajat ja kaksi kuollutta yleisnäkymää on poistettu
  omana peruutettavana porttinaan. Spesal-tähdistön pysyvä yleisnäkymä kuolee,
  koska Nightin osoittimen alla näkyvä kytkentäsointu kertoo saman topologisen
  totuuden juuri step-eleen kohdalla. Staattinen Coco-muistipolkukartta kuolee,
  koska se ei ole interaktiivinen eikä live; Nightin kuoret ja Workshopin
  täsmäkontrollit kattavat sen käytännöllisen tiedon. Automaattinen ensimmäinen
  portti: `CQ_COMPILE_OK`, **13 033 unit-passia / 0 failurea / 32/32 luokkaa**,
  `CQ_SAFETY_PASSED`, kaikki neljä GUI-marker-testia sekä
  `CQ_PAINT_PROFILE_DONE`. Oikea soitto- ja visuaalinen katselmus hyväksyi
  Nightin itsenäiseksi soittimeksi ja molempien yleisnäkymien kirjatut kuolemat.

## Korvapassi — musikaaliset hienosäädöt (§13)

Riippumaton yllä olevasta GUI-järjestyksestä, voi kulkea limittäin; kukin
kohta on oma kuunteluiteraationsa (hypoteesi → yksi muutos → koko patteri →
3–5 askeleen kuunteluskripti → kuvaus → säätö tai peruutus). **Yksi muuttuja
per sessio.** Suositeltu järjestys hyöty/riski: 13.1 feedback-taper → 13.4
chaos-käyrä → Space/reverbin konseptipäätös ja kuuntelu → 13.2 speed-lattia
→ 13.5 VCA-laki → 13.3 dust. Kohta 13.6 zipper-mittaus ja 13.8 preampin
mahdollinen äänenvoimakkuuskompensaatio tehdään tämän jälkeen samassa
mittaus-/equal-loudness-vaiheessa: ensin todetaan ongelma, vasta sitten
auditoidaan korjaus. Kohta 13.7 on punch (= kohta 1 yllä). Lopuksi suljetaan
mahdolliset korvakokeissa löytyvät täsmävelat. Nightin kokomittakaavan
polish-portti on jo hyväksytty eikä kuulu avoimeen jonoon. Korvapassin jälkeen
edetään Audio I/O Setupiin, sen toiminnalliseen hardware-katselmukseen,
erilliseen pitkäkestoiseen laitteisto-/resource-soakiin ja kolmeen huokoiseen
soittotilaan; vasta niiden jälkeen Norns-porttiin. Jokainen hyväksytty
DSP-muutos ajaa myös
`nrt_safety`n ja
`nrt_hold_integrity`n ja päivittää SPEC §5:n samassa commitissa.

`nocturne-feedback-taper`-haaran 13.1-kandidaatin automaattinen portti
2026-07-28: `CQ_COMPILE_OK`, **13 095 unit-passia / 0 failurea / 32/32
luokkaa**, `CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`,
`CQ_GUI_DRAWER_PASSED`, `CQ_GUI_DRAWER_REENTRY_PASSED` ja
`CQ_GUI_NIGHT_PASSED`. `CQ_HOLD_OK` kantaa, koska muutos koskee vain
normalisoidun käden map/unmap-karttaa, ei DSP:tä tai tallennettuja arvoja.
Ensimmäisen korvakokeen jälkeen feedback-sirppi sai samojen CQSpec-
breakpointien diskreetin vihreä/meripihka/punainen asteikon ja nykykohdan
helmen. Käyttäjä vahvisti sen toimivaksi ja näkyväksi. Semanttinen
asteikkoaudit lisäsi SP.AF:lle nollaloven ja inverso/verso-puoliskot, inputille
unity/overdrive-rajan sekä linnun levelille neutraalin unity-viivan. Speed,
storm iris ja juurieleet jäivät tarkoituksella ennalleen. Käyttäjä hyväksyi
taperin sekä kaikki nämä lisäykset 2026-07-28; 13.1 on suljettu.

13.4:n kuuntelukandidaatti käyttää `qChaos`-käyrää **+2.0**: faderin
puoliväli mapataan arvoon ≈ 0.269 ja 3/4-kohta arvoon ≈ 0.545. Tämä vastaa
§13.4:n tavoiteltuja käsituntuman maamerkkejä täsmällisemmin kuin luonnoksen
alustava +2.5-arvio. DSP-arvoalue, suorat `CQ.set`-arvot ja snapshotit eivät
muutu. Automaattinen portti 2026-07-28: `CQ_COMPILE_OK`, **13 106
unit-passia / 0 failurea / 32/32 luokkaa** ja `CQ_GUI_NIGHT_PASSED`.
Ensimmäinen kuuntelu hyväksyi alueiden dramaturgian. Käsituntumakatselmuksessa
silmän myrskysäteet jäivät liian pieneksi paikkatiedoksi, joten hover/drag
paljastaa nyt neljä rikkonaista sääkehää täsmälleen ¼-, ½-, ¾- ja
täysi-kohdissa sekä niiden läpi kulkevan elävän rintaman. Samalla korjattiin
puuttunut silmän hover-palaute ja näkyvä sääkeho otettiin kokonaan saman
kaapelipriorisoidun osuma-alueen sisään. Laajennetun kandidaatin portti:
`CQ_COMPILE_OK`, **13 116 unit-passia / 0 failurea / 32/32 luokkaa** ja
`CQ_GUI_NIGHT_PASSED`. Käyttäjä hyväksyi sääkehät ja hoverin
visuaalikatselmuksessa 2026-07-28; 13.4 on suljettu.

13.2:n kuuntelukandidaatti (vaihtoehto **A**) toteutettiin 2026-07-29:
Speed-nupin lattia 3000 → **1500 Hz** ja modulaation jälkeinen kova
kellorajoitin 1500 → **750 Hz**, eli ground jack -temppu säilyy täsmälleen
oktaavina nupin oman ulottuvuuden alapuolella. Katto, oletus, exp-käyrä ja
kaikki tallennetut arvot ovat ennallaan; vanhat snapshotit osuvat uuden
alueen sisään, joten migraatiota ei ole. Rajoitin ei ole enää literaali
`\cqCoco`:ssa vaan `CQSpec.speedClampFloor`, koska se on Speed-parametrin
aluetosiasia; yksikkötesti pinnaa sen täsmälleen oktaaviksi nupin lattian
alle. **DSP muuttui, joten koko patteri ajettiin:** `CQ_COMPILE_OK`,
**13 147 unit-passia / 0 failurea / 32/32 luokkaa**, `CQ_SAFETY_PASSED`,
`CQ_HOLD_OK` (A4-korrelaatio 1.0, A5-drift 0.0 dB) sekä kaikki neljä
GUI-markkeria. `\mayhem`in ±4 oktaavin spaf painaa kellon nyt aidosti
750 Hz:n lattialle, ja DC-assert kestää selvällä marginaalilla
(8e-05 / 2.8e-04 vs. katto 0.02) — LeakDC tekee sen mitä §13.2 toivoi,
eikä mittaus siis pakota vaihtoehtoon B. **Ei vielä hyväksytty:** portti
odottaa korvaa. Checklist-lohko 13.2 kysyy ensin uuden pohjan
soitettavuuden, mutta sen ratkaiseva kohta on keskialue — fader kattaa nyt
viisi oktaavia neljän sijasta, ja jos tarkkuus katosi käyttökaistalta,
kirjataan vaihtoehto B (lattia 2000, rajoitin jää 1500:aan).

**Käyttäjä hyväksyi 13.2:n vaihtoehto A:n 2026-07-29.** Äänialueet ovat
paikoin äärimmäisiä, mutta se sopii kokeelliseen musiikkiin; mahdollinen
hienosäätö jää myöhemmäksi eikä vaihtoehto B:tä tarvittu.

Saman katselmuksen löytämä vika korjattiin heti erillisenä porttinaan
(checklist **N10**): **kuoren sydämen ja kelan hover/painallus olivat eri
mieltä.** Hitailla nopeuksilla kelan helmi on sydämen osuma-alueen sisällä
(mitattu ennen korjausta: norm-0-helmi 14 px keskipisteestä minimilavalla,
21 px koossa 1920×855, sydämen osumasäde 22 px), joten hover kirkasti
spiraalin mutta painallus kytki holdin. Syy oli rakenteellinen: osumaketju
oli kirjoitettu kahdesti — `shellDown` kysyi sydäntä, `prHoverShell` ei
kysynyt sitä lainkaan. Nyt molemmat kysyvät saman puhtaan funktion
`CQNightGeometry.shellTargetAt`, joka on §4.5:n laki rakenteena eikä
konventiona; neljä vihjettä ovat samalla toisensa poissulkevia ilman
käsin kirjoitettuja `.not`-ehtoja. **Sydän voittaa oman kiekkonsa**, koska
se on pieni, kiinteä ja seurauksiltaan raskas (harhanapautus aloittaa tai
lopettaa äänityksen), kun taas kelaus on kulmaele ja onnistuu koko
viisikierroksisen uran matkalta — spiraalin silmän luovuttaminen ei siis
maksa kelalle mitään. Sydän sai käyttäjän luvalla oman hover-kehän, joka
piirretään sen **todelliseen 22 px osumasäteeseen** eikä nupin ympärille:
vihje kertoo silloin myös sen, mihin kela loppuu. Portti:
`CQ_COMPILE_OK`, **13 217 unit-passia / 0 failurea / 32/32 luokkaa** ja
kaikki neljä GUI-markkeria; `CQ_SAFETY_PASSED` ja `CQ_HOLD_OK` kantavat
saman päivän 13.2-portista, koska tämä inkrementti ei koske DSP:hen.
Samassa katselmuksessa löytyi myös, ettei mouth-chevronilla ollut lainkaan
hover-vihjettä, vaikka §4.5 lupaa sellaisen jokaiselle painettavalle
kontrollille. Käyttäjä hyväksyi sen lisäämisen 2026-07-29. Chevron vastaa
**painolla** (hiusviiva → normaali) eikä värillä: se lepää jo `cas`-sävyssä
(193,186,173), joka on kirkkaampi kuin `line` (169,174,184) johon sen kaksi
siskomerkkiä — skip-glyfi ja range-väre — kirkastuvat, joten värivaihto voisi
vain himmentää sen, ja `spark` tarkoittaa jo "jokin juuri laukesi". Paino on
saman kuoren kelalovi-vihjeen kanaava, joten yksi keho puhuu yhtä
hover-kieltä eikä yksikään lepotilan muste liiku (§16.3/§16.10).

**Käyttäjä hyväksyi N10:n lopullisen visuaalisen portin 2026-07-29:**
sydämen todellinen 22 px hover-kehä ja chevronin painonlisäys ovat luettavat,
vihje ja teko ovat samaa mieltä ja ulomman kierroksen kautta aloitettava
hitaimman pään kelaus on hyväksyttävä.

**13.5 VCA-laki (hyväksytty 2026-07-29, kiinteä p = 2).** §13.5:n
luonnos esitti lain muodossa `gain = (1 + sum) ** p`, mutta koodin todellinen
muoto oli `gain = (nuppi + summa).clip(...)`: nuppi on VCA:n lepoasento ja
matriisi lisää siihen. Koko gainin suora muotoilu olisi siirtänyt nupin
kalibrointia ja uhannut dokumentoitua tapaa avata kiinni käännetty VCA
banaanilla. Siksi hyväksytty laki muotoilee vain modulaation **osuuden**:
`shaped = sign(1+sum)·|1+sum|² − 1`; summa rajataan ±2 ennen muotoilua.

Tämä säilyttää nupin arvon silloin kun summa on nolla, mutta **ei** säilytä
VCA-banaaneja sisältävän snapshotin tai presetin soundia: sen painot saavat
uuden vasteen. Myös alkuperäinen “täsmälleen sama laki ykkösessä” pätee vain
muotoiltuun väliarvoon. Todellinen input-VCA rajataan sen jälkeen 0–2:een ja
feedback-VCA 0–2,5:een, joten luonnoksen summa +1 → ×4/+12 dB ei toteudu;
p = 2 saavuttaa olemassa olevan katon aikaisemmin ja viipyy siellä pidempään.
Vastaavasti summa −1 **vähentää yhden**; se sulkee VCA:n vain, jos nuppi on
enintään ykkösessä. Nämä eivät ole turvallisuusvikoja vaan portin keskeisiä
musikaalisia eroja.

Saman session A/B/C vertasi lineaarista, p = 1,5:tä ja p = 2:ta. p = 2
säilytti 0,05–0,10-painoilla hienovaraisen hengityksen; 0,20 teki fraaseja,
0,40 syvän mutta pyöreäreunaisen liikkeen ja vasta 0,80 täydet aukot.
Positiivinen kaari kävi katossa jähmettymättä, feedback-painot −0,20/−0,50
tekivät erisyvyisiä kuoppia ilman tyhjiä paloja, ja SP.AF-verrokki sekä
kokonaiset presetit pysyivät hallinnassa. p = 1,5 jäi p = 2:n
kaapelipainolla saavutettavaksi välimuodoksi. Lineaarisella oli oma identiteetti,
mutta ei testeissä omaa välttämätöntä soittotehtävää; siksi uutta
character-kytkintä ei lisätty. Tilapäinen audition-kytkin ja p = 1,5 -DSP-haara
poistettiin hyväksynnässä.

Lopullinen portti: `CQ_COMPILE_OK`, **13 276 unit-passia /
0 failurea / 33/33 luokkaa**, `CQ_SAFETY_PASSED` ja `CQ_HOLD_OK`
(A4-korrelaatio 1.0, A5-drift 0.0 dB). `\mayhem` pysyy turvallisesti
masterin 0,89-limitterin alla, mutta limitteriin osuva huippu todistaa
turvallisuuden, ei tekstuurin tai dynamiikan säilymistä; ne hyväksyttiin
erikseen manuaalisessa portissa.

Space-keskustelun ensimmäinen päätös 2026-07-28: Silhouette hylättiin tämän
soittimen sisältä. Tuotanto-FDN:ään, `CQSpec`iin tai GUI:hin koskematon
`prototypes/space_lab.scd` rakensi yhden vertailtavan kuuntelusession. Neljä
unity-palautukseen 0.0–0.4 dB:n sisään kalibroitua kandidaattia ovat tumma
utility-FDN, todellisia Coco-organ-kelloja
seuraava clock field, Coco-puskureiden kirjoitukseen koskematon memory halo sekä
ei-reverb `confluence`, jossa kumpikin Coco uudelleennäytteistää toista oman
kellonsa reunoilla. Yksi kerrallaan ajettava labra ohittaa vanhan placeholderin
palautettavasti mutta käyttää sen send/return-väyliä ja koko `cqMaster`-
turvaketjua. Automaattinen portti: `CQ_SPACE_LAB_COMPILE_OK`,
`CQ_SPACE_LAB_NRT_PASSED`; kaikki neljä renderiä ovat ei-hiljaisia, finitejä,
DC-rajattuja, keskenään eri signatuurisia ja master-katon alla. Tämä ei vielä
hyväksynyt mitään kandidaattia eikä muuttanut Space-parametrilistaa.

Kuuntelutuomio 2026-07-28: `utility` hylättiin liian tavanomaisena,
`clockField` havaittiin mittauksesta huolimatta vailla selvästi kuultavaa
identiteettiä ja `confluence` lähinnä stereokenttää sekoittavana ilman uutta
merkityksellistä soittotapaa. `memoryHalo` oli ainoa jatkoajatusta synnyttänyt
luonnos, mutta kiinteillä lukupääsuhteilla liian ennalta arvattava. Se
säilytetään tutkimusmateriaalina tulevalle muistilukutilalle, ei hyväksyttynä
efektinä. Nykyistä Space/FDN-linjaa ei jatketa.

- [x] 13.1 feedback-taper (**hyväksytty 2026-07-28, myös asteikkolisäykset**) ·
  [x] 13.2 speed-lattia (**hyväksytty 2026-07-29, vaihtoehto A**) ·
  [x] 13.3 dust + `selfNoise` (**hyväksytty 2026-07-29; Bits-eristys on
  myöhempi ei-estävä testi**) ·
  [x] 13.4 chaos-käyrä (**hyväksytty 2026-07-28, myös sääkehät**) ·
  [x] 13.5 VCA-laki (**hyväksytty 2026-07-29, kiinteä p = 2**) ·
  [x] 13.6 zipper-mittaus (**hyväksytty 2026-07-29, ei kuulu luotettavasti;
  ei DSP-muutosta**) ·
  [x] 13.8 preamp loudness compensation (**hyväksytty 2026-07-29;
  −1 dB/okt valittu, toteutettu ja vahvistettu live-inputilla**)

**13.6 suljettu ja 13.8:n tuotantoehdokas toteutettu 2026-07-29.**
`nrt_zipper_measure.scd` korjasi ensin lähtötiedon: matriisipainot eivät ole
lagittomia, vaan `prMatrixWeight` on silottanut kaikkia kohteita 50 ms:llä
M1:stä asti. Hidas 30 Hz shared-cell -liuku jäi jatkuvasta verrokista
−55,87 dB RMS:n päähän (`CQ_ZIPPER_MEASURED`). Käyttäjä ei erottanut
zipperiä luotettavasti ja piti sitä todennäköisesti kuulumattomana, joten
tuotantopolkuun ei tehty muutosta. Historiallinen helper edeltää myös
flip/skip/spesal-Schmittejä; sen mahdollinen erottelu on oma
porttisemantiikan auditointi, ei zipper-korjaukseen piilotettava sivumuutos.

`nrt_preamp_loudness.scd` teki puheesta, rumpuloopista ja sustain-urusta
vanhan kompensoimattoman sekä −1/−1,5 dB per todellisen Drive-oktaavin
koeraidat (`CQ_PREAMP_OK`). Vanhan polun voimakas tasohyppy peitti
säröelettä; käyttäjä
valitsi käyttökelpoisimmaksi välimuodoksi −1 dB/okt-version, joka hillitsee
karkaamista mutta ei tasapäistä painetta. Tuotanto käyttää nyt lakia
`dbamp(−log2(drive))` waveshaperin jälkeen ja ennen 20 Hz HPF:ää; envelope
follower lukee kompensoitua audiosignaalia. Parametria, GUI:ta tai
snapshot-migraatiota ei lisätty. Drive 40 säilyttää valitulla lailla
materiaalista riippuen vielä +10,77…+16,84 dB RMS-nousun Drive 1:een nähden.
Tiedosto-A/B ja valitun tuotantopolun live-input-koe on hyväksytty.

Tuotanto-DSP:n jälkeinen automaattinen portti: `CQ_COMPILE_OK`,
**13 340 unit-passia / 0 failurea / 33/33 luokkaa**, `CQ_PREAMP_OK`,
`CQ_SAFETY_PASSED`, `CQ_HOLD_OK`, `CQ_DUST_OK` ja `CQ_SELFNOISE_OK`.
N10:n `CQ_NIGHT_CAPTURE_DONE` sekä muuttumattoman matrix-polun
`CQ_ZIPPER_MEASURED` ovat myös vihreät.

## Norns-portti (§14) — jonon viimeinen, ei korvapassin välitön seuraaja

Tavoiteympäristö norns + grid + 16n (+ crow). Puhdas ydin
(`CQSpec/CQState/CQMatrix/CQSynthDefs/CQCompass` + turvaketju) siirtyy
CroneEngineen, Lua korvaa GUI:n ja 16n:n, grid on oma uusi keho omalla
speksillään. Dietti otetaan lahjana: 8-bit-rakeisuus ja epätarkkuus ovat
identiteettiä, kalliit osat ovat kohteliaisuuksia (FDN → nornsin reverb,
recorder → tape, dekit → softcut lähteenä). Askelmerkit N0–N6 ja karikot
`NIGHT_STANDALONE_PLAN.md §14`:ssä. Ei aloiteta ennen kuin night-first on
soitettu valmiiksi. Tämän osion sijainti korvapassin checklistin vieressä on
vain §-viittauksen vuoksi eikä toteutusjärjestys: ennen Nornsia valmistuvat
Audio I/O Setup, erillinen pitkä laitteisto-/resource-soak ja kolme huokoista
soittotilaa.

## Jokaisen osan välitarkastus (sitova)

Jokainen käyttäjälle näkyvä osa tehdään näin:

1. Toteuta pienin ajettava ensimmäinen versio ja sen käyttäytymistestit.
2. Aja pakolliset portit (alla) sekä osan oma GUI-smoke.
3. Näytä versio käyttäjälle ja kerro myös keskeneräiset kohdat. **Pysähdy
   visuaalista/kuuntelupalautetta varten.**
4. Tee viimeistely palautteen perusteella. Älä aloita seuraavaa näkyvää
   kokonaisuutta ennen palautetta.
5. Viimeistään hyväksytyn milestone-tarkastuksen jälkeen tee commit ja push
   `nocturne`-haaraan.

## Jokaisen portin hyväksyntä

Automaatio ajetaan tässä järjestyksessä ja onnistuminen määräytyy viimeisestä
markerista, ei prosessin silmämääräisestä tulosteesta:

1. `tests/compile_check.scd` → `CQ_COMPILE_OK`
2. `tests/run_all.scd` → `CQ_TESTS_PASSED`
3. `tests/nrt_safety.scd` → `CQ_SAFETY_PASSED`
4. DSP-käyttäytymisen muutoksissa `tests/nrt_hold_integrity.scd` → `CQ_HOLD_OK`

Lisäksi neljä GUI-smokea (`gui_shell_smoke`, `gui_drawer_smoke`,
`gui_drawer_preview_reentry_smoke`, `gui_night_smoke`) sekä relevantit
`GUI_SPEC.md` §13:n serverless interaction-testit, manuaalinen kuuntelu,
visuaalinen tarkastus 100/125/150 % skaalauksilla ja CPU/resource-soak.
G-porttia ei merkitä valmiiksi pelkän toteutuscommitin perusteella.
Lähtötaso 2026-07-26 (kevättulvan portti, `nocturne` = `a5e4b17`):
`CQ_COMPILE_OK`, **11 175 unit-passia / 0 failurea** (oli 6 611 — §8:n
törmäysmatriisi tuo ~4 560 assertia molemmilla referenssikoolla),
`CQ_SAFETY_PASSED` ja kaikki neljä GUI-markkeria. `CQ_HOLD_OK` kantaa
edellisestä portista: DSP:hen ei koskettu.

Haaran `nocturne-nightscape` portti 2026-07-27 (§9 v2, **vihreä mutta ei
hyväksytty**): `CQ_COMPILE_OK`, **11 525 unit-passia / 0 failurea / 28/28
luokkaa**, `CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED`, `CQ_GUI_NIGHT_PASSED`; `CQ_HOLD_OK` kantaa.
Samalla haaralla on **`run_all.scd`:n porttikorjaus**: kesken kaatunut ajo
tulosti ennen sekä `CQ_TESTS_FAILED`in että `CQ_TESTS_PASSED`in (sclangin
`try` ei kestä rutiinin `0.1.wait`-yieldiä), joten summary kertoo nyt myös
suoritettujen luokkien määrän eikä passed-markeri lähde ilman sitä. Se on oma
committinsa ja kelpaa `nocturne`en riippumatta §9:n kohtalosta
(`git cherry-pick`).

Saman haaran portti 2026-07-27 (§9 v3, **ensimmäinen vuorovesikentän
katselmointiversio; vihreä mutta ei hyväksytty**): `CQ_COMPILE_OK`,
**11 206 unit-passia / 0 failurea / 28/28 luokkaa**, `CQ_SAFETY_PASSED`,
`CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED` ja `CQ_GUI_NIGHT_PASSED`. Assertimäärän lasku
v2:sta johtuu poistetun lampigeometrian testipinnan korvaamisesta
vuorovesikentän käyttäytymistesteillä, ei ohitetusta testiluokasta.
`CQ_HOLD_OK` kantaa: DSP:hen ei koskettu.

Saman haaran portti 2026-07-27 (§9 v3.1, **Tab/kidus-fokus, luettavat
siemenet, breed-kohdekukinta ja linnojen kerroskorjaus; vihreä mutta ei
hyväksytty**): `CQ_COMPILE_OK`, **11 252 unit-passia / 0 failurea / 28/28
luokkaa**, `CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`,
`CQ_GUI_DRAWER_PASSED`, `CQ_GUI_DRAWER_REENTRY_PASSED` ja
`CQ_GUI_NIGHT_PASSED`. Tavallinen vasen veto avoimessa vedessä on nyt
passiivinen; Tab tai klikattava kidus lukitsee soitettavan vesifokuksen ja
Shift avaa sen hetkeksi. Breed-suhde ja validin kohteen sininen kukinta
kulkevat päällimmäisellä läpinäkyvällä elekerroksella. Linnojen ja niiden
neljän yksityiskohtanäkymän paperi on läpinäkyvä, eikä geometria enää tuota
synteettisiä jokitappeja. `CQ_HOLD_OK` kantaa: DSP:hen ei koskettu.

Saman haaran portti 2026-07-27 (§9 v3.2, **pieni sirppi, kasvava sääjännite
ja siementen kolme luettavaa elintilaa; vihreä ja käyttäjän hyväksymä**):
`CQ_COMPILE_OK`, **11 269 unit-passia / 0 failurea / 28/28 luokkaa**,
`CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED` ja `CQ_GUI_NIGHT_PASSED`. Kuu avautuu
määrästä 0.22 ja kylvää epälineaarisesti (`0.2 × vaihe²`), joten pieni sirppi
tekee pieniä patcheja ja täysi kuu kasvaa voimakkaammin. Sateen ja auringon
vedon syvyys ohjaa sekä merkin runsautta että toiston tahtia; sade on nyt
aurinkoa harkitsevampi. Tyhjä kuori, talletettu hiljainen ydin ja elävä siemen
ovat eri tilat. Tavallinen klikkaus ei kirjoita tyhjään kuoreen: Shift-klikkaus
tallentaa, ja avoin Tab/kidus sallii saman ilman näppäimistöä. Topologiaa
ymmärtävä “sweet spot” -shake jäi tietoisesti myöhemmäksi musiikilliseksi
työksi. `CQ_HOLD_OK` kantaa: DSP:hen ei koskettu.

Haaran `nocturne-range-ripples` v2-portti 2026-07-27 (§3, **ääni,
GUI-kirjoitus ja visuaalinen v2 käyttäjän hyväksymät, checklist N4**):
`CQ_COMPILE_OK`, **11 365 unit-passia / 0 failurea / 28/28 luokkaa**,
`CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED` ja `CQ_GUI_NIGHT_PASSED`. Yösmoke askeltaa
väreen koko enum-kierroksen, tarkistaa funnel-päivityksen ja istuttaa oikean
kaapelin väreen yli todistaakseen cable-first-hoverin ja -painalluksen.
Kirjaimeton köli/aalto-perhe, 13 px todellisen kehonreunan hengitys ja joen
ylityksen paikallinen vektoriura hyväksyttiin toimiviksi.
`CQ_HOLD_OK` kantaa: DSP:hen ei koskettu.

Haaran `nocturne-dolby-oysters` hyväksytty portti 2026-07-27 (§4,
**visuaalinen ja kuuntelukatselmus hyväksytty, checklist N5**): `CQ_COMPILE_OK`,
**12 162 unit-passia / 0 failurea / 28/28 luokkaa**, `CQ_SAFETY_PASSED`,
`CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED` ja `CQ_GUI_NIGHT_PASSED`. Yösmoke askeltaa
osterin koko off/gate/duck-kierroksen, varmistaa ettei osterista alkava veto
liikuta VCA-sirppiä, ja istuttaa oikean kaapelin osterin yli todistaakseen
cable-first-hoverin ja -painalluksen. `CQ_HOLD_OK` kantaa: DSP:hen ei koskettu.
Hyväksynnässä havaittu päällekkäisen kaapelin tyhjä hover korjattiin: smoke
varmistaa nyt myös, että osterin väistyessä `hoverCable` saa risteävän kaapelin.
Samassa portissa org-jakin opaakin laatikon alle jäänyt playhead-spiraali
palautettiin piirtämällä laatikkoon täsmälleen kuoren oma käyrä ja varsi.
Ensimmäinen compile-ajo paljasti myös pysyvän dev-runner-aukon: luokkakirjaston
parse-virheessä sclang ei suorita `compile_check.scd`:n `exit`iä vaan jää
interaktiiviseksi. `tests/run_sclang_gate.ps1` antaa porteille nyt ulkoisen
aikarajan, säilyttää virhetulosteen ja lopettaa vain itse käynnistämänsä PID:n.
N8:n viimeistelyssä 2026-07-28 sama virhe toistui suoralla agenttikomennolla,
joten suojaus tehtiin rakenteelliseksi: `AGENTS.md` kieltää agentilta suoran
ajon, wrapper käyttää aina sclangin daemon-tilaa, validoi skriptikohtaisen
markerinsa ja kirjoittaa jokaisesta ajosta diagnostiikkalokin
`tests/tmp/gate-logs/`-hakemistoon. Myös CI kulkee nyt saman wrapperin kautta.

Haaran `nocturne-birds-fetch` hyväksytty portti 2026-07-28 (§11,
**visuaalinen ja soitettava katselmus hyväksytty, checklist N6**):
`CQ_COMPILE_OK`, **12 174 unit-passia / 0 failurea / 28/28 luokkaa**,
`CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED` ja `CQ_GUI_NIGHT_PASSED`. Yösmoke käyttää
injektoitua dialogia ja deck-dispatcheria, joten se avaa oikean natiivipaneelin
sijaan testipolun ja varmistaa dialogin tuloksen/peruutuksen, pyörän
aloituskansion, momentum-portin, yhden luontevan ulos–takaisin-pörhistyksen,
muun vedon inerttiyden, exit-keskeytyksen, `\file`-auktoriteetin 120 ms
välähdyksen sekä lintuhandlerin cable-first-hoverin ja -painalluksen.
Livekatselmuksen kaksi korjauskierrosta vaihtoivat dialogin aloituskansioksi
projektin konfiguroidun `samples`-kansion ja tekivät pörhistyksestä aidossa
Windows/Qt-ikkunassa luotettavan: käsitelty drag ei vuoda lavalle, globaalia
kursoria seurataan painalluksen ajan ja selvä suunnanvaihto säilyy kelvollisena
myös leveässä tai toistuvassa liikkeessä. Lopullinen yksikkö- ja Night-smoke
ovat vihreät; muut GUI- ja NRT-portit kantavat saman inkrementin v1-portista,
koska korjaukset eivät koskeneet DSP:tä tai muita näkymiä.
`CQ_HOLD_OK` kantaa: DSP:hen ei koskettu.

Haaran `nocturne-preamp-roots` **hyväksytty portti 2026-07-28** (§10,
checklist N7): `CQ_COMPILE_OK`,
**12 219 unit-passia / 0 failurea / 28/28 luokkaa**, `CQ_SAFETY_PASSED`,
`CQ_HOLD_OK`, `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED` ja `CQ_GUI_NIGHT_PASSED`. Night-smoke todistaa
CQSpecistä johdetun säie-hoverin, lähteen valinnan ja `none`-togglen,
akselilukitun drive/bias-kirjoituksen, slot-gainien saapumisen reittipiirtoon
sekä cable-first-prioriteetin. Yksi hiiriläpäisevä topologiataso pitää kaikki
hover-reitit ja vaaleat provenienssitekstit yhtenäisinä paikallisten Qt-paperien
yli; vihreä env-output on yksi tangentiaalinen cubic läpinäkyvän jakin alla.
Seedattu geometria testaa jokaisen väljemmän säiesuun osuma-alueen,
deck→Coco / deck→kumpi tahansa preamp -reittien todelliset päätepisteet ja
output-haaran jatkuvuuden. Qt-kuvapassi tarkisti levon, lähde-hoverin,
juuriprovenienssin ja lintu-hoverin suoran deck-reitin. §13.8:n
loudness-kompensaatio kirjattiin myöhemmäksi kuuntelukokeeksi; DSP:hen ei
koskettu tässä portissa.

Saman haaran **hyväksytty loppupassi** 2026-07-28 (N8 v6, käyttäjän viidennen
testauskierroksen palaute; **valmis ja hyväksytty**): `CQ_COMPILE_OK`,
**13 027 unit-passia / 0 failurea / 31/31 luokkaa**,
`CQ_GUI_DRAWER_PASSED` ja `CQ_SAFETY_PASSED`; DSP:tä ei muutettu.
Memoryn `outlined knots differ…` käyttää nyt 9/11 px tiny-rankia ja sen sekä
`decks`-otsikon välissä on kaksi eksplisiittistä riviväliä. Inspectorin
matriisi antaa täsmäsolulle kevyen spark-pesun ja ääriviivan. Inspectorin
preset ja Memoryn kaksi Morph Bridge -valitsinta jakavat uuden `CQInkMenu`-
kontrollin: sekä suljettu laatikko että borderless-avattu lista piirretään
soittimen paletilla, rivillä on spark-hover ja alareunan lista aukeaa
tarvittaessa ylöspäin.

Samassa portissa suljettiin neljästi toistunut dev-runner-aukko. Suora
`sclang` saattoi luokkakirjastovirheessä jäädä interaktiiviseen promptiin ja
puskuroida juuri sen virheen, jota agentti odotti. `AGENTS.md` kieltää suoran
agenttiajon, `run_sclang_gate.ps1` käyttää `-D`:tä, lapsikohtaista watchdogia,
viimeisen markerin validointia ja pysyvää lokia, ja CI käyttää samaa runneria.
Korjaus todistettiin oikealla compile-virheellä: koko diagnoosi palautui,
vain runnerin oma PID lopetettiin ja seuraava korjattu ajo meni vihreäksi.

N8:n hyväksynnän jälkeinen live-tarkistus löysi vielä kaksi automaation
väärää vihreää ja yhden toistuvan typografiakarikon. Matriisi ja avatun
`CQInkMenu`-listan rivit oli sidottu `mouseMoveAction`iin, jonka Qt lähettää
vain hiiren nappi pohjassa; tavallinen hover on `mouseOverAction`, ja popupin
oma top-level-ikkuna tarvitsee lisäksi oman `acceptsMouseOver`-luvan. Smoke
kutsuu nyt oikeaa no-button-callbackia ja todistaa rivin indeksin vaihtuvan.
Dayn cell/list-highlightia vahvistettiin. Memoryn tiny-huomautuksen korkeus
ei enää ole fonttikoko + arvattu vakio: `CQGuiScale.textBandHeight` varaa
ascent/descent-ilman, yksikkötesti lukitsee sen ja GUI_SPEC §16.4 määrää
vaaka- **ja pystyleikkauksen** tarkistettavaksi jokaisen fonttimuutoksen jälkeen.
Korjausportti: `CQ_COMPILE_OK`, **13 031 unit-passia / 0 failurea / 31/31
luokkaa**, `CQ_GUI_DRAWER_PASSED` ja `CQ_SAFETY_PASSED`.
Seuraava live-tarkistus jäljitti huomautuksen viimeisen leikkauksen sen
ympäröivään `HLayout`iin: scalar `margins_(8)` vei myös 8 px ylhäältä ja
alhaalta, joten fontin oma suurempi view ei saanut tilaansa. Rivi käyttää nyt
vain vaakainsettiä `[8, 0, 8, 0]`. Saved snapshots -lista sai valintaa
muuttamattoman rivikohtaisen hoverin, ja Memoryn kolme laatikko-otsikkoa
jakavat yhden kokoaskeleesta tulevan hieman syvemmän top-insetin.
Portti: `CQ_COMPILE_OK`, **13 035 unit-passia / 0 failurea / 31/31 luokkaa**,
`CQ_GUI_DRAWER_PASSED` ja `CQ_SAFETY_PASSED`.

Viimeinen laitetarkistus osoitti, että kumpikin jäljellä ollut vika oli Qt:n
sisäisen geometrian varassa eikä enää korjattavissa sovelluksen puolelta
arvaamalla. `outlined knots differ…` ei ole enää `StaticText`: se piirretään
Penillä mitattuun, väljään `UserView`-kaistaan, jonka smoke-portti vertaa
merkkijonon todelliseen glyfikorkeuteen. Natiivi `ListView` ei puolestaan
paljastanut todellista sisältö-/rivialkuaan, joten saved snapshots käyttää nyt
`CQInkList`iä: sama `rowHeight` ja sama indeksifunktio määräävät piirron,
tavallisen hoverin ja klikkauksen. Yleinen sääntö on kirjattu GUI_SPEC
§4.5/§16.4: tuntematonta natiivigeometriaa ei soviteta visuaaliseen
osumakohteeseen likimäärällä. Portti: `CQ_COMPILE_OK`, **13 035
unit-passia / 0 failurea / 31/31 luokkaa**, `CQ_GUI_DRAWER_PASSED` (mukana
hoverin ja klikkauksen sama riviosuma) ja `CQ_SAFETY_PASSED`.

Saman haaran **viimeistelykierros** 2026-07-28 (N8 v5, käyttäjän neljännen
testauskierroksen palaute; **vihreä mutta ei hyväksytty**): `CQ_COMPILE_OK`,
**13 027 unit-passia / 0 failurea / 31/31 luokkaa**, `CQ_SAFETY_PASSED` ja
kaikki neljä GUI-markkeria; `CQ_HOLD_OK` kantaa. Kuusi täsmäkorjausta:

- **Toolbarin otsikko tasattiin viivaansa.** Viiva piirrettiin 8 px:n
  sisennyksellä samalla kun rivin layout käytti 5:tä. Viiva on toolbarin
  tasausviiva, joten se alkaa nyt samasta sisennyksestä kuin alleviivaamansa
  rivi — ei erillisestä vakiosta.
- **Master-nuppien nimet eivät enää osu nuppiin.** Arvokaari kulkee 3 px
  ympyrän ulkopuolella ja pyyhkäisee yli laen, joten nimi tarvitsee väljyyden
  *kaaresta*, ei nupista. Nupin laatikko kasvaa nyt tekstikoon mukana
  (`CQInk.knobBoxHeight`); Day ja Night eivät välitä kokoa ja pysyvät
  pikselilleen ennallaan.
- **Loomin sarakeotsikkorivi sai ilmaa** preset-rivin ja itsensä väliin —
  ilma menee väliin, ei otsikon ja sen oman ruudukon väliin.
- **Memoryn ylämarginaali korjattu oikeasti.** VLayoutin `spacing` pätee myös
  ensimmäisen kohteen yläpuolelle, joten sivun koko sisältö oli 6 px muita
  alempana. Välit ovat nyt eksplisiittisiä välikkeitä ja spacing on 0.
- **Fingerprint-laatikon yläosa purettiin.** Laatikko piirsi oman otsikkonsa
  samaan kohtaan, jossa layout piti `live patch · now` -riviä; rivi alkaa nyt
  otsikon alta, matriisin yläreuna on johdettu molemmista, ja selittävä
  `outlined knots…` -teksti on yhden asteen pienempi — se on infoa, ei otsikko.
- Laatikot venyvät alaspäin, joten fingerprintin korkeus kasvoi 300 → 370
  (windowed) / 514 (maksimoitu).

Karikko, joka melkein meni ohi: **piilotettu StackLayout-sivu raportoi
vanhentuneet absoluuttiset koordinaatit**, joten neljän sivun tasausta ei voi
mitata kerralla — testi vertaa nyt vanhempaan nähden suhteellisia arvoja
samalla kokoaskeleella (kaikki neljä: 12 px).

Saman haaran **kehys- ja kromikierros** 2026-07-28 (N8 v4, käyttäjän kolmannen
testauskierroksen palaute; **vihreä mutta ei hyväksytty**): `CQ_COMPILE_OK`,
**13 014 unit-passia / 0 failurea / 31/31 luokkaa**, `CQ_SAFETY_PASSED` ja
kaikki neljä GUI-markkeria; `CQ_HOLD_OK` kantaa. Kokoruudun skaalaus todettiin
toimivaksi (käyttäjän läppäri 2880×1800 @ 200 %). Loput:

- **Yksi jaettu sisennys.** Railit olivat jo samassa x:ssä (mitattu 28/42),
  mutta Workshopin muste alkoi 12 px sisempää kuin muiden. Sisennys on nyt
  yksi arvo (`contentInset` 4/6), jota noudattavat Workshopin nimet, loomin
  rivinimet ja Memoryn laatikot.
- **Memory ei enää vuoda sivultaan.** Morph-palkki piirtyi sivun alarajan
  alapuolelle (mitattu alaväli 20 px); nyt sivulla on alapehmuste ja väli on
  38/42 px. Body venyy morph-palkkiin asti, joten fingerprintin solut ja
  rivinimien koko kasvavat saamansa tilan mukana.
- **Liian pieneksi jääneet tekstit kasvoivat**: loomin vinot sarakenimet
  (9 → 11 / 13 px), Memoryn laatikko-otsikot ja morphin lukema.
- **Kromi (`CQGuiScale.chrome`) nostettiin työpöydän lattiaan**: toolbarin
  otsikko ja kohteet, drawerin välilehdet sekä master-strippi. Master-strippi
  pysyy 118 px korkeana ja REC/PANIC pitävät §16.7:n osumalaatikkonsa — vain
  muste kasvoi. Napit mitataan nyt omasta tekstistään, joten kasvava fontti
  ei voi leikata kuvausta. Kromi **ei** seuraa ikkunan kokoa: sen korkeus on
  kiinteä ja sen sommittelu on lihasmuistia (§16.1).
- Sivun inset johdetaan nyt kromista (`pageInset`), joten välilehtirivin
  korkeuden muutos ei enää siirrä hiljaa sitä, mihin kokoaskeleeseen ikkuna
  kelpaa. Sama kierros paljasti, että Qt voi asetella lavan **useammalla kuin
  yhdellä läpimenolla**: `applyScale` palauttaa nyt tiedon siitä liikkuiko
  mikään, ja synkronointi kysyy vielä kerran ja pysähtyy heti kun mikään ei
  liiku.

Saman haaran **marginaali- ja kokoaskelkorjaus** 2026-07-28 (N8 v3, käyttäjän
toisen testauskierroksen palaute; **vihreä mutta ei hyväksytty**):
`CQ_COMPILE_OK`, **13 012 unit-passia / 0 failurea / 31/31 luokkaa**,
`CQ_SAFETY_PASSED` ja kaikki neljä GUI-markkeria; `CQ_HOLD_OK` kantaa.
Kaksi vikaa:

1. **Sivumarginaalit eivät olleet samat.** Inspector käytti `margins_(4)`,
   Spellbook ja Memory 12:ta ja Workshop omaa sisäistä paddiaan. Kaikilla
   neljällä on nyt **yksi jaettu sivukehys** (`CQGuiScale.pageFrame`): sama
   ilma välilehtirivin ja ensimmäisen musteen välissä, sama keskitetty raili.
   Alamarginaali saa vaihdella, koska sisällöt ovat eri. Memoryn body
   venyy nyt morph-palkkiin asti, ja fingerprintin solut **ja rivinimien
   koko** kasvavat sen mukana.
2. **Kokoaskel ei lauennut maksimoitaessa — ja se oli oikea bugi.** Roomyn
   kynnys oli kirjoitettu käsin (1400×640) samalla kun askeleen oma
   minimikorkeus oli 624. Windows-läppärit ajetaan yleisesti 125 %:n
   skaalauksella ja sclang laskee loogisia pikseleitä, joten maksimoitu
   1080p-näyttö raportoi sivun ~1528×**630** — kaksi pikseliä liian matala.
   Kynnykset **johdetaan nyt askeleen omista mitoista**
   (`minimumWidth`/`minimumHeight` + `comfort`), ja roomyn rivit tiivistettiin
   30 → 28 px, jolloin vaatimus on 1356×600 ja 125 % yltää siihen. Käsin
   kirjoitettu kynnys lakkaa vastaamasta heti kun mitta liikkuu. Lisäksi
   compactin solukatto nostettiin 52 → 64, jotta leveä mutta matala sivu
   käyttää leveytensä railiin eikä pelkkään liuskekiveen, ja resize-koukkuja
   on nyt **kaksi**: piilotettua StackLayout-sivua ei asetella, joten
   suljettuna tehty koon muutos jäisi muuten vanhaan mittaan.

Saman haaran **luettavuuskierros** 2026-07-28 (N8 v2, käyttäjän ensimmäisen
testauskierroksen palaute; **vihreä mutta ei hyväksytty**): `CQ_COMPILE_OK`,
**13 001 unit-passia / 0 failurea / 31/31 luokkaa**, `CQ_SAFETY_PASSED` ja
kaikki neljä GUI-markkeria; `CQ_HOLD_OK` kantaa. Uusi `CQGuiScale` antaa
koko Drawerille **kaksi katselmoitua kokoaskelta**: `compact` (1280×720:n
minimi, sivu 1272×526) ja `roomy` (sivu ≥ 1400×640). Mitattu ensin, sitten
valittu — sivun todellinen suorakaide on aina lava miinus (8, 44). Askel
seuraa ikkunaa Qt:n omasta `onResize`ista, eikä yksikään sivu rakennu
uudelleen: rivit vain mitataan uudelleen, joten koon muutos ei voi
rekisteröidä funnel-sidontaa toiseen kertaan. Tyyppi 10 → 12/15 px, rivi
24 → 30 px, raili 1128 → 1224/1358…1688 px, loomin solu 48 → 52/57…72 px.
Kaikki neljä sivua jakavat nyt saman railin (spellbook ei aiemmin käyttänyt
sitä lainkaan). Väliotsikoille annettiin oma **arvoaste**: otsikko saa koko
saaren mittaisen viivan, väliotsikko nimensä jälkeen alkavan kevyemmän
viivan ja viivamusteen, rivi vain oman arvorailinsa — ei versaaleja, koska
§16.4 pitää kaiken pienaakkosina.

Kierros paljasti kaksi omaa virhettä, jotka testit löysivät ennen käyttäjää:
solun **minimileveys saattoi työntää railin ulos sivultaan** kapealla
ikkunalla (minimi joka rikkoo marginaalin ei ole minimi — lattia väistyy nyt
ennen marginaalia), ja **oma väitteeni sarakkeiden tasaisuudesta oli
ristiriidassa oman suunnittelupäätökseni kanssa** (pariton jako antaa jäännöksen
keskelle juuri siksi, että Cocojen leveydet pysyvät identtisinä).

Haaran `nocturne-drawer-workshop` portti 2026-07-28 (§12.1 / N8,
**ensimmäinen ajettava versio; vihreä mutta ei hyväksytty**):
`CQ_COMPILE_OK`, **12 650 unit-passia / 0 failurea / 30/30 luokkaa**,
`CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED` ja `CQ_GUI_NIGHT_PASSED`; `CQ_HOLD_OK` kantaa,
koska inkrementti ei koskenut DSP:hen. Kaksi uutta testiluokkaa:
`CQGuiCoverageTest` (kattavuusportti + omistajuus) ja `CQGuiWorkshopTest`
(sommittelu, ledger-geometria, arvon muotoilu, funnel-sidonnat — se rakentaa
kaikki 43 riviä palvelimettomasti ja tarkistaa että jokainen kirjoittaa
`CQState`n läpi eikä kaiuta ulkoista kirjoitusta takaisin).

Matkalla kaksi karikkoa, jotka kannattaa muistaa:
**isolla alkukirjaimella alkava paljas sana `#[...]`-literaalissa jäsentyy
luokkanimeksi**, joten `#[In, Fb]` on parse-virhe ja symbolit on kirjoitettava
`#[\In, \Fb]`; ja **`#[...]`-literaali on muuttumaton, mutta `sort` lajittelee
paikan päällä**, joten inventaariota ei saa lajitella kopioimatta — `run_all`in
`completed`-vahti nappasi jälkimmäisen (`_BasicPut failed`, 9/29 luokkaa) juuri
niin kuin sen kuuluukin.

Hyväksyntäportin +20 passia ovat provenienssin pyyhkäisyn törmäysmatriisi
(`test_inkClearances`, 2 kokoa × 2 juurta × 5 säiettä). Se kirjoitettiin ensin
vanhoilla vakioilla ja kaatui täsmälleen käyttäjän näkemiin kohtiin — juuri B,
säikeet 1 ja 2 (0,0 ja 0,55 px) — sekä yhteen 0,8 px:n peilitapaukseen
juuressa A, jota kukaan ei ollut huomannut. **Vakio inset ei voi luvata
väljyyttä siemenetystä kehosta:** väljyys on mitattava.

## Omistajuus- ja julkaisurata (kulkee Nocturnen rinnalla)

Tämä rata tekee valmiista instrumentista rehellisen ja kurantin
portfoliotyönäytteen. Se ei korvaa Nocturnen inkrementtejä eikä todista
teknistä omistajuutta pelkällä dokumentaatiolla: tavoite on, että tekijä osaa
perustella keskeiset ratkaisut, jäljittää niiden toteutuksen ja ylläpitää niitä
myös ilman agentin valmista selitystä. Julkaisuportti sulkeutuu vasta
Night-only-tuomion ja alla olevien julkaisugatejen jälkeen.

### A. Julkaisupohja ja tekijyyden kuvaus

- [x] **Julkinen identiteetti:** Yölahti on oma digitaalinen instrumentti;
  Cocoquantus on avoimesti nimetty vaikutteeksi ja tekniseksi lähtökohdaksi,
  ei tuotteen nimeksi, viralliseksi portiksi eikä uskollisen emulaation
  lupaukseksi. `CQ`/`Cocoquantus` jää tekniseksi nimiavaruudeksi.
- [x] **README:n kaksi yleisöä:** muusikko näkee ensin soittimen luonteen,
  kuuntelu-/käyttötavan ja rajat; työnantaja löytää arkkitehtuurin,
  turvallisuuden, testauksen ja kehitysprosessin ilman liioiteltua
  rivitason tekijyysväitettä.
- [x] **Läpinäkyvä tekijyys:** README ja kehitysprosessikuvaus kertovat
  AI-avusteisesta työnkulusta sekä ihmisen vastuusta konseptissa,
  estetiikassa, äänellisissä päätöksissä, hyväksymiskriteereissä,
  kuuntelussa, manuaalitestauksessa ja julkaistavasta lopputuloksesta.
- [x] **Projektin perustiedostot:** `LICENSE`, `SOURCES.md`, `CHANGELOG.md`,
  `CONTRIBUTING.md`, `docs/CASE_STUDY.md` ja
  `docs/DEVELOPMENT_PROCESS.md` ovat olemassa ja linkitettyinä.
- [x] **Automaatio pohjalle:** Windows/SuperCollider GitHub Actions -workflow
  ajaa compile-, unit-, NRT safety- ja hold integrity -portit. Ensimmäinen
  täysi SuperCollider 3.14.1 -etäajo läpäisi kaikki portit commitilla
  `747aaec` (2026-07-23); release-ehdokkaan tuore ajo kuuluu silti kohtaan C.

### B. Teknisen omistajuuden itseopiskelu

Jokaisesta alla olevasta siivusta tehdään oma yhden sivun muistiinpano tai
kaavio `docs/`-kansioon. Valmis tarkoittaa, että tekijä pystyy selittämään
kulun lähdekoodista ja testeistä omin sanoin, paikantamaan muutoksen
vaikutuspinnat sekä vastaamaan jatkokysymyksiin ilman AI-avustusta.

- [ ] **1. Tila ja ohjauspolku:** jäljitä yksi parametri koko ketjun läpi
  `CQSpec → CQState → server → GUI/MIDI/snapshot`; selitä validointi,
  clamp/enum-käytös, yksi kirjoitussuppilo ja epäkelvon syötteen kohtalo.
- [ ] **2. DSP ja ääniturvallisuus:** käy läpi yksi Coco sekä master-ketju;
  selitä `BufRd`/`BufWr`, kellon vaikutus sävelkorkeuteen, hold, feedback,
  clip/wrap ennen kvantisointia sekä DC-, NaN- ja ulostuloturva. Yhdistä
  jokainen väite sitä todentavaan testiin tai nimeä avoin testiaukko.
- [ ] **3. Night-ele päästä päähän:** jäljitä yksi kosketus eleestä puhtaaseen
  geometriaan, prioriteettiketjuun, tilapäivitykseen, piirtoon,
  cancel/exit-polkuun, Qt-jonotukseen ja telemetriaan.
- [ ] **4. Punch omistajuusharjoituksena:** ennusta ensin kaikki
  vaikutuspinnat, kirjoita tai uudelleenkirjoita itse vähintään yksi kohdistettu
  testi, lue toteutuksen jokainen diff-rivi ja tee tarkoituksellinen
  mutaatiokoe, joka osoittaa exit-polun testin oikeasti rikkoutuvan.
- [ ] **5. Auditointitapauksen rekonstruktio:** valitse yksi aiempi kriittinen
  löydös (esimerkiksi päällekkäinen ulostulo tai lifecycle-ongelma) ja
  rekonstruoi oire → reproduktio → juurisyy → korjaus → regressiotesti.
- [ ] **6. Haastatteluharjoitus:** tallenna ilman AI:ta 90 sekunnin pitch,
  10 minuutin arkkitehtuuriselitys ja 20 minuutin koodikatselmus. Merkitse
  epävarmat kohdat opiskelujonoon ja toista, kun selitys kestää jatkokysymykset.
- [ ] **7. Ylläpitonäyttö:** ratkaise myöhemmässä Nocturne-inkrementissä yksi
  rajattu bugi tai ominaisuus niin, että hypoteesi, suunnitelma, kaikki diffit,
  testit ja hyväksyntä käydään tietoisesti läpi. AI:ta saa käyttää, mutta
  mitään lukematonta muutosta ei hyväksytä.

Erillinen pieni, pääosin käsin kirjoitettu audio-ohjelmointiprojekti voi olla
hyvä lisä koko portfolioon, mutta se ei ole Yölahden julkaisuportti eikä ehto
tämän projektin omistajuudelle.

### C. Public beta -julkaisuportti

- [ ] **Etä-CI vihreäksi:** todenna GitHub Actionsista kaikki neljä markeria
  tuoreella release-ehdokascommitilla; korjaa workflow, jos paikallinen ja
  etäympäristö eroavat.
- [ ] **Nimi ja törmäystarkistus:** tee ennen lopullista nimeämistä tavallinen
  haku GitHubista, hakukoneesta, musiikkiohjelmistoista ja olennaisista
  tavaramerkkitietokannoista. Yölahti on nykyinen työ- ja tuotenimi, ei tässä
  dokumentissa tehty juridinen selvitys.
- [ ] **Historian yksityisyys- ja lähdeauditointi:** tarkista koko Git-historia
  salaisuuksien, henkilötietojen, yksityisten polkujen, sähköpostiosoitteiden,
  lisensoimattomien assettien ja tarpeettomien binäärien varalta. Päätä ennen
  public-vaihtoa, säilytetäänkö nykyinen commit-sähköposti vai puhdistetaanko
  historia hallitusti.
- [ ] **Etärepon uudelleennimeäminen:** nimeä repo esimerkiksi `yolahti`:ksi,
  päivitä paikallinen remote komennolla
  `git remote set-url origin https://github.com/VaahtoluotoTuomas/yolahti.git`
  ja varmista `git remote -v`, fetch ja push. Älä luo myöhemmin uutta repoa
  vanhalla `sc-cocoquantus`-nimellä, jotta GitHubin uudelleenohjaukset eivät
  rikkoudu.
- [ ] **Repo-nimeen sidottujen viittausten päivitys:** aja
  `rg -n "sc-cocoquantus|github.com/VaahtoluotoTuomas"` ja korjaa ainakin
  README-badge, clone-ohjeet, Pages-/demo-osoitteet, submodulet, ulkoiset
  integraatiot ja mahdolliset `OWNER/REPO`-muotoiset Actions-viittaukset.
  GitHub Pagesille käytetään omaa domainia tai sivuston uusi osoite
  päivitetään erikseen.
- [ ] **Public-vaihto:** tee vasta historia-auditoinnin jälkeen. Oleta, että
  koko historia, Actions-ajot ja -lokit tulevat julkisiksi ja kuka tahansa voi
  forkata repon; tarkista samalla Actions-oikeudet ja community profile.
- [ ] **Oikea media:** lisää aito käyttökuva, 60–90 sekunnin käyttövideo ja
  2–3 erilaista ääniesimerkkiä. Normalisoi kuuntelutaso turvalliseksi ja kerro,
  jos ääni on jälkikäsitelty.
- [ ] **Vieraan käyttäjän puhdas asennus:** yksi projektiin perehtymätön
  testaaja asentaa ja käynnistää soittimen README:n avulla ilman suullista
  apua; dokumentoi löydökset ja korjaa estävät epäselvyydet.
- [ ] **Laitteistoraja:** todenna ≥4-kanavainen live-stem-kuuntelu tai pidä
  rajoite näkyvästi dokumentoituna julkaisussa.
- [ ] **Sukulinjan kohteliaisuus:** harkitse lyhyttä ilmoitusta Peter
  Blasserille/Ciat-Lonbardelle. Se on kohteliaisuus, ei lupapyyntö eikä väite
  hyväksynnästä; nimeä, logoja tai ulkoasua ei esitetä virallisena tuotteena.
- [ ] **Beta-julkaisu:** tagi ja GitHub Release, lyhyet release notes,
  kuvallinen social preview, aihetunnisteet, issue-templatet sekä selkeä kanava
  bugiraporteille. README:ssä säilyvät tunnetut rajat ja beta-status.

**Radan exit-kriteeri:** repo on puhtaasti asennettavissa, etä-CI on vihreä,
media näyttää ja kuulostaa oikealta tuotteelta, sukulinja ja AI-avusteinen
tekijyys on kuvattu avoimesti, vieras käyttäjä pääsee alkuun dokumentaatiolla
ja tekijä omistaa vähintään yllä olevat kolme teknistä siivua. Vasta tämän
jälkeen projektia esitellään CV:ssä valmiina portfoliojulkaisuna; sitä ennen
sanamuoto on rehellisesti *private beta / active development*.

## Karikot ja opitut lait (lue ennen koodausta)

`NIGHT_STANDALONE_PLAN.md §6` (ja §14.6) kokoaa tämän vaiheen karikot. Kärjessä:
`var`-esittelyt ensin; `Rect.extent` on **Point** (ei `.width`); **Imagea ei
renderöidä paint-eventin sisällä** ja johtoja ei niputeta yhdeksi jättipoluksi
(molemmat mitattu kohtalokkaiksi, GUI_SPEC §11); sclangin stdout puskuroituu
uudelleenohjattuna (yösmoken watchdog-exit huuhtoo evidenssin); opaakit
endpoint-laatikot peittävät viivoja ja merkkejä (piirrä ne omistavaan
näkymään tai testaa clearance); Qt-näkymänvaihto pysyy jonossa; kustannus
mitataan Qt:n paint-eventistä (`tests/gui_paint_profile.scd`), ei arvata.
§9:stä kolme lisää: **Qt-kerros on suorakaide**, joten uuden kappaleen tilaa
arvioidaan suurimmasta vapaasta *suorakaiteesta*, ei kiekosta (tämä teki
"1.8× pinta-alaa" -väitteestä väärän); **kappaleen siirto varatusta vedestä
lahteen vie sen kaapelikentän alle** ja §4.5 antaa painalluksen kaapelille —
varjostus mitataan monella satunnaisella kentällä ja tuodaan käyttäjälle
kauppana, ei löydetä jälkikäteen; ja **suunnitelmateksti voi olla väärässä** —
jos toteutus sen mukaan tuntuu väärältä, epäile diagnoosia äläkä vain
sommittelua (§7.2 korjattiin vasta hylätyn buildin jälkeen).

## Hyväksytty Night-kokomittakaavan polish-portti

- [x] **Eläköitä Space-ohjauspinta ennen layoutin mittaamista.** Valmis
  2026-07-28: hylätyn efektin näkyvät, fyysiset ja DSP-reitit poistettiin
  yhtenä vaikutuspintana, F14 jätettiin vapaaksi ja Masteriin jäi tulevalle
  tilavalitsimelle varattu ilma.
- [x] **Nightin säätimien ja muun anatomian järjestelmällinen suurennus.**
  N2:n yleinen väljennys ei vielä tehnyt kaikista käsistä ja merkeistä
  riittävän suuria: erityisesti tide-sirpit, niiden asteikkolovet ja Dolby-
  osterit ovat nyt paremmin luettavia mutta edelleen fyysisesti pieniä, ja sama
  koskee monia muitakin Nightin osia. Tämä on koko näkymän mittakaava- ja
  sommittelupassi, ei yksittäisten säätimien satunnainen kasvatus.
  **Hyväksytty kuvallisessa katselmuksessa 2026-07-29.**
- [x] Inventoi ensin koko Night todellisessa käytössä ja ryhmittele kasvatukset
  `CQNightMeasures`-mitoiksi. Aja sen jälkeen puhdas geometria/clearance-portti
  ennen näkymien siirtämistä: crescentit, osterit, endpointit, skip/chevron,
  range-ripplet, kidukset, linnut, juuret ja ringin kädet ovat vähintään
  katselmuslista, eivät ennalta päätetty sama kerroin. **Valmis 2026-07-28:**
  kaikki kasvatukset ovat mittataulussa; puhdas törmäysmatriisi kattaa myös
  koko siemenen halo-/osuma-alueen, juurten käyrät ja siirretyt endpointit.
- [x] Hyväksy pienin ja suurin tuettu ikkuna sekä käyttöjärjestelmän
  näyttöskaalaus. Referenssikokojen automaattiset geometria-/osumaportit ovat
  vihreät, ja käyttäjän visuaalinen 100–200 % katselmus hyväksyttiin
  2026-07-29.

**Katselubuildin sommittelu:** ringin säde 158 → 191 levittää linnakkeet;
Cocot siirtyvät ulommas ja kasvavat Ø 200 → 228; juuret asettuvat 12/88 %:
iin leveyttä ja niiden bias-/drive-solmut kasvavat r 13 → 18. Kaikki Coco- ja
lintusirpit käyttävät nyt Ø 36 piirtokehoa 44 px drag-laatikossa. Linnakkeet
kasvavat 58×50 → 74×66, silmän tarkka hit-alue Ø 84 → 108, kuu Ø 56 → 70,
siementen tarkka hit-alue Ø 18 → 28 ja kiduksen Ø 34 → 48. Nightin jack-/
socket-paperi kasvaa 30/24 → 34/28, näkyvä endpoint-ink 1,2-kertaiseksi ja
symbolit 8 → 12 px. `gui_night_smoke` varmistaa kaikkien 38 endpoint-näkymän
keskukset ja jokaisen istutetun kaapelin molemmat päät siirron jälkeen.
Katselukuvat: `tests/tmp/night-layout.png` (sommittelu) ja
`tests/tmp/night-layout-cables.png` (täysi kaapelikenttä),
`tests/tmp/night-layout-couplings.png` (vihreät kytkökset) ja
`tests/tmp/night-layout-targets.png` (banana kädessä) sekä
`tests/tmp/night-layout-control-edges.png` (täysi nopeus + yli-unity).
Katselmuksen jälkeinen polish poisti silmän opaakin neliömaskin, jätti
Coco-mouth-outputtien paperin läpinäkyväksi ja pitää täyden nopeuden merkin
luettavana normissa 0,99. Lintusirpin unity-ylitys muuttuu punaiseksi. Tyhjä
kohdesocket ei enää lupaa inertillä hoverilla toimintaa; banana kädessä kaikki
sallitut kohteet saavat staattisen halon ja lähin kohde vahvemman vasteen.
Lukupään liike nostettiin 5 → 15 Hz:iin 2026-07-29. Kummankin Cocon comet on
nyt omalla läpinäkyvällä, hiiren läpäisevällä tasollaan: tavallinen phase-ruutu
piirtää vain pisteen ja hännän, muuttumaton ruutu ei mitään, ja raskas shell
piirtyy phase-polusta vain chevronin todellisen suuntaregimen vaihtuessa.
Portit: compile OK; 13 144 assertionia / 0 virhettä / 32 testiluokkaa;
`CQ_SAFETY_PASSED`, `CQ_GUI_PHASE_LAYER_PASSED`, `CQ_GUI_NIGHT_PASSED`,
`CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED` ja
`CQ_GUI_DRAWER_REENTRY_PASSED`.

**Sijainti etenemisessä:** Space-päätös, hallittu eläköitys, yhtenäinen
koko-/layout-passi, 100–200 % fyysisen skaalauksen katselmus ja koko korvapassi
ovat valmiit. AIO-1:n sommittelu on hyväksytty, AIO-2:n draft/profile-
inkrementti, AIO-3:n vahvistettu restart/rollback ja AIO-4:n rajattu
signaalipresence on toteutettu. Seuraava portti on AIO-4:n fyysinen
hardware-I/O-katselmus.

## Valmistunut polish-passissa 2026-07-28

- [x] **Johtojen piilotus/himmennys taustalle.** N3:n Tab/kidus-vesifokus on
  tämän toiveen valmis muoto: se himmentää kaapelijänteet tausta-alfaan ja
  poistaa niiden osumapolut, mutta jättää kehot, endpointit ja istutetut plugit
  eläviksi. Erillistä rinnakkaista kaapelikytkintä ei tarvita.
- [x] **Kiduksen rajattu kaapeliprioriteetti.** Kiduksen tarkka osumamuoto
  voittaa sen yli kulkevan kaapelin hoverissa ja painalluksessa, tyhjentää
  kaapelihovin ja piirtyy päällimmäisellä hiiriläpäisevällä elekerroksella.
  Muualla §4.5:n cable-first-laki säilyy.
- [x] **Snapshotit omaan kansioonsa.** Uudet tallenteet menevät
  `snapshots/`-kansioon. Vanha `presets/*.cqsnap.scd` on vain-lukuinen
  yhteensopivuuspolku, joten aiemmat henkilökohtaiset snapshotit näkyvät ja
  palautuvat ilman siirtoa; saman niminen uusi snapshot voittaa vanhan.
- [x] **Space/reverbin hallittu eläköitys.** Neljän kuuntelukandidaatin
  jälkeen erillinen reverb-/efektimoduuli hylättiin. `spaceMix`, `spaceSize`,
  `spaceDamp` ja molemmat Coco-sendit poistettiin `CQSpec`/`CQState`-
  sopimuksesta; `\cqSpace`, FX-ryhmä, send/return-bussit ja master-return
  poistettiin tuotantograafista. Master ja Workshop eivät piirrä kuolleita
  säätimiä, F14 on inertti ja vanhojen snapshotien kentät ohitetaan.
  Night 31 · Master 4 · Workshop 39 kattaa kaikki 74 elävää parametria.
  MemoryHalo säilyy vain arkistoidussa prototyyppitutkimuksessa.

## Valmis työvaihe AIO — Audio I/O Setup Drawerissa

**Status 2026-07-30: AIO on valmis ja käyttäjän hyväksymä.** AIO-1…AIO-5 ovat
toteutettuja ja gatettuja, ja fyysinen hardware-I/O-katselmus hyväksyttiin
2026-07-30 (AIO.9:n rajaus ja kaksi ei-toistettavaa failurea kirjattu siellä).
**Ainoa avoin AIO-jatko on kaksisuuntainen VB-CABLE A+B -DAW-portti**, joka
vaatii käyttäjän ajuriasennuksen ja tehdään omana päivänään; sen reitityskartta
ja ennakkoehdot ovat AIO.9:n hand-offissa. **Seuraava jonossa on erillinen pitkä
koko soittimen laitteisto-/resource-soak**, jota AIO:n 10 minuutin vakauskoe ei
korvaa.

AIO-sopimus asuu normatiivisesti `SPEC.md` §7.1:ssä, `GUI_SPEC.md`
§8/§9/§13:ssa ja `ARCHITECTURE.md` §5/§6:ssa; alla oleva §AIO.1–§AIO.9 on
vaiheen suunnittelu- ja todistusaineisto.

### AIO.1 Tavoite ja vastuuraja

Tavoite on tehdä jo olemassa oleva konekohtainen audiokytkentä löydettäväksi
ilman SuperCollider-koodin käsin muokkaamista:

`OS/driver → Audio I/O Setup → liveL/liveR/mic → Nightin juuret → Coco →
Master → main/stem-outputit → hardware tai virtuaalinen loopback`.

- **Audio-sivu hallitsee käynnistyskonfiguraatiota**, ei musiikillista
  patchia. Se ei kuulu `CQSpec`iin, snapshotteihin, matrix-undoon, Spellbookiin
  eikä banana-grammariin.
- Nightin juuret säilyvät soittopinnan live-lähdevalintana:
  `live l`, `live r`, `mic`, `deck a`, `deck b`. Audio-sivu päättää vain,
  mistä fyysisistä/virtuaalisista kanavista nuo kolme live-lähdettä tulevat.
- Master kulkee edelleen automaattisesti `outChans`-pariin ja valinnaiset
  Coco-stemit `stemOuts`-pariin. Audio-sivu ei saa luoda yhtään
  `Out.ar`-ohitusta pyhän master-/stem-turvaketjun ohi.
- Käyttöjärjestelmien välistä audiokaapelia ei luvata: selain, DAW tai toinen
  sovellus näkyy Yölahdelle vain, jos OS/driver tarjoaa sen tavallisena
  loopback-/virtuaalilaitteena. GUI voi valita sellaisen laitteen mutta ei luo
  sitä, asenna ajuria, rakenna macOS Aggregate Devicea tai yhdistä Windowsin
  eri host API -laitteita keskenään.
- Tämä ei ole hot-swap. Laite, sample rate, block size ja hardware-I/O-kanavat
  ovat scsynthin boot-asetuksia; niiden käyttöönotto on aina näkyvä,
  käyttäjän vahvistama **audio restart**.

### AIO.2 Drawerin paikka ja navigaatio

Audio I/O on Drawerin **viides normatiivinen sivu**, ei Nightiin lisättävä
paneeli, popup-ikkuna eikä Workshopin 40. parametririvi.

- Välilehtisymboli ja näkyvä nimi ovat `\audio` / `audio`.
- Sitova järjestys on
  `#[inspector, workshop, spellbook, memory, audio]`. Audio on oikeanpuoleisin
  sivu, koska se on tekninen kone-/engine-raja eikä soitannollinen pinta.
- `CQGuiDrawer.tabs` ja `pages` muuttuvat yhdessä. Shell ei saa kovakoodata
  viittä indeksiä muualla, vaan sivu ja tab sidotaan edelleen saman
  järjestetyn inventaarion kautta.
- Sivu rakennetaan kerran `CQGuiDrawer.prBuild`issä ja vain mitataan uudelleen
  `applyScale`ssa. Drawer-tabien vaihto, sulkeminen tai resize ei saa
  rekisteröidä uusia OSCdefejä, tiedostowatchereita tai lifecycle-callbackeja.
- Valittu Audio-sivu säilyy Drawer close/open -kierroksen yli kuten muutkin
  sivut. Onnistuneen Audio-restartin jälkeen uusi GUI avaa Drawerin takaisin
  Audio-sivulle yhden jonotetun Qt-vaihdon kautta, jotta käyttäjä näkee uuden
  aktiivisen tilan. Vanhan sivun Qt-viitteitä ei saa kantaa rebootin yli:
  säilytä vain symbolinen `\audio`-aikomus lifecycle-omistajalla.

### AIO.3 Sommittelu ja esteettinen ohjeistus

Sivu on koneen ja soittimen **rauhallinen rantaportti**, ei yleinen
Preferences-dialogi eikä toinen patch bay. Sen pitää näyttää samalta
Ink & Phosphor -työpenkiltä kuin muut Drawer-sivut.

**Yhteinen kehys**

- Käytä täsmälleen `CQGuiScale.pageFrame`ä, samaa yläilmaa ja samaa keskitettyä
  `CQGuiScale.railWidth`-sisältökiskoa kuin Inspector/Workshop/Spellbook/
  Memory. Compact 1280×720 -ikkunan Drawer-sivu (noin 1272×526) on
  ensimmäinen sitova sommittelu; roomy-askel saa lisätä ilmaa ja mittareiden
  pituutta, ei uusia kontrolleja.
- Ei scrollausta, resizable-paneelia, irrallisia ikkunoita, rounded card
  -ruudukkoa, varjoja, gradientteja, native Qt -sinistä tai käyttöjärjestelmän
  checkbox-/combo-tyyliä. Käytä recessed slate -pohjaa, suoria hairline-
  sääntöjä, 8 px gridin guttereita, `CQInkControl`-kontrolleja ja
  `CQInkMenu`n Ink-piirrettyjä avautuvia rivejä.
- Kaikki control-/fontti-/tekstibandimitat tulevat `CQGuiScale.metrics`istä.
  Älä keksi Audio-sivulle kiinteää rinnakkaista mittataulua. Avautuvan
  laitelistauksen jokainen rivi tarvitsee saman tavallisen hover-ink-kielen
  kuin Inspectorin preset- ja Memoryn valintalistat.

**Kolme vaakavyöhykettä samalla sisältökiskolla**

1. **Host / engine, täysi leveys.** Yksi ohut aktiivisen serverin tilarivi
   (`running · 48 kHz · block 64 · hw 256`) ja sen alla device mode
   (`system default`, `single device`, `split devices`), input-device ja
   output-device. Single-tilassa yksi pitkä laiterivi; split-tilassa kaksi
   tasalevyistä riviä. Laitteen täydellinen merkkijono on totuus, mutta
   ylileveä nimi katkaistaan vain hännästä ellipsillä; se ei saa venyttää
   railia tai kadottaa nimen alkua/host APIa.
2. **In / out, kaksi yhtä leveää saarta.** Vasemmalla `into yölahti`:
   `live l`, `live r`, `mic off/on + channel` sekä kolme ohutta
   signal-presence-uraa. Oikealla `out of yölahti`: `main l`, `main r`,
   `stems off/on`, `stem 1`, `stem 2`, `record 4ch off/on` sekä nykyinen
   master-outputin stereoaktiivisuus. Saaret erottaa gutter ja yksi suora
   hairline; niiden ei pidä näyttää neljältä tai kahdeksalta asetuskortilta.
3. **Restart gate, täysi leveys.** Vasemmalla pysyvä validointi-/provenanssiteksti,
   keskellä `discard changes`, oikealla ensisijainen `apply & restart audio`.
   Ensimmäinen apply-painallus ei vielä reboottaa: se avaa saman railin sisään
   staattisen vahvistustilan `audio stops · current unsaved patch resets ·
   Server.default restarts`, jossa ovat `cancel` ja `restart`. Ei native-
   modaalia, countdownia, spinneriä eikä vilkkumista.

**Väri ja tilat**

- Pysyvä aktiivinen arvo = bone/line-ink. Muuttunut draft-arvo = warning ochre
  -alleviivaus tai vasen pieni delta-lovi; **älä väritä koko riviä**.
  Rakenteellisesti virheellinen draft = danger-red sääntö + täsmällinen yksi
  virketason syy. Onnistuminen = vihreä/input-värinen lyhyt `audio ready`,
  mutta tavallinen aktiivinen sivu palaa neutraaliksi.
- Inputin signaalijälki käyttää nykyistä `input`-vihreää, master-output line/
  bonea ja stemit olemassa olevaa Coco/cas-perhettä. Väri kertoo signaalin
  läsnäolon tai virheen, ei koristeellista vasen/oikea-identiteettiä.
- `mic off` pitää mic-rivin ja kanavapaikan näkyvänä mutta himmeänä, jotta
  layout ei hypi. `mic on` **ei koskaan nosta `micGain`ia nollasta**. Jos
  micGain on myöhemmin käyttäjän toimesta live, Audio-sivu toistaa nykyisen
  danger-red `MIC LIVE` -tilan vain read-only-varoituksena; Workshop säilyy
  ainoana interaktiivisena `micGain`-omistajana.
- `stems off` himmentää mutta ei poista stem-kanavarivejä. Pending, restarting,
  rollback ja failed ovat staattisia tekstimuotoja; liike kuuluu vain
  oikealle ≤15 Hz signaalitelemetrialle.
- Audio-sivulla ei ole jack-/socket-palloja, johtoja, kaapelihaloja tai
  vedettäviä reittejä. Se olisi väärä lupaus: OS-laite ei ole matrix-endpoint.

### AIO.4 Muokattavat kentät ja täsmällinen semantiikka

| UI-kenttä | config-totuus | laki |
|---|---|---|
| device mode | `device` | default = `nil`; single = yksi exact String; split = `[inDevice, outDevice]`, jossa kumpikin voi olla nil |
| input/output device | `device` | runtime näyttää vain exact konfiguroidut endpointit tai system defaultin; directional discovery palautetaan vasta stopped-server-portin takana, ei substring-arvausta |
| sample rate | `sampleRate` | vain CQConfigin 44.1/48/96 kHz |
| server block | `blockSize` | vain CQConfigin 16/32/64/128/256/512 |
| hardware buffer | `hardwareBufferSize` | `driver default` = nil tai validoitu 16…8192 |
| live l / live r | `inChans[0..1]` | kaksi näkyvää kanavaa; sama indeksi molemmissa on sallittu tarkoituksellista mono-duplikaatiota varten |
| mic | `micChan` | off = nil; on = yksi indeksi ≥0; saa olla sama kuin live-kanava |
| main l / main r | `outChans` | täsmälleen kaksi eri indeksiä |
| stems | `stemOuts` | off = nil; on = täsmälleen kaksi eri indeksiä, eivät saa osua main-pariin |
| record 4ch | `recStems` | boolean; ei automaattisesti kytke hardware-stemejä päälle |

- Käyttäjälle kanavat näytetään **1-pohjaisina** (`ch 1`), configiin ne
  tallennetaan **0-pohjaisina** (`0`). Muunnos on yhdessä puhtaassa helperissä
  ja roundtrip-testissä; mitään call-site-kohtaista `+1/-1`:tä ei sallita.
- `inChans`-configin vanha 1…8 pituuden hyväksyntä säilyy taaksepäin
  yhteensopivana, mutta Audio-sivu omistaa vain ensimmäisen stereo-/mono-parin
  ja kirjoittaa aina kaksi arvoa. Se ei saa väittää kanavia 2…7 käytetyiksi,
  koska nykyinen `\cqInput` lukee vain kaksi ensimmäistä.
- `bufFrames`, kansiot, 16n, `autoTrim`, compass-polku ja muut ei-I/O-
  asetukset eivät kuulu tälle sivulle eivätkä audio-overlayhin.
- Device-lista ei kerro luotettavasti kanavakapasiteettia. GUI saa todistaa
  rakenteellisen kelvollisuuden mutta ei saa näyttää vihreää “hardware
  supports channel 8” -väitettä ennen onnistunutta boottia.
- Windowsin split-device-tilassa näytä pysyvä whisper-ohje `same host API
  required`; älä yritä päätellä tai korjata host APIa laitenimen pilkkomisella.

### AIO.5 Aktiivinen tila, draft ja persistenssi

Tämän eron pitää näkyä sekä mallissa että ruudulla:

- **active** = `Cocoquantus.config`, jolla nykyinen onnistuneesti käynnistynyt
  sessio todella soi;
- **draft** = Audio-sivulla muokattu, vielä käyttämätön overlay;
- **dirty** = draftin omistamat kentät eroavat activesta;
- **valid** = `CQConfig` hyväksyy base+draft-yhdistelmän rakenteellisesti;
- **last working** = viimeisin config, jonka koko server+graph-boot päätti
  `\running`-tilaan, ei viimeisin levylle kirjoitettu teksti.

Käsin ylläpidettyä `config.scd`:tä **ei koskaan kirjoiteta, parsita tekstinä
eikä formatoida uudelleen**. GUI omistaa erillisen konekohtaisen,
gitignorettavan overlayn:

- oletus `config.scd` → saman kansion `config.audio.scd`;
- custom `foo.scd` → saman kansion `foo.audio.scd`;
- merge-järjestys `CQConfig.defaults < base config < audio overlay`, jonka
  jälkeen validointi tehdään **kerran lopulliselle Eventille**;
- overlay saa sisältää vain AIO.4-taulukon config-avaimet. Tuntematon avain on
  generated-overlayn hard error, ei pelkkä warning.

SuperColliderin `Event` poistaa avaimen, jos sen arvoksi asetetaan `nil`.
Siksi generoitu overlay käyttää neljän nilattavan avaimen (`device`,
`hardwareBufferSize`, `micChan`, `stemOuts`) arvona sisäistä `\default`-
merkkiä; `CQAudioConfig` purkaa sen takaisin todelliseksi puuttuvaksi/nil-
config-arvoksi ennen kuin candidate saavuttaa `CQGraph`in. Tämä on tiedoston
esitysmuoto, ei uusi käyttäjälle näkyvä config-arvo.

Tee puhdas ei-Qt-luokka (työnimi `CQAudioConfig`) omistamaan allowed keys,
base+overlay-merge, active/draft-vertailu, 0/1-kanavamuunnos, validointi,
profile-path ja serialisointi. Device-merkkijono tulee valintalistasta tai
nykyisestä configista ja kirjoitetaan `asCompileString`-escaped Stringinä;
UI-tekstiä ei koskaan evaluoida SuperCollider-koodina.

Persistenssi on atominen:

1. serialisoi vain validoitu overlay saman kansion `.pending`-tiedostoon;
2. lataa pending takaisin ja validoi base+pending roundtrip;
3. älä vielä korvaa viimeistä toimivaa overlayta;
4. käynnistä candidate Eventillä;
5. vasta onnistuneen koko graph-bootin jälkeen vaihda pending atomisesti
   varsinaiseksi `.audio.scd`:ksi ja säilytä yksi `.bak`;
6. jos final rename epäonnistuu, audio saa jäädä toimimaan candidatella mutta
   sivu näyttää `running · not saved` ja tarjoaa tallennuksen uudelleen.

Puuttuva overlay säilyttää nykyisen bootin bit-identtisenä. Base-configin
muuttaminen myöhemmin voi jäädä overlayn alle, joten Audio-sivu näyttää
whisper-provenanssin `audio profile overrides config.scd`, kun overlay on
olemassa. `discard changes` palauttaa draftin activeen; se ei poista tiedostoa
eikä reboottaa.

### AIO.6 Apply/restart/rollback-lifecycle

GUI ei omista serveriä. Lisää yksi facade-operaatio (työnimi
`Cocoquantus.reconfigureAudio`) ja pidä `CQGraph.applyOptions` ainoana
paikkana, joka kirjoittaa `ServerOptions`-arvot.

Sitova transaktio:

1. Apply on inertti, jos draft ei ole dirty+valid tai lifecycle ei ole
   `\running`.
2. Jos recorder on `starting/recording/stopping`, restart on estetty:
   `stop REC before restarting audio`. Audio-sivu kuuntelee recorderin
   `\state`-muutoksia suoraan: `idle` poistaa lukon ilman uutta kenttämuutosta,
   eikä disabled Apply piirrä hover-cuesta. Älä katkaise WAV-tiedostoa salaa.
3. Ensimmäinen painallus näyttää inline-vahvistuksen. Kerro aina, että
   `Server.default.reboot` pysäyttää myös muut samaa serveriä käyttävät synthit.
4. Vahvistus captureoi vain arvot/dataa — ei vanhan Qt-sivun viitteitä —
   kirjoittaa/validoi pendingin ja käynnistää olemassa olevan sukupolvi-
   suojatun free→server reboot→build-polun candidatella.
5. Ensimmäisen AIO-version hyväksytty rajaus: nykyinen musiikillinen patch,
   deck-positiot ja recorder eivät palaudu automaattisesti. Vahvistusrivillä
   lukee siksi `current unsaved patch resets`; käyttäjä voi tallentaa
   snapshotin Memoryssä ennen restartia. Älä toteuta puolittaista “best
   effort” -palautusta, joka palauttaa matrixin mutta unohtaa muun session.
6. Candidate merkitään active/last workingiksi ja overlay commitataan vasta,
   kun server, SynthDefit, graph spawn, state/matrix push ja fade-in ovat
   päässeet nykyisen generationin `\running`-tilaan.
7. Jos candidate-boot epäonnistuu, pending hylätään ja viimeinen toimiva Event
   rebootataan automaattisesti **täsmälleen kerran**. Ei retry-loopia.
   Onnistunut rollback avaa Audio-sivun danger-viestillä, säilyttää hylätyn
   draftin korjattavaksi eikä muuta last-working-tiedostoa.
8. Jos rollbackkin epäonnistuu, lifecycle jää `\failed`, output on hiljaa ja
   post window saa tarkan candidate-errorin, rollback-errorin, aktiivisen
   base/profile-polun sekä yhden palautuskomennon. Älä väitä GUI:ssa
   onnistumista äläkä yritä kolmatta boottia.

`Cocoquantus.reconfigureAudio` tarvitsee `onReady/onCandidateFailure/
onRollbackReady/onRollbackFailure`-tulokset tai yhden dokumentoidun
result-Eventin. Kaikki async-haarat tarkistavat `sessionGeneration`in.
Vanhan session myöhäinen callback ei saa tallentaa overlayta, avata uutta
Drawer-sivua tai muuttaa uuden session statusriviä.

### AIO.7 Laitelista ja signaalin vahvistus

- Running Audio -sivu ei kutsu `_ListAudioDevices`-primitiiviä lainkaan.
  Fyysinen Windows WASAPI -portti osoitti konstruktorin
  `ServerOptions.devices/inDevices/outDevices`-kyselyn jumittavan jo endpointit
  omistavan `scsynth`in. AIO-5 kerää `inDevices/outDevices`-snapshotin vain
  varmasti pysähtyneenä ennen ensimmäistä bootia tai `Server.reboot`in
  stopped-callbackissa. Facade säilyttää snapshotin vain muistissa ja uusi GUI
  lukee sen kerran; tab/menu/resize/filter eivät koskaan skannaa OS:ää.
- Windowsin recommended-näkymä säilyttää WASAPI-, ASIO- ja tuntemattomat
  ajurit sekä jokaisen exact active/draft-endpointin. Vain täydet `MME : `,
  `Windows DirectSound : ` ja `Windows WDM-KS : ` -prefixit piilotetaan
  oletuksena paikallisen `show all drivers` / `recommended drivers` -kytkimen
  taakse. Kytkin suodattaa cachea, ei laitteistoa. Muilla alustoilla tai
  tuntemattomilla prefixeillä mitään ei piiloteta.
- Jos turvallista snapshotia ei ole, runtime-katalogi sisältää exact
  konfiguroidut nimet ja `system default` -valinnan ja näyttää disabled
  `configured devices`. Scan failure ei estä audiobootia; viimeinen onnistunut
  cache saa jäädä stale-varoituksella.
- Jos aktiivinen tai draft-laitenimi ei enää ole listassa, säilytä ja näytä
  exact String danger-/warning-tilassa `device not currently reported`;
  älä vaihda sitä automaattisesti ensimmäiseen laitteeseen.
- Ensimmäiseen versioon ei tule test tonea. Väärään hardware-outputiin
  automaattisesti lähetetty ääni on turvallisuusriski eikä todista fyysistä
  kytkentää.
- Input-vahvistus on kolme read-only presence-uraa aktiivisten hardware-
  kanavien **raakasignaalille ennen `liveGain`/`micGain`-kertoimia ja ennen
  preampin source-valintaa**. Presence vastaa “tuleeko porttiin mitään”, ei
  “kuuluuko tämä nyt”; rinnalla oleva muted/live-tila kertoo gainin. Jaa
  `\cqInput`issa nykyinen SoundIn ensin `rawLiveL/rawLiveR/rawMic`-muuttujiin
  ja pidä varsinainen audiolaskenta niiden jälkeen muuttumattomana. Yksi
  kolmekanavainen `/cq_io_inputs`-`SendPeakRMS`-blokki kulkee 15 Hz:llä;
  canonical order on `[rawLiveL, rawLiveR, rawMic]`, puuttuva mic = 0. Viesti
  on telemetriaa eikä kirjoita audiobussiin. Tap palautettiin, kun sama fyysinen
  jumi toistui ilman sitä ja poistui vasta runtime-`_ListAudioDevices`-kutsun
  poistolla; eristetty raw-tapin uusintatesti on silti hyväksymisportti.
- Output-ura käyttää jo olemassa olevan post-safety `/cq_meters`-masterin
  stereoarvoja. Älä rekisteröi toista samannimistä OSCdefiä tai varasta
  nykyistä yksipaikkaista `meterFuncs[\master]`-callbackia. Laajenna CQGui
  tarvittaessa cache+listener-listaksi, jonka omistajat vapauttavat itsensä,
  tai anna Audio-sivun lukea yhtä keskitettyä cachea.
- Telemetria cachetaan aina, mutta Audio-sivun view refreshataan vain, kun
  Drawer on auki Audio-tabilla ja näkyvä arvo ylittää piirrettävän delta-
  kynnyksen. Piilotettu viides sivu ei saa maksaa 15 Qt-painttia sekunnissa.
- Presence-ura todistaa vain, että aktiiviseen server-kanavaan tulee
  signaalia. Se ei todista draft-laitetta tai fyysisen kaapelin kohdetta ennen
  restartia/hardware-kuuntelua.

### AIO.8 Toteutusrajat heikommalle mallille — älä oikaise näistä

- Älä lisää Audio-kenttiä `CQSpec`iin tai `CQState`en. Boot-config ei ole
  automatisoitava musiikkiparametri.
- Älä kirjoita `config.scd`:tä, älä säilytä device-nimiä snapshotissa, presetissä
  tai repossa, äläkä commitoi käyttäjän `.audio.scd`/`.bak`/`.pending`-tiedostoja.
- Älä kutsu `Server.default.options`, `Server.quit`, `Server.reboot`,
  `Synth`, `Bus` tai `Out` suoraan GUI-luokasta. Page → controller/model →
  Cocoquantus facade → CQGraph on ainoa suunta.
- Älä sovella yhtäkään draft-kenttää live-sessioon ennen vahvistettua
  restartia. Dropdown-valinta ei saa mykistää ääntä.
- Älä arvioi laitekanavien määrää device-stringistä. Älä moduloa tai clampaa
  virheellistä output-indeksiä “lähimpään toimivaan”: validointi pysäyttää.
- Älä muuta input/output-kanavia 1-pohjaisiksi configissa. 1-pohjaisuus on
  vain esitys.
- Älä anna main- ja stem-outputtien osua. Älä löysää `CQConfig`in uniqueness/
  disjoint-sääntöjä GUI:n helpottamiseksi.
- Älä avaa micGainia micChan-valinnan sivuvaikutuksena. Älä palauta
  `monitorMute`a snapshotista tai ohita kahden sekunnin boot fadea.
- Älä ota käyttöön uutta output-meteriä safety-chainin edestä; käyttäjälle
  näytetään se ääni, joka oikeasti lähtee masterista.
- Älä tee automaattista device-refresh-timeria, vilkkuvaa restart-spinneriä
  tai animaatiota väittämään, että pysähtynyt server on elossa.
- Älä pidä vanhan Drawer-/view-instanssin callbackia rebootin yli. Tämä on
  sama stale-generation-luokan bugi, jonka `CQSessionLifecycleTest` jo
  rekonstruoi.
- Älä oleta, että server on vain Yölahden käytössä. Inline-vahvistus nimeää
  `Server.default`-restartin vaikutuksen muihin syntheihin.
- Älä sekoita Audio-sivua nelikanavaisen hardware-gaten hyväksyntään:
  serverless testit todistavat rakenteen, mutta oikea main/stem-kuuntelu
  oikeilla fyysisillä porteilla on yhä erillinen manuaalinen portti.

### AIO.9 Inkrementit, testit ja hyväksyntä

Tee työ neljänä pienenä porttina; älä rakenna file I/O:ta, lifecyclea,
telemetriaa ja koko sivua yhteen vaikeasti diagnosoitavaan diffiin.

**AIO-1 — puhdas malli ja read-only-sivu**

- [x] `CQAudioConfig`: allowed keys, merge precedence, active/draft/dirty/valid,
  channel display roundtrip ja profile-path.
- [x] `CQGuiAudioSetup`: viides sivu, yhteinen rail/pageFrame, compact+roomy,
  aktiivisen configin näyttö, device-listan explicit refresh; ei vielä savea
  tai reboottia.
- [x] Unit: nil/single/split device; Unicode-, lainausmerkki- ja backslash-
  device-stringit; custom config path; 0↔1 channel roundtrip; overlap/
  duplicate/invalid range; base Event ei muutu.
- [x] Drawer/Audio smoke: viisi tabia/sivua samassa järjestyksessä ja railissa,
  Audio-sivu mahtuu 1280×720:een ja roomyyn, Ink-menujen avatut rivit hoveravat,
  mic/stem off ei muuta layoutia, close/open säilyttää sivun.
- [x] **Visuaalinen käyttäjästoppi:** sommittelu hyväksyttiin 2026-07-29.
  Katselmuksen jälkeen refresh kohdistettiin oikeaan railiin ja laitenimien
  popup-rivit tehtiin yhden rivin ellipsoiduiksi. Katselukuvat:
  `tests/tmp/audio-setup-compact.png` ja
  `tests/tmp/audio-setup-roomy.png`. Live-polku: Ctrl+Shift+L → `CQ.boot` →
  Drawer → `audio`.

Portit AIO-1-baselinella: `CQ_COMPILE_OK`;
**13 386 unit-passia / 0 failurea / 34/34 luokkaa**;
`CQ_GUI_AUDIO_PASSED`, `CQ_GUI_DRAWER_PASSED`, `CQ_GUI_SHELL_PASSED` ja
`CQ_GUI_DRAWER_REENTRY_PASSED`; muuttumaton audiograafi vahvistettiin lisäksi
markkereilla `CQ_SAFETY_PASSED` ja `CQ_HOLD_OK`.

**AIO-2 — draft ja atominen overlay**

- [x] Kaikki AIO.4-kentät, invalid/dirty/provenance-tilat, discard, pending-
  roundtrip ja gitignore.
- [x] Unit käyttää aina `tests/tmp`-polkuja: base säilyy byte-for-byte,
  epäonnistunut serialisointi/validointi ei koske activea, pending ei korvaa
  last workingiä, rename-failure näkyy `running · not saved` -tilana.
- [x] GUI smoke injektoi directional laitelistan; profile-store-testit
  injektoivat file-adapterin. GUI ei lue oikeita OS-laitteita eikä kumpikaan
  kirjoita käyttäjän configia.
- [x] Exact-duplikaatit poistetaan säilyttäen järjestys; single-lista on
  in/out-intersection, split-listat suuntakohtaisia. Popup on enintään 16
  vieritettävää yhden rivin nimeä.
- [x] Boot lataa olemassa olevan generated sibling-overlayn base-configin
  päälle; `config.scd`:tä ei kirjoiteta.

Portit AIO-2:ssa ja Windows-rivinvaihtokorjauksessa: `CQ_COMPILE_OK`;
**13 408 unit-passia / 0 failurea /
35/35 luokkaa**; `CQ_GUI_AUDIO_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_SHELL_PASSED` ja `CQ_GUI_DRAWER_REENTRY_PASSED`.

**AIO-3 — vahvistettu restart ja rollback**

- [x] Facaden reconfigure-operaatio, generation-suoja, recorder-portti,
  candidate success, candidate fail→yksi rollback, rollback fail ja
  symbolinen Audio-tab-reopen.
- [x] Laajenna `CQTestServer`/lifecycle-doublea; testaa reboot-count, callback-
  järjestys, ettei stale callback committaa tiedostoa, eikä retryjä tule yli
  kahta (candidate + rollback).
- [x] GUI smoke todistaa: edit ei reboottaa; invalid ei armaudu; ensimmäinen Apply
  vain vahvistaa; cancel palaa; recorder estää; success/fail/rollback-state
  käyttää oikeaa staattista ink-kieltä.
- [x] Aja compile → run_all → `nrt_safety` → `nrt_hold_integrity` sekä kaikki
  Drawer/Night/shell-smoket, koska boot-lifecycle ja config ovat muuttuneet.
- [x] Fyysinen split-WASAPI candidate-roundtrip 2026-07-29: server block
  64→128→64 tallentui exact generated overlayhin, GUI palasi kummankin
  restartin jälkeen, ääni ja Night-liike jatkuivat ja lopullinen `scsynth`
  vastasi kolmeen ulkoiseen `/status`-kyselyyn.
- [x] Fyysiset draft-rajat 2026-07-29: Discard palautti 128→64 kirjoittamatta
  tai restarttaamatta; confirmation Cancel säilytti 128-draftin mutta perui
  restartin, minkä jälkeen Discard palautti 64:n. PID ja overlay pysyivät
  muuttumattomina.
- [x] Fyysinen REC-lock-uusinta 2026-07-30: REC start lukitsi Applyn ilman
  hover-cuesta, REC stop poisti varoituksen ja palautti validin draft-gaten
  ilman kenttämuutosta, Discard palautti block 64:n ja sama serveri vastasi
  kolmeen ulkoiseen `/status`-kyselyyn.

Portit AIO-3:ssa: `CQ_COMPILE_OK`; **13 449 unit-passia / 0 failurea /
36/36 luokkaa**; `CQ_SAFETY_PASSED`, `CQ_HOLD_OK`,
`CQ_GUI_AUDIO_PASSED`, `CQ_GUI_DRAWER_PASSED`, `CQ_GUI_NIGHT_PASSED`,
`CQ_GUI_PHASE_LAYER_PASSED`, `CQ_GUI_SHELL_PASSED` ja
`CQ_GUI_DRAWER_REENTRY_PASSED`.

**AIO-4 — signaalipresence ja rajattu hardware-I/O-katselmus**

- [x] Raw-input-telemetria palautettiin vihreiden koneporttien taakse ja
  eristetty fyysinen split-WASAPI-uusintatesti hyväksyttiin 2026-07-29:
  molemmat live-presence-rivit reagoivat, ääni ja Night-liike jatkuivat, sama
  `scsynth`-PID vastasi kolmeen 72 tavun `/status.reply`-viestiin, CPU eteni
  jokaisella kierroksella ja working set pysyi 37,4 Mt:ssa.
- [x] AIO-5 stopped-server directional discovery, memory-cache,
  recommended/all-toggle ja failure/fallback-portit; running-sivulla on
  edelleen nolla `_ListAudioDevices`-kutsua. Fyysinen split-WASAPI-portti
  hyväksyttiin 2026-07-29: recommended-listat olivat tiiviit, show-all palautti
  legacy-ajurit, exact aktiiviset endpointit säilyivät ja ääni, Night-liike
  sekä presence jatkuivat Audio-sivu auki. Clean `restart audio` käytti saman
  stopped-server-rajan; GUI palasi ja ääni sekä Night-liike palautuivat.
- [x] Master-cache/listener, visible-page refresh gate ja
  paint-count-/cleanup-testit.
- [x] Qt-paint-batch-portti todistaa, ettei piilotettu Audio-sivu piirrä,
  yhdistetty 40 Hz input/master-testivirta ylitä avoimella sivulla 15 Hz:ä
  eikä listener/OSCdef jää eloon freen jälkeen.
- [x] Compile, unit, Audio/presence/Drawer/shell-smoket sekä muuttumattomat
  safety- ja hold-portit ovat vihreitä: **13 502 passia / 0 failurea /
  37/37 luokkaa**, `CQ_GUI_AUDIO_PASSED`,
  `CQ_GUI_AUDIO_PRESENCE_PASSED`, `CQ_GUI_DRAWER_PASSED`,
  `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_REENTRY_PASSED`,
  `CQ_SAFETY_PASSED` ja `CQ_HOLD_OK`.
- [x] Responsiivinen mutta audiokelloltaan epäterve candidate ei enää saavuta
  graafin allokointia, fade-iniä tai profile-promootiota. Facade vaatii kolme
  0,75 s välein luettua toteutuvaa sample ratea 80–120 %:iin pyydetystä.
  Mitattu 2610,8 Hz / 48 kHz sekä vanha terve ensimmäinen arvo ja sitä seuraava
  romahdus `[48000, 7076.4, 7019.8]` palauttavat hermettisessä
  lifecycle-portissa yhden failure-callbackin ja käyttävät transaktion olemassa
  olevaa yhtä last-working-rollbackia. Portit:
  `CQ_COMPILE_OK`; **13 502 / 0 / 37/37**; `CQ_GUI_AUDIO_PASSED`,
  `CQ_GUI_AUDIO_PRESENCE_PASSED`, `CQ_GUI_DRAWER_PASSED`,
  `CQ_GUI_SHELL_PASSED`, `CQ_SAFETY_PASSED`, `CQ_HOLD_OK`.
- [x] Fyysinen hardware-I/O-katselmus. **Käyttäjä hyväksyi katselmuksen
  2026-07-30 tässä rajauksessa:** kohdat 2, 3, 5, 6, 7, 8 ja 10 ovat fyysisesti
  hyväksytyt, kohta 9 hyväksyttiin näkyvyysväitteensä laajuudessa, ja kohdat 1
  ja 4 ovat kirjatut fyysiset failuret, joita **ei toisteta tällä koneella** —
  molempien juurisyy (responsiivinen mutta audiokelloltaan kuollut candidate)
  on korjattu ja katettu hermettisellä regressiolla. Vaadittu vähimmäissisältö:
  1. system default boot + aktiivinen live L/R -signaali — tämän kannettavan
     fyysinen portti epäonnistui 2026-07-30 alla kuvatulla tavalla;
  2. yksi USB-interface single-device-tilassa — K-Mix ASIO, inputit 1/2,
     main 1/2 ja vakaa 48 kHz hyväksytty 2026-07-30;
  3. Windows split in/out samalla host APIlla — boot, raw presence ja
     palvelinvakaus hyväksytty 2026-07-29;
  4. tarkoituksella puuttuva laite → yksi turvallinen rollback — fyysinen koe
     epäonnistui 2026-07-30: irrotettu ASIO-laite käynnistyi näennäisesti,
     ensimmäinen kellolukema jäi vanhaksi ja profiili promotoitiin ennen
     romahdusta; kolmipisteinen guard kattaa regression hermettisesti, eikä
     riskikoetta toisteta tällä koneella;
  5. mic off→configured pysyy mykkänä, kunnes Workshopin micGain nostetaan —
     ch 1 raw-presence, muted/live-tila, kuultava dry-monitor, gainin palautus
     nollaan ja explicit mic-off-overlay hyväksytty 2026-07-30;
  6. main L/R fyysisesti oikeissa porteissa ja PANIC mykistää ne — split-
     WASAPI-output sekä K-Mix 1/2, master-presencen hiljeneminen,
     raw-presencen riippumattomuus ja resume hyväksytty;
  7. vähintään nelikanavaisella interfacella stem 1/2 oikeissa, mainista
     erillisissä porteissa ja saman safety-katon alla — K-Mixin fyysiset
     ulostulot 3/4, itsenäinen Coco1/Coco2-kartoitus ja PANIC hyväksytty
     2026-07-30; yhteinen safety-ketju on katettu koodilla ja NRT-portilla;
  8. REC stereo ja `record 4ch` tuottavat oikean kanavamäärän — 2 ch ja 4 ch
     RIFF/WAVE-headerit, 48 kHz / 24-bit, exact `recStems`-persistenssi sekä
     paluu off-tilaan hyväksytty 2026-07-30;
  9. virtuaalinen loopback näkyy tavallisena driver-laitteena, mutta GUI ei
     lupaa luoneensa järjestelmäkytkentää — hyväksytty 2026-07-30 tässä
     rajauksessa: koneen valmiiksi aktiivinen Realtek `Stereo Mix` on exact
     input-valinta `show all drivers` -kytkimen takana, koska sen host API on
     legacy `Windows WDM-KS`. **Kaksisuuntainen DAW-reitti on erillinen avoin
     portti** (VB-CABLE A+B, ks. hand-off alla);
  10. 10–15 minuutin tiheä patch + molemmat Cocot + Audio-sivu auki/kiinni:
      rajattu AIO-vakauskoe hyväksytty 2026-07-30 alla kuvatulla tavalla.

**Jatkopiste 2026-07-30 — hardware-I/O-katselmus**

- Uudelleenkäynnistyksen jälkeen jo promotoitu `system default` avasi GUI:n ja
  tuotti aktiiviset live L/R -presence-rivit, mutta Night eteni noin 1 FPS,
  SuperCollider hidastui ja ulostulo oli pelkkää glitchaavaa korkeahkon
  keskialueen häiriöääntä. Sama oire oli alkanut heti ensimmäisellä
  system-default-vaihdolla ennen Windowsin uudelleenkäynnistystä.
- Serveri ei ollut muodollisesti kuollut: se vastasi kolmeen 72 tavun
  `/status.reply`-viestiin. Vastaukset paljastivat varsinaisen virheen:
  nimellinen sample rate oli 48 000 Hz, mutta toteutuva vain
  2612,8 → 2611,4 → 2610,8 Hz. Tyhjennettykin scsynth käytti noin 11,5 %
  yhdestä CPU-ytimestä. Nykyinen ready-raja hyväksyi ja promotoi siis
  responsiivisen mutta audiokelloltaan käyttökelvottoman candidate-session.
- `CQ.free` hiljensi graafin, generated overlay palautettiin viimeiseen
  split-WASAPI-arvoon ja `CQ.reboot` käynnisti uuden scsynth-PID:n. Käyttäjän
  mukaan ääni ja Night-liike palautuivat normaaleiksi. Kolme ulkoista vastausta
  raportoi 47 986–48 003 Hz, 7,8–9,5 % server-CPU:n ja 37,7 Mt working setin.
- Palautettu ja persisted baseline on split WASAPI:
  input `Microphone Array (AMD Audio Device)`, output
  `Headphones (Realtek(R) Audio)`, 48 kHz, server block 64, hardware buffer
  256, live in ch 1/2, main out ch 1/2, mic off, stems off ja record 4ch off.
  `config.audio.scd` on Git-ignored ja sisältää jälleen tämän exact tilan.
- Candidate-readinessin toteutuvan sample raten terveystarkistus ja hermettinen
  regressio ovat nyt valmiit ja vihreät.
- Split-baselinen 10 minuutin tiheä `\mayhem`-patch hyväksyttiin:
  molemmat Cocot, myöhemmin ladatut kaksi deck-samplea sekä toistuvat
  Audio↔Night-vaihdot pysyivät toimivina. Kaikki 21 ulkoista status-kyselyä
  vastasivat samalla PID:llä; audiokello pysyi 47 986–48 021 Hz:ssä,
  serverin avg CPU enintään 12,55 % ja peak enintään 14,04 %. Node-puu kasvoi
  odotetusti 6→8 vain kahdella `cqDeck`-synthillä. Sclang/scsynth-muisti ja
  handlemäärät tasaantuivat kertaluonteisten GUI-/samplevarausten jälkeen.
  Käyttäjä kuuli nolla dropoutia tai glitchiä; massiivisen 352-kaapelin
  renderöinnissä oli ajoittaista pientä mutta ei kasvavaa hitautta, samalla kun
  Drawer↔Night-vaihdot pysyivät nopeina.
- K-Mix ASIO hyväksyttiin single-device-tilassa: 48 kHz, block 64 ja hardware
  buffer 256 käynnistyivät normaalilla äänellä ja GUI-liikkeellä, molemmat
  fyysiset inputit näkyivät raw live L/R -presence-riveissä ja main kuului
  ulostuloista 1/2. Kolme statusvastausta pysyi noin 47 986–48 003 Hz:ssa.
- Neljän ulostulon portissa stem 1/2 kuuluivat erikseen K-Mixin fyysisistä
  ulostuloista 3/4: vain Coco1 avasi 3:n ja vain Coco2 avasi 4:n. PANIC hiljensi
  sekä mainin että stemit ja palautus toimi. Sen jälkeen toimiva split-WASAPI
  palautettiin last-working-baselineksi.
- Puuttuvan laitteen fyysinen rollback-koe ei läpäissyt: K-Mix irrotettiin
  vahvistetun candidate-restartin edellä, mutta backend avasi responsiivisen
  näennäissession. Ensimmäinen actual-SR-lukema oli edellisen terveen serverin
  vanha 48 kHz; sen jälkeen kello romahti noin 7076 → 7020 Hz:iin, ääni
  glitchasi ja Night putosi noin 1 FPS:ään. Yhden lukeman guard ehti siksi
  promotoida profiilin. `CQ.free`, split-overlayn manuaalinen palautus ja reboot
  palauttivat normaalin äänen ja GUI:n; uusi PID raportoi jälleen
  47 986–48 003 Hz.
- Guard lukee nyt kolme näytettä 0,75 s välein ja vaatii jokaisen terveeksi,
  jolloin vanha ensimmäinen statusarvo ei voi peittää käynnistyksen romahdusta.
  Exact fyysinen riskiportti ei toisteta; `[48000, 7076.4, 7019.8]` on
  regressiotestattu hermettisesti. Compile ja unit ovat vihreät:
  `CQ_COMPILE_OK`, **13 502 / 0 / 37/37**.
- **Jatka tästä:** älä aja system default- tai puuttuvan laitteen koetta tällä
  koneella uudelleen. Loopback-näkyvyyden portti (kohta 9) on sen jälkeen tehty
  ja kirjattu alla olevaan hand-offiin; jäljellä on vain kaksisuuntainen
  VB-CABLE-portti, joka vaatii ajuriasennuksen.

**Hand-off 2026-07-30 (b) — jatka testaamista tästä**

- Split-WASAPI-baseline varmistettiin tämän session alussa: `Ctrl+Shift+L` +
  `CQ.boot` tuotti normaalin äänen, sujuvan Night-liikkeen, rivin
  `running · 48 kHz · block 64 · hw 256` sekä toimivan live L/R- ja
  master-presencen. Kolmen näytteen actual-SR-guard on siis myös fyysisesti
  ajetussa class libraryssä.
- **Kohta 9 on tehty ja kirjattu siinä rajauksessa minkä se todistaa.** Realtek
  `Stereo Mix` oli koneella valmiiksi aktiivinen, mutta se listautuu exact
  input-valintana vasta `show all drivers` -kytkimen takaa, koska sen host API
  on `Windows WDM-KS` — yksi `CQAudioDeviceCatalog.legacyWindowsPrefixes`in
  kolmesta. Recommended ei siis piilota ajurin tarjoamaa loopbackia, vaan
  luokittelee sen ei-suositelluksi; exact valinta säilyy. Sivu ei väitä
  luoneensa kytkentää. Vahingossa tehty laitevalinta palautui puhtaaksi ilman
  toimenpiteitä, koska `dirty` on arvovertailu (`CQAudioConfig.sc:221`): gate
  luki jälleen `no changes`, mitään ei kirjoitettu eikä serveriä restartattu.
- **Avoin, käyttäjän pyytämä oikea DAW-portti: VB-CABLE A+B, oma päivä.**
  WDM-KS-loopback ei kelpaa kaksisuuntaiseksi reitiksi: split vaatii saman host
  API:n molemmissa päissä, ja WDM-KS on exclusive-mode kernel streaming, jota
  DAW ei pysty pitämään auki toisesta päästä yhtä aikaa. Yölahti ei myöskään
  saa luoda kytkentää itse (AIO.1), joten silta on oltava olemassa ennen
  bootia. Suunniteltu reitti: DAW → `CABLE-A Input`, Yölahden input =
  `CABLE-A Output`, Yölahden main out = `CABLE-B Input`, DAW monitoroi ja
  nauhoittaa `CABLE-B Output`ista; molemmat Yölahden päät ovat WASAPI. Ennen
  koetta: käyttäjä asentaa ajurin (agentti ei asenna eikä muuta järjestelmän
  audioreititystä) ja asettaa kaapeleille 48 kHz Windowsin ääniasetuksista.
  Huomioi kokeessa kaksi asiaa: monitorointi siirtyy DAW:lle, koska main out
  menee kaapeliin, ja **restart palauttaa patchin oletuksiin**, joten
  `preASrc`/`preBSrc` ovat jälleen `liveL`/`liveR` heti restartin jälkeen —
  tarkista se ennen kuin syötät kaapeliin mitään, jos lähde voi kuulla
  Yölahden oman ulostulon.
- Älä valitse tällä koneella `system default` -reittiä äläkä tee uudelleen
  irrotetun/puuttuvan laitteen rollback-koetta. Molempien havaittu failure on
  dokumentoitu yllä ja stale-first-reading-regressio on katettu hermettisesti.
- Muut fyysiset AIO-kohdat on testattu: split, USB single-device, inputit 1/2,
  main 1/2, stemit 3/4, PANIC, stereo/4ch REC ja 10 minuutin tiheä patch.
  **Käyttäjä hyväksyi fyysisen AIO-katselmuksen tässä rajauksessa 2026-07-30**,
  joten AIO on valmis vaihe ja VB-CABLE-portti kulkee omanaan. Sama K-Mixin
  4-kanavainen stem-portti sulki myös vanhan KUUNTELU 7:n 7.4/7.5-laitteistovelan.
  Erillinen pitkä koko soittimen hardware/resource-soak on seuraava vaihe.

**Definition of done — täyttyi 2026-07-30:** käyttäjä hyväksyi static layoutin
(AIO-1, 2026-07-29) sekä oikeat input→Coco→main-output-ketjut split-WASAPIlla ja
K-Mix ASIOlla, ja lopuksi koko fyysisen hardware-I/O-katselmuksen 2026-07-30.
Nelikanavainen stem-koe sulki samalla vanhan
KUUNTELU 7:n 7.4/7.5-laitteistovelan. `SPEC`, `GUI_SPEC`, README/config.example,
testit ja tämä status päivitetään samassa hyväksyntäcommitissa; overlayt ja
oikeat device-nimet eivät näy Git-diffissä. Tämä AIO-portti todistaa
laitevalinnan, kanavareitityksen, rollbackin ja telemetrian toiminnallisuuden;
sen lyhyt vakauskoe **ei korvaa** jonossa seuraavaa erillistä pitkäkestoista
koko soittimen laitteisto-/resource-soakia.

## Tie 1.0:aan (käyttäjän päätös 2026-07-30)

Käyttäjän arvio on, että soitin alkaa vasta nyt olla lähellä todellista
1.0-tilaa. Järjestys on **AUD-W → soak → VB-CABLE → merge `main`iin ja tagi**,
ja huokoiset soittotilat jäävät tarkoituksella tämän ulkopuolelle.

0. [x] **MOR-1 — morph ei saa jäädyttää käyttöliittymää.** Löytyi soakissa
   2026-07-30 ja **hyväksyttiin samana päivänä** (checklist MOR-1); ks. sopimus
   alla. Oli ensimmäisenä, koska rikkoi turvainvarianttia #3 (panic aina
   tavoitettavissa).
1. [x] **DAW-silta (ReaRoute).** **Käyttäjä siirsi tämän jonon kärkeen
   2026-07-30**, koska soittimen kytkeminen Reaperiin on hänen seuraava
   käytännön tarpeensa. **Keino vaihtui samalla VB-CABLEsta ReaRouteen**, ks.
   perustelu alla. **Silta todettiin toimivaksi molempiin suuntiin 2026-07-31**
   (checklist DAW-1); toimiva resepti ja sen karikot ovat alla.
2. **AUD-W — ajonaikainen audiokelloguard.** Oma pieni portti, tehdään
   **ennen** soakia, jotta soak voi ylipäätään havaita sen mitä tämä valvoo.
   Sopimus alla. **Toteutettu, portit vihreät ja osa 1 hyväksytty 2026-07-31**
   (malli, guard, Audio-sivun rehellinen tila ja työkalurivin aina näkyvä
   merkki). Auki on enää osa 2, oikea lepotilaprovokaatio, joka tehdään soakin
   yhteydessä.
3. **Pitkä laitteisto-/resource-soak.** Koko soittimen pitkäkestoinen vakaus;
   AIO:n 10 minuutin koe ei korvaa tätä. **Kantaa nyt myös AUD-W:n osan 2**
   (käyttäjän päätös 2026-07-31) — mutta soak-monitori pitää koneen hereillä,
   joten lepotilaprovokaatio tarvitsee oman ikkunansa monitorin pysäytyksen
   jälkeen. **Koeprotokolla on kirjoitettu 2026-07-31: checklist-lohko
   `SOAK-1`** (`tests/manual_checklist.md`) neljässä vaiheessa — käynnistys +
   tiheä käsipatchaus, yön ajo aktiviteettisilmukalla ja `soak_monitor.ps1`:llä,
   aamun luenta, ja erillisenä ikkunanaan AUD-W:n osa 2. **Soakattava
   kokoonpano on K-Mix ASIO single-device** (käyttäjän päätös 2026-07-31), ei
   `config.scd`:n split-WASAPI-pohja: se on sama laite jolla soitetaan, ja
   USB-laite + lepotila on tasan se ilmiö jota AUD-W vartioi. Yhdeksän
   hyväksyntäkriteeriä on kirjattu lohkoon **ennen ajoa**, jotta tulos
   tuomitaan eikä silmäillä. Aikabudjetti: yksi yö kelloa, noin tunti
   käyttäjän aikaa kolmessa ikkunassa.

   **Tilanne 2026-07-31 illalla — kaksi ajoa tehty:**
   - **Ajo 1 (6,53 h):** palvelin kesti moitteetta, mutta ajo löysi kolme
     vikaa (A morph-jäätymä, B valehteleva kelloguard, C tuhoisa
     `restart audio`). Kaikki kolme on korjattu ja **käyttäjän hyväksymiä**
     järjestyksessä C → B → A; ks. "SOAK-1:n tulos" alla.
   - **Ajo 2 (5,42 h, korjatulla koodilla):** **kahdeksan kriteeriä
     yhdeksästä vihreänä**, ja vaihe 4 sulki samalla AUD-W:n osan 2 sekä
     SOAK-FIX 3b:n oikeassa lepotilaviassa.
   - **Avoinna on tasan yksi asia: kriteeri 5, sclangin muistiramppi**
     (~20 MB/h, lineaarinen, sama molemmissa ajoissa). Seuraava teko ei ole
     kolmas yö vaan lähteen eristys: `tests/memory_probe.ps1` (kuusi
     skenaariota × 8 min, valvomaton, ei palvelinta eikä äänilaitetta).
     **Harness on rakennettu ja committoitu muttei vielä ajettu.**
   - Soakia ei siis ole vielä hyväksytty kokonaisuutena, ja se on viimeinen
     este ennen kohtaa 4 (merge `main`iin ja 1.0-tagi).
4. **Merge `main`iin ja 1.0-tagi.**

### DAW-sillan keino: ReaRoute, ei VB-CABLE

**VB-CABLE A+B ei pysty siihen mitä AIO.9:n hand-off siltä odotti.** Reaperilla
on yksi äänilaite kerrallaan. Jos sen ulostulo on `CABLE-A Input`, se ei voi
samaan aikaan soittaa kuulokkeisiin; ja jos se monitoroi Yölahdeltä palaavaa
raitaa, monitorointi menee suoraan takaisin `CABLE-A`:han eli Yölahteen —
takaisinkytkentä. Yölahti ei voi paikata tätä soittamalla kuulokkeisiin, koska
sen ulostulon pitää mennä `CABLE-B`:hen. **Kaksi stereokaapelia ja yksi laite
per sovellus eivät riitä molempiin suuntiin ja korviin yhtä aikaa.** Tämä on
suunnitteluvirhe hand-offissa, ei toteutuksessa; teksti jää tähän varoitukseksi.

**ReaRoute** on REAPERin oma virtuaalinen ASIO-ajuri (16 kanavaa kumpaankin
suuntaan), asennusohjelman valinnainen osa. Reaper pysyy **oikealla
laitteistollaan**, joten monitorointi toimii tavallisesti; Yölahti avaa
`ASIO : ReaRoute ASIO (x64)`:n **single-device-tilassa**, ja kanavia riittää myös stemeille
3/4. Kolmannen osapuolen ajuria ei asenneta. Rajat, jotka on tiedettävä:
ASIO-only, vanhaa koodia, ja **REAPERin on oltava käynnissä ennen Yölahden
boottia** — muuten laitetta ei ole olemassa. Se koskee myös laitelistaa: AIO
lukee laitteet vain pysähtyneenä, joten ReaRoute ilmestyy Audio-sivun listaan
vasta kun REAPER oli käynnissä sitä edeltävällä boot-/reboot-hetkellä.
**Ja vasta tuoreessa tulkissa** — ks. mitattu laki alla.

**Mitattu 2026-07-30: vasta asennettu ASIO-ajuri ei ilmesty käynnissä olevan
`sclang`-prosessin laitelistaan lainkaan.** ReaRoute oli asennettu oikein
(`HKLM:\SOFTWARE\ASIO\ReaRoute ASIO (x64)` → CLSID → olemassa oleva
`rearoute.dll`) ja REAPER oli päällä koko ajan, mutta Audio-sivu ei näyttänyt
sitä `single`- eikä `split`-listassa edes `show all drivers` päällä eikä
puhtaan `CQ.boot`in jälkeen. Tuore sclang näki sen heti: erillisajo löysi
20 in / 27 out, joista dedupin jälkeen 20/24, kun IDE:n sessio näytti 19/23 ja
recommended 4/5 — **erotus oli tasan yksi laite kumpaankin suuntaan, ReaRoute,
ja kaikki muu täsmäsi riviltä riville.** Syy on se, että ajuriluettelo elää
tulkkiprosessissa (`ServerOptions.inDevices` → `_ListAudioDevices` → PortAudio),
ei scsynthissä; palvelimen restart ei siis voi uusia sitä. **Laki: uusi
ääniajuri vaatii uuden sclangin (IDE → Language → Reboot Interpreter), ei
palvelimen restarttia.** Sama koskee mitä tahansa myöhemmin asennettavaa
ASIO-ajuria. Tästä seuraa yksi tunnettu tekstivelka: Audio-sivun engine-rivi
lupaa `restart audio to rescan`, mikä ei pidä paikkaansa juuri asennetulle
ajurille (`CQGuiAudioSetup.deviceListNote`, `GUI_SPEC.md` §Audio).

Yölahden puolella tämä on tavallinen AIO-laitevaihto: se kulkee Audio-sivun
vahvistetun apply & restart -transaktion läpi ja palautuu yhdellä rollbackilla,
jos laitetta ei saada auki. Split-WASAPI jää last-working-baselineksi.

**Valmis ja todettu toimivaksi molempiin suuntiin 2026-07-31** (checklist
DAW-1). Toimiva resepti, tässä järjestyksessä:
1. **Asenna ReaRoute.** REAPERin asennusohjelma uudelleen järjestelmän-
   valvojana, komponenttilistalta **ReaRoute ASIO** (ei ole oletuksena päällä).
   Varmistus: `HKLM:\SOFTWARE\ASIO` saa rivin `ReaRoute ASIO (x64)` — huomaa
   `(x64)`, 64-bittinen nimi on eri kuin 32-bittisen puolen `ReaRoute ASIO`.
2. **REAPER ASIOlle omalla laitteistollaan.** Audio system **ASIO**,
   driver `K-MixUSBAudio ASIO Driver`. Tämä ei ole makuasia: **ReaRoute on
   olemassa vain kun REAPER itse ajaa ASIOlla** — WASAPI/DirectSound-tilassa
   sen kanavia ei ole lainkaan. Sample rate **48 000** (`Request sample rate`
   rastittuna): ReaRoute kulkee REAPERin kellolla ja Yölahti ajaa 48 kHz:llä.
   Lisäksi **"Close audio device when stopped and application is inactive" pois
   päältä** — muuten laite katoaa aina kun REAPER menee taustalle ja Yölahden
   palvelin putoaa mukana, mikä näyttäisi satunnaiselta kaatumiselta.
3. **Tuore sclang.** Language → Reboot Interpreter. Ilman tätä ajuri ei näy
   listassa millään; ks. mitattu laki yllä.
4. **Yölahden laitevaihto.** REAPER ensin pystyyn, sitten `CQ.boot`; Audio-sivu
   → `single device` → `ASIO : ReaRoute ASIO (x64)` → apply & restart. Tämän
   jälkeen Yölahti ei soi omiin korviin lainkaan — koko ulostulo menee
   REAPERiin, ja monitorointi on REAPERin puolella K-Mixillä.
5. **REAPERin raidat.** Paluuraita: input `ReaRoute 1/2`, record arm **ja input
   monitoring** päällä, master send päällä. Lähetysraita: lähde + Route → Add
   new hardware output → `ReaRoute 1/2`. Suunnat ovat toisistaan riippumattomat
   vaikka numerot ovat samat. Silmukka syntyy vain jos paluuraidalle lisätään
   hardware output ReaRouteen — sitä ei tehdä.

Kaksi kompastuskiveä, jotka veivät katselmuksessa aikaa ja jotka on syytä
tunnistaa uudelleen: **hardware output -rivin oma `M`** reititysikkunassa
(eri nappi kuin raidan mute; punaisena mitään ei lähde) ja **input monitoring**
— armattu raita näyttää mittarissa sisääntulon *vaikka monitorointi olisi
pois*, mutta silloin signaali ei mene mastereille eikä hardware outputeihin.
**Mittarin liike ei siis todista että ääni virtaa eteenpäin.**

Avoin, ei 1.0:n ehto: paluun kierrosviive ei kompensoidu automaattisesti, joten
paluuraidalle nauhoitettu materiaali on aikasiirtynyt. REAPERin oma
`Media playback offset` on paikka korjata se, kun se kerran mitataan.

### MOR-1:n sopimus — morph ei saa jäädyttää käyttöliittymää

**Mitattu 2026-07-30 soakin aikana, `tests/tmp/soak/soak-20260730-130127.csv`.**
Kahden tiheän snapshotin morph Memory-sivulla, pyydetty kesto **90 s**:
`sclang` nousi 100 %:iin yhdestä ytimestä klo 13:09:59 ja palasi normaaliin
13:15:15–13:15:30, eli **noin 320 s — 3,6-kertaisesti pyydettyyn nähden.**
Koko sen ajan GUI oli jäässä eikä **PANIC vastannut**. `scsynth` oli koko ajan
täysin terve (vastasi, kello 47 986 Hz, 8 synthiä, muisti vakaa), joten ääni
jatkui — soittaja ei siis saanut ääntä kiinni. `sclang`in working set kasvoi
354 → 523 MB ja tasaantui; kyse **ei** ole runaway-silmukasta vaan
kyllästymisestä. Kone oli akulla, mikä noin kaksinkertaistaa kertoimen, mutta
ei ole syy: verkkovirrassakin morph ylittäisi pyydetyn keston.

**Mekanismi.** `CQSnapshots.morph` (`CQSnapshots.sc:92`) ajaa `dur × 30` tikkiä
ja tekee joka tikillä `state.set`in jokaiselle jatkuvalle parametrille **sekä
erillisen `matrix.patch`in jokaiselle kahden snapshotin matriisisolujen
unionille**. `CQMatrix.patch` (`CQMatrix.sc:53`) tekee per solu sekä `prPush`in
serverille että `changed`-ilmoituksen. Tiheillä snapshoteilla se on luokkaa
400 + 400 tapahtumaa per tikki × 30 tikkiä/s. Kun tikin hinta ylittää 33 ms:n
budjetin, SystemClock-rutiini on pysyvästi myöhässä eikä tulkki palaa Qt:n
tapahtumasilmukkaan — PANIC-klikkaus jonottaa tulkille joka ei jouda.
**`stopMorph` on samasta syystä yhtä tavoittamaton**, joten morphia ei voi
myöskään keskeyttää.

**Korjaussuunta.** Primitiivi on jo olemassa eikä morph käytä sitä:
`CQMatrix.writeMatrix` (`CQMatrix.sc:87`) validoi ja clamppaa koko tiheän
matriisin, tekee `prPushAll`in (yksi push per destination, 22) ja **yhden**
ilmoituksen. Yhden tikin matriisikustannus romahtaa sadoista noin
kahteenkymmeneen. Lisäksi harkittava tikkitaajuuden pudotus §10:n 15 Hz
kattoon. Huom: ilmoituksen tyyppi vaihtuu `\patch`istä `\matrix`iin, joten
GUI:n ja undon kuuntelupolut on tarkistettava — `origin`/`committed`/
`operation` kulkevat `writeMatrix`in läpi, joten `\morphFrame`-semantiikka
säilyy.

**Hyväksyntäehdot.** (1) Kahden tiheän snapshotin morph pitää kielisäikeen
responsiivisena koko ajon. (2) **PANIC vastaa morphin aikana** — tämä on
invariantti #3, ei suorituskykytoive. (3) `stopMorph` keskeyttää morphin
kesken. (4) Morphin todellinen kesto pysyy määritellyn toleranssin sisällä
pyydetystä. (5) Mitattu tapaus ajetaan regressiona: tiheä lähtö- ja
kohdesnapshot, ei pelkkä harva pari. Portit: compile + unit + GUI-smoket;
DSP:hen ei kosketa.

**Toteutettu ja käyttäjän hyväksymä `nocturne-morph-freeze`-haaralla
2026-07-30 (checklist MOR-1).** Kolme muutosta `CQSnapshots.morph`iin:
1. **Yksi tiheä kirjoitus per kehys.** Unioni ratkaistaan kerran
   `[dest, src, wA, wB]`-indekseiksi, ja kehys lukee elävän matriisin, korvaa
   vain unionin solut ja kutsuu `CQMatrix.writeMatrix`iä kerran. Elävän
   matriisin lukeminen säilyttää molempien snapshotien ulkopuoliset solut
   täsmälleen yhtä koskemattomina kuin per-solu-patchaus jätti ne — tälle on
   oma testinsä.
2. **Kehykset ajastaa kulunut aika, ei tikkilaskuri.** Hidas kehys pudotetaan,
   joten morph osuu pyydettyyn kestoon sen sijaan että venyisi 3,6-kertaiseksi.
3. **Jokainen kehys luovuttaa vähintään `morphMinYield` (4 ms)**, olipa se
   miten myöhässä tahansa; muuten SystemClock ajaa myöhästyneen rutiinin heti
   uudelleen eikä tapahtumasilmukka tyhjene koskaan. Kehystaajuus on nyt
   `morphTickPeriod` = §10:n 15 Hz katto (matriisipainot on jo silotettu 50 ms
   ja gain-argumentit lagattu ≥ 30 ms, joten 66 ms askel ei kuulu portaana).

Katselmuksessa löytyi myös **spec-ajautuma, joka on nyt korjattu**: GUI_SPEC §10
lupasi että "the cable layer's matrix-driven repaint" on pacattu — ja se
todella on (`CQGuiCableOverlay.prRefresh`). Pacattu **piirto** ei kuitenkaan
pacaa **ilmoitusta**: jokainen `\patch` maksaa `.defer`in kolmessa
kuuntelijassa ennen kuin yhtäkään pacing-porttia kysytään. Siksi §10:een on
lisätty laki "a stream must be thinned at its source as well as at its
painter".

**Hyväksyntäkierroksen tulokset.** 90 s morph tiheillä snapshoteilla ei enää
jäädytä GUI:ta, PANIC vastaa, ja uusi morph kesken morphin toimii. Ensimmäisen
kierroksen ~5 sekunnin jäätyminen lyhyellä morphilla oli **kertaluonteinen
lämmittely** boottauksen jälkeisessä ensimmäisessä tiheässä morphissa, ei
kehyskustannus: kolme peräkkäistä 5 s morphia ovat täysin puhtaita. Kirjattu
lämmittelynä, ei avoimena vikana.

**Sama katselmus löysi Memory-sivun tekstikentät kelvottomiksi, ja ne korjattiin
kahdella kierroksella (hyväksytty 2026-07-30).** Löydökset ja niiden syyt:
1. **Kirjoitettu kesto ei mennyt perille ilman Enteriä.** `NumberBox` committoi
   `value`n vain Enterillä, ja koska `CQGui.prDefocus` jättää tekstikentät
   sivun ainoiksi fokusoitaviksi, mikään nappi ei vie fokusta eikä
   `focusLost` laukea. Nimikentässä vikaa ei ollut, koska se luki jo `.string`
   — juuri se epäsymmetria teki laatikosta yllättävän.
2. **Kursori vaati tuplaklikkauksen.** `QcNumberBox` käyttää yksöisklikkauksen
   arvonvetoeleeseensä eikä sitä voi kytkeä pois. Koska laatikko luetaan
   tekstinä joka tapauksessa, numerowidget ei tuonut mitään: se on nyt
   `TextField`, ja `prCommitDuration` on ainoa ovi joka jäsentää, muistaa ja
   kirjoittaa clampatun arvon takaisin.
3. **Punainen kirjoitusväri** oli Qt:n oletus (`typingColor = Color.red`), joka
   lukee virheenä. Nyt tavallinen `line`-muste.
4. **Natiivi sininen valinta ja puuttuva hover.** Molemmat kentät lainaavat nyt
   Ink-kielen oman Qt-palettinsa kautta: hover `spark` valikkolaatikon
   painolla, valinta `spaf` listarivin painolla. Yksi ovi,
   `CQInk.applyTextEntryInk`.
5. **Fokus ei vapautunut mistään.** Napit, snapshot-lista, molemmat valikot,
   paneelit ja paljas sivupohja palauttavat sen nyt `prReleaseEntries`in kautta.
6. **`arrived` leikkautui**, koska lukemaleveys mitattiin pelkästä `"100%"`:sta.
   Nyt jokaisesta sanasta jonka mittari voi tulostaa, testillä pinnattuna.
7. **Käyttäjän pyytämä swap:** `a → b` -erottimesta tuli itse vaihtokontrolli
   (kaksipäinen nuoli), joten se ei vie lisätilaa kiskolta.

Koko GUI:ssa on täsmälleen kaksi tekstikenttää, molemmat Memory-sivulla, ja
molemmat saivat saman kohtelun.

Portit 2026-07-30: `CQ_COMPILE_OK`; **13 544 unit-passia / 0 failurea /
37/37 luokkaa**; `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED`, `CQ_GUI_NIGHT_PASSED`, `CQ_GUI_AUDIO_PASSED`
ja `CQ_GUI_AUDIO_PRESENCE_PASSED`. `CQ_SAFETY_PASSED` ja `CQ_HOLD_OK` kantavat:
DSP:hen ei koskettu, vain kielen puolen kirjoitus- ja ajastustapaan.

### AUD-W:n sopimus

**Ongelma, mitattu kahdesti fyysisesti.** Kelloguard ajetaan täsmälleen kerran,
session buildissa ennen graafin allokointia
(`Cocoquantus.prBuildSession`, `Cocoquantus.sc:706`). Ajonaikaista audiokellon
valvontaa ei ole, ja `Cocoquantus.serverResponsive` (`Cocoquantus.sc:105`)
tarkistaa vain `serverRunning && unresponsive.not` — **täsmälleen se mittari,
joka valehteli molemmilla kerroilla.** Jos laite kuolee kesken session (kortti
irti, ajurin degradoituminen, lepotilasta herääminen), sovellus on muodollisesti
running, GUI näyttää normaalilta, ääni on roskaa eikä mikään pinta kerro
totuutta.

**Lait, joista ei oikaista:**

- **Ei automaattista recoveryä.** Itsestään reboottaava soitin kesken esityksen
  on pahempi kuin rehellinen virhetila. Tavoite on lakata valehtelemasta, ei
  korjata itseään.
- **Ei uutta serverliikennettä.** `Server.actualSampleRate` täyttyy jo olemassa
  olevan status-watcherin `/status`-vastauksista, joten valvonta on pelkkä luku.
  **Valvonta ei saa missään tilanteessa kutsua `_ListAudioDevices`ia** eikä
  muuta käynnissä olevaa WASAPI-serveriä koskevaa laitelistausta — se on
  2026-07-29 eristetty jumin syy.
- **Matala taajuus.** Tarkistusväli luokkaa 2–5 s, ei telemetrian 15 Hz:ää.
  Terve tila ei liikuta ainuttakaan mustetta (§16.3/§16.10 ja §9:n laki:
  ambient-liike tulee telemetriakehyksistä, ei omasta kellosta).
- **Rehellinen danger-tila ja tavoitettava `restart audio`.** Tila näkyy
  Audio-sivun status-rivillä, mutta koska vika voi iskeä kun Drawer on kiinni,
  se tarvitsee myös aina näkyvän pinnan. Muoto ratkaistaan omana katselmuksena;
  se on staattinen ink-tila, ei vilkkuva hälytys.
- **Sama epäterveyden määritelmä kuin boot-portissa.** `audioClockRatioBounds`
  ja `prAudioClockHealthy` ovat jo olemassa; niitä käytetään, ei kirjoiteta
  toista rajaa. Yksi huono lukema ei riitä tilan vaihtoon — sama settle-logiikka
  kuin `prRequireSettledAudioClock`issa.
- **Guard ei saa laueta suppenevaan transienttiin.** Mitattu 2026-07-30:
  `actualSampleRate` on juokseva keskiarvo, joten Modern Standbystä palaava
  istunto kulkee **molempien** rajojen ulkopuolella ennen asettumistaan — mitattu
  sarja oli 7419 Hz (ratio 0,15) → ylitys 69 184 Hz (ratio 1,44) → 47 986 Hz noin
  90 sekunnissa. Naiivi kolmen näytteen tarkistus olisi julistanut vian kahteen
  kertaan, molempiin suuntiin, vaikka mikään ei ollut rikki. Ajonaikaisen guardin
  ehto on siis **jumissa oleva** lukema, ei pelkkä rajan ulkopuolinen: vaadi
  suunnan ja suppenemisen tarkastelu tai riittävän pitkä ikkuna, ja kirjoita
  tämä mitattu sarja regressiotestiksi samalla tavalla kuin
  `[48000, 7076.4, 7019.8]`. Boot-portin kertaluonteinen tarkistus ei kärsi
  tästä, koska se ajetaan vasta uuden serverin omista lukemista.
- **Modern Standby kuuluu tähän ilmiöön, ei vikoihin.** Kone menee
  matalatehotilaan näytön sammuessa riippumatta `powercfg`in lepotila-arvosta;
  se pysäyttää audiostriimin ja tuottaa juuri yllä kuvatun transientin.
  `tests/soak_monitor.ps1` pitää järjestelmän hereillä ajonsa ajan, jotta
  valvomaton soak ei mittaa tätä artefaktia.
- **Ei elinkaarivuotoja.** `CQ.free`, `CQ.reboot`, CmdPeriod ja epäonnistunut
  session lopettavat valvonnan; sukupolvisuoja pätee kuten muihinkin
  async-polkuihin. Testi todistaa, ettei routine/OSCdef jää eloon freen jälkeen.

**Portit:** compile + unit + Audio/presence/Drawer/shell-smoket. DSP:hen ei
kosketa, joten `CQ_SAFETY_PASSED` ja `CQ_HOLD_OK` kantavat, jos näin on.
Fyysinen hyväksyntä vaatii yhden turvallisen provokaation — ei irtikytkentää
kesken restartin (se on kirjattu ei-toistettava koe), vaan esimerkiksi
lepotila/herätys tai soakissa itsestään ilmenevä tapaus.

**Toteutettu 2026-07-31, odottaa fyysistä hyväksyntää (checklist AUD-W).**

- `CQAudioClockWatch` on portin puhdas malli: ei serveriä, ei GUI:ta, ei omaa
  kelloa. **Boot-portin raja ja predikaatti muuttivat sinne**, ja
  `Cocoquantus.audioClockRatioBounds` / `prAudioClockHealthy` ovat nyt
  läpivientejä — yksi määritelmä, ei toista rajaa, ja testi vahtii että ne
  pysyvät samana.
- **Vian ehto on jumi, ei rajan ylitys.** Tila on `\danger` vasta kun lukemat
  ovat epäterveitä koko ikkunan ajan **eivätkä suppene** kohti pyydettyä
  taajuutta. Mitattu Modern Standby -sarja on regressiotestinä sekä alkuperäisinä
  kolmena näytteenä että tiheämpänä versiona; kumpikaan ei laukaise guardia.
- **Ikkuna on 60 s eikä 20 s**, ja se on tietoinen kauppa. Suppenemistarkistus
  lukee vain jakson päät, joten lyhyt ikkuna julistaisi vian ehjästä koneesta jos
  toipuva striimi viipyy tasanteella. Väärä hälytys on tässä pahempi kuin
  myöhäinen totuus, koska guardin koko tarkoitus on lakata valehtelemasta.
  Vakiot ovat `CQAudioClockWatch`in luokkametodeja, joten fyysinen katselmus voi
  muuttaa niitä yhdestä paikasta.
- **Elinkaari tuli ilmaiseksi oikeasta koukusta:** valvonta syntyy
  `prWatchServer`issa ja kuolee `prUnwatchServer`issa, jonka kautta kulkevat jo
  `free`, CmdPeriod, epäonnistunut boot ja odottamaton scsynthin kuolema. Testi
  todistaa ettei routine jää eloon.
- **Vain näkyvän totuuden muutos ilmoitetaan.** `\watching` on tarkoituksella
  näkymätön, terve kello on täysin hiljainen, eikä guard piirrä mitään omasta
  kellostaan. Lukeminen on `Server.actualSampleRate` eli status-watcherin jo
  keräämä arvo: ei uutta serverliikennettä, ei laitelistausta.
- **Audio-sivu kertoo sen kahdessa paikassa:** engine-rivi
  `audio clock stuck · reports N Hz` danger-perheessä pyydetyn taajuuden
  *rinnalla*, ja status-rivi `audio clock stuck · restart audio`. Automaattista
  recoveryä ei ole eikä tule.
- Samassa portissa korjattiin luvattu tekstivelka: laitelistan huomautus on nyt
  `scanned at boot · restart audio to rescan · new driver: restart sclang`
  (`GUI_SPEC.md` §Audio, testi mukana).
- Portit ajettu 2026-07-31: `CQ_COMPILE_OK`, **13 595 unit-passia / 0
  epäonnistumista / 38/38 luokkaa**, `CQ_SAFETY_PASSED`, `CQ_GUI_AUDIO_PASSED`,
  `CQ_GUI_AUDIO_PRESENCE_PASSED`, `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
  `CQ_GUI_DRAWER_REENTRY_PASSED`, `CQ_GUI_NIGHT_PASSED`. `CQ_HOLD_OK` kantaa,
  DSP:hen ei koskettu.

- **Aina näkyvä pinta: työkalurivin drawer-nappi** (käyttäjän päätös
  2026-07-31). Merkki on staattista mustetta pysyvässä kromissa — täytetty
  danger-piste ja samat sanat kuin Audio-sivulla, oikeaan reunaan napin viereen.
  Se istuu **ainoalla tiellä ainoaan korjaukseen**, eli vihje ja teko jakavat
  saman geometrian (§4.5). Ei vilku, ei sykki, eikä sitä ole olemassa muulloin
  kuin vikatilassa. Sijoittelu on puhdasta geometriaa
  (`CQGuiShell.clockDangerLayout`), joten se testataan ilman piirtotapahtumaa;
  jos otsikko ei jätä tilaa, sanat putoavat ja piste jää — piste on tila, sanat
  ovat kohteliaisuus. Vikatilassa avattu GUI aukeaa merkki jo päällä.
- Portit ajettu uudelleen merkin kanssa 2026-07-31: `CQ_COMPILE_OK`,
  **13 595 passia / 0 / 38 luokkaa**, ja kaikki kuusi GUI-smokea vihreinä
  (`CQ_GUI_SHELL_PASSED` sisältää nyt merkin mahtumisen 1280 px:n minimissä).

**Osa 1 hyväksyttiin 2026-07-31** (checklist AUD-W): simuloitu jumi — guardille
kerrotaan että pyydettiin 96 kHz vaikka laite ajaa 48 kHz, jolloin ääneen ei
kosketa lainkaan mutta guard näkee todellisen vikatilanteen — sytytti merkin ja
Audio-sivun mitatun taajuuden, PANIC vastasi danger-tilassa, ja palautus sammutti
merkin heti. **Katselmus löysi kaksi vikaa, molemmat korjattu:**

1. **Pysäytys nollasi tilan hiljaa** (`bbcad67`), joten valvonnan
   uudelleenkäynnistys jätti työkalurivin väittämään vikaa jota malli ei enää
   väittänyt. Oletukseni oli, että pysäytys tapahtuu vain kun GUI on muutenkin
   katoamassa. **Näkyvä tila johon pääsee sisään muttei ulos on vika**, olipa
   ulosreitti kuinka harvinainen tahansa; laki on nyt `GUI_SPEC`issä.
2. **Laitelistan huomautus paisutti engine-rivin** `show all drivers` -napin alle
   (`d331ff4`). Lyhentäminen olisi vain siirtänyt törmäystä, koska AUD-W kasvattaa
   samaa riviä juuri silloin kun sen pitää olla luettavin — laskurit ja huomautus
   saivat oman `device list` -rivin.

**Avoinna: osa 2, oikea lepotilaprovokaatio.** Käyttäjän päätös 2026-07-31 on
tehdä se **osana pitkää soakia**. Yksi ehto on tiedettävä etukäteen:
`tests/soak_monitor.ps1` pitää järjestelmän hereillä koko ajonsa ajan, juuri
siksi ettei valvomaton soak mittaisi Modern Standby -artefaktia — joten osa 2
tarvitsee **oman ikkunansa monitorin pysäytyksen jälkeen** tai ajon ilman
monitoria. Muuten kone ei nukahda eikä provokaatiota tapahdu.

### SOAK-1:n tulos 2026-07-31 — palvelin kesti, kolme vikaa löytyi

**Ajo:** 6,53 h (01:56–08:27), K-Mix ASIO single-device, `\selfNoise`-parametrit
+ `\mayhem`-matriisi (352 solua), aktiviteettisilmukka 60 s tikillä, monitori
20 s välein. CSV `tests/tmp/soak/soak-20260731-015614.csv`, **1174 näytettä**.

**Vihreä ja kiistaton — palvelinpuoli ei horju:**

- 1174/1174 `/status`-vastausta, **0 epäonnistumista**; 0 epätervettä
  kellonäytettä; actual SR 47 986–48 021 Hz.
- Yksi ja sama scsynth- ja sclang-PID koko ajon.
- Server CPU ka **11,89 %**, suurin avg 14,2 %, suurin peak 17,9 % (SPEC §9:n
  katto 25 %) — tasainen kaikkina seitsemänä tuntina, ei nousevaa trendiä.
- Synthejä **8** joka näytteessä; scsynthin handlet **täsmälleen 344** ja
  työjoukko 55–78 MB ilman trendiä; sclangin handlet 1717 → 1719 ja säikeet 32.
  **Ei handle- eikä säievuotoa.**

Kriteerit 1–4, 6 ja 7 täyttyivät. Kriteeri 5 (muisti) jäi auki ja kriteerit
8–9 kaatuivat. Löydökset korjataan käyttäjän 2026-07-31 päättämässä
järjestyksessä **C → B → A**: C tekee kahdesta muusta toipumisen
mahdottomaksi, B on pieni ja tarkka, A on suurin työ.

#### Löydös C — `restart audio` vastaamattomalle palvelimelle hävittää soittimen

**Mitattu 2026-07-31 08:45–08:55.** Lepotilan jälkeen palvelin oli
vastaamaton, ja Audio-sivun oma korjausreitti painettiin. Tapahtui tämä:

```
Cocoquantus: freed.
Server 'localhost' is currently unresponsive. Forcing process to stop via system command.
SUCCESS: The process with PID 6012 has been terminated.
Cocoquantus: cached 20 input / 24 output audio devices before server boot.
```

GUI purkautui eikä palannut. Mitattu tila sen jälkeen: **ajossa ollut scsynth
PID 17716 oli yhä elossa** (CPU jäässä 3507 s, säikeet 9 → 7) ja `netstat`
osoitti sen omistavan **UDP 57110:n**, joten uusi palvelin ei voinut varata
porttia. Pakkopysäytys oli tappanut **PID 6012:n**, joka ei ollut tämä
palvelin; mikä se oli, ei ole enää todennettavissa. Toipuminen vaati
orvon tappamisen käsin.

**Mekanismi (hypoteesi, todennettava korjauksessa):** Windowsilla `Server`in
muistama pid ei ole varsinainen scsynth-prosessi, ja tämä polku ajetaan vain
kun palvelin on vastaamaton — käytännössä se ei ole koskaan päässyt oikeaan
testiin. Lisäksi transaktio vapauttaa GUI:n **ennen** kuin uusi boot on
onnistunut, joten epäonnistuessaan se jättää soittajan ilman mitään pintaa.

**Hyväksyntäehdot.** (1) Vastaamattoman palvelimen pysäytys pysäyttää
**todella sen palvelimen** — todennus PID:llä ja portilla, ei tulosteella.
(2) Mitään PID:iä ei force-killata ennen kuin on varmistettu että se on tämän
session scsynth; tuntematon tai kierrätetty PID ei ole kelvollinen kohde.
(3) Jos boot epäonnistuu, **käyttöliittymä palaa** rehelliseen virhetilaan
josta uudelleenyritys on tavoitettavissa — soittaja ei koskaan jää ilman
pintaa. (4) Portin varaus tarkistetaan ennen bootia, jotta orpo havaitaan
syyksi eikä boot jää epämääräisesti jumiin. (5) Pid/portti-logiikka katetaan
hermeettisellä testillä; fyysinen tapaus jää checklist-riviksi.

#### Löydös B — kelloguard väittää `ok` jäätyneestä lukemasta

**Mitattu 2026-07-31 08:40.** Palvelin ei vastannut yhteenkään neljästä
riippumattomasta `/status`-koetuksesta, ja sen ääniketju oli kuollut (CPU
jäässä, säikeet 9 → 7), mutta `Cocoquantus.audioClockState` sanoi **`ok`** ja
`audioClockRate` **48 003,47 Hz** — edellisillan lukema.

**Mekanismi, todennettu.** `Server.actualSampleRate` päivittyy vain
`/status`-vastauksista. Kun vastaukset loppuvat, arvo jäätyy viimeiseen
terveeseen lukemaansa, joten suhdetesti sitä vastaan **ei voi enää koskaan
epäonnistua**. Guardin suunnittelu oletti, että lukemia tulee jatkuvasti.

**Laki, joka tästä jää:** *välimuistista lukevan vartijan on vaadittava
tuoreutta — hiljaisuus ei ole terveys.* Sama sukua kuin `/cq_aura`-laki "no
telemetry is not the same as zero" ja MOR-1:n "gate joka voi valehdella on
vika". Kirjataan `GUI_SPEC`iin korjauksen mukana.

**Hyväksyntäehdot.** (1) Guard vaatii tuoreen lukeman: jos uutta
`/status`-vastausta ei ole tullut määritellyn ikkunan sisällä, se **ei saa
väittää `ok`**. (2) Mitattu tapaus (jäätynyt terve lukema + ei vastauksia) on
regressiotesti. (3) Suppenevan transientin laki ei saa rikkoutua — mitattu
Modern Standby -sarja ei edelleenkään laukaise vikaa. (4) **Käyttäjän
päätettävä oma kysymyksensä:** kattaako työkalurivin aina näkyvä merkki myös
`server not responding` -tilan. AUD-W:n oma perustelu merkille — "vika voi
iskeä kun Drawer on kiinni" — pätee siihen sanasta sanaan, ja juuri se vika
tapahtui, mutta se on GUI-semantiikkaa eikä sitä arvata.

#### Löydös A — morph jäädyttää kielisäikeen, PANIC ei vastaa

**Mitattu koko yön ajan.** Joka ~8. minuutti eristetty 20 s morph tuotti 2–3
peräkkäistä näytettä, joissa sclang käytti **17–20 s CPU:ta per 20 s
seinäkelloa** eli yhden ytimen täysin, 40–60 sekunnin ajan. Käyttäjän
katselmus 08:35: GUI jäätyy, **PANIC ei vastaa Spacella eikä koodista** (vain
viiveellä), ja ääneen tuli glitchejä joiden syytä ei ole todennettu. Tämä on
**turvainvariantti #3**, sama jonka MOR-1 sulki 2026-07-30 — se aukeaa
uudelleen 352 kaapelin tiheydellä.

**Piirtohypoteesi on kumottu mittauksella:** sama jäätyminen tapahtuu myös
Audio-sivu auki, jolloin Night on StackLayoutissa piilossa eikä piirrä
lainkaan. Kustannus on kielipuolella, ei kaapelimusteessa.

**Jäljelle jäävä hypoteesi:** MOR-1 tiivisti vain matriisipuolen. Morph tekee
yhä joka kehyksellä `state.set`in **jokaiselle jatkuvalle parametrille** —
luokkaa 60 parametria × 15 Hz ≈ 900 kirjoitusta ilmoituksineen sekunnissa.

**Toinen havainto, joka sitoo tämän muistiin:** morphin CPU-hinta **kasvoi
yön mittaan** samaa tahtia sclangin työjoukon kanssa — tunnin 0 keskiarvo
14,5 s → tunnin 6 keskiarvo 17,6 s, kun työjoukko kasvoi 306 → 412 MB.
A ja alla oleva muistiramppi voivat olla **sama ilmiö** (kasvavan keon
hinta), eivät kaksi. Se on tarkistettava ennen kuin niitä korjataan erikseen.

**Hyväksyntäehdot.** MOR-1:n neljä ehtoa uudelleen, mutta **352 kaapelin
tiheydellä ja tuntien ajossa**, ei tuoreella bootilla: kielisäie pysyy
responsiivisena, PANIC vastaa morphin aikana, `stopMorph` keskeyttää, ja
todellinen kesto pysyy toleranssin sisällä. Regressio ajetaan sekä tuoreella
bootilla että pitkän ajon jälkeen, koska juuri se ero jäi MOR-1:ssä
huomaamatta.

#### Auki jäänyt kriteeri 5 — sclangin muistiramppi

Työjoukko tunneittain: 305,7 → 311,8 → 327,3 → 351 → 371,8 → 395,3 →
**411,6 MB**, eli noin **+20 MB/h eikä tasaantumista 6,5 tunnissa**. Ei yksi
vuotava tapahtuma vaan jatkuvaa kertymää (336 näytettä kasvaa, 72 laskee, 767
ennallaan). MOR-1:ssä mitattiin vastaava kasvu joka **tasaantui** ~523 MB:hen,
joten katto voi vielä tulla. Ratkaistaan löydöksen A yhteydessä, koska
korrelaatio morphin hinnan kanssa on vahva.

#### Korjaukset 2026-07-31 (C → B → A), portit vihreät, fyysinen hyväksyntä auki

**C — `CQServerProcess` ja kaksi rakenteellista lakia.** Uusi luokka on ainoa
ovi, joka pysäyttää palvelinprosessin väkisin, ja ainoa paikka joka kysyy
käyttöjärjestelmältä kuka porttia pitää. **Portti on todiste:** palvelin on
pysähtynyt kun sen soketti on vapaa, ei kun tappokomento tulosti SUCCESSin.
**Mitään ei tapeta ennen kuin imagen nimi on varmistettu:** `netstat` kertoo
pitäjän PID:n, `tasklist` sen imagen, ja vain `scsynth`/`supernova` on kelvollinen
kohde — tuntematon tai vieras pitäjä on rehellinen kieltäytyminen, jonka viesti
nimeää pitäjän. Kirjaston oma sokea tappo ei enää laukea: `Server:quit` ampuu
`unixCmd`in palauttamaan pid:iin (`Server.sc:1042`, Windowsilla käynnistävä
kuori), joten vastaamattomassa polussa pysäytämme varmistetun pitäjän itse ja
nollaamme sen jälkeen kirjaston muistaman pid:n. Parserit ovat puhtaita ja
testattuja; **testikaksoiskappale ei pääse käyttöjärjestelmään lainkaan** ilman
eksplisiittistä fixtureä, koska sen nimeämä portti voisi olla soittajan elävä
palvelin. Mitattu tapaus (orpo 17716 pitää UDP 57110:tä, kirjattu pid 6012) on
regressiotesti sekä `CQServerProcess`issa että elinkaaressa.

**B — tuoreusvaatimus.** `CQAudioClockWatch.push` sai `responding`-argumentin ja
`\stale`-tilan: kun lukemaa ei ole, viimeinen lukema unohdetaan (`lastRate` →
nil), jottei mikään pinta piirrä sitä mitattuna, eikä epäterve sarja jatku
evidenssin katkon yli. Facade julkaisee nyt **symbolin eikä boolia**:
`\clockStuck`, `\serverDown` tai `\none`. Työkalurivin merkki on sama piste ja
sama muste, mutta omat sanansa kummallekin vialle (käyttäjän päätös
2026-07-31) — `clockDangerLayout` mittaa käytettävät sanat, joten pidempi
"server not responding" ei luota lyhyemmän mittoihin. Pysäytys retraktoi
molemmat merkit, eli aiempi "sisään mutta ei ulos" -laki kattaa myös uuden.

**A — morph kirjoittaa vain sen mikä liikkuu.** Käyttäjän oma havainto osui:
Nightin vuorovesi on kevyempi koska se kirjoittaa **vain matriisin**
(`CQGuiNightMap.commitBlend` → `writeMatrix`, 30 Hz), kun taas snapshot-morph
teki lisäksi `state.set`in **jokaiselle** jatkuvalle parametrille joka kehyksellä
— ~60 kirjoitusta ilmoituksineen, ja jokainen kuuntelija defer-kutsuu erikseen,
eli ~2 700 deferrattua sulkeumaa sekunnissa Qt:n jonoon joka täyttyi nopeammin
kuin tyhjeni. MOR-1 tiivisti vain matriisipuolen. Nyt `CQState.setAll` on sama
primitiivi parametreille kuin `writeMatrix` on matriisille: sama clamppaus, sama
serveripush, **yksi ilmoitus**. Lisäksi morph erottaa liikkuvat parametrit
paikallaan pysyvistä — pari joka eroaa vain matriisissa (yön oma tapaus) ei
maksa kehyksessä parametriliikennettä lainkaan. Sama niputus koskee `prApply`ta
ja `prApplyDiscrete`ta, joten myös tavallinen recall on yksi teko eikä 74.

Mitattu `tests/morph_profile.scd`illa (headless, serverless):

| pari | kehyksiä | tila-ilmoituksia | per kehys |
|---|---|---|---|
| vain matriisi (yön oma pari) | 64 | 4 | **0,02** |
| myös nupit liikkuvat | 64 | 72 | **1,08** |

Ennen korjausta jokainen kehys ilmoitti kerran per jatkuva parametri: ~60 per
kehys, ~900/s. Kesto osui molemmissa 4,14–4,15 s pyydetystä 4 s:stä.
**Poistamista ei tarvittu** — käyttäjän esittämä vaihtoehto luopua morphista
jää käyttämättä, koska korjaus on sama primitiivi joka koodipohjassa jo oli.

**Mutta se ei riittänyt: käyttäjän katselmus 2026-07-31 osoitti jäätymisen
jatkuvan.** Ilmoitusmäärä laski 55-kertaisesti mitatusti, eikä se ollut
dominoiva termi. Kolmas arvaus jätettiin tekemättä ja tilalle rakennettiin
mittari: **`tests/morph_profile_gui.scd` ajaa morphin oikean Qt-GUI:n kanssa ja
laskee, montako 20 Hz kilpailevaa AppClock-tikkiä ehtii ajoon** — tasan se
jono, johon PANIC-klikkaus menisi. Tavoitettavuus on siis mitattavissa ilman
ihmistä, ja se paljasti todellisen syyn:

| vaihe | records | ink | tavoitettavuus (Night näkyvissä) |
|---|---|---|---|
| ennen | 122,9 ms | 42,2 ms | **8 %** (5/60 tikkiä) |
| O(n²) pois | 75,0 ms | 48,5 ms | 12 % |
| geometriacache | 3,5 ms | 41,3 ms | 37 % |
| vanhentunut blit + kustannustahdistus | 4,0 ms | 43,2 ms | **67 %** |

Vertailukohta ilman GUI:ta on 80 %, ja Drawer auki (Night piilossa) 72 %.
Kolme lakia, kaikki nyt `GUI_SPEC` §11:ssä:

1. **Geometria ei ole suure.** Kaapelin käyrä riippuu päätepisteiden
   sijainneista, ei painosta, joten morph ei saa rakentaa sitä uudelleen.
   Records-rakennus maksoi enemmän (122,9 ms) kuin koko muste jonka
   välttämiseksi se on olemassa; siitä osa oli vahinko-O(n²) (`select`
   `collect`in sisällä), loput 704 päätepistehakua ja 352 bezier-näytteistystä.
   Nyt geometria on cachessa avaimenaan päätepisteiden sijainnit.
2. **Vanhentunut blit voittaa jäätyneen ikkunan.** Morphissa kirjoitus osuu
   aina renderin ja maalauksen väliin, joten kuva ei ollut *koskaan* tuore ja
   jokainen maalaus piirsi 352 kaapelia suoraan maalaustapahtuman sisällä.
   Nyt kelvollinen samankokoinen kuva blitataan ja render ajastetaan.
3. **Virtaa tahdistaa sen oman työn hinta, ei kiinteä taajuus.** 15 Hz olettaa
   piirron mahtuvan kehykseen; 352 kaapelilla yksi muste on ~41 ms eli 62 %
   kielisäikeestä. Nyt piirtoon menee enintään yksi siivu kolmesta
   (`renderDuty`), joten tiheä patch animoituu karkeammin sen sijaan että
   soitin jäätyisi. Yksittäiset teot (plant, rip, harvest) piirtyvät heti.

Kaksi mittauksen löytämää karikkoa kirjattiin samalla: **ennen layoutia otettua
sijaintia ei saa cachettaa** (vanha polku korjasi itsensä joka kehyksellä,
cache jättäisi reiän ikuisesti) ja **yösmoken liikkuneiden päätepisteiden
assertio on se testi joka pitää tämän rehellisenä** — se kaatui ensimmäisestä
cache-versiosta ja pakotti avaimeen todelliset sijainnit.

**Löydös A hyväksyttiin käyttäjän katselmuksessa 2026-07-31** (checklist
SOAK-FIX osa 1): morph ei enää jäädytä käyttöliittymää ja PANIC vastaa aina.
Turvainvariantti #3 on jälleen ehjä 352 kaapelin tiheydellä. Tiheän morphin
kaapelianimaatio on tietoisesti karkeampi kuin ennen; tavallinen patch ei
muutu, koska sen muste on murto-osan hinnasta.

**Auki löydöksistä ovat B ja C**, joiden fyysinen todistus vaatii uuden
lepotilaprovokaation (SOAK-FIX osa 3), sekä **koko soakin uusinta-ajo**: kolme
kriteeriä (5 muisti, 8 vasteet, 9 lepotila) jäivät edellisessä ajossa auki tai
kaatuivat, joten soakia ei voi hyväksyä ilman puhdasta ajoa korjatulla koodilla.
Muistiramppi kannattaa lukea uudestaan juuri tässä ajossa: kaapelikerros
allokoi ennen korjausta jokaisella matriisikirjoituksella 352 käyrän verran
uusia Pointeja, mikä on uskottava mutta **todistamaton** osasyy +20 MB/h
-ramppiin.

Portit 2026-07-31: `CQ_COMPILE_OK`, **13 681 unit-passia / 0 failurea / 39
luokkaa**, `CQ_SAFETY_PASSED`, `CQ_GUI_SHELL_PASSED`, `CQ_GUI_DRAWER_PASSED`,
`CQ_GUI_DRAWER_REENTRY_PASSED`, `CQ_GUI_NIGHT_PASSED`, `CQ_GUI_AUDIO_PASSED`,
`CQ_GUI_AUDIO_PRESENCE_PASSED` ja `CQ_MORPH_PROFILE_DONE`. `CQ_HOLD_OK` kantaa:
DSP:hen ei koskettu. **Fyysinen hyväksyntä on auki** — checklist-lohko
`SOAK-FIX`, jonka osa 3 vaatii uuden lepotilaprovokaation.

#### Työkaluvelka — `soak_monitor.ps1` voi hukata oman yhteenvetonsa (korjattu)

Ctrl+C `Start-Sleep`in aikana ohittaa skriptin `finally`-lohkon, joten
`CQ_SOAK_SUMMARY` jäi tulostumatta yön ajosta. CSV oli tallessa ja luettiin
käsin, mutta **checklist käskee raportoimaan juuri sen yhteenvedon**. Sama
laji kuin `run_all.scd`:n vale-marker: portti joka voi hukata todisteensa.

**Korjattu 2026-07-31, eikä ovelammalla trapilla vaan lakina: yhteenveto
lasketaan todisteesta, ei muistista.** Se johdetaan aina CSV:stä, kirjoitetaan
levylle `soak-<aikaleima>-summary.txt` 15 näytteen välein ja on toistettavissa
jälkikäteen mistä tahansa ajosta:
`soak_monitor.ps1 -SummarizeCsv <polku>`. Päättyipä ajo miten tahansa — Ctrl+C,
tapettu ikkuna, sähkökatko — viimeisin yhteenveto on levyllä ja tarkka on yhden
komennon päässä. Todennettu palauttamalla yön kadonnut yhteenveto sen omasta
CSV:stä (6,61 h / 1189 näytettä / 0 epäonnistumista).

#### Löydös B:n jälkinäytös — aliaksen kadonnut ilmoitus (korjattu 2026-07-31)

Käyttäjän katselmus 3a:ssa: guard tulostaa varoituksensa, malli sanoo
`\serverDown`, `audioClockRate` on nil — ja **työkalurivin merkki ei syty**.
Kaksi hypoteesia kaatui mittauksessa (pinta toimii, ilmoitusketju toimii
tulkista ajettuna), kolmas löytyi kuuntelijaprobella: `Cocoquantus.changed`ille
rekisteröity probe ei kuullut mitään, vaikka julkaisuhaara ajettiin.

Syy: **`CQ : Cocoquantus {}` on aliasluokka.** `CQ.boot`in kautta kulkevissa
luokkametodeissa vastaanottaja on `CQ`, joten `this.changed(...)` julkaisi
`CQ`:lle samalla kun jokainen kuuntelija tilaa `Cocoquantus`ilta. Ilmoitus
katosi kirjaimellisesti tyhjään.

**Miksi tämä läpäisi AUD-W:n osan 1:** checklist ajoi guardin komennolla
`Cocoquantus.prStartAudioClockWatch(...)`, siis *toisella nimellä*, ja silloin
julkaisu osui oikeaan objektiin. **Testi oli samaa mieltä koodin kanssa vain
koska se kirjoitti saman nimen.** Laki: *luokkatason ilmoituksen on nimettävä
luokka, ei koskaan `this`.* Nyt on yksi ovi (`Cocoquantus.prPublish`), ja sama
korjaus tehtiin `CmdPeriod`-rekisteröinneille, joissa aliaksen kautta bootattu
ja perusnimellä vapautettu sessio olisi jättänyt rekisteröinnin roikkumaan.

Regressio ajaa guardin **aliaksen kautta** (`CQ.prAudioClockTick`) ja vaatii
että `Cocoquantus`in kuuntelija kuulee sen. Todennettu molempiin suuntiin:
ilman korjausta se kaatuu kolmesta assertiosta, korjauksen kanssa se on vihreä.

**Toinen soak-ajo 2026-07-31 (5,42 h, 976 näytettä): kahdeksan kriteeriä
yhdeksästä vihreänä.** 976/976 `/status`-vastausta, 0 epätervettä kellonäytettä,
yksi PID, SR 47 986–48 021 Hz, server CPU ka 11,96 % tasaisena, synthejä 8,
scsynthin handlet 334 ja sclangin 1714 liikkumattomina, säikeet 32. Vasteet
5,4 tunnin jälkeen hyväksyttiin, ja **vaihe 4 sulki sekä AUD-W:n osan 2 että
SOAK-FIX 3b:n**: lepotila tappoi palvelimen ääniketjun, työkalurivi kertoi
`server not responding` Drawer kiinni, `audioClockRate` oli **nil** siinä missä
sama tilanne edellisenä päivänä palautti jäätyneen 48 003,48:n ja väitti `ok`,
eikä tila muuttunut itsestään; `restart audio` palautti soittimen kokonaan.

**Kriteeri 5 on ainoa avoin.** sclangin työjoukko 275 → 352 MB,
tunti-inkrementit +19,4 / +19,3 / +22,6 MB: lineaarinen, ei tasaantumista, ja
**sama kulmakerroin kuin ensimmäisessä ajossa** — kaapelikerroksen allokaatiot
eivät olleet syy. Lähde eristetään `tests/memory_probe.ps1`:llä, koska "mistä
kasvu tulee" on eri kysymys kuin "tasaantuuko se" eikä vaadi yötä.

Sivutuotteena mitattu: ajon ensimmäiset minuutit menivät vahingossa akulla, ja
sama patch maksoi silloin **23 % CPU:ta 12 %:n sijaan**. Akkukellotus
kaksinkertaistaa kuorman.

**Löydökset B ja C hyväksyttiin deterministisessä muodossaan 2026-07-31**
(checklist SOAK-FIX osat 2 ja 3a): provokaatio yksin sytytti työkalurivin
`server not responding` -merkin Drawer kiinni, guard ei väittänyt `ok`, ja
`restart audio` palautti soittimen kokonaan — tulos (a). Auki on enää **osa
3b**, sama todistus oikeassa lepotilaviassa, joka kulkee SOAK-1:n vaiheen 4
mukana; vain oikea lepotila tappaa palvelimen ääniketjun.

#### Löydösten B ja C koejärjestys

**B ja C eivät kulje soakin sisällä.** Ne vaativat vastaamattoman palvelimen,
eikä sellaista voi provosoida monitorin käydessä, koska monitori pitää koneen
hereillä juuri estääkseen sen. Ne ajetaan SOAK-1:n **vaiheessa 4**, monitorin
pysäytyksen jälkeen — sama teko kuin `SOAK-FIX` osa 3b ja AUD-W osa 2.

Niille on nyt myös **deterministinen esikoe** (`SOAK-FIX` osa 3a), joka ei
vaadi lepotilaa: `stopAliveThread` + `statusWatcher.unresponsive = true` kertoo
kielelle ettei palvelin vastaa, vaikka prosessi elää ja pitää porttia — tasan
se muoto jossa vika mitattiin. Se todistaa elinkaari- ja pintapolun (guardin
`\stale`, työkalurivin merkki, restartin oikean kohteen ja paluun), **muttei
kuollutta ääniketjua**; sen todistaa vain oikea lepotila.

### 1.0-tagin rajaus ja avoin nimipäätös

- **Tekninen 1.0 on eri asia kuin julkaisurata.** 1.0-tagi `main`issa on
  tekninen virstanpylväs; rata C (public repo, media, beta-release) ja rata B
  (omistajuuden itseopiskelu) eivät ole sen ehtoja. Repo voi olla tagatessa
  yhä private.
- **Versionumero ja nimi ovat avoinna eikä niitä lyödä lukkoon vielä.** `v1.0`
  on käytössä (commit `43724f3`, vanha Cocoquantus-tutkielma), joten uusi tagi
  tarvitsee oman tunnuksen. **Käyttäjä ilmoitti 2026-07-30, että nimi Yölahti
  todennäköisesti vielä muuttuu ja identiteettipohdinta on edessä**, joten
  tagia ei nimetä nykyisen tuotenimen mukaan ennen sitä päätöstä. Tämä on sama
  päätös kuin alla olevan "Myöhemmin"-osion identiteetti- ja sisäosien
  nimikohta sekä julkaisuradan nimi-/törmäystarkistus; ne ratkaistaan yhdessä,
  ei kolmena erillisenä.

## Myöhemmin, ei tämän vaiheen scopea
- **Identiteetti ja sisäosien nimet** (käyttäjän havainto 2026-07-28, N8:n
  katselmoinnissa). Nightissä Cocoquantus-sukulinja näkyy *muotona* —
  castlejen anatomiana ja arkkitehtuurina — mutta se on jo selvästi eri soitin
  eikä esikuvaa nimetä eksplisiittisesti. Drawerissa esikuva sen sijaan
  **nimetään suoraan**: `quantussy`, `coco 1`, `coco 2`, `spesal`, `balcium`.
  Workshop-sivu teki tämän ristiriidan näkyväksi, koska se on ensimmäinen
  pinta jossa sisäosien nimet luetaan tekstinä eikä hahmoteta muotona.
  Kaksi ratkaisusuuntaa, joista pitää valita yksi:
  (a) **nimetään koko soitin avoimesti Cocoquantus-johdannaiseksi** — ei
  emulaatioksi vaan uudeksi soittimeksi joka tekee kunniaa jo nimen tasolla,
  jolloin sisäosien nimet ovat rehellistä sukulinjaa; tai
  (b) **nimetään sisäosat uudelleen** (quantussy, coco jne. saavat Yölahden
  omat nimet) ja Cocoquantus jää pelkäksi dokumentoiduksi vaikutteeksi.
  Vaikuttaa `README.md`:hen, `SOURCES.md`:hen, GUI:n kaikkiin otsikoihin ja
  mahdollisesti `CQ*`-nimiavaruuteen, joten se on oma päätöksensä eikä osa
  N8:aa. Liittyy suoraan julkaisuradan "nimi ja törmäystarkistus" -kohtaan.
  **Käyttäjän ilmoitus 2026-07-30: myös tuotenimi Yölahti todennäköisesti vielä
  muuttuu, ja identiteettipohdinta on edessä.** Sen vuoksi 1.0-tagia ei nimetä
  nykyisen tuotenimen mukaan ennen tätä päätöstä (ks. "Tie 1.0:aan"), eikä
  nimeä lyödä lukkoon dokumenteissa sitä ennen.
- **Kosketusnäyttö-optimointi Night-modeen** (multi-touch, ele-erottelu) —
  liittyy kevättulvaan (§8 kasvattaa kohteita jo ~40 px suuntaan); oma
  työvaihe kun mittakaava on lyöty lukkoon.
- ~~**Stemien live-kuuntelu**~~ — **suljettu 2026-07-30.** Aiempien vaiheiden
  viimeinen avoin manuaalitesti (`manual_checklist.md` KUUNTELU 7, kohdat
  7.4/7.5) vaati ≥ nelikanavaisen äänikortin, ja AIO-4:n fyysinen K-Mix-portti
  todensi sen: stem 1/2 kuuluivat mainista erillisistä ulostuloista 3/4,
  Coco1/Coco2-kartoitus oli itsenäinen ja PANIC hiljensi molemmat. Muut
  checklistin rastittamattomat rivit ovat vanhentunutta jäännöstä valmiista
  v1.0/G1-vaiheista (v1.0 tagattu, G1 Drawer sulautui hyväksyttyyn
  G1-loppuporttiin).

## Viimeinen musiikillinen lisävaihe: kolme huokoista soittotilaa

**Status 2026-07-30: hyväksytty jatkosuunta, oma haara, eikä 1.0:n ehto.**
Käyttäjän päätös 2026-07-30: soittimen perustoiminta ei vaadi tätä, joten se
toteutetaan omana haaranaan **1.0-tagin jälkeen** eikä sitä oteta mukaan
tien 1.0:aan (AUD-W → soak → VB-CABLE → merge + tagi). Tämä on silti viimeinen
uusi soitinarkkitehtuurin vaihe, ei syy lykätä nykyisiä viimeistelytöitä.

### Taiteellinen teesi ja rajaus

Puuttuva makro ei ole reverb vaan valinta siitä, **mikä herättää Coco-muistin
ja miten muistia luetaan**. Työnimet ovat:

1. `loop` — nykyinen outo luupperi: live/deck/ristisyöttö kirjoittaa, yksi
   kellotettu ZOH-lukupää lukee;
2. `drone` — soittimen omat castle-trianglet ja Coco-kellojen organ-pinot
   herättävät 8-bittisen muistin ja sen feedback-kierron;
3. `tide` / `field` — sama Coco-muisti luetaan liikkuvana,
   monilukupäisenä muistokenttänä tavallisen luupin tai päälle liimatun
   reverbin sijasta. Lopullinen nimi päätetään vasta kuuntelusta; `ambient`
   kuvaa käyttötapaa, ei vielä algoritmia.

Tilat ovat **huokoisia ekologioita**, eivät kolme toisiaan poissulkevaa
presetiä. Globaali valitsin siirtää painopistettä ja vaihtaa
heräte-/lukutopologiaa lennosta, mutta ulkoiset lähteet, drone-heräte,
suora luuppiluku ja field-luku saavat olla eri määrin yhtä aikaa auki. Näin
drone voidaan kerrostaa olemassa olevaan luuppiin ja uusi aine voi jäädä
Coco-muistiin tilanvaihdon jälkeen. Erillistä efektiräkkiä tai täyttä
patchattavaa audiomatriisia ei lisätä ennen kuin tämä ydin on todistettu.

Mannequins Just Friendsin Technical Map on myöhemmän suunnittelun
vertailukohta, ei kopioitava ominaisuuslista:
<https://github.com/whimsicalraps/Mannequins-Technical-Maps/blob/main/just-friends/just-friends.md>.
Hyödyllinen periaate on kuvata täsmällisesti, miten sama generaattori ja sama
käsi muuttavat tehtäväänsä mode- ja timebase-yhdistelmissä. Yölahdelle tehdään
lopuksi oma technical/mode map, joka kertoo vastaavasti jokaisen nykyisen
säätimen, patch-kohteen ja eleen merkityksen kaikissa kolmessa tilassa.

### Pysyvät soittolait

- Mode-vaihto ei tyhjennä matriisia, lataa presetiä, muuta Spesal-tiloja,
  nollaa phasea eikä kirjoita puskuriin itsestään.
- `Hold` säilyy soittajan kirjoitusporttina. Drone-tila voidaan valita
  turvallisesti, mutta sisäinen heräte tallentuu vasta kun soittaja avaa Cocon.
- Ensimmäisessä versiossa kaikki tilat jakavat nykyiset kaksi Coco-bufferia.
  Tilasta toiseen vuotava muisti on ominaisuus. Kolme erillistä audio-
  muistiparia, automaattinen buffer-copy ja tilakohtainen patch-/nuppimuisti
  rajataan pois, ellei käytännön soittaminen myöhemmin osoita niitä
  välttämättömiksi.
- Nykyisten käsien semantiikka säilyy: `Speed` on kello/sävelkorkeus/aika,
  `Spaf` kellon tai lukukentän liike, `Feedback` pysyvyys/eroosio, `Hold`
  kirjoita–säilytä, `Flip` suunta ja `Skip` vaiheen uudelleenasetus.
  Tilat voivat laajentaa näiden seurausta, mutta sama säädin ei saa muuttua
  mielivaltaisesti toiseksi käsitteeksi.
- Sama 16×22 patchi pysyy voimassa tilan yli. Nykyiset `spaf`, `inVca`,
  `fbVca`, `flip` ja `skip` -summat annetaan myös uuden lukutavan käyttöön
  aina kun yhteys on semanttisesti sama. Uusia matriisikohteita lisätään vasta
  jos kuunteluprototyyppi osoittaa, ettei nykyinen kieli riitä; matriisin
  laajennus muuttaa myös Inspectorin geometriaa, snapshotteja ja testisopimusta.
- Tilanvaihto on palvelimella silotettu crossfade, ei SynthDefin vaihto tai
  nodejen churn. Vanhat kerrokset ja hännät saavat jatkua siirtymän läpi,
  mutta ulostulo kulkee aina nykyisen master-turvaketjun kautta.
- Snapshot voi myöhemmin tallentaa valitun tilan ja sen jatkuvat painot,
  mutta snapshot ei edelleenkään lupaa tallentaa Coco-bufferien ääntä.

### DSP-lähtökohta

`loop` säilyttää nykyisen normatiivisen Coco-ketjun koskemattomana
vertailupolkuna. Uudet polut rakennetaan sen rinnalle staattiseen graafiin ja
ristihäivytetään; nykyisen hold-integriteetin on säilyttävä bitintarkkana.

`drone` käyttää kahta jo osittain olemassa olevaa ääniperhettä:

- Quantussyn audioalueiset castle-trianglet summataan nykyiselle `qMix`-
  väylälle ja syötetään Cocon input-mikseriin kuten `quantumDrone`-reseptissä.
- Cocon osoitekellosta johdettu `org1`/`org2` square-stack tuodaan uutena
  **pre-converter-herätteenä** omaan Cocoon, vastakkaiseen Cocoon tai
  molempiin. Nykyinen `cocoNOrgan` on post-memory-ulostulokerros ja
  modulaatiolähde; tätä ei saa sekoittaa uuteen organ→memory-reittiin.
- Oman kellon organ→oma muisti muodostaa itseensä viittaavan sävelketjun;
  organ→vastakkainen muisti muodostaa kahden kellon intervalli- ja
  ristikierron. Molemmat pysyvät Cocon clip/wrap→quantize-rajoituksen sisällä.
- Ulkoinen `pre`/`deck` ja sisäinen triangle/organ eivät sulje toisiaan pois.
  Prototyypin pitää ratkaista pienin ymmärrettävä heräteseos ilman uutta
  geneeristä audiomatriisia.

`tide/field` jatkaa vain `memoryHalo`-luonnoksen käyttökelpoista ydintä:
kirjoitukseen koskemattomat rinnakkaiset lukupäät samassa muistissa. Kiinteä
neljän offsetin multitap hylätään liian ennustettavana. Tutkittavat suhteet
johdetaan Cocon kellosta, kahden Cocon suhteesta, puskurin vaiheesta ja
nykyisistä patch-summista. Suora alkuperäinen lukupää on aina saatava
vertailuksi; field on vaihtoehtoinen muistiluku, ei Space-sendin päässä oleva
efekti. Ensimmäiset elehypoteesit:

- `Spaf` avaa/supistaa lukupääkonstellaatiota ja voi erottaa päiden nopeuksia;
- `Flip` kääntää kentän suunnan säilyttäen keskinäiset suhteet;
- `Skip` kokoaa päät samaan vaiheeseen tai kylvää ne deterministisesti uudelleen;
- `Feedback` ja Dolby päättävät, miten uusi materiaali korvaa vanhaa;
- Coco-ristisyöttö saa liikuttaa muistia, mutta ei pelkästään levittää
  stereokuvaa kuten hylätty `confluence`.

### Toteutusjärjestys — aloita tästä, kun vaihe viimein avataan

- [ ] **0. Päivitä sopimus ennen koodia.** Lukitse työtermit, mode-vaihdon
  puskuri-/Hold-laki, tilojen sekoittumisen malli ja se, mitkä arvot ovat
  snapshot-muistia. Piirrä signal map sekä tilakohtainen control map.
- [ ] **1. Drone-labra, ei GUI:ta.** Vertaa samalla materiaalilla neljä
  herätettä: `qMix→Coco`, organ→oma Coco, organ→vastakkainen Coco sekä niiden
  rajattu seos. Kuuntele eri Speed-suhteet, Spaf-patchit, feedbackin
  unity-raja, Hold-siirtymät ja organin suora/post-memory-osuus.
- [ ] **2. Valitse drone-identiteetti.** Hyväksymisehto ei ole “kuuluu” vaan
  uusi, vähintään yhden pitkän session kantava soittotapa, jossa nykyiset
  patchit ja Coco-eleet muuttavat ääntä merkityksellisesti.
- [ ] **3. Field-labra, ei GUI:ta.** Tee korkeintaan kolme suhteellista
  lukupäämallia. A/B-kuuntele ne suoraa lukupäätä vasten; hylkää tavalliseksi
  chorukseksi, multitapiksi tai stereolevitykseksi typistyvät mallit.
- [ ] **4. Staattinen tuotanto-DSP.** Lisää vain hyväksytyt rinnakkaispolut,
  silotetut tila-/seospainot ja tarvittavat sisäiset bussit. Älä vaihda
  SynthDefejä soitettaessa. Lisää CQSpec/CQState vasta kun parametrien
  semantiikka on kuuntelulla vakaa.
- [ ] **5. Turva ja regressiot.** NRT jokaiselle puhtaalle tilalle, kaikille
  siirtymille ja seoksille; pitkä feedback/wrap/chaos-worst-case; DC/NaN/
  ceiling; hold-integriteetti; mode-vaihto Holdissa ei muuta bufferia;
  sama matrix ennen/jälkeen; panic, recorder ja stemit kaikissa tiloissa.
- [ ] **6. Ohjauspinnat.** Master-paneelin alaosaan yksi globaali
  kolmiasentoinen tilavalitsin, joka näyttää myös siirtymän/seoksen ilman
  efektiräkin estetiikkaa. Päätä kuuntelulla, tarvitseeko sen rinnalle yksi
  jatkuva huokoisuus-/blend-makro. F14:n uusi tehtävä päätetään vasta tässä;
  fyysisen faderin ei pidä joutua arvaamaan diskreettiä tilaa.
- [ ] **7. Muisti ja lennosta soittaminen.** Snapshot roundtrip/morph,
  MIDI-pickup, vanhojen snapshotien migraatio ja 30 minuutin sessio, jossa
  jokaiseen tilaan siirrytään kesken äänen ja aineistoa kerrostetaan tilojen
  välillä ilman tahatonta resettiä tai napsua.
- [ ] **8. Oma Technical Map.** Dokumentoi lähde→muisti→lukija→master,
  puhtaat tilat, kaikki kahden tilan seokset, säätimien/eleiden merkitys,
  normaaloinnit, bufferin pysyvyys sekä muutama löytämiseen ohjaava patch —
  ei reseptikokoelmaa, joka väittää löytöjä loppuun käsitellyiksi.

### Exit-kriteeri

Kolme ankkuria kuulostavat eri instrumentaalisilta näkökulmilta mutta samalta
Yölahdelta; niiden välillä voi vaihtaa ja niitä voi sekoittaa kesken äänen;
sama patchi ja sama käden kieli säilyvät ymmärrettävinä; kirjoitus tapahtuu
vain soittajan Hold-päätöksellä; yksikään tila ei tarvitse geneeristä reverbiä
ollakseen kokonainen; ja kaikki vanhat turvallisuus-, hold-, snapshot-,
recorder-, GUI- ja suorituskykyportit kantavat.
