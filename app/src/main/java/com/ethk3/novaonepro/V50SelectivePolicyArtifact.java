package com.ethk3.novaonepro;

/** Generated strict-OOS selective risk policy artifact. */
public final class V50SelectivePolicyArtifact {
    private V50SelectivePolicyArtifact() {}

    public static final String ARTIFACT_ID = "V50_SELECTIVE_RISK_POLICY_PIT";
    public static final String DATA_SHA256 =
            "f2c22c332cf43a573d77be4484b3cfa42db5251188d2558545d0baee9e112acd";
    public static final String OOS_REPORT_JSON =
            "{\"strict_point_in_time\":true,\"group_unit\":\"target_issue\",\"folds\":[{\"train\":998,\"test\":374,\"surface_status\":\"NO_QUALITY_SURFACE\",\"selected_p_threshold\":1.0,\"rank_score_tie_threshold\":1.7976931348623157e+308,\"training_selected\":0,\"training_coverage\":0.0,\"training_hit_rate\":0.0,\"training_ece\":1.0,\"training_block_min\":0.0,\"selected\":0,\"coverage\":0.0,\"hits\":0,\"rate\":0.0,\"ece\":1.0},{\"train\":1372,\"test\":375,\"surface_status\":\"NO_QUALITY_SURFACE\",\"selected_p_threshold\":1.0,\"rank_score_tie_threshold\":1.7976931348623157e+308,\"training_selected\":0,\"training_coverage\":0.0,\"training_hit_rate\":0.0,\"training_ece\":1.0,\"training_block_min\":0.0,\"selected\":0,\"coverage\":0.0,\"hits\":0,\"rate\":0.0,\"ece\":1.0},{\"train\":1747,\"test\":374,\"surface_status\":\"NO_QUALITY_SURFACE\",\"selected_p_threshold\":1.0,\"rank_score_tie_threshold\":1.7976931348623157e+308,\"training_selected\":0,\"training_coverage\":0.0,\"training_hit_rate\":0.0,\"training_ece\":1.0,\"training_block_min\":0.0,\"selected\":0,\"coverage\":0.0,\"hits\":0,\"rate\":0.0,\"ece\":1.0},{\"train\":2121,\"test\":375,\"surface_status\":\"NO_QUALITY_SURFACE\",\"selected_p_threshold\":1.0,\"rank_score_tie_threshold\":1.7976931348623157e+308,\"training_selected\":0,\"training_coverage\":0.0,\"training_hit_rate\":0.0,\"training_ece\":1.0,\"training_block_min\":0.0,\"selected\":0,\"coverage\":0.0,\"hits\":0,\"rate\":0.0,\"ece\":1.0}],\"eligible_issues\":2496,\"selected\":0,\"hits\":0,\"coverage\":0.0,\"hit_rate\":0.0,\"ece\":1.0,\"four_blocks\":[],\"rolling_300\":{\"window\":300,\"selected\":0,\"windows\":0,\"latest_rate\":0.0,\"minimum_rate\":0.0,\"maximum_rate\":0.0,\"target_passed\":false},\"rank1_rate\":0.30432692307692305,\"rank2_rate\":0.3019230769230769,\"rank3_rate\":0.30264423076923075,\"rank_stable_folds\":1,\"rank_fold_count\":4,\"deployment_surface\":{\"status\":\"NO_QUALITY_SURFACE\",\"selected_p_threshold\":1.0,\"rank_score_tie_threshold\":1.7976931348623157e+308,\"training_selected\":0,\"training_coverage\":0.0,\"training_hit_rate\":0.0,\"training_ece\":1.0,\"training_block_min\":0.0},\"quantitative_gate_passed\":false,\"independent_confirmation\":{\"required\":true,\"passed\":false,\"reason\":\"EXISTING_10000_DRAW_DATASET_REUSED_FOR_MODEL_DEVELOPMENT;REQUIRES_NEW_FROZEN_TIME_FORWARD_BLOCK\"},\"locked_requirements\":{\"coverage_min\":0.15,\"hit_rate_min\":0.4,\"each_block_min\":0.37,\"ece_max\":0.04,\"rank1_above_rank2_rank3\":true,\"rank_stable_fold_min\":3,\"rolling_300_min\":0.4,\"independent_confirmation_required\":true},\"model_gate_passed\":false}";
    public static final double GLOBAL_SELECTED_P_THRESHOLD = 1d;
    public static final double GLOBAL_RANK_SCORE_TIE_THRESHOLD = 1.7976931348623157e+308d;
    public static final double MIN_COVERAGE = 0.15d;
    public static final double TARGET_COVERAGE_LOW = 0.18d;
    public static final double TARGET_COVERAGE_HIGH = 0.22d;
    public static final double MIN_HIT_RATE = 0.40d;
    public static final double MIN_BLOCK_HIT_RATE = 0.37d;
    public static final double MAX_ECE = 0.04d;
    public static final double OOS_COVERAGE = 0d;
    public static final double OOS_HIT_RATE = 0d;
    public static final double OOS_ECE = 1d;
    public static final boolean MODEL_GATE_PASSED = false;
}
