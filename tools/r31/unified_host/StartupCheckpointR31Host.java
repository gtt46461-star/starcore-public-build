package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.SchemaDomainCoordinatorR926;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.DbStore;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import sun.misc.Unsafe;

/** Real startup archive consumer, native SQLite, complete CAS codec and actual
 * retirement protocol. These checkpoint bytes are synthetic engineering input. */
public final class StartupCheckpointR31Host {
    static int checks;static final String SOURCE=StartupCheckpointIdentityR31.SOURCE,ARCHIVE=StartupCheckpointIdentityR31.ARCHIVE;
    static void need(boolean value,String name){if(!value)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    static void tx(SQLiteDatabase db,Runnable work){try{db.jdbc.setAutoCommit(false);try{work.run();db.jdbc.commit();}catch(Throwable e){db.jdbc.rollback();throw e;}finally{db.jdbc.setAutoCommit(true);}}catch(SQLException e){throw new IllegalStateException(e);}}
    static final class ObservedDb extends SQLiteDatabase {
        boolean committing;int largeWriterQueries;Runnable beforeArtifactChunk;
        ObservedDb(Connection db){super(db);}
        @Override public Cursor rawQuery(String sql,String[] args){if(beforeArtifactChunk!=null&&sql.contains("substr(CAST(\"artifact\"")){Runnable action=beforeArtifactChunk;beforeArtifactChunk=null;action.run();}if(committing&&(sql.contains("CAST(\"artifact\" AS BLOB)")||sql.contains("SELECT \"artifact\" FROM")||sql.contains("substr(CAST(")||sql.startsWith("SELECT row_id,row_hash,bytes")))largeWriterQueries++;return super.rawQuery(sql,args);}
    }
    static ObservedDb open(File file)throws Exception{file.getParentFile().mkdirs();return new ObservedDb(DriverManager.getConnection("jdbc:sqlite:"+file));}
    static File root(File db){return new File(db.getParentFile().getParentFile(),"files/r31-keep");}
    static void set(Object object,String name,Object value)throws Exception{Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);}
    /** Only object/platform arrangement is substituted: actual DbStore.readable,
     * contextForR26, coordinator.readable and the actual Runtime factory run. */
    static StartupCheckpointIdentityR31.Prepared factory(ObservedDb db)throws Exception{
        Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);Unsafe unsafe=(Unsafe)field.get(null);
        File path=new File(db.getPath());android.app.Application app=new android.app.Application(path);android.database.sqlite.SQLiteOpenHelper.bindExistingForHostR31(path,db);DbHelper helper=new DbHelper(app);
        DatabaseCoordinator coordinator=(DatabaseCoordinator)unsafe.allocateInstance(DatabaseCoordinator.class);set(coordinator,"helper",helper);set(coordinator,"app",app);
        DbStore store=(DbStore)unsafe.allocateInstance(DbStore.class);set(store,"coordinator",coordinator);set(store,"helper",helper);set(store,"researchAppCP12",app);
        long before=n(db,"SELECT total_changes()");StartupCheckpointIdentityR31.Prepared result=RuntimeCoordinatorR26.prepareStartupCheckpointR31(store);need(result!=null,"ACTUAL_RUNTIME_FACTORY_PREPARES_FROM_REAL_DBSTORE_AND_CONTEXT_ROOT");need(n(db,"SELECT total_changes()")==before&&!db.inTransaction(),"ACTUAL_RUNTIME_FACTORY_IS_WRITER_OUTSIDE_AND_READ_ONLY");return result;
    }
    static void startup(ObservedDb db,StartupCheckpointIdentityR31.Prepared prepared){
        try{Method method=ResearchRetentionStoreR30.class.getDeclaredMethod("ensureProtectedObjectsR31",SQLiteDatabase.class,StartupCheckpointIdentityR31.Prepared.class);method.setAccessible(true);db.committing=true;
            try{method.invoke(null,db,prepared);}catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof RuntimeException)throw (RuntimeException)cause;throw new IllegalStateException(cause);}finally{db.committing=false;}
        }catch(ReflectiveOperationException e){throw new IllegalStateException("ACTUAL_STARTUP_PRODUCTION_OVERLOAD_NOT_INTEGRATED",e);}
    }
    static KeepMigrationStoreR31.Writer writer(ObservedDb db){return (label,work)->tx(db,()->work.run(db));}
    static byte[] blob(int salt){byte[] value=new byte[160001];for(int i=0;i<value.length;i++)value[i]=(byte)(i+salt);return value;}
    static void insert(ObservedDb db,String owner,int salt){db.execSQL("INSERT OR REPLACE INTO r29_model_checkpoint(owner,feature_abi,dataset_hash,trained_through,state_hash,artifact,updated_at,nullable_value,empty_blob,empty_text,untyped_value) VALUES(?,'ABI','DATASET','202611120001','SAME_STATE_HASH_PREFILTER_ONLY',?,7,NULL,x'','',?)",new Object[]{owner,blob(salt),Long.valueOf(5)});}
    static void schema(ObservedDb db,File files)throws Exception{
        files.mkdirs();db.execSQL(ModelArtifactR29.DDL);for(String col:new String[]{"nullable_value","empty_blob","empty_text","untyped_value"})db.execSQL("ALTER TABLE r29_model_checkpoint ADD COLUMN "+col);
        db.execSQL("CREATE TABLE r27_chart_cache(panel_id TEXT,key TEXT,payload_json TEXT,updated_at INTEGER,PRIMARY KEY(panel_id,key))");
        tx(db,()->{for(String sql:StartupCheckpointIdentityR31.ddlR31())db.execSQL(sql);StartupCheckpointIdentityR31.seedWithinMigrationR31(db);insert(db,"MODEL",0);startup(db,null);for(String sql:KeepMigrationStoreR31.DDL)db.execSQL(sql);for(String sql:KeepMismatchReferenceR31.DDL)db.execSQL(sql);for(String sql:KeepArchiveRetirementR31.DDL)db.execSQL(sql);for(String sql:KeepArchiveRetirementR31.guardDefinitionsR31(db))db.execSQL(sql);SchemaDomainCoordinatorR926.migrateR31MainlineChoice(db);MainlineChoiceStoreR31.appendChoice(db,"","",CertificateGateR27.GATE_VERSION,"","",1,1,1,1);db.execSQL("INSERT INTO r27_chart_cache VALUES('mainline','choice',?,1)",new Object[]{JsonR27.write(Collections.singletonMap("choice",MainlineChoiceStoreR31.FALLBACK))});MainlineChoiceStoreR31.switchToTypedRead(db,1);});
        need(n(db,"SELECT count(*) FROM "+ARCHIVE)==1,"FIRST_PROCESS_PRESERVES_COMPLETE_INCOMING_CHECKPOINT");need(n(db,"SELECT sum(retained_rows) FROM r30_retention_boot_checkpoint")==1,"FIRST_REAL_BOOT_COUNT_ONE");
        KeepMigrationStoreR31 store=new KeepMigrationStoreR31(db,files,writer(db));boolean done=false;for(int i=0;i<100;i++){KeepMigrationR31.Stage stage=KeepMigrationR31.step(store,ARCHIVE);if(stage==KeepMigrationR31.Stage.SWITCH){done=true;break;}}need(done,"ACTUAL_COMPLETE_KEEP_MIGRATION_STAGES");
        for(int i=0;i<5&&!KeepArchiveRetirementR31.MODE.equals(KeepArchiveRetirementR31.mode(db,SOURCE));i++)tx(db,()->KeepArchiveRetirementR31.advanceWithinMigrationR31(db));need(KeepArchiveRetirementR31.MODE.equals(KeepArchiveRetirementR31.mode(db,SOURCE)),"STARTUP_READER_GATES_ACTUAL_R29_DUPLICATE_RETIREMENT");
        need(KeepArchiveRetirementR31.retireOne(db,files,writer(db)),"ACTUAL_VERIFIED_CAS_DUPLICATE_RETIRED");need(n(db,"SELECT count(*) FROM "+ARCHIVE)==0&&n(db,"SELECT count(*) FROM r31_keep_original WHERE archive_table=?",ARCHIVE)==1,"SQLITE_COPY_REMOVED_AND_COMPLETE_CAS_RETAINED");
        Object[] actual=KeepMigrationStoreR31.readOriginal(db,ARCHIVE,1,"artifact","nullable_value","empty_blob","empty_text","untyped_value");need(Arrays.equals(blob(0),(byte[])actual[0])&&actual[1]==null&&((byte[])actual[2]).length==0&&"".equals(actual[3])&&actual[4] instanceof Long,"COMPLETE_TYPED_CAS_ORIGINAL_READ_AFTER_RETIREMENT");
    }
    static void consume(ObservedDb db,File files,String mode)throws Exception{
        KeepMigrationStoreR31.registerLiveRoot(db,files);StartupCheckpointIdentityR31.Prepared prepared=factory(db);need(((Number)prepared.evidence().get("matchedSourceRows")).intValue()==1,"PREPARED_ACTUAL_FULL_TYPED_CAS_MATCH");
        long boots=n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint"),rows=n(db,"SELECT count(*) FROM "+ARCHIVE),retained=n(db,"SELECT retained_rows FROM r30_retention_archive_index WHERE source_table=?",SOURCE);
        if(mode.equals("normal")){boolean unavailable=false;try{tx(db,()->startup(db,null));}catch(RuntimeException expected){unavailable=String.valueOf(expected.getMessage()).contains("CAS_EQUALITY_PREPARATION_REQUIRED");}need(unavailable,"MISSING_PREPARATION_IS_EXPLICIT_WAIT_AFTER_CAS_RETIREMENT");need(n(db,"SELECT count(*) FROM "+ARCHIVE)==rows&&n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")==boots,"UNAVAILABLE_PREPARATION_NEVER_ACKNOWLEDGED_OR_CLAIMED_ZERO");}
        if(mode.equals("kill-before")){System.out.println("STARTUP_REAL_KILL_BEFORE_CONSUMER checks="+checks);System.out.flush();Runtime.getRuntime().halt(73);}
        if(mode.equals("race-during")){
            db.beforeArtifactChunk=()->{try(Connection other=DriverManager.getConnection("jdbc:sqlite:"+db.getPath())){SQLiteDatabase concurrent=new SQLiteDatabase(other);concurrent.execSQL("UPDATE r29_model_checkpoint SET artifact=? WHERE owner='MODEL'",new Object[]{blob(2)});}catch(SQLException e){throw new IllegalStateException(e);}};
            boolean refused=false;try{StartupCheckpointIdentityR31.prepare(db,files);}catch(IOException expected){refused=String.valueOf(expected.getMessage()).contains("SOURCE_CHANGED_DURING_PREPARATION");}need(refused,"CONCURRENT_SOURCE_CHANGE_DURING_FULL_BYTE_READ_IS_UNAVAILABLE_NOT_FAKE_ZERO");prepared=StartupCheckpointIdentityR31.prepare(db,files);need(((Number)prepared.evidence().get("matchedSourceRows")).intValue()==0,"FRESH_SOURCE_AFTER_CONCURRENT_READ_STILL_COMPARES_FULL_BYTES");
        }else if(mode.startsWith("race-")){
            try(Connection other=DriverManager.getConnection("jdbc:sqlite:"+db.getPath())){SQLiteDatabase concurrent=new SQLiteDatabase(other);String kind=mode.substring(5);
                if(kind.equals("blob"))concurrent.execSQL("UPDATE r29_model_checkpoint SET artifact=? WHERE owner='MODEL'",new Object[]{blob(1)});
                else if(kind.equals("append"))concurrent.execSQL("INSERT INTO r29_model_checkpoint SELECT 'APPENDED',feature_abi,dataset_hash,trained_through,state_hash,artifact,updated_at,nullable_value,empty_blob,empty_text,untyped_value FROM r29_model_checkpoint WHERE owner='MODEL'");
                else if(kind.equals("replace"))concurrent.execSQL("INSERT OR REPLACE INTO r29_model_checkpoint SELECT * FROM r29_model_checkpoint WHERE owner='MODEL'");
                else if(kind.equals("storage"))concurrent.execSQL("UPDATE r29_model_checkpoint SET untyped_value=CAST(5 AS REAL) WHERE owner='MODEL'");
                else if(kind.equals("null"))concurrent.execSQL("UPDATE r29_model_checkpoint SET nullable_value=x'' WHERE owner='MODEL'");
                else throw new AssertionError(kind);
            }
            StartupCheckpointIdentityR31.Prepared staleInput=prepared;boolean stale=false;try{tx(db,()->startup(db,staleInput));}catch(IllegalStateException expected){stale=String.valueOf(expected.getMessage()).contains("SOURCE_CHANGED");}need(stale,"SOURCE_CHANGE_OR_REPLACE_REJECTS_STALE_STARTUP_PREPARATION");need(n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")==boots&&n(db,"SELECT count(*) FROM "+ARCHIVE)==rows,"REJECTED_STARTUP_CANNOT_ACK_NONCE_OR_COPY_PARTIAL_INPUT");
            prepared=StartupCheckpointIdentityR31.prepare(db,files);int matches=((Number)prepared.evidence().get("matchedSourceRows")).intValue();need(mode.equals("race-replace")?matches==1:mode.equals("race-append")?matches==1:matches==0,"FULL_BYTES_NULL_AND_STORAGE_CLASS_NOT_STATE_HASH_DEFINE_EQUALITY");
        }
        if(mode.equals("trigger-drift")){
            db.execSQL("DROP TRIGGER r31_startup_checkpoint_update");db.execSQL(StartupCheckpointIdentityR31.DDL[2].replace("generation=generation+1","generation=generation+0"));StartupCheckpointIdentityR31.Prepared driftedInput=prepared;boolean rejected=false;try{tx(db,()->startup(db,driftedInput));}catch(RuntimeException expected){rejected=true;}need(rejected,"UNKNOWN_TRIGGER_CANNOT_AUTHORIZE_CAS_EQUALITY");need(n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")==boots,"DRIFT_DOES_NOT_ACK_STARTUP");db.execSQL("DROP TRIGGER r31_startup_checkpoint_update");db.execSQL(StartupCheckpointIdentityR31.DDL[2]);prepared=StartupCheckpointIdentityR31.prepare(db,files);
        }
        if(mode.equals("corrupt-cas")){
            File original;try(Cursor c=db.rawQuery("SELECT row_hash FROM r31_keep_original WHERE archive_table=?",new String[]{ARCHIVE})){need(c.moveToFirst(),"CAS_METADATA_PRESENT");original=new File(files,c.getString(0)+".row");}byte[] bytes=Files.readAllBytes(original.toPath()),corrupt=bytes.clone();corrupt[corrupt.length-1]^=1;Files.write(original.toPath(),corrupt);boolean refused=false;try{StartupCheckpointIdentityR31.prepare(db,files);}catch(IOException expected){refused=String.valueOf(expected.getMessage()).contains("CAS_HASH");}finally{Files.write(original.toPath(),bytes);}need(refused,"CORRUPT_CAS_CANNOT_BECOME_ZERO_COPY_STARTUP_PROOF");need(n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")==boots,"CORRUPT_CAS_DID_NOT_ACK_STARTUP");prepared=StartupCheckpointIdentityR31.prepare(db,files);
        }
        StartupCheckpointIdentityR31.Prepared selected=prepared;tx(db,()->startup(db,selected));
        int additions=mode.equals("race-append")||mode.equals("race-blob")||mode.equals("race-storage")||mode.equals("race-null")||mode.equals("race-during")?1:0;
        need(n(db,"SELECT count(*) FROM "+ARCHIVE)==rows+additions,"TRUE_STARTUP_CONSUMER_PRESERVES_ONLY_NEW_COMPLETE_INCOMING_IDENTITIES");need(n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")==boots+1,"NEW_PROCESS_STARTUP_NONCE_ACTUALLY_COMMITTED");need(n(db,"SELECT retained_rows FROM r30_retention_boot_checkpoint ORDER BY snapshot_at DESC,rowid DESC LIMIT 1")==additions,"ACTUAL_CHANGES_COUNT_NEVER_FAKED_ZERO");need(n(db,"SELECT retained_rows FROM r30_retention_archive_index WHERE source_table=?",SOURCE)==retained+additions,"CUMULATIVE_RETAINED_COUNTER_PRESERVED");
        tx(db,()->startup(db,null));need(n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")==boots+1&&n(db,"SELECT count(*) FROM "+ARCHIVE)==rows+additions,"SAME_PROCESS_NONCE_PREVENTS_INTERMEDIATE_CHECKPOINT_REARCHIVE");need(db.largeWriterQueries==0,"NO_CAS_OR_LARGE_ARTIFACT_READ_HASH_WHILE_WRITER_HELD");
        long changed=n(db,"SELECT total_changes()"),receipts=n(db,"SELECT count(*) FROM r31_startup_checkpoint_receipt");Map<String,Object> report=StartupCheckpointIdentityR31.report(db);need("ACTUAL_COMMITTED_STARTUP_CONSUMPTION".equals(report.get("status"))&&Boolean.TRUE.equals(report.get("readOnly")),"REAL_COMMITTED_STARTUP_CONSUMER_RECEIPTS_HAVE_READ_ONLY_REPORT");need(n(db,"SELECT total_changes()")==changed,"REPORT_NEVER_WRITES_OR_RECAPTURES_CHECKPOINTS");need(receipts>=1&&n(db,"SELECT cas_matched_rows+actual_new_archive_rows<=source_rows FROM r31_startup_checkpoint_receipt ORDER BY committed_at DESC,rowid DESC LIMIT 1")==1,"PERSISTENT_RECEIPT_COUNTS_BIND_CAS_MATCH_AND_ACTUAL_NEW_ROWS");
        if(mode.equals("kill-after")){System.out.println("STARTUP_REAL_KILL_AFTER_CONSUMER_COMMIT checks="+checks);System.out.flush();Runtime.getRuntime().halt(74);}
        Object[] original=KeepMigrationStoreR31.readOriginal(db,ARCHIVE,1,"artifact");need(Arrays.equals(blob(0),(byte[])original[0]),"INITIAL_FULL_ORIGINAL_STILL_RECOVERABLE_AFTER_STARTUP_AND_SOURCE_MUTATION");
    }
    public static void main(String[] args)throws Exception{
        File file=new File(args[1]);ObservedDb db=open(file);File files=root(file);String mode=args[0];
        if(mode.equals("init"))schema(db,files);else consume(db,files,mode);
        db.jdbc.close();System.out.println("STARTUP_CHECKPOINT_NATIVE_PASS mode="+mode+" checks="+checks+" actualConsumer=ResearchRetentionStoreR30.ensureProtectedObjectsR31 syntheticOnly=true");
    }
}
