# Lopullinen auditointiarvio

> **Historiallinen rajaus:** tämä arvio koskee 2026-07-19 auditoitua teknistä
> `v1.0`-baselinea. Sen varmennetut korjaukset toteutettiin myöhemmissä vaiheissa;
> nykyinen release-status on juuren [`PLAN.md`](../PLAN.md)-tiedostossa. Raporttia
> ei pidä lukea nykyisen Yönpuoli-ehdokkaan uutena julkaisupäätöksenä.

Auditointipäivä: 2026-07-19
Ohje: [`history/prompts/AUDIT_EVAL.md`](history/prompts/AUDIT_EVAL.md)
Laajuus: kaikki `audit/`-raportit, koko alkuperäinen lähdekoodi, testit ja keskeinen
dokumentaatio. Lähdekoodia ei muutettu.

## Auditoinnin jälkeinen GUI-päätös

Käyttäjä hyväksyi 2026-07-19 uuden Archipelago-vision. `GUI_SPEC.md` on nyt uuden
GUI:n autoritatiivinen käyttäytymis- ja visuaalinen määrittely; tämän auditin
GUI-löydökset säilyvät v1.0-toteutuksen havaintoina, mutta aiemmat vapaat GUI-
parannusehdotukset eivät ole enää backlog. Toteutusjärjestys on päivitetty tiedostoihin
`GUI_DEVELOPMENT_PLAN.md`, `REMEDIATION_ROADMAP.md` ja projektin `PLAN.md`.

## Kokonaispäätös

Projekti on **käyttökelpoinen hallitussa nykykäytössä**, kun main- ja stem-lähdöt
ovat varmasti eri fyysisillä kanavilla, käyttäjä välttää nopeita päällekkäisiä
boot/free/REC/load-operaatioita eikä oleta `CQ.reboot`-komennon vaihtavan käyvän
scsynthin asetuksia. Core-sound, headless-ohjaus, state/matrix-funnelit ja normaali
finite-feedback toimivat vahvan automatisoidun näytön perusteella.

Projekti ei kuitenkaan ole nykyisellään valmis uudeksi release-artefaktiksi:

- **CQ-AUD-001 on varmennettu P0-julkaisun estäjä.** Hyväksytty output-konfiguraatio
  voi summata main/stem-signaaleja samalle fyysiselle bussille erillisten limiterien
  jälkeen ja rikkoa keskeisen audioturvallisuuslupauksen.
- **CQ-AUD-002–004 ovat P1-tason vakausesteitä seuraavalle vakaaksi nimetylle
  versiolle.** Recorderin omistus, session boot/free/reboot ja todellinen server
  reboot eivät ole luotettavia poikkeus- ja kilpailutilanteissa.

Alkuperäisen auditin kahden P0:n ja neljän P1:n luokitus oli liian ankara. Riippumaton
arvio päätyi **yhteen P0:aan ja kolmeen P1:een**. Recorderin tiedostonkorruptiota ei
toistettu, deckin last-request-wins-rikettä ei saatu esiin, Coco ei jäänyt NaN-tilaan
ja master turvasi fyysisen ulostulon. Nämä rajaukset on sisällytetty
[`VERIFIED_FINDINGS.md`](VERIFIED_FINDINGS.md)-raporttiin.

## Varmennettu julkaisutila

| Luokka | Päätös |
|---|---|
| Uusi release build | **No-go**, kunnes CQ-AUD-001 on korjattu ja config/offline-NRT-regressiotesti läpäisee. |
| Seuraava vakaa versio | Korjaa lisäksi recorderin tilakone, session generation/rollback ja oikea server-reboot. |
| Päivittäinen kokeellinen käyttö | Kyllä, disjoint-output-konfiguraatiolla ja välttäen nopeita päällekkäisiä lifecycle-kutsuja. |
| “Kaikki patchit ovat turvallisia” -lupaus | Ydinsignaalille uskottava, mutta ei nykyisen output-konfiguraation aliasointitapauksessa. |
| Nelikanavainen hardware/stem-hyväksyntä | Avoin; NRT on vahva, fyysistä ≥4-ch-laitetta ei tässä auditissa käytetty. |

## Mikä on tekninen vika

1. Output-indeksien globaali yksikäsitteisyys puuttuu; tämä on ainoa P0.
2. Recorderilla ei ole `starting/recording/stopping`-tilakonetta eikä attempt-kohtaista
   resurssiomistusta. Kaksois-start ja immediate-stop toistettiin live-serverillä.
3. Session elinkaarella ei ole generation-tokenia, peruutusta eikä kattavaa
   rollbackia. `boot`→välitön `free` jätti serverin käyntiin ja myöhäinen callback
   kaatui nil-graafiin.
4. `CQ.reboot` rakentaa vain graafin uudelleen; se ei käynnistä scsynthiä uudelleen
   eikä siten sovella käynnistysoptioita.
5. Deck-load voi valmistua `free`-kutsun jälkeen ja resurrectoida resursseja.
6. Space- ja Quantussy-feedback voivat jäädä ei-finite-tilaan, vaikka master
   fail-closed-mykistää fyysisen ulostulon. Coco-silmukassa samaa ongelmaa ei toistettu.
7. Linked-stereo dry-monitorointi summaa L/R-kanavat dual-monoksi.
8. `micChan:nil`-tilassa GUI/facade voi väittää micin olevan live, vaikka DSP-reittiä
   ei ole.
9. Ulkoinen server quit/crash ja GUI:n deferred init eivät kuulu yhtenäiseen session
   omistusmalliin.
10. Live lifecycle-, recorder/deck-, GUI→State→DSP- ja SR/block-size-regressiotestit
    puuttuvat; nykyinen class-library-ajo ei ole täysin hermettinen.

## Mikä on konseptuaalinen päätös, ei automaattinen bugi

- **Preset vs overlay:** nykyinen patchPreset tyhjentää matriisin mutta overlaytaa vain
  osan parametreista. Järjestysriippuvuus on tosi, mutta kerrostaminen voi olla
  tarkoituksellinen ja musikaalinen. Päätä täysi sound-preset tai nimeä toiminto
  patch-reseptiksi; älä determinisoi sitä hiljaa.
- **Space:** nykyinen nelilinjainen metallinen FDN on valinnainen studioefekti,
  oletuksena pois, eikä historiallisen Coco-arkkitehtuurin sisäinen osa. Sen säilytys,
  viritys tai rinnakkainen vaihtoehto ratkaistaan kuuntelulla. Space ei saa peittää
  Cocon uudelleenkirjoittuvaa muistia geneerisellä reverb-taililla.
- **Castle-offset-lag ja kvantisointitasot:** toteutuksen yksityiskohdat voidaan
  mitata, mutta kuultavaa vikaa tai historiallista laitevastaavuutta ei osoitettu.
  Smoothing tai matemaattinen “korjaus” voi steriloida Quantussyn luonnetta.
- **Snapshot-formaatti:** `.load` on tarkoituksellinen executable SuperCollider
  -tekstiformaatti. Se on luottamusraja, ei haavoittuvuus niin kauan kuin vain
  käyttäjän omia/luotettuja snapshotteja luvataan tuettavan.
- **Space/Shake-edellytykset:** send+return ja avoimet kohdepolut ovat patch-topologiaa,
  eivät rikkinäisiä ohjaimia. GUI voi opastaa, mutta oletuksia ei pidä avata
  näkymättömänä sivuvaikutuksena.

## Valinnaiset parannukset

- suurten facade/GUI-tiedostojen pieni vastuu kerrallaan tehtävä jako;
- turvallinen dataformaatti vain, jos epäluotettujen snapshot-tiedostojen tuki tulee
  todelliseksi vaatimukseksi;
- fidelity-lähdeketjuun perustuvat mahdolliset castle-offset-/kvantisointikokeet.

GUI:n rakenne, eleet, minimi-viewport, fokuslaki, Space-rajaus ja Ink & Phosphor -kieli
eivät ole enää tämän listan valinnaisia ehdotuksia vaan `GUI_SPEC.md`:n päätöksiä.
Muutokset niihin vaativat käyttäjän etukäteishyväksynnän. Mikään työ ei saa ohittaa
P0/P1-korjauksia tai muuttaa soundia visuaalisen commitin sivuvaikutuksena.

## Erityisen onnistuneet ratkaisut, jotka pitää säilyttää

- Server tree on oikein järjestetty: Sources → Quantussy → Cocos → FX → Master.
  Deckit ajetaan ennen input-syntikkaa ja recorder Master-ryhmän lopussa.
- Parametri-, patch- ja toimintofunnelit ovat selkeät: `CQSpec`/`CQState`,
  `CQMatrix` ja `Cocoquantus`-facade. GUI, koodi, snapshot ja MIDI jakavat saman tilan.
- Kaksi itsenäistä Coco-muistia, ristikytkentä, signed 16×22-matriisi, Quantussyn
  castle/spesal-rakenne, Hold/Flip/Skip/dolby ja digital dust muodostavat aidosti
  omaleimaisen, emergentin instrumentin.
- Normaali feedback on tarkoituksellisesti elävä mutta rajattu. Master- ja stem-
  ketjujen LeakDC→tanh→Limiter→bad-value-squelch, hiljainen boot-fade ja panic ovat
  hyviä ei-ohitettavia turvarakenteita.
- Headless on ensiluokkainen; 16n on valinnainen; fyysiset I/O:t ovat
  konfiguroitavia; staattinen ydinsynth-graafi on suorituskyvyn ja ennustettavan
  node-orderin kannalta vahva valinta.
- Viisisarakkeinen `Sources | Coco 1 | Quantussy | Coco 2 | Master` ja sen alla oleva
  banana-matriisi välittää instrumentin topologian hyvin. Tätä ei pidä korvata
  geneerisellä kanavastrip-plugin-layoutilla.
- Nykyinen automaatio on poikkeuksellisen hyödyllinen: unit-, NRT safety-, matrix,
  Space-, stem- ja Hold-testit löysivät tässäkin arvioinnissa oikeat rajat.

## Riippumaton testinäyttö

Arvioija ajoi testit uudelleen eikä nojannut vain aiempien raporttien väitteisiin.

| Ajo | Tulos |
|---|---|
| Luokkakirjaston compile | `CQ_COMPILE_OK`, exit 0. Myöhempi AppData-archive-varoitus oli sandbox-ympäristön jälkivaihe, ei compile-failure. |
| Serverless unit | 3 082 pass, 0 failure, `CQ_TESTS_PASSED`. |
| NRT safety | torture, Coco, Quantussy, mayhem, matrix-FM, Space ja stems läpäisivät; suurin raportoitu yksittäiskanavan peak 0,89; `CQ_SAFETY_PASSED`. |
| NRT Hold | A4 ennen/jälkeen peak 0,547 ja korrelaatio 1,0; A5 pass 1/21 drift 0,0 dB; `CQ_HOLD_OK`. A4 ei ole bit-exact-todiste. |
| Output-alias | Config hyväksyi törmäykset; säilytetyn 4-ch-renderin offline-summa enintään 3,21924. Fyysistä aliasia ei ajettu. |
| Recorder live | Double-start loi kaksi nodea, stop jätti yhden orphanin; immediate-stopin jälkeen tallennus käynnistyi. WAV oli luettavissa. |
| Deck live | Load + immediate free resurrectoi deckin; kahden loadin last-request-rike ei toistunut. |
| Boot/free live | Myöhäinen build-callback kaatui; graph nil, server jäi käyntiin, ready-callback 0 kertaa. |
| Bad values NRT | Coco palautui ja bufferi pysyi puhtaana; Space ja Quantussy jäivät saastuneiksi; masterin ulostulosquelch säilyi. |
| Preset-sekvenssit | `selfNoise→classic`: 13 jäännösarvoa; `quantumDrone→empty`: 20. |

Kaikki arvioijan live-serverit lopetettiin; auditin lopussa ei ollut jääneitä sclang-
tai scsynth-prosesseja. Testiharnessit olivat väliaikaisia eivätkä muuttaneet
tuotantolähdekoodia.

## Mitä ei testattu

- päällekkäisiä fyysisiä output-kanavia kaiuttimilla/kuulokkeilla — tarkoituksella;
- neljän erillisen fyysisen ulostulon stem-kuuntelua tai 4-ch hardware-recordingia;
- todellista tiedostonkorruptiota/datanmenetystä recorder-kilvassa — sitä ei
  havaittu;
- linked stereo -parin näytetarkkaa start-offsetia;
- täydellistä GUI-automaatiota, DPI/resize/accessibility-matriisia tai visuaalista
  käyttöä tässä pääarvioinnissa;
- fyysistä 16n-laitetta tai MIDI hot-plugia tässä pääarvioinnissa;
- 44,1/96 kHz × kaikkien block sizejen täyttä matriisia;
- 60 minuutin resource/CPU-soakia tai sokkokuuntelua oikeaa Coco-laitetta vastaan;
- ulkoisen scsynth-crashin live-reproduceria; stale-state-mekanismi tarkistettiin
  lähdekoodista.

Historialliset manuaalitulokset ovat hyödyllisiä, mutta niitä ei merkitä tämän
pääarvioijan uudelleen suorittamiksi.

## Auditointiraporttien ristiriitojen ratkaisu

- Alkuperäisen `EXECUTIVE_SUMMARY.md`-raportin P0-lukumäärä 2 korvataan luvulla 1.
- CQ-AUD-002 on P1, CQ-AUD-005 ja CQ-AUD-006 P2; CQ-AUD-006 koskee Spacea ja
  Quantussya, ei Cocoa.
- CQ-AUD-009 ja CQ-AUD-010 eivät ole varmennettuja käyttäjävaikutuksia.
- CQ-AUD-014 on tarkoituksellinen topologia, CQ-AUD-022 luottamusraja ja
  CQ-AUD-025 esteettinen valinta.
- README/PLANin “v1.0 valmis” on historiallinen status, ei nykyisen P0/P1-auditin
  läpäissyt release-portti.
- NRT:n yksittäisten kanavien rajallisuus ja CQ-AUD-001 eivät ole ristiriidassa:
  vika syntyy vasta, kun erilliset jo rajoitetut kirjoitukset aliasoituvat samalle
  fyysiselle bussille niiden turvaketjujen jälkeen.
- “Space on toteutettu” ja “Spacen luonne on avoin” voivat molemmat olla totta:
  nykyinen FDN toimii, mutta sen lopullinen esteettinen rooli on tuotekysymys.

## Auditoinnin luotettavuusarvio

Alkuperäinen auditointi on **vahva lähdekoodin jäljitettävyydessä ja testiaukkojen
löytämisessä**, mutta **vain kohtalainen vakavuuksien kalibroinnissa**. Se löysi
todellisen P0:n ja kaikki tärkeät lifecycle-mekanismit, mutta sekoitti osin
mekanismin, mahdollisen vaikutuksen ja toistetun vaikutuksen sekä nosti muutamia
tuote-/estetiikkavalintoja vioiksi. Pääarvioinnin live- ja bad-value-testit poistivat
nämä väärät laajennukset.

Autoritatiivinen jatkojärjestys on
[`history/REMEDIATION_ROADMAP.md`](history/REMEDIATION_ROADMAP.md). GUI:n erillinen,
instrumentin estetiikkaa suojaava toteutusjärjestys on
[`history/GUI_DEVELOPMENT_PLAN.md`](history/GUI_DEVELOPMENT_PLAN.md).
