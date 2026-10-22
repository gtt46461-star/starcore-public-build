package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Legacy V49 observer-only logarithmic opinion pool.
 *
 * <p>V50 experts deliberately implement only V50NumberExpert so they cannot be pulled back into
 * this mutable legacy runtime. This compatibility observer therefore retains only the three frozen
 * V49 NumberExpert implementations.
 */
public final class MixtureOfExpertsGateEngine {
    public static final String MODULE_VERSION = V49BuildManifest.EXPERT_SCHEMA;
    private final ArrayList<NumberExpert> experts = new ArrayList<NumberExpert>();
    private final V49DirichletCalibrationEngine calibration = new V49DirichletCalibrationEngine();

    public MixtureOfExpertsGateEngine() {
        experts.add(new ContextTreeNumberEngine());
        experts.add(new MotifMatrixProfileEngine());
        experts.add(new RelationTensorEngine());
    }

    public FusionResult evaluate(DbStore store, List<Models.Draw> history, V49FeatureSnapshot f) {
        FusionResult out = new FusionResult();
        NumberExpert.Context context = new NumberExpert.Context(store, history, f);
        for (int i = 0; i < experts.size(); i++) {
            try {
                NumberExpert.Result r = experts.get(i).evaluate(context);
                if (r != null && r.valid) {
                    r.normalize();
                    out.experts.add(r);
                }
            } catch (Throwable ignore) {
            }
        }
        if (out.experts.size() == 0) return out;
        for (int p = 0; p < 10; p++) {
            double[] base = stateWeights(f.family[p], f.lifecycle[p]);
            double[] w = new double[out.experts.size()];
            double sum = 0.0d;
            for (int e = 0; e < out.experts.size(); e++) {
                NumberExpert.Result r = out.experts.get(e);
                int originalIndex = expertIndex(r.expertName);
                double reliability =
                        V49Repository.expertReliability(store, r.expertName, f.family[p], p + 1);
                double current = r.trust[p] * (1.0d - 0.65d * r.uncertainty[p]);
                double weight =
                        base[Math.max(0, originalIndex)]
                                * (0.45d + 0.55d * reliability)
                                * (0.35d + 0.65d * Math.max(0.0d, current));
                if (!r.valid) weight = 0.0d;
                w[e] = Math.max(0.0001d, weight);
                sum += w[e];
            }
            for (int e = 0; e < w.length; e++) w[e] /= Math.max(1.0e-12d, sum);
            double[] logScore = new double[10];
            for (int n = 0; n < 10; n++) {
                for (int e = 0; e < out.experts.size(); e++) {
                    logScore[n] +=
                            w[e]
                                    * FeatureContractRegistry.safeLog(
                                            out.experts.get(e).probability[p][n]);
                }
            }
            double[] pooled = new double[10];
            V49ExpertSupport.softmax(logScore, pooled);
            double[] calibrated = calibration.apply(store, p + 1, pooled);
            for (int n = 0; n < 10; n++) {
                out.rawProbability[p][n] = pooled[n];
                out.probability[p][n] = calibrated[n];
            }
            double divergence = 0.0d;
            for (int e = 0; e < out.experts.size(); e++)
                divergence += w[e] * V49ExpertSupport.js(out.experts.get(e).probability[p], pooled);
            out.agreement[p] = FeatureContractRegistry.clamp01(1.0d - divergence);
            out.uncertainty[p] =
                    FeatureContractRegistry.clamp01(
                            0.48d * FeatureContractRegistry.entropy10(calibrated)
                                    + 0.34d * divergence
                                    + 0.18d * f.switchRisk[p]);
            out.weightSummary[p] = summary(out.experts, w);
        }
        out.valid = true;
        return out;
    }

    private static int expertIndex(String name) {
        if ("HIERARCHICAL_PRIOR".equals(name)) return 0;
        if ("MULTISCALE_TREND".equals(name)) return 1;
        if ("CONTEXT_TREE".equals(name)) return 2;
        if ("STATE_DURATION".equals(name)) return 3;
        if ("GOLDEN_MOTIF".equals(name)) return 4;
        if ("RELATION_TENSOR".equals(name)) return 5;
        if ("TEACHER_POSTERIOR".equals(name)) return 6;
        if ("OFFLINE_RESIDUAL".equals(name)) return 7;
        if ("ONLINE_RESIDUAL".equals(name)) return 8;
        return 0;
    }

    private static double[] stateWeights(String family, String lifecycle) {
        double[] w = new double[] {0.18, 0.16, 0.12, 0.10, 0.10, 0.10, 0.10, 0.08, 0.06};
        if (family == null) family = "";
        if (family.startsWith("HOT") || family.startsWith("COLD")) {
            w[1] += 0.08;
            w[3] += 0.04;
            w[0] -= 0.04;
            w[5] -= 0.02;
        }
        if (family.startsWith("OSCILLATION") || family.contains("ROTATION")) {
            w[2] += 0.06;
            w[5] += 0.05;
            w[1] -= 0.04;
        }
        if (family.contains("MIGRATION") || family.contains("SYNC") || family.contains("SQUEEZE")) {
            w[5] += 0.10;
            w[4] += 0.03;
            w[0] -= 0.04;
        }
        if (family.contains("COMPRESSION") || family.contains("BREAKOUT")) {
            w[4] += 0.08;
            w[3] += 0.04;
            w[1] += 0.03;
        }
        if ("BREAK".equals(lifecycle) || "EXHAUST".equals(lifecycle)) {
            w[3] += 0.09;
            w[4] += 0.04;
            w[1] -= 0.08;
        }
        double sum = 0.0d;
        for (int i = 0; i < w.length; i++) {
            if (w[i] < 0.01d) w[i] = 0.01d;
            sum += w[i];
        }
        for (int i = 0; i < w.length; i++) w[i] /= sum;
        return w;
    }

    private static String summary(List<NumberExpert.Result> r, double[] w) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < r.size(); i++) {
            if (i > 0) b.append('|');
            b.append(r.get(i).expertName)
                    .append('=')
                    .append(String.format(Locale.US, "%.3f", w[i]));
        }
        return b.toString();
    }

    public static final class FusionResult {
        public final double[][] rawProbability = new double[10][10];
        public final double[][] probability = new double[10][10];
        public final double[] agreement = new double[10];
        public final double[] uncertainty = new double[10];
        public final String[] weightSummary = new String[10];
        public final ArrayList<NumberExpert.Result> experts = new ArrayList<NumberExpert.Result>();
        public boolean valid;
    }
}
