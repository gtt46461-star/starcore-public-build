package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Locale;

/**
 * R947 research-only causal/counterfactual feedback bridge.
 *
 * <p>This class never treats diagnostic association as causal proof. It reads only post-draw miss
 * rows whose recorded actual number exactly matches the persisted clean draw, aggregates recurring
 * failure modes, and chooses a low-level repair direction for a future N=0 research child. The
 * child must still pass the normal pre-draw Future-OOS, calibration, window and certificate gates.
 */
public final class CausalResearchFeedbackR947 {
    public static final String VERSION = "R947_CAUSAL_RESEARCH_FEEDBACK_V1";
    public static final String TABLE = "causal_research_feedback_r947";
    public static final boolean MAY_AUTHORIZE_REAL = false;
    private static final int MAX_ROWS = 240;
    private static final int MIN_ROWS = 12;
    private static final int MIN_DIRECTION_SUPPORT = 8;
    private static final double MIN_MEAN_FAILURE = 0.20d;

    private CausalResearchFeedbackR947() {}

    static void schema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE
                + "(lane TEXT PRIMARY KEY,module TEXT NOT NULL,support INTEGER NOT NULL,"
                + "score REAL NOT NULL,through_issue TEXT NOT NULL,evidence_hash TEXT NOT NULL,"
                + "reason TEXT NOT NULL,updated_at INTEGER NOT NULL)");
    }

    /** Low-priority continuous refresh; no Production/REAL authority. */
    public static String maintain(final DbStore store) {
        if (store == null) return "STORE_NULL";
        if (RealtimePressureGateR811.hasActiveCriticalWork() || SettlementExecutionGateR819.isActive())
            return "YIELD_P0_PRESSURE";
        String result = store.coordinatorR36().runAtomicWrite(
                "R947_CAUSAL_RESEARCH_FEEDBACK_REFRESH",
                new DatabaseCoordinator.WriteCallable<String>() {
                    @Override public String call(SQLiteDatabase db) {
                        String latest = latestIssue(db);
                        if (latest.length() == 0) return "WAIT_HISTORY";
                        Plan plan = compute(db, latest);
                        schema(db);
                        long now = System.currentTimeMillis();
                        for (String lane : new String[] {"algorithm", "genesis", "formula"}) {
                            db.execSQL("INSERT OR REPLACE INTO " + TABLE + " VALUES(?,?,?,?,?,?,?,?)",
                                    new Object[] {lane, plan.module, plan.support, plan.score,
                                            plan.throughIssue, plan.evidenceHash, plan.reason, now});
                        }
                        return plan.status();
                    }
                }, "CAUSAL_FEEDBACK_COMMIT_FAILED");
        try {
            store.setState("r947_causal_feedback_status", result);
            store.setState("r947_causal_feedback_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable ignored) {}
        return result;
    }

    /** Called inside the paid creation transaction. Recomputes from settled evidence for PIT safety. */
    static Plan choose(SQLiteDatabase db, String lane, String throughIssue) {
        if (db == null || !validLane(lane) || !TargetIssueResolver.isValid(throughIssue))
            return Plan.none(throughIssue, "INPUT_INVALID");
        Plan plan = compute(db, throughIssue);
        schema(db);
        db.execSQL("INSERT OR REPLACE INTO " + TABLE + " VALUES(?,?,?,?,?,?,?,?)",
                new Object[] {lane, plan.module, plan.support, plan.score, plan.throughIssue,
                        plan.evidenceHash, plan.reason, System.currentTimeMillis()});
        return plan;
    }

    private static Plan compute(SQLiteDatabase db, String throughIssue) {
        if (MAY_AUTHORIZE_REAL) throw new IllegalStateException("R947_CAUSAL_FEEDBACK_REAL_BOUNDARY");
        double trend = 0.0d, shape = 0.0d, relation = 0.0d;
        int rows = 0, trendN = 0, shapeN = 0, relationN = 0;
        String first = "", last = "";
        Cursor c = null;
        try {
            String actual = "CASE m.position"
                    + " WHEN 1 THEN d.n1 WHEN 2 THEN d.n2 WHEN 3 THEN d.n3 WHEN 4 THEN d.n4"
                    + " WHEN 5 THEN d.n5 WHEN 6 THEN d.n6 WHEN 7 THEN d.n7 WHEN 8 THEN d.n8"
                    + " WHEN 9 THEN d.n9 WHEN 10 THEN d.n10 ELSE 0 END";
            c = db.rawQuery(
                    "SELECT m.target_issue,m.window_wrong_score,m.position_wrong_score,"
                            + "m.main_number_wrong_score,m.third_drag_score,m.counter_ignored_score "
                            + "FROM " + DbHelper.TABLE_MISS_REASON_SAMPLE + " m JOIN "
                            + DrawDependencyInputCP12.SOURCE + " d ON d.issue=m.target_issue AND d.dirty=0 "
                            + "WHERE m.target_issue<=? AND m.position BETWEEN 1 AND 10 "
                            + "AND m.actual_number BETWEEN 1 AND 10 AND m.actual_number=(" + actual + ") "
                            + "ORDER BY m.target_issue DESC,m.created_at DESC LIMIT ?",
                    new String[] {throughIssue, String.valueOf(MAX_ROWS)});
            while (c.moveToNext()) {
                ResearchWorkBudgetR927.checkpoint();
                String issue = safe(c.getString(0));
                if (!TargetIssueResolver.isValid(issue)) continue;
                if (first.length() == 0) first = issue;
                last = issue;
                rows++;
                double w = clamp01(c.getDouble(1));
                double p = clamp01(c.getDouble(2));
                double main = clamp01(c.getDouble(3));
                double third = clamp01(c.getDouble(4));
                double counter = clamp01(c.getDouble(5));
                double t = w;
                double s = 0.62d * main + 0.38d * third;
                double r = 0.62d * p + 0.38d * counter;
                trend += t; shape += s; relation += r;
                if (t >= 0.15d) trendN++;
                if (s >= 0.15d) shapeN++;
                if (r >= 0.15d) relationN++;
            }
        } finally { if (c != null) c.close(); }

        if (rows < MIN_ROWS) {
            String hash = evidence(throughIssue, rows, trendN, shapeN, relationN,
                    trend, shape, relation, first, last);
            return new Plan("OPEN", rows, 0.0d, throughIssue, hash,
                    "INSUFFICIENT_SETTLED_DIAGNOSTIC_ROWS|rows=" + rows
                            + "|minimum=" + MIN_ROWS
                            + "|diagnosticAssociationOnly=1|causalProof=0");
        }

        double trendMean = trend / rows, shapeMean = shape / rows, relationMean = relation / rows;
        String module = "TREND"; int support = trendN; double score = trendMean;
        if (shapeMean > score || (shapeMean == score && shapeN > support)) {
            module = "SHAPE"; support = shapeN; score = shapeMean;
        }
        if (relationMean > score || (relationMean == score && relationN > support)) {
            module = "RELATION"; support = relationN; score = relationMean;
        }
        if (support < MIN_DIRECTION_SUPPORT || score < MIN_MEAN_FAILURE) module = "OPEN";
        String hash = evidence(throughIssue, rows, trendN, shapeN, relationN,
                trend, shape, relation, first, last);
        String reason = "SETTLED_MISS_DIAGNOSTIC|rows=" + rows
                + "|trend=" + fmt(trendMean) + ':' + trendN
                + "|shape=" + fmt(shapeMean) + ':' + shapeN
                + "|relation=" + fmt(relationMean) + ':' + relationN
                + "|selected=" + module + "|diagnosticAssociationOnly=1|causalProof=0"
                + "|futureChildStartsN0=1|realAuthority=0";
        return new Plan(module, support, score, throughIssue, hash, reason);
    }

    private static String evidence(String through, int rows, int tn, int sn, int rn,
            double t, double s, double r, String first, String last) {
        return V51ChecksumEngine.sha256(VERSION + '|' + through + '|' + rows + '|'
                + tn + '|' + sn + '|' + rn + '|' + Double.toHexString(t) + '|'
                + Double.toHexString(s) + '|' + Double.toHexString(r) + '|' + first + '|' + last);
    }

    private static String latestIssue(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT issue FROM " + DbHelper.TABLE_DRAWS
                + " WHERE dirty=0 ORDER BY issue DESC LIMIT 1", null)) {
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        }
    }
    private static boolean validLane(String lane) {
        return "algorithm".equals(lane) || "genesis".equals(lane) || "formula".equals(lane);
    }
    private static double clamp01(double value) {
        return Math.max(0.0d, Math.min(1.0d, Double.isFinite(value) ? value : 0.0d));
    }
    private static String fmt(double value) { return String.format(Locale.US, "%.4f", value); }
    private static String safe(String value) { return value == null ? "" : value.trim(); }

    static final class Plan {
        final String module, throughIssue, evidenceHash, reason;
        final int support;
        final double score;
        Plan(String module, int support, double score, String through, String evidence, String reason) {
            this.module = safe(module); this.support = Math.max(0, support);
            this.score = Double.isFinite(score) ? score : 0.0d;
            this.throughIssue = safe(through); this.evidenceHash = safe(evidence); this.reason = safe(reason);
        }
        static Plan none(String through, String reason) {
            return new Plan("OPEN", 0, 0.0d, through,
                    V51ChecksumEngine.sha256(VERSION + "|NONE|" + safe(through) + '|' + safe(reason)), reason);
        }
        boolean ready() {
            return ("TREND".equals(module) || "SHAPE".equals(module) || "RELATION".equals(module))
                    && support >= MIN_DIRECTION_SUPPORT && score >= MIN_MEAN_FAILURE
                    && evidenceHash.matches("[0-9a-f]{64}");
        }
        String status() {
            return (ready() ? "READY" : "OBSERVING") + "|module=" + module + "|support=" + support
                    + "|score=" + fmt(score) + "|through=" + throughIssue + "|evidence=" + evidenceHash
                    + "|" + reason;
        }
    }
}
