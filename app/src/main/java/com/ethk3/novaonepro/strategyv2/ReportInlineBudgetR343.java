package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;

/** A fixed total with protected diagnostic indexes. Bulk evidence cannot starve graph identities. */
final class ReportInlineBudgetR343 {
    static final int TOTAL_BYTES = 768 * 1024;
    private final Map<String, Integer> reserved = new LinkedHashMap<>();
    private int remaining;

    ReportInlineBudgetR343() {
        reserved.put("chartObjectIndex", 16 * 1024);
        reserved.put("repairFrameIndex", 32 * 1024);
        reserved.put("researchCurrentHeadsR342", 32 * 1024);
        reserved.put("researchCurrentActionsR342", 32 * 1024);
        remaining = TOTAL_BYTES;
        for (int bytes : reserved.values()) remaining -= bytes;
    }

    int take(String key, int requestedBytes) {
        if (key == null || requestedBytes < 0) throw new IllegalArgumentException("REPORT_INLINE_BUDGET");
        Integer protectedBytes = reserved.remove(key);
        if (protectedBytes != null) return protectedBytes;
        int allocated = Math.min(remaining, requestedBytes);
        remaining -= allocated;
        return allocated;
    }
}
