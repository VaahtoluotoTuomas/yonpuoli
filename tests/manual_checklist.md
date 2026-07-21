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

Osa KUUNTELU 7:ää — ks. tiedoston loppu.

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

---

## KUUNTELU 6 — GUI, shake, presetit, REC (vaihe 6)

```supercollider
// 6.1 — GUI-läpikäynti. (classes/ muuttui -> Ctrl+Shift+L ensin!)
CQ.boot;                       // -> GUI-ikkuna aukeaa itsekseen (headless: false)
CQ.deckA.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
CQ.hold(1, 0);
// Tarkista ikkunasta (mitään koskematta):
// - Master-meterit (out) elävät; coco1:n in/out-meterit elävät, coco2:n in
//   on lähes tyhjä (vain power-on-looppi kiertää).
// - Quantussy: C1..C5-LEDit — infra-castlet sykkivät hitaasti oranssina,
//   C5 (audio) värisee; spesal-näytön 15 pistettä = 5 castlen tila bitteinä.
// - Coco-stripin nupit OVAT oikeissa arvoissa (in 0.8, fb 0.35, ...).
// Ristiinkytkentä (funnel toimii molempiin suuntiin):
CQ.set(\coco1Fb, 1.0);         // -> coco1:n fb-nuppi liikkuu ikkunassa
// - Käännä GUI:n fb-nuppia -> CQ.get(\coco1Fb) seuraa; 16n-fader F4 liikuttaa
//   samaa nuppia (kolme kerrosta, yksi tila).
// - HOLD-nappi: punainen kun päällä; FLIP/SKIP kuuluvat kuten M5:ssä.
// - Matriisigrid: vedä solua pystysuunnassa (oranssi = verso, sininen =
//   inverso), oikea klikkaus irrottaa banaanin; statusrivi näyttää painon.
// - Preset-valikko (gridin ylla): \classic -> gridiin ilmestyvät tutut solut.
// - File drop: raahaa wav Explorerista DECK A -laatikkoon -> latautuu ja
//   tiedostonimi ilmestyy laatikkoon. TOISTA useampi pudotus perakkain
//   molempiin deckeihin, myos nuppien/nappien paalle (koko laatikko kelpaa).
// - Näppäimet ikkunassa: 1/2 hold, q/w flip, a/s skip, 3..7 spesal,
//   r rec, space panic (nappi muuttuu "MUTED"), p shake.
// - Fokus: heti ikkunan avauduttua (mitään klikkaamatta) paina 1 -> hold
//   menee päälle. Klikkaa REC päälle ja pois hiirellä, paina sitten space ->
//   PANIC toimii (space EI togglaa RECiä); TAB ei siirrä valintaa napeille.
//   Kirjoita snap-nimikenttään, paina Enter -> näppäimet taas soittimella.
```

```supercollider
// 6.2 — M5 loppuun GUI:sta: dolby-kytkimet hiirellä. Asetelma GUI:sta:
// deck A ladattu mutta LEVEL-nuppi 0:aan (jatkuva tekstuuri pitäisi
// duck-avaimen ylhäällä — avain kuuntelee koko input-mixiä!), coco1 HOLD
// pois (record-tila), speed täysille (looppi ~2.7 s), master dry ~0.4
// (kuulet puheesi heti), mic + pre A drive niin että puhe pomputtaa
// coco1:n in-meteriä selvästi (duckin syvyys ~ tämä taso).
// - dolby fb -> duck: puhuessasi vanhat kerrokset painuvat alas, tauoissa
//   palaavat; kaiuissa sanat erottuvat puuroutumatta (voi olla hienovarainen)
// - dolby in -> gate: tauoissa mikin taustakohina ei tallennu looppiin;
//   HUOM hiljaisessa huoneessa ero voi jäädä kuulumattomaksi (ei kohinaa
//   gatettavaksi) — se on odotettu tulos, ei vika
// - HOLD/FLIP/SKIP GUI-napeista: sama käytös kuin koodista
// Lopuksi: dolbyt off, mic 0, dry 0, deck level ylös, speed ~24 k
```

```supercollider
// 6.3 — Uudet presetit (kuuntele jokaista ~1-2 min):
CQ.patchPreset(\quantumDrone); // castle-klusteri drone-parina: neljä castlea
                               // audiolla cocojen läpi (coco2 oktaavin alla),
                               // flipit tanssivat vastakkain (cas3 ±),
                               // tri1 resetoi coco2:n fraasin ~12 s välein
CQ.patchPreset(\organMode);    // urkupillit molemmista cocoista (kvintti-
                               // suhde 3:2); soita melodioita F2/F6-fadereilla
                               // (speed = urkujen pitch); cas-askeleet
                               // pumppaavat looppipohjaa urkujen alla,
                               // tri3-vibrato ~3 Hz
CQ.patchPreset(\mayhem);       // tiheä kaaos - VIRITETTY: painot vaihtelevat
                               // (ei enää tasaruudukko); patchDepth (F15 /
                               // masterin depth-nuppi) on kesytysvipu
CQ.set(\patchDepth, 0.3);      //   -> "mayhem 30 %" on jo musiikillista
CQ.set(\patchDepth, 1);
CQ.patchPreset(\empty);        // puhdas pöytä
```

```supercollider
// 6.4 — Shake (SPEC §5.5): p-näppäin tai SHAKE-nappi (amplitudinuppi napin
// vieressä — p-näppäin käyttää samaa arvoa) tai koodi.
// HUOM: shake heittää VAIN painot + spesalit, ei koskaan nuppeja (turva +
// musikaalisuus) -> kuuluvuus riippuu soittimen valmistelusta, kuten
// hardwaressa: banaanit kuuroihin kohteisiin eivät kuulu. Oletusnupeilla
// suurin osa heitosta on hiljaista: spaf-banaanit skaalautuvat cocoNSpaf-
// nupilla (oletus 0!), quantussyn kuulee suoraan vain slotQ:n kautta
// (oletus 0), ja flip/skip/castle-spesal-rivit (11/22 destistä) laukeavat
// vasta kun summa ylittää Schmitt-kynnyksen +0.5 -> amplitudilla 0.3
// yksittäinen banaani (lähteet ~±1) ei laukaise niitä KOSKAAN.
// Valmistele siis pöytä ennen heittoa:
CQ.deckA.load("...");          // materiaalia looppeihin (tai mikki/live)
CQ.set(\coco1Spaf, 0.4);  CQ.set(\coco2Spaf, 0.4);    // spaf-syvyys auki
CQ.set(\coco1SlotQ, 0.8); CQ.set(\coco2SlotQ, 0.8);   // quantussy kuuluviin
                               // (sama GUI:sta: coco-stripin slots-rivi
                               // pre/deck/q/x = sisäänottomikseri)
CQ.shake;                      // ~35 satunnaista banaania ±0.3 + spesal-arvonta
                               // -> gridi näyttää heiton; levelit/fb ennallaan
CQ.shake(0.7);                 // isompi amplitudi -> myös flip/skip/spesal-
                               // rivit voivat herätä (huiput ylittävät +0.5)
CQ.shake(0.4, seed: 1234);     // sama seed -> sama heitto (toistettavissa)
CQ.shake(0.4, seed: 1234);     // -> identtinen edellisen kanssa
// GUI: käännä amplitudinuppi ~0.7, paina p -> isompi heitto samalla arvolla;
// masterin depth-nuppi (patchDepth) kesyttää/palauttaa koko heiton pehmeästi.
CQ.patchPreset(\empty);
```

```supercollider
// 6.5 — REC (SPEC §5.7): nauhoita hetki mitä tahansa yllä olevaa.
CQ.rec;                        // GUI: REC punaiseksi + aika juoksee
                               // -> post: "recording -> ...recordings/cq_..."
CQ.recStop;                    // tai REC-nappi/r-näppäin uudelleen
// -> TARKISTA: recordings/-kansiossa uusi cq_YYMMDD_HHMMSS.wav, 24-bit,
//    sisältö = mitä kaiuttimista kuului (post-safety). Uusi rec = UUSI
//    tiedosto, koskaan ylikirjoitusta.
```

```supercollider
// 6.6 — Headless-varmistus: config.scd -> headless: true, boottaa uudelleen
// -> ei ikkunaa, kaikki toimii koodista + 16n:stä. Palauta headless: false.
// (16n:n puuttuminen on jo todettu M8:ssa: notice, ei virhettä.)
```

- [x] GUI-läpikäynti ok (meterit, LEDit, spesal-näyttö, grid, drop, näppäimet)
      (2026-07-18; matkalla korjattu file-drop- ja näppäimistöfokus-bugit)
- [x] M5 kokonaan (dolbyt GUI:sta) ok (2026-07-18: duck kuultavissa joskin
      hienovarainen; gate ei erotu hiljaisessa huoneessa = odotettu;
      hold/flip/skip GUI:sta ok)
- [x] Presetit \quantumDrone / \organMode / viritetty \mayhem ok (2026-07-18)
- [x] Shake ok (myös seed-toisto) (2026-07-18: valmisteltu pöytä — deck +
      spaf ~0.4 + slotQ ~0.8 — ja isompi amplitudi tekevät heitoista eläviä;
      SPEC-oletus 0.3 pidetään)
- [x] REC ok (tiedosto syntyy, aikaleima, ei ylikirjoitusta) (2026-07-18)
- [x] Headless ok (2026-07-18; matkalla käyttäjän config.scd:hen eksynyt
      CQ.boot-rivi kaatoi latauksen — config on puhdas data-lauseke,
      boot ajetaan boot.scd:stä)
- [x] 30 min sessio pelkillä patch-riveillä + GUI + 16n -> M1-M8 läpi (2026-07-18)
- [x] KUUNTELU 6 hyväksytty (pvm: 2026-07-18) → **M2 valmis**

---

## ARCHIPELAGO G1 — banana grammar + audition

```supercollider
// G1 banana/audition — classes/ muuttui: Ctrl+Shift+L ennen tätä blokkia.
CQ.boot;
CQ.deckA.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
CQ.patchPreset(\empty); CQ.hold(1, 0); CQ.set(\coco1Spaf, 1);
// 1. Paina Quantussyn tri3-oranssia jackia vasemmalla ja vedä hitaasti kohti
//    coco 1:n spaf-nupin alla olevaa violettia socketia. Ennen vapautusta
//    viremodulaation pitää PAISUA etäisyyden neliönä; poistu 40 px säteeltä:
//    audition katoaa täysin. Palaa lähelle ja vapauta: kuulet täsmälleen saman
//    painon, jonka näet istutettuna — ei piilotettua plant-oletusta.
// 2. Tartu istutettuun plugiin/kaapeliin ja siirrä se coco 2 spafiin; vapauta
//    tyhjälle paperille = rip. Ctrl+Z palauttaa koko move/rip-operaation kerralla.
// 3. Kaapelin/socket-stackin päällä: staattinen mustekorostus näyttää täsmälleen
//    kohdistetun kaapelin. Wheel = |w| ±0.05, Ctrl+wheel = ±0.01,
//    Alt+click vaihtaa verso/inverso-polariteetin, oikea click rippaa heti.
//    Oikealla jackista aloittaminen istuttaa suoraan sinisen inverso-banaanin.
// 4. Istuta 3–5 banaania. Aloita oikea drag TYHJÄLTÄ paperilta ja kierrä
//    vapaa lasso Cocon ympäri: punaisella alivalotetut kaapelit irtoavat yhtenä
//    harvest-operaationa; yksi Ctrl+Z palauttaa koko saaliin. Esc kesken lasson
//    peruu mitään muuttamatta.
// 5. Double-click jack/socket/organ body = solo-light (muut kaapelit 25 %).
//    "Organ body" = Coco/Quantussy-organismipaneelin tyhjä taustapinta,
//    ei organ-lähtöjack. Esc vapauttaa solon ja pudottaa kädessä olevan banaanin.
//    Space/PANIC, Tab, ikkunan sulku ja Cmd-. eivät saa koskaan jättää
//    audition-painoa matriisiin.
// 6. Hoveroi potikoita, slidereita, valitsimia, nappeja sekä jack/socket-pisteitä:
//    jokaisen interaktiivisen elementin pitää saada välitön, hillitty staattinen
//    fokus ilman arvon muutosta. Vie osoitin kohtaan, jossa johto ylittää säätimen:
//    säätimen fokus voittaa, kaapelin fokus katoaa ja ele vaikuttaa säätimeen.
//    Tyhjällä paperilla kaapeli voittaa: kohde pysyy täysvahvana, muut kaapelit
//    himmenevät ja haalea mustealleviiva kertoo tarkan click/wheel-kohteen.
```

- [x] G1 banana/audition visuaalinen katselmus hyväksytty (2026-07-20;
      koko G1:n 100/125/150 % loppupyyhkäisy tehdään vielä G1-portissa)
- [x] G1 banana/audition kuuntelu hyväksytty (2026-07-20)

---

## KUUNTELU 7 — space, watchdog, patternit, stemit, M7 abuse (vaihe 7 → v1.0)

Viimeinen kuuntelu-gate. (`classes/` muuttui → **Ctrl+Shift+L** ennen boottia.)

```supercollider
// 7.1 — Space (SPEC §5.6): 4-linjainen FDN-kaiku, oletuksena POIS (spaceMix 0).
// (Lähtötila: deck A soi coco1:n läpi.)
CQ.boot;
CQ.deckA.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
CQ.hold(1, 0);
CQ.set(\coco1Send, 0.6);        // coco1 -> space-send auki (post-fader, pan-sijoitettu)
CQ.set(\spaceMix, 0.4);         // kaiun paluutaso kuuluviin (16n F14 liikuttaa samaa)
// 1. Kaiun pitää ilmestyä (pieni huone/levy, ~30-50 ms viiveet). Kumpikin
//    tarvitaan: ilman coco1Sendiä TAI spaceMixiä = kuiva.
CQ.set(\spaceSize, 0.3);        // 2. lyhyt, tiukka häntä...
CQ.set(\spaceSize, 0.98);       //    ...pitkä häntä (feedback katossa 0.98, ei jää soimaan ikuisesti)
CQ.set(\spaceDamp, 800);        // 3. tumma (vaimennettu ylä)...
CQ.set(\spaceDamp, 12000);      //    ...kirkas
CQ.set(\coco2Send, 0.6);        // 4. molemmat cocot kaikuun (pan -0.3/+0.3 levittää)
CQ.set(\spaceMix, 0);           // lopuksi: kaiku pois (purismi)
```

```supercollider
// 7.2 — Watchdog (SPEC §5.6): kun masteria AJETAAN kovaa yhtäjaksoisesti 5 s ->
// notice (konsoli + GUI). Mittaa kuinka kovaa masteria AJETAAN (driven-mix,
// pre-safety, kynnys -6 dBFS), EI kesytettyä outputtia.
// HUOM: auto-trim (-6 dB) on OPT-IN — config autoTrim oletus on false (mukavuus,
// ei turva; limitteri hoitaa turvan). Oletuksena watchdog vain ILMOITTAA.
CQ.boot;

// A) Notice-polku (varmista riippumatta audiotasosta):
CQ.testWatchdog;   // -> post: "watchdog — driving the master hard..." + GUI-notice
                   //    "OUTPUT HOT (> 5 s)". EI trimmiä (autoTrim false oletus).
CQ.trimDb;         // -> 0 (oletuksena ei trimmata)

// B) Todellinen audiolaukaisu: TIHEÄ jatkuva materiaali (EI harva cricket! se ajaa
//    vain ~-20 dBFS). Tiheä laulu/orkesteri + tiheysnupit + TÄYSI vol:
CQ.deckA.load("91112__krishnanow__seva-sound.wav");   // tiheä laulu
CQ.deckB.load("91112__krishnanow__seva-sound.wav");
CQ.hold(1, 0); CQ.hold(2, 0);
CQ.set(\coco1Fb, 1.25); CQ.set(\coco2Fb, 1.25);
CQ.set(\coco1SlotX, 1.2); CQ.set(\coco2SlotX, 1.2);
CQ.set(\coco1Level, 1.5); CQ.set(\coco2Level, 1.5);
CQ.set(\masterVol, 0);          // TÄYSI vol (driven-mix ~ +5 dBFS mitattuna)
// -> ~5-10 s (loopin täytyttyä) -> notice "OUTPUT HOT" (ei tason muutosta oletuksena)
CQ.set(\masterVol, -12);
CQ.set(\coco1Fb, 0.35); CQ.set(\coco2Fb, 0.35);
CQ.set(\coco1SlotX, 0); CQ.set(\coco2SlotX, 0);
CQ.set(\coco1Level, 0.8); CQ.set(\coco2Level, 0.8);   // oletukset takaisin

// C) (Valinnainen) auto-trim opt-in: config.scd -> autoTrim: true, reboot, toista B.
//    Nyt laukeaminen vetää -6 dB (CQ.trimDb -> -6, ~6 dB pudotus); CQ.resetTrim
//    palauttaa. testWatchdog vetää myös trimin kun autoTrim: true.
```

```supercollider
// 7.3 — Patternit (\cq-eventtityyppi, SPEC §6.4): Pbind sekvensoi diskreettejä
// aktioita tempossa. (Lähtötila: jokin soi, esim. deck A hold 0 + \classic.)
CQ.boot;
CQ.deckA.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
CQ.hold(1, 0); CQ.patchPreset(\classic);
// 1. Spesal-askellus tempossa castleen 3:
p = Pbind(\type, \cq, \action, \spesal, \castle, 3, \dur, Pexprand(0.5, 3)).play;
p.stop;
// 2. Rytminen skip/flip-vuorottelu coco1:lle:
p = Pbind(\type, \cq, \action, Pseq([\skip, \flip], inf), \coco, 1, \dur, 1).play;
p.stop;
// 3. Speed-heitto sekvenssinä:
p = Pbind(\type, \cq, \action, \set, \param, \coco1Speed,
    \value, Prand([6000, 12000, 24000, 48000], inf), \dur, 2).play;
p.stop;
// 4. Deck-navigointi tempossa:
p = Pbind(\type, \cq, \action, \deck, \deck, \a, \do, \next, \dur, 8).play;
p.stop;
// -> aktioiden pitää laueta tarkasti tempossa. \rest / bootaamaton = hiljainen
//    no-op (pattern selviää reboottien yli varoittelematta).
```

```supercollider
// 7.4 — Stem-ulostulot (SPEC §5.7): post-safety coco1/coco2 lisäulostuloihin
// modulaariin. Vaatii >= 4-kanavaisen äänikortin.
// config.scd:hen  stemOuts: [2, 3]  (kanavat coco1=3, coco2=4), boottaa uudelleen.
// 1. Kuuntele modulaarista kanavat 3-4: coco1 kanavassa 3, coco2 kanavassa 4,
//    molemmat post-safety (identtinen limitteri + squelch) = ei koskaan
//    vaarallista tasoa. Master-miksi jatkuu kanavissa 1-2.
// 2. PANIC (space) mykistää myös stemit; boot-fade koskee stemejäkin (turva).
```

```supercollider
// 7.5 — Stem-äänitys (SPEC §5.7): config.scd:hen  recStems: true,  boottaa uudelleen.
CQ.rec;                         // 4-ch: master L/R + coco1 + coco2 -stemit
CQ.recStop;                     // -> post: "recording 4-ch stems -> ...cq_....wav"
// -> TARKISTA: recordings/-kansion wav on 4-kanavainen (Reaper/Audacity);
//    kanavat 3-4 ovat post-safety cocot. recStems: false -> takaisin 2-ch masteriin.
```

```supercollider
// 7.6 — Suorituskyky (SPEC §9): koko instrumentti raskaimmillaan <= 25 % / core
// @ 48 kHz / block 64. (GUI + 16n auki mittauksen aikana.)
CQ.boot;
CQ.deckA.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
CQ.hold(1, 0); CQ.hold(2, 0);
CQ.patchPreset(\mayhem);                          // raskain patch
CQ.set(\coco1SlotQ, 1); CQ.set(\coco2SlotQ, 1);
CQ.set(\coco1Send, 0.5); CQ.set(\coco2Send, 0.5); CQ.set(\spaceMix, 0.4);  // space päällä
CQ.server.avgCPU.round(0.1);   // -> keskimääräinen CPU-% (aja pari kertaa)
CQ.server.peakCPU.round(0.1);  // -> piikki-% (tavoite: reilusti alle 25)
```

```supercollider
// 7.7 — M7 abuse-sessio (SPEC M7): 30 min kuulokkeilla tahallista väärinkäyttöä.
// Kaikki ääripäissä yhtä aikaa — tavoite: MIKÄÄN ei koskaan satu korvia, ei
// tasohyppyjä, ei kipeitä piikkejä (turvaketju + watchdog pitävät kurissa).
CQ.patchPreset(\mayhem); CQ.shake(1.0);           // maksimikaaos
(1..5).do { |k| CQ.set(("c" ++ k ++ "Range").asSymbol, \audio);
             CQ.set(("c" ++ k ++ "Send").asSymbol, 1) };  // quantussy audiolle
CQ.set(\coco1Fb, 1.25); CQ.set(\coco2Fb, 1.25);   // feedback yli unityn
CQ.set(\coco1SlotQ, 1.5); CQ.set(\coco2SlotQ, 1.5);   // quantussy + ristisyöttö sisään
CQ.set(\coco1SlotX, 1.5); CQ.set(\coco2SlotX, 1.5);   // -> aidosti tiheä/kuuma
CQ.set(\coco1Level, 1.5); CQ.set(\coco2Level, 1.5);
CQ.set(\coco1Speed, 3000); CQ.set(\coco2Speed, 48000);
CQ.set(\qChaos, 1); CQ.set(\qFreq, 500);
CQ.set(\coco1Wrap, 1); CQ.set(\coco1Dust, 0.5); CQ.set(\coco2Dust, 0.5);
CQ.set(\spaceMix, 1); CQ.set(\spaceSize, 0.98);   // kaiku täysille
CQ.set(\masterVol, 0);                            // täysi volume
// -> limitteri (-1 dBFS) + watchdog (nyt ~-8 dBFS, laukeaa) pitävät kurissa AINA.
CQ.panic; CQ.resume;                              // panic aina tavoitettavissa
```

- [x] 7.1 Space ok (mix/size/damp; oletus pois) (2026-07-19; reverbin luonne avoin
      suunnittelukysymys)
- [x] 7.2 Watchdog ok (notice oletuksena; auto-trim opt-in `autoTrim: true`) (2026-07-19)
- [x] 7.3 Patternit ok (`\cq`-aktiot tempossa; skip = coco-loop-reset + GUI-välähdys,
      spesal tekstuurinen) (2026-07-19)
- [ ] 7.4 Stemit — **SIIRRETTY**: tarvitsee ≥4-ch äänikortin (NRT-turva ok, live myöh.)
- [ ] 7.5 Stem-äänitys — **SIIRRETTY, sama syy** (4-ch tiedosto, `recStems: true`)
- [x] 7.6 Suorituskyky ok (avgCPU ~20–23 %, peak ~23 % ≤ 25 %) (2026-07-19, laptop)
- [x] 7.7 M7 abuse ok (mikään ei satu ääriasetuksilla, ilman watchdogia) (2026-07-19)
- [ ] KUUNTELU 7 hyväksytty (pvm: ____) → **v1.0-tag** (stemit jäävät live-todennukseen)

## G1 DRAWER — visuaalinen ja touch-and-listen-välitarkastus

```supercollider
// Luokkamuutosten jälkeen ensin Ctrl+Shift+L. Käynnistä sitten täsmälleen tämä tila:
CQ.boot;
CQ.deckA.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
CQ.patchPreset(\empty);
CQ.set(\coco1SlotDeck, 1); CQ.hold(1, 0);
CQ.set(\coco1Spaf, 0.35); CQ.set(\coco1Level, 0.8);

// 1. Avaa Drawer -> Inspector. Paina (älä vielä tuplaklikkaa) solua
//    tri3 -> coco1_spaf: painalluksen aikana pitää kuulua +0.3-modulaation swell,
//    vapautuksessa äänen ja solun pitää palata täsmälleen lähtöön.
// 2. Tuplaklikkaa sama solu: +0.3 jää soimaan. Ctrl+Z irrottaa sen yhdellä kertaa.
//    Vedä solua pystysuunnassa: paino muuttuu hienosti; yksi Ctrl+Z palauttaa koko vedon.
// 3. Avaa Spellbook ja aja seuraava koodikerroksesta. Rivin pitää ilmestyä muodossa
//    CQ.patch(\envA, \coco1_inVca, -0.4), ja copy line -napin liittää sama teksti.
CQ.patch(\envA, \coco1_inVca, -0.4);

// 4. Inspector-headerista apply preset -> classic: yksi // preset -blokki ilmestyy
//    Spellbookiin ja yksi Ctrl+Z palauttaa koko aiemman matriisin. Memory-välilehdellä
//    pitää näkyä snapshot save/recall sekä A -> B morph ennallaan.
// 5. Tiheystesti: apply preset -> mayhem. Kokeile Day-säätimiä kaapelien alta sekä
//    endpointteja. Tämä hyväksyttiin 2026-07-20: controls-first + endpointit riittävät,
//    eikä cable opacity -säädintä lisätä.
// 6. Workbench + patch loom -kokeilu: sama tab avaa koko keskialueen workbenchin ja sulkee
//    sen takaisin Day/Night-soittimeen; Master pysyy näkyvänä, resizea tai
//    instrumenttiscrollia ei ole.
//    Inspectorin koko 16x22-kentän pitää näkyä ilman sisäscrollia noin 48x24 px soluina.
//    Kolumnit ryhmittyvät coco 1 / coco 2 / castle fm / castle spesal / decks -saariin,
//    child-labelit pysyvät diagonaalisina. Tyhjät risteykset ovat hiljaisia pisteitä ja
//    patchit painon/polariteetin näyttäviä solmuja. Hover korostaa yhden rivin+kolumnin;
//    klikattu solu jää rajatuksi ja tarkka reitti näkyy kiinteässä yläpalkissa.
//    Tarkista myös Spellbookin pysyvä rivivalinta ja Memoryn snapshot/morph-jako.
// 7. Viimeistelykierros: `drawer` pysyy aina samalla nimellä Nightin vieressä; nuoli alas
//    avaa ja nuoli ylös sulkee. Day/Night-valinta palauttaa soittimeen. Inspectorin route-
//    tieto, presetit, matriisi ja leikkaamaton gesture-footer ovat samalla 1128 px raililla,
//    ja ryhmäotsikot tuntuvat kuuluvan diagonaalilabeleihin. Spellbookin kaikki rivit ovat
//    luettavia myös ilman valintaa. Memory näyttää live patch -sormenjäljen, tallennetun
//    snapshotin vertailun ja A→B morph bridgen; snapshotin valinta ei vielä recallaa sitä.
// 8. Muistisanasto (päätös 2026-07-21): GUI:ssa on vain kaksi muistisanaa. Inspectorin
//    header puhuu preseteistä (koodin patch-reseptit), Memory snapshoteista
//    ("save snapshot", "saved snapshots"); sana "seed" saa esiintyä enää sivun
//    otsikkometaforassa "memory · seed bank", ei yhdenkään kohteen niminä.
CQ.patchPreset(\empty);   // lopuksi turvallinen, selkeä lähtötila
```

```supercollider
// 9. Qt-jäätymisregressio (korjattu 2026-07-21 — tämä testi vartioi sitä).
//    Aiemmin näkymän piilotus hiiren osoittimen alta lukitsi koko ikkunan.
CQ.boot;
// a) Jätä hiiri paikalleen Coco 1:n paneelin päälle (älä liikuta) ja paina Tab.
//    -> Night-näkymän pitää vaihtua heti; ikkuna EI saa jäätyä ("Not Responding").
// b) Paina Tab uudelleen samasta kohdasta -> takaisin Dayhin, yhä responsiivinen.
// c) Toista sama hiiri Inspectorin loom-kentän päällä: avaa Drawer toolbarista,
//    jätä osoitin solujen päälle ja vaihda sivua inspector -> spellbook -> memory
//    sekä Day/Night-napista. Mikään vaihto ei saa jäätyä eikä jäädä väärään sivuun.
// d) Vaihda näkymää ~20 kertaa nopeasti hiiri paneelin päällä -> ikkuna pysyy
//    responsiivisena ja lopputila vastaa viimeistä painallusta.
```

- [x] G1 Drawer behavior/touch 1–5 hyväksytty (2026-07-20; `cable opacity`: ei tarvita)
- [x] G1 Drawer workbench + patch loom -kokeilu 6 hyväksytty (2026-07-20)
- [x] G1 Drawer toolbar/alignment/Spellbook/seed-bank-viimeistely 7 hyväksytty
      (2026-07-21)
- [ ] G1 muistisanasto 8 tarkistettu (pvm: ____ — sanat vaihtuivat päätöksen
      jälkeen; silmäile Memory-sivu seuraavalla käynnistyksellä)
- [x] G1 Qt-jäätymisregressio 9 tarkistettu (2026-07-21; Tab hiiri Coco 1:n
      päällä ei enää jäädytä ikkunaa)
- [ ] G1 Drawer myöhempi soitettavuus- ja kuunteluportti hyväksytty (pvm: ____)
