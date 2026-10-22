package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Real ablation, directed counterfactual, minimal repair and quality-driven structure creation.
 *
 * <p>An ablation is accepted only when the evaluator actually recomputes the output with the
 * requested node removed.  A text tag, fixed score or same-output replay is rejected.  Repair
 * children preserve an immutable parent and are evaluated on exactly the same chronological
 * cases.  Search creates new typed graph structures; it cannot mint a certificate or REAL action.</p>
 */
public final class AblationRepairCreationR930 {
    public static final String VERSION = "R930_ABLATION_REPAIR_CREATION_V1";
    public static final boolean MAY_CREATE_REAL = false;

    private AblationRepairCreationR930() {}

    public enum FailureStage {
        CAPTURE,
        ATOM,
        FACTOR,
        MOLECULE,
        MATRIX,
        NUMBER_MODEL,
        ACTION_RANK,
        CALIBRATION,
        CERTIFICATE,
        CONTRIBUTION
    }
    public enum RepairKind {
        REMOVE_NODE,
        REPLACE_NODE,
        ADD_RESIDUAL,
        CHANGE_LAG,
        CHANGE_WINDOW,
        CLIP_OUTPUT,
        SHRINK_WEIGHT,
        ROUTE_DOMAIN,
        LOWER_RESOURCE_COST
    }
    public enum InterventionKind { SET_ZERO, SET_VALUE, MASK_MISSING, REMOVE_EDGE }

    public static final class CandidateDefinition {
        public final String candidateId;
        public final String familyId;
        public final String graphHash;
        public final List<String> orderedNodeHashes;
        public final String outputNodeId;
        public final String inputStage;
        public final int estimatedParameters;
        public final long estimatedCostNs;
        public final String parentCandidateHash;
        public final String creationReason;
        public final String structuralHash;

        public CandidateDefinition(String candidateId, String familyId, String graphHash,
                List<String> orderedNodeHashes, String outputNodeId, String inputStage,
                int estimatedParameters, long estimatedCostNs, String parentCandidateHash,
                String creationReason) {
            this.candidateId = required(candidateId, "CANDIDATE_ID");
            this.familyId = required(familyId, "CANDIDATE_FAMILY");
            this.graphHash = requireHash(graphHash, "CANDIDATE_GRAPH_HASH");
            if (orderedNodeHashes == null || orderedNodeHashes.isEmpty()
                    || orderedNodeHashes.size() > 256 || estimatedParameters < 0
                    || estimatedCostNs < 0L)
                throw new IllegalArgumentException("R930_CANDIDATE_DEFINITION_INVALID");
            ArrayList<String> nodes = new ArrayList<String>();
            for (String hash : orderedNodeHashes)
                nodes.add(requireHash(hash, "CANDIDATE_NODE_HASH"));
            this.orderedNodeHashes = Collections.unmodifiableList(nodes);
            this.outputNodeId = required(outputNodeId, "CANDIDATE_OUTPUT_NODE");
            this.inputStage = required(inputStage, "CANDIDATE_INPUT_STAGE");
            this.estimatedParameters = estimatedParameters;
            this.estimatedCostNs = estimatedCostNs;
            this.parentCandidateHash = clean(parentCandidateHash).length() == 0 ? ""
                    : requireHash(parentCandidateHash, "CANDIDATE_PARENT_HASH");
            this.creationReason = required(creationReason, "CANDIDATE_CREATION_REASON");
            this.structuralHash = sha256(canonicalWithoutHash());
        }

        String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(1024);
            out.append(familyId).append('|').append(graphHash).append('|')
                    .append(outputNodeId).append('|').append(inputStage).append('|')
                    .append(estimatedParameters).append('|').append(estimatedCostNs).append('|')
                    .append(parentCandidateHash).append('|').append(creationReason);
            for (String node : orderedNodeHashes) out.append('|').append(node);
            return out.toString();
        }
    }

    public static final class IssueCase {
        public final String targetIssue;
        public final String maxSourceIssue;
        public final long frozenAtMs;
        public final String inputHash;
        public final String labelHash;
        public final boolean eligible;
        public final boolean hit;
        public final String domain;
        public final String caseHash;

        public IssueCase(String targetIssue, String maxSourceIssue, long frozenAtMs,
                String inputHash, String labelHash, boolean eligible, boolean hit, String domain) {
            this.targetIssue = required(targetIssue, "CASE_TARGET");
            this.maxSourceIssue = required(maxSourceIssue, "CASE_MAX_SOURCE");
            if (!PitFeatureDagR929.isStrictPast(this.maxSourceIssue, this.targetIssue)
                    || frozenAtMs <= 0L)
                throw new IllegalArgumentException("R930_CASE_NOT_PIT");
            this.frozenAtMs = frozenAtMs;
            this.inputHash = requireHash(inputHash, "CASE_INPUT_HASH");
            this.labelHash = requireHash(labelHash, "CASE_LABEL_HASH");
            this.eligible = eligible;
            this.hit = hit;
            this.domain = required(domain, "CASE_DOMAIN");
            this.caseHash = sha256(this.targetIssue + '|' + this.maxSourceIssue + '|'
                    + frozenAtMs + '|' + this.inputHash + '|' + this.labelHash + '|'
                    + (eligible ? 1 : 0) + '|' + (hit ? 1 : 0) + '|' + this.domain);
        }
    }

    public static final class Intervention {
        public final InterventionKind kind;
        public final String nodeId;
        public final String edgeParentId;
        public final double value;
        public final String interventionHash;

        public Intervention(InterventionKind kind, String nodeId, String edgeParentId,
                double value) {
            if (kind == null || !finite(value))
                throw new IllegalArgumentException("R930_INTERVENTION_INVALID");
            this.kind = kind;
            this.nodeId = required(nodeId, "INTERVENTION_NODE");
            this.edgeParentId = clean(edgeParentId);
            if (kind == InterventionKind.REMOVE_EDGE && this.edgeParentId.length() == 0)
                throw new IllegalArgumentException("R930_INTERVENTION_EDGE_PARENT_REQUIRED");
            this.value = value;
            this.interventionHash = sha256(kind.name() + '|' + this.nodeId + '|'
                    + this.edgeParentId + '|' + Long.toHexString(Double.doubleToLongBits(value)));
        }
    }

    public static final class EvaluationOutput {
        public final String targetIssue;
        public final String candidateHash;
        public final String selectedActionHash;
        public final double selectedHitProbability;
        public final double rawScore;
        public final boolean selected;
        public final boolean hit;
        public final long costNs;
        public final String inputHash;
        public final String executionHash;

        public EvaluationOutput(String targetIssue, String candidateHash,
                String selectedActionHash, double selectedHitProbability, double rawScore,
                boolean selected, boolean hit, long costNs, String inputHash,
                String executionHash) {
            this.targetIssue = required(targetIssue, "OUTPUT_TARGET");
            this.candidateHash = requireHash(candidateHash, "OUTPUT_CANDIDATE_HASH");
            this.selectedActionHash = requireHash(selectedActionHash, "OUTPUT_ACTION_HASH");
            if ((selected && !probability(selectedHitProbability)) || !finite(rawScore)
                    || costNs < 0L)
                throw new IllegalArgumentException("R930_EVALUATION_OUTPUT_INVALID");
            this.selectedHitProbability = selectedHitProbability;
            this.rawScore = rawScore;
            this.selected = selected;
            this.hit = hit;
            this.costNs = costNs;
            this.inputHash = requireHash(inputHash, "OUTPUT_INPUT_HASH");
            this.executionHash = requireHash(executionHash, "OUTPUT_EXECUTION_HASH");
        }

        public double brier() {
            if (!selected) return Double.NaN;
            double y = hit ? 1.0d : 0.0d;
            double d = selectedHitProbability - y;
            return d * d;
        }
        public double nll() {
            if (!selected) return Double.NaN;
            double p = clamp(selectedHitProbability, 1.0e-12d, 1.0d - 1.0e-12d);
            return hit ? -Math.log(p) : -Math.log(1.0d - p);
        }
    }

    public interface Evaluator {
        EvaluationOutput evaluate(CandidateDefinition candidate, IssueCase issue,
                Set<String> disabledNodes, List<Intervention> interventions);
    }

    public static final class FailureRecord {
        public final String candidateHash;
        public final String targetIssue;
        public final FailureStage stage;
        public final String code;
        public final String detail;
        public final boolean productionFallbackSucceeded;
        public final long elapsedNs;
        public final String inputHash;
        public final String failureHash;

        public FailureRecord(String candidateHash, String targetIssue, FailureStage stage,
                String code, String detail, boolean productionFallbackSucceeded,
                long elapsedNs, String inputHash) {
            this.candidateHash = requireHash(candidateHash, "FAILURE_CANDIDATE_HASH");
            this.targetIssue = required(targetIssue, "FAILURE_TARGET");
            if (stage == null || elapsedNs < 0L)
                throw new IllegalArgumentException("R930_FAILURE_RECORD_INVALID");
            this.stage = stage;
            this.code = required(code, "FAILURE_CODE");
            this.detail = clean(detail);
            this.productionFallbackSucceeded = productionFallbackSucceeded;
            this.elapsedNs = elapsedNs;
            this.inputHash = requireHash(inputHash, "FAILURE_INPUT_HASH");
            this.failureHash = sha256(this.candidateHash + '|' + this.targetIssue + '|'
                    + stage.name() + '|' + this.code + '|' + this.detail + '|'
                    + (productionFallbackSucceeded ? 1 : 0) + '|' + elapsedNs + '|'
                    + this.inputHash);
        }
    }

    public static final class AblationRow {
        public final String candidateHash;
        public final String nodeId;
        public final String targetIssue;
        public final String caseHash;
        public final EvaluationOutput baseline;
        public final EvaluationOutput ablated;
        public final double probabilityDelta;
        public final double brierDelta;
        public final double nllDelta;
        public final double costDeltaNs;
        public final boolean actionChanged;
        public final String rowHash;

        AblationRow(String candidateHash, String nodeId, IssueCase issue,
                EvaluationOutput baseline, EvaluationOutput ablated) {
            this.candidateHash = candidateHash;
            this.nodeId = nodeId;
            this.targetIssue = issue.targetIssue;
            this.caseHash = issue.caseHash;
            this.baseline = baseline;
            this.ablated = ablated;
            this.probabilityDelta = bothSelected(baseline, ablated)
                    ? baseline.selectedHitProbability - ablated.selectedHitProbability
                    : Double.NaN;
            this.brierDelta = bothSelected(baseline, ablated)
                    ? ablated.brier() - baseline.brier() : Double.NaN;
            this.nllDelta = bothSelected(baseline, ablated)
                    ? ablated.nll() - baseline.nll() : Double.NaN;
            this.costDeltaNs = baseline.costNs - ablated.costNs;
            this.actionChanged = !baseline.selectedActionHash.equals(ablated.selectedActionHash);
            this.rowHash = sha256(candidateHash + '|' + nodeId + '|' + issue.caseHash + '|'
                    + baseline.executionHash + '|' + ablated.executionHash + '|'
                    + probabilityDelta + '|' + brierDelta + '|' + nllDelta + '|'
                    + costDeltaNs + '|' + (actionChanged ? 1 : 0));
        }
    }

    public static AblationRow ablate(CandidateDefinition candidate, IssueCase issue,
            String nodeId, Evaluator evaluator) {
        if (candidate == null || issue == null || evaluator == null)
            throw new IllegalArgumentException("R930_ABLATION_INPUT_REQUIRED");
        String node = required(nodeId, "ABLATION_NODE");
        EvaluationOutput baseline = evaluator.evaluate(candidate, issue,
                Collections.<String>emptySet(), Collections.<Intervention>emptyList());
        LinkedHashSet<String> disabled = new LinkedHashSet<String>();
        disabled.add(node);
        EvaluationOutput ablated = evaluator.evaluate(candidate, issue,
                Collections.unmodifiableSet(disabled), Collections.<Intervention>emptyList());
        validatePaired(candidate, issue, baseline, ablated);
        if (baseline.executionHash.equals(ablated.executionHash))
            throw new IllegalArgumentException("R930_ABLATION_NOT_RECOMPUTED");
        return new AblationRow(candidate.structuralHash, node, issue, baseline, ablated);
    }

    public static final class AblationSummary {
        public final String candidateHash;
        public final String nodeId;
        public final int pairedN;
        public final int selectedPairN;
        public final double meanProbabilityDelta;
        public final double meanBrierIncreaseWhenRemoved;
        public final double meanNllIncreaseWhenRemoved;
        public final double actionChangeRate;
        public final double meanCostSavedNs;
        public final int positiveBrierEffects;
        public final double signTestP;
        public final String supportState;
        public final String summaryHash;

        AblationSummary(String candidateHash, String nodeId, int pairedN, int selectedPairN,
                double meanProbabilityDelta, double meanBrierIncreaseWhenRemoved,
                double meanNllIncreaseWhenRemoved, double actionChangeRate,
                double meanCostSavedNs, int positiveBrierEffects, double signTestP,
                String supportState, String summaryHash) {
            this.candidateHash = candidateHash;
            this.nodeId = nodeId;
            this.pairedN = pairedN;
            this.selectedPairN = selectedPairN;
            this.meanProbabilityDelta = meanProbabilityDelta;
            this.meanBrierIncreaseWhenRemoved = meanBrierIncreaseWhenRemoved;
            this.meanNllIncreaseWhenRemoved = meanNllIncreaseWhenRemoved;
            this.actionChangeRate = actionChangeRate;
            this.meanCostSavedNs = meanCostSavedNs;
            this.positiveBrierEffects = positiveBrierEffects;
            this.signTestP = signTestP;
            this.supportState = supportState;
            this.summaryHash = summaryHash;
        }

        public boolean evidenceOfUsefulContribution() {
            return pairedN >= 120 && selectedPairN >= 48
                    && meanBrierIncreaseWhenRemoved > 0.0d && signTestP <= 0.05d;
        }
    }

    public static AblationSummary summarize(List<AblationRow> rows) {
        if (rows == null || rows.isEmpty())
            throw new IllegalArgumentException("R930_ABLATION_ROWS_EMPTY");
        String candidate = rows.get(0).candidateHash;
        String node = rows.get(0).nodeId;
        Set<String> issues = new HashSet<String>();
        int selected = 0, changed = 0, positive = 0;
        double pDelta = 0.0d, brier = 0.0d, nll = 0.0d, cost = 0.0d;
        StringBuilder evidence = new StringBuilder(candidate).append('|').append(node);
        for (AblationRow row : rows) {
            if (row == null || !candidate.equals(row.candidateHash) || !node.equals(row.nodeId)
                    || !issues.add(row.targetIssue))
                throw new IllegalArgumentException("R930_ABLATION_ROW_IDENTITY_OR_DUPLICATE");
            if (bothSelected(row.baseline, row.ablated)) {
                selected++;
                pDelta += row.probabilityDelta;
                brier += row.brierDelta;
                nll += row.nllDelta;
                if (row.brierDelta > 0.0d) positive++;
            }
            if (row.actionChanged) changed++;
            cost += row.costDeltaNs;
            evidence.append('|').append(row.rowHash);
        }
        int n = rows.size();
        double signP = selected == 0 ? 1.0d : twoSidedBinomialP(positive, selected);
        String state = n < 48 ? "SUPPORT_PENDING"
                : n < 120 ? "OBSERVING"
                : selected < 48 ? "SELECTED_SUPPORT_LOW" : "PAIRED_READY";
        String hash = sha256(evidence.toString() + '|' + state);
        return new AblationSummary(candidate, node, n, selected,
                selected == 0 ? Double.NaN : pDelta / selected,
                selected == 0 ? Double.NaN : brier / selected,
                selected == 0 ? Double.NaN : nll / selected,
                changed / (double) n, cost / n, positive, signP, state, hash);
    }

    public static final class CounterfactualRow {
        public final String candidateHash;
        public final String targetIssue;
        public final Intervention intervention;
        public final String confoundPolicyHash;
        public final EvaluationOutput baseline;
        public final EvaluationOutput counterfactual;
        public final double probabilityEffect;
        public final double scoreEffect;
        public final boolean actionChanged;
        public final String rowHash;

        CounterfactualRow(CandidateDefinition candidate, IssueCase issue,
                Intervention intervention, String confoundPolicyHash,
                EvaluationOutput baseline, EvaluationOutput counterfactual) {
            this.candidateHash = candidate.structuralHash;
            this.targetIssue = issue.targetIssue;
            this.intervention = intervention;
            this.confoundPolicyHash = confoundPolicyHash;
            this.baseline = baseline;
            this.counterfactual = counterfactual;
            this.probabilityEffect = bothSelected(baseline, counterfactual)
                    ? counterfactual.selectedHitProbability - baseline.selectedHitProbability
                    : Double.NaN;
            this.scoreEffect = counterfactual.rawScore - baseline.rawScore;
            this.actionChanged = !baseline.selectedActionHash.equals(
                    counterfactual.selectedActionHash);
            this.rowHash = sha256(this.candidateHash + '|' + this.targetIssue + '|'
                    + intervention.interventionHash + '|' + confoundPolicyHash + '|'
                    + baseline.executionHash + '|' + counterfactual.executionHash + '|'
                    + probabilityEffect + '|' + scoreEffect + '|'
                    + (actionChanged ? 1 : 0));
        }
    }

    public static CounterfactualRow counterfactual(CandidateDefinition candidate,
            IssueCase issue, Intervention intervention, String confoundPolicyHash,
            Evaluator evaluator) {
        if (candidate == null || issue == null || intervention == null || evaluator == null)
            throw new IllegalArgumentException("R930_COUNTERFACTUAL_INPUT_REQUIRED");
        String confound = requireHash(confoundPolicyHash, "CONFOUND_POLICY_HASH");
        EvaluationOutput baseline = evaluator.evaluate(candidate, issue,
                Collections.<String>emptySet(), Collections.<Intervention>emptyList());
        EvaluationOutput changed = evaluator.evaluate(candidate, issue,
                Collections.<String>emptySet(), Collections.singletonList(intervention));
        validatePaired(candidate, issue, baseline, changed);
        if (baseline.executionHash.equals(changed.executionHash))
            throw new IllegalArgumentException("R930_COUNTERFACTUAL_NOT_RECOMPUTED");
        return new CounterfactualRow(candidate, issue, intervention, confound,
                baseline, changed);
    }

    public static final class RepairEdit {
        public final RepairKind kind;
        public final String nodeId;
        public final String replacementNodeHash;
        public final int integerValue;
        public final double numericValue;
        public final String editHash;

        public RepairEdit(RepairKind kind, String nodeId, String replacementNodeHash,
                int integerValue, double numericValue) {
            if (kind == null || integerValue < 0 || !finite(numericValue))
                throw new IllegalArgumentException("R930_REPAIR_EDIT_INVALID");
            this.kind = kind;
            this.nodeId = required(nodeId, "REPAIR_NODE");
            this.replacementNodeHash = clean(replacementNodeHash).length() == 0 ? ""
                    : requireHash(replacementNodeHash, "REPAIR_REPLACEMENT_HASH");
            this.integerValue = integerValue;
            this.numericValue = numericValue;
            this.editHash = sha256(kind.name() + '|' + this.nodeId + '|'
                    + this.replacementNodeHash + '|' + integerValue + '|'
                    + Long.toHexString(Double.doubleToLongBits(numericValue)));
        }
    }

    public static final class RepairChild {
        public final CandidateDefinition parent;
        public final CandidateDefinition child;
        public final List<RepairEdit> edits;
        public final String hypothesisId;
        public final String repairHash;

        public RepairChild(CandidateDefinition parent, CandidateDefinition child,
                List<RepairEdit> edits, String hypothesisId) {
            if (parent == null || child == null || edits == null || edits.isEmpty()
                    || edits.size() > 4
                    || !parent.structuralHash.equals(child.parentCandidateHash))
                throw new IllegalArgumentException("R930_REPAIR_CHILD_INVALID");
            this.parent = parent;
            this.child = child;
            this.edits = Collections.unmodifiableList(new ArrayList<RepairEdit>(edits));
            this.hypothesisId = required(hypothesisId, "REPAIR_HYPOTHESIS_ID");
            StringBuilder canonical = new StringBuilder(parent.structuralHash).append('|')
                    .append(child.structuralHash).append('|').append(this.hypothesisId);
            for (RepairEdit edit : this.edits) canonical.append('|').append(edit.editHash);
            this.repairHash = sha256(canonical.toString());
        }
    }

    public static final class RepairScore {
        public final RepairChild repair;
        public final int pairedN;
        public final double parentLoss;
        public final double childLoss;
        public final double lossImprovement;
        public final double childCoverage;
        public final long childP95CostNs;
        public final boolean feasible;
        public final String reason;
        public final String scoreHash;

        RepairScore(RepairChild repair, int pairedN, double parentLoss, double childLoss,
                double childCoverage, long childP95CostNs, boolean feasible, String reason,
                String evidence) {
            this.repair = repair;
            this.pairedN = pairedN;
            this.parentLoss = parentLoss;
            this.childLoss = childLoss;
            this.lossImprovement = parentLoss - childLoss;
            this.childCoverage = childCoverage;
            this.childP95CostNs = childP95CostNs;
            this.feasible = feasible;
            this.reason = reason;
            this.scoreHash = sha256(repair.repairHash + '|' + pairedN + '|' + parentLoss + '|'
                    + childLoss + '|' + childCoverage + '|' + childP95CostNs + '|'
                    + (feasible ? 1 : 0) + '|' + reason + '|' + evidence);
        }
    }

    public static RepairScore scoreRepair(RepairChild repair, List<IssueCase> cases,
            Evaluator evaluator, long commonCostBudgetNs) {
        if (repair == null || cases == null || cases.isEmpty() || evaluator == null
                || commonCostBudgetNs <= 0L)
            throw new IllegalArgumentException("R930_REPAIR_SCORE_INPUT");
        Set<String> issues = new HashSet<String>();
        double parentLoss = 0.0d, childLoss = 0.0d;
        int parentSelected = 0, childSelected = 0;
        long[] costs = new long[cases.size()];
        StringBuilder evidence = new StringBuilder(repair.repairHash);
        for (int i = 0; i < cases.size(); i++) {
            IssueCase issue = cases.get(i);
            if (issue == null || !issues.add(issue.targetIssue))
                throw new IllegalArgumentException("R930_REPAIR_CASE_DUPLICATE");
            EvaluationOutput parent = evaluator.evaluate(repair.parent, issue,
                    Collections.<String>emptySet(), Collections.<Intervention>emptyList());
            EvaluationOutput child = evaluator.evaluate(repair.child, issue,
                    Collections.<String>emptySet(), Collections.<Intervention>emptyList());
            validatePaired(repair.parent, issue, parent, parent);
            validatePaired(repair.child, issue, child, child);
            if (parent.selected) { parentLoss += parent.nll(); parentSelected++; }
            else parentLoss += 1.0d;
            if (child.selected) { childLoss += child.nll(); childSelected++; }
            else childLoss += 1.0d;
            costs[i] = child.costNs;
            evidence.append('|').append(issue.caseHash).append(':')
                    .append(parent.executionHash).append(':').append(child.executionHash);
        }
        Arrays.sort(costs);
        int n = cases.size();
        double parentMean = parentLoss / n;
        double childMean = childLoss / n;
        double coverage = childSelected / (double) n;
        long p95 = costs[Math.max(0, (int) Math.ceil(costs.length * 0.95d) - 1)];
        boolean feasible = n >= 48 && childMean < parentMean && coverage >= 0.15d
                && p95 <= commonCostBudgetNs;
        String reason = n < 48 ? "SUPPORT_PENDING"
                : childMean >= parentMean ? "NO_LOSS_IMPROVEMENT"
                : coverage < 0.15d ? "COVERAGE_BELOW_EFFECT_TARGET"
                : p95 > commonCostBudgetNs ? "RESOURCE_BUDGET_FAIL" : "REPAIR_FEASIBLE_RETEST_FUTURE";
        return new RepairScore(repair, n, parentMean, childMean, coverage, p95,
                feasible, reason, sha256(evidence.toString()));
    }

    /** Chooses the smallest feasible edit set, then largest loss gain; no unregistered retry. */
    public static RepairScore chooseMinimalRepair(List<RepairScore> scores) {
        if (scores == null || scores.isEmpty()) return null;
        ArrayList<RepairScore> feasible = new ArrayList<RepairScore>();
        for (RepairScore score : scores) if (score != null && score.feasible) feasible.add(score);
        if (feasible.isEmpty()) return null;
        Collections.sort(feasible, new Comparator<RepairScore>() {
            @Override public int compare(RepairScore left, RepairScore right) {
                int byEdits = Integer.compare(left.repair.edits.size(), right.repair.edits.size());
                if (byEdits != 0) return byEdits;
                int byGain = Double.compare(right.lossImprovement, left.lossImprovement);
                if (byGain != 0) return byGain;
                return left.repair.repairHash.compareTo(right.repair.repairHash);
            }
        });
        return feasible.get(0);
    }

    public static final class SearchProposal {
        public final String proposalId;
        public final String familyId;
        public final List<String> parentNodeHashes;
        public final TypedAtomMoleculeGraphR930.Operator operator;
        public final double[] parameters;
        public final String targetSemantic;
        public final String structuralHash;
        public final String noveltyKey;

        SearchProposal(String proposalId, String familyId, List<String> parentNodeHashes,
                TypedAtomMoleculeGraphR930.Operator operator, double[] parameters,
                String targetSemantic) {
            this.proposalId = proposalId;
            this.familyId = familyId;
            this.parentNodeHashes = Collections.unmodifiableList(
                    new ArrayList<String>(parentNodeHashes));
            this.operator = operator;
            this.parameters = Arrays.copyOf(parameters, parameters.length);
            this.targetSemantic = targetSemantic;
            StringBuilder canonical = new StringBuilder(familyId).append('|')
                    .append(operator.name()).append('|').append(targetSemantic);
            for (String parent : this.parentNodeHashes) canonical.append('|').append(parent);
            for (double value : this.parameters) canonical.append('|').append(value);
            this.structuralHash = sha256(canonical.toString());
            this.noveltyKey = sha256(operator.name() + '|' + sortedJoin(parentNodeHashes));
        }
    }

    /**
     * Deterministic grammar search over admitted predraw nodes. Existing structural hashes and
     * already-consumed novelty keys are excluded before any expensive evaluation.
     */
    public static List<SearchProposal> createNovelStructures(String familyId,
            List<TypedAtomMoleculeGraphR930.Admission> admissions,
            Set<String> existingStructuralHashes, Set<String> consumedNoveltyKeys,
            int maximumProposals, long seed) {
        String family = required(familyId, "SEARCH_FAMILY");
        if (admissions == null || maximumProposals <= 0 || maximumProposals > 512)
            throw new IllegalArgumentException("R930_SEARCH_INPUT_INVALID");
        ArrayList<TypedAtomMoleculeGraphR930.Admission> active =
                new ArrayList<TypedAtomMoleculeGraphR930.Admission>();
        for (TypedAtomMoleculeGraphR930.Admission admission : admissions)
            if (admission != null && admission.productionEligible
                    && admission.state == TypedAtomMoleculeGraphR930.AdmissionState.ACTIVE)
                active.add(admission);
        Collections.sort(active, new Comparator<TypedAtomMoleculeGraphR930.Admission>() {
            @Override public int compare(TypedAtomMoleculeGraphR930.Admission left,
                    TypedAtomMoleculeGraphR930.Admission right) {
                int byQuality = Double.compare(right.quality.meanQuality,
                        left.quality.meanQuality);
                if (byQuality != 0) return byQuality;
                return left.node.structuralHash.compareTo(right.node.structuralHash);
            }
        });
        Set<String> structural = existingStructuralHashes == null
                ? Collections.<String>emptySet() : existingStructuralHashes;
        Set<String> novelty = consumedNoveltyKeys == null
                ? Collections.<String>emptySet() : consumedNoveltyKeys;
        TypedAtomMoleculeGraphR930.Operator[] operators = new TypedAtomMoleculeGraphR930.Operator[] {
            TypedAtomMoleculeGraphR930.Operator.RESIDUAL,
            TypedAtomMoleculeGraphR930.Operator.ABS_DIFFERENCE,
            TypedAtomMoleculeGraphR930.Operator.SAFE_DIVIDE,
            TypedAtomMoleculeGraphR930.Operator.SIGNED_PRODUCT,
            TypedAtomMoleculeGraphR930.Operator.ORTHOGONAL_RESIDUAL,
            TypedAtomMoleculeGraphR930.Operator.INTERACTION3
        };
        ArrayList<SearchProposal> proposals = new ArrayList<SearchProposal>();
        LinkedHashSet<String> emitted = new LinkedHashSet<String>();
        int limit = Math.min(active.size(), 64);
        long cursor = seed;
        for (int left = 0; left < limit && proposals.size() < maximumProposals; left++) {
            for (int right = left + 1; right < limit && proposals.size() < maximumProposals; right++) {
                cursor = mix64(cursor + left * 131L + right * 8191L);
                TypedAtomMoleculeGraphR930.Operator operator = operators[
                        (int) Math.floorMod(cursor, operators.length)];
                int arity = operator == TypedAtomMoleculeGraphR930.Operator.INTERACTION3 ? 3 : 2;
                if (arity == 3 && right + 1 >= limit) continue;
                ArrayList<String> parents = new ArrayList<String>();
                parents.add(active.get(left).node.structuralHash);
                parents.add(active.get(right).node.structuralHash);
                if (arity == 3) parents.add(active.get(right + 1).node.structuralHash);
                double[] parameters = operator == TypedAtomMoleculeGraphR930.Operator.ORTHOGONAL_RESIDUAL
                        ? new double[] {0.50d} : new double[0];
                SearchProposal proposal = new SearchProposal(
                        "R930-" + family + '-' + String.format(java.util.Locale.US, "%04d",
                                proposals.size() + 1), family, parents, operator, parameters,
                        "RESIDUAL_COMPLEMENT_TO_CURRENT_MAINLINE");
                if (structural.contains(proposal.structuralHash)
                        || novelty.contains(proposal.noveltyKey)
                        || !emitted.add(proposal.structuralHash)) continue;
                proposals.add(proposal);
            }
        }
        return Collections.unmodifiableList(proposals);
    }

    public static final class DiversitySignature {
        public final String structuralHash;
        public final String behaviorHash;
        public final String residualHash;
        public final double residualComplementarity;
        public final String signatureHash;

        public DiversitySignature(String structuralHash, String behaviorHash,
                String residualHash, double residualComplementarity) {
            this.structuralHash = requireHash(structuralHash, "DIVERSITY_STRUCTURAL_HASH");
            this.behaviorHash = requireHash(behaviorHash, "DIVERSITY_BEHAVIOR_HASH");
            this.residualHash = requireHash(residualHash, "DIVERSITY_RESIDUAL_HASH");
            if (!unit(residualComplementarity))
                throw new IllegalArgumentException("R930_DIVERSITY_COMPLEMENTARITY");
            this.residualComplementarity = residualComplementarity;
            this.signatureHash = sha256(this.structuralHash + '|' + this.behaviorHash + '|'
                    + this.residualHash + '|' + residualComplementarity);
        }
    }

    public static boolean genuinelyNovel(DiversitySignature proposal,
            List<DiversitySignature> existing, double minimumComplementarity) {
        if (proposal == null || !unit(minimumComplementarity))
            throw new IllegalArgumentException("R930_NOVELTY_INPUT");
        if (proposal.residualComplementarity < minimumComplementarity) return false;
        if (existing == null) return true;
        for (DiversitySignature value : existing) {
            if (value == null) continue;
            if (proposal.structuralHash.equals(value.structuralHash)
                    || proposal.behaviorHash.equals(value.behaviorHash)
                    || proposal.residualHash.equals(value.residualHash)) return false;
        }
        return true;
    }

    private static void validatePaired(CandidateDefinition candidate, IssueCase issue,
            EvaluationOutput left, EvaluationOutput right) {
        if (left == null || right == null
                || !issue.targetIssue.equals(left.targetIssue)
                || !issue.targetIssue.equals(right.targetIssue)
                || !candidate.structuralHash.equals(left.candidateHash)
                || !candidate.structuralHash.equals(right.candidateHash)
                || !issue.inputHash.equals(left.inputHash)
                || !issue.inputHash.equals(right.inputHash))
            throw new IllegalArgumentException("R930_EVALUATION_NOT_PAIRED");
    }

    private static boolean bothSelected(EvaluationOutput left, EvaluationOutput right) {
        return left.selected && right.selected;
    }

    /** Exact two-sided sign-test tail for n <= 1024; conservative normal tail beyond it. */
    private static double twoSidedBinomialP(int positive, int n) {
        if (n <= 0 || positive < 0 || positive > n) return 1.0d;
        int extreme = Math.min(positive, n - positive);
        if (n <= 1024) {
            double probability = Math.pow(0.5d, n);
            double term = probability;
            double tail = term;
            for (int k = 1; k <= extreme; k++) {
                term *= (n - k + 1) / (double) k;
                tail += term;
            }
            return Math.min(1.0d, 2.0d * tail);
        }
        double z = Math.abs(positive - n * 0.5d) / Math.sqrt(n * 0.25d);
        return Math.min(1.0d, 2.0d * normalUpperTail(z));
    }

    private static double normalUpperTail(double z) {
        double t = 1.0d / (1.0d + 0.2316419d * z);
        double density = 0.3989422804014327d * Math.exp(-0.5d * z * z);
        return density * t * (0.319381530d + t * (-0.356563782d
                + t * (1.781477937d + t * (-1.821255978d + t * 1.330274429d))));
    }

    private static String sortedJoin(List<String> values) {
        ArrayList<String> copy = new ArrayList<String>(values);
        Collections.sort(copy);
        StringBuilder out = new StringBuilder();
        for (String value : copy) out.append('|').append(value);
        return out.toString();
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
    private static boolean probability(double value) { return unit(value); }
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
