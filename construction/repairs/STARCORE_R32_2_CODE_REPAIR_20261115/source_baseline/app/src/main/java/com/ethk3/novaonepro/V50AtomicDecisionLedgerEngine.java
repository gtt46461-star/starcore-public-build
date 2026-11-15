package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.Arrays;

/**
 * One transaction freezes Issue/100 Number/10 Action/state/Selected/Auth rows. A failure rolls the
 * complete V50 prediction back.
 */
public final class V50AtomicDecisionLedgerEngine {
    public static final class Result {
        public final boolean success;
        public final boolean idempotent;
        public final V50Enums.FailureCode failureCode;
        public final String detail;
        public final LedgerReconciliationEngine.Report reconciliation;

        Result(
                boolean success,
                boolean idempotent,
                V50Enums.FailureCode failureCode,
                String detail,
                LedgerReconciliationEngine.Report reconciliation) {
            this.success = success;
            this.idempotent = idempotent;
            this.failureCode = failureCode == null ? V50Enums.FailureCode.UNKNOWN : failureCode;
            this.detail = detail == null ? "" : detail;
            this.reconciliation = reconciliation;
        }
    }

    public Result persist(
            final DbStore store,
            final V50Models.PredictionContext context,
            final V50Models.NumberFeatureSnapshot features,
            final V50Models.StateTrajectorySnapshot[] states,
            final V50MixtureOfExpertsEngine.FusionResult fusion,
            final V50Models.PositionAction[] actions,
            final V50Models.SelectedActionEnvelope selected,
            final V50Models.AuthorizedDecision authorized,
            final String hypothesisBlob,
            final Models.Decision legacyDecision) {
        return fail(V50Enums.FailureCode.PREDICTION_INSERT_FAILED,
                "CP4_LEGACY_V50_WRITER_RETIRED_USE_R36_CANONICAL",null);
    }

    private static boolean insertStates(
            SQLiteDatabase db, String issue, V50Models.StateTrajectorySnapshot[] states)
            throws WriteFailure {
        int existing = count(db, SchemaContractRegistry.TABLE_STATE_HISTORY, issue);
        if (existing == 10) return true;
        if (existing != 0) {
            throw new WriteFailure(
                    V50Enums.FailureCode.SQLITE_ERROR, "V50_STATE_HISTORY_PARTIAL=" + existing);
        }
        for (int i = 0; i < states.length; i++) {
            V50Models.StateTrajectorySnapshot state = states[i];
            if (state == null || state.position != i + 1) {
                throw new WriteFailure(
                        V50Enums.FailureCode.SQLITE_ERROR, "V50_STATE_HISTORY_INVALID_P" + (i + 1));
            }
            ContentValues cv = new ContentValues();
            cv.put("target_issue", issue);
            cv.put("position", state.position);
            cv.put(
                    "prototype",
                    state.prototypes.length == 0 ? "" : state.prototypes[0].prototypeId);
            cv.put("lifecycle", state.lifecycle.name());
            cv.put("domain_id", state.dynamicDomain);
            cv.put("coarse_run", state.coarseRunLength);
            cv.put("medium_run", state.mediumRunLength);
            cv.put("fine_run", state.fineRunLength);
            cv.put("hazard", state.hazard);
            cv.put("transition_risk", state.transitionRisk);
            cv.put("confidence", state.confidence);
            cv.put("state_hash", state.checksum);
            cv.put("created_at", System.currentTimeMillis());
            long row = db.insert(SchemaContractRegistry.TABLE_STATE_HISTORY, null, cv);
            if (row < 0L) {
                throw new WriteFailure(
                        V50Enums.FailureCode.SQLITE_ERROR,
                        "V50_STATE_HISTORY_INSERT_FAILED_P" + state.position);
            }
        }
        return false;
    }

    private static Auxiliary auxiliary(
            V50MixtureOfExpertsEngine.FusionResult fusion,
            V50Models.StateTrajectorySnapshot[] states) {
        Auxiliary out = new Auxiliary();
        for (int p = 0; p < 10; p++) {
            int[] order = order(fusion.numberProbability[p]);
            for (int rank = 0; rank < 10; rank++) out.ranks[p][order[rank]] = rank + 1;
            out.domains[p] = states[p].dynamicDomain;
            for (int n = 0; n < 10; n++) {
                StringBuilder expert = new StringBuilder();
                String teacher = "";
                for (int e = 0; e < fusion.experts.length; e++) {
                    V50Models.ExpertEvidence evidence = fusion.experts[e];
                    if (expert.length() > 0) expert.append(',');
                    expert.append(evidence.expertId)
                            .append('=')
                            .append(evidence.numberProbability[p][n]);
                    if ("TEACHER_HIERARCHICAL_POSTERIOR".equals(evidence.expertId)) {
                        teacher =
                                evidence.expertId
                                        + '='
                                        + evidence.numberProbability[p][n]
                                        + "|trust="
                                        + evidence.trust[p];
                    }
                }
                out.expertContribution[p][n] = expert.toString();
                out.teacherContribution[p][n] = teacher;
            }
        }
        return out;
    }

    private static int[] order(double[] probability) {
        int[] out = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                int left = out[i], right = out[j];
                if (probability[right] > probability[left]
                        || (probability[right] == probability[left] && right < left)) {
                    out[i] = right;
                    out[j] = left;
                }
            }
        }
        return out;
    }

    private static String stateHash(V50Models.StateTrajectorySnapshot[] states) {
        String[] values = new String[states.length];
        for (int i = 0; i < states.length; i++)
            values[i] = states[i] == null ? "" : states[i].checksum;
        return V50ChecksumEngine.hashStrings("V50_STATES", values);
    }

    private static String expertHash(V50Models.ExpertEvidence[] experts) {
        String[] values = new String[experts == null ? 0 : experts.length];
        for (int i = 0; i < values.length; i++)
            values[i] = experts[i] == null ? "" : experts[i].checksum;
        return V50ChecksumEngine.hashStrings("V50_EXPERTS", values);
    }

    private static int count(SQLiteDatabase db, String table, String issue) {
        Cursor c = null;
        try {
            c =
                    db.rawQuery(
                            "SELECT COUNT(*) FROM " + table + " WHERE target_issue=?",
                            new String[] {issue});
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static void require(V50Repository.WriteResult result) throws WriteFailure {
        if (result == null || !result.success) {
            throw new WriteFailure(
                    result == null ? V50Enums.FailureCode.UNKNOWN : result.failureCode,
                    result == null ? "V50_WRITE_RESULT_NULL" : result.detail);
        }
    }

    private static Result fail(
            V50Enums.FailureCode code,
            String detail,
            LedgerReconciliationEngine.Report reconciliation) {
        return new Result(false, false, code, detail, reconciliation);
    }

    private static final class Auxiliary {
        final int[][] ranks = new int[10][10];
        final String[][] expertContribution = new String[10][10];
        final String[][] teacherContribution = new String[10][10];
        final String[] domains = new String[10];
    }

    private static final class WriteFailure extends Exception {
        final V50Enums.FailureCode code;

        WriteFailure(V50Enums.FailureCode code, String message) {
            super(message);
            this.code = code == null ? V50Enums.FailureCode.UNKNOWN : code;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
