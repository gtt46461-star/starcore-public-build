package com.ethk3.novaonepro;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * R929 fair-comparison runtime for NumberQ, Gate9, D0-D3 and R0-R5.
 *
 * <p>All candidates are evaluated on exactly the same chronological cases and resource budget.
 * Full action-space metrics treat 1200 rows as one issue, not 1200 independent samples.  Teacher
 * logits are accepted only when the teacher was frozen strictly before the target issue.</p>
 */
public final class DeepModelFamilyR929 {
    public static final String VERSION = "R929_DEEP_MODEL_FAMILY_V1";
    public static final int POSITIONS = 10;
    public static final int NUMBERS = 10;
    public static final int EXPERTS = 9;
    public static final int ACTIONS = 1200;
    public static final double MATRIX_TOLERANCE = 1.0e-6d;

    private DeepModelFamilyR929() {}

    public enum NumberModelKind {
        BASE_NUMBER_Q,
        D0_SMALL_MLP,
        D1_RESIDUAL,
        D2_FAST_SLOW,
        D3_DISTILLED,
        ONLINE_FTRL,
        ONLINE_TINY_DEEP
    }

    public enum RankerKind {
        R0_RAW_ACTION_P,
        R1_PAIRWISE_LINEAR,
        R2_LISTWISE_LINEAR,
        R3_TOP_WEIGHTED_PAIRWISE,
        R4_SMALL_MLP,
        R5_TREE_RESEARCH
    }

    public static final class Matrix100 {
        private final double[][] values;
        public final String sourceIssue;
        public final String targetIssue;
        public final String schemaHash;
        public final String matrixHash;

        public Matrix100(double[][] values, String sourceIssue, String targetIssue,
                String schemaHash) {
            if (values == null || values.length != POSITIONS)
                throw new IllegalArgumentException("R929_MATRIX_ROWS");
            this.values = new double[POSITIONS][NUMBERS];
            for (int p = 0; p < POSITIONS; p++) {
                if (values[p] == null || values[p].length != NUMBERS)
                    throw new IllegalArgumentException("R929_MATRIX_COLUMNS");
                for (int n = 0; n < NUMBERS; n++) {
                    double value = values[p][n];
                    if (!finite(value) || value < 0.0d)
                        throw new IllegalArgumentException("R929_MATRIX_NONFINITE_OR_NEGATIVE");
                    this.values[p][n] = value;
                }
            }
            this.sourceIssue = required(sourceIssue, "MATRIX_SOURCE_ISSUE");
            this.targetIssue = required(targetIssue, "MATRIX_TARGET_ISSUE");
            if (!PitFeatureDagR929.isStrictPast(this.sourceIssue, this.targetIssue))
                throw new IllegalArgumentException("R929_MATRIX_NOT_PIT");
            this.schemaHash = requireHash(schemaHash, "MATRIX_SCHEMA_HASH");
            validateDoublyStochastic(this.values, MATRIX_TOLERANCE);
            this.matrixHash = sha256(canonical());
        }

        public double get(int positionZeroBased, int numberZeroBased) {
            return values[positionZeroBased][numberZeroBased];
        }

        public double[][] copy() {
            double[][] copy = new double[POSITIONS][NUMBERS];
            for (int p = 0; p < POSITIONS; p++)
                System.arraycopy(values[p], 0, copy[p], 0, NUMBERS);
            return copy;
        }

        String canonical() {
            StringBuilder out = new StringBuilder(2200);
            out.append(sourceIssue).append('|').append(targetIssue).append('|').append(schemaHash);
            for (int p = 0; p < POSITIONS; p++) {
                for (int n = 0; n < NUMBERS; n++)
                    out.append('|').append(Long.toHexString(Double.doubleToLongBits(values[p][n])));
            }
            return out.toString();
        }
    }

    public static void validateDoublyStochastic(double[][] matrix, double tolerance) {
        if (matrix == null || matrix.length != POSITIONS || !finite(tolerance)
                || tolerance <= 0.0d)
            throw new IllegalArgumentException("R929_MATRIX_VALIDATION_INPUT");
        double[] columns = new double[NUMBERS];
        for (int p = 0; p < POSITIONS; p++) {
            if (matrix[p] == null || matrix[p].length != NUMBERS)
                throw new IllegalArgumentException("R929_MATRIX_VALIDATION_SHAPE");
            double row = 0.0d;
            for (int n = 0; n < NUMBERS; n++) {
                double value = matrix[p][n];
                if (!finite(value) || value < 0.0d)
                    throw new IllegalArgumentException("R929_MATRIX_VALUE_INVALID");
                row += value;
                columns[n] += value;
            }
            if (Math.abs(row - 1.0d) > tolerance)
                throw new IllegalArgumentException("R929_MATRIX_ROW_SUM:" + p + ':' + row);
        }
        for (int n = 0; n < NUMBERS; n++) {
            if (Math.abs(columns[n] - 1.0d) > tolerance)
                throw new IllegalArgumentException("R929_MATRIX_COLUMN_SUM:" + n + ':' + columns[n]);
        }
    }

    public static final class ExpertOutput {
        public final String expertId;
        public final boolean available;
        public final String unavailableReason;
        public final Matrix100 matrix;
        public final long costNs;
        public final String expertVersionHash;
        public final String outputHash;

        public ExpertOutput(String expertId, boolean available, String unavailableReason,
                Matrix100 matrix, long costNs, String expertVersionHash) {
            this.expertId = required(expertId, "EXPERT_ID");
            if (costNs < 0L) throw new IllegalArgumentException("R929_EXPERT_COST");
            this.available = available;
            this.unavailableReason = clean(unavailableReason);
            this.matrix = matrix;
            this.costNs = costNs;
            this.expertVersionHash = requireHash(expertVersionHash, "EXPERT_VERSION_HASH");
            if (available && matrix == null)
                throw new IllegalArgumentException("R929_AVAILABLE_EXPERT_WITHOUT_MATRIX");
            if (!available && this.unavailableReason.length() == 0)
                throw new IllegalArgumentException("R929_UNAVAILABLE_EXPERT_REASON_REQUIRED");
            this.outputHash = sha256(this.expertId + '|' + (available ? 1 : 0) + '|'
                    + this.unavailableReason + '|' + costNs + '|' + this.expertVersionHash + '|'
                    + (matrix == null ? "" : matrix.matrixHash));
        }
    }

    public static final class Gate9Result {
        private final double[] weights;
        public final int availableExperts;
        public final boolean fallback;
        public final String fallbackReason;
        public final String gateArtifactHash;
        public final String gateHash;

        public Gate9Result(List<ExpertOutput> experts, double[] rawWeights,
                String gateArtifactHash) {
            if (experts == null || experts.size() != EXPERTS || rawWeights == null
                    || rawWeights.length != EXPERTS)
                throw new IllegalArgumentException("R929_GATE9_CARDINALITY");
            this.gateArtifactHash = requireHash(gateArtifactHash, "GATE_ARTIFACT_HASH");
            this.weights = new double[EXPERTS];
            int count = 0;
            double sum = 0.0d;
            for (int i = 0; i < EXPERTS; i++) {
                ExpertOutput expert = experts.get(i);
                if (expert == null) throw new IllegalArgumentException("R929_GATE9_EXPERT_NULL");
                if (!finite(rawWeights[i]) || rawWeights[i] < 0.0d)
                    throw new IllegalArgumentException("R929_GATE9_WEIGHT_INVALID");
                if (expert.available) {
                    count++;
                    weights[i] = rawWeights[i];
                    sum += rawWeights[i];
                }
            }
            this.availableExperts = count;
            if (count == 0) {
                this.fallback = true;
                this.fallbackReason = "ALL_EXPERTS_UNAVAILABLE";
            } else if (!(sum > 0.0d)) {
                this.fallback = true;
                this.fallbackReason = "GATE_DEGENERATE_EQUAL_AVAILABLE_FALLBACK";
                double equal = 1.0d / count;
                for (int i = 0; i < EXPERTS; i++)
                    if (experts.get(i).available) weights[i] = equal;
            } else {
                this.fallback = false;
                this.fallbackReason = "";
                for (int i = 0; i < EXPERTS; i++) weights[i] /= sum;
            }
            double normalized = 0.0d;
            for (double value : weights) normalized += value;
            if (count > 0 && Math.abs(normalized - 1.0d) > 1.0e-12d)
                throw new IllegalStateException("R929_GATE9_NOT_NORMALIZED");
            StringBuilder canonical = new StringBuilder(this.gateArtifactHash).append('|')
                    .append(count).append('|').append(fallback ? 1 : 0).append('|')
                    .append(this.fallbackReason);
            for (double value : weights)
                canonical.append('|').append(Long.toHexString(Double.doubleToLongBits(value)));
            this.gateHash = sha256(canonical.toString());
        }

        public double weight(int expert) { return weights[expert]; }
        public double[] copyWeights() { return Arrays.copyOf(weights, weights.length); }
    }

    public static Matrix100 fuse(List<ExpertOutput> experts, Gate9Result gate,
            String sourceIssue, String targetIssue, String outputSchemaHash) {
        if (experts == null || experts.size() != EXPERTS || gate == null)
            throw new IllegalArgumentException("R929_FUSE_INPUT");
        if (gate.availableExperts == 0)
            throw new IllegalStateException("R929_FUSE_NO_EXPERT");
        double[][] output = new double[POSITIONS][NUMBERS];
        for (int i = 0; i < EXPERTS; i++) {
            ExpertOutput expert = experts.get(i);
            double weight = gate.weight(i);
            if (!expert.available || weight == 0.0d) continue;
            for (int p = 0; p < POSITIONS; p++)
                for (int n = 0; n < NUMBERS; n++)
                    output[p][n] += weight * expert.matrix.get(p, n);
        }
        // Convex fusion of doubly stochastic matrices remains doubly stochastic.
        return new Matrix100(output, sourceIssue, targetIssue, outputSchemaHash);
    }

    public static final class ModelCandidate {
        public final String candidateId;
        public final NumberModelKind numberKind;
        public final RankerKind rankerKind;
        public final String artifactHash;
        public final String inputStage;
        public final String parentArtifactHash;
        public final String trainedUntilIssue;
        public final long maximumCostNs;
        public final int parameterCount;
        public final String candidateHash;

        public ModelCandidate(String candidateId, NumberModelKind numberKind,
                RankerKind rankerKind, String artifactHash, String inputStage,
                String parentArtifactHash, String trainedUntilIssue,
                long maximumCostNs, int parameterCount) {
            this.candidateId = required(candidateId, "MODEL_CANDIDATE_ID");
            if (numberKind == null || rankerKind == null || maximumCostNs < 0L
                    || parameterCount < 0)
                throw new IllegalArgumentException("R929_MODEL_CANDIDATE_INVALID");
            this.numberKind = numberKind;
            this.rankerKind = rankerKind;
            this.artifactHash = requireHash(artifactHash, "MODEL_ARTIFACT_HASH");
            this.inputStage = required(inputStage, "MODEL_INPUT_STAGE");
            this.parentArtifactHash = clean(parentArtifactHash).length() == 0 ? ""
                    : requireHash(parentArtifactHash, "MODEL_PARENT_HASH");
            this.trainedUntilIssue = required(trainedUntilIssue, "MODEL_TRAINED_UNTIL");
            this.maximumCostNs = maximumCostNs;
            this.parameterCount = parameterCount;
            this.candidateHash = sha256(this.candidateId + '|' + numberKind.name() + '|'
                    + rankerKind.name() + '|' + this.artifactHash + '|' + this.inputStage + '|'
                    + this.parentArtifactHash + '|' + this.trainedUntilIssue + '|'
                    + maximumCostNs + '|' + parameterCount);
        }

        public void requireUsableFor(String targetIssue, String expectedInputStage,
                String expectedParentArtifactHash) {
            if (!PitFeatureDagR929.isStrictPast(trainedUntilIssue,
                    required(targetIssue, "MODEL_TARGET_ISSUE")))
                throw new IllegalArgumentException("R929_MODEL_NOT_PIT");
            if (!inputStage.equals(required(expectedInputStage, "EXPECTED_INPUT_STAGE")))
                throw new IllegalArgumentException("R929_MODEL_INPUT_SEMANTIC_MISMATCH");
            String expectedParent = clean(expectedParentArtifactHash);
            if (!parentArtifactHash.equals(expectedParent))
                throw new IllegalArgumentException("R929_MODEL_PARENT_MISMATCH");
        }
    }

    public static final class RankedAction {
        public final int ordinal;
        public final int position;
        public final int n1;
        public final int n2;
        public final int n3;
        public final double score;
        public final int rank;
        public final String checksum;

        RankedAction(int ordinal, int position, int n1, int n2, int n3,
                double score, int rank, String candidateHash, String issue) {
            this.ordinal = ordinal;
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.score = score;
            this.rank = rank;
            this.checksum = sha256(candidateHash + '|' + issue + '|' + ordinal + '|'
                    + position + '|' + n1 + '|' + n2 + '|' + n3 + '|'
                    + Long.toHexString(Double.doubleToLongBits(score)) + '|' + rank);
        }

        public boolean hit(int[] actual) {
            int value = actual[position - 1];
            return value == n1 || value == n2 || value == n3;
        }
    }

    public static final class CandidateOutput {
        public final ModelCandidate candidate;
        public final String issue;
        public final Matrix100 matrix;
        public final List<RankedAction> ranked;
        public final long costNs;
        public final String outputHash;

        public CandidateOutput(ModelCandidate candidate, String issue, Matrix100 matrix,
                double[] actionScores, long costNs) {
            if (candidate == null || matrix == null || actionScores == null
                    || actionScores.length != ACTIONS || costNs < 0L
                    || costNs > candidate.maximumCostNs)
                throw new IllegalArgumentException("R929_CANDIDATE_OUTPUT_INVALID");
            this.candidate = candidate;
            this.issue = required(issue, "CANDIDATE_OUTPUT_ISSUE");
            if (!matrix.targetIssue.equals(this.issue))
                throw new IllegalArgumentException("R929_CANDIDATE_MATRIX_ISSUE");
            this.matrix = matrix;
            this.costNs = costNs;
            ArrayList<ActionScore> scores = new ArrayList<ActionScore>(ACTIONS);
            int ordinal = 0;
            for (int p = 1; p <= POSITIONS; p++) {
                for (int a = 1; a <= 8; a++) {
                    for (int b = a + 1; b <= 9; b++) {
                        for (int c = b + 1; c <= 10; c++) {
                            double score = actionScores[ordinal];
                            if (!finite(score))
                                throw new IllegalArgumentException("R929_ACTION_SCORE_NONFINITE");
                            scores.add(new ActionScore(ordinal, p, a, b, c, score));
                            ordinal++;
                        }
                    }
                }
            }
            Collections.sort(scores, new Comparator<ActionScore>() {
                @Override public int compare(ActionScore left, ActionScore right) {
                    int byScore = Double.compare(right.score, left.score);
                    return byScore != 0 ? byScore : Integer.compare(left.ordinal, right.ordinal);
                }
            });
            ArrayList<RankedAction> output = new ArrayList<RankedAction>(ACTIONS);
            for (int rank = 0; rank < scores.size(); rank++) {
                ActionScore value = scores.get(rank);
                output.add(new RankedAction(value.ordinal, value.position, value.n1, value.n2,
                        value.n3, value.score, rank + 1, candidate.candidateHash, this.issue));
            }
            this.ranked = Collections.unmodifiableList(output);
            StringBuilder hashInput = new StringBuilder(candidate.candidateHash).append('|')
                    .append(this.issue).append('|').append(matrix.matrixHash).append('|').append(costNs);
            for (RankedAction action : output) hashInput.append('|').append(action.checksum);
            this.outputHash = sha256(hashInput.toString());
        }

        public RankedAction rankOne() { return ranked.get(0); }
    }

    private static final class ActionScore {
        final int ordinal, position, n1, n2, n3;
        final double score;
        ActionScore(int ordinal, int position, int n1, int n2, int n3, double score) {
            this.ordinal = ordinal;
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.score = score;
        }
    }

    public static final class EvaluationCase {
        public final String foldId;
        public final String issue;
        private final int[] actual;
        public final Map<String, CandidateOutput> outputs;
        public final String caseHash;

        public EvaluationCase(String foldId, String issue, int[] actual,
                Map<String, CandidateOutput> outputs) {
            this.foldId = required(foldId, "EVALUATION_FOLD");
            this.issue = required(issue, "EVALUATION_ISSUE");
            validatePermutation(actual);
            this.actual = Arrays.copyOf(actual, actual.length);
            if (outputs == null || outputs.isEmpty())
                throw new IllegalArgumentException("R929_EVALUATION_OUTPUTS_EMPTY");
            ArrayList<String> ids = new ArrayList<String>(outputs.keySet());
            Collections.sort(ids);
            LinkedHashMap<String, CandidateOutput> copy = new LinkedHashMap<String, CandidateOutput>();
            StringBuilder hashInput = new StringBuilder(this.foldId).append('|').append(this.issue)
                    .append('|').append(Arrays.toString(this.actual));
            for (String id : ids) {
                CandidateOutput output = outputs.get(id);
                if (output == null || !id.equals(output.candidate.candidateId)
                        || !this.issue.equals(output.issue))
                    throw new IllegalArgumentException("R929_EVALUATION_OUTPUT_MISMATCH");
                copy.put(id, output);
                hashInput.append('|').append(id).append(':').append(output.outputHash);
            }
            this.outputs = Collections.unmodifiableMap(copy);
            this.caseHash = sha256(hashInput.toString());
        }

        public int actual(int positionZeroBased) { return actual[positionZeroBased]; }
        public int[] actualCopy() { return Arrays.copyOf(actual, actual.length); }
    }

    public static final class Metrics {
        public final String candidateId;
        public final int issueN;
        public final double crossEntropy;
        public final double top1;
        public final double top3;
        public final double brier;
        public final double rankOneHitRate;
        public final double meanReciprocalRank;
        public final double ndcgAt10;
        public final long medianCostNs;
        public final long p95CostNs;
        public final String metricsHash;

        Metrics(String candidateId, int issueN, double crossEntropy, double top1, double top3,
                double brier, double rankOneHitRate, double meanReciprocalRank, double ndcgAt10,
                long medianCostNs, long p95CostNs) {
            this.candidateId = candidateId;
            this.issueN = issueN;
            this.crossEntropy = crossEntropy;
            this.top1 = top1;
            this.top3 = top3;
            this.brier = brier;
            this.rankOneHitRate = rankOneHitRate;
            this.meanReciprocalRank = meanReciprocalRank;
            this.ndcgAt10 = ndcgAt10;
            this.medianCostNs = medianCostNs;
            this.p95CostNs = p95CostNs;
            this.metricsHash = sha256(candidateId + '|' + issueN + '|' + crossEntropy + '|'
                    + top1 + '|' + top3 + '|' + brier + '|' + rankOneHitRate + '|'
                    + meanReciprocalRank + '|' + ndcgAt10 + '|' + medianCostNs + '|'
                    + p95CostNs);
        }
    }

    /**
     * Fair comparison: every candidate must be present for every issue and obey the same maximum
     * cost.  Missing/failed candidates are reported as failures by the caller, never silently
     * dropped from the denominator.
     */
    public static List<Metrics> compare(List<EvaluationCase> cases,
            List<ModelCandidate> candidates, long commonCostBudgetNs) {
        if (cases == null || cases.isEmpty() || candidates == null || candidates.isEmpty()
                || commonCostBudgetNs <= 0L)
            throw new IllegalArgumentException("R929_COMPARISON_INPUT_INVALID");
        LinkedHashSet<String> issueKeys = new LinkedHashSet<String>();
        for (EvaluationCase item : cases) {
            if (item == null || !issueKeys.add(item.foldId + '|' + item.issue))
                throw new IllegalArgumentException("R929_DUPLICATE_EVALUATION_ISSUE");
        }
        ArrayList<Metrics> metrics = new ArrayList<Metrics>();
        for (ModelCandidate candidate : candidates) {
            double ce = 0.0d, top1 = 0.0d, top3 = 0.0d, brier = 0.0d;
            double rankOneHit = 0.0d, reciprocal = 0.0d, ndcg = 0.0d;
            long[] costs = new long[cases.size()];
            int index = 0;
            for (EvaluationCase item : cases) {
                CandidateOutput output = item.outputs.get(candidate.candidateId);
                if (output == null)
                    throw new IllegalArgumentException("R929_CANDIDATE_MISSING_CASE:"
                            + candidate.candidateId + ':' + item.issue);
                if (!output.candidate.candidateHash.equals(candidate.candidateHash))
                    throw new IllegalArgumentException("R929_CANDIDATE_IDENTITY_DRIFT");
                if (output.costNs > commonCostBudgetNs)
                    throw new IllegalArgumentException("R929_CANDIDATE_COMMON_COST_FAIL:"
                            + candidate.candidateId);
                costs[index++] = output.costNs;
                for (int p = 0; p < POSITIONS; p++) {
                    int actual = item.actual(p) - 1;
                    double trueP = clamp(output.matrix.get(p, actual), 1.0e-15d, 1.0d);
                    ce += -Math.log(trueP);
                    int[] ordering = numberOrder(output.matrix, p);
                    if (ordering[0] == actual) top1 += 1.0d;
                    if (ordering[0] == actual || ordering[1] == actual || ordering[2] == actual)
                        top3 += 1.0d;
                    for (int n = 0; n < NUMBERS; n++) {
                        double y = n == actual ? 1.0d : 0.0d;
                        double error = output.matrix.get(p, n) - y;
                        brier += error * error;
                    }
                }
                RankedAction first = output.rankOne();
                if (first.hit(item.actual)) rankOneHit += 1.0d;
                int firstRelevantRank = 0;
                double dcg = 0.0d;
                for (int r = 0; r < output.ranked.size(); r++) {
                    RankedAction action = output.ranked.get(r);
                    if (action.hit(item.actual)) {
                        if (firstRelevantRank == 0) firstRelevantRank = r + 1;
                        if (r < 10) dcg += 1.0d / log2(r + 2.0d);
                    }
                }
                if (firstRelevantRank > 0) reciprocal += 1.0d / firstRelevantRank;
                double ideal = 0.0d;
                for (int r = 0; r < 10; r++) ideal += 1.0d / log2(r + 2.0d);
                ndcg += ideal <= 0.0d ? 0.0d : dcg / ideal;
            }
            Arrays.sort(costs);
            int issueN = cases.size();
            double numberDenominator = issueN * (double) POSITIONS;
            metrics.add(new Metrics(candidate.candidateId, issueN, ce / numberDenominator,
                    top1 / numberDenominator, top3 / numberDenominator,
                    brier / (numberDenominator * NUMBERS), rankOneHit / issueN,
                    reciprocal / issueN, ndcg / issueN,
                    percentile(costs, 0.50d), percentile(costs, 0.95d)));
        }
        return Collections.unmodifiableList(metrics);
    }

    public static ModelCandidate chooseChampion(List<Metrics> metrics,
            Map<String, ModelCandidate> candidates, String baselineId) {
        if (metrics == null || metrics.isEmpty() || candidates == null)
            throw new IllegalArgumentException("R929_CHAMPION_INPUT");
        Metrics baseline = null;
        for (Metrics value : metrics) if (value.candidateId.equals(baselineId)) baseline = value;
        if (baseline == null) throw new IllegalArgumentException("R929_BASELINE_MISSING");
        Metrics best = baseline;
        for (Metrics value : metrics) {
            // Candidate must improve calibration-sensitive number loss and cannot degrade rank hit.
            boolean improves = value.crossEntropy + 1.0e-9d < best.crossEntropy
                    && value.brier <= best.brier + 1.0e-9d
                    && value.rankOneHitRate + 1.0e-9d >= best.rankOneHitRate
                    && value.p95CostNs <= best.p95CostNs;
            if (improves) best = value;
        }
        ModelCandidate selected = candidates.get(best.candidateId);
        if (selected == null) throw new IllegalArgumentException("R929_CHAMPION_ID_UNKNOWN");
        return selected;
    }

    public static void requireTeacherStrictlyPast(String teacherTrainedUntilIssue,
            String targetIssue, String teacherArtifactHash) {
        requireHash(teacherArtifactHash, "TEACHER_ARTIFACT_HASH");
        if (!PitFeatureDagR929.isStrictPast(required(teacherTrainedUntilIssue,
                "TEACHER_TRAINED_UNTIL"), required(targetIssue, "TEACHER_TARGET")))
            throw new IllegalArgumentException("R929_TEACHER_NOT_STRICTLY_PAST");
    }

    /** Immutable labelled update used by the dual-memory continual learner. */
    public static final class LearningExample {
        public final String game;
        public final String issue;
        public final String labelVersion;
        public final String inputHash;
        private final double[] features;
        public final int target;
        public final String idempotencyKey;

        public LearningExample(String game, String issue, String labelVersion, String inputHash,
                double[] features, int target) {
            this.game = required(game, "LEARN_GAME");
            this.issue = required(issue, "LEARN_ISSUE");
            this.labelVersion = required(labelVersion, "LEARN_LABEL_VERSION");
            this.inputHash = requireHash(inputHash, "LEARN_INPUT_HASH");
            if (features == null || features.length == 0 || target < 0 || target >= NUMBERS)
                throw new IllegalArgumentException("R929_LEARN_EXAMPLE_INVALID");
            this.features = Arrays.copyOf(features, features.length);
            for (double value : this.features) if (!finite(value))
                throw new IllegalArgumentException("R929_LEARN_FEATURE_NONFINITE");
            this.target = target;
            this.idempotencyKey = sha256(this.game + '|' + this.issue + '|'
                    + this.labelVersion + '|' + this.inputHash);
        }

        public double[] featureCopy() { return Arrays.copyOf(features, features.length); }
    }

    public static final class LearnerConfig {
        public final int dimensions;
        public final int recentCapacity;
        public final int longCapacity;
        public final int stepsPerUpdate;
        public final double learningRate;
        public final double gradientClip;
        public final long seed;
        public final String configHash;

        public LearnerConfig(int dimensions, int recentCapacity, int longCapacity,
                int stepsPerUpdate, double learningRate, double gradientClip, long seed) {
            if (dimensions <= 0 || dimensions > 4096 || recentCapacity <= 0
                    || longCapacity <= 0 || stepsPerUpdate <= 0 || stepsPerUpdate > 128
                    || !finite(learningRate) || learningRate <= 0.0d || learningRate > 1.0d
                    || !finite(gradientClip) || gradientClip <= 0.0d)
                throw new IllegalArgumentException("R929_LEARNER_CONFIG_INVALID");
            this.dimensions = dimensions;
            this.recentCapacity = recentCapacity;
            this.longCapacity = longCapacity;
            this.stepsPerUpdate = stepsPerUpdate;
            this.learningRate = learningRate;
            this.gradientClip = gradientClip;
            this.seed = seed;
            this.configHash = sha256(dimensions + "|" + recentCapacity + "|" + longCapacity
                    + "|" + stepsPerUpdate + "|" + learningRate + "|" + gradientClip
                    + "|" + seed);
        }
    }

    public static final class LearnerState {
        public final LearnerConfig config;
        private final double[][] weights;
        private final double[] bias;
        private final ArrayDeque<LearningExample> recent;
        private final ArrayList<LearningExample> longReplay;
        private final LinkedHashSet<String> appliedKeys;
        public final long updates;
        public final String trainedUntilIssue;
        public final String stateHash;

        private LearnerState(LearnerConfig config, double[][] weights, double[] bias,
                ArrayDeque<LearningExample> recent, ArrayList<LearningExample> longReplay,
                LinkedHashSet<String> appliedKeys, long updates, String trainedUntilIssue) {
            this.config = config;
            this.weights = deepCopy(weights);
            this.bias = Arrays.copyOf(bias, bias.length);
            this.recent = new ArrayDeque<LearningExample>(recent);
            this.longReplay = new ArrayList<LearningExample>(longReplay);
            this.appliedKeys = new LinkedHashSet<String>(appliedKeys);
            this.updates = updates;
            this.trainedUntilIssue = clean(trainedUntilIssue);
            this.stateHash = sha256(canonical());
        }

        public static LearnerState initial(LearnerConfig config) {
            if (config == null) throw new IllegalArgumentException("R929_LEARNER_CONFIG_REQUIRED");
            double[][] weights = new double[NUMBERS][config.dimensions];
            double[] bias = new double[NUMBERS];
            // Deterministic tiny initialization prevents exact symmetry without hidden randomness.
            long value = config.seed;
            for (int c = 0; c < NUMBERS; c++) {
                for (int d = 0; d < config.dimensions; d++) {
                    value = mix64(value + 0x9E3779B97F4A7C15L + c * 131L + d);
                    weights[c][d] = ((value >>> 11) * 0x1.0p-53d - 0.5d) * 0.002d;
                }
            }
            return new LearnerState(config, weights, bias,
                    new ArrayDeque<LearningExample>(), new ArrayList<LearningExample>(),
                    new LinkedHashSet<String>(), 0L, "");
        }

        public LearnerState update(LearningExample example) {
            if (example == null || example.features.length != config.dimensions)
                throw new IllegalArgumentException("R929_LEARNER_EXAMPLE_DIMENSION");
            if (appliedKeys.contains(example.idempotencyKey)) return this;
            if (trainedUntilIssue.length() > 0
                    && PitFeatureDagR929.compareIssues(example.issue, trainedUntilIssue) <= 0)
                throw new IllegalArgumentException("R929_LEARNER_OUT_OF_ORDER_OR_DUPLICATE_LABEL");
            double[][] nextWeights = deepCopy(weights);
            double[] nextBias = Arrays.copyOf(bias, bias.length);
            ArrayDeque<LearningExample> nextRecent = new ArrayDeque<LearningExample>(recent);
            ArrayList<LearningExample> nextLong = new ArrayList<LearningExample>(longReplay);
            LinkedHashSet<String> nextKeys = new LinkedHashSet<String>(appliedKeys);
            nextRecent.addLast(example);
            while (nextRecent.size() > config.recentCapacity) nextRecent.removeFirst();
            if (nextLong.size() < config.longCapacity) {
                nextLong.add(example);
            } else {
                long index = Math.floorMod(mix64(config.seed ^ updates
                        ^ example.idempotencyKey.hashCode()), updates + 1L);
                if (index < config.longCapacity) nextLong.set((int) index, example);
            }
            ArrayList<LearningExample> replay = new ArrayList<LearningExample>();
            replay.addAll(nextRecent);
            replay.addAll(nextLong);
            if (replay.isEmpty()) replay.add(example);
            for (int step = 0; step < config.stepsPerUpdate; step++) {
                long pick = Math.floorMod(mix64(config.seed + updates * 17L + step * 7919L),
                        replay.size());
                applyGradient(nextWeights, nextBias, replay.get((int) pick), config);
            }
            nextKeys.add(example.idempotencyKey);
            while (nextKeys.size() > config.recentCapacity + config.longCapacity + 1024) {
                java.util.Iterator<String> iterator = nextKeys.iterator();
                iterator.next();
                iterator.remove();
            }
            return new LearnerState(config, nextWeights, nextBias, nextRecent, nextLong,
                    nextKeys, updates + 1L, example.issue);
        }

        public double[] predict(double[] features, String targetIssue) {
            if (features == null || features.length != config.dimensions)
                throw new IllegalArgumentException("R929_LEARNER_PREDICT_DIMENSION");
            if (trainedUntilIssue.length() > 0
                    && !PitFeatureDagR929.isStrictPast(trainedUntilIssue,
                            required(targetIssue, "LEARNER_TARGET_ISSUE")))
                throw new IllegalArgumentException("R929_LEARNER_CURRENT_LABEL_LEAK");
            double[] logits = new double[NUMBERS];
            for (int c = 0; c < NUMBERS; c++) {
                double value = bias[c];
                for (int d = 0; d < config.dimensions; d++) value += weights[c][d] * features[d];
                logits[c] = value;
            }
            return softmax(logits);
        }

        public byte[] encode() {
            int bytes = 8 + 4 + NUMBERS * config.dimensions * 8 + NUMBERS * 8
                    + 4 + trainedUntilIssue.getBytes(StandardCharsets.UTF_8).length;
            ByteBuffer buffer = ByteBuffer.allocate(bytes).order(ByteOrder.BIG_ENDIAN);
            buffer.putLong(updates);
            buffer.putInt(config.dimensions);
            for (int c = 0; c < NUMBERS; c++)
                for (int d = 0; d < config.dimensions; d++) buffer.putDouble(weights[c][d]);
            for (double value : bias) buffer.putDouble(value);
            byte[] issue = trainedUntilIssue.getBytes(StandardCharsets.UTF_8);
            buffer.putInt(issue.length);
            buffer.put(issue);
            return buffer.array();
        }

        String canonical() {
            StringBuilder out = new StringBuilder(config.dimensions * NUMBERS * 18 + 512);
            out.append(config.configHash).append('|').append(updates).append('|')
                    .append(trainedUntilIssue).append('|').append(recent.size()).append('|')
                    .append(longReplay.size()).append('|').append(appliedKeys.size());
            for (int c = 0; c < NUMBERS; c++) {
                for (int d = 0; d < config.dimensions; d++)
                    out.append('|').append(Long.toHexString(Double.doubleToLongBits(weights[c][d])));
                out.append('|').append(Long.toHexString(Double.doubleToLongBits(bias[c])));
            }
            for (LearningExample value : recent) out.append('|').append(value.idempotencyKey);
            for (LearningExample value : longReplay) out.append('|').append(value.idempotencyKey);
            return out.toString();
        }
    }

    private static void applyGradient(double[][] weights, double[] bias,
            LearningExample example, LearnerConfig config) {
        double[] logits = new double[NUMBERS];
        for (int c = 0; c < NUMBERS; c++) {
            double value = bias[c];
            for (int d = 0; d < config.dimensions; d++)
                value += weights[c][d] * example.features[d];
            logits[c] = value;
        }
        double[] probability = softmax(logits);
        for (int c = 0; c < NUMBERS; c++) {
            double error = probability[c] - (c == example.target ? 1.0d : 0.0d);
            error = clamp(error, -config.gradientClip, config.gradientClip);
            bias[c] -= config.learningRate * error;
            for (int d = 0; d < config.dimensions; d++) {
                double gradient = error * example.features[d];
                gradient = clamp(gradient, -config.gradientClip, config.gradientClip);
                weights[c][d] -= config.learningRate * gradient;
                if (!finite(weights[c][d]))
                    throw new IllegalStateException("R929_LEARNER_WEIGHT_NONFINITE");
            }
        }
    }

    private static double[] softmax(double[] logits) {
        double max = -Double.MAX_VALUE;
        for (double value : logits) {
            if (!finite(value)) throw new IllegalArgumentException("R929_LOGIT_NONFINITE");
            if (value > max) max = value;
        }
        double[] output = new double[logits.length];
        double sum = 0.0d;
        for (int i = 0; i < logits.length; i++) {
            output[i] = Math.exp(Math.max(-700.0d, Math.min(700.0d, logits[i] - max)));
            sum += output[i];
        }
        if (!(sum > 0.0d) || !finite(sum))
            throw new IllegalStateException("R929_SOFTMAX_DEGENERATE");
        for (int i = 0; i < output.length; i++) output[i] /= sum;
        return output;
    }

    private static int[] numberOrder(final Matrix100 matrix, final int position) {
        Integer[] values = new Integer[NUMBERS];
        for (int i = 0; i < NUMBERS; i++) values[i] = Integer.valueOf(i);
        Arrays.sort(values, new Comparator<Integer>() {
            @Override public int compare(Integer left, Integer right) {
                int by = Double.compare(matrix.get(position, right.intValue()),
                        matrix.get(position, left.intValue()));
                return by != 0 ? by : Integer.compare(left.intValue(), right.intValue());
            }
        });
        int[] output = new int[NUMBERS];
        for (int i = 0; i < NUMBERS; i++) output[i] = values[i].intValue();
        return output;
    }

    private static long percentile(long[] sorted, double fraction) {
        if (sorted.length == 0) return 0L;
        int index = (int) Math.ceil(fraction * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }

    private static double log2(double value) { return Math.log(value) / Math.log(2.0d); }

    private static void validatePermutation(int[] actual) {
        if (actual == null || actual.length != POSITIONS)
            throw new IllegalArgumentException("R929_ACTUAL_SHAPE");
        boolean[] seen = new boolean[NUMBERS + 1];
        for (int value : actual) {
            if (value < 1 || value > NUMBERS || seen[value])
                throw new IllegalArgumentException("R929_ACTUAL_NOT_PERMUTATION");
            seen[value] = true;
        }
    }

    private static double[][] deepCopy(double[][] values) {
        double[][] copy = new double[values.length][];
        for (int i = 0; i < values.length; i++) copy[i] = Arrays.copyOf(values[i], values[i].length);
        return copy;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    private static boolean finite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
    private static double clamp(double value, double low, double high) {
        if (!finite(value)) return low;
        return Math.max(low, Math.min(high, value));
    }
    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String required(String value, String code) {
        String clean = clean(value);
        if (clean.length() == 0) throw new IllegalArgumentException("R929_" + code + "_REQUIRED");
        return clean;
    }
    private static String requireHash(String value, String code) {
        String clean = required(value, code).toLowerCase(java.util.Locale.US);
        if (clean.length() != 64) throw new IllegalArgumentException("R929_" + code + "_NOT_SHA256");
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')))
                throw new IllegalArgumentException("R929_" + code + "_NOT_SHA256");
        }
        return clean;
    }
    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(64);
            for (byte item : hash)
                output.append(String.format(java.util.Locale.US, "%02x", item & 255));
            return output.toString();
        } catch (Exception failure) {
            throw new IllegalStateException("R929_SHA256_UNAVAILABLE", failure);
        }
    }
}
