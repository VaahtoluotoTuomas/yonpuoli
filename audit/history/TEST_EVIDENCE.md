# Testinäyttö (alkuperäinen auditointi)

Auditointipäivä: 2026-07-19
Ympäristö: Windows, SuperCollider 3.13.0, PowerShell
Repository: paikallinen klooni, `<repo>` alla

> **Huom (yksityisyysauditointi 2026-08-01):** tämän raportin
> käyttäjäkohtaiset polut on korvattu paikanpitäjillä `<repo>` (repositorion
> juuri) ja `%LOCALAPPDATA%` (Windowsin oma muuttuja). Jäljelle jätetyt
> `C:\Program Files\SuperCollider-3.13.0\…` -polut eivät sisällä
> henkilökohtaista tietoa ja ne ovat itsessään näyttöä siitä, mitä
> SuperColliderin versiota auditoitiin. Vain polkumerkkijonot muuttuivat;
> yksikään ajettu komento, mitattu tulos tai johtopäätös ei ole muuttunut.

Tämä raportti erottaa toisistaan todella ajetut testit, kertaluonteiset
kohdennetut reproducer-harnessit ja testaamatta jääneet alueet. Onnistumiseksi
ei ole laskettu pelkkää oletusta: automaattitestiltä vaadittiin testin oma
onnistumismarkeri ja onnistunut prosessin exit-status. Kertaluonteisten
harnessien tulokset on merkitty erikseen, koska niiden lähdekoodi ei kuulu
repositoryn pysyvään testisuiteen.

## Git-lähtötilanne

- Branch: `main`
- HEAD: `43724f31b8f53a407cb56bd61972ed736aabb944`
- Lyhyt commit: `43724f3`
- HEADin tarkka tagi: `v1.0`
- Commit: `v1.0: KUUNTELU 7 hyväksytty — M3 valmis (stemit live-deferred)`
- Commit-aika: `2026-07-19T12:43:55+03:00`

Auditointityövirran alussa `git status --short` oli:

```text
?? AUDIT_PROMPT.md
?? presets/a.cqsnap.scd
?? presets/b.cqsnap.scd
?? presets/fadertesti.cqsnap.scd
?? presets/mictesti.cqsnap.scd
```

Nämä olivat olemassa ennen tätä työvirtaa, eikä niitä muutettu. Auditoinnissa
ei muutettu lähdekoodia, testejä, konfiguraatiota eikä käyttäjän preset-tiedostoja.
Tämän raportin luonti lisää pyydetyt `audit/`-raportit. NRT-ajot käyttivät
repositoryn jo tähän tarkoitukseen rajattua, git-ignorettua `tests/tmp`-
hakemistoa väliaikaisiin renderi- ja archive-tiedostoihin; ne eivät muuttaneet
lähdekoodia tai versionhallittuja testejä.

## Yhteenveto ajetuista testeistä

| Testi | Tapa | Tulos |
|---|---|---|
| Class library + SynthDef compile | `tests/compile_check.scd` | PASS, `CQ_COMPILE_OK`, exit 0 |
| Serverless UnitTest-suite | `tests/run_all.scd` | PASS, 3082 pass / 0 failure, `CQ_TESTS_PASSED`, exit 0 |
| NRT DSP- ja output-turva | `tests/nrt_safety.scd` stdin-wrapperilla | PASS, `CQ_SAFETY_PASSED`, exit 0 |
| NRT hold/unity-looper | `tests/nrt_hold_integrity.scd` stdin-wrapperilla | PASS, `CQ_HOLD_OK`, exit 0 |
| Output-kanavien aliasointi | kertaluonteinen sclang-reproducer | Ongelma toistui |
| Presetien peräkkäinen overlay | kertaluonteinen serverless sclang-reproducer | Ongelma toistui, `CQ_PRESET_OVERLAY_REPRO_OK` |

## 1. Compile-testi

Ajettu komento:

```powershell
& 'C:\Program Files\SuperCollider-3.13.0\sclang.exe' -l tests/sclang_conf_dev.yaml -i none tests/compile_check.scd
```

Havaittu tulos:

- prosessin exit-status 0
- class library kääntyi; ajossa raportoitiin 353 tiedostoa ja noin 0,82 s
- `CQSpec` sisälsi odotetut 79 parametria
- `CQConfig.defaults` validoitiin
- kaikki `CQSynthDefs`-määrittelyt rakennettiin byteiksi ilman serveriä
- onnistumismarkeri `CQ_COMPILE_OK`

Compile-testin kattavuus on tarkoituksella rajattu käännökseen ja SynthDefien
rakentamiseen. Se ei käynnistä scsynthiä eikä instansioi live-nodeja.

## 2. Serverless-yksikkötestit

Ajettu komento:

```powershell
& 'C:\Program Files\SuperCollider-3.13.0\sclang.exe' -l tests/sclang_conf_dev.yaml -i none tests/run_all.scd
```

Havaittu tulos:

```text
CQ test summary: 3082 passes, 0 failures
CQ_TESTS_PASSED
```

Prosessin exit-status oli 0. Repositoryssa oli 79 `test_*`-metodia; UnitTestin
3082 passes -luku on hyväksyttyjen assertioiden määrä, ei testimetodien määrä.
Suite ajaa kaikki class libraryyn ladatut `CQ*Test`-
aliluokat. Se kattaa laajasti `CQSpec`-, config-, state-, matrix-, MIDI-logiikka-,
snapshot-, deck-navigaatio-, recorder-tiedostonimi- ja event-dispatch-logiikkaa.
Se ei käynnistä audioserveriä, joten tulos ei todista server-resurssien elinkaarta,
DiskOut-käyttäytymistä, oikeaa bufferilatausta tai GUI:ta.

## 3. NRT safety

Repositoryn dokumentoitu suora komento on:

```powershell
& 'C:\Program Files\SuperCollider-3.13.0\sclang.exe' -l tests/sclang_conf_dev.yaml -i none tests/nrt_safety.scd
```

Suora ajo törmäsi ensin sandboxin ulkopuoliseen SuperColliderin oletus-temp-
polkuun. Hyväksytty ajo tehtiin saman `sclang.exe`-ohjelman ja samojen
`-l tests/sclang_conf_dev.yaml -i none` -argumenttien kautta stdin-wrapperilla.
Wrapper suoritti ennen testitiedoston lataamista vastaavat asetukset:

```supercollider
PathName.tmp_("<repo>/tests/tmp");
Archive.archiveDir_("<repo>/tests/tmp");
"<repo>/tests/nrt_safety.scd".load;
```

Wrapperia ei kirjoitettu pysyväksi tiedostoksi. Se muutti vain sclang-prosessin
temp- ja archive-kohteet kirjoitettavissa olevaan `tests/tmp`-hakemistoon;
projektin DSP:tä tai testikynnystä ei muutettu.

Prosessin exit-status oli 0 ja lopputulos `CQ_SAFETY_PASSED`. Renderöinti käytti projektin todellisia
SynthDefejä, 48 kHz:n sample ratea ja oletuskonfiguraation 64 samplen blockia.
Testin hyväksymiskatto on −0,75 dBFS, NaN/inf ei saa päästä ulostuloon ja DC:n
itseisarvon pitää olla enintään 0,02.

| NRT-tapaus | Mitattu evidenssi |
|---|---|
| Master torture | peak 0.89; dcL 0.00011; dcR −0.00026; 480064 framea |
| Coco worst case | peak 0.8709; dcL −0.00015; dcR −0.00012 |
| Quantussy worst case | peak 0.6228; dcL 0.00002; dcR 0.00001 |
| 90 s `mayhem` | peak 0.89; dcL 0.00016; dcR 0.00035; 4320064 framea |
| Matrix castle-FM | peak 0.3989; nousevan puoliskon zero-crossingit 1209; laskevan 648 |
| Space FDN stress | peak 0.8866; dcL 0.00001; dcR −0.00007 |
| 4-kanavainen main + stems | kaikkien kanavien peak 0.8664; stem1 0.7584; stem2 0.765 |
| Master-busin NaN-injektio | injektoitu huono arvo ei tullut outputiin |

Matrix-FM-tapaus todentaa reitityksen elävyyden: positiivisen ja negatiivisen
tri1-puoliskon crossing-määrät eroavat testin vaatiman suhteen mukaisesti. Se ei
yksin todista Quantussyn kuultavaa estetiikkaa.

## 4. NRT hold integrity

Repositoryn dokumentoitu suora komento on:

```powershell
& 'C:\Program Files\SuperCollider-3.13.0\sclang.exe' -l tests/sclang_conf_dev.yaml -i none tests/nrt_hold_integrity.scd
```

Hyväksytty ajo tehtiin samalla edellä kuvatulla stdin-wrapper-menetelmällä,
mutta wrapper latasi `tests/nrt_hold_integrity.scd`-tiedoston. Prosessin exit-status
oli 0 ja lopputulos `CQ_HOLD_OK`.

- A4, hold ja ±2 oktaavin speed-sweep: bufferidumppien peak ennen ja jälkeen
  0.547; Pearson-korrelaatio 1.0.
- A5, unity looper: ensimmäisen täyden kierroksen ja kierroksen 21 RMS 0.01622;
  drift 0.0 dB; renderissä ei NaN/inf-arvoja.

### A4-testin rajoite

`nrt_hold_integrity.scd` tarkistaa A4:ssa dumppien koon, ei-hiljaisuuden ja
Pearson-korrelaation `>= 0.999`. Korrelaatio 1.0 on vahva signaalin muodon
säilymisen evidenssi, ja samat peak-arvot tukevat sitä, mutta testi ei tee suoraa
sample-by-sample- tai bittivertailua eikä raportoi maksimiabsoluuttista erotusta.
Siksi se ei yksin todista kommentissa mainittua bittitarkkaa identtisyyttä.

## 5. Kohdennettu output-aliasoinnin reproducer

Tämä oli kertaluonteinen sclang-harness, ei repositoryyn lisätty testi.
Alkuperäistä PowerShell-komentoriviä ei ole säilytetty, joten sitä ei esitetä
täsmällisenä pysyvänä testikomentona.

Reproducerissa kutsuttiin `CQConfig.validate` ainakin seuraaville olennaisille
rakenteille:

```supercollider
CQConfig.validate((outChans: [0, 0]));
CQConfig.validate((outChans: [0, 1], stemOuts: [0, 1]));
```

Molemmat hyväksyttiin: validaattori ei hylännyt saman pääkanavan kahdentamista
eikä main/stem-päällekkäisyyttä. Yhdistelmää
`(outChans: [0,0], stemOuts: [0,0])` ei ajettu alkuperäisessä harnessissa.
Sen hyväksyntä seuraa staattisesti siitä, että CQConfig validoi nämä kaksi
taulukkoa toisistaan riippumatta eikä tee duplikaatti- tai joukkoleikkaustarkistusta.

Toinen osa luki onnistuneen nelikanavaisen NRT stem-renderin ja laski
samplekohtaisesti mahdollisten päällekkäisten `Out.ar`-kirjoitusten summan:
`max(abs(sum(selectedChannels)))`. Tämä vastaa SC:n output-busin additiivista
kirjoitusta, jonka paikallinen primääriohje kuvaa tiedostossa
`C:/Program Files/SuperCollider-3.13.0/HelpSource/Classes/Out.schelp:12-18,57-58`.
Tulokset olivat:

| Simuloitu bus-konflikti | Summatun signaalin peak |
|---|---:|
| mainL + mainR | 1.7323701381683 |
| mainL + stem1 | 1.6085547208786 |
| mainR + stem2 | 1.6125874519348 |
| stem1 + stem2 | 1.4868720173836 |
| mainL + mainR + stem1 + stem2 | 3.2192421556 eli noin +10,16 dBFS |

Parikohtaiset luvut kirjattiin alkuperäisessä kohdennetussa reproducerissa.
All-four-luku laskettiin myöhemmässä read-only QA:ssa samasta säilyneestä
`tests/tmp/nrt_safety_stem.wav`-renderistä; se ei ollut alkuperäisen
`CQ_PRESET_OVERLAY_REPRO_OK`-tyyppisen marker-ajon tulos.

Jokainen summa ylittää sekä yksittäisen turvaketjun 0.89-tason että digitaalisen
full scalen 1.0. Evidenssi osoittaa, että erillisten kanavien NRT-PASS ei kata
konfiguraation sallimaa safety-chainien jälkeistä bus-summausta. Varsinaista
päällekkäisille hardware-buseille kirjoittavaa NRT- tai live-renderiä ei ajettu;
taulukko on neljän erillisen renderöidyn kanavan samplekohtainen offline-summa.
Pahinta konfiguraatiota ei pidä koskaan ajaa fyysiseen DAC-ulostuloon.

## 6. Kohdennettu preset-overlay-reproducer

Tämäkin oli kertaluonteinen serverless sclang-harness, ei pysyvä testitiedosto.
Harness käytti tuoreita language-state/matrix-instansseja, sovelsi samat sparse
preset-datat kuin `Cocoquantus.patchPreset` ja vertasi peräkkäisen presetin
lopputilaa tilanteeseen, jossa kohdepreset oli sovellettu puhtaaseen alkutilaan.

Havaittu tulos:

- `selfNoise → classic`: lopputilaan jäi 13 eroa puhtaaseen `classic`-tilaan
  verrattuna.
- `quantumDrone → empty`: lopputilaan jäi 20 eroa puhtaaseen `empty`-/default-
  tilaan verrattuna.
- harnessin onnistumismarkeri: `CQ_PRESET_OVERLAY_REPRO_OK`.

Ensimmäisen vertailun 13 residuaalia olivat molempien Cocojen `Dust`, `Fb`,
`Hold`, `SlotDeck`, `SlotPre` ja `SlotX` sekä `coco2Speed`. Toisen vertailun 20
residuaalia olivat castlejen 1, 2 ja 4 audio-ranget ja 0.4 sendit, molempien
Cocojen `Fb`, `Hold`, `SlotDeck`, `SlotPre`, `SlotQ` ja `Spaf`, sekä
`coco2Speed = 12000` ja `qChaos = 0.25`.

Markeri tarkoittaa, että overlay-käyttäytymisen reproducer suoritettiin
onnistuneesti; se ei tarkoita, että sticky-parametrit olisivat hyväksyttyä
toimintaa. Testi oli serverless, joten se todentaa language-staten eron mutta ei
erojen kuultavuutta.

## 7. Sandbox- ja temp-havainnot

Suorat compile- ja unit-ajot tulostivat omat onnistumismarkerinsa ja päättyivät
exit-statuksella 0. Markerien jälkeen SuperCollider yritti kirjoittaa
`%LOCALAPPDATA%\SuperCollider\archive.sctxar`-tiedostoa, minkä
workspace-sandbox esti. Tämä jälkikäteinen archive-virhe ei muuttanut testien
marker- tai exit-tulosta.

NRT:n ensimmäinen suora yritys tarvitsi lisäksi oletus-temp-polulle kirjoitusta
jo renderöinnin aikana. Siksi hyväksytyt NRT-ajot toistettiin stdin-wrapperilla,
joka ohjasi sekä `PathName.tmp_`- että `Archive.archiveDir_`-polun `tests/tmp`-
hakemistoon. Tämä ympäristökorjaus on erotettava projektivirheestä.

## 8. Hermettisyys ja kattavuusaukot

### Class library ei ole täysin hermettinen

`tests/sclang_conf_dev.yaml` lisää projektin `classes`- ja `tests/classes`-
hakemistot, mutta ei aseta `excludeDefaultPaths: true`. Compile-output osoitti,
että sclang kävi myös käyttäjä- ja system extension -hakemistoja ja ilmoitti
kahdesta extensionien ylikirjoittamasta metodista. Paikalliset extensionit voivat
siksi aiheuttaa toisella koneella eri pass/fail-tuloksen. Lisäksi YAML on
gitignored ja sisältää tämän koneen absoluuttiset include-polut.

Suositeltu regression-harness käyttää hermettistä language-configia, jossa
default extension -polut on suljettu pois ja vain SC:n ydinkirjasto sekä projektin
luokat/testit ovat käytössä. Projekti ilmoittaa käyttävänsä vanilla-UGeneita,
joten testien ei pitäisi riippua käyttäjän Quarkeista tai extensioneista.

### Automaatiosta puuttuvat

- `Cocoquantus`/`CQGraph` live boot, free ja todellinen server reboot
- toistettu ja kesken bootin tapahtuva start/stop
- node-, group-, bus- ja bufferimäärien vertailu ennen/jälkeen
- recorderin asynkroninen double-start, immediate-stop ja teardown-race
- deck-loadien järjestys sekä load/free-race
- GUI:n open/close/reopen ja deferred-init-teardown
- oikean MIDI-laitteen connect, rescan, disconnect ja hot-plug
- oikean vähintään nelikanavaisen äänikortin hardware-stemit
- automaattinen testi duplicate/overlap-output-konfiguraatiolle
- presetien täydellisen kohdetilan/overlay-semanttiikan eksplisiittinen regressiotesti
- 44,1/48/96 kHz:n sample-rate-sweep ja useiden block size -arvojen sweep
- pitkäkestoinen resurssi- ja CPU-soak/leak-testi
- ihmisen suorittama kuuntelutesti

Nykyinen NRT käyttää 48 kHz / 64 samplea. Sen tuloksia ei pidä yleistää ilman
erillistä sample-rate- ja block-size-matriisia.

## 9. Testaamatta jääneet ympäristöriippuvaiset alueet

Seuraavia ei auditoitu onnistuneena live-testinä tässä ympäristössä:

- kuultava live audio ja latency
- live scsynthin boot/shutdown/reboot sekä device-option muutokset
- GUI:n avaaminen, sulkeminen, skaalaus, hiiri ja näppäimistö
- fyysinen 16n/MIDI
- hardware-stemien reititys vähintään nelikanavaisella interface-laitteella
- recorderin ja deckin tunnistetut concurrency-racet live-serverillä
- 44,1–96 kHz:n ja eri block size -arvojen runtime-matriisi
- pitkäkestoinen node/buffer/bus/Routine-vuotojen seuranta
- Cocoquantus-estetiikan ja presetien varsinainen kuunteluhyväksyntä

Näitä ei saa merkitä läpäistyiksi compile-, unit- tai NRT-markerien perusteella.

## Lopputila

Raportin kirjoittamisen jälkeen lähdekoodin tracked-tila on edelleen muuttumaton.
Viimeisessä tarkistuksessa `git status --short` oli:

```text
?? AUDIT_PROMPT.md
?? audit/
?? presets/a.cqsnap.scd
?? presets/b.cqsnap.scd
?? presets/fadertesti.cqsnap.scd
?? presets/mictesti.cqsnap.scd
```

`audit/` sisältää rinnakkaisten auditointityövirtojen kahdeksan pyydettyä raporttia.
Auditoinnin pysyvät repository-kirjoitukset rajattiin näihin raportteihin; NRT:n
git-ignorettu `tests/tmp`-sisältö on väliaikaista testiaineistoa.
