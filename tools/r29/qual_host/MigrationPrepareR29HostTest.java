package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

/** Actual catalog/selected-table copy on independent real SQLite files. Root admission is injected at
 * a non-first table. File reopening is not claimed as an OS crash or physical low-storage test. */
public final class MigrationPrepareR29HostTest {
    static int checks;static long now=System.currentTimeMillis();
    interface Body {void run()throws Exception;}
    static void check(String id,boolean ok){System.out.println("R29_MIGRATION_PREPARE|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static final class Db extends SQLiteDatabase {
        long queries,ddl;
        Db(Path path)throws Exception{super(DriverManager.getConnection("jdbc:sqlite:"+path));execSQL("PRAGMA journal_mode=WAL");execSQL("PRAGMA synchronous=FULL");}
        @Override public Cursor rawQuery(String sql,String[] args){queries++;return super.rawQuery(sql,args);}
        @Override public void execSQL(String sql,Object[] args){if(sql.matches("(?is)^\\s*(CREATE|ALTER|DROP)\\b.*"))ddl++;super.execSQL(sql,args);}
    }
    static void tx(SQLiteDatabase db,Body body)throws Exception{db.jdbc.setAutoCommit(false);try{body.run();db.jdbc.commit();}catch(Exception e){db.jdbc.rollback();throw e;}finally{db.jdbc.setAutoCommit(true);}}
    static long n(SQLiteDatabase db,String sql){return MigrationCopyR29.scalar(db,sql,null);}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static void fixture(SQLiteDatabase root,SQLiteDatabase stage,int count)throws Exception{
        tx(root,()->{for(int i=0;i<count;i++){root.execSQL("CREATE TABLE r29_prepare_"+String.format(java.util.Locale.ROOT,"%03d",i)+"(id INTEGER PRIMARY KEY,value TEXT)");root.execSQL("INSERT INTO r29_prepare_"+String.format(java.util.Locale.ROOT,"%03d",i)+" VALUES(1,'original-"+i+"')");}});
        tx(stage,()->{for(String ddl:MigrationCopyR29.DESTINATION_DDL)stage.execSQL(ddl);});
    }
    static MigrationCopyR29.Transaction writer(SQLiteDatabase root,SQLiteDatabase stage,int[] allowance,int[] calls){
        return body->{calls[0]++;if(allowance[0]--<=0)throw new IllegalStateException("CP3_DB_BUSY_DEADLINE");if(stage.inTransaction())throw new AssertionError("ROOT_AND_STAGE_WRITERS_OVERLAP");tx(root,()->body.run(root));};
    }
    static MigrationCopyR29.Transaction stageWriter(SQLiteDatabase root,SQLiteDatabase stage){return body->{if(root.inTransaction())throw new AssertionError("ROOT_AND_STAGE_WRITERS_OVERLAP");tx(stage,()->body.run(stage));};}
    static void catalog(SQLiteDatabase root,SQLiteDatabase stage,List<MigrationCopyR29.Table> tables)throws Exception{MigrationCopyR29.prepare(tables,stageWriter(root,stage));}
    static MigrationCopyR29.Result step(SQLiteDatabase root,SQLiteDatabase stage,List<MigrationCopyR29.Table> tables,int[] allowance,int[] calls)throws Exception{return MigrationCopyR29.step(root,stage,tables,writer(root,stage,allowance,calls),stageWriter(root,stage),now+=2000);}
    static boolean rejected(Body body,String prefix)throws Exception{try{body.run();return false;}catch(IllegalStateException e){if(!e.getMessage().startsWith(prefix))throw e;return true;}}
    static String cursor(SQLiteDatabase stage,String table){return s(stage,"SELECT state||':'||attempt||':'||coalesce(last_rowid,'NULL')||':'||copied_rows||':'||verified_rows||':'||copy_hash FROM r29_copy_manifest WHERE table_name='"+table+"'");}
    static void firstVerified(SQLiteDatabase root,SQLiteDatabase stage,List<MigrationCopyR29.Table> tables,int[] allowance,int[] calls)throws Exception{for(int i=0;i<12&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==0;i++)step(root,stage,tables,allowance,calls);}

    static void lazyCatalog(Path dir,boolean old)throws Exception{
        try(Db root=new Db(dir.resolve("catalog-root.db"));Db stage=new Db(dir.resolve("catalog-stage.db"))){
            fixture(root,stage,320);List<MigrationCopyR29.Table> tables=MigrationCopyR29.tables(root);int[] writes={0},allowance={1000};
            root.queries=stage.queries=root.ddl=stage.ddl=0;long start=System.nanoTime();
            if(old){for(MigrationCopyR29.Table table:tables)if(!MigrationCopyR29.sourceTracked(root,table))writer(root,stage,allowance,writes).write(db->MigrationCopyR29.trackSource(db,table));}
            catalog(root,stage,tables);
            long catalogWall=(System.nanoTime()-start)/1_000_000;
            check("P01_PENDING_CATALOG_WITHOUT_ROOT_WRITES",n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='PENDING' AND attempt=0 AND copied_rows=0 AND verified_rows=0 AND verified_at=0")==320&&writes[0]==0&&root.ddl==0);
            Map<String,Object> pending=MigrationCopyR29.diagnostics(root,stage);boolean unknown=true;
            for(Object value:(List<?>)pending.get("tables")){Map<?,?> row=(Map<?,?>)value;unknown&=row.get("sourceWatermark")==null&&row.get("currentWatermark")==null&&row.get("catchUpRequired")==null&&row.get("verifiedAt")==null;}
            check("P02_UNTRACKED_PENDING_DIAGNOSTICS_ARE_UNKNOWN_NOT_VERIFIED",unknown&&Boolean.FALSE.equals(pending.get("cutoverEnabled"))&&n(stage,"SELECT count(*) FROM r29_copy_rows")==0);
            check("P03_OLD_STEP_WITHOUT_SOURCE_WRITER_STILL_REQUIRES_TRACKING",rejected(()->MigrationCopyR29.step(root,stage,tables,stageWriter(root,stage),now+=2000),"R29_SOURCE_NOT_TRACKED")&&n(stage,"SELECT max(attempt) FROM r29_copy_manifest")==0);
            firstVerified(root,stage,tables,allowance,writes);
            long wall=(System.nanoTime()-start)/1_000_000;
            System.out.println("R29_MIGRATION_PREPARE|LAZY_PROFILE|tables=320|rootQueries="+root.queries+"|stageQueries="+stage.queries+"|rootWriterTransactions="+writes[0]+"|rootDDL="+root.ddl+"|catalogWallMs="+catalogWall+"|firstVerifiedWallMs="+wall);
            check("P04_FIRST_TABLE_VERIFIED_WITHOUT_319_OTHER_ROOT_TRANSACTIONS",writes[0]==1&&n(root,"SELECT count(*) FROM r29_source_watermark")==1&&n(root,"SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name LIKE 'r29_copy_track_%'")==3&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==1&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='PENDING'")==319);
            check("P05_FULL_SOURCE_CATALOG_AND_ORIGINAL_DATA_RETAINED",n(root,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name LIKE 'r29_prepare_%'")==320&&n(root,"SELECT count(*) FROM r29_prepare_319 WHERE value='original-319'")==1);
        }
    }

    static void retryAndReopen(Path dir)throws Exception{
        Path rootPath=dir.resolve("retry-root.db"),stagePath=dir.resolve("retry-stage.db");String first;
        try(Db root=new Db(rootPath);Db stage=new Db(stagePath)){
            fixture(root,stage,7);List<MigrationCopyR29.Table> tables=MigrationCopyR29.tables(root);catalog(root,stage,tables);int[] calls={0};firstVerified(root,stage,tables,new int[]{100},calls);first=cursor(stage,"r29_prepare_000");
            String untouched=cursor(stage,"r29_prepare_001");int[] busyCalls={0};
            for(int i=0;i<4;i++)check("P06_NON_FIRST_BUSY_"+i+"_DOES_NOT_RESTART_PREFIX",rejected(()->step(root,stage,tables,new int[]{0},busyCalls),"CP3_DB_BUSY_DEADLINE")&&first.equals(cursor(stage,"r29_prepare_000"))&&untouched.equals(cursor(stage,"r29_prepare_001"))&&n(root,"SELECT count(*) FROM r29_source_watermark")==1);
            int[] admitted={0};step(root,stage,tables,new int[]{1},admitted);step(root,stage,tables,new int[]{0},admitted);
            check("P07_SECOND_TABLE_ONLY_NEW_TRACKER_AND_REAL_COPY_CURSOR",admitted[0]==1&&n(root,"SELECT count(*) FROM r29_source_watermark")==2&&n(stage,"SELECT copied_rows FROM r29_copy_manifest WHERE table_name='r29_prepare_001'")==1);
            String before=cursor(stage,"r29_prepare_001");catalog(root,stage,MigrationCopyR29.tables(root));
            check("P08_CATALOG_REREGISTER_NEVER_RESETS_MANIFEST",before.equals(cursor(stage,"r29_prepare_001"))&&first.equals(cursor(stage,"r29_prepare_000")));
            root.execSQL("UPDATE r29_prepare_000 SET value=value");
        }
        try(Db root=new Db(rootPath);Db stage=new Db(stagePath);Db held=new Db(rootPath)){
            List<MigrationCopyR29.Table> tables=MigrationCopyR29.tables(root);String before=cursor(stage,"r29_prepare_001");catalog(root,stage,tables);int[] calls={0};
            check("P09_FILE_REOPEN_PRESERVES_CURSOR_AND_SOURCE_GENERATION",before.contains("COPYING:1:1:1:")&&n(root,"SELECT generation FROM r29_source_watermark WHERE table_name='r29_prepare_000'")==1);
            held.jdbc.setAutoCommit(false);held.execSQL("UPDATE r29_prepare_000 SET value=value");
            try{for(int i=0;i<5&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")<2;i++)step(root,stage,tables,new int[]{0},calls);}finally{held.jdbc.rollback();held.jdbc.setAutoCommit(true);}
            check("P10_ALREADY_TRACKED_TABLE_CONTINUES_WHILE_ROOT_WRITER_HELD",calls[0]==0&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==2&&first.equals(cursor(stage,"r29_prepare_000")));
            root.execSQL("CREATE TABLE r29_prepare_later(id INTEGER PRIMARY KEY,value TEXT)");catalog(root,stage,MigrationCopyR29.tables(root));
            check("P11_REFRESHED_FULL_CATALOG_ADDS_LATER_TABLE_WITHOUT_RESET",n(stage,"SELECT count(*) FROM r29_copy_manifest")==8&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE table_name='r29_prepare_later' AND state='PENDING' AND attempt=0")==1&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==2);
        }
    }

    static void guards(Path dir)throws Exception{
        try(Db root=new Db(dir.resolve("guard-root.db"));Db stage=new Db(dir.resolve("guard-stage.db"))){
            fixture(root,stage,2);List<MigrationCopyR29.Table> tables=MigrationCopyR29.tables(root);catalog(root,stage,tables);int[] calls={0};step(root,stage,tables,new int[]{1},calls);String before=cursor(stage,"r29_prepare_000");
            root.execSQL("DROP TRIGGER r29_copy_track_r29_prepare_000_update");
            check("P12_LOST_TRIGGER_FAILS_CLOSED_BEFORE_COPY",rejected(()->step(root,stage,tables,new int[]{100},calls),"R29_MIGRATION_TRACKER_INCOMPLETE")&&before.equals(cursor(stage,"r29_prepare_000")));
            root.execSQL("CREATE TRIGGER r29_copy_track_r29_prepare_000_update AFTER UPDATE ON r29_prepare_000 BEGIN UPDATE r29_source_watermark SET generation=generation+1 WHERE table_name='R29_PREPARE_000'; END");
            check("P13_SAME_NAME_WRONG_TRIGGER_REJECTED",rejected(()->step(root,stage,tables,new int[]{100},calls),"R29_MIGRATION_TRACKER_SQL_MISMATCH")&&before.equals(cursor(stage,"r29_prepare_000")));
            for(String event:new String[]{"insert","update","delete"})root.execSQL("DROP TRIGGER r29_copy_track_r29_prepare_000_"+event);
            root.execSQL("DELETE FROM r29_source_watermark WHERE table_name='r29_prepare_000'");
            check("P14_ALL_TRACKERS_LOST_AFTER_COPY_CANNOT_RESET_GENERATION",rejected(()->step(root,stage,tables,new int[]{100},calls),"R29_MIGRATION_TRACKERS_LOST_AFTER_COPY_STARTED")&&before.equals(cursor(stage,"r29_prepare_000"))&&calls[0]==1);
            check("P15_STARTED_MISSING_WATERMARK_DIAGNOSTICS_FAIL_CLOSED",rejected(()->MigrationCopyR29.diagnostics(root,stage),"R29_SOURCE_NOT_TRACKED"));
        }
        try(Db root=new Db(dir.resolve("ddl-root.db"));Db stage=new Db(dir.resolve("ddl-stage.db"))){
            fixture(root,stage,2);List<MigrationCopyR29.Table> tables=MigrationCopyR29.tables(root);catalog(root,stage,tables);root.execSQL("ALTER TABLE r29_prepare_000 ADD COLUMN later TEXT");
            check("P16_SOURCE_SCHEMA_DRIFT_FAILS_BEFORE_ADMISSION",rejected(()->step(root,stage,tables,new int[]{100},new int[]{0}),"R29_SOURCE_SCHEMA_CHANGED_DURING_COPY")&&n(stage,"SELECT max(attempt) FROM r29_copy_manifest")==0);
        }
        try(Db root=new Db(dir.resolve("watermark-root.db"));Db stage=new Db(dir.resolve("watermark-stage.db"))){
            fixture(root,stage,2);catalog(root,stage,MigrationCopyR29.tables(root));root.execSQL("CREATE TABLE r29_source_watermark(table_name TEXT PRIMARY KEY,generation INTEGER)");
            check("P17_WRONG_WATERMARK_DDL_REJECTED",rejected(()->step(root,stage,MigrationCopyR29.tables(root),new int[]{100},new int[]{0}),"R29_MIGRATION_WATERMARK_SCHEMA_MISMATCH"));
        }
    }
    static void corrupt(Path dir)throws Exception{
        try(Db root=new Db(dir.resolve("corrupt-root.db"));Db stage=new Db(dir.resolve("corrupt-stage.db"))){
            fixture(root,stage,2);List<MigrationCopyR29.Table> tables=MigrationCopyR29.tables(root);catalog(root,stage,tables);int[] calls={0};
            for(int i=0;i<6&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFYING'")==0;i++)step(root,stage,tables,new int[]{100},calls);
            stage.execSQL("UPDATE r29_copy_rows SET payload=X'00' WHERE table_name='r29_prepare_000'");
            check("P18_CORRUPT_TARGET_BYTES_CANNOT_BECOME_VERIFIED",rejected(()->step(root,stage,tables,new int[]{100},calls),"R29_DESTINATION_ROW_CHECKSUM")&&n(stage,"SELECT count(*) FROM r29_copy_manifest WHERE state='VERIFIED_BASELINE'")==0&&n(root,"SELECT count(*) FROM r29_prepare_000 WHERE value='original-0'")==1);
        }
    }
    public static void main(String[]args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path dir=Files.createTempDirectory("r29-lazy-prepare-");boolean old=args.length>0&&args[0].equals("--old-prefix-control");
        try{lazyCatalog(dir,old);if(!old){retryAndReopen(dir);guards(dir);corrupt(dir);}System.out.println("R29_MIGRATION_PREPARE|ALL_PASS|checks="+checks+"|LAZY_SELECTED_TABLE_TRACKING_REAL_SQLITE_NOT_OS_CRASH");}
        finally{try(java.util.stream.Stream<Path>files=Files.walk(dir)){for(Path p:files.sorted(java.util.Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
    }
}
