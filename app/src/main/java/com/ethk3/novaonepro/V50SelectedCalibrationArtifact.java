package com.ethk3.novaonepro;

/** Generated selection-conditioned isotonic calibration artifact. */
public final class V50SelectedCalibrationArtifact {
    private V50SelectedCalibrationArtifact() {}

    public static final String ARTIFACT_ID = "V50_SELECTED_RANK1_ISOTONIC";
    public static final String DATA_SHA256 =
            "f2c22c332cf43a573d77be4484b3cfa42db5251188d2558545d0baee9e112acd";
    public static final String PARAMETER_SHA256 =
            "ea8843e2e11b8a4bfac30d90a18ad112d50b6b5c08ca94440693101a6bea95e7";
    public static final String OOS_METRICS_JSON =
            "{\"folds\":[{\"train\":1664,\"test\":624,\"rate\":0.3092948717948718,\"ece\":0.016027239092500308},{\"train\":2288,\"test\":624,\"rate\":0.27724358974358976,\"ece\":0.03477913191042584},{\"train\":2912,\"test\":624,\"rate\":0.296474358974359,\"ece\":0.15345845371126315},{\"train\":3536,\"test\":624,\"rate\":0.3333333333333333,\"ece\":0.015479047540126745}],\"oos_count\":2496,\"oos_rate\":0.30408653846153844,\"oos_ece\":0.04436452438969472}";
    public static final double OOS_ECE = 0.044364524389694723d;
    public static final double OOS_HIT_RATE = 0.30408653846153844d;
    public static final double[] X =
            new double[] {
                0.11157300000000001,
                0.111635,
                0.116757,
                0.117038,
                0.219916,
                0.21992400000000001,
                0.39354299999999998,
                0.39363700000000001,
                0.41887200000000002,
                0.41908299999999998,
                0.48432799999999998
            };
    public static final double[] Y =
            new double[] {
                0.01,
                0.10000000000000001,
                0.10000000000000001,
                0.29551993423756689,
                0.29551993423756689,
                0.313927781871776,
                0.313927781871776,
                0.32800000000000001,
                0.32800000000000001,
                0.34862385321100919,
                0.34862385321100919
            };
}
