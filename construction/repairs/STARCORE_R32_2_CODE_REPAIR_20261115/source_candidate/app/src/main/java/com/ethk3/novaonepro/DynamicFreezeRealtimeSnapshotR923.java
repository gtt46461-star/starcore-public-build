package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;

/**
 * Materialized dynamic-freeze snapshot for the real-time prediction path.
 *
 * <p>The full matrix builder scans hundreds of settled REAL/SHADOW records and belongs to the
 * periodic diagnosis loop.  R923 persists that exact result atomically, while prediction performs
 * one indexed lookup across the candidate's eight keys.  This class has no BET authority: it only
 * returns the same freeze decision that the existing gate consumes.</p>
 */
public final class DynamicFreezeRealtimeSnapshotR923 {
    public static final String VERSION = "R923_FREEZE_RULE_SNAPSHOT_V1";
    public static final String SOURCE_TYPE = "R923_FREEZE_RULE_SNAPSHOT";
    private static final String TABLE = "dynamic_freeze_matrix";

    private DynamicFreezeRealtimeSnapshotR923() {}

    /** Replaces only the R923 materialization and keeps the prior good snapshot on build failure. */
    public static PersistResult persist(
            final DbStore store, final DynamicFreezeMatrixEngine.FreezeReport report) {
        if (store == null || report == null || report.ok != 1 || report.freezeRules == null)
            return new PersistResult(false, 0, "INVALID_FREEZE_REPORT");
        final long createdAt = report.createdAt > 0L ? report.createdAt : System.currentTimeMillis();
        Integer count = store.coordinatorR36().runWrite(
                "R923_MATERIALIZE_FREEZE_RULES",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    @Override public Integer call(SQLiteDatabase db) {
                        boolean owns = !db.inTransaction();
                        if (owns) db.beginTransaction();
                        try {
                            db.delete(TABLE, "source_type=?", new String[] {SOURCE_TYPE});
                            int written = 0;
                            for (DynamicFreezeMatrixEngine.FreezeRule rule : report.freezeRules) {
                                if (rule == null || safe(rule.freezeKey).length() == 0) continue;
                                ContentValues v = values(rule, createdAt, report);
                                long row = db.insertWithOnConflict(
                                        TABLE, null, v, SQLiteDatabase.CONFLICT_REPLACE);
                                if (row >= 0L) written++;
                            }
                            putState(db, "r923_freeze_snapshot_version", VERSION, createdAt);
                            putState(db, "r923_freeze_snapshot_at", String.valueOf(createdAt), createdAt);
                            putState(db, "r923_freeze_snapshot_rules", String.valueOf(written), createdAt);
                            putState(db, "r923_freeze_snapshot_status", "READY", createdAt);
                            if (owns) db.setTransactionSuccessful();
                            return Integer.valueOf(written);
                        } finally {
                            if (owns && db.inTransaction()) db.endTransaction();
                        }
                    }
                },
                Integer.valueOf(-1));
        int written = count == null ? -1 : count.intValue();
        return new PersistResult(written >= 0, Math.max(0, written),
                written >= 0 ? "READY" : "WRITE_FAILED");
    }

    /** One bounded indexed query; expired rules are ignored and never grant authority. */
    public static DynamicFreezeMatrixEngine.FreezeDecision check(
            DbStore store, Object candidate, String targetIssue) {
        DynamicFreezeMatrixEngine.FreezeDecision out = pass("SNAPSHOT_NOT_READY");
        if (store == null || candidate == null) return out;
        DynamicFreezeMatrixEngine.CandidateSnapshot snap =
                DynamicFreezeMatrixEngine.CandidateSnapshot.fromObject(candidate);
        ArrayList<String> keys = keys(snap);
        if (keys.isEmpty()) return pass("NO_CANDIDATE_FREEZE_KEY");

        StringBuilder sql = new StringBuilder(
                "SELECT freeze_key,freeze_type,freeze_level,freeze_reason,freeze_until_issue,"
                        + "freeze_until_time,shadow_review_count,recover_need_hits,"
                        + "recover_need_rate,recover_hint FROM " + TABLE
                        + " WHERE source_type=? AND frozen=1 AND freeze_key IN (");
        String[] args = new String[keys.size() + 1];
        args[0] = SOURCE_TYPE;
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) sql.append(',');
            sql.append('?');
            args[i + 1] = keys.get(i);
        }
        sql.append(')');

        Cursor c = null;
        Rule strongest = null;
        long now = System.currentTimeMillis();
        try {
            c = store.readable().rawQuery(sql.toString(), args);
            while (c.moveToNext()) {
                Rule one = new Rule();
                one.key = safe(c.getString(0));
                one.type = safe(c.getString(1));
                one.level = safe(c.getString(2));
                one.reason = safe(c.getString(3));
                one.untilIssue = safe(c.getString(4));
                one.untilTime = c.getLong(5);
                one.shadowReview = c.getInt(6);
                one.recoverHits = c.getInt(7);
                one.recoverRate = c.getDouble(8);
                one.recoverHint = safe(c.getString(9));
                if (expired(one, targetIssue, now)) continue;
                if (strongest == null || rank(one.level) > rank(strongest.level)) strongest = one;
            }
        } catch (Throwable ignored) {
            return pass("SNAPSHOT_READ_ERROR");
        } finally {
            if (c != null) c.close();
        }
        if (strongest == null) return pass("NO_ACTIVE_FREEZE_RULE");

        out.version = VERSION;
        out.createdAt = now;
        out.frozen = 1;
        out.freezeKey = strongest.key;
        out.freezeType = strongest.type;
        out.freezeLevel = strongest.level;
        out.freezeReason = strongest.reason;
        out.freezeUntilIssue = strongest.untilIssue;
        out.freezeUntilTime = strongest.untilTime;
        out.shadowReviewCount = strongest.shadowReview;
        out.recoverNeedHits = strongest.recoverHits;
        out.recoverNeedRate = strongest.recoverRate;
        out.recoverHint = strongest.recoverHint;
        if (DynamicFreezeMatrixEngine.LEVEL_HARD.equals(strongest.level)) {
            out.action = DynamicFreezeMatrixEngine.ACTION_FREEZE;
            out.downgradeToShadow = 1;
        } else if (DynamicFreezeMatrixEngine.LEVEL_SOFT.equals(strongest.level)
                || DynamicFreezeMatrixEngine.LEVEL_RECOVERING.equals(strongest.level)) {
            out.action = DynamicFreezeMatrixEngine.ACTION_DOWNGRADE_TO_SHADOW;
            out.downgradeToShadow = 1;
        } else {
            out.action = DynamicFreezeMatrixEngine.ACTION_WATCH;
            out.downgradeToShadow = 0;
        }
        return out;
    }

    private static ContentValues values(
            DynamicFreezeMatrixEngine.FreezeRule r, long createdAt,
            DynamicFreezeMatrixEngine.FreezeReport report) {
        ContentValues v = new ContentValues();
        v.put("freeze_key", safe(r.freezeKey));
        v.put("record_key", safe(r.freezeKey));
        v.put("source_type", SOURCE_TYPE);
        v.put("freeze_type", safe(r.freezeType));
        v.put("freeze_value", safe(r.freezeValue));
        v.put("freeze_level", safe(r.freezeLevel));
        v.put("created_at", createdAt);
        v.put("updated_at", System.currentTimeMillis());
        v.put("frozen", 1);
        v.put("freeze_until_issue", safe(r.freezeUntilIssue));
        v.put("freeze_until_time", r.freezeUntilTime);
        v.put("real_count", r.realCount);
        v.put("real_hit", r.realHit);
        v.put("real_miss", r.realMiss);
        v.put("real_hit_rate", r.realHitRate);
        v.put("shadow_review_count", r.shadowReviewCount);
        v.put("shadow_hit", r.shadowHit);
        v.put("shadow_miss", r.shadowMiss);
        v.put("shadow_hit_rate", r.shadowHitRate);
        v.put("recover_need_hits", r.recoverNeedHits);
        v.put("recover_need_rate", r.recoverNeedRate);
        v.put("freeze_reason", safe(r.freezeReason));
        v.put("recover_hint", safe(r.recoverHint));
        v.put("summary_text", safe(report.summary));
        v.put("detail_text", safe(r.fullText()));
        v.put("ext_json", "{\"version\":\"" + VERSION + "\",\"reliability\":"
                + format(r.reliability) + ",\"avgFinalP\":" + format(r.avgFinalP)
                + ",\"avgGate\":" + format(r.avgGate) + ",\"avgEdge\":"
                + format(r.avgEdge) + "}");
        return v;
    }

    private static void putState(SQLiteDatabase db, String key, String value, long now) {
        ContentValues v = new ContentValues();
        v.put("state_key", key);
        v.put("state_value", value);
        v.put("updated_at", now);
        db.insertWithOnConflict(DbHelper.TABLE_APP_STATE, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static ArrayList<String> keys(DynamicFreezeMatrixEngine.CandidateSnapshot c) {
        LinkedHashSet<String> unique = new LinkedHashSet<String>();
        if (c == null) return new ArrayList<String>();
        unique.add(DynamicFreezeMatrixEngine.FREEZE_POSITION + ":P" + c.position);
        unique.add(DynamicFreezeMatrixEngine.FREEZE_STATE + ":" + safe(c.windowBucket));
        unique.add(DynamicFreezeMatrixEngine.FREEZE_TEACHER + ":" + safe(c.teacherBucket));
        unique.add(DynamicFreezeMatrixEngine.FREEZE_V20 + ":" + safe(c.v20Bucket));
        unique.add(DynamicFreezeMatrixEngine.FREEZE_PROB_BUCKET + ":" + safe(c.finalPBucket));
        unique.add(DynamicFreezeMatrixEngine.FREEZE_EDGE_BUCKET + ":" + safe(c.edgeBucket));
        unique.add(DynamicFreezeMatrixEngine.FREEZE_K3_LAYER + ":" + safe(c.k3RankPattern));
        unique.add(DynamicFreezeMatrixEngine.FREEZE_COMBO_DOMAIN + ":" + safe(c.comboDomain));
        return new ArrayList<String>(unique);
    }

    private static boolean expired(Rule rule, String targetIssue, long now) {
        if (rule.untilTime > 0L && now > rule.untilTime) return true;
        if (TargetIssueResolver.isValid(targetIssue)
                && TargetIssueResolver.isValid(rule.untilIssue)
                && TargetIssueResolver.isAfter(targetIssue, rule.untilIssue)) return true;
        return false;
    }

    private static int rank(String level) {
        if (DynamicFreezeMatrixEngine.LEVEL_HARD.equals(level)) return 5;
        if (DynamicFreezeMatrixEngine.LEVEL_SOFT.equals(level)) return 4;
        if (DynamicFreezeMatrixEngine.LEVEL_RECOVERING.equals(level)) return 3;
        if (DynamicFreezeMatrixEngine.LEVEL_WATCH.equals(level)) return 2;
        return 0;
    }

    private static DynamicFreezeMatrixEngine.FreezeDecision pass(String reason) {
        DynamicFreezeMatrixEngine.FreezeDecision out =
                new DynamicFreezeMatrixEngine.FreezeDecision();
        out.version = VERSION;
        out.createdAt = System.currentTimeMillis();
        out.freezeReason = reason;
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String format(double value) {
        return String.format(Locale.US, "%.8f", Double.isFinite(value) ? value : 0.0d);
    }

    private static final class Rule {
        String key, type, level, reason, untilIssue, recoverHint;
        long untilTime;
        int shadowReview, recoverHits;
        double recoverRate;
    }

    public static final class PersistResult {
        public final boolean ok;
        public final int rules;
        public final String status;

        PersistResult(boolean okValue, int ruleCount, String statusValue) {
            ok = okValue;
            rules = Math.max(0, ruleCount);
            status = safe(statusValue);
        }

        public String canonical() {
            return VERSION + "|ok=" + (ok ? 1 : 0) + "|rules=" + rules + "|status=" + status;
        }
    }
}
