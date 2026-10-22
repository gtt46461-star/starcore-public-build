package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * V40 event-probability calibration.
 *
 * <p>Buckets are indexed by the pre-draw K3 event probability, never by a rank score. The mapping
 * is allowed to move probability upward or downward when settled OOS evidence supports that change.
 * Small buckets are partially pooled with adjacent probability buckets and a Beta prior centred on
 * the bucket's own probability range.
 */
public final class MonotonicCalibrationEngine {
    public static final String V46_ROLE = "GLOBAL_MONOTONIC_BASE_FOR_V46_CONDITIONAL_CALIBRATION";

    public static final String MODULE_VERSION = AppConfig.CALIBRATION_VERSION;
    public static final String STATUS_READY = "OOS_MONOTONIC_READY";
    public static final String STATUS_FULL = "OOS_MONOTONIC_FULL";
    public static final String STATUS_WARM = "OOS_MONOTONIC_WARM";
    public static final String STATUS_PROBE = "OOS_MONOTONIC_PROBE";
    public static final String STATUS_BOOTSTRAP = "OOS_MONOTONIC_BOOTSTRAP";
    public static final String STATUS_NOT_READY = "OOS_MONOTONIC_NOT_READY";

    public static final class Snapshot {
        public final double[] rate = new double[AppConfig.CALIBRATION_BUCKET_COUNT];
        public final double[] brier = new double[AppConfig.CALIBRATION_BUCKET_COUNT];
        public final int[] sample = new int[AppConfig.CALIBRATION_BUCKET_COUNT];
        public final int[] hit = new int[AppConfig.CALIBRATION_BUCKET_COUNT];
        public int totalSample;
        public int totalHit;
        public double globalRate = AppConfig.RANDOM_K3_BASELINE;
        public long loadedAt;
        public String version = MODULE_VERSION;
        public String status = STATUS_NOT_READY;
    }

    public static final class CalibrationResult {
        public double probability = AppConfig.RANDOM_K3_BASELINE;
        public double modelProbability = AppConfig.RANDOM_K3_BASELINE;
        public double bucketProbability = AppConfig.RANDOM_K3_BASELINE;
        public double posteriorProbability = AppConfig.RANDOM_K3_BASELINE;
        public double familyAdjustment;
        public int totalSample;
        public int bucketSample;
        public int effectiveBucketSample;
        public int bucket;
        public String status = STATUS_NOT_READY;
        public String version = MODULE_VERSION;
        public boolean ready;
        public boolean warm;
        public boolean probe;
        public double brier;
    }

    public Snapshot loadSnapshot(DbStore store) {
        Snapshot s = new Snapshot();
        s.loadedAt = System.currentTimeMillis();
        Cursor c = null;
        try {
            if (store != null) {
                SQLiteDatabase db = store.readable();
                c =
                        db.query(
                                DbHelper.TABLE_MONOTONIC_CALIBRATION,
                                new String[] {
                                    "bucket_no", "sample_count", "hit_count", "brier_sum"
                                },
                                "calibration_version=?",
                                new String[] {MODULE_VERSION},
                                null,
                                null,
                                "bucket_no ASC");
                while (c != null && c.moveToNext()) {
                    int b = c.getInt(0);
                    if (b < 0 || b >= s.sample.length) continue;
                    int n = Math.max(0, c.getInt(1));
                    int h = Math.max(0, Math.min(n, c.getInt(2)));
                    double bs = finite(c.getDouble(3), 0.0d);
                    s.sample[b] = n;
                    s.hit[b] = h;
                    s.brier[b] = n > 0 ? bs / n : 0.0d;
                    s.totalSample += n;
                    s.totalHit += h;
                }
            }
        } catch (Throwable ignore) {
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }

        double priorStrength = Math.max(1.0d, AppConfig.OOS_CALIBRATION_PRIOR_STRENGTH);
        s.globalRate =
                (s.totalHit + priorStrength * AppConfig.RANDOM_K3_BASELINE)
                        / (s.totalSample + priorStrength);
        double[] weight = new double[s.rate.length];
        for (int i = 0; i < s.rate.length; i++) {
            double priorMean = bucketCenterProbability(i);
            int n = s.sample[i];
            s.rate[i] = (s.hit[i] + priorStrength * priorMean) / (n + priorStrength);
            weight[i] = Math.max(0.50d, n);
        }
        isotonic(s.rate, weight);
        s.status = statusFor(s.totalSample);
        return s;
    }

    public CalibrationResult apply(
            DbStore store,
            Models.ComboCandidate c,
            HierarchicalPosteriorEngine.Posterior posterior) {
        OnlineActionModelEngine.Snapshot model = OnlineActionModelEngine.load(store);
        double modelP = OnlineActionModelEngine.predict(model, c);
        return apply(loadSnapshot(store), c, posterior, modelP);
    }

    public CalibrationResult apply(
            Snapshot snapshot,
            Models.ComboCandidate c,
            HierarchicalPosteriorEngine.Posterior posterior) {
        double modelP =
                c != null && validProbability(c.modelP)
                        ? c.modelP
                        : (c != null && validProbability(c.rawP)
                                ? c.rawP
                                : AppConfig.RANDOM_K3_BASELINE);
        return apply(snapshot, c, posterior, modelP);
    }

    public CalibrationResult apply(
            Snapshot snapshot,
            Models.ComboCandidate c,
            HierarchicalPosteriorEngine.Posterior posterior,
            double modelProbability) {
        CalibrationResult r =
                calibrate(snapshot, c == null ? 0.5d : c.rawRankScore, modelProbability);

        double family = posterior == null ? r.probability : posterior.mean;
        r.posteriorProbability =
                clamp(
                        family,
                        AppConfig.CALIBRATED_PROBABILITY_MIN,
                        AppConfig.CALIBRATED_PROBABILITY_MAX);
        int familySample = posterior == null ? 0 : Math.max(0, posterior.sample);
        double reliability =
                Math.min(
                        1.0d,
                        familySample / Math.max(1.0d, AppConfig.OOS_FAMILY_ADJUSTMENT_FULL_SAMPLE));
        if (posterior == null) reliability = 0.0d;
        else if (!(HierarchicalPosteriorEngine.POLICY_READY.equals(posterior.policy)
                || HierarchicalPosteriorEngine.POLICY_STRONG.equals(posterior.policy))) {
            reliability *= 0.18d;
        }
        double adjustment =
                (r.posteriorProbability - r.probability)
                        * AppConfig.V40_POSTERIOR_BLEND_MAX
                        * reliability;
        adjustment =
                clamp(
                        adjustment,
                        -AppConfig.OOS_FAMILY_ADJUSTMENT_MAX,
                        AppConfig.OOS_FAMILY_ADJUSTMENT_MAX);
        r.familyAdjustment = adjustment;
        r.probability =
                clamp(
                        r.probability + adjustment,
                        AppConfig.CALIBRATED_PROBABILITY_MIN,
                        AppConfig.CALIBRATED_PROBABILITY_MAX);

        if (c != null) {
            c.modelP = r.modelProbability;
            c.calibrationBucket = r.bucket;
            c.calibrationSample = r.effectiveBucketSample;
            c.calibrationTotalSample = r.totalSample;
            c.calibrationStatus = r.status;
            c.calibrationVersion = r.version;
            c.calibratedHitProbability = r.probability;
            c.calibratedP = r.probability;
            c.rawP = r.modelProbability;
            c.finalP = r.probability;
            c.finalPAfterFusion = r.probability;
            c.finalPAfterDiagnosis = r.probability;
            c.calibrationReady = r.ready ? 1 : 0;
            c.calibrationBrier = r.brier;
        }
        return r;
    }

    /** Compatibility signature: rawRankScore is no longer used for bucket selection. */
    public CalibrationResult calibrate(DbStore store, double rawRankScore, double fallback) {
        return calibrate(loadSnapshot(store), rawRankScore, fallback);
    }

    /** Compatibility only. New callers should pass the model probability explicitly. */
    public CalibrationResult calibrate(Snapshot snapshot, double rawRankScore) {
        return calibrate(snapshot, rawRankScore, bootstrapProbability(rawRankScore));
    }

    public CalibrationResult calibrate(
            Snapshot snapshot, double rawRankScore, double modelProbability) {
        Snapshot s = snapshot == null ? new Snapshot() : snapshot;
        CalibrationResult r = new CalibrationResult();
        r.modelProbability =
                clamp(
                        modelProbability,
                        AppConfig.CALIBRATED_PROBABILITY_MIN,
                        AppConfig.CALIBRATED_PROBABILITY_MAX);
        r.bucket = bucket(r.modelProbability);
        r.totalSample = s.totalSample;
        r.bucketSample = s.sample[r.bucket];
        r.effectiveBucketSample = effectiveSample(s.sample, r.bucket);
        r.bucketProbability =
                clamp(
                        finite(s.rate[r.bucket], r.modelProbability),
                        AppConfig.CALIBRATED_PROBABILITY_MIN,
                        AppConfig.CALIBRATED_PROBABILITY_MAX);

        double localWeight =
                r.effectiveBucketSample
                        / (r.effectiveBucketSample + AppConfig.OOS_CALIBRATION_PRIOR_STRENGTH);
        localWeight = clamp(localWeight, 0.0d, AppConfig.V40_CALIBRATION_MAX_LOCAL_WEIGHT);
        if (s.totalSample < AppConfig.CALIBRATION_PROBE_TOTAL_SAMPLE) localWeight *= 0.25d;
        else if (s.totalSample < AppConfig.CALIBRATION_WARM_TOTAL_SAMPLE) localWeight *= 0.55d;
        else if (s.totalSample < AppConfig.CALIBRATION_READY_TOTAL_SAMPLE) localWeight *= 0.78d;

        /* Cold-start discipline: the sequence ensemble is a ranking model until
         * enough genuinely settled OOS evidence exists.  Shrink its event
         * probability toward the K3 base rate instead of treating a selected
         * Top-3 sum as already calibrated. */
        double maturity =
                clamp(
                        s.totalSample
                                / (double) Math.max(1, AppConfig.CALIBRATION_READY_TOTAL_SAMPLE),
                        0.0d,
                        1.0d);
        double modelTrust =
                AppConfig.V40_COLD_MODEL_PRIOR_TRUST
                        + (1.0d - AppConfig.V40_COLD_MODEL_PRIOR_TRUST) * maturity;
        double priorProbability =
                AppConfig.RANDOM_K3_BASELINE
                        + modelTrust * (r.modelProbability - AppConfig.RANDOM_K3_BASELINE);
        r.probability =
                clamp(
                        priorProbability + localWeight * (r.bucketProbability - priorProbability),
                        AppConfig.CALIBRATED_PROBABILITY_MIN,
                        AppConfig.CALIBRATED_PROBABILITY_MAX);
        r.brier = weightedBrier(s.brier, s.sample, r.bucket);
        r.probe = s.totalSample >= AppConfig.CALIBRATION_PROBE_TOTAL_SAMPLE;
        r.warm = s.totalSample >= AppConfig.CALIBRATION_WARM_TOTAL_SAMPLE;
        r.ready =
                s.totalSample >= AppConfig.CALIBRATION_READY_TOTAL_SAMPLE
                        && r.effectiveBucketSample >= AppConfig.V40_CALIBRATION_MIN_LOCAL;
        r.status =
                r.ready
                        ? (s.totalSample >= AppConfig.CALIBRATION_FULL_TOTAL_SAMPLE
                                ? STATUS_FULL
                                : STATUS_READY)
                        : (r.warm
                                ? STATUS_WARM
                                : (r.probe
                                        ? STATUS_PROBE
                                        : (s.totalSample > 0
                                                ? STATUS_BOOTSTRAP
                                                : STATUS_NOT_READY)));
        return r;
    }

    private static String statusFor(int total) {
        if (total >= AppConfig.CALIBRATION_FULL_TOTAL_SAMPLE) return STATUS_FULL;
        if (total >= AppConfig.CALIBRATION_READY_TOTAL_SAMPLE) return STATUS_READY;
        if (total >= AppConfig.CALIBRATION_WARM_TOTAL_SAMPLE) return STATUS_WARM;
        if (total >= AppConfig.CALIBRATION_PROBE_TOTAL_SAMPLE) return STATUS_PROBE;
        if (total > 0) return STATUS_BOOTSTRAP;
        return STATUS_NOT_READY;
    }

    public static boolean isReadyStatus(String s) {
        return STATUS_READY.equals(s) || STATUS_FULL.equals(s);
    }

    public static boolean isWarmStatus(String s) {
        return isReadyStatus(s) || STATUS_WARM.equals(s);
    }

    public static boolean isProbeStatus(String s) {
        return isWarmStatus(s) || STATUS_PROBE.equals(s) || STATUS_BOOTSTRAP.equals(s);
    }

    private static int effectiveSample(int[] n, int b) {
        if (n == null || b < 0 || b >= n.length) return 0;
        double v = n[b];
        if (b > 0) v += 0.60d * n[b - 1];
        if (b + 1 < n.length) v += 0.60d * n[b + 1];
        if (b > 1) v += 0.25d * n[b - 2];
        if (b + 2 < n.length) v += 0.25d * n[b + 2];
        return (int) Math.floor(v + 0.000001d);
    }

    private static double weightedBrier(double[] b, int[] n, int at) {
        if (b == null || n == null || at < 0 || at >= b.length) return 0.0d;
        double sum = b[at] * n[at], w = n[at];
        if (at > 0) {
            sum += 0.5d * b[at - 1] * n[at - 1];
            w += 0.5d * n[at - 1];
        }
        if (at + 1 < b.length) {
            sum += 0.5d * b[at + 1] * n[at + 1];
            w += 0.5d * n[at + 1];
        }
        return w > 0.0d ? sum / w : 0.0d;
    }

    /** Bucket by event probability, not by rank score. */
    public static int bucket(double probability) {
        double lo = AppConfig.CALIBRATED_PROBABILITY_MIN;
        double hi = AppConfig.CALIBRATED_PROBABILITY_MAX;
        double normalized = (clamp(probability, lo, hi) - lo) / Math.max(0.000001d, hi - lo);
        int b = (int) Math.floor(normalized * AppConfig.CALIBRATION_BUCKET_COUNT);
        if (b < 0) b = 0;
        if (b >= AppConfig.CALIBRATION_BUCKET_COUNT) b = AppConfig.CALIBRATION_BUCKET_COUNT - 1;
        return b;
    }

    /** Legacy display bridge only; it is not an authorization probability. */
    public static double bootstrapProbability(double rawRankScore) {
        double p =
                AppConfig.RANDOM_K3_BASELINE + (clamp(rawRankScore, 0.0d, 1.0d) - 0.50d) * 0.040d;
        return clamp(p, 0.26d, 0.34d);
    }

    private static double bucketCenterProbability(int bucket) {
        double x = (bucket + 0.5d) / Math.max(1.0d, AppConfig.CALIBRATION_BUCKET_COUNT);
        return AppConfig.CALIBRATED_PROBABILITY_MIN
                + (AppConfig.CALIBRATED_PROBABILITY_MAX - AppConfig.CALIBRATED_PROBABILITY_MIN) * x;
    }

    static void isotonic(double[] y, double[] w) {
        int n = y == null ? 0 : y.length;
        if (n == 0) return;
        double[] value = new double[n], weight = new double[n];
        int[] start = new int[n], end = new int[n];
        int m = 0;
        for (int i = 0; i < n; i++) {
            value[m] = y[i];
            weight[m] = Math.max(0.0001d, w[i]);
            start[m] = i;
            end[m] = i;
            m++;
            while (m >= 2 && value[m - 2] > value[m - 1]) {
                double nw = weight[m - 2] + weight[m - 1];
                value[m - 2] = (value[m - 2] * weight[m - 2] + value[m - 1] * weight[m - 1]) / nw;
                weight[m - 2] = nw;
                end[m - 2] = end[m - 1];
                m--;
            }
        }
        for (int j = 0; j < m; j++) {
            for (int i = start[j]; i <= end[j]; i++) y[i] = value[j];
        }
    }

    private static boolean validProbability(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v > 0.0d && v < 1.0d;
    }

    private static double finite(double v, double d) {
        return Double.isNaN(v) || Double.isInfinite(v) ? d : v;
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
