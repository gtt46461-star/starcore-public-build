package com.ethk3.novaonepro;

/** Dependency-free host behavior checks used when no Android emulator exists. */
public final class R36HostContractHarness {
    public static void main(String[] args) {
        issueContract();
        singleFlightContract();
        actionSpaceContract();
        startupClosureContract();
        databaseUpgradeContract();
        runtimeHistoryHashParityContract();
        historyWarmupPolicyContract();
        preCommitPipelineContract();
        upgradeRearmPolicyContract();
        durableLeasePolicyContract();
        executionFenceContract();
        diagnosisReportContract();
        immutableRedlines();
        System.out.println(
                "R36_HOST_CONTRACTS=PASS|T01_SCHEDULER_SUBSET,T02,T03,T04,T05,T27_STATIC,T28_ENUMERATION,T35_DIAGNOSIS_DAG,T36_FULL_REPORT,T37_STARTUP_TRUTH,T38_DB35_DB36_ADDITIVE,T39_HISTORY_HASH_PARITY,T40_HISTORY_BOOTSTRAP_POLICY,T41_B04_B10_PRECOMMIT,T42_R363_REQUEST_REARM,T43_R366_DURABLE_LEASE_POLICY,T44_R366_EXECUTION_FENCE,T45_R366_1200_ACTION_SNAPSHOT_ROUNDTRIP");
    }

    private static void issueContract() {
        TargetIssueResolver.Resolution x = TargetIssueResolver.next("202608101440");
        require(x.valid && "202608110001".equals(x.targetIssue), "T05_ROLLOVER");
        require(TargetIssueResolver.distance("202608101440", "202608110001") == 1, "T05_DISTANCE");
        require(!TargetIssueResolver.isValid("202608101441"), "T05_INVALID_SEQUENCE");
    }

    private static void singleFlightContract() {
        AnalysisSingleFlightR36 f = AnalysisSingleFlightR36.shared();
        f.resetForTests();
        AnalysisSingleFlightR36.SubmitResult owner =
                f.submit(request("202608100001", "202608100002", 1));
        for (int i = 3; i <= 100; i++)
            f.submit(request("20260810" + four(i - 1), "20260810" + four(i), i));
        require(f.activeCount() == 1 && f.maximumObserved() == 1, "T01_CONCURRENCY");
        require("202608100100".equals(f.pendingTarget()), "T01_LATEST_PENDING");
        AnalysisSingleFlightR36.Request next = f.releaseAndTakeNext(owner.lease);
        require(next != null && "202608100100".equals(next.targetIssue), "T04_RELEASE_LATEST");
        AnalysisSingleFlightR36.SubmitResult successor = f.submit(next);
        require(successor.startNow && f.activeCount() == 1, "T04_SUCCESSOR");
        f.releaseAndTakeNext(successor.lease);

        f.resetForTests();
        owner = f.submit(request("202608100100", "202608100101", 101));
        long at = owner.lease.token.lastProgressAt();
        require(!f.cancelIfNoProgress(at + 12000L), "T02_NO_BUDGET_CANCEL");
        require(f.cancelIfNoProgress(at + 30000L), "T03_STALL_CANCEL");
        require(!f.cancelIfNoProgress(at + 60000L), "T03_CANCEL_ONCE");
        f.releaseAndTakeNext(owner.lease);

        f.resetForTests();
        owner = f.submit(request("202608100200", "202608100201", 201));
        final AnalysisSingleFlightR36.Lease deadLease = owner.lease;
        Thread deadWorker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                deadLease.bindCurrentThread();
                            }
                        },
                        "r368-host-dead-analysis-worker");
        deadWorker.start();
        try {
            deadWorker.join(5000L);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError("T04_DEAD_WORKER_JOIN", interrupted);
        }
        require(!deadWorker.isAlive(), "T04_DEAD_WORKER_PROOF");
        f.submit(request("202608100201", "202608100202", 202));
        AnalysisSingleFlightR36.ReapResult reaped =
                f.reapDeadWorker(System.currentTimeMillis(), true);
        require(reaped.reaped, "T04_DEAD_WORKER_REAP");
        require(reaped.released == deadLease, "T04_EXACT_DEAD_OWNER_RELEASED");
        require(
                reaped.next != null && "202608100202".equals(reaped.next.targetIssue),
                "T04_LATEST_PENDING_PRESERVED");
        require(f.currentLease() == null && f.activeCount() == 0, "T04_NO_GHOST_OWNER");
        f.resetForTests();
    }

    private static void actionSpaceContract() {
        double[][] p = new double[10][10];
        int[] map = new int[10];
        V51Models.StateTrajectorySnapshot[] states = new V51Models.StateTrajectorySnapshot[10];
        V51EffectiveConsensusEngine.Consensus[] consensus =
                new V51EffectiveConsensusEngine.Consensus[10];
        for (int i = 0; i < 10; i++) {
            map[i] = i + 1;
            for (int n = 0; n < 10; n++) p[i][n] = 0.1d;
            states[i] =
                    new V51Models.StateTrajectorySnapshot(
                            "202608100002",
                            i + 1,
                            "P" + i,
                            "D" + i,
                            V51Enums.Lifecycle.MATURE,
                            1,
                            0.5d,
                            1.0d,
                            0.1d,
                            0.8d,
                            "S" + i);
            consensus[i] = new V51EffectiveConsensusEngine.Consensus(1, 1, 1, 1, 1, 1, 1, 1, 1);
        }
        V51Models.NumberProbabilityMatrix matrix =
                new V51Models.NumberProbabilityMatrix(
                        p, 0, 0, 1, true, false, map, V51ChecksumEngine.sha256("MATRIX"));
        V51Models.PositionAction[] actions =
                new V51PositionActionBuilder().build(matrix, states, consensus);
        K3ActionSpaceEngine.validateOrThrow(actions);
        require(actions.length == 10 * 120, "T28_EXACT_1200");
        K3ActionSpaceEngine.ValidationReport actionAudit =
                K3ActionSpaceEngine.validateDetailed(actions);
        require(actionAudit.valid && actionAudit.uniqueRows == 1200, "T28_ACTION_UNIVERSE_AUDIT");
        require(K3ActionSpaceEngine.choose(10, 3) == 120, "T28_COMBINATION_FORMULA");
        require(K3ActionSpaceEngine.combinationOrdinal(1, 2, 3) == 0, "T28_FIRST_ORDINAL");
        require(K3ActionSpaceEngine.combinationOrdinal(8, 9, 10) == 119, "T28_LAST_ORDINAL");
        require(K3ActionSpaceEngine.globalOrdinal(10, 8, 9, 10) == 1199, "T28_GLOBAL_ORDINAL");
        K3FunnelOrchestrator.Audit funnel = new K3FunnelOrchestrator().evaluate(matrix, actions);
        require(
                funnel.neverDeleteFormalAction && funnel.formalActionCount == 1200,
                "T28_FUNNEL_NO_DELETE");
        require(funnel.allPathsNested && funnel.paths.length == 10, "T28_FUNNEL_NESTED");
        require(funnel.retained[0].length == 7, "T28_FUNNEL_SEVEN_STAGES");
        for (int stage = 0; stage < K3FunnelOrchestrator.STAGE_SIZES.length; stage++)
            require(
                    funnel.retained[0][stage].length == K3FunnelOrchestrator.STAGE_SIZES[stage],
                    "T28_FUNNEL_STAGE_" + stage);
        require(funnel.pathForPosition(1).finalThree.length == 3, "T28_FUNNEL_FINAL_K3");
    }

    private static void immutableRedlines() {
        require(!V51BuildManifest.MODEL_PROVEN, "T27_MODEL_UNPROVEN");
        require(!V51BuildManifest.REALTIME_PROVEN, "T27_REALTIME_UNPROVEN");
        require(
                !V51BuildManifest.SHADOW_ONLY
                        && !V51BuildManifest.REAL_BET_HARD_DISABLED
                        && V51BuildManifest.CANDIDATE_REAL_AUTH_ENABLED,
                "T27_CONTROLLED_CANDIDATE");
        require(!AppConfig.R36_REAL_BET_UNLOCK, "T27_LEGACY_UNLOCK_FALSE");
        require("Binance1-PK10".equals(V51BuildManifest.GAME_CODE), "T27_GAME");
    }

    private static void startupClosureContract() {
        long entered = 1786424743182L;
        ServiceStartupStateR363.Evidence reportEvidence =
                new ServiceStartupStateR363.Evidence(
                        true,
                        1786424759565L,
                        entered,
                        0L,
                        entered,
                        entered + 9L,
                        0L,
                        "STARTING",
                        "DB_CRITICAL_OPEN",
                        "");
        require(
                ServiceStartupStateR363.classify(reportEvidence, 45000L)
                        == ServiceStartupStateR363.Phase.INITIALIZING,
                "T37_FOREGROUND_ENTERED_NOT_NEVER_STARTED");
        require(
                ServiceStartupStateR363.preReadyStaleReason(reportEvidence, 45000L).length() == 0,
                "T37_STARTUP_GRACE_NOT_FALSE_STALE");
        require(
                "STARTING".equals(ServiceStartupStateR363.truthState(reportEvidence, 45000L)),
                "T37_STARTING_TRUTH");

        ServiceStartupStateR363.Evidence failed =
                new ServiceStartupStateR363.Evidence(
                        true,
                        entered + 20000L,
                        entered,
                        entered,
                        entered,
                        entered,
                        0L,
                        "FAILED",
                        "R36_SCHEMA",
                        "SQLiteException:broken");
        require(
                ServiceStartupStateR363.preReadyStaleReason(failed, 45000L)
                        .startsWith("SERVICE_INIT_FAILED[R36_SCHEMA]"),
                "T37_FAILURE_STAGE_EXPOSED");

        ServiceStartupStateR363.Evidence ready =
                new ServiceStartupStateR363.Evidence(
                        true,
                        entered + 30000L,
                        entered,
                        entered,
                        entered,
                        entered,
                        entered + 25000L,
                        "READY",
                        "BUSINESS_READY",
                        "");
        require(
                ServiceStartupStateR363.classify(ready, 45000L)
                        == ServiceStartupStateR363.Phase.READY,
                "T37_READY_DISTINCT_FROM_FOREGROUND");
    }

    private static void databaseUpgradeContract() {
        DatabaseUpgradePlanR363.Plan exact = DatabaseUpgradePlanR363.select(35, 36);
        require(
                exact.path == DatabaseUpgradePlanR363.Path.R35_TO_R36_ADDITIVE_ONLY,
                "T38_EXACT_DB35_DB36_PATH");
        require(!exact.runV50Migration && !exact.destructive, "T38_NO_FULL_RESCAN_NO_DELETE");
        require(exact.createR35 && exact.createR36, "T38_REQUIRED_SCHEMA_INCLUDED");
        DatabaseUpgradePlanR363.Plan legacy = DatabaseUpgradePlanR363.select(33, 36);
        require(
                legacy.path == DatabaseUpgradePlanR363.Path.LEGACY_RECONCILE
                        && legacy.runV50Migration,
                "T38_LEGACY_MIGRATION_PRESERVED");
    }

    private static void runtimeHistoryHashParityContract() {
        java.util.ArrayList<Models.Draw> history = new java.util.ArrayList<Models.Draw>();
        for (int row = 1; row <= 12; row++) {
            Models.Draw draw = new Models.Draw();
            draw.issue = "20260810" + four(row);
            draw.openTime = 1700000000000L + row * 60000L;
            for (int p = 0; p < 10; p++) draw.numbers[p] = ((p + row - 1) % 10) + 1;
            require(draw.isValid(), "T39_FIXTURE_VALID_" + row);
            history.add(draw);
        }
        AnalysisSingleFlightR36.Request request =
                request("202608100012", "202608100013", 39L);
        V51RuntimeInputR36 frozen = V51RuntimeInputR36.freeze(history, request);
        V51Models.DrawFrame[] frames = new V51Models.DrawFrame[frozen.history.size()];
        for (int i = 0; i < frames.length; i++) frames[i] = V51DrawAdapter.convert(frozen.history.get(i));
        String featureHash = V51PermutationFeatureEngine.historyHash(frames);
        require(frozen.historyHash.equals(featureHash), "T39_HISTORY_HASH_SINGLE_OWNER");
        V51Models.PredictionContext context =
                new V51Models.PredictionContext(
                        frozen.baseIssue,
                        frozen.targetIssue,
                        frozen.baseIssue,
                        frozen.historyHash,
                        "HOST_COHORT",
                        "HOST_ARTIFACTS",
                        1700000000000L,
                        frames.length);
        V51PermutationFeatureEngine.PointInTimeAudit audit =
                new V51PermutationFeatureEngine().auditPointInTime(context, frames);
        require(audit.passed, "T39_B04_PIT_HASH_PARITY");
        require(
                new V51PermutationFeatureEngine().build(context, frames).valid(),
                "T39_B04_FEATURE_BUILD");
    }

    private static void historyWarmupPolicyContract() {
        require(
                R36AnalysisHistoryPolicyR364.needsBootstrap(5),
                "T40_LATEST_FIVE_BOOTSTRAPS");
        require(R36AnalysisHistoryPolicyR364.usable(12), "T40_TWELVE_IS_USABLE");
        require(
                R36AnalysisHistoryPolicyR364.PREFERRED_BOOTSTRAP_HISTORY == 100,
                "T40_BOOTSTRAP_100");
        require(
                R36AnalysisHistoryPolicyR364.MAX_FROZEN_HISTORY == 240
                        && R36AnalysisHistoryPolicyR364.MIN_LIVE_HISTORY != 240,
                "T40_240_IS_MAX_NOT_MINIMUM");
    }

    private static void preCommitPipelineContract() {
        java.util.ArrayList<Models.Draw> history = new java.util.ArrayList<Models.Draw>();
        for (int row = 1; row <= 24; row++) {
            Models.Draw draw = new Models.Draw();
            draw.issue = "20260810" + four(row);
            draw.openTime = 1700000000000L + row * 60000L;
            for (int p = 0; p < 10; p++) draw.numbers[p] = ((p + row * 3 - 1) % 10) + 1;
            history.add(draw);
        }
        AnalysisSingleFlightR36.Request request =
                request("202608100024", "202608100025", 41L);
        V51RuntimeInputR36 frozen = V51RuntimeInputR36.freeze(history, request);
        V51Models.DrawFrame[] frames = new V51Models.DrawFrame[frozen.history.size()];
        for (int i = 0; i < frames.length; i++) frames[i] = V51DrawAdapter.convert(frozen.history.get(i));
        V51Models.PredictionContext context =
                new V51Models.PredictionContext(
                        frozen.baseIssue,
                        frozen.targetIssue,
                        frozen.baseIssue,
                        frozen.historyHash,
                        V51BuildManifest.COHORT_ID,
                        V51ArtifactBundleManifest.BUNDLE_HASH,
                        1700000000000L,
                        frames.length);
        V51PermutationFeatureEngine featureEngine = new V51PermutationFeatureEngine();
        V51Models.PermutationFeatureSnapshot features = featureEngine.build(context, frames);
        RollingWindowCache.shared().clear();
        RollingWindowCache.SnapshotSet windows = RollingWindowCache.shared().getOrBuild(frames);
        require(windows.validate().ok, "T41_B04_WINDOWS");
        FullInformationFeatureGraphEngine.shared().clear();
        FullInformationFeatureGraphEngine.FrozenFeatureGraph graph =
                FullInformationFeatureGraphEngine.shared()
                        .build(
                                context,
                                frames,
                                features.globalFeatures,
                                features.numberFeatures,
                                windows);
        require(graph.valid() && graph.featureHash.equals(graph.replayHash()), "T41_B04_GRAPH");
        V51StateTrajectoryEngine.TrajectoryBundle stateBundle =
                new V51StateTrajectoryEngine().buildAudited(features, null);
        require(stateBundle.valid, "T41_B05_STATE");
        V51Models.StateTrajectorySnapshot[] states = stateBundle.states;
        require(new WindowFeatureEngine().buildB05Evidence(frames, states).valid, "T41_B05_WINDOW");
        require(new TrendStateMachine().buildB05Evidence(frames, states).valid, "T41_B05_TREND");
        require(new BreakRecoveryEngine().buildB05Evidence(frames, states).valid, "T41_B05_BREAK");
        require(new CycleRhythmEngine().buildB05Evidence(frames).valid, "T41_B05_CYCLE");
        require(new DistributionDriftEngine().buildB05Evidence(frames).valid, "T41_B05_DRIFT");
        V51PermutationNumberModelEngine.Result numberModel =
                new V51PermutationNumberModelEngine().predict(features, states, frames);
        require(numberModel.matrix != null, "T41_NUMBER_100");
        V51EffectiveConsensusEngine.Consensus[] consensus =
                new V51EffectiveConsensusEngine()
                        .evaluate(numberModel.experts, states, new V51ExpertRegretEngine());
        V51Models.PositionAction[] baseActions =
                new V51PositionActionBuilder().build(numberModel.matrix, states, consensus);
        K3ActionSpaceEngine.validateOrThrow(baseActions);
        K3FunnelOrchestrator.Audit funnel =
                new K3FunnelOrchestrator().evaluate(numberModel.matrix, baseActions);
        require(funnel.formalActionCount == 1200, "T41_FUNNEL_1200");
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
        require(rank.rankOne != null && rank.ranked.length == 1200, "T41_RANK_1200");

        V51RuntimeResult runtime =
                new V51RuntimeResult(
                        true,
                        context,
                        features,
                        states,
                        numberModel,
                        consensus,
                        funnel,
                        rank.ranked,
                        null,
                        null,
                        null,
                        "SHADOW_HOST_FIXTURE",
                        1L,
                        V51ChecksumEngine.sha256("T45_RUNTIME_RESULT"));
        DeferredEvidenceSnapshotR36.Snapshot snapshot =
                DeferredEvidenceSnapshotR36.fromRuntimeResult(runtime, null);
        byte[] encoded = DeferredEvidenceSnapshotR36.encode(snapshot);
        DeferredEvidenceSnapshotR36.EncodedSnapshot streamed =
                DeferredEvidenceSnapshotR36.encodeRuntimeResult(
                        runtime, null, snapshot.actionSpaceHash);
        require(
                java.util.Arrays.equals(encoded, streamed.bytes),
                "T45_STREAMING_SNAPSHOT_BYTE_PARITY");
        require(
                streamed.sha256.equals(DeferredEvidenceSnapshotR36.sha256(encoded))
                        && streamed.header.targetIssue.equals(snapshot.targetIssue)
                        && streamed.header.actionSpaceHash.equals(snapshot.actionSpaceHash),
                "T45_STREAMING_SNAPSHOT_HEADER_HASH");
        DeferredEvidenceSnapshotR36.Snapshot decoded =
                DeferredEvidenceSnapshotR36.decode(encoded);
        require(decoded.actions.size() == 1200, "T45_SNAPSHOT_ACTION_COUNT");
        require(decoded.numbers.size() == 100, "T45_SNAPSHOT_NUMBER_COUNT");
        require(
                decoded.actionSpaceHash.equals(K3ActionSpaceEngine.actionSpaceHash(rank.ranked)),
                "T45_PRODUCER_CONSUMER_ACTION_HASH_PARITY");
        for (int i = 0; i < decoded.actions.size(); i++) {
            DeferredEvidenceSnapshotR36.ActionRow action = decoded.actions.get(i);
            require(
                    K3ActionSpaceEngine.validFourthBoundary(
                            action.n1, action.n2, action.n3, action.n4),
                    "T45_VALID_N4_ROW_" + i);
        }
        V51Models.PositionAction settlementProbe = rank.ranked[0];
        int[] actual = new int[10];
        actual[settlementProbe.position - 1] = settlementProbe.n4;
        require(
                !K3ActionSpaceEngine.hit(settlementProbe, actual),
                "T45_N4_IS_EVIDENCE_NOT_K4_SETTLEMENT");
        actual[settlementProbe.position - 1] = settlementProbe.n1;
        require(
                K3ActionSpaceEngine.hit(settlementProbe, actual),
                "T45_FIXED_K3_SETTLEMENT_UNCHANGED");
        boolean rejectedLegacyZero = false;
        try {
            new DeferredEvidenceSnapshotR36.ActionRow(
                            1,
                            1,
                            2,
                            3,
                            0,
                            0.3d,
                            1.0d,
                            -1.0d,
                            1,
                            V51ChecksumEngine.sha256("T45_FP"),
                            "",
                            V51ChecksumEngine.sha256("T45_ACTION"),
                            V51ChecksumEngine.sha256("T45_RECORD"))
                    .validate();
        } catch (IllegalStateException expected) {
            rejectedLegacyZero =
                    expected.getMessage() != null
                            && expected.getMessage().startsWith(
                                    "R366_ACTION_BOUNDARY_N4_INVALID");
        }
        require(rejectedLegacyZero, "T45_ZERO_N4_REJECTED_WITH_FIELD_DETAIL");
    }

    private static void upgradeRearmPolicyContract() {
        require(
                AnalysisRequestStoreR36.isKnownR363RepairableError(
                        "B04_PERMUTATION_PIT_FAIL:HISTORY_HASH_MISMATCH"),
                "T42_HASH_FAILURE_REARMABLE");
        require(
                AnalysisRequestStoreR36.isKnownR363RepairableError("V51_HISTORY_LT_12"),
                "T42_WARMUP_FAILURE_REARMABLE");
        require(
                AnalysisRequestStoreR36.isKnownR363RepairableError(
                        "R36_HISTORY_HASH_CONFLICT"),
                "T42_BOUND_HASH_FAILURE_REARMABLE");
        require(
                !AnalysisRequestStoreR36.isKnownR363RepairableError("ARBITRARY_RUNTIME_FAILURE"),
                "T42_UNKNOWN_FAILURE_NOT_REARMED");
    }

    private static void durableLeasePolicyContract() {
        long now = 1700001000000L;
        String hash = V51ChecksumEngine.sha256("R366_POLICY_REQUEST");
        require(
                R36AnalysisLeasePolicyR365.decide(
                                AnalysisRequestStoreR36.RUNNING,
                                7L,
                                hash,
                                7L,
                                hash,
                                "foreign-owner",
                                "new-owner",
                                now + 45000L,
                                0L,
                                now,
                                false,
                                false)
                        == R36AnalysisLeasePolicyR365.Decision.DEFER_ACTIVE_OWNER,
                "T43_ACTIVE_FOREIGN_OWNER_DEFERRED");
        require(
                R36AnalysisLeasePolicyR365.decide(
                                AnalysisRequestStoreR36.RUNNING,
                                7L,
                                hash,
                                7L,
                                hash,
                                "foreign-owner",
                                "new-owner",
                                now - 1L,
                                0L,
                                now,
                                false,
                                false)
                        == R36AnalysisLeasePolicyR365.Decision.CLAIM_STALE_TAKEOVER,
                "T43_STALE_FOREIGN_OWNER_TAKEOVER");
        require(
                R36AnalysisLeasePolicyR365.decide(
                                AnalysisRequestStoreR36.RUNNING,
                                7L,
                                hash,
                                7L,
                                hash,
                                "same-owner",
                                "same-owner",
                                now + 45000L,
                                0L,
                                now,
                                false,
                                false)
                        == R36AnalysisLeasePolicyR365.Decision.RESUME_SAME_OWNER,
                "T43_SAME_OWNER_RESUME");
        require(
                R36AnalysisLeasePolicyR365.decide(
                                AnalysisRequestStoreR36.RETRY_WAIT,
                                7L,
                                hash,
                                7L,
                                hash,
                                "",
                                "new-owner",
                                0L,
                                now + 1000L,
                                now,
                                false,
                                false)
                        == R36AnalysisLeasePolicyR365.Decision.NOT_DUE,
                "T43_RETRY_BACKOFF_HONORED");
        require(
                R36AnalysisLeasePolicyR365.decide(
                                AnalysisRequestStoreR36.REQUESTED,
                                7L,
                                hash,
                                7L,
                                hash,
                                "",
                                "new-owner",
                                0L,
                                0L,
                                now,
                                true,
                                false)
                        == R36AnalysisLeasePolicyR365.Decision.TARGET_ALREADY_DRAWN,
                "T43_POST_DRAW_BLOCKED");
        require(
                R36AnalysisLeasePolicyR365.canUpgradeRearm(
                        AnalysisRequestStoreR36.FAILED_TERMINAL,
                        "V57.15.3-R3.6.5",
                        false,
                        false,
                        false),
                "T43_R365_ACTION_INVALID_TERMINAL_ONE_SHOT_REARM");
        require(
                !R36AnalysisLeasePolicyR365.canUpgradeRearm(
                        AnalysisRequestStoreR36.FAILED_TERMINAL,
                        R36AnalysisLeasePolicyR365.REPAIR_VERSION,
                        false,
                        false,
                        false),
                "T43_R366_TERMINAL_NO_LOOP_REARM");
        require(
                R36AnalysisLeasePolicyR365.retryDelayMs(1L) == 1000L
                        && R36AnalysisLeasePolicyR365.retryDelayMs(100L) == 30000L,
                "T43_BOUNDED_RETRY_BACKOFF");
    }

    private static void executionFenceContract() {
        AnalysisSingleFlightR36 f = AnalysisSingleFlightR36.shared();
        f.resetForTests();
        AnalysisSingleFlightR36.Request r =
                request("202608100200", "202608100201", 201L);
        AnalysisSingleFlightR36.SubmitResult first = f.submit(r);
        String firstOwner = first.lease.ownerId;
        f.releaseAndTakeNext(first.lease);
        AnalysisSingleFlightR36.SubmitResult second = f.submit(r);
        String secondOwner = second.lease.ownerId;
        require(!firstOwner.equals(secondOwner), "T44_UNIQUE_EXECUTION_OWNER_PER_LEASE");
        require(firstOwner.length() == 32 && secondOwner.length() == 32, "T44_OWNER_ID_SHAPE");
        f.releaseAndTakeNext(second.lease);
    }

    private static void diagnosisReportContract() {
        LiveResearchDiagnosisOrchestrator orchestrator = new LiveResearchDiagnosisOrchestrator();
        LiveResearchDiagnosisOrchestrator.ReplayResult diagnosis =
                orchestrator.replay(LiveResearchDiagnosisOrchestrator.deterministicFixture());
        require(diagnosis.deterministic, "T35_DIAGNOSIS_HASH_DETERMINISTIC");
        require(diagnosis.dagValid, "T35_DIAGNOSIS_DAG_VALID");
        require(
                diagnosis.first.stages.size()
                        == LiveResearchDiagnosisOrchestrator.Stage.values().length,
                "T35_ALL_FIXED_STAGES");
        require(diagnosis.first.edges.size() >= 14, "T35_DEPENDENCY_EDGES");
        require(
                diagnosis.first.overallStatus
                        == LiveResearchDiagnosisOrchestrator.Status.NOT_PROVEN,
                "T35_MODEL_NOT_PROVEN_PRIORITY");

        LiveDiagnosticReportEngine engine = new LiveDiagnosticReportEngine();
        LiveDiagnosticReportEngine.ReplayResult report = engine.replayDeterminism();
        require(report.valid, "T36_REPORT_REPLAY_VALID");
        require(report.deterministicBody, "T36_BODY_HASH_DETERMINISTIC");
        require(report.deterministicFull, "T36_FULL_HASH_DETERMINISTIC");
        require(report.deterministicIndex, "T36_INDEX_DETERMINISTIC");
        require(report.chunksRoundTrip, "T36_CHUNK_ROUND_TRIP");
        require(report.first.chapters.size() == 18, "T36_ALL_CHAPTERS");
        require(report.first.validation.valid, "T36_REPORT_VALIDATION");
        require(
                report.first.copyPayload.status == LiveDiagnosticReportEngine.CopyStatus.EXACT_FULL,
                "T36_EXACT_COPY_WHEN_CLEAN");
        require(
                report.first.copyPayload.text.equals(report.first.fullText),
                "T36_COPY_NOT_TRUNCATED");
        require(
                report.first.fullText.endsWith(LiveDiagnosticReportEngine.END_MARKER + "\n"),
                "T36_END_MARKER");
        LiveDiagnosticReportEngine.RedactionResult redacted =
                engine.redact("API_KEY=host-secret\nstatus=PASS");
        require(redacted.changed && redacted.events.size() == 1, "T36_SECRET_REDACTION");
        require(redacted.text.indexOf("host-secret") < 0, "T36_SECRET_REMOVED");
    }

    private static AnalysisSingleFlightR36.Request request(
            String base, String target, long generation) {
        long now = 1700000000000L + generation;
        return new AnalysisSingleFlightR36.Request(
                base,
                target,
                generation,
                now,
                now + 60000L,
                "HOST",
                V51ChecksumEngine.sha256("HOST|" + target + "|" + generation),
                "");
    }

    private static String four(int v) {
        String s = String.valueOf(v);
        while (s.length() < 4) s = "0" + s;
        return s;
    }

    private static void require(boolean value, String id) {
        if (!value) throw new AssertionError(id);
    }
}
