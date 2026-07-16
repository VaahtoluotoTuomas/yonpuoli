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

- [ ] M3 hyväksytty (pvm: )

## M4 — \selfNoise-preset (vaihe 6, tulossa)
## M6 — \classic kenttä-äänitteellä (vaihe 4, tulossa)
## M7 — 30 min abuse-sessio (vaihe 7, tulossa)
## M8 — 16n-faderit (vaihe 5, tulossa)
