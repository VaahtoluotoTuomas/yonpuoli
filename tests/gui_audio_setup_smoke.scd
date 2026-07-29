// AIO-3 Qt smoke: editable draft, directional/deduplicated device menus,
// inline restart confirmation and stable compact/roomy composition.
// No real OS device, file or server is used.
// Run only through tests/run_sclang_gate.ps1.

(
var longInput = "WASAPI : Extremely Long Input Device Name With Loopback Channels 1–64";
var longOutput = "WASAPI : Extremely Long Output Device Name With Loopback Channels 1–64";
var wrappedInput = "Windows WDM-KS : Headset Hands-Free%0"
	++ Char.ret ++ Char.nl ++ Char.tab ++ ";(Tuomas - AirPods Pro)";
var common = "ASIO : Öljyjärvi \"main\" \\ loopback";
var config = CQConfig.validate((
	headless: true,
	device: [longInput, longOutput],
	inChans: [3, 3],
	micChan: nil,
	outChans: [0, 1],
	stemOuts: nil,
	recStems: false
));
var model = CQAudioConfig(config,
	Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "aio-base.scd");
var extraInputs = [wrappedInput] ++ Array.fill(23, { |index|
	"WASAPI : auxiliary input %".format(index + 1)
});
var catalog = (
	all: [longInput, longOutput, common, longInput],
	inputs: [longInput, longInput, common] ++ extraInputs,
	outputs: [longOutput, common, common]
);
var page, window, ok = true, applies = 0, retries = 0;
var restartStatus = \idle, restartError, recorderState = \idle;
var capturedProfile;
var controller = (
	lifecycle: { \running },
	recorderState: { recorderState },
	status: { restartStatus },
	error: { restartError },
	apply: { |audioModel|
		applies = applies + 1;
		capturedProfile = audioModel.draftProfile.copy;
		restartStatus = \restarting;
		true
	},
	retry: {
		retries = retries + 1;
		restartStatus = \ready;
		true
	}
);
var compactOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "audio-setup-compact.png";
var roomyOutput = Cocoquantus.repoDir +/+ "tests" +/+ "tmp"
	+/+ "audio-setup-roomy.png";
var check = { |condition, message|
	if(condition) { ("ok:   " ++ message).postln } {
		ok = false;
		("FAIL: " ++ message).postln;
	};
};
var selectMenu = { |menu, index|
	var rowH;
	menu.open;
	rowH = menu.view.bounds.height.max(24);
	menu.popupView.mouseDownAction.value(
		menu.popupView, 4, (index + 0.5) * rowH);
};
var render;

render = { |path|
	var width = page.view.bounds.width.asInteger;
	var height = page.view.bounds.height.asInteger;
	var origin = page.view.absoluteBounds.leftTop;
	var image = Image(width, height);
	var paint = { |child|
		var point = child.absoluteBounds.leftTop - origin;
		Pen.use {
			Pen.translate(point.x, point.y);
			child.drawFunc.value(child);
		};
	};
	var visibleMenus = page.hostMenus ++ page.deviceMenus
		++ page.inputMenus ++ page.outputMenus;
	image.draw {
		Pen.fillColor = CQInk.palette(\night)[\recess];
		Pen.fillRect(Rect(0, 0, width, height));
		([page.hostView, page.inputView, page.outputView, page.restartView]
			++ visibleMenus.collect(_.view)
			++ [
			page.refreshButton.view, page.discardButton.view,
			page.applyButton.view]
		).do(paint);
	};
	image.write(path);
	image.free;
};

{
	try {
		CQInkControl.resetDrawErrors;
		page = CQGuiAudioSetup(nil, model, { catalog.deepCopy }, controller);
		window = Window("Yölahti · Audio I/O · AIO-3",
			Rect(70, 70, 1272, 526), true)
			.background_(CQInk.palette(\night)[\recess]);
		window.view.layout = VLayout(page.view).margins_(0).spacing_(0);
		window.front;
		AppClock.sched(0.4, {
			try {
				var hostRows = page.hostRows;
				var beforeRead = page.deviceReadCount;
				var inHeight = page.inputView.bounds.height;
				var outHeight = page.outputView.bounds.height;
				var refreshRight = page.refreshButton.view.absoluteBounds.right;
				var menuRight = page.hostMenus.last.view.absoluteBounds.right;
				check.value(page.deviceReadCount == 1,
					"device catalog is read exactly once when the page is built");
				check.value(page.deviceSnapshot.size == 27
					and: { page.inputDeviceSnapshot.size == 26 }
					and: { page.outputDeviceSnapshot.size == 2 }
					and: { page.singleDeviceSnapshot == [common] },
					"exact duplicates collapse and directional capability filters choices");
				check.value(CQGuiAudioSetup.writable
					and: { model.dirty.not and: { model.valid } },
					"AIO-3 starts as a clean active/draft pair");
				check.value(hostRows[1][\value] == "split devices"
					and: { hostRows[2][\value] == longInput }
					and: { hostRows[3][\value] == longOutput },
					"split input/output device Strings remain exact model data");
				check.value((refreshRight - menuRight).abs <= 1,
					"refresh devices shares the controls' right-hand rail");
				check.value(page.inputRows[0][\value] == "ch 4"
					and: { page.inputRows[1][\value] == "ch 4" },
					"intentional mono duplication is shown one-based");
				check.value(page.inputRows[2][\value] == "off"
					and: { page.outputRows[2][\value] == "off" }
					and: { page.outputRows[3][\value] == "—" }
					and: { page.outputRows[4][\value] == "—" },
					"mic/stems off stay visible without inventing channels");
				check.value(inHeight == outHeight
					and: { page.inputRows.size == 6 }
					and: { page.outputRows.size == 6 },
					"the two I/O islands keep stable equal-height composition");
				check.value(page.rail.bounds.width
					== CQGuiScale.railWidth(\compact, 1272)
					and: { page.rail.bounds.bottom <= page.view.bounds.bottom },
					"compact Audio uses the shared rail and needs no scroll");
				check.value(CQInkControl.drawErrors == 0,
					"compact Audio page paints without callback errors");

				page.deviceMenus.first.open;
				check.value(page.deviceMenus.first.popupView.bounds.height
					<= (page.deviceMenus.first.view.bounds.height.max(24) * 16),
					"a large directional list stays on-screen in sixteen scrollable rows");
				page.deviceMenus.first.popupView.mouseOverAction.value(
					page.deviceMenus.first.popupView, 4,
					page.deviceMenus.first.view.bounds.height * 1.5);
				check.value(page.deviceMenus.first.popupHover == 1,
					"opened exact device rows keep one-row hover grammar");
				page.deviceMenus.first.popupView.mouseWheelAction.value(
					page.deviceMenus.first.popupView, 4,
					page.deviceMenus.first.view.bounds.height * 1.5,
					0, 0, -1);
				check.value(page.deviceMenus.first.scrollOffset == 1
					and: { page.deviceMenus.first.popupHover == 2 },
					"the capped device popup scrolls through the remaining exact rows");
				check.value(CQInkMenu.ellipsize(longInput, 170, 12)
					.includes(Char.nl).not,
					"long device names are shortened before Qt can wrap them");
				check.value(CQInkMenu.ellipsize(wrappedInput, 4096, 12)
					== "Windows WDM-KS : Headset Hands-Free%0 ;(Tuomas - AirPods Pro)",
					"embedded Windows endpoint line breaks collapse only in display text");
				page.deviceMenus.first.close;

				// Select 96 kHz: only draft changes, active keeps playing at 48 kHz.
				selectMenu.value(page.hostMenus[1], 2);
				check.value(model.dirty and: { model.valid }
					and: { model.draftAt(\sampleRate) == 96000 }
					and: { model.activeAt(\sampleRate) == 48000 },
					"an ordinary field edit changes only the valid draft");
				check.value(page.restartRows[0][\value]
					== "valid draft · restart required",
					"restart gate states the dirty valid boundary");

				// Make main outputs collide: invalid stays visible and never clamps.
				selectMenu.value(page.outputMenus[1], 0);
				check.value(model.valid.not
					and: { model.draftAt(\outChans) == [0, 0] }
					and: { model.validationError.find("unique").notNil },
					"invalid duplicate outputs remain an exact draft with one reason");
				check.value(page.restartRows[0][\family] == \danger,
					"invalid draft uses the danger state");

				// Discard is the only write-like AIO-2 button and touches draft only.
				page.discardButton.view.mouseDownAction.value(
					page.discardButton.view, 4, 4);
				page.discardButton.view.mouseUpAction.value(
					page.discardButton.view, 4, 4);
				check.value(model.dirty.not and: { model.valid }
					and: { model.draftAt(\outChans) == [0, 1] },
					"discard returns every field to active without restart");
				restartStatus = \ready;
				page.refreshTransaction;
				check.value(page.restartRows[0][\value] == "audio ready"
					and: { page.restartRows[0][\family] == \input },
					"successful restart uses the static input-green ready state");
				restartStatus = \notSaved;
				restartError = "injected rename failure";
				page.refreshTransaction;
				check.value(page.restartRows[0][\value] == "running · not saved"
					and: { page.applyButton.label == "retry save" }
					and: { page.applyButton.enabled },
					"working audio with rename failure offers save retry without restart");
				page.applyButton.view.mouseDownAction.value(
					page.applyButton.view, 4, 4);
				page.applyButton.view.mouseUpAction.value(
					page.applyButton.view, 4, 4);
				check.value(retries == 1 and: { restartStatus == \ready },
					"retry save is a separate non-reboot action");
				restartStatus = \idle;
				restartError = nil;
				page.refreshTransaction;

				// Split -> single chooses the exact bidirectional intersection.
				selectMenu.value(page.modeMenu, 1);
				check.value(model.draftAt(\device) == common
					and: { model.activeAt(\device) == [longInput, longOutput] },
					"single mode cannot offer an input-only or output-only endpoint");

				// Only explicit refresh may query the provider again; draft survives.
				page.refreshButton.view.mouseDownAction.value(
					page.refreshButton.view, 4, 4);
				page.refreshButton.view.mouseUpAction.value(
					page.refreshButton.view, 4, 4);
				check.value(page.deviceReadCount == (beforeRead + 1)
					and: { model.draftAt(\device) == common },
					"refresh is explicit and preserves the draft");
				restartStatus = \rollbackReady;
				restartError = "candidate device missing";
				page.refreshTransaction;
				check.value(page.restartRows[0][\value]
					== "candidate failed · previous audio restored"
					and: { page.restartRows[0][\family] == \danger }
					and: { model.dirty },
					"rollback-ready state keeps the rejected draft visible in danger ink");
				restartStatus = \idle;
				restartError = nil;
				page.refreshTransaction;

				// An active recorder blocks before confirmation or persistence.
				recorderState = \recording;
				check.value(page.requestApply.not
					and: { page.restartRows[0][\value]
						== "stop REC before restarting audio" }
					and: { applies == 0 },
					"an active recorder blocks restart without touching the candidate");
				recorderState = \idle;
				page.refreshTransaction;

				// First Apply is confirmation only and names every destructive edge.
				page.applyButton.view.mouseDownAction.value(
					page.applyButton.view, 4, 4);
				page.applyButton.view.mouseUpAction.value(
					page.applyButton.view, 4, 4);
				check.value(page.confirming
					and: { applies == 0 }
					and: { page.restartRows[0][\value]
						== "audio stops · current unsaved patch resets · Server.default restarts" }
					and: { page.restartRows[1][\value]
						== "unrelated synths on Server.default also stop" },
					"first Apply enters the complete static inline confirmation");
				page.discardButton.view.mouseDownAction.value(
					page.discardButton.view, 4, 4);
				page.discardButton.view.mouseUpAction.value(
					page.discardButton.view, 4, 4);
				check.value(page.confirming.not
					and: { applies == 0 }
					and: { model.dirty },
					"cancel returns to the untouched draft without restarting");
				render.value(compactOutput);

				window.setInnerExtent(1434, 630);
				AppClock.sched(0.4, {
					try {
						page.applyScale(\roomy, page.view.bounds.width);
						AppClock.sched(0.2, {
							try {
								var score = CQGuiAudioSetup.layoutFor(\roomy);
								check.value(page.view.bounds.width >= 1400
									and: { page.step == \roomy }
									and: { page.rail.bounds.width
										== CQGuiScale.railWidth(\roomy,
											page.view.bounds.width) },
									"roomy Audio re-measures onto the shared wider rail");
								check.value(page.hostView.bounds.height == score[\hostHeight]
									and: { page.inputView.bounds.height == score[\ioHeight] }
									and: { page.restartView.bounds.height
										== score[\restartHeight] },
									"roomy re-measure changes metrics without rebuilding");
								check.value(page.deviceReadCount == (beforeRead + 1),
									"resize does not refresh OS devices");
								check.value(page.inputView.bounds.height
									== page.outputView.bounds.height
									and: { page.rail.bounds.bottom
										<= page.view.bounds.bottom }
									and: { page.applyButton.view.absoluteBounds.right
										<= page.restartView.absoluteBounds.right },
									"roomy draft layout and restart controls are contained");
								check.value(CQInkControl.drawErrors == 0,
									"roomy Audio page paints without callback errors");
								render.value(roomyOutput);
								check.value(page.requestApply
									and: { page.confirmRestart }
									and: { applies == 1 }
									and: { capturedProfile[\device] == common },
									"confirmed restart sends only captured model data to the facade");
								window.close;
								if(ok) {
									"CQ_GUI_AUDIO_PASSED".postln;
									0.exit;
								} {
									"CQ_GUI_AUDIO_FAILED".postln;
									1.exit;
								};
							} { |error|
								error.reportError;
								window.close;
								"CQ_GUI_AUDIO_FAILED".postln;
								1.exit;
							};
							nil
						});
					} { |error|
						error.reportError;
						window.close;
						"CQ_GUI_AUDIO_FAILED".postln;
						1.exit;
					};
					nil
				});
			} { |error|
				error.reportError;
				window.close;
				"CQ_GUI_AUDIO_FAILED".postln;
				1.exit;
			};
			nil
		});
	} { |error|
		error.reportError;
		window !? { window.close };
		"CQ_GUI_AUDIO_FAILED".postln;
		1.exit;
	};
}.defer;
)
