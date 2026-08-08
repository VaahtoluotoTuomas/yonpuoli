# Yönpuoli release-media brief

Release media is evidence of the actual instrument, not decoration added to make
the project look more complete. The repository may generate a deterministic GUI
capture, but sound, responsiveness and musical value require a human performance
and listening decision.

## Repository image

`docs/media/yonpuoli-night.png` is generated from the exact release candidate by
`tests/gui_night_capture.scd`, invoked only through `tests/run_sclang_gate.ps1`.
It should show the complete roomy Night surface, active internal streams and a
small readable cable field. Regenerate and inspect it whenever the public Night
composition changes.

Keep only the selected compressed image in Git. Intermediate captures remain in
ignored `tests/tmp/`; do not commit test contact sheets or local window captures.

## Performance video

Record one honest 45–90 second take from a clean boot at the tagged-candidate
commit. Show, without cuts that imply false responsiveness:

1. a self-starting release preset or snapshot on Night;
2. direct Coco, Organ, SlotX or Dust playing;
3. one visible cable edit and one precise Drawer edit;
4. PANIC muting the instrument immediately.

Capture the application at native scale, keep the pointer visible and avoid
post-production that replaces the instrument's sound. A title card may name the
commit, audio route and scene. Host the finished video with the GitHub Release or
on a durable video service instead of storing a large binary in the repository.

## Audio excerpts

Four excerpts of roughly 30–60 seconds are enough for 1.0 if they are musically
distinct. A useful set is:

- memory/feedback playing (`undertow` or `classic` with an original source);
- a self-generating Quantussy/organ field (`organ_lanterns` or `quantumDrone`);
- granular/noise material (`dust_bloom` or `dustGarden`);
- a denser cross-modulated scene (`glass_tide` or `crossCurrent`).

Use only original or clearly redistributable source material. Name the preset or
snapshot, state whether an external source was used, retain an unmastered archival
take and avoid loudness processing that hides instability or dynamics. Listen to
the final exports at a modest level on both headphones and speakers; automation
cannot approve this gate.

Lossless masters and delivery files belong in release storage, not Git history.
Do not add placeholder links: add URLs to README and release notes only after the
maintainer has heard and accepted the exact exports.

## Acceptance record

Before merge and tag, record in `tests/manual_checklist.md` that:

- image, video and audio were captured from the candidate commit;
- the image matches the current UI;
- the video represents gesture response honestly;
- every audio source may be redistributed;
- the final audio was accepted on headphones and speakers;
- media captions, README, changelog and release notes name the same version.
