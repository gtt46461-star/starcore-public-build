package com.ethk3.novaonepro;

import java.util.List;

/**
 * R922 semantic materializer for the 240 registered L0 atoms (R920 class name retained for ABI).
 *
 * <p>The former V4.1 bridge registered every atom but populated many of them through a generic
 * rotation over sixteen base signals.  That made the cardinality real while leaving several names
 * semantically weak.  This owner instead builds one immutable point-in-time statistics object from
 * the profile's legal history and gives every A0-A11 slot a stage-appropriate, deterministic
 * meaning.  A9 is deliberately all zero before the target draw and remains unavailable to genome
 * selection through {@link GenesisSchemaRegistryR915.AtomSpec#predrawComputable()}.</p>
 *
 * <p>All outputs are finite [0,1] representations because the mobile genome VM has a normalized
 * numeric input contract.  Text/hash atoms use stable hash embeddings for audit/freeze identity;
 * they are not selectable as numeric score inputs.  Per-asset persistent state remains owned by
 * {@link AlgorithmGenomeVmR908.State}; this class never mutates state.</p>
 */
public final class GenesisAtomicWorldR920 {
    public static final String VERSION = "R922_SEMANTIC_ATOMIC_WORLD_V2";
    private static final int[] WINDOWS = {12, 24, 60, 120, 480};
    private static volatile CachedStats cache;

    private GenesisAtomicWorldR920() {}

    public static double[] materialize(
            HistoricalStrategyInventionR908.Profile profile,
            V51Models.PositionAction action,
            int historyRows,
            String historyHash,
            long elapsedMs) {
        return materializeObserved(profile,action,historyRows,historyHash,elapsedMs).values;
    }

    public static AtomCaptureR932.Values materializeObserved(HistoricalStrategyInventionR908.Profile profile,
            V51Models.PositionAction action,int historyRows,String historyHash,long elapsedMs) {
        if (profile == null || !profile.ready || action == null)
            throw new IllegalArgumentException("R920_ATOMIC_WORLD_INPUT");
        Stats s = stats(profile);
        EventStats e = EventStats.of(s, action);
        AtomCaptureR932.Values observed=new AtomCaptureR932.Values(GenesisSchemaRegistryR915.atomCount(),s.latestIssue);
        double[] out = observed.values;
        for (int index = 0; index < out.length; index++) {
            int group = index / 20;
            int local = index % 20;
            GenesisSchemaRegistryR915.AtomSpec spec=GenesisSchemaRegistryR915.atoms().get(index);
            if(spec.settlementEvidence()){observed.missingReasons[index]="POSTDRAW_NOT_AVAILABLE_PREDRAW";continue;}
            long started=System.nanoTime();
            double raw=value(group, local, s, e, action,Math.max(historyRows,s.n),safe(historyHash),Math.max(0L,elapsedMs));
            observed.costNs[index]=Math.max(0L,System.nanoTime()-started);
            out[index]=clamp01(raw);
            observed.present[index]=Double.isFinite(raw)&&s.n>=spec.minSupport;
            observed.missingReasons[index]=!Double.isFinite(raw)?"NONFINITE":s.n<spec.minSupport?"HISTORY_SUPPORT_INSUFFICIENT":"";
            if(spec.semanticTier().equals(GenesisSchemaRegistryR915.TIER_RESEARCH_PROXY)){observed.present[index]=false;observed.missingReasons[index]="PROXY_SOURCE_UNAVAILABLE";}
        }
        // The boundary is asserted again here so later refactors cannot accidentally fill A9.
        for (int i = 180; i < 200; i++) {
            if (out[i] != 0.0d) throw new IllegalStateException("R922_A9_PREDRAW_NONZERO:" + i);
        }
        return observed;
    }

    private static Stats stats(HistoricalStrategyInventionR908.Profile profile) {
        CachedStats seen = cache;
        if (seen != null && seen.profile == profile) return seen.stats;
        synchronized (GenesisAtomicWorldR920.class) {
            seen = cache;
            if (seen != null && seen.profile == profile) return seen.stats;
            Stats built = new Stats(profile.historyForGenesisR920());
            cache = new CachedStats(profile, built);
            return built;
        }
    }

    private static double value(
            int group, int local, Stats s, EventStats e, V51Models.PositionAction a,
            int historyRows, String historyHash, long elapsedMs) {
        switch (group) {
            case 0: return truthAtom(local, s, historyRows, historyHash);
            case 1: return coordinateAtom(local, s, e, a);
            case 2: return temporalAtom(local, s, e);
            case 3: return topologyAtom(local, s, e, a);
            case 4: return transformAtom(local, s, e, a);
            case 5: return informationAtom(local, s, e, a);
            case 6: return memoryAtom(local, s, e, a);
            case 7: return riskAtom(local, s, e, a);
            case 8: return actionAtom(local, s, e, a);
            case 9: return 0.0d;
            case 10: return runtimeAtom(local, s, elapsedMs, historyRows, historyHash);
            case 11: return metaAtom(local, s, e, a);
            default: return 0.0d;
        }
    }

    private static double truthAtom(int k, Stats s, int historyRows, String historyHash) {
        switch (k) {
            case 0: return hash01(s.latestIssue);
            case 1: return hash01(s.previousIssue);
            case 2: return timeOfDay(s.latestFetchTime);
            case 3: return timeOfDay(s.latestOpenTime + 60_000L);
            case 4: return hash01(s.latestIssue + "|TRAINED_UNTIL");
            case 5: return clamp01(s.contiguous / 480.0d);
            case 6: return s.sourceConsistency;
            case 7: return s.dirtyRate;
            case 8: return s.lateRate;
            case 9: return clamp01(s.meanLatencyMs / 120_000.0d);
            case 10: return clamp01(s.meanLatencyMs / 30_000.0d);
            case 11: return hash01(GenesisSchemaRegistryR915.fingerprint());
            case 12: return hash01(historyHash);
            case 13: return hash01(s.latestSource);
            case 14: return clamp01(Math.min(historyRows, s.n) / 480.0d);
            case 15: return 1.0d - s.duplicateRate;
            case 16: return s.sourceConsistency;
            case 17: return s.clockMonotonicity;
            case 18: return hash01(historyHash + '|' + s.n + '|' + s.latestIssue);
            case 19: return s.dataQuality;
            default: return 0.0d;
        }
    }

    private static double coordinateAtom(int k, Stats s, EventStats e, V51Models.PositionAction a) {
        int[] n = numbers(a);
        double meanNumber = (n[0] + n[1] + n[2] - 3.0d) / 27.0d;
        double meanCurrentPosition = 0.0d;
        double meanPositionDistance = 0.0d;
        double meanCyclicDistance = 0.0d;
        for (int one : n) {
            int current = s.lastPosition[one - 1];
            meanCurrentPosition += (current - 1) / 9.0d;
            meanPositionDistance += Math.abs(current - a.position) / 9.0d;
            int raw = Math.abs(current - a.position);
            meanCyclicDistance += Math.min(raw, 10 - raw) / 5.0d;
        }
        meanCurrentPosition /= 3.0d;
        meanPositionDistance /= 3.0d;
        meanCyclicDistance /= 3.0d;
        switch (k) {
            case 0: return (a.position - 1) / 9.0d;
            case 1: return meanNumber;
            case 2: return e.lastHit;
            case 3: return meanCurrentPosition;
            case 4: return clamp01((a.rank - 1) / 1199.0d);
            case 5: return 1.0d - clamp01((a.rank - 1) / 1199.0d);
            case 6: return meanPositionDistance;
            case 7: return (max3(n) - min3(n)) / 9.0d;
            case 8: return meanCyclicDistance;
            case 9: return relativeOrderScore(s, n);
            case 10: return 1.0d;
            case 11: return e.recentRate;
            case 12: return hash01(n[0] + ":" + n[1]);
            case 13: return hash01(n[0] + ":" + n[1] + ":" + n[2]);
            case 14: return hash01(a.position + ":" + n[0] + ":" + n[1] + ":" + n[2]);
            case 15: return hash01(a.position + ":" + s.lastPosition[n[0] - 1]);
            case 16: return hash01(n[0] + ":" + n[2]);
            case 17: return hash01(a.position + ":" + s.lastPosition[n[0] - 1]
                    + ":" + s.lastPosition[n[1] - 1]);
            case 18: return hash01(n[0] + ":" + n[1] + ":" + n[2] + ":TRIPLE");
            case 19: return hash01(s.latestPermutation);
            default: return 0.0d;
        }
    }

    private static double temporalAtom(int k, Stats s, EventStats e) {
        switch (k) {
            case 0: return clamp01(e.lastGap / 120.0d);
            case 1: return clamp01(e.lastGap / 60.0d);
            case 2: return clamp01(e.meanInterval / 60.0d);
            case 3: return clamp01(e.currentRun / 60.0d);
            case 4: return clamp01(e.meanInterval / 120.0d);
            case 5: return clamp01(e.firstPassage / Math.max(1.0d, s.n));
            case 6: return clamp01(e.count60 / 60.0d);
            case 7: return e.longRate;
            case 8: return Math.exp(-e.lastGap / 24.0d);
            case 9: return e.temporalOrder;
            case 10: return clamp01(0.5d + 2.5d * (e.recentRate - e.longRate));
            case 11: return clamp01(0.5d + 3.0d * (e.rate12 - e.rate60));
            case 12: return clamp01(0.5d + 3.0d * (e.rate60 - e.rate120));
            case 13: return e.changeDuration;
            case 14: return 1.0d - e.changeDuration;
            case 15: return clamp01(Math.abs(e.lastGap - e.meanInterval) / 60.0d);
            case 16: return clamp01(Math.log(2.0d) * e.meanInterval / 120.0d);
            case 17: return clamp01(e.intervalCv / 2.0d);
            case 18: return clamp01(e.longRate * Math.exp(-e.lastGap / 60.0d) / 0.30d);
            case 19: return clamp01(e.eventCount / 120.0d);
            default: return 0.0d;
        }
    }

    private static double topologyAtom(int k, Stats s, EventStats e, V51Models.PositionAction a) {
        double directed = e.transitionSupport;
        double reverse = e.reverseTransitionSupport;
        double migration = e.migrationSupport;
        switch (k) {
            case 0: return directed;
            case 1: return 0.5d * (directed + reverse);
            case 2: return clamp01(directed * Math.sqrt(Math.max(0.0d, e.support)));
            case 3: return clamp01(e.lastGap / 120.0d);
            case 4: return e.meanPositionDistance;
            case 5: return e.twoStepSupport;
            case 6: return e.threeStepSupport;
            case 7: return e.cycleMembership;
            case 8: return e.cycleLength;
            case 9: return migration;
            case 10: return e.outflow;
            case 11: return clamp01(0.5d + 0.5d * (migration - e.outflow));
            case 12: return clamp01(e.collisionPressure);
            case 13: return e.swapSupport;
            case 14: return 1.0d - e.memberSimilarity;
            case 15: return e.longRate;
            case 16: return clamp01(directed * e.support / 0.30d);
            case 17: return e.movementDiversity;
            case 18: return 1.0d - e.memberPositionSpread;
            case 19: return communityScore(s, a);
            default: return 0.0d;
        }
    }

    private static double transformAtom(int k, Stats s, EventStats e, V51Models.PositionAction a) {
        double x = e.transitionSupport;
        double y = e.migrationSupport;
        double z = e.recentRate;
        double w = e.longRate;
        switch (k) {
            case 0: return signed01(x - y);
            case 1: return signed01(e.rate12 - 2.0d * e.rate60 + e.rate120);
            case 2: return clamp01(x / Math.max(0.03d, y) / 4.0d);
            case 3: return clamp01(Math.log1p(9.0d * x) / Math.log(10.0d));
            case 4: return sigmoid(4.0d * (x - 0.5d));
            case 5: return sigmoid(6.0d * (x - y));
            case 6: return clamp01(x);
            case 7: return normalize3(x, y, z);
            case 8: return signed01((x - e.memberMean) / Math.max(0.05d, e.memberSd));
            case 9: return clamp01((a.rank - 1) / 1199.0d);
            case 10: return clamp01((x + y + z) / 3.0d);
            case 11: return clamp01(0.55d * e.rate12 + 0.30d * e.rate60 + 0.15d * e.rate120);
            case 12: return clamp01(e.fastTrace + 0.5d * e.slowTrace);
            case 13: return Math.min(x, Math.min(y, z));
            case 14: return Math.max(x, Math.max(y, z));
            case 15: return clamp01(x * y);
            case 16: return signed01((x - 0.30d) * (y - 0.30d) * 4.0d);
            case 17: return signed01(z - w);
            case 18: return signed01((z - w) - (x - y));
            case 19: return clamp01((x + y + z + w) / 4.0d);
            default: return 0.0d;
        }
    }

    private static double informationAtom(int k, Stats s, EventStats e, V51Models.PositionAction a) {
        double p = clamp(e.longRate, 1e-6d, 1.0d - 1e-6d);
        double q = clamp(a.rawActionP, 1e-6d, 1.0d - 1e-6d);
        double posterior = (e.eventCount + 1.0d) / (e.observations + 2.0d);
        switch (k) {
            case 0: return clamp01(e.eventCount / 120.0d);
            case 1: return e.longRate;
            case 2: return clamp01(e.pairSupport);
            case 3: return e.transitionSupport;
            case 4: return posterior;
            case 5: return clamp01(posterior * (1.0d - posterior) / 0.25d);
            case 6: return entropy3(e.memberValues[0], e.memberValues[1], e.memberValues[2]);
            case 7: return e.transitionEntropy;
            case 8: return clamp01(-(p * Math.log(q) + (1.0d - p) * Math.log(1.0d - q)) / 2.5d);
            case 9: return clamp01(Math.abs(e.transitionSupport - e.longRate) * e.support * 3.0d);
            case 10: return clamp01(Math.abs(e.rate12 - e.rate120) * e.transitionSupport * 3.0d);
            case 11: return jsBernoulli(e.recentRate, e.longRate);
            case 12: return max3(e.memberValues);
            case 13: return clamp01(e.observations / 120.0d);
            case 14: return min3(e.memberValues);
            case 15: return clamp01(Math.abs(e.recentRate - e.longRate) / 0.30d);
            case 16: return signed01(e.recentRate - e.longRate);
            case 17: return clamp01(Math.exp(-Math.abs(e.recentRate - 0.30d) * e.observations / 8.0d));
            case 18: return clamp01(e.memberSd / 0.50d);
            case 19: return clamp01(jsBernoulli(e.recentRate, e.longRate)
                    + Math.abs(e.transitionSupport - e.longRate));
            default: return 0.0d;
        }
    }

    private static double memoryAtom(int k, Stats s, EventStats e, V51Models.PositionAction a) {
        switch (k) {
            case 0: return e.fastTrace;
            case 1: return e.slowTrace;
            case 2: return e.migrationSupport;
            case 3: return e.fastTrace;
            case 4: return e.slowTrace;
            case 5: return e.motifSupport;
            case 6: return clamp01((e.recentRate + e.transitionSupport) * 0.5d);
            case 7: return clamp01((e.fastTrace + e.slowTrace + e.motifSupport) / 3.0d);
            case 8: return s.positionRegime[a.position - 1];
            case 9: return 1.0d - e.fastTrace;
            case 10: return e.fastTrace;
            case 11: return signed01(e.recentRate - a.rawActionP);
            case 12: return clamp01(e.lastGap / 120.0d);
            case 13: return 1.0d - clamp01(e.intervalCv / 2.0d);
            case 14: return clamp01(Math.exp(-1.0d / Math.max(1.0d, e.meanInterval)));
            case 15: return e.intervalCv > 1.25d ? 1.0d : 0.0d;
            case 16: return e.support >= 0.20d ? 1.0d : 0.0d;
            case 17: return e.support;
            case 18: return hash01(VERSION);
            case 19: return clamp01(e.lastGap / Math.max(1.0d, s.n));
            default: return 0.0d;
        }
    }

    private static double riskAtom(int k, Stats s, EventStats e, V51Models.PositionAction a) {
        double entropy = clamp01(a.entropy);
        double margin = clamp01(Math.abs(a.rankMargin) * 10.0d);
        double supportUncertainty = 1.0d - e.support;
        double drift = clamp01(Math.abs(e.recentRate - e.longRate) / 0.30d);
        double instability = clamp01(1.0d - a.stability);
        switch (k) {
            case 0: return entropy;
            case 1: return margin;
            case 2: return supportUncertainty;
            case 3: return clamp01(e.meanPositionDistance);
            case 4: return clamp01(e.memberSd / 0.50d);
            case 5: return clamp01(Math.abs(e.transitionSupport - e.migrationSupport));
            case 6: return clamp01(0.45d * supportUncertainty + 0.35d * drift + 0.20d * entropy);
            case 7: return signed01(a.rawActionP - e.longRate);
            case 8: return clamp01((a.rawActionP - e.lastHit) * (a.rawActionP - e.lastHit));
            case 9: return wilsonLower(e.eventCount, e.observations);
            case 10: return wilsonUpper(e.eventCount, e.observations);
            case 11: return signed01(e.longRate - 0.30d);
            case 12: return drift;
            case 13: return clamp01(e.memberSd + Math.abs(e.transitionSupport - e.longRate));
            case 14: return instability;
            case 15: return clamp01(e.maxMiss / 30.0d);
            case 16: return clamp01(0.6d * instability + 0.4d * supportUncertainty);
            case 17: return 1.0d - s.dataQuality;
            case 18: return 1.0d - s.positionRegime[a.position - 1];
            case 19: return (entropy > 0.72d || supportUncertainty > 0.70d || instability > 0.65d) ? 1.0d : 0.0d;
            default: return 0.0d;
        }
    }

    private static double actionAtom(int k, Stats s, EventStats e, V51Models.PositionAction a) {
        int[] n = numbers(a);
        double diversity = (max3(n) - min3(n)) / 9.0d;
        double weakest = min3(e.memberValues);
        double dependence = clamp01(1.0d - e.memberSd / 0.50d);
        switch (k) {
            case 0: return (a.position - 1) / 9.0d;
            case 1: return hash01(n[0] + ":" + n[1] + ":" + n[2]);
            case 2: return e.memberMean;
            case 3: return weakest;
            case 4: return clamp01(Math.abs(a.thirdMargin) * 10.0d);
            case 5: return clamp01((1.0d - weakest) * (1.0d - diversity));
            case 6: return diversity;
            case 7: return dependence;
            case 8: return clamp01(1.0d - e.longRate);
            case 9: return clamp01(1.0d / 1199.0d * 64.0d);
            case 10: return e.support;
            case 11: return clamp01((a.rank - 1) / 1199.0d);
            case 12: return clamp01(a.stability);
            case 13: return clamp01(1.0d - a.stability + Math.abs(a.rankMargin) * 4.0d);
            case 14: return clamp01(a.rawActionP);
            case 15: return clamp01(0.65d * a.rawActionP + 0.35d * e.longRate);
            case 16: return (a.entropy > 0.72d || e.support < 0.18d || a.stability < 0.35d) ? 1.0d : 0.0d;
            case 17: return clamp01(1.0d - Math.abs(s.positionRegime[a.position - 1] - e.recentRate));
            case 18: return hash01(a.position + "|" + bucket(a.rawActionP) + "|" + bucket(a.stability)
                    + "|" + bucket(a.entropy));
            case 19: return signed01(e.recentRate - weakest);
            default: return 0.0d;
        }
    }

    private static double runtimeAtom(int k, Stats s, long elapsedMs, int historyRows, String hash) {
        switch (k) {
            case 0: return clamp01(elapsedMs / 5000.0d);
            case 1: return 0.0d;
            case 2: return clamp01(elapsedMs / 2200.0d);
            case 3: return 0.0d;
            case 4: return clamp01(240.0d * 8.0d / 65536.0d);
            case 5: return clamp01(Math.min(historyRows, 480) / 480.0d);
            case 6: return 0.0d;
            case 7: return 0.0d;
            case 8: return 0.0d;
            case 9: return 0.0d;
            case 10: return 0.0d;
            case 11: return 0.0d;
            case 12: return 0.0d;
            case 13: return 0.25d;
            case 14: return 1.0d;
            case 15: return clamp01(elapsedMs / 5000.0d);
            case 16: return clamp01(s.meanLatencyMs / 120_000.0d);
            case 17: return 1.0d;
            case 18: return 0.0d;
            case 19: return clamp01(0.55d * s.dataQuality + 0.45d * (1.0d - s.lateRate));
            default: return hash01(hash);
        }
    }

    private static double metaAtom(int k, Stats s, EventStats e, V51Models.PositionAction a) {
        double lift = signed01(e.longRate - 0.30d);
        double novelty = clamp01(e.memberSd + Math.abs(e.transitionSupport - e.migrationSupport));
        switch (k) {
            case 0: return clamp01(Math.abs(e.recentRate - e.longRate));
            case 1: return 1.0d - novelty;
            case 2: return lift;
            case 3: return clamp01(1.0d - Math.abs(e.transitionSupport - e.migrationSupport));
            case 4: return clamp01(Math.abs(e.transitionSupport - e.migrationSupport));
            case 5: return novelty;
            case 6: return hash01(bucket(e.recentRate) + "|" + bucket(a.entropy));
            case 7: return hash01("AF|" + bucket(e.transitionSupport) + '|' + bucket(e.migrationSupport));
            case 8: return signed01(e.lastHit - a.rawActionP);
            case 9: return clamp01(Math.abs(a.rankMargin) * 10.0d);
            case 10: return 0.0d;
            case 11: return 0.0d;
            case 12: return 0.0d;
            case 13: return clamp01(1.0d - Math.abs(s.positionRegime[a.position - 1] - e.recentRate));
            case 14: return e.support;
            case 15: return clamp01(a.stability);
            case 16: return clamp01((e.support + a.stability) * 0.5d);
            case 17: return 0.0d;
            case 18: return hash01((e.longRate < 0.22d ? "LOW_HIT" : e.maxMiss > 12 ? "STREAK" : "DRIFT"));
            case 19: return clamp01(e.fastTrace - e.slowTrace + 0.5d);
            default: return 0.0d;
        }
    }

    private static final class CachedStats {
        final HistoricalStrategyInventionR908.Profile profile;
        final Stats stats;
        CachedStats(HistoricalStrategyInventionR908.Profile profile, Stats stats) {
            this.profile = profile;
            this.stats = stats;
        }
    }

    private static final class Stats {
        final List<Models.Draw> history;
        final int n, contiguous;
        final String latestIssue, previousIssue, latestSource, latestPermutation;
        final long latestOpenTime, latestFetchTime;
        final double sourceConsistency, dirtyRate, lateRate, duplicateRate, clockMonotonicity;
        final double meanLatencyMs, dataQuality;
        final double[][][] frequency = new double[WINDOWS.length][10][10];
        final int[][] gap = new int[10][10];
        final double[][] meanInterval = new double[10][10];
        final double[][] intervalCv = new double[10][10];
        final double[][][] transition = new double[10][10][10];
        final double[][][] migration = new double[10][10][10];
        final int[] lastPosition = new int[10];
        final int[] previousPosition = new int[10];
        final double[] positionEntropy = new double[10];
        final double[] positionRegime = new double[10];

        Stats(List<Models.Draw> source) {
            history = source;
            n = source == null ? 0 : source.size();
            Models.Draw latest = n == 0 ? null : source.get(n - 1);
            Models.Draw previous = n < 2 ? null : source.get(n - 2);
            latestIssue = latest == null ? "" : safe(latest.issue);
            previousIssue = previous == null ? "" : safe(previous.issue);
            latestSource = latest == null ? "" : safe(latest.sourceLine);
            latestOpenTime = latest == null ? 0L : latest.openTime;
            latestFetchTime = latest == null ? 0L : latest.fetchTime;
            latestPermutation = latest == null ? "" : latest.numbersText();

            int clean = 0, dirty = 0, late = 0, duplicate = 0, monotonic = 0, sameSource = 0;
            long latencyTotal = 0L;
            String priorIssue = "";
            long priorTime = Long.MIN_VALUE;
            for (int i = 0; i < n; i++) {
                Models.Draw d = source.get(i);
                if (d == null || !d.isValid() || d.dirty != 0) dirty++; else clean++;
                if (d != null && safe(d.sourceLine).equals(latestSource)) sameSource++;
                if (d != null && d.fetchTime > 0L && d.openTime > 0L) {
                    long latency = Math.max(0L, d.fetchTime - d.openTime);
                    latencyTotal += latency;
                    if (latency > 120_000L) late++;
                }
                String issue = d == null ? "" : safe(d.issue);
                if (issue.equals(priorIssue) && issue.length() > 0) duplicate++;
                if (d != null && (priorTime == Long.MIN_VALUE || d.openTime >= priorTime)) monotonic++;
                priorIssue = issue;
                if (d != null) priorTime = d.openTime;
            }
            sourceConsistency = n == 0 ? 0.0d : sameSource / (double)n;
            dirtyRate = n == 0 ? 1.0d : dirty / (double)n;
            lateRate = n == 0 ? 1.0d : late / (double)n;
            duplicateRate = n == 0 ? 1.0d : duplicate / (double)n;
            clockMonotonicity = n == 0 ? 0.0d : monotonic / (double)n;
            meanLatencyMs = n == 0 ? 0.0d : latencyTotal / (double)n;
            contiguous = contiguousTail(source);
            dataQuality = clamp01(0.30d * (clean / (double)Math.max(1, n))
                    + 0.20d * sourceConsistency + 0.20d * (1.0d - duplicateRate)
                    + 0.15d * clockMonotonicity + 0.15d * clamp01(contiguous / 120.0d));

            buildFrequency(source);
            buildIntervals(source);
            buildTransitions(source);
            buildMigration(source);
            if (latest != null) for (int number = 1; number <= 10; number++)
                lastPosition[number - 1] = findPosition(latest, number);
            if (previous != null) for (int number = 1; number <= 10; number++)
                previousPosition[number - 1] = findPosition(previous, number);
            for (int p = 0; p < 10; p++) {
                positionEntropy[p] = entropy(frequency[4][p]);
                double drift = 0.0d;
                for (int number = 0; number < 10; number++)
                    drift += Math.abs(frequency[1][p][number] - frequency[3][p][number]);
                positionRegime[p] = clamp01(0.5d * drift);
            }
        }

        private void buildFrequency(List<Models.Draw> rows) {
            for (int wi = 0; wi < WINDOWS.length; wi++) {
                int start = Math.max(0, n - WINDOWS[wi]);
                int count = Math.max(0, n - start);
                for (int i = start; i < n; i++) {
                    Models.Draw d = rows.get(i);
                    if (d == null || !d.isValid()) continue;
                    for (int p = 1; p <= 10; p++) {
                        int number = d.getNumberAtPosition(p);
                        if (number >= 1 && number <= 10) frequency[wi][p - 1][number - 1]++;
                    }
                }
                if (count > 0) for (int p = 0; p < 10; p++) for (int number = 0; number < 10; number++)
                    frequency[wi][p][number] /= count;
            }
        }

        private void buildIntervals(List<Models.Draw> rows) {
            for (int p = 1; p <= 10; p++) for (int number = 1; number <= 10; number++) {
                int last = -1, intervals = 0;
                double sum = 0.0d, sumSq = 0.0d;
                for (int i = 0; i < n; i++) if (rows.get(i).getNumberAtPosition(p) == number) {
                    if (last >= 0) {
                        double one = i - last;
                        sum += one; sumSq += one * one; intervals++;
                    }
                    last = i;
                }
                gap[p - 1][number - 1] = last < 0 ? Math.min(480, n) : n - 1 - last;
                double mean = intervals == 0 ? Math.max(1.0d, n) : sum / intervals;
                meanInterval[p - 1][number - 1] = mean;
                double variance = intervals == 0 ? 0.0d : Math.max(0.0d, sumSq / intervals - mean * mean);
                intervalCv[p - 1][number - 1] = Math.sqrt(variance) / Math.max(1.0d, mean);
            }
        }

        private void buildTransitions(List<Models.Draw> rows) {
            double[][][] count = new double[10][10][10];
            double[][] total = new double[10][10];
            for (int p = 0; p < 10; p++) for (int from = 0; from < 10; from++)
                for (int to = 0; to < 10; to++) count[p][from][to] = 0.25d;
            for (int p = 0; p < 10; p++) for (int from = 0; from < 10; from++) total[p][from] = 2.5d;
            for (int i = 1; i < n; i++) for (int p = 1; p <= 10; p++) {
                int from = rows.get(i - 1).getNumberAtPosition(p) - 1;
                int to = rows.get(i).getNumberAtPosition(p) - 1;
                if (from >= 0 && to >= 0) { count[p - 1][from][to]++; total[p - 1][from]++; }
            }
            for (int p = 0; p < 10; p++) for (int from = 0; from < 10; from++)
                for (int to = 0; to < 10; to++) transition[p][from][to] = count[p][from][to] / total[p][from];
        }

        private void buildMigration(List<Models.Draw> rows) {
            double[][][] count = new double[10][10][10];
            double[][] total = new double[10][10];
            for (int number = 0; number < 10; number++) for (int from = 0; from < 10; from++) {
                total[number][from] = 2.5d;
                for (int to = 0; to < 10; to++) count[number][from][to] = 0.25d;
            }
            for (int i = 1; i < n; i++) for (int number = 1; number <= 10; number++) {
                int from = findPosition(rows.get(i - 1), number) - 1;
                int to = findPosition(rows.get(i), number) - 1;
                if (from >= 0 && to >= 0) { count[number - 1][from][to]++; total[number - 1][from]++; }
            }
            for (int number = 0; number < 10; number++) for (int from = 0; from < 10; from++)
                for (int to = 0; to < 10; to++) migration[number][from][to] = count[number][from][to] / total[number][from];
        }
    }

    private static final class EventStats {
        final int observations, eventCount, count60, lastGap, currentRun, maxMiss, firstPassage;
        final double rate12, recentRate, rate60, rate120, longRate, meanInterval, intervalCv;
        final double temporalOrder, changeDuration, fastTrace, slowTrace, support, lastHit;
        final double transitionSupport, reverseTransitionSupport, twoStepSupport, threeStepSupport;
        final double migrationSupport, outflow, meanPositionDistance, cycleMembership, cycleLength;
        final double collisionPressure, swapSupport, memberSimilarity, movementDiversity;
        final double memberPositionSpread, motifSupport, pairSupport, transitionEntropy;
        final double memberMean, memberSd;
        final double[] memberValues;

        private EventStats(Stats s, V51Models.PositionAction a) {
            int[] members = numbers(a);
            observations = Math.min(120, s.n);
            int start = Math.max(0, s.n - observations);
            int events = 0, recent = 0, c60 = 0, first = s.n, last = -1, prior = -1;
            int misses = 0, max = 0;
            double intervalSum = 0.0d, intervalSq = 0.0d; int intervalN = 0;
            double fast = 0.30d, slow = 0.30d;
            for (int i = 0; i < s.n; i++) {
                int actual = s.history.get(i).getNumberAtPosition(a.position);
                boolean hit = contains(members, actual);
                fast = 0.25d * (hit ? 1.0d : 0.0d) + 0.75d * fast;
                slow = 0.06d * (hit ? 1.0d : 0.0d) + 0.94d * slow;
                if (i >= start) {
                    if (hit) { events++; misses = 0; } else { misses++; max = Math.max(max, misses); }
                }
                if (i >= s.n - 24 && hit) recent++;
                if (i >= s.n - 60 && hit) c60++;
                if (hit) {
                    if (first == s.n) first = i;
                    prior = last;
                    if (last >= 0) { double one = i - last; intervalSum += one; intervalSq += one * one; intervalN++; }
                    last = i;
                }
            }
            eventCount = events; count60 = c60; lastGap = last < 0 ? Math.min(480, s.n) : s.n - 1 - last;
            currentRun = lastGap; maxMiss = max; firstPassage = first == s.n ? s.n : first;
            rate12 = unionFrequency(s, 0, a.position, members);
            recentRate = recent / (double)Math.max(1, Math.min(24, s.n));
            rate60 = unionFrequency(s, 2, a.position, members);
            rate120 = unionFrequency(s, 3, a.position, members);
            longRate = unionFrequency(s, 4, a.position, members);
            meanInterval = intervalN == 0 ? Math.max(1.0d, s.n) : intervalSum / intervalN;
            double variance = intervalN == 0 ? 0.0d : Math.max(0.0d, intervalSq / intervalN - meanInterval * meanInterval);
            intervalCv = Math.sqrt(variance) / Math.max(1.0d, meanInterval);
            temporalOrder = prior < 0 || last < 0 ? 0.5d : clamp01((last - prior) / 60.0d);
            changeDuration = clamp01(Math.abs(recentRate - rate120) / 0.30d);
            fastTrace = clamp01(fast); slowTrace = clamp01(slow);
            support = clamp01(observations / 120.0d); lastHit = lastGap == 0 ? 1.0d : 0.0d;

            int currentAtPosition = s.n == 0 ? 1 : s.history.get(s.n - 1).getNumberAtPosition(a.position);
            double direct = 0.0d, reverse = 0.0d, two = 0.0d, three = 0.0d, mig = 0.0d;
            double move = 0.0d, out = 0.0d, swap = 0.0d, cycle = 0.0d, cycleLen = 0.0d;
            double[] values = new double[3];
            for (int j = 0; j < members.length; j++) {
                int number = members[j], to = number - 1;
                values[j] = s.frequency[3][a.position - 1][to];
                direct += s.transition[a.position - 1][currentAtPosition - 1][to];
                reverse += s.transition[a.position - 1][to][currentAtPosition - 1];
                two += matrixPower(s.transition[a.position - 1], currentAtPosition - 1, to, 2);
                three += matrixPower(s.transition[a.position - 1], currentAtPosition - 1, to, 3);
                int from = Math.max(1, s.lastPosition[to]);
                int previousPosition = Math.max(1, s.previousPosition[to]);
                mig += s.migration[to][from - 1][a.position - 1];
                double outgoing = 0.0d;
                for (int p = 0; p < 10; p++) if (p != from - 1) outgoing += s.migration[to][from - 1][p];
                out += outgoing;
                move += Math.abs(from - a.position) / 9.0d;
                if (previousPosition == a.position && from != a.position) swap += 1.0d;
                if (previousPosition == a.position && from == a.position) cycle += 1.0d;
                cycleLen += Math.min(1.0d, Math.abs(previousPosition - from) / 9.0d);
            }
            memberValues = values;
            memberMean = (values[0] + values[1] + values[2]) / 3.0d;
            memberSd = Math.sqrt(((values[0] - memberMean) * (values[0] - memberMean)
                    + (values[1] - memberMean) * (values[1] - memberMean)
                    + (values[2] - memberMean) * (values[2] - memberMean)) / 3.0d);
            transitionSupport = clamp01(direct / 3.0d);
            reverseTransitionSupport = clamp01(reverse / 3.0d);
            twoStepSupport = clamp01(two / 3.0d);
            threeStepSupport = clamp01(three / 3.0d);
            migrationSupport = clamp01(mig / 3.0d);
            outflow = clamp01(out / 3.0d);
            meanPositionDistance = clamp01(move / 3.0d);
            cycleMembership = clamp01(cycle / 3.0d);
            cycleLength = clamp01(cycleLen / 3.0d);
            collisionPressure = clamp01((1.0d - memberSd) * (1.0d - longRate));
            swapSupport = clamp01(swap / 3.0d);
            memberSimilarity = clamp01(1.0d - memberSd / 0.50d);
            movementDiversity = movementDiversity(s, members);
            memberPositionSpread = positionSpread(s, members);
            motifSupport = clamp01((transitionSupport + migrationSupport + recentRate) / 3.0d);
            pairSupport = clamp01((values[0] * values[1] + values[0] * values[2]
                    + values[1] * values[2]) / 3.0d * 4.0d);
            transitionEntropy = entropy(s.transition[a.position - 1][currentAtPosition - 1]);
        }

        static EventStats of(Stats stats, V51Models.PositionAction action) {
            return new EventStats(stats, action);
        }
    }

    private static int contiguousTail(List<Models.Draw> history) {
        if (history == null || history.isEmpty()) return 0;
        int count = 1;
        for (int i = history.size() - 1; i > 0; i--) {
            Models.Draw previous = history.get(i - 1), current = history.get(i);
            if (previous == null || current == null) break;
            TargetIssueResolver.Resolution next = TargetIssueResolver.next(safe(previous.issue));
            if (!next.valid || !safe(current.issue).equals(next.targetIssue)) break;
            count++;
        }
        return count;
    }

    private static double unionFrequency(Stats s, int windowIndex, int position, int[] members) {
        double sum = 0.0d;
        for (int number : members) sum += s.frequency[windowIndex][position - 1][number - 1];
        return clamp01(sum);
    }

    private static double relativeOrderScore(Stats s, int[] members) {
        int a = s.lastPosition[members[0] - 1], b = s.lastPosition[members[1] - 1], c = s.lastPosition[members[2] - 1];
        int concordant = (a < b ? 1 : 0) + (a < c ? 1 : 0) + (b < c ? 1 : 0);
        return concordant / 3.0d;
    }

    private static double communityScore(Stats s, V51Models.PositionAction action) {
        int[] members = numbers(action); double score = 0.0d;
        for (int number : members) {
            boolean sameHalf = (s.lastPosition[number - 1] <= 5) == (action.position <= 5);
            if (sameHalf) score += 1.0d;
        }
        return score / 3.0d;
    }

    private static double movementDiversity(Stats s, int[] members) {
        boolean[] seen = new boolean[10]; int count = 0;
        for (int number : members) {
            int p = Math.max(1, s.lastPosition[number - 1]) - 1;
            if (!seen[p]) { seen[p] = true; count++; }
        }
        return count / 3.0d;
    }

    private static double positionSpread(Stats s, int[] members) {
        int a = s.lastPosition[members[0] - 1], b = s.lastPosition[members[1] - 1], c = s.lastPosition[members[2] - 1];
        return (Math.max(a, Math.max(b, c)) - Math.min(a, Math.min(b, c))) / 9.0d;
    }

    private static double matrixPower(double[][] matrix, int from, int to, int power) {
        double[] row = new double[10]; row[from] = 1.0d;
        for (int step = 0; step < power; step++) {
            double[] next = new double[10];
            for (int i = 0; i < 10; i++) for (int j = 0; j < 10; j++) next[j] += row[i] * matrix[i][j];
            row = next;
        }
        return clamp01(row[to]);
    }

    private static int findPosition(Models.Draw draw, int number) {
        if (draw == null) return 0;
        for (int p = 1; p <= 10; p++) if (draw.getNumberAtPosition(p) == number) return p;
        return 0;
    }

    private static int[] numbers(V51Models.PositionAction action) {
        return new int[]{action.n1, action.n2, action.n3};
    }

    private static boolean contains(int[] values, int number) {
        return values[0] == number || values[1] == number || values[2] == number;
    }

    private static double entropy(double[] values) {
        double total = 0.0d; for (double value : values) total += Math.max(0.0d, value);
        if (total <= 0.0d) return 1.0d;
        double h = 0.0d;
        for (double value : values) if (value > 0.0d) { double p = value / total; h -= p * Math.log(p); }
        return clamp01(h / Math.log(values.length));
    }

    private static double entropy3(double a, double b, double c) {
        return entropy(new double[]{a, b, c});
    }

    private static double jsBernoulli(double p, double q) {
        p = clamp(p, 1e-6d, 1.0d - 1e-6d); q = clamp(q, 1e-6d, 1.0d - 1e-6d);
        double m = 0.5d * (p + q);
        double left = p * Math.log(p / m) + (1.0d - p) * Math.log((1.0d - p) / (1.0d - m));
        double right = q * Math.log(q / m) + (1.0d - q) * Math.log((1.0d - q) / (1.0d - m));
        return clamp01(0.5d * (left + right) / Math.log(2.0d));
    }

    private static double wilsonLower(int hit, int n) {
        if (n <= 0) return 0.0d;
        double z = 1.959963984540054d, p = hit / (double)n, z2 = z * z;
        double center = p + z2 / (2.0d * n);
        double margin = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return clamp01((center - margin) / (1.0d + z2 / n));
    }

    private static double wilsonUpper(int hit, int n) {
        if (n <= 0) return 1.0d;
        double z = 1.959963984540054d, p = hit / (double)n, z2 = z * z;
        double center = p + z2 / (2.0d * n);
        double margin = z * Math.sqrt((p * (1.0d - p) + z2 / (4.0d * n)) / n);
        return clamp01((center + margin) / (1.0d + z2 / n));
    }

    private static double normalize3(double a, double b, double c) {
        double min = Math.min(a, Math.min(b, c)), max = Math.max(a, Math.max(b, c));
        return max - min < 1e-9d ? 0.5d : clamp01((a - min) / (max - min));
    }

    private static double signed01(double value) { return clamp01(0.5d + 0.5d * value); }
    private static double sigmoid(double value) { return 1.0d / (1.0d + Math.exp(-clamp(value, -20.0d, 20.0d))); }
    private static double timeOfDay(long millis) { return millis <= 0L ? 0.0d : Math.floorMod(millis, 86_400_000L) / 86_400_000.0d; }
    private static double hash01(String value) {
        String hash = V51ChecksumEngine.sha256(safe(value));
        try { return Long.parseUnsignedLong(hash.substring(0, 13), 16) / (double)0xFFFFFFFFFFFFFL; }
        catch (Throwable ignored) { return 0.0d; }
    }
    private static int bucket(double value) { return (int)Math.floor(clamp01(value) * 9.999999d); }
    private static int min3(int[] values) { return Math.min(values[0], Math.min(values[1], values[2])); }
    private static int max3(int[] values) { return Math.max(values[0], Math.max(values[1], values[2])); }
    private static double min3(double[] values) { return Math.min(values[0], Math.min(values[1], values[2])); }
    private static double max3(double[] values) { return Math.max(values[0], Math.max(values[1], values[2])); }
    private static double clamp01(double value) { return clamp(value, 0.0d, 1.0d); }
    private static double clamp(double value, double lo, double hi) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return lo;
        return Math.max(lo, Math.min(hi, value));
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
}
