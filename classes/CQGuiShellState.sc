// CQGuiShellState — serverless presentation state for Yölahti's one-window,
// Night-only shell (GUI_SPEC §2, §11). It owns no musical values: Night is
// the instrument, and this state only chooses between it and the workbench.

CQGuiShellState {
	var <>audition;
	var <lifecycleState = \building, <viewMode = \night;
	var <drawerOpen = false, <drawerTab = \inspector;

	*new { |audition| ^super.newCopyArgs(audition) }

	minSize { ^[1280, 720] }
	masterVisible { ^true }
	canBuild { ^lifecycleState == \building }

	markOpen {
		if(lifecycleState == \building) {
			lifecycleState = \open;
			this.changed(\lifecycle, lifecycleState);
		};
		^this
	}

	beginClose {
		if(#[building, open].includes(lifecycleState)) {
			lifecycleState = \closing;
			this.changed(\lifecycle, lifecycleState);
		};
		^this
	}

	close {
		if(lifecycleState != \closed) {
			audition !? { audition.windowClose };
			lifecycleState = \closed;
			this.changed(\lifecycle, lifecycleState);
		};
		^this
	}

	toggleDrawer {
		if(lifecycleState == \closed) { ^this };
		// A stage change is an audition exit path just as the former view cut
		// was: no hidden Night hand or Inspector preview may keep sounding.
		audition !? { audition.viewToggle };
		drawerOpen = drawerOpen.not;
		this.changed(\drawer, drawerOpen, drawerTab);
		^this
	}

	// The normative page list lives with the pages themselves
	// (CQGuiDrawer.tabs), so a page cannot appear in the toolbar and be refused
	// by presentation state.
	selectDrawer { |tab|
		if(CQGuiDrawer.tabs.includes(tab)) {
			drawerTab = tab;
			this.changed(\drawer, drawerOpen, drawerTab);
		};
		^this
	}
}
