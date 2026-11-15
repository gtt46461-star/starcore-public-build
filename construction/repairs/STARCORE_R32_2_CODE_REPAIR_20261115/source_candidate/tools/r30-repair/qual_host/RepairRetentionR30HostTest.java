package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import com.ethk3.novaonepro.RetentionRulesSU2;
import java.sql.*;
import java.util.*;
import java.nio.file.*;
/** Synthetic fixtures on native SQLite with actual production retention code; never Android/device proof. */
public final class RepairRetentionR30HostTest {
    interface Work{void run()throws Exception;}
    static void need(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static void tx(SQLiteDatabase db,Work work)throws Exception{db.jdbc.setAutoCommit(false);try{work.run();db.jdbc.commit();}catch(Exception|Error failure){db.jdbc.rollback();throw failure;}finally{db.jdbc.setAutoCommit(true);}}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static void schema(SQLiteDatabase db)throws Exception{tx(db,()->{RuntimeSchemaR26.ensure(db);for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);for(String ddl:StrategySchemaSU2.DDL)if(ddl.startsWith("CREATE TABLE")||ddl.startsWith("CREATE INDEX"))db.execSQL(ddl);});}
    static String genome(char c){char[] text=new char[64];Arrays.fill(text,c);return new String(text);}
    static void folds()throws Exception{
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(connection);schema(db);
            for(String g:new String[]{genome('a'),genome('b')})for(int i=0;i<150;i++)db.execSQL("INSERT INTO su2_live_decision(genome_key,sequence_ordinal,position,number_mask,act,gscore,phat,frozen_at,target_open_time,hit,settled_at) VALUES(?,?,1,7,?,0,.3,1,2,?,3)",new Object[]{g,1400+i,i%2,i%3==0?1:0});
            long periods=n(db,"SELECT count(*) FROM su2_live_decision"),actions=n(db,"SELECT sum(act) FROM su2_live_decision"),hits=n(db,"SELECT sum(CASE WHEN act=1 THEN hit ELSE 0 END) FROM su2_live_decision");
            RetentionRulesSU2.PreparedFold first=RetentionRulesSU2.prepareFold(db,20000);need(first.periods<=64,"bounded exact source set");
            try{tx(db,()->{RetentionRulesSU2.commitFold(db,first,1);throw new IllegalStateException("SYNTHETIC_AFTER_FOLD_BEFORE_COMMIT");});throw new AssertionError("fault missing");}catch(IllegalStateException expected){}
            need(n(db,"SELECT count(*) FROM su2_live_decision")==periods&&n(db,"SELECT count(*) FROM su2_live_daily")==0,"fold+delete rolled back together");
            tx(db,()->RetentionRulesSU2.commitFold(db,first,2));long afterFirst=n(db,"SELECT sum(periods) FROM su2_live_daily");tx(db,()->need(RetentionRulesSU2.commitFold(db,first,3)==0,"same prepared retry no duplicate"));need(n(db,"SELECT sum(periods) FROM su2_live_daily")==afterFirst,"idempotent original aggregate");
            int work=1;for(;;){RetentionRulesSU2.PreparedFold prepared=RetentionRulesSU2.prepareFold(db,20000);if(prepared==null)break;need(prepared.periods<=64,"all bounded");tx(db,()->RetentionRulesSU2.commitFold(db,prepared,4));need(++work<30,"finite recovery");}
            need(n(db,"SELECT count(*) FROM su2_live_decision")==0,"all old rows consumed");need(n(db,"SELECT sum(periods) FROM su2_live_daily")==periods,"period sufficient statistic exact");need(n(db,"SELECT sum(actions) FROM su2_live_daily")==actions,"action denominator exact");need(n(db,"SELECT sum(hits) FROM su2_live_daily")==hits,"hit numerator exact");
            db.execSQL("INSERT INTO su2_live_decision(genome_key,sequence_ordinal,position,number_mask,act,gscore,phat,frozen_at,target_open_time,hit,settled_at) VALUES(?,2000,1,7,1,0,.3,1,2,1,3)",new Object[]{genome('c')});
            RetentionRulesSU2.PreparedFold stale=RetentionRulesSU2.prepareFold(db,20000);db.execSQL("UPDATE su2_live_decision SET hit=0 WHERE genome_key=?",new Object[]{genome('c')});
            try{tx(db,()->RetentionRulesSU2.commitFold(db,stale,5));throw new AssertionError("changed source accepted");}catch(IllegalStateException expected){need(expected.getMessage().contains("SOURCE_CHANGED"),"correct conflict");}
            need(n(db,"SELECT sum(periods) FROM su2_live_daily")==periods,"conflicted source not counted");
            System.out.println("REPAIR_RETENTION_EXACT_KEY_FOLD_NATIVE_HOST_PASS periods="+periods+" actions="+actions+" hits="+hits+" maxBatch=64 rollback_retry_no_duplicate=1 changed_source_rejected=1");
        }
    }
    static void phases()throws Exception{
        Path path=Files.createTempFile("repair-retention-phase-",".db");byte[] original=new byte[400];new Random(29).nextBytes(original);
        try{
            try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+path)){
                SQLiteDatabase db=new SQLiteDatabase(connection);schema(db);
                // Same order as the production startup owner: retention archives first, then the R31 warm-publication index
                // that WarmCandidateR26.pruneInitializers requires (SchemaDomainCoordinatorR926.migrateR31WarmPublication).
                tx(db,()->{ResearchRetentionStoreR30.ensureProtected(db);com.ethk3.novaonepro.SchemaDomainCoordinatorR926.migrateR31WarmPublication(db);});
                db.execSQL("INSERT INTO r26_frozen VALUES('SYNTHETIC_ORIGINAL_MODEL',1,0,'checkpoint','state','source',1,1,2,?)",new Object[]{original});
                RetentionR26.Checkpoint initial=RetentionR26.checkpoint(db);need(initial.phase==0,"fresh phase");
                for(int phase=0;phase<13;phase++){
                    RetentionR26.Checkpoint checkpoint=RetentionR26.checkpoint(db);need(checkpoint.phase==phase,"ordered phase");
                    if(phase==13-1){try{tx(db,()->{RetentionR26.pruneStep(db,100000,checkpoint.phase-2);RetentionR26.commitCheckpoint(db,checkpoint,99999,null,-1);throw new IllegalStateException("SYNTHETIC_KILL_BEFORE_COMMIT");});throw new AssertionError("missing fault");}catch(IllegalStateException expected){}need(RetentionR26.checkpoint(db).phase==phase,"failed phase did not publish cursor");}
                    tx(db,()->{if(checkpoint.phase>=2)RetentionR26.pruneStep(db,100000,checkpoint.phase-2);RetentionR26.commitCheckpoint(db,checkpoint,99999,null,-1);});
                }
            }
            // A new native SQLite connection simulates process-local state loss; only the persisted phase survives.
            try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+path)){
                SQLiteDatabase db=new SQLiteDatabase(connection);need(RetentionR26.checkpoint(db).phase==13,"durable restart phase");
                for(int phase=13;phase<RetentionR26.PHASES_R30_REPAIR+3;phase++){RetentionR26.Checkpoint checkpoint=RetentionR26.checkpoint(db);need(checkpoint.phase==phase,"resumed phase");tx(db,()->{if(checkpoint.phase<RetentionR26.PHASES_R30_REPAIR+2)RetentionR26.pruneStep(db,100000,checkpoint.phase-2);RetentionR26.commitCheckpoint(db,checkpoint,99999,null,-1);});}
                RetentionR26.Checkpoint complete=RetentionR26.checkpoint(db);need(complete.phase==0&&complete.lastCompletedSequence==99999&&complete.workCursor==27,"full durable phase cycle");need(n(db,"SELECT count(*) FROM r26_frozen")==0,"original retention action");
                try(Cursor c=db.rawQuery("SELECT probability_table FROM r30_keep_r26_frozen WHERE policy_hash='SYNTHETIC_ORIGINAL_MODEL'",null)){need(c.moveToFirst()&&Arrays.equals(original,c.getBlob(0)),"protected original model bytes exact");}
                RetentionR26.Checkpoint stale=complete;tx(db,()->RetentionR26.commitCheckpoint(db,complete,100000,null,-1));try{tx(db,()->RetentionR26.commitCheckpoint(db,stale,100000,null,-1));throw new AssertionError("stale phase accepted");}catch(IllegalStateException expected){}
                System.out.println("REPAIR_RETENTION27_PHASES_NATIVE_HOST_PASS rollback_before_cursor=1 restart_from_durable_phase13=1 immutable_original_bytes=1 stale_phase_rejected=1 scope=HOST_not_Android_or_phone_SLO");
            }
        }finally{Files.deleteIfExists(path);}
    }
    public static void main(String[] args)throws Exception{Class.forName("org.sqlite.JDBC");folds();phases();}
}
