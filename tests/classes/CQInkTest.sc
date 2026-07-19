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
		#[hold, flip, skip, spesal, panic, rec, drawer, day, night].do { |name|
			this.assert(inkClass.glyphNames.includes(name),
				"8x8 Pen glyph inventory includes %".format(name));
		};
	}
}
