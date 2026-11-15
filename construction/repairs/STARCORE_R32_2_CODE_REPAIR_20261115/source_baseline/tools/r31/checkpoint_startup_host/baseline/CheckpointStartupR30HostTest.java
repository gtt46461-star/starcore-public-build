package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
public final class CheckpointStartupR30HostTest {
    interface Work {void run()throws Exception;}
    static void tx(SQLiteDatabase db,Work work)throws Exception{db.jdbc.setAutoCommit(false);try{work.run();db.jdbc.commit();}catch(Exception|Error e){db.jdbc.rollback();throw e;}finally{db.jdbc.setAutoCommit(true);}}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError("missing scalar");return c.getLong(0);}}
    static byte[] state(int seed){byte[] v=new byte[128*1024];new Random(seed).nextBytes(v);return v;}
    static void replace(SQLiteDatabase db,int seed){byte[] bytes=state(seed);db.execSQL("INSERT OR REPLACE INTO r29_model_checkpoint VALUES('ATOMNET_TEN_R30','abi','dataset','through',?,?,1)",new Object[]{StatsR27.sha256(bytes),bytes});}
    static long retained(SQLiteDatabase db,int seed){try(Cursor c=db.rawQuery("SELECT count(*) FROM (SELECT state_hash FROM r29_model_checkpoint UNION ALL SELECT state_hash FROM r30_keep_r29_model_checkpoint) WHERE state_hash=?",new String[]{StatsR27.sha256(state(seed))})){if(!c.moveToFirst())throw new AssertionError("missing retained count");return c.getLong(0);}}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path file=args.length>1?Paths.get(args[1]):Files.createTempFile("r30-startup-checkpoint-",".db");
        try(Connection conn=DriverManager.getConnection("jdbc:sqlite:"+file)){
            SQLiteDatabase db=new SQLiteDatabase(conn);
            if(args.length==0){
                db.execSQL("CREATE TABLE r29_model_checkpoint(owner TEXT PRIMARY KEY,feature_abi TEXT,dataset_hash TEXT,trained_through TEXT,state_hash TEXT,artifact BLOB,updated_at INTEGER)");replace(db,1);
                tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));replace(db,2);
                if(retained(db,1)==0)throw new AssertionError("PRE_UPGRADE_CHECKPOINT_BYTES_NOT_RETAINED_AFTER_NORMAL_TRAINING_REPLACE");
                long before=n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint");tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));replace(db,3);tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));
                if(n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint")!=before)throw new AssertionError("EVERY_TRAINING_UPDATE_WAS_ARCHIVED_WITHIN_SAME_PROCESS");
                System.out.println("CHECKPOINT_STARTUP_BASELINE_RETAINED_WITHOUT_PER_BATCH_ARCHIVE_CHURN");
            }else{
                // A real new JVM observes the most recent pre-upgrade checkpoint, then training advances.
                db.execSQL("CREATE TABLE host_startup_archive_failure(enabled INTEGER)");
                db.execSQL("INSERT INTO host_startup_archive_failure VALUES(1)");
                db.execSQL("CREATE TRIGGER host_startup_archive_failure BEFORE INSERT ON r30_keep_r29_model_checkpoint WHEN NEW.__r30_reason='PRE_TRAINING_STARTUP_CHECKPOINT' AND (SELECT enabled FROM host_startup_archive_failure)=1 BEGIN SELECT RAISE(ABORT,'HOST_STARTUP_ARCHIVE_FAILURE'); END");
                long markers=n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint");
                try{tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));throw new AssertionError("FAILED_SNAPSHOT_WAS_ACCEPTED");}
                catch(android.database.sqlite.SQLiteException expected){}
                if(n(db,"SELECT count(*) FROM r30_retention_boot_checkpoint")!=markers||retained(db,3)==0)throw new AssertionError("FAILED_SNAPSHOT_ADVANCED_MARKER_OR_CHANGED_ORIGINAL");
                db.execSQL("UPDATE host_startup_archive_failure SET enabled=0");
                tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));replace(db,4);
                if(retained(db,1)==0||retained(db,3)==0)throw new AssertionError("REAL_REOPEN_DID_NOT_PRESERVE_PRE_UPGRADE_CHECKPOINT");
                System.out.println("CHECKPOINT_REAL_JVM_REOPEN_PRESERVES_PRE_UPGRADE_BYTES_PASS");return;
            }
        }
        Process child=new ProcessBuilder("java","-cp",System.getProperty("java.class.path"),CheckpointStartupR30HostTest.class.getName(),"reopen",file.toString()).inheritIO().start();if(child.waitFor()!=0)throw new AssertionError("REAL_REOPEN_FAILED");
        System.out.println("R30_CHECKPOINT_STARTUP|REAL_SQLITE|NORMAL_TRAINING_REPLACE|REAL_JVM_REOPEN|NO_PER_BATCH_ARCHIVE_CHURN|HOST_NOT_DEVICE|PASS");
    }
}
