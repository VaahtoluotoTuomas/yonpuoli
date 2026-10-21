// Short, always-visible field notes. Semantic targets come from the existing
// hover/hit handlers; these notes do not introduce controls or parameter values.
CQHoverText {
	*note { |title, text| ^(title: title, text: text) }
	*topic { |target|
		var kind, key;
		if(target.isNil) { ^nil };
		kind = target[0]; key = target[1];
		^switch(kind,
			\param, { this.param(key) },
			\master, { this.master(key) },
			\endpoint, { this.endpoint(key) },
			\bird, { this.note("bird / deck " ++ if(key == 0) { "A" } { "B" },
				"Click or drop a file to load. Wheel walks the folder; pull and release for a random sample. Raise Master dry to preview.") },
			\skip, { this.note("Coco " ++ (key + 1) ++ " / restart",
				"Click the cross to restart the loop without erasing it. A / S restart Coco 1 / 2.") },
			\castle, { this.note("castle " ++ (key + 1) ++ " / sampling & sound",
				"Tap to change sampling partner and hold mode. Pull inward for audio send; open a Coco Q inlet and heart to hear it.") },
			\preamp, { this.note("root " ++ (key + 1) ++ " / drive & bias",
				"Drag up/down for saturation, sideways for asymmetry. The first clear direction chooses the axis.") },
			\source, { this.note("root " ++ (key + 1) ++ " / " ++ CQGuiNightMap.preampSourceLabel(target[2]),
				"Click this strand to select the incoming source. Click again to close it. Mic gain is set separately in Drawer > workshop.") },
			\dust, { this.note("Coco " ++ (key + 1) ++ " / dust",
				"Up/down adds or removes noise. Left/right blends steady hiss into sparse crackles. Open the heart to write it into memory.") },
			\eye, { this.note("eye / shared motion",
				"Drag up/down for all five castle rates; sideways for chaos. Castle sends and Coco Q inlets control their audible level.") },
			\moon, { this.note("moon / sow & weather",
				"Tap to sow a random patch. Drag sideways for density; hold down/up to grow/wither cables. Ctrl+Z undoes; Ctrl+X redoes.") },
			\seed, { this.note("seed " ++ (key + 1) ++ " / patch memory",
				"Shift-click stores cables; click recalls; right-click clears. Drag one stored seed onto another to breed a new live patch.") },
			\tideGate, { this.note("tide / water focus",
				"Click or press Tab, then drag between seeds to blend. Release keeps the blend; right-drag returns. Shift gives temporary focus.") },
			\body, { this.note(if(key == \quantussy) { "castles / connected ring" } { key.asString ++ " / outline" },
				"Double-click the outline to light its connected cables. Repeat or press Esc to clear the light; sound stays unchanged.") },
			\cable, { this.note("cable / " ++ key ++ " > " ++ target[2],
				"Wheel sets strength; Ctrl gives fine steps. Alt-click reverses polarity. Drag the seated end to move; right-click removes.") },
			\ui, { this.ui(key) },
			{ nil })
	}

	*param { |name|
		var s = name.asString, suffix, entry, prefix;
		if(s.beginsWith("coco")) {
			prefix = "Coco " ++ s[4] ++ " / "; suffix = s.drop(5).asSymbol;
			entry = (
				Hold: ["heart / write & hold", "Click to write (hollow) or preserve the loop (red). Hold right-click to invert temporarily. Keys 1 / 2 toggle the two hearts."],
				Speed: ["spiral / speed", "Drag along the groove. Faster raises pitch and shortens the loop; slower stretches it and changes the converter texture."],
				Spaf: ["SP.AF / speed modulation", "Drag up/down to set depth and direction for the purple speed input. Zero blocks its influence; right-click resets to zero."],
				In: ["input", "Drag up/down to set incoming sound. Open the heart to write it. The nearby hollow socket modulates this gain."],
				Fb: ["feedback", "Drag up/down to return old sound through the converter. More feedback sustains or saturates repeats while the heart is open."],
				DolbyIn: ["input / gate & duck", "Click the paired arc: off, gate or duck. Gate suppresses quiet input; duck lowers it under this Coco's feedback envelope."],
				DolbyFb: ["feedback / gate & duck", "Click the paired arc: off, gate or duck. Gate suppresses quiet repeats; duck lowers them under this Coco's input envelope."],
				Flip: ["reverse", "Click the chevron to reverse the loop. Q / W reverse Coco 1 / 2; memory is preserved."],
				Level: ["shoreline / level", "Drag up/down on the outer arc to balance this Coco in the output mix. This changes listening level, not the recorded input."],
				Organ: ["Organ", "Drag up/down at the yellow calyx for the added tone's level. Its pitch follows the spiral; the yellow output also sends modulation."],
				SlotQ: ["Q inlet", "Drag the gill up/down to admit castle audio. Raise a castle send and open this heart to write it. The hollow ring modulates the feed."],
				SlotX: ["bridge", "Drag the upper gill up/down to receive the other Coco. Open this heart to write it. The hollow ring modulates this direction."],
				Dust: ["dust", "Drag up/down for noise amount; left/right blends hiss into crackles. Open the heart to write it; the hollow ring modulates amount."],
				DustGrain: ["dust grain", "Drag left for steady hiss, right for sparse crackles. This blends noise sources, not sample-grain size. Up/down controls amount."],
				Bits: ["bits", "Set converter resolution. Lower values make coarser steps and stronger quantization; higher values preserve more detail."],
				Wrap: ["wrap", "Switch between clipping and wrapping overdriven samples at the converter's edge."],
				Interp: ["read interpolation", "Choose stepped sample reading or smooth interpolation. This changes the memory's playback texture."],
				Pan: ["pan", "Place this Coco between the left and right output channels."],
				SlotPre: ["root input level", "Set how much of this side's processed root enters the Coco input mix. Open the heart to capture it."],
				SlotDeck: ["direct deck level", "Set the direct file-deck feed into this Coco, independent of the root path. Open the heart to capture it."]
			)[suffix];
			if(entry.notNil) { ^this.note(prefix ++ entry[0], entry[1]) };
		};
		if(s.beginsWith("deck")) {
			prefix = "deck " ++ s[4] ++ " / ";
			entry = (
				Level: ["level", "Drag up/down on the bird's crescent to set its level before Coco capture and Master dry monitoring."],
				Rate: ["rate", "Set file playback speed. Negative rates play backwards; this is independent of the Coco memory clock."],
				Loop: ["loop", "Choose whether the loaded sound file repeats or plays through once."]
			)[s.drop(5).asSymbol];
			if(entry.notNil) { ^this.note(prefix ++ entry[0], entry[1]) };
		};
		if(s.beginsWith("pre")) {
			^this.topic(if(s.endsWith("Src")) { [\source, if(s[3] == $A) { 0 } { 1 }, \none] } {
				[\preamp, if(s[3] == $A) { 0 } { 1 }]
			})
		};
		if(#[qFreq, qChaos].includes(name)) { ^this.topic([\eye]) };
		if(CQGuiPanels.masterParamNames.includes(name)) { ^this.master(name) };
		if(s.size > 2 and: { s[0] == $c and: { s[1].isDecDigit } }) {
			prefix = "castle " ++ s[1] ++ " / ";
			if(s.endsWith("Range")) { ^this.note(prefix ++ "range", "Click the bottom ripple to cycle slow, middle and audio oscillator ranges.") };
			if(s.endsWith("Send")) { ^this.topic([\castle, s[1].digit - 1]) };
			^this.note(prefix ++ "detune", "Offset this castle's rate relative to the others. The central eye still scales all five together.")
		};
		^switch(name,
			\qShLag, { this.note("castles / sample slew", "Smooth the transitions in the castles' sampled outputs. More slew softens abrupt steps.") },
			\liveGain, { this.note("live input / gain", "Set the live-input level before the root paths. Select a live source on a root strand to use it.") },
			\micGain, { this.note("microphone / gain", "Raise from zero to admit the configured microphone. Use headphones when opening the mic near speakers.") },
			{ nil })
	}

	*master { |key|
		^switch(key,
			\masterVol, { this.note("master / volume", "Drag up/down to set the final listening level. The output safety chain stays active.") },
			\masterDry, { this.note("master / dry", "Preview file and live sources alongside the Coco memories. Lower dry after capturing a loop to hear the memory alone.") },
			\patchDepth, { this.note("master / cable depth", "Scale all cable influence together. Lower it to soften modulation without removing any connections.") },
			\out, { this.note("output meters", "Left and right listening levels after the output safety chain.") },
			\gr, { this.note("limiter / gain reduction", "Shows how much the final limiter reduces peaks. Sustained reduction suggests easing input levels or feedback.") },
			\notice, { this.note("instrument status", "Audio, microphone and recording notices appear here. Drawer > audio contains device and routing setup.") },
			\rec, { this.note("REC / record output", "Click or press R to start or stop an output recording. This is separate from writing a Coco memory.") },
			\panic, { this.note("PANIC / silence", "Click or press Space to mute all outputs immediately. Repeat to resume; the patch and memories remain.") },
			\monitorMute, { this.master(\panic) },
			{ nil })
	}

	*endpoint { |name|
		var s = name.asString, suffix, label, purpose, n;
		if(CQMatrix.srcIndex(name).notNil) {
			n = s.last;
			purpose = case
				{ s.beginsWith("tri") } { ["castle " ++ n ++ " / triangle output", "Sends a continuous triangle signal."] }
				{ s.beginsWith("cas") } { ["castle " ++ n ++ " / sampled output", "Sends sampled steps or tracking motion."] }
				{ s.beginsWith("env") } { ["root " ++ n ++ " / envelope output", "Sends the processed source's loudness."] }
				{ s.beginsWith("org") } { ["Coco " ++ n ++ " / Organ output", "Sends the Organ signal even with its audible level down."] }
				{ true } { ["Coco " ++ n ++ " / memory output", "Sends this memory as a modulation signal."] };
			^this.note(purpose[0], purpose[1] ++ " Drag to a hollow input; right-drag starts a negative cable.")
		};
		if(s.beginsWith("coco")) {
			label = "Coco " ++ s[4] ++ " / "; suffix = s.drop(6).asSymbol;
			purpose = (
				spaf: ["speed input", "Modulates the memory clock. Raise the purple SP.AF crescent from zero to hear the influence."],
				inVca: ["input gain socket", "Modulates the gain of new sound entering memory."],
				fbVca: ["feedback socket", "Modulates the gain of recirculating sound."],
				flip: ["flip trigger", "Each rising trigger reverses direction once. It stays there until the next trigger or chevron tap."],
				skip: ["restart input", "A rising trigger restarts the loop without clearing memory."],
				record: ["record input", "A high gate inverts the heart's write/hold setting for its duration."],
				organ: ["Organ input", "Modulates the audible Organ level; pitch still follows the memory clock."],
				slotQ: ["Q inlet socket", "Modulates how much castle audio enters this Coco."],
				slotX: ["bridge socket", "Modulates the other Coco's feed into this one. The opposite direction is independent."],
				dust: ["dust input", "Modulates noise amount. Grain remains a separate hand setting on the veil."]
			)[suffix];
		};
		if(s.beginsWith("castle")) {
			label = "castle " ++ s[6] ++ " / ";
			purpose = if(s.endsWith("_fm")) {
				["FM input", "Modulates this castle's oscillator frequency."]
			} { ["step input", "A rising trigger changes this castle's sampling relationship."] };
		};
		if(s.beginsWith("deck")) {
			label = "deck " ++ s[4] ++ " / ";
			purpose = ["rate input", "Modulates file playback speed; negative rates play backwards."];
		};
		^purpose !? { this.note(label ++ purpose[0], purpose[1] ++ " Drop an output cable here.") }
	}

	*ui { |key|
		var entry = (
			inspectInfo: ["info mode", "Click a shape to explore it. The guides and shortcuts are at top left. Esc or info returns to playing."],
			guidedInfo: ["basics & first loop / play", "Read, then try the described controls. Hover pictures to locate them. Next continues; close inspects; Esc leaves info."],
			advancedInfo: ["organ & advanced / play", "Build the patch one step at a time. Hover pictures to locate controls. Next continues; close inspects; Esc leaves info."],
			advanced: [CQInfoAdvanced.label, "Build an Organ patch, shape roots, connect audio paths, then explore patch memories and the Drawer. Play the illustrated steps."],
			info: ["info / inspect", "Turn on click-to-inspect cards and illustrated guides. Click info again or press Esc to return to ordinary playing."],
			readingGuide: ["basics & first loop / reading", "Read here; use the guide button above to start playing through it. Esc leaves info so you can try the gestures."],
			tour: [CQInfoTour.label, "Meet the instrument, capture two voices, then explore cables, feedback, dust and the moon. Follow the illustrated steps while playing."],
			shortcuts: ["shortcuts", "Read the performance keys and contextual cable/navigation keys."],
			drawer: ["drawer", "Open the workbench for the exact routing matrix, setup, patch history, snapshots and audio settings."],
			inspector: ["inspector", "The exact cable matrix. Press a cell to audition; double-click to plant, drag vertically to trim, right-click to clear."],
			workshop: ["workshop", "Exact settings for file decks, live-input trims, Coco converters, input mixes and castle detuning."],
			spellbook: ["spellbook", "Read the history of committed cable edits and copy patch data for code."],
			memory: ["memory", "Save and recall whole-instrument snapshots, or morph between two saved states."],
			audio: ["audio", "Choose audio devices and routes. Review the changes, then confirm to save the setup and restart audio."],
			preset: ["patch preset", "Choose a starting preset, then use apply preset to load its connections and musical settings."],
			'apply preset': ["apply preset", "Load the selected preset's connections and musical settings. Ctrl+Z restores the preceding cable patch."],
			'save snapshot': ["save snapshot", "Save the current instrument state under the entered name, including parameters and cable connections."],
			recall: ["recall snapshot", "Load the selected saved state into the instrument."],
			refresh: ["refresh snapshots", "Rescan the snapshot folder and update the list of saved states."],
			morph: ["morph snapshots", "Travel from snapshot A to B over the chosen duration."],
			'copy line': ["copy patch line", "Copy the selected committed edit as code to the clipboard."],
			'copy all as preset data': ["copy preset data", "Copy the current routing matrix as preset data for code."],
			'discard changes': ["discard audio changes", "Reset the edited audio setup to the saved configuration."],
			cancel: ["cancel", "Cancel the pending action and return to settings."],
			'apply & restart audio': ["apply audio setup", "Review the edited devices and routes, then confirm to save them and restart audio."]
		)[key];
		^entry !? { this.note(entry[0], entry[1]) }
	}
}
