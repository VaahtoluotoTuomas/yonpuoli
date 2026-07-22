// CQ-AUD-004/CQ-AUD-019 contract: immediate free must cancel CQGui's queued
// deferred build, listener registration and REC task before they are created.

CQGuiLifecycleTest : UnitTest {
	var gui;

	tearDown {
		gui !? { gui.free };
		#[cq_gui_meters, cq_gui_gr, cq_gui_coco, cq_gui_castle,
			cq_gui_spesal, cq_gui_hot].do { |key|
			OSCdef.all[key] !? { |def| def.free };
		};
	}

	test_immediateFreeCancelsDeferredInit {
		var state = CQState(nil);
		var matrix = CQMatrix(nil);
		var config = CQConfig.validate((headless: true));
		var constructed = Condition.new;
		// Function:defer executes immediately when already on AppClock. Construct
		// from SystemClock to exercise the actual queued-init cancellation seam.
		{
			gui = CQGui(state, matrix, config);
			gui.free;
			constructed.unhang;
		}.fork(SystemClock);
		constructed.hang;
		0.05.wait;
		this.assert(gui.window.isNil,
			"cancelled deferred GUI init never constructs a window (got %)"
				.format(gui.window));
		#[cq_gui_meters, cq_gui_gr, cq_gui_coco, cq_gui_castle,
			cq_gui_spesal, cq_gui_hot].do { |key|
			this.assert(OSCdef.all[key].isNil,
				"cancelled deferred GUI init does not leak %".format(key));
		};
	}
}
