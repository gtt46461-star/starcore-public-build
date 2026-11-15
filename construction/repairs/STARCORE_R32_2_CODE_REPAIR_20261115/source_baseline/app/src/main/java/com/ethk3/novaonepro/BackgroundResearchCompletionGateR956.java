package com.ethk3.novaonepro;

/**
 * R956 fail-closed gate for the autonomous research-owner completion heartbeat.
 *
 * <p>R955 separated attempt time from progress time, but the owner still refreshed the progress
 * clock after a continuity failure or a phase that returned a cooperative/error status as data.
 * This gate makes a completed-progress heartbeat conditional on actual continuity work plus a
 * non-failing phase outcome. It owns no Production decision and cannot authorize REAL.
 */
public final class BackgroundResearchCompletionGateR956 {
    public static final String VERSION = "R956_RESEARCH_OWNER_COMPLETION_V1";
    public static final boolean MAY_AUTHORIZE_REAL = false;

    private BackgroundResearchCompletionGateR956() {}

    public static Decision evaluate(
            boolean continuitySuccess,
            int completedLanes,
            int errorLanes,
            String phaseResult,
            String bootstrapStatus,
            String sidecarStatus,
            String evolutionStatus) {
        if (MAY_AUTHORIZE_REAL) throw new IllegalStateException("R956_REAL_BOUNDARY");
        if (!continuitySuccess)
            return Decision.no("CONTINUITY_NOT_SUCCESS");
        if (completedLanes <= 0)
            return Decision.no("CONTINUITY_NO_COMPLETED_LANE");
        if (errorLanes > 0)
            return Decision.no("CONTINUITY_LANE_ERROR:" + errorLanes);
        String bad = failure("PHASE", phaseResult);
        if (bad.length() > 0) return Decision.no(bad);
        bad = optionalFailure("BOOTSTRAP", bootstrapStatus);
        if (bad.length() > 0) return Decision.no(bad);
        bad = optionalFailure("SIDECAR", sidecarStatus);
        if (bad.length() > 0) return Decision.no(bad);
        bad = optionalFailure("EVOLUTION", evolutionStatus);
        if (bad.length() > 0) return Decision.no(bad);
        return Decision.yes("USEFUL_OWNER_COMPLETION");
    }

    private static String optionalFailure(String lane, String value) {
        String x = safe(value);
        if (x.length() == 0 || "NOT_THIS_PHASE".equalsIgnoreCase(x)) return "";
        return failure(lane, x);
    }

    private static String failure(String lane, String value) {
        String x = safe(value).toUpperCase(java.util.Locale.US);
        if (x.length() == 0) return lane + "_EMPTY";
        if (x.indexOf("YIELD") >= 0) return lane + "_YIELD";
        if (x.indexOf("ERROR:") >= 0 || x.startsWith("ERROR")) return lane + "_ERROR";
        if (x.indexOf("FAILED") >= 0 || x.indexOf("COMMIT_FAILED") >= 0) return lane + "_FAILED";
        if (x.indexOf("|SUCCESS=0") >= 0 || x.startsWith("SUCCESS=0")) return lane + "_SUCCESS_FALSE";
        return "";
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Decision {
        public final boolean progress;
        public final String reason;

        private Decision(boolean progress, String reason) {
            this.progress = progress;
            this.reason = safe(reason);
        }

        static Decision yes(String reason) { return new Decision(true, reason); }
        static Decision no(String reason) { return new Decision(false, reason); }

        public String canonical() {
            return VERSION + "|progress=" + (progress ? 1 : 0) + "|reason=" + reason
                    + "|directREAL=0";
        }
    }
}
