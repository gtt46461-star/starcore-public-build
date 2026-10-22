package com.ethk3.novaonepro;

import java.util.List;

/** Sole R3.6 compute/commit orchestrator. Runtime duration never selects a model branch. */
public final class V51RuntimeOrchestrator {
    public static final String B13_SOLE_RUNTIME_ORCHESTRATOR = "V51RuntimeOrchestrator";
    public static final String V57_PRODUCTION_RUNTIME_OWNER = "V51RuntimeOrchestrator";
    public static final String V57_PRODUCTION_CHAIN =
            "FROZEN_PIT->FEATURES->EVIDENCE->NUMBER_100->ACTION_1200->RANK1->DECISION_GATE->ATOMIC_CANONICAL";

    public OutcomeR36 runR36(
            DbStore store,
            V51RuntimeInputR36 input,
            AnalysisSingleFlightR36.Request request,
            AnalysisSingleFlightR36.Lease lease) {
        return runR36(store, input, request, lease, null);
    }

    OutcomeR36 runR36(
            DbStore store,
            V51RuntimeInputR36 input,
            AnalysisSingleFlightR36.Request request,
            AnalysisSingleFlightR36.Lease lease, PredrawWakeLeaseR81 predrawWakeR81) {
        if (store == null
                || input == null
                || request == null
                || lease == null
                || !request.valid()
                || !request.targetIssue.equals(input.targetIssue)
                || !request.baseIssue.equals(input.baseIssue)
                || !request.historyHash.equals(input.historyHash)
                || !lease.targetIssue.equals(request.targetIssue)
                || !lease.request.requestHash.equals(request.requestHash))
            throw new IllegalArgumentException("R36_ORCHESTRATOR_INPUT_INVALID");
        RuntimeTraceR36 trace =
                new RuntimeTraceR36(request.attemptId(), request.targetIssue, request.requestAt);
        RuntimeTraceR36.Mark root = trace.begin("R36_COMPUTE");
        long computeStarted = TimeSourceR811.monoNowMs();
        final long runtimeEnteredMonoR927 = TimeSourceR811.monoNowMs();
        final long runtimeEnteredR925 = System.currentTimeMillis();
        AnalysisRequestStoreR36 requests = new AnalysisRequestStoreR36(store);
        try (RecipeBudgetScopeCP12 recipeScope=RecipeBudgetScopeCP12.open();ModelBundleRuntimeR932.Scope modelScope = openBundleTracedCP3(store, request, trace)) {
            checkpoint(requests, lease, "RUNTIME_ENTER");
            V51Models.DrawFrame[] history = input.frozenFramesR36();
            if (history.length < 12) throw new IllegalStateException("V51_HISTORY_LT_12");
            if (!request.baseIssue.equals(history[history.length - 1].issue))
                throw new IllegalStateException("R36_HISTORY_MAX_MISMATCH");
            V51Models.PredictionContext context =
                    new V51Models.PredictionContext(
                            request.baseIssue,
                            request.targetIssue,
                            request.baseIssue,
                            input.historyHash,
                            V51BuildManifest.COHORT_ID,
                            ModelBundleRuntimeR932.current().hash,
                            request.requestAt,
                            history.length);

            RuntimeTraceR36.Mark mark = trace.begin("FEATURES_SUB");
            V51PermutationFeatureEngine featureEngine = new V51PermutationFeatureEngine();
            V51Models.PermutationFeatureSnapshot features = featureEngine.build(context, history);
            V51Models.PermutationFeatureSnapshot previous =
                    previousFeatures(featureEngine, history, context.createdAt);
            RollingWindowCache.SnapshotSet windows =
                    RollingWindowCache.shared().getOrBuild(history);
            RollingWindowCache.Validation rollingValidation = windows.validate();
            if (!rollingValidation.ok)
                throw new IllegalStateException(
                        "B04_ROLLING_WINDOW_INVALID:" + rollingValidation.detail);
            FullInformationFeatureGraphEngine.FrozenFeatureGraph graph =
                    FullInformationFeatureGraphEngine.shared()
                            .build(
                                    context,
                                    history,
                                    features.globalFeatures,
                                    features.numberFeatures,
                                    windows);
            if (!graph.valid() || !graph.featureHash.equals(graph.replayHash()))
                throw new IllegalStateException("B04_FEATURE_GRAPH_INVALID");
            String featureContractHash =
                    V51ChecksumEngine.sha256(
                            features.checksum + '|' + graph.featureHash + '|' + windows.checksum);
            trace.end(mark, true, RuntimeMemoryGuardR811.snapshot("FEATURES_DONE").canonical());
            checkpoint(requests, lease, "FEATURES_DONE");

            mark = trace.begin("STATE_SUB");
            V51StateTrajectoryEngine.TrajectoryBundle stateBundle =
                    new V51StateTrajectoryEngine().buildAudited(features, previous);
            if (!stateBundle.valid)
                throw new IllegalStateException(
                        "B05_STATE_TRAJECTORY_INVALID:" + stateBundle.detail);
            V51Models.StateTrajectorySnapshot[] states = stateBundle.states;
            WindowFeatureEngine.B05WindowEvidenceBundle windowEvidence =
                    new WindowFeatureEngine().buildB05Evidence(history, states);
            TrendStateMachine.B05TrendEvidenceBundle trendEvidence =
                    new TrendStateMachine().buildB05Evidence(history, states);
            BreakRecoveryEngine.B05BreakEvidenceBundle breakEvidence =
                    new BreakRecoveryEngine().buildB05Evidence(history, states);
            CycleRhythmEngine.B05CycleEvidenceBundle cycleEvidence =
                    new CycleRhythmEngine().buildB05Evidence(history);
            DistributionDriftEngine.B05DriftEvidenceBundle driftEvidence =
                    new DistributionDriftEngine().buildB05Evidence(history);
            if (!windowEvidence.valid
                    || !trendEvidence.valid
                    || !breakEvidence.valid
                    || !cycleEvidence.valid
                    || !driftEvidence.valid)
                throw new IllegalStateException("B05_EVIDENCE_BUNDLE_INVALID");
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
            featureContractHash =
                    V51ChecksumEngine.sha256(featureContractHash + '|' + evidenceHash);
            trace.end(
                    mark,
                    true,
                    RuntimeMemoryGuardR811.snapshot("STATE_EVIDENCE_DONE").canonical());
            checkpoint(requests, lease, "STATE_EVIDENCE_DONE");

            mark = trace.begin("NUMBER_MODEL");
            V51PermutationNumberModelEngine.Result numberModel =
                    new V51PermutationNumberModelEngine().predict(
                            features, states, history, lease.token);
            if (numberModel.matrix == null || numberModel.matrix.probability.length != 10)
                throw new IllegalStateException("V51_NUMBER_MATRIX_INVALID");
            trace.end(
                    mark,
                    true,
                    RuntimeMemoryGuardR811.snapshot("NUMBER_MODEL_DONE").canonical());
            checkpoint(requests, lease, "NUMBER_MODEL_DONE");

            mark = trace.begin("ACTION_BUILD_FUNNEL_META_RANK");
            ActionStage actionStage =
                    buildRankedActionStage(
                            store,
                            context.targetIssue,
                            features,
                            states,
                            numberModel,
                            request.requestAt,
                            lease);
            V51EffectiveConsensusEngine.Consensus[] consensus = actionStage.consensus;
            K3FunnelOrchestrator.Audit funnel = actionStage.funnel;
            V51Models.PositionAction[] actions = actionStage.actions;
            V51Models.PositionAction rankOne = actionStage.rankOne;
            V51RankStabilityEngine.Result stability = actionStage.stability;
            trace.end(
                    mark,
                    true,
                    RuntimeMemoryGuardR811.snapshot("ACTION_1200_DONE").canonical());
            checkpoint(requests, lease, "ACTION_1200_DONE");

            mark = trace.begin("SELECTION_POLICY");
            V51Models.StateTrajectorySnapshot state = states[rankOne.position - 1];
            V51Models.DomainCertificate domain =
                    new V51DomainCertificateEngine().certify(store, state);
            /* Deterministic active call: no elapsed-time skip branch. */
            V51TianshuEvidenceBridge.Result tianshu =
                    new V51TianshuEvidenceBridgeR923()
                            .evaluate(
                                    store,
                                    input.history,
                                    context,
                                    rankOne,
                                    state,
                                    numberModel.matrix.probability[rankOne.position - 1]);
            double bridgeCounter = Math.max(tianshu.risk, 1.0d - tianshu.confidence);
            double falsifier =
                    new V51FalsifierExpert()
                            .score(
                                    state,
                                    1.0d - consensus[rankOne.position - 1].effective,
                                    1.0d - stability.retention,
                                    1.0d - stability.thirdRobustness,
                                    bridgeCounter,
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
                                    V51PredictabilityEngine.expertReliability(numberModel.experts,"LAGGED_RELATION_TENSOR",rankOne.position),
                                    V51PredictabilityEngine.expertReliability(numberModel.experts,"MULTILENGTH_MATRIX_PROFILE",rankOne.position),
                                    V51PredictabilityEngine.expertReliability(numberModel.experts,"TEACHER_POSTERIOR",rankOne.position),
                                    falsifier,
                                    SelectionEvidenceContractR929.actualPolicy(actionStage.governanceLane,actionStage.governanceCandidate,actionStage.governanceWeight,actionStage.governanceApplied));
            V51SelectedActionCalibrationEngine.CalibrationResult calibration =
                    new V51SelectedActionCalibrationEngine()
                            .calibrateDetailed(
                                    rankOne,
                                    consensus[rankOne.position - 1],
                                    state,
                                    domain,
                                    uncertainty,
                                    falsifier,
                                    predictability.probability,
                                    SelectionEvidenceContractR929.actualPolicy(actionStage.governanceLane,actionStage.governanceCandidate,actionStage.governanceWeight,actionStage.governanceApplied));
            double selectedHitP = actionStage.certifiedPrediction==null?calibration.selectedHitP:
                    actionStage.certifiedPrediction.selectedHitP;
            String selectedPolicyHash=actionStage.certifiedPrediction==null?calibration.selectionPolicyHash:
                    actionStage.certifiedPrediction.selectionHash;
            String contractHash =
                    V51ChecksumEngine.sha256(
                            context.targetIssue
                                    + '|'
                                    + rankOne.checksum
                                    + '|'
                                    + selectedHitP
                                    + '|'
                                    + calibration.status.name()
                                    + '|'
                                    + predictability.checksum
                                    + '|'
                                    + domain.checksum
                                    + '|'
                                    + ModelBundleRuntimeR932.current().hash);
            V51Models.SelectedActionEnvelope selected =
                    new V51Models.SelectedActionEnvelope(
                            context,
                            rankOne,
                            selectedHitP,
                            predictability,
                            domain,
                            V51Enums.Lane.SHADOW,
                            uncertainty.total,
                            falsifier,
                            contractHash,selectedPolicyHash);
            V51SelectiveRiskController.Result policy =
                    new V51SelectiveRiskController()
                            .decide(selected, consensus[rankOne.position - 1], stability);
            V51CandidateRealGateR915.Evaluation candidateEvaluation;
            // R947: Selection Policy is latency critical. Preserve the exact fail-closed decision
            // semantics, but defer transient rollback cleanup writes until after canonical commit.
            if (V51ArtifactRollbackEngine.effectiveForcedShadowR947(store)) {
                candidateEvaluation =
                        new V51CandidateRealGateR915.Evaluation(
                                false,
                                "ARTIFACT_ROLLBACK_FORCED_SHADOW",
                                "TRANSIENT_ROLLBACK_OR_REVOCATION_ACTIVE");
                policy =
                        new V51SelectiveRiskController.Result(
                                V51Enums.Lane.SHADOW, V51Enums.RejectCode.MODEL_NOT_PROVEN, null);
            } else {
                candidateEvaluation =
                        actionStage.certifiedPrediction==null?V51CandidateRealGateR915.evaluate(store, selected, stability):
                                CertifiedPredictionR974.evaluate(store,selected,actionStage.certifiedPrediction);
                if (candidateEvaluation.eligible
                        && (!actionStage.governanceApplied
                                || actionStage.controlledInfluenceEligible))
                    policy =
                            new V51SelectiveRiskController.Result(
                                    V51Enums.Lane.CONTROLLED_PROBE,
                                    V51Enums.RejectCode.NONE,
                                    null,
                                    true,
                                    candidateEvaluation.code + '|' + candidateEvaluation.checksum);
                // The governed-shadow diagnostic is persisted post-canonical with the rest of
                // the non-authoritative telemetry; it must never acquire the write lock here.
            }
            selected =
                    new V51Models.SelectedActionEnvelope(
                            context,
                            rankOne,
                            selectedHitP,
                            predictability,
                            domain,
                            policy.lane,
                            uncertainty.total,
                            falsifier,
                            contractHash,selectedPolicyHash);
            trace.end(
                    mark,
                    true,
                    RuntimeMemoryGuardR811.snapshot("SELECTION_DONE").canonical());
            lease.token.enterFinalization();
            checkpoint(requests, lease, "SELECTION_DONE");

            mark = trace.begin("DECISION_GATE");
            V51CompiledActionContract compiled =
                    new V51CompiledActionContract(
                            selected,
                            policy,
                            stability,
                            featureContractHash,
                            stateHash(states),
                            expertHash(numberModel.experts),
                            numberModel.matrix.checksum,
                            actionHash(actions),actionStage.certifiedPrediction);
            V51Models.AuthorizedDecision authorized =
                    new DecisionGateEngine().decideV51B12(store, compiled);
            if (authorized == null)
                throw new IllegalStateException("R915_AUTHORIZATION_NULL");
            if (authorized.action == V51Enums.DecisionAction.BET
                    && !V51CandidateRealGateR915.verifyAuthorized(store, compiled, authorized))
                throw new IllegalStateException("R915_CONTROLLED_BET_CONTRACT_INVALID");
            trace.end(
                    mark,
                    true,
                    RuntimeMemoryGuardR811.snapshot("DECISION_GATE_DONE").canonical());
            checkpoint(requests, lease, "BEFORE_CANONICAL_COMMIT");
            String status =
                    authorized.action.name()
                            + '|'
                            + authorized.rejectCode.name()
                            + '|'
                            + V51BuildManifest.releaseState()
                            + "|R903_MAINLINE="
                            + actionStage.governanceStatus;
            long computeMs = Math.max(0L, TimeSourceR811.monoNowMs() - computeStarted);
            ControlledCandidateOpportunityAuditR922.Opportunity opportunityAudit =
                    ControlledCandidateOpportunityAuditR922.from(
                            context.baseIssue,
                            context.targetIssue,
                            selected,
                            stability,
                            candidateEvaluation,
                            authorized,
                            actionStage.governanceLane,
                            actionStage.governanceCandidate,
                            actionStage.governanceStatus,
                            actionStage.governanceWeight,
                            actionStage.governanceApplied,
                            actionStage.controlledInfluenceEligible,
                            request.requestAt);
            ResearchMainlineContributionAuditR922.Contribution contributionAudit =
                    actionStage.contribution(context.targetIssue, request.requestAt);
            String resultChecksum =
                    V51ChecksumEngine.sha256(
                            compiled.checksum
                                    + '|'
                                    + status
                                    + '|'
                                    + opportunityAudit.recordHash
                                    + '|'
                                    + contributionAudit.recordHash);
            V51RuntimeResult result =
                    new V51RuntimeResult(
                            true,
                            context,
                            features,
                            states,
                            numberModel,
                            consensus,
                            funnel,
                            actions,
                            selected,
                            authorized,
                            compiled,
                            status,
                            computeMs,
                            resultChecksum,
                            opportunityAudit,
                            contributionAudit,
                            actionStage.frozenConsumerRecipe);
            PredictionOutputContractR815.requireCanonicalReady(
                    PredictionOutputContractR815.validate(
                            request.targetIssue, result, input.history));
            trace.end(root, true, "");
            RuntimeTraceR36.Mark snapshotMark = trace.begin("SNAPSHOT_STREAM");
            DeferredEvidenceSnapshotR36.EncodedSnapshot snapshot =
                    DeferredEvidenceSnapshotR36.encodeRuntimeResult(
                            result, trace, actionStage.actionSpaceHash);
            trace.end(
                    snapshotMark,
                    true,
                    RuntimeMemoryGuardR811.snapshot("SNAPSHOT_STREAM_DONE").canonical());
            RuntimeTraceR36.Mark commitMark = trace.begin("DB_CANONICAL_WAIT");
            V51Repository.CanonicalCommitResult commit =
                    V51Repository.commitCanonicalBundleR36(
                            store,
                            request,
                            lease.ownerId,
                            result,
                            snapshot,
                            trace);
            if (commit != null && commit.success) {
                lease.token.markCanonicalCommitted();
                if (predrawWakeR81 != null) predrawWakeR81.canonicalCommitted();
            }
            trace.end(commitMark, true, "CANONICAL_COMMITTED");
            final long durableReturnedAt = System.currentTimeMillis();
            final long durableReturnedMonoR927 = TimeSourceR811.monoNowMs();
            final long requestedMonoR927 = RuntimeObservabilityR925.requestOrigin(request);
            final long requestElapsedR927 = requestedMonoR927 < 0L ? -1L : Math.max(0L, durableReturnedMonoR927-requestedMonoR927);
            final long commitCostR925 = Math.max(0L, (System.nanoTime() - commitMark.nanoStartedAt)/1000000L);
            lease.token.markCanonicalCommitted();
            try {
                final java.util.LinkedHashMap<String,String> telemetry = new java.util.LinkedHashMap<String,String>();
                final String candidateGateStatusR947 =
                        candidateEvaluation.eligible
                                && actionStage.governanceApplied
                                && !actionStage.controlledInfluenceEligible
                                ? "RESEARCH_GOVERNED_ACTION_SHADOW_ONLY"
                                : candidateEvaluation.code;
                telemetry.put("r915_candidate_gate_issue", context.targetIssue);
                telemetry.put("r915_candidate_gate_status", candidateGateStatusR947);
                telemetry.put("r915_candidate_gate_detail", candidateEvaluation.detail);
                telemetry.put("r915_candidate_gate_hash", candidateEvaluation.checksum);
                telemetry.put("r903_mainline_governor_status", actionStage.governanceStatus);
                telemetry.put("r903_mainline_governor_candidate", actionStage.governanceCandidate);
                telemetry.put("r903_mainline_governor_target", request.targetIssue);
                telemetry.put("r903_mainline_governor_applied", actionStage.governanceApplied ? "1" : "0");
                telemetry.put("r923_request_to_durable_return_ms",
                        String.valueOf(requestElapsedR927));
                store.coordinatorR36().runWrite("R923_POST_COMMIT_TELEMETRY",
                        new DatabaseCoordinator.WriteRunnable() {
                            @Override public void run(android.database.sqlite.SQLiteDatabase db) {
                                DbStore.putStatesR36(db, telemetry);
                                RuntimeObservabilityR925.timing(db, request.targetIssue, request.requestAt, runtimeEnteredR925, durableReturnedAt, computeMs, commitCostR925, request.attemptId(), requestedMonoR927, runtimeEnteredMonoR927, durableReturnedMonoR927);
                                RuntimeHealthLedgerR36.recordWithinTransaction(db,
                                        RuntimeHealthLedgerR36.DURABLE_RETURN_EVENT_R923,
                                        request.attemptId(),request.targetIssue,"SUCCESS",
                                        request.requestAt,durableReturnedAt,
                                        requestElapsedR927,
                                        AnalysisSingleFlightR36.shared().activeCount(),
                                        AnalysisSingleFlightR36.shared().maximumObserved(),true,
                                        requestedMonoR927 < 0L ? "REHYDRATED_ORIGIN_UNAVAILABLE" : "MONOTONIC_REQUEST_TO_TRANSACTION_RETURN|TELEMETRY_IO_EXCLUDED");
                            }
                        });
            } catch (Throwable ignoredGovernanceTelemetry) {
                // The canonical commit is already durable; telemetry cannot invalidate it.
            }
            try {
                V51ArtifactRollbackEngine.reconcileTransientRollbackR915(store);
            } catch (Throwable ignoredRollbackCleanup) {
                // The read-only hot-path decision already preserved the same fail-closed contract.
                // Cleanup is diagnostic state only after canonical durability.
            }
            // R907 P0 latency rule: after canonical commit, return immediately to the service so
            // Decision broadcast is not held behind Research/diagnostic work. Research capture is
            // scheduled on the dedicated background-serial HandlerThread by AutoFetchService.
            return new OutcomeR36(result.publishedViewCP12(), commit, snapshot.sha256, trace);
        } catch (OutOfMemoryError oom) {
            trace.end(root, false, "OOM");
            RuntimeMemoryGuardR811.recoverAfterFailure();
            throw oom;
        } catch (RuntimeException e) {
            trace.end(root, false, e.getClass().getSimpleName());
            throw e;
        } catch (Throwable e) {
            trace.end(root, false, e.getClass().getSimpleName());
            throw new IllegalStateException("R36_RUNTIME_FAILED", e);
        }
    }

    private static void checkpoint(
            AnalysisRequestStoreR36 requests, AnalysisSingleFlightR36.Lease lease, String stage) {
        lease.token.checkpoint(stage);
        long now = System.currentTimeMillis();
        boolean force = "BEFORE_CANONICAL_COMMIT".equals(stage);
        if (!lease.token.claimDurablePulse(now, force)) return;
        if (!requests.pulse(lease.request, stage, lease.ownerId, now))
            throw new IllegalStateException("R366_DURABLE_LEASE_FENCE_LOST:" + stage);
    }

    /** Keep n4 as excluded-number diagnostics while the strict rank owner receives exactly K=3. */
    static V51ActionRankerEngine.RankResult rankK3DiagnosticActionsF06(
            V51Models.PositionAction[] source, double[][] meta) {
        K3ActionSpaceEngine.validateOrThrow(source);
        V51Models.PositionAction[] strict = new V51Models.PositionAction[source.length];
        for (int i = 0; i < source.length; i++) {
            V51Models.PositionAction a = source[i];
            strict[i] = new V51Models.PositionAction(a.position, a.n1, a.n2, a.n3, 0,
                    a.rawActionP, a.rankScore, a.rankMargin, a.thirdMargin, a.entropy,
                    a.stability, a.metaFeatures, a.fingerprint, a.rank, a.checksum);
        }
        V51ActionRankerEngine.RankResult ranked = new V51ActionRankerEngine().rankDetailed(strict, meta);
        V51Models.PositionAction[] restored = new V51Models.PositionAction[source.length];
        for (int i = 0; i < restored.length; i++) {
            V51Models.PositionAction a = ranked.ranked[i];
            restored[i] = new V51Models.PositionAction(a.position, a.n1, a.n2, a.n3, source[i].n4,
                    a.rawActionP, a.rankScore, a.rankMargin, a.thirdMargin, a.entropy,
                    a.stability, a.metaFeatures, a.fingerprint, a.rank, a.checksum);
        }
        V51Models.PositionAction one = new V51ActionRankerEngine().rankOne(restored);
        if (one == null) throw new IllegalStateException("F06_K3_RANK_ONE_MISSING");
        return new V51ActionRankerEngine.RankResult(restored, one, ranked.status,
                ranked.artifactProven, ranked.topMargin);
    }

    private static ActionStage buildRankedActionStage(
            DbStore store,
            String targetIssue,
            V51Models.PermutationFeatureSnapshot features,
            V51Models.StateTrajectorySnapshot[] states,
            V51PermutationNumberModelEngine.Result numberModel,
            long frozenAt,
            AnalysisSingleFlightR36.Lease lease) {
        V51EffectiveConsensusEngine.Consensus[] consensus =
                new V51EffectiveConsensusEngine()
                        .evaluate(numberModel.experts, states, new V51ExpertRegretEngine());
        lease.token.checkpoint("ACTION_CONSENSUS_DONE");
        V51Models.PositionAction[] baseActions =
                new V51PositionActionBuilder().build(numberModel.matrix, states, consensus);
        K3ActionSpaceEngine.validateOrThrow(baseActions);
        lease.token.checkpoint("ACTION_1200_ENUM_DONE");
        K3FunnelOrchestrator.Audit funnel =
                new K3FunnelOrchestrator().evaluate(numberModel.matrix, baseActions);
        if (!funnel.neverDeleteFormalAction || funnel.formalActionCount != 1200)
            throw new IllegalStateException("B10_FUNNEL_DELETED_FORMAL_ACTION");
        lease.token.checkpoint("ACTION_FUNNEL_DONE");
        double[][] meta =
                new V51ActionMetaFeatureEngine()
                        .build(
                                baseActions,
                                features,
                                states,
                                numberModel.experts,
                                numberModel.gateWeight,
                                consensus);
        lease.token.checkpoint("ACTION_META_DONE");
        V51ActionRankerEngine.RankResult ranked =
                rankK3DiagnosticActionsF06(baseActions, meta);
        lease.token.checkpoint("ACTION_RANK_DONE");
        V51Models.PositionAction[] actions = ranked.ranked;
        V51Models.PositionAction rankOne = ranked.rankOne;
        boolean researchChoiceFrozen = UnifiedResearchChoiceR932.freeze(store,targetIssue);
        CertifiedPredictionR974.Prepared exact=researchChoiceFrozen?CertifiedPredictionR974.prepare(store,features.context,actions,lease.request):null;
        if(exact!=null){
            K3ActionSpaceEngine.FrozenChampion champion=new K3ActionSpaceEngine().freeze(exact.rankOne,exact.actions,frozenAt);
            UnifiedResearchChoiceR932.Evidence ev=exact.evidence;
            return new ActionStage(consensus,funnel,exact.actions,exact.rankOne,
                    new V51RankStabilityEngine().evaluate(exact.rankOne,exact.actions),champion.actionSpaceHash,
                    true,"R974_EXACT_FROZEN_MODEL|certificate="+ev.certificate,ev.id,true,
                    CertifiedPredictionR974.LANE,"独立证据合格的冻结模型",ev.evidence,1.0,
                    ev.count("FUTURE_N"),ev.metric("HIT_RATE"),ev.metric("WILSON_LOWER"),ev.metric("BRIER"),ev.metric("ECE"),
                    ev.metric("COVERAGE"),ev.count("PAIRED_N"),ev.metric("PAIRED_DELTA"),ev.metric("FWER_ADJUSTED_P"),
                    exact.proof.recipe,exact.proof);
        }
        // Research overlay failure is fail-closed to Research but cannot delete the base 1200
        // Production action space. The downstream governors return zero influence when no choice
        // row exists; final authority remains DecisionGateEngine.
        // The certified new-asset lane is the only GENESIS_NEW consumer. It receives a complete
        // immutable predraw view, evaluates at most Production top-64 with weight capped by the frozen unified certificate. It has no BET writer; final authority remains DecisionGateEngine.
        V51RuntimeResult assetContext = new V51RuntimeResult(
                true,
                features == null ? null : features.context,
                features,
                states,
                numberModel,
                consensus,
                funnel,
                actions,
                null,
                null,
                null,
                "R915_CERTIFIED_ASSET_CONTEXT",
                0L,
                actionHash(actions));
        CertifiedResearchMixerR915.Result assetGovernance =
                CertifiedResearchMixerR915.mix(
                        store,
                        features == null || features.context == null
                                ? "" : features.context.baseIssue,
                        targetIssue,
                        assetContext,
                        actions);
        if (assetGovernance.applied) {
            actions = assetGovernance.actions;
            rankOne = assetGovernance.rankOne;
        }
        // R915 gives a genuinely invented, stateful algorithm one bounded production influence
        // path only after its own chronological Future OOS lifecycle reaches CHALLENGER.  The
        // algorithm still cannot authorize REAL; the final action remains subject to the exact
        // Production candidate gate and DecisionGateEngine.  Formula/DSL Research certificates
        // retain their historical SHADOW-only governor and are consulted only when no qualifying
        // algorithm genome exists.
        AlgorithmProductionGovernorR915.Result algorithmGovernance =
                assetGovernance.applied ? AlgorithmProductionGovernorR915.skippedForAsset(actions) : AlgorithmProductionGovernorR915.govern(
                        store,
                        features == null || features.context == null
                                ? ""
                                : features.context.baseIssue,
                        targetIssue,
                        actions);
        ResearchMainlineGovernorR903.Result governance;
        if (algorithmGovernance.applied) {
            actions = algorithmGovernance.actions;
            rankOne = algorithmGovernance.rankOne;
            governance = ResearchMainlineGovernorR903.Result.fallback(
                    actions, "SKIP_DSL_CERTIFIED_ALGORITHM_PRIORITY");
        } else if (assetGovernance.applied) {
            governance = ResearchMainlineGovernorR903.Result.fallback(
                    actions, "SKIP_DSL_CERTIFIED_NEW_ASSET_PRIORITY");
        } else {
            governance = ResearchMainlineGovernorR903.govern(store, features.context.baseIssue, targetIssue, actions);
            if (governance.applied) {
                actions = governance.actions;
                rankOne = governance.rankOne;
            }
        }
        if (rankOne == null || actions == null || actions.length != 1200)
            throw new IllegalStateException("R36_RANK_CARDINALITY_INVALID");
        K3ActionSpaceEngine.FrozenChampion champion =
                new K3ActionSpaceEngine().freeze(rankOne, actions, frozenAt);
        if (!champion.unchanged(rankOne))
            throw new IllegalStateException("B10_CHAMPION_MUTATED_AFTER_FREEZE");
        V51RankStabilityEngine.Result stability =
                new V51RankStabilityEngine().evaluate(rankOne, actions);
        lease.token.checkpoint("ACTION_STABILITY_DONE");
        String governanceLane = "PRODUCTION_FALLBACK";
        String governanceCandidate = "";
        String governanceName = "";
        String governanceEvidence = "";
        double governanceWeight = 0.0d;
        int futureN = 0, pairedN = 0;
        double hitRate = -1.0d, wilson = -1.0d, brier = -1.0d, ece = -1.0d;
        double coverage = -1.0d, pairedDelta = -1.0d, fdrQ = -1.0d;
        if (assetGovernance.applied) {
            governanceLane = "GENESIS_NEW_CERTIFIED";
            governanceCandidate = assetGovernance.assetId;
            governanceName = assetGovernance.nameZh;
            governanceEvidence = assetGovernance.evidenceHash;
            governanceWeight = assetGovernance.influenceWeight;
            futureN = assetGovernance.futureN;
            pairedN = assetGovernance.pairedN;
            hitRate = assetGovernance.futureRate;
            wilson = assetGovernance.wilson;
            brier = assetGovernance.brier;
            ece = assetGovernance.ece;
            coverage = assetGovernance.coverage;
            pairedDelta = assetGovernance.pairedDelta;
            fdrQ = assetGovernance.fdrQ;
        } else if (algorithmGovernance.applied) {
            governanceLane = "INVENTED_ALGORITHM_CERTIFIED";
            governanceCandidate = algorithmGovernance.genomeId;
            governanceName = algorithmGovernance.nameZh;
            governanceEvidence = algorithmGovernance.evidenceHash;
            governanceWeight = algorithmGovernance.influenceWeight;
            futureN = algorithmGovernance.futureN;
            pairedN = algorithmGovernance.pairedN;
            hitRate = algorithmGovernance.futureRate;
            wilson = algorithmGovernance.wilson;
            brier = algorithmGovernance.brier;
            ece = algorithmGovernance.ece;
            coverage = algorithmGovernance.coverage;
            pairedDelta = algorithmGovernance.pairedDelta;
            fdrQ = algorithmGovernance.fdrQ;
        } else if (governance.applied) {
            governanceLane = "FORMULA_DSL_ACTIVE_SHADOW";
            governanceCandidate = governance.candidateId;
            governanceWeight = governance.influenceWeight;
            futureN = governance.futureN;
            hitRate = governance.hitRate;
            wilson = governance.wilsonLower;
            brier = governance.brier;
        }
        /* baseActions and meta become unreachable at this method boundary. */
        return new ActionStage(
                consensus,
                funnel,
                actions,
                rankOne,
                stability,
                champion.actionSpaceHash,
                assetGovernance.applied || algorithmGovernance.applied || governance.applied,
                (assetGovernance.applied ? assetGovernance.proof() + "|" : "")
                        + (algorithmGovernance.applied
                                ? algorithmGovernance.proof()
                                : governance.status)+"|choice_reason="+UnifiedResearchChoiceR932.currentChoiceReasonCP12(store,targetIssue),
                governanceCandidate,
                !governance.applied
                        && (assetGovernance.applied
                                || algorithmGovernance.controlledInfluenceEligible()),
                governanceLane,
                governanceName,
                governanceEvidence,
                governanceWeight,
                futureN,
                hitRate,
                wilson,
                brier,
                ece,
                coverage,
                pairedN,
                pairedDelta,
                fdrQ,
                assetGovernance.applied?assetGovernance.frozenRecipe:algorithmGovernance.applied?algorithmGovernance.frozenRecipe:governance.frozenRecipe);
    }

    private static final class ActionStage {
        final V51EffectiveConsensusEngine.Consensus[] consensus;
        final K3FunnelOrchestrator.Audit funnel;
        final V51Models.PositionAction[] actions;
        final V51Models.PositionAction rankOne;
        final V51RankStabilityEngine.Result stability;
        final String actionSpaceHash, frozenConsumerRecipe;
        final CertifiedPredictionR974.Proof certifiedPrediction;
        final boolean governanceApplied;
        final boolean controlledInfluenceEligible;
        final String governanceStatus, governanceCandidate, governanceLane;
        final String governanceName, governanceEvidence;
        final double governanceWeight, hitRate, wilson, brier, ece, coverage;
        final double pairedDelta, fdrQ;
        final int futureN, pairedN;

        ActionStage(
                V51EffectiveConsensusEngine.Consensus[] consensus,
                K3FunnelOrchestrator.Audit funnel,
                V51Models.PositionAction[] actions,
                V51Models.PositionAction rankOne,
                V51RankStabilityEngine.Result stability,
                String actionSpaceHash,
                boolean governanceApplied,
                String governanceStatus,
                String governanceCandidate,
                boolean controlledInfluenceEligible,
                String governanceLane,
                String governanceName,
                String governanceEvidence,
                double governanceWeight,
                int futureN,
                double hitRate,
                double wilson,
                double brier,
                double ece,
                double coverage,
                int pairedN,
                double pairedDelta,
                double fdrQ, String frozenConsumerRecipe) {
            this(consensus,funnel,actions,rankOne,stability,actionSpaceHash,governanceApplied,governanceStatus,
                governanceCandidate,controlledInfluenceEligible,governanceLane,governanceName,governanceEvidence,
                governanceWeight,futureN,hitRate,wilson,brier,ece,coverage,pairedN,pairedDelta,fdrQ,frozenConsumerRecipe,null);
        }
        ActionStage(V51EffectiveConsensusEngine.Consensus[] consensus,K3FunnelOrchestrator.Audit funnel,
                V51Models.PositionAction[] actions,V51Models.PositionAction rankOne,V51RankStabilityEngine.Result stability,
                String actionSpaceHash,boolean governanceApplied,String governanceStatus,String governanceCandidate,
                boolean controlledInfluenceEligible,String governanceLane,String governanceName,String governanceEvidence,
                double governanceWeight,int futureN,double hitRate,double wilson,double brier,double ece,double coverage,
                int pairedN,double pairedDelta,double fdrQ,String frozenConsumerRecipe,CertifiedPredictionR974.Proof proof){
            this.certifiedPrediction=proof;
            this.frozenConsumerRecipe=frozenConsumerRecipe;
            this.consensus = consensus;
            this.funnel = funnel;
            this.actions = actions;
            this.rankOne = rankOne;
            this.stability = stability;
            this.actionSpaceHash = actionSpaceHash;
            this.governanceApplied = governanceApplied;
            this.governanceStatus = governanceStatus == null ? "" : governanceStatus;
            this.governanceCandidate = governanceCandidate == null ? "" : governanceCandidate;
            this.controlledInfluenceEligible = controlledInfluenceEligible;
            this.governanceLane = governanceLane == null ? "PRODUCTION_FALLBACK" : governanceLane;
            this.governanceName = governanceName == null ? "" : governanceName;
            this.governanceEvidence = governanceEvidence == null ? "" : governanceEvidence;
            this.governanceWeight = governanceWeight;
            this.futureN = Math.max(0, futureN);
            this.hitRate = hitRate;
            this.wilson = wilson;
            this.brier = brier;
            this.ece = ece;
            this.coverage = coverage;
            this.pairedN = Math.max(0, pairedN);
            this.pairedDelta = pairedDelta;
            this.fdrQ = fdrQ;
        }

        ResearchMainlineContributionAuditR922.Contribution contribution(
                String targetIssue, long createdAt) {
            return new ResearchMainlineContributionAuditR922.Contribution(
                    targetIssue,
                    governanceLane,
                    governanceCandidate,
                    governanceName,
                    governanceStatus,
                    governanceApplied,
                    governanceWeight,
                    futureN,
                    hitRate,
                    wilson,
                    brier,
                    ece,
                    coverage,
                    pairedN,
                    pairedDelta,
                    fdrQ,
                    governanceEvidence,
                    rankOne == null ? "" : rankOne.checksum,
                    controlledInfluenceEligible,
                    createdAt);
        }
    }

    private static V51Models.PermutationFeatureSnapshot previousFeatures(
            V51PermutationFeatureEngine engine, V51Models.DrawFrame[] history, long createdAt) {
        if (history.length <= 12) return null;
        V51Models.DrawFrame[] prior = new V51Models.DrawFrame[history.length - 1];
        System.arraycopy(history, 0, prior, 0, prior.length);
        V51Models.PredictionContext c =
                new V51Models.PredictionContext(
                        prior[prior.length - 1].issue,
                        history[history.length - 1].issue,
                        prior[prior.length - 1].issue,
                        V51PermutationFeatureEngine.historyHash(prior),
                        V51BuildManifest.COHORT_ID,
                        ModelBundleRuntimeR932.current().hash,
                        createdAt,
                        prior.length);
        return engine.build(c, prior);
    }

    private static String stateHash(V51Models.StateTrajectorySnapshot[] x) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < x.length; i++) b.append(x[i].checksum);
        return V51ChecksumEngine.sha256(b.toString());
    }

    private static String expertHash(V51Models.ExpertEvidence[] x) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < x.length; i++) b.append(x[i].checksum);
        return V51ChecksumEngine.sha256(b.toString());
    }

    private static String actionHash(V51Models.PositionAction[] x) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < x.length; i++) b.append(x[i].checksum);
        return V51ChecksumEngine.sha256(b.toString());
    }

    public static final class OutcomeR36 {
        public final V51RuntimeResult runtime;
        public final V51Repository.CanonicalCommitResult commit;
        public final String snapshotHash;
        public final RuntimeTraceR36 trace;

        OutcomeR36(
                V51RuntimeResult r,
                V51Repository.CanonicalCommitResult c,
                String h,
                RuntimeTraceR36 t) {
            runtime = r;
            commit = c;
            snapshotHash = h;
            trace = t;
        }
    }

    private static ModelBundleRuntimeR932.Scope openBundleTracedCP3(
            DbStore store, AnalysisSingleFlightR36.Request request, RuntimeTraceR36 trace) {
        RuntimeTraceR36.Mark mark = trace.begin("MODEL_BUNDLE_OPEN_CP3");
        boolean ok = false;
        try {
            ModelBundleRuntimeR932.Scope scope = ModelBundleRuntimeR932.open(
                    store, request.targetIssue, request.requestAt);
            ok = true;
            return scope;
        } finally {
            trace.end(mark, ok, ok ? "BUNDLE_FROZEN" : "BUNDLE_OPEN_FAILED");
        }
    }
}
