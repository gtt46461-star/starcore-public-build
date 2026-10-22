package com.ethk3.novaonepro;

/** Lightweight stage trace used to locate missing calls without changing decisions. */
public final class StrategyExecutionTraceEngine {
    public static final String VERSION = AppConfig.V43_TRACE_VERSION;

    private StrategyExecutionTraceEngine() {}

    public static long begin(DbStore store, String stage, int count) {
        long now = System.currentTimeMillis();
        if (store != null)
            try {
                store.setState("v43_trace_current_stage", safe(stage));
                store.setState("v43_trace_" + key(stage) + "_input", String.valueOf(count));
                store.setState("v43_trace_" + key(stage) + "_started", String.valueOf(now));
            } catch (Throwable ignore) {
            }
        return now;
    }

    public static void end(DbStore store, String stage, int count, long started, String status) {
        if (store != null)
            try {
                store.setState("v43_trace_" + key(stage) + "_output", String.valueOf(count));
                store.setState(
                        "v43_trace_" + key(stage) + "_cost_ms",
                        String.valueOf(Math.max(0L, System.currentTimeMillis() - started)));
                store.setState("v43_trace_" + key(stage) + "_status", safe(status));
                store.setState("v43_trace_version", VERSION);
            } catch (Throwable ignore) {
            }
    }

    private static String key(String s) {
        return safe(s).toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
