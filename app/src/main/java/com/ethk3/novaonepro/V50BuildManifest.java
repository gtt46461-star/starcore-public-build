package com.ethk3.novaonepro;

/**
 * Immutable build identity for V50 PRIME.
 *
 * <p>Engineering completion and model proof are deliberately separate. A build may compile and
 * migrate successfully while MODEL_PROVEN remains false. In that state DecisionGateEngine must not
 * treat the artifact as a proven release.
 */
public final class V50BuildManifest {
    private V50BuildManifest() {}

    public static final String VERSION_NAME = "V50.0.50072403";
    public static final int VERSION_CODE = 500500725;

    public static final String PRODUCT_NAME = "V50 PRIME";
    public static final String FORMAL_NAME =
            "Point-in-Time Full-Information State-Causal Selective Action System";
    public static final String STRATEGY_VERSION =
            "V50_PRIME_PIT_FULL_INFO_STATE_CAUSAL_SELECTIVE_ACTION_20260724_50072403";

    public static final String BUILD_ID = "NOVA_BUILD_ID_V50_PRIME_50072403";
    public static final String COHORT_ID = "NOVA_COHORT_V50_PRIME_50072403";
    public static final String SCHEMA_ID = "NOVA_SCHEMA_V50_PRIME_1_10_100_50072403";
    public static final String FEATURE_SCHEMA = "V50_FEATURE_CONTRACT_PIT_50072403";
    public static final String STATE_SCHEMA = "V50_STATE_TRAJECTORY_50072403";
    public static final String EXPERT_SCHEMA = "V50_EXPERT_EVIDENCE_50072403";
    public static final String ACTION_SCHEMA = "V50_TEN_POSITION_ACTION_50072403";

    /* Artifacts are populated only by verified training/export jobs. */
    public static final String MODEL_ARTIFACT_ID = V50NumberModelArtifact.MODEL_ID;
    public static final String RANK_ARTIFACT_ID = V50ActionRankerArtifact.ARTIFACT_ID;
    public static final String CALIBRATION_ARTIFACT_ID = V50SelectedCalibrationArtifact.ARTIFACT_ID;
    public static final String POLICY_ARTIFACT_ID = V50SelectivePolicyArtifact.ARTIFACT_ID;

    public static final String NUMBER_FEATURE_OWNER = "V50NumberFeatureEngine";
    public static final String STATE_OWNER = "V50StateTrajectoryEngine";
    public static final String NUMBER_PROBABILITY_OWNER = "V50MixtureOfExpertsEngine";
    public static final String ACTION_BUILDER_OWNER = "V50PositionActionBuilder";
    public static final String ACTION_RANK_OWNER = "V50ActionRankerEngine";
    public static final String SELECTED_PROBABILITY_OWNER = "V50SelectedActionCalibrationEngine";
    public static final String DOMAIN_CERTIFICATE_OWNER = "V50DomainCertificateEngine";
    public static final String LANE_OWNER = "LanePolicyCompiler";
    public static final String AUTHORIZATION_OWNER = "DecisionAuthorizationEngine";
    public static final String BET_OWNER = "DecisionGateEngine";
    public static final String SETTLEMENT_OWNER = "SettlementEngine";

    public static final boolean ENGINEERING_COMPLETE = false;
    public static final boolean MODEL_PROVEN = V50SelectivePolicyArtifact.MODEL_GATE_PASSED;
    public static final boolean REALTIME_PROVEN = false;
    public static final String RELEASE_STATE =
            !ENGINEERING_COMPLETE
                    ? "V50_ENGINEERING_IN_PROGRESS_MODEL_NOT_PROVEN"
                    : MODEL_PROVEN
                            ? "V50_ENGINEERING_COMPLETE_MODEL_GATE_PASSED_REALTIME_NOT_PROVEN"
                            : "V50_ENGINEERING_COMPLETE_MODEL_NOT_PROVEN";

    public static String fingerprint() {
        return VERSION_NAME
                + '|'
                + STRATEGY_VERSION
                + '|'
                + BUILD_ID
                + '|'
                + COHORT_ID
                + '|'
                + SCHEMA_ID
                + '|'
                + FEATURE_SCHEMA
                + '|'
                + STATE_SCHEMA
                + '|'
                + EXPERT_SCHEMA
                + '|'
                + ACTION_SCHEMA
                + '|'
                + MODEL_ARTIFACT_ID
                + '|'
                + RANK_ARTIFACT_ID
                + '|'
                + CALIBRATION_ARTIFACT_ID
                + '|'
                + POLICY_ARTIFACT_ID;
    }

    public static boolean mayLabelFullRelease() {
        return ENGINEERING_COMPLETE && MODEL_PROVEN && REALTIME_PROVEN;
    }
}
