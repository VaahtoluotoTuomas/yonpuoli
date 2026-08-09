// AIO-2 — pending roundtrip and atomic generated-overlay persistence.
// Every real file is an explicit tests/tmp sibling; config.scd is never touched.

CQAudioProfileStoreTest : UnitTest {
	var repoRoot, tmpDir, basePath, profilePath;

	setUp {
		CQConfig.postWarnings = false;
		repoRoot = this.class.filenameSymbol.asString.dirname.dirname.dirname;
		tmpDir = repoRoot +/+ "tests" +/+ "tmp";
		basePath = tmpDir +/+ "aio-store-base.scd";
		profilePath = tmpDir +/+ "aio-store-base.audio.scd";
		[
			basePath, profilePath, profilePath ++ ".pending",
			profilePath ++ ".bak"
		].do { |path| if(File.exists(path)) { File.delete(path) } };
	}

	tearDown {
		[
			basePath, profilePath, profilePath ++ ".pending",
			profilePath ++ ".bak"
		].do { |path| if(File.exists(path)) { File.delete(path) } };
		CQConfig.postWarnings = true;
	}

	test_prepareRoundtripsWithoutTouchingBaseOrLastWorking {
		var baseText = "(\n    sampleRate: 44100,\n    outChans: [0, 1]\n)\n";
		var base = (sampleRate: 44100, outChans: [0, 1]);
		var model, store, candidate;
		File.use(basePath, "w", { |file| file.write(baseText) });
		File.use(profilePath, "w", { |file| file.write("(sampleRate: 44100)") });
		model = CQAudioConfig(base, basePath, true);
		model.setDraft(\sampleRate, 96000);
		store = CQAudioProfileStore(profilePath);
		candidate = store.prepare(model.active, model.draftProfile);
		this.assertEquals(candidate[\sampleRate], 96000,
			"the reloaded pending Event is the validated candidate");
		this.assertEquals(File.readAllString(basePath), baseText,
			"the hand-maintained base stays byte-for-byte unchanged");
		this.assert(File.exists(profilePath ++ ".pending"),
			"a valid candidate remains pending for the later restart");
		this.assertEquals(profilePath.load[\sampleRate], 44100,
			"prepare never replaces the last-working profile");
		this.assertEquals(store.state, \prepared,
			"the store exposes the prepared lifecycle state");
	}

	test_atomicPromotionKeepsExactlyOneBackup {
		var model = CQAudioConfig((sampleRate: 44100, outChans: [0, 1]),
			basePath);
		var store = CQAudioProfileStore(profilePath);
		model.setDraft(\sampleRate, 48000);
		store.prepare(model.active, model.draftProfile);
		store.promote;
		this.assertEquals(profilePath.load[\sampleRate], 48000,
			"first promotion installs the generated profile");
		this.assert(File.exists(profilePath ++ ".pending").not,
			"same-directory rename consumes pending");
		model.setDraft(\sampleRate, 96000);
		store.prepare(model.active, model.draftProfile);
		store.promote;
		this.assertEquals(profilePath.load[\sampleRate], 96000,
			"second promotion atomically installs the next profile");
		this.assertEquals((profilePath ++ ".bak").load[\sampleRate], 48000,
			"one backup is the immediately previous last-working profile");
	}

	test_roundtripFailureDeletesPendingAndLeavesModelUntouched {
		var files = IdentityDictionary.new;
		var adapter = (
			exists: { |path| files.includesKey(path.asString) },
			write: { |path, text| files[path.asString] = text },
			read: { |path| (rogueKey: 1) },
			remove: { |path| files.removeAt(path.asString) },
			promote: { Error("must not promote").throw }
		);
		var model = CQAudioConfig((outChans: [0, 1]), basePath);
		var store = CQAudioProfileStore(profilePath, adapter);
		model.setDraft(\sampleRate, 96000);
		this.assertException({
			store.prepare(model.active, model.draftProfile)
		}, Error, "a malformed readback is rejected");
		this.assertEquals(store.state, \failed,
			"roundtrip failure is explicit");
		this.assert(store.hasPending.not,
			"failed validation removes only the generated pending file");
		this.assertEquals(model.activeAt(\sampleRate), 48000,
			"persistence failure cannot change active");
	}

	test_promoteFailureIsRetryableRunningNotSavedState {
		var files = IdentityDictionary.new;
		var adapter = (
			exists: { |path| files.includesKey(path.asString) },
			write: { |path, text| files[path.asString] = text },
			read: { |path| files[path.asString].interpret },
			remove: { |path| files.removeAt(path.asString) },
			promote: { Error("injected rename failure").throw }
		);
		var model = CQAudioConfig((outChans: [0, 1]), basePath);
		var store = CQAudioProfileStore(profilePath, adapter);
		model.setDraft(\blockSize, 128);
		store.prepare(model.active, model.draftProfile);
		this.assertException({ store.promote }, Error,
			"an atomic-rename failure is surfaced");
		this.assertEquals(store.state, \notSaved,
			"AIO-3 can render running · not saved and retry");
		this.assert(store.hasPending,
			"validated pending survives for an explicit save retry");
		this.assert(store.hasProfile.not,
			"failed rename never invents a committed last working profile");
	}
}
