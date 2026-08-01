# Ihmisen suoritettava kuuntelu- ja käyttötestiprotokolla

## Suoritustila

> **TÄTÄ PROTOKOLLAA EI SUORITETTU TÄSSÄ AUDITISSA.**

Automaattiset testit eivät vahvista kuultavaa estetiikkaa, fyysisen ohjaimen tuntumaa tai monikanavaisen äänikortin käytöstä. Tämä dokumentti määrittää turvallisen, toistettavan jatkotestin ihmiselle. Jokainen suoritettu kohta tulee päivätä, nimetä, todentaa tallenteella tai lokilla ja merkitä erikseen läpäistyksi, hylätyksi tai estyneeksi.

Protokolla arvioi projektin omia käyttäytymisvaatimuksia. Se ei voi yksin todistaa historiallista tai piirikohtaista Cocoquantus-uskollisuutta, koska repositoriossa ei ole tarkistettavaa lähdebibliografiaa eikä vertailulaitteen tallenteita.

## 1. Turvallisuussäännöt

### 1.1 Ehdottomat säännöt

1. Pidä äänikortin fyysinen monitoritaso aluksi minimissä ja nosta se hitaasti vasta, kun boot, mute ja mittarit on tarkistettu.
2. Käytä ensimmäisissä vaiheissa `masterVol = -40 dB`, `masterDry = 0`, `spaceMix = 0` ja `micGain = 0`.
3. Pidä `CQ.panic`-komento valmiina. Jos ilmenee odottamaton voimakas ääni, jatkuva täystason signaali, digitaalinen huuto, DC-tyyppinen liike, dropout tai hallinnan katoaminen, paina PANIC/mute, laske fyysinen monitoritaso ja lopeta kyseinen koe.
4. Älä käytä kannettavan sisäistä mikrofonia ja kaiuttimia samanaikaisesti. Akustinen feedback ei kuulu tähän protokollaan.
5. Älä kytke stem-ulostuloja takaisin projektin inputteihin tai DAW:n input-monitoroinnin kautta master-ulostuloihin.
6. Käytä korkeaa feedbackia, `mayhem`-presetiä, wrapia ja hyperkaaosta vain lyhyinä jaksoina. Pidä kuuntelutaukoja.
7. Älä muuta turvaketjua, limiterin kattoa tai sisäistä feedback-rajaa testiä varten.
8. Pidä testitiedostojen huippu enintään noin −12 dBFS:ssä ennen projektin omaa deck-normalisointia. Käytä sisältöä, jonka transientit tunnet.

### 1.2 Fyysisten lähtöjen päällekkäisyys — ei kuuntelutestiä

Nykyinen konfiguraatiovalidointi ei estä master- ja stem-ulostulojen kirjoittamista samalle fyysiselle kanavalle. Samalle bussille kirjoitetut jo erikseen limiteroidut signaalit summataan limiterien jälkeen.

**Älä koskaan testaa duplikaatti- tai päällekkäisiä `outChans`/`stemOuts`-määrityksiä kuulokkeilla, kaiuttimilla tai fyysisellä DAC-ulostulolla.** Tällainen regressio testataan vain offline-NRT:llä tai bussien deterministisellä tiedostoanalyysillä sen jälkeen, kun testi on erikseen rakennettu.

Ennen jokaista live-boottia tarkista käsin:

- `outChans` sisältää kaksi eri kanavaa
- jos `stemOuts` on käytössä, se sisältää kaksi eri kanavaa
- yksikään `stemOuts`-kanava ei esiinny `outChans`-listassa
- äänikortissa on oikeasti kaikki nimetyt ulostulot
- DAW ei monitoroi stem-inputteja takaisin master-ulostuloon.

Turvallinen nelikanavainen esimerkki on `outChans: [0, 1]`, `stemOuts: [2, 3]` vain, jos laite tarjoaa vähintään neljä itsenäistä fyysistä ulostuloa. Jos laitteessa on vain kaksi ulostuloa, käytä `stemOuts: nil`.

### 1.3 Keskeytysehdot

Keskeytä testivaihe välittömästi, jos jokin seuraavista tapahtuu:

- master- tai stem-tallenteessa huippu ylittää −0,75 dBFS
- mittari tai analyysi näyttää NaN/inf-arvon tai `|DC| ≥ 0,02`
- limiter on jatkuvasti kiinni ilman että vaihe nimenomaisesti testaa ääripatchia
- PANIC ei mykistä havaittavasti noin välittömästi tai RESUME palaa odottamattomalla tasolla
- serveri kaatuu, audio jää päälle `CQ.free`:n jälkeen tai ääni palaa ilman käyttäjän komentoa
- tallennus ylikirjoittaa olemassa olevan tiedoston
- fyysisen ulostulon reititys ei vastaa tarkistettua kanavakarttaa
- kuuntelija kokee tason, kirkkauden tai rasituksen epämukavaksi.

Keskeytys merkitään tuloslomakkeeseen; testiä ei jatketa suuremmalla tasolla.

## 2. Testiympäristö ja aineisto

### 2.1 Tarvittava ympäristö

- sama SuperCollider-versio kuin projektin tuetussa ympäristössä
- testattava commit/hash ja puhdas tieto kaikista työpuun muutoksista
- äänikortti ja ajuri kirjattuna
- kuulokkeet tai monitorit, joiden voimakkuus voidaan laskea fyysisesti
- DAW tai muu tallennin peak-, RMS-, DC- ja kanavatarkastukseen
- vähintään nelikanavainen äänikortti vain stem-livevaiheeseen
- fyysinen 16n vain MIDI-vaiheeseen; sen puute ei estä muita vaiheita.

### 2.2 Vakioaineisto

Valmistele seuraavat tiedostot ja kirjaa niiden checksumit:

1. **Puhe/field:** 30–60 s mono, selviä taukoja ja vaihteleva dynamiikka.
2. **Rytmi/transientit:** 20–30 s mono, yksittäisiä iskuja ja hiljaisia välejä.
3. **Vakaa testiääni:** matalatasoinen siniaalto tai harmoninen ääni pitch-seurantaan.
4. **Stereo-identiteetti:** L-kanavalla vain 440 Hz:n jaksot, R-kanavalla vain 880 Hz:n jaksot, alussa molemmissa samanaikainen yksittäinen impulssi.
5. **Identtinen stereoimpulssi:** täysin sama yksittäinen impulssi molemmissa kanavissa, muu tiedosto hiljainen.

Älä käytä masteroitua täystason musiikkia feedback- tai abuse-vaiheissa.

### 2.3 Vakioitu lähtötila

Boottaa fyysinen monitoritaso minimissä:

```supercollider
CQ.boot(onReady: {
    CQ.set(\masterVol, -40);
    CQ.set(\masterDry, 0);
    CQ.set(\spaceMix, 0);
    CQ.set(\micGain, 0);
    CQ.clearPatch;
    "LT_READY".postln;
});
```

Odota ensin `LT_READY`-markeri ja sen jälkeen boot-fade; tarkista, ettei konsolissa
ole virheitä. Nosta monitoritaso vasta tämän jälkeen hiljaiseen tarkistustasoon.
Älä aja `CQ.free; CQ.boot` samassa tulkkausblokissa: suorita `CQ.free`, odota
`Cocoquantus: freed.` ja varmista vanhan session hiljaisuus/resurssien poistuminen,
ja käynnistä vasta sitten uusi boot omana komentona. Tämä on pakollinen varotoimi
CQ-AUD-003/-004:n vuoksi. Kirjaa vaiheiden välissä käytetty palautustapa.

## 3. Arviointimenetelmä

### 3.1 Kolme hyväksyntälajia

- **H — kova tekninen ehto:** mitattava turvallisuus, tiedoston eheys, kanavajärjestys, hallinnan toiminta tai resurssikäytös. Yksi epäonnistuminen hylkää vaiheen.
- **B — käyttäytymisehto:** projektin dokumentoiman musiikillisen syy-seuraussuhteen tulee kuulua toistettavasti, esimerkiksi speedin tulee muuttaa yhtä aikaa pitchiä, pituutta ja rouhetta.
- **E — esteettinen arvio:** kuvaileva havainto, ei yksin pass/fail. Käytä sitä vertaamaan muutoksia, presettejä ja Space-vaihtoehtoja.

Historiallista autenttisuutta ei hyväksytä pelkällä “kuulostaa oikealta” -arviolla.

### 3.2 Kuuntelijoiden ja toistojen määrä

- Käyttäytymisvaiheet: vähintään kaksi toistoa eri bootissa.
- Preset-järjestys, Space ja mahdolliset DSP-muutosten A/B:t: mieluiten kaksi kuuntelijaa tai yksi kuuntelija vähintään kolmessa satunnaistetussa toistossa.
- A/B-vertailussa tasaa havaittu loudness ennen arviointia. Älä tulkitse kovempaa automaattisesti paremmaksi.
- Kuuntelija ei saa nähdä A/B-vaihtoehdon nimeä silloin, kun arvioidaan esteettistä paremmuutta.

### 3.3 Asteikot

Arvioi E-kohdissa 1–5:

- muistimaisuus: staattinen efekti 1 — jatkuvasti uudelleenkirjoittuva muisti 5
- karheus: geneerinen bitcrusher 1 — kerroksittain kehittyvä ja kelloon sidottu 5
- emergenssi: ennustettava LFO 1 — hallittavasti yllätyksellinen verkko 5
- soitettavuus: vaikeasti tulkittava 1 — eleeseen selvästi vastaava 5
- avoimuus: valmis preset-efekti 1 — uudelleen patchattava instrumentti 5
- studiomainen pinta: raaka/rakenteellinen 1 — kiillotettu/irrallinen 5
- rasittavuus: helppo pitkäkuuntelu 1 — nopeasti rasittava 5.

Asteikot ovat vertailutyökaluja, eivät absoluuttinen fidelity-todistus.

## 4. Vaiheittainen protokolla

## LT-00 — konfiguraatio, boot, panic ja hiljainen baseline

**Tavoite:** varmistaa turvallinen lähtötila ennen kuultavia ääritestejä.

**Toimet:**

1. Tarkista kanavien erillisyys kohdan 1.2 mukaan.
2. Boottaa vakiolähtötilaan fyysinen taso minimissä.
3. Varmista kahden sekunnin fade-in ja ettei suoraa käsittelemätöntä inputtia kuulu.
4. Avaa hiljainen deck-materiaali ja nosta fyysistä monitoria vain mukavalle tarkistustasolle.
5. Aja `CQ.panic`, varmista mykistys, sitten `CQ.resume`.
6. Aja `CQ.free` ja varmista, että audio loppuu eikä nodeja jää kuuluviin.
7. Toista boot/free kolme kertaa.

**Hyväksymiskriteerit:**

- H: ei virheitä, odottamatonta suoraa inputtia tai ääntä free-komennon jälkeen
- H: PANIC mykistää ja RESUME palauttaa saman tilan ilman tasohyppyä
- B: power-on dust voi olla kuultavissa vain Cocon oman tason kautta; se ei saa peittää hallittua testilähdettä oletustasoilla.

## LT-01 — Cocon puolten itsenäisyys

**Tavoite:** osoittaa, että Coco 1 ja Coco 2 ovat itsenäisiä muistisoittimia eivätkä yhden stereoefektin kanavapari.

**Toimet:**

1. Lataa puhe deck A:han ja rytmimateriaali deck B:hen.
2. Aseta molemmat Cocos record-tilaan, Space ja crossfeed pois.
3. Hiljennä Coco 2 ja tallenna 30 s Coco 1:n speed-, feedback- ja hold-eleitä.
4. Hiljennä Coco 1, palauta Coco 2 kuuluviin ja tee eri speed-/hold-eleet.
5. Palauta molemmat kuuluviin, panoroi ne erilleen ja vertaa muistien sisältöä.
6. Muuta vain Coco 1:n speediä ja varmista, ettei Coco 2:n pitch tai loopin pituus seuraa.
7. Toista roolit vaihtaen.

**Hyväksymiskriteerit:**

- H: toisen Cocon parametrimuutos ei muuta toisen state-arvoa
- B: muistien sisältö, pitch, loopin pituus ja hold voidaan erottaa toisistaan
- B: crossfeed syntyy vain, kun slotX avataan eksplisiittisesti
- E: arvioi epäsymmetria, kerrostettavuus ja stereoidentiteetti.

## LT-02 — Cocon clocked-RAM-identiteetti

**Tavoite:** arvioida speed/pituus/pitch/fideliteetti, feedback-uudelleenkirjoitus, hold, Flip, Skip, dust, bits, wrap, interpolation, dolby ja SP.AF.

**Lähtötila:** yksi Coco kuuluviin, toinen nollaan; Space, dry, crossfeed, Quantussy-slot ja matriisi pois. Käytä transientti- ja puheaineistoa.

**Osakokeet:**

1. **Speed:** nauhoita fraasi, lukitse hold ja käy järjestyksessä 48000 → 24000 → 12000 → 3000 → 24000 Hz. Mittaa loopin periodi ja pitch jokaisessa kohdassa.
2. **Hold:** tallenna sama hold-looppi ennen ja jälkeen vähintään viiden minuutin toiston. Vertaa äänenvoimakkuutta, spektriä ja korrelaatiota. Muuta speediä holdin aikana ja palaa alkuarvoon.
3. **Feedback:** record-tilassa kokeile 0,9 → 1,0 → 1,15, korkeintaan 10–15 s kutakin. Arvioi, kehittyykö jokainen kierros eikä vain vahvistu.
4. **Flip/Skip:** käytä tunnistettavaa puhefraasia; varmista suunnan vaihto ja vaihe-reset sekä holdissa että record-tilassa.
5. **Dust:** sulje muut input-slotit, aseta feedback 1,1 ja nosta dust 0,02 → 0,3. Lukitse muodostunut tekstuuri ja muuta speediä.
6. **Bits:** vertaa 12, 8 ja 3 bittiä samalla lyhyellä loopilla. Kirjaa, muuttuuko kierros kierrokselta vai vain pintakohinana.
7. **Wrap:** vertaa clip- ja wrap-tilaa vain alhaisella fyysisellä monitoritasolla ja lyhyesti. Kirjaa polariteettikäännökset/kovuus.
8. **Interpolation:** sokkoutettu ZOH/smooth-A/B samalla hold-loopilla.
9. **dolby:** käytä puheen ja iskujen kerroksia; vertaa off, gate ja duck erikseen input- ja feedback-haarassa.
10. **SP.AF:** patchaa yksi hidas triangle Cocon speediin, testaa positiivinen ja negatiivinen paino sekä `cocoNSpaf` 0 → pieni → suuri.

**Hyväksymiskriteerit:**

- H: ei runaway-ulostuloa, NaN/inf-arvoa, pysyvää hiljenemistä tai > −0,75 dBFS tallennehuippua
- B: speed muuttaa samanaikaisesti pitchiä, loopin pituutta ja korkeiden taajuuksien/aliasoinnin luonnetta
- B: hold ei itsessään degradoudu; speedin palautus palauttaa loopin ilman muistisisällön tuhoutumista
- B: feedback > 1 kasvattaa sisäistä rouhetta mutta pysyy ulostulolta rajattuna
- B: Flip ja Skip ovat fraasissa yksiselitteisesti kuultavia
- B: dust toimii itsenäisenä muistilähteenä
- E: ZOH:n tulee arvioitua selvemmin kellotetuksi/raakaksi kuin smooth; smoothin ei tarvitse hävitä vertailua, koska se on tietoinen lisävaihtoehto.

## LT-03 — Quantussy-makrot ja castleverkon käyttäytyminen

**Tavoite:** arvioida vakaa → vaeltava → hyperkaoottinen jatkumo, castlen alueet, Spesal ja S&H-slew.

**Toimet:**

1. Tyhjennä matriisi ja kuuntele qMix suoraan yhden Cocon slotin kautta tai turvallisesti audiona.
2. Aseta `qChaos = 0`. Käy `qFreq`-arvot 0,5, 4, 50 ja 500 Hz; kirjaa vakaus ja pitch/rate.
3. Aseta `qChaos` järjestyksessä 0,1, 0,4 ja 1,0, korkeintaan 15 s maksimissa.
4. Vaihda yksi castle järjestyksessä infra → balcium → audio ja pidä muut muuttumattomina.
5. Aja yhden castlen `CQ.spesal(k)` kahdeksan kertaa, alkaen tunnetusta `CQ.spesalSet(k, 0)` -tilasta. Kirjaa jokainen tila.
6. Vertaa `qShLag` 0, 0,004 ja 0,08 s samalla tunnetulla tilalla.
7. Toista koko koe toisessa bootissa samoilla parametreilla. Erottele deterministinen alkutila ja emergentti kehitys.

**Hyväksymiskriteerit:**

- H: kaikki arvot pysyvät äärellisinä ja ulostulo turvallisuusrajan sisällä
- B: chaos 0 on selvästi vakaampi kuin 0,4; maksimi saa sisältää purskeita ja hiljaisuuksia ilman hallinnan menetystä
- B: range-vaihto siirtää castlen aidosti infra-, väli- ja audiokäyttöön
- B: Spesal muuttaa partneri-/moodikäyttäytymistä eikä ole näennäinen trigger
- B: `qShLag` kasvattaessa askelten tulee muuttua kuultavasti liukuvammiksi
- E: arvioi emergenssi, liiallisen tasoituksen tunne ja se, kuulostaako verkko yhdeltä LFO:lta vai keskinäiseltä järjestelmältä.

## LT-04 — matriisin avoimuus, polariteetti ja gate/triggerit

**Tavoite:** varmistaa, että nimetty matriisi toimii audio/CV-patchbayna eikä vain preset-valitsimena.

**Toimet:**

1. `CQ.clearPatch`; varmista, että kaikki matriisivaikutukset katoavat pehmeästi.
2. Lisää yksi tri → Coco SP.AF -reitti painoilla +0,25, +1, −0,25 ja −1. Kirjaa suunnan ja syvyyden vaikutus.
3. Lisää yksi env → input- tai feedback-VCA -reitti positiivisena ja negatiivisena. Käytä puhetta/iskua.
4. Testaa yksi hidas tri → Flip ja tri → Skip. Varmista Schmitt-käytös ilman trigger-chatteria.
5. Testaa cas → castle FM ja tri → Spesal erikseen.
6. Aseta `patchDepth` 1 → 0 → 1 aktiivisessa mutta maltillisessa patchissa.
7. Aja `CQ.shake(0.3, seed: 1234)`, kirjaa patch, tyhjennä ja toista samalla seedillä. Tee sitten eri seed.
8. Tee yksi kaksisuuntainen Coco-cross-modulaatio maltillisilla painoilla ja arvioi emergentti käyttäytyminen.

**Hyväksymiskriteerit:**

- H: kaikki painot clampaantuvat, eikä yksittäinen reitti ohita turvaa
- B: positiivinen ja negatiivinen paino tuottavat vastakkaisen tai inverso-tyyppisen vaikutuksen kohteen semantiikan mukaan
- B: `patchDepth = 0` poistaa matriisin vaikutuksen ilman vanhojen yhteyksien tuhoamista
- B: sama shake-seed tuottaa saman patchin ja eri seed yleensä eri patchin
- B: gate/trigger ei chatteroi kynnyksen ympärillä
- E: arvioi, tuntuuko patchaus avoimelta instrumentilta ja säilyykö yksittäisen yhteyden syy-seuraus.

## LT-05 — patch-presetien järjestys ja snapshot-morph

**Tavoite:** erottaa presetin oma ääni edellisen tilan jäämistä ja arvioida, säilyykö avoimen instrumentin luonne.

**Testattava presetjärjestys:**

`empty → classic → selfNoise → quantumDrone → organMode → mayhem`

**Toimet:**

1. Tee jokaiselle presetille **puhdas ajo**: uusi boot, sama testitiedosto, sama 30 s aikajana, preset päälle vasta aikajanan kohdassa 5 s. Tallenna 5 s ennen ja 25 s jälkeen.
2. Tee **ketjutettu ajo** yllä olevassa järjestyksessä ilman boottia; kuuntele kutakin 30 s.
3. Tee ketjutettu ajo käänteisessä järjestyksessä.
4. Palaa jokaisen välin jälkeen `empty`-presetiin ja varmista vain sen nykyinen,
   varmistettu sopimus: matriisi tyhjenee. Tallenna kaikki state-residuaalit. Älä
   käytä `empty`ä turvallisena resetinä tai puhtaan baselinen korvikkeena.
5. Vertaa saman presetin puhdasta, eteenpäin ketjutettua ja taaksepäin ketjutettua tallennetta loudness-tasattuna.
6. Tallenna snapshot A tunnetusta `classic`-tilasta ja snapshot B muokatusta epäsymmetrisestä tilasta.
7. Recall A ja B kaksi kertaa; varmista state, patchit ja Spesal.
8. Morphaa A → B 30 sekunnissa, keskeytä kerran, recall A kesken toisen morphin ja tarkista turvallinen paluu.
9. Varmista, ettei tavallinen snapshot avaa mikkiä; `micGain` pysyy nollassa ilman eksplisiittistä includeMic-valintaa.

**Hyväksymiskriteerit:**

- H: presetvaihto, recall tai morph ei aiheuta click-/tasopiikkiä yli turvallisuusrajan
- H: snapshot ei avaa mikrofonia implisiittisesti
- B: `empty` palauttaa matriisin aidosti tyhjäksi
- B: jos tuotepäätös on **deterministinen sound-preset**, saman presetin koko
  dokumentoitu parametri- ja matriisitila on sama riippumatta järjestyksestä
- B: jos tuotepäätös on **overlay-resepti**, residuaalit vastaavat täsmälleen
  dokumentoitua overlay-sopimusta ja erillinen Reset palauttaa deterministisen,
  turvallisen baselinen
- B: recall palauttaa kuultavan ja näkyvän tilan; morph saavuttaa B:n lopputilan
- E: jokaisella presetillä on erotettava tarkoitus, mutta vähintään `empty` ja manuaalinen patchaus tuntuvat täysivaltaisilta käyttötavoilta
- E: `mayhem` ei saa määrittää koko instrumentin identiteettiä vain suuremman loudnessin vuoksi.

Nykyinen toteutus on jo osoitettu state-järjestysriippuvaiseksi: selfNoise →
classic jätti 13 ja quantumDrone → empty 20 residuaalia (CQ-AUD-007). Ennen
deterministinen-vs-overlay-tuotepäätöstä tämä kohta ei voi saada yleistä PASSia.
Jos tallenteet eroavat state-arvojen ollessa aidosti samat, kirjaa vasta silloin
ero mahdollisena muistihistoriana; erota se sparse preset -datasta tallentamalla
state ja RAMin muodostumisvaihe.

## LT-06 — Space: kuiva ydin, nykyinen FDN ja konseptivaihtoehdot

**Tavoite:** arvioida nykyisen Spacen suhdetta Coco-ytimeen ilman oletusta, että reverb kuuluu alkuperäiseen estetiikkaan.

**Toimet nykyversiolla:**

1. Luo kolme erilaista lähdettä: lyhyt percussive Coco-looppi, pitkä feedback/dust-tekstuuri ja Quantussylla moduloitu looppi.
2. Tallenna jokaisesta kuiva referenssi: `cocoNSend = 0`, `spaceMix = 0`.
3. Tallenna nyky-FDN samoilla eleillä seuraavilla pisteillä:
   - pieni: send 0,25, mix 0,15, size 0,5
   - oletusluonne: send 0,5, mix 0,3, size 0,85, damp 4500
   - ääripää: send 0,7, mix 0,5, size 0,98, lyhyenä jaksona.
4. Satunnaista dry/FDN-tallenteet, loudness-tasaa ja arvioi sokkona.
5. Kirjaa erikseen transientin luettavuus, ZOH-rouheen säilyminen, metallisuus, stereokuvan leveneminen, tilan riippuvuus lähteestä ja “bolt-on studio effect” -vaikutelma.

**Jos myöhemmin on prototyyppejä**, vertaa samalla materiaalilla:

- A: kuiva
- B: nykyinen FDN
- C: kontekstitietoinen Coco/Quantussy-makro
- D: ulkoinen host-reverb.

**Hyväksymiskriteerit nykyversiolle:**

- H: Space vaimenee kaikilla sallituilla arvoilla, ei kerää DC:tä eikä ylitä masterin turvallisuusrajaa
- B: `spaceMix = 0` on aidosti kuiva ja oletusbootissa Space ei kuulu
- E: nyky-FDN voidaan hyväksyä valinnaisena C-luokan efektinä, vaikka se ei voittaisi kuivaa fidelity-arviota
- E: kontekstuaalinen korvaaja hyväksytään vain, jos vähintään kaksi kolmesta sokkotoistosta arvioidaan yhtä soitettavaksi ja vähemmän irralliseksi ilman ydinkarheuden peittymistä.

Spacea ei tule muuttaa pelkän yhden märän demon perusteella.

## LT-07 — linked stereo: kanavaidentiteetti ja käynnistyssynkronia

**Tavoite:** tarkistaa dokumentoitu L → deck A, R → deck B -pari, kanavien stereosisältö ja näytetarkka aloitus.

**Toimet:**

1. Käytä kohdassa 2.2 määritettyä stereo-identiteettitiedostoa.
2. Tee synkronian **tekninen testi erillisellä offline/live-harnessilla suoraan
   deck A/B -synthien ulostuloista ennen Coco-RAMia**. Nollaa testibussit, käynnistä
   capture ennen `CQ.loadStereo`-kutsua ja käytä identtistä stereoimpulssia.
   Power-on-RAMin kohinaa tai Cocon record-viivettä sisältävästä masterista ei saa
   päätellä deckien start-offsetia.
3. Toista tekninen impulssicapture vähintään kymmenen latausta. Mittaa ensimmäinen
   ei-nollanäyte ja varmista tulos ristikorrelaatiolla.
4. Tee erillinen kuuntelu-/kanavaidentiteettiajo: puhdas ready-boot, Space,
   crossfeed, qMix, pre, dust ja feedback pois; molemmat Cocos record-tilaan,
   samalle nopeudelle ja bits/interp-asetukselle. Panoroi Coco 1 vasemmalle ja
   Coco 2 oikealle, lataa stereo-identiteettitiedosto ja tallenna vähintään yksi
   täysi RAM-kierros.
5. Spektrianalyysillä tarkista L/deck A/Coco 1- ja R/deck B/Coco 2 -sisältö.
   Tämä ajo todentaa reitin mutta ei näytetarkkaa deck-starttia.
6. Testaa erikseen `masterDry = 1` Cocosignaalit nollassa ja tarkista, säilyttääkö dry-monitorointi linked-parin L/R-identiteetin.
7. Testaa linked-rate-ohjaus MIDI F12:lla ja varmista, että molemmat rate-parametrit seuraavat dokumentoidusti. Ilman 16n:ää aseta `deckARate` ja `deckBRate` eksplisiittisesti samaan arvoon; yksittäisen `CQ.deckA.rate_`-kutsun ei oleteta linkittävän deck B:tä.

**Hyväksymiskriteerit:**

- H: ei latausvirhettä, orphan-soitinta tai turvallisuusrajan ylitystä
- B: L-materiaali päätyy deck A/Coco 1 -polkuun ja R deck B/Coco 2 -polkuun
- B: deck-bus-impulssien aloitusero on 0 samplea jokaisella kymmenellä latauksella
- B: linked dry-monitorointi säilyttää L/R-sisällön eikä muuta paria dual monoksi
- B: linked rate muuttaa molempia samassa eleessä.

Nykyinen koodi käynnistää kaksi asynkronista deck-latausta erikseen ja rakentaa dry-bussin mono-deckien stereo-duplikaateista. Siksi tämä vaihe on erityisen tärkeä, eikä hyväksyntää saa merkitä pelkällä “stereo kuuluu” -havainnolla.

## LT-08 — recorder ja tiedostoeheys

**Tavoite:** varmistaa turvallinen post-master-tallennus, toistuvat start/stopit, uniikit tiedostot ja oikea formaatti.

**Toimet:**

1. Käytä tavallista stereo-recording-konfiguraatiota (`recStems: false`) ja
   aseta `folders.recordings` eksplisiittisesti uuteen, kertakäyttöiseen
   testihakemistoon ennen boottia. Varmista recorderin konsoliviestistä ratkaistu
   absoluuttinen polku ja pidä kaikki tärkeät tallenteet pois tästä hakemistosta.
2. Aja `CQ.rec`, soita tunnettu 20 s jakso ja `CQ.recStop`.
3. Varmista tiedoston nimi, olemassaolo, 24-bit WAV, kaksi kanavaa, odotettu kesto ja ettei edellistä tiedostoa ylikirjoitettu.
4. Toista viisi normaalia start/stop-sykliä.
5. Tee yksi lyhyt panic/resume tallennuksen aikana; mykistyksen tulee tallentua masteriin.
6. Tee yksi `CQ.free` tallennuksen aikana ja varmista, että tiedosto sulkeutuu luettavaksi eikä recorder-node/buffer jää eloon.
7. Erillisessä hallitussa ajossa paina/kutsu startia kaksi kertaa nopeasti. Älä käytä korvaamatonta lähdemateriaalia. Kirjaa konsoli, syntyneet tiedostot sekä node-/buffer-count ennen ja jälkeen.
8. Analysoi jokaisesta tiedostosta peak, DC, NaN/inf ja kanavamäärä.

**Hyväksymiskriteerit:**

- H: yksikään tiedosto ei ylikirjoitu tai korruptoidu
- H: vain yksi recorder on aktiivinen; kaksois-starti ei luo orphan-nodea/bufferia tai kahta kirjoittajaa samaan polkuun
- H: jokainen valmis tiedosto avautuu, on oikean pituinen/kanavainen ja huippu ≤ −0,75 dBFS
- H: stop/free vapauttaa recorder-resurssit
- B: tallenne vastaa kuultua post-master-signaalia, mukaan lukien panic.

Kaksois-startilla on kooditarkastuksen perusteella kilpailutilariski. Testaa se
monitoritaso alhaalla vain yllä konfiguroituun tyhjään kertakäyttöhakemistoon,
ei tärkeän session aikana.

## LT-09 — stemit ja nelikanavainen stem-recording

**Tavoite:** todentaa erilliset post-safety Coco-stemit turvallisella vähintään nelikanavaisella laitteella sekä sisäinen nelikanavatallennus.

**Fyysiset stemit:**

1. Varmista neljä itsenäistä ulostuloa. Käytä vain disjoint-määritystä, esimerkiksi master `[0,1]`, stemit `[2,3]`.
2. Poista DAW:n input monitoring ja kaikki stemien paluureitit masteriin.
3. Aloita fyysiset stem-lähdöt minimigainilla ja mittaa ne ennen kuuntelua.
4. Soita vain Coco 1, sitten vain Coco 2, sitten molemmat; varmista kanavaidentiteetti.
5. Lisää Space masteriin. Stemien tulee pysyä kuivina post-Coco-tappeina määritelmän mukaan.
6. Muuta masterVol. Stemien ei tule seurata masterVolia, mutta monitorMute/panicin tulee mykistää ne.
7. Mittaa kummankin stemin peak, DC ja NaN/inf.

**Nelikanavainen disk recording:**

1. Aseta `recStems: true`; fyysisiä `stemOuts`-kanavia ei tarvita tähän osaan.
2. Tallenna vaihe, jossa Coco 1 ja Coco 2 ovat vuorotellen ja yhdessä aktiivisia.
3. Varmista tiedoston kanavat: master L, master R, Coco 1 stem, Coco 2 stem.
4. Tarkista kanavien sample-pituus, synkronia, peak, DC ja sisältö.

**Hyväksymiskriteerit:**

- H: fyysiset master- ja stem-kanavat ovat kaikki eri busseja
- H: jokainen stem käy oman turvaketjunsa läpi ja peak ≤ −0,75 dBFS
- H: panic mykistää masterin ja stemit; masterVol ei muuta stem-tasoa
- B: stem 1 sisältää vain Coco 1:n ja stem 2 vain Coco 2:n ennen mahdollista ulkoista reititystä
- B: nelikanavatiedoston master ja stemit ovat samassa sample-aikajanassa
- H: ei loopbackia tai limiterin jälkeistä summausta.

Jos käytössä ei ole vähintään neljää fyysistä ulostuloa, merkitse live-stem-osuus **estynyt**, älä improvisoi kanavien päällekkäisyyttä.

## LT-10 — MIDI/16n, jump, pickup ja ulkoinen state-muutos

**Tavoite:** arvioida fyysisen ohjaimen kartoitus, kaksisuuntainen tilaseuranta ja turvallinen takeover.

**Toimet:**

1. Boottaa mikki nollassa ja tarkista 16n-yhteys. Tarvittaessa aja `CQ.midiRescan`.
2. Liikuta kukin F1–F16 yksitellen ääriarvoihin ja keskelle. Varmista GUI:sta ja `CQ.get`-kutsuilla, että vain dokumentoitu parametri muuttuu ja arvo clampaantuu.
3. Erityisesti tarkista speedin eksponentiaalinen tuntuma, feedbackin unity-alue, Space-mix ja linked deck rate.
4. `jump`-tilassa tee pieniä eleitä ja tarkkaile, syntyykö vaarallista tasohyppyä tai clickiä.
5. Tallenna snapshot, muuta parametri koodista, recall snapshot ja liikuta vastaavaa faderia. Testaa sekä jump että pickup.
6. Pickup-tilassa faderi ei saa muuttaa arvoa ennen kuin fyysinen asema ylittää nykyisen arvon. Kun arvo muuttuu GUI:sta, koodista tai snapshotista, pickup tulee aseistaa uudelleen.
7. Irrota/kytke MIDI-laite testien välissä, aja `CQ.midiRescan` ja varmista, ettei audio/serveri kaadu.
8. Varmista, ettei MIDI:n puuttuminen estä muun instrumentin käyttöä.

**Hyväksymiskriteerit:**

- H: yksikään faderi ei kirjoita väärää parametria tai clampin ulkopuolelle
- H: MIDI ei avaa mikkiä tai ohita panic/mutea
- B: jump seuraa välittömästi mutta server-side lag estää clickit
- B: pickup ottaa kiinni vain arvon ylityksessä ja aseistuu uudelleen ulkoisen muutoksen jälkeen
- B: linked rate ohjaa molempia deckejä dokumentoidusti
- H: rescan tai laitteen puute ei pysäytä audiojärjestelmää
- E: kirjaa faderin käyttökelpoinen resoluutio erityisesti speedin, feedbackin unity-kohdan ja patchDepthin ympärillä.

## LT-11 — sample rate- ja block size -matriisi

**Tavoite:** arvioida dokumentoitu 44,1/48/96 kHz -tuki ja tarkoituksellisten yhden blokin feedback-reittien kuultava herkkyys.

**Konfiguraatiot:**

| Ajo | Sample rate | Block size |
|---|---:|---:|
| SR1 | 44100 | 64 |
| SR2 | 48000 | 64 |
| SR3 | 96000 | 64 |
| BS1 | 48000 | 16 |
| BS2 | 48000 | 64 |
| BS3 | 48000 | 256 |

Laajenna 3×3-matriisiin vain, jos yllä oleva seulonta löytää eroja. Käytä samaa
config-pohjaa ja muuta vain sample rate/block size. CQ-AUD-003:n vuoksi
`CQ.reboot` ei kelpaa tämän testin sessionvaihdoksi nykyversiossa. Samassa
kielisessiossa järjestys on: `CQ.free` → odota `Cocoquantus: freed.` →
`s.quit` → odota scsynth-prosessin poistuminen → muuta config → uusi
`CQ.boot(onReady: ...)`. Turvallisempi vaihtoehto on uusi sclang-prosessi
jokaiselle ajolle. Älä tee pelkkää ulkoista `s.quit` + `CQ.boot` -paria vanhaan
facadeen, koska CQ-AUD-018 voi jättää sen bootatuksi. Mittaa toteutunut sample
rate ja block size serveriltä/one-shot `SampleRate.ir` + `ControlDur.ir`
-probella; älä hyväksy pelkkää config-arvoa.

**Vakioitu minisarja jokaisessa ajossa:**

1. sama hold-looppi speed-arvoilla 3000, 12000, 24000 ja sallittu maksimi
2. feedback 0,9/1,0/1,15 lyhyesti
3. sama tunnettu Quantussy-tila chaos-arvoilla 0/0,4/1
4. sama kaksisuuntainen Coco-crossfeed-patch
5. sama `classic`-preset
6. sama lyhyt Space-impulssivaste
7. 60 s recording ja CPU-/xrun-/node-/buffer-loki.

**Analyysi:**

- Vertaa loopin periodia sekunteina suhteessa toteutuneeseen clockHziin.
- Pitch-seuraa sama kohta.
- Loudness-tasaa tallenteet ennen esteettistä vertailua.
- Kirjaa yhden blokin reittien absoluuttinen viive: `blockSize / sampleRate`.
- Vertaa Quantussyn tapahtumatiheyttä, crossfeedin vakautta, Space-decayta, CPU:ta ja dropouteja.

**Hyväksymiskriteerit:**

- H: kaikki kuusi konfiguraatiota boottaavat, tallentavat ja sulkeutuvat ilman crashia, xruneja, NaN/inf-arvoja tai turvallisuusrajan ylitystä
- B: speed/pitch/loop-suhde pysyy selitettävänä jokaisella sample ratella; kellon SR-clamp on ainoa odotettu ylärajaero
- B: hold säilyy ja feedback pysyy rajattuna
- B: Quantussy ja crossfeed eivät vaihda kvalitatiivisesti toimivasta verkosta pysyvästi mykäksi tai periodiseksi pelkän tuetun konfiguraation vuoksi
- E: kuultavat erot kirjataan, mutta niitä ei “korjata” automaattisesti; yhden blokin viive on suojattava rakenne, kunnes vaihtoehto voittaa sokkokuuntelun ja regressiot.

## LT-12 — pitkä abuse-, resurssi- ja palautumistesti

**Tavoite:** löytää pitkän soiton aikana node-/buffer-vuodot, tallennus- ja latauskilpailut, tilan korruptio, CPU:n kasvu sekä äänellisen hallinnan katoaminen.

**Kesto:** vähintään 60 minuuttia. Pidä fyysinen monitoritaso matalana; osan ajosta voi monitoroida mittareilla tai tallentaa kuuntelematta jatkuvasti.

**Aikajana:**

1. 0–10 min: kaksi deckiä, molemmat Cocos record/hold-vaihdoin, speed-eleitä.
2. 10–20 min: feedback 0,9–1,15, dust, Flip/Skip ja dolby. Maksimiarvot vain lyhyesti.
3. 20–30 min: Quantussy 0–1 chaos, Spesal, useita matriisireittejä, patchDepth 0/1.
4. 30–40 min: käy presetit järjestyksessä, snapshot recall/morph ja kolme deck load/next/prev -vaihtoa.
5. 40–50 min: Space maltillisena, recorder start/stop vähintään viisi kertaa; yksi panic/resume.
6. 50–55 min: `mayhem` enintään 60 s kerrallaan; tarkkaile limiteriä ja CPU:ta.
   Älä käytä `empty`ä kesytys- tai turvallisuusresetinä. Jokaisen jakson jälkeen
   paina PANIC, tyhjennä matriisi, aseta molemmat Hold-arvoon 1, SlotX/SlotQ/Spaf
   nollaan, feedback 0,35:een, dust 0,02:een, Space ja masterDry nollaan sekä
   masterVol -40 dB:iin; RESUME vasta fyysisen tason tarkistuksen jälkeen.
7. 55–60 min: palauta turvallinen tila, lopeta recorder, `CQ.free`, tarkista resurssit ja syntyneet tiedostot.

Ota node-, buffer-, CPU-, xrun- ja muistilukema vähintään kohdissa 0, 10, 20, 30, 40, 50 ja 60 min. Kirjaa jokainen konsolivaroitus aikaleimalla.

**Hyväksymiskriteerit:**

- H: node- ja buffer-määrä palaa lähtötasolle `CQ.free`:n jälkeen
- H: resurssimäärä ei kasva monotonisesti deck-latausten tai recorder-syklien mukana
- H: ei crashia, xrun-sarjaa, korruptoitunutta/yli kirjoitettua tiedostoa, NaN/inf-arvoa tai pysyvää hiljaisuutta
- H: jokaisen tallenteen peak ≤ −0,75 dBFS ja `|DC| < 0,02`
- B: panic/resume, presetit ja morph pysyvät hallittavina myös kuormassa
- B: eksplisiittinen yllä kuvattu safe-state-palautus kesyttää patchin ilman, että
  sparse `empty`-overlaylle annetaan reset-vastuuta
- E: arvioi 60 minuutin jälkeen, säilyikö instrumentti soitettavana vai ajautuiko se geneeriseen jatkuvaan kohinaan/limiteriin.

Jos testi kaatuu tai jää äänellisesti mykäksi, säilytä viimeinen loki, config, preset/snapshot, recorder-tiedosto ja täsmällinen aikajana. Älä jatka samassa session tilassa “katsoaksesi palaako se”.

## 5. Tuloslomakkeet

## 5.1 Session perustiedot

```text
Session ID:
Päivä ja kellonaika:
Testaaja(t):
Commit/hash:
Työpuun muutokset:
SuperCollider-versio:
Käyttöjärjestelmä:
Äänikortti / ajuri:
Output-kartta (master):
Output-kartta (stemit tai nil):
Vahvistus: kaikki fyysiset outputit uniikkeja ja disjoint [ ]
Sample rate / block size / hardware buffer:
Kuuntelulaite ja fyysinen tasoasetus:
MIDI-laite / firmware tai ei käytössä:
Testitiedostot + checksumit:
Config-kopio / tunniste:
```

## 5.2 Vaihekohtainen tulos

Täytä yksi lomake jokaista LT-vaihetta ja uusintaa kohti:

```text
Test ID / toisto:
Alkutila (fresh boot / palautus):
Tarkat komennot ja parametrit:
Syöte:
Alkuaika / loppuaika:
Tallenne- tai lokipolku:

Peak L/R/stemit:
RMS L/R/stemit:
DC L/R/stemit:
NaN/inf:
CPU min/mediaani/max:
Node/buffer ennen/jälkeen:
Xrunit / konsolivaroitukset:

H-kriteerit: PASS / FAIL / BLOCKED
B-kriteerit: PASS / FAIL / BLOCKED
E-havainnot:
Poikkeaman ensimmäinen aikaleima:
Toisto-ohje:
Lisäevidenssi:
Testaajan allekirjoitus/päiväys:
```

## 5.3 A/B- tai A/B/C-kuuntelulomake

```text
Vertailun ID:
Hypoteesi ennen testiä:
Vaihtoehtojen todelliset nimet (testin vetäjälle):
Satunnaistettu esitysjärjestys:
Loudness-tasausmenetelmä ja ero:
Kuuntelija ei nähnyt vaihtoehtojen nimiä [ ]

Näyte __:
  muistimaisuus 1–5:
  kelloon sidottu karheus 1–5:
  emergenssi 1–5:
  soitettavuus 1–5:
  avoimuus 1–5:
  studiomainen pinta 1–5:
  rasittavuus 1–5:
  vapaa kuvaus:

Pakotettu valinta + perustelu:
Luottamus valintaan 1–5:
Arvaus vaihtoehdon identiteetistä:
```

## 5.4 Preset-järjestyslomake

| Preset | Puhdas boot | Ketju eteen | Ketju taakse | State/matrix sama | Kuultava ero | PASS/FAIL |
|---|---|---|---|---|---|---|
| empty | | | | | | |
| classic | | | | | | |
| selfNoise | | | | | | |
| quantumDrone | | | | | | |
| organMode | | | | | | |
| mayhem | | | | | | |

Jos state ja matrix ovat samat mutta ääni eroaa, merkitse erikseen, selittyykö ero odotetulla RAM-historialla.

## 5.5 Sample-rate/block-size-lomake

| Ajo | SR | Block | Boot | Peak/DC/NaN | Hold | Quantussy | Crossfeed | CPU/xrun | E-huomiot | PASS/FAIL |
|---|---:|---:|---|---|---|---|---|---|---|---|
| SR1 | 44100 | 64 | | | | | | | | |
| SR2 | 48000 | 64 | | | | | | | | |
| SR3 | 96000 | 64 | | | | | | | | |
| BS1 | 48000 | 16 | | | | | | | | |
| BS2 | 48000 | 64 | | | | | | | | |
| BS3 | 48000 | 256 | | | | | | | | |

## 6. Kokonaishyväksyntä

Kuuntelu- ja käyttöauditointi voidaan merkitä suoritetuksi vasta, kun:

1. LT-00…LT-12 on merkitty PASS, tai estyneelle laitekohtaiselle vaiheelle on kirjattu syy ja erillinen myöhempi omistaja/päivä.
2. Yksikään H-ehto ei ole avoimesti epäonnistunut.
3. Linked stereon kanavaidentiteetti ja käynnistyssynkronia on mitattu, ei vain kuunneltu.
4. Recorderin kaksois-start, toistuva start/stop ja free tallennuksen aikana on todennettu ilman korvaamatonta aineistoa.
5. Fyysiset stemit on testattu vain erillisillä kanavilla, tai vaihe on rehellisesti merkitty estyneeksi.
6. Sample-rate/block-size-seulonta ja 60 minuutin abuse on suoritettu.
7. Coco-, Quantussy-, matrix-, preset- ja Space-havainnoista on tallenteet sekä täytetyt E-lomakkeet.
8. Kaikki failit on siirretty löydökseksi, jossa on tarkka toisto, evidenssi, vaikutus ja regressioriski.

Nykytila: **ei suoritettu tässä auditissa; mitään yllä olevista manuaalisista hyväksynnöistä ei saa päätellä automaattitestien perusteella.**
