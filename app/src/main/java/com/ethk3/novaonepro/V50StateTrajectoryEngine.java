package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/**
 * Reconstructs each position's prior state sequence before assigning run length, lifecycle and
 * transition hazard. Run length is never inferred from a current label string.
 */
public final class V50StateTrajectoryEngine {
    public static final String MODULE_VERSION = V50BuildManifest.STATE_SCHEMA;
    private static final int REPLAY_LIMIT = 48;

    private final V50StatePrototypeEngine prototypeEngine = new V50StatePrototypeEngine();
    private final V50LifecycleTransitionEngine lifecycleEngine = new V50LifecycleTransitionEngine();
    private final V50StateHistoryCache cache = new V50StateHistoryCache();

    public V50Models.StateTrajectorySnapshot[] build(
            V50Models.NumberFeatureSnapshot features, List<Models.Draw> history) {
        if (features == null
                || features.context == null
                || history == null
                || history.size() < 12) {
            return new V50Models.StateTrajectorySnapshot[0];
        }
        String cacheKey = features.context.targetIssue + '|' + features.checksum;
        V50Models.StateTrajectorySnapshot[] cached = cache.get(cacheKey);
        if (cached != null) return cached;
        V50Models.StateTrajectorySnapshot[] out = new V50Models.StateTrajectorySnapshot[10];
        for (int position = 1; position <= 10; position++) {
            V50Models.StatePrototypeScore[] prototypes = prototypeEngine.score(features, position);
            String current = prototypes.length == 0 ? "UNKNOWN" : prototypes[0].prototypeId;
            double strength = prototypes.length == 0 ? 0.0d : prototypes[0].score;
            Replay replay = replay(history, position);
            int fineRun = run(replay.fine, current);
            int mediumRun = run(replay.medium, medium(current));
            int coarseRun = run(replay.coarse, coarse(current));
            double changeRate = replay.transitions / Math.max(1.0d, replay.count - 1.0d);
            double strengthDelta = replay.lastStrength - replay.previousStrength;
            double cusum = Math.min(1.0d, Math.abs(replay.shortBias - replay.longBias) * 2.5d);
            double durationHazard = Math.min(1.0d, Math.max(0, fineRun - 4) / 10.0d);
            double hazard = clamp01(0.42d * changeRate + 0.34d * cusum + 0.24d * durationHazard);
            boolean previousBreak = replay.previousBreak;
            V50Enums.Lifecycle lifecycle =
                    lifecycleEngine.resolve(
                            fineRun, strength, strengthDelta, hazard, previousBreak);
            double remaining = Math.max(0.0d, replay.meanRun - fineRun);
            double confidence =
                    clamp01(
                            0.38d * strength
                                    + 0.34d * (1.0d - hazard)
                                    + 0.28d * Math.min(1.0d, fineRun / 6.0d));
            String domain =
                    V50DynamicDomainEngine.domainId(
                            current, lifecycle, position, hazard, confidence);
            String checksum =
                    checksum(
                            features.context.targetIssue,
                            position,
                            prototypes,
                            domain,
                            lifecycle,
                            coarseRun,
                            mediumRun,
                            fineRun,
                            remaining,
                            hazard,
                            confidence);
            out[position - 1] =
                    new V50Models.StateTrajectorySnapshot(
                            features.context.targetIssue,
                            position,
                            prototypes,
                            domain,
                            lifecycle,
                            coarseRun,
                            mediumRun,
                            fineRun,
                            remaining,
                            hazard,
                            hazard,
                            confidence,
                            checksum);
        }
        cache.put(cacheKey, out);
        return (V50Models.StateTrajectorySnapshot[]) out.clone();
    }

    private static Replay replay(List<Models.Draw> history, int position) {
        int size = history.size();
        int start = Math.max(12, size - REPLAY_LIMIT);
        Replay out = new Replay();
        String previousFine = "";
        int currentRun = 0;
        int runTotal = 0;
        int runCount = 0;
        boolean lastBreak = false;
        for (int end = start; end <= size; end++) {
            Point point = stateAt(history, end, position);
            out.fine.add(point.fine);
            out.medium.add(medium(point.fine));
            out.coarse.add(coarse(point.fine));
            out.count++;
            if (previousFine.length() > 0 && !previousFine.equals(point.fine)) {
                out.transitions++;
                if (currentRun > 0) {
                    runTotal += currentRun;
                    runCount++;
                }
                currentRun = 1;
            } else {
                currentRun++;
            }
            previousFine = point.fine;
            out.previousStrength = out.lastStrength;
            out.lastStrength = point.strength;
            out.longBias = point.longBias;
            out.shortBias = point.shortBias;
            out.previousBreak = lastBreak;
            lastBreak = point.breakLike;
        }
        if (currentRun > 0) {
            runTotal += currentRun;
            runCount++;
        }
        out.meanRun = runCount == 0 ? 1.0d : ((double) runTotal) / runCount;
        return out;
    }

    private static Point stateAt(List<Models.Draw> history, int end, int position) {
        double[] shortP = frequency(history, end, position, 6);
        double[] mediumP = frequency(history, end, position, 24);
        double[] longP = frequency(history, end, position, 48);
        double entropy = FeatureContractRegistry.entropy10(mediumP);
        double drift = V49ExpertSupport.js(shortP, longP);
        double maxSlope = -1.0d;
        double minSlope = 1.0d;
        for (int n = 0; n < 10; n++) {
            double slope = shortP[n] - longP[n];
            maxSlope = Math.max(maxSlope, slope);
            minSlope = Math.min(minSlope, slope);
        }
        String fine;
        double strength;
        if (drift > 0.075d && entropy < 0.88d) {
            fine = "COMPRESSION_BREAKOUT";
            strength = drift * 4.0d + (1.0d - entropy) * 0.5d;
        } else if (maxSlope > 0.055d) {
            fine = "HOT_ACCELERATION";
            strength = maxSlope * 5.0d;
        } else if (minSlope < -0.055d) {
            fine = "REVERSAL";
            strength = Math.abs(minSlope) * 4.5d;
        } else if (entropy > 0.94d) {
            fine = "OSCILLATION";
            strength = entropy * 0.55d;
        } else {
            fine = "STABLE_BALANCED";
            strength = 0.55d - drift;
        }
        double shortBias = bigBias(history, end, position, 6);
        double longBias = bigBias(history, end, position, 24);
        return new Point(fine, clamp01(strength), shortBias, longBias, drift > 0.12d);
    }

    private static double[] frequency(
            List<Models.Draw> history, int end, int position, int window) {
        int observed = Math.min(window, end);
        int start = end - observed;
        int[] count = new int[10];
        for (int i = start; i < end; i++) {
            int number = history.get(i).getNumberAtPosition(position);
            if (number >= 1 && number <= 10) count[number - 1]++;
        }
        double[] out = new double[10];
        for (int n = 0; n < 10; n++) {
            out[n] = FeatureContractRegistry.smoothFrequency(count[n], observed);
        }
        FeatureContractRegistry.normalize10(out);
        return out;
    }

    private static double bigBias(List<Models.Draw> history, int end, int position, int window) {
        int observed = Math.min(window, end);
        if (observed <= 0) return 0.0d;
        int big = 0;
        for (int i = end - observed; i < end; i++) {
            if (history.get(i).getNumberAtPosition(position) >= 6) big++;
        }
        return ((double) big) / observed - 0.5d;
    }

    private static int run(ArrayList<String> states, String current) {
        int count = 0;
        for (int i = states.size() - 1; i >= 0; i--) {
            if (!safe(current).equals(safe(states.get(i)))) break;
            count++;
        }
        return Math.max(1, count);
    }

    private static String coarse(String fine) {
        String value = safe(fine);
        if (value.indexOf("HOT") >= 0 || value.indexOf("COMPRESSION") >= 0) return "EXPANSION";
        if (value.indexOf("REVERSAL") >= 0) return "TRANSITION";
        return "BALANCED";
    }

    private static String medium(String fine) {
        String value = safe(fine);
        if (value.indexOf("HOT") >= 0) return "TREND";
        if (value.indexOf("REVERSAL") >= 0 || value.indexOf("COMPRESSION") >= 0) return "CHANGE";
        return "OSCILLATION";
    }

    private static String checksum(
            String issue,
            int position,
            V50Models.StatePrototypeScore[] prototypes,
            String domain,
            V50Enums.Lifecycle lifecycle,
            int coarseRun,
            int mediumRun,
            int fineRun,
            double remaining,
            double hazard,
            double confidence) {
        StringBuilder value =
                new StringBuilder(MODULE_VERSION)
                        .append('|')
                        .append(safe(issue))
                        .append('|')
                        .append(position)
                        .append('|')
                        .append(safe(domain))
                        .append('|')
                        .append(lifecycle)
                        .append('|')
                        .append(coarseRun)
                        .append('|')
                        .append(mediumRun)
                        .append('|')
                        .append(fineRun)
                        .append('|')
                        .append(Double.doubleToLongBits(remaining))
                        .append('|')
                        .append(Double.doubleToLongBits(hazard))
                        .append('|')
                        .append(Double.doubleToLongBits(confidence));
        for (int i = 0; i < prototypes.length; i++) {
            value.append('|')
                    .append(prototypes[i].prototypeId)
                    .append(':')
                    .append(Double.doubleToLongBits(prototypes[i].score));
        }
        return V50ChecksumEngine.sha256(value.toString());
    }

    private static double clamp01(double value) {
        return FeatureContractRegistry.clamp01(value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class Replay {
        final ArrayList<String> fine = new ArrayList<String>();
        final ArrayList<String> medium = new ArrayList<String>();
        final ArrayList<String> coarse = new ArrayList<String>();
        int count;
        int transitions;
        double previousStrength;
        double lastStrength;
        double shortBias;
        double longBias;
        double meanRun;
        boolean previousBreak;
    }

    private static final class Point {
        final String fine;
        final double strength;
        final double shortBias;
        final double longBias;
        final boolean breakLike;

        Point(String fine, double strength, double shortBias, double longBias, boolean breakLike) {
            this.fine = fine;
            this.strength = strength;
            this.shortBias = shortBias;
            this.longBias = longBias;
            this.breakLike = breakLike;
        }
    }
}
