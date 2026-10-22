package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.List;

/** Unique persistent Research orchestrator. It has no Production writer or promotion authority. */
public final class AutonomousResearchEvolutionEngine {
    public enum ResearchState {
        IDLE,
        COLLECT,
        DIAGNOSE,
        HYPOTHESIZE,
        GENERATE,
        STATIC_VALIDATE,
        WALK_FORWARD,
        ABLATE,
        CALIBRATE,
        STRESS,
        CERTIFY_RESEARCH,
        SHADOW_CHALLENGER,
        OBSERVE,
        ACCEPT_RESEARCH,
        REJECT,
        ROLLBACK
    }

    public TickResult tick(DbStore store) {
        if (store == null) return TickResult.yield("STORE_NULL");
        if (RealtimePressureGateR811.hasActiveCriticalWork()) return TickResult.yield("YIELD_REALTIME");
        /* Schema creation and the first four planning states are bounded metadata work.  R816
         * incorrectly put them behind a near-empty Production backlog, so a device with deferred
         * evidence could remain RESEARCH_SCHEMA_NOT_READY forever. */
        if (!store.coordinatorR36().ensureR811ResearchSchemaOnce())
            return TickResult.yield("YIELD_SCHEMA_OR_REALTIME");
        Run run = load(store);
        StorageHealthR811.Snapshot storage = StorageHealthR811.snapshot(store);
        DiagnosticSnapshotR811 diag = DiagnosticSnapshotR811.capture(store);
        boolean planning =
                run == null
                        || run.state.ordinal() <= ResearchState.HYPOTHESIZE.ordinal();
        ResearchBudgetGovernorR811.Metrics metrics =
                new ResearchBudgetGovernorR811.Metrics(
                        planning ? 0 : diag.settlementPending,
                        planning ? 0 : diag.deferredPending,
                        AnalysisSingleFlightR36.shared().activeCount() == 0,
                        planning ? 0L : parseLong(store.getState("r811_live_p95_ms", "0")),
                        storage.lowStorage);
        ResearchBudgetGovernorR811.Slice slice = ResearchBudgetGovernorR811.beginSlice(metrics);
        if (!slice.admitted())
            return TickResult.yield(storage.lowStorage ? "YIELD_LOW_STORAGE" : "YIELD_BUDGET");
        try {
            if (run == null) return createRun(store, diag);
            if (run.state == ResearchState.ACCEPT_RESEARCH
                    || run.state == ResearchState.REJECT
                    || run.state == ResearchState.ROLLBACK) {
                DatasetSnapshot latest = freezeStrictOosDataset(store, diag.latestOos);
                if (latest.valid() && !latest.hash.equals(run.datasetHash))
                    return createRun(store, diag);
                return TickResult.ok(run, "TERMINAL_WAIT_NEW_STRICT_OOS");
            }
            if (run.state == ResearchState.OBSERVE) return closeFromFutureLiveDecision(store, run);

            ResearchState next = next(run.state);
            DatasetSnapshot dataset = freezeStrictOosDataset(store, run.dataUntilIssue);
            String blocked = evidenceGate(store, run, next, dataset);
            if (!blocked.isEmpty()) {
                if (next == ResearchState.CERTIFY_RESEARCH
                        && "WAIT_FROZEN_OOS_AND_FDR_EVIDENCE".equals(blocked)
                        && ResearchExperimentRunnerR811.allRuntimeCandidatesEvaluated(
                                store, run.runId))
                    return rejectGeneration(store, run, "ALL_EXECUTABLE_CHALLENGERS_FAILED_FDR");
                return TickResult.yield(blocked);
            }

            int generatedDelta = 0;
            int evaluatedDelta = 0;
            int shadowDelta = 0;
            String stageDetail;
            if (next == ResearchState.HYPOTHESIZE) {
                StrategyChallengerFactoryEngine.FindingBundle finding = hypothesize(store);
                persistHypothesis(store, run, finding);
                stageDetail =
                        V51ChecksumEngine.sha256(
                                finding.hypothesis + '|' + finding.counterEvidenceRequired);
            } else if (next == ResearchState.GENERATE) {
                List<StrategyChallengerFactoryEngine.CandidateSpec> candidates =
                        generate(store, dataset.hash, dataset.dataUntilIssue, dataset.issueN);
                if (candidates.isEmpty()) return TickResult.yield("WAIT_VALID_RESEARCH_CANDIDATE");
                persistCandidates(store, run, candidates);
                generatedDelta = candidates.size();
                stageDetail = candidateSetHash(candidates);
            } else if (next == ResearchState.STATIC_VALIDATE) {
                ResearchExperimentRunnerR811.BatchResult batch =
                        ResearchExperimentRunnerR811.runNext(
                                store, run.runId, run.datasetHash, run.dataUntilIssue);
                if (!batch.complete) return TickResult.yield(batch.status);
                stageDetail = "STATIC_VALIDATE_AND_EXPERIMENTS_COMPLETE|" + batch.status;
            } else {
                stageDetail = stage(next, store);
                if (next == ResearchState.WALK_FORWARD)
                    evaluatedDelta = experimentCandidateCount(store, run.runId);
                if (next == ResearchState.SHADOW_CHALLENGER) {
                    shadowDelta = markShadowChallengers(store, run.runId);
                    if (shadowDelta <= 0)
                        return TickResult.yield("WAIT_VERIFIED_SHADOW_CERTIFICATE");
                }
            }
            Run advanced =
                    new Run(
                            run.runId,
                            next,
                            run.targetIssue,
                            run.dataUntilIssue,
                            run.inputHash,
                            run.datasetHash,
                            run.generatedCount + generatedDelta,
                            run.evaluatedCount + evaluatedDelta,
                            run.shadowEligibleCount + shadowDelta,
                            "",
                            run.createdAt);
            persistRun(store, advanced);
            return TickResult.ok(advanced, stageDetail);
        } catch (Throwable error) {
            return TickResult.yield("RESEARCH_ERROR:" + error.getClass().getSimpleName());
        } finally {
            ResearchBudgetGovernorR811.finishSlice(slice, 1);
        }
    }

    public String collect(DbStore store) {
        DiagnosticSnapshotR811 snapshot = DiagnosticSnapshotR811.capture(store);
        return V51ChecksumEngine.sha256(
                "COLLECT|" + snapshot.latestOos + '|' + snapshot.shadowSettled);
    }

    public String diagnose(DbStore store) {
        DiagnosticSnapshotR811 snapshot = DiagnosticSnapshotR811.capture(store);
        return "CORE="
                + snapshot.coreStatus
                + "|FULL="
                + snapshot.fullStatus
                + "|OOS="
                + snapshot.latestOos;
    }

    public StrategyChallengerFactoryEngine.FindingBundle hypothesize(DbStore store) {
        ResearchFailureHypothesisEngineR811.Diagnosis diagnosis =
                ResearchFailureHypothesisEngineR811.diagnose(store);
        return new StrategyChallengerFactoryEngine.FindingBundle(
                diagnosis.primaryLayer,
                diagnosis.hypothesis,
                diagnosis.counterEvidence,
                "PRODUCTION_FROZEN",
                "ResearchDslCandidateRuntimeR811,ResearchModelCandidateRuntimeR811",
                "DecisionGateEngine,SettlementEngine,REAL_BET_UNLOCK");
    }

    public List<StrategyChallengerFactoryEngine.CandidateSpec> generate(
            DbStore store, String datasetHash, String dataUntilIssue, int issueN) {
        return StrategyChallengerFactoryEngine.generate(
                hypothesize(store),
                new StrategyChallengerFactoryEngine.ResearchDataset(
                        datasetHash, dataUntilIssue, issueN));
    }

    public String evaluate(String candidateHash, String datasetHash) {
        return V51ChecksumEngine.sha256(
                "EVALUATE|" + candidateHash + '|' + datasetHash + "|PIT|GROUP_BY_ISSUE");
    }

    public String report(DbStore store) {
        Run run = load(store);
        return run == null ? "RESEARCH_NOT_STARTED" : run.canonical();
    }

    /** Stages that must never be advanced from a timer alone. */
    public static boolean requiresExternalEvidence(ResearchState state) {
        return state == ResearchState.WALK_FORWARD
                || state == ResearchState.ABLATE
                || state == ResearchState.CALIBRATE
                || state == ResearchState.STRESS
                || state == ResearchState.CERTIFY_RESEARCH
                || state == ResearchState.SHADOW_CHALLENGER
                || state == ResearchState.OBSERVE;
    }

    public void persistRun(final DbStore store, final Run run) {
        if (store == null || run == null || RealtimePressureGateR811.hasActiveCriticalWork()) return;
        store.coordinatorR36()
                .runWrite(
                        "R811_RESEARCH_RUN",
                        new DatabaseCoordinator.WriteRunnable() {
                            public void run(SQLiteDatabase db) {
                                long now = TimeSourceR811.wallNowMs();
                                ContentValues values = new ContentValues();
                                values.put("run_id", run.runId);
                                values.put("state", run.state.name());
                                values.put("target_issue", run.targetIssue);
                                values.put("data_until_issue", run.dataUntilIssue);
                                values.put("input_hash", run.inputHash);
                                values.put("dataset_hash", run.datasetHash);
                                values.put("champion_hash", "");
                                values.put("generated_count", run.generatedCount);
                                values.put("evaluated_count", run.evaluatedCount);
                                values.put("shadow_eligible_count", run.shadowEligibleCount);
                                values.put("error_code", run.error);
                                values.put("error_detail", "");
                                values.put("created_at", run.createdAt);
                                values.put("updated_at", now);
                                db.insertWithOnConflict(
                                        "v57_research_run_r811",
                                        null,
                                        values,
                                        SQLiteDatabase.CONFLICT_REPLACE);
                            }
                        });
    }

    private TickResult createRun(DbStore store, DiagnosticSnapshotR811 diag) {
        DatasetSnapshot dataset = freezeStrictOosDataset(store, diag.latestOos);
        if (!dataset.valid()) return TickResult.yield("WAIT_STRICT_OOS_DATASET");
        String target =
                TargetIssueResolver.isValid(diag.targetIssue)
                        ? diag.targetIssue
                        : TargetIssueResolver.next(dataset.dataUntilIssue).targetIssue;
        String input =
                V51ChecksumEngine.sha256(
                        "R811_RESEARCH_INPUT|"
                                + target
                                + '|'
                                + dataset.dataUntilIssue
                                + '|'
                                + dataset.hash
                                + '|'
                                + V51BuildManifest.fingerprint());
        Run run =
                new Run(
                        "R811-RUN-" + input.substring(0, 24),
                        ResearchState.IDLE,
                        target,
                        dataset.dataUntilIssue,
                        input,
                        dataset.hash,
                        0,
                        0,
                        0,
                        "",
                        TimeSourceR811.wallNowMs());
        persistRun(store, run);
        return TickResult.ok(run, "CREATED|STRICT_OOS_N=" + dataset.issueN);
    }

    private TickResult closeFromFutureLiveDecision(DbStore store, Run run) {
        String decision = store.getState("r811_research_observation_decision", "");
        if (!"ACCEPT_RESEARCH".equals(decision)
                && !"REJECT".equals(decision)
                && !"ROLLBACK".equals(decision))
            return TickResult.yield("WAIT_FUTURE_LIVE_PROOF");
        ResearchState terminal = ResearchState.valueOf(decision);
        Run closed =
                new Run(
                        run.runId,
                        terminal,
                        run.targetIssue,
                        run.dataUntilIssue,
                        run.inputHash,
                        run.datasetHash,
                        run.generatedCount,
                        run.evaluatedCount,
                        run.shadowEligibleCount,
                        "",
                        run.createdAt);
        persistRun(store, closed);
        return TickResult.ok(closed, "EXPLICIT_FUTURE_LIVE_DECISION");
    }

    private String evidenceGate(
            DbStore store, Run run, ResearchState next, DatasetSnapshot dataset) {
        if (next == ResearchState.GENERATE) {
            if (!dataset.valid()) return "WAIT_STRICT_OOS_DATASET";
            // R900 creates auditable exploratory candidates immediately; effectiveness claims still wait for evidence.
            if (dataset.issueN < 1) return "WAIT_STRICT_OOS_DATASET";
        }
        if (next == ResearchState.STATIC_VALIDATE && candidateCount(store, run.runId) <= 0)
            return "WAIT_GENERATED_CANDIDATES";
        if (next == ResearchState.WALK_FORWARD) {
            if (dataset.issueN < 120) return "WAIT_STRICT_OOS_SCREENING_N_120";
            if (experimentRoleCount(store, run.runId, "WF_%") <= 0)
                return "WAIT_TIME_ORDERED_WALK_FORWARD_EVIDENCE";
        }
        if (next == ResearchState.ABLATE
                && experimentRoleCount(store, run.runId, "ABLATION") <= 0)
            return "WAIT_ABLATION_EVIDENCE";
        if (next == ResearchState.CALIBRATE
                && experimentRoleCount(store, run.runId, "CALIBRATION") <= 0)
            return "WAIT_CALIBRATION_EVIDENCE";
        if (next == ResearchState.STRESS
                && (experimentRoleCount(store, run.runId, "BLOCK_STRESS") <= 0
                        || experimentRoleCount(store, run.runId, "REGIME_STRESS") <= 0))
            return "WAIT_BLOCK_AND_REGIME_STRESS_EVIDENCE";
        if (next == ResearchState.CERTIFY_RESEARCH
                && verifiedFrozenCandidateCount(store, run.runId) <= 0)
            return "WAIT_FROZEN_OOS_AND_FDR_EVIDENCE";
        if (next == ResearchState.SHADOW_CHALLENGER) {
            if (dataset.issueN < 240) return "WAIT_STRICT_OOS_SHADOW_N_240";
            if (candidateStatusCount(store, run.runId, "SHADOW_ELIGIBLE") <= 0)
                return "WAIT_VERIFIED_SHADOW_CERTIFICATE";
        }
        if (next == ResearchState.OBSERVE
                && candidateStatusCount(store, run.runId, "SHADOW_CHALLENGER") <= 0)
            return "WAIT_SHADOW_CHALLENGER_ACTIVATION";
        return "";
    }

    private String stage(ResearchState next, DbStore store) {
        switch (next) {
            case COLLECT:
                return collect(store);
            case DIAGNOSE:
                return diagnose(store);
            case STATIC_VALIDATE:
                return "STATIC_VALIDATE_PASS";
            case WALK_FORWARD:
                return "WALK_FORWARD_EVIDENCE_VERIFIED";
            case ABLATE:
                return "ABLATION_EVIDENCE_VERIFIED";
            case CALIBRATE:
                return "CALIBRATION_EVIDENCE_VERIFIED";
            case STRESS:
                return "BLOCK_AND_REGIME_STRESS_VERIFIED";
            case CERTIFY_RESEARCH:
                return "FROZEN_OOS_AND_FDR_VERIFIED_NOT_PRODUCTION";
            case SHADOW_CHALLENGER:
                return "SHADOW_ONLY";
            case OBSERVE:
                return "FUTURE_LIVE_OBSERVE";
            default:
                return next.name();
        }
    }

    private void persistHypothesis(
            final DbStore store,
            final Run run,
            final StrategyChallengerFactoryEngine.FindingBundle finding) {
        if (store == null || run == null || finding == null) return;
        final ResearchFailureHypothesisEngineR811.Diagnosis diagnosis =
                ResearchFailureHypothesisEngineR811.diagnose(store);
        final StringBuilder failureVector = new StringBuilder(diagnosis.positiveEvidence);
        for (int i = 0; i < diagnosis.layers.size(); i++)
            failureVector.append('|').append(diagnosis.layers.get(i).canonical());
        store.coordinatorR36()
                .runWrite(
                        "R811_RESEARCH_HYPOTHESIS",
                        new DatabaseCoordinator.WriteRunnable() {
                            public void run(SQLiteDatabase db) {
                                long now = TimeSourceR811.wallNowMs();
                                ContentValues values = new ContentValues();
                                values.put("hypothesis_id", hypothesisId(run));
                                values.put("run_id", run.runId);
                                values.put("failure_layer", finding.failureLayer);
                                values.put("mechanism", finding.hypothesis);
                                values.put("positive_evidence", failureVector.toString());
                                values.put("counter_evidence", diagnosis.counterEvidence);
                                values.put(
                                        "affected_regime",
                                        "PIT_ONLY|PRIMARY_LAYER=" + diagnosis.primaryLayer);
                                values.put(
                                        "candidate_family",
                                        "FORMULA,EXPERT,CALIBRATION,TINY_MLP,ATTENTION,FTRL");
                                values.put(
                                        "acceptance_json",
                                        "{\"incubatorN\":12,\"screeningN\":120,\"shadowN\":240,"
                                                + "\"proofN\":480,\"hitRateTarget\":0.45,"
                                                + "\"wilsonLowerTarget\":0.40,\"coverageMin\":0.15,"
                                                + "\"eceMax\":0.04,\"walkForwardFolds\":4,"
                                                + "\"fdrQ\":0.05,\"groupUnit\":\"target_issue\","
                                                + "\"rollback\":\"retain_production_and_delete_research_pointer\","
                                                + "\"productionMutation\":false}");
                                values.put("status", "FALSIFIABLE_RESEARCH_ONLY");
                                values.put("created_at", now);
                                values.put("updated_at", now);
                                db.insertWithOnConflict(
                                        "v57_research_hypothesis_r811",
                                        null,
                                        values,
                                        SQLiteDatabase.CONFLICT_REPLACE);
                            }
                        });
    }

    private void persistCandidates(
            final DbStore store,
            final Run run,
            final List<StrategyChallengerFactoryEngine.CandidateSpec> candidates) {
        store.coordinatorR36()
                .runWrite(
                        "R811_RESEARCH_CANDIDATES",
                        new DatabaseCoordinator.WriteRunnable() {
                            public void run(SQLiteDatabase db) {
                                long now = TimeSourceR811.wallNowMs();
                                for (int i = 0; i < candidates.size(); i++) {
                                    StrategyChallengerFactoryEngine.CandidateSpec candidate =
                                            candidates.get(i);
                                    CandidateCodeSynthesisEngine.Bundle bundle =
                                            CandidateCodeSynthesisEngine.synthesize(candidate);
                                    ContentValues values = new ContentValues();
                                    values.put("candidate_id", candidate.candidateId);
                                    values.put("run_id", run.runId);
                                    values.put("hypothesis_id", hypothesisId(run));
                                    values.put("candidate_type", candidate.type.name());
                                    values.put("parent_champion_hash", candidate.parentChampionHash);
                                    values.put("canonical_spec", candidate.canonicalSpec);
                                    values.put("dataset_hash", candidate.datasetHash);
                                    values.put(
                                            "formula_ast",
                                            candidate.type
                                                            == StrategyChallengerFactoryEngine.CandidateType.FORMULA
                                                    ? candidate.canonicalSpec
                                                    : "");
                                    values.put("changed_symbols", candidate.changedSymbols);
                                    values.put("forbidden_symbols", candidate.forbiddenSymbols);
                                    values.put("seed", candidate.seed);
                                    values.put("complexity", candidate.complexity);
                                    values.put("generation", candidate.generation);
                                    values.put("genetic_operator", candidate.geneticOperator);
                                    values.put("parent_formula_ids", candidate.parentFormulaIds);
                                    values.put(
                                            "feature_dependencies",
                                            candidate.featureDependencies);
                                    values.put("code_hash", bundle.bundleHash);
                                    values.put("artifact_hash", candidate.candidateHash);
                                    values.put("runtime_executable", candidate.runtimeExecutable ? 1 : 0);
                                    values.put("status", candidate.status);
                                    values.put("created_at", now);
                                    values.put("updated_at", now);
                                    db.insertWithOnConflict(
                                            "v57_research_candidate_r811",
                                            null,
                                            values,
                                            SQLiteDatabase.CONFLICT_REPLACE);

                                    ContentValues code = new ContentValues();
                                    code.put("candidate_id", candidate.candidateId);
                                    code.put(
                                            "bundle_path",
                                            "research/generated/"
                                                    + candidate.candidateId.replaceAll(
                                                            "[^A-Za-z0-9_]", "_")
                                                    + ".java");
                                    code.put("java_files", "1");
                                    code.put("patch_sha256", V51ChecksumEngine.sha256(bundle.patch));
                                    code.put(
                                            "manifest_sha256",
                                            V51ChecksumEngine.sha256(bundle.manifest));
                                    code.put("compile_status", bundle.status);
                                    code.put("owner_audit_status", "PASS_RESEARCH_ONLY_NO_OWNER");
                                    code.put("java_source", bundle.javaSource);
                                    code.put("patch_text", bundle.patch);
                                    code.put("manifest_text", bundle.manifest);
                                    code.put("fixtures_text", bundle.fixtures);
                                    code.put("bundle_hash", bundle.bundleHash);
                                    code.put("generated_at", now);
                                    db.insertWithOnConflict(
                                            "v57_research_codegen_r811",
                                            null,
                                            code,
                                            SQLiteDatabase.CONFLICT_REPLACE);
                                }
                            }
                        });
    }

    private TickResult rejectGeneration(DbStore store, Run run, String reason) {
        Run rejected =
                new Run(
                        run.runId,
                        ResearchState.REJECT,
                        run.targetIssue,
                        run.dataUntilIssue,
                        run.inputHash,
                        run.datasetHash,
                        run.generatedCount,
                        run.evaluatedCount,
                        run.shadowEligibleCount,
                        reason,
                        run.createdAt);
        persistRun(store, rejected);
        return TickResult.ok(rejected, "REJECTED_AND_WAIT_NEW_STRICT_OOS|" + reason);
    }

    private int markShadowChallengers(final DbStore store, final String runId) {
        final int eligible = candidateStatusCount(store, runId, "SHADOW_ELIGIBLE");
        if (eligible <= 0) return 0;
        store.coordinatorR36()
                .runWrite(
                        "R811_RESEARCH_SHADOW_ACTIVATE",
                        new DatabaseCoordinator.WriteRunnable() {
                            public void run(SQLiteDatabase db) {
                                ContentValues values = new ContentValues();
                                values.put("status", "SHADOW_CHALLENGER");
                                values.put("updated_at", TimeSourceR811.wallNowMs());
                                db.update(
                                        "v57_research_candidate_r811",
                                        values,
                                        "status=? AND runtime_executable=1 AND run_id=?",
                                        new String[] {"SHADOW_ELIGIBLE", runId});
                            }
                        });
        return eligible;
    }

    private static DatasetSnapshot freezeStrictOosDataset(DbStore store, String untilIssue) {
        if (store == null || !TargetIssueResolver.isValid(untilIssue))
            return DatasetSnapshot.empty();
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT target_issue,record_hash FROM "
                                            + DbHelper.TABLE_R35_OOS_LABEL
                                            + " WHERE strict_oos=1 AND target_issue<=?"
                                            + " ORDER BY target_issue ASC",
                                    new String[] {untilIssue});
            StringBuilder canonical = new StringBuilder("R811_STRICT_OOS_DATASET_V1\n");
            int issueN = 0;
            String latest = "";
            while (cursor.moveToNext()) {
                String issue = safe(cursor.getString(0));
                String recordHash = safe(cursor.getString(1));
                if (!TargetIssueResolver.isValid(issue) || recordHash.length() != 64) continue;
                canonical.append(issue).append('|').append(recordHash).append('\n');
                issueN++;
                latest = issue;
            }
            return issueN == 0
                    ? DatasetSnapshot.empty()
                    : new DatasetSnapshot(
                            issueN, latest, V51ChecksumEngine.sha256(canonical.toString()));
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static int candidateCount(DbStore store, String runId) {
        return integer(
                store,
                "SELECT COUNT(1) FROM v57_research_candidate_r811 WHERE run_id=?",
                new String[] {runId});
    }

    private static int candidateStatusCount(DbStore store, String runId, String status) {
        return integer(
                store,
                "SELECT COUNT(1) FROM v57_research_candidate_r811"
                        + " WHERE run_id=? AND status=? AND runtime_executable=1",
                new String[] {runId, status});
    }

    private static int experimentRoleCount(DbStore store, String runId, String role) {
        return integer(
                store,
                "SELECT COUNT(DISTINCT e.candidate_id) FROM v57_research_experiment_r811 e"
                        + " JOIN v57_research_candidate_r811 c ON c.candidate_id=e.candidate_id"
                        + " WHERE c.run_id=? AND e.split_role LIKE ?",
                new String[] {runId, role});
    }

    private static int experimentCandidateCount(DbStore store, String runId) {
        return integer(
                store,
                "SELECT COUNT(DISTINCT e.candidate_id) FROM v57_research_experiment_r811 e"
                        + " JOIN v57_research_candidate_r811 c ON c.candidate_id=e.candidate_id"
                        + " WHERE c.run_id=? AND e.split_role LIKE 'WF_%'",
                new String[] {runId});
    }

    private static int verifiedFrozenCandidateCount(DbStore store, String runId) {
        return integer(
                store,
                "SELECT COUNT(DISTINCT e.candidate_id) FROM v57_research_experiment_r811 e"
                        + " JOIN v57_research_candidate_r811 c ON c.candidate_id=e.candidate_id"
                        + " WHERE c.run_id=? AND e.split_role='FROZEN_TEST' AND e.n>=120"
                        + " AND e.fdr_pass=1 AND e.result_hash IS NOT NULL"
                        + " AND length(e.result_hash)=64",
                new String[] {runId});
    }

    private static int integer(DbStore store, String sql, String[] args) {
        Cursor cursor = null;
        try {
            cursor = store.readable().rawQuery(sql, args);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String candidateSetHash(
            List<StrategyChallengerFactoryEngine.CandidateSpec> candidates) {
        StringBuilder canonical = new StringBuilder("R811_CANDIDATE_SET_V1\n");
        for (int i = 0; i < candidates.size(); i++)
            canonical.append(candidates.get(i).candidateHash).append('\n');
        return V51ChecksumEngine.sha256(canonical.toString());
    }

    private static String hypothesisId(Run run) {
        return "R811-HYP-"
                + V51ChecksumEngine.sha256(run.runId + "|HYPOTHESIS").substring(0, 24);
    }

    private static ResearchState next(ResearchState state) {
        ResearchState[] states = ResearchState.values();
        for (int i = 0; i < states.length - 3; i++)
            if (states[i] == state) return states[i + 1];
        return ResearchState.OBSERVE;
    }

    private static Run load(DbStore store) {
        if (store == null || !store.coordinatorR36().isR811ResearchSchemaReady()) return null;
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT run_id,state,target_issue,data_until_issue,input_hash,dataset_hash,"
                                            + "generated_count,evaluated_count,shadow_eligible_count,error_code,created_at"
                                            + " FROM v57_research_run_r811 ORDER BY updated_at DESC LIMIT 1",
                                    null);
            if (!cursor.moveToFirst()) return null;
            ResearchState state;
            try {
                state = ResearchState.valueOf(cursor.getString(1));
            } catch (Throwable ignored) {
                state = ResearchState.ROLLBACK;
            }
            return new Run(
                    cursor.getString(0),
                    state,
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getInt(6),
                    cursor.getInt(7),
                    cursor.getInt(8),
                    cursor.getString(9),
                    cursor.getLong(10));
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class DatasetSnapshot {
        final int issueN;
        final String dataUntilIssue, hash;

        DatasetSnapshot(int issueN, String until, String hash) {
            this.issueN = issueN;
            dataUntilIssue = safe(until);
            this.hash = safe(hash);
        }

        boolean valid() {
            return issueN > 0
                    && TargetIssueResolver.isValid(dataUntilIssue)
                    && hash.length() == 64;
        }

        static DatasetSnapshot empty() {
            return new DatasetSnapshot(0, "", "");
        }
    }

    public static final class Run {
        public final String runId, targetIssue, dataUntilIssue, inputHash, datasetHash, error;
        public final ResearchState state;
        public final int generatedCount, evaluatedCount, shadowEligibleCount;
        public final long createdAt;

        Run(
                String id,
                ResearchState state,
                String target,
                String dataUntil,
                String input,
                String dataset,
                int generated,
                int evaluated,
                int shadow,
                String error,
                long createdAt) {
            runId = id;
            this.state = state;
            targetIssue = target;
            dataUntilIssue = dataUntil;
            inputHash = input;
            datasetHash = dataset;
            generatedCount = generated;
            evaluatedCount = evaluated;
            shadowEligibleCount = shadow;
            this.error = error;
            this.createdAt = createdAt;
        }

        public String canonical() {
            return runId
                    + '|'
                    + state
                    + '|'
                    + targetIssue
                    + '|'
                    + dataUntilIssue
                    + '|'
                    + inputHash
                    + '|'
                    + datasetHash
                    + '|'
                    + generatedCount
                    + '|'
                    + evaluatedCount
                    + '|'
                    + shadowEligibleCount
                    + '|'
                    + error;
        }
    }

    public static final class TickResult {
        public final boolean advanced;
        public final String status, detail;
        public final Run run;

        private TickResult(boolean advanced, String status, String detail, Run run) {
            this.advanced = advanced;
            this.status = status;
            this.detail = detail;
            this.run = run;
        }

        static TickResult yield(String status) {
            return new TickResult(false, status, "", null);
        }

        static TickResult ok(Run run, String detail) {
            return new TickResult(true, run.state.name(), detail, run);
        }
    }
}
