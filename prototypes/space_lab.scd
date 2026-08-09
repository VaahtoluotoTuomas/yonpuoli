// Yönpuoli Space Lab — archived live harness.
//
// The production Space node and return bus were retired on 2026-07-28. This
// file now posts an archive notice and exits; the standalone SynthDefs remain
// in space_lab_defs.scd for offline research, especially memoryHalo.
//
// Main commands:
//   ~cqSpaceLab.playVariant(\utility);      // dark, stable reference reverb
//   ~cqSpaceLab.playVariant(\clockField);   // Coco-clock-relative echo field
//   ~cqSpaceLab.playVariant(\memoryHalo);   // read-only heads in the Coco memories
//   ~cqSpaceLab.playVariant(\confluence);   // mutual clocked resampling, not reverb
//
//   ~cqSpaceLab.nextVariant;                // cycle manually
//   ~cqSpaceLab.compareAll(25, 5);           // 5 s dry + 25 s wet per candidate
//   ~cqSpaceLab.setWet(0.35);                // 0 = exact dry reference
//   ~cqSpaceLab.setSends(0.65, 0.65);        // Coco 1 / Coco 2 feeds
//   ~cqSpaceLab.setShape(0.6);               // reach; candidate 4: relation
//   ~cqSpaceLab.setMotion(0.3);
//   ~cqSpaceLab.setTone(0.45);               // dark <-> bright
//   ~cqSpaceLab.setLevel(0.72);               // calibrated return trim
//   ~cqSpaceLab.freeLab;                     // remove lab, restore old Space state

(
var here, defs, server, graph, oldLab;

if(CQSpec.includes(\spaceMix).not) {
	"Yönpuoli Space Lab is archived: production Space routing was retired on 2026-07-28. The SynthDef research remains available in space_lab_defs.scd.".warn;
} {
if(CQ.isBooted.not) {
	"Yönpuoli Space Lab: CQ.boot must be fully ready first.".warn;
} {
	here = File.realpath(thisProcess.nowExecutingPath).dirname;
	defs = thisProcess.interpreter.executeFile(here +/+ "space_lab_defs.scd");
	server = CQ.server;
	graph = CQ.graph;
	oldLab = ~cqSpaceLab;
	oldLab !? {
		if(oldLab.isKindOf(Event)) {
			// One-time recovery for the first Space Lab revision: its Event
			// parent supplied a default \synth Function when the local field was
			// nil, so startup could stop after pausing the placeholder. Use
			// direct dictionary access here — never the inherited Event values.
			oldLab[\stopped] = true;
			oldLab[\comparison] !? { |routine| routine.stop };
			oldLab[\tracker] !? { |routine| routine.stop };
			CQ.set(\spaceMix, 0);
			oldLab[\synth] !? { |synth| synth.free };
			graph.synths[\space] !? { |space| space.run(true) };
			oldLab[\previous] !? { |previous|
				CQ.set(\coco1Send, previous[\send1]);
				CQ.set(\coco2Send, previous[\send2]);
				CQ.set(\spaceMix, previous[\mix]);
			};
			"Yönpuoli Space Lab: recovered the earlier failed Event controller."
				.postln;
		} {
			oldLab.freeLab
		};
	};

	// A plain, parentless controller — deliberately NOT an Event. Event supplies
	// musical defaults such as \synth, \id and \isPlaying; an empty lab field
	// would otherwise fall through to those functions because dictionaries
	// remove nil-valued keys.
	~cqSpaceLab = IdentityDictionary.new(64, nil, nil, true).putAll((
		variants: #[utility, clockField, memoryHalo, confluence],
		defNames: (
			utility: \cqProtoSpaceUtility,
			clockField: \cqProtoSpaceClockField,
			memoryHalo: \cqProtoSpaceMemoryHalo,
			confluence: \cqProtoSpaceConfluence
		),
		descriptions: (
			utility: "dark utility diffusion",
			clockField: "Coco-clock-relative field",
			memoryHalo: "read-only Coco memory heads",
			confluence: "mutual clocked resampling"
		),
		server: server,
		graph: graph,
		defs: defs,
		synth: nil,
		tracker: nil,
		comparison: nil,
		current: nil,
		shapeValue: 0.6,
		motionValue: 0.3,
		toneValue: 0.46,
		levelValue: 0.72,
		previous: (
			mix: CQ.get(\spaceMix),
			send1: CQ.get(\coco1Send),
			send2: CQ.get(\coco2Send)
		),
		stopped: false,

		feeds: { |self|
			[
				CQ.get(\coco1Send) * CQ.get(\coco1Level),
				CQ.get(\coco2Send) * CQ.get(\coco2Level)
			]
		},

		spawnArgs: { |self|
			var buses, buffers, feeds;
			buses = self.graph.buses;
			buffers = self.graph.buffers;
			feeds = self.feeds;
			[
				\inBus, buses[\space].index,
				\outBus, buses[\spaceRet].index,
				\modSrcBus, buses[\modSrc].index,
				\coco1Bus, buses[\coco1].index,
				\coco2Bus, buses[\coco2].index,
				\buf1, buffers[\coco1].bufnum,
				\buf2, buffers[\coco2].bufnum,
				\speed1, CQ.get(\coco1Speed),
				\speed2, CQ.get(\coco2Speed),
				\feed1, feeds[0],
				\feed2, feeds[1],
				\reach, self.shapeValue,
				\relation, self.shapeValue,
				\motion, self.motionValue,
				\tone, self.toneValue,
				\level, self.levelValue
			]
		},

		playVariant: { |self, which = \utility|
			var name;
			if(self.stopped) {
				"Yönpuoli Space Lab: this lab has been stopped; evaluate the file again.".warn;
			} {
				if(self.variants.includes(which).not) {
				("Yönpuoli Space Lab: unknown variant %, choose %."
					.format(which, self.variants)).warn;
				} {
					self.synth !? { self.synth.free };
					name = self.defNames[which];
					self.synth = Synth(name, self.spawnArgs,
						self.graph.groups[\fx], \addToTail);
					self.current = which;
					("SPACE LAB % — %"
						.format(which.asString.toUpper, self.descriptions[which])).postln;
				};
			};
			self
		},

		nextVariant: { |self|
			var index;
			index = self.variants.indexOf(self.current) ? -1;
			self.playVariant(self.variants.wrapAt(index + 1));
			self
		},

		setWet: { |self, value = 0.35|
			CQ.set(\spaceMix, value.clip(0, 1));
			self
		},

		setSends: { |self, coco1 = 0.65, coco2|
			coco2 = coco2 ? coco1;
			CQ.set(\coco1Send, coco1.clip(0, 1));
			CQ.set(\coco2Send, coco2.clip(0, 1));
			self
		},

		setShape: { |self, value = 0.6|
			self.shapeValue = value.clip(0, 1);
			self.synth !? {
				if(self.current == \confluence) {
					self.synth.set(\relation, self.shapeValue)
				} {
					self.synth.set(\reach, self.shapeValue)
				}
			};
			self
		},

		setMotion: { |self, value = 0.3|
			self.motionValue = value.clip(0, 1);
			if(self.current != \utility) {
				self.synth !? { self.synth.set(\motion, self.motionValue) };
			};
			self
		},

		setTone: { |self, value = 0.46|
			self.toneValue = value.clip(0, 1);
			self.synth !? { self.synth.set(\tone, self.toneValue) };
			self
		},

		setLevel: { |self, value = 0.72|
			self.levelValue = value.clip(0, 1);
			self.synth !? { self.synth.set(\level, self.levelValue) };
			self
		},

		dryReference: { |self|
			CQ.set(\spaceMix, 0);
			self
		},

		restoreWet: { |self|
			CQ.set(\spaceMix, 0.35);
			self
		},

		compareAll: { |self, wetSeconds = 25, drySeconds = 5|
			var wetValue;
			wetValue = CQ.get(\spaceMix).max(0.01);
			self.comparison !? { self.comparison.stop };
			self.comparison = Routine({
				self.variants.do { |variant|
					if(self.stopped.not) {
						self.playVariant(variant);
						self.setWet(0);
						("SPACE LAB DRY — next: %".format(variant)).postln;
						drySeconds.max(1).wait;
						self.setWet(wetValue);
						("SPACE LAB WET — % for % s"
							.format(variant, wetSeconds.max(1))).postln;
						wetSeconds.max(1).wait;
					};
				};
				if(self.stopped.not) {
					"SPACE LAB comparison complete; Confluence remains selected.".postln;
				};
				self.comparison = nil;
			}).play(AppClock);
			self
		},

		cancelComparison: { |self|
			self.comparison !? { self.comparison.stop; self.comparison = nil };
			self
		},

		showStatus: { |self|
			("SPACE LAB variant:% wet:% sends:%/% level:% shape:% motion:% tone:%"
				.format(
					self.current,
					CQ.get(\spaceMix).round(0.001),
					CQ.get(\coco1Send).round(0.001),
					CQ.get(\coco2Send).round(0.001),
					self.levelValue.round(0.001),
					self.shapeValue.round(0.001),
					self.motionValue.round(0.001),
					self.toneValue.round(0.001)
				)).postln;
			self
		},

		freeLab: { |self|
			if(self.stopped.not) {
				self.stopped = true;
				self.comparison !? { self.comparison.stop; self.comparison = nil };
				self.tracker !? { self.tracker.stop; self.tracker = nil };
				CQ.set(\spaceMix, 0);
				self.synth !? { self.synth.free; self.synth = nil };
				self.graph.synths[\space] !? { |space| space.run(true) };
				CQ.set(\coco1Send, self.previous[\send1]);
				CQ.set(\coco2Send, self.previous[\send2]);
				CQ.set(\spaceMix, self.previous[\mix]);
				"Yönpuoli Space Lab stopped; previous Space state restored.".postln;
			};
			self
		}
	));

	Routine({
		defs.do { |def| def.send(server) };
		server.sync;
		// The placeholder and the selected prototype must never sum. Pausing the
		// existing node preserves graph ownership and makes stop reversible.
		graph.synths[\space] !? { |space| space.run(false) };
		~cqSpaceLab.setSends(0.65, 0.65);
		~cqSpaceLab.setWet(0.35);
		~cqSpaceLab.playVariant(\utility);
		// Keep buffer-head rates and direct-feed variants aligned with normal
		// CQState edits while the disposable lab is open.
		~cqSpaceLab.tracker = Routine({
			var feeds;
			loop {
				if(~cqSpaceLab.stopped.not and: { CQ.isBooted }) {
					feeds = ~cqSpaceLab.feeds;
					~cqSpaceLab.synth !? { |synth|
						case
						{ ~cqSpaceLab.current == \memoryHalo } {
							synth.set(
								\speed1, CQ.get(\coco1Speed),
								\speed2, CQ.get(\coco2Speed),
								\feed1, feeds[0],
								\feed2, feeds[1]
							)
						}
						{ ~cqSpaceLab.current == \confluence } {
							synth.set(\feed1, feeds[0], \feed2, feeds[1])
						};
					};
				};
				0.1.wait;
			}
		}).play(AppClock);
		"Yönpuoli Space Lab ready — returns unity-calibrated; wet 0.35 is clearly audible."
			.postln;
		"Use ~cqSpaceLab.showStatus for current settings.".postln;
	}).play(AppClock);
};
};
)
