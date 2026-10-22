package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Production entry points on a synthetic PIT stream. This is not live Future evidence. */
public final class ResearchEntryHarnessR958 {
    private static int checks;
    private static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    private static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    private static void draw(SQLiteDatabase db,String issue,Random random){
        ArrayList<Integer> n=new ArrayList<Integer>();for(int i=1;i<=10;i++)n.add(i);Collections.shuffle(n,random);
        Object[] values=new Object[14];values[0]=issue;for(int i=0;i<10;i++)values[i+1]=n.get(i);
        values[11]=0;values[12]=System.currentTimeMillis()+1;values[13]=System.currentTimeMillis()+2;
        db.execSQL("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",values);
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try {
            DbStore store=new DbStore(db);DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);
            RuntimeEventLedgerR926.ensureSchema(db);DbHelper.ensureR906CriticalSchema(db);V51Repository.ensure(db);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER)");
            // Empty legacy base table normally established by DbHelper.onCreate, before Research.
            db.execSQL("CREATE TABLE miss_reason_sample(target_issue TEXT,created_at INTEGER,position INTEGER,actual_number INTEGER,window_wrong_score REAL,position_wrong_score REAL,main_number_wrong_score REAL,third_drag_score REAL,counter_ignored_score REAL)");
            RealtimePressureGateR811.resetForTests();
            check(ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorR36(),8),"FULL_RUNTIME_SCHEMA_READY");
            Random random=new Random(958);String base="202609190001";
            db.beginTransaction();try{for(int i=0;i<320;i++){draw(db,base,random);if(i<319)base=TargetIssueResolver.next(base).targetIssue;}db.setTransactionSuccessful();}finally{db.endTransaction();}
            for(String lane:new String[]{"algorithm","genesis","formula"}){
                String result=DirectedResearchCreationR932.maintain(store,lane);System.out.println("CREATION "+lane+" "+result);
                check(result.startsWith("CREATED_N0|"),"REAL_CREATION_ENTRY:"+lane);
                check(ResearchEvolutionHeartbeatR947.isCreated(result),"ACTUAL_CREATION_RESULT_RECOGNIZED:"+lane);
            }
            check(!ResearchEvolutionHeartbeatR947.isCreated("GENERATED=0")&&!ResearchEvolutionHeartbeatR947.isCreated("CREATED=0"),"ZERO_GENERATION_IS_NOT_NEW_CHILD");
            check(ResearchEvolutionHeartbeatR947.isCreated("DONE|GENERATED=2|N=0")&&!ResearchEvolutionHeartbeatR947.isCreated("DONE|GENERATED=0|N=0"),"STRUCTURED_GENERATION_COUNT_PARSED");
            check(count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==1,"ONE_NEW_ALGORITHM_REGISTERED");
            check(count(db,"SELECT SUM(future_n) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==0,"CREATION_DOES_NOT_FABRICATE_FUTURE");
            for(int period=0;period<6;period++){
                String target=TargetIssueResolver.next(base).targetIssue;
                DeferredEvidenceSnapshotR36.Snapshot snapshot=ReportedCaptureHarnessR945.snapshot(base,target);
                byte[] blob=DeferredEvidenceSnapshotR36.encode(snapshot);
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue,job_type,status,created_at,updated_at,record_hash,snapshot_blob,snapshot_hash) VALUES(?,?,?,0,0,?,?,?)",new Object[]{target,"SYNTHETIC_TEST","PENDING",snapshot.recordHash,blob,DeferredEvidenceSnapshotR36.sha256(blob)});
                for(int retry=0;retry<24;retry++){
                    try{System.out.println("CAPTURE "+ResearchPredrawCaptureR923.captureTarget(store,target,1600).canonical());}
                    catch(ResearchWorkBudgetR927.Yield yielded){System.out.println("CAPTURE YIELD "+yielded.getMessage());}
                    if(ResearchCaptureQueueR946.complete(store,target))break;
                }
                check(ResearchCaptureQueueR946.complete(store,target),"ALL_ADMITTED_MODES_DURABLE:"+period);
                if(period<3) {
                    check(count(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.PROXY+" WHERE target_issue='"+target+"'")==3,"NURSERY_THREE_LANES_PROXY120:"+period);
                    check(count(db,"SELECT COUNT(*) FROM "+CandidateExecutionAuditR932.TABLE+" WHERE target_issue='"+target+"'")==0,"PROXY_CANNOT_BECOME_FULL1200_EVIDENCE:"+period);
                    check(count(db,"SELECT SUM(state_updates) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==0,"NO_PROXY_LABELS_USED_FOR_TRAINING:"+period);
                    draw(db,target,random);
                    db.beginTransaction();try{java.util.ArrayList<String> keys=new java.util.ArrayList<String>();try(Cursor c=db.rawQuery("SELECT candidate_key FROM "+GlobalResearchScheduleR932.TASK+" WHERE target_issue=?",new String[]{target})){while(c.moveToNext())keys.add(c.getString(0));}for(String key:keys)ResearchLifecycleR932.review(db,key,target);db.setTransactionSuccessful();}finally{db.endTransaction();}
                    base=target;continue;
                }
                check(count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE target_issue='"+target+"'")==1,"FULL_ENTRY_FREEZES_NEW_ALGORITHM:"+period);
                check(count(db,"SELECT COUNT(*) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" WHERE target_issue='"+target+"'")==1,"FULL_ENTRY_FREEZES_NEW_ASSET:"+period);
                check(count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE target_issue='"+target+"' AND candidate_id IN(SELECT asset_id FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+")")==1,"FULL_ENTRY_FREEZES_NEW_FORMULA:"+period);
                check(count(db,"SELECT COUNT(*) FROM "+CandidateExecutionAuditR932.TABLE+" WHERE target_issue='"+target+"' AND action_count=1200")>=2,"FULL1200_EXECUTION_PROOFS_FROM_ACTUAL_VM:"+period);
                draw(db,target,random);Models.Draw actual=store.getDrawByIssue(target);
                AlgorithmInventionLabR908.settleDraw(store,actual);AssetGenesisEngineR915.settleDraw(store,actual);
                ResearchShadowChallengerRuntimeR817.settle(store,actual);
                check(count(db,"SELECT MIN(state_updates) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==period-2,"NEW_ALGORITHM_LEARNS_NEXT_LABEL:"+period);
                check(count(db,"SELECT MIN(state_updates) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME)==period-2,"NEW_ASSET_LEARNS_NEXT_LABEL:"+period);
                check(count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE target_issue='"+target+"' AND hit_label IN(0,1) AND candidate_id IN(SELECT asset_id FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+")")==1,"NEW_FORMULA_RECEIVES_EXACT_SETTLEMENT:"+period);
                AlgorithmInventionLabR908.settleDraw(store,actual);
                check(count(db,"SELECT MIN(state_updates) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==period-2,"REPEATED_LABEL_NOT_TRAINED_TWICE:"+period);
                base=target;
            }
            System.out.println("R958_RESEARCH_ENTRY_PASS|checks="+checks+"|scope=PRODUCTION_ENTRY_VM_SQLITE_SYNTHETIC_NOT_LIVE");
        }finally{db.shutdown();}
    }
}
