// live.scd — performance scratchpad. Evaluate blocks manually; nothing here is
// loaded automatically and no scratch code belongs in classes/ (CLAUDE.md).

// ---- phase 1 smoke test / KUUNTELU 1 ----
/*
CQ.boot;                     // silence, then a 2 s fade-in

CQ.set(\masterDry, 0.5);     // monitor the live inputs through the safety chain
CQ.set(\masterVol, -12);     // default; try small moves, must be click-free

CQ.panic;                    // instant mute (30 ms fade), state preserved
CQ.resume;                   // and back

CQ.get(\masterVol);          // read any parameter
CQ.free;                     // clean teardown
// CQ.reboot;                // strong: really restarts Server.default (stops other synths)
*/

// ---- phase 2: deck A + the coco pair / KUUNTELU 2 ----
/*
CQ.boot;                             // power-on: RAM full of dust, loop-locked

CQ.deckA.load("field.wav");          // relative paths resolve inside samples/
// NB: use forward slashes in absolute Windows paths — backslash is the string
// escape character in sclang ("C:/Users/..." works, "C:\Users\..." breaks)
CQ.deckA.rate_(1.0);                 // -2..2, negative = reverse
CQ.deckA.loop_(1);

CQ.hold(1, 0);                       // coco1 records (delay mode)
CQ.hold(1);                          // no value = toggle -> loop-locked again
CQ.set(\coco1Speed, 12000);          // re-pitch the held loop: dark + gritty
CQ.set(\coco1Speed, 48000);          // bright, short
CQ.set(\coco1Fb, 1.0);               // unity looper; 1.15 = escalating crunch
CQ.flip(1);                          // reverse playback direction
CQ.skip(1);                          // reset loop to its start

CQ.set(\coco1DolbyFb, \duck);        // "other-puncher": input punches the layers
CQ.set(\coco1Dust, 0.3);             // digital dust source, try with no input
CQ.set(\coco1Organ, 0.5);            // square-stack voice, pitch tracks Speed

CQ.set(\coco1SlotX, 0.6);            // cross-feed coco2 -> coco1
CQ.set(\coco2SlotX, 0.4);            // ...and back (one block late by design)
*/

// ---- phase 4: routing matrix / KUUNTELU 4 ----
/*
CQ.boot;
CQ.deckA.load("field.wav");
CQ.hold(1, 0);

CQ.patchPreset(\classic);            // tri1 -> coco1/coco2 spaf, castle FM ring
CQ.patchPreset(\empty);              // banana bay cleared (hardware default)

CQ.patch(\envA, \coco2_fbVca, -0.5); // inverso: input duckes coco2's feedback
CQ.patch(\cas3, \coco1_spaf, 0.7);   // castle 3 modulates coco1's speed
CQ.getPatch(\envA, \coco2_fbVca);    // read a single cell back
CQ.unpatch(\envA, \coco2_fbVca);     // remove one connection
CQ.clearPatch;                       // remove everything

CQ.set(\patchDepth, 0);              // global tamer: 0 = matrix has no effect
CQ.set(\patchDepth, 1);              // full depth, lagged 50 ms either way

CQ.patchPreset(\mayhem);             // dense, capped-depth stress patch (SPEC A3)
*/

// ---- patch recipes (KUUNTELU 4 / vapaa sessio) ----
// Lähteet (16): tri1..5, cas1..5, envA, envB, coco1, coco2, org1, org2
// Destit (22): coco1/2_spaf _inVca _fbVca _flip _skip, castle1..5_fm,
//              castle1..5_spesal, deckA/B_rate
// envA/envB ovat sileitä verhokäyriä (ducking!); muut lähteet ovat raakaa
// signaalia — VCA-destissä AM-rousketta, spaf-destissä vibratoa/kaaosta.
// Gate/trig-destit (flip/skip/spesal): Schmitt >0.5 päälle, <0.3 pois —
// painon suuruus säätää kuinka usein kiinteä kynnys ylittyy.

/*
// R1 — Askeltava nauha: cas (stepped S&H) spafiin -> humalainen porras-repitch
CQ.clearPatch;
CQ.set(\qChaos, 0.4);              // verkko elää -> cas-arvot vaeltavat
CQ.patch(\cas3, \coco1_spaf, 0.5);
CQ.set(\coco1Spaf, 0.5);           // syvyys: kokeile 0.2 .. 1

// R2 — Puhe hidastaa nauhurin: envA suoraan deckin nopeuteen (välitön!)
CQ.clearPatch;
CQ.set(\preADrive, 6);
CQ.patch(\envA, \deckA_rate, -1);  // painolla -2 puhe ajaa deckin takaperin

// R3 — Rytminen skip: LFO resetoi loopin tahdissa
CQ.clearPatch;
CQ.hold(1, 1);                     // lukittu looppi
CQ.set(\c2Range, \balcium);        // tri2 LFO:sta ~sykemaiseksi
CQ.set(\qFreq, 1);                 // ~1.2 Hz stutter; nosta = tiheampi
CQ.patch(\tri2, \coco1_skip, 1);

// R4b — Jaksollinen reverse-vuorottelu: kolmio-LFO gaten lapi
CQ.clearPatch;  CQ.hold(1, 1);
CQ.set(\c2Range, \balcium);  CQ.set(\qFreq, 0.5);   // tri2 ~0.6 Hz
CQ.patch(\tri2, \coco1_flip, 1);   // eteen ~3/4, takaperin ~1/4 syklista

// R4c — Satunnaiset reverse-patkat kayttokelpoisella tahdilla
CQ.clearPatch;  CQ.hold(1, 1);
CQ.set(\c1Range, \balcium);        // castle 1:n kello ~4 Hz -> cas1 askeltaa tiheaan
CQ.set(\qChaos, 0.3);
CQ.patch(\cas1, \coco1_flip, 0.8); // paino = reverse-todennakoisyys
// Huom: infra-castlen cas askeltaa vain ~12 s valein -> pitkia reverse-jaksoja;
// aari-chaos voi jaadyttaa castlen kellon (gate jumiin) -> CQ.spesal(k) herattaa.
CQ.set(\c1Range, \infra); CQ.set(\c2Range, \infra); CQ.set(\qFreq, 4);  // palautus

// R5 — Coco-pari: coco1 syottaa coco2:ta oktaavia alempana + AM-ristimod
CQ.clearPatch;
CQ.set(\coco2Level, 0.8);
CQ.set(\coco2SlotX, 0.8);          // coco1:n ulostulo coco2:n inputtiin
CQ.hold(2, 0);                     // coco2 nauhoittaa
CQ.set(\coco2Speed, 12000);        // oktaavin alemmat, tummemmat kerrokset
CQ.set(\coco2Fb, 0.9);
CQ.patch(\coco2, \coco1_fbVca, -0.4);  // raaka audio VCA:han = AM-rousketta

// R6 — Itsegeneroiva verkko (ei inputtia): looppi FM:aa castlea, castle soi looppiin
CQ.clearPatch;
CQ.hold(1, 0);  CQ.set(\coco1Fb, 1.0);
CQ.patch(\coco1, \castle5_fm, 0.6);
CQ.set(\coco1SlotQ, 0.4);
CQ.patch(\tri4, \castle3_spesal, 1);   // verkko re-patchaa itseaan ~13 s valein
*/

// ---- phase 7: space, watchdog, patternit / KUUNTELU 7 ----
/*
CQ.boot;
CQ.deckA.load("field.wav");  CQ.hold(1, 0);

// Space (FDN-kaiku, oletuksena pois): kumpikin tarvitaan, send + mix
CQ.set(\coco1Send, 0.6);             // coco1 -> space (post-fader, pan-sijoitettu)
CQ.set(\spaceMix, 0.4);              // paluutaso (16n F14)
CQ.set(\spaceSize, 0.85);            // 0.3 tiukka .. 0.98 pitka hanta (feedback <= 0.98)
CQ.set(\spaceDamp, 4500);            // 500 tumma .. 12000 kirkas
CQ.set(\spaceMix, 0);                // pois

// Watchdog: aja kuumaksi -> auto-trim -6 dB 5 s jalkeen (config autoTrim)
CQ.trimDb;                           // nykyinen trim (0 = ei trimmia)
CQ.resetTrim;                        // palauta 0 dB kasin

// Patternit (\cq-eventtityyppi): diskreetit aktiot tempossa
p = Pbind(\type, \cq, \action, \spesal, \castle, 3, \dur, Pexprand(0.5, 3)).play;
p.stop;
p = Pbind(\type, \cq, \action, Pseq([\skip, \flip], inf), \coco, 1, \dur, 1).play;
p.stop;
// aktiot: \hold \flip \skip (\coco); \spesal (\castle, \state); \set (\param,
// \value); \patch (\src \dest \weight); \deck (\deck \a/\b, \do next/prev/
// random/restart); \shake (\amount \seed). \rest / bootaamaton = no-op.

// Stemit (config stemOuts: [2,3] + reboot): post-safety coco1/coco2 modulaariin.
// Stem-aanitys (config recStems: true + reboot): CQ.rec -> 4-ch tiedosto.
*/
