# Cocoquantus- ja Ciat-Lonbarde-uskollisuusarvio

## Tila, rajaus ja evidenssin laatu

Tämä raportti arvioi projektin rakenteellista ja äänellistä suhdetta siihen Cocoquantus-kuvaan, jonka **tämä projekti itse** määrittelee. Auditissa ei löytynyt repositoriosta lähdebibliografiaa, URL-osoitteita, kaaviokopioita tai arkistoituja ulkoisia lähteitä. `SPEC.md:40–42` nimeää Peter Blasserin kuvauksen, julkaistun kaavion/Richard Brewsterin analyysin ja Perfect Circuitin artikkelin, mutta lähteitä ei voi tämän repositoryn aineistolla tarkistaa.

Siksi tässä raportissa käytetään aina seuraavaa jakoa:

1. **Paikalliseen projektidokumentaatioon perustuva väite** kertoo, mitä `SPEC.md`, `ARCHITECTURE.md`, nykyinen `docs/history/PLAN_v1.0.md`, `README.md` tai projektin testidokumentaatio väittää. Se ei ole tässä auditissa ulkoisesti varmennettu historiallinen fakta.
2. **Koodihavainto** kertoo, mitä nykyinen toteutus tekee tarkastettujen lähdekoodirivien perusteella.
3. **Esteettinen tulkinta** on auditoijan arvio todennäköisestä kuultavasta tai soitettavasta luonteesta. Sitä ei pidetä kuuntelutuloksena.
4. **Suositus** on suunnitteluehdotus, ei kuvaus alkuperäisestä laitteesta.

Äänellistä estetiikkaa ei vahvistettu tässä auditissa kuuntelemalla. Automaattiset NRT-testit vahvistivat turvallisuus- ja hold-ominaisuuksia, mutta eivät autenttista tuntumaa. Ihmisen suoritettava jatkotesti on kuvattu tiedostossa `audit/LISTENING_TEST_PROTOCOL.md`.

## Luokitus

- **A — dokumentoituun Cocoquantus-rakenteeseen perustuva:** paikallinen projektidokumentaatio liittää rakenteen suoraan tavoiteltuun Cocoquantus-käyttäytymiseen, ja koodi toteuttaa tämän rakenteen tunnistettavasti.
- **B — Ciat-Lonbarde-henkinen luova tulkinta:** rakenne jatkaa projektin kuvaamaa avoimen, keskinäisesti moduloivan instrumentin ajatusta, mutta tarkka toteutustapa on projektin oma tulkinta tai SuperCollider-sovitus.
- **C — tavanomainen tai irrallinen lisäys:** hyödyllinen työasema-, studio- tai turvallisuusominaisuus, jolla on heikko tai epäsuora suhde projektin kuvaamaan Cocoquantus-ytimeen.

Luokitus ei ole laatupisteytys. Erityisesti C-luokan turva- ja tallennusrakenteet voivat olla välttämättömiä, vaikka ne eivät lisää laiteuskollisuutta.

## Yhteenveto

Nykyinen DSP säilyttää projektin dokumentoiman ydinajatuksen hyvin: kaksi
itsenäistä kellotettua RAM-silmukkaa, nopeuteen sidottu pituus/pitch/fideliteetti,
paluussa uudelleen kvantisoituva muisti, identity-rewrite-hold, Flip/Skip,
digital dust sekä viiden castlen audiotaajuinen keskinäinen modulaatio ovat
toteutuksessa rakenteellisesti keskeisiä eivätkä päälle liimattuja efektejä.
Lossless/bit-exact hold on projektin vaatimus; koodi sekä samat peak-arvot,
korrelaatio 1,0 ja 0,0 dB drift tukevat sitä, mutta nykyinen A4-testi ei tee
sample- tai bittivertailua.

Toteutus ei pyri piirisimulaatioon, eikä sen pidäkään `SPEC.md:29–34`:n mukaan pyrkiä. Eniten projektin omaa tulkintaa ovat Quantussyn kahdeksan tilan tarkka partner/mode-kartta, address-counter-organin syntetisointi ja banaanipaneelin korvaava nimetty matriisi. Eniten ytimestä erillisiä ovat tiedostodekit, snapshotit, recorder, masterDry, watchdog ja nykyinen Space-FDN. Näistä Space on ainoa, jonka esteettinen rooli on myös projektin omassa suunnitelmassa nimenomaisesti auki (`docs/history/PLAN_v1.0.md:688–700`).

## A/B/C-kartta

| Osa | Luokka | Perustelu | Varmistuksen tila |
|---|---:|---|---|
| Kaksi itsenäistä Coco-RAMia | A | Kaksi erillistä puskuria ja kaksi saman Coco-SynthDefin instanssia | Koodista varmistettu |
| Speed → kellotaajuus, osoiteaskel, loopin pituus ja pitch | A | Kello ohjaa suoraan RAM-osoitteen etenemistä | Koodista varmistettu; kuultava tuntuma testaamatta |
| ZOH sisään/ulos ja aliasointi | A | Dokumentoitu ääni-identiteetin vaatimus ja ZOH-luku | Koodista varmistettu; auditiivinen laatu testaamatta |
| Feedbackin clip/wrap → quantize → RAM | A | Uudelleenkirjoittuva ja sisäisesti rajattu paluusilmukka | Koodista ja NRT-turvasta varmistettu |
| Hold, Flip, Skip | A | Rakenteet toimivat RAM-osoituksessa/kirjoituksessa | Koodista; hold myös NRT:llä varmistettu |
| Dolby-tilat, verso/inverso-VCA:t ja digital dust | A/B | Perustoiminnot seuraavat paikallista kuvausta; SC:n tarkat käyrät ovat tulkintaa | Koodista varmistettu; estetiikka testaamatta |
| Organ-ulostulo | B | Olemassaolo kuuluu A-tavoitteeseen, mutta pulse-stack/address-bit-johdos on eksplisiittinen tulkinta | Koodista varmistettu |
| Viisi keskinäisesti moduloivaa castlea | A/B | Viiden verkko ja S&H-periaate ovat A; tarkka 8-state partner/mode -kartta on dokumentoitu tulkinnaksi | Koodista varmistettu |
| Nimetty 16×22 signed-matriisi | B | Säilyttää audio/CV-avoimuutta ilman ruudun patch-kaapeleita | Koodista varmistettu |
| `patchDepth`, `shake`, presetit ja snapshot-morph | B/C | Soitettavuutta ja emergenssiä tukevia SC-laajennuksia; snapshotit ovat studio-ominaisuus | Koodista varmistettu; presetien state-järjestysriippuvuus varmistettu, kuultava vaikutus testaamatta |
| Coco-crossfeed ja slot-mikserit | B | Tukevat ristiinkytkentää ja epäsymmetriaa, mutta tarkka topologia on projektin oma | Koodista varmistettu |
| Stereo-pan ja linked file decks | C | Tavanomainen stereo-/tiedostotyöskentelykerros | Koodista varmistettu; linked-stereo-kuuntelu testaamatta |
| MasterDry, recorder, stemit ja watchdog | C | Studio-, monitorointi- ja mukavuusominaisuuksia | Koodista varmistettu; live-stemit testaamatta |
| LeakDC, tanh, limiter ja bad-value-squelch | C, suojattava | Turvallisuuskerros, ei autenttisuusefekti | Koodista ja NRT:llä osin varmistettu |
| Nykyinen nelilinjainen Space-FDN | C | Erillinen post-Coco-kaiku ilman yhteyttä Coco-kelloihin tai Quantussy-tilaan | Koodista varmistettu; estetiikka testaamatta |

## 1. Kaksi Coco-muistia ja niiden itsenäisyys — A

### Paikalliseen projektidokumentaatioon perustuva väite

`SPEC.md:44–64`, `SPEC.md:99–123` ja `SPEC.md:201–228` kuvaavat kaksi rinnakkaista 8-bittistä delay-/looperipuolta. Nopeuden tulee muuttaa samanaikaisesti jo tallennetun materiaalin pitchiä, loopin pituutta ja fideliteettiä. Holdin tulee säilyttää muisti muuttumattomana, ja Flipin sekä Skipin tulee vaikuttaa osoitukseen.

### Koodihavainto

- `classes/CQGraph.sc:44–69` varaa `coco1`- ja `coco2`-äänibussit sekä kaksi erillistä kohinalla alustettua RAM-puskuria.
- `classes/CQGraph.sc:97–117` luo kaksi `\cqCoco`-instanssia, kummallekin oman puskurin, lähdebusseja, crossfeedin ja matriisikohteet.
- `classes/CQSynthDefs.sc:182–208` johtaa `clockHz`:n speedistä ja SP.AF-modulaatiosta, muuttaa sen RAM-osoitteen askelsuhteeksi ja lukee puskurin ennen kirjoitusta.
- `classes/CQSynthDefs.sc:216–241` muodostaa input- ja feedback-haarat, lisää dustin, rajaa tai wrappaa, kvantisoi ja kirjoittaa tuloksen takaisin samaan RAMiin.
- `classes/CQSynthDefs.sc:198–205` toteuttaa matrix-gated Flip/Skip-toiminnot osoitelaskurissa.
- Crossover on erikseen valinnainen slot (`classes/CQSynthDefs.sc:222–223`), joten se ei yhdistä muistipuskureita yhdeksi muistiksi.

### Esteettinen tulkinta

Kaksi fyysisesti erillistä muistia ovat toteutuksessa aidosti kaksi itsenäistä ääntä, eivät yhden stereoefektin kaksi kanavaa. Tämä tukee kerrostamista ja eri nopeuksien välistä jännitettä. Oletustilat ovat kuitenkin melko symmetrisiä: sama puskurikoko ja sama perusnopeus voivat tehdä ensimmäisestä vaikutelmasta digitaalisesti tasapainoisen. Presettien epäsymmetria ja käyttäjän patchaus rikkovat tätä, mutta mahdollisen lisävariaation tarve on kuuntelukysymys, ei todettu virhe.

### Suositus

Säilytä erilliset RAMit, erilliset synthit ja valinnainen crossfeed. Jos oletuksiin joskus lisätään epäsymmetriaa, tee se toistettavana ja kuuntelulla hyväksyttynä B-luokan variaationa; älä jaa yhteistä muistia tai piilota muistien identiteettiä stereokäsittelyn alle.

## 2. Kello, pituus, pitch, sample rate ja aliasointi — A

### Paikalliseen projektidokumentaatioon perustuva väite

`SPEC.md:45–47` ja `SPEC.md:99–105` vaativat, että clocked-RAM-kello sitoo yhteen loopin pituuden, pitchin ja fideliteetin. Anti-alias-suodatusta ei tule lisätä RAM-silmukan ympärille, ja zero-order hold -luonteen tulee kuulua sekä kirjoitus- että lukusuunnassa. `SPEC.md:504–505` vaatii toiminnan 44,1, 48 ja 96 kHz:llä.

### Koodihavainto

- `classes/CQSynthDefs.sc:201–206` rajaa moduloidun kellon välille 1500 Hz…serverin sample rate ja käyttää suhdetta `clockHz / SampleRate.ir` osoitelaskurin askelena.
- `classes/CQSynthDefs.sc:206–207` tarjoaa ZOH-luennan ja valinnaisen cubic-luennan; `classes/CQSynthDefs.sc:242–243` valitsee niiden välillä.
- Kello ei ole erillinen jälkikäteinen pitch shifter: se muuttaa suoraan muistiosoituksen etenemistä.
- Useat tarkoitukselliset `LocalIn`-/`InFeedback`-reitit ovat yhden control blockin viiveisiä (`classes/CQSynthDefs.sc:394–422`). Niiden absoluuttinen viive muuttuu sample raten ja block sizen mukana.

### Esteettinen tulkinta

Tämä rakenne on uskollisempi projektin omalle käyttäytymistavoitteelle kuin tavallinen delay + bitcrusher + pitch-shifter -ketju. Valinnainen `smooth` on tarkoituksellisesti puhtaampi SC-laajennus ja siten B/C, ei ydinasetus. Sample-rate- ja block-size-vaihtelun esteettistä vaikutusta ei kuitenkaan ole kuunneltu; kaikki automaattiset NRT-ajot olivat 48 kHz:llä.

### Suositus

Pidä ZOH oletuksena ja vältä anti-alias-suodatusta ydinloopissa. Parametrisoi kuuntelu- ja NRT-vertailut 44,1/48/96 kHz:lle sekä block size -arvoille 16/64/256 ennen kuin yhden blokin viiveitä tai sample-rate-kompensaatiota muutetaan.

## 3. Feedback, uudelleenkirjoittuva muisti ja rajallisuus — A

### Paikalliseen projektidokumentaatioon perustuva väite

`SPEC.md:106–114`, `SPEC.md:226–228` ja `SPEC.md:472–488` edellyttävät, että feedback palaa muuntimen kautta RAMiin, jokainen kierros rajataan ja kvantisoidaan uudelleen ja yli unityn menevä palaute kasvaa saturoituneeksi mutta pysyy sisäisesti rajattuna. Holdin tulee olla häviötön.

### Koodihavainto

- Normatiivinen järjestys toteutuu riveillä `classes/CQSynthDefs.sc:234–241`: input + feedback + dust → clip/wrap → round → hold-valinta → `BufWr`.
- `BufRd` luodaan ennen `BufWr`:ää (`classes/CQSynthDefs.sc:164–171`, `206–208`), minkä koodi nimeää hold-identiteetin ehdoksi.
- NRT-turvatesti läpäisi ääriarvorenderit; auditin ajoissa torture-peak oli 0,89 ja 90 sekunnin mayhem-peak 0,89.
- NRT-hold-testi tuotti A4-korrelaation 1,0 ja A5-driftin 0,0 dB.
- Kvantisoinnin askel `2 ** (1 - bits)` yhdessä inklusiivisten ±1-rajojen kanssa tuottaa matemaattisesti `2^bits + 1` kooditasoa. Tämä on dokumentoidun n-bittisyyden tarkkuuspoikkeama, joka kuuluu erillisenä löydöksenä `CQ-AUD-023`.

### Esteettinen tulkinta

Koska rajoitus ja kvantisointi sijaitsevat silmukan sisällä, karheus kertyy muistissa eikä synny vain masterin päälle asetetusta efektistä. Tämä on projektin ääni-identiteetin vahvimpia kohtia. Masterin tanh/limiter käsittelee ulostuloa vasta tämän jälkeen; se voi tasata ääripatchin dynamiikkaa, mutta ei poista silmukan sisäistä historiaa.

### Suositus

Älä siirrä clip/wrap- tai quantize-vaihetta RAM-kirjoituksen jälkeen. Korjaa mahdollinen bittitasojen määrä vain kontrolloidun A/B-kuuntelun kautta, sillä pienikin kvantizerin muutos vaikuttaa feedback-häntään, hold-materiaaliin ja tunnistettavaan rouheeseen.

## 4. Hold, Flip, Skip, Dolby ja digital dust — A/B

### Paikalliseen projektidokumentaatioon perustuva väite

Projektin tutkimusyhteenveto kuvaa loop-lockin, power-on-kohinan, Flipin, Skipin, SP.AF:n, Input/Feedback-VCA:t sekä Dolby gate/other-puncher -käyttäytymisen (`SPEC.md:48–64`). Tarkat SC-käyrät eivät ole dokumentoitua piirivastaavuutta; piirisimulaatio on nimenomaisesti ei-tavoite (`SPEC.md:29–31`).

### Koodihavainto

- Hold vaihtaa RAMiin kirjoitettavan arvon `record`-signaalista ennen kirjoitusta luettuun `here`-arvoon (`classes/CQSynthDefs.sc:240–241`).
- Flip vaihtaa osoiteaskelen merkin ja Skip triggaa osoitelaskurin alkuun (`classes/CQSynthDefs.sc:198–205`).
- Dolby gate ja duck käyttävät envelope-seurantaa ja kiinteitä SC-aikavakioita (`classes/CQSynthDefs.sc:209–233`).
- Dust syntyy valkoisen kohinan ja hitaan satunnaismodulaation tulona ennen muuntimen rajoitusta (`classes/CQSynthDefs.sc:234–239`).
- Power-on-puskurit alustetaan erikseen 8-bittiseen kohinaan ja hold on oletuksena päällä (`classes/CQGraph.sc:64–69`, `SPEC.md:213`).

### Esteettinen tulkinta

Toiminnot ovat osa muistisoitinta, eivät irrallisia transport-nappeja. Erityisesti power-on dust antaa soittimelle lähteen myös ilman tiedostoa tai live-inputia. Dolby-algoritmit ovat kuitenkin välttämättä B-luokan käyttäytymistulkintoja: yksinkertainen expander/duck ei itsessään todista laitekohtaista dynamiikkaa.

### Suositus

Pidä power-on hold/dust, Flip/Skipin osoitetason toteutus ja Dolby-tilojen keskinäinen avainnus. Kalibroi Dolby-kynnykset ja aikavakiot puheella, iskuäänellä ja feedback-kerroksilla; älä markkinoi tarkkoja käyriä piirivastaavina ilman lähdettä ja mittausta.

## 5. Quantussy ja “everything is signal” — A/B

### Paikalliseen projektidokumentaatioon perustuva väite

`SPEC.md:66–82` kuvaa viisi triangle-core-castlea, keskinäiset sample-and-hold-parit, Master Frequency- ja Chaos-makrot sekä kahdeksan Spesal-tilaa. Dokumentaatio merkitsee eksplisiittisesti tulkinnaksi väitteen, että kahdeksan tilaa tarkoittaa neljää partneria × kahta hold-moodia (`SPEC.md:81–82`). `SPEC.md:115–123` vaatii verkon siirtymisen vakaasta hyperkaaokseen ja sen, ettei CV:n ja audion välille tehdä keinotekoista rajaa.

### Koodihavainto

- `classes/CQSynthDefs.sc:100–160` rakentaa viiden oskillaattorin audiotaajuisen LocalIn/LocalOut-verkon.
- Castle valitsee partnerin ja S&H/T&H-moodin kahdeksan tilan kartalla; ulostulo osallistuu takaisin taajuusmodulaatioon.
- Tri- ja castle-signaalit julkaistaan audiobussiin. Writer-aware `In`/`InFeedback`-apuvalinnat säilyttävät audiotaajuisen reitityksen myös group-järjestyksen yli (`classes/CQSynthDefs.sc:394–422`).
- `qShLag` pehmentää castle-ulostuloja; dokumentaation 4 ms oletus on tarkoitettu lähinnä declickiksi (`SPEC.md:236–245`).
- `c1Off`–`c5Off` luetaan ilman parametrirekisterin lagia (`classes/CQSynthDefs.sc:135–138`), vaikka snapshot-morph päivittää numeerisia arvoja jatkuvana sarjana. Mahdollinen kuultava porrastus on löydös `CQ-AUD-010`, mutta sitä ei tässä auditissa kuunneltu.

### Esteettinen tulkinta

Verkko ei ole tavallinen viiden LFO:n modulaatiopankki: se on audiotaajuinen, palautteinen ja patchattavissa takaisin Cocosilmukoihin. Se tukee projektin kuvaamaa emergenttiä soittamista. Tarkka kahdeksan tilan semantiikka ja neljän millisekunnin slew ovat kuitenkin projektin omaa instrumenttisuunnittelua, eivät tämän auditin vahvistamaa laitehistoriaa. Liian suuri `qShLag` tai yleinen parametrien ylitasoitus voi tehdä askelverkosta studiomaista, mutta tätä ei pidä päätellä ilman sokkoutettua kuuntelua.

### Suositus

Säilytä audio-rate-verkko, palautteisuus, viiden castlen erilaiset oletusalueet ja tri/cas-signaalien avoin käyttö. Käsittele partner/mode-karttaa ja slew-arvoja B-luokan kalibrointipisteinä. Kuuntele `qShLag` 0, 0,004 ja 0,08 sekunnin vertailuna samalla deterministisellä tilalla.

## 6. Matriisi, epäsymmetria ja emergentti käyttäytyminen — B

### Paikalliseen projektidokumentaatioon perustuva väite

Projektin ei-tavoitteisiin kuuluu ruudulla simuloitu patch-kaapelointi (`SPEC.md:29–32`). Sen tilalla `SPEC.md:262–282` määrittää 16 lähteen, 22 kohteen, signed-painot, gate-/trigger-kynnykset, `patchDepth`-makron, `shake`-toiminnon ja nimetyt patch-presetit.

### Koodihavainto

- Jokaisella kohteella on 16-kanavainen weight-bussi (`classes/CQGraph.sc:60–63`).
- Matriisin kaikki kirjoitusreitit kulkevat clamp-funnelin läpi (`classes/CQMatrix.sc:40–76`).
- Painot silotetaan 50 ms ja kerrotaan `patchDepth`:llä (`classes/CQSynthDefs.sc:420–422`).
- Lähteisiin kuuluvat tri/cas, envelope followerit, Coco-tapit ja organit; kohteisiin kuuluvat speed, VCA:t, Flip/Skip, castle-FM/Spesal ja deck rate.
- Matriisi on bootissa tyhjä. Presetit ja `shake` ovat käyttäjän eksplisiittisiä toimia, eivät jatkuva automaattinen generatiivinen kerros.

### Esteettinen tulkinta

Nimetty matriisi säilyttää olennaisen avoimuuden paremmin kuin kiinteä modulaatiokaavio, vaikka fyysinen patch-ele ja sähköinen epäideaalisuus eivät siirry ohjelmistoon. Signed-painot vastaavat konseptuaalisesti verso/inverso-ajattelua. `patchDepth` on hyödyllinen kokonaisuuden kesyttäjä, mutta 50 ms:n painolag voi pehmentää aggressiivisia kytkentäeleitä. Tämä on soitettavuuden ja raakuden välinen kalibrointikysymys.

### Suositus

Pidä boot-matriisi tyhjänä ja patchaus eksplisiittisenä. Älä muuta instrumenttia automaattisesti preset-efektiksi. Jos 50 ms lagia arvioidaan uudelleen, tee 0/10/50 ms sokkoutettu vertailu sekä gate-/trigger-reiteillä että jatkuvalla SP.AF-reitillä; klikkiturvaa ei saa heikentää.

## 7. Presetit/overlay-reseptit eivät ole historiallisia patch-todisteita — B/C

### Paikalliseen projektidokumentaatioon perustuva väite

`SPEC.md:278–282` nimeää presetit `empty`, `classic`, `selfNoise`, `quantumDrone`, `organMode` ja `mayhem`. `SPEC.md:560–569` luettelee presetit, tiedostodekit, snapshotit, morphin ja recorderin tietoisina SC-sovituksina tai studio-ominaisuuksina.

### Koodihavainto

- Presetit korvaavat matriisin, mutta muuttavat vain kunkin presetin sparse-listassa
  nimetyt parametrit `CQMatrix`/`CQState`-funnelien kautta.
- `empty` tyhjentää matriisin mutta sen params-lista on tyhjä; se ei palauta
  muuta statea oletuksiin.
- Serverittömässä järjestysreprossa selfNoise → classic jätti 13 residuaalia
  verrattuna fresh → classic -tilaan ja quantumDrone → empty 20 residuaalia.
  State-järjestysriippuvuus on siten varmistettu löydöksessä `CQ-AUD-007`;
  sen kuultavaa vaikutusta ei tässä auditissa testattu.
- `shake` tekee harvan, seedillä toistettavan patchin eikä muuta tasoja tai feedbackia.
- Snapshotit tallentavat laajemman instrumenttitilan ja morph päivittää jatkuvia arvoja noin 30 Hz; enumit, togglet ja Spesal vaihtuvat midpointissa (`classes/CQSnapshots.sc:55–105`, `219–235`).

### Esteettinen tulkinta

`classic`, `selfNoise`, `quantumDrone` ja `organMode` voivat olla käyttökelpoisia
B-luokan demonstraatioita, mutta niiden todellinen ääni riippuu nyt edeltävästä
statesta. Repossa ei ole lähdeaineistoa, jolla ne voisi nimetä historiallisesti
autenttisiksi laitepatcheiksi. `mayhem` on lähimpänä B/C-rajaa: se havainnollistaa
järjestelmän rajoja, mutta tiheä valmis tila voi kuulostaa “preset-efektiltä”, jos
sitä käytetään instrumentin ensivaikutelmana.

### Suositus

Tee eksplisiittinen tuotevalinta. Jos nimet ovat deterministisiä sound-presettejä,
sovella ensin rajattu ja dokumentoitu baseline ja sitten sparse erot. Jos ne ovat
overlay-reseptejä, nimeä ja dokumentoi ne sellaisiksi sekä lisää erillinen
deterministinen Reset; `empty` saa edelleen tarkoittaa tyhjää matriisia, mutta ei
salattua full-state-resettiä. Säilytä nykyinen kerrostaminen vain tietoisena,
testattuna yhteensopivuusominaisuutena. Kuunteluprotokolla vertaa molempia malleja.

## 8. Organ-output — B

### Paikalliseen projektidokumentaatioon perustuva väite

Paikallinen tutkimusyhteenveto mainitsee keltaisen organ-ulostulon, jonka pitch seuraa Speediä (`SPEC.md:60–64`). Sound identity -vaatimus merkitsee osoitelaskurin biteistä johdetun toteutustavan eksplisiittisesti tulkinnaksi (`SPEC.md:122–123`).

### Koodihavainto

`classes/CQSynthDefs.sc:244–248` muodostaa pulse-stackin kellotaajuuden jakajilla 128, 256 ja 512. Organ julkaistaan myös matriisin modulaatiolähteeksi (`classes/CQSynthDefs.sc:257–258`).

### Esteettinen tulkinta

Organ liittyy RAM-osoitekelloon ja on siten rakenteellisesti perustellumpi kuin erillinen sub-oskillaattori. Tarkat jakajat, amplitudit ja puhtaat Pulse-UGenit ovat kuitenkin B-luokan digitaalinen sovitus.

### Suositus

Säilytä kellosidonnaisuus ja matriisikäyttö. Kalibroi jakajien balanssi yhdessä Cocon aliasing- ja feedback-äänen kanssa; vältä organille erillistä “hifi-viimeistelyä”, joka irrottaisi sen kellorakenteesta.

## 9. Studio- ja turvallisuuskerros — C, mutta suurelta osin suojattava

### Paikalliseen projektidokumentaatioon perustuva väite

`SPEC.md:284–316`, `SPEC.md:472–493` ja `SPEC.md:560–569` erottavat Space-, master-, stem-, recorder- ja turvallisuusominaisuudet ydininstrumentista. Fixed safety chain on ei-ohitettava vaatimus; watchdog on vain mukavuustoiminto.

### Koodihavainto

- Master yhdistää panoroidut Cocosignaalit, Spacen ja masterDryn, minkä jälkeen se käyttää `LeakDC → tanh → Limiter → bad-value-squelch → fade` -ketjua (`classes/CQSynthDefs.sc:293–343`).
- Stemillä on sama perusketju (`classes/CQSynthDefs.sc:344–355`, `387–391`).
- File deckit, snapshotit, morph ja disk recorder ovat erillisiä käyttökerroksia.
- Konfiguraatio ei tällä hetkellä estä `outChans`- ja `stemOuts`-kanavien duplikaatteja tai päällekkäisyyttä. Samalle fyysiselle bussille turvallisuusketjujen jälkeen summatut signaalit voivat ylittää limiterin katon; tämä on turvallisuuslöydös `CQ-AUD-001`, ei fidelity-ominaisuus.

### Esteettinen tulkinta

Tanh ja limiter voivat tasata kaikkein äärimmäisimpien patchien transientteja, mutta sisäinen Coco-saturaatio ja uudelleenkirjoitus tapahtuvat ennen niitä. Turvaketju ei siksi lähtökohtaisesti muuta ydintä tavalliseksi “hifi-efektiksi”. Tiedostodekit, pan, recorder ja snapshotit tekevät ohjelmistosta käytännöllisen, mutta niiden ei tule määrätä ydinäänen oletustilaa.

### Suositus

Säilytä ei-ohitettava turvaketju ja pidä masterDry oletuksena nollassa. Korjaa fyysisten lähtöjen disjoint-validointi ennen live-stem-käyttöä. Älä heikennä limiteriä tai sisäistä feedback-rajaa autenttisuuden nimissä.

## 10. Space — nykyinen toteutus C, konsepti avoin

### Paikalliseen projektidokumentaatioon perustuva väite

- `SPEC.md:284–306` nimeää Spacen valinnaiseksi FDN-reverbiksi ja asettaa `spaceMix`-oletuksen nollaan.
- `SPEC.md:567` toteaa paikallisen projektidokumentaation tasolla, ettei tavoitellussa laitteessa ole reverb-efektiä.
- `docs/history/PLAN_v1.0.md:688–700` dokumentoi käyttäjän 2026-07-19 linjauksen: nykyinen Space koetaan metalliseksi/plate-tyyppiseksi, eikä sitä tai GUI-parametreja tule viilata ennen suurempaa konseptipäätöstä. Vaihtoehtona mainitaan Cocon omasta arkkitehtuurista nouseva kontekstitietoinen makro.

### Koodihavainto

- Cocon post-fader-ääni panoroidaan Space-sendille (`classes/CQSynthDefs.sc:251–256`).
- `\cqSpace` on nelilinjainen FDN: 29,3/37,3/43,7/53,1 ms DelayC-linjat, Householder-palautematriisi, OnePole-damping ja feedbackin kova 0,98-raja (`classes/CQSynthDefs.sc:264–286`).
- Space vastaanottaa vain Coco-sendin. Sen viiveet tai palaute eivät johdu Coco-nopeuksista, RAM-vaiheesta, Quantussy-verkon aktiivisuudesta tai matriisin tilasta.
- Space on pitkäikäinen erillinen synth FX-groupissa ja sen return lisätään masterissa (`classes/CQGraph.sc:118–120`, `classes/CQSynthDefs.sc:296–307`).

### Esteettinen tulkinta

Nykyinen Space on erillinen studio-/laatu-efekti, ei Coco-makro. Lyhyt ja harva nelilinjainen verkko voi tuottaa metallisen plate-halon. Suurella send/mixillä se voi peittää Cocon kuivan ZOH-rouheen ja transienttien rajallisuuden. Koska oletusmix on nolla, se ei kontaminoi ydintä ilman käyttäjän valintaa.

### Suositus ja vaihtoehdot

Konsepti tulee päättää ennen pieniä DSP- tai GUI-muutoksia:

1. **Säilytä nykyinen FDN ja nimeä se rehellisesti.** Kutsu toimintoa esimerkiksi “FDN” tai “Plate”, pidä se valinnaisena ja oletuksena pois. Tämä on vähäriskisin vaihtoehto ja hyväksyy C-luokituksen.
2. **Tee Spacesta kontekstitietoinen B-luokan makro.** Johda rajattuja suhteellisia ohjauksia esimerkiksi Coco-kellojen suhteesta/erosta, feedback-loopin energiasta ja Quantussy-aktiivisuudesta. Makro ei saa avata suoraa palautereittiä masterin ohi eikä vaarantaa 0,98-rajaa. Tarkka suhde vaatii prototyypit ja sokkoutetun kuuntelun.
3. **Siirrä reverb ytimen ulkopuolelle.** Pidä kuiva instrumentti ytimenä ja käsittele tila hostissa tai erillisessä efektimoduulissa. Tämä tekee fidelity-rajan selvimmäksi mutta muuttaa nykyistä v1-työnkulkua.

Kaikissa vaihtoehdoissa tulee säilyttää oletusarvo pois, turvallinen palaute, DC-suoja ja masterin lopputurva. Muutosriski on korkea: Space koskettaa DSP:tä, GUI:ta, F14/MIDI-karttaa, presettejä, NRT:tä, CPU-budjettia ja hyväksyttyä käyttötyönkulkua.

## 11. Avoin instrumentti vai preset-efekti

### Paikalliseen projektidokumentaatioon perustuva väite

Projektin tavoite on “processor-instrument”, jossa kaksi Cocoa, viisi castlea ja patchaus ovat jatkuvasti käytettävissä (`SPEC.md:15–33`). Matriisi on bootissa tyhjä, Space pois ja masterDry pois.

### Koodihavainto

- Ydinsynthit ovat pitkäikäisiä ja graph staattinen; preset ei vaihda koko DSP-algoritmia.
- Presetit käyttävät samoja matriisi- ja state-funneleita kuin manuaalinen ohjaus.
- `shake` ei koske tasoihin tai feedbackiin ja voidaan toistaa seedillä.
- Space, dry ja organ ovat tasolla pois tai vähäisiä oletustilassa.

### Esteettinen tulkinta

Rakenne suosii avointa instrumenttia enemmän kuin vaihtuvia black-box-algoritmeja. Riski preset-efektimäisyyteen syntyy lähinnä käyttöliittymästä tai demojärjestyksestä: jos `mayhem` tai märkä Space esitellään ensimmäisenä ja `empty` jää piiloon, käyttäjän käsitys voi kiinnittyä valmiiseen efektiin.

### Suositus

Pidä tyhjä matriisi ja manuaalinen patchaus ensimmäisen luokan toimintoina.
Esittele presetit/overlayt esimerkkeinä, näytä niiden muuttamat yhteydet ja anna
käyttäjän purkaa niitä. Deterministinen preset testataan puhtaasta baselinesta ja
järjestysriippumattomasti; overlay testataan eri järjestyksissä sen dokumentoitua
residuaalisopimusta vasten, ja rinnalla pitää olla erillinen Reset.

## Suojattavat rakenteet

Seuraavia rakenteita ei tule rikkoa fidelity-, turvallisuus- tai siistimismuutoksissa:

1. **Kaksi erillistä Coco-puskuria ja synthiä.** Älä yhdistä niitä stereo-RAMiksi.
2. **`BufRd` ennen `BufWr`:ää.** Järjestys on hold-identiteetin ja täyden puskuriviiveen ehto (`classes/CQSynthDefs.sc:164–171`, `206–208`).
3. **`clip/wrap → quantize → RAM`.** Sisäinen rajoitus on sekä ääni-identiteetti että turvallisuusraja (`classes/CQSynthDefs.sc:234–241`).
4. **Speedin suora yhteys RAM-osoiteaskeleeseen.** Älä korvaa sitä erillisellä pitch-shifterillä.
5. **ZOH oletuksena ja anti-alias-suodatuksen puuttuminen ydinloopista.** `smooth` saa pysyä valinnaisena.
6. **Hold-integriteetti.** Säilytä identity-rewrite-rakenne sekä A4/A5-regressiot
   ja niiden tiukat rajat; lisää erikseen sample-/bittivertailu ennen
   bit-exact-väitettä.
7. **Power-on hold ja erillinen dust-kohina kummallekin muistille.** Ne ovat tarkoituksellinen lähde, eivät alustusbugin jäänne.
8. **Viiden castlen audio-rate-palautusverkko ja tri/cas-signaalien avoin käyttö.** Älä alenna verkkoa control-rate-LFO-pankiksi.
9. **Tarkoitukselliset yhden blokin `LocalIn`/`InFeedback`-viiveet ja staattinen group-järjestys.** Älä “korjaa” niitä ilman koko verkon analyysia.
10. **Writer-aware `In`/`InFeedback`-apurakenteet.** Ne estävät väärän blokin hiljaisuuden ja säilyttävät palautereitit.
11. **Matriisin ja staten clamp-funnelit.** Kaikki GUI-, MIDI-, preset-, snapshot- ja koodikirjoitukset kulkevat niiden kautta.
12. **Tyhjä boot-matriisi ja eksplisiittinen patchaus.** Tämä suojaa avoimen instrumentin luonnetta.
13. **Ei-ohitettava master-/stem-turvaketju, boot-fade, panic ja mute.** Korjaa lopullisten fyysisten bussien validointi, älä heikennä ketjua.
14. **Space oletuksena pois ja feedback alle yhden.** Konseptimuutos ei saa avata uutta runaway-silmukkaa.
15. **NRT-turva- ja hold-kynnykset.** Laajenna testejä; älä löysennä rajoja äänen “vapauttamiseksi”.

## Todentamattomat asiat ja jatkoaineisto

Tässä auditissa ei vahvistettu:

- ulkoisia historiallisia tai piirikohtaisia väitteitä
- fyysisen laitteen ja SC-toteutuksen sokkoutettua A/B-kuuntelua
- Dolby-käyrien, organ-jakajien tai Quantussy-tilakartan laitekohtaista vastaavuutta
- 44,1/96 kHz:n tai eri block size -arvojen kuultavaa käyttäytymistä
- Spacen esteettistä sopivuutta kuuntelemalla
- presetien järjestysriippumattomuutta kuuntelemalla
- linked-stereon vaihetta tai dry-monitoroinnin stereokuvaa
- live-stemien toimintaa vähintään nelikanavaisella äänikortilla
- MIDI-eleiden tuntumaa fyysisellä 16n-ohjaimella
- pitkän abuse-session soitettavuutta tai resurssikäyttäytymistä.

Ulkoista fidelity-arviota varten projektiin kannattaa lisätä lähdeluettelo, jossa jokaiselle väitteelle annetaan URL tai arkistoviite, hakupäivä, sivu/kohta ja tieto siitä, onko kyse kuvaustekstistä, kaaviosta, mittauksesta vai esteettisestä tulkinnasta. Siihen asti oikea ilmaisu on “projektin dokumentoiman Cocoquantus-rakenteen mukainen”, ei “historiallisesti” tai “piiritasolla varmennettu”.
