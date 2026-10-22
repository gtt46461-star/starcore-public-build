package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

/**
 * Semantic event hooks for the real runtime chain. These hooks do not own business decisions;
 * they bind immutable evidence to the exact owner transaction and reject only broken provenance.
 */
public final class RuntimeEventHooksR926 {
    private RuntimeEventHooksR926() {}

    public static boolean requested(DbStore store, AnalysisSingleFlightR36.Request r) {
        return append(store, r, "REQUESTED", "REQUESTED", RuntimeEventLedgerR926.RANK_REQUESTED,
                "base=" + r.baseIssue + "|reason=" + safe(r.reason) + "|deadline=" + r.deadlineAt
                        + "|request_budget_ms=" + Batch2ContractCoreR926.REQUEST_TO_DURABLE_BUDGET_MS,
                "AutoFetchService", "REQUESTED");
    }

    public static boolean queued(DbStore store, AnalysisSingleFlightR36.Request r, String ownerTarget) {
        return append(store, r, "QUEUED", "QUEUED", RuntimeEventLedgerR926.RANK_QUEUED,
                "owner_target=" + safe(ownerTarget), "AnalysisSingleFlightR36", "QUEUED");
    }

    public static boolean accepted(DbStore store, AnalysisSingleFlightR36.Request r, String ownerId) {
        return append(store, r, "ACCEPTED", "ACCEPTED", RuntimeEventLedgerR926.RANK_ACCEPTED,
                "owner=" + safe(ownerId), "AnalysisSingleFlightR36", "ACCEPTED");
    }

    public static boolean rejected(DbStore store, AnalysisSingleFlightR36.Request r, String code) {
        return append(store, r, "REJECTED", safe(code), RuntimeEventLedgerR926.RANK_ACCEPTED,
                "code=" + safe(code), "AutoFetchService", "REJECTED|" + safe(code));
    }

    public static boolean started(DbStore store, AnalysisSingleFlightR36.Request r, String ownerId) {
        return append(store, r, "STARTED", "RUNNING", RuntimeEventLedgerR926.RANK_STARTED,
                "owner=" + safe(ownerId), "AutoFetchService", "STARTED");
    }

    public static boolean frozen(DbStore store, AnalysisSingleFlightR36.Request r, V51RuntimeInputR36 input) {
        return append(store, r, "FROZEN", "PIT_FROZEN", RuntimeEventLedgerR926.RANK_FROZEN,
                "history_hash=" + safe(input == null ? "" : input.historyHash)
                        + "|history_max_issue=" + safe(input == null ? "" : input.baseIssue),
                "V51RuntimeInputR36", "FROZEN");
    }

    public static void canonicalWithinTransaction(
            SQLiteDatabase db,
            AnalysisSingleFlightR36.Request r,
            DeferredEvidenceSnapshotR36.Header snapshot,
            String snapshotHash,
            RuntimeTraceR36 trace,
            String canonicalHash) {
        canonicalWithinTransaction(db,null,r,snapshot,snapshotHash,trace,canonicalHash,null);
    }

    public static void canonicalWithinTransaction(SQLiteDatabase db,DbStore store,
            AnalysisSingleFlightR36.Request r,DeferredEvidenceSnapshotR36.Header snapshot,
            String snapshotHash,RuntimeTraceR36 trace,String canonicalHash,V51CompiledActionContract compiled) {
        Batch2ContractCoreR926.GateCheck gateEvidence =
                Batch2GateEvidenceR926.validateAndAppendWithinTransaction(
                        db,store,r,snapshot,snapshotHash,canonicalHash,compiled);
        String traceText = trace == null ? "" : trace.compactStagesR923();
        long wallAuditElapsed = Math.max(0L, System.currentTimeMillis() - r.requestAt);
        String payload = "canonical_hash=" + safe(canonicalHash)
                + "|snapshot_hash=" + safe(snapshotHash)
                + "|history_hash=" + safe(snapshot == null ? "" : snapshot.historyHash)
                + "|action_space_hash=" + safe(snapshot == null ? "" : snapshot.actionSpaceHash)
                + "|bundle_hash=" + safe(snapshot == null ? "" : snapshot.bundleHash)
                + "|contract_hash=" + safe(snapshot == null ? "" : snapshot.contractHash)
                + "|snapshot_record_hash=" + safe(snapshot == null ? "" : snapshot.recordHash)
                + "|gate_provenance=" + safe(gateEvidence.provenanceHash)
                + "|wall_elapsed_audit_ms=" + wallAuditElapsed
                + "|action_rows=1200|number_rows=100|stage_trace=" + safe(traceText);
        RuntimeEventLedgerR926.appendWithinTransaction(
                db,
                RuntimeEventLedgerR926.Event.of(
                        r.targetIssue,
                        String.valueOf(r.generation),
                        r.attemptId(),
                        "CANONICAL",
                        "CANONICAL_COMMITTED",
                        RuntimeEventLedgerR926.RANK_CANONICAL,
                        payload,
                        "V51Repository",
                        key(r, "CANONICAL")));

        // R930 C04/D09 is an evidence consumer only. It executes after the canonical event and
        // isolates itself with a SAVEPOINT, so a research failure cannot suppress an issue output.
        Batch34RuntimeRepositoryR930.captureCanonicalWithinTransaction(
                db, r, snapshot, snapshotHash, canonicalHash);
    }

    public static void settledWithinTransaction(
            SQLiteDatabase db, String issue, String settlementHash, String status, String drawHash) {
        RuntimeEventLedgerR926.appendWithinTransaction(
                db,
                RuntimeEventLedgerR926.Event.of(
                        issue,
                        "",
                        "",
                        "SETTLED",
                        safe(status),
                        RuntimeEventLedgerR926.RANK_SETTLED,
                        "settlement_hash=" + safe(settlementHash) + "|draw_hash=" + safe(drawHash),
                        "SettlementEngine",
                        "SETTLED|" + safe(issue) + '|' + safe(drawHash)));
        String identity = Batch2ContractCoreR926.learningKey(
                ReleaseManifestR926.GAME_CODE,
                issue,
                "STRICT_OOS_LEARNING_FRONTIER_R926",
                "DRAW_HASH_V1",
                drawHash);
        RuntimeEventLedgerR926.appendWithinTransaction(
                db,
                RuntimeEventLedgerR926.Event.of(
                        issue,
                        "",
                        "",
                        "LEARNING_PENDING",
                        "LEARNING_PENDING",
                        RuntimeEventLedgerR926.RANK_SETTLED,
                        "learning_identity=" + identity
                                + "|settlement_hash=" + safe(settlementHash)
                                + "|draw_hash=" + safe(drawHash),
                        "SettlementEngine",
                        "LEARNING_PENDING|" + safe(issue) + '|' + identity));

        Batch34RuntimeRepositoryR930.recordSettlementWithinTransaction(
                db, issue, settlementHash, status, drawHash);
    }

    public static void fullWithinTransaction(
            SQLiteDatabase db, String issue, String snapshotHash, String actionSpaceHash) {
        RuntimeEventLedgerR926.appendWithinTransaction(
                db,
                RuntimeEventLedgerR926.Event.of(
                        issue,
                        "",
                        "",
                        "FULL",
                        "FULL_EVIDENCE_COMPLETED",
                        RuntimeEventLedgerR926.RANK_FULL,
                        "snapshot_hash=" + safe(snapshotHash)
                                + "|action_space_hash=" + safe(actionSpaceHash)
                                + "|number_rows=100|action_rows=1200|summary_rows=10|funnel_rows=10",
                        "DeferredEvidenceWriterR36",
                        "FULL|" + safe(issue) + '|' + safe(snapshotHash)));
        Batch34RuntimeRepositoryR930.recordFullWithinTransaction(
                db, issue, snapshotHash, actionSpaceHash);
    }

    /**
     * Atomic learner completion for learners that update model state inside the caller-owned SQLite
     * transaction. Invalid evidence aborts that same transaction so artifact state and LEARNED truth
     * can never diverge.
     */
    public static void learnedWithinTransaction(
            SQLiteDatabase db,
            String issue,
            String source,
            String learningIdentity,
            String preStateHash,
            String postStateHash,
            String trainedUntil,
            String detail) {
        if (db == null || !db.inTransaction())
            throw new IllegalStateException("B09_LEARN_TRANSACTION_REQUIRED");
        if (!TargetIssueResolver.isValid(issue))
            throw new IllegalArgumentException("B09_LEARN_ISSUE_INVALID");
        if (safe(source).length() == 0)
            throw new IllegalArgumentException("B09_LEARN_SOURCE_MISSING");
        if (!Batch2ContractCoreR926.isSha256(learningIdentity))
            throw new IllegalArgumentException("B09_LEARNING_IDENTITY_INVALID");
        if (!Batch2ContractCoreR926.isSha256(preStateHash))
            throw new IllegalArgumentException("B09_PRE_STATE_HASH_INVALID");
        if (!Batch2ContractCoreR926.isSha256(postStateHash))
            throw new IllegalArgumentException("B09_POST_STATE_HASH_INVALID");
        if (!safe(issue).equals(safe(trainedUntil)))
            throw new IllegalArgumentException("B09_TRAINED_UNTIL_MISMATCH");
        RuntimeEventLedgerR926.appendWithinTransaction(
                db,
                RuntimeEventLedgerR926.Event.of(
                        issue,
                        "",
                        "",
                        "LEARNED",
                        "LEARNED_EXACTLY_ONCE",
                        RuntimeEventLedgerR926.RANK_LEARNED,
                        "learning_identity=" + learningIdentity
                                + "|pre_state_hash=" + safe(preStateHash)
                                + "|post_state_hash=" + safe(postStateHash)
                                + "|trained_until=" + safe(trainedUntil)
                                + "|detail=" + trim(detail, 1000),
                        safe(source),
                        "LEARNED|" + safe(issue) + '|' + safe(source) + '|' + learningIdentity));
        Batch34RuntimeRepositoryR930.recordLearnedWithinTransaction(
                db,
                issue,
                source,
                learningIdentity,
                preStateHash,
                postStateHash,
                trainedUntil);
    }

    public static boolean learned(DbStore store, String issue, String source, String detail) {
        if (store == null || safe(issue).length() == 0) return false;
        return RuntimeExternalEventWriterR926.append(
                store,
                RuntimeEventLedgerR926.Event.of(
                        issue,
                        "",
                        "",
                        "LEARNED",
                        "LEARNED_RECONCILED",
                        RuntimeEventLedgerR926.RANK_LEARNED,
                        "detail=" + trim(detail, 1000),
                        safe(source),
                        "LEARNED_RECONCILED|" + safe(issue) + '|' + safe(source)));
    }

    public static boolean recovery(DbStore store, String issue, String code, String detail) {
        if (store == null || safe(issue).length() == 0) return false;
        return RuntimeExternalEventWriterR926.append(
                store,
                RuntimeEventLedgerR926.Event.of(
                        issue,
                        "",
                        "",
                        "RECOVERY",
                        safe(code),
                        RuntimeEventLedgerR926.RANK_REQUESTED,
                        "detail=" + trim(detail, 1000),
                        "RuntimeRecoveryR926",
                        "RECOVERY|" + safe(issue) + '|' + safe(code) + '|' + V51ChecksumEngine.sha256(trim(detail, 500))));
    }

    private static boolean append(
            DbStore store,
            AnalysisSingleFlightR36.Request r,
            String type,
            String stage,
            int rank,
            String payload,
            String source,
            String suffix) {
        if (store == null || r == null || !r.valid()) return false;
        return RuntimeExternalEventWriterR926.append(
                store,
                RuntimeEventLedgerR926.Event.of(
                        r.targetIssue,
                        String.valueOf(r.generation),
                        r.attemptId(),
                        type,
                        stage,
                        rank,
                        payload,
                        source,
                        key(r, suffix)));
    }

    private static String key(AnalysisSingleFlightR36.Request r, String suffix) {
        return safe(r.targetIssue) + '|' + safe(r.attemptId()) + '|' + safe(suffix);
    }

    private static String trim(String s, int max) {
        String x = safe(s);
        return x.length() <= max ? x : x.substring(0, max);
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
