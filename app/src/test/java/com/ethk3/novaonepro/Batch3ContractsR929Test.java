package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public final class Batch3ContractsR929Test {
    private static final String H0 = PitFeatureDagR929.sha256("h0");
    private static final String H1 = PitFeatureDagR929.sha256("h1");
    private static final String H2 = PitFeatureDagR929.sha256("h2");
    private static final String H3 = PitFeatureDagR929.sha256("h3");
    private static final String H4 = PitFeatureDagR929.sha256("h4");
    private static final String H5 = PitFeatureDagR929.sha256("h5");
    private static final String H6 = PitFeatureDagR929.sha256("h6");
    private static final String H7 = PitFeatureDagR929.sha256("h7");
    private static final String H8 = PitFeatureDagR929.sha256("h8");
    private static final String H9 = PitFeatureDagR929.sha256("h9");

    @Test public void legalZeroIsPresentAndMissingIsDistinct() {
        PitFeatureDagR929.ValueResult zero = PitFeatureDagR929.ValueResult.present(
                new double[] {0.0d}, "100", 10L, 1L, H0);
        assertTrue(zero.isPresent(0));
        assertEquals(PitFeatureDagR929.MissingReason.NONE, zero.missingReason(0));
        assertEquals(0.0d, zero.value(0), 0.0d);

        PitFeatureDagR929.ValueResult missing = PitFeatureDagR929.ValueResult.missing(
                1, PitFeatureDagR929.MissingReason.SOURCE_ABSENT,
                "100", 10L, 1L, H0);
        assertFalse(missing.isPresent(0));
        assertEquals(PitFeatureDagR929.MissingReason.SOURCE_ABSENT,
                missing.missingReason(0));
        assertNotEquals(zero.valueHash, missing.valueHash);
    }

    @Test public void pitDagRejectsCycleAndFutureSource() {
        final PitFeatureDagR929.FeatureSpec a = new PitFeatureDagR929.FeatureSpec(
                "a", "raw-a", "ratio", PitFeatureDagR929.ValueType.SCALAR,
                new PitFeatureDagR929.Shape(1, 1), PitFeatureDagR929.TemporalKind.PREDRAW,
                Collections.singletonList("b"), 1, 12, "history", "mask", 100L, "1");
        final PitFeatureDagR929.FeatureSpec b = new PitFeatureDagR929.FeatureSpec(
                "b", "raw-b", "ratio", PitFeatureDagR929.ValueType.SCALAR,
                new PitFeatureDagR929.Shape(1, 1), PitFeatureDagR929.TemporalKind.PREDRAW,
                Collections.singletonList("a"), 1, 12, "history", "mask", 100L, "1");
        try {
            new PitFeatureDagR929.Manifest(PitFeatureDagR929.GAME, "RAW", "1",
                    Arrays.asList(a, b));
            fail("cycle must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("CYCLE"));
        }

        final PitFeatureDagR929.FeatureSpec raw = new PitFeatureDagR929.FeatureSpec(
                "raw", "raw", "ratio", PitFeatureDagR929.ValueType.SCALAR,
                new PitFeatureDagR929.Shape(1, 1), PitFeatureDagR929.TemporalKind.PREDRAW,
                Collections.<String>emptyList(), 1, 12, "history", "mask", 100L, "1");
        PitFeatureDagR929.Manifest manifest = new PitFeatureDagR929.Manifest(
                PitFeatureDagR929.GAME, "RAW", "1", Collections.singletonList(raw));
        Map<String, PitFeatureDagR929.Node> nodes =
                new LinkedHashMap<String, PitFeatureDagR929.Node>();
        nodes.put("raw", new PitFeatureDagR929.Node(raw, new PitFeatureDagR929.Computer() {
            @Override public PitFeatureDagR929.ValueResult compute(
                    PitFeatureDagR929.ReadOnlyContext context) {
                return PitFeatureDagR929.ValueResult.present(
                        new double[] {0.2d}, context.targetIssue, 9L, 1L, H0);
            }
        }));
        try {
            PitFeatureDagR929.freeze(manifest, "101", 10L, nodes);
            fail("current target as source must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("TIME_TRAVEL"));
        }
    }

    @Test public void actionSurfaceHasExactly1200And360PositiveLabels() {
        List<PitFeatureDagR929.ActionRow> rows = new ArrayList<PitFeatureDagR929.ActionRow>();
        int ordinal = 0;
        for (int p = 1; p <= 10; p++) {
            for (int a = 1; a <= 8; a++) {
                for (int b = a + 1; b <= 9; b++) {
                    for (int c = b + 1; c <= 10; c++) {
                        rows.add(new PitFeatureDagR929.ActionRow(ordinal++, p, a, b, c,
                                0.3d, 1200.0d - ordinal, new double[] {0.0d, 1.0d}));
                    }
                }
            }
        }
        PitFeatureDagR929.ActionSurface surface = new PitFeatureDagR929.ActionSurface(
                PitFeatureDagR929.GAME, "101", "100", 10L, "META1200",
                H0, H1, rows);
        PitFeatureDagR929.LabelJoin label = new PitFeatureDagR929.LabelJoin(
                PitFeatureDagR929.GAME, "101",
                new int[] {1,2,3,4,5,6,7,8,9,10}, 20L, 21L, "v1");
        assertEquals(1200, surface.actions.size());
        assertEquals(360, surface.positiveLabels(label));
        assertEquals(64, surface.labelledHash(label).length());
    }

    @Test public void splitPlanRequiresFiveExpandingChronologicalFolds() {
        List<ExperimentProtocolR929.Fold> folds = new ArrayList<ExperimentProtocolR929.Fold>();
        for (int i = 0; i < 5; i++) {
            folds.add(new ExperimentProtocolR929.Fold("f" + i,
                    range(1, 10 + i), range(20 + i * 10, 21 + i * 10),
                    range(22 + i * 10, 23 + i * 10),
                    range(24 + i * 10, 25 + i * 10),
                    range(26 + i * 10, 27 + i * 10), 1, 1));
        }
        ExperimentProtocolR929.SplitPlan plan = new ExperimentProtocolR929.SplitPlan(
                folds, range(100, 120), "AVAILABLE_AT_BEFORE_STAGE_BOUNDARY");
        assertEquals(5, plan.folds.size());
        assertEquals(64, plan.planHash.length());
    }

    @Test public void sealedTokenIsConsumedOnlyOnce() {
        ExperimentProtocolR929.MemoryTrialStore store =
                new ExperimentProtocolR929.MemoryTrialStore();
        ExperimentProtocolR929.Registry registry = new ExperimentProtocolR929.Registry(store);
        ExperimentProtocolR929.TrialIdentity identity = new ExperimentProtocolR929.TrialIdentity(
                PitFeatureDagR929.GAME, H0, H1, H2, H3, 929L,
                "family", H4, ExperimentProtocolR929.Stage.RANKER, H5,
                Collections.<ExperimentProtocolR929.ArtifactDependency>emptyList());
        registry.register(identity, 1L);
        registry.start(identity.trialId, 2L);
        registry.consumeSealedToken(identity.trialId, H6, H7, 3L);
        try {
            registry.consumeSealedToken(identity.trialId, H6, H7, 4L);
            fail("sealed access must be one-shot");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("ALREADY_CONSUMED"));
        }
    }

    @Test public void rawScoreCannotPassAsProbabilityAndPolicyChangeRejectsCalibration() {
        SelectionCalibrationR929.SelectionPolicy policy = policy("consumer-a", 0.05d);
        SelectionCalibrationR929.CalibrationArtifact artifact =
                new SelectionCalibrationR929.CalibrationArtifact(
                        SelectionCalibrationR929.CalibrationKind.LOGISTIC,
                        policy.policyHash, H0, H1, "global", H1, "100", H3,
                        200, new double[] {-1.0d, 2.0d}, null, null,
                        Collections.singletonList(new SelectionCalibrationR929.CalibrationBin(
                                0.0d, 1.0d, 200, 80)));
        SelectionCalibrationR929.ScoreEvidence raw =
                new SelectionCalibrationR929.ScoreEvidence(0.9d, 0.3d,
                        SelectionCalibrationR929.ProbabilityKind.RAW_SCORE, H4);
        SelectionCalibrationR929.CalibrationResult ok = SelectionCalibrationR929.calibrate(
                policy, raw, artifact, "101", "global", H1, 120);
        assertTrue(ok.usableProbability());
        assertNotEquals(0.9d, ok.selectedHitProbability, 1.0e-9d);

        SelectionCalibrationR929.SelectionPolicy changed = policy("consumer-b", 0.05d);
        SelectionCalibrationR929.CalibrationResult rejected =
                SelectionCalibrationR929.calibrate(changed, raw, artifact,
                        "101", "global", H2, 120);
        assertEquals(SelectionCalibrationR929.SupportState.POLICY_MISMATCH,
                rejected.supportState);
        assertFalse(rejected.usableProbability());
    }

    @Test public void sameCertificateContractAppliesToEveryConsumerAndNeverCreatesReal() {
        CertificateBundleServiceR929.EvidenceInput evidence =
                new CertificateBundleServiceR929.EvidenceInput(true, true, true,
                        600, 600, 3, 0.42d, 0.34d, 0.18d, 0.19d, 0.03d,
                        0.03d, 0.01d, true, true, H0);
        CertificateBundleServiceR929.Evaluation evaluation =
                CertificateBundleServiceR929.evaluate(evidence);
        assertEquals(CertificateBundleServiceR929.Level.L7_CHAMPION, evaluation.level);
        for (CertificateBundleServiceR929.Consumer consumer
                : CertificateBundleServiceR929.Consumer.values()) {
            CertificateBundleServiceR929.CandidateKey key =
                    new CertificateBundleServiceR929.CandidateKey(
                            consumer, "ns", "family", consumer.name(), H1);
            CertificateBundleServiceR929.Certificate certificate =
                    new CertificateBundleServiceR929.Certificate(key, evaluation.level,
                            CertificateBundleServiceR929.CertificateState.ACTIVE,
                            H2, H3, H4, H5, H6, H7, H8, H9, H0, H1,
                            "101", "", 10L, 0.20d, evaluation.gates, "test");
            CertificateBundleServiceR929.CertificateService service =
                    new CertificateBundleServiceR929.CertificateService();
            service.put(certificate, Collections.<String>emptyList());
            CertificateBundleServiceR929.Authorization authorization = service.authorize(
                    key, "101", H2, H3, H6, H7, 0.05d);
            assertTrue(authorization.authorized);
            assertFalse(authorization.mayCreateReal);
        }
    }

    @Test public void matrixAndLearnerAreDeterministicAndIdempotent() {
        double[][] uniform = new double[10][10];
        for (int p = 0; p < 10; p++) Arrays.fill(uniform[p], 0.1d);
        DeepModelFamilyR929.Matrix100 matrix = new DeepModelFamilyR929.Matrix100(
                uniform, "100", "101", H0);
        assertEquals(0.1d, matrix.get(0, 0), 0.0d);

        DeepModelFamilyR929.LearnerConfig config =
                new DeepModelFamilyR929.LearnerConfig(3, 4, 8, 3,
                        0.01d, 1.0d, 929L);
        DeepModelFamilyR929.LearnerState initial =
                DeepModelFamilyR929.LearnerState.initial(config);
        DeepModelFamilyR929.LearningExample example =
                new DeepModelFamilyR929.LearningExample(PitFeatureDagR929.GAME,
                        "100", "v1", H1, new double[] {0.0d, 1.0d, -1.0d}, 2);
        DeepModelFamilyR929.LearnerState once = initial.update(example);
        DeepModelFamilyR929.LearnerState twice = once.update(example);
        assertEquals(once.stateHash, twice.stateHash);
        assertArrayEquals(once.predict(new double[] {0.0d, 1.0d, -1.0d}, "101"),
                twice.predict(new double[] {0.0d, 1.0d, -1.0d}, "101"), 0.0d);
    }

    private static SelectionCalibrationR929.SelectionPolicy policy(String consumer, double weight) {
        return new SelectionCalibrationR929.SelectionPolicy(H0, consumer, weight,
                H1, "SCORE_DESC_ORDINAL_ASC", "family", H2);
    }

    private static ExperimentProtocolR929.IssueRange range(int first, int last) {
        return new ExperimentProtocolR929.IssueRange(String.format("%03d", first),
                String.format("%03d", last));
    }
}
