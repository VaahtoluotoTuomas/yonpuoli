# Manuaalinen kuuntelulista (SPEC M1–M8)

Ihmiskorvatestit — Claude ei kuule ääntä, joten nämä tarkistaa aina käyttäjä.
Lista kasvaa vaiheittain; M3 (chaos), M4 (`\selfNoise`), M6 (`\classic`),
M7 (abuse-sessio) ja M8 (16n) täydentyvät vaiheissa 3–6.

**Ennen jokaista kierrosta:** jos `classes/` on muuttunut, IDE:ssä
Ctrl+Shift+L (class-lib recompile), sitten `CQ.boot` (bootin pitää olla
täysin hiljainen, sitten 2 s fade-in).

Hyödyllinen lähtötila kuunteluun:

```supercollider
CQ.boot;
CQ.deckA.load("polku/aanite.wav");    // suhteellinen polku etsitään samples/-kansiosta
CQ.set(\masterDry, 0.3);              // kuulet myös lähteen
CQ.set(\coco2Level, 0);               // hiljennä coco2:n power-on-kohinalooppi
```

Huom: absoluuttisissa Windows-poluissa käytä **kauttaviivoja**
(`"C:/Users/..."`) — kenoviiva on sclangin merkkijonoissa escape-merkki,
joten `"C:\tuoma\..."` hajoaa (`\t` = sarkain).

---

## M1 — Speed-repitch (vaihe 2)

Held loopin uudelleenvirityksen pitää olla nauhamainen ja jatkuva.

1. `CQ.hold(1, 0);` → deck A -materiaali alkaa kiertää coco1:een (delay-tila).
2. Muutaman sekunnin päästä `CQ.hold(1, 1);` → looppi lukittuu.
3. `CQ.set(\coco1Speed, 12000);` → sävelkorkeus putoaa oktaavin, sointi
   tummuu ja rouhentuu (ZOH-alias).
4. `CQ.set(\coco1Speed, 48000);` → kirkas, nopea, loopin kesto lyhenee.
5. Liu'uta arvoja edestakaisin: repitchin pitää olla portaaton (tape/doppler),
   eikä loopin sisältö saa rapistua, vaikka sweeppaat minuutin.

- [x] M1 hyväksytty (pvm: 2026-07-16)

## M2 — Feedback-ride (vaihe 2)

Tason on pysyttävä vakaana kaikilla feedback-arvoilla (safety-invariantti).

1. `CQ.hold(1, 0);` (delay-tila), deck A soi.
2. `CQ.set(\coco1Fb, 0.9);` → kaikumaisia toistoja, jotka hiipuvat.
3. `CQ.set(\coco1Fb, 1.0);` → looper: toistot eivät hiivu eivätkä kasva.
4. `CQ.set(\coco1Fb, 1.15);` → eskaloituva, saturoituva crunch — mutta
   äänenvoimakkuus EI kasva hallitsemattomasti (kvantisointiklippi rajaa).
5. Aja fb hitaasti 0 → 1.25 → 0: ei koskaan tasohyppyä eikä kipeää piikkiä.
   Huom: fb-pudotuksen jälkeen buffer tyhjenee portaittain (askel = yksi
   loop-kierto) ja häntä katkeaa bittilattiaan — tämä on autenttista.

- [x] M2 hyväksytty (pvm: 2026-07-16)

## M5 (osa) — Flip, skip, dolby (vaihe 2)

Duck-artikulaatio ja suunnanvaihto; dolby-osuus täydentyy GUI:sta vaiheessa 6.

1. Pidä looppi holdissa (`CQ.hold(1, 1)`).
2. `CQ.flip(1);` → toisto kääntyy takaperin kuultavasti; uusi `CQ.flip(1)`
   palauttaa suunnan.
3. `CQ.skip(1);` → looppi hyppää alkuunsa (fraasi "resetoituu").
4. Delay-tilassa (`CQ.hold(1, 0)`) syötä puhetta/rytmiä ja aseta
   `CQ.set(\coco1DolbyFb, \duck);` → uusi input "lyö" vanhat kerrokset
   hiljaisemmiksi — monofoninen time-layer-artikulaatio.
   Vinkki: duck kuuluu vain materiaalilla, jossa on taukoja (puhe mikistä,
   `preADrive` ~6) — tasainen tekstuuri pitää key-envelopen ylhäällä
   jatkuvasti eikä artikulaatiota synny.
5. `CQ.set(\coco1DolbyFb, \off);` → kerrokset summautuvat taas vapaasti.

- [x] M5/vaihe-2-osa hyväksytty (pvm: 2026-07-16)

## Vaihe 2 -ekstra — Digital dust

1. `CQ.set(\coco1SlotDeck, 0); CQ.set(\coco1SlotPre, 0);` (ei inputtia).
2. `CQ.hold(1, 0); CQ.set(\coco1Fb, 1.1); CQ.set(\coco1Dust, 0.3);`
3. Kuuntele ~30 s: rakeinen kohinapohja kertyy loopiksi — purskeittain,
   ei tasaisesti (LFNoise2-moduloitu dust + 8-bit-bittilattia, by design).
4. `CQ.hold(1, 1);` → dust-tekstuuri jää kiertämään; `coco1Speed` virittää sitä.

- [x] Dust-workflow hyväksytty (pvm: 2026-07-16)

---

## M3 — Quantussy-tuntuma (vaihe 3)

Alkuasetelma — Quantussy kuuluviin coco1:n läpi (castle 5 on boot-oletuksena
audio-alueella, `c5Send 0.5`):

```supercollider
CQ.boot;                       // odota 2 s fade-in
CQ.set(\coco2Level, 0);        // coco2:n power-on-kohinalooppi pois
CQ.set(\coco1SlotDeck, 0);
CQ.set(\coco1SlotPre, 0);
CQ.set(\coco1SlotQ, 1);        // Quantussy-mix coco1:n input-mikseriin
CQ.hold(1, 0);                 // record-tila: qMix pääsee looppiin
```

→ Castle 5:n kolmioaalto (~140 Hz) kuuluu coco1:n läpi.

1. **Chaos-sweep** (`qFreq` oletuksessa 4):
   `CQ.set(\qChaos, 0);` → täysin vakaa sävel.
   `CQ.set(\qChaos, 0.1);` → vibratomainen huojunta.
   `CQ.set(\qChaos, 0.4);` → askeltava satunnaiskulku (S&H-melodia).
   `CQ.set(\qChaos, 1);` → hyperkaaos: purskeita, pillejä, hiljaisuuksia.
2. **qFreq-sweep** (`qChaos` ~0.3): liu'uta `CQ.set(\qFreq, ...)` arvoilla
   0.5 → 4 → 50 → 500. Kaikkien viiden castlen pitää skaalautua yhdessä ja
   LFO↔audio-rajan ylitysten olla portaattomia (ei hyppyjä, ei klikkejä).
3. **Range-kytkimet**:
   `CQ.set(\c1Range, \audio); CQ.set(\c1Send, 0.5);` → toinen ääni sisään.
   `CQ.set(\c1Range, \balcium);` → putoaa keskialueelle.
   `CQ.set(\c1Range, \infra); CQ.set(\c1Send, 0);` → takaisin LFO:ksi.
4. **Spesal** (`qChaos` > 0, jotta verkko elää):
   `CQ.spesal(5);` toistuvasti → partner/mode vaihtuu, tekstuuri muuttuu
   kuultavasti joka askeleella. Myös `CQ.spesal(3)` (FM-lähteen vaihto näkyy
   verkon käytöksessä). `CQ.spesalSet(5, 0);` palauttaa tunnettuun tilaan.
5. **qShLag** (`qChaos` 0.4): `CQ.set(\qShLag, 0.004);` → autenttiset kovat
   askeleet. `CQ.set(\qShLag, 0.08);` → askeleet liukuvat, verkko kesyyntyy.
   Palauta `0.004`.

- [x] M3 hyväksytty (pvm: 2026-07-16)

## M4 — \selfNoise-preset (vaihe 6, tulossa)

## M6 — \classic kenttä-äänitteellä + matriisi (vaihe 4)

Alkuasetelma:

```supercollider
CQ.boot;                       // odota 2 s fade-in
CQ.deckA.load("123681__aidenfry__cricket-and-bird-field-recording.wav");
// ^ TARKISTA post-ikkunasta rivi "CQDeck deckA: loaded ..." — jos siellä lukee
//   "file not found", deck EI soi ja kuulet pelkkää kohinaa!
CQ.set(\coco2Level, 0);        // hiljennä coco2:n power-on-kohinalooppi
CQ.set(\coco1SlotPre, 0);      // LÄPPÄRILLÄ: liveL = sisäinen mikki -> preamp A
                               // syöttäisi huonekohinaa coco1:een (oletus 1.0).
                               // Pöytäkoneella modulaari-inputilla jätä oletukseen.
CQ.patchPreset(\classic);      // banaanit JA nupit: tri1+tri3 -> spaf (syvyys 0.12),
                               // castle->castle FM-rinki
CQ.hold(1, 0);                 // coco1 record-tilaan
```

**Mikki tällä läppärillä:** sisäinen mikki ON `\liveL` (WASAPI-sisääntulolaite =
mikkiryhmä; `inChans[0]`). ÄLÄ vaihda `\preASrc`-arvoon `\mic` — configissa on
`micChan: nil`, joten `\mic`-lähde on hiljaisuus (SPEC §7: mic-polku kokonaan
pois; ja `micGain`-oletus on joka tapauksessa 0). Preamp A kuulee mikin
oletusasetuksella `\liveL`, tasona `liveGain` (oletus 1).

1. Kuuntele oletusarvoilla, koskematta mihinkään muuhun: pitäisi kuulostaa heti
   "Cocoquantus-maiselta" (SPEC M6). Mitä odottaa:
   - **Huojunta**: hidas ±50 sentin vire-ajelehdinta (~6–20 s aaltoina, tri1;
     aaltojen kesto vaihtelee chaosin ja FM-ringin mukana) + nopeampi ~3 Hz
     ±17 sentin flutter (tri3) — "kulunut mutta toimiva nauhuri", elävää,
     EI merisairasta. Kuuluu selvimmin sävelkorkeudellisesta/perkussiivisesta
     materiaalista; kohinamainen materiaali ei paljasta viredriftiä.
   - **Aliasointi + 8-bit-rae** materiaalin päällä koko ajan.
   - Ensimmäiset ~15–20 s: power-on-kohina kiertää vielä pohjalla ja hiipuu
     portaittain pois (fb 0.35, askel = loop-kierto ~5.5 s) — normaalia.
   - Dust-purskeet (oletus 0.02) rapisevat satunnaisina aaltoina pohjalla
     eivätkä lopu koskaan kokonaan — by design, hardwaren "digital dust"
     (A/B-varmistus: `CQ.set(\coco1Dust, 0)` hiljentää ne).
2. Inverso-banaani (coco1:een, jota tässä asetelmassa kuunnellaan — coco2 on
   mykistetty): `CQ.patch(\envA, \coco1_fbVca, -0.5); CQ.set(\coco1Fb, 0.9);`
   → puhu mikkiin (preamp A on jo `\liveL` = mikki; slotPre 0 pitää äänen
   poissa loopista, envA seuraa sitä silti) → loopin kaiut painuvat alas
   puhuessasi ja palaavat tauoilla (negatiivinen paino = inverso).
   Lopuksi: `CQ.unpatch(\envA, \coco1_fbVca); CQ.set(\coco1Fb, 0.35);`
3. `CQ.set(\patchDepth, 0);` → koko matriisin vaikutus katoaa pehmeästi (50 ms
   lag), ei klikkiä. `CQ.set(\patchDepth, 1);` → banaanit palaavat täyteen
   voimaan, yhtä pehmeästi.
4. `CQ.clearPatch;` → kaikki banaanit irti, takaisin puhtaaseen coco-ääneen.
5. Vapaa ~20 min koodi-only-sessio (ei GUI:ta vielä) — kokeile omia patcheja,
   esim. `CQ.patch(\cas3, \coco1_spaf, 0.7);`, `CQ.patch(\coco1, \coco2_fbVca, 0.4);`,
   `CQ.patch(\coco2, \coco1_fbVca, 0.4);` (banaani-pari = kahden coco-ristikaiun
   ducking). Kirjoita vain patch-rivejä ja tunnettuja `CQ.set`-kutsuja.

- [ ] M6 hyväksytty (pvm: )

## M7 — 30 min abuse-sessio (vaihe 7, tulossa)
## M8 — 16n-faderit (vaihe 5, tulossa)
