package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Post-draw label writer. It reads only the frozen R3.5 row and the actual draw; it never calls a
 * model, ranker, gate, calibration engine or predictor.
 */
public final class OosAfterDrawWriterR35 {
    private OosAfterDrawWriterR35() {}

    public static LabelResult settleFrozen(DbStore store, Models.Draw draw) {
        if (store == null) return fail("", "STORE_NULL");
        if (draw == null || !draw.isValid()) return fail("", "DRAW_INVALID");
        String target = safe(draw.issue);
        Cursor c = null;
        try {
            SQLiteDatabase db = store.writable();
            DbHelper.ensureR35Schema(db);
            c =
                    db.rawQuery(
                            "SELECT base_issue,position,n1,n2,n3,selected_hit_p,created_at,"
                                    + "record_hash,pre_draw_frozen,settled FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE target_issue=? LIMIT 1",
                            new String[] {target});
            if (!c.moveToFirst()) return fail(target, "FROZEN_OBSERVATION_MISSING");
            String base = safe(c.getString(0));
            int position = c.getInt(1), n1 = c.getInt(2), n2 = c.getInt(3), n3 = c.getInt(4);
            double probability = c.getDouble(5);
            long predictedAt = c.getLong(6);
            String frozenHash = safe(c.getString(7));
            int preDraw = c.getInt(8), alreadySettled = c.getInt(9);
            c.close();
            c = null;
            if (preDraw != 1) return fail(target, "NOT_PRE_DRAW_FROZEN");
            if (predictedAt <= 0L || (draw.openTime > 0L && predictedAt >= draw.openTime)) {
                return fail(target, "PIT_TIME_ORDER_INVALID");
            }
            if (position < 1 || position > 10 || !DecisionGateEngine.b12K3(n1, n2, n3)) {
                return fail(target, "FROZEN_K3_INVALID");
            }
            int actual = draw.getNumberAtPosition(position);
            if (!AppConfig.isValidNumber(actual))
                return fail(target, "ACTUAL_POSITION_NUMBER_INVALID");
            int hit = actual == n1 || actual == n2 || actual == n3 ? 1 : 0;
            long now = System.currentTimeMillis();
            String provenance =
                    V51ChecksumEngine.sha256(
                            "R35_OOS_PROVENANCE|"
                                    + frozenHash
                                    + "|"
                                    + target
                                    + "|"
                                    + draw.numbersText()
                                    + "|"
                                    + draw.openTime);
            String record = V51ChecksumEngine.sha256(provenance + "|" + actual + "|" + hit);
            double brier =
                    probability >= 0.0d && probability <= 1.0d
                            ? (probability - hit) * (probability - hit)
                            : -1.0d;
            ContentValues label = new ContentValues();
            label.put("target_issue", target);
            label.put("game_code", V51Repository.GAME_CODE);
            label.put("base_issue", base);
            label.put("position", position);
            label.put("n1", n1);
            label.put("n2", n2);
            label.put("n3", n3);
            label.put("selected_hit_p", probability);
            label.put("actual_number", actual);
            label.put("hit_label", hit);
            label.put("strict_oos", 1);
            label.put("pre_draw_frozen", 1);
            label.put("prediction_created_at", predictedAt);
            label.put("draw_open_time", Math.max(0L, draw.openTime));
            label.put("settled_at", now);
            label.put("brier", brier);
            label.put("provenance_hash", provenance);
            label.put("record_hash", record);
            label.put("error_code", "");
            long inserted =
                    db.insertWithOnConflict(
                            DbHelper.TABLE_R35_OOS_LABEL,
                            null,
                            label,
                            SQLiteDatabase.CONFLICT_IGNORE);
            ContentValues update = new ContentValues();
            update.put("settled", 1);
            update.put("actual_number", actual);
            update.put("hit_label", hit);
            update.put("settled_at", now);
            update.put("settlement_hash", record);
            update.put("error_code", "");
            int changed =
                    db.update(
                            DbHelper.TABLE_R35_SHADOW_OBSERVATION,
                            update,
                            "target_issue=? AND settled=0",
                            new String[] {target});
            boolean replay = inserted < 0L || alreadySettled == 1 || changed == 0;
            store.setState("r35_last_oos_issue", target);
            store.setState(
                    "r35_last_oos_status", replay ? "REPLAY_IDEMPOTENT" : "STRICT_OOS_COMMITTED");
            return new LabelResult(
                    true,
                    replay,
                    target,
                    hit,
                    probability,
                    brier,
                    replay ? "REPLAY_IDEMPOTENT" : "STRICT_OOS_COMMITTED",
                    record);
        } catch (OutOfMemoryError oom) {
            RuntimeHealthLedgerR35.recordOom(store, "OOS_AFTER_DRAW", target, oom);
            return fail(target, "OOM_CAUGHT_NO_MODEL_RETRY");
        } catch (Throwable e) {
            return fail(target, "LABEL_WRITE_ERROR:" + e.getClass().getSimpleName());
        } finally {
            if (c != null) c.close();
        }
    }

    private static LabelResult fail(String target, String status) {
        return new LabelResult(false, false, target, -1, -1.0d, -1.0d, status, "");
    }

    private static String safe(String x) {
        return x == null ? "" : x;
    }

    public static final class LabelResult {
        public final boolean success, replay;
        public final String targetIssue, status, recordHash;
        public final int hitLabel;
        public final double selectedHitP, brier;

        LabelResult(
                boolean success,
                boolean replay,
                String target,
                int hit,
                double p,
                double brier,
                String status,
                String hash) {
            this.success = success;
            this.replay = replay;
            targetIssue = safe(target);
            hitLabel = hit;
            selectedHitP = p;
            this.brier = brier;
            this.status = safe(status);
            recordHash = safe(hash);
        }

        public String summary() {
            return status
                    + "|target="
                    + targetIssue
                    + "|hit="
                    + hitLabel
                    + "|p="
                    + selectedHitP
                    + "|brier="
                    + brier
                    + "|replay="
                    + replay;
        }
    }
}
