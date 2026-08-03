// CQAudioBootRecovery — startup device preflight plus one-shot recovery for a
// stale generated Audio profile. It owns data and persistence only;
// Cocoquantus owns server lifecycle.
//
// A Windows ASIO driver can remain enumerable and even let scsynth open after
// its hardware has been unplugged. The only honest second gate is the server's
// actual sample rate. If either the stopped-server catalog rejects the saved
// device or the pre-graph clock gate rejects the resulting server, startup may
// try the hand-maintained base config exactly once. If a fresh scan rejects all
// configured routes, startup stops before scsynth. Only a fully ready fallback
// is promoted; the rejected profile becomes the ordinary .bak.

CQAudioBootRecovery {
	var <base, <candidate, <profilePath, <store;
	var <state = \candidate, <error, <candidateError, <fallbackError;
	var <attemptCount = 1, <baseAvailability, <candidateAvailability;
	var <alternative = false;

	*new { |base, candidate, profilePath, store, forceFallback = false|
		^super.new.prInit(base, candidate, profilePath, store, forceFallback)
	}

	*audioDiffers { |a, b|
		^CQAudioConfig.allowedKeys.any { |key|
			a[key].asCompileString != b[key].asCompileString
		}
	}

	// nil means the stopped-server catalog was unavailable or stale: absence of
	// evidence must not reject a configured route. false is reserved for a fresh
	// directional scan that positively did not report the endpoint.
	*deviceAvailability { |config, catalog, catalogState|
		var device = config[\device], mode, inputs, outputs, has;
		if(device.isNil) { ^true };
		if(catalogState != \scanned or: { catalog.isNil }) { ^nil };
		inputs = catalog[\inputs] ? [];
		outputs = catalog[\outputs] ? [];
		has = { |items, name|
			name.isNil or: {
				items.any { |item| item.asString == name.asString }
			}
		};
		mode = CQAudioConfig.deviceMode(device);
		^switch(mode,
			\default, { true },
			\single, {
				has.value(inputs, device) and: { has.value(outputs, device) }
			},
			\split, {
				has.value(inputs, device[0]) and: {
					has.value(outputs, device[1])
				}
			},
			{ false }
		)
	}

	*errorText { |failure|
		^failure.tryPerform(\errorString) ? failure.asString
	}

	prInit { |baseConfig, candidateConfig, path, injectedStore, forceFallback|
		base = CQConfig.validate(baseConfig).copy;
		candidate = CQConfig.validate(candidateConfig).copy;
		profilePath = path.asString;
		store = injectedStore ? CQAudioProfileStore(profilePath);
		alternative = (forceFallback == true)
			or: { this.class.audioDiffers(base, candidate) };
		^this
	}

	hasAlternative { ^alternative }
	fallbackActive { ^state == \rollback }
	preflightFailed { ^state == \failed and: { attemptCount == 0 } }

	selectedConfig {
		^if(this.fallbackActive) { base.copy } { candidate.copy }
	}

	// Called only after the stopped-server device capture. A saved route is
	// skipped when its absence is proven and the base route is not also known
	// absent. A driver which lies about availability still reaches the clock gate.
	selectInitial { |catalog, catalogState|
		candidateAvailability = this.class.deviceAvailability(
			candidate, catalog, catalogState);
		baseAvailability = this.class.deviceAvailability(
			base, catalog, catalogState);
		if((state == \candidate) and: { candidateAvailability == false }) {
			if(this.hasAlternative and: { baseAvailability != false }) {
				this.beginFallback(
					"saved audio device is not currently reported: %"
						.format(candidate[\device].asCompileString), false);
			} {
				this.prRejectUnavailable;
			};
		};
		^this.selectedConfig
	}

	// Server boot and the actual-rate gate are the only startup failures a
	// different audio route may repair. Graph/SynthDef failures remain failures.
	canFallback { |stage|
		^state == \candidate
			and: { this.hasAlternative }
			and: { #[serverBoot, audioClock].includes(stage) }
			and: { baseAvailability != false }
	}

	beginFallback { |failure, candidateWasAttempted = true|
		if(state == \rollback) { ^base.copy };
		candidateError = this.class.errorText(failure);
		error = candidateError;
		state = \rollback;
		attemptCount = if(candidateWasAttempted) { 2 } { 1 };
		^base.copy
	}

	// The base config becomes the new generated profile only after its complete
	// session reached ready. CQAudioProfileStore's atomic promotion preserves the
	// rejected profile as .bak.
	complete {
		if(state != \rollback) { ^this.result };
		if(this.prSave) {
			state = \rollbackReady;
			error = candidateError;
		} {
			state = \notSaved;
		};
		^this.result
	}

	retrySave {
		if(state != \notSaved) { ^false };
		if(this.prSave) {
			state = \rollbackReady;
			error = candidateError;
			^true
		};
		^false
	}

	fallbackFailed { |failure|
		fallbackError = this.class.errorText(failure);
		error = "saved profile: % · base fallback: %".format(
			candidateError, fallbackError);
		state = \failed;
		^this.result
	}

	result {
		^(
			status: state,
			error: error,
			candidateError: candidateError,
			rollbackError: fallbackError,
			attemptCount: attemptCount,
			startupFallback: true,
			baseDevice: base[\device],
			candidateDevice: candidate[\device]
		)
	}

	prRejectUnavailable {
		attemptCount = 0;
		candidateError = if(this.hasAlternative) {
			"saved audio device is not currently reported: %"
				.format(candidate[\device].asCompileString)
		} {
			"configured audio device is not currently reported: %"
				.format(candidate[\device].asCompileString)
		};
		fallbackError = if(this.hasAlternative) {
			"base audio device is also not currently reported: %"
				.format(base[\device].asCompileString)
		} { nil };
		error = if(fallbackError.notNil) {
			"saved profile: % · base fallback: %".format(
				candidateError, fallbackError)
		} {
			candidateError
		};
		state = \failed;
		^this
	}

	prSave {
		var profile = CQAudioConfig.profileFor(base);
		try {
			if(store.hasPending.not) { store.prepare(base, profile) };
			store.promote;
			fallbackError = nil;
			^true
		} { |failure|
			fallbackError = this.class.errorText(failure);
			error = "% · base audio is running but its profile was not saved: %"
				.format(candidateError, fallbackError);
			^false
		}
	}
}
