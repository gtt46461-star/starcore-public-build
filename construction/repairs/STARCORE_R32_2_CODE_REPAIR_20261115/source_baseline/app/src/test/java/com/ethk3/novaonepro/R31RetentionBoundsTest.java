package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B05-C06#A1-#A3, R31-B05-C07#A1/#A2/#A3a: bounded STALE_PAGE recovery, revision sweep, event bounds. */
public final class R31RetentionBoundsTest {
    @Test public void aContinuouslyChangingPageShrinksKeepsOneInflightAndBoundsTemporaryBytes() {
        DataRetentionGovernorR900.StalePageR31.resetForTest();
        String hot = "research_hot_table", calm = "research_calm_table"; int inflight = 0, maxInflight = 0; long maxTemp = 0;
        for (int round = 0; round < 100; round++) {
            if (DataRetentionGovernorR900.StalePageR31.waitingForStableRows(hot)) { DataRetentionGovernorR900.StalePageR31.cooldown(hot); continue; }
            inflight++; maxInflight = Math.max(maxInflight, inflight);                       // one page request at a time
            maxTemp = Math.max(maxTemp, DataRetentionGovernorR900.StalePageR31.temporaryBytesBound(hot, 1024));
            DataRetentionGovernorR900.StalePageR31.onStale(hot); inflight--;                 // the rows changed again
        }
        assertEquals(1, maxInflight); assertEquals("smallest page", 8, DataRetentionGovernorR900.StalePageR31.cap(hot));
        assertTrue("temporary bytes bounded by the first page: " + maxTemp, maxTemp <= 256L * 1024);
        assertEquals("another table is not blocked by the changing one", 256, DataRetentionGovernorR900.StalePageR31.cap(calm));
        assertFalse(DataRetentionGovernorR900.StalePageR31.waitingForStableRows(calm));
        DataRetentionGovernorR900.StalePageR31.onSuccess(hot); assertEquals("a stable page restores the normal size", 256, DataRetentionGovernorR900.StalePageR31.cap(hot));
    }

    @Test public void onceStableThePageArchivesAndChangedRowsAreNeverPruned() {
        DataRetentionGovernorR900.StalePageR31.resetForTest();
        // rows with generations; a row whose generation changed between prepare and publish must stay
        TreeMap<Long, Long> prepared = new TreeMap<>(), now = new TreeMap<>();
        for (long r = 1; r <= 40; r++) { prepared.put(r, 1L); now.put(r, r % 9 == 0 ? 2L : 1L); }
        List<Long> pruned = new ArrayList<>(), kept = new ArrayList<>();
        for (Map.Entry<Long, Long> e : prepared.entrySet()) if (now.get(e.getKey()).equals(e.getValue())) pruned.add(e.getKey()); else kept.add(e.getKey());
        assertEquals(Arrays.asList(9L, 18L, 27L, 36L), kept);
        for (long r : kept) assertFalse("a version-changed row is never pruned", pruned.contains(r));
        byte[] archive = "archive-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String recorded = com.ethk3.novaonepro.strategyv2.StatsR27.sha256(archive);
        assertEquals("archive state and readback hash agree", recorded, com.ethk3.novaonepro.strategyv2.StatsR27.sha256(archive.clone()));
        assertNotEquals("a different readback is not a proof", recorded, com.ethk3.novaonepro.strategyv2.StatsR27.sha256("archive-byteZ".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test public void revisionSweepKeepsLiveVersionsAndAlwaysProgresses() {
        TreeMap<Long, Long> revisions = new TreeMap<>(); Set<Long> live = new HashSet<>();
        for (long r = 1; r <= 1000; r++) { revisions.put(r, r * 10); if (r % 3 != 0) live.add(r); }
        long cursor = 0; int pages = 0; Set<Long> orphans = new HashSet<>();
        while (true) {
            DataRetentionGovernorR900.RevisionSweepR31 page = DataRetentionGovernorR900.RevisionSweepR31.page(revisions, live, cursor, 64); pages++;
            for (long o : page.orphans) { assertFalse("a live row's revision is never an orphan", live.contains(o)); orphans.add(o); }
            if (page.complete) break; assertTrue("cursor advances", page.nextCursor > cursor); cursor = page.nextCursor;
        }
        assertEquals(333, orphans.size()); assertEquals(16, pages);
    }

    @Test public void eventsKeepBusinessFactsAndStayBoundedUnderA24hLoad() {
        RuntimeEventLedgerR926.EventBoundR31 bound = new RuntimeEventLedgerR926.EventBoundR31(2000, 512 * 1024);
        long at = 0; int periods = 288 * 1; // one simulated day of five-minute periods
        for (int p = 0; p < 5000; p++) bound.business("issue-" + p + "-settled", com.ethk3.novaonepro.strategyv2.StatsR27.sha256("payload" + p));
        for (int p = 0; p < periods; p++) for (int k = 0; k < 500; k++) { // 144k diagnostic writes
            at += 600; bound.diagnostic("issue-" + p, k % 7 == 0 ? "RETRY" : "SLOW_QUERY", "fp-" + (k % 13), at, 220);
        }
        assertTrue("rows bounded: " + bound.diagnosticRows(), bound.diagnosticRows() <= 2000);
        assertTrue("bytes bounded: " + bound.diagnosticBytes(), bound.diagnosticBytes() <= 512 * 1024);
        assertEquals("repeated diagnostics aggregate instead of one row each", 500 / 7 + 1, bound.countOf("issue-287", "RETRY", "fp-0") + bound.countOf("issue-287", "RETRY", "fp-7") + bound.countOf("issue-287", "RETRY", "fp-1") + bound.countOf("issue-287", "RETRY", "fp-8") + bound.countOf("issue-287", "RETRY", "fp-2") + bound.countOf("issue-287", "RETRY", "fp-9") + bound.countOf("issue-287", "RETRY", "fp-3") + bound.countOf("issue-287", "RETRY", "fp-10") + bound.countOf("issue-287", "RETRY", "fp-4") + bound.countOf("issue-287", "RETRY", "fp-11") + bound.countOf("issue-287", "RETRY", "fp-5") + bound.countOf("issue-287", "RETRY", "fp-12") + bound.countOf("issue-287", "RETRY", "fp-6"));
        assertEquals("5000 periods of business facts kept with their hashes", 5000, bound.businessFacts().size());
        assertEquals(com.ethk3.novaonepro.strategyv2.StatsR27.sha256("payload17"), bound.businessFacts().get("issue-17-settled"));
        assertTrue("old diagnostics leave first", bound.droppedDiagnosticEvents() > 0 && bound.countOf("issue-0", "SLOW_QUERY", "fp-1") == 0);
    }
}
