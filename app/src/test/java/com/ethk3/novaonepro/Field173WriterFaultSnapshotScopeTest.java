package com.ethk3.novaonepro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.StatsR27;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public final class Field173WriterFaultSnapshotScopeTest {
    private static final String SNAPSHOT = "7".repeat(64);

    @Test public void validHistoricalBytesWithoutFreezeClockAreNotCurrent() {
        String original = JsonR27.write(sample());
        String hash = StatsR27.sha256(original);
        Map<String, Object> output = project(original, hash);
        assertEquals("UNKNOWN", output.get("observationState"));
        assertEquals("SNAPSHOT_CLOCK_ANCHOR_UNAVAILABLE", output.get("reason"));
        assertNull(output.get("pending"));
        assertEquals(original, output.get("historicalOriginalJson"));
        assertEquals(hash, output.get("historicalOriginalSha256"));
        assertEquals(SNAPSHOT, output.get("snapshotSha256"));
    }

    @Test public void aSelfHashedSampleWithWrongScopeIsNotVerifiedHistory() {
        for (String field : new String[]{"format", "releaseId", "sourceCommit", "databaseIdentity"}) {
            Map<String, Object> row = sample();
            row.put(field, "wrong-scope");
            String original = JsonR27.write(row);
            Map<String, Object> output = project(original, StatsR27.sha256(original));
            assertEquals(field, "UNKNOWN", output.get("observationState"));
            assertEquals(field, "SNAPSHOT_WRITER_SOURCE_IDENTITY_UNVERIFIED", output.get("reason"));
            assertNull(field, output.get("pending"));
            assertFalse(field + " must not promote the original", output.containsKey("historicalOriginalJson"));
            assertFalse(field + " must not advertise a verified hash", output.containsKey("historicalOriginalSha256"));
            assertFalse(field + " must not advertise verified scope", output.containsKey("historicalEvidenceScope"));
        }
    }

    @Test public void tamperedBytesNeverBecomeVerifiedHistory() {
        String original = JsonR27.write(sample());
        Map<String, Object> output = project(original + " ", StatsR27.sha256(original));
        assertEquals("UNKNOWN", output.get("observationState"));
        assertEquals("MAIN_PROJECTION_HASH_MISMATCH", output.get("reason"));
        assertNull(output.get("pending"));
        assertFalse(output.containsKey("historicalOriginalJson"));
    }

    @Test public void scopeValidIndependentAnchorKeepsAsOfSemantics() {
        String original = JsonR27.write(sample());
        String raw = envelope(original, StatsR27.sha256(original));
        WriterFaultSnapshotViewR343.Anchor bracket = new WriterFaultSnapshotViewR343.Anchor(
                SNAPSHOT, "release", "source", "boot", "db", 2_000L, 10_000L);
        Map<String, Object> output = WriterFaultSnapshotViewR343.project(
                raw, 99, SNAPSHOT, "release", "source", "db", bracket);
        assertEquals("OBSERVED_AS_OF_SNAPSHOT_INTERVAL", output.get("observationState"));
        assertEquals("SQL_SNAPSHOT_AS_OF_INDEPENDENT_PIN_INTERVAL; NOT_LIVE_FRESH", output.get("evidenceScope"));
        assertEquals(4L, output.get("pending"));
        assertEquals(original, output.get("historicalOriginalJson"));
    }

    private static Map<String, Object> project(String original, String hash) {
        return WriterFaultSnapshotViewR343.project(
                envelope(original, hash), 99, SNAPSHOT, "release", "source", "db", null);
    }

    private static String envelope(String original, String hash) {
        return JsonR27.write(Map.of("original", original, "sha256", hash));
    }

    private static Map<String, Object> sample() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("format", "R342_MAIN_WRITER_FAULT_PROJECTION_1");
        row.put("releaseId", "release");
        row.put("sourceCommit", "source");
        row.put("databaseIdentity", "db");
        row.put("bootId", "boot");
        row.put("processEpoch", "epoch");
        row.put("producerPid", 44);
        row.put("sampleElapsedMs", 1_000L);
        row.put("validUntilElapsedMs", 13_000L);
        row.put("pending", 4L);
        return row;
    }
}
