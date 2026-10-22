package com.ethk3.novaonepro;

/** Java 17 lightweight inference for an externally trained linear artifact. */
public final class LightweightModelInferenceEngine {
    public static final String MODULE_VERSION = "V48_LIGHTWEIGHT_MODEL_INFERENCE_20260721_48072148";

    public static final class Snapshot {
        public final double[][] probability = new double[10][10];
        public String modelId = "";
        public String artifactChecksum = "";
        public String featureSummary = "";
    }

    private LightweightModelInferenceEngine() {}

    public static Snapshot predict(
            PointInTimeNumberFeatureEngine.Snapshot features,
            ModelArtifactManifest.Artifact artifact) {
        Snapshot out = new Snapshot();
        if (features == null || artifact == null) {
            uniform(out);
            return out;
        }
        out.modelId = artifact.id;
        out.artifactChecksum = artifact.checksum();
        out.featureSummary = features.summary();
        for (int p = 0; p < 10; p++) {
            double max = -Double.MAX_VALUE;
            double[] score = new double[10];
            for (int n = 0; n < 10; n++) {
                double z = artifact.bias;
                double[] f = features.feature[p][n];
                for (int k = 0; k < artifact.weight.length; k++) z += artifact.weight[k] * f[k];
                score[n] = z;
                if (z > max) max = z;
            }
            double sum = 0;
            for (int n = 0; n < 10; n++) {
                score[n] = Math.exp(Math.max(-30.0d, Math.min(30.0d, score[n] - max)));
                sum += score[n];
            }
            for (int n = 0; n < 10; n++) out.probability[p][n] = sum <= 0 ? 0.10d : score[n] / sum;
        }
        return out;
    }

    public static double eventProbability(Snapshot snapshot, Models.ComboCandidate candidate) {
        if (snapshot == null || candidate == null || !candidate.isValidK3()) return 0.30d;
        int p = candidate.position - 1;
        return clamp(
                snapshot.probability[p][candidate.n1 - 1]
                        + snapshot.probability[p][candidate.n2 - 1]
                        + snapshot.probability[p][candidate.n3 - 1],
                0.03d,
                0.80d);
    }

    private static void uniform(Snapshot s) {
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) s.probability[p][n] = 0.10d;
        s.modelId = "UNIFORM";
    }

    private static double clamp(double v, double lo, double hi) {
        return !Double.isNaN(v) && !Double.isInfinite(v) ? Math.max(lo, Math.min(hi, v)) : lo;
    }
}
