package com.ethk3.novaonepro.strategyv2;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

/** Actual production research installer to READY in an independent SQLite file, plus Root SQL freeze guards.
 * HOST fixture only: synthetic records never certify natural learning or Android timing. */
public final class ResearchSchemaOwnerR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_SCHEMA_OWNER|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    interface Work{void run()throws Exception;}
    static void tx(SQLiteDatabase db,Work work)throws Exception{db.beginTransaction();try{work.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static void base(SQLiteDatabase db){
        db.execSQL("PRAGMA journal_mode=WAL");db.execSQL("PRAGMA synchronous=FULL");
        db.execSQL("CREATE TABLE draws(_id INTEGER PRIMARY KEY AUTOINCREMENT,issue TEXT NOT NULL UNIQUE,open_time INTEGER DEFAULT 0,fetch_time INTEGER DEFAULT 0,source_line TEXT,n1 INTEGER DEFAULT 0,n2 INTEGER DEFAULT 0,n3 INTEGER DEFAULT 0,n4 INTEGER DEFAULT 0,n5 INTEGER DEFAULT 0,n6 INTEGER DEFAULT 0,n7 INTEGER DEFAULT 0,n8 INTEGER DEFAULT 0,n9 INTEGER DEFAULT 0,n10 INTEGER DEFAULT 0,raw_json TEXT,dirty INTEGER DEFAULT 0,dirty_reason TEXT)");
        db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER DEFAULT 0)");
    }
    static void rootBaseAndOwnedCopy(SQLiteDatabase root,SQLiteDatabase research)throws Exception{
        // These private methods are the actual product base DDL/column migration, not hand-written stand-ins.
        // A HOST-only uninitialized helper avoids invoking the Android framework constructor on the JVM.
        Class<?> unsafeClass=Class.forName("sun.misc.Unsafe");java.lang.reflect.Field f=unsafeClass.getDeclaredField("theUnsafe");f.setAccessible(true);Object unsafe=f.get(null);
        Object helper=unsafeClass.getMethod("allocateInstance",Class.class).invoke(unsafe,DbHelper.class);
        tx(root,()->{for(String name:new String[]{"createBaseTables","migrateColumns"}){
            Method method=DbHelper.class.getDeclaredMethod(name,SQLiteDatabase.class);method.setAccessible(true);try{method.invoke(helper,root);}catch(java.lang.reflect.InvocationTargetException failure){throw new IllegalStateException("ACTUAL_ROOT_BASE_DDL_FAILED:"+name,failure.getCause());}
        }DbHelper.ensureR35Schema(root);DbHelper.ensureR36Schema(root);DbHelper.ensureR811CriticalSchema(root);DbHelper.ensureR815CriticalSchema(root);DbHelper.ensureR906CriticalSchema(root);V51Repository.ensure(root);DbHelper.ensureTianshuB03(root);SchemaRevisionR925.ensureCriticalTables(root);RuntimeEventLedgerR926.ensureSchema(root);FormalReceiptSchemaR29.ensureRoot(root);RuntimeSchemaR26.ensure(root);StrategySchemaDomainR29.ensureRealtime(root);CriticalInputMirrorR29.ensureRealtime(root);});
        List<MigrationCopyR29.Table> tables=ResearchCutoverR29.tables(root);int copied=0;
        for(MigrationCopyR29.Table table:tables){if(table.name.equals("draws")){if(n(research,"SELECT count(*) FROM draws")!=0)throw new AssertionError("FIXTURE_ROOT_DRAW_SCHEMA_REPLACEMENT_REQUIRES_EMPTY_TARGET");research.execSQL("DROP TABLE draws");research.execSQL(table.createSql);continue;}if(!MigrationCopyR29.ownedResearch(table.name)&&!CriticalInputMirrorR29.tables().contains(table.name)&&!table.name.equals("su1_source_clock"))continue;
            tx(research,()->research.execSQL(table.createSql));Long after=null;
            for(;;){List<MigrationCopyR29.Row> rows=ResearchCutoverR29.page(root,table,after);if(rows.isEmpty())break;tx(research,()->ResearchCutoverR29.restorePage(research,table,rows));after=rows.get(rows.size()-1).rowid;}copied++;
        }
        check("S12_ACTUAL_PRODUCT_LEGACY_BASE_COPIED_WITHOUT_ROOT_AUTHORITY",copied>=10&&n(research,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='auto_learn_cursor'")==1);
    }
    static void omissionAudit(SQLiteDatabase rp){
        int count=0,su1=0;try(Cursor c=rp.rawQuery("SELECT phase,sql_sha256,target_table,original_sql FROM r29_schema_domain_omission",null)){while(c.moveToNext()){
            if(!c.getString(1).equals(StatsR27.sha256(c.getString(3))))throw new AssertionError("OMISSION_HASH_MISMATCH:"+c.getString(2));count++;if(c.getString(0).equals("SU1_STORAGE"))su1++;
        }}check("S11_ORIGINAL_ROOT_GUARD_AND_STRATEGY_SQL_OMISSIONS_ARE_HASH_AUDITED",count>20&&su1==10);
    }
    static String sql(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){if(!c.moveToFirst())throw new AssertionError("ACTUAL_ROOT_CONTROL_DDL_MISSING:"+name);return c.getString(0);}}
    static void owner(SQLiteDatabase rp){rp.execSQL(ResearchRouteR29.OWNER_DDL);rp.execSQL("INSERT INTO r29_research_owner VALUES(1,'COPYING',?,?,42,1)",new Object[]{StatsR27.sha256("schema"),StatsR27.sha256("root")});}
    static void strategyCompatibility(SQLiteDatabase root)throws Exception{
        try(SQLiteDatabase old=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"))){
            owner(old);for(String table:new String[]{"su1_mainline_binding","su2_mainline_choice","su2_mainline_binding"})old.execSQL(sql(root,table));
            check("S16_EMPTY_OLD_CONTROL_COPY_IS_NOT_REPORTABLE_AS_READY",!StrategySchemaSU1.ready(old)&&!StrategySchemaSU2.ready(old));
            tx(old,()->StrategySchemaSU2.ensure(old));
            check("S17_EMPTY_OLD_THREE_BINDINGS_RETAINED_ATOMICALLY",n(old,"SELECT count(*) FROM r29_strategy_root_control_retention")==3&&n(old,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN('su1_mainline_binding','su2_mainline_choice','su2_mainline_binding')")==0&&StrategySchemaSU2.ready(old));
            try(Cursor c=old.rawQuery("SELECT original_sql,original_sha256,object_manifest,manifest_sha256,retained_table,original_count FROM r29_strategy_root_control_retention",null)){while(c.moveToNext()){
                if(!StatsR27.sha256(c.getString(0)).equals(c.getString(1))||!StatsR27.sha256(c.getString(2)).equals(c.getString(3))||c.getLong(5)!=0)throw new AssertionError("RETAINED_ROOT_METADATA_LOSS");
                rejects(old,"S18_RETAINED_"+c.getString(4)+"_CANNOT_BE_REACTIVATED","INSERT INTO "+c.getString(4)+" DEFAULT VALUES","R29_RETAINED_ROOT_STRATEGY_COPY_IMMUTABLE");
            }}
            rejects(old,"S19_ORIGINAL_CONTROL_DDL_HASH_AUDIT_IS_IMMUTABLE","UPDATE r29_strategy_root_control_retention SET original_count=1","R29_STRATEGY_RETENTION_AUDIT_IMMUTABLE");
        }
        try(SQLiteDatabase nonempty=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"))){
            owner(nonempty);nonempty.execSQL(sql(root,"su2_mainline_choice"));nonempty.execSQL("INSERT INTO su2_mainline_choice VALUES(1,'SYNTHETIC_RETAINED',0,NULL,'SYNTHETIC',1,1)");boolean denied=false;
            try{tx(nonempty,()->StrategySchemaSU2.ensure(nonempty));}catch(IllegalStateException expected){if(!expected.getMessage().contains("R29_MISPLACED_ROOT_STRATEGY_AUTHORITY_PRESERVED"))throw expected;denied=true;}
            check("S20_NONEMPTY_MISPLACED_BINDING_FAILS_CLOSED_WITH_ROW_PRESERVED",denied&&n(nonempty,"SELECT count(*) FROM su2_mainline_choice")==1&&n(nonempty,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name='r29_retained_root_su2_mainline_choice'")==0);
        }
    }
    static void noRootAuthorities(SQLiteDatabase rp){
        String[] forbidden={"trades","decisions","v57_draw_canonical_r811","research_exact_probability_f05","r974_exact_real_receipt",DbHelper.TABLE_R906_SELECTED_EVIDENCE,DbHelper.TABLE_R906_ISSUE_CLOSURE,DbHelper.TABLE_R906_SETTLEMENT_OUTBOX,DbHelper.TABLE_R906_RUNTIME_TRUTH,RuntimeEventLedgerR926.TABLE_EVENT,RuntimeEventLedgerR926.TABLE_PROJECTION,RuntimeEventLedgerR926.TABLE_OUTBOX,SchemaRevisionR925.TABLE_FETCH_LEDGER,SchemaRevisionR925.TABLE_PREDICTION_TIMING,SchemaRevisionR925.TABLE_RUNTIME_EPOCH,"b03_action_event","b03_number_event","b03_permutation_event","b03_prediction_bundle","b03_selected_event","b03_authorization_event","b03_settlement_event","b03_transaction_audit","su2_mainline_choice","su1_mainline_binding"};
        List<String>found=new ArrayList<>();for(String table:forbidden)try(Cursor c=rp.rawQuery("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name=?",new String[]{table})){if(c.moveToFirst())found.add(table);}
        check("S2_RESEARCH_FILE_HAS_NO_INVENTED_ROOT_AUTHORITIES",found.isEmpty());if(!found.isEmpty())throw new AssertionError(found.toString());
    }
    static ContentValues row(SQLiteDatabase db,String table,String issue){
        ContentValues v=new ContentValues();try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext()){
            String name=c.getString(1),type=c.getString(2);boolean needed=c.getInt(3)!=0||c.getInt(5)!=0;
            if(!needed||!c.isNull(4))continue;
            if(type.toUpperCase(Locale.ROOT).contains("INT"))v.put(name,1L);else if(type.toUpperCase(Locale.ROOT).contains("REAL"))v.put(name,0.25);else v.put(name,"SYNTHETIC_"+name);
        }}
        v.put("target_issue",issue);
        if(hasColumn(db,table,"game_code"))v.put("game_code",ReleaseManifestR926.GAME_CODE);
        if(hasColumn(db,table,"n1")){v.put("n1",1);v.put("n2",2);v.put("n3",3);}
        return v;
    }
    static boolean hasColumn(SQLiteDatabase db,String table,String name){try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())if(name.equals(c.getString(1)))return true;return false;}}
    static void rejects(SQLiteDatabase db,String name,String sql,String code){boolean blocked=false;try{db.execSQL(sql);}catch(RuntimeException e){if(!String.valueOf(e.getMessage()).contains(code))throw e;blocked=true;}check(name,blocked);}
    static void rootFreeze(SQLiteDatabase root)throws Exception{
        tx(root,()->{DbHelper.ensureR35Schema(root);DbHelper.ensureR36Schema(root);DbHelper.ensureR906CriticalSchema(root);RuntimeEventLedgerR926.ensureSchema(root);SchemaRevisionR925.ensureCriticalTables(root);});
        final String issue="202610220001";
        ContentValues oos=row(root,DbHelper.TABLE_R35_OOS_LABEL,issue),evidence=row(root,DbHelper.TABLE_R906_SELECTED_EVIDENCE,issue);
        root.insertOrThrow(DbHelper.TABLE_R35_OOS_LABEL,null,oos);root.insertOrThrow(DbHelper.TABLE_R906_SELECTED_EVIDENCE,null,evidence);
        root.execSQL("UPDATE "+DbHelper.TABLE_R35_OOS_LABEL+" SET hit_label=0 WHERE target_issue=?",new Object[]{issue});
        root.execSQL("UPDATE "+DbHelper.TABLE_R906_SELECTED_EVIDENCE+" SET raw_action_p_value=0.4 WHERE target_issue=?",new Object[]{issue});
        check("S3_ROOT_PRE_LEARN_CORRECTION_REMAINS_POSSIBLE",n(root,"SELECT hit_label FROM "+DbHelper.TABLE_R35_OOS_LABEL)==0);
        root.execSQL("INSERT INTO "+RuntimeEventLedgerR926.TABLE_EVENT+"(event_id,dedup_key,game_code,target_issue,epoch_id,attempt_id,event_type,stage,state_rank,payload_text,payload_hash,source,created_at) VALUES(?,?,?,?,?,?,'LEARNED','LEARNED_EXACTLY_ONCE',90,'{}',?,'ResearchAdaptiveLearnerStoreR811',1)",new Object[]{StatsR27.sha256("synthetic-learn"),"synthetic-learn",ReleaseManifestR926.GAME_CODE,issue,"synthetic-epoch","synthetic-attempt",StatsR27.sha256("{}")});
        rejects(root,"S4_ROOT_LEARNED_OOS_UPDATE_FROZEN","UPDATE "+DbHelper.TABLE_R35_OOS_LABEL+" SET hit_label=1","E02C_FTRL_LABEL_FROZEN_AFTER_LEARN");
        rejects(root,"S5_ROOT_LEARNED_OOS_DELETE_FROZEN","DELETE FROM "+DbHelper.TABLE_R35_OOS_LABEL,"E02C_FTRL_LABEL_FROZEN_AFTER_LEARN");
        rejects(root,"S6_ROOT_LEARNED_EVIDENCE_UPDATE_FROZEN","UPDATE "+DbHelper.TABLE_R906_SELECTED_EVIDENCE+" SET raw_action_p_value=0.5","E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN");
        rejects(root,"S7_ROOT_LEARNED_EVIDENCE_DELETE_FROZEN","DELETE FROM "+DbHelper.TABLE_R906_SELECTED_EVIDENCE,"E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN");
        boolean insertBlocked=false;try{root.insertWithOnConflict(DbHelper.TABLE_R906_SELECTED_EVIDENCE,null,evidence,SQLiteDatabase.CONFLICT_REPLACE);}catch(RuntimeException e){if(!String.valueOf(e.getMessage()).contains("E02C_FTRL_EVIDENCE_FROZEN_AFTER_LEARN"))throw e;insertBlocked=true;}
        check("S8_ROOT_LEARNED_EVIDENCE_REINSERT_FROZEN",insertBlocked);
        check("S9_ROOT_AUTHORITY_ROWS_RETAINED",n(root,"SELECT count(*) FROM "+DbHelper.TABLE_R35_OOS_LABEL)==1&&n(root,"SELECT count(*) FROM "+DbHelper.TABLE_R906_SELECTED_EVIDENCE)==1&&n(root,"SELECT count(*) FROM "+RuntimeEventLedgerR926.TABLE_EVENT)==1);
    }
    public static void main(String[]args)throws Exception{
        Path directory=Files.createTempDirectory("r29-schema-owner-");
        try(SQLiteDatabase root=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("root.db")));SQLiteDatabase rp=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+directory.resolve("research.db")))){
            base(root);base(rp);rootBaseAndOwnedCopy(root,rp);RuntimeEventLedgerR926.ensureSchema(root);strategyCompatibility(root);
            rp.execSQL(ResearchRouteR29.OWNER_DDL);rp.execSQL("INSERT INTO r29_research_owner VALUES(1,'COPYING',?, ?,42,1)",new Object[]{"0".repeat(64),"1".repeat(64)});
            tx(rp,()->{LegacyLearnedInputR29.installResearch(rp);CriticalInputMirrorR29.initialized(rp,CriticalInputMirrorR29.sourceEpoch(root),CriticalInputMirrorR29.sourceSequence(root),CriticalInputMirrorR29.sourceViewSql(root));});
            for(int i=0;i<1000&&!CriticalInputMirrorR29.canonicalReady(rp);i++){
                CriticalInputMirrorR29.Step step=CriticalInputMirrorR29.capture(root,rp);tx(rp,()->CriticalInputMirrorR29.apply(rp,step));
            }
            check("S22_REAL_SEVEN_INPUT_BOOTSTRAP_CANONICAL_ALIAS_IS_READY",CriticalInputMirrorR29.canonicalReady(rp));
            for(int i=0;i<100&&!LegacyLearnedInputR29.ready(rp);i++){
                LegacyLearnedInputR29.Capture capture=LegacyLearnedInputR29.capture(root,rp);tx(rp,()->LegacyLearnedInputR29.apply(rp,capture));
            }
            check("S0_REAL_ZERO_EVENT_BOOTSTRAP_IS_READY",LegacyLearnedInputR29.ready(rp));
            Method advance=ResearchSchemaCoordinatorR926.class.getDeclaredMethod("advance",SQLiteDatabase.class);advance.setAccessible(true);
            boolean ready=false;int steps=0;long start=System.nanoTime();
            for(;steps<1000&&!ready;steps++){
                final boolean[] result={false};tx(rp,()->{try{result[0]=(Boolean)advance.invoke(null,rp);}catch(java.lang.reflect.InvocationTargetException fail){Throwable cause=fail.getCause();if(cause instanceof Exception)throw(Exception)cause;throw fail;}});ready=result[0];
                String status=ResearchSchemaCoordinatorR926.summary();System.out.println("R29_SCHEMA_OWNER|STEP|"+steps+"|"+status);
                if(status.startsWith("FAILED")){java.lang.reflect.Field cause=ResearchSchemaCoordinatorR926.class.getDeclaredField("failureCause");cause.setAccessible(true);Throwable actual=(Throwable)cause.get(null);if(actual!=null)actual.printStackTrace();throw new AssertionError("ACTUAL_RESEARCH_SCHEMA_FAILED:"+status);}
            }
            check("S1_ENTIRE_PRODUCTION_RESEARCH_ADVANCE_REACHES_READY",ready&&ResearchSchemaCoordinatorR926.summary().startsWith("READY|"));
            noRootAuthorities(rp);omissionAudit(rp);
            rejects(rp,"S13_ROOT_INPUT_MIRROR_CANNOT_BECOME_WRITABLE_AUTHORITY","INSERT INTO v57_oos_label_r35(target_issue) VALUES('202610220005')","R29_ROOT_AUTHORITY_INPUT_IS_READ_ONLY_REPLICA");
            tx(rp,()->StrategySchemaSU2.ensure(rp));check("S14_SU2_RESEARCH_SCHEMA_READY_WITHOUT_ROOT_BINDING_OR_NEW_DECISIONS",StrategySchemaSU2.ready(rp));
            check("S15_SU2_SEVEN_ORIGINAL_CONTROL_STATEMENTS_AUDITED",n(rp,"SELECT count(*) FROM r29_schema_domain_omission WHERE phase='SU2_STORAGE'")==7);
            noRootAuthorities(rp);
            check("S10_RESEARCH_CONTRACT_MATCHES_AND_IMMUTABLE_REGISTRIES_SEEDED",SchemaDomainCoordinatorR926.matches(rp,SchemaDomainCoordinatorR926.researchContract(rp))&&n(rp,"SELECT count(*) FROM "+AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY)==133&&n(rp,"SELECT count(*) FROM "+AssetGenesisEngineR915.TABLE_ATOM_SCHEMA)==240);
            System.out.println("R29_SCHEMA_OWNER|MEASURED|steps="+steps+"|tables="+n(rp,"SELECT count(*) FROM sqlite_master WHERE type='table'")+"|milliseconds="+((System.nanoTime()-start)/1000000));
            rootFreeze(root);
        }finally{try(java.util.stream.Stream<Path> files=Files.walk(directory)){files.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}}
        System.out.println("R29_SCHEMA_OWNER|ALL_PASS|checks="+checks+"|ACTUAL_SCHEMA_REAL_SQLITE_HOST_ONLY");
    }
}
