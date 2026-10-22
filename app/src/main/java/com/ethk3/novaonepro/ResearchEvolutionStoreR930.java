package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Append-oriented SQLite evidence store for Batch4.
 *
 * <p>All writes are serialized by DatabaseCoordinatorR36.  Rows are content-addressed and use
 * insert-ignore or compare-and-set semantics, so retries cannot inflate support.  This store is
 * evidence only: it cannot create a prediction, settlement or REAL action.</p>
 */
public final class ResearchEvolutionStoreR930 {
    public static final String VERSION = "R930_RESEARCH_EVOLUTION_STORE_V1";
    public static final String T_ATOM = "research_atom_contract_r930";
    public static final String T_ATOM_QUALITY = "research_atom_quality_r930";
    public static final String T_CAPTURE = "research_capture_stage_r930";
    public static final String T_MATRIX = "research_matrix_snapshot_r930";
    public static final String T_ABLATION = "research_ablation_r930";
    public static final String T_FAILURE = "research_issue_failure_r930";
    public static final String T_TASK = "research_scheduler_task_r930";
    public static final String T_LIFECYCLE = "research_lifecycle_event_r930";
    public static final String T_DEDUPE = "research_dedupe_r930";
    public static final String T_FRONTIER = "research_frequency_frontier_r930";
    public static final String T_GRAPH_NODE = "research_contribution_node_r930";
    public static final String T_GRAPH_EDGE = "research_contribution_edge_r930";
    public static final String T_EVENT = "research_evolution_event_r930";
    public static final boolean MAY_CREATE_REAL = false;

    private static final java.util.WeakHashMap<SQLiteDatabase,Boolean> READY=new java.util.WeakHashMap<SQLiteDatabase,Boolean>();
    private static final Object SCHEMA_LOCK = new Object();

    private ResearchEvolutionStoreR930() {}

    public static void ensureSchema(final DbStore store) {
        if (store == null) throw new IllegalArgumentException("R930_STORE_REQUIRED");
        synchronized (SCHEMA_LOCK) {
            SQLiteDatabase handle=store.readable();
            if(READY.containsKey(handle))return;
                Boolean ok = store.coordinatorR36().runAtomicWrite("R930_EVOLUTION_SCHEMA",
                    new DatabaseCoordinator.WriteCallable<Boolean>() {
                        @Override public Boolean call(SQLiteDatabase db) {
                            createSchema(db);
                            return Boolean.TRUE;
                        }
                    }, Boolean.FALSE);
            if (!Boolean.TRUE.equals(ok))
                throw new IllegalStateException("R930_EVOLUTION_SCHEMA_FAILED");
            READY.put(handle,Boolean.TRUE);
        }
    }

    public static void createSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R930_DB_REQUIRED");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_ATOM + "("
                + "qualified_id TEXT PRIMARY KEY NOT NULL,namespace TEXT NOT NULL,node_id TEXT NOT NULL,"
                + "semantic TEXT NOT NULL,node_kind TEXT NOT NULL,value_type TEXT NOT NULL,"
                + "shape TEXT NOT NULL,stage TEXT NOT NULL,unit TEXT NOT NULL,lag INTEGER NOT NULL,"
                + "window_size INTEGER NOT NULL,missing_policy TEXT NOT NULL,source TEXT NOT NULL,"
                + "availability_contract TEXT NOT NULL,p95_budget_ns INTEGER NOT NULL,"
                + "legacy_protected INTEGER NOT NULL,version TEXT NOT NULL,structural_hash TEXT NOT NULL UNIQUE,"
                + "registered_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_atom_stage ON " + T_ATOM
                + "(stage,legacy_protected,registered_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_ATOM_QUALITY + "("
                + "evidence_hash TEXT PRIMARY KEY NOT NULL,qualified_id TEXT NOT NULL,support INTEGER NOT NULL,"
                + "available_n INTEGER NOT NULL,pit_failures INTEGER NOT NULL,missing_rate REAL NOT NULL,"
                + "mean_value REAL NOT NULL,variance REAL NOT NULL,mean_quality REAL NOT NULL,"
                + "p95_latency_ns INTEGER NOT NULL,recent_drift_z REAL NOT NULL,admission_state TEXT NOT NULL,"
                + "production_eligible INTEGER NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_atom_quality ON " + T_ATOM_QUALITY
                + "(qualified_id,created_at,admission_state)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_CAPTURE + "("
                + "capture_hash TEXT PRIMARY KEY NOT NULL,target_issue TEXT NOT NULL,max_source_issue TEXT NOT NULL,"
                + "stage TEXT NOT NULL,frozen_at INTEGER NOT NULL,freeze_hash TEXT NOT NULL,input_hash TEXT NOT NULL,"
                + "expected_rows INTEGER NOT NULL,captured_rows INTEGER NOT NULL,missing_rows INTEGER NOT NULL,"
                + "cost_ns INTEGER NOT NULL,payload_hash TEXT NOT NULL,complete INTEGER NOT NULL,created_at INTEGER NOT NULL,"
                + "UNIQUE(target_issue,stage,freeze_hash))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_capture_issue ON " + T_CAPTURE
                + "(target_issue,stage,created_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_MATRIX + "("
                + "matrix_hash TEXT PRIMARY KEY NOT NULL,snapshot_hash TEXT NOT NULL,target_issue TEXT NOT NULL,"
                + "max_source_issue TEXT NOT NULL,kind TEXT NOT NULL,frozen_at INTEGER NOT NULL,"
                + "input_stage TEXT NOT NULL,graph_hash TEXT NOT NULL,input_hash TEXT NOT NULL,"
                + "bundle_hash TEXT NOT NULL,model_hash TEXT NOT NULL,present_cells INTEGER NOT NULL,"
                + "created_at INTEGER NOT NULL,UNIQUE(snapshot_hash,kind))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_matrix_issue ON " + T_MATRIX
                + "(target_issue,kind,created_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_ABLATION + "("
                + "summary_hash TEXT PRIMARY KEY NOT NULL,candidate_hash TEXT NOT NULL,node_id TEXT NOT NULL,"
                + "paired_n INTEGER NOT NULL,selected_pair_n INTEGER NOT NULL,probability_delta REAL,"
                + "brier_delta REAL,nll_delta REAL,action_change_rate REAL NOT NULL,cost_saved_ns REAL NOT NULL,"
                + "positive_effects INTEGER NOT NULL,sign_test_p REAL NOT NULL,support_state TEXT NOT NULL,"
                + "useful_contribution INTEGER NOT NULL,created_at INTEGER NOT NULL,"
                + "UNIQUE(candidate_hash,node_id,summary_hash))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_ablation_candidate ON " + T_ABLATION
                + "(candidate_hash,node_id,paired_n,created_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_FAILURE + "("
                + "failure_hash TEXT PRIMARY KEY NOT NULL,task_hash TEXT NOT NULL,candidate_hash TEXT NOT NULL,"
                + "target_issue TEXT NOT NULL,stage TEXT NOT NULL,code TEXT NOT NULL,detail TEXT NOT NULL,"
                + "retryable INTEGER NOT NULL,production_fallback_succeeded INTEGER NOT NULL,"
                + "elapsed_ns INTEGER NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_failure_issue ON " + T_FAILURE
                + "(target_issue,candidate_hash,stage,created_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_TASK + "("
                + "task_hash TEXT PRIMARY KEY NOT NULL,task_id TEXT NOT NULL,candidate_hash TEXT NOT NULL,"
                + "family_id TEXT NOT NULL,tier TEXT NOT NULL,priority INTEGER NOT NULL,target_issue TEXT NOT NULL,"
                + "input_hash TEXT NOT NULL,graph_hash TEXT NOT NULL,experiment_id TEXT NOT NULL,"
                + "estimated_cost_ns INTEGER NOT NULL,attempt INTEGER NOT NULL,status TEXT NOT NULL,"
                + "reason TEXT NOT NULL,updated_at INTEGER NOT NULL,UNIQUE(task_id,target_issue))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_task_schedule ON " + T_TASK
                + "(target_issue,tier,status,priority,updated_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_LIFECYCLE + "("
                + "event_hash TEXT PRIMARY KEY NOT NULL,candidate_hash TEXT NOT NULL,from_state TEXT NOT NULL,"
                + "to_state TEXT NOT NULL,event_type TEXT NOT NULL,window_hash TEXT NOT NULL,"
                + "parent_event_hash TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_lifecycle_candidate ON " + T_LIFECYCLE
                + "(candidate_hash,created_at,event_hash)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_DEDUPE + "("
                + "decision_hash TEXT PRIMARY KEY NOT NULL,proposal_hash TEXT NOT NULL,admit INTEGER NOT NULL,"
                + "nearest_hash TEXT NOT NULL,similarity_class TEXT NOT NULL,behavior_cosine REAL,"
                + "residual_cosine REAL,behavior_agreement REAL,reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_dedupe_proposal ON " + T_DEDUPE
                + "(proposal_hash,created_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_FRONTIER + "("
                + "point_hash TEXT PRIMARY KEY NOT NULL,frontier_hash TEXT NOT NULL,policy_hash TEXT NOT NULL,"
                + "window_role TEXT NOT NULL,window_hash TEXT NOT NULL,planned_n INTEGER NOT NULL,"
                + "selected_n INTEGER NOT NULL,hit_n INTEGER NOT NULL,hit_rate REAL,coverage REAL NOT NULL,"
                + "brier REAL,nll REAL,ece REAL,stability REAL,uncertainty REAL,p95_latency_ms INTEGER NOT NULL,"
                + "longest_miss_streak INTEGER NOT NULL,dominated INTEGER NOT NULL,utility REAL NOT NULL,"
                + "status TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r930_frontier_window ON " + T_FRONTIER
                + "(window_hash,window_role,status,created_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_GRAPH_NODE + "("
                + "graph_hash TEXT NOT NULL,node_id TEXT NOT NULL,node_type TEXT NOT NULL,artifact_hash TEXT NOT NULL,"
                + "node_state TEXT NOT NULL,namespace TEXT NOT NULL,node_hash TEXT NOT NULL,created_at INTEGER NOT NULL,"
                + "PRIMARY KEY(graph_hash,node_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_GRAPH_EDGE + "("
                + "graph_hash TEXT NOT NULL,parent_id TEXT NOT NULL,child_id TEXT NOT NULL,semantic TEXT NOT NULL,"
                + "maximum_weight REAL NOT NULL,certificate_hash TEXT NOT NULL,edge_hash TEXT NOT NULL,"
                + "created_at INTEGER NOT NULL,PRIMARY KEY(graph_hash,parent_id,child_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_EVENT + "("
                + "event_hash TEXT PRIMARY KEY NOT NULL,event_type TEXT NOT NULL,entity_id TEXT NOT NULL,"
                + "entity_hash TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
    }

    public static boolean putAdmission(final DbStore store,
            final TypedAtomMoleculeGraphR930.Admission admission, final long nowMs) {
        if (admission == null || nowMs <= 0L)
            throw new IllegalArgumentException("R930_ADMISSION_PERSIST_INPUT");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R930_ATOM_ADMISSION_PUT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        TypedAtomMoleculeGraphR930.NodeSpec node = admission.node;
                        ContentValues atom = new ContentValues();
                        atom.put("qualified_id", node.qualifiedId());
                        atom.put("namespace", node.namespace);
                        atom.put("node_id", node.id);
                        atom.put("semantic", node.semantic);
                        atom.put("node_kind", node.kind.name());
                        atom.put("value_type", node.valueType.name());
                        atom.put("shape", node.shape.canonical());
                        atom.put("stage", node.stage.name());
                        atom.put("unit", node.unit);
                        atom.put("lag", node.lag);
                        atom.put("window_size", node.window);
                        atom.put("missing_policy", node.missingPolicy.name());
                        atom.put("source", node.source);
                        atom.put("availability_contract", node.availabilityContract);
                        atom.put("p95_budget_ns", node.p95CostBudgetNs);
                        atom.put("legacy_protected", node.legacyProtected ? 1 : 0);
                        atom.put("version", node.version);
                        atom.put("structural_hash", node.structuralHash);
                        atom.put("registered_at", nowMs);
                        if (db.insertWithOnConflict(T_ATOM, null, atom,
                                SQLiteDatabase.CONFLICT_REPLACE) < 0L) return Boolean.FALSE;
                        TypedAtomMoleculeGraphR930.AtomQuality quality = admission.quality;
                        ContentValues q = new ContentValues();
                        q.put("evidence_hash", quality.evidenceHash);
                        q.put("qualified_id", quality.qualifiedId);
                        q.put("support", quality.support);
                        q.put("available_n", quality.availableN);
                        q.put("pit_failures", quality.pitFailures);
                        q.put("missing_rate", quality.missingRate);
                        q.put("mean_value", quality.mean);
                        q.put("variance", quality.variance);
                        q.put("mean_quality", quality.meanQuality);
                        q.put("p95_latency_ns", quality.p95LatencyNs);
                        q.put("recent_drift_z", quality.recentDriftZ);
                        q.put("admission_state", admission.state.name());
                        q.put("production_eligible", admission.productionEligible ? 1 : 0);
                        q.put("reason", admission.reason);
                        q.put("created_at", nowMs);
                        boolean inserted = db.insertWithOnConflict(T_ATOM_QUALITY, null, q,
                                SQLiteDatabase.CONFLICT_IGNORE) >= 0L;
                        event(db, "ATOM_ADMISSION", node.qualifiedId(), admission.admissionHash,
                                inserted ? "RECORDED" : "IDEMPOTENT_DUPLICATE", admission.reason, nowMs);
                        return Boolean.valueOf(inserted);
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static boolean putSnapshot(final DbStore store,
            final MatrixSnapshotFeedbackR930.CommonSnapshot snapshot, final long nowMs) {
        if (snapshot == null || nowMs <= 0L)
            throw new IllegalArgumentException("R930_SNAPSHOT_PERSIST_INPUT");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R930_MATRIX_SNAPSHOT_PUT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        for (MatrixSnapshotFeedbackR930.CaptureRecord record
                                : snapshot.capture.stages.values()) {
                            ContentValues values = new ContentValues();
                            values.put("capture_hash", record.captureHash);
                            values.put("target_issue", record.targetIssue);
                            values.put("max_source_issue", record.maxSourceIssue);
                            values.put("stage", record.stage.name());
                            values.put("frozen_at", record.frozenAtMs);
                            values.put("freeze_hash", record.freezeHash);
                            values.put("input_hash", record.inputHash);
                            values.put("expected_rows", record.expectedRows);
                            values.put("captured_rows", record.capturedRows);
                            values.put("missing_rows", record.missingRows);
                            values.put("cost_ns", record.costNs);
                            values.put("payload_hash", record.payloadHash);
                            values.put("complete", record.complete() ? 1 : 0);
                            values.put("created_at", nowMs);
                            if (db.insertWithOnConflict(T_CAPTURE, null, values,
                                    SQLiteDatabase.CONFLICT_IGNORE) < 0L) return Boolean.FALSE;
                        }
                        for (MatrixSnapshotFeedbackR930.FrozenMatrix matrix
                                : snapshot.matrices.values()) {
                            ContentValues values = new ContentValues();
                            values.put("matrix_hash", matrix.matrixHash);
                            values.put("snapshot_hash", snapshot.snapshotHash);
                            values.put("target_issue", matrix.targetIssue);
                            values.put("max_source_issue", matrix.maxSourceIssue);
                            values.put("kind", matrix.kind.name());
                            values.put("frozen_at", matrix.frozenAtMs);
                            values.put("input_stage", matrix.inputStage);
                            values.put("graph_hash", matrix.graphHash);
                            values.put("input_hash", matrix.inputHash);
                            values.put("bundle_hash", snapshot.bundleHash);
                            values.put("model_hash", matrix.modelHash);
                            values.put("present_cells", matrix.presentCells());
                            values.put("created_at", nowMs);
                            if (db.insertWithOnConflict(T_MATRIX, null, values,
                                    SQLiteDatabase.CONFLICT_IGNORE) < 0L) return Boolean.FALSE;
                        }
                        event(db, "COMMON_MATRIX_SNAPSHOT", snapshot.targetIssue,
                                snapshot.snapshotHash, "RECORDED", "COMMON_FREEZE_PASS", nowMs);
                        return Boolean.TRUE;
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static boolean putAblation(final DbStore store,
            final AblationRepairCreationR930.AblationSummary summary, final long nowMs) {
        if (summary == null || nowMs <= 0L)
            throw new IllegalArgumentException("R930_ABLATION_PERSIST_INPUT");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R930_ABLATION_PUT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        ContentValues values = new ContentValues();
                        values.put("summary_hash", summary.summaryHash);
                        values.put("candidate_hash", summary.candidateHash);
                        values.put("node_id", summary.nodeId);
                        values.put("paired_n", summary.pairedN);
                        values.put("selected_pair_n", summary.selectedPairN);
                        putFiniteOrNull(values, "probability_delta", summary.meanProbabilityDelta);
                        putFiniteOrNull(values, "brier_delta", summary.meanBrierIncreaseWhenRemoved);
                        putFiniteOrNull(values, "nll_delta", summary.meanNllIncreaseWhenRemoved);
                        values.put("action_change_rate", summary.actionChangeRate);
                        values.put("cost_saved_ns", summary.meanCostSavedNs);
                        values.put("positive_effects", summary.positiveBrierEffects);
                        values.put("sign_test_p", summary.signTestP);
                        values.put("support_state", summary.supportState);
                        values.put("useful_contribution",
                                summary.evidenceOfUsefulContribution() ? 1 : 0);
                        values.put("created_at", nowMs);
                        boolean inserted = db.insertWithOnConflict(T_ABLATION, null, values,
                                SQLiteDatabase.CONFLICT_IGNORE) >= 0L;
                        event(db, "TRUE_ABLATION", summary.candidateHash, summary.summaryHash,
                                inserted ? "RECORDED" : "IDEMPOTENT_DUPLICATE",
                                summary.supportState, nowMs);
                        return Boolean.valueOf(inserted);
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static boolean putFailure(final DbStore store,
            final ResearchSchedulerLifecycleR930.IssueFailure failure, final long nowMs) {
        if (failure == null || nowMs <= 0L)
            throw new IllegalArgumentException("R930_FAILURE_PERSIST_INPUT");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R930_FAILURE_PUT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        ContentValues values = new ContentValues();
                        values.put("failure_hash", failure.failureHash);
                        values.put("task_hash", failure.taskHash);
                        values.put("candidate_hash", failure.candidateHash);
                        values.put("target_issue", failure.targetIssue);
                        values.put("stage", failure.stage);
                        values.put("code", failure.code);
                        values.put("detail", failure.detail);
                        values.put("retryable", failure.retryable ? 1 : 0);
                        values.put("production_fallback_succeeded",
                                failure.productionFallbackSucceeded ? 1 : 0);
                        values.put("elapsed_ns", failure.elapsedNs);
                        values.put("created_at", nowMs);
                        return Boolean.valueOf(db.insertWithOnConflict(T_FAILURE, null, values,
                                SQLiteDatabase.CONFLICT_IGNORE) >= 0L);
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static boolean putTask(final DbStore store,
            final ResearchSchedulerLifecycleR930.ResearchTask task,
            final String status, final String reason, final long nowMs) {
        if (task == null || nowMs <= 0L)
            throw new IllegalArgumentException("R930_TASK_PERSIST_INPUT");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R930_TASK_PUT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        ContentValues values = new ContentValues();
                        values.put("task_hash", task.taskHash);
                        values.put("task_id", task.taskId);
                        values.put("candidate_hash", task.candidateHash);
                        values.put("family_id", task.familyId);
                        values.put("tier", task.tier.name());
                        values.put("priority", task.priority);
                        values.put("target_issue", task.targetIssue);
                        values.put("input_hash", task.inputHash);
                        values.put("graph_hash", task.graphHash);
                        values.put("experiment_id", task.experimentId);
                        values.put("estimated_cost_ns", task.estimatedCostNs);
                        values.put("attempt", task.attempt);
                        values.put("status", required(status, "TASK_STATUS"));
                        values.put("reason", clean(reason));
                        values.put("updated_at", nowMs);
                        return Boolean.valueOf(db.insertWithOnConflict(T_TASK, null, values,
                                SQLiteDatabase.CONFLICT_REPLACE) >= 0L);
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static boolean putLifecycleEvent(final DbStore store,
            final ResearchSchedulerLifecycleR930.LifecycleEvent lifecycle, final long nowMs) {
        if (lifecycle == null || nowMs <= 0L)
            throw new IllegalArgumentException("R930_LIFECYCLE_PERSIST_INPUT");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R930_LIFECYCLE_PUT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        ContentValues values = new ContentValues();
                        values.put("event_hash", lifecycle.eventHash);
                        values.put("candidate_hash", lifecycle.candidateHash);
                        values.put("from_state", lifecycle.from.name());
                        values.put("to_state", lifecycle.to.name());
                        values.put("event_type", lifecycle.type.name());
                        values.put("window_hash", lifecycle.windowHash);
                        values.put("parent_event_hash", lifecycle.parentEventHash);
                        values.put("reason", lifecycle.reason);
                        values.put("created_at", lifecycle.createdAtMs);
                        return Boolean.valueOf(db.insertWithOnConflict(T_LIFECYCLE, null, values,
                                SQLiteDatabase.CONFLICT_IGNORE) >= 0L);
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static boolean putDedupe(final DbStore store,
            final CandidateDiversityContributionR930.DedupeDecision decision,
            final long nowMs) {
        if (decision == null || nowMs <= 0L)
            throw new IllegalArgumentException("R930_DEDUPE_PERSIST_INPUT");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R930_DEDUPE_PUT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        ContentValues values = new ContentValues();
                        values.put("decision_hash", decision.decisionHash);
                        values.put("proposal_hash", decision.proposal.signatureHash);
                        values.put("admit", decision.admit ? 1 : 0);
                        values.put("nearest_hash", decision.nearest == null ? ""
                                : decision.nearest.right.signatureHash);
                        values.put("similarity_class", decision.nearest == null ? "NONE"
                                : decision.nearest.classification.name());
                        putFiniteOrNull(values, "behavior_cosine", decision.nearest == null
                                ? Double.NaN : decision.nearest.behaviorCosine);
                        putFiniteOrNull(values, "residual_cosine", decision.nearest == null
                                ? Double.NaN : decision.nearest.residualCosine);
                        putFiniteOrNull(values, "behavior_agreement", decision.nearest == null
                                ? Double.NaN : decision.nearest.behaviorAgreement);
                        values.put("reason", decision.reason);
                        values.put("created_at", nowMs);
                        return Boolean.valueOf(db.insertWithOnConflict(T_DEDUPE, null, values,
                                SQLiteDatabase.CONFLICT_IGNORE) >= 0L);
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static boolean putFrontier(final DbStore store,
            final FrequencyQualityFrontierR930.FrontierResult frontier, final long nowMs) {
        if (frontier == null || nowMs <= 0L)
            throw new IllegalArgumentException("R930_FRONTIER_PERSIST_INPUT");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R930_FRONTIER_PUT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        for (FrequencyQualityFrontierR930.FrontierPoint point : frontier.points) {
                            FrequencyQualityFrontierR930.Metrics metrics = point.metrics;
                            ContentValues values = new ContentValues();
                            values.put("point_hash", point.pointHash);
                            values.put("frontier_hash", frontier.frontierHash);
                            values.put("policy_hash", metrics.policy.policyHash);
                            values.put("window_role", metrics.role.name());
                            values.put("window_hash", metrics.windowContentHash);
                            values.put("planned_n", metrics.plannedN);
                            values.put("selected_n", metrics.selectedN);
                            values.put("hit_n", metrics.hitN);
                            putFiniteOrNull(values, "hit_rate", metrics.hitRate);
                            values.put("coverage", metrics.coverage);
                            putFiniteOrNull(values, "brier", metrics.brier);
                            putFiniteOrNull(values, "nll", metrics.nll);
                            putFiniteOrNull(values, "ece", metrics.ece);
                            putFiniteOrNull(values, "stability", metrics.meanStability);
                            putFiniteOrNull(values, "uncertainty", metrics.meanUncertainty);
                            values.put("p95_latency_ms", metrics.p95LatencyMs);
                            values.put("longest_miss_streak", metrics.longestMissStreak);
                            values.put("dominated", point.dominated ? 1 : 0);
                            values.put("utility", point.utility);
                            values.put("status", frontier.status.name());
                            values.put("reason", frontier.reason);
                            values.put("created_at", nowMs);
                            if (db.insertWithOnConflict(T_FRONTIER, null, values,
                                    SQLiteDatabase.CONFLICT_IGNORE) < 0L) return Boolean.FALSE;
                        }
                        return Boolean.TRUE;
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static boolean putContributionGraph(final DbStore store,
            final CandidateDiversityContributionR930.ContributionGraph graph,
            final long nowMs) {
        if (graph == null || nowMs <= 0L)
            throw new IllegalArgumentException("R930_GRAPH_PERSIST_INPUT");
        ensureSchema(store);
        Boolean result = store.coordinatorR36().runAtomicWrite("R930_CONTRIBUTION_GRAPH_PUT",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    @Override public Boolean call(SQLiteDatabase db) {
                        for (CandidateDiversityContributionR930.ContributionNode node
                                : graph.nodes.values()) {
                            ContentValues values = new ContentValues();
                            values.put("graph_hash", graph.graphHash);
                            values.put("node_id", node.nodeId);
                            values.put("node_type", node.type.name());
                            values.put("artifact_hash", node.artifactHash);
                            values.put("node_state", node.state.name());
                            values.put("namespace", node.namespace);
                            values.put("node_hash", node.nodeHash);
                            values.put("created_at", nowMs);
                            if (db.insertWithOnConflict(T_GRAPH_NODE, null, values,
                                    SQLiteDatabase.CONFLICT_REPLACE) < 0L) return Boolean.FALSE;
                        }
                        for (CandidateDiversityContributionR930.ContributionEdge edge : graph.edges) {
                            ContentValues values = new ContentValues();
                            values.put("graph_hash", graph.graphHash);
                            values.put("parent_id", edge.parentNodeId);
                            values.put("child_id", edge.childNodeId);
                            values.put("semantic", edge.semantic);
                            values.put("maximum_weight", edge.maximumWeight);
                            values.put("certificate_hash", edge.certificateHash);
                            values.put("edge_hash", edge.edgeHash);
                            values.put("created_at", nowMs);
                            if (db.insertWithOnConflict(T_GRAPH_EDGE, null, values,
                                    SQLiteDatabase.CONFLICT_REPLACE) < 0L) return Boolean.FALSE;
                        }
                        event(db, "CONTRIBUTION_GRAPH", graph.graphHash, graph.graphHash,
                                "RECORDED", "NODES=" + graph.nodes.size() + "|EDGES="
                                        + graph.edges.size(), nowMs);
                        return Boolean.TRUE;
                    }
                }, Boolean.FALSE);
        return Boolean.TRUE.equals(result);
    }

    public static Map<String, Long> diagnosticCounts(DbStore store) {
        if (store == null) return Collections.emptyMap();
        ensureSchema(store);
        LinkedHashMap<String, Long> output = new LinkedHashMap<String, Long>();
        String[] tables = new String[] {T_ATOM, T_ATOM_QUALITY, T_CAPTURE, T_MATRIX,
                T_ABLATION, T_FAILURE, T_TASK, T_LIFECYCLE, T_DEDUPE, T_FRONTIER,
                T_GRAPH_NODE, T_GRAPH_EDGE, T_EVENT};
        SQLiteDatabase db = store.readable();
        for (String table : tables) {
            Cursor cursor = null;
            try {
                cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
                output.put(table, Long.valueOf(cursor.moveToFirst() ? cursor.getLong(0) : 0L));
            } catch (Throwable ignored) {
                output.put(table, Long.valueOf(-1L));
            } finally { if (cursor != null) cursor.close(); }
        }
        return Collections.unmodifiableMap(output);
    }

    private static void putFiniteOrNull(ContentValues values, String key, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) values.putNull(key);
        else values.put(key, value);
    }

    private static void event(SQLiteDatabase db, String type, String entityId,
            String entityHash, String status, String reason, long nowMs) {
        String eventHash = TypedAtomMoleculeGraphR930.sha256(type + '|' + entityId + '|'
                + entityHash + '|' + status + '|' + reason + '|' + nowMs);
        ContentValues values = new ContentValues();
        values.put("event_hash", eventHash);
        values.put("event_type", type);
        values.put("entity_id", entityId);
        values.put("entity_hash", entityHash);
        values.put("status", status);
        values.put("reason", reason);
        values.put("created_at", nowMs);
        db.insertWithOnConflict(T_EVENT, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private static String clean(String value) { return value == null ? "" : value.trim(); }
    private static String required(String value, String code) {
        String clean = clean(value);
        if (clean.length() == 0) throw new IllegalArgumentException("R930_" + code + "_REQUIRED");
        return clean;
    }
}
