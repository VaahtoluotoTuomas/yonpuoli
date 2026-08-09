// The disposable device probe must never execute the player's startup.scd.
+ WindowsPlatform {
	startupFiles { ^[] }
}
