# Riippumattoman arvioinnin alkuperäinen prompti

Toimi nyt tämän auditoinnin riippumattomana pääarvioijana.

Älä käytä uusia subagentteja, ellei jokin yksittäinen rajattu tekninen tarkistus sitä ehdottomasti vaadi. Tämän vaiheen tavoitteena on yhtenäinen harkinta, löydösten varmennus ja väärien positiivisten havaintojen poistaminen.

Lue kaikki `audit/`-hakemiston raportit sekä alkuperäinen lähdekoodi.

Tee seuraavat asiat:

1. Tarkista itsenäisesti jokainen P0- ja P1-löydös suoraan lähdekoodista.
2. Suorita jokaiselle mahdollinen kohdennettu testi tai toistamisyritys.
3. Tarkista otos P2-löydöksistä ja arvioi auditoinnin yleinen luotettavuus.
4. Hylkää tai alenna löydökset, joilla ei ole riittävää evidenssiä.
5. Erota todelliset viat:

   * tietoisista suunnitteluratkaisuista
   * Ciat-Lonbarde-henkisestä hallitusta epävakaudesta
   * puhtaasti esteettisistä mieltymyksistä
6. Tarkista, etteivät ehdotetut korjaukset vahingossa steriloi instrumenttia, poista emergenttiä käyttäytymistä tai muuta sitä geneeriseksi delay/reverb-plug-iniksi.
7. Tarkista erityisesti:

   * server tree ja node/group-luontijärjestys
   * start/stop/restart-elinkaari
   * feedbackin vakaus ja audioturvallisuus
   * buffer-, bus-, OSC-, clock-, routine- ja GUI-resurssien vapautuminen
   * ominaisuusmatriisin kattavuus
   * GUI-ohjainten ja DSP-parametrien todellinen yhteys
   * `Space`-parametrin luonne ja suhde Coco-arkkitehtuuriin
8. Arvioi GUI-kehityssuunnitelma uudelleen kokonaisarkkitehtuurin ja instrumentin estetiikan näkökulmasta.
9. Ratkaise auditointiraporttien väliset ristiriidat.
10. Älä muuta lähdekoodia vielä.

Luo tai päivitä:

* `audit/FINAL_AUDIT.md`
* `audit/VERIFIED_FINDINGS.md`
* `audit/REMEDIATION_ROADMAP.md`
* `audit/GUI_DEVELOPMENT_PLAN.md`

`VERIFIED_FINDINGS.md`:ssä saa olla vain löydöksiä, joiden evidenssi on tarkistettu.

`REMEDIATION_ROADMAP.md`:ssä jaa työ pieniin, järjestettyihin kokonaisuuksiin:

* Milestone 0: turvaverkot ja testit
* Milestone 1: P0-korjaukset
* Milestone 2: P1-korjaukset
* Milestone 3: ominaisuuksien täydentäminen
* Milestone 4: arkkitehtuurin selkeyttäminen
* Milestone 5: DSP:n ja Coco-uskollisuuden parantaminen
* Milestone 6: GUI:n rakenteellinen parantaminen
* Milestone 7: visuaalinen ja esteettinen viimeistely
* Milestone 8: regressio- ja kuuntelutestaus

Anna jokaiselle työvaiheelle:

* tavoite
* muutettavat tiedostot
* riippuvuudet
* hyväksymiskriteerit
* testit
* regressioriskit
* arvio siitä, voiko muutoksen tehdä erillisenä commitina

Lopuksi anna tiivis kokonaisarvio:

* onko projekti nykytilassaan käyttökelpoinen
* mitkä ongelmat estävät julkaisemisen
* mitkä puutteet ovat teknisiä
* mitkä ovat konseptuaalisia
* mitkä ovat vain valinnaisia parannuksia
* mitkä nykyiset ratkaisut ovat erityisen onnistuneita ja tulee säilyttää
