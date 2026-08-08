# SlotX/Dust-Nightin UI-korjaussuunnitelma

> **Status 2026-08-08: hyväksytty.** Tässä kuvattu UI-korjaus on toteutettu.
> Automaattinen geometria-, unit- ja Qt-portti sekä compact/roomy-kuvasarja ovat
> vihreät, ja toteuttajan kuvakatselmus löysi vain speckien mist-pään liian
> himmeän alfan; se korjattiin pitämällä chip-pään kirkkaus ennallaan. Ääni ja
> toiminta on hyväksytty. SlotX on myös visuaalisesti hyväksytty ja jäädytetty.
> Käyttäjä hyväksyi compact/fullscreen-kuvat ja oikean sovelluksen lopullisen
> UI:n 2026-08-08. Checklisti on rastittu. Shake-karanteenia ei poistettu, koska
> se on hyväksynnästä erillinen musiikillinen päätös.

Tämä dokumentti on toteuttajan työohje. Se syrjäyttää SlotX/Dust-välivaiheen
väitteen valmiista visuaaliportista, mutta **ei** avaa hyväksyttyä DSP:tä,
matriisilakeja, eleitä tai SlotX:n ulkoasua uudelleen.

## 1. Rajaus: mitä ei saa muuttaa

Seuraavat asiat ovat käyttäjän hyväksymiä ja ne on pidettävä bit- tai
käytösidenttisinä:

- SlotX:n kaksi suuntaa, silta, venttiilit, socketien paikat, telemetry,
  matriisilaki, eleet ja koko SlotX:n visuaalinen toteutus;
- 16×32-matriisi ja sen nimet, indeksit, serialisointi sekä undo;
- `cocoNDust`-alue 0…0,5, `cocoNDustGrain`-alue 0…1 ja Grainin täsmällinen
  legacy-keskikohta 0,5;
- Dustin signed-square-matriisiosuus ±0,18 ja lopullinen 0…0,5-clamp;
- Dustin paikka Cocon kirjoitusketjussa, Hold-integriteetti, telemetry ja
  safety-lait;
- Dust-ele: Coco–runko-suuntainen veto muuttaa amountia ja kohtisuora veto
  Grania; `dustSensitivity = 120` ei muutu;
- Dust-usvan nykyinen noin 106×72 body-px:n epäsäännöllinen keho, värikieli,
  höyrylangat, poikittainen luusiemen ja amount/grain-merkitys. Ongelma on
  sommittelu ja hiukkastiheys, ei kehon tyylin epäonnistuminen;
- morph-lehtien hyväksytty ylä-/alareuna 0,13/0,90 sekä niiden 5 px
  ground-vallihauta. TAB-silmä erotetaan näistä omaksi sijaintipäätöksekseen;
- juurten drive/bias-eleet, parametrit, lähdevalinta, env-haara ja
  audioyhteys;
- Nightin cable-first-, hover-, undo- ja fokuslait;
- SlotX/Dust-shake-karanteeni. Sitä ei poisteta ennen tämän visuaaliportin
  käyttäjähyväksyntää.

Korjaukseen ei lisätä animaatiota, uutta ajastinta, telemetry-viestiä,
parametria, tekstiä, paneelia eikä uutta väriä.

## 2. Todetut viat ja mitattu lähtötilanne

Referenssilavat ovat:

- compact/windowed: 1280×570 px Night-lava eli 1280×720-ikkuna;
- roomy/fullscreen: 1920×855 px Night-lava eli 1920×1005-ikkuna.

Nykygeometria mitattiin `CQNightGeometry.compute`-tuloksesta todellisia
`endpointBox`- ja `crescentViewBox`-laatikoita vasten. Kyse ei ole vain
kuvakaappauksen optisesta vaikutelmasta.

| Vika | Nykyinen syy | Mitattu todiste |
|---|---|---|
| Molempien Dustien lähtö on ahdas | Runko, socket ja kuoresta alkava suora viiva käyttävät kaikki samaa 37° sädettä. Socket on vain 150 body-px päässä Cocon keskustasta ja suoni kulkee 118…150 body-px välillä. | Compactissa kummankin suoni menee oman skip-merkin 32×32-alueen läpi. Lisäksi Dust 1:n ääriviiva menee `coco1_skip`-endpointin todelliseen näkymälaatikkoon. |
| Oikean Dustin ulkokaari leikkautuu SP.AF:iin | Sama 198 body-px:n reach jättää usvan liian lähelle oikean Cocon SP.AF-sirppiä. Myöhemmin luotu, opaque sirppinäkymä peittää Dust-kerrosta. | Compactissa ääriviiva menee `coco2`-SP.AF-sirpin 44×44-laatikkoon. Roomy-koossa ääriviivan pienin väli laatikkoon on vain 1,66 px. |
| Vasemman juuren bias-pallot jäävät dolbyn alle | PLAY-8:n Coco-siirto muutti Cocon ja paikallaan pysyneen juuren suhdetta. Nykyinen testi tarkistaa vain juuriviivat tavallista 44 px sirppilaatikkoa vasten. Se ei sisällä bias-palloja eikä 72 px korkeaa dolby-näkymää. | Compactissa pahimman bias-asennon juurikuori `Rect(134.42, 475.65, 38.36, 36.55)` leikkaa input-VCA:n dolby-laatikkoa `Rect(144.61, 421.79, 44, 72)`. Roomy-koossa sama kohta ei vielä leikkaa, joten vika näkyy nimenomaan windowed-tilassa. |
| TAB-silmä osuu ringiin | Morph-kentän siirrossa `tideGateFraction` muutettiin 0,94 → 0,86, vaikka silmä ei tarvitse samaa y-rajaa kuin alalehdet. | Silmän keskipiste on vain 2,8 px ringin alareunan alla compactissa ja 4,2 px roomyssa. Vanha 0,94-paikka antaa 48,4/72,6 px keskiviivan välin. |
| Suurella Dustilla on liian vähän speckejä | Geometria luo vain 14 hiukkasta. Piirto paljastaa niiden listan etuosan ehdolla `(i + 1) / 14 <= sqrt(amount)`. | Maksimissakin näkyy vain 14 sirpaletta; suuri amount näyttää siksi harvalta. |

Dust 1:n nykyinen compact-keskus on noin `(379.1, 395.6)`, socket
`(340.8, 366.7)` ja suonen kuoripää `(315.2, 347.4)`. Dust 2 peilautuu
keskukseen `(900.9, 395.6)`. Nämä ovat regression lähtöarvoja, eivät uuden
sommittelun tavoitteita.

### Miksi z-järjestys tai läpinäkyvyys ei ole korjaus

Dust-näkymä on jo läpinäkyvä. Leikkautuminen syntyy siitä, että myöhemmin
piirtyvät endpoint- ja sirppinäkymät osuvat sen oikeaan ääriviivaan. Jos Dust
vain nostettaisiin niiden päälle, se puolestaan peittäisi tai varastaisi
ohjainten vihjeitä. Käyttäjä totesi lisäksi aivan oikein, että sommittelu olisi
silti liian ahdas, vaikka yhtään pikseliä ei leikkautuisi. Siksi korjaus tehdään
geometrialla ja mitatulla negatiivisella tilalla, ei kerrosjärjestyksellä.

## 3. Valittu Dust-ratkaisu

### 3.1 Poista jatkuva Coco–Dust-viiva, säilytä omistajuus

Nykyistä kuoren reunasta socketiin kulkevaa jatkuvaa suonta ei pidennetä. Se
joutuisi edelleen lähtemään ohjaimilla täytetystä sektorista ja muodostaisi
uuden näennäisen reitin skipin, SP.AF:n tai VCA:n kanssa.

Sen tilalle tulee **Dust-runkoon kuuluva katkaistu hengitysjuuri**:

- juuri päättyy Dust-socketiin ja osoittaa täsmälleen kohti oman Cocon
  keskustaa;
- juuren Coco-puoleinen pää jää vapaaseen veteen eikä koske kuorta tai mitään
  Cocon ohjainta;
- juuri on 24 body-px pitkä ensimmäisessä kandidaatissa;
- se piirretään kolmessa peräkkäisessä ja aukottomassa, samaan suuntaan
  jatkuvassa osassa niin, että alpha kasvaa hienovaraisesti socketia kohti.
  Coco-pää saa häipyä, socket-pää on luettavin;
- se ei ole kaapeli, uusi reitti eikä uusi hit-alue. Dust-keho säilyy ainoana
  amount/grain-eleen pintana ja socket tavallisena patch-pisteenä.

Omistajuus säilyy Gestaltin neljällä yhtäaikaisella vihjeellä: yksi usva per
Coco, peilattu sijainti, Cocon suuntaan osoittava kehon amount-akseli ja juuri,
sekä socket kehon Cocon-puoleisella reunalla. Täysi yhdysviiva ei ole tarpeen.

### 3.2 Uusi ensimmäinen sijoituskandidaatti

`CQNightMeasures`-taulukon ensimmäinen testattava ehdokas on:

| Mitta | Nykyinen | Ensimmäinen ehdokas |
|---|---:|---:|
| Cocon keskus → Dust-kehon keskus | `dustReach = 198` | `dustBodyReach = 240` body-px |
| kulma sisäänpäin osoittavasta vaaka-akselista alas | `dustAngle = 37°` | `dustBodyAngle = 32°` |
| jatkuva suoni | kuorietäisyys 118 → socket-etäisyys 150 | poistetaan |
| katkaistu hengitysjuuri | — | `dustTailLength = 24` body-px, socketista Cocoa kohti |
| kehon säteet | 53×36 | **ei muutosta** |
| socketin inset | 48 | **ei muutosta** |
| eleherkkyys | 120 | **ei muutosta** |

Tämä siirtää compactissa rungon selvästi sisemmäksi mutta lähes samalle
korkeudelle. Socketin etäisyys Cocon keskustasta kasvaa 150 → 192 body-px ja
hengitysjuuren vapaa pää alkaa noin 168 body-px etäisyydeltä. Se jättää
lähtöarviona noin 12 px välin skip-merkin todelliseen laatikkoon ilman että
kehoa tarvitsee pienentää.

Nämä kolme uutta mittaa ovat **kandidaatteja**, eivät silmämääräisesti
hyväksyttyjä vakioita. Toteuttaja saa virittää niitä vain seuraavassa
järjestyksessä ja aina molemmille puolille peilattuna:

1. Jos oma shell-ohjain tai sen näkymälaatikko jää liian lähelle, kasvata
   `dustBodyReach`-arvoa 4 body-px kerrallaan, enintään arvoon 252.
2. Jos keho lähestyy linnan näkyvää lihaa/range-värettä, muuta kulmaa 2°
   kerrallaan kohti alempaa avovettä; tarkista samalla alalehdet ja lavaraja.
3. Jos keho lähestyy alalehteä tai lavarajaa, palauta kulmaa 2° ylöspäin.
4. Älä ratkaise törmäystä pienentämällä `dustRx`, `dustRy`, socketia,
   ohjainlaatikoita tai hit-alueita.
5. Älä lisää vasemmalle ja oikealle omia käsin arvattuja reach-/angle-arvoja.
   Yksi peilattu sommittelulaki säilyy; seedatun ääriviivan molemmat todelliset
   muodot on vain testattava.

### 3.3 Pakolliset Dust-väljyydet

Sekä 1280×570- että 1920×855-lavalla on toteuduttava:

- seedatun Dust-ääriviivan ja minkä tahansa muun endpointin todellisen
  `endpointBox`-laatikon väli vähintään 12 px;
- ääriviivan ja kaikkien `crescentViewBox`-laatikoiden väli vähintään 12 px;
- oman Dust-socketin todellisen endpoint-laatikon ja skip-/chevron-merkin
  32×32-laatikon väli vähintään 12 px;
- hengitysjuuren kaikkien näytepisteiden ja edellä mainittujen ohjainlaatikoiden
  väli vähintään 10 px;
- ääriviivan ja linnan näkyvän seeded-blobin, range-väreen, morph-siemenen
  `seedHalo`-laatikon sekä juuren näkyvän musteen väli vähintään 12 px;
- Dust-näkymälaatikko sisältää koko ääriviivan, socketin todellisen
  endpoint-laatikon, hengitysjuuren ja pisimmän hiukkassirun sekä nykyisen
  `dustBoxPad`-marginaalin;
- molempien usvien väliin jää selvä keskinen negatiivinen tila. Nykyinen testi
  `dust[0].box.right < dust[1].box.left` säilyy, mutta se ei yksin riitä;
- mitään Dustin näkyvää mustetta ei saa joutua opaque Qt-näkymän alle.

Suojattujen esteiden lista rakennetaan todellisesta geometriasta, ei käsin
kirjoitetuista koordinaateista. Siihen kuuluvat vähintään kaikki Cocon vanhat ja
uudet endpointit, skip/chevron, kaikki kolme sirppiä täysine dolby-korkeuksineen,
Organ/SlotQ/SlotX-käsialueet, linnat ja niiden endpointit, morph-siemenet,
juurimuste sekä lavarajat. Oman Dustin oma socket jätetään luonnollisesti pois
sen kehon vastaisesta törmäyslistasta.

## 4. Dust-speckien tiheys ja liike

### 4.1 Ensimmäiseen korjaukseen tulee enemmän mutta täysin staattisia speckejä

Nosta deterministinen hiukkaspooli 14 → **36**. Älä käytä satunnaista uutta
geometriaa jokaisella repaintilla. Jokaisella hiukkasella on seedatusti:

- kehon sisäinen paikka;
- oma tasaisesti jakautuva activation-threshold;
- grainin määräämä akseli/slant;
- pieni pituus- ja alpha-jitter.

Paikat pitää jakaa koko usvakehoon esimerkiksi golden-angle- tai
sektorikerrostuksella, jotta listan ensimmäiset alkiot eivät muodosta yhtä
rypästä. Näkyvyysehto saa käyttää amount-käyrää
`threshold <= amount.pow(0.72)`. Pakolliset tulokset 36 hiukkasen poolilla ovat:

- amount 0: 0 speckiä;
- amount 0,25: noin 13 speckiä;
- amount 0,5: noin 22 speckiä;
- amount 0,8: vähintään 30 speckiä;
- amount 1: kaikki 36 speckiä.

Grain muuttaa edelleen sirujen pituutta, alpha-arvoa ja mist/chip-luonnetta,
mutta se ei saa tyhjentää suuren amountin hiukkaspopulaatiota. Kaikkien
speckien ja niiden koko siruviivan on pysyttävä Dust-kehon tai sen turvallisen
piirtomarginaalin sisällä.

36 staattista hiusviivaa syntyy vain Dust-näkymän state/hover-repaintissa eikä
luo jatkuvaa CPU-kuormaa.

### 4.2 Liike jää erilliseksi myöhemmäksi A/B-kokeeksi

Ensimmäiseen korjaukseen **ei** lisätä liikettä. Syyt:

- Nightin nykyinen laki on, ettei instrumentti liiku ilman musiikillista tai
  telemetry-syytä;
- hienovarainenkin ambient-animaatio vaatisi jatkuvan repaint-ajastimen;
- tässä vaiheessa on ensin erotettava liian harvan tekstuurin ongelma
  liikkeen kaipuusta;
- liike vaikeuttaisi uuden sommittelun ja leikkautumisen arviointia.

Jos staattinen 36-speckin versio hyväksytään ja käyttäjä haluaa myöhemmin
kokeen, se tehdään omana `DUST-MOTION`-porttinaan. Sen enimmäisraja olisi 3–5
hyvin himmeää speckiä, enintään 2 px:n ajelehtiminen 8–12 sekunnissa, repaint
enintään 6 Hz, ei liikettä amountin ollessa nolla eikä koskaan keholle,
ääriviivalle, socketille tai hit-geometrialle. Tätä ei toteuteta tämän
suunnitelman ensimmäisessä passissa.

## 5. Vasemman juuren korjaus

### 5.1 Testiaukko suljetaan ensin

Nykyinen `test_floodRootsClearTheLowerWater` kerää `rootInk`-listaan vain:

- source-strandien näytteet;
- audio-veinin näytteet ja joinin;
- env-haaran pisteet.

Se ei sisällä `drawPreampRoot`-metodin kahta bias-loboa ja käyttää kaikille
sirpeille 44×44-laatikkoa. Korjaa testi käyttämään:

- bias-lobojen koko pahimman asennon mustekuorta;
- `CQNightGeometry.crescentViewBox(center, m > 0)`-laatikkoa, jolloin in/fb
  käyttävät todellista 44×72 dolby-paperia;
- myös juurisolmun 27 px hit-kehää, jotta dolby-näkymä ei peitä eleen alkua.

Siirrä `drawPreampRoot`-metodin lobeihin vaikuttavat nykyiset literaalit
`0.42`, `0.72`, `0.48`, biasin `+0.48` ja `-0.30` joko
`CQNightMeasures`-taulukkoon tai yhteen jaettuun puhtaaseen apurakenteeseen.
Piirto ja törmäystesti eivät saa laskea eri kuorta.

### 5.2 Sijainti ratkaistaan todellisesta dolby-laatikosta

Älä nosta juurta dolbyn päälle z-järjestyksellä äläkä pienennä bias-palloja.
Laske ensin nykyinen nimellinen knot-paikka. Sitten kummallekin juurelle samalla
algoritmilla:

1. muodosta pahimman bias-asennon lobe-envelope;
2. etsi oman Cocon kaikki 44×72 dolby-laatikot, joiden x-alue voi kohdata
   envelopen;
3. jos envelope leikkaa laatikkoa tai jää alle 12 px päähän, kasvata knotin
   y-koordinaattia juuri sen verran, että envelopen yläreuna on dolby-laatikon
   alareunasta vähintään 12 px alempana;
4. varmista samalla, että knotin 27 px hit-kehä ei leikkaa dolby-laatikkoa;
5. clampaa lopputulos niin, että koko envelope jää vähintään 8 px lavarajan
   yläpuolelle;
6. laske source-strandit, audio-vein, env-haara, label ja `root.box` vasta
   korjatusta knotista.

Nykyisillä compact-mitoilla input-dolby päättyy y≈493.79 ja pahimman
bias-envelopen yläulottuma knotista on noin 20.35 px. 12 px väljyys antaa
vasemman knotin tavoite-y:ksi noin **526.14** eli noin 30.1 px nykyistä alemmaksi.
Roomy-koossa sama collision-aware laki tekee vain tarvitsemansa pienen
korjauksen. Oikealle ei kirjoiteta omaa erikoislukua: sama lasku jättää sen
paikalleen, jos väljyys jo toteutuu.

## 6. TAB-silmän korjaus

Pidä morph-lehdet arvoissa 0,13/0,90. Palauta vain
`tideGateFraction` **0,86 → 0,94** ja muuta sopimus niin, ettei silmän
keskipisteen tarvitse olla morph-suorakulmion sisällä. Silmä on kentän
fokuskytkin sen alapuolella, ei viides morph-siemen.

Pakolliset ehdot molemmilla referenssilavoilla:

- silmän keskiviivan etäisyys sampled-ringin alimmasta näkyvästä kohdasta on
  vähintään `tideGateHit + 20` eli 44 px;
- koko `tideGateBox` pysyy lavalla ja sen alle jää vähintään 8 px;
- laatikko ei leikkaa linnan, endpointin, Dust-kehon tai morph-siemenen
  käsialuetta;
- silmä pysyy x-suunnassa ringin keskellä;
- Tab, klikkaus, fokus, kaapelien backgrounding ja undo-käytös eivät muutu.

Poista nykyisestä testistä vaatimus `tide.containsPoint(gate)`. Korvaa se
edellä olevilla todellisilla väljyys- ja lavarajavaatimuksilla sekä väitteellä,
että gate on alalehtien alapuolella. Arvolla 0,94 odotetut ring-välit ovat noin
48,4 px compactissa ja 72,6 px roomyssa.

## 7. Toteutusjärjestys tiedostoittain

### Vaihe A — kirjoita ensin epäonnistuvat geometriatestit

Muokkaa `tests/classes/CQNightGeometryTest.sc`:

1. Laajenna Dust-testi ottamaan todelliset endpoint-, sirppi-, mark-, linna-,
   juuri- ja seed-esteet. Testaa erikseen ääriviiva, socket-laatikko ja
   hengitysjuuren näytepisteet edellä määritellyillä 12/10 px rajoilla.
2. Lisää regressioväitteet nimeltä mainituille vioille:
   `coco1_skip`, vasen skip-mark ja oikea SP.AF-sirppi.
3. Laajenna juuritesti koko bias-envelopeen, 44×72 dolby-paperiin ja
   root-knotin hit-kehään.
4. Korvaa tide-gaten “sisällä kentässä” -väite ring-väljyydellä,
   alalehtien-alapuolella-väitteellä ja lavarajalla.
5. Testaa kaikki kohdat sekä koolla `[1280, 570]` että `[1920, 855]`.

Testien pitää epäonnistua nykygeometrialla juuri käyttäjän ilmoittamissa
kohdissa ennen tuotantogeometrian muuttamista.

### Vaihe B — erota Dustin mitat ja geometria

Muokkaa `classes/CQNightMeasures.sc`:

- korvaa `dustReach` nimellä `dustBodyReach`;
- korvaa `dustAngle` nimellä `dustBodyAngle`;
- poista shelliin sidottu `dustVeinOutset`;
- lisää `dustTailLength`, `dustBodyClear`, `dustTailClear` ja
  `dustParticleCount`;
- pidä `dustRx`, `dustRy`, `dustSocketInset`, `dustHitSlack`, `dustBoxPad` ja
  `dustSensitivity` ennallaan.

Muokkaa `classes/CQNightGeometry.sc`:

- laske kehon center/axis/tangent uusista body-mitoista;
- pidä socket kohdassa `center - axis * dustSocketInset * scale`;
- laske `tailStart = socket - axis * dustTailLength * scale`;
- älä enää laske `veinStart`-pistettä Cocon kuoresta;
- tallenna hengitysjuuri omalla nimellä, esimerkiksi `ownershipTail`, jotta
  vanha shelliin ulottuva semantiikka ei jää piiloon;
- sisällytä koko tail, socketin todellinen endpoint-laatikko, ääriviiva ja
  hiukkasten pisin mahdollinen siru `dust.box`-laskuun;
- generoi 36 seedattua, koko kehoon hajautettua hiukkasrakennetta omine
  threshold-arvoineen.

### Vaihe C — piirrä muuttamatta elettä tai kerroslakia

Muokkaa `classes/CQInkNight.sc`:

- korvaa kaksi `Pen.line(veinStart, socket)` -piirtoa katkaistun
  `ownershipTail`-juuren 3-osaisella, Cocoa kohti häipyvällä piirrolla;
- pidä juuri täysin staattisena ja hiusviivamaisena;
- käytä hiukkasten omia threshold-arvoja ja 36-speckin amount-käyrää;
- pidä grainin nykyinen mist/chip-vaikutus, kehon kasvu, vapour threads,
  transverse spine ja telemetry-wake ennallaan;
- älä muuta `CQGuiNightTouch.dustAxisNorms`-logiikkaa.

`classes/CQGuiNightMap.sc`:ssa pidä Dust-layer läpinäkyvänä ja samassa
kerrosjärjestyksessä. `prLayout` saa edelleen boundsin geometriasta. Jos uusi
box on laskettu oikein, kartassa ei tarvita z-korjausta.

### Vaihe D — korjaa juuri ja gate erillisinä pieninä muutoksina

1. Lisää yhteinen root-lobe-envelope ja collision-aware knot-y-ratkaisu
   `CQNightMeasures`/`CQNightGeometry`-tasolle; pidä Ink sen yhden totuuden
   kuluttajana.
2. Palauta `tideGateFraction = 0.94` ja päivitä vain gatea koskeva geometria,
   testi ja kommentit. Älä liikuta lehtiä uudelleen.
3. Älä niputa näihin mitään muuta Night-polishia.

### Vaihe E — automaattiportit

Kaikki headless-SuperCollider-ajot on ajettava vain wrapperilla
`tests/run_sclang_gate.ps1`. Vähimmäisportti UI-korjaukselle on:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/compile_check.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/run_all.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_night_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_night_capture.scd
```

Jos DSP-, state-, matrix- tai gesture-tiedosto muuttuu vahingossa, muutos on
rajauksen vastainen. Se pitää perua; tätä UI-passia ei käytetä äänen uudelleen
virityksen porttina.

## 8. Pakolliset kuvakaappaukset ja käsikatselmus

Laajenna `tests/gui_night_capture.scd` tuottamaan vähintään seuraavat kuvat
sekä compact- että roomy-koossa:

- rest: Dust amount 0, Grain 0,5, ei kaapeleita;
- high mist: amount 1, Grain 0;
- high chip: amount 1, Grain 1;
- telemetry/cables: Dust-socket patchattuna ja wake näkyvissä;
- root bias 0 ja root bias 1;
- tide focus suljettuna ja avattuna.

Katselmuksessa zoomataan erikseen:

1. vasen skip/chevron/Dust-väli;
2. oikea SP.AF/Dust-väli;
3. molempien Dustien suhde omaan Cocoon, lähimpään linnaan ja alalehteen;
4. vasemman input-dolbyn ja bias-pallojen väli windowed-tilassa;
5. TAB-silmän väli ringiin ja alareunaan molemmissa koissa;
6. suuren amountin 36-speckin tekstuuri molemmilla Grain-ääripäillä.

Visuaaliportti hylätään, jos:

- jokin ääriviiva, socket, specki tai juuri leikkautuu;
- Dust näyttää edelleen puristuvan ulos Cocon ohjainryppäästä;
- katkaistu juuri näyttää kaapelilta tai uudelta signaalireitiltä;
- Dustin omistava Coco ei selviä kahden sekunnin silmäyksellä ilman tekstiä;
- kehoa on pienennetty väljyydentunteen saamiseksi;
- TAB-silmä näyttää osalta ringiä;
- staattinen Dust alkaa liikkua tai aiheuttaa jatkuvan repaintin;
- SlotX:n yksikin hyväksytty kohta muuttuu.

## 9. Hyväksyntä ja commit-raja

### Hyväksytty toteutusevidenssi 2026-08-08

- Lopullinen katselmuskandidaatti käyttää peilattua
  `dustBodyReach = 240`, `dustBodyAngle = 32°` ja
  `dustTailLength = 24` -geometriaa. Keho säilyi koossa 53×36 body-px ja
  eleherkkyys arvossa 120.
- Deterministinen pooli on 36 täysin staattista hiukkasta. Aktivointimäärät
  normeilla 0 / 0,25 / 0,5 / 0,8 / 1 ovat 0 / 13 / 22 / 31 / 36.
- Rootin piirto ja collision-aware knot-ratkaisu käyttävät samaa
  `preampLobeBoxes`-geometriaa; TAB-silmä on palautettu arvoon
  `tideGateFraction = 0.94` lehtien jäädessä arvoihin 0,13/0,90.
- Portit: `CQ_COMPILE_OK`, `CQ_TESTS_PASSED`, `CQ_GUI_NIGHT_PASSED` ja
  `CQ_NIGHT_CAPTURE_DONE`.
- Katselmuskuvat ovat paikallisesti `tests/tmp/slotx-dust-ui-compact-*.png` ja
  `tests/tmp/slotx-dust-ui-roomy-*.png`: rest, high-mist, high-chip,
  telemetry-cables, root-bias-0/1 ja tide-focus-closed/open.

Portti suljettiin, kun kaikki seuraavat täyttyivät yhtä aikaa:

- Dustin ääni, kaksi akselia, matriisi ja Hold ovat edelleen hyväksytyn
  toteutuksen mukaisia;
- SlotX on diffissä ja käytöksessä muuttumaton;
- kaikki numeeriset väljyystestit ovat vihreitä molemmilla referenssikoilla;
- staattinen speck-populaatio on tiheä suurella amountilla;
- root/dolby ja tide-gate/ring -regressiot ovat poissa;
- käyttäjä hyväksyi compact- ja fullscreen-kuvat sekä oikean sovelluksen.

Hyväksynnän jälkeen:

1. `tests/manual_checklist.md`:n SLOTX/DUST-UI-portti merkitään valmiiksi;
2. `PLAN.md`, `GUI_SPEC.md` ja playability guide päivitetään lopullisiin
   toteutuneisiin mittoihin;
3. shake-karanteenin poistamista harkitaan omana musiikillisena päätöksenään;
4. hyväksytty työ commitoidaan ja pushataan.

Hyväksyntä sulkee UI-portin mutta ei yksin poista shake-karanteenia.
