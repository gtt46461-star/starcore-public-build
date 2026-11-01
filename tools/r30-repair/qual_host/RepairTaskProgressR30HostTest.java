package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import java.sql.*;
public final class RepairTaskProgressR30HostTest {
    interface Work{void run()throws Exception;}
    static void need(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static void tx(SQLiteDatabase db,Work work)throws Exception{db.jdbc.setAutoCommit(false);try{work.run();db.jdbc.commit();}catch(Exception|Error failure){db.jdbc.rollback();throw failure;}finally{db.jdbc.setAutoCommit(true);}}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(c);db.execSQL("PRAGMA foreign_keys=ON");db.execSQL("CREATE TABLE protected_legacy_state(id INTEGER PRIMARY KEY,bytes BLOB)");db.execSQL("INSERT INTO protected_legacy_state VALUES(1,x'01020304')");
            long before=n(db,"PRAGMA schema_version");try{tx(db,()->{int at=0;for(String ddl:ResearchSchemaR30R1.DDL){db.execSQL(ddl);if(++at==4)throw new IllegalStateException("SYNTHETIC_INTERRUPTED_ADDITIVE_MIGRATION");}});throw new AssertionError();}catch(IllegalStateException expected){}
            need(n(db,"PRAGMA schema_version")==before,"interrupted schema transaction rolls back");need(n(db,"SELECT count(*) FROM sqlite_master WHERE type='table'")==1,"no partial schema");
            tx(db,()->{for(String ddl:ResearchSchemaR30R1.DDL)db.execSQL(ddl);});long schema=n(db,"PRAGMA schema_version");tx(db,()->{for(String ddl:ResearchSchemaR30R1.DDL)db.execSQL(ddl);});need(schema==n(db,"PRAGMA schema_version"),"idempotent migration");need(n(db,"SELECT count(*) FROM sqlite_master WHERE type='table'")==7,"only six additive physical tables");
            db.execSQL("CREATE TABLE product(id TEXT PRIMARY KEY,value INTEGER)");ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();batch.add("INSERT INTO product VALUES('logical-step',1)");
            final int[] guardCalls={0};batch.guardBefore(tx->{guardCalls[0]++;return 0;});batch.after=tx->{throw new IllegalStateException("SYNTHETIC_FINAL_SOURCE_CHANGED");};
            ResearchStoreR30R1.Facts facts=new ResearchStoreR30R1.Facts(18460,18461,1L,null,null,HashingSU1.text("exact-committed-artifact"));ResearchStoreR30R1.appendProgress(batch,"r27-evolution","SYNTHETIC_SESSION",7,1200,facts);
            // This host fixture deliberately uses a non-R27 task row adapter below only for the legacy display SQL.
            db.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,cursor INTEGER,last_progress INTEGER,steps INTEGER,first_progress_at INTEGER,owner TEXT,fence INTEGER)");db.execSQL("INSERT INTO r26_task VALUES('r27-evolution',0,0,0,0,'SYNTHETIC_SESSION',7)");
            try{tx(db,()->ResearchWriteQueueR27.execute(db,batch,"FAULT"));throw new AssertionError();}catch(IllegalStateException expected){}
            need(n(db,"SELECT count(*) FROM product")==0&&n(db,"SELECT count(*) FROM r30r1_task_progress")==0,"final guard rolls back artifact and typed facts together");need(n(db,"SELECT cursor FROM r26_task")==0,"legacy display cursor never advanced on rollback");need(guardCalls[0]==1,"original before guard preserved");
            ResearchWriteQueueR27.Batch retry=new ResearchWriteQueueR27.Batch();retry.add("INSERT OR IGNORE INTO product VALUES('logical-step',1)");ResearchStoreR30R1.appendProgress(retry,"r27-evolution","SYNTHETIC_SESSION",7,1200,facts);tx(db,()->ResearchWriteQueueR27.execute(db,retry,"RETRY"));
            need(n(db,"SELECT count(*) FROM product")==1&&n(db,"SELECT work_cursor FROM r30r1_task_progress")==18460,"retry commits both facts exactly once");need(n(db,"SELECT last_commit_clock_ordinal FROM r30r1_task_progress")==1200,"calendar clock distinct from phase");
            tx(db,()->ResearchWriteQueueR27.execute(db,retry,"REPEATED"));need(n(db,"SELECT steps FROM r26_task")==1,"repeated exact artifact cannot fabricate another progress step");
            try(Cursor preserved=db.rawQuery("SELECT hex(bytes) FROM protected_legacy_state",null)){need(preserved.moveToFirst()&&"01020304".equals(preserved.getString(0)),"original model bytes preserved");}
            System.out.println("REPAIR_TYPED_PROGRESS_NATIVE_SQLITE_HOST_PASS six_additive_tables=1 interrupted_migration_rollback=1 ensure_idempotent=1 old_bytes_unchanged=1 product_and_progress_atomic=1 final_guard_preserved=1 failed_receipt_no_advance=1 retry_idempotent=1 phase_and_calendar_distinct=1 HOST_NOT_ANDROID");
        }
    }
}
