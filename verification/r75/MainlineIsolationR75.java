package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
/** Actual negative scalar score before a healthy AST3 candidate, not a mocked scorer. */
public final class MainlineIsolationR75 {
    public static void main(String[] a)throws Exception{
        try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
            String good=System.getProperty("starcore.test.ast");
            System.setProperty("starcore.test.ast",a[3]);MainlineCaptureR75.seed(db,"r75-invalid-score",true);
            System.setProperty("starcore.test.ast",good);MainlineCaptureR75.seed(db,"r75-healthy-score",false);
            // A legitimate descending draw produces the signed delta's negative
            // value. The healthy frequency atom remains nonnegative.
            db.execSQL("UPDATE draws SET n1=10,n10=1 WHERE issue='202609230440'");
            String target=MainlineCaptureR75.issue(442),bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash;
            DeferredEvidenceSnapshotR36.Snapshot snapshot=MainlineCaptureR75.snapshot(MainlineCaptureR75.issue(441),target,bundle);
            db.beginTransaction();try{
                db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,1,2,3,?,1)",new Object[]{target,bundle,snapshot.recordHash,selector,MainlineCaptureR75.h("fixture-champion")});
                if(GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,target,bundle,selector)!=2)throw new AssertionError("two reservations");
                db.execSQL("UPDATE research_capture_reservation_v2 SET created_at=0 WHERE candidate_key IN(SELECT 'algorithm:'||candidate_id FROM r967_frozen_evaluation_v2 WHERE learner_id='r75-invalid-score')");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            int captured=0;for(int retry=0;retry<8&&captured==0;retry++)try{captured=FrozenCandidateAdapterR958.captureReservedCP12(store,snapshot);}catch(ResearchWorkBudgetR927.Yield expected){}
            if(captured!=1)throw new AssertionError("healthy candidate behind invalid graph never captured");
            MainlineCaptureR75.ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2 WHERE state='RETIRED' AND last_reason='CAPTURE_INVALID_SCORE:NEGATIVE_GRAPH_SCORE'")==1,"invalid fixed graph reservation retired with exact reason");
            MainlineCaptureR75.ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM research_candidate_cal_study_r932 WHERE state='BLOCKED_INVALID_INPUT' AND captured_n=0")==1,"invalid graph retains zero-sample blocked study");
            MainlineCaptureR75.ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM research_capture_opportunity_r932 WHERE status='EXECUTION_FAILED' AND frozen=0")==1,"failure remains visible without fabricated freeze");
            MainlineCaptureR75.ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM r958_frozen_predraw p JOIN r967_frozen_evaluation_v2 f ON f.candidate_id=p.candidate_id WHERE f.learner_id='r75-healthy-score'")==1,"later healthy graph actually captures");
            MainlineCaptureR75.ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM research_candidate_cal_observation_r932")==1,"only healthy observation enters calibration");
            MainlineCaptureR75.ok(FrozenCandidateAdapterR958.captureReservedCP12(store,snapshot)==0,"repeat cannot reset failed study or duplicate healthy sample");
            for(int retry=0;retry<3;retry++){
                db.beginTransaction();try{
                    if(GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,target,bundle,selector)!=0)throw new AssertionError("invalid immutable score admitted again");
                    db.setTransactionSuccessful();
                }finally{db.endTransaction();}
            }
            MainlineCaptureR75.ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM research_capture_reservation_v2")==2&&MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM r979_calibration_restart")==0,"repeated admission retains failed study without new reservation or reset");
            // Separately inject one cleanup call after an exhausted compute
            // slice. This tests bounded bookkeeping, not another scoring result.
            String pending=MainlineCaptureR75.text(db,"SELECT study_hash FROM research_capture_reservation_v2 WHERE state='ACTIVE'");
            GlobalResearchScheduleR932.CaptureReservationCP11 active=GlobalResearchScheduleR932.reservationForStudyCP11(db,pending);
            ResearchWorkBudgetR927.Scope expired=ResearchWorkBudgetR927.enter(1);
            try{
                Thread.sleep(3);
                java.lang.reflect.Method cleanup=FrozenCandidateAdapterR958.class.getDeclaredMethod("recordInvalidScoreR75",DbStore.class,GlobalResearchScheduleR932.CaptureReservationCP11.class,String.class);
                cleanup.setAccessible(true);
                MainlineCaptureR75.ok((Boolean)cleanup.invoke(null,store,active,active.next),"failure bookkeeping completes after compute deadline under bounded writer deadline");
                boolean restored=false;try{ResearchWorkBudgetR927.checkpoint();}catch(ResearchWorkBudgetR927.Yield expected){restored=true;}
                MainlineCaptureR75.ok(restored,"expired compute scope restored so later scoring cannot overrun");
            }finally{ResearchWorkBudgetR927.leave(expired);}
            MainlineCaptureR75.ok(MainlineCaptureR75.n(db,"SELECT COUNT(*) FROM research_candidate_cal_observation_r932")==1,"failure bookkeeping cannot manufacture samples");
            Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"scope\":\"PUBLIC_AST3_NEGATIVE_SCORE_ISOLATION_ACTUAL_SQL\"}\n");
        }
    }
}
