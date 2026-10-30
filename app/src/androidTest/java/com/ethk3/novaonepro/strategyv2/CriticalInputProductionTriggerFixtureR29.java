package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.lang.reflect.Method;
import java.util.*;

/** Shared HOST/Android fixture using the actual production table and trigger installers. The sole Root
 * LEARNED row is an explicit synthetic positive control, never a natural or transferred learning receipt. */
public final class CriticalInputProductionTriggerFixtureR29 {
    private CriticalInputProductionTriggerFixtureR29(){}
    interface Body{void run()throws Exception;}
    static void tx(SQLiteDatabase db,Body work)throws Exception{db.beginTransactionNonExclusive();try{work.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static long n(SQLiteDatabase db,String sql){return CriticalInputMirrorR29.scalar(db,sql);}
    static void base(SQLiteDatabase db){
        db.execSQL("CREATE TABLE draws(_id INTEGER PRIMARY KEY AUTOINCREMENT,issue TEXT NOT NULL UNIQUE,open_time INTEGER DEFAULT 0,fetch_time INTEGER DEFAULT 0,source_line TEXT,n1 INTEGER DEFAULT 0,n2 INTEGER DEFAULT 0,n3 INTEGER DEFAULT 0,n4 INTEGER DEFAULT 0,n5 INTEGER DEFAULT 0,n6 INTEGER DEFAULT 0,n7 INTEGER DEFAULT 0,n8 INTEGER DEFAULT 0,n9 INTEGER DEFAULT 0,n10 INTEGER DEFAULT 0,raw_json TEXT,dirty INTEGER DEFAULT 0,dirty_reason TEXT,first_seen_at INTEGER NOT NULL DEFAULT 0,draw_hash_version INTEGER NOT NULL DEFAULT 0,draw_hash_v2 TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER DEFAULT 0)");
    }
    static void oos(SQLiteDatabase root,String issue,int hit){
        root.execSQL("INSERT INTO "+DbHelper.TABLE_R35_OOS_LABEL+"(target_issue,game_code,base_issue,position,n1,n2,n3,selected_hit_p,actual_number,hit_label,strict_oos,pre_draw_frozen,prediction_created_at,draw_open_time,settled_at,brier,provenance_hash,record_hash,error_code) VALUES(?,?,?,1,1,2,3,.4,1,?,1,1,1000,60000,70000,.1,?,?,'')",new Object[]{issue,ReleaseManifestR926.GAME_CODE,"202610290001",hit,"synthetic-provenance-"+issue,"synthetic-record-"+issue});
    }
    static void installRoot(SQLiteDatabase root)throws Exception{
        base(root);tx(root,()->{
            // Actual production DDL, including full OOS columns and its real Critical dependencies.
            DbHelper.ensureR35Schema(root);DbHelper.ensureR36Schema(root);DbHelper.ensureR906CriticalSchema(root);RuntimeEventLedgerR926.ensureSchema(root);SchemaRevisionR925.ensureCriticalTables(root);
            Method inputs=DrawDependencyInputCP12.class.getDeclaredMethod("schemaWithinMigration",SQLiteDatabase.class);inputs.setAccessible(true);inputs.invoke(null,root);
            oos(root,"202610300001",1);oos(root,"202610300002",1);CriticalInputMirrorR29.ensureRealtime(root);
            root.execSQL("INSERT INTO "+RuntimeEventLedgerR926.TABLE_EVENT+"(event_id,dedup_key,game_code,target_issue,epoch_id,attempt_id,event_type,stage,state_rank,payload_text,payload_hash,source,created_at) VALUES('synthetic-positive-event','synthetic-positive-dedup',?,'202610300001','synthetic-epoch','synthetic-attempt','LEARNED','LEARNED_EXACTLY_ONCE',1,'SYNTHETIC_POSITIVE_CONTROL_NOT_NATURAL',?,'ResearchAdaptiveLearnerStoreR811',1)",new Object[]{ReleaseManifestR926.GAME_CODE,StatsR27.sha256("synthetic-positive-only")});
        });
    }
    static boolean rootFrozen(SQLiteDatabase root){try{root.execSQL("DELETE FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE target_issue='202610300001'");return false;}catch(RuntimeException expected){if(!String.valueOf(expected.getMessage()).contains("E02C_FTRL_LABEL_FROZEN_AFTER_LEARN"))throw expected;return true;}}
    static void sync(SQLiteDatabase root,SQLiteDatabase research)throws Exception{int turns=0;boolean ready;do{CriticalInputMirrorR29.Step step=CriticalInputMirrorR29.capture(root,research);boolean[] out={false};tx(research,()->out[0]=CriticalInputMirrorR29.apply(research,step));ready=out[0];if(++turns>50)throw new AssertionError("production-input bootstrap stalled");}while(!ready);}
    public static Map<String,Object> exercise(SQLiteDatabase root,SQLiteDatabase research)throws Exception{
        Map<String,Object> result=new LinkedHashMap<>();result.put("scope","SYNTHETIC_ANDROID_NOT_PHYSICAL_NOT_NATURAL");installRoot(root);Map<String,String> original=CriticalInputMirrorR29.misplacedRootTriggers(root);require(original.size()==2,"actual Root installer did not install both FTRL OOS constraints");require(rootFrozen(root),"Root learned label was mutable before repair");
        base(research);tx(research,()->{research.execSQL(ResearchRouteR29.OWNER_DDL);research.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY','SYNTHETIC_SCHEMA','SYNTHETIC_ROOT',1,1)");});sync(root,research);
        // Reproduce the ACTUAL surviving DDL from the faulty old combined schema installer. No simplified trigger
        // and no fabricated Root event table are installed on RP to make this SQL appear usable.
        for(String ddl:original.values())research.execSQL(ddl);
        String legacyFailure="";try{tx(research,()->{research.execSQL("UPDATE "+CriticalInputMirrorR29.GUARD+" SET active=1 WHERE singleton=1");research.execSQL("DELETE FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE target_issue='202610300002'");});}
        catch(RuntimeException expected){legacyFailure=String.valueOf(expected.getMessage());if(!legacyFailure.contains(RuntimeEventLedgerR926.TABLE_EVENT))throw expected;}
        require(!legacyFailure.isEmpty(),"old erroneous Root trigger did not reproduce the missing-authority failure");result.put("legacyMisinstalledRootTriggerActuallyFailsOnRpWithoutRootEventTable",true);result.put("legacyMisinstalledRootTriggerFailure",legacyFailure);
        require(!CriticalInputMirrorR29.misplacedRootTriggers(research).isEmpty(),"poisoned READY replica was falsely treated as clean");
        tx(root,()->root.execSQL("UPDATE "+DbHelper.TABLE_R35_OOS_LABEL+" SET hit_label=0,actual_number=10,settled_at=71000 WHERE target_issue='202610300002'"));sync(root,research);
        result.put("rpMirrorApplySucceedsAfterPreciseIsolation",n(research,"SELECT hit_label FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE target_issue='202610300002'")==0&&CriticalInputMirrorR29.misplacedRootTriggers(research).isEmpty()&&"READY".equals(CriticalInputMirrorR29.state(research).phase));
        boolean readonly=false;try{research.execSQL("DELETE FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE target_issue='202610300002'");}catch(RuntimeException expected){readonly=String.valueOf(expected.getMessage()).contains("ROOT_AUTHORITY_INPUT_IS_READ_ONLY_REPLICA");}result.put("rpInputReplicaStillReadOnly",readonly);
        result.put("productionRootOosFreezeStillActive",rootFrozen(root));result.put("originalRootTriggerSqlUnchanged",original.equals(CriticalInputMirrorR29.misplacedRootTriggers(root)));
        result.put("noFakeRootAuthorityCreatedInRp",!ResearchInputMirrorR29.table(research,RuntimeEventLedgerR926.TABLE_EVENT)&&!ResearchInputMirrorR29.table(research,DbHelper.TABLE_R906_SELECTED_EVIDENCE));
        boolean preserved=n(research,"SELECT count(*) FROM "+CriticalInputMirrorR29.TRIGGER_QUARANTINE)==2;
        for(Map.Entry<String,String> entry:original.entrySet())try(Cursor c=research.rawQuery("SELECT original_sql,ddl_hash FROM "+CriticalInputMirrorR29.TRIGGER_QUARANTINE+" WHERE trigger_name=?",new String[]{entry.getKey()})){preserved&=c.moveToFirst()&&entry.getValue().equals(c.getString(0))&&StatsR27.sha256(entry.getValue()).equals(c.getString(1));}
        result.put("exactForeignTriggerDdlQuarantined",preserved);
        boolean rootRefused=false;try{tx(root,()->CriticalInputMirrorR29.installResearch(root));}catch(IllegalStateException expected){rootRefused=String.valueOf(expected.getMessage()).contains("ISOLATION_REQUIRES_RESEARCH_OWNER");}
        require(rootRefused&&original.equals(CriticalInputMirrorR29.misplacedRootTriggers(root)),"isolation modified or accepted original Root ownership");result.put("rootRefusesResearchOnlyIsolation",true);
        for(String key:Arrays.asList("productionRootOosFreezeStillActive","legacyMisinstalledRootTriggerActuallyFailsOnRpWithoutRootEventTable","rpMirrorApplySucceedsAfterPreciseIsolation","rpInputReplicaStillReadOnly","originalRootTriggerSqlUnchanged","noFakeRootAuthorityCreatedInRp","exactForeignTriggerDdlQuarantined"))require(Boolean.TRUE.equals(result.get(key)),key);
        result.put("result","PASS_WITH_STATED_ANDROID_SCOPE");return result;
    }
}
