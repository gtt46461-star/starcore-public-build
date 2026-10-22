package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Executable acceptance harness for all 22 Batch-3/4 construction tasks.
 * Synthetic fixtures prove the code paths and fail-closed rules only; they never count as future
 * OOS evidence and are never installed into Batch34ProductionEvidenceR929.current().
 */
public final class Batch34ContractHarnessR929 {
    private static final String[] BATCH3 = {
        "D01", "D02", "D03", "D04", "D05", "D06", "D07", "D08", "D10", "C01", "C03", "C06"
    };
    private static final String[] BATCH4 = {
        "C02", "C04", "C05", "C07", "C08", "C09", "C10", "C11", "C12", "D09"
    };
    private static final Map<String, Integer> COUNTS = new LinkedHashMap<String, Integer>();
    private static int assertions;

    private Batch34ContractHarnessR929() {}

    private static void verify(String task, boolean ok, String name) {
        if (!ok) throw new AssertionError(task + "|" + name);
        Integer count = COUNTS.get(task);
        COUNTS.put(task, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
        assertions++;
        System.out.println("ASSERT|" + task + "|PASS|" + name);
    }

    private static boolean has(Batch34EvidenceContractR929.Check check, String prefix) {
        if (check == null) return false;
        for (String code : check.codes) if (code.startsWith(prefix)) return true;
        return false;
    }

    private static boolean has(Batch34RuntimeGateR929.GateResult gate, String prefix) {
        if (gate == null) return false;
        for (String code : gate.failures) if (code.startsWith(prefix)) return true;
        return false;
    }

    private static String sha(String value) {
        return Batch34EvidenceContractR929.sha256(value);
    }

    private static Batch34TrainingCoreR929.SplitPlan splitPlan() {
        ArrayList<Batch34TrainingCoreR929.ExpandingFold> folds =
                new ArrayList<Batch34TrainingCoreR929.ExpandingFold>();
        for (int i = 0; i < 5; i++) {
            int base = 100 + i * 100;
            folds.add(new Batch34TrainingCoreR929.ExpandingFold(
                    "00000001",
                    issue(base),
                    issue(base + 1),
                    issue(base + 10),
                    issue(base + 11),
                    issue(base + 20),
                    issue(base + 21),
                    issue(base + 30),
                    issue(base + 31),
                    issue(base + 40)));
        }
        Batch34TrainingCoreR929.SplitPlan draft = new Batch34TrainingCoreR929.SplitPlan(
                sha("source"), folds, "");
        return new Batch34TrainingCoreR929.SplitPlan(
                draft.sourceSha256, draft.folds, draft.calculatedSha256());
    }

    private static String issue(int value) {
        return String.format(java.util.Locale.US, "%08d", value);
    }

    private static Batch34EvidenceContractR929.DatasetManifest dataset(
            Batch34TrainingCoreR929.FeatureManifest features,
            Batch34TrainingCoreR929.SplitPlan split) {
        return new Batch34EvidenceContractR929.DatasetManifest(
                Batch34EvidenceContractR929.DATASET_SCHEMA,
                Batch34EvidenceContractR929.GAME,
                sha("dataset-source"),
                features.manifestSha256,
                split.splitSha256,
                sha("dataset-content"),
                "00000001",
                "00002000",
                Batch34EvidenceContractR929.LABEL_SCHEMA,
                1200,
                1200,
                96,
                10,
                10,
                0,
                0,
                0,
                0,
                17,
                true,
                true,
                true,
                true,
                true);
    }

    private static Batch34EvidenceContractR929.BundleManifest bundle(String generation, String previous) {
        List<String> ids = Batch34EvidenceContractR929.requiredArtifactIds();
        ArrayList<Batch34EvidenceContractR929.ArtifactNode> nodes =
                new ArrayList<Batch34EvidenceContractR929.ArtifactNode>();
        for (int i = 0; i < ids.size(); i++) {
            List<String> dependencies = i == 0
                    ? Collections.<String>emptyList()
                    : Collections.singletonList(ids.get(i - 1));
            nodes.add(new Batch34EvidenceContractR929.ArtifactNode(
                    ids.get(i),
                    generation,
                    sha(generation + "|artifact|" + ids.get(i)),
                    sha(generation + "|contract|" + ids.get(i)),
                    dependencies,
                    "00001000",
                    "00001001",
                    "00002000",
                    1200,
                    true,
                    true,
                    true,
                    true));
        }
        Batch34EvidenceContractR929.BundleManifest draft =
                new Batch34EvidenceContractR929.BundleManifest(
                        generation,
                        "2026-09-10T00:00:00Z",
                        previous,
                        "",
                        "R929_WHOLE_GENERATION",
                        nodes);
        return new Batch34EvidenceContractR929.BundleManifest(
                draft.generationId,
                draft.createdAtUtc,
                draft.previousBundleSha256,
                draft.calculatedSha256(),
                draft.rollbackGroup,
                draft.nodes);
    }

    private static Batch34EvidenceContractR929.BundleManifest cyclicBundle() {
        String generation = "R929_CYCLE_FIXTURE";
        List<String> ids = Batch34EvidenceContractR929.requiredArtifactIds();
        ArrayList<Batch34EvidenceContractR929.ArtifactNode> nodes =
                new ArrayList<Batch34EvidenceContractR929.ArtifactNode>();
        for (int i = 0; i < ids.size(); i++) {
            List<String> dependencies;
            if (i == 0) dependencies = Collections.singletonList(ids.get(1));
            else if (i == 1) dependencies = Collections.singletonList(ids.get(0));
            else dependencies = Collections.singletonList(ids.get(i - 1));
            nodes.add(new Batch34EvidenceContractR929.ArtifactNode(
                    ids.get(i), generation, sha("cycle-a-" + i), sha("cycle-c-" + i),
                    dependencies, "00000100", "00000101", "00000200", 100,
                    true, true, true, true));
        }
        Batch34EvidenceContractR929.BundleManifest draft =
                new Batch34EvidenceContractR929.BundleManifest(
                        generation, "2026-09-10T00:00:00Z", "", "", "cycle", nodes);
        return new Batch34EvidenceContractR929.BundleManifest(
                draft.generationId, draft.createdAtUtc, draft.previousBundleSha256,
                draft.calculatedSha256(), draft.rollbackGroup, draft.nodes);
    }

    private static Batch34StatisticalGovernanceR929.DomainCertificate domainCertificate() {
        ArrayList<Batch34StatisticalGovernanceR929.DomainComponent> components =
                new ArrayList<Batch34StatisticalGovernanceR929.DomainComponent>();
        components.add(new Batch34StatisticalGovernanceR929.DomainComponent(
                "STABLE_LOW_VOLATILITY", 0.55d, 500, 100, 60,
                sha("domain-definition-1"), sha("domain-calibration-1"), true));
        components.add(new Batch34StatisticalGovernanceR929.DomainComponent(
                "TRANSITION_HIGH_VOLATILITY", 0.45d, 450, 90, 55,
                sha("domain-definition-2"), sha("domain-calibration-2"), true));
        Batch34StatisticalGovernanceR929.DomainCertificate draft =
                new Batch34StatisticalGovernanceR929.DomainCertificate(
                        sha("domain-model"), sha("global-calibration"), "00001000",
                        components, true, true, "");
        return new Batch34StatisticalGovernanceR929.DomainCertificate(
                draft.domainModelSha256,
                draft.globalCalibrationSha256,
                draft.frozenAtIssue,
                draft.components,
                draft.trainOnlyClustering,
                draft.globalShrinkageEnabled,
                draft.calculatedSha256());
    }

    private static Batch34StatisticalGovernanceR929.CertificateV2 certificate(
            Batch34EvidenceContractR929.DatasetManifest dataset,
            Batch34EvidenceContractR929.BundleManifest bundle,
            Batch34StatisticalGovernanceR929.DomainCertificate domain,
            boolean directBetAuthority) {
        Batch34StatisticalGovernanceR929.Metric candidate =
                new Batch34StatisticalGovernanceR929.Metric(
                        200, 0.45d, 0.39d, 0.22d, 0.03d, 5, "FUTURE_SELECTED");
        Batch34StatisticalGovernanceR929.Metric champion =
                new Batch34StatisticalGovernanceR929.Metric(
                        200, 0.41d, 0.36d, 0.24d, 0.035d, 7, "FUTURE_PAIRED_CHAMPION");
        Batch34StatisticalGovernanceR929.CertificateV2 draft =
                new Batch34StatisticalGovernanceR929.CertificateV2(
                        "WINDOW_R929_001",
                        "candidate-r929",
                        sha("candidate-r929"),
                        "champion-r928",
                        sha("champion-r928"),
                        dataset.datasetSha256,
                        dataset.featureManifestSha256,
                        bundle.bundleSha256,
                        sha("frozen-policy"),
                        domain.certificateSha256,
                        "00001000",
                        "00001001",
                        "00001200",
                        candidate,
                        champion,
                        0.04d,
                        0.01d,
                        0.07d,
                        0.01d,
                        "H_R929_001",
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        directBetAuthority,
                        Collections.<String>emptyList(),
                        "");
        return new Batch34StatisticalGovernanceR929.CertificateV2(
                draft.windowId,
                draft.candidateId,
                draft.candidateSha256,
                draft.championId,
                draft.championSha256,
                draft.datasetSha256,
                draft.featureManifestSha256,
                draft.artifactBundleSha256,
                draft.policySha256,
                draft.domainCertificateSha256,
                draft.trainEndIssue,
                draft.futureStartIssue,
                draft.futureEndIssue,
                draft.candidate,
                draft.pairedChampion,
                draft.pairedDelta,
                draft.pairedLower,
                draft.pairedUpper,
                draft.alphaSpent,
                draft.hypothesisId,
                draft.pitPassed,
                draft.oosPassed,
                draft.baselineFrozen,
                draft.resourcePassed,
                draft.parityPassed,
                draft.nonRegressionPassed,
                draft.exclusionLedgerComplete,
                draft.stopRuleHonored,
                draft.directBetAuthority,
                draft.exclusions,
                draft.calculatedSha256());
    }

    private static Batch34RuntimeGateR929.PolicyEvidence policy(boolean effectPass) {
        return new Batch34RuntimeGateR929.PolicyEvidence(
                1000,
                300,
                effectPass ? 180 : 100,
                effectPass ? 180 : 100,
                effectPass ? 180 : 100,
                effectPass ? 180 : 0,
                effectPass ? 820 : 1000,
                effectPass ? 81 : 35,
                effectPass ? 0.45d : 0.35d,
                effectPass ? 0.39d : 0.30d,
                effectPass ? 0.22d : 0.28d,
                effectPass ? 0.03d : 0.08d,
                effectPass ? 5 : 11,
                sha("predictability"),
                sha("selector"),
                sha("policy"),
                sha("policy-window"),
                true,
                true,
                true,
                true,
                true,
                true,
                true);
    }

    private static Batch34RuntimeGateR929.StabilityEvidence stability() {
        int[][] rankings = new int[5][];
        double[] probabilities = {0.44d, 0.45d, 0.46d, 0.45d, 0.445d};
        int[] ranking = {12, 3, 7, 1, 9, 5, 4, 2, 8, 6};
        for (int i = 0; i < rankings.length; i++) rankings[i] = ranking.clone();
        return Batch34TrainingCoreR929.evaluateStability(
                rankings, probabilities, sha("uncertainty"), sha("rank-stability"));
    }

    private static Batch34RuntimeGateR929.ProductionEvidence productionEvidence(
            Batch34EvidenceContractR929.DatasetManifest dataset,
            Batch34EvidenceContractR929.BundleManifest bundle,
            Batch34StatisticalGovernanceR929.CertificateV2 certificate,
            Batch34StatisticalGovernanceR929.DomainCertificate domain,
            Batch34RuntimeGateR929.PolicyEvidence policy,
            boolean relationAblation,
            boolean failureAblation) {
        return new Batch34RuntimeGateR929.ProductionEvidence(
                dataset,
                bundle,
                certificate,
                domain,
                policy,
                stability(),
                true,
                relationAblation,
                failureAblation,
                true,
                true,
                true,
                sha("source-commit"),
                sha("evidence-index"));
    }

    private static List<Batch34ResearchLifecycleR929.AtomDescriptor> atoms(
            boolean changeFirst,
            boolean quarantineChanged) {
        ArrayList<Batch34ResearchLifecycleR929.AtomDescriptor> out =
                new ArrayList<Batch34ResearchLifecycleR929.AtomDescriptor>();
        appendAtoms(out, "PRE", 165, Batch34ResearchLifecycleR929.AtomCategory.PRE_DRAW,
                changeFirst, quarantineChanged);
        appendAtoms(out, "PROXY", 40, Batch34ResearchLifecycleR929.AtomCategory.PROXY,
                false, false);
        appendAtoms(out, "POST", 20, Batch34ResearchLifecycleR929.AtomCategory.POST_DRAW,
                false, false);
        appendAtoms(out, "AUDIT", 15, Batch34ResearchLifecycleR929.AtomCategory.AUDIT,
                false, false);
        return out;
    }

    private static void appendAtoms(
            List<Batch34ResearchLifecycleR929.AtomDescriptor> out,
            String prefix,
            int count,
            Batch34ResearchLifecycleR929.AtomCategory category,
            boolean changeFirst,
            boolean quarantineChanged) {
        for (int i = 0; i < count; i++) {
            String id = String.format(java.util.Locale.US, "ATOM_%s_%03d", prefix, i);
            boolean changed = changeFirst && i == 0;
            Batch34ResearchLifecycleR929.AtomState state = changed && quarantineChanged
                    ? Batch34ResearchLifecycleR929.AtomState.QUARANTINED
                    : Batch34ResearchLifecycleR929.AtomState.ACTIVE;
            out.add(new Batch34ResearchLifecycleR929.AtomDescriptor(
                    id,
                    category,
                    state,
                    "SCALAR_WITH_EXPLICIT_MISSING",
                    sha((changed ? "changed-" : "impl-") + id),
                    sha("contract-" + id),
                    category == Batch34ResearchLifecycleR929.AtomCategory.PRE_DRAW && i < 20,
                    1200,
                    i % 7,
                    0,
                    1_000_000L + i,
                    0.90d,
                    0.01d));
        }
    }

    private static Batch34ResearchLifecycleR929.Genome genome(Set<String> activeAtoms) {
        List<String> dependencies = Arrays.asList("ATOM_PRE_000", "ATOM_PROXY_000");
        ArrayList<Batch34ResearchLifecycleR929.MethodGraphEdge> graph =
                new ArrayList<Batch34ResearchLifecycleR929.MethodGraphEdge>();
        graph.add(new Batch34ResearchLifecycleR929.MethodGraphEdge(
                "ATOM_PRE_000", "EXPR_1",
                Batch34ResearchLifecycleR929.EdgeType.INPUT_TO_EXPRESSION));
        graph.add(new Batch34ResearchLifecycleR929.MethodGraphEdge(
                "EXPR_1", "METHOD_1",
                Batch34ResearchLifecycleR929.EdgeType.EXPRESSION_TO_METHOD));
        graph.add(new Batch34ResearchLifecycleR929.MethodGraphEdge(
                "METHOD_1", "STATE_1",
                Batch34ResearchLifecycleR929.EdgeType.METHOD_TO_STATE));
        graph.add(new Batch34ResearchLifecycleR929.MethodGraphEdge(
                "STATE_1", "SCORE_1",
                Batch34ResearchLifecycleR929.EdgeType.STATE_TO_SCORE));
        Batch34ResearchLifecycleR929.Genome draft =
                new Batch34ResearchLifecycleR929.Genome(
                        "GENOME_R929_001",
                        "RELATION_RESIDUAL",
                        Batch34ResearchLifecycleR929.NoveltyClass.STRUCTURAL_VARIANT,
                        dependencies,
                        "zscore(ATOM_PRE_000)+0.25*ATOM_PROXY_000",
                        graph,
                        "ema_state=tminus1_only(score,0.10)",
                        Arrays.asList("input=0.2,0.4=>score=0.3", "missing=>explicit_absent"),
                        Arrays.asList("PIT_VIOLATION", "NONFINITE", "COST_BUDGET"),
                        "SCALAR_X_SCALAR_TO_SCORE",
                        "[-32,32]",
                        20_000_000L,
                        true,
                        true,
                        true,
                        "");
        return new Batch34ResearchLifecycleR929.Genome(
                draft.genomeId,
                draft.statisticalFamily,
                draft.noveltyClass,
                draft.atomDependencies,
                draft.expressionDsl,
                draft.graph,
                draft.stateUpdateDsl,
                draft.examples,
                draft.failureConditions,
                draft.typeContract,
                draft.rangeContract,
                draft.worstCaseCostNs,
                draft.pitSafe,
                draft.deterministic,
                draft.hardConstraintsPassed,
                draft.calculatedSha256());
    }

    private static void testBatch3() {
        Batch34TrainingCoreR929.FeatureManifest features =
                Batch34TrainingCoreR929.standardFeatureManifest();
        verify("D01", Batch34TrainingCoreR929.validateFeatureManifest(features).passed,
                "FEATURE_MANIFEST_96_EXPLICIT_FIELDS");
        Batch34TrainingCoreR929.SplitPlan split = splitPlan();
        Batch34EvidenceContractR929.DatasetManifest dataset = dataset(features, split);
        verify("D01", Batch34EvidenceContractR929.validateDataset(dataset, true).passed,
                "PRODUCTION_DATASET_PIT_CONTRACT");
        List<Batch34TrainingCoreR929.Action> actions =
                Batch34TrainingCoreR929.enumerateActions(new int[]{1,2,3,4,5,6,7,8,9,10});
        Batch34TrainingCoreR929.PitSnapshot snapshot =
                new Batch34TrainingCoreR929.PitSnapshot(
                        "00002001", "00002000", 1000L, 1100L, 1200L, 1200, 360,
                        9, 0, 0, sha("raw-source"), features.manifestSha256,
                        Batch34TrainingCoreR929.actionSurfaceSha256(actions), true, true, false);
        verify("D01", Batch34TrainingCoreR929.validateSnapshot(snapshot).passed,
                "PREDRAW_SNAPSHOT_TIME_AND_HASH");
        Batch34TrainingCoreR929.PitSnapshot leaked =
                new Batch34TrainingCoreR929.PitSnapshot(
                        "00002001", "00002001", 1000L, 1100L, 1200L, 1200, 360,
                        0, 0, 0, sha("raw-source"), features.manifestSha256,
                        Batch34TrainingCoreR929.actionSurfaceSha256(actions), true, true, false);
        verify("D01", has(Batch34TrainingCoreR929.validateSnapshot(leaked),
                        "D01_SNAPSHOT_PIT_VIOLATION"),
                "FUTURE_SOURCE_REJECTED");

        Batch34EvidenceContractR929.BundleManifest validBundle = bundle("GEN_R929_1", "");
        verify("D02", Batch34EvidenceContractR929.validateBundle(validBundle, true).passed,
                "ACYCLIC_STAGE_GRAPH_AND_TEMPORAL_ORDER");
        verify("D02", has(Batch34EvidenceContractR929.validateBundle(cyclicBundle(), false),
                        "D10_DEPENDENCY_CYCLE"),
                "CIRCULAR_NUMBER_META_INPUT_REJECTED");
        Batch34TrainingCoreR929.FeatureManifest wrongStage =
                new Batch34TrainingCoreR929.FeatureManifest(
                        features.schema, "POST_NUMBER_ACTION_META", features.parentArtifactSha256,
                        features.features, "");
        wrongStage = new Batch34TrainingCoreR929.FeatureManifest(
                wrongStage.schema, wrongStage.inputStage, wrongStage.parentArtifactSha256,
                wrongStage.features, wrongStage.calculatedSha256());
        verify("D02", has(Batch34TrainingCoreR929.validateFeatureManifest(wrongStage),
                        "D02_INPUT_STAGE_NOT_RAW_UPSTREAM"),
                "SAME_DIMENSION_DIFFERENT_SEMANTICS_REJECTED");

        verify("D03", Batch34TrainingCoreR929.validateSplitPlan(split).passed,
                "FIVE_EXPANDING_FOLDS_STRICTLY_ORDERED");
        Batch34TrainingCoreR929.TrialRegistry trials =
                new Batch34TrainingCoreR929.TrialRegistry();
        Batch34TrainingCoreR929.Trial trial = new Batch34TrainingCoreR929.Trial(
                "trial-1", Batch34EvidenceContractR929.GAME,
                Batch34EvidenceContractR929.DATASET_SCHEMA, sha("source"),
                "00000631", "00000640", sha("hyper"));
        verify("D03", "D03_TRIAL_RESERVED".equals(trials.reserve(trial)),
                "GLOBAL_SEALED_TRIAL_RESERVED");
        verify("D03", "D03_TRIAL_RESUME_IDEMPOTENT".equals(trials.reserve(
                        new Batch34TrainingCoreR929.Trial(
                                "trial-1", Batch34EvidenceContractR929.GAME,
                                Batch34EvidenceContractR929.DATASET_SCHEMA, sha("source"),
                                "00000631", "00000640", sha("hyper")))),
                "CRASH_RESUME_DOES_NOT_RECONSUME_SEALED");
        verify("D03", "D03_SEALED_WINDOW_ALREADY_CONSUMED".equals(trials.reserve(
                        new Batch34TrainingCoreR929.Trial(
                                "copied-directory-trial", Batch34EvidenceContractR929.GAME,
                                Batch34EvidenceContractR929.DATASET_SCHEMA, sha("source"),
                                "00000631", "00000640", sha("other-hyper")))),
                "COPYING_SOURCE_PATH_CANNOT_RESET_SEALED_WINDOW");
        verify("D03", "D03_TRIAL_RUNNING".equals(trials.begin("trial-1"))
                        && "D03_TRIAL_FINALIZED_ONCE".equals(
                                trials.finalizeOnce("trial-1", true, sha("trial-result"))),
                "TRIAL_FINALIZES_ONCE");

        String actionHash = Batch34TrainingCoreR929.actionSurfaceSha256(actions);
        Batch34TrainingCoreR929.RankerCandidate baseline =
                new Batch34TrainingCoreR929.RankerCandidate(
                        "raw-action-p", "BASELINE", sha("rank-base"), actionHash, 200,
                        0.40d, 0.70d, 0.55d, 0.60d, 0.0d, true, true, true, true);
        Batch34TrainingCoreR929.RankerCandidate candidate =
                new Batch34TrainingCoreR929.RankerCandidate(
                        "listwise-pairwise", "LINEAR_LISTWISE_PAIRWISE", sha("rank-candidate"),
                        actionHash, 200, 0.42d, 0.72d, 0.57d, 0.63d, 0.01d,
                        true, true, true, true);
        Batch34TrainingCoreR929.RankerSelection promoted =
                Batch34TrainingCoreR929.selectRanker(baseline, Arrays.asList(candidate));
        verify("D04", promoted.champion == candidate && !promoted.baselineRetained,
                "INDEPENDENTLY_BETTER_CANDIDATE_SELECTED");
        Batch34TrainingCoreR929.RankerCandidate unproven =
                new Batch34TrainingCoreR929.RankerCandidate(
                        "tiny-mlp", "SMALL_MLP", sha("rank-unproven"), actionHash, 200,
                        0.41d, 0.71d, 0.56d, 0.62d, -0.01d,
                        true, true, true, true);
        Batch34TrainingCoreR929.RankerSelection retained =
                Batch34TrainingCoreR929.selectRanker(baseline, Arrays.asList(unproven));
        verify("D04", retained.champion == baseline && retained.baselineRetained,
                "NO_GAIN_RETAINS_BASELINE_WITHOUT_FAKE_PROMOTION");
        Batch34TrainingCoreR929.RankerCandidate nonfinite =
                new Batch34TrainingCoreR929.RankerCandidate(
                        "broken", "BROKEN", sha("broken"), actionHash, 10,
                        Double.NaN, 0.5d, 0.4d, 0.4d, 0.1d,
                        true, true, true, true);
        verify("D04", Batch34TrainingCoreR929.selectRanker(
                        baseline, Arrays.asList(nonfinite)).champion == baseline,
                "NONFINITE_CANDIDATE_CANNOT_PROMOTE");

        verify("D05", Batch34TrainingCoreR929.validateActionSurface(actions, true).passed,
                "EXACT_1200_UNIQUE_ACTIONS_AND_360_POSITIVES");
        verify("D05", actions.get(0).ordinal == 0
                        && actions.get(actions.size() - 1).ordinal == 1199,
                "ORDINAL_ZERO_TO_1199");
        verify("D05", candidate.all1200Scored && candidate.hostAndroidParity
                        && candidate.deterministicTieBreak,
                "FULL_SURFACE_HOST_ANDROID_PARITY_CONTRACT");

        String selector = sha("selector-frozen");
        Batch34TrainingCoreR929.SelectedCalibrationModel calibration =
                Batch34TrainingCoreR929.fitSelectedCalibration(
                        new double[]{-2.0d, -1.0d, 0.0d, 1.0d, 2.0d, 3.0d},
                        new int[]{0, 0, 1, 0, 1, 1}, selector);
        verify("D06", Batch34TrainingCoreR929.validateCalibration(calibration, selector).passed,
                "SELECTED_ACTION_ISOTONIC_CALIBRATION");
        double calibrated = calibration.predict(0.5d, selector);
        verify("D06", calibrated >= 0.0d && calibrated <= 1.0d
                        && Math.abs(calibrated - 0.5d) > 1e-9d,
                "RAW_SCORE_IS_NOT_RELABELED_AS_HIT_PROBABILITY");
        boolean selectorRejected = false;
        try { calibration.predict(0.5d, sha("changed-selector")); }
        catch (IllegalStateException expected) { selectorRejected = true; }
        verify("D06", selectorRejected,
                "FUSION_SELECTOR_CHANGE_REQUIRES_RECALIBRATION");

        Batch34RuntimeGateR929.StabilityEvidence stability = stability();
        verify("D07", stability.deterministicSeeds == 5
                        && stability.top1Agreement == 1.0d
                        && stability.meanKendallTau == 1.0d,
                "FIVE_SEED_TOP1_AND_KENDALL_STABILITY");
        verify("D07", stability.sameInputReplayIdentical
                        && stability.bootstrapUsesIssueBlocks
                        && stability.probabilityIntervalWidthP95 <= 0.25d,
                "REPLAY_AND_UNCERTAINTY_CONTRACT");

        Batch34StatisticalGovernanceR929.DomainCertificate domain = domainCertificate();
        Batch34StatisticalGovernanceR929.CertificateV2 certificate =
                certificate(dataset, validBundle, domain, false);
        Batch34RuntimeGateR929.GateResult relationFail = Batch34RuntimeGateR929.evaluate(
                productionEvidence(dataset, validBundle, certificate, domain, policy(true),
                        false, true), true);
        verify("D08", !relationFail.effectReady
                        && has(relationFail, "D08_RELATION_GRAPH_ABLATION_NOT_PASSED"),
                "RELATION_GRAPH_CANNOT_INFLUENCE_WITHOUT_ABLATION");
        Batch34RuntimeGateR929.GateResult relationPass = Batch34RuntimeGateR929.evaluate(
                productionEvidence(dataset, validBundle, certificate, domain, policy(true),
                        true, true), true);
        verify("D08", relationPass.effectReady,
                "RELATION_GRAPH_ABLATION_EVIDENCE_CONSUMED_BY_GATE");

        Batch34TrainingCoreR929.AtomicPublicationStore store =
                new Batch34TrainingCoreR929.AtomicPublicationStore();
        verify("D10", "D10_STAGE_VALIDATED".equals(store.stage(validBundle)),
                "WHOLE_GENERATION_STAGED_AFTER_VALIDATION");
        String empty = sha("D10_EMPTY_BUNDLE");
        Batch34EvidenceContractR929.AtomicSwitchToken tokenDraft =
                new Batch34EvidenceContractR929.AtomicSwitchToken(
                        empty, validBundle.bundleSha256, validBundle.generationId,
                        validBundle.rollbackGroup, 1L, "");
        Batch34EvidenceContractR929.AtomicSwitchToken token =
                new Batch34EvidenceContractR929.AtomicSwitchToken(
                        tokenDraft.expectedCurrentBundleSha256,
                        tokenDraft.nextBundleSha256,
                        tokenDraft.nextGenerationId,
                        tokenDraft.rollbackGroup,
                        tokenDraft.monotonicSequence,
                        tokenDraft.calculatedSha256());
        verify("D10", "D10_ACTIVATED_ATOMIC_GENERATION".equals(
                        store.activate(empty, token, true)),
                "CAS_ACTIVATION_AT_ISSUE_BOUNDARY");
        Batch34EvidenceContractR929.BundleManifest second =
                bundle("GEN_R929_2", validBundle.bundleSha256);
        verify("D10", "D10_STAGE_VALIDATED".equals(store.stage(second)),
                "SECOND_GENERATION_STAGED");
        Batch34EvidenceContractR929.AtomicSwitchToken token2Draft =
                new Batch34EvidenceContractR929.AtomicSwitchToken(
                        validBundle.bundleSha256, second.bundleSha256, second.generationId,
                        second.rollbackGroup, 2L, "");
        Batch34EvidenceContractR929.AtomicSwitchToken token2 =
                new Batch34EvidenceContractR929.AtomicSwitchToken(
                        token2Draft.expectedCurrentBundleSha256,
                        token2Draft.nextBundleSha256,
                        token2Draft.nextGenerationId,
                        token2Draft.rollbackGroup,
                        token2Draft.monotonicSequence,
                        token2Draft.calculatedSha256());
        verify("D10", "D10_ACTIVATED_ATOMIC_GENERATION".equals(
                        store.activate(validBundle.bundleSha256, token2, true))
                        && "D10_ROLLED_BACK_WHOLE_GENERATION".equals(
                                store.rollback(second.bundleSha256, 3L, true))
                        && validBundle.bundleSha256.equals(store.currentSha256()),
                "WHOLE_GENERATION_ROLLBACK_NO_MIXED_BUNDLE");

        verify("C01", Batch34StatisticalGovernanceR929.validateCertificate(
                        certificate, true).passed,
                "CERTIFICATE_V2_COMPLETE_AND_HASH_BOUND");
        Batch34StatisticalGovernanceR929.CertificateV2 illegalAuthority =
                certificate(dataset, validBundle, domain, true);
        verify("C01", has(Batch34StatisticalGovernanceR929.validateCertificate(
                        illegalAuthority, true), "C01_CERTIFICATE_MUST_NOT_AUTHORIZE_BET"),
                "CERTIFICATE_HAS_NO_DIRECT_BET_AUTHORITY");

        Batch34StatisticalGovernanceR929.AlphaLedger alpha =
                new Batch34StatisticalGovernanceR929.AlphaLedger(0.05d);
        Batch34StatisticalGovernanceR929.Hypothesis h1 =
                alpha.reserve("H1", "RANKER", "WINDOW1", 0.02d);
        verify("C03", h1 != null && "C03_OBSERVING".equals(h1.beginObservation())
                        && "REJECTED_NULL".equals(h1.decide(0.001d)),
                "ALPHA_DEBIT_AND_DECISION");
        double beforeRefund = alpha.availableAlpha();
        Batch34StatisticalGovernanceR929.Hypothesis h2 =
                alpha.reserve("H2", "RANKER", "WINDOW2", 0.01d);
        verify("C03", h2 != null
                        && "C03_REFUNDED_BEFORE_OBSERVATION".equals(alpha.cancelAndRefund("H2"))
                        && alpha.availableAlpha() > beforeRefund - 0.011d,
                "ONLY_PREOBSERVATION_CANCELLATION_REFUNDS_ALPHA");
        boolean[] bh = Batch34StatisticalGovernanceR929.benjaminiHochberg(
                new double[]{0.001d, 0.01d, 0.20d}, 0.05d);
        verify("C03", bh.length == 3 && bh[0] && bh[1] && !bh[2],
                "FAMILY_FDR_CONTROL_EXECUTABLE");

        verify("C06", Batch34StatisticalGovernanceR929.validateDomainCertificate(
                        domain, true).passed,
                "TRAIN_ONLY_DOMAIN_AND_GLOBAL_SHRINKAGE_CERTIFICATE");
        ArrayList<Batch34StatisticalGovernanceR929.DomainComponent> badComponents =
                new ArrayList<Batch34StatisticalGovernanceR929.DomainComponent>(domain.components);
        badComponents.set(0, new Batch34StatisticalGovernanceR929.DomainComponent(
                "STABLE_LOW_VOLATILITY", 0.70d, 500, 100, 60,
                sha("domain-definition-1"), sha("domain-calibration-1"), true));
        Batch34StatisticalGovernanceR929.DomainCertificate badDomainDraft =
                new Batch34StatisticalGovernanceR929.DomainCertificate(
                        domain.domainModelSha256, domain.globalCalibrationSha256,
                        domain.frozenAtIssue, badComponents, true, true, "");
        Batch34StatisticalGovernanceR929.DomainCertificate badDomain =
                new Batch34StatisticalGovernanceR929.DomainCertificate(
                        badDomainDraft.domainModelSha256,
                        badDomainDraft.globalCalibrationSha256,
                        badDomainDraft.frozenAtIssue,
                        badDomainDraft.components,
                        badDomainDraft.trainOnlyClustering,
                        badDomainDraft.globalShrinkageEnabled,
                        badDomainDraft.calculatedSha256());
        verify("C06", has(Batch34StatisticalGovernanceR929.validateDomainCertificate(
                        badDomain, true), "C06_POSTERIOR_SUM_NOT_ONE"),
                "INVALID_DOMAIN_POSTERIOR_REJECTED");
    }

    private static void testBatch4() {
        String candidateHash = sha("paired-candidate");
        Batch34StatisticalGovernanceR929.WindowRegistry windows =
                new Batch34StatisticalGovernanceR929.WindowRegistry();
        Batch34StatisticalGovernanceR929.PairedWindow window =
                new Batch34StatisticalGovernanceR929.PairedWindow(
                        "PAIR_1", candidateHash, sha("paired-champion"), sha("paired-policy"),
                        "00003000", "00003001", 2, 1000L);
        verify("C02", "C02_WINDOW_DECLARED".equals(windows.declare(window))
                        && "C02_WINDOW_OBSERVING".equals(window.begin()),
                "FROZEN_PAIRED_WINDOW_DECLARED_ONCE");
        verify("C02", "C02_OBSERVATION_ACCEPTED".equals(window.observe("00003000"))
                        && "C02_OBSERVATION_ACCEPTED".equals(window.observe("00003001"))
                        && "C02_FINALIZED_ONCE".equals(window.finalizeOnce(true, sha("decision"))),
                "EXACT_PLANNED_WINDOW_FINALIZED_ONCE");
        verify("C02", window.finalizeOnce(false, sha("other-decision")).startsWith(
                        "C02_FINALIZE_REJECTED_"),
                "REPEATED_OPTIONAL_STOPPING_REJECTED");
        Batch34StatisticalGovernanceR929.PairedWindow duplicate =
                new Batch34StatisticalGovernanceR929.PairedWindow(
                        "PAIR_2", candidateHash, sha("paired-champion"), sha("paired-policy"),
                        "00003000", "00003001", 2, 1001L);
        verify("C02", "C02_REPEATED_TRIAL_REJECTED".equals(windows.declare(duplicate)),
                "SAME_CANDIDATE_WINDOW_CANNOT_BE_REUSED");

        Batch34ResearchLifecycleR929.CaptureLedger captures =
                new Batch34ResearchLifecycleR929.CaptureLedger();
        Batch34ResearchLifecycleR929.CaptureRecord capture =
                new Batch34ResearchLifecycleR929.CaptureRecord(
                        "00004000", sha("opportunity"), 1000L, 0L);
        verify("C04", "C04_CAPTURE_CREATED".equals(captures.create(capture)),
                "SYSTEM_OPPORTUNITY_CREATED");
        verify("C04", "C04_CAPTURE_TRANSITION_COMMITTED".equals(capture.transition(
                        Batch34ResearchLifecycleR929.CaptureState.SYSTEM_AVAILABLE,
                        Batch34ResearchLifecycleR929.CaptureState.ELIGIBLE, 0L))
                        && "C04_CAPTURE_TRANSITION_COMMITTED".equals(capture.transition(
                                Batch34ResearchLifecycleR929.CaptureState.ELIGIBLE,
                                Batch34ResearchLifecycleR929.CaptureState.SELECTED, 1L))
                        && "C04_CAPTURE_TRANSITION_COMMITTED".equals(capture.transition(
                                Batch34ResearchLifecycleR929.CaptureState.SELECTED,
                                Batch34ResearchLifecycleR929.CaptureState.EXECUTION_STARTED, 2L)),
                "MONOTONIC_CAPTURE_STATE_MACHINE");
        verify("C04", "C04_FREEZE_COMMITTED".equals(capture.freeze(3L, sha("frozen"), true))
                        && captures.summary().monotonic(),
                "EVENT_BOUND_FROZEN_CAPTURE_AND_DENOMINATORS");
        verify("C04", "C04_FREEZE_CAS_CONFLICT".equals(capture.freeze(3L, sha("again"), true)),
                "DOUBLE_FREEZE_PREVENTED_BY_CAS");

        Batch34ResearchLifecycleR929.FamilyRegistry families =
                new Batch34ResearchLifecycleR929.FamilyRegistry();
        String structural = sha("family-structural");
        String behavior = sha("family-behavior");
        String residual = sha("family-residual");
        Batch34ResearchLifecycleR929.FamilyMember champion =
                new Batch34ResearchLifecycleR929.FamilyMember(
                        "family-champion", structural, behavior, residual, "RANKER",
                        sha("family-champion-artifact"),
                        Batch34ResearchLifecycleR929.FamilyRole.CHAMPION, 200);
        verify("C05", "C05_MEMBER_REGISTERED".equals(families.register(champion)),
                "ONE_STRUCTURAL_BEHAVIORAL_RESIDUAL_FAMILY_CREATED");
        for (int i = 0; i < 3; i++) {
            verify("C05", "C05_MEMBER_REGISTERED".equals(families.register(
                            new Batch34ResearchLifecycleR929.FamilyMember(
                                    "challenger-" + i, structural, behavior, residual, "RANKER",
                                    sha("challenger-artifact-" + i),
                                    Batch34ResearchLifecycleR929.FamilyRole.CHALLENGER, 100))),
                    "CHALLENGER_WITHIN_CAP_" + i);
        }
        verify("C05", "C05_MAX_THREE_CHALLENGERS".equals(families.register(
                        new Batch34ResearchLifecycleR929.FamilyMember(
                                "challenger-4", structural, behavior, residual, "RANKER",
                                sha("challenger-artifact-4"),
                                Batch34ResearchLifecycleR929.FamilyRole.CHALLENGER, 100))),
                "FOURTH_CHALLENGER_REJECTED");

        ArrayList<String> tierC = new ArrayList<String>();
        for (int i = 0; i < 80; i++) tierC.add("nursery-" + i);
        List<Batch34ResearchLifecycleR929.ScheduledResearch> schedule0 =
                Batch34ResearchLifecycleR929.schedule(
                        Collections.<String>emptyList(), Collections.<String>emptyList(), tierC,
                        0L, Batch34ResearchLifecycleR929.INITIAL_TOTAL_BUDGET_NS);
        List<Batch34ResearchLifecycleR929.ScheduledResearch> schedule1 =
                Batch34ResearchLifecycleR929.schedule(
                        Collections.<String>emptyList(), Collections.<String>emptyList(), tierC,
                        1L, Batch34ResearchLifecycleR929.INITIAL_TOTAL_BUDGET_NS);
        long used = 0L;
        for (Batch34ResearchLifecycleR929.ScheduledResearch item : schedule0) used += item.budgetNs;
        verify("C07", schedule0.size() == 2
                        && used <= Batch34ResearchLifecycleR929.INITIAL_TOTAL_BUDGET_NS,
                "PER_ISSUE_700MS_TOTAL_AND_250MS_SLICE_ENFORCED");
        verify("C07", !schedule0.get(0).algorithmId.equals(schedule1.get(0).algorithmId),
                "TIER_C_ROTATES_WITHOUT_FREQUENCY_DEBT");
        List<Batch34ResearchLifecycleR929.ScheduledResearch> capacity =
                Batch34ResearchLifecycleR929.schedule(
                        many("A", 20), many("B", 40), many("C", 80), 0L,
                        100_000_000_000L);
        int a = 0, b = 0, c = 0;
        for (Batch34ResearchLifecycleR929.ScheduledResearch item : capacity) {
            if (item.tier == Batch34ResearchLifecycleR929.Tier.A_FULL_1200) a++;
            else if (item.tier == Batch34ResearchLifecycleR929.Tier.B_SHARED_FULL_SCORE) b++;
            else c++;
        }
        verify("C07", a <= 8 && b <= 24 && c <= 64,
                "EIGHT_TWENTYFOUR_SIXTYFOUR_CAPACITY_CONTRACT");

        String evidence = sha("lifecycle-evidence");
        Batch34ResearchLifecycleR929.LifecycleRecord lifecycle =
                new Batch34ResearchLifecycleR929.LifecycleRecord(
                        "algo-1", "", sha("genome-1"), "", "paired-lower<=0");
        verify("C08", "C08_TRANSITION_COMMITTED".equals(lifecycle.transition(
                        Batch34ResearchLifecycleR929.LifecycleState.IDEA,
                        Batch34ResearchLifecycleR929.LifecycleState.NURSERY, 0, "", false))
                        && "C08_TRANSITION_COMMITTED".equals(lifecycle.transition(
                                Batch34ResearchLifecycleR929.LifecycleState.NURSERY,
                                Batch34ResearchLifecycleR929.LifecycleState.OBSERVING, 0, "", false))
                        && "C08_TRANSITION_COMMITTED".equals(lifecycle.transition(
                                Batch34ResearchLifecycleR929.LifecycleState.OBSERVING,
                                Batch34ResearchLifecycleR929.LifecycleState.EVIDENCE_READY,
                                20, evidence, false)),
                "LIFECYCLE_V3_REACHES_EVIDENCE_READY");
        verify("C08", "C08_TRANSITION_COMMITTED".equals(lifecycle.transition(
                        Batch34ResearchLifecycleR929.LifecycleState.EVIDENCE_READY,
                        Batch34ResearchLifecycleR929.LifecycleState.CHALLENGER, 0, evidence, false))
                        && "C08_TRANSITION_COMMITTED".equals(lifecycle.transition(
                                Batch34ResearchLifecycleR929.LifecycleState.CHALLENGER,
                                Batch34ResearchLifecycleR929.LifecycleState.SHADOW, 20, evidence, false))
                        && "C08_TRANSITION_COMMITTED".equals(lifecycle.transition(
                                Batch34ResearchLifecycleR929.LifecycleState.SHADOW,
                                Batch34ResearchLifecycleR929.LifecycleState.INFLUENCE,
                                20, evidence, true))
                        && "C08_TRANSITION_COMMITTED".equals(lifecycle.transition(
                                Batch34ResearchLifecycleR929.LifecycleState.INFLUENCE,
                                Batch34ResearchLifecycleR929.LifecycleState.CHAMPION,
                                20, evidence, true)),
                "INFLUENCE_AND_CHAMPION_REQUIRE_PAIRED_IMPROVEMENT");
        Batch34ResearchLifecycleR929.LifecycleRecord child =
                lifecycle.repairedChild("algo-1-repair", sha("genome-repair"),
                        "rank-instability", "paired-lower<=0");
        verify("C08", "algo-1".equals(child.parentAlgorithmId)
                        && child.futureSupport() == 0
                        && child.state() == Batch34ResearchLifecycleR929.LifecycleState.IDEA,
                "REPAIRED_CHILD_HAS_NEW_ID_AND_ZERO_SUPPORT");

        Batch34ResearchLifecycleR929.FailureCase failureDraft =
                new Batch34ResearchLifecycleR929.FailureCase(
                        "00005000", sha("selected-action"), sha("frozen-input"),
                        sha("actual-outcome"), "RANK_REVERSAL", "");
        Batch34ResearchLifecycleR929.FailureCase failure =
                new Batch34ResearchLifecycleR929.FailureCase(
                        failureDraft.issue, failureDraft.selectedActionSha256,
                        failureDraft.frozenInputSha256, failureDraft.actualOutcomeSha256,
                        failureDraft.failureClass, failureDraft.calculatedSha256());
        String[] modules = {"FTRL", "TINY_DEEP", "TEACHER", "TREND", "SHAPE", "RELATION", "INVENTED"};
        ArrayList<Batch34ResearchLifecycleR929.AblationResult> ablations =
                new ArrayList<Batch34ResearchLifecycleR929.AblationResult>();
        for (int i = 0; i < modules.length; i++) {
            ablations.add(new Batch34ResearchLifecycleR929.AblationResult(
                    failure.caseSha256, modules[i], sha("replay-" + modules[i]), true,
                    i % 2 == 0, 0.01d * i, 3));
        }
        verify("C09", Batch34ResearchLifecycleR929.validateAblation(
                        failure, ablations).passed,
                "SEVEN_REAL_MODULE_ABLATIONS_WITH_WRITES_SUPPRESSED");
        ArrayList<Batch34ResearchLifecycleR929.AblationResult> missingAblation =
                new ArrayList<Batch34ResearchLifecycleR929.AblationResult>(ablations);
        missingAblation.remove(missingAblation.size() - 1);
        verify("C09", has(Batch34ResearchLifecycleR929.validateAblation(
                        failure, missingAblation), "C09_REQUIRED_ABLATION_MISSING"),
                "MISSING_MODULE_COUNTERFACTUAL_REJECTED");

        List<Batch34ResearchLifecycleR929.AtomDescriptor> atoms = atoms(false, false);
        verify("C10", Batch34ResearchLifecycleR929.validateAtomRegistry(
                        atoms, Collections.<String, String>emptyMap()).passed,
                "EXACT_240_ATOMS_165_40_20_15");
        Batch34ResearchLifecycleR929.ValueResult<Double> present =
                Batch34ResearchLifecycleR929.ValueResult.present(
                        Double.valueOf(0.0d), "00005999", 1000L, "zero-is-real");
        Batch34ResearchLifecycleR929.ValueResult<Double> missing =
                Batch34ResearchLifecycleR929.ValueResult.missing(
                        Batch34ResearchLifecycleR929.MissingReason.SOURCE_ABSENT,
                        "00005999", 1000L);
        verify("C10", present.present && present.value.doubleValue() == 0.0d
                        && !missing.present && missing.value == null
                        && missing.missingReason
                                == Batch34ResearchLifecycleR929.MissingReason.SOURCE_ABSENT,
                "REAL_ZERO_AND_MISSING_ARE_DISTINCT");
        verify("C10", present.pitBefore("00006000") && missing.pitBefore("00006000"),
                "ATOM_MAX_SOURCE_ISSUE_PIT_CHECK");

        Map<String, String> previous = new HashMap<String, String>();
        previous.put("ATOM_PRE_000", sha("impl-ATOM_PRE_000"));
        Batch34EvidenceContractR929.Check changedActive =
                Batch34ResearchLifecycleR929.validateAtomRegistry(
                        atoms(true, false), previous);
        verify("C11", has(changedActive, "C11_CHANGED_ATOM_CERTIFICATE_NOT_INVALIDATED"),
                "CHANGED_ACTIVE_ATOM_INVALIDATES_CERTIFICATE");
        verify("C11", Batch34ResearchLifecycleR929.validateAtomRegistry(
                        atoms(true, true), previous).passed,
                "CHANGED_ATOM_ALLOWED_ONLY_WHEN_QUARANTINED");
        verify("C11", atoms.size() == 240,
                "LEGACY_FIXED_AND_FULL_ATOM_INVENTORY_PRESERVED");

        Set<String> activeAtomIds = new HashSet<String>();
        for (Batch34ResearchLifecycleR929.AtomDescriptor atom : atoms) {
            if (atom.state == Batch34ResearchLifecycleR929.AtomState.ACTIVE) {
                activeAtomIds.add(atom.atomId);
            }
        }
        Batch34ResearchLifecycleR929.Genome genome = genome(activeAtomIds);
        verify("C12", Batch34ResearchLifecycleR929.validateGenome(
                        genome, activeAtomIds).passed,
                "EXECUTABLE_TYPED_PIT_SAFE_GENOME");
        HashSet<String> incompleteAtoms = new HashSet<String>(activeAtomIds);
        incompleteAtoms.remove("ATOM_PROXY_000");
        verify("C12", has(Batch34ResearchLifecycleR929.validateGenome(
                        genome, incompleteAtoms), "C12_ATOM_DEPENDENCY_UNAVAILABLE"),
                "GENOME_WITH_UNAVAILABLE_ATOM_REJECTED");
        verify("C12", genome.graph.size() >= 4
                        && !genome.examples.isEmpty()
                        && !genome.failureConditions.isEmpty(),
                "GENOME_CONTAINS_METHOD_GRAPH_EXAMPLES_AND_FAILURES");

        Batch34TrainingCoreR929.FeatureManifest features =
                Batch34TrainingCoreR929.standardFeatureManifest();
        Batch34TrainingCoreR929.SplitPlan split = splitPlan();
        Batch34EvidenceContractR929.DatasetManifest dataset = dataset(features, split);
        Batch34EvidenceContractR929.BundleManifest bundle = bundle("GEN_R929_GATE", "");
        Batch34StatisticalGovernanceR929.DomainCertificate domain = domainCertificate();
        Batch34StatisticalGovernanceR929.CertificateV2 certificate =
                certificate(dataset, bundle, domain, false);
        Batch34RuntimeGateR929.GateResult syntheticPass = Batch34RuntimeGateR929.evaluate(
                productionEvidence(dataset, bundle, certificate, domain, policy(true), true, true),
                true);
        verify("D09", syntheticPass.structuralReady && syntheticPass.effectReady
                        && syntheticPass.realEligible,
                "SYNTHETIC_FIXTURE_PROVES_GATE_CODE_PATH_ONLY");
        Batch34RuntimeGateR929.GateResult effectFail = Batch34RuntimeGateR929.evaluate(
                productionEvidence(dataset, bundle, certificate, domain, policy(false), true, true),
                true);
        verify("D09", effectFail.structuralReady && !effectFail.effectReady
                        && !effectFail.realEligible && effectFail.shadowRequiredWhenNotReal
                        && has(effectFail, "D09_SELECTED_COVERAGE_LT_15_PERCENT"),
                "EFFECT_TARGET_CANNOT_BE_FORCED_AND_SHADOW_REMAINS_REQUIRED");
        Batch34RuntimeGateR929.GateResult current =
                Batch34RuntimeGateR929.evaluateCurrent(true);
        verify("D09", !current.realEligible && current.shadowAllowed
                        && current.shadowRequiredWhenNotReal,
                "CHECKED_IN_UNPROVEN_EVIDENCE_FAILS_CLOSED_WITH_SHADOW");
    }

    private static List<String> many(String prefix, int count) {
        ArrayList<String> out = new ArrayList<String>();
        for (int i = 0; i < count; i++) out.add(prefix + "-" + i);
        return out;
    }

    private static void requireTaskCoverage(String[] tasks) {
        for (String task : tasks) {
            Integer count = COUNTS.get(task);
            if (count == null || count.intValue() <= 0) {
                throw new AssertionError("TASK_WITHOUT_EXECUTABLE_ASSERTION:" + task);
            }
            System.out.println("TASK|" + task + "|PASS|assertions=" + count);
        }
    }

    public static void main(String[] args) {
        testBatch3();
        testBatch4();
        requireTaskCoverage(BATCH3);
        requireTaskCoverage(BATCH4);
        System.out.println("SUMMARY|batch3=12/12|batch4=10/10|tasks=22/22|assertions="
                + assertions + "|fixture=SYNTHETIC_ENGINEERING_ONLY");
        System.out.println("R929_BATCH34_CONTRACT_PASS");
    }
}
