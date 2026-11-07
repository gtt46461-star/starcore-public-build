package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B06-C02#A4a: releasing a retired candidate drops its cache references and bytes; the hit rate and the byte
 * ledger can be checked against the entries (eviction and release subtract exactly what admission charged). */
public final class R31ReplayLibraryAccountingTest {
    private static int[][] draws(int n) {
        int[][] d = new int[n][]; Random r = new Random(41);
        for (int t = 0; t < n; t++) { int[] v = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}; for (int k = 9; k > 0; k--) { int j = r.nextInt(k + 1), x = v[k]; v[k] = v[j]; v[j] = x; } d[t] = v; }
        return d;
    }
    private static final class Outputs implements ModelReplaySU2.Library.DurableOutputs {
        int raw, calibrated;
        public OnlineModelsSU2.Output raw(ModelGenomeSU2.Member member, int[][] draws) { raw++; return new OnlineModelsSU2.Output(draws.length, 8); }
        public OnlineModelsSU2.Output calibrated(ModelGenomeSU2.Member member, int[][] draws, OnlineModelsSU2.Output r) { calibrated++; return new OnlineModelsSU2.Output(new float[draws.length][10][10], r.state); }
        public OnlineModelsSU2.Model trained(ModelGenomeSU2.Member member, int through) { throw new UnsupportedOperationException(); }
        public CalibrationSU2.Live calibrator(ModelGenomeSU2.Member member, int through) { throw new UnsupportedOperationException(); }
    }
    private static long n(Map<String, Object> m, String k) { return ((Number) m.get(k)).longValue(); }
    private static ModelGenomeSU2.Member member(double lr) { return new ModelGenomeSU2.Member("MATCHING", Collections.singletonMap("lr", Double.toString(lr)), 1.0); }
    @Test public void retiredCandidateReleaseDropsReferencesAndBytesAndLedgerMatches() {
        int[][] d = draws(60); Outputs outputs = new Outputs();
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(d, 1L << 30, null, 0, outputs);
        ModelGenomeSU2.Member a = member(0.1), b = member(0.2);
        lib.get(a); lib.get(a); lib.getCal(a); lib.get(b); lib.getCal(b); lib.getCal(b);
        Map<String, Object> before = lib.snapshotR31();
        assertEquals(4, n(before, "entries"));
        assertEquals("ledger equals the per-entry charges", n(before, "entryBytesSum"), n(before, "chargedBytes"));
        assertEquals(4, n(before, "misses")); assertEquals(4, n(before, "hits"));
        assertEquals(0.5, (Double) before.get("hitRate"), 1e-12);
        assertEquals("each output was produced exactly once", 2, outputs.raw); assertEquals(2, outputs.calibrated);
        long released = lib.releaseRetiredR31(a);
        Map<String, Object> after = lib.snapshotR31();
        assertTrue("retirement releases real bytes", released > 0);
        assertEquals(n(before, "chargedBytes") - released, n(after, "chargedBytes"));
        assertEquals("references to the retired candidate are gone", 2, n(after, "entries"));
        assertEquals(n(after, "entryBytesSum"), n(after, "chargedBytes"));
        assertEquals(2, n(after, "releasedEntries"));
        assertEquals(0, lib.releaseRetiredR31(a));
        lib.get(b);
        assertEquals("the surviving candidate is still served from cache", 2, outputs.raw);
    }
    @Test public void evictionUnderTheByteCapKeepsTheLedgerExact() {
        int[][] d = draws(40); Outputs outputs = new Outputs();
        long one = new OnlineModelsSU2.Output(d.length, 8).bytes();
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(d, 3 * one, null, 0, outputs);
        for (int i = 0; i < 12; i++) { lib.get(member(0.05 + i * 0.01)); lib.getCal(member(0.05 + i * 0.01)); }
        Map<String, Object> s = lib.snapshotR31();
        assertTrue("bounded by bytes", n(s, "chargedBytes") <= 3 * one + (long) d.length * 400L);
        assertEquals("evictions subtract exactly what was charged", n(s, "entryBytesSum"), n(s, "chargedBytes"));
        assertTrue(n(s, "evicted") > 0);
    }
}
