package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Owner-requested exploration lane: about 30% of frozen pre-draw observations are shown as
 * "出手（探索·未经验证）". The decision is made once, inside the same transaction that freezes
 * the canonical observation, from pre-draw data only, and is never revised after the draw.
 *
 * <p>It is a display/statistics lane, not DecisionGateEngine REAL authority: it creates no
 * trade, receipt, certificate or settlement of its own. Hits are read from the existing
 * SHADOW settlement of the same frozen numbers. A certified REAL decision (BET) is always
 * shown as REAL and counts as an action. The frequency target is a quota by design; it does
 * not make the selected numbers more likely to hit.
 */
final class ExplorationLaneR83 {
    static final String TABLE = "v57_exploration_r83";
    static final double TARGET_RATE = 0.30;
    static final int SCORE_WINDOW = 100, RATE_WINDOW = 20, STATS_WINDOW = 200;
    static final String STATE_ISSUE = "r83_explore_issue", STATE_ACTION = "r83_explore_action",
            STATE_STATS = "r83_explore_stats";
    private ExplorationLaneR83() {}

    static void ensure(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + "(target_issue TEXT PRIMARY KEY,position INTEGER NOT NULL,"
                + "n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,score REAL NOT NULL,threshold REAL NOT NULL,"
                + "history_n INTEGER NOT NULL,recent_rate REAL NOT NULL,action TEXT NOT NULL,mode TEXT NOT NULL,decided_at INTEGER NOT NULL)");
    }

    /** Called by CanonicalObservationWriterR36 after the canonical insert, same transaction. */
    static void decideWithinTransaction(SQLiteDatabase db, String targetIssue, int position, int n1, int n2, int n3,
            double selectedHitP, double predictability, String decisionAction) {
        ensure(db);
        try (Cursor exists = db.rawQuery("SELECT 1 FROM " + TABLE + " WHERE target_issue=?", new String[] {targetIssue})) {
            if (exists.moveToFirst()) return;
        }
        double score = valid(selectedHitP) ? selectedHitP : (valid(predictability) ? predictability : 0.0d);
        ArrayList<Double> scores = new ArrayList<Double>();
        int recent = 0, recentActions = 0;
        try (Cursor c = db.rawQuery("SELECT score,action FROM " + TABLE + " WHERE target_issue<? ORDER BY target_issue DESC LIMIT "
                + SCORE_WINDOW, new String[] {targetIssue})) {
            while (c.moveToNext()) {
                scores.add(c.getDouble(0));
                if (recent < RATE_WINDOW) {
                    recent++;
                    if (!"NONE".equals(c.getString(1))) recentActions++;
                }
            }
        }
        double rate = recent == 0 ? 0.0d : recentActions / (double) recent;
        double threshold = quantile(scores, 1.0d - TARGET_RATE);
        String mode, action;
        if ("BET".equals(decisionAction)) {
            mode = "CERTIFIED_REAL";
            action = "REAL";
        } else if (scores.size() < 10) {
            // Warm-up: no score distribution yet; keep the realized frequency at the target.
            mode = "WARMUP_QUOTA";
            action = recentActions < Math.ceil(TARGET_RATE * (recent + 1)) ? "EXPLORE" : "NONE";
        } else {
            mode = "SCORE_QUANTILE";
            boolean strong = score >= threshold;
            action = (strong && rate < TARGET_RATE + 0.10d) || rate < TARGET_RATE - 0.10d ? "EXPLORE" : "NONE";
        }
        android.content.ContentValues cv = new android.content.ContentValues();
        cv.put("target_issue", targetIssue);
        cv.put("position", position);
        cv.put("n1", n1);
        cv.put("n2", n2);
        cv.put("n3", n3);
        cv.put("score", score);
        cv.put("threshold", Double.isNaN(threshold) ? -1.0d : threshold);
        cv.put("history_n", scores.size());
        cv.put("recent_rate", rate);
        cv.put("action", action);
        cv.put("mode", mode);
        cv.put("decided_at", System.currentTimeMillis());
        if (db.insert(TABLE, null, cv) < 0L) return;
        HashMap<String, String> states = new HashMap<String, String>();
        states.put(STATE_ISSUE, targetIssue);
        states.put(STATE_ACTION, action);
        states.put(STATE_STATS, stats(db));
        DbStore.putStatesR36(db, states);
    }

    /** Rolling statistics joined with the existing SHADOW settlement of the same frozen row. */
    static String stats(SQLiteDatabase db) {
        int total = 0, actions = 0, settled = 0, hits = 0;
        try (Cursor c = db.rawQuery("SELECT e.action,COALESCE(o.settled,0),COALESCE(o.hit_label,-1) FROM (SELECT * FROM " + TABLE
                + " ORDER BY target_issue DESC LIMIT " + STATS_WINDOW + ") e LEFT JOIN " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                + " o ON o.target_issue=e.target_issue", null)) {
            while (c.moveToNext()) {
                total++;
                if ("NONE".equals(c.getString(0))) continue;
                actions++;
                if (c.getInt(1) == 1 && c.getInt(2) >= 0) {
                    settled++;
                    hits += c.getInt(2) == 1 ? 1 : 0;
                }
            }
        }
        if (total == 0) return "";
        // Same single-stake settlement as the formal ledger: hit +WIN_NET_PROFIT, miss -MISS_LOSS.
        double pnl = hits * AppConfig.WIN_NET_PROFIT - (settled - hits) * AppConfig.MISS_LOSS;
        return "近" + total + "期：出手 " + actions + " 期（" + pct(actions, total) + "）；已结算 " + settled + " 次，命中 " + hits
                + (settled == 0 ? "" : "（" + pct(hits, settled) + "）；盈亏 " + String.format(java.util.Locale.ROOT, "%+.1f", pnl)
                + "（保本命中率" + String.format(java.util.Locale.ROOT, "%.1f%%",
                        100.0d * AppConfig.MISS_LOSS / (AppConfig.WIN_NET_PROFIT + AppConfig.MISS_LOSS)) + "）");
    }

    /** Loads the three exploration states on the existing UI snapshot worker. */
    static void load(DbStore store, Map<String, String> states) {
        if (store == null || states == null) return;
        try {
            for (String key : new String[] {STATE_ISSUE, STATE_ACTION, STATE_STATS})
                states.put(key, store.getState(key, ""));
        } catch (RuntimeException unavailable) {
            states.put(STATE_ISSUE, "");
        }
    }

    /** Home card line from snapshot states; empty when no decision exists for this issue. */
    static String homeLine(Map<String, String> states, String targetIssue) {
        if (states == null || targetIssue == null || !targetIssue.equals(states.get(STATE_ISSUE))) return "";
        String action = states.get(STATE_ACTION), stats = states.get(STATE_STATS);
        String head = "REAL".equals(action) ? "本期出手：是（已认证）" : "EXPLORE".equals(action) ? "本期出手：是（探索·未经验证）" : "本期出手：否";
        return head + (stats == null || stats.isEmpty() ? "" : "｜探索" + stats);
    }

    static boolean isExplore(Map<String, String> states, String targetIssue) {
        return states != null && targetIssue != null && targetIssue.equals(states.get(STATE_ISSUE))
                && "EXPLORE".equals(states.get(STATE_ACTION));
    }

    private static boolean valid(double p) {
        return !Double.isNaN(p) && !Double.isInfinite(p) && p > 0.0d && p <= 1.0d;
    }

    private static double quantile(ArrayList<Double> values, double q) {
        if (values.isEmpty()) return Double.NaN;
        ArrayList<Double> sorted = new ArrayList<Double>(values);
        Collections.sort(sorted);
        int index = (int) Math.floor(q * (sorted.size() - 1));
        return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
    }

    private static String pct(int a, int b) {
        return String.format(java.util.Locale.ROOT, "%.1f%%", b == 0 ? 0.0d : 100.0d * a / b);
    }
}
