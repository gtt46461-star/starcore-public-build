package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
/** Actual product DDL and owners against SQLite. Fault/migration fixtures are NOT prediction evidence. */
public final class StreamInvariantHarnessR969 {
    interface Op{void run();} static int checks;
    static void check(boolean v,String name){if(!v)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static String h(String s){return V51ChecksumEngine.sha256(s);}
    static long n(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static void reject(String name,Op f){boolean rejected=false;try{f.run();}catch(RuntimeException e){rejected=true;}check(rejected,name);}
    static void tx(SQLiteDatabase d,Op f){d.beginTransaction();try{f.run();d.setTransactionSuccessful();}finally{d.endTransaction();}}
    static DbStore setup(SQLiteDatabase d){
        DbStore store=new DbStore(d);DbHelper.ensureR35Schema(d);DbHelper.ensureR36Schema(d);RuntimeEventLedgerR926.ensureSchema(d);DbHelper.ensureR906CriticalSchema(d);V51Repository.ensure(d);
        d.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER)");
        RealtimePressureGateR811.resetForTests();check(ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorR36(),8),"STREAM_PRODUCT_SCHEMA_READY");
        tx(d,()->{String issue="202609190001";for(int i=0;i<120;i++){VersionedEvaluationHarnessR969.draw(d,issue,System.currentTimeMillis()-10000000L+i*60000);issue=TargetIssueResolver.next(issue).targetIssue;}});
        return store;
    }
    static void basic(String bridge)throws Exception{
        final SQLiteDatabase d=new SQLiteDatabase(bridge);try{
            final DbStore store=setup(d);final String target="202609190121";
            check(ResearchDirectorR952.capturePredraw(store,target,h("SYNTHETIC_BASIC_CAPTURE"),8)==1,"ACTUAL_FIRST_CAPTURE_BINDS_ONE_RUN_AND_INPUT");
            final String id=text(d,"SELECT candidate_id FROM r952_candidate"),input=text(d,"SELECT input_hash FROM "+ResearchEvaluationRunR967.INPUT),model=text(d,"SELECT model_hash FROM "+ResearchEvaluationRunR967.INPUT);
            ResearchEvaluationRunR967.Run run=ResearchEvaluationRunR967.requireTrain(d,id,target);
            check(!run.epoch.equals(h("NOT_ALGORITHM"))&&run.epoch.matches("[0-9a-f]{64}"),"ORIGIN_BOUND_TO_REAL_RUN_NOT_R948_WRONG_NAMESPACE");
            ResearchEvaluationRunR967.verifyInput(d,id,target,input,model);check(true,"ACTUAL_CAPTURE_IDENTITY_REPLAY_VALID");
            check(ResearchEvaluationRunR967.enroll(store,target)==0&&ResearchDirectorR952.capturePredraw(store,target,h("RETRY"),8)==0,"REPEATED_REGISTRATION_AND_CAPTURE_DO_NOT_RESET");
            for(String table:new String[]{ResearchEvaluationRunR967.RUN,ResearchEvaluationRunR967.INPUT}){
                reject(table+"_UPDATE",()->d.execSQL("UPDATE "+table+" SET "+(table.equals(ResearchEvaluationRunR967.RUN)?"rule_hash":"input_hash")+"='changed'"));
                reject(table+"_DELETE",()->d.execSQL("DELETE FROM "+table));
                reject(table+"_REPLACE",()->d.execSQL("INSERT OR REPLACE INTO "+table+" SELECT * FROM "+table));
            }
            reject("CHANGED_INPUT_REJECTED",()->ResearchEvaluationRunR967.verifyInput(d,id,target,h("different"),model));
            reject("CHANGED_MODEL_REJECTED",()->ResearchEvaluationRunR967.verifyInput(d,id,target,input,h("different")));
            reject("EVAL_ROLE_CANNOT_TRAIN",()->ResearchEvaluationRunR967.requireTrain(d,id,"202609190122"));
            reject("EVAL_ROLE_CANNOT_FEEDBACK",()->ResearchEvaluationRunR967.verifyRecordedInput(d,id,"202609190122",input,model));
            d.execSQL("UPDATE r952_candidate SET state='RESOURCE_RETIRED' WHERE candidate_id=?",new Object[]{id});reject("RETIRED_PARENT_CANNOT_NEW_TRAIN",()->ResearchEvaluationRunR967.requireTrain(d,id,target));
            d.execSQL("UPDATE r952_candidate SET state='NURSERY' WHERE candidate_id=?",new Object[]{id});
            VersionedEvaluationHarnessR969.settle(store,d,target);check(ResearchOutboxR952.drainOne(store),"V2_ACTUAL_NUMERIC_COMMIT");
            d.execSQL("CREATE TRIGGER inject_v2_post_ack BEFORE UPDATE ON "+ResearchTaskQueueR967.TABLE+" WHEN NEW.status='COMPLETED' BEGIN SELECT RAISE(ABORT,'V2_POST_ACK_CRASH'); END");
            check(!PostSettlementResearchLoopR952.drainOne(store),"V2_POST_CRASH_DEFERRED");
            check(n(d,"SELECT COUNT(*) FROM r952_knowledge")==0&&n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE status='COMPLETED'")==0,"V2_FEEDBACK_AND_ACK_ROLL_BACK_TOGETHER");
            d.execSQL("DROP TRIGGER inject_v2_post_ack");d.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET available_at=0 WHERE status='YIELDED'");
            check(PostSettlementResearchLoopR952.drainOne(store),"V2_POST_CRASH_RETRY_FINISHES");
            check(n(d,"SELECT COUNT(*) FROM r952_knowledge")==1&&n(d,"SELECT MAX(numeric_updates) FROM r952_artifact")==1,"V2_FEEDBACK_DOES_NOT_RETRAIN");
            check(!PostSettlementResearchLoopR952.drainOne(store),"V2_POST_REPLAY_NO_DUPLICATE");
            VersionedEvaluationHarnessR969.settle(store,d,"202609190122");
            d.execSQL("CREATE TRIGGER inject_input BEFORE INSERT ON "+ResearchEvaluationRunR967.INPUT+" BEGIN SELECT RAISE(ABORT,'INPUT_WRITE_CRASH'); END");
            reject("PREDRAW_SIDECAR_FAILURE_PROPAGATES",()->ResearchDirectorR952.capturePredraw(store,"202609190123",h("NEW"),8));
            check(n(d,"SELECT COUNT(*) FROM r952_candidate_predraw WHERE target_issue='202609190123'")==0,"PREDRAW_AND_SIDECAR_ATOMIC_ROLLBACK");
            d.execSQL("DROP TRIGGER inject_input");check(ResearchDirectorR952.capturePredraw(store,"202609190123",h("NEW"),8)==1,"CAPTURE_RECOVERS_AFTER_SIDECAR_FAULT");
            for(String table:new String[]{ResearchEvaluationRunR967.RUN,ResearchEvaluationRunR967.INPUT,ResearchEvaluationRunR967.EVAL})for(String op:new String[]{"no_update","no_delete"}){
                d.execSQL("DROP TRIGGER "+table+"_"+op);
                check(!SchemaDomainCoordinatorR926.matches(d,SchemaDomainCoordinatorR926.researchContract()),"MISSING_TRIGGER_NOT_READY:"+table+'_'+op);
                check(ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorR36(),8),"MISSING_TRIGGER_REPAIRED:"+table+'_'+op);
            }
            check(n(d,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.RUN)==1&&n(d,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.INPUT)==2,"TRIGGER_REPAIR_PRESERVES_IMMUTABLE_HISTORY");
            d.execSQL("DROP TABLE "+ResearchEvaluationRunR967.RUN);ResearchEvaluationRunR967.schema(d);
            reject("LOST_RUN_DOES_NOT_REBIND_CAPTURED_INPUT",()->ResearchEvaluationRunR967.verifyRecordedInput(d,id,target,input,model));
            // A completed old draw cannot be used to recreate a missing run.
            check(ResearchEvaluationRunR967.enroll(store,"202609190119")==0,"LOST_RUN_BLOCKED_PENDING_INPUT_POSTDRAW_NOOP");
            check(n(d,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.RUN)==0,"NO_FABRICATED_RUN_AFTER_LOSS");
        }finally{d.shutdown();}
    }
    static void upgrade(String bridge)throws Exception{
        final SQLiteDatabase d=new SQLiteDatabase(bridge);try{
            final DbStore store=setup(d);final String first="202609190121",next="202609190123";
            final CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.RECENCY_BLEND,1,120);final String id="R952-C-"+ast.structuralHash.substring(0,20);
            final String oldRule=h("EXPLICIT_OLD_RULE_FIXTURE"),initial=CandidateCompilerR952.Model.initial().payload(),model=h(initial),input=h("OLD_PENDING_INPUT");
            final String epoch=new ResearchSemanticsR967.Builder("R952_NUMERIC_RUN_ORIGIN_V1").text("learner",id).text("first",first).hash("ast",ast.structuralHash).hash("rule",oldRule).hash("initial",h(initial)).text("artifact","").digest();
            final String run=new ResearchSemanticsR967.Builder("R967_NUMERIC_RUN_V2").text("learner",id).text("first",first).hash("ast",ast.structuralHash).hash("rule",oldRule).hash("initial",h(initial)).text("artifact","").text("split",ResearchEvaluationRunR967.SPLIT).hash("epoch",epoch).digest();
            final String record=new ResearchSemanticsR967.Builder("R967_TRAIN_INPUT_V2").hash("run",run).text("learner",id).text("target",first).hash("input",input).hash("model",model).digest();
            // Handwritten fixture represents an old pending queue, not a training/calibration/certificate result.
            tx(d,()->{
                String intent=ResearchIntentLedgerR952.put(d,"MIGRATION_FIXTURE","","N=0","NONE","RECENCY_BLEND",1,first);
                d.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,parent_id,state,future_n,created_issue,may_create_real,created_at) VALUES(?,?,?,?,?,'NURSERY',0,?,0,?)",new Object[]{id,intent,ast.canonical,ast.structuralHash,"",first,System.currentTimeMillis()});
                ResearchLifecycleR932.enrollR952(d,"r952:"+id,ast.structuralHash,first);
                d.execSQL("INSERT INTO "+ResearchEvaluationRunR967.RUN+" VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{run,id,first,ast.structuralHash,oldRule,initial,0,"",ResearchEvaluationRunR967.SPLIT,epoch,System.currentTimeMillis()});
                d.execSQL("INSERT INTO r952_candidate_predraw(target_issue,candidate_id,prediction_mask,position,snapshot_hash,structure_hash,model_hash,code_hash,data_hash,captured_at,frozen_features,input_hash,evidence_role) VALUES(?,?,7,1,?,?,?,?,?,?,?,?,?)",new Object[]{first,id,h("OLD_SNAPSHOT"),ast.structuralHash,model,oldRule,h("OLD_DATASET"),System.currentTimeMillis(),"OLD_FEATURE_FIXTURE_NOT_TRAINED",input,ResearchEvaluationRunR967.ROLE});
                d.execSQL("INSERT INTO "+ResearchEvaluationRunR967.INPUT+" VALUES(?,?,?,?,?,?)",new Object[]{id,first,run,input,model,record});
                ResearchOutboxR952.enqueue(d,first,id,h("OLD_SETTLEMENT"),h("OLD_LABEL"),input);
            });
            check(ResearchEvaluationRunR967.current(d,id,first).hash.equals(run),"OLD_RULE_FIXTURE_PASSES_TYPED_ORIGIN_REPLAY");
            reject("OLD_RULE_CANNOT_RUN_NEW_NUMERIC_CODE",()->ResearchEvaluationRunR967.requireTrain(d,id,first));
            d.execSQL("CREATE TRIGGER inject_run BEFORE INSERT ON "+ResearchEvaluationRunR967.RUN+" BEGIN SELECT RAISE(ABORT,'RUN_UPGRADE_WRITE_FAULT'); END");
            reject("UPGRADE_ATOMIC_INSERT_FAULT",()->ResearchEvaluationRunR967.enroll(store,next));
            check(text(d,"SELECT state FROM r952_research_outbox").equals("PENDING")&&text(d,"SELECT proof_error FROM r952_candidate_predraw").isEmpty(),"UPGRADE_ROLLBACK_RESTORES_PRIOR_QUEUE_AND_CAPTURE");
            d.execSQL("DROP TRIGGER inject_run");
            check(ResearchEvaluationRunR967.enroll(store,next)==1,"PROSPECTIVE_NEW_RULE_RUN_REGISTERED");
            check(text(d,"SELECT state FROM r952_research_outbox").equals("DEPENDENCY_RETIRED"),"OLD_PENDING_NOT_COMPLETED_NOT_QUALITY_FAILED");
            check(n(d,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.RUN)==2&&n(d,"SELECT COUNT(*) FROM r952_artifact")==0,"UPGRADE_PRESERVES_PRIOR_RUN_WITHOUT_FAKE_TRAINING");
            ResearchEvaluationRunR967.Run active=ResearchEvaluationRunR967.requireTrain(d,id,next);check(!active.hash.equals(run)&&active.initialUpdates==0,"NEW_RUN_HAS_DISTINCT_PROVENANCE_NO_FUTURE_TRANSFER");
            check(ResearchEvaluationRunR967.enroll(store,next)==0,"UPGRADE_REPLAY_DOES_NOT_RESET_AGAIN");
            ResearchEvaluationRunR967.verifyRecordedInput(d,id,first,input,model);check(true,"HISTORICAL_IDENTITY_REMAINS_VERIFIABLE_NOT_TRAINABLE");
            VersionedEvaluationHarnessR969.settle(store,d,first);VersionedEvaluationHarnessR969.settle(store,d,"202609190122");
            check(ResearchDirectorR952.capturePredraw(store,next,h("NEW_RULE_CAPTURE"),8)==1,"NEW_RUN_REAL_CAPTURE_AFTER_RETIRED_OLD_QUEUE");
            VersionedEvaluationHarnessR969.settle(store,d,next);check(ResearchOutboxR952.drainOne(store),"RETIRED_DEPENDENCY_DOES_NOT_BLOCK_NEW_TRAIN_OWNER");
            check(n(d,"SELECT MAX(numeric_updates) FROM r952_artifact")==1&&n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='COMPLETED'")==1,"ONLY_NEW_VALID_LABEL_WAS_TRAINED");
            check(PostSettlementResearchLoopR952.drainOne(store),"NEW_RUN_FEEDBACK_AFTER_RULE_UPGRADE");
        }finally{d.shutdown();}
    }
    public static void main(String[] a)throws Exception{basic(a[0]);upgrade(a[0]);System.out.println("R969_STREAM_INVARIANTS_PASS checks="+checks+"; old-rule fixture=QUEUE_METADATA_ONLY; host_SQLite=YES; physical_device=NOT_RUN");}
}
