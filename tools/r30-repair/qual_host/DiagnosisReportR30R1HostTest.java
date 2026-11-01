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
        }
    }
}
