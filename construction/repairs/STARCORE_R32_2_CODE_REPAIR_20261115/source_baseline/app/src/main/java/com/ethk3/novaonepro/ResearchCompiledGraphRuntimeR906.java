package com.ethk3.novaonepro;

import java.util.Arrays;
import java.util.Locale;

/**
 * Deterministic light-asset runtime over one immutable shared 1200xF matrix.
 *
 * <p>R906 intentionally supports several semantically different executable families instead of
 * relabeling every candidate as a different formula. All families are SHADOW research only. They
 * never create a BET, never read post-draw labels, and all parameters are derived from the frozen
 * method signature so Android/Python parity can be tested byte-for-byte.</p>
 */
public final class ResearchCompiledGraphRuntimeR906 {
    public static final int MATRIX_FEATURES = 7;
    public static final String MATRIX_SCHEMA =
            "raw_action_p|rank_score|third_margin|entropy_risk|stability|window_consensus|assignment_support";

    public enum Family {
        LINEAR_GRAPH,
        BAYESIAN_TRANSITION,
        METRIC_RETRIEVAL,
        COMPACT_FOREST,
        HISTORICAL_SYNTHESIS,
        TYPED_EXPRESSION
    }

    private ResearchCompiledGraphRuntimeR906() {}

    public static Compiled compile(ResearchMethodPlanR906 p) {
        if (p == null || !p.runtimeExecutable)
            return Compiled.waiting(p == null ? "PLAN_NULL" : "HEAVY_OFFLINE_ASSET");
        if(p.representation.equals("TYPED_EXPRESSION_R932")){
            TypedExpressionR932.Program expression=TypedExpressionR932.compile(p.feature);
            return new Compiled(true,"TYPED_PIT_BYTECODE_READY",Family.TYPED_EXPRESSION,ResearchMethodSignatureR906.of(p),new double[0],expression);
        }
        String rep = upper(p.representation), learner = upper(p.learner);
        Family family;
        if (rep.contains("HISTORICAL_SYNTHESIS") || learner.contains("EMPIRICAL_SYMBOLIC_PROGRAM")) {
            family = Family.HISTORICAL_SYNTHESIS;
        } else if (rep.contains("BAYES") || learner.contains("BAYES") || learner.contains("MARKOV")) {
            family = Family.BAYESIAN_TRANSITION;
        } else if (rep.contains("RETRIEVAL") || learner.contains("RETRIEVAL") || learner.contains("KNN")) {
            family = Family.METRIC_RETRIEVAL;
        } else if (rep.contains("FOREST") || learner.contains("FOREST")) {
            family = Family.COMPACT_FOREST;
        } else if (learner.contains("LINEAR") || learner.contains("RULE") || learner.contains("RESIDUAL")
                || learner.contains("FTRL") || learner.contains("WEAVE") || rep.contains("GRAPH")
                || rep.contains("TRANSPORT") || rep.contains("MOTIF") || rep.contains("EVIDENCE")) {
            family = Family.LINEAR_GRAPH;
        } else {
            return Compiled.waiting("WAITING_OFFLINE_TRAINER");
        }
        String methodHash = ResearchMethodSignatureR906.of(p);
        double[] params = parameters(methodHash, family);
        return new Compiled(true, "PARITY_PASS", family, methodHash, params);
    }

    public static double[] evaluateAll(Compiled c, double[][] matrix) {
        if (c == null || !c.executable || matrix == null) return new double[0];
        double[] out = new double[matrix.length];
        for (int r = 0; r < matrix.length; r++) out[r] = evaluate(c, matrix[r]);
        return out;
    }

    public static double evaluate(Compiled c, double[] x) {
        if (c == null || !c.executable) return 0.0d;
        double[] v = canonicalVector(x);
        switch (c.family) {
            case TYPED_EXPRESSION: return c.expression.evaluate(x);
            case BAYESIAN_TRANSITION:
                return bayesian(v, c.params);
            case METRIC_RETRIEVAL:
                return retrieval(v, c.params);
            case COMPACT_FOREST:
                return forest(v, c.params);
            case HISTORICAL_SYNTHESIS:
                // History-dependent programs require the shared PIT Profile and are evaluated by
                // HistoricalStrategyInventionR908 from the Research sidecar batch.
                return 0.0d;
            case LINEAR_GRAPH:
            default:
                return linear(v, c.params);
        }
    }

    /** Stable 7-feature order shared by the research sidecar and certified mainline overlay. */
    public static double[] canonicalVector(V51Models.PositionAction a) {
        if (a == null) return new double[MATRIX_FEATURES];
        return new double[] {
            finite(a.rawActionP),
            finite(a.rankScore),
            finite(a.thirdMargin),
            clamp01(a.entropy),
            clamp01(a.stability),
            clamp01(meta(a.metaFeatures, 20)),
            clamp01(meta(a.metaFeatures, 15))
        };
    }

    private static double linear(double[] x, double[] p) {
        double z = p[0];
        for (int i = 0; i < MATRIX_FEATURES; i++) z += p[i + 1] * standardized(i, x[i]);
        return sigmoid(z);
    }

    /**
     * A bounded log-odds Bayesian update: the immutable raw K3 probability is the prior, while
     * cross-position/window/stability evidence contributes shrinkage-limited likelihood lifts.
     */
    private static double bayesian(double[] x, double[] p) {
        double prior = clamp(x[0], 0.02d, 0.98d);
        double logOdds = Math.log(prior / (1.0d - prior));
        double evidence =
                p[0] * centered01(x[6])
                        + p[1] * centered01(x[5])
                        + p[2] * clamp(x[2] * 12.0d, -1.0d, 1.0d)
                        + p[3] * centered01(x[4])
                        - Math.abs(p[4]) * centered01(x[3]);
        double pseudoCount = 4.0d + Math.abs(p[5]) * 12.0d;
        double shrink = pseudoCount / (pseudoCount + 18.0d);
        return sigmoid(logOdds + shrink * evidence);
    }

    /**
     * Metric-retrieval family. Parameters encode a frozen centroid, feature scales and local prior;
     * score is inverse-distance similarity rather than a linear projection.
     */
    private static double retrieval(double[] x, double[] p) {
        double d2 = 0.0d;
        for (int i = 0; i < MATRIX_FEATURES; i++) {
            double center = p[i];
            double scale = 0.30d + 0.35d * Math.abs(p[MATRIX_FEATURES + i]);
            double d = (normalized(i, x[i]) - center) / scale;
            d2 += d * d;
        }
        double similarity = 1.0d / (1.0d + Math.sqrt(d2 / MATRIX_FEATURES));
        double prior = clamp(x[0], 0.0d, 1.0d);
        return clamp01(0.60d * similarity + 0.30d * prior + 0.10d * clamp01(x[4]));
    }

    /** Small deterministic threshold ensemble; unlike the linear family it models interactions. */
    private static double forest(double[] x, double[] p) {
        int votes = 0, trees = 9;
        for (int t = 0; t < trees; t++) {
            int a = ((int) Math.abs(Math.round(p[t] * 1000.0d)) + t) % MATRIX_FEATURES;
            int b = (a + 1 + t * 2) % MATRIX_FEATURES;
            double ta = clamp01(0.50d + 0.22d * p[9 + t]);
            double tb = clamp01(0.50d - 0.18d * p[18 + t]);
            boolean left = normalized(a, x[a]) >= ta;
            boolean right = normalized(b, x[b]) >= tb;
            if ((left && right) || (left && t % 3 == 0) || (right && t % 4 == 0)) votes++;
        }
        double ensemble = (votes + 1.0d) / (trees + 2.0d);
        return clamp01(0.70d * ensemble + 0.30d * clamp01(x[0]));
    }

    private static double[] parameters(String methodHash, Family family) {
        if (family == Family.HISTORICAL_SYNTHESIS) return new double[0];
        int n = family == Family.METRIC_RETRIEVAL ? 14 : family == Family.COMPACT_FOREST ? 27 : 8;
        double[] p = new double[n];
        long s = seed(methodHash + '|' + family.name());
        for (int i = 0; i < p.length; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            p[i] = (((s >>> 16) & 0xffffL) / 65535.0d) * 2.0d - 1.0d;
        }
        if (family == Family.METRIC_RETRIEVAL) {
            for (int i = 0; i < 7; i++) p[i] = clamp01(0.5d + p[i] * 0.30d);
        }
        return p;
    }

    private static double[] canonicalVector(double[] x) {
        double[] out = new double[MATRIX_FEATURES];
        if (x != null) for (int i = 0; i < out.length && i < x.length; i++) out[i] = finite(x[i]);
        out[0] = clamp01(out[0]);
        out[3] = clamp01(out[3]);
        out[4] = clamp01(out[4]);
        out[5] = clamp01(out[5]);
        out[6] = clamp01(out[6]);
        return out;
    }

    private static double standardized(int i, double v) {
        if (i == 1) return Math.tanh(v / 4.0d);
        if (i == 2) return Math.tanh(v * 12.0d);
        return centered01(clamp01(v));
    }

    private static double normalized(int i, double v) {
        if (i == 1) return clamp01(0.5d + 0.5d * Math.tanh(v / 4.0d));
        if (i == 2) return clamp01(0.5d + 0.5d * Math.tanh(v * 12.0d));
        return clamp01(v);
    }

    private static double centered01(double x) { return (clamp01(x) - 0.5d) * 2.0d; }
    private static double sigmoid(double z) { return 1.0d / (1.0d + Math.exp(-clamp(z, -20.0d, 20.0d))); }
    private static double finite(double x) { return Double.isNaN(x) || Double.isInfinite(x) ? 0.0d : x; }
    private static double clamp01(double x) { return clamp(finite(x), 0.0d, 1.0d); }
    private static double clamp(double x, double lo, double hi) { return Math.max(lo, Math.min(hi, x)); }
    private static double meta(double[] x, int i) { return x != null && i >= 0 && i < x.length ? finite(x[i]) : 0.0d; }
    private static String upper(String x) { return x == null ? "" : x.trim().toUpperCase(Locale.US); }
    private static long seed(String x) {
        String s = x == null ? "" : x;
        long h = 1125899906842597L;
        for (int i = 0; i < s.length(); i++) h = 31L * h + s.charAt(i);
        return h;
    }

    public static final class Compiled {
        public final boolean executable;
        public final String status, artifactHash, methodHash;
        public final Family family;
        public final double[] params;
        /** Alias retained for older R906 callers/tests. */
        public final double[] weights;
        public final TypedExpressionR932.Program expression;

        Compiled(boolean e, String s, Family f, String h, double[] p) {this(e,s,f,h,p,null);}
        Compiled(boolean e, String s, Family f, String h, double[] p,TypedExpressionR932.Program expression) {
            this.expression=expression;
            executable = e;
            status = s == null ? "" : s;
            family = f == null ? Family.LINEAR_GRAPH : f;
            methodHash = h == null ? "" : h;
            params = p == null ? new double[0] : p.clone();
            weights = params.clone();
            artifactHash = expression!=null?V51ChecksumEngine.sha256("R932_TYPED_METHOD|"+MATRIX_SCHEMA+'|'+expression.hash+'|'+methodHash):e
                    ? V51ChecksumEngine.sha256(
                            "R906_RUNTIME|schema=" + MATRIX_SCHEMA + "|family=" + family.name()
                                    + "|method=" + methodHash + "|params=" + Arrays.toString(params))
                    : "";
        }

        static Compiled waiting(String s) {
            return new Compiled(false, s, Family.LINEAR_GRAPH, "", null);
        }
    }
}
