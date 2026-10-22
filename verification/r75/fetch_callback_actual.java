package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/** Actual release Service, SQL gate, continuity, retention, reclaim and heartbeat.
 * Android adapters use real SQLite and monotonic time. Work is observed/faulted at
 * the SQL boundary; no production owner class is replaced. Empty history proves
 * dispatch and budgets, not trainer throughput or on-device timing. */
public final class fetch_callback_actual {
    static int checks;
    static final ArrayList<String> calls=new ArrayList<String>();
    static long continuityRemaining;
    static void check(boolean value,String name) {
        if(!value)throw new AssertionError(name+"|calls="+calls);
        checks++;System.out.println("PASS|"+name);
    }
    static void set(Object object,Class<?> owner,String name,Object value)throws Exception {
        Field f=owner.getDeclaredField(name);f.setAccessible(true);f.set(object,value);
    }
    static final class HostContext extends android.app.Application {
        final File files;final Map<String,Object> values=new HashMap<String,Object>();
        final SharedPreferences prefs;
        HostContext(Path path)throws Exception {
            super();files=Files.createDirectories(path).toFile();
            SharedPreferences.Editor editor=(SharedPreferences.Editor)Proxy.newProxyInstance(
                SharedPreferences.Editor.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(p,m,a)->{
                    String n=m.getName();if(n.startsWith("put")){values.put((String)a[0],a[1]);return p;}
                    if(n.equals("remove")){values.remove(a[0]);return p;}
                    if(n.equals("clear")){values.clear();return p;}
                    if(n.equals("commit"))return true;if(n.equals("apply"))return null;
                    throw new UnsupportedOperationException(n);
                });
            prefs=(SharedPreferences)Proxy.newProxyInstance(SharedPreferences.class.getClassLoader(),
                new Class[]{SharedPreferences.class},(p,m,a)->{
                    String n=m.getName();if(n.equals("edit"))return editor;if(n.equals("getAll"))return new HashMap<String,Object>(values);
                    if(n.equals("contains"))return values.containsKey(a[0]);
                    if(n.startsWith("get"))return values.getOrDefault(a[0],a[1]);
                    if(n.startsWith("register")||n.startsWith("unregister"))return null;
                    throw new UnsupportedOperationException(n);
                });
        }
        @Override public Context getApplicationContext(){return this;}
        @Override public File getFilesDir(){return files;}
        @Override public File getCacheDir(){return files;}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){return prefs;}
    }
    static final class ObservedDb extends SQLiteDatabase {
        boolean retentionBudgetFault,claimWriteFault;
        boolean retentionYieldObserved;
        long retentionBudgetBefore;
        final ArrayList<String> observed=new ArrayList<String>();
        ObservedDb(String bridge,String path)throws Exception{super(bridge,path);}
        void reset(){calls.clear();observed.clear();continuityRemaining=0;}
        void probe(String sql,Object[] args) {
            String owner="";
            for(StackTraceElement frame:Thread.currentThread().getStackTrace()) {
                String type=frame.getClassName(),method=frame.getMethodName();
                if(type.equals("com.ethk3.novaonepro.ResearchLearningContinuityR912")&&method.equals("pulseWithinR927"))owner="CONTINUITY";
                if(type.equals("com.ethk3.novaonepro.DataRetentionGovernorR900")&&method.equals("trimOwnedCP12"))owner="RETENTION";
                if(type.equals("com.ethk3.novaonepro.StorageReclaimR943")&&method.equals("quiet"))owner="RECLAIM";
            }
            if(!owner.isEmpty()&&!observed.contains(owner)) {
                observed.add(owner);calls.add(owner);
                if(owner.equals("CONTINUITY"))continuityRemaining=ResearchWorkBudgetR927.remainingMs();
                if(owner.equals("RETENTION")&&retentionBudgetFault) {
                    retentionBudgetFault=false;
                    retentionBudgetBefore=ResearchWorkBudgetR927.remainingMs();
                    try{Thread.sleep(800L);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                    // The real 750ms child expires; no fake clock or owner result.
                    try{ResearchWorkBudgetR927.checkpoint();}
                    catch(ResearchWorkBudgetR927.Yield expired){retentionYieldObserved=true;throw expired;}
                }
            }
            if(claimWriteFault&&sql.startsWith("INSERT OR REPLACE INTO app_state")&&args!=null
                    &&args.length>0&&"r75_research_callback_turn".equals(args[0])) {
                claimWriteFault=false;throw new IllegalStateException("INJECTED_CALLBACK_TURN_WRITE_FAILURE");
            }
        }
        @Override public synchronized Cursor rawQuery(String sql,String[] args){probe(sql,args);return super.rawQuery(sql,args);}
        @Override public synchronized Cursor rawQuery(String sql,String[] args,android.os.CancellationSignal signal){probe(sql,args);return super.rawQuery(sql,args,signal);}
        @Override public synchronized void execSQL(String sql){probe(sql,null);super.execSQL(sql);}
        @Override public synchronized void execSQL(String sql,Object[] args){probe(sql,args);super.execSQL(sql,args);}
    }
    static DbStore store(ObservedDb db,HostContext context)throws Exception {
        DbStore store=HostProductR974.store(db);
        set(store.coordinatorR36(),DatabaseCoordinator.class,"app",context);return store;
    }
    static Handler callback(DbStore store,ObservedDb db,HostContext context)throws Exception {
        AutoFetchService service=(AutoFetchService)HostProductR974.U.allocateInstance(AutoFetchService.class);
        Handler handler=new Handler(Looper.getMainLooper());
        set(service,AutoFetchService.class,"running",true);set(service,AutoFetchService.class,"store",store);
        set(service,AutoFetchService.class,"researchHandlerR908",handler);
        set(service,AutoFetchService.class,"researchEvolutionR811",HostProductR974.U.allocateInstance(AutonomousResearchEvolutionEngine.class));
        set(service,AutoFetchService.class,"researchTickPostedR908",new AtomicBoolean());
        set(service,AutoFetchService.class,"heartbeatStoreV8",new ServiceHeartbeatStore(context));
        Method post=AutoFetchService.class.getDeclaredMethod("postAutonomousResearchTickR908",String.class);post.setAccessible(true);
        set(service,AutoFetchService.class,"researchContinuationR70",(Runnable)()->{try{db.reset();post.invoke(service,"ACTUAL_CONTINUATION");}catch(Exception e){throw new RuntimeException(e);}});
        db.reset();post.invoke(service,"ACTUAL_CALLBACK_FIXTURE");handler.runNext();
        check(!ResearchWorkBudgetR927.isActive(),"PARENT_SCOPE_RELEASED");return handler;
    }
    static boolean only(String owner){return calls.size()==1&&calls.get(0).equals(owner);}
    static void turn(DbStore store,String value){store.setState("r75_research_callback_turn",value);}
    static void continuity(DbStore store,String label){
        check(only("CONTINUITY")&&continuityRemaining>=950L&&continuityRemaining<=1000L,
                label+"_FULL_CONTINUITY_SLICE");
        check("WAIT_HISTORY".equals(store.getState("r908_research_tick_status","")),label+"_ACTUAL_EMPTY_HISTORY_RESULT");
    }
    public static void main(String[] args)throws Exception {
        RealtimePressureGateR811.resetForTests();HostContext context=new HostContext(Path.of(args[3]));
        try(ObservedDb db=new ObservedDb(args[0],args[1])) {
            DbStore store=store(db,context);
            turn(store,"0");store.setState("r979_retention_due_at","0");store.setState("r943_storage_reclaim_at","0");
            callback(store,db,context);continuity(store,"COLD");
            check("1".equals(store.getState("r75_research_callback_turn","")),"NEXT_RETENTION_DURABLE");
        }
        try(ObservedDb db=new ObservedDb(args[0],args[1])) {
            DbStore store=store(db,context);
            Handler retention=callback(store,db,context);
            check(only("RETENTION"),"RESTART_ACTUAL_RETENTION_ONLY");
            long due=Long.parseLong(store.getState("r979_retention_due_at","0"));
            check(due>System.currentTimeMillis()+50000L,"ACTUAL_EMPTY_RETENTION_60_SECOND_CADENCE");
            check(retention.pending()==1&&"2".equals(store.getState("r75_research_callback_turn","")),"RETENTION_SCHEDULES_ONE_DURABLE_RESEARCH_TURN");
            retention.runNext();retention.runNext();continuity(store,"AFTER_RETENTION");
            check(retention.pending()==0,"IDLE_ACTUAL_CONTINUITY_STOPS_CHAIN");
            callback(store,db,context);
            check(only("RECLAIM")&&store.getState("r943_storage_reclaim","").startsWith("RECLAIM_COMPLETE"),"ACTUAL_RECLAIM_COMPLETES_IN_OWN_TURN");
            check("0".equals(store.getState("r75_research_callback_turn","")),"ACTUAL_WHEEL_WRAP_DURABLE");
            turn(store,"1");callback(store,db,context);continuity(store,"RETENTION_NOT_DUE");
            turn(store,"3");callback(store,db,context);continuity(store,"RECLAIM_NOT_DUE");
            turn(store,"1");store.setState("r979_retention_due_at","0");db.retentionBudgetFault=true;
            Handler yielded=callback(store,db,context);
            check(only("RETENTION")&&db.retentionYieldObserved&&db.retentionBudgetBefore>0L&&db.retentionBudgetBefore<=750L
                    &&yielded.pending()==1&&"2".equals(store.getState("r75_research_callback_turn","")),"REAL_750MS_RETENTION_EXPIRY_PRESERVES_NEXT_TURN");
            callback(store,db,context);continuity(store,"AFTER_REAL_BUDGET_EXPIRY");
            turn(store,"0");db.claimWriteFault=true;callback(store,db,context);
            check(calls.isEmpty()&&"0".equals(store.getState("r75_research_callback_turn","")),"CLAIM_WRITE_FAILURE_ROLLS_BACK_WITHOUT_OWNER");
            check(store.getState("r908_research_tick_status","").startsWith("ERROR:"),"CLAIM_WRITE_FAILURE_VISIBLE");
            callback(store,db,context);continuity(store,"AFTER_CLAIM_WRITE_RECOVERY");
            turn(store,"0");RealtimePressureGateR811.beginAnalysis();
            try{callback(store,db,context);check(calls.isEmpty()&&"0".equals(store.getState("r75_research_callback_turn","")),"ACTUAL_P0_PREEMPTS_WITHOUT_SPENDING_TURN");}
            finally{RealtimePressureGateR811.endAnalysis();}
        }
        String result="{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_PRODUCT_CALLBACK_AND_OWNERS_REAL_SQLITE_ANDROID_HOST_ADAPTERS_EMPTY_HISTORY\"}\n";
        Files.writeString(Path.of(args[2]),result);System.out.println(result);
    }
}
