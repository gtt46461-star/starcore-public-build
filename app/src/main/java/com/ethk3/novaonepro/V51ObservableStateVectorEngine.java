package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * B04 objective continuous state-vector owner.
 *
 * <p>No HOT/WATCH/RISK string is used to create a production action. The vector is deterministic,
 * bounded, replayable and carries an explicit missing mask through the audited API.
 */
public final class V51ObservableStateVectorEngine {
    public static final String B04_VERSION = "STARCORE_TIANSHU_B04_OBSERVABLE_STATE_V1";
    public static final int DIMENSION = 14;
    public static final int POSITION_COUNT = 10;
    public static final int IDX_GLOBAL_START = 0;
    public static final int IDX_ENTROPY = 8;
    public static final int IDX_MAX_GAP = 9;
    public static final int IDX_DRIFT = 10;
    public static final int IDX_VELOCITY = 11;
    public static final int IDX_ACCELERATION = 12;
    public static final int IDX_REVERSAL = 13;

    public double[][] build(V51Models.PermutationFeatureSnapshot snapshot) {
        if (snapshot == null) throw new IllegalArgumentException("B04_STATE_SNAPSHOT_NULL");
        double[][] out = new double[POSITION_COUNT][DIMENSION];
        for (int position = 0; position < POSITION_COUNT; position++) {
            double maxGap = 0.0d;
            double drift = 0.0d;
            double velocity = 0.0d;
            double acceleration = 0.0d;
            double reversal = 0.0d;
            double[] distribution = new double[10];
            for (int number = 0; number < 10; number++) {
                double[] feature = snapshot.numberFeatures[position][number];
                distribution[number] = feature[V51TrajectoryFeatureEngine.IDX_FREQ_START + 5];
                maxGap = Math.max(maxGap, feature[V51TrajectoryFeatureEngine.IDX_GAP]);
                drift += Math.abs(feature[V51TrajectoryFeatureEngine.IDX_SHORT_LONG]);
                velocity += Math.abs(feature[V51TrajectoryFeatureEngine.IDX_VELOCITY]);
                acceleration += Math.abs(feature[V51TrajectoryFeatureEngine.IDX_ACCELERATION]);
                reversal += feature[V51TrajectoryFeatureEngine.IDX_REVERSAL];
            }
            double[] vector = out[position];
            for (int i = 0; i < 8; i++) vector[i] = safeGlobal(snapshot.globalFeatures, i);
            vector[IDX_ENTROPY] = normalizedEntropy(distribution);
            vector[IDX_MAX_GAP] = maxGap;
            vector[IDX_DRIFT] = drift / 10.0d;
            vector[IDX_VELOCITY] = velocity / 10.0d;
            vector[IDX_ACCELERATION] = acceleration / 10.0d;
            vector[IDX_REVERSAL] = reversal / 10.0d;
            sanitize(vector);
        }
        return out;
    }

    public StateVectorSnapshot buildAudited(V51Models.PermutationFeatureSnapshot snapshot) {
        long start = System.currentTimeMillis();
        double[][] vectors = build(snapshot);
        boolean[][] missing = missingMask(snapshot);
        Validation validation = validate(vectors, missing);
        PositionSummary[] summaries = summarize(vectors);
        String vectorHash = hash(vectors);
        String missingHash = hash(missing);
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|"
                                + (snapshot.context == null ? "" : snapshot.context.targetIssue)
                                + "|"
                                + vectorHash
                                + "|"
                                + missingHash
                                + "|"
                                + schemaHash());
        return new StateVectorSnapshot(
                vectors,
                missing,
                summaries,
                validation,
                vectorHash,
                missingHash,
                checksum,
                Math.max(0L, System.currentTimeMillis() - start));
    }

    public double[] positionVector(V51Models.PermutationFeatureSnapshot snapshot, int position) {
        if (position < 1 || position > POSITION_COUNT)
            throw new IllegalArgumentException("B04_STATE_POSITION");
        return V51Math.copy(build(snapshot)[position - 1]);
    }

    public double distance(double[] left, double[] right) {
        if (left == null || right == null) return 1.0d;
        int count = Math.min(left.length, right.length);
        if (count == 0) return 1.0d;
        double sum = 0.0d;
        for (int i = 0; i < count; i++) {
            double scale = Math.max(1.0e-6d, schema().get(Math.min(i, DIMENSION - 1)).scale);
            double d = (V51Math.finite(left[i]) - V51Math.finite(right[i])) / scale;
            sum += d * d;
        }
        return Math.sqrt(sum / count);
    }

    public double changeScore(
            V51Models.PermutationFeatureSnapshot current,
            V51Models.PermutationFeatureSnapshot previous,
            int position) {
        if (current == null) return 1.0d;
        double[][] now = build(current);
        if (previous == null) return 1.0d;
        double[][] old = build(previous);
        return V51Math.clamp(distance(now[position - 1], old[position - 1]) / 2.0d, 0, 1);
    }

    public boolean[][] missingMask(V51Models.PermutationFeatureSnapshot snapshot) {
        boolean[][] out = new boolean[POSITION_COUNT][DIMENSION];
        for (int p = 0; p < POSITION_COUNT; p++)
            for (int i = 0; i < DIMENSION; i++) out[p][i] = isMissing(snapshot, p, i);
        return out;
    }

    private boolean isMissing(
            V51Models.PermutationFeatureSnapshot snapshot, int position, int index) {
        if (snapshot == null || snapshot.context == null) return true;
        if (index < 8)
            return snapshot.globalFeatures == null
                    || snapshot.globalFeatures.length <= index
                    || !finite(snapshot.globalFeatures[index]);
        if (snapshot.numberFeatures == null
                || snapshot.numberFeatures.length <= position
                || snapshot.numberFeatures[position] == null
                || snapshot.numberFeatures[position].length < 10) return true;
        for (int n = 0; n < 10; n++) {
            double[] row = snapshot.numberFeatures[position][n];
            if (row == null || row.length < V51TrajectoryFeatureEngine.FEATURE_COUNT) return true;
        }
        return false;
    }

    public Validation validate(double[][] vectors, boolean[][] missing) {
        ArrayList<String> errors = new ArrayList<String>();
        if (vectors == null) {
            errors.add("VECTOR_MATRIX_NULL");
            return new Validation(false, errors, 0, "", "");
        }
        if (vectors.length != POSITION_COUNT) errors.add("POSITION_DIM=" + vectors.length);
        int missingCount = 0;
        List<ComponentSpec> specs = schema();
        int positions = Math.min(POSITION_COUNT, vectors.length);
        for (int p = 0; p < positions; p++) {
            if (vectors[p] == null || vectors[p].length != DIMENSION) {
                errors.add("DIM_P" + p);
                continue;
            }
            for (int i = 0; i < DIMENSION; i++) {
                boolean miss =
                        missing != null
                                && missing.length > p
                                && missing[p] != null
                                && missing[p].length > i
                                && missing[p][i];
                if (miss) missingCount++;
                double value = vectors[p][i];
                ComponentSpec spec = specs.get(i);
                if (!finite(value)) errors.add("NON_FINITE_P" + p + "I" + i);
                else if (value < spec.minimum - 1.0e-9d || value > spec.maximum + 1.0e-9d)
                    errors.add("RANGE_P" + p + "I" + i + "=" + value);
            }
        }
        return new Validation(errors.isEmpty(), errors, missingCount, hash(vectors), hash(missing));
    }

    public PositionSummary[] summarize(double[][] vectors) {
        PositionSummary[] out = new PositionSummary[POSITION_COUNT];
        for (int p = 0; p < POSITION_COUNT; p++) {
            double[] x =
                    vectors != null && vectors.length > p && vectors[p] != null
                            ? vectors[p]
                            : new double[DIMENSION];
            double mean = mean(x),
                    variance = variance(x, mean),
                    l1 = l1(x),
                    l2 = l2(x),
                    minimum = min(x),
                    maximum = max(x);
            out[p] =
                    new PositionSummary(
                            p + 1,
                            mean,
                            variance,
                            l1,
                            l2,
                            minimum,
                            maximum,
                            V51ChecksumEngine.sha256(vectorText(x)));
        }
        return out;
    }

    public ReplayAudit replayAudit(V51Models.PermutationFeatureSnapshot snapshot) {
        double[][] first = build(snapshot);
        double[][] second = build(snapshot);
        String firstHash = hash(first), secondHash = hash(second);
        boolean[][] missing = missingMask(snapshot);
        Validation validation = validate(first, missing);
        return new ReplayAudit(
                firstHash.equals(secondHash) && validation.passed,
                firstHash,
                secondHash,
                validation);
    }

    public static List<ComponentSpec> schema() {
        ArrayList<ComponentSpec> list = new ArrayList<ComponentSpec>();
        list.add(new ComponentSpec(0, "kendall_tau", -1, 1, 1, "global"));
        list.add(new ComponentSpec(1, "mean_abs_displacement", 0, 1, 1, "global"));
        list.add(new ComponentSpec(2, "max_displacement", 0, 1, 1, "global"));
        list.add(new ComponentSpec(3, "fixed_point_rate", 0, 1, 1, "global"));
        list.add(new ComponentSpec(4, "cycle_rate", 0, 1, 1, "global"));
        list.add(new ComponentSpec(5, "adjacency_preservation", 0, 1, 1, "global"));
        list.add(new ComponentSpec(6, "region_exchange", 0, 1, 1, "global"));
        list.add(new ComponentSpec(7, "displacement_entropy", 0, 1, 1, "global"));
        list.add(new ComponentSpec(8, "position_entropy", 0, 1, 1, "position-aggregate"));
        list.add(new ComponentSpec(9, "max_gap", 0, 1, 1, "position-aggregate"));
        list.add(new ComponentSpec(10, "mean_abs_short_long", 0, 1, 1, "position-aggregate"));
        list.add(new ComponentSpec(11, "mean_abs_velocity", 0, 1, 1, "position-aggregate"));
        list.add(new ComponentSpec(12, "mean_abs_acceleration", 0, 2, 2, "position-aggregate"));
        list.add(new ComponentSpec(13, "mean_reversal", 0, 1, 1, "position-aggregate"));
        return Collections.unmodifiableList(list);
    }

    public static String schemaHash() {
        StringBuilder out = new StringBuilder(B04_VERSION);
        List<ComponentSpec> specs = schema();
        for (int i = 0; i < specs.size(); i++) out.append('|').append(specs.get(i).canonical());
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static double safeGlobal(double[] values, int index) {
        return values != null && values.length > index ? V51Math.finite(values[index]) : 0.0d;
    }

    private static boolean finite(double x) {
        return !Double.isNaN(x) && !Double.isInfinite(x);
    }

    private static void sanitize(double[] x) {
        for (int i = 0; i < x.length; i++) x[i] = V51Math.finite(x[i]);
    }

    private static double normalizedEntropy(double[] values) {
        double sum = 0;
        for (int i = 0; i < values.length; i++) sum += Math.max(0, values[i]);
        if (sum <= 0) return 0;
        double e = 0;
        for (int i = 0; i < values.length; i++) {
            double p = Math.max(0, values[i]) / sum;
            if (p > 0) e -= p * Math.log(p);
        }
        return V51Math.clamp(e / Math.log(values.length), 0, 1);
    }

    private static double mean(double[] x) {
        if (x == null || x.length == 0) return 0;
        double s = 0;
        for (int i = 0; i < x.length; i++) s += V51Math.finite(x[i]);
        return s / x.length;
    }

    private static double variance(double[] x, double mean) {
        if (x == null || x.length == 0) return 0;
        double s = 0;
        for (int i = 0; i < x.length; i++) {
            double d = V51Math.finite(x[i]) - mean;
            s += d * d;
        }
        return s / x.length;
    }

    private static double l1(double[] x) {
        double s = 0;
        if (x != null) for (int i = 0; i < x.length; i++) s += Math.abs(V51Math.finite(x[i]));
        return s;
    }

    private static double l2(double[] x) {
        double s = 0;
        if (x != null)
            for (int i = 0; i < x.length; i++) {
                double v = V51Math.finite(x[i]);
                s += v * v;
            }
        return Math.sqrt(s);
    }

    private static double min(double[] x) {
        if (x == null || x.length == 0) return 0;
        double m = Double.MAX_VALUE;
        for (int i = 0; i < x.length; i++) m = Math.min(m, V51Math.finite(x[i]));
        return m;
    }

    private static double max(double[] x) {
        if (x == null || x.length == 0) return 0;
        double m = -Double.MAX_VALUE;
        for (int i = 0; i < x.length; i++) m = Math.max(m, V51Math.finite(x[i]));
        return m;
    }

    private static String vectorText(double[] x) {
        StringBuilder out = new StringBuilder();
        if (x != null)
            for (int i = 0; i < x.length; i++)
                out.append(Long.toHexString(Double.doubleToLongBits(V51Math.finite(x[i]))))
                        .append(',');
        return out.toString();
    }

    public static String hash(double[][] x) {
        StringBuilder out = new StringBuilder();
        if (x != null) for (int i = 0; i < x.length; i++) out.append(vectorText(x[i])).append(';');
        return V51ChecksumEngine.sha256(out.toString());
    }

    public static String hash(boolean[][] x) {
        StringBuilder out = new StringBuilder();
        if (x != null)
            for (int i = 0; i < x.length; i++) {
                if (x[i] != null)
                    for (int j = 0; j < x[i].length; j++) out.append(x[i][j] ? '1' : '0');
                out.append(';');
            }
        return V51ChecksumEngine.sha256(out.toString());
    }

    public static final class StateVectorSnapshot {
        public final double[][] vectors;
        public final boolean[][] missing;
        public final PositionSummary[] summaries;
        public final Validation validation;
        public final String vectorHash, missingHash, checksum;
        public final long elapsedMs;

        StateVectorSnapshot(
                double[][] vectors,
                boolean[][] missing,
                PositionSummary[] summaries,
                Validation validation,
                String vectorHash,
                String missingHash,
                String checksum,
                long elapsedMs) {
            this.vectors = V51Math.copy(vectors);
            this.missing = copy(missing);
            this.summaries = summaries;
            this.validation = validation;
            this.vectorHash = safe(vectorHash);
            this.missingHash = safe(missingHash);
            this.checksum = safe(checksum);
            this.elapsedMs = Math.max(0L, elapsedMs);
        }

        public boolean valid() {
            return validation != null && validation.passed && checksum.length() == 64;
        }

        public String summary() {
            return "valid="
                    + (valid() ? 1 : 0)
                    + "|missing="
                    + (validation == null ? 0 : validation.missingCount)
                    + "|cost="
                    + elapsedMs
                    + "ms|hash="
                    + checksum;
        }
    }

    public static final class PositionSummary {
        public final int position;
        public final double mean, variance, l1, l2, minimum, maximum;
        public final String checksum;

        PositionSummary(
                int position,
                double mean,
                double variance,
                double l1,
                double l2,
                double minimum,
                double maximum,
                String checksum) {
            this.position = position;
            this.mean = mean;
            this.variance = variance;
            this.l1 = l1;
            this.l2 = l2;
            this.minimum = minimum;
            this.maximum = maximum;
            this.checksum = safe(checksum);
        }

        public String canonical() {
            return position + "|" + mean + "|" + variance + "|" + l1 + "|" + l2 + "|" + minimum
                    + "|" + maximum + "|" + checksum;
        }
    }

    public static final class Validation {
        public final boolean passed;
        public final List<String> errors;
        public final int missingCount;
        public final String vectorHash, missingHash;

        Validation(
                boolean passed,
                List<String> errors,
                int missingCount,
                String vectorHash,
                String missingHash) {
            this.passed = passed;
            this.errors =
                    errors == null
                            ? Collections.<String>emptyList()
                            : Collections.unmodifiableList(new ArrayList<String>(errors));
            this.missingCount = Math.max(0, missingCount);
            this.vectorHash = safe(vectorHash);
            this.missingHash = safe(missingHash);
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL")
                    + "|errors="
                    + errors.size()
                    + "|missing="
                    + missingCount
                    + "|vector="
                    + vectorHash
                    + "|mask="
                    + missingHash;
        }
    }

    public static final class ReplayAudit {
        public final boolean passed;
        public final String firstHash, secondHash;
        public final Validation validation;

        ReplayAudit(boolean passed, String firstHash, String secondHash, Validation validation) {
            this.passed = passed;
            this.firstHash = safe(firstHash);
            this.secondHash = safe(secondHash);
            this.validation = validation;
        }

        public String summary() {
            return (passed ? "PASS" : "FAIL")
                    + "|first="
                    + firstHash
                    + "|second="
                    + secondHash
                    + "|"
                    + (validation == null ? "validation=NULL" : validation.summary());
        }
    }

    public static final class ComponentSpec {
        public final int index;
        public final String name;
        public final double minimum, maximum, scale;
        public final String source;

        ComponentSpec(
                int index,
                String name,
                double minimum,
                double maximum,
                double scale,
                String source) {
            this.index = index;
            this.name = safe(name);
            this.minimum = minimum;
            this.maximum = maximum;
            this.scale = Math.max(1.0e-6d, scale);
            this.source = safe(source);
        }

        public String canonical() {
            return index + ":" + name + ":" + minimum + ":" + maximum + ":" + scale + ":" + source;
        }
    }

    private static boolean[][] copy(boolean[][] source) {
        if (source == null) return new boolean[0][0];
        boolean[][] out = new boolean[source.length][];
        for (int i = 0; i < source.length; i++) {
            out[i] = new boolean[source[i].length];
            System.arraycopy(source[i], 0, out[i], 0, source[i].length);
        }
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static List<StateContractRule> stateContractRules() {
        ArrayList<StateContractRule> out = new ArrayList<StateContractRule>();
        out.add(
                new StateContractRule(
                        "B04-STATE-001",
                        "P1_C00_FINITE",
                        "OBSERVABLE_VECTOR",
                        0,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-002",
                        "P1_C01_FINITE",
                        "OBSERVABLE_VECTOR",
                        1,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-003",
                        "P1_C02_FINITE",
                        "OBSERVABLE_VECTOR",
                        2,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-004",
                        "P1_C03_FINITE",
                        "OBSERVABLE_VECTOR",
                        3,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-005",
                        "P1_C04_FINITE",
                        "OBSERVABLE_VECTOR",
                        4,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-006",
                        "P1_C05_FINITE",
                        "OBSERVABLE_VECTOR",
                        5,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-007",
                        "P1_C06_FINITE",
                        "OBSERVABLE_VECTOR",
                        6,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-008",
                        "P1_C07_FINITE",
                        "OBSERVABLE_VECTOR",
                        7,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-009",
                        "P1_C08_FINITE",
                        "OBSERVABLE_VECTOR",
                        8,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-010",
                        "P1_C09_FINITE",
                        "OBSERVABLE_VECTOR",
                        9,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-011",
                        "P1_C10_FINITE",
                        "OBSERVABLE_VECTOR",
                        10,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-012",
                        "P1_C11_FINITE",
                        "OBSERVABLE_VECTOR",
                        11,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-013",
                        "P1_C12_FINITE",
                        "OBSERVABLE_VECTOR",
                        12,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-014",
                        "P1_C13_FINITE",
                        "OBSERVABLE_VECTOR",
                        13,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-015",
                        "P2_C00_FINITE",
                        "OBSERVABLE_VECTOR",
                        0,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-016",
                        "P2_C01_FINITE",
                        "OBSERVABLE_VECTOR",
                        1,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-017",
                        "P2_C02_FINITE",
                        "OBSERVABLE_VECTOR",
                        2,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-018",
                        "P2_C03_FINITE",
                        "OBSERVABLE_VECTOR",
                        3,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-019",
                        "P2_C04_FINITE",
                        "OBSERVABLE_VECTOR",
                        4,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-020",
                        "P2_C05_FINITE",
                        "OBSERVABLE_VECTOR",
                        5,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-021",
                        "P2_C06_FINITE",
                        "OBSERVABLE_VECTOR",
                        6,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-022",
                        "P2_C07_FINITE",
                        "OBSERVABLE_VECTOR",
                        7,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-023",
                        "P2_C08_FINITE",
                        "OBSERVABLE_VECTOR",
                        8,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-024",
                        "P2_C09_FINITE",
                        "OBSERVABLE_VECTOR",
                        9,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-025",
                        "P2_C10_FINITE",
                        "OBSERVABLE_VECTOR",
                        10,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-026",
                        "P2_C11_FINITE",
                        "OBSERVABLE_VECTOR",
                        11,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-027",
                        "P2_C12_FINITE",
                        "OBSERVABLE_VECTOR",
                        12,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-028",
                        "P2_C13_FINITE",
                        "OBSERVABLE_VECTOR",
                        13,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-029",
                        "P3_C00_FINITE",
                        "OBSERVABLE_VECTOR",
                        0,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-030",
                        "P3_C01_FINITE",
                        "OBSERVABLE_VECTOR",
                        1,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-031",
                        "P3_C02_FINITE",
                        "OBSERVABLE_VECTOR",
                        2,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-032",
                        "P3_C03_FINITE",
                        "OBSERVABLE_VECTOR",
                        3,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-033",
                        "P3_C04_FINITE",
                        "OBSERVABLE_VECTOR",
                        4,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-034",
                        "P3_C05_FINITE",
                        "OBSERVABLE_VECTOR",
                        5,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-035",
                        "P3_C06_FINITE",
                        "OBSERVABLE_VECTOR",
                        6,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-036",
                        "P3_C07_FINITE",
                        "OBSERVABLE_VECTOR",
                        7,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-037",
                        "P3_C08_FINITE",
                        "OBSERVABLE_VECTOR",
                        8,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-038",
                        "P3_C09_FINITE",
                        "OBSERVABLE_VECTOR",
                        9,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-039",
                        "P3_C10_FINITE",
                        "OBSERVABLE_VECTOR",
                        10,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-040",
                        "P3_C11_FINITE",
                        "OBSERVABLE_VECTOR",
                        11,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-041",
                        "P3_C12_FINITE",
                        "OBSERVABLE_VECTOR",
                        12,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-042",
                        "P3_C13_FINITE",
                        "OBSERVABLE_VECTOR",
                        13,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-043",
                        "P4_C00_FINITE",
                        "OBSERVABLE_VECTOR",
                        0,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-044",
                        "P4_C01_FINITE",
                        "OBSERVABLE_VECTOR",
                        1,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-045",
                        "P4_C02_FINITE",
                        "OBSERVABLE_VECTOR",
                        2,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-046",
                        "P4_C03_FINITE",
                        "OBSERVABLE_VECTOR",
                        3,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-047",
                        "P4_C04_FINITE",
                        "OBSERVABLE_VECTOR",
                        4,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-048",
                        "P4_C05_FINITE",
                        "OBSERVABLE_VECTOR",
                        5,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-049",
                        "P4_C06_FINITE",
                        "OBSERVABLE_VECTOR",
                        6,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-050",
                        "P4_C07_FINITE",
                        "OBSERVABLE_VECTOR",
                        7,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-051",
                        "P4_C08_FINITE",
                        "OBSERVABLE_VECTOR",
                        8,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-052",
                        "P4_C09_FINITE",
                        "OBSERVABLE_VECTOR",
                        9,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-053",
                        "P4_C10_FINITE",
                        "OBSERVABLE_VECTOR",
                        10,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-054",
                        "P4_C11_FINITE",
                        "OBSERVABLE_VECTOR",
                        11,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-055",
                        "P4_C12_FINITE",
                        "OBSERVABLE_VECTOR",
                        12,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-056",
                        "P4_C13_FINITE",
                        "OBSERVABLE_VECTOR",
                        13,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-057",
                        "P5_C00_FINITE",
                        "OBSERVABLE_VECTOR",
                        0,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-058",
                        "P5_C01_FINITE",
                        "OBSERVABLE_VECTOR",
                        1,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-059",
                        "P5_C02_FINITE",
                        "OBSERVABLE_VECTOR",
                        2,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-060",
                        "P5_C03_FINITE",
                        "OBSERVABLE_VECTOR",
                        3,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-061",
                        "P5_C04_FINITE",
                        "OBSERVABLE_VECTOR",
                        4,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-062",
                        "P5_C05_FINITE",
                        "OBSERVABLE_VECTOR",
                        5,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-063",
                        "P5_C06_FINITE",
                        "OBSERVABLE_VECTOR",
                        6,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-064",
                        "P5_C07_FINITE",
                        "OBSERVABLE_VECTOR",
                        7,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-065",
                        "P5_C08_FINITE",
                        "OBSERVABLE_VECTOR",
                        8,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-066",
                        "P5_C09_FINITE",
                        "OBSERVABLE_VECTOR",
                        9,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-067",
                        "P5_C10_FINITE",
                        "OBSERVABLE_VECTOR",
                        10,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-068",
                        "P5_C11_FINITE",
                        "OBSERVABLE_VECTOR",
                        11,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-069",
                        "P5_C12_FINITE",
                        "OBSERVABLE_VECTOR",
                        12,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-070",
                        "P5_C13_FINITE",
                        "OBSERVABLE_VECTOR",
                        13,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-071",
                        "P6_C00_FINITE",
                        "OBSERVABLE_VECTOR",
                        0,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-072",
                        "P6_C01_FINITE",
                        "OBSERVABLE_VECTOR",
                        1,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-073",
                        "P6_C02_FINITE",
                        "OBSERVABLE_VECTOR",
                        2,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-074",
                        "P6_C03_FINITE",
                        "OBSERVABLE_VECTOR",
                        3,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-075",
                        "P6_C04_FINITE",
                        "OBSERVABLE_VECTOR",
                        4,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-076",
                        "P6_C05_FINITE",
                        "OBSERVABLE_VECTOR",
                        5,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-077",
                        "P6_C06_FINITE",
                        "OBSERVABLE_VECTOR",
                        6,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-078",
                        "P6_C07_FINITE",
                        "OBSERVABLE_VECTOR",
                        7,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-079",
                        "P6_C08_FINITE",
                        "OBSERVABLE_VECTOR",
                        8,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-080",
                        "P6_C09_FINITE",
                        "OBSERVABLE_VECTOR",
                        9,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-081",
                        "P6_C10_FINITE",
                        "OBSERVABLE_VECTOR",
                        10,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-082",
                        "P6_C11_FINITE",
                        "OBSERVABLE_VECTOR",
                        11,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-083",
                        "P6_C12_FINITE",
                        "OBSERVABLE_VECTOR",
                        12,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-084",
                        "P6_C13_FINITE",
                        "OBSERVABLE_VECTOR",
                        13,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-085",
                        "P7_C00_FINITE",
                        "OBSERVABLE_VECTOR",
                        0,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-086",
                        "P7_C01_FINITE",
                        "OBSERVABLE_VECTOR",
                        1,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-087",
                        "P7_C02_FINITE",
                        "OBSERVABLE_VECTOR",
                        2,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-088",
                        "P7_C03_FINITE",
                        "OBSERVABLE_VECTOR",
                        3,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-089",
                        "P7_C04_FINITE",
                        "OBSERVABLE_VECTOR",
                        4,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-090",
                        "P7_C05_FINITE",
                        "OBSERVABLE_VECTOR",
                        5,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-091",
                        "P7_C06_FINITE",
                        "OBSERVABLE_VECTOR",
                        6,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-092",
                        "P7_C07_FINITE",
                        "OBSERVABLE_VECTOR",
                        7,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-093",
                        "P7_C08_FINITE",
                        "OBSERVABLE_VECTOR",
                        8,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-094",
                        "P7_C09_FINITE",
                        "OBSERVABLE_VECTOR",
                        9,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-095",
                        "P7_C10_FINITE",
                        "OBSERVABLE_VECTOR",
                        10,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-096",
                        "P7_C11_FINITE",
                        "OBSERVABLE_VECTOR",
                        11,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-097",
                        "P7_C12_FINITE",
                        "OBSERVABLE_VECTOR",
                        12,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-098",
                        "P7_C13_FINITE",
                        "OBSERVABLE_VECTOR",
                        13,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-099",
                        "P8_C00_FINITE",
                        "OBSERVABLE_VECTOR",
                        0,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-100",
                        "P8_C01_FINITE",
                        "OBSERVABLE_VECTOR",
                        1,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-101",
                        "P8_C02_FINITE",
                        "OBSERVABLE_VECTOR",
                        2,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-102",
                        "P8_C03_FINITE",
                        "OBSERVABLE_VECTOR",
                        3,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-103",
                        "P8_C04_FINITE",
                        "OBSERVABLE_VECTOR",
                        4,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-104",
                        "P8_C05_FINITE",
                        "OBSERVABLE_VECTOR",
                        5,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-105",
                        "P8_C06_FINITE",
                        "OBSERVABLE_VECTOR",
                        6,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-106",
                        "P8_C07_FINITE",
                        "OBSERVABLE_VECTOR",
                        7,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-107",
                        "P8_C08_FINITE",
                        "OBSERVABLE_VECTOR",
                        8,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-108",
                        "P8_C09_FINITE",
                        "OBSERVABLE_VECTOR",
                        9,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-109",
                        "P8_C10_FINITE",
                        "OBSERVABLE_VECTOR",
                        10,
                        true,
                        "V51ObservableStateVectorEngine"));
        out.add(
                new StateContractRule(
                        "B04-STATE-110",
                        "P8_C11_FINITE",
                        "OBSERVABLE_VECTOR",
                        11,
                        true,
                        "V51ObservableStateVectorEngine"));
        return Collections.unmodifiableList(out);
    }

    public StateContractReport verifyStateContract(V51Models.PermutationFeatureSnapshot snapshot) {
        ArrayList<String> failures = new ArrayList<String>();
        double[][] vectors = build(snapshot);
        boolean[][] missing = missingMask(snapshot);
        Validation validation = validate(vectors, missing);
        if (!validation.passed) failures.addAll(validation.errors);
        ReplayAudit replay = replayAudit(snapshot);
        if (!replay.passed) failures.add("REPLAY_MISMATCH");
        if (schema().size() != DIMENSION) failures.add("SCHEMA_DIMENSION_MISMATCH");
        if (stateContractRules().size() < 100) failures.add("STATE_RULE_CATALOG_INCOMPLETE");
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION
                                + "|STATE_CONTRACT|"
                                + validation.summary()
                                + "|"
                                + replay.summary()
                                + "|"
                                + failures.toString());
        return new StateContractReport(
                failures.isEmpty(), stateContractRules().size(), failures, checksum);
    }

    public StateDelta compare(
            V51Models.PermutationFeatureSnapshot current,
            V51Models.PermutationFeatureSnapshot previous) {
        double[][] now = build(current);
        double[][] old = previous == null ? new double[POSITION_COUNT][DIMENSION] : build(previous);
        double[][] delta = new double[POSITION_COUNT][DIMENSION];
        double[] distance = new double[POSITION_COUNT];
        for (int p = 0; p < POSITION_COUNT; p++) {
            for (int i = 0; i < DIMENSION; i++) delta[p][i] = V51Math.finite(now[p][i] - old[p][i]);
            distance[p] = distance(now[p], old[p]);
        }
        String checksum =
                V51ChecksumEngine.sha256(
                        B04_VERSION + "|DELTA|" + hash(now) + "|" + hash(old) + "|" + hash(delta));
        return new StateDelta(delta, distance, checksum);
    }

    public static final class StateContractRule {
        public final String id, name, group, owner;
        public final int component;
        public final boolean required;

        StateContractRule(
                String id,
                String name,
                String group,
                int component,
                boolean required,
                String owner) {
            this.id = safe(id);
            this.name = safe(name);
            this.group = safe(group);
            this.component = component;
            this.required = required;
            this.owner = safe(owner);
        }

        public String canonical() {
            return id
                    + ":"
                    + name
                    + ":"
                    + group
                    + ":"
                    + component
                    + ":"
                    + (required ? 1 : 0)
                    + ":"
                    + owner;
        }
    }

    public static final class StateContractReport {
        public final boolean passed;
        public final int ruleCount;
        public final List<String> failures;
        public final String checksum;

        StateContractReport(boolean passed, int ruleCount, List<String> failures, String checksum) {
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

    public static final class StateDelta {
        public final double[][] delta;
        public final double[] distance;
        public final String checksum;

        StateDelta(double[][] delta, double[] distance, String checksum) {
            this.delta = V51Math.copy(delta);
            this.distance = V51Math.copy(distance);
            this.checksum = safe(checksum);
        }

        public double maximumDistance() {
            double m = 0;
            for (int i = 0; i < distance.length; i++) m = Math.max(m, distance[i]);
            return m;
        }

        public String summary() {
            return "maxDistance=" + maximumDistance() + "|hash=" + checksum;
        }
    }
}
