package com.ethk3.novaonepro;

import java.util.List;

/**
 * Deterministic 30-family state taxonomy with explicit lifecycle and transition risk. This engine
 * never changes probability; it only describes observable context.
 */
public final class V49WindowStateEngine {
    public static final String MODULE_VERSION = V49BuildManifest.STATE_SCHEMA;

    public static final String[] FAMILIES =
            new String[] {
                "HOT_CONTINUATION_DENSE", "HOT_CONTINUATION_LIGHT", "HOT_ACCELERATION",
                "HOT_EXHAUSTION", "COLD_ACCUMULATION", "COLD_RELEASE_EARLY",
                "COLD_RELEASE_CONFIRMED", "COMPRESSION_LOW_ENTROPY", "COMPRESSION_BREAKOUT",
                "EXPANSION_HIGH_ENTROPY", "OSCILLATION_BALANCED", "OSCILLATION_BIASED",
                "REVERSAL_EARLY", "REVERSAL_CONFIRMED", "FALSE_REVERSAL_RISK",
                "POSITION_MIGRATION_FRONT", "POSITION_MIGRATION_MID", "POSITION_MIGRATION_BACK",
                "CROSS_POSITION_SYNC", "CROSS_POSITION_SQUEEZE", "NUMBER_SUBSTITUTION",
                "NEIGHBOR_PROPAGATION", "TAIL_CLUSTER", "PARITY_ROTATION",
                "BIG_SMALL_ROTATION", "PULSE_BURST", "INTERMITTENT_GAP",
                "BREAK_RECOVERY", "REGIME_TRANSITION", "UNSTABLE_NOISE"
            };

    private V49WindowStateEngine() {}

    public static void apply(V49FeatureSnapshot s, List<Models.Draw> history) {
        if (s == null) return;
        for (int p = 0; p < 10; p++) {
            State z = classify(s, history, p);
            s.family[p] = z.family;
            s.lifecycle[p] = z.lifecycle;
            s.switchRisk[p] = z.switchRisk;
            s.familyStrength[p] = z.strength;
            s.stateRunLength[p] = z.runLength;
        }
    }

    private static State classify(V49FeatureSnapshot s, List<Models.Draw> h, int p) {
        double maxSlope = -9.0d, minSlope = 9.0d, maxGap = 0.0d, maxAccel = -9.0d;
        int hotN = 0, coldN = 0;
        for (int n = 0; n < 10; n++) {
            if (s.slope[p][n] > maxSlope) {
                maxSlope = s.slope[p][n];
                hotN = n;
            }
            if (s.slope[p][n] < minSlope) minSlope = s.slope[p][n];
            if (s.gapPercentile[p][n] > maxGap) {
                maxGap = s.gapPercentile[p][n];
                coldN = n;
            }
            if (s.acceleration[p][n] > maxAccel) maxAccel = s.acceleration[p][n];
        }
        double entropy = s.entropy[p];
        double concentration = s.concentration[p];
        double recentShift = distributionDistance(s.frequency[0][p], s.frequency[3][p]);
        double mediumShift = distributionDistance(s.frequency[2][p], s.frequency[4][p]);
        double cusum = Math.min(1.0d, (recentShift * 2.4d + mediumShift * 1.4d));
        double hazard = lifecycleHazard(h, p + 1);
        double switchRisk = FeatureContractRegistry.clamp01(0.52d * cusum + 0.48d * hazard);

        String family;
        if (switchRisk > 0.78d) family = "UNSTABLE_NOISE";
        else if (switchRisk > 0.62d) family = "REGIME_TRANSITION";
        else if (entropy < 0.72d && recentShift > 0.18d) family = "COMPRESSION_BREAKOUT";
        else if (entropy < 0.72d) family = "COMPRESSION_LOW_ENTROPY";
        else if (entropy > 0.96d && concentration < 0.34d) family = "EXPANSION_HIGH_ENTROPY";
        else if (maxSlope > 0.085d && maxAccel > 0.025d) family = "HOT_ACCELERATION";
        else if (maxSlope > 0.060d
                && s.frequency[4][p][hotN] > 0.145d
                && s.acceleration[p][hotN] < -0.015d) family = "HOT_EXHAUSTION";
        else if (maxSlope > 0.050d && concentration > 0.38d) family = "HOT_CONTINUATION_DENSE";
        else if (maxSlope > 0.035d) family = "HOT_CONTINUATION_LIGHT";
        else if (maxGap > 0.92d && s.frequency[0][p][coldN] > s.frequency[2][p][coldN])
            family = "COLD_RELEASE_CONFIRMED";
        else if (maxGap > 0.86d && s.acceleration[p][coldN] > 0.0d) family = "COLD_RELEASE_EARLY";
        else if (maxGap > 0.84d) family = "COLD_ACCUMULATION";
        else if (minSlope < -0.055d && recentShift > 0.12d) family = "REVERSAL_EARLY";
        else if (isNeighborPropagation(s, p)) family = "NEIGHBOR_PROPAGATION";
        else if (isParityRotation(h, p + 1)) family = "PARITY_ROTATION";
        else if (isBigSmallRotation(h, p + 1)) family = "BIG_SMALL_ROTATION";
        else if (isPulse(s, p)) family = "PULSE_BURST";
        else if (maxGap > 0.70d && entropy > 0.88d) family = "INTERMITTENT_GAP";
        else if (concentration > 0.40d) family = "OSCILLATION_BIASED";
        else family = "OSCILLATION_BALANCED";

        int run = estimateStateRun(h, p + 1, family);
        String lifecycle;
        if (family.equals("REGIME_TRANSITION") || family.equals("UNSTABLE_NOISE"))
            lifecycle = "BREAK";
        else if (run <= 1) lifecycle = "START";
        else if (run <= 3) lifecycle = "GROW";
        else if (run <= 6) lifecycle = "MATURE";
        else if (hazard > 0.62d || (family.startsWith("HOT") && maxAccel < -0.02d))
            lifecycle = "EXHAUST";
        else if (recentShift < mediumShift * 0.55d) lifecycle = "RECOVER";
        else lifecycle = "MATURE";

        double prototype =
                prototypeStrength(family, maxSlope, maxGap, entropy, concentration, recentShift);
        double strength = FeatureContractRegistry.clamp01(prototype * (1.0d - 0.55d * switchRisk));
        return new State(family, lifecycle, switchRisk, strength, run);
    }

    private static double prototypeStrength(
            String family,
            double slope,
            double gap,
            double entropy,
            double concentration,
            double shift) {
        if (family.startsWith("HOT"))
            return FeatureContractRegistry.clamp01(0.45d + slope * 4.0d + concentration * 0.35d);
        if (family.startsWith("COLD"))
            return FeatureContractRegistry.clamp01(0.35d + gap * 0.45d + shift * 0.35d);
        if (family.startsWith("COMPRESSION"))
            return FeatureContractRegistry.clamp01(0.78d - entropy * 0.50d + shift * 0.55d);
        if (family.startsWith("REVERSAL"))
            return FeatureContractRegistry.clamp01(0.40d + Math.abs(slope) * 3.0d + shift * 0.45d);
        if (family.equals("REGIME_TRANSITION") || family.equals("UNSTABLE_NOISE")) return 0.25d;
        return FeatureContractRegistry.clamp01(
                0.42d + (1.0d - Math.abs(entropy - 0.90d)) * 0.25d + concentration * 0.20d);
    }

    private static double distributionDistance(double[] a, double[] b) {
        double sum = 0.0d;
        for (int i = 0; i < 10; i++) sum += Math.abs(a[i] - b[i]);
        return Math.min(1.0d, sum * 0.5d);
    }

    private static double lifecycleHazard(List<Models.Draw> h, int position) {
        if (h == null || h.size() < 12) return 0.5d;
        int n = h.size();
        int changes = 0;
        double prev = shortBias(h, position, n - 12, n - 6);
        double last = shortBias(h, position, n - 6, n);
        if (Math.signum(prev) != Math.signum(last)) changes++;
        double earlier = shortBias(h, position, Math.max(0, n - 24), n - 12);
        if (Math.signum(earlier) != Math.signum(prev)) changes++;
        double magnitude = Math.min(1.0d, Math.abs(last - prev) * 2.0d);
        return FeatureContractRegistry.clamp01(0.18d + changes * 0.22d + magnitude * 0.40d);
    }

    private static double shortBias(List<Models.Draw> h, int position, int start, int end) {
        if (h == null || end <= start) return 0.0d;
        int big = 0, total = 0;
        for (int i = Math.max(0, start); i < Math.min(h.size(), end); i++) {
            int x = h.get(i).getNumberAtPosition(position);
            if (x > 0) {
                if (x >= 6) big++;
                total++;
            }
        }
        if (total == 0) return 0.0d;
        return ((double) big / total) - 0.5d;
    }

    private static int estimateStateRun(List<Models.Draw> h, int position, String family) {
        if (h == null || h.size() < 8) return 1;
        int run = 1;
        int n = h.size();
        double current = shortBias(h, position, n - 6, n);
        int sign = current > 0.08d ? 1 : (current < -0.08d ? -1 : 0);
        for (int back = 1; back <= 8; back++) {
            int end = n - back;
            int start = end - 6;
            if (start < 0) break;
            double b = shortBias(h, position, start, end);
            int s = b > 0.08d ? 1 : (b < -0.08d ? -1 : 0);
            if (s != sign) break;
            run++;
        }
        return run;
    }

    private static boolean isNeighborPropagation(V49FeatureSnapshot s, int p) {
        int last = s.lastNumber[p];
        if (last <= 0) return false;
        int a = last - 2, b = last;
        double v = 0.0d;
        if (a >= 0) v += s.frequency[0][p][a] - s.frequency[3][p][a];
        if (b < 10) v += s.frequency[0][p][b] - s.frequency[3][p][b];
        return v > 0.055d;
    }

    private static boolean isParityRotation(List<Models.Draw> h, int position) {
        if (h == null || h.size() < 6) return false;
        int flips = 0;
        int prev = h.get(h.size() - 6).getNumberAtPosition(position) & 1;
        for (int i = h.size() - 5; i < h.size(); i++) {
            int now = h.get(i).getNumberAtPosition(position) & 1;
            if (now != prev) flips++;
            prev = now;
        }
        return flips >= 4;
    }

    private static boolean isBigSmallRotation(List<Models.Draw> h, int position) {
        if (h == null || h.size() < 6) return false;
        int flips = 0;
        int prev = h.get(h.size() - 6).getNumberAtPosition(position) >= 6 ? 1 : 0;
        for (int i = h.size() - 5; i < h.size(); i++) {
            int now = h.get(i).getNumberAtPosition(position) >= 6 ? 1 : 0;
            if (now != prev) flips++;
            prev = now;
        }
        return flips >= 4;
    }

    private static boolean isPulse(V49FeatureSnapshot s, int p) {
        for (int n = 0; n < 10; n++) {
            if (s.frequency[0][p][n] > 0.21d
                    && s.frequency[2][p][n] < 0.13d
                    && s.volatility[p][n] > 0.12d) return true;
        }
        return false;
    }

    private static final class State {
        final String family;
        final String lifecycle;
        final double switchRisk;
        final double strength;
        final int runLength;

        State(String f, String l, double r, double s, int run) {
            family = f;
            lifecycle = l;
            switchRisk = r;
            strength = s;
            runLength = run;
        }
    }
}
