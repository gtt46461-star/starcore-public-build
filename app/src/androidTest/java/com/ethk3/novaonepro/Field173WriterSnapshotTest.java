package com.ethk3.novaonepro;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.StatsR27;
import com.ethk3.novaonepro.strategyv2.WriterHoldFaultsR31;
import java.io.File;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Test;

public final class Field173WriterSnapshotTest {
    @Test public void readHistoricalOriginalAfterLiveExpiry() throws Exception {
        run("DELAYED");
    }

    @Test public void corruptAndWrongScopeHistoryStayUnverified() throws Exception {
        run("REJECT");
    }

    @Test public void preserveLiveTtlAndSnapshotReadOnlyScope() throws Exception {
        run("KEEP");
    }

    private static void run(String scenario) throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File file = new File(app.getCacheDir(), "field173-writer-projection.db");
            try {
                String original;
                try (SQLiteDatabase write = SQLiteDatabase.openOrCreateDatabase(file, null)) {
                    write.beginTransaction();
                    try {
                        RuntimeObservationScopeR341.installWithinTransaction(write);
                        write.execSQL("CREATE TABLE " + DbHelper.TABLE_APP_STATE
                                + "(state_key TEXT PRIMARY KEY, state_value TEXT, updated_at INTEGER)");
                        write.setTransactionSuccessful();
                    } finally {
                        write.endTransaction();
                    }
                    original = MainWriterFaultProjectionR342.capture(app, write, new long[]{1, 2, 3});
                    checks.require(original != null, "Production writer sample exists");
                    writeProjection(write, original);
                }
                Map<?, ?> envelope = (Map<?, ?>) JsonR27.parse(original);
                String body = (String) envelope.get("original");
                Map<?, ?> sample = (Map<?, ?>) JsonR27.parse(body);
                String database = (String) sample.get("databaseIdentity");
                checks.database("SYNTHETIC", "202611260002", 1795651440000L, database);
                long expiration = ((Number) sample.get("validUntilElapsedMs")).longValue();

                if ("REJECT".equals(scenario)) {
                    rejectInvalid(app, checks, file, database, body, envelope);
                    rejectExpiredOrWrongBoot(checks, original, sample, database, expiration);
                    return;
                }

                if ("DELAYED".equals(scenario)) {
                    SystemClock.sleep(Math.max(0L, expiration - SystemClock.elapsedRealtime()) + 5L);
                    checks.require(SystemClock.elapsedRealtime() > expiration, "Original TTL has elapsed at export");
                } else {
                    Map<String, Object> live = live(original, database, String.valueOf(sample.get("bootId")),
                            SystemClock.elapsedRealtime());
                    checks.equal("OBSERVED_MAIN_OWNER", live.get("observationState"), "A fresh live sample is still recognized");
                    checks.equal(sample.get("pending"), live.get("pending"), "Live counters retain their real source");
                    rejectExpiredOrWrongBoot(checks, original, sample, database, expiration);
                    checks.record("live", new JSONObject(live));
                }

                Object pendingBefore = WriterHoldFaultsR31.snapshot().get("pending");
                String hash = hash(file);
                Map<String, Object> report = snapshot(app, file, hash, database, checks);
                checks.equal("UNKNOWN", report.get("observationState"), "No independent SQL pin time");
                checks.equal("SNAPSHOT_CLOCK_ANCHOR_UNAVAILABLE", report.get("reason"), "Missing anchor is explicit");
                checks.equal(null, report.get("pending"), "Historical pending cannot become current");
                checks.equal(null, report.get("producerPid"), "Snapshot cannot prove current owner");
                checks.equal(body, report.get("historicalOriginalJson"), "Verified source body is retained");
                checks.equal(envelope.get("sha256"), report.get("historicalOriginalSha256"), "Verified source digest is retained");
                checks.equal(hash, report.get("snapshotSha256"), "Exact immutable SQL image hash");
                checks.equal(hash, hash(file), "Snapshot projection makes no SQLite writes");
                checks.equal(pendingBefore, WriterHoldFaultsR31.snapshot().get("pending"), "Projection does not drain the writer queue");
                checks.record("snapshot", new JSONObject(report));
            } finally {
                SQLiteDatabase.deleteDatabase(file);
            }
        });
    }

    private static void rejectInvalid(Context app, Field173NativeEvidence.Checks checks, File file,
                                      String database, String body, Map<?, ?> envelope) throws Exception {
        Map<String, String> errors = new LinkedHashMap<>();
        errors.put("changedPayload", JsonR27.write(Map.of("original", body + " ", "sha256", envelope.get("sha256"))));
        errors.put("wrongHash", JsonR27.write(Map.of("original", body, "sha256", "0".repeat(64))));
        Map<?, ?> source = (Map<?, ?>) JsonR27.parse(body);
        for (String field : new String[]{"format", "releaseId", "sourceCommit", "databaseIdentity"}) {
            Map<String, Object> bad = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) bad.put((String) entry.getKey(), entry.getValue());
            bad.put(field, "disconnected-scope");
            String changed = JsonR27.write(bad);
            errors.put(field, JsonR27.write(Map.of("original", changed, "sha256", StatsR27.sha256(changed))));
        }
        for (Map.Entry<String, String> error : errors.entrySet()) {
            try (SQLiteDatabase write = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READWRITE)) {
                writeProjection(write, error.getValue());
            }
            String sha = hash(file);
            Map<String, Object> output = snapshot(app, file, sha, database, checks);
            String expected = error.getKey().equals("changedPayload") || error.getKey().equals("wrongHash")
                    ? "MAIN_PROJECTION_HASH_MISMATCH" : "SNAPSHOT_WRITER_SOURCE_IDENTITY_UNVERIFIED";
            checks.equal("UNKNOWN", output.get("observationState"), "Rejected snapshot state");
            checks.equal(expected, output.get("reason"), "Wrong scope distinguished from byte corruption");
            checks.equal(null, output.get("pending"), "Rejected row cannot populate pending");
            checks.require(!output.containsKey("historicalOriginalJson")
                    && !output.containsKey("historicalOriginalSha256") && !output.containsKey("historicalEvidenceScope"),
                    "Rejected row does not advertise verified history");
            checks.equal(sha, hash(file), "Rejection preserves source bytes");
            checks.record(error.getKey(), new JSONObject(output));
        }
    }

    private static void rejectExpiredOrWrongBoot(Field173NativeEvidence.Checks checks, String raw,
                                                  Map<?, ?> sample, String database, long expiration) throws Exception {
        String boot = (String) sample.get("bootId");
        Map<String, Object> expired = live(raw, database, boot, expiration + 1);
        Map<String, Object> wrongBoot = live(raw, database, boot + "-different", SystemClock.elapsedRealtime());
        checks.equal("MAIN_PROJECTION_EXPIRED_OR_CLOCK_UNVERIFIED", expired.get("reason"), "Live TTL is fixed at 12 seconds");
        checks.equal("MAIN_PROJECTION_OWNER_CHANGED", wrongBoot.get("reason"), "Live result refuses another boot");
        checks.equal(null, expired.get("pending"), "Expired live result contains no pending count");
        checks.equal(null, wrongBoot.get("pending"), "Another boot cannot publish the pending count");
        checks.record("expiredLive", new JSONObject(expired));
        checks.record("wrongBootLive", new JSONObject(wrongBoot));
    }

    private static Map<String, Object> live(String raw, String database, String boot, long now) {
        int pid = android.os.Process.myPid();
        return MainWriterFaultProjectionR342.validate(raw, pid, pid, ProcessBootIdentityR906.id(), boot, database, now);
    }

    private static Map<String, Object> snapshot(Context app, File file, String sha, String database,
                                                Field173NativeEvidence.Checks checks) {
        try (SQLiteDatabase read = SQLiteDatabase.openDatabase(file.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
            checks.equal(database, RuntimeObservationScopeR341.databaseIdentity(read), "Independent SQL read identity");
            Map<String, Object> output = MainWriterFaultProjectionR342.fromSnapshot(app, read, sha);
            checks.require(!read.inTransaction(), "Projection leaves no reader transaction open");
            return output;
        }
    }

    private static String hash(File file) throws Exception {
        return StatsR27.sha256(Files.readAllBytes(file.toPath()));
    }

    private static void writeProjection(SQLiteDatabase write, String raw) {
        write.execSQL("INSERT OR REPLACE INTO " + DbHelper.TABLE_APP_STATE
                        + "(state_key,state_value,updated_at) VALUES(?,?,?)",
                new Object[]{MainWriterFaultProjectionR342.KEY, raw, System.currentTimeMillis()});
    }
}
