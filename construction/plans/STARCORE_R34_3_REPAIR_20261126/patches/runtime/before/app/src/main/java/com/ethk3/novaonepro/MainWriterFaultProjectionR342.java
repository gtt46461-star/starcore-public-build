package com.ethk3.novaonepro;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.Settings;
import com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.WriterHoldFaultsR31;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Correlates a main service writer sample with its producer and a bounded observation interval. */
final class MainWriterFaultProjectionR342 {
    static final String KEY = "r342_main_writer_fault_projection";
    private static final long LIFETIME_MS = 12_000L;
    private static final String FORMAT = "R342_MAIN_WRITER_FAULT_PROJECTION_1";

    private MainWriterFaultProjectionR342() {}

    static String capture(Context context, SQLiteDatabase db, long[] schedule) {
        String epoch = ProcessBootIdentityR906.id();
        String boot = bootIdentity(context, epoch);
        String database = RuntimeObservationScopeR341.databaseIdentity(db);
        if (boot == null || RuntimeObservationScopeR341.UNRESOLVED.equals(database)) return null;
        long now = android.os.SystemClock.elapsedRealtime();
        Map<String, Object> sample = new LinkedHashMap<>(WriterHoldFaultsR31.snapshot());
        sample.remove("observerPid");
        sample.remove("observerProcessSession");
        sample.put("format", FORMAT);
        sample.put("producerPid", android.os.Process.myPid());
        sample.put("producerProcess", context.getApplicationInfo().processName);
        sample.put("processEpoch", epoch);
        sample.put("releaseId", ReleaseManifestR926.MANIFEST_SHA256);
        sample.put("sourceCommit", BuildConfig.SOURCE_COMMIT_R26);
        sample.put("databaseIdentity", database);
        sample.put("bootId", boot);
        sample.put("sampleElapsedMs", now);
        sample.put("validUntilElapsedMs", now + LIFETIME_MS);
        if (schedule != null) {
            java.util.ArrayList<Long> values = new java.util.ArrayList<>(schedule.length);
            for (long value : schedule) values.add(value);
            sample.put("controlSchedule", values);
        }
        String raw = JsonR27.write(sample);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("sha256", V51ChecksumEngine.sha256(raw));
        envelope.put("original", raw);
        return JsonR27.write(envelope);
    }

    static Map<String, Object> fromSnapshot(Context context, SQLiteDatabase db) {
        long now = android.os.SystemClock.elapsedRealtime();
        int observer = android.os.Process.myPid();
        try (Cursor row = db.rawQuery("SELECT state_value FROM " + DbHelper.TABLE_APP_STATE
                + " WHERE state_key=? AND length(CAST(state_value AS BLOB))<=24576", new String[]{KEY})) {
            if (!row.moveToFirst()) return unavailable(observer, "MAIN_PROJECTION_MISSING");
            String actualProcess = context.getApplicationInfo().processName;
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> processes = am == null ? null : am.getRunningAppProcesses();
            int producer = 0;
            if (processes != null) {
                for (ActivityManager.RunningAppProcessInfo process : processes) {
                    if (actualProcess.equals(process.processName) && process.uid == context.getApplicationInfo().uid) {
                        producer = process.pid;
                        break;
                    }
                }
            }
            Object start = JsonR27.parse(new ServiceHeartbeatStore(context).processStartScopeR341());
            String epoch = start instanceof Map ? (String) ((Map<?, ?>) start).get("processEpoch") : null;
            return validate(row.getString(0), observer, producer, epoch, bootIdentity(context, epoch),
                    RuntimeObservationScopeR341.databaseIdentity(db), now);
        } catch (android.os.OperationCanceledException cancelled) {
            throw cancelled;
        } catch (RuntimeException unavailable) {
            return unavailable(observer, "MAIN_PROJECTION_SOURCE_UNAVAILABLE");
        }
    }

    static Map<String, Object> validate(String raw, int observer, int actualProducer, String actualEpoch,
            String boot, String database, long now) {
        if (actualProducer <= 0) return unavailable(observer, "MAIN_PROCESS_UNOBSERVED");
        if (actualEpoch == null || actualEpoch.isEmpty()) return unavailable(observer, "MAIN_PROCESS_EPOCH_UNAVAILABLE");
        if (boot == null) return unavailable(observer, "BOOT_IDENTITY_UNAVAILABLE");
        try {
            Map<?, ?> envelope = (Map<?, ?>) JsonR27.parse(raw);
            String original = (String) envelope.get("original");
            if (!V51ChecksumEngine.sha256(original).equals(envelope.get("sha256"))) {
                return unavailable(observer, "MAIN_PROJECTION_HASH_MISMATCH");
            }
            Map<?, ?> source = (Map<?, ?>) JsonR27.parse(original);
            if (!FORMAT.equals(source.get("format"))
                    || !ReleaseManifestR926.MANIFEST_SHA256.equals(source.get("releaseId"))
                    || !BuildConfig.SOURCE_COMMIT_R26.equals(source.get("sourceCommit"))
                    || !database.equals(source.get("databaseIdentity"))
                    || RuntimeObservationScopeR341.UNRESOLVED.equals(database)
                    || !boot.equals(source.get("bootId"))
                    || !actualEpoch.equals(source.get("processEpoch"))
                    || actualProducer != ((Number) source.get("producerPid")).intValue()) {
                return unavailable(observer, "MAIN_PROJECTION_OWNER_CHANGED");
            }
            long sampled = ((Number) source.get("sampleElapsedMs")).longValue();
            long expires = ((Number) source.get("validUntilElapsedMs")).longValue();
            if (sampled <= 0 || sampled > now || expires != sampled + LIFETIME_MS || now > expires) {
                return unavailable(observer, "MAIN_PROJECTION_EXPIRED_OR_CLOCK_UNVERIFIED");
            }
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : source.entrySet()) out.put((String) entry.getKey(), entry.getValue());
            out.put("observerPid", observer);
            out.put("observationState", "OBSERVED_MAIN_OWNER");
            out.put("originalSha256", envelope.get("sha256"));
            return out;
        } catch (RuntimeException malformed) {
            return unavailable(observer, "MAIN_PROJECTION_INVALID");
        }
    }

    static Map<String, Object> unavailable(int observer, String reason) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("observerPid", observer);
        out.put("producerPid", null);
        out.put("pending", null);
        out.put("admitting", null);
        out.put("observationState", "UNKNOWN");
        out.put("reason", reason);
        return out;
    }

    private static String bootIdentity(Context context, String ownerEpoch) {
        String boot = IssueTimelineWriterR30.bootId();
        if (boot.startsWith("KERNEL_BOOT:")) return boot;
        try {
            int count = Settings.Global.getInt(context.getContentResolver(), Settings.Global.BOOT_COUNT, -1);
            if (count >= 0) return "ANDROID_BOOT_COUNT:" + count;
        } catch (SecurityException unavailable) {
            // Use a verified observation of the exact still-running owner when a device hides boot count.
        }
        return ownerEpoch == null ? null : "EXACT_LIVE_OWNER_EPOCH:" + ownerEpoch;
    }
}
