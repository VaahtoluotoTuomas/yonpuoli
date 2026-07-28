// CQGuiShellState — serverless local presentation state for the one-window
// Archipelago shell (GUI_SPEC §2, §11). It owns no musical values and only
// tells CQGui which already-derived view to show.

CQGuiShellState {
	var <>audition;
	var <lifecycleState = \building, <viewMode = \day;
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

	toggleView { |textHasFocus = false|
		if(textHasFocus or: { lifecycleState == \closed }) { ^this };
		audition !? { audition.viewToggle };
		// Day and Night are peer instrument destinations. Choosing either while
		// the workbench is visible returns to the instrument in the same action.
		if(drawerOpen) {
			drawerOpen = false;
			this.changed(\drawer, drawerOpen, drawerTab);
		};
		viewMode = if(viewMode == \day) { \night } { \day };
		this.changed(\view, viewMode);
		^this
	}

	toggleDrawer {
		if(lifecycleState == \closed) { ^this };
		drawerOpen = drawerOpen.not;
		this.changed(\drawer, drawerOpen, drawerTab);
		^this
	}

	// the normative page list lives with the pages themselves (CQGuiDrawer.tabs),
	// so a fourth page cannot appear in one place and be rejected in another
	selectDrawer { |tab|
		if(CQGuiDrawer.tabs.includes(tab)) {
			drawerTab = tab;
			this.changed(\drawer, drawerOpen, drawerTab);
		};
		^this
	}
}
