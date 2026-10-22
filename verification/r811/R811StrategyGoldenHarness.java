package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, dependency-free strategy golden-vector generator.
 *
 * <p>This source deliberately uses only pre-R811 production types so the exact same harness can be
 * compiled at the locked R3.6.7 baseline and at R3.6.8-811. Diagnostic timings and Research output
 * are intentionally outside the canonical line.
 */
public final class R811StrategyGoldenHarness {
    private static final int DEFAULT_CASES = 100;

    public static void main(String[] args) {
        int cases = args.length == 0 ? DEFAULT_CASES : Integer.parseInt(args[0]);
        if (cases < 1 || cases > 1000) throw new IllegalArgumentException("R811_GOLDEN_CASES");
        for (int caseId = 0; caseId < cases; caseId++) {
            Golden g = evaluate(caseId);
            System.out.println(g.canonical());
        }
    }

    private static Golden evaluate(int caseId) {
        List<Models.Draw> source = history(caseId);
        V51Models.DrawFrame[] frames = new V51Models.DrawFrame[source.size()];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = V51DrawAdapter.convert(source.get(i));
            require(frames[i] != null, "DRAW_ADAPT_" + caseId + '_' + i);
        }
        long createdAt = 1786400000000L + caseId * 1000L;
        String baseIssue = frames[frames.length - 1].issue;
        String targetIssue = TargetIssueResolver.next(baseIssue).targetIssue;
        String historyHash = V51PermutationFeatureEngine.historyHash(frames);
        V51Models.PredictionContext context =
                new V51Models.PredictionContext(
                        baseIssue,
                        targetIssue,
                        baseIssue,
                        historyHash,
                        V51BuildManifest.COHORT_ID,
                        V51ArtifactBundleManifest.BUNDLE_HASH,
                        createdAt,
                        frames.length);

        RollingWindowCache.shared().clear();
        FullInformationFeatureGraphEngine.shared().clear();
        V51PermutationFeatureEngine featureEngine = new V51PermutationFeatureEngine();
        V51Models.PermutationFeatureSnapshot features = featureEngine.build(context, frames);
        V51Models.PermutationFeatureSnapshot previous = previousFeatures(featureEngine, frames, createdAt);
        RollingWindowCache.SnapshotSet windows = RollingWindowCache.shared().getOrBuild(frames);
        require(windows.validate().ok, "WINDOWS_" + caseId);
        FullInformationFeatureGraphEngine.FrozenFeatureGraph graph =
                FullInformationFeatureGraphEngine.shared()
                        .build(
                                context,
                                frames,
                                features.globalFeatures,
                                features.numberFeatures,
                                windows);
        require(graph.valid() && graph.featureHash.equals(graph.replayHash()), "GRAPH_" + caseId);
        String featureContractHash =
                V51ChecksumEngine.sha256(
                        features.checksum + '|' + graph.featureHash + '|' + windows.checksum);

        V51StateTrajectoryEngine.TrajectoryBundle stateBundle =
                new V51StateTrajectoryEngine().buildAudited(features, previous);
        require(stateBundle.valid, "STATE_" + caseId);
        V51Models.StateTrajectorySnapshot[] states = stateBundle.states;
        WindowFeatureEngine.B05WindowEvidenceBundle windowEvidence =
                new WindowFeatureEngine().buildB05Evidence(frames, states);
        TrendStateMachine.B05TrendEvidenceBundle trendEvidence =
                new TrendStateMachine().buildB05Evidence(frames, states);
        BreakRecoveryEngine.B05BreakEvidenceBundle breakEvidence =
                new BreakRecoveryEngine().buildB05Evidence(frames, states);
        CycleRhythmEngine.B05CycleEvidenceBundle cycleEvidence =
                new CycleRhythmEngine().buildB05Evidence(frames);
        DistributionDriftEngine.B05DriftEvidenceBundle driftEvidence =
                new DistributionDriftEngine().buildB05Evidence(frames);
        require(
                windowEvidence.valid
                        && trendEvidence.valid
                        && breakEvidence.valid
                        && cycleEvidence.valid
                        && driftEvidence.valid,
                "EVIDENCE_" + caseId);
        String evidenceHash =
                V51ChecksumEngine.sha256(
                        stateBundle.checksum
                                + '|'
                                + windowEvidence.checksum
                                + '|'
                                + trendEvidence.checksum
                                + '|'
                                + breakEvidence.checksum
                                + '|'
                                + cycleEvidence.checksum
                                + '|'
                                + driftEvidence.checksum);
        featureContractHash = V51ChecksumEngine.sha256(featureContractHash + '|' + evidenceHash);
        String stateHash = stateHash(states);

        V51PermutationNumberModelEngine.Result numberModel =
                new V51PermutationNumberModelEngine().predict(features, states, frames);
        require(numberModel.matrix != null, "NUMBER_" + caseId);
        V51EffectiveConsensusEngine.Consensus[] consensus =
                new V51EffectiveConsensusEngine()
                        .evaluate(numberModel.experts, states, new V51ExpertRegretEngine());
        V51Models.PositionAction[] baseActions =
                new V51PositionActionBuilder().build(numberModel.matrix, states, consensus);
        K3ActionSpaceEngine.validateOrThrow(baseActions);
        K3FunnelOrchestrator.Audit funnel =
                new K3FunnelOrchestrator().evaluate(numberModel.matrix, baseActions);
        require(funnel.formalActionCount == 1200 && funnel.neverDeleteFormalAction, "FUNNEL_" + caseId);
        double[][] meta =
                new V51ActionMetaFeatureEngine()
                        .build(
                                baseActions,
                                features,
                                states,
                                numberModel.experts,
                                numberModel.gateWeight,
                                consensus);
        V51ActionRankerEngine.RankResult rank =
                new V51ActionRankerEngine().rankDetailed(baseActions, meta);
        V51Models.PositionAction[] actions = rank.ranked;
        V51Models.PositionAction rankOne = rank.rankOne;
        require(actions.length == 1200 && rankOne != null, "RANK_" + caseId);
        String actionUniverseHash = K3ActionSpaceEngine.actionSpaceHash(actions);
        String rankOrderHash = hashActions(actions);
        K3ActionSpaceEngine.FrozenChampion champion =
                new K3ActionSpaceEngine().freeze(rankOne, actions, createdAt);

        V51Models.StateTrajectorySnapshot state = states[rankOne.position - 1];
        V51Models.DomainCertificate domain = new V51DomainCertificateEngine().certify(state);
        V51TianshuEvidenceBridge.Result tianshu =
                new V51TianshuEvidenceBridge()
                        .evaluate(
                                null,
                                source,
                                context,
                                rankOne,
                                state,
                                numberModel.matrix.probability[rankOne.position - 1]);
        V51RankStabilityEngine.Result stability =
                new V51RankStabilityEngine().evaluate(rankOne, actions);
        double falsifier =
                new V51FalsifierExpert()
                        .score(
                                state,
                                1.0d - consensus[rankOne.position - 1].effective,
                                1.0d - stability.retention,
                                1.0d - stability.thirdRobustness,
                                Math.max(tianshu.risk, 1.0d - tianshu.confidence),
                                tianshu.hardRisk == 1
                                        ? 1.0d
                                        : Math.max(0.0d, 0.50d - tianshu.support));
        V51UncertaintyEngine.Breakdown uncertainty =
                new V51UncertaintyEngine()
                        .evaluate(
                                numberModel.matrix.probability[rankOne.position - 1],
                                consensus[rankOne.position - 1],
                                state,
                                domain,
                                numberModel.matrix);
        V51Models.PredictabilityEvidence predictability =
                new V51PredictabilityEngine()
                        .evaluate(
                                rankOne,
                                consensus[rankOne.position - 1],
                                state,
                                stability,
                                0.5d,
                                0.5d,
                                0.5d,
                                falsifier);
        V51SelectedActionCalibrationEngine.CalibrationResult calibration =
                new V51SelectedActionCalibrationEngine()
                        .calibrateDetailed(
                                rankOne,
                                consensus[rankOne.position - 1],
                                state,
                                domain,
                                uncertainty,
                                falsifier,
                                predictability.probability);
        String selectedContractHash =
                V51ChecksumEngine.sha256(
                        targetIssue
                                + '|'
                                + rankOne.checksum
                                + '|'
                                + calibration.selectedHitP
                                + '|'
                                + calibration.status.name()
                                + '|'
                                + predictability.checksum
                                + '|'
                                + domain.checksum
                                + '|'
                                + V51ArtifactBundleManifest.BUNDLE_HASH);
        V51Models.SelectedActionEnvelope selected =
                new V51Models.SelectedActionEnvelope(
                        context,
                        rankOne,
                        calibration.selectedHitP,
                        predictability,
                        domain,
                        V51Enums.Lane.SHADOW,
                        uncertainty.total,
                        falsifier,
                        selectedContractHash);
        V51SelectiveRiskController.Result policy =
                new V51SelectiveRiskController()
                        .decide(selected, consensus[rankOne.position - 1], stability);
        selected =
                new V51Models.SelectedActionEnvelope(
                        context,
                        rankOne,
                        calibration.selectedHitP,
                        predictability,
                        domain,
                        policy.lane,
                        uncertainty.total,
                        falsifier,
                        selectedContractHash);
        V51CompiledActionContract compiled =
                new V51CompiledActionContract(
                        selected,
                        policy,
                        stability,
                        featureContractHash,
                        stateHash,
                        expertHash(numberModel.experts),
                        numberModel.matrix.checksum,
                        hashActions(actions));
        V51Models.AuthorizedDecision authorized =
                new DecisionGateEngine().decideV51B12(null, compiled);
        require(authorized != null, "AUTH_" + caseId);
        return new Golden(
                targetIssue,
                historyHash,
                featureContractHash,
                stateHash,
                numberModel.matrix.checksum,
                actionUniverseHash,
                rankOrderHash,
                champion.championHash,
                selectedContractHash,
                authorized.action.name(),
                authorized.rejectCode.name());
    }

    private static List<Models.Draw> history(int caseId) {
        int rows = 24 + caseId % 25;
        int multiplier = new int[] {1, 3, 7, 9}[caseId % 4];
        int shift = 1 + (caseId % 9);
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>(rows);
        for (int row = 0; row < rows; row++) {
            Models.Draw draw = new Models.Draw();
            draw.issue = "20260801" + four(row + 1);
            draw.openTime = 1785528000000L + row * 60000L;
            for (int position = 0; position < 10; position++)
                draw.numbers[position] =
                        Math.floorMod(position * multiplier + row * shift + caseId * 3, 10) + 1;
            require(draw.isValid(), "FIXTURE_" + caseId + '_' + row);
            out.add(draw);
        }
        return out;
    }

    private static V51Models.PermutationFeatureSnapshot previousFeatures(
            V51PermutationFeatureEngine engine, V51Models.DrawFrame[] history, long createdAt) {
        if (history.length <= 12) return null;
        V51Models.DrawFrame[] prior = new V51Models.DrawFrame[history.length - 1];
        System.arraycopy(history, 0, prior, 0, prior.length);
        V51Models.PredictionContext context =
                new V51Models.PredictionContext(
                        prior[prior.length - 1].issue,
                        history[history.length - 1].issue,
                        prior[prior.length - 1].issue,
                        V51PermutationFeatureEngine.historyHash(prior),
                        V51BuildManifest.COHORT_ID,
                        V51ArtifactBundleManifest.BUNDLE_HASH,
                        createdAt,
                        prior.length);
        return engine.build(context, prior);
    }

    private static String stateHash(V51Models.StateTrajectorySnapshot[] values) {
        StringBuilder out = new StringBuilder();
        for (V51Models.StateTrajectorySnapshot value : values) out.append(value.checksum);
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static String expertHash(V51Models.ExpertEvidence[] values) {
        StringBuilder out = new StringBuilder();
        for (V51Models.ExpertEvidence value : values) out.append(value.checksum);
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static String hashActions(V51Models.PositionAction[] values) {
        StringBuilder out = new StringBuilder();
        for (V51Models.PositionAction value : values) out.append(value.checksum);
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static String four(int value) {
        String x = String.valueOf(value);
        while (x.length() < 4) x = '0' + x;
        return x;
    }

    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalStateException("R811_GOLDEN_" + code);
    }

    private static final class Golden {
        final String targetIssue;
        final String historyHash;
        final String featureContractHash;
        final String stateHash;
        final String numberMatrixChecksum;
        final String actionUniverseHash;
        final String rankOrderHash;
        final String championChecksum;
        final String selectedContractHash;
        final String authorizationAction;
        final String rejectCode;

        Golden(
                String targetIssue,
                String historyHash,
                String featureContractHash,
                String stateHash,
                String numberMatrixChecksum,
                String actionUniverseHash,
                String rankOrderHash,
                String championChecksum,
                String selectedContractHash,
                String authorizationAction,
                String rejectCode) {
            this.targetIssue = targetIssue;
            this.historyHash = historyHash;
            this.featureContractHash = featureContractHash;
            this.stateHash = stateHash;
            this.numberMatrixChecksum = numberMatrixChecksum;
            this.actionUniverseHash = actionUniverseHash;
            this.rankOrderHash = rankOrderHash;
            this.championChecksum = championChecksum;
            this.selectedContractHash = selectedContractHash;
            this.authorizationAction = authorizationAction;
            this.rejectCode = rejectCode;
        }

        String canonical() {
            return targetIssue
                    + '|'
                    + historyHash
                    + '|'
                    + featureContractHash
                    + '|'
                    + stateHash
                    + '|'
                    + numberMatrixChecksum
                    + '|'
                    + actionUniverseHash
                    + '|'
                    + rankOrderHash
                    + '|'
                    + championChecksum
                    + '|'
                    + selectedContractHash
                    + '|'
                    + authorizationAction
                    + '|'
                    + rejectCode;
        }
    }
}
