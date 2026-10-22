package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Actual VM/SQLite production methods on synthetic draws, not live evidence. */
public final class DevelopmentCapacityHarnessR966 {
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
            // Fill the remaining slots with explicitly synthetic, unobserved research fixtures.
            db.beginTransaction();try{
                for(int i=0;i<63;i++){
                    AlgorithmGenomeR908 filler=AlgorithmGenomeR908.seed(296600+i,1,"CAPACITY_FIXTURE");
                    AlgorithmInventionLabR908.registerDirected(db,filler,base,later,"FIXTURE","SYNTHETIC_CAPACITY");
                    ResearchLifecycleR932.enroll(db,"algorithm:"+filler.genomeId,filler.structuralHash,later,"C");
                }
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            check(ResearchPopulationR966.active(db)==64,"TRAINING_POOL_FULL_OF_IMMATURE_PROGRAMS");
            int retries=0;String result="";
            for(;retries<120;retries++){
                store=new DbStore(db);result=ResearchDynamicBootstrapR922.pulse(store,1000).status;
                if(result.startsWith("ERROR"))throw new AssertionError(result);
                if(result.startsWith("DEV_WAIT_POPULATION_CAPACITY"))break;
            }
            check(result.startsWith("DEV_WAIT_POPULATION_CAPACITY"),"FINISHED_NUMERIC_TRAINING_WAITS_FOR_CAPACITY");
            JSONObject waiting=new JSONObject(store.getState(key,""));
            check(waiting.getInt("n")==240&&waiting.getInt("cursor")==320&&ResearchPopulationR966.active(db)==64,"WAIT_PERSISTS_COMPLETE_STATE_WITHOUT_EXTRA_CHILD");
            check(!store.getState("r959_development_done_"+parent,"").equals("DONE"),"WAIT_IS_NOT_FALSE_COMPLETION");
            String state=waiting.getString("state");store=new DbStore(db);result=ResearchDynamicBootstrapR922.pulse(store,1000).status;
            check(result.startsWith("DEV_WAIT_POPULATION_CAPACITY")&&new JSONObject(store.getState(key,"")).getString("state").equals(state),"RECREATED_OWNER_DOES_NOT_RETRAIN_FINISHED_STATE");
            db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET future_n=120,future_hit=40 WHERE genome_id=(SELECT genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id<>? LIMIT 1)",new Object[]{parent});
            result=ResearchDynamicBootstrapR922.pulse(store,1000).status;
            check(result.startsWith("DEV_COMPLETED")&&ResearchPopulationR966.active(db)==64,"MATURE_RESOURCE_REPLACEMENT_PUBLISHES_FINISHED_TRAINING");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.TABLE+" WHERE state='RESOURCE_RETIRED'")==1,"EXACTLY_ONE_RESOURCE_RETIREMENT");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"TRAINING_WAIT_NEVER_MINTS_CERTIFICATES");
            System.out.println("R966_DEVELOPMENT_CAPACITY_PASS|checks="+checks+"|SYNTHETIC_FIXTURES_REAL_NUMERIC_TRAINING_AND_SQLITE");
        }finally{db.shutdown();}
    }
}
