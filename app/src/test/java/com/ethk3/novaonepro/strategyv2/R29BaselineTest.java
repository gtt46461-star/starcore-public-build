package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.junit.Test;

/** R29 B00: R29-001 identity rules, R29-002 preserved ledger (1832 requirements / 299 file cards) and R29-003 metric
 * results and complete denominators. No fixture can turn a missing receipt into PASS. */
public class R29BaselineTest {
    static String asset() throws Exception { return new String(Files.readAllBytes(Paths.get("src/main/assets/" + LegacyContinuityR27.PRESERVATION_ASSET)), StandardCharsets.UTF_8); }

    @Test public void packagedLedgerKeepsEveryRequirementAndCard() throws Exception {
        Map<String, Object> m = LegacyContinuityR27.preservation(asset(), new HashMap<String, String>());
        assertEquals(1832L, m.get("requirements")); assertEquals(1832L, m.get("uniqueRequirementIds")); assertEquals(299L, m.get("fileCards"));
        assertEquals(0L, m.get("duplicateIds")); assertEquals(0L, m.get("missingStatusOwnerOrEvidencePath")); assertEquals(0L, m.get("completionClaimsWithoutReceipt"));
        assertEquals(1832L, m.get("notYetReverified")); assertEquals(Boolean.TRUE, m.get("ledgerComplete"));
        assertEquals("RECORDED_NOT_REVERIFIED", m.get("status"));
        R27TestSupport.log("R29_002_PASS " + m.get("byClosure") + " unresolvedCardLinks=" + m.get("unresolvedCardLinks"));
    }

    @SuppressWarnings("unchecked")
    @Test public void droppedDuplicatedOrClaimedRequirementsFail() throws Exception {
        Map<String, Object> root = (Map<String, Object>) JsonR27.parse(asset());
        List<Object> reqs = (List<Object>) root.get("requirements");
        // A completion claim without a receipt.
        ((Map<String, Object>) reqs.get(0)).put("closure", "VERIFIED");
        assertEquals("FAIL", LegacyContinuityR27.preservation(JsonR27.write(root), new HashMap<String, String>()).get("status"));
        // The same claim with a receipt is accepted as a receipt, not as silence.
        Map<String, String> receipts = new HashMap<>(); receipts.put((String) ((Map<String, Object>) reqs.get(0)).get("id"), "receipt://test");
        Map<String, Object> ok = LegacyContinuityR27.preservation(JsonR27.write(root), receipts);
        assertEquals("RECORDED_NOT_REVERIFIED", ok.get("status")); assertEquals(1L, ok.get("regressionReceipts"));
        // Dropping or duplicating a requirement is a ledger failure.
        ((Map<String, Object>) reqs.get(0)).put("closure", "NOT_VERIFIED");
        reqs.set(1, reqs.get(0));
        assertEquals("FAIL", LegacyContinuityR27.preservation(JsonR27.write(root), receipts).get("status"));
        reqs.remove(1);
        Map<String, Object> dropped = LegacyContinuityR27.preservation(JsonR27.write(root), receipts);
        assertEquals("FAIL", dropped.get("status")); assertEquals(1831L, dropped.get("requirements"));
    }

    @SuppressWarnings("unchecked")
    @Test public void missingReceiptsNeverBecomePassOrNumbers() {
        Map<String, Object> snapshot = new LinkedHashMap<>(); snapshot.put("timeRange", null);
        List<Map<String, Object>> rows = MetricEvaluatorR29.evaluate(ReportR29.probes(null, null), snapshot);
        assertEquals(44, rows.size());
        for (Map<String, Object> r : rows) {
            assertNotEquals(r.get("metricId") + " PASS without evidence", "PASS", r.get("status"));
            if (!"M32".equals(r.get("metricId"))) { assertNull(r.get("value")); assertNull(r.get("sampleCount")); assertNotNull(r.get("missingReason")); }
            assertFalse(((List<Object>) r.get("repairCardIds")).isEmpty());
        }
        try { new MetricEvaluatorR29.Probe("PASS", "", "", 1, 1, 1.0, 0, new ArrayList<String>()); fail("PASS without samples accepted"); } catch (IllegalArgumentException expected) { }
    }

    @Test public void identityAndPreservationProbes() throws Exception {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("sourceCommit", "4eec7146ea172078cfcc2b62bb9998a14cd122a1"); b.put("installedApkSha256", sha('a')); b.put("signingCertificateSha256", sha('b'));
        b.put("releaseManifestSha256", sha('c')); b.put("versionCode", 577291001L);
        assertTrue(BaselineIdentityR29.consistent(b));
        Map<String, MetricEvaluatorR29.Probe> p = ReportR29.probes(b, LegacyContinuityR27.preservation(asset(), new HashMap<String, String>()));
        assertEquals("PARTIAL", p.get("M01").status); assertEquals("PARTIAL", p.get("M02").status); assertEquals(1.0, p.get("M02").value.doubleValue(), 0);
        b.put("versionCode", 577271030L); assertFalse("version must advance past R27", BaselineIdentityR29.consistent(b));
        b.put("versionCode", 577291001L); b.put("installedApkSha256", null); assertFalse(BaselineIdentityR29.consistent(b));
        assertEquals("FAIL", ReportR29.probes(b, null).get("M01").status);
    }
    static String sha(char c) { char[] x = new char[64]; Arrays.fill(x, c); return new String(x); }

    @SuppressWarnings("unchecked")
    @Test public void crossStrategyRepeatedIssuesAreNotIndependentSamples() {
        ReportR27.Input in = new ReportR27.Input();
        for (int s = 0; s < 3; s++) { Map<String, Object> o = new LinkedHashMap<>(); o.put("observer", "o" + s); o.put("periods", 100L); o.put("hits", 31L); in.observers.add(o); }
        in.shadowStatusCensus.put("OBSERVE", 24L); in.shadowStatusCensus.put("OBSERVE_RETIRED", 80L); in.shadowStatusCensus.put("CONTROL_I00", 1L); in.shadowStatusCensus.put("CONTROL_NULL", 1L);
        in.observerStrategiesTotal = 104; in.observerRowsTotal = 1651; in.observerHitsTotal = 507; in.observerDistinctIssues = 120; in.observerActRowsTotal = 659; in.observerActHitsTotal = 176; in.observerActDistinctIssues = 90;
        Map<String, Object> d = (Map<String, Object>) ((Map<String, Object>) ReportR27.truth(in).get("observers")).get("denominators");
        assertEquals(300L, d.get("displayedStrategyPeriodRows")); assertEquals(1651L, d.get("strategyPeriodRowsTotal")); assertEquals(120L, d.get("independentIssues"));
        assertEquals(507 / 1651.0, (Double) d.get("strategyPeriodHitRate"), 1e-12); assertEquals(176 / 659.0, (Double) d.get("actHitRate"), 1e-12);
        assertEquals("DISTINCT_TARGET_ISSUE", d.get("effectiveSampleUnit")); assertEquals(106L, sum((Map<String, Object>) d.get("shadowStatusCensus")));
        // Unmeasured denominators stay null instead of 0.
        Map<String, Object> empty = (Map<String, Object>) ((Map<String, Object>) ReportR27.truth(new ReportR27.Input()).get("observers")).get("denominators");
        assertNull(empty.get("independentIssues")); assertNull(empty.get("strategyPeriodHitRate"));
    }
    static long sum(Map<String, Object> m) { long s = 0; for (Object v : m.values()) s += ((Number) v).longValue(); return s; }
}
