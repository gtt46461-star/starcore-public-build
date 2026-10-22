package com.ethk3.novaonepro;

/** Immutable truth with separate draw-task closure and eligible-prediction settlement denominators. */
public final class SettlementCoverageTruthR906 {
    public final int predictedEligible;
    public final int settledPredictions;
    public final int excludedNoPredraw;
    public final int blocked;
    public final int openEligible;
    public final int drawTaskTotal;
    public final int drawTaskTerminal;
    public final int drawTaskOpen;
    public final double settlementCoverage;
    public final double drawTaskClosure;

    /** Compatibility constructor for callers that do not yet own draw-task counts. */
    public SettlementCoverageTruthR906(int predicted, int settled, int excluded, int blocked, int open) {
        this(predicted, settled, excluded, blocked, open, 0, 0, 0);
    }

    public SettlementCoverageTruthR906(
            int predicted,
            int settled,
            int excluded,
            int blocked,
            int open,
            int taskTotal,
            int taskTerminal,
            int taskOpen) {
        predictedEligible = Math.max(0, predicted);
        settledPredictions = Math.max(0, settled);
        excludedNoPredraw = Math.max(0, excluded);
        this.blocked = Math.max(0, blocked);
        openEligible = Math.max(0, open);
        drawTaskTotal = Math.max(0, taskTotal);
        drawTaskTerminal = Math.max(0, Math.min(drawTaskTotal, taskTerminal));
        drawTaskOpen = Math.max(0, taskOpen);
        settlementCoverage = predictedEligible <= 0
                ? 0.0d
                : (double) settledPredictions / predictedEligible;
        drawTaskClosure = drawTaskTotal <= 0
                ? 0.0d
                : (double) drawTaskTerminal / drawTaskTotal;
    }

    public String canonical() {
        return "R914_COVERAGE"
                + "|drawTaskTotal=" + drawTaskTotal
                + "|drawTaskTerminal=" + drawTaskTerminal
                + "|drawTaskOpen=" + drawTaskOpen
                + "|drawTaskClosure=" + drawTaskClosure
                + "|predictedEligible=" + predictedEligible
                + "|settledPredictions=" + settledPredictions
                + "|excludedNoPredraw=" + excludedNoPredraw
                + "|blocked=" + blocked
                + "|openEligible=" + openEligible
                + "|predictionCoverage=" + settlementCoverage;
    }
}
