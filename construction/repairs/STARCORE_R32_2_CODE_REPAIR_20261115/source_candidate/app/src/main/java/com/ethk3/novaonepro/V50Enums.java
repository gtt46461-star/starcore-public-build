package com.ethk3.novaonepro;

/** Typed V50 state codes. Strings are serialization/UI only, never hot-path logic. */
public final class V50Enums {
    private V50Enums() {}

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

    public enum AttributionType {
        NUMBER_MODEL_MISS,
        POSITION_RANK_MISS,
        THIRD_BOUNDARY_MISS,
        REGIME_SWITCH,
        FALSE_CONTINUATION,
        FALSE_REVERSAL,
        FALSE_RELEASE,
        RELATION_MISLEAD,
        TEACHER_MISLEAD,
        MOTIF_MISMATCH,
        EXPERT_FALSE_CONSENSUS,
        CALIBRATION_OVERCONFIDENT,
        ACTION_STALE,
        LUCKY_HIT,
        STRUCTURAL_HIT,
        UNKNOWN
    }

    public enum RejectCode {
        NONE,
        DATA_INCOMPLETE,
        TARGET_ISSUE_INVALID,
        DUPLICATE_TRADE,
        UNSETTLED_TRADE_EXISTS,
        DATA_STALE,
        LEDGER_INCOMPLETE,
        CONTRACT_MISMATCH,
        BUILD_IDENTITY_MISMATCH,
        MODEL_ARTIFACT_UNPROVEN,
        DOMAIN_HARD_FREEZE,
        STRUCTURE_HARD_RISK,
        SELECTED_HIT_P_BELOW_POLICY,
        RANK_MARGIN_WEAK,
        EFFECTIVE_CONSENSUS_WEAK,
        COUNTER_EVIDENCE_STRONG,
        UNCERTAINTY_HIGH,
        AUTHORIZATION_FAILED
    }

    public enum FailureCode {
        NONE,
        SCHEMA_MISSING_TABLE,
        SCHEMA_MISSING_COLUMN,
        SCHEMA_MISSING_INDEX,
        MIGRATION_FAILED,
        PREDICTION_INSERT_FAILED,
        PREDICTION_HASH_CONFLICT,
        PREDICTION_ALREADY_SETTLED,
        NUMBER_EVENT_COUNT_NOT_100,
        POSITION_ACTION_COUNT_NOT_10,
        SELECTED_DECISION_COUNT_GT_1,
        REAL_TRADE_COUNT_GT_1,
        TARGET_ISSUE_MISMATCH,
        NUMBER_PROBABILITY_NOT_NORMALIZED,
        TOP3_INVALID,
        AUTH_CHAIN_INCOMPLETE,
        SETTLEMENT_ALREADY_APPLIED,
        SQLITE_ERROR,
        UNKNOWN
    }
}
