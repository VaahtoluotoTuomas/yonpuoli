# Manuaalinen kuuntelulista (SPEC M1–M8)

Ihmiskorvatestit — Claude ei kuule ääntä, joten nämä tarkistaa aina käyttäjä.
Lista kasvaa vaiheittain; M4 (`\selfNoise`) ja M7 (abuse-sessio) täydentyvät
vaiheissa 6–7.

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

## M4 — \selfNoise-preset (vaihe 6, tulossa)

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

## M7 — 30 min abuse-sessio (vaihe 7, tulossa)

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

## KÄYTTÖ 5 — Snapshotit & morph (vaihe 5)

```supercollider
// Snapshotit: tilan pitää palata korvakuulolta identtisenä.
// (Lähtötila: deck A soi, jotain patchattuna, esim. CQ.patchPreset(\classic).)
CQ.snap(\a);                   // -> post: "saved 'a' -> ...presets/a.cqsnap.scd"
// 1. Säädä reilusti: speed, fb, pari banaania lisää, CQ.spesal(5) muutaman kerran
CQ.set(\coco1Speed, 8000); CQ.set(\coco1Fb, 1.1);
CQ.patch(\cas3, \coco1_spaf, 0.7);
CQ.snap(\b);                   // talteen myös tämä
CQ.recall(\a);                 // 2. -> tilan pitää palata korvakuulolta
                               //    (speed/fb/banaanit/spesal — kaikki)
CQ.morph(\a, \b, 30);          // 3. 30 s liuku a:sta b:hen: jatkuvien parametrien
                               //    ja painojen pitää liukua MUSIKAALISESTI,
                               //    kytkimet (hold/dolby/spesal) vaihtuvat
                               //    puolivälissä kertarysäyksellä
CQ.snapshots.stopMorph;        //    (tarvittaessa: keskeytys)
CQ.recall(\a);                 // 4. recall kesken morphin = turvallinen

// 5. Mic-politiikka (SPEC §7) — toimii vaikka micChan olisi nil:
CQ.set(\micGain, 0.5);         // -> post-ikkunaan VAROITUS "mic is LIVE"
CQ.snap(\mictesti);            // oletustallennus: micGain talletetaan nollana
CQ.set(\micGain, 0);
CQ.recall(\mictesti);          // -> micGain PYSYY nollassa (ei varoitusta)
CQ.get(\micGain);              // -> 0
```

- [x] Snapshotit + morph hyväksytty (pvm: 2026-07-18 — recall palaa korvakuulolta,
      morph liukuu, keskeytys ok, mic-politiikka ok. Recallin hetkellinen
      repitch-transientti = RAM-kellokäytös, oletettu: puskurin vanha materiaali
      soi uudella kellolla yhden kierroksen ajan.)

## KÄYTTÖ 5 — Deck B, kansionavigointi, linked stereo (vaihe 5)

```supercollider
// Deck B + navigointi. (Coco2 kuuluviin: sen slotDeck lukee deck B:tä.)
CQ.set(\coco2Level, 0.8);
CQ.set(\coco1Level, 0);        // kuunnellaan hetki pelkkää coco2:ta
CQ.deckB.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
CQ.hold(2, 0);                 // -> deck B kiertää coco2:een
CQ.deckB.rate_(-1);            // 1. deck B taaksepäin; F12 liikuttaa molempia
CQ.deckB.rate_(1);

// 2. Kansionavigointi (skannaa ladatun tiedoston kansion; ennen ensimmäistä
//    latausta samples/-kansion):
CQ.deckA.next;                 // -> post: "loaded ..." seuraava tiedosto
CQ.deckA.prev;                 // -> edellinen
CQ.deckA.random;               // -> satunnainen samasta kansiosta
CQ.deckA.loadFolder("samples");// -> kansion ensimmäinen tiedosto

// 3. Linked stereo (SPEC §5.1): stereotiedosto -> A saa L:n, B saa R:n.
//    (Monotiedostolla molemmat saavat saman kanavan + varoitus posiin.)
CQ.set(\coco1Level, 0.8);      // molemmat kuuluviin
CQ.loadStereo("jokin_stereo.wav");
// -> kuuntele: A/B-parin pitää olla saman äänitteen kaksi kanavaa
//    (pan-oletukset -0.3/+0.3 levittävät ne stereokuvaan)
```

- [x] Deck B + navigointi + stereo hyväksytty (pvm: 2026-07-18 — deck B + F12-pari
      ok; navigointi ok korjauksen `56ab8a7` jälkeen (includes-identiteettibugi +
      loadFolder-polku); linked stereo L→A/R→B ok, monon fallback + varoitus ok)
