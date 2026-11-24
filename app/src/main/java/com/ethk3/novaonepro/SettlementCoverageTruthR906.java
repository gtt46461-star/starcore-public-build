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
    public final String sourceStatus, sourceReason, fromIssue, throughIssue;
    public final Integer historicalSettledOutsideWindow;
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
        this(predicted,settled,excluded,blocked,open,taskTotal,taskTerminal,taskOpen,null,null,null,"");
    }

    SettlementCoverageTruthR906(int predicted,int settled,int excluded,int blocked,int open,int taskTotal,
            int taskTerminal,int taskOpen,String from,String through,Integer historical,String reason) {
        predictedEligible = Math.max(0, predicted);
        settledPredictions = Math.max(0, settled);
        excludedNoPredraw = Math.max(0, excluded);
        this.blocked = Math.max(0, blocked);
        openEligible = Math.max(0, open);
        drawTaskTotal = Math.max(0, taskTotal);
        drawTaskTerminal = Math.max(0, taskTerminal);
        drawTaskOpen = Math.max(0, taskOpen);
        fromIssue=from;throughIssue=through;historicalSettledOutsideWindow=historical;
        sourceStatus=!reason.isEmpty()?"SOURCE_UNAVAILABLE":settled>predicted||taskTerminal>taskTotal?"DATA_CONFLICT":"READ_OK";
        sourceReason="DATA_CONFLICT".equals(sourceStatus)?"COUNTS_OUTSIDE_SHARED_DENOMINATOR":reason;
        settlementCoverage = predictedEligible <= 0 || !"READ_OK".equals(sourceStatus)
                ? Double.NaN
                : (double) settledPredictions / predictedEligible;
        drawTaskClosure = drawTaskTotal <= 0 || !"READ_OK".equals(sourceStatus)
                ? Double.NaN
                : (double) drawTaskTerminal / drawTaskTotal;
        int presenceDenominator = predictedEligible + excludedNoPredraw;
        predictionPresenceCoverage = presenceDenominator <= 0
                ? Double.NaN
                : (double) predictedEligible / presenceDenominator;
    }

    static SettlementCoverageTruthR906 unavailable(String reason) {
        return new SettlementCoverageTruthR906(0,0,0,0,0,0,0,0,null,null,null,reason);
    }

    /** Settlement work health (draw tasks closed) and prediction coverage (draws that had a pre-draw freeze)
     * are two independent columns; a healthy settlement queue never implies healthy prediction coverage. */
    public String predictionPresenceText() {
        int denominator = predictedEligible + excludedNoPredraw;
        return denominator <= 0 ? "UNKNOWN_NO_ELIGIBLE_DRAW" : predictedEligible + "/" + denominator + "(缺" + excludedNoPredraw + ")";
    }

    public String canonical() {
        if("SOURCE_UNAVAILABLE".equals(sourceStatus))return "R914_COVERAGE|sourceStatus="+sourceStatus+"|sourceReason="+sourceReason;
        return "R914_COVERAGE"
                + "|sourceStatus=" + sourceStatus + "|sourceReason=" + sourceReason
                + "|window=" + fromIssue + ".." + throughIssue
                + "|historicalSettledOutsideWindow=" + historicalSettledOutsideWindow
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
