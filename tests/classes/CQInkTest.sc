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
			drawer, day, night].do { |name|
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
		this.assertEquals(metrics[\knobDiameter], 26, "knob body is exactly 26 px");
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
}
