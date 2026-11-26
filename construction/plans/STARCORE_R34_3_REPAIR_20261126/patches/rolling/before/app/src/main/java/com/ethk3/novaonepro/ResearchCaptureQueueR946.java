package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.TreeSet;

/** One continuation on the existing research Handler; durable snapshots remain the work source. */
public final class ResearchCaptureQueueR946 {
    private static final int MAX_PENDING = 96;
    private final TreeSet<String> pending = new TreeSet<String>();
    private String completedThrough = "";
    private long sequence;
    private Ticket active;
    public static final class Ticket {
        final long sequence; final String target; private boolean started;
        private Ticket(long value, String issue) { sequence=value; target=issue; }
    }
    public synchronized Ticket offer(String target) {
        if (!TargetIssueResolver.isValid(target)) return null;
        if (!completedThrough.isEmpty() && target.compareTo(completedThrough) <= 0) return null;
        pending.add(target); trimImpossibleTail(); if (active != null) return null; return activateHead();
    }
    public synchronized String begin(Ticket ticket) {
        if (ticket == null || ticket != active || ticket.started) return "";
        ticket.started=true; return ticket.target;
    }
    public synchronized Ticket finish(Ticket ticket,String target,boolean terminal) {
        if(ticket==null||ticket!=active||!ticket.started)return null;
        if(!ticket.target.equals(target)){active=null;return activateHead();}
        active=null;
        if(terminal){pending.remove(target);if(completedThrough.isEmpty()||target.compareTo(completedThrough)>0)completedThrough=target;pending.headSet(completedThrough,true).clear();}
        else pending.add(target);
        return activateHead();
    }
    public synchronized void rejected(Ticket ticket){if(ticket==active){if(ticket!=null&&TargetIssueResolver.isValid(ticket.target))pending.add(ticket.target);active=null;}}
    public synchronized void stop(){if(active!=null&&TargetIssueResolver.isValid(active.target))pending.add(active.target);active=null;}
    synchronized int pendingCountForTest(){return pending.size()+(active==null?0:(pending.contains(active.target)?0:1));}
    synchronized String activeTargetForTest(){return active==null?"":active.target;}
    // Pre-draw work is time-sensitive: the newest still-legal target must never sit behind stale
    // backlog. Older targets remain auditable in durable tables and will be retired by drawn() /
    // recovery, but they cannot starve the current target in this in-memory dispatcher.
    private Ticket activateHead(){if(active!=null||pending.isEmpty())return null;String target=pending.last();active=new Ticket(++sequence,target);return active;}
    // Under overload discard the oldest queue entries first. Keeping the newest 96 targets is the
    // only direction compatible with a pre-draw fence; the old implementation discarded newest
    // targets and could therefore freeze capture progress many draws behind the watermark.
    private void trimImpossibleTail(){while(pending.size()>MAX_PENDING)pending.pollFirst();}

    /** Final roster and each admitted execution mode must be durable before closing an issue. */
    public static boolean complete(DbStore store,String target){
        if(store==null||!TargetIssueResolver.isValid(target))return false;
        return mandatoryCompleteR80(store,target)&&!com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.pendingR80(store.readable(),target);
    }
    /** Lower-priority source/probe work may use only the remainder after this gate. */
    public static boolean mandatoryCompleteR80(DbStore store,String target){
        if(store==null||!TargetIssueResolver.isValid(target))return false;
        if(com.ethk3.novaonepro.r952.ResearchDirectorR952.hasPendingCapture(store,target))return false;
        try(Cursor row=BudgetedQueryR927.query(store.readable(),completeSql(),new String[]{target})){return row.moveToFirst();}
    }
    private static String completeSql(){
        String plan=GlobalResearchScheduleR932.PLAN,task=GlobalResearchScheduleR932.TASK,capture=ResearchCaptureLedgerR932.TABLE;
        return "SELECT 1 FROM "+plan+" p WHERE p.target_issue=?"
                +" AND NOT EXISTS(SELECT 1 FROM "+GlobalResearchScheduleR932.RESERVATION_CP11+" r"
                +" WHERE r.state IN('RESERVED','ACTIVE') AND r.next_issue=p.target_issue)"
                +" AND NOT EXISTS(SELECT 1 FROM "+task+" t WHERE t.target_issue=p.target_issue AND t.selected=1"
                +" AND NOT EXISTS(SELECT 1 FROM "+capture+" c WHERE c.target_issue=t.target_issue"
                +" AND c.candidate_key=t.candidate_key AND "+completedCapture("t","c")+"))";
    }

    private static String completedCapture(String task,String capture) {
        return capture+".frozen=1 AND (("+task+".execution_mode='FULL1200' AND "+capture+".full1200=1) OR ("+task+".execution_mode='PROXY120' AND "+task+".tier='C' AND "+capture+".full1200=0 AND EXISTS(SELECT 1 FROM "+GlobalResearchScheduleR932.PROXY+" px WHERE px.target_issue="+task+".target_issue AND px.candidate_key="+task+".candidate_key AND px.action_count=120 AND px.record_hash="+capture+".record_hash)))";
    }

    public static boolean drawn(DbStore store,String target){
        if(store==null||!TargetIssueResolver.isValid(target))return false;
        try(Cursor c=BudgetedQueryR927.query(store.readable(),
                "SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 AND issue>=? LIMIT 1",
                new String[]{target})){return c.moveToFirst();}
    }
    public static String recover(DbStore store){
        if(store==null)return "";
        try(Cursor c=BudgetedQueryR927.query(store.readable(),recoverSql(store.readable(),1),null)){return c.moveToFirst()?c.getString(0):"";}
    }
    private static String recoverSql(android.database.sqlite.SQLiteDatabase db,int limit){
        boolean boundInstalled=false;
        try(Cursor schema=db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN('research_bound_cohort_r80','research_bound_source_r80','research_bound_input_r80')",null)){boundInstalled=schema.moveToFirst()&&schema.getInt(0)==3;}
        String jobs=DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,plan=GlobalResearchScheduleR932.PLAN,task=GlobalResearchScheduleR932.TASK,capture=ResearchCaptureLedgerR932.TABLE;
        return "SELECT j.target_issue FROM "+jobs+" j WHERE j.snapshot_blob IS NOT NULL"
                +" AND length(j.snapshot_hash)=64 AND length(j.target_issue)=12"
                +" AND NOT EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.dirty=0 AND d.issue>=j.target_issue)"
                +" AND (EXISTS(SELECT 1 FROM "+GlobalResearchScheduleR932.RESERVATION_CP11+" r"
                +" WHERE r.state IN('RESERVED','ACTIVE') AND r.next_issue=j.target_issue)"
                +(boundInstalled?" OR EXISTS(SELECT 1 FROM "+com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.COHORT+" b WHERE b.state IN('CAPTURING','READY','PROPOSED','REGISTERED') AND b.first_issue<=j.target_issue AND ((SELECT COUNT(*) FROM "+com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.SOURCE+" bs WHERE bs.cohort_hash=b.cohort_hash AND bs.target_issue=j.target_issue)<2 OR (b.state='CAPTURING' AND NOT EXISTS(SELECT 1 FROM "+com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.INPUT+" bi WHERE bi.cohort_hash=b.cohort_hash AND bi.target_issue=j.target_issue))))":"")
                +" OR NOT EXISTS(SELECT 1 FROM "+plan+" p WHERE p.target_issue=j.target_issue)"
                +" OR EXISTS(SELECT 1 FROM "+task+" t WHERE t.target_issue=j.target_issue"
                +" AND t.selected=1 AND NOT EXISTS(SELECT 1 FROM "+capture+" e WHERE e.target_issue=t.target_issue AND e.candidate_key=t.candidate_key AND "+completedCapture("t","e")+")))"
                +" OR (j.snapshot_blob IS NOT NULL AND length(j.snapshot_hash)=64 AND length(j.target_issue)=12"
                +" AND NOT EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.dirty=0 AND d.issue>=j.target_issue)"
                +" AND EXISTS(SELECT 1 FROM r952_candidate nc JOIN "+ResearchEvaluationRunR967.RUN+" nr ON nr.learner_id=nc.candidate_id"
                +" WHERE nc.state NOT IN ('QUARANTINED','QUALITY_PRUNED','RESOURCE_RETIRED') AND "+TrainingContinuationR973.predicate("nr.rule_hash")
                +" AND nr.first_issue=(SELECT MAX(nr2.first_issue) FROM "+ResearchEvaluationRunR967.RUN+" nr2 WHERE nr2.learner_id=nc.candidate_id AND nr2.first_issue<=j.target_issue)"
                +" AND NOT EXISTS(SELECT 1 FROM r952_candidate_predraw np WHERE np.candidate_id=nc.candidate_id AND np.target_issue=j.target_issue)))"
                +" ORDER BY j.target_issue DESC LIMIT "+limit;
    }
    /** Prepares the same owner queries without scanning data or constructing missing tables. */
    public static void verifySqlContract(android.database.sqlite.SQLiteDatabase db){
        try(Cursor c=db.rawQuery(recoverSql(db,0),null)){c.getColumnCount();}
        try(Cursor c=db.rawQuery(completeSql()+" LIMIT 0",new String[]{"202609190001"})){c.getColumnCount();}
    }

    /** At most two capture-only owner pulses; the third admits existing learning/creation work.
     * Target changes do not reset the bound. Losing the counter write grants no deferral.
     * Capture is still posted by the caller in either case; no second worker is introduced.
     */
    public static boolean claimCapturePriority(final DbStore store,final String target){
        if(store==null)return false;
        return Boolean.TRUE.equals(store.coordinatorR36().runAtomicWrite("R958_CAPTURE_PRIORITY",new DatabaseCoordinator.WriteCallable<Boolean>(){
            public Boolean call(android.database.sqlite.SQLiteDatabase db){
                int streak=0;
                try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key='r958_capture_priority_streak'",null)){
                    if(c.moveToFirst())try{streak=Math.max(0,Integer.parseInt(c.getString(0)));}catch(NumberFormatException invalid){streak=0;}
                }
                boolean defer=TargetIssueResolver.isValid(target)&&streak<2;
                android.content.ContentValues values=new android.content.ContentValues();
                values.put("state_key","r958_capture_priority_streak");values.put("state_value",String.valueOf(defer?streak+1:0));
                values.put("updated_at",System.currentTimeMillis());
                if(db.insertWithOnConflict(DbHelper.TABLE_APP_STATE,null,values,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)<0)
                    throw new IllegalStateException("R958_CAPTURE_PRIORITY_NOT_DURABLE");
                return defer;
            }
        },Boolean.FALSE));
    }
}
