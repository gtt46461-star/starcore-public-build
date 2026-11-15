package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
/** Actual original retention producer + SQLite transactions and files; synthetic fixtures are not device results. */
public final class ResearchRetentionR30HostTest {
    interface Work{void run()throws Exception;}
    static void tx(SQLiteDatabase db,Work work)throws Exception{db.jdbc.setAutoCommit(false);try{work.run();db.jdbc.commit();}catch(Exception|Error failed){db.jdbc.rollback();throw failed;}finally{db.jdbc.setAutoCommit(true);}}
    static void need(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),"SCALAR_MISSING");return c.getLong(0);}}
    static byte[] blob(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),"BLOB_ROW_MISSING");return c.getBlob(0);}}
    static void reject(Work work,String why)throws Exception{try{work.run();throw new AssertionError(why);}catch(android.database.sqlite.SQLiteException expected){}}
    static void schema(SQLiteDatabase db)throws Exception{
        tx(db,()->{RuntimeSchemaR26.ensure(db);for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);for(String ddl:StrategySchemaSU2.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);MatrixStoreSU2.ensureMetadata(db);});
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path data=Files.createTempDirectory("r30-retention-host-");File database=new File(data.toFile(),"databases/root.db");database.getParentFile().mkdirs();
        try(Connection conn=DriverManager.getConnection("jdbc:sqlite:"+database)){
            SQLiteDatabase db=new SQLiteDatabase(conn);schema(db);
            tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));long installed=n(db,"PRAGMA schema_version");tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));need(n(db,"PRAGMA schema_version")==installed,"ENSURE_NOT_IDEMPOTENT");
            byte[] frozen=new byte[400];new Random(300805).nextBytes(frozen);
            db.execSQL("INSERT INTO r26_frozen VALUES('frozen-original',1,0,'checkpoint','state','source',1,1,2,?)",new Object[]{frozen});
            tx(db,()->RetentionR26.prune(db,100000));
            need(n(db,"SELECT count(*) FROM r26_frozen")==0,"ORIGINAL_WINDOW_NOT_CLEANED");need(Arrays.equals(frozen,blob(db,"SELECT probability_table FROM r30_keep_r26_frozen WHERE policy_hash='frozen-original'")),"ORIGINAL_FROZEN_BYTES_LOST");
            byte[] model=new byte[1024];new Random(7).nextBytes(model);db.execSQL("INSERT INTO r29_model_checkpoint VALUES('HOST_SYNTHETIC','abi','dataset','through','hash',?,1)",new Object[]{model});
            tx(db,()->db.execSQL("UPDATE r29_model_checkpoint SET artifact=X'' WHERE owner='HOST_SYNTHETIC'"));need(Arrays.equals(model,blob(db,"SELECT artifact FROM r30_keep_r29_model_checkpoint")),"MODEL_PAYLOAD_CLEAR_LOST_OLD_STATE");
            long before=n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint");
            db.jdbc.setAutoCommit(false);try{db.execSQL("DELETE FROM r29_model_checkpoint");need(n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint")==before+1,"ARCHIVE_NOT_VISIBLE_IN_SAME_TRANSACTION");}finally{db.jdbc.rollback();db.jdbc.setAutoCommit(true);}
            need(n(db,"SELECT count(*) FROM r29_model_checkpoint")==1&&n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint")==before,"ROLLBACK_ADVANCED_ARCHIVE_OR_REMOVED_MODEL");
            reject(()->db.execSQL("DELETE FROM r30_keep_r29_model_checkpoint"),"ARCHIVE_DELETE_ALLOWED");reject(()->db.execSQL("UPDATE r30_keep_r29_model_checkpoint SET artifact=X''"),"ARCHIVE_UPDATE_ALLOWED");
            // A source adds a field: old trigger remains but its fingerprint is disabled; exactly one full row archives.
            db.execSQL("ALTER TABLE r29_model_checkpoint ADD COLUMN extra_state BLOB");tx(db,()->ResearchRetentionStoreR30.ensureProtected(db));db.execSQL("UPDATE r29_model_checkpoint SET extra_state=?",new Object[]{model});tx(db,()->db.execSQL("DELETE FROM r29_model_checkpoint"));
            need(n(db,"SELECT count(*) FROM r30_keep_r29_model_checkpoint")==before+1&&Arrays.equals(model,blob(db,"SELECT extra_state FROM r30_keep_r29_model_checkpoint ORDER BY __r30_id DESC LIMIT 1")),"SCHEMA_EVOLUTION_LOST_FIELD_OR_DOUBLE_COUNTED");
            need(n(db,"SELECT retained_rows FROM r30_retention_archive_index WHERE source_table='r29_model_checkpoint'")==before+1,"ARCHIVE_COUNTER_NOT_ATOMIC");
            MatrixSnapshotSU2.Data matrix=new MatrixSnapshotSU2.Data("{\"scope\":\"HOST_SYNTHETIC\"}",Collections.emptyList(),new AtomStreamSU2());
            tx(db,()->{for(int i=1;i<=10;i++)MatrixStoreSU2.save(db,"host-matrix",i,matrix,i);});
            need(n(db,"SELECT count(*) FROM su2_matrix_snapshot")==8&&n(db,"SELECT count(*) FROM r30_keep_su2_matrix_snapshot")==2,"ORIGINAL_MATRIX_WINDOW_OR_OLD_SNAPSHOTS_LOST");
            need(Arrays.equals(matrix.stream,blob(db,"SELECT reason_state FROM r30_keep_su2_matrix_snapshot LIMIT 1")),"ORIGINAL_MATRIX_STATE_BYTES_LOST");
            File retired=new File(data.toFile(),ResearchRetentionR30.RETIRED);byte[] oldPayload=new byte[2*1024*1024];new Random(99).nextBytes(oldPayload);
            try(Connection source=DriverManager.getConnection("jdbc:sqlite:"+retired)){
                SQLiteDatabase src=new SQLiteDatabase(source);src.execSQL("CREATE TABLE retired_fixture(id TEXT PRIMARY KEY,model BLOB)");src.execSQL("INSERT INTO retired_fixture VALUES('original',?)",new Object[]{oldPayload});
                TableCopierR30.Outcome copy=TableCopierR30.copy(src,db,(operation,body)->tx(db,()->body.run(db)),"retired_fixture","FB_HOST","R29_RP_TO_ROOT");need(copy.state.equals("COPIED")&&copy.copiedRows==1,"ORIGINAL_FOLDBACK_COPY_FAILED");
            }
            File cache=new File(data.toFile(),"code_cache/recomputable");cache.getParentFile().mkdirs();Files.write(cache.toPath(),new byte[128*1024]);
            File original=new File(data.toFile(),"files/model-bundles-r929/current");original.getParentFile().mkdirs();Files.write(original.toPath(),model);
            ResearchRetentionStoreR30.Writer writer=(operation,batch)->tx(db,()->ResearchWriteQueueR27.exec(db,batch));
            ResearchRetentionR30.Result noProof=ResearchRetentionStoreR30.cleanFiles(db,data.toFile(),1,writer);need(retired.exists()&&!cache.exists()&&original.exists()&&noProof.state.equals("BLOCKED_PROTECTED_DATA_BUDGET"),"UNPROVEN_RETIRED_OR_MODEL_DELETED");
            db.execSQL(R29FoldBackR30.STATE_DDL);db.execSQL("CREATE TABLE r29_database_route(singleton INTEGER PRIMARY KEY,phase TEXT)");db.execSQL("INSERT INTO r29_database_route VALUES(1,'RETIRED_BY_R30')");db.execSQL("INSERT INTO r30_foldback_state VALUES(1,'FOLDED_BACK','FB_HOST',1,1,0,'',1)");
            need("FB_HOST".equals(ResearchRetentionStoreR30.verifiedFoldback(db)),"REAL_EMPTY_MISSING_COLUMNS_COPY_RECEIPT_NOT_RECOGNIZED");
            db.execSQL("UPDATE r30_foldback_state SET state='NO_ACTIVE_R29_ROUTE:RETIRED_BY_R30',run_id='FB_RESTART',tables=0,rows=0");
            need("FB_HOST".equals(ResearchRetentionStoreR30.verifiedFoldback(db)),"SECOND_START_OVERWROTE_COMPLETE_COPY_PROOF");
            db.execSQL("CREATE TABLE host_reference_failure(enabled INTEGER)");db.execSQL("INSERT INTO host_reference_failure VALUES(1)");db.execSQL("CREATE TRIGGER host_reference_failure BEFORE INSERT ON r30_copy_log WHEN NEW.direction='R30_RETIRED_FILE_DELETE_REFERENCE' AND (SELECT enabled FROM host_reference_failure)=1 BEGIN SELECT RAISE(ABORT,'HOST_INJECTED_REFERENCE_WRITE_FAILURE'); END");
            reject(()->ResearchRetentionStoreR30.cleanFiles(db,data.toFile(),1,writer),"FAILED_REFERENCE_WRITE_DID_NOT_STOP_UNLINK");need(retired.isFile(),"FILE_DELETED_BEFORE_DURABLE_REFERENCE");
            db.execSQL("UPDATE host_reference_failure SET enabled=0");long budget=ResearchRetentionR30.measure(data.toFile()).bytes-retired.length()/2;
            ResearchRetentionR30.Result done=ResearchRetentionStoreR30.cleanFiles(db,data.toFile(),budget,writer);
            need(!retired.exists()&&original.exists()&&done.state.equals("WITHIN_BUDGET"),"VERIFIED_DUPLICATE_NOT_RECLAIMED_OR_BUDGET_FALSE");
            need(n(db,"SELECT count(*) FROM r30_copy_log WHERE direction='R30_RETIRED_FILE_DELETE_REFERENCE'")==1&&n(db,"SELECT count(*) FROM r30_retention_file_action WHERE kind='VERIFIED_RETIRED_DUPLICATE' AND state='DELETED'")==1,"DURABLE_FILE_AUDIT_MISSING");
            need(Arrays.equals(oldPayload,blob(db,"SELECT model FROM retired_fixture WHERE id='original'")),"MERGED_ROOT_MODEL_BYTES_CHANGED");
            for(String[] row:new String[][]{{"p01_three_scores","latest"},{"r30","q08"},{"atomnet","ten-position-r30"},{"calibration-r30","policy"}})db.execSQL("INSERT OR REPLACE INTO r27_chart_cache VALUES(?,?,'{}',1)",new Object[]{row[0],row[1]});
            tx(db,()->db.execSQL(ResearchRetentionStoreR30.visualCacheDeleteSql()));need(n(db,"SELECT count(*) FROM r27_chart_cache WHERE panel_id='p01_three_scores'")==0&&n(db,"SELECT count(*) FROM r27_chart_cache WHERE panel_id IN ('r30','atomnet','calibration-r30')")==3,"UNIQUE_Q08_MODEL_CALIBRATION_CACHE_EVIDENCE_DELETED");
            tx(db,()->db.execSQL("INSERT INTO r30_retention_run VALUES('HOST_RUN',1,?,?)",new Object[]{budget,JsonR27.write(done.json())}));
            ReportR27.R30Input captured=ReportSnapshotR30.capture(db);need(captured.errors.isEmpty(),"RETENTION_REPORT_CAPTURE_FAILED:"+captured.errors);List<Map<String,Object>> chapters=ReportR29.chapters(captured);
            need(ReportR27.summaryR30(chapters).contains("存储预算")&&ReportR27.markdownR30(chapters).contains("retention")&&((Map<?,?>)chapters.get(10).get("retention")).get("lastRun")==captured.data.get("retentionRun"),"STORAGE_UI_AND_EXPORT_NOT_SAME_CAPTURED_RESULT");
            System.out.println("R30_RETENTION_SQLITE_FILES|ACTUAL_ORIGINAL_RETENTION_WINDOW|FROZEN_AND_MODEL_BLOB_EXACT|SAME_TX_ROLLBACK|IMMUTABLE_ARCHIVE|SCHEMA_EVOLUTION_SINGLE_ARCHIVE|ACTUAL_MATRIX_SAVE_8_LIVE_2_PROTECTED|ORIGINAL_FOLDBACK_COPY_AND_PREUNLINK_REFERENCE|ONLY_RECOMPUTABLE_CACHE|PROTECTED_BUDGET_REMAINS_BLOCKED|HOST_NOT_DEVICE|PASS");
        }
    }
}
