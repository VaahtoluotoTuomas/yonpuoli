// AIO-5 — pure stopped-server device snapshot and reversible driver filtering.

CQAudioDeviceCatalogTest : UnitTest {

	test_scanReadsProviderOnceAndNormalizesDirections {
		var reads = 0;
		var catalog = CQAudioDeviceCatalog.scan({
			reads = reads + 1;
			(
				all: ["Windows WASAPI : Shared", "Windows WASAPI : Shared"],
				inputs: ["Windows WASAPI : Mic", "ASIO : Duplex", "ASIO : Duplex"],
				outputs: ["Windows WASAPI : Phones", "ASIO : Duplex"]
			)
		});
		this.assertEquals(reads, 1,
			"one stopped-server capture invokes its provider exactly once");
		this.assertEquals(catalog[\inputs],
			["Windows WASAPI : Mic", "ASIO : Duplex"],
			"input capability is exact-deduplicated without reordering");
		this.assertEquals(catalog[\outputs],
			["Windows WASAPI : Phones", "ASIO : Duplex"],
			"output capability remains directional");
		this.assertEquals(
			CQAudioConfig.bidirectionalDeviceNames(
				catalog[\inputs], catalog[\outputs]),
			["ASIO : Duplex"],
			"single-device choices remain the exact directional intersection");
	}

	test_recommendedHidesOnlyExactLegacyPrefixes {
		var catalog = (
			inputs: [
				"MME : Mapper Input",
				"Windows DirectSound : Capture",
				"Windows WDM-KS : Raw Mic",
				"Windows WASAPI : Mic",
				"ASIO : Studio",
				"Virtual MME Bridge"
			],
			outputs: [
				"MME : Mapper Output",
				"Windows DirectSound : Speakers",
				"Windows WDM-KS : Raw Phones",
				"Windows WASAPI : Phones",
				"ASIO : Studio"
			]
		);
		var view = CQAudioDeviceCatalog.recommended(catalog,
			["Windows WDM-KS : Raw Mic", "Windows DirectSound : Speakers"]);
		var has = { |items, text|
			items.any { |item| item.asString == text.asString }
		};
		this.assert(has.value(view[\inputs], "MME : Mapper Input").not
			and: { has.value(view[\outputs], "MME : Mapper Output").not },
			"unselected MME entries are hidden from the recommended view");
		this.assert(has.value(
			view[\inputs], "Windows DirectSound : Capture").not
			and: { has.value(
				view[\outputs], "Windows WDM-KS : Raw Phones").not },
			"unselected DirectSound/WDM-KS entries are hidden");
		this.assert(has.value(view[\inputs], "Windows WASAPI : Mic")
			and: { has.value(view[\outputs], "Windows WASAPI : Phones") }
			and: { has.value(view[\inputs], "ASIO : Studio") },
			"WASAPI and ASIO remain recommended");
		this.assert(has.value(view[\inputs], "Virtual MME Bridge"),
			"fuzzy device text is never mistaken for a complete legacy prefix");
		this.assert(has.value(view[\inputs], "Windows WDM-KS : Raw Mic")
			and: { has.value(
				view[\outputs], "Windows DirectSound : Speakers") },
			"exact active/draft endpoints survive filtering even when advanced");
	}

	test_configuredFallbackPreservesModesAndCopiesData {
		var split = CQAudioDeviceCatalog.configured(["input", "output"]);
		var single = CQAudioDeviceCatalog.configured("duplex");
		var defaults = CQAudioDeviceCatalog.configured(nil);
		this.assertEquals(split[\inputs], ["input"],
			"split fallback keeps the configured input exact");
		this.assertEquals(split[\outputs], ["output"],
			"split fallback keeps the configured output exact");
		this.assertEquals(single[\inputs], ["duplex"],
			"single fallback serves both directions");
		this.assert(defaults[\all].isEmpty,
			"system default needs no invented device name");
	}

	// A reconnected K-Mix could not be found on 2026-07-30: the list is a
	// stopped-server snapshot, and nothing on the page said so. The `scanned at
	// boot` wording existed but lived on the device control's label, which loses
	// that slot to `show all drivers` on any machine that has a legacy driver.
	test_deviceListNoteAlwaysSaysHowToRenewTheList {
		this.assertEquals(
			CQGuiAudioSetup.deviceListNote(\scanned, true),
			"refresh devices to rescan",
			"a safe rescan is offered as the action when one is possible");
		this.assertEquals(
			CQGuiAudioSetup.deviceListNote(\scanned, false),
			"scanned at boot · restart audio to rescan",
			"a running server's list names its age AND the way to renew it");
		this.assertEquals(
			CQGuiAudioSetup.deviceListNote(\stale, false),
			"scanned at boot · restart audio to rescan",
			"a stale cache is still a boot scan, not a configured fallback");
		this.assertEquals(
			CQGuiAudioSetup.deviceListNote(\configured, false),
			"configured devices only",
			"without a snapshot the note admits the list is not a scan");
		#[scanned, stale, configured, failed, injected].do { |source|
			this.assert(
				CQGuiAudioSetup.deviceListNote(source, false).isEmpty.not,
				"every catalog source has a note (%)".format(source));
		};
	}
}
