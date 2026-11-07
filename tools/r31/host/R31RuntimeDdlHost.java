package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.*;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.File;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Native SQLite and actual production callbacks. Controlled fixtures are never model/quality/device evidence. */
public final class R31RuntimeDdlHost {
    static int checks;static SQLiteDatabase db;static DbStore store;static Unsafe unsafe;
    static final List<String> businessStages=new ArrayList<>();
    static void need(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
    static void stage(String stage){SQLiteDatabase.stage=stage;if(stage.startsWith("BUSINESS_")&&!businessStages.contains(stage))businessStages.add(stage);}
    static long count(String sql){String previous=SQLiteDatabase.stage;SQLiteDatabase.stage="ASSERTION_READ";try(Cursor rows=db.rawQuery(sql,null)){need(rows.moveToFirst(),sql);return rows.getLong(0);}finally{SQLiteDatabase.stage=previous;}}
    /** Every row of a table, all columns quoted, in rowid order: a byte-exact row-by-row snapshot. */
    static String rowsR31(String table){
        List<String> columns=new ArrayList<>();String previous=SQLiteDatabase.stage;SQLiteDatabase.stage="ASSERTION_READ";
        try(Cursor info=db.rawQuery("PRAGMA table_info("+table+")",null)){while(info.moveToNext())columns.add(info.getString(1));}finally{SQLiteDatabase.stage=previous;}
        need(!columns.isEmpty(),"table exists for snapshot: "+table);StringBuilder row=new StringBuilder();
        for(String column:columns){if(row.length()>0)row.append("||'|'||");row.append("quote(\"").append(column).append("\")");}
        return text("SELECT coalesce(group_concat(r,char(10)),'') FROM (SELECT "+row+" AS r FROM "+table+" ORDER BY rowid)");
    }
    static String text(String sql){String previous=SQLiteDatabase.stage;SQLiteDatabase.stage="ASSERTION_READ";try(Cursor rows=db.rawQuery(sql,null)){need(rows.moveToFirst(),sql);return rows.getString(0);}finally{SQLiteDatabase.stage=previous;}}
    static void field(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    static Object field(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    interface Action{void run()throws Exception;}
    static void tx(Action body)throws Exception{db.beginTransaction();try{body.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static void dependency(Throwable failure,String why){need(FailureDispositionR31.classify(failure).disposition==FailureDispositionR31.Disposition.DEPENDENCY,why);need(!FailureDispositionR31.classify(failure).consumesFaultBudget(),why+" no fault budget");}
    static void revocation()throws Exception{
        stage("BUSINESS_CERT_NO_AUTHORITY");tx(()->CertificateRevocationR30.revokeWithin(db,"202611040001","HOST_CONFLICT","fixture"));need(count("SELECT count(*) FROM sqlite_master WHERE name LIKE 'r30_revok%' OR name='r30_certificate_revocation_event'")==0,"no authority business branch creates no objects");
        stage("BOOTSTRAP_CERT_AUTHORITY_ONLY");for(String sql:SchemaR27.DDL)if(sql.startsWith("CREATE TABLE"))db.execSQL(sql);
        db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INT,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT,draw_hash_v2 TEXT)");db.execSQL("INSERT INTO draws VALUES('202611040001',0,1,2,3,4,5,6,7,8,9,10,'HOST_DRAW')");
        db.execSQL("INSERT INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at,gate_version) VALUES('HOST_POLICY','ACTIVE','HOST_GATES','HOST_EVIDENCE',1,9223372036854775807,'HOST_GATE')");
        stage("BUSINESS_CERT_SCHEMA_MISSING");boolean rejected=false;db.beginTransaction();try{db.execSQL("UPDATE draws SET dirty=1 WHERE issue='202611040001'");try{CertificateRevocationR30.revokeWithin(db,"202611040001","HOST_CONFLICT","fixture");}catch(DatabaseCoordinator.SchemaPendingR31 pending){dependency(pending,"existing authority requires revocation migration");rejected=true;}}finally{db.endTransaction();}
        need(rejected,"missing revocation schema cannot bypass authority protection");need(text("SELECT status FROM r27_certificate").equals("ACTIVE")&&count("SELECT dirty FROM draws")==0,"failed source transaction rolls back without partial authority mutation");need(count("SELECT count(*) FROM sqlite_master WHERE name='r30_certificate_revocation_event'")==0,"missing schema was not created by revoke business call");
        stage("BOOTSTRAP_CERT_MIGRATION");tx(()->CertificateRevocationR30.ensure(db));
        stage("BUSINESS_CERT_REVOKE_READY");tx(()->CertificateRevocationR30.revokeWithin(db,"202611040001","HOST_CANONICAL_CONFLICT","quoted'fixture"));need(text("SELECT status FROM r27_certificate").equals("REVOKED"),"ready schema performs actual authority revocation");need(count("SELECT count(*) FROM r30_revoked_certificate WHERE evidence_digest='HOST_EVIDENCE'")==1,"original certificate retained in immutable archive");
        stage("BOOTSTRAP_CERT_DRIFT_INJECTION");db.execSQL("DROP TRIGGER r30_revoke_r27_reissue_insert");
        stage("BUSINESS_CERT_GUARD_DRIFT");boolean triggerMissing=false;try{tx(()->CertificateRevocationR30.revokeWithin(db,"202611040001","HOST_DRIFT","fixture"));}catch(DatabaseCoordinator.SchemaPendingR31 pending){dependency(pending,"missing anti-reissue trigger is dependency");triggerMissing=true;}need(triggerMissing,"table names alone cannot make authority schema ready");need(count("SELECT count(*) FROM sqlite_master WHERE name='r30_revoke_r27_reissue_insert'")==0,"business does not recreate missing trigger");
        String[] revocationTables={"r27_certificate","r26_certificate_revocation","r30_revoked_certificate","r30_revoked_policy","r30_certificate_revocation_event"};Map<String,String> beforeRepair=new LinkedHashMap<>();
        for(String table:revocationTables)beforeRepair.put(table,rowsR31(table));
        need(count("SELECT count(*) FROM r30_revoked_certificate")>=1,"existing revocation records present before the upgrade");
        stage("BOOTSTRAP_CERT_REPAIR");tx(()->CertificateRevocationR30.ensure(db));
        for(String table:revocationTables)need(beforeRepair.get(table).equals(rowsR31(table)),"R31-ENG-01#A2: existing revocation rows unchanged row by row across the upgrade: "+table);
        tx(()->CertificateRevocationR30.ensure(db));
        for(String table:revocationTables)need(beforeRepair.get(table).equals(rowsR31(table)),"repeated upgrade keeps revocation rows unchanged: "+table);
        System.out.println("R31_RUNTIME_CERT_DDL_HOST_PASS noAuthorityNoCreate=true missingAuthoritySchemaDependency=true actualRevokeAndArchive=true missingTriggerDependency=true");
    }
    static void transactionWrapper()throws Exception{
        stage("BUSINESS_TX_SCHEMA_MISSING");AtomicInteger entered=new AtomicInteger();DatabaseCoordinator.TransactionContext missing=new DatabaseCoordinator.TransactionContext("HOST_TX","202611040002","host-missing-audit");DatabaseCoordinator.TransactionResult<Integer> failed=DatabaseCoordinator.runExistingTransaction(db,missing,(database,context)->{entered.incrementAndGet();return 1;},0);
        need(!failed.success&&failed.failureCause!=null,"missing transaction audit has retained failure cause");dependency(failed.failureCause,"missing transaction audit is dependency");need(entered.get()==0,"missing audit schema never enters business body");need(count("SELECT count(*) FROM sqlite_master WHERE name='b03_transaction_audit'")==0,"wrapper never creates its audit schema");
        stage("BOOTSTRAP_V51_MANUAL_MIGRATION");V51Repository.ensure(db);db.execSQL("CREATE TABLE host_model(samples INTEGER)");db.execSQL("INSERT INTO host_model VALUES(0)");
        stage("BUSINESS_TX_READY");DatabaseCoordinator.TransactionContext normal=new DatabaseCoordinator.TransactionContext("HOST_TX","202611040002","host-ready-audit");DatabaseCoordinator.TransactionCallable<Integer> body=(database,context)->{database.execSQL("UPDATE host_model SET samples=samples+1");entered.incrementAndGet();return 8;};DatabaseCoordinator.TransactionResult<Integer> committed=DatabaseCoordinator.runExistingTransaction(db,normal,body,0);
        need(committed.success&&!committed.replay&&committed.value==8,"wrapper commits original business result");DatabaseCoordinator.TransactionResult<Integer> replay=DatabaseCoordinator.runExistingTransaction(db,normal,body,0);need(replay.success&&replay.replay&&entered.get()==1&&count("SELECT samples FROM host_model")==1,"wrapper replay performs no duplicate body");
        need(count("SELECT count(*) FROM sqlite_master WHERE name='v57_shadow_observation_r35'")==0,"legacy wrapper does not create unrelated R35 tables");
    }
    static V51RuntimeResult controlledResult(String target)throws Exception{
        V51Models.PredictionContext context=new V51Models.PredictionContext("202611040001",target,"202611040001","HOST_HISTORY","HOST_COHORT","HOST_ARTIFACT",1,1);
        V51Models.PermutationFeatureSnapshot features=new V51Models.PermutationFeatureSnapshot(context,new double[0],new double[0][][],"HOST_FEATURES");
        V51PermutationNumberModelEngine.Result numbers=(V51PermutationNumberModelEngine.Result)unsafe.allocateInstance(V51PermutationNumberModelEngine.Result.class);field(numbers,"experts",new V51Models.ExpertEvidence[0]);
        return new V51RuntimeResult(true,context,features,new V51Models.StateTrajectorySnapshot[0],numbers,new V51EffectiveConsensusEngine.Consensus[0],null,new V51Models.PositionAction[0],null,null,null,"CONTROLLED_HOST_SCHEMA_FIXTURE_NOT_MODEL_EVIDENCE",0,"HOST_BUNDLE");
    }
    static void v51()throws Exception{
        stage("BOOTSTRAP_V51_EXISTING_FROZEN_BUNDLE");V51Repository.requireBusinessSchemaR31(db,"HOST_SCHEMA_ASSERT");String target="202611040002";
        db.execSQL("INSERT INTO b03_prediction_bundle(bundle_id,game_code,target_issue,base_issue,history_hash,feature_hash,state_hash,expert_hash,bundle_hash,contract_hash,artifact_bundle_hash,cohort_id,status,record_hash,created_at) VALUES('HOST_FIRST','Binance1-PK10',?,'202611040001','HOST','HOST','HOST','HOST','HOST_FIRST_BUNDLE','HOST','HOST','HOST','HOST_FROZEN','HOST_FIRST_RECORD',1)",new Object[]{target});
        V51RuntimeResult fixture=controlledResult(target);stage("BUSINESS_V51_PERSIST_CALLBACK");V51Repository.PersistReport preserved=V51Repository.persistWithReport(store,fixture);need(preserved.success&&preserved.detail.contains("IMMUTABLE_FIRST_WRITE_PRESERVED"),"actual prediction callback preserves first frozen bundle on conflicting retry");need(text("SELECT bundle_hash FROM b03_prediction_bundle").equals("HOST_FIRST_BUNDLE"),"existing pre-draw bundle not replaced");
        stage("BUSINESS_V51_PERSIST_REPLAY");need(V51Repository.persistWithReport(store,fixture).replay,"actual prediction callback replays durable audit");
        stage("BUSINESS_V51_SETTLEMENT_CALLBACK");V51Repository.SettlementReport result=V51Repository.settleShadow(store,target,"1,2,3,4,5,6,7,8,9,10","HOST_FIXTURE_RESULT",2);need(result.success&&!result.replay,"actual settlement callback commits");need(count("SELECT count(*) FROM b03_settlement_event")==1,"one actual settlement receipt");
        stage("BUSINESS_V51_SETTLEMENT_REPLAY");need(V51Repository.settleShadow(store,target,"1,2,3,4,5,6,7,8,9,10","HOST_FIXTURE_RESULT",2).replay&&count("SELECT count(*) FROM b03_settlement_event")==1,"duplicate settlement has one receipt");
        stage("BOOTSTRAP_V51_MISSING_TABLE_INJECTION");db.execSQL("DROP TABLE v51_performance_span");stage("BUSINESS_V51_MISSING_SCHEMA");boolean missing=false;try{V51Repository.settleShadow(store,"202611040003","1,2,3,4,5,6,7,8,9,10","HOST",3);}catch(DatabaseCoordinator.SchemaPendingR31 pending){dependency(pending,"V51 callback propagates schema dependency to persistent job owner");missing=true;}need(missing&&count("SELECT count(*) FROM sqlite_master WHERE name='v51_performance_span'")==0,"missing V51 schema not repaired by business callback");need(count("SELECT count(*) FROM b03_settlement_event")==1,"missing-schema callback never fabricates a settlement");
        stage("BOOTSTRAP_V51_ADDITIVE_REPAIR");V51Repository.ensure(db);V51Repository.requireBusinessSchemaR31(db,"HOST_SCHEMA_REPAIRED");need(count("SELECT count(*) FROM b03_settlement_event")==1&&text("SELECT bundle_hash FROM b03_prediction_bundle").equals("HOST_FIRST_BUNDLE"),"explicit additive migration preserves existing frozen and settlement data");
        System.out.println("R31_RUNTIME_V51_DDL_HOST_PASS actualPersistCallback=true firstBundlePreserved=true actualSettlementAndReplay=true missingSchemaDependencyNoCreate=true additiveRepairPreservesData=true");
    }
    static void jobAndHealth()throws Exception{
        stage("BOOTSTRAP_R35_SCHEMA");DbHelper.ensureR35Schema(db);db.execSQL("ALTER TABLE v57_deferred_evidence_job ADD COLUMN snapshot_blob BLOB");db.execSQL("ALTER TABLE v57_deferred_evidence_job ADD COLUMN snapshot_hash TEXT");db.execSQL("ALTER TABLE v57_deferred_evidence_job ADD COLUMN lease_owner TEXT");db.execSQL("INSERT INTO v57_deferred_evidence_job(target_issue,job_type,status,attempts,bundle_hash,created_at,updated_at,record_hash,snapshot_blob,snapshot_hash,lease_owner) VALUES('202611040004','V51_FULL_EVIDENCE','PENDING',2,'HOST_JOB_BUNDLE',1,1,'HOST_JOB',X'01020304','HOST_SNAPSHOT_HASH','HOST_R36_OWNER')");
        Method update=DeferredEvidenceWriterR35.class.getDeclaredMethod("upsertJob",DbStore.class,String.class,String.class,String.class,String.class,boolean.class);update.setAccessible(true);
        stage("BUSINESS_R35_JOB_RETRY");update.invoke(null,store,"202611040004","HOST_JOB_BUNDLE","RETRY_REQUIRED_WRITE_ERROR","SCHEMA_PENDING:HOST",true);need(text("SELECT status FROM v57_deferred_evidence_job").equals("RETRY_REQUIRED_WRITE_ERROR"),"actual durable retry state retained");need(text("SELECT hex(snapshot_blob)||'|'||snapshot_hash||'|'||lease_owner FROM v57_deferred_evidence_job").equals("01020304|HOST_SNAPSHOT_HASH|HOST_R36_OWNER"),"R35 status UPDATE cannot erase R36 snapshot/hash/lease columns");need(count("SELECT attempts FROM v57_deferred_evidence_job")==3,"attempt increment persisted exactly once");need(DeferredEvidenceWriterR35.pendingCount(store)==1,"pending read reports actual durable job");
        need(text("SELECT record_hash||'|'||job_type FROM v57_deferred_evidence_job").equals("HOST_JOB|V51_FULL_EVIDENCE"),"status update preserves the original persistent identity and protocol");
        stage("BUSINESS_R35_JOB_STALE_BUNDLE");boolean stale=false;try{update.invoke(null,store,"202611040004","HOST_DIFFERENT_BUNDLE","COMPLETED","",false);}catch(InvocationTargetException failed){stale=FailureDispositionR31.classify(failed.getCause()).disposition==FailureDispositionR31.Disposition.STALE_OWNER;}need(stale&&text("SELECT status FROM v57_deferred_evidence_job").equals("RETRY_REQUIRED_WRITE_ERROR"),"old bundle completion cannot overwrite current durable job");
        stage("BOOTSTRAP_R35_INDEX_DRIFT");db.execSQL("DROP INDEX idx_r35_deferred_status");stage("BUSINESS_R35_JOB_SCHEMA_MISSING");boolean missing=false;try{update.invoke(null,store,"202611040004","HOST_JOB_BUNDLE","COMPLETED","",false);}catch(InvocationTargetException failed){dependency(failed.getCause(),"job writer missing schema propagates dependency");missing=true;}need(missing&&count("SELECT count(*) FROM sqlite_master WHERE name='idx_r35_deferred_status'")==0,"job update never creates missing schema");need(text("SELECT status FROM v57_deferred_evidence_job").equals("RETRY_REQUIRED_WRITE_ERROR"),"failed schema admission preserves the pending job");boolean pendingUnknown=false;try{DeferredEvidenceWriterR35.pendingCount(store);}catch(DatabaseCoordinator.SchemaPendingR31 expected){pendingUnknown=true;}need(pendingUnknown,"missing schema cannot become pending=0");
        stage("BOOTSTRAP_R35_INDEX_REPAIR");DbHelper.ensureR35Schema(db);need(DeferredEvidenceWriterR35.pendingCount(store)==1,"explicit migration restores pending read without losing retry");
        stage("BUSINESS_R35_HEALTH_RECORD_AND_SNAPSHOT");RuntimeHealthLedgerR35.record(store,"HOST_STAGE","202611040004","HOST_ONLY",1,1,1,0,"CONTROLLED_SCHEMA_TEST");need(RuntimeHealthLedgerR35.snapshot(store).lastStatus.equals("HOST_ONLY"),"health record/read consumes migrated schema without ensure");
        stage("BOOTSTRAP_R35_HEALTH_MISSING");db.execSQL("DROP INDEX idx_r35_health_stage");stage("BUSINESS_R35_HEALTH_SCHEMA_MISSING");need(RuntimeHealthLedgerR35.snapshot(store).lastStatus.equals("SCHEMA_PENDING"),"missing diagnostic schema is explicit not repaired");need(count("SELECT count(*) FROM sqlite_master WHERE name='idx_r35_health_stage'")==0,"health getter cannot create index");
        stage("BOOTSTRAP_R36_PROTOCOL_FIXTURE");db.execSQL("INSERT INTO v57_deferred_evidence_job(target_issue,job_type,status,attempts,bundle_hash,created_at,updated_at,record_hash,snapshot_blob,snapshot_hash,lease_owner) VALUES('202611040005','V51_FULL_EVIDENCE_R36','PENDING',0,'HOST_R36_BUNDLE',1,1,'HOST_R36_RECORD',X'05060708','HOST_R36_HASH','HOST_R36_OWNER')");stage("BUSINESS_R35_REJECT_R36_PROTOCOL_OWNER");boolean r36Rejected=false;try{update.invoke(null,store,"202611040005","HOST_R36_BUNDLE","RETRY_REQUIRED_WRITE_ERROR","",true);}catch(InvocationTargetException failed){r36Rejected=FailureDispositionR31.classify(failed.getCause()).disposition==FailureDispositionR31.Disposition.STALE_OWNER;}need(r36Rejected,"old R35 cannot publish an unconsumable R35 status into the R36 protocol");need(text("SELECT status||'|'||record_hash||'|'||job_type||'|'||hex(snapshot_blob) FROM v57_deferred_evidence_job WHERE target_issue='202611040005'").equals("PENDING|HOST_R36_RECORD|V51_FULL_EVIDENCE_R36|05060708"),"R36 pending status identity and actual snapshot remain consumable by its real owner");
        need(DatabaseCoordinator.OPERATION_REGISTRY_R31.get("R31_FETCH_BEGIN_ASYNC")==DatabaseCoordinator.WriteClass.TELEMETRY,"async fetch attempt remains explicit background telemetry");
        need(DatabaseCoordinator.OPERATION_REGISTRY_R31.get("R31_LEGACY_SCHEMA_PREPARE")==DatabaseCoordinator.WriteClass.MIGRATION,"legacy preparation stays on the existing explicit migration owner");
        stage("CONTROL_LEGACY_READINESS_INVALIDATION_NO_SQL");DatabaseCoordinator coordinator=store.coordinatorR36();int sqlBefore=SQLiteDatabase.trace.size();coordinator.invalidateLegacyRuntimeSchemaR31("HOST_DRIFT".repeat(100));need(SQLiteDatabase.trace.size()==sqlBefore,"legacy readiness invalidation executes no SQL");need(!((AtomicBoolean)field(coordinator,"runtimeSchemaReady")).get()&&((AtomicBoolean)field(coordinator,"criticalSchemaReady")).get(),"legacy invalidation does not demote Critical readiness");need(field(coordinator,"legacySchemaStage").equals("SCHEMA_PENDING")&&field(coordinator,"legacySchemaError").toString().length()<=239,"legacy pending reason is explicit and bounded");
        System.out.println("R31_RUNTIME_JOB_DDL_HOST_PASS durableRetry=true immutableR36ColumnsRetained=true staleBundleRejected=true missingSchemaNotZero=true healthPureReadiness=true");
    }
    static void trace(Path output)throws Exception{
        int total=0;for(String stage:businessStages){int ddl=0,calls=0;for(String line:SQLiteDatabase.trace)if(line.startsWith(stage+"\t")){calls++;String sql=line.substring(line.indexOf('\t',stage.length()+1)+1).stripLeading();if(sql.matches("(?is)(CREATE|ALTER|DROP|VACUUM|REINDEX)\\b.*"))ddl++;}need(calls>0&&ddl==0,"business stage has real SQL calls and zero DDL: "+stage+" calls="+calls+" ddl="+ddl);total+=calls;System.out.println("R31_BUSINESS_SQL_CALL_TRACE stage="+stage+" calls="+calls+" ddl="+ddl);}
        Files.write(output,SQLiteDatabase.trace);System.out.println("R31_BUSINESS_SQL_CALL_TRACE_TOTAL stages="+businessStages.size()+" actualBusinessCalls="+total+" ddl=0 scope=JAVA_SQLITE_API_CALLS_ON_NATIVE_JDBC_NOT_ANDROID_TRACE");
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Field unsafeField=Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);unsafe=(Unsafe)unsafeField.get(null);
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+args[0])){
            db=new SQLiteDatabase(connection);SQLiteOpenHelper.hostDatabase=db;
            DatabaseCoordinator coordinator=(DatabaseCoordinator)unsafe.allocateInstance(DatabaseCoordinator.class);Field global=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");global.setAccessible(true);field(coordinator,"writeLock",(ReentrantLock)global.get(null));field(coordinator,"helper",new DbHelper(new android.content.Context()));field(coordinator,"app",new android.app.Application(new File(args[0])));field(coordinator,"waitingWriters",new AtomicInteger());field(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());field(coordinator,"runtimeSchemaReady",new AtomicBoolean(true));field(coordinator,"criticalSchemaReady",new AtomicBoolean(true));store=new DbStore(db,coordinator);
            revocation();transactionWrapper();v51();jobAndHealth();trace(Paths.get(args[1]));System.out.println("R31_RUNTIME_DDL_NATIVE_HOST_PASS assertions="+checks+" scope=ACTUAL_PRODUCTION_METHODS_NATIVE_SQLITE_HOST_CONTROLLED_FIXTURES_NOT_ANDROID_NOT_DEVICE_NOT_NATURAL_QUALITY");
        }
    }
}
