// CQRecorderTest — the recorder's file naming with no server (A2): timestamp
// format and the never-overwrite guarantee (safety #7). The actual DiskOut
// path is exercised by ear in KUUNTELU 6 (REC -> file appears, has audio).

CQRecorderTest : UnitTest {
	var tmpDir;

	setUp {
		tmpDir = this.class.filenameSymbol.asString.dirname.dirname
			+/+ "tmp" +/+ "cqrec_test";
		if(File.exists(tmpDir).not) { File.mkdir(tmpDir) };
		(tmpDir +/+ "*").pathMatch.do { |p| File.delete(p) };
	}

	tearDown {
		(tmpDir +/+ "*").pathMatch.do { |p| File.delete(p) };
	}

	test_timestampFormat {
		// Leave dayOfWeek/rawSeconds unresolved: SC 3.14 treats an explicit
		// rawSeconds of 0 as authoritative and correctly resolves it to 1970.
		var d = Date.new(2026, 7, 18, 14, 30, 5);
		var path = CQRecorder.nextFreePath(tmpDir, d);
		this.assertEquals(path.basename, "cq_260718_143005.wav",
			"filename is cq_YYMMDD_HHMMSS.wav (SPEC §5.7)");
		this.assertEquals(path.dirname.withoutTrailingSlash,
			tmpDir.withoutTrailingSlash, "file lands in the recordings dir");
	}

	test_timestampZeroPadding {
		var d = Date.new(2026, 1, 2, 3, 4, 5);
		this.assertEquals(CQRecorder.nextFreePath(tmpDir, d).basename,
			"cq_260102_030405.wav", "single-digit fields are zero-padded");
	}

	test_neverOverwrites {
		var d = Date.new(2026, 7, 18, 14, 30, 5);
		var first = CQRecorder.nextFreePath(tmpDir, d);
		File.use(first, "w", { |f| f.write("x") });
		this.assertEquals(CQRecorder.nextFreePath(tmpDir, d).basename,
			"cq_260718_143005_2.wav",
			"same-second collision gets a _2 suffix, never overwrites (safety #7)");
		File.use(tmpDir +/+ "cq_260718_143005_2.wav", "w", { |f| f.write("x") });
		this.assertEquals(CQRecorder.nextFreePath(tmpDir, d).basename,
			"cq_260718_143005_3.wav", "suffix keeps counting past _2");
	}
}
