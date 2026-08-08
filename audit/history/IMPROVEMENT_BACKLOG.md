# Alkuperäinen audit-backlog — korvattu jatkosuunnittelussa (arkisto)

Tämä backlog ei ole toteutusloki. Auditointi ei muuttanut lähdekoodia. Järjestys perustuu FINDINGS.md:n vakavuuteen, riippuvuuksiin ja regressioriskiin.

> **Status 2026-07-19:** tämän tiedoston alkuperäiset prioriteetit ja GUI-ehdotukset
> säilyvät audit trailina, mutta eivät ohjaa toteutusta. Pääarviointi alensi
> CQ-AUD-002:n P1:een sekä CQ-AUD-005/006:n P2:een. Autoritatiivinen järjestys on
> `REMEDIATION_ROADMAP.md`; uusi GUI noudattaa repositoryn `GUI_SPEC.md`:ää ja
> `GUI_DEVELOPMENT_PLAN.md`:ää. Erityisesti BL-012–015 ja niiden responsive/
> Advanced-oletukset ovat korvattuja.

## Julkaisuportti

Uutta release-artefaktia ei julkaista ennen BL-001/CQ-AUD-001:n hyväksyntää. BL-002
sekä pääarvioinnin muut P1-kokonaisuudet kuuluvat seuraavaan vakaaksi nimettyyn
versioon. Yksityiskohtaiset portit ovat `REMEDIATION_ROADMAP.md`:ssä.

## Vaihe 0 — P0, estää julkaisun

### BL-001 — Hardware-output-indeksien globaali validointi

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P0 / välitön |
| Löydös | CQ-AUD-001 |
| Muutoskokonaisuus | Hylkää outChans- ja stemOuts-duplikaatit sekä main/stem-päällekkäisyys. Jos mono halutaan, lisää eksplisiittinen ennen yhtä safety-ketjua tehtävä foldaus, ei duplikaatti-Out-kutsuja. |
| Hyväksymiskriteeri | CQConfig hylkää [0,0], stem [2,2] ja main [0,1] + stem [1,2]. Erillinen 4-kanavakonfiguraatio hyväksytään. Offline-testi mittaa todellisen hardware-bus-kohtaisen loppusumman; mikään hyväksytty konfiguraatio ei ylitä nykyistä 0,89:n kattoa. |
| Riippuvuudet | Ei teknistä riippuvuutta. Päätä mono-migraation käyttäjäviesti. |
| Regressioriski | Keskitaso konfiguraatioyhteensopivuudelle, matala DSP:lle. |

### BL-002 — Recorderin atominen lifecycle ja tiedosto-omistus

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P0 / välitön |
| Löydös | CQ-AUD-002 |
| Muutoskokonaisuus | idle/starting/recording/stopping-tilakone; operaatiotoken; paikalliset resurssiviitteet; tiedoston atominen varaus; cancel/rollback; idempotentti GUI- ja näppäintoggle. |
| Hyväksymiskriteeri | Kymmenen nopeaa start-kutsua luo enintään yhden DiskOut-noden ja yhden tiedoston. Start+välitön stop ei jätä myöhemmin käynnistyvää nodea. Stop/free sulkee tiedoston, vapauttaa bufferin ja poistaa graph-rekisteröinnit. Sama läpäisee 2ch- ja recStems 4ch -tiloissa, eikä olemassa olevaa tiedostoa ylikirjoiteta. |
| Riippuvuudet | Hyötyy BL-004:n yhteisestä session/lifecycle-mallista, mutta paikallinen tilakone voidaan tehdä ensin. |
| Regressioriski | Korkea async-teardownille ja tallenneformaatille; käytä vain testihakemistoa hyväksynnässä. |

## Vaihe 1 — P1, runtime- ja DSP-eheys

### BL-003 — Erota graph restart ja todellinen server reboot

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P1 |
| Löydös | CQ-AUD-003 |
| Muutoskokonaisuus | Tunnista boot-only ServerOptions-muutokset, tee hallittu Server.reboot tai vaadi käyttäjän päätös. Dokumentoi jaetun Server.defaultin omistus. |
| Hyväksymiskriteeri | 44100 → 48000 -muutos näkyy serverin toteutuneena sample ratena. Block size, device ja hardware output count varmennetaan vastaavasti. Graph-only restart ei turhaan sammuta serveriä. |
| Riippuvuudet | BL-004:n awaitable teardown ja generation-token. |
| Regressioriski | Korkea muille samassa scsynthissä oleville töille. |

### BL-004 — Yhteinen session generation, lifecycle-tila ja rollback

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P1 |
| Löydökset | CQ-AUD-004, CQ-AUD-018, CQ-AUD-019 |
| Muutoskokonaisuus | booting/ready/stopping/stopped/failed-tila; session generation -token; awaitable idempotentti teardown; osittaisen buildin protect/rollback; ServerQuit-watcher; GUI:n pending/deferred-init samaan sessioniin sidottuna. |
| Hyväksymiskriteeri | boot+free, boot+boot, GUI+free ja kaksi GUI-kutsua eivät jätä nodeja, buffereita, OSCdefejä, controllereita, taskia tai kaksoisikkunaa. Tahallinen poikkeus jokaisessa boot-vaiheessa rollbackaa resurssit. Ulkoinen s.quit merkitsee session kuolleeksi ja seuraava boot onnistuu. |
| Riippuvuudet | Määrittelee BL-003-, BL-005- ja BL-002-operaatioiden session-omistuksen. |
| Regressioriski | Korkea; CmdPeriod, panic, GUI close, recorder ja deckit on testattava yhtenä kokonaisuutena. |

### BL-005 — Deck-loadin last-request-wins ja peruutus

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P1 |
| Löydös | CQ-AUD-005 |
| Muutoskokonaisuus | Request-generation, peruutettava task/timeout, paikallisesti omistettu bufferi, stale-session-validointi ja aina vapauttava error-polku. |
| Hyväksymiskriteeri | Suuri → pieni lataus jättää aina pienen aktiiviseksi. Load → free ei lisää pysyviä server-buffereita tai nodeja. Epäonnistunut/timeout-lataus säilyttää vanhan toimivan deckin. next/prev/random ja crossfade läpäisevät regressiot. |
| Riippuvuudet | BL-004:n session-token. |
| Regressioriski | Korkea bufferien ja crossfaden omistukselle. |

### BL-006 — Sanitointi tilallisten DSP-silmukoiden kirjoitusrajoille

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P1 |
| Löydös | CQ-AUD-006 |
| Muutoskokonaisuus | Finite-value-sanitointi ennen Cocon BufWr-, Quantussyn LocalOut- ja Spacen LocalOut-kirjoituksia; rate-limitattu diagnoosi; nykyinen lopun fail-closed-squelch säilyy. |
| Hyväksymiskriteeri | Yhden blokin NaN ja inf kussakin silmukassa eivät saavuta ulostuloa, eivät jää RAMiin/feedbackiin eivätkä estä äärelliseen signaaliin palautumista. Diagnostiikka ilmoittaa kerran eikä tulvi. Normaalin äärellisen NRT:n renderi ja CPU eivät muutu sovittua toleranssia enempää. |
| Riippuvuudet | Uusi kohdennettu NRT-harness BL-007:ssä. |
| Regressioriski | Keskitaso CPU:lle, korkea jos normaalipolun soundi muuttuu. |

## Vaihe 2 — Testiperusta ja semanttiset korjaukset

### BL-007 — Hermettinen ja kerrostettu testimatriisi

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2, aloita ennen laajoja muutoksia |
| Löydökset | CQ-AUD-016, CQ-AUD-017, CQ-AUD-020 |
| Muutoskokonaisuus | excludeDefaultPaths-pohjainen vanilla-class-library; serveritön state/route/GUI-smoke; live facade-lifecycle-suite; SR/BS-parametrisoitu NRT; resurssimäärien laskenta. |
| Hyväksymiskriteeri | Compile/unit eivät lataa käyttäjäextensioneita. Kaikki 79 parametria tarkistetaan CQSpec → CQState → route -ketjussa; 71 GUI-kontrollia ja 8 tietoisesti API-only-parametria inventoidaan. NRT läpäisee 44.1/48/96 kHz:n ja sovitut block sizet. Kymmenen boot/free-kierrosta ei kasvata resursseja. |
| Riippuvuudet | Display-required Qt-smoke voidaan pitää erillisessä jobissa. |
| Regressioriski | Testiaika ja Qt-flakiness; erottele unit, NRT ja live/display-jobs. |

### BL-008 — Määrittele preset vs overlay ja oikea reset

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2 |
| Löydös | CQ-AUD-007 |
| Muutoskokonaisuus | Valitse deterministiset sound-presetit tai eksplisiittiset overlay-reseptit. Lisää rajattu baseline/reset ja päivitä valikko/dokumentaatio. |
| Hyväksymiskriteeri | Jos deterministinen: fresh → classic ja mikä tahansa preset → classic tuottavat saman sovitun parametri- ja matrix-tilan. Empty tuottaa dokumentoidun puhtaan pöydän. Jos overlay: nimeä se näkyvästi, ja erillinen Reset tuottaa saman deterministic baselinen. |
| Riippuvuudet | Päätä mitä ei koskaan resetata: masterVol, monitorMute, micGain, device/config, ladatut deckit ja mahdollisesti recorder. |
| Regressioriski | Korkea sound- ja performance-workflow’lle. |

### BL-009 — Linked stereo yhtenä operaationa ja stereona dry-polussa

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2 |
| Löydökset | CQ-AUD-008, CQ-AUD-009 |
| Muutoskokonaisuus | Yhteinen stereo-load-barrier, yhteinen aikaleimattu synth-start ja L/R-sijoittelun säilyttävä dry-route. Erota itsenäinen dual-mono-tila. |
| Hyväksymiskriteeri | Samanaikaisen L/R-impulssin ensimmäisen ei-nollanäytteen ero on 0. L-only dry ei näy R-kanavassa eikä R-only L-kanavassa linked-tilassa. Mono-deckit säilyttävät dokumentoidun toimintansa. |
| Riippuvuudet | BL-005:n bufferi- ja request-omistus. |
| Regressioriski | Korkea deck-workflow’lle ja nykyisille keskitettyä dual-monoa hyödyntäville patcheille. |

### BL-010 — Castle-offsetien smoothing

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2 |
| Löydös | CQ-AUD-010 |
| Muutoskokonaisuus | Kytke c1Off–c5Off CQSpec.lag-arvoon ja lisää kontrollikäyrätesti. |
| Hyväksymiskriteeri | API-set ja snapshot-morph eivät tuota portaita suurempina kuin sovittu toleranssi. A/B-kuuntelu hyväksyy Quantussy-tuntuman eikä fixed-spread-default muutu. |
| Riippuvuudet | LISTENING_TEST_PROTOCOL.md:n Quantussy/morph-osuus. |
| Regressioriski | Keskitaso/korkea sound-identiteetille. |

### BL-011 — Turvallinen snapshot-dataformaatti tai eksplisiittinen trust boundary

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2 |
| Löydös | CQ-AUD-022 |
| Muutoskokonaisuus | Korvaa path.load ei-evaluoivalla parserilla tai lisää selkeä luottamusmalli ja vahvistus ulkopuolisille tiedostoille. |
| Hyväksymiskriteeri | Snapshotin data ei voi suorittaa post-, tiedosto-, verkko- tai shell-sivuvaikutusta ennen validointia. Nykyisille snapshot-versioille on testattu import/migraatio ja rakenteellinen validointi. |
| Riippuvuudet | Formaatti- ja yhteensopivuuspäätös. |
| Regressioriski | Korkea vanhoille snapshot-tiedostoille ja käsin muokattavuudelle. |

## Vaihe 3 — GUI:n semantiikka ennen visuaalista viimeistelyä

### BL-012 — GUI näyttää todellisen mic-, recorder- ja trim-tilan

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2 |
| Löydökset | CQ-AUD-002, CQ-AUD-011, CQ-AUD-012 |
| Muutoskokonaisuus | Runtime-oliot julkaisevat toteutuvan mic/recorder/trim-tilan uuden persistent Master-stripin ja Dayn käyttöön; näkyvä muoto määräytyy vain GUI_SPEC.md:n mukaan. |
| Hyväksymiskriteeri | GUI ei omista rinnakkaista recorder-/trim-booleania. GUI_SPEC §16.8:sta puuttuvat REC-välitilat ja §16.6:n mic-root-sauma hyväksytetään ennen uuden lookin lisäämistä. |
| Riippuvuudet | BL-002:n recorder-state on lähde; älä kopioi tilaa GUI:hin erilliseksi totuudeksi. |
| Regressioriski | Matala DSP:lle, keskitaso shortcut-/toggle-semanttiikalle. |

### BL-013 — Presentation-metadata, yksiköt, termit ja unity-merkit

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2 |
| Löydös | CQ-AUD-013 |
| Muutoskokonaisuus | Korvattu GUI_SPEC §16.4/§16.7:n type- ja widget-kielellä. Day säilyttää v1.0-ohjaimet; Nightissa ei näytetä numeroita. |
| Hyväksymiskriteeri | Labelit, fontit, koot ja sallitut night-tekstit vastaavat GUI_SPECiä; Inspector on täsmällinen numeropolku. |
| Riippuvuudet | BL-007:n inventaariotesti ja yhteinen Ink kit. |
| Regressioriski | Älä rakenna yleistä metadata-/tooltip-järjestelmää, jota GUI_SPEC ei pyydä. |

### BL-014 — Space/Shake-rajaus Archipelagossa

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2 |
| Löydökset | CQ-AUD-014 ja FEATURE_TRACEABILITY.md:n 8 API-only-parametria |
| Muutoskokonaisuus | GUI_SPEC määrää Shaken Night-toolbarille ja Weather-law'n alle. Advanced-paneelia ei tehdä; Space pysyy Day/Masterin minimaalisena ohjauksena eikä saa Night-bodya. |
| Hyväksymiskriteeri | Shake säilyttää SPEC §5.5:n weights+spesal/no-CQSpec-param -käytöksen; weather-työkalut ovat weights-only; Spaceen ei lisätä uutta pintaa ilman GUI_SPEC-muutosta. |
| Riippuvuudet | COCOQUANTUS_FIDELITY.md:n Space-päätös vain mahdollista myöhempää spec-muutosta varten. |
| Regressioriski | Matala vihjeille, korkea jos oletuspatcheja tai soundia muutetaan. |

### BL-015 — Archipelago-layout, fokuslaki ja GUI-lifecycle-smoke

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2 |
| Löydökset | CQ-AUD-015, CQ-AUD-019, CQ-AUD-020 |
| Muutoskokonaisuus | Yksi Day/Night + persistent Master + Drawer -ikkuna, minimi 1280×720, nykyinen non-focusable-widget-laki, Tab/Esc/Ctrl+Z ja display-required smoke. |
| Hyväksymiskriteeri | GUI_SPEC §2/§10/§16.10 täyttyy; Master ei peity; hard-cut Tab toimii vain ilman text-focusa; kaikki performance-shortcutit säilyvät; open/close/reopen/free ei jätä listener/task/OSCdef-resursseja. |
| Riippuvuudet | BL-004 GUI-lifecycle ja BL-007 testikerrokset. |
| Regressioriski | Korkea nykyiselle fokusratkaisulle ja audition-cleanupille; älä lisää keyboard-navigation-tilaa tai breakpoint-rakennetta ilman spec-muutosta. |

## Vaihe 4 — Dokumentaatio, fidelity-päätökset ja viimeistely

### BL-016 — Yksi kanoninen status ja turvallisuusvastuun korjaus

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2 |
| Löydökset | CQ-AUD-021, CQ-AUD-026 |
| Muutoskokonaisuus | Yksi release/status-lähde; watchdog vain comfort/notice; arkkitehtuuridokumentti toteutuksen tasalle; live-stem-status eksplisiittiseksi; lähdebibliografia fidelity-väitteille. |
| Hyväksymiskriteeri | README, SPEC, PLAN ja manual checklist eivät anna ristiriitaista v1.0/KUUNTELU 7- tai watchdog-vastuuta. MIDI-hotplug, deck-placeholder ja recovery on kuvattu toteutuksen mukaisesti. Jokaisella historiallisella väitteellä on lähde tai tulkintamerkintä. |
| Riippuvuudet | Todellinen hyväksyntä LISTENING_TEST_PROTOCOL.md:n jälkeen. |
| Regressioriski | Ei runtime-riskiä; historiallinen tieto pitää säilyttää changelogina. |

### BL-017 — Space-konseptipäätös

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P2/P3 |
| Löydös | COCOQUANTUS_FIDELITY.md:n Space-arvio |
| Muutoskokonaisuus | Valitse: nykyinen rehellisesti nimetty FDN/Plate; kontekstuaalinen Coco-makro; tai ydininstrumentin ulkopuolinen efekti. |
| Hyväksymiskriteeri | Päätös on dokumentoitu, default on edelleen off, feedback ≤ 0.98 ja nykyinen output safety säilyy. Kuunteluraati arvioi, peittääkö efekti RAM/aliasing/Quantussy-rakennetta. |
| Riippuvuudet | LISTENING_TEST_PROTOCOL.md:n Space A/B. |
| Regressioriski | Korkea instrumentin identiteetille, jos DSP muuttuu; matala pelkälle nimeämiselle. |

### BL-018 — Kvantisoijan täsmällinen määritelmä

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P3 |
| Löydös | CQ-AUD-023 |
| Muutoskokonaisuus | Määrittele signed n-bit -tasot, lisää ramp-testi ja valitse yhteensopivuuspolitiikka. |
| Hyväksymiskriteeri | Testi osoittaa täsmälleen dokumentoidun määrän tasoja. Ennen/jälkeen-renderit sekä feedback/hold-kuuntelu on hyväksytty. |
| Riippuvuudet | Kuuntelukalibrointi; ei ennen P0/P1-työtä. |
| Regressioriski | Korkea sound-identiteetille. |

### BL-019 — Matriisin legenda ja kaksitasoinen värisemantiikka

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P3 |
| Löydös | CQ-AUD-025 |
| Muutoskokonaisuus | Polariteetti glyphillä/kirkkaudella, lähdeperhe reunuksella/ryhmäotsikolla, näkyvä legenda. |
| Hyväksymiskriteeri | Käyttäjä erottaa ilman speksiä positiivisen/negatiivisen painon ja triangle/castle/FM-perheen. Nykyinen banana-paletti ja kontrasti säilyvät. |
| Riippuvuudet | BL-015:n responsiivinen matriisi. |
| Regressioriski | Matala teknisesti, keskitaso visuaaliselle jatkuvuudelle. |

### BL-020 — Vastuiden asteittainen tiedostojako

| Kenttä | Arvo |
|---|---|
| Prioriteetti | P3 / viimeinen |
| Löydös | CQ-AUD-024 |
| Muutoskokonaisuus | Erota facade-lifecycle/OSC/pattern-vastuita ja jaa SynthDef-alijärjestelmiä muuttamatta julkisia nimiä tai järjestystä. |
| Hyväksymiskriteeri | Julkinen CQ-API, SynthDef-nimet, group-järjestys, compile, nykyiset 3082 hyväksyttyä unit-assertiota, NRT-kynnykset ja kuuntelureferenssit säilyvät. |
| Riippuvuudet | BL-007:n vahva regressiopohja; tee vasta toiminnallisten korjausten jälkeen. |
| Regressioriski | Keskitaso/korkea SuperColliderin class-library- ja SynthDef-latausjärjestykselle. |

## Jokaisen muutospaketin Definition of Done

- Löydöksen alkuperäinen reproduktio epäonnistuu ennen korjausta ja läpäisee sen jälkeen.
- Uusi testi kuuluu oikeaan kerrokseen: serveritön unit/route, NRT, live-server tai display/hardware.
- Nykyiset compile-, 3082 unit-assertion-, safety- ja hold-testit läpäisevät vähintään samoilla rajoilla; kynnystä ei löysennetä korjauksen vuoksi.
- Resurssimuutos tarkistaa nodet, groupit, bufferit, OSCdefit, controllerit, taskit ja avoimet tiedostot.
- Audioidentiteettiin vaikuttava muutos sisältää ennen/jälkeen-renderin ja LISTENING_TEST_PROTOCOL.md:n soveltuvan hyväksynnän.
- README, SPEC, ARCHITECTURE, PLAN ja manual checklist päivitetään vain, jos niiden normatiivinen väite muuttuu.
- Git-diffissä ei ole muutokseen liittymättömiä käyttäjän tiedostoja.

## Invariantit, joita backlog ei saa rikkoa

- BufRd luodaan ja ajetaan ennen BufWr:ää.
- Hold-identiteetti ja A4/A5-regressiot säilyvät.
- clip/wrap → quantize → RAM -järjestys säilyy, ellei erikseen kuunneltu fidelity-päätös muuta sitä.
- Cocon kaksi bufferia ja synthiä pysyvät erillisinä.
- sources → quantussy → cocos → fx → master -group-järjestys säilyy.
- Tarkoitukselliset LocalIn/InFeedback-yhden blokin viiveet säilyvät, kunnes SR/BS-kuuntelu osoittaa muutostarpeen.
- State- ja matrix-clamp-funnelit säilyvät ainoina kirjoitusreitteinä.
- Boot-fade, panic/mute, bounded Coco-loop, master/stem safety, Space default-off ja Space-feedback-katto säilyvät.
