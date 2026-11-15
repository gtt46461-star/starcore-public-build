package com.ethk3.novaonepro;

import java.util.Arrays;

/** Deployable lightweight model artifact. Heavy training remains off-device. */
public final class ModelArtifactManifest {
    public static final String MODULE_VERSION = "V48_MODEL_ARTIFACT_MANIFEST_20260721_48072148";
    public static final String MODEL_ID = "V48_LINEAR_PIT_24F";
    public static final int FEATURE_COUNT = PointInTimeNumberFeatureEngine.FEATURE_COUNT;

    public static final class Artifact {
        public final String id;
        public final String version;
        public final double bias;
        public final double[] weight;

        public Artifact(String id, String version, double bias, double[] weight) {
            this.id = id;
            this.version = version;
            this.bias = bias;
            this.weight = Arrays.copyOf(weight, weight.length);
            if (this.weight.length != FEATURE_COUNT)
                throw new IllegalArgumentException("feature count");
        }

        public String checksum() {
            long h = 1125899906842597L;
            h = 31 * h + id.hashCode();
            h = 31 * h + version.hashCode();
            for (double v : weight) h = 31 * h + Double.doubleToLongBits(v);
            return Long.toHexString(h);
        }
    }

    private static final double[] DEFAULT_WEIGHT =
            new double[] {
                0.00, 0.18, 0.13, 0.10, 0.08, 0.05, 0.03, 0.22, 0.17, 0.05, -0.06, -0.02, 0.08,
                0.04, 0.01, 0.00, 0.01, 0.01, 0.03, 0.02, 0.04, 0.04, 0.03, -0.08
            };

    private ModelArtifactManifest() {}

    public static Artifact current() {
        return new Artifact(MODEL_ID, V48BuildManifest.MODEL_VERSION, -2.20d, DEFAULT_WEIGHT);
    }
}
