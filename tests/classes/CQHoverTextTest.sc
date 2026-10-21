CQHoverTextTest : UnitTest {
	test_everyPerformanceAndWorkshopParameterHasAConciseNote {
		(CQGuiNightMap.reachableParamNames ++ CQGuiWorkshop.paramNames
			++ CQGuiPanels.masterParamNames).asSet.do { |name|
			var note = CQHoverText.param(name);
			this.assert(note.notNil, "hover note for parameter " ++ name);
			if(note.notNil) {
				this.assert(note[\title].notEmpty and: { note[\text].notEmpty }
					and: { note[\text].size < 190 }, "concise authored note for " ++ name);
			};
		};
		(CQMatrix.sourceNames ++ CQMatrix.destNames).do { |name|
			var note = CQHoverText.endpoint(name);
			this.assert(note.notNil and: { note[\text].notEmpty }, "port explains its own signal: " ++ name);
		};
		this.assert(CQHoverText.topic(nil).isNil, "empty space has no help text");
		this.assert(CQHoverText.topic([\water]).isNil, "background is not presented as a control");
	}
	test_notesDistinguishWriteDirectionAndOutputRecording {
		this.assert(CQHoverText.param(\coco1Hold)[\text].contains("temporarily"), "right hold is momentary");
		this.assert(CQHoverText.endpoint(\coco1_record)[\text].contains("inverts"), "record input inverts hold");
		this.assert(CQHoverText.endpoint(\coco1_flip)[\text].contains("rising trigger"), "flip input toggles on a rising trigger");
		this.assert(CQHoverText.master(\rec)[\text].contains("separate"), "output recording is distinct from memory");
		this.assert(CQHoverText.param(\coco1SlotX)[\text].contains("receive"), "bridge direction names the receiver");
		this.assert(CQHoverText.endpoint(\coco1_spaf)[\text].contains("from zero"), "speed socket explains the depth gate");
	}
}
