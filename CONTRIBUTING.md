# Contributing to Yölahti

Thank you for considering a contribution. Yölahti is currently a maintainer-led
experimental instrument in active Nocturne development. Small, focused bug fixes,
tests, documentation improvements, platform reports, and reproducible musical or
interaction findings are especially useful.

## Before opening a change

- Read [`README.md`](README.md), [`PLAN.md`](PLAN.md), and the relevant normative
  document: `SPEC.md` for behavior and safety, `GUI_SPEC.md` for GUI work, and
  `ARCHITECTURE.md` for code ownership.
- Open an issue before proposing a new parameter, changed range, new dependency,
  public gesture, persistent format, or safety-chain modification.
- Do not copy Ciat-Lonbarde artwork, panel graphics, documentation, audio, or other
  third-party assets into the repository. Add the source and license of any
  permitted third-party material to [`SOURCES.md`](SOURCES.md).

## Development setup

The reference environment is Windows with SuperCollider 3.13 or newer. Copy
`config.example.scd` to the ignored `config.scd`, then add `<repo>/classes` and
`<repo>/tests/classes` to the SuperCollider language include paths.

For isolated headless runs, create a local YAML file such as
`tests/sclang_conf_dev.yaml`:

```yaml
includePaths:
  - "C:/path/to/yolahti/classes"
  - "C:/path/to/yolahti/tests/classes"
```

## Required gates

Run these in order after a class or SynthDef change:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/compile_check.scd -Sclang "C:\Program Files\SuperCollider-3.14.1\sclang.exe"
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/run_all.scd -Sclang "C:\Program Files\SuperCollider-3.14.1\sclang.exe"
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_safety.scd -Sclang "C:\Program Files\SuperCollider-3.14.1\sclang.exe"
```

DSP-behavior changes also require:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_hold_integrity.scd -Sclang "C:\Program Files\SuperCollider-3.14.1\sclang.exe"
```

A marker missing from the end of output counts as failure even if the process exits
without a conventional error. GUI changes additionally require their focused
interaction tests and the relevant Qt smoke. Audible or visible behavior is not
accepted from automation alone; include a concise manual protocol and observed
result.

## Change design

- Keep one source of truth for parameter definitions in `CQSpec` and `SPEC.md`.
- Route all parameter and matrix writes through their existing funnels.
- Never weaken output safety, feedback bounding, microphone defaults, or their
  tests.
- Preserve failure toward silence and exhaustive cancellation of temporary GUI
  gestures.
- Add a regression test that demonstrates the reported wrong behavior, not only
  the desired implementation.
- Update code, normative documentation, tests, and changelog together when they
  form one behavior change.

## AI-assisted contributions

AI tools are welcome, but contributors remain responsible for every submitted
change. State significant AI assistance in the pull request, review generated
diffs, verify third-party provenance, and never use an agent's agreement as the
only evidence for correctness. Safety-critical paths require human line-by-line
review as described in [`docs/DEVELOPMENT_PROCESS.md`](docs/DEVELOPMENT_PROCESS.md).

## Pull requests

A useful pull request contains:

- the problem and intended behavior;
- affected invariants and known risks;
- automated commands and exact final markers;
- manual listening/visual/hardware evidence where applicable;
- screenshots or short recordings for visible or audible changes;
- no unrelated formatting or refactoring.

By submitting a contribution, you agree to license it under the repository's
GNU GPL v3.0-or-later license and certify that you have the right to do so.
