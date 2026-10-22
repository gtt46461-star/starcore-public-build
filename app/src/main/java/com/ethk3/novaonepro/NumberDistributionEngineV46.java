package com.ethk3.novaonepro;

import java.util.List;
import java.util.Locale;

/**
 * V46 number-first probability owner facade.
 *
 * <p>The underlying SequenceDistributionEngine already produces one normalized ten-number
 * distribution for every position. This facade makes that contract explicit, audits normalization
 * and writes the exact three-number decomposition to each candidate.
 */
public final class NumberDistributionEngineV46 {
    public static final String MODULE_VERSION = "V47_BASE_DISTRIBUTION_FALLBACK_20260721_47072147";

    public static final class Snapshot {
        public SequenceDistributionEngine.Forecast forecast;
        public int historySize;
        public int validPositions;
        public int invalidPositions;
        public double maxNormalizationError;
        public long costMs;
        public String status = "EMPTY";
        public String baseIssue = "";

        public boolean ready() {
            return forecast != null && invalidPositions == 0 && validPositions == 10;
        }

        public String summary() {
            return "status="
                    + status
                    + "|history="
                    + historySize
                    + "|validPositions="
                    + validPositions
                    + "|invalidPositions="
                    + invalidPositions
                    + "|maxNormError="
                    + fmt(maxNormalizationError)
                    + "|costMs="
                    + costMs
                    + "|base="
                    + safe(baseIssue);
        }
    }

    private final SequenceDistributionEngine delegate = new SequenceDistributionEngine();

    public Snapshot build(List<Models.Draw> history) {
        long started = System.currentTimeMillis();
        Snapshot out = new Snapshot();
        out.forecast = delegate.build(history);
        out.historySize = out.forecast == null ? 0 : out.forecast.historySize;
        out.baseIssue = out.forecast == null ? "" : safe(out.forecast.baseIssue);
        if (out.forecast != null) {
            for (int p = 0; p < AppConfig.POSITION_COUNT; p++) {
                double sum = 0.0d;
                boolean valid = true;
                for (int n = 0; n < AppConfig.NUMBER_COUNT; n++) {
                    double v = out.forecast.probability[p][n];
                    if (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0d || v > 1.0d) {
                        valid = false;
                    }
                    sum += finite(v, 0.0d);
                }
                double err = Math.abs(sum - 1.0d);
                out.maxNormalizationError = Math.max(out.maxNormalizationError, err);
                if (valid && err <= 0.000001d) out.validPositions++;
                else out.invalidPositions++;
            }
        }
        out.costMs = System.currentTimeMillis() - started;
        out.status =
                out.ready() ? "READY" : (out.forecast == null ? "EMPTY" : "INVALID_DISTRIBUTION");
        return out;
    }

    public void apply(Snapshot snapshot, Models.ComboCandidate c) {
        if (c == null || !c.isValidK3()) return;
        SequenceDistributionEngine.Forecast f = snapshot == null ? null : snapshot.forecast;
        if (f == null) {
            c.v46EventProbability = AppConfig.RANDOM_K3_BASELINE;
            c.v46NumberDistributionSummary = "V46_NUMBER_DISTRIBUTION_MISSING";
            c.v46Version = AppConfig.V46_VERSION;
            return;
        }
        delegate.applyToCandidate(f, c);
        double p1 = f.numberProbability(c.position, c.n1);
        double p2 = f.numberProbability(c.position, c.n2);
        double p3 = f.numberProbability(c.position, c.n3);
        double event = clamp01(p1 + p2 + p3);
        c.basePBeforeFusion = event;
        c.modelP = event;
        c.rawP = event;
        c.v46EventProbability = event;
        c.v46NumberDistributionSummary =
                "P"
                        + c.position
                        + "["
                        + two(c.n1)
                        + "="
                        + pct(p1)
                        + ","
                        + two(c.n2)
                        + "="
                        + pct(p2)
                        + ","
                        + two(c.n3)
                        + "="
                        + pct(p3)
                        + "]"
                        + "|K3="
                        + pct(event)
                        + "|Norm="
                        + (snapshot.ready() ? "OK" : "ERROR")
                        + "|History="
                        + snapshot.historySize
                        + "|Regime="
                        + regime(f, c.position);
        c.v46ProbabilityTrace = append(c.v46ProbabilityTrace, "NumberDistribution=" + pct(event));
        c.v46Version = AppConfig.V46_VERSION;
        c.probabilityMode = "V46_NUMBER_FIRST_K3_EVENT";
        c.probabilityFieldVersion = MODULE_VERSION;
    }

    public static double eventProbability(Snapshot snapshot, int position, int n1, int n2, int n3) {
        if (snapshot == null || snapshot.forecast == null) return AppConfig.RANDOM_K3_BASELINE;
        return snapshot.forecast.comboProbability(position, n1, n2, n3);
    }

    private static String regime(SequenceDistributionEngine.Forecast f, int position) {
        int p = position - 1;
        return p >= 0 && p < f.regimeKey.length ? safe(f.regimeKey[p]) : "";
    }

    private static double finite(double v, double d) {
        return Double.isNaN(v) || Double.isInfinite(v) ? d : v;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String append(String a, String b) {
        if (safe(a).length() == 0) return safe(b);
        if (safe(b).length() == 0 || a.indexOf(b) >= 0) return safe(a);
        return a + "|" + b;
    }

    private static String two(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private static String pct(double v) {
        return String.format(Locale.US, "%.2f%%", 100.0d * v);
    }

    private static String fmt(double v) {
        return String.format(Locale.US, "%.8f", v);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
