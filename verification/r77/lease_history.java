package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/** Actual service checkpoint + durable store CAS; faults only at the Android SQLite adapter.
 * Production cancellation token, coordinator and SQL callbacks are not replaced. */
public final class lease_history {
    static int checks;
    static void check(boolean v,String name){if(!v)throw new AssertionError(name);checks++;System.out.println("PASS|"+name);}
    static void set(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
    static final class ObservedDb extends SQLiteDatabase {
        int pulseWrites,transitionWrites,wakeFaults; boolean pulseFault,bindFault,retryFault,thrownRetryFault;
        ObservedDb(String bridge,String file)throws Exception{super(bridge,file);}
        @Override public synchronized int update(String table,ContentValues values,String where,String[] args) {
            if("v57_analysis_request_r36".equals(table)&&values.containsKey("status")) {
                transitionWrites++;
                if(retryFault){retryFault=false;throw new IllegalStateException("INJECTED_EXIT_RELEASE_SQL_FAILURE");}
                if(thrownRetryFault){thrownRetryFault=false;throw new RuntimeResourceAuditR927.BudgetExceeded("INJECTED_EXIT_RELEASE_THROWN");}
            }
            if("v57_analysis_request_r36".equals(table)&&values.containsKey("lease_until")){
                pulseWrites++;
                if(pulseFault){pulseFault=false;throw new IllegalStateException("INJECTED_PULSE_SQL_FAILURE");}
            }
            return super.update(table,values,where,args);
        }
        @Override public synchronized Cursor rawQuery(String sql,String[] args){
            if(wakeFaults>0&&sql.contains(" FROM draws")){wakeFaults--;throw new IllegalStateException("INJECTED_WAKE_READ_FAILURE");}
            if(bindFault&&sql.startsWith("SELECT history_hash FROM v57_analysis_request_r36")){
                bindFault=false;throw new IllegalStateException("INJECTED_BIND_SQL_FAILURE");}
            return super.rawQuery(sql,args);
        }
    }
    static Throwable cause(Throwable e){while(e.getCause()!=null)e=e.getCause();return e;}
    static void expiredPulse(AnalysisSingleFlightR36.Lease lease)throws Exception{
        set(lease.token,"lastDurablePulseWallAt",System.currentTimeMillis()-10001L);
    }
    static void renew(Method m,AnalysisRequestStoreR36 requests,AnalysisSingleFlightR36.Request req,
            AnalysisSingleFlightR36.Lease lease,String stage)throws Throwable{
        try{m.invoke(null,requests,req,lease,stage);}catch(InvocationTargetException e){throw e.getCause();}
    }
    static long scalar(SQLiteDatabase db,String sql){Cursor c=db.rawQuery(sql,null);try{if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}finally{c.close();}}
    static void resetRunning(ObservedDb db,AnalysisSingleFlightR36.Request req,String owner) {
        db.execSQL("UPDATE v57_analysis_request_r36 SET status='RUNNING',lease_owner=?,lease_until=?,attempts=1,next_attempt_at=0,history_hash='',error_code='',first_error_code='' WHERE target_issue=?",
                new Object[]{owner,System.currentTimeMillis()+45000L,req.targetIssue});
    }
    static void recovery(DbStore store,ObservedDb db,AnalysisSingleFlightR36.Request req)throws Throwable {
        AutoFetchService service=(AutoFetchService)HostProductR974.U.allocateInstance(AutoFetchService.class);
        Handler handler=new Handler(Looper.getMainLooper());ConcurrentHashMap<Object,Object> pending=new ConcurrentHashMap<Object,Object>();
        set(service,"running",true);set(service,"store",store);set(service,"analysisHandler",handler);set(service,"exitedAnalysisReleasesR77",pending);
        Method schedule=AutoFetchService.class.getDeclaredMethod("scheduleExitedAnalysisReleaseR77",AnalysisSingleFlightR36.Lease.class,String.class);schedule.setAccessible(true);
        Method cancel=AutoFetchService.class.getDeclaredMethod("cancelExitedAnalysisReleasesR77");cancel.setAccessible(true);
        Method retryable=AutoFetchService.class.getDeclaredMethod("retryableExitTransitionR77",AnalysisRequestStoreR36.TransitionResult.class);retryable.setAccessible(true);
        check(Boolean.TRUE.equals(retryable.invoke(null,new Object[]{null})),"NULL_TRANSITION_REQUIRES_RELEASE_RECOVERY");
        check(Boolean.TRUE.equals(retryable.invoke(null,AnalysisRequestStoreR36.TransitionResult.lost("TRANSITION_NULL"))),"NORMALIZED_NULL_TRANSITION_REQUIRES_RELEASE_RECOVERY");
        AnalysisSingleFlightR36.Lease exited=AnalysisSingleFlightR36.shared().submit(req).lease;
        resetRunning(db,req,exited.ownerId);schedule.invoke(service,exited,"ORIGINAL_PULSE_DB_FAILURE");
        check(handler.pending()==0&&pending.isEmpty(),"LIVE_OWNER_CANNOT_SCHEDULE_EXIT_RELEASE");
        AnalysisSingleFlightR36.shared().releaseAndTakeNext(exited);
        schedule.invoke(service,exited,"ORIGINAL_PULSE_DB_FAILURE");schedule.invoke(service,exited,"DUPLICATE");
        check(handler.pending()==1&&pending.size()==1,"ONE_PENDING_RELEASE_PER_EXACT_OWNER");
        db.retryFault=true;handler.runNext();
        check(handler.pending()==1&&pending.size()==1&&scalar(db,"SELECT count(*) FROM v57_analysis_request_r36 WHERE status='RUNNING' AND attempts=1")==1,"FAILED_RELEASE_REPOSTS_WITHOUT_SPENDING_ATTEMPT");
        handler.runNext();
        check(handler.pending()==1&&scalar(db,"SELECT count(*) FROM v57_analysis_request_r36 WHERE status='RETRY_WAIT' AND lease_until=0 AND lease_owner='' AND attempts=1")==1,"EXIT_RELEASE_RECOVERS_WITHOUT_WAITING_45S_LEASE");
        check(scalar(db,"SELECT next_attempt_at FROM v57_analysis_request_r36")<System.currentTimeMillis()+2000L,"RELEASE_USES_EXISTING_1S_RETRY_POLICY");
        int writes=db.transitionWrites;db.wakeFaults=2;handler.runNext();
        check(db.wakeFaults==0&&handler.pending()==1&&pending.size()==1,"ACTUAL_WAKE_EXCEPTION_REPOSTS_ONLY_RELEASED_TICKET");
        check(db.transitionWrites==writes,"WAKE_FAILURE_DOES_NOT_REPEAT_DURABLE_RELEASE");
        db.wakeFaults=0;while(handler.pending()>0)handler.runNext();
        check(pending.isEmpty()&&db.transitionWrites==writes,"WAKE_RECOVERY_CLEANS_TICKET_WITHOUT_SECOND_TRANSITION");
        resetRunning(db,req,exited.ownerId);schedule.invoke(service,exited,"THROWN_RELEASE");db.thrownRetryFault=true;handler.runNext();
        check(handler.pending()==1&&pending.size()==1,"THROWN_TRANSITION_REPOSTS_WITH_ORIGINAL_IDENTITY");
        handler.runNext();handler.runNext();check(pending.isEmpty(),"THROWN_TRANSITION_RECOVERS_ON_EXISTING_HANDLER");
        resetRunning(db,req,"new-owner");schedule.invoke(service,exited,"STALE_RELEASE");writes=db.transitionWrites;handler.runNext();
        check(pending.isEmpty()&&db.transitionWrites==writes&&scalar(db,"SELECT count(*) FROM v57_analysis_request_r36 WHERE lease_owner='new-owner' AND status='RUNNING'")==1,"NEW_OWNER_CAS_MISMATCH_STOPS_OLD_RELEASE");
        resetRunning(db,req,exited.ownerId);schedule.invoke(service,exited,"STOPPED");set(service,"userRequestedStopV5",true);writes=db.transitionWrites;handler.runNext();
        check(pending.isEmpty()&&db.transitionWrites==writes,"USER_STOP_PREVENTS_RELEASE_CALLBACK_WRITE");
        set(service,"userRequestedStopV5",false);schedule.invoke(service,exited,"CANCEL_ALL");cancel.invoke(service);
        check(pending.isEmpty()&&handler.pending()==0,"STOP_DESTROY_CANCEL_PENDING_RELEASE_CALLBACKS");
        db.execSQL("INSERT OR REPLACE INTO draws(issue,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,1,2,3,4,5,6,7,8,9,10)",new Object[]{req.targetIssue,System.currentTimeMillis()});
        resetRunning(db,req,exited.ownerId);schedule.invoke(service,exited,"ALREADY_DRAWN_RELEASE");handler.runNext();
        check(pending.isEmpty()&&handler.pending()==0&&scalar(db,"SELECT count(*) FROM v57_analysis_request_r36 WHERE status='EXPIRED' AND lease_until=0")==1,"DRAWN_TARGET_EXPIRES_WITHOUT_REOPENING_EARLY_DEADLINE");
        db.execSQL("DELETE FROM draws WHERE issue=?",new Object[]{req.targetIssue});
        resetRunning(db,req,exited.ownerId);exited.token.requestCancelOnce("NEWER_TARGET:202610131003");schedule.invoke(service,exited,"SUPERSEDED_RELEASE");handler.runNext();
        check(pending.isEmpty()&&handler.pending()==0&&scalar(db,"SELECT count(*) FROM v57_analysis_request_r36 WHERE status='EXPIRED' AND lease_until=0")==1,"SUPERSEDED_OWNER_EXPIRES_WITHOUT_RETRY");
        set(exited.token,"cancelReason","");resetRunning(db,req,exited.ownerId);set(req,"deadlineMonoAt",TimeSourceR811.monoNowMs()-1L);schedule.invoke(service,exited,"EXPIRED_RELEASE");handler.runNext();
        check(pending.isEmpty()&&handler.pending()==0&&scalar(db,"SELECT count(*) FROM v57_analysis_request_r36 WHERE status='EXPIRED' AND lease_until=0")==1,"ORIGINAL_MONOTONIC_DEADLINE_EXPIRES_WITHOUT_REQUEUE");
    }
    public static void main(String[] args)throws Throwable {
        RealtimePressureGateR811.resetForTests();
        try(ObservedDb db=new ObservedDb(args[0],args[1])){
            DbStore store=HostProductR974.store(db);AnalysisRequestStoreR36 requests=new AnalysisRequestStoreR36(store);
            long now=System.currentTimeMillis();String hash=V51ChecksumEngine.sha256("R77_LEASE_FIXTURE");
            AnalysisSingleFlightR36.Request req=new AnalysisSingleFlightR36.Request("202610131001","202610131002",9001L,now,now+60000L,"R77_TEST",hash,"");
            AnalysisSingleFlightR36.Lease lease=AnalysisSingleFlightR36.shared().submit(req).lease;
            db.execSQL("DELETE FROM v57_analysis_request_r36");
            db.execSQL("INSERT INTO v57_analysis_request_r36(target_issue,base_issue,status,generation,request_at,deadline_at,lease_owner,lease_until,request_hash) VALUES(?,?,?,?,?,?,?,?,?)",
                    new Object[]{req.targetIssue,req.baseIssue,"RUNNING",req.generation,now,req.deadlineAt,lease.ownerId,now+60000L,hash});
            Method method=AutoFetchService.class.getDeclaredMethod("renewAnalysisLeaseOrThrowR365",AnalysisRequestStoreR36.class,AnalysisSingleFlightR36.Request.class,AnalysisSingleFlightR36.Lease.class,String.class);method.setAccessible(true);
            try{
                lease.token.claimDurablePulse(now,true);
                renew(method,requests,req,lease,"HISTORY_PREPARE_ENTER");
                renew(method,requests,req,lease,"HISTORY_PREPARE_DONE");
                check(db.pulseWrites==0,"FRESH_CLAIM_HISTORY_CHECKPOINTS_DO_NOT_REWRITE_LEASE");
                check("HISTORY_PREPARE_DONE".equals(lease.token.stage()),"IN_MEMORY_PROGRESS_STILL_ADVANCES");
                expiredPulse(lease);renew(method,requests,req,lease,"HISTORY_BOOTSTRAP_SAVED");
                check(db.pulseWrites==1,"ELAPSED_10S_PERFORMS_REAL_DURABLE_PULSE");
                check(scalar(db,"SELECT lease_until FROM v57_analysis_request_r36")>=now+AppConfig.R36_ANALYSIS_LEASE_MS,"PULSE_RENEWS_ACTUAL_SQL_ROW");
                check(lease.token.claimDurablePulse(System.currentTimeMillis(),true),"FINAL_COMMIT_CAN_FORCE_WITHIN_COALESCING_WINDOW");
                check(requests.pulse(req,"BEFORE_CANONICAL_COMMIT",lease.ownerId,System.currentTimeMillis()),"FINAL_FORCED_PULSE_CHECKS_REAL_OWNER_CAS");
                db.execSQL("UPDATE v57_analysis_request_r36 SET lease_owner='different-owner'");expiredPulse(lease);
                try{renew(method,requests,req,lease,"HISTORY_PREPARE_ENTER");throw new AssertionError("stale owner accepted");}
                catch(IllegalStateException e){check(e.getMessage().equals("R366_DURABLE_LEASE_FENCE_LOST:HISTORY_PREPARE_ENTER"),"WRONG_OWNER_STILL_FAILS_CLOSED");}
                db.execSQL("UPDATE v57_analysis_request_r36 SET lease_owner=?",new Object[]{lease.ownerId});
                db.pulseFault=true;
                try{requests.pulse(req,"PULSE_SQL_FAILURE",lease.ownerId,System.currentTimeMillis());throw new AssertionError("SQL failure suppressed");}
                catch(IllegalStateException e){check(e.getMessage().contains("F01_WRITE_FAILED")&&cause(e).getMessage().equals("INJECTED_PULSE_SQL_FAILURE"),"PULSE_DATABASE_FAILURE_PRESERVES_ITS_CAUSE");}
                check(requests.pulse(req,"PULSE_RECOVERED",lease.ownerId,System.currentTimeMillis()),"PULSE_RECOVERS_WITH_SAME_VALID_OWNER");
                String history=V51ChecksumEngine.sha256("R77_HISTORY");db.bindFault=true;
                try{requests.bindHistoryHash(req,history,lease.ownerId);throw new AssertionError("SQL bind failure suppressed");}
                catch(IllegalStateException e){check(e.getMessage().contains("F01_WRITE_FAILED")&&cause(e).getMessage().equals("INJECTED_BIND_SQL_FAILURE"),"HISTORY_BIND_DATABASE_FAILURE_PRESERVES_ITS_CAUSE");}
                AnalysisSingleFlightR36.Request bound=requests.bindHistoryHash(req,history,lease.ownerId);
                check(history.equals(bound.historyHash),"HISTORY_BIND_RECOVERS_WITH_EXACT_HASH");
                check(history.equals(requests.bindHistoryHash(req,history,lease.ownerId).historyHash),"SAME_HISTORY_BIND_IS_IDEMPOTENT");
                try{requests.bindHistoryHash(req,V51ChecksumEngine.sha256("OTHER_HISTORY"),lease.ownerId);throw new AssertionError("history changed");}
                catch(IllegalStateException e){check(cause(e).getMessage().equals("R36_HISTORY_HASH_CONFLICT"),"CONFLICTING_HISTORY_STILL_REJECTED");}
                try{requests.bindHistoryHash(req,history,"different-owner");throw new AssertionError("wrong bind owner accepted");}
                catch(IllegalStateException e){check(e.getMessage().equals("R36_HISTORY_BIND_FAILED"),"WRONG_HISTORY_OWNER_STILL_REJECTED");}
                int writes=db.pulseWrites;lease.token.requestCancelOnce("R77_TEST_CANCEL");
                try{renew(method,requests,req,lease,"HISTORY_PREPARE_ENTER");throw new AssertionError("cancellation suppressed");}
                catch(AnalysisSingleFlightR36.CancelledBeforeCommitException e){check(db.pulseWrites==writes,"COALESCING_NEVER_SKIPS_CANCELLATION_CHECK");}
                check(scalar(db,"SELECT count(1) FROM v57_analysis_request_r36 WHERE status='CANONICAL_COMMITTED'")==0,"LEASE_TEST_NEVER_MANUFACTURES_CANONICAL_COMMIT");
            }finally{AnalysisSingleFlightR36.shared().releaseAndTakeNext(lease);}
            recovery(store,db,req);
        }
        String json="{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_SERVICE_CHECKPOINT_AND_STORE_CAS_WITH_REAL_SQLITE_ANDROID_ADAPTER_FAULTS\"}\n";
        Files.writeString(Path.of(args[2]),json);System.out.println(json);
    }
}
