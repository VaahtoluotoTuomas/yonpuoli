// Concise, illustrated component notes. References are existing semantic
// targets, shared by the miniature and the hover locator on the instrument.
CQInfoPages {
	*row { |label, text, refs = #[], axis| ^(label: label, text: text, refs: refs, axis: axis) }
	*pageCount { |group| ^if(group[1] == \castles) { 2 } { 1 } }
	*page { |group, page = 0|
		var kind = group[1], i = group[2], prefix = "coco" ++ (i + 1);
		var param = { |suffix| [\param, (prefix ++ suffix).asSymbol] };
		var endpoint = { |suffix| [\endpoint, (prefix ++ "_" ++ suffix).asSymbol] };
		var title, intro, rows;
		switch(kind,
			\coco, {
				title = prefix ++ " / the whole memory";
				intro = "Speed sets pitch, length and texture. Drag crescents/gills up/down. Solid dots send; hollow rings receive.";
				rows = [
					this.row("heart / spiral", "Click heart: write / hold. Wind spiral: speed & pitch. Right-hold or a high record input inverts Hold.", [param.value("Hold"), param.value("Speed"), endpoint.value("record")]),
					this.row("SP.AF", "Left crescent: depth / direction for purple speed input. Zero = no modulation.", [param.value("Spaf"), endpoint.value("spaf")], \vertical),
					this.row("input", "Middle crescent: incoming sound. Socket modulates gain. Click paired arc: off / gate / duck.", [param.value("In"), endpoint.value("inVca"), param.value("DolbyIn")], \vertical),
					this.row("feedback", "Right crescent: repeats. Socket modulates gain. Click paired arc: off / gate / duck.", [param.value("Fb"), endpoint.value("fbVca"), param.value("DolbyFb")], \vertical),
					this.row("flip / skip", "Chevron or rising flip trigger: turn the current direction around. Cross or rising skip trigger: restart the loop.", [param.value("Flip"), [\skip, i], endpoint.value("flip"), endpoint.value("skip")]),
					this.row("shore / outputs", "Outer arc: drag up/down for level. Grey/yellow dots send memory/Organ modulation.", [param.value("Level"), [\endpoint, prefix.asSymbol], [\endpoint, ("org" ++ (i + 1)).asSymbol]]),
					this.row("Organ", "Yellow calyx: drag up/down for tone level. Socket modulates it; pitch follows speed.", [param.value("Organ"), endpoint.value("organ")]),
					this.row("dust", "Veil: up/down for amount; left/right blends hiss into sparse crackles. Socket modulates amount.", [[\dust, i], endpoint.value("dust")]),
					this.row("Q inlet", "Gill admits castle audio. Hollow socket modulates this feed.", [param.value("SlotQ"), endpoint.value("slotQ")]),
					this.row("bridge", "Upper gill admits Coco " ++ (2 - i) ++ ". Hollow ring modulates this feed. Each direction is independent.", [param.value("SlotX"), endpoint.value("slotX")], \vertical)
				];
			},
			\castles, {
				title = "castles / " ++ if(page == 0) { "the shared ring" } { "one castle" };
				if(page == 0) {
					intro = "Five oscillators sample each other. Example links.";
					rows = [
						this.row("into memory", "Pull a castle inward for sound. Open a Coco's Q inlet and heart to hear and record it.", 5.collect { |n| [\castle, n] }),
						this.row("outer ring", "Hover shows sampling links. Double-click lights the ring's cables; repeat clears.", [[\body, \quantussy]]),
						this.row("central eye", "Drag up/down for all five rates; sideways for chaos. The eye has its own info card.", [[\eye]])
					];
				} {
					intro = "Top castle; hover to locate all five. Outputs work with audio send closed.";
					rows = [
						this.row("clock", "Tap to change sampling partner and hold mode.", 5.collect { |n| [\castle, n] }),
						this.row("castle out", "Upper-left crown: sampled steps / tracking.", 5.collect { |n| [\endpoint, ("cas" ++ (n + 1)).asSymbol] }),
						this.row("triangle out", "Upper right: continuous triangle.", 5.collect { |n| [\endpoint, ("tri" ++ (n + 1)).asSymbol] }),
						this.row("step in", "Lower left: a rising trigger changes pairing.", 5.collect { |n| [\endpoint, ("castle" ++ (n + 1) ++ "_spesal").asSymbol] }),
						this.row("FM in", "Lower right: frequency modulation.", 5.collect { |n| [\endpoint, ("castle" ++ (n + 1) ++ "_fm").asSymbol] }),
						this.row("range", "Bottom ripple: click for slow / middle / audio rates.", 5.collect { |n| [\param, ("c" ++ (n + 1) ++ "Range").asSymbol] })
					];
				};
			},
			\bird, {
				title = "bird / deck " ++ if(i == 0) { "A" } { "B" };
				intro = "A looping file player. Preview with Master dry; open the matching Coco heart to capture it.";
				rows = [
					this.row("wings", "Click to load or drop a sound file. Wheel walks the folder. Pull the bird in any direction and release for a random file.", [[\bird, i]]),
					this.row("level crescent", "Drag vertically to set the file's level before Coco and dry monitoring.", [[\param, if(i == 0) { \deckALevel } { \deckBLevel }]]),
					this.row("rate input", "The hollow socket changes playback rate; negative rates play backwards.", [[\endpoint, if(i == 0) { \deckA_rate } { \deckB_rate }]])
				];
			},
			\root, {
				title = "root / " ++ if(i == 0) { "left" } { "right" };
				intro = "Select and colour an incoming source before it reaches the memory.";
				rows = [
					this.row("source strands", "Click a strand to choose a live input or file deck. Click it again to close the intake.", CQSpec.enumValues(if(i == 0) { \preASrc } { \preBSrc }).reject { |s| s == \none }.collect { |source| [\source, i, source] }),
					this.row("knot", "Drag vertically for drive, horizontally for bias. The first clear direction chooses the axis.", [[\preamp, i]]),
					this.row("green output", "Sends the processed source's loudness envelope as modulation.", [[\endpoint, if(i == 0) { \envA } { \envB }]])
				];
				intro = intro ++ " Mic gain starts muted; set it in Drawer > workshop.";
			},
			\eye, {
				title = "eye / shared motion"; intro = "One hand shapes all five castles together.";
				rows = [
					this.row("up / down", "Drag vertically to scale their oscillator rates.", [[\eye]], \vertical),
					this.row("left / right", "Drag sideways for chaos: more gives irregular, wandering motion.", [[\eye]], \horizontal),
					this.row("hearing the castles", "The eye does not set volume. Use castle sends, a Coco's Q inlet and an open heart.")
				];
			},
			\moon, {
				title = "moon / sow & weather"; intro = "Grow a patch from a musical starting point.";
				rows = [
					this.row("tap", "Sow a playable patch. P also sows; Ctrl+Z undoes it.", [[\moon]]),
					this.row("sideways", "Drag for density: a fuller moon adds more detail around the core.", [[\moon]], \horizontal),
					this.row("down / up", "Drag and hold to grow / wither cables. Greater distance increases the pace.", [[\moon]], \vertical)
				];
			},
			\tide, {
				title = "tide / patch memories"; intro = "The four seeds around the castle ring remember cables, not audio. Whole-instrument snapshots live in Drawer > memory.";
				rows = [
					this.row("store / recall", "Shift-click a seed to store; click to recall; right-click to clear. Open husk = empty, closed = stored.", 4.collect { |n| [\seed, n] }),
					this.row("open water focus", "Click the gill below the castles or press Tab. Hold Shift for temporary focus. In focus, clicking an empty seed also stores.", [[\tideGate]]),
					this.row("blend", "With focus on, drag between stored seeds. Release to keep the blend; right-drag returns on release.", 4.collect { |n| [\seed, n] }),
					this.row("breed", "Drag one stored seed onto another stored seed to make a new live patch. Both memories stay intact.", 4.collect { |n| [\seed, n] })
				];
			},
			\master, {
				title = "master / listen & record"; intro = "These controls shape the final listening mix. PANIC and Space remain live during info.";
				rows = [
					this.row("vol / dry", "Set listening level / preview file and live sources alongside the Coco memories.", [[\master, \masterVol], [\master, \masterDry]]),
					this.row("depth", "Scale all cable influence without removing the patch.", [[\master, \patchDepth]]),
					this.row("out / gr", "Stereo output level / final limiter gain reduction.", [[\master, \out], [\master, \gr]]),
					this.row("REC / PANIC", "Record the output with REC or R. PANIC or Space mutes; repeat to resume.", [[\master, \rec], [\master, \panic]])
				];
			}
		);
		^(title: title, intro: intro, rows: rows)
	}
	*shortcuts { |page|
		^if(page == 0) {
			(title: "shortcuts / 1 of 2 / play & capture",
				intro: "Letter keys need no Shift. Performance keys stay live while reading info.",
				rows: [
					this.row("1 / 2", "Coco 1 / 2 heart: toggle write (hollow) / hold (red)."),
					this.row("Q / W", "Reverse Coco 1 / 2."),
					this.row("A / S", "Restart Coco 1 / 2 at the loop's beginning."),
					this.row("3 to 7", "Step castle 1 to 5: change its sampling partner and hold mode."),
					this.row("P", "Sow a playable patch with the moon."),
					this.row("R", "Start / stop recording the stereo output to a file."),
					this.row("Space", "PANIC: mute. Press again to resume.")
				])
		} {
			(title: "shortcuts / 2 of 2 / patch & navigate",
				intro: "These keys depend on what you are doing. Basics & first loop teaches the cable gestures.",
				rows: [
					this.row("Left / right", "Previous / next page of an open info card. Unavailable directions stay grey."),
					this.row("Esc", "Cancel the held gesture, drop a cable in hand or clear solo-light. Also exits info."),
					this.row("Ctrl+Z / Ctrl+X", "Undo / redo the last cable edit or sow. Text fields keep their normal undo / cut keys."),
					this.row("Tab", "Toggle water focus on Night. Inactive in Drawer or while entering text."),
					this.row("Shift", "On Night, hold for temporary water focus; release to return. Not while carrying a cable."),
					this.row("Z / X", "While holding a cable: lower / raise its strength. Add Shift for finer steps.")
				])
		}
	}
}
