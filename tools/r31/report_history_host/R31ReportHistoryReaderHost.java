package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.*;

/** Actual ReportSnapshot/History scalar; direct fixture states are not an importer/training/device PASS. */
public final class R31ReportHistoryReaderHost {
    static int assertions,finalSelects;static String largeSelect;static Runnable beforeSelect;static final List<Map<String,Object>> outcomes=new ArrayList<>();
    static void need(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    static long number(Object value){return ((Number)value).longValue();}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static final String NUMBERS="01020304050607080910";
    static void legacy(SQLiteDatabase db){db.execSQL(HistoryImportR27.DDL);for(int i=1;i<=3;i++)db.execSQL("INSERT INTO r27_history_import VALUES(?,?,?,?,?,?,?)",new Object[]{issue(i),i,NUMBERS,new String[]{"IMPORTED","MATCH","CONFLICT"}[i-1],null,"HOST_READER_FIXTURE",System.currentTimeMillis()});}
    static String issue(int n){return String.format(java.util.Locale.ROOT,"20261104%04d",n);}
    static void schema(SQLiteDatabase db){db.beginTransaction();try{HistoryImportR27.ensureR31PublicationSchema(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static void job(SQLiteDatabase db,String id){db.execSQL("INSERT INTO r31_history_import_run(job_id,source_hash,parser_abi,source_path,source_bytes,data_role,status,owner_epoch,fence,owner_pid,owner_boot,lease_elapsed_ms,created_at) VALUES(?,?,?,?,?,?,'STAGING','HOST_READER_FIXTURE',1,?,'HOST_READER_FIXTURE',0,?)",new Object[]{id,HashingSU1.text(id),HistoryImportR27.R31_IMPORT_ABI,"HOST_ENGINEERING_FIXTURE_NOT_FILE_IMPORT",0,HistoryImportR27.R31_ROLE,Math.toIntExact(ProcessHandle.current().pid()),System.currentTimeMillis()});}
    static void row(SQLiteDatabase db,String job,int n,String status,int selfConflict){db.execSQL("INSERT INTO r31_history_import_row(job_id,issue,ordinal,numbers,status,db_numbers,source_line,duplicate_conflicts,row_hash) VALUES(?,?,?,?,?,NULL,?,?,?)",new Object[]{job,issue(n),n,NUMBERS,status,n,selfConflict,HashingSU1.fields("HOST_READER_FIXTURE",job,issue(n),status)});}
    static void complete(SQLiteDatabase db,String job,int sequence){db.execSQL("UPDATE r31_history_import_run SET status='COMPLETE',publish_seq=?,completed_at=?,manifest_hash=?,row_count=(SELECT count(*) FROM r31_history_import_row WHERE job_id=?) WHERE job_id=?",new Object[]{sequence,System.currentTimeMillis(),HashingSU1.text(job),job,job});}
    static ReportR27.R30Input capture(SQLiteDatabase db,String label,Long expected,String state){
        long changes=scalar(db,"SELECT total_changes()"),schema=scalar(db,"PRAGMA schema_version");int prior=finalSelects;db.execSQL("PRAGMA query_only=ON");
        ReportR27.R30Input in;try{in=ReportSnapshotR30.capture(db);}finally{db.execSQL("PRAGMA query_only=OFF");}
        need(in.errors.isEmpty(),label+" actual report SELECT errors "+in.errors);Object value=in.facts.get("importedRows");need(expected==null?value==null:value instanceof Number&&number(value)==expected,label+" actual logical published count: "+value);
        need(state.equals(in.facts.get("historyImportPublicationState")),label+" explicit availability: "+in.facts.get("historyImportPublicationState"));need(finalSelects-prior==1,label+" all facts use one final SELECT");need(scalar(db,"SELECT total_changes()")==changes&&scalar(db,"PRAGMA schema_version")==schema,label+" capture writes no data/schema");
        Map<String,Object> record=new LinkedHashMap<>();record.put("case",label);record.put("importedRows",value);record.put("publicationState",in.facts.get("historyImportPublicationState"));for(String key:new String[]{"historyImportLegacyRows","historyImportCompleteJobs","historyImportStagingJobs","historyImportUnpublishedRows"})record.put(key,in.facts.get(key));record.put("finalSelects",finalSelects-prior);record.put("metricResults",in.metricResults.size());outcomes.add(record);need(in.metricResults.size()==44,"existing44 metric definitions retained");return in;
    }
    public static void main(String[] args)throws Exception{
        File output=new File(args[1]);boolean baseline=args[0].equals("baseline");Class.forName("org.sqlite.JDBC");File database=File.createTempFile("r31-history-report-reader-", ".db");
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+database);Connection publisher=DriverManager.getConnection("jdbc:sqlite:"+database)){
            SQLiteDatabase db=new SQLiteDatabase(connection),second=new SQLiteDatabase(publisher);
            SQLiteDatabase.readObserver=(actual,sql)->{if(sql.startsWith("SELECT ")&&sql.contains(" AS v0")){finalSelects++;largeSelect=sql;if(beforeSelect!=null){Runnable action=beforeSelect;beforeSelect=null;action.run();}}};
            if(baseline){legacy(db);schema(db);job(db,"complete1");row(db,"complete1",1,"MATCH",0);row(db,"complete1",4,"CONFLICT",0);complete(db,"complete1",1);ReportR27.R30Input old=ReportSnapshotR30.capture(db);need(old.errors.isEmpty(),"actual old report executable");need(number(old.facts.get("importedRows"))==3&&scalar(db,"SELECT "+HistoryImportR27.publishedCountSqlR31())==4,"actual original report misses completed staged issue while actual production relation has4");System.out.println("BASELINE_REPORT_LOGICAL_PUBLICATION_MISSING_REPRODUCED legacy3_published4");return;}
            capture(db,"missing-legacy",null,"SOURCE_MISSING_LEGACY");legacy(db);ReportR27.R30Input old=capture(db,"not-migrated",null,"PUBLICATION_NOT_MIGRATED");need(number(old.facts.get("historyImportLegacyRows"))==3,"known old record total preserved separately");
            db.execSQL(HistoryImportR27.R31_PUBLICATION_DDL[0]);capture(db,"partial-schema",null,"UNKNOWN_PUBLICATION_COLUMNS_OR_TABLES");schema(db);capture(db,"legacy-current",3L,"READY_PUBLISHED");
            job(db,"complete1");row(db,"complete1",1,"MATCH",0);row(db,"complete1",4,"CONFLICT",0);ReportR27.R30Input staging=capture(db,"staging-invisible",3L,"READY_PUBLISHED_WITH_STAGING_EXCLUDED");need(number(staging.facts.get("historyImportUnpublishedRows"))==2,"actual uncompleted rows stay separate");
            complete(db,"complete1",1);capture(db,"complete-overlay",4L,"READY_PUBLISHED");
            job(db,"complete2");row(db,"complete2",1,"IMPORTED",1);row(db,"complete2",5,"CONFLICT",0);capture(db,"second-staging-invisible",4L,"READY_PUBLISHED_WITH_STAGING_EXCLUDED");complete(db,"complete2",2);capture(db,"complete-self-conflict-excluded",5L,"READY_PUBLISHED");
            db.execSQL("DROP TRIGGER r31_history_import_row_keep");capture(db,"missing-owned-trigger",null,"UNKNOWN_PUBLICATION_SCHEMA_CONTRACT");schema(db);capture(db,"restored-exact-ten",5L,"READY_PUBLISHED");
            job(db,"publish-during-capture");row(db,"publish-during-capture",6,"MATCH",0);beforeSelect=()->complete(second,"publish-during-capture",3);ReportR27.R30Input coherent=capture(db,"publication-before-final-select",6L,"READY_PUBLISHED");need(number(coherent.facts.get("historyImportCompleteJobs"))==3&&number(coherent.facts.get("historyImportUnpublishedRows"))==0,"scalar and job/row facts share actual same read view");need(largeSelect.contains(HistoryImportR27.publishedCountSqlR31()),"actual final SQL includes production scalar verbatim");
            try(Connection empty=DriverManager.getConnection("jdbc:sqlite::memory:")){SQLiteDatabase zero=new SQLiteDatabase(empty);zero.execSQL(HistoryImportR27.DDL);schema(zero);capture(zero,"ready-empty",0L,"READY_NO_PUBLISHED_ROWS");job(zero,"only-stage");row(zero,"only-stage",7,"IMPORTED",0);capture(zero,"ready-only-staging",0L,"READY_ONLY_STAGING_NO_PUBLISHED_ROWS");}
            Map<String,Object> result=new LinkedHashMap<>();result.put("scope","ACTUAL_REPORT_AND_HISTORY_HELPER_NATIVE_HOST_NOT_IMPORTER_NOT_ANDROID_NOT_QUALITY");result.put("assertions",assertions);result.put("outcomes",outcomes);result.put("sameSelectConcurrentPublication",true);result.put("status","TARGETED_READER_ASSERTIONS_COMPLETED");Files.write(new File(output,"reader-result.json").toPath(),(JsonR27.write(result)+"\n").getBytes(StandardCharsets.UTF_8));Files.write(new File(output,"actual-final-facts-select.sql").toPath(),(largeSelect+";\n").getBytes(StandardCharsets.UTF_8));System.out.println(JsonR27.write(result));
        }finally{SQLiteDatabase.readObserver=null;for(String suffix:new String[]{"","-wal","-shm","-journal"})Files.deleteIfExists(new File(database.getPath()+suffix).toPath());}
    }
}
