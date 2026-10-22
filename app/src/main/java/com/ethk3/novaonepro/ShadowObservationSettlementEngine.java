package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Settles diagnostic observations independently from real trades and SHADOW accounting. */
public final class ShadowObservationSettlementEngine {
    public static final String MODULE_VERSION = "V48_DIAGNOSTIC_SETTLEMENT_20260721_48072148";

    private ShadowObservationSettlementEngine() {}

    public static int settleByDraw(DbStore store, Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid()) return 0;
        int changed = 0;
        Cursor c = null;
        try {
            SQLiteDatabase db = store.writable();
            c =
                    db.query(
                            DbHelper.TABLE_V48_DIAGNOSTIC_OBSERVATION,
                            new String[] {"_id", "position", "n1", "n2", "n3"},
                            "target_issue=? AND settled=0",
                            new String[] {safe(draw.issue)},
                            null,
                            null,
                            "_id ASC");
            while (c != null && c.moveToNext()) {
                long id = c.getLong(0);
                int p = c.getInt(1),
                        actual = draw.getNumberAtPosition(p),
                        hit =
                                (actual == c.getInt(2)
                                                || actual == c.getInt(3)
                                                || actual == c.getInt(4))
                                        ? 1
                                        : 0;
                ContentValues v = new ContentValues();
                v.put("settled", 1);
                v.put("actual_number", actual);
                v.put("hit", hit);
                v.put("result", hit == 1 ? AppConfig.RESULT_WIN : AppConfig.RESULT_MISS);
                v.put("settled_at", System.currentTimeMillis());
                changed +=
                        db.update(
                                DbHelper.TABLE_V48_DIAGNOSTIC_OBSERVATION,
                                v,
                                "_id=? AND settled=0",
                                new String[] {String.valueOf(id)});
            }
            store.setState("v48_diagnostic_settled_count", String.valueOf(changed));
            store.setState("v48_diagnostic_settlement_version", MODULE_VERSION);
        } catch (Throwable e) {
            try {
                store.setState(
                        "v48_diagnostic_settlement_error",
                        e.getClass().getSimpleName() + ":" + safe(e.getMessage()));
            } catch (Throwable ignore) {
            }
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        return changed;
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }

    public static final class V57ShadowReport {
        public final String targetIssue;
        public final int selectedRows;
        public final int shadowRows;
        public final int hit;
        public final int miss;
        public final int realPnlWrites;
        public final String checksum;

        V57ShadowReport(
                String targetIssue,
                int selectedRows,
                int shadowRows,
                int hit,
                int miss,
                int realPnlWrites) {
            this.targetIssue = safe(targetIssue);
            this.selectedRows = Math.max(0, selectedRows);
            this.shadowRows = Math.max(0, shadowRows);
            this.hit = Math.max(0, hit);
            this.miss = Math.max(0, miss);
            this.realPnlWrites = Math.max(0, realPnlWrites);
            this.checksum = V51ChecksumEngine.sha256(summary());
        }

        public boolean valid() {
            return realPnlWrites == 0;
        }

        public String summary() {
            return "target="
                    + targetIssue
                    + "|selected="
                    + selectedRows
                    + "|shadow="
                    + shadowRows
                    + "|hit="
                    + hit
                    + "|miss="
                    + miss
                    + "|ShadowRealPnlWrites="
                    + realPnlWrites;
        }
    }

    public static V57ShadowReport settleV51ShadowEvidence(DbStore store, Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid()) {
            return new V57ShadowReport("", 0, 0, 0, 0, 0);
        }
        SQLiteDatabase db = store.readable();
        Cursor c = null;
        int selected = 0;
        int shadow = 0;
        int hit = 0;
        int miss = 0;
        try {
            c =
                    db.rawQuery(
                            "SELECT s.decision_action,l.hit_label FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " s LEFT JOIN "
                                    + DbHelper.TABLE_R35_OOS_LABEL + " l ON l.target_issue=s.target_issue AND l.game_code=s.game_code"
                                    + " WHERE s.game_code=? AND s.target_issue=?",
                            new String[] {V51Repository.GAME_CODE, safe(draw.issue)});
            while (c.moveToNext()) {
                selected++;
                String mode = safe(c.getString(0));
                if (!"BET".equals(mode) && !c.isNull(1)) {
                    int label = c.getInt(1);
                    if (label != 0 && label != 1) throw new IllegalStateException("F02_SHADOW_LABEL_INVALID");
                    shadow++;
                    if (label == 1) hit++; else miss++;
                }
            }
        } catch (Throwable error) {
            try {
                store.setState(
                        "v57_b12_shadow_settlement_error",
                        error.getClass().getSimpleName() + ':' + safe(error.getMessage()));
            } catch (Throwable ignore) {
            }
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        return new V57ShadowReport(draw.issue, selected, shadow, hit, miss, 0);
    }

    public static boolean canWriteRealPnl() {
        return false;
    }

    public static boolean canCreateBet() {
        return false;
    }
}
