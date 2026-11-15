package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

public final class R31ServiceProtocolHost {
    private static void need(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    private static void set(Object target,String name,Object value)throws Exception{Field f=DatabaseCoordinator.class.getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    private static Models.Draw draw(String issue){Models.Draw d=new Models.Draw();d.issue=issue;d.openTime=10000;d.fetchTime=10010;d.sourceLine="HOST";for(int i=0;i<10;i++)d.numbers[i]=i+1;return d;}
    private static void insertDraw(SQLiteDatabase db,Models.Draw draw){Object[] args=new Object[16];args[0]=draw.issue;args[1]=draw.openTime;args[2]=draw.fetchTime;args[3]=draw.fetchTime;args[4]=DrawHashR36.sha256V2(draw);for(int i=0;i<10;i++)args[i+5]=draw.numbers[i];args[15]=0;db.execSQL("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",args);db.execSQL("INSERT INTO "+DbHelper.TABLE_R811_DRAW_CANONICAL+" VALUES(?,'CONFIRMED_CANONICAL_DRAW',?)",new Object[]{draw.issue,DrawHashR36.sha256V2(draw)});}
    private static void schema(SQLiteDatabase db){
        db.beginTransaction();try{
            RuntimeSchemaR26.ensure(db);for(String sql:SchemaR27.R31_WRITE_DDL)db.execSQL(sql);SchemaDomainCoordinatorR926.migrateR31TransportTiming(db);
            db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_B03_TRANSACTION_AUDIT+"(idempotency_key TEXT PRIMARY KEY,operation TEXT,game_code TEXT,target_issue TEXT,transaction_hash TEXT,status TEXT,error_detail TEXT,created_at INTEGER)");
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,fetch_time INTEGER,first_seen_at INTEGER,draw_hash_v2 TEXT,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER)");
            db.execSQL("CREATE VIEW "+DrawDependencyInputCP12.SOURCE+" AS SELECT * FROM draws");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R811_DRAW_CANONICAL+"(target_issue TEXT PRIMARY KEY,status TEXT,canonical_draw_hash TEXT)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,draw_issue TEXT,status TEXT,created_at INTEGER,updated_at INTEGER,completed_at INTEGER,lease_owner TEXT,lease_until INTEGER,attempts INTEGER,next_attempt_at INTEGER,draw_hash TEXT,settlement_hash TEXT,last_error TEXT,task_class TEXT,priority INTEGER,terminal_code TEXT,source TEXT,lease_generation INTEGER DEFAULT 0,process_boot_id TEXT DEFAULT '',last_progress_at INTEGER DEFAULT 0,eligibility TEXT,failure_class TEXT)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue TEXT PRIMARY KEY,base_issue TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,selected_hit_p REAL,created_at INTEGER,commit_at INTEGER,record_hash TEXT,pre_draw_frozen INTEGER,model_proven INTEGER,settled INTEGER,history_hash TEXT,snapshot_hash TEXT,action_space_hash TEXT,game_code TEXT,history_max_issue TEXT,actual_number INTEGER,hit_label INTEGER,settled_at INTEGER,settlement_hash TEXT,error_code TEXT)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_OOS_LABEL+"(target_issue TEXT PRIMARY KEY,game_code TEXT,base_issue TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,selected_hit_p REAL,actual_number INTEGER,hit_label INTEGER,strict_oos INTEGER,pre_draw_frozen INTEGER,prediction_created_at INTEGER,draw_open_time INTEGER,settled_at INTEGER,brier REAL,provenance_hash TEXT,record_hash TEXT,error_code TEXT)");
            db.execSQL("CREATE TABLE "+SchemaRevisionR925.TABLE_FETCH_LEDGER+"(request_id TEXT PRIMARY KEY,method TEXT,endpoint TEXT,request_at INTEGER,status TEXT,error_code TEXT,clock_epoch TEXT,label_version TEXT,request_role TEXT,response_at INTEGER,received_at INTEGER DEFAULT 0,latency_ms INTEGER,http_code INTEGER,response_hash TEXT,payload_hash TEXT,received_mono_ms INTEGER,received_boot_id TEXT)");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    private static SettlementEngine.OwnerToken token(String issue)throws Exception{Constructor<SettlementEngine.OwnerToken> c=SettlementEngine.OwnerToken.class.getDeclaredConstructor(String.class,long.class);c.setAccessible(true);return c.newInstance(issue,System.currentTimeMillis());}
    private static void analysisDependencyRetry(SQLiteDatabase db,DatabaseCoordinator coordinator) {
        String table=DbHelper.TABLE_R36_ANALYSIS_REQUEST,base="202611040700",target="202611040701";
        db.execSQL("CREATE TABLE "+table+"(base_issue TEXT,target_issue TEXT PRIMARY KEY,status TEXT,generation INTEGER,request_at INTEGER,deadline_at INTEGER,started_at INTEGER,finished_at INTEGER,lease_owner TEXT,lease_until INTEGER,attempts INTEGER,last_stage TEXT,error_code TEXT,history_hash TEXT,canonical_hash TEXT,request_hash TEXT,next_attempt_at INTEGER,repair_version TEXT,first_error_code TEXT,last_transition_at INTEGER)");
        String hash=V51ChecksumEngine.sha256("HOST_RETRY_INPUT");long now=System.currentTimeMillis();
        db.execSQL("INSERT INTO "+table+" VALUES(?,?,'RUNNING',1,?,?,?,0,'same-owner',?,100,'INFER','','','',?,0,'','original-first-error',?)",
                new Object[]{base,target,now,now+55000,now,now+55000,hash,now});
        AnalysisSingleFlightR36.Request request=new AnalysisSingleFlightR36.Request(base,target,1,now,now+55000,"HOST_RETRY",hash,"");
        AnalysisRequestStoreR36 requests=new AnalysisRequestStoreR36(new DbStore(db,coordinator));
        for(String category:new String[]{"DEPENDENCY","LOCK_BUSY","RESOURCE_MEMORY","RESOURCE_QUEUE","TEMP_TIMEOUT"}) {
            db.execSQL("UPDATE "+table+" SET status='RUNNING',lease_owner='same-owner'");
            Throwable failure=new DatabaseCoordinator.WriteRejectedR31(category,"R932_DOMAIN_ROUTE","ADMISSION",
                    DatabaseCoordinator.CommitOutcome.ROLLED_BACK,new IllegalStateException("PRODUCER_WAIT"));
            AnalysisRequestStoreR36.TransitionResult changed=requests.markRetry(request,"same-owner","unmodified diagnostic",failure,now+1);
            need(changed.updated&&AnalysisRequestStoreR36.RETRY_WAIT.equals(changed.status),"typed "+category+" must remain retryable beyond the fault-attempt limit");
            try(Cursor row=db.rawQuery("SELECT first_error_code,attempts,lease_owner FROM "+table,null)) {
                need(row.moveToFirst()&&"original-first-error".equals(row.getString(0))&&row.getLong(1)==100&&"".equals(row.getString(2)),"retry keeps original error and attempts while releasing only its own lease");
            }
        }
        db.execSQL("UPDATE "+table+" SET status='RUNNING',lease_owner='same-owner'");
        AnalysisRequestStoreR36.TransitionResult stale=requests.markRetry(request,"stale-owner","wait",new IllegalStateException("DEPENDENCY"),now+1);
        need(!stale.updated&&scalar(db,"SELECT count(*) FROM "+table+" WHERE status='RUNNING' AND lease_owner='same-owner'")==1,"dependency recovery retains the original owner fence");
        AnalysisRequestStoreR36.TransitionResult fault=requests.markRetry(request,"same-owner","actual bug",new IllegalStateException("ACTUAL_BUG"),now+1);
        need(fault.updated&&AnalysisRequestStoreR36.FAILED_TERMINAL.equals(fault.status),"actual faults still enforce the unchanged attempt limit");
        System.out.println("R31_ANALYSIS_TYPED_RETRY_HOST_PASS typed_waits_not_terminal=true original_error_and_attempts_preserved=true stale_owner_refused=true actual_fault_limit_unchanged=true");
    }
    private static void analysisLeaseRecovery(SQLiteDatabase db,DatabaseCoordinator coordinator) {
        String table=DbHelper.TABLE_R36_ANALYSIS_REQUEST,base="202611041100",target="202611041101";
        long now=System.currentTimeMillis();Models.Draw previous=draw(base);previous.openTime=now-1000;insertDraw(db,previous);
        long deadline=previous.openTime+60000;String hash=V51ChecksumEngine.sha256("HOST_LEASE_RECOVERY");
        db.execSQL("INSERT INTO "+table+" VALUES(?,?,'REQUESTED',1,?,?,0,0,'',0,0,'REQUESTED','','','',?,0,'','original-first-error',?)",new Object[]{base,target,now,deadline,hash,now});
        AnalysisSingleFlightR36.Request request=new AnalysisSingleFlightR36.Request(base,target,1,now,deadline,"HOST_LEASE",hash,"");
        AnalysisRequestStoreR36 requests=new AnalysisRequestStoreR36(new DbStore(db,coordinator));
        AnalysisRequestStoreR36.ClaimResult first=requests.claimDetailed(request,"dead-owner",now);
        need(first.claimed,"real durable request claim: "+first.code);
        need(first.leaseUntil==R36AnalysisLeasePolicyR365.leaseUntil(now,deadline)&&first.leaseUntil<deadline,"claim reserves recovery time without extending fixed deadline");
        long pulseAt=now+20000;need(requests.pulse(request,"SELECTION_POLICY","dead-owner",pulseAt),"actual live owner pulses");
        long until=scalar(db,"SELECT lease_until FROM "+table+" WHERE target_issue='"+target+"'");
        need(until==R36AnalysisLeasePolicyR365.leaseUntil(pulseAt,deadline)&&deadline-until>=19000,"late pulse cannot consume the successor's remaining immutable window");
        AnalysisRequestStoreR36.ClaimResult early=requests.claimDetailed(request,"successor",until-1);
        need(!early.claimed&&early.decision==R36AnalysisLeasePolicyR365.Decision.DEFER_ACTIVE_OWNER&&early.retryAt==until,"original owner lease is respected until its real expiry");
        AnalysisRequestStoreR36.ClaimResult next=requests.claimDetailed(request,"successor",until);
        need(next.claimed&&next.decision==R36AnalysisLeasePolicyR365.Decision.CLAIM_STALE_TAKEOVER,"takeover only after expiry: "+next.code);
        need(!requests.pulse(request,"STALE_OWNER","dead-owner",until+1),"old owner cannot renew after takeover");
        try(Cursor row=db.rawQuery("SELECT deadline_at,request_hash,generation,first_error_code,attempts FROM "+table+" WHERE target_issue=?",new String[]{target})){
            need(row.moveToFirst()&&row.getLong(0)==deadline&&hash.equals(row.getString(1))&&row.getLong(2)==1&&"original-first-error".equals(row.getString(3))&&row.getLong(4)==2,"recovery preserves deadline, request/generation fence, first error and real attempt count");
        }
        need(R36AnalysisLeasePolicyR365.leaseUntil(now,now+120000)==now+AppConfig.R36_ANALYSIS_LEASE_MS,"original lease ceiling unchanged");
        need(R36AnalysisLeasePolicyR365.leaseUntil(deadline+1,deadline)==deadline,"expired request cannot receive future lease");
        System.out.println("R31_ANALYSIS_LEASE_RECOVERY_NATIVE_PASS actual_claim_and_pulse=true original_lease_respected=true immutable_deadline=true stale_owner_fenced=true lease_ceiling_unchanged=true synthetic_protocol_not_natural_quality=true");
    }
    private static void outboxAndSettlement(SQLiteDatabase db,DatabaseCoordinator coordinator)throws Exception{
        Models.Draw first=draw("202611040100"),second=draw("202611040101");
        coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.DRAW_INGEST,"R31_HOST_CONFIRMED_INGRESS"),tx->{insertDraw(tx,first);SettlementTaskStoreR36.enqueueConfirmedWithinWrite(tx,first,10020,SettlementTaskStoreR36.BACKLOG,0,"HOST_CONFIRMED_INGRESS");return 0;});
        need(scalar(db,"SELECT count(*) FROM r26_feedback")==1,"confirmed draw feedback commits before settlement");need(scalar(db,"SELECT count(*) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION)==0,"no historical prediction fabricated");
        SettlementEngine.OwnerToken firstToken=token(first.issue);
        OosAfterDrawWriterR36.LabelResult missing=coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.SETTLEMENT,"R31_HOST_MISSING_FREEZE"),tx->OosAfterDrawWriterR36.settleFrozenWithinTransaction(tx,firstToken,first));
        need(!missing.success&&"FROZEN_OBSERVATION_MISSING".equals(missing.status),"actual OOS labeler still refuses missing freeze");
        coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.DRAW_INGEST,"R31_HOST_SECOND_INGRESS"),tx->{insertDraw(tx,second);SettlementTaskStoreR36.enqueueConfirmedWithinWrite(tx,second,10020,SettlementTaskStoreR36.BACKLOG,0,"HOST_CONFIRMED_INGRESS");return 0;});
        String proof=V51ChecksumEngine.sha256("HOST_CONTROLLED_ENGINEERING_FIXTURE_NOT_NATURAL");
        db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue,base_issue,position,n1,n2,n3,selected_hit_p,created_at,commit_at,record_hash,pre_draw_frozen,model_proven,settled,history_hash,snapshot_hash,action_space_hash,game_code,history_max_issue) VALUES(?,?,1,1,2,3,0.4,9000,9000,?,1,0,0,?,?,?,?,?)",new Object[]{second.issue,first.issue,proof,proof,proof,proof,V51Repository.GAME_CODE,first.issue});
        SettlementEngine.OwnerToken secondToken=token(second.issue);OosAfterDrawWriterR36.LabelResult labelled=coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.SETTLEMENT,"R31_HOST_REAL_LABELER_FIXTURE"),tx->OosAfterDrawWriterR36.settleFrozenWithinTransaction(tx,secondToken,second));
        need(labelled.success,"controlled original frozen fixture receives actual production label");
        db.execSQL("UPDATE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" SET status='RUNNING',lease_owner='same-owner',lease_generation=2,process_boot_id='boot-2' WHERE target_issue=?",new Object[]{first.issue});
        SettlementTaskStoreR36.TaskClaim old=new SettlementTaskStoreR36.TaskClaim(first.issue,DrawHashR36.sha256V2(first),"same-owner",1,SettlementTaskStoreR36.BACKLOG,0,"HOST",1,"boot-1");
        try{coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.SETTLEMENT,"R31_HOST_STALE_TERMINAL"),tx->{SettlementTaskStoreR36.markTerminalWithinTransaction(tx,old,SettlementTaskStoreR36.COMPLETED,proof,"",10030);return 0;});throw new AssertionError("old generation terminal accepted");}catch(DatabaseCoordinator.WriteRejectedR31 expected){}
        DbStore store=new DbStore(db,coordinator);SettlementTaskStoreR36.markRetry(store,old,"database is locked");need(scalar(db,"SELECT lease_generation FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue='202611040100'")==2&&scalar(db,"SELECT count(*) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue='202611040100' AND status='RUNNING'")==1,"stale terminal and failure receipt cannot alter new generation");
        need(SettlementTaskStoreR36.pendingCountR31(store)==2,"pending comes from durable tasks, not one terminal heartbeat");
        R31FeedbackHost.verify(store,db,coordinator);
        System.out.println("R31_DRAW_OUTBOX_SETTLEMENT_HOST_PASS confirmed_draw_without_freeze_training_input=true strict_OOS_missing_refused=true existing_lease_generation_boot_CAS=true no_past_prediction_fabricated=true");
    }
    /** R31-B03-C01#A1/#A3: duplicate, late and conflicting draw receipts through the production outbox entry that
     * DbStore.saveDraw calls in the same transaction. One task per issue, conflicts never overwrite, the current
     * target outranks late history, and every known draw has a durable settlement task (no silent omission). */
    private static void ingestOrderingAndConflicts(SQLiteDatabase db,DatabaseCoordinator coordinator)throws Exception{
        String task=DbHelper.TABLE_R36_SETTLEMENT_TASK;Models.Draw current=draw("202611040205"),late1=draw("202611040200"),late2=draw("202611040201");
        coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.DRAW_INGEST,"R31_HOST_CURRENT_INGRESS"),tx->{insertDraw(tx,current);SettlementTaskStoreR36.enqueueConfirmedWithinWrite(tx,current,20000,SettlementTaskStoreR36.LIVE_CURRENT,SettlementTaskStoreR36.PRIORITY_LIVE_CURRENT,"HOST_CURRENT");return 0;});
        long feedback=scalar(db,"SELECT count(*) FROM r26_feedback");
        for(int i=0;i<5;i++)coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.DRAW_INGEST,"R31_HOST_DUPLICATE_RECEIPT"),tx->{SettlementTaskStoreR36.enqueueConfirmedWithinWrite(tx,current,20001,SettlementTaskStoreR36.LIVE_CURRENT,SettlementTaskStoreR36.PRIORITY_LIVE_CURRENT,"HOST_DUPLICATE");return 0;});
        need(scalar(db,"SELECT count(*) FROM "+task+" WHERE target_issue='202611040205'")==1,"duplicate receipts keep exactly one settlement task");
        need(scalar(db,"SELECT count(*) FROM r26_feedback")==feedback,"duplicate receipts do not repeat training feedback");
        String hash=DrawHashR36.sha256V2(current);Models.Draw conflicting=draw("202611040205");conflicting.numbers[0]=10;conflicting.numbers[9]=1;
        boolean refused=false;
        try{coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.DRAW_INGEST,"R31_HOST_CONFLICTING_SOURCE"),tx->{SettlementTaskStoreR36.enqueueConfirmedWithinWrite(tx,conflicting,20002,SettlementTaskStoreR36.LIVE_CURRENT,SettlementTaskStoreR36.PRIORITY_LIVE_CURRENT,"HOST_SECOND_SOURCE");return 0;});}
        catch(RuntimeException expected){String chain=String.valueOf(expected)+String.valueOf(expected.getCause());refused=chain.contains("R36_DRAW_HASH_CONFLICT")||chain.contains("R914_DRAW_NOT_CONFIRMED_BEFORE_TASK");}
        need(refused,"a conflicting second source is refused, never accepted");
        try(Cursor row=db.rawQuery("SELECT draw_hash FROM "+task+" WHERE target_issue='202611040205'",null)){need(row.moveToFirst()&&hash.equals(row.getString(0)),"conflict never overwrites the canonical task hash");}
        for(Models.Draw late:new Models.Draw[]{late1,late2})coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R914_DRAW_HISTORY_BATCH"),tx->{insertDraw(tx,late);SettlementTaskStoreR36.enqueueConfirmedWithinWrite(tx,late,20010,SettlementTaskStoreR36.BACKLOG,SettlementTaskStoreR36.PRIORITY_BACKLOG,"HOST_LATE_HISTORY");return 0;});
        need("202611040205".equals(SettlementTaskStoreR36.nextRunnable(new DbStore(db,coordinator),30000)),"the current target is served before late history backlog");
        need(scalar(db,"SELECT count(*) FROM draws d WHERE NOT EXISTS(SELECT 1 FROM "+task+" t WHERE t.target_issue=d.issue)")==0,"every known draw has a durable settlement task; none silently omitted");
        System.out.println("R31_INGEST_ORDERING_CONFLICT_HOST_PASS duplicates_one_task=true conflict_refused=true current_before_history=true every_draw_tasked=true");
    }
    private static CanonicalObservationWriterR36.CanonicalRecord canonicalRecord(String base,String target,String source,String decision,String lane,String auth,long commit){
        String h=V51ChecksumEngine.sha256("HOST_HISTORY|"+base),a=V51ChecksumEngine.sha256("HOST_ACTIONS|"+target),snap=V51ChecksumEngine.sha256("HOST_SNAPSHOT|"+source),record=V51ChecksumEngine.sha256("HOST_CANONICAL|"+target+"|"+source);
        return new CanonicalObservationWriterR36.CanonicalRecord(base,target,1,1,2,3,0.4,0.5,lane,decision,"NONE","",""," ".equals(auth)?"":auth,record,"host-"+source,h,a,snap,9000,9000,commit,commit+60000);
    }
    /** R31-B03-C02#A1/#A3: the first canonical freeze is authoritative. An identical replay is idempotent, a later FULL
     * (or any different) record is refused without changing the frozen row, the fixed deadline is enforced at +1 ms, a
     * drawn target never receives a post-draw commit, and an unqualified fast path cannot produce a REAL bet. */
    private static void canonicalFirstAuthoritative(SQLiteDatabase db,DatabaseCoordinator coordinator)throws Exception{
        String base="202611040300",target="202611040301",table=DbHelper.TABLE_R35_SHADOW_OBSERVATION;
        CanonicalObservationWriterR36.CanonicalRecord fast=canonicalRecord(base,target,"FAST","NO_BET","SHADOW","",9500),full=canonicalRecord(base,target,"FULL","NO_BET","SHADOW","",9800);
        fast.validate();full.validate();
        db.execSQL("INSERT INTO "+table+"(target_issue,base_issue,position,n1,n2,n3,selected_hit_p,created_at,commit_at,record_hash,pre_draw_frozen,model_proven,settled,history_hash,snapshot_hash,action_space_hash,game_code,history_max_issue) VALUES(?,?,1,1,2,3,0.4,9000,9500,?,1,0,0,?,?,?,?,?)",
                new Object[]{target,base,fast.recordHash,fast.historyHash,fast.snapshotHash,fast.actionSpaceHash,V51Repository.GAME_CODE,base});
        Constructor<DatabaseCoordinator.TransactionContext> context=DatabaseCoordinator.TransactionContext.class.getDeclaredConstructor(String.class,String.class,String.class);context.setAccessible(true);
        DatabaseCoordinator.TransactionContext tx=context.newInstance("R31_HOST_CANONICAL",target,"host-canonical");
        db.beginTransaction();try{CanonicalObservationWriterR36.WriteResult replay=CanonicalObservationWriterR36.insertWithinTransaction(db,tx,fast);need("REPLAY_IDEMPOTENT".equals(replay.status),"identical canonical replay is idempotent: "+replay.status);}finally{db.endTransaction();}
        boolean conflict=false;db.beginTransaction();try{CanonicalObservationWriterR36.insertWithinTransaction(db,tx,full);}catch(IllegalStateException expected){conflict="R36_CANONICAL_INTEGRITY_CONFLICT".equals(expected.getMessage());}finally{db.endTransaction();}
        need(conflict,"late FULL with a different record is refused");
        need(scalar(db,"SELECT count(*) FROM "+table+" WHERE target_issue='"+target+"'")==1,"at most one canonical row per target");
        try(Cursor row=db.rawQuery("SELECT record_hash,commit_at FROM "+table+" WHERE target_issue=?",new String[]{target})){need(row.moveToFirst()&&fast.recordHash.equals(row.getString(0))&&row.getLong(1)==9500,"late FULL cannot change canonical content or freeze time");}
        Method preDraw=V51Repository.class.getDeclaredMethod("assertPreDrawR36",SQLiteDatabase.class,AnalysisSingleFlightR36.Request.class,V51RuntimeResult.class,DeferredEvidenceSnapshotR36.Header.class);preDraw.setAccessible(true);
        String requestHash=V51ChecksumEngine.sha256("HOST_REQUEST|"+target);long now=System.currentTimeMillis();
        AnalysisSingleFlightR36.Request late=new AnalysisSingleFlightR36.Request(base,"202611040302",1,now-5000,now-1,"HOST_DEADLINE_PLUS_1MS",requestHash,fast.historyHash);
        String lateFailure="";db.beginTransaction();try{preDraw.invoke(null,db,late,null,null);}catch(InvocationTargetException expected){lateFailure=String.valueOf(expected.getCause().getMessage());}finally{db.endTransaction();}
        need("R974_CANONICAL_DEADLINE_EXCEEDED".equals(lateFailure),"a commit 1 ms after the fixed deadline is refused: "+lateFailure);
        insertDraw(db,draw("202611040303"));
        AnalysisSingleFlightR36.Request drawn=new AnalysisSingleFlightR36.Request("202611040302","202611040303",1,now-5000,now+60000,"HOST_TARGET_ALREADY_DRAWN",requestHash,fast.historyHash);
        String drawnFailure="";db.beginTransaction();try{preDraw.invoke(null,db,drawn,null,null);}catch(InvocationTargetException expected){drawnFailure=String.valueOf(expected.getCause().getMessage());}finally{db.endTransaction();}
        need("R36_TARGET_ALREADY_DRAWN_NO_POST_DRAW_COMMIT".equals(drawnFailure),"a drawn target never receives a post-draw prediction: "+drawnFailure);
        boolean unqualified=false;try{canonicalRecord(base,target,"FAST_REAL","BET","FAST_PATH","",9900).validate();}catch(IllegalStateException expected){unqualified="R915_CANONICAL_CONTROLLED_FIELDS_INVALID".equals(expected.getMessage());}
        need(unqualified,"an unqualified fast path cannot produce a REAL bet");
        System.out.println("R31_CANONICAL_FIRST_AUTHORITATIVE_HOST_PASS replay_idempotent=true late_full_refused=true one_row=true deadline_plus_1ms_refused=true post_draw_refused=true unqualified_real_refused=true");
    }
    /** R31-B03-C02#A2a: every known realtime opportunity reaches a terminal state under simulated timings (frozen early,
     * missed, frozen, frozen only after the draw). The production labeler decides, the production terminal writer records. */
    private static void everyOpportunityReachesTerminal(SQLiteDatabase db,DatabaseCoordinator coordinator)throws Exception{
        String task=DbHelper.TABLE_R36_SETTLEMENT_TASK;String[] targets={"202611040401","202611040402","202611040403","202611040404"};long[] freezeCommit={9000,-1,9500,10500};
        String proof=V51ChecksumEngine.sha256("HOST_TERMINAL_COVERAGE_FIXTURE_NOT_NATURAL");
        for(int i=0;i<targets.length;i++){
            Models.Draw d=draw(targets[i]);final int at=i;
            coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.DRAW_INGEST,"R31_HOST_OPPORTUNITY_INGRESS"),tx->{insertDraw(tx,d);SettlementTaskStoreR36.enqueueConfirmedWithinWrite(tx,d,10020,SettlementTaskStoreR36.BACKLOG,0,"HOST_OPPORTUNITY");return 0;});
            if(freezeCommit[i]>0)db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue,base_issue,position,n1,n2,n3,selected_hit_p,created_at,commit_at,record_hash,pre_draw_frozen,model_proven,settled,history_hash,snapshot_hash,action_space_hash,game_code,history_max_issue) VALUES(?,?,1,1,2,3,0.4,9000,?,?,1,0,0,?,?,?,?,?)",
                    new Object[]{targets[i],"20261104040"+i,freezeCommit[i],proof,proof,proof,proof,V51Repository.GAME_CODE,"20261104040"+i});
            db.execSQL("UPDATE "+task+" SET status='RUNNING',lease_owner='host-terminal',lease_generation=3,process_boot_id='boot-3' WHERE target_issue=?",new Object[]{targets[i]});
            SettlementTaskStoreR36.TaskClaim claim=new SettlementTaskStoreR36.TaskClaim(targets[i],DrawHashR36.sha256V2(d),"host-terminal",1,SettlementTaskStoreR36.BACKLOG,0,"HOST",3,"boot-3");
            SettlementEngine.OwnerToken owner=token(targets[i]);
            coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.SETTLEMENT,"R31_HOST_OPPORTUNITY_TERMINAL"),tx->{
                OosAfterDrawWriterR36.LabelResult label=OosAfterDrawWriterR36.settleFrozenWithinTransaction(tx,owner,d);
                String status=label.success?SettlementTaskStoreR36.COMPLETED:SettlementTaskStoreR36.NO_PREDRAW_CANONICAL_TERMINAL;
                SettlementTaskStoreR36.markTerminalWithinTransaction(tx,claim,status,label.success?proof:"",label.success?"":String.valueOf(label.status),20000+at);return 0;});
        }
        String list="'"+String.join("','",targets)+"'";
        need(scalar(db,"SELECT count(*) FROM "+task+" WHERE target_issue IN ("+list+") AND status NOT IN ('COMPLETED','NO_PREDRAW_CANONICAL_TERMINAL','FAILED_TERMINAL','INVALID_DRAW_TERMINAL')")==0,"100% of known opportunities reach a terminal state");
        need(scalar(db,"SELECT count(*) FROM "+task+" WHERE target_issue IN ("+list+") AND status='NO_PREDRAW_CANONICAL_TERMINAL' AND coalesce(last_error,'')=''")==0,"every exclusion carries its explicit reason");
        need(scalar(db,"SELECT count(*) FROM "+task+" WHERE target_issue='202611040402' AND status='NO_PREDRAW_CANONICAL_TERMINAL'")==1,"a missed freeze is an explicit no-predraw terminal");
        need(scalar(db,"SELECT count(*) FROM "+task+" WHERE target_issue='202611040404' AND status='COMPLETED'")==0,"a freeze committed after the draw never becomes a valid OOS completion");
        need(scalar(db,"SELECT count(*) FROM "+task+" WHERE target_issue IN ('202611040401','202611040403') AND status='COMPLETED'")==2,"pre-draw freezes settle to completed terminals");
        System.out.println("R31_OPPORTUNITY_TERMINAL_COVERAGE_HOST_PASS opportunities=4 terminal=4 explicit_exclusions=true post_draw_freeze_not_completed=true");
    }
    private static void transport(SQLiteDatabase db,DatabaseCoordinator coordinator)throws Exception{
        System.setProperty("http.nonProxyHosts","localhost|127.0.0.1");HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/chunked",exchange->{try{byte[] a="{\"ok\":".getBytes(java.nio.charset.StandardCharsets.UTF_8),b="true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,a.length+b.length);exchange.getResponseBody().write(a);exchange.getResponseBody().flush();try{Thread.sleep(80);}catch(InterruptedException e){Thread.currentThread().interrupt();}exchange.getResponseBody().write(b);}finally{exchange.close();}});
        CountDownLatch hangStarted=new CountDownLatch(1),hangRelease=new CountDownLatch(1);
        server.createContext("/hanging",exchange->{try{exchange.sendResponseHeaders(200,0);exchange.getResponseBody().write("{\"wait\":".getBytes(java.nio.charset.StandardCharsets.UTF_8));exchange.getResponseBody().flush();hangStarted.countDown();try{hangRelease.await(10,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}exchange.getResponseBody().write("true}".getBytes(java.nio.charset.StandardCharsets.UTF_8));}finally{exchange.close();}});
        server.createContext("/rate-limit",exchange->{try{byte[] body="rate limited".getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(429,body.length);exchange.getResponseBody().write(body);}finally{exchange.close();}});
        server.start();try{
            ApiClient.RawHttpResult raw=new ApiClient(new DbStore(db,coordinator)).requestGet("http://127.0.0.1:"+server.getAddress().getPort()+"/chunked");
            need(raw.ok&&raw.requestStartMonoMs>0&&raw.headersMonoMs>=raw.requestStartMonoMs&&raw.firstByteMonoMs>=raw.headersMonoMs&&raw.bodyCompleteMonoMs>=raw.firstByteMonoMs&&raw.finishedMonoMs>=raw.bodyCompleteMonoMs,"real loopback HTTP stage ordering");need(raw.bodyCompleteMonoMs-raw.firstByteMonoMs>=40,"first body read recorded before delayed remainder/EOF");
            coordinator.submitBackgroundWrite("HOST_TRACE_BARRIER",tx->{}).get(10,TimeUnit.SECONDS);
            try(Cursor row=db.rawQuery("SELECT request_mono_ms,first_byte_mono_ms,body_complete_mono_ms,parse_mono_ms,validated_mono_ms FROM r31_fetch_timing",null)){need(row.moveToFirst()&&row.getLong(0)==raw.requestStartMonoMs&&row.getLong(1)==raw.firstByteMonoMs&&row.getLong(2)==raw.bodyCompleteMonoMs,"persisted actual clocks equal RawHttpResult capture");need(row.isNull(3)&&row.isNull(4),"unexecuted parse/validation stay NULL");}
            need(scalar(db,"SELECT count(*) FROM "+SchemaRevisionR925.TABLE_FETCH_LEDGER+" WHERE status='HTTP_OK_BODY_RECEIVED' AND length(payload_hash)=64")==1,"async begin converges to exact durable HTTP payload binding");
            db.beginTransaction();try{SchemaDomainCoordinatorR926.migrateR31TransportTiming(db);SchemaDomainCoordinatorR926.migrateR31Feedback(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            need(scalar(db,"SELECT count(*) FROM r31_fetch_timing")==1,"idempotent migration preserves timing evidence");
            ApiClient unbound=new ApiClient();AtomicBoolean cancelled=new AtomicBoolean();unbound.setRequestCancellationR31(cancelled::get);AtomicReference<ApiClient.RawHttpResult> cancelledResult=new AtomicReference<>();
            Thread request=new Thread(()->cancelledResult.set(unbound.requestGet("http://127.0.0.1:"+server.getAddress().getPort()+"/hanging")),"HOST_ACTUAL_HTTP_CANCEL");request.start();
            need(hangStarted.await(5,TimeUnit.SECONDS)&&unbound.activeRequestCountR31()==1,"actual hanging HTTP owns its request permit before cancellation");cancelled.set(true);unbound.cancelInFlightR31();hangRelease.countDown();request.join(5000);
            need(!request.isAlive()&&unbound.activeRequestCountR31()==0&&cancelledResult.get()!=null&&!cancelledResult.get().ok,"cancelled actual HTTP exits then releases permit and cannot publish success");
            cancelled.set(false);ApiClient.RawHttpResult retry=unbound.requestGet("http://127.0.0.1:"+server.getAddress().getPort()+"/chunked");need(retry.ok&&unbound.activeRequestCountR31()==0,"same client restarts only after actual request exit");
            ApiClient.RawHttpResult rateLimited=unbound.requestGet("http://127.0.0.1:"+server.getAddress().getPort()+"/rate-limit");need(!rateLimited.ok&&rateLimited.httpCode==429&&rateLimited.bodyCompleteMonoMs>0,"actual HTTP429 retains response stages as failure");
            FetchBackoffR30 backoff=new FetchBackoffR30(()->500000);for(int i=0;i<8;i++){need(backoff.tryBegin(),"singleflight available");need(!backoff.tryBegin(),"second in-flight rejected without request queue");backoff.end(false);need(backoff.nextDelayMs(false,Long.MIN_VALUE)<=18000,"HTTP failure backoff remains bounded");}need(backoff.tryBegin(),"success recovery admission");backoff.end(true);need(backoff.nextDelayMs(false,Long.MIN_VALUE)==1000,"success restores one-second target");
            System.out.println("R31_TRANSPORT_CANCEL_429_HOST_PASS real_hanging_socket_cancel_exit=true permit_released_only_after_exit=true client_restart=true actual_429_failure=true existing_singleflight_bounded_backoff=true scope=HOST_NOT_ANDROID_LIFECYCLE");
            System.out.println("R31_TRANSPORT_NATIVE_HTTP_HOST_PASS actual_socket_body_stages=true EOF_before_close_capture=true raw_equals_persisted=true missing_parse_validation_NULL=true durable_request_payload_binding=true additive_migration_preserves_rows=true scope=LOOPBACK_NOT_PROVIDER_OR_DEVICE_SLO");
        }finally{hangRelease.countDown();server.stop(0);}
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");File path=new File(args[0]);ThreadPoolExecutor background=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(32),r->{Thread t=new Thread(r,"HOST_EXISTING_WRITER_QUEUE");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+path.getPath())){
            SQLiteDatabase db=new SQLiteDatabase(connection);schema(db);Field uf=Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);DatabaseCoordinator coordinator=(DatabaseCoordinator)((Unsafe)uf.get(null)).allocateInstance(DatabaseCoordinator.class);Field lf=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lf.setAccessible(true);set(coordinator,"writeLock",(ReentrantLock)lf.get(null));set(coordinator,"helper",new DbHelper(db));set(coordinator,"app",new android.app.Application(path));set(coordinator,"criticalSchemaReady",new AtomicBoolean(true));set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());set(coordinator,"backgroundWriter",background);
            outboxAndSettlement(db,coordinator);ingestOrderingAndConflicts(db,coordinator);canonicalFirstAuthoritative(db,coordinator);everyOpportunityReachesTerminal(db,coordinator);analysisDependencyRetry(db,coordinator);analysisLeaseRecovery(db,coordinator);transport(db,coordinator);R31ServiceHeartbeatHost.verify(path);
            System.out.println("R31_SERVICE_PROTOCOL_HOST_PASS scope=actual_production_sources_native_SQLite_HTTP_loopback_adapters_NOT_ANDROID_NOT_DEVICE_NOT_MODEL_QUALITY");
        }finally{background.shutdownNow();}
    }
}
