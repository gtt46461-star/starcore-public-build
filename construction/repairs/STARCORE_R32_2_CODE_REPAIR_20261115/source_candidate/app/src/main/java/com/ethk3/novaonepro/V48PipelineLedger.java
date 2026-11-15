package com.ethk3.novaonepro;

/** Exact per-target 1200 -> 60 -> 60 -> 40 -> 40 -> 1 -> 12 ledger. */
public final class V48PipelineLedger {
    public static final String MODULE_VERSION = "V48_PIPELINE_LEDGER_20260721_48072148";

    private V48PipelineLedger() {}

    public static void begin(DbStore store, String target) {
        put(store, "v48_ledger_version", MODULE_VERSION);
        put(store, "v48_ledger_target", safe(target));
        put(store, "v48_ledger_scan", 0);
        put(store, "v48_ledger_probability", 0);
        put(store, "v48_ledger_pre_final", 0);
        put(store, "v48_ledger_formal", 0);
        put(store, "v48_ledger_oos_inserted", 0);
        put(store, "v48_ledger_oos_duplicate", 0);
        put(store, "v48_ledger_oos_failed", 0);
        put(store, "v48_ledger_diagnostic", 0);
        put(store, "v48_ledger_ui", 0);
        put(store, "v48_ledger_lane_checked", 0);
        put(store, "v48_ledger_authorized", 0);
        put(store, "v48_ledger_status", "RUNNING");
    }

    public static void scan(DbStore store, int count) {
        put(store, "v48_ledger_scan", count);
    }

    public static void probability(DbStore store, int count) {
        put(store, "v48_ledger_probability", count);
    }

    public static void preFinal(DbStore store, int count) {
        put(store, "v48_ledger_pre_final", count);
    }

    public static void formal(DbStore store, int count) {
        put(store, "v48_ledger_formal", count);
    }

    public static void oos(DbStore store, int inserted, int duplicate, int failed) {
        put(store, "v48_ledger_oos_inserted", inserted);
        put(store, "v48_ledger_oos_duplicate", duplicate);
        put(store, "v48_ledger_oos_failed", failed);
    }

    public static void diagnostic(DbStore store, int count) {
        put(store, "v48_ledger_diagnostic", count);
    }

    public static void ui(DbStore store, int count) {
        put(store, "v48_ledger_ui", count);
    }

    public static void laneChecked(DbStore store, int count) {
        put(store, "v48_ledger_lane_checked", count);
    }

    public static void authorized(DbStore store, int count) {
        put(store, "v48_ledger_authorized", count);
    }

    public static String finalizeIssue(DbStore store) {
        int scan = getInt(store, "v48_ledger_scan");
        int probability = getInt(store, "v48_ledger_probability");
        int preFinal = getInt(store, "v48_ledger_pre_final");
        int formal = getInt(store, "v48_ledger_formal");
        int inserted = getInt(store, "v48_ledger_oos_inserted");
        int duplicate = getInt(store, "v48_ledger_oos_duplicate");
        int failed = getInt(store, "v48_ledger_oos_failed");
        int diagnostic = getInt(store, "v48_ledger_diagnostic");
        int ui = getInt(store, "v48_ledger_ui");
        int authorized = getInt(store, "v48_ledger_authorized");
        boolean oosOk = failed == 0 && inserted + duplicate == AppConfig.V48_FORMAL_CANDIDATE_COUNT;
        boolean ok =
                scan == AppConfig.V48_FULL_SCAN_CANDIDATE_COUNT
                        && probability == AppConfig.V48_PROBABILITY_CANDIDATE_COUNT
                        && preFinal == AppConfig.V48_PROBABILITY_CANDIDATE_COUNT
                        && formal == AppConfig.V48_FORMAL_CANDIDATE_COUNT
                        && oosOk
                        && diagnostic == 1
                        && ui == AppConfig.V48_UI_DISPLAY_CANDIDATE_COUNT
                        && authorized <= 1;
        String summary =
                "scan="
                        + scan
                        + "->prob="
                        + probability
                        + "->preFinal="
                        + preFinal
                        + "->formal="
                        + formal
                        + "->oos="
                        + inserted
                        + '+'
                        + duplicate
                        + "/failed="
                        + failed
                        + "->diagnostic="
                        + diagnostic
                        + "->ui="
                        + ui
                        + "->authorized="
                        + authorized
                        + "|status="
                        + (ok ? "OK" : AppConfig.V48_ROOT_SURFACE);
        put(store, "v48_ledger_status", ok ? "OK" : AppConfig.V48_ROOT_SURFACE);
        put(store, "v48_ledger_summary", summary);
        return summary;
    }

    private static int getInt(DbStore store, String key) {
        try {
            return Integer.parseInt(store == null ? "0" : store.getState(key, "0"));
        } catch (Throwable e) {
            return 0;
        }
    }

    private static void put(DbStore store, String key, Object value) {
        try {
            if (store != null) store.setState(key, String.valueOf(value));
        } catch (Throwable ignore) {
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
