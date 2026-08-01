# Repon yksityisyys- ja lähdeauditointi

Päivä: 2026-08-01. Haara: `nocturne`. Tämä on `PLAN.md`:n "Tie 1.0:aan" kohta 5
ennen repon julkiseksi vaihtamista. Auditointi ei kuulu 2026-07-19:n jäädytettyyn
koodiauditointiin (`audit/README.md`), vaan on erillinen julkaisuportti.

**Kysymys:** sisältääkö repo henkilökohtaista tai koneriippuvaista tietoa —
**koko historiassa, ei vain työpuussa**? `.gitignore` ei poista sitä mikä on jo
committoitu, joten pelkkä työpuun tarkastus olisi vastannut väärään kysymykseen.

## Menetelmä ja kattavuus

Auditoitu kohde oli koko objektitietokanta, ei nykyinen tiedostopuu:

| Kohde | Määrä | Miten |
| --- | --- | --- |
| Commitit (kaikki refit) | 193 | `git log --all` |
| Tagit | 2 | `git for-each-ref` tagger-identiteetteineen |
| Seurattuja tiedostoja nyt | 167 | `git ls-files` |
| **Historian uniikkeja blobeja** | **1 433** | jokainen `git cat-file`illa auki ja grepattu |
| Historiassa koskaan lisättyjä polkuja | 167 | `git log --all --diff-filter=A` |
| Historiasta poistettuja polkuja | 3 | `comm` lisättyjen ja nykyisten erotuksena |

Haetut kuviot: absoluuttiset Windows-polut, `/home/`- ja `/Users/`-polut,
käyttäjä- ja henkilönimet, sähköpostiosoitteet, IP- ja MAC-osoitteet, konenimet,
laitenimet, API-avaimet ja tokenit (`ghp_`, `github_pat_`, `sk-`, `xox[baprs]-`,
`AKIA…`, `BEGIN … PRIVATE KEY`), sekä binäärit ja lisensoimattomat assetit.
Lisäksi tarkastettiin commit-viestit, CI-workflow, `.gitattributes`,
`.gitignore`-kattavuus ja työpuun seuraamattomat tiedostot.

## Löydökset ja korjaukset

### L1 — Windows-käyttäjätunnus ja henkilökohtainen kansiopuu (korjattu)

`audit/TEST_EVIDENCE.md` sisälsi viisi kertaa käyttäjätunnuksen paljastavan
absoluuttisen polun muodossa `C:\Users\<käyttäjä>\Documents\…\<repo>` (rivit 5,
108–110) ja `C:\Users\<käyttäjä>\AppData\Local\SuperCollider\archive.sctxar`
(rivi 239). Tämä oli auditoinnin ainoa varsinainen henkilötietovuoto.

Tunnus on tässä raportissa tarkoituksella kirjoitettu paikanpitäjänä: auditointi
ei julkaise uudelleen sitä mitä se poisti. Todellinen arvo on nähtävissä
`git show`illa vanhasta blobista, kunnes historia mahdollisesti kirjoitetaan
uudelleen (ks. avoin päätös alla).

**Korjaus:** polut korvattu paikanpitäjillä `<repo>` ja `%LOCALAPPDATA%`, ja
tiedoston alkuun kirjattu näkyvä huomautus korvauksesta. Jäljelle jätettiin
`C:\Program Files\SuperCollider-3.13.0\…` — se ei sisällä henkilötietoa ja on
itsessään näyttöä auditoidusta SuperColliderin versiosta. Yksikään ajettu komento,
mitattu tulos tai johtopäätös ei muuttunut.

### L2 — Henkilön etunimi ja oma Bluetooth-laite testifixturessa (korjattu)

`tests/classes/CQAudioConfigTest.sc` ja `tests/gui_audio_setup_smoke.scd`
sisälsivät neljä kertaa merkkijonon `;(<etunimi> - AirPods Pro)` — todellisen
Windows-endpointin nimi, jossa on laitteen omistajan etunimi.

**Korjaus:** `;(Studio - AirPods Pro)`. Fixturen tekninen arvo on siihen upotettu
CR/LF/TAB-patologia (`Char.ret ++ Char.nl ++ Char.tab`), jota testi vartioi — ei
nimi. Patologia säilyi sellaisenaan, joten testin kattavuus ei heikentynyt.

### L3 — `.gitignore`-kattavuuden aukko (korjattu)

`.claude/settings.local.json` oli suojassa **vain käyttäjän globaalilla**
ignore-tiedostolla (`~/.config/git/ignore`), ei repon omalla `.gitignore`lla.
Suojaus koski siis tätä konetta, ei tuoretta kloonia, CI-checkoutia eikä
mahdollista toista kehittäjää. Tiedosto sisältää paikallisia työkalulupia ja
absoluuttisia SuperCollider-polkuja.

**Korjaus:** rivi lisätty repon omaan `.gitignore`en. Muut `PLAN.md`:n kohdassa 5
nimetyt kohteet olivat jo katettuja ja se varmennettiin koneellisesti
(`git check-ignore -v`): `config.scd`, `tests/dev_paths.scd`,
`tests/sclang_conf_dev.yaml`, `samples/`, `recordings/`, `tests/tmp/`,
`/snapshots/*.cqsnap.scd`, `/presets/*.cqsnap.scd`,
`/presets/compass.cqcompass.scd`.

### L4 — Yhden koneen SuperCollider-polku kovakoodattuna portteihin (korjattu)

`tests/run_sclang_gate.ps1`, `tests/memory_probe.ps1` ja
`tests/run_soak_validation.ps1` käyttivät oletusarvona polkua
`C:\Program Files\SuperCollider-3.13.0\sclang.exe`. Tämä rikkoo `CLAUDE.md`:n
sääntöä ("Never hardcode absolute paths") ja oli lisäksi **versiopinni joka oli jo
vanhentunut**: CI ja `CONTRIBUTING.md` puhuivat versiosta 3.14.1.

**Korjaus:** uusi `tests/resolve_sclang.ps1` etsii tulkin järjestyksessä
`SC_LANG` → `tests/dev_paths.scd` (gitignoroitu, `CLAUDE.md`:n osoittama koti
konekohtaisille poluille) → uusin `Program Files\SuperCollider-*`. Eksplisiittinen
`-Sclang` voittaa yhä kaiken, joten CI-kutsut eivät muuttuneet. `CONTRIBUTING.md`:n
esimerkit eivät enää sisällä yhdenkään koneen polkua.

### L5 — Polkuesimerkki, jossa käyttäjätunnus oli havainnollistus (korjattu)

`tests/manual_checklist.md` ja `docs/history/manual_checklist_archipelago.md`
opettivat sclangin escape-karikkoa esimerkillä `"C:\tuoma\..."`. Esimerkin idea on
että `\t` = sarkain; `"C:\temp\..."` opettaa saman ilman käyttäjätunnusta.

### L6 — Kirjoittajan oman kirjaston tiedostonimi seuratussa koodissa (korjattu)

`tests/gui_banana_preview.scd` asetti deck-labelin arvoksi
`123681__aidenfry__cricket-and-bird-field-recording.wav`. Tiedostoa ei ladata —
merkkijono vain testaa pitkän nimen piirtoa — joten geneerinen Freesound-muotoinen
nimi ajaa saman asian. Manuaalichecklistien `CQ.deckA.load(...)`-rivit viittaavat
yhä samaan materiaaliin; ne ovat käyttäjän omia kuunteluohjeita, joten niihin
lisättiin esipuheeseen maininta, että `samples/` ei kulje repon mukana ja nimet
ovat esimerkkejä. Varsinainen "mitä paketissa lähtee" -päätös kuuluu `PLAN.md`:n
kohtaan 8 (soitettavuuskierros), ei tähän auditointiin.

## Varmennetusti puhdasta (ei toimenpiteitä)

Nämä ovat auditoinnin tulos siinä missä löydöksetkin — negatiivinen tulos on
todistettu, ei oletettu:

- **Ei salaisuuksia.** Ei API-avaimia, tokeneita, salasanoja eikä yksityisiä
  avaimia työpuussa eikä yhdessäkään 1 433 historiallisesta blobista. Kaikki
  `token`-osumat ovat `CQAudioReconfigure`n elinkaarigeneraatioita ja auditin
  omaa tekstiä.
- **Ei sähköpostiosoitteita tiedostojen sisällössä.** Osoitteet esiintyvät vain
  commit-metadatassa (ks. avoin päätös alla).
- **Ei IP- tai MAC-osoitteita, ei konenimiä** (`DESKTOP-*` / `LAPTOP-*`).
- **Ei koskaan committoituja henkilökohtaisia tiedostoja.** `config.scd`,
  `config.audio.scd`, `tests/dev_paths.scd`, `sclang_conf_dev.yaml`, `samples/`,
  `recordings/`, `tests/tmp/`, henkilökohtaiset snapshotit ja
  `compass.cqcompass.scd` eivät esiinny yhdessäkään historian 167 polusta.
  Levyllä olevat 20 äänitystä, 11 näytettä ja 4 omaa snapshottia ovat kaikki
  ignoroituja.
- **Ei binäärejä historiassa.** Suurin blob on `PLAN.md` (178 kt). Repo ei sisällä
  kopioitua Ciat-Lonbarde-materiaalia, kuvia, fontteja eikä kytkentäkaavioita —
  `SOURCES.md`:n väite pitää edelleen paikkansa.
- **Historiasta poistetut kolme polkua ovat projektimateriaalia:**
  `AUDIT_EVAL.md`, `AUDIT_PROMPT.md`, `presets/example.cqsnap.scd`. Mikään niistä
  ei ollut henkilökohtainen.
- **Commit-viestit ovat puhtaat.** Kaikki 193 vanhaa viestiä tarkastettiin.
  Ainoa toistuva metadatarivi on tarkoituksellinen `Co-Authored-By: Claude`, ja
  ainoa henkilöosuma on GitHubin automaattinen
  `Merge pull request #1 from VaahtoluotoTuomas/archipelago`, jossa on jo
  julkinen GitHub-käyttäjätunnus (sama kuin repon URL:ssä, README-badgessa ja
  remotessa).
- **Työpuu ei vuoda vahingossa:** seuraamattomia, ignoroimattomia tiedostoja on
  nolla, eli `git add -A` ei tänään lisäisi mitään henkilökohtaista.
- **`tests/evidence/mor1-morph-freeze-20260730.csv`** (77 riviä) sisältää vain
  prosessimittareita — PID, muisti, CPU, kahvat, säikeet. Ei polkuja, ei konenimiä.
- **CI-workflow** ei sisällä salaisuuksia eikä kovakoodattuja henkilökohtaisia
  polkuja; oikeudet on rajattu (`permissions: contents: read`).
- **Historiassa näkyy hylätty aiempi tuotenimi** ("Yölahti", `yolahti.git`).
  Vaaraton, mutta kirjattu tässä, koska se on julkisesti luettavissa.

## Laki, jonka tämä auditointi opetti itsestään

**Auditoinnin oma tuotos kuuluu auditoinnin piiriin.** Tämän raportin
ensimmäinen versio julkaisi uudelleen täsmälleen ne merkkijonot jotka se poisti
koodista — ja kun se korjattiin raportista, sama virhe toistui **commit-viestissä**
(`b293b61`, L2 lainasi fixturen sellaisenaan). Käyttäjä huomasi sen; automaattinen
sweep ei olisi, koska sweep ajettiin ennen kuin commit oli olemassa.

Kaksi seurausta, jotka pätevät jokaiseen tulevaan puhdistukseen tässä repossa:

1. **Redaktoitua arvoa ei toisteta missään** — ei raportissa, ei commit-viestissä,
   ei PR-kuvauksessa, ei checklistissä. Paikanpitäjä riittää aina; tarkka arvo on
   `git show`illa saatavissa niin kauan kuin se on historiassa.
2. **Sweep on ajettava uudelleen commitin jälkeen, ei vain ennen sitä.** Pelkkä
   työpuun tarkastus ennen committia jättää viestin katveeseen — ja viesti on yhtä
   pysyvä kuin blob.

Korjattiin `git commit --amend` + `--force-with-lease` -parilla, kun commit oli
minuutteja vanha: yksi kärki-SHA muuttui (`b293b61` → `91ced9b`), puu pysyi tavu
tavulta samana, tagit ja `main` eivät liikkuneet. Tämä on **eri asia** kuin alla
hylätty 193 commitin uudelleenkirjoitus, ja se ero on syytä pitää selvänä.

## Päätös: commit-sähköposti ja historian uudelleenkirjoitus

Tämä oli ainoa kohta jota **ei** voi ratkaista koodimuutoksella.
**Käyttäjä päätti 2026-08-01: vaihdetaan vain tuleva identiteetti,
historia jätetään ennalleen** (vaihtoehto 3 alla). Perustelut ja hinnat
säilytetään tässä, jotta valinta on luettavissa valintana.

| Identiteetti | Committeja | Missä |
| --- | --- | --- |
| `TuomasVaahtoluoto <…@student.hamk.fi>` | 192 / 193 | myös molemmat tagit |
| `Tuomas Vaahtoluoto <…@gmail.com>` | 1 | GitHubin tekemä merge-commit (PR #1) |

Tarkat osoitteet: `git log --all --format='%ae' | sort -u`.

Osoite on oppilaitossähköposti, joka on nimetyn korkeakoulun osoite ja jonka
elinkaari päättyy opintojen päättyessä. Vaihtoehdot:

1. **Säilytä.** Ei työtä, ei rikkoutuvia SHA-tunnisteita. Osoite jää julkiseksi
   pysyvästi, myös sen jälkeen kun se lakkaa toimimasta.
2. **Kirjoita historia uudelleen** (`git filter-repo --mailmap` tai vastaava)
   ennen public-vaihtoa. Samalla ajolla poistuisi myös L1:n käyttäjätunnuspolku
   vanhoista blobeista. **Hinta:** kaikki 193 commit-SHA:ta muuttuvat, molemmat
   tagit on luotava uudelleen, ja `origin` vaatii force-pushin. Repo on jo
   julkaistu GitHubiin, joten vanhat SHA:t voivat elää linkeissä.
3. **Välimuoto — VALITTU.** Jätä historia rauhaan ja vaihda vain tuleva
   identiteetti, jolloin 1.0-tagi ja sen jälkeinen työ kantavat pysyvää
   osoitetta.

### Mitä valinnasta seurasi

`git config --local user.email` osoittaa nyt pysyvään henkilökohtaiseen
osoitteeseen. Asetus tehtiin **repokohtaisena**, ei globaalina: käyttäjän muut
projektit eivät muuttuneet. Nimi `TuomasVaahtoluoto` jätettiin ennalleen, koska
päätös koski osoitetta.

Jos osoitettakaan ei haluta julkaista, GitHubin noreply-osoite on yhden komennon
päässä ja vaikuttaa vain tuleviin committeihin:
`git config --local user.email "<ID>+<käyttäjä>@users.noreply.github.com"`.

**Tietoisesti hyväksytty jäännösriski:** vanhoihin committeihin jäävät sekä
oppilaitososoite (192 committia + molemmat tagit) että L1:n käyttäjätunnuksen
sisältävä polku `audit/TEST_EVIDENCE.md`:n vanhassa blobissa. Molemmat
poistuisivat vain historian uudelleenkirjoituksella, jonka hinta (193 muuttuvaa
SHA:ta, tagien uudelleenluonti, force-push jo julkaistuun repoon) arvioitiin
hyötyä suuremmaksi. **Tämä on kirjattu valinta, ei auditoinnista unohtunut
kohta** — ja se on peruutettavissa myöhemmin, koska historian puhdistus ei
vanhene.

## Portit

Muutokset koskivat testifixturea, dokumentteja, `.gitignore`a ja PowerShell-
polunratkaisua; **DSP:hen ei koskettu**, joten `CQ_HOLD_OK` kantaa edellisestä
portista.

- `CQ_COMPILE_OK`
- **13 699 unit-passia / 0 failurea / 39/39 luokkaa** (`CQ_TESTS_PASSED`)
- `CQ_SAFETY_PASSED`
- `CQ_GUI_AUDIO_PASSED` (kantaa muuttuneen `;(Studio - AirPods Pro)` -fixturen)

Portit ajettiin ilman `-Sclang`-argumenttia, joten sama ajo todistaa myös L4:n
uuden polunratkaisun toimivaksi.
