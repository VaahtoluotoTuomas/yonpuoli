// CQAudioReconfigure — serverless coordinator for the AIO-3 transaction.
//
// The facade supplies a restart driver; this class owns the strict sequence:
// validate/write pending -> candidate restart -> promote on success, or one
// rollback restart on failure. It holds data and callbacks only, never Qt.

CQAudioReconfigure {
	var <model, <base, <store, <runner, onChange;
	var <state = \idle, <error, <candidateError, <rollbackError;
	var <attemptCount = 0, <generation = 0;
	var previousConfig, candidateConfig, candidateProfile;

	*new { |model, base, store, runner, onChange|
		^super.newCopyArgs(model, base, store, runner, onChange)
	}

	isActive { ^#[preparing, restarting, rollback].includes(state) }

	result {
		^(
			status: state,
			error: error,
			candidateError: candidateError,
			rollbackError: rollbackError,
			attemptCount: attemptCount
		)
	}

	start { |allowClean = false|
		var token, recorderState;
		if(this.isActive) {
			^this.prReject(\blocked, "audio restart already in progress")
		};
		if(runner[\lifecycle].value != \running) {
			^this.prReject(\blocked, "audio restart requires a running session")
		};
		recorderState = runner[\recorderState].value ? \idle;
		if(#[starting, recording, stopping].includes(recorderState)) {
			^this.prReject(\blocked, "stop REC before restarting audio")
		};
		if(model.dirty.not and: { allowClean != true }) {
			^this.prReject(\blocked, "audio draft has no changes")
		};
		if(model.valid.not) {
			^this.prReject(\invalid, model.validationError)
		};

		generation = generation + 1;
		token = generation;
		attemptCount = 0;
		error = nil;
		candidateError = nil;
		rollbackError = nil;
		previousConfig = model.active.copy;
		candidateProfile = model.draftProfile.copy;
		this.prSetState(\preparing);
		try {
			candidateConfig = store.prepare(base, candidateProfile);
		} { |failure|
			error = this.class.errorText(failure);
			this.prSetState(\failed);
			^false
		};
		if(token != generation) { ^false };
		this.prSetState(\restarting);
		this.prRestart(token, candidateConfig,
			{ this.prCandidateReady(token) },
			{ |failure| this.prCandidateFailed(token, failure) });
		^true
	}

	cancel {
		generation = generation + 1;
		if(store.hasPending) {
			try { store.discardPending } { |failure|
				error = this.class.errorText(failure)
			};
		};
		this.prSetState(\idle);
		^this
	}

	retrySave {
		if(state != \notSaved) { ^false };
		try {
			store.promote;
			model.profilePresent = true;
			error = nil;
			this.prSetState(\ready);
		} { |failure|
			error = this.class.errorText(failure);
			this.prSetState(\notSaved);
			^false
		};
		^true
	}

	// ---- private ----

	prCandidateReady { |token|
		if((token != generation) or: { state != \restarting }) { ^this };
		model.acceptCandidate(candidateProfile, candidateConfig);
		try {
			store.promote;
			model.profilePresent = true;
			error = nil;
			this.prSetState(\ready);
		} { |failure|
			error = this.class.errorText(failure);
			this.prSetState(\notSaved);
		};
		^this
	}

	prCandidateFailed { |token, failure|
		if((token != generation) or: { state != \restarting }) { ^this };
		candidateError = this.class.errorText(failure);
		error = candidateError;
		try { store.discardPending } { |discardFailure|
			error = error ++ " · pending cleanup: "
				++ this.class.errorText(discardFailure)
		};
		this.prSetState(\rollback);
		this.prRestart(token, previousConfig,
			{ this.prRollbackReady(token) },
			{ |rollbackFailure|
				this.prRollbackFailed(token, rollbackFailure)
			});
		^this
	}

	prRollbackReady { |token|
		if((token != generation) or: { state != \rollback }) { ^this };
		error = candidateError;
		this.prSetState(\rollbackReady);
		^this
	}

	prRollbackFailed { |token, failure|
		if((token != generation) or: { state != \rollback }) { ^this };
		rollbackError = this.class.errorText(failure);
		error = "candidate: % · rollback: %".format(
			candidateError, rollbackError);
		this.prSetState(\failed);
		^this
	}

	prRestart { |token, config, onReady, onFailure|
		attemptCount = attemptCount + 1;
		try {
			runner[\restart].value(config.copy,
				{
					if(token == generation) { onReady.value }
				},
				{ |failure|
					if(token == generation) { onFailure.value(failure) }
				});
		} { |failure|
			if(token == generation) { onFailure.value(failure) };
		};
		^this
	}

	prReject { |next, reason|
		error = reason;
		this.prSetState(next);
		^false
	}

	prSetState { |next|
		state = next;
		onChange.value(this.result);
		^this
	}

	*errorText { |failure|
		^failure.tryPerform(\errorString) ? failure.asString
	}
}
