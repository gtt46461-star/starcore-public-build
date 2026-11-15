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
    /** R31-B03-C03#A3: prediction presence over draws that had to be predicted (predicted + excluded for no
     * pre-draw freeze). Separate from settlement work health; NaN when there is no eligible draw yet. */
    public final double predictionPresenceCoverage;

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
        int presenceDenominator = predictedEligible + excludedNoPredraw;
        predictionPresenceCoverage = presenceDenominator <= 0
                ? Double.NaN
                : (double) predictedEligible / presenceDenominator;
    }

    /** Settlement work health (draw tasks closed) and prediction coverage (draws that had a pre-draw freeze)
     * are two independent columns; a healthy settlement queue never implies healthy prediction coverage. */
    public String predictionPresenceText() {
        int denominator = predictedEligible + excludedNoPredraw;
        return denominator <= 0 ? "UNKNOWN_NO_ELIGIBLE_DRAW" : predictedEligible + "/" + denominator + "(缺" + excludedNoPredraw + ")";
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
