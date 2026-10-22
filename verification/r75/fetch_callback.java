package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/** Real service callback, coordinator, SQLite and monotonic budget; collaborator work is injected.
 * This proves dispatch/budget isolation and durable fairness, not actual trainer throughput. */
public final class fetch_callback {
    static int checks;
    static final ArrayList<String> calls=new ArrayList<String>();
    static long continuityRemaining, parentRemaining;
    static boolean pending=true, retentionYield=false, retentionError=false;
    static long retentionMillis=650L,reclaimMillis=1500L;
    static void check(boolean value,String name) {
        if(!value)throw new AssertionError(name+"|calls="+calls);
        checks++;System.out.println("PASS|"+name);
    }
    static void set(Object object,String name,Object value)throws Exception {
        Field f=AutoFetchService.class.getDeclaredField(name);f.setAccessible(true);f.set(object,value);
    }
    static void pause(long ms) { try{Thread.sleep(ms);}catch(InterruptedException e){Thread.currentThread().interrupt();} }
    static Handler callback(DbStore store)throws Exception {
        AutoFetchService service=(AutoFetchService)HostProductR974.U.allocateInstance(AutoFetchService.class);
        Handler handler=new Handler(Looper.getMainLooper());
        set(service,"running",true);set(service,"store",store);set(service,"researchHandlerR908",handler);
        set(service,"researchEvolutionR811",HostProductR974.U.allocateInstance(AutonomousResearchEvolutionEngine.class));
        set(service,"researchTickPostedR908",new AtomicBoolean());
        set(service,"heartbeatStoreV8",new ServiceHeartbeatStore(null));
        Method post=AutoFetchService.class.getDeclaredMethod("postAutonomousResearchTickR908",String.class);post.setAccessible(true);
        set(service,"researchContinuationR70",(Runnable)()->{try{post.invoke(service,"FIXTURE_CONTINUATION");}catch(Exception e){throw new RuntimeException(e);}});
        calls.clear();post.invoke(service,"FIXTURE_REQUEST");handler.runNext();
        check(!ResearchWorkBudgetR927.isActive(),"PARENT_SCOPE_RELEASED");
        return handler;
    }
    static void nextTurn(DbStore store,String turn){store.setState("r75_research_callback_turn",turn);}
    public static void main(String[] args)throws Exception {
        RealtimePressureGateR811.resetForTests();
        try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])) {
            db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
            DbStore store=HostProductR974.store(db);
            Handler first=callback(store);
            check(calls.size()==1&&calls.get(0).equals("CONTINUITY")&&continuityRemaining>=950L,
                    "COLD_CONTINUITY_GETS_ITS_FULL_SLICE_DESPITE_DUE_SLOW_CLEANUP");
            check(parentRemaining>1000L&&parentRemaining<=2000L&&continuityRemaining<=1000L,
                    "ORIGINAL_2000MS_PARENT_AND_1000MS_CHILD_LIMITS_RETAINED");
            check(first.pending()==1,"PENDING_RESEARCH_KEEPS_ONE_CONTINUATION");
            check("1".equals(store.getState("r75_research_callback_turn","")),"NEXT_RETENTION_TURN_DURABLE");
        }
        // New DB connection and service object: slot state cannot be process-local.
        try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])) {
            DbStore store=HostProductR974.store(db);
            pending=false;
            Handler retention=callback(store);
            check(calls.size()==1&&calls.get(0).equals("RETENTION"),"RESTART_RUNS_RETENTION_IN_SEPARATE_CALLBACK");
            check(retention.pending()==1,"MAINTENANCE_RETURN_SCHEDULES_NEXT_RESEARCH_EVEN_WITHOUT_PENDING_LANES");
            retention.runNext();retention.runNext();
            check(calls.size()==2&&calls.get(1).equals("CONTINUITY"),"MAINTENANCE_CONTINUATION_REACHES_RESEARCH");
            check(retention.pending()==0,"IDLE_RESEARCH_STOPS_RETRY_CHAIN");
            callback(store);
            check(calls.size()==1&&calls.get(0).equals("RECLAIM"),"RECLAIM_FAIR_WITHOUT_ALL_LANES_COMPLETING");
            check("0".equals(store.getState("r75_research_callback_turn","")),"FOUR_SLOT_WHEEL_WRAPS_DURABLY");
            retentionMillis=0;reclaimMillis=0;pending=true;
            for(int i=0;i<8;i++) {
                store.setState("r979_retention_due_at","0");store.setState("r943_storage_reclaim_at","0");
                callback(store);
                check(calls.size()==1&&calls.get(0).equals(new String[]{"CONTINUITY","RETENTION","CONTINUITY","RECLAIM"}[i%4]),
                        "PERSISTENT_FAIR_TURN_"+i);
            }
            nextTurn(store,"1");store.setState("r979_retention_due_at",Long.toString(System.currentTimeMillis()+60000L));
            callback(store);check(calls.size()==1&&calls.get(0).equals("CONTINUITY"),"RETENTION_NOT_DUE_LENDS_TURN_TO_RESEARCH");
            nextTurn(store,"3");store.setState("r943_storage_reclaim_at",Long.toString(System.currentTimeMillis()));
            callback(store);check(calls.size()==1&&calls.get(0).equals("CONTINUITY"),"RECLAIM_NOT_DUE_LENDS_TURN_TO_RESEARCH");
            nextTurn(store,"1");store.setState("r979_retention_due_at","0");retentionYield=true;
            callback(store);check("2".equals(store.getState("r75_research_callback_turn","")),"RETENTION_YIELD_CANNOT_PIN_WHEEL");
            callback(store);check(calls.size()==1&&calls.get(0).equals("CONTINUITY")&&continuityRemaining>=950L,
                    "RESEARCH_FULL_SLICE_AFTER_YIELDED_CLEANUP");
            retentionYield=false;
            nextTurn(store,"1");store.setState("r979_retention_due_at","0");retentionError=true;
            Handler failed=callback(store);
            check(failed.pending()==1&&"2".equals(store.getState("r75_research_callback_turn","")),
                    "MAINTENANCE_EXCEPTION_PRESERVES_NEXT_RESEARCH_CALLBACK");
            check(store.getState("r908_research_tick_status","").startsWith("ERROR:"),"MAINTENANCE_EXCEPTION_REMAINS_VISIBLE");
            retentionError=false;
            callback(store);check(calls.size()==1&&calls.get(0).equals("CONTINUITY"),"RESEARCH_RUNS_AFTER_MAINTENANCE_EXCEPTION");
            nextTurn(store,"0");RealtimePressureGateR811.beginAnalysis();
            try{callback(store);check(calls.isEmpty(),"P0_PREEMPTS_BEFORE_ANY_OWNER");}
            finally{RealtimePressureGateR811.endAnalysis();}
            check("0".equals(store.getState("r75_research_callback_turn","")),"P0_YIELD_DOES_NOT_SPEND_RESERVED_TURN");
        }
        String result="{\"status\":\"PASS\",\"checks\":"+checks+
                ",\"scope\":\"ACTUAL_SERVICE_CALLBACK_REAL_SQLITE_INJECTED_OWNER_WORK_NOT_DEVICE\"}\n";
        Files.writeString(Path.of(args[2]),result);System.out.println(result);
    }
}

// The test doubles expose only the collaborators reached by the real service callback.
// The service, DB coordinator, SQLite adapter and ResearchWorkBudget are never replaced.
class DataRetentionGovernorR900 {
    public static String trim(DbStore s){return trim(s,150L);}
    public static String trim(DbStore s,long budget){
        fetch_callback.calls.add("RETENTION");fetch_callback.pause(fetch_callback.retentionMillis);
        if(fetch_callback.retentionYield)throw new ResearchWorkBudgetR927.Yield("INJECTED_RETRY");
        if(fetch_callback.retentionError)throw new IllegalStateException("INJECTED_STORAGE_ERROR");
        ResearchWorkBudgetR927.checkpoint();return "RETENTION_NO_ELIGIBLE_ROWS";
    }
}
class StorageReclaimR943 {
    public static void quiet(DbStore s){fetch_callback.calls.add("RECLAIM");fetch_callback.pause(fetch_callback.reclaimMillis);ResearchWorkBudgetR927.checkpoint();}
}
class ResearchRuntimeSqlGateR911 {
    public static final class Report {public boolean passed=true;public String detail="FIXTURE_SCHEMA_READY";}
    public static Report verifyAndPublish(DbStore s){ResearchWorkBudgetR927.checkpoint();fetch_callback.parentRemaining=ResearchWorkBudgetR927.remainingMs();return new Report();}
}
class ResearchCaptureQueueR946 { public static String recover(DbStore s){return "";} }
class ResearchLearningContinuityR912 {
    public static final class Result {public String status="PERIOD_PROGRESS_FIXTURE";public boolean success=true;public int completedLanes=1;}
    public static Result pulse(DbStore s,long budget) {
        ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(budget);
        try{ResearchWorkBudgetR927.checkpoint();fetch_callback.calls.add("CONTINUITY");fetch_callback.continuityRemaining=ResearchWorkBudgetR927.remainingMs();return new Result();}
        finally{ResearchWorkBudgetR927.leave(scope);}
    }
    public static boolean needsContinuation(DbStore s){return fetch_callback.pending;}
    public static long nextContinuationDelayMs(DbStore s){return 250L;}
}
class ServiceHeartbeatStore {
    public ServiceHeartbeatStore(android.content.Context context){}
    public void markLearningAttempt(String stage,String detail){}
    public void markLearningPump(String stage,String detail){}
}
