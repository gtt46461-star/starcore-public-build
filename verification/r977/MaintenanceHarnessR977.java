package com.ethk3.novaonepro;

import java.lang.reflect.Method;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Actual owner/SQL regression. Fabricated fixtures are NOT predictive evidence. */
public final class MaintenanceHarnessR977 {
    private static int checks, failures;
    private static void check(boolean pass,String name){checks++;if(!pass)failures++;System.out.println((pass?"PASS ":"FAIL ")+name);}
    private static String lane(DbStore s,int n)throws Exception{
        Method m=ResearchLearningContinuityR912.class.getDeclaredMethod("runLane",DbStore.class,int.class,long.class);
        m.setAccessible(true);return (String)m.invoke(null,s,n,100000L);
    }
    private static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    private static String value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    private static void draw(SQLiteDatabase db,String issue){
        db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+"(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,1,1,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{issue});
    }
    private static void setup(SQLiteDatabase db,DbStore store)throws Exception{
        HostProductR974.schema(db,store);
        db.beginTransaction();try{FixedResearchWindowR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    private static void seedCal(SQLiteDatabase db,String issue,int n){
        for(int i=0;i<n;i++){
            String id="fixture-cal-"+i,key="algorithm:fixture-"+i;
            db.execSQL("INSERT INTO "+CandidateCalibrationR932.STUDY+" VALUES(?,?,1,0,'COLLECTING',?)",new Object[]{id,key,issue});
            db.execSQL("INSERT INTO "+CandidateCalibrationR932.OBS+"(study_hash,target_issue,candidate_key,structure_hash,bundle_hash,selection_hash,raw_score,position,n1,n2,n3,prediction_hash,frozen_at) VALUES(?,?,?,'s','b','p',0.5,1,1,2,3,'fixture',0)",new Object[]{id,issue,key});
        }
    }
    private static void pendingAndPreempt(String bridge)throws Exception{
        for(String p:new String[]{"FIXED_WINDOW_PENDING","CERTIFICATE_PENDING","MODULE_PENDING","wrapper=FIXED_WINDOW_PENDING|n=1"})
            check(!ResearchLearningContinuityR912.terminalReviewOutcome(p),"DO_NOT_ACK_"+p);
        SQLiteDatabase db=new SQLiteDatabase(bridge);
        try{
            DbStore s=HostProductR974.store(db);setup(db,s);
            RealtimePressureGateR811.beginAnalysis();
            try{for(int n:new int[]{6,7,9,15,16,19})check(!ResearchLearningContinuityR912.terminalReviewOutcome(lane(s,n)),"PREEMPT_NOT_ACK_LANE_"+n);}
            finally{RealtimePressureGateR811.endAnalysis();}
            // R980 can drain 16 x 16 labels per callback. Use MORE than that bound,
            // retaining the original incomplete-page/no-ACK invariant rather than
            // incorrectly requiring a 40-label queue to remain unfinished.
            final int labels=400;
            String issue="202609210002";draw(db,issue);seedCal(db,issue,labels);
            String first=lane(s,6);long remaining=count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS+" WHERE hit=-1");
            check(remaining>0&&remaining<labels,"BOUNDED_PAGE_MAKES_REAL_PROGRESS");
            check(!ResearchLearningContinuityR912.terminalReviewOutcome(first),"PARTIAL_PAGE_DOES_NOT_COMPLETE_WATERMARK:"+first);
            int pages=1;String last=first;
            while(pages<10&&!ResearchLearningContinuityR912.terminalReviewOutcome(last)){last=lane(s,6);pages++;}
            check(count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS+" WHERE hit=-1")==0,"DRAIN_400_WITHOUT_NEW_DRAW");
            check(ResearchLearningContinuityR912.terminalReviewOutcome(last),"DRAINED_PAGE_CAN_ACK");
            check(count(db,"SELECT SUM(settled_n) FROM "+CandidateCalibrationR932.STUDY)==labels,"EACH_LABEL_COUNTED_ONCE");
            lane(s,6);check(count(db,"SELECT SUM(settled_n) FROM "+CandidateCalibrationR932.STUDY)==labels,"REPEATED_REVIEW_IS_IDEMPOTENT");
            System.out.println("MEASURE cal_rows="+labels+" pages="+pages+" new_draws_during_drain=0");
        }finally{db.shutdown();}
    }
    private static void window(SQLiteDatabase db,String id,String last,String contract){
        db.execSQL("INSERT INTO "+FixedResearchWindowR932.WINDOW+"(window_id,candidate_key,structural_hash,champion_hash,selection_hash,first_issue,last_issue,planned_n,window_no,registered_at,state,reason) VALUES(?,?,'s','b','p','202609210001',?,120,1,0,'OPEN','HOST_FIXTURE')",new Object[]{id,"algorithm:"+id,last});
        db.execSQL("INSERT INTO "+FixedResearchWindowR932.CONTRACT+" VALUES(?,?)",new Object[]{id,contract});
        db.execSQL("INSERT INTO "+ResearchMultiplicityR932.LOOK+" VALUES(?,?,1,'202609210001',?,0.001,'REGISTERED','',NULL)",new Object[]{id,id,last});
    }
    private static void headOfLine(String bridge)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(bridge);
        try{
            DbStore s=HostProductR974.store(db);setup(db,s);draw(db,"202609210005");
            String contract=FixedResearchWindowR932.contract(db);
            window(db,"pending-oldest","202609210002",contract);window(db,"ready-later","202609210003",contract);
            db.execSQL("INSERT INTO "+FixedResearchWindowR932.PAIR+"(window_id,target_issue,candidate_hash,champion_prediction_hash,position,n1,n2,n3,selected_hit_p,calibration_hash,frozen_at) VALUES('pending-oldest','202609210001','c','b',1,1,2,3,0.5,'cal',0)");
            FixedResearchWindowR932.closeOne(s);
            check(value(db,"SELECT state FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_id='pending-oldest'").equals("OPEN"),"PENDING_WINDOW_NOT_FABRICATED_CLOSED");
            check(value(db,"SELECT state FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_id='ready-later'").equals("PAUSED_CAPTURE"),"READY_LATER_WINDOW_NOT_STARVED_BY_PENDING_HEAD");
            check(count(db,"SELECT COUNT(*) FROM "+FixedWindowStatisticsR932.TABLE+" WHERE window_id='ready-later'")==1,"LATER_WINDOW_TERMINAL_RESULT_PERSISTED");
            check(count(db,"SELECT COUNT(*) FROM "+FixedWindowCertificatePublisherR932.TABLE+" WHERE state='PUBLISHED'")==0,"NO_CERTIFICATE_FROM_MISSING_CAPTURE_FIXTURE");
        }finally{db.shutdown();}
    }
    private static void sameWatermarkAndPublication(String bridge)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(bridge);
        try{
            DbStore store=HostProductR974.store(db);setup(db,store);String issue="202609210006";draw(db,issue);
            db.beginTransaction();try{
                ResearchTaskQueueR967.schema(db);
                for(int n=0;n<ResearchLearningContinuityR912.CONTINUITY_LANES;n++)
                    db.execSQL("INSERT INTO "+ResearchTaskQueueR967.LANE+"(lane,desired_issue,served_issue,updated_at) VALUES(?,?,?,1)",new Object[]{n,issue,issue});
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            seedCal(db,issue,1);
            check(ResearchLearningContinuityR912.needsContinuation(store),"WORK_AFTER_SERVED_WATERMARK_RESTARTS_CONTINUATION");
            db.beginTransaction();try{ResearchLearningContinuityR912.ensureDesired(db,issue,false,false);db.setTransactionSuccessful();}finally{db.endTransaction();}
            check(count(db,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE candidate_id='lane:6' AND status='PENDING'")==1,"SAME_WATERMARK_REOPENS_FIXED_LANE");
            db.beginTransaction();try{ResearchLearningContinuityR912.ensureDesired(db,issue,false,false);db.setTransactionSuccessful();}finally{db.endTransaction();}
            check(count(db,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE candidate_id='lane:6'")==1,"REOPEN_ENQUEUE_IS_IDEMPOTENT");
            check(!FixedWindowCertificatePublisherR932.hasRunnableWork(db),"EMPTY_PUBLICATION_NOT_BUSY_LOOP");
            String contract=FixedResearchWindowR932.contract(db);
            window(db,"terminal-a","202609210003",contract);window(db,"terminal-b","202609210004",contract);
            db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET state='PAUSED_CAPTURE' WHERE window_id IN ('terminal-a','terminal-b')");
            check(FixedWindowCertificatePublisherR932.hasRunnableWork(db),"TERMINAL_WINDOWS_REMAINING_AUDIT_VISIBLE");
            try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609210007",System.currentTimeMillis())){
                String first=lane(store,9);
                check(!ResearchLearningContinuityR912.terminalReviewOutcome(first),"PUBLICATION_FIRST_WINDOW_DOES_NOT_ACK_SECOND");
                String second=lane(store,9);
                check(ResearchLearningContinuityR912.terminalReviewOutcome(second),"PUBLICATION_ACK_AFTER_ALL_WINDOWS_REVIEWED");
            }
            check(count(db,"SELECT COUNT(*) FROM "+FixedWindowCertificatePublisherR932.TABLE+" WHERE state='BLOCKED'")==2,"PAUSED_WINDOWS_AUDITED_BLOCKED_NOT_CERTIFIED");
            check(!FixedWindowCertificatePublisherR932.hasRunnableWork(db),"AUDITED_BLOCKED_WINDOWS_DO_NOT_SPIN");
            db.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue TEXT PRIMARY KEY,snapshot_blob BLOB,snapshot_hash TEXT)");
            store.setState("r965_ablation_source",SourceManifestR815.SOURCE_TREE_SHA256);
            store.setState("r932_ablation_current",issue);store.setState("r932_ablation_cursor","202609210005");
            check(!ModuleAblationR932.hasRunnableWork(db),"MODULE_MISSING_INPUT_DOES_NOT_SPIN");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" VALUES(?,?,?)",new Object[]{issue,new byte[]{0,1,2},"INVALID_FIXTURE"});
            check(ModuleAblationR932.hasRunnableWork(db),"MODULE_CURRENT_HAS_UNFINISHED_OWNER_WORK");
            // An invalid snapshot is explicitly blocked, not treated as a successful ablation.
            String result=lane(store,7);
            check(ResearchLearningContinuityR912.terminalReviewOutcome(result),"BLOCKED_MODULE_INPUT_REVIEW_TERMINATES");
            check(count(db,"SELECT COUNT(*) FROM "+ModuleAblationR932.TABLE+" WHERE state='BLOCKED_INPUT_OR_PARITY'")==1,"MODULE_INVALID_BYTES_NOT_FABRICATED_AS_EVIDENCE");
            check(!ModuleAblationR932.hasRunnableWork(db),"BLOCKED_MODULE_CURSOR_PREVENTS_REPEAT_SPIN");
        }finally{db.shutdown();}
    }
    private static final class FaultDb extends SQLiteDatabase {
        String failTable="";
        FaultDb(String bridge)throws Exception{super(bridge);}
        private void fault(String sql){if(!failTable.isEmpty()&&sql.startsWith("CREATE TABLE")&&sql.contains(failTable)){failTable="";throw new IllegalStateException("INJECTED_WORLD_SQL_FAILURE");}}
        @Override public synchronized void execSQL(String sql){fault(sql);super.execSQL(sql);}
        @Override public synchronized void execSQL(String sql,Object[] args){fault(sql);super.execSQL(sql,args);}
    }
    private static void worldFailureReceipts(String bridge)throws Exception{
        FaultDb db=new FaultDb(bridge);
        try{
            DbStore store=HostProductR974.store(db);setup(db,store);draw(db,"202609210004");
            db.failTable=MolecularToolWorldR947.TABLE;
            String molecular=lane(store,15);
            check(molecular.contains("FAILED")&&!ResearchLearningContinuityR912.terminalReviewOutcome(molecular),"MOLECULAR_COMMIT_FAILURE_NOT_DISCARDED:"+molecular);
            db.failTable=CausalResearchFeedbackR947.TABLE;
            String causal=lane(store,16);
            check(causal.contains("FAILED")&&!ResearchLearningContinuityR912.terminalReviewOutcome(causal),"CAUSAL_COMMIT_FAILURE_NOT_DISCARDED:"+causal);
            check(!db.inTransaction(),"WORLD_FAILURE_LEAVES_NO_OPEN_TRANSACTION");
        }finally{db.shutdown();}
    }
    public static void main(String[] args)throws Exception{
        pendingAndPreempt(args[0]);headOfLine(args[0]);sameWatermarkAndPublication(args[0]);worldFailureReceipts(args[0]);
        System.out.println("R977_MAINTENANCE|checks="+checks+"|failures="+failures+"|scope=PRODUCT_JAVA_REAL_SQLITE_HOST_NOT_DEVICE");
        if(failures>0)throw new AssertionError("REGRESSION_FAILURES="+failures);
    }
}
