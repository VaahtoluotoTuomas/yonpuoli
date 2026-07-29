// GUI_SPEC §16.1–16.11: one palette/Ink kit, deterministic session-seeded
// asymmetry, fixed stroke hierarchy and vector-glyph inventory.

CQInkTest : UnitTest {
	var inkClass;

	setUp { inkClass = \CQInk.asClass }

	prRequireInk {
		if(inkClass.isNil) {
			this.assert(false, "G1.1: CQInk is the one shared vector drawing kit");
			^false
		};
		^true
	}

	prRgb { |color|
		^[color.red, color.green, color.blue].collect { |value| (value * 255).round.asInteger }
	}

	test_normativeGroundsAndSignals {
		var day, night;
		if(this.prRequireInk.not) { ^this };
		day = inkClass.palette(\day);
		night = inkClass.palette(\night);
		this.assertEquals(this.prRgb(day[\ground]), [244, 237, 224],
			"Day ground is paper #F4EDE0");
		this.assertEquals(this.prRgb(night[\ground]), [18, 20, 28],
			"Night ground is sky #12141C");
		this.assertEquals(this.prRgb(day[\danger]), [196, 61, 43],
			"Day danger is #C43D2B");
		this.assertEquals(this.prRgb(night[\danger]), [255, 107, 84],
			"Night danger is #FF6B54");
		this.assertEquals(this.prRgb(day[\whisper]), [98, 89, 77],
			"Day whisper is the reviewed higher-contrast #62594D");
		this.assertEquals(this.prRgb(night[\whisper]), [122, 128, 140],
			"Night whisper is the reviewed higher-contrast #7A808C");
	}

	test_paletteInventoryIsCompleteInBothSkins {
		var required;
		if(this.prRequireInk.not) { ^this };
		required = #[ground, recess, line, whisper, spark, tri, cas, fm, input,
			spaf, organ, pre, verso, inverso, danger, warning, lamp];
		#[day, night].do { |skin|
			required.do { |name|
				this.assert(inkClass.palette(skin)[name].notNil,
					"% palette defines %".format(skin, name));
			};
		};
	}

	test_seededWobbleIsPixelStable {
		var points, first, second, other;
		if(this.prRequireInk.not) { ^this };
		points = [Point(0, 0), Point(20, 10), Point(40, 0)];
		first = inkClass.wobble(points, 1234, 1.5);
		second = inkClass.wobble(points, 1234, 1.5);
		other = inkClass.wobble(points, 4321, 1.5);
		this.assertEquals(first, second, "same session seed redraws identical pixels");
		this.assert(first != other, "different session seed changes the organic line");
		this.assertEquals(points, [Point(0, 0), Point(20, 10), Point(40, 0)],
			"wobble never mutates caller geometry");
	}

	test_strokesAndGlyphsAreCentralized {
		if(this.prRequireInk.not) { ^this };
		this.assertEquals(inkClass.strokes, (hairline: 1.0, normal: 1.5,
			strong: 2.0, state: 3.0), "fixed 1/1.5/2/3 px hierarchy");
		#[wedge, flip, spark, wave, tickRing, pendulum, hold, panic, rec,
			drawer, collapse, expand, day, night].do { |name|
			this.assert(inkClass.glyphNames.includes(name),
				"8x8 Pen glyph inventory includes %".format(name));
		};
	}

	test_visualMetricsComeFromOneNotebookGrammar {
		var metrics;
		if(this.prRequireInk.not) { ^this };
		metrics = inkClass.metrics;
		this.assertEquals(metrics[\grid], 8, "Day layout uses the 8 px spacing grid");
		this.assertEquals(metrics[\labelSize], 10, "labels are reviewed 10 px whispers");
		this.assertEquals(metrics[\numberSize], 10, "numeric values are 10 px mono");
		this.assertEquals(metrics[\noticeSize], 11, "notice and spellbook text is 11 px");
		this.assertEquals(metrics[\rangeSize], 9, "range letters are 9 px");
		this.assertEquals(metrics[\knobDiameter], 26, "standard knob body is 26 px");
		this.assertEquals(metrics[\featuredKnobDiameter], 34,
			"featured instrument-shaping knob body is 34 px");
		this.assertEquals(metrics[\primaryKnobDiameter], 44,
			"primary macro knob body is 44 px");
		this.assertEquals(#[standard, featured, primary].collect { |role|
			CQInk.knobDiameter(role) },
			[26, 34, 44], "semantic knob hierarchy resolves through the Ink kit");
		this.assertEquals(metrics[\buttonRadius], 3, "button corner radius is 3 px");
		this.assertEquals(metrics[\socketSpacing], 24,
			"socket centers never approach closer than 24 px");
	}

	test_cableGeometryFallsIntoThePaperField {
		var points;
		if(inkClass.respondsTo(\cablePoints).not) {
			this.assert(false, "Ink kit owns gravity-sag cable geometry");
			^this
		};
		points = inkClass.cablePoints(Point(0, 0), Point(120, 0), 12);
		this.assertEquals(points.first, Point(0, 0), "cable begins at its source jack");
		this.assertEquals(points.last, Point(120, 0), "cable ends at its destination socket");
		this.assert(points.collect(_.y).maxItem > 18,
			"gravity sag carries long cables into the open paper field");
	}

	test_feedbackCrescentRegionsSpeakInThreeSemanticColours {
		var edges = CQSpec.mapBreakpoints(\coco1Fb).collect { |point| point[0] };
		this.assertEquals(CQInkNight.feedbackCrescentFamilies,
			#[input, warning, danger],
			"feedback reads as echo green, bloom amber, mayhem red");
		this.assertEquals(CQInkNight.feedbackCrescentFamilyAt(0.2, edges), \input,
			"the fading echo country is green");
		this.assertEquals(CQInkNight.feedbackCrescentFamilyAt(0.74, edges), \warning,
			"unity and the broad riding band are amber");
		this.assertEquals(CQInkNight.feedbackCrescentFamilyAt(0.95, edges), \danger,
			"the compressed mayhem tail is red");
	}

	test_adjacentCrescentScalesKeepTheirOwnSemantics {
		this.assertEquals(CQInkNight.spafCrescentFamilies,
			#[inverso, verso],
			"SP.AF reads as two signed directions, not a warning ramp");
		this.assertEquals(CQInkNight.inputCrescentFamilies,
			#[input, warning],
			"input reads as normal gain followed by overdrive");
		this.assertEquals(CQInkNight.levelCrescentFamilies, #[line, danger],
			"bird level stays neutral through unity and warns only above it");
		this.assertEquals(CQInkNight.levelCrescentFamilyAt(0.5, 0.5), \line,
			"exact unity is still the normal listening level");
		this.assertEquals(CQInkNight.levelCrescentFamilyAt(0.5001, 0.5), \danger,
			"the travelled level past unity turns danger-red");
	}

	test_stormIrisCarriesFourPoeticButExactWeatherSeasons {
		var norms = CQInkNight.irisWeatherNorms;
		var radii = CQInkNight.irisWeatherRadii;
		this.assertEquals(norms, #[0.25, 0.5, 0.75, 1.0],
			"the four unlabelled growth rings still land on exact hand quarters");
		this.assertEquals(radii, [28.0, 36.0, 44.0, 52.0],
			"the weather seasons grow evenly inside the eye's own layer");
		norms.do { |norm, i|
			this.assertFloatEquals(CQInkNight.irisWeatherRadiusAt(norm), radii[i],
				"season % owns its exact radius".format(i + 1), 1e-12);
		};
		this.assertEquals(CQInkNight.irisWeatherArcs.size, 4,
			"every season is a distinct planted broken contour");
		this.assert(CQInkNight.irisWeatherArcs.every { |arcs| arcs.size >= 3 },
			"no weather ring collapses into a mechanical full circle");
	}
}
