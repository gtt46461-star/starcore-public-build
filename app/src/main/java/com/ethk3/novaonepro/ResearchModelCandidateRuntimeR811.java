package com.ethk3.novaonepro;

/** Deterministic precompiled TinyMLP/attention/calibration inference; Research/SHADOW only. */
public final class ResearchModelCandidateRuntimeR811 {
    public static final int INPUT_D = 96;
    public static final int HIDDEN_1 = 64;
    public static final int HIDDEN_2 = 32;
    public static final int OUTPUT_LOGITS = 100;
    private ResearchModelCandidateRuntimeR811() {}

    public static double[] inferTinyMlp(
            double[] input, double[][] hiddenWeights, double[] hiddenBias, double[][] outputWeights, double[] outputBias) {
        if (RealtimePressureGateR811.isUnderPressure()) return new double[0];
        double[] hidden = dense(input, hiddenWeights, hiddenBias, true);
        return softmax(dense(hidden, outputWeights, outputBias, false));
    }

    /** Locked deep challenger architecture D→64→32→100; output is 10 row-wise distributions. */
    public static double[] inferTinyMlp96x64x32x100(
            double[] input,
            double[][] weight1,
            double[] bias1,
            double[][] weight2,
            double[] bias2,
            double[][] weight3,
            double[] bias3) {
        if (RealtimePressureGateR811.isUnderPressure()) return new double[0];
        if (input == null || input.length != INPUT_D)
            throw new IllegalArgumentException("R811_MLP_INPUT_D96");
        double[] logits = tinyMlpLogits(input, weight1, bias1, weight2, bias2, weight3, bias3);
        double[] probability = new double[OUTPUT_LOGITS];
        for (int position = 0; position < 10; position++) {
            double[] row = new double[10];
            System.arraycopy(logits, position * 10, row, 0, 10);
            double[] normalized = softmax(row);
            System.arraycopy(normalized, 0, probability, position * 10, 10);
        }
        return probability;
    }

    /** Loads the canonical frozen artifact and projects its 10x10 logits through Sinkhorn. */
    public static NeuralInference inferTinyMlpArtifact(
            ResearchArtifactCodecR811.Artifact artifact,
            double[] input,
            String targetIssue,
            double temperature) {
        if (RealtimePressureGateR811.isUnderPressure()) return NeuralInference.yield();
        if (artifact == null || !"TINY_MLP_96_64_32_100".equals(artifact.type))
            throw new IllegalArgumentException("R811_MLP_ARTIFACT_TYPE");
        if (input == null || input.length != INPUT_D)
            throw new IllegalArgumentException("R811_MLP_INPUT_D96");
        if (!TargetIssueResolver.isValid(artifact.trainedUntilIssue)
                || !TargetIssueResolver.isValid(targetIssue)
                || TargetIssueResolver.compare(artifact.trainedUntilIssue, targetIssue) >= 0)
            throw new IllegalArgumentException("R811_MLP_TRAINED_UNTIL_PIT");
        double[][] w1 = matrix(field(artifact, "W1", HIDDEN_1 * INPUT_D), HIDDEN_1, INPUT_D);
        double[][] w2 = matrix(field(artifact, "W2", HIDDEN_2 * HIDDEN_1), HIDDEN_2, HIDDEN_1);
        double[][] w3 = matrix(field(artifact, "W3", OUTPUT_LOGITS * HIDDEN_2), OUTPUT_LOGITS, HIDDEN_2);
        double[] mean = field(artifact, "feature_mean", INPUT_D);
        double[] scale = field(artifact, "feature_scale", INPUT_D);
        double[] normalizedInput = new double[INPUT_D];
        for (int i = 0; i < INPUT_D; i++) {
            if (!(scale[i] > 0.0d))
                throw new IllegalArgumentException("R811_MLP_FEATURE_SCALE");
            normalizedInput[i] = (finite(input[i]) - mean[i]) / scale[i];
        }
        double[] logits =
                tinyMlpLogits(
                        normalizedInput,
                        w1,
                        field(artifact, "b1", HIDDEN_1),
                        w2,
                        field(artifact, "b2", HIDDEN_2),
                        w3,
                        field(artifact, "b3", OUTPUT_LOGITS));
        double[][] score = matrix(logits, 10, 10);
        V51SinkhornEngine.ProjectionResult projection =
                new V51SinkhornEngine().projectDetailed(score, temperature, 240, 1e-10d);
        String hash = ResearchArtifactCodecR811.hash(artifact);
        return new NeuralInference(
                "SHADOW_ONLY",
                projection,
                hash,
                artifact.trainedUntilIssue,
                projection.productionValid());
    }

    private static double[] tinyMlpLogits(
            double[] input,
            double[][] weight1,
            double[] bias1,
            double[][] weight2,
            double[] bias2,
            double[][] weight3,
            double[] bias3) {
        if (input == null || input.length != INPUT_D)
            throw new IllegalArgumentException("R811_MLP_INPUT_D96");
        double[] hidden1 = dense(input, weight1, bias1, true);
        if (hidden1.length != HIDDEN_1)
            throw new IllegalArgumentException("R811_MLP_HIDDEN_64");
        double[] hidden2 = dense(hidden1, weight2, bias2, true);
        if (hidden2.length != HIDDEN_2)
            throw new IllegalArgumentException("R811_MLP_HIDDEN_32");
        double[] logits = dense(hidden2, weight3, bias3, false);
        if (logits.length != OUTPUT_LOGITS)
            throw new IllegalArgumentException("R811_MLP_OUTPUT_100");
        return logits;
    }

    public static double[] inferAttention(double[][] sequence, double[] query) {
        if (RealtimePressureGateR811.isUnderPressure()) return new double[0];
        if (sequence == null || sequence.length == 0 || query == null) return new double[0];
        double[] score = new double[sequence.length];
        for (int i = 0; i < sequence.length; i++) score[i] = dot(sequence[i], query) / Math.sqrt(Math.max(1, query.length));
        double[] attention = softmax(score), out = new double[query.length];
        for (int i = 0; i < sequence.length; i++)
            for (int j = 0; j < out.length && j < sequence[i].length; j++) out[j] += attention[i] * finite(sequence[i][j]);
        return out;
    }

    public static double inferCalibration(double logit, double temperature, double bias) {
        if (RealtimePressureGateR811.isUnderPressure()) return -1.0d;
        if (!(temperature > 0.0d)) throw new IllegalArgumentException("R811_CALIBRATION_TEMPERATURE");
        double x = finite(logit) / temperature + finite(bias);
        return x >= 0.0d ? 1.0d / (1.0d + Math.exp(-Math.min(60.0d, x))) : Math.exp(Math.max(-60.0d, x)) / (1.0d + Math.exp(Math.max(-60.0d, x)));
    }

    public static double[] xavierUniform(int fanIn, int fanOut, int count, long seed) {
        if (fanIn <= 0 || fanOut <= 0 || count < 0)
            throw new IllegalArgumentException("R811_XAVIER_SHAPE");
        double limit = Math.sqrt(6.0d / (fanIn + fanOut));
        ResearchDeterministicRngR811 rng = new ResearchDeterministicRngR811(seed);
        double[] weights = new double[count];
        for (int i = 0; i < count; i++)
            weights[i] = (rng.nextDouble() * 2.0d - 1.0d) * limit;
        return weights;
    }

    /** Exact Adam constants from the locked plan, with global gradient norm clipping at 5.0. */
    public static AdamUpdate adamStep(
            double[] weights,
            double[] gradients,
            double[] firstMoment,
            double[] secondMoment,
            int step,
            double learningRate) {
        if (RealtimePressureGateR811.isUnderPressure()) return AdamUpdate.yield();
        if (weights == null
                || gradients == null
                || firstMoment == null
                || secondMoment == null
                || weights.length != gradients.length
                || weights.length != firstMoment.length
                || weights.length != secondMoment.length
                || step <= 0
                || !allowedLearningRate(learningRate))
            throw new IllegalArgumentException("R811_ADAM_INPUT");
        double normSquared = 0.0d;
        for (int i = 0; i < gradients.length; i++) {
            if (!isFinite(weights[i])
                    || !isFinite(gradients[i])
                    || !isFinite(firstMoment[i])
                    || !isFinite(secondMoment[i]))
                throw new IllegalArgumentException("R811_ADAM_NONFINITE");
            normSquared += gradients[i] * gradients[i];
        }
        double norm = Math.sqrt(normSquared);
        double scale = norm > 5.0d ? 5.0d / norm : 1.0d;
        double[] nextWeights = weights.clone();
        double[] nextFirst = firstMoment.clone();
        double[] nextSecond = secondMoment.clone();
        double firstCorrection = 1.0d - Math.pow(0.9d, step);
        double secondCorrection = 1.0d - Math.pow(0.999d, step);
        for (int i = 0; i < weights.length; i++) {
            double gradient = gradients[i] * scale;
            nextFirst[i] = 0.9d * firstMoment[i] + 0.1d * gradient;
            nextSecond[i] = 0.999d * secondMoment[i] + 0.001d * gradient * gradient;
            double firstHat = nextFirst[i] / firstCorrection;
            double secondHat = nextSecond[i] / secondCorrection;
            nextWeights[i] =
                    weights[i]
                            - learningRate * firstHat / (Math.sqrt(secondHat) + 1e-8d);
        }
        return new AdamUpdate(
                "RESEARCH_UPDATED",
                nextWeights,
                nextFirst,
                nextSecond,
                norm,
                scale,
                V51ChecksumEngine.hashDoubles(
                        new double[][] {nextWeights, nextFirst, nextSecond}));
    }

    public static double permutationCrossEntropy(
            double[][] probability,
            int[] actualPermutation,
            double lambda2,
            double[] weights) {
        if (probability == null
                || probability.length != 10
                || actualPermutation == null
                || actualPermutation.length != 10
                || !(lambda2 == 0.0d
                        || lambda2 == 1e-6d
                        || lambda2 == 1e-5d
                        || lambda2 == 1e-4d))
            throw new IllegalArgumentException("R811_NEURAL_LOSS_INPUT");
        boolean[] seen = new boolean[11];
        double loss = 0.0d;
        for (int position = 0; position < 10; position++) {
            if (probability[position] == null || probability[position].length != 10)
                throw new IllegalArgumentException("R811_NEURAL_LOSS_SHAPE");
            int actual = actualPermutation[position];
            if (actual < 1 || actual > 10 || seen[actual])
                throw new IllegalArgumentException("R811_NEURAL_LOSS_PERMUTATION");
            seen[actual] = true;
            double value = probability[position][actual - 1];
            if (!isFinite(value) || value < 0.0d || value > 1.0d)
                throw new IllegalArgumentException("R811_NEURAL_LOSS_PROBABILITY");
            loss -= Math.log(Math.max(1e-15d, value));
        }
        if (weights != null)
            for (int i = 0; i < weights.length; i++) {
                if (!isFinite(weights[i]))
                    throw new IllegalArgumentException("R811_NEURAL_LOSS_WEIGHT");
                loss += lambda2 * weights[i] * weights[i];
            }
        return loss;
    }

    private static double[] dense(double[] x, double[][] w, double[] b, boolean relu) {
        if (x == null || w == null || b == null || w.length != b.length) throw new IllegalArgumentException("R811_MLP_SHAPE");
        double[] out = new double[w.length];
        for (int i = 0; i < w.length; i++) {
            if (w[i] == null || w[i].length != x.length) throw new IllegalArgumentException("R811_MLP_ROW");
            out[i] = finite(b[i]) + dot(w[i], x);
            if (relu && out[i] < 0.0d) out[i] = 0.0d;
        }
        return out;
    }
    private static double dot(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) throw new IllegalArgumentException("R811_DOT_SHAPE");
        double out = 0.0d; for (int i = 0; i < a.length; i++) out += finite(a[i]) * finite(b[i]); return out;
    }
    private static double[] softmax(double[] x) {
        if (x == null || x.length == 0) return new double[0];
        double max = -Double.MAX_VALUE; for (int i = 0; i < x.length; i++) max = Math.max(max, finite(x[i]));
        double[] out = new double[x.length]; double sum = 0.0d;
        for (int i = 0; i < x.length; i++) { out[i] = Math.exp(Math.max(-60.0d, Math.min(60.0d, finite(x[i]) - max))); sum += out[i]; }
        for (int i = 0; i < out.length; i++) out[i] /= sum; return out;
    }
    private static boolean allowedLearningRate(double value) {
        return value == 1e-4d || value == 3e-4d || value == 1e-3d;
    }
    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
    private static double finite(double x) { return Double.isNaN(x) || Double.isInfinite(x) ? 0.0d : x; }

    private static double[] field(
            ResearchArtifactCodecR811.Artifact artifact, String name, int length) {
        double[] values = artifact.fields.get(name);
        if (values == null || values.length != length)
            throw new IllegalArgumentException("R811_MLP_ARTIFACT_FIELD_" + name);
        return values;
    }

    private static double[][] matrix(double[] flat, int rows, int columns) {
        if (flat == null || flat.length != rows * columns)
            throw new IllegalArgumentException("R811_MLP_MATRIX_SHAPE");
        double[][] output = new double[rows][columns];
        for (int row = 0; row < rows; row++)
            System.arraycopy(flat, row * columns, output[row], 0, columns);
        return output;
    }

    public static final class NeuralInference {
        public final String status, artifactHash, trainedUntilIssue;
        public final V51SinkhornEngine.ProjectionResult projection;
        public final boolean finiteAndConverged;

        NeuralInference(
                String status,
                V51SinkhornEngine.ProjectionResult projection,
                String hash,
                String trained,
                boolean valid) {
            this.status = status;
            this.projection = projection;
            artifactHash = hash;
            trainedUntilIssue = trained;
            finiteAndConverged = valid;
        }

        static NeuralInference yield() {
            return new NeuralInference("YIELD_REALTIME", null, "", "", false);
        }
    }

    public static final class AdamUpdate {
        public final String status, artifactHash;
        public final double[] weights, firstMoment, secondMoment;
        public final double gradientNorm, gradientScale;

        AdamUpdate(
                String status,
                double[] weights,
                double[] firstMoment,
                double[] secondMoment,
                double gradientNorm,
                double gradientScale,
                String artifactHash) {
            this.status = status;
            this.weights = weights.clone();
            this.firstMoment = firstMoment.clone();
            this.secondMoment = secondMoment.clone();
            this.gradientNorm = gradientNorm;
            this.gradientScale = gradientScale;
            this.artifactHash = artifactHash;
        }

        static AdamUpdate yield() {
            return new AdamUpdate(
                    "YIELD_REALTIME",
                    new double[0],
                    new double[0],
                    new double[0],
                    0.0d,
                    0.0d,
                    "");
        }
    }
}
