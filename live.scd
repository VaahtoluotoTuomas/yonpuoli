// live.scd — performance scratchpad. Evaluate blocks manually; nothing here is
// loaded automatically and no scratch code belongs in classes/ (CLAUDE.md).

// ---- phase 1 smoke test / KUUNTELU 1 ----
/*
CQ.boot;                     // silence, then a 2 s fade-in

CQ.set(\masterDry, 0.5);     // monitor the live inputs through the safety chain
CQ.set(\masterVol, -12);     // default; try small moves, must be click-free

CQ.panic;                    // instant mute (30 ms fade), state preserved
CQ.resume;                   // and back

CQ.get(\masterVol);          // read any parameter
CQ.free;                     // clean teardown
*/
