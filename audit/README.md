# Audit reading order

Yönpuoli's technical audit was performed in two passes: an initial source and
documentation review, followed by independent evaluation and focused
reproducers. Later evidence refined several early severity classifications.

## Active decision layer

1. [`FINAL_AUDIT.md`](FINAL_AUDIT.md) records the independent evaluation of the
   July 2026 baseline and the corrected severity decisions.
2. [`VERIFIED_FINDINGS.md`](VERIFIED_FINDINGS.md) records the verified findings,
   reproducers and limits for that baseline.
3. [`PRIVACY_AUDIT.md`](PRIVACY_AUDIT.md) records the separate public-release
   privacy and full-history review, including the explicit decision not to
   rewrite history.

These documents are retained evidence, not a claim that the old audited commit is
the current release candidate. Current status and remaining release gates are in
the root [`PLAN.md`](../PLAN.md). [`GUI_SPEC.md`](../GUI_SPEC.md) remains the
normative source for GUI behavior and visual language.

## Historical material

Original prompts, first-pass reports, test evidence and superseded remediation
plans are preserved under [`history/`](history/README.md). They remain public to
make the audit reproducible and to show how the project responded to criticism.
Historical command output and paths describe the repository at audit time and are
not rewritten to resemble today's tree.
