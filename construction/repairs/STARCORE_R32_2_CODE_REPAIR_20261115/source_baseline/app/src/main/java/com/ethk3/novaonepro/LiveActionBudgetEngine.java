package com.ethk3.novaonepro;

/**
 * V42 single owner for bounded REAL_PROBE pacing.
 *
 * <p>This class never creates BET and never changes probability. Unlike V41 it counts only settled
 * REAL_PROBE actions against the probe budget; NORMAL and QUALIFIED_FLOOR are never miscounted as
 * probes.
 */
public final class LiveActionBudgetEngine {
    public static final String V46_ROLE = "BACKGROUND_FREQUENCY_OBSERVER_NO_QUALITY_OVERRIDE";

    public static final String MODULE_VERSION = AppConfig.V42_LIVE_ACTION_BUDGET_VERSION;

    public static final class State {
        public boolean allowed;
        public int observed50;
        public int used50;
        public int cap50;
        public int gap;
        public String mode = "BOOTSTRAP";
        public String code = "BUDGET_NOT_EVALUATED";

        public String summary() {
            return "allowed="
                    + (allowed ? 1 : 0)
                    + "|used50="
                    + used50
                    + "/"
                    + cap50
                    + "|observed50="
                    + observed50
                    + "|gap="
                    + gap
                    + "|mode="
                    + safe(mode)
                    + "|code="
                    + safe(code);
        }
    }

    private LiveActionBudgetEngine() {}

    public static State evaluate(DbStore store) {
        return evaluate(RiskCoverageController.evaluate(store));
    }

    public static State evaluate(RiskCoverageController.State rc) {
        State out = new State();
        Models.FrequencyState fs = rc == null ? null : rc.frequency;
        out.observed50 = fs == null ? 0 : Math.max(0, Math.min(50, fs.observed50));
        out.used50 = rc == null ? 0 : Math.max(0, rc.probeUsed50);
        out.cap50 = rc == null ? AppConfig.V42_PROBE_CAP_PER_50 : Math.max(0, rc.probeCap50);
        out.gap =
                fs == null
                        ? 999
                        : ((fs.betCount100 <= 0 && fs.betCount50 <= 0)
                                ? 999
                                : Math.max(0, fs.issuesSinceLastRealBet));
        out.mode = rc == null ? "BOOTSTRAP" : safe(rc.mode);

        if (rc != null && !rc.allowProbe) {
            out.code = "PROBE_RC_DISABLED_" + safe(rc.mode);
            return out;
        }
        int minGap = rc == null ? AppConfig.V42_PROBE_MIN_GAP : Math.max(1, rc.probeMinGap);
        if (out.gap < minGap) {
            out.code = "PROBE_GAP_LOW";
            return out;
        }
        if (out.cap50 <= 0 || out.used50 >= out.cap50) {
            out.code = "PROBE_BUDGET_FULL";
            return out;
        }
        out.allowed = true;
        out.code = "PROBE_BUDGET_READY";
        return out;
    }

    /** Compatibility only for old diagnostics/harnesses. */
    public static State evaluate(Models.FrequencyState fs) {
        RiskCoverageController.State rc = new RiskCoverageController.State();
        rc.frequency = fs == null ? new Models.FrequencyState() : fs;
        rc.probeCap50 = AppConfig.V42_PROBE_CAP_PER_50;
        rc.probeMinGap = AppConfig.V42_PROBE_MIN_GAP;
        rc.allowProbe = true;
        rc.mode = "COMPAT";
        return evaluate(rc);
    }

    public static void persist(DbStore store, State state) {
        if (store == null || state == null) return;
        try {
            store.setState("v42_live_action_budget_version", MODULE_VERSION);
            store.setState("v42_live_action_budget_summary", state.summary());
            store.setState("v42_live_action_budget_allowed", state.allowed ? "1" : "0");
        } catch (Throwable ignore) {
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
