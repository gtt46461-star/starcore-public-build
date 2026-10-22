package com.ethk3.novaonepro;

/**
 * Single immutable owner for every live V48 identity token. Compatibility aliases may expose these
 * values, but no other class may invent a current build/cohort/schema/model identity.
 */
public final class V48BuildManifest {
    public static final String VERSION_NAME = "V48.0.48072148";
    public static final String STRATEGY_VERSION =
            "V48_POINT_IN_TIME_MODEL_LAB_SINGLE_ACTION_CHAMPION_FULL_DIAGNOSIS_20260721_48072148";
    public static final String BUILD_ID = "NOVA_BUILD_ID_V48_48072148";
    public static final String COHORT_ID = "NOVA_COHORT_ID_V48_48072148";
    public static final String SCHEMA_VERSION = "NOVA_SCHEMA_VERSION_V48_48072148";
    public static final String FEATURE_VERSION = "NOVA_FEATURE_SCHEMA_VERSION_V48_48072148";
    public static final String MODEL_VERSION = "NOVA_POINT_IN_TIME_MODEL_V48_48072148";
    public static final String CALIBRATION_VERSION =
            "NOVA_RANK_CONDITIONAL_CALIBRATION_V48_48072148";
    public static final String RANKING_VERSION = "NOVA_SINGLE_ACTION_RANKING_V48_48072148";
    public static final String GATE_VERSION = "NOVA_SINGLE_LANE_GATE_V48_48072148";
    public static final String CONTRACT_VERSION = "NOVA_PREDICTION_CONTRACT_V48_48072148";
    public static final String OOS_VERSION = "NOVA_OOS_ACTION_LOG_V48_48072148";
    public static final String DIAGNOSIS_VERSION = "NOVA_DIAGNOSIS_V48_48072148";

    private V48BuildManifest() {}

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
