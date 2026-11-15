package com.ethk3.novaonepro;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable per-target cross-position primitives. The assignment dynamic program runs once and
 * all 1,013 position subsets are precomputed before any candidate/action loop begins.
 */
public final class CrossPositionPrimitiveCacheR815 {
    private static final int POSITION_COUNT = 10;
    private static final int NUMBER_COUNT = 10;
    private static final int MAX_CACHE = 8;
    private static final Object CACHE_LOCK = new Object();
    private static final LinkedHashMap<String, Snapshot> CACHE =
            new LinkedHashMap<String, Snapshot>(MAX_CACHE + 1, 0.75f, true);

    public interface AssignmentEngine {
        int[] assign(double[][] normalizedQ);
    }

    public static final class Binding {
        public final String targetIssue;
        public final String historyHash;
        public final String schemaHash;
        public final int contiguousHistory;

        public Binding(String targetIssue, String historyHash, String schemaHash, int contiguousHistory) {
            this.targetIssue = safe(targetIssue);
            this.historyHash = safe(historyHash);
            this.schemaHash = safe(schemaHash);
            this.contiguousHistory = Math.max(0, contiguousHistory);
            if (!TargetIssueResolver.isValid(this.targetIssue)
                    || !sha256(this.historyHash)
                    || !ResearchFeatureSchemaR815.schemaSha256().equals(this.schemaHash))
                throw new IllegalArgumentException("R815_PRIMITIVE_BINDING_INVALID");
        }

        String canonical() {
            return targetIssue + '|' + historyHash + '|' + schemaHash + '|' + contiguousHistory;
        }
    }

    public static final class Atom {
        public final int position;
        public final int number;
        public final double q;
        public final boolean assigned;

        Atom(int position, int number, double q, boolean assigned) {
            this.position = position;
            this.number = number;
            this.q = q;
            this.assigned = assigned;
        }
    }

    public static final class Subset {
        public final int mask;
        public final int size;
        public final double assignmentMass;
        public final double qMean;
        public final double qMin;
        public final double qMax;

        Subset(int mask, int size, double assignmentMass, double qMean, double qMin, double qMax) {
            this.mask = mask;
            this.size = size;
            this.assignmentMass = assignmentMass;
            this.qMean = qMean;
            this.qMin = qMin;
            this.qMax = qMax;
        }
    }

    public static final class Snapshot {
        public final Binding binding;
        public final String qHash;
        private final Atom[] atoms;
        private final int[] assignment;
        private final double[][] location;
        private final Subset[] subsets;
        public final double assignmentScore;
        public final int dpCalls;
        public final int voteCalls;

        Snapshot(
                Binding binding,
                String qHash,
                Atom[] atoms,
                int[] assignment,
                double[][] location,
                Subset[] subsets,
                double assignmentScore,
                int dpCalls,
                int voteCalls) {
            this.binding = binding;
            this.qHash = qHash;
            this.atoms = atoms.clone();
            this.assignment = assignment.clone();
            this.location = copy(location);
            this.subsets = subsets.clone();
            this.assignmentScore = assignmentScore;
            this.dpCalls = dpCalls;
            this.voteCalls = voteCalls;
        }

        public Atom atom(int position, int number) {
            checkPosition(position);
            checkNumber(number);
            return atoms[(position - 1) * NUMBER_COUNT + (number - 1)];
        }

        public int assignedNumber(int position) {
            checkPosition(position);
            return assignment[position - 1];
        }

        public double location(int position, int number) {
            checkPosition(position);
            checkNumber(number);
            return location[position - 1][number - 1];
        }

        public Subset subset(int mask) {
            if (mask < 0 || mask >= subsets.length || subsets[mask] == null)
                throw new IllegalArgumentException("R815_SUBSET_MASK_INVALID");
            return subsets[mask];
        }

        public int subsetCount() {
            int count = 0;
            for (int mask = 0; mask < subsets.length; mask++) if (subsets[mask] != null) count++;
            return count;
        }

        public double[][] locationCopy() {
            return copy(location);
        }
    }

    private CrossPositionPrimitiveCacheR815() {}

    public static Snapshot build(Binding binding, double[][] q) {
        return build(binding, q, new HungarianDpAssignmentEngine());
    }

    public static Snapshot build(Binding binding, double[][] q, AssignmentEngine assignmentEngine) {
        if (binding == null || assignmentEngine == null) throw new IllegalArgumentException("R815_PRIMITIVE_ARGUMENT_NULL");
        double[][] normalized = normalize(q);
        String qHash = qHash(normalized);
        String key = binding.canonical() + '|' + qHash;
        synchronized (CACHE_LOCK) {
            Snapshot cached = CACHE.get(key);
            if (cached != null) return cached;
        }
        int[] assignment = assignmentEngine.assign(copy(normalized));
        validateAssignment(assignment);
        Atom[] atoms = new Atom[POSITION_COUNT * NUMBER_COUNT];
        double[][] location = new double[POSITION_COUNT][NUMBER_COUNT];
        int votes = 0;
        double score = 0.0d;
        for (int position = 0; position < POSITION_COUNT; position++) {
            for (int number = 0; number < NUMBER_COUNT; number++) {
                double value = normalized[position][number];
                location[position][number] = value;
                boolean assigned = assignment[position] == number + 1;
                if (assigned) score += value;
                atoms[position * NUMBER_COUNT + number] = new Atom(position + 1, number + 1, value, assigned);
                votes++;
            }
        }
        if (votes > 100) throw new IllegalStateException("R815_LOCATION_VOTE_LIMIT");
        Subset[] subsets = buildSubsets(normalized, assignment);
        Snapshot snapshot = new Snapshot(binding, qHash, atoms, assignment, location, subsets, score, 1, votes);
        if (snapshot.subsetCount() != 1013 || snapshot.dpCalls != 1 || snapshot.voteCalls > 100)
            throw new IllegalStateException("R815_PRIMITIVE_CONTRACT");
        synchronized (CACHE_LOCK) {
            CACHE.put(key, snapshot);
            while (CACHE.size() > MAX_CACHE) {
                String eldest = CACHE.keySet().iterator().next();
                CACHE.remove(eldest);
            }
        }
        return snapshot;
    }

    public static void clear() {
        synchronized (CACHE_LOCK) {
            CACHE.clear();
        }
    }

    private static Subset[] buildSubsets(double[][] q, int[] assignment) {
        Subset[] out = new Subset[1 << POSITION_COUNT];
        for (int mask = 0; mask < out.length; mask++) {
            int count = Integer.bitCount(mask);
            if (count < 2) continue;
            double sum = 0.0d;
            double min = 1.0d;
            double max = 0.0d;
            for (int position = 0; position < POSITION_COUNT; position++) {
                if ((mask & (1 << position)) == 0) continue;
                double value = q[position][assignment[position] - 1];
                sum += value;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            out[mask] = new Subset(mask, count, sum, sum / count, min, max);
        }
        return out;
    }

    private static final class HungarianDpAssignmentEngine implements AssignmentEngine {
        @Override
        public int[] assign(double[][] q) {
            int states = 1 << NUMBER_COUNT;
            double[] best = new double[states];
            int[] parentMask = new int[states];
            int[] parentNumber = new int[states];
            Arrays.fill(best, Double.NEGATIVE_INFINITY);
            Arrays.fill(parentMask, -1);
            best[0] = 0.0d;
            for (int mask = 0; mask < states; mask++) {
                int position = Integer.bitCount(mask);
                if (position >= POSITION_COUNT || Double.isInfinite(best[mask])) continue;
                for (int number = 0; number < NUMBER_COUNT; number++) {
                    if ((mask & (1 << number)) != 0) continue;
                    int next = mask | (1 << number);
                    double candidate = best[mask] + q[position][number];
                    if (candidate > best[next]
                            || (candidate == best[next] && (parentNumber[next] < 0 || number < parentNumber[next]))) {
                        best[next] = candidate;
                        parentMask[next] = mask;
                        parentNumber[next] = number;
                    }
                }
            }
            int[] assignment = new int[POSITION_COUNT];
            int mask = states - 1;
            for (int position = POSITION_COUNT - 1; position >= 0; position--) {
                if (parentMask[mask] < 0 || parentNumber[mask] < 0)
                    throw new IllegalStateException("R815_ASSIGNMENT_DP_FAILED");
                assignment[position] = parentNumber[mask] + 1;
                mask = parentMask[mask];
            }
            return assignment;
        }
    }

    private static double[][] normalize(double[][] input) {
        if (input == null || input.length != POSITION_COUNT)
            throw new IllegalArgumentException("R815_Q_POSITION_COUNT");
        double[][] out = new double[POSITION_COUNT][NUMBER_COUNT];
        for (int position = 0; position < POSITION_COUNT; position++) {
            if (input[position] == null || input[position].length != NUMBER_COUNT)
                throw new IllegalArgumentException("R815_Q_NUMBER_COUNT");
            double sum = 0.0d;
            for (int number = 0; number < NUMBER_COUNT; number++) {
                double value = input[position][number];
                if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0d)
                    throw new IllegalArgumentException("R815_Q_VALUE_INVALID");
                sum += value;
            }
            if (!(sum > 0.0d) || Double.isInfinite(sum)) throw new IllegalArgumentException("R815_Q_SUM_INVALID");
            for (int number = 0; number < NUMBER_COUNT; number++) out[position][number] = input[position][number] / sum;
        }
        return out;
    }

    private static void validateAssignment(int[] assignment) {
        if (assignment == null || assignment.length != POSITION_COUNT)
            throw new IllegalArgumentException("R815_ASSIGNMENT_SIZE");
        boolean[] seen = new boolean[NUMBER_COUNT + 1];
        for (int i = 0; i < assignment.length; i++) {
            if (assignment[i] < 1 || assignment[i] > NUMBER_COUNT || seen[assignment[i]])
                throw new IllegalArgumentException("R815_ASSIGNMENT_INVALID");
            seen[assignment[i]] = true;
        }
    }

    private static String qHash(double[][] q) {
        StringBuilder out = new StringBuilder(2048);
        for (int p = 0; p < POSITION_COUNT; p++) {
            for (int n = 0; n < NUMBER_COUNT; n++)
                out.append('|').append(Long.toHexString(Double.doubleToLongBits(q[p][n])));
        }
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static double[][] copy(double[][] source) {
        double[][] out = new double[source.length][];
        for (int i = 0; i < source.length; i++) out[i] = source[i].clone();
        return out;
    }

    private static void checkPosition(int position) {
        if (position < 1 || position > POSITION_COUNT) throw new IllegalArgumentException("R815_POSITION_RANGE");
    }

    private static void checkNumber(int number) {
        if (number < 1 || number > NUMBER_COUNT) throw new IllegalArgumentException("R815_NUMBER_RANGE");
    }

    private static boolean sha256(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
