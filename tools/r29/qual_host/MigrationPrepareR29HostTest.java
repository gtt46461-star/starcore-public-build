package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;

/** Real independent SQLite files. A rejected non-first Root transaction must not restart the
 * completed DDL prefix. This does not claim OS crash or physical low-storage coverage. */
public final class MigrationPrepareR29HostTest {
    static int checks;
    interface Body { void run() throws Exception; }
    static void check(String name, boolean value) {
        System.out.println("R29_MIGRATION_PREPARE|"+name+"|"+(value?"OK":"FAILED"));
        if(!value)throw new AssertionError(name);checks++;
    }
    static SQLiteDatabase open(Path path) throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+path));
        db.execSQL("PRAGMA journal_mode=WAL");db.execSQL("PRAGMA synchronous=FULL");return db;
    }
    static final class CountedDatabase extends SQLiteDatabase {
        long queries,ddlStatements;
        CountedDatabase(Path path)throws Exception{super(DriverManager.getConnection("jdbc:sqlite:"+path));execSQL("PRAGMA journal_mode=WAL");execSQL("PRAGMA synchronous=FULL");}
        @Override public Cursor rawQuery(String sql,String[] args){queries++;return super.rawQuery(sql,args);}
        @Override public void execSQL(String sql,Object[] args){if(sql.matches("(?is)^\\s*(CREATE|ALTER|DROP)\\b.*"))ddlStatements++;super.execSQL(sql,args);}
    }
    static void tx(SQLiteDatabase db, Body body) throws Exception {
        db.jdbc.setAutoCommit(false);
        try {body.run();db.jdbc.commit();}catch(Exception failure){db.jdbc.rollback();throw failure;}
        finally {db.jdbc.setAutoCommit(true);}
    }
    static long n(SQLiteDatabase db,String sql){return MigrationCopyR29.scalar(db,sql,null);}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static void fixture(SQLiteDatabase root,SQLiteDatabase stage)throws Exception{
        for(int i=0;i<7;i++){root.execSQL("CREATE TABLE r29_prepare_"+i+"(id INTEGER PRIMARY KEY,value TEXT)");root.execSQL("INSERT INTO r29_prepare_"+i+" VALUES(1,'original-"+i+"')");}
        tx(stage,()->{for(String ddl:MigrationCopyR29.DESTINATION_DDL)stage.execSQL(ddl);});
    }
    static MigrationCopyR29.Transaction rootWriter(SQLiteDatabase root,SQLiteDatabase stage,int[] remaining,int[] calls){
        return body->{
            calls[0]++;
            if(remaining[0]--<=0)throw new IllegalStateException("CP3_DB_BUSY_DEADLINE");
            if(stage.inTransaction())throw new AssertionError("TWO_FILE_WRITERS_HELD");
            tx(root,()->body.run(root));
        };
    }
    static MigrationCopyR29.Transaction stageWriter(SQLiteDatabase root,SQLiteDatabase stage){
        return body->{if(root.inTransaction())throw new AssertionError("TWO_FILE_WRITERS_HELD");tx(stage,()->body.run(stage));};
    }
    static boolean rejected(Body body,String prefix)throws Exception{
        try{body.run();return false;}catch(IllegalStateException expected){if(!expected.getMessage().startsWith(prefix))throw expected;return true;}
    }
    static void prepare(SQLiteDatabase root,SQLiteDatabase stage,List<MigrationCopyR29.Table> tables,
                        int[] remaining,int[] calls,boolean old)throws Exception{
        MigrationCopyR29.Transaction rt=rootWriter(root,stage,remaining,calls),rp=stageWriter(root,stage);
        if(old){for(MigrationCopyR29.Table table:tables)rt.write(db->MigrationCopyR29.trackSource(db,table));rp.write(db->MigrationCopyR29.register(db,tables));}
        else MigrationCopyR29.prepare(root,stage,tables,rt,rp);
    }
    static void busyPrefix(Path dir,boolean old)throws Exception{
        try(SQLiteDatabase root=open(dir.resolve("busy-root.db"));SQLiteDatabase stage=open(dir.resolve("busy-stage.db"))){
            fixture(root,stage);List<MigrationCopyR29.Table> tables=MigrationCopyR29.tables(root);int[] calls={0};int attempts=0;
            for(;attempts<12&&n(stage,"SELECT count(*) FROM r29_copy_manifest")==0;attempts++){
                int[] remaining={2};
                try{prepare(root,stage,tables,remaining,calls,old);}catch(IllegalStateException busy){if(!busy.getMessage().equals("CP3_DB_BUSY_DEADLINE"))throw busy;}
            }
            check("P01_PREFIX_PREPARE_CANNOT_RESUME_PAST_BUSY",n(stage,"SELECT count(*) FROM r29_copy_manifest")==7&&attempts==4);
            check("P02_ROOT_WRITES_ONLY_MISSING_TRACKERS",calls[0]==10&&n(root,"SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name LIKE 'r29_copy_track_%'")==21);
            check("P03_ALL_TABLES_AND_ORIGINAL_ROWS_PRESERVED",n(root,"SELECT count(*) FROM r29_prepare_6 WHERE value='original-6'")==1&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='PENDING'")==7);
            int[] noWriteCalls={0},none={0};
            try(SQLiteDatabase held=open(dir.resolve("busy-root.db"))){
                held.jdbc.setAutoCommit(false);held.execSQL("UPDATE r29_prepare_0 SET value=value");
                try{prepare(root,stage,tables,none,noWriteCalls,false);}finally{held.jdbc.rollback();held.jdbc.setAutoCommit(true);}
            }
            check("P04_CORRECT_TRACKERS_NEVER_JOIN_HELD_ROOT_WRITER",noWriteCalls[0]==0);
            stage.execSQL("UPDATE r29_copy_manifest SET state='COPYING',attempt=4,last_rowid=123,copied_rows=12 WHERE table_name='r29_prepare_3'");
            String before=s(stage,"SELECT state||':'||attempt||':'||last_rowid||':'||copied_rows||':'||schema_hash FROM r29_copy_manifest WHERE table_name='r29_prepare_3'");
            prepare(root,stage,tables,none,noWriteCalls,false);
            check("P05_EXISTING_MANIFEST_CURSOR_ATTEMPT_NEVER_RESET",before.equals(s(stage,"SELECT state||':'||attempt||':'||last_rowid||':'||copied_rows||':'||schema_hash FROM r29_copy_manifest WHERE table_name='r29_prepare_3'")));
            root.execSQL("DROP TRIGGER r29_copy_track_r29_prepare_3_update");
            check("P06_LOST_TRACKER_FAILS_CLOSED_NO_STAGE_RESET",rejected(()->prepare(root,stage,tables,new int[]{100},new int[]{0},false),"R29_MIGRATION_TRACKER_INCOMPLETE")&&before.equals(s(stage,"SELECT state||':'||attempt||':'||last_rowid||':'||copied_rows||':'||schema_hash FROM r29_copy_manifest WHERE table_name='r29_prepare_3'")));
            root.execSQL("CREATE TRIGGER r29_copy_track_r29_prepare_3_update AFTER UPDATE ON r29_prepare_3 BEGIN UPDATE r29_source_watermark SET generation=generation+1 WHERE table_name='R29_PREPARE_3'; END");
            check("P07_SAME_NAME_WRONG_LITERAL_TRACKER_REJECTED",rejected(()->prepare(root,stage,tables,new int[]{100},new int[]{0},false),"R29_MIGRATION_TRACKER_SQL_MISMATCH"));
        }
    }
    static void reopen(Path dir)throws Exception{
        Path rp=dir.resolve("reopen-root.db"),sp=dir.resolve("reopen-stage.db");
        try(SQLiteDatabase root=open(rp);SQLiteDatabase stage=open(sp)){
            fixture(root,stage);
            check("P08_NON_FIRST_BUSY_PRESERVES_COMMITTED_ROOT_TRACKERS",rejected(()->prepare(root,stage,MigrationCopyR29.tables(root),new int[]{2},new int[]{0},false),"CP3_DB_BUSY_DEADLINE")&&n(root,"SELECT count(*) FROM r29_source_watermark")==2&&n(stage,"SELECT count(*) FROM r29_copy_manifest")==0);
            root.execSQL("UPDATE r29_prepare_0 SET value=value");
        }
        try(SQLiteDatabase root=open(rp);SQLiteDatabase stage=open(sp)){
            int[] calls={0};prepare(root,stage,MigrationCopyR29.tables(root),new int[]{100},calls,false);
            check("P09_PROCESS_FILE_REOPEN_RESUMES_ONLY_REMAINING_TABLES",calls[0]==5&&n(root,"SELECT generation FROM r29_source_watermark WHERE table_name='r29_prepare_0'")==1&&n(stage,"SELECT count(*) FROM r29_copy_manifest")==7);
            List<MigrationCopyR29.Table> old=MigrationCopyR29.tables(root);root.execSQL("ALTER TABLE r29_prepare_2 ADD COLUMN later TEXT");
            check("P10_SOURCE_SCHEMA_DRIFT_FRESH_CHECKED",rejected(()->prepare(root,stage,old,new int[]{100},new int[]{0},false),"R29_SOURCE_SCHEMA_CHANGED_DURING_TRACKING"));
        }
    }
    static void lost(Path dir)throws Exception{
        try(SQLiteDatabase root=open(dir.resolve("lost-root.db"));SQLiteDatabase stage=open(dir.resolve("lost-stage.db"))){
            fixture(root,stage);List<MigrationCopyR29.Table> tables=MigrationCopyR29.tables(root);prepare(root,stage,tables,new int[]{100},new int[]{0},false);
            for(String event:new String[]{"insert","update","delete"})root.execSQL("DROP TRIGGER r29_copy_track_r29_prepare_1_"+event);
            root.execSQL("DELETE FROM r29_source_watermark WHERE table_name='r29_prepare_1'");
            check("P11_ALL_TRACKERS_LOST_AFTER_MANIFEST_CANNOT_RESET_GENERATION",rejected(()->prepare(root,stage,tables,new int[]{100},new int[]{0},false),"R29_MIGRATION_TRACKERS_LOST_AFTER_MANIFEST")&&n(root,"SELECT count(*) FROM r29_source_watermark WHERE table_name='r29_prepare_1'")==0);
        }
        try(SQLiteDatabase root=open(dir.resolve("watermark-root.db"));SQLiteDatabase stage=open(dir.resolve("watermark-stage.db"))){
            fixture(root,stage);root.execSQL("CREATE TABLE r29_source_watermark(table_name TEXT PRIMARY KEY,generation INTEGER)");
            check("P12_WRONG_WATERMARK_DDL_REJECTED",rejected(()->prepare(root,stage,MigrationCopyR29.tables(root),new int[]{100},new int[]{0},false),"R29_MIGRATION_WATERMARK_SCHEMA_MISMATCH"));
        }
    }
    static void profile(Path dir)throws Exception{
        try(CountedDatabase root=new CountedDatabase(dir.resolve("profile-root.db"));CountedDatabase stage=new CountedDatabase(dir.resolve("profile-stage.db"))){
            tx(root,()->{for(int i=0;i<320;i++)root.execSQL("CREATE TABLE r29_profile_"+i+"(id INTEGER PRIMARY KEY,payload TEXT)");});
            tx(stage,()->{for(String ddl:MigrationCopyR29.DESTINATION_DDL)stage.execSQL(ddl);});
            prepare(root,stage,MigrationCopyR29.tables(root),new int[]{1000},new int[]{0},false);
            root.queries=stage.queries=root.ddlStatements=stage.ddlStatements=0;int[] rootWrites={0};long started=System.nanoTime();
            prepare(root,stage,MigrationCopyR29.tables(root),new int[]{0},rootWrites,false);
            long wallMs=(System.nanoTime()-started)/1_000_000;
            System.out.println("R29_MIGRATION_PREPARE|PROFILE|tables=320|rootQueries="+root.queries+"|stageQueries="+stage.queries+"|rootWriterTransactions="+rootWrites[0]+"|rootDDL="+root.ddlStatements+"|stageDDL="+stage.ddlStatements+"|wallMs="+wallMs);
            check("P13_FULL_320_TABLE_FRESH_PROOF_WITH_ZERO_ROOT_WRITER_OR_DDL",rootWrites[0]==0&&root.ddlStatements==0&&stage.ddlStatements==0&&n(stage,"SELECT count(*) FROM r29_copy_manifest")==320);
        }
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path dir=Files.createTempDirectory("r29-prepare-");boolean old=args.length>0&&args[0].equals("--old-prefix-control");
        try{busyPrefix(dir,old);if(!old){reopen(dir);lost(dir);profile(dir);}System.out.println("R29_MIGRATION_PREPARE|ALL_PASS|checks="+checks+"|HOST_REAL_SQLITE_BUSY_BOUNDARY_NOT_OS_CRASH");}
        finally{try(java.util.stream.Stream<Path> paths=Files.walk(dir)){for(Path p:paths.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
