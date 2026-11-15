package com.ethk3.novaonepro;

/** Single immutable owner for every active V49 identity token. */
public final class V49BuildManifest {
    private V49BuildManifest() {}

    public static final String VERSION_NAME = "V49.0.49072201";
    public static final int VERSION_CODE = 490490722;
    public static final String STRATEGY_VERSION =
            "V49_FULL_WINDOW_CAUSAL_MOE_SINGLE_TOP3_20260722_49072201";
    public static final String BUILD_ID = "NOVA_BUILD_ID_V49_49072201";
    public static final String COHORT_ID = "NOVA_COHORT_ID_V49_49072201";
    public static final String SCHEMA_VERSION = "NOVA_SCHEMA_V49_49072201";
    public static final String FEATURE_VERSION = "V49_FEATURE_CONTRACT_42F_49072201";
    public static final String FEATURE_SCHEMA = FEATURE_VERSION;
    public static final String STATE_SCHEMA = "V49_WINDOW_FAMILY_LIFECYCLE_49072201";
    public static final String EXPERT_SCHEMA = "V49_NINE_EXPERT_LOG_POOL_49072201";
    public static final String MODEL_VERSION = "V49_CONDITIONAL_SOFTMAX_ARTIFACT_49072201";
    public static final String MODEL_SCHEMA = MODEL_VERSION;
    public static final String CALIBRATION_VERSION = "V49_ONLINE_DIRICHLET_CALIBRATION_49072201";
    public static final String CALIBRATION_SCHEMA = CALIBRATION_VERSION;
    public static final String RANKING_VERSION = "V49_TEN_POSITION_OPPORTUNITY_RANK_49072201";
    public static final String GATE_VERSION = "V49_CORE_DYNAMIC_PROBE_QUANTILE_49072201";
    public static final String LANE_SCHEMA = GATE_VERSION;
    public static final String CONTRACT_VERSION = "V49_PREDRAW_IMMUTABLE_CONTRACT_49072201";
    public static final String AUTH_SCHEMA = "V49_SIGNED_SINGLE_ACTION_AUTH_49072201";
    public static final String OOS_VERSION = "V49_FULL_INFORMATION_ACTION_LOG_49072201";
    public static final String DIAGNOSIS_VERSION = "V49_CAUSE_ABLATION_DIAGNOSIS_49072201";
    public static final String UI_SCHEMA = "V49_SNAPSHOT_LAZY_COLLAPSIBLE_UI_49072201";

    public static final String PIPELINE =
            "PIT_HISTORY->100_NUMBER_FEATURES->WINDOW_STATE->NINE_EXPERTS"
                    + "->LOG_POOL->DIRICHLET->10_POSITION_TOP3->OPPORTUNITY_RANK"
                    + "->CORE_DYNAMIC_PROBE->DECISION_GATE->SIGNED_AUTH";

    public static String fingerprint() {
        return VERSION_NAME
                + '|'
                + STRATEGY_VERSION
                + '|'
                + BUILD_ID
                + '|'
                + COHORT_ID
                + '|'
                + SCHEMA_VERSION
                + '|'
                + FEATURE_VERSION
                + '|'
                + MODEL_VERSION
                + '|'
                + CALIBRATION_VERSION
                + '|'
                + RANKING_VERSION
                + '|'
                + OOS_VERSION;
    }

    public static boolean exact(
            String strategy,
            String build,
            String cohort,
            String schema,
            String feature,
            String model,
            String calibration,
            String ranking,
            String oos) {
        return STRATEGY_VERSION.equals(safe(strategy))
                && BUILD_ID.equals(safe(build))
                && COHORT_ID.equals(safe(cohort))
                && SCHEMA_VERSION.equals(safe(schema))
                && FEATURE_VERSION.equals(safe(feature))
                && MODEL_VERSION.equals(safe(model))
                && CALIBRATION_VERSION.equals(safe(calibration))
                && RANKING_VERSION.equals(safe(ranking))
                && OOS_VERSION.equals(safe(oos));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
