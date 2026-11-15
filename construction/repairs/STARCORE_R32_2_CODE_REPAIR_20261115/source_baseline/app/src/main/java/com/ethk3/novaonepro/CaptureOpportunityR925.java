package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

/** Distinguishes a system opportunity from the candidate's execution result. Never edits a prediction. */
public final class CaptureOpportunityR925 {
    private CaptureOpportunityR925() {}
    public static void record(final DbStore store,final String target,final String genome,
            final String status,final long cost,final String reason){
        boolean ok=store.coordinatorR36().runWrite("R925_CAPTURE_OPPORTUNITY",new DatabaseCoordinator.WriteRunnable(){
            public void run(SQLiteDatabase db){
                if(!"FROZEN".equals(status))ResearchCaptureLedgerR932.record(db,target,"algorithm:"+genome,status,reason,"",cost,false);
                db.execSQL("INSERT OR IGNORE INTO v57_capture_opportunity_r925 VALUES (?,?,?,?,?,?)",
                    new Object[]{target,genome,System.currentTimeMillis(),status,cost,reason});
                db.execSQL("UPDATE v57_capture_opportunity_r925 SET status=?,cost_ms=?,reason=? WHERE target_issue=? AND genome_id=? AND status<>'FROZEN'",
                    new Object[]{status,cost,reason,target,genome});
            }});
        if(!ok)throw new IllegalStateException("R932_CAPTURE_OPPORTUNITY_COMMIT_FAILED");
    }
    public static void eligible(final DbStore store,final String target){
        boolean ok=store.coordinatorR36().runAtomicWrite("R925_CAPTURE_ELIGIBILITY",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            db.execSQL("INSERT OR IGNORE INTO v57_capture_opportunity_r925 SELECT ?,genome_id,?,'NOT_SELECTED',0,'ELIGIBLE_COHORT' FROM v57_algorithm_genome_r908 WHERE status IN ('IDEA_WAIT_FUTURE_OOS','OBSERVING_FUTURE_OOS','CHALLENGER_FUTURE_OOS') AND (earliest_use_issue='' OR earliest_use_issue<=?) AND trained_until_issue<?",new Object[]{target,System.currentTimeMillis(),target,target});
            // One set operation per retry, limited to the immutable admitted prefix.
            // Existing EXECUTION/YIELD/FROZEN results remain untouched.
            db.execSQL("UPDATE v57_capture_opportunity_r925 SET status='SYSTEM_PENDING',reason='SELECTED_AWAITING_SURFACE'"
                    +" WHERE target_issue=? AND status='NOT_SELECTED' AND EXISTS(SELECT 1 FROM "
                    +GlobalResearchScheduleR932.TASK+" sq WHERE sq.target_issue=v57_capture_opportunity_r925.target_issue"
                    +" AND sq.candidate_key=('algorithm:'||v57_capture_opportunity_r925.genome_id) AND sq.selected=1 AND "
                    +GlobalResearchScheduleR932.admittedPage("sq")+")",new Object[]{target});
        }});
        if(!ok)throw new IllegalStateException("R932_CAPTURE_ELIGIBILITY_COMMIT_FAILED");
    }
    public static String report(DbStore store){return "覆盖合同R925：system_capture按实际开奖；eligible_n是赛前可运行机会；selection=调度选中/eligible；execution=有效输出/实际执行（不把预算让步、系统冻结拒绝计为质量失败）。\n"+RuntimeObservabilityR925.query(store,
        "SELECT c.genome_id,c.system_capture_coverage,c.candidate_execution_coverage,c.execution_opportunities,COUNT(o.target_issue) eligible_n,SUM(CASE WHEN o.status<>'NOT_SELECTED' THEN 1 ELSE 0 END) selected_n,CASE WHEN COUNT(o.target_issue)>0 THEN 1.0*SUM(CASE WHEN o.status<>'NOT_SELECTED' THEN 1 ELSE 0 END)/COUNT(o.target_issue) ELSE NULL END candidate_selection_coverage,c.coverage_contract FROM v57_candidate_coverage_r925 c LEFT JOIN v57_capture_opportunity_r925 o ON o.genome_id=c.genome_id GROUP BY c.genome_id",false);}
    public static double[] executionCoverage(DbStore store,String id){
        android.database.Cursor c=store.readable().rawQuery("SELECT COUNT(*),COALESCE(SUM(CASE WHEN status='FROZEN' THEN 1 ELSE 0 END),0) FROM v57_capture_opportunity_r925 WHERE genome_id=? AND status IN ('FROZEN','EXECUTION_NO_OUTPUT')",new String[]{id});
        try{c.moveToFirst();int n=c.getInt(0);return new double[]{n,n==0?-1:(double)c.getInt(1)/n};}finally{c.close();}
    }


public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store){return "覆盖合同R925：system_capture按实际开奖；eligible_n是赛前可运行机会；selection=调度选中/eligible；execution=有效输出/实际执行（不把预算让步、系统冻结拒绝计为质量失败）。\n"+RuntimeObservabilityR925.querySnapshotCP4(report, store,
        "SELECT c.genome_id,c.system_capture_coverage,c.candidate_execution_coverage,c.execution_opportunities,COUNT(o.target_issue) eligible_n,SUM(CASE WHEN o.status<>'NOT_SELECTED' THEN 1 ELSE 0 END) selected_n,CASE WHEN COUNT(o.target_issue)>0 THEN 1.0*SUM(CASE WHEN o.status<>'NOT_SELECTED' THEN 1 ELSE 0 END)/COUNT(o.target_issue) ELSE NULL END candidate_selection_coverage,c.coverage_contract FROM v57_candidate_coverage_r925 c LEFT JOIN v57_capture_opportunity_r925 o ON o.genome_id=c.genome_id GROUP BY c.genome_id",false);}
}
