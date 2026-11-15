package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.SchemaDomainCoordinatorR926;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
/** Actual retention producer and native SQLite. The JDBC-only fault adapter is never application code. */
public final class CheckpointStartupR30HostTest {
    static int assertions;
    static void need(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    interface Work {void run()throws Exception;}
    static void tx(SQLiteDatabase db,Work work)throws Exception{db.jdbc.setAutoCommit(false);try{work.run();db.jdbc.commit();}catch(Exception|Error e){db.jdbc.rollback();throw e;}finally{db.jdbc.setAutoCommit(true);}}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError("missing scalar");return c.getLong(0);}}
    static byte[] state(int seed){byte[] v=new byte[128*1024];new Random(seed).nextBytes(v);return v;}
    static void replace(SQLiteDatabase db,int seed){byte[] bytes=state(seed);db.execSQL("INSERT OR REPLACE INTO r29_model_checkpoint VALUES('ATOMNET_TEN_R30','abi','dataset','through',?,?,1)",new Object[]{StatsR27.sha256(bytes),bytes});}
    static long retained(SQLiteDatabase db,int seed){try(Cursor c=db.rawQuery("SELECT count(*) FROM (SELECT state_hash FROM r29_model_checkpoint UNION ALL SELECT state_hash FROM r30_keep_r29_model_checkpoint) WHERE state_hash=?",new String[]{StatsR27.sha256(state(seed))})){if(!c.moveToFirst())throw new AssertionError("missing retained count");return c.getLong(0);}}
    static void retainedBytes(SQLiteDatabase db,int seed){
        byte[] expected=state(seed);int rows=0;
        try(Cursor c=db.rawQuery("SELECT artifact,typeof(artifact) FROM (SELECT state_hash,artifact FROM r29_model_checkpoint UNION ALL SELECT state_hash,artifact FROM r30_keep_r29_model_checkpoint) WHERE state_hash=?",new String[]{StatsR27.sha256(expected)})){
            while(c.moveToNext()){rows++;need("blob".equals(c.getString(1))&&Arrays.equals(expected,c.getBlob(0)),"RETAINED_CHECKPOINT_BYTES_OR_STORAGE_CLASS_CHANGED:"+seed);}
        }
        need(rows>0,"RETAINED_CHECKPOINT_MISSING:"+seed);
    }
    static void missingCritical(SQLiteDatabase db){
        need("STAMP_MISSING".equals(SchemaDomainCoordinatorR926.matchReason(db,SchemaDomainCoordinatorR926.criticalContract())),"MINIMAL_CHECKPOINT_FIXTURE_MUST_NOT_AUTHENTICATE_CRITICAL");
        need(n(db,"SELECT count(*) FROM "+SchemaDomainCoordinatorR926.TABLE_DOMAIN+" WHERE domain='CRITICAL'")==0,"MISSING_CRITICAL_STAMP_WAS_FABRICATED");
    }
    static void blockedAttachment(SQLiteDatabase db,long markers,long archived)throws Exception{
        try{tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));throw new AssertionError("UNKNOWN_ARCHIVE_ATTACHMENT_WAS_ACCEPTED");}
        catch(DatabaseCoordinator.SchemaPendingR31 expected){
            need("DEPENDENCY".equals(expected.failureCategory())&&"SCHEMA_READINESS".equals(expected.phase()),"UNKNOWN_ATTACHMENT_MISCLASSIFIED");
            need(expected.getMessage().contains("UNKNOWN_ARCHIVE_ATTACHMENT:r30_keep_r29_model_checkpoint:host_startup_archive_failure"),"WRONG_SCHEMA_DEPENDENCY:"+expected);
        }
        need(n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")==markers&&n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint")==archived,"BLOCKED_SCHEMA_ADVANCED_STARTUP_OR_ARCHIVE");
        retainedBytes(db,1);retainedBytes(db,3);missingCritical(db);
    }
    static Object invoke(Object target,Method method,Object[] args)throws Throwable{
        try{return method.invoke(target,args);}catch(InvocationTargetException failure){throw failure.getCause();}
    }
    /** Arm real SQLite query_only immediately before the actual archive INSERT executes.
     * Schema stays exact; no synthetic archive trigger bypasses or replaces the production migration. */
    static Connection failArchiveInsert(Connection actual,int[] hits){
        return (Connection)Proxy.newProxyInstance(CheckpointStartupR30HostTest.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
            Object result=invoke(actual,method,args);
            if(!method.getName().equals("prepareStatement")||args==null||!(args[0] instanceof String)||!(result instanceof PreparedStatement))return result;
            String sql=(String)args[0];PreparedStatement statement=(PreparedStatement)result;
            if(!sql.startsWith("INSERT INTO \"r30_keep_r29_model_checkpoint\"(__r30_at,__r30_reason,"))return result;
            return Proxy.newProxyInstance(CheckpointStartupR30HostTest.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(prepared,call,values)->{
                if(call.getName().equals("execute")){
                    hits[0]++;need(hits[0]==1,"ARCHIVE_INSERT_FAULT_RAN_MORE_THAN_ONCE");
                    try(Statement control=actual.createStatement()){control.execute("PRAGMA query_only=ON");}
                }
                return invoke(statement,call,values);
            });
        });
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path file=args.length>1?Paths.get(args[1]):Files.createTempFile("r30-startup-checkpoint-",".db");
        try(Connection conn=DriverManager.getConnection("jdbc:sqlite:"+file)){
            SQLiteDatabase db=new SQLiteDatabase(conn);
            if(args.length==0){
                db.execSQL("CREATE TABLE r29_model_checkpoint(owner TEXT PRIMARY KEY,feature_abi TEXT,dataset_hash TEXT,trained_through TEXT,state_hash TEXT,artifact BLOB,updated_at INTEGER)");replace(db,1);
                tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));replace(db,2);
                if(retained(db,1)==0)throw new AssertionError("PRE_UPGRADE_CHECKPOINT_BYTES_NOT_RETAINED_AFTER_NORMAL_TRAINING_REPLACE");
                retainedBytes(db,1);missingCritical(db);
                long before=n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint");tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));replace(db,3);tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));
                if(n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint")!=before)throw new AssertionError("EVERY_TRAINING_UPDATE_WAS_ARCHIVED_WITHIN_SAME_PROCESS");
                retainedBytes(db,1);retainedBytes(db,3);missingCritical(db);
                System.out.println("CHECKPOINT_STARTUP_BASELINE_RETAINED_WITHOUT_PER_BATCH_ARCHIVE_CHURN");
            }else{
                // A real new JVM observes the most recent pre-upgrade checkpoint, then training advances.
                db.execSQL("CREATE TABLE host_startup_archive_failure(enabled INTEGER)");
                db.execSQL("INSERT INTO host_startup_archive_failure VALUES(1)");
                db.execSQL("CREATE TRIGGER host_startup_archive_failure BEFORE INSERT ON r30_keep_r29_model_checkpoint WHEN NEW.__r30_reason='PRE_TRAINING_STARTUP_CHECKPOINT' AND (SELECT enabled FROM host_startup_archive_failure)=1 BEGIN SELECT RAISE(ABORT,'HOST_STARTUP_ARCHIVE_FAILURE'); END");
                long markers=n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint");
                long archived=n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint");
                missingCritical(db);blockedAttachment(db,markers,archived);
                if(n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")!=markers||retained(db,3)==0)throw new AssertionError("FAILED_SNAPSHOT_ADVANCED_MARKER_OR_CHANGED_ORIGINAL");
                db.execSQL("UPDATE host_startup_archive_failure SET enabled=0");
                // A disabled unknown attachment is still an unknown schema, not a repaired archive.
                blockedAttachment(db,markers,archived);
                db.execSQL("DROP TRIGGER host_startup_archive_failure");
                need(n(db,"SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name='host_startup_archive_failure'")==0,"SYNTHETIC_TRIGGER_NOT_REMOVED");
                db.execSQL("CREATE TABLE host_startup_tx_probe(value INTEGER)");
                int[] faults={0};SQLiteDatabase failing=new SQLiteDatabase(failArchiveInsert(conn,faults));
                try{
                    tx(failing,()->{failing.execSQL("INSERT INTO host_startup_tx_probe VALUES(1)");ResearchRetentionStoreR30.ensureProtected(failing);});
                    throw new AssertionError("NATIVE_READONLY_ARCHIVE_INSERT_WAS_ACCEPTED");
                }catch(android.database.sqlite.SQLiteException expected){
                    need(expected.getMessage()!=null&&expected.getMessage().contains("SQLITE_READONLY"),"FAULT_WAS_NOT_NATIVE_SQLITE_READONLY:"+expected);
                    System.out.println("CHECKPOINT_ACTUAL_ARCHIVE_INSERT_FAILURE|"+expected.getMessage());
                }finally{try(Statement control=conn.createStatement()){control.execute("PRAGMA query_only=OFF");}}
                need(faults[0]==1,"ACTUAL_ARCHIVE_INSERT_FAULT_NOT_REACHED");
                need(n(db,"SELECT count(*) FROM host_startup_tx_probe")==0,"NATIVE_INSERT_FAILURE_DID_NOT_ROLL_BACK_EARLIER_WRITE");
                need(n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")==markers&&n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint")==archived,"NATIVE_INSERT_FAILURE_ADVANCED_MARKER_OR_ARCHIVE");
                need(n(db,"PRAGMA query_only")==0&&conn.getAutoCommit(),"FAULT_RESOURCE_STATE_NOT_RESTORED");
                retainedBytes(db,1);retainedBytes(db,3);missingCritical(db);
                tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));replace(db,4);
                if(retained(db,1)==0||retained(db,3)==0)throw new AssertionError("REAL_REOPEN_DID_NOT_PRESERVE_PRE_UPGRADE_CHECKPOINT");
                need(n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")==markers+1&&n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint")==archived+1,"RECOVERY_DID_NOT_PUBLISH_EXACTLY_ONE_STARTUP_SNAPSHOT");
                retainedBytes(db,1);retainedBytes(db,3);retainedBytes(db,4);missingCritical(db);
                long recovered=n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint");
                tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));
                need(n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint")==recovered&&n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")==markers+1,"REPEATED_RECOVERY_ARCHIVED_INTERMEDIATE_TRAINING_OR_ADVANCED_MARKER");
                System.out.println("CHECKPOINT_REAL_JVM_REOPEN_PRESERVES_PRE_UPGRADE_BYTES_PASS|assertions="+assertions+"|UNKNOWN_TRIGGER_DEPENDENCY_DISABLED_STILL_BLOCKED|DROP_SPECIFIC_TRIGGER|NATIVE_SQLITE_READONLY_INSERT_ROLLBACK_RECOVERY|CRITICAL_STAMP_MISSING_NOT_AUTHENTICATED");return;
            }
        }
        Process child=new ProcessBuilder("java","-Xmx768m","-cp",System.getProperty("java.class.path"),CheckpointStartupR30HostTest.class.getName(),"reopen",file.toString()).inheritIO().start();
        if(!child.waitFor(45,TimeUnit.SECONDS)){child.destroyForcibly();need(child.waitFor(10,TimeUnit.SECONDS),"REAL_REOPEN_CHILD_DID_NOT_EXIT");throw new AssertionError("REAL_REOPEN_TIMEOUT");}
        if(child.exitValue()!=0)throw new AssertionError("REAL_REOPEN_FAILED");
        need(!child.isAlive(),"REAL_REOPEN_CHILD_STILL_ALIVE");
        System.out.println("R30_CHECKPOINT_STARTUP|REAL_SQLITE|NORMAL_TRAINING_REPLACE|REAL_JVM_REOPEN|NO_PER_BATCH_ARCHIVE_CHURN|HOST_NOT_DEVICE|PASS|parentAssertions="+assertions);
    }
}
