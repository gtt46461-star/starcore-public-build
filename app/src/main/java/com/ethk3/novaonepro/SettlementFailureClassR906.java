package com.ethk3.novaonepro;

/** Typed settlement failure taxonomy.  A transport/SQLite retry is never confused with a business exclusion. */
public enum SettlementFailureClassR906 {
    NONE,
    EXCLUDED_NO_PREDRAW,
    BLOCKED_DRAW_CONFLICT,
    BLOCKED_PIT_CONFLICT,
    TRANSIENT_DB_BUSY,
    TRANSIENT_MEMORY_PRESSURE,
    INVALID_DRAW,
    PERMANENT_INTEGRITY,
    UNKNOWN;

    public static SettlementFailureClassR906 classify(String code) {
        String x = code == null ? "" : code.trim().toUpperCase(java.util.Locale.US);
        if (x.length() == 0) return NONE;
        if (x.contains("FROZEN_OBSERVATION_MISSING") || x.contains("NO_PREDRAW")) return EXCLUDED_NO_PREDRAW;
        if (x.contains("DRAW_CONFLICT") || x.contains("DRAW_HASH_MISMATCH")) return BLOCKED_DRAW_CONFLICT;
        if (x.contains("PIT_") || x.contains("FIRST_WRITE_CONFLICT") || x.contains("TARGET_ISSUE_MISMATCH")) return BLOCKED_PIT_CONFLICT;
        if (x.contains("OOM") || x.contains("MEMORY")) return TRANSIENT_MEMORY_PRESSURE;
        if (x.contains("LOCKED") || x.contains("BUSY") || x.contains("TIMEOUT") || x.contains("SQLITE") || x.contains("TX_FAILED") || x.contains("WRITE_COORDINATION")) return TRANSIENT_DB_BUSY;
        if (x.contains("INVALID_DRAW") || x.contains("DRAW_INVALID")) return INVALID_DRAW;
        if (x.contains("HASH") || x.contains("INTEGRITY") || x.contains("MULTIPLE_REAL")) return PERMANENT_INTEGRITY;
        return UNKNOWN;
    }

    public boolean retryable() {
        return this == TRANSIENT_DB_BUSY || this == TRANSIENT_MEMORY_PRESSURE || this == UNKNOWN;
    }
}
