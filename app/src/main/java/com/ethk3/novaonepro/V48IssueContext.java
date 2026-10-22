package com.ethk3.novaonepro;

import java.util.List;

/** Immutable per-target context. All candidates for one target share these values. */
public final class V48IssueContext {
    public static final String MODULE_VERSION = "V48_ISSUE_CONTEXT_20260721_48072148";
    public final String baseIssue;
    public final String targetIssue;
    public final int visibleHistory;
    public final long createdAt;
    public final long serviceGeneration;

    private V48IssueContext(String base, String target, int history, long generation) {
        baseIssue = safe(base);
        targetIssue = safe(target);
        visibleHistory = Math.max(0, history);
        serviceGeneration = Math.max(0L, generation);
        createdAt = System.currentTimeMillis();
    }

    public static V48IssueContext begin(
            DbStore store, List<Models.Draw> history, String base, String target) {
        int visible = 0;
        if (history != null)
            for (Models.Draw draw : history) if (draw != null && draw.isValid()) visible++;
        long generation = parseLong(get(store, "service_generation", "0"));
        V48IssueContext context = new V48IssueContext(base, target, visible, generation);
        put(store, "v48_issue_context_version", MODULE_VERSION);
        put(store, "v48_issue_base", context.baseIssue);
        put(store, "v48_issue_target", context.targetIssue);
        put(store, "v48_issue_visible_history", context.visibleHistory);
        put(store, "v48_issue_created_at", context.createdAt);
        put(store, "v48_issue_generation", context.serviceGeneration);
        return context;
    }

    public static int visibleHistory(DbStore store) {
        return parseInt(get(store, "v48_issue_visible_history", "0"));
    }

    public static String target(DbStore store) {
        return get(store, "v48_issue_target", "");
    }

    public static long generation(DbStore store) {
        return parseLong(get(store, "v48_issue_generation", "0"));
    }

    private static void put(DbStore store, String key, Object value) {
        try {
            if (store != null) store.setState(key, String.valueOf(value));
        } catch (Throwable ignore) {
        }
    }

    private static String get(DbStore store, String key, String fallback) {
        try {
            return store == null ? fallback : store.getState(key, fallback);
        } catch (Throwable error) {
            return fallback;
        }
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Throwable e) {
            return 0;
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Throwable e) {
            return 0L;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
