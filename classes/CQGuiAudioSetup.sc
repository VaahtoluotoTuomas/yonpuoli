// CQGuiAudioSetup — the Drawer's fifth/rightmost page (AIO-4).
//
// The page edits CQAudioConfig's draft only. No dropdown touches the running
// server. Apply first enters a static inline confirmation; only its restart
// action sends the non-Qt model into Cocoquantus.reconfigureAudio.

CQGuiAudioSetup {
	var gui, model, deviceProvider, transactionController;
	var <view, <rail, <hostView, <inputView, <outputView, <restartView;
	var <refreshButton, <discardButton, <applyButton;
	var <modeMenu, <singleDeviceMenu, <inputDeviceMenu, <outputDeviceMenu;
	var sampleRateMenu, blockSizeMenu, hardwareBufferMenu;
	var liveLMenu, liveRMenu, micMenu;
	var mainLMenu, mainRMenu, stemsMenu, stem1Menu, stem2Menu, recStemsMenu;
	var <deviceMenus, <allMenus, <hostMenus, <inputMenus, <outputMenus;
	var hostMenuRows, inputMenuRows, outputMenuRows;
	var devicePrimaryStack, deviceOutputStack;
	var <deviceSnapshot = #[], <inputDeviceSnapshot = #[];
	var <outputDeviceSnapshot = #[], <singleDeviceSnapshot = #[];
	var fullDeviceSnapshot = #[], fullInputDeviceSnapshot = #[];
	var fullOutputDeviceSnapshot = #[];
	var <deviceReadCount = 0, <deviceError;
	var <deviceRefreshAvailable = true;
	var <deviceCatalogSource = \configured, <showAllDrivers = false;
	var <advancedDeviceCount = 0;
	var <recorderCtl;
	var <step = \compact, <metrics, <pageWidth = 1272, <skin = \night;
	var ioBand, railLayout, topPad, hostGap, restartGap, bottomPad;
	var hostFirstSpacer, hostTitleSpacer, refreshRightSpacer;
	var inputTitleSpacer, outputTitleSpacer, restartButtonSpacer;
	var <confirming = false, <localStatus, <localError;
	var <inputPresence, <masterPresence, inputPresenceDraw, masterPresenceDraw;
	var presenceGains;
	var <presenceFrameCount = 0, <presenceRefreshCount = 0;
	var <presencePaintCount = 0, presenceSerial = 0, paintedPresenceSerial = 0;

	*new { |gui, audioConfig, provider, controller|
		^super.newCopyArgs(gui, audioConfig, provider, controller).prInit
	}

	*writable { ^true }

	*layoutFor { |step = \compact|
		var m = CQGuiScale.metrics(step);
		^(
			hostHeight: m[\titleHeight] + (7 * m[\rowHeight]),
			ioHeight: m[\titleHeight] + (6 * m[\rowHeight]),
			restartHeight: m[\titleHeight] + (2 * m[\rowHeight])
				+ m[\rowGap] + m[\boxHeight] + (2 * m[\contentInset])
		)
	}

	*sampleRateText { |rate|
		^switch(rate,
			44100, { "44.1 kHz" },
			48000, { "48 kHz" },
			96000, { "96 kHz" },
			{ "% Hz".format(rate) }
		)
	}

	*channelText { |index|
		^"ch %".format(CQAudioConfig.channelToDisplay(index))
	}

	*hardwareBufferText { |frames|
		^if(frames.isNil) { "driver default" } { frames.asString }
	}

	// AUD-W: the measured clock, never rounded into agreement with the request.
	*clockRateText { |rate|
		if(rate.isNumber.not) { ^"nothing" };
		^"% Hz".format(rate.asFloat.round(1))
	}

	// `_ListAudioDevices` touches the host audio subsystem. On the physical
	// Windows WASAPI split-device path, invoking it while scsynth owns the
	// endpoints reproducibly wedged the running server. The runtime page uses
	// only the exact configured endpoint(s); full discovery belongs to a
	// separately gated stopped-server workflow.
	*configuredDeviceCatalog { |device|
		^CQAudioDeviceCatalog.configured(device)
	}

	*presenceThreshold { ^(-48).dbamp }

	// A presence rail is deliberately coarse: it answers whether a physical
	// port carries anything, not how loud the instrument is. Quantizing the
	// -60..0 dB range prevents sub-pixel meter noise from asking Qt to repaint.
	*presenceNorm { |value|
		var norm;
		if(value.isNil) { ^nil };
		norm = ((value.asFloat.abs.max(1e-6).ampdb + 60) / 60).clip(0, 1);
		^(norm * 20).round / 20
	}

	*deviceModeText { |device|
		^switch(CQAudioConfig.deviceMode(device),
			\default, { "system default" },
			\single, { "single device" },
			{ "split devices" }
		)
	}

	*ellipsize { |string, width, fontSize|
		^CQInkMenu.ellipsize(string, width, fontSize)
	}

	railWidth { ^CQGuiScale.railWidth(step, pageWidth) }

	refreshDevices {
		var result;
		deviceReadCount = deviceReadCount + 1;
		deviceError = nil;
		try {
			result = deviceProvider.value;
			this.prInstallDeviceCatalog(result);
		} { |error|
			fullDeviceSnapshot = #[];
			fullInputDeviceSnapshot = #[];
			fullOutputDeviceSnapshot = #[];
			deviceSnapshot = #[];
			inputDeviceSnapshot = #[];
			outputDeviceSnapshot = #[];
			singleDeviceSnapshot = #[];
			deviceCatalogSource = \configured;
			advancedDeviceCount = 0;
			deviceError = error.errorString;
		};
		this.prSyncDraft;
		^this
	}

	toggleDriverView {
		if(advancedDeviceCount <= 0) { ^false };
		showAllDrivers = showAllDrivers.not;
		this.prApplyDeviceFilter;
		this.prSyncDraft;
		^true
	}

	transactionStatus {
		^localStatus ?? { transactionController[\status].value ? \idle }
	}

	requestApply {
		if(this.prRecorderBusy) {
			localStatus = \blocked;
			localError = "stop REC before restarting audio";
			this.prSyncDraft;
			^false
		};
		if(this.prCanApply.not) { ^false };
		confirming = true;
		localStatus = nil;
		localError = nil;
		this.prSyncDraft;
		^true
	}

	cancelConfirmation {
		confirming = false;
		localStatus = nil;
		localError = nil;
		this.prSyncDraft;
		^this
	}

	confirmRestart {
		var accepted;
		if(confirming.not) { ^false };
		confirming = false;
		localStatus = \restarting;
		localError = nil;
		// A dirty draft crosses into the transactional profile flow. A clean
		// page instead restarts the exact active config without writing a file.
		accepted = if(model.dirty) {
			transactionController[\apply].value(model) == true
		} {
			transactionController[\restart].value == true
		};
		if(accepted.not) {
			localStatus = transactionController[\status].value ? \failed;
			localError = transactionController[\error].value;
		};
		this.prSyncDraft;
		^accepted
	}

	retrySave {
		var accepted = transactionController[\retry].value == true;
		localStatus = transactionController[\status].value;
		localError = transactionController[\error].value;
		this.prSyncDraft;
		^accepted
	}

	refreshTransaction {
		localStatus = nil;
		localError = nil;
		this.prSyncDraft;
		^this
	}

	serverHealthChanged {
		this.prSyncDraft;
		^this
	}

	recorderStateChanged {
		if(localStatus == \blocked) {
			localStatus = nil;
			localError = nil;
		};
		this.prSyncDraft;
		^this
	}

	hostRows {
		var cfg = model.draft, device = model.draftAt(\device), mode;
		var responsive = this.prServerResponsive;
		// The list is a stopped-server snapshot, so a device plugged in
		// mid-session cannot appear until the next start. Say so where the
		// counts already are: the `scanned at boot` wording used to live on
		// the device control's label, which on any machine with a legacy
		// driver always loses that slot to `show all drivers` -- so the age
		// of the list was never visible at all (user review 2026-07-30,
		// after a reconnected K-Mix could not be found).
		//
		// It rides its OWN row since 2026-07-31. Riding the engine row was fine
		// while the note was short, but it ran under the `show all drivers`
		// button once the sclang clause was added, and AUD-W's danger text grows
		// the engine value further exactly when it must stay readable. Counts and
		// note stay together, which is all the 2026-07-30 review required.
		var listValue = if(deviceError.notNil) {
			"% in / % out · device scan failed".format(
				inputDeviceSnapshot.size, outputDeviceSnapshot.size)
		} {
			"% in / % out · %".format(
				inputDeviceSnapshot.size, outputDeviceSnapshot.size,
				this.class.deviceListNote(deviceCatalogSource,
					deviceRefreshAvailable))
		};
		var rows;
		var clockDanger = this.prAudioClockDanger;
		mode = CQAudioConfig.deviceMode(device);
		rows = [
			(label: "engine", value: ("% · active % · block % · hw %"
				.format(if(responsive.not) {
					"server not responding"
				} {
					// AUD-W: a responsive server whose clock is stuck is the exact
					// case this row used to report as healthy. Say the measured
					// truth beside the requested rate, never instead of it.
					if(clockDanger) {
						"audio clock stuck · reports %".format(
							this.class.clockRateText(this.prAudioClockRate))
					} {
						transactionController[\lifecycle].value
					}
				},
					this.class.sampleRateText(model.activeAt(\sampleRate)),
					model.activeAt(\blockSize),
					this.class.hardwareBufferText(
						model.activeAt(\hardwareBufferSize)))),
				family: if(responsive.not or: { clockDanger }) {
					\danger
				} {
					\line
				}),
			(label: "device list", value: listValue,
				family: if(deviceError.notNil) { \danger } { \line }),
			(label: "device mode", value: this.class.deviceModeText(device),
				control: true, dirty: model.dirtyAt(\device))
		];
		if(mode == \split) {
			rows = rows ++ [
				this.prDeviceRow("input device", device[0],
					this.prDeviceReported(device[0], \input)),
				this.prDeviceRow("output device", device[1],
					this.prDeviceReported(device[1], \output))
			];
		} {
			rows = rows ++ [
				this.prDeviceRow("device",
					if(device.isNil) { "system default" } { device },
					this.prDeviceReported(device, \single)),
				nil
			];
		};
		rows = rows ++ [
			(label: "sample rate",
				value: this.class.sampleRateText(cfg[\sampleRate]),
				control: true, dirty: model.dirtyAt(\sampleRate)),
			(label: "server block", value: cfg[\blockSize].asString,
				control: true, dirty: model.dirtyAt(\blockSize)),
			(label: "hardware buffer",
				value: this.class.hardwareBufferText(cfg[\hardwareBufferSize]),
				control: true, dirty: model.dirtyAt(\hardwareBufferSize))
		];
		^rows
	}

	inputRows {
		var input = model.draftAt(\inChans);
		var mic = model.draftAt(\micChan);
		var activeMic = model.activeAt(\micChan);
		^[
			(label: "live l", value: this.class.channelText(input[0]),
				control: true, dirty: model.dirtyAt(\inChans)),
			(label: "live r", value: this.class.channelText(input[1] ? input[0]),
				control: true, dirty: model.dirtyAt(\inChans)),
			(label: "mic", value: if(mic.isNil) { "off" } {
				"on · " ++ this.class.channelText(mic)
			}, control: true, dirty: model.dirtyAt(\micChan), muted: mic.isNil),
			this.prPresenceRow("live l signal", inputPresence[0],
				inputPresenceDraw[0], presenceGains[0], true),
			this.prPresenceRow("live r signal", inputPresence[1],
				inputPresenceDraw[1], presenceGains[0], true),
			this.prPresenceRow("mic signal", inputPresence[2],
				inputPresenceDraw[2], presenceGains[1], activeMic.notNil)
		]
	}

	masterPresenceText {
		^"master l % · r %".format(
			this.prPresenceWord(masterPresence[0]),
			this.prPresenceWord(masterPresence[1]))
	}

	masterPresenceFamily {
		^if(masterPresence.any { |value|
			value.notNil and: { value >= this.class.presenceThreshold }
		}) { \input } { \whisper }
	}

	presenceFrame { |inputs, master, gains|
		var nextInputs = this.prPresenceValues(inputs, 3);
		var nextMaster = this.prPresenceValues(master, 2);
		var nextInputDraw = nextInputs.collect { |value|
			this.class.presenceNorm(value)
		};
		var nextMasterDraw = nextMaster.collect { |value|
			this.class.presenceNorm(value)
		};
		var inputChanged = nextInputDraw != inputPresenceDraw;
		var masterChanged = nextMasterDraw != masterPresenceDraw;
		presenceFrameCount = presenceFrameCount + 1;
		inputPresence = nextInputs;
		masterPresence = nextMaster;
		inputPresenceDraw = nextInputDraw;
		masterPresenceDraw = nextMasterDraw;
		if(gains.notNil) {
			gains = this.prPresenceValues(gains, 2);
			if(gains != presenceGains) {
				presenceGains = gains;
				inputChanged = true;
			};
		};
		if(inputChanged or: { masterChanged }) {
			presenceRefreshCount = presenceRefreshCount + 1;
			presenceSerial = presenceSerial + 1;
			if(inputChanged) { inputView !? { inputView.refresh } };
			if(masterChanged) { outputView !? { outputView.refresh } };
		};
		^(inputChanged or: { masterChanged })
	}

	outputRows {
		var main = model.draftAt(\outChans), stems = model.draftAt(\stemOuts);
		var stemsOff = stems.isNil;
		^[
			(label: "main l", value: this.class.channelText(main[0]),
				control: true, dirty: model.dirtyAt(\outChans)),
			(label: "main r", value: this.class.channelText(main[1]),
				control: true, dirty: model.dirtyAt(\outChans)),
			(label: "stems", value: if(stemsOff) { "off" } { "on" },
				control: true, family: \cas, dirty: model.dirtyAt(\stemOuts),
				muted: stemsOff),
			(label: "stem 1", value: if(stemsOff) { "—" } {
				this.class.channelText(stems[0])
			}, control: true, family: \cas, dirty: model.dirtyAt(\stemOuts),
				muted: stemsOff),
			(label: "stem 2", value: if(stemsOff) { "—" } {
				this.class.channelText(stems[1])
			}, control: true, family: \cas, dirty: model.dirtyAt(\stemOuts),
				muted: stemsOff),
			(label: "record 4ch",
				value: if(model.draftAt(\recStems)) { "on" } { "off" },
				control: true, dirty: model.dirtyAt(\recStems))
		]
	}

	restartRows {
		var device = model.draftAt(\device);
		var reported = this.prDeviceReported(device,
			CQAudioConfig.deviceMode(device));
		var validation, family, provenance;
		var status = this.transactionStatus;
		case
		{ confirming } {
			validation =
				"audio stops · current unsaved patch resets · Server.default restarts";
			provenance = "unrelated synths on Server.default also stop";
			family = \warning;
		}
		{ this.prRecorderBusy } {
			validation = "stop REC before restarting audio";
			family = \danger;
		}
		{ this.prServerResponsive.not } {
			validation = "server not responding · restart audio";
			family = \danger;
		}
		// AUD-W. This case is the whole point of the guard: the server answers,
		// the lifecycle says running, and the sound is still not trustworthy.
		// No automatic recovery is offered — only the reachable restart.
		{ this.prAudioClockDanger } {
			validation = "audio clock stuck · restart audio";
			provenance = "server reports % against the requested %".format(
				this.class.clockRateText(this.prAudioClockRate),
				this.class.sampleRateText(model.activeAt(\sampleRate)));
			family = \danger;
		}
		{ status == \restarting or: { status == \preparing } } {
			validation = "restarting audio";
			family = \warning;
		}
		{ status == \rollback } {
			validation = "candidate failed · restoring previous audio";
			provenance = transactionController[\error].value;
			family = \danger;
		}
		{ status == \rollbackReady } {
			validation = "candidate failed · previous audio restored";
			provenance = transactionController[\error].value;
			family = \danger;
		}
		{ status == \failed } {
			validation = "audio restart failed · output silent";
			provenance = localError
				?? { transactionController[\error].value };
			family = \danger;
		}
		{ status == \notSaved } {
			validation = "running · not saved";
			provenance = localError
				?? { transactionController[\error].value };
			family = \warning;
		}
		{ model.valid.not } {
			validation = model.validationError;
			family = \danger;
		}
		{ reported.not } {
			validation = "device not currently reported";
			family = \danger;
		}
		{ model.dirty } {
			validation = "valid draft · restart required";
			family = \warning;
		}
		{ status == \ready } {
			validation = "audio ready";
			family = \input;
		}
		{
			validation = "active config valid";
			family = \input;
		};
		provenance = provenance ?? {
			if(model.profilePresent) {
				"audio profile overrides %".format(model.configPath.basename)
			} {
				"% · no audio profile".format(model.configPath.basename)
			}
		};
		^[
			(label: "validation", value: validation, family: family),
			(label: "provenance", value: provenance, muted: true)
		]
	}

	free {
		recorderCtl !? { recorderCtl.remove };
		recorderCtl = nil;
		gui !? { gui.removeAudioPresenceListener(this) };
		allMenus.do { |menu| menu.free };
		^this
	}

	applySkin { |next|
		var palette;
		skin = next;
		palette = CQInk.palette(skin);
		[view, rail, hostView, inputView, outputView, restartView, ioBand].do {
			|item| item !? { item.background = palette[\recess]; item.refresh }
		};
		[refreshButton, discardButton, applyButton].do { |button|
			button !? { button.skin = skin }
		};
		allMenus.do { |menu| menu.skin = skin };
		^this
	}

	applyScale { |nextStep, width|
		var nextLayout, railW, columns;
		nextStep = nextStep ? step;
		width = width ? pageWidth;
		if(nextStep == step and: { width == pageWidth }) { ^this };
		step = nextStep;
		pageWidth = width;
		metrics = CQGuiScale.metrics(step);
		nextLayout = this.class.layoutFor(step);
		railW = CQGuiScale.railWidth(step, pageWidth);
		columns = CQGuiScale.columns(railW, 2, metrics[\gutter]);
		rail.fixedWidth = railW;
		hostView.fixedSize = Size(railW, nextLayout[\hostHeight]);
		inputView.fixedSize = Size(columns[0], nextLayout[\ioHeight]);
		outputView.fixedSize = Size(columns[1], nextLayout[\ioHeight]);
		ioBand.fixedSize = Size(railW, nextLayout[\ioHeight]);
		ioBand.layout.spacing = metrics[\gutter];
		restartView.fixedSize = Size(railW, nextLayout[\restartHeight]);
		restartButtonSpacer.fixedHeight =
			metrics[\titleHeight] + (2 * metrics[\rowHeight])
				+ metrics[\rowGap];
		topPad.fixedHeight = metrics[\vMargin];
		hostGap.fixedHeight = metrics[\bandGap];
		restartGap.fixedHeight = metrics[\bandGap];
		bottomPad.fixedHeight = metrics[\vMargin];
		this.prScaleButtons;
		this.prScaleMenus(railW, columns);
		[hostView, inputView, outputView, restartView].do { |item| item.refresh };
		^this
	}

	// ---- private ----

	prInit {
		inputPresence = [nil, nil, nil];
		masterPresence = [nil, nil];
		inputPresenceDraw = [nil, nil, nil];
		masterPresenceDraw = [nil, nil];
		presenceGains = [
			CQSpec.default(\liveGain), CQSpec.default(\micGain)
		];
		deviceRefreshAvailable = deviceProvider.notNil;
		if(deviceProvider.isNil) {
			deviceProvider = {
				Cocoquantus.audioDeviceCatalogForGui(
					model.activeAt(\device))
			};
		};
		transactionController = transactionController ?? {
			(
				lifecycle: { Cocoquantus.lifecycleState },
				recorderState: {
					var current = Cocoquantus.recorder;
					if(current.isNil) { \idle } { current.state }
				},
				recorder: { Cocoquantus.recorder },
				status: { Cocoquantus.audioReconfigureState },
				error: { Cocoquantus.audioReconfigureError },
				responsive: { Cocoquantus.serverResponsive },
				clockDanger: { Cocoquantus.audioClockDanger },
				clockRate: { Cocoquantus.audioClockRate },
				apply: { |audioModel|
					Cocoquantus.reconfigureAudio(audioModel)
				},
				restart: { Cocoquantus.restartCurrentAudio },
				retry: { Cocoquantus.retryAudioProfileSave }
			)
		};
		this.prListenRecorder;
		step = \compact;
		metrics = CQGuiScale.metrics(step);
		this.refreshDevices;
		this.prBuild;
		gui !? {
			gui.addAudioPresenceListener(this, { |inputs, master, gains|
				this.presenceFrame(inputs, master, gains)
			})
		};
		^this
	}

	prListenRecorder {
		var provider = transactionController[\recorder];
		var current;
		if(provider.isNil) { ^this };
		current = provider.value;
		current !? {
			recorderCtl = SimpleController(current);
			recorderCtl.put(\state, {
				{
					if(recorderCtl.notNil) {
						this.recorderStateChanged
					}
				}.defer
			});
		};
		^this
	}

	prBuild {
		var palette = CQInk.palette(skin), sizes = this.class.layoutFor(step);
		var railW = CQGuiScale.railWidth(step, pageWidth);
		var columns = CQGuiScale.columns(railW, 2, metrics[\gutter]);
		refreshButton = this.prButton(this.prDeviceControlLabel,
			this.prDeviceControlEnabled);
		discardButton = this.prButton("discard changes", false);
		applyButton = this.prButton("apply & restart audio", false);
		refreshButton.action = { this.prDeviceControlAction };
		discardButton.action = { this.prDiscardOrCancel };
		applyButton.action = { this.prApplyAction };
		this.prBuildMenus;

		hostView = UserView().fixedSize_(Size(railW, sizes[\hostHeight]))
			.background_(palette[\recess]);
		hostView.drawFunc = {
			try {
				this.prDrawSection(hostView, "host / engine", this.hostRows, \line)
			} { |error| CQInkControl.noteDrawError(error) }
		};
		this.prBuildHostLayout;

		inputView = UserView().fixedSize_(Size(columns[0], sizes[\ioHeight]))
			.background_(palette[\recess]);
		inputView.drawFunc = {
			try {
				this.prNotePresencePaint;
				this.prDrawSection(inputView, "into yölahti", this.inputRows, \input)
			} { |error| CQInkControl.noteDrawError(error) }
		};
		inputTitleSpacer = View().fixedHeight_(metrics[\titleHeight]);
		inputMenuRows = inputMenus.collect { |menu| this.prMenuRow(menu.view) };
		inputView.layout = VLayout(
			*([inputTitleSpacer] ++ inputMenuRows ++ [nil])
		).margins_(0).spacing_(0);

		outputView = UserView().fixedSize_(Size(columns[1], sizes[\ioHeight]))
			.background_(palette[\recess]);
		outputView.drawFunc = {
			try {
				this.prNotePresencePaint;
				this.prDrawSection(outputView, "out of yölahti", this.outputRows,
					\line, this.masterPresenceText, this.masterPresenceFamily)
			} { |error| CQInkControl.noteDrawError(error) }
		};
		outputTitleSpacer = View().fixedHeight_(metrics[\titleHeight]);
		outputMenuRows = outputMenus.collect { |menu| this.prMenuRow(menu.view) };
		outputView.layout = VLayout(
			*([outputTitleSpacer] ++ outputMenuRows)
		).margins_(0).spacing_(0);

		ioBand = View().fixedSize_(Size(railW, sizes[\ioHeight]))
			.background_(palette[\recess]);
		ioBand.layout = HLayout(inputView, outputView).margins_(0)
			.spacing_(metrics[\gutter]);

		restartView = UserView().fixedSize_(Size(railW, sizes[\restartHeight]))
			.background_(palette[\recess]);
		restartView.drawFunc = {
			try {
				this.prDrawSection(restartView, "restart gate",
					this.restartRows, \warning)
			} { |error| CQInkControl.noteDrawError(error) }
		};
		restartButtonSpacer = View().fixedHeight_(
			metrics[\titleHeight] + (2 * metrics[\rowHeight])
				+ metrics[\rowGap]);
		restartView.layout = VLayout(
			restartButtonSpacer,
			HLayout(nil, discardButton.view, nil, applyButton.view)
				.margins_(0).spacing_(metrics[\rowGap])
		).margins_(metrics[\contentInset]).spacing_(0);

		hostGap = View().fixedHeight_(metrics[\bandGap]);
		restartGap = View().fixedHeight_(metrics[\bandGap]);
		bottomPad = View().fixedHeight_(metrics[\vMargin]);
		railLayout = VLayout(hostView, hostGap, ioBand, restartGap,
			restartView, bottomPad).margins_(0).spacing_(0);
		rail = View().fixedWidth_(railW).background_(palette[\recess])
			.layout_(railLayout);
		topPad = View().fixedHeight_(metrics[\vMargin]);
		view = View().background_(palette[\recess]).layout_(VLayout(
			topPad,
			HLayout(nil, rail, nil).margins_(0).spacing_(0),
			nil
		).margins_(0).spacing_(0));
		this.prScaleButtons;
		this.prScaleMenus(railW, columns);
		this.prSyncDraft;
		^this
	}

	prButton { |label, enabled|
		var button = CQInkControl.button(label,
			CQGuiScale.buttonWidth(step, label), skin, false, false, \line)
			.surface_(\recess).enabled_(enabled);
		gui !? { gui.registerInkControl(button) };
		^button
	}

	prBuildMenus {
		modeMenu = CQInkMenu(["system default", "single device", "split devices"],
			skin);
		singleDeviceMenu = CQInkMenu([], skin);
		inputDeviceMenu = CQInkMenu([], skin);
		outputDeviceMenu = CQInkMenu([], skin);
		sampleRateMenu = CQInkMenu(["44.1 kHz", "48 kHz", "96 kHz"], skin);
		blockSizeMenu = CQInkMenu(#[16, 32, 64, 128, 256, 512]
			.collect(_.asString), skin);
		hardwareBufferMenu = CQInkMenu(
			["driver default"] ++ #[16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192]
				.collect(_.asString), skin);
		liveLMenu = CQInkMenu(this.prChannelChoices, skin);
		liveRMenu = CQInkMenu(this.prChannelChoices, skin);
		micMenu = CQInkMenu(["off"] ++ this.prChannelChoices, skin);
		mainLMenu = CQInkMenu(this.prChannelChoices, skin);
		mainRMenu = CQInkMenu(this.prChannelChoices, skin);
		stemsMenu = CQInkMenu(["off", "on"], skin);
		stem1Menu = CQInkMenu(this.prChannelChoices, skin);
		stem2Menu = CQInkMenu(this.prChannelChoices, skin);
		recStemsMenu = CQInkMenu(["off", "on"], skin);
		hostMenus = [modeMenu, sampleRateMenu, blockSizeMenu, hardwareBufferMenu];
		inputMenus = [liveLMenu, liveRMenu, micMenu];
		outputMenus = [mainLMenu, mainRMenu, stemsMenu, stem1Menu, stem2Menu,
			recStemsMenu];
		allMenus = [modeMenu, singleDeviceMenu, inputDeviceMenu, outputDeviceMenu,
			sampleRateMenu, blockSizeMenu, hardwareBufferMenu]
			++ inputMenus ++ outputMenus;
		^this
	}

	prBuildHostLayout {
		var primarySlot = View(), outputSlot = View(), blank = View();
		hostFirstSpacer = View().fixedHeight_(
			metrics[\titleHeight] + metrics[\rowHeight]);
		hostTitleSpacer = View().fixedHeight_(metrics[\titleHeight]);
		refreshRightSpacer = View().fixedWidth_(metrics[\contentInset]);
		hostFirstSpacer.layout = VLayout(
			hostTitleSpacer,
			HLayout(nil, refreshButton.view, refreshRightSpacer)
				.margins_(0).spacing_(0)
		).margins_(0).spacing_(0);
		devicePrimaryStack = StackLayout(singleDeviceMenu.view,
			inputDeviceMenu.view).mode_(\stackOne);
		primarySlot.layout = devicePrimaryStack;
		deviceOutputStack = StackLayout(outputDeviceMenu.view, blank)
			.mode_(\stackOne);
		outputSlot.layout = deviceOutputStack;
		hostMenuRows = [
			this.prMenuRow(modeMenu.view),
			this.prMenuRow(primarySlot),
			this.prMenuRow(outputSlot),
			this.prMenuRow(sampleRateMenu.view),
			this.prMenuRow(blockSizeMenu.view),
			this.prMenuRow(hardwareBufferMenu.view)
		];
		hostView.layout = VLayout(
			*([hostFirstSpacer] ++ hostMenuRows)
		).margins_(0).spacing_(0);
		^this
	}

	prInstallDeviceCatalog { |result|
		var normalized = CQAudioDeviceCatalog.normalize(result);
		if(result.isKindOf(Event)) {
			deviceCatalogSource = result[\source] ? \injected;
			deviceError = result[\error];
		} {
			deviceCatalogSource = \injected;
		};
		if(#[scanned, stale, configured, failed].includes(
			deviceCatalogSource)) {
			deviceRefreshAvailable = false;
		};
		fullDeviceSnapshot = normalized[\all];
		fullInputDeviceSnapshot = normalized[\inputs];
		fullOutputDeviceSnapshot = normalized[\outputs];
		this.prApplyDeviceFilter;
		^this
	}

	prApplyDeviceFilter {
		var full = (
			all: fullDeviceSnapshot,
			inputs: fullInputDeviceSnapshot,
			outputs: fullOutputDeviceSnapshot
		);
		var recommended = CQAudioDeviceCatalog.recommended(
			full, model.draftAt(\device));
		var visible = if(showAllDrivers or: {
			#[scanned, stale].includes(deviceCatalogSource).not
		}) {
			CQAudioDeviceCatalog.withConfigured(
				full, model.draftAt(\device))
		} {
			recommended
		};
		advancedDeviceCount =
			(fullDeviceSnapshot.size - recommended[\all].size).max(0);
		deviceSnapshot = visible[\all];
		inputDeviceSnapshot = visible[\inputs];
		outputDeviceSnapshot = visible[\outputs];
		singleDeviceSnapshot =
			CQAudioConfig.bidirectionalDeviceNames(
				inputDeviceSnapshot, outputDeviceSnapshot);
		this.prUpdateDeviceControl;
		^this
	}

	// What the device list is and how to renew it, in one phrase. This is the
	// engine row's business, not the control's: the control can only ever show
	// one of three things, and on a machine with any legacy driver the
	// `show all drivers` toggle wins that slot forever.
	//
	// The second clause was added 2026-07-31, after a correctly installed
	// ReaRoute stayed invisible through several audio restarts: the driver list
	// is enumerated inside the interpreter process (`_ListAudioDevices` ->
	// PortAudio), so restarting audio can renew a *reconnected* device but never
	// a *newly installed* driver. The old note promised both. A note that can
	// lie about how to renew the list is the same defect class as a gate that
	// can lie about passing.
	*deviceListNote { |source, refreshAvailable = false|
		if(refreshAvailable) { ^"refresh devices to rescan" };
		^if(#[scanned, stale].includes(source)) {
			"scanned at boot · restart audio to rescan · new driver: restart sclang"
		} {
			"configured devices only"
		}
	}

	prDeviceControlLabel {
		if(deviceRefreshAvailable) { ^"refresh devices" };
		if(advancedDeviceCount > 0) {
			^if(showAllDrivers) {
				"recommended drivers"
			} {
				"show all drivers"
			}
		};
		^if(#[scanned, stale].includes(deviceCatalogSource)) {
			"scanned at boot"
		} {
			"configured devices"
		}
	}

	prDeviceControlEnabled {
		^deviceRefreshAvailable or: { advancedDeviceCount > 0 }
	}

	prDeviceControlAction {
		if(deviceRefreshAvailable) {
			this.refreshDevices
		} {
			this.toggleDriverView
		};
		^this
	}

	prUpdateDeviceControl {
		refreshButton !? {
			refreshButton.label = this.prDeviceControlLabel;
			refreshButton.enabled = this.prDeviceControlEnabled;
			this.prScaleButtons;
		};
		^this
	}

	prRefreshMenus {
		var device = model.draftAt(\device), mode = CQAudioConfig.deviceMode(device);
		var primaryChoices, inputChoices, outputChoices;
		var inChans = model.draftAt(\inChans);
		var outChans = model.draftAt(\outChans);
		var stems = model.draftAt(\stemOuts);
		if(modeMenu.isNil) { ^this };

		modeMenu.value = #[default, single, split].indexOf(mode);
		modeMenu.action = { |menu|
			this.prSetDeviceMode(#[default, single, split][menu.value])
		};

		primaryChoices = ["system default"] ++ singleDeviceSnapshot;
		if(mode == \single and: {
			primaryChoices.any { |item| item.asString == device.asString }.not
		}) {
			primaryChoices = primaryChoices ++ [device.asString]
		};
		this.prSetDeviceMenu(singleDeviceMenu, primaryChoices,
			if(mode == \single) { device } { nil }, \single);

		inputChoices = ["system default"] ++ inputDeviceSnapshot;
		outputChoices = ["system default"] ++ outputDeviceSnapshot;
		if(mode == \split) {
			if(device[0].notNil and: {
				inputChoices.any { |item| item.asString == device[0].asString }.not
			}) { inputChoices = inputChoices ++ [device[0].asString] };
			if(device[1].notNil and: {
				outputChoices.any { |item| item.asString == device[1].asString }.not
			}) { outputChoices = outputChoices ++ [device[1].asString] };
		};
		this.prSetDeviceMenu(inputDeviceMenu, inputChoices,
			if(mode == \split) { device[0] } { nil }, \input);
		this.prSetDeviceMenu(outputDeviceMenu, outputChoices,
			if(mode == \split) { device[1] } { nil }, \output);
		deviceMenus = if(mode == \split) {
			[inputDeviceMenu, outputDeviceMenu]
		} {
			[singleDeviceMenu]
		};
		devicePrimaryStack.setProperty(\currentIndex,
			if(mode == \split) { 1 } { 0 }, false);
		deviceOutputStack.setProperty(\currentIndex,
			if(mode == \split) { 0 } { 1 }, false);

		this.prSetChoiceMenu(sampleRateMenu, #[44100, 48000, 96000],
			model.draftAt(\sampleRate), { |value|
				model.setDraft(\sampleRate, value)
			});
		this.prSetChoiceMenu(blockSizeMenu, #[16, 32, 64, 128, 256, 512],
			model.draftAt(\blockSize), { |value|
				model.setDraft(\blockSize, value)
			});
		this.prSetChoiceMenu(hardwareBufferMenu,
			[nil, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192],
			model.draftAt(\hardwareBufferSize), { |value|
				model.setDraft(\hardwareBufferSize, value)
			});
		this.prSetChannelMenu(liveLMenu, inChans[0], { |value|
			model.setDraft(\inChans, [value, model.draftAt(\inChans)[1]])
		});
		this.prSetChannelMenu(liveRMenu, inChans[1] ? inChans[0], { |value|
			model.setDraft(\inChans, [model.draftAt(\inChans)[0], value])
		});
		this.prSetChoiceMenu(micMenu, [nil] ++ (0..63),
			model.draftAt(\micChan), { |value| model.setDraft(\micChan, value) });
		this.prSetChannelMenu(mainLMenu, outChans[0], { |value|
			model.setDraft(\outChans, [value, model.draftAt(\outChans)[1]])
		});
		this.prSetChannelMenu(mainRMenu, outChans[1], { |value|
			model.setDraft(\outChans, [model.draftAt(\outChans)[0], value])
		});
		this.prSetChoiceMenu(stemsMenu, [nil, \on],
			if(stems.isNil) { nil } { \on }, { |value|
				model.setDraft(\stemOuts,
					if(value.isNil) { nil } { this.prDefaultStemPair })
			});
		if(stems.isNil) {
			[stem1Menu, stem2Menu].do { |menu|
				menu.items = ["—"];
				menu.value = 0;
				menu.enabled = false;
			};
		} {
			[stem1Menu, stem2Menu].do { |menu|
				menu.items = this.prChannelChoices;
				menu.enabled = true;
			};
			this.prSetChannelMenu(stem1Menu, stems[0], { |value|
				var current = model.draftAt(\stemOuts);
				current !? { model.setDraft(\stemOuts, [value, current[1]]) };
			});
			this.prSetChannelMenu(stem2Menu, stems[1], { |value|
				var current = model.draftAt(\stemOuts);
				current !? { model.setDraft(\stemOuts, [current[0], value]) };
			});
		};
		this.prSetChoiceMenu(recStemsMenu, [false, true],
			model.draftAt(\recStems), { |value|
				model.setDraft(\recStems, value)
			});
		^this
	}

	prSetDeviceMode { |mode|
		var current = model.draftAt(\device), first;
		switch(mode,
			\default, { model.setDraft(\device, nil) },
			\single, {
				first = if(current.isString) { current } {
					singleDeviceSnapshot.first
				};
				if(first.notNil) { model.setDraft(\device, first) };
			},
			\split, {
				model.setDraft(\device, if(current.isString) {
					[
						if(this.prDeviceReported(current, \input)) { current } { nil },
						if(this.prDeviceReported(current, \output)) { current } { nil }
					]
				} {
					if(current.isNil) { [nil, nil] } { current.copy }
				})
			}
		);
		this.prSyncDraft;
		^this
	}

	prSetDeviceMenu { |menu, choices, target, direction|
		var selected;
		menu.items = choices;
		selected = if(target.isNil) { 0 } {
			choices.detectIndex { |item| item.asString == target.asString } ? 0
		};
		menu.value = selected;
		menu.action = { |changed|
			var next = if(changed.value == 0) { nil } {
				choices[changed.value].asString
			};
			var current = model.draftAt(\device);
			switch(direction,
				\single, { model.setDraft(\device, next) },
				\input, {
					model.setDraft(\device,
						[next, if(current.isArray) { current[1] } { nil }])
				},
				\output, {
					model.setDraft(\device,
						[if(current.isArray) { current[0] } { nil }, next])
				}
			);
			this.prSyncDraft;
		};
		^this
	}

	prSetChoiceMenu { |menu, values, target, function|
		var selected = values.detectIndex { |value|
			value.asCompileString == target.asCompileString
		} ? 0;
		menu.value = selected;
		menu.action = { |changed|
			function.value(values[changed.value]);
			this.prSyncDraft;
		};
		^this
	}

	prSetChannelMenu { |menu, target, function|
		menu.value = target.asInteger.clip(0, 63);
		menu.action = { |changed|
			function.value(changed.value);
			this.prSyncDraft;
		};
		^this
	}

	prDiscardOrCancel {
		if(confirming) {
			this.cancelConfirmation
		} {
			model.discard;
			localStatus = \idle;
			localError = nil;
			this.prSyncDraft;
		};
		^this
	}

	prApplyAction {
		if(this.transactionStatus == \notSaved) {
			this.retrySave
		} {
			if(confirming) {
				this.confirmRestart
			} {
				this.requestApply
			}
		};
		^this
	}

	prRecorderBusy {
		^#[starting, recording, stopping].includes(
			transactionController[\recorderState].value ? \idle)
	}

	prServerResponsive {
		var responder = transactionController[\responsive];
		^if(responder.isNil) { true } { responder.value == true }
	}

	// A page with no clock provider (older hermetic doubles) is not in danger:
	// absent telemetry is not the same as bad telemetry (GUI_SPEC §4.5).
	prAudioClockDanger {
		var provider = transactionController[\clockDanger];
		^if(provider.isNil) { false } { provider.value == true }
	}

	prAudioClockRate {
		var provider = transactionController[\clockRate];
		^provider !? { provider.value }
	}

	prCanApply {
		var device = model.draftAt(\device);
		var reported = this.prDeviceReported(device,
			CQAudioConfig.deviceMode(device));
		^model.valid
			and: { reported }
			and: { transactionController[\lifecycle].value == \running }
			and: { this.prRecorderBusy.not }
			and: {
				#[preparing, restarting, rollback].includes(
					this.transactionStatus).not
			}
	}

	prSyncDraft {
		if(modeMenu.notNil) {
			allMenus.do { |menu| menu.enabled = true };
			this.prRefreshMenus;
			if(confirming or: {
				#[preparing, restarting, rollback].includes(
					this.transactionStatus)
			}) {
				allMenus.do { |menu| menu.enabled = false };
				refreshButton.enabled = false;
			} {
				this.prUpdateDeviceControl;
			};
			if(confirming) {
				discardButton.label = "cancel";
				discardButton.enabled = true;
				applyButton.label = "restart";
				applyButton.enabled = true;
			} {
				discardButton.label = if(model.dirty) {
					"discard changes"
				} {
					"no changes"
				};
				discardButton.enabled = model.dirty;
				if(this.transactionStatus == \notSaved) {
					applyButton.label = "retry save";
					applyButton.enabled = true;
				} {
					applyButton.label = if(model.dirty) {
						"apply & restart audio"
					} {
						"restart audio"
					};
					applyButton.enabled = this.prCanApply;
				};
			};
			this.prScaleButtons;
			[hostView, inputView, outputView, restartView].do { |surface|
				surface !? { surface.refresh }
			};
		};
		^this
	}

	prMenuRow { |controlView|
		var row = View().fixedHeight_(metrics[\rowHeight]);
		row.layout = HLayout(
			View().fixedWidth_(100),
			controlView,
			nil
		).margins_(0).spacing_(0);
		^row
	}

	prScaleMenus { |railW, columns|
		var hostLabel, hostValue, inputLabel, inputValue, outputLabel, outputValue;
		if(hostMenuRows.isNil) { ^this };
		hostFirstSpacer.fixedHeight =
			metrics[\titleHeight] + metrics[\rowHeight];
		hostTitleSpacer.fixedHeight = metrics[\titleHeight];
		refreshRightSpacer.fixedWidth = metrics[\contentInset];
		inputTitleSpacer.fixedHeight = metrics[\titleHeight];
		outputTitleSpacer.fixedHeight = metrics[\titleHeight];
		hostLabel = (railW * 0.3).clip(88, 170);
		hostValue = (railW - (2 * metrics[\contentInset])
			- hostLabel - metrics[\rowGap]).max(120);
		inputLabel = (columns[0] * 0.3).clip(88, 170);
		inputValue = (columns[0] - (2 * metrics[\contentInset])
			- inputLabel - metrics[\rowGap]).max(80);
		outputLabel = (columns[1] * 0.3).clip(88, 170);
		outputValue = (columns[1] - (2 * metrics[\contentInset])
			- outputLabel - metrics[\rowGap]).max(80);
		this.prScaleMenuRows(hostMenuRows, hostLabel, hostValue);
		this.prScaleMenuRows(inputMenuRows, inputLabel, inputValue);
		this.prScaleMenuRows(outputMenuRows, outputLabel, outputValue);
		[modeMenu, singleDeviceMenu, inputDeviceMenu, outputDeviceMenu,
			sampleRateMenu, blockSizeMenu, hardwareBufferMenu].do { |menu|
			menu.font = CQInk.font(\number, metrics[\mono]);
			menu.fixedSize = Size(hostValue, metrics[\boxHeight]);
		};
		inputMenus.do { |menu|
			menu.font = CQInk.font(\number, metrics[\mono]);
			menu.fixedSize = Size(inputValue, metrics[\boxHeight]);
		};
		outputMenus.do { |menu|
			menu.font = CQInk.font(\number, metrics[\mono]);
			menu.fixedSize = Size(outputValue, metrics[\boxHeight]);
		};
		^this
	}

	prScaleMenuRows { |rows, labelWidth, valueWidth|
		rows.do { |row|
			row.fixedHeight = metrics[\rowHeight];
			row.children.first.fixedWidth =
				metrics[\contentInset] + labelWidth + metrics[\rowGap];
		};
		^this
	}

	prScaleButtons {
		[refreshButton, discardButton, applyButton].do { |button|
			button.scale = metrics;
			button.resizeTo(CQGuiScale.buttonWidth(step, button.label),
				metrics[\boxHeight]);
		};
		^this
	}

	prPresenceValues { |values, size|
		values = values ? [];
		^Array.fill(size, { |index|
			values[index] !? { |value| value.asFloat.abs }
		})
	}

	prPresenceWord { |value|
		if(value.isNil) { ^"—" };
		^if(value >= this.class.presenceThreshold) { "signal" } { "quiet" }
	}

	prPresenceRow { |label, value, level, gain, configured|
		var muted = configured.not or: { (gain ? 0) <= 0 };
		var word = if(configured) { this.prPresenceWord(value) } { "off" };
		^(
			label: label,
			value: if(configured) {
				"% · %".format(word, if(muted) { "muted" } { "live" })
			} {
				"off"
			},
			family: if(configured and: {
				value.notNil and: { value >= this.class.presenceThreshold }
			}) { \input } { \line },
			presence: true,
			level: if(configured) { level } { nil },
			muted: muted
		)
	}

	// Both I/O UserViews can paint in one Qt refresh batch. Count the batch once
	// by serial rather than pretending its two child callbacks are two frames.
	prNotePresencePaint {
		if(presenceSerial > paintedPresenceSerial) {
			paintedPresenceSerial = presenceSerial;
			presencePaintCount = presencePaintCount + 1;
		};
		^this
	}

	prDeviceRow { |label, value, reported|
		var text = value;
		if(value.isNil) { text = "system default" };
		^(
			label: label,
			value: text.asString
				++ if(reported) { "" } { " · device not currently reported" },
			family: if(reported) { \line } { \danger },
			control: true,
			dirty: model.dirtyAt(\device)
		)
	}

	prDeviceReported { |device, direction|
		^switch(direction,
			\input, { CQAudioConfig.deviceReported(device, inputDeviceSnapshot) },
			\output, { CQAudioConfig.deviceReported(device, outputDeviceSnapshot) },
			\single, {
				CQAudioConfig.deviceReported(device, singleDeviceSnapshot)
			},
			\default, { true },
			\split, {
				CQAudioConfig.deviceReported(device[0], inputDeviceSnapshot)
					and: {
						CQAudioConfig.deviceReported(device[1], outputDeviceSnapshot)
					}
			},
			{ CQAudioConfig.deviceReported(device, deviceSnapshot) }
		)
	}

	prDefaultStemPair {
		var main = model.draftAt(\outChans);
		^(0..63).reject { |channel| main.includes(channel) }.copyRange(0, 1)
	}

	prChannelChoices {
		^Array.fill(64, { |index| this.class.channelText(index) })
	}

	prDrawSection { |surface, title, rows, titleFamily = \line,
		titleRight, titleRightFamily = \whisper|
		var palette = CQInk.palette(skin), rect = surface.bounds.moveTo(0, 0);
		var left = metrics[\contentInset], right = rect.width - metrics[\contentInset];
		var titleY = metrics[\titleHeight] - 3;
		var labelWidth = (rect.width * 0.3).clip(88, 170);
		Pen.stringLeftJustIn(title,
			Rect(left, 2, right - left, metrics[\titleHeight]),
			CQInk.font(\label, metrics[\title]), palette[titleFamily]);
		titleRight !? {
			Pen.stringRightJustIn(titleRight.asString,
				Rect(left, 2, right - left, metrics[\titleHeight]),
				CQInk.font(\number, metrics[\mono]), palette[titleRightFamily]);
		};
		Pen.strokeColor = palette[\whisper];
		Pen.width = CQInk.strokes[\hairline];
		Pen.line(Point(left, titleY), Point(right, titleY));
		Pen.stroke;
		rows.do { |row, index|
			var y, valueLeft, valueWidth, color, alpha, text;
			if(row.notNil) {
				y = metrics[\titleHeight] + (index * metrics[\rowHeight]);
				valueLeft = left + labelWidth + metrics[\rowGap];
				valueWidth = (right - valueLeft).max(20);
				alpha = if(row[\muted] == true) { 0.45 } { 1.0 };
				color = palette[row[\family] ? \line].copy.alpha_(alpha);
				Pen.stringLeftJustIn(row[\label].asString,
					Rect(left, y, labelWidth, metrics[\rowHeight]),
					CQInk.font(\label, metrics[\label]),
					if(row[\control] == true and: {
						row[\family] == \danger
					}) {
						palette[\danger].copy.alpha_(alpha)
					} {
						palette[\whisper].copy.alpha_(alpha)
					});
				if(row[\dirty] == true) {
					Pen.strokeColor = palette[\warning].copy.alpha_(0.92);
					Pen.width = CQInk.strokes[\normal];
					Pen.line(Point(valueLeft - 3, y + 5),
						Point(valueLeft - 3, y + metrics[\rowHeight] - 5));
					Pen.stroke;
				};
				if(row[\control] != true) {
					text = this.class.ellipsize(row[\value].asString,
						valueWidth, metrics[\mono]);
					Pen.stringLeftJustIn(text,
						Rect(valueLeft, y, valueWidth, metrics[\rowHeight]),
						CQInk.font(\number, metrics[\mono]), color);
					Pen.strokeColor = palette[\whisper].copy.alpha_(
						if(row[\presence] == true) { 0.42 } { 0.24 });
					Pen.width = CQInk.strokes[\hairline];
					Pen.line(Point(valueLeft, y + metrics[\rowHeight] - 3),
						Point(right, y + metrics[\rowHeight] - 3));
					Pen.stroke;
					if(row[\presence] == true and: { row[\level].notNil }) {
						Pen.fillColor = color.copy.alpha_(alpha * 0.78);
						Pen.fillRect(Rect(valueLeft,
							y + metrics[\rowHeight] - 5,
							valueWidth * row[\level].clip(0, 1), 3));
					};
				};
			};
		};
	}
}
