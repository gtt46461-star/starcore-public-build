package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.DbStore;
import java.io.File;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Actual writer -> ledger -> flush -> report, with native SQLite and declared HOST boundaries. */
public final class R31WriterOutcomeChainHost {
    static int assertions;
    static TraceDb db;static DatabaseCoordinator coordinator;static DbStore store;static ReentrantLock writer;
    static final List<Map<String,Object>> sql=new ArrayList<>(),cases=new ArrayList<>();
    static void need(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    static final class TraceDb extends SQLiteDatabase {
        boolean record,failAfterRealCommit;int ddl;int transactionEnds;
        TraceDb(Connection nativeConnection){super(nativeConnection);}
        @Override public void execSQL(String statement,Object[] args){
            if(record){Map<String,Object> call=new LinkedHashMap<>();call.put("sql",statement);call.put("inTransaction",inTransaction());sql.add(call);if(statement.trim().matches("(?is)^(CREATE|ALTER|DROP|VACUUM|REINDEX)\\b.*"))ddl++;}
            super.execSQL(statement,args);
        }
        @Override public void endTransaction(){super.endTransaction();transactionEnds++;if(failAfterRealCommit){failAfterRealCommit=false;throw new SQLiteException("HOST_END_ACK_FAILURE_AFTER_REAL_NATIVE_COMMIT");}}
    }
    static void set(Object object,String name,Object value)throws Exception{Field field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);}
    static long scalar(String statement){try(Cursor cursor=db.rawQuery(statement,null)){need(cursor.moveToFirst(),statement);return cursor.getLong(0);}}
    static Map<String,Object> stat(String operation){for(WriterHoldLedgerR30.Stat stat:WriterHoldLedgerR30.snapshot())if(stat.operation.equals(operation))return WriterHoldLedgerR30.toMap(stat);throw new AssertionError("MISSING_ACTUAL_WRITER_STAT:"+operation);}
    static void counts(Map<?,?> row,long count,long failures,Long rolled,long known,long unknown){
        need(((Number)row.get("count")).longValue()==count,"ATTEMPT_COUNT:"+row);
        need(((Number)row.get("failures")).longValue()==failures,"ATTEMPT_FAILURE_COUNT:"+row);
        need(rolled==null?row.get("rolledBack")==null:row.get("rolledBack") instanceof Number&&((Number)row.get("rolledBack")).longValue()==rolled,"EXACT_ROLLBACK_COUNT_OR_UNKNOWN:"+row);
        need(((Number)row.get("rolledBackKnown")).longValue()==known,"KNOWN_ROLLBACK_COUNT:"+row);
        need(((Number)row.get("commitOutcomeUnknown")).longValue()==unknown,"UNKNOWN_OUTCOME_COUNT:"+row);
    }
    static DatabaseCoordinator.CommitResultR31<Integer> increment(String operation,String key,boolean failSql,boolean slow){
        DatabaseCoordinator.CommitIdentityR31 id=new DatabaseCoordinator.CommitIdentityR31(key,StatsR27.sha256("CONTROLLED_PAYLOAD:"+key),"writer-outcome-fixture","host-owner",0,"CONTROLLED_INPUT",0,"R31_OUTCOME_CHAIN_HOST_V1");
        return coordinator.runIdempotentAtomicWriteR31(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,operation),id,actual->{
            need(actual==db&&db.inTransaction()&&writer.isHeldByCurrentThread(),"REAL_COORDINATOR_TRANSACTION_AND_LOCK");
            actual.execSQL("UPDATE outcome_probe SET value=value+1 WHERE id=1");
            if(slow)Thread.sleep(70); // Controlled real hold duration; never a device performance claim.
            if(failSql)actual.execSQL("INSERT INTO outcome_probe VALUES(1,999)"); // Actual native UNIQUE failure after mutation.
            return 1;
        },1);
    }
    @SuppressWarnings("unchecked") static void diagnostic(boolean enabled)throws Exception{
        Field field=DatabaseCoordinator.class.getDeclaredField("diagnosticFaultR31");field.setAccessible(true);ThreadLocal<Object> hooks=(ThreadLocal<Object>)field.get(null);
        if(!enabled){hooks.remove();return;}
        Class<?> api=Class.forName("com.ethk3.novaonepro.DatabaseCoordinator$DiagnosticFaultR31");
        hooks.set(Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(proxy,method,args)->{if(args!=null&&args.length==1&&"AFTER_COMMIT".equals(args[0]))throw new IllegalStateException("HOST_ACTUAL_AFTER_COMMIT_DIAGNOSTIC_FAILURE");return null;}));
    }
    static ReportR27.R30Input capture(){long changes=scalar("SELECT total_changes()"),schema=scalar("PRAGMA schema_version");db.execSQL("PRAGMA query_only=ON");ReportR27.R30Input report;try{report=ReportSnapshotR30.capture(db);}finally{db.execSQL("PRAGMA query_only=OFF");}need(report.errors.isEmpty(),"REPORT_CAPTURE:"+report.errors);need(changes==scalar("SELECT total_changes()")&&schema==scalar("PRAGMA schema_version"),"REPORT_IS_READ_ONLY");return report;}
    @SuppressWarnings("unchecked") static Map<String,Object> reportRow(ReportR27.R30Input report,String operation){for(Object row:(List<?>)report.data.get("writerWindows"))if(operation.equals(((Map<?,?>)row).get("operation")))return(Map<String,Object>)row;throw new AssertionError("PERSISTED_REPORT_ROW_MISSING:"+operation);}
    static void archivedFixture(String operation,String histogram){long now=System.currentTimeMillis();db.execSQL(HoldLedgerFlushR30.INSERT,new Object[]{now-10000,now,operation,0,7,1,2,3,60,1,2,3,0,6,histogram,"OLD_ENGINEERING_FIXTURE"});}
    static Map<String,Object> classOrigins(String ahead,String support)throws Exception{
        Map<String,Object> origins=new LinkedHashMap<>();
        Set<String> overlay=new HashSet<>(Arrays.asList("DatabaseCoordinator","strategyv2.FailureDispositionR31","strategyv2.WriterHoldLedgerR30","strategyv2.ReportSnapshotR30","strategyv2.ReportR27"));
        for(String name:new String[]{"DatabaseCoordinator","DbStore","DbHelper","strategyv2.FailureDispositionR31","strategyv2.WriterHoldLedgerR30","strategyv2.HoldLedgerFlushR30","strategyv2.ReportSnapshotR30","strategyv2.ReportR27"}){
            Class<?> type=Class.forName("com.ethk3.novaonepro."+name,false,R31WriterOutcomeChainHost.class.getClassLoader());
            File actual=new File(type.getProtectionDomain().getCodeSource().getLocation().toURI()).getCanonicalFile();File expected=new File(overlay.contains(name)?ahead:support).getCanonicalFile();need(actual.equals(expected),"ACTUAL_BUSINESS_CLASS_ORIGIN:"+name+":"+actual);origins.put(name,actual.toString());
        }return origins;
    }
    public static void main(String[] args)throws Exception{
        Map<String,Object> origins=classOrigins(args[1],args[2]);Class.forName("org.sqlite.JDBC");File file=new File(args[0]);file.getParentFile().mkdirs();
        try(Connection nativeConnection=DriverManager.getConnection("jdbc:sqlite:"+file)){
            db=new TraceDb(nativeConnection);ReportR27.R30Input missing=capture();need(missing.facts.get("writerWindows")==null,"MISSING_WRITER_SOURCE_MUST_NOT_BE_KNOWN_ZERO");
            cases.add(new LinkedHashMap<String,Object>(){{put("case","missing-table");put("writerWindowCount",missing.facts.get("writerWindows"));put("status","UNKNOWN_SOURCE");}});
            for(String ddl:SchemaR27.R31_WRITE_DDL)db.execSQL(ddl);db.execSQL(HoldLedgerFlushR30.DDL);db.execSQL("CREATE TABLE outcome_probe(id INTEGER PRIMARY KEY,value INTEGER NOT NULL)");db.execSQL("INSERT INTO outcome_probe VALUES(1,0)");
            Field unsafeField=Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);Unsafe unsafe=(Unsafe)unsafeField.get(null);coordinator=(DatabaseCoordinator)unsafe.allocateInstance(DatabaseCoordinator.class);
            Field lock=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lock.setAccessible(true);writer=(ReentrantLock)lock.get(null);
            android.app.Application app=new android.app.Application(file);android.database.sqlite.SQLiteOpenHelper.bindExistingForHostR31(file,db);DbHelper helper=new DbHelper(app);
            set(coordinator,"writeLock",writer);set(coordinator,"helper",helper);set(coordinator,"app",app);set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
            store=(DbStore)unsafe.allocateInstance(DbStore.class);set(store,"coordinator",coordinator);set(store,"researchAppCP12",app);
            WriterHoldLedgerR30.resetForTest();db.record=true;
            DatabaseCoordinator.CommitResultR31<Integer> committed=increment("R31_CHAIN_COMMIT","commit",false,true);need(committed.outcome==DatabaseCoordinator.CommitOutcome.COMMITTED,"REAL_COMMIT_OUTCOME");need(scalar("SELECT value FROM outcome_probe")==1,"REAL_COMMIT_MUTATION");counts(stat("R31_CHAIN_COMMIT"),1,0,0L,0,0);
            try{increment("R31_CHAIN_ROLLBACK","rollback",true,false);throw new AssertionError("NATIVE_CONSTRAINT_MUST_REJECT");}catch(DatabaseCoordinator.WriteRejectedR31 expected){need(expected.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK&&"INTEGRITY".equals(expected.category),"ACTUAL_SQL_ROLLBACK_CLASSIFIED");}
            need(scalar("SELECT value FROM outcome_probe")==1&&scalar("SELECT count(*) FROM r31_commit_receipt WHERE commit_key='rollback'")==0,"SQL_ROLLBACK_REVERTS_MUTATION_AND_RECEIPT");counts(stat("R31_CHAIN_ROLLBACK"),1,1,1L,1,0);
            long diagBefore=((Number)DatabaseCoordinator.writeProtocolSnapshotR31().get("diagnosticFailures")).longValue();diagnostic(true);DatabaseCoordinator.CommitResultR31<Integer> diagnosticResult;try{diagnosticResult=increment("R31_CHAIN_DIAGNOSTIC","diagnostic",false,false);}finally{diagnostic(false);}
            need(diagnosticResult.outcome==DatabaseCoordinator.CommitOutcome.COMMITTED_DIAGNOSTIC_FAILED&&diagnosticResult.diagnosticFailure!=null,"ACTUAL_DIAGNOSTIC_IS_COMMITTED");need(((Number)DatabaseCoordinator.writeProtocolSnapshotR31().get("diagnosticFailures")).longValue()>diagBefore,"DIAGNOSTIC_RECORDED_RED");need(scalar("SELECT value FROM outcome_probe")==2,"DIAGNOSTIC_FAILURE_DOES_NOT_REDO_BUSINESS");counts(stat("R31_CHAIN_DIAGNOSTIC"),1,0,0L,0,0);
            db.failAfterRealCommit=true;System.setProperty("r31.host.receipt.readback.unavailable","true");try{increment("R31_CHAIN_UNKNOWN","unknown",false,false);throw new AssertionError("END_ACK_WITHOUT_READBACK_MUST_STAY_UNKNOWN");}catch(DatabaseCoordinator.WriteRejectedR31 expected){need(expected.commitOutcome==DatabaseCoordinator.CommitOutcome.UNKNOWN,"ACTUAL_COORDINATOR_UNKNOWN_OUTCOME");}finally{System.clearProperty("r31.host.receipt.readback.unavailable");}
            need(scalar("SELECT value FROM outcome_probe")==3,"UNKNOWN_ACK_ACTUALLY_COMMITTED");counts(stat("R31_CHAIN_UNKNOWN"),1,1,null,0,1);
            WriterHoldLedgerR30.record("R31_CHAIN_LEGACY_FIVE_ARG",false,1,1,true);counts(stat("R31_CHAIN_LEGACY_FIVE_ARG"),1,1,null,0,1);
            Map<String,Map<String,Object>> original=new LinkedHashMap<>();for(String operation:new String[]{"R31_CHAIN_COMMIT","R31_CHAIN_ROLLBACK","R31_CHAIN_DIAGNOSTIC","R31_CHAIN_UNKNOWN","R31_CHAIN_LEGACY_FIVE_ARG"})original.put(operation,stat(operation));
            need(((Number)original.get("R31_CHAIN_COMMIT").get("overBudget50ms")).longValue()==1,"REAL_CONTROLLED_HOLD_OVER50MS_RECORDED");
            HoldLedgerFlushR30.maybeFlush(store);need(((Number)HoldLedgerFlushR30.flushStateR31().get("pendingRows")).longValue()==0,"ACTUAL_HOLD_FLUSH_COMPLETED");need(scalar("SELECT count(*) FROM r30_writer_hold")==5,"EXACT_FIVE_SOURCE_OPERATIONS_PERSISTED");
            archivedFixture("R31_OLD_HISTOGRAM","{\"version\":\"R30_HISTOGRAM_16_OPEN_UPPER_V1\"}");archivedFixture("R31_MISSING_HISTOGRAM",null);
            ReportR27.R30Input report=capture();
            for(Map.Entry<String,Map<String,Object>> entry:original.entrySet()){
                Map<String,Object> row=reportRow(report,entry.getKey());Map<String,Object> actual=entry.getValue();counts(row,((Number)actual.get("count")).longValue(),((Number)actual.get("failures")).longValue(),actual.get("rolledBack")==null?null:((Number)actual.get("rolledBack")).longValue(),((Number)actual.get("rolledBackKnown")).longValue(),((Number)actual.get("commitOutcomeUnknown")).longValue());
                need(((Number)row.get("over_budget")).longValue()==((Number)actual.get("overBudget50ms")).longValue(),"PERSISTED_OVER_BUDGET_EXPORTED");
                Map<?,?> histogram=(Map<?,?>)JsonR27.parse((String)row.get("histogram_json"));need(histogram.get("commitOutcomes") instanceof Map,"ACTUAL_FLUSH_PERSISTS_VERSIONED_OUTCOMES");cases.add(row);
            }
            for(String operation:new String[]{"R31_OLD_HISTOGRAM","R31_MISSING_HISTOGRAM"}){Map<String,Object> row=reportRow(report,operation);need(row.get("rolledBack")==null&&row.get("rolledBackKnown")==null&&"UNKNOWN_LEGACY_OR_MISSING_OUTCOME_SOURCE".equals(row.get("rollbackEvidenceState")),"LEGACY_FAILURES_NEVER_MASQUERADE_AS_ROLLBACKS");cases.add(row);}
            need(report.data.get("r31WriterProtocolObservation") instanceof Map,"REPORT_CONSUMES_PROTOCOL_DIAGNOSTICS");need(db.ddl==0,"BUSINESS_AND_REPORT_DDL_ZERO");need(!db.inTransaction()&&!writer.isLocked(),"REAL_TX_AND_WRITER_RELEASED");
            List<Map<String,Object>> chapters=ReportR27.chaptersR30(report);Map<?,?> writerChapter=chapters.get(2),writerData=(Map<?,?>)writerChapter.get("data");
            need(writerData.get("r31WriterProtocolObservation").equals(report.data.get("r31WriterProtocolObservation")),"WRITER_CHAPTER_CONSUMES_ACTUAL_DIAGNOSTICS");
            String rendered=ReportR27.markdownR30(chapters);need(rendered.contains("r31WriterProtocolObservation")&&rendered.contains("rolledBack"),"FINAL_REPORT_RENDERS_WRITER_OUTCOME_EVIDENCE");
            need("RECORDED_UNVALIDATED".equals(writerChapter.get("status"))&&"NOT_RUN".equals(writerChapter.get("physicalDevice")),"RECORDED_WRITER_EVIDENCE_DOES_NOT_CLAIM_BUSINESS_OR_DEVICE_PASS");
            Map<String,Object> output=new LinkedHashMap<>();output.put("status","ACTUAL_WRITER_LEDGER_FLUSH_REPORT_CHAIN_PASS");output.put("assertions",assertions);output.put("typedOutcomeProducer","ACTUAL_DATABASE_COORDINATOR_NO_HAND_SUPPLIED_TYPED_OUTCOMES");output.put("probeValue",scalar("SELECT value FROM outcome_probe"));output.put("businessDdl",db.ddl);output.put("cases",cases);output.put("businessClassOrigins",origins);output.put("sql",sql);output.put("scope","FIVE_FROZEN_PRODUCTION_SOURCES_AHEAD_C55_WHOLE_SUPPORT_NATIVE_SQLITE_HOST;NOT_WHOLE_NEW_BUILD_NOT_ANDROID_NOT_NATURAL_QUALITY");System.out.println(JsonR27.write(output));
        }
    }
}
