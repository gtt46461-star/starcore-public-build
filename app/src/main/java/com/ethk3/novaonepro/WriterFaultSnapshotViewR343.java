package com.ethk3.novaonepro;

import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.StatsR27;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** A frozen SQL row is historical evidence. It cannot prove a currently live main owner. */
final class WriterFaultSnapshotViewR343 {
    private static final long TTL_MS = 12_000L;
    private WriterFaultSnapshotViewR343() {}

    /** Optional independently captured report/native pin bracket. Never construct
     * this from the writer sample itself or from export-time wall-clock guesses.
     * The current exporter has no such receipt and passes null, intentionally. */
    static final class Anchor {
        final String snapshot, release, source, boot, database;
        final long earliestPinMono, latestPinMono;
        Anchor(String snapshot, String release, String source, String boot, String database,
                long earliestPinMono, long latestPinMono) {
            this.snapshot=snapshot; this.release=release; this.source=source;
            this.boot=boot; this.database=database;
            this.earliestPinMono=earliestPinMono; this.latestPinMono=latestPinMono;
        }
    }

    static Map<String,Object> project(String raw, int observer, String snapshot, String release,
            String source, String database, Anchor anchor) {
        Map<String,Object> out = unknown(observer, "SNAPSHOT_CLOCK_ANCHOR_UNAVAILABLE");
        try {
            Map<?,?> envelope = (Map<?,?>) JsonR27.parse(raw);
            String original = (String) envelope.get("original");
            if (original == null || !StatsR27.sha256(original).equals(envelope.get("sha256")))
                return unknown(observer,"MAIN_PROJECTION_HASH_MISMATCH");
            Map<?,?> sample = (Map<?,?>) JsonR27.parse(original);
            // Preserve exact hash-verified bytes even when freshness/owner identity
            // cannot be joined. Consumers must not promote this nested historical value.
            out.put("historicalOriginalJson", original);
            out.put("historicalOriginalSha256", envelope.get("sha256"));
            out.put("historicalEvidenceScope", "HASH_VERIFIED_ROW_FROM_IMMUTABLE_SQL_SNAPSHOT; NOT_CURRENT_MAIN_STATE");
            out.put("snapshotSha256", snapshot);
            if (!"R342_MAIN_WRITER_FAULT_PROJECTION_1".equals(sample.get("format"))
                    || !Objects.equals(release,sample.get("releaseId"))
                    || !Objects.equals(source,sample.get("sourceCommit"))
                    || database == null || "UNRESOLVED".equals(database)
                    || !database.equals(sample.get("databaseIdentity"))) {
                out.put("reason","SNAPSHOT_WRITER_SOURCE_IDENTITY_UNVERIFIED");
                return out;
            }
            if (anchor == null) return out;
            if (snapshot == null || !snapshot.matches("[a-f0-9]{64}")
                    || !snapshot.equals(anchor.snapshot) || !Objects.equals(release,anchor.release)
                    || !Objects.equals(source,anchor.source) || !database.equals(anchor.database)
                    || anchor.boot == null || "UNRESOLVED".equals(anchor.boot)
                    || !anchor.boot.equals(sample.get("bootId"))
                    || anchor.earliestPinMono < 0 || anchor.latestPinMono < anchor.earliestPinMono) {
                out.put("reason","SNAPSHOT_CLOCK_ANCHOR_IDENTITY_OR_ORDER_UNVERIFIED");
                return out;
            }
            long sampled=((Number)sample.get("sampleElapsedMs")).longValue();
            long expires=((Number)sample.get("validUntilElapsedMs")).longValue();
            int producer=((Number)sample.get("producerPid")).intValue();
            Object epoch=sample.get("processEpoch");
            if (sampled<=0 || sampled>Long.MAX_VALUE-TTL_MS || expires!=sampled+TTL_MS
                    || producer<=0 || !(epoch instanceof String) || ((String)epoch).isEmpty()) {
                out.put("reason","SNAPSHOT_WRITER_SAMPLE_INVALID");
                return out;
            }
            if (sampled>anchor.earliestPinMono || expires<anchor.latestPinMono) {
                out.put("reason",expires<anchor.earliestPinMono ? "WRITER_SAMPLE_EXPIRED_BEFORE_SNAPSHOT"
                        : "SNAPSHOT_PIN_INTERVAL_NOT_FULLY_FRESH");
                return out;
            }
            for (Map.Entry<?,?> entry : sample.entrySet()) out.put((String)entry.getKey(),entry.getValue());
            out.remove("reason");
            out.put("observerPid",observer);
            out.put("observationState","OBSERVED_AS_OF_SNAPSHOT_INTERVAL");
            out.put("evidenceScope","SQL_SNAPSHOT_AS_OF_INDEPENDENT_PIN_INTERVAL; NOT_LIVE_FRESH");
            out.put("snapshotPinEarliestMonoMs",anchor.earliestPinMono);
            out.put("snapshotPinLatestMonoMs",anchor.latestPinMono);
            return out;
        } catch (RuntimeException invalid) {
            out.put("reason","MAIN_PROJECTION_INVALID");
            return out;
        }
    }

    private static Map<String,Object> unknown(int observer,String reason) {
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("observerPid",observer);out.put("producerPid",null);
        out.put("pending",null);out.put("admitting",null);
        out.put("observationState","UNKNOWN");out.put("reason",reason);
        return out;
    }
}
