// One-click application lifecycle decisions are pure so an Audio restart can
// never be mistaken for the user closing the program.

CQStandaloneAppTest : UnitTest {
	test_privateServerPortIsValidatedBeforeBoot {
		this.assertEquals(CQStandaloneApp.serverPortFrom("59995"), 59995,
			"the launcher's private server port crosses as plain exact data");
		[nil, "", "not-a-port", "80", "70000"].do { |value|
			this.assertException({ CQStandaloneApp.serverPortFrom(value) }, Error,
				"an absent or unsafe standalone server port fails before boot")
		}
	}

	test_runningReplacementGenerationIsAdopted {
		this.assertEquals(
			CQStandaloneApp.pollAction(\running, 8, 7, true),
			\adoptGui,
			"an Audio restart adopts its replacement GUI before inspecting the old close"
		);
	}

	test_sameGenerationWindowCloseExitsCleanly {
		this.assertEquals(
			CQStandaloneApp.pollAction(\running, 8, 8, true),
			\exitClean,
			"closing the application window exits its owned process"
		);
	}

	test_transitionalLifecycleWaitsAndFailureExits {
		#[booting, stopping, stopped].do { |state|
			this.assertEquals(
				CQStandaloneApp.pollAction(state, 8, 7, true),
				\wait,
				"% is transitional rather than a false user close".format(state)
			);
		};
		this.assertEquals(
			CQStandaloneApp.pollAction(\failed, 8, 8, false),
			\exitFailed,
			"a failed owned session exits with failure"
		);
	}
}
