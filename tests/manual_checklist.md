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
CQ.patchPreset(\selfNoise);    // sulkee ulkoiset slotit, hold 0, fb alle unityn,
                               // dust ylös, täysin sulkeutuva ristisyöttö,
                               // held-cas feedback-sää + vastakkainen spaf-drift
// 1. Kuuntele 120 s ilman inputtia: rakeinen dust-pohja rakentaa bloomin,
//    held-feedback purkaa sen oikeaksi clearingiksi ja kasvattaa uuden.
//    Tapahtumien ei pidä toistua säännöllisen meren rytmissä.
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

## Suljettu laitteistogate (aiemmilta vaiheilta)

- [x] **Stemien live-kuuntelu** (KUUNTELU 7, kohdat 7.4/7.5 — arkistossa):
  vaadittu ≥ nelikanavainen äänikortti saatiin AIO-4:n fyysisessä
  katselmuksessa. **Suljettu 2026-07-30:** K-Mixin fyysisistä ulostuloista 3/4
  kuului stem 1/2 mainista erillään, vain Coco1 avasi 3:n ja vain Coco2 4:n, ja
  PANIC hiljensi molemmat. NRT-turva oli jo vihreä; live-todennus on nyt tehty.
  Ks. AIO-4-lohko tämän tiedoston lopussa.

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

### 13.3 — Digital dustin korvaportti

Kandidaatti korvaa jatkuvan `WhiteNoise × LFNoise2` -maton kahdella kiinteällä
kerroksella: hiljainen 9 kHz:iin pehmennetty pohja ja bipolaarinen chip-crackle.
Uusia parametreja tai snapshot-semanttiikkaa ei ole. Tee testit kuulokkeilla tai
hyvillä monitoreilla hiljaisella mutta yksityiskohdat paljastavalla tasolla;
aloita aina `masterVol = -30`, sillä selfNoise ja maksimi-dust kasvavat nopeasti.

#### A. Tarkka vanha/uusi A/B ilman patchin muita muuttujia

Aja ensin:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_dust_character.scd
```

Se luo `tests/tmp/nrt_dust_1500.wav`, `nrt_dust_24000.wav` ja
`nrt_dust_48000.wav`. Tuo yksi kerrallaan DAWiin, split stereo to mono ja keskitä
molemmat raidat: **vasen = uusi**, **oikea = vanha**. Älä normalisoi, kompressoi
tai tasaa gainia. Looppa viimeiset kolme sekuntia (5–8 s) ja solo-vaihda raitoja
5–10 sekunnin välein.

1. Aloita 24 kHz:stä. Uuden pitää olla vähemmän yhtenäinen kohinaverho ja enemmän
   pieniä, epäsäännöllisiä tapahtumia. Se saa tuntua hieman hiljaisemmalta, mutta
   ei tyhjältä.
2. 1,5 kHz:ssä yksittäisten rakeiden ja nollan väliin pitää jäädä ilmaa. Hylkää,
   jos tulos on harva napsutesti, tasainen bitcrush-surina tai bassoinen popitus.
3. 48 kHz:ssä rakeet saavat sulautua kuivaksi kihinäksi, mutta ylä-ääni ei saa
   muuttua valkoiseksi hissiksi, lasiseksi sirinäksi tai korvaa väsyttäväksi
   neulaksi.
4. Palaa 24 kHz:iin ja gain-matchaa uusi vain vertailun vuoksi noin +4 dB.
   Karakterieron pitää säilyä. Palauta sitten gain nollaan: varsinainen
   hyväksyntä tehdään ilman loudness-kompensaatiota.

#### B. Sama materiaali koko Speed-kaistan läpi

```supercollider
// Ctrl+Shift+L ensin: SynthDef muuttui.
CQ.free;
CQ.boot;
CQ.patchPreset(\empty);
CQ.set(\masterVol, -30);
// Eristä Coco 1 ja nollaa kaikki muu, myös aiempien presetien residuaalit.
CQ.set(\coco2Level, 0);
CQ.set(\coco1SlotPre, 0); CQ.set(\coco1SlotDeck, 0);
CQ.set(\coco1SlotQ, 0);   CQ.set(\coco1SlotX, 0);
CQ.set(\coco1In, 0);      CQ.set(\coco1Fb, 0);
CQ.set(\coco1Organ, 0);   CQ.set(\coco1Bits, 8);
CQ.set(\coco1Wrap, 0);    CQ.set(\coco1Interp, \zoh);
CQ.set(\coco1Level, 1);   CQ.set(\coco1Dust, 0.08);
CQ.set(\coco1Speed, 48000);
CQ.hold(1, 0);            // kirjoita pölyä täydellä kellolla 4 s
// odota 4 s
CQ.hold(1, 1);            // jäädytä täsmälleen sama materiaali
```

Käy sitten `coco1Speed`-arvot 1500 → 3000 → 6000 → 12000 → 24000 → 48000 ja
takaisin. Viivy jokaisessa 8–12 s.

- 1500–3000 Hz: erillinen, tumma rapina; rouhea mutta ei rikkinäinen liitin.
- 6000–12000 Hz: soramainen väli, jonka pitäisi olla erityisen käyttökelpoinen
  rytmittömissä droneissa ja hiljaisissa kenttä-äänissä.
- 24000 Hz: kuiva chip-pöly, joka antaa reunaa viemättä lähteeltä syvyyttä.
- 48000 Hz: tiheä mutta ohut kihinä; ei leveää hissimattoa eikä kivuliasta
  yläpäätä.
- Vaihtojen pitää olla yhtä materiaalia eri nopeuksilla, ei neljä irrallista
  efektiä. Mitään sävelkorkeuteen lukittuvaa vihellystä ei saa ilmaantua.

Lähteen hitaalla kellolla kirjoittuvan jakauman voi varmistaa asettamalla
lopuksi `hold = 0`, `speed = 1500` ja odottamalla yhden täyden oletusbufferin
kierroksen (~87,4 s). Sen jälkeen lukitse hold uudelleen. Suoraan 1,5 kHz:llä
kirjoitetun ja ensin 48 kHz:llä kirjoitetun mutta 1,5 kHz:llä toistetun
materiaalin kuuluukin olla lähes yhtä tiheää: molemmissa noin 2 % RAM-soluista
kantaa crackle-tapahtumaa. Nopeusero kuuluu, kun samaa bufferia **toistetaan**
eri nopeuksilla; tallennusnopeuden ei pidä muuttaa solukohtaista jakaumaa.

#### C. Oletus-dust oikean ohjelmamateriaalin sisällä

```supercollider
CQ.free;
CQ.boot;
CQ.patchPreset(\classic);
CQ.set(\masterVol, -24);
CQ.deckA.load("<hiljainen puhe, piano tai yksityiskohtainen kenttä-ääni>");
CQ.set(\coco1SlotDeck, 1); CQ.set(\coco1In, 0.8);
CQ.set(\coco1Fb, 0.85);    CQ.set(\coco1Speed, 24000);
CQ.set(\coco1Bits, 8);     CQ.set(\coco1Dust, 0.02);
CQ.hold(1, 0);
```

Kuuntele vähintään 30 s ja vaihda dust 0 ↔ 0.02 noin 10 s jaksoissa; anna aina
vähintään yksi looppi muutoksen jälkeen. Oletuksen pitää karhentaa hiljaisia
häntiä ja nollakohtia, mutta puheen konsonanttien, pianon atakin ja
kenttä-äänen etäisyyden pitää säilyä. Hylkää, jos stereokuva kutistuu, hiljaiset
yksityiskohdat peittyvät tai kokonaisuus vain kuulostaa gainilta.

Toista dust-arvoilla 0.08 ja 0.20:

- 0.08: crackle saa erottua, mutta ei varastaa ohjelmamateriaalia.
- 0.20: kohina on jo oma selvä ääni; säätimen käyttömaa ei saa loppua tähän.

##### 13.3b — Bits-eristys (myöhemmäksi kirjattu)

Ensimmäinen 5/8/12-bitin vertailu jäi ohjelmamateriaalin ja bufferiin jääneen
aiemman sisällön peittämäksi. Tee myöhemmin oma testi yhdellä identtisellä,
hiljaisella loopilla: `dust = 0.08`, muut tulot ja feedback pois, kirjoita
jokaisella bittisyvyydellä vähintään kolme täyttä kierrosta ja lukitse Hold
ennen A/B:tä. Tämä ei estä hyväksyttyä dust-algoritmia.

- [ ] 13.3b Bits 5/8/12 eristetty korvatesti (pvm: ______)

#### D. `selfNoise` — varsinainen hyväksyntäkorva

```supercollider
CQ.free;
CQ.boot;                    // fresh state: presetit ovat tarkoituksella overlay
CQ.set(\masterVol, -30);
CQ.patchPreset(\selfNoise);
```

1. Anna ekologian elää 120 s koskematta mihinkään. Sen pitää rakentaa vähintään
   yksi selvä bloom, tyhjentyä kuultavasti ja rakentaa myöhemmin uusi bloom.
   Ajankohtien ei pidä toistua merenaallon säännöllisyydellä. Hylkää, jos taso
   vain kasvaa, kuolee pois tai asettuu yhdeksi jatkuvaksi kohinamassaksi.
2. Nosta masteria vain tarpeen mukaan enintään −18 dB:iin. Kuuntele erityisesti
   hiljaisten aukkojen reunoja: niissä pitää olla pientä digitaalista roskaa,
   ei gate-naksua.
3. Lukitse molemmat: `CQ.hold(1, 1); CQ.hold(2, 1);`. Tekstuurin pitää jäädä
   kiinnostavaksi ilman uusia virheitä. Kelaa kumpaakin erikseen 1500, 6000,
   18000/24000 ja 48000 Hz: hitaassa päässä kaunis rouheus, nopeassa kuiva
   sizzle — ei piercing-hissiä.
4. Avaa holdit ja kokeile lyhyesti `coco1Dust = 0.5`, `coco2Dust = 0.5`
   masterin ollessa −30 dB. Maksimi saa olla väkivaltainen ja kyllästyvä, mutta
   yksittäiset piikit eivät saa hypätä muun tekstuurin eteen eikä taso saa
   karata kierros kierrokselta.
5. Palauta `selfNoise`, soita sen päälle yksi oma tuttu patchi ja arvioi viimeinen
   asia: onko pölyllä oma kaunis, rouhea identiteetti, mutta jättääkö se silti
   tilaa patchin rytmille, korkeuksille ja dynamiikalle?

Dust-lähteen korvaportti on valmis; tämä viimeinen D-osuus hyväksyy erikseen
uudelleen viritetyn presetin.

- [x] 13.3 dust-algoritmi hyväksytty (2026-07-29 — uusi A/B:ssa
      miellyttävämpi ja hengittävämpi; koko Speed-kaista samaa materiaalia;
      oletus ei peitä ohjelmaa; maksimi turvallinen; oma patch toimii)
- [x] M4b selfNoise broken-weather -preset hyväksytty (2026-07-29 —
      bloom → clearing → rebloom on vanhaa meriaalto-versiota kiinnostavampi;
      preset toimii musiikillisesti paremmin eikä asetu rail-massaksi)

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

- [x] N10 hyväksytty (pvm: 2026-07-29 — vihje ja teko samaa mieltä, sydämen
      22 px kehä ja chevronin painovihje luettavat, kelaus ulomman kierroksen
      kautta hyväksyttävä)

### 13.5 — VCA-lain korvaportti

Korvaportti tehtiin saman session A/B/C:nä kolmella lailla: vanha lineaarinen,
etumerkillinen p = 1,5 ja etumerkillinen p = 2. Tilapäinen 30 ms
ristihäivytetty kuuntelukytkin oli vain mittalaite; hyväksynnän jälkeen se ja
p = 1,5 -haara poistettiin tuotanto-DSP:stä. Lopullinen laki on yksi kiinteä
**p = 2**, ei uusi GUI-, API- tai snapshot-parametri.

**Mitä laki todella tekee.** Nuppi säilyttää lepoasentonsa ja suljettu VCA
voidaan yhä avata banaanilla. Sen sijaan VCA:ta moduloivia snapshotteja ja
presettejä ei pidetä musiikillisesti ennallaan: niiden painot saavat uuden
vasteen. p = 2:n kaksinkertainen pienen signaalin kulmakerroin on seuraus
valitusta eksponentista, mutta p = 2:n valitseminen on nimenomaan tämän portin
virityspäätös.

Alla olevat luvut ovat modulaation osuuksia **ennen** todellisen VCA:n kattoa.
Input-gain rajataan lopuksi 0–2:een ja feedback-gain 0–2,5:een; siksi summa +1
ei toteudu ×4/+12 dB gainina. p = 2 saavuttaa katon aikaisemmin ja voi viipyä
siellä pidempään, mutta kuuntelussa kaari ei jähmettynyt staattiseksi tasanteeksi.

| summa | ennen (lineaari) | nyt (muotoiltu) |
|---|---|---|
| −1.0 | −1.00 | −1.00 |
| −0.5 | −0.50 | −0.75 |
| −0.1 | −0.10 | −0.19 |
| +0.1 | +0.10 | +0.21 |
| +0.5 | +0.50 | +1.25 |
| +1.0 | +1.00 | +3.00 |

**Kuuntelutulos 2026-07-29:**

- p = 2 säilytti painoilla 0,05 ja 0,10 hienovaraisen hengityksen; vasta
  noin 0,20–0,30 alkoi lukea selvänä modulaationa.
- Inverso-chop −0,20 teki fraaseja ilman hiljaisuuksia. −0,40 kävi hyvin
  matalalla mutta säilytti kulmikkaassa vaihdossa pyöreän reunan; −0,80 teki
  vasta tarkoitukselliset täydet aukot. Painot eivät romahtaneet samaksi
  on/off-eleeksi.
- Positiivinen paine muodosti muhkean kaaren, kävi vain lyhyesti katossa ja
  lähti takaisin alas ilman staattista tasannetta.
- Feedback-duck −0,20 teki kuoppia ilman aukkoja; −0,50 syvensi huojuntaa
  mutta ei pyyhkinyt loopista tyhjiä paloja.
- SP.AF-verrokki pysyi samana, kuten pitää: §13.5 ei koske virettä/nopeutta.
  `\selfNoise`, `\organMode`, oma patch ja kokonaissoitto hyväksyttiin.

p = 1,5 ei saanut riittävän omaa identiteettiä p = 2:n rinnalla; saman
käyttömaan saa VCA-kaapelin painolla. Lineaarisella lailla oli oma liukuva
identiteettinsä, mutta testeissä ei löytynyt tehtävää, joka oikeuttaisi uuden
character-kytkimen, snapshot-tilan ja GUI-kielen. Jos myöhempi säveltäminen
toistuvasti tarvitsee p = 2:lla saavuttamattoman suoran, symmetrisen swellin,
asia voidaan avata uudelleen kaksiasentoisena `slide/valve`-kysymyksenä ilman
p = 1,5:tä.

- [x] 13.5 hyväksytty (pvm: 2026-07-29 — kiinteä etumerkillinen p = 2:
      hienovarainen 0,05–0,10, fraseeraava keskialue, täydet aukot vasta
      korkealla painolla, hallittu positiivinen kaari ja feedback-duck;
      ei character-kytkintä)

### 13.6 — Matriisiliu’un zipper-portti

Mittaus käyttää toteutuksen todellista lakia. Vanha suunnitelmateksti kutsui
weight-busseja lagittomiksi, mutta `CQSynthDefs.prMatrixWeight` on silottanut
kaikkia kohteita 50 ms:llä M1:stä asti. Portti vertaa tuotannon 30 Hz + 50 ms
-polkua yhtä lailla silotettuun jatkuvaan ramppiin ja lagittomaan positiiviseen
kontrolliin:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_zipper_measure.scd
```

Odotettu markkeri: `CQ_ZIPPER_MEASURED`. Kuuntelutiedostot syntyvät:

- `tests/tmp/zipper_current_30hz.wav`
- `tests/tmp/zipper_reference_continuous.wav`
- `tests/tmp/zipper_unlagged_positive_control.wav`
- `tests/tmp/zipper_unlagged_reference.wav`

Mittaus 2026-07-29: nykyisen polun residual RMS oli −55,87 dB suhteessa
signaaliin; lagiton kontrolli −55,15 dB. Numerot eivät ratkaise korvaporttia.
Kuuntele ensin nykyinen vastaan jatkuva; käytä lagitonta paria vain etsimäsi
portaikon opetteluun.

- [x] 13.6 hyväksytty (pvm: 2026-07-29 — zipperiä ei erottunut
      luotettavasti ja se on todennäköisesti kuulumaton; ei DSP-muutosta)
- [ ] 13.6 tarvitsee korjauksen (havainto: ________________________________)

Jos korjaus tarvitaan, älä lisää uutta blanket-lagia. Kokeile ensin vain
jaettujen solujen client-glidea; serveriratkaisussa nykyinen helper korvataan
continuous-only-polulla. Flip/skip/spesal eivät saa periä uutta lagia.
Nykyinen historiallinen 50 ms helper osuu myös niiden Schmitt-summiin, joten
sen mahdollinen poistaminen on oma porttiajoituskoe, ei huomaamaton sivumuutos.

### 13.8 — Preampin equal-loudness-koe

Labra tekee samasta peak-tasatusta puheesta, rumpuloopista ja sustain-urusta
kolme versiota: vanha kompensoimaton polku, tuotantoon valittu −1 dB per
todellinen Drive-oktaavi ja hylätty vahvempi −1,5 dB/okt-verrokki.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_preamp_loudness.scd
```

Odotettu markkeri: `CQ_PREAMP_OK`. Jokaisessa
`tests/tmp/preamp_<materiaali>_<versio>.wav`-tiedostossa sama 2,5 s katkelma
toistuu järjestyksessä Drive 1/2/6/16/40, Bias 0,15; lopuksi Bias
0/0,15/0,6, Drive 12. Kuuntele:

1. Peittääkö vanhan kompensoimattoman Driven tasohyppy itse särön eleen?
2. Säilyttääkö ehdokas perkussion atakin ja sustainin paineen?
3. Pysyykö puhe läsnä vai vetäytyykö se Driven kasvaessa oudosti?
4. Onko bias-kolmikko musikaalisesti sama ele kaikissa versioissa?
5. Vasta tiedostovalinnan jälkeen kokeile voittajaa live-inputilla.

Drive 40:n mitattu RMS-nousu Drive 1:een nähden oli vanhalla kompensoimattomalla
+21,67/+22,17/+16,09 dB (puhe/rummut/urut), −1 dB/okt-versiolla
+16,34/+16,84/+10,77 dB ja −1,5 dB/okt-versiolla
+13,68/+14,18/+8,11 dB. Vahvempikin ehdokas jättää siis tarkoituksella paljon
painetta; kumpikaan ei ole automaattinen loudness-normalisoija.

- [x] 13.8 tiedostovalinta hyväksytty (pvm: 2026-07-29 — −1 dB/okt;
      vanha polku kasvattaa äänenvoimakkuutta liikaa, valittu välimuoto
      hillitsee karkaamista tasapäistämättä särön painetta)
- [x] 13.8 valitun tuotantopolun live-input-vahvistus
      (pvm: 2026-07-29 — vaihe hyväksytty)
- [ ] 13.8 hylätty/uusi hienoviritys tarvitaan: ___________________________

### AIO-1 — Audio I/O -sivun staattinen portti

AIO-1 on tarkoituksella vain luku -versio. `refresh devices` saa päivittää
exact laitelistauksen, mutta `discard changes` ja `apply & restart audio` ovat
inerttejä, kunnes tämä sommittelu on hyväksytty. Tässä portissa ei kirjoiteta
`config.scd`:tä tai `.audio.scd`:tä eikä restartata serveriä.

Koneelliset portit ja katselukuvat:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/compile_check.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/run_all.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_audio_setup_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_drawer_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_shell_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_drawer_preview_reentry_smoke.scd
```

Odotetut markkerit: `CQ_COMPILE_OK`, `CQ_TESTS_PASSED`,
`CQ_GUI_AUDIO_PASSED`, `CQ_GUI_DRAWER_PASSED`. Audio-smoke kirjoittaa:

- `tests/tmp/audio-setup-compact.png`
- `tests/tmp/audio-setup-roomy.png`

Live-katselmus:

```supercollider
// Ctrl+Shift+L ensin, koska classes/ muuttui.
CQ.boot;
// Avaa Drawer → audio. Sivu näyttää nykyisen aktiivisen configin.
// Kokeile refresh devices ja avaa mode/device-listat; valinta palautuu
// AIO-1:ssä active-arvoon eikä muuta ääntä tai configia.
```

Katso compactissa ja roomyssa:

1. Lukeeko sivu rauhallisena rantaporttina eikä yleisenä Preferences-ikkunana:
   host/engine ylhäällä, kaksi yhtä vahvaa I/O-saarta keskellä ja restart gate
   alhaalla?
2. Ovatko device-mode ja exact device -rivit löydettävät ilman että ne
   hallitsevat koko sivua? Säilyttääkö pitkä nimi alkunsa ja päättyykö se
   ellipsiin?
3. Pysyvätkö `mic off` sekä `stems off`, stem 1 ja stem 2 paikoillaan himmeinä
   ilman layout-hyppyä?
4. Ovatko `refresh devices`, `discard changes` ja
   `apply & restart audio` oikeassa tärkeysjärjestyksessä? AIO-1:ssä vain
   refresh saa toimia.
5. Kasvaako roomy-versio luettavasti samalla raililla ilman että painikkeet,
   rivit tai oikea reuna leikkautuvat?

- [x] **AIO-1:n staattinen sommittelu hyväksytty**
      (pvm: 2026-07-29 — compact/roomy, kolmen vyöhykkeen hierarkia,
      device-menut, mic/stem-off-vakaus, restart-gaten tärkeysjärjestys;
      hyväksynnän huomioina refresh oli liian oikealla ja pitkät popup-nimet
      kietoutuivat kaksirivisiksi yksirivisen highlightin alla)
- [ ] AIO-1 vaatii uuden visuaalisen kierroksen
      (havainto: ei — refresh kohdistettiin samaan oikeaan railiin; popupit
      ellipsoivat ennen piirtoa ja näyttävät enintään 16 vieritettävää riviä)

### AIO-2 — draft ja atominen audio-overlay

AIO-2:n kaikki kenttävalinnat koskevat vain `CQAudioConfig.draft`ia.
`discard changes` palauttaa activen. Tässä historiallisessa portissa
`apply & restart audio` oli vielä rakenteellisesti lukittu AIO-3:een.
Injektoitu laitelista lukee erilliset
`ServerOptions.inDevices/outDevices`-snapshotit, exact-dedupplikoi ne ja näyttää
single-tilassa vain niiden exact-intersectionin. Se ei päättele kanavamääriä tai
host APIa nimestä.

Koneellinen näyttö:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/compile_check.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/run_all.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_audio_setup_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_drawer_smoke.scd
```

- [x] Refreshin oikea reuna = muiden host-kontrollien oikea rail
- [x] Pitkä device String pysyy yhden rivin tekstinä ja yhden rivin highlightina
- [x] Windows-endpointin sisäiset CR/LF/tab-merkit tiivistyvät vain
      näyttötekstissä; exact malliarvo ei muutu
- [x] Exact-duplikaatit poistuvat; input-only/output-only eivät vuoda väärään
      suuntaan; single-lista sisältää vain molempiin suuntiin sopivat nimet
- [x] Kaikki yhdeksän startup-kenttää kirjoittavat vain draftiin
- [x] Invalid main/stem-kanava jää näkyviin tarkan syyn kanssa; ei clampia
- [x] Discard palauttaa activen; Apply ei vielä kirjoita tai restarttaa
- [x] Pending roundtrip, base-byte-immutability, atomic replace, yksi `.bak`
      ja retryable `notSaved` on todistettu `tests/tmp`-poluilla

### AIO-3 — vahvistettu restart ja rollback

Ensimmäinen `apply & restart audio` avaa vain saman railin inline-
vahvistuksen. `restart` käynnistää facaden data-only-transaktion; vanhan sivun
Qt-viitettä ei säilytetä. Candidate saa korvata last-working-profiilin vasta,
kun koko uusi sessio on `running`. Epäonnistuminen tekee täsmälleen yhden
rollbackin.

Koneellinen näyttö:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/compile_check.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/run_all.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_safety.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_hold_integrity.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_audio_setup_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_drawer_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_night_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_night_phase_layer_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_shell_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_drawer_preview_reentry_smoke.scd
```

- [x] Clean/invalid/non-running draft ei kirjoita pendingiä eikä reboottia
- [x] Aktiivinen recorder näyttää `stop REC before restarting audio`
- [x] REC-stop poistaa varoituksen heti ilman kenttämuutosta, palauttaa Applyn
      ja disabled Apply ei piirrä hover-highlightia (split-WASAPI 2026-07-30)
- [x] Ensimmäinen Apply vain vahvistaa; cancel säilyttää draftin
- [x] Candidate success hyväksyy capturoidun datan ja promottaa vasta readyssä
- [x] Candidate fail poistaa pendingin, säilyttää draftin ja tekee yhden rollbackin
- [x] Rollback fail päättyy hiljaiseen `failed`-tilaan ilman kolmatta yritystä
- [x] Stale callback ei muuta activea, profiilia eikä Audio-tabin tilaa
- [x] `running · not saved` voidaan tallentaa uudelleen ilman reboottia
- [x] Uusi GUI kuluttaa symbolisen `\audio`-reopenin vain kerran
- [x] Fyysinen candidate-roundtrip (split-WASAPI 2026-07-29): server block
      64→128→64 säilyy exact overlay-arvona, GUI/ääni/Night palautuvat
      kummallakin restartilla ja lopullinen serveri vastaa `/status`-kyselyihin
- [x] Fyysinen Discard/Cancel (2026-07-29): Discard palauttaa draftin ilman
      kirjoitusta/restartia; confirmation Cancel säilyttää draftin mutta peruu
      restartin. PID ja persisted block 64 pysyvät muuttumattomina

### AIO-4 — signaalipresence ja fyysinen hardware-I/O-portti

Koneellinen osa on valmis. `into yölahti` näyttää aktiivisten server-kanavien
raakasignaalin ennen live/mic-gainia ja preamp-valintaa; `muted/live` kertoo
erikseen gainin tilan. Oikean saaren otsikon master L/R tulee olemassa olevasta
post-safety-mittarista. Raw-tap palautettiin, kun kontrolloitu baseline toisti
WASAPI-jumin ilman sitä ja runtime-laitelistauksen poisto palautti äänen,
Night-liikkeen ja `/status`-vastaukset. Presence ei todista draft-laitetta eikä
fyysistä kaapelointia ennen restartia ja kuuntelua.

Running Audio -sivu käyttää vain ennen bootia tai rebootin stopped-callbackissa
otettua muistikuvaa. `show all drivers` vaihtaa paikallisesti recommended/all-
näkymää eikä lue OS:ää uudelleen; aktiivinen/draft exact endpoint säilyy aina.
Ilman turvallista snapshotia kontrolli näyttää disabled `configured devices`.
Käynnissä oleva Windows WASAPI -palvelin ei saa enää millään UI-polulla
`_ListAudioDevices`-kutsua.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/compile_check.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/run_all.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_audio_setup_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_audio_presence_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_drawer_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/gui_shell_smoke.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_safety.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_hold_integrity.scd
```

- [x] Piilotettu Audio-sivu cachettaa telemetrian ilman callbackia/painttia
- [x] Avattu Audio-sivu saa viimeisen cachen heti
- [x] Yhdistetty 40 Hz testivirta tuottaa enintään 15 GUI-paint-batchia/s
- [x] Sub-pixel-tasonmuutos ei repainttaa
- [x] Free poistaa Audio-listenerin, `/cq_io_inputs`- ja master-OSCdefit
- [x] Puhdas draft näyttää `no changes` ja aktiivisen `restart audio` -polun
- [x] Vastaamaton rekisteröity serveri näkyy danger-tilana ja hermettinen
      lifecycle-portti todistaa quit-before-boot-palautuksen
- [x] Responsiivinen mutta epäterve audiokello ei julkaise readyä: kolme
      0,75 s välein luettua arvoa vaaditaan terveiksi. 47 986–48 003 Hz
      läpäisee 48 kHz:n pyynnön; mitattu 2610,8 Hz sekä vanha terve ensimmäinen
      arvo ja romahdus `[48000, 7076.4, 7019.8]` hylätään ennen graafin
      allokointia ja palauttavat yhden facade-failure-callbackin
- [x] AIO-5: provider luetaan vain varmasti pysähtyneenä; boot/reboot-cache
      sisältää exact directional listat
- [x] Recommended piilottaa vain legacy-prefixit, show-all palauttaa ne ilman
      provider-read-countin kasvua ja active/draft exact valinta säilyy
- [x] Scan failure ei estä bootia; fallback/stale-cache näkyy rehellisesti
- [x] Fyysinen AIO-5 split-WASAPI-portti (2026-07-29): tiivis recommended,
      show-all ja paluu recommendediin toimivat Audio-sivu auki ilman äänen,
      Night-liikkeen tai input/master-presencen katkeamista
- [x] Clean `restart audio` pysäyttää serverin, ottaa uuden directional-cachen
      ja rakentaa GUI:n takaisin; ääni ja Night-liike palautuvat ilman
      unresponsive-tilaa (split-WASAPI 2026-07-29)
- [x] Eristetty raw-input-uusintatesti (2026-07-29): split-WASAPI käynnistyi,
      ääni/Night pysyivät elävinä, molemmat live-presence-rivit reagoivat ja
      sama `scsynth` vastasi kolmeen ulkoiseen `/status`-kyselyyn
- [ ] System default boot: tämän kannettavan testi 2026-07-30 ei ole
      hyväksyttävissä. GUI ja live L/R -presence toimivat, mutta Night putosi
      noin 1 FPS:ään ja ääni glitchasi; kolme vastausta raportoi nimellisen
      48 kHz:n rinnalla vain 2610,8–2612,8 Hz toteutuvan sample raten.
      Manuaalinen paluu split-WASAPI-overlayhin + `CQ.reboot` palautti normaalin
      äänen/Night-liikkeen, 47 986–48 003 Hz:n kellon ja 7,8–9,5 % server-CPU:n.
      Älä toista system-default-porttia tällä koneella; epäterveen audiokellon
      pre-graph failure ja yhden rollbackin reitti on tämän jälkeen katettu
      hermettisesti ilman uutta fyysistä riskiä.
- [x] Yksi USB-interface single-device-tilassa (K-Mix ASIO 2026-07-30):
      restart onnistui normaalilla äänellä ja GUI:lla, inputit 1/2 vastasivat
      raw live L/R -presenceen ja main L/R kuuluivat fyysisistä ulostuloista 1/2
- [ ] Windows split input/output samalla host APIlla: molemmat laitteet
      käynnistyvät. Väärän/päättyneen nimen fyysinen rollback-osuus ei läpäissyt
      2026-07-30: irrotetun ASIO-laitteen backend avasi näennäissession, jonka
      ensimmäinen kellolukema oli vanha 48 kHz ja seuraavat noin 7076/7020 Hz.
      Profiili ehti promotoitua ja split palautettiin manuaalisesti. Korjattu
      kolmipisteinen kelloguard kattaa exact sarjan hermettisesti; fyysistä
      riskikoetta ei toisteta tällä koneella
- [x] Mic `off` → ch 1 säilytti gain 0:n ja näytti raw-presencen muted-tilassa;
      Workshopin ainoa `micGain` avasi kuultavan dry-monitorin ja `live`-tilan
      ilman restarttia, palasi nollaan ja explicit mic `off` persistoi takaisin
      toimivaan audioon (split-WASAPI 2026-07-30)
- [x] PANIC mykistää fyysiset main L/R -portit ja master-presence hiljenee;
      raw live L/R jatkaa portin näyttämistä ja toinen painallus palauttaa äänen
      (split-WASAPI 2026-07-29 sekä K-Mix main 1/2 ja stemit 3/4 2026-07-30)
- [x] Vähintään 4-kanavainen interface (K-Mix 2026-07-30): stem 1/2 kuuluivat
      mainista erillisistä fyysisistä porteista 3/4, vain Coco1 avasi 3:n ja
      vain Coco2 4:n. PANIC hiljensi molemmat; yhteinen safety-katto on katettu
      koodilla ja NRT-portilla
- [x] REC stereo kirjoitti 2026-07-29 RIFF/WAVE-tiedoston: 2 ch, 48 kHz,
      24-bit, 21,24 s; header ja tiedostokoko ovat ehjät
- [x] `record 4ch` kirjoitti 2026-07-30 RIFF/WAVE-tiedoston: 4 ch, 48 kHz,
      24-bit, 13,15 s; `recStems: true` persistoi exact ja palautus `off` /
      `recStems: false` restarttasi takaisin toimivaan audioon
- [x] Virtuaalinen loopback näkyy tavallisena driver-laitteena ilman lupausta
      siitä, että Yölahti olisi luonut järjestelmäkytkennän (2026-07-30):
      koneen valmiiksi aktiivinen Realtek `Stereo Mix` löytyi input-listalta
      exact-nimenä vasta `show all drivers` -kytkimellä, koska sen host API on
      `Windows WDM-KS` — yksi `CQAudioDeviceCatalog.legacyWindowsPrefixes`in
      kolmesta. Recommended ei siis piilota laitetta vaan luokittelee sen
      ei-suositelluksi, ja exact valinta säilyi. Vahingossa tehty valinta
      palautui arvovertailulla puhtaaksi: gate luki jälleen `no changes`,
      mitään ei kirjoitettu eikä serveriä restartattu. Sivulla ei ole
      `connected`-väitettä, jackeja eikä johtoja
- [ ] **Avoin: kaksisuuntainen DAW-reitti VB-CABLE A+B:llä.** WDM-KS-loopback
      ei kelpaa tähän: split vaatii saman host API:n molemmissa päissä, ja
      WDM-KS on exclusive-mode kernel streaming, jota DAW ei pysty pitämään
      auki toisesta päästä yhtä aikaa. Portti tehdään omana päivänään, kun
      ajuri on asennettu: DAW → `CABLE-A Input`, Yölahden input =
      `CABLE-A Output`, Yölahden main out = `CABLE-B Input`, DAW monitoroi/
      nauhoittaa `CABLE-B Output`ista. Molemmat Yölahden päät ovat WASAPI, eli
      split-ehto täyttyy. Vaatii 48 kHz:n asettamisen kaapeleihin Windowsin
      ääniasetuksista ja siirtää monitoroinnin DAW:lle. Todistettava:
      exact-nimet recommended-listalla, presence kumpaankin suuntaan,
      palautus split-baselineen
- [x] 10 min tiheä `\mayhem`, molemmat Cocot, kaksi myöhemmin ladattua deckiä
      ja Audio↔Night-vaihdot (2026-07-30): 21/21 vastausta samalla PID:llä,
      actual SR 47 986–48 021 Hz, server avg enintään 12,55 % / peak 14,04 %.
      Node-puu kasvoi 6→8 vain kahdella odotetulla `cqDeck`-synthillä; muisti
      ja handlet tasaantuivat kertavarausten jälkeen. Ei kuultua dropoutia tai
      glitchiä. Täyden 352-kaapelin Night-renderissä oli ajoittaista pientä,
      ei kasvavaa hitautta; Drawer↔Night-vaihdot pysyivät nopeina.
- [x] **Fyysinen AIO-hardware-katselmus hyväksytty 2026-07-30.** Hyväksytty
      rajaus: split-WASAPI ja K-Mix ASIO single-device, inputit 1/2, main 1/2,
      stemit 3/4, mic-turvaportti, PANIC, stereo/4ch REC, loopbackin näkyvyys
      ja 10 minuutin tiheä patch. `system default` ja irrotetun laitteen
      rollback ovat kirjattuja fyysisiä failureita, joita **ei toisteta tällä
      koneella**; niiden yhteinen juurisyy on korjattu ja katettu hermettisellä
      regressiolla. Avoimeksi jää vain yllä kuvattu VB-CABLE-DAW-portti, ja
      tämä lyhyt vakauskoe **ei korvaa** jonossa seuraavaa pitkää koko
      soittimen laitteisto-/resource-soakia.

### MOR-1 — morph ei jäädytä käyttöliittymää

Löytyi soakissa 2026-07-30: 90 s morph kahden tiheän snapshotin välillä piti
`sclang`in 100 %:ssa noin 320 s, GUI oli jäässä ja **PANIC tavoittamattomissa**
samalla kun `scsynth` soi terveenä. Korjaus: yksi tiheä `writeMatrix` per kehys,
kuluneeseen aikaan sidotut kehykset ja taattu vähimmäisluovutus.

```supercollider
// Ctrl+Shift+L ensin, sitten CQ.boot.
CQ.patchPreset(\mayhem);
CQ.snap(\mor1_a);
CQ.shake;
CQ.snap(\mor1_b);

// 1. Pitka morph tiheilla snapshoteilla. Kerro morphin AIKANA:
//    - liikkuuko Night sujuvasti
//    - vastaako PANIC heti (tama on ratkaiseva)
//    - liikkuvatko kaapelit ja nupit tasaisesti, ei portaittain
CQ.morph(\mor1_a, \mor1_b, 90);

// 2. Kestaako morph noin pyydetyn ajan (kellota karkeasti)?
// 3. Uusi morph kesken morphin: GUI:n pitaa vastata koko ajan.
// 4. Kolme lyhytta morphia perakkain, odota valissa etta edellinen paattyy:
CQ.morph(\mor1_a, \mor1_b, 5);
CQ.morph(\mor1_b, \mor1_a, 5);
CQ.morph(\mor1_a, \mor1_b, 5);

// 5. Drawer -> memory, sekuntilaatikko ja swap:
//    - klikkaa laatikkoa KERRAN: tuleeko vilkkuva kursori heti
//    - korostuuko laatikko hoverissa, onko valinta violetti (ei natiivi sininen)
//    - onko kirjoitettu teksti tavallisen varista (ei punaista)
//    - kirjoita "20" ILMAN enteria, klikkaa muualta: katoaako kursori
//    - kirjoita "20" ILMAN enteria ja paina morph: kestaako 20 s
//    - kirjoita roskaa ja paina morph: palaako edellinen kelpo arvo
//    - vaihtaako kaksipainen nuoli a:n ja b:n paittain, ja onko sen hover nakyva
//    - lukeeko mittarin oikeassa reunassa lopussa kokonaan "arrived"
```

- [x] **Hyväksytty 2026-07-30.** 90 s morph tiheillä snapshoteilla ei enää
      jäädytä GUI:ta, morph kesken morphin toimii, ja kolme peräkkäistä 5 s
      morphia eivät jäädytä mitään. Ensimmäisen katselmuskierroksen ~5 s
      jäätyminen oli **kertaluonteinen lämmittely** boottauksen jälkeisessä
      ensimmäisessä tiheässä morphissa, ei kehyskustannus: sama koe toistettuna
      kolmesti peräkkäin on täysin puhdas.
- [x] **Sekuntilaatikko ja swap hyväksytty 2026-07-30** kahden
      korjauskierroksen jälkeen: yhden klikkauksen kursori, hover, violetti
      valinta, tavallisen värinen teksti, elävän tekstin luku ilman Enteriä,
      fokuksen vapautus mistä tahansa muualta sivulta, kokonaan piirtyvä
      `arrived` ja kaksipäinen swap-nuoli päiden välissä.

### DAW-1 — ReaRoute-silta REAPERiin

Yölahti avaa `ASIO : ReaRoute ASIO (x64)` single-device-tilassa; REAPER pysyy
omalla laitteistollaan (K-Mix ASIO), joten monitorointi on REAPERin puolella.
Esiehdot ja karikot: PLAN.md → "DAW-sillan keino: ReaRoute, ei VB-CABLE".
**REAPERin on oltava pystyssä ennen `CQ.boot`ia**, ja vasta asennettu ajuri
vaatii tuoreen tulkin (Language → Reboot Interpreter), ei palvelimen restarttia.

```supercollider
// DAW-1. Esiehdot: REAPER ASIOlla (K-Mix), 48000 Hz, "Close audio device when
// stopped and application is inactive" POIS. REAPER pystyyn ENNEN boottia.
// Audio-sivu -> single device -> "ASIO : ReaRoute ASIO (x64)" -> apply&restart.
CQ.boot;

// 1. SUUNTA YOLAHTI -> REAPER. Paluuraita: input ReaRoute 1/2, record arm JA
//    input monitoring paalla, master send paalla.
CQ.set(\masterVol, -24);       // aloita hiljaa, selfNoise kasvaa nopeasti
CQ.patchPreset(\selfNoise);    // soi ilman mitaan sisaantuloa
//    Todennus: REAPERin paluuraidan mittari liikkuu ja aani kuuluu K-Mixista.
//    Nosta masterVol maltilla kunnes taso on mukava.

// 2. SUUNTA REAPER -> YOLAHTI. Lahetysraita: lahde + Route -> Add new hardware
//    output -> ReaRoute 1/2. Tarkista molemmat mykistykset:
//      - hardware output -rivin OMA M reititysikkunassa (ei raidan M)
//      - raidan input monitoring
//    Armattu raita nayttaa mittarissa sisaantulon vaikka monitorointi olisi
//    pois -- mittarin liike ei todista etta aani virtaa eteenpain.
CQ.patchPreset(\empty);
//    Todennus SILMALLA, ei korvalla: Drawer -> Audio -> input presence -palkki
//    liikkuu kun lahetysraita soittaa. Se lukee raakaa sisaantuloa, joten se
//    kertoo saapuuko signaali riippumatta siita mita patchi sille tekee.
//    Oletukset riittavat: preASrc \liveL, preBSrc \liveR, liveGain 1.0.

// 3. Silmukan valttaminen: paluuraidalle EI koskaan hardware outputia
//    ReaRouteen. Turvaketju rajaa tason mutta se ei ole haettu aani.
```

- [x] **DAW-1 todettu toimivaksi molempiin suuntiin 2026-07-31.** Yölahti →
      REAPER kuului paluuraidan kautta K-Mixistä, ja REAPER → Yölahti näkyi
      input presence -palkissa. Molemmat kompastuskivet osuivat kohdalle
      katselmuksessa: hardware output -rivin oma `M` oli päällä ja input
      monitoring oli pois.
- [ ] Kierrosviiveen mittaus ja `Media playback offset` -kompensaatio (ei 1.0:n
      ehto — paluuraidalle nauhoitettu materiaali on siihen asti aikasiirtynyt)

### AUD-W — ajonaikainen audiokelloguard

Guard lukee `Server.actualSampleRate`ia 2,5 s välein ja julistaa vian vasta kun
kello on **jumissa**: epäterve 60 s ajan eikä suppene kohti pyydettyä taajuutta.
Ei automaattista recoveryä — tavoite on lakata valehtelemasta, ei korjata
itseään. **Älä provosoi irrottamalla laitetta kesken restartin** (kirjattu
ei-toistettava koe); käytä lepotilaa/herätystä tai soakissa itsestään osuvaa
tapausta.

```supercollider
// AUD-W. Ctrl+Shift+L ensin (classit muuttuivat), sitten:
CQ.boot;
CQ.set(\masterVol, -24);
CQ.patchPreset(\selfNoise);

// 1. TERVE TILA EI SAA NAKYA. Drawer -> Audio, katso engine-rivia pari
//    minuuttia: tavallinen lifecycle-teksti, ei danger-varia, ei valahdyksia.
//    Mitaan ei saa liikkua guardin takia.
Cocoquantus.audioClockState;    // -> ok
Cocoquantus.audioClockRate;     // -> mitattu taajuus, esim 47986.3
Cocoquantus.audioClockDanger;   // -> false

// 2. PROVOKAATIO. Anna naytön sammua / koneen mennä matalatehotilaan ja hera
//    se. Modern Standby pysayttaa striimin ja tuottaa mitatun transientin.
//    Kerro kumpi tapahtui:
//      (a) Yolahti toipui HILJAA eika sanonut mitaan  -> guard toimii oikein
//      (b) engine-rivi sanoi "audio clock stuck - reports N Hz" danger-varilla
//          ja palautui itsestaan kun aani palasi     -> guard toimii oikein
//      (c) hallytys jai paalle vaikka aani oli kunnossa -> VIKA, kerro N
//      (d) aani oli roskaa mutta mikaan ei sanonut mitaan -> VIKA, kerro
//          mita Cocoquantus.audioClockRate sanoi silloin

// 3. PANIC ja Audio-sivun "restart audio" pitaa olla tavoitettavissa koko ajan
//    myos danger-tilassa (turvainvariantti #3).
CQ.panic;

// 4. KOKO KETJU LAPI TURVALLISESTI (osa 1). Kerrotaan guardille etta pyydettiin
//    96 kHz vaikka laite ajaa 48 kHz: aaneen ei kosketa lainkaan, vain guardin
//    vertailukohta on vaara, joten se nakee "kello kay puolinopeudella eika
//    korjaannu" ja tekee tasan sen mita oikeassa viassa.
Cocoquantus.prStartAudioClockWatch(CQ.server, Cocoquantus.sessionGeneration, 96000);
//    Odota ~65 s. Sen jalkeen pitaa olla totta:
//      - tyokalurivilla palaa "audio clock stuck"
//      - Drawer -> Audio: engine-rivi danger-varilla, "reports 47986.3 Hz"
//      - status-rivi: "audio clock stuck - restart audio"
//      - post-ikkunassa YKSI varoitus, ei toistuvaa spammia
Cocoquantus.audioClockState;   // -> danger
CQ.panic;                      // soittimen pitaa olla kaytettavissa danger-tilassa
//    Palautus. Merkin pitaa kadota HETI, ilman odotusta:
Cocoquantus.prStartAudioClockWatch(CQ.server, Cocoquantus.sessionGeneration);

// 5. AINA NAKYVA MERKKI ERIKSEEN. Talla voi katsoa merkin ilman etta mitaan on
//    rikki -- se kirjoittaa vain nakymaan, ei guardin tilaan:
CQ.gui.shell.audioClockDanger = true;
//    Tyokalurivilla, drawer-napin vasemmalla puolella, pitaa nyt olla
//    danger-varinen piste ja teksti "audio clock stuck". Katso etta:
//      - se ei vilku eika liiku
//      - se ei mene yolahti-otsikon eika drawer-napin paalle
//      - se nakyy myos Drawer auki ja Drawer kiinni
//      - kavenna ikkuna minimiin (1280): teksti saa pudota, piste ei
CQ.gui.shell.audioClockDanger = false;
//    Merkin pitaa kadota kokonaan, eika tyokaluriville saa jaada jalkea.
```

- [x] **AUD-W osa 1 hyväksytty 2026-07-31.** Terve tila hiljainen, simuloitu
      jumi sytytti merkin ja Audio-sivun mitatun taajuuden, PANIC vastasi
      danger-tilassa, ja palautus sammutti merkin heti.
      Katselmus löysi kaksi vikaa, molemmat korjattu ennen hyväksyntää:
      pysäytys nollasi tilan **hiljaa** eikä merkki siksi koskaan sammunut
      (`bbcad67`), ja laitelistan huomautus paisutti engine-rivin `show all
      drivers` -napin alle (`d331ff4`).
- [x] **AUD-W:n aina näkyvä merkki hyväksytty 2026-07-31** — paikka, koko ja
      luettavuus työkalurivillä drawer-napin vieressä, katoaa jäljettä.
- [ ] **AUD-W osa 2 — oikea lepotilaprovokaatio.** Tehdään osana pitkää soakia
      (käyttäjän päätös 2026-07-31). **Huomaa:** `tests/soak_monitor.ps1` pitää
      järjestelmän hereillä koko ajonsa ajan (`SetThreadExecutionState`), juuri
      siksi ettei valvomaton soak mittaisi Modern Standby -artefaktia. Osa 2
      vaatii siis **oman ikkunansa monitorin pysäytyksen jälkeen** (tai ajon
      ilman monitoria) — muuten kone ei nukahda eikä provokaatiota tapahdu.
      Odotus: guard **ei** saa huutaa toipuvasta heräämisestä; mitattu sarja
      7419 → 69 184 → 47 986 Hz on suppeneva eikä vika.
      **Ajettu 2026-07-31 SOAK-1:n vaiheessa 4 — EI hyväksytty.** Lepotila ei
      tuottanut suppenevaa transienttia vaan tappoi palvelimen ääniketjun
      kokonaan (prosessi elossa, CPU jäässä, 0/4 `/status`-vastausta), ja
      guard väitti silti `ok` / 48 003,47 Hz jäätyneestä välimuistilukemasta.
      Tulos on siis kohta **(d)**: ääni oli poissa eikä guard sanonut mitään.
      Löydös B PLAN.md:ssä; korjataan järjestyksessä C → B → A.

### SOAK-1 — pitkä laitteisto-/resource-soak (+ AUD-W osa 2)

Tien 1.0:aan askel 3. Todistaa, että soitin kestää tuntikausia ilman että mikään
ryömii: ei muistin, handlejen tai säikeiden kasvua, ei palvelimen kuolemaa, ei
kellon karkaamista, ei GUI:n hidastumista. AIO:n 10 minuutin vakauskoe **ei**
korvaa tätä — MOR-1 löytyi juuri soakista ja rikkoi turvainvarianttia #3.

**Soakattava kokoonpano on K-Mix ASIO single-device** (käyttäjän päätös
2026-07-31): sama laite jolla oikeasti soitetaan, ei `config.scd`:n
split-WASAPI-pohja. USB-äänilaite + lepotila on myös tasan se ilmiö jota AUD-W
vartioi, joten vaihe 4 on tällä kokoonpanolla mielekkäin.

**Esiehdot — täytettävä ennen aloitusta:**

- **Verkkovirta, ei akku.** 10 h ei mene akulla, ja akku noin kaksinkertaisti
  MOR-1:n mitatun kertoimen.
- **K-Mix kiinni ja päällä ennen boottia.** REAPER kiinni: ReaRoute on oma
  muuttujansa eikä sitä soakata tässä.
- **Ei toista `sclang`- tai `scsynth`-prosessia koneella koko ajon aikana.**
  `soak_monitor.ps1` ottaa `Select-Object -First 1`, joten porttiajo kesken
  soakin mittaisi väärää prosessia. Ei siis gate-ajoja sinä yönä eikä toista
  IDE:tä.
- **Ei äänitystä yöksi** (jokainen REC luo uuden tiedoston) eikä laitelistausta
  ajon aikana (käynnissä oleva palvelin ei saa nähdä `_ListAudioDevices`ia).
- **Monitori käynnistetään vasta kun laitevaihto on tehty ja palvelin on
  asettunut.** Apply & restart vaihtaa scsynthin PID:n, ja monitori kirjaisi sen
  uudelleenkäynnistyksenä.
- Jätä **Night näkyviin ja Drawer kiinni** yöksi: Night on raskaampi piirtopinta,
  ja työkalurivin aina näkyvä merkki hoitaa danger-tilan ilman auki olevaa
  Audio-sivua. Näyttö saa sammua — keep-awake pitää *järjestelmän* hereillä.

#### Vaihe 1 — käynnistys ja tiheä käsipatchaus (~30 min, sinä läsnä)

```supercollider
// SOAK-1 vaihe 1. Ctrl+Shift+L jos classit ovat muuttuneet, sitten:
CQ.boot;

// 1. Laitevaihto: Drawer -> Audio -> single device ->
//    "ASIO : K-MixUSBAudio ASIO Driver" (tarkista tarkka nimi listasta)
//    -> apply & restart. Odota etta lifecycle-rivi on rauhoittunut.
CQ.server.options.device;      // -> valittu K-Mix, EI WASAPI-paria
CQ.server.options.sampleRate;  // -> 48000
Cocoquantus.audioClockState;   // -> ok
Cocoquantus.audioClockRate;    // -> ~47986 .. 48021
Cocoquantus.audioClockDanger;  // -> false

// 2. Tiheaa kasipatchausta 20-30 min. Tee ainakin nama, kuunnellen:
//      - Nightissa oma patchaus, punch/hold molemmilla kuorilla
//      - kuun shake muutaman kerran (pieni sirppi ja tayskuu)
//      - deck-lataus linnun kautta molempiin dekkeihin
//      - Drawer <-> Night -vaihtoja, Workshopin sailmia
//      - Memory-sivulta yksi morph kasin
//      - PANIC kerran ja CQ.resume
// 3. Kerro lopuksi: tuntuuko mikaan hitaalta, glitchaako, onko taso vakaa.
CQ.panic;
CQ.resume;
```

#### Vaihe 2 — yön ajo (8–10 h, valvomaton)

Ensin aktiviteettisilmukka soittimeen, sitten monitori **omaan
PowerShell-ikkunaansa** (ei editorin sisään, jotta se elää editorin yli).

```supercollider
// SOAK-1 vaihe 2 -- yon aktiviteettisilmukka. Pieni, hidas ja rajattu liike,
// jotta soak mittaa myos kielipuolen elamaa eika pelkkaa lepotilaa. Ei
// aanitysta, ei uusia snapshot-tiedostoja (kaksi nimea kirjoitetaan kerran),
// ei laitelistausta, eika masterVolia tai micGainia kosketa mikaan.
// Joka 8. askel ajaa 20 s morphin kahden TIHEAN snapshotin valilla -- se on
// MOR-1:n oma regressio, nyt noin 70 kertaa yon yli -- ja lataa dekkiin
// satunnaisen samplen. deck.random skannaa configin samples-kansion itse
// (sama polku kuin linnun porhistys), joten polkuja ei tarvitse kirjoittaa;
// tyhja kansio on hiljainen no-op.
(
~soakStop = false;
~soakActivity = Routine({
    var step = 0;
    // KAKSI presettia tassa jarjestyksessa, ei yksi: \mayhem on tiheysstressi
    // jonka params-osa on TYHJA -- se tayttaa 352 matriisisolua muttei sytyta
    // yhtaan aanilahdetta, joten pelkastaan sina yo olisi aanettomasti tiheä.
    // \selfNoise antaa aanen (dust, fb, cross-feed) ilman sisaantuloa, ja
    // \mayhem korvaa sen jalkeen vain matriisin -> tiheä JA kuuluva yö.
    CQ.patchPreset(\selfNoise);
    CQ.patchPreset(\mayhem);
    CQ.snap(\soak_a);
    CQ.shake(0.3, density: 0.35);
    CQ.snap(\soak_b);
    while({ ~soakStop.not }, {
        step = step + 1;
        switch(step % 8,
            1, { CQ.set(\coco1Speed, rrand(6000, 30000)) },
            2, { CQ.set(\coco2Speed, rrand(6000, 30000)) },
            3, { CQ.set(\coco1Fb, rrand(0.2, 0.95)) },
            4, { CQ.hold(1, step.div(8) % 2) },
            // dekkilataus on OMALLA tikillaan eika morphin kanssa samalla:
            // yksi teko per tikki, jotta jaatyminen voidaan lukea yhdesta
            // syysta. Kaksi tekoa samassa hetkessa teki 2026-07-31 yon
            // ensimmaisesta havainnosta tulkinnanvaraisen.
            5, { if(step.div(8).odd, { CQ.deckA.random }, { CQ.deckB.random }) },
            // flip ja skip samalla tikilla: molemmat ovat kertalaukaisuja,
            // joiden kielikustannus on mikrosekunteja -- ne eivat voi olla
            // jaatymisen syy, joten ne eivat riko yhden teon saantoa
            6, { CQ.flip(1, [0, 1].choose); CQ.skip(2) },
            7, { CQ.shake(0.25, density: 0.2) },
            0, { CQ.morph(\soak_a, \soak_b, 20) }
        );
        60.wait;
    });
    "SOAK: activity loop stopped.".postln;
}).play;
)
```

```powershell
# SOAK-1 vaihe 2 -- monitori omaan ikkunaansa, vasta kun laite on asettunut.
# Anna -Hours tarkasti halutun mittaiseksi: silloin CQ_SOAK_SUMMARY tulostuu
# itsestaan eika jaa Ctrl+C:n varaan.
powershell -NoProfile -ExecutionPolicy Bypass -File tests/soak_monitor.ps1 -Hours 10 -IntervalSeconds 20
```

Kuunteluäänen saa yöksi hiljaiseksi **K-Mixin omasta monitorinupista** — älä
laske `masterVol`ia, koska digitaalinen ketju ja limitteri kuuluvat testiin.
Jos watchdog laukeaa yön aikana (`autoTrim`), se on **havainto eikä vika**:
`CQ.trimDb` kertoo aamulla paljonko se leikkasi.

Monitori näytteistää 20 s välein `/status`illa — ei uutta serverliikennettä, ei
laitelistausta — ja pitää järjestelmän hereillä `SetThreadExecutionState`illa.
CSV flushataan joka näytteellä, joten keskeytyskin säilyttää mitatun.
Tulos: `tests/tmp/soak/soak-<aikaleima>.csv` + `-events.log` (gitignored).

#### Vaihe 3 — aamun luenta (~15 min)

**Älä boottaa uudelleen ennen tätä** — soittimen pitää olla sama prosessi joka
soi yön yli, muuten mittaus menetetään.

```supercollider
// SOAK-1 vaihe 3. Ensin aktiviteetti pois, soitin jaa pystyyn:
~soakStop = true; ~soakActivity.stop;

Cocoquantus.audioClockState;   // -> ok
Cocoquantus.audioClockRate;    // -> ~48000
CQ.server.avgCPU;              // -> alle SPEC 9:n 25 %
CQ.server.numSynths;           // -> sama levotaso kuin illalla, ei kasvanut

// 1. Vasteet 10 h jalkeen -- kerro tuntuuko mikaan hitaammalta kuin illalla:
CQ.panic;                      // pitaa vastata VALITTOMASTI (turvainvariantti #3)
CQ.resume;
CQ.morph(\soak_a, \soak_b, 20); // GUI ei saa jaatya, kaapelit liikkuvat tasaisesti
// 2. Hover Nightissa, Drawer auki ja kiinni: yhta nopeaa kuin illalla?
// 3. Kuuntele: onko aani puhdasta, onko taso sama, kuuluuko dropouteja?
```

Monitorin `CQ_SOAK_SUMMARY` ja CSV luetaan tässä kohtaa yhdessä: Claude käy
sarakkeet läpi trendinä (working set, handlet, säikeet, avg/peak CPU, synths).
Kertaluonteinen alkuvaraus on ok, tasainen nousu ei ole.

#### Vaihe 4 — AUD-W osa 2, lepotilaprovokaatio (~30–40 min, enimmäkseen odotusta)

Tämä **ei mahdu soakin sisään**: monitori pitää koneen hereillä nimenomaan
estääkseen tämän ilmiön. Oma ikkunansa monitorin pysäytyksen jälkeen, soitin
edelleen käynnissä.

```supercollider
// SOAK-1 vaihe 4. Esiehdot: monitori pysaytetty (Ctrl+C tai -Hours umpeen),
// aktiviteettisilmukka pysaytetty, soitin EDELLEEN pystyssa ja soimassa.
// Aseta naytön sammutus lyhyeksi (esim. 2 min) ja ALA KOSKE koneeseen ~15 min.
// Modern Standby pysayttaa audiostriimin ja tuottaa mitatun transientin.
Cocoquantus.audioClockRate;    // kirjaa arvo ENNEN nukahtamista

// Heraamisen jalkeen: odota vahintaan 2 min (guard tarkistaa 2,5 s valein,
// ikkuna on 60 s), KUUNTELE, ja kerro sitten kumpi tapahtui:
//   (a) toipui HILJAA, ei sanonut mitaan             -> guard toimii oikein
//   (b) sanoi "audio clock stuck", ja merkki katosi
//       itsestaan kun aani palasi                    -> guard toimii oikein
//   (c) hallytys jai paalle vaikka aani oli kunnossa -> VIKA
//   (d) aani oli roskaa mutta mikaan ei sanonut      -> VIKA
//   (e) palvelin kuoli koska K-Mix katosi            -> kirjataan, kerro mita
//       Audio-sivu ja tyokalurivi sanoivat
Cocoquantus.audioClockState;
Cocoquantus.audioClockRate;
CQ.panic;                      // tavoitettava myos danger-tilassa
CQ.resume;
```

Odotus: guard **ei** saa huutaa toipuvasta heräämisestä — mitattu sarja
7419 → 69 184 → 47 986 Hz on suppeneva eikä vika. Rehellinen, itsestään
sammuva danger on yhtä hyväksyttävä tulos kuin täysi hiljaisuus; valhe ei ole.

#### Hyväksyntäkriteerit (kirjattu ennen ajoa, ei jälkikäteen)

1. `status failures: 0` — jokaiseen `/status`iin vastattiin.
2. `scsynth restart: 0` — sama PID koko ajon.
3. `unhealthy clock: 0` näytettä valvotussa ikkunassa (suhde 0,8–1,2).
4. Server avg CPU alle SPEC §9:n 25 % koko ajon **eikä** nouseva trendi.
5. scsynthin ja sclangin working set tasaantuu ensimmäisen tunnin jälkeen:
   viimeisen tunnin keskiarvo ei ylitä toisen tunnin keskiarvoa yli 5 %:lla
   eikä käyrä ole monotoninen ramppi.
6. Handlet ja säikeet tasaisia ensimmäisen tunnin jälkeen — vuoto näkyy suorana
   nousevana viivana.
7. Synth-määrä palaa lepotasolleen; synthit eivät kerry.
8. Vaiheen 3 vasteet: PANIC välitön, morph ei jäädytä, GUI yhtä nopea kuin
   illalla, ei kuultua dropoutia eikä tason ajautumista.
9. Vaihe 4: guard vaikeni tai kertoi totuuden ja palautui itse. Ei (c) eikä (d).

- [ ] **SOAK-1 hyväksytty** (pvm, kesto, CSV-tiedosto, kriteerit 1–9)
- [ ] **AUD-W osa 2 hyväksytty** (kumpi tulos: a vai b, ja mitattu taajuus
      ennen ja jälkeen)

**Ensimmäinen ajo 2026-07-31, 6,53 h, ei hyväksytty.**
`tests/tmp/soak/soak-20260731-015614.csv`, 1174 näytettä. Täydet mittausluvut,
mekanismit ja hyväksyntäehdot: PLAN.md → "SOAK-1:n tulos 2026-07-31".

- **Kriteerit 1–4, 6 ja 7 vihreät.** 1174/1174 `/status`-vastausta, 0
  epätervettä kellonäytettä, yksi PID, SR 47 986–48 021 Hz, server CPU ka
  11,89 % (max peak 17,9 %), synthejä 8 joka näytteessä, scsynthin handlet
  344 ja sclangin handlet/säikeet tasaiset — ei handle- eikä säievuotoa.
- **Kriteeri 5 auki:** sclangin työjoukko 305,7 → 411,6 MB (~+20 MB/h) eikä
  tasaantumista 6,5 tunnissa.
- **Kriteeri 8 kaatui (löydös A):** eristetty 20 s morph vie yhden ytimen
  40–60 s ~8 minuutin välein, GUI jäätyy ja **PANIC ei vastaa** —
  turvainvariantti #3. Toistui joka kerta, ei ollut lämmittely. Sama
  jäätyminen myös Audio-sivu auki (Night piilossa), joten syy ei ole
  kaapelimuste. Morphin hinta kasvoi yön mittaan 14,5 → 17,6 s samaa tahtia
  muistin kanssa.
- **Kriteeri 9 kaatui (löydökset B ja C):** lepotila tappoi palvelimen
  ääniketjun (prosessi elossa, CPU jäässä, 0/4 `/status`-vastausta), mutta
  guard sanoi **`ok`** ja 48 003,47 Hz — jäätynyt lukema. Audio-sivun
  lifecycle-rivi kertoi totuuden, työkalurivin merkki ei syttynyt. Sen jälkeen
  `restart audio` **hävitti käyttöliittymän**: pakkopysäytys tappoi PID 6012:n
  vaikka palvelin oli 17716, orpo jäi pitämään UDP 57110:tä eikä uusi boot
  voinut onnistua. Toipuminen vaati orvon tappamisen käsin.
- **Työkaluvelka:** Ctrl+C `Start-Sleep`in aikana ohitti skriptin
  `finally`-lohkon, joten `CQ_SOAK_SUMMARY` jäi tulostumatta. CSV oli tallessa.

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
