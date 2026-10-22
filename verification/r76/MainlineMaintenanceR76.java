package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;

/** Actual prospective capture and public fixed-window maintenance; isolated synthetic draws.
 * Uses the unchanged R75 numeric fixture builder, never inserts derived CAL labels/models. */
public final class MainlineMaintenanceR76 {
    static int checks;
    static void ok(boolean value,String name){if(!value)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static long n(SQLiteDatabase db,String sql){return MainlineCaptureR75.n(db,sql);}
    static String text(SQLiteDatabase db,String sql){return MainlineCaptureR75.text(db,sql);}
    static String issue(int i){return MainlineCaptureR75.issue(i);}
    static String h(String s){return MainlineCaptureR75.h(s);}
    public static void main(String[] a)throws Exception {
        boolean initial="seed".equals(a[2]);int pulses=0,yields=0;
        try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            if(initial)MainlineCaptureR75.seed(db,"r76-maintenance-capture",true);
            int begin=initial?442:562,end=initial?561:682;
            String bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash;
            String original=text(db,"SELECT model_hash FROM "+ResearchEvaluationRunR967.EVAL);
            if(!initial)ok(n(db,"SELECT captured_n FROM "+CandidateCalibrationR932.STUDY)==120&&n(db,"SELECT settled_n FROM "+CandidateCalibrationR932.STUDY)==120,"fresh JVM restores 120 captured and publicly settled observations");
            for(int i=begin;i<=end;i++){
                String target=issue(i);DeferredEvidenceSnapshotR36.Snapshot snap=MainlineCaptureR75.snapshot(issue(i-1),target,bundle);
                db.beginTransaction();try{
                    db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,1,2,3,?,?)",new Object[]{target,bundle,snap.recordHash,selector,h("fixture-champion:"+target),System.currentTimeMillis()});
                    GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,target,bundle,selector);db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                int captured=0;for(int retry=0;retry<8&&captured==0;retry++)try{captured=FrozenCandidateAdapterR958.captureReservedCP12(store,snap);}catch(ResearchWorkBudgetR927.Yield y){yields++;}
                if(captured!=1)throw new AssertionError("actual capture stalled at "+target);
                if(FrozenCandidateAdapterR958.captureReservedCP12(store,snap)!=0)throw new AssertionError("duplicate capture "+target);
                long at=System.currentTimeMillis()+2;String terminal=h("synthetic-terminal:"+target);
                db.beginTransaction();try{
                    MainlineCaptureR75.draw(db,i,at);
                    db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash) VALUES(?,?,'COMPLETED',?,?,?,?,?)",new Object[]{target,target,at+1,at+1,at+1,terminal,terminal});
                    if(FrozenCandidateAdapterR958.settle(db,store.getDrawByIssue(target),terminal)!=1)throw new AssertionError("frozen settlement "+target);
                    db.setTransactionSuccessful();
                }finally{db.endTransaction();}
                int wanted=i-441;
                for(int retry=0;retry<12&&n(db,"SELECT settled_n FROM "+CandidateCalibrationR932.STUDY)<wanted;retry++){
                    ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(350);pulses++;
                    try{FixedResearchWindowR932.maintainReviewed(store);}catch(ResearchWorkBudgetR927.Yield y){yields++;}finally{ResearchWorkBudgetR927.leave(scope);}
                }
                if(n(db,"SELECT settled_n FROM "+CandidateCalibrationR932.STUDY)!=wanted)throw new AssertionError("public maintenance stalled at "+target);
                if(i%20==1)System.out.println("MAINTENANCE_PROGRESS "+wanted+"/241");
            }
            int wanted=initial?120:241;
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==wanted,"actual prospective calibration observations="+wanted);
            ok(n(db,"SELECT captured_n FROM "+CandidateCalibrationR932.STUDY)==wanted&&n(db,"SELECT settled_n FROM "+CandidateCalibrationR932.STUDY)==wanted,"public maintenance commits exact capture and settle counters="+wanted);
            ok(n(db,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW+" WHERE settled=1 AND proof_error=''")==wanted,"actual frozen predictions authoritatively settled="+wanted);
            ok(n(db,"SELECT COUNT(*) FROM "+CandidateExecutionAuditR932.TABLE+" WHERE action_count=1200")==wanted,"complete 1200-action execution proofs="+wanted);
            ok(original.equals(text(db,"SELECT model_hash FROM "+ResearchEvaluationRunR967.EVAL)),"frozen model stays identical across maintenance and restart");
            ok(n(db,"SELECT COUNT(*) FROM r979_calibration_restart")==0,"public maintenance preserves one uninterrupted study");
            if(!initial){
                for(int retry=0;retry<12&&n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)==0;retry++){
                    ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(350);pulses++;
                    try{FixedResearchWindowR932.maintainReviewed(store);}catch(ResearchWorkBudgetR927.Yield y){yields++;}finally{ResearchWorkBudgetR927.leave(scope);}
                }
                ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)==1,"public maintenance fits exactly one actual 120-1-120 model");
                String study=text(db,"SELECT study_hash FROM "+CandidateCalibrationR932.STUDY);
                CandidateCalibrationR932.Qualification quality=CandidateCalibrationR932.qualificationForStudyCP12(db,study);
                ok(quality.state==CandidateCalibrationR932.QualificationState.FITTED_BUT_NOT_QUALIFIED,"actual holdout quality failure remains blocked without threshold changes");
                System.out.println("ACTUAL_QUALITY="+quality.state+" ECE="+quality.ece+" BRIER="+quality.brier+" BASELINE="+quality.baselineBrier);
                ok(n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE state='COMPLETE'")==1,"prospective reservation completes at original endpoint");
                FixedResearchWindowR932.maintainReviewed(store);
                ok(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)==1&&n(db,"SELECT settled_n FROM "+CandidateCalibrationR932.STUDY)==241,"maintenance replay cannot duplicate fitted model or observations");
            }
            ok(n(db,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.PAIR)==0&&n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_TRADES)==0,"unqualified calibration creates no formal evidence certificate or REAL");
            Files.writeString(Path.of(a[3]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"observations\":"+wanted+",\"maintenance_pulses\":"+pulses+",\"slice_ms\":350,\"yields\":"+yields+",\"scope\":\"ACTUAL_PUBLIC_CAPTURE_AND_MAINTENANCE_SYNTHETIC_HOST_NOT_PHONE_QUALITY\"}\n");
        }
    }
}
