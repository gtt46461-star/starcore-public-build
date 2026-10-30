package com.ethk3.novaonepro.strategyv2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual API34 SQLite installer/routing/trigger/SQLiteSession probe on isolated synthetic files. */
@RunWith(AndroidJUnit4.class)
public final class R29LegacySchemaOwnerDeviceTest {
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    static boolean column(SQLiteDatabase db,String table,String name){try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())if(name.equals(c.getString(1)))return true;return false;}}
    static ContentValues row(SQLiteDatabase db,String table,String issue){
        ContentValues v=new ContentValues();try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext()){
            String name=c.getString(1),type=c.getString(2);if((c.getInt(3)==0&&c.getInt(5)==0)||!c.isNull(4))continue;
            if(type.toUpperCase(Locale.ROOT).contains("INT"))v.put(name,1L);else if(type.toUpperCase(Locale.ROOT).contains("REAL"))v.put(name,0.25);else if(type.toUpperCase(Locale.ROOT).contains("BLOB"))v.put(name,new byte[]{1,2,3});else v.put(name,"SYNTHETIC_"+name);
        }}v.put("target_issue",issue);if(column(db,table,"game_code"))v.put("game_code",ReleaseManifestR926.GAME_CODE);if(column(db,table,"n1")){v.put("n1",1);v.put("n2",2);v.put("n3",3);}return v;
    }
    static void blocked(SQLiteDatabase db,String sql,String code){boolean blocked=false;try{db.execSQL(sql);}catch(android.database.sqlite.SQLiteException expected){assertTrue(expected.toString(),expected.toString().contains(code));blocked=true;}assertTrue("mutation did not hit actual SQLite guard: "+sql,blocked);}
    static void learned(SQLiteDatabase db,String issue,String source){
        String id=StatsR27.sha256("SYNTHETIC_LEARNED|"+issue+"|"+source),payload="scope=SYNTHETIC_ISOLATED_ANDROID_NOT_NATURAL";
        db.execSQL("INSERT INTO "+RuntimeEventLedgerR926.TABLE_EVENT+"(event_id,dedup_key,game_code,target_issue,epoch_id,attempt_id,event_type,stage,state_rank,payload_text,payload_hash,source,created_at) VALUES(?,?,?,?,?,?,'LEARNED','LEARNED_EXACTLY_ONCE',90,?,?,?,1)",new Object[]{id,id,ReleaseManifestR926.GAME_CODE,issue,"synthetic-epoch","synthetic-attempt",payload,StatsR27.sha256(payload),source});
    }
    static boolean advance(SQLiteDatabase db)throws Exception{
        Method m=ResearchSchemaCoordinatorR926.class.getDeclaredMethod("advance",SQLiteDatabase.class);m.setAccessible(true);
        try{return(Boolean)m.invoke(null,db);}catch(InvocationTargetException failure){Throwable cause=failure.getCause();if(cause instanceof Exception)throw(Exception)cause;throw failure;}
    }
    static DatabaseCoordinator.TransactionContext transaction()throws Exception{
        Constructor<DatabaseCoordinator.TransactionContext> c=DatabaseCoordinator.TransactionContext.class.getDeclaredConstructor(String.class,String.class,String.class);c.setAccessible(true);
        return c.newInstance("SYNTHETIC_R29_NESTED_PROBE","202610220003",StatsR27.sha256("synthetic-inner-rollback"));
    }
    @Test public void completeResearchSchemaAndOwnerGuardsOnAndroid()throws Exception{
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertNull(DatabaseCoordinator.peek());
        R26RuntimeDeviceTest.Sandbox sandbox=new R26RuntimeDeviceTest.Sandbox(target);DbStore store=new DbStore(sandbox);
        ResearchPoolR27 pool=new ResearchPoolR27(store,"SYNTHETIC_R29_SCHEMA_OWNER",2);SQLiteDatabase workerReader=null,rootReader=null,rp=null;
        Map<String,Object> result=new LinkedHashMap<>();result.put("scope","API34_SYNTHETIC_ANDROID_SQLITE_NOT_PHYSICAL_NOT_NATURAL");
        try{
            final String ftrlIssue="202610220001",tinyIssue="202610220002";
            DatabaseCoordinator owner=store.coordinatorForResearchR952();
            owner.runAtomicWriteOrThrowF01("SYNTHETIC_R29_SCHEMA_OWNER_ROOT",db->{
                RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);DbHelper.ensureR906CriticalSchema(db);RuntimeEventLedgerR926.ensureSchema(db);
                DbHelper.ensureR811ResearchSchema(db);DbHelper.ensureR815ResearchSchema(db);DbHelper.ensureR906ResearchSchema(db);SchemaRevisionR925.ensureCriticalTables(db);
                db.insertOrThrow(DbHelper.TABLE_R35_OOS_LABEL,null,row(db,DbHelper.TABLE_R35_OOS_LABEL,ftrlIssue));
                db.insertOrThrow(DbHelper.TABLE_R906_SELECTED_EVIDENCE,null,row(db,DbHelper.TABLE_R906_SELECTED_EVIDENCE,ftrlIssue));
                ContentValues tiny=row(db,DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL,tinyIssue);tiny.put("asset_id","PRODUCTION_SELECTED");db.insertOrThrow(DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL,null,tiny);
                learned(db,ftrlIssue,"ResearchAdaptiveLearnerStoreR811");learned(db,tinyIssue,"ResearchTinyDeepLearnerR909");return 0;
            });
            // Independent incomplete RP: real schema/install, no copied Root event table and no invented ready bit.
            File pending=new File(sandbox.getCacheDir(),"pending-research.db");
            try(SQLiteDatabase incomplete=SQLiteDatabase.openOrCreateDatabase(pending,null)){
                incomplete.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER DEFAULT 0)");incomplete.execSQL(ResearchRouteR29.OWNER_DDL);incomplete.execSQL("INSERT INTO r29_research_owner VALUES(1,'COPYING',?,?,42,1)",new Object[]{StatsR27.sha256("pending-schema"),StatsR27.sha256("pending-source")});
                incomplete.beginTransaction();try{DbHelper.ensureR906ResearchSchema(incomplete);LegacyLearnedInputR29.installResearch(incomplete);incomplete.setTransactionSuccessful();}finally{incomplete.endTransaction();}
                assertFalse(LegacyLearnedInputR29.ready(incomplete));assertFalse(table(incomplete,RuntimeEventLedgerR926.TABLE_EVENT));
                ContentValues tiny=row(incomplete,DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL,"202610220004");tiny.put("asset_id","PRODUCTION_SELECTED");boolean denied=false;
                try{incomplete.insertOrThrow(DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL,null,tiny);}catch(android.database.sqlite.SQLiteException expected){assertTrue(expected.toString(),expected.toString().contains("R29_TINY_LABEL_BOOTSTRAP_OR_LEARNED"));denied=true;}assertTrue(denied);
                incomplete.beginTransaction();try{assertFalse(advance(incomplete));assertTrue(ResearchSchemaCoordinatorR926.summary(),ResearchSchemaCoordinatorR926.summary().contains("R29_LEARNED_INPUT_BOOTSTRAP"));incomplete.setTransactionSuccessful();}finally{incomplete.endTransaction();}
            }
            result.put("bootstrapFailClosed",true);
            rootReader=SQLiteDatabase.openDatabase(store.readable().getPath(),null,SQLiteDatabase.OPEN_READONLY);workerReader=ResearchRouteR29.openWorkerReader(sandbox);
            ResearchPoolR27.Worker worker=pool.new Worker(workerReader,rootReader,"synthetic-schema-cutover");
            assertEquals("ACTIVE_INDEPENDENT_RESEARCH_WRITER",ResearchCutoverR29.run(store,worker));workerReader.close();workerReader=null;
            owner.runAtomicWriteOrThrowF01("SYNTHETIC_R29_SCHEMA_OWNER_INITIALIZE",db->{LegacyCapabilityAdapterR29.initialize(db,"synthetic-schema-owner");return 0;});
            rp=ResearchRouteR29.openWorkerReader(sandbox);assertTrue(ResearchRouteR29.active(rootReader));
            long began=System.nanoTime(),deadline=began+60_000_000_000L;boolean ready=false,intermediateCommit=false;int steps=0,pendingSteps=0;
            List<Map<String,Object>> measuredSteps=new ArrayList<>();
            try{while(System.nanoTime()<deadline&&!ready){
                String beforeStep=ResearchSchemaCoordinatorR926.summary();long stepBegan=System.nanoTime();
                try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){
                    ResearchSchemaCoordinatorR926.StepResult step=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);ready=step.ready;
                    if(step.pending){
                        pendingSteps++;assertFalse(step.ready);assertNull(step.detail,step.cause);
                        assertEquals("an input dependency cannot count as a schema unit",0,step.committedUnits);
                        assertEquals("WRITER_BUSY",step.status);
                    }else{
                        assertNull(step.detail,step.cause);if(!ready&&step.committedUnits>0)intermediateCommit=true;
                    }
                }
                steps++;String state=ResearchSchemaCoordinatorR926.summary();assertFalse(state,state.startsWith("FAILED"));
                Map<String,Object> timing=new LinkedHashMap<>();timing.put("before",beforeStep);timing.put("after",state);timing.put("elapsedMicros",(System.nanoTime()-stepBegan)/1000);measuredSteps.add(timing);
            }}finally{
                result.put("schemaDeadlineMs",60000);result.put("schemaElapsedBeforeFinalChecksMs",(System.nanoTime()-began)/1000000);
                result.put("schemaStepsBeforeFinalChecks",steps);result.put("schemaPendingStepsBeforeFinalChecks",pendingSteps);result.put("schemaStateBeforeFinalChecks",ResearchSchemaCoordinatorR926.summary());result.put("schemaReachedReadyBeforeFinalChecks",ready);
                measuredSteps.sort((a,b)->Long.compare(((Number)b.get("elapsedMicros")).longValue(),((Number)a.get("elapsedMicros")).longValue()));
                result.put("slowestSchemaSteps",new ArrayList<>(measuredSteps.subList(0,Math.min(24,measuredSteps.size()))));
            }
            assertTrue("complete research installer failed within original 60s service target: "+ResearchSchemaCoordinatorR926.summary(),ready);
            assertTrue("actual intermediate DDL must produce a committed schema unit",intermediateCommit);
            try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){
                ResearchSchemaCoordinatorR926.StepResult noChange=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);assertTrue(noChange.ready);assertEquals(0,noChange.committedUnits);assertNull(noChange.cause);
            }
            result.put("intermediateSchemaCommittedUnits",true);result.put("readySchemaNoProgressUnit",true);
            assertTrue(SchemaDomainCoordinatorR926.matches(rp,SchemaDomainCoordinatorR926.researchContract(rp)));assertEquals(133,n(rp,"SELECT count(*) FROM "+AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY));assertEquals(240,n(rp,"SELECT count(*) FROM "+AssetGenesisEngineR915.TABLE_ATOM_SCHEMA));
            result.put("legacySchemaReady",true);result.put("schemaSteps",steps);result.put("schemaPendingSteps",pendingSteps);result.put("schemaMillis",(System.nanoTime()-began)/1000000);
            String[] forbidden={"trades","decisions","v57_draw_canonical_r811","research_exact_probability_f05","r974_exact_real_receipt",DbHelper.TABLE_R906_SELECTED_EVIDENCE,DbHelper.TABLE_R906_ISSUE_CLOSURE,DbHelper.TABLE_R906_SETTLEMENT_OUTBOX,DbHelper.TABLE_R906_RUNTIME_TRUTH,RuntimeEventLedgerR926.TABLE_EVENT,RuntimeEventLedgerR926.TABLE_PROJECTION,RuntimeEventLedgerR926.TABLE_OUTBOX,SchemaRevisionR925.TABLE_FETCH_LEDGER,SchemaRevisionR925.TABLE_PREDICTION_TIMING,SchemaRevisionR925.TABLE_RUNTIME_EPOCH,"b03_action_event","b03_number_event","b03_permutation_event","b03_prediction_bundle","b03_selected_event","b03_authorization_event","b03_settlement_event","b03_transaction_audit","su1_mainline_binding","su2_mainline_choice"};
            // Critical input mirrors may exist, but this list must remain Root-owned and absent in RP.
            for(String name:forbidden)assertFalse("research installer invented Root authority "+name,table(rp,name));
            result.put("noFakeRootAuthority",true);
            owner.runAtomicWriteOrThrowF01("SYNTHETIC_R29_ROOT_FREEZE_UNCHANGED",db->{
                blocked(db,"UPDATE "+DbHelper.TABLE_R35_OOS_LABEL+" SET hit_label=0","E02C_FTRL_LABEL_FROZEN_AFTER_LEARN");blocked(db,"DELETE FROM "+DbHelper.TABLE_R35_OOS_LABEL,"E02C_FTRL_LABEL_FROZEN_AFTER_LEARN");
                blocked(db,"UPDATE "+DbHelper.TABLE_R906_SELECTED_EVIDENCE+" SET raw_action_p_value=0.5","E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN");blocked(db,"DELETE FROM "+DbHelper.TABLE_R906_SELECTED_EVIDENCE,"E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN");
                boolean denied=false;try{db.insertWithOnConflict(DbHelper.TABLE_R906_SELECTED_EVIDENCE,null,row(db,DbHelper.TABLE_R906_SELECTED_EVIDENCE,ftrlIssue),SQLiteDatabase.CONFLICT_REPLACE);}catch(android.database.sqlite.SQLiteException expected){assertTrue(expected.toString().contains("E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN"));denied=true;}assertTrue(denied);return 0;
            });result.put("rootFtrlFreeze",true);
            ResearchRouteR29.write(store,"SYNTHETIC_R29_HISTORICAL_TINY_FREEZE",db->{
                assertTrue(LegacyLearnedInputR29.ready(db));assertEquals(2,n(db,"SELECT count(*) FROM "+LegacyLearnedInputR29.TABLE));
                blocked(db,"UPDATE "+DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL+" SET hit_label=0 WHERE asset_id='PRODUCTION_SELECTED'","E02C_TINY_LABEL_FROZEN_AFTER_LEARN");blocked(db,"DELETE FROM "+DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL+" WHERE asset_id='PRODUCTION_SELECTED'","E02C_TINY_LABEL_FROZEN_AFTER_LEARN");return 0;
            });result.put("legacyTinyFreeze",true);
            final DatabaseCoordinator.TransactionContext transaction=transaction();
            ResearchRouteR29.write(store,"SYNTHETIC_R29_NESTED_ROLLBACK_ANDROID_SESSION",db->{
                LegacyLearningCommitR29.installResearch(db);db.execSQL("CREATE TABLE r29_nested_transaction_probe(k TEXT PRIMARY KEY)");
                DatabaseCoordinator.TransactionResult<Integer> failed=DatabaseCoordinator.runExistingTransaction(db,transaction,(inner,context)->{inner.execSQL("INSERT INTO r29_nested_transaction_probe VALUES('inner-must-rollback')");throw new IOException("SYNTHETIC_INNER_FAILURE");},-1);
                assertFalse(failed.success);assertNotNull(failed.failureCause);assertEquals("SYNTHETIC_INNER_FAILURE",failed.failureCause.getMessage());assertTrue(failed.code,failed.code.contains("SYNTHETIC_INNER_FAILURE"));assertTrue(db.inTransaction());db.execSQL("INSERT INTO r29_nested_transaction_probe VALUES('outer-must-commit')");return 0;
            });assertEquals(0,n(rp,"SELECT count(*) FROM r29_nested_transaction_probe WHERE k='inner-must-rollback'"));assertEquals(1,n(rp,"SELECT count(*) FROM r29_nested_transaction_probe WHERE k='outer-must-commit'"));
            assertEquals(0,n(rp,"SELECT count(*) FROM "+"r29_research_tx_receipt"+" WHERE idempotency_key='"+transaction.idempotencyKey+"'"));
            for(String name:forbidden)assertFalse("nested transaction invented Root authority "+name,table(rp,name));result.put("nestedResearchTransactionRollback",true);
            result.put("status","PASS_ANDROID_ENGINEERING_SUBSET");
        }finally{
            pool.close();if(workerReader!=null)workerReader.close();if(rp!=null)rp.close();if(rootReader!=null)rootReader.close();store.close();
            try(FileOutputStream out=new FileOutputStream(new File(target.getFilesDir(),"r29-legacy-schema-owner-device.json"))){out.write(JsonR27.write(result).getBytes(java.nio.charset.StandardCharsets.UTF_8));}R26RuntimeDeviceTest.remove(sandbox.root);
        }
    }
}
