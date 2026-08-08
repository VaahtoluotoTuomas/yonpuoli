# CLAUDE.md — shared Yönpuoli agent rules

This file contains stable repository rules, not a live handoff. Current release
status belongs in `PLAN.md`; completed implementation history belongs under
`docs/history/`. Read `SPEC.md`, `ARCHITECTURE.md`, and, for GUI work,
`GUI_SPEC.md` before changing their respective contracts.

## Environment and gates

- Windows is the tested development host. Core code targets SuperCollider 3.13+
  and CI runs the documented current version.
- Never hardcode a user or installation path. Machine data belongs in ignored
  `config.scd`, `config.audio.scd`, or `tests/dev_paths.scd`.
- Follow `AGENTS.md`: every headless SuperCollider invocation goes through
  `tests/run_sclang_gate.ps1`; never launch `sclang.exe` directly from an agent
  shell.
- After a change to `classes/` or SynthDefs, run compile and unit gates. Run NRT
  safety and hold gates for DSP, routing, lifecycle or safety changes, plus the
  focused GUI/test gate for the changed surface.
- A missing success marker, timeout, or nonzero gate exit is a failure.
- After editing `classes/*.sc`, remind a live IDE user to recompile the class
  library before hands-on testing.

## SuperCollider and ownership rules

- Declare function-local `var`s before executable statements.
- Keep parameter names, defaults, ranges and curves canonical in `CQSpec`; every
  write path clamps through it.
- `CQGraph` owns server groups, buses and buffers. Pass server ownership
  explicitly and synchronize boot stages.
- GUI, MIDI, snapshots and code are clients of the same `CQState`/`CQMatrix`
  write funnel. Do not add a parallel control path.
- Namespace `MIDIdef`/`OSCdef` keys with `cq_` and release every listener,
  Routine, view callback and server resource on teardown.
- Keep scratch work out of `classes/`; use `live.scd`, `tests/`, or a clearly
  labelled experiment.
- Prefer cohesive owner classes over arbitrary line-count targets. If an owner
  becomes difficult to explain or test, extract a responsibility behind focused
  regression coverage rather than performing a release-only cosmetic split.

## Safety invariants

Never weaken these or their tests:

1. Every hardware output passes through `LeakDC`, bounded tanh headroom,
   `Limiter(0.89, 0.01)`, and bad-value squelch.
2. Coco feedback is clipped/wrapped before re-quantization inside the loop.
3. Boot stays silent until verification and fades in; `CQ.panic` remains
   reachable.
4. Every external value path clamps or rejects malformed input.
5. `micGain` defaults to zero and snapshots do not silently raise it.
6. NRT safety limits are evidence, not tuning suggestions.
7. Recordings use new timestamped files and never overwrite a take.

## Evidence and human review

- Agents cannot hear sound or substitute for visual, hardware or musical
  acceptance. Never turn an automated result into one of those claims.
- For audible or visible changes, provide a short reproducible manual script and
  wait for the maintainer's decision before calling the work accepted.
- Review generated diffs, failure paths, provenance and test relevance. An
  agent's agreement is not independent verification.
- Update normative documentation when public behavior changes, and keep the
  concise roadmap free of implementation-session transcripts.

## Durable platform pitfalls

- Windows device enumeration is safe only after the owned audio server stops.
  Production discovery uses `CQAudioDeviceProbe` and a bounded fresh isolated
  interpreter; never restore live `_ListAudioDevices` on owned WASAPI endpoints.
- `.ps1` files remain pure ASCII for Windows PowerShell 5.1 compatibility.
- Repository prose and SuperCollider files are UTF-8; do not pass them through a
  text rewrite that changes encoding.
- `Collection:includes` uses identity on Strings; use equality-aware lookup for
  device-name data.
- Qt stage/view swaps are queued. Directly hiding the view under the pointer can
  deadlock QtCollider.
- `BufRd` precedes `BufWr` in the Coco loop; the pre-write read is load-bearing.
- `LocalIn`/`InFeedback` block delays in the Quantussy, organ return and Coco
  cross-feed paths are intentional.
