package com.ethk3.novaonepro;

/** Deterministic, evidence-bounded failure attribution for shadow research outcomes. */
public final class ResearchFailureAttributionR815 {
    public enum Category {
        CALIBRATION,
        RANKING,
        COVERAGE,
        SYSTEM_AVAILABILITY,
        SCHEDULER,
        REGIME,
        DATA_QUALITY,
        EXECUTOR,
        UNKNOWN
    }

    public static final class Evidence {
        public final double brier;
        public final double logLoss;
        public final int rank;
        public final int actualRank;
        public final double coverage;
        public final boolean historyClean;
        public final boolean executorSucceeded;
        public final String regime;

        public Evidence(
                double brier,
                double logLoss,
                int rank,
                int actualRank,
                double coverage,
                boolean historyClean,
                boolean executorSucceeded,
                String regime) {
            this.brier = finite(brier);
            this.logLoss = finite(logLoss);
            this.rank = rank;
            this.actualRank = actualRank;
            this.coverage = finite(coverage);
            this.historyClean = historyClean;
            this.executorSucceeded = executorSucceeded;
            this.regime = safe(regime);
        }
    }

    public static final class Result {
        public final Category primary;
        public final Category secondary;
        public final double confidence;
        public final String mutationHint;
        public final String checksum;

        Result(Category primary, Category secondary, double confidence, String hint) {
            this.primary = primary;
            this.secondary = secondary;
            this.confidence = Math.max(0.0d, Math.min(1.0d, confidence));
            this.mutationHint = safe(hint);
            checksum = V51ChecksumEngine.sha256(primary.name() + '|' + secondary.name() + '|' + confidence + '|' + mutationHint);
        }
    }

    private ResearchFailureAttributionR815() {}

    public static Result attribute(Evidence evidence) {
        if (evidence == null) return new Result(Category.UNKNOWN, Category.UNKNOWN, 0.0d, "INSUFFICIENT_EVIDENCE");
        if (!evidence.historyClean)
            return new Result(Category.DATA_QUALITY, Category.UNKNOWN, 1.0d, "REJECT_UNCLEAN_HISTORY");
        if (!evidence.executorSucceeded)
            return new Result(Category.EXECUTOR, Category.UNKNOWN, 1.0d, "REJECT_EXECUTOR_FAILURE");
        if (evidence.coverage < 0.30d)
            return new Result(Category.COVERAGE, Category.UNKNOWN, 0.25d, "REVIEW_SYSTEM_SCHEDULING_EXECUTION_DENOMINATORS_NO_QUALITY_PRUNE");
        if (evidence.actualRank > 0 && evidence.actualRank > Math.max(12, evidence.rank + 8))
            return new Result(Category.RANKING, Category.REGIME, 0.75d, "REVIEW_RANK_FEATURES");
        if (evidence.brier > 0.30d || evidence.logLoss > 1.25d)
            return new Result(Category.CALIBRATION, Category.REGIME, 0.70d, "RECALIBRATE_ON_FROZEN_SPLIT");
        if (evidence.regime.length() == 0)
            return new Result(Category.REGIME, Category.UNKNOWN, 0.40d, "COLLECT_REGIME_EVIDENCE");
        return new Result(Category.UNKNOWN, Category.REGIME, 0.25d, "NO_AGGRESSIVE_MUTATION");
    }

    private static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? Double.NaN : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
