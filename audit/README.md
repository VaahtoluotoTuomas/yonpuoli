# Audit-hakemiston lukujärjestys

Auditointi tehtiin kahdessa vaiheessa: ensin laaja lähdekoodi- ja
dokumentaatioauditointi, sitten riippumaton evaluointi ja reproducer-testit. Tästä
syystä alkuperäisissä raporteissa voi olla vakavuusluokkia, jotka myöhempi evidenssi
korvaa.

## Ajantasainen päätös- ja toteutustaso

1. [`FINAL_AUDIT.md`](FINAL_AUDIT.md) — riippumattoman evaluoinnin lopputulos ja
   korjatut vakavuusluokat.
2. [`VERIFIED_FINDINGS.md`](VERIFIED_FINDINGS.md) — varmennetut löydökset,
   reproducerit ja rajaukset. Tämä voittaa ristiriidassa alkuperäisen löydösraportin.
3. [`REMEDIATION_ROADMAP.md`](REMEDIATION_ROADMAP.md) — korjausten ja release-porttien
   toteutusjärjestys.
4. [`GUI_DEVELOPMENT_PLAN.md`](GUI_DEVELOPMENT_PLAN.md) — `GUI_SPEC.md`:stä johdettu
   Archipelago-GUI:n file-, dependency-, test-, riski- ja commit-suunnitelma.

Projektin tiivis ajantasainen työjärjestys on juuren [`PLAN.md`](../PLAN.md)-
tiedostossa. GUI:n normatiivinen lähde on aina [`GUI_SPEC.md`](../GUI_SPEC.md), ei
audit-raportti.

## Evidenssi ja alkuperäiset raportit

- [`TEST_EVIDENCE.md`](TEST_EVIDENCE.md) — komentojen, testien ja ympäristön evidenssi.
- [`FINDINGS.md`](FINDINGS.md) — ensimmäisen auditin yksityiskohtaiset löydökset;
  vakavuuksissa historiallinen.
- [`EXECUTIVE_SUMMARY.md`](EXECUTIVE_SUMMARY.md) — ensimmäisen auditin yhteenveto;
  P0/P1-luvut ovat historialliset.
- [`FEATURE_TRACEABILITY.md`](FEATURE_TRACEABILITY.md) — ominaisuuksien jäljitettävyys.
- [`COCOQUANTUS_FIDELITY.md`](COCOQUANTUS_FIDELITY.md) — uskollisuus- ja
  tulkinta-analyysi.
- [`IMPROVEMENT_BACKLOG.md`](IMPROVEMENT_BACKLOG.md) — laajempi parannusbacklog.
- [`LISTENING_TEST_PROTOCOL.md`](LISTENING_TEST_PROTOCOL.md) — kuuntelun
  hyväksyntäprotokolla.

Alkuperäiset toimeksiannot säilytetään toistettavuutta varten hakemistossa
[`prompts/`](prompts/). Raporttien historialliset `git status` -tulosteet ovat
evidenssiä auditointihetkestä eikä niitä päivitetä vastaamaan nykyistä tiedostopuuta.
