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
//    F2 coco1Speed  F6 coco2Speed  F10 qChaos               F14 vapaa (ei muutosta)
//    F3 coco1Spaf   F7 coco2Spaf   F11 preA+B Drive
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
→ dolby → linnut → juuret → drawer-workshop → Night-only). Korvapassin (§13)
lohkot samoin.

### N1 — Punch (momentaarinen hold, §2)

Oikean napin **pito** kuoren sydämellä (spiraalin sisin silmä, sama merkki kuin
HOLD) invertoi loopin sinetin pidon ajaksi ja palauttaa irrotettaessa. Vasen
napautus sydämeen = tavallinen hold-toggle kuten ennen. Uusi ele, ei uutta
grafiikkaa — sinetti/sydän piirtyvät jo hold-tilasta, joten pidon aikana sinetti
näkyvästi halkeaa tai sulkeutuu. **Vaatii GUI:n yökartan.**

```supercollider
// N1 — Punch. Lähtötila: deck A soi (ks. yllä). Valitse toolbarista Night.
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
- **Tab** (pidä nappia, paina Tab — vesifokus vaihtuu, hold palautuu silti)
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
CQ.boot;                       // 1. käynnistä ja avaa GUI, valitse toolbarista Night
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

### N3 — Kuu ja koko lahden vuorovesi (§9, versio 3, 2026-07-27)

Lampi ja almanakkarypäs on poistettu. Neljä siementä ovat nyt ainoat levossa
näkyvät vuorovesimerkit; niiden väliin ei piirretä rajaa. Kuu on yksin oikean
linnun takaisessa ylätaivaassa ja kantaa myös sään pystyeleissään.

```supercollider
// N3 — Vaatii GUI:n; Ctrl+Shift+L ennen tätä.
CQ.boot;                       // 1. avaa GUI, valitse toolbarista Night
// 2. Katso koko maisemaa: tuntuuko kuu taivaankappaleelta eikä sivupaneelilta?
//    Tuntuvatko neljä pientä siementä yhden laajan kentän tuulensuunnilta?
// 3. Kuun pitää aueta pienenä sirppinä. Napauta eri vaiheissa: neljänneskuun
//    pitäisi tuottaa vain noin 4 johtoa, puolikuun noin 18 ja täyden noin 70.
//    Vedä vaakaan = vaihe. Vedä alas = sade/grow, ylös = aurinko/wither:
//    aivan kuun vieressä sää on harva; noin 40 px vedolla sadepisaroita /
//    auringonsäteitä on enemmän ja rytmi nopeampi. Sade saa silti olla selvästi
//    aurinkoa hitaampi. Merkit näkyvät vain pidon ajan.
CQ.patch(\tri1, \coco1_spaf, 0.55);
CQ.patch(\envB, \castle3_fm, -0.45);
// 4. Tavallinen tap vapaaseen kuoreen EI kirjoita. Shift+tap tallentaa/korvaa;
//    täyden tap palauttaa ja oikea tyhjentää. Avaa Tab/kidus-fokus: nyt tap
//    vapaaseen kuoreen tallentaa myös ilman näppäimistöä.
// 5. Klikkaa TAI vedä >8 px fokusoimatonta avointa vettä: mitään ei tapahdu.
//    Paina Tab: alakeskuksen kidus aukeaa sinisenä ja kaapelijänteet uppoavat.
//    Nyt vasen morph jatkuu myös kehojen yli; vapautus = yksi undo-op.
// 6. Paina Tab uudelleen tai klikkaa kidusta: sama pysyvä fokus sulkeutuu.
//    Shift antaa hetkellisen fokuksen. Fokuksessa oikea veto on elastinen ja
//    palaa lähtöön; fokuksen ulkopuolella oikea veto on edelleen lasso.
// 7. Varmista kolme eri kuvaa: vapaa paikka = avoin kuori; tallenna siihen
//    täysin tyhjä patch = suljettu tumma leposiemen; tallenna soiva patch =
//    tiheyden mukaan hohtava suonellinen ydin. Vedä täysi toisen täyden päälle:
//    lanka kulkee kaikkien kehojen päällä, tarttuu kohteen keskelle ja kohde
//    kukkii sinisellä kaksoiskehällä.
// 8. Katso kaikki viisi castlea: ei lyhyitä irrallisia viivastubeja, ei
//    suorakulmaisia tyhjiä endpoint-aukkoja castlen ääriviivan päällä.
```

**Katso erityisesti:**

1. Onko kuu tarpeeksi kaukana linnusta ja tarpeeksi pieni (Ø 56), vai näyttääkö
   se yhä irralliselta kontrollilta?
2. Erottuvatko avoin kuori, tallennettu hiljaisuus ja elävä siemen varmasti
   toisistaan, mutta pysyvätkö ne silti osana maisemaa?
3. Näkyvätkö aktiiviset, enintään 8 % wavefront-kaaret riittävästi ilman että
   vesi muuttuu koristeanimaatioksi?
4. Onko Tab/kidus-fokus ymmärrettävä ja säilyykö patchin topologia luettavana?
5. Ovatko uusi store/recall-suoja ja breedin kelvollinen pudotuskohde
   yksiselitteisiä?
6. Ovatko castlet ehjiä kaikissa viidessä asennossa ilman laatikkoleikkauksia?
7. Koko lahden kokojälkitarkastus 100/125/150 %. Juuret arvioidaan tässä, mutta
   niiden preamp-kehoa ei vielä toteutettu.

- [x] N3 hyväksytty (pvm: 2026-07-27 — kuun koti/koko, siementen paikat,
      vuorovesipalautteen voimakkuus, Tab/kidus-fokus, breed-vihje,
      castle-kerrokset ja kokojen jälkitarkastus. Myöhemmäksi polish-velaksi
      kirjattiin yksi rajattu poikkeus: Tab-kiduksen pitää voittaa sen yli
      kulkeva kaapeli hoverissa ja painalluksessa sekä pysyä löydettävänä
      kaapeli-inkistä huolimatta. Tämä ei avaa N3-porttia uudelleen.)

### N4 — Range-väreet (§3)

Jokaisen linnan 6-suunnassa on nyt pieni soitettava väre. Yksi pitkä
puoliaalto = infra, kaksi puoliaaltoa = balcium, viisi tiheää puoliaaltoa =
audio. Vanhoja `i/b/a`-kirjaimia ei enää ole: käyttäjän N4-päätöksellä muoto
riittää ja peritty Cocoquantus-nimistö poistui yöstä. Yhteinen vaakaköli erottaa
väreen fm-kuun vapaasta yhden syklin aallosta. Napautus askeltaa; sisäkello
askeltaa edelleen spesalia ja linnan liha on edelleen body-solo. Kolmen
hover-vihjeen pitää olla toisensa poissulkevia.

```supercollider
// N4 — Vaatii GUI:n; Ctrl+Shift+L ennen tätä. \classic yksin ei takaa
// kuultavaa range-eroa: ilman samplea/live-inputtia se käyttää linnoja vain
// Coco-modulaattoreina. Tämä eristää yhden Quantussy-linnan omaksi ääneksi.
CQ.boot; CQ.set(\masterVol, -24); CQ.patchPreset(\quantumDrone);
[\c2Send, \c3Send, \c4Send, \c5Send].do { |n| CQ.set(n, 0) };
CQ.set(\c1Send, 0.7); CQ.set(\qChaos, 0); CQ.set(\qFreq, 4);
CQ.set(\coco2Level, 0); CQ.set(\coco1Fb, 0.2); CQ.hold(1, 0);
CQ.set(\c1Range, \infra);            // 1. valitse toolbarista Night
// 2. Napauta castle 1:n ALAPUOLISTA VÄRETTÄ kolme kertaa:
//    infra (1 puoliaalto) -> balcium (2) -> audio (5) -> infra (1).
//    Äänessä linnan pitää nousta lähes kuulumattomasta hitaasta liikkeestä
//    noin 4 Hz sykkeeksi ja sitten selväksi noin 160 Hz ääneksi.
// 3. Valitse kuultava linna ja nollaa muut yhdellä apurilla:
~hearCastle = { |n| (1..5).do { |k|
    CQ.set(("c" ++ k ++ "Send").asSymbol, if(k == n) { 0.7 } { 0 })
} };
//    Aja ~hearCastle.(2), napauta castle 2; sitten 3, 4 ja 5.
//    Toista halutessasi kaksi kierrosta / 6 askelta.
//    Napauta myös sisäkelloa ja linnan lihaa: väre ei saa varastaa niiden eleitä.
// 4. Vie osoitin vuorotellen väreen, sisäkellon ja lihan päälle. Vain yksi vihje
//    saa kirkastua kerrallaan. Vie kaapeli väreen yli: kaapelin pitää voittaa
//    hoverissa ja painalluksessa, eikä range saa silloin vaihtua.
// 5. Katso 1280×720-koossa ja maksimoituna: kaikilla linnanreuna -> väre -raon
//    pitää näyttää samalta. Kölin pitää erottaa balcium fm-kuun aallosta.
//    Kun jokirengas kulkee väreen alta, kapea muotoa seuraava ura erottaa ne:
//    ei yhteistä samanväristä sotkua eikä suorakulmaista aukkoa joessa.
```

**Katselmointikysymys:** erottaako yhteinen köli nyt 1/2/5-puoliaaltoisen
hidas/keskinopea/nopea-perheen linnan `fm`-kuun vapaasta wave-glyyfistä?
Pysyvätkö joen ylitykset ehjinä mutta yksiselitteisinä, vai näyttääkö kapea
erotusura joen katkolta?

- [x] N4 hyväksytty (pvm: 2026-07-27 — `\quantumDrone` varmisti
      infra/balcium/audio-muutoksen ja GUI-kirjoituksen; kirjaimeton
      köli/aalto-perhe erottuu fm-kuusta, 13 px kehorako ja joen paikallinen
      vektoriura toimivat, hover-prioriteetti ja molemmat ikkunakoot hyväksytty)

### N5 — Dolby-osterit (§4)

Kummankin Cocon input- ja feedback-sirpin alla on oma tekstittämätön
kolmiasentoinen osteri. Auki = `off`, lähes kiinni = `gate`, kiinni ja
kumppanikanavan suunnasta tuleva lyhyt painallusviiva = `duck`. Napautus askeltaa
`off → gate → duck → off`; sirpin oma pystysuora VCA-veto ei muutu.

```supercollider
// N5 — Dolby-osterit. Ctrl+Shift+L, sitten käynnistä Night.
CQ.boot;
CQ.clearPatch;
CQ.loadA(Platform.resourceDir +/+ "sounds/a11wlk01.wav");
CQ.set(\deckALevel, 0.7);
CQ.set(\coco1SlotPre, 1);      // deck A input-kanavaan
CQ.set(\coco1In, 0.8);
CQ.set(\coco1Fb, 0.72);
CQ.set(\coco1Speed, 0.34);
CQ.hold(1, 0);
CQ.set(\coco1DolbyIn, \off);
CQ.set(\coco1DolbyFb, \off);
// 1. Etsi vasemman kuoren IN- ja FB-sirppien alta kaksi osteria. Niissä ei ole
//    kirjaimia. Ovatko auki / lähes kiinni / kumppanin painama kiinni luettavissa
//    100 % skaalassa normaalilta soittoetäisyydeltä?
// 2. Napauta INPUT-osteria kolmesti. Kuuntele: gate sulkee inputin oman
//    hiljaisuuden; duck antaa feedback-envelope'n artikuloida inputia; off palaa.
// 3. Napauta FEEDBACK-osteri duckiin. Kuuntele loopin aikakerrosten pumppaava
//    vastavuoroisuus: input-envelope painaa feedbackia, ei sama kanava itseään.
// 4. Vedä IN- ja FB-sirppejä pystysuoraan. VCA:n pitää yhä kaatua täsmälleen
//    kuten ennen; osterista alkava veto ei saa liikuttaa VCA:ta.
// 5. Tee tiheä patch CQ.shake-komennolla. Kaapeli osterin päällä voittaa hoverin
//    ja painalluksen: siitä tartutaan johtoon, Dolby ei askella.
CQ.clearPatch;
```

**Visuaalinen portti:** tarkista sekä 1280×720 että maksimoitu ikkuna, 100/125/
150 % käyttöjärjestelmäskaalaus. Osterin pitää pysyä oman sirppinsä alla,
naapurisirppien ja kuun osuma-alueiden pitää säilyä erillään, eikä partnerin
saapumisviiva saa leikkautua oman 30 px leveän paperinsa reunaan.

- [x] N5 hyväksytty (pvm: 2026-07-27 — off/gate/duck luettavuus,
      input/feedbackin oikea ristikkäinen artikulaatio ja VCA-vedon säilyminen
      hyväksytty. Katselmuksessa löytynyt kaapeli×osteri-hoverin tyhjä tila
      korjattiin niin, että kaapeli saa sekä hoverin että painalluksen; samalla
      org-outputin opaakin laatikon katkaisema playhead-spiraali palautettiin
      aidolla käyrä-underlaylla. Automaatio varmistaa overlap-prioriteetin.)

### N6 — Linnut hakevat (§11)

Lintu on nyt deckin oma tekstittämätön transportti. Napautus hakee tiedoston,
rulla kävelee ladatun tiedoston kansiota ja lyhyt vaakasuuntainen
ulos–takaisin-pörhistys arpoo sisaruksista yhden. Uusia nappeja, nuolia tai
glyyfejä ei lisätty. Kaikki onnistuneet latauspolut päivittävät saman
tiedostonimen ja väläyttävät linnun 120 ms ajan.

```supercollider
// N6 — Ctrl+Shift+L, sitten CQ.boot ja toolbarista Night.
CQ.boot;
CQ.set(\deckALevel, 0.7);
CQ.set(\coco1SlotPre, 1);
CQ.set(\coco1In, 0.8);
CQ.set(\masterVol, -24);
// 1. NAPAUTA vasenta lintua liikkumatta yli 8 px. Dialogin pitää avautua
//    suoraan konfiguroituun samples/-kansioon. Valitse jokin wav-tiedosto:
//    nimen pitää vaihtua linnun alle, linnun välähtää ja materiaalin kuulua
//    coco1:n läpi. Avaa dialogi uudelleen ja peruuta: nimi ja ääni eivät muutu.
// 2. RULLAA linnun päällä alas: seuraava tiedosto aakkosjärjestyksessä.
//    Rullaa ylös: edellinen. Nimen ja äänen pitää seurata. Yksi tavallinen
//    pykälä = yksi tiedosto; touchpadin momentum ei saa hypätä viittä kerralla.
// 3. PÖRHISTÄ lintua vaakaan: vähintään 6 px ulos ja vähintään 6 px takaisin
//    (yksi luonteva suunnanvaihto), sitten irrota. Satunnaisen sisarustiedoston
//    pitää latautua ja näkyä nimessä. Tee viisi kertaa:
//    valintojen ei tarvitse olla eri, mutta jokaisen onnistumisen pitää välähtää.
//    Kokeile myös leveää ja usean suunnanvaihdon heilautusta: selvä pörhistys
//    ei saa raueta, vaikka osoitin käy linnun pienen piirtoalueen ulkopuolella.
// 4. Vedä lintua kerran suoraan yli 8 px ja irrota. Tämä ei ole napautus eikä
//    pörhistys: dialogia ei aukea eikä tiedosto vaihdu.
// 5. Vie osoitin linnun päälle: lintu kirkastuu staattisesti. Kokeile myös
//    tiedoston pudotusta ja koodipolkua; molemmat käyttävät samaa nimi+välähdys-
//    palautetta:
CQ.deckA.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
// 6. Varmista, että siiven rate-socket ja linnun level-sirppi vastaavat yhä
//    omiin vanhoihin eleisiinsä eivätkä avaa dialogia tai pörhistystä.
```

**Visuaalinen ja soitettava portti:** kokeile 1280×720 ja maksimoituna sekä
100/125/150 % käyttöjärjestelmäskaalaus. Hoverin pitää olla selvä mutta
staattinen, onnistumisvälähdyksen erotuttava tavallisesta hoverista, eikä
siipien muodon tai tiedostonimen pidä hypätä. Jos kaapeli osuu linnun paperille,
kaapeli voittaa hoverin ja painalluksen; automaatio syöttää tämän prioriteetin
myös suoraan lintuhandlerille, koska nykyisessä hyväksytyssä sommittelussa
rate-socketin ylempi sisarnäkymä omistaa tavallisesti linnun alapuolisen
kaapelin viimeiset pikselit.

- [x] N6 hyväksytty (pvm: 2026-07-28 — dialogin samples-aloituskansio/peruutus,
      kansion rullaus ja momentum, pörhistyksen satunnaislataus, suoran vedon
      inerttiys sekä nimi+120 ms välähdys toimivat live-testissä. Ensimmäinen
      pörhistystunnistin ei lauennut Windows/Qt-ikkunassa; hyväksytty versio
      kaappaa lintudragin ja seuraa kursoria myös lintunäkymän ulkopuolella.)

### N7 — Juuret ovat preamp-keho (§10)

Kummankin alareunan viisi nimetöntä, leveästi ja epäsäännöllisesti kasvavaa
lähdesäiettä kokoontuvat kaksilobiseen preamp-solmuun. Säikeen hover paljastaa
ehdokkaan, klikkaus valitsee sen ja valitun toinen klikkaus sulkee tulon
(`none`). Solmun 8 px akselilukon jälkeen pystysuora 240 px veto soittaa
drivea ja vaakasuora biasia. Audiojuoni nousee vastaavaan Cocoon, ohenee
levossa 1,5 px:iin ja kasvaa driven mukana 2/3 px:iin; `SlotPre` ja env-aura
kertovat reitin avautumisesta. Vihreä `envA/B` säilyy pienenä ulospäin
kasvavana patchattavana sivusilmuna. Lintu- tai juuri-hover paljastaa vain
todet, ehdolliset deck→Coco- ja bird→preamp-lennot.

```supercollider
// N7 — Ctrl+Shift+L, sitten käynnistä Night.
CQ.boot;
CQ.clearPatch;
CQ.loadA(Platform.resourceDir +/+ "sounds/a11wlk01.wav");
CQ.set(\preASrc, \deckA);
CQ.set(\preADrive, 2);
CQ.set(\preABias, 0.15);
CQ.set(\coco1SlotPre, 1);
CQ.set(\coco1In, 0.8);
CQ.set(\coco1Fb, 0.35);
CQ.set(\masterVol, -24);
CQ.hold(1, 0);
// 1. LEPO: kummallakin puolella pitää näkyä viisi leveää, epäsymmetristä,
//    nimetöntä tulosäiettä, kaksilobinen solmu, hillitty audiojuoni,
//    ja pieni vihreään env-jakkiin päättyvä sivusilmu. Audiojuonen pitää
//    liittyä Cocoon viivaan — ei pysähtyä suorakulmaiseen näkymärajaan.
// 2. HOVER jokaisella säikeellä: vain ehdokas kirkastuu ja sen nimi ilmestyy.
//    Nimen pitää erottua vaaleana myös kirkkaan vihreän juuren vieressä.
//    Klikkaa deck a; sen pitää jäädä hienovaraisesti aktiiviseksi. Klikkaa
//    samaa uudelleen: kaikki säikeet lepäävät ja solmun hover sanoo "none".
// 3. Valitse deck a ja vedä vasenta solmua YLÖS. Driven pitää nousta
//    aiempaa rauhallisemmin, äänen säröytyä ja audiojuonen painon kasvaa
//    1,5 -> 2 -> 3 px. Vedä alas: arvo palaa kohti 1:tä.
// 4. Vedä samaa solmua OIKEALLE/VASEMMALLE. Biasin pitää muuttua ilman driven
//    liikettä; oikealle ylempi lobe paisuu ja alempi kuroutuu. Vinossa vedossa
//    ensimmäinen selvä akseli lukittuu koko eleeksi.
// 5. Hoveroi lintu A: himmeä reitti menee sen omaan Cocoon ja katoaa, jos
//    coco1SlotDeck = 0. Kun deck A on preamp A/B:n lähde, hover paljastaa myös
//    lennon oikeaan juureen. Deck-säikeen hover tekee saman juuresta käsin.
//    Reitin pitää kulkea katkeamatta jokaisen linnun, Cocon ja muun elementin
//    näkymättömän piirtoalueen yli.
// 6. Patchaa vihreä sivusilmu. Vain env-jakista lähtee tavallinen kaapeli;
//    solmusta, tulosäikeistä tai paksusta audiojuonesta ei voi tarttua.
CQ.patch(\envA, \coco1_spaf, 0.7);
// 7. Vedä/repäise kaapelia solmun tai säikeen yli. §4.5: kaapeli voittaa
//    hoverin ja painalluksen. Esc, Tab, PANIC ja ikkunan sulku vapauttavat
//    kesken jääneen solmuvedon; jo kirjoitettu arvo jää voimaan.
```

**Visuaalinen ja soitettava portti:** tarkista 1280×720 ja maksimoituna sekä
100/125/150 % käyttöjärjestelmäskaalaus. Arvioi erityisesti:

1. Lukeeko 1,5/2/3 px juoni audioanatomiana eikä uutena patch-kaapelina?
2. Onko vihreä `envA/B` selvästi pienempi sivukasvu mutta silti helppo tarttua?
3. Valitsevatko viisi säiettä varmasti oikean lähteen, ja tuntuuko aktiivisen
   toinen klikkaus luontevalta tavalta sulkea tulo?
4. Lukittuuko solmu varmasti pystysuunnassa driveen ja vaakasuunnassa biasiin?
   Tuntuuko 240 px riittävän rauhalliselta mutta yhä soitettavalta?
5. Näkyykö biasin muoto ja driven viivapaino soittoetäisyydeltä ilman numeroa?
6. Kiertääkö audiojuoni sirpit luonnollisesti molemmilla puolilla, ja liittyykö
   se spiraaliin ilman katkoa kaikissa kolmessa skaalassa?
7. Paljastuuko bird→Coco vain lintu-hoverissa ja häviääkö se `SlotDeck = 0`:ssa?
   Seuraako bird→root aina todellista valintaa myös ristiin A→B / B→A?
8. Ovatko juuret riittävän leveät, epäsymmetriset ja levottomat mutta jokainen
   säie silti seurattavissa ja klikattavissa? Erottuvatko erityisesti vasemman
   puolen `deck a` ja `mic` toisistaan?
9. Kuuluuko drive/bias-muutos selvästi deckillä/live-inputilla eikä muuta
   DSP-reittiä tai env-patchin toimintaa?
10. Jatkuvatko kaikki hover-reitit ilman suorakulmaisia aukkoja ja piirtyykö
    vihreä output-sivuhaara yhtenä pyöreänä käyränä jakille asti?

- [x] N7 hyväksytty (pvm: 2026-07-28 — lähdeklikkaus, drive/bias-käden tuntu,
      viivapaine, bird/Coco/root-reitit, env-sivusilmu, kaapeliprioriteetti,
      juurten epäsymmetria, 100/125/150 % ja kuuntelu. Katselmoinnissa yksi
      korjaus: oikean juuren provenienssitekstin näkymätön taustalaatikko
      pyyhki kahden vasemmanpuoleisimman säikeen yläosan. Erasure mittaa nyt
      siemenetyn deltan omalta kaistaltaan ja väistyy ulospäin; törmäysmatriisi
      kirjoitettiin ensin vanhoilla vakioilla ja kaatui juuri noihin kahteen
      säikeeseen sekä 0,8 px:n peilitapaukseen juuressa A.)

### N8 — Drawer-workshop (§12.1)

Drawerin neljäs pysyvä sivu `workshop` on **Nightin hiljainen mittapöytä**:
sama soitin vastakkaisella kieliopilla. Jaettu raili, kolmen moduulin huoltorivi
(`deck a · input trims · deck b`) ja sen alla `coco 1 · quantussy ·
coco 2`. Kaikki 39 puuttuvaa CQSpec-parametria ovat
ledger-rivejä: nimi vasemmalla, hiusviivaraili keskellä, **tarkka arvo**
monolla oikealla. `micGain` **siirtyi** Daystä tänne — Dayhin jäi vain
passiivinen osoitin, eikä koko GUI:ssa ole toista mic-kontrollia.

```supercollider
// N8 — Ctrl+Shift+L ensin. Tämän lohkon voi ajaa myös ilman serveriä:
//   tests/gui_drawer_preview.scd  (pelkkä ulkoasu, ei ääntä)
CQ.boot;
CQ.set(\masterVol, -24);
// 1. Valitse toolbarista NIGHT, sitten DRAWER ja välilehti WORKSHOP.
//    SIRISTYSTESTI (katso 2 m päästä, älä lue): näetkö yhden matalan
//    huoltorivin ja sen alla kolme ledgeriä — etkä neljää pyöreää korttia,
//    natiivia asetuspaneelia, pienoiskoko-Daytä tai Nightin koristekuvastoa?
// 2. Lue läheltä: jokaisella 39 rivillä pitää näkyä tarkka arvo. Vertaa
//    coco 1:n ja coco 2:n ledgereitä: alaotsikoiden (converter · material ·
//    output · input mix) pitää olla täsmälleen samalla korkeudella.
// 3. Vedä `coco 1 · output · level` railia. Äänen pitää muuttua heti ja
//    arvon seurata kättä. Klikkaa saman rivin NIMEÄ: mitään ei saa tapahtua.
// 4. Rullaa `quantussy · detune · castle 3` -rivin päällä: hienosäätö.
//    Bipolaarisella rivillä täytön pitää kasvaa keskimerkistä ulospäin.
// 5. `coco 1 · converter · wrap` ja `read`: istuimet (off·on / zoh·smooth).
//    Napautus istuimelle vaihtaa tilan; kuuntele ero.
CQ.set(\coco1Level, 1.2);
// 6. Koodikirjoitus: rivin kahvan ja arvon pitää seurata ilman että
//    kirjoitat itse mitään takaisin.
CQ.set(\micGain, 0.5);
// 7. MIC: rivin pitää muuttua punaiseksi ja master-strippiin ilmestyä
//    "MIC LIVE". Vaihda välilehteä ja SULJE drawer: varoituksen pitää
//    JÄÄDÄ näkyviin. Tarkista myös ettei Dayn sources-sarakkeessa ole enää
//    mic-nuppia — vain teksti "mic · drawer / workshop".
CQ.set(\micGain, 0);
// 8. KOKOASKEL. Suurenna ikkuna maksimiin: koko drawerin pitää vaihtaa
//    isompaan kokoaskeleeseen — teksti kasvaa (12 -> 15 px), rivit
//    24 -> 28 px, raili levenee ja loomin solut kasvavat. Reunoille pitää
//    silti jäädä marginaali; sivu ei saa mennä kiinni reunoihin.
//    Pienennä takaisin 1280x720:een: askel palaa, mitään ei leikkaudu eikä
//    sivu vierity. Viimeisen `input mix · x` -rivin ja `castle 5` -sendin
//    pitää näkyä kokonaan molemmissa koissa.
// 8b. JOS teksti EI kasva maksimoitaessa, aja tämä ja kerro tulos — se
//    kertoo suoraan kummasta on kyse, liian pienestä sivusta vai bugista:
CQ.gui.drawer.pageSize;   // sivun todellinen koko, esim. Size(1528, 630)
CQ.gui.drawer.step;       // \compact tai \roomy
CQGuiScale.minimumWidth(\roomy);                          // vaadittu leveys
CQGuiScale.minimumHeight(\roomy) + CQGuiScale.comfort;    // vaadittu korkeus
//    Huom: Windowsin 150 % skaalauksella maksimoitu 1080p-näyttö ON
//    looginen 1280x720, jolloin compact on oikea eikä vika.
// 9. Vaihda DAY: saman geometrian pitää piirtyä musteena paperille.
// 10. Käy KAIKKI neljä välilehteä läpi molemmissa koissa: inspector,
//    workshop, spellbook, memory. Niiden pitää jakaa sama railin leveys
//    (sama vasen ja oikea reuna), **sama ylämarginaali** välilehtirivin ja
//    sisällön välissä, sama tekstikoko ja sama tyyli. Tarkista erityisesti:
//    loomin rivinimet ja vinot sarakenimet, spellbookin monoteksti,
//    memoryn nimilista ja patch-fingerprint. Memoryn saved snapshots-
//    ja patch fingerprint -laatikoiden pitää nyt yltää alas morph-palkkiin
//    asti, ja fingerprintin rivinimien kasvaa sen mukana. Valitse snapshot:
//    "outlined knots differ..." on pieni sivuhuomautus ja sen sekä alapuolisen
//    decks-otsikon välissä on selvä ilma.
// 11. HOVER. Liikuta Inspectorissa osoitinta eri matriisipisteiden yli:
//    täsmäsolu saa kevyen spark-pesun ja ääriviivan ennen klikkausta.
//    Hoveroi ja avaa Inspectorin preset-valitsin sekä Memoryn molemmat
//    Morph Bridge -valitsimet. Suljetun laatikon ja jokaisen avatun listan
//    rivin pitää käyttää samaa Ink-spark-kieltä — ei Windowsin sinistä
//    natiivivalikkoa. Liikuta osoitinta vähintään kolmen vaihtoehdon yli:
//    highlightin pitää seurata osoitinta ennen klikkausta, ei jäädä vain
//    nykyiseen valintaan.
// 12. FONTIN LEIKKAUSPORTTI. Aina kun fontin kokoa tai arvorankia muutetaan,
//    tarkista sekä vaakasuuntainen mahtuminen että yläreuna, alareuna ja
//    laskevat kirjaimet (g, j, p, q, y). Pienempi fontti ei saa pienentää
//    tekstikaistaa ilman ascent/descent-ilmaa. Tässä passissa tarkista
//    erityisesti koko "outlined knots differ from the live patch" -rivi:
//    Pen-kaistan pitää näyttää l-kirjaimen laki sekä p-kirjaimen alapidennys.
// 13. MEMORY-LISTA JA OTSIKOT. Hoveroi saved snapshots -listan eri rivejä:
//    kevyt highlight seuraa täsmälleen osoittimen riviä, ei yhtä riviä sen
//    yläpuolella, ja klikkaus valitsee juuri saman rivin.
//    `saved snapshots`, `patch fingerprint` ja `morph bridge` -otsikoiden
//    yläpuolella pitää olla sama pieni ilmatila laatikon reunaan.
```

**Visuaalinen ja soitettava portti:** tarkista 1280×720 ja maksimoituna sekä
100/125/150 % käyttöjärjestelmäskaalaus, molemmilla skineillä. Arvioi:

1. **Luettavuus ensin (2026-07-28 palautteen ydin):** onko teksti nyt
   riittävän suurta windowed-tilassa? Entä maksimoituna? Jos ei, kerro
   kumpi askel jää pieneksi — ne ovat erikseen säädettävissä.
1b. **Kromi:** ovatko toolbarin otsikko ja napit, drawerin välilehdet sekä
   master-stripin tekstit (vol/dry/space/depth, out/gr, notice, REC-aika)
   nyt linjassa drawerin tekstin kanssa? Master-strippi ei kasvanut
   korkeussuunnassa — vain muste. Jos jokin niistä on yhä pieni tai
   päinvastoin liian iso, nimeä se.
2. Lukeeko sivu Nightin *mittapöytänä* — sama lajityyppi, tietoisesti eri
   kielioppi — vai geneerisenä settings-paneelina?
3. **Väliotsikot** (converter · material · output · input mix): erottuvatko
   ne nyt riittävästi? Ne ovat isompaa tekstiä, viivamustetta ja saavat
   oman viivan nimen jälkeen. Jos yhä liian vaimeita, seuraava keino on
   lisää ilmaa niiden ympärille — ei versaaleja (§16.4) eikä uutta väriä.
4. Onko `coco 1 · quantussy · coco 2` -järjestys tunnistettavasti yön oma
   maantiede vai pelkkä lista?
5. Riittääkö ledger-rivin raili soitettavaksi, vai onko se liian lyhyt?
   Erottuvatko kahva, keskimerkki ja istuimet soittoetäisyydeltä?
6. Ovatko tarkat arvot oikealla luettavia ja onko desimaalitarkkuus
   kussakin ryhmässä järkevä (esim. `qShLag` 0.0040, `c5Off` -0.170)?
7. Onko semanttista väriä liian vähän vai liikaa (vain dust · organ · live ·
   mic saavat perheen)?
8. Tuntuuko nimipalstan klikkauksen inerttiys turvalta vai hämäävältä?
9. **Yhdenmukaisuus:** näyttävätkö neljä sivua yhdeltä työpöydältä vai
   neljältä eri tyyliltä? Onko marginaali reunoihin joka sivulla sopiva —
   liian ahdas vai liian väljä?
10. Löytyykö mic Daystä ilman apua pelkän osoitintekstin perusteella?
11. Kuuluuko jokaisen alaryhmän edustaja (deck rate/loop, converter,
   material, output, slot, castle detune/send, qShLag, space size/damp)
   oikein — eli kirjoittaako jokainen rivi sitä parametria jonka se nimeää?
12. Pystyykö koko session soittamaan avaamatta Daytä kertaakaan?

- [x] N8 hyväksytty (2026-07-28 — luettavuus molemmissa kokoaskeleissa,
      väliotsikoiden erottuvuus, neljän sivun yhdenmukaisuus, siristystesti,
      coco-symmetria, tarkat arvot, ledger-ele, väririisto, mic-siirto ja
      pysyvä varoitus, molemmat skinit, kuuntelupassi sekä viimeinen
      sivuhuomautus-/hover-/valikkopassi)

### N9 — Night on ainoa soitin (§0)

Ensimmäinen peruutettava Night-only-versio on valmis
`nocturne-night-only`-haaralla. Tässä portissa arvioidaan kokonaisuutta, ei
uuden osan pintakiillotusta: onko Night + Master + Drawer nyt aidosti yksi
itsenäinen soitin, ja kaivataanko poistetuista Day-yleisnäkymistä mitään
soitossa?

```supercollider
// N9 — Ctrl+Shift+L ensin, koska luokkarakenne muuttui.
CQ.free;
CQ.boot;
CQ.set(\masterVol, -24);
// 1. Soittimen pitää aueta suoraan Nightiin. Toolbarissa näkyy YÖLAHTI ja
//    DRAWER, mutta ei DAY/NIGHT-vaihtoa. Master ja PANIC pysyvät alhaalla.
// 2. Soita Nightia 10–15 minuuttia ilman Draweria: hae lintu napauttamalla,
//    vaihda tiedostoa rullalla/rufflella, soita juurten driveä ja biasia,
//    shellien hold/punch/flip/skip/Dolby-eleitä, castlejen rangeä ja steppejä
//    sekä kuuta, säätä, vuorovettä, siemeniä ja kaapeleita.
// 3. Avaa DRAWER. Käy inspector · workshop · spellbook · memory läpi ja
//    palaa samalla DRAWER-napilla Nightiin. Master, notice ja PANIC eivät saa
//    kadota; Nightissä kesken jääneen käden/auditionin pitää peruuntua.
// 4. SPESAL-TÄHDISTÖN KUOLEMA: hoveroi castlea ja napauta sen sisäkelloa.
//    Näkyykö partneri, moodi ja signaalin suunta riittävän selvästi juuri
//    eleen alla? Kaipaatko oikeassa soitossa pysyvää koko verkon karttaa?
// 5. MUISTIPOLKUKARTAN KUOLEMA: soita shelliä ja tarkista Workshopista
//    slotit, bits/wrap/interp ja input mix. Jääkö pois mitään päätöstä, jonka
//    tekisit staattisesta Coco-polun kuvasta mutta et näistä kahdesta pinnasta?
// 6. Paina välilyöntiä: PANICin pitää toimia riippumatta siitä, missä
//    viimeksi klikkasit. Sulje lopuksi turvallisesti.
CQ.free;
```

- [x] N9 hyväksytty (2026-07-28 — Night itsenäisenä soittimena, Drawer-paluu,
      yhden käyttöliittymän selkeys, spesal-tähdistön kirjattu kuolema ja
      muistipolkukartan kirjattu kuolema)

### 13.1 — Feedback-taperin korvaportti

Ensimmäinen kuuntelukandidaatti elää `nocturne-feedback-taper`-haaralla.
Feedbackin arvoalue, oletus, snapshotit ja DSP ovat ennallaan; vain käden
normalisoitu kartta muuttui. 16n:n `takeover: \pickup` on suositeltava, jos
snapshot-palautuksen jälkeinen fyysisen faderin hyppy häiritsee.

```supercollider
// 13.1 — Ctrl+Shift+L ensin, koska CQSpec muuttui.
CQ.free;
CQ.boot;
CQ.patchPreset(\empty);
CQ.set(\masterVol, -24);
CQ.deckA.load("<perkussiivinen kenttä-äänite>");
CQ.set(\coco1SlotDeck, 1);
CQ.set(\coco1In, 0.8);
CQ.hold(1, 0);
// 1. Aja vasemman Cocon feedback-sirppiä tai 16n:n F4:ää hitaasti alhaalta
//    ylös ja takaisin. Kaiku → luuppi → rouhe ei saa enää ryöpsähtää faderin
//    viimeisessä kuudenneksessa: 0.85→1.10 on nyt leveä 50→90 % kaista.
// 2. Etsi ikuinen looppi (~1.0). Löytyykö se ilman mikrometriliikettä, ja
//    pystyykö käsi palaamaan siihen luotettavasti?
// 3. Ratsasta 0.95↔1.15 edestakaisin. Onko bloom-kynnys ennustettava, vai
//    tuntuuko yläpään 1.10→1.25 viimeinen 10 % liian jyrkältä?
// 4. Toista Coco 2:lla / F8:lla. Molempien pitää tuntua samalta.
// 5. Katso samalla kummankin feedback-sirpin asteikkoa: koko vihreä /
//    meripihka / punainen rata näkyy himmeänä, kuljettu osuus kirkkaana ja
//    helmi nykykohdassa. Vaihtojen pitää osua kuultuihin 0.85- ja 1.10-rajoihin,
//    eikä jälkimmäisen Dolby-osterin pidä hukkua värin alle.
// 6. Aja SP.AF keskikohdan molemmin puolin: nollaloven pitää pysyä paikallaan,
//    sinisen inverso- ja oranssin verso-osuuden kasvaa keskeltä ulospäin.
// 7. Aja input unityn yli: vihreän normaalialueen pitää vaihtua lovella
//    meripihkaiseen overdriveen. Sen alla olevan Dolby-osterin pitää yhä erottua.
// 8. Katso kummankin linnun level-sirppiä: neutraalin unity-viivan pitää pysyä
//    täsmälleen samassa paikassa arvosta riippumatta; värialueita ei ole.
CQ.free;
```

- [x] 13.1 hyväksytty (2026-07-28 — taper sekä feedback-, SP.AF-, input- ja
      linnun level-sirppien semanttiset lisäykset hyväksytty)

### 13.2 — Speed-lattian korvaportti

Kuuntelukandidaatti (§13.2 vaihtoehto **A**) elää `nocturne-feedback-taper`-
haaralla. Nupin lattia 3000 → **1500 Hz** ja modulaation jälkeinen kova
kellorajoitin 1500 → **750 Hz**, eli ground jack -temppu säilyy täsmälleen
oktaavina nupin oman ulottuvuuden alapuolella. Katto (48 k), oletus (24000) ja
exp-käyrä ovat ennallaan, samoin kaikki tallennetut snapshot-arvot.

**Tämä on koko kädenkartan muutos, ei vain uusi alapää.** Speed-kaista on nyt
viisi oktaavia neljän sijasta, joten *jokainen* faderin senttimetri kattaa
enemmän. Kohta 4 on siksi portin tärkein kysymys: jos pimeys varasti liikaa
matkaa keskialueelta, kirjataan vaihtoehto **B** (lattia 2000, rajoitin jää
1500:aan) eikä tätä hyväksytä. 16n:llä `takeover: \pickup` on suositeltava —
sama fyysinen faderin asento tarkoittaa nyt eri lukemaa.

```supercollider
// 13.2 — Ctrl+Shift+L ensin, koska CQSpec ja \cqCoco muuttuivat.
// Huom: \empty eikä \classic, jotta kuulet kellon eikä preset'n omaa
// tri1+tri3 -> spaf -modulaatiota. Yksi muuttuja kerrallaan.
CQ.free;
CQ.boot;
CQ.patchPreset(\empty);
CQ.set(\masterVol, -24);
CQ.deckA.load("<kirkas kenttä-äänite>");
CQ.set(\coco1SlotDeck, 1);
CQ.set(\coco1In, 0.8);
CQ.hold(1, 0);                 // kerää looppi...
CQ.hold(1, 1);                 // ...ja lukitse se
// 1. UUSI POHJA. Kelaa coco1:n kello pohjaan (Night: kierrä kuoren spiraalia;
//    16n: F2). Oletusbufferilla looppi venyy pohjalla ~87 sekuntiin ja sointi
//    tummuu ja rakeistuu. Kysymys korvalle: onko 1500–3000 Hz SOITETTAVAA
//    pimeyttä vai mutaa? Missä kohtaa siitä tulee mutaa?
CQ.get(\coco1Speed);           // ...lue tästä se kohta jossa se kääntyy
// 2. URUT POHJALLA. Nosta organ ja kelaa uudestaan alas.
CQ.set(\coco1Organ, 0.5);
//    Urkupilli seuraa kelloa, joten se laskeutuu nyt oktaavin entistä alemmas
//    — pohjalla se lakkaa olemasta sävelkorkeus ja muuttuu sykkeeksi.
//    Onko se ääni jota käyttäisit, vai pelkkä rätinä? Kuuletko rajan?
CQ.set(\coco1Organ, 0);
// 3. GROUND JACK -TEMPUN SYVIN PÄÄ. Nupin oma lattia on 1500 Hz; modulaatio
//    saa painaa sen alta 750 Hz:iin. Vie käsi ensin omaan pohjaansa:
CQ.set(\coco1Speed, 1500);
CQ.patch(\tri1, \coco1_spaf, 0.8);
CQ.set(\coco1Spaf, -2.5);      // tri1 on infra-kolmio: odota koko hidas kierros
//    Keinunnan alaosa viipyy 750 Hz:n rajoittimella ja nousee takaisin.
//    Kimmellystä vai katastrofia? Rajoittimeen osuminen ei saa naksahtaa,
//    vaimentaa looppia pysyvästi eikä jättää kelloa jumiin.
CQ.unpatch(\tri1, \coco1_spaf);
CQ.set(\coco1Spaf, 0);
// 4. HINTA KESKIALUEELLA (portin ratkaiseva kohta). Palaa käyttökaistalle ja
//    hae kädellä tuttuja pisteitä — 12000, 18000, 24000 — pelkällä sirpillä
//    tai faderilla, ilman että katsot lukemaa:
CQ.set(\coco1Speed, 24000);
//    Osuuko käsi yhtä luotettavasti kuin ennen, vai tuntuuko keskialue nyt
//    liian herkältä? Jos tuntuu: se on vaihtoehto B, ja portti hylätään.
// 5. M1-REGRESSIO. Aja hidas sweep pohjasta kattoon ja takaisin minuutin ajan
//    loopin ollessa lukittuna. Repitchin pitää yhä olla portaaton ja
//    nauhamainen, eikä loopin sisältö saa rapistua matkalla.
// 6. Tee sama coco2:lla (F6). Molempien pitää tuntua samalta.
CQ.set(\coco1Speed, 24000);
CQ.free;
```

- [x] 13.2 hyväksytty (2026-07-29 — äänialueet paikoin äärimmäisiä mutta
      kokeelliseen musiikkiin sopivia; mahdollinen hienosäätö myöhemmin)

### N10 — Kuoren sydän omistaa oman kiekkonsa (§4.5)

Käyttäjän löytämä vika 2026-07-29: hitailla nopeuksilla kelan helmi on
sydämen osuma-alueen sisällä, ja **hover lupasi kelauksen samalla kun
painallus kytki loopin**. Osoitinkysymys kysytään nyt kerran
(`CQNightGeometry.shellTargetAt`), joten vihje ja teko eivät voi enää erota.
Sydän voittaa oman kiekkonsa, koska se on pieni, kiinteä ja seurauksiltaan
raskas; kelaus on kulmaele ja onnistuu koko viisikierroksisen uran matkalta.
Sydän sai myös oman hover-kehän — **piirretty sen todelliseen 22 px
osumasäteeseen**, ei nupin ympärille, jotta vihje kertoo myös missä kela
loppuu.

```supercollider
// N10 — Ctrl+Shift+L ensin. Riittää katsoa ja kokeilla, ei tarvitse ääntä.
CQ.free;
CQ.boot;
CQ.set(\masterVol, -40);
CQ.set(\coco1Speed, 1500);     // helmi kelan sisimpään päähän
// 1. Vie osoitin uralle keskivaiheille: koko spiraali kirkastuu ja lovi
//    kasvaa. Sydämen kehää EI näy. Raahaa: nopeus muuttuu.
// 2. Vie osoitin nyt helmen päälle (aivan keskellä). Nyt pitää näkyä
//    sydämen ohut kehä, ja spiraalin kirkastuksen pitää sammua.
//    Lupaus on siis "sydän", ei "kela".
// 3. Napauta siinä: hold-sinetti vaihtuu — juuri se minkä kehä lupasi.
//    Raahaa napautuksen jälkeen ulospäin: nopeus EI saa muuttua.
// 4. Pidä oikeaa nappia samassa kohdassa: punchin pitää toimia entiseen
//    tapaan, ja irrotuksen palauttaa hold ennalleen.
// 5. Kelaa nopeus takaisin ylös uran ULOMMALTA kierrokselta. Tämä on koko
//    ratkaisun hinta: hitaimmassa päässä kelaus aloitetaan kierros ulompaa.
//    Tuntuuko se luontevalta vai kiusalliselta?
// 6. Vie osoitin suun chevronille (nuoli suujakin alla). Sen pitää nyt
//    paksuntua hiusviivasta normaaliin — chevron oli yön viimeinen
//    painettava kontrolli ilman vihjettä. Väri ei muutu: chevron lepää jo
//    siskomerkkejään kirkkaammassa sävyssä, joten paino on ainoa rehellinen
//    kanava. Napautus kääntää flipin kuten ennen.
// 7. Tee sama coco2:lla. Tarkista myös 1280x570 ja maksimoituna: sydämen
//    kiekko on kiinteä 22 px, ura skaalautuu, joten päällekkäisyys on eri
//    kokoinen eri koossa — kehän pitää silti aina kertoa totuus.
CQ.free;
```

- [ ] N10 hyväksytty (pvm: ______ — vihje ja teko samaa mieltä, sydämen kehä
      luettava, chevronin painovihje luettava, kelaus ulomman kierroksen
      kautta hyväksyttävä)

### 13.5 — VCA-lain korvaportti

Matriisin in/fb-VCA:t summautuivat lineaarisesti; analoginen verso/inverso-VCA
hengittää eksponentiaalisemmin. Modulaation osuus on nyt **muotoiltu**:
`shaped = (1 + sum)² − 1`, summa rajattu ±2 ennen muotoilua.

**Tärkeää: nuppi ei muuttunut.** Muotoilu koskee vain banaanien tuomaa
modulaatiota, ei nupin omaa arvoa — siksi yksikään oletus, snapshot tai
13.1:ssä hyväksytty sirpin asteikko ei siirry, ja banaani osaa yhä avata
kiinni käännetyn VCA:n. Kiinteät pisteet: summa 0 ei tee mitään, −1 sulkee
venttiilin täysin, +1 työntää +3 siinä missä lineaarinen työnsi +1.

**Odotettavissa oleva sivuvaikutus, jonka pitää kuulua heti.** Käyrän
kulmakerroin nollassa on **2**, joten *kaikki* olemassa oleva VCA-modulaatio
noin kaksinkertaistuu syvyydeltään. Tämä on §13.5:n oman lain ominaisuus,
ei toteutuksen valinta — `(1+x)²` nousee kaksi kertaa niin jyrkästi kuin
`1+x`. Käytännössä: `\selfNoise`, `\organMode`, `\mayhem` ja omat snapshotit
hengittävät noin tuplasti entiseen verrattuna, ja vanha paino −0.4 vastaa
suunnilleen entistä −0.8:aa. Vaimennus ei myöskään ole symmetristä:
osuus kasvaa 3× auki-suunnassa mutta vain 1× kiinni-suunnassa (−1 on
kiinteä piste), eli **venttiili aukeaa leveämmin kuin se sulkeutuu**.
Jos tämä on liikaa, dokumentoitu vaihtoehto on p = 1.5.

| summa | ennen (lineaari) | nyt (muotoiltu) |
|---|---|---|
| −1.0 | −1.00 | −1.00 |
| −0.5 | −0.50 | −0.75 |
| −0.1 | −0.10 | −0.19 |
| +0.1 | +0.10 | +0.21 |
| +0.5 | +0.50 | +1.25 |
| +1.0 | +1.00 | +3.00 |

```supercollider
// 13.5 — Ctrl+Shift+L ensin, koska \cqCoco muuttui.
CQ.free;
CQ.boot;
CQ.set(\masterVol, -24);
CQ.patchPreset(\selfNoise);
// 1. \selfNoise hengittää fb-VCA:ita infra-kolmioilla. Vertaa muistikuvaan:
//    aukeaako ja sulkeutuuko kuori nyt VENTTIILINÄ — nopeampi sulku,
//    pyöreämpi avaus — tasaisen liu'un sijaan? Tämä on portin ydinkysymys.
CQ.patchPreset(\empty);
CQ.deckA.load("<rytmikäs äänite>");
CQ.set(\coco1SlotDeck, 1);
CQ.set(\coco1In, 0.8);
CQ.hold(1, 0);
CQ.patch(\cas1, \coco1_inVca, -0.8);
// 2. INVERSO CHOP. cas1:n portaat leikkaavat sisääntuloa. Pureeko chop nyt:
//    pohja aidosti kiinni, avaus muhkea?
CQ.patch(\cas1, \coco1_inVca, -0.4);
//    ...ja tässä on se sivuvaikutus kuultavaksi: puolitettu paino ei puolita
//    vaikutusta, vaan -0.4 kuulostaa suunnilleen siltä miltä -0.8 ennen.
//    Onko se hyvä (banaanit ovat nyt ilmaisuvoimaisempia) vai huono
//    (vanhat patchit ylimoduloituvat ja hienovaraisuus katosi)?
CQ.patch(\cas1, \coco1_inVca, 0.8);
// 3. YLIPAINE. Käännä paino +0.8:aan. Nyt osuus on selvästi kuumempi kuin
//    ennen (+2.24 vs +0.8) — puskeeko se konvertteriin KUULUVASTI mutta
//    hallitusti, vai onko se pelkkää säröä? Jos jälkimmäinen, kokeillaan
//    p = 1.5 tilalla p = 2.
CQ.clearPatch;
CQ.patch(\envA, \coco2_fbVca, -0.5);
CQ.set(\coco2SlotPre, 1);
// 4. DUCK. Soita tai puhu liveen (tai aja deck A:ta preampin läpi):
//    feedbackin pitää väistää ARTIKULOIDEN, ei liukuen.
// 5. TURVATARKISTUS KORVALLA: aja CQ.shake tiheäksi ja nosta patchDepth
//    ykköseen. Tason pitää pysyä kurissa kuten ennenkin — mittaus sanoo
//    että pysyy (mayhem-huippu 0.889 -> 0.890), mutta kuuntele silti.
CQ.clearPatch;
CQ.free;
```

- [ ] 13.5 hyväksytty (pvm: ______ — venttiilin luonne, chopin purenta,
      ylipaineen hallittavuus, duckin artikulaatio) — TAI kirjattu siirtymä
      arvoon p = 1.5

<details>
<summary><b>Superseded N3 v2 — kuu taivaalla ja ellipsilampi pohjassa</b></summary>

Ensimmäinen §9-versio piti kuun, padon ja säärivin yhdessä oikeassa yläkulmassa
(niin kuin suunnitelmateksti sanoi) eikä ratkaissut valikkomaisuutta. Tämä
versio **hajottaa ryppään**: osat eivät ole yhtä asiaa, joten ne eivät ole
yhdessä paikassa. Kukin meni sinne missä se **toimii**:

- **Kuu taivaalle** lintujen korkeudelle, castle 1:n ja linnun B väliin — se
  jonka ali linnut lentävät. Ø 64.
- **Sade ja aurinko kuun seuraksi taivaalle.** Ne kasvattavat ja harventavat
  *elävää lahtea*, eivät lammen muisteja — ne ovat koko lahden sääilmiöitä
  aivan kuten kuun kylvö. Almanakan taivas on sään paikka.
- **Lampi veteen**, lahden pohjukkaan, **tangentiksi alareunaan**. Juuret
  kulkevat jo sen reunan ali, joten reuna lukee vesirajana — mitään ei tarvitse
  piirtää sanomaan että kasvit ja lampi ovat samassa vedessä. Ellipsi 200 × 118,
  siemenillä sahattu ranta joka kurkottaa vesirajan yli.
- **Oksastus jäi lammelle**, koska se ottaa kaksi vanhempaa siemennapautuksina:
  merkki jonka ele päättyy siemeniin ei saa asua lahden toisella puolella.
- **Mikroverso** siirtyi juuri A:n toiselle puolelle (lampi otti sen vanhan
  veden) — kaisla vesirajassa.

**Tila: testi-commit haarassa `nocturne-nightscape`, ei hyväksytty.** `nocturne`
seisoo commitissa `a5e4b17` (kevättulva) eikä sisällä tästä riviäkään. Työ tulee
esiin komennolla `git switch nocturne-nightscape` ja katoaa komennolla
`git switch nocturne`.

**Korjaus aiempaan arviooni koosta.** Sanoin että pohjukka antaisi lammelle
~1.8× padin pinta-alan. Se oli väärin: Qt-kerros on **suorakaide**, joten lampi
saa käyttöönsä suurimman vapaan suorakaiteen eikä kiekkoa, ja pohjukkaa rajaavat
coco 1:n sirpit, joen kerros, envA:n jakki ja vesiraja. Lopputulos 18 535 px²
vastaan G3-padin 19 600 — siirto ostaa **kuulumisen, ei kokoa**. Jos lampi
tuntuu liian pieneltä käyttää, vipu on minimi-ikkunan koko (§15), ei sijainti.

```supercollider
// N3 — Kuu ja lampi. Vaatii GUI:n; Ctrl+Shift+L ennen tätä.
CQ.boot;                       // 1. avaa GUI, valitse toolbarista Night
// 2. Katso KOKO lahtea, älä yhtä kulmaa: onko oikea yläkulma nyt tyhjä vettä?
//    Lukeeko taivas maisemana (lintu — castle — kuu+sää — lintu) ja pohjukka
//    vetenä johon lampi ja juuret kuuluvat? Vai näyttääkö jompikumpi yhä
//    ryppäältä?
// 3. Vedä kuuta VAAKAAN (vaihe täyttyy vasemmalta), napauta = kylvö.
// 4. Pidä sadetta pohjassa taivaalla -> johtoja kasvaa 2.5 Hz. Aurinko harventaa.
CQ.patch(\tri1, \coco1_spaf, 0.55);
CQ.patch(\envB, \castle3_fm, -0.45);
// 5. Napauta lammen YLÄVASENTA siementä: leimaa nykyisen kentän.
CQ.clearPatch;
// 6. Napauta rantaa sen siemenen suunnassa (siemenen ULKOPUOLELLA): kentän
//    pitää palata täsmälleen. Puhdas kulma asuu nyt rannalla, ei neliön
//    nurkassa; kokeile myös hieman rannan ulkopuolelta (kiinnikaista).
// 7. Vedä lammen poikki = matka; oikea nappi = kuminauha.
// 8. Oksastus (lammen oikea alaranta): aseistuu, ottaa kaksi vanhempaa.
```

**Mitä katsoa erityisesti:** (a) onko rypäs oikeasti purkautunut vai vaihtoiko
se vain paikkaa, (b) kuuluuko lampi pohjukkaan vai näyttääkö se pudotetulta,
(c) toimiiko kuun ja sään yhdistelmä taivaalla vai lukeeko sekin ryppäänä,
(d) **kokojen jälkitarkastus koko lahdesta** — kerro mitkä elementit haluat
vielä isommiksi.

**Avoin päätös, jonka mittasin (tärkeä).** Lampi on nyt lahden omassa vedessä,
joten **kaapelit kulkevat sen siementen yli** ja §4.5:n mukaan kaapeli voittaa
painalluksen. Mittasin 60 satunnaisella kentällä: puolen kuun kourallisella
(35 johtoa) oikeanpuoleiset siemenet ovat kaapelin alla **17 % ja 35 %**
kentistä, oksastus 17 %; 70 johdolla 32 % / 48 % / 25 %. Vasemmat siemenet ja
koko taivas (kuu, sade, aurinko) **0 %**. Tämä on raskaampi kauppa kuin
skip-merkin, koska siemenet ovat padin koko käyttöliittymä. Vaihtoehdot:
(a) hyväksy, (b) paikallinen §4.5-poikkeus "lammen rannan sisällä lampi voittaa"
(hinta: lammen yli makaavaa johtoa ei voi repiä juuri siitä kohtaa), tai
(c) lampi takaisin vedelle jonka yli johdot eivät kulje.

- [ ] N3 hyväksytty (pvm: ______ — rypään purku, lammen paikka ja koko,
      taivaan sää, siementen kaapelivarjo, kokojen jälkitarkastus)

</details>
