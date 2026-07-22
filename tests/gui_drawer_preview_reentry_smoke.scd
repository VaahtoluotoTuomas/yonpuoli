// IDE regression: evaluating gui_drawer_preview.scd twice must replace the old
// GUI and still open the Inspector, never accumulate preview schedulers/windows.

(
var previewPath = thisProcess.nowExecutingPath.dirname +/+ "gui_drawer_preview.scd";
var first, second, ok = true;
var check = { |condition, message|
	if(condition) { ("ok:   " ++ message).postln } {
		ok = false;
		("FAIL: " ++ message).postln;
	};
};
CQInkControl.resetDrawErrors;
thisProcess.interpreter.executeFile(previewPath);
AppClock.sched(0.5, {
	first = ~cqDrawerReview;
	thisProcess.interpreter.executeFile(previewPath);
	AppClock.sched(0.5, {
		second = ~cqDrawerReview;
		check.value(first.notNil and: { first.isClosed },
			"second IDE evaluation closes the previous preview");
		check.value(second.notNil and: { second !== first } and: { second.isClosed.not },
			"second IDE evaluation owns one fresh preview");
		second.shell.toggleDrawer;
		AppClock.sched(0.25, {
			check.value(second.shellState.drawerOpen
				and: { second.shell.workspaceStack.index == 1 },
			"fresh preview opens the Inspector workbench from its toolbar tab");
			check.value(CQInkControl.drawErrors == 0,
				"re-entered preview paints without callback errors");
			second.free;
			if(ok) { "CQ_GUI_DRAWER_REENTRY_PASSED".postln; 0.exit } {
				"CQ_GUI_DRAWER_REENTRY_FAILED".postln; 1.exit
			};
			nil
		});
		nil
	});
	nil
});
)
