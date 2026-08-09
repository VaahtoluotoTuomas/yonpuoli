// The one-click application has a reproducible class library and never runs
// unrelated code from the player's startup.scd.
+ Platform {
	startupFiles { ^[] }
}
