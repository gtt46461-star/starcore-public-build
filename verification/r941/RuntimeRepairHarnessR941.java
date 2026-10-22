package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/** Regression tests use production methods with real SQLite and explicit test-only Android adapters. */
public final class RuntimeRepairHarnessR941 {
    static int checks;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println(name+"=PASS");}
    static final class ProbeDb extends SQLiteDatabase {
        int ddl;
        ProbeDb(String bridge) throws Exception {super(bridge);}
        @Override public synchronized void execSQL(String sql){if(sql.startsWith("CREATE "))ddl++;super.execSQL(sql);}
    }
    public static void main(String[] args) throws Exception {
        File dir=Files.createTempDirectory("r941-detail-test").toFile();
        StringBuilder text=new StringBuilder();
        for(int i=0;i<180000;i++)text.append("算法甲\\n\t\n轮胎😀完整内容");
        String body=text.toString();text=null;
        File file=DiagnosticFileStoreR941.write(dir,body);
        check(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8).equals(body),"UTF8_LONG_DETAIL_EXACT_ROUNDTRIP");
        check(DiagnosticFileStoreR941.preview(file).length()<17000,"PREVIEW_MEMORY_BOUNDED");
        check(DiagnosticFileStoreR941.write(dir,body).equals(file),"REPEATED_DETAIL_CONTENT_IDEMPOTENT");
        boolean rejected=false;try{DiagnosticFileStoreR941.resolve(dir,"../escape.txt");}catch(java.io.IOException expected){rejected=true;}
        check(rejected,"DETAIL_PATH_ESCAPE_REJECTED");
        ProbeDb db=new ProbeDb(args[0]);
        try {
            DatabaseCoordinator coordinator=new DatabaseCoordinator(db);
            boolean rolledBack=coordinator.runAtomicWrite("ROLLBACK_SCHEMA",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase database){
                CandidateCalibrationR932.schema(database);int first=db.ddl;
                for(int repeat=0;repeat<100;repeat++)CandidateCalibrationR932.schema(database);
                check(db.ddl==first,"REPEATED_SCHEMA_DDL_ELIMINATED_WITHIN_TRANSACTION");
                throw new IllegalStateException("INJECTED_ROLLBACK");
            }});
            check(!rolledBack,"INJECTED_SCHEMA_TRANSACTION_ROLLED_BACK");
            try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE name=?",new String[]{CandidateCalibrationR932.OBS})){
                check(c.moveToFirst()&&c.getInt(0)==0,"ROLLED_BACK_SCHEMA_NOT_PRESENT");
            }
            check(coordinator.runAtomicWrite("RETRY_SCHEMA",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase database){CandidateCalibrationR932.schema(database);}}),"SCHEMA_READY_CACHE_DOES_NOT_ESCAPE_ROLLBACK");
            check(!TransactionSchemaCacheR941.ready(db,"CandidateCalibrationR932"),"SCHEMA_READY_CACHE_NOT_SHARED_AFTER_COMMIT");
            DbStore store=new DbStore(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue TEXT PRIMARY KEY,status TEXT,snapshot_blob BLOB,snapshot_hash TEXT,next_action_ordinal INTEGER,lease_owner TEXT,lease_until INTEGER,next_attempt_at INTEGER,updated_at INTEGER,last_stage TEXT,last_error TEXT)");
            for(String[] row:new String[][]{{"a","SQLiteDatabaseLockedException:busy"},{"b","OOM_CAUGHT"},{"c","SNAPSHOT_HASH_MISMATCH"},{"d","SNAPSHOT_HASH_MISMATCH:SQLiteDatabaseLockedException"}})
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" VALUES(?,'FAILED_TERMINAL',X'010203','frozen',120,'',0,0,0,'FAILED_TERMINAL',?)",row);
            check(DeferredEvidenceWriterR36.recoverResourceFailures(store)==2,"ONLY_RESOURCE_FAILURES_REOPENED");
            try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" WHERE status='RETRY_WAIT' AND snapshot_hash='frozen' AND next_action_ordinal=120 AND length(snapshot_blob)=3",null)){
                check(c.moveToFirst()&&c.getInt(0)==2,"RECOVERY_PRESERVES_FROZEN_BYTES_HASH_AND_CURSOR");
            }
            check(DeferredEvidenceWriterR36.recoverResourceFailures(store)==0,"RESOURCE_RECOVERY_IS_IDEMPOTENT");
            check(DeferredEvidenceWriterR36.shouldContinueDrain(true,-1)&&!DeferredEvidenceWriterR36.shouldContinueDrain(false,6754),"UNKNOWN_BACKLOG_RETRIES_STOPPED_SERVICE_DOES_NOT");
            check(DeferredEvidenceWriterR36.continuationDelayMs(-1,false)==5000L,"UNKNOWN_BACKLOG_BACKOFF_PREVENTS_SPIN");
            Set<Integer> attempted=new HashSet<Integer>();
            // Missing lane inputs deliberately fail: each task must still get a turn, and cursors stay durable.
            for(int pulse=0;pulse<8;pulse++){
                ResearchLearningContinuityR912.pulse(store,500);
                for(int lane=0;lane<ResearchLearningContinuityR912.CONTINUITY_LANES;lane++)
                    if(!store.getState("r941_continuity_lane_"+lane,"").isEmpty())attempted.add(lane);
            }
            check(attempted.size()==14,"ALL_FOURTEEN_LANES_ATTEMPTED_DESPITE_TASK_FAILURES");
            check(ResearchPipelineStatusR941.compact(store).contains("N/A:"),"MISSING_EVIDENCE_TABLE_NOT_REPORTED_AS_ZERO");
        } finally {db.shutdown();file.delete();dir.delete();}
        auditProductionEnvelope(args[0]);
        System.out.println("R941_RUNTIME_REPAIR_PASS|checks="+checks+"|scope=HOST_REAL_SQLITE_NOT_PHONE");
    }
    static void auditProductionEnvelope(String bridge) throws Exception {
        String hash=V51ChecksumEngine.sha256("R941_TEST_CONTRACT");
        java.lang.reflect.Method signer=V51AuthorizationCore.class.getDeclaredMethod("sign",String.class,String.class,
                String.class,V51Enums.DecisionAction.class,V51Enums.RejectCode.class,long.class);
        signer.setAccessible(true);
        String auth=(String)signer.invoke(null,hash,hash,hash,V51Enums.DecisionAction.BET,V51Enums.RejectCode.NONE,1789209860000L);
        check(auth.length()>64 && V51AuthorizationCore.isBetEnvelope(auth),"PRODUCTION_AUTHORIZATION_ENVELOPE_RECOGNIZED");
        check(!V51AuthorizationCore.isBetEnvelope(auth+"|extra")
                && !V51AuthorizationCore.isBetEnvelope(auth.replace("|BET|","|SHADOW|"))
                && !V51AuthorizationCore.isBetEnvelope(auth.replace(hash,"invalid")),"MALFORMED_AND_NONBET_ENVELOPES_REJECTED");
        ProbeDb db=new ProbeDb(bridge);
        try {
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_TRADES+"(_id INTEGER PRIMARY KEY,target_issue TEXT,settled INTEGER,action TEXT,stat_scope TEXT,source_type TEXT,is_realtime_real INTEGER,authorization_valid INTEGER,authorized_mode TEXT,authorization_id TEXT,contract_valid INTEGER,contract_status TEXT,lane_candidate TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,k3_mask INTEGER,raw_p REAL,calibrated_p REAL,final_p REAL,authorization_probability REAL)");
            db.execSQL("CREATE TABLE "+V51Repository.AUTH+"(decision_id TEXT,target_issue TEXT,authorization_hash TEXT,contract_hash TEXT)");
            db.execSQL("CREATE TABLE "+V51Repository.SELECTED+"(decision_id TEXT,target_issue TEXT,authorization_hash TEXT,contract_hash TEXT,decision_action TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue TEXT,authorization_hash TEXT,contract_hash TEXT,decision_action TEXT,pre_draw_frozen INTEGER,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_TRADES+" VALUES(1,'202609120645',1,'BET','REAL_TRADE','REALTIME',1,1,'BET',?,1,?,'CONTROLLED_PROBE',4,2,6,9,0,0.43,0,0,0)",new Object[]{auth,ControlledRealContractAuditR922.EXACT_CONTRACT_STATUS});
            db.execSQL("INSERT INTO "+V51Repository.AUTH+" VALUES(?,'202609120645',?,?)",new Object[]{hash,auth,hash});
            db.execSQL("INSERT INTO "+V51Repository.SELECTED+" VALUES(?,'202609120645',?,?,'BET',4,2,6,9)",new Object[]{hash,auth,hash});
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" VALUES('202609120645',?,?,'BET',1,4,2,6,9)",new Object[]{auth,hash});
            DbStore store=new DbStore(db);
            check(ControlledRealContractAuditR922.capture(store).passed,"SIGNED_ENVELOPE_WITH_MATCHING_SQL_WITNESSES_PASSES_AUDIT");
            db.execSQL("UPDATE "+V51Repository.SELECTED+" SET n1=3");
            ControlledRealContractAuditR922.Report invalid=ControlledRealContractAuditR922.capture(store);
            check(!invalid.passed&&invalid.rejectReasons.containsKey("AUTHORIZATION_EVIDENCE_MISMATCH"),"MISMATCHED_FROZEN_ACTION_REMAINS_INVALID");
            db.execSQL("UPDATE "+V51Repository.SELECTED+" SET n1=2");
            db.execSQL("UPDATE "+DbHelper.TABLE_TRADES+" SET source_type='UNKNOWN'");
            invalid=ControlledRealContractAuditR922.capture(store);
            check(!invalid.passed&&invalid.rejectReasons.containsKey("SOURCE_NOT_REALTIME"),"NONREALTIME_HISTORY_NOT_RELABELED_BY_FORMAT_FIX");
        } finally {db.shutdown();}
    }
}
