package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * Outcome-time owner for the immutable V50 prediction.
 *
 * <p>One transaction settles the 1/10/100 ledgers and updates only learning ledgers. Frozen
 * prediction probabilities, ranks, causes, lane and contract hashes are never changed after the
 * draw.
 */
public final class V50FullInformationLearningEngine {
    private static final int RECENT_WINDOW = 40;
    private static final double MAX_RESIDUAL = 0.18d;

    public V50Models.LearningReport settleLabels(DbStore store, Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid()) {
            return report(
                    draw == null ? "" : draw.issue,
                    false,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    V50Enums.FailureCode.TARGET_ISSUE_MISMATCH,
                    "V50_LEARNING_INPUT_INVALID");
        }
        V50Repository.ReadyReport ready = V50Repository.ensure(store);
        if (!ready.ready) {
            return report(
                    draw.issue,
                    false,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    V50Enums.FailureCode.MIGRATION_FAILED,
                    ready.integrity == null ? "V50_DATABASE_NOT_READY" : ready.integrity.detail);
        }

        final SQLiteDatabase db = store.writable();
        final String issue = draw.issue;
        LedgerReconciliationEngine.Report before = LedgerReconciliationEngine.reconcile(db, issue);
        if (!before.valid) {
            V50Repository.recordLedgerError(
                    db,
                    issue,
                    before.failureCode,
                    "V50FullInformationLearningEngine",
                    before.detail);
            return report(
                    issue,
                    false,
                    before.issueCount,
                    before.numberEventCount,
                    before.positionActionCount,
                    before.selectedDecisionCount,
                    0,
                    0,
                    before.failureCode,
                    before.detail);
        }
        if (isIssueSettled(db, issue)) {
            return report(
                    issue,
                    true,
                    1,
                    100,
                    10,
                    before.selectedDecisionCount,
                    0,
                    selectedHit(db, issue),
                    V50Enums.FailureCode.SETTLEMENT_ALREADY_APPLIED,
                    "V50_SETTLEMENT_IDEMPOTENT");
        }

        int[] actual = actualNumbers(draw);
        NumberSnapshot numbers = null;
        StateRow[] states = null;
        ActionRow[] actions = null;
        SelectedRow selected = null;
        int numberRows = 0;
        int actionRows = 0;
        int positionHits = 0;
        int selectedHit = 0;
        V50Enums.FailureCode failureCode = V50Enums.FailureCode.NONE;
        String failureDetail = "";

        db.beginTransaction();
        try {
            numbers = readNumbers(db, issue);
            states = readStates(db, issue);
            actions = readActions(db, issue);
            selected = readSelected(db, issue);
            if (numbers.count != 100) {
                throw failure(
                        V50Enums.FailureCode.NUMBER_EVENT_COUNT_NOT_100,
                        "V50_NUMBER_SNAPSHOT_COUNT=" + numbers.count);
            }
            if (states.length != 10) {
                throw failure(
                        V50Enums.FailureCode.SQLITE_ERROR,
                        "V50_STATE_SNAPSHOT_COUNT=" + states.length);
            }
            if (actions.length != 10) {
                throw failure(
                        V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                        "V50_ACTION_SNAPSHOT_COUNT=" + actions.length);
            }
            if (selected == null) {
                throw failure(
                        V50Enums.FailureCode.AUTH_CHAIN_INCOMPLETE,
                        "V50_SELECTED_DECISION_MISSING");
            }

            long settledAt = System.currentTimeMillis();
            for (int position = 1; position <= 10; position++) {
                int actualNumber = actual[position - 1];
                for (int number = 1; number <= 10; number++) {
                    ContentValues values = new ContentValues();
                    values.put("actual_label", number == actualNumber ? 1 : 0);
                    values.put("settled", 1);
                    values.put("settled_at", settledAt);
                    int changed =
                            db.update(
                                    SchemaContractRegistry.TABLE_NUMBER,
                                    values,
                                    "target_issue=? AND position=? AND number=?" + " AND settled=0",
                                    new String[] {
                                        issue, String.valueOf(position), String.valueOf(number)
                                    });
                    if (changed != 1) {
                        throw failure(
                                V50Enums.FailureCode.NUMBER_EVENT_COUNT_NOT_100,
                                "V50_NUMBER_LABEL_UPDATE_P"
                                        + position
                                        + "N"
                                        + number
                                        + '='
                                        + changed);
                    }
                    numberRows += changed;
                }
            }

            for (int i = 0; i < actions.length; i++) {
                ActionRow action = actions[i];
                int hit = action.contains(actual[action.position - 1]) ? 1 : 0;
                action.actualHit = hit;
                ContentValues values = new ContentValues();
                values.put("actual_hit", hit);
                values.put("settled", 1);
                values.put("settled_at", settledAt);
                int changed =
                        db.update(
                                SchemaContractRegistry.TABLE_ACTION,
                                values,
                                "target_issue=? AND position=? AND settled=0",
                                new String[] {issue, String.valueOf(action.position)});
                if (changed != 1) {
                    throw failure(
                            V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                            "V50_ACTION_LABEL_UPDATE_P" + action.position + '=' + changed);
                }
                actionRows += changed;
                positionHits += hit;
                if (action.position == selected.position) selectedHit = hit;
            }

            String result = selectedHit == 1 ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS;
            double pnl = selected.realTrade ? AppConfig.pnlForHit(selectedHit == 1) : 0.0d;
            ContentValues selectedValues = new ContentValues();
            selectedValues.put("settled", 1);
            selectedValues.put("result", result);
            selectedValues.put("pnl", pnl);
            selectedValues.put("settled_at", settledAt);
            int selectedRows =
                    db.update(
                            SchemaContractRegistry.TABLE_SELECTED,
                            selectedValues,
                            "target_issue=? AND settled=0",
                            new String[] {issue});
            if (selectedRows != 1) {
                throw failure(
                        V50Enums.FailureCode.SELECTED_DECISION_COUNT_GT_1,
                        "V50_SELECTED_SETTLEMENT_ROWS=" + selectedRows);
            }

            updateTeacher(db, issue, numbers, states, selected, actual);
            updateExpertRegret(db, numbers, states, actual);
            updateOnlineResidual(db, numbers, states, actual);
            V50Enums.AttributionType attribution =
                    updateAttribution(db, issue, actions, states, selected, actual, selectedHit);
            updateDomain(
                    db, issue, states[selected.position - 1], selected, selectedHit, attribution);

            ContentValues issueValues = new ContentValues();
            issueValues.put("settled", 1);
            issueValues.put("settled_at", settledAt);
            issueValues.put("actual_blob", drawBlob(actual));
            issueValues.put(
                    "learning_status",
                    "V50_FULL_INFORMATION_COMPLETE"
                            + "|numberRows="
                            + numberRows
                            + "|actionRows="
                            + actionRows
                            + "|positionHits="
                            + positionHits
                            + "|selectedHit="
                            + selectedHit
                            + "|attribution="
                            + attribution.name());
            int issueRows =
                    db.update(
                            SchemaContractRegistry.TABLE_ISSUE,
                            issueValues,
                            "target_issue=? AND settled=0",
                            new String[] {issue});
            if (issueRows != 1) {
                throw failure(
                        V50Enums.FailureCode.PREDICTION_INSERT_FAILED,
                        "V50_ISSUE_LABEL_UPDATE=" + issueRows);
            }
            db.setTransactionSuccessful();
        } catch (LearningFailure failure) {
            failureCode = failure.code;
            failureDetail = failure.getMessage();
        } catch (Throwable error) {
            failureCode = V50Enums.FailureCode.SQLITE_ERROR;
            failureDetail = error.getClass().getSimpleName() + ':' + safe(error.getMessage());
        } finally {
            try {
                db.endTransaction();
            } catch (Throwable ignore) {
            }
        }

        if (failureCode != V50Enums.FailureCode.NONE) {
            V50Repository.recordLedgerError(
                    db, issue, failureCode, "V50FullInformationLearningEngine", failureDetail);
            return report(
                    issue,
                    false,
                    1,
                    numberRows,
                    actionRows,
                    before.selectedDecisionCount,
                    positionHits,
                    selectedHit,
                    failureCode,
                    failureDetail);
        }
        LedgerReconciliationEngine.Report after = LedgerReconciliationEngine.reconcile(db, issue);
        if (!after.valid) {
            V50Repository.recordLedgerError(
                    db, issue, after.failureCode, "V50FullInformationLearningEngine", after.detail);
            return report(
                    issue,
                    false,
                    after.issueCount,
                    after.numberEventCount,
                    after.positionActionCount,
                    after.selectedDecisionCount,
                    positionHits,
                    selectedHit,
                    after.failureCode,
                    after.detail);
        }
        return report(
                issue,
                true,
                1,
                numberRows,
                actionRows,
                before.selectedDecisionCount,
                positionHits,
                selectedHit,
                V50Enums.FailureCode.NONE,
                "V50_FULL_INFORMATION_LEARNING_COMPLETE");
    }

    private static NumberSnapshot readNumbers(SQLiteDatabase db, String issue)
            throws LearningFailure {
        NumberSnapshot out = new NumberSnapshot();
        boolean[][] seen = new boolean[10][10];
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_NUMBER,
                            new String[] {
                                "position", "number", "predicted_p", "expert_contribution"
                            },
                            "target_issue=?",
                            new String[] {issue},
                            null,
                            null,
                            "position ASC,number ASC");
            while (cursor.moveToNext()) {
                int position = cursor.getInt(0);
                int number = cursor.getInt(1);
                if (position < 1
                        || position > 10
                        || number < 1
                        || number > 10
                        || seen[position - 1][number - 1]) {
                    throw failure(
                            V50Enums.FailureCode.NUMBER_EVENT_COUNT_NOT_100,
                            "V50_NUMBER_SNAPSHOT_KEY_INVALID");
                }
                seen[position - 1][number - 1] = true;
                out.count++;
                out.fused[position - 1][number - 1] = cursor.getDouble(2);
                parseExperts(out.experts, position, number, cursor.getString(3));
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return out;
    }

    private static StateRow[] readStates(SQLiteDatabase db, String issue) throws LearningFailure {
        StateRow[] rows = new StateRow[10];
        int count = 0;
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_STATE_HISTORY,
                            new String[] {
                                "position",
                                "prototype",
                                "lifecycle",
                                "domain_id",
                                "hazard",
                                "transition_risk",
                                "confidence"
                            },
                            "target_issue=?",
                            new String[] {issue},
                            null,
                            null,
                            "position ASC");
            while (cursor.moveToNext()) {
                int position = cursor.getInt(0);
                if (position < 1 || position > 10 || rows[position - 1] != null) {
                    throw failure(
                            V50Enums.FailureCode.SQLITE_ERROR, "V50_STATE_SNAPSHOT_KEY_INVALID");
                }
                StateRow row = new StateRow();
                row.position = position;
                row.prototype = safe(cursor.getString(1));
                row.lifecycle = lifecycle(cursor.getString(2));
                row.domain = safe(cursor.getString(3));
                row.hazard = cursor.getDouble(4);
                row.transitionRisk = cursor.getDouble(5);
                row.confidence = cursor.getDouble(6);
                rows[position - 1] = row;
                count++;
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        if (count != 10) {
            throw failure(V50Enums.FailureCode.SQLITE_ERROR, "V50_STATE_SNAPSHOT_COUNT=" + count);
        }
        return rows;
    }

    private static ActionRow[] readActions(SQLiteDatabase db, String issue) throws LearningFailure {
        ActionRow[] rows = new ActionRow[10];
        int count = 0;
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_ACTION,
                            new String[] {
                                "position",
                                "n1",
                                "n2",
                                "n3",
                                "raw_action_p",
                                "third_p",
                                "third_margin",
                                "entropy",
                                "effective_consensus",
                                "rank_score",
                                "rank_adjusted_p",
                                "action_rank",
                                "lifecycle",
                                "state_domain",
                                "fingerprint",
                                "action_hash"
                            },
                            "target_issue=?",
                            new String[] {issue},
                            null,
                            null,
                            "position ASC");
            while (cursor.moveToNext()) {
                int position = cursor.getInt(0);
                if (position < 1 || position > 10 || rows[position - 1] != null) {
                    throw failure(
                            V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                            "V50_ACTION_SNAPSHOT_KEY_INVALID");
                }
                ActionRow row = new ActionRow();
                row.issue = issue;
                row.position = position;
                row.top3 = new int[] {cursor.getInt(1), cursor.getInt(2), cursor.getInt(3)};
                row.rawP = cursor.getDouble(4);
                row.thirdP = cursor.getDouble(5);
                row.thirdMargin = cursor.getDouble(6);
                row.entropy = cursor.getDouble(7);
                row.consensus = cursor.getDouble(8);
                row.rankScore = cursor.getDouble(9);
                row.rankAdjustedP = cursor.getDouble(10);
                row.rank = cursor.getInt(11);
                row.lifecycle = lifecycle(cursor.getString(12));
                row.domain = safe(cursor.getString(13));
                row.fingerprint = safe(cursor.getString(14));
                row.checksum = safe(cursor.getString(15));
                if (!row.model().validFixedK3()) {
                    throw failure(
                            V50Enums.FailureCode.TOP3_INVALID,
                            "V50_ACTION_TOP3_INVALID_P" + position);
                }
                rows[position - 1] = row;
                count++;
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        if (count != 10) {
            throw failure(
                    V50Enums.FailureCode.POSITION_ACTION_COUNT_NOT_10,
                    "V50_ACTION_SNAPSHOT_COUNT=" + count);
        }
        return rows;
    }

    private static SelectedRow readSelected(SQLiteDatabase db, String issue) {
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_SELECTED,
                            new String[] {
                                "selected_position",
                                "selected_hit_p",
                                "uncertainty",
                                "decision_action",
                                "real_trade",
                                "primary_cause",
                                "secondary_cause",
                                "counter_evidence",
                                "contract_hash"
                            },
                            "target_issue=?",
                            new String[] {issue},
                            null,
                            null,
                            null,
                            "1");
            if (!cursor.moveToFirst()) return null;
            SelectedRow row = new SelectedRow();
            row.position = cursor.getInt(0);
            row.selectedHitP = cursor.getDouble(1);
            row.uncertainty = cursor.getDouble(2);
            row.decisionAction = safe(cursor.getString(3));
            row.realTrade = cursor.getInt(4) == 1;
            row.primaryCause = safe(cursor.getString(5));
            row.secondaryCause = safe(cursor.getString(6));
            row.counterEvidence = safe(cursor.getString(7));
            row.contractHash = safe(cursor.getString(8));
            return row.position < 1 || row.position > 10 ? null : row;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static void updateExpertRegret(
            SQLiteDatabase db, NumberSnapshot numbers, StateRow[] states, int[] actual) {
        V50ExpertRegretEngine engine = new V50ExpertRegretEngine();
        for (Map.Entry<String, double[][]> entry : numbers.experts.entrySet()) {
            String expertId = entry.getKey();
            double[][] probability = entry.getValue();
            for (int p = 0; p < 10; p++) {
                StateRow state = states[p];
                engine.update(
                        db,
                        expertId,
                        p + 1,
                        state.prototype,
                        state.lifecycle,
                        state.domain,
                        1.0d - FeatureContractRegistry.clamp01(state.confidence),
                        probability[p],
                        actual[p]);
            }
        }
    }

    private static void updateTeacher(
            SQLiteDatabase db,
            String issue,
            NumberSnapshot numbers,
            StateRow[] states,
            SelectedRow selected,
            int[] actual) {
        double[][] teacher = numbers.experts.get("TEACHER_HIERARCHICAL_POSTERIOR");
        for (int p = 0; p < 10; p++) {
            StateRow state = states[p];
            for (int n = 0; n < 10; n++) {
                int label = actual[p] == n + 1 ? 1 : 0;
                double predicted = teacher == null ? 0.1d : teacher[p][n];
                upsertTeacher(
                        db,
                        "POSITION_NUMBER",
                        "*",
                        V50Enums.Lifecycle.UNKNOWN.name(),
                        p + 1,
                        n + 1,
                        label,
                        predicted,
                        selected.realTrade);
                upsertTeacher(
                        db,
                        "DOMAIN_POSITION_NUMBER",
                        state.domain,
                        V50Enums.Lifecycle.UNKNOWN.name(),
                        p + 1,
                        n + 1,
                        label,
                        predicted,
                        selected.realTrade);
                upsertTeacher(
                        db,
                        "DOMAIN_LIFECYCLE_POSITION_NUMBER",
                        state.domain,
                        state.lifecycle.name(),
                        p + 1,
                        n + 1,
                        label,
                        predicted,
                        selected.realTrade);
            }
        }
    }

    private static void upsertTeacher(
            SQLiteDatabase db,
            String level,
            String domain,
            String lifecycle,
            int position,
            int number,
            int label,
            double predicted,
            boolean realTrade) {
        TeacherRow row = readTeacher(db, level, domain, lifecycle, position, number);
        int sample = row.sample + 1;
        int recentSample;
        int recentHits;
        if (row.recentSample < RECENT_WINDOW) {
            recentSample = row.recentSample + 1;
            recentHits = row.recentHits + label;
        } else {
            recentSample = RECENT_WINDOW;
            recentHits =
                    (int)
                            Math.round(
                                    row.recentHits * ((RECENT_WINDOW - 1.0d) / RECENT_WINDOW)
                                            + label);
        }
        int block = Math.min(3, row.sample / 50);
        boolean mislead = (predicted >= 0.13d && label == 0) || (predicted <= 0.07d && label == 1);
        ContentValues values = new ContentValues();
        values.put("sample", sample);
        values.put("hits", row.hits + label);
        values.put("recent_sample", recentSample);
        values.put("recent_hits", recentHits);
        values.put("oos_sample", row.oosSample + 1);
        values.put("real_sample", row.realSample + (realTrade ? 1 : 0));
        values.put("shadow_sample", row.shadowSample + (realTrade ? 0 : 1));
        for (int i = 0; i < 4; i++) {
            values.put("block" + i + "_sample", row.blockSample[i] + (i == block ? 1 : 0));
            values.put("block" + i + "_hits", row.blockHits[i] + (i == block ? label : 0));
        }
        values.put("mislead_count", row.mislead + (mislead ? 1 : 0));
        values.put("updated_at", System.currentTimeMillis());
        String where = teacherWhere();
        String[] args = teacherArgs(level, domain, lifecycle, position, number);
        int changed = db.update(SchemaContractRegistry.TABLE_TEACHER, values, where, args);
        if (changed == 0) {
            values.put("evidence_level", level);
            values.put("state_domain", safeOrStar(domain));
            values.put("lifecycle", safe(lifecycle));
            values.put("position", position);
            values.put("number", number);
            long inserted = db.insert(SchemaContractRegistry.TABLE_TEACHER, null, values);
            if (inserted < 0L) {
                throw new IllegalStateException("V50_TEACHER_INSERT_FAILED");
            }
        } else if (changed != 1) {
            throw new IllegalStateException("V50_TEACHER_UPDATE_ROWS=" + changed);
        }
    }

    private static TeacherRow readTeacher(
            SQLiteDatabase db,
            String level,
            String domain,
            String lifecycle,
            int position,
            int number) {
        TeacherRow row = new TeacherRow();
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_TEACHER,
                            new String[] {
                                "sample",
                                "hits",
                                "recent_sample",
                                "recent_hits",
                                "oos_sample",
                                "real_sample",
                                "shadow_sample",
                                "block0_sample",
                                "block0_hits",
                                "block1_sample",
                                "block1_hits",
                                "block2_sample",
                                "block2_hits",
                                "block3_sample",
                                "block3_hits",
                                "mislead_count"
                            },
                            teacherWhere(),
                            teacherArgs(level, domain, lifecycle, position, number),
                            null,
                            null,
                            null,
                            "1");
            if (!cursor.moveToFirst()) return row;
            row.sample = cursor.getInt(0);
            row.hits = cursor.getInt(1);
            row.recentSample = cursor.getInt(2);
            row.recentHits = cursor.getInt(3);
            row.oosSample = cursor.getInt(4);
            row.realSample = cursor.getInt(5);
            row.shadowSample = cursor.getInt(6);
            row.blockSample =
                    new int[] {
                        cursor.getInt(7), cursor.getInt(9), cursor.getInt(11), cursor.getInt(13)
                    };
            row.blockHits =
                    new int[] {
                        cursor.getInt(8), cursor.getInt(10), cursor.getInt(12), cursor.getInt(14)
                    };
            row.mislead = cursor.getInt(15);
            return row;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static void updateOnlineResidual(
            SQLiteDatabase db, NumberSnapshot numbers, StateRow[] states, int[] actual) {
        for (int p = 0; p < 10; p++) {
            StateRow state = states[p];
            for (int n = 0; n < 10; n++) {
                ResidualRow row =
                        readResidual(db, state.domain, state.lifecycle.name(), p + 1, n + 1);
                double label = actual[p] == n + 1 ? 1.0d : 0.0d;
                double error = label - numbers.fused[p][n];
                double rate = Math.min(0.02d, 0.10d / Math.sqrt(row.sample + 1.0d));
                double delta = clamp(row.delta + rate * error, -MAX_RESIDUAL, MAX_RESIDUAL);
                boolean misleading =
                        (row.delta > 0.0d && error < 0.0d) || (row.delta < 0.0d && error > 0.0d);
                int mislead = misleading ? row.mislead + 1 : 0;
                if (mislead >= 4) delta *= 0.5d;
                ContentValues values = new ContentValues();
                values.put("sample", row.sample + 1);
                values.put("delta", delta);
                values.put("mislead_run", mislead);
                values.put("updated_at", System.currentTimeMillis());
                String where = "domain_id=? AND lifecycle=?" + " AND position=? AND number=?";
                String[] args =
                        new String[] {
                            safeOrStar(state.domain),
                            state.lifecycle.name(),
                            String.valueOf(p + 1),
                            String.valueOf(n + 1)
                        };
                int changed =
                        db.update(
                                SchemaContractRegistry.TABLE_ONLINE_RESIDUAL, values, where, args);
                if (changed == 0) {
                    values.put("domain_id", safeOrStar(state.domain));
                    values.put("lifecycle", state.lifecycle.name());
                    values.put("position", p + 1);
                    values.put("number", n + 1);
                    long inserted =
                            db.insert(SchemaContractRegistry.TABLE_ONLINE_RESIDUAL, null, values);
                    if (inserted < 0L) {
                        throw new IllegalStateException("V50_RESIDUAL_INSERT_FAILED");
                    }
                } else if (changed != 1) {
                    throw new IllegalStateException("V50_RESIDUAL_UPDATE_ROWS=" + changed);
                }
            }
        }
    }

    private static ResidualRow readResidual(
            SQLiteDatabase db, String domain, String lifecycle, int position, int number) {
        ResidualRow row = new ResidualRow();
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_ONLINE_RESIDUAL,
                            new String[] {"sample", "delta", "mislead_run"},
                            "domain_id=? AND lifecycle=?" + " AND position=? AND number=?",
                            new String[] {
                                safeOrStar(domain),
                                safe(lifecycle),
                                String.valueOf(position),
                                String.valueOf(number)
                            },
                            null,
                            null,
                            null,
                            "1");
            if (cursor.moveToFirst()) {
                row.sample = cursor.getInt(0);
                row.delta = cursor.getDouble(1);
                row.mislead = cursor.getInt(2);
            }
            return row;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static V50Enums.AttributionType updateAttribution(
            SQLiteDatabase db,
            String issue,
            ActionRow[] actionRows,
            StateRow[] states,
            SelectedRow selected,
            int[] actual,
            int selectedHit) {
        V50Models.PositionAction[] actions = new V50Models.PositionAction[10];
        for (int i = 0; i < actionRows.length; i++) {
            actions[i] = actionRows[i].model();
        }
        V50Models.PositionAction rankOne = actions[selected.position - 1];
        String causeText = (selected.primaryCause + '|' + selected.secondaryCause).toUpperCase(java.util.Locale.ROOT);
        boolean teacherDominant = causeText.indexOf("TEACHER") >= 0;
        boolean relationDominant = causeText.indexOf("RELATION") >= 0;
        V50Enums.AttributionType type =
                new V50CauseResponsibilityEngine()
                        .classify(
                                rankOne,
                                actions,
                                actual[selected.position - 1],
                                states[selected.position - 1].transitionRisk >= 0.55d,
                                teacherDominant,
                                relationDominant,
                                selected.selectedHitP,
                                actual);
        String ablation = issueValue(db, issue, "hypothesis_blob");
        ContentValues values = new ContentValues();
        values.put("target_issue", issue);
        values.put("attribution_type", type.name());
        values.put("actual_number", actual[selected.position - 1]);
        values.put("selected_position", selected.position);
        values.put("selected_hit", selectedHit);
        values.put("ablation_blob", ablation);
        values.put(
                "responsibility_blob",
                "primary="
                        + selected.primaryCause
                        + "|secondary="
                        + selected.secondaryCause
                        + "|counter="
                        + selected.counterEvidence
                        + "|otherPositionHit="
                        + (otherPositionHit(actionRows, selected.position) ? 1 : 0));
        values.put("prediction_contract_hash", selected.contractHash);
        values.put("created_at", System.currentTimeMillis());
        long inserted = db.insert(SchemaContractRegistry.TABLE_ATTRIBUTION, null, values);
        if (inserted < 0L) {
            throw new IllegalStateException("V50_ATTRIBUTION_INSERT_FAILED");
        }
        return type;
    }

    private static void updateDomain(
            SQLiteDatabase db,
            String issue,
            StateRow state,
            SelectedRow selected,
            int selectedHit,
            V50Enums.AttributionType attribution) {
        DomainRow row = readDomain(db, state.domain);
        int sample = row.sample + 1;
        int block = Math.min(3, row.sample / 50);
        int currentLose = selectedHit == 1 ? 0 : row.currentLoseStreak + 1;
        int maxLose = Math.max(row.maxLoseStreak, currentLose);
        double calibrationEvent = Math.abs(selected.selectedHitP - selectedHit);
        double calibration = runningMean(row.calibrationError, row.sample, calibrationEvent);
        double causeEvent =
                attribution == V50Enums.AttributionType.STRUCTURAL_HIT
                        ? 1.0d
                        : (attribution == V50Enums.AttributionType.LUCKY_HIT ? 0.25d : 0.0d);
        boolean weakCounter =
                selected.counterEvidence.length() == 0
                        || selected.counterEvidence.indexOf("NONE") >= 0
                        || selected.counterEvidence.indexOf("WEAK") >= 0;
        double counterFailureEvent = selectedHit == 0 && weakCounter ? 1.0d : 0.0d;
        double transitionEvent =
                state.transitionRisk >= 0.55d ? (selectedHit == 0 ? 1.0d : 0.0d) : 0.5d;
        int recentSample;
        int recentHits;
        if (row.recentSample < RECENT_WINDOW) {
            recentSample = row.recentSample + 1;
            recentHits = row.recentHits + selectedHit;
        } else {
            recentSample = RECENT_WINDOW;
            recentHits =
                    (int)
                            Math.round(
                                    row.recentHits * ((RECENT_WINDOW - 1.0d) / RECENT_WINDOW)
                                            + selectedHit);
        }
        ContentValues values = new ContentValues();
        values.put("parent_prototype", state.prototype);
        values.put("lifecycle", state.lifecycle.name());
        values.put("position_scope", String.valueOf(selected.position));
        values.put(
                "prototype_vector",
                state.prototype + '|' + state.hazard + '|' + state.transitionRisk);
        values.put("sample", sample);
        values.put("recent_sample", recentSample);
        values.put("hit_count", row.hits + selectedHit);
        values.put("recent_hit_count", recentHits);
        for (int i = 0; i < 4; i++) {
            values.put("block" + i + "_sample", row.blockSample[i] + (i == block ? 1 : 0));
            values.put("block" + i + "_hits", row.blockHits[i] + (i == block ? selectedHit : 0));
        }
        values.put("rank1_sample", row.rank1Sample + 1);
        values.put("rank1_hits", row.rank1Hits + selectedHit);
        values.put("calibration_error", calibration);
        values.put("max_lose_streak", maxLose);
        values.put("current_lose_streak", currentLose);
        values.put("cause_validity", runningMean(row.causeValidity, row.sample, causeEvent));
        values.put(
                "counter_failure",
                runningMean(row.counterFailure, row.sample, counterFailureEvent));
        values.put(
                "transition_validity",
                runningMean(row.transitionValidity, row.sample, transitionEvent));
        values.put(
                "status",
                nextDomainStatus(
                                row,
                                sample,
                                row.hits + selectedHit,
                                calibration,
                                currentLose,
                                block,
                                selectedHit)
                        .name());
        values.put("updated_at", System.currentTimeMillis());
        int changed =
                db.update(
                        SchemaContractRegistry.TABLE_DOMAIN,
                        values,
                        "domain_id=?",
                        new String[] {safeOrStar(state.domain)});
        if (changed == 0) {
            values.put("domain_id", safeOrStar(state.domain));
            long inserted = db.insert(SchemaContractRegistry.TABLE_DOMAIN, null, values);
            if (inserted < 0L) {
                throw new IllegalStateException("V50_DOMAIN_INSERT_FAILED");
            }
        } else if (changed != 1) {
            throw new IllegalStateException("V50_DOMAIN_UPDATE_ROWS=" + changed);
        }
    }

    private static DomainRow readDomain(SQLiteDatabase db, String domain) {
        DomainRow row = new DomainRow();
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_DOMAIN,
                            new String[] {
                                "sample",
                                "recent_sample",
                                "hit_count",
                                "recent_hit_count",
                                "block0_sample",
                                "block0_hits",
                                "block1_sample",
                                "block1_hits",
                                "block2_sample",
                                "block2_hits",
                                "block3_sample",
                                "block3_hits",
                                "rank1_sample",
                                "rank1_hits",
                                "calibration_error",
                                "max_lose_streak",
                                "current_lose_streak",
                                "cause_validity",
                                "counter_failure",
                                "transition_validity",
                                "status"
                            },
                            "domain_id=?",
                            new String[] {safeOrStar(domain)},
                            null,
                            null,
                            null,
                            "1");
            if (!cursor.moveToFirst()) return row;
            row.sample = cursor.getInt(0);
            row.recentSample = cursor.getInt(1);
            row.hits = cursor.getInt(2);
            row.recentHits = cursor.getInt(3);
            row.blockSample =
                    new int[] {
                        cursor.getInt(4), cursor.getInt(6), cursor.getInt(8), cursor.getInt(10)
                    };
            row.blockHits =
                    new int[] {
                        cursor.getInt(5), cursor.getInt(7), cursor.getInt(9), cursor.getInt(11)
                    };
            row.rank1Sample = cursor.getInt(12);
            row.rank1Hits = cursor.getInt(13);
            row.calibrationError = cursor.getDouble(14);
            row.maxLoseStreak = cursor.getInt(15);
            row.currentLoseStreak = cursor.getInt(16);
            row.causeValidity = cursor.getDouble(17);
            row.counterFailure = cursor.getDouble(18);
            row.transitionValidity = cursor.getDouble(19);
            row.status = domainStatus(cursor.getString(20));
            return row;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static V50Enums.DomainStatus nextDomainStatus(
            DomainRow row,
            int sample,
            int hits,
            double calibration,
            int currentLose,
            int updatedBlock,
            int selectedHit) {
        if (row.status == V50Enums.DomainStatus.RETIRED) return row.status;
        if (currentLose >= 8) return V50Enums.DomainStatus.HARD_FREEZE;
        if (currentLose >= 5 || (sample >= 30 && calibration > 0.09d)) {
            return V50Enums.DomainStatus.SOFT_RISK;
        }
        if (row.status == V50Enums.DomainStatus.HARD_FREEZE
                || row.status == V50Enums.DomainStatus.SOFT_RISK) {
            return currentLose == 0 && row.recentSample >= 30
                    ? V50Enums.DomainStatus.RECOVERING
                    : row.status;
        }
        int qualifyingBlocks = 0;
        double minBlock = 1.0d;
        for (int i = 0; i < 4; i++) {
            int blockSample = row.blockSample[i] + (i == updatedBlock ? 1 : 0);
            int blockHits = row.blockHits[i] + (i == updatedBlock ? selectedHit : 0);
            if (blockSample < 12) continue;
            qualifyingBlocks++;
            minBlock = Math.min(minBlock, ((double) blockHits) / blockSample);
        }
        double low = wilsonLow(hits, sample);
        if (sample >= 160
                && qualifyingBlocks == 4
                && minBlock >= 0.37d
                && low >= 0.37d
                && calibration <= 0.04d) {
            return V50Enums.DomainStatus.PROVEN;
        }
        if (sample >= 80 && qualifyingBlocks >= 3 && low >= 0.34d && calibration <= 0.06d) {
            return V50Enums.DomainStatus.READY;
        }
        if (sample >= 30) return V50Enums.DomainStatus.WATCH;
        return V50Enums.DomainStatus.DISCOVERED;
    }

    private static double wilsonLow(int hits, int sample) {
        if (sample <= 0) return 0.0d;
        double z = 1.96d;
        double p = ((double) hits) / sample;
        double denominator = 1.0d + z * z / sample;
        double center = p + z * z / (2.0d * sample);
        double margin = z * Math.sqrt((p * (1.0d - p) + z * z / (4.0d * sample)) / sample);
        return Math.max(0.0d, (center - margin) / denominator);
    }

    private static void parseExperts(
            HashMap<String, double[][]> experts, int position, int number, String blob) {
        String[] entries = safe(blob).split(",");
        for (int i = 0; i < entries.length; i++) {
            int at = entries[i].indexOf('=');
            if (at <= 0 || at >= entries[i].length() - 1) continue;
            String id = entries[i].substring(0, at);
            double probability;
            try {
                probability = Double.parseDouble(entries[i].substring(at + 1));
            } catch (Throwable ignore) {
                continue;
            }
            double[][] matrix = experts.get(id);
            if (matrix == null) {
                matrix = new double[10][10];
                experts.put(id, matrix);
            }
            matrix[position - 1][number - 1] = FeatureContractRegistry.clamp01(probability);
        }
    }

    private static boolean isIssueSettled(SQLiteDatabase db, String issue) {
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_ISSUE,
                            new String[] {"settled"},
                            "target_issue=?",
                            new String[] {issue},
                            null,
                            null,
                            null,
                            "1");
            return cursor.moveToFirst() && cursor.getInt(0) == 1;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static int selectedHit(SQLiteDatabase db, String issue) {
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_SELECTED,
                            new String[] {"result"},
                            "target_issue=?",
                            new String[] {issue},
                            null,
                            null,
                            null,
                            "1");
            return cursor.moveToFirst() && AppConfig.isWinResult(cursor.getString(0)) ? 1 : 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static int[] actualNumbers(Models.Draw draw) {
        int[] actual = new int[10];
        for (int p = 1; p <= 10; p++) {
            int value = draw.getNumberAtPosition(p);
            if (value < 1 || value > 10) {
                throw new IllegalArgumentException("V50_DRAW_NUMBER_INVALID_P" + p + '=' + value);
            }
            actual[p - 1] = value;
        }
        return actual;
    }

    private static String drawBlob(int[] actual) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < actual.length; i++) {
            if (i > 0) out.append(',');
            out.append(actual[i]);
        }
        return out.toString();
    }

    private static String issueValue(SQLiteDatabase db, String issue, String column) {
        Cursor cursor = null;
        try {
            cursor =
                    db.query(
                            SchemaContractRegistry.TABLE_ISSUE,
                            new String[] {column},
                            "target_issue=?",
                            new String[] {issue},
                            null,
                            null,
                            null,
                            "1");
            return cursor.moveToFirst() ? safe(cursor.getString(0)) : "";
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static boolean otherPositionHit(ActionRow[] actions, int selectedPosition) {
        for (int i = 0; i < actions.length; i++) {
            if (actions[i].position != selectedPosition && actions[i].actualHit == 1) return true;
        }
        return false;
    }

    private static String teacherWhere() {
        return "evidence_level=? AND state_domain=? AND lifecycle=?"
                + " AND position=? AND number=?";
    }

    private static String[] teacherArgs(
            String level, String domain, String lifecycle, int position, int number) {
        return new String[] {
            safe(level),
            safeOrStar(domain),
            safe(lifecycle),
            String.valueOf(position),
            String.valueOf(number)
        };
    }

    private static V50Enums.Lifecycle lifecycle(String value) {
        try {
            return V50Enums.Lifecycle.valueOf(safe(value));
        } catch (Throwable ignore) {
            return V50Enums.Lifecycle.UNKNOWN;
        }
    }

    private static V50Enums.DomainStatus domainStatus(String value) {
        try {
            return V50Enums.DomainStatus.valueOf(safe(value));
        } catch (Throwable ignore) {
            return V50Enums.DomainStatus.DISCOVERED;
        }
    }

    private static double runningMean(double mean, int count, double value) {
        return count <= 0 ? value : (mean * count + value) / (count + 1.0d);
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(high, value));
    }

    private static LearningFailure failure(V50Enums.FailureCode code, String detail) {
        return new LearningFailure(code, detail);
    }

    private static V50Models.LearningReport report(
            String issue,
            boolean success,
            int issueRows,
            int numberRows,
            int actionRows,
            int selectedRows,
            int positionHits,
            int selectedHit,
            V50Enums.FailureCode failureCode,
            String detail) {
        String checksum =
                V50ChecksumEngine.sha256(
                        safe(issue)
                                + '|'
                                + success
                                + '|'
                                + issueRows
                                + '|'
                                + numberRows
                                + '|'
                                + actionRows
                                + '|'
                                + selectedRows
                                + '|'
                                + positionHits
                                + '|'
                                + selectedHit
                                + '|'
                                + failureCode
                                + '|'
                                + safe(detail));
        return new V50Models.LearningReport(
                issue,
                success,
                issueRows,
                numberRows,
                actionRows,
                selectedRows,
                positionHits,
                selectedHit,
                failureCode,
                detail,
                checksum);
    }

    private static final class NumberSnapshot {
        int count;
        final double[][] fused = new double[10][10];
        final HashMap<String, double[][]> experts = new HashMap<String, double[][]>();
    }

    private static final class StateRow {
        int position;
        String prototype = "";
        V50Enums.Lifecycle lifecycle = V50Enums.Lifecycle.UNKNOWN;
        String domain = "";
        double hazard;
        double transitionRisk;
        double confidence;
    }

    private static final class ActionRow {
        String issue;
        int position;
        int[] top3;
        double rawP;
        double thirdP;
        double thirdMargin;
        double entropy;
        double consensus;
        double rankScore;
        double rankAdjustedP;
        int rank;
        V50Enums.Lifecycle lifecycle;
        String domain;
        String fingerprint;
        String checksum;
        int actualHit;

        boolean contains(int number) {
            for (int i = 0; i < top3.length; i++) {
                if (top3[i] == number) return true;
            }
            return false;
        }

        V50Models.PositionAction model() {
            return new V50Models.PositionAction(
                    issue,
                    position,
                    top3,
                    rawP,
                    thirdP,
                    thirdMargin,
                    entropy,
                    consensus,
                    domain,
                    lifecycle,
                    new double[0],
                    rankScore,
                    rankAdjustedP,
                    rank,
                    fingerprint,
                    checksum);
        }
    }

    private static final class SelectedRow {
        int position;
        double selectedHitP;
        double uncertainty;
        String decisionAction = "";
        boolean realTrade;
        String primaryCause = "";
        String secondaryCause = "";
        String counterEvidence = "";
        String contractHash = "";
    }

    private static final class TeacherRow {
        int sample;
        int hits;
        int recentSample;
        int recentHits;
        int oosSample;
        int realSample;
        int shadowSample;
        int[] blockSample = new int[4];
        int[] blockHits = new int[4];
        int mislead;
    }

    private static final class ResidualRow {
        int sample;
        double delta;
        int mislead;
    }

    private static final class DomainRow {
        int sample;
        int recentSample;
        int hits;
        int recentHits;
        int[] blockSample = new int[4];
        int[] blockHits = new int[4];
        int rank1Sample;
        int rank1Hits;
        double calibrationError;
        int maxLoseStreak;
        int currentLoseStreak;
        double causeValidity;
        double counterFailure;
        double transitionValidity;
        V50Enums.DomainStatus status = V50Enums.DomainStatus.DISCOVERED;
    }

    private static final class LearningFailure extends Exception {
        final V50Enums.FailureCode code;

        LearningFailure(V50Enums.FailureCode code, String message) {
            super(message);
            this.code = code == null ? V50Enums.FailureCode.UNKNOWN : code;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String safeOrStar(String value) {
        String out = safe(value);
        return out.length() == 0 ? "*" : out;
    }
}
