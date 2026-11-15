package com.ethk3.novaonepro;

/**
 * Label-blind E03E drift decomposition. Runtime/data outages are a separate namespace and can never
 * be reinterpreted as market drift. The classifier is diagnostic only: it does not change K, create
 * a bet, loosen a gate, or claim a single-issue guarantee.
 */
public final class DriftUncertaintyStateR932 {
    public static final String REVISION = "R932_DRIFT_UNCERTAINTY_STATE_V1";

    public enum State {
        STABLE,
        ABRUPT_MARKET_DRIFT,
        GRADUAL_MARKET_DRIFT,
        TRANSIENT_SHORT_NOISE,
        RUNTIME_OR_NETWORK_OUTAGE,
        UNKNOWN_INSUFFICIENT_SUPPORT
    }

    private DriftUncertaintyStateR932() {}

    /**
     * @param shortMedium JS-distance between short and medium windows
     * @param mediumLong JS-distance between medium and long windows
     * @param shortLong JS-distance between short and long windows
     * @param runtimeHealthy false only for runtime/network/data-source health failures
     * @param supportRows pre-draw history support, never current/future labels
     */
    public static Result classify(
            double shortMedium,
            double mediumLong,
            double shortLong,
            boolean runtimeHealthy,
            int supportRows) {
        if (!runtimeHealthy)
            return new Result(State.RUNTIME_OR_NETWORK_OUTAGE, 1.0d, 0.0d,
                    "RUNTIME_HEALTH_SEPARATE_FROM_MARKET");
        if (supportRows < 32 || !finite01(shortMedium) || !finite01(mediumLong) || !finite01(shortLong))
            return new Result(State.UNKNOWN_INSUFFICIENT_SUPPORT, 1.0d, 0.0d,
                    "INSUFFICIENT_OR_NONFINITE_PREDRAW_SUPPORT");

        // Short spike while the older baseline remains close: temporary/noisy shock, not a durable
        // market-state rewrite.
        if (shortMedium >= 0.55d && mediumLong < 0.22d && shortLong < 0.48d)
            return new Result(State.TRANSIENT_SHORT_NOISE,
                    clamp01(0.60d * shortMedium + 0.40d * (1.0d - mediumLong)),
                    clamp01(1.0d - shortLong), "SHORT_SPIKE_WITH_LONG_BASELINE_STABLE");

        // Abrupt break: short and long endpoints are far apart while the medium-to-long segment has
        // not yet accumulated the same displacement.
        if (shortMedium >= 0.48d && shortLong >= 0.58d && mediumLong < 0.42d)
            return new Result(State.ABRUPT_MARKET_DRIFT,
                    clamp01(0.45d * shortMedium + 0.55d * shortLong),
                    clamp01(1.0d - shortLong), "ENDPOINT_BREAK_WITH_RECENT_JUMP");

        // Gradual migration: displacement is present across adjacent scales and persists to the
        // longest window instead of existing only at the shortest edge.
        if (mediumLong >= 0.38d && shortLong >= 0.50d && shortMedium >= 0.22d)
            return new Result(State.GRADUAL_MARKET_DRIFT,
                    clamp01((shortMedium + mediumLong + shortLong) / 3.0d),
                    clamp01(1.0d - Math.max(mediumLong, shortLong)),
                    "MULTISCALE_PERSISTENT_MIGRATION");

        double risk = clamp01(0.35d * shortMedium + 0.25d * mediumLong + 0.40d * shortLong);
        return new Result(State.STABLE, risk, clamp01(1.0d - risk), "NO_DURABLE_DRIFT_PATTERN");
    }

    public static final class Result {
        public final State state;
        public final double risk, stability;
        public final String reason, checksum;

        Result(State state, double risk, double stability, String reason) {
            this.state = state == null ? State.UNKNOWN_INSUFFICIENT_SUPPORT : state;
            this.risk = clamp01(risk);
            this.stability = clamp01(stability);
            this.reason = reason == null ? "" : reason;
            checksum = V51ChecksumEngine.sha256(REVISION + '|' + this.state.name() + '|'
                    + this.risk + '|' + this.stability + '|' + this.reason);
        }

        public String canonical() {
            return REVISION + "|state=" + state.name() + "|risk=" + risk + "|stability="
                    + stability + "|reason=" + reason + "|diagnostic_only=1|may_change_k=0|may_create_real=0";
        }
    }

    private static boolean finite01(double value) {
        return Double.isFinite(value) && value >= 0.0d && value <= 1.0d;
    }

    private static double clamp01(double value) {
        if (!Double.isFinite(value)) return 0.0d;
        return Math.max(0.0d, Math.min(1.0d, value));
    }
}
