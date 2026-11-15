package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Assignment/location research over legal 1..10 bijections; never creates multi-position REAL. */
public final class CrossPositionAssignmentResearchEngine {
    private static final double EPS = 1e-12d;
    private CrossPositionAssignmentResearchEngine() {}

    public static double locationEntropy(double[][] q, int number) {
        requireMatrix(q); int n = number - 1; if (n < 0 || n >= 10) throw new IllegalArgumentException("R811_NUMBER");
        double out = 0.0d;
        for (int p = 0; p < 10; p++) { double v = Math.max(0.0d, q[p][n]); if (v > 0.0d) out -= v * Math.log(v); }
        return out / Math.log(10.0d);
    }

    public static double exclusivity(double[][] q, int position, int number) {
        requireMatrix(q); int p = position - 1, n = number - 1; requireIndex(p, n);
        double others = 0.0d; for (int i = 0; i < 10; i++) if (i != p) others += q[i][n];
        return q[p][n] - others / 9.0d;
    }

    public static double positionMargin(double[][] q, int position, int number) {
        requireMatrix(q); int p = position - 1, n = number - 1; requireIndex(p, n);
        return q[p][n] - collisionPressure(q, position, number);
    }

    public static double bestPositionMargin(double[][] q, int number) {
        requireMatrix(q); int n = number - 1; if (n < 0 || n >= 10) throw new IllegalArgumentException("R811_NUMBER");
        double first = -Double.MAX_VALUE, second = -Double.MAX_VALUE;
        for (int p = 0; p < 10; p++) { double v = q[p][n]; if (v > first) { second = first; first = v; } else if (v > second) second = v; }
        return first - second;
    }

    public static double collision(double[][] q, int position, int number) { return collisionPressure(q, position, number); }
    public static double collisionPressure(double[][] q, int position, int number) {
        requireMatrix(q); int p = position - 1, n = number - 1; requireIndex(p, n);
        double out = -Double.MAX_VALUE;
        for (int i = 0; i < 10; i++) if (i != p) out = Math.max(out, q[i][n]);
        return out;
    }

    /** Correlation-penalized expert top-k intersection for one location. */
    public static Vote locationVote(double[][][] expertQ, double[][] correlations, int position, int number, int topK) {
        double[] weights = new double[expertQ == null ? 0 : expertQ.length];
        Arrays.fill(weights, 1.0d);
        return locationVote(expertQ, weights, correlations, 1.0d, position, number, topK);
    }

    /** Exact diversity-adjusted vote: sum(w*I_topK)/(1+lambda*sum positive correlation). */
    public static Vote locationVote(
            double[][][] expertQ,
            double[] weights,
            double[][] correlations,
            double lambda,
            int position,
            int number,
            int topK) {
        if (expertQ == null || expertQ.length == 0) return new Vote(0.0d, 0, "NO_EXPERT");
        if (weights == null || weights.length != expertQ.length || lambda < 0.0d)
            throw new IllegalArgumentException("R811_LOCATION_VOTE_INPUT");
        int p = position - 1, n = number - 1; requireIndex(p, n); int k = Math.max(1, Math.min(10, topK));
        double weighted = 0.0d, positiveCorrelation = 0.0d; int support = 0;
        for (int e = 0; e < expertQ.length; e++) {
            requireMatrix(expertQ[e]);
            if (Double.isNaN(weights[e]) || Double.isInfinite(weights[e]) || weights[e] < 0.0d)
                throw new IllegalArgumentException("R811_LOCATION_VOTE_WEIGHT");
            if (!inTopK(expertQ[e][p], n, k)) continue;
            weighted += weights[e]; support++;
        }
        if (correlations != null) {
            for (int i = 0; i < expertQ.length; i++) {
                if (i >= correlations.length || correlations[i] == null)
                    throw new IllegalArgumentException("R811_LOCATION_VOTE_CORRELATION");
                for (int j = 0; j < i; j++) {
                    if (j >= correlations[i].length)
                        throw new IllegalArgumentException("R811_LOCATION_VOTE_CORRELATION");
                    double value = correlations[i][j];
                    if (Double.isNaN(value) || Double.isInfinite(value))
                        throw new IllegalArgumentException("R811_LOCATION_VOTE_CORRELATION");
                    positiveCorrelation += Math.max(0.0d, value);
                }
            }
        }
        double score = weighted / (1.0d + lambda * positiveCorrelation);
        return new Vote(
                score,
                support,
                V51ChecksumEngine.sha256(
                        position
                                + "|"
                                + number
                                + "|"
                                + k
                                + "|"
                                + support
                                + "|"
                                + weighted
                                + "|"
                                + positiveCorrelation
                                + "|"
                                + lambda
                                + "|"
                                + score));
    }

    /** Dynamic-programming k-best legal bijections and their normalized (p,n) support. */
    public static AssignmentSupport kBestAssignmentSupport(double[][] q, int requestedK) {
        return kBestAssignmentSupport(q, requestedK, 1.0d);
    }

    public static AssignmentSupport kBestAssignmentSupport(
            double[][] q, int requestedK, double temperature) {
        requireMatrix(q); int k = Math.max(1, Math.min(64, requestedK));
        if (!(temperature > 0.0d) || Double.isInfinite(temperature))
            throw new IllegalArgumentException("R811_ASSIGNMENT_TEMPERATURE");
        @SuppressWarnings("unchecked") ArrayList<Path>[] dp = new ArrayList[1 << 10];
        dp[0] = new ArrayList<Path>(); dp[0].add(new Path(0.0d, new int[0]));
        for (int mask = 0; mask < (1 << 10); mask++) {
            if (dp[mask] == null) continue; int position = Integer.bitCount(mask); if (position >= 10) continue;
            for (int number = 0; number < 10; number++) if ((mask & (1 << number)) == 0) {
                int nextMask = mask | (1 << number); if (dp[nextMask] == null) dp[nextMask] = new ArrayList<Path>();
                for (int i = 0; i < dp[mask].size(); i++) {
                    Path old = dp[mask].get(i); int[] perm = Arrays.copyOf(old.permutation, position + 1); perm[position] = number + 1;
                    dp[nextMask].add(new Path(old.cost - Math.log(Math.max(EPS, q[position][number])), perm));
                }
                trim(dp[nextMask], k);
            }
        }
        ArrayList<Path> paths = dp[(1 << 10) - 1]; trim(paths, k); double[][] support = new double[10][10];
        double best = paths.get(0).cost, sum = 0.0d;
        for (int i = 0; i < paths.size(); i++) { double w = Math.exp(Math.max(-60.0d, (best - paths.get(i).cost) / temperature)); sum += w; for (int p = 0; p < 10; p++) support[p][paths.get(i).permutation[p] - 1] += w; }
        for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) support[p][n] /= Math.max(EPS, sum);
        return new AssignmentSupport(Collections.unmodifiableList(new ArrayList<Path>(paths)), support, V51ChecksumEngine.hashDoubles(support));
    }

    public static int numberPosition(int[] permutation, int number) {
        requirePermutation(permutation);
        if (number < 1 || number > 10) throw new IllegalArgumentException("R811_NUMBER");
        for (int p = 0; p < 10; p++) if (permutation[p] == number) return p + 1;
        throw new IllegalStateException("R811_PERMUTATION_NUMBER_MISSING");
    }

    public static int migrationVelocity(int[] current, int[] previous, int number) {
        return numberPosition(current, number) - numberPosition(previous, number);
    }

    public static int migrationAcceleration(
            int[] current, int[] previous, int[] previousPrevious, int number) {
        return migrationVelocity(current, previous, number)
                - migrationVelocity(previous, previousPrevious, number);
    }

    /** rho = current o inverse(previous), encoded as a canonical 1..10 permutation. */
    public static int[] relativePermutation(int[] current, int[] previous) {
        requirePermutation(current); requirePermutation(previous);
        int[] relative = new int[10];
        for (int number = 1; number <= 10; number++)
            relative[number - 1] = current[numberPosition(previous, number) - 1];
        requirePermutation(relative);
        return relative;
    }

    public static double kendallTauDistance(int[] left, int[] right) {
        requirePermutation(left); requirePermutation(right);
        int discordant = 0;
        for (int a = 1; a <= 10; a++)
            for (int b = a + 1; b <= 10; b++) {
                int leftOrder = numberPosition(left, a) - numberPosition(left, b);
                int rightOrder = numberPosition(right, a) - numberPosition(right, b);
                if ((long) leftOrder * rightOrder < 0L) discordant++;
            }
        return discordant / 45.0d;
    }

    public static CycleFeatures cycleFeatures(int[] relative) {
        requirePermutation(relative);
        boolean[] seen = new boolean[10];
        int[] histogram = new int[11];
        int fixed = 0, cycles = 0, longest = 0;
        for (int start = 0; start < 10; start++) {
            if (seen[start]) continue;
            cycles++;
            int length = 0, at = start;
            while (!seen[at]) {
                seen[at] = true;
                length++;
                at = relative[at] - 1;
            }
            histogram[length]++;
            if (length == 1) fixed++;
            longest = Math.max(longest, length);
        }
        return new CycleFeatures(fixed / 10.0d, cycles, longest, histogram);
    }

    public static boolean thirdBoundaryMiss(
            int actualRank, boolean baselineHit, boolean counterfactualSwapHit) {
        return actualRank == 4 && !baselineHit && counterfactualSwapHit;
    }

    public static ThirdBoundaryAudit auditThirdBoundary(
            int[] selectedK3, int actualNumber, int actualRank) {
        if (selectedK3 == null
                || selectedK3.length != 3
                || !DecisionGateEngine.b12K3(selectedK3[0], selectedK3[1], selectedK3[2])
                || !AppConfig.isValidNumber(actualNumber))
            throw new IllegalArgumentException("R811_THIRD_BOUNDARY_INPUT");
        boolean baseline =
                actualNumber == selectedK3[0]
                        || actualNumber == selectedK3[1]
                        || actualNumber == selectedK3[2];
        int[] swapped = new int[] {selectedK3[0], selectedK3[1], actualNumber};
        boolean legalSwap =
                actualRank == 4
                        && !baseline
                        && DecisionGateEngine.b12K3(swapped[0], swapped[1], swapped[2]);
        return new ThirdBoundaryAudit(
                baseline,
                legalSwap,
                thirdBoundaryMiss(actualRank, baseline, legalSwap),
                swapped);
    }

    public static double boundaryRisk(double[] beta, double[] features) {
        if (beta == null || features == null || beta.length != 8 || features.length != 7)
            throw new IllegalArgumentException("R811_BOUNDARY_RISK_SHAPE");
        double logit = beta[0];
        for (int i = 0; i < features.length; i++) {
            double value = features[i];
            if (Double.isNaN(value) || Double.isInfinite(value))
                throw new IllegalArgumentException("R811_BOUNDARY_RISK_NONFINITE");
            logit += beta[i + 1] * value;
        }
        if (logit >= 0.0d) return 1.0d / (1.0d + Math.exp(-Math.min(60.0d, logit)));
        double exponential = Math.exp(Math.max(-60.0d, logit));
        return exponential / (1.0d + exponential);
    }

    private static void trim(ArrayList<Path> paths, int k) { Collections.sort(paths, Comparator.comparingDouble(a -> a.cost)); while (paths.size() > k) paths.remove(paths.size() - 1); }
    private static boolean inTopK(double[] scores, int numberIndex, int k) {
        int rank = 1;
        for (int i = 0; i < scores.length; i++)
            if (scores[i] > scores[numberIndex]
                    || (scores[i] == scores[numberIndex] && i < numberIndex)) rank++;
        return rank <= k;
    }
    private static void requireMatrix(double[][] q) { if (q == null || q.length != 10) throw new IllegalArgumentException("R811_Q_ROWS"); for (int i = 0; i < 10; i++) if (q[i] == null || q[i].length != 10) throw new IllegalArgumentException("R811_Q_COLS"); }
    private static void requirePermutation(int[] permutation) { if (permutation == null || permutation.length != 10) throw new IllegalArgumentException("R811_PERMUTATION_LENGTH"); boolean[] seen = new boolean[11]; for (int value : permutation) { if (value < 1 || value > 10 || seen[value]) throw new IllegalArgumentException("R811_PERMUTATION_INVALID"); seen[value] = true; } }
    private static void requireIndex(int p, int n) { if (p < 0 || p >= 10 || n < 0 || n >= 10) throw new IllegalArgumentException("R811_LOCATION"); }
    public static final class Vote { public final double score; public final int supportExperts; public final String hash; Vote(double s, int n, String h) { score = s; supportExperts = n; hash = h; } }
    public static final class Path { public final double cost; public final int[] permutation; Path(double c, int[] p) { cost = c; permutation = p.clone(); } public boolean legal() { boolean[] seen = new boolean[11]; if (permutation.length != 10) return false; for (int n : permutation) { if (n < 1 || n > 10 || seen[n]) return false; seen[n] = true; } return true; } }
    public static final class AssignmentSupport { public final List<Path> assignments; public final double[][] support; public final String hash; AssignmentSupport(List<Path> a, double[][] s, String h) { assignments = a; support = s; hash = h; } }
    public static final class CycleFeatures { public final double fixedPointRate; public final int cycleCount,longestCycle; public final int[] cycleLengthHistogram; CycleFeatures(double fixed,int count,int longest,int[] histogram){fixedPointRate=fixed;cycleCount=count;longestCycle=longest;cycleLengthHistogram=histogram.clone();} }
    public static final class ThirdBoundaryAudit { public final boolean baselineHit,counterfactualSwapHit,thirdBoundaryMiss; public final int[] swappedK3; ThirdBoundaryAudit(boolean baseline,boolean swap,boolean miss,int[] selected){baselineHit=baseline;counterfactualSwapHit=swap;thirdBoundaryMiss=miss;swappedK3=selected.clone();} }
}
