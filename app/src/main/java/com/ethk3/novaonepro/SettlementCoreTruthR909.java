package com.ethk3.novaonepro;

/**
 * R909 production settlement truth.  The durable settlement task + strict OOS row are the
 * authoritative core; R906 closure/outbox tables are derivative diagnostics and may lag/fail.
 */
public final class SettlementCoreTruthR909 {
    private SettlementCoreTruthR909() {}

    public static boolean businessClosed(DbStore store, String target) {
        SettlementTaskStoreR36.Terminal t = SettlementTaskStoreR36.terminal(store, safe(target));
        return t != null && (SettlementTaskStoreR36.COMPLETED.equals(t.status)
                || SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL.equals(t.status));
    }

    public static boolean settledPrediction(DbStore store, String target) {
        SettlementTaskStoreR36.Terminal t = SettlementTaskStoreR36.terminal(store, safe(target));
        return t != null && SettlementTaskStoreR36.COMPLETED.equals(t.status);
    }

    public static boolean blocked(DbStore store, String target) {
        SettlementTaskStoreR36.Terminal t = SettlementTaskStoreR36.terminal(store, safe(target));
        return t != null && (SettlementTaskStoreR36.FAILED_TERMINAL.equals(t.status)
                || SettlementTaskStoreR36.INVALID_DRAW_TERMINAL.equals(t.status));
    }

    public static String state(DbStore store, String target) {
        SettlementTaskStoreR36.Terminal t = SettlementTaskStoreR36.terminal(store, safe(target));
        if (t != null) return "TERMINAL:" + safe(t.status) + ":" + safe(t.terminalCode);
        return "OPEN";
    }

    private static String safe(String x) { return x == null ? "" : x.trim(); }
}
