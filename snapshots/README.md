# Release snapshots

Yönpuoli ships five deliberately versioned snapshots:

- `example` is the small, readable format/tutorial snapshot;
- `undertow` is a held, asymmetrical SlotX exchange;
- `dust_bloom` contrasts a slow mist memory with a fast chip memory;
- `glass_tide` catches an animated Quantussy field into two clock countries;
- `organ_lanterns` is the clear, directly playable two-organ state.

Five is intentional: four complete artistic scenes give two useful morph pairs
without filling the player's Memory page, while `example` remains a readable
hand-editing example. The artistic scenes are complete states and speak from
empty RAM; snapshots do not and cannot contain recorded Coco buffer audio.

All other `*.cqsnap.scd` files in this folder remain gitignored personal session
memory. Rebuild only the four exact artistic files with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File tests/run_sclang_gate.ps1 tests/generate_release_snapshots.scd
```
