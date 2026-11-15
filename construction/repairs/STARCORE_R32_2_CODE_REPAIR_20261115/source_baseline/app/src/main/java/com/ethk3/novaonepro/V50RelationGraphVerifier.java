package com.ethk3.novaonepro;

import java.util.List;

/**
 * Verifies lagged cross-position edges before they can enter the V50 feature surface. Each live
 * edge needs support, matching direction in two time blocks and a deterministic circular-shift
 * permutation check.
 */
public final class V50RelationGraphVerifier {
    public static final String MODULE_VERSION = "V50_RELATION_GRAPH_VERIFIER_50072401";
    private static final int MAX_HISTORY = 360;
    private static final int MIN_SUPPORT = 12;
    private static final int[] PERMUTATION_SHIFTS = new int[] {7, 13, 29, 47, 83, 127, 173, 211};

    public static final class Result {
        public final double[][] lift = new double[10][10];
        public final double[][] pValue = new double[10][10];
        public final double[][] stability = new double[10][10];
        public final int[][] support = new int[10][10];
        public final int[][] sourcePosition = new int[10][10];
        public final int[][] sourceNumber = new int[10][10];
        public final String checksum;

        Result(String checksum) {
            this.checksum = checksum == null ? "" : checksum;
        }
    }

    public Result evaluate(List<Models.Draw> history) {
        int size = history == null ? 0 : history.size();
        if (size < 36) return new Result(V50ChecksumEngine.sha256("V50_RELATION_EMPTY|" + size));
        int start = Math.max(1, size - MAX_HISTORY);
        Models.Draw latest = history.get(size - 1);
        Result out = new Result("");
        StringBuilder hash = new StringBuilder(MODULE_VERSION);
        for (int targetPosition = 1; targetPosition <= 10; targetPosition++) {
            for (int number = 1; number <= 10; number++) {
                Edge best = null;
                for (int sourcePosition = 1; sourcePosition <= 10; sourcePosition++) {
                    if (sourcePosition == targetPosition) continue;
                    int sourceNumber = latest.getNumberAtPosition(sourcePosition);
                    Edge edge =
                            edge(
                                    history,
                                    start,
                                    targetPosition,
                                    number,
                                    sourcePosition,
                                    sourceNumber);
                    if (edge == null) continue;
                    if (best == null
                            || edge.quality > best.quality
                            || (edge.quality == best.quality
                                    && sourcePosition < best.sourcePosition)) {
                        best = edge;
                    }
                }
                int p = targetPosition - 1;
                int n = number - 1;
                if (best != null) {
                    out.lift[p][n] = best.accepted ? best.lift : 0.0d;
                    out.pValue[p][n] = best.pValue;
                    out.stability[p][n] = best.stability;
                    out.support[p][n] = best.support;
                    out.sourcePosition[p][n] = best.sourcePosition;
                    out.sourceNumber[p][n] = best.sourceNumber;
                } else {
                    out.pValue[p][n] = 1.0d;
                }
                hash.append('|')
                        .append(targetPosition)
                        .append(':')
                        .append(number)
                        .append('=')
                        .append(Double.doubleToLongBits(out.lift[p][n]))
                        .append(',')
                        .append(Double.doubleToLongBits(out.pValue[p][n]))
                        .append(',')
                        .append(out.support[p][n]);
            }
        }
        return copyWithChecksum(out, V50ChecksumEngine.sha256(hash.toString()));
    }

    private static Edge edge(
            List<Models.Draw> h,
            int start,
            int targetPosition,
            int number,
            int sourcePosition,
            int sourceNumber) {
        int size = h.size();
        int total = 0;
        int baseHit = 0;
        int support = 0;
        int conditionalHit = 0;
        int firstSupport = 0;
        int firstHit = 0;
        int secondSupport = 0;
        int secondHit = 0;
        int middle = start + (size - start) / 2;
        for (int i = start; i < size; i++) {
            int actual = h.get(i).getNumberAtPosition(targetPosition);
            if (actual == number) baseHit++;
            total++;
            int prior = h.get(i - 1).getNumberAtPosition(sourcePosition);
            if (prior != sourceNumber) continue;
            support++;
            if (actual == number) conditionalHit++;
            if (i < middle) {
                firstSupport++;
                if (actual == number) firstHit++;
            } else {
                secondSupport++;
                if (actual == number) secondHit++;
            }
        }
        if (support < MIN_SUPPORT || firstSupport < 4 || secondSupport < 4 || total <= 0)
            return null;
        double base = (baseHit + 0.5d) / (total + 5.0d);
        double conditional = (conditionalHit + 0.5d) / (support + 5.0d);
        double lift = conditional / Math.max(0.02d, base) - 1.0d;
        double first = (firstHit + 0.5d) / (firstSupport + 5.0d) / Math.max(0.02d, base) - 1.0d;
        double second = (secondHit + 0.5d) / (secondSupport + 5.0d) / Math.max(0.02d, base) - 1.0d;
        boolean sameDirection = Math.signum(first) == Math.signum(second);
        double stability =
                sameDirection
                        ? Math.max(0.0d, 1.0d - Math.abs(first - second) / (Math.abs(lift) + 0.25d))
                        : 0.0d;
        int permutationWins = 0;
        for (int k = 0; k < PERMUTATION_SHIFTS.length; k++) {
            int shift = PERMUTATION_SHIFTS[k] % Math.max(2, total - 1);
            int permSupport = 0;
            int permHit = 0;
            for (int i = start; i < size; i++) {
                int shifted = start + ((i - start + shift) % total);
                int priorIndex = Math.max(0, shifted - 1);
                if (h.get(priorIndex).getNumberAtPosition(sourcePosition) != sourceNumber) continue;
                permSupport++;
                if (h.get(i).getNumberAtPosition(targetPosition) == number) permHit++;
            }
            double perm = (permHit + 0.5d) / (permSupport + 5.0d) / Math.max(0.02d, base) - 1.0d;
            if (Math.abs(perm) >= Math.abs(lift)) permutationWins++;
        }
        double pValue = (1.0d + permutationWins) / (1.0d + PERMUTATION_SHIFTS.length);
        double shrunk = lift * support / (support + 30.0d);
        boolean accepted =
                sameDirection && stability >= 0.28d && pValue <= 0.34d && Math.abs(shrunk) >= 0.04d;
        double quality =
                (accepted ? 2.0d : 0.0d)
                        + Math.abs(shrunk) * stability * (1.0d - pValue)
                        + Math.min(1.0d, support / 80.0d) * 0.05d;
        return new Edge(
                sourcePosition,
                sourceNumber,
                support,
                shrunk,
                stability,
                pValue,
                accepted,
                quality);
    }

    private static Result copyWithChecksum(Result source, String checksum) {
        Result out = new Result(checksum);
        for (int p = 0; p < 10; p++) {
            for (int n = 0; n < 10; n++) {
                out.lift[p][n] = source.lift[p][n];
                out.pValue[p][n] = source.pValue[p][n];
                out.stability[p][n] = source.stability[p][n];
                out.support[p][n] = source.support[p][n];
                out.sourcePosition[p][n] = source.sourcePosition[p][n];
                out.sourceNumber[p][n] = source.sourceNumber[p][n];
            }
        }
        return out;
    }

    private static final class Edge {
        final int sourcePosition;
        final int sourceNumber;
        final int support;
        final double lift;
        final double stability;
        final double pValue;
        final boolean accepted;
        final double quality;

        Edge(
                int sourcePosition,
                int sourceNumber,
                int support,
                double lift,
                double stability,
                double pValue,
                boolean accepted,
                double quality) {
            this.sourcePosition = sourcePosition;
            this.sourceNumber = sourceNumber;
            this.support = support;
            this.lift = lift;
            this.stability = stability;
            this.pValue = pValue;
            this.accepted = accepted;
            this.quality = quality;
        }
    }
}
