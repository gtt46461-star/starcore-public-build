package com.ethk3.novaonepro;

import java.util.Locale;
import java.util.Map;

/**
 * One-place, statistically honest view of controlled REAL opportunity frequency.
 *
 * <p>The 15% value is an operational observation target, never a quota.  This diagnostic exposes
 * small/medium/long windows, a Wilson interval and the dominant rejection path so a future gate
 * proposal can be tested on strictly later OOS data without back-filling or frequency debt.</p>
 */
public final class ControlledFrequencyDiagnosticR923 {
    public static final String VERSION = "R923_CONTROLLED_FREQUENCY_DIAGNOSTIC_V1";

    private ControlledFrequencyDiagnosticR923() {}

    public static Report capture(DbStore store) {
        return new Report(
                ControlledCandidateOpportunityAuditR922.capture(store, 30),
                ControlledCandidateOpportunityAuditR922.capture(store, 120),
                ControlledCandidateOpportunityAuditR922.capture(store, 1200));
    }

    static Interval wilson(int successes, int sample) {
        if (sample <= 0) return new Interval(-1.0d, -1.0d);
        int hit = Math.max(0, Math.min(sample, successes));
        double z = 1.959963984540054d;
        double p = hit / (double) sample;
        double z2 = z * z;
        double denominator = 1.0d + z2 / sample;
        double center = p + z2 / (2.0d * sample);
        double radius = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * sample)) / sample);
        return new Interval(
                clamp((center - radius) / denominator),
                clamp((center + radius) / denominator));
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) return 0.0d;
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private static String percent(double value) {
        return value < 0.0d ? "N/A" : String.format(Locale.US, "%.2f%%", value * 100.0d);
    }

    private static String dominant(Map<String, Integer> distribution) {
        String key = "NONE";
        int maximum = 0;
        if (distribution != null) {
            for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
                int value = entry.getValue() == null ? 0 : entry.getValue().intValue();
                if (value > maximum) {
                    maximum = value;
                    key = entry.getKey() == null ? "UNKNOWN" : entry.getKey();
                }
            }
        }
        return key + ':' + maximum;
    }

    private static String line(ControlledCandidateOpportunityAuditR922.Report report) {
        if (report == null) return "REPORT_NULL";
        Interval interval = wilson(report.bets, report.opportunities);
        double gap = report.frequency < 0.0d
                ? -1.0d : report.frequency - ControlledCandidateOpportunityAuditR922.OBSERVATION_TARGET;
        return "window=" + report.requestedWindow
                + "|sample=" + report.opportunities
                + "|BET=" + report.bets
                + "|frequency=" + report.frequencyText()
                + "|Wilson95=" + percent(interval.lower) + ".." + percent(interval.upper)
                + "|targetGap=" + (report.frequency < 0.0d ? "N/A"
                        : (gap < -1.0e-12d ? "-" : "+") + percent(Math.abs(gap)))
                + "|sampleState="
                + (report.opportunities >= report.requestedWindow ? "FULL_WINDOW" : "PARTIAL_WINDOW")
                + "|targetState=" + report.targetState
                + "|dominantReject=" + dominant(report.rejectDistribution)
                + "|read=" + report.readStatus;
    }

    public static final class Interval {
        public final double lower, upper;
        Interval(double lower, double upper) {
            this.lower = lower;
            this.upper = upper;
        }
    }

    public static final class Report {
        public final ControlledCandidateOpportunityAuditR922.Report recent30, recent120, recent1200;

        Report(
                ControlledCandidateOpportunityAuditR922.Report shortWindow,
                ControlledCandidateOpportunityAuditR922.Report mediumWindow,
                ControlledCandidateOpportunityAuditR922.Report longWindow) {
            recent30 = shortWindow;
            recent120 = mediumWindow;
            recent1200 = longWindow;
        }

        public String summary() {
            return VERSION
                    + "\n" + line(recent30)
                    + "\n" + line(recent120)
                    + "\n" + line(recent1200)
                    + "\nOperationalTarget=15.00%|Enforcement=FUTURE_OOS_GATE_PROPOSAL_ONLY"
                    + "|Quota=FORBIDDEN|FrequencyDebt=FORBIDDEN|Chase=FORBIDDEN"
                    + "\nInterpretation=低于15%时定位主拒绝路径并在影子分支提出门策略；"
                    + "只有严格未来OOS非劣且校准/分块/Regime/最长连挂同时通过才可发布。";
        }
    }

public static Report captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        return new Report(
                ControlledCandidateOpportunityAuditR922.captureSnapshotCP4(report, store, 30),
                ControlledCandidateOpportunityAuditR922.captureSnapshotCP4(report, store, 120),
                ControlledCandidateOpportunityAuditR922.captureSnapshotCP4(report, store, 1200));
    }
}
