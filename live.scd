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

// ---- phase 2: deck A + the coco pair / KUUNTELU 2 ----
/*
CQ.boot;                             // power-on: RAM full of dust, loop-locked

CQ.deckA.load("field.wav");          // relative paths resolve inside samples/
CQ.deckA.rate_(1.0);                 // -2..2, negative = reverse
CQ.deckA.loop_(1);

CQ.hold(1, 0);                       // coco1 records (delay mode)
CQ.hold(1);                          // no value = toggle -> loop-locked again
CQ.set(\coco1Speed, 12000);          // re-pitch the held loop: dark + gritty
CQ.set(\coco1Speed, 48000);          // bright, short
CQ.set(\coco1Fb, 1.0);               // unity looper; 1.15 = escalating crunch
CQ.flip(1);                          // reverse playback direction
CQ.skip(1);                          // reset loop to its start

CQ.set(\coco1DolbyFb, \duck);        // "other-puncher": input punches the layers
CQ.set(\coco1Dust, 0.3);             // digital dust source, try with no input
CQ.set(\coco1Organ, 0.5);            // square-stack voice, pitch tracks Speed

CQ.set(\coco1SlotX, 0.6);            // cross-feed coco2 -> coco1
CQ.set(\coco2SlotX, 0.4);            // ...and back (one block late by design)
*/
