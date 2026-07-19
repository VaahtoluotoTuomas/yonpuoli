# Korjaus- ja kehitysroadmap

Päivitetty: 2026-07-19
Lähde: [`prompts/AUDIT_EVAL.md`](prompts/AUDIT_EVAL.md) ja
[`VERIFIED_FINDINGS.md`](VERIFIED_FINDINGS.md)

GUI-työssä autoritatiivinen käyttäytymis- ja visuaalinen määrittely on
[`../GUI_SPEC.md`](../GUI_SPEC.md). `SPEC.md` voittaa parametri-, DSP- ja
turvallisuusristiriidat, `ARCHITECTURE.md` säilyttää pure-view/funnel/ownership-
rajat. Roadmap ei saa poiketa `GUI_SPEC.md`:stä ilman käyttäjän etukäteishyväksyntää.
`GUI_DEVELOPMENT_PLAN.md`:n neljä aiempaa spesifikaatiosaumaa ratkaistiin käyttäjän
päätöksellä 2026-07-19 ja kirjattiin `GUI_SPEC.md`:ään: 9-kanavainen charge,
G1-harvest-lasso, display-only mic sprout sekä staattiset REC-välitilat/error.

Työjärjestys on tarkoituksellinen. Milestone 0 tekee virheistä toistettavia,
Milestone 1 sulkee ainoan P0:n, Milestone 2 korjaa P1-elinkaaren, ja vasta sen jälkeen
muutetaan ominaisuuksien semantiikkaa, DSP:tä tai GUI:ta.

Kaikissa vaiheissa suojataan nämä invariantit:

- Sources → Quantussy → Cocos → FX → Master -server tree ja staattinen ydinsynth-graafi;
- kaikki parametrit `CQSpec` → `CQState`, kaikki patchit `CQMatrix` ja kaikki toiminnot
  `Cocoquantus`-facaden kautta;
- kaksi itsenäistä Coco-muistia, feedback, aliasointi, digital dust, Flip, Skip, Hold,
  signed matrix ja Quantussyn hallittu epävakaus;
- kiinteä, ei-ohitettava fyysisten lähtöjen turvaketju sekä hiljainen boot;
- headless-, koodi- ja MIDI-käyttö ilman GUI-riippuvuutta;
- Space valinnaisena kerroksena, ei Cocon sisäisen topologian korvikkeena.
- Archipelago-rakenne: yksi ikkuna, Day/Night, pysyvä Master-strip ja Drawer; vanha
  grid säilyy Inspectorina eikä ensisijaisena patchauspintana.

## Milestone 0: turvaverkot ja testit

### 0.1 Hermettinen testikäynnistys

- **Tavoite:** tehdä compile-, unit- ja integraatioajosta koneesta riippumaton ja
  erottaa testien omat extensionit käyttäjän/system-extensioneista.
- **Muutettavat tiedostot:** `tests/sclang_conf_dev.yaml` tai uusi commitoitava
  `tests/sclang_conf_ci.yaml`, testien käynnistysskriptit, `README.md`.
- **Riippuvuudet:** ei koodiriippuvuutta; varmista ensin SuperCollider 3.13:n
  platform-kohtaiset extension-polut.
- **Hyväksymiskriteerit:** luokkakirjasto kääntyy vain dokumentoiduista poluista;
  archive-, synthdef- ja render-väliaikaistiedostot menevät testihakemistoon; skip ja
  failure ovat eri exit/marker-tiloja.
- **Testit:** puhdas compile kahdesti, 3 082 nykyistä unit-assertiota, ajo koneella
  jossa on tarkoituksella ristiriitainen user extension.
- **Regressioriskit:** liian tiukka polkurajaus voi piilottaa projektin luokat tai
  standardikirjaston; Windows/macOS/Linux-polut eroavat.
- **Erillinen commit:** kyllä, testiinfrastruktuurina.

### 0.2 P0/P1-reproducerit ilman kuultavaa riskiä

- **Tavoite:** muuttaa aliasointi-, recorder- ja session-lifecycle-viat pysyviksi
  regressiotesteiksi ennen korjausta.
- **Muutettavat tiedostot:** uudet `tests/classes/CQOutputConfigTest.sc`,
  `CQRecorderLifecycleTest.sc`, `CQSessionLifecycleTest.sc`; tarvittaessa
  `tests/live_lifecycle.scd` ja testipalvelimen apuluokka.
- **Riippuvuudet:** 0.1; hiljainen master/fade ja erillinen testiserver-portti.
- **Hyväksymiskriteerit:** testit osoittavat nykytilassa duplikaatin/päällekkäisyyden,
  recorderin kaksi nodea/orphanin, immediate-stopin ja boot→free-resurrectionin;
  mitään päällekkäistä fyysistä ulostuloa ei ajeta laitteelle.
- **Testit:** config-only alias-matriisi, nelikanavainen NRT-summa, recorderin
  `/n_go`/`/n_end`-seuranta, boot/free/reboot-looppi vähintään 50 kierrosta.
- **Regressioriskit:** ajoitusriippuvainen testi voi olla flaky; käytä serverin
  completion-messageja ja eksplisiittisiä state-eventtejä, ei pelkkiä sleep-aikoja.
- **Erillinen commit:** kyllä test-first-commitina; jos CI ei hyväksy punaista testiä,
  yhdistä vastaavaan korjauscommitiin ennen mergeä.

### 0.3 Reitti-, GUI- ja alustamatriisin sopimustestit

- **Tavoite:** todistaa koko 79 parametrin tila→reitti-sopimus, Archipelagon 16 source /
  22 destination -inventaario sekä kirjata nykyinen sample rate/block size- ja
  GUI-perusta ennen uuden pinnan rakentamista.
- **Muutettavat tiedostot:** uudet `tests/classes/CQRouteContractTest.sc`,
  `CQMatrixTransactionTest.sc`, `CQGuiInteractionTest.sc`; näyttöä edellyttävä
  `tests/gui_smoke.scd`; NRT-ajon parametrisointi.
- **Riippuvuudet:** 0.1; GUI-testin pitää osata skipata eksplisiittisesti ilman Qt-
  näyttöä.
- **Hyväksymiskriteerit:** kaikki 79 nimeä luokitellaan reititetyksi tai tarkoituksella
  vain tallennetuksi; nykyisen Dayn parametrit eivät katoa; kaikki 16 source jackia ja
  22 socketia mappaavat canonical `CQMatrix`-nimiin; full-matrix-read on syvä kopio;
  352 solun batch-write clampaa ja lähettää yhden notificationin; kaikki
  `GUI_SPEC.md` §4.1 audition-revert-polut palauttavat matriisin; 44,1/48/96 kHz sekä
  block size 32/64/128 saavat selkeän pass/unsupported-tuloksen.
- **Testit:** fake/spy-graph, Day/Night/Drawer/Master-inventaario, matrix transaction ja
  operation-boundary mukaan lukien harvestin yksi batch-operaatio, serverless
  interaction state machine, Qt build/close/reopen, NRT safety -matriisi lyhyellä
  renderillä.
- **Regressioriskit:** älä lukitse frontend-luokkajakoa tai widget-lukumäärää testiin;
  lukitse canonical nimet, funnelit, ilmoitusmäärä ja käyttäytymissopimus.
- **Erillinen commit:** kyllä.

## Milestone 1: P0-korjaukset

### 1.1 Fyysisten output-indeksien globaali validointi

- **Tavoite:** estää kaikki post-limiter-summaukset fyysisillä lähtöbusseilla.
- **Muutettavat tiedostot:** `classes/CQConfig.sc`, `config.example.scd`, `SPEC.md`,
  `README.md`, 0.2:n config/NRT-testit.
- **Riippuvuudet:** 0.2; päätä eksplisiittisesti, sallitaanko mono-main muodossa
  yksi kanava vai pidetäänkö kaksi eri kanavaa pakollisena.
- **Hyväksymiskriteerit:** `outChans` on sisäisesti yksikäsitteinen,
  `stemOuts` on sisäisesti yksikäsitteinen ja joukot ovat erilliset; virhe sisältää
  törmäävät indeksit ja syntyy ennen serverin käynnistystä. Nykyinen turvallinen
  `[0,1]` + `[2,3]` toimii.
- **Testit:** duplikaatit kummassakin taulukossa, kaikki main/stem-risteymät, nil-
  stemit, suuret lailliset indeksit, offline-alias-testi, täysi compile/unit/NRT.
- **Regressioriskit:** aiemmin hyväksytyt vaaralliset konfiguraatiot lakkaavat
  boottaamasta; tarjoa selkeä migraatioviesti, älä yritä korjata indeksejä hiljaa.
- **Erillinen commit:** kyllä; tämä on oma P0-korjaus ja release-portti.

## Milestone 2: P1-korjaukset

### 2.1 Session eksplisiittiseksi tilakoneeksi

- **Tavoite:** serialisoida boot/free/reboot ja antaa kaikelle asynkroniselle työlle
  session generation sekä kattava rollback.
- **Muutettavat tiedostot:** `classes/Cocoquantus.sc`, `classes/CQGraph.sc`,
  `classes/CQGui.sc`, mahdollinen uusi pieni `classes/CQSession.sc`, lifecycle-testit.
- **Riippuvuudet:** 0.2; määrittele tilat vähintään `stopped`, `booting`, `running`,
  `stopping`, `failed`.
- **Hyväksymiskriteerit:** stale callback ei voi rakentaa tai rekisteröidä resursseja;
  päällekkäinen komento joko jonoutuu tai palauttaa dokumentoidun tilan; boot-virhe
  vapauttaa osittaiset bussit/bufferit/OSCdefit/controllerit; free on odotettava ja
  idempotentti; ServerQuit/crash invalidioi facaden.
- **Testit:** 50–100 boot/free/reboot-kierrosta eri vaiheissa, synteettiset build-
  virheet jokaisen boot-vaiheen jälkeen, ulkoinen server quit, CmdPeriod, resurssi-
  ja node-count ennen/jälkeen.
- **Regressioriskit:** liiallinen lukitus voi tehdä live-käytöstä tahmean tai jättää
  panicin odottamaan. Panic/CmdPeriod pysyvät välittöminä; generation hylkää vain
  stale-työn.
- **Erillinen commit:** kyllä, mutta pidä mahdollinen luokkaerottelu samassa
  käyttäytymiscommitissa tai sen jälkeen.

### 2.2 Todellinen server reboot ja omistuspolitiikka

- **Tavoite:** erottaa graph restart ja ServerOptions-muutoksia soveltava scsynth-
  reboot nimeltä ja käyttäytymiseltä.
- **Muutettavat tiedostot:** `classes/Cocoquantus.sc`, `live.scd`, `boot.scd`,
  `README.md`, `SPEC.md`, `ARCHITECTURE.md`, lifecycle-testit.
- **Riippuvuudet:** 2.1; päätä, saako instrumentti rebootata jaetun
  `Server.default`-instanssin vai vaatiiko se eksplisiittisen luvan/oman serverin.
- **Hyväksymiskriteerit:** graph-only-operaatio ei lupaa soveltaa server-optioita;
  server-reboot todella käynnistää scsynthin uudelleen ja uusi sample rate, block
  size, device ja I/O-count varmennetaan bootin jälkeen; epäonnistuminen päätyy
  `failed/stopped`-tilaan.
- **Testit:** server PID/status ennen/jälkeen, sample rate/block size -kysely,
  invalid device -rollback, jaetun serverin suojakysymys/policy-testi.
- **Regressioriskit:** jaetun serverin reboot voi katkaista käyttäjän muut synthit;
  tee tämä eksplisiittiseksi API-sopimukseksi.
- **Erillinen commit:** kyllä 2.1:n päälle.

### 2.3 Recorderin tilakone ja paikallinen resurssiomistus

- **Tavoite:** tehdä jokaisesta recording-attemptista yksikäsitteinen ja
  keskeytettävä.
- **Muutettavat tiedostot:** `classes/CQRecorder.sc`, `classes/Cocoquantus.sc`,
  `classes/CQGui.sc`, `classes/CQGuiPanels.sc`, recorder-testit.
- **Riippuvuudet:** 2.1:n generation/teardown-sopimus.
- **Hyväksymiskriteerit:** tilat `idle/starting/recording/stopping/error`; path,
  buffer ja synth ovat attempt-kohtaisia paikallisia resursseja; double-start ei luo
  toista nodea/tiedostokirjoittajaa; stop/free kesken startingin peruu tai viimeistelee
  hallitusti; tiedosto suljetaan täsmälleen kerran.
- **Testit:** double-start, immediate-stop, free/reboot jokaisen async-vaiheen aikana,
  stereo ja 4-ch stem, write/open-failure, `/n_go`/`/n_end`, WAV-headerin luettavuus.
- **Regressioriskit:** liian aikainen bufferin vapautus voi katkaista DiskOutin;
  odota node end ja buffer close ennen omistuksen purkua.
- **Erillinen commit:** kyllä.

## Milestone 3: ominaisuuksien täydentäminen

### 3.1 Deck-load transaktiona ja oikea linked stereo

- **Tavoite:** tehdä latauksesta peruutettava last-request-wins-operaatio ja säilyttää
  linked pairin stereokuva.
- **Muutettavat tiedostot:** `classes/CQDeck.sc`, `classes/Cocoquantus.sc`,
  `classes/CQSynthDefs.sc`, deck- ja NRT-stereotestit.
- **Riippuvuudet:** 2.1:n session generation; 0.3:n reittisopimus.
- **Hyväksymiskriteerit:** stale load vapauttaa oman bufferinsa eikä julkaise pathia;
  free ei voi resurrectoida deckiä; linked L/R vaihtuu yhtenä transaktiona ja käynnistyy
  yhteisestä server-timetagista/barrierista; dry L pysyy vasemmalla ja dry R oikealla.
- **Testit:** large→small ja small→large completion-järjestykset, free/reboot kesken
  loadin, virheellinen tiedosto, mono fallback, impulssi vain L/R-kanavalla ja
  sample-offsetin mittaus.
- **Regressioriskit:** Cocon nykyiset mono deck A/B -syötöt eivät saa vaihtua tai
  linkittyä vahingossa; muuta vain linked dry -semantiikkaa.
- **Erillinen commit:** kyllä; cancellation ja stereodry voidaan tehdä kahtena
  peräkkäisenä commitina, jos yhteiset testit säilyvät vihreinä.

### 3.2 Preset vs patch-resepti -sopimus

- **Tavoite:** ratkaista järjestysriippuvaisen `patchPreset`-toiminnon semanttinen
  sopimus muuttamatta Archipelagon ennalta määriteltyä rakennetta omin päin.
- **Muutettavat tiedostot:** `classes/Cocoquantus.sc`, `classes/CQMatrix.sc`,
  preset-data, `SPEC.md`, `GUI_SPEC.md` vain käyttäjän hyväksymällä muutoksella,
  dokumentaatio ja preset-sekvenssitestit.
- **Riippuvuudet:** kirjallinen tuote-/soittopäätös; 0.3:n parametri-inventaario;
  ennen päätöstä GUI_SPECin termi ja paikka ovat `patch-preset menu` Night-toolbarilla
  ja Inspector-headerissa.
- **Hyväksymiskriteerit:** joko (A) sound preset määrittelee dokumentoidun baseline-
  parametrijoukon ja on järjestyksestä riippumaton tai (B) nykyinen overlay-sopimus
  dokumentoidaan. Jos päätös vaatii uusia GUI-sanoja, Modified-tilaa tai reset-
  toimintoja, `GUI_SPEC.md` päivitetään ensin käyttäjän hyväksynnällä. Mic, tiedostot,
  mute ja laitekonfiguraatio eivät nollaudu hiljaa.
- **Testit:** kaikki presetit kaikista edeltävistä preseteistä, `empty`, snapshot-
  yhteispeli, user overlay sekä GUI-presetin yksi matrix undo/spellbook-operaatio.
- **Regressioriskit:** determinisointi voi poistaa esiintyjien käyttämän kerrostamisen;
  älä muuta nykyistä semantiikkaa ilman migraatiota ja nimettyä vaihtoehtoa.
- **Erillinen commit:** kyllä päätöksen jälkeen.

### 3.3 Archipelagon näkymien tarvitsemat runtime-tilat

- **Tavoite:** tarjota Daylle, Nightille, Masterille ja Drawerille toteutunut runtime-
  tila ilman GUI:n rinnakkaista totuutta tai GUI_SPECin ulkopuolisia uusia pintoja.
- **Muutettavat tiedostot:** `classes/Cocoquantus.sc`, `classes/CQGui.sc`,
  `classes/CQGuiPanels.sc`, recorder/deck/snapshot-luokat, mahdollinen read-only
  presentation model ja GUI-testit.
- **Riippuvuudet:** 2.3 recorderille, 3.1 deckille; `autoTrim`-tila facadesta luettavaksi.
- **Hyväksymiskriteerit:** persistent Masterin REC/elapsed, PANIC/mute, GR ja notice;
  REC idle/starting/recording/stopping/error ja seuraavan REC-painalluksen error-
  kuittaus; Day/Night deck filename/drop; Drawer Memory recall/morph sekä Dayn mic-
  varoitus ja Nightin display-only mic sprout saavat tilansa omistavilta runtime-
  olioilta. Näkyvä muoto noudattaa vain `GUI_SPEC.md` §2, §5, §8, §12, §16.6 ja
  §16.8:n tiloja; puuttuva uusi tila lisätään ensin speciin.
- **Testit:** presentation-mallin unit-testit kaikille tilasiirtymille ja Qt-smoke
  ulkoisilla `CQ.set`/MIDI-tyylisillä muutoksilla, close/reopenilla sekä
  Day/Night/Drawer-vaihdoilla.
- **Regressioriskit:** UI ei saa muodostua toiseksi tilan lähteeksi; se vain näyttää
  facaden/state-objektien tapahtumia.
- **Erillinen commit:** kyllä, mieluiten tila-perheittäin.

## Milestone 4: arkkitehtuurin selkeyttäminen

### 4.1 Asynkronisten resurssien omistustaulukko ja yhtenäinen teardown

- **Tavoite:** antaa jokaiselle bufferille, bussille, synthille, OSCdefille,
  controllerille, taskille/routinelle, clock-eventille ja GUI-deferille yksi omistaja
  ja yksi generation-raja.
- **Muutettavat tiedostot:** `ARCHITECTURE.md`, `classes/Cocoquantus.sc`,
  `classes/CQGraph.sc`, `CQDeck.sc`, `CQRecorder.sc`, `CQGui.sc`.
- **Riippuvuudet:** Milestone 2–3:n toteutunut käyttäytyminen.
- **Hyväksymiskriteerit:** dokumentoitu omistustaulukko vastaa koodia; teardown
  palauttaa node/buffer/bus/OSCdef/task-countit baselineen kaikissa tiloissa;
  callback tarkistaa omistavan generationin ennen publishia.
- **Testit:** resurssisoak 100 boot/freeä, lataus/REC/morph/GUI aktiivisena; server
  query tree ja language-registrien diff.
- **Regressioriskit:** kaksinkertainen free tai omistuksen liian aikainen siirto;
  idempotenssi testataan jokaiselle resurssityypille.
- **Erillinen commit:** dokumentaatio erikseen; mekaaninen refaktorointi vasta vihreiden
  lifecycle-testien päällä omana committinaan.

### 4.2 Vastuurajojen kevyt erottelu

- **Tavoite:** pienentää `Cocoquantus`- ja GUI-tiedostojen vastuumäärää muuttamatta API:a
  tai DSP-graafia.
- **Muutettavat tiedostot:** mahdolliset `classes/CQSession.sc`,
  `CQPresentation.sc`/`CQGuiModel.sc`; nykyiset facade/GUI-tiedostot ja
  `ARCHITECTURE.md`.
- **Riippuvuudet:** 4.1 ja kattavat contract-testit.
- **Hyväksymiskriteerit:** julkinen `CQ`/`Cocoquantus`-API, state/matrix-funnelit ja
  suoritusnäppäimet säilyvät; uutena syntyy selvä session- ja presentation-vastuu,
  ei geneeristä frameworkia.
- **Testit:** koko unit/NRT/lifecycle/GUI-sarja ilman muutoksia odotettuun
  käyttäytymiseen.
- **Regressioriskit:** laaja refaktorointi voi peittää toiminnalliset muutokset;
  pidä commitit mekaanisina ja pieninä.
- **Erillinen commit:** kyllä, yksi rajattu vastuu kerrallaan.

### 4.3 `CQMatrix` transaction- ja notification-sopimus

- **Tavoite:** tarjota Archipelagon auditionille, undolle, Compassille, Breedille ja
  Spellbookille yksi clampattu ja tehokas matrix-funnel laajentamatta GUI:ta toiseksi
  tilan lähteeksi.
- **Muutettavat tiedostot:** `classes/CQMatrix.sc`, `classes/Cocoquantus.sc`
  tarvittaessa operaation origin/commit-rajaan, uudet matrix transaction -unit-testit,
  `ARCHITECTURE.md`.
- **Riippuvuudet:** 0.3:n testisopimus; 4.1:n ownership; nykyinen single-cell `patch`
  ja snapshot triple -formaatti säilytetään.
- **Hyväksymiskriteerit:** syvä full-matrix-read; 352 solun clampattu batch-write
  yhdellä push-kierroksella ja yhdellä notificationilla; committed vs transient-
  ilmoitukset mahdollistavat Spellbookin ilman gesture-layer-hookkia; operation-
  boundary vastaa `GUI_SPEC.md` §4.3–4.4:ää; harvest on yksi batch-operaatio ja code
  writes eivät mene GUI-local-undoon.
- **Testit:** invalid/NaN/inf/over-range kaikkiin soluihin, deep-copy-mutaatio,
  notification count, serverless ja fake-bus push, audition exclusion, GUI/code/
  preset/shake/compass/breed-operaatiot.
- **Regressioriskit:** ilmoituksen muodon muutos voi rikkoa nykyisen `CQGuiMatrix`-
  repaintin tai snapshotit; säilytä backwards-compatible per-cell-polku ja vaihda
  dependants testien alla.
- **Erillinen commit:** kyllä; API + unit-testit ennen ensimmäistä uutta piirtonäkymää.

## Milestone 5: DSP:n ja Coco-uskollisuuden parantaminen

### 5.1 Bad-value-palautuminen silmukoiden rajoilla

- **Tavoite:** estää Space- ja Quantussy-tilan pysyvä saastuminen muuttamatta niiden
  finite-feedbackia, spektriä tai emergenttiä käyttäytymistä.
- **Muutettavat tiedostot:** `classes/CQSynthDefs.sc`, NRT bad-value- ja A/B-testit,
  diagnostiikan facade/GUI vain jos tapahtuma julkaistaan.
- **Riippuvuudet:** nykyisen soundin referenssirenderit ja 0.3:n SR/BS-matriisi.
- **Hyväksymiskriteerit:** NaN/inf ei kirjoitu `LocalOut`/pitkäikäiseen tilaan;
  transientin jälkeen alijärjestelmä palautuu finite-signaaliin; master pysyy
  fail-closed; normaalien finite-syötteiden renderit ovat määritellyn toleranssin
  sisällä ja feedback-decay säilyy.
- **Testit:** injektoitu 1 sample/1 block/47 sample NaN, ±inf, Space/Quantussy/Coco,
  44,1/48/96 kHz, block 32/64/128, null-/spektri-/decay-A/B ja kuuntelu.
- **Regressioriskit:** liian aggressiivinen `Sanitize`/clamp voi steriloida kaaosta tai
  lisätä särön. Suodata vain ei-finite-arvot silmukan kirjoitusrajalla ja käytä
  rate-limited-diagnostiikkaa.
- **Erillinen commit:** kyllä, Space ja Quantussy erikseen jos testifixture on yhteinen.

### 5.2 Fidelity-väitteiden lähdeketju ja kalibrointipäätökset

- **Tavoite:** erottaa toteutettu Coco-henkinen tulkinta historiallisesta
  laitevastaavuudesta ja päättää vasta kuuntelulla offset-lag/kvantisointiasiat.
- **Muutettavat tiedostot:** uusi `docs/FIDELITY_SOURCES.md` tai vastaava,
  `SPEC.md`, `ARCHITECTURE.md`, `audit/COCOQUANTUS_FIDELITY.md`; DSP vain erillisen
  hyväksytyn päätöksen jälkeen.
- **Riippuvuudet:** ensisijaiset lähteet, laite-/referenssinäytteet ja kuuntelupaneeli.
- **Hyväksymiskriteerit:** jokainen historiallinen väite on lähteistetty tai merkitty
  paikalliseksi tulkinnaksi; `2^bits+1` ja castle-offset-lag saavat kirjallisen
  keep/change-päätöksen; mitään ei muuteta pelkän matemaattisen siisteyden vuoksi.
- **Testit:** A/B-renderit, sokkokuuntelu, pitch/decay/alias-mittaus ja olemassa oleva
  safety/hold-sarja.
- **Regressioriskit:** “korjaus” voi tehdä instrumentista geneerisen delayn; soundi ja
  soitettavuus ovat hyväksymisen ensisijaiset mittarit turvallisuuden jälkeen.
- **Erillinen commit:** lähdedokumentti kyllä; jokainen hyväksytty DSP-muutos erikseen.

### 5.3 Space-konseptipäätös

- **Tavoite:** päättää säilytetäänkö metallinen 4-line FDN, viritetäänkö sitä vai
  tarjotaanko toinen valinnainen studio-space, sekoittamatta sitä Cocon identiteettiin.
- **Muutettavat tiedostot:** päätöksestä riippuen `classes/CQSynthDefs.sc`,
  `CQSpec.sc`, Dayn nykyinen minimaalinen Space-ohjaus, `SPEC.md`, `GUI_SPEC.md` vain
  käyttäjän hyväksymällä spec-muutoksella, `PLAN.md` ja kuuntelutestit.
- **Riippuvuudet:** 5.1, lähteistetty konseptikeskustelu ja useita A/B-prototyyppejä.
- **Hyväksymiskriteerit:** Space pysyy oletuksena pois ja send→FDN→return-topologia
  eksplisiittisenä; Cocon feedbackia ei korvata reverb-taililla; size/damp-nimien
  semantiikka vastaa algoritmia; valinta hyväksytään kuuntelulla. Archipelagon Night-
  näkymään ei lisätä Space-bodya ennen erillistä `GUI_SPEC.md`-päätöstä.
- **Testit:** decay/spektri/stereo-korrelaatio, safety ääriarvoilla, A/B eri Coco-
  preseteillä ja performanssikuormitus.
- **Regressioriskit:** liian sileä reverb peittää Cocon rakeisuuden ja tekee paketista
  tavallisen delay/reverb-pluginin.
- **Erillinen commit:** päätösdokumentti kyllä; algoritmimuutos omana committinaan.

## Milestone 6: GUI:n rakenteellinen parantaminen

### 6.1 Archipelago-foundation ja peruutettava ikkunakuori

- **Tavoite:** rakentaa yksi turvallinen GUI-runko muodossa Day/Night + persistent
  Master + click-only Drawer sekä normatiivinen Ink kit ennen gestural patchingia.
- **Muutettavat tiedostot:** `classes/CQGui.sc`, `CQGuiWidgets.sc`, mahdolliset pienet
  GUI-only interaction/drawing-luokat toteuttajan harkinnan mukaan,
  `classes/Cocoquantus.sc`, GUI-lifecycle- ja visual-contract-testit.
- **Riippuvuudet:** 2.1 session generation, 2.3 recorder ownership, 3.3 runtime-state,
  4.3 matrix transaction; `GUI_SPEC.md` §2, §11 ja §16.
- **Hyväksymiskriteerit:** `building/open/closing/closed`; queued build peruuntuu;
  headless ei luo Qt:ta; Tab on hard cut ilman text-focusa; Master ei peity; Drawer ei
  saa keybindingia; minimi 1280×720; yksi keskitetty palette/Ink kit, seeded pixel-
  stable wobble ja vain Pen/vector-primitivet.
- **Testit:** rapid `CQ.gui`, close-before-build, free/reboot, Day/Night/Drawer-state,
  controller/OSCdef/task-count, headless, palette/contrast/glyph/screenshot-smoke.
- **Regressioriskit:** AppClock/Qt-race sekä liian aikainen visuaalinen abstrahointi;
  toteutuksen luokkajako on vapaa mutta käyttäytyminen ja Ink kit eivät.
- **Erillinen commit:** kyllä, lifecycle ja shell/Ink kit voidaan hyväksyä erikseen.

### 6.2 G1 — Day skin, banana grammar ja Drawer

- **Tavoite:** tehdä Daystä kuunneltava patchauspinta ja siirtää ledger-työ Draweriin
  täsmälleen `GUI_SPEC.md` §3–5, §8 ja §13 G1:n mukaan.
- **Muutettavat tiedostot:** `classes/CQGui.sc`, `CQGuiPanels.sc`, `CQGuiWidgets.sc`,
  `CQGuiMatrix.sc`, `CQMatrix.sc`, `CQSynthDefs.sc` + graph-args `/cq_aura`:lle sekä
  uudet GUI/matrix/telemetry-testit.
- **Riippuvuudet:** 6.1 ja 4.3; F0-audition-testit; nykyiset 16 source / 22 destination
  canonical nimet.
- **Hyväksymiskriteerit:** skin sockets oikeissa kodeissa; overlay hit-testaa vain
  ≥12 px jack/socket/cable-targetit ja harvestille vain tyhjän pohjan; koko §4 grammar,
  WYHIWYG, exhaustive audition revert, §4.4:n harvest-preview/batch/no-op/Esc ja 32-op
  GUI undo; Inspector touch-and-listen; Spellbook kaikkien kerrosten committed matrix-
  operaatioille; Memory relocation; `/cq_aura` 16 magnitudia noin 15 Hz; Dayn nykyinen
  muscle memory ja keyboard map säilyvät.
- **Testit:** kaikki §4.1 exit-pathit, operation boundaries, wheel/flip/move/rip/
  stacked-plug, harvestin <8 px/no-op/closing-chord/~12 px sampling/endpoint/catch/
  preview/Esc/yksi undo+spellbook, overlay-vs-widget/drop, spellbook inclusion/
  exclusion, aura order/rate, GUI CPU noin ≤3 %, G1 listening gate.
- **Regressioriskit:** audition voi jäädä matriisiin, overlay voi syödä knob-eleen tai
  mouse-rate write zipperöidä. Älä lisää server-lagia ilman käyttäjän hyväksyntää;
  mittaa ja käytä ensisijaisesti client-write thinning/glideä.
- **Erillinen commit:** kyllä osina, mutta G1 merkitään valmiiksi vain yhtenä
  hyväksyttynä käyttäytymisporttina.

### 6.3 G2 — Night map: Archipelago

- **Tavoite:** toteuttaa topology-true, patch-only Night-landscape ilman nuppeja,
  numeroita, Space-bodya tai dekoratiivista liikettä.
- **Muutettavat tiedostot:** Ink kit/Night-view-luokat toteuttajan valinnan mukaan,
  `classes/CQGui.sc`, `CQGuiPanels.sc`, `CQSynthDefs.sc` loop phase- ja `/cq_charge`-
  telemetrialle, graph-args ja GUI/NRT/telemetry-testit.
- **Riippuvuudet:** hyväksytty G1; §16.6 anatomy; nykyinen `/cq_spesal`, deck-drop ja
  source-aura.
- **Hyväksymiskriteerit:** viisi castlea oikeassa `k→k+1`-ringissä; Cocos left/right,
  roots below, birds above; ei-patchattava kolmitilainen mic sprout; spiral phase
  spark, flip/hold state; 9 charge-lamppua post-weight/pre-Schmitt kanonisessa
  destination-järjestyksessä; solo-light; deck bird drop; exact toolbar; Master pysyy;
  kaikki liike on telemetrian välittämää ja ≤15 Hz.
- **Testit:** source/dest home inventory, topology order, no-number/no-Space-body-
  visual contract, phase/charge payload, gate/trig lamp states, solo/Esc, deck child-
  drop, redraw/CPU ja G2 listening gate.
- **Regressioriskit:** visuaalinen topologia voi valehdella DSP-ringistä tai telemetria
  kasvattaa CPU/OSC-kuormaa; koordinaatteja saa säätää vain §15:n vapauksissa.
- **Erillinen commit:** kyllä osina, mutta Night-portti vaatii käyttäjän visuaalisen ja
  kuunteluhyväksynnän.

## Milestone 7: visuaalinen ja esteettinen viimeistely

### 7.1 G3 — Compass, grow, wither ja breed

- **Tavoite:** toteuttaa Archipelagon weights-only Weather machine, joka muuttaa koko
  patch-ekologiaa yhdellä hiirieleellä mutta ei koske CQSpec-parametreihin.
- **Muutettavat tiedostot:** Night toolbar/weather-view toteuttajan valinnan mukaan,
  `classes/CQMatrix.sc`, uusi compass persistence -apu tarvittaessa,
  `presets/compass.cqcompass.scd` ajonaikaisena käyttäjätiedostona sekä weather-
  transaction/persistence-testit.
- **Riippuvuudet:** G2 ja 4.3; `GUI_SPEC.md` §7, §10 ja §13 G3.
- **Hyväksymiskriteerit:** neljän full-matrix-cornerin bilinear left commit ja
  three-frame right elastic; ≤60 Hz batch writes; `patchDepth` koskematon; versioned,
  validated, clamped autosave/load; grow 2.5 Hz ±U(0.15,0.4); wither ×0.7/floor 0.05;
  breed coin-pick + p=0.08/≤0.15 mutaatio; oikeat yhden undo-operaation rajat.
- **Testit:** corner/edge/center blend, exact snapback, malformed/version/clamp,
  deterministic-rand distribution, hold-op-raja, spellbook ja G3 listening gate.
- **Regressioriskit:** 352-cell frame floods notifications/serverin tai persistence
  suorittaa epäluotettua sisältöä; käytä yhtä batch-notificationia ja validoitua pure-
  data-expression-sopimusta.
- **Erillinen commit:** Compass+persistence ja grow/wither/breed erikseen; G3-portti
  yhdessä käyttäjän hyväksynnällä.

### 7.2 Ink & Phosphor -konformanssi

- **Tavoite:** varmistaa, että kaikki G1–G3:n osat muodostavat yhden normatiivisen
  field-notebook-by-day / bioluminescent-bay-by-night -lajin ilman jälkikäteen päälle
  liimattua teemaa.
- **Muutettavat tiedostot:** keskitetty Ink kit/paletti, kaikki GUI-piirtokohdat ja
  visual-contract-/screenshot-testit; ei kuvatiedostoja tai ulkoisia assetteja.
- **Riippuvuudet:** G1–G3; ulkoasu on tarkistettu jo jokaisessa portissa, tämä on
  kokonaisuuden konformanssia eikä vapaa redesign.
- **Hyväksymiskriteerit:** §16.2–16.12 kokonaan: paletti, kontrasti, line hierarchy,
  seeded wobble/asymmetry, fonts, Pen-glyphs, anatomy, widgets, state table, motion,
  1280×720 scale, yksi Drawer-shadow ja koko forbidden cheapness -lista; squint test
  näyttää patchin/syvyyden/hot/hold/panic/rec.
- **Testit:** deterministic redraw pixel/geometry check, state-screenshot-matriisi,
  contrast/color-with-secondary-channel, motion whitelist/rates, resize/DPI referenssi-
  läppärillä ja käyttäjän visuaalinen review.
- **Regressioriskit:** koristeellisuus voi syödä informaation tai tehdä halvaksi
  digitaalisen hardware-kuvan; jokainen glow ja motion tarvitsee todellisen signaalin.
- **Erillinen commit:** kyllä, pieninä konformanssikorjauksina ilman DSP/state-muutosta.

## Milestone 8: regressio- ja kuuntelutestaus

### 8.1 Täysi automaattinen release-matriisi

- **Tavoite:** ajaa kaikki turvallisuus-, contract-, lifecycle-, GUI- ja DSP-testit
  yhdellä dokumentoidulla release-prosessilla.
- **Muutettavat tiedostot:** test runner/CI, `README.md`, release-checklist.
- **Riippuvuudet:** Milestone 0–7.
- **Hyväksymiskriteerit:** compile; kaikki unit-testit; NRT safety/hold/matrix/Space/
  stems/bad-values; SR/BS-matriisi; 100-kierroksen lifecycle-soak; recorder/deck;
  route contract; matrix transaction/audition; Day/Night/Drawer/Master-lifecycle;
  16/22 inventory; aura/phase/charge; weather/persistence; Qt-smoke, visual-contract,
  CPU/redraw ja resurssivuodot läpäisevät. Jokaisesta tulee yksikäsitteinen marker,
  exit code ja artefakti.
- **Testit:** edellä lueteltu koko sarja vähintään kahdella tuetulla alustalla.
- **Regressioriskit:** pitkä suite voi jäädä ajamatta; erota nopea PR-gate ja täysi
  nightly/release-gate, mutta älä ohita P0/NRT-safetyä PR:ssä.
- **Erillinen commit:** kyllä.

### 8.2 Laitteisto-, kuuntelu- ja performanssihyväksyntä

- **Tavoite:** täydentää se evidenssi, jota NRT tai lähdekooditarkastus ei voi antaa.
- **Muutettavat tiedostot:** `audit/LISTENING_TEST_PROTOCOL.md`, manuaalinen
  hyväksyntäloki, mahdolliset kalibroidut referenssirenderit; ei koodia ilman uutta
  löydöstä.
- **Riippuvuudet:** 8.1 vihreänä; ≥4 ulostulon äänikortti, 16n ja turvallinen
  monitorointiketju.
- **Hyväksymiskriteerit:** erilliset main/stem-ulostulot ja 4-ch tallenne;
  L/R-linked stereo; 60 min soak; kaikki LT-00…LT-12-skenaariot; Space/Coco/Quantussy-
  A/B; panic, CmdPeriod ja recovery; CPU/node/buffer-kasvua ei; `GUI_SPEC.md` §13:n
  G1/G2/G3-kuunteluportit sekä 30–60 min Archipelago-session patchaus **PC:n hiirellä
  ja QWERTYllä ilman kontrolleria** läpäisevät.
- **Testit:** kalibroitu matala lähtötaso, kaiuttimet vasta kun config-validation on
  vihreä; päällekkäisiä fyysisiä kanavia ei koskaan käytetä testinä.
- **Regressioriskit:** subjektiivista kuuntelua ei saa sekoittaa turvatodisteeseen;
  kirjaa laite, asetukset, build ja kuulohavainnot erikseen.
- **Erillinen commit:** kyllä, evidenssi-/hyväksyntäcommittina.

### 8.3 Kanoninen release-status ja jäljitettävyys

- **Tavoite:** ratkaista README/SPEC/ARCHITECTURE/PLAN-ristiriidat ja julkaista yksi
  todennettava status.
- **Muutettavat tiedostot:** `README.md`, `SPEC.md`, `ARCHITECTURE.md`, `PLAN.md`,
  `CLAUDE.md`, release notes ja audit-raporttien statusviitteet.
- **Riippuvuudet:** 8.1 ja 8.2:n todelliset tulokset.
- **Hyväksymiskriteerit:** v1.x-status erottaa automatisoidun, manuaalisen,
  laitteistolla varmennetun ja lykätyn työn; stemOuts-, MIDI-rescan-, reboot-, preset-
  ja Space-semanttiikka ovat yhtenäiset; turvallisuuslupaus vastaa P0-testattua
  toteutusta; fidelity-väitteillä on lähteet tai tulkintamerkintä; `SPEC.md` osoittaa
  uuteen GUI:hin `GUI_SPEC.md`:n kautta ja G1/G2/G3-status perustuu porttien todelliseen
  evidenssiin.
- **Testit:** dokumenttilinkki-/esimerkkitarkistus ja release-checklistin riippumaton
  läpikäynti.
- **Regressioriskit:** status ei saa luvata tulevaa työtä valmiiksi eikä mitätöidä
  historiallista hyväksyntää; päivämäärää ja evidenssityyppiä käytetään.
- **Erillinen commit:** kyllä, release-candidate-dokumentaatiocommittina.

## Julkaisuportit

1. **Uutta release-artefaktia ei julkaista ennen Milestone 1:tä:** CQ-AUD-001 on ainoa
   varmennettu P0.
2. **Seuraavaa vakaaksi/robustiksi nimettyä versiota ei julkaista ennen Milestone
   2:ta:** recorder, session lifecycle ja reboot ovat P1.
3. Milestone 3–7 voidaan toimittaa pieninä versioina, kun niiden omat testit ovat
   vihreitä eikä soundi-/workflow-semanttiikka muutu hiljaa. GUI-poikkeama
   `GUI_SPEC.md`:stä vaatii käyttäjän etukäteishyväksynnän.
4. Lopullinen release-status syntyy vasta Milestone 8:n todellisesta evidenssistä,
   ei suunnitelman checkboxeista.
