package com.ethk3.novaonepro;

import java.util.HashMap;

/**
 * B09 proper-scoring expert regret ledger. Tracks log-loss, Brier, calibration, rank regret and a
 * recent ring independently.
 */
public final class V51ExpertRegretEngine {
    public static final String V57_B09_OWNER = "EXPERT_REGRET_ONLY";
    private static final int RECENT_CAPACITY = 256;
    private static final int CAL_BINS = 10;

    public static final class Snapshot {
        public final String expertId;
        public final int sample;
        public final double logLoss,
                brier,
                calibrationError,
                rankRegret,
                recentLogLoss,
                recentBrier,
                reliability,
                recentReliability;
        public final int consecutiveMislead;
        public final String checksum;

        Snapshot(String id, Stat s) {
            expertId = id;
            sample = s.count;
            logLoss = s.logLoss;
            brier = s.brier;
            calibrationError = s.ece();
            rankRegret = s.rankRegret;
            recentLogLoss = s.recentLogLoss();
            recentBrier = s.recentBrier();
            reliability = score(logLoss, brier, calibrationError, rankRegret);
            recentReliability = score(recentLogLoss, recentBrier, s.recentEce(), s.recentRank());
            consecutiveMislead = s.consecutiveMislead;
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return expertId
                    + '|'
                    + sample
                    + '|'
                    + logLoss
                    + '|'
                    + brier
                    + '|'
                    + calibrationError
                    + '|'
                    + rankRegret
                    + '|'
                    + recentLogLoss
                    + '|'
                    + recentBrier
                    + '|'
                    + reliability
                    + '|'
                    + recentReliability
                    + '|'
                    + consecutiveMislead;
        }
    }

    private static final class Stat {
        int count, consecutiveMislead, head, recentSize;
        double logLoss = 0.6931471805599453d, brier = 0.25d, rankRegret = 0.5d;
        final int[] binN = new int[CAL_BINS];
        final double[] binP = new double[CAL_BINS];
        final double[] binY = new double[CAL_BINS];
        final double[] recentP = new double[RECENT_CAPACITY],
                recentY = new double[RECENT_CAPACITY],
                recentRank = new double[RECENT_CAPACITY];

        void push(double p, double y, double rank) {
            recentP[head] = p;
            recentY[head] = y;
            recentRank[head] = rank;
            head = (head + 1) % RECENT_CAPACITY;
            if (recentSize < RECENT_CAPACITY) recentSize++;
        }

        double ece() {
            int total = 0;
            for (int i = 0; i < CAL_BINS; i++) total += binN[i];
            if (total == 0) return 1.0d;
            double e = 0;
            for (int i = 0; i < CAL_BINS; i++)
                if (binN[i] > 0) {
                    double p = binP[i] / binN[i], y = binY[i] / binN[i];
                    e += ((double) binN[i] / total) * Math.abs(p - y);
                }
            return clamp01(e);
        }

        double recentLogLoss() {
            if (recentSize == 0) return 0.6931471805599453d;
            double x = 0;
            for (int k = 0; k < recentSize; k++) {
                int i = (head - 1 - k + RECENT_CAPACITY) % RECENT_CAPACITY;
                double p = clamp(recentP[i], 1e-6, 1 - 1e-6), y = recentY[i];
                x += -y * Math.log(p) - (1 - y) * Math.log(1 - p);
            }
            return x / recentSize;
        }

        double recentBrier() {
            if (recentSize == 0) return 0.25d;
            double x = 0;
            for (int k = 0; k < recentSize; k++) {
                int i = (head - 1 - k + RECENT_CAPACITY) % RECENT_CAPACITY;
                double d = recentP[i] - recentY[i];
                x += d * d;
            }
            return x / recentSize;
        }

        double recentRank() {
            if (recentSize == 0) return 0.5d;
            double x = 0;
            for (int k = 0; k < recentSize; k++) {
                int i = (head - 1 - k + RECENT_CAPACITY) % RECENT_CAPACITY;
                x += recentRank[i];
            }
            return clamp01(x / recentSize);
        }

        double recentEce() {
            if (recentSize == 0) return 1.0d;
            int[] n = new int[CAL_BINS];
            double[] p = new double[CAL_BINS], y = new double[CAL_BINS];
            for (int k = 0; k < recentSize; k++) {
                int i = (head - 1 - k + RECENT_CAPACITY) % RECENT_CAPACITY;
                int b = Math.min(CAL_BINS - 1, (int) Math.floor(clamp01(recentP[i]) * CAL_BINS));
                n[b]++;
                p[b] += recentP[i];
                y[b] += recentY[i];
            }
            double e = 0;
            for (int b = 0; b < CAL_BINS; b++)
                if (n[b] > 0)
                    e += ((double) n[b] / recentSize) * Math.abs(p[b] / n[b] - y[b] / n[b]);
            return clamp01(e);
        }
    }

    private final HashMap<String, Stat> stats = new HashMap<String, Stat>();

    public synchronized void observe(String expertId, double probability, boolean hit) {
        observeDetailed(expertId, probability, hit, hit ? 0.0d : 1.0d);
    }

    public synchronized void observeDetailed(
            String expertId, double probability, boolean hit, double rankLoss) {
        String id = expertId == null ? "" : expertId;
        Stat s = stats.get(id);
        if (s == null) {
            s = new Stat();
            stats.put(id, s);
        }
        double p = clamp(probability, 1.0e-6d, 1.0d - 1.0e-6d),
                y = hit ? 1.0d : 0.0d,
                r = clamp01(rankLoss);
        double log = -y * Math.log(p) - (1 - y) * Math.log(1 - p), br = (p - y) * (p - y);
        s.count++;
        double a = s.count < 100 ? 1.0d / s.count : 0.02d;
        s.logLoss = (1 - a) * s.logLoss + a * log;
        s.brier = (1 - a) * s.brier + a * br;
        s.rankRegret = (1 - a) * s.rankRegret + a * r;
        int bin = Math.min(CAL_BINS - 1, (int) Math.floor(clamp01(p) * CAL_BINS));
        s.binN[bin]++;
        s.binP[bin] += p;
        s.binY[bin] += y;
        s.consecutiveMislead = (!hit && p >= 0.45d) ? s.consecutiveMislead + 1 : 0;
        s.push(p, y, r);
    }

    public synchronized Snapshot snapshot(String expertId) {
        String id = expertId == null ? "" : expertId;
        Stat s = stats.get(id);
        if (s == null) s = new Stat();
        return new Snapshot(id, s);
    }

    public synchronized double reliability(String expertId) {
        return snapshot(expertId).reliability;
    }

    public synchronized double recentReliability(String expertId) {
        return snapshot(expertId).recentReliability;
    }

    public synchronized double calibrationError(String expertId) {
        return snapshot(expertId).calibrationError;
    }

    public synchronized double rankRegret(String expertId) {
        return snapshot(expertId).rankRegret;
    }

    public synchronized int sample(String expertId) {
        return snapshot(expertId).sample;
    }

    public synchronized int expertCount() {
        return stats.size();
    }

    private static double score(double log, double brier, double ece, double rank) {
        double a = Math.exp(-Math.max(0.0d, log)),
                b = 1.0d - clamp01(brier),
                c = 1.0d - clamp01(ece),
                d = 1.0d - clamp01(rank);
        return clamp01(0.35d * a + 0.25d * b + 0.20d * c + 0.20d * d);
    }

    private static double clamp01(double x) {
        return clamp(x, 0.0d, 1.0d);
    }

    private static double clamp(double x, double lo, double hi) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return lo;
        return x < lo ? lo : (x > hi ? hi : x);
    }
}
