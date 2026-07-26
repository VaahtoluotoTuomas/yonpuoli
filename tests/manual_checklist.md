# Manuaalinen kuuntelulista — pysyvät SPEC M1–M8 -testit + Nocturne-vaihe

Ihmiskorvatestit — Claude ei kuule ääntä, joten nämä tarkistaa aina käyttäjä.
Tämä on **elävä regressiolista**: SPEC:n M1–M8-kuuntelutestit, jotka ajetaan
aina DSP-muutoksen jälkeen (niitä ei voi automatisoida — ei ääntä), sekä kunkin
uuden vaiheen omat lohkot lisätään loppuun. Aiempien vaiheiden (v1.0,
Archipelago G1–G3) hyväksytyt välitarkastuslohkot on jäädytetty tiedostoon
[`docs/history/manual_checklist_archipelago.md`](../docs/history/manual_checklist_archipelago.md).

**Ennen jokaista kierrosta:** jos `classes/` on muuttunut, IDE:ssä
Ctrl+Shift+L (class-lib recompile), sitten `CQ.boot` (bootin pitää olla
täysin hiljainen, sitten 2 s fade-in).

**Muotoilu:** jokainen testi(osa) on yksi SC-koodilohko, jonka voi kopioida
sellaisenaan IDE:hen — ohjeet ovat `//`-kommentteina. Rivit ajetaan yksi
kerrallaan (Ctrl+Enter), kuunnellen välissä; älä evaluoi koko lohkoa kerralla.

Hyödyllinen lähtötila kuunteluun:

```supercollider
// Lähtötila useimpiin testeihin. Huom: absoluuttisissa Windows-poluissa
// käytä kauttaviivoja ("C:/Users/...") — kenoviiva on sclangin
// merkkijonoissa escape-merkki ("C:\tuoma\..." hajoaa, \t = sarkain).
CQ.boot;
CQ.deckA.load("polku/aanite.wav");    // suhteellinen polku etsitään samples/-kansiosta
                                      // -> TARKISTA post-ikkunasta "CQDeck deckA: loaded ..."
CQ.set(\masterDry, 0.3);              // kuulet myös lähteen
CQ.set(\coco2Level, 0);               // hiljennä coco2:n power-on-kohinalooppi
```

---

## M1 — Speed-repitch (vaihe 2)

```supercollider
// M1 — Speed-repitch: held loopin uudelleenvirityksen pitää olla
// nauhamainen ja jatkuva. (Lähtötila: deck A soi, ks. yllä.)
CQ.hold(1, 0);                 // 1. deck A -materiaali alkaa kiertää coco1:een (delay-tila)
CQ.hold(1, 1);                 // 2. muutaman sekunnin päästä: looppi lukittuu
CQ.set(\coco1Speed, 12000);    // 3. pitch putoaa oktaavin, sointi tummuu ja rouhentuu (ZOH-alias)
CQ.set(\coco1Speed, 48000);    // 4. kirkas, nopea, loopin kesto lyhenee
// 5. Liu'uta arvoja edestakaisin: repitchin pitää olla portaaton (tape/doppler),
//    eikä loopin sisältö saa rapistua, vaikka sweeppaat minuutin.
CQ.set(\coco1Speed, 24000);    // lopuksi: takaisin oletukseen
```

- [x] M1 hyväksytty (pvm: 2026-07-16)

## M2 — Feedback-ride (vaihe 2)

```supercollider
// M2 — Feedback-ride: tason on pysyttävä vakaana kaikilla feedback-arvoilla
// (safety-invariantti). (Lähtötila: deck A soi.)
CQ.hold(1, 0);                 // 1. delay-tila, deck A soi
CQ.set(\coco1Fb, 0.9);         // 2. kaikumaisia toistoja, jotka hiipuvat
CQ.set(\coco1Fb, 1.0);         // 3. looper: toistot eivät hiivu eivätkä kasva
CQ.set(\coco1Fb, 1.15);        // 4. eskaloituva, saturoituva crunch — mutta äänenvoimakkuus
                               //    EI kasva hallitsemattomasti (kvantisointiklippi rajaa)
// 5. Aja fb hitaasti 0 -> 1.25 -> 0: ei koskaan tasohyppyä eikä kipeää piikkiä.
//    Huom: fb-pudotuksen jälkeen buffer tyhjenee portaittain (askel = yksi
//    loop-kierto) ja häntä katkeaa bittilattiaan — tämä on autenttista.
CQ.set(\coco1Fb, 0.35);        // lopuksi: takaisin oletukseen
```

- [x] M2 hyväksytty (pvm: 2026-07-16)

## M5 (osa) — Flip, skip, dolby (vaihe 2)

```supercollider
// M5 (osa) — Flip, skip, dolby: duck-artikulaatio ja suunnanvaihto;
// dolby-osuus täydentyy GUI:sta vaiheessa 6.
CQ.hold(1, 1);                 // 1. pidä looppi holdissa
CQ.flip(1);                    // 2. toisto kääntyy takaperin kuultavasti...
CQ.flip(1);                    //    ...ja uusi flip palauttaa suunnan
CQ.skip(1);                    // 3. looppi hyppää alkuunsa (fraasi "resetoituu")
CQ.hold(1, 0);                 // 4. delay-tilaan; syötä puhetta/rytmiä mikistä
CQ.set(\preADrive, 6);         //    (puhe mikistä erottuu tällä driveltä)
CQ.set(\coco1DolbyFb, \duck);  //    uusi input "lyö" vanhat kerrokset hiljaisemmiksi
                               //    — monofoninen time-layer-artikulaatio
// Vinkki: duck kuuluu vain materiaalilla, jossa on taukoja (puhe) — tasainen
// tekstuuri pitää key-envelopen ylhäällä jatkuvasti eikä artikulaatiota synny.
CQ.set(\coco1DolbyFb, \off);   // 5. kerrokset summautuvat taas vapaasti
CQ.set(\preADrive, 2);         // lopuksi: drive takaisin oletukseen
```

- [x] M5/vaihe-2-osa hyväksytty (pvm: 2026-07-16)

## Vaihe 2 -ekstra — Digital dust

```supercollider
// Digital dust: itsekohina kertyy loopiksi ilman inputtia.
CQ.set(\coco1SlotDeck, 0);     // 1. ei inputtia...
CQ.set(\coco1SlotPre, 0);      //    ...kummastakaan slotista
CQ.hold(1, 0);                 // 2. record-tila...
CQ.set(\coco1Fb, 1.1);         //    ...feedback yli unityn...
CQ.set(\coco1Dust, 0.3);       //    ...dust ylös
// 3. Kuuntele ~30 s: rakeinen kohinapohja kertyy loopiksi — purskeittain,
//    ei tasaisesti (LFNoise2-moduloitu dust + 8-bit-bittilattia, by design).
CQ.hold(1, 1);                 // 4. dust-tekstuuri jää kiertämään...
CQ.set(\coco1Speed, 12000);    //    ...ja coco1Speed virittää sitä
// Lopuksi: palauta oletukset
CQ.set(\coco1Dust, 0.02); CQ.set(\coco1Fb, 0.35); CQ.set(\coco1Speed, 24000);
CQ.set(\coco1SlotDeck, 1); CQ.set(\coco1SlotPre, 1);
```

- [x] Dust-workflow hyväksytty (pvm: 2026-07-16)

---

## M3 — Quantussy-tuntuma (vaihe 3)

```supercollider
// M3 — Alkuasetelma: Quantussy kuuluviin coco1:n läpi (castle 5 on
// boot-oletuksena audio-alueella, c5Send 0.5).
CQ.boot;                       // odota 2 s fade-in
CQ.set(\coco2Level, 0);        // coco2:n power-on-kohinalooppi pois
CQ.set(\coco1SlotDeck, 0);
CQ.set(\coco1SlotPre, 0);
CQ.set(\coco1SlotQ, 1);        // Quantussy-mix coco1:n input-mikseriin
CQ.hold(1, 0);                 // record-tila: qMix pääsee looppiin
// -> Castle 5:n kolmioaalto (~140 Hz) kuuluu coco1:n läpi.
```

```supercollider
// M3 testit (aja rivi kerrallaan, kuuntele välissä):

// 1. Chaos-sweep (qFreq oletuksessa 4):
CQ.set(\qChaos, 0);            // täysin vakaa sävel
CQ.set(\qChaos, 0.1);          // vibratomainen huojunta
CQ.set(\qChaos, 0.4);          // askeltava satunnaiskulku (S&H-melodia)
CQ.set(\qChaos, 1);            // hyperkaaos: purskeita, pillejä, hiljaisuuksia

// 2. qFreq-sweep (qChaos ~0.3): kaikkien viiden castlen pitää skaalautua
//    yhdessä, LFO<->audio-rajan ylitykset portaattomia (ei hyppyjä, ei klikkejä):
CQ.set(\qChaos, 0.3);
CQ.set(\qFreq, 0.5);
CQ.set(\qFreq, 4);
CQ.set(\qFreq, 50);
CQ.set(\qFreq, 500);
CQ.set(\qFreq, 4);             // takaisin oletukseen

// 3. Range-kytkimet:
CQ.set(\c1Range, \audio); CQ.set(\c1Send, 0.5);   // toinen ääni sisään
CQ.set(\c1Range, \balcium);                       // putoaa keskialueelle
CQ.set(\c1Range, \infra); CQ.set(\c1Send, 0);     // takaisin LFO:ksi

// 4. Spesal (qChaos > 0, jotta verkko elää):
CQ.spesal(5);                  // toistuvasti: partner/mode vaihtuu, tekstuuri
                               // muuttuu kuultavasti joka askeleella
CQ.spesal(3);                  // FM-lähteen vaihto näkyy verkon käytöksessä
CQ.spesalSet(5, 0);            // palauttaa tunnettuun tilaan

// 5. qShLag (qChaos 0.4):
CQ.set(\qChaos, 0.4);
CQ.set(\qShLag, 0.08);         // askeleet liukuvat, verkko kesyyntyy
CQ.set(\qShLag, 0.004);        // takaisin: autenttiset kovat askeleet
```

- [x] M3 hyväksytty (pvm: 2026-07-16)

## M4 — \selfNoise-preset (vaihe 6)

```supercollider
// M4 — \selfNoise: no-input digital-dust -workflow yhtenä eleenä
// (vaiheen 2 dust-resepti presetiksi paketoituna + VCA-wobble).
CQ.boot;                       // (tai jatka sessiota)
CQ.patchPreset(\selfNoise);    // sulkee pre/deck-slotit, hold 0, fb 1.1,
                               // dust ylös, slotX-ristisyöttö 0.3,
                               // tri->fbVca-wobble + cas->inVca-portit + spaf-drift
// 1. Kuuntele ~60 s ilman inputtia: rakeinen dust-pohja kertyy molempiin
//    looppeihin PURSKEINA (ei tasaisesti); feedback-wobble hengittää
//    tiheyttä hitaasti, cas-askeleet avaavat/sulkevat ristisyöttöä.
//    Loopit eri kelloilla (24k/18k) - pari ei lukitu samaan fraasiin.
CQ.set(\qChaos, 0.5);          // 2. villimmät askeleet wobble/gate-lähteisiin
CQ.hold(1); CQ.hold(2);        // 3. lukitse tekstuuri looppipariksi...
CQ.set(\coco1Speed, 12000);    //    ...ja viritä kuten mitä tahansa looppia
// 4. Palautus: CQ.patchPreset(\empty); CQ.hold(1, 0); CQ.hold(2, 0);
//    (tai jatka soittoa tästä)
```

- [x] M4 hyväksytty (pvm: 2026-07-18)

## M6 — \classic kenttä-äänitteellä + matriisi (vaihe 4)

```supercollider
// M6 — Alkuasetelma. LÄPPÄRIHUOMIO (mikki): sisäinen mikki ON \liveL
// (WASAPI-sisääntulolaite = mikkiryhmä, inChans[0]). ÄLÄ vaihda \preASrc-
// arvoon \mic — configissa on micChan: nil, joten \mic-lähde on hiljaisuus
// (SPEC §7: mic-polku pois; micGain-oletus on joka tapauksessa 0). Preamp A
// kuulee mikin oletusasetuksella \liveL, tasona liveGain (oletus 1).
CQ.boot;                       // odota 2 s fade-in
CQ.deckA.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
// ^ TARKISTA post-ikkunasta rivi "CQDeck deckA: loaded ..." — jos siellä
//   lukee "file not found", deck EI soi ja kuulet pelkkää kohinaa!
CQ.set(\coco2Level, 0);        // hiljennä coco2:n power-on-kohinalooppi
CQ.set(\coco1SlotPre, 0);      // LÄPPÄRILLÄ: mikin huonekohina pois coco1:stä;
                               // pöytäkoneella modulaari-inputilla jätä oletukseen
CQ.patchPreset(\classic);      // banaanit JA nupit: tri1+tri3 -> spaf (syvyys 0.12),
                               // castle->castle FM-rinki
CQ.hold(1, 0);                 // coco1 record-tilaan
```

```supercollider
// M6 testit:

// 1. Kuuntele oletusarvoilla, koskematta mihinkään muuhun: pitäisi kuulostaa
//    heti "Cocoquantus-maiselta" (SPEC M6). Mitä odottaa:
//    - Huojunta: hidas ±50 sentin vire-ajelehdinta (~6–20 s aaltoina, tri1;
//      keston vaihtelu tulee chaosista + FM-ringistä) + ~3 Hz ±17 sentin
//      flutter (tri3) — "kulunut mutta toimiva nauhuri", elävää, EI
//      merisairasta. Kuuluu selvimmin sävelkorkeudellisesta/perkussiivisesta
//      materiaalista; kohinamainen materiaali ei paljasta viredriftiä.
//    - Aliasointi + 8-bit-rae materiaalin päällä koko ajan.
//    - Ensimmäiset ~15–20 s: power-on-kohina kiertää pohjalla ja hiipuu
//      portaittain pois (fb 0.35, askel = loop-kierto ~5.5 s) — normaalia.
//    - Dust-purskeet (oletus 0.02) rapisevat satunnaisina aaltoina pohjalla
//      eivätkä lopu koskaan kokonaan — by design, hardwaren "digital dust".
CQ.set(\coco1Dust, 0);         //    (A/B-varmistus: aaltojen pitää loppua)
CQ.set(\coco1Dust, 0.02);      //    (takaisin oletukseen)

// 2. Inverso-banaani coco1:een (coco2 on mykistetty). Kaksi huomiota:
//    - Omaa ääntäsi EI kuulu: slotPre 0 pitää puheen poissa loopista,
//      se on pelkkä ohjaussignaali (envA).
//    - fbVca istuu kierrätyksen KIRJOITUSPOLUSSA (ennen RAMia), joten duck
//      kuuluu vasta kun looppi kiertää takaisin (~5.5 s oletusnopeudella) —
//      välitöntä reaktiota ei ole, sama kuin hardwaressa.
CQ.set(\preADrive, 6);         // envA riittävän vahvaksi puheella (kuten M5:ssä)

// 2a. Välitön todistus envA-reitityksestä — spaf vaikuttaa kelloon HETI
//     (koko coco1:n ääneen: ensikierto + toistot re-pitchaavat yhdessä).
//     Irrota ensin classicin tri-banaanit, jotta VAIN puhe liikuttaa kelloa
//     (muuten nuppi täysillä syventää myös presetin oman huojunnan):
CQ.unpatch(\tri1, \coco1_spaf);  CQ.unpatch(\tri3, \coco1_spaf);
CQ.set(\coco1Spaf, 1);         // syvyysnuppi täysille
CQ.patch(\envA, \coco1_spaf, -1);
// -> ilman puhetta pitchin pitää pysyä vakaana; kun puhut, loopin pitch
//    sukeltaa HETI (jopa -1 okt, inverso) ja palaa tauoilla.
CQ.unpatch(\envA, \coco1_spaf);
CQ.patchPreset(\classic);      // palauttaa tri-banaanit JA nupin (0.12)

// 2b. Varsinainen fbVca-duck — kuuluu VAIN toistokerroksissa (feedback-
//     haara; samplen ensikierto kulkee inVca:n kautta eikä muutu), ja
//     vasta loop-kierron viiveellä:
CQ.set(\coco1Fb, 1.05);        // kerrostuva looper: kerrokset jäävät soimaan
CQ.patch(\envA, \coco1_fbVca, -1);
// -> puhu YHTÄJAKSOISESTI 5-10 s (koko kierto): kerrostuma ohenee selvästi
//    seuraavilla kierroksilla; ole hiljaa ~15 s -> kasautuu takaisin.
//    Lyhyet tokaisut tekevät vain ~5.5 s myöhässä kuuluvia ohuita kohtia.
CQ.unpatch(\envA, \coco1_fbVca);   // lopuksi
CQ.set(\coco1Fb, 0.35); CQ.set(\preADrive, 2);

// 3. patchDepth-makro:
CQ.set(\patchDepth, 0);        // koko matriisin vaikutus katoaa pehmeästi
                               // (50 ms lag), ei klikkiä
CQ.set(\patchDepth, 1);        // banaanit palaavat täyteen voimaan, yhtä pehmeästi

// 4. Kaikki banaanit irti -> takaisin puhtaaseen coco-ääneen:
CQ.clearPatch;

// 5. Vapaa ~20 min koodi-only-sessio (ei GUI:ta vielä) — kokeile omia
//    patcheja, kirjoita vain patch-rivejä ja tunnettuja CQ.set-kutsuja:
CQ.patch(\cas3, \coco1_spaf, 0.7);      // castle 3 moduloi coco1:n kelloa
CQ.set(\coco2Level, 0.8);               // coco2 takaisin kuuluviin...
CQ.patch(\coco1, \coco2_fbVca, 0.4);    // ...banaani-pari = kahden cocon
CQ.patch(\coco2, \coco1_fbVca, 0.4);    //    ristikaiun keskinäinen ducking
```

- [x] M6 hyväksytty (pvm: 2026-07-17)

## M7 — 30 min abuse-sessio (vaihe 7)

```supercollider
// M7 — 30 min tahallista väärinkäyttöä kuulokkeilla: MIKÄÄN ele, patch,
// preset tai modulaatiotila ei saa koskaan tuottaa kipeää tasoa (SPEC M7/§8).
// Surprising sound yes, surprising loudness no.
CQ.boot; CQ.patchPreset(\mayhem);
CQ.set(\coco1Fb, 1.25); CQ.set(\coco2Fb, 1.25);
CQ.set(\preADrive, 40); CQ.set(\preBDrive, 40);
CQ.set(\coco1Wrap, 1); CQ.set(\coco2Wrap, 1);      // evil mode
CQ.set(\coco1Dust, 0.5); CQ.set(\coco2Dust, 0.5);
CQ.set(\qChaos, 1); CQ.set(\qFreq, 500);
// Ratsasta kaikkia: shake, flip, skip, spesal, speed-sweepit, patchDepth.
// Master saa soida kovaa, mutta limitteri + rajattu kierrätys pitävät tason
// aina hallussa. `space` (panic) mykistää heti; `CQ.resume` palauttaa.
CQ.patchPreset(\empty);
```

- [x] M7 hyväksytty (osana KUUNTELU 7:ää, arkistoitu — v1.0-hyväksyntä 2026-07-19)

## M8 — 16n-faderit (vaihe 5)

```supercollider
// M8 — Alkuasetelma. Kytke 16n USB:hen ENNEN boottia.
// (classes/ muuttui vaiheessa 5 -> Ctrl+Shift+L ensin!)
CQ.boot;
// -> TARKISTA post-ikkunasta rivi "CQMidi16n: 16n found — CC 32..47 (any chan) live"
//    (atovprojectin 16n lähettää joka faderin omalla MIDI-kanavalla; sidonta
//    tapahtuu CC-numerolla, configissa chan: nil).
//    Jos siellä lukee "no 16n detected": kytke laite ja aja
CQ.midiRescan;                 // (vain tarvittaessa)
CQ.deckA.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
CQ.set(\coco2Level, 0);
CQ.hold(1, 0);                 // deck kiertää coco1:een
```

```supercollider
// M8 testit:

// 1. Faderikartta (SPEC §6.1) — liikuta yksi kerrallaan ja varmista korvalla
//    ja/tai CQ.get-kutsulla, että oikea parametri liikkuu:
//    F1 coco1In     F5 coco2In     F9  qFreq                F13 deckA+B Level
//    F2 coco1Speed  F6 coco2Speed  F10 qChaos               F14 spaceMix (ei DSP:tä
//    F3 coco1Spaf   F7 coco2Spaf   F11 preA+B Drive              vielä -> ei ääntä)
//    F4 coco1Fb     F8 coco2Fb     F12 deckA+B Rate         F15 patchDepth
//                                                           F16 masterVol
CQ.get(\coco1Speed);           // esim. faderin F2 liikkeen jälkeen
CQ.get(\deckARate);            // F12: linkitetyn parin molempien pitää muuttua
CQ.get(\deckBRate);

// 2. Pehmeys (SPEC M8/§9): aja F2 (coco1Speed) hitaasti ääripäästä toiseen
//    -> repitchin pitää olla portaaton, ei zipper-rahinaa; F4 (fb) -ride
//    0 -> 1.25 ei saa tuottaa tasohyppyjä. Tuntuman pitää olla <= 20 ms.

// 3. Hot-plug (SPEC M8): irrota 16n USB:stä kesken session -> soitto jatkuu
//    (ei virheryöppyä posissa). Kytke takaisin ja aja:
CQ.midiRescan;
//    -> faderit toimivat taas (Windows/portmidi ei hot-plugaa itsekseen).

// 4. Takeover: \jump (oletus) = fader kirjoittaa heti liikkeestä.
CQ.snap(\fadertesti);          // talleta tila
CQ.set(\coco1Fb, 1.2);         // muuta koodista (fader F4 on nyt "väärässä")
//    -> \jump: F4:n pienikin liike hyppäyttää arvon faderin kohtaan.
CQ.midi.takeover = \pickup;    // vaihda pickup-tilaan
CQ.recall(\fadertesti);        // recall siirtää arvot faderien alta
//    -> \pickup: fader on kuuro kunnes se AJETAAN nykyisen arvon yli,
//       sitten se tarttuu — ei hyppyjä recallin jälkeen.
CQ.midi.takeover = \jump;      // palaa oletukseen (tai jätä pickup päälle)
```

- [x] M8 hyväksytty (pvm: 2026-07-18 — kaikki 16 faderia, pehmeys ok, hot-plug +
      `CQ.midiRescan` ok, jump/pickup ok; \jump jää oletukseksi. F2:n hetkellinen
      repitch nostettaessa = speksin mukainen RAM-kellokäytös, ei bugi.)

---

## Avoin laitteistogate (aiemmilta vaiheilta)

- [ ] **Stemien live-kuuntelu** (KUUNTELU 7, kohdat 7.4/7.5 — arkistossa):
  vaatii ≥ nelikanavaisen äänikortin. NRT-turva on jo vihreä; live-todennus
  odottaa laitteistoa. Ainoa vielä avoin manuaalitesti aiemmilta vaiheilta.

---

## Nocturne-vaihe — yö koko soittimena

Vaiheen kuvaus ja rakennusjärjestys: [`PLAN.md`](../PLAN.md) ja
[`docs/NIGHT_STANDALONE_PLAN.md`](../docs/NIGHT_STANDALONE_PLAN.md) §12.
Jokaisen inkrementin oma kuuntelu-/välitarkastuslohko lisätään tähän sitä
mukaa kun se rakennetaan (punch → kevättulva → almanakan maisemointi → ranget
→ dolby → linnut → juuret → drawer-workshop). Korvapassin (§13) lohkot samoin.

### N1 — Punch (momentaarinen hold, §2)

Oikean napin **pito** kuoren sydämellä (spiraalin sisin silmä, sama merkki kuin
HOLD) invertoi loopin sinetin pidon ajaksi ja palauttaa irrotettaessa. Vasen
napautus sydämeen = tavallinen hold-toggle kuten ennen. Uusi ele, ei uutta
grafiikkaa — sinetti/sydän piirtyvät jo hold-tilasta, joten pidon aikana sinetti
näkyvästi halkeaa tai sulkeutuu. **Vaatii GUI:n yökartan.**

```supercollider
// N1 — Punch. Lähtötila: deck A soi (ks. yllä). Avaa GUI ja paina Tab -> yökartta.
CQ.hold(1, 1);                 // 1. lukitse coco1:n looppi: punainen sydän, sinetti ehjä
// 2. Vie hiiri coco1:n (vasemman kuoren) SYDÄMEEN ja pidä OIKEAA nappia pohjassa:
//    venttiili aukeaa pidon ajaksi -> uutta materiaalia (deck A) tippuu renkaaseen
//    ja sinetti halkeaa näkyvästi. Irrota nappi -> looppi sulkeutuu ja juuri
//    pudonnut pätkä jää kiertämään. Tämä on nauhurin punch-in.
CQ.hold(1, 0);                 // 3. avaa looppi (delay-tila, deck A kiertää sisään)
// 4. Tee sama oikean napin pito sydämellä: looppi JÄÄTYY hetkeksi (pätkä
//    pysähtyy), irrotus vapauttaa kierron taas. Sama ele, molempiin suuntiin.
// 5. Vasen napautus sydämeen = hold-toggle kuten ennen (punch on vain oikea nappi).
```

**Exit-polut (KRIITTINEN turvatesti).** Jumiin jäänyt punch jättäisi loopin
auki tallentamaan ja tuhoaisi materiaalin, joten jokainen keskeytys palauttaa
holdin täsmälleen pidon-alkua-edeltäneeseen arvoon. Aloita oikean napin **pito**
sydämellä ja keskeytä se kullakin tavalla erikseen; joka kerran jälkeen sinetin
(punaisen holdin) pitää palata lähtötilaansa **eikä** coco1 saa jäädä
materiaalia/kohinaa kasaavaan tilaan:

- **irrota hiiren nappi** (luonnollinen vapautus)
- **Esc** (pidä nappia, paina Esc)
- **Tab** (pidä nappia, paina Tab — näkymä vaihtuu, hold palautuu silti)
- **välilyönti = panic** (pidä nappia, paina space; jälkeen `CQ.resume`)
- **ikkunan X / `CQ.free`** ja **CmdPeriod (Ctrl+.)** — nämä repivät koko GUI:n;
  hold palautuu tilaan, CmdPeriodin jälkeen vaadi `CQ.reboot`.

Koko exit-polkujoukko (mouse-up · Esc · Tab · panic · window close · GUI free ·
CmdPeriod) on lisäksi konevarmistettu automaattitestissä
`tests/gui_night_smoke.scd` — manuaalisesti riittää todeta soiva käytös ja
muutama kädellä toistettava keskeytys.

- [x] N1 hyväksytty (pvm: 2026-07-23 — punch soi (materiaali tippuu sinetöityyn
      looppiin ja jää; auki-tilassa jäätyy pätkä), exit-polut palauttavat holdin
      eikä looppi jää tallentamaan. Oikea nappi = punchin koti; valinnainen
      Shift+1/2-näppäinpunch jätettiin pois.)

### N2 — Kevättulva: kokopassi (§8)

Yön kaikkien kehojen ja kosketuskohteiden peruskoot on nostettu **yhtenä
sommitelmana**. Ei uusia muotoja eikä uusia merkityksiä: sama topologia, samat
eleet, samat värit, samat viivanpaksuudet ja kirjasinkoot (§16.3/§16.4 eivät
liiku — muste ei paksune kun paperi kasvaa). Mitat asuvat nyt yhdessä
taulukossa, `classes/CQNightMeasures.sc`, ja törmäysmatriisi
(`CQNightGeometryTest`) pitää ne erillään molemmilla referenssikoolla.

**Tämä on sommittelukatselmointi: katso ensin, älä säädä.** Käy läpi
**kahdella ikkunakoolla** (§15:n oma laki): ensin minimi-ikkuna 1280×720,
sitten läppärin maksimoitu koko.

```supercollider
// N2 — Kevättulva. Aja rivi kerrallaan. Vaatii GUI:n; luokkakirjasto
// uudelleenkäännetty (Ctrl+Shift+L) ennen tätä.
CQ.boot;                       // 1. käynnistä ja avaa GUI, paina Tab -> yökartta
// 2. MINIMIKOKO: raahaa ikkuna 1280x720:een (tai lähimpään). Katso koko lahtea
//    kerralla: onko mittakaava nyt käden mittainen vai liian ahdas? Erityisesti
//    (a) linnojen sisäkellot, (b) kuorten sydämet, (c) renkaan silmä (myrskyiiris),
//    (d) vuorovesisirpit, (e) lintujen siivet ja juurten korkeus.
CQ.set(\qChaos, 0.9); CQ.set(\qFreq, 0.8);   // 3. iiris: säteet ja pupilli isoina
CQ.set(\qChaos, 0.05); CQ.set(\qFreq, 0.2);  // 4. ...ja tyynenä. Lukeeko ero yhä?
CQ.hold(1, 1);                 // 5. sinetti + sydän: näkyykö punainen selvästi?
CQ.hold(1, 0);
CQ.shake;                      // 6. heitä kaapelikenttä: peittääkö mikään kasvanut
                               //    keho tai laatikko merkkejä joita ennen näkyi?
CQ.clearPatch;                 // 7. tyhjennä kenttä (tai Ctrl+Z GUI:ssa)
// 8. MAKSIMOI ikkuna. Sama katsaus: pysyykö sommitelma kasassa, jääkö vettä
//    liikaa/liian vähän, ovatko merkit yhä oikean kokoisia suhteessa kehoihin?
// 9. Kokeile käsin: sydän (hold), sisäkello (spesal-askel), sirppi (veto),
//    silmä (veto = qFreq/qChaos), kuoren ura (nopeus), suun chevron ja skip-merkki.
//    Osuuko käsi nyt kohteisiin ilman tähtäämistä?
```

**Tiedossa oleva, katselmoinnissa päätettävä asia:** suun alla oleva
**skip-merkki** on tulvan jälkeen 30 + 26 px suusta (kasvaneet osumalaatikot
pakottavat sen sinne). Juuri siinä vedessä kulkevat renkaasta tämän kuoren
kuihin menevät kaapelit: mitattu esimerkki `tri1 → coco1_spaf` kulkee **6.0 px**
merkin ohi (ennen tulvaa 17.5 px), ja §4.5:n mukaan **kaapeli voittaa
osoittimen alla** — eli sellaisen patchin aikana skip-merkki ei vastaa ennen
kuin kaapeli siirretään. Merkin koti lyötiin lukkoon G2-portissa neljän
katselmointikierroksen jälkeen, joten sitä ei siirretty omin päin. Päätä
katselmoinnissa: (a) hyväksy kielioppi sellaisenaan, (b) kutista `shellOuterR` /
`moonArc`, vai (c) avaa merkin koti uudelleen.

- [x] N2 hyväksytty (pvm: 2026-07-26 — mittakaava, molemmat ikkunakoot.
      Skip-merkin kaapelikaista hyväksyttiin sellaisenaan: §4.5:n kielioppi
      (kaapeli voittaa osoittimen alla) jää voimaan, merkin koti ei liiku.
      Kokoihin palataan vielä §9:n uuden sommitelman yhteydessä.)

