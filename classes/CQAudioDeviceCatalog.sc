// CQAudioDeviceCatalog — AIO-5's pure snapshot/filter boundary.
//
// The default provider is the only production path to ServerOptions device
// enumeration. Cocoquantus calls it only while the local server is confirmed
// stopped, then passes the normalized Event to the running GUI as memory data.

CQAudioDeviceCatalog {

	*legacyWindowsPrefixes {
		^#["MME : ", "Windows DirectSound : ", "Windows WDM-KS : "]
	}

	*scan { |provider|
		var result;
		provider = provider ?? {
			{
				var inputs = ServerOptions.inDevices;
				var outputs = ServerOptions.outDevices;
				(
					all: (inputs ? []) ++ (outputs ? []),
					inputs: inputs,
					outputs: outputs
				)
			}
		};
		result = provider.value;
		^this.normalize(result)
	}

	*normalize { |result|
		var all, inputs, outputs;
		if(result.isKindOf(Event)) {
			inputs = CQAudioConfig.uniqueDeviceNames(
				result[\inputs] ? result[\all] ? []);
			outputs = CQAudioConfig.uniqueDeviceNames(
				result[\outputs] ? result[\all] ? []);
			all = CQAudioConfig.uniqueDeviceNames(
				(result[\all] ? []) ++ inputs ++ outputs);
		} {
			all = CQAudioConfig.uniqueDeviceNames(result ? []);
			inputs = all.copy;
			outputs = all.copy;
		};
		^(all: all, inputs: inputs, outputs: outputs)
	}

	*configured { |device|
		var mode = CQAudioConfig.deviceMode(device);
		var inputs = [], outputs = [], all;
		switch(mode,
			\single, {
				inputs = [device.asString];
				outputs = [device.asString];
			},
			\split, {
				device[0] !? { |name| inputs = [name.asString] };
				device[1] !? { |name| outputs = [name.asString] };
			}
		);
		all = CQAudioConfig.uniqueDeviceNames(inputs ++ outputs);
		^(
			all: all,
			inputs: CQAudioConfig.uniqueDeviceNames(inputs),
			outputs: CQAudioConfig.uniqueDeviceNames(outputs)
		)
	}

	*withConfigured { |catalog, device|
		var normalized = this.normalize(catalog);
		var exact = this.configured(device);
		^(
			all: CQAudioConfig.uniqueDeviceNames(
				normalized[\all] ++ exact[\all]),
			inputs: CQAudioConfig.uniqueDeviceNames(
				normalized[\inputs] ++ exact[\inputs]),
			outputs: CQAudioConfig.uniqueDeviceNames(
				normalized[\outputs] ++ exact[\outputs])
		)
	}

	*recommended { |catalog, device|
		var full = this.withConfigured(catalog, device);
		var exact = this.configured(device);
		^(
			all: this.recommendedNames(full[\all], exact[\all]),
			inputs: this.recommendedNames(full[\inputs], exact[\inputs]),
			outputs: this.recommendedNames(full[\outputs], exact[\outputs])
		)
	}

	*recommendedNames { |names, exactNames|
		var exact = CQAudioConfig.uniqueDeviceNames(exactNames ? []);
		^CQAudioConfig.uniqueDeviceNames(names ? []).select { |name|
			this.isLegacyWindowsDriver(name).not or: {
				exact.any { |item| item.asString == name.asString }
			}
		}
	}

	*isLegacyWindowsDriver { |name|
		var text = name.asString;
		^this.legacyWindowsPrefixes.any { |prefix| text.beginsWith(prefix) }
	}
}
