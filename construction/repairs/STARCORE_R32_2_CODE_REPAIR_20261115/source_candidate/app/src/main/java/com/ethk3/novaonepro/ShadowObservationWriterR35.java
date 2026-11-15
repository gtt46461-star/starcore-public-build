package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Freezes exactly one pre-draw observation per target issue. */
public final class ShadowObservationWriterR35 {
    private ShadowObservationWriterR35() {}

    public static WriteResult writeFrozen(DbStore store, V51RuntimeResult result) {
        if (store == null
                || result == null
                || !result.success
                || result.context == null
                || result.selected == null
                || result.selected.action == null
                || result.authorized == null) {
            return new WriteResult(false, false, "", "INVALID_RUNTIME_RESULT", "");
        }
        V51Models.PositionAction a = result.selected.action;
        String target = safe(result.context.targetIssue);
        String base = safe(result.context.baseIssue);
        if (target.length() == 0 || base.length() == 0 || target.compareTo(base) <= 0) {
            return new WriteResult(false, false, target, "PIT_ISSUE_ORDER_INVALID", "");
        }
        if (!DecisionGateEngine.b12K3(a.n1, a.n2, a.n3) || a.position < 1 || a.position > 10) {
            return new WriteResult(false, false, target, "FIXED_K3_INVALID", "");
        }
        long now = System.currentTimeMillis();
        double selectedP =
                finiteProbability(result.selected.selectedHitP)
                        ? result.selected.selectedHitP
                        : -1.0d;
        String contract = safe(result.selected.contractHash);
        String authorization = safe(result.authorized.authorizationHash);
        String record =
                V51ChecksumEngine.sha256(
                        "R35_SHADOW|"
                                + V51Repository.GAME_CODE
                                + "|"
                                + target
                                + "|"
                                + base
                                + "|"
                                + a.position
                                + "|"
                                + a.n1
                                + "|"
                                + a.n2
                                + "|"
                                + a.n3
                                + "|"
                                + selectedP
                                + "|"
                                + contract
                                + "|"
                                + authorization);
        Cursor c = null;
        try {
            SQLiteDatabase db = store.writable();
            DbHelper.ensureR35Schema(db);
            c =
                    db.rawQuery(
                            "SELECT record_hash FROM "
                                    + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                    + " WHERE target_issue=? LIMIT 1",
                            new String[] {target});
            if (c.moveToFirst()) {
                String old = safe(c.getString(0));
                boolean same = old.equals(record);
                return new WriteResult(
                        same,
                        true,
                        target,
                        same
                                ? "REPLAY_IDEMPOTENT"
                                : "IMMUTABLE_FIRST_WRITE_PRESERVED_HASH_CONFLICT",
                        old);
            }
            c.close();
            c = null;
            ContentValues cv = new ContentValues();
            cv.put("target_issue", target);
            cv.put("game_code", V51Repository.GAME_CODE);
            cv.put("base_issue", base);
            cv.put("position", a.position);
            cv.put("n1", a.n1);
            cv.put("n2", a.n2);
            cv.put("n3", a.n3);
            cv.put("selected_hit_p", selectedP);
            cv.put(
                    "predictability",
                    result.selected.predictability == null
                            ? 0.0d
                            : result.selected.predictability.probability);
            cv.put("lane", result.selected.lane == null ? "SHADOW" : result.selected.lane.name());
            cv.put(
                    "decision_action",
                    result.authorized.action == null ? "SHADOW" : result.authorized.action.name());
            cv.put(
                    "reject_code",
                    result.authorized.rejectCode == null
                            ? "MODEL_NOT_PROVEN"
                            : result.authorized.rejectCode.name());
            cv.put("bundle_hash", V51ArtifactBundleManifest.BUNDLE_HASH);
            cv.put("contract_hash", contract);
            cv.put("authorization_hash", authorization);
            cv.put("record_hash", record);
            cv.put("pre_draw_frozen", 1);
            cv.put("model_proven", V51BuildManifest.MODEL_PROVEN ? 1 : 0);
            cv.put("created_at", result.context.createdAt > 0L ? result.context.createdAt : now);
            long row =
                    db.insertWithOnConflict(
                            DbHelper.TABLE_R35_SHADOW_OBSERVATION,
                            null,
                            cv,
                            SQLiteDatabase.CONFLICT_IGNORE);
            if (row < 0L) return new WriteResult(false, false, target, "INSERT_FAILED", record);
            store.setState("r35_canonical_target_issue", target);
            store.setState("r35_shadow_observation_status", "FROZEN_FIRST_WRITE");
            store.setState("r35_shadow_observation_hash", record);
            return new WriteResult(true, false, target, "FROZEN_FIRST_WRITE", record);
        } catch (Throwable e) {
            return new WriteResult(
                    false, false, target, "WRITE_ERROR:" + e.getClass().getSimpleName(), record);
        } finally {
            if (c != null) c.close();
        }
    }

    public static boolean hasCanonical(DbStore store, String targetIssue) {
        return count(
                        store,
                        "SELECT COUNT(1) FROM "
                                + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                                + " WHERE target_issue=? AND pre_draw_frozen=1",
                        new String[] {safe(targetIssue)})
                == 1;
    }

    public static int recentCanonicalCount(DbStore store, int limit) {
        return count(
                store,
                "SELECT COUNT(1) FROM (SELECT target_issue FROM "
                        + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                        + " WHERE pre_draw_frozen=1 ORDER BY created_at DESC LIMIT "
                        + Math.max(1, limit)
                        + ")",
                null);
    }

    private static int count(DbStore store, String sql, String[] args) {
        if (store == null) return 0;
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            DbHelper.ensureR35Schema(db);
            c = db.rawQuery(sql, args);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (Throwable e) {
            return 0;
        } finally {
            if (c != null) c.close();
        }
    }

    private static boolean finiteProbability(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0.0d && value <= 1.0d;
    }

    private static String safe(String x) {
        return x == null ? "" : x;
    }

    public static final class WriteResult {
        public final boolean success, replay;
        public final String targetIssue, status, recordHash;

        WriteResult(boolean success, boolean replay, String target, String status, String hash) {
            this.success = success;
            this.replay = replay;
            targetIssue = safe(target);
            this.status = safe(status);
            recordHash = safe(hash);
        }

        public String summary() {
            return status + "|target=" + targetIssue + "|replay=" + replay;
        }
    }
}
