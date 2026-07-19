Suorita koko tämän repositoryn perusteellinen, evidenssipohjainen ohjelmisto-, DSP-, ominaisuus-, käyttöliittymä- ja konseptiauditointi.

Käytä rinnakkaisia subagentteja, koska tehtävä jakautuu itsenäisiin lukupainotteisiin työalueisiin. Pidä pääagentti auditoinnin koordinoijana ja lopullisten tulosten syntetisoijana.

TÄRKEÄ TOIMINTARAJAUS

* Älä muuta ohjelman lähdekoodia tässä vaiheessa.
* Älä korjaa löytämiäsi ongelmia vielä.
* Saat suorittaa turvallisia paikallisia komentoja, käynnistää testejä ja tehdä väliaikaisia testitiedostoja, jos ne eivät muuta projektin käyttäytymistä.
* Saat kirjoittaa raportit vain hakemistoon `audit/`.
* Älä poista tiedostoja tai muuta käyttäjän asetuksia.
* Älä oleta testin onnistuneen, ellet ole todella suorittanut sitä.
* Jos jotain ei voida testata tässä ympäristössä, merkitse se selvästi testaamattomaksi ja anna täsmällinen manuaalinen testiohje.
* Odota kaikkien subagenttien tulokset ennen lopullisen raportin laatimista.

PROJEKTIN TAVOITE

Tämä on SuperColliderilla toteutettu Cocoquantus-/Cocoquantus 2 -henkinen soitin tai emulaatio. Sen on tarkoitus:

1. olla teknisesti luotettava ja korkealaatuinen
2. toimia ilman piileviä virheitä, rikkoutuneita ohjaimia tai toteutumattomia ominaisuuksia
3. käyttää selkeää ja ylläpidettävää SuperCollider-arkkitehtuuria
4. käsitellä audio-, serveri-, node-, bus-, buffer-, clock- ja GUI-resursseja turvallisesti
5. säilyttää Cocoquantuksen ja laajemmin Ciat-Lonbarden kokeellinen, orgaaninen, epävakaa, kosketeltava ja ei-liian-siloiteltu luonne
6. erottaa alkuperäiselle laitteelle uskolliset rakenteet tietoisista luovista laajennuksista
7. tarjota käyttökelpoinen ja esteettisesti johdonmukainen GUI

Lue ennen johtopäätöksiä kaikki relevantit projektitiedostot, erityisesti:

* `AGENTS.md`
* `README.md`
* arkkitehtuuri- ja ominaisuusdokumentit
* Cocoquantus- ja Ciat-Lonbarde-referenssit
* tunnetut ongelmat ja TODO-tiedostot
* kaikki `.scd`, `.sc`, konfiguraatio-, preset- ja testitiedostot

Jos jokin näistä puuttuu, kirjaa puute raporttiin. Älä keksi puuttuvaa spesifikaatiota.

DELEGOI VÄHINTÄÄN SEURAAVAT TYÖVIRRAT

1. ARKKITEHTUURI JA KOODIN LAATU

Tutki:

* projektin rakenne ja vastuunjako
* moduulien riippuvuudet
* globaali tila ja ympäristömuuttujat
* alustuksen ja sammutuksen järjestys
* uudelleenkäynnistettävyys
* nimeäminen ja luettavuus
* toisteinen, kuollut tai saavuttamaton koodi
* liian suuret funktiot ja piilevät riippuvuudet
* virheenkäsittely
* polkujen ja resurssien hallinta
* dokumentaation ja toteutuksen vastaavuus
* ylläpidettävyys ja laajennettavuus

2. SUPERCOLLIDER-RUNTIME JA RESURSSIT

Tutki ja testaa mahdollisuuksien mukaan:

* kääntyvätkö kaikki tiedostot
* syntyvätkö ja latautuvatko SynthDefit
* boot- ja re-boot-käyttäytyminen
* Group- ja Node-järjestys
* puuttuvat target-nodet ja kilpailutilanteet
* Bus- ja Buffer-allokointi ja vapauttaminen
* OSCdef-, MIDIdef-, Routine-, Task-, Clock- ja callback-resurssien siivoaminen
* ikkunan sulkeminen ja ohjelman pysäyttäminen
* toistuvan start/stop-ajon turvallisuus
* suhteelliset tiedostopolut
* Windows-yhteensopivuus
* mahdolliset riippuvuudet quarkeihin tai ulkoisiin plugineihin
* virheilmoitukset ja hiljaisesti epäonnistuvat operaatiot

3. DSP, AUDIO JA TURVALLISUUS

Tutki:

* audio-rate- ja control-rate-signaalien oikea käyttö
* delay- ja bufferirakenteet
* palautekytkennät ja niiden numeerinen vakaus
* vaaralliset feedback-arvot
* DC-komponentti
* NaN-, inf- ja räjähdysriskit
* clipping ja headroom
* limiterien tarkoituksenmukaisuus
* parametrien smoothing
* zipper noise
* äkilliset amplitudi- tai nopeusmuutokset
* sample rate -riippuvuus
* monikanavalaajennus
* stereo- ja mono-käyttäytyminen
* CPU-kuorma
* vapautumattomat synthit
* vuotavat bufferit tai nodet
* hiljaiset, väärin reititetyt tai vaiheeltaan ongelmalliset signaalipolut

Arvioi myös, peittävätkö turvallisuusrakenteet liikaa soittimen tarkoituksellista arvaamattomuutta tai tekevätkö ne siitä liian tavanomaisen digitaalisen efektin.

4. OMINAISUUSKATTAVUUS JA KÄYTTÄYTYMINEN

Laadi ensin täydellinen ominaisuusinventaario dokumentaatiosta, GUI:sta ja koodista.

Tarkista jokaisesta ominaisuudesta:

* onko se todella toteutettu
* onko se saavutettavissa GUI:sta tai API:sta
* vaikuttaako ohjain oikeaan DSP-parametriin
* onko parametri kuultavasti ja teknisesti toimiva
* vastaako alue ja skaalaus dokumentaatiota
* päivittyykö GUI ja audio molempiin suuntiin oikein
* tallentuuko ja palautuuko arvo preseteissä
* toimiiko ominaisuus myös ääritilanteissa
* onko ominaisuus vain näennäinen tai osittain toteutettu
* löytyykö dokumentoimattomia ominaisuuksia tai käyttämätöntä koodia

Tee jäljitettävyysmatriisi muodossa:

ominaisuus → dokumentaatio → GUI-ohjain → koodin toteutus → DSP-kohde → testi → tila.

5. COCOQUANTUS- JA CIAT-LONBARDE-USKOLLISUUS

Arvioi jokainen olennainen äänellinen ja rakenteellinen osa kolmella luokituksella:

A. dokumentoituun Cocoquantus-rakenteeseen perustuva
B. Ciat-Lonbarde-henkinen luova tulkinta
C. tavanomainen tai irrallinen lisäys, jonka suhde alkuperäiseen estetiikkaan on heikko

Tutki erityisesti:

* kahden Coco-muistin tai delay-/looperipuolen itsenäisyys
* speedin, delay-pituuden, sample raten ja pitchin keskinäinen suhde
* feedback ja uudelleenkirjoittuva muisti
* freeze/sample
* flip
* skip
* kellomodulaatio ja Speed Affect
* Quantussy-tyyppinen keskinäinen modulointi
* epäsymmetria, ristiinkytkennät ja emergentti käyttäytyminen
* digitaalinen karheus, aliasointi ja rajallisuus
* liian puhdas, geneerinen tai studiomainen prosessointi
* ominaisuudet, jotka tekevät instrumentista preset-efektin avoimen soittimen sijasta
* `Space`-parametrin nykyinen toteutus ja se, onko se erillinen reverb, kontekstitietoinen Coco-makro vai jotain muuta

Älä väitä historiallista tai laitekohtaista uskollisuutta ilman projektissa olevaa lähdettä. Erottele raportissa:

* dokumentoitu tosiasia
* koodista havaittu toteutus
* esteettinen tulkinta
* oma suositus

6. GUI JA KÄYTETTÄVYYS

Tutki:

* vastaako visuaalinen hierarkia soittimen rakennetta
* ymmärtääkö käyttäjä kaksi Coco-puolta ja keskimmäisen modulaatiojärjestelmän
* ohjainten ryhmittely
* nimeäminen ja terminologia
* parametrien arvot, yksiköt ja tooltipit
* tilan näkyvyys
* feedbackin ja vaarallisten tilojen visualisointi
* freeze-, flip-, skip- ja modulaatiotilojen ymmärrettävyys
* preset-järjestelmä
* skaalautuvuus eri resoluutioille
* hiiren ja näppäimistön käyttö
* visuaalinen johdonmukaisuus
* liian geneerinen plug-in-estetiikka
* Ciat-Lonbarden hengen välittyminen ilman suoraa pinnallista kopiointia
* mahdollisuudet tehdä GUI:sta sekä selkeä että omaleimainen

Laadi GUI:lle vaiheittainen kehityssuunnitelma, mutta älä toteuta sitä vielä.

TESTAUS

Etsi ensin projektin omat käynnistys- ja testiohjeet. Käytä niitä, jos ne ovat turvallisia ja käyttökelpoisia.

Suorita mahdollisuuksien mukaan:

* syntaksi- ja compile-testit
* headless- tai komentorivipohjaiset testit
* serverin boot- ja shutdown-smoke-testit
* toistetut start/stop-testit
* SynthDefien luonti- ja instansiointitestit
* GUI:n avaaminen ja sulkeminen
* kaikkien ohjainten perustason savutesti
* presetien round-trip-testi
* resurssivuotojen tarkistus
* ääriarvojen testit
* lyhyt pitkäkestoisuustesti
* projektin dokumentoitujen ominaisuuksien läpikäynti

Älä pidä kuultavaa estetiikkaa automaattisesti todennettuna pelkillä yksikkötesteillä. Luo erillinen ihmisen suoritettava kuuntelutestiprotokolla.

RAPORTOINTIVAATIMUKSET

Luo hakemisto `audit/` ja seuraavat tiedostot:

1. `audit/EXECUTIVE_SUMMARY.md`
2. `audit/FINDINGS.md`
3. `audit/FEATURE_TRACEABILITY.md`
4. `audit/COCOQUANTUS_FIDELITY.md`
5. `audit/GUI_DEVELOPMENT_PLAN.md`
6. `audit/LISTENING_TEST_PROTOCOL.md`
7. `audit/TEST_EVIDENCE.md`
8. `audit/IMPROVEMENT_BACKLOG.md`

Jokaisessa löydöksessä tulee olla:

* yksilöllinen tunniste
* vakavuus: P0, P1, P2, P3 tai Info
* luottamustaso: varmistettu, vahva epäily tai testaamaton
* vaikutus
* tarkka tiedosto ja rivikohta
* tekninen perustelu
* toistamisohje
* havaittu evidenssi
* suositeltu korjaussuunta
* mahdollinen regressioriski

Prioriteetit:

* P0: datan menetys, kaatuminen, vaarallinen audiotaso tai ohjelman perustoiminnan estävä virhe
* P1: olennainen ominaisuus ei toimi, merkittävä resurssivuoto tai vakava arkkitehtuuriongelma
* P2: käyttäytymisvirhe, epäjohdonmukaisuus, ylläpidettävyysongelma tai selvä käytettävyysongelma
* P3: pieni parannus
* Info: havainto tai tietoinen suunnitteluvaihtoehto

Älä täytä raporttia spekulatiivisilla huomioilla. Yhdistä päällekkäiset löydökset. Nosta esiin myös hyvin toteutetut asiat ja rakenteet, joita ei pidä rikkoa korjauksissa.

LOPULLINEN VASTAUS

Kun kaikki subagentit ovat valmistuneet:

1. syntetisoi niiden tulokset
2. ratkaise tai merkitse niiden väliset ristiriidat
3. varmista, että jokaisella korkeaprioriteettisella löydöksellä on evidenssi
4. kerro, mitä todella testattiin
5. kerro, mitä ei voitu testata
6. listaa tärkeimmät löydökset tärkeysjärjestyksessä
7. nimeä raporttitiedostot
8. älä muuta lähdekoodia
