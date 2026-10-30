package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.Map;
import org.junit.Test;

/** R29-034/036 R29_GATE_V2 calibration support, time blocks, frequency scope and risk tail (docs/12 §2, §4).
 * Synthetic inputs check the rule mechanics only; they are not quality evidence. */
public class R29GateV2Test {
    static double[] fill(int n, double p) { double[] x = new double[n]; java.util.Arrays.fill(x, p); return x; }
    static int[] hits(int n, int k) { int[] y = new int[n]; for (int i = 0; i < k; i++) y[i] = 1; return y; }

    @Test public void singleBinIsSupportedAndNotRejected() {
        // docs/12 §2: 200 predictions at 0.34, 68 hits -> ECE 0, one non-empty bin; the old ">=5 bins" rule rejected it.
        GateV2R29.Calibration c = GateV2R29.calibration(fill(200, 0.34), hits(200, 68), 200);
        assertEquals("PASS", c.status); assertEquals(0, c.ece, 1e-12); assertEquals(1.0, c.supportedMass, 1e-12);
        assertTrue(c.inSupport(0.31)); assertFalse(c.inSupport(0.62)); // 0.62 is OUT_OF_SUPPORT, not extrapolated
    }
    @Test public void fewerThan200IsInsufficient() {
        assertEquals("INSUFFICIENT", GateV2R29.calibration(fill(199, 0.34), hits(199, 68), 199).status);
        assertEquals("INSUFFICIENT", GateV2R29.calibration(new double[0], new int[0], 0).status);
    }
    @Test public void sparseBinsStayInEceAndSupportMassIsEnforced() {
        // 181 supported at 0.34 (62 hits) + 19 sparse predictions at 0.95, all misses: supported mass 0.905 >= 0.9, but the
        // sparse error is kept in the all-sample ECE (>= 19/200*0.95) -> FAIL on ECE, never by dropping the tail.
        double[] p = new double[200]; int[] y = new int[200];
        for (int i = 0; i < 181; i++) { p[i] = 0.34; y[i] = i < 62 ? 1 : 0; }
        for (int i = 181; i < 200; i++) { p[i] = 0.95; y[i] = 0; }
        GateV2R29.Calibration c = GateV2R29.calibration(p, y, 200);
        assertEquals(0.905, c.supportedMass, 1e-12); assertEquals(19, c.count[9]); assertEquals("FAIL", c.status);
        assertTrue(c.ece > 0.09);
        // 175 supported + 25 spread one-per-bin-ish below 20 each: supported mass 0.875 < 0.9 -> FAIL even with small ECE
        double[] q = new double[200]; int[] z = new int[200];
        for (int i = 0; i < 175; i++) { q[i] = 0.34; z[i] = i < 60 ? 1 : 0; }
        for (int i = 175; i < 200; i++) { q[i] = 0.25; z[i] = i < 181 ? 1 : 0; }
        GateV2R29.Calibration d = GateV2R29.calibration(q, z, 200);
        assertEquals(25, d.count[2]); assertEquals("PASS", d.status); // 25 >= 20 supports bin 2
        for (int i = 175; i < 200; i++) q[i] = 0.05 + 0.1 * (i % 6 == 0 ? 0 : i % 6 + 3); // bins 0,4..8, each < 20
        GateV2R29.Calibration f = GateV2R29.calibration(q, z, 200);
        assertEquals(0.875, f.supportedMass, 1e-12); assertEquals("FAIL", f.status);
    }
    @Test public void probabilityOneBelongsToLastBin() { assertEquals(9, GateV2R29.bin(1.0)); assertEquals(0, GateV2R29.bin(0.0)); assertEquals(3, GateV2R29.bin(0.34)); }

    @Test public void streakTailIsExact() {
        assertEquals(0.375, GateV2R29.streakTail(3, 2, 0.5), 1e-12); // MMM, MMH, HMM of 8
        assertEquals(Math.pow(0.7, 6), GateV2R29.streakTail(6, 6, 0.7), 1e-12);
        assertEquals(0, GateV2R29.streakTail(5, 6, 0.7), 0);
    }

    @Test public void frequencyIsReportedNotRequiredAndNotDeployedBeforeReal() {
        GateV2R29.Evidence e = new GateV2R29.Evidence(); e.sequentialState = SequentialQualificationR27.QUALIFIED;
        int n = 300; e.registeredOrdinal = 0; e.latestOrdinal = 3 * 720 + 9; e.opportunities = e.latestOrdinal + 1; e.frozen = e.opportunities; e.actDecisions = n;
        e.settledActs = n; e.p = fill(n, 0.36); e.y = new int[n]; e.actOrdinal = new long[n];
        for (int i = 0; i < n; i++) { e.y[i] = i % 25 < 9 ? 1 : 0; e.actOrdinal[i] = (i / 100) * 720L + i % 100; e.hits += e.y[i]; } // 36% in every block
        e.pairedLcbVsI00 = 0.01; e.pairedLcbVsNull = 0.01;
        GateV2R29.Verdict v = GateV2R29.evaluate(e);
        assertTrue(v.failing.toString(), v.wouldIssue);
        Map<?, ?> fr = (Map<?, ?>) v.sections.get("frequency");
        assertEquals("NOT_DEPLOYED", fr.get("Q02")); assertEquals(Boolean.FALSE, fr.get("targetMet")); // 300/2170 < 15%, still issuable
        assertEquals(Boolean.FALSE, ((Map<?, ?>) v.sections.get("policy")).get("enabled"));
        e.pendingActs = 5; e.pairedLcbVsNull = Double.NaN; // pending ACT never enters HR; missing control comparison blocks
        assertTrue(GateV2R29.evaluate(e).failing.contains("betterThanShuffledControl"));
        e.hits = 80; for (int i = 0; i < n; i++) e.y[i] = i < 80 ? 1 : 0; e.pairedLcbVsNull = 0.01;
        GateV2R29.Verdict low = GateV2R29.evaluate(e);
        assertTrue(low.failing.contains("hitRate>=0.34")); assertTrue(low.failing.contains("worstBlock>=0.26"));
    }
}
