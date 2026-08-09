# Ominaisuuksien jäljitettävyysauditointi (alkuperäinen auditointi)

Auditointipäivä: 2026-07-19

Tämä raportti jäljittää dokumentoidun ominaisuuspinnan GUI:n, tila/API-reitityksen,
serverin DSP:n ja testien läpi. Raportti on kuvaileva: se ei muuta toteutusta eikä
määrittele spesifikaatiota uudelleen.

## Rajaus ja lukumäärät

CQSpec.sc:151-217 määrittelee täsmälleen 79 parametria:

- 8 lähde-/deck-parametria
- 6 preamp-parametria
- 20 parametria kummallekin kahdesta Cocosta, yhteensä 40
- 18 Quantussy-parametria
- 1 matriisin syvyysmakro
- 6 Space-/Master-parametria

GUI näyttää 71 parametria 79:stä. Täsmälleen kahdeksan vain API:ssa olevaa
parametria ovat **qShLag**, **c1Off**, **c2Off**, **c3Off**, **c4Off**,
**c5Off**, **spaceSize** ja **spaceDamp**. Kaikki kahdeksan ovat silti CQSpecissä,
CQState tallentaa ne, ne voidaan reitittää CQ.set-kutsulla ja DSP käyttää niitä.

## Evidenssiavain

Dokumentaatio:

- D1: SPEC.md:173-187, lähteet ja deckit
- D2: SPEC.md:188-200, preampit
- D3: SPEC.md:201-229, Coco-pari
- D4: SPEC.md:230-261, Quantussy
- D5: SPEC.md:262-283, matriisimakro, presetit ja shake
- D6: SPEC.md:284-307, Space, Master ja watchdog
- D7: SPEC.md:308-317, tallennus ja stemit
- D8: SPEC.md:338-360, 16n
- D9: SPEC.md:361-409, GUI ja julkinen API
- D10: SPEC.md:410-428, pattern-toiminnot
- D11: SPEC.md:429-493, konfiguraatio ja turvallisuus

Testit ja havaittu evidenssi:

- T-SPEC: tests/classes/CQSpecTest.sc:9-132 tarkistaa 79 parametrin määrän,
  jokaisen oletusarvon clampin kautta, virheelliset/fuzzatut arvot, tyypit,
  mappaukset, lag-arvot ja 16n-mappauksen.
- T-STATE: tests/classes/CQStateTest.sc:16-51 tarkistaa tilan clamp-/oletus-/
  observer-suppilon edustavilla parametreilla. Testi käyttää graph=nil-arvoa eikä
  siksi varmista serveriargumenttien reititystä.
- T-GUI: tests/manual_checklist.md:389-514 tallentaa historiallisen GUI:n,
  kaksisuuntaisen tilan, mittarien, LEDien, matriisin, dropin, oikoteiden,
  presettien, shaken ja REC-toiminnon tarkistukset. Repossa ei ole commitoitua
  automatisoitua CQGui-testiä.
- T-DECK: tests/classes/CQDeckNavTest.sc:35-101 tarkistaa skannauksen ja
  polkunavigoinnin. Varsinainen Buffer-lataus ja cqDeck-ääni ovat edelleen
  live-/manuaalievidenssiä tiedostossa tests/manual_checklist.md:357-383.
- T-COCO: tests/nrt_safety.scd:77-110 ja 141-198 ajavat todellisia cqInput- ja
  cqCoco-SynthDefejä pahimman tapauksen asetuksilla.
  tests/nrt_hold_integrity.scd:46-175 todistaa erikseen Hold-loopin.
  Kuunteluevidenssi on riveillä tests/manual_checklist.md:30-104 ja 180-263.
- T-QUANT: tests/nrt_safety.scd:114-138 ja 200-226 ajavat todellista Quantussya
  ja yhtä castle-FM-reittiä. Kuunteluevidenssi, qShLag mukaan lukien, on
  tiedostossa tests/manual_checklist.md:107-153.
- T-MATRIX: tests/classes/CQMatrixTest.sc:20-225 tarkistaa matriisinimet, clampit,
  latauksen/tyhjennyksen, preset-datan ja shaken; tests/nrt_safety.scd:141-226
  ajaa täyden mayhem-matriisin ja yhden FM-kohteen.
- T-SPACE: tests/nrt_safety.scd:229-255 todistaa ei-mykkää, rajattua FDN-paluuta,
  kun size/damp annetaan suoraan SynthDefeille. Manuaalinen mix-/size-/damp-
  evidenssi on tiedostossa tests/manual_checklist.md:523-535.
- T-MASTER: tests/nrt_safety.scd:40-75 ja 369-498 todistavat Master-ulostulon
  rajauksen, DC-rajat ja stem-turvallisuuden suorilla SynthDef-argumenteilla.
- T-SNAP: tests/classes/CQSnapshotTest.sc:39-188 tarkistaa tilan edestakaisen
  tallennuksen, matriisin korvauksen, validoinnin, mic-/panic-politiikan ja morphin
  valmistumisen.
- T-MIDI: tests/classes/CQMidi16nTest.sc:23-105 tarkistaa mappauksen, linkitetyt
  slotit ja takeover-logiikan ilman serveriä; oikean laitteen tarkistus on
  historiallista manuaalievidenssiä tiedostossa tests/manual_checklist.md:271-310.

Auditin käännösmarkkeri oli CQ_COMPILE_OK. Koko unit-testisarja raportoi 3082
hyväksyttyä assertiota ja 0 epäonnistumista. Alla olevat NRT-viitteet tunnistavat repossa
olevan testikattavuuden; tässä feature-auditissa ei ajettu uudelleen
äänilaitteisto- tai visuaalisia Qt-tarkistuksia.

## Kaikki 79 CQSpec-parametria

### Lähteet ja deckit: 8

| Parametri | Dokumentaatio | GUI | Tila-/API-reitti | DSP-kohde | Testievidenssi | Tila |
|---|---|---|---|---|---|---|
| **liveGain** | D1, D9 | SOURCES-osion live-nuppi, CQGuiPanels.sc:128-129 | CQ.set → CQState → input.liveGain, CQState.sc:68-69 | cqInput skaalaa liveL/liveR SoundIn -signaaleja, CQSynthDefs.sc:31-33 | T-SPEC, T-STATE; M6-manuaali riveillä 183-204; ei erillistä automatisoitua audiovaikutuksen assertiota | GUI + API + DSP |
| **micGain** | D1, D11 | SOURCES-osion mic-nuppi ja punainen varoitus, CQGuiPanels.sc:127-133 | Tallennetaan aina; reititetään input.micGain-argumenttiin vain, kun graph.config.micChan ei ole nil, CQState.sc:70-71 | cqInput skaalaa konfiguroidun mikrofonin SoundIn-signaalia; muulloin mic on DC 0, CQSynthDefs.sc:34-40 | T-SPEC; T-SNAPin mic-politiikka; manuaali 344-349 ja 422-434; ei erillistä automatisoitua audiovaikutuksen assertiota | GUI + ehdollinen reitti + DSP; affordanssivirhe CQ-AUD-011 |
| **deckALevel** | D1 | DECKA-osion level-nuppi, CQGuiPanels.sc:257-261 | CQState → deckA.level, CQState.sc:107-110; myös CQDeck kirjoittaa tilan kautta | cqDeckin level, CQSynthDefs.sc:68-85 | T-SPEC, T-DECK-manuaali; linkitetty MIDI-slot testataan T-MIDIssä | GUI + API + DSP deckin lataamisen jälkeen |
| **deckBLevel** | D1 | DECKB-osion level-nuppi, CQGuiPanels.sc:257-261 | CQState → deckB.level, CQState.sc:107-110 | cqDeckin level, CQSynthDefs.sc:68-85 | T-SPEC, T-DECK-manuaali; linkitetty MIDI-slot testataan T-MIDIssä | GUI + API + DSP deckin lataamisen jälkeen |
| **deckARate** | D1 | DECKA-osion rate-nuppi, CQGuiPanels.sc:257-261 | CQState → deckA.rate, CQState.sc:107-110 | cqDeckin toistonopeus sekä matriisin nopeusmodulaatio, CQSynthDefs.sc:68-85 | T-SPEC, T-DECK, T-MIDI; live reverse/rate -manuaali 357-383 | GUI + API + DSP deckin lataamisen jälkeen |
| **deckBRate** | D1 | DECKB-osion rate-nuppi, CQGuiPanels.sc:257-261 | CQState → deckB.rate, CQState.sc:107-110 | cqDeckin toistonopeus sekä matriisin nopeusmodulaatio, CQSynthDefs.sc:68-85 | T-SPEC, T-DECK, T-MIDI; live reverse/rate -manuaali 357-383 | GUI + API + DSP deckin lataamisen jälkeen |
| **deckALoop** | D1 | DECKA-osion loop-kytkin, CQGuiPanels.sc:257-261 | CQState → deckA.loop, CQState.sc:107-110 | cqDeckin PlayBuf-loop, CQSynthDefs.sc:68-85 | T-SPEC; manuaalinen T-DECK; ei erillistä automatisoitua DSP-assertiota | GUI + API + DSP deckin lataamisen jälkeen |
| **deckBLoop** | D1 | DECKB-osion loop-kytkin, CQGuiPanels.sc:257-261 | CQState → deckB.loop, CQState.sc:107-110 | cqDeckin PlayBuf-loop, CQSynthDefs.sc:68-85 | T-SPEC; manuaalinen T-DECK; ei erillistä automatisoitua DSP-assertiota | GUI + API + DSP deckin lataamisen jälkeen |

### Preampit: 6

| Parametri | Dokumentaatio | GUI | Tila-/API-reitti | DSP-kohde | Testievidenssi | Tila |
|---|---|---|---|---|---|---|
| **preASrc** | D2 | pre A:n lähde-enum, CQGuiPanels.sc:137-141 | CQState → input.preASrc, CQState.sc:68-69 | cqInputin Select valitsee liveL/liveR/mic/deckA/deckB/none, CQSynthDefs.sc:25-49 | T-SPEC; T-SNAPin enumin edestakainen tallennus; T-COCO NRT käyttää deck-lähdettä suoraan; GUI/manuaali 422-434 | GUI + API + DSP |
| **preBSrc** | D2 | pre B:n lähde-enum, CQGuiPanels.sc:142-146 | CQState → input.preBSrc, CQState.sc:68-69 | cqInputin Select valitsee liveL/liveR/mic/deckA/deckB/none, CQSynthDefs.sc:25-53 | T-SPEC; T-COCO NRT käyttää deck-lähdettä suoraan; GUI/manuaali 422-434 | GUI + API + DSP |
| **preADrive** | D2 | pre A:n drive-nuppi, CQGuiPanels.sc:137-141 | CQState → input.preADrive, CQState.sc:68-69 | cqInputin epälineaarinen preamp A, CQSynthDefs.sc:46-49 | T-SPEC, T-MIDIn linkitetty slot; T-COCOn suora pahimman tapauksen DSP-testi; manuaali 422-434 | GUI + API + DSP |
| **preBDrive** | D2 | pre B:n drive-nuppi, CQGuiPanels.sc:142-146 | CQState → input.preBDrive, CQState.sc:68-69 | cqInputin epälineaarinen preamp B, CQSynthDefs.sc:50-53 | T-SPEC, T-MIDIn linkitetty slot; T-COCOn suora pahimman tapauksen DSP-testi; manuaali 422-434 | GUI + API + DSP |
| **preABias** | D2 | pre A:n bias-nuppi, CQGuiPanels.sc:137-141 | CQState → input.preABias, CQState.sc:68-69 | cqInputin epälineaarisen preamp A:n bias, CQSynthDefs.sc:46-49 | T-SPEC; T-COCOn suora pahimman tapauksen DSP-testi; manuaali 422-434 | GUI + API + DSP |
| **preBBias** | D2 | pre B:n bias-nuppi, CQGuiPanels.sc:142-146 | CQState → input.preBBias, CQState.sc:68-69 | cqInputin epälineaarisen preamp B:n bias, CQSynthDefs.sc:50-53 | T-SPEC; T-COCOn suora pahimman tapauksen DSP-testi; manuaali 422-434 | GUI + API + DSP |

### Coco 1: 20

Kaikki Coco 1:n ohjaimet näkyvät tiedostossa CQGuiPanels.sc:21-61, reitittyvät
tiedoston CQState.sc:81-96 kautta ja päätyvät cqCocoon tiedostossa
CQSynthDefs.sc:172-259.

| Parametri | Dokumentaatio | GUI | Tila-/API-reitti | DSP-kohde | Testievidenssi | Tila |
|---|---|---|---|---|---|---|
| **coco1In** | D3 | in-nuppi, rivi 26 | CQState → coco1.inLevel | sisääntulon VCA ennen tallennuspolkua | T-SPEC, T-MIDI, T-COCO | GUI + API + DSP |
| **coco1Fb** | D3 | fb-nuppi, rivi 29 | CQState → coco1.fbLevel | feedback-VCA ennen tallennuspolkua | T-SPEC, T-STATE, T-MIDI, T-COCO; feedback-kuuntelu M2 | GUI + API + DSP |
| **coco1Speed** | D3 | speed-nuppi, rivi 27 | CQState → coco1.speed | loop-kellon/phasorin nopeus | T-SPEC, T-MIDI, T-COCO; nopeuden kuuntelu M1 | GUI + API + DSP |
| **coco1Spaf** | D3 | spaf-nuppi, rivi 28 | CQState → coco1.spafKnob | SP.AF-kellomodulaation skaala | T-SPEC, T-MIDI, T-MATRIX, T-COCO | GUI + API + DSP |
| **coco1DolbyIn** | D3 | dolby in -enum, rivit 48-49 | CQState → coco1.dolbyIn | sisääntulon gate/duck-valitsin | T-SPEC; manuaali 64-79 ja 422-434; ei erillistä NRT-käyttäytymisassertiota | GUI + API + DSP |
| **coco1DolbyFb** | D3 | dolby fb -enum, rivit 48-49 | CQState → coco1.dolbyFb | feedbackin gate/duck-valitsin | T-SPEC; manuaali 64-79 ja 422-434; ei erillistä NRT-käyttäytymisassertiota | GUI + API + DSP |
| **coco1Hold** | D3 | HOLD-kytkin, rivi 45 | CQState → coco1.hold; CQ.hold-facade | valitsee uuden tallennuskirjoituksen tai identiteettiuudelleenkirjoituksen | T-SPEC, T-SNAP, T-COCO; erillinen hold-integrity-NRT | GUI + API + DSP |
| **coco1Flip** | D3 | FLIP-kytkin, rivit 45-46 | CQState → coco1.flip; CQ.flip-facade | kääntää kellon suunnan; myös matriisitriggeri voi aktivoida flipin | T-SPEC; manuaali 64-79 ja GUI 422-434 | GUI + API + DSP |
| **coco1Bits** | D3 | kokonaislukukenttä, rivit 50-54 | CQState → coco1.bits | muuntimen kvantisoinnin bittisyvyys | T-SPEC, T-SNAP; T-COCOn suora pahimman tapauksen DSP | GUI + API + DSP |
| **coco1Wrap** | D3 | wrap-kytkin, rivit 50-54 | CQState → coco1.wrap | valitsee clip2:n tai wrap2:n ennen kvantisointia | T-SPEC; T-COCOn suora pahimman tapauksen DSP | GUI + API + DSP |
| **coco1Interp** | D3 | interpolaatio-enum, rivit 50-54 | CQState → coco1.interp | valitsee ZOH:n tai silotetun loop-tapin | T-SPEC; T-COCO; luonteesta vain kuunteluevidenssi | GUI + API + DSP |
| **coco1Dust** | D3 | dust-nuppi, rivi 55 | CQState → coco1.dust | LF-moduloitu WhiteNoise syötetään tallennuspolkuun | T-SPEC; T-COCO; manuaalinen digital dust 85-104 | GUI + API + DSP |
| **coco1Organ** | D3 | organ-nuppi, rivi 56 | CQState → coco1.organLvl | osoitelaskuriurkutapin taso | T-SPEC; preset-validointi T-MATRIX; manuaalinen organ-preset 438-456 | GUI + API + DSP |
| **coco1Level** | D3 | level-nuppi, rivi 57 | CQState → coco1.level | äänen jälkeinen taso ennen Coco-väylää/sendiä | T-SPEC; T-COCO | GUI + API + DSP |
| **coco1Pan** | D3 | pan-nuppi, rivi 58 | CQState haarautuu kohteisiin master.pan1 ja coco1.pan | kuivan Master-signaalin sijoitus ja panoroitu stereo-Space-send | T-SPEC; T-SPACEn/T-MASTERin suora DSP; ei erillistä reittitestiä | GUI + API + kaksi DSP-reittiä |
| **coco1Send** | D3, D6 | send-nuppi, rivi 59 | CQState → coco1.spaceSend | post-fader-send stereona Space-sisääntuloon | T-SPEC, T-SPACEn suora DSP, manuaalinen Space 523-535 | GUI + API + DSP |
| **coco1SlotPre** | D3 | pre-slotin liukusäädin, rivit 34-43 | CQState → coco1.slotPre | preamp A:n osuus loopin sisääntuloon | T-SPEC, preset-validointi T-MATRIX, T-COCO | GUI + API + DSP |
| **coco1SlotDeck** | D3 | deck-slotin liukusäädin, rivit 34-43 | CQState → coco1.slotDeck | deck A:n osuus loopin sisääntuloon | T-SPEC, preset-validointi T-MATRIX, T-COCO | GUI + API + DSP |
| **coco1SlotQ** | D3, D4 | q-slotin liukusäädin, rivit 34-43 | CQState → coco1.slotQ | Quantussy-mixin osuus loopin sisääntuloon | T-SPEC, T-QUANT ja T-COCO; manuaali M3 | GUI + API + DSP |
| **coco1SlotX** | D3 | x-slotin liukusäädin, rivit 34-43 | CQState → coco1.slotX | yhden blokin viivästetty Coco 2 -ristisyöttö | T-SPEC, preset-validointi T-MATRIX, T-COCO | GUI + API + DSP |

### Coco 2: 20

Toinen paneeli tuotetaan samalla rakentajalla, ja sillä on sama toteutus- ja
testipinta. Sen vastaavat sisääntulot ovat preamp B, deck B ja Coco 1.

| Parametri | Dokumentaatio | GUI | Tila-/API-reitti | DSP-kohde | Testievidenssi | Tila |
|---|---|---|---|---|---|---|
| **coco2In** | D3 | in-nuppi, rivi 26 | CQState → coco2.inLevel | sisääntulon VCA ennen tallennuspolkua | T-SPEC, T-MIDI, T-COCO | GUI + API + DSP |
| **coco2Fb** | D3 | fb-nuppi, rivi 29 | CQState → coco2.fbLevel | feedback-VCA ennen tallennuspolkua | T-SPEC, T-MIDI, T-COCO; feedback-kuuntelun M2-rakenne | GUI + API + DSP |
| **coco2Speed** | D3 | speed-nuppi, rivi 27 | CQState → coco2.speed | loop-kellon/phasorin nopeus | T-SPEC, T-MIDI, T-COCO | GUI + API + DSP |
| **coco2Spaf** | D3 | spaf-nuppi, rivi 28 | CQState → coco2.spafKnob | SP.AF-kellomodulaation skaala | T-SPEC, T-MIDI, T-MATRIX, T-COCO | GUI + API + DSP |
| **coco2DolbyIn** | D3 | dolby in -enum, rivit 48-49 | CQState → coco2.dolbyIn | sisääntulon gate/duck-valitsin | T-SPEC; symmetrinen toteutus; manuaalinen GUI-käyttäytyminen | GUI + API + DSP |
| **coco2DolbyFb** | D3 | dolby fb -enum, rivit 48-49 | CQState → coco2.dolbyFb | feedbackin gate/duck-valitsin | T-SPEC; symmetrinen toteutus; manuaalinen GUI-käyttäytyminen | GUI + API + DSP |
| **coco2Hold** | D3 | HOLD-kytkin, rivi 45 | CQState → coco2.hold; CQ.hold-facade | valitsee uuden tallennuskirjoituksen tai identiteettiuudelleenkirjoituksen | T-SPEC, T-STATE, T-SNAP, T-COCO; erillinen hold-integrity-rakenne kattaa cqCocon | GUI + API + DSP |
| **coco2Flip** | D3 | FLIP-kytkin, rivit 45-46 | CQState → coco2.flip; CQ.flip-facade | kääntää kellon suunnan; myös matriisitriggeri voi aktivoida flipin | T-SPEC; manuaalinen GUI-/toimintoevidenssi | GUI + API + DSP |
| **coco2Bits** | D3 | kokonaislukukenttä, rivit 50-54 | CQState → coco2.bits | muuntimen kvantisoinnin bittisyvyys | T-SPEC; symmetrinen cqCoco; T-COCOn suora pahin tapaus | GUI + API + DSP |
| **coco2Wrap** | D3 | wrap-kytkin, rivit 50-54 | CQState → coco2.wrap | valitsee clip2:n tai wrap2:n ennen kvantisointia | T-SPEC; T-COCOn suora pahin tapaus | GUI + API + DSP |
| **coco2Interp** | D3 | interpolaatio-enum, rivit 50-54 | CQState → coco2.interp | valitsee ZOH:n tai silotetun loop-tapin | T-SPEC; symmetrinen cqCoco; kuunteluevidenssi | GUI + API + DSP |
| **coco2Dust** | D3 | dust-nuppi, rivi 55 | CQState → coco2.dust | LF-moduloitu WhiteNoise syötetään tallennuspolkuun | T-SPEC; T-COCO; manuaalinen selfNoise | GUI + API + DSP |
| **coco2Organ** | D3 | organ-nuppi, rivi 56 | CQState → coco2.organLvl | osoitelaskuriurkutapin taso | T-SPEC; preset-validointi T-MATRIX; manuaalinen organ-preset | GUI + API + DSP |
| **coco2Level** | D3 | level-nuppi, rivi 57 | CQState → coco2.level | äänen jälkeinen taso ennen Coco-väylää/sendiä | T-SPEC; T-COCO | GUI + API + DSP |
| **coco2Pan** | D3 | pan-nuppi, rivi 58 | CQState haarautuu kohteisiin master.pan2 ja coco2.pan | kuivan Master-signaalin sijoitus ja panoroitu stereo-Space-send | T-SPEC; T-SPACEn/T-MASTERin suora DSP; ei erillistä reittitestiä | GUI + API + kaksi DSP-reittiä |
| **coco2Send** | D3, D6 | send-nuppi, rivi 59 | CQState → coco2.spaceSend | post-fader-send stereona Space-sisääntuloon | T-SPEC, T-SPACEn suora DSP, manuaalinen Space | GUI + API + DSP |
| **coco2SlotPre** | D3 | pre-slotin liukusäädin, rivit 34-43 | CQState → coco2.slotPre | preamp B:n osuus loopin sisääntuloon | T-SPEC, preset-validointi T-MATRIX, T-COCO | GUI + API + DSP |
| **coco2SlotDeck** | D3 | deck-slotin liukusäädin, rivit 34-43 | CQState → coco2.slotDeck | deck B:n osuus loopin sisääntuloon | T-SPEC, preset-validointi T-MATRIX, T-COCO | GUI + API + DSP |
| **coco2SlotQ** | D3, D4 | q-slotin liukusäädin, rivit 34-43 | CQState → coco2.slotQ | Quantussy-mixin osuus loopin sisääntuloon | T-SPEC, T-QUANT ja T-COCO; manuaalinen quantumDrone | GUI + API + DSP |
| **coco2SlotX** | D3 | x-slotin liukusäädin, rivit 34-43 | CQState → coco2.slotX | yhden blokin viivästetty Coco 1 -ristisyöttö | T-SPEC, preset-validointi T-MATRIX, T-COCO | GUI + API + DSP |

### Quantussy: 18

Quantussy-reitit rakennetaan tiedostossa CQState.sc:98-104, ja cqQuantussy käyttää
niitä tiedostossa CQSynthDefs.sc:96-163. GUI näyttää tarkoituksella taajuuden,
kaaoksen, viisi range-arvoa ja viisi sendiä, mutta ei sample-and-hold-lagia eikä
viittä offsetia.

| Parametri | Dokumentaatio | GUI | Tila-/API-reitti | DSP-kohde | Testievidenssi | Tila |
|---|---|---|---|---|---|---|
| **qFreq** | D4 | qFreq-nuppi, CQGuiPanels.sc:110-111 | CQState → quantussy.qFreq | yhteinen perustaajuus castle-taajuusyhtälössä | T-SPEC, T-STATEn oletukset, T-MIDI, T-QUANT, manuaali M3 | GUI + API + DSP |
| **qChaos** | D4 | qChaos-nuppi, CQGuiPanels.sc:110-111 | CQState → quantussy.qChaos | sample-and-hold-sävelkorkeuskaaoksen syvyys | T-SPEC, T-STATEn observer, T-MIDI, T-QUANT, T-SNAP | GUI + API + DSP |
| **qShLag** | D4 | Ei ole | CQState → quantussy.qShLag | Lag castle-neliö-/tilaulostulossa, CQSynthDefs.sc:108,144 | T-SPEC; eksplisiittinen manuaalivertailu riveillä 150-153; ei erillistä automatisoitua DSP-vaikutuksen assertiota | **Vain API:ssa (API-only)** + DSP |
| **c1Range** | D4 | C1:n range-enum, CQGuiPanels.sc:95-105 | CQState → quantussy.range1 | C1:n taajuusaluekerroin | T-SPEC, T-QUANTin suora DSP, T-MATRIXin preset-validius | GUI + API + DSP |
| **c2Range** | D4 | C2:n range-enum, CQGuiPanels.sc:95-105 | CQState → quantussy.range2 | C2:n taajuusaluekerroin | T-SPEC, T-QUANTin suora DSP, T-MATRIXin preset-validius | GUI + API + DSP |
| **c3Range** | D4 | C3:n range-enum, CQGuiPanels.sc:95-105 | CQState → quantussy.range3 | C3:n taajuusaluekerroin | T-SPECin oletus, T-QUANTin suora DSP, T-MATRIXin preset-validius | GUI + API + DSP |
| **c4Range** | D4 | C4:n range-enum, CQGuiPanels.sc:95-105 | CQState → quantussy.range4 | C4:n taajuusaluekerroin | T-SPEC, T-QUANTin suora DSP, T-MATRIXin preset-validius | GUI + API + DSP |
| **c5Range** | D4 | C5:n range-enum, CQGuiPanels.sc:95-105 | CQState → quantussy.range5 | C5:n taajuusaluekerroin | T-SPEC, T-QUANTin suora DSP, T-MATRIXin preset-validius | GUI + API + DSP |
| **c1Off** | D4 | Ei ole | CQState → quantussy.off1 | C1:n oktaavisiirtymä kahden potenssin offset-termissä | Vain yleinen T-SPEC; ei suoraa reitti-/käyttäytymisassertiota | **Vain API:ssa (API-only)** + DSP; kohdennettu testiaukko |
| **c2Off** | D4 | Ei ole | CQState → quantussy.off2 | C2:n oktaavisiirtymä kahden potenssin offset-termissä | Vain yleinen T-SPEC; ei suoraa reitti-/käyttäytymisassertiota | **Vain API:ssa (API-only)** + DSP; kohdennettu testiaukko |
| **c3Off** | D4 | Ei ole | CQState → quantussy.off3 | C3:n oktaavisiirtymä kahden potenssin offset-termissä | Vain yleinen T-SPEC; ei suoraa reitti-/käyttäytymisassertiota | **Vain API:ssa (API-only)** + DSP; kohdennettu testiaukko |
| **c4Off** | D4 | Ei ole | CQState → quantussy.off4 | C4:n oktaavisiirtymä kahden potenssin offset-termissä | Vain yleinen T-SPEC; ei suoraa reitti-/käyttäytymisassertiota | **Vain API:ssa (API-only)** + DSP; kohdennettu testiaukko |
| **c5Off** | D4 | Ei ole | CQState → quantussy.off5 | C5:n oktaavisiirtymä kahden potenssin offset-termissä | T-SPEC; matrix-FM-NRT antaa off5:n suoraan tiedostossa tests/nrt_safety.scd:217 | **Vain API:ssa (API-only)** + DSP; reitti yhä testaamatta |
| **c1Send** | D4 | C1:n send-liukusäädin, CQGuiPanels.sc:95-105 | CQState → quantussy.send1 | C1:n osuus qMix-väylään | T-SPEC, T-QUANT, T-MATRIXin preset-validius | GUI + API + DSP |
| **c2Send** | D4 | C2:n send-liukusäädin, CQGuiPanels.sc:95-105 | CQState → quantussy.send2 | C2:n osuus qMix-väylään | T-SPEC, T-QUANT, T-MATRIXin preset-validius | GUI + API + DSP |
| **c3Send** | D4 | C3:n send-liukusäädin, CQGuiPanels.sc:95-105 | CQState → quantussy.send3 | C3:n osuus qMix-väylään | T-SPEC, T-QUANT, T-MATRIXin preset-validius | GUI + API + DSP |
| **c4Send** | D4 | C4:n send-liukusäädin, CQGuiPanels.sc:95-105 | CQState → quantussy.send4 | C4:n osuus qMix-väylään | T-SPEC, T-QUANT, T-MATRIXin preset-validius | GUI + API + DSP |
| **c5Send** | D4 | C5:n send-liukusäädin, CQGuiPanels.sc:95-105 | CQState → quantussy.send5 | C5:n osuus qMix-väylään | T-SPECin oletus, T-QUANT, T-MATRIXin preset-validius | GUI + API + DSP |

### Matriisimakro: 1

| Parametri | Dokumentaatio | GUI | Tila-/API-reitti | DSP-kohde | Testievidenssi | Tila |
|---|---|---|---|---|---|---|
| **patchDepth** | D5 | MASTER-osion depth-nuppi, CQGuiPanels.sc:203-206 | CQState haarautuu coco1-, coco2-, quantussy-, deckA- ja deckB-syntikoiden patchDepth-argumentteihin, CQState.sc:75-77 | kertoo jokaisen silotetun matriisipainon tiedostossa CQSynthDefs.sc:420-423 | T-SPECin lag/map, T-MIDI F15, T-MATRIX, mayhem/FM-NRT | GUI + API + viisi DSP-reittiä |

### Space ja Master: 6

| Parametri | Dokumentaatio | GUI | Tila-/API-reitti | DSP-kohde | Testievidenssi | Tila |
|---|---|---|---|---|---|---|
| **spaceMix** | D6 | MASTER-osion space-nuppi, CQGuiPanels.sc:203-206 | CQState → master.spaceMix, CQState.sc:73 | cqMaster skaalaa stereo-FDN-paluuta, CQSynthDefs.sc:300,306-307 | T-SPEC, T-MIDI F14, T-SPACE, manuaali 523-535 | GUI + API + DSP; riippuvuuden affordanssi CQ-AUD-014 |
| **spaceSize** | D6 | Ei ole | CQState → space.spaceSize, CQState.sc:74 | cqSpacen viiveiden skaalaus/feedback-luonne, CQSynthDefs.sc:265-288 | T-SPEC; T-SPACEn suora SynthDef; manuaali 532-535; tilareittiä ei ole testattu suoraan | **Vain API:ssa (API-only)** + DSP; GUI-konsepti on eksplisiittisesti lykätty tiedostossa docs/history/PLAN_v1.0.md:687-700 |
| **spaceDamp** | D6 | Ei ole | CQState → space.spaceDamp, CQState.sc:74 | cqSpacen vaimennussuodatin feedback-verkossa, CQSynthDefs.sc:267,275-288 | T-SPEC; T-SPACEn suora SynthDef; manuaali 532-535; tilareittiä ei ole testattu suoraan | **Vain API:ssa (API-only)** + DSP; GUI-konsepti on eksplisiittisesti lykätty tiedostossa docs/history/PLAN_v1.0.md:687-700 |
| **masterDry** | D6 | MASTER-osion dry-nuppi, CQGuiPanels.sc:203-206 | CQState → master.masterDry, CQState.sc:73 | cqMasterin kuivan lähteen osuus, CQSynthDefs.sc:301,306-307 | T-SPEC, T-MASTERin suora DSP, laaja käyttö manuaalin asetelmissa | GUI + API + DSP |
| **masterVol** | D6 | MASTER-osion vol-nuppi, CQGuiPanels.sc:203-206 | CQState → master.masterVol, CQState.sc:73 | dB-gain ennen turvaketjua, CQSynthDefs.sc:302 | T-SPEC, T-MIDI F16, T-MASTERin pahin tapaus, watchdog-manuaali | GUI + API + DSP |
| **monitorMute** | D6, D11 | PANIC/MUTED-painike ja välilyöntioikotie, CQGuiPanels.sc:165-193; CQGui.sc:213-215 | CQState → master.mute, CQState.sc:78; CQ.panic/CQ.resume-facade | 30 ms mykistys Masteriin ja stemeihin, CQSynthDefs.sc:303-307,349-350 | T-SPEC; T-SNAP todistaa, ettei panic palaudu recallissa; manuaaliset GUI- ja stem-panic-tarkistukset | GUI-toiminto + API + DSP |

## Ei-parametristen ominaisuuksien jäljitettävyys

| Ominaisuus | Dokumentaatio | GUI / operaattorin pinta | Toteutuspolku | Testievidenssi | Tila ja audit-huomio |
|---|---|---|---|---|---|
| Boot-, free-, reboot-, Cmd-period- ja headless-elinkaari | README.md:18-42; ARCHITECTURE.md:n elinkaarikohdat; D9-D11 | Automaattinen avaus, ellei headless; CQ.gui voi avata uudelleen | Cocoquantus.sc:11-64,280-325; CQGraph.sc:44-139 | Konfiguraation unit-testit; manuaalinen headless 495-513; käännöstarkistus | Toteutettu. Lykätty GUI-rakennus tekee välittömästi toistetuista CQ.gui-kutsuista elinkaarikilvan; CQ-AUD-019 |
| GUI:n elinkaari ja siivous | D9 | Ikkunan sulkeminen/uudelleenavaus; tarkoitus on yksi paneeli | CQGui.sc:22-31,52-64,258-274; Cocoquantus.sc:218-225 | Vain historiallinen manuaalinen sulkemis-/headless-evidenssi; ei commitoitua automaatiota | Siivous on harkitusti idempotentti, mutta build/open-kilpa ja automaatioaukko ovat jäljellä; CQ-AUD-019, CQ-AUD-020 |
| Deck A/B:n lataus, tiedostonimi, seuraava, edellinen, satunnainen ja kansioskannaus | D1, D9 | Lataus- ja navigointipainikkeet, tiedostonimi, koko paneelin drop | CQDeck.sc:27-91,179-197; CQGuiPanels.sc:238-283 | T-DECK sekä manuaalit 357-383 ja 389-514 | Toteutettu. Varsinainen buffer-/audiolataus on edelleen manuaalisesti testattu |
| Linkitetty stereolataus | D1, D9 | Ei omaa GUI-toimintoa | Cocoquantus.loadStereo, Cocoquantus.sc:141-149 | Manuaali 357-383 | Vain API:ssa oleva toiminto |
| Cocon Hold-, Flip- ja Skip-toiminnot | D3, D9 | Painikkeet ja näppäimet 1/2, q/w, a/s | Cocoquantus.sc:81-108; pysyvät Hold/Flip tilan kautta, Skip-triggeri suoraan syntikalle | T-COCO sekä manuaaliset GUI-/toimintotarkistukset | Toteutettu; Skip on oikein hetkellinen ja välähtää myös muista kuin GUI-kutsuista |
| Quantussyn spesal-toiminnot/tilanäyttö | D4, D9 | Viisi S-painiketta, näppäimet 3-7, numeerinen/15 pisteen näyttö ja castle-LEDit | Cocoquantus.sc:111-126,365-369; cqQuantussyn SendReply | T-QUANT sekä manuaalinen GUI 389-514 | Toteutettu; toiminto ei ole CQSpec-parametri |
| Matriisin patch, unpatch, clear ja load | D5, D9 | 16 × 22 banana bay; veto ja oikea napsautus | CQMatrix.sc:18-144; CQGuiMatrix.sc:4-104; kontrolliväylät CQGraphissa | T-MATRIX ja matriisin NRT-tapaukset | Toteutettu. Ei automatisoitua GUI-solun vuorovaikutustestiä |
| Sisäänrakennetut patch-presetit | D5 | Preset-ponnahdusvalikko | CQMatrix.sc:160-280; Cocoquantus.sc:206-215 | T-MATRIX validoi yksittäisen datan; manuaali 438-456 | Presetit käyttävät koko matriisia mutta vain osittaista parametrioverlayta. Järjestysriippuvuus on varmennettu ja kirjattu tunnuksella CQ-AUD-007 |
| Shake | D5 | SHAKE, p-näppäin ja GUI-paikallinen amount-nuppi | CQMatrix.sc:115-141; Cocoquantus.sc:241-248; CQGui.sc:17-20 | T-MATRIXin determinismi/rajat; manuaali 457-483 | Toteutettu. Oletukset jättävät usein kuuluvat edellytykset kiinni; CQ-AUD-014 |
| Snapshotit: tallennus, listaus ja recall | D9, D11 | Nimi, SNAP, kaksi valikkoa, refresh ja RECALL | CQSnapshots.sc:23-55,116-225; Cocoquantus.sc:150-167 | T-SNAP; manuaali 325-355 | Toteutettu mic- ja panic-turvapolitiikalla |
| Snapshot-morph ja keskeytys | D9 | MORPH ja kesto; keskeytys vain API:ssa | CQSnapshots.sc:55-114 | T-SNAPin morphin valmistuminen; manuaalinen keskeytys/recall 325-355 | Toteutettu. GUI:sta puuttuvat eteneminen, dirty/current-tila ja keskeytys |
| 16n MIDIn tunnistus, rescan, mappaus ja takeover | D8 | Ei konfiguraatiopaneelia; mapattu tila näkyy widgeteissä | CQMidi16n.sc:44-97; CQSpec.sc:219-227; Cocoquantus.midiRescan | T-MIDI; historiallinen laitteisto-M8 | Logiikka testattu; nykyistä oikean laitteen ajoa ei tehty |
| Master-tallennin | D7, D9 | REC-painike, r-näppäin, kulunut aika | CQRecorder.sc:17-76; cqRecord tiedostossa CQSynthDefs.sc:364-371 | Vain tiedostonimen unit-testi; manuaali 485-509 | Audiopolku on historiallisesti varmennettu. Start/stop-async-elinkaarelta puuttuu automaatiokattavuus |
| Stem-tallennus | D7, D11 | REC käyttää konfiguraatiota; ei stem-tilan ilmaisinta | CQRecorder valitsee cqRecordStemsin; CQSynthDefs.sc:373-383 | NRT varmistaa stem-signaalit, ei DiskOut-stem-tallennusta; manuaali 652-653 on auki | Osittain validoitu |
| Laitteiston stem-ulostulot | D7, D11; SPEC.md:311 näyttää esimerkin stemOuts [2,3,4,5], mutta CQConfig.sc:111,149-156 hyväksyy täsmälleen kaksi lähtöä ja config.example.scd:13 käyttää arvoa [2,3] | Vain konfiguraatio | CQGraphin yksityiset stem-väylät; cqMasterin valinnainen stemOuts | NRT:n nelikanavaiset rajattu/ei-mykkä-assertiot | Yksittäisten erillisten NRT-kanavien boundedness varmennettu; hyväksytyn hardware-konfiguraation kokonaisturvaa estää CQ-AUD-001; live-monilähtölaitteiston tarkistus lykätty; dokumentti-/konfiguraatioristiriita CQ-AUD-021 |
| Watchdog-ilmoitus, valinnainen auto-trim ja reset | D6, D11 | Väliaikainen hot-ilmoitus; ei effective trim -näyttöä/reset-ohjainta | CQSynthDefs.sc:310-341; Cocoquantus.sc:261-277,371-386 | Manuaali 542-570; testWatchdog-diagnostiikka | DSP/facade toteutettu. GUI-teksti sanoo aina miinus 6 dB ja piilottaa kertyneen trimmin; CQ-AUD-012 |
| Panic/resume | D6, D9, D11 | PANIC-painike ja välilyönti | monitorMute-tila sekä facade | Snapshot-turvallisuus ja manuaaliset oikotie-/stem-tarkistukset | Toteutettu ja tarkoituksella aina saavutettavissa |
| Pattern-event-toiminnot | D10 | Ei pattern-editoria; tila ja Skip-välähdys näkyvät | Cocoquantus.sc:409-455 rekisteröi set/hold/flip/skip/spesal/patch/deck/shake | CQEventTest.sc:9-48 tarkistaa rekisteröinnin/unbooted no-opin; live-dispatch manuaalissa 575-595 | Toteutettu; bootatun tilan automatisoidussa dispatchissa aukko |
| Näppäinoikotiet | D9 | 1/2, q/w, a/s, 3-7, r, välilyönti, p; selite ikkunassa | CQGui.sc:87-95,198-223 | Historiallinen manuaali 392-418; ei automatisoitua Qt-event-testiä | Vahva suoritusominaisuus, joka pitää säilyttää; CQ-AUD-020 |
| Tiedoston drop | D9 | Koko Deck A/B -laatikko lapsinäkymineen | Rekursiivinen sidonta tiedostossa CQGuiPanels.sc:238-283 | Historiallinen manuaali 410-412; ei automatisoitua Qt-drop-testiä | Toteutettu aiemman regression jälkeen; suojattava automaatiolla CQ-AUD-020 |
| Mittarit, gain reduction, Coco-aktiivisuus ja castle-LEDit | D9 | Master-/Coco-mittarit, GR, castle-LEDit, spesal-tila | SynthDefien SendPeakRMS/SendReply; OSCdefit tiedostossa CQGui.sc:126-153 | Historiallinen manuaali 389-514; NRT varmistaa signaalin mutta ohittaa GUI:n | Toteutettu; näyttösidoksia ei ole automatisoitu |
| Arvolabelit, yksiköt ja kontekstiohje | D9 | Raakaluvut nupeissa, ei numeerista slot-/send-liukusäätimen arvoa, ei tooltipejä | Yleinen formatointi tiedostossa CQGuiWidgets.sc:19-82 | Ei formatter-/help-sopimustestiä | Toimiva mutta semanttisesti vajaa; CQ-AUD-013 |
| Responsiivinen layout ja saavutettavuus | D9 | Kiinteä 1120 × 740 aloitusikkuna, kiinteä matriisi, mouse-only-fokuspolitiikka | CQGui.sc:74,99-111; CQGuiMatrix.sc:7,22-34 | Vain historiallinen oikotie-/fokustarkistus; ei DPI-, resize- tai näppäinnavigointimatriisia | Varmistamatta pienillä/korkean DPI:n näytöillä; CQ-AUD-015 |

## Kattavuuden johtopäätökset

1. Kaikilla 79 nimellä on täydellinen CQSpec → CQState/API → DSP -määrittely. Ainoa
   ehdollinen parametrireitti on micGain, kun micChan on nil.
2. GUI-kattavuus on täsmälleen 71/79. Alussa luetellut kahdeksan puuttuvaa
   GUI-ohjainta eivät ole puuttuvia DSP-ominaisuuksia. qShLag ja c1Off–c5Off
   sopivat ehdokkaiksi Advanced Quantussy -osioon; spaceSize ja spaceDamp
   edellyttävät tiedostoon docs/history/PLAN_v1.0.md:687-700 kirjattua avoinna olevaa
   Space-konseptipäätöstä.
3. Unit-kattavuus on vahvimmillaan arvojen turvallisuudessa, serialisoinnissa,
   matriisidatassa ja serverittömässä MIDI-logiikassa. NRT-kattavuus on vahvimmillaan
   rajauksessa ja valitussa DSP-käyttäytymisessä. Se ohittaa yleensä CQState-reitit.
4. Repossa ei ole commitoitua päästä päähän GUI → CQState → reitti →
   Synth-argumentti-testiä. Tiedostossa docs/history/PLAN_v1.0.md:526-529 mainittua historiallista
   scratch-smokea ei voi toistaa repon sisällöstä. Tämä on CQ-AUD-020.
5. Nimetyt sisäänrakennetut presetit eivät ole deterministisiä koko tilan
   presettejä. Serveritön sekvenssitesti löysi 13 jäännösarvoa reitillä
   selfNoise → classic ja 20 oletuksesta poikkeavaa arvoa reitillä
   quantumDrone → empty. Tämä on CQ-AUD-007.
6. Näkyvät mic-, watchdog-, Space- ja Shake-pinnat eivät aina kuvaa toteutuvaa
   ajonaikaista tilaa tai edellytyksiä; katso CQ-AUD-011–CQ-AUD-014.
