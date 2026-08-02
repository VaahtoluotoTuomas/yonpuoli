# Yönpuolen soitettavuusopas ja 1.0-auditointi

Tämä on sekä soittotutoriaali että 1.0-soitettavuuskierroksen työpöytä. Se
selittää ensin, miksi johto tai shake voi olla täysin oikein kytketty mutta
silti äänetön, ja eristää sitten soittimen osat yksi kerrallaan korvalla
tarkistettaviksi. Aja koodilohkojen rivit SuperCollider IDE:ssä yksi kerrallaan
ja kuuntele kommenttien välissä; älä evaluoi kokonaista lohkoa yhdellä kertaa.

Jos `classes/` tai SynthDefit ovat muuttuneet, tee ensin **Ctrl+Shift+L** ja
käynnistä audio sen jälkeen uudelleen (`CQ.boot` tai Audio-sivun restart).
Pelkkä GUI:n luokkakirjaston vaihto ei vaihda jo serverillä eläviä pitkäikäisiä
`cqCoco`-nodeja uuteen SynthDefiin.

## Mitä automaatio voi jo todistaa

Nykyinen release-sopimus todistaa koneellisesti seuraavat asiat:

- Nightin 44, Masterin 4 ja Workshopin 26 interaktiivista parametria ovat
  erilliset ja niiden yhdiste on täsmälleen kaikki 74 `CQSpec`-parametria.
- Mikillä varustetussa graafissa kaikki 74 parametria päätyvät oikean
  tuotanto-SynthDefin oikeasti olemassa olevaan control-argumenttiin. Kun
  Audio-sivulla `mic` on pois, ainoa tarkoituksella DSP:stä irti oleva säädin
  on `micGain`; käyttöliittymä ei silloin teeskentele olematonta mikkiä.
- Matriisissa on yksi kanoninen 16 lähteen järjestys ja 22 kohdetta. Jokainen
  kohde lukee saman 16-kanavaisen painovektorin, joten lähde–kohde-solujen 352
  yhdistelmää ei ole toteutettu 352 erillisellä, erkaantuvalla koodipolulla.
- Offline-renderi `tests/nrt_playability.scd` käyttää oikeita `\cqQuantussy`-
  ja `\cqCoco`-SynthDefejä, mittaa A/B-eron ja jättää kuuntelutiedostot
  `tests/tmp/`-hakemistoon.

Tämä todistaa **reitityksen**, ei sitä että mikä tahansa johto kuuluu missä
tahansa tilassa. Modulaarinen kohde voi olla oikein toimiva mutta sillä hetkellä
vailla äänilähdettä, kirjoituslupaa tai ulostuloreittiä. Alla olevat ehdot tekevät
eron kuolleen toteutuksen ja suljetun signaalipolun välillä.

## Koko soitin yhdellä kartalla

```text
live / mic ──> root source + fuzz preamp ──┐
deck A/B ──────────────────────────────────┼─> Coco input mix
Quantussy qMix ── cKSend ── SlotQ ─────────┤
other Coco ──────────────── SlotX ─────────┘
                                             │
                              In knob + inVca bananas
                                             │
                         ┌── write/8-bit RAM <── Fb + fbVca bananas
                         │          │
                         │       read head <── Speed, SP.AF, flip, skip
                         │          │
                         │      held memory + audible Organ layer
                         │          │
                         └──────> Level ─> Pan ─> Master safety/output

Quantussy tri/cas + env + Coco + organ = 16 modulation sources
                                  │
                                  └─> 22 matrix destinations
```

Kaksi rinnakkaista asiaa kannattaa pitää erillään:

1. **`qMix` on ääntä.** Castle-kolmiot summataan `cKSend`-tasoilla ja seos
   pääsee Cocoon vain `cocoNSlotQ`:n kautta.
2. **`triK` ja `casK` ovat modulaatiolähteitä.** Ne voivat ohjata mitä tahansa
   matriisikohdetta, vaikka `qMix` ja kaikki `cKSend`it olisivat kiinni.

## Miksi bootin shake saattoi olla täysin äänetön

Pakollinen ensimittaus antoi seuraavan tuloksen:

| A/B-koe | tulos |
|---|---|
| yksi 0,5-johto harvassa soivassa patchissa | selvä ero, −6,22 dB suhteessa signaaliin |
| sama johto tiheyksillä 0,05…1,0 | ero ei romahtanut; tiheä takaisinkytkentä pikemminkin hajotti A/B:t eri ratoihin |
| kaksi täyden kuun shakea valmistellussa soivassa patchissa | lähes korreloimattomat ulostulot (`corr 0,04`) |
| kaksi kaikkien 352 solun shakea | eri aallot mutta lähes sama spektripainopiste: tiheä satunnainen vaihtui toiseen samanluonteiseen tiheään satunnaiseen |
| kaksi täyden kuun shakea täsmällisillä boot-oletuksilla | **bitilleen sama ulostulo** |
| 0,49 / 0,50 / 0,55 / 0,60 / 0,65 Schmittiin | 0 / 0 / 6 / 6 / 6 nousua kolmessa sekunnissa |

Boot-tuloksen syy ei ollut master-limitteri eikä yleinen matriisin
tiheyskyllästyminen. Neljä oletusta osuivat päällekkäin:

- `Hold = 1`: RAM kirjoittaa vanhan näytteen takaisin itseensä. Silloin input-
  ja feedback-VCA-johdot muuttavat laskelmaa, joka ei päädy muistiin.
- `Spaf = 0`: `_spaf`-johtojen summa kerrotaan nollalla.
- `SlotQ = 0`: Quantussyn audioseos ei pääse Cocoon.
- shaken painokatto on 0,3: yksittäinen gate/trig-johto ei yllä +0,5
  Schmitt-kynnykseen.

Täyden kuun noin 70 johtoa olivat siis enimmäkseen kytkettyinä suljettuihin
toimintoihin. Tämä on käyttäjän havainnon varsinainen diagnoosi.

Ensimmäinen yhden muuttujan korjaus on nyt **uuden käsin tai Inspectorista
istutetun johdon paino 0,60** (ennen 0,5; ensimmäinen kuuntelukandidaatti oli
0,65). Nightin koko 40 px magneettinen pudotusalue on binäärinen: joko johto
istutetaan valitulla painolla tai matriisiin ei kirjoiteta mitään. Etäisyys ei
enää vaimenna johtoa eikä socketin lähellä ole kuultavaa auditionia. Johdon
ollessa kädessä rulla valitsee alkupainon (0,05; Ctrl 0,01) ja painokuu näyttää
sen. Kannettavalla `Z` pienentää ja `X` kasvattaa samaa arvoa; Shift tekee
0,01-askeleen. Näin ensimmäiseksi arvoksi voi valita vaikka 0,10 ilman
0,60-transienttia tai tarvetta rullata touchpadilla painalluksen aikana.
Tämä ei muuta vanhoja patcheja, snapshotteja, shakea eikä Schmitt-kynnystä.
0,55 oli mittauksen pienin portin varmasti
laukaissut koetin; 0,60 jättää siihen marginaalin mutta keventää jatkuvaa
ensikosketusta käyttäjän 0,65-kuuntelun perusteella. Kaksi seuraavaa erikseen
mitattua ehdokasta ovat `Spaf`-boot-oletus 0,12 ja shaken paino 0,65; kumpikin
teki bootin kahdesta täyden kuun shakesta voimakkaasti erilaiset. Niitä ei pidä
yhdistää samaan kuuntelukierrokseen.

## Lähteet: mitä kahdeksan lähtöperhettä oikeasti antavat

| Night-lähtö | signaali | milloin se elää |
|---|---|---|
| `tri1…tri5` | kunkin castlen raaka ±1 kolmio | aina Quantussyn käydessä |
| `cas1…cas5` | toisesta castlesta kellotettu sample/track-and-hold | aina, mutta sisältö riippuu spesal-tilasta |
| `envA`, `envB` | preamp A/B:n 0…1 envelope | vain kun valitussa juuressa on signaalia |
| `coco1`, `coco2` | RAM-lukupään raaka muistisisältö ennen Leveliä | boot-kohinaloopista tai tallennetusta materiaalista |
| `org1`, `org2` | Cocon kellosta johdettu kanttiaaltonippu, patch-jakissa ×1,6 ja rajattu ±1:een | **aina**, vaikka Nightin kuultava `Organ = 0` |

Viimeinen kohta on tärkeä: `Organ`-nuppi määrää vain sen, paljonko urkua
lisätään kuultavaan Coco-ääneen. Keltainen `orgN`-jakki ei sammu nupin mukana.

## Kohteet ja niiden kuuluvuusehdot

| kohde | mitä se tekee | mitä pitää olla auki, jotta sen kuulee |
|---|---|---|
| `cocoN_spaf` | eksponentiaalinen kellon/pitchin FM | `cocoNSpaf != 0` ja kuultava muistisisältö |
| `cocoN_inVca` | avaa/sulkee Cocoon kirjoitettavaa inputtia | `Hold = 0`, jokin Slot + sen lähde auki |
| `cocoN_fbVca` | avaa/sulkee takaisin RAMiin kirjoitettavaa vanhaa muistia | `Hold = 0`, muistissa sisältöä ja Fb riittävä |
| `cocoN_flip` | vaihtaa lukusuunnan Schmitt-gatella | summa käy yli +0,5; materiaalissa suunnan pitää erottua |
| `cocoN_skip` | heittää lukupään alkuun nousureunalla | summa käy yli +0,5; materiaalissa ajallinen fraasi |
| `castleK_fm` | lisää castlen taajuuseksponenttiin oktaaveina | castle pitää viedä qMixin tai toisen aktiivisen johdon kautta kuultavaksi |
| `castleK_spesal` | askeltaa castlen 8-tilaista sisäkytkentää | summa käy yli +0,5; verkon pitää saavuttaa ääni/modulaatio |
| `deckA/B_rate` | lisää −2…+2 soittimen nopeuteen | tiedosto ladattu ja deckin Level/reitti kuultavana |

Magneettiseen pudotusalueeseen kiinnittynyt uusi 0,60-johto takaa kynnyksen
ylityksen täydelle ±1-lähteelle. Envelope, hiljainen Coco tai tarkoituksella
rullalla vaimennettu johto voi silti jäädä sen alle; Nightin charge-lamppu
näyttää juuri tämän todellisen summan amberina täyttönä. Vaalea vinoneliö sen
ympärillä on eri tieto: DSP:n Schmitt todella laukesi. Gatella vinoneliö pysyy
niin kauan kuin gate on päällä; trig jättää 0,15 sekunnin hiipuvan leimahduksen.

## Castle / Quantussy ilman mystiikkaa

Yhdellä castlella on kaksi ulostuloa ja kaksi matriisituloa:

- oranssi **`triK`** = linnan oma jatkuva kolmio;
- harmaa **`casK`** = jonkin toisen linnan kolmio, jonka nykyinen linna on
  sample/track-and-holdannut;
- sininen **`castleK_fm`** = ulkoisten banaanien taajuussumma;
- vihreä **`castleK_spesal`** = ulkoisten banaanien askellustriggeri.

Linnasaarekkeen kelloklikkaus ja linnan välinen johto eivät siis tee samaa:

- **klikkaus missä tahansa näkyvällä linnapinnalla** askeltaa sisäkellon tilaa 0…7. Tila valitsee, mitä neljästä
  muusta linnasta nykyinen linna kuuntelee ja käyttääkö se sample-and-holdia
  (0…3) vai track-and-holdia (4…7). Klikkaus ei luo matriisijohtoa;
- johto **`… -> castleK_spesal`** askeltaa täsmälleen samaa tilakonetta, mutta
  lähteen rytmissä;
- johto **`… -> castleK_fm`** ei vaihda kumppania tai hold-moodia. Se taivuttaa
  nykyisen linnan omaa kelloa jatkuvasti.

Nightin linnasta toiseen piirtyvä spesal-jänne näyttää sisäisen kytkennän
suunnan **kumppanista nykyiseen linnaan**. Yhtenäinen jänne tarkoittaa
sample-and-holdia, katkonainen track-and-holdia. Se on visualisointi, ei
istutettu johto.

Konkreettinen ero on yhden linnan oman kanttikellon aikana tämä:

- **sample-and-hold (S&H):** kellon nousureunalla `casK` ottaa kumppanin
  kolmiosta yhden hetkellisen näytteen ja pitää juuri sen arvon muuttumattomana
  koko seuraavaan nousureunaan asti — tulos on porras;
- **track-and-hold (T&H):** kellon korkean puolijakson ajan `casK` seuraa
  kumppanin kolmiota jatkuvasti, mutta matalan puolijakson ajan jäädyttää
  viimeisen arvon — tulos vuorottelee liikkuvan ja pidetyn jakson välillä.

Tämä on näkyvä `casK`/S&H1-kytkentä. Sen lisäksi Quantussy käyttää erillistä
sisäistä S&H2-held-signaalia chaos-FM:ään; siksi yhden spesal-jänteen vaihtaminen
ei irrota linnaa muusta viiden oskillaattorin takaisinkytkentäverkosta.

Sisäkellon kuvan kolme samanaikaista asiaa kannattaa lukea erikseen:

- kahdeksan kehätikkiä ja niiden **kiertävä vaalea helmi** = Spesal-tila 0…7;
- keskellä oleva oranssi **kieli** = tämän linnan oma raaka `triK`-kello. Alle
  3 Hz:n nopeudella helmi kulkee kielellä; näyttöä nopeampana koko kieli hohtaa
  vakaana eikä sahaa aliasoitujen pisteiden välillä;
- linnapinnan hoverissa ilmestyvä vihreä **jänne** = mistä toisesta linnasta
  `casK` luetaan ja onko lukutapa sample- vai track-and-hold.

Vanha koko linnan himmeä hengitys seurasi `casK`:n magnitudia ja toisti siten
harmaan lähtöjakinkin tiedon. Se on poistettu. PLAY-5:stä alkaen koko näkyvä,
epäsäännöllinen linnasaareke — keskipisteestä aivan piirrettyyn lihareunaan —
on yksi spesal-askelen hover- ja klikkauspinta. Keskikello kirkastuu kertomaan,
mihin mekanismiin ele osuu, ja sama jänne paljastuu kaikkialta saarekkeelta.
Erillistä ulkoliha-kehää tai ryhmäsoloa ei silti ole; neljän jakin/socketin omat
solot säilyvät. Vain piirretyn blobin ulkopuolinen ohut kerros on tavallista
johto-/haravointimaata.

`qChaos` määrää, kuinka voimakkaasti toinen sisäinen held-signaali (`S&H2`)
moduloi linnan omaa taajuutta. `qFreq` skaalaa kaikki viisi kelloa yhdessä,
`cKRange` valitsee karkeasti infra/balcium/audio ja Workshopin `cKOff` tekee
±1 oktaavin yksilövirityksen.

Vertailukohta alkuperäiseen soittimeen on tarkoituksellinen mutta ei
emulaatiolupaus: Ciat-Lonbarden oma kuvaus nimeää Quantussyn viiden oskillaattorin
verkoksi, jossa kunkin rytmi kvantisoi muiden neljän liikkeitä, ja korostaa juuri
sen käyttöä SP.AF-, flip- ja skip-modulaatioon. Kuvauksen **15 keskimmäistä
LEDiä näyttävät “Spesal Statea”**; niitä ei kuvata viiden raw-oskillaattorin
erillisiksi tasovaloiksi. Yönpuolen kahdeksan kehätikkiä ja kiertävä helmi ovat
tämän tilatiedon suora, eivät ulkoasultaan kirjaimellinen tulkinta; keskikieli
lisää erikseen raw-tri-kellon, jota alkuperäisen sivun LED-kuvaus ei lupaa.
Lähde:
[Ciat-Lonbarde: Cocoquantus](https://www.ciat-lonbarde.net/ciat-lonbarde/cocoquantus/index.html).

## Workshop: mitkä piilotetut säädöt muuttavat Nightin merkitystä

Workshop ei sisällä kuolleita lisukkeita. Erityisesti nämä kannattaa hahmottaa:

| Workshop-säädin | vaikutus Nightiin |
|---|---|
| `cocoNLevel`, `Pan` | kuultavan Cocon lopputaso ja stereopaikka; eivät muuta muistia tai `cocoN`-modulaatiojakkia |
| `SlotPre` | paljonko valitun juuren fuzz-preampia menee Cocon inputtiin |
| `SlotDeck` | paljonko saman puolen deckiä menee suoraan inputtiin |
| `SlotQ` | Quantussy-audion portti; ilman tätä qFreq/qChaos voivat olla vain modulaatiota |
| `SlotX` | toisen Cocon kuultava output takaisin tämän Cocon inputtiin |
| `cKSend` | mitkä raw-tri-linnat muodostavat qMixin; bootissa vain castle 5 = 0,5 |
| `Organ` | lisää kellosta johdetun urun Cocon outputiin, ei omaan RAM-kirjoitukseen |
| `Bits/Wrap/Interp/Dust` | muistin muunninluonne; setup/äänensävy, ei kaapelireitti |
| `qShLag`, `cKOff` | held-askelten pehmeys ja linnan yksilöviritys |

Night + Master + Drawer tavoittavat siis kaiken, mutta **Night yksin ei ole
itsenäinen 74 säätimen syntikka**. Tämän kierroksen aidot
promootiokandidaatit ovat `cocoNLevel`, yhteinen `SlotQ` ja `Organ`: jos jokin
niistä osoittautuu alla toistuvasti fraasin sisäiseksi eleeksi eikä
setup-päätökseksi, sille pitää suunnitella Night-ele. Sitä ei pidä ratkaista
lisäämällä kolme satunnaista nuppia ennen soittotestiä.

## Range-auditoinnin tiivistelmä

| perhe | alue ja oletus | arvio |
|---|---|---|
| live / mic | 0…4 / 0…8, oletus 1 / **0** | riittävä gain; mikki turvallisesti mykkä |
| deck | level 0…2, rate −2…2, loop on | unity keskellä, reverse/stop/2× samassa eleessä |
| preamp | Drive 1…40 exp (2), Bias 0…0,6 (0,15) | laaja mutta hyväksytyllä osittaisella loudness-kompensaatiolla |
| Coco In / Fb | 0…2 (0,8) / 0…1,25 (0,35) | inputissa headroom, feedbackissa leveä unityn ympäristö |
| Speed / SP.AF | 1 500…48 000 Hz exp (24k) / −4…+4 oct (**0**) | alueet käyttökelpoiset ja jälkiklampatut; SP.AF:n signed-square-käsikartta antaa ±0,25 oct hienoalueen nollan ympärille, mutta boot-nolla on yhä erillinen soitettavuuspäätös |
| muunnin | Bits 3…12 (8), Dust 0…0,5 (0,02), ZOH, clip | hiljaisesta raaka-aineesta tuhoisaan mutta bounded-tilaan |
| output / slots | Level 0…1,5 (0,8); slotit 0…1,5 | riittävä miksivara; Q/X tarkoituksella kiinni bootissa |
| Quantussy | qFreq 0,05…500 exp (4), chaos 0…1 (0,15), lag 0…0,2 | infra–audio yhdellä perustalla; per-castle DSP-katto 4 kHz estää vaarallisen yläpään |
| castle | range 3 tilaa, off ±1 oct, send 0…1 | karkeus, yksilöviritys ja audioseos erotettu toisistaan |
| master / matrix | −60…0 dB (−12), patchDepth 0…1 (1), weight ±1 | turvallinen ulostulo ja täysi makro-/polaarisuusalue |

Automaattinen auditointi ei löytänyt puuttuvaa tai väärään SynthDef-argumenttiin
reititettyä nuppia. Avoimet asiat ovat musiikillisia oletuksia, eivät
rekisteri- tai reititysvikoja: SP.AF:n boot-nolla, shaken 0,3-painot ja se,
ansaitseeko jokin kolmesta Workshop-kandidaatista Night-eleen.

---

## Soittotesti 1 — Cocon muistipolku

```supercollider
// Ctrl+Shift+L ensin, koska ensimmäisen johdon oletus muuttui.
CQ.boot;                                      // odota 2 s fade-in
CQ.clearPatch;
CQ.deckA.load(Platform.resourceDir +/+ "sounds/a11wlk01.wav");
CQ.set(\coco2Level, 0);                       // kuuntele vain vasenta Cocoa
CQ.set(\coco1SlotPre, 0);                     // ei live/mic-juurta tähän kokeeseen
CQ.set(\coco1SlotDeck, 1);                    // deck A suoraan Cocoon
CQ.set(\coco1In, 0.8); CQ.set(\coco1Fb, 0.7);
CQ.hold(1, 0);                                // NYT input/fb-VCA-reitit kirjoittavat RAMiin
// Kuuntele muutama sekunti: deck tallentuu ja kiertää.
CQ.hold(1, 1);                                // lukitse täsmälleen nykyinen muisti
CQ.set(\coco1In, 0);                          // ei muuta jo lukittua loopia
CQ.set(\coco1Fb, 0);                          // ei muuta sitä sekään Hold=1-tilassa
CQ.set(\coco1Speed, 12000);                   // mutta lukupää kuuluu heti: pitch alas
CQ.flip(1);                                   // ja suunta kuuluu heti
CQ.skip(1);                                   // fraasi palaa bufferin alkuun
// Tavoite: erotat kirjoituspolun (In/Fb/Hold) ja lukupään (Speed/Flip/Skip).
```

## Soittotesti 2 — uuden 0,60-johdon ensimmäinen kosketus

```supercollider
// Jatka testin 1 kuultavalla held-loopilla. Nightissä:
// 1. Vedä tri5-jakista coco1:n inVca-socketin magneettialueelle, mutta pidä kädessä.
//    Reitin/painon pitää näkyä, mutta matriisin ja äänen pysyä ennallaan releaseen asti.
//    Vie hiiri melkein 40 px päähän socketin keskustasta ja irrota.
//    Hold=1: mitään ei kuulu — oikein, koska inVca on kirjoituspolussa.
CQ.hold(1, 0);                                // nyt saman johdon pitää tremoloida tallennusta
CQ.matrix.getPatch(\tri5, \coco1_inVca);     // -> 0.60 koko 40 px pudotusalueella
// 2. Revi johto ja vedä tri1 -> coco1_flip näkyvään socketiin.
CQ.hold(1, 1);                                // selkeä held-fraasi suunnan kuulemiseen
CQ.matrix.getPatch(\tri1, \coco1_flip);      // -> 0.60
//    Odota rauhassa: tri1 on infra ja yksi kierros kestää bootissa noin 12,5 s.
//    Amberin varauksen pitää nousta ja vaalean vinoneliön välähtää, kun suunta
//    todella vaihtuu. Vanha 0.50-oletus EI laukaissut tätä koskaan.
// 3. Ota uusi johto käteen ja rullaa ENNEN pudotusta: painokuu muuttuu mutta
//    CQ.matrix pysyy nollassa. Pudota vasta sitten: ensimmäinen DSP-arvo on valittu arvo.
//    Touchpadilla käytä Z = alas, X = ylös, Shift+Z/X = hienoaskel.
// 4. Rullaa istutetun gate-johdon päällä alaspäin. Kun paino jää kynnyksen alle,
//    lampun pitää näyttää lataus mutta suunnanvaihdon lakata — ei mystistä hiljaisuutta.
```

Nopea porttidiagnostiikka on `tri3 -> coco1_skip`: castle 3 on bootissa
balcium-alueella, noin 3 Hz, joten lampun ja kelan nollauksen pitää vastata lähes
heti. Castlet 1, 2 ja 4 ovat infra-alueella (suuruusluokaltaan yksi kierros
9–13 sekunnissa ennen chaos-heilahtelua), joten oikein toimivakin johto voi
odottaa seuraavaa nousureunaa pitkään. Castle 5 on puolestaan audioalueella:
sen sadat triggerit sekunnissa kuuluvat värinä/stasis-äänenä, eivät yksittäisinä
suunnanvaihtoina.

**Hyväksytty 2026-08-02:** testien 1–2 muistipolku, koko magneettisen alueen
etäisyydestä riippumaton kiinnitys ja tuoreen johdon välitön flip/skip-triggeröinti toimivat.
Ensimmäinen 0,65-kandidaatti arvioitiin jatkuvaan modulaatioon hieman vahvaksi;
tuotantooletus tarkennettiin 0,60:een muuttamatta Schmitt-kynnystä.

### Soittotesti 2B — SP.AF:n hienovesi ja johdon painokuu

```supercollider
// Ctrl+Shift+L uuden luokkakirjaston lataamiseksi. Jatka testin 1 held-loopilla.
CQ.set(\coco1Spaf, 0);
// 1. Vedä SP.AF-sirppiä nollasta noin 15 px ylös, sitten lue:
CQ.get(\coco1Spaf);                           // odotus noin +0.25 oct
CQ.set(\coco1Spaf, 0);
// Vedä nyt nollasta noin 30 px ylös, sitten lue:
CQ.get(\coco1Spaf);                           // odotus noin +1.0 oct
// Sama alas antaa täsmälleen symmetriset negatiiviset arvot; täysi liike on yhä ±4.
// Sirpin pieni sisempi satamakaari = ±0.25 oct hienoalue, kaksi pistettä = ±1 oct.
// Right-click kumpaan tahansa SP.AF-sirppiin palauttaa täsmälleen nollaan.
// Raahauksessa ei ole nollalukkoa: liikkeen saa viedä katkeamatta nollan yli.

// 2. Ota tavallinen verso-johto käteen: painokuu näkyy jo ennen pudotusta.
//    Rullaa ja varmista, ettei ääni muutu vielä. Pudota ja vie sitten osoitin kaarelle.
// Odotus: keskikohdan painokuun kehä on noin 3/5 täynnä myötäpäivään.
// Rullaa yksi askel alas: kehä lyhenee havaittavasti paksuuden rinnalla.
// Alt-klikkaa polariteetti: väri ja kiertosuunta vaihtuvat yhdessä.
```

Arvioi korvalla erityisesti ±0,25 oktaavin maa: 0,60-johto tekee siitä enintään
±0,15 oktaavin liikkeen. Sen pitäisi tuottaa hienovarainen huojunta ilman, että
laaja ±4 oktaavin äärialue tai suorien `CQ.set`-arvojen merkitys on kadonnut.

**Hyväksytty 2026-08-01:** 0,60-ensipaino, SP.AF:n signed-square-käsikartta,
satamakaari/pinprick-asteikko ja pelkkä painokuu ilman prosenttikapselia jäävät
tuotantoon.

### Soittotesti 2C — Cocon elävä asetus ja todellinen modulaatio

Pidä SP.AF:n värillistä helmeä perusasetuksena ja kytke esimerkiksi
`tri1 -> coco1_spaf`. Ulomman ontton satelliitin pitää liikkua hitaasti
todellisen pitch-panoksen mukana, mutta värillinen helmi ei saa liikkua eikä
`CQ.get(\coco1Spaf)` muuttua. Kokeile sitten audioalueista `tri5`:tä. Nyt
satelliitti ei saa nykiä vääränä hitaana aaltona, vaan nopea heilahdus näkyy
vakaana ulompana wake-kaarena ja ontto helmi asettuu sen keskelle. Sama laki
toimii Input- ja Feedback-sirpeissä;
niissä satelliitti näyttää post-law/post-clip-VCA:n. Kun viimeinen kyseisen
kohteen johto revitään tai `patchDepth` viedään nollaan, satelliitti katoaa.

Arvioi erityisesti, erottuvatko kaksi helmeä heti toisistaan: värillinen =
**mihin käsi jätti asetuksen**, ontto = **mitä DSP tekee nyt**. Jos ero ei ole
ensi silmäyksellä selvä, älä päättele sitä liikkeestä vaan raportoi näkyvyys;
väärin opittava indikaattori on huonompi kuin ei indikaattoria.

**Hyväksytty 2026-08-02:** asetushelmi, live-satelliitti ja wake jäivät
tuotantoon. Audio-rate-katselmuksessa löytynyt kahden näennäisen aseman sahaus
poistettiin: kun wake kattaa vähintään 1/16 sirpin alueesta, satelliitti näyttää
vakaasti mitatun alueen keskuksen eikä yksittäistä 15 Hz näytettä.

Spiraalin poikkiviiva pysyy tarkoituksella **Speed-perusasetuksen** kohdalla.
Se on kädellä tartuttava asteikkomerkki, ei toteutuneen moduloidun kellon
mittari. SP.AF-satelliitti/wake näyttää erikseen kelloon lisätyn toteutuneen
oktaavipanoksen ja lukupään komeetan todellinen liike näyttää seurauksen RAMissa.
Poikkiviivan liikuttaminen tekisi säätökohteesta liikkuvan maalin ja kertoisi
saman modulaation kahdesti; tämän työnjaon vuoksi staattisuus ei ole epärehellistä.

## Soittotesti 3 — linnakello, FM-johto ja spesal-johto

```supercollider
// Eristä castle 1 oikeaksi ääneksi. Aja rivit yksi kerrallaan.
CQ.patchPreset(\quantumDrone);
CQ.set(\masterVol, -24); CQ.set(\coco2Level, 0);
[\c2Send, \c3Send, \c4Send, \c5Send].do { |n| CQ.set(n, 0) };
CQ.set(\c1Send, 0.65); CQ.set(\c1Range, \audio);
CQ.set(\coco1SlotQ, 1); CQ.set(\coco1SlotPre, 0); CQ.set(\coco1SlotDeck, 0);
CQ.hold(1, 0); CQ.set(\qChaos, 0.35);
// 1. Klikkaa missä tahansa castle 1:n näkyvän saarekkeen sisällä kerran kerrallaan.
//    Keskikello kirkastuu ja sama sisäkytkennän jänne näkyy koko pinta-alalla.
//    Kumppanijänne vaihtuu; neljän askeleen jälkeen solid/broken-moodi vaihtuu.
//    Tämä on sisäinen partneri+hold-tila, ei uusi johto.
CQ.spesalSet(1, 0);                           // tunnettu paluu
// 2. Istuta tri3 -> castle1_fm (0.60).
//    Castle 1:n pitch liikkuu, mutta kumppanijänne/spesal-tila EI vaihdu.
CQ.patch(\tri3, \castle1_fm, 0.60);
CQ.unpatch(\tri3, \castle1_fm);
// 3. Istuta tri3 -> castle1_spesal (0.60).
//    Nyt sama tila askeltuu automaattisesti tri3:n rytmissä ja vihreä lamppu välähtää.
CQ.patch(\tri3, \castle1_spesal, 0.60);
CQ.unpatch(\tri3, \castle1_spesal);
```

`castle1_fm` on siis kyllä castle 1:n oman kolmio-oskillaattorin jatkuva
taajuusmodulaatiotulo. Se muistuttaa Cocon SP.AF-tuloa sikäli, että kumpikin
lisätään eksponenttiin oktaaveina. Ero on, että castle-FM:llä ei ole erillistä
SP.AF-syvyysnuppia: johdon paino ja yhteinen `patchDepth` ovat sen attenuaatio,
ja lopputaajuuteen vaikuttavat lisäksi `qFreq`, range, `c1Off`, chaos ja sisäinen
held-signaali.

**Hyväksytty 2026-08-02:** kello/FM/spesal-erottelu toimii. Keskipisteen jälkeen
myös loppu näkyvä linnaliha liitettiin samaan askelpintaan; erillinen
ulkoliha-hover/solo pysyy poistettuna. `cas`-aura ja epäselvä
charge-vs-fire-lamppu korjattiin yllä kuvatulla tavalla.

## Soittotesti 4 — tri ja cas ovat eri ulostuloja

```supercollider
// Palaa held-looppiin ja avaa SP.AF käsin, koska boot-oletus on vielä 0.
CQ.patchPreset(\empty);                       // HUOM: tyhjentää johdot, EI resetoi nuppeja
CQ.set(\coco1Spaf, 0.35); CQ.set(\coco1Speed, 24000);
CQ.patch(\tri1, \coco1_spaf, 0.60);          // jatkuva hidas kolmio: pehmeä pitch-kaari
CQ.unpatch(\tri1, \coco1_spaf);
CQ.patch(\cas1, \coco1_spaf, 0.60);          // toisen castlen held-liike: askeltavampi sää
CQ.unpatch(\cas1, \coco1_spaf);
// Klikkaa castle 1:n näkyvää pintaa ja toista vain cas1-koe:
CQ.spesal(1); CQ.patch(\cas1, \coco1_spaf, 0.60);
// cas1 muuttuu, koska sen sampled partner/moodi vaihtui; tri1 on yhä linnan oma aalto.
```

**Hyväksytty 2026-08-02:** tri ja cas käyttäytyvät eri lähteinä ja spesal-tila
muuttaa vain cas-lukemaa/sisäverkkoa odotetusti.

## Soittotesti 5 — urku on sekä lähde että kuultava kerros

```supercollider
CQ.clearPatch;
CQ.set(\coco1Organ, 0);                       // kuultava urkukerros pois
CQ.set(\coco1Speed, 24000); CQ.set(\coco1Spaf, 0.12);
CQ.patch(\org1, \coco1_spaf, 0.2);           // tarkoituksella hienovarainen self-FM
CQ.get(\coco1Spaf); CQ.get(\patchDepth);      // odotus 0.12 ja 1.0
// Onton satelliitin pitää olla lähellä nollaa, mutta harmaan wake-kaaren näkyä.
// Reitti on yhä hienovarainen mutta uuden lähdekunnostuksen jälkeen noin ±0.025 oct.
CQ.set(\coco1Spaf, 0.35);
CQ.patch(\org1, \coco1_spaf, 0.60);          // hetkellinen selvä koetin
// Nyt wake ja FM-vaikutus eivät saa jäädä tulkinnanvaraisiksi.
CQ.unpatch(\org1, \coco1_spaf);
CQ.patch(\org1, \coco1_skip, 0.60);          // diskreetti: lukupää resetoituu nopeasti
CQ.unpatch(\org1, \coco1_skip);
CQ.set(\coco1Organ, 0.65);                    // sama perhe lisätään NYT outputiin
CQ.set(\coco1Speed, 12000);                   // urun sävel putoaa kellon mukana
CQ.set(\coco1Speed, 24000);
CQ.hold(1, 0); CQ.hold(1, 1);
// Oman Cocon Organ on lukupään JÄLKEINEN kerros: hold ei tallenna sitä omaan RAMiin.
CQ.set(\coco1Organ, 0);                       // kerros katoaa heti, muistilooppi jää
```

Livehavainto paljasti lähdetason virheen, jota ensimmäinen SP.AF:n vaihe-A/B ei
havainnut: 24 kHz:n kellolla bandlimited kanttiaaltonipun positiivinen huippu oli
vain `+0,5602`, joten 0,60-johto antoi Cocon Schmittille enintään `0,336` eikä
voinut laukaista flip/skipiä. Patch-jakki käyttää nyt kiinteää ×1,6-vahvistusta
ja ±1-rajausta; kuultava Organ-kerros käyttää edelleen alkuperäistä signaalia ja
tasoa. Tuotantopolun offline-portissa `org1 → coco1_skip 0.60` muutti held-loopin
RMS:n `0,1528 → 0,0414` (diff-RMS `0,1583`), eli triggeri päätyy varmasti oikeaan
Phasoriin. `org1 → oma SP.AF` toimii saman eksplisiittisen yhden blokin
self-feedbackin kautta ja näkyvä wake on aiempaa noin 1,6 kertaa leveämpi.
Muutos vaatii luokkakirjaston käännöksen ja Audio-sivun restartin.

**Hyväksytty 2026-08-02:** urun lähde/output-erottelu ja nopeuden mukana
muuttuva kuultava urkukerros toimivat. Organ-lähteen jatkuva ja diskreetti
Coco-paluu on nyt vartioitu erillisillä tuotanto-NRT-regressioilla; uusi live-
retesti varmistaa vielä käyttöympäristön.

## Soittotesti 6 — Nightin jatkuva drone-anatomia

```supercollider
// Tee kahdesta Cocosta soiva pari ja palaa Drawerista Nightiin.
CQ.patchPreset(\selfNoise);
// Alla olevat CQ.set-rivit ovat tarvittaessa täsmällinen A/B-lukema;
// tee sama ele ensin Nightin piirretyistä tartuntapinnoista.
// A. LEVEL: vedä kuorten ulompia rantakaaria ja tee ristihäivytys.
CQ.set(\coco1Level, 0.2); CQ.set(\coco2Level, 1.0);
CQ.set(\coco1Level, 1.0); CQ.set(\coco2Level, 0.2);

// B. ORGAN: vedä keltaista maltoa jakin ympärillä; jakki pysyy lähteenä.
CQ.set(\coco1Organ, 0.6); CQ.set(\coco2Organ, 0.6);
CQ.set(\coco1Organ, 0); CQ.set(\coco2Organ, 0);

// C. SLOT Q: vedä kuorten Q-kiduksia ja ratsasta qFreq/qChaos-renkaasta.
CQ.set(\coco1SlotQ, 0.8); CQ.set(\coco2SlotQ, 0.8);
CQ.set(\coco1SlotQ, 0); CQ.set(\coco2SlotQ, 0);

// D. SLOT X: silta on kaksikaistainen, ei yksi linkitetty feedback-säädin.
CQ.set(\coco1SlotX, 0.8); // Coco 2 -> Coco 1
CQ.set(\coco2SlotX, 0.0);
CQ.set(\coco1SlotX, 0.0);
CQ.set(\coco2SlotX, 0.8); // Coco 1 -> Coco 2
CQ.set(\coco2SlotX, 0.0);

// E. CASTLE SEND: nollaa, tap linnaa, tarkista ettei send muuttunut;
// vedä sitten samaa linnaa selvästi sisäänpäin ja tarkista arvon nousu.
CQ.set(\c1Send, 0); CQ.get(\c1Send);
```

### Testin 6 toteutettu suunnittelupäätös

`coco1Level` ja `coco2Level` ovat hyväksyttyjä Night-eleitä: niitä tarvitaan
fraasin sisäiseen miksaukseen ja ristihäivytykseen. Ne eivät kuulu master-
stripille, koska stripin tehtävä on yhteinen turvallisuus ja kokonaislähtö;
kanavakohtaisen äänen irrottaminen omasta Cocostaan tekisi maisemasta pienen
geneerisen mikserin. Toteutus on kummankin kuoren **ulompi rantakaari**: avoin,
unity-lovella varustettu Level-kaari, jonka täyttö ja helmi näyttävät arvon.
Kuoren spiraali saa seurata Leveliä vain paineellaan/alfallaan niin, että nollakin
jättää soittimen anatomian näkyviin. Itse rantakaari on täsmällinen tartuntapinta.

Erillinen drone-moodi olisi tarpeeton toinen käyttötila. Yksi jatkuva Night,
jossa drone syntyy avaamalla sisäisiä virtauksia, sopii paremmin instrumentin
logiikkaan. Tämä vaatii kuitenkin enemmän kuin `SlotQ`: bootin `qMix` sisältää
vain `c5Send = 0,5`; `c1Send…c4Send` ovat nollassa. Siksi kokonainen kieli on:

- kummastakin Quantussy-renkaan laidasta Cocoon kulkeva himmeä **sivujoki**;
  kuoren lähellä oleva kidus säätää sen puolen `cocoNSlotQ`:ta, ja joen paine
  kertoo arvon;
- kunkin castlen sisäänpäin osoittava, levossa lähes näkymätön **sivu-uoma**;
  yli 8 px radiaalinen veto sisään avaa ja ulos sulkee `cNSend`:iä. Alle 8 px
  release jää spesal-askeleeksi, eikä askelta tehdä pressissä ennen kuin eleen
  tarkoitus tiedetään;
- organ-jakin varren ympärillä oma **hedelmämalto**: sen täyttö ja pystysuuntainen
  veto säätävät `cocoNOrgan`:ia, mutta keltainen siemen/jakki pysyy saman kokoisena
  ja aina patchattavana. Näin lähde ei valehtele sammuneeksi Organ=0-asennossa;
- kummankin Cocon ulompi Level-rantakaari yllä kuvatulla tavalla.

`SlotX` ei ole SlotQ:n toinen nimi vaan suunnattu Coco→toinen Coco -ristisyöttö.
Nightissä se on kahden kuoren välinen kaksisuuntainen, kahdella erillisellä
sululla ja nuolella varustettu silta — ei linkitetty Q-kidus eikä yksi yhteinen
määrä. Cocon 1 venttiili on `coco1SlotX`, siis **Coco 2 → Coco 1**; Cocon 2
venttiili on `coco2SlotX`, siis **Coco 1 → Coco 2**. Feedback, Input, Hold,
Speed ja castle-ranget
ovat jo Nightissä, joten muita drone-säätimiä ei tarvita ennen näitä. Erityisesti
Bits/Wrap/Dust ovat edelleen sointivärin valmistelua, eivät drone-portin ehtoja.

Kaikki kuorten neljä uutta pintaa käyttävät samaa pystysuuntaista vetoa ja
kirjoittavat `CQState`-suppilon läpi. Kaapeli ja Alt-ele voittavat osumajärjestyksessä.
Workshopista poistettiin näiden kaksoiskappaleet: nykyinen omistajuus on
Night 44 · Master 4 · Workshop 26. Automaattinen geometria-, omistajuus- ja
GUI-eleportti on vihreä; tämän testin jäljellä oleva osa on käsin tehtävä
soitettavuus- ja luettavuusarvio.

## Soittotesti 7 — shaken kaksi seuraavaa yhden muuttujan kandidaattia

```supercollider
// Tee ensin nykyinen ongelma tarkoituksella. Uusi boot on selkein lähtötila.
CQ.free; CQ.boot; CQ.clearPatch;
CQ.set(\coco1Spaf, 0); CQ.set(\coco2Spaf, 0);
CQ.hold(1, 1); CQ.hold(2, 1);
CQ.shake(0.3, seed: 8101, density: 0.2);       // kuuntele 10–15 s
CQ.shake(0.3, seed: 9202, density: 0.2);       // voi yhä olla lähes/sama ääni

// KANDIDAATTI A — muuta vain SP.AF:n lähtösyvyys 0 -> 0.12.
CQ.set(\coco1Spaf, 0.12); CQ.set(\coco2Spaf, 0.12);
CQ.shake(0.3, seed: 8101, density: 0.2);       // sama kaksi siementä
CQ.shake(0.3, seed: 9202, density: 0.2);
// Arvioi: tekeekö shake nyt sopivan eron? Ovatko yksittäiset SP.AF-johdot
// bootista lähtien elossa vai onko held-looppi jatkuvasti liian levoton?

// KANDIDAATTI B — palauta SP.AF nollaan ja muuta vain shaken paino 0.3 -> 0.65.
CQ.set(\coco1Spaf, 0); CQ.set(\coco2Spaf, 0);
CQ.shake(0.65, seed: 8101, density: 0.2);
CQ.shake(0.65, seed: 9202, density: 0.2);
// Arvioi: ovatko flip/skip/spesal-spasmit musikaalisia vai liian kaoottisia?
// Älä arvioi A+B-yhdistelmää vielä: tarvitsemme yhden muuttujan päätöksen.
```

Kuun oletussirppi 0,22 kylvää vain `0,2 × 0,22² ≈ 0,97 %` matriisista eli
keskimäärin noin 3–4 johtoa. Täysikuu kylvää noin 70. Kuu muuttaa vain
**tiheyttä**; shaken painon antaa edelleen `CQ.shake` (GUI:ssa nykyinen 0,3),
ja `patchDepth` on jo istutetun kentän yhteinen elävyyssäädin.

## Mitä palautteeksi tarvitaan

Seuraavasta kierroksesta riittää, jos vastaat näihin:

1. Testissä 4: onko `triK`:n jatkuvan liikkeen ja `casK`:n sisäisesti
   kellotetun liikkeen ero sekä korvalla että uuden keskikielen avulla selvä?
2. Shakessa: kandidaatti A (`Spaf 0,12`) vai B (`amount 0,65`) tuntuu
   soittimelta eikä satunnaisefektiltä — vai ei kumpikaan?
3. Testissä 6: löytyvätkö Level-ranta, Organ-malto, Q-kidus, kaksi SlotX-suuntaa
   ja linnan tap/send-erottelu ilman tekstiä, ja ovatko vetoalueet soitettavat?

16n:n rinnakkainen käsituntumatesti on tiedostossa
[`tests/manual_checklist.md`](../tests/manual_checklist.md) kohdassa `16N-1` ja
yhden silmäyksen kartta tiedostossa [`16N_MAP.md`](16N_MAP.md).
