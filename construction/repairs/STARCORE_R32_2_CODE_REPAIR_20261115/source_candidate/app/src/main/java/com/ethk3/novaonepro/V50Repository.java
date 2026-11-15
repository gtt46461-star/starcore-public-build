package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;
import java.util.Locale;

/**
 * Storage-only V50 repository. It owns no probability, rank, lane, authorization or BET decision.
 * All writes are checked and all prediction ledgers are append-only.
 */
public final class V50Repository {
    public static final class ReadyReport {
        public final boolean ready;
        public final DatabaseMigrationEngine.MigrationReport migration;
        public final DatabaseIntegrityVerifier.IntegrityReport integrity;

        ReadyReport(
                boolean ready,
                DatabaseMigrationEngine.MigrationReport migration,
                DatabaseIntegrityVerifier.IntegrityReport integrity) {
            this.ready = ready;
            this.migration = migration;
            this.integrity = integrity;
        }
    }

    public static final class WriteResult {
        public final boolean success;
        public final boolean idempotent;
        public final int affectedRows;
        public final V50Enums.FailureCode failureCode;
        public final String detail;

        WriteResult(
                boolean success,
                boolean idempotent,
                int affectedRows,
                V50Enums.FailureCode failureCode,
                String detail) {
            this.success = success;
            this.idempotent = idempotent;
            this.affectedRows = affectedRows;
            this.failureCode = failureCode == null ? V50Enums.FailureCode.UNKNOWN : failureCode;
            this.detail = detail == null ? "" : detail;
        }
    }

    public interface TransactionBody<T> {
        T run(SQLiteDatabase db) throws Exception;
    }

    private V50Repository() {}

    public static ReadyReport ensure(DbStore store) {
        if (store == null) {
            DatabaseMigrationEngine.MigrationReport migration =
                    DatabaseMigrationEngine.migrate(null);
            return new ReadyReport(false, migration, DatabaseIntegrityVerifier.verify(null));
        }
        SQLiteDatabase db = store.writable();
        DatabaseMigrationEngine.MigrationReport migration = DatabaseMigrationEngine.migrate(db);
        DatabaseIntegrityVerifier.IntegrityReport integrity =
                migration.success
                        ? DatabaseIntegrityVerifier.verify(db)
                        : DatabaseIntegrityVerifier.verify(db);
        return new ReadyReport(migration.success && integrity.valid, migration, integrity);
    }

    public static <T> T transaction(DbStore store, TransactionBody<T> body) throws Exception {
        if (store == null || body == null)
            throw new IllegalArgumentException("V50_TRANSACTION_INPUT_NULL");
        SQLiteDatabase db = store.writable();
        db.beginTransaction();
        try {
            T result = body.run(db);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    public static WriteResult insertIssuePrediction(
            SQLiteDatabase db,
            V50Models.PredictionContext context,
            String featureHash,
            String stateHash,
            String expertHash,
            double[][] numberProbability,
            List<V50Models.PositionAction> actions,
            V50Models.SelectedActionEnvelope selected,
            String modelHash,
            String hypothesisBlob) {
        if (db == null || context == null) {
            return fail(V50Enums.FailureCode.PREDICTION_INSERT_FAILED, "V50_ISSUE_INPUT_NULL");
        }
        if (!validProbabilityMatrix(numberProbability)) {
            return fail(
                    V50Enums.FailureCode.NUMBER_PROBABILITY_NOT_NORMALIZED,
                    "V50_NUMBER_MATRIX_INVALID");
        }
        if (actions == null || actions.size() != 10) {
            return fail(
                    V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                    "V50_POSITION_ACTION_SIZE=" + (actions == null ? 0 : actions.size()));
        }
        String targetIssue = safe(context.targetIssue);
        String contractHash = selected == null ? "" : safe(selected.contractHash);
        ExistingIssue existing = readExistingIssue(db, targetIssue);
        if (existing.exists) {
            if (existing.settled) {
                return fail(
                        V50Enums.FailureCode.PREDICTION_ALREADY_SETTLED,
                        "V50_PREDICTION_ALREADY_SETTLED:" + targetIssue);
            }
            if (contractHash.equals(existing.contractHash)) {
                return ok(true, 0, "V50_ISSUE_IDEMPOTENT");
            }
            recordLedgerError(
                    db,
                    targetIssue,
                    V50Enums.FailureCode.PREDICTION_HASH_CONFLICT,
                    "IssuePredictionLedgerStore",
                    "existing=" + existing.contractHash + " incoming=" + contractHash);
            return fail(
                    V50Enums.FailureCode.PREDICTION_HASH_CONFLICT,
                    "V50_PREDICTION_HASH_CONFLICT:" + targetIssue);
        }

        ContentValues cv = new ContentValues();
        cv.put("target_issue", targetIssue);
        cv.put("base_issue", safe(context.baseIssue));
        cv.put("history_max_issue", safe(context.historyMaxIssue));
        cv.put("created_at", context.createdAt);
        cv.put("history_hash", safe(context.historyHash));
        cv.put("feature_hash", safe(featureHash));
        cv.put("state_hash", safe(stateHash));
        cv.put("expert_hash", safe(expertHash));
        String probabilityHash = V50ChecksumEngine.hashMatrix("V50_NUMBER_P", numberProbability);
        cv.put("number_probability_hash", probabilityHash);
        cv.put("action_hash", actionHash(actions));
        cv.put("model_hash", safe(modelHash));
        cv.put("cohort_id", V50BuildManifest.COHORT_ID);
        cv.put("build_id", V50BuildManifest.BUILD_ID);
        cv.put("strategy_version", V50BuildManifest.STRATEGY_VERSION);
        cv.put("feature_schema", V50BuildManifest.FEATURE_SCHEMA);
        cv.put("state_schema", V50BuildManifest.STATE_SCHEMA);
        cv.put("expert_schema", V50BuildManifest.EXPERT_SCHEMA);
        cv.put("action_schema", V50BuildManifest.ACTION_SCHEMA);
        cv.put("number_probability_blob", encodeMatrix(numberProbability));
        cv.put("action_blob", encodeActions(actions));
        cv.put("hypothesis_blob", safe(hypothesisBlob));
        cv.put(
                "selected_position",
                selected == null || selected.action == null ? 0 : selected.action.position);
        cv.put("lane", selected == null ? V50Enums.Lane.SHADOW.name() : selected.lane.name());
        cv.put("authorization_state", "PREDRAW_FROZEN");
        cv.put("contract_hash", contractHash);
        cv.put("settled", 0);
        long row = db.insert(SchemaContractRegistry.TABLE_ISSUE, null, cv);
        if (row < 0L) {
            recordLedgerError(
                    db,
                    targetIssue,
                    V50Enums.FailureCode.PREDICTION_INSERT_FAILED,
                    "IssuePredictionLedgerStore",
                    "SQLite insert returned " + row);
            return fail(V50Enums.FailureCode.PREDICTION_INSERT_FAILED, "V50_ISSUE_INSERT_FAILED");
        }
        return ok(false, 1, "V50_ISSUE_INSERTED");
    }

    public static WriteResult insertNumberEvents(
            SQLiteDatabase db,
            String targetIssue,
            double[][] numberProbability,
            int[][] numberRanks,
            String[][] expertContribution,
            String[][] teacherContribution,
            String[] stateDomain,
            String featureHash) {
        if (db == null || !validProbabilityMatrix(numberProbability)) {
            return fail(
                    V50Enums.FailureCode.NUMBER_PROBABILITY_NOT_NORMALIZED,
                    "V50_NUMBER_EVENT_MATRIX_INVALID");
        }
        if (!matrix10(numberRanks)
                || !matrix10(expertContribution)
                || !matrix10(teacherContribution)
                || stateDomain == null
                || stateDomain.length != 10) {
            return fail(
                    V50Enums.FailureCode.NUMBER_EVENT_COUNT_NOT_100,
                    "V50_NUMBER_EVENT_AUXILIARY_SHAPE_INVALID");
        }
        int existing = count(db, SchemaContractRegistry.TABLE_NUMBER, targetIssue);
        if (existing == 100) return ok(true, 0, "V50_NUMBER_EVENTS_IDEMPOTENT");
        if (existing != 0) {
            recordLedgerError(
                    db,
                    targetIssue,
                    V50Enums.FailureCode.NUMBER_EVENT_COUNT_NOT_100,
                    "NumberEventLedgerStore",
                    "partial_existing=" + existing);
            return fail(
                    V50Enums.FailureCode.NUMBER_EVENT_COUNT_NOT_100,
                    "V50_NUMBER_EVENTS_PARTIAL_EXISTING=" + existing);
        }
        String probabilityHash = V50ChecksumEngine.hashMatrix("V50_NUMBER_P", numberProbability);
        int inserted = 0;
        for (int p = 0; p < 10; p++) {
            for (int n = 0; n < 10; n++) {
                ContentValues cv = new ContentValues();
                cv.put("target_issue", safe(targetIssue));
                cv.put("position", p + 1);
                cv.put("number", n + 1);
                cv.put("predicted_p", numberProbability[p][n]);
                cv.put("actual_label", -1);
                cv.put("number_rank", numberRanks[p][n]);
                cv.put("expert_contribution", safe(expertContribution[p][n]));
                cv.put("teacher_contribution", safe(teacherContribution[p][n]));
                cv.put("state_domain", safe(stateDomain[p]));
                cv.put("feature_hash", safe(featureHash));
                cv.put("probability_hash", probabilityHash);
                cv.put("settled", 0);
                long row = db.insert(SchemaContractRegistry.TABLE_NUMBER, null, cv);
                if (row < 0L) {
                    recordLedgerError(
                            db,
                            targetIssue,
                            V50Enums.FailureCode.NUMBER_EVENT_COUNT_NOT_100,
                            "NumberEventLedgerStore",
                            "failed_at=P" + (p + 1) + "N" + (n + 1));
                    return fail(
                            V50Enums.FailureCode.NUMBER_EVENT_COUNT_NOT_100,
                            "V50_NUMBER_EVENT_INSERT_FAILED_AT=" + inserted);
                }
                inserted++;
            }
        }
        return inserted == 100
                ? ok(false, inserted, "V50_NUMBER_EVENTS_INSERTED")
                : fail(
                        V50Enums.FailureCode.NUMBER_EVENT_COUNT_NOT_100,
                        "V50_NUMBER_EVENT_INSERTED=" + inserted);
    }

    public static WriteResult insertPositionActions(
            SQLiteDatabase db, String targetIssue, List<V50Models.PositionAction> actions) {
        if (db == null || actions == null || actions.size() != 10) {
            return fail(
                    V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                    "V50_ACTION_INPUT_SIZE=" + (actions == null ? 0 : actions.size()));
        }
        int existing = count(db, SchemaContractRegistry.TABLE_ACTION, targetIssue);
        if (existing == 10) return ok(true, 0, "V50_ACTIONS_IDEMPOTENT");
        if (existing != 0) {
            recordLedgerError(
                    db,
                    targetIssue,
                    V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                    "PositionActionLedgerStore",
                    "partial_existing=" + existing);
            return fail(
                    V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                    "V50_ACTION_PARTIAL_EXISTING=" + existing);
        }
        boolean[] positions = new boolean[10];
        int inserted = 0;
        for (int i = 0; i < actions.size(); i++) {
            V50Models.PositionAction action = actions.get(i);
            if (action == null
                    || !action.validFixedK3()
                    || action.position < 1
                    || action.position > 10
                    || positions[action.position - 1]
                    || !safe(targetIssue).equals(action.targetIssue)) {
                return fail(
                        V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                        "V50_ACTION_CONTRACT_INVALID_AT=" + i);
            }
            positions[action.position - 1] = true;
            ContentValues cv = new ContentValues();
            cv.put("target_issue", safe(targetIssue));
            cv.put("position", action.position);
            cv.put("n1", action.top3[0]);
            cv.put("n2", action.top3[1]);
            cv.put("n3", action.top3[2]);
            cv.put("raw_action_p", action.rawActionP);
            cv.put("third_p", action.thirdP);
            cv.put("third_margin", action.thirdMargin);
            cv.put("entropy", action.entropy);
            cv.put("effective_consensus", action.effectiveConsensus);
            cv.put("rank_score", action.rankScore);
            cv.put("rank_adjusted_p", action.rankAdjustedP);
            cv.put("action_rank", action.rank);
            cv.put("lifecycle", action.lifecycle.name());
            cv.put("state_domain", action.dynamicDomain);
            cv.put("fingerprint", action.fingerprint);
            cv.put("action_hash", action.checksum);
            cv.put("actual_hit", -1);
            cv.put("settled", 0);
            long row = db.insert(SchemaContractRegistry.TABLE_ACTION, null, cv);
            if (row < 0L) {
                recordLedgerError(
                        db,
                        targetIssue,
                        V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                        "PositionActionLedgerStore",
                        "failed_position=" + action.position);
                return fail(
                        V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                        "V50_ACTION_INSERT_FAILED_AT=" + inserted);
            }
            inserted++;
        }
        return inserted == 10
                ? ok(false, inserted, "V50_ACTIONS_INSERTED")
                : fail(
                        V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                        "V50_ACTION_INSERTED=" + inserted);
    }

    public static WriteResult insertAuthorizedDecision(
            SQLiteDatabase db, V50Models.AuthorizedDecision decision, boolean realTrade) {
        if (db == null
                || decision == null
                || decision.envelope == null
                || decision.envelope.action == null
                || decision.envelope.context == null) {
            return fail(
                    V50Enums.FailureCode.AUTH_CHAIN_INCOMPLETE,
                    "V50_AUTHORIZED_DECISION_INPUT_NULL");
        }
        V50PredictionContract.Result contract = V50PredictionContract.validate(decision.envelope);
        if (!contract.valid) {
            return fail(
                    V50Enums.FailureCode.AUTH_CHAIN_INCOMPLETE,
                    "V50_CONTRACT_INVALID:" + contract.detail);
        }
        if (decision.authorizationSignature.length() != 64
                || !V50BuildManifest.AUTHORIZATION_OWNER.equals(decision.authorizationOwner)) {
            return fail(
                    V50Enums.FailureCode.AUTH_CHAIN_INCOMPLETE,
                    "V50_AUTH_SIGNATURE_OR_OWNER_INVALID");
        }
        String targetIssue = decision.envelope.context.targetIssue;
        int existing = count(db, SchemaContractRegistry.TABLE_SELECTED, targetIssue);
        if (existing == 1) {
            Cursor current = null;
            try {
                current =
                        db.query(
                                SchemaContractRegistry.TABLE_SELECTED,
                                new String[] {
                                    "decision_id",
                                    "contract_hash",
                                    "authorization_signature",
                                    "decision_action"
                                },
                                "target_issue=?",
                                new String[] {targetIssue},
                                null,
                                null,
                                null,
                                "1");
                if (current.moveToFirst()
                        && decision.decisionId.equals(safe(current.getString(0)))
                        && decision.envelope.contractHash.equals(safe(current.getString(1)))
                        && decision.authorizationSignature.equals(safe(current.getString(2)))
                        && decision.action.name().equals(safe(current.getString(3)))) {
                    return ok(true, 0, "V50_SELECTED_IDEMPOTENT");
                }
            } finally {
                if (current != null) current.close();
            }
            return fail(
                    V50Enums.FailureCode.PREDICTION_HASH_CONFLICT,
                    "V50_SELECTED_HASH_CONFLICT:" + targetIssue);
        }
        if (existing > 1)
            return fail(
                    V50Enums.FailureCode.SELECTED_DECISION_COUNT_GT_1,
                    "V50_SELECTED_DUPLICATE=" + existing);
        V50Models.PositionAction action = decision.envelope.action;
        ContentValues cv = new ContentValues();
        cv.put("target_issue", targetIssue);
        cv.put("decision_id", decision.decisionId);
        cv.put("selected_position", action.position);
        cv.put("n1", action.top3[0]);
        cv.put("n2", action.top3[1]);
        cv.put("n3", action.top3[2]);
        cv.put("raw_action_p", action.rawActionP);
        cv.put("rank_adjusted_p", action.rankAdjustedP);
        cv.put("selected_hit_p", decision.envelope.selectedHitP);
        cv.put("uncertainty", decision.envelope.uncertainty);
        cv.put("domain_status", decision.envelope.domainStatus.name());
        cv.put("lane", decision.envelope.lane.name());
        cv.put("decision_action", decision.action.name());
        cv.put("primary_cause", decision.envelope.primaryCause);
        cv.put("secondary_cause", decision.envelope.secondaryCause);
        cv.put("third_cause", decision.envelope.thirdCause);
        cv.put("counter_evidence", decision.envelope.counterEvidence);
        cv.put("necessary_condition", decision.envelope.necessaryCondition);
        cv.put("falsifier_condition", decision.envelope.falsifierCondition);
        cv.put("contract_hash", decision.envelope.contractHash);
        cv.put("authorization_signature", decision.authorizationSignature);
        cv.put("authorization_owner", decision.authorizationOwner);
        cv.put("strategy_cohort", V50BuildManifest.COHORT_ID);
        cv.put("probability_owner", V50BuildManifest.SELECTED_PROBABILITY_OWNER);
        cv.put("rank_owner", V50BuildManifest.ACTION_RANK_OWNER);
        cv.put("lane_owner", V50BuildManifest.LANE_OWNER);
        cv.put("real_trade", realTrade ? 1 : 0);
        cv.put("settled", 0);
        cv.put("created_at", decision.authorizationTime);
        long row = db.insert(SchemaContractRegistry.TABLE_SELECTED, null, cv);
        if (row < 0L) return fail(V50Enums.FailureCode.SQLITE_ERROR, "V50_SELECTED_INSERT_FAILED");
        WriteResult auth = insertAuthorizationAudit(db, decision);
        if (!auth.success) return auth;
        return ok(false, 2, "V50_SELECTED_AND_AUTH_INSERTED");
    }

    public static WriteResult insertAuthorizationAudit(
            SQLiteDatabase db, V50Models.AuthorizedDecision decision) {
        if (db == null
                || decision == null
                || decision.envelope == null
                || decision.envelope.context == null) {
            return fail(V50Enums.FailureCode.AUTH_CHAIN_INCOMPLETE, "V50_AUTH_AUDIT_INPUT_NULL");
        }
        ContentValues cv = new ContentValues();
        cv.put("decision_id", decision.decisionId);
        cv.put("target_issue", decision.envelope.context.targetIssue);
        cv.put("contract_hash", decision.envelope.contractHash);
        cv.put("authorization_signature", decision.authorizationSignature);
        cv.put("authorization_owner", decision.authorizationOwner);
        cv.put("strategy_cohort", V50BuildManifest.COHORT_ID);
        cv.put("probability_owner", V50BuildManifest.SELECTED_PROBABILITY_OWNER);
        cv.put("rank_owner", V50BuildManifest.ACTION_RANK_OWNER);
        cv.put("lane_owner", V50BuildManifest.LANE_OWNER);
        cv.put("bet_owner", V50BuildManifest.BET_OWNER);
        cv.put("created_at", decision.authorizationTime);
        long row =
                db.insertWithOnConflict(
                        SchemaContractRegistry.TABLE_AUTH,
                        null,
                        cv,
                        SQLiteDatabase.CONFLICT_IGNORE);
        if (row < 0L) {
            Cursor c = null;
            try {
                c =
                        db.query(
                                SchemaContractRegistry.TABLE_AUTH,
                                new String[] {"contract_hash", "authorization_signature"},
                                "decision_id=?",
                                new String[] {decision.decisionId},
                                null,
                                null,
                                null,
                                "1");
                if (c.moveToFirst()
                        && decision.envelope.contractHash.equals(c.getString(0))
                        && decision.authorizationSignature.equals(c.getString(1))) {
                    return ok(true, 0, "V50_AUTH_IDEMPOTENT");
                }
            } finally {
                if (c != null) c.close();
            }
            return fail(V50Enums.FailureCode.AUTH_CHAIN_INCOMPLETE, "V50_AUTH_AUDIT_INSERT_FAILED");
        }
        return ok(false, 1, "V50_AUTH_AUDIT_INSERTED");
    }

    public static int count(SQLiteDatabase db, String table, String targetIssue) {
        if (db == null || table == null) return -1;
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT COUNT(*) FROM " + table + " WHERE target_issue=?",
                            new String[] {safe(targetIssue)});
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    public static void recordLedgerError(
            SQLiteDatabase db,
            String targetIssue,
            V50Enums.FailureCode code,
            String owner,
            String detail) {
        if (db == null) return;
        try {
            ContentValues cv = new ContentValues();
            cv.put("target_issue", safe(targetIssue));
            cv.put(
                    "failure_code",
                    code == null ? V50Enums.FailureCode.UNKNOWN.name() : code.name());
            cv.put("owner", safe(owner));
            cv.put("detail", safe(detail));
            cv.put("created_at", System.currentTimeMillis());
            cv.put("resolved", 0);
            db.insert(SchemaContractRegistry.TABLE_LEDGER_ERROR, null, cv);
        } catch (Throwable ignore) {
        }
    }

    private static ExistingIssue readExistingIssue(SQLiteDatabase db, String targetIssue) {
        Cursor c = null;
        try {
            c =
                    db.query(
                            SchemaContractRegistry.TABLE_ISSUE,
                            new String[] {"settled", "contract_hash"},
                            "target_issue=?",
                            new String[] {safe(targetIssue)},
                            null,
                            null,
                            null,
                            "1");
            if (c.moveToFirst())
                return new ExistingIssue(true, c.getInt(0) == 1, safe(c.getString(1)));
        } catch (Throwable ignore) {
        } finally {
            if (c != null) c.close();
        }
        return new ExistingIssue(false, false, "");
    }

    private static final class ExistingIssue {
        final boolean exists;
        final boolean settled;
        final String contractHash;

        ExistingIssue(boolean exists, boolean settled, String contractHash) {
            this.exists = exists;
            this.settled = settled;
            this.contractHash = contractHash;
        }
    }

    private static boolean validProbabilityMatrix(double[][] p) {
        if (p == null || p.length != 10) return false;
        for (int i = 0; i < 10; i++) {
            if (!V50PredictionContract.normalized10(p[i], 1.0e-8d)) return false;
        }
        return true;
    }

    private static boolean matrix10(int[][] values) {
        if (values == null || values.length != 10) return false;
        for (int i = 0; i < 10; i++) if (values[i] == null || values[i].length != 10) return false;
        return true;
    }

    private static boolean matrix10(String[][] values) {
        if (values == null || values.length != 10) return false;
        for (int i = 0; i < 10; i++) if (values[i] == null || values[i].length != 10) return false;
        return true;
    }

    private static String encodeMatrix(double[][] values) {
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < values.length; p++) {
            if (p > 0) sb.append(';');
            for (int n = 0; n < values[p].length; n++) {
                if (n > 0) sb.append(',');
                sb.append(String.format(Locale.US, "%.15f", values[p][n]));
            }
        }
        return sb.toString();
    }

    private static String encodeActions(List<V50Models.PositionAction> actions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actions.size(); i++) {
            V50Models.PositionAction a = actions.get(i);
            if (i > 0) sb.append(';');
            sb.append(a.position)
                    .append(':')
                    .append(a.top3[0])
                    .append(',')
                    .append(a.top3[1])
                    .append(',')
                    .append(a.top3[2])
                    .append(':')
                    .append(String.format(Locale.US, "%.15f", a.rawActionP))
                    .append(':')
                    .append(String.format(Locale.US, "%.15f", a.rankScore))
                    .append(':')
                    .append(a.rank)
                    .append(':')
                    .append(a.checksum);
        }
        return sb.toString();
    }

    private static String actionHash(List<V50Models.PositionAction> actions) {
        String[] hashes = new String[actions.size()];
        for (int i = 0; i < actions.size(); i++) hashes[i] = actions.get(i).checksum;
        return V50ChecksumEngine.hashStrings("V50_ACTIONS", hashes);
    }

    private static WriteResult ok(boolean idempotent, int rows, String detail) {
        return new WriteResult(true, idempotent, rows, V50Enums.FailureCode.NONE, detail);
    }

    private static WriteResult fail(V50Enums.FailureCode code, String detail) {
        return new WriteResult(false, false, 0, code, detail);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
