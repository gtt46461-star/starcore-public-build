package com.ethk3.novaonepro;

/** Exact implementation contract for UI-visible pre-draw metrics. */
public final class MetricValueR906 {
    public enum Status { AVAILABLE, UNAVAILABLE, INVALID, STALE }
    public enum Unit { PROBABILITY, SCORE, RANK, TEXT }

    public final Status status;
    public final Unit unit;
    public final double value;
    public final String source;
    public final String targetIssue;
    public final long frozenAt;
    public final String inputHash;
    public final String reasonCode;

    private MetricValueR906(
            Status status, Unit unit, double value, String source, String targetIssue,
            long frozenAt, String inputHash, String reasonCode) {
        if (status == null || unit == null) throw new IllegalArgumentException("R906_METRIC_TYPE");
        if (status == Status.AVAILABLE && (Double.isNaN(value) || Double.isInfinite(value)))
            throw new IllegalArgumentException("R906_METRIC_NONFINITE");
        if (status != Status.AVAILABLE && safe(reasonCode).length() == 0)
            throw new IllegalArgumentException("R906_METRIC_REASON_REQUIRED");
        this.status = status;
        this.unit = unit;
        this.value = value;
        this.source = safe(source);
        this.targetIssue = safe(targetIssue);
        this.frozenAt = Math.max(0L, frozenAt);
        this.inputHash = safe(inputHash);
        this.reasonCode = safe(reasonCode);
    }

    public static MetricValueR906 available(
            Unit unit, double value, String source, String targetIssue, long frozenAt, String hash) {
        return new MetricValueR906(
                Status.AVAILABLE, unit, value, source, targetIssue, frozenAt, hash, "");
    }

    public static MetricValueR906 unavailable(
            Unit unit, String source, String targetIssue, String reasonCode) {
        return new MetricValueR906(
                Status.UNAVAILABLE, unit, Double.NaN, source, targetIssue, 0L, "", reasonCode);
    }

    public boolean availableProbability() {
        return status == Status.AVAILABLE
                && unit == Unit.PROBABILITY
                && value >= 0.0d
                && value <= 1.0d;
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
}

