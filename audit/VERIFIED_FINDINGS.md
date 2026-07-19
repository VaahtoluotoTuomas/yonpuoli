# Varmennetut löydökset

Auditointipäivä: 2026-07-19
Arvioija: riippumaton pääarviointi `AUDIT_EVAL.md`:n mukaan

Tässä tiedostossa ovat vain lähdekoodista tarkistetut ja joko ajolla toistetut tai
deterministisesti toteutuksesta osoitetut löydökset. Prioriteetti kuvaa varmennettua
vaikutusta, ei alkuperäisen auditin arviota.

## Yhteenveto

| Prioriteetti | Määrä | Tunnisteet |
|---|---:|---|
| P0 | 1 | CQ-AUD-001 |
| P1 | 3 | CQ-AUD-002–004 |
| P2 | 9 | CQ-AUD-005–008, 011, 016–021 osin |
| P3 | 1 koonti | CQ-AUD-012, 013 ja 015 |

## P0

### CQ-AUD-001 — hyväksytty output-aliasointi voi summata limiterien jälkeen

**Tila:** varmennettu, P0, julkaisun estävä.

- `CQConfig` tarkistaa `outChans`- ja `stemOuts`-taulukot erikseen mutta ei niiden
  sisäistä yksikäsitteisyyttä tai joukkojen erillisyyttä
  (`classes/CQConfig.sc:106–111,149–157`). Konfiguraatiot kuten `outChans: [0,0]`
  ja main/stem-päällekkäisyys hyväksytään.
- Main ja kaksi stemmiä käyvät kukin oman turvaketjunsa läpi, minkä jälkeen ne
  kirjoitetaan erillisillä `Out.ar`-kutsuilla fyysisille busseille
  (`classes/CQSynthDefs.sc:312–355`). SuperColliderin `Out` summaa samalle bussille
  tehdyt kirjoitukset; summan jälkeen ei ole limiteriä.
- Kohdennettu konfiguraatiotesti hyväksyi duplikaatin ja main/stem-päällekkäisyyden.
  Säilytetystä nelikanavaisesta NRT-renderistä lasketut alias-summat olivat
  1,48687–1,73237 ja kaikkien neljän kanavan summa 3,21924 (+10,16 dBFS), vaikka
  yksittäiset kanavat olivat rajattuja.
- Fyysistä päällekkäistä ulostuloa ei turvallisuussyistä ajettu kaiuttimiin tai
  kuulokkeisiin; sitä ei tarvita mekanismin osoittamiseen.

**Vaikutus:** käyttäjän hyväksytty konfiguraatio voi ohittaa dokumentoidun 0,89:n
lopullisen katon ja stem-overlap voi lisäksi ohittaa `masterVol`-tason. Tämä rikkoo
projektin keskeisen audioturvallisuuslupauksen.

## P1

### CQ-AUD-002 — recorderin start/stop ei omista asynkronista työtä atomisesti

**Tila:** varmennettu, alennettu P0 → P1.

- `isRecording` perustuu vain `synth.notNil`-arvoon, vaikka `start` asettaa synthin
  vasta kahden `sync`-vaiheen jälkeen forkissa. Samat `path`, `buffer` ja `synth`
  -instanssimuuttujat jaetaan kaikille käynnistyksille
  (`classes/CQRecorder.sc:13–17,24–60`). `stop` poistuu heti, jos synth on vielä nil
  (`classes/CQRecorder.sc:63–76`).
- Hiljaisella live-serverillä nopea kaksois-start loi kaksi recorder-synthiä
  (node-ID:t 1012 ja 1013) samalle polulle. Yksi `stop` lopetti vain toisen ja jätti
  node 1012:n orvoksi. `start` + välitön `stop` päätyi myöhemmin tilaan
  `isRecording == true`.
- Luotu tiedosto oli tässä toistossa luettavissa. Korruptiota tai todellista
  tallennedatan menetystä ei osoitettu, joten alkuperäisen P0-väitteen vaikutus oli
  liian vahva.

**Vaikutus:** REC on nopeissa tai GUI:n toistetuissa kutsuissa epäluotettava,
resursseja voi jäädä eloon ja käyttäjän stop-komento voidaan käytännössä ohittaa.
Kyse on olennaisen ominaisuuden ja resurssiomistuksen P1-viasta, ei varmennetusta
P0-datanmenetyksestä.

### CQ-AUD-003 — `CQ.reboot` ei reboottaa scsynthiä

**Tila:** varmennettu, P1.

- `CQGraph.applyOptions` kirjoittaa sample raten, block sizen, laitteet ja I/O-määrät
  `ServerOptions`-olioon (`classes/CQGraph.sc:24–40`).
- `Cocoquantus.reboot` kutsuu vain asynkronista `free`-polkua ja 0,8 sekunnin päästä
  `boot`-polkua (`classes/Cocoquantus.sc:280–319`). `boot` käyttää
  `server.waitForBoot`-kutsua; jos serveri jo käy, se ei tee `Server.reboot`- tai
  quit/boot-operaatiota (`classes/Cocoquantus.sc:11–65`).
- Paikallisen SuperCollider 3.13:n `Server`-toteutus vahvistaa, että `waitForBoot`
  vain odottaa/käynnistää tarvittaessa, kun taas todellinen `reboot` lopettaa ja
  käynnistää serverin uudelleen.

**Vaikutus:** käyttäjän dokumentoitu reboot ei aktivoi käynnistysoptioita jo
käyvään serveriin. Laite-, sample rate-, block size- ja kanavamäärämuutokset voivat
jäädä hiljaisesti toteutumatta.

### CQ-AUD-004 — session boot/free/reboot ei ole kilpailuturvallinen

**Tila:** varmennettu, P1.

- `boot` julkaisee uuden `graph`-olion classvariin ennen `waitForBoot`-callbackia,
  ja callback käyttää myöhemmin samaa muuttuvaa classvaria. Elinkaarella ei ole
  tilaa, generation-tokenia, peruutusta tai virheen rollbackia
  (`classes/Cocoquantus.sc:11–65`).
- `free` on fire-and-forget-forkki ja nollaa jaetut viitteet myöhemmin
  (`classes/Cocoquantus.sc:280–312`).
- Kohdennetussa hiljaisessa live-toistossa välitön `CQ.boot` → `CQ.free` jätti
  serverin käyntiin mutta `graph == nil`; myöhäinen callback antoi
  `Message 'build' not understood`. `onReady` ei toteutunut.
- Sama omistusaukko kattaa osittaisen bootin virheet. GUI:n ja serverin
  odottamattoman kuoleman erilliset oireet on kirjattu alempana.

**Vaikutus:** nopea free/reboot, boot-virhe tai päällekkäinen toiminto voi jättää
serverin, facaden ja omistetut resurssit keskenään ristiriitaiseen tilaan.

## P2

### CQ-AUD-005 — deckin kesken oleva load voi herättää vapautetun deckin

**Tila:** varmennettu rajattuna, alennettu P1 → P2.

- `loadMono` tekee `Buffer.readChannel`-, `sync`-, normalisointi- ja
  synthinvaihtotyön forkissa ilman generation- tai cancellation-tokenia
  (`classes/CQDeck.sc:27–54`). `free` vapauttaa vain senhetkiset viitteet
  (`classes/CQDeck.sc:94–101`).
- Live-toistossa suuri load ja välitön `deck.free` johtivat myöhemmin tilaan, jossa
  deckillä oli taas bufferi/synth ja ladattu polku.
- Kahden eri tiedoston testissä valmistumisjärjestys ei rikkonut viimeisen pyynnön
  voittoa. Siksi alkuperäistä yleistä last-request-wins-väitettä ei merkitä
  toistetuksi.

**Vaikutus:** teardownin aikana voi syntyä orpo bufferi/synth tai vanhaan sessioon
kuuluva player. Normaali yksittäinen lataus toimi, joten P2 vastaa näyttöä.

### CQ-AUD-006 — Space- ja Quantussy-silmukat eivät palaudu bad values -tilasta

**Tila:** varmennettu rajattuna, alennettu P1 → P2.

- 47 samplea kestänyt NaN-syöte oikeaan Coco-silmukkaan ei saastuttanut loop-bufferia
  eikä Cocon myöhempää ulostuloa: 0 huonoa bufferisolua ja 0 Coco-bad-valuea.
  Alkuperäinen väite Cocon pysyvästä myrkyttymisestä hylättiin.
- Sama transientti oikean `cqSpace`- ja `cqQuantussy`-SynthDefin
  feedback/LocalIn-polkuun jäi eloon syötteen päätyttyä. Space-probessa oli 19 515
  huonoa näytettä, kaikki myöhäisessä ikkunassa; Quantussy-probessa 24 064, joista
  23 583 myöhäisessä ikkunassa.
- Master ja stem-ketju käyttävät `CheckBadValues`-squelchiä
  (`classes/CQSynthDefs.sc:321–323,387–391`), joten fyysinen ulostulo fail-closed
  mykistyy. Normaaleilla finite-signaaleilla NRT safety läpäisee.
- `post: 0` ei raportoi tapahtumaa, joten sisäinen saastuminen voi jäädä käyttäjältä
  näkymättömäksi.

**Vaikutus:** harvinainen plugin-/ulkoisen syötteen virhe voi mykistää Space- tai
Quantussy-osan session loppuun asti. Turvallisuusketju toimii; kyse on palautumis- ja
diagnostiikkaviasta. Korjaus ei saa clampata normaalia kaoottista finite-käyttäytymistä
eikä muuttaa feedbackin sointia.

### CQ-AUD-007 — patch-presetin sopimus on järjestysriippuvainen

**Tila:** käyttäytyminen varmennettu, P2-tuotepäätös.

- `patchPreset` tyhjentää koko matriisin, mutta kirjoittaa vain reseptissä mainitut
  parametrit (`classes/Cocoquantus.sc:203–216`). Puuttuvat parametrit jäävät
  edellisestä tilasta.
- Serveritön sekvenssitesti löysi reitillä `selfNoise → classic` 13 jäännösarvoa ja
  reitillä `quantumDrone → empty` 20 oletuksesta poikkeavaa arvoa.

**Vaikutus:** toiminto ei ole deterministinen täysi sound-preset, vaikka nimi ja osa
dokumentaatiosta antaa sen vaikutelman. Overlay voi olla täysin tarkoituksellinen ja
instrumentille sopiva; vika on määrittelemätön sopimus, ei emergentin käyttäytymisen
olemassaolo. Korjaus edellyttää ensin päätöstä preset vs patch-resepti.

### CQ-AUD-008 — linked stereo muuttuu dry-monitorissa dual monoksi

**Tila:** varmennettu, P2.

- `loadStereo` lataa vasemman kanavan Deck A:han ja oikean Deck B:hen
  (`classes/Cocoquantus.sc:139–148`).
- Jokainen mono deck-synth kirjoittaa oman signaalinsa molempiin dry-kanaviin
  (`classes/CQSynthDefs.sc:80–81`). Näin L ja R summataan kumpaankin dry-kanavaan.

**Vaikutus:** linked pairin kuiva stereokuva ei vastaa dokumentoitua L→A/R→B-
semantiikkaa. Cocon mono-syötteet voivat silti säilyä erillisinä; vika on
nimenomaan stereo dry-monitorissa.

### CQ-AUD-011 — konfiguroimaton mic näyttää virheellisesti live-tilaa

**Tila:** varmennettu, P2.

- `CQState` reitittää `micGain`-arvon DSP:lle vain, jos `micChan` ei ole nil
  (`classes/CQState.sc:70–72`).
- Facaden varoitus ja GUI:n punainen tila tarkistavat vain arvon `> 0`, eivät
  konfiguraatiota (`classes/Cocoquantus.sc:391–403`,
  `classes/CQGuiPanels.sc:125–133`).

**Vaikutus:** ohjain voi näyttää vaarallisen aktiiviselta, vaikka DSP-lähde on
hiljainen DC 0. Tämä on tilan totuudellisuusvika; tallennettu arvo voidaan silti
säilyttää tulevaa konfiguraatiota varten.

### CQ-AUD-018 — odottamaton serverin kuolema voi jättää facaden bootatuksi

**Tila:** lähdekoodista varmennettu, P2.

- `isBooted` perustuu `graph.notNil`- ja `graph.isRunning`-arvoihin
  (`classes/Cocoquantus.sc:67–69`). `CQGraph.isRunning` muuttuu falseksi normaalissa
  free- tai eksplisiittisessä `cmdPeriod`-polussa, mutta server quit/crash -watcheria
  ei rekisteröidä (`classes/CQGraph.sc:131,144–155`,
  `classes/Cocoquantus.sc:321–330`).

**Vaikutus:** ulkoisesti lopetettu tai kaatunut scsynth voi jättää API:n hyväksymään
komentoja kuolleeseen sessioon. Mekanismi on deterministinen; erillistä crash-live-
toistoa ei tehty.

### CQ-AUD-019 — GUI:n lykätty init voidaan suorittaa cleanupin jälkeen

**Tila:** lähdekoodista varmennettu, P2.

- `CQGui.new` palauttaa ennen kuin lykätty `prBuild/prListen/prStartRecTask` on
  suoritettu (`classes/CQGui.sc:22–23,52–64`).
- `free/prCleanup` asettaa `cleaned`-tilan, mutta jo jonossa oleva init ei tarkista
  sitä ennen ikkunan, controllerien ja taskin rakentamista
  (`classes/CQGui.sc:28–30,258–274`).

**Vaikutus:** välitön close/free tai toistuva `CQ.gui` voi rakentaa vapautetun
instanssin tai kaksoisresursseja. Qt-ajotestiä ei tässä ympäristössä tehty; suora
control-flow todentaa puuttuvan peruutusehdon.

### CQ-AUD-016, CQ-AUD-017 ja CQ-AUD-020 — regressioturvassa on varmennetut aukot

**Tila:** varmennettu, P2-laatuvelka.

- Nykyinen sclang-konfiguraatio ei sulje oletusarvoisia user/system-extension-
  polkuja, joten compile/unit ei ole täysin hermettinen.
- Automaatiosta puuttuvat live recorder/deck/session-lifecycle, server quit/recovery,
  GUI-lifecycle, GUI→State→route→DSP ja todellinen sample-rate/block-size-matriisi.
- GUI:n historiallinen scratch-smoke ei ole repossa toistettavana testinä.
- Tästä huolimatta nykyinen perusta on vahva: tuore ajo tuotti 3 082 unit-passia,
  0 failurea, kaikki NRT safety -skenaariot läpäisivät ja hold-testit läpäisivät.

**Vaikutus:** yllä olevat kilpailut ja esitysketjun katkokset voivat palata ilman
CI-signaalia; eri koneen extensionit voivat myös muuttaa testitulosta.

### CQ-AUD-021 — julkaisu-, turvallisuus- ja ominaisuusstatus ei ole yhtenäinen

**Tila:** varmennettu, P2-dokumentaatiovirhe.

- README ja PLAN ilmoittavat v1.0:n valmiiksi, vaikka monikanavainen live-stem-
  hyväksyntä on lykätty ja tämän auditin P0-konfiguraatiovika on avoin.
- `SPEC.md:311` antaa neljän `stemOuts`-arvon esimerkin, mutta config hyväksyy
  täsmälleen kaksi (`SPEC.md:445`, `classes/CQConfig.sc:111`).
- ARCHITECTURE kuvaa automaattisen MIDI-device watchin, kun toteutus tarjoaa
  eksplisiittisen rescan-polun.
- Dokumenteissa on myös vanhentuneita vastuukuvauksia, ja historiallista
  Coco-fidelityä koskeville väitteille ei ole repositoryssä lähdeluetteloa.

**Vaikutus:** käyttäjä ei voi luotettavasti päätellä, mikä on automatisoidusti,
manuaalisesti tai laitteistolla hyväksytty. Turvallisuusvastuu on ilmaistava erityisen
täsmällisesti.

## P3: varmennetut mutta valinnaiset GUI-puutteet

Nämä kuvaavat auditoitua v1.0-GUI:ta. Auditoinnin jälkeen hyväksytty
`GUI_SPEC.md` määrää uuden Archipelago-GUI:n ratkaisun; tämän osion aiemmat vapaat
responsive/Advanced-ehdotukset eivät ohita sitä.

### CQ-AUD-012, CQ-AUD-013 ja CQ-AUD-015 — esitystieto ja skaalautuvuus

**Tila:** varmennettu puute/riskirajaus, P3; ei julkaisun tekninen estäjä.

- Opt-in `autoTrim` voi kerryttää toteutuvaa trimmiä, mutta GUI näyttää vain
  hetkellisen −6 dB -ilmoituksen eikä pysyvää arvoa tai reset-toimintoa.
- Useilta jatkuvilta ohjaimilta puuttuvat yksiköt, selittävät tooltipit ja joiltakin
  liukusäätimiltä näkyvä arvo.
- Ikkuna alkaa kiinteästä 1120 × 740 -koosta, matriisi on kiinteärakenteinen eikä
  DPI/resize/näppäinnavigointia ole automatisoitu. Varsinaista leikkautumista tietyllä
  näytöllä ei tässä auditissa toistettu.

Nämä ovat totuudellisuus-, opittavuus- ja saavutettavuusparannuksia. Ne eivät saa
rikkoa globaaleja performance-oikoteitä tai tehdä headless-ajosta Qt-riippuvaista.

## Hylätyt, rajatut ja uudelleenluokitellut alkuperäislöydökset

| Alkuperäinen | Lopullinen ratkaisu | Peruste |
|---|---|---|
| CQ-AUD-002 P0 | P1 | Elinkaarivika ja orphan toistettiin; korruptiota tai datanmenetystä ei. |
| CQ-AUD-005 P1 | P2, rajattu | Free-resurrection toistettiin; last-request-wins-rikettä ei. |
| CQ-AUD-006 P1 | P2, rajattu | Space/Quantussy saastuvat, Coco ei; master fail-closed. |
| CQ-AUD-009 P2 | Ei varmennettu | Kaksi async-latausta on riskimekanismi, mutta sampletarkkaa starttivirhettä ei mitattu. |
| CQ-AUD-010 P2 | Info/hypoteesi | Offsetit ohittavat lagin, mutta kuultavaa zipper-virhettä ei osoitettu; smoothing voi muuttaa Quantussyn luonnetta. |
| CQ-AUD-012/013 | P3 | Todellisia GUI-puutteita, mutta oletuskäytön tekninen ydin toimii. |
| CQ-AUD-014 P2 | Ei vika | Space tarvitsee sendin ja returnin, Shake avoimia kohdepolkuja. Tämä on tarkoituksellista patch-topologiaa; vihjeet ovat valinnaisia. |
| CQ-AUD-015 P2 | P3-riski | Kiinteä layout ja testiaukko varmennettiin, konkreettista käyttökelvottomuutta ei. |
| CQ-AUD-022 P2 | Info / trust boundary | Snapshot on tarkoituksella executable SuperCollider -tekstitiedosto. Ongelma syntyy vain, jos epäluotettuja snapshotteja luvataan tuettavan. |
| CQ-AUD-023 P3 | Info / fidelity-päätös | `2^bits + 1` inklusiivista tasoa on matemaattinen fakta, mutta historiallista laitevastaavuutta tai kuultavaa vikaa ei osoitettu. |
| CQ-AUD-024 P3 | Valinnainen ylläpito | Projektin tiedostokokosuositus ylittyy; ei ajonaikainen vika. |
| CQ-AUD-025 P3 | Esteettinen mieltymys | Värien mahdollinen semanttinen päällekkäisyys ei ole toiminnallinen vika. |
| CQ-AUD-026 Info | Info / evidenssivaje | Puuttuva fidelity-lähdeketju on tosi, mutta ei ohjelmistovika. |

## Tarkistetut alueet, joista ei syntynyt löydöstä

- Server tree on oikeassa järjestyksessä: Sources → Quantussy → Cocos → FX → Master
  (`classes/CQGraph.sc:70–75,80–130`). Deck-playerit lisätään Sources-ryhmän alkuun
  ennen input-syntikkaa; recorder lisätään Master-ryhmän loppuun.
- Normaali finite-feedback pysyi NRT-ajoissa rajattuna. Space käyttää enintään 0,98:n
  feedbackia, Coco rajattua uudelleenkirjoitusta ja lopullinen master/stem-ketju
  LeakDC→tanh→Limiter→bad-value-squelch-rakennetta.
- Kaikki 79 `CQSpec`-parametria löytyivät tila-/reittikartasta; 71:llä on nykyinen
  GUI-ohjain ja kahdeksan ovat tarkoituksella vain API:ssa
  (`qShLag`, `c1Off`–`c5Off`, `spaceSize`, `spaceDamp`). Kooditarkastus ei löytänyt
  katkennutta näkyvä widget → `CQState` → DSP -reittiä.
- Space on valinnainen studioefekti, oletuksena pois, eikä alkuperäisen Cocon
  historiallinen osa. Sen metallinen FDN-luonne on avoin konseptipäätös, ei nykyisen
  toteutuksen oikeellisuusvika.
