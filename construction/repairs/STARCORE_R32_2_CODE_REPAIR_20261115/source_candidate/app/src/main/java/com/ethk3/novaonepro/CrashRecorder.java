package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.PrintWriter;
import java.io.StringWriter;

/** 莉脂�鎦��綽��э�筝���膸㊧���logcat鐚�筝�������茵�綛炊�㏍�� */
public final class CrashRecorder implements Thread.UncaughtExceptionHandler {
    public static final String VERSION = "V44_NOVA_DIAGNOSIS_FREQUENCY_STABILITY_20260720_44157296";
    private static final String PREF = "v20_crash_snapshot";
    // Constructor rejects Activity/null fallback; only process-lifetime Application context is retained.
    @android.annotation.SuppressLint("StaticFieldLeak")
    private static volatile CrashRecorder installed;
    private static volatile String currentTargetIssue = "";
    private static volatile String currentStage = "";
    private final Context app;
    private final Thread.UncaughtExceptionHandler previous;

    private CrashRecorder(Context context) {
        Context c = context == null ? null : context.getApplicationContext();
        if (c == null && !(context instanceof android.app.Application))
            throw new IllegalArgumentException("APPLICATION_CONTEXT_REQUIRED");
        app = c == null ? context : c;
        previous = Thread.getDefaultUncaughtExceptionHandler();
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

    private void persist(Thread thread, Throwable error, String stage) {
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
        ed.commit();
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
