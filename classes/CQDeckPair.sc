// CQDeckPair — two-phase linked-stereo coordinator (SPEC §5.1). Each deck owns
// its pending buffer; the pair publishes neither until both are ready, then sends
// both synth starts in one Server.bind bundle for a shared timetag.

CQDeckPair {
	var deckA, deckB, server, generation = 0, activeTransaction;
	var <loadState = \idle, <error;

	*new { |deckA, deckB, server|
		^super.newCopyArgs(deckA, deckB, server)
	}

	load { |filePath, onDone|
		var transaction, maybeCommit, fail;
		this.prCancelActive;
		generation = generation + 1;
		error = nil;
		loadState = \loading;
		transaction = (
			generation: generation,
			attemptA: nil, attemptB: nil,
			readyA: nil, readyB: nil
		);
		activeTransaction = transaction;
		fail = { |failure|
			if(activeTransaction === transaction) {
				this.prCancel(transaction);
				activeTransaction = nil;
				error = failure.errorString;
				loadState = \error;
				this.changed(\state, loadState, error);
			};
		};
		maybeCommit = {
			if(transaction[\readyA].notNil and: {
				transaction[\readyB].notNil
			}) {
				if((activeTransaction === transaction)
					and: { transaction[\generation] == generation }
					and: { deckA.canCommit(transaction[\readyA]) }
					and: { deckB.canCommit(transaction[\readyB]) }) {
					server.bind {
						deckA.commitPrepared(transaction[\readyA], 0);
						deckB.commitPrepared(transaction[\readyB], 1);
					};
					activeTransaction = nil;
					loadState = \idle;
					this.changed(\state, loadState, nil);
					onDone.value(this);
				} {
					fail.value(Error("CQDeckPair: linked load was superseded"));
				};
			};
		};
		transaction[\attemptA] = deckA.prepare(filePath, 0, { |attempt|
			transaction[\readyA] = attempt;
			maybeCommit.value;
		}, fail);
		if(transaction[\attemptA].isNil) { ^this };
		transaction[\attemptB] = deckB.prepare(filePath, 1, { |attempt|
			transaction[\readyB] = attempt;
			maybeCommit.value;
		}, fail);
		if(transaction[\attemptB].isNil) {
			fail.value(Error("CQDeckPair: linked load could not start"));
		};
		^this
	}

	free {
		generation = generation + 1;
		this.prCancelActive;
		loadState = \idle;
		error = nil;
	}

	prCancelActive {
		activeTransaction !? { |transaction| this.prCancel(transaction) };
		activeTransaction = nil;
	}

	prCancel { |transaction|
		deckA.cancelPrepared(transaction[\attemptA]);
		deckB.cancelPrepared(transaction[\attemptB]);
	}
}
