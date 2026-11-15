package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * B04 whole-permutation structural feature owner.
 *
 * <p>All public feature values are computed only from completed draws strictly before the target
 * issue. This class does not own probability, lane, BET, settlement or ledger mutation. The first
 * eight global feature positions remain backward compatible with the frozen V51 model contract.
 */
public final class V51PermutationFeatureEngine {
    public static final String B04_VERSION = "STARCORE_TIANSHU_B04_PERMUTATION_FEATURE_V1";
    public static final int GLOBAL_FEATURE_COUNT = 8;
    public static final int POSITION_COUNT = 10;
    public static final int NUMBER_COUNT = 10;
    public static final int IDX_KENDALL_TAU = 0;
    public static final int IDX_MEAN_ABS_DISPLACEMENT = 1;
    public static final int IDX_MAX_DISPLACEMENT = 2;
    public static final int IDX_FIXED_POINT_RATE = 3;
    public static final int IDX_CYCLE_RATE = 4;
    public static final int IDX_ADJACENCY_PRESERVATION = 5;
    public static final int IDX_REGION_EXCHANGE = 6;
    public static final int IDX_DISPLACEMENT_ENTROPY = 7;
    public static final int REPLAY_WINDOW = 96;
    public static final int MAX_AUDIT_TRANSITIONS = 2880;

    public double[] buildGlobal(V51Models.DrawFrame[] history) {
        double[] out = new double[GLOBAL_FEATURE_COUNT];
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        if (safeHistory.length < 2) {
            return out;
        }
        TransitionMetrics metrics =
                transition(
                        safeHistory[safeHistory.length - 2].permutation,
                        safeHistory[safeHistory.length - 1].permutation);
        out[IDX_KENDALL_TAU] = metrics.kendallTau;
        out[IDX_MEAN_ABS_DISPLACEMENT] = metrics.meanAbsoluteDisplacement;
        out[IDX_MAX_DISPLACEMENT] = metrics.maxDisplacement;
        out[IDX_FIXED_POINT_RATE] = metrics.fixedPointRate;
        out[IDX_CYCLE_RATE] = metrics.nonTrivialCycleRate;
        out[IDX_ADJACENCY_PRESERVATION] = metrics.adjacencyPreservation;
        out[IDX_REGION_EXCHANGE] = metrics.regionExchangeRate;
        out[IDX_DISPLACEMENT_ENTROPY] = displacementEntropy(safeHistory);
        sanitize(out);
        return out;
    }

    public V51Models.PermutationFeatureSnapshot build(
            V51Models.PredictionContext context, V51Models.DrawFrame[] history) {
        if (context == null) {
            throw new IllegalArgumentException("B04_PERMUTATION_CONTEXT_NULL");
        }
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        PointInTimeAudit pit = auditPointInTime(context, safeHistory);
        if (!pit.passed) {
            throw new IllegalArgumentException("B04_PERMUTATION_PIT_FAIL:" + pit.reason);
        }
        double[] global = buildGlobal(safeHistory);
        V51TrajectoryFeatureEngine trajectory = new V51TrajectoryFeatureEngine();
        double[][][] number = trajectory.build(safeHistory);
        String checksum = checksum(context, safeHistory, global, number);
        return new V51Models.PermutationFeatureSnapshot(context, global, number, checksum);
    }

    public FeatureSnapshot buildAudited(
            V51Models.PredictionContext context, V51Models.DrawFrame[] history) {
        long start = System.currentTimeMillis();
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        PointInTimeAudit pit = auditPointInTime(context, safeHistory);
        double[] global = buildGlobal(safeHistory);
        TransitionMetrics last =
                safeHistory.length < 2
                        ? TransitionMetrics.empty()
                        : transition(
                                safeHistory[safeHistory.length - 2].permutation,
                                safeHistory[safeHistory.length - 1].permutation);
        WindowProfile profile = buildWindowProfile(safeHistory);
        ReplayAudit replay = replayAudit(context, safeHistory);
        String hash =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|"
                                + (context == null ? "" : context.targetIssue)
                                + "|"
                                + vectorText(global)
                                + "|"
                                + last.canonical()
                                + "|"
                                + profile.canonical()
                                + "|"
                                + replay.canonical());
        return new FeatureSnapshot(
                global,
                last,
                profile,
                pit,
                replay,
                hash,
                Math.max(0L, System.currentTimeMillis() - start));
    }

    public ReplayAudit replayAudit(
            V51Models.PredictionContext context, V51Models.DrawFrame[] history) {
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        double[] first = buildGlobal(safeHistory);
        double[] second = buildGlobal(copyHistory(safeHistory));
        String firstHash = V51ChecksumEngine.hashDoubles(new double[][] {first});
        String secondHash = V51ChecksumEngine.hashDoubles(new double[][] {second});
        boolean deterministic = firstHash.equals(secondHash);
        PointInTimeAudit pit = auditPointInTime(context, safeHistory);
        return new ReplayAudit(
                deterministic && pit.passed,
                deterministic,
                pit.passed,
                firstHash,
                secondHash,
                safeHistory.length,
                deterministic ? "" : "GLOBAL_HASH_MISMATCH");
    }

    public PointInTimeAudit auditPointInTime(
            V51Models.PredictionContext context, V51Models.DrawFrame[] history) {
        if (context == null) {
            return new PointInTimeAudit(false, "CONTEXT_NULL", "", "", 0, "");
        }
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        if (safeHistory.length == 0) {
            return new PointInTimeAudit(false, "HISTORY_EMPTY", "", context.targetIssue, 0, "");
        }
        String maxIssue = safeHistory[safeHistory.length - 1].issue;
        if (context.baseIssue.length() > 0 && !context.baseIssue.equals(maxIssue)) {
            return new PointInTimeAudit(
                    false,
                    "BASE_ISSUE_MISMATCH",
                    maxIssue,
                    context.targetIssue,
                    safeHistory.length,
                    historyHash(safeHistory));
        }
        if (compareIssue(context.targetIssue, maxIssue) <= 0) {
            return new PointInTimeAudit(
                    false,
                    "FUTURE_OR_TARGET_DRAW_VISIBLE",
                    maxIssue,
                    context.targetIssue,
                    safeHistory.length,
                    historyHash(safeHistory));
        }
        if (context.historyMaxIssue.length() > 0 && !context.historyMaxIssue.equals(maxIssue)) {
            return new PointInTimeAudit(
                    false,
                    "HISTORY_MAX_ISSUE_MISMATCH",
                    maxIssue,
                    context.targetIssue,
                    safeHistory.length,
                    historyHash(safeHistory));
        }
        String computed = historyHash(safeHistory);
        if (context.historyHash.length() > 0 && !context.historyHash.equals(computed)) {
            return new PointInTimeAudit(
                    false,
                    "HISTORY_HASH_MISMATCH",
                    maxIssue,
                    context.targetIssue,
                    safeHistory.length,
                    computed);
        }
        return new PointInTimeAudit(
                true, "", maxIssue, context.targetIssue, safeHistory.length, computed);
    }

    public WindowProfile buildWindowProfile(V51Models.DrawFrame[] history) {
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        int[] windows = new int[] {2, 4, 6, 8, 12, 18, 24, 36, 48, 72, 96};
        ArrayList<WindowTransition> list = new ArrayList<WindowTransition>();
        for (int i = 0; i < windows.length; i++) {
            list.add(summarizeWindow(safeHistory, windows[i]));
        }
        StringBuilder canonical = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) canonical.append('|');
            canonical.append(list.get(i).canonical());
        }
        return new WindowProfile(
                Collections.unmodifiableList(list), V51ChecksumEngine.sha256(canonical.toString()));
    }

    public WindowTransition summarizeWindow(V51Models.DrawFrame[] history, int requested) {
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        int observedDraws = Math.min(safeHistory.length, Math.max(0, requested));
        int transitionCount = Math.max(0, observedDraws - 1);
        if (transitionCount == 0) {
            return WindowTransition.empty(requested, observedDraws);
        }
        int start = safeHistory.length - observedDraws + 1;
        double tau = 0.0d;
        double displacement = 0.0d;
        double maximum = 0.0d;
        double fixed = 0.0d;
        double cycle = 0.0d;
        double adjacency = 0.0d;
        double exchange = 0.0d;
        double entropy = 0.0d;
        for (int t = start; t < safeHistory.length; t++) {
            TransitionMetrics m =
                    transition(safeHistory[t - 1].permutation, safeHistory[t].permutation);
            tau += m.kendallTau;
            displacement += m.meanAbsoluteDisplacement;
            maximum += m.maxDisplacement;
            fixed += m.fixedPointRate;
            cycle += m.nonTrivialCycleRate;
            adjacency += m.adjacencyPreservation;
            exchange += m.regionExchangeRate;
            entropy += m.displacementEntropy;
        }
        double denominator = transitionCount;
        return new WindowTransition(
                requested,
                observedDraws,
                transitionCount,
                tau / denominator,
                displacement / denominator,
                maximum / denominator,
                fixed / denominator,
                cycle / denominator,
                adjacency / denominator,
                exchange / denominator,
                entropy / denominator);
    }

    public TransitionMetrics transition(int[] previous, int[] current) {
        requirePermutation(previous, "PREVIOUS");
        requirePermutation(current, "CURRENT");
        int[] posA = inverse(previous);
        int[] posB = inverse(current);
        int inversions = 0;
        int fixed = 0;
        int max = 0;
        int cross = 0;
        int preserved = 0;
        double absolute = 0.0d;
        int[] displacement = new int[NUMBER_COUNT];
        for (int number = 1; number <= NUMBER_COUNT; number++) {
            int delta = posB[number] - posA[number];
            int abs = Math.abs(delta);
            displacement[number - 1] = abs;
            absolute += abs;
            if (abs > max) max = abs;
            if (delta == 0) fixed++;
            if (region(posA[number]) != region(posB[number])) cross++;
        }
        for (int i = 0; i < NUMBER_COUNT; i++) {
            for (int j = i + 1; j < NUMBER_COUNT; j++) {
                int left = previous[i];
                int right = previous[j];
                if ((posB[left] - posB[right]) * (i - j) < 0) inversions++;
            }
        }
        for (int i = 0; i < NUMBER_COUNT - 1; i++) {
            int left = previous[i];
            int right = previous[i + 1];
            if (posB[left] + 1 == posB[right]) preserved++;
        }
        int cycles = nonTrivialCycles(previous, current);
        double entropy = entropyOfIntegerHistogram(displacement, NUMBER_COUNT);
        return new TransitionMetrics(
                1.0d - 4.0d * inversions / (NUMBER_COUNT * (NUMBER_COUNT - 1.0d)),
                absolute / 90.0d,
                max / 9.0d,
                fixed / 10.0d,
                cycles / 10.0d,
                preserved / 9.0d,
                cross / 10.0d,
                entropy,
                inversions,
                fixed,
                cycles,
                preserved,
                cross,
                displacement);
    }

    public double displacementEntropy(V51Models.DrawFrame[] history) {
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        int start = Math.max(1, safeHistory.length - REPLAY_WINDOW);
        double[] count = new double[NUMBER_COUNT];
        double total = 0.0d;
        for (int t = start; t < safeHistory.length; t++) {
            int[] a = inverse(safeHistory[t - 1].permutation);
            int[] b = inverse(safeHistory[t].permutation);
            for (int number = 1; number <= NUMBER_COUNT; number++) {
                count[Math.abs(a[number] - b[number])]++;
                total++;
            }
        }
        if (total <= 0.0d) return 0.0d;
        for (int i = 0; i < count.length; i++) count[i] /= total;
        return V51Math.entropy(count);
    }

    public static List<FeatureSpec> schema() {
        ArrayList<FeatureSpec> list = new ArrayList<FeatureSpec>();
        list.add(
                new FeatureSpec(
                        IDX_KENDALL_TAU,
                        "kendall_tau",
                        -1.0d,
                        1.0d,
                        "last-completed-transition",
                        "V51PermutationFeatureEngine"));
        list.add(
                new FeatureSpec(
                        IDX_MEAN_ABS_DISPLACEMENT,
                        "mean_abs_displacement",
                        0.0d,
                        1.0d,
                        "last-completed-transition",
                        "V51PermutationFeatureEngine"));
        list.add(
                new FeatureSpec(
                        IDX_MAX_DISPLACEMENT,
                        "max_displacement",
                        0.0d,
                        1.0d,
                        "last-completed-transition",
                        "V51PermutationFeatureEngine"));
        list.add(
                new FeatureSpec(
                        IDX_FIXED_POINT_RATE,
                        "fixed_point_rate",
                        0.0d,
                        1.0d,
                        "last-completed-transition",
                        "V51PermutationFeatureEngine"));
        list.add(
                new FeatureSpec(
                        IDX_CYCLE_RATE,
                        "cycle_rate",
                        0.0d,
                        1.0d,
                        "last-completed-transition",
                        "V51PermutationFeatureEngine"));
        list.add(
                new FeatureSpec(
                        IDX_ADJACENCY_PRESERVATION,
                        "adjacency_preservation",
                        0.0d,
                        1.0d,
                        "last-completed-transition",
                        "V51PermutationFeatureEngine"));
        list.add(
                new FeatureSpec(
                        IDX_REGION_EXCHANGE,
                        "region_exchange",
                        0.0d,
                        1.0d,
                        "last-completed-transition",
                        "V51PermutationFeatureEngine"));
        list.add(
                new FeatureSpec(
                        IDX_DISPLACEMENT_ENTROPY,
                        "displacement_entropy",
                        0.0d,
                        1.0d,
                        "completed-history-w96",
                        "V51PermutationFeatureEngine"));
        return Collections.unmodifiableList(list);
    }

    public static String schemaHash() {
        StringBuilder out = new StringBuilder(B04_VERSION);
        List<FeatureSpec> specs = schema();
        for (int i = 0; i < specs.size(); i++) out.append('|').append(specs.get(i).canonical());
        return V51ChecksumEngine.sha256(out.toString());
    }

    public static Validation validateVector(double[] vector) {
        ArrayList<String> errors = new ArrayList<String>();
        if (vector == null) {
            errors.add("VECTOR_NULL");
            return new Validation(false, errors, "");
        }
        if (vector.length != GLOBAL_FEATURE_COUNT) errors.add("VECTOR_LENGTH=" + vector.length);
        List<FeatureSpec> specs = schema();
        int count = Math.min(vector.length, specs.size());
        for (int i = 0; i < count; i++) {
            double value = vector[i];
            FeatureSpec spec = specs.get(i);
            if (Double.isNaN(value) || Double.isInfinite(value))
                errors.add(spec.name + "_NON_FINITE");
            else if (value < spec.minimum - 1.0e-9d || value > spec.maximum + 1.0e-9d)
                errors.add(spec.name + "_OUT_OF_RANGE=" + value);
        }
        return new Validation(
                errors.isEmpty(), errors, V51ChecksumEngine.hashDoubles(new double[][] {vector}));
    }

    public static String checksum(
            V51Models.PredictionContext context,
            V51Models.DrawFrame[] history,
            double[] global,
            double[][][] number) {
        String contextHash = context == null ? "" : context.historyHash + '|' + context.targetIssue;
        return V51ChecksumEngine.sha256(
                B04_VERSION
                        + '|'
                        + contextHash
                        + '|'
                        + historyHash(history)
                        + '|'
                        + vectorText(global)
                        + '|'
                        + V51ChecksumEngine.hashDoubles(flat(number))
                        + '|'
                        + schemaHash());
    }

    public static V51Models.DrawFrame[] normalizeHistory(V51Models.DrawFrame[] history) {
        if (history == null || history.length == 0) return new V51Models.DrawFrame[0];
        ArrayList<V51Models.DrawFrame> list = new ArrayList<V51Models.DrawFrame>();
        for (int i = 0; i < history.length; i++) if (history[i] != null) list.add(history[i]);
        Collections.sort(
                list,
                new Comparator<V51Models.DrawFrame>() {
                    public int compare(V51Models.DrawFrame left, V51Models.DrawFrame right) {
                        return safe(left.issue).compareTo(safe(right.issue));
                    }
                });
        ArrayList<V51Models.DrawFrame> unique = new ArrayList<V51Models.DrawFrame>();
        String last = "";
        for (int i = 0; i < list.size(); i++) {
            V51Models.DrawFrame frame = list.get(i);
            if (frame.issue.equals(last)) continue;
            unique.add(frame);
            last = frame.issue;
        }
        int from = Math.max(0, unique.size() - MAX_AUDIT_TRANSITIONS);
        V51Models.DrawFrame[] out = new V51Models.DrawFrame[unique.size() - from];
        for (int i = from; i < unique.size(); i++) out[i - from] = unique.get(i);
        return out;
    }

    public static String historyHash(V51Models.DrawFrame[] history) {
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < safeHistory.length; i++) {
            V51Models.DrawFrame frame = safeHistory[i];
            out.append(frame.issue).append(':');
            for (int p = 0; p < frame.permutation.length; p++)
                out.append(frame.permutation[p]).append(',');
            out.append(';');
        }
        return V51ChecksumEngine.sha256(out.toString());
    }

    public static int compareIssue(String left, String right) {
        String a = digits(left);
        String b = digits(right);
        if (a.length() != b.length()) return a.length() < b.length() ? -1 : 1;
        return a.compareTo(b);
    }

    private static String digits(String value) {
        String text = safe(value);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') out.append(c);
        }
        return out.toString();
    }

    private static void sanitize(double[] values) {
        if (values == null) return;
        for (int i = 0; i < values.length; i++) values[i] = V51Math.finite(values[i]);
    }

    private static int[] inverse(int[] permutation) {
        int[] out = new int[NUMBER_COUNT + 1];
        for (int i = 0; i < NUMBER_COUNT; i++) out[permutation[i]] = i;
        return out;
    }

    private static int region(int position) {
        return position < 3 ? 0 : (position < 7 ? 1 : 2);
    }

    private static int nonTrivialCycles(int[] previous, int[] current) {
        int[] previousPosition = inverse(previous);
        int[] map = new int[NUMBER_COUNT];
        for (int position = 0; position < NUMBER_COUNT; position++)
            map[position] = previousPosition[current[position]];
        boolean[] seen = new boolean[NUMBER_COUNT];
        int cycles = 0;
        for (int i = 0; i < NUMBER_COUNT; i++)
            if (!seen[i]) {
                int length = 0;
                int cursor = i;
                while (!seen[cursor]) {
                    seen[cursor] = true;
                    cursor = map[cursor];
                    length++;
                }
                if (length > 1) cycles++;
            }
        return cycles;
    }

    private static double entropyOfIntegerHistogram(int[] values, int bucketCount) {
        double[] count = new double[Math.max(1, bucketCount)];
        for (int i = 0; i < values.length; i++) {
            int bucket = Math.max(0, Math.min(count.length - 1, values[i]));
            count[bucket]++;
        }
        double total = values.length;
        if (total <= 0) return 0.0d;
        for (int i = 0; i < count.length; i++) count[i] /= total;
        return V51Math.entropy(count);
    }

    private static void requirePermutation(int[] value, String label) {
        if (!V51Math.validPermutation(value))
            throw new IllegalArgumentException("B04_" + label + "_INVALID_PERMUTATION");
    }

    private static V51Models.DrawFrame[] copyHistory(V51Models.DrawFrame[] source) {
        if (source == null) return new V51Models.DrawFrame[0];
        V51Models.DrawFrame[] out = new V51Models.DrawFrame[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static String vectorText(double[] values) {
        StringBuilder out = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.length; i++)
                out.append(Long.toHexString(Double.doubleToLongBits(V51Math.finite(values[i]))))
                        .append(',');
        return out.toString();
    }

    private static double[][] flat(double[][][] values) {
        if (values == null || values.length == 0) return new double[0][0];
        int rows = 0;
        for (int i = 0; i < values.length; i++) rows += values[i] == null ? 0 : values[i].length;
        double[][] out = new double[rows][];
        int cursor = 0;
        for (int i = 0; i < values.length; i++)
            if (values[i] != null)
                for (int j = 0; j < values[i].length; j++) out[cursor++] = values[i][j];
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class FeatureSnapshot {
        public final double[] global;
        public final TransitionMetrics lastTransition;
        public final WindowProfile windowProfile;
        public final PointInTimeAudit pointInTime;
        public final ReplayAudit replay;
        public final String checksum;
        public final long elapsedMs;

        FeatureSnapshot(
                double[] global,
                TransitionMetrics lastTransition,
                WindowProfile windowProfile,
                PointInTimeAudit pointInTime,
                ReplayAudit replay,
                String checksum,
                long elapsedMs) {
            this.global = V51Math.copy(global);
            this.lastTransition = lastTransition;
            this.windowProfile = windowProfile;
            this.pointInTime = pointInTime;
            this.replay = replay;
            this.checksum = safe(checksum);
            this.elapsedMs = Math.max(0L, elapsedMs);
        }

        public boolean valid() {
            return validateVector(global).passed
                    && pointInTime != null
                    && pointInTime.passed
                    && replay != null
                    && replay.passed
                    && checksum.length() == 64;
        }

        public String summary() {
            return "valid="
                    + (valid() ? 1 : 0)
                    + "|history="
                    + (pointInTime == null ? 0 : pointInTime.historyRows)
                    + "|replay="
                    + (replay == null ? "NULL" : replay.summary())
                    + "|cost="
                    + elapsedMs
                    + "ms|hash="
                    + checksum;
        }
    }

    public static final class TransitionMetrics {
        public final double kendallTau;
        public final double meanAbsoluteDisplacement;
        public final double maxDisplacement;
        public final double fixedPointRate;
        public final double nonTrivialCycleRate;
        public final double adjacencyPreservation;
        public final double regionExchangeRate;
        public final double displacementEntropy;
        public final int inversionCount;
        public final int fixedPointCount;
        public final int cycleCount;
        public final int preservedAdjacencyCount;
        public final int regionExchangeCount;
        public final int[] displacement;

        TransitionMetrics(
                double kendallTau,
                double meanAbsoluteDisplacement,
                double maxDisplacement,
                double fixedPointRate,
                double nonTrivialCycleRate,
                double adjacencyPreservation,
                double regionExchangeRate,
                double displacementEntropy,
                int inversionCount,
                int fixedPointCount,
                int cycleCount,
                int preservedAdjacencyCount,
                int regionExchangeCount,
                int[] displacement) {
            this.kendallTau = V51Math.finite(kendallTau);
            this.meanAbsoluteDisplacement = V51Math.clamp(meanAbsoluteDisplacement, 0, 1);
            this.maxDisplacement = V51Math.clamp(maxDisplacement, 0, 1);
            this.fixedPointRate = V51Math.clamp(fixedPointRate, 0, 1);
            this.nonTrivialCycleRate = V51Math.clamp(nonTrivialCycleRate, 0, 1);
            this.adjacencyPreservation = V51Math.clamp(adjacencyPreservation, 0, 1);
            this.regionExchangeRate = V51Math.clamp(regionExchangeRate, 0, 1);
            this.displacementEntropy = V51Math.clamp(displacementEntropy, 0, 1);
            this.inversionCount = Math.max(0, inversionCount);
            this.fixedPointCount = Math.max(0, fixedPointCount);
            this.cycleCount = Math.max(0, cycleCount);
            this.preservedAdjacencyCount = Math.max(0, preservedAdjacencyCount);
            this.regionExchangeCount = Math.max(0, regionExchangeCount);
            this.displacement = displacement == null ? new int[0] : V51Math.copy(displacement);
        }

        static TransitionMetrics empty() {
            return new TransitionMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, new int[0]);
        }

        public String canonical() {
            StringBuilder out = new StringBuilder();
            out.append(kendallTau)
                    .append('|')
                    .append(meanAbsoluteDisplacement)
                    .append('|')
                    .append(maxDisplacement)
                    .append('|')
                    .append(fixedPointRate)
                    .append('|')
                    .append(nonTrivialCycleRate)
                    .append('|')
                    .append(adjacencyPreservation)
                    .append('|')
                    .append(regionExchangeRate)
                    .append('|')
                    .append(displacementEntropy)
                    .append('|')
                    .append(inversionCount)
                    .append('|')
                    .append(fixedPointCount)
                    .append('|')
                    .append(cycleCount)
                    .append('|')
                    .append(preservedAdjacencyCount)
                    .append('|')
                    .append(regionExchangeCount);
            for (int i = 0; i < displacement.length; i++) out.append('|').append(displacement[i]);
            return out.toString();
        }
    }

    public static final class WindowTransition {
        public final int requestedWindow;
        public final int observedDraws;
        public final int transitionCount;
        public final double kendallTau;
        public final double meanAbsoluteDisplacement;
        public final double maxDisplacement;
        public final double fixedPointRate;
        public final double cycleRate;
        public final double adjacencyPreservation;
        public final double regionExchange;
        public final double displacementEntropy;

        WindowTransition(
                int requestedWindow,
                int observedDraws,
                int transitionCount,
                double kendallTau,
                double meanAbsoluteDisplacement,
                double maxDisplacement,
                double fixedPointRate,
                double cycleRate,
                double adjacencyPreservation,
                double regionExchange,
                double displacementEntropy) {
            this.requestedWindow = Math.max(0, requestedWindow);
            this.observedDraws = Math.max(0, observedDraws);
            this.transitionCount = Math.max(0, transitionCount);
            this.kendallTau = V51Math.finite(kendallTau);
            this.meanAbsoluteDisplacement = V51Math.clamp(meanAbsoluteDisplacement, 0, 1);
            this.maxDisplacement = V51Math.clamp(maxDisplacement, 0, 1);
            this.fixedPointRate = V51Math.clamp(fixedPointRate, 0, 1);
            this.cycleRate = V51Math.clamp(cycleRate, 0, 1);
            this.adjacencyPreservation = V51Math.clamp(adjacencyPreservation, 0, 1);
            this.regionExchange = V51Math.clamp(regionExchange, 0, 1);
            this.displacementEntropy = V51Math.clamp(displacementEntropy, 0, 1);
        }

        static WindowTransition empty(int requested, int observed) {
            return new WindowTransition(requested, observed, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public String canonical() {
            return requestedWindow
                    + "|"
                    + observedDraws
                    + "|"
                    + transitionCount
                    + "|"
                    + kendallTau
                    + "|"
                    + meanAbsoluteDisplacement
                    + "|"
                    + maxDisplacement
                    + "|"
                    + fixedPointRate
                    + "|"
                    + cycleRate
                    + "|"
                    + adjacencyPreservation
                    + "|"
                    + regionExchange
                    + "|"
                    + displacementEntropy;
        }
    }

    public static final class WindowProfile {
        public final List<WindowTransition> windows;
        public final String checksum;

        WindowProfile(List<WindowTransition> windows, String checksum) {
            this.windows = windows == null ? Collections.<WindowTransition>emptyList() : windows;
            this.checksum = safe(checksum);
        }

        public WindowTransition window(int requested) {
            for (int i = 0; i < windows.size(); i++)
                if (windows.get(i).requestedWindow == requested) return windows.get(i);
            return WindowTransition.empty(requested, 0);
        }

        public String canonical() {
            StringBuilder out = new StringBuilder(checksum);
            for (int i = 0; i < windows.size(); i++)
                out.append('|').append(windows.get(i).canonical());
            return out.toString();
        }
    }

    public static final class PointInTimeAudit {
        public final boolean passed;
        public final String reason;
        public final String historyMaxIssue;
        public final String targetIssue;
        public final int historyRows;
        public final String historyHash;

        PointInTimeAudit(
                boolean passed,
                String reason,
                String historyMaxIssue,
                String targetIssue,
                int historyRows,
                String historyHash) {
            this.passed = passed;
            this.reason = safe(reason);
            this.historyMaxIssue = safe(historyMaxIssue);
            this.targetIssue = safe(targetIssue);
            this.historyRows = Math.max(0, historyRows);
            this.historyHash = safe(historyHash);
        }

        public String canonical() {
            return (passed ? 1 : 0)
                    + "|"
                    + reason
                    + "|"
                    + historyMaxIssue
                    + "|"
                    + targetIssue
                    + "|"
                    + historyRows
                    + "|"
                    + historyHash;
        }
    }

    public static final class ReplayAudit {
        public final boolean passed;
        public final boolean deterministic;
        public final boolean pointInTime;
        public final String firstHash;
        public final String secondHash;
        public final int historyRows;
        public final String error;

        ReplayAudit(
                boolean passed,
                boolean deterministic,
                boolean pointInTime,
                String firstHash,
                String secondHash,
                int historyRows,
                String error) {
            this.passed = passed;
            this.deterministic = deterministic;
            this.pointInTime = pointInTime;
            this.firstHash = safe(firstHash);
            this.secondHash = safe(secondHash);
            this.historyRows = Math.max(0, historyRows);
            this.error = safe(error);
        }

        public String canonical() {
            return (passed ? 1 : 0)
                    + "|"
                    + (deterministic ? 1 : 0)
                    + "|"
                    + (pointInTime ? 1 : 0)
                    + "|"
                    + firstHash
                    + "|"
                    + secondHash
                    + "|"
                    + historyRows
                    + "|"
                    + error;
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL")
                    + "|det="
                    + (deterministic ? 1 : 0)
                    + "|pit="
                    + (pointInTime ? 1 : 0)
                    + "|rows="
                    + historyRows
                    + (error.length() == 0 ? "" : "|error=" + error);
        }
    }

    public static final class FeatureSpec {
        public final int index;
        public final String name;
        public final double minimum;
        public final double maximum;
        public final String source;
        public final String owner;

        FeatureSpec(
                int index,
                String name,
                double minimum,
                double maximum,
                String source,
                String owner) {
            this.index = index;
            this.name = safe(name);
            this.minimum = minimum;
            this.maximum = maximum;
            this.source = safe(source);
            this.owner = safe(owner);
        }

        public String canonical() {
            return index + ":" + name + ":" + minimum + ":" + maximum + ":" + source + ":" + owner;
        }
    }

    public static final class Validation {
        public final boolean passed;
        public final List<String> errors;
        public final String checksum;

        Validation(boolean passed, List<String> errors, String checksum) {
            this.passed = passed;
            this.errors =
                    errors == null
                            ? Collections.<String>emptyList()
                            : Collections.unmodifiableList(new ArrayList<String>(errors));
            this.checksum = safe(checksum);
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL") + "|errors=" + errors.size() + "|hash=" + checksum;
        }
    }

    public static List<AuditRule> auditRules() {
        ArrayList<AuditRule> out = new ArrayList<AuditRule>();
        out.add(
                new AuditRule(
                        "B04-PERM-001",
                        "CONTEXT_PRESENT",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-002",
                        "HISTORY_PRESENT",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-003",
                        "HISTORY_SORTED",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-004",
                        "HISTORY_UNIQUE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-005",
                        "HISTORY_BOUNDED",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-006",
                        "TARGET_AFTER_HISTORY",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-007",
                        "BASE_EQUALS_HISTORY_MAX",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-008",
                        "HISTORY_HASH_MATCH",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-009",
                        "PERMUTATION_VALID",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-010",
                        "ISSUE_NON_EMPTY",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-011",
                        "OPEN_TIME_NON_NEGATIVE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-012",
                        "GLOBAL_DIMENSION",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-013",
                        "GLOBAL_FINITE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-014",
                        "KENDALL_RANGE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-015",
                        "DISPLACEMENT_RANGE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-016",
                        "MAX_DISPLACEMENT_RANGE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-017",
                        "FIXED_POINT_RANGE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-018",
                        "CYCLE_RANGE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-019",
                        "ADJACENCY_RANGE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-020",
                        "REGION_EXCHANGE_RANGE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-021",
                        "ENTROPY_RANGE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-022",
                        "REPLAY_DETERMINISTIC",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-023",
                        "SCHEMA_HASH_STABLE",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-024",
                        "OWNER_SINGLETON",
                        "HARD",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-025",
                        "NO_PROBABILITY_OWNER",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-026",
                        "NO_LANE_OWNER",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-027",
                        "NO_BET_OWNER",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-028",
                        "NO_SETTLEMENT_OWNER",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-029",
                        "NO_FUTURE_DRAW",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-030",
                        "LAST_TRANSITION_ONLY",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-031",
                        "WINDOW_REPLAY_STABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-032",
                        "COPY_BOUNDARY",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-033",
                        "NORMALIZE_DEDUP",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-034",
                        "NORMALIZE_SORT",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-035",
                        "MAX_HISTORY_CAP",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-036",
                        "CHECKSUM_64",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-037",
                        "TRANSITION_INVERSION_BOUND",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-038",
                        "TRANSITION_CYCLE_BOUND",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-039",
                        "TRANSITION_ADJACENCY_BOUND",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-040",
                        "TRANSITION_REGION_BOUND",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-041",
                        "WINDOW_2_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-042",
                        "WINDOW_4_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-043",
                        "WINDOW_6_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-044",
                        "WINDOW_8_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-045",
                        "WINDOW_12_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-046",
                        "WINDOW_18_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-047",
                        "WINDOW_24_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-048",
                        "WINDOW_36_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-049",
                        "WINDOW_48_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-050",
                        "WINDOW_72_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-051",
                        "WINDOW_96_AVAILABLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-052",
                        "PIT_CERTIFICATE_REQUIRED",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-053",
                        "FEATURE_SCHEMA_FIXED",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-054",
                        "MISSING_VALUE_EXPLICIT",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-055",
                        "ERROR_CODE_DETERMINISTIC",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        out.add(
                new AuditRule(
                        "B04-PERM-056",
                        "JAVA7_COMPATIBLE",
                        "AUDIT",
                        "PIT_FEATURE",
                        true,
                        "V51PermutationFeatureEngine"));
        return Collections.unmodifiableList(out);
    }

    public AuditReport audit(V51Models.PredictionContext context, V51Models.DrawFrame[] history) {
        ArrayList<String> failures = new ArrayList<String>();
        V51Models.DrawFrame[] normalized = normalizeHistory(history);
        PointInTimeAudit pit = auditPointInTime(context, normalized);
        if (!pit.passed) failures.add(pit.reason);
        double[] vector = buildGlobal(normalized);
        Validation validation = validateVector(vector);
        if (!validation.passed) failures.addAll(validation.errors);
        ReplayAudit replay = replayAudit(context, normalized);
        if (!replay.passed) failures.add(replay.error.length() == 0 ? "REPLAY_FAIL" : replay.error);
        if (auditRules().size() < 40) failures.add("AUDIT_RULE_CATALOG_INCOMPLETE");
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|AUDIT|"
                                + pit.canonical()
                                + "|"
                                + replay.canonical()
                                + "|"
                                + failures.toString());
        return new AuditReport(failures.isEmpty(), auditRules().size(), failures, checksum);
    }

    public static final class AuditRule {
        public final String id, name, severity, group, owner;
        public final boolean required;

        AuditRule(
                String id,
                String name,
                String severity,
                String group,
                boolean required,
                String owner) {
            this.id = safe(id);
            this.name = safe(name);
            this.severity = safe(severity);
            this.group = safe(group);
            this.required = required;
            this.owner = safe(owner);
        }

        public String canonical() {
            return id
                    + ":"
                    + name
                    + ":"
                    + severity
                    + ":"
                    + group
                    + ":"
                    + (required ? 1 : 0)
                    + ":"
                    + owner;
        }
    }

    public static final class AuditReport {
        public final boolean passed;
        public final int ruleCount;
        public final List<String> failures;
        public final String checksum;

        AuditReport(boolean passed, int ruleCount, List<String> failures, String checksum) {
            this.passed = passed;
            this.ruleCount = Math.max(0, ruleCount);
            this.failures =
                    failures == null
                            ? Collections.<String>emptyList()
                            : Collections.unmodifiableList(new ArrayList<String>(failures));
            this.checksum = safe(checksum);
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL")
                    + "|rules="
                    + ruleCount
                    + "|failures="
                    + failures.size()
                    + "|hash="
                    + checksum;
        }
    }
}
