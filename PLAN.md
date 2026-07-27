# Yölahti — ajantasainen kehityssuunnitelma (Nocturne-vaihe)

Päivitetty 2026-07-23. Tämä tiedosto kertoo, **mitä tehdään seuraavaksi** ja
**missä järjestyksessä**. Vaiheen yksityiskohtainen *miten* — jokaisen osan
suunnittelu, geometria, karikot, korvakokeet — on
[`docs/NIGHT_STANDALONE_PLAN.md`](docs/NIGHT_STANDALONE_PLAN.md):ssä; tämä
tiedosto on sen status- ja järjestysnäkymä. Aiempien vaiheiden jäädytetyt
työpäiväkirjat: v1.0 [`docs/history/PLAN_v1.0.md`](docs/history/PLAN_v1.0.md),
Archipelago-GUI G1–G3 [`docs/history/PLAN_archipelago.md`](docs/history/PLAN_archipelago.md).

**Tilanne yhdellä rivillä:** v1.0 (DSP + koodikerros) ja koko Archipelago-GUI
(G1 Day · G2 Night · G3 Weather machine) ovat valmiita ja käyttäjän hyväksymiä;
archipelago on sulautettu `main`iin (PR #1, tagi `archipelago-1.0`). **Uusi
vaihe: Nocturne — yö koko soittimena.** Tavoitteena tehdä yökartasta itsenäinen,
täysin soitettava instrumentti (loput laitteiston olennaiset eleet yöhön,
maisema uudelleen sommiteltuna) ja lopulta harkita Day-paneelin poistoa.

**Julkinen identiteetti:** soittimen nimi on nyt **Yölahti**. Cocoquantus on
lähtökohta ja avoimesti dokumentoitu sukulinja, ei tuotteen nimi eikä lupaus
uskollisesta emulaatiosta. `CQ`/`Cocoquantus` säilyvät toistaiseksi teknisenä
API- ja luokkanimiavaruutena; niitä ei massauudelleennimetä kesken Nocturnen.
Portfolio- ja julkaisupinta on `README.md`:ssä, `SOURCES.md`:ssä ja `docs/`-kansion
case study / development process -dokumenteissa. Etärepon uudelleennimeäminen,
public-vaihto ja oikeat kuva-/äänidemot ovat erillinen julkaisutoimi.

**Seuraava askel konkreettisesti:** rakennusjärjestyksen (`NIGHT_STANDALONE_PLAN.md
§12`) kohdat 1 **punch (§2)**, 2 **kevättulva (§8)** ja 3 **almanakka maisemaksi
(§9)** ja 4 **range-väreet (§3)** ovat valmiit ja hyväksytyt (2026-07-23,
2026-07-26 ja 2026-07-27), samoin kohta 5 **Dolby-osterit (§4)** on valmis ja
hyväksytty 2026-07-27. Seuraava inkrementti on kohta 6 **Linnut hakevat (§11)**:
lintua napautetaan tiedostodialogiin, rullataan kansion seuraavaan/edelliseen
tiedostoon ja pörhistetään satunnaiseen tiedostoon ilman nuoli- tai tekstinappeja.
Hyväksytty N5 elää `nocturne-dolby-oysters`-haaralla; `nocturne` seisoo vielä
commitissa `a5e4b17` (kevättulva), eikä haaroja sulauteta ilman erillistä
pyyntöä. §13 korvapassi voi kulkea limittäin, mutta Norns-portti (§14) on vasta
night-first-vaiheen jälkeen.

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
  lisätä. Night-only arvioidaan valitsemalla Night toolbarista ja pysymällä
  siinä muutaman session ajan — ei koodia, ei lippua, ei poistoa ennen päätöstä.
- Erillisiä lyhytikäisiä työhaaroja tarvitaan vain todellisessa rinnakkaistyössä
  **tai kun valmis inkrementti jää käyttäjän harkintaan.** §9 (almanakka
  maisemaksi) hyväksyttiin 2026-07-27 `nocturne-nightscape`-haaralla; haara
  säilyy erillään, kunnes käyttäjä pyytää sulautuksen `nocturne`en. Työn saa
  esiin komennolla `git switch nocturne-nightscape` ja pois komennolla
  `git switch nocturne`. **Testi-commit ei ole hyväksyntä** — hyväksyntä on
  aina checklist-rivin rasti käyttäjältä.
- Lähdekoodi, testit ja dokumentit pidetään samassa commitissa silloin, kun ne
  yhdessä muodostavat yhden todennettavan käyttäytymismuutoksen.
- Nocturnen nykyiset N3→N4→N5-haarat ovat **pinottu ketju**, eivät kolme
  toisistaan riippumatonta toteutusta: `nocturne-nightscape` pohjautuu
  `nocturne`en, `nocturne-range-ripples` sen päälle ja
  `nocturne-dolby-oysters` edelleen N4:n päälle. Oma haara ei ole jokaisen
  N-vaiheen itseisarvo; sitä käytetään nyt hyväksytyn checkpointin säilyttämiseen
  samalla kun seuraava näkyvä inkrementti voidaan arvioida erikseen. Ketju
  sulautetaan `nocturne`-integraatiohaaraan erillisellä pyynnöllä.

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
- [ ] **6. Linnut hakevat** (§11). Deckin lataus ja kansionavigointi ilman
  ainuttakaan nuolta: napautus = hakee (tiedostodialogi), rulla = seuraava/
  edellinen, pörhistys = satunnainen.
- [ ] **7. Juuret — preamp-keho** (§10). Lähdesäikeet → drive/bias-solmu →
  kiinteä audiojuoni Cocoon; `envA/B` jää post-preamp-envelope-sivusilmuksi.
  Ei koristeellista juurimäärää eikä tekaistuja audiojakkeja. Toteutetaan
  vasta vuorovesikentän sommittelupalautteen jälkeen omana porttinaan.
- [ ] **8. Drawer-workshop** (§0). Kaikki loput CQSpec-parametrit rehellisin
  ledger-säätimin drawerin sivulle. Exit-kriteeri: **SPEC §6.2:n kattavuusauditointi
  — jokainen §5-parametri saavutettavissa** pelkällä yöllä + drawerilla +
  masterilla. Oma porttinsa.
- [ ] **9. Day-tuomio** (§0). Vasta kun kohtia 1–8 on *soitettu* oikeissa
  sessioissa. Dayn poisto, jos valitaan, on oma peruutettava porttinsa —
  ja tarvitsee ensin kodin (tai kirjatun kuoleman) Dayn kahdelle
  yleisnäkymälle (spesal-tähdistö, staattinen muistipolkukartta).

## Korvapassi — musikaaliset hienosäädöt (§13)

Riippumaton yllä olevasta GUI-järjestyksestä, voi kulkea limittäin; kukin
kohta on oma kuunteluiteraationsa (hypoteesi → yksi muutos → koko patteri →
3–5 askeleen kuunteluskripti → kuvaus → säätö tai peruutus). **Yksi muuttuja
per sessio.** Suositeltu järjestys hyöty/riski: 13.1 feedback-taper → 13.4
chaos-käyrä → 13.2 speed-lattia → 13.5 VCA-laki → 13.3 dust. Kohta 13.6
zipper-mittaus ajetaan G3.1-tyylisen kompassivedon yhteydessä; 13.7 on punch
(= kohta 1 yllä). Jokainen hyväksytty DSP-muutos ajaa myös `nrt_safety`n ja
`nrt_hold_integrity`n ja päivittää SPEC §5:n samassa commitissa.

- [ ] 13.1 feedback-taper · [ ] 13.2 speed-lattia · [ ] 13.3 dust ·
  [ ] 13.4 chaos-käyrä · [ ] 13.5 VCA-laki · [ ] 13.6 zipper-mittaus

## Norns-portti (§14) — vasta night-first-vaiheen jälkeen

Tavoiteympäristö norns + grid + 16n (+ crow). Puhdas ydin
(`CQSpec/CQState/CQMatrix/CQSynthDefs/CQCompass` + turvaketju) siirtyy
CroneEngineen, Lua korvaa GUI:n ja 16n:n, grid on oma uusi keho omalla
speksillään. Dietti otetaan lahjana: 8-bit-rakeisuus ja epätarkkuus ovat
identiteettiä, kalliit osat ovat kohteliaisuuksia (FDN → nornsin reverb,
recorder → tape, dekit → softcut lähteenä). Askelmerkit N0–N6 ja karikot
`NIGHT_STANDALONE_PLAN.md §14`:ssä. Ei aloiteta ennen kuin night-first on
soitettu valmiiksi.

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

## Myöhemmin, ei tämän vaiheen scopea

- **Johtojen piilotus/himmennys taustalle** (käyttäjän toive 2026-07-26, N2:n
  katselmoinnissa): GUI tarvitsee todennäköisesti jonkinlaisen kytkimen, joka
  vetää kaapelikerroksen taustalle — piiloon tai selvästi himmeämmäksi — jotta
  täyden kentän alta näkee kehot ja merkit. Ei vielä päätetty muoto (kytkin vai
  pidike, koko kerros vai vain ei-fokusoidut, näkyvyys vai osumat) eikä koti.
  Huomaa kaksi rajaa jo nyt: §4.5:n mukaan kaapeli voittaa yleensä osoittimen alla,
  joten pelkkä *näkyvyyden* himmennys ei saa muuttaa sitä kuka painalluksen saa
  — muuten kytkin tekee valehtelevan vihjeen; ja §16.8:n johtokieli (napa,
  paino, solo-dim) on jo hyväksyttyä pintaa. Ratkaisisi myös osan siitä, miksi
  skip-merkin kaltaiset kohteet jäävät kaapelin alle. **Yksi rajattu poikkeus
  päätettiin N3:n hyväksynnässä 2026-07-27:** Tab-kidus saa myöhemmässä
  polish-passissa aina painalluksen ennen kaapelijännettä, ja sen hover-/piirto-
  vihje nostetaan löydettäväksi johtojen alta. Poikkeusta ei yleistetä muihin
  kontrolleihin ilman uutta päätöstä. Palataan toteutukseen erikseen.
- **Snapshotit omaan kansioonsa** (`snapshots/` vs `presets/`) — kirjattu jo
  Archipelago-vaiheessa, tehdään omana pienenä työnään.
- **Kosketusnäyttö-optimointi Night-modeen** (multi-touch, ele-erottelu) —
  liittyy kevättulvaan (§8 kasvattaa kohteita jo ~40 px suuntaan); oma
  työvaihe kun mittakaava on lyöty lukkoon.
- **Space-body ja Space-estetiikka** odottavat erillistä konseptipäätöstä.
- **Stemien live-kuuntelu** — ainoa vielä avoin manuaalitesti aiemmilta
  vaiheilta (`manual_checklist.md` KUUNTELU 7, kohdat 7.4/7.5): vaatii
  ≥ nelikanavaisen äänikortin, joten se on erillinen laitteistogate. NRT-turva
  on jo vihreä; live-todennus odottaa laitteistoa. Muut checklistin
  rastittamattomat rivit ovat vanhentunutta jäännöstä valmiista v1.0/G1-
  vaiheista (v1.0 tagattu, G1 Drawer sulautui hyväksyttyyn G1-loppuporttiin).
