// CQDeck subclass whose reads complete only when the test releases them.

CQTestDeck : CQDeck {
	var <readRequests;

	*new { |graph, state, which = \deckA, samplesDir|
		^super.new(graph, state, which, samplesDir).prTestInit
	}

	prTestInit {
		readRequests = List.new;
		^this
	}

	prReadBuffer { |filePath, channel|
		var request = (
			path: filePath,
			channel: channel,
			gate: Condition.new,
			buffer: CQTestBuffer(graph.server.nextBufferNumber),
			failure: nil
		);
		readRequests.add(request);
		request[\gate].hang;
		request[\failure] !? { |failure| failure.throw };
		^request[\buffer]
	}

	completeRead { |index|
		readRequests[index][\gate].unhang;
	}

	failRead { |index, message = "injected deck read failure"|
		readRequests[index][\failure] = Error(message);
		this.completeRead(index);
	}
}
