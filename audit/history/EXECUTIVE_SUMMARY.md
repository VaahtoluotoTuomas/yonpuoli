# Johdon yhteenveto (alkuperäinen auditointi)

> **Pääarvioinnin status 2026-07-19:** tämä on alkuperäisen auditin yhteenveto.
> Lopulliset prioriteetit ja reproduktiot ovat `FINAL_AUDIT.md`:ssä ja
> `VERIFIED_FINDINGS.md`:ssä: CQ-AUD-001 on ainoa P0, CQ-AUD-002–004 ovat P1.
> Auditoinnin jälkeen hyväksytyn GUI:n autoritatiivinen määrittely on repositoryn
> `GUI_SPEC.md`.

## Auditointipäätelmä

Repository on poikkeuksellisen pitkälle dokumentoitu ja sen keskeinen DSP-rakenne on nykyisillä, erillisillä lähtökanavilla ajettuna vakaa: käännös onnistui, 79 testimetodia tuotti 3082 hyväksyttyä assertiota ja 0 epäonnistumista, 90 sekunnin NRT-mayhem pysyi 0,89:n huipussa ja Coco hold -testit säilyttivät signaalin. Arkkitehtuurissa on myös useita suojattavia vahvuuksia: keskitetty parametri- ja matriisifunneli, selkeä group-järjestys, kaksi erillistä RAM-silmukkaa, BufRd ennen BufWr:ää sekä yhtenäinen main/stem-turvaketju.

Nykyistä v1.0-tilaa ei silti pitäisi julkaista uutena jakeluna ilman kahta P0-korjausta:

1. Hyväksytty output-konfiguraatio sallii main- ja stem-signaalien summauksen samalle fyysiselle kanavalle limiterien jälkeen. Kahden kanavan offline-summat nousivat arvoihin 1.4869–1.7324 ja tässä renderissä kaikkien neljän offline-summa arvoon 3.2192 (+10,16 dBFS), vaikka yksittäisen kanavan luvattu katto on 0,89. Stem-overlap ohittaa myös masterVol-tasonsäädön. Tämä on varmistettu turvallisuusvika, CQ-AUD-001.
2. Recorderin käynnistys ja pysäytys eivät ole serialisoituja. Nopea kaksois-REC tai REC + välitön stop voi menettää DiskOut-synthin, bufferin tai tiedoston omistuksen. Mekanismi on staattisesti vahva, mutta live-reproduktio jätettiin tekemättä datariskin vuoksi, CQ-AUD-002.

P1-tasolla serverin reboot-semanttiikka on varmistetusti väärä: CQ.reboot rakentaa
graphin uudelleen mutta ei reboottaa jo käyvää scsynthiä, joten boot-only
ServerOptions-muutokset eivät astu voimaan. Boot/free- ja deck-load-polut tarvitsevat
async-omistuksen ja lifecycle-suojauksen. Tilalliset Coco-, Quantussy- ja
Space-silmukat tarvitsevat erikseen kirjoitusrajan NaN/inf-sanitoinnin,
diagnostiikan ja kohdennetut integraatiotestit.

Auditin julkaisusuositus on siksi:

- **No-go uudelle release-artefaktille**, kunnes CQ-AUD-001 ja CQ-AUD-002 on korjattu ja niiden hyväksymistestit läpäisevät.
- **P1-löydökset ennen seuraavaa vakaaksi nimettyä versiota.**
- P2/P3-työ voidaan vaiheistaa, mutta preset-semanttiikka, linked stereo, GUI:n todellinen tila ja testien hermettisyys kannattaa hoitaa ennen laajempaa käyttöä.

## Laajuus ja menetelmä

Auditoinnin ensisijainen ohje oli repositoryn
`audit/prompts/AUDIT_PROMPT.md`. Luettiin kokonaan:

- README.md, SPEC.md, ARCHITECTURE.md, nykyinen
  `docs/history/PLAN_v1.0.md` ja CLAUDE.md
- tests/manual_checklist.md
- kaikki classes-, tests-, presets- ja config-esimerkkitiedostot
- paikallisen SuperCollider 3.13:n relevantti Server.waitForBoot/Server.reboot-toteutus.

Työ jaettiin rinnakkain arkkitehtuuri/runtime-, DSP/fidelity- sekä feature/GUI-osuuksiin. Kaikki osuudet valmistuivat ennen synteesiä. Lähdekoodia ei muutettu; vain audit-hakemiston raportit kirjoitettiin.

AGENTS.md:ää tai erillistä TODO/known-issues-tiedostoa ei löytynyt. Nykyinen
`docs/history/PLAN_v1.0.md` toimi auditointihetkellä osittain status- ja
tehtävähistoriana. Repositoryssa ei myöskään ole lähdeketjua, jolla
Cocoquantus/Ciat-Lonbarde-historialliset väitteet voisi vahvistaa itsenäisesti;
fidelity-raportti erottaa siksi projektidokumentaation väitteet, koodihavainnot ja
esteettisen tulkinnan.

## Oikeasti ajettu testinäyttö

| Tarkistus | Tulos |
|---|---|
| SuperCollider compile | exit 0, CQ_COMPILE_OK |
| Unit/headless | exit 0, 3082 läpäisyä, 0 epäonnistumista, CQ_TESTS_PASSED |
| NRT safety | CQ_SAFETY_PASSED |
| NRT hold integrity | CQ_HOLD_OK |
| 90 s mayhem | peak 0.89, DC L 0.00016, DC R 0.00035 |
| Coco-stress | peak 0.8709 |
| Quantussy-stress | peak 0.6228 |
| Space-stress | peak 0.8866 |
| 4-kanavainen main+stem | overall peak 0.8664; stem1 0.7584; stem2 0.765 |
| Master-bus NaN | ei päässyt ulostuloon |
| Hold A4 | peak ennen/jälkeen 0.547, korrelaatio 1.0 |
| Hold A5 | RMS 0.01622, drift 0.0 dB |
| Preset-järjestysrepro | selfNoise → classic: 13 eroa; quantumDrone → empty: 20 eroa |
| Output-overlap-repro | kahden kanavan summat 1.4869–1.7324; kaikki neljä 3.2192 (+10,16 dBFS), vain offline |

Compile- ja unit-ajojen onnistumismarkerien jälkeen sandbox esti SuperColliderin
normaalin archive.sctxar-kirjoituksen käyttäjän AppDataan. NRT:n ensimmäinen suora
ajo törmäsi lisäksi oletus-temp-polun estoon jo renderin aikana, joten hyväksytyt
NRT-ajot tehtiin kertaluonteisella stdin-wrapperilla, joka ohjasi temp- ja
archive-hakemistot tests/tmp:hen. Nämä olivat auditointiympäristön rajoitteita,
eivät projektin test failureja.

Yksityiskohtaiset komennot, arvot, wrapper ja testikattavuuden rajat ovat raportissa TEST_EVIDENCE.md.

## Löydösten jakauma

| Vakavuus | Määrä | Keskeinen teema |
|---|---:|---|
| P0 | 2 | fyysisen lähtösumman turvallisuus; tallennuksen omistus |
| P1 | 4 | todellinen server-reboot; boot/free- ja deck-lifecycle; tilalliset NaN/inf-arvot |
| P2 | 16 | presetit, stereo, smoothing, GUI:n tila/käytettävyys, testit, dokumentaatio, snapshot-luottamus |
| P3 | 3 | kvantisoinnin täsmällisyys, tiedostokoko, matriisin visuaalinen semantiikka |
| Info | 1 | historiallisten lähteiden ja repository-ohjeiden puuttuminen |

Täydet löydökset, toistot, evidenssi, korjaussuunnat ja regressioriskit ovat raportissa FINDINGS.md.

## Keskeiset vahvuudet

- CQSpec ja CQState.set tarjoavat yhden, hyvin testatun parametrifunnelin. Sama state ajaa APIa, GUI:ta ja snapshotteja.
- CQMatrix validoi reitit ja clampaa painot; boot-matriisi on tyhjä eikä piilopatchausta synny.
- Graphin sources → quantussy → cocos → fx → master -järjestys vastaa signaalin omistusta ja vähentää ajoitusyllätyksiä.
- Coco-RAMin BufRd ennen BufWr:ää on oikein tunnistettu kuormaa kantavaksi invariantiksi. Hold-testit tukevat sitä.
- Cocon kierrätys on rajattu joka kierroksella ja master/stemit käyttävät yhtenäistä LeakDC → tanh → Limiter → bad-value-squelch -ketjua.
- 90 sekunnin NRT-mayhem, matrix-FM, Space, stemit ja masteriin injektoitu NaN läpäisivät nykyiset rajat.
- GUI:n Sources | Coco 1 | Quantussy | Coco 2 | Master -hierarkia on selkeä; Coco-paneelit ovat symmetriset, ja meter/GR/LED/spesal/file-drop/shortcut-perusta on vahva.
- Vanilla-SuperCollider-UGenit, suhteelliset polut ja headless-tuki tekevät projektista siirrettävän.

Näitä ominaisuuksia ei pidä purkaa korjausten yhteydessä. Erityisesti nykyisiä safety-kynnyksiä ei pidä löysentää, BufRd/BufWr-järjestystä ei pidä muuttaa, eikä Space-feedbackin ylärajaa tai default-off-tilaa pidä poistaa.

## Olennaisimmat P1/P2-asiat P0-korjausten jälkeen

1. **Server-lifecycle:** erota graph restart ja oikea server reboot; lisää session generation, rollback ja server-quit watcher.
2. **Asynkroninen omistus:** tee deck-loadista last-request-wins ja peruutettava; sido kaikki callbackit oikeaan sessioon.
3. **DSP-palautuminen:** sanitointi ennen Coco BufWr-, Quantussy LocalOut- ja Space LocalOut -kirjoituksia sekä rate-limitattu diagnostiikka.
4. **Preset-semanttiikka:** päätä deterministisen presetin ja overlay-reseptin välillä. Nykyinen toteutus ja “empty/puhdas pöytä” -dokumentaatio eivät täsmää.
5. **Linked stereo:** säilytä dry-polun L/R-kuva ja käynnistä kanavat yhteisessä bundlessa.
6. **GUI:n todellinen tila:** micChan:nil, watchdog-trim, Space/Shake-riippuvuudet, yksiköt ja lifecycle pitää näyttää käyttäjälle oikein ennen visuaalista viimeistelyä.
7. **Testaus:** hermettinen class library, facade-lifecycle, GUI→route→DSP, 44.1/48/96 kHz ja block-size-matriisi.
8. **Dokumentaatio:** watchdog ei ole turvallisuuskomponentti; release/status- ja arkkitehtuuriväitteet tarvitsevat yhden kanonisen lähteen.

## Tässä auditissa testaamatta

Seuraavia ei pidä tulkita läpäistyiksi:

- live-audio tai varsinainen kuuntelu
- todellinen server boot/shutdown/reboot ja ServerOptions-muutoksen toteutuminen
- pitkäkestoinen live node/buffer/CPU-vuoto
- GUI:n Qt-avaus, sulkeminen, DPI, resize, accessibility, shortcutit ja file drop
- MIDI-hardware ja hotplug
- recorderin kaksoisstart/stop sekä syntyvät 2-/4-kanavaiset tiedostot
- deckin last-request-wins/free-kilpailut
- linked stereon näytetarkkuus ja dry-polku käytännössä
- vähintään neljän fyysisen ulostulon live-stemit tai stem-recording
- 44,1 ja 96 kHz sekä block-size-pyyhkäisy
- Coco-, Quantussy- ja Space-silmukoiden palautuminen sisäisestä NaN/inf-arvosta.

LISTENING_TEST_PROTOCOL.md antaa turvallisen manuaalisen protokollan. Päällekkäisiä fyysisiä output-indeksejä ei pidä testata kuulokkeilla tai kaiuttimilla; CQ-AUD-001:n hyväksyntä tehdään konfiguraatio- ja offline-renderitesteillä.

## Raportit

- EXECUTIVE_SUMMARY.md — tämä päätelmä, rajaus ja release-suositus
- FINDINGS.md — kaikki löydökset vaadittuine evidenssikenttineen
- FEATURE_TRACEABILITY.md — 79 parametrin ja ei-parametristen ominaisuuksien jäljitettävyys
- COCOQUANTUS_FIDELITY.md — dokumentoitu rakenne, toteutushavainnot ja tulkinnat eriteltyinä
- GUI_DEVELOPMENT_PLAN.md — vaiheistettu GUI-parannussuunnitelma
- LISTENING_TEST_PROTOCOL.md — turvallinen manuaalinen kuuntelu- ja hyväksyntäprotokolla
- TEST_EVIDENCE.md — suoritetut komennot, tulokset, ympäristörajoitteet ja testaamatta jäänyt
- IMPROVEMENT_BACKLOG.md — priorisoitu korjausjärjestys ja hyväksymiskriteerit
