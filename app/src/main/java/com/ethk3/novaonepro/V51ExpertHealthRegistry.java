package com.ethk3.novaonepro;

import java.util.HashMap;
import java.util.Map;

/**
 * B09 authoritative expert lifecycle registry. Owns expert health only. It cannot write NumberQ,
 * SelectedHitP, Lane, BET or settlement.
 */
public final class V51ExpertHealthRegistry {
    public static final String V57_B09_OWNER = "EXPERT_HEALTH_ONLY";

    public enum HealthState {
        NO_SAMPLE,
        WATCH,
        READY,
        STRONG,
        SOFT_RISK,
        HARD_FREEZE,
        RECOVERING
    }

    public static final class Snapshot {
        public final String expertId;
        public final HealthState state;
        public final int sample;
        public final int consecutiveBad;
        public final int recoveryGood;
        public final double reliability;
        public final double recentReliability;
        public final double calibrationError;
        public final double rankRegret;
        public final double weightMultiplier;
        public final String reason;
        public final long updatedAt;

        Snapshot(
                String id,
                HealthState st,
                int n,
                int bad,
                int good,
                double rel,
                double recent,
                double cal,
                double rank,
                double mult,
                String why,
                long at) {
            expertId = s(id);
            state = st == null ? HealthState.NO_SAMPLE : st;
            sample = Math.max(0, n);
            consecutiveBad = Math.max(0, bad);
            recoveryGood = Math.max(0, good);
            reliability = clamp01(rel);
            recentReliability = clamp01(recent);
            calibrationError = clamp01(cal);
            rankRegret = clamp01(rank);
            weightMultiplier = clamp(mult, 0.0d, 1.25d);
            reason = s(why);
            updatedAt = Math.max(0L, at);
        }

        public boolean zeroWeight() {
            return state == HealthState.HARD_FREEZE;
        }

        public boolean productionHealthy() {
            return state == HealthState.READY || state == HealthState.STRONG;
        }

        public String canonical() {
            return expertId
                    + '|'
                    + state
                    + '|'
                    + sample
                    + '|'
                    + consecutiveBad
                    + '|'
                    + recoveryGood
                    + '|'
                    + reliability
                    + '|'
                    + recentReliability
                    + '|'
                    + calibrationError
                    + '|'
                    + rankRegret
                    + '|'
                    + weightMultiplier
                    + '|'
                    + reason
                    + '|'
                    + updatedAt;
        }
    }

    private static final class Entry {
        HealthState state = HealthState.NO_SAMPLE;
        int recoveryGood;
        long lastChangeAt;
    }

    private final HashMap<String, Entry> entries = new HashMap<String, Entry>();

    /** Compatibility mapping for old callers. New code should consume HealthState/Snapshot. */
    public V51Enums.ExpertStatus status(
            int sample, double reliability, double recentReliability, int consecutiveMislead) {
        HealthState s =
                classify(
                        sample,
                        reliability,
                        recentReliability,
                        0.0d,
                        0.0d,
                        consecutiveMislead,
                        0,
                        HealthState.NO_SAMPLE);
        if (s == HealthState.STRONG || s == HealthState.READY) return V51Enums.ExpertStatus.READY;
        if (s == HealthState.SOFT_RISK) return V51Enums.ExpertStatus.SOFT_RISK;
        if (s == HealthState.HARD_FREEZE) return V51Enums.ExpertStatus.FROZEN;
        if (s == HealthState.RECOVERING) return V51Enums.ExpertStatus.RECOVERING;
        return V51Enums.ExpertStatus.WATCH;
    }

    public synchronized Snapshot assess(
            String expertId, V51ExpertRegretEngine.Snapshot regret, long nowMs) {
        String id = s(expertId);
        Entry e = entries.get(id);
        if (e == null) {
            e = new Entry();
            entries.put(id, e);
        }
        int n = regret == null ? 0 : regret.sample;
        double rel = regret == null ? 0.5d : regret.reliability;
        double recent = regret == null ? 0.5d : regret.recentReliability;
        double cal = regret == null ? 1.0d : regret.calibrationError;
        double rank = regret == null ? 1.0d : regret.rankRegret;
        int bad = regret == null ? 0 : regret.consecutiveMislead;
        boolean good = recent >= 0.48d && cal <= 0.12d && rank <= 0.35d && bad == 0;
        if (e.state == HealthState.HARD_FREEZE
                || e.state == HealthState.SOFT_RISK
                || e.state == HealthState.RECOVERING) {
            e.recoveryGood = good ? e.recoveryGood + 1 : 0;
        } else e.recoveryGood = good ? Math.min(20, e.recoveryGood + 1) : 0;
        HealthState next = classify(n, rel, recent, cal, rank, bad, e.recoveryGood, e.state);
        if (next != e.state) {
            e.state = next;
            e.lastChangeAt = nowMs;
        }
        double mult = multiplier(next, rel, recent, cal, rank);
        return new Snapshot(
                id,
                next,
                n,
                bad,
                e.recoveryGood,
                rel,
                recent,
                cal,
                rank,
                mult,
                reason(next, n, bad, e.recoveryGood, rel, recent, cal, rank),
                nowMs);
    }

    public synchronized HealthState state(String expertId) {
        Entry e = entries.get(s(expertId));
        return e == null ? HealthState.NO_SAMPLE : e.state;
    }

    public synchronized int trackedExperts() {
        return entries.size();
    }

    public synchronized Map<String, String> diagnostics() {
        HashMap<String, String> out = new HashMap<String, String>();
        for (Map.Entry<String, Entry> x : entries.entrySet())
            out.put(
                    x.getKey(),
                    x.getValue().state.name()
                            + "|recover="
                            + x.getValue().recoveryGood
                            + "|changed="
                            + x.getValue().lastChangeAt);
        return out;
    }

    private static HealthState classify(
            int n,
            double rel,
            double recent,
            double cal,
            double rank,
            int bad,
            int recoveryGood,
            HealthState prior) {
        if (n < 20) return HealthState.NO_SAMPLE;
        if (bad >= 12 || recent < 0.16d || cal > 0.30d || rank > 0.75d)
            return HealthState.HARD_FREEZE;
        if (prior == HealthState.HARD_FREEZE && recoveryGood < 12) return HealthState.RECOVERING;
        if (bad >= 6 || recent < 0.28d || cal > 0.20d || rank > 0.58d) return HealthState.SOFT_RISK;
        if ((prior == HealthState.SOFT_RISK || prior == HealthState.RECOVERING) && recoveryGood < 8)
            return HealthState.RECOVERING;
        if (n >= 180 && rel >= 0.64d && recent >= 0.58d && cal <= 0.075d && rank <= 0.28d)
            return HealthState.STRONG;
        if (n >= 80 && rel >= 0.50d && recent >= 0.44d && cal <= 0.14d && rank <= 0.42d)
            return HealthState.READY;
        return HealthState.WATCH;
    }

    private static double multiplier(
            HealthState state, double rel, double recent, double cal, double rank) {
        double quality =
                clamp01(
                        0.45d * rel
                                + 0.35d * recent
                                + 0.10d * (1.0d - cal)
                                + 0.10d * (1.0d - rank));
        if (state == HealthState.STRONG) return clamp(0.95d + 0.25d * quality, 0.0d, 1.20d);
        if (state == HealthState.READY) return clamp(0.70d + 0.25d * quality, 0.0d, 1.00d);
        if (state == HealthState.WATCH) return clamp(0.30d + 0.25d * quality, 0.0d, 0.55d);
        if (state == HealthState.RECOVERING) return clamp(0.10d + 0.20d * quality, 0.0d, 0.30d);
        if (state == HealthState.SOFT_RISK) return clamp(0.03d + 0.10d * quality, 0.0d, 0.15d);
        return 0.0d;
    }

    private static String reason(
            HealthState st,
            int n,
            int bad,
            int good,
            double rel,
            double recent,
            double cal,
            double rank) {
        return st
                + "|n="
                + n
                + "|bad="
                + bad
                + "|recover="
                + good
                + "|rel="
                + fmt(rel)
                + "|recent="
                + fmt(recent)
                + "|cal="
                + fmt(cal)
                + "|rank="
                + fmt(rank);
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.US, "%.5f", v);
    }

    private static String s(String x) {
        return x == null ? "" : x;
    }

    private static double clamp01(double v) {
        return clamp(v, 0.0d, 1.0d);
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
