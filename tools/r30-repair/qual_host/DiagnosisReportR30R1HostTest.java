package com.ethk3.novaonepro.strategyv2;
import java.sql.*;
import java.util.*;
import android.database.sqlite.SQLiteDatabase;
/** Actual production bytecode and JDBC SQLite. HOST adapters, never Android/device evidence. */
public final class DiagnosisReportR30R1HostTest {
    static void need(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked") static Map<String,Object> report(SQLiteDatabase db){return(Map<String,Object>)JsonR27.parse(DiagnosisStoreSU2.report(db)[1]);}
    static long n(Connection db,String sql)throws Exception{try(Statement s=db.createStatement();ResultSet r=s.executeQuery(sql)){need(r.next(),sql);return r.getLong(1);}}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(connection);
            Map<String,Object> missing=report(db);need("READ_SNAPSHOT".equals(missing.get("failureStage")),"real missing SQLite source");need(missing.get("snapshotHash")==null,"no fabricated input hash");
            db.execSQL("CREATE TABLE su2_diag_snapshot(report_sequence INTEGER PRIMARY KEY,snapshot TEXT)");
            db.execSQL("CREATE TABLE su2_diag_state(singleton INTEGER PRIMARY KEY,ledger_text TEXT)");
            need(!report(db).containsKey("failureStage"),"readable empty table is insufficient, not failed");
            String input="SU2DIAG\t1\ndataset\tSYNTHETIC_STAGE_TEST\nseq\t2000\nprobe\tCONTMAT\t-\t\n";
            db.execSQL("INSERT INTO su2_diag_snapshot VALUES(1,?)",new Object[]{input});
            long changes=n(connection,"SELECT total_changes()"),schema=n(connection,"SELECT count(*) FROM sqlite_master");
            for(String phase:new String[]{"READ_SNAPSHOT","PARSE_SNAPSHOT","DIAGNOSE","READ_LEDGER","RENDER_MARKDOWN","RENDER_JSON"}){
                DiagnosisStoreSU2.reportFaultR30R1.set(stage->{if(stage.equals(phase))throw new IllegalStateException("SYNTHETIC_SECRET_MESSAGE_DO_NOT_EXPORT",new NullPointerException("SYNTHETIC_PAYLOAD_DO_NOT_EXPORT"));});
                try{Map<String,Object> result=report(db);need(phase.equals(result.get("failureStage")),phase);need(((List<?>)result.get("errorChain")).size()<=12,"bounded frames");need(((List<?>)result.get("exceptionTypes")).size()==2,"nested cause retained");need(!JsonR27.write(result).contains("SYNTHETIC_SECRET_MESSAGE_DO_NOT_EXPORT")&&!JsonR27.write(result).contains("SYNTHETIC_PAYLOAD_DO_NOT_EXPORT"),"no raw exception payload");need(phase.equals("READ_SNAPSHOT")?result.get("snapshotHash")==null:HashingSU1.text(input).equals(result.get("snapshotHash")),"actual captured input hash");}
                finally{DiagnosisStoreSU2.reportFaultR30R1.remove();}
            }
            need(!report(db).containsKey("failureStage"),"valid source after fault hook removed");
            need(n(connection,"SELECT total_changes()")==changes&&n(connection,"SELECT count(*) FROM sqlite_master")==schema,"report never writes or creates schema");
            db.execSQL("UPDATE su2_diag_snapshot SET snapshot='SYNTHETIC_CORRUPT_HEADER'");
            Map<String,Object> corrupt=report(db);need("PARSE_SNAPSHOT".equals(corrupt.get("failureStage")),"malformed header classification");
            System.out.println("FR_DIAGNOSIS_REPORT_HOST|REAL_SQLITE_AND_PRODUCTION_BYTECODE|6_FAILURE_STAGES|CAUSE_CHAIN_BOUNDS_AND_REDACTION|READ_ONLY|CORRUPT_HEADER|HOST_NOT_ANDROID|PASS");
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER NOT NULL)");db.execSQL("INSERT INTO draws VALUES('202611010001',0)");
            db.execSQL("CREATE TABLE trades(target_issue TEXT,is_realtime_real INTEGER)");db.execSQL("INSERT INTO trades VALUES('202611010001',1),('202611010001',1)");
            db.execSQL("CREATE TABLE research_matrix_snapshot_r930(matrix_kind TEXT,matrix_hash TEXT,target_issue TEXT,frozen_at INTEGER)");
            db.execSQL("INSERT INTO research_matrix_snapshot_r930 VALUES('EVIDENCE_TEACHER','SYNTHETIC_EXACT_ORIGINAL_HASH','202611010001',1),('EVIDENCE_TEACHER','SYNTHETIC_SECOND_HASH','202611010002',2)");
            long reportChanges=n(connection,"SELECT total_changes()"),reportSchema=n(connection,"SELECT count(*) FROM sqlite_master");db.execSQL("PRAGMA query_only=ON");
            ReportR27.R30Input captured=ReportSnapshotR30.capture(db,Collections.singletonMap("snapshotSha256","SYNTHETIC_JDBC_SNAPSHOT"));
            need(captured.errors.isEmpty(),"real production report SELECT: "+captured.errors);need(((Number)captured.facts.get("draws")).longValue()==1,"exact draw source");need(((Number)captured.facts.get("duplicateRealIssues")).longValue()==1,"original REAL conflict retained");
            Map<?,?> matrixIndex=(Map<?,?>)((Map<?,?>)captured.data.get("objectIndex")).get("matrices");boolean found=false;for(Object item:(List<?>)matrixIndex.get("objects")){Map<?,?> row=(Map<?,?>)item;if("R930:EVIDENCE_TEACHER".equals(row.get("objectId"))){need(((Number)row.get("retainedCount")).longValue()==2,"full matrix denominator");need("SYNTHETIC_SECOND_HASH".equals(((Map<?,?>)row.get("runtimeReference")).get("latestOriginalHash")),"exact original hash");found=true;}}need(found,"complete matrix index");
            need(captured.metricResults.size()==44&&ReportR29.fromSnapshotR30(captured).size()==12,"actual read feeds44 metrics12 chapters");need(n(connection,"SELECT total_changes()")==reportChanges&&n(connection,"SELECT count(*) FROM sqlite_master")==reportSchema,"report capture writes no state");
            System.out.println("FR_REPORT_CAPTURE_HOST|ACTUAL_PRODUCTION_SELECT_NATIVE_SQLITE|44_METRICS_12_CHAPTERS|FULL_MATRIX_DENOMINATOR|ORIGINAL_REAL_NEGATIVE_EVIDENCE|QUERY_ONLY_NO_WRITES|HOST_NOT_ANDROID|PASS");
        }
    }
}
