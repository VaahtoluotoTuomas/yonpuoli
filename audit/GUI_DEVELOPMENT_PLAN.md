# Archipelago-GUI:n toteutussuunnitelma

Päivitetty: 2026-07-19
Autoritatiivinen visiospesifikaatio: [`../GUI_SPEC.md`](../GUI_SPEC.md)

Tämä tiedosto muuttaa `GUI_SPEC.md`:n toteutusjärjestykseksi. Se ei tulkitse
spesifikaatiota vapaasti eikä korvaa sitä. Jos tekninen toteutus edellyttäisi
muutosta määriteltyyn eleeseen, näkymärakenteeseen, vakioon, turvallisuusreunaan,
värilakiin tai liikkeeseen, työ pysähtyy ja kompromissi hyväksytetään käyttäjällä
ennen lähdekoodimuutosta.

## Auktoriteettijärjestys

1. `SPEC.md` määrää parametrit, arvovälit, DSP:n ja turvallisuuden; sen §8 voittaa
   kaikki ristiriidat.
2. `GUI_SPEC.md` määrää uuden GUI:n käyttäytymisen, rakenteen, eleet, vakioarvot,
   testattavat reunat ja visuaalisen kielen.
3. `ARCHITECTURE.md` määrää pure-view/funnel/ownership-koodirajat.
4. Tämä suunnitelma määrää työjärjestyksen ja portit.

Tästä seuraa yksi eksplisiittinen ristiriitaratkaisu: nykyinen `CQ.shake` säilyttää
SPEC §5.5:n käytöksen eli muuttaa matriisipainoja ja spesal-tiloja mutta ei
CQSpec-parametreja. Compass, grow, wither ja breed muuttavat vain matriisipainoja.

## Aiemmasta GUI-suunnitelmasta poistuvat oletukset

- Uudessa GUI:ssa on **yksi ikkuna, kaksi näkymää, yksi pysyvä Master-strip ja yksi
  Drawer**. Kolmatta Play/Patch/X-ray-päänäkymää ei tehdä.
- Day säilyttää nykyiset neljä instrumenttisaraketta; Master on erillinen pysyvä
  strip. Vanha 16×22-grid siirtyy Drawer/Inspector-välilehteen.
- Night on Archipelago: ei nuppeja eikä numeroita. Inspector on ainoa
  täsmämuokkaus-/ledger-pinta.
- Minimikoko on `GUI_SPEC.md` §16.10:n 1280×720. Aiempi 1024×768-tavoite,
  compact-breakpointit ja näkymien vapaa uudelleenpinoaminen eivät kuulu tähän
  toteutukseen.
- Nykyinen non-focusable-widget-laki säilyy. Erillistä keyboard-navigation-tilaa ei
  lisätä; uudet globaalit näppäimet ovat vain Tab, Esc ja Ctrl+Z spesifikaation mukaan.
- Advanced-paneelia tai kahdeksan API-only-parametrin GUI-ohjaimia ei lisätä.
  Erityisesti Space pysyy minimaalisena eikä saa Night-bodya ennen erillistä
  Space-konseptipäätöstä.
- Visuaalinen työ ei ole vapaa “polish”-vaihe: Ink & Phosphor, paletti, viivat,
  glyphit, organismit, state table ja motion-laki ovat normatiivisia alusta asti.

## Nykyinen tekninen lähtökohta

- `CQMatrix.patch` clampaa yhden solun, puskee yhden destination-rivin ja lähettää
  `changed(\patch, src, dest, w)`-ilmoituksen. `load`, `clearPatch` ja `shake`
  käyttävät bulk-pushia ja yhtä yleistä `\clear`-ilmoitusta.
- `CQMatrix.asArray` antaa sparse-triple-muodon ja `load` osaa palauttaa sen. Compass,
  undo, breed ja spellbook vaativat tätä täsmällisemmän full-matrix-/transaction-
  sopimuksen, mutta API:n nimi ja sisäinen muoto ovat toteuttajan valittavissa.
- `CQGui` rakentaa nykyisen ikkunan yhdellä deferred-callbackilla. Auditissa
  varmennettu deferred-init-resurrection korjataan ennen Archipelago-resurssien
  rakentamista.
- `CQGuiMatrix` on käyttökelpoinen inspectorin lähtökohta, mutta sen eleisiin lisätään
  touch-and-listen sekä yhteinen audition-kirjanpito.
- Nykyiset `/cq_cocometers`, `/cq_castle`, `/cq_gr`, `/cq_spesal` ja `/cq_hot`
  säilyvät. Uusia serveriviestejä ovat vain `/cq_aura`, loop phase ja `/cq_charge`.
- Nykyiset Day-ohjaimet, deck-drop, meters, LEDs, spesal-display, shake amount,
  keyboard map ja Master-toiminnot säilytetään, ellei `GUI_SPEC.md` nimenomaisesti
  siirrä niitä Draweriin tai toolbarille.

## Ennen G1:tä: pakolliset perustat

Nämä eivät ole uusi GUI-vaihe vaan turvallinen aloitusjärjestys.

### F0.1 Release- ja lifecycle-riippuvuudet

- CQ-AUD-001:n output-kanavavalidointi korjataan ennen mitään uutta julkaistavaa
  buildiä.
- Session generation/rollback (CQ-AUD-004) valmistuu ennen G1:n hyväksyntää, koska
  audition pitää varmasti palauttaa myös window close-, GUI free-, CmdPeriod- ja
  panic-poluilta.
- Recorderin P1-tilakone valmistuu ennen uuden pysyvän Master-stripin REC-kytkentää.
  GUI ei saa peittää recorder-kilpaa omalla paikallisella booleanilla.
- GUI:n deferred-init saa eksplisiittisen peruutettavan lifecycle-tilan ennen uuden
  overlayn, taskien tai OSCdefien lisäämistä.

### F0.2 Vihreä baseline

Tallenna ennen GUI-muutoksia:

- `CQ_COMPILE_OK`;
- kaikki serverless unit-testit;
- `CQ_SAFETY_PASSED`;
- `CQ_HOLD_OK`;
- nykyisen GUI:n serverless build/close-smoke ja keyboard/deck-drop-fixturet.

Qt-testi ilmoittaa `SKIP: no display` eksplisiittisesti. Skip ei ole pass.

### F0.3 Matriisin transaction-sopimus

Lisää testit ennen API-muutosta. Sopimuksen on mahdollistettava:

1. syvä full-matrix-read undoa, compass-stamppeja ja breediä varten;
2. kaikkien 352 solun clampattu batch-write yhdellä server-push-kierroksella ja yhdellä
   dependant-ilmoituksella;
3. committed-operaation ja transientin audition-writejen erottaminen niin, etteivät
   auditionit päädy undoon tai spellbookiin;
4. operation boundary: plant/rip/move/flip/harvest/shake/GUI-preset/breed/yksi
   grow- tai wither-hold/compass-release = kukin yksi op;
5. code-, preset- ja shake-kirjoitusten näkyminen spellbookin matrix-notification-
   streamissä ilman että GUI koukuttaa jokaista kutsupaikkaa;
6. code-layer-kirjoitusten jääminen GUI-local-undon ulkopuolelle;
7. yhden solun `patch`-API:n ja nykyisen snapshot triple -formaatin säilyminen.

API:n nimeä tai dense/sparse-sisämuotoa ei lukita tässä. Käyttäytyminen ja ilmoitusten
määrä lukitaan testeillä.

### F0.4 Audition-tilakone serverittömänä

Tee yksi GUI-model-/interaction-testattava kirjanpito tietueelle
`{src, dest, prevW}`. Testaa kaikki `GUI_SPEC.md` §4.1:n revert-polut:

- candidate-radius poistuu;
- candidate vaihtuu;
- plant;
- Esc;
- release voidissa;
- Day/Night-toggle;
- window close;
- GUI free;
- CmdPeriod;
- `CQ.panic`.

Jokainen testi vertaa koko matriisia lähtötilaan. Ei timer-pohjaista cleanupia.

## G1 — Day skin

G1 toteuttaa täsmälleen `GUI_SPEC.md` §13:n ensimmäisen portin.

### G1.1 Ink kit ja yksi ikkunakuori

- Rakenna §16.11:n yhteinen Ink kit: yksi palettilähde, stroke-vakiot, seeded wobble,
  blob/socket/jack/plug/lamp/glyph/cable-renderöinti.
- Kaikki glyphit ovat 8×8 Pen-patheja; ei Unicode-ikoneita, kuvatiedostoja,
  icon-fontteja tai ulkoisia assetteja.
- Session seed tekee wobblesta/asymmetriasta redrawin yli pikselivakaan.
- Ikkunakuori sisältää Day/Night-sisältöalueen, molemmissa identtisen Master-stripin
  ja clickillä avautuvan Drawer-alueen, joka ei koskaan peitä Masteria.
- Tab on hard cut vain ilman text-focusa. Drawerilla ei ole uutta keybindingia.
- Vähimmäiskoko 1280×720; fontti- ja stroke-koot pysyvät pikseleinä.

Ensimmäinen visuaalinen hyväksyntä tehdään tässä: paper `#F4EDE0`, night
`#12141C`, normatiivinen paletti, kontrastit ja forbidden-cheapness-lista tarkistetaan
ennen organismien monistamista.

### G1.2 Day-skin sockets ja overlay

- Rekisteröi kaikki 16 source jackia ja 22 destination socketia niiden §3.1/§3.2
  Day-koteihin.
- Socket on 4–6 px syötettävän ohjaimen vieressä; Proximity is meaning.
- Window-spanning cable overlay piirtää gravity-sagged kaaret ja stacked plugs mutta
  hit-testaa normaalisti vain jackit, socketit ja kaapelit. Ainoa poikkeus on
  §4.4:n harvest: oikean napin drag saa alkaa vain aidosti tyhjältä pohjalta.
- Target on vähintään 12 px; magnetic candidate halo näyttää ainoan ehdokkaan.
- Overlay ei saa varastaa knob-, slider-, button-, deck-drop- tai Master-eleitä.
- Jackin väri kertoo perheen, kaapelin amber/blue polariteetin ja plug depth/width
  painon. Inversolla on barbs; merkitys ei saa perustua yksin hueen.

### G1.3 Yhteinen banana grammar

Toteuta yksi interaction state machine, jota Day, Night ja Inspector käyttävät:

- left/right source press = verso/inverso banana in hand;
- proximity audition `handStrength × closeness²`, enintään yksi live preview;
- release candidateen = WYHIWYG-plant; voidiin = revert;
- Shift-release = plant ja source jää käteen;
- planted plug/cable grab = move tai voidiin yank/rip;
- right-click = instant rip;
- right-drag tyhjältä pohjalta = harvest-lasso; alle 8 px liike on no-op;
- lasso suljetaan virtuaalisella chordilla ja testaa planted-kaapelipolun noin 12 px
  välein endpointit mukaan lukien; osumien preview on §16.8:n danger-under-glow;
- release harvestoi koko catchin yhtenä batch commit/undo/spellbook-operaationa;
  empty catch ei tee entryä ja Esc peruu muuttamatta matriisia;
- wheel = 0.05, Ctrl 0.01, sign säilyy, floor 0.05;
- Alt-click = polarity flip;
- double-click jack/socket/body = solo-light; Esc vapauttaa;
- Ctrl+Z = GUI-local 32-op full-matrix undo.

Kaikki kirjoitukset kulkevat `CQMatrix`-funnelin kautta. Plant sitoo täsmälleen
auditionissa kuullun painon. Audition-banaani ei kuulu harvest-joukkoon. Audition tai
compass-frame ei koskaan tee undo-entryä.

### G1.4 Drawer: Inspector, Spellbook, Memory

- Siirrä nykyinen grid Drawerin Inspector-välilehteen; säilytä vertical fine edit ja
  right-click clear.
- Lisää touch-and-listen: press audition ±0.3 releaselle asti, double-click plant 0.3,
  yhteinen §4.1 revert-kirjanpito.
- Spellbook kuuntelee matrix notification streamiä, ei GUI:n gesture handleria.
  Se näyttää kaikkien kerrosten committed matrix-opit, ei audition/frame-writeja.
- Jokainen line on kopioitavaa `CQ.patch(...)`-kieltä. Batch-operaatio on yksi
  tool-commentti ja sen diffatut cell-rivit; harvest tuottaa `// harvest` +
  `CQ.patch(\src, \dest, 0)` -rivit. `copy all as preset data` tuottaa
  `(matrix: [[src, dest, w], ...])` clipboardiin.
- Siirrä snapshot save/recall sekä A→B morph Drawerin Memory-välilehteen muuttamatta
  SPEC §7:n semantiikkaa tai mic-politiikkaa.
- Patch-preset-menu siirtyy Night-toolbarille ja Inspector-headeriin.

### G1.5 Day-declutter ja pysyvä Master

- Day säilyttää neljä nykyistä saraketta ja niiden muscle memoryn.
- Siirrä vain `GUI_SPEC.md` §5:n nimeämät number boxes, snapshotit ja morph Draweriin.
- Deck-drop ja filename, meters, LEDs, spesal display sekä shake+amount säilyvät.
- Master-strip on identtinen paikassa ja toiminnassa Day/Night: vol, dry, space,
  depth, GR, notice, REC+elapsed ja PANIC.
- Masteria ei peitetä, dimmata tai siirretä. Muted-tila käyttää §16.8:n punaista
  filliä ja rulea mutta jättää tilat luettaviksi.
- REC näyttää runtime-tilakoneen suoraan: idle = ontto danger-rengas; starting… ja
  stopping… = staattinen okrateksti elapsed-paikalla ja reagoimaton nappi; recording =
  täytetty rengas + elapsed; error = ontto rengas piirretyllä vinoviivalla ja syy
  notice-rivillä danger-punaisena, kuittaus seuraavalla REC-painalluksella.
- Vanhat performance-näppäimet pysyvät globaaleina; Tab, Esc ja Ctrl+Z lisätään ilman
  törmäyksiä.

### G1.6 `/cq_aura` ja informatiivinen liike

- Lisää 16 source magnituden canonical-order `/cq_aura` noin 15 Hz:llä.
- Aura on read-only-telemetria; se ei muuta DSP:tä, parametreja tai safety-rajoja.
- Jack glow, cable alpha, root/castle/body glow käyttävät samaa dataa.
- Ambient redraw enintään 15 Hz, active gesture enintään 60 Hz, ei whole-window-
  repaintteja. GUI CPU -tavoite on noin ≤3 % referenssiläppärillä ja koko projektin
  ≤25 % envelope säilyy.

### G1-portti

Automaatio:

- kaikki neljä nykyistä markeria;
- batch-write clamp + yksi notification;
- 16/22 inventory ja canonical mapping;
- kaikki audition-revert-polut matriisiyhtäläisyydellä;
- undo-operaatiorajat ja ring depth 32;
- harvest-geometria: <8 px no-op, closing chord, noin 12 px cable sampling,
  endpoint-osumat, preview enter/leave, empty catch ja Esc; audition-banaani ulos;
- harvest-release = yksi notification/commit/undo/spellbook-operaatio ja Ctrl+Z
  palauttaa koko catchin;
- spellbook: GUI/code/preset/shake/harvest sisään, audition/compass-frame ulos;
- Day/Night/Drawer/Master build-close-free ilman resurrectionia;
- overlay hit-test ei estä jokaista nykyistä widget/drop-elettä;
- headless ei rakenna Qt:ta.

Kuunteluportti:

1. planttaa `tri3 → coco1_spaf` kuulemalla swell ennen releasea;
2. move, yank/rip, wheel-trim, polarity flip ja Ctrl+Z;
3. lasso-harvestoi useita banaaneja, peru koko catch yhdellä Ctrl+Z:lla ja tarkista
   `// harvest` -spellbook-entry;
4. varmista WYHIWYG-paino Inspectorista;
5. testaa panic ja CmdPeriod kesken auditionin: matriisi palautuu.

G1 commitataan vasta tämän portin ja käyttäjän visuaalisen Day-skin-hyväksynnän
jälkeen.

## G2 — Night map: Archipelago

### G2.1 Topologia ja organismit

- Viisi castlea ovat seeded-epäsymmetrisessä oikean järjestyksen pentagonissa,
  castle 1 ylhäällä; ring arrows/fireflies näyttävät `k → k+1` LocalIn-kierron.
- Coco 1 vasemmalla ja Coco 2 oikealla 2.5-turn Archimedean spiraaleina.
- envA/envB ovat roots alhaalla, deckA/deckB birds ylhäällä.
- Juurten vieressä on ei-patchattava mic sprout: ei piirtoa ilman `micChan`-asetusta,
  whisper-silmu gainilla 0 ja danger-punainen silmu+kärkirengas vasta kun
  `micChan` on asetettu ja `micGain > 0`; sillä ei ole jackia tai hit targetia.
- Kaikki jackit, moons, homes, clock-angle-sijainnit, glyphit ja state-lookit tulevat
  suoraan §3, §6 ja §16:sta.
- Nightissa ei ole nuppeja eikä numeroita. Sallitut tekstit ovat toolbar, bird-
  filenames ja i/b/a display.
- Space ei saa bodya.

Koordinaatteja saa säätää silmällä vain §15:n sallimissa rajoissa: ring order/direction,
Coco left/right, roots below, birds above ja vähintään 24 px socket center distance
ovat invariansseja.

### G2.2 Loop phase ja charge

- Lisää loop-phase 0..1 joko nykyiseen `/cq_cocometers`-viestiin tai omaan viestiin;
  valinta ei muuta spec-semanttiikkaa.
- Read-head spark seuraa todellista vaihetta; flip kääntää spark/chevron-suunnan ja
  Hold sulkee uloimman kehän danger-rediksi.
- Lisää `/cq_charge`: 9 gate/trig-destination summaa post-weight/pre-Schmitt noin
  15 Hz. Kanoninen järjestys on `coco1_flip`, `coco1_skip`, `coco2_flip`,
  `coco2_skip`, `castle1_spesal` … `castle5_spesal`. Lampun alpha näyttää etenemisen
  kohti +0.5-rajaa, gate jää päälle Schmitt-tilassa ja trig saa 120 ms spark-bloomin.
- Telemetriat voidaan yhdistää pakettiin vain, jos payload mapping ja testit pysyvät
  yksiselitteisinä.

### G2.3 Solo-light, birds ja toolbar

- Double-click body/jack/socket tekee 150 ms solo-lightin; vain yhteydet kohteeseen/
  lähteestä jäävät kirkkaiksi, muut 25 % alphaan. Esc vapauttaa.
- Bird käyttää nykyistä rekursiivista deck-drop-plumbingia; drag-hover saa ainoan
  dashed rectangle -korostuksen ja filename näkyy monospacena.
- Toolbar sisältää vain patch-preset, shake, grow, wither, breed, amount mini-knob ja
  compass placeholderin/valmiin padin G3:a varten.
- Shake amount on sama GUI-local arvo kuin Day-buttonilla ja `p`-näppäimellä.

### G2-portti

- Screenshot/state-table-testit Day/Night paletteille ja kaikille jack/socket/body-
  tiloille;
- aura/phase/charge payload order ja cleanup;
- telemetry ≤15 Hz, redraw discipline ja CPU-mittaus;
- deck-bird drop kaikilla child-hit-alueilla;
- hard-cut Tab, persistent Master ja audition-revert view togglessa;
- ei numeroita, image assetteja, decorative motionia tai Space-bodya Nightissa.

Kuunteluportti: patchaa flip-banana ja seuraa charge-lampun latausta sekä Schmitt-
laukaisua; varmista castle-ring-suunnan vastaavan `\classic` FM-ringiä sekä kuultuna
että nähtynä.

## G3 — Weather machine

### G3.1 Compass

- Neljä corner-stampia ovat weights-only full-matrix-muisteja; empty corner = zero
  matrix, right-click clear.
- Left-drag tekee bilinear blendin enintään 60 Hz batch-writeina; release on yksi
  undo-op.
- Right-drag on elastic: release palauttaa pre-gesture-matriisiin noin kolmessa
  framessa eikä tee undo-entryä.
- `patchDepth` ei muutu.
- Cornerit autosave/autoloadataan `presets/compass.cqcompass.scd`-tiedostoon pure data
  expressionina, integer versionilla, validation+clampilla ja malformed-warningilla.

### G3.2 Grow ja wither

- Hold-repeat noin 2.5 Hz.
- Grow valitsee random zero-cellin ja antaa random signillä `U(0.15, 0.4)`-painon.
- Wither valitsee patched-cellin, kertoo 0.7:llä ja nollaa kun `|w| < 0.05`.
- Yksi yhtäjaksoinen hold on yksi undo-op. Ei seed-UI:ta.

### G3.3 Breed

- Breed → click kaksi corneria → current matrix.
- Jokainen celli coin-pickkaa parentin; p=0.08 mutaatio ±U(0,0.15); clamp funnelissa.
- Koko breed on yksi undo-op ja spellbookiin päätyvä committed matrix-muutos.

### G3-portti

- Bilinear corner/edge/center-testit;
- ≤60 Hz ja yksi notification/frame;
- left commit vs right elastic exact matrix equality;
- persistence valid/missing/malformed/version/clamp;
- deterministic test-randilla grow/wither/breed distribution ja rajat;
- hold = yksi undo-op, ei yksi per tick;
- mikään weather-ele ei muuta CQSpec-parametreja tai compassissa spesalia;
- full compile/unit/NRT safety/Hold ja GUI CPU/memory soak.

Kuunteluportti: stamp kaksi erilaista junglea, crossfade ne yhdellä liikkeellä,
varmista elastic snapback ja kuuntele että breed-child eroaa mutta “rhymes”.

## Visuaalinen hyväksyntä jokaisessa portissa

Jokainen G1–G3-portti tarkistaa, ei vasta lopussa:

- vain §16.2:n keskitetty paletti; ei inline-värejä;
- 1/1.5/2/3 px line hierarchy, round caps/joins;
- organismien seeded wobble/asymmetry, työkalujen puhdas geometria;
- sans labels + mono numbers/spellbook/filenames; lowercase paitsi PANIC/REC/HOLD;
- glyphit Pen-patheina;
- kontrasti text ≥4.5:1 ja large marks ≥3:1;
- state table ja motion-taulukon tarkat tilat/nopeudet;
- yksi sallittu shadow vain Drawerin reunassa;
- no cheapness smells -lista kokonaan;
- squint test kahdesta metristä: patch, syvyys, hot, hold/panic/rec erottuvat.

## Avoimet tekniset saumat — ei lupaa poiketa

Nämä mitataan ensimmäisellä relevantilla prototyypillä:

- Jos mouse-rate weight-write zipperöi, kokeile ensin GUI-writejen ohennusta noin
  30 Hz:iin tai client-side glideä. Server-side lagia ei lisätä ilman käyttäjän
  hyväksyntää ja norns-dietin arviointia.
- Jos overlay ylittää CPU-budjetin, staattinen ja animoitu piirto voidaan erottaa.
  Kaapelin ulkoasu, informatiivinen pulssi tai hit-law ei muutu ilman hyväksyntää.
- Aura/charge message merge ja Nightin tarkat koordinaatit ovat toteuttajan vapaita
  vain `GUI_SPEC.md` §15:n rajojen sisällä.
- Frontend-luokkien nimet ja jako ovat vapaita, mutta §16.11:n yksi Ink kit sekä
  pure-view/funnel/lifecycle-lait eivät ole.

## Ratkaistut spesifikaatiosaumat

Käyttäjä ratkaisi kaikki neljä saumaa 2026-07-19, ja päätökset ovat normatiivisina
`GUI_SPEC.md`:ssä:

1. **Charge:** 9 kanavaa; matriisin destination-järjestys suodatettuna gate/trigeihin,
   `coco1_flip`, `coco1_skip`, `coco2_flip`, `coco2_skip`, castle 1–5 `spesal`.
2. **Harvest:** kuuluu G1:n yhteiseen kielioppiin §4.4:n freehand-lassona. Endpointit
   osallistuvat, audition ei; release on yksi batch commit/undo/spellbook-operaatio.
3. **Mic sprout:** display-only, ei 17. sourcea tai jackia; kolme §16.6:n tilaa.
4. **REC:** §16.8:n staattiset starting/stopping-lookit sekä vinoviivallinen error,
   danger-notice ja seuraavan REC-painalluksen kuittaus.

Saumat eivät enää estä toteutusta. Mahdollinen uusi tekninen ristiriita käsitellään
`GUI_SPEC.md`:n normaalilla etukäteishyväksynnällä.

## Commit- ja handoff-järjestys

1. Test-only baseline + matrix transaction/audition contracts.
2. Matrix full-read/batch-write/notification foundation.
3. Session/GUI lifecycle prerequisites.
4. G1 Ink kit + shell.
5. G1 Day sockets/overlay/banana grammar.
6. G1 Drawer/Inspector/Spellbook/Memory.
7. G1 aura + portti + hyväksyntä.
8. G2 Night organisms.
9. G2 phase/charge/solo/birds + portti + hyväksyntä.
10. G3 Compass+persistence.
11. G3 grow/wither/breed + portti + hyväksyntä.
12. Täysi release-/kuuntelu-/CPU-/resource-soak ja dokumentaation statuspäivitys.

Committeja saa pilkkoa pienemmiksi, mutta G-porttia ei merkitä valmiiksi eikä tagata
ilman `GUI_SPEC.md` §13:n automaattista ja manuaalista hyväksyntää. Seuraavan session
ensimmäinen koodityö on F0.2–F0.4:n testiperusta, ei Night-organismien piirtäminen.
