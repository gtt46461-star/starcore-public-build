package com.ethk3.novaonepro;

import java.util.Map;

/** Precompiled generic SHADOW runtime for bounded formulas, linear and FTRL artifacts. */
public final class ResearchDslCandidateRuntimeR811 {
    private ResearchDslCandidateRuntimeR811() {}

    public static Evaluation evaluateFormulaCandidate(
            String candidateId, String formula, Map<String, Double> features) {
        if (RealtimePressureGateR811.hasActiveCriticalWork()) return Evaluation.yield(candidateId);
        ResearchStrategyDslEngine.Node ast = ResearchStrategyDslEngine.parse(formula);
        double score = ResearchStrategyDslEngine.evaluate(ast, features);
        return Evaluation.ok(candidateId, score, ResearchStrategyDslEngine.hash(ast));
    }

    public static Evaluation evaluateLinearCandidate(
            String candidateId, String[] featureNames, double[] weights, double bias, Map<String, Double> features) {
        if (RealtimePressureGateR811.hasActiveCriticalWork()) return Evaluation.yield(candidateId);
        if (featureNames == null || weights == null || featureNames.length != weights.length)
            throw new IllegalArgumentException("R811_LINEAR_SHAPE");
        double score = finite(bias);
        StringBuilder canonical = new StringBuilder("LINEAR|").append(Long.toHexString(Double.doubleToLongBits(bias)));
        for (int i = 0; i < weights.length; i++) {
            String name = featureNames[i] == null ? "" : featureNames[i];
            Double value = features == null ? null : features.get(name);
            score += finite(weights[i]) * finite(value == null ? 0.0d : value.doubleValue());
            canonical.append('|').append(name).append(':').append(Long.toHexString(Double.doubleToLongBits(weights[i])));
        }
        return Evaluation.ok(candidateId, score, V51ChecksumEngine.sha256(canonical.toString()));
    }

    /** Exact FTRL-Proximal inference: w_i=0 when |z_i|<=l1, else standard closed form. */
    public static Evaluation evaluateFtrlCandidate(
            String candidateId,
            double[] x,
            double[] z,
            double[] n,
            double alpha,
            double beta,
            double l1,
            double l2) {
        if (RealtimePressureGateR811.hasActiveCriticalWork()) return Evaluation.yield(candidateId);
        if (x == null || z == null || n == null || x.length != z.length || x.length != n.length)
            throw new IllegalArgumentException("R811_FTRL_SHAPE");
        if (!(alpha > 0.0d) || beta < 0.0d || l1 < 0.0d || l2 < 0.0d)
            throw new IllegalArgumentException("R811_FTRL_HYPERPARAM");
        double logit = 0.0d;
        StringBuilder canonical = new StringBuilder("FTRL|").append(x.length);
        for (int i = 0; i < x.length; i++) {
            double sign = z[i] < 0.0d ? -1.0d : 1.0d;
            double w = Math.abs(z[i]) <= l1 ? 0.0d : -(z[i] - sign * l1) / ((beta + Math.sqrt(Math.max(0.0d, n[i]))) / alpha + l2);
            logit += w * finite(x[i]);
            canonical.append('|').append(Long.toHexString(Double.doubleToLongBits(w)));
        }
        double probability = sigmoid(logit);
        return Evaluation.ok(candidateId, probability, V51ChecksumEngine.sha256(canonical.toString()));
    }

    public static final class Evaluation {
        public final String candidateId, status, artifactHash;
        public final double score;
        private Evaluation(String id, String status, double score, String hash) {
            candidateId = id == null ? "" : id; this.status = status; this.score = score; artifactHash = hash;
        }
        static Evaluation ok(String id, double score, String hash) { return new Evaluation(id, "SHADOW_ONLY", finite(score), hash); }
        static Evaluation yield(String id) { return new Evaluation(id, "YIELD_REALTIME", 0.0d, ""); }
    }

    private static double sigmoid(double x) {
        if (x >= 0.0d) return 1.0d / (1.0d + Math.exp(-Math.min(60.0d, x)));
        double e = Math.exp(Math.max(-60.0d, x)); return e / (1.0d + e);
    }
    private static double finite(double x) { return Double.isNaN(x) || Double.isInfinite(x) ? 0.0d : x; }
}
