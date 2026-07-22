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

**Seuraava askel konkreettisesti:** aloita `NIGHT_STANDALONE_PLAN.md §12`:n
rakennusjärjestyksestä — ensimmäisenä **punch (§2)**, joka on riippumaton
kaikesta muusta ja välittömästi musikaalinen. §13 korvapassi voi kulkea
limittäin (jopa ensin — se on pelkkää kuuntelua). Norns-portti (§14) on vasta
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
  lisätä. Night-only arvioidaan yksinkertaisesti **olemalla painamatta Tabia**
  muutaman session ajan — ei koodia, ei lippua, ei poistoa ennen päätöstä.
- Erillisiä lyhytikäisiä työhaaroja tarvitaan vain todellisessa rinnakkaistyössä.
- Lähdekoodi, testit ja dokumentit pidetään samassa commitissa silloin, kun ne
  yhdessä muodostavat yhden todennettavan käyttäytymismuutoksen.

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

- [ ] **1. Punch — momentaarinen hold** (§2). Oikean napin pito kuoren
  sydämellä invertoi holdin painalluksen ajaksi. Ei geometriaa; kriittistä on
  tyhjentävä exit-polkulista (jumiin jäänyt punch on tuhoisa). Riippumaton
  kaikesta muusta → hyvä aloitus.
- [ ] **2. Kevättulva — kokopassi** (§8). Yön kaikkien kehojen ja
  kosketuskohteiden peruskokojen nosto minimilavalla. Ei uusia muotoja:
  törmäysmatriisi-testit ensin, sitten viritys punaisesta vihreäksi, yksi
  sommittelukatselmointi kahdella ikkunakoolla.
- [ ] **3. Almanakka maisemaksi** (§9). Kuu nousee, pato muuttuu
  vuorovesilammikoksi (kuun heijastukseksi), säärivi sirotellaan rannoille.
  Sama matematiikka, uusi ruumiinkieli. Pidetään tulvasta erillisenä, jottei
  padon muodosta erimielisyys panttaa kokopassia.
- [ ] **4. Range-väreet** (§3). Castlen 6-suunnan piirretty väre, jonka *muoto
  on regime* (infra/balcium/audio); napautus askeltaa. Yön suurin
  soitettavuusaukko. Kasvaneeseen maisemaan, jotta clearance tehdään kerran.
- [ ] **5. Dolby-osterit** (§4). In/fb-kanavien dolby-kytkin sirpin alle
  osterina, jonka asento on regime (off/gate/duck). Napautus askeltaa.
- [ ] **6. Linnut hakevat** (§11). Deckin lataus ja kansionavigointi ilman
  ainuttakaan nuolta: napautus = hakee (tiedostodialogi), rulla = seuraava/
  edellinen, pörhistys = satunnainen.
- [ ] **7. Juuret — mangrove** (§10). Kaksi ohutta viivaa → useampi säie
  yhteen kärkijakkiin; massaa ilman valehtelevia merkkejä. Pelkkää piirtoa;
  voi kulkea kohdan 6 mukana jos katselmoinnit sallivat.
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
Lähtötaso 2026-07-22: `CQ_COMPILE_OK`, **6 611 unit-passia / 0 failurea**,
`CQ_SAFETY_PASSED`, `CQ_HOLD_OK` ja kaikki neljä GUI-markkeria.

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
  ajaa compile-, unit-, NRT safety- ja hold integrity -portit. Ensimmäisen
  etäajon onnistuminen todennetaan vielä kohdassa C.

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

## Myöhemmin, ei tämän vaiheen scopea

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
