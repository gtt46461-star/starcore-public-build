package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public final class Batch4ContractsR930Test {
    private static final String H0 = TypedAtomMoleculeGraphR930.sha256("h0");
    private static final String H1 = TypedAtomMoleculeGraphR930.sha256("h1");
    private static final String H2 = TypedAtomMoleculeGraphR930.sha256("h2");
    private static final String H3 = TypedAtomMoleculeGraphR930.sha256("h3");
    private static final String H4 = TypedAtomMoleculeGraphR930.sha256("h4");
    private static final String H5 = TypedAtomMoleculeGraphR930.sha256("h5");
    private static final String H6 = TypedAtomMoleculeGraphR930.sha256("h6");
    private static final String H7 = TypedAtomMoleculeGraphR930.sha256("h7");

    @Test public void typedGraphRejectsPostdrawParentForPredrawMolecule() {
        TypedAtomMoleculeGraphR930.NodeSpec label = atom("label",
                TypedAtomMoleculeGraphR930.Stage.POSTDRAW_LABEL, false);
        TypedAtomMoleculeGraphR930.NodeSpec molecule = new TypedAtomMoleculeGraphR930.NodeSpec(
                TypedAtomMoleculeGraphR930.NEW_NAMESPACE, "bad", "bad-leak",
                TypedAtomMoleculeGraphR930.NodeKind.MOLECULE,
                TypedAtomMoleculeGraphR930.ValueType.DOUBLE,
                new TypedAtomMoleculeGraphR930.Shape(1, 1),
                TypedAtomMoleculeGraphR930.Stage.FROZEN_PREDRAW, "ratio", 1, 12,
                TypedAtomMoleculeGraphR930.MissingPolicy.REJECT, "graph", "predraw",
                1_000_000L, Collections.singletonList(label.qualifiedId()),
                TypedAtomMoleculeGraphR930.Operator.IDENTITY, null, false, "1");
        try {
            new TypedAtomMoleculeGraphR930.Graph(PitFeatureDagR929.GAME,
                    "FROZEN_PREDRAW", "1", Arrays.asList(label, molecule));
            fail("postdraw parent must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("POSTDRAW_TO_PREDRAW_LEAK"));
        }
    }

    @Test public void typedGraphEvaluatesRealCompositeAndPreservesLegacyIdentity() {
        TypedAtomMoleculeGraphR930.NodeSpec a = atom("a",
                TypedAtomMoleculeGraphR930.Stage.FROZEN_PREDRAW, false);
        TypedAtomMoleculeGraphR930.NodeSpec b = atom("b",
                TypedAtomMoleculeGraphR930.Stage.FROZEN_PREDRAW, false);
        TypedAtomMoleculeGraphR930.NodeSpec factor = new TypedAtomMoleculeGraphR930.NodeSpec(
                TypedAtomMoleculeGraphR930.NEW_NAMESPACE, "residual", "a minus b",
                TypedAtomMoleculeGraphR930.NodeKind.FACTOR,
                TypedAtomMoleculeGraphR930.ValueType.DOUBLE,
                new TypedAtomMoleculeGraphR930.Shape(1, 1),
                TypedAtomMoleculeGraphR930.Stage.FROZEN_PREDRAW, "ratio", 1, 12,
                TypedAtomMoleculeGraphR930.MissingPolicy.REJECT, "graph", "predraw",
                1_000_000L, Arrays.asList(a.qualifiedId(), b.qualifiedId()),
                TypedAtomMoleculeGraphR930.Operator.RESIDUAL, null, false, "1");
        TypedAtomMoleculeGraphR930.Graph graph = new TypedAtomMoleculeGraphR930.Graph(
                PitFeatureDagR929.GAME, "FROZEN_PREDRAW", "1", Arrays.asList(a, b, factor));
        Map<String, TypedAtomMoleculeGraphR930.Value> values =
                new LinkedHashMap<String, TypedAtomMoleculeGraphR930.Value>();
        values.put(a.qualifiedId(), TypedAtomMoleculeGraphR930.Value.present(
                new double[] {0.75d}, "100", 10L, 1L, H0));
        values.put(b.qualifiedId(), TypedAtomMoleculeGraphR930.Value.present(
                new double[] {0.25d}, "100", 10L, 1L, H1));
        // This numerical fixture measures an initialized operator. Initialize the real
        // enum dispatch before the timed graph evaluation; retain its 1 ms node budget
        // and the production four-times hard limit, with no fake clock or retry.
        assertEquals(0.50d, TypedAtomMoleculeGraphR930.scalarOperation(
                factor, new double[] {0.75d, 0.25d}), 1.0e-12d);
        TypedAtomMoleculeGraphR930.Evaluation evaluation =
                TypedAtomMoleculeGraphR930.evaluate(graph,
                        new TypedAtomMoleculeGraphR930.FrozenInputs("101", 10L, H2, values));
        assertEquals(0.50d, evaluation.require(factor.qualifiedId()).get(0), 1.0e-12d);
        List<TypedAtomMoleculeGraphR930.NodeSpec> legacy =
                TypedAtomMoleculeGraphR930.protectedLegacyGenesisNodes();
        assertFalse(legacy.isEmpty());
        for (TypedAtomMoleculeGraphR930.NodeSpec node : legacy) {
            assertTrue(node.legacyProtected);
            assertEquals(TypedAtomMoleculeGraphR930.LEGACY_NAMESPACE, node.namespace);
        }
    }

    @Test public void typedGraphEnforcesOriginalHardCostBoundary() {
        TypedAtomMoleculeGraphR930.NodeSpec a = atom("cost-boundary",
                TypedAtomMoleculeGraphR930.Stage.FROZEN_PREDRAW, false);
        TypedAtomMoleculeGraphR930.Graph graph = new TypedAtomMoleculeGraphR930.Graph(
                PitFeatureDagR929.GAME, "FROZEN_PREDRAW", "1", Collections.singletonList(a));
        Map<String, TypedAtomMoleculeGraphR930.Value> values = new LinkedHashMap<>();
        long hardLimit = a.p95CostBudgetNs * 4L;
        assertEquals(4_000_000L, hardLimit);
        values.put(a.qualifiedId(), TypedAtomMoleculeGraphR930.Value.present(
                new double[] {0.75d}, "100", 10L, hardLimit, H0));
        assertEquals(0.75d, TypedAtomMoleculeGraphR930.evaluate(graph,
                new TypedAtomMoleculeGraphR930.FrozenInputs("101", 10L, H2, values))
                .require(a.qualifiedId()).get(0), 1.0e-12d);
        values.put(a.qualifiedId(), TypedAtomMoleculeGraphR930.Value.present(
                new double[] {0.75d}, "100", 10L, hardLimit + 1L, H0));
        try {
            TypedAtomMoleculeGraphR930.evaluate(graph,
                    new TypedAtomMoleculeGraphR930.FrozenInputs("101", 10L, H2, values));
            fail("Original hard cost limit was bypassed");
        } catch (IllegalStateException expected) {
            assertEquals("R930_NODE_HARD_COST_MULTIPLIER:cost-boundary", expected.getMessage());
        }
    }

    @Test public void matrixSnapshotRequiresOneFreezeAndFeedbackIsDelayed() {
        MatrixSnapshotFeedbackR930.CaptureBundle capture = capture("101", "100", 10L, H0, H1);
        MatrixSnapshotFeedbackR930.FrozenMatrix teacher = matrix(
                MatrixSnapshotFeedbackR930.MatrixKind.EVIDENCE_TEACHER,
                "101", "100", 10L, H0, H1);
        MatrixSnapshotFeedbackR930.FrozenMatrix forward = matrix(
                MatrixSnapshotFeedbackR930.MatrixKind.FORWARD_REASONING,
                "101", "100", 10L, H0, H1);
        MatrixSnapshotFeedbackR930.CommonSnapshot snapshot =
                new MatrixSnapshotFeedbackR930.CommonSnapshot("101", "100", 10L,
                        H0, H1, H2, capture, Arrays.asList(teacher, forward),
                        new HashSet<MatrixSnapshotFeedbackR930.MatrixKind>(Arrays.asList(
                                MatrixSnapshotFeedbackR930.MatrixKind.EVIDENCE_TEACHER,
                                MatrixSnapshotFeedbackR930.MatrixKind.FORWARD_REASONING)));
        MatrixSnapshotFeedbackR930.SettlementLabel label =
                new MatrixSnapshotFeedbackR930.SettlementLabel("101",
                        new int[] {1,2,3,4,5,6,7,8,9,10}, 20L, 21L, "fixture", "v1");
        try {
            MatrixSnapshotFeedbackR930.settle(snapshot, label, "101");
            fail("same issue feedback must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("NOT_DELAYED"));
        }
        MatrixSnapshotFeedbackR930.FeedbackDelta delta =
                MatrixSnapshotFeedbackR930.settle(snapshot, label, "102");
        MatrixSnapshotFeedbackR930.FeedbackState state =
                MatrixSnapshotFeedbackR930.FeedbackState.initial("state").update(
                        delta, 0.20d, 0.02d);
        assertEquals("101", state.trainedUntilIssue);
        assertTrue(Double.isFinite(state.correction(0, 0, "102")));
    }

    @Test public void matrixModelIdentityDriftIsRejectedBeforeSettlement() {
        MatrixSnapshotFeedbackR930.CaptureBundle capture = capture("101","100",10L,H0,H1);
        MatrixSnapshotFeedbackR930.FrozenMatrix teacher = matrix(
                MatrixSnapshotFeedbackR930.MatrixKind.EVIDENCE_TEACHER,"101","100",10L,H0,H1,H3);
        try {
            new MatrixSnapshotFeedbackR930.CommonSnapshot("101","100",10L,H0,H1,H2,
                    capture,Arrays.asList(teacher),new HashSet<MatrixSnapshotFeedbackR930.MatrixKind>(
                            Arrays.asList(MatrixSnapshotFeedbackR930.MatrixKind.EVIDENCE_TEACHER)));
            fail("different frozen model accepted");
        } catch(IllegalArgumentException expected) {
            assertEquals("R930_COMMON_MATRIX_FREEZE_DRIFT",expected.getMessage());
        }
    }

    @Test public void ablationMustActuallyRecomputeAndSummaryIsPaired() {
        final AblationRepairCreationR930.CandidateDefinition candidate = candidate("candidate");
        AblationRepairCreationR930.Evaluator evaluator =
                new AblationRepairCreationR930.Evaluator() {
                    @Override public AblationRepairCreationR930.EvaluationOutput evaluate(
                            AblationRepairCreationR930.CandidateDefinition definition,
                            AblationRepairCreationR930.IssueCase issue,
                            Set<String> disabled, List<AblationRepairCreationR930.Intervention> interventions) {
                        boolean removed = disabled.contains("node-a");
                        return new AblationRepairCreationR930.EvaluationOutput(issue.targetIssue,
                                definition.structuralHash, removed ? H3 : H4,
                                removed ? 0.20d : 0.60d, removed ? 0.1d : 0.9d,
                                true, issue.hit, removed ? 10L : 20L, issue.inputHash,
                                AblationRepairCreationR930.sha256(issue.targetIssue + "|" + removed));
                    }
                };
        List<AblationRepairCreationR930.AblationRow> rows =
                new ArrayList<AblationRepairCreationR930.AblationRow>();
        for (int i = 0; i < 120; i++) {
            String issue = String.format("%03d", 101 + i);
            rows.add(AblationRepairCreationR930.ablate(candidate,
                    new AblationRepairCreationR930.IssueCase(issue,
                            String.format("%03d", 100 + i), 10L + i,
                            AblationRepairCreationR930.sha256("input" + i),
                            AblationRepairCreationR930.sha256("label" + i),
                            true, true, "global"), "node-a", evaluator));
        }
        AblationRepairCreationR930.AblationSummary summary =
                AblationRepairCreationR930.summarize(rows);
        assertEquals(120, summary.pairedN);
        assertTrue(summary.meanBrierIncreaseWhenRemoved > 0.0d);
        assertTrue(summary.evidenceOfUsefulContribution());
    }

    @Test public void schedulerEnforcesEightTwentyFourSixtyFourAndFamilyFairness() {
        List<ResearchSchedulerLifecycleR930.ResearchTask> tasks =
                new ArrayList<ResearchSchedulerLifecycleR930.ResearchTask>();
        int id = 0;
        for (ResearchSchedulerLifecycleR930.Tier tier
                : ResearchSchedulerLifecycleR930.Tier.values()) {
            for (int i = 0; i < 100; i++) {
                tasks.add(new ResearchSchedulerLifecycleR930.ResearchTask("t" + id,
                        ResearchSchedulerLifecycleR930.sha256("c" + id), "f" + (i % 5),
                        tier, 100 - i, "101", H0, H1, "exp" + id,
                        Math.min(1_000_000L, tier.maximumTaskCostNs), 0));
                id++;
            }
        }
        ResearchSchedulerLifecycleR930.WavePlan plan =
                ResearchSchedulerLifecycleR930.planWave("101", tasks);
        assertEquals(8, plan.elite.size());
        assertEquals(24, plan.challenger.size());
        assertEquals(64, plan.nursery.size());
        assertEquals(5, plan.distinctFamilies);
    }

    @Test public void lifecycleConsumesEachWindowOnlyOnceAndDoesNotJumpLevels() {
        String candidate = ResearchSchedulerLifecycleR930.sha256("candidate");
        ResearchSchedulerLifecycleR930.LifecycleLedger ledger =
                ResearchSchedulerLifecycleR930.LifecycleLedger.register(candidate, 1L);
        ResearchSchedulerLifecycleR930.WindowEvidence w24 = window(candidate, "w24", 24, 10, 5);
        ledger = ledger.apply(w24, 2L);
        assertEquals(ResearchSchedulerLifecycleR930.LifecycleState.OBSERVING, ledger.state);
        try {
            ledger.apply(w24, 3L);
            fail("same content window must be one-shot");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("ALREADY_CONSUMED"));
        }
        ResearchSchedulerLifecycleR930.WindowEvidence w48 = window(candidate, "w48", 48, 20, 10);
        ledger = ledger.apply(w48, 4L);
        assertEquals(ResearchSchedulerLifecycleR930.LifecycleState.EVIDENCE_READY, ledger.state);
    }

    @Test public void dedupeRejectsBehaviorCloneAndQuarantineIsOnlyDependencyClosure() {
        CandidateDiversityContributionR930.Signature a = signature("a", H0, H1, H2,
                new double[] {1,0,-1}, new double[] {1,0,-1});
        CandidateDiversityContributionR930.Signature b = signature("b", H3, H1, H4,
                new double[] {1,0,-1}, new double[] {-1,0,1});
        CandidateDiversityContributionR930.DedupeDecision decision =
                CandidateDiversityContributionR930.dedupe(b, Collections.singletonList(a));
        assertFalse(decision.admit);
        assertEquals(CandidateDiversityContributionR930.SimilarityClass.BEHAVIOR_DUPLICATE,
                decision.nearest.classification);

        List<CandidateDiversityContributionR930.ContributionNode> nodes = Arrays.asList(
                node("production", CandidateDiversityContributionR930.NodeType.POLICY),
                node("research", CandidateDiversityContributionR930.NodeType.CANDIDATE),
                node("blend", CandidateDiversityContributionR930.NodeType.CONTRIBUTION),
                node("selection", CandidateDiversityContributionR930.NodeType.SELECTION));
        List<CandidateDiversityContributionR930.ContributionEdge> edges = Arrays.asList(
                new CandidateDiversityContributionR930.ContributionEdge(
                        "production", "selection", "baseline", 0.0d, ""),
                new CandidateDiversityContributionR930.ContributionEdge(
                        "research", "blend", "research", 0.05d, H5),
                new CandidateDiversityContributionR930.ContributionEdge(
                        "blend", "selection", "bounded", 0.05d, H5));
        CandidateDiversityContributionR930.ContributionGraph graph =
                new CandidateDiversityContributionR930.ContributionGraph(nodes, edges);
        Set<String> closure = graph.quarantineClosure("research");
        assertTrue(closure.contains("research"));
        assertTrue(closure.contains("blend"));
        assertTrue(closure.contains("selection"));
        assertFalse(closure.contains("production"));
    }

    @Test public void frontierNeverCreatesFrequencyDebtAndNeedsIndependentFuture() {
        FrequencyQualityFrontierR930.ThresholdPolicy policy =
                new FrequencyQualityFrontierR930.ThresholdPolicy("p", 0.40d, 0.60d,
                        0.50d, 0.60d, 0.60d, 5000L, 30, H0, H1, H2, H3);
        List<FrequencyQualityFrontierR930.Observation> rows =
                new ArrayList<FrequencyQualityFrontierR930.Observation>();
        for (int i = 0; i < 120; i++) {
            boolean selectedLike = i < 24;
            rows.add(new FrequencyQualityFrontierR930.Observation(
                    String.format("%03d", 101 + i), String.format("%03d", 100 + i),
                    true, selectedLike ? 0.42d : 0.20d, 0.20d, 0.80d,
                    0.10d, 0.10d, 200, 100L, selectedLike && i % 2 == 0,
                    H0, H1, H2, FrequencyQualityFrontierR930.sha256("input" + i)));
        }
        String content = FrequencyQualityFrontierR930.contentHash(
                FrequencyQualityFrontierR930.WindowRole.SEALED_FUTURE, rows);
        FrequencyQualityFrontierR930.Metrics metrics =
                FrequencyQualityFrontierR930.evaluate(policy, rows,
                        FrequencyQualityFrontierR930.WindowRole.SEALED_FUTURE,
                        content, 10);
        assertEquals(0.20d, metrics.coverage, 1.0e-12d);
        FrequencyQualityFrontierR930.FrontierResult frontier =
                FrequencyQualityFrontierR930.frontier(Collections.singletonList(metrics), 5000L);
        assertEquals(FrequencyQualityFrontierR930.Status.NOT_PROVEN, frontier.status);
        assertFalse(FrequencyQualityFrontierR930.FREQUENCY_DEBT_ALLOWED);
        assertFalse(FrequencyQualityFrontierR930.MAY_CREATE_REAL);
    }

    private static TypedAtomMoleculeGraphR930.NodeSpec atom(String id,
            TypedAtomMoleculeGraphR930.Stage stage, boolean legacy) {
        return new TypedAtomMoleculeGraphR930.NodeSpec(
                legacy ? TypedAtomMoleculeGraphR930.LEGACY_NAMESPACE
                        : TypedAtomMoleculeGraphR930.NEW_NAMESPACE,
                id, id, TypedAtomMoleculeGraphR930.NodeKind.ATOM,
                TypedAtomMoleculeGraphR930.ValueType.DOUBLE,
                new TypedAtomMoleculeGraphR930.Shape(1, 1), stage, "ratio", 1, 12,
                TypedAtomMoleculeGraphR930.MissingPolicy.REJECT, "fixture", "predraw",
                1_000_000L, Collections.<String>emptyList(), null, null, legacy, "1");
    }

    private static MatrixSnapshotFeedbackR930.CaptureBundle capture(String target,
            String source, long frozenAt, String freezeHash, String inputHash) {
        List<MatrixSnapshotFeedbackR930.CaptureRecord> records =
                new ArrayList<MatrixSnapshotFeedbackR930.CaptureRecord>();
        for (MatrixSnapshotFeedbackR930.CaptureStage stage
                : MatrixSnapshotFeedbackR930.CaptureStage.values()) {
            int expected = stage == MatrixSnapshotFeedbackR930.CaptureStage.S3_MATRIX_SURFACE
                    ? 100 : stage == MatrixSnapshotFeedbackR930.CaptureStage.S4_SELECTION_ENVELOPE
                    ? 1200 : 10;
            records.add(new MatrixSnapshotFeedbackR930.CaptureRecord(stage, target, source,
                    frozenAt, freezeHash, inputHash, expected, expected, 0, 1L,
                    MatrixSnapshotFeedbackR930.sha256(stage.name())));
        }
        return new MatrixSnapshotFeedbackR930.CaptureBundle(records);
    }

    private static MatrixSnapshotFeedbackR930.FrozenMatrix matrix(
            MatrixSnapshotFeedbackR930.MatrixKind kind, String target, String source,
            long frozenAt, String graphHash, String inputHash) {
        return matrix(kind,target,source,frozenAt,graphHash,inputHash,H2);
    }
    private static MatrixSnapshotFeedbackR930.FrozenMatrix matrix(
            MatrixSnapshotFeedbackR930.MatrixKind kind, String target, String source,
            long frozenAt, String graphHash, String inputHash, String modelHash) {
        List<MatrixSnapshotFeedbackR930.MatrixCell> cells =
                new ArrayList<MatrixSnapshotFeedbackR930.MatrixCell>();
        for (int row = 0; row < 10; row++) for (int column = 0; column < 10; column++)
            cells.add(new MatrixSnapshotFeedbackR930.MatrixCell(row, column, true,
                    0.1d, 120, 0.8d, 1L, true,
                    MatrixSnapshotFeedbackR930.sha256(kind.name() + row + column)));
        return new MatrixSnapshotFeedbackR930.FrozenMatrix(kind, target, source, frozenAt,
                "FROZEN_PREDRAW", graphHash, inputHash,
                modelHash, cells);
    }

    private static AblationRepairCreationR930.CandidateDefinition candidate(String id) {
        return new AblationRepairCreationR930.CandidateDefinition(id, "family", H0,
                Arrays.asList(H1, H2), "output", "FROZEN_PREDRAW", 10, 1000L,
                "", "fixture");
    }

    private static ResearchSchedulerLifecycleR930.WindowEvidence window(String candidate,
            String id, int planned, int selected, int hits) {
        return new ResearchSchedulerLifecycleR930.WindowEvidence(id, "001", "999",
                ResearchSchedulerLifecycleR930.sha256(id), H0, candidate, planned, planned,
                selected, hits, planned, Math.max(0, hits - 1), 0.19d, 0.03d,
                0.80d, 1000L, 0.01d, true);
    }

    private static CandidateDiversityContributionR930.Signature signature(String id,
            String structural, String behavior, String residual,
            double[] behaviorVector, double[] residualVector) {
        return new CandidateDiversityContributionR930.Signature(id, "family", structural,
                behavior, residual, behaviorVector, residualVector, 120,
                0.42d, 0.18d, 0.03d, 0.80d, 1000L);
    }

    private static CandidateDiversityContributionR930.ContributionNode node(String id,
            CandidateDiversityContributionR930.NodeType type) {
        return new CandidateDiversityContributionR930.ContributionNode(id, type,
                CandidateDiversityContributionR930.sha256("artifact" + id),
                CandidateDiversityContributionR930.NodeState.ACTIVE, "fixture");
    }
}
