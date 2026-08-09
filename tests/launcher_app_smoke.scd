// Parse the production application entry point without booting audio. The
// branch still compiles the complete file and proves its validation marker.

(
"YONPUOLI_APP_VALIDATE_ONLY".setenv("1");
thisProcess.interpreter.executeFile(
	Cocoquantus.repoDir +/+ "support" +/+ "yonpuoli_app.scd"
);
)
