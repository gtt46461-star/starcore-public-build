package com.ethk3.novaonepro;

/** Persistable, typed output availability. Probability and REAL authorization never share a flag. */
public enum PredictionAvailabilityR815 {
    NO_HISTORY_LT_12(false, false, true),
    HISTORY_GAPPED(false, false, true),
    HISTORY_CONFLICT(false, false, true),
    TARGET_ALREADY_DRAWN(false, false, true),
    PRODUCER_QUEUED(false, false, false),
    PRODUCER_RUNNING(false, false, false),
    PRODUCER_FAILED(false, false, true),
    CANONICAL_READY_SHADOW(true, false, true),
    /** Numbers/action are ready and an exact unproven engineering candidate may be ledgered. */
    CANONICAL_READY_CONTROLLED_UNPROVEN(true, false, true),
    CANONICAL_READY_PROVEN(true, true, true);

    public static final int SCHEMA_VERSION = 2;
    private final boolean numbers;
    private final boolean probabilityWhenProven;
    private final boolean terminal;

    PredictionAvailabilityR815(boolean numbers, boolean probabilityWhenProven, boolean terminal) {
        this.numbers = numbers;
        this.probabilityWhenProven = probabilityWhenProven;
        this.terminal = terminal;
    }

    public boolean hasNumbers() {
        return numbers;
    }

    public boolean probabilityAvailable(boolean modelProven) {
        return numbers && modelProven && probabilityWhenProven && this == CANONICAL_READY_PROVEN;
    }

    public boolean realAuthorized(boolean modelProven, boolean realHardDisabled) {
        return probabilityAvailable(modelProven) && !realHardDisabled && !AppConfig.R815_SHADOW_ONLY;
    }

    public boolean isTerminalForTarget() {
        return terminal;
    }

    public static PredictionAvailabilityR815 fromPersisted(String raw) {
        if (raw != null) {
            String value = raw.trim();
            for (PredictionAvailabilityR815 candidate : values()) {
                if (candidate.name().equals(value)) return candidate;
            }
        }
        return PRODUCER_FAILED;
    }
}
