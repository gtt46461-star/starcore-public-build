package com.ethk3.novaonepro;

/** Selection-bias guard: Wilson shrinkage, FDR and winner's-curse penalty. */
public final class V51FalseDiscoveryGuard {
    public double wilsonLower(int hit, int sample) {
        if (sample <= 0) return 0;
        double n = sample, p = Math.max(0, Math.min(hit, sample)) / n, z = 1.96d, z2 = z * z;
        return V51Math.clamp(
                (p + z2 / (2 * n) - z * Math.sqrt((p * (1 - p) + z2 / (4 * n)) / n)) / (1 + z2 / n),
                0,
                1);
    }

    public double correctedQuality(
            int hit,
            int sample,
            double recentLift,
            double rankLift,
            double falseDiscoveryRisk,
            double drift) {
        double lcb = wilsonLower(hit, sample);
        double winnerPenalty = Math.min(0.12d, 1.0d / Math.sqrt(Math.max(1, sample)));
        return V51Math.clamp(
                lcb
                        + 0.25d * recentLift
                        + 0.20d * rankLift
                        - 0.35d * falseDiscoveryRisk
                        - 0.30d * drift
                        - winnerPenalty,
                0,
                1);
    }

    public boolean eligible(
            int hit, int sample, double recentLift, double rankLift, double fdr, double drift) {
        return sample >= 30
                && fdr <= 0.10d
                && correctedQuality(hit, sample, recentLift, rankLift, fdr, drift) >= 0.34d;
    }
}
