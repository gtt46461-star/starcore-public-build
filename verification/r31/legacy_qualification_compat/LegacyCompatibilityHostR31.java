package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.nio.file.*;
import java.util.*;
import java.lang.reflect.Field;

/** Controlled actual SQLite compatibility fixtures; never natural evidence. */
public final class LegacyCompatibilityHostR31 {
    static final String ID=LiveBudgetHostR31.ID;
    static final long BASE=LiveBudgetHostR31.BASE;
    static void need(boolean condition,String detail){LiveBudgetHostR31.need(condition,detail);}
    static long scalar(SQLiteDatabase db,String sql,String...args){return LiveBudgetHostR31.scalar(db,sql,args);}
    static String record(SQLiteDatabase db,String sql,String...args){List<Object> rows=new ArrayList<>();try(Cursor c=db.rawQuery(sql,args)){while(c.moveToNext()){List<Object> row=new ArrayList<>();for(int i=0;i<c.getColumnCount();i++)row.add(c.isNull(i)?null:c.getType(i)==1?Long.valueOf(c.getLong(i)):c.getType(i)==2?Double.valueOf(c.getDouble(i)):c.getString(i));rows.add(row);}}return JsonR27.write(rows);}
    static String eprocess(SQLiteDatabase db){return record(db,"SELECT * FROM r27_eprocess WHERE policy_hash=?",ID);}
    static String certificateContent(SQLiteDatabase db){return record(db,"SELECT gates_json,evidence_digest,issued_at,expires_at,gate_version FROM r27_certificate WHERE policy_hash=?",ID);}
    static Map<Long,String> originalReceipts(SQLiteDatabase db){Map<Long,String> rows=new LinkedHashMap<>();try(Cursor c=db.rawQuery("SELECT target_ordinal FROM r29_qual_receipt WHERE experiment_id=? ORDER BY target_ordinal",new String[]{ID})){while(c.moveToNext()){long ordinal=c.getLong(0);rows.put(ordinal,record(db,"SELECT * FROM r29_qual_receipt WHERE experiment_id=? AND target_ordinal=?",ID,Long.toString(ordinal)));}}return rows;}
    static void originalsUnchanged(SQLiteDatabase db,Map<Long,String> original){for(Map.Entry<Long,String> row:original.entrySet())need(row.getValue().equals(record(db,"SELECT * FROM r29_qual_receipt WHERE experiment_id=? AND target_ordinal=?",ID,Long.toString(row.getKey()))),"EXISTING_RECEIPT_WAS_ERASED_OR_REWRITTEN:"+row.getKey());}
    static void otherLedgers(SQLiteDatabase db){need(scalar(db,"SELECT count(*) FROM r27_holdout_exposure")==1,"INDEPENDENT_EXPOSURE_CHANGED");need(scalar(db,"SELECT count(*) FROM r27_brier_pair WHERE source='REPLAY'")==1,"REPLAY_RELABELED");need(scalar(db,"SELECT count(*) FROM "+com.ethk3.novaonepro.DbHelper.TABLE_TRADES+" WHERE stat_scope='REAL_TRADE' AND settled=1")==1,"REAL_COUNT_CHANGED");need("[[6.9]]".equals(record(db,"SELECT sum(pnl) FROM "+com.ethk3.novaonepro.DbHelper.TABLE_TRADES+" WHERE stat_scope='REAL_TRADE'")),"REAL_FUNDS_CHANGED");}
    static QualificationInboxR29.Pass round(LiveBudgetHostR31.World w)throws Exception{return QualificationInboxR29.consume(w.ro,(operation,batch)->{RuntimeR27.submit(w.worker,operation,batch,false,QualificationInboxR29.preparedInputR31(batch));return true;},"('ACTIVE_REPAIR1')",BASE+256);}
    @SuppressWarnings("unchecked") static Map<String,String> dependencies(QualificationInboxR29.Pass pass)throws Exception {Field f=pass.getClass().getField("dependencies");return(Map<String,String>)f.get(pass);}
    static void drain(LiveBudgetHostR31.World w)throws Exception{for(int i=0;i<40&&QualificationInboxR29.needsRebuild(w.ro,ID);i++)round(w);need(!QualificationInboxR29.needsRebuild(w.ro,ID),"COMPATIBILITY_DID_NOT_COMPLETE");}
    static String[] guardDdl()throws Exception{return(String[])QualificationInboxR29.class.getMethod("legacyReceiptGuardDdlR31").invoke(null);}
    static void requireGuards(SQLiteDatabase db)throws Exception{try{QualificationInboxR29.class.getMethod("requireLegacyReceiptGuardsR31",SQLiteDatabase.class).invoke(null,db);}catch(java.lang.reflect.InvocationTargetException failure){Throwable cause=failure.getCause();if(cause instanceof Exception)throw(Exception)cause;throw(Error)cause;}}
    static void installGuards(LiveBudgetHostR31.World w)throws Exception {
        RuntimeCoordinatorR26.unbindBackgroundClaimR31();SQLiteDatabase.scenario="ACTUAL_SCHEMA_OWNER_QUALIFICATION_RECEIPT_MIGRATION";
        try{w.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R26_RUNTIME_SCHEMA"),db->{try{QualificationInboxR29.class.getMethod("ensureLegacyReceiptGuardsR31",SQLiteDatabase.class).invoke(null,db);}catch(java.lang.reflect.InvocationTargetException failure){Throwable cause=failure.getCause();if(cause instanceof Exception)throw(Exception)cause;throw(Error)cause;}requireGuards(db);return 0;});}
        finally{RuntimeCoordinatorR26.bindBackgroundClaim(w.claim);}
        // Test accounting is activated only after the real SQL contract verifies.
        try{w.db.getClass().getField("receiptInsertGuardInstalledR31").setBoolean(w.db,true);}catch(NoSuchFieldException oldAdapter){}
    }
    static String receipts(SQLiteDatabase db) {return receiptSuffix(db,Long.MIN_VALUE);}
    static String receiptSuffix(SQLiteDatabase db,long after) {
        List<Object> rows=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT target_ordinal,target_issue,act,hit,u,digest,state_after,consumed_at FROM r29_qual_receipt WHERE experiment_id=? AND target_ordinal>? ORDER BY target_ordinal",new String[]{ID,Long.toString(after)})) {
            while(c.moveToNext())rows.add(Arrays.asList(c.getLong(0),c.getString(1),c.getInt(2),c.getInt(3),Double.doubleToLongBits(c.getDouble(4)),c.getString(5),c.getString(6),c.getLong(7)));
        }return JsonR27.write(rows);
    }
    static void fixture(LiveBudgetHostR31.World w)throws Exception {
        if(LiveBudgetHostR31.scalar(w.db,"SELECT count(*) FROM r27_live_shadow WHERE policy_hash=?",ID)>0)return;
        w.fixture(256,false,true);SQLiteDatabase.scenario="CONTROLLED_ORIGINAL_REGISTRATION_AND_PIT_FIXTURE";
        w.db.execSQL("UPDATE draws SET open_time=20000,fetch_time=20001");w.db.execSQL("UPDATE r29_qual_calendar SET registered_at=0,origin='REGISTERED_BEFORE_FUTURE' WHERE experiment_id=?",new Object[]{ID});
        w.qualify(256);w.invariants(256);SQLiteDatabase.scenario="CONTROLLED_LEGACY_RECEIPT_LOSS";
        w.db.execSQL("DELETE FROM r29_qual_receipt WHERE experiment_id=? AND target_ordinal<=?",new Object[]{ID,BASE+160});
        w.db.execSQL("INSERT INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at) VALUES(?,'ACTIVE','{\"original\":true}','ORIGINAL_CERTIFICATE_DIGEST',1,999999)",new Object[]{ID});
    }
    static void positive(Path folder,String kind)throws Exception {
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("positive-"+kind+".db"))){
            fixture(w);int initial=96,pages=20;
            if(kind.equals("NO_RECEIPTS_NO_CURSOR")){SQLiteDatabase.scenario="CONTROLLED_LEGACY_WITHOUT_RECEIPTS_OR_CURSOR";w.db.execSQL("DELETE FROM r29_qual_receipt WHERE experiment_id=?",new Object[]{ID});w.db.execSQL("DELETE FROM r29_qual_cursor WHERE experiment_id=?",new Object[]{ID});initial=0;pages=32;}
            installGuards(w);
            String old=eprocess(w.db),cert=certificateContent(w.db);Map<Long,String> original=originalReceipts(w.db);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_positive_"+kind;
            for(int i=0;i<pages;i++){
                QualificationInboxR29.Pass pass=round(w);need(old.equals(eprocess(w.db)),"COMPATIBILITY_CHANGED_EPROCESS_ROW");originalsUnchanged(w.db,original);otherLedgers(w.db);
                need(scalar(w.db,"SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=?",ID)==initial+(i+1)*8,"LEGACY_PAGE_NOT_APPENDED:"+pass.blocked);
                if(i+1<pages)need("LEGACY_RECEIPT_EVIDENCE_RESTORING".equals(dependencies(pass).get(ID)),"PARTIAL_MIGRATION_NOT_EXPLICIT_DEPENDENCY");
                need(QualificationInboxR29.blockedForCertificate(w.ro,ID)==(i+1<pages),"PARTIAL_LEGACY_MIGRATION_AUTHORIZED_CERTIFICATE");
            }
            need(cert.equals(certificateContent(w.db)),"ORIGINAL_CERTIFICATE_CONTENT_WAS_REWRITTEN");need(scalar(w.db,"SELECT count(*) FROM r29_qual_rebuild WHERE experiment_id=?",ID)==1,"LEGACY_AUDIT_NOT_IDEMPOTENT");w.invariants(256);int before=SQLiteDatabase.frames.size();round(w);round(w);need(SQLiteDatabase.frames.size()==before,"COMPLETE_LEGACY_MIGRATION_REPEATED_WRITES");w.invariants(256);LiveBudgetHostR31.budgets(start,false);
        }System.out.println("CHECK append_only_legacy_"+kind+"_all_old_bytes_and_math_preserved");
    }
    static void healthyExperiment(LiveBudgetHostR31.World w)throws Exception {
        String healthy=StatsR27.sha256("CONTROLLED_HEALTHY_INDEPENDENT_EXPERIMENT");SQLiteDatabase.scenario="CONTROLLED_HEALTHY_EXPERIMENT";
        w.db.execSQL("INSERT INTO r27_live_shadow SELECT ?,spec_json,temperature,bias,2,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi FROM r27_live_shadow WHERE policy_hash=?",new Object[]{healthy,ID});
        w.db.execSQL("INSERT INTO r27_eprocess VALUES(?,2,?,0,0,0,0,0,0,0,-1,'ACCUMULATING',.15,0)",new Object[]{healthy,SequentialQualificationR27.alpha(2)});
        w.db.execSQL("INSERT INTO r29_qual_calendar VALUES(?,?,0,'REGISTERED_BEFORE_FUTURE')",new Object[]{healthy,BASE+241});
        w.db.execSQL("INSERT INTO r27_live_frozen SELECT ?,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit,settled_at FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal>? AND target_ordinal<=?",new Object[]{healthy,ID,BASE+240,BASE+256});
    }
    static void negatives(Path folder)throws Exception {
        for(String kind:new String[]{"GAPPED_STATE","RECEIPT_CONFLICT","MISSING_ORIGINAL","UNKNOWN_BOUNDARY","MATH_MISMATCH","CURSOR_EXCESS","PIT_UNPROVEN"})try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("negative-"+kind+".db"))){
            fixture(w);SQLiteDatabase.scenario="CONTROLLED_INCOMPATIBLE_LEGACY_"+kind;
            if(kind.equals("GAPPED_STATE"))w.db.execSQL("UPDATE r27_eprocess SET n=255 WHERE policy_hash=?",new Object[]{ID});
            if(kind.equals("RECEIPT_CONFLICT"))w.db.execSQL("UPDATE r29_qual_receipt SET digest='ORIGINAL_CONFLICT_MUST_REMAIN' WHERE experiment_id=? AND target_ordinal=?",new Object[]{ID,BASE+161});
            if(kind.equals("MISSING_ORIGINAL"))w.db.execSQL("DELETE FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new Object[]{ID,BASE+200});
            if(kind.equals("UNKNOWN_BOUNDARY"))w.db.execSQL("UPDATE r29_qual_calendar SET origin='LEGACY_FIRST_FREEZE_LOWER_BOUND' WHERE experiment_id=?",new Object[]{ID});
            if(kind.equals("MATH_MISMATCH"))w.db.execSQL("UPDATE r27_eprocess SET log_e_up=log_e_up+1 WHERE policy_hash=?",new Object[]{ID});
            if(kind.equals("CURSOR_EXCESS"))w.db.execSQL("UPDATE r29_qual_cursor SET consumed_total=300 WHERE experiment_id=?",new Object[]{ID});
            if(kind.equals("PIT_UNPROVEN"))w.db.execSQL("UPDATE draws SET open_time=1 WHERE issue=?",new Object[]{LiveBudgetHostR31.issue(1)});
            healthyExperiment(w);installGuards(w);String old=eprocess(w.db),oldReceipts=receipts(w.db),cert=certificateContent(w.db);long oldCursor=scalar(w.db,"SELECT consumed_total FROM r29_qual_cursor WHERE experiment_id=?",ID);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="fail_closed_one_legacy_experiment_"+kind;
            QualificationInboxR29.Pass pass=round(w);String reason=dependencies(pass).get(ID);need(reason!=null&&reason.startsWith("LEGACY_"),"UNPROVEN_LEGACY_STATE_NOT_TYPED_DEPENDENCY:"+kind);
            need(old.equals(eprocess(w.db)),"FAIL_CLOSED_RESET_EXPOSURE:"+kind);need(oldReceipts.equals(receipts(w.db)),"FAIL_CLOSED_ERASED_OR_REWROTE_RECEIPTS:"+kind);need(cert.equals(certificateContent(w.db)),"FAIL_CLOSED_REWROTE_CERTIFICATE_CONTENT");
            need(oldCursor==scalar(w.db,"SELECT consumed_total FROM r29_qual_cursor WHERE experiment_id=?",ID),"FAIL_CLOSED_RESET_OR_INCREASED_OLD_CURSOR_EXPOSURE");
            need(scalar(w.db,"SELECT count(*) FROM r27_certificate WHERE policy_hash=? AND status='REVOKED_LEGACY_EXPOSURE'",ID)==1,"UNPROVEN_CERTIFICATE_NOT_REVOKED");need(QualificationInboxR29.blockedForCertificate(w.ro,ID),"UNPROVEN_LEGACY_STATE_ELIGIBLE_FOR_CERTIFICATE");otherLedgers(w.db);
            String healthy=StatsR27.sha256("CONTROLLED_HEALTHY_INDEPENDENT_EXPERIMENT");need(scalar(w.db,"SELECT n FROM r27_eprocess WHERE policy_hash=?",healthy)==16,"ONE_BLOCKED_EXPERIMENT_DISABLED_HEALTHY_RESEARCH");need(scalar(w.db,"SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=?",healthy)==16,"HEALTHY_EXPERIMENT_LOST_SAMPLES");
            int before=SQLiteDatabase.frames.size();round(w);round(w);need(SQLiteDatabase.frames.size()==before,"UNCHANGED_BLOCKED_LEGACY_REPEATS_WRITES");need(old.equals(eprocess(w.db))&&oldReceipts.equals(receipts(w.db)),"REENTRY_RESET_UNPROVEN_EVIDENCE");LiveBudgetHostR31.budgets(start,false);System.out.println("CHECK fail_closed_"+kind+"_original_evidence_kept_healthy_experiment_continues reason="+reason);
        }
    }
    static void rollbackCuts(Path folder)throws Exception {
        for(String edge:new String[]{"BEFORE_RECEIPTS","AFTER_BUSINESS"})for(int cut=1;cut<=20;cut++)try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("rollback-"+edge+"-"+cut+".db"))){
            fixture(w);installGuards(w);String old=eprocess(w.db);Map<Long,String> original=originalReceipts(w.db);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_rollback_"+edge+"_"+cut;SQLiteDatabase.failTransaction=SQLiteDatabase.txCounter+cut;SQLiteDatabase.failSqlPrefix=edge.equals("BEFORE_RECEIPTS")?"INSERT INTO r29_qual_receipt":"INSERT INTO r31_commit_receipt";
            try{drain(w);throw new AssertionError("LEGACY_ROLLBACK_NOT_REACHED");}catch(DatabaseCoordinator.WriteRejectedR31 expected){need(expected.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK,"LEGACY_ROLLBACK_OUTCOME_UNKNOWN");}finally{SQLiteDatabase.failTransaction=-1;}
            need(old.equals(eprocess(w.db)),"LEGACY_ROLLBACK_CHANGED_EXPOSURE");originalsUnchanged(w.db,original);need(scalar(w.db,"SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=?",ID)==96+(cut-1)*8,"LEGACY_ROLLBACK_PARTIAL_RECEIPTS");
            if(cut==1){need(scalar(w.db,"SELECT count(*) FROM r29_qual_rebuild")==0,"AUDIT_PUBLISHED_ON_ROLLBACK");need(scalar(w.db,"SELECT count(*) FROM r27_certificate WHERE status='ACTIVE'")==1,"CERTIFICATE_REVOKED_ON_ROLLBACK");}
            drain(w);w.invariants(256);need(old.equals(eprocess(w.db)),"LEGACY_REENTRY_RECOUNTED_EXPOSURE");originalsUnchanged(w.db,original);need(scalar(w.db,"SELECT count(*) FROM r29_qual_rebuild")==1,"LEGACY_REENTRY_DUPLICATED_AUDIT");LiveBudgetHostR31.budgets(start,false);
        }System.out.println("CHECK every_legacy_append_slice_before_after_business_rollback_reentry_no_reset");
    }
    static ResearchWriteQueueR27.Batch prepare(LiveBudgetHostR31.World w)throws Exception {ResearchWriteQueueR27.Batch[] prepared=new ResearchWriteQueueR27.Batch[1];boolean result=QualificationInboxR29.rebuild(w.ro,(operation,batch)->{prepared[0]=batch;return false;},ID);need(!result&&prepared[0]!=null,"LEGACY_PREPARED_BATCH_MISSING");return prepared[0];}
    static void ownersAndSource(Path folder)throws Exception {
        for(String kind:new String[]{"CANCEL_BEFORE","CANCEL_DURING","OLD_OWNER","SOURCE_CHANGED","STATE_CHANGED","GUARD_CHANGED"})try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("owner-"+kind+".db"))){
            fixture(w);installGuards(w);ResearchWriteQueueR27.Batch b=prepare(w);String old=eprocess(w.db),oldReceipts=receipts(w.db);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_owner_or_CAS_"+kind;
            if(kind.equals("CANCEL_BEFORE"))Thread.currentThread().interrupt();
            if(kind.equals("CANCEL_DURING"))SQLiteDatabase.interruptTransaction=SQLiteDatabase.txCounter+1;
            if(kind.equals("OLD_OWNER")){RuntimeCoordinatorR26.Claim claim=w.claim;w.bind(2);RuntimeCoordinatorR26.bindBackgroundClaim(claim);}
            if(kind.equals("SOURCE_CHANGED")){w.db.execSQL("UPDATE draws SET open_time=open_time+1 WHERE issue=?",new Object[]{LiveBudgetHostR31.issue(1)});}
            if(kind.equals("STATE_CHANGED")){w.db.execSQL("UPDATE r27_eprocess SET sum_u=sum_u+1 WHERE policy_hash=?",new Object[]{ID});old=eprocess(w.db);}
            if(kind.equals("GUARD_CHANGED")){SQLiteDatabase.scenario="CONTROLLED_SCHEMA_DRIFT";w.db.execSQL("DROP TRIGGER r31_qual_receipt_no_update");w.db.execSQL("CREATE TRIGGER r31_qual_receipt_no_update BEFORE UPDATE ON r29_qual_receipt BEGIN SELECT 1; END");SQLiteDatabase.scenario="legacy_wrong_same_name_guard";}
            try{w.commit("R29_QUAL_LEGACY_COMPAT",b);throw new AssertionError("STALE_OR_CANCELLED_LEGACY_BATCH_ACCEPTED:"+kind);}catch(InterruptedException cancelled){need(kind.equals("CANCEL_BEFORE"),"UNEXPECTED_CANCEL_CLASSIFICATION");}catch(DatabaseCoordinator.WriteRejectedR31 rejected){need(rejected.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK,"LEGACY_OWNER_OR_SOURCE_OUTCOME_UNKNOWN");}
            finally{Thread.interrupted();SQLiteDatabase.interruptTransaction=-1;RuntimeCoordinatorR26.bindBackgroundClaim(w.claim);}
            need(old.equals(eprocess(w.db))&&oldReceipts.equals(receipts(w.db)),"OLD_OWNER_OR_CANCEL_ADVANCED_LEGACY_EVIDENCE");need(scalar(w.db,"SELECT count(*) FROM r29_qual_rebuild")==0,"OLD_OWNER_OR_CANCEL_PUBLISHED_AUDIT");need(scalar(w.db,"SELECT count(*) FROM r27_certificate WHERE status='ACTIVE'")==1,"OLD_OWNER_OR_CANCEL_REVOKED_NEW_OWNER_CERTIFICATE");LiveBudgetHostR31.budgets(start,false);
        }System.out.println("CHECK legacy_cancel_owner_source_receipt_and_complete_state_CAS_no_publication");
    }
    static void diagnostic(Path folder)throws Exception {
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("diagnostic.db"))){fixture(w);installGuards(w);String old=eprocess(w.db);Map<Long,String> original=originalReceipts(w.db);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_committed_diagnostic_failure";
            LiveBudgetHostR31.diagnostic("AFTER_COMMIT",()->{throw new IllegalStateException("CONTROLLED_LEGACY_COMMITTED_DIAGNOSTIC");});try{drain(w);}finally{LiveBudgetHostR31.diagnostic(null,null);}w.invariants(256);need(old.equals(eprocess(w.db)),"POST_COMMIT_DIAGNOSTIC_RESET_EXPOSURE");originalsUnchanged(w.db,original);need(scalar(w.db,"SELECT count(*) FROM r29_qual_rebuild")==1,"POST_COMMIT_DIAGNOSTIC_DUPLICATED_AUDIT");round(w);w.invariants(256);LiveBudgetHostR31.budgets(start,false);
        }System.out.println("CHECK legacy_commit_diagnostic_failure_cannot_erase_or_duplicate_old_or_reconstructed_evidence");
    }
    static void migrationAndGuards(Path folder)throws Exception {
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("migration.db"))){
            fixture(w);String old=eprocess(w.db),receipts=receipts(w.db);SQLiteDatabase.scenario="legacy_missing_schema_dependency_without_DDL";int ddl=SQLiteDatabase.outsideDdl.size();QualificationInboxR29.Pass pending=round(w);
            need("LEGACY_RECEIPT_GUARD_SCHEMA_PENDING".equals(dependencies(pending).get(ID)),"MISSING_GUARD_NOT_DEPENDENCY");need(SQLiteDatabase.outsideDdl.size()==ddl,"BUSINESS_CREATED_GUARDS");need(old.equals(eprocess(w.db))&&receipts.equals(receipts(w.db)),"MISSING_GUARD_RESET_EVIDENCE");
            SQLiteDatabase.failTransaction=SQLiteDatabase.txCounter+1;SQLiteDatabase.failSqlPrefix="CREATE TRIGGER IF NOT EXISTS r31_qual_receipt_no_delete";
            try{installGuards(w);throw new AssertionError("MIGRATION_ROLLBACK_NOT_REACHED");}catch(IllegalStateException expected){}finally{SQLiteDatabase.failTransaction=-1;}
            need(scalar(w.db,"SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name LIKE 'r31_qual_receipt_no_%'")==0,"PARTIAL_SCHEMA_MIGRATION_PUBLISHED");need(old.equals(eprocess(w.db))&&receipts.equals(receipts(w.db)),"DDL_ROLLBACK_CHANGED_OLD_EXPOSURE");
            for(int i=0;i<4;i++)installGuards(w);requireGuards(w.ro);need(old.equals(eprocess(w.db))&&receipts.equals(receipts(w.db)),"REPEATED_MIGRATION_CHANGED_OLD_DATA");int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_after_actual_schema_owner_migration";drain(w);w.invariants(256);need(old.equals(eprocess(w.db)),"MIGRATION_RECOUNTED_OR_RESET_EXPOSURE");need(scalar(w.db,"SELECT count(*) FROM r29_qual_rebuild")==2,"PENDING_AND_VERIFIED_VERSIONED_AUDITS_NOT_PRESERVED");LiveBudgetHostR31.budgets(start,false);
        }System.out.println("CHECK missing_guard_business_DDL_zero_actual_migration_rollback_repeat_preserves_old_data_and_recovers");
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("guard-mutation.db"))){
            fixture(w);installGuards(w);ResearchWriteQueueR27.Batch prepared=prepare(w);String old=eprocess(w.db),receipts=receipts(w.db);SQLiteDatabase.scenario="guarded_legacy_same_count_race";
            String[] mutations={"UPDATE r29_qual_receipt SET digest='RACE' WHERE experiment_id=? AND target_ordinal=?","DELETE FROM r29_qual_receipt WHERE experiment_id=? AND target_ordinal=?","INSERT OR REPLACE INTO r29_qual_receipt SELECT experiment_id,target_ordinal,target_issue,act,hit,u,'RACE',state_after,consumed_at FROM r29_qual_receipt WHERE experiment_id=? AND target_ordinal=?"};
            for(String sql:mutations){try{w.db.execSQL(sql,new Object[]{ID,BASE+161});throw new AssertionError("IMMUTABLE_RECEIPT_MUTATION_ACCEPTED");}catch(android.database.sqlite.SQLiteException blocked){need(String.valueOf(blocked.getMessage()).contains("R31_QUAL_RECEIPT_IMMUTABLE"),"WRONG_IMMUTABILITY_REJECTION");}need(old.equals(eprocess(w.db))&&receipts.equals(receipts(w.db)),"GUARD_MUTATION_CHANGED_OLD_EVIDENCE");}
            int start=SQLiteDatabase.frames.size();w.commit("R29_QUAL_LEGACY_COMPAT",prepared);drain(w);w.invariants(256);need(old.equals(eprocess(w.db)),"GUARDED_REPAIR_CHANGED_EXPOSURE");LiveBudgetHostR31.budgets(start,false);
            String explain=record(w.db,"EXPLAIN INSERT INTO r29_qual_receipt(experiment_id,target_ordinal,target_issue,act,hit,u,digest,state_after,consumed_at) VALUES(?,?,?,?,?,?,?,?,?)",ID,Long.toString(BASE+999),LiveBudgetHostR31.issue(999),"0","-1","0","CONTROLLED","ACCUMULATING","1");need(explain.contains("TRIGGER r31_qual_receipt_no_replace")&&explain.contains("SeekGE"),"INSERT_GUARD_NOT_INDEXED_EXISTS");System.out.println("EXPLAIN_GUARD "+explain);
        }System.out.println("CHECK immutable_update_delete_replace_guards_close_actual_receipt_CAS_race_with_indexed_lookup");
    }
    static void normalReplay(Path folder)throws Exception {
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("normal-100-replay.db"))){
            w.fixture(16,false,true);installGuards(w);SequentialQualificationR27.State s=StoreR27.readEprocess(w.ro,ID);QualificationInboxR29.Plan p=QualificationInboxR29.plan(s,QualificationInboxR29.rows(w.ro,ID,-1,16),BASE+16,16,BASE+1);ResearchWriteQueueR27.Batch b=QualificationInboxR29.step(ID,s,p,-1,0);
            String key=HashingSU1.text("CONTROLLED_100_IDENTICAL_NORMAL_RECEIPT_REPLAYS"),input=HashingSU1.text("CONTROLLED_ORIGINAL_NORMAL_16_PREFIX");ResearchWriteQueueR27.ImmutableBatch immutable=b.prepareR31("R29_QUAL_CONSUME",key,"r27-live",w.claim.owner,w.claim.fence,input,999,"R31_NORMAL_REPLAY_HOST");
            int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="normal_16_prefix_and_100_durable_receipt_replays";
            for(int i=0;i<101;i++){need(w.worker.writes().submit("R29_QUAL_CONSUME",immutable,w.worker::cancelled),"NORMAL_DURABLE_RECEIPT_REPLAY_REJECTED");w.invariants(16);}
            need(scalar(w.db,"SELECT count(*) FROM r31_commit_receipt WHERE commit_key=?",key)==1,"NORMAL_REPLAY_DUPLICATED_COMMIT_RECEIPT");for(int i=0;i<100;i++)w.qualify(16);w.invariants(16);LiveBudgetHostR31.budgets(start,false);
        }System.out.println("CHECK normal_16_prefix_100_durable_replays_and_100_business_reentries_keep_all_original_samples_once");
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("normal-full-256.db"))){w.fixture(256,false,true);installGuards(w);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="normal_full_256_with_startup_receipt_guards";w.qualify(256);w.invariants(256);for(int i=0;i<100;i++)w.qualify(256);w.invariants(256);LiveBudgetHostR31.budgets(start,false);}System.out.println("CHECK all_original_256_normal_opportunities_and_100_reentries_with_guard_cost_counted");
    }
    static void retention(Path folder)throws Exception {
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("retention.db"))){
            fixture(w);installGuards(w);String old=eprocess(w.db),rows=receipts(w.db);RuntimeCoordinatorR26.unbindBackgroundClaimR31();SQLiteDatabase.scenario="ACTUAL_SCHEMA_OWNER_EXISTING_RETENTION_ARCHIVES";
            try{w.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R26_RUNTIME_SCHEMA"),db->{ResearchRetentionStoreR30.ensureProtected(db);return 0;});w.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R26_RUNTIME_SCHEMA"),db->{ResearchRetentionStoreR30.ensureProtected(db);return 0;});}finally{RuntimeCoordinatorR26.bindBackgroundClaim(w.claim);}
            requireGuards(w.ro);need(old.equals(eprocess(w.db))&&rows.equals(receipts(w.db)),"EXISTING_RETENTION_SCHEMA_CHANGED_QUALIFICATION_LEDGER");
            SQLiteDatabase.scenario="CONTROLLED_EXPIRED_RETENTION_FAILURE_METADATA";for(int i=0;i<2600;i++)w.db.execSQL("INSERT INTO r26_live_failure VALUES('CONTROLLED_EXPIRED',?,'CONTROLLED',?)",new Object[]{i,i});
            RuntimeCoordinatorR26.unbindBackgroundClaimR31();SQLiteDatabase.scenario="EXISTING_RETENTION_PHASE17_COMPATIBILITY_NOT_ITS_BUDGET_ACCEPTANCE";
            try{for(int i=0;i<3;i++)w.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RETENTION,"R26_RETENTION_PHASE_17"),db->{RetentionR26.pruneStep(db,BASE+256,17);return 0;});}finally{RuntimeCoordinatorR26.bindBackgroundClaim(w.claim);}
            need(scalar(w.db,"SELECT count(*) FROM r26_live_failure")==2048,"LEGITIMATE_EXISTING_RETENTION_FAILURE_TAIL_STOPPED");need(old.equals(eprocess(w.db))&&rows.equals(receipts(w.db)),"EXISTING_RETENTION_RESET_OR_ERASED_QUALIFICATION");int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_repair_with_existing_retention_triggers_installed";drain(w);w.invariants(256);need(old.equals(eprocess(w.db)),"RETENTION_INTEROP_CHANGED_OLD_EXPOSURE");LiveBudgetHostR31.budgets(start,false);
        }System.out.println("CHECK actual_existing_retention_schema_and_metadata_pruning_coexist_with_guarded_append_only_qualification");
    }
    static void foldbackDuplicates(Path folder)throws Exception {
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("foldback-target.db"));SQLiteDatabase source=new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:"+folder.resolve("foldback-source.db")))){
            fixture(w);installGuards(w);source.execSQL(QualificationInboxR29.DDL[0]);Object[] row=new Object[9];try(Cursor c=w.ro.rawQuery("SELECT * FROM r29_qual_receipt ORDER BY target_ordinal LIMIT 1",null)){need(c.moveToFirst(),"ORIGINAL_FOLDBACK_RECEIPT_MISSING");for(int i=0;i<row.length;i++)row[i]=c.getType(i)==1?Long.valueOf(c.getLong(i)):c.getType(i)==2?Double.valueOf(c.getDouble(i)):c.getString(i);}source.execSQL("INSERT INTO r29_qual_receipt VALUES(?,?,?,?,?,?,?,?,?)",row);
            String old=eprocess(w.db),rows=receipts(w.db);RuntimeCoordinatorR26.unbindBackgroundClaimR31();SQLiteDatabase.scenario="ACTUAL_IDENTICAL_RECEIPT_FOLDBACK_MIGRATION_100_REPLAYS";
            try{for(int i=0;i<100;i++){TableCopierR30.Outcome copy=TableCopierR30.copy(source,w.ro,(operation,body)->w.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R26_RUNTIME_SCHEMA"),db->{body.run(db);return 0;}),"r29_qual_receipt","CONTROLLED_IDENTICAL_FOLDBACK_"+i,"R29_RP_TO_ROOT");need(copy.state.equals("COPIED")&&copy.copiedRows==1,"LEGITIMATE_IDENTICAL_FOLDBACK_STOPPED");need(old.equals(eprocess(w.db))&&rows.equals(receipts(w.db)),"IDENTICAL_FOLDBACK_REPLACED_OR_RECOUNTED_OLD_RECEIPT");}
                source.execSQL("UPDATE r29_qual_receipt SET digest='CONFLICTING_OLD_SOURCE_MUST_REMAIN'");boolean rejected=false;try{TableCopierR30.copy(source,w.ro,(operation,body)->w.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R26_RUNTIME_SCHEMA"),db->{body.run(db);return 0;}),"r29_qual_receipt","CONTROLLED_CONFLICTING_FOLDBACK","R29_RP_TO_ROOT");}catch(IllegalStateException conflict){for(Throwable cause=conflict;cause!=null;cause=cause.getCause())if(String.valueOf(cause.getMessage()).contains("R31_QUAL_RECEIPT_IMMUTABLE"))rejected=true;}need(rejected,"CONFLICTING_FOLDBACK_OVERWROTE_ORIGINAL");need(old.equals(eprocess(w.db))&&rows.equals(receipts(w.db)),"CONFLICTING_FOLDBACK_CHANGED_ORIGINAL_ROOT_EVIDENCE");need(record(source,"SELECT digest FROM r29_qual_receipt").contains("CONFLICTING_OLD_SOURCE_MUST_REMAIN"),"CONFLICTING_SOURCE_WAS_ERASED");
            }finally{RuntimeCoordinatorR26.bindBackgroundClaim(w.claim);}
            int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_append_after_identical_folback";drain(w);w.invariants(256);need(old.equals(eprocess(w.db)),"FOLDBACK_COMPAT_RESET_EXPOSURE");LiveBudgetHostR31.budgets(start,false);
        }System.out.println("CHECK actual_TableCopier_100_identical_receipt_duplicates_noop_conflicting_source_and_root_both_preserved");
    }
    static void upgrade(Path folder)throws Exception {
        Path database=folder.resolve("upgrade-v1-v2.db");String oldSql=new String(Files.readAllBytes(folder.resolve("actual-v1-guard.sql")),java.nio.charset.StandardCharsets.UTF_8).trim();String old,rows;
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(database)){
            fixture(w);old=eprocess(w.db);rows=receipts(w.db);String[] ddl=guardDdl();RuntimeCoordinatorR26.unbindBackgroundClaimR31();SQLiteDatabase.scenario="ACTUAL_KNOWN_V1_GUARD_SCHEMA_FIXTURE";
            try{w.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R26_RUNTIME_SCHEMA"),db->{db.execSQL(ddl[0]);db.execSQL(ddl[1]);db.execSQL(oldSql);return 0;});}finally{RuntimeCoordinatorR26.bindBackgroundClaim(w.claim);}
            try{requireGuards(w.ro);throw new AssertionError("OLD_STRICT_GUARD_PASSED_NEW_READY_CHECK");}catch(DatabaseCoordinator.SchemaPendingR31 expected){}
            SQLiteDatabase.failTransaction=SQLiteDatabase.txCounter+1;SQLiteDatabase.failSqlPrefix="CREATE TRIGGER IF NOT EXISTS r31_qual_receipt_no_replace";
            try{installGuards(w);throw new AssertionError("UPGRADE_ROLLBACK_NOT_REACHED");}catch(IllegalStateException expected){}finally{SQLiteDatabase.failTransaction=-1;}
            need(oldSql.equals(LiveBudgetHostR31.text(w.db,"SELECT sql FROM sqlite_master WHERE name='r31_qual_receipt_no_replace'")),"UPGRADE_ROLLBACK_LOST_OR_CHANGED_OLD_GUARD");need(old.equals(eprocess(w.db))&&rows.equals(receipts(w.db)),"UPGRADE_ROLLBACK_MODIFIED_OLD_EXPOSURE");
            installGuards(w);requireGuards(w.ro);long version=scalar(w.db,"PRAGMA schema_version");installGuards(w);need(scalar(w.db,"PRAGMA schema_version")==version,"SAME_VERSION_OWNER_MIGRATION_NOT_IDEMPOTENT");need(old.equals(eprocess(w.db))&&rows.equals(receipts(w.db)),"KNOWN_V1_TO_V2_UPGRADE_CHANGED_OLD_DATA");
        }
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(database)){installGuards(w);requireGuards(w.ro);need(old.equals(eprocess(w.db))&&rows.equals(receipts(w.db)),"REOPEN_AFTER_UPGRADE_CHANGED_OLD_EXPOSURE");int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_after_known_v1_v2_upgrade_and_reopen";drain(w);w.invariants(256);LiveBudgetHostR31.budgets(start,false);}
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("unknown-guard.db"))){fixture(w);SQLiteDatabase.scenario="CONTROLLED_UNKNOWN_SAME_NAME_SCHEMA";w.db.execSQL("CREATE TRIGGER r31_qual_receipt_no_update BEFORE UPDATE ON r29_qual_receipt BEGIN SELECT 1; END");String unknown=LiveBudgetHostR31.text(w.db,"SELECT sql FROM sqlite_master WHERE name='r31_qual_receipt_no_update'"),before=eprocess(w.db),beforeRows=receipts(w.db);long version=scalar(w.db,"PRAGMA schema_version");
            try{installGuards(w);throw new AssertionError("UNKNOWN_GUARD_DROPPED_OR_ACCEPTED");}catch(IllegalStateException rejected){}
            need(unknown.equals(LiveBudgetHostR31.text(w.db,"SELECT sql FROM sqlite_master WHERE name='r31_qual_receipt_no_update'"))&&version==scalar(w.db,"PRAGMA schema_version"),"UNKNOWN_SAME_NAME_SQL_WAS_MUTATED");need(scalar(w.db,"SELECT count(*) FROM sqlite_master WHERE name IN ('r31_qual_receipt_no_delete','r31_qual_receipt_no_replace')")==0,"UNKNOWN_GUARD_DID_NOT_FAIL_BEFORE_DDL");need(before.equals(eprocess(w.db))&&beforeRows.equals(receipts(w.db)),"UNKNOWN_GUARD_CHANGED_ORIGINAL_EVIDENCE");
            try{QualificationInboxR29.class.getMethod("ensureLegacyReceiptGuardsR31",SQLiteDatabase.class).invoke(null,w.db);throw new AssertionError("SCHEMA_OWNER_TRANSACTION_NOT_REQUIRED");}catch(java.lang.reflect.InvocationTargetException rejected){need(String.valueOf(rejected.getCause().getMessage()).contains("SCHEMA_OWNER_TRANSACTION_REQUIRED"),"WRONG_STARTUP_OWNER_REJECTION");}
        }
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("shadowed-rowid.db"))){fixture(w);SQLiteDatabase.scenario="CONTROLLED_UNSUPPORTED_ROWID_SHADOW";w.db.execSQL("ALTER TABLE r29_qual_receipt ADD COLUMN rowid INTEGER");String before=eprocess(w.db),beforeRows=receipts(w.db);try{installGuards(w);throw new AssertionError("SHADOWED_ROWID_WATERMARK_ACCEPTED");}catch(IllegalStateException rejected){}need(before.equals(eprocess(w.db))&&beforeRows.equals(receipts(w.db)),"ROWID_SCHEMA_REFUSAL_CHANGED_OLD_EVIDENCE");need(scalar(w.db,"SELECT count(*) FROM sqlite_master WHERE name LIKE 'r31_qual_receipt_no_%'")==0,"UNSUPPORTED_ROWID_SCHEMA_NOT_REJECTED_BEFORE_DDL");}
        System.out.println("CHECK actual_known_v1_v2_upgrade_rollback_repeat_reopen_unknown_SQL_and_rowid_shadow_refusal_no_original_row_change");
    }
    static void vmBacklog(Path folder,boolean expectFailure)throws Exception {
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("vm-backlog-"+expectFailure+".db"))){
            fixture(w);installGuards(w);SQLiteDatabase.scenario="CONTROLLED_100000_RECEIPT_LEGACY_BACKLOG";
            w.db.execSQL("WITH RECURSIVE x(i) AS (SELECT 1 UNION ALL SELECT i+1 FROM x WHERE i<100000) INSERT INTO r29_qual_receipt SELECT ?,?+i,'CONTROLLED_NOT_NATURAL',0,-1,0,'ORIGINAL_UNPROVEN','ACCUMULATING',1 FROM x",new Object[]{ID,BASE+256});
            String old=eprocess(w.db);long oldRows=scalar(w.db,"SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=?",ID);need(oldRows==100096,"LARGE_LEGACY_FIXTURE_NOT_REAL");w.db.getClass().getField("trackVmStepsR31").setBoolean(w.db,true);int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_actual_writer_100000_receipt_VM_steps";round(w);w.db.getClass().getField("trackVmStepsR31").setBoolean(w.db,false);
            long steps=0;for(Map<String,Object> frame:SQLiteDatabase.frames.subList(start,SQLiteDatabase.frames.size()))steps+=((Number)frame.get("nativeVmStepLowerBound")).longValue();need(old.equals(eprocess(w.db))&&oldRows==scalar(w.db,"SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=?",ID),"LARGE_LEGACY_EVIDENCE_ERASED_OR_RESET");
            Map<String,Object> fact=new LinkedHashMap<>();fact.put("test","ACTUAL_LEGACY_WRITER_VM_STEPS_100000_RECEIPTS");fact.put("receiptRows",oldRows);fact.put("nativeVmStepLowerBound",steps);fact.put("interval",100);fact.put("instrumentedTimingIsNotLatencyAcceptance",true);System.out.println("RESULT "+JsonR27.write(fact));
            if(expectFailure){need(steps>200000,"UNBOUNDED_COUNT_COUNTEREXAMPLE_NOT_REPRODUCED");throw new AssertionError("CANDIDATE_FAIL_REPRODUCED_UNBOUNDED_LEGACY_WRITER_COUNT_SCAN");}
            need(steps<10000,"LEGACY_WRITER_STILL_SCANS_HISTORY");LiveBudgetHostR31.budgets(start,false);String explain=record(w.db,"EXPLAIN SELECT coalesce(max(rowid),0) FROM r29_qual_receipt");need(explain.contains("Last"),"GLOBAL_ROWID_MAX_NOT_LAST_KEY_PLAN");System.out.println("EXPLAIN_WATERMARK "+explain);
        }System.out.println("CHECK actual_100000_receipt_writer_VM_steps_bounded_original_unprovable_evidence_preserved");
    }
    static void concurrentOtherExperiment(Path folder)throws Exception {
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("other-experiment-append.db"))){
            fixture(w);healthyExperiment(w);installGuards(w);String old=eprocess(w.db),rows=receipts(w.db);boolean[] appended={false};int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_global_watermark_other_experiment_fair_CAS_retry";
            QualificationInboxR29.Pass pass=QualificationInboxR29.consume(w.ro,(operation,batch)->{
                if(!appended[0]&&operation.equals("R29_QUAL_LEGACY_COMPAT")){
                    appended[0]=true;String healthy=StatsR27.sha256("CONTROLLED_HEALTHY_INDEPENDENT_EXPERIMENT");SequentialQualificationR27.State s=StoreR27.readEprocess(w.ro,healthy);QualificationInboxR29.Plan p=QualificationInboxR29.plan(s,QualificationInboxR29.rows(w.ro,healthy,-1,16),BASE+256,16,BASE+241);ResearchWriteQueueR27.Batch other=QualificationInboxR29.step(healthy,s,p,-1,0);RuntimeR27.submit(w.worker,"R29_QUAL_CONSUME",other,false,QualificationInboxR29.preparedInputR31(other));
                }RuntimeR27.submit(w.worker,operation,batch,false,QualificationInboxR29.preparedInputR31(batch));return true;
            },"('ACTIVE_REPAIR1')",BASE+256);
            need(appended[0]&&pass.casRetries==1,"GLOBAL_WATERMARK_DID_NOT_RETRY_OTHER_EXPERIMENT_APPEND");need(old.equals(eprocess(w.db))&&rows.equals(receipts(w.db)),"CONSERVATIVE_CAS_PUBLISHED_STALE_LEGACY_PAGE");need(scalar(w.db,"SELECT count(*) FROM r29_qual_rebuild")==0,"STALE_GLOBAL_WATERMARK_PUBLISHED_AUDIT");need(!w.worker.cancelled()&&scalar(w.db,"SELECT count(*) FROM r26_task WHERE task_id='r27-live' AND status='RUNNING'")==1,"FAIR_CAS_RETRY_QUARANTINED_OWNER");
            drain(w);w.invariants(256);need(old.equals(eprocess(w.db)),"FAIR_RETRY_RESET_OR_RECOUNTED_EXPOSURE");LiveBudgetHostR31.budgets(start,false);
        }System.out.println("CHECK global_append_watermark_other_experiment_conservative_retry_makes_progress_without_permanent_isolation");
    }
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");Path folder=Paths.get(args[1]);Files.createDirectories(folder);
        String mode=args[0];
        if(mode.equals("positive")){positive(folder,"MISSING_160");positive(folder,"NO_RECEIPTS_NO_CURSOR");}
        else if(mode.equals("negative"))negatives(folder);
        else if(mode.equals("rollback"))rollbackCuts(folder);
        else if(mode.equals("owner"))ownersAndSource(folder);
        else if(mode.equals("diagnostic"))diagnostic(folder);
        else if(mode.equals("migration"))migrationAndGuards(folder);
        else if(mode.equals("normal"))normalReplay(folder);
        else if(mode.equals("retention"))retention(folder);
        else if(mode.equals("foldback"))foldbackDuplicates(folder);
        else if(mode.equals("upgrade"))upgrade(folder);
        else if(mode.equals("vm-backlog"))vmBacklog(folder,false);
        else if(mode.equals("vm-counterexample"))vmBacklog(folder,true);
        else if(mode.equals("concurrent"))concurrentOtherExperiment(folder);
        else if(mode.equals("halt")||mode.equals("resume")){
            try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("legacy-halt.db"))){
                fixture(w);installGuards(w);Path oldState=folder.resolve("original-eprocess.json"),oldRows=folder.resolve("original-receipts.json");
                if(mode.equals("halt")){Files.write(oldState,eprocess(w.db).getBytes(java.nio.charset.StandardCharsets.UTF_8));Files.write(oldRows,receipts(w.db).getBytes(java.nio.charset.StandardCharsets.UTF_8));SQLiteDatabase.scenario="legacy_actual_jvm_halt_after_durable_commit";LiveBudgetHostR31.diagnostic("AFTER_COMMIT",()->{System.out.flush();Runtime.getRuntime().halt(73);});round(w);throw new AssertionError("LEGACY_ACTUAL_HALT_NOT_REACHED");}
                String savedRows=new String(Files.readAllBytes(oldRows),java.nio.charset.StandardCharsets.UTF_8);need(savedRows.equals(receiptSuffix(w.db,BASE+160)),"JVM_HALT_CHANGED_ORIGINAL_RECEIPT_BYTES");need(new String(Files.readAllBytes(oldState),java.nio.charset.StandardCharsets.UTF_8).equals(eprocess(w.db)),"JVM_HALT_RESET_LEGACY_EXPOSURE");need(scalar(w.db,"SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=?",ID)==104,"JVM_HALT_LOST_OR_DUPLICATED_COMMITTED_LEGACY_PAGE");need(scalar(w.db,"SELECT count(*) FROM r29_qual_rebuild")==1,"JVM_HALT_LOST_OR_DUPLICATED_AUDIT");int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="legacy_fresh_process_resume_original_exposure";drain(w);w.invariants(256);need(new String(Files.readAllBytes(oldState),java.nio.charset.StandardCharsets.UTF_8).equals(eprocess(w.db)),"FRESH_JVM_REENTRY_RESET_LEGACY_EXPOSURE");need(savedRows.equals(receiptSuffix(w.db,BASE+160)),"FRESH_JVM_REENTRY_REWROTE_OR_ERASED_ORIGINAL_RECEIPT_BYTES");LiveBudgetHostR31.budgets(start,false);
            }System.out.println("CHECK actual_legacy_jvm_halt_and_fresh_process_resume_without_reset_or_recount");
        }
        else if(!mode.equals("baseline"))throw new IllegalArgumentException(mode);
        if(!mode.equals("baseline")){System.out.println("LEGACY_COMPAT_NATIVE_HOST_PASS assertions="+LiveBudgetHostR31.assertions+" scope=CONTROLLED_NATIVE_SQLITE_NOT_ANDROID_NOT_NATURAL_QUALITY");return;}
        try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve(args[0]+".db"))) {
            fixture(w);String original=receipts(w.db);long beforeN=LiveBudgetHostR31.scalar(w.db,"SELECT n FROM r27_eprocess WHERE policy_hash=?",ID),beforeRows=LiveBudgetHostR31.scalar(w.db,"SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=?",ID);
            SQLiteDatabase.scenario="baseline_legacy_rebuild_erasure";
            QualificationInboxR29.rebuild(w.ro,(operation,batch)->{w.commit(operation,batch);return true;},ID);
            long afterN=LiveBudgetHostR31.scalar(w.db,"SELECT n FROM r27_eprocess WHERE policy_hash=?",ID),afterRows=LiveBudgetHostR31.scalar(w.db,"SELECT count(*) FROM r29_qual_receipt WHERE experiment_id=?",ID);
            Map<String,Object> fact=new LinkedHashMap<>();fact.put("test","BASELINE_LEGACY_REBUILD_ERASURE");fact.put("beforeN",beforeN);fact.put("afterN",afterN);fact.put("beforeReceiptRows",beforeRows);fact.put("afterReceiptRows",afterRows);fact.put("originalReceiptSha256",HashingSU1.text(original));fact.put("afterReceiptSha256",HashingSU1.text(receipts(w.db)));System.out.println("RESULT "+JsonR27.write(fact));
            if(beforeN==256&&beforeRows==96&&afterN==0&&afterRows==0)throw new AssertionError("BASELINE_FAIL_REPRODUCED_LEGACY_EXPOSURE_RESET_AND_RECEIPT_ERASURE");
            throw new AssertionError("BASELINE_COUNTEREXAMPLE_NOT_REPRODUCED");
        }
    }
}
