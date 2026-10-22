package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Actual VM/SQLite production methods on synthetic draws, not live evidence. */
public final class DevelopmentTrainingHarnessR959 {
    static int checks;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static long n(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static void draw(SQLiteDatabase db,String issue,Random r){List<Integer> nums=new ArrayList<Integer>();for(int i=1;i<=10;i++)nums.add(i);Collections.shuffle(nums,r);Object[] v=new Object[14];v[0]=issue;for(int i=0;i<10;i++)v[i+1]=nums.get(i);v[11]=0;v[12]=System.currentTimeMillis();v[13]=System.currentTimeMillis();db.execSQL("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",v);}
    static DbStore init(SQLiteDatabase db){
        DbStore s=new DbStore(db);DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);RuntimeEventLedgerR926.ensureSchema(db);db.execSQL("CREATE TABLE "+DbHelper.TABLE_OOS_ACTION_LOG+"(id INTEGER PRIMARY KEY)");DbHelper.ensureR811CriticalSchema(db);DbHelper.ensureR906CriticalSchema(db);V51Repository.ensure(db);
        db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER)");
        db.execSQL("CREATE TABLE miss_reason_sample(target_issue TEXT,created_at INTEGER,position INTEGER,actual_number INTEGER,window_wrong_score REAL,position_wrong_score REAL,main_number_wrong_score REAL,third_drag_score REAL,counter_ignored_score REAL)");
        RealtimePressureGateR811.resetForTests();check(ResearchSchemaCoordinatorR926.ensureToReady(s.coordinatorR36(),8),"ACTUAL_SCHEMA_READY");return s;
    }
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try {
            DbStore store=init(db);Random random=new Random(959);String base="202609190001";
            db.beginTransaction();try{for(int i=0;i<320;i++){draw(db,base,random);if(i<319)base=TargetIssueResolver.next(base).targetIssue;}db.setTransactionSuccessful();}finally{db.endTransaction();}
            check(DirectedResearchCreationR932.maintain(store,"algorithm").startsWith("CREATED_N0|"),"ACTUAL_GENERATOR_CREATES_PARENT");
            String parent="",spec="";try(Cursor c=db.rawQuery("SELECT genome_id,genome_spec FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME,null)){c.moveToFirst();parent=c.getString(0);spec=c.getString(1);}
            AlgorithmGenomeR908 legacy=AlgorithmGenomeR908.parse(spec);
            check(legacy.initialParameters.isEmpty()&&legacy.genomeId.equals(parent)&&AlgorithmGenomeR908.parse(legacy.canonical()).canonical().equals(legacy.canonical()),"LEGACY_EXECUTABLE_ID_AND_PARSED_ROUNDTRIP_UNCHANGED");
            String registered=ResearchDynamicBootstrapR922.pulse(store,1000).status;
            check(registered.startsWith("DEV_REGISTERED")||registered.startsWith("DEV_RUNNING"),"HISTORICAL_TRIAL_PREREGISTERED_AND_MAY_TRAIN_IN_SAME_SLICE");
            String key="r959_development_job_"+parent,initial=store.getState(key,"");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_TRIAL+" WHERE status='RUNNING' AND sealed_consumed=0")==1,"ONE_PAID_RUNNING_TRIAL_NO_SEALED_TOKEN");
            // Failure after computation must roll back state, cursor, output child and trial together.
            db.execSQL("CREATE TRIGGER fail_dev_checkpoint BEFORE INSERT ON "+DbHelper.TABLE_APP_STATE+" WHEN NEW.state_key='"+key+"' BEGIN SELECT RAISE(ABORT,'INJECTED_DEV_CHECKPOINT_FAILURE'); END");
            String failed=ResearchDynamicBootstrapR922.pulse(store,1000).status;
            check(failed.contains("COMMIT_FAILED"),"CHECKPOINT_FAILURE_IS_VISIBLE");
            check(store.getState(key,"").equals(initial)&&n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==1,"FAILED_CHECKPOINT_PRESERVES_STATE_AND_CURSOR");
            db.execSQL("DROP TRIGGER fail_dev_checkpoint");
            String later=TargetIssueResolver.next(base).targetIssue;draw(db,later,random);
            check(ResearchDynamicBootstrapR922.pulse(store,1000).status.startsWith("DEV_RUNNING"),"BOUNDED_PARTIAL_NUMERIC_CHECKPOINT");
            JSONObject partial=new JSONObject(store.getState(key,""));
            check(partial.getInt("n")>0&&partial.getInt("n")<240,"PARTIAL_CURSOR_AND_NUMERIC_STATE_PERSIST_BEFORE_RESTART");
            int retries=0;String result="";
            for(;retries<120;retries++){
                store=new DbStore(db); // Same persistent DB, no owner in-memory training state.
                result=ResearchDynamicBootstrapR922.pulse(store,1000).status;
                if(result.startsWith("ERROR"))throw new AssertionError(result);
                if(result.startsWith("DEV_COMPLETED"))break;
            }
            check(retries<120,"NUMERIC_TRAINING_FINISHES_WITH_RECREATED_OWNER");
            JSONObject job=new JSONObject(store.getState(key,""));
            check(job.getInt("n")==240&&job.getInt("cursor")==320,"REAL_240_CHRONOLOGICAL_LABEL_UPDATES");
            check(job.getString("birth").equals(base),"LATER_DRAW_NOT_ADDED_TO_FROZEN_TRAINING_DATA");
            String childId=job.getString("output_child");AlgorithmGenomeR908 child;String state="";
            try(Cursor c=db.rawQuery("SELECT genome_spec,state_blob,dev_n,state_updates,future_n,earliest_use_issue FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{childId})){
                check(c.moveToFirst(),"TRAINED_CHILD_DURABLE");child=AlgorithmGenomeR908.parse(c.getString(0));state=c.getString(1);
                check(c.getInt(2)==240&&c.getInt(3)==0&&c.getInt(4)==0,"DEV_COUNTER_SEPARATE_FROM_LIVE_FUTURE_COUNTERS");
                check(c.getString(5).equals(TargetIssueResolver.next(later).targetIssue),"CHILD_ONLY_USABLE_AFTER_ACTUAL_PUBLICATION");
            }
            check(!child.structuralHash.equals(legacy.structuralHash)&&!state.equals(new AlgorithmGenomeVmR908.State().encode()),"REAL_NUMERIC_PARAMETERS_CHANGE_ARTIFACT_ID");
            check(child.initialParameters.equals(state)&&AlgorithmGenomeVmR908.initialState(child).encode().equals(state),"RELOADED_VM_USES_TRAINED_PARAMETERS");
            check(n(db,"SELECT COUNT(*) FROM "+AlgorithmProofRecoveryR948.HEAD+" WHERE genome_id='"+childId+"' AND status='CLEAN'")==1,"TRAINED_INITIAL_HEAD_ACCEPTED_WITH_FUTURE_ZERO");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_TRIAL+" WHERE status='COMPLETED' AND output_hash<>''")==1,"TRAINING_ARTIFACT_AND_TRIAL_COMMIT_TOGETHER");
            check(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW)==0&&n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"DEVELOPMENT_NEVER_FABRICATES_FUTURE_OR_CERTIFICATE");
            check(ResearchDynamicBootstrapR922.pulse(store,1000).status.startsWith("IDLE"),"COMPLETED_PARENT_NOT_TRAINED_OR_REGISTERED_TWICE");
            long changes=n(db,"SELECT total_changes()");
            String diagnostics=com.ethk3.novaonepro.r952.FullbloodDiagnosticsProviderR952.render(store);
            check(diagnostics.contains("numericUpdates=240")&&diagnostics.contains("outputChild="+childId)
                    &&diagnostics.contains("stateHash="+V51ChecksumEngine.sha256(job.getString("state")))
                    &&diagnostics.contains("HISTORICAL_DEVELOPMENT_NOT_FUTURE_OR_CERTIFICATE"),"REPORT_EXPOSES_ACTUAL_TRAINED_STATE_AND_SEPARATE_EVIDENCE_ROLE");
            check(n(db,"SELECT total_changes()")==changes,"TRAINING_DIAGNOSTICS_IS_READ_ONLY");
            String liveBase=later;
            for(int period=0;period<4;period++) {
                String target=TargetIssueResolver.next(liveBase).targetIssue;
                DeferredEvidenceSnapshotR36.Snapshot surface=ReportedCaptureHarnessR945.snapshot(liveBase,target);
                byte[] blob=DeferredEvidenceSnapshotR36.encode(surface);
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue,job_type,status,created_at,updated_at,record_hash,snapshot_blob,snapshot_hash) VALUES(?,?,?,0,0,?,?,?)",new Object[]{target,"SYNTHETIC_TRAINED_CHILD","PENDING",surface.recordHash,blob,DeferredEvidenceSnapshotR36.sha256(blob)});
                for(int retry=0;retry<30;retry++) {
                    try{ResearchPredrawCaptureR923.captureTarget(store,target,1600);}
                    catch(ResearchWorkBudgetR927.Yield yielded){}
                    if(ResearchCaptureQueueR946.complete(store,target))break;
                }
                check(ResearchCaptureQueueR946.complete(store,target),"TRAINED_CHILD_USES_REAL_CAPTURE_PATH:"+period);
                if(period==3)check("VALID".equals(CandidateExecutionAuditR932.verifyAlgorithm(db,childId,target)),"TRAINED_INITIAL_PARAMETERS_PASS_FULL1200_EXECUTION_PROOF");
                draw(db,target,random);
                if(period<3) {
                    db.beginTransaction();try{ResearchLifecycleR932.review(db,"algorithm:"+parent,target);ResearchLifecycleR932.review(db,"algorithm:"+childId,target);db.setTransactionSuccessful();}finally{db.endTransaction();}
                } else {
                    AlgorithmInventionLabR908.settleDraw(store,store.getDrawByIssue(target));
                    AlgorithmInventionLabR908.settleDraw(store,store.getDrawByIssue(target));
                    check(n(db,"SELECT future_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+childId+"'")==1,"TRAINED_CHILD_LEARNS_ONE_NEW_FROZEN_LABEL_EXACTLY_ONCE");
                    check(n(db,"SELECT dev_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+childId+"'")==240,"FUTURE_LEARNING_PRESERVES_SEPARATE_HISTORICAL_N");
                }
                liveBase=target;
            }
            check(DirectedResearchCreationR932.maintain(store,"algorithm").startsWith("CREATED_N0|"),"NEXT_DRAW_RUNS_NEW_REGISTERED_CREATION_ATTEMPT");
            long count=n(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT);
            DirectedResearchCreationR932.maintain(store,"algorithm");
            check(n(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT)==count,"SAME_DRAW_RETRY_DOES_NOT_REGENERATE");
            System.out.println("R959_DEVELOPMENT_TRAINING_PASS|checks="+checks+"|retries="+retries+"|scope=SYNTHETIC_REAL_VM_SQLITE_NOT_LIVE");
        }finally{db.shutdown();}
    }
}
