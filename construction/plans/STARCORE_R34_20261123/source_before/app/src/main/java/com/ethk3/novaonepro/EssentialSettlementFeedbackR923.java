package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Priority lane for the user-visible Teacher/reason/reverse/causal hit-or-miss explanation.
 *
 * <p>This is derivative of an already terminal Production settlement and cannot roll it back or
 * authorize a future BET.  It runs before heavier formula/genome/asset fan-out so the latest issue
 * no longer remains {@code NO_SAMPLE} merely because Research exhausted its time budget.</p>
 */
public final class EssentialSettlementFeedbackR923 {
    public static final String VERSION = "R923_ESSENTIAL_SETTLEMENT_FEEDBACK_V1";

    private EssentialSettlementFeedbackR923() {}
    private static final java.util.concurrent.atomic.AtomicBoolean DRAINING_R335=new java.util.concurrent.atomic.AtomicBoolean();
    private static final String CURSOR_R335="r335_essential_feedback_after";
    private static long elapsedR335(long start){return Math.max(0L,(System.nanoTime()-start)/1000000L);}
    /** A rows=0 writer result is ambiguous. Only exact immutable source readback is a success. */
    private static boolean exactExplanationR335(DbStore store,Models.Draw draw){
        String sql="SELECT p.record_hash,p.predraw_record_hash,p.draw_hash,p.status,p.attribution_summary,p.feedback_summary,p.actual_number,p.hit_label,"
                +"k.record_hash,k.frozen_at,k.position,d.first_seen_at,d.open_time,s.deadline_at,s.position,s.actual_number,s.hit_label,s.commit_at,"
                +"s.n1,s.n2,s.n3,k.teacher_summary,k.reasoning_summary,k.reverse_summary,k.causal_summary,k.feedback_contract,k.trend_summary,k.shape_summary,k.relation_summary,s.contract_hash,d.draw_hash_v2"
                +" FROM "+LegacyPredrawContextBridgeR915.POST_TABLE+" p JOIN "+LegacyPredrawContextBridgeR915.TABLE+" k ON k.target_issue=p.target_issue"
                +" JOIN "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s ON s.target_issue=p.target_issue"
                +" JOIN "+DrawDependencyInputCP12.SOURCE+" d ON d.issue=p.target_issue AND d.dirty=0"
                +" JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t ON t.target_issue=p.target_issue AND t.status='COMPLETED' AND t.draw_issue=p.target_issue"
                +" WHERE p.target_issue=? AND s.pre_draw_frozen=1 AND s.settled=1 AND p.draw_hash=t.draw_hash";
        try(Cursor c=store.readable().rawQuery(sql,new String[]{draw.issue})){
            if(!c.moveToFirst())return false;
            String expected=V51ChecksumEngine.sha256("R919_LEGACY_POST|"+draw.issue+'|'+c.getString(1)+'|'+c.getString(2)+'|'+c.getInt(6)+'|'+c.getInt(7)+'|'+c.getString(3)+'|'+c.getString(4)+'|'+c.getString(5));
            String predrawCanonical=draw.issue;for(int column=21;column<=29;column++)predrawCanonical+='|'+c.getString(column);
            int n1=c.getInt(18),n2=c.getInt(19),n3=c.getInt(20),actual=c.getInt(6);
            boolean valid="ATTRIBUTED_EXACT_DRAW".equals(c.getString(3))&&expected.equals(c.getString(0))
                    &&c.getString(1)!=null&&c.getString(1).matches("[0-9a-f]{64}")&&c.getString(1).equals(c.getString(8))
                    &&V51ChecksumEngine.sha256(predrawCanonical).equals(c.getString(8))
                    &&DrawHashR36.sha256V2(draw).equals(c.getString(2))&&c.getString(2).equals(c.getString(30))&&c.getInt(10)==c.getInt(14)
                    &&n1>=1&&n1<=10&&n2>=1&&n2<=10&&n3>=1&&n3<=10&&n1!=n2&&n1!=n3&&n2!=n3
                    &&c.getInt(7)==((actual==n1||actual==n2||actual==n3)?1:0)
                    &&(c.getInt(7)==0||c.getInt(7)==1)&&c.getInt(10)>=1&&c.getInt(10)<=10
                    &&c.getInt(6)==c.getInt(15)&&c.getInt(7)==c.getInt(16)&&c.getInt(6)==draw.getNumberAtPosition(c.getInt(10))
                    &&c.getLong(9)>0&&c.getLong(13)>0&&c.getLong(9)<c.getLong(11)&&c.getLong(9)<c.getLong(12)&&c.getLong(9)<c.getLong(13)
                    &&c.getLong(17)>0&&c.getLong(17)<c.getLong(13)&&c.getLong(17)<c.getLong(11)&&c.getLong(17)<c.getLong(12);
            return valid&&!c.moveToNext();
        }
    }

    public static Result settleNow(DbStore store, Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid())
            return new Result(false, "INVALID_INPUT", "", 0, 0L);
        long started = System.nanoTime();
        try {
            int rows = LegacyPredrawContextBridgeR915.settleDraw(store, draw);
            boolean verified=exactExplanationR335(store,draw);
            Result out = new Result(verified,verified?(rows>0?"CREATED_VERIFIED":"ALREADY_VERIFIED"):"NO_EXACT_EXPLANATION_PRESERVED",
                    draw.issue,verified?rows:0,elapsedR335(started));
            publish(store, out);
            return out;
        } catch (Throwable failure) {
            Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(failure);if(fatal!=null)throw fatal;
            Result out = new Result(false,
                    "NON_FATAL_" + failure.getClass().getSimpleName(), draw.issue, 0,
                    elapsedR335(started));
            publish(store, out);
            return out;
        }
    }

    /** Bounded stable cursor rotation: a persistent old failure cannot monopolize every page. */
    public static Result drain(DbStore store, int requestedLimit, long budgetMs) {
        if (store == null) return new Result(false, "STORE_NULL", "", 0, 0L);
        if (RealtimePressureGateR811.hasActiveCriticalWork()
                || SettlementExecutionGateR819.isActive())
            return new Result(false, "YIELD_P0", "", 0, 0L);
        if(!DRAINING_R335.compareAndSet(false,true))return new Result(false,"ALREADY_DRAINING","",0,0L);
        try {
        long started = System.nanoTime();
        long deadline = started + Math.max(100L, Math.min(1200L, budgetMs))*1000000L;
        int limit = Math.max(1, Math.min(16, requestedLimit));
        ArrayList<String> issues = new ArrayList<String>();
        Cursor c = null;
        try {
            String after=store.getState(CURSOR_R335,"");if(!after.matches("[0-9]{12}"))after="";
            c = store.readable().rawQuery(pendingIssuesSql(store.readable()),
                    new String[] {SettlementTaskStoreR36.COMPLETED,after,String.valueOf(limit)});
            while (c.moveToNext()) issues.add(c.getString(0));
        } catch (Throwable unavailable) {
            Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(unavailable);if(fatal!=null)throw fatal;
            Result out = new Result(false,
                    "SCAN_" + unavailable.getClass().getSimpleName(), "", 0,
                    elapsedR335(started));
            publish(store, out);
            return out;
        } finally {
            if (c != null) c.close();
        }

        int completed = 0;
        String last = "NO_PENDING";
        for (String issue : issues) {
            if (System.nanoTime() >= deadline
                    || RealtimePressureGateR811.hasActiveCriticalWork()) {
                last = "YIELD_BUDGET_OR_P0";
                break;
            }
            Models.Draw draw = store.getDrawByIssue(issue);
            Result one = settleNow(store, draw);
            last = one.status;
            if (one.success) completed++;
            // Native app_state is only a scheduling cursor; it never certifies an explanation.
            store.setState(CURSOR_R335,issue);
            if(!issue.equals(store.getState(CURSOR_R335,""))){last="CURSOR_COMMIT_UNCONFIRMED";break;}
        }
        Result out = new Result(issues.isEmpty()||completed==issues.size(), last, issues.isEmpty() ? "" : issues.get(0), completed,
                elapsedR335(started));
        publish(store, out);
        return out;
        } finally {DRAINING_R335.set(false);}
    }

    static String pendingIssuesSql(android.database.sqlite.SQLiteDatabase db) {
        return "SELECT t.target_issue FROM " + DbHelper.TABLE_R36_SETTLEMENT_TASK
                            + " t JOIN " + DrawDependencyInputCP12.SOURCE
                            + " d ON d.issue=t.target_issue AND d.dirty=0"
                            + " WHERE t.status=? AND EXISTS(SELECT 1 FROM "
                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " s WHERE s.target_issue=t.target_issue AND s.pre_draw_frozen=1"
                            + " AND s.settled=1 AND s.hit_label IN(0,1)) AND "
                            + LegacyPredrawContextBridgeR915.missingPostRowPredicate(db)
                            + " ORDER BY CASE WHEN t.target_issue>? THEN 0 ELSE 1 END,t.target_issue ASC LIMIT ?";
    }

    private static void publish(DbStore store, Result result) {
        if (store == null || result == null) return;
        try {
            LinkedHashMap<String, String> state = new LinkedHashMap<String, String>();
            state.put("r923_essential_feedback_version", VERSION);
            state.put("r923_essential_feedback_status", result.canonical());
            state.put("r923_essential_feedback_issue", result.issue);
            state.put("r923_essential_feedback_at", String.valueOf(System.currentTimeMillis()));
            store.trySetTelemetryStatesR77(state);
        } catch (Throwable ignored) {Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(ignored);if(fatal!=null)throw fatal;}
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '_').trim();
    }

    public static final class Result {
        public final boolean success;
        public final String status, issue;
        public final int rows;
        public final long costMs;

        Result(boolean ok, String state, String target, int count, long elapsed) {
            success = ok;
            status = safe(state);
            issue = safe(target);
            rows = Math.max(0, count);
            costMs = Math.max(0L, elapsed);
        }

        public String canonical() {
            return VERSION + "|ok=" + (success ? 1 : 0) + "|status=" + status
                    + "|issue=" + issue + "|rows=" + rows + "|costMs=" + costMs;
        }
    }
}
