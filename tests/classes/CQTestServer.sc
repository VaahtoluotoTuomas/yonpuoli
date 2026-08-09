// Hermetic Server protocol double. It records OSC commands and holds sync barriers
// until a test releases them, so async races are driven by events rather than sleep.

CQTestServer {
	var <name = \cqTest, <addr, <sampleRate = 48000, actualSampleRateOverride;
	var actualSampleRateReadings, actualSampleRateReadIndex = 0;
	var <bufferAllocator, <options;
	var <messages, <syncGates, <bootCallbacks, <rebootCount = 0, <bindCount = 0;
	var <quitCount = 0, <bootCount = 0, <>unresponsive = false;
	var <>processRunning = true, <>quitCallbackBeforeProcessExit = false;
	var <>failSyncAt, <syncCallCount = 0;
	var nextNode = 1000, releasedSyncs = 0;

	*new { ^super.new.prInit }

	prInit {
		addr = NetAddr("127.0.0.1", 59999);
		bufferAllocator = CQTestAllocator();
		options = ServerOptions.new;
		messages = List.new;
		syncGates = List.new;
		bootCallbacks = List.new;
	}

	nextBufferNumber { |n = 1| ^bufferAllocator.alloc(n) }

	nextNodeID {
		var id = nextNode;
		nextNode = nextNode + 1;
		^id
	}

	actualSampleRate {
		var value;
		if(actualSampleRateReadings.notNil) {
			value = actualSampleRateReadings[
				actualSampleRateReadIndex.min(actualSampleRateReadings.size - 1)
			];
			actualSampleRateReadIndex = actualSampleRateReadIndex + 1;
			^value
		};
		^actualSampleRateOverride ? (options.sampleRate ? sampleRate)
	}

	actualSampleRate_ { |value| actualSampleRateOverride = value }

	actualSampleRateReadings_ { |values|
		actualSampleRateReadings = values.asArray;
		actualSampleRateReadIndex = 0;
	}

	cqAudioClockProbePeriod { ^0 }

	listSendMsg { |msg| messages.add(msg.asArray) }

	sendMsg { |... msg| messages.add(msg.asArray) }

	sync {
		var gate = Condition.new;
		syncCallCount = syncCallCount + 1;
		if(failSyncAt == syncCallCount) {
			Error("CQTestServer: injected sync failure %".format(syncCallCount)).throw;
		};
		syncGates.add(gate);
		gate.hang;
	}

	pendingSyncCount { ^syncGates.size - releasedSyncs }

	releasePendingSyncs {
		var limit = syncGates.size;
		while { releasedSyncs < limit } {
			syncGates[releasedSyncs].unhang;
			releasedSyncs = releasedSyncs + 1;
		};
	}

	waitForBoot { |onComplete, limit = 100, onFailure|
		bootCallbacks.add(onComplete);
	}

	runNextBoot {
		var callback;
		if(bootCallbacks.isEmpty) { ^this };
		callback = bootCallbacks.removeAt(0);
		callback.value;
	}

	runServerBoot { ServerBoot.run(this); ^this }

	reboot { |func, onFailure|
		rebootCount = rebootCount + 1;
		processRunning = false;
		func.value;
		processRunning = true;
	}

	quit { |onComplete, onFailure, watchShutDown = true|
		quitCount = quitCount + 1;
		unresponsive = false;
		if(quitCallbackBeforeProcessExit) {
			onComplete.value;
		} {
			processRunning = false;
			onComplete.value;
		};
	}

	completeProcessExit {
		processRunning = false;
		^this
	}

	boot { |register = true, onComplete, onFailure|
		bootCount = bootCount + 1;
		processRunning = true;
		onComplete.value;
	}

	bind { |func|
		bindCount = bindCount + 1;
		func.value;
	}

	serverRunning { ^processRunning }
	applicationRunning { ^processRunning }
	hasBooted { ^true }
}
