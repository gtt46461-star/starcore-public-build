package com.ethk3.novaonepro;

/** Evidence-derived expert health transitions; no one-hit instant recovery. */
public final class V50ExpertHealthRegistry {
    private V50ExpertHealthRegistry() {}

    public static V50Enums.ExpertStatus resolve(
            int sample,
            double meanLogLoss,
            double recentLoss,
            int misleadRun,
            V50Enums.ExpertStatus previous) {
        V50Enums.ExpertStatus old = previous == null ? V50Enums.ExpertStatus.WATCH : previous;
        if (misleadRun >= 8 || (sample >= 80 && recentLoss > 2.85d)) {
            return V50Enums.ExpertStatus.FROZEN;
        }
        if (misleadRun >= 5 || (sample >= 50 && meanLogLoss > 2.65d)) {
            return V50Enums.ExpertStatus.ZERO_WEIGHT;
        }
        if (misleadRun >= 3 || (sample >= 30 && recentLoss > 2.50d)) {
            return V50Enums.ExpertStatus.SOFT_RISK;
        }
        if (old == V50Enums.ExpertStatus.FROZEN
                || old == V50Enums.ExpertStatus.ZERO_WEIGHT
                || old == V50Enums.ExpertStatus.SOFT_RISK) {
            return sample >= 30 && misleadRun == 0 && recentLoss < 2.35d
                    ? V50Enums.ExpertStatus.RECOVERING
                    : old;
        }
        if (sample >= 40 && meanLogLoss < 2.42d && recentLoss < 2.42d) {
            return V50Enums.ExpertStatus.READY;
        }
        return V50Enums.ExpertStatus.WATCH;
    }

    public static double weight(V50Enums.ExpertStatus status) {
        if (status == V50Enums.ExpertStatus.READY) return 1.0d;
        if (status == V50Enums.ExpertStatus.WATCH) return 0.55d;
        if (status == V50Enums.ExpertStatus.SOFT_RISK) return 0.20d;
        if (status == V50Enums.ExpertStatus.RECOVERING) return 0.30d;
        return 0.0d;
    }
}
