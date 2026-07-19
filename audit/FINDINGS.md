# Auditointilöydökset

> **Pääarvioinnin status 2026-07-19:** alla on alkuperäinen löydösjoukko ja sen
> alkuperäinen vakavuus. Käytä toteutuksessa `VERIFIED_FINDINGS.md`:n rajauksia ja
> prioriteetteja sekä `REMEDIATION_ROADMAP.md`:n järjestystä. Uuden GUI:n ratkaisut
> tulevat repositoryn `GUI_SPEC.md`:stä.

Auditointi tehtiin repositoryn commitissa 43724f31b8f53a407cb56bd61972ed736aabb944 (main, tag v1.0). Vakavuusluokat ja luottamustasot noudattavat AUDIT_PROMPT.md:tä. Varmistettu tarkoittaa, että havainto on osoitettu lähdekoodista, dokumentaatiosta tai suoritetulla testillä. Vahva epäily tarkoittaa, että mekanismi on osoitettu, mutta vaikutusta ei ajettu loppuun live-runtime- tai laitteistoympäristössä. Testaamaton tarkoittaa suunnitteluriskiä ilman riittävää toteutusnäyttöä.

## Yhteenveto

| Vakavuus | Määrä | Tunnisteet |
|---|---:|---|
| P0 | 2 | CQ-AUD-001–002 |
| P1 | 4 | CQ-AUD-003–006 |
| P2 | 16 | CQ-AUD-007–022 |
| P3 | 3 | CQ-AUD-023–025 |
| Info | 1 | CQ-AUD-026 |

Kaksi P0-löydöstä ovat julkaisun estäviä: hyväksytty lähtökonfiguraatio voi summata erikseen rajoitettuja signaaleja turvakaton jälkeen, ja recorderin asynkroninen käynnistys voi menettää tallennuksen omistuksen. P1-ryhmässä korostuvat serverin todellisen rebootin puute, session elinkaarikilpailut, deck-latausten omistus ja tilallisten DSP-silmukoiden NaN-palautuminen.

## CQ-AUD-001 — Päällekkäiset hardware-lähtökanavat ohittavat lopullisen turvakaton

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-001 |
| Vakavuus | P0 |
| Luottamus | varmistettu |
| Vaikutus | Samaan fyysiseen kanavaan summatut main- ja/tai stem-signaalit voivat ylittää dokumentoidun 0,89:n katon ja klipata tallenteen/DAC-syötteen. Stem-overlap voi lisäksi tuoda lähes täystasoisen Coco-signaalin master-ulostuloon masterVolista riippumatta, joten käyttäjän normaali tasonsäätö ei takaa odotettua vaimennusta. |
| Tarkka sijainti | classes/CQConfig.sc:106-111,149-156; classes/CQSynthDefs.sc:312-379; C:/Program Files/SuperCollider-3.13.0/HelpSource/Classes/Out.schelp:12-18,57-58; SPEC.md:472-488; tests/nrt_safety.scd:19,259-295,473-492 |
| Tekninen perustelu | Konfiguraatio tarkistaa vain taulukon koon, kokonaislukutyypin ja ei-negatiivisuuden. Se sallii duplikaatit outChans-taulukossa, duplikaatit stemOuts-taulukossa sekä main/stem-päällekkäisyyden. Main ja stemit kulkevat erillisissä LeakDC → tanh → Limiter → bad-value-squelch -ketjuissa ja kirjoitetaan niiden jälkeen Out.ar-kutsuilla. SuperCollider summaa samaan busiin tehdyt kirjoitukset, joten summan jälkeen ei enää ole limiteriä. Stemit eivät tarkoituksella seuraa masterVolia, joten päällekkäisyys ohittaa myös sen normaalin tasonsäätöroolin; monitorMute/panic vaikuttaa silti molempiin. |
| Toisto | Validoi ensin outChans: [0, 0]. Validoi erikseen outChans: [0, 1], stemOuts: [0, 1] ja pahin outChans: [0,0], stemOuts: [0,0]. Renderöi neljä post-safety-kanavaa erillisiksi tiedostokanaviksi ja laske mahdolliset hardware-bus-summat vain offline-analyysinä. Älä koskaan toista päällekkäistä konfiguraatiota DAC:iin, kuulokkeisiin tai kaiuttimiin. |
| Havaittu evidenssi | outChans-duplikaatti ja main/stem-päällekkäisyys hyväksyttiin erillisissä harness-kutsuissa; kaikkien neljän yhdistelmän hyväksyntä seuraa validaattorin riippumattomista tarkistuksista. Nykyisen 4-kanavaisen NRT-renderin offline-summahuiput olivat mainL+mainR 1.7323701381683, mainL+stem1 1.6085547208786, mainR+stem2 1.6125874519348 ja stem1+stem2 1.4868720173836. Myöhemmässä read-only QA:ssa tässä samassa renderissä kaikkien neljän offline-summa oli 3.2192421556 eli noin +10,16 dBFS. Tavallinen erillisillä kanavilla ajettu NRT läpäisi, joten nykyinen testi ei kata vikaa. |
| Suositeltu korjaussuunta | Vaadi kaikkien hardware-output-indeksien globaali yksikäsitteisyys ja main/stem-joukkojen erillisyys. Jos mono-foldaus halutaan, miksaa se eksplisiittisesti ennen yhtä yhteistä lopputurvaketjua. Lisää konfiguraation hylkäystestit ja laitekanavakohtaisen loppusumman regressiotesti. |
| Regressioriski | Käyttäjät, jotka ovat käyttäneet samaa indeksiä epävirallisena monoasetuksena, tarvitsevat migraation. Älä heikennä limiterin nykyisiä NRT-rajoja. |

## CQ-AUD-002 — Recorderin asynkroninen start/stop ei ole serialisoitu

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-002 |
| Vakavuus | P0 |
| Luottamus | vahva epäily |
| Vaikutus | Kaksi DiskOut-synthiä voi kirjoittaa samaan tiedostoon, ensimmäinen bufferi tai synth jäädä omistuksen ulkopuolelle ja stop sulkea vain viimeisen. Seurauksena voi olla puuttuva, vioittunut tai auki jäänyt tallenne. |
| Tarkka sijainti | classes/CQRecorder.sc:13-17,24-75,80-98; classes/Cocoquantus.sc:228-239,280-312; classes/CQGuiPanels.sc:185-188; classes/CQGui.sc:207-210; tests/classes/CQRecorderTest.sc:34-43 |
| Tekninen perustelu | isRecording on tosi vasta, kun synth-instanssimuuttuja on asetettu kahden server.sync-odotuksen jälkeen. Kaksi nopeaa start-kutsua läpäisee saman guardin. Molemmat käyttävät jaettuja path-, buffer- ja synth-instanssimuuttujia, ja voivat valita saman vielä luomattoman tiedostonimen. Välitön stop on no-op käynnistysikkunassa. |
| Toisto | Boottaa testiympäristö, suorita samassa tulkkausblokissa CQ.rec; CQ.rec; ja tarkista nodet, bufferit sekä tiedostot ennen ja jälkeen CQ.recStop-kutsun. Toista CQ.rec; CQ.recStop välittömästi. Käytä vain testisignaalia ja erillistä testihakemistoa. |
| Havaittu evidenssi | Staattinen async-polku vahvistaa guard-ikkunan ja jaetut viitteet. Nykyinen unit-testi tarkistaa vain jo olemassa olevien tiedostojen suffix-nimeämisen. Live-reproduktiota ei tehty, jotta auditointi ei loisi tai vaarantaisi tallenteita. |
| Suositeltu korjaussuunta | Lisää eksplisiittinen idle → starting → recording → stopping -tilakone, operaatiotoken tai peruutettava task, lokaalisti omistetut path/buffer/synth-viitteet, tiedostopolun atominen varaus sekä virhe- ja teardown-rollback. Tee toggle idempotentiksi. |
| Regressioriski | REC-napin vaste, stopin odotus ja graph-resurssien rekisteröintijärjestys muuttuvat. Testaa sekä 2- että 4-kanavainen recStems-polku. |

## CQ-AUD-003 — CQ.reboot ei reboottaa scsynthiä eikä aktivoi ServerOptions-muutoksia

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-003 |
| Vakavuus | P1 |
| Luottamus | varmistettu |
| Vaikutus | Device-, sampleRate-, blockSize-, hardwareBufferSize- ja kanavamäärämuutokset voivat jäädä käyttämättä. StemOuts voi näyttää konfiguroidulta, vaikka serveri olisi edelleen bootattu vain kahdelle hardware-ulostulolle. |
| Tarkka sijainti | classes/Cocoquantus.sc:11-64,280-319; classes/CQGraph.sc:22-40; C:/Program Files/SuperCollider-3.13.0/SCClassLibrary/Common/Control/Server.sc:789-797,1059-1069; README.md:92-103 |
| Tekninen perustelu | CQ.free vapauttaa projektin graphin mutta ei serveriä. Seuraava CQ.boot asettaa ServerOptions-arvot ja kutsuu waitForBootia. SC 3.13:n waitForBoot boottaa vain, jos server ei jo käy; muuten callback jatkuu nykyisessä prosessissa. Todellinen quit+boot on Server.reboot. Sama ongelma koskee ensimmäistä CQ.boot-kutsua, jos Server.default on valmiiksi käynnissä. |
| Toisto | Boottaa serveri 44100 Hz:llä, muuta config 48000 Hz:iin, suorita CQ.reboot ja kysy serverin toteutunut sample rate. Toista hardware-ulostulojen määrää muuttavalla stemOuts-konfiguraatiolla. |
| Havaittu evidenssi | Projektin ja SuperCollider 3.13:n kontrollivirta luettiin kokonaan ja osoittaa, ettei server quit/reboot -kutsua tehdä. Varsinaista audiolaitteen reboot-koetta ei auditissa ajettu. |
| Suositeltu korjaussuunta | Erota käsitteet graph restart ja server reboot. Tunnista muuttuneet boot-only-optiot ja tee hallittu Server.reboot tai anna eksplisiittinen virhe. Määrittele samalla, saako CQ sammuttaa muiden käyttämän Server.defaultin. |
| Regressioriski | Oikea server-reboot katkaisee kaiken samassa scsynthissä olevan työn. Jaetun serverin omistuspolitiikka on ratkaistava ennen muutosta. |

## CQ-AUD-004 — Boot/free/reboot-elinkaari ei ole kilpailuturvallinen eikä osittaisella bootilla ole kattavaa rollbackia

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-004 |
| Vakavuus | P1 |
| Luottamus | vahva epäily |
| Vaikutus | Nopeat boot/free/boot-kutsut tai bootin keskellä tapahtuva virhe voivat jättää grouppeja, nodeja, buffereita, OSCdefejä tai vanhan callbackin toimimaan väärään sessioon. |
| Tarkka sijainti | classes/Cocoquantus.sc:11-64,280-319; classes/CQGraph.sc:43-76,79-132,144-155 |
| Tekninen perustelu | waitForBoot-callback käyttää muuttuvia classvareja ilman session generation -tunnistetta. free käynnistää seuraamattoman AppClock-routinen, ja reboot luottaa kiinteään 0,8 sekunnin viiveeseen. CQGraph asettaa isRunning-lipun vasta spawnin lopussa; free vapauttaa root-ryhmän vain isRunning-tilassa, joten osittainen build voi jäädä omistamatta. |
| Toisto | Server sammutettuna suorita CQ.boot; CQ.free tai CQ.boot; CQ.boot samassa tulkkauskierroksessa. Tee erillisessä testissä tarkoituksellinen poikkeus buildin ja spawnin väliin. Tarkista s.queryAllNodes, bufferimäärä, OSCdefit ja seuraavan bootin toimivuus. |
| Havaittu evidenssi | Kontrollivirta ja puuttuva generation/rollback ovat varmistettuja. Kilpailua ei ajettu live-serverillä tässä auditissa. |
| Suositeltu korjaussuunta | Lisää eksplisiittinen lifecycle-tila, session generation -token, callbackeihin paikalliset session viitteet, awaitable/idempotentti teardown ja protect/rollback. Vapauta olemassa oleva root group myös osittaisessa bootissa. |
| Regressioriski | CmdPeriod-, GUI-, recorder-, deck- ja reboot-järjestys on testattava yhdessä; väärä omistuksen muutos voi vapauttaa uuden session resursseja. |

## CQ-AUD-005 — Deck-latauksissa ei ole peruutusta, session validointia eikä last-request-wins-takuuta

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-005 |
| Vakavuus | P1 |
| Luottamus | vahva epäily |
| Vaikutus | Hitaampi vanha lataus voi korvata käyttäjän viimeisimmän valinnan. CQ.free-kutsun jälkeen valmistuva lataus voi vuotaa bufferin tai yrittää luoda synthin jo vapautettuun graphiin. |
| Tarkka sijainti | classes/CQDeck.sc:27-53,84-101,177-198; classes/Cocoquantus.sc:139-148,302-306 |
| Tekninen perustelu | Jokainen load luo seuraamattoman fork-routinen ja odottaa Condition.hang-kutsulla ilman timeoutia. free nollaa vain kielipuolen viitteet. Lataus ei tarkista pyyntögeneraatiota tai session voimassaoloa ennen prSwapia. |
| Toisto | Lataa ensin suuri ja heti perään pieni tiedosto; varmista, että viimeisin pyyntö jää voimaan. Toista suuri load + välitön CQ.free ja tarkista myöhemmin node- ja bufferimäärä. |
| Havaittu evidenssi | Staattinen async- ja free-polku vahvistaa omistuksen aukon. Kilpailua ei ajettu live-serverillä eikä suurilla tiedostoilla. |
| Suositeltu korjaussuunta | Lisää request-generation, peruutettava task, lokaalisti omistettu bufferi, session-validointi juuri ennen swappia sekä stale/error-polku, joka aina vapauttaa uuden bufferin. Lisää timeout ja last-request-wins-testi. |
| Regressioriski | next/prev/random, folder-scan, crossfade ja callback-semanttiikka voivat muuttua. |

## CQ-AUD-006 — NaN/inf suodatetaan vasta lopussa, mutta tilallisia silmukoita ei suojata eikä virheitä raportoida

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-006 |
| Vakavuus | P1 |
| Luottamus | vahva epäily |
| Vaikutus | Hetkellinen huono arvo voi jäädä Coco-RAMiin tai Quantussy/Space-palautesilmukkaan ja pitää alijärjestelmän mykkänä resetointiin asti. Käyttäjä ei saa dokumentoitua diagnoosia. |
| Tarkka sijainti | classes/CQSynthDefs.sc:103-155,234-241,273-285,321-323,387-391; C:/Program Files/SuperCollider-3.13.0/HelpSource/Classes/CheckBadValues.schelp:15-24; tests/nrt_safety.scd:53-57,65-72,494-498; SPEC.md:486 |
| Tekninen perustelu | CheckBadValues-kutsut ovat master- ja stem-ketjujen lopussa ja käyttävät post: 0 -asetusta. Quantussyn LocalOut, Cocon BufWr ja Spacen LocalOut saavat kirjoittaa huonon arvon omaan tilaansa ennen lopputarkistusta. Nykyinen NRT injektoi NaN:n suoraan master-bussille ja ohittaa nämä silmukat. |
| Toisto | Injektoi yksi NaN- ja inf-lohko erikseen Cocon record-polkuun, Quantussyn ringiin ja Space-feedbackiin. Jatka äärellisellä signaalilla ja vaadi palautuminen määräajassa sekä yksi rate-limitattu ilmoitus. |
| Havaittu evidenssi | Master-bus-NaN ei päässyt NRT-ulostuloon. Raportointipuute ja sanitoinnin sijainti ovat varmistettuja; pysyvää loop-myrkytystä ei ajettu. |
| Suositeltu korjaussuunta | Sanitointi ennen jokaista BufWr- ja LocalOut-kirjoitusta sekä reunataajuudella rajoitettu SendReply/diagnostiikka. Säilytä nykyinen lopun fail-closed-squelch. |
| Regressioriski | Pieni CPU-lisä ja mahdollinen vaikutus erittäin poikkeaviin signaaleihin; äärellisen normaalipolun on pysyttävä näytetasolla muuttumattomana. |

## CQ-AUD-007 — Sisäänrakennetut presetit ovat järjestysriippuvaisia overlay-reseptejä

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-007 |
| Vakavuus | P2 |
| Luottamus | varmistettu |
| Vaikutus | Samanniminen preset ei tuota toistettavaa statea. Classic voi jättää odotetun pre/deck-lähteen käsittelemättä aiemman presetin nollaamien slotien vuoksi, ja empty ei vastaa manuaalin kuvaamaa puhdasta pöytää. Kuultavan vaikutuksen voimakkuutta ei tässä auditissa todennettu. |
| Tarkka sijainti | classes/CQMatrix.sc:160-280; classes/Cocoquantus.sc:203-215; SPEC.md:278-282; tests/manual_checklist.md:174,438-453; tests/classes/CQMatrixTest.sc:82-151 |
| Tekninen perustelu | Facade korvaa koko matriisin, mutta kirjoittaa vain presetin eksplisiittisesti listaamat parametrit. Empty.params on tyhjä. Kaikki listaamattomat state-arvot jäävät edellisestä tilanteesta. |
| Toisto | Vertaa fresh → classic ja selfNoise → classic. Vertaa default-statea ja quantumDrone → empty. |
| Havaittu evidenssi | Serverittömässä reprossa selfNoise → classic erosi fresh → classic -tilasta 13 parametrilla. QuantumDrone → empty jätti 20 epäoletusarvoista parametria. Ajo päättyi markerilla CQ_PRESET_OVERLAY_REPRO_OK. |
| Suositeltu korjaussuunta | Päätä ovatko nimet deterministisiä sound-presettejä vai overlay-reseptejä. Deterministisessä mallissa sovelletaan rajattu, dokumentoitu baseline ennen presetiä. Overlay-mallissa nimeä toiminto sen mukaisesti ja lisää erillinen oikea reset/clear. |
| Regressioriski | Korkea: nykyiset käyttäjät voivat hyödyntää kerrostamista. Älä resetoi masteria, mic gainia, laiteasetuksia tai ladattuja tiedostoja ilman erillistä tuotevalintaa. |

## CQ-AUD-008 — Linked stereo -tiedoston dry-monitorointi romahtaa dual monoksi

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-008 |
| Vakavuus | P2 |
| Luottamus | varmistettu |
| Vaikutus | Stereokuva katoaa masterDry-polussa ja L/R-signaalit summataan molemmille kanaville. Tämä muuttaa balanssia ja voi kasvattaa masteriin tulevaa tasoa noin 6 dB ennen lopputurvaa. |
| Tarkka sijainti | classes/Cocoquantus.sc:139-148; classes/CQSynthDefs.sc:78-81,306-308; SPEC.md:183-186 |
| Tekninen perustelu | Jokainen mono-deck kirjoitetaan dry-bussin molempiin kanaviin. loadStereo mapittaa vasemman tiedoston deck A:han ja oikean deck B:hen, jolloin masterDry lukee kummastakin kanavasta saman L+R-summan. |
| Toisto | Käytä tiedostoa, jossa L-kanavalla on 440 Hz ja R-kanavalla 880 Hz. Aseta Coco-levelit nollaan ja masterDry arvoon 1. Molemmista lähdöistä löytyy koodin mukaan molemmat taajuudet. |
| Havaittu evidenssi | Signaalireitti on yksiselitteinen koodissa. Varsinaista audiotiedosto-NRT:tä ei ajettu. |
| Suositeltu korjaussuunta | Anna linked-parille eksplisiittinen stereodry-reitti, joka säilyttää L/R-sijoittelun. Säilytä itsenäisten mono-deckien keskitys erillisenä toimintatapana. |
| Regressioriski | Nykyiset patchit voivat luottaa keskitettyihin deckeihin; linked-tila pitää erottaa selvästi mono-tilasta. |

## CQ-AUD-009 — Linked stereo -kanavat eivät käynnisty näytetarkasti yhdessä

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-009 |
| Vakavuus | P2 |
| Luottamus | vahva epäily |
| Vaikutus | L/R-alkuhetki riippuu kahden asynkronisen tiedostoluvun valmistumisjärjestyksestä ja voi tuottaa transientti- tai vaihevirheen. |
| Tarkka sijainti | classes/Cocoquantus.sc:139-148; classes/CQDeck.sc:27-53,177-198 |
| Tekninen perustelu | loadStereo käynnistää kaksi itsenäistä CQDeck.load-routinen polkua. Kumpikin lukee, normalisoi, synkronoi, vaihtaa bufferin ja luo soittimen omalla aikataulullaan; yhteistä barrieria tai yhtä OSC-bundlea ei ole. |
| Toisto | Tee stereotiedosto, jonka molemmissa kanavissa on samaan näytteeseen asetettu impulssi. Lataa se useita kertoja ja mittaa ensimmäisten ei-nollanäytteiden ero. |
| Havaittu evidenssi | Asynkroninen rakenne on varmistettu. Näytetarkkaa impulssimittausta ei auditissa tehty. |
| Suositeltu korjaussuunta | Lataa molemmat bufferit yhteisen operaation alle, odota yhteisellä barrierilla ja käynnistä molemmat synthit samassa aikaleimatussa bundlessa. |
| Regressioriski | Bufferien omistus, mono-fallback, navigointi ja crossfade tarvitsevat yhteiset regressiot. |

## CQ-AUD-010 — Castle-offsetit ohittavat parametrirekisterin lagin

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-010 |
| Vakavuus | P2 |
| Luottamus | vahva epäily |
| Vaikutus | Snapshot-morph tai API-automaatio voi muuttaa c1Off–c5Off-arvoja portaittain ja aiheuttaa sävelkorkeuden zipper-artefaktia. |
| Tarkka sijainti | classes/CQSpec.sc:130-133,201-203; classes/CQSynthDefs.sc:135-138; classes/CQSnapshots.sc:64-65,87-101 |
| Tekninen perustelu | CQSpec antaa numeerisille parametreille oletuksena 30 ms lagin, mutta Quantussy SynthDef lukee offsetit ilman CQSpec.lag-arvoa. Morph päivittää statea noin 30 Hz. |
| Toisto | Morphaa c1Off arvojen -1 ja +1 välillä hitaalla kestolla ja mittaa oskillaattorin hetkellinen taajuus tai kuuntele portaittaisuus matalalla turvallisella tasolla. |
| Havaittu evidenssi | Lagin puuttuminen on varmistettu lähdekoodista. Artefaktia ei kuunneltu eikä mitattu. |
| Suositeltu korjaussuunta | Käytä CQSpec.lag(name)-arvoa myös castle-offseteille ja lisää kontrollikäyrän regressiotesti. |
| Regressioriski | Muuttaa nykyistä Quantussy-morphin tuntumaa; hyväksy vain A/B-kuuntelun jälkeen. |

## CQ-AUD-011 — micChan:nil jättää aktiiviselta näyttävän mutta mykän mic-ohjaimen

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-011 |
| Vakavuus | P2 |
| Luottamus | varmistettu |
| Vaikutus | Oletuskonfiguraatiossa käyttäjä voi nostaa micGainia ja saada MIC LIVE -varoituksen, vaikka DSP korvaa mic-signaalin nollalla. Tämä näyttää reititys- tai laitevialta ja heikentää turvallisuusviestin uskottavuutta. |
| Tarkka sijainti | config.example.scd:11; SPEC.md:443,457-462; classes/CQSynthDefs.sc:30-36; classes/CQState.sc:70-71; classes/CQGuiPanels.sc:128-133; classes/Cocoquantus.sc:394-399; tests/manual_checklist.md:344-349 |
| Tekninen perustelu | micChan:nil tekee DSP-lähteestä DC.ar(0):n ja estää micGain-reitin serverille, mutta GUI:n mic-ohjain ja facade-varoitus eivät perustu todellisen mic-reitin olemassaoloon. |
| Toisto | Käynnistä oletuskonfiguraatiolla, valitse preampin lähteeksi mic ja nosta micGain. Tarkista ääni, GUI-varoitus ja lähetetyt synth-argumentit. |
| Havaittu evidenssi | Reititys ja GUI-polku on vahvistettu koodista; Qt/live-audio-toistoa ei tehty. Manuaalin odotus on ristiriidassa SPEC:n nil-semanttiikan kanssa. |
| Suositeltu korjaussuunta | Näytä disabled/hidden MIC (not configured), estä tai merkitse mykkä lähdevalinta ja sido varoitus todelliseen micChan-reittiin. Erota mahdollinen liveL:n sisäinen mikrofoni tästä polusta. |
| Regressioriski | Laitteiden erilaiset input-nimet ja sisäinen mikrofoni vaativat selkeän terminologian. |

## CQ-AUD-012 — Watchdog-trimmin todellinen tila jää GUI:ssa piiloon

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-012 |
| Vakavuus | P2 |
| Luottamus | varmistettu |
| Vaikutus | Watchdog voi jättää järjestelmän pysyvästi -12 tai -18 dB:iin, mutta GUI näyttää vain noin 12 sekunnin ajan tekstin “auto-trimmed -6 dB”. Käyttäjä voi tulkita pysyvän vaimennuksen audio- tai laiteviaksi. |
| Tarkka sijainti | classes/Cocoquantus.sc:261-277,371-383; classes/CQGui.sc:225-245; classes/CQGuiPanels.sc:195-227 |
| Tekninen perustelu | Facade askeltaa trimDb:tä -6 dB kerrallaan lattiaan -18 dB. GUI-viesti on vakio ja transientti, eikä master-paneeli näytä effective trim -arvoa tai reset-toimintoa. |
| Toisto | autoTrim:true, lähetä CQ.testWatchdog kolme kertaa ja tarkkaile GUI:ta 12 sekunnin jälkeen. Vertaa CQ.trimDb-arvoa näytettyyn tekstiin. |
| Havaittu evidenssi | Koodi osoittaa -6/-12/-18-portaat ja vakioidun -6 dB -tekstin. GUI:ta ei avattu auditissa. |
| Suositeltu korjaussuunta | Näytä pysyvä effective trim, esimerkiksi TRIM -12 dB, sekä Reset Trim. Pidä transientti OUTPUT HOT erillisenä ilmoituksena. |
| Regressioriski | Matala; huomioi kuitenkin autoTrim:false, jolloin trimmiä ei pidä näyttää syntyneenä. |

## CQ-AUD-013 — GUI:n arvot ovat yksiköttömiä ja terminologia jää liian tiiviiksi

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-013 |
| Vakavuus | P2 |
| Luottamus | varmistettu |
| Vaikutus | Hz, dB, oktaavit, sekunnit ja amplitudit näyttävät samalta raakaluvalta. Feedbackin unity-kohta, Space-returnin merkitys ja lyhenteet kuten spaf, q ja x eivät avaudu ilman lähdekoodia tai speksiä. |
| Tarkka sijainti | classes/CQGuiWidgets.sc:19-33,69-82; classes/CQGuiPanels.sc:26-59,98-111,203-206; SPEC.md:173-306 |
| Tekninen perustelu | Yksi geneerinen formatteri näyttää vain pyöristetyn luvun. CQSpecissä ei ole presentation-metadataa yksiköille, pitkille nimille tai tooltipeille, eikä GUI-koodista löydy tooltippejä. |
| Toisto | Avaa GUI ja vertaa qFreq-, masterVol-, qShLag-, c1Off- ja spaceDamp-arvojen esitystä niiden SPEC-yksiköihin. |
| Havaittu evidenssi | Widget-factoryn formatteri ja labelit on tarkastettu; visuaalista Qt-testiä ei ajettu. |
| Suositeltu korjaussuunta | Lisää CQSpeciin tai erilliseen presentation-kerrokseen label, unit, formatter, tooltip ja semanttiset merkit. Näytä vähintään Hz, dB, oct ja s sekä feedbackin unity-kohta. |
| Regressioriski | Layout voi kasvaa, käännökset vaikeutua ja tiivis livekäyttö kärsiä; käytä lyhyttä labelia ja tooltipissä pitkää selitystä. |

## CQ-AUD-014 — Space- ja Shake-ohjaimet näyttävät oletustilassa helposti tehottomilta

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-014 |
| Vakavuus | P2 |
| Luottamus | vahva epäily |
| Vaikutus | Käyttäjä voi tulkita näkyvän Space- tai Shake-ohjaimen rikkinäiseksi, koska vaikutuksen edellytykset ovat muualla paneelissa eikä GUI ilmaise niitä. |
| Tarkka sijainti | classes/CQGuiPanels.sc:67-76,118,195-206; classes/CQSpec.sc:175,187,190,212; classes/CQSynthDefs.sc:119,196-200; PLAN.md:579-591; tests/manual_checklist.md:457-477,523-535 |
| Tekninen perustelu | spaceMix ei tuota ääntä, jos molempien Cocojen Send-arvot ovat nolla. Shake 0.3 jää monessa oletustilassa vähäiseksi, koska Spaf ja Q-slotit ovat nollassa sekä porttikynnys on 0.5. GUI ei näytä riippuvuuksia. |
| Toisto | Boottaa default-state, nosta vain spaceMix; tämän jälkeen nosta Coco Send. Testaa vastaavasti Shake 0.3 ensin default-stateen ja sitten manuaalin edellytysasetuksilla. |
| Havaittu evidenssi | Oletusarvot ja signaalireitit ovat varmistettuja. Käytettävyysvaikutusta ei tässä auditissa kuunneltu tai käyttäjätestattu. |
| Suositeltu korjaussuunta | Näytä kontekstuaalinen prerequisite-vihje tai korosta Send/Spaf/Q-slotit. Mahdollinen Prepare-macro pitää tehdä eksplisiittiseksi. Älä muuta DSP-oletuksia vain löydettävyyden vuoksi. |
| Regressioriski | Esityskerroksen ratkaisu on matalariskinen; oletusarvojen muutos on korkean sound-identiteettiriskin muutos. |

## CQ-AUD-015 — Kiinteä, hiirikeskeinen GUI on skaalaus- ja saavutettavuusriski

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-015 |
| Vakavuus | P2 |
| Luottamus | vahva epäily |
| Vaikutus | 1024×768-, korkean DPI:n tai resize-tilanteessa sisältö voi leikkautua tai muuttua vaikeaksi käyttää. Lähes kaikkien kontrollien canFocus=false estää näppäinnavigointia ja heikentää accessibility-polkuja. |
| Tarkka sijainti | classes/CQGui.sc:74,93-111; classes/CQGuiMatrix.sc:7,22-34; tests/manual_checklist.md:392-418 |
| Tekninen perustelu | Ikkuna avataan kiinteään 1120×740-kokoon, matriisilla on kiinteät mitat ja fokus poistetaan rekursiivisesti lähes kaikilta widgeteiltä. Valinta suojaa globaaleja performance-shortcutteja, mutta vaihtoehtoista saavutettavaa polkua ei ole. |
| Toisto | Avaa GUI 1024×768-resoluutiolla ja DPI-asetuksilla 125, 150 ja 200 %. Resize pienemmäksi, käy kaikki kontrollit läpi näppäimistöllä ja varmista samalla performance-shortcutit. |
| Havaittu evidenssi | Kiinteät mitat ja fokuslogiikka ovat varmistettuja. Qt/DPI/accessibility-testiä ei ajettu. |
| Suositeltu korjaussuunta | Lisää scroll/breakpointit, matriisin zoomaus ja valinnainen näppäinfokus. Globaali event filter voi säilyttää shortcutit ilman kaikkien widgettien fokuskiellon pakkoa. |
| Regressioriski | Nykyinen performance-näppäinten fokussemantiikka on tietoinen ja manuaalisesti regressiotestattu; se on suojattava. |

## CQ-AUD-016 — Headless-testien class library ei ole hermettinen eikä runtime-resurssielinkaarta testata

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-016 |
| Vakavuus | P2 |
| Luottamus | varmistettu |
| Vaikutus | Käyttäjän tai koneen extensionit voivat muuttaa testitulosta. Boot/free/reboot-, recorder/deck-concurrency-, duplicate-output- ja resource-leak-viat voivat läpäistä koko nykyisen sarjan. |
| Tarkka sijainti | tests/sclang_conf_dev.yaml:1-6; tests/compile_check.scd:1-3,8-29; tests/run_all.scd:1-4,12-36; tests/nrt_safety.scd:21-24,299; tests/nrt_hold_integrity.scd:15-18,94 |
| Tekninen perustelu | YAML lisää includePaths-polut mutta ei käytä excludeDefaultPaths:true-asetusta. Todellinen compile latasi system/user-extensioneita ja raportoi kahden metodin ylikirjoituksen. NRT rakentaa scoret suoraan SynthDefeistä eikä aja Cocoquantus/CQGraph-facaden elinkaarta. |
| Toisto | Aja compile kahdessa ympäristössä, joista toisessa on ylimääräinen SC-extension. Vertaa class treeä ja tuloksia. Aja lisäksi toistuva boot/free-integraatiotesti ja laske node-, group-, buffer-, OSCdef- ja taskimäärät. |
| Havaittu evidenssi | Compile ja unit läpäisivät, mutta lokissa näkyi kaksi extension method overridea. Testihakemiston inventaario osoitti mainittujen lifecycle- ja concurrency-testien puuttuvan. |
| Suositeltu korjaussuunta | Tee projektin vanilla-vaatimusta vastaava hermettinen sclang-konfiguraatio ja erillinen live-server-integration suite resurssimäärien tarkistuksineen. |
| Regressioriski | Tarkoituksella asennetut extensionit eivät enää ole testien käytössä; projektin vanilla-vaatimuksen perusteella tämä on tavoiteltua, mutta testikäynnistysdokumentaatio muuttuu. |

## CQ-AUD-017 — 44,1/96 kHz- ja block-size-lupauksille ei ole regressiotestiä

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-017 |
| Vakavuus | P2 |
| Luottamus | varmistettu |
| Vaikutus | Dokumentoitu sample-rate-riippumattomuus voi rikkoutua huomaamatta. Yhden control blockin tarkoituksellisten viiveiden ajallinen pituus muuttuu sample raten ja block sizen mukana. |
| Tarkka sijainti | SPEC.md:436,504-505; classes/CQConfig.sc:91-94; tests/nrt_safety.scd:21-24; tests/nrt_hold_integrity.scd:15-18; tests/manual_checklist.md:615-624 |
| Tekninen perustelu | Konfiguraatio hyväksyy 44,1/48/96 kHz ja block sizet 16–512, mutta molemmat NRT:t sekä dokumentoitu CPU-testi käyttävät vain 48 kHz/64-skenaariota. |
| Toisto | Parametrisoi NRT:t näytetaajuuksille 44100, 48000 ja 96000 sekä block sizeille vähintään 16, 64, 256 ja 512. Vertaa turvallisuutta, hold-integriteettiä, Quantussy-taajuuksia ja tarkoituksellisten viiveiden käyttäytymistä. |
| Havaittu evidenssi | Testikoodin kiinteät arvot ja puuttuvat muut ajot on varmistettu. Tässä auditissa ajettiin vain 48 kHz:n NRT:t. |
| Suositeltu korjaussuunta | Tee SR/BS-matriisi CI:hin tai vähintään yöajoon. Älä kompensoi yhden blokin viiveitä ennen mittaus- ja kuuntelupäätöstä. |
| Regressioriski | Testiaika kasvaa. Mahdollinen DSP-kompensaatio olisi korkean identiteettiriskin muutos. |

## CQ-AUD-018 — Odottamaton scsynthin kuolema voi jättää facaden virheellisesti bootatuksi

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-018 |
| Vakavuus | P2 |
| Luottamus | vahva epäily |
| Vaikutus | Ulkoisen s.quit-kutsun tai scsynth-crashin jälkeen CQ.isBooted voi edelleen palauttaa true, jolloin uusi CQ.boot kieltäytyy ja GUI/state jää kuolleeseen sessioon. |
| Tarkka sijainti | classes/Cocoquantus.sc:11-24,67-69,321-331; classes/CQGraph.sc:144-155 |
| Tekninen perustelu | isBooted luottaa graph.isRunning-lippuun. Projekti nollaa lipun omassa CmdPeriod-hookissa, mutta ei seuraa ServerQuit- tai status-tapahtumaa. |
| Toisto | Boottaa CQ, suorita s.quit ulkoisesti tai lopeta scsynth, odota statuspäivitys ja kutsu CQ.isBooted sekä CQ.boot. |
| Havaittu evidenssi | Puuttuva server-quit-watcher ja lipun ainoa normaali nollauspolku ovat varmistettuja. Crash/quit-toistoa ei auditissa ajettu. |
| Suositeltu korjaussuunta | Lisää sessioniin sidottu ServerQuit/status-watcher, joka merkitsee graphin kuolleeksi, siivoaa kielipuolen hallitusti ja mahdollistaa recovery-bootin. |
| Regressioriski | Tilapäinen unresponsive-tila on erotettava aidosta quitistä; watcher ei saa purkaa uutta sessiota vanhan ilmoituksen perusteella. |

## CQ-AUD-019 — GUI:n deferred-init voi rakentaa vapautetun instanssin tai kaksoisikkunan

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-019 |
| Vakavuus | P2 |
| Luottamus | vahva epäily |
| Vaikutus | Nopea CQ.gui/CQ.free tai kaksi CQ.gui-kutsua voi luoda ikkunan ja taskit jo cleaned-tilassa tai useita GUI-instansseja, joiden kuuntelijoita ei enää siivota oikein. |
| Tarkka sijainti | classes/CQGui.sc:22-31,52-64,258-274; classes/Cocoquantus.sc:220-225,290-291 |
| Tekninen perustelu | prInit ajoittaa build/listen/task-ketjun deferillä. free voi asettaa cleaned=true ennen deferin ajoa, mutta defer ei tarkista peruutusta. Ennen window-instanssin syntyä isClosed palauttaa true, joten facade voi luoda toisen CQGui-instanssin. |
| Toisto | Display-ympäristössä kutsu CQ.gui kahdesti samassa event-loopissa. Toista CQ.gui; CQ.free välittömästi ja tarkista ikkunat, OSCdefit, SimpleControllerit ja taskit. |
| Havaittu evidenssi | Deferred-kontrollivirta ja puuttuva lifecycle-token ovat varmistettuja. Qt-reproduktiota ei tehty. |
| Suositeltu korjaussuunta | Lisää initializing/ready/closing/closed-tila, defer-generation ja cancelled-tarkistus ennen ja jälkeen buildin. Facaden pitää pitää pending-instanssia varattuna. |
| Regressioriski | GUI:n reopen, ikkunan close-box ja CmdPeriod-polut on testattava yhdessä. |

## CQ-AUD-020 — GUI → State → route → DSP -ketjulle ei ole toistettavaa automaattitestiä

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-020 |
| Vakavuus | P2 |
| Luottamus | varmistettu |
| Vaikutus | Väärä widget-bind, state-route tai Synth-argumentin nimi voi läpäistä unit- ja NRT-sarjan, vaikka näkyvä kontrolli ei vaikuttaisi ääneen. |
| Tarkka sijainti | PLAN.md:526-529; tests/classes/CQStateTest.sc:1,9; tests/nrt_safety.scd:21-24,299; tests/nrt_hold_integrity.scd:15-18,94; tests-hakemiston inventaario |
| Tekninen perustelu | PLAN mainitsee scratch-GUI-smoken, mutta sitä ei ole repositoryn testeissä. State-testit käyttävät graph=nil-tilaa ja NRT antaa SynthDef-argumentit suoraan. Testihakemistossa ei ole CQGui-viittauksia. |
| Toisto | Riko paikallisesti testibranchissa yhden GUI-kontrollin bind-nimi tai yhden route-argumentin nimi ja aja nykyinen sarja; se ei kata koko päästä-päähän-ketjua. |
| Havaittu evidenssi | Testien rakenne ja puuttuvat GUI-viitteet tarkastettiin. Scratch-smoken tulosta ei voitu toistaa repositoryn aineistosta. |
| Suositeltu korjaussuunta | Lisää serveritön GUI/view-model-smoke, fake/spy-graph-route-testi kaikille 79 parametreille ja erillinen display-required Qt-smoke cleanup-, resize-, shortcut- ja drop-poluille. Lisää kriittisille kontrolleille NRT-vaikutustestit. |
| Regressioriski | Qt/headless-flakiness; pidä logiikka-, route- ja rendering-testit eri kerroksissa. |

## CQ-AUD-021 — Turvallisuus-, julkaisu- ja arkkitehtuuridokumentaatio on keskenään ristiriidassa

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-021 |
| Vakavuus | P2 |
| Luottamus | varmistettu |
| Vaikutus | Käyttäjä voi antaa watchdogille turvallisuusvastuun, päätellä väärin release-hyväksynnän tai odottaa runtime-ominaisuuksia, joita toteutus ei tarjoa. |
| Tarkka sijainti | tests/manual_checklist.md:628-643,647-656; SPEC.md:8,298-303,311-316,490-493; README.md:9-13,85-87; PLAN.md:7-13,636-653; ARCHITECTURE.md:263,285,294-295; config.example.scd:13; classes/CQConfig.sc:111,149-156; classes/CQMidi16n.sc:73-82; classes/CQGraph.sc:43-76; classes/Cocoquantus.sc:38,308 |
| Tekninen perustelu | Manuaalin abuse-osuus sanoo turvaketjun ja watchdogin pitävän kurissa, vaikka SPEC ja PLAN määrittelevät watchdogin ei-turvalliseksi notice/comfort-toiminnoksi. README/PLAN ilmoittavat v1.0:n hyväksytyksi, mutta README ja checklist jättävät KUUNTELU 7:n tai stem-livekohdat avoimeksi. SPEC antaa stemOuts-esimerkiksi neljä kanavaa [2,3,4,5], vaikka validaattori hyväksyy täsmälleen kaksi ja config-esimerkki käyttää [2,3]. ARCHITECTURE lupaa automaattisen MIDI-hotplug-watchin, deck-placeholderit ja live-state-recoveryn, joita koodi ei toteuta kuvatulla tavalla. |
| Toisto | Lue mainitut normatiiviset kohdat rinnakkain ja vertaa CQMidi16n-, CQGraph- ja boot-koodiin. |
| Havaittu evidenssi | Ristiriidat ovat suoria teksti- ja koodihavaintoja. Neljän stemOuts-arvon SPEC-esimerkki hylätään CQConfig.prCheckChansin 2..2-kokorajan vuoksi. PLANissa on historiallista kuuntelunäyttöä, mutta yksi kanoninen status puuttuu. |
| Suositeltu korjaussuunta | Nimeä yksi kanoninen release/status-lähde. Kreditoi turvallisuus bounded loopille ja limiterille, watchdog vain operatiiviseksi comfort-toiminnoksi. Päivitä arkkitehtuuri toteutuksen mukaiseksi ja merkitse live-stemit eksplisiittisesti testaamattomiksi. |
| Regressioriski | Ei runtime-riskiä. Säilytä historialliset hyväksyntämerkinnät changelogina, älä pyyhi niitä pois. |

## CQ-AUD-022 — Snapshot-tiedoston lukeminen suorittaa SuperCollider-koodia ennen validointia

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-022 |
| Vakavuus | P2 |
| Luottamus | varmistettu |
| Vaikutus | Toiselta henkilöltä saatu tai verkosta ladattu cqsnap.scd voi suorittaa mielivaltaista SuperCollider-koodia käyttäjän prosessin oikeuksilla ennen rakenteellista validointia. |
| Tarkka sijainti | classes/CQSnapshots.sc:118-136; tests/classes/CQSnapshotTest.sc:98-113; SPEC.md:379-409 |
| Tekninen perustelu | read kutsuu path.load-funktiota ja validoi palautetun arvon vasta suorituksen jälkeen. Tiedostopääte .scd myös vihjaa suoritettavaan SC-koodiin, mutta käyttäjäohje ei määrittele trust boundarya. |
| Toisto | Luo erilliseen testihakemistoon snapshot, joka tekee harmittoman marker-postauksen ennen validia Event-paluuarvoa, ja lue se CQSnapshots.readilla. Marker suoritetaan ennen prValidatea. |
| Havaittu evidenssi | Kutsu- ja validointijärjestys on varmistettu lähdekoodista. Auditissa ei suoritettu erillistä payload-tiedostoa. |
| Suositeltu korjaussuunta | Käytä dataformaattia ja parseria, joka ei evaluoi koodia, tai dokumentoi snapshotit eksplisiittisesti luotetuksi paikalliseksi koodiksi ja vaadi vahvistus ulkopuolisille poluille. Suunnittele version/migraation polku. |
| Regressioriski | Formaattimuutos voi rikkoa olemassa olevat snapshotit ja käsin muokattavuuden; tarvitaan turvallinen import/migraatio. |

## CQ-AUD-023 — Bits-kvantisoija tuottaa yhden kooditason enemmän kuin n-bittinen määritelmä

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-023 |
| Vakavuus | P3 |
| Luottamus | varmistettu |
| Vaikutus | Pienillä bits-arvoilla dokumentoitu n-bittisyys ja todellinen aaltomuoto eivät täsmää; esimerkiksi 3 bitillä saadaan 9 eikä 8 tasoa. |
| Tarkka sijainti | classes/CQSynthDefs.sc:237-240; SPEC.md:216,227; ARCHITECTURE.md:195 |
| Tekninen perustelu | Signaali rajataan inklusiivisesti välille -1…+1 ja pyöristysaskel on 2 ** (1 - bits). Molemmat päätepisteet mukaan lukien tasoja syntyy 2^bits + 1. |
| Toisto | Syötä lineaarinen ramppi record-polkuun, ohita muut muuttujat ja laske uniikit kvantisoidut arvot 3- ja 8-bit-asetuksilla. |
| Havaittu evidenssi | Kaava ja tasomäärä ovat matemaattisesti varmistettuja. Erillistä ramp-renderiä ei ajettu. |
| Suositeltu korjaussuunta | Määrittele täsmällinen signed-quantizer: esimerkiksi asymmetrinen yläraja tai vaihtoehtoinen askel. Tee päätös vasta kuuntelukalibroinnin jälkeen. |
| Regressioriski | Korkea ääni-identiteetille, feedbackille ja hold-käytökselle; muutos ei ole kiireellinen turvallisuuskorjaus. |

## CQ-AUD-024 — Kaksi ydintiedostoa ylittää projektin oman kokosuosituksen

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-024 |
| Vakavuus | P3 |
| Luottamus | varmistettu |
| Vaikutus | Lifecycle-, facade- ja DSP-vastuiden tarkastaminen sekä riskittömät muutokset vaikeutuvat, mikä kasvattaa regressioiden todennäköisyyttä. |
| Tarkka sijainti | CLAUDE.md:51; classes/Cocoquantus.sc:1-458; classes/CQSynthDefs.sc:1-424 |
| Tekninen perustelu | Projektin ohje suosittelee noin 400 rivin enimmäiskokoa. Cocoquantus yhdistää lifecycle-, OSC-, recorder-, deck-, GUI- ja pattern-vastuut; CQSynthDefs sisältää kaikki DSP-alijärjestelmät. |
| Toisto | Laske tiedostojen rivit ja vertaa CLAUDE.md:n ohjeeseen. |
| Havaittu evidenssi | Rivimäärät ovat 458 ja 424. |
| Suositeltu korjaussuunta | Jaa vastuita asteittain muuttamatta julkista CQ-APIa, SynthDef-nimiä tai graph-järjestystä. Tee vasta korkeampien prioriteettien jälkeen. |
| Regressioriski | SuperColliderin class-library compile-järjestys, class-nimet ja SynthDef-lähetysjärjestys on säilytettävä. |

## CQ-AUD-025 — Matriisin värit sekoittavat reittiperheen ja polariteetin merkityksen

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-025 |
| Vakavuus | P3 |
| Luottamus | vahva epäily |
| Vaikutus | Käyttäjä voi tulkita oranssin ja sinisen joko triangle/FM-perheiksi tai positiiviseksi/negatiiviseksi painoksi. Tämä vaikeuttaa 16×22-matriisin nopeaa lukemista. |
| Tarkka sijainti | SPEC.md:363-367; classes/CQGuiWidgets.sc:85-90; classes/CQGuiMatrix.sc:43-65 |
| Tekninen perustelu | Paletti dokumentoi oranssin triangle- ja sinisen FM-väriksi, mutta matriisi käyttää oranssia kaikille positiivisille ja sinistä kaikille negatiivisille arvoille. Legendaa tai erillistä perhemerkintää ei ole. |
| Toisto | Avaa matriisi, tee positiivinen ja negatiivinen reitti eri lähdeperheistä ja pyydä käyttäjää tulkitsemaan väri ilman speksiä. |
| Havaittu evidenssi | Väriarvot ja niiden kaksi dokumentoitua semantiikkaa ovat varmistettuja. Käytettävyysvaikutus on esteettinen tulkinta; käyttäjätestiä ei tehty. |
| Suositeltu korjaussuunta | Erota polariteetti merkillä, glyfillä tai kirkkaudella ja säilytä perheväri reunuksessa/ryhmäotsikossa. Lisää näkyvä legenda. |
| Regressioriski | Muuttaa tunnistettavaa banana-palettia; säilytä nykyiset perusvärit ja validoi muutos käyttäjällä. |

## CQ-AUD-026 — Historiallisen fidelityn lähdeketju ja osa pyydetyistä repository-ohjeista puuttuvat

| Kenttä | Arvo |
|---|---|
| Tunniste | CQ-AUD-026 |
| Vakavuus | Info |
| Luottamus | varmistettu |
| Vaikutus | Cocoquantus/Ciat-Lonbarde-historiaa ei voi auditoida itsenäisesti repositoryn ulkopuolisiin alkuperäislähteisiin asti. AGENTS.md- tai erillistä known-issues-lähdettä ei voitu sisällyttää auditointiin. |
| Tarkka sijainti | AUDIT_PROMPT.md:29-36; SPEC.md:40-57,557-569; repositoryn juuren ja dokumenttitiedostojen inventaario |
| Tekninen perustelu | SPEC nimeää Peter Blasserin kuvauksen, julkaistun kaavion/Richard Brewsterin ja Perfect Circuitin, mutta repositoryssa ei ole bibliografiaa, URL-osoitteita tai arkistoituja lähteitä. AGENTS.md puuttuu. Erillistä TODO/known-issues-tiedostoa ei ole; PLAN.md toimii osittain tässä roolissa. |
| Toisto | Suorita rg --files ja hae AGENTS.md, TODO, known issues, bibliography, http-viitteet sekä mainitut lähdenimet. |
| Havaittu evidenssi | Inventaario vahvisti puuttuvat tiedostot ja lähdeviitteet. |
| Suositeltu korjaussuunta | Lisää versionhallittu lähdeluettelo tai arkistoidut viitteet, merkitse väitteiden alkuperä ja pidä fakta, toteutushavainto sekä esteettinen tulkinta erillään. AGENTS.md ei ole tarpeen vain auditointia varten, ellei projekti halua repository-kohtaisia agenttiohjeita. |
| Regressioriski | Ei runtime-riskiä. Vältä jälkikäteistä historiallisten väitteiden ylivarmistamista ilman lähteitä. |

## Suojattavat vahvuudet

- CQSpec ja CQState.set muodostavat keskitetyn parametrifunnelin: arvot, enumit, NaN/inf ja rajat käsitellään ennen serveriä.
- CQMatrix clampaa kaikki kirjoituspolut ja hylkää tuntemattomat reitit.
- Graphin normaali suoritusjärjestys sources → quantussy → cocos → fx → master on selkeä ja tarkoituksenmukainen.
- Kaksi Coco-RAMia ovat aidosti erilliset, BufRd luodaan ennen BufWr:ää ja hold-integriteetti läpäisee nykyisen NRT-testin.
- Bounded recirculating clip/requantize ja main/stem-turvaketju läpäisevät erillisillä lähtökanavilla 90 sekunnin mayhem-testin.
- Tavallisessa teardownissa snapshot-morph, MIDIdef, facade-OSCdefit, GUI-controllerit ja taskit siivotaan kattavasti.
- GUI:n viisiosainen Sources | Coco 1 | Quantussy | Coco 2 | Master -hierarkia, symmetriset Coco-paneelit, mittarit, LEDit, file drop ja performance-shortcutit muodostavat vahvan perustan.
- Space on oletuksena pois päältä ja sen feedback on rajattu. Näitä rajoja tai nykyisiä NRT-kynnyksiä ei pidä löysentää korjausten yhteydessä.
