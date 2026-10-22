package com.ethk3.novaonepro;

import java.util.ArrayList;

/**
 * Host workload probe for the live 96-row PIT core. It verifies the complete 100-number/1200-action
 * computation and synchronous evidence bridge without claiming equivalence to an Android device.
 */
public final class R816CorePerformanceHarness {
    public static void main(String[] args) {
        int historyCount = 96;
        if (args != null && args.length > 0) {
            try {
                historyCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
                historyCount = 96;
            }
        }
        if (historyCount < 12 || historyCount > 240)
            throw new IllegalArgumentException("R816_PERFORMANCE_HISTORY_RANGE");
        RuntimeMemoryGuardR811.recoverAfterFailure();
        long usedBefore = used();
        long started = System.nanoTime();
        ArrayList<Models.Draw> rows = new ArrayList<Models.Draw>();
        for (int i = 1; i <= historyCount; i++) rows.add(draw(i));
        AnalysisSingleFlightR36.Request request =
                new AnalysisSingleFlightR36.Request(
                        issue(historyCount),
                        issue(historyCount + 1),
                        816L,
                        1700000000816L,
                        1700000060816L,
                        "R816_PERFORMANCE",
                        V51ChecksumEngine.sha256("R816_PERFORMANCE_REQUEST"),
                        "");
        V51RuntimeInputR36 input = V51RuntimeInputR36.freeze(rows, request);
        V51Models.DrawFrame[] history = input.frozenFramesR36();
        V51Models.PredictionContext context =
                new V51Models.PredictionContext(
                        input.baseIssue,
                        input.targetIssue,
                        input.baseIssue,
                        input.historyHash,
                        V51BuildManifest.COHORT_ID,
                        V51ArtifactBundleManifest.BUNDLE_HASH,
                        request.requestAt,
                        history.length);

        long featuresAt = System.nanoTime();
        V51PermutationFeatureEngine featureEngine = new V51PermutationFeatureEngine();
        V51Models.PermutationFeatureSnapshot features = featureEngine.build(context, history);
        V51Models.DrawFrame[] prior = new V51Models.DrawFrame[history.length - 1];
        System.arraycopy(history, 0, prior, 0, prior.length);
        V51Models.PredictionContext priorContext =
                new V51Models.PredictionContext(
                        prior[prior.length - 1].issue,
                        history[history.length - 1].issue,
                        prior[prior.length - 1].issue,
                        V51PermutationFeatureEngine.historyHash(prior),
                        V51BuildManifest.COHORT_ID,
                        V51ArtifactBundleManifest.BUNDLE_HASH,
                        request.requestAt,
                        prior.length);
        V51Models.PermutationFeatureSnapshot previous = featureEngine.build(priorContext, prior);
        RollingWindowCache.SnapshotSet windows = RollingWindowCache.shared().getOrBuild(history);
        FullInformationFeatureGraphEngine.FrozenFeatureGraph graph =
                FullInformationFeatureGraphEngine.shared()
                        .build(
                                context,
                                history,
                                features.globalFeatures,
                                features.numberFeatures,
                                windows);
        require(features.valid() && windows.validate().ok && graph.valid(), "FEATURE_CORE");
        long featureMs = elapsed(featuresAt);

        long stateAt = System.nanoTime();
        V51StateTrajectoryEngine.TrajectoryBundle stateBundle =
                new V51StateTrajectoryEngine().buildAudited(features, previous);
        V51Models.StateTrajectorySnapshot[] states = stateBundle.states;
        require(stateBundle.valid, "STATE_CORE");
        require(new WindowFeatureEngine().buildB05Evidence(history, states).valid, "WINDOW_EVIDENCE");
        require(new TrendStateMachine().buildB05Evidence(history, states).valid, "TREND_EVIDENCE");
        require(new BreakRecoveryEngine().buildB05Evidence(history, states).valid, "BREAK_EVIDENCE");
        require(new CycleRhythmEngine().buildB05Evidence(history).valid, "CYCLE_EVIDENCE");
        require(new DistributionDriftEngine().buildB05Evidence(history).valid, "DRIFT_EVIDENCE");
        long stateMs = elapsed(stateAt);

        long numberAt = System.nanoTime();
        V51PermutationNumberModelEngine.Result numberModel =
                new V51PermutationNumberModelEngine().predict(features, states, history);
        require(numberModel.matrix != null, "NUMBER_100");
        long numberMs = elapsed(numberAt);

        long actionAt = System.nanoTime();
        V51EffectiveConsensusEngine.Consensus[] consensus =
                new V51EffectiveConsensusEngine()
                        .evaluate(numberModel.experts, states, new V51ExpertRegretEngine());
        V51Models.PositionAction[] baseActions =
                new V51PositionActionBuilder().build(numberModel.matrix, states, consensus);
        K3ActionSpaceEngine.validateOrThrow(baseActions);
        K3FunnelOrchestrator.Audit funnel =
                new K3FunnelOrchestrator().evaluate(numberModel.matrix, baseActions);
        double[][] meta =
                new V51ActionMetaFeatureEngine()
                        .build(
                                baseActions,
                                features,
                                states,
                                numberModel.experts,
                                numberModel.gateWeight,
                                consensus);
        V51ActionRankerEngine.RankResult ranked =
                new V51ActionRankerEngine().rankDetailed(baseActions, meta);
        require(
                funnel.formalActionCount == 1200
                        && ranked.ranked.length == 1200
                        && ranked.rankOne != null,
                "ACTION_1200_RANK1");
        long actionMs = elapsed(actionAt);

        long bridgeAt = System.nanoTime();
        V51Models.PositionAction rankOne = ranked.rankOne;
        V51TianshuEvidenceBridge.Result bridge =
                new V51TianshuEvidenceBridge()
                        .evaluate(
                                null,
                                input.history,
                                context,
                                rankOne,
                                states[rankOne.position - 1],
                                numberModel.matrix.probability[rankOne.position - 1]);
        require(bridge != null && bridge.fingerprint.length() == 64, "EVIDENCE_BRIDGE");
        long bridgeMs = elapsed(bridgeAt);
        long totalMs = elapsed(started);
        long usedAfter = used();
        long delta = Math.max(0L, usedAfter - usedBefore);
        require(totalMs < 30000L, "HOST_WORKLOAD_NOT_STALLED");
        System.out.println(
                "R816_CORE_PERFORMANCE=PASS|HISTORY="
                        + historyCount
                        + "|NUMBER=100|ACTION=1200|RANK1=1"
                        + "|FEATURE_MS="
                        + featureMs
                        + "|STATE_MS="
                        + stateMs
                        + "|NUMBER_MS="
                        + numberMs
                        + "|ACTION_MS="
                        + actionMs
                        + "|BRIDGE_MS="
                        + bridgeMs
                        + "|TOTAL_MS="
                        + totalMs
                        + "|HEAP_DELTA_BYTES="
                        + delta
                        + "|TRUTH=HOST_ONLY_DEVICE_RETEST_REQUIRED");
    }

    private static Models.Draw draw(int sequence) {
        Models.Draw draw = new Models.Draw();
        draw.issue = issue(sequence);
        draw.openTime = 1700000000000L + sequence * 60000L;
        draw.fetchTime = draw.openTime + 1000L;
        for (int position = 0; position < 10; position++) {
            draw.numbers[position] = ((position + sequence * 3) % 10) + 1;
        }
        return draw;
    }

    private static String issue(int sequence) {
        String value = String.valueOf(sequence);
        while (value.length() < 4) value = "0" + value;
        return "20260824" + value;
    }

    private static long elapsed(long startNanos) {
        return Math.max(0L, System.nanoTime() - startNanos) / 1000000L;
    }

    private static long used() {
        Runtime runtime = Runtime.getRuntime();
        return Math.max(0L, runtime.totalMemory() - runtime.freeMemory());
    }

    private static void require(boolean value, String code) {
        if (!value) throw new AssertionError("R816_PERFORMANCE_FAILED:" + code);
    }
}
