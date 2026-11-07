package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B03-C01#A2a: 1000 simulated fetch measurements yield the full distribution, its denominators and the
 * worst 20 attempts with reasons, not only the most recent 96 rows. Synthetic timings, not device network. */
public final class R31FetchLatencyDistributionTest {
    @Test public void thousandFetchesReportFullDistributionDenominatorAndWorstTwenty() {
        int n = 1000; long[] latency = new long[n]; String[] outcome = new String[n];
        Random random = new Random(310301);
        for (int i = 0; i < n; i++) {
            if (i % 50 == 7) { latency[i] = -1; outcome[i] = "FAILED:TIMEOUT"; continue; }
            latency[i] = 40 + random.nextInt(400) + (i % 97 == 0 ? 5_000 + i : 0);
            outcome[i] = i % 97 == 0 ? "ACCEPTED:SLOW_SOURCE" : i % 13 == 0 ? "REJECTED:HTTP_429" : "ACCEPTED";
        }
        Map<String, Object> d = FetchLedgerR925.LatencyDistributionR31.summarize(latency, outcome);
        assertEquals(1000, d.get("attempts"));
        assertEquals(980, d.get("measured"));
        assertEquals(20, d.get("unmeasured"));
        @SuppressWarnings("unchecked") Map<String, Long> counts = (Map<String, Long>) d.get("outcomeCounts");
        assertEquals(1000L, counts.values().stream().mapToLong(Long::longValue).sum());
        assertEquals(Long.valueOf(20), counts.get("FAILED:TIMEOUT"));
        List<Long> sorted = new ArrayList<>();
        for (long v : latency) if (v >= 0) sorted.add(v);
        Collections.sort(sorted);
        assertEquals(sorted.get((int) Math.ceil(0.50 * sorted.size()) - 1), d.get("p50Ms"));
        assertEquals(sorted.get((int) Math.ceil(0.95 * sorted.size()) - 1), d.get("p95Ms"));
        assertEquals(sorted.get((int) Math.ceil(0.99 * sorted.size()) - 1), d.get("p99Ms"));
        assertEquals(sorted.get(sorted.size() - 1), d.get("maxMs"));
        @SuppressWarnings("unchecked") List<String> worst = (List<String>) d.get("worst20");
        assertEquals(20, worst.size());
        assertTrue("worst attempt first with its reason: " + worst.get(0), worst.get(0).startsWith(sorted.get(sorted.size() - 1) + "ms|ACCEPTED:SLOW_SOURCE"));
        long previous = Long.MAX_VALUE;
        for (String row : worst) { long value = Long.parseLong(row.substring(0, row.indexOf("ms|"))); assertTrue(value <= previous); previous = value; }
        assertEquals("ALL_RETAINED_ATTEMPTS_NOT_RECENT_WINDOW", d.get("denominator"));
    }
    @Test public void emptyOrAllFailedHasNoInventedPercentile() {
        Map<String, Object> d = FetchLedgerR925.LatencyDistributionR31.summarize(new long[]{-1, -1}, new String[]{"FAILED:OFFLINE", null});
        assertEquals(2, d.get("attempts")); assertEquals(0, d.get("measured"));
        assertNull(d.get("p95Ms")); assertNull(d.get("maxMs"));
        assertTrue(((List<?>) d.get("worst20")).isEmpty());
    }
}
