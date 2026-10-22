package com.ethk3.novaonepro;

/**
 * Immutable boundary for the optional V51 offline residual model.
 *
 * <p>This release deliberately contains no trained residual coefficients: no signed PIT/OOS
 * artifact satisfying the production evidence contract exists in the repository.  The former
 * ten-line zero holder looked like an unfinished shell and offered no way to distinguish an
 * intentionally unavailable model from a corrupt export.  This class now owns the complete
 * shape, provenance, validation, fingerprint and fail-closed availability contract.  A future
 * exporter must replace all metadata and coefficients atomically; setting {@link #PROVEN} alone
 * can never activate the expert.</p>
 */
public final class V51OfflineResidualArtifact {
    private V51OfflineResidualArtifact() {}

    public static final String SCHEMA_VERSION = "V51_OFFLINE_RESIDUAL_V2";
    public static final String ARTIFACT_ID = "V51_OFFLINE_RESIDUAL_NOT_TRAINED_R919";
    public static final String DATASET_HASH = "";
    public static final String PROTOCOL_HASH = "";
    public static final String TRAINED_UNTIL_ISSUE = "";
    public static final String EVIDENCE_HASH = "";
    public static final int TRAINING_ROWS = 0;
    public static final int FUTURE_OOS_N = 0;
    public static final double FUTURE_WILSON_LOWER = 0.0d;
    public static final double FUTURE_BRIER = 1.0d;
    public static final double FUTURE_ECE = 1.0d;
    private static final boolean EXPORTED_PROVEN_FLAG = false;
    public static final double[] WEIGHT = new double[V51TrajectoryFeatureEngine.FEATURE_COUNT];
    public static final double[][] POSITION_NUMBER_BIAS = new double[10][10];
    public static final boolean PROVEN = EXPORTED_PROVEN_FLAG && evidenceContractValid();

    /** The expert may contribute only when both evidence and serialized coefficients are valid. */
    public static boolean available() {
        return PROVEN;
    }

    private static boolean evidenceContractValid() {
        return TRAINING_ROWS >= 240
                && FUTURE_OOS_N >= 240
                && FUTURE_WILSON_LOWER > 0.30d
                && FUTURE_BRIER >= 0.0d && FUTURE_BRIER < 1.0d
                && FUTURE_ECE >= 0.0d && FUTURE_ECE <= 0.12d
                && TargetIssueResolver.isValid(TRAINED_UNTIL_ISSUE)
                && isHash(DATASET_HASH)
                && isHash(PROTOCOL_HASH)
                && isHash(EVIDENCE_HASH)
                && shapeValid()
                && finiteCoefficients()
                && coefficientNonZeroCount() > 0;
    }

    public static String status() {
        if (available()) return "AVAILABLE_SIGNED_PIT_OOS";
        if (!EXPORTED_PROVEN_FLAG) return "NOT_TRAINED_NO_SIGNED_PIT_OOS_ARTIFACT";
        if (!shapeValid()) return "REJECTED_SHAPE";
        if (!finiteCoefficients()) return "REJECTED_NON_FINITE";
        if (coefficientNonZeroCount() == 0) return "REJECTED_EMPTY_COEFFICIENTS";
        if (FUTURE_OOS_N < 240) return "REJECTED_FUTURE_OOS_N";
        if (FUTURE_ECE > 0.12d) return "REJECTED_CALIBRATION";
        return "REJECTED_EVIDENCE_OR_PROVENANCE";
    }

    /** Deterministic identity used by diagnostics and future export parity tests. */
    public static String fingerprint() {
        StringBuilder canonical = new StringBuilder(4096)
                .append(SCHEMA_VERSION).append('|').append(ARTIFACT_ID)
                .append('|').append(DATASET_HASH).append('|').append(PROTOCOL_HASH)
                .append('|').append(TRAINED_UNTIL_ISSUE).append('|').append(EVIDENCE_HASH)
                .append('|').append(TRAINING_ROWS).append('|').append(FUTURE_OOS_N)
                .append('|').append(Long.toHexString(Double.doubleToLongBits(FUTURE_WILSON_LOWER)))
                .append('|').append(Long.toHexString(Double.doubleToLongBits(FUTURE_BRIER)))
                .append('|').append(Long.toHexString(Double.doubleToLongBits(FUTURE_ECE)))
                .append('|').append(PROVEN);
        for (double value : WEIGHT) canonical.append('|').append(hex(value));
        for (double[] row : POSITION_NUMBER_BIAS) {
            canonical.append("|ROW");
            if (row != null) for (double value : row) canonical.append('|').append(hex(value));
        }
        return V51ChecksumEngine.sha256(canonical.toString());
    }

    public static int coefficientNonZeroCount() {
        int count = 0;
        for (double value : WEIGHT) if (Double.isFinite(value) && value != 0.0d) count++;
        for (double[] row : POSITION_NUMBER_BIAS) if (row != null) {
            for (double value : row) if (Double.isFinite(value) && value != 0.0d) count++;
        }
        return count;
    }

    public static boolean shapeValid() {
        if (WEIGHT == null || WEIGHT.length != V51TrajectoryFeatureEngine.FEATURE_COUNT) return false;
        if (POSITION_NUMBER_BIAS == null || POSITION_NUMBER_BIAS.length != 10) return false;
        for (double[] row : POSITION_NUMBER_BIAS) if (row == null || row.length != 10) return false;
        return true;
    }

    private static boolean finiteCoefficients() {
        for (double value : WEIGHT) if (!Double.isFinite(value)) return false;
        for (double[] row : POSITION_NUMBER_BIAS) {
            if (row == null) return false;
            for (double value : row) if (!Double.isFinite(value)) return false;
        }
        return true;
    }

    private static boolean isHash(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }

    private static String hex(double value) {
        return Long.toHexString(Double.doubleToLongBits(value));
    }
}
