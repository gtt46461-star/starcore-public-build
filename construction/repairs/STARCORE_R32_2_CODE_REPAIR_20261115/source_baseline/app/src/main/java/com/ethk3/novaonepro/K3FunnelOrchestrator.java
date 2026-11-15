package com.ethk3.novaonepro;

/**
 * B10 deterministic 9→8→7→6→5→4→3 research funnel.
 *
 * <p>The funnel records an evidence path over the ten-number probability row. It never removes a
 * row from the formal 1200-action universe, never changes the ranker input, and never owns a BET.
 */
public final class K3FunnelOrchestrator {
    public static final String V57_B10_OWNER = "FUNNEL_EVIDENCE_ONLY";
    public static final String FORMAL_ACTION_DELETION = "FORBIDDEN";
    public static final int POSITIONS = 10;
    public static final int NUMBERS = 10;
    public static final int[] STAGE_SIZES = new int[] {9, 8, 7, 6, 5, 4, 3};

    public static final class StageEvidence {
        public final int position;
        public final int stageIndex;
        public final int retainedCount;
        public final int[] retained;
        public final int removedNumber;
        public final double weakestRetainedP;
        public final double strongestRemovedP;
        public final double boundaryMargin;
        public final String reason;
        public final String checksum;

        StageEvidence(
                int position,
                int stageIndex,
                int[] retained,
                int removedNumber,
                double weakestRetainedP,
                double strongestRemovedP) {
            this.position = position;
            this.stageIndex = stageIndex;
            this.retained = copy(retained);
            this.retainedCount = this.retained.length;
            this.removedNumber = removedNumber;
            this.weakestRetainedP = finite(weakestRetainedP);
            this.strongestRemovedP = finite(strongestRemovedP);
            this.boundaryMargin = finite(this.weakestRetainedP - this.strongestRemovedP);
            this.reason =
                    "PRE_DRAW_Q_DESC_STABLE_TIE_NUMBER_ASC|stage="
                            + retainedCount
                            + "|removed="
                            + removedNumber;
            this.checksum =
                    V51ChecksumEngine.sha256(
                            "B10_FUNNEL_STAGE|"
                                    + position
                                    + '|'
                                    + stageIndex
                                    + '|'
                                    + csv(this.retained)
                                    + '|'
                                    + removedNumber
                                    + '|'
                                    + this.weakestRetainedP
                                    + '|'
                                    + this.strongestRemovedP
                                    + '|'
                                    + this.boundaryMargin
                                    + '|'
                                    + reason);
        }

        public boolean contains(int number) {
            for (int i = 0; i < retained.length; i++) if (retained[i] == number) return true;
            return false;
        }

        public String traceFragment() {
            return retainedCount
                    + ":"
                    + csv(retained)
                    + "@removed="
                    + removedNumber
                    + "@margin="
                    + boundaryMargin;
        }
    }

    public static final class PositionPath {
        public final int position;
        public final int[] sourceOrder;
        public final StageEvidence[] stages;
        public final int[] finalThree;
        public final boolean nested;
        public final String trace;
        public final String checksum;

        PositionPath(int position, int[] sourceOrder, StageEvidence[] stages) {
            this.position = position;
            this.sourceOrder = copy(sourceOrder);
            this.stages = copy(stages);
            this.finalThree =
                    this.stages.length == 0
                            ? new int[0]
                            : copy(this.stages[this.stages.length - 1].retained);
            this.nested = validateNested(this.sourceOrder, this.stages);
            StringBuilder out = new StringBuilder(1024);
            for (int i = 0; i < this.stages.length; i++) {
                if (i > 0) out.append('|');
                out.append(this.stages[i].traceFragment());
            }
            this.trace = out.toString();
            StringBuilder hashes = new StringBuilder();
            for (int i = 0; i < this.stages.length; i++)
                hashes.append('|').append(this.stages[i].checksum);
            this.checksum =
                    V51ChecksumEngine.sha256(
                            "B10_FUNNEL_PATH|"
                                    + position
                                    + '|'
                                    + csv(this.sourceOrder)
                                    + '|'
                                    + trace
                                    + '|'
                                    + nested
                                    + hashes.toString());
        }

        public String finalThreeCsv() {
            return csv(finalThree);
        }
    }

    /**
     * Immutable all-position evidence artifact. Existing retained field is preserved for callers.
     */
    public static final class Audit {
        public final int[][][] retained;
        public final PositionPath[] paths;
        public final int formalActionCount;
        public final boolean neverDeleteFormalAction;
        public final boolean allPathsNested;
        public final String matrixChecksum;
        public final String checksum;

        Audit(
                int[][][] retained,
                PositionPath[] paths,
                int formalActionCount,
                String matrixChecksum) {
            this.retained = copy(retained);
            this.paths = copy(paths);
            this.formalActionCount = formalActionCount;
            this.neverDeleteFormalAction = formalActionCount == K3ActionSpaceEngine.TOTAL_ACTIONS;
            this.matrixChecksum = safe(matrixChecksum);
            boolean nested = this.paths.length == POSITIONS;
            StringBuilder body = new StringBuilder(16384);
            for (int p = 0; p < this.paths.length; p++) {
                nested &= this.paths[p] != null && this.paths[p].nested;
                body.append('|').append(this.paths[p] == null ? "NULL" : this.paths[p].checksum);
            }
            this.allPathsNested = nested;
            this.checksum =
                    V51ChecksumEngine.sha256(
                            "B10_FUNNEL_AUDIT|"
                                    + formalActionCount
                                    + '|'
                                    + neverDeleteFormalAction
                                    + '|'
                                    + allPathsNested
                                    + '|'
                                    + this.matrixChecksum
                                    + body.toString());
        }

        public PositionPath pathForPosition(int position) {
            if (position < 1 || position > POSITIONS)
                throw new IllegalArgumentException("B10_FUNNEL_POSITION_" + position);
            return paths[position - 1];
        }

        public String traceForPosition(int position) {
            return pathForPosition(position).trace;
        }

        public String finalThreeForPosition(int position) {
            return pathForPosition(position).finalThreeCsv();
        }

        public String summary() {
            return "formal="
                    + formalActionCount
                    + "|neverDelete="
                    + neverDeleteFormalAction
                    + "|nested="
                    + allPathsNested
                    + "|matrix="
                    + matrixChecksum
                    + "|hash="
                    + checksum;
        }
    }

    public Audit evaluate(
            V51Models.NumberProbabilityMatrix matrix, V51Models.PositionAction[] formalActions) {
        validateMatrix(matrix);
        K3ActionSpaceEngine.validateOrThrow(formalActions);
        int[][][] retained = new int[POSITIONS][STAGE_SIZES.length][];
        PositionPath[] paths = new PositionPath[POSITIONS];

        for (int positionIndex = 0; positionIndex < POSITIONS; positionIndex++) {
            double[] probability = matrix.probability[positionIndex];
            int[] order = stableProbabilityOrder(probability);
            StageEvidence[] stages = new StageEvidence[STAGE_SIZES.length];
            int previouslyRetained = NUMBERS;
            for (int stageIndex = 0; stageIndex < STAGE_SIZES.length; stageIndex++) {
                int retainedCount = STAGE_SIZES[stageIndex];
                int[] numbers = prefix(order, retainedCount);
                retained[positionIndex][stageIndex] = copy(numbers);
                int removedNumber = order[retainedCount];
                double weakestRetainedP = probability[order[retainedCount - 1] - 1];
                double strongestRemovedP = probability[order[retainedCount] - 1];
                if (previouslyRetained - retainedCount != 1)
                    throw new IllegalStateException(
                            "B10_FUNNEL_NON_UNIT_STAGE_"
                                    + previouslyRetained
                                    + '_'
                                    + retainedCount);
                stages[stageIndex] =
                        new StageEvidence(
                                positionIndex + 1,
                                stageIndex,
                                numbers,
                                removedNumber,
                                weakestRetainedP,
                                strongestRemovedP);
                previouslyRetained = retainedCount;
            }
            paths[positionIndex] = new PositionPath(positionIndex + 1, order, stages);
            if (!paths[positionIndex].nested)
                throw new IllegalStateException(
                        "B10_FUNNEL_NOT_NESTED_POSITION_" + (positionIndex + 1));
        }

        Audit audit = new Audit(retained, paths, formalActions.length, matrix.checksum);
        if (!audit.neverDeleteFormalAction || !audit.allPathsNested)
            throw new IllegalStateException("B10_FUNNEL_AUDIT_INVALID|" + audit.summary());
        return audit;
    }

    private static void validateMatrix(V51Models.NumberProbabilityMatrix matrix) {
        if (matrix == null || matrix.probability == null || matrix.probability.length != POSITIONS)
            throw new IllegalArgumentException("B10_FUNNEL_MATRIX_SHAPE");
        for (int p = 0; p < POSITIONS; p++) {
            double[] row = matrix.probability[p];
            if (row == null || row.length != NUMBERS)
                throw new IllegalArgumentException("B10_FUNNEL_ROW_SHAPE_" + (p + 1));
            double sum = 0.0d;
            for (int n = 0; n < NUMBERS; n++) {
                if (Double.isNaN(row[n]) || Double.isInfinite(row[n]) || row[n] < 0.0d)
                    throw new IllegalArgumentException(
                            "B10_FUNNEL_PROBABILITY_" + (p + 1) + '_' + (n + 1));
                sum += row[n];
            }
            if (sum <= 0.0d) throw new IllegalArgumentException("B10_FUNNEL_ROW_ZERO_" + (p + 1));
        }
    }

    private static int[] stableProbabilityOrder(double[] probability) {
        int[] numbers = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int i = 0; i < numbers.length; i++) {
            int best = i;
            for (int j = i + 1; j < numbers.length; j++) {
                int candidate = numbers[j];
                int incumbent = numbers[best];
                double candidateP = probability[candidate - 1];
                double incumbentP = probability[incumbent - 1];
                if (candidateP > incumbentP || (candidateP == incumbentP && candidate < incumbent))
                    best = j;
            }
            if (best != i) {
                int swap = numbers[i];
                numbers[i] = numbers[best];
                numbers[best] = swap;
            }
        }
        return numbers;
    }

    private static boolean validateNested(int[] sourceOrder, StageEvidence[] stages) {
        if (!validPermutation(sourceOrder) || stages == null || stages.length != STAGE_SIZES.length)
            return false;
        int[] previous = copy(sourceOrder);
        for (int i = 0; i < stages.length; i++) {
            StageEvidence stage = stages[i];
            if (stage == null || stage.retainedCount != STAGE_SIZES[i]) return false;
            if (!uniqueInRange(stage.retained)) return false;
            if (!isPrefix(previous, stage.retained)) return false;
            if (stage.removedNumber != previous[previous.length - 1]) return false;
            previous = copy(stage.retained);
        }
        return previous.length == K3ActionSpaceEngine.K;
    }

    private static boolean isPrefix(int[] previous, int[] next) {
        if (previous == null || next == null || previous.length != next.length + 1) return false;
        for (int i = 0; i < next.length; i++) if (previous[i] != next[i]) return false;
        return true;
    }

    private static boolean validPermutation(int[] values) {
        return values != null && values.length == NUMBERS && uniqueInRange(values);
    }

    private static boolean uniqueInRange(int[] values) {
        if (values == null) return false;
        boolean[] seen = new boolean[NUMBERS + 1];
        for (int i = 0; i < values.length; i++) {
            int value = values[i];
            if (value < 1 || value > NUMBERS || seen[value]) return false;
            seen[value] = true;
        }
        return true;
    }

    private static int[] prefix(int[] source, int length) {
        int[] out = new int[length];
        System.arraycopy(source, 0, out, 0, length);
        return out;
    }

    private static int[] copy(int[] source) {
        if (source == null) return new int[0];
        int[] out = new int[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static int[][][] copy(int[][][] source) {
        if (source == null) return new int[0][][];
        int[][][] out = new int[source.length][][];
        for (int i = 0; i < source.length; i++) {
            out[i] = new int[source[i].length][];
            for (int j = 0; j < source[i].length; j++) out[i][j] = copy(source[i][j]);
        }
        return out;
    }

    private static StageEvidence[] copy(StageEvidence[] source) {
        if (source == null) return new StageEvidence[0];
        StageEvidence[] out = new StageEvidence[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static PositionPath[] copy(PositionPath[] source) {
        if (source == null) return new PositionPath[0];
        PositionPath[] out = new PositionPath[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static String csv(int[] values) {
        StringBuilder out = new StringBuilder();
        if (values != null)
            for (int i = 0; i < values.length; i++) {
                if (i > 0) out.append(',');
                out.append(values[i]);
            }
        return out.toString();
    }

    private static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0d : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static boolean hasDecisionAuthority() {
        return false;
    }

    public static boolean canDeleteFormalActions() {
        return false;
    }
}
