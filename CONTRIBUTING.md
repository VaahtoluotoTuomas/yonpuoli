# Contributing to Yönpuoli

Yönpuoli is a personal experimental instrument, but focused bug fixes,
documentation improvements, platform reports, tests, and reproducible musical
or interaction findings are welcome.

Please open an issue before adding a parameter, dependency, public gesture,
persistent format, or change to the output-safety chain. Those choices affect
the identity of the instrument, not only its implementation.

## Before changing code

- Read `SPEC.md` for behavior and safety, `GUI_SPEC.md` for interaction, and
  `ARCHITECTURE.md` for ownership.
- Keep parameter definitions canonical in `CQSpec` and route parameter and
  matrix writes through their existing funnels.
- Do not copy Ciat-Lonbarde artwork, documentation, audio, or other third-party
  assets into the repository. Record the source and license of permitted
  material in [`SOURCES.md`](SOURCES.md).
- Review and take responsibility for AI-assisted changes. Mention substantial
  AI assistance in a pull request; an agent's agreement is not evidence that a
  musical, visual, or safety result is correct.

## Development checks

The reference environment is Windows with SuperCollider 3.13 or newer. Copy
`config.example.scd` to the ignored `config.scd`, then add `classes/` and
`tests/classes/` to the SuperCollider language include paths.

Every headless run must go through the guarded PowerShell wrapper:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/compile_check.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/run_all.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/nrt_safety.scd
```

DSP, routing, lifecycle, and safety changes may also require the focused NRT
gates. GUI changes require their interaction smoke. A timeout, missing success
marker, or nonzero exit is a failure.

For audible or visible behavior, include a short manual check and the observed
result. Automation cannot listen, inspect real hardware, or decide whether a
gesture belongs to the instrument.

Keep pull requests focused and explain the problem, intended behavior, relevant
risks, and checks performed. By submitting a contribution, you agree to license
it under GNU GPL v3.0 or later and confirm that you have the right to do so.
