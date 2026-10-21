// View-only, click-to-inspect state. Enabling never opens a card. A selected
// note stays put until another selection, close or exit; there are no timers.
CQInfoState {
	var <enabled = false, <playing = false, <expanded = false, <target;
	var <groupPage = 0;

	*new { ^super.new }
	*guideClass { |kind| ^switch(kind, \tour, { CQInfoTour }, \advanced, { CQInfoAdvanced }, { nil }) }
	*pageCount { |kind| ^if(kind == \shortcuts) { 2 } { this.guideClass(kind) !? { |guide| guide.count } ? 0 } }
	guideKind { ^if(target.notNil and: { this.class.guideClass(target[0]).notNil }) { target[0] } { nil } }
	guideClass { ^this.class.guideClass(this.guideKind) }
	isGuide { ^this.guideClass.notNil }
	pageCount {
		^if(target.notNil and: { target[0] == \group }) {
			CQInfoPages.pageCount(target)
		} { this.class.pageCount(target !? { target[0] }) }
	}
	pageIndex {
		if(target.isNil) { ^0 };
		^if(target[0] == \group) { groupPage } { target[1] ? 0 }
	}
	inspecting { ^enabled and: { playing.not } }
	canPage { |delta|
		^enabled and: { this.pageCount > 1 }
			and: { (this.pageIndex + delta).inclusivelyBetween(0, this.pageCount - 1) }
	}
	footerItems {
		^if(this.pageCount > 1) {
			[["back", \back, this.canPage(-1)], ["next", \next, this.canPage(1)], ["close", \close, true]]
		} {
			if(this.pageCount == 1) { [["close", \close, true]] } {
				[[if(expanded) { "less" } { "more" }, \more, true], ["close", \close, true]]
			}
		}
	}
	enabled_ { |flag|
		this.dismiss; enabled = flag == true;
		^this
	}
	select { |next|
		if(this.inspecting.not) { ^false };
		target = next; expanded = false;
		groupPage = 0;
		^true
	}
	more {
		if(enabled and: { target.notNil } and: { this.pageCount == 0 }) { expanded = expanded.not };
		^this
	}
	tour { |step = 0, play = true|
		^this.guide(\tour, step, play)
	}
	advanced { |step = 0| ^this.guide(\advanced, step, true) }
	guide { |kind, step = 0, play = true|
		if(this.class.guideClass(kind).isNil) { ^this };
		if(enabled) {
			playing = play == true; expanded = false;
			target = [kind, step.asInteger.clip(0, this.class.pageCount(kind) - 1)];
		};
		^this
	}
	page { |delta|
		if(enabled and: { this.pageCount > 0 }) {
			switch(target[0],
				\group, { groupPage = (groupPage + delta).asInteger.clip(0, this.pageCount - 1) },
				\shortcuts, { this.shortcuts(target[1] + delta) },
				\tour, { this.guide(\tour, target[1] + delta, playing) },
				\advanced, { this.guide(\advanced, target[1] + delta, playing) });
		};
		^this
	}
	shortcuts { |page = 0|
		if(enabled) {
			playing = false; expanded = false;
			target = [\shortcuts, page.asInteger.clip(0, this.class.pageCount(\shortcuts) - 1)];
		};
		^this
	}
	dismiss { target = nil; playing = false; expanded = false; ^this }
}
