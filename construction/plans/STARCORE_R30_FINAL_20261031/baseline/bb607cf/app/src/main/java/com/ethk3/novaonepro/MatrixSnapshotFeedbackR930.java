package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Common point-in-time matrix snapshot and delayed teacher/feedback contract.
 *
 * <p>Every matrix used to score one target issue is frozen from one common graph/input/bundle
 * identity. Settlement labels are joined later and can update only issues strictly after the
 * settled target. This prevents the historical failure mode where teacher, reverse, causal and
 * feedback matrices were captured from different mutable states.</p>
 */
public final class MatrixSnapshotFeedbackR930 {
    public static final String VERSION = "R930_COMMON_MATRIX_FEEDBACK_V1";
    public static final int POSITIONS = 10;
    public static final int NUMBERS = 10;

    private MatrixSnapshotFeedbackR930() {}

    public enum MatrixKind {
        EVIDENCE_TEACHER,
        STUDENT_ERROR,
        FORWARD_REASONING,
        REVERSE_REASONING,
        COUNTERFACTUAL,
        FEEDBACK,
        CONTRADICTION,
        TEACHER_DIVERSITY,
        TEMPORAL_PRECEDENCE,
        INVARIANCE,
        CAUSAL_SUPPORT,
        COUNTER_EVIDENCE,
        RESIDUAL_MECHANISM,
        NUMBER_POSITION_COMPATIBILITY,
        CROSS_POSITION_FLOW,
        ACTION_UTILITY,
        ALGORITHM_COMPLEMENTARITY,
        EVOLUTION_CREDIT,
        RESOURCE_EFFICIENCY
    }

    public enum CaptureStage {
        S1_ATOM_INPUT,
        S2_FACTOR_MOLECULE,
        S3_MATRIX_SURFACE,
        S4_SELECTION_ENVELOPE
    }

    public static final class MatrixCell {
        public final int row;
        public final int column;
        public final boolean present;
        public final double value;
        public final int support;
        public final double quality;
        public final long latencyNs;
        public final boolean pitPass;
        public final String sourceNodeHash;
        public final String cellHash;

        public MatrixCell(int row, int column, boolean present, double value, int support,
                double quality, long latencyNs, boolean pitPass, String sourceNodeHash) {
            if (row < 0 || row >= POSITIONS || column < 0 || column >= NUMBERS
                    || !finite(value) || support < 0 || !unit(quality) || latencyNs < 0L)
                throw new IllegalArgumentException("R930_MATRIX_CELL_INVALID");
            this.row = row;
            this.column = column;
            this.present = present;
            this.value = value;
            this.support = support;
            this.quality = quality;
            this.latencyNs = latencyNs;
            this.pitPass = pitPass;
            this.sourceNodeHash = requireHash(sourceNodeHash, "MATRIX_CELL_SOURCE_HASH");
            this.cellHash = sha256(row + "|" + column + "|" + (present ? 1 : 0) + "|"
                    + Long.toHexString(Double.doubleToLongBits(value)) + "|" + support + "|"
                    + Long.toHexString(Double.doubleToLongBits(quality)) + "|R958_CELL2"
                    + "|" + (pitPass ? 1 : 0) + "|" + this.sourceNodeHash);
        }
    }

    public static final class FrozenMatrix {
        public final MatrixKind kind;
        public final String targetIssue;
        public final String maxSourceIssue;
        public final long frozenAtMs;
        public final String inputStage;
        public final String graphHash;
        public final String inputHash;
        public final String modelHash;
        private final MatrixCell[][] cells;
        public final String matrixHash;

        public FrozenMatrix(MatrixKind kind, String targetIssue, String maxSourceIssue,
                long frozenAtMs, String inputStage, String graphHash, String inputHash,
                String modelHash, List<MatrixCell> cells) {
            if (kind == null || frozenAtMs <= 0L || cells == null
                    || cells.size() != POSITIONS * NUMBERS)
                throw new IllegalArgumentException("R930_FROZEN_MATRIX_HEADER");
            this.kind = kind;
            this.targetIssue = required(targetIssue, "MATRIX_TARGET");
            this.maxSourceIssue = required(maxSourceIssue, "MATRIX_MAX_SOURCE");
            if (!PitFeatureDagR929.isStrictPast(this.maxSourceIssue, this.targetIssue))
                throw new IllegalArgumentException("R930_MATRIX_NOT_PIT");
            this.frozenAtMs = frozenAtMs;
            this.inputStage = required(inputStage, "MATRIX_INPUT_STAGE");
            this.graphHash = requireHash(graphHash, "MATRIX_GRAPH_HASH");
            this.inputHash = requireHash(inputHash, "MATRIX_INPUT_HASH");
            this.modelHash = requireHash(modelHash, "MATRIX_MODEL_HASH");
            this.cells = new MatrixCell[POSITIONS][NUMBERS];
            StringBuilder canonical = new StringBuilder(10000);
            canonical.append(kind.name()).append('|').append(this.targetIssue).append('|')
                    .append(this.maxSourceIssue).append('|').append(frozenAtMs).append('|')
                    .append(this.inputStage).append('|').append(this.graphHash).append('|')
                    .append(this.inputHash).append('|').append(this.modelHash);
            for (MatrixCell cell : cells) {
                if (cell == null || this.cells[cell.row][cell.column] != null)
                    throw new IllegalArgumentException("R930_MATRIX_CELL_DUPLICATE");
                if (!cell.pitPass)
                    throw new IllegalArgumentException("R930_MATRIX_CELL_PIT_FAIL");
                this.cells[cell.row][cell.column] = cell;
            }
            for (int row = 0; row < POSITIONS; row++) {
                for (int column = 0; column < NUMBERS; column++) {
                    MatrixCell cell = this.cells[row][column];
                    if (cell == null) throw new IllegalArgumentException("R930_MATRIX_CELL_MISSING");
                    canonical.append('|').append(cell.cellHash);
                }
            }
            this.matrixHash = sha256(canonical.toString());
        }

        public MatrixCell cell(int row, int column) { return cells[row][column]; }

        public double[][] valuesWithMissingAsZero() {
            double[][] output = new double[POSITIONS][NUMBERS];
            for (int row = 0; row < POSITIONS; row++)
                for (int column = 0; column < NUMBERS; column++)
                    output[row][column] = cells[row][column].present ? cells[row][column].value : 0.0d;
            return output;
        }

        public int presentCells() {
            int n = 0;
            for (int row = 0; row < POSITIONS; row++)
                for (int column = 0; column < NUMBERS; column++)
                    if (cells[row][column].present) n++;
            return n;
        }
    }

    public static final class CaptureRecord {
        public final CaptureStage stage;
        public final String targetIssue;
        public final String maxSourceIssue;
        public final long frozenAtMs;
        public final String freezeHash;
        public final String inputHash;
        public final int expectedRows;
        public final int capturedRows;
        public final int missingRows;
        public final long costNs;
        public final String payloadHash;
        public final String captureHash;

        public CaptureRecord(CaptureStage stage, String targetIssue, String maxSourceIssue,
                long frozenAtMs, String freezeHash, String inputHash, int expectedRows,
                int capturedRows, int missingRows, long costNs, String payloadHash) {
            if (stage == null || frozenAtMs <= 0L || expectedRows <= 0 || capturedRows < 0
                    || capturedRows > expectedRows || missingRows < 0 || missingRows > expectedRows
                    || capturedRows + missingRows != expectedRows || costNs < 0L)
                throw new IllegalArgumentException("R930_CAPTURE_RECORD_INVALID");
            this.stage = stage;
            this.targetIssue = required(targetIssue, "CAPTURE_TARGET");
            this.maxSourceIssue = required(maxSourceIssue, "CAPTURE_MAX_SOURCE");
            if (!PitFeatureDagR929.isStrictPast(this.maxSourceIssue, this.targetIssue))
                throw new IllegalArgumentException("R930_CAPTURE_NOT_PIT");
            this.frozenAtMs = frozenAtMs;
            this.freezeHash = requireHash(freezeHash, "CAPTURE_FREEZE_HASH");
            this.inputHash = requireHash(inputHash, "CAPTURE_INPUT_HASH");
            this.expectedRows = expectedRows;
            this.capturedRows = capturedRows;
            this.missingRows = missingRows;
            this.costNs = costNs;
            this.payloadHash = requireHash(payloadHash, "CAPTURE_PAYLOAD_HASH");
            this.captureHash = sha256(stage.name() + '|' + this.targetIssue + '|'
                    + this.maxSourceIssue + '|' + frozenAtMs + '|' + this.freezeHash + '|'
                    + this.inputHash + '|' + expectedRows + '|' + capturedRows + '|'
                    + missingRows + "|R958_CAPTURE2|" + this.payloadHash);
        }

        public boolean complete() { return capturedRows == expectedRows && missingRows == 0; }
        public double coverage() { return capturedRows / (double) expectedRows; }
    }

    public static final class CaptureBundle {
        public final String targetIssue;
        public final String maxSourceIssue;
        public final long frozenAtMs;
        public final String freezeHash;
        public final String inputHash;
        public final Map<CaptureStage, CaptureRecord> stages;
        public final boolean complete;
        public final double minimumStageCoverage;
        public final String bundleHash;

        public CaptureBundle(List<CaptureRecord> records) {
            if (records == null || records.size() != CaptureStage.values().length)
                throw new IllegalArgumentException("R930_CAPTURE_STAGE_CARDINALITY");
            EnumMap<CaptureStage, CaptureRecord> map = new EnumMap<CaptureStage, CaptureRecord>(
                    CaptureStage.class);
            CaptureRecord first = records.get(0);
            if(first==null)throw new IllegalArgumentException("R930_CAPTURE_RECORD_NULL");
            this.targetIssue = first.targetIssue;
            this.maxSourceIssue = first.maxSourceIssue;
            this.frozenAtMs = first.frozenAtMs;
            this.freezeHash = first.freezeHash;
            this.inputHash = first.inputHash;
            boolean all = true;
            double minimum = 1.0d;
            StringBuilder canonical = new StringBuilder(this.freezeHash).append('|')
                    .append(this.inputHash);
            for (CaptureRecord record : records) {
                if (record == null || map.put(record.stage, record) != null)
                    throw new IllegalArgumentException("R930_CAPTURE_STAGE_DUPLICATE");
                if (!targetIssue.equals(record.targetIssue)
                        || !maxSourceIssue.equals(record.maxSourceIssue)
                        || frozenAtMs != record.frozenAtMs
                        || !freezeHash.equals(record.freezeHash)
                        || !inputHash.equals(record.inputHash))
                    throw new IllegalArgumentException("R930_CAPTURE_FREEZE_DRIFT");
                all &= record.complete();
                minimum = Math.min(minimum, record.coverage());
            }
            for (CaptureStage stage : CaptureStage.values()) if (!map.containsKey(stage))
                throw new IllegalArgumentException("R930_CAPTURE_STAGE_MISSING:" + stage.name());
            for(CaptureStage stage:CaptureStage.values())canonical.append('|').append(stage.name()).append(':').append(map.get(stage).captureHash);
            this.stages = Collections.unmodifiableMap(map);
            this.complete = all;
            this.minimumStageCoverage = minimum;
            this.bundleHash = sha256(canonical.toString());
        }
    }

    public static final class CommonSnapshot {
        public final String targetIssue;
        public final String maxSourceIssue;
        public final long frozenAtMs;
        public final String graphHash;
        public final String inputHash;
        public final String bundleHash;
        public final CaptureBundle capture;
        public final Map<MatrixKind, FrozenMatrix> matrices;
        public final String snapshotHash;

        public CommonSnapshot(String targetIssue, String maxSourceIssue, long frozenAtMs,
                String graphHash, String inputHash, String bundleHash, CaptureBundle capture,
                List<FrozenMatrix> matrices, Set<MatrixKind> requiredKinds) {
            this.targetIssue = required(targetIssue, "SNAPSHOT_TARGET");
            this.maxSourceIssue = required(maxSourceIssue, "SNAPSHOT_MAX_SOURCE");
            if (!PitFeatureDagR929.isStrictPast(this.maxSourceIssue, this.targetIssue)
                    || frozenAtMs <= 0L || capture == null || !capture.complete)
                throw new IllegalArgumentException("R930_COMMON_SNAPSHOT_HEADER");
            this.frozenAtMs = frozenAtMs;
            this.capture = capture;
            this.graphHash = requireHash(graphHash, "SNAPSHOT_GRAPH_HASH");
            this.inputHash = requireHash(inputHash, "SNAPSHOT_INPUT_HASH");
            this.bundleHash = requireHash(bundleHash, "SNAPSHOT_BUNDLE_HASH");
            if (!this.targetIssue.equals(capture.targetIssue)
                    || !this.maxSourceIssue.equals(capture.maxSourceIssue)
                    || this.frozenAtMs != capture.frozenAtMs
                    || !this.inputHash.equals(capture.inputHash))
                throw new IllegalArgumentException("R930_CAPTURE_SNAPSHOT_MISMATCH");
            if (matrices == null || matrices.isEmpty())
                throw new IllegalArgumentException("R930_COMMON_SNAPSHOT_NO_MATRICES");
            EnumMap<MatrixKind, FrozenMatrix> map = new EnumMap<MatrixKind, FrozenMatrix>(
                    MatrixKind.class);
            for (FrozenMatrix matrix : matrices) {
                if (matrix == null || map.put(matrix.kind, matrix) != null)
                    throw new IllegalArgumentException("R930_COMMON_MATRIX_DUPLICATE");
                if (!this.targetIssue.equals(matrix.targetIssue)
                        || !this.maxSourceIssue.equals(matrix.maxSourceIssue)
                        || this.frozenAtMs != matrix.frozenAtMs
                        || !this.graphHash.equals(matrix.graphHash)
                        || !this.inputHash.equals(matrix.inputHash)
                        || !this.bundleHash.equals(matrix.modelHash))
                    throw new IllegalArgumentException("R930_COMMON_MATRIX_FREEZE_DRIFT");
            }
            if (requiredKinds != null) {
                for (MatrixKind required : requiredKinds)
                    if (!map.containsKey(required))
                        throw new IllegalArgumentException("R930_REQUIRED_MATRIX_MISSING:"
                                + required.name());
            }
            this.matrices = Collections.unmodifiableMap(map);
            StringBuilder canonical = new StringBuilder(VERSION).append('|')
                    .append(this.targetIssue).append('|').append(this.maxSourceIssue).append('|')
                    .append(this.frozenAtMs).append('|').append(this.graphHash).append('|')
                    .append(this.inputHash).append('|').append(this.bundleHash).append('|')
                    .append(capture.bundleHash);
            ArrayList<MatrixKind> kinds = new ArrayList<MatrixKind>(map.keySet());
            Collections.sort(kinds, new Comparator<MatrixKind>() {
                @Override public int compare(MatrixKind left, MatrixKind right) {
                    return left.name().compareTo(right.name());
                }
            });
            for (MatrixKind kind : kinds)
                canonical.append('|').append(kind.name()).append(':')
                        .append(map.get(kind).matrixHash);
            this.snapshotHash = sha256(canonical.toString());
        }

        public FrozenMatrix require(MatrixKind kind) {
            FrozenMatrix matrix = matrices.get(kind);
            if (matrix == null) throw new IllegalArgumentException("R930_MATRIX_NOT_IN_SNAPSHOT");
            return matrix;
        }
    }

    public static final class SettlementLabel {
        public final String targetIssue;
        private final int[] actual;
        public final long drawAtMs;
        public final long availableAtMs;
        public final String truthSource;
        public final String labelVersion;
        public final String labelHash;

        public SettlementLabel(String targetIssue, int[] actual, long drawAtMs,
                long availableAtMs, String truthSource, String labelVersion) {
            this.targetIssue = required(targetIssue, "LABEL_TARGET");
            if (actual == null || actual.length != POSITIONS || drawAtMs <= 0L
                    || availableAtMs < drawAtMs)
                throw new IllegalArgumentException("R930_LABEL_HEADER");
            boolean[] seen = new boolean[NUMBERS + 1];
            this.actual = Arrays.copyOf(actual, actual.length);
            for (int value : this.actual) {
                if (value < 1 || value > NUMBERS || seen[value])
                    throw new IllegalArgumentException("R930_LABEL_NOT_PERMUTATION");
                seen[value] = true;
            }
            this.drawAtMs = drawAtMs;
            this.availableAtMs = availableAtMs;
            this.truthSource = required(truthSource, "LABEL_TRUTH_SOURCE");
            this.labelVersion = required(labelVersion, "LABEL_VERSION");
            this.labelHash = sha256(this.targetIssue + '|' + Arrays.toString(this.actual) + '|'
                    + drawAtMs + '|' + availableAtMs + '|' + this.truthSource + '|'
                    + this.labelVersion);
        }

        public int numberAt(int position) { return actual[position] - 1; }
        public int[] actualCopy() { return Arrays.copyOf(actual, actual.length); }
    }

    public static final class FeedbackDelta {
        public final String settledIssue;
        public final String effectiveFromIssue;
        public final String snapshotHash;
        public final String labelHash;
        private final double[][] teacherResidual;
        private final double[][] studentError;
        public final double brier;
        public final double crossEntropy;
        public final String deltaHash;

        FeedbackDelta(String settledIssue, String effectiveFromIssue, String snapshotHash,
                String labelHash, double[][] teacherResidual, double[][] studentError,
                double brier, double crossEntropy) {
            this.settledIssue = settledIssue;
            this.effectiveFromIssue = effectiveFromIssue;
            this.snapshotHash = snapshotHash;
            this.labelHash = labelHash;
            this.teacherResidual = deepCopy(teacherResidual);
            this.studentError = deepCopy(studentError);
            this.brier = brier;
            this.crossEntropy = crossEntropy;
            StringBuilder canonical = new StringBuilder(settledIssue).append('|')
                    .append(effectiveFromIssue).append('|').append(snapshotHash).append('|')
                    .append(labelHash).append('|').append(brier).append('|').append(crossEntropy);
            for (int row = 0; row < POSITIONS; row++)
                for (int column = 0; column < NUMBERS; column++)
                    canonical.append('|').append(teacherResidual[row][column]).append(':')
                            .append(studentError[row][column]);
            this.deltaHash = sha256(canonical.toString());
        }

        public double teacherResidual(int row, int column) { return teacherResidual[row][column]; }
        public double studentError(int row, int column) { return studentError[row][column]; }
    }

    /** Computes post-draw residuals but explicitly delays them to a later issue. */
    public static FeedbackDelta settle(CommonSnapshot snapshot, SettlementLabel label,
            String effectiveFromIssue) {
        if (snapshot == null || label == null
                || !snapshot.targetIssue.equals(label.targetIssue)
                || label.drawAtMs <= snapshot.frozenAtMs)
            throw new IllegalArgumentException("R930_FEEDBACK_SETTLEMENT_MISMATCH");
        String next = required(effectiveFromIssue, "FEEDBACK_EFFECTIVE_ISSUE");
        if (!PitFeatureDagR929.isStrictPast(snapshot.targetIssue, next))
            throw new IllegalArgumentException("R930_FEEDBACK_NOT_DELAYED");
        FrozenMatrix teacher = snapshot.require(MatrixKind.EVIDENCE_TEACHER);
        FrozenMatrix student;
        if (snapshot.matrices.containsKey(MatrixKind.FORWARD_REASONING))
            student = snapshot.require(MatrixKind.FORWARD_REASONING);
        else student = teacher;
        for(int row=0;row<POSITIONS;row++){
            double teacherSum=0,studentSum=0;
            for(int column=0;column<NUMBERS;column++){
                MatrixCell t=teacher.cell(row,column),s=student.cell(row,column);
                if(!t.present||!s.present||!unit(t.value)||!unit(s.value))throw new IllegalArgumentException("R958_FEEDBACK_PROBABILITY_MISSING_OR_INVALID");
                teacherSum+=t.value;studentSum+=s.value;
            }
            if(Math.abs(teacherSum-1d)>1e-6||Math.abs(studentSum-1d)>1e-6)throw new IllegalArgumentException("R958_FEEDBACK_PROBABILITY_NORMALIZATION");
        }
        double[][] residual = new double[POSITIONS][NUMBERS];
        double[][] error = new double[POSITIONS][NUMBERS];
        double brier = 0.0d, crossEntropy = 0.0d;
        for (int row = 0; row < POSITIONS; row++) {
            int actual = label.numberAt(row);
            for (int column = 0; column < NUMBERS; column++) {
                double truth = column == actual ? 1.0d : 0.0d;
                double teacherValue = teacher.cell(row, column).present
                        ? teacher.cell(row, column).value : 0.0d;
                double studentValue = student.cell(row, column).present
                        ? student.cell(row, column).value : 0.0d;
                residual[row][column] = truth - teacherValue;
                error[row][column] = truth - studentValue;
                double difference = studentValue - truth;
                brier += difference * difference;
            }
            double actualP = clamp(student.cell(row, actual).value, 1.0e-15d, 1.0d);
            crossEntropy += -Math.log(actualP);
        }
        brier /= POSITIONS * NUMBERS;
        crossEntropy /= POSITIONS;
        return new FeedbackDelta(snapshot.targetIssue, next, snapshot.snapshotHash,
                label.labelHash, residual, error, brier, crossEntropy);
    }

    public static final class FeedbackState {
        public final String stateId;
        public final String trainedUntilIssue;
        public final int updates;
        private final double[][] fast;
        private final double[][] slow;
        public final String stateHash;

        private FeedbackState(String stateId, String trainedUntilIssue, int updates,
                double[][] fast, double[][] slow) {
            this.stateId = required(stateId, "FEEDBACK_STATE_ID");
            this.trainedUntilIssue = clean(trainedUntilIssue);
            this.updates = updates;
            this.fast = deepCopy(fast);
            this.slow = deepCopy(slow);
            StringBuilder canonical = new StringBuilder(this.stateId).append('|')
                    .append(this.trainedUntilIssue).append('|').append(updates);
            for (int row = 0; row < POSITIONS; row++)
                for (int column = 0; column < NUMBERS; column++)
                    canonical.append('|').append(fast[row][column]).append(':')
                            .append(slow[row][column]);
            this.stateHash = sha256(canonical.toString());
        }

        public static FeedbackState initial(String stateId) {
            return new FeedbackState(stateId, "", 0,
                    new double[POSITIONS][NUMBERS], new double[POSITIONS][NUMBERS]);
        }

        public FeedbackState update(FeedbackDelta delta, double fastRate, double slowRate) {
            if (delta == null || !unit(fastRate) || !unit(slowRate) || fastRate < slowRate)
                throw new IllegalArgumentException("R930_FEEDBACK_UPDATE_INPUT");
            if (trainedUntilIssue.length() > 0
                    && PitFeatureDagR929.compareIssues(delta.settledIssue, trainedUntilIssue) <= 0)
                throw new IllegalArgumentException("R930_FEEDBACK_OUT_OF_ORDER");
            double[][] nextFast = deepCopy(fast);
            double[][] nextSlow = deepCopy(slow);
            for (int row = 0; row < POSITIONS; row++) {
                for (int column = 0; column < NUMBERS; column++) {
                    double value = delta.studentError(row, column);
                    nextFast[row][column] = (1.0d - fastRate) * nextFast[row][column]
                            + fastRate * value;
                    nextSlow[row][column] = (1.0d - slowRate) * nextSlow[row][column]
                            + slowRate * value;
                }
            }
            return new FeedbackState(stateId, delta.settledIssue, updates + 1,
                    nextFast, nextSlow);
        }

        public double correction(int row, int column, String targetIssue) {
            if (trainedUntilIssue.length() > 0
                    && !PitFeatureDagR929.isStrictPast(trainedUntilIssue,
                            required(targetIssue, "FEEDBACK_TARGET")))
                throw new IllegalArgumentException("R930_FEEDBACK_CURRENT_LABEL_LEAK");
            return 0.70d * fast[row][column] + 0.30d * slow[row][column];
        }
    }

    public static Set<MatrixKind> defaultRequiredKinds() {
        HashSet<MatrixKind> kinds = new HashSet<MatrixKind>();
        kinds.add(MatrixKind.EVIDENCE_TEACHER);
        kinds.add(MatrixKind.STUDENT_ERROR);
        kinds.add(MatrixKind.FORWARD_REASONING);
        kinds.add(MatrixKind.REVERSE_REASONING);
        kinds.add(MatrixKind.COUNTERFACTUAL);
        kinds.add(MatrixKind.FEEDBACK);
        kinds.add(MatrixKind.CAUSAL_SUPPORT);
        kinds.add(MatrixKind.CONTRADICTION);
        return Collections.unmodifiableSet(kinds);
    }

    private static double[][] deepCopy(double[][] values) {
        double[][] copy = new double[values.length][];
        for (int i = 0; i < values.length; i++) copy[i] = Arrays.copyOf(values[i], values[i].length);
        return copy;
    }
    private static boolean unit(double value) { return finite(value) && value >= 0.0d && value <= 1.0d; }
    private static boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }
    private static double clamp(double value, double low, double high) {
        if (!finite(value)) return low;
        return Math.max(low, Math.min(high, value));
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String required(String value, String code) {
        String clean = clean(value);
        if (clean.length() == 0) throw new IllegalArgumentException("R930_" + code + "_REQUIRED");
        return clean;
    }
    private static String requireHash(String value, String code) {
        String clean = required(value, code).toLowerCase(java.util.Locale.US);
        if (clean.length() != 64) throw new IllegalArgumentException("R930_" + code + "_NOT_SHA256");
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                throw new IllegalArgumentException("R930_" + code + "_NOT_SHA256");
        }
        return clean;
    }
    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(64);
            for (byte item : bytes)
                output.append(String.format(java.util.Locale.US, "%02x", item & 255));
            return output.toString();
        } catch (Exception failure) {
            throw new IllegalStateException("R930_SHA256_UNAVAILABLE", failure);
        }
    }
}
