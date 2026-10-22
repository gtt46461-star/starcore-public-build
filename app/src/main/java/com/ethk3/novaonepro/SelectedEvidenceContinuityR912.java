package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;

/**
 * Repairs the R907 auxiliary selected-evidence gap from immutable pre-draw canonical columns.
 * It never reads an outcome and never changes the selected action or DecisionGate result.
 */
public final class SelectedEvidenceContinuityR912 {
    private SelectedEvidenceContinuityR912() {}

    /** Fast read used only as a post-commit invariant check; legacy gaps remain repairable below. */
    public static boolean exists(DbStore store, String targetIssue) {
        if (store == null || !TargetIssueResolver.isValid(targetIssue)) return false;
        Cursor c = null;
        try {
            c = store.readable().rawQuery(
                    "SELECT 1 FROM " + DbHelper.TABLE_R906_SELECTED_EVIDENCE
                            + " WHERE target_issue=? LIMIT 1",
                    new String[] {targetIssue});
            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    public static int repairBatch(final DbStore store, int requested) {
        if (store == null) return 0;
        final int limit = Math.max(1, Math.min(32, requested));
        final ArrayList<Row> rows = new ArrayList<Row>();
        Cursor c = null;
        try {
            c = store.readable().rawQuery(
                    "SELECT v.target_issue,v.record_hash,v.position,v.n1,v.n2,v.n3,v.rank_no,v.raw_action_p,v.rank_score,"
                            + "v.third_margin,v.entropy,v.stability,v.window_consensus,v.assignment_support,"
                            + "COALESCE(NULLIF(s.commit_at,0),s.created_at) FROM "
                            + V51Repository.SELECTED + " v JOIN "
                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION + " s ON s.target_issue=v.target_issue "
                            + "LEFT JOIN " + DbHelper.TABLE_R906_SELECTED_EVIDENCE
                            + " e ON e.target_issue=v.target_issue WHERE e.target_issue IS NULL "
                            + "AND s.pre_draw_frozen=1 ORDER BY v.target_issue ASC LIMIT ?",
                    new String[] {String.valueOf(limit)});
            while (c.moveToNext()) rows.add(new Row(
                    c.getString(0), c.getString(1), c.getInt(2), c.getInt(3),
                    c.getInt(4), c.getInt(5), c.getInt(6), c.getDouble(7),
                    c.getDouble(8), c.getDouble(9), c.getDouble(10), c.getDouble(11),
                    c.getDouble(12), c.getDouble(13), c.getLong(14)));
        } finally { if (c != null) c.close(); }
        if (rows.isEmpty()) return 0;
        Integer written = store.coordinatorR36().runWrite(
                "R912_SELECTED_EVIDENCE_CONTINUITY",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    @Override public Integer call(SQLiteDatabase db) {
                        int count = 0;
                        for (Row r : rows) {
                            ContentValues v = new ContentValues();
                            v.put("target_issue", r.target);
                            v.put("action_ordinal", K3ActionSpaceEngine.globalOrdinal(
                                    r.position, r.n1, r.n2, r.n3));
                            v.put("action_hash", safe(r.sourceHash));
                            putProbability(v, "raw_action_p", r.raw);
                            v.put("raw_action_p_source", "V51_SELECTED_CORE_R912_REPAIR");
                            v.put("raw_action_p_reason", validProbability(r.raw) ? "" : "LEGACY_VALUE_MISSING");
                            if (r.rank > 0) v.put("rank_no", r.rank); else v.putNull("rank_no");
                            if (r.rank > 0 && finite(r.rankScore)) v.put("rank_score_value", r.rankScore);
                            else v.putNull("rank_score_value");
                            v.put("rank_status", r.rank > 0 && finite(r.rankScore)
                                    ? "AVAILABLE" : "UNAVAILABLE");
                            putNumber(v, "third_margin", r.third, true);
                            putProbability(v, "entropy_risk", r.entropy);
                            v.put("entropy_risk_source", "V51_SELECTED_CORE_R912_REPAIR");
                            putProbability(v, "stability", r.stability);
                            v.put("stability_source", "V51_SELECTED_CORE_R912_REPAIR");
                            v.put("stability_reason", validProbability(r.stability) ? "" : "LEGACY_VALUE_MISSING");
                            putProbability(v, "window_consensus", r.window);
                            putProbability(v, "assignment_support", r.assignment);
                            v.put("metric_schema_hash", V51ChecksumEngine.sha256(
                                    "R906_SELECTED_METRIC_SCHEMA_V1|raw_action_p|rank_no|rank_score|third_margin|entropy_risk|stability|window_consensus|assignment_support"));
                            v.put("frozen_at", Math.max(0L, r.frozenAt));
                            v.put("record_hash", V51ChecksumEngine.sha256(
                                    "R912_SELECTED_EVIDENCE_REPAIR|" + r.canonical()));
                            long inserted = db.insertWithOnConflict(
                                    DbHelper.TABLE_R906_SELECTED_EVIDENCE, null, v,
                                    SQLiteDatabase.CONFLICT_IGNORE);
                            if (inserted >= 0L) count++;
                        }
                        return Integer.valueOf(count);
                    }
                }, Integer.valueOf(0));
        int n = written == null ? 0 : written.intValue();
        try {
            store.setState("r912_selected_evidence_repaired", String.valueOf(n));
            store.setState("r912_selected_evidence_repaired_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable ignore) {}
        return n;
    }

    private static void putProbability(ContentValues v, String key, double x) {
        if (validProbability(x)) v.put(key + "_value", x); else v.putNull(key + "_value");
        v.put(key + "_status", validProbability(x) ? "AVAILABLE" : "UNAVAILABLE");
    }

    private static void putNumber(ContentValues v, String key, double x, boolean present) {
        if (present && finite(x)) v.put(key + "_value", x); else v.putNull(key + "_value");
        v.put(key + "_status", present && finite(x) ? "AVAILABLE" : "UNAVAILABLE");
    }

    private static boolean validProbability(double x) { return finite(x) && x >= 0.0d && x <= 1.0d; }
    private static boolean finite(double x) { return !Double.isNaN(x) && !Double.isInfinite(x); }
    private static String safe(String x) { return x == null ? "" : x.trim(); }

    private static final class Row {
        final String target, sourceHash;
        final int position,n1,n2,n3,rank;
        final double raw, rankScore, third, entropy, stability, window, assignment;
        final long frozenAt;
        Row(String t,String h,int position,int n1,int n2,int n3,int r,double raw,double score,double third,double entropy,
                double stability,double window,double assignment,long at) {
            target=safe(t);sourceHash=safe(h);this.position=position;this.n1=n1;this.n2=n2;this.n3=n3;rank=r;this.raw=raw;rankScore=score;
            this.third=third;this.entropy=entropy;this.stability=stability;
            this.window=window;this.assignment=assignment;frozenAt=at;
        }
        String canonical(){return target+'|'+sourceHash+'|'+position+'|'+n1+'|'+n2+'|'+n3+'|'+rank+'|'+raw+'|'+rankScore+'|'
                +third+'|'+entropy+'|'+stability+'|'+window+'|'+assignment+'|'+frozenAt;}
    }
}
