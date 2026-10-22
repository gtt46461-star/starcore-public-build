package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Dataset-owned sparse sliding estimator. A cancelled advance never publishes partial counts. */
public final class SparseConditionalCountsSU1 {
    public static final int MAX_CELLS = 4096;
    private final int[][] draws;
    private final RelationSpecSU1 spec;
    private final String sourceRoot;
    private Map<Long, int[]> counts = new TreeMap<>();
    private int target = -1;
    private long fullRebuilds, incrementalUpdates;

    public SparseConditionalCountsSU1(int[][] draws, RelationSpecSU1 spec, String sourceRoot) {
        if (draws == null || draws.length > 20000 || spec == null)
            throw new IllegalArgumentException("SU1_RELATION_DATASET");
        this.draws = draws; this.spec = spec; this.sourceRoot = HashingSU1.sha(sourceRoot);
    }

    private void apply(Map<Long, int[]> map, int u, int sign, BudgetTokenSU1 budget) {
        budget.checkpointOrYield();
        for (int p = 0; p < 10; p++) {
            long key = spec.key(draws, u, p);
            int[] row = map.get(key);
            if (row == null) {
                if (sign < 0) throw new IllegalStateException("SU1_RELATION_REMOVE_MISSING");
                if ((map.size() + 1L) * 10 > MAX_CELLS)
                    throw new IllegalArgumentException("SU1_RELATION_CELL_BUDGET");
                row = new int[10]; map.put(key, row);
            }
            int n = draws[u][p] - 1;
            row[n] += sign;
            if (row[n] < 0) throw new IllegalStateException("SU1_NEGATIVE_RELATION_SUPPORT");
            boolean empty = true; for (int v : row) empty &= v == 0;
            if (empty) map.remove(key);
        }
    }

    public Estimate at(int t, BudgetTokenSU1 budget) {
        if (t < 0 || t > draws.length) throw new IllegalArgumentException("SU1_RELATION_TARGET");
        int lo = Math.max(spec.requiredLag, t - spec.window);
        if (t != target) {
            Map<Long, int[]> next = new TreeMap<>();
            boolean incremental = t == target + 1 && target >= 0;
            if (incremental) {
                for (Map.Entry<Long, int[]> e : counts.entrySet()) next.put(e.getKey(), e.getValue().clone());
                int oldLo = Math.max(spec.requiredLag, target - spec.window);
                for (int u = oldLo; u < Math.min(lo, target); u++) apply(next, u, -1, budget);
                if (target >= spec.requiredLag) apply(next, target, 1, budget);
            } else {
                for (int u = lo; u < t; u++) apply(next, u, 1, budget);
            }
            budget.checkpointOrYield();
            counts = next; target = t;
            if (incremental) incrementalUpdates++; else fullRebuilds++;
        }
        int[][] selected = new int[10][10];
        if (t >= spec.requiredLag) for (int p = 0; p < 10; p++) {
            int[] row = counts.get(spec.key(draws, t, p));
            if (row != null) selected[p] = row.clone();
        }
        return new Estimate(spec, sourceRoot, t, Math.min(lo, t), selected, counts.size() * 10);
    }

    public long fullRebuilds() { return fullRebuilds; }
    public long incrementalUpdates() { return incrementalUpdates; }

    public static final class Estimate {
        public final RelationSpecSU1 spec;
        public final String sourceRoot, hash;
        public final int targetExclusive, firstSample, nEligible, cells;
        private final int[][] rawCounts;
        Estimate(RelationSpecSU1 spec, String root, int t, int first, int[][] counts, int cells) {
            this.spec = spec; sourceRoot = root; targetExclusive = t; firstSample = first;
            nEligible = Math.max(0, t - first); this.cells = cells; rawCounts = counts;
            hash = HashingSU1.text(payload());
        }
        public int support(int p) { int sum = 0; for (int v : rawCounts[p]) sum += v; return sum; }
        public int count(int p, int n) { return rawCounts[p][n]; }
        public double[][] probabilities() {
            double[][] p = new double[10][10];
            for (int q = 0; q < 10; q++) for (int n = 0; n < 10; n++)
                p[q][n] = ProgramTreeSU1.f32((rawCounts[q][n] + 1.0) / (support(q) + 10.0));
            return p;
        }
        /** Raw estimates use NA at zero support. The prior never adds observations. */
        public String payload() {
            StringBuilder b = new StringBuilder("SU1_RELATION_AUDIT_V1\n").append(spec.hash).append('\n')
                    .append(spec.canonical()).append('\n').append(sourceRoot).append('\n')
                    .append(firstSample).append('|').append(targetExclusive).append('|').append(nEligible)
                    .append("|observed=").append(nEligible).append("|missing=0|priorMass=10|uncertainty=UNAVAILABLE_DEPENDENCE|nEffective=UNKNOWN\n");
            for (int p = 0; p < 10; p++) {
                int support = support(p);
                b.append("position=").append(p + 1).append("|conditionSupport=").append(support)
                        .append("|matchedIssues=").append(support).append("|status=")
                        .append(support == 0 ? "BACKOFF_PRIOR" : "OBSERVED_ESTIMATE");
                for (int n = 0; n < 10; n++) b.append('|').append(rawCounts[p][n]).append(':')
                        .append(support == 0 ? "NA" : Double.toHexString(rawCounts[p][n] / (double) support))
                        .append(':').append(Double.toHexString((rawCounts[p][n] + 1.0) / (support + 10.0)));
                b.append('\n');
            }
            return b.toString();
        }
    }
}
