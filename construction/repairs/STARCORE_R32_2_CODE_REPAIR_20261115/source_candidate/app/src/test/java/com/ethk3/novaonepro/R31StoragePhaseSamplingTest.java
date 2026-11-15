package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import org.junit.Test;

/** R31-B05-C01#A2: the same path sampled at each stage locates the snapshot peak and the post-cleanup
 * steady state in the bounded time series. Real temporary files; fixture wall clock, not handset time. */
public final class R31StoragePhaseSamplingTest {
    private static void write(File root, String name, int bytes) throws IOException {
        File f = new File(root, name); f.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(f)) { out.write(new byte[bytes]); }
    }
    private static long bytesOf(Map<String, Object> point) { return ((Number) point.get("regularFileBytes")).longValue(); }
    @Test public void stageSamplesLocatePeakAndSteadyState() throws Exception {
        File root = Files.createTempDirectory("r31-storage-phase-").toFile();
        write(root, "databases/main.db", 4096);
        StorageLedgerR31 ledger = new StorageLedgerR31(root);
        long wall = 1_700_000_000_000L;
        ledger.record(StorageCensusSU2.measure(root), "STARTUP", "host-owner", wall, 1L << 30, 0, "NORMAL", "FIXTURE_CLOCK");
        write(root, "files/r981_reports/jobs/000001/snapshot.db", 65_536);
        ledger.record(StorageCensusSU2.measure(root), "REPORT_SNAPSHOT_PUBLISHED", "report-job-1", wall + 60_000, 1L << 30, 65_536, "NORMAL", "FIXTURE_CLOCK");
        assertTrue(new File(root, "files/r981_reports/jobs/000001/snapshot.db").delete());
        ledger.record(StorageCensusSU2.measure(root), "AFTER_REPORT_CLEANUP", "report-job-1", wall + 120_000, 1L << 30, 0, "NORMAL", "FIXTURE_CLOCK");
        Map<String, Map<String, Object>> byPhase = new LinkedHashMap<>();
        for (Map<String, Object> point : ledger.history(16)) byPhase.put(String.valueOf(point.get("phase")), point);
        assertEquals(new HashSet<>(Arrays.asList("STARTUP", "REPORT_SNAPSHOT_PUBLISHED", "AFTER_REPORT_CLEANUP")), byPhase.keySet());
        long startup = bytesOf(byPhase.get("STARTUP")), peak = bytesOf(byPhase.get("REPORT_SNAPSHOT_PUBLISHED")), steady = bytesOf(byPhase.get("AFTER_REPORT_CLEANUP"));
        assertTrue("peak includes the published snapshot: startup=" + startup + " peak=" + peak, peak >= startup + 65_536);
        assertTrue("cleanup returns to steady state: peak=" + peak + " steady=" + steady, steady <= peak - 60_000);
        assertTrue("steady state differs from startup only by the bounded sample ledger: " + startup + "/" + steady, Math.abs(steady - startup) < 8192);
        for (Map<String, Object> point : byPhase.values()) {
            assertEquals("FIXTURE_CLOCK", point.get("clockRole"));
            assertNotNull("every point is bound to a boot", point.get("boot"));
        }
        assertEquals("the owner of the peak is recorded", "report-job-1", byPhase.get("REPORT_SNAPSHOT_PUBLISHED").get("owner"));
    }
}
