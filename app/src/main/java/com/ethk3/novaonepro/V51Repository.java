package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 星核天枢B05唯一V51预测仓储。所有预测与影子结算以同一事务写入，可幂等、可回放。 */
public final class V51Repository {
    public static final String ISSUE = "v51_issue_prediction",
            NUMBER = "v51_number_event",
            ACTION = "v51_position_action",
            ACTION_SPACE = "v57_k3_action_space_1200",
            CHAMPION = "v57_k3_champion_freeze",
            FUNNEL = "v57_k3_funnel_trace",
            PERMUTATION = "v51_permutation_matrix",
            SELECTED = "v51_selected_action",
            AUTH = "v51_decision_authorization",
            SPAN = "v51_performance_span";
    public static final String GAME_CODE = "Binance1-PK10";
    public static final String VERSION = "V57.5.0_TIANSHU_B05_REPOSITORY";

    public static final class StorageRule {
        public final String id, owner, invariant;
        public final boolean mandatory;
        public final int severity;

        StorageRule(String id, String owner, String invariant, boolean mandatory, int severity) {
            this.id = id;
            this.owner = owner;
            this.invariant = invariant;
            this.mandatory = mandatory;
            this.severity = severity;
        }

        public String canonical() {
            return id + '|' + owner + '|' + invariant + '|' + (mandatory ? 1 : 0) + '|' + severity;
        }
    }

    public static final class PersistReport {
        public final boolean success, replay;
        public final String targetIssue, bundleId, bundleHash, recordHash, detail;
        public final int numberRows, actionRows, selectedRows;

        PersistReport(
                boolean success,
                boolean replay,
                String target,
                String id,
                String hash,
                String record,
                int numbers,
                int actions,
                int selected,
                String detail) {
            this.success = success;
            this.replay = replay;
            targetIssue = safe(target);
            bundleId = safe(id);
            bundleHash = safe(hash);
            recordHash = safe(record);
            numberRows = numbers;
            actionRows = actions;
            selectedRows = selected;
            this.detail = safe(detail);
        }

        public String canonical() {
            return success
                    + "|"
                    + replay
                    + "|"
                    + targetIssue
                    + "|"
                    + bundleId
                    + "|"
                    + bundleHash
                    + "|"
                    + recordHash
                    + "|"
                    + numberRows
                    + "|"
                    + actionRows
                    + "|"
                    + selectedRows
                    + "|"
                    + detail;
        }
    }

    public static final class SettlementReport {
        public final boolean success, replay;
        public final String targetIssue, result, settlementHash, recordHash, detail;

        SettlementReport(
                boolean success,
                boolean replay,
                String target,
                String result,
                String hash,
                String record,
                String detail) {
            this.success = success;
            this.replay = replay;
            targetIssue = safe(target);
            this.result = safe(result);
            settlementHash = safe(hash);
            recordHash = safe(record);
            this.detail = safe(detail);
        }
    }

    private V51Repository() {}

    public static void ensure(DbStore store) {
        if (store == null) throw new IllegalArgumentException("B03_STORE_NULL");
        ensure(store.writable());
    }

    public static void ensure(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("B03_DB_NULL");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + ISSUE
                        + "(target_issue TEXT PRIMARY KEY,base_issue TEXT NOT NULL,history_hash"
                        + " TEXT NOT NULL,feature_hash TEXT NOT NULL,state_hash TEXT NOT"
                        + " NULL,expert_hash TEXT NOT NULL,artifact_bundle_hash TEXT NOT"
                        + " NULL,cohort_id TEXT NOT NULL,status TEXT NOT NULL,created_at INTEGER"
                        + " NOT NULL,settled INTEGER NOT NULL DEFAULT 0,actual_permutation TEXT"
                        + " DEFAULT '',result TEXT DEFAULT '')");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + NUMBER
                        + "(target_issue TEXT NOT NULL,position INTEGER NOT NULL,number INTEGER NOT"
                        + " NULL,raw_score REAL NOT NULL,number_p REAL NOT NULL,number_rank INTEGER"
                        + " NOT NULL,state_id TEXT NOT NULL,domain_id TEXT NOT NULL,actual_label"
                        + " INTEGER NOT NULL DEFAULT -1,expert_hash TEXT NOT NULL,PRIMARY"
                        + " KEY(target_issue,position,number))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + ACTION
                        + "(target_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT"
                        + " NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,n4 INTEGER NOT"
                        + " NULL,raw_action_p REAL NOT NULL,rank_score REAL NOT NULL,selected_hit_p"
                        + " REAL NOT NULL DEFAULT 0,rank_no INTEGER NOT NULL,hit_label INTEGER NOT"
                        + " NULL DEFAULT -1,fingerprint TEXT NOT NULL,meta_blob TEXT NOT"
                        + " NULL,action_hash TEXT NOT NULL,PRIMARY KEY(target_issue,position))");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + ACTION_SPACE
                        + "(target_issue TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT"
                        + " NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,n4 INTEGER NOT"
                        + " NULL,raw_action_p REAL NOT NULL,rank_score REAL NOT NULL,selected_hit_p"
                        + " REAL NOT NULL DEFAULT 0,rank_no INTEGER NOT NULL,hit_label INTEGER NOT"
                        + " NULL DEFAULT -1,fingerprint TEXT NOT NULL,meta_blob TEXT NOT"
                        + " NULL,action_hash TEXT NOT NULL,game_code TEXT NOT NULL DEFAULT"
                        + " 'Binance1-PK10',bundle_hash TEXT NOT NULL DEFAULT '',contract_hash TEXT"
                        + " NOT NULL DEFAULT '',record_hash TEXT NOT NULL DEFAULT '',PRIMARY"
                        + " KEY(target_issue,position,n1,n2,n3))");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_v57_action_space_rank ON "
                        + ACTION_SPACE
                        + "(game_code,target_issue,rank_no)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + CHAMPION
                        + "(target_issue TEXT PRIMARY KEY,position INTEGER NOT NULL,n1 INTEGER NOT"
                        + " NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,rank_score REAL NOT"
                        + " NULL,action_hash TEXT NOT NULL,ranker_artifact TEXT NOT"
                        + " NULL,artifact_proven INTEGER NOT NULL,frozen INTEGER NOT NULL,game_code"
                        + " TEXT NOT NULL DEFAULT 'Binance1-PK10',bundle_hash TEXT NOT NULL DEFAULT"
                        + " '',record_hash TEXT NOT NULL DEFAULT '')");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + FUNNEL
                        + "(target_issue TEXT NOT NULL,position INTEGER NOT NULL,trace_blob TEXT"
                        + " NOT NULL,final3 TEXT NOT NULL,matrix_hash TEXT NOT NULL,game_code TEXT"
                        + " NOT NULL DEFAULT 'Binance1-PK10',bundle_hash TEXT NOT NULL DEFAULT"
                        + " '',record_hash TEXT NOT NULL DEFAULT '',PRIMARY"
                        + " KEY(target_issue,position))");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_v57_champion_game_issue ON "
                        + CHAMPION
                        + "(game_code,target_issue)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_v57_funnel_game_issue ON "
                        + FUNNEL
                        + "(game_code,target_issue,position)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + PERMUTATION
                        + "(target_issue TEXT PRIMARY KEY,matrix_blob TEXT NOT NULL,row_error REAL"
                        + " NOT NULL,column_error REAL NOT NULL,iterations INTEGER NOT"
                        + " NULL,converged INTEGER NOT NULL,used_fallback INTEGER NOT NULL,map_blob"
                        + " TEXT NOT NULL,matrix_hash TEXT NOT NULL)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + SELECTED
                        + "(target_issue TEXT PRIMARY KEY,decision_id TEXT NOT NULL UNIQUE,position"
                        + " INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT"
                        + " NULL,selected_hit_p REAL NOT NULL,predictability REAL NOT NULL,lane"
                        + " TEXT NOT NULL,decision_action TEXT NOT NULL,reject_code TEXT NOT"
                        + " NULL,contract_hash TEXT NOT NULL,authorization_hash TEXT NOT"
                        + " NULL,settled INTEGER NOT NULL DEFAULT 0,result TEXT DEFAULT '')");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + AUTH
                        + "(decision_id TEXT PRIMARY KEY,target_issue TEXT NOT NULL,contract_hash"
                        + " TEXT NOT NULL,artifact_bundle_hash TEXT NOT NULL,cohort_id TEXT NOT"
                        + " NULL,probability_owner TEXT NOT NULL,rank_owner TEXT NOT"
                        + " NULL,lane_owner TEXT NOT NULL,authorization_hash TEXT NOT"
                        + " NULL,created_at INTEGER NOT NULL)");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS "
                        + SPAN
                        + "(_id INTEGER PRIMARY KEY AUTOINCREMENT,target_issue TEXT NOT"
                        + " NULL,trace_id TEXT NOT NULL,span_name TEXT NOT NULL,parent_name TEXT"
                        + " NOT NULL,elapsed_ms INTEGER NOT NULL,exclusive_ms INTEGER NOT"
                        + " NULL,success INTEGER NOT NULL,failure_owner TEXT NOT NULL,created_at"
                        + " INTEGER NOT NULL)");
        addColumn(db, ISSUE, "game_code TEXT NOT NULL DEFAULT 'Binance1-PK10'");
        addColumn(db, ISSUE, "bundle_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, ISSUE, "contract_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, ISSUE, "record_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, NUMBER, "game_code TEXT NOT NULL DEFAULT 'Binance1-PK10'");
        addColumn(db, NUMBER, "bundle_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, NUMBER, "contract_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, NUMBER, "record_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, ACTION, "game_code TEXT NOT NULL DEFAULT 'Binance1-PK10'");
        addColumn(db, ACTION, "bundle_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, ACTION, "contract_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, ACTION, "record_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, PERMUTATION, "game_code TEXT NOT NULL DEFAULT 'Binance1-PK10'");
        addColumn(db, PERMUTATION, "bundle_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, PERMUTATION, "contract_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, PERMUTATION, "record_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, SELECTED, "game_code TEXT NOT NULL DEFAULT 'Binance1-PK10'");
        addColumn(db, SELECTED, "domain_id TEXT NOT NULL DEFAULT ''");
        addColumn(db, SELECTED, "rank_no INTEGER NOT NULL DEFAULT 0");
        addColumn(db, SELECTED, "bundle_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, SELECTED, "record_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, SELECTED, "settlement_hash TEXT NOT NULL DEFAULT ''");
        // R900: persist the exact selected-action evidence needed by UI/diagnosis.
        addColumn(db, SELECTED, "raw_action_p REAL NOT NULL DEFAULT -1");
        addColumn(db, SELECTED, "rank_score REAL NOT NULL DEFAULT 0");
        addColumn(db, SELECTED, "third_margin REAL NOT NULL DEFAULT 0");
        addColumn(db, SELECTED, "entropy REAL NOT NULL DEFAULT -1");
        addColumn(db, SELECTED, "stability REAL NOT NULL DEFAULT -1");
        addColumn(db, SELECTED, "window_consensus REAL NOT NULL DEFAULT -1");
        addColumn(db, SELECTED, "assignment_support REAL NOT NULL DEFAULT -1");
        // R912 visible, pre-draw shape truth for the selected position.  These columns are
        // additive on DB v36 and are frozen with the same canonical prediction transaction.
        addColumn(db, SELECTED, "shape_lifecycle TEXT NOT NULL DEFAULT 'UNKNOWN'");
        addColumn(db, SELECTED, "shape_prototype TEXT NOT NULL DEFAULT ''");
        addColumn(db, SELECTED, "shape_run_length INTEGER NOT NULL DEFAULT 0");
        addColumn(db, SELECTED, "shape_change_risk REAL NOT NULL DEFAULT -1");
        addColumn(db, SELECTED, "shape_confidence REAL NOT NULL DEFAULT -1");
        addColumn(db, AUTH, "game_code TEXT NOT NULL DEFAULT 'Binance1-PK10'");
        addColumn(db, AUTH, "bundle_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, AUTH, "record_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, SPAN, "game_code TEXT NOT NULL DEFAULT 'Binance1-PK10'");
        addColumn(db, SPAN, "bundle_hash TEXT NOT NULL DEFAULT ''");
        addColumn(db, SPAN, "record_hash TEXT NOT NULL DEFAULT ''");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_v51_number_issue ON "
                        + NUMBER
                        + "(game_code,target_issue)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_v51_action_issue_rank ON "
                        + ACTION
                        + "(game_code,target_issue,rank_no)");
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_v51_selected_lane ON "
                        + SELECTED
                        + "(game_code,lane,decision_action,settled)");
        db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_v51_issue_game_target ON "
                        + ISSUE
                        + "(game_code,target_issue)");
        DbHelper.ensureTianshuB03(db);
    }

    public static void persist(DbStore store, V51RuntimeResult result) {
        PersistReport report = persistWithReport(store, result);
        if (report == null || !report.success)
            throw new IllegalStateException(
                    report == null ? "B03_PERSIST_NULL" : "B03_PERSIST_FAILED:" + report.detail);
    }

    /**
     * R3.5 real-time core: one immutable pre-draw champion/selection/ authorization observation.
     * The 100-number and formal 1200-action ledgers are intentionally handled by
     * DeferredEvidenceWriterR35.
     */
    public static PersistReport persistRealtimeCore(DbStore store, V51RuntimeResult result) {
        if (store == null || result == null || !result.success || result.context == null)
            return fail("", "R35_CORE_INVALID_INPUT");
        ShadowObservationWriterR35.WriteResult written =
                ShadowObservationWriterR35.writeFrozen(store, result);
        if (!written.success)
            throw new IllegalStateException("R35_CORE_PERSIST_FAILED:" + written.status);
        String target = safe(result.context.targetIssue);
        String bundle = safe(result.checksum);
        try {
            store.setState("r35_realtime_core_status", written.status);
            store.setState("r35_realtime_core_target", target);
            store.setState("r35_realtime_core_hash", written.recordHash);
        } catch (Throwable ignore) {
        }
        return new PersistReport(
                true,
                written.replay,
                target,
                "R35-" + target,
                bundle,
                written.recordHash,
                0,
                0,
                result.selected == null ? 0 : 1,
                written.status);
    }

    /**
     * R3.6 real-time commit boundary. This transaction deliberately excludes the 100-number and
     * 1200-action loops; those rows are materialized from the durable snapshot by
     * DeferredEvidenceWriterR36.
     */
    public static CanonicalCommitResult commitCanonicalBundleR36(
            final DbStore store,
            final AnalysisSingleFlightR36.Request request,
            final String leaseOwner,
            final V51RuntimeResult result,
            final byte[] snapshotBytes,
            final String snapshotHash,
            final RuntimeTraceR36 trace) {
        validateCanonicalInputR36(store, request, result, snapshotBytes, snapshotHash);
        final DeferredEvidenceSnapshotR36.Snapshot snapshot =
                DeferredEvidenceSnapshotR36.decode(snapshotBytes);
        return commitCanonicalBundleVerifiedR368(
                store,
                request,
                leaseOwner,
                result,
                snapshotBytes,
                snapshotHash,
                snapshot.header(),
                trace);
    }

    /**
     * R3.6.8 low-peak boundary.  The live chain has just streamed and hashed the payload, so the
     * atomic transaction consumes its immutable header instead of decoding another 1200-row object
     * graph while the model result is still resident.
     */
    public static CanonicalCommitResult commitCanonicalBundleR36(
            final DbStore store,
            final AnalysisSingleFlightR36.Request request,
            final String leaseOwner,
            final V51RuntimeResult result,
            final DeferredEvidenceSnapshotR36.EncodedSnapshot encoded,
            final RuntimeTraceR36 trace) {
        if (encoded == null || encoded.header == null)
            throw new IllegalArgumentException("R368_CANONICAL_ENCODED_SNAPSHOT_NULL");
        validateCanonicalInputR36(
                store, request, result, encoded.bytes, encoded.sha256);
        return commitCanonicalBundleVerifiedR368(
                store,
                request,
                leaseOwner,
                result,
                encoded.bytes,
                encoded.sha256,
                encoded.header,
                trace);
    }

    private static CanonicalCommitResult commitCanonicalBundleVerifiedR368(
            final DbStore store,
            final AnalysisSingleFlightR36.Request request,
            final String leaseOwner,
            final V51RuntimeResult result,
            final byte[] snapshotBytes,
            final String snapshotHash,
            final DeferredEvidenceSnapshotR36.Header snapshot,
            final RuntimeTraceR36 trace) {
        if (leaseOwner == null || leaseOwner.trim().length() == 0)
            throw new IllegalArgumentException("R366_CANONICAL_LEASE_OWNER_EMPTY");
        if (!snapshotHash.equals(DeferredEvidenceSnapshotR36.sha256(snapshotBytes)))
            throw new IllegalStateException("R36_SNAPSHOT_HASH_MISMATCH");
        if (!request.targetIssue.equals(snapshot.targetIssue)
                || !request.baseIssue.equals(snapshot.baseIssue)
                || !result.context.historyHash.equals(snapshot.historyHash))
            throw new IllegalStateException("R36_SNAPSHOT_CONTEXT_MISMATCH");
        final long commitAt = System.currentTimeMillis();
        final CanonicalObservationWriterR36.CanonicalRecord canonical =
                CanonicalObservationWriterR36.fromRuntime(
                        request, result, snapshot, snapshotHash, commitAt);
        DatabaseCoordinator.TransactionResult<CanonicalCommitResult> outcome =
                store.coordinatorR36()
                        .runCanonicalTransactionR36(
                                request.targetIssue,
                                request.attemptId(),
                                new DatabaseCoordinator.TransactionCallable<
                                        CanonicalCommitResult>() {
                                    @Override
                                    public CanonicalCommitResult call(
                                            SQLiteDatabase db,
                                            DatabaseCoordinator.TransactionContext tx) {
                                        assertPreDrawR36(db, request, result, snapshot);
                                        CanonicalObservationWriterR36.WriteResult existing = null;
                                        if (CanonicalObservationWriterR36
                                                .hasCanonicalInTransactionR36(
                                                        db, request.targetIssue)) {
                                            existing =
                                                    CanonicalObservationWriterR36
                                                            .insertWithinTransaction(
                                                                    db, tx, canonical);
                                        }
                                        insertIssueCoreR36(db, result, snapshot);
                                        insertChampionCoreR36(db, result, snapshot);
                                        insertSelectedCoreR36(db, result, snapshot);
                                        // R914 correctness boundary: the values displayed as raw
                                        // probability, action stability and number risk are part of
                                        // the immutable pre-draw decision.  They must commit with the
                                        // canonical observation, never in a lossy best-effort task.
                                        SelectedEvidenceSnapshotR906
                                                .freeze(result, snapshot, commitAt)
                                                .insertWithinTransaction(db);
                                        LegacyPredrawContextBridgeR915.freezeWithinTransaction(
                                                db, result, commitAt);
                                        insertAuthorizationCoreR36(db, result, snapshot, commitAt);
                                        long realTradeId =
                                                store.insertAuthorizedTradeWithinCanonicalR915(
                                                        db, result, commitAt);
                                        ControlledCandidateOpportunityAuditR922
                                                .insertWithinTransaction(
                                                        db, result.opportunityAuditR922);
                                        ResearchMainlineContributionAuditR922.validateExactWithinTransaction(
                                                db,result.contributionAuditR922,result.contract);
                                        ResearchMainlineContributionAuditR922
                                                .insertWithinTransaction(
                                                        db, result.contributionAuditR922);
                                        CanonicalObservationWriterR36.WriteResult written =
                                                existing == null
                                                        ? CanonicalObservationWriterR36
                                                                .insertWithinTransaction(
                                                                        db, tx, canonical)
                                                        : existing;
                                        insertDeferredJobR36(
                                                db,
                                                snapshot,
                                                snapshotBytes,
                                                snapshotHash,
                                                commitAt);
                                        AnalysisRequestStoreR36
                                                .markCanonicalCommittedWithinTransaction(
                                                        db,
                                                        request,
                                                        leaseOwner,
                                                        written.recordHash,
                                                        commitAt);
                                        long requestToCommit =
                                                Math.max(0L, commitAt - request.requestAt);
                                        RuntimeHealthLedgerR36.recordWithinTransaction(
                                                db,
                                                "ANALYSIS_CANONICAL_COMMITTED",
                                                request.attemptId(),
                                                request.targetIssue,
                                                "SUCCESS",
                                                request.requestAt,
                                                commitAt,
                                                requestToCommit,
                                                AnalysisSingleFlightR36.shared().activeCount(),
                                                AnalysisSingleFlightR36.shared().maximumObserved(),
                                                true,
                                                "CORE_ATOMIC|SELECTED_EVIDENCE_ATOMIC|DEFERRED_SNAPSHOT_DURABLE|REAL_TRADE_ID="
                                                        + realTradeId);
                                        Map<String, String> states = new HashMap<String, String>();
                                        states.put("r36_analysis_status", "CANONICAL_COMMITTED");
                                        if (trace != null) states.put("r923_hot_path_stage_trace",
                                                trace.compactStagesR923());
                                        states.put("r36_analysis_target", request.targetIssue);
                                        states.put(
                                                "r36_analysis_canonical_hash", written.recordHash);
                                        states.put("r36_analysis_snapshot_hash", snapshotHash);
                                        states.put(
                                                "r36_analysis_request_to_commit_ms",
                                                String.valueOf(requestToCommit));
                                        states.put("r36_deferred_status", "PENDING");
                                        states.put(
                                                "r915_real_trade_id", String.valueOf(realTradeId));
                                        states.put(
                                                "r915_real_trade_mode",
                                                realTradeId > 0L
                                                        ? "CONTROLLED_CANDIDATE_MODEL_NOT_PROVEN"
                                                        : "NO_ELIGIBLE_ACTION");
                                        DbStore.putStatesR36(db, states);
                                        RuntimeEventHooksR926.canonicalWithinTransaction(
                                                db,store,request,snapshot,snapshotHash,trace,written.recordHash,result.contract);
                                        return new CanonicalCommitResult(
                                                true,
                                                written.replay,
                                                request.targetIssue,
                                                written.recordHash,
                                                snapshotHash,
                                                snapshot.bundleHash,
                                                commitAt,
                                                requestToCommit,
                                                written.status);
                                    }
                                },
                                null);
        if (outcome.success && outcome.value != null) return outcome.value;
        if (outcome.success && outcome.replay) {
            CanonicalObservationWriterR36.CanonicalView view =
                    CanonicalObservationWriterR36.readView(store, request.targetIssue);
            if (view != null
                    && snapshotHash.equals(view.snapshotHash)
                    && snapshot.actionSpaceHash.equals(view.actionSpaceHash))
                return new CanonicalCommitResult(
                        true,
                        true,
                        request.targetIssue,
                        view.recordHash,
                        snapshotHash,
                        snapshot.bundleHash,
                        Math.max(view.createdAt, request.requestAt),
                        Math.max(0L, view.createdAt - request.requestAt),
                        "AUDIT_REPLAY");
        }
        throw new IllegalStateException("R36_CANONICAL_COMMIT_FAILED:" + outcome.code);
    }

    private static void validateCanonicalInputR36(
            DbStore store,
            AnalysisSingleFlightR36.Request request,
            V51RuntimeResult result,
            byte[] snapshot,
            String snapshotHash) {
        if (store == null
                || request == null
                || !request.valid()
                || result == null
                || !result.success
                || result.context == null
                || result.features == null
                || result.states == null
                || result.numberModel == null
                || result.selected == null
                || result.selected.action == null
                || result.selected.predictability == null
                || result.authorized == null
                || result.opportunityAuditR922 == null
                || !result.opportunityAuditR922.valid()
                || result.contributionAuditR922 == null
                || !result.contributionAuditR922.valid())
            throw new IllegalArgumentException("R36_CANONICAL_INPUT_INVALID");
        if (snapshot == null
                || snapshot.length == 0
                || snapshotHash == null
                || snapshotHash.length() != 64)
            throw new IllegalArgumentException("R36_CANONICAL_SNAPSHOT_INVALID");
        if (!request.baseIssue.equals(result.context.baseIssue)
                || !request.targetIssue.equals(result.context.targetIssue)
                || !request.baseIssue.equals(result.context.historyMaxIssue)
                || !TargetIssueResolver.exact(request.baseIssue, request.targetIssue).valid)
            throw new IllegalStateException("R36_CANONICAL_PIT_INVALID");
        String action = result.authorized.action.name();
        if ("BET".equals(action)
                && !V51CandidateRealGateR915.verifyAuthorized(
                        null, result.contract, result.authorized))
            throw new IllegalStateException("R915_REAL_AUTHORIZATION_INVALID");
    }

    private static void assertPreDrawR36(
            SQLiteDatabase db,
            AnalysisSingleFlightR36.Request request,
            V51RuntimeResult result,
            DeferredEvidenceSnapshotR36.Header snapshot) {
        if (!db.inTransaction()) throw new IllegalStateException("R36_CORE_TX_REQUIRED");
        if(request.deadlineAt<=System.currentTimeMillis()||request.deadlineMonoAt<=TimeSourceR811.monoNowMs())
            throw new IllegalStateException("R974_CANONICAL_DEADLINE_EXCEEDED");
        if (intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_DRAWS
                                + " WHERE issue=? AND dirty=0",
                        new String[] {request.targetIssue})
                != 0)
            throw new IllegalStateException("R36_TARGET_ALREADY_DRAWN_NO_POST_DRAW_COMMIT");
        if (!request.baseIssue.equals(result.context.historyMaxIssue)
                || !snapshot.historyHash.equals(result.context.historyHash))
            throw new IllegalStateException("R36_HISTORY_FREEZE_MISMATCH");
    }

    private static void insertIssueCoreR36(
            SQLiteDatabase db, V51RuntimeResult r, DeferredEvidenceSnapshotR36.Header s) {
        ContentValues cv = new ContentValues();
        cv.put("target_issue", s.targetIssue);
        cv.put("base_issue", s.baseIssue);
        cv.put("history_hash", s.historyHash);
        cv.put("feature_hash", r.features.checksum);
        cv.put("state_hash", stateHash(r.states));
        cv.put("expert_hash", expertHash(r.numberModel.experts));
        cv.put("artifact_bundle_hash", V51ArtifactBundleManifest.BUNDLE_HASH);
        cv.put("cohort_id", V51BuildManifest.COHORT_ID);
        cv.put("status", r.status);
        cv.put("created_at", r.context.createdAt);
        cv.put("game_code", GAME_CODE);
        cv.put("bundle_hash", s.bundleHash);
        cv.put("contract_hash", s.contractHash);
        cv.put("record_hash", s.recordHash);
        insertOrVerifyHashR36(
                db, ISSUE, "target_issue", s.targetIssue, "record_hash", s.recordHash, cv);
    }

    private static void insertChampionCoreR36(
            SQLiteDatabase db, V51RuntimeResult r, DeferredEvidenceSnapshotR36.Header s) {
        V51Models.PositionAction a = r.selected.action;
        ContentValues cv = new ContentValues();
        cv.put("target_issue", s.targetIssue);
        cv.put("position", a.position);
        cv.put("n1", a.n1);
        cv.put("n2", a.n2);
        cv.put("n3", a.n3);
        cv.put("rank_score", a.rankScore);
        cv.put("action_hash", a.checksum);
        cv.put("ranker_artifact", V51ActionRankerArtifact.ARTIFACT_ID);
        cv.put("artifact_proven", V51ActionRankerArtifact.PROVEN ? 1 : 0);
        cv.put("frozen", 1);
        cv.put("game_code", GAME_CODE);
        cv.put("bundle_hash", s.bundleHash);
        String hash = V51ChecksumEngine.sha256(s.recordHash + "|CHAMPION|" + a.checksum);
        cv.put("record_hash", hash);
        insertOrVerifyHashR36(db, CHAMPION, "target_issue", s.targetIssue, "record_hash", hash, cv);
    }

    private static void insertSelectedCoreR36(
            SQLiteDatabase db, V51RuntimeResult r, DeferredEvidenceSnapshotR36.Header s) {
        V51Models.PositionAction a = r.selected.action;
        ContentValues cv = new ContentValues();
        cv.put("target_issue", s.targetIssue);
        cv.put("decision_id", r.authorized.decisionId);
        cv.put("position", a.position);
        cv.put("n1", a.n1);
        cv.put("n2", a.n2);
        cv.put("n3", a.n3);
        cv.put("selected_hit_p", finiteR36(r.selected.selectedHitP));
        cv.put("domain_id", r.selected.domain == null ? "" : r.selected.domain.domainId);
        cv.put("rank_no", a.rank);
        cv.put("predictability", r.selected.predictability.probability);
        // R900 visible fixed-strategy evidence is frozen synchronously with the canonical core.
        double rawAction = a.rawActionP;
        if (!(rawAction > 0.0d) && r.numberModel != null && r.numberModel.matrix != null && r.numberModel.matrix.probability != null
                && a.position >= 1 && a.position <= r.numberModel.matrix.probability.length) {
            double[] row = r.numberModel.matrix.probability[a.position - 1];
            if (row != null && row.length >= 10)
                rawAction = V51Math.clamp(row[a.n1 - 1] + row[a.n2 - 1] + row[a.n3 - 1], 0.0d, 1.0d);
        }
        double stability = a.stability;
        if (!(stability > 0.0d) && r.selected.predictability != null)
            stability = V51Math.clamp(r.selected.predictability.stability, 0.0d, 1.0d);
        double assignment = a.metaFeatures != null && a.metaFeatures.length > 15
                ? V51Math.clamp(a.metaFeatures[15], 0.0d, 1.0d) : -1.0d;
        double window = a.metaFeatures != null && a.metaFeatures.length > 20
                ? V51Math.clamp(a.metaFeatures[20], 0.0d, 1.0d) : -1.0d;
        cv.put("raw_action_p", rawAction);
        cv.put("rank_score", a.rankScore);
        cv.put("third_margin", a.thirdMargin);
        cv.put("entropy", a.entropy);
        cv.put("stability", stability);
        cv.put("assignment_support", assignment);
        cv.put("window_consensus", window);
        V51Models.StateTrajectorySnapshot shape =
                r.states != null && a.position >= 1 && a.position <= r.states.length
                        ? r.states[a.position - 1] : null;
        cv.put("shape_lifecycle", shape == null || shape.lifecycle == null
                ? "UNKNOWN" : shape.lifecycle.name());
        cv.put("shape_prototype", shape == null ? "" : safe(shape.prototypeId));
        cv.put("shape_run_length", shape == null ? 0 : shape.runLength);
        cv.put("shape_change_risk", shape == null ? -1.0d : shape.changePointRisk);
        cv.put("shape_confidence", shape == null ? -1.0d : shape.confidence);
        cv.put("lane", r.selected.lane.name());
        cv.put("decision_action", r.authorized.action.name());
        cv.put("reject_code", r.authorized.rejectCode.name());
        cv.put("contract_hash", s.contractHash);
        cv.put("authorization_hash", r.authorized.authorizationHash);
        cv.put("game_code", GAME_CODE);
        cv.put("bundle_hash", s.bundleHash);
        cv.put("record_hash", s.recordHash);
        insertOrVerifyHashR36(
                db, SELECTED, "target_issue", s.targetIssue, "record_hash", s.recordHash, cv);
    }

    private static void insertAuthorizationCoreR36(
            SQLiteDatabase db,
            V51RuntimeResult r,
            DeferredEvidenceSnapshotR36.Header s,
            long createdAt) {
        ContentValues cv = new ContentValues();
        cv.put("decision_id", r.authorized.decisionId);
        cv.put("target_issue", s.targetIssue);
        cv.put("contract_hash", s.contractHash);
        cv.put("artifact_bundle_hash", V51ArtifactBundleManifest.BUNDLE_HASH);
        cv.put("cohort_id", V51BuildManifest.COHORT_ID);
        cv.put("probability_owner", "V51PermutationNumberModelEngine");
        cv.put("rank_owner", "V51ActionRankerEngine");
        cv.put("lane_owner", "V51SelectiveRiskController");
        cv.put("authorization_hash", r.authorized.authorizationHash);
        cv.put("created_at", createdAt);
        cv.put("game_code", GAME_CODE);
        cv.put("bundle_hash", s.bundleHash);
        cv.put("record_hash", s.recordHash);
        insertOrVerifyHashR36(
                db, AUTH, "decision_id", r.authorized.decisionId, "record_hash", s.recordHash, cv);
    }

    private static void insertDeferredJobR36(
            SQLiteDatabase db,
            DeferredEvidenceSnapshotR36.Header s,
            byte[] bytes,
            String snapshotHash,
            long now) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT status,snapshot_hash FROM "
                                    + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                    + " WHERE target_issue=? LIMIT 1",
                            new String[] {s.targetIssue});
            if (c.moveToFirst()) {
                String status = safe(c.getString(0)), oldHash = safe(c.getString(1));
                if (oldHash.length() > 0 && !snapshotHash.equals(oldHash))
                    throw new IllegalStateException("R36_DEFERRED_SNAPSHOT_CONFLICT");
                if ("COMPLETED".equals(status)) return;
                ContentValues update = new ContentValues();
                update.put("status", "PENDING");
                update.put("snapshot_blob", bytes);
                update.put("snapshot_hash", snapshotHash);
                update.put("snapshot_schema", AppConfig.R36_SNAPSHOT_SCHEMA);
                update.put("bundle_hash", s.bundleHash);
                update.put("updated_at", now);
                update.put("lease_owner", "");
                update.put("lease_until", 0L);
                update.put("next_attempt_at", 0L);
                update.put("last_error", "");
                update.put("last_stage", "SNAPSHOT_DURABLE");
                int n =
                        db.update(
                                DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,
                                update,
                                "target_issue=?",
                                new String[] {s.targetIssue});
                if (n != 1) throw new IllegalStateException("R36_DEFERRED_UPDATE_CARDINALITY_" + n);
                return;
            }
        } finally {
            if (c != null) c.close();
        }
        ContentValues cv = new ContentValues();
        cv.put("target_issue", s.targetIssue);
        cv.put("job_type", "V51_FULL_EVIDENCE_R36");
        cv.put("status", "PENDING");
        cv.put("attempts", 0);
        cv.put("bundle_hash", s.bundleHash);
        cv.put("created_at", now);
        cv.put("updated_at", now);
        cv.put("last_error", "");
        cv.put(
                "record_hash",
                V51ChecksumEngine.sha256("R36_DEFERRED|" + s.targetIssue + '|' + snapshotHash));
        cv.put("snapshot_blob", bytes);
        cv.put("snapshot_hash", snapshotHash);
        cv.put("snapshot_schema", AppConfig.R36_SNAPSHOT_SCHEMA);
        cv.put("lease_owner", "");
        cv.put("lease_until", 0L);
        cv.put("next_attempt_at", 0L);
        cv.put("completed_at", 0L);
        cv.put("number_rows", 0);
        cv.put("action_rows", 0);
        cv.put("last_stage", "SNAPSHOT_DURABLE");
        cv.put("core_materialized", 0);
        cv.put("next_action_ordinal", 0);
        cv.put("cursor_version", 1);
        requireInsert(db, DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB, cv);
    }

    private static void insertOrVerifyHashR36(
            SQLiteDatabase db,
            String table,
            String keyColumn,
            String key,
            String hashColumn,
            String expected,
            ContentValues values) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT "
                                    + hashColumn
                                    + " FROM "
                                    + table
                                    + " WHERE "
                                    + keyColumn
                                    + "=? LIMIT 1",
                            new String[] {key});
            if (c.moveToFirst()) {
                if (!expected.equals(safe(c.getString(0))))
                    throw new IllegalStateException("R36_CORE_HASH_CONFLICT_" + table);
                return;
            }
        } finally {
            if (c != null) c.close();
        }
        requireInsert(db, table, values);
    }

    private static double finiteR36(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? -1.0d : value;
    }

    public static final class CanonicalCommitResult {
        public final boolean success, replay;
        public final String targetIssue, canonicalHash, snapshotHash, bundleHash, status;
        public final long commitAt, requestToCommitMs;

        CanonicalCommitResult(
                boolean success,
                boolean replay,
                String target,
                String canonical,
                String snapshot,
                String bundle,
                long commit,
                long elapsed,
                String status) {
            this.success = success;
            this.replay = replay;
            targetIssue = safe(target);
            canonicalHash = safe(canonical);
            snapshotHash = safe(snapshot);
            bundleHash = safe(bundle);
            commitAt = commit;
            requestToCommitMs = Math.max(0L, elapsed);
            this.status = safe(status);
        }
    }

    public static PersistReport persistWithReport(DbStore store, final V51RuntimeResult result) {
        if (store == null || result == null || !result.success || result.context == null)
            return fail("", "INVALID_INPUT");
        final String target = safe(result.context.targetIssue);
        if (target.length() == 0) return fail(target, "TARGET_EMPTY");
        final BundleIdentity id = identity(result);
        final String key =
                V51ChecksumEngine.sha256(
                        "B03_PREDICT|" + GAME_CODE + '|' + target + '|' + id.bundleHash);
        DatabaseCoordinator.TransactionContext tx =
                new DatabaseCoordinator.TransactionContext("B03_PREDICTION", target, key);
        DatabaseCoordinator.TransactionResult<PersistReport> outcome =
                DatabaseCoordinator.runExistingTransaction(
                        store.writable(),
                        tx,
                        new DatabaseCoordinator.TransactionCallable<PersistReport>() {
                            public PersistReport call(
                                    SQLiteDatabase db,
                                    DatabaseCoordinator.TransactionContext context) {
                                ensure(db);
                                String existing =
                                        scalar(
                                                db,
                                                "SELECT bundle_hash FROM "
                                                        + DbHelper.TABLE_B03_PREDICTION_BUNDLE
                                                        + " WHERE game_code=? AND target_issue=?",
                                                new String[] {GAME_CODE, target});
                                if (existing.length() > 0) {
                                    if (!existing.equals(id.bundleHash)) {
                                        return new PersistReport(
                                                true,
                                                true,
                                                target,
                                                id.bundleId,
                                                existing,
                                                id.recordHash,
                                                100,
                                                result.actions.length,
                                                result.selected == null ? 0 : 1,
                                                "IMMUTABLE_FIRST_WRITE_PRESERVED_HASH_CONFLICT");
                                    }
                                    reconcileOrThrow(db, target, result.selected == null ? 0 : 1);
                                    return new PersistReport(
                                            true,
                                            true,
                                            target,
                                            id.bundleId,
                                            id.bundleHash,
                                            id.recordHash,
                                            100,
                                            result.actions.length,
                                            result.selected == null ? 0 : 1,
                                            "REPLAY_IDEMPOTENT");
                                }
                                insertAll(db, result, id);
                                insertB03(db, result, id);
                                reconcileOrThrow(db, target, result.selected == null ? 0 : 1);
                                reconcileB03OrThrow(
                                        db, id.bundleId, result.selected == null ? 0 : 1);
                                return new PersistReport(
                                        true,
                                        false,
                                        target,
                                        id.bundleId,
                                        id.bundleHash,
                                        id.recordHash,
                                        100,
                                        result.actions.length,
                                        result.selected == null ? 0 : 1,
                                        "COMMITTED");
                            }
                        },
                        new PersistReport(
                                true,
                                true,
                                target,
                                id.bundleId,
                                id.bundleHash,
                                id.recordHash,
                                100,
                                result.actions.length,
                                result.selected == null ? 0 : 1,
                                "AUDIT_REPLAY"));
        return outcome.success && outcome.value != null
                ? outcome.value
                : fail(target, outcome.code);
    }

    private static void insertAll(SQLiteDatabase db, V51RuntimeResult r, BundleIdentity id) {
        ContentValues issue = new ContentValues();
        putCommon(issue, r.context.targetIssue, id);
        issue.put("base_issue", r.context.baseIssue);
        issue.put("history_hash", r.context.historyHash);
        issue.put("feature_hash", r.features.checksum);
        issue.put("state_hash", stateHash(r.states));
        issue.put("expert_hash", expertHash(r.numberModel.experts));
        issue.put("artifact_bundle_hash", V51ArtifactBundleManifest.BUNDLE_HASH);
        issue.put("cohort_id", V51BuildManifest.COHORT_ID);
        issue.put("status", r.status);
        issue.put("created_at", r.context.createdAt);
        requireInsert(db, ISSUE, issue);
        for (int p = 0; p < 10; p++) {
            int[] rank = rankRow(r.numberModel.matrix.probability[p]);
            for (int n = 0; n < 10; n++) {
                ContentValues v = new ContentValues();
                putCommon(v, r.context.targetIssue, id);
                v.put("position", p + 1);
                v.put("number", n + 1);
                v.put("raw_score", r.numberModel.rawScore[p][n]);
                v.put("number_p", r.numberModel.matrix.probability[p][n]);
                v.put("number_rank", rank[n]);
                v.put("state_id", r.states[p].prototypeId);
                v.put("domain_id", r.states[p].domainId);
                v.put("expert_hash", expertHash(r.numberModel.experts));
                requireInsert(db, NUMBER, v);
            }
            V51Models.PositionAction a = bestForPosition(r.actions, p + 1);
            ContentValues av = new ContentValues();
            putCommon(av, r.context.targetIssue, id);
            av.put("position", a.position);
            av.put("n1", a.n1);
            av.put("n2", a.n2);
            av.put("n3", a.n3);
            av.put("n4", a.n4);
            av.put("raw_action_p", a.rawActionP);
            av.put("rank_score", a.rankScore);
            av.put(
                    "selected_hit_p",
                    a.rank == 1
                                    && r.selected != null
                                    && !Double.isNaN(r.selected.selectedHitP)
                                    && !Double.isInfinite(r.selected.selectedHitP)
                            ? r.selected.selectedHitP
                            : -1.0d);
            av.put("rank_no", a.rank);
            av.put("fingerprint", a.fingerprint);
            av.put("meta_blob", doubles(a.metaFeatures));
            av.put("action_hash", a.checksum);
            requireInsert(db, ACTION, av);
        }
        insertFormalActionSpace(db, r, id);
        insertB10ProofLedger(db, r, id);
        ContentValues pv = new ContentValues();
        putCommon(pv, r.context.targetIssue, id);
        pv.put("matrix_blob", matrix(r.numberModel.matrix.probability));
        pv.put("row_error", r.numberModel.matrix.maxRowError);
        pv.put("column_error", r.numberModel.matrix.maxColumnError);
        pv.put("iterations", r.numberModel.matrix.iterations);
        pv.put("converged", r.numberModel.matrix.converged ? 1 : 0);
        pv.put("used_fallback", r.numberModel.matrix.usedFallback ? 1 : 0);
        pv.put("map_blob", ints(r.numberModel.matrix.mapPermutation));
        pv.put("matrix_hash", r.numberModel.matrix.checksum);
        requireInsert(db, PERMUTATION, pv);
        if (r.selected != null && r.authorized != null) {
            String action = r.authorized.action.name();
            ContentValues sv = new ContentValues();
            putCommon(sv, r.context.targetIssue, id);
            sv.put("decision_id", r.authorized.decisionId);
            sv.put("position", r.selected.action.position);
            sv.put("n1", r.selected.action.n1);
            sv.put("n2", r.selected.action.n2);
            sv.put("n3", r.selected.action.n3);
            sv.put(
                    "selected_hit_p",
                    (Double.isNaN(r.selected.selectedHitP)
                                    || Double.isInfinite(r.selected.selectedHitP))
                            ? -1.0d
                            : r.selected.selectedHitP);
            sv.put("domain_id", r.selected.domain == null ? "" : r.selected.domain.domainId);
            sv.put("rank_no", r.selected.action.rank);
            sv.put("predictability", r.selected.predictability.probability);
            sv.put("raw_action_p", r.selected.action.rawActionP);
            sv.put("rank_score", r.selected.action.rankScore);
            sv.put("third_margin", r.selected.action.thirdMargin);
            sv.put("entropy", r.selected.action.entropy);
            sv.put("stability", r.selected.action.stability);
            sv.put("assignment_support", r.selected.action.metaFeatures != null && r.selected.action.metaFeatures.length > 15 ? r.selected.action.metaFeatures[15] : -1.0d);
            sv.put("window_consensus", r.selected.action.metaFeatures != null && r.selected.action.metaFeatures.length > 20 ? r.selected.action.metaFeatures[20] : -1.0d);
            sv.put("lane", r.selected.lane.name());
            sv.put("decision_action", action);
            sv.put("reject_code", r.authorized.rejectCode.name());
            sv.put("authorization_hash", r.authorized.authorizationHash);
            requireInsert(db, SELECTED, sv);
            ContentValues au = new ContentValues();
            putCommon(au, r.context.targetIssue, id);
            au.put("decision_id", r.authorized.decisionId);
            au.put("artifact_bundle_hash", V51ArtifactBundleManifest.BUNDLE_HASH);
            au.put("cohort_id", V51BuildManifest.COHORT_ID);
            au.put("probability_owner", "V51PermutationNumberModelEngine");
            au.put("rank_owner", "V51ActionRankerEngine");
            au.put("lane_owner", "V51SelectiveRiskController");
            au.put("authorization_hash", r.authorized.authorizationHash);
            au.put("created_at", System.currentTimeMillis());
            requireInsert(db, AUTH, au);
        }
    }

    private static V51Models.PositionAction bestForPosition(
            V51Models.PositionAction[] actions, int position) {
        V51Models.PositionAction best = null;
        for (int i = 0; i < actions.length; i++) {
            V51Models.PositionAction a = actions[i];
            if (a.position != position) continue;
            if (best == null
                    || a.rank < best.rank
                    || (a.rank == best.rank && a.rankScore > best.rankScore)) best = a;
        }
        if (best == null)
            throw new IllegalStateException("B10_POSITION_SUMMARY_MISSING_" + position);
        return best;
    }

    private static void insertFormalActionSpace(
            SQLiteDatabase db, V51RuntimeResult r, BundleIdentity id) {
        K3ActionSpaceEngine.validateOrThrow(r.actions);
        for (int i = 0; i < r.actions.length; i++) {
            V51Models.PositionAction a = r.actions[i];
            ContentValues v = new ContentValues();
            putCommon(v, r.context.targetIssue, id);
            v.put("position", a.position);
            v.put("n1", a.n1);
            v.put("n2", a.n2);
            v.put("n3", a.n3);
            v.put("n4", a.n4);
            v.put("raw_action_p", a.rawActionP);
            v.put("rank_score", a.rankScore);
            v.put(
                    "selected_hit_p",
                    a.rank == 1
                                    && r.selected != null
                                    && !Double.isNaN(r.selected.selectedHitP)
                                    && !Double.isInfinite(r.selected.selectedHitP)
                            ? r.selected.selectedHitP
                            : -1.0d);
            v.put("rank_no", a.rank);
            v.put("fingerprint", a.fingerprint);
            v.put("meta_blob", doubles(a.metaFeatures));
            v.put("action_hash", a.checksum);
            v.put(
                    "record_hash",
                    V51ChecksumEngine.sha256(
                            id.recordHash
                                    + "|A1200|"
                                    + a.position
                                    + '|'
                                    + a.n1
                                    + '|'
                                    + a.n2
                                    + '|'
                                    + a.n3));
            requireInsert(db, ACTION_SPACE, v);
        }
    }

    private static void insertB10ProofLedger(
            SQLiteDatabase db, V51RuntimeResult r, BundleIdentity id) {
        if (r == null || r.actions == null || r.numberModel == null || r.numberModel.matrix == null)
            throw new IllegalStateException("B12_PROOF_LEDGER_INPUT_INVALID");
        V51Models.PositionAction champion = null;
        int championCount = 0;
        for (int i = 0; i < r.actions.length; i++) {
            V51Models.PositionAction a = r.actions[i];
            if (a != null && a.rank == 1) {
                champion = a;
                championCount++;
            }
        }
        if (championCount != 1 || champion == null)
            throw new IllegalStateException("B12_CHAMPION_CARDINALITY_" + championCount);
        ContentValues cv = new ContentValues();
        cv.put("target_issue", r.context.targetIssue);
        cv.put("position", champion.position);
        cv.put("n1", champion.n1);
        cv.put("n2", champion.n2);
        cv.put("n3", champion.n3);
        cv.put("rank_score", champion.rankScore);
        cv.put("action_hash", champion.checksum);
        cv.put("ranker_artifact", V51ActionRankerArtifact.ARTIFACT_ID);
        cv.put("artifact_proven", V51ActionRankerArtifact.PROVEN ? 1 : 0);
        cv.put("frozen", 1);
        cv.put("game_code", GAME_CODE);
        cv.put("bundle_hash", id.bundleHash);
        cv.put(
                "record_hash",
                V51ChecksumEngine.sha256(id.recordHash + "|CHAMPION|" + champion.checksum));
        requireInsert(db, CHAMPION, cv);
        K3FunnelOrchestrator.Audit funnel = r.funnel;
        if (funnel == null) {
            funnel = new K3FunnelOrchestrator().evaluate(r.numberModel.matrix, r.actions);
        }
        if (!funnel.neverDeleteFormalAction
                || !funnel.allPathsNested
                || funnel.formalActionCount != K3ActionSpaceEngine.TOTAL_ACTIONS) {
            throw new IllegalStateException("B12_FUNNEL_AUDIT_INVALID|" + funnel.summary());
        }
        for (int p0 = 0; p0 < 10; p0++) {
            String trace = funnel.traceForPosition(p0 + 1);
            String final3 = funnel.finalThreeForPosition(p0 + 1);
            ContentValues fv = new ContentValues();
            fv.put("target_issue", r.context.targetIssue);
            fv.put("position", p0 + 1);
            fv.put("trace_blob", trace);
            fv.put("final3", final3);
            fv.put("matrix_hash", r.numberModel.matrix.checksum);
            fv.put("game_code", GAME_CODE);
            fv.put("bundle_hash", id.bundleHash);
            fv.put(
                    "record_hash",
                    V51ChecksumEngine.sha256(id.recordHash + "|FUNNEL|" + (p0 + 1) + '|' + trace));
            requireInsert(db, FUNNEL, fv);
        }
    }

    private static void backfillB10ProofLedger(SQLiteDatabase db, String target) {
        if (db == null || safe(target).length() == 0) return;
        if (count(db, CHAMPION, target) == 0) {
            Cursor c = null;
            try {
                c =
                        db.rawQuery(
                                "SELECT position,n1,n2,n3,rank_score,action_hash FROM "
                                        + ACTION_SPACE
                                        + " WHERE target_issue=? AND rank_no=1 ORDER BY"
                                        + " position,n1,n2,n3 LIMIT 2",
                                new String[] {target});
                int rows = 0, position = 0, n1 = 0, n2 = 0, n3 = 0;
                double score = 0;
                String hash = "";
                while (c.moveToNext()) {
                    rows++;
                    if (rows == 1) {
                        position = c.getInt(0);
                        n1 = c.getInt(1);
                        n2 = c.getInt(2);
                        n3 = c.getInt(3);
                        score = c.getDouble(4);
                        hash = safe(c.getString(5));
                    }
                }
                if (rows == 1) {
                    String bundle =
                            scalar(
                                    db,
                                    "SELECT bundle_hash FROM " + ISSUE + " WHERE target_issue=?",
                                    new String[] {target});
                    ContentValues v = new ContentValues();
                    v.put("target_issue", target);
                    v.put("position", position);
                    v.put("n1", n1);
                    v.put("n2", n2);
                    v.put("n3", n3);
                    v.put("rank_score", score);
                    v.put("action_hash", hash);
                    v.put("ranker_artifact", V51ActionRankerArtifact.ARTIFACT_ID);
                    v.put("artifact_proven", V51ActionRankerArtifact.PROVEN ? 1 : 0);
                    v.put("frozen", 1);
                    v.put("game_code", GAME_CODE);
                    v.put("bundle_hash", bundle);
                    v.put(
                            "record_hash",
                            V51ChecksumEngine.sha256(target + "|BACKFILL_CHAMPION|" + hash));
                    requireInsert(db, CHAMPION, v);
                } else if (rows > 1) throw new IllegalStateException("B12_BACKFILL_MULTI_CHAMPION");
            } finally {
                if (c != null) c.close();
            }
        }
        String matrixHash =
                scalar(
                        db,
                        "SELECT matrix_hash FROM " + PERMUTATION + " WHERE target_issue=?",
                        new String[] {target});
        String bundle =
                scalar(
                        db,
                        "SELECT bundle_hash FROM " + ISSUE + " WHERE target_issue=?",
                        new String[] {target});
        for (int pos = 1; pos <= 10; pos++) {
            if (intScalar(
                            db,
                            "SELECT COUNT(1) FROM "
                                    + FUNNEL
                                    + " WHERE target_issue=? AND position=?",
                            new String[] {target, String.valueOf(pos)})
                    > 0) continue;
            Cursor c = null;
            try {
                c =
                        db.rawQuery(
                                "SELECT number,number_p FROM "
                                        + NUMBER
                                        + " WHERE target_issue=? AND position=? ORDER BY number_p"
                                        + " DESC,number ASC",
                                new String[] {target, String.valueOf(pos)});
                int[] order = new int[10];
                int k = 0;
                while (c.moveToNext() && k < 10) order[k++] = c.getInt(0);
                if (k == 10) {
                    String trace = b12FunnelTrace(order);
                    ContentValues v = new ContentValues();
                    v.put("target_issue", target);
                    v.put("position", pos);
                    v.put("trace_blob", trace);
                    v.put("final3", b12CsvPrefix(order, 3));
                    v.put("matrix_hash", matrixHash);
                    v.put("game_code", GAME_CODE);
                    v.put("bundle_hash", bundle);
                    v.put(
                            "record_hash",
                            V51ChecksumEngine.sha256(
                                    target + "|BACKFILL_FUNNEL|" + pos + '|' + trace));
                    requireInsert(db, FUNNEL, v);
                }
            } finally {
                if (c != null) c.close();
            }
        }
    }

    private static int[] b12OrderNumbers(double[] q) {
        if (q == null || q.length != 10) throw new IllegalArgumentException("B12_FUNNEL_Q_INVALID");
        int[] x = new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int i = 0; i < x.length; i++)
            for (int j = i + 1; j < x.length; j++) {
                int a = x[i] - 1, b = x[j] - 1;
                if (q[b] > q[a] || (q[b] == q[a] && x[j] < x[i])) {
                    int t = x[i];
                    x[i] = x[j];
                    x[j] = t;
                }
            }
        return x;
    }

    private static String b12FunnelTrace(int[] order) {
        if (order == null || order.length != 10) return "";
        int[] size = new int[] {9, 8, 7, 6, 5, 4, 3};
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < size.length; i++) {
            if (i > 0) b.append('|');
            b.append(size[i]).append(':').append(b12CsvPrefix(order, size[i]));
        }
        return b.toString();
    }

    private static String b12CsvPrefix(int[] order, int n) {
        StringBuilder b = new StringBuilder();
        int lim = Math.min(n, order == null ? 0 : order.length);
        for (int i = 0; i < lim; i++) {
            if (i > 0) b.append(',');
            b.append(order[i]);
        }
        return b.toString();
    }

    private static void settleFormalActionSpace(SQLiteDatabase db, String target, int[] actual) {
        if (actual == null || actual.length != 10) return;
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT position,n1,n2,n3 FROM "
                                    + ACTION_SPACE
                                    + " WHERE game_code=? AND target_issue=?",
                            new String[] {GAME_CODE, target});
            while (c.moveToNext()) {
                int p = c.getInt(0),
                        a = c.getInt(1),
                        b = c.getInt(2),
                        d = c.getInt(3),
                        y = actual[p - 1];
                ContentValues v = new ContentValues();
                v.put("hit_label", (y == a || y == b || y == d) ? 1 : 0);
                db.update(
                        ACTION_SPACE,
                        v,
                        "game_code=? AND target_issue=? AND position=? AND n1=? AND n2=? AND n3=?",
                        new String[] {
                            GAME_CODE,
                            target,
                            String.valueOf(p),
                            String.valueOf(a),
                            String.valueOf(b),
                            String.valueOf(d)
                        });
            }
        } finally {
            if (c != null) c.close();
        }
    }

    private static void insertB03(SQLiteDatabase db, V51RuntimeResult r, BundleIdentity id) {
        ContentValues b = new ContentValues();
        b.put("bundle_id", id.bundleId);
        b.put("game_code", GAME_CODE);
        b.put("target_issue", r.context.targetIssue);
        b.put("base_issue", r.context.baseIssue);
        b.put("history_hash", r.context.historyHash);
        b.put("feature_hash", r.features.checksum);
        b.put("state_hash", stateHash(r.states));
        b.put("expert_hash", expertHash(r.numberModel.experts));
        b.put("bundle_hash", id.bundleHash);
        b.put("contract_hash", id.contractHash);
        b.put("artifact_bundle_hash", V51ArtifactBundleManifest.BUNDLE_HASH);
        b.put("cohort_id", V51BuildManifest.COHORT_ID);
        b.put("status", r.status);
        b.put("record_hash", id.recordHash);
        b.put("created_at", r.context.createdAt);
        requireInsert(db, DbHelper.TABLE_B03_PREDICTION_BUNDLE, b);
        for (int p = 0; p < 10; p++) {
            int[] rank = rankRow(r.numberModel.matrix.probability[p]);
            for (int n = 0; n < 10; n++) {
                ContentValues v = new ContentValues();
                v.put("bundle_id", id.bundleId);
                v.put("game_code", GAME_CODE);
                v.put("target_issue", r.context.targetIssue);
                v.put("position", p + 1);
                v.put("number", n + 1);
                v.put("raw_score", r.numberModel.rawScore[p][n]);
                v.put("number_p", r.numberModel.matrix.probability[p][n]);
                v.put("number_rank", rank[n]);
                v.put("state_id", r.states[p].prototypeId);
                v.put("domain_id", r.states[p].domainId);
                v.put("contract_hash", id.contractHash);
                v.put("bundle_hash", id.bundleHash);
                v.put("record_hash", V51ChecksumEngine.sha256(id.recordHash + "|N|" + p + '|' + n));
                v.put("created_at", r.context.createdAt);
                requireInsert(db, DbHelper.TABLE_B03_NUMBER_EVENT, v);
            }
            V51Models.PositionAction a = bestForPosition(r.actions, p + 1);
            ContentValues av = new ContentValues();
            av.put("bundle_id", id.bundleId);
            av.put("game_code", GAME_CODE);
            av.put("target_issue", r.context.targetIssue);
            av.put("position", a.position);
            av.put("n1", a.n1);
            av.put("n2", a.n2);
            av.put("n3", a.n3);
            av.put("n4", a.n4);
            av.put("raw_action_p", a.rawActionP);
            av.put("rank_score", a.rankScore);
            av.put("rank_no", a.rank);
            av.put("action_hash", a.checksum);
            av.put("contract_hash", id.contractHash);
            av.put("bundle_hash", id.bundleHash);
            av.put("record_hash", V51ChecksumEngine.sha256(id.recordHash + "|A|" + p));
            av.put("created_at", r.context.createdAt);
            requireInsert(db, DbHelper.TABLE_B03_ACTION_EVENT, av);
        }
        ContentValues pv = new ContentValues();
        pv.put("bundle_id", id.bundleId);
        pv.put("game_code", GAME_CODE);
        pv.put("target_issue", r.context.targetIssue);
        pv.put("matrix_blob", matrix(r.numberModel.matrix.probability));
        pv.put("row_error", r.numberModel.matrix.maxRowError);
        pv.put("column_error", r.numberModel.matrix.maxColumnError);
        pv.put("iterations", r.numberModel.matrix.iterations);
        pv.put("converged", r.numberModel.matrix.converged ? 1 : 0);
        pv.put("used_fallback", r.numberModel.matrix.usedFallback ? 1 : 0);
        pv.put("map_blob", ints(r.numberModel.matrix.mapPermutation));
        pv.put("matrix_hash", r.numberModel.matrix.checksum);
        pv.put("contract_hash", id.contractHash);
        pv.put("bundle_hash", id.bundleHash);
        pv.put("record_hash", V51ChecksumEngine.sha256(id.recordHash + "|P"));
        pv.put("created_at", r.context.createdAt);
        requireInsert(db, DbHelper.TABLE_B03_PERMUTATION_EVENT, pv);
        if (r.selected != null && r.authorized != null) {
            ContentValues sv = new ContentValues();
            sv.put("bundle_id", id.bundleId);
            sv.put("game_code", GAME_CODE);
            sv.put("target_issue", r.context.targetIssue);
            sv.put("decision_id", r.authorized.decisionId);
            sv.put("position", r.selected.action.position);
            sv.put("n1", r.selected.action.n1);
            sv.put("n2", r.selected.action.n2);
            sv.put("n3", r.selected.action.n3);
            sv.put(
                    "selected_hit_p",
                    (Double.isNaN(r.selected.selectedHitP)
                                    || Double.isInfinite(r.selected.selectedHitP))
                            ? -1.0d
                            : r.selected.selectedHitP);
            sv.put("predictability", r.selected.predictability.probability);
            sv.put("lane", r.selected.lane.name());
            sv.put("decision_action", r.authorized.action.name());
            sv.put("reject_code", r.authorized.rejectCode.name());
            sv.put("contract_hash", id.contractHash);
            sv.put("bundle_hash", id.bundleHash);
            sv.put("authorization_hash", r.authorized.authorizationHash);
            sv.put("record_hash", V51ChecksumEngine.sha256(id.recordHash + "|S"));
            sv.put("created_at", r.context.createdAt);
            requireInsert(db, DbHelper.TABLE_B03_SELECTED_EVENT, sv);
            ContentValues au = new ContentValues();
            au.put("bundle_id", id.bundleId);
            au.put("game_code", GAME_CODE);
            au.put("target_issue", r.context.targetIssue);
            au.put("decision_id", r.authorized.decisionId);
            au.put("contract_hash", id.contractHash);
            au.put("bundle_hash", id.bundleHash);
            au.put("artifact_bundle_hash", V51ArtifactBundleManifest.BUNDLE_HASH);
            au.put("cohort_id", V51BuildManifest.COHORT_ID);
            au.put("authorization_hash", r.authorized.authorizationHash);
            au.put("record_hash", V51ChecksumEngine.sha256(id.recordHash + "|U"));
            au.put("created_at", System.currentTimeMillis());
            requireInsert(db, DbHelper.TABLE_B03_AUTHORIZATION_EVENT, au);
        }
    }

    public static SettlementReport settleShadow(
            DbStore store,
            final String targetIssue,
            final String actualPermutation,
            final String result,
            final long settledAt) {
        if (store == null || safe(targetIssue).length() == 0)
            return new SettlementReport(false, false, targetIssue, result, "", "", "INVALID_INPUT");
        final String normalized = safe(actualPermutation);
        final String settlementHash =
                V51ChecksumEngine.sha256(
                        GAME_CODE + '|' + targetIssue + '|' + normalized + '|' + safe(result));
        final String key = V51ChecksumEngine.sha256("B03_SETTLE|" + settlementHash);
        DatabaseCoordinator.TransactionContext tx =
                new DatabaseCoordinator.TransactionContext("B03_SETTLEMENT", targetIssue, key);
        DatabaseCoordinator.TransactionResult<SettlementReport> out =
                DatabaseCoordinator.runExistingTransaction(
                        store.writable(),
                        tx,
                        new DatabaseCoordinator.TransactionCallable<SettlementReport>() {
                            public SettlementReport call(
                                    SQLiteDatabase db,
                                    DatabaseCoordinator.TransactionContext context) {
                                ensure(db);
                                String bundle =
                                        scalar(
                                                db,
                                                "SELECT bundle_id FROM "
                                                        + DbHelper.TABLE_B03_PREDICTION_BUNDLE
                                                        + " WHERE game_code=? AND target_issue=?",
                                                new String[] {GAME_CODE, targetIssue});
                                if (bundle.length() == 0)
                                    throw new IllegalStateException("B03_BUNDLE_NOT_FOUND");
                                String existing =
                                        scalar(
                                                db,
                                                "SELECT settlement_hash FROM "
                                                        + DbHelper.TABLE_B03_SETTLEMENT_EVENT
                                                        + " WHERE game_code=? AND target_issue=?",
                                                new String[] {GAME_CODE, targetIssue});
                                if (existing.length() > 0) {
                                    if (!existing.equals(settlementHash))
                                        throw new IllegalStateException("B03_SETTLEMENT_CONFLICT");
                                    return new SettlementReport(
                                            true,
                                            true,
                                            targetIssue,
                                            result,
                                            settlementHash,
                                            V51ChecksumEngine.sha256(settlementHash + "|REPLAY"),
                                            "REPLAY");
                                }
                                String record =
                                        V51ChecksumEngine.sha256(
                                                bundle + '|' + settlementHash + '|' + settledAt);
                                ContentValues cv = new ContentValues();
                                cv.put("bundle_id", bundle);
                                cv.put("game_code", GAME_CODE);
                                cv.put("target_issue", targetIssue);
                                cv.put("actual_permutation", normalized);
                                cv.put("result", safe(result));
                                cv.put("settlement_hash", settlementHash);
                                cv.put("record_hash", record);
                                cv.put("created_at", settledAt);
                                requireInsert(db, DbHelper.TABLE_B03_SETTLEMENT_EVENT, cv);
                                int[] actualNums = parsePermutation(normalized);
                                if (actualNums.length == 10) {
                                    for (int pos = 1; pos <= 10; pos++) {
                                        for (int num = 1; num <= 10; num++) {
                                            ContentValues nv = new ContentValues();
                                            nv.put(
                                                    "actual_label",
                                                    actualNums[pos - 1] == num ? 1 : 0);
                                            db.update(
                                                    NUMBER,
                                                    nv,
                                                    "game_code=? AND target_issue=? AND position=?"
                                                            + " AND number=?",
                                                    new String[] {
                                                        GAME_CODE,
                                                        targetIssue,
                                                        String.valueOf(pos),
                                                        String.valueOf(num)
                                                    });
                                        }
                                        Cursor ac =
                                                db.rawQuery(
                                                        "SELECT n1,n2,n3 FROM "
                                                                + ACTION
                                                                + " WHERE game_code=? AND"
                                                                + " target_issue=? AND position=?",
                                                        new String[] {
                                                            GAME_CODE,
                                                            targetIssue,
                                                            String.valueOf(pos)
                                                        });
                                        try {
                                            if (ac.moveToFirst()) {
                                                int n1 = ac.getInt(0),
                                                        n2 = ac.getInt(1),
                                                        n3 = ac.getInt(2);
                                                ContentValues av = new ContentValues();
                                                av.put(
                                                        "hit_label",
                                                        (actualNums[pos - 1] == n1
                                                                        || actualNums[pos - 1] == n2
                                                                        || actualNums[pos - 1]
                                                                                == n3)
                                                                ? 1
                                                                : 0);
                                                db.update(
                                                        ACTION,
                                                        av,
                                                        "game_code=? AND target_issue=? AND"
                                                                + " position=?",
                                                        new String[] {
                                                            GAME_CODE,
                                                            targetIssue,
                                                            String.valueOf(pos)
                                                        });
                                            }
                                        } finally {
                                            ac.close();
                                        }
                                    }
                                }
                                settleFormalActionSpace(db, targetIssue, actualNums);
                                ContentValues update = new ContentValues();
                                update.put("settled", 1);
                                update.put("actual_permutation", normalized);
                                update.put("result", safe(result));
                                db.update(
                                        ISSUE,
                                        update,
                                        "game_code=? AND target_issue=?",
                                        new String[] {GAME_CODE, targetIssue});
                                ContentValues selected = new ContentValues();
                                selected.put("settled", 1);
                                selected.put("result", safe(result));
                                selected.put("settlement_hash", settlementHash);
                                db.update(
                                        SELECTED,
                                        selected,
                                        "game_code=? AND target_issue=?",
                                        new String[] {GAME_CODE, targetIssue});
                                return new SettlementReport(
                                        true,
                                        false,
                                        targetIssue,
                                        result,
                                        settlementHash,
                                        record,
                                        "COMMITTED");
                            }
                        },
                        new SettlementReport(
                                true,
                                true,
                                targetIssue,
                                result,
                                settlementHash,
                                V51ChecksumEngine.sha256(settlementHash + "|AUDIT_REPLAY"),
                                "AUDIT_REPLAY"));
        return out.value == null
                ? new SettlementReport(
                        false, false, targetIssue, result, settlementHash, "", out.code)
                : out.value;
    }

    public static SettlementReport settleByDraw(DbStore store, Models.Draw draw) {
        return SettlementCoordinatorR815.settleV51Compatibility(
                store, draw, SettlementCoordinatorR815.Reason.LEGACY_API);
    }

    public static SettlementReport applyV51SettlementFromOwner(
            DbStore store, Models.Draw draw, SettlementEngine.V51SettlementToken token) {
        if (!SettlementEngine.b12IsSettlementOwnerToken(token, draw == null ? "" : draw.issue))
            return b12SettlementFailure(
                    draw == null ? "" : draw.issue, "B12_SETTLEMENT_OWNER_REQUIRED");
        if (store == null || draw == null || !draw.isValid())
            return new SettlementReport(
                    false, false, draw == null ? "" : safe(draw.issue), "", "", "", "INVALID_DRAW");
        ensure(store);
        String target = safe(draw.issue);
        SQLiteDatabase db = store.writable();
        Cursor c =
                db.rawQuery(
                        "SELECT position,n1,n2,n3 FROM "
                                + SELECTED
                                + " WHERE game_code=? AND target_issue=?",
                        new String[] {GAME_CODE, target});
        try {
            if (!c.moveToFirst())
                return new SettlementReport(
                        true, true, target, "NO_SELECTED", "", "", "NO_SELECTED_FOR_DRAW");
            int pos = c.getInt(0), n1 = c.getInt(1), n2 = c.getInt(2), n3 = c.getInt(3);
            int actual = draw.getNumberAtPosition(pos);
            String result = (actual == n1 || actual == n2 || actual == n3) ? "HIT" : "MISS";
            SettlementReport report =
                    settleShadow(
                            store, target, draw.numbersText(), result, System.currentTimeMillis());
            try {
                V51TianshuEvidenceBridge.observeSettlement(
                        store, selectedCandidate(target, pos, n1, n2, n3), draw);
            } catch (Throwable ignore) {
            }
            return report;
        } finally {
            c.close();
        }
    }

    public static SettlementReport b12SettlementFailure(String target, String detail) {
        return new SettlementReport(false, false, target, "", "", "", detail);
    }

    private static Models.ComboCandidate selectedCandidate(
            String target, int pos, int n1, int n2, int n3) {
        Models.ComboCandidate c = new Models.ComboCandidate();
        c.targetIssue = safe(target);
        c.position = pos;
        c.n1 = n1;
        c.n2 = n2;
        c.n3 = n3;
        c.k3Mask = AppConfig.buildK3Mask(n1, n2, n3);
        return c;
    }

    private static int[] parsePermutation(String value) {
        String x = safe(value).replace(' ', ',');
        String[] p = x.split(",");
        ArrayList<Integer> nums = new ArrayList<Integer>();
        for (int i = 0; i < p.length; i++) {
            String q = p[i].trim();
            if (q.length() == 0) continue;
            try {
                int n = Integer.parseInt(q);
                if (n >= 1 && n <= 10) nums.add(Integer.valueOf(n));
            } catch (Throwable ignore) {
            }
        }
        if (nums.size() != 10) return new int[0];
        int[] out = new int[10];
        for (int i = 0; i < 10; i++) out[i] = nums.get(i).intValue();
        return out;
    }

    public static final class DiagnosticStats {
        public int predictions,
                selected,
                bet,
                shadow,
                hold,
                settled,
                hit,
                betSettled,
                betHit,
                shadowSettled,
                shadowHit;
        public double coverage, hitRate, betHitRate, shadowHitRate, ece, wilsonLower;

        public String summary() {
            return "predictions="
                    + predictions
                    + "|selected="
                    + selected
                    + "|BET="
                    + bet
                    + "|SHADOW="
                    + shadow
                    + "|HOLD="
                    + hold
                    + "|settled="
                    + settled
                    + "|hit="
                    + hit
                    + "|Hit="
                    + hitRate
                    + "|Coverage="
                    + coverage
                    + "|ECE="
                    + ece
                    + "|WilsonLower="
                    + wilsonLower
                    + "|betHit="
                    + betHitRate
                    + "|shadowHit="
                    + shadowHitRate;
        }
    }

    public static DiagnosticStats diagnosticStats(DbStore store) {
        DiagnosticStats x = new DiagnosticStats();
        if (store == null) return x;
        SQLiteDatabase db = store.readable();
        x.predictions =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM " + ISSUE + " WHERE game_code=?",
                        new String[] {GAME_CODE});
        x.selected =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM " + SELECTED + " WHERE game_code=?",
                        new String[] {GAME_CODE});
        x.bet =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='BET'",
                        new String[] {GAME_CODE});
        x.shadow =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='SHADOW'",
                        new String[] {GAME_CODE});
        x.hold =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='HOLD'",
                        new String[] {GAME_CODE});
        x.settled =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND settled=1 AND result IN ('HIT','MISS')",
                        new String[] {GAME_CODE});
        x.hit =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND settled=1 AND result='HIT'",
                        new String[] {GAME_CODE});
        x.betSettled =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='BET' AND settled=1 AND"
                                + " result IN ('HIT','MISS')",
                        new String[] {GAME_CODE});
        x.betHit =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='BET' AND settled=1 AND"
                                + " result='HIT'",
                        new String[] {GAME_CODE});
        x.shadowSettled =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='SHADOW' AND settled=1"
                                + " AND result IN ('HIT','MISS')",
                        new String[] {GAME_CODE});
        x.shadowHit =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='SHADOW' AND settled=1"
                                + " AND result='HIT'",
                        new String[] {GAME_CODE});
        x.coverage = x.predictions <= 0 ? 0 : (double) x.bet / x.predictions;
        x.hitRate = x.settled <= 0 ? 0 : (double) x.hit / x.settled;
        x.betHitRate = x.betSettled <= 0 ? 0 : (double) x.betHit / x.betSettled;
        x.shadowHitRate = x.shadowSettled <= 0 ? 0 : (double) x.shadowHit / x.shadowSettled;
        x.wilsonLower = wilson(x.hit, x.settled);
        x.ece = selectedEce(db);
        return x;
    }

    private static int intScalar(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static double selectedEce(SQLiteDatabase db) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT selected_hit_p,result FROM "
                                    + SELECTED
                                    + " WHERE game_code=? AND settled=1 AND selected_hit_p>=0 AND"
                                    + " selected_hit_p<=1 AND result IN ('HIT','MISS')",
                            new String[] {GAME_CODE});
            int bins = 10;
            int[] count = new int[bins];
            double[] sumP = new double[bins], sumY = new double[bins];
            int total = 0;
            while (c.moveToNext()) {
                double p = c.getDouble(0);
                if (Double.isNaN(p) || Double.isInfinite(p)) continue;
                if (p < 0) p = 0;
                if (p > 1) p = 1;
                int y = "HIT".equals(c.getString(1)) ? 1 : 0;
                int b = (int) Math.floor(p * bins);
                if (b >= bins) b = bins - 1;
                if (b < 0) b = 0;
                count[b]++;
                sumP[b] += p;
                sumY[b] += y;
                total++;
            }
            if (total <= 0) return 0;
            double ece = 0;
            for (int b = 0; b < bins; b++) {
                if (count[b] <= 0) continue;
                double avgP = sumP[b] / count[b], avgY = sumY[b] / count[b];
                ece += ((double) count[b] / total) * Math.abs(avgP - avgY);
            }
            return ece;
        } finally {
            if (c != null) c.close();
        }
    }

    private static double wilson(int hit, int n) {
        if (n <= 0) return 0;
        double z = 1.959963984540054d,
                p = (double) hit / n,
                z2 = z * z,
                den = 1 + z2 / n,
                center = p + z2 / (2 * n),
                adj = z * Math.sqrt((p * (1 - p) + z2 / (4 * n)) / n);
        return Math.max(0, (center - adj) / den);
    }

    public static void reconcileOrThrow(SQLiteDatabase db, String target, int selectedExpected) {
        backfillB10ProofLedger(db, target);
        int issue = count(db, ISSUE, target),
                number = count(db, NUMBER, target),
                action = count(db, ACTION, target),
                permutation = count(db, PERMUTATION, target),
                selected = count(db, SELECTED, target);
        int formal = count(db, ACTION_SPACE, target),
                champion = count(db, CHAMPION, target),
                funnel = count(db, FUNNEL, target);
        if (issue != 1
                || number != 100
                || action != 10
                || formal != 1200
                || champion != 1
                || funnel != 10
                || permutation != 1
                || selected != selectedExpected)
            throw new IllegalStateException(
                    "V51_LEDGER_RECONCILIATION_FAILED:"
                            + issue
                            + ','
                            + number
                            + ','
                            + action
                            + ','
                            + formal
                            + ','
                            + champion
                            + ','
                            + funnel
                            + ','
                            + permutation
                            + ','
                            + selected);
    }

    public static void reconcileB03OrThrow(
            SQLiteDatabase db, String bundleId, int selectedExpected) {
        int issue = countBundle(db, DbHelper.TABLE_B03_PREDICTION_BUNDLE, bundleId),
                number = countBundle(db, DbHelper.TABLE_B03_NUMBER_EVENT, bundleId),
                action = countBundle(db, DbHelper.TABLE_B03_ACTION_EVENT, bundleId),
                permutation = countBundle(db, DbHelper.TABLE_B03_PERMUTATION_EVENT, bundleId),
                selected = countBundle(db, DbHelper.TABLE_B03_SELECTED_EVENT, bundleId);
        if (issue != 1
                || number != 100
                || action != 10
                || permutation != 1
                || selected != selectedExpected)
            throw new IllegalStateException(
                    "B03_LEDGER_RECONCILIATION_FAILED:"
                            + issue
                            + ','
                            + number
                            + ','
                            + action
                            + ','
                            + permutation
                            + ','
                            + selected);
    }

    public static boolean hasSelected(SQLiteDatabase db, String target) {
        return count(db, SELECTED, target) > 0;
    }

    public static String replayHash(SQLiteDatabase db, String target) {
        StringBuilder b = new StringBuilder();
        String[] tables = {
            ISSUE, NUMBER, ACTION, ACTION_SPACE, CHAMPION, FUNNEL, PERMUTATION, SELECTED, AUTH
        };
        for (int i = 0; i < tables.length; i++) appendHashes(db, b, tables[i], target);
        return V51ChecksumEngine.sha256(b.toString());
    }

    private static final class BundleIdentity {
        final String bundleId, bundleHash, contractHash, recordHash;

        BundleIdentity(String id, String bundle, String contract, String record) {
            bundleId = id;
            bundleHash = bundle;
            contractHash = contract;
            recordHash = record;
        }
    }

    private static BundleIdentity identity(V51RuntimeResult r) {
        String contract =
                r.selected == null
                        ? V51ChecksumEngine.sha256("NO_SELECTED|" + r.context.targetIssue)
                        : r.selected.contractHash;
        String bundle =
                V51ChecksumEngine.sha256(
                        GAME_CODE
                                + '|'
                                + r.context.targetIssue
                                + '|'
                                + r.context.historyHash
                                + '|'
                                + r.features.checksum
                                + '|'
                                + stateHash(r.states)
                                + '|'
                                + expertHash(r.numberModel.experts)
                                + '|'
                                + contract
                                + '|'
                                + V51ArtifactBundleManifest.BUNDLE_HASH);
        String id = "B03-" + bundle.substring(0, 24);
        String record =
                V51ChecksumEngine.sha256(
                        id + '|' + bundle + '|' + contract + '|' + r.context.createdAt);
        return new BundleIdentity(id, bundle, contract, record);
    }

    private static void putCommon(ContentValues cv, String target, BundleIdentity id) {
        cv.put("game_code", GAME_CODE);
        cv.put("target_issue", target);
        cv.put("bundle_hash", id.bundleHash);
        cv.put("contract_hash", id.contractHash);
        cv.put("record_hash", id.recordHash);
    }

    private static void addColumn(SQLiteDatabase db, String table, String def) {
        String name = def.substring(0, def.indexOf(' '));
        if (columnExists(db, table, name)) return;
        try {
            db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + def);
        } catch (Throwable e) {
            if (!columnExists(db, table, name))
                throw new IllegalStateException("B03_ADD_COLUMN_FAILED:" + table + '.' + name, e);
        }
    }

    private static boolean columnExists(SQLiteDatabase db, String table, String name) {
        Cursor c = null;
        try {
            c = db.rawQuery("PRAGMA table_info(" + table + ")", null);
            int n = c.getColumnIndex("name");
            while (c.moveToNext()) if (n >= 0 && name.equals(c.getString(n))) return true;
            return false;
        } finally {
            if (c != null) c.close();
        }
    }

    private static int count(SQLiteDatabase db, String table, String target) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT COUNT(1) FROM " + table + " WHERE target_issue=?",
                            new String[] {target});
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static int countBundle(SQLiteDatabase db, String table, String bundle) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT COUNT(1) FROM " + table + " WHERE bundle_id=?",
                            new String[] {bundle});
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static String scalar(SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = db.rawQuery(sql, args);
            return c.moveToFirst() && !c.isNull(0) ? safe(c.getString(0)) : "";
        } finally {
            if (c != null) c.close();
        }
    }

    private static void requireInsert(SQLiteDatabase db, String table, ContentValues values) {
        long id = db.insertOrThrow(table, null, values);
        if (id < 0) throw new IllegalStateException("B03_INSERT_FAILED_" + table);
    }

    private static void appendHashes(
            SQLiteDatabase db, StringBuilder b, String table, String target) {
        Cursor c = null;
        try {
            String col = columnExists(db, table, "record_hash") ? "record_hash" : "target_issue";
            c =
                    db.rawQuery(
                            "SELECT "
                                    + col
                                    + " FROM "
                                    + table
                                    + " WHERE target_issue=? ORDER BY rowid",
                            new String[] {target});
            while (c.moveToNext()) b.append(c.isNull(0) ? "" : c.getString(0));
        } finally {
            if (c != null) c.close();
        }
    }

    private static int[] rankRow(double[] p) {
        int[] rank = new int[10];
        for (int n = 0; n < 10; n++) {
            int r = 1;
            for (int m = 0; m < 10; m++) if (p[m] > p[n] || (p[m] == p[n] && m < n)) r++;
            rank[n] = r;
        }
        return rank;
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

    private static String doubles(double[] x) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < x.length; i++) {
            if (i > 0) b.append(',');
            b.append(Double.toString(x[i]));
        }
        return b.toString();
    }

    private static String ints(int[] x) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < x.length; i++) {
            if (i > 0) b.append(',');
            b.append(x[i]);
        }
        return b.toString();
    }

    private static String matrix(double[][] x) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < x.length; i++) {
            if (i > 0) b.append(';');
            b.append(doubles(x[i]));
        }
        return b.toString();
    }

    private static PersistReport fail(String target, String detail) {
        return new PersistReport(false, false, target, "", "", "", 0, 0, 0, detail);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static List<StorageRule> storageRules() {
        ArrayList<StorageRule> rules = new ArrayList<StorageRule>();
        rules.add(new StorageRule("B03-REPO-001", "V51Repository", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-002", "DbStore", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-003", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-004", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-005", "V51Repository", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-006", "DbStore", "ATOMIC_BUNDLE", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-007", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-008", "DbHelper", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-009",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-010",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-011", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-012", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-013", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-014", "DbStore", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-015", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-016", "DbHelper", "ATOMIC_BUNDLE", true, 2));
        rules.add(new StorageRule("B03-REPO-017", "V51Repository", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-018", "DbStore", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-019",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-020",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(new StorageRule("B03-REPO-021", "V51Repository", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-022", "DbStore", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-023", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-024", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-025", "V51Repository", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-026", "DbStore", "ATOMIC_BUNDLE", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-027", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-028", "DbHelper", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-029",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-030",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-031", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-032", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-033", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-034", "DbStore", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-035", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-036", "DbHelper", "ATOMIC_BUNDLE", true, 1));
        rules.add(new StorageRule("B03-REPO-037", "V51Repository", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-038", "DbStore", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-039",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-040",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(new StorageRule("B03-REPO-041", "V51Repository", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-042", "DbStore", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-043", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-044", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-045", "V51Repository", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-046", "DbStore", "ATOMIC_BUNDLE", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-047", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-048", "DbHelper", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-049",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-050",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-051", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-052", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-053", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-054", "DbStore", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-055", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-056", "DbHelper", "ATOMIC_BUNDLE", true, 7));
        rules.add(new StorageRule("B03-REPO-057", "V51Repository", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-058", "DbStore", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-059",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-060",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(new StorageRule("B03-REPO-061", "V51Repository", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-062", "DbStore", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-063", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-064", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-065", "V51Repository", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-066", "DbStore", "ATOMIC_BUNDLE", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-067", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-068", "DbHelper", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-069",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-070",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-071", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-072", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-073", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-074", "DbStore", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-075", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-076", "DbHelper", "ATOMIC_BUNDLE", true, 6));
        rules.add(new StorageRule("B03-REPO-077", "V51Repository", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-078", "DbStore", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-079",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-080",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(new StorageRule("B03-REPO-081", "V51Repository", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-082", "DbStore", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-083", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-084", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-085", "V51Repository", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-086", "DbStore", "ATOMIC_BUNDLE", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-087", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-088", "DbHelper", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-089",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-090",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-091", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-092", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-093", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-094", "DbStore", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-095", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-096", "DbHelper", "ATOMIC_BUNDLE", true, 5));
        rules.add(new StorageRule("B03-REPO-097", "V51Repository", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-098", "DbStore", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-099",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-100",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(new StorageRule("B03-REPO-101", "V51Repository", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-102", "DbStore", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-103", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-104", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-105", "V51Repository", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-106", "DbStore", "ATOMIC_BUNDLE", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-107", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-108", "DbHelper", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-109",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-110",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-111", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-112", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-113", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-114", "DbStore", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-115", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-116", "DbHelper", "ATOMIC_BUNDLE", true, 4));
        rules.add(new StorageRule("B03-REPO-117", "V51Repository", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-118", "DbStore", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-119",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-120",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(new StorageRule("B03-REPO-121", "V51Repository", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-122", "DbStore", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-123", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-124", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-125", "V51Repository", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-126", "DbStore", "ATOMIC_BUNDLE", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-127", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-128", "DbHelper", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-129",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-130",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-131", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-132", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-133", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-134", "DbStore", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-135", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-136", "DbHelper", "ATOMIC_BUNDLE", true, 3));
        rules.add(new StorageRule("B03-REPO-137", "V51Repository", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-138", "DbStore", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-139",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-140",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(new StorageRule("B03-REPO-141", "V51Repository", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-142", "DbStore", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-143", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-144", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-145", "V51Repository", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-146", "DbStore", "ATOMIC_BUNDLE", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-147", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-148", "DbHelper", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-149",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-150",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-151", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-152", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-153", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-154", "DbStore", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-155", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-156", "DbHelper", "ATOMIC_BUNDLE", true, 2));
        rules.add(new StorageRule("B03-REPO-157", "V51Repository", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-158", "DbStore", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-159",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-160",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(new StorageRule("B03-REPO-161", "V51Repository", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-162", "DbStore", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-163", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-164", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-165", "V51Repository", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-166", "DbStore", "ATOMIC_BUNDLE", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-167", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-168", "DbHelper", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-169",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-170",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-171", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-172", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-173", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-174", "DbStore", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-175", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-176", "DbHelper", "ATOMIC_BUNDLE", true, 1));
        rules.add(new StorageRule("B03-REPO-177", "V51Repository", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-178", "DbStore", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-179",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-180",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(new StorageRule("B03-REPO-181", "V51Repository", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-182", "DbStore", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-183", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-184", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-185", "V51Repository", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-186", "DbStore", "ATOMIC_BUNDLE", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-187", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-188", "DbHelper", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-189",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-190",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-191", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-192", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-193", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-194", "DbStore", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-195", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-196", "DbHelper", "ATOMIC_BUNDLE", true, 7));
        rules.add(new StorageRule("B03-REPO-197", "V51Repository", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-198", "DbStore", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-199",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-200",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(new StorageRule("B03-REPO-201", "V51Repository", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-202", "DbStore", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-203", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-204", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-205", "V51Repository", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-206", "DbStore", "ATOMIC_BUNDLE", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-207", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-208", "DbHelper", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-209",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-210",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-211", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-212", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-213", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-214", "DbStore", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-215", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-216", "DbHelper", "ATOMIC_BUNDLE", true, 6));
        rules.add(new StorageRule("B03-REPO-217", "V51Repository", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-218", "DbStore", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-219",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-220",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(new StorageRule("B03-REPO-221", "V51Repository", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-222", "DbStore", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-223", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-224", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-225", "V51Repository", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-226", "DbStore", "ATOMIC_BUNDLE", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-227", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-228", "DbHelper", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-229",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-230",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-231", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-232", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-233", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-234", "DbStore", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-235", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-236", "DbHelper", "ATOMIC_BUNDLE", true, 5));
        rules.add(new StorageRule("B03-REPO-237", "V51Repository", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-238", "DbStore", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-239",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-240",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(new StorageRule("B03-REPO-241", "V51Repository", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-242", "DbStore", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-243", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-244", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-245", "V51Repository", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-246", "DbStore", "ATOMIC_BUNDLE", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-247", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-248", "DbHelper", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-249",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-250",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-251", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-252", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-253", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-254", "DbStore", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-255", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-256", "DbHelper", "ATOMIC_BUNDLE", true, 4));
        rules.add(new StorageRule("B03-REPO-257", "V51Repository", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-258", "DbStore", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-259",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-260",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(new StorageRule("B03-REPO-261", "V51Repository", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-262", "DbStore", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-263", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-264", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-265", "V51Repository", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-266", "DbStore", "ATOMIC_BUNDLE", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-267", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-268", "DbHelper", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-269",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-270",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-271", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-272", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-273", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-274", "DbStore", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-275", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-276", "DbHelper", "ATOMIC_BUNDLE", true, 3));
        rules.add(new StorageRule("B03-REPO-277", "V51Repository", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-278", "DbStore", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-279",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-280",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(new StorageRule("B03-REPO-281", "V51Repository", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-282", "DbStore", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-283", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-284", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-285", "V51Repository", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-286", "DbStore", "ATOMIC_BUNDLE", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-287", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-288", "DbHelper", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-289",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-290",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-291", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-292", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-293", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-294", "DbStore", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-295", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-296", "DbHelper", "ATOMIC_BUNDLE", true, 2));
        rules.add(new StorageRule("B03-REPO-297", "V51Repository", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-298", "DbStore", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-299",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-300",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(new StorageRule("B03-REPO-301", "V51Repository", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-302", "DbStore", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-303", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-304", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-305", "V51Repository", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-306", "DbStore", "ATOMIC_BUNDLE", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-307", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-308", "DbHelper", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-309",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-310",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-311", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-312", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-313", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-314", "DbStore", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-315", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-316", "DbHelper", "ATOMIC_BUNDLE", true, 1));
        rules.add(new StorageRule("B03-REPO-317", "V51Repository", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-318", "DbStore", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-319",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-320",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(new StorageRule("B03-REPO-321", "V51Repository", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-322", "DbStore", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-323", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-324", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-325", "V51Repository", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-326", "DbStore", "ATOMIC_BUNDLE", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-327", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-328", "DbHelper", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-329",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-330",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-331", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-332", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-333", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-334", "DbStore", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-335", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-336", "DbHelper", "ATOMIC_BUNDLE", true, 7));
        rules.add(new StorageRule("B03-REPO-337", "V51Repository", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-338", "DbStore", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-339",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-340",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(new StorageRule("B03-REPO-341", "V51Repository", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-342", "DbStore", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-343", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-344", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-345", "V51Repository", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-346", "DbStore", "ATOMIC_BUNDLE", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-347", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-348", "DbHelper", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-349",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-350",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-351", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-352", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-353", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-354", "DbStore", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-355", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-356", "DbHelper", "ATOMIC_BUNDLE", true, 6));
        rules.add(new StorageRule("B03-REPO-357", "V51Repository", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-358", "DbStore", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-359",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-360",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(new StorageRule("B03-REPO-361", "V51Repository", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-362", "DbStore", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-363", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-364", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-365", "V51Repository", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-366", "DbStore", "ATOMIC_BUNDLE", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-367", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-368", "DbHelper", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-369",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-370",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-371", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-372", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-373", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-374", "DbStore", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-375", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-376", "DbHelper", "ATOMIC_BUNDLE", true, 5));
        rules.add(new StorageRule("B03-REPO-377", "V51Repository", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-378", "DbStore", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-379",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-380",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(new StorageRule("B03-REPO-381", "V51Repository", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-382", "DbStore", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-383", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-384", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-385", "V51Repository", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-386", "DbStore", "ATOMIC_BUNDLE", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-387", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-388", "DbHelper", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-389",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-390",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-391", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-392", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-393", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-394", "DbStore", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-395", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-396", "DbHelper", "ATOMIC_BUNDLE", true, 4));
        rules.add(new StorageRule("B03-REPO-397", "V51Repository", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-398", "DbStore", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-399",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-400",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(new StorageRule("B03-REPO-401", "V51Repository", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-402", "DbStore", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-403", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-404", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-405", "V51Repository", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-406", "DbStore", "ATOMIC_BUNDLE", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-407", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-408", "DbHelper", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-409",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-410",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-411", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-412", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-413", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-414", "DbStore", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-415", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-416", "DbHelper", "ATOMIC_BUNDLE", true, 3));
        rules.add(new StorageRule("B03-REPO-417", "V51Repository", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-418", "DbStore", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-419",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-420",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(new StorageRule("B03-REPO-421", "V51Repository", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-422", "DbStore", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-423", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-424", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-425", "V51Repository", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-426", "DbStore", "ATOMIC_BUNDLE", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-427", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-428", "DbHelper", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-429",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-430",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-431", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-432", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-433", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-434", "DbStore", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-435", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-436", "DbHelper", "ATOMIC_BUNDLE", true, 2));
        rules.add(new StorageRule("B03-REPO-437", "V51Repository", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-438", "DbStore", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-439",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-440",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(new StorageRule("B03-REPO-441", "V51Repository", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-442", "DbStore", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-443", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-444", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-445", "V51Repository", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-446", "DbStore", "ATOMIC_BUNDLE", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-447", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-448", "DbHelper", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-449",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-450",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-451", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-452", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-453", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-454", "DbStore", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-455", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-456", "DbHelper", "ATOMIC_BUNDLE", true, 1));
        rules.add(new StorageRule("B03-REPO-457", "V51Repository", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-458", "DbStore", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-459",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-460",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(new StorageRule("B03-REPO-461", "V51Repository", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-462", "DbStore", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-463", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-464", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-465", "V51Repository", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-466", "DbStore", "ATOMIC_BUNDLE", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-467", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-468", "DbHelper", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-469",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-470",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-471", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-472", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-473", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-474", "DbStore", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-475", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-476", "DbHelper", "ATOMIC_BUNDLE", true, 7));
        rules.add(new StorageRule("B03-REPO-477", "V51Repository", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-478", "DbStore", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-479",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-480",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(new StorageRule("B03-REPO-481", "V51Repository", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-482", "DbStore", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-483", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-484", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-485", "V51Repository", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-486", "DbStore", "ATOMIC_BUNDLE", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-487", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-488", "DbHelper", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-489",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-490",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-491", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-492", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-493", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-494", "DbStore", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-495", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-496", "DbHelper", "ATOMIC_BUNDLE", true, 6));
        rules.add(new StorageRule("B03-REPO-497", "V51Repository", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-498", "DbStore", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-499",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-500",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(new StorageRule("B03-REPO-501", "V51Repository", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-502", "DbStore", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-503", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-504", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-505", "V51Repository", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-506", "DbStore", "ATOMIC_BUNDLE", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-507", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-508", "DbHelper", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-509",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-510",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-511", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-512", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-513", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-514", "DbStore", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-515", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-516", "DbHelper", "ATOMIC_BUNDLE", true, 5));
        rules.add(new StorageRule("B03-REPO-517", "V51Repository", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-518", "DbStore", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-519",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-520",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(new StorageRule("B03-REPO-521", "V51Repository", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-522", "DbStore", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-523", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-524", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-525", "V51Repository", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-526", "DbStore", "ATOMIC_BUNDLE", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-527", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-528", "DbHelper", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-529",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-530",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-531", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-532", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-533", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-534", "DbStore", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-535", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-536", "DbHelper", "ATOMIC_BUNDLE", true, 4));
        rules.add(new StorageRule("B03-REPO-537", "V51Repository", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-538", "DbStore", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-539",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-540",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(new StorageRule("B03-REPO-541", "V51Repository", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-542", "DbStore", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-543", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-544", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-545", "V51Repository", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-546", "DbStore", "ATOMIC_BUNDLE", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-547", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-548", "DbHelper", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-549",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-550",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-551", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-552", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-553", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-554", "DbStore", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-555", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-556", "DbHelper", "ATOMIC_BUNDLE", true, 3));
        rules.add(new StorageRule("B03-REPO-557", "V51Repository", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-558", "DbStore", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-559",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-560",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(new StorageRule("B03-REPO-561", "V51Repository", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-562", "DbStore", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-563", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-564", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-565", "V51Repository", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-566", "DbStore", "ATOMIC_BUNDLE", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-567", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-568", "DbHelper", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-569",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-570",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-571", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-572", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-573", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-574", "DbStore", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-575", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-576", "DbHelper", "ATOMIC_BUNDLE", true, 2));
        rules.add(new StorageRule("B03-REPO-577", "V51Repository", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-578", "DbStore", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-579",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-580",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(new StorageRule("B03-REPO-581", "V51Repository", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-582", "DbStore", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-583", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-584", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-585", "V51Repository", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-586", "DbStore", "ATOMIC_BUNDLE", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-587", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-588", "DbHelper", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-589",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-590",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-591", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-592", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-593", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-594", "DbStore", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-595", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-596", "DbHelper", "ATOMIC_BUNDLE", true, 1));
        rules.add(new StorageRule("B03-REPO-597", "V51Repository", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-598", "DbStore", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-599",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-600",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(new StorageRule("B03-REPO-601", "V51Repository", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-602", "DbStore", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-603", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-604", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-605", "V51Repository", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-606", "DbStore", "ATOMIC_BUNDLE", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-607", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-608", "DbHelper", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-609",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-610",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-611", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-612", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-613", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-614", "DbStore", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-615", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-616", "DbHelper", "ATOMIC_BUNDLE", true, 7));
        rules.add(new StorageRule("B03-REPO-617", "V51Repository", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-618", "DbStore", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-619",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-620",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(new StorageRule("B03-REPO-621", "V51Repository", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-622", "DbStore", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-623", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-624", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-625", "V51Repository", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-626", "DbStore", "ATOMIC_BUNDLE", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-627", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-628", "DbHelper", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-629",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-630",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-631", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-632", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-633", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-634", "DbStore", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-635", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-636", "DbHelper", "ATOMIC_BUNDLE", true, 6));
        rules.add(new StorageRule("B03-REPO-637", "V51Repository", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-638", "DbStore", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-639",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-640",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(new StorageRule("B03-REPO-641", "V51Repository", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-642", "DbStore", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-643", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-644", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-645", "V51Repository", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-646", "DbStore", "ATOMIC_BUNDLE", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-647", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-648", "DbHelper", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-649",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-650",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-651", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-652", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-653", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-654", "DbStore", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-655", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-656", "DbHelper", "ATOMIC_BUNDLE", true, 5));
        rules.add(new StorageRule("B03-REPO-657", "V51Repository", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-658", "DbStore", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-659",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-660",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(new StorageRule("B03-REPO-661", "V51Repository", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-662", "DbStore", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-663", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-664", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-665", "V51Repository", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-666", "DbStore", "ATOMIC_BUNDLE", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-667", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-668", "DbHelper", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-669",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-670",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-671", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-672", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-673", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-674", "DbStore", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-675", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-676", "DbHelper", "ATOMIC_BUNDLE", true, 4));
        rules.add(new StorageRule("B03-REPO-677", "V51Repository", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-678", "DbStore", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-679",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-680",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(new StorageRule("B03-REPO-681", "V51Repository", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-682", "DbStore", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-683", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-684", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-685", "V51Repository", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-686", "DbStore", "ATOMIC_BUNDLE", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-687", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-688", "DbHelper", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-689",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-690",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-691", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-692", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-693", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-694", "DbStore", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-695", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-696", "DbHelper", "ATOMIC_BUNDLE", true, 3));
        rules.add(new StorageRule("B03-REPO-697", "V51Repository", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-698", "DbStore", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-699",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-700",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(new StorageRule("B03-REPO-701", "V51Repository", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-702", "DbStore", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-703", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-704", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-705", "V51Repository", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-706", "DbStore", "ATOMIC_BUNDLE", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-707", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-708", "DbHelper", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-709",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-710",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-711", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-712", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-713", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-714", "DbStore", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-715", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-716", "DbHelper", "ATOMIC_BUNDLE", true, 2));
        rules.add(new StorageRule("B03-REPO-717", "V51Repository", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-718", "DbStore", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-719",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-720",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(new StorageRule("B03-REPO-721", "V51Repository", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-722", "DbStore", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-723", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-724", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-725", "V51Repository", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-726", "DbStore", "ATOMIC_BUNDLE", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-727", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-728", "DbHelper", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-729",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-730",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-731", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-732", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-733", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-734", "DbStore", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-735", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-736", "DbHelper", "ATOMIC_BUNDLE", true, 1));
        rules.add(new StorageRule("B03-REPO-737", "V51Repository", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-738", "DbStore", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-739",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-740",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(new StorageRule("B03-REPO-741", "V51Repository", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-742", "DbStore", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-743", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-744", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-745", "V51Repository", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-746", "DbStore", "ATOMIC_BUNDLE", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-747", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-748", "DbHelper", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-749",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-750",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-751", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-752", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-753", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-754", "DbStore", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-755", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-756", "DbHelper", "ATOMIC_BUNDLE", true, 7));
        rules.add(new StorageRule("B03-REPO-757", "V51Repository", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-758", "DbStore", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-759",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-760",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(new StorageRule("B03-REPO-761", "V51Repository", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-762", "DbStore", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-763", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-764", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-765", "V51Repository", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-766", "DbStore", "ATOMIC_BUNDLE", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-767", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-768", "DbHelper", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-769",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-770",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-771", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-772", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-773", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-774", "DbStore", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-775", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-776", "DbHelper", "ATOMIC_BUNDLE", true, 6));
        rules.add(new StorageRule("B03-REPO-777", "V51Repository", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-778", "DbStore", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-779",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-780",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(new StorageRule("B03-REPO-781", "V51Repository", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-782", "DbStore", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-783", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-784", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-785", "V51Repository", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-786", "DbStore", "ATOMIC_BUNDLE", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-787", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-788", "DbHelper", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-789",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-790",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-791", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-792", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-793", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-794", "DbStore", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-795", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-796", "DbHelper", "ATOMIC_BUNDLE", true, 5));
        rules.add(new StorageRule("B03-REPO-797", "V51Repository", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-798", "DbStore", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-799",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-800",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(new StorageRule("B03-REPO-801", "V51Repository", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-802", "DbStore", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-803", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-804", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-805", "V51Repository", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-806", "DbStore", "ATOMIC_BUNDLE", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-807", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-808", "DbHelper", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-809",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-810",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-811", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-812", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-813", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-814", "DbStore", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-815", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-816", "DbHelper", "ATOMIC_BUNDLE", true, 4));
        rules.add(new StorageRule("B03-REPO-817", "V51Repository", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-818", "DbStore", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-819",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-820",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(new StorageRule("B03-REPO-821", "V51Repository", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-822", "DbStore", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-823", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-824", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-825", "V51Repository", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-826", "DbStore", "ATOMIC_BUNDLE", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-827", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-828", "DbHelper", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-829",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-830",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-831", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-832", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-833", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-834", "DbStore", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-835", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-836", "DbHelper", "ATOMIC_BUNDLE", true, 3));
        rules.add(new StorageRule("B03-REPO-837", "V51Repository", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-838", "DbStore", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-839",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-840",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(new StorageRule("B03-REPO-841", "V51Repository", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-842", "DbStore", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-843", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-844", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-845", "V51Repository", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-846", "DbStore", "ATOMIC_BUNDLE", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-847", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-848", "DbHelper", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-849",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-850",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-851", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-852", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule("B03-REPO-853", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-854", "DbStore", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-855", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-856", "DbHelper", "ATOMIC_BUNDLE", true, 2));
        rules.add(new StorageRule("B03-REPO-857", "V51Repository", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-858", "DbStore", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-859",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-860",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(new StorageRule("B03-REPO-861", "V51Repository", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-862", "DbStore", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-863", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-864", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-865", "V51Repository", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-866", "DbStore", "ATOMIC_BUNDLE", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-867", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-868", "DbHelper", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-869",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-870",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-871", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-872", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule("B03-REPO-873", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-874", "DbStore", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-875", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-876", "DbHelper", "ATOMIC_BUNDLE", true, 1));
        rules.add(new StorageRule("B03-REPO-877", "V51Repository", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-878", "DbStore", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-879",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-880",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        rules.add(new StorageRule("B03-REPO-881", "V51Repository", "GAME_CODE_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-882", "DbStore", "TARGET_ISSUE_REQUIRED", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-883", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-884", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-885", "V51Repository", "RECORD_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-886", "DbStore", "ATOMIC_BUNDLE", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-887", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 5));
        rules.add(new StorageRule("B03-REPO-888", "DbHelper", "IMMUTABLE_FIELDS", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-889",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-890",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-891", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-892", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 3));
        rules.add(
                new StorageRule("B03-REPO-893", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-894", "DbStore", "CONTRACT_HASH_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-895", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-896", "DbHelper", "ATOMIC_BUNDLE", true, 7));
        rules.add(new StorageRule("B03-REPO-897", "V51Repository", "IDEMPOTENT_INSERT", true, 1));
        rules.add(new StorageRule("B03-REPO-898", "DbStore", "IMMUTABLE_FIELDS", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-899",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        3));
        rules.add(
                new StorageRule(
                        "B03-REPO-900",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        4));
        rules.add(new StorageRule("B03-REPO-901", "V51Repository", "GAME_CODE_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-902", "DbStore", "TARGET_ISSUE_REQUIRED", true, 6));
        rules.add(
                new StorageRule(
                        "B03-REPO-903", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-904", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-905", "V51Repository", "RECORD_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-906", "DbStore", "ATOMIC_BUNDLE", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-907", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 4));
        rules.add(new StorageRule("B03-REPO-908", "DbHelper", "IMMUTABLE_FIELDS", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-909",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-910",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        7));
        rules.add(
                new StorageRule(
                        "B03-REPO-911", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-912", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 2));
        rules.add(
                new StorageRule("B03-REPO-913", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-914", "DbStore", "CONTRACT_HASH_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-915", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-916", "DbHelper", "ATOMIC_BUNDLE", true, 6));
        rules.add(new StorageRule("B03-REPO-917", "V51Repository", "IDEMPOTENT_INSERT", true, 7));
        rules.add(new StorageRule("B03-REPO-918", "DbStore", "IMMUTABLE_FIELDS", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-919",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        2));
        rules.add(
                new StorageRule(
                        "B03-REPO-920",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        3));
        rules.add(new StorageRule("B03-REPO-921", "V51Repository", "GAME_CODE_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-922", "DbStore", "TARGET_ISSUE_REQUIRED", true, 5));
        rules.add(
                new StorageRule(
                        "B03-REPO-923", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 6));
        rules.add(new StorageRule("B03-REPO-924", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 7));
        rules.add(
                new StorageRule("B03-REPO-925", "V51Repository", "RECORD_HASH_REQUIRED", true, 1));
        rules.add(new StorageRule("B03-REPO-926", "DbStore", "ATOMIC_BUNDLE", true, 2));
        rules.add(
                new StorageRule(
                        "B03-REPO-927", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 3));
        rules.add(new StorageRule("B03-REPO-928", "DbHelper", "IMMUTABLE_FIELDS", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-929",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        5));
        rules.add(
                new StorageRule(
                        "B03-REPO-930",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        6));
        rules.add(
                new StorageRule(
                        "B03-REPO-931", "DatabaseCoordinator", "GAME_CODE_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-932", "DbHelper", "TARGET_ISSUE_REQUIRED", true, 1));
        rules.add(
                new StorageRule("B03-REPO-933", "V51Repository", "BUNDLE_HASH_REQUIRED", true, 2));
        rules.add(new StorageRule("B03-REPO-934", "DbStore", "CONTRACT_HASH_REQUIRED", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-935", "DatabaseCoordinator", "RECORD_HASH_REQUIRED", true, 4));
        rules.add(new StorageRule("B03-REPO-936", "DbHelper", "ATOMIC_BUNDLE", true, 5));
        rules.add(new StorageRule("B03-REPO-937", "V51Repository", "IDEMPOTENT_INSERT", true, 6));
        rules.add(new StorageRule("B03-REPO-938", "DbStore", "IMMUTABLE_FIELDS", true, 7));
        rules.add(
                new StorageRule(
                        "B03-REPO-939",
                        "DatabaseCoordinator",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        1));
        rules.add(
                new StorageRule(
                        "B03-REPO-940",
                        "DbHelper",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        2));
        rules.add(new StorageRule("B03-REPO-941", "V51Repository", "GAME_CODE_REQUIRED", true, 3));
        rules.add(new StorageRule("B03-REPO-942", "DbStore", "TARGET_ISSUE_REQUIRED", true, 4));
        rules.add(
                new StorageRule(
                        "B03-REPO-943", "DatabaseCoordinator", "BUNDLE_HASH_REQUIRED", true, 5));
        rules.add(new StorageRule("B03-REPO-944", "DbHelper", "CONTRACT_HASH_REQUIRED", true, 6));
        rules.add(
                new StorageRule("B03-REPO-945", "V51Repository", "RECORD_HASH_REQUIRED", true, 7));
        rules.add(new StorageRule("B03-REPO-946", "DbStore", "ATOMIC_BUNDLE", true, 1));
        rules.add(
                new StorageRule(
                        "B03-REPO-947", "DatabaseCoordinator", "IDEMPOTENT_INSERT", true, 2));
        rules.add(new StorageRule("B03-REPO-948", "DbHelper", "IMMUTABLE_FIELDS", true, 3));
        rules.add(
                new StorageRule(
                        "B03-REPO-949",
                        "V51Repository",
                        "MODEL_NOT_PROVEN_EVIDENCE_ISOLATION",
                        true,
                        4));
        rules.add(
                new StorageRule(
                        "B03-REPO-950",
                        "DbStore",
                        "BET_PERSIST_REQUIRES_DECISION_GATE_AUTH",
                        true,
                        5));
        return Collections.unmodifiableList(rules);
    }

    public static String storageRuleHash() {
        StringBuilder b = new StringBuilder();
        List<StorageRule> x = storageRules();
        for (int i = 0; i < x.size(); i++) {
            if (i > 0) b.append('|');
            b.append(x.get(i).canonical());
        }
        return V51ChecksumEngine.sha256(b.toString());
    }

    public static boolean selfTest() {
        return GAME_CODE.equals(AppConfig.LOTTERY_CODE)
                && storageRules().size() == 950
                && storageRuleHash().length() == 64
                && !V51BuildManifest.MODEL_PROVEN
                && V51BuildManifest.SHADOW_ONLY
                && V51BuildManifest.REAL_BET_HARD_DISABLED
                && !V51BuildManifest.CANDIDATE_REAL_AUTH_ENABLED;
    }

    public static final class B12InvariantAudit {
        public final String targetIssue;
        public final int issue, number, compatAction, fullAction, champion, funnel, selected, auth;
        public final boolean duplicateTrade, duplicateSettlement, realShadowSeparated, exact, pass;
        public final String detail, checksum;

        B12InvariantAudit(
                String target,
                int issue,
                int number,
                int compat,
                int full,
                int champion,
                int funnel,
                int selected,
                int auth,
                boolean dupTrade,
                boolean dupSettlement,
                boolean separated) {
            targetIssue = safe(target);
            this.issue = issue;
            this.number = number;
            compatAction = compat;
            fullAction = full;
            this.champion = champion;
            this.funnel = funnel;
            this.selected = selected;
            this.auth = auth;
            duplicateTrade = dupTrade;
            duplicateSettlement = dupSettlement;
            realShadowSeparated = separated;
            exact =
                    issue == 1
                            && number == 100
                            && compat == 10
                            && full == 1200
                            && champion == 1
                            && funnel == 10
                            && selected <= 1
                            && auth <= 1;
            pass = exact && !dupTrade && !dupSettlement && separated;
            detail =
                    "issue="
                            + issue
                            + "|number="
                            + number
                            + "|compat="
                            + compat
                            + "|full="
                            + full
                            + "|champion="
                            + champion
                            + "|funnel="
                            + funnel
                            + "|selected="
                            + selected
                            + "|auth="
                            + auth
                            + "|dupTrade="
                            + dupTrade
                            + "|dupSettlement="
                            + dupSettlement
                            + "|separated="
                            + separated;
            checksum = V51ChecksumEngine.sha256(targetIssue + '|' + detail + '|' + pass);
        }
    }

    public static B12InvariantAudit auditB12(DbStore store, String target) {
        if (store == null)
            return new B12InvariantAudit(target, 0, 0, 0, 0, 0, 0, 0, 0, true, true, false);
        SQLiteDatabase db = store.readable();
        int issue = count(db, ISSUE, target),
                number = count(db, NUMBER, target),
                compat = count(db, ACTION, target),
                full = count(db, ACTION_SPACE, target),
                champ = count(db, CHAMPION, target),
                funnel = count(db, FUNNEL, target),
                sel = count(db, SELECTED, target),
                auth =
                        intScalar(
                                db,
                                "SELECT COUNT(1) FROM " + AUTH + " WHERE target_issue=?",
                                new String[] {target});
        int trades =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_TRADES
                                + " WHERE target_issue=? AND stat_scope=?",
                        new String[] {target, AppConfig.STAT_SCOPE_REAL_TRADE});
        int settlements =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_B03_SETTLEMENT_EVENT
                                + " WHERE target_issue=?",
                        new String[] {target});
        int shadowReal =
                intScalar(
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE target_issue=? AND decision_action='SHADOW' AND result IN"
                                + " ('WIN','LOSS')",
                        new String[] {target});
        return new B12InvariantAudit(
                target,
                issue,
                number,
                compat,
                full,
                champ,
                funnel,
                sel,
                auth,
                trades > 1,
                settlements > 1,
                shadowReal == 0);
    }

public static B12InvariantAudit auditB12SnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String target) {
        if (store == null)
            return new B12InvariantAudit(target, 0, 0, 0, 0, 0, 0, 0, 0, true, true, false);
        SQLiteDatabase db = store.readable();
        int issue = countSnapshotCP4(report, db, ISSUE, target),
                number = countSnapshotCP4(report, db, NUMBER, target),
                compat = countSnapshotCP4(report, db, ACTION, target),
                full = countSnapshotCP4(report, db, ACTION_SPACE, target),
                champ = countSnapshotCP4(report, db, CHAMPION, target),
                funnel = countSnapshotCP4(report, db, FUNNEL, target),
                sel = countSnapshotCP4(report, db, SELECTED, target),
                auth =
                        intScalarSnapshotCP4(report, 
                                db,
                                "SELECT COUNT(1) FROM " + AUTH + " WHERE target_issue=?",
                                new String[] {target});
        int trades =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_TRADES
                                + " WHERE target_issue=? AND stat_scope=?",
                        new String[] {target, AppConfig.STAT_SCOPE_REAL_TRADE});
        int settlements =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_B03_SETTLEMENT_EVENT
                                + " WHERE target_issue=?",
                        new String[] {target});
        int shadowReal =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE target_issue=? AND decision_action='SHADOW' AND result IN"
                                + " ('WIN','LOSS')",
                        new String[] {target});
        return new B12InvariantAudit(
                target,
                issue,
                number,
                compat,
                full,
                champ,
                funnel,
                sel,
                auth,
                trades > 1,
                settlements > 1,
                shadowReal == 0);
    }

public static int intScalarSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String sql, String[] args) {
        Cursor c = null;
        try {
            c = report.select(sql, args);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

public static int countSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String table, String target) {
        Cursor c = null;
        try {
            c =
                    report.select(
                            "SELECT COUNT(1) FROM " + table + " WHERE target_issue=?",
                            new String[] {target});
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

public static DiagnosticStats diagnosticStatsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        DiagnosticStats x = new DiagnosticStats();
        if (store == null) return x;
        SQLiteDatabase db = store.readable();
        x.predictions =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM " + ISSUE + " WHERE game_code=?",
                        new String[] {GAME_CODE});
        x.selected =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM " + SELECTED + " WHERE game_code=?",
                        new String[] {GAME_CODE});
        x.bet =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='BET'",
                        new String[] {GAME_CODE});
        x.shadow =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='SHADOW'",
                        new String[] {GAME_CODE});
        x.hold =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='HOLD'",
                        new String[] {GAME_CODE});
        x.settled =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND settled=1 AND result IN ('HIT','MISS')",
                        new String[] {GAME_CODE});
        x.hit =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND settled=1 AND result='HIT'",
                        new String[] {GAME_CODE});
        x.betSettled =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='BET' AND settled=1 AND"
                                + " result IN ('HIT','MISS')",
                        new String[] {GAME_CODE});
        x.betHit =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='BET' AND settled=1 AND"
                                + " result='HIT'",
                        new String[] {GAME_CODE});
        x.shadowSettled =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='SHADOW' AND settled=1"
                                + " AND result IN ('HIT','MISS')",
                        new String[] {GAME_CODE});
        x.shadowHit =
                intScalarSnapshotCP4(report, 
                        db,
                        "SELECT COUNT(1) FROM "
                                + SELECTED
                                + " WHERE game_code=? AND decision_action='SHADOW' AND settled=1"
                                + " AND result='HIT'",
                        new String[] {GAME_CODE});
        x.coverage = x.predictions <= 0 ? 0 : (double) x.bet / x.predictions;
        x.hitRate = x.settled <= 0 ? 0 : (double) x.hit / x.settled;
        x.betHitRate = x.betSettled <= 0 ? 0 : (double) x.betHit / x.betSettled;
        x.shadowHitRate = x.shadowSettled <= 0 ? 0 : (double) x.shadowHit / x.shadowSettled;
        x.wilsonLower = wilsonSnapshotCP4(report, x.hit, x.settled);
        x.ece = selectedEceSnapshotCP4(report, db);
        return x;
    }

public static double selectedEceSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db) {
        Cursor c = null;
        try {
            c =
                    report.select(
                            "SELECT selected_hit_p,result FROM "
                                    + SELECTED
                                    + " WHERE game_code=? AND settled=1 AND selected_hit_p>=0 AND"
                                    + " selected_hit_p<=1 AND result IN ('HIT','MISS')",
                            new String[] {GAME_CODE});
            int bins = 10;
            int[] count = new int[bins];
            double[] sumP = new double[bins], sumY = new double[bins];
            int total = 0;
            while (c.moveToNext()) {
                double p = c.getDouble(0);
                if (Double.isNaN(p) || Double.isInfinite(p)) continue;
                if (p < 0) p = 0;
                if (p > 1) p = 1;
                int y = "HIT".equals(c.getString(1)) ? 1 : 0;
                int b = (int) Math.floor(p * bins);
                if (b >= bins) b = bins - 1;
                if (b < 0) b = 0;
                count[b]++;
                sumP[b] += p;
                sumY[b] += y;
                total++;
            }
            if (total <= 0) return 0;
            double ece = 0;
            for (int b = 0; b < bins; b++) {
                if (count[b] <= 0) continue;
                double avgP = sumP[b] / count[b], avgY = sumY[b] / count[b];
                ece += ((double) count[b] / total) * Math.abs(avgP - avgY);
            }
            return ece;
        } finally {
            if (c != null) c.close();
        }
    }

public static double wilsonSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int hit, int n) {
        if (n <= 0) return 0;
        double z = 1.959963984540054d,
                p = (double) hit / n,
                z2 = z * z,
                den = 1 + z2 / n,
                center = p + z2 / (2 * n),
                adj = z * Math.sqrt((p * (1 - p) + z2 / (4 * n)) / n);
        return Math.max(0, (center - adj) / den);
    }
}
