package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ethk3.novaonepro.strategyv2.JsonR27;

/** 莉脂�鎦��綽��э�筝���膸㊧���logcat鐚�筝�������茵�綛炊�㏍�� */
public final class CrashRecorder implements Thread.UncaughtExceptionHandler {
    public static final String VERSION = "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String PREF = "v20_crash_snapshot";
    private static final String EVENT_INDEX = "r342_timestamped_events";
    private static final int EVENT_LIMIT = 64;
    // Constructor rejects Activity/null fallback; only process-lifetime Application context is retained.
    @android.annotation.SuppressLint("StaticFieldLeak")
    private static volatile CrashRecorder installed;
    private static volatile String currentTargetIssue = "";
    private static volatile String currentStage = "";
    private final Context app;
    private final Thread.UncaughtExceptionHandler previous;
    private final RuntimeObservationScopeR341 recorderScope;

    private CrashRecorder(Context context) {
        Context c = context == null ? null : context.getApplicationContext();
        if (c == null && !(context instanceof android.app.Application))
            throw new IllegalArgumentException("APPLICATION_CONTEXT_REQUIRED");
        app = c == null ? context : c;
        previous = Thread.getDefaultUncaughtExceptionHandler();
        recorderScope = RuntimeObservationScopeR341.capture(null, null, RuntimeObservationScopeR341.SYSTEM);
    }

    public static void install(Context context) {
        if (context == null) return;
        if (installed != null) return;
        synchronized (CrashRecorder.class) {
            if (installed == null) {
                CrashRecorder r = new CrashRecorder(context);
                installed = r;
                Thread.setDefaultUncaughtExceptionHandler(r);
                LifecycleHealthR963.install(context);
            }
        }
    }

    public static void setRuntimeContext(String targetIssue, String stage) {
        currentTargetIssue = targetIssue == null ? "" : targetIssue;
        currentStage = stage == null ? "" : stage;
    }

    public static void recordCaught(Context context, Throwable e, String stage) {
        try {
            CrashRecorder r = installed;
            if (r == null) {
                install(context);
                r = installed;
            }
            if (r != null) r.persist(Thread.currentThread(), e, stage);
        } catch (Throwable ignore) {
        }
    }

    public void uncaughtException(Thread thread, Throwable error) {
        try {
            persist(thread, error, currentStage);
        } catch (Throwable ignore) {
        }
        if (previous != null && previous != this) previous.uncaughtException(thread, error);
    }

    private synchronized void persist(Thread thread, Throwable error, String stage) {
        if (app == null) return;
        if (error instanceof OutOfMemoryError) {
            persistOomLowAllocation(thread, stage);
            return;
        }
        long now = System.currentTimeMillis();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (error != null) error.printStackTrace(pw);
        pw.flush();
        String stack = sw.toString();
        String[] lines = stack.split("\\n");
        StringBuilder shortStack = new StringBuilder();
        for (int i = 0; i < lines.length && i < 24; i++) shortStack.append(lines[i]).append('\n');
        Runtime rt = Runtime.getRuntime();
        Models.DatabaseQueueState q = new Models.DatabaseQueueState();
        try {
            DatabaseCoordinator c = DatabaseCoordinator.peek();
            if (c != null) q = c.snapshot();
        } catch (Throwable ignore) {
        }
        SharedPreferences prefs = app.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putLong("at", now);
        ed.putInt("crash_count", prefs.getInt("crash_count", 0) + 1);
        long windowStart = prefs.getLong("crash_window_start", 0L);
        int windowCount = prefs.getInt("crash_window_count", 0);
        if (windowStart <= 0L || now - windowStart > 600000L) {
            windowStart = now;
            windowCount = 0;
        }
        windowCount++;
        ed.putLong("crash_window_start", windowStart);
        ed.putInt("crash_window_count", windowCount);
        if (error instanceof OutOfMemoryError)
            ed.putInt("oom_count", prefs.getInt("oom_count", 0) + 1);
        if (error instanceof StackOverflowError)
            ed.putInt("soe_count", prefs.getInt("soe_count", 0) + 1);
        ed.putString("thread", thread == null ? "" : thread.getName());
        ed.putString("type", error == null ? "" : error.getClass().getName());
        ed.putString("message", error == null ? "" : String.valueOf(error.getMessage()));
        ed.putString("stack", shortStack.toString());
        ed.putString("target_issue", currentTargetIssue);
        ed.putString("stage", stage == null ? "" : stage);
        ed.putInt("db_queue", q.waitingWriters + q.backgroundQueue);
        ed.putLong("free_memory", rt.freeMemory());
        ed.putLong("total_memory", rt.totalMemory());
        ed.putString(
                "fingerprint",
                (error == null ? "" : error.getClass().getName())
                        + "|"
                        + (stage == null ? "" : stage)
                        + "|"
                        + (thread == null ? "" : thread.getName()));
        ed.putLong("max_memory", rt.maxMemory());
        ed.putString("version", AppConfig.B07R2_VERSION);
        try {
            appendTimestampedRecordR342(prefs, ed, now, error, stage, shortStack.toString());
        } catch (RuntimeException damagedIndex) {
            ed.putLong("r342_index_append_failed_at", now);
        }
        ed.commit();
    }

    private void appendTimestampedRecordR342(SharedPreferences prefs, SharedPreferences.Editor editor,
            long wall, Throwable error, String stage, String stack) {
        List<Object> rows = new ArrayList<>(readEventIndexR342(prefs));
        if (rows.size() >= EVENT_LIMIT) rows.remove(0);
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("release", recorderScope.releaseId);
        row.put("sourceCommit", recorderScope.sourceCommit);
        row.put("processEpoch", recorderScope.processEpoch);
        row.put("bootId", recorderScope.bootId);
        row.put("pid", android.os.Process.myPid());
        row.put("kind", error == null ? "UNKNOWN" : error.getClass().getName());
        String stageName = stage == null ? "" : stage;
        row.put("stage", stageName.substring(0, Math.min(stageName.length(), 96)));
        row.put("wallMs", wall);
        row.put("elapsedMs", TimeSourceR811.monoNowMs());
        row.put("stackPrefixSha256", V51ChecksumEngine.sha256(stack));
        rows.add(row);
        editor.putString(EVENT_INDEX, JsonR27.write(rows));
    }

    private static List<?> readEventIndexR342(SharedPreferences prefs) {
        Object raw = JsonR27.parse(prefs.getString(EVENT_INDEX, "[]"));
        if (!(raw instanceof List) || ((List<?>) raw).size() > EVENT_LIMIT)
            throw new IllegalStateException("CRASH_EVENT_INDEX_INVALID");
        return (List<?>) raw;
    }

    private static void appendWindowR342(StringBuilder result, SharedPreferences prefs) {
        try {
            long elapsed = TimeSourceR811.monoNowMs();
            RuntimeObservationScopeR341 scope = RuntimeObservationScopeR341.capture(null, null, RuntimeObservationScopeR341.SYSTEM);
            List<?> rows = readEventIndexR342(prefs);
            int recent = 0, otherBuild = 0;
            for (Object raw : rows) {
                if (!(raw instanceof Map)) throw new IllegalStateException("CRASH_EVENT_INVALID");
                Map<?,?> row = (Map<?,?>)raw;
                if (!scope.releaseId.equals(row.get("release")) || !scope.sourceCommit.equals(row.get("sourceCommit"))) {
                    otherBuild++;
                    continue;
                }
                if (!scope.bootId.equals(row.get("bootId"))) continue;
                Object recorded = row.get("elapsedMs");
                if (!(recorded instanceof Number)) throw new IllegalStateException("CRASH_EVENT_TIME_INVALID");
                long at = ((Number)recorded).longValue();
                if (at >= 0 && at <= elapsed && elapsed - at <= 600_000L) recent++;
            }
            result.append("r342CurrentReleaseRecordedEvents10m=").append(recent)
                    .append("|scope=BOUNDED_APP_EVENTS_SAME_BOOT")
                    .append("|indexEntries=").append(rows.size())
                    .append("|historicalReleaseEntries=").append(otherBuild)
                    .append("|lastOomWithoutIndexAt=").append(prefs.getLong("r342_oom_unindexed_wall",0L))
                    .append("|lastIndexWriteFailureAt=").append(prefs.getLong("r342_index_append_failed_at",0L))
                    .append('\n');
            result.append("r342CrashEventIndex=").append(JsonR27.write(rows)).append('\n');
        } catch (RuntimeException error) {
            result.append("r342CurrentReleaseRecordedEvents10m=UNKNOWN|cause=")
                    .append(error.getClass().getSimpleName()).append('\n');
        }
    }

    private void persistOomLowAllocation(Thread thread, String stage) {
        try {
            long now = System.currentTimeMillis();
            Runtime rt = Runtime.getRuntime();
            SharedPreferences prefs = app.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            int crashes = prefs.getInt("crash_count", 0);
            int ooms = prefs.getInt("oom_count", 0);
            SharedPreferences.Editor ed = prefs.edit();
            ed.putLong("at", now);
            ed.putInt("crash_count", crashes + 1);
            ed.putInt("oom_count", ooms + 1);
            ed.putString("thread", thread == null ? "" : thread.getName());
            ed.putString("type", "java.lang.OutOfMemoryError");
            ed.putString("message", "OOM_LOW_ALLOCATION_RECORD");
            ed.putString("stack", "OOM_STACK_SKIPPED_TO_PRESERVE_PROCESS");
            ed.putString("target_issue", currentTargetIssue);
            ed.putString("stage", stage == null ? "" : stage);
            ed.putLong("free_memory", rt.freeMemory());
            ed.putLong("total_memory", rt.totalMemory());
            ed.putLong("max_memory", rt.maxMemory());
            ed.putString("fingerprint", "OOM_LOW_ALLOC");
            ed.putString("version", AppConfig.B07R2_VERSION);
            ed.putLong("r342_oom_unindexed_wall", now);
            ed.putString("r342_oom_unindexed_source", recorderScope.sourceCommit);
            ed.putString("r342_oom_unindexed_process", recorderScope.processEpoch);
            ed.putString("r342_oom_unindexed_boot", recorderScope.bootId);
            ed.putLong("r342_oom_unindexed_elapsed", TimeSourceR811.monoNowMs());
            ed.commit();
        } catch (Throwable ignore) {
        }
    }

    public static String detailedSummary(Context context) {
        if (context == null) return "NO_CONTEXT";
        try {
            SharedPreferences p =
                    context.getApplicationContext()
                            .getSharedPreferences(PREF, Context.MODE_PRIVATE);
            StringBuilder b = new StringBuilder(4096);
            b.append("crashCount=").append(p.getInt("crash_count", 0)).append('\n');
            b.append("oomCount=").append(p.getInt("oom_count", 0)).append('\n');
            b.append("stackOverflowCount=").append(p.getInt("soe_count", 0)).append('\n');
            long observedAt = System.currentTimeMillis();
            b.append("windowCrashCount10m=")
                    .append(recentCrashCountR77(observedAt, p.getLong("at", 0L), p.getInt("crash_count", 0)))
                    .append('\n');
            b.append("windowCrashCountScope=NO_ROLLING_TIMESTAMP_LOG;UNKNOWN_WHEN_RECENT_EVENT\n");
            b.append("recordedCrashWindowCount=").append(p.getInt("crash_window_count", 0)).append('\n');
            b.append("recordedCrashWindowStart=").append(p.getLong("crash_window_start", 0L)).append('\n');
            appendWindowR342(b,p);
            b.append("crashObservedAt=").append(observedAt).append('\n');
            b.append("lastAt=").append(p.getLong("at", 0L)).append('\n');
            b.append("thread=").append(p.getString("thread", "")).append('\n');
            b.append("type=").append(p.getString("type", "")).append('\n');
            b.append("message=").append(p.getString("message", "")).append('\n');
            b.append("stage=").append(p.getString("stage", "")).append('\n');
            b.append("targetIssue=").append(p.getString("target_issue", "")).append('\n');
            b.append("dbQueue=").append(p.getInt("db_queue", 0)).append('\n');
            b.append("freeMemory=").append(p.getLong("free_memory", 0L)).append('\n');
            b.append("totalMemory=").append(p.getLong("total_memory", 0L)).append('\n');
            b.append("maxMemory=").append(p.getLong("max_memory", 0L)).append('\n');
            b.append("fingerprint=").append(p.getString("fingerprint", "")).append('\n');
            b.append("stack=\n").append(p.getString("stack", ""));
            b.append("\n").append(LifecycleHealthR963.summary(context));
            return b.toString();
        } catch (Throwable e) {
            return "CRASH_EVIDENCE_READ_ERROR:" + e.getClass().getSimpleName();
        }
    }

    /** Legacy counters are fixed buckets, not a rolling ten-minute event log. */
    static String recentCrashCountR77(long now, long lastAt, int lifetimeCount) {
        if (lastAt <= 0L) return lifetimeCount == 0 ? "0" : "UNKNOWN";
        if (now < lastAt) return "UNKNOWN_CLOCK_ROLLBACK";
        return now - lastAt > 600000L ? "0" : "UNKNOWN";
    }

    /** Uses the application context captured by install(), so non-UI report engines can read it. */
    public static String detailedSummary() {
        CrashRecorder r = installed;
        return r == null ? "CRASH_RECORDER_NOT_INSTALLED" : detailedSummary(r.app);
    }

    public static Models.CrashSnapshot load(Context context) {
        Models.CrashSnapshot x = new Models.CrashSnapshot();
        if (context == null) return x;
        try {
            SharedPreferences p =
                    context.getApplicationContext()
                            .getSharedPreferences(PREF, Context.MODE_PRIVATE);
            x.at = p.getLong("at", 0L);
            x.threadName = p.getString("thread", "");
            x.errorType = p.getString("type", "");
            x.message = p.getString("message", "");
            x.stack = p.getString("stack", "");
            x.targetIssue = p.getString("target_issue", "");
            x.stage = p.getString("stage", "");
            x.databaseQueueLength = p.getInt("db_queue", 0);
            x.freeMemory = p.getLong("free_memory", 0L);
            x.totalMemory = p.getLong("total_memory", 0L);
        } catch (Throwable ignore) {
        }
        return x;
    }
}
