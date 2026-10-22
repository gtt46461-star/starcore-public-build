package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** The only R3.6 writer for the frozen pre-draw canonical observation. */
public final class CanonicalObservationWriterR36 {
    private CanonicalObservationWriterR36() {}

    static WriteResult insertWithinTransaction(
            SQLiteDatabase db, DatabaseCoordinator.TransactionContext tx, CanonicalRecord record) {
        if (db == null || !db.inTransaction() || tx == null)
            throw new IllegalStateException("R36_CANONICAL_TRANSACTION_REQUIRED");
        record.validate();
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT record_hash,history_hash,snapshot_hash,action_space_hash FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE target_issue=? LIMIT 1",
                            new String[] {record.targetIssue});
            if (c.moveToFirst()) {
                boolean exact =
                        record.recordHash.equals(safe(c.getString(0)))
                                && record.historyHash.equals(safe(c.getString(1)))
                                && record.snapshotHash.equals(safe(c.getString(2)))
                                && record.actionSpaceHash.equals(safe(c.getString(3)));
                if (!exact) throw new IllegalStateException("R36_CANONICAL_INTEGRITY_CONFLICT");
                return new WriteResult(
                        true,
                        true,
                        record.targetIssue,
                        record.recordHash,
                        record.commitAt,
                        "REPLAY_IDEMPOTENT");
            }
        } finally {
            if (c != null) c.close();
        }

        ContentValues cv = new ContentValues();
        cv.put("target_issue", record.targetIssue);
        cv.put("game_code", V51Repository.GAME_CODE);
        cv.put("base_issue", record.baseIssue);
        cv.put("position", record.position);
        cv.put("n1", record.n1);
        cv.put("n2", record.n2);
        cv.put("n3", record.n3);
        cv.put("selected_hit_p", record.selectedHitP);
        cv.put("predictability", record.predictability);
        cv.put("lane", record.lane);
        cv.put("decision_action", record.decisionAction);
        cv.put("reject_code", record.rejectCode);
        CanonicalReasonCP12.Receipt reasonReceipt = new CanonicalReasonCP12.Receipt(
                record.rejectCode, "CANONICAL:"+record.recordHash, record.targetIssue,
                record.attemptId, record.historyHash, record.createdAt, TimeSourceR811.monoNowMs());
        cv.put("canonical_reason_json", reasonReceipt.json);
        cv.put("canonical_reason_hash", reasonReceipt.hash);
        cv.put("bundle_hash", record.bundleHash);
        cv.put("contract_hash", record.contractHash);
        cv.put("authorization_hash", record.authorizationHash);
        cv.put("record_hash", record.recordHash);
        cv.put("pre_draw_frozen", 1);
        cv.put("model_proven", 0);
        cv.put("created_at", record.createdAt);
        cv.put("settled", 0);
        cv.put("actual_number", 0);
        cv.put("hit_label", -1);
        cv.put("settled_at", 0);
        cv.put("settlement_hash", "");
        cv.put("error_code", "");
        cv.put("analysis_attempt_id", record.attemptId);
        cv.put("history_hash", record.historyHash);
        cv.put("history_max_issue", record.baseIssue);
        cv.put("action_space_hash", record.actionSpaceHash);
        cv.put("snapshot_hash", record.snapshotHash);
        cv.put("request_at", record.requestAt);
        cv.put("commit_at", record.commitAt);
        cv.put("deadline_at", record.deadlineAt);
        cv.put("runtime_version", AppConfig.R36_RUNTIME_REPAIR_VERSION);
        long id = db.insertOrThrow(DbHelper.TABLE_R35_SHADOW_OBSERVATION, null, cv);
        if (id < 0L) throw new IllegalStateException("R36_CANONICAL_INSERT_FAILED");
        repairActivationBoundaryFromCanonicalWithinTransaction(db);
        setR811ActivationBoundaryOnceWithinTransaction(db);
        setR922ActivationBoundaryOnceWithinTransaction(db);
        return new WriteResult(
                true,
                false,
                record.targetIssue,
                record.recordHash,
                record.commitAt,
                "CANONICAL_COMMITTED");
    }

    /** Coverage begins at the first actually frozen R3.6 canonical base, not at a possibly stale
     * persisted draw that happened to be present when the service process started. */
    private static void repairActivationBoundaryFromCanonicalWithinTransaction(SQLiteDatabase db) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT base_issue FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE pre_draw_frozen=1 AND base_issue<>''"
                                    + " ORDER BY base_issue ASC LIMIT 1",
                            null);
            if (!c.moveToFirst()) return;
            String base = safe(c.getString(0));
            if (!TargetIssueResolver.isValid(base)) return;
            java.util.HashMap<String, String> states = new java.util.HashMap<String, String>();
            states.put("r36_activation_base_issue", base);
            states.put("r36_activation_at", String.valueOf(System.currentTimeMillis()));
            states.put("r367_activation_source", "FIRST_FROZEN_CANONICAL");
            DbStore.putStatesR36(db, states);
        } finally {
            if (c != null) c.close();
        }
    }

    /** R811 clean activation is immutable once the first R811 canonical has been committed. */
    private static void setR811ActivationBoundaryOnceWithinTransaction(SQLiteDatabase db) {
        Cursor present = null, first = null;
        try {
            present =
                    db.rawQuery(
                            "SELECT 1 FROM "
                                    + DbHelper.TABLE_APP_STATE
                                    + " WHERE state_key='r811_activation_base_issue' LIMIT 1",
                            null);
            if (present.moveToFirst()) return;
            first =
                    db.rawQuery(
                            "SELECT base_issue,target_issue,commit_at FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE pre_draw_frozen=1 AND runtime_version=?"
                                    + " ORDER BY commit_at ASC,target_issue ASC LIMIT 1",
                            new String[] {AppConfig.R36_RUNTIME_REPAIR_VERSION});
            if (!first.moveToFirst()) return;
            String base = safe(first.getString(0)), target = safe(first.getString(1));
            long at = first.getLong(2);
            if (!TargetIssueResolver.exact(base, target).valid || at <= 0L) return;
            insertStateOnce(db, "r811_activation_base_issue", base, at);
            insertStateOnce(db, "r811_activation_target_issue", target, at);
            insertStateOnce(db, "r811_activation_at", String.valueOf(at), at);
            insertStateOnce(db, "r811_activation_source", "FIRST_R811_FROZEN_CANONICAL", at);
        } finally {
            if (present != null) present.close();
            if (first != null) first.close();
        }
    }

    /**
     * R922 coverage has its own immutable cohort boundary.  It cannot inherit an R811 denominator
     * because opportunity-audit rows and the repaired queue policy did not exist in older builds.
     */
    private static void setR922ActivationBoundaryOnceWithinTransaction(SQLiteDatabase db) {
        Cursor present = null, first = null;
        try {
            present = db.rawQuery(
                    "SELECT 1 FROM " + DbHelper.TABLE_APP_STATE
                            + " WHERE state_key='r922_activation_base_issue' LIMIT 1",
                    null);
            if (present.moveToFirst()) return;
            first = db.rawQuery(
                    "SELECT base_issue,target_issue,commit_at FROM "
                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " WHERE pre_draw_frozen=1 AND runtime_version=?"
                            + " ORDER BY commit_at ASC,target_issue ASC LIMIT 1",
                    new String[] {AppConfig.R36_RUNTIME_REPAIR_VERSION});
            if (!first.moveToFirst()) return;
            String base = safe(first.getString(0)), target = safe(first.getString(1));
            long at = first.getLong(2);
            if (!TargetIssueResolver.exact(base, target).valid || at <= 0L) return;
            insertStateOnce(db, "r922_activation_base_issue", base, at);
            insertStateOnce(db, "r922_activation_target_issue", target, at);
            insertStateOnce(db, "r922_activation_at", String.valueOf(at), at);
            insertStateOnce(db, "r922_activation_version", AppConfig.R36_RUNTIME_REPAIR_VERSION, at);
            insertStateOnce(db, "r922_activation_source", "FIRST_R922_FROZEN_CANONICAL", at);
        } finally {
            if (present != null) present.close();
            if (first != null) first.close();
        }
    }

    private static void insertStateOnce(SQLiteDatabase db, String key, String value, long at) {
        db.execSQL(
                "INSERT OR IGNORE INTO "
                        + DbHelper.TABLE_APP_STATE
                        + "(state_key,state_value,updated_at) VALUES(?,?,?)",
                new Object[] {key, value, Long.valueOf(at)});
    }

    public static boolean hasCanonical(DbStore store, String targetIssue) {
        if (store == null || !TargetIssueResolver.isValid(targetIssue)) return false;
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT 1 FROM "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " WHERE target_issue=? AND pre_draw_frozen=1 LIMIT 1",
                                    new String[] {targetIssue});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    static boolean hasCanonicalInTransactionR36(SQLiteDatabase db, String targetIssue) {
        if (db == null || !db.inTransaction())
            throw new IllegalStateException("R36_CANONICAL_TX_REQUIRED");
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT 1 FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE target_issue=? AND pre_draw_frozen=1 LIMIT 1",
                            new String[] {targetIssue});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    public static CanonicalView readView(DbStore store, String targetIssue) {
        if (store == null || !TargetIssueResolver.isValid(targetIssue)) return null;
        Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT base_issue,target_issue,position,n1,n2,n3,"
                                        + "selected_hit_p,predictability,lane,decision_action,reject_code,created_at,settled,actual_number,hit_label,record_hash,snapshot_hash,action_space_hash,canonical_reason_json,canonical_reason_hash"
                                        + " FROM "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                            + " WHERE target_issue=? AND pre_draw_frozen=1 LIMIT 1",
                                    new String[] {targetIssue});
            if (!c.moveToFirst()) return null;
            return new CanonicalView(
                    c.getString(0),
                    c.getString(1),
                    c.getInt(2),
                    c.getInt(3),
                    c.getInt(4),
                    c.getInt(5),
                    c.getDouble(6),
                    c.getDouble(7),
                    c.getString(8),
                    c.getString(9),
                    c.getString(10),
                    c.getLong(11),
                    c.getInt(12),
                    c.getInt(13),
                    c.getInt(14),
                    c.getString(15),
                    c.getString(16),
                    c.getString(17), c.getString(18), c.getString(19));
        } finally {
            if (c != null) c.close();
        }
    }

    static CanonicalRecord fromRuntime(
            AnalysisSingleFlightR36.Request request,
            V51RuntimeResult result,
            DeferredEvidenceSnapshotR36.Header snapshot,
            String snapshotHash,
            long commitAt) {
        V51Models.PositionAction action = result.selected.action;
        String decisionAction = result.authorized.action.name();
        if ("BET".equals(decisionAction)
                && !V51CandidateRealGateR915.verifyAuthorized(
                        null, result.contract, result.authorized))
            throw new IllegalStateException("R915_CANONICAL_CONTROLLED_BET_INVALID");
        String recordHash =
                V51ChecksumEngine.sha256(
                        "R36_CANONICAL|"
                                + request.targetIssue
                                + '|'
                                + request.baseIssue
                                + '|'
                                + snapshot.historyHash
                                + '|'
                                + snapshot.actionSpaceHash
                                + '|'
                                + snapshotHash
                                + '|'
                                + action.checksum
                                + '|'
                                + result.authorized.authorizationHash);
        return new CanonicalRecord(
                request.baseIssue,
                request.targetIssue,
                action.position,
                action.n1,
                action.n2,
                action.n3,
                finite(result.selected.selectedHitP),
                finite(result.selected.predictability.probability),
                result.selected.lane.name(),
                decisionAction,
                result.authorized.rejectCode.name(),
                snapshot.bundleHash,
                snapshot.contractHash,
                result.authorized.authorizationHash,
                recordHash,
                request.attemptId(),
                snapshot.historyHash,
                snapshot.actionSpaceHash,
                snapshotHash,
                result.context.createdAt,
                request.requestAt,
                commitAt,
                request.deadlineAt);
    }

    static final class CanonicalRecord {
        final String baseIssue,
                targetIssue,
                lane,
                decisionAction,
                rejectCode,
                bundleHash,
                contractHash,
                authorizationHash,
                recordHash,
                attemptId,
                historyHash,
                actionSpaceHash,
                snapshotHash;
        final int position, n1, n2, n3;
        final double selectedHitP, predictability;
        final long createdAt, requestAt, commitAt, deadlineAt;

        CanonicalRecord(
                String base,
                String target,
                int position,
                int n1,
                int n2,
                int n3,
                double p,
                double predictability,
                String lane,
                String decision,
                String reject,
                String bundle,
                String contract,
                String auth,
                String record,
                String attempt,
                String history,
                String actionHash,
                String snapshot,
                long created,
                long request,
                long commit,
                long deadline) {
            baseIssue = safe(base);
            targetIssue = safe(target);
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            selectedHitP = p;
            this.predictability = predictability;
            this.lane = safe(lane);
            decisionAction = safe(decision);
            rejectCode = safe(reject);
            bundleHash = safe(bundle);
            contractHash = safe(contract);
            authorizationHash = safe(auth);
            recordHash = safe(record);
            attemptId = safe(attempt);
            historyHash = safe(history);
            actionSpaceHash = safe(actionHash);
            snapshotHash = safe(snapshot);
            createdAt = created;
            requestAt = request;
            commitAt = commit;
            deadlineAt = deadline;
        }

        void validate() {
            if (!TargetIssueResolver.exact(baseIssue, targetIssue).valid
                    || position < 1
                    || position > 10
                    || !DecisionGateEngine.b12K3(n1, n2, n3))
                throw new IllegalStateException("R36_CANONICAL_FIELDS_INVALID");
            if (recordHash.length() != 64
                    || historyHash.length() != 64
                    || actionSpaceHash.length() != 64
                    || snapshotHash.length() != 64)
                throw new IllegalStateException("R36_CANONICAL_HASH_INVALID");
            if (createdAt <= 0L
                    || requestAt <= 0L
                    || commitAt < requestAt)
                throw new IllegalStateException("R36_CANONICAL_TIME_OR_REAL_INVALID");
            if ("BET".equals(decisionAction)
                    && (!("CONTROLLED_PROBE".equals(lane))
                            || !"NONE".equals(rejectCode)
                            || authorizationHash.length() < 64))
                throw new IllegalStateException("R915_CANONICAL_CONTROLLED_FIELDS_INVALID");
        }
    }

    public static final class WriteResult {
        public final boolean success, replay;
        public final String targetIssue, recordHash, status;
        public final long commitAt;

        WriteResult(
                boolean success,
                boolean replay,
                String target,
                String hash,
                long commit,
                String status) {
            this.success = success;
            this.replay = replay;
            targetIssue = safe(target);
            recordHash = safe(hash);
            commitAt = commit;
            this.status = safe(status);
        }
    }

    public static final class CanonicalView {
        public final String baseIssue,
                targetIssue,
                lane,
                decisionAction,
                rejectCode,
                recordHash,
                snapshotHash,
                actionSpaceHash, reasonJson, reasonHash;
        public final int position, n1, n2, n3, settled, actualNumber, hitLabel;
        public final double selectedHitP, predictability;
        public final long createdAt;

        CanonicalView(
                String base,
                String target,
                int position,
                int n1,
                int n2,
                int n3,
                double p,
                double predict,
                String lane,
                String action,
                String reject,
                long created,
                int settled,
                int actual,
                int hit,
                String record,
                String snapshot,
                String actionHash) {
            baseIssue = safe(base);
            targetIssue = safe(target);
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            selectedHitP = p;
            predictability = predict;
            this.lane = safe(lane);
            decisionAction = safe(action);
            rejectCode = safe(reject);
            createdAt = created;
            this.settled = settled;
            actualNumber = actual;
            hitLabel = hit;
            recordHash = safe(record);
            snapshotHash = safe(snapshot);
            actionSpaceHash = safe(actionHash);
            reasonJson="";reasonHash="";
        }
        CanonicalView(
                String base,
                String target,
                int position,
                int n1,
                int n2,
                int n3,
                double p,
                double predict,
                String lane,
                String action,
                String reject,
                long created,
                int settled,
                int actual,
                int hit,
                String record,
                String snapshot,
                String actionHash, String reasonJson, String reasonHash) {
            baseIssue = safe(base);
            targetIssue = safe(target);
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            selectedHitP = p;
            predictability = predict;
            this.lane = safe(lane);
            decisionAction = safe(action);
            rejectCode = safe(reject);
            createdAt = created;
            this.settled = settled;
            actualNumber = actual;
            hitLabel = hit;
            recordHash = safe(record);
            snapshotHash = safe(snapshot);
            actionSpaceHash = safe(actionHash);
            this.reasonJson=safe(reasonJson);this.reasonHash=safe(reasonHash);
        }
    }

    private static double finite(double x) {
        return Double.isNaN(x) || Double.isInfinite(x) ? -1.0d : x;
    }

    private static String safe(String x) {
        return x == null ? "" : x.trim();
    }
}
