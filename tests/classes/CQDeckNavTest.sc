// CQDeckNavTest — folder scan + resolve logic (SPEC §5.1 navigation), server-free:
// prScanDir/prResolveDir/prEnsureFiles operate on plain paths, so a CQDeck with
// graph = nil exercises them fully. Regressions guarded: extension filtering must
// match by value (Collection:includes compares identity — the table holds symbols),
// and loadFolder("samples") must resolve the samples folder itself from any cwd
// (KÄYTTÖ 5 gate finding, 2026-07-18).

CQDeckNavTest : UnitTest {
	var dir, deck;

	setUp {
		CQDeck.postWarnings = false;
		dir = Cocoquantus.repoDir +/+ "tests" +/+ "tmp" +/+ "cqdeck_test";
		this.prClean;
		File.mkdir(dir);
		["b.wav", "a.WAV", "c.aiff", "d.flac", "notes.txt", "noext"].do { |n|
			File.use(dir +/+ n, "w", { |f| f.write("x") });
		};
		deck = CQDeck(nil, nil, \deckA, dir);
	}

	tearDown {
		this.prClean;
		CQDeck.postWarnings = true;
	}

	prClean {
		var sub = dir +/+ "sub";
		(sub +/+ "*").pathMatch.do { |f| File.delete(f) };
		File.delete(sub);
		(dir +/+ "*").pathMatch.do { |f| File.delete(f) };
		File.delete(dir);
	}

	test_extensionTableMatchesByIdentity {
		this.assert(CQDeck.audioExtensions.includes(\wav),
			"extension table holds symbols — includes() matches by identity");
	}

	test_scanFiltersAndSorts {
		deck.prScanDir(dir);
		this.assertEquals(deck.files.size, 4,
			"wav/WAV/aiff/flac scanned in, txt and extensionless out");
		this.assertEquals(deck.files, deck.files.copy.sort, "scan result is sorted");
	}

	test_scanCaseInsensitive {
		deck.prScanDir(dir);
		this.assert(deck.files.any { |f| f.basename == "a.WAV" },
			"upper-case extension accepted");
	}

	test_ensureFilesFallsBackToSamplesDir {
		this.assert(deck.files.isNil, "no scan before first navigation");
		deck.prEnsureFiles;
		this.assertEquals(deck.files.size, 4,
			"first navigation scans the samples dir");
	}

	test_emptyFolderWarnsNotCrashes {
		var empty = dir +/+ "sub";
		var d2;
		File.mkdir(empty);
		d2 = CQDeck(nil, nil, \deckB, empty);
		d2.prEnsureFiles;
		this.assert(d2.files.notNil and: { d2.files.isEmpty },
			"empty folder scans to an empty list, no error");
	}

	test_resolveDirSubfolder {
		var sub = dir +/+ "sub";
		File.mkdir(sub);
		this.assertEquals(deck.prResolveDir("sub"), sub,
			"a subfolder of the samples dir resolves");
	}

	test_resolveDirSamplesItself {
		// samplesDir here is .../tests/tmp/cqdeck_test — its own basename must
		// resolve via the sibling rule, exactly like loadFolder("samples")
		this.assertEquals(
			deck.prResolveDir("cqdeck_test").tr($\\, $/),
			dir.tr($\\, $/),
			"the samples folder's own name resolves from any cwd");
	}

	test_resolveDirUnknownIsNil {
		this.assert(deck.prResolveDir("no_such_folder_xyz").isNil,
			"unknown folder resolves to nil (with a warning)");
	}

	test_scanAfterResolvedLoadPath {
		// prScanFolder is what load() runs: mixed-separator path in, siblings out
		var p = deck.prResolve("b.wav");
		this.assert(p.notNil, "bare name resolves against the samples dir");
		deck.prScanFolder(p);
		this.assertEquals(deck.files.size, 4, "loaded file's folder scanned");
		this.assertEquals(deck.files[deck.fileIndex].basename, "b.wav",
			"fileIndex points at the loaded file");
	}
}
