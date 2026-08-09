# Development process and technical ownership

Yönpuoli is a human-directed, AI-assisted project. This document states what that
means in practice, what evidence is and is not trustworthy, and what must be true
before generated changes are accepted.

## Responsibilities

The maintainer owns:

- the instrument's purpose, sonic direction, interaction language, and visual
  composition;
- product requirements, constraints, priorities, and final decisions;
- hands-on listening, visual, mouse, and hardware-controller evaluation;
- accepting or rejecting generated changes;
- release claims, user safety, maintenance, and public communication.

Claude Code and OpenAI Codex are used to inspect the repository, propose plans,
write and review code, create tests and documentation, run tools, and investigate
failures. Agent output is not treated as authority merely because it compiles or
because another agent agrees with it.

## Change workflow

Every meaningful change follows this loop:

1. State the user-visible or engineering outcome and the invariants it may affect.
2. Inspect the current implementation and relevant normative documents.
3. Make the smallest coherent change, with its tests and documentation together.
4. Run the required automated gates in the repository-defined order.
5. For visible or audible work, stop at the first runnable version for hands-on
   review before polishing or starting the next feature.
6. Record what was actually verified. Do not turn an automated result into a
   listening, visual, hardware, or performance claim.
7. Commit one accepted behavior change at a time.

The implementation agent reads the stable repository rules in
[`../CLAUDE.md`](../CLAUDE.md) and the concise current status in
[`../PLAN.md`](../PLAN.md). Completed handoffs and acceptance journals live under
[`history/`](history/README.md). `SPEC.md`, `GUI_SPEC.md`, and `ARCHITECTURE.md`
remain normative in their documented areas.

## Evidence hierarchy

Different questions need different evidence:

| Claim | Required evidence |
|---|---|
| Classes and SynthDefs compile | `tests/compile_check.scd` marker and exit code |
| State, routing, transaction, or geometry behavior | focused UnitTest that fails for the known wrong behavior |
| Output safety and hold integrity | NRT render of the real SynthDefs plus numerical analysis |
| GUI interaction | serverless interaction test or real Qt smoke, depending on the claim |
| Paint cost | measurement inside the real Qt paint event |
| Musical usefulness | a short, repeatable listening protocol and maintainer decision |
| Visual hierarchy or affordance | real-window review at the documented scales |
| Optional hardware behavior | a test on that hardware; simulation is labelled simulation |

A passing suite is necessary but not sufficient. Safety-critical and lifecycle
tests should periodically be mutation-checked by introducing a known fault and
confirming that the expected gate fails.

## Review policy for generated diffs

All future generated diffs are reviewed for behavior, affected invariants, failure
paths, and test relevance. The following areas additionally require line-by-line
review by the maintainer before acceptance:

- the master output chain and in-loop feedback bounding;
- parameter clamps and output configuration validation;
- microphone defaults and snapshot recall safety;
- boot/free/reboot, recorder, and async resource ownership;
- persistent file formats and destructive file operations;
- a new public control path or gesture that can leave a latched state.

For ordinary generated code, the maintainer must still be able to locate its owner,
trace its input and output, identify the relevant test, and revert it safely.

## Technical-ownership pass

The existing codebase is being learned vertically rather than by rereading every
historical diff.

### Slice 1: state and control

Trace one continuous parameter and one enum from `CQSpec` through `CQState`, the
server, notifications, GUI, MIDI, and snapshot recall. Explain why there is one
write funnel and demonstrate a malformed input being clamped or rejected.

### Slice 2: feedback and output safety

Trace audio through one Coco and the master. Explain `BufRd`/`BufWr` ordering,
clock-rate repitch, hold, feedback, clip/wrap-before-quantize, DC removal, headroom,
limiting, and bad-value squelching. Relate each invariant to an automated and a
manual test.

### Slice 3: Night interaction

Trace one gesture from hit geometry and priority through the state funnel and back
to drawing. Explain cancellation paths, Qt defer rules, telemetry truth, and the
test that prevents the gesture from leaking state.

The momentary hold punch is the first ownership exercise. Before an agent writes
it, the maintainer predicts the affected paths. At least one focused test is
written or substantially authored by the maintainer, the complete diff is read,
and one mutation demonstrates that the exit-path test turns red.

## Interview readiness

Before presenting line-level ownership, the maintainer should be able to deliver
without an AI assistant:

- a 90-second description of the instrument and personal role;
- a 10-minute whiteboard walkthrough of the architecture;
- a 20-minute code walkthrough covering the three slices above;
- one audit story from symptom to reproducer, root cause, correction, and
  regression protection;
- one honest limitation and a concrete next experiment.

It is acceptable not to remember every method. The standard is the ability to
navigate, reason, verify, and take responsibility—not memorization or manual
keystroke count.

## Authorship statement

Public descriptions should use language such as “designed and led by Tuomas
Vaahtoluoto with extensive AI-assisted implementation.” They should not claim
that the repository was hand-coded alone, that an agent's output was independently
verified when it was not, or that an automated check proves a listening or hardware
result.
