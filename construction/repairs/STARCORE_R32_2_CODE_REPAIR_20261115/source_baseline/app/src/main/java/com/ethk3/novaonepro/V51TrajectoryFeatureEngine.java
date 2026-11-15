package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * B04 position-number multi-resolution trajectory feature owner.
 *
 * <p>The first eighteen tensor columns preserve the frozen V51 engineering contract. Additional
 * diagnostics are exposed through audited snapshots instead of silently changing the untrained
 * model tensor width.
 */
public final class V51TrajectoryFeatureEngine {
    public static final String B04_VERSION = "STARCORE_TIANSHU_B04_TRAJECTORY_FEATURE_V1";
    public static final int[] WINDOWS = new int[] {4, 6, 8, 12, 18, 24, 36, 48, 72, 96};
    public static final int FEATURE_COUNT = 18;
    public static final int POSITION_COUNT = 10;
    public static final int NUMBER_COUNT = 10;
    public static final int IDX_POSITION = 0;
    public static final int IDX_NUMBER = 1;
    public static final int IDX_FREQ_START = 2;
    public static final int IDX_GAP = 12;
    public static final int IDX_SHORT_LONG = 13;
    public static final int IDX_VELOCITY = 14;
    public static final int IDX_ACCELERATION = 15;
    public static final int IDX_REVERSAL = 16;
    public static final int IDX_LAST_DISTANCE = 17;
    public static final int MAX_HISTORY = 2880;

    public double[][][] build(V51Models.DrawFrame[] history) {
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        double[][][] out = new double[POSITION_COUNT][NUMBER_COUNT][FEATURE_COUNT];
        int total = safeHistory.length;
        int[][] positions = numberPositionTimeline(safeHistory);
        GapStatistics gaps = buildGapStatistics(safeHistory);
        for (int position = 0; position < POSITION_COUNT; position++) {
            for (int number = 0; number < NUMBER_COUNT; number++) {
                double[] feature = out[position][number];
                feature[IDX_POSITION] = position / 9.0d;
                feature[IDX_NUMBER] = number / 9.0d;
                for (int w = 0; w < WINDOWS.length; w++) {
                    int observed = Math.min(total, WINDOWS[w]);
                    int start = total - observed;
                    int count = countAtPosition(safeHistory, start, total, position, number + 1);
                    feature[IDX_FREQ_START + w] = (count + 0.5d) / (observed + 5.0d);
                }
                feature[IDX_GAP] = Math.min(96, gaps.current[position][number]) / 96.0d;
                feature[IDX_SHORT_LONG] = feature[IDX_FREQ_START + 3] - feature[IDX_FREQ_START + 7];
                feature[IDX_VELOCITY] = velocity(positions[number], total, 12);
                feature[IDX_ACCELERATION] =
                        velocity(positions[number], total, 6)
                                - velocity(positions[number], total, 24);
                feature[IDX_REVERSAL] = reversal(positions[number], total, 12);
                int lastNumber = total == 0 ? 5 : safeHistory[total - 1].permutation[position];
                feature[IDX_LAST_DISTANCE] = Math.abs((number + 1) - lastNumber) / 9.0d;
                sanitize(feature);
            }
        }
        return out;
    }

    public TensorSnapshot buildAudited(
            V51Models.PredictionContext context, V51Models.DrawFrame[] history) {
        long start = System.currentTimeMillis();
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        double[][][] tensor = build(safeHistory);
        TensorValidation validation = validate(tensor);
        GapStatistics gaps = buildGapStatistics(safeHistory);
        MotionStatistics motion = buildMotionStatistics(safeHistory);
        FrequencyProfile frequency = buildFrequencyProfile(safeHistory);
        String historyHash = V51PermutationFeatureEngine.historyHash(safeHistory);
        String tensorHash = tensorHash(tensor);
        String contextIssue = context == null ? "" : context.targetIssue;
        boolean pit =
                context != null
                        && safeHistory.length > 0
                        && V51PermutationFeatureEngine.compareIssue(
                                        safeHistory[safeHistory.length - 1].issue,
                                        context.targetIssue)
                                < 0;
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|"
                                + contextIssue
                                + "|"
                                + historyHash
                                + "|"
                                + tensorHash
                                + "|"
                                + gaps.checksum
                                + "|"
                                + motion.checksum
                                + "|"
                                + frequency.checksum
                                + "|"
                                + schemaHash());
        return new TensorSnapshot(
                tensor,
                validation,
                gaps,
                motion,
                frequency,
                pit,
                historyHash,
                tensorHash,
                checksum,
                Math.max(0L, System.currentTimeMillis() - start));
    }

    public TensorValidation validate(double[][][] tensor) {
        ArrayList<String> errors = new ArrayList<String>();
        if (tensor == null) {
            errors.add("TENSOR_NULL");
            return new TensorValidation(false, errors, "");
        }
        if (tensor.length != POSITION_COUNT) errors.add("POSITION_DIM=" + tensor.length);
        int positions = Math.min(POSITION_COUNT, tensor.length);
        for (int p = 0; p < positions; p++) {
            if (tensor[p] == null || tensor[p].length != NUMBER_COUNT) {
                errors.add("NUMBER_DIM_P" + p);
                continue;
            }
            for (int n = 0; n < NUMBER_COUNT; n++) {
                double[] row = tensor[p][n];
                if (row == null || row.length != FEATURE_COUNT) {
                    errors.add("FEATURE_DIM_P" + p + "N" + n);
                    continue;
                }
                for (int f = 0; f < row.length; f++) {
                    double value = row[f];
                    if (Double.isNaN(value) || Double.isInfinite(value))
                        errors.add("NON_FINITE_P" + p + "N" + n + "F" + f);
                    FeatureSpec spec = schema().get(f);
                    if (value < spec.minimum - 1.0e-9d || value > spec.maximum + 1.0e-9d)
                        errors.add("RANGE_P" + p + "N" + n + "F" + f + "=" + value);
                }
            }
        }
        return new TensorValidation(errors.isEmpty(), errors, tensorHash(tensor));
    }

    public GapStatistics buildGapStatistics(V51Models.DrawFrame[] history) {
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        int[][] current = new int[POSITION_COUNT][NUMBER_COUNT];
        double[][] percentile = new double[POSITION_COUNT][NUMBER_COUNT];
        int[][] completedCount = new int[POSITION_COUNT][NUMBER_COUNT];
        int[][] completedLessOrEqual = new int[POSITION_COUNT][NUMBER_COUNT];
        for (int p = 0; p < POSITION_COUNT; p++)
            for (int n = 0; n < NUMBER_COUNT; n++) {
                int last = -1;
                for (int t = safeHistory.length - 1; t >= 0; t--)
                    if (safeHistory[t].permutation[p] == n + 1) {
                        last = t;
                        break;
                    }
                current[p][n] = last < 0 ? safeHistory.length : safeHistory.length - 1 - last;
                int previous = -1;
                for (int t = 0; t < safeHistory.length; t++)
                    if (safeHistory[t].permutation[p] == n + 1) {
                        if (previous >= 0) {
                            int gap = t - previous - 1;
                            completedCount[p][n]++;
                            if (gap <= current[p][n]) completedLessOrEqual[p][n]++;
                        }
                        previous = t;
                    }
                percentile[p][n] =
                        completedCount[p][n] == 0
                                ? 0.5d
                                : completedLessOrEqual[p][n] / (double) completedCount[p][n];
            }
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|GAP|"
                                + intMatrixText(current)
                                + "|"
                                + matrixText(percentile)
                                + "|"
                                + intMatrixText(completedCount));
        return new GapStatistics(current, percentile, completedCount, checksum);
    }

    public MotionStatistics buildMotionStatistics(V51Models.DrawFrame[] history) {
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        int[][] positions = numberPositionTimeline(safeHistory);
        double[] velocity6 = new double[NUMBER_COUNT];
        double[] velocity12 = new double[NUMBER_COUNT];
        double[] velocity24 = new double[NUMBER_COUNT];
        double[] acceleration = new double[NUMBER_COUNT];
        double[] reversal12 = new double[NUMBER_COUNT];
        double[] reversal24 = new double[NUMBER_COUNT];
        double[] mobility = new double[NUMBER_COUNT];
        for (int n = 0; n < NUMBER_COUNT; n++) {
            velocity6[n] = velocity(positions[n], safeHistory.length, 6);
            velocity12[n] = velocity(positions[n], safeHistory.length, 12);
            velocity24[n] = velocity(positions[n], safeHistory.length, 24);
            acceleration[n] = velocity6[n] - velocity24[n];
            reversal12[n] = reversal(positions[n], safeHistory.length, 12);
            reversal24[n] = reversal(positions[n], safeHistory.length, 24);
            mobility[n] = mobility(positions[n], safeHistory.length, 48);
        }
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|MOTION|"
                                + vectorText(velocity6)
                                + "|"
                                + vectorText(velocity12)
                                + "|"
                                + vectorText(velocity24)
                                + "|"
                                + vectorText(acceleration)
                                + "|"
                                + vectorText(reversal12)
                                + "|"
                                + vectorText(reversal24)
                                + "|"
                                + vectorText(mobility));
        return new MotionStatistics(
                velocity6,
                velocity12,
                velocity24,
                acceleration,
                reversal12,
                reversal24,
                mobility,
                checksum);
    }

    public FrequencyProfile buildFrequencyProfile(V51Models.DrawFrame[] history) {
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        double[][][] frequency = new double[WINDOWS.length][POSITION_COUNT][NUMBER_COUNT];
        double[][] entropy = new double[WINDOWS.length][POSITION_COUNT];
        double[][] concentration = new double[WINDOWS.length][POSITION_COUNT];
        for (int w = 0; w < WINDOWS.length; w++) {
            int observed = Math.min(safeHistory.length, WINDOWS[w]);
            int start = safeHistory.length - observed;
            for (int p = 0; p < POSITION_COUNT; p++) {
                double[] row = new double[NUMBER_COUNT];
                for (int n = 0; n < NUMBER_COUNT; n++) {
                    int count = countAtPosition(safeHistory, start, safeHistory.length, p, n + 1);
                    row[n] = frequency[w][p][n] = (count + 0.5d) / (observed + 5.0d);
                }
                entropy[w][p] = normalizedEntropy(row);
                concentration[w][p] = topKSum(row, 3);
            }
        }
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|FREQUENCY|"
                                + tensorHash(frequency)
                                + "|"
                                + matrixText(entropy)
                                + "|"
                                + matrixText(concentration));
        return new FrequencyProfile(frequency, entropy, concentration, checksum);
    }

    public ReplayAudit replayAudit(V51Models.DrawFrame[] history) {
        V51Models.DrawFrame[] safeHistory = normalizeHistory(history);
        double[][][] first = build(safeHistory);
        double[][][] second = build(copyHistory(safeHistory));
        String firstHash = tensorHash(first);
        String secondHash = tensorHash(second);
        TensorValidation validation = validate(first);
        return new ReplayAudit(
                firstHash.equals(secondHash) && validation.passed,
                firstHash,
                secondHash,
                validation,
                safeHistory.length);
    }

    public static List<FeatureSpec> schema() {
        ArrayList<FeatureSpec> list = new ArrayList<FeatureSpec>();
        list.add(new FeatureSpec(IDX_POSITION, "position_norm", 0, 1, 0, "identity"));
        list.add(new FeatureSpec(IDX_NUMBER, "number_norm", 0, 1, 0, "identity"));
        for (int i = 0; i < WINDOWS.length; i++)
            list.add(
                    new FeatureSpec(
                            IDX_FREQ_START + i,
                            "frequency_w" + WINDOWS[i],
                            0,
                            1,
                            WINDOWS[i],
                            "beta-smoothed"));
        list.add(new FeatureSpec(IDX_GAP, "gap_norm", 0, 1, 96, "clip-96"));
        list.add(new FeatureSpec(IDX_SHORT_LONG, "short_long_delta", -1, 1, 48, "w12-minus-w48"));
        list.add(
                new FeatureSpec(IDX_VELOCITY, "trajectory_velocity", -1, 1, 12, "mean-delta-div9"));
        list.add(
                new FeatureSpec(
                        IDX_ACCELERATION, "trajectory_acceleration", -2, 2, 24, "v6-minus-v24"));
        list.add(
                new FeatureSpec(IDX_REVERSAL, "trajectory_reversal", 0, 1, 12, "sign-change-rate"));
        list.add(
                new FeatureSpec(
                        IDX_LAST_DISTANCE,
                        "last_number_distance",
                        0,
                        1,
                        1,
                        "number-distance-div9"));
        return Collections.unmodifiableList(list);
    }

    public static String schemaHash() {
        StringBuilder out = new StringBuilder(B04_VERSION);
        List<FeatureSpec> list = schema();
        for (int i = 0; i < list.size(); i++) out.append('|').append(list.get(i).canonical());
        return V51ChecksumEngine.sha256(out.toString());
    }

    public static V51Models.DrawFrame[] normalizeHistory(V51Models.DrawFrame[] history) {
        if (history == null || history.length == 0) return new V51Models.DrawFrame[0];
        ArrayList<V51Models.DrawFrame> list = new ArrayList<V51Models.DrawFrame>();
        for (int i = 0; i < history.length; i++) if (history[i] != null) list.add(history[i]);
        Collections.sort(
                list,
                new Comparator<V51Models.DrawFrame>() {
                    public int compare(V51Models.DrawFrame left, V51Models.DrawFrame right) {
                        return left.issue.compareTo(right.issue);
                    }
                });
        ArrayList<V51Models.DrawFrame> unique = new ArrayList<V51Models.DrawFrame>();
        String last = "";
        for (int i = 0; i < list.size(); i++) {
            V51Models.DrawFrame f = list.get(i);
            if (f.issue.equals(last)) continue;
            unique.add(f);
            last = f.issue;
        }
        int from = Math.max(0, unique.size() - MAX_HISTORY);
        V51Models.DrawFrame[] out = new V51Models.DrawFrame[unique.size() - from];
        for (int i = from; i < unique.size(); i++) out[i - from] = unique.get(i);
        return out;
    }

    private static int[][] numberPositionTimeline(V51Models.DrawFrame[] history) {
        int[][] out = new int[NUMBER_COUNT][Math.max(1, history.length)];
        for (int t = 0; t < history.length; t++)
            for (int p = 0; p < POSITION_COUNT; p++) out[history[t].permutation[p] - 1][t] = p;
        return out;
    }

    private static int countAtPosition(
            V51Models.DrawFrame[] history, int start, int end, int position, int number) {
        int count = 0;
        for (int t = Math.max(0, start); t < Math.min(history.length, end); t++)
            if (history[t].permutation[position] == number) count++;
        return count;
    }

    private static double velocity(int[] positions, int total, int window) {
        int start = Math.max(1, total - window);
        if (total - start < 1) return 0.0d;
        double sum = 0.0d;
        int count = 0;
        for (int t = start; t < total; t++) {
            sum += (positions[t] - positions[t - 1]) / 9.0d;
            count++;
        }
        return count == 0 ? 0.0d : sum / count;
    }

    private static double reversal(int[] positions, int total, int window) {
        int start = Math.max(2, total - window);
        int changes = 0;
        int count = 0;
        int last = 0;
        for (int t = start; t < total; t++) {
            int d = positions[t] - positions[t - 1];
            int sign = d > 0 ? 1 : (d < 0 ? -1 : 0);
            if (last != 0 && sign != 0 && sign != last) changes++;
            if (sign != 0) {
                last = sign;
                count++;
            }
        }
        return count < 2 ? 0.0d : changes / (double) (count - 1);
    }

    private static double mobility(int[] positions, int total, int window) {
        int start = Math.max(1, total - window);
        if (total - start < 1) return 0.0d;
        double sum = 0.0d;
        int count = 0;
        for (int t = start; t < total; t++) {
            sum += Math.abs(positions[t] - positions[t - 1]) / 9.0d;
            count++;
        }
        return count == 0 ? 0.0d : sum / count;
    }

    private static double normalizedEntropy(double[] row) {
        double total = 0.0d;
        for (int i = 0; i < row.length; i++) total += Math.max(0, row[i]);
        if (total <= 0) return 0.0d;
        double[] p = new double[row.length];
        for (int i = 0; i < row.length; i++) p[i] = Math.max(0, row[i]) / total;
        double max = Math.log(row.length);
        return max <= 0 ? 0.0d : V51Math.entropy(p) / max;
    }

    private static double topKSum(double[] row, int k) {
        double[] copy = V51Math.copy(row);
        java.util.Arrays.sort(copy);
        double sum = 0.0d;
        for (int i = 0; i < Math.min(k, copy.length); i++) sum += copy[copy.length - 1 - i];
        return V51Math.clamp(sum, 0, 1);
    }

    private static void sanitize(double[] row) {
        for (int i = 0; i < row.length; i++) row[i] = V51Math.finite(row[i]);
    }

    private static V51Models.DrawFrame[] copyHistory(V51Models.DrawFrame[] source) {
        V51Models.DrawFrame[] out = new V51Models.DrawFrame[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    public static String tensorHash(double[][][] tensor) {
        StringBuilder out = new StringBuilder();
        if (tensor != null)
            for (int p = 0; p < tensor.length; p++)
                if (tensor[p] != null)
                    for (int n = 0; n < tensor[p].length; n++) {
                        double[] row = tensor[p][n];
                        if (row == null) {
                            out.append("null;");
                            continue;
                        }
                        for (int f = 0; f < row.length; f++)
                            out.append(
                                            Long.toHexString(
                                                    Double.doubleToLongBits(
                                                            V51Math.finite(row[f]))))
                                    .append(',');
                        out.append(';');
                    }
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static String vectorText(double[] values) {
        StringBuilder out = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.length; i++)
                out.append(Long.toHexString(Double.doubleToLongBits(V51Math.finite(values[i]))))
                        .append(',');
        return out.toString();
    }

    private static String matrixText(double[][] values) {
        StringBuilder out = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.length; i++) out.append(vectorText(values[i])).append(';');
        return out.toString();
    }

    private static String intMatrixText(int[][] values) {
        StringBuilder out = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.length; i++) {
                if (values[i] != null)
                    for (int j = 0; j < values[i].length; j++) out.append(values[i][j]).append(',');
                out.append(';');
            }
        return out.toString();
    }

    public static final class TensorSnapshot {
        public final double[][][] tensor;
        public final TensorValidation validation;
        public final GapStatistics gaps;
        public final MotionStatistics motion;
        public final FrequencyProfile frequency;
        public final boolean pointInTime;
        public final String historyHash, tensorHash, checksum;
        public final long elapsedMs;

        TensorSnapshot(
                double[][][] tensor,
                TensorValidation validation,
                GapStatistics gaps,
                MotionStatistics motion,
                FrequencyProfile frequency,
                boolean pointInTime,
                String historyHash,
                String tensorHash,
                String checksum,
                long elapsedMs) {
            this.tensor = tensor;
            this.validation = validation;
            this.gaps = gaps;
            this.motion = motion;
            this.frequency = frequency;
            this.pointInTime = pointInTime;
            this.historyHash = safe(historyHash);
            this.tensorHash = safe(tensorHash);
            this.checksum = safe(checksum);
            this.elapsedMs = Math.max(0L, elapsedMs);
        }

        public boolean valid() {
            return pointInTime
                    && validation != null
                    && validation.passed
                    && checksum.length() == 64;
        }

        public String summary() {
            return "valid="
                    + (valid() ? 1 : 0)
                    + "|pit="
                    + (pointInTime ? 1 : 0)
                    + "|"
                    + (validation == null ? "validation=NULL" : validation.summary())
                    + "|cost="
                    + elapsedMs
                    + "ms|hash="
                    + checksum;
        }
    }

    public static final class GapStatistics {
        public final int[][] current;
        public final double[][] percentile;
        public final int[][] completedCount;
        public final String checksum;

        GapStatistics(
                int[][] current, double[][] percentile, int[][] completedCount, String checksum) {
            this.current = copy(current);
            this.percentile = V51Math.copy(percentile);
            this.completedCount = copy(completedCount);
            this.checksum = safe(checksum);
        }

        public double percentile(int position, int number) {
            return percentile[position][number];
        }

        public String summary() {
            int max = 0;
            for (int p = 0; p < current.length; p++)
                for (int n = 0; n < current[p].length; n++) max = Math.max(max, current[p][n]);
            return "maxGap=" + max + "|hash=" + checksum;
        }
    }

    public static final class MotionStatistics {
        public final double[] velocity6,
                velocity12,
                velocity24,
                acceleration,
                reversal12,
                reversal24,
                mobility;
        public final String checksum;

        MotionStatistics(
                double[] velocity6,
                double[] velocity12,
                double[] velocity24,
                double[] acceleration,
                double[] reversal12,
                double[] reversal24,
                double[] mobility,
                String checksum) {
            this.velocity6 = V51Math.copy(velocity6);
            this.velocity12 = V51Math.copy(velocity12);
            this.velocity24 = V51Math.copy(velocity24);
            this.acceleration = V51Math.copy(acceleration);
            this.reversal12 = V51Math.copy(reversal12);
            this.reversal24 = V51Math.copy(reversal24);
            this.mobility = V51Math.copy(mobility);
            this.checksum = safe(checksum);
        }

        public String summary() {
            return "meanMobility="
                    + mean(mobility)
                    + "|meanReversal="
                    + mean(reversal12)
                    + "|hash="
                    + checksum;
        }
    }

    public static final class FrequencyProfile {
        public final double[][][] frequency;
        public final double[][] entropy, concentration;
        public final String checksum;

        FrequencyProfile(
                double[][][] frequency,
                double[][] entropy,
                double[][] concentration,
                String checksum) {
            this.frequency = frequency;
            this.entropy = V51Math.copy(entropy);
            this.concentration = V51Math.copy(concentration);
            this.checksum = safe(checksum);
        }

        public String summary() {
            return "windows="
                    + frequency.length
                    + "|meanEntropy="
                    + mean(entropy)
                    + "|meanTop3="
                    + mean(concentration)
                    + "|hash="
                    + checksum;
        }
    }

    public static final class TensorValidation {
        public final boolean passed;
        public final List<String> errors;
        public final String checksum;

        TensorValidation(boolean passed, List<String> errors, String checksum) {
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

    public static final class ReplayAudit {
        public final boolean passed;
        public final String firstHash, secondHash;
        public final TensorValidation validation;
        public final int historyRows;

        ReplayAudit(
                boolean passed,
                String firstHash,
                String secondHash,
                TensorValidation validation,
                int historyRows) {
            this.passed = passed;
            this.firstHash = safe(firstHash);
            this.secondHash = safe(secondHash);
            this.validation = validation;
            this.historyRows = Math.max(0, historyRows);
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL")
                    + "|rows="
                    + historyRows
                    + "|first="
                    + firstHash
                    + "|second="
                    + secondHash;
        }
    }

    public static final class FeatureSpec {
        public final int index;
        public final String name;
        public final double minimum, maximum;
        public final int window;
        public final String normalization;

        FeatureSpec(
                int index,
                String name,
                double minimum,
                double maximum,
                int window,
                String normalization) {
            this.index = index;
            this.name = safe(name);
            this.minimum = minimum;
            this.maximum = maximum;
            this.window = window;
            this.normalization = safe(normalization);
        }

        public String canonical() {
            return index
                    + ":"
                    + name
                    + ":"
                    + minimum
                    + ":"
                    + maximum
                    + ":"
                    + window
                    + ":"
                    + normalization;
        }
    }

    private static int[][] copy(int[][] source) {
        if (source == null) return new int[0][0];
        int[][] out = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            out[i] = new int[source[i].length];
            System.arraycopy(source[i], 0, out[i], 0, source[i].length);
        }
        return out;
    }

    private static double mean(double[] x) {
        if (x == null || x.length == 0) return 0;
        double s = 0;
        for (int i = 0; i < x.length; i++) s += V51Math.finite(x[i]);
        return s / x.length;
    }

    private static double mean(double[][] x) {
        if (x == null) return 0;
        double s = 0;
        int c = 0;
        for (int i = 0; i < x.length; i++)
            if (x[i] != null)
                for (int j = 0; j < x[i].length; j++) {
                    s += V51Math.finite(x[i][j]);
                    c++;
                }
        return c == 0 ? 0 : s / c;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static List<ContractRule> contractRules() {
        ArrayList<ContractRule> out = new ArrayList<ContractRule>();
        out.add(
                new ContractRule(
                        "B04-TRAJ-001",
                        "FREQ_W4_FINITE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-002",
                        "FREQ_W4_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-003",
                        "FREQ_W4_PIT",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-004",
                        "FREQ_W4_DETERMINISTIC",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-005",
                        "FREQ_W6_FINITE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-006",
                        "FREQ_W6_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-007",
                        "FREQ_W6_PIT",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-008",
                        "FREQ_W6_DETERMINISTIC",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-009",
                        "FREQ_W8_FINITE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-010",
                        "FREQ_W8_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-011",
                        "FREQ_W8_PIT",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-012",
                        "FREQ_W8_DETERMINISTIC",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-013",
                        "FREQ_W12_FINITE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-014",
                        "FREQ_W12_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-015",
                        "FREQ_W12_PIT",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-016",
                        "FREQ_W12_DETERMINISTIC",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-017",
                        "FREQ_W18_FINITE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-018",
                        "FREQ_W18_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-019",
                        "FREQ_W18_PIT",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-020",
                        "FREQ_W18_DETERMINISTIC",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-021",
                        "FREQ_W24_FINITE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-022",
                        "FREQ_W24_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-023",
                        "FREQ_W24_PIT",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-024",
                        "FREQ_W24_DETERMINISTIC",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-025",
                        "FREQ_W36_FINITE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-026",
                        "FREQ_W36_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-027",
                        "FREQ_W36_PIT",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-028",
                        "FREQ_W36_DETERMINISTIC",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-029",
                        "FREQ_W48_FINITE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-030",
                        "FREQ_W48_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-031",
                        "FREQ_W48_PIT",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-032",
                        "FREQ_W48_DETERMINISTIC",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-033",
                        "FREQ_W72_FINITE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-034",
                        "FREQ_W72_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-035",
                        "FREQ_W72_PIT",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-036",
                        "FREQ_W72_DETERMINISTIC",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-037",
                        "FREQ_W96_FINITE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-038",
                        "FREQ_W96_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-039",
                        "FREQ_W96_PIT",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-040",
                        "FREQ_W96_DETERMINISTIC",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-041",
                        "TENSOR_POSITION_DIM",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-042",
                        "TENSOR_NUMBER_DIM",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-043",
                        "TENSOR_FEATURE_DIM",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-044",
                        "POSITION_NORM_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-045",
                        "NUMBER_NORM_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-046",
                        "GAP_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-047",
                        "SHORT_LONG_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-048",
                        "VELOCITY_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-049",
                        "ACCELERATION_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-050",
                        "REVERSAL_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-051",
                        "LAST_DISTANCE_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-052",
                        "GAP_PERCENTILE_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-053",
                        "GAP_COMPLETED_COUNT_NONNEGATIVE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-054",
                        "MOTION_V6_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-055",
                        "MOTION_V12_RANGE",
                        "HARD",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-056",
                        "MOTION_V24_RANGE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-057",
                        "MOTION_ACCELERATION_RANGE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-058",
                        "MOTION_REVERSAL12_RANGE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-059",
                        "MOTION_REVERSAL24_RANGE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-060",
                        "MOBILITY_RANGE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-061",
                        "FREQUENCY_ENTROPY_RANGE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-062",
                        "FREQUENCY_CONCENTRATION_RANGE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-063",
                        "HISTORY_SORTED",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-064",
                        "HISTORY_UNIQUE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-065",
                        "HISTORY_CAP_2880",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-066",
                        "REPLAY_HASH_EQUAL",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-067",
                        "SCHEMA_COUNT_18",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-068",
                        "SCHEMA_HASH_64",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-069",
                        "NO_FUTURE_DRAW",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-070",
                        "NO_PROBABILITY_OWNER",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-071",
                        "NO_LANE_OWNER",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-072",
                        "NO_BET_OWNER",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-073",
                        "NO_SETTLEMENT_OWNER",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-074",
                        "MISSING_EXPLICIT",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-075",
                        "FINITE_ALL_CELLS",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-076",
                        "CHECKSUM_64",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-077",
                        "JAVA7_COMPATIBLE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-078",
                        "THREAD_STATELESS",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-079",
                        "INPUT_NOT_RETAINED",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-080",
                        "POSITION_1_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-081",
                        "POSITION_2_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-082",
                        "POSITION_3_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-083",
                        "POSITION_4_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-084",
                        "POSITION_5_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-085",
                        "POSITION_6_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-086",
                        "POSITION_7_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-087",
                        "POSITION_8_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-088",
                        "POSITION_9_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-089",
                        "POSITION_10_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-090",
                        "NUMBER_1_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-091",
                        "NUMBER_2_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-092",
                        "NUMBER_3_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-093",
                        "NUMBER_4_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-094",
                        "NUMBER_5_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-095",
                        "NUMBER_6_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-096",
                        "NUMBER_7_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-097",
                        "NUMBER_8_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-098",
                        "NUMBER_9_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        out.add(
                new ContractRule(
                        "B04-TRAJ-099",
                        "NUMBER_10_COMPLETE",
                        "AUDIT",
                        "TRAJECTORY_TENSOR",
                        true,
                        "V51TrajectoryFeatureEngine"));
        return Collections.unmodifiableList(out);
    }

    public ContractReport verifyContract(
            V51Models.PredictionContext context, V51Models.DrawFrame[] history) {
        ArrayList<String> failures = new ArrayList<String>();
        V51Models.DrawFrame[] normalized = normalizeHistory(history);
        double[][][] tensor = build(normalized);
        TensorValidation validation = validate(tensor);
        if (!validation.passed) failures.addAll(validation.errors);
        ReplayAudit replay = replayAudit(normalized);
        if (!replay.passed) failures.add("REPLAY_HASH_MISMATCH");
        if (context == null) failures.add("CONTEXT_NULL");
        else if (normalized.length > 0
                && V51PermutationFeatureEngine.compareIssue(
                                normalized[normalized.length - 1].issue, context.targetIssue)
                        >= 0) failures.add("FUTURE_DRAW_VISIBLE");
        if (schema().size() != FEATURE_COUNT) failures.add("SCHEMA_SIZE_MISMATCH");
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|CONTRACT|"
                                + validation.summary()
                                + "|"
                                + replay.summary()
                                + "|"
                                + failures.toString());
        return new ContractReport(failures.isEmpty(), contractRules().size(), failures, checksum);
    }

    public PositionNumberDiagnostics diagnose(
            V51Models.DrawFrame[] history, int position, int number) {
        if (position < 1 || position > 10 || number < 1 || number > 10)
            throw new IllegalArgumentException("B04_TRAJECTORY_COORDINATE");
        V51Models.DrawFrame[] normalized = normalizeHistory(history);
        double[][][] tensor = build(normalized);
        GapStatistics gaps = buildGapStatistics(normalized);
        MotionStatistics motion = buildMotionStatistics(normalized);
        double[] features = V51Math.copy(tensor[position - 1][number - 1]);
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|DIAG|"
                                + position
                                + "|"
                                + number
                                + "|"
                                + vectorText(features)
                                + "|"
                                + gaps.checksum
                                + "|"
                                + motion.checksum);
        return new PositionNumberDiagnostics(
                position,
                number,
                features,
                gaps.current[position - 1][number - 1],
                gaps.percentile[position - 1][number - 1],
                motion.velocity12[number - 1],
                motion.acceleration[number - 1],
                motion.reversal12[number - 1],
                checksum);
    }

    public static final class ContractRule {
        public final String id, name, severity, group, owner;
        public final boolean required;

        ContractRule(
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

    public static final class ContractReport {
        public final boolean passed;
        public final int ruleCount;
        public final List<String> failures;
        public final String checksum;

        ContractReport(boolean passed, int ruleCount, List<String> failures, String checksum) {
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

    public static final class PositionNumberDiagnostics {
        public final int position, number, currentGap;
        public final double[] features;
        public final double gapPercentile, velocity, acceleration, reversal;
        public final String checksum;

        PositionNumberDiagnostics(
                int position,
                int number,
                double[] features,
                int currentGap,
                double gapPercentile,
                double velocity,
                double acceleration,
                double reversal,
                String checksum) {
            this.position = position;
            this.number = number;
            this.features = V51Math.copy(features);
            this.currentGap = Math.max(0, currentGap);
            this.gapPercentile = V51Math.clamp(gapPercentile, 0, 1);
            this.velocity = V51Math.finite(velocity);
            this.acceleration = V51Math.finite(acceleration);
            this.reversal = V51Math.clamp(reversal, 0, 1);
            this.checksum = safe(checksum);
        }

        public String summary() {
            return "P"
                    + position
                    + "N"
                    + number
                    + "|gap="
                    + currentGap
                    + "|pct="
                    + gapPercentile
                    + "|v="
                    + velocity
                    + "|a="
                    + acceleration
                    + "|r="
                    + reversal
                    + "|hash="
                    + checksum;
        }
    }
}
