# Agent notes

Read `SPEC.md` for behavior and safety, `ARCHITECTURE.md` for code ownership, and
`GUI_SPEC.md` before changing interaction contracts. If the ignored local file
`.agents/PROJECT_CONTEXT.md` exists, read it for the current handoff.

## SuperCollider gate

For every headless SuperCollider run, invoke `tests/run_sclang_gate.ps1`. Never
launch `sclang.exe` directly from an agent shell. A class-library parse error can
otherwise leave sclang at an interactive prompt with buffered output and strand
the tool call.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/compile_check.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/run_all.scd
```

The wrapper owns the child PID, uses daemon mode, enforces a timeout, validates
the expected marker, and writes diagnostics under `tests/tmp/gate-logs/`. A
timeout, missing marker, or nonzero exit is a failed gate.

## Durable rules

- Never hardcode user or installation paths. Machine data belongs in ignored
  config or test-path files.
- Keep parameter names, defaults, ranges, and curves canonical in `CQSpec`.
  Every public value path must clamp or reject malformed input.
- GUI, MIDI, snapshots, code, and generative gestures are clients of the same
  `CQState` / `CQMatrix` write funnels. Do not add a parallel control path.
- `CQGraph` owns server resources. Release listeners, routines, callbacks, and
  server resources on teardown.
- Keep the server graph static while playing apart from explicit transactions
  such as deck-buffer swaps and recording.
- Preserve failure toward silence, a zero microphone-gain default, reachable
  PANIC, bounded feedback before re-quantization, and the non-bypassable output
  safety chain.
- `BufRd` must precede `BufWr` in the Coco loop. Existing block delays in
  feedback paths are intentional.
- Windows device enumeration happens only after the owned audio server stops and
  through `CQAudioDeviceProbe`; do not restore live `_ListAudioDevices` calls.
- Keep PowerShell scripts ASCII-compatible with Windows PowerShell 5.1 and all
  repository prose and SuperCollider files UTF-8.
- Automation cannot make a listening, visual, hardware, or musical acceptance
  decision. Give the maintainer a reproducible manual check for such changes.
- Update the relevant specification, test, and changelog when public behavior
  changes.
