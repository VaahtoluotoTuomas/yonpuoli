// CQAudioClockWatch — AUD-W:n puhdas malli (PLAN.md "AUD-W:n sopimus").
//
// Ei serveriä, ei GUI:ta, ei omaa kelloa: tälle syötetään lukemia ja se vastaa
// tilalla. Sen ainoa tehtävä on erottaa **jumissa oleva** audiokello
// **suppenevasta transientista** — se ero on koko portin vaikea osa.

CQAudioClockWatch {

	var <requestedRate, <state = \ok, <lastRate, <lastRatio;
	var <healthyRun = 0, streak;

	// Yksi epäterveyden määritelmä koko soittimelle: boot-portti
	// (`Cocoquantus.prAudioClockHealthy`) delegoi tänne, joten ajonaikainen
	// guard ei voi erkaantua siitä portista joka päästää session sisään.
	*ratioBounds { ^#[0.8, 1.2] }

	// Mitattu 2026-07-30: Modern Standbystä palaava istunto kulkee **molempien**
	// rajojen ulkopuolella ennen asettumistaan — 7419 Hz (ratio 0,15) → 69 184 Hz
	// (1,44) → 47 986 Hz noin 90 sekunnissa. Rajan ulkopuolinen lukema ei siis
	// ole vika. Vian ehto on jumi: epäterve koko ikkunan ajan **eikä** suppene
	// kohti pyydettyä taajuutta.
	//
	// Ikkuna on tarkoituksella pitkä. Suppenemistarkistus yksin riittäisi
	// mitatulle sarjalle, mutta se lukee vain ikkunan päät: jos toipuva striimi
	// viipyy tasanteella, lyhyt ikkuna julistaisi vian ehjästä koneesta. Väärä
	// hälytys on tässä pahempi kuin myöhäinen totuus, koska guardin koko
	// tarkoitus on lakata valehtelemasta. Kuollut kello kestää siis noin
	// minuutin ennen kuin se sanotaan ääneen.
	*dangerWindow { ^60 }
	*minDangerReadings { ^8 }
	*settleReadings { ^3 }
	*convergenceFactor { ^0.9 }
	*watchPeriod { ^2.5 }

	*ratio { |requestedRate, actualRate|
		if(requestedRate.isNumber.not or: { requestedRate <= 0 }) { ^nil };
		if(actualRate.isNumber.not or: { actualRate <= 0 }) { ^nil };
		^(actualRate.asFloat / requestedRate.asFloat)
	}

	*healthy { |requestedRate, actualRate|
		var ratio = this.ratio(requestedRate, actualRate);
		var bounds = this.ratioBounds;
		if(ratio.isNil) { ^false };
		^(ratio >= bounds[0]) and: { ratio <= bounds[1] }
	}

	// Ei lukemaa on yhtä väärin kuin pysähtynyt kello, ei "vähän väärin".
	*errorOf { |requestedRate, actualRate|
		var ratio = this.ratio(requestedRate, actualRate);
		if(ratio.isNil) { ^1.0 };
		^(ratio - 1).abs
	}

	*new { |requestedRate|
		^super.new.prInit(requestedRate)
	}

	prInit { |rate|
		requestedRate = rate;
		streak = List.new;
		^this
	}

	// Lukemien väli tulee kutsujalta, ei tämän luokan kellosta: sama sarja
	// testataan sekunneissa ilman että testi odottaa oikeaa aikaa.
	//
	// `responding` on SOAK-1:n löydös B (mitattu 2026-07-31). `actualSampleRate`
	// päivittyy vain `/status`-vastauksista, joten vastausten loputtua se jäätyy
	// viimeiseen terveeseen lukemaansa — ja suhdetesti sitä vastaan ei voi enää
	// koskaan epäonnistua. Mitattu tapaus: palvelin ei vastannut yhteenkään
	// neljästä koetuksesta ja sen ääniketju oli kuollut, mutta guard sanoi `ok`
	// ja 48 003,47 Hz. **Välimuistista lukevan vartijan on vaadittava tuoreutta
	// — hiljaisuus ei ole terveys.** Kun lukemaa ei ole, ei ole myöskään mitään
	// mitä väittää: tila on `\stale` ja viimeinen lukema unohdetaan, jottei
	// mikään pinta piirrä sitä mitattuna.
	push { |rate, time, responding = true|
		var err, span, converging;
		if(responding == false) {
			state = \stale;
			healthyRun = 0;
			lastRate = nil;
			lastRatio = nil;
			// Evidenssin katkos katkaisee myös "jumissa 60 s" -väitteen: sitä ei
			// voi jatkaa sarjasta jonka keskeltä puuttuu kaikki tieto.
			streak = List.new;
			^state
		};
		// Ilman pyydettyä taajuutta ei ole mitään mihin verrata. Silloin oikea
		// vastaus on vaieta, ei julistaa vikaa (headless-sessio ilman configia).
		if(requestedRate.isNumber.not or: { requestedRate <= 0 }) {
			state = \ok;
			^state
		};
		lastRate = rate;
		lastRatio = this.class.ratio(requestedRate, rate);
		if(this.class.healthy(requestedRate, rate)) {
			healthyRun = healthyRun + 1;
			streak = List.new;
			if(state == \danger) {
				// Danger ei purkaudu yhdestä hyvästä lukemasta — sama
				// settle-logiikka kuin boot-portin prRequireSettledAudioClockissa.
				if(healthyRun >= this.class.settleReadings) { state = \ok };
			} {
				state = \ok;
			};
			^state
		};
		healthyRun = 0;
		err = this.class.errorOf(requestedRate, rate);
		streak.add([time, err]);
		if(state == \danger) { ^state };
		span = streak.last[0] - streak.first[0];
		converging = (streak.size >= 2) and: {
			streak.last[1] < (streak.first[1] * this.class.convergenceFactor)
		};
		state = if((span >= this.class.dangerWindow)
			and: { streak.size >= this.class.minDangerReadings }
			and: { converging.not }) { \danger } { \watching };
		^state
	}

	// Kuinka kauan nykyinen epäterve jakso on kestänyt (0 kun kello on terve).
	streakSeconds {
		if(streak.size < 2) { ^0 };
		^(streak.last[0] - streak.first[0])
	}

	streakCount { ^streak.size }

	reset {
		state = \ok;
		healthyRun = 0;
		lastRate = nil;
		lastRatio = nil;
		streak = List.new;
		^this
	}
}
