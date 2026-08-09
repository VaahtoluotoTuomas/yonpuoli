# Agent gate rules

For every headless SuperCollider run, invoke `tests/run_sclang_gate.ps1`.
Never launch `sclang.exe` directly from an agent shell. A class-library parse
error can otherwise leave sclang at an interactive prompt with buffered output,
which strands the tool call without returning the diagnostic.

Examples:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/compile_check.scd
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/run_all.scd
```

The wrapper owns the child PID, runs sclang in daemon mode, enforces a timeout,
validates the script's marker and writes a diagnostic log under
`tests/tmp/gate-logs/`. Treat a timeout, a missing marker, or a nonzero exit as
a failed gate and inspect the printed log path.
