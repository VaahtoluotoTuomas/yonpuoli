// AIO-3 — hermetic candidate/rollback coordinator tests. Restart callbacks
// are explicit events; no server, Qt, real config or user profile is touched.

CQAudioReconfigureTest : UnitTest {
	var basePath, profilePath;

	setUp {
		CQConfig.postWarnings = false;
		basePath = this.class.filenameSymbol.asString.dirname.dirname
			+/+ "tmp" +/+ "aio-reconfigure-base.scd";
		profilePath = CQAudioConfig.profilePathFor(basePath);
	}

	tearDown { CQConfig.postWarnings = true }

	prFixture { |promoteFailure = false|
		var files = IdentityDictionary.new;
		var callbacks = List.new, attempts = List.new;
		var lifecycle = \running, recorderState = \idle;
		var failPromote = promoteFailure;
		var adapter = (
			exists: { |path| files.includesKey(path.asString) },
			write: { |path, text| files[path.asString] = text },
			read: { |path| files[path.asString].interpret },
			remove: { |path| files.removeAt(path.asString) },
			promote: { |pending, profile, backup|
				if(failPromote) { Error("injected rename failure").throw };
				if(files.includesKey(profile.asString)) {
					files[backup.asString] = files[profile.asString]
				};
				files[profile.asString] = files[pending.asString];
				files.removeAt(pending.asString);
			}
		);
		var base = CQConfig.validate((
			headless: true,
			sixteenN: (enabled: false),
			outChans: [0, 1]
		));
		var model = CQAudioConfig(base, basePath);
		var store = CQAudioProfileStore(profilePath, adapter);
		var runner = (
			lifecycle: { lifecycle },
			recorderState: { recorderState },
			restart: { |config, onReady, onFailure|
				attempts.add(config.copy);
				callbacks.add((ready: onReady, failure: onFailure));
			}
		);
		var transaction = CQAudioReconfigure(model, base, store, runner);
		^(
			files: files,
			callbacks: callbacks,
			attempts: attempts,
			model: model,
			store: store,
			transaction: transaction,
			setLifecycle: { |value| lifecycle = value },
			setRecorder: { |value| recorderState = value },
			allowPromote: { failPromote = false }
		)
	}

	test_successCommitsOnlyAfterCandidateReady {
		var f = this.prFixture;
		f[\model].setDraft(\sampleRate, 96000);
		this.assert(f[\transaction].start,
			"a valid dirty running draft starts the transaction");
		this.assertEquals(f[\transaction].state, \restarting,
			"validated pending waits for the candidate callback");
		this.assertEquals(f[\attempts].size, 1,
			"candidate is the first and only initial restart");
		this.assert(f[\store].hasPending,
			"candidate profile remains pending while restart is unresolved");
		this.assertEquals(f[\model].activeAt(\sampleRate), 48000,
			"active cannot move before the full candidate reports ready");

		f[\callbacks][0][\ready].value;
		this.assertEquals(f[\transaction].state, \ready,
			"candidate success publishes audio ready");
		this.assertEquals(f[\model].activeAt(\sampleRate), 96000,
			"the exact captured candidate becomes active");
		this.assert(f[\model].dirty.not and: { f[\model].profilePresent },
			"accepted candidate is clean and records generated provenance");
		this.assert(f[\store].hasProfile and: { f[\store].hasPending.not },
			"profile promotion happens only after ready");
	}

	test_candidateFailureRunsOneRollbackAndPreservesDraft {
		var f = this.prFixture;
		var oldProfile = "(sampleRate: 48000)";
		f[\files][profilePath] = oldProfile;
		f[\model].profilePresent = true;
		f[\model].setDraft(\blockSize, 128);
		f[\transaction].start;
		f[\callbacks][0][\failure].value(Error("candidate device missing"));
		this.assertEquals(f[\transaction].state, \rollback,
			"candidate failure enters one explicit rollback");
		this.assertEquals(f[\attempts].size, 2,
			"candidate plus rollback is exactly two restart attempts");
		this.assert(f[\store].hasPending.not,
			"rejected candidate pending is discarded before rollback");
		this.assertEquals(f[\files][profilePath], oldProfile,
			"candidate failure never replaces the last-working profile");

		f[\callbacks][1][\ready].value;
		this.assertEquals(f[\transaction].state, \rollbackReady,
			"successful rollback reports previous audio restored");
		this.assert(f[\model].dirty
			and: { f[\model].draftAt(\blockSize) == 128 }
			and: { f[\model].activeAt(\blockSize) == 64 },
			"rejected draft survives over the restored active config");
	}

	test_rollbackFailureStopsAfterTwoAttempts {
		var f = this.prFixture;
		f[\model].setDraft(\sampleRate, 44100);
		f[\transaction].start;
		f[\callbacks][0][\failure].value(Error("candidate failed"));
		f[\callbacks][1][\failure].value(Error("rollback failed"));
		this.assertEquals(f[\transaction].state, \failed,
			"rollback failure leaves an explicit failed state");
		this.assertEquals(f[\transaction].attemptCount, 2,
			"failure cannot enter a third retry loop");
		this.assert(f[\transaction].error.find("candidate failed").notNil
			and: { f[\transaction].error.find("rollback failed").notNil },
			"terminal diagnostics retain both failures");
	}

	test_staleCandidateCallbackCannotPublishOrPromote {
		var f = this.prFixture;
		f[\model].setDraft(\sampleRate, 96000);
		f[\transaction].start;
		f[\transaction].cancel;
		f[\callbacks][0][\ready].value;
		this.assertEquals(f[\transaction].state, \idle,
			"cancel invalidates the older callback generation");
		this.assertEquals(f[\model].activeAt(\sampleRate), 48000,
			"stale ready cannot publish active state");
		this.assert(f[\store].hasProfile.not and: { f[\store].hasPending.not },
			"stale ready cannot publish a profile");
	}

	test_runningNotSavedCanRetryWithoutAnotherRestart {
		var f = this.prFixture(true);
		f[\model].setDraft(\blockSize, 128);
		f[\transaction].start;
		f[\callbacks][0][\ready].value;
		this.assertEquals(f[\transaction].state, \notSaved,
			"rename failure leaves the successful candidate running");
		this.assertEquals(f[\model].activeAt(\blockSize), 128,
			"persistence failure does not roll back working audio");
		this.assert(f[\store].hasPending,
			"validated pending remains retryable");
		f[\allowPromote].value;
		this.assert(f[\transaction].retrySave,
			"retry promotes without rebooting audio again");
		this.assertEquals(f[\transaction].state, \ready,
			"successful retry returns to audio ready");
		this.assertEquals(f[\attempts].size, 1,
			"save retry adds no server restart");
	}

	test_guardsAreInertBeforePendingOrRestart {
		var f = this.prFixture;
		f[\model].setDraft(\sampleRate, 96000);
		f[\setRecorder].value(\recording);
		this.assert(f[\transaction].start.not,
			"an active recorder blocks restart");
		this.assertEquals(f[\transaction].error,
			"stop REC before restarting audio",
			"recorder gate is exact and user-facing");
		this.assertEquals(f[\attempts].size, 0,
			"blocked recorder causes no restart");
		this.assert(f[\store].hasPending.not,
			"blocked recorder causes no pending write");
		f[\setRecorder].value(\idle);
		f[\setLifecycle].value(\failed);
		this.assert(f[\transaction].start.not,
			"non-running lifecycle is inert");
		this.assertEquals(f[\attempts].size, 0,
			"non-running lifecycle still causes no restart");
	}
}
