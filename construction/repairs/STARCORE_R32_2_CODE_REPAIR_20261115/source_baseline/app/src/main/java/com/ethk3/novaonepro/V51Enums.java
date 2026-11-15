package com.ethk3.novaonepro;

/** Typed V51 codes. String matching is forbidden in the active runtime. */
public final class V51Enums {
    private V51Enums() {}

    public enum Lifecycle {
        ENTRY,
        GROW,
        MATURE,
        EXHAUST,
        BREAK,
        RECOVER,
        UNKNOWN
    }

    public enum Lane {
        CORE_PROVEN,
        DYNAMIC_CONFIRMED,
        CONTROLLED_PROBE,
        SHADOW,
        HOLD
    }

    public enum DomainStatus {
        DISCOVERED,
        WATCH,
        READY,
        PROVEN,
        DECAYING,
        SOFT_RISK,
        HARD_FREEZE,
        RECOVERING,
        RETIRED
    }

    public enum WindowStatus {
        DISCOVERED,
        WATCH,
        READY,
        PROVEN,
        DECAYING,
        SOFT_RISK,
        HARD_FREEZE,
        RECOVERING,
        RETIRED
    }

    public enum TeacherStatus {
        NO_SAMPLE,
        WATCH,
        READY,
        STRONG,
        SOFT_RISK,
        HARD_FREEZE,
        RECOVERING
    }

    public enum ExpertStatus {
        READY,
        WATCH,
        SOFT_RISK,
        ZERO_WEIGHT,
        FROZEN,
        RECOVERING
    }

    public enum DecisionAction {
        BET,
        SHADOW,
        HOLD
    }

    public enum ArtifactStatus {
        MISSING,
        INVALID,
        ENGINEERING,
        CHALLENGER,
        CHAMPION_NOT_PROVEN,
        PROVEN,
        ROLLED_BACK
    }

    public enum ImplementationStatus {
        NOT_STARTED,
        PARTIAL,
        IMPLEMENTED_NOT_VERIFIED,
        VERIFIED_NOT_PROVEN,
        PROVEN
    }

    public enum RejectCode {
        NONE,
        DATA_INCOMPLETE,
        TARGET_ISSUE_INVALID,
        HISTORY_NOT_POINT_IN_TIME,
        HISTORY_HASH_MISMATCH,
        PERMUTATION_INPUT_INVALID,
        PERMUTATION_NORMALIZATION_FAILED,
        ARTIFACT_BUNDLE_MISMATCH,
        OWNER_CONFLICT,
        FEATURE_CONTRACT_MISMATCH,
        MODEL_NOT_PROVEN,
        RANK_ORDER_NOT_PROVEN,
        CALIBRATION_NOT_PROVEN,
        PREDICTABILITY_NOT_PROVEN,
        POLICY_CELL_NOT_PROVEN,
        DOMAIN_NOT_PROVEN,
        RANK_UNSTABLE,
        THIRD_BOUNDARY_UNSTABLE,
        FALSE_CONSENSUS,
        FALSIFIER_TOO_STRONG,
        UNCERTAINTY_HIGH,
        LEDGER_INCOMPLETE,
        DUPLICATE_TRADE,
        UNSETTLED_TRADE_EXISTS,
        FIXED_K3_CONTRACT_FAILED,
        AUTHORIZATION_FAILED,
        RUNTIME_EXCEPTION
    }

    public enum FailureCode {
        NONE,
        SCHEMA_MISSING_TABLE,
        SCHEMA_MISSING_COLUMN,
        SCHEMA_MISSING_INDEX,
        MIGRATION_FAILED,
        ISSUE_COUNT_NOT_1,
        NUMBER_EVENT_COUNT_NOT_100,
        POSITION_ACTION_COUNT_NOT_10,
        PERMUTATION_COUNT_NOT_1,
        SELECTED_DECISION_COUNT_GT_1,
        REAL_TRADE_COUNT_GT_1,
        PREDICTION_HASH_CONFLICT,
        PREDICTION_ALREADY_SETTLED,
        TARGET_ISSUE_MISMATCH,
        ROW_SUM_ERROR,
        COLUMN_SUM_ERROR,
        MAP_ASSIGNMENT_INVALID,
        ARTIFACT_IDENTITY_MISMATCH,
        SQLITE_ERROR,
        UNKNOWN
    }

    public enum AttributionType {
        NUMBER_MODEL_MISS,
        PERMUTATION_CONSTRAINT_MISS,
        POSITION_RANK_MISS,
        THIRD_BOUNDARY_MISS,
        REGIME_SWITCH,
        TEACHER_MISLEAD,
        RELATION_MISLEAD,
        MOTIF_MISMATCH,
        FALSE_CONSENSUS,
        CALIBRATION_OVERCONFIDENT,
        STALE_ACTION,
        WINDOW_FALSE_DISCOVERY,
        WINDOW_DECAY_MISS,
        PREDICTABILITY_FALSE_POSITIVE,
        LUCKY_HIT,
        STRUCTURAL_HIT,
        UNKNOWN
    }
}
