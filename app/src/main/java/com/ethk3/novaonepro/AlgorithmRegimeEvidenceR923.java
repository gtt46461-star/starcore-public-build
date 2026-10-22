package com.ethk3.novaonepro;

import java.util.Arrays;
import java.util.Locale;

/**
 * Label-blind regime stratification for invented-algorithm Future-OOS evidence.
 *
 * <p>R912 treated the continuous pre-draw regime score as if it were uniformly distributed on
 * {@code [0,1]} and cut it at {@code 1/3,2/3}.  In production that score normally occupies a much
 * narrower range, so otherwise strong candidates frequently had only one represented bucket.
 * The missing subgroup was then persisted as a zero hit rate and irreversibly pruned as a regime
 * collapse.  This class separates two different facts: an observed subgroup failure and missing
 * subgroup support.</p>
 *
 * <p>Boundaries are empirical tertiles of <em>pre-draw feature values only</em>.  Outcome labels
 * are never consulted while choosing boundaries.  Ties remain ties; if at least two adequately
 * supported strata cannot be formed the result is explicitly unsupported and must not be used to
 * prune or certify a candidate.</p>
 */
public final class AlgorithmRegimeEvidenceR923 {
    public static final String VERSION = "R923_LABEL_BLIND_REGIME_TERTILES_V1";
    public static final int MIN_GROUPS = 2;
    public static final int MIN_GROUP_N = 12;

    private AlgorithmRegimeEvidenceR923() {}

    /** Reproduces the R912 missing-subgroup defect, using features only (never labels). */
    public static boolean legacyMissingRegime(double[] scores) {
        if (scores == null || scores.length == 0) return false;
        boolean[] seen = new boolean[3];
        for (double score : scores) {
            if (!Double.isFinite(score)) return false;
            seen[score < 1.0d / 3.0d ? 0 : score < 2.0d / 3.0d ? 1 : 2] = true;
        }
        int groups = 0;
        for (boolean represented : seen) if (represented) groups++;
        return groups < MIN_GROUPS;
    }

    public static Result evaluate(double[] requestedScores, int[] requestedHits) {
        if (requestedScores == null || requestedHits == null
                || requestedScores.length != requestedHits.length
                || requestedScores.length == 0) {
            return Result.unsupported("INPUT_EMPTY_OR_MISMATCH", 0);
        }

        int finite = 0;
        for (int i = 0; i < requestedScores.length; i++) {
            if (Double.isFinite(requestedScores[i])
                    && (requestedHits[i] == 0 || requestedHits[i] == 1)) finite++;
        }
        if (finite < MIN_GROUPS * MIN_GROUP_N)
            return Result.unsupported("FINITE_SUPPORT_LT_" + (MIN_GROUPS * MIN_GROUP_N), finite);

        double[] sorted = new double[finite];
        int cursor = 0;
        for (int i = 0; i < requestedScores.length; i++) {
            if (Double.isFinite(requestedScores[i])
                    && (requestedHits[i] == 0 || requestedHits[i] == 1))
                sorted[cursor++] = requestedScores[i];
        }
        Arrays.sort(sorted);
        double q1 = sorted[Math.min(sorted.length - 1, sorted.length / 3)];
        double q2 = sorted[Math.min(sorted.length - 1, (sorted.length * 2) / 3)];
        if (!Double.isFinite(q1) || !Double.isFinite(q2) || q2 - q1 <= 1.0e-12d)
            return Result.unsupported("REGIME_FEATURE_HAS_NO_TERTILE_SPREAD", finite);

        int[] n = new int[3];
        int[] hit = new int[3];
        for (int i = 0; i < requestedScores.length; i++) {
            double score = requestedScores[i];
            int label = requestedHits[i];
            if (!Double.isFinite(score) || (label != 0 && label != 1)) continue;
            int bucket = score < q1 ? 0 : score < q2 ? 1 : 2;
            n[bucket]++;
            hit[bucket] += label;
        }

        int groups = 0;
        int minN = Integer.MAX_VALUE;
        double minRate = 1.0d;
        for (int i = 0; i < n.length; i++) {
            if (n[i] < MIN_GROUP_N) continue;
            groups++;
            minN = Math.min(minN, n[i]);
            minRate = Math.min(minRate, (double) hit[i] / n[i]);
        }
        boolean supported = groups >= MIN_GROUPS;
        if (!supported)
            return new Result(false, 0.0d, groups, minN == Integer.MAX_VALUE ? 0 : minN,
                    finite, q1, q2, n, hit, "REGIME_GROUP_SUPPORT_INSUFFICIENT");
        return new Result(true, clamp(minRate), groups, minN, finite, q1, q2, n, hit,
                "SUPPORTED_LABEL_BLIND_TERTILES");
    }

    private static double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d,
                Double.isFinite(value) ? value : 0.0d));
    }

    public static final class Result {
        public final boolean supported;
        public final double minimumHitRate, lowerBoundary, upperBoundary;
        public final int representedGroups, minimumGroupN, finiteN;
        public final int[] groupN, groupHit;
        public final String reason;

        Result(boolean supported, double minimumHitRate, int representedGroups,
                int minimumGroupN, int finiteN, double lowerBoundary, double upperBoundary,
                int[] groupN, int[] groupHit, String reason) {
            this.supported = supported;
            this.minimumHitRate = clamp(minimumHitRate);
            this.representedGroups = Math.max(0, representedGroups);
            this.minimumGroupN = Math.max(0, minimumGroupN);
            this.finiteN = Math.max(0, finiteN);
            this.lowerBoundary = lowerBoundary;
            this.upperBoundary = upperBoundary;
            this.groupN = groupN == null ? new int[3] : groupN.clone();
            this.groupHit = groupHit == null ? new int[3] : groupHit.clone();
            this.reason = reason == null ? "" : reason;
        }

        static Result unsupported(String reason, int finiteN) {
            return new Result(false, 0.0d, 0, 0, finiteN, Double.NaN, Double.NaN,
                    new int[3], new int[3], reason);
        }

        public String canonical() {
            return VERSION + "|supported=" + (supported ? 1 : 0)
                    + "|finiteN=" + finiteN + "|groups=" + representedGroups
                    + "|minGroupN=" + minimumGroupN
                    + "|minRate=" + String.format(Locale.US, "%.6f", minimumHitRate)
                    + "|q=" + value(lowerBoundary) + ',' + value(upperBoundary)
                    + "|n=" + Arrays.toString(groupN) + "|hit=" + Arrays.toString(groupHit)
                    + "|reason=" + reason;
        }

        private static String value(double x) {
            return Double.isFinite(x) ? String.format(Locale.US, "%.6f", x) : "N/A";
        }
    }
}
