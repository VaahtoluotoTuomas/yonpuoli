// Read-only field notes. Keys are the instrument's existing semantic identities;
// ranges, defaults and stored values are never duplicated here.
CQInfoText {
	*note { |title, gesture, body, detail = ""|
		^(title: title, gesture: gesture, body: body, detail: detail)
	}

	*param { |name|
		var s = name.asString, n = s.copyRange(4, 4), note;
		if(s.beginsWith("coco")) {
			note = switch(s.drop(5).asSymbol,
				\Speed, { this.note("spiral / speed", "wind along the spiral groove",
					"Faster memory raises the pitch and shortens the loop. Slower memory stretches it into coarser, darker sound.",
					"Try sealing the heart, then slowing the spiral. Speed changes the clock that reads the memory; pitch, duration and converter texture travel together.") },
				\Hold, { this.note("heart / hold", "click to seal or open; hold right-click to punch",
					"A sealed loop preserves its memory. Open it to write incoming sound and feedback into the loop.",
					"Right-click temporarily inverts the hand setting until release. The record socket can also invert it: the red seal shows the effective state. Hold preserves memory even while you change speed.") },
				\Flip, { this.note("mouth chevron / flip", "click to reverse; Q / W for the two shells",
					"Reverse the direction in which this Coco reads its memory.",
					"Each rising trigger at the flip socket turns playback the other way. The direction stays when the signal falls; the next rise turns it back. The chevron also turns the current direction around. Fast triggers can become texture.") },
				\Spaf, { this.note("tide crescent / spaf", "drag vertically; right-click returns to zero",
					"Set the depth and direction of speed modulation arriving at the purple socket. At zero, that modulation has no effect.",
					"Patch a slow triangle into the nearby socket and start close to zero. Positive and negative depth bend pitch in opposite directions. The hollow satellite shows the effective modulation, separately from your setting.") },
				\In, { this.note("tide crescent / input", "drag vertically",
					"Control how much new sound enters the converter. More input can push it into gritty clipping.",
					"Open the heart to write new sound. Input is different from the outer shoreline: the shoreline changes the output level, while Input changes what is written. The adjacent socket modulates this input gain.") },
				\Fb, { this.note("tide crescent / feedback", "drag vertically",
					"Return old sound through the converter. Repeats fade, sustain or grow into saturated crunch as feedback rises.",
					"Each written repeat is clipped and quantized again. With the heart sealed, memory is preserved instead. Green marks fading echoes, amber sustain and bloom, red the strongest regeneration.") },
				\DolbyIn, { this.dolby("input", "feedback") },
				\DolbyFb, { this.dolby("feedback", "input") },
				\Level, { this.note("shoreline / level", "drag vertically on the outer shore",
					"Balance this Coco in the output mix. The other shell has its own independent level.",
					"This is output gain, not Input or Feedback. Lowering it lets you keep a strongly driven memory while placing it quietly in the mix.") },
				\Organ, { this.note("yellow calyx / organ", "drag vertically around the yellow seed",
					"Bring in a square-stack voice whose pitch follows this Coco's memory clock.",
					"The yellow jack remains a modulation source even when this audible organ layer is quiet. Wind the spiral to change its pitch. The nearby hollow socket modulates the audible layer.") },
				\SlotQ, { this.note("tributary gill / quantussy input", "drag vertically at the river's shell end",
					"Let the castles' audio mix enter this Coco. Each shell has its own entrance.",
					"Pull a castle inward to raise its send, open this gill, then open the heart to write. The castles' modulation jacks remain available even when their audio sends are closed.") },
				\SlotX, { this.note("bridge valve / cross-feed", "drag vertically",
					"Send Coco " ++ if(n == "1") { "2" } { "1" } ++ " into Coco " ++ n ++ ". The other bridge lane controls the opposite direction.",
					"Open the receiving heart to write the incoming loop. Together, the two valves can make the memories feed each other. Their gains are independent of the Quantussy tributaries.") },
				\Dust, { this.dust },
				\DustGrain, { this.dust },
				{ nil });
			if(note.notNil) { note = note.copy; note[\title] = note[\title] ++ " / coco " ++ n };
			^note
		};
		if(#[deckALevel, deckBLevel].includes(name)) {
			^this.note("wing crescent / deck level", "drag vertically",
				"Set the file player's level before it reaches the Coco and the dry monitor.",
				"The bird loads a file; this crescent controls its level. Raise Master dry to hear the file directly. Open the matching heart to capture it in memory.")
		};
		if(#[qFreq, qChaos].includes(name)) { ^this.eye };
		if(s.beginsWith("pre")) {
			^if(s.endsWith("Src")) { this.source } { this.preamp }
		};
		if(s.endsWith("Range")) {
			^this.note("castle ripple / range", "click to cycle slow, middle and audio ranges",
				"Choose whether this castle moves as a slow modulation, a quicker pulse or an audible oscillator.",
				"The ripple's shape shows its range. The central eye scales all five castle rates together; their individual ranges remain independent.")
		};
		if(s.endsWith("Send")) { ^this.castle };
		^switch(name,
			\masterVol, { this.note("vol / master level", "drag vertically",
				"Set the instrument's listening level. Start low while learning feedback.",
				"The output safety chain remains active. PANIC or Space mutes the owned outputs; a second press resumes.") },
			\masterDry, { this.note("dry / direct monitor", "drag vertically",
				"Hear the file decks and live sources directly, alongside the processed Coco memories.",
				"Raise dry to check a loaded file. Lower it after capturing a loop to hear the Coco alone. Dry is independent of the hearts' hold state.") },
			\patchDepth, { this.note("depth / matrix influence", "drag vertically",
				"Scale the influence of the patch cables together without removing them.",
				"Low depth can quiet modulation even when cables are visible. The stored cable weights and your hand settings remain separate.") },
			\monitorMute, { this.master(\panic) }, { nil })
	}

	*dolby { |path, partner|
		^this.note("oyster / " ++ path ++ " dolby", "click to cycle off, gate and duck",
			"Shape the " ++ path ++ " path: leave it open, suppress its quiet parts, or duck it under the " ++ partner ++ " envelope.",
			"Gate follows this path's own level. Duck follows the other path within this Coco. The paired arcs show the mode; this is a dynamics control, not noise-reduction encoding.")
	}
	*dust {
		^this.note("veil / dust", "up/down for amount; left/right for grain",
			"Write noise into memory: up adds more, down less; left favours steady hiss, right sparse chip crackles. Grain blends noise sources, not sample-grain size.",
			"Open the heart to hear changes; old sound needs a lap to be replaced. Lower Input and Feedback to compare the two textures. The hollow socket modulates amount only.")
	}
	*eye {
		^this.note("ring eye / frequency + chaos", "drag up/down for pitch; left/right for chaos",
			"Shape the five interacting castles with one hand. More chaos makes their rates wander and break into irregular motion.",
			"The eye does not turn up their audio. Pull a castle inward and open a shell's Quantussy gill to hear it through memory, or patch its sources into modulation sockets.")
	}
	*castle {
		^this.note("castle / spesal + audio send", "tap to step; pull inward to raise the audio send",
			"A tap changes this castle's sampling relationship. A radial drag opens or closes its contribution to the shared audio mix.",
			"A drag never performs the tap's rewire. The triangle and stepped castle jacks remain modulation sources regardless of this send. Open a shell's Quantussy gill to let the audio mix enter memory.")
	}
	*preamp {
		^this.note("root knot / drive + bias", "drag vertically for drive; horizontally for bias",
			"Drive adds saturation to the selected source. Bias changes the asymmetry and harmonic colour of that saturation.",
			"The first clear direction selects the axis for this gesture. The green root jack follows the processed signal's envelope. Source selection lives on the strands below the knot.")
	}
	*source {
		^this.note("root strand / source", "click a source strand",
			"Choose which live input or file deck feeds this preamp. Click the selected strand again to close the intake.",
			"Choosing mic does not raise microphone gain. That gain starts muted and is controlled in Drawer > workshop. The Coco's separate direct deck input is also configured in Workshop.")
	}
	*master { |key|
		^switch(key,
			\panic, { this.note("PANIC / silence", "click or press Space; repeat to resume",
				"Mute every owned output path immediately.",
				"PANIC also cancels held performance gestures. It is always reachable while reading info. It does not erase the patch or the captured memories.") },
			\rec, { this.note("REC / record output", "click or press R to start or stop",
				"Record the instrument's output to a new audio file. The elapsed display shows the take's duration.",
				"Recordings go to the configured recordings folder. Existing files are never overwritten. This recorder is separate from writing or holding the Coco memories.") },
			\out, { this.note("out / stereo output meters", "read-only",
				"The two meters show the post-safety left and right output levels.",
				"If both are quiet, check vol and PANIC, then preview your source with dry. A moving castle does not by itself mean it is routed to audio.") },
			\gr, { this.note("gr / gain reduction", "read-only",
				"Watch how strongly the final limiter is reducing output peaks.",
				"Sustained reduction is a reason to ease levels or feedback while listening. The safety chain cannot be bypassed.") },
			\notice, { this.note("notice / instrument status", "read the current message",
				"Audio-route notices, microphone status and sustained-driving messages appear here.",
				"Use Drawer > audio for device and route problems. Info never covers this line or the Master controls.") },
			{ this.param(key) })
	}

	*endpoint { |name|
		var s = name.asString, body, detail;
		if(CQMatrix.srcIndex(name).notNil) {
			body = case
				{ s.beginsWith("tri") } { "A castle's continuous triangle signal: slow motion or audio, depending on its rate." }
				{ s.beginsWith("cas") } { "A castle's sampled signal: stepped or tracking motion taken from another castle." }
				{ s.beginsWith("env") } { "The selected preamp's envelope: modulation that follows the strength of its processed input." }
				{ s.beginsWith("org") } { "A square-stack signal whose pitch follows the Coco clock. It remains available when the audible organ layer is down." }
				{ true } { "The Coco memory's output, available as a modulation source for other parts of the instrument." };
			^this.note(s ++ " / source jack", "drag to a socket; right-drag for negative polarity", body,
				"While carrying a cable, wheel or Z / X chooses its strength before planting. Double-click a jack to light its connections without changing sound. Esc releases that light or drops a held cable.")
		};
		body = case
			{ s.endsWith("_spaf") } { "Modulate memory speed. The nearby SP.AF crescent sets the depth and direction; zero depth leaves this route inaudible." }
			{ s.endsWith("_inVca") } { "Modulate the gain of new sound entering this Coco's converter." }
			{ s.endsWith("_fbVca") } { "Modulate this Coco's feedback gain, shaping how old sound recirculates." }
			{ s.endsWith("_flip") } { "Each rising trigger reverses the current direction once. Holding high or falling low leaves it there. The next rise reverses it again, from either hand-set direction." }
			{ s.endsWith("_skip") } { "A rising trigger restarts the memory read position." }
			{ s.endsWith("_record") } { "A high gate inverts the hand-set hold state: sealed becomes writing, writing becomes sealed." }
			{ s.endsWith("_organ") } { "Modulate the audible organ layer independently of its yellow source jack." }
			{ s.endsWith("_slotQ") } { "Modulate the castles' audio entrance into this Coco." }
			{ s.endsWith("_slotX") } { "Modulate the opposite Coco's feed into this Coco. Each direction has its own socket." }
			{ s.endsWith("_dust") } { "Modulate converter dust amount. Grain remains a separate hand setting on the veil." }
			{ s.endsWith("_fm") } { "Modulate this castle's oscillator frequency." }
			{ s.endsWith("_spesal") } { "A rising trigger steps this castle's sampling relationship." }
			{ s.endsWith("_rate") } { "Modulate file playback rate. Negative rates reverse the file." }
			{ true } { nil };
		if(body.isNil) { ^nil };
		detail = "The source's colour identifies its family. Cable colour identifies polarity: orange positive, blue negative. Wheel a planted cable to change its weight; Ctrl makes a fine step. Right-click removes it.";
		^this.note(s ++ " / destination socket", "plant a source cable here", body, detail)
	}

	*topic { |target, matrix|
		var kind, index, note, weight;
		if(target.isNil) { ^nil };
		kind = target[0]; index = target[1];
		^switch(kind,
			\param, { this.param(index) },
			\master, { this.master(index) },
			\endpoint, { this.endpoint(index) },
			\eye, { this.eye },
			\castle, { note = this.castle; note[\title] = note[\title] ++ " / " ++ (index + 1); note },
			\preamp, { this.preamp },
			\source, {
				note = this.source;
				if(target[2].notNil) { note[\title] = "root strand / " ++ CQGuiNightMap.preampSourceLabel(target[2]) };
				note
			},
			\dust, { this.dust },
			\bird, { this.note("bird / deck " ++ if(index == 0) { "a" } { "b" },
				"click to load; wheel to walk the folder",
				"Choose a sound file, or drop one on this bird. Pull it in any direction and release: it springs home and chooses a random file.",
				"Raise Master dry to preview it. Open the matching Coco heart to write it into memory, then seal the heart to keep a loop. Loading a file alone does not open a sealed heart.") },
			\skip, { this.note("mouth spark / skip", "click to restart; A / S for the two shells",
				"Return this memory's read position to the start without clearing its sound.",
				"The moving spark shows the current read position. This stationary mark is the control. Patch a pulse into the skip socket for repeated restarts.") },
			\moon, { this.note("sowing moon / shake + weather", "tap to sow; drag sideways for density",
				"Sow a playable patch. A fuller moon adds more detail around its musical core. P also sows; Ctrl+Z undoes the gesture.",
				"Drag downward and hold to grow cables; upward to wither them. Further travel increases the pace. Shake can prepare musical settings as well as cables; grow and wither change only cable weights.") },
			\tideGate, { this.note("tide gill / water focus", "click this gill or press Tab to open water focus",
				"With water focus on, drag between the four seeds to travel through their stored patches.",
				"Left-drag commits on release. Right-drag is elastic and returns to the prior patch. Shift offers momentary water focus. Outside focus, right-drag draws a cable-harvesting lasso; an ordinary left sweep is inert.") },
			\seed, { this.note("wind seed / patch memory", "click to recall; Shift-click to store",
				"Keep one matrix patch at this corner of the tide. With water focus on, clicking an empty seed also stores the current patch.",
				"Right-click clears a seed. Drag one filled seed to another to breed their cables. Seeds remember cable weights only; whole-instrument snapshots live in Drawer > memory.") },
			\body, { this.note(index.asString ++ " / connected routes", "double-click the outline for solo-light",
				"Light the cables belonging to this body so you can read its connections.",
				"Solo-light changes only the drawing. Double-click again or press Esc to clear it. Individual controls and cable hit paths keep their own gestures.") },
			\cable, {
				weight = matrix !? { matrix.getPatch(index, target[2]) };
				this.note(index.asString ++ " -> " ++ target[2].asString,
					"drag to move; wheel for strength; right-click removes",
					if(weight.isNil) { "A modulation route between a source and a destination." } {
						if(weight == 0) { "This route is no longer patched." } {
						if(weight < 0) { "Negative polarity inverts the source's contribution. " } { "Positive polarity adds the source's contribution. " }
						++ "The cable's width and seated depth show its strength."
						}
					},
					(this.endpoint(target[2])[\body] ? "") ++ " Alt-click reverses polarity. Ctrl+wheel gives a fine step; Ctrl+Z undoes a committed cable edit.")
			},
			\tour, { this.tour(index) }, \advanced, { CQInfoAdvanced.step(index) }, { nil })
	}

	*tour { |step|
		^CQInfoTour.step(step)
	}
}
