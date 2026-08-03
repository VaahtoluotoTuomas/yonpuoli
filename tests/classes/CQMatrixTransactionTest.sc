// Archipelago F0 matrix contract: deep full read, clamped dense batch write,
// one push pass, one notification, and committed/transient operation metadata.

CQMatrixTransactionTest : UnitTest {
	var matrix;

	setUp {
		CQMatrix.postWarnings = false;
		matrix = CQMatrix(nil);
	}

	tearDown { CQMatrix.postWarnings = true }

	prHasApi {
		var ok = matrix.respondsTo(\fullMatrix) and: { matrix.respondsTo(\writeMatrix) };
		this.assert(ok,
			"F0: CQMatrix provides fullMatrix and writeMatrix transaction APIs");
		^ok
	}

	prDense { |value = 0.0|
		^CQMatrix.destNames.collect { CQMatrix.sourceNames.collect { value } }
	}

	test_fullReadIsDeepCopy {
		var copy;
		if(this.prHasApi.not) { ^this };
		matrix.patch(\tri1, \coco1_spaf, 0.4);
		copy = matrix.fullMatrix;
		copy[0][0] = -0.9;
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 0.4,
			"mutating a full-matrix read cannot mutate CQMatrix");
	}

	test_batchClampsAndNotifiesExactlyOnce {
		var dense, count = 0, event, ctl;
		if(this.prHasApi.not) { ^this };
		dense = this.prDense;
		dense[0][0] = 99;
		dense[0][1] = -99;
		dense[0][2] = 0.0 / 0.0;
		ctl = SimpleController(matrix);
		ctl.put(\matrix, { |obj, what ... args|
			count = count + 1;
			event = args;
		});
		matrix.writeMatrix(dense, origin: \gui, committed: true, operation: \harvest);
		ctl.remove;
		this.assertEquals(matrix.getPatch(\tri1, \coco1_spaf), 1.0,
			"batch clamps high weights");
		this.assertEquals(matrix.getPatch(\tri2, \coco1_spaf), -1.0,
			"batch clamps low weights");
		this.assertEquals(matrix.getPatch(\tri3, \coco1_spaf), 0.0,
			"batch converts NaN to an unpatched cell");
		this.assertEquals(count, 1, "448-cell batch emits exactly one notification");
		this.assertEquals(event, [\gui, true, \harvest],
			"batch notification identifies origin, commit boundary and operation");
	}

	test_batchPushesEachDestinationOnce {
		var graph, dense, writes;
		if(this.prHasApi.not) { ^this };
		graph = CQTestGraph(CQTestServer());
		matrix = CQMatrix(graph);
		dense = this.prDense(0.25);
		matrix.writeMatrix(dense, origin: \compass, committed: false,
			operation: \compassFrame);
		writes = CQMatrix.destNames.sum { |dest| graph.buses[dest].writes.size };
		this.assertEquals(writes, 28,
			"batch performs one setn per destination, never 448 per-cell pushes");
	}

	test_singleCellReportsTransientBoundary {
		var got, ctl;
		ctl = SimpleController(matrix);
		ctl.put(\patch, { |obj, what ... args| got = args });
		matrix.patch(\tri1, \coco1_spaf, 0.6,
			origin: \audition, committed: false, operation: \audition);
		ctl.remove;
		this.assertEquals(got, [\tri1, \coco1_spaf, 0.6, \audition, false, \audition],
			"audition patch is observable but explicitly transient");
	}
}
