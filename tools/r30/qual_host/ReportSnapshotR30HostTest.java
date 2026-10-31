package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Actual SQLite WAL reader/writer and unchanged production report code. HOST is not Android execution. */
public final class ReportSnapshotR30HostTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static long value(Map<String,Object> facts,String key){Object v=facts.get(key);require(v instanceof Number,"MISSING_METRIC:"+key+":"+v);return((Number)v).longValue();}
    static void seed(SQLiteDatabase db){
        for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
        for(String ddl:RuntimeSchemaR26.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
        db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INT)");db.execSQL("INSERT INTO draws VALUES('202610310001',0)");
        db.execSQL("CREATE TABLE su2_storage_budget(category TEXT PRIMARY KEY,cap_bytes INT,measured_bytes INT,measured_at INT,action TEXT) WITHOUT ROWID");db.execSQL("INSERT INTO su2_storage_budget VALUES('CACHE',1000,10,1,'KEEP')");
        db.execSQL("INSERT INTO r26_evolution(attempt_id,campaign_id,policy_hash,operation,category,structure_hash) VALUES('a','host','policy','HOST','HOST','host')");
        PolicySpecR27 spec=new PolicySpecR27(PolicyAstR27.leaf("freq",1,32),1,PolicySpecR27.Rule.ALWAYS,0,0,1,false,.01,.01,IdeaFamilyR27.meta("I01","HOST",0,32,"report snapshot fixture"));
        db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at) VALUES('policy',?,1,?,1,'ACTIVE','HOST',1,1)",new Object[]{spec.fullJson(),StoreR27.pack(new double[10])});
        db.execSQL("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,updated_at) VALUES('policy',1,.001,0,0,1,1,1,0,0,1,?,1)",new Object[]{SequentialQualificationR27.ACCUMULATING});
        db.execSQL("INSERT INTO r29_qual_receipt VALUES('policy',1,'202610310001',1,1,.7,'host','ACCUMULATING',1)");
        db.execSQL("INSERT INTO r27_chart_cache VALUES('health','summary',?,1)",new Object[]{"{\"scope\":\"真实UTF8 & ; :\\n边界\"}"});
        db.execSQL("INSERT INTO r27_chart_cache VALUES('health','source',?,2)",new Object[]{String.join("",Collections.nCopies(17000,"x"))});
        db.execSQL(AtomScoreR27.CONTRIBUTION_DDL_R30);db.execSQL("INSERT INTO r30_atom_contribution VALUES('atom','dataset',1,660,659,1318,?,.001,'SYNTHETIC_H_SELECT',1)",new Object[]{.0123456789012345});
    }
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");Path file=Files.createTempFile("r30-report-snapshot-",".db");ExecutorService writer=Executors.newSingleThreadExecutor();
        try(Connection setup=DriverManager.getConnection("jdbc:sqlite:"+file);Connection read=DriverManager.getConnection("jdbc:sqlite:"+file)){
            try(Statement s=setup.createStatement()){s.execute("PRAGMA journal_mode=WAL");s.execute("PRAGMA busy_timeout=2000");}
            SQLiteDatabase db=new SQLiteDatabase(setup),ro=new SQLiteDatabase(read);seed(db);
            ReportR27.R30Input before=ReportSnapshotR30.capture(ro);require(before.errors.isEmpty(),"CAPTURE_FAILED:"+before.errors);
            require(value(before.facts,"futurePeriods")==1&&value(before.facts,"futureActs")==1&&value(before.facts,"futureHits")==1&&value(before.facts,"evolutionAttempts")==1,"FIRST_VIEW_NOT_FROM_ACTUAL_TABLES");
            Future<?> committed=writer.submit(()->{try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file)){c.setAutoCommit(false);try(Statement s=c.createStatement()){s.execute("INSERT INTO draws VALUES('202610310002',0)");s.execute("INSERT INTO r29_qual_receipt VALUES('policy',2,'202610310002',1,0,-.3,'host2','ACCUMULATING',2)");s.execute("UPDATE r27_eprocess SET n=2,acts=2,last_ordinal=2");s.execute("INSERT INTO r26_evolution(attempt_id,campaign_id,policy_hash,operation,category,structure_hash) VALUES('b','host','policy','HOST','HOST','host')");}c.commit();}catch(Exception error){throw new IllegalStateException(error);}});
            committed.get(2,TimeUnit.SECONDS); // No product writer lock is held while formatting chapters.
            List<Map<String,Object>> chapters=ReportR27.chaptersR30(before);require(chapters.size()==12,"NOT_TWELVE_CHAPTERS");for(Map<String,Object> chapter:chapters){Map<?,?> common=(Map<?,?>)chapter.get("commonMetrics");require(common.get("futurePeriods").equals(1L)&&common.get("evolutionAttempts").equals(1L),"CHAPTER_REQUERIED_NEWER_ROWS");require(((Map<?,?>)chapter.get("snapshotWatermark")).get("latestIssue").equals("202610310001"),"CHAPTER_WATERMARK_DRIFTED");require(chapter.get("reportConsistencyConflicts").equals(0L),"VALID_RECEIPTS_FALSELY_CONFLICTED");}
            ReportR27.R30Input after=ReportSnapshotR30.capture(ro);require(after.errors.isEmpty(),"SECOND_CAPTURE_FAILED:"+after.errors);require(value(after.facts,"futurePeriods")==2&&value(after.facts,"futureHits")==1&&value(after.facts,"evolutionAttempts")==2,"NEXT_CAPTURE_STALE");
            Map<?,?> storage=(Map<?,?>)((Map<?,?>)after.watermark.get("tables")).get("su2_storage_budget");require(Boolean.TRUE.equals(storage.get("withoutRowid"))&&storage.get("maxRowid")==null&&((Map<?,?>)storage.get("maxPrimaryKeyByColumn")).get("category").equals("CACHE"),"WITHOUT_ROWID_WATERMARK_FABRICATED");
            require(after.facts.get("writerWindows")==null,"MISSING_TABLE_FALSELY_REPORTED_ZERO");
            List<?> caches=(List<?>)after.data.get("runtimeCaches");boolean fullUtf8=false,clipped=false;for(Object item:caches){Map<?,?> row=(Map<?,?>)item;if("summary".equals(row.get("key")))fullUtf8="{\"scope\":\"真实UTF8 & ; :\\n边界\"}".equals(row.get("payload_json"));if("source".equals(row.get("key")))clipped=row.get("payload_json") instanceof Map;}require(fullUtf8&&clipped,"TEXT_ROUNDTRIP_OR_TRUNCATION_REFERENCE_WRONG");
            Map<?,?> atom=(Map<?,?>)((List<?>)after.data.get("atoms")).get(0);require(((Number)atom.get("n_periods")).longValue()==659&&Double.compare(((Number)atom.get("mean_gain")).doubleValue(),.0123456789012345)==0,"ACTUAL_BRIER_FIELDS_OR_NUMERIC_ROUNDTRIP_LOST");
            db.execSQL("UPDATE r27_eprocess SET n=3");ReportR27.R30Input conflict=ReportSnapshotR30.capture(ro);require(value(conflict.facts,"receiptStateMismatches")==1&&ReportR27.chaptersR30(conflict).get(0).get("reportConsistencyConflicts").equals(1L),"ACTUAL_RECEIPT_CONFLICT_HIDDEN");
            Map<String,Object> document=ReportR29.document(chapters);require(((List<?>)document.get("chapters")).size()==12&&ReportR27.markdownR30(chapters).contains("R30_12"),"JSON_MARKDOWN_NOT_SAME_CHAPTER_LIST");
            System.out.println("R30_REPORT_SNAPSHOT_SQLITE|12_CHAPTERS_ONE_DETACHED_READ_VIEW|WAL_WRITER_COMMITS_DURING_FORMATTING|NEXT_CAPTURE_ADVANCES|COMMON_DENOMINATORS_AND_WATERMARKS_MATCH|WITHOUT_ROWID_EXPLICIT|MISSING_NOT_ZERO|UTF8_AND_CLIPPED_REFERENCES|REAL_RECEIPT_CONFLICT_EXPOSED|NO_ANDROID_OR_NATURAL_CLAIM|PASS");
        }finally{writer.shutdownNow();Files.deleteIfExists(file);Files.deleteIfExists(Path.of(file+"-wal"));Files.deleteIfExists(Path.of(file+"-shm"));}
    }
}
