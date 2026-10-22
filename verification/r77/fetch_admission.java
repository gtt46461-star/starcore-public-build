package com.ethk3.novaonepro;

import android.database.Cursor;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Real service -> real ApiClient; network boundary is local synthetic HTTP 503.
 * A blocked runtime snapshot must not be a prerequisite to opening the connection.
 * Does not assert provider publication speed or phone scheduling performance. */
public final class fetch_admission {
    static int checks;
    static final CountDownLatch network=new CountDownLatch(1),statusBlocked=new CountDownLatch(1),releaseStatus=new CountDownLatch(1);
    static volatile int statusSqlBeforeNetwork;
    static volatile boolean prohibitSql;static volatile int prohibitedSql;
    static volatile String responseBody="";
    static void check(boolean v,String name){if(!v)throw new AssertionError(name);checks++;System.out.println("PASS|"+name);}
    static void set(Object o,Class<?> c,String n,Object v)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
    static final class CaptureContext extends android.app.Application {
        int stateBroadcasts;
        @Override public Context getApplicationContext(){return this;}
        @Override public String getPackageName(){return AppConfig.PACKAGE_NAME;}
        @Override public void sendBroadcast(Intent intent){if(AppConfig.ACTION_STATE_CHANGED.equals(intent.getAction()))stateBroadcasts++;}
    }
    static final class ObservedDb extends SQLiteDatabase {
        String telemetryFailureKey="";int telemetryFailures;
        @Override public long insertWithOnConflict(String table,String nullColumn,ContentValues values,int conflict){
            if(DbHelper.TABLE_APP_STATE.equals(table)&&!telemetryFailureKey.isEmpty()
                    &&telemetryFailureKey.equals(values.getAsString("state_key"))){
                telemetryFailureKey="";telemetryFailures++;throw new IllegalStateException("FIXTURE_OPTIONAL_PROJECTION_WRITE_FAILURE");
            }
            return super.insertWithOnConflict(table,nullColumn,values,conflict);
        }
        ObservedDb(String bridge,String file)throws Exception{super(bridge,file);}
        void probe(){
            if(prohibitSql){prohibitedSql++;throw new IllegalStateException("FIXTURE_DB_MUST_NOT_SCHEDULE_FETCH");}
            boolean snapshot=false;
            for(StackTraceElement f:Thread.currentThread().getStackTrace())
                if(f.getClassName().equals(AutoFetchService.class.getName())&&f.getMethodName().equals("updateRuntimeState"))snapshot=true;
            if(!snapshot)return;
            if(network.getCount()!=0L)statusSqlBeforeNetwork++;
            statusBlocked.countDown();
            try{if(!releaseStatus.await(5,TimeUnit.SECONDS))throw new AssertionError("FIXTURE_STATUS_RELEASE_TIMEOUT");}
            catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
        }
        @Override public synchronized Cursor rawQuery(String sql,String[] args){probe();return super.rawQuery(sql,args);}
        @Override public synchronized Cursor rawQuery(String sql,String[] args,android.os.CancellationSignal signal){probe();return super.rawQuery(sql,args,signal);}
        @Override public synchronized void execSQL(String sql,Object[] args){probe();super.execSQL(sql,args);}
    }
    public static void main(String[] args)throws Exception {
        // Synthetic, memory-only credential for intercepted URLs. Nothing is sent to a server.
        PrivateApiCredentialCP12 credential=(PrivateApiCredentialCP12)HostProductR974.U.allocateInstance(PrivateApiCredentialCP12.class);
        set(credential,PrivateApiCredentialCP12.class,"key","R77SyntheticNoNetworkCredential");
        set(null,KeyProvider.class,"credential",credential);
        URL.setURLStreamHandlerFactory(protocol -> "https".equals(protocol)?new URLStreamHandler(){
            @Override protected URLConnection openConnection(URL url){
                network.countDown();
                return new HttpURLConnection(url){
                    public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}
                    public int getResponseCode(){return responseBody.isEmpty()?503:200;}
                    public InputStream getInputStream(){return new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8));}
                };
            }
        }:null);
        try(ObservedDb db=new ObservedDb(args[0],args[1])){
            db.execSQL("DELETE FROM draws");
            DbStore store=HostProductR974.store(db);
            AutoFetchService service=new AutoFetchService();
            set(service,AutoFetchService.class,"store",store);set(service,AutoFetchService.class,"running",true);
            set(service,AutoFetchService.class,"forceRefresh",true);set(service,AutoFetchService.class,"dataGuard",new DataGuard());
            // Ledger is absent in this isolated admission test; it is not an alternate HTTP implementation.
            set(service,AutoFetchService.class,"apiClient",new ApiClient());
            Handler fetch=new Handler(Looper.getMainLooper()),guard=new Handler(Looper.getMainLooper());
            set(service,AutoFetchService.class,"fetchHandler",fetch);set(service,AutoFetchService.class,"runtimeGuardHandler",guard);
            Method decision=AutoFetchService.class.getDeclaredMethod("broadcastDecision",Models.Decision.class);decision.setAccessible(true);
            decision.invoke(service,new Object[]{null});decision.invoke(service,new Object[]{null});
            check(fetch.pending()==0&&guard.pending()==1,"DECISION_TELEMETRY_COALESCES_ON_GUARD_NOT_FETCH");
            Method startHeartbeat=AutoFetchService.class.getDeclaredMethod("startBacktestHeartbeat",String.class,int.class,int.class,int.class,String.class,boolean.class);startHeartbeat.setAccessible(true);
            Method stopHeartbeat=AutoFetchService.class.getDeclaredMethod("stopBacktestHeartbeat",int.class,String.class);stopHeartbeat.setAccessible(true);
            startHeartbeat.invoke(service,"FIXTURE",100,1,90,"fixture",false);
            check(fetch.pending()==0&&guard.pending()==2,"BACKTEST_HEARTBEAT_USES_EXISTING_GUARD");
            stopHeartbeat.invoke(service,90,"stopped");
            check(fetch.pending()==0&&guard.pending()==1,"STOP_REMOVES_HEARTBEAT_FROM_ITS_NEW_OWNER");
            startHeartbeat.invoke(service,"FIXTURE",100,1,90,"fixture",false);stopHeartbeat.invoke(service,90,"stopped again");
            check(guard.pending()==1,"RESTART_STOP_LEAVES_NO_OLD_BACKTEST_HEARTBEAT");
            startHeartbeat.invoke(service,"FIXTURE",100,1,90,"fixture",false);
            guard.runNext();guard.runNext();
            check(fetch.pending()==0&&guard.pending()==1,"EXECUTED_HEARTBEAT_REPOSTS_ONLY_TO_GUARD");
            stopHeartbeat.invoke(service,90,"done");decision.invoke(service,new Object[]{null});
            Method schedule=AutoFetchService.class.getDeclaredMethod("scheduleNextFetchLoop");schedule.setAccessible(true);
            prohibitSql=true;try{schedule.invoke(service);}finally{prohibitSql=false;}
            check(prohibitedSql==0&&fetch.pending()==1,"NEXT_FETCH_SCHEDULING_NEVER_READS_SQL");
            Method run=AutoFetchService.class.getDeclaredMethod("runFetchChannelOnce");run.setAccessible(true);
            Throwable[] failure={null};Thread worker=new Thread(()->{try{run.invoke(service);}catch(Throwable e){failure[0]=e;}},"R77_actual_fetch_admission");
            worker.start();
            boolean connected;
            try{connected=network.await(2,TimeUnit.SECONDS);}
            finally{releaseStatus.countDown();worker.join(5000L);}
            check(connected&&statusSqlBeforeNetwork==0,"ACTUAL_FETCH_CONNECTS_WITHOUT_RUNTIME_SNAPSHOT_SQL");
            check(guard.pending()==1,"QUEUED_SLOW_TELEMETRY_DOES_NOT_GATE_HTTP");
            check(statusBlocked.getCount()==0L,"RUNTIME_SNAPSHOT_STILL_PUBLISHED_AFTER_NETWORK_ATTEMPT");
            check(!worker.isAlive()&&failure[0]==null,"SERIAL_FETCH_WORKER_RETURNS_AFTER_FAILURE");
            Field busy=AutoFetchService.class.getDeclaredField("fetchBusy");busy.setAccessible(true);
            check(!busy.getBoolean(service),"FETCH_BUSY_CLEARED_AFTER_NETWORK_FAILURE");
            long drawAt=System.currentTimeMillis()-5000L;String issue="202610131001";
            responseBody="{\"issue\":\""+issue+"\",\"openTime\":"+drawAt+",\"numbers\":[1,2,3,4,5,6,7,8,9,10]}";
            CountdownClock clock=new CountdownClock(Looper.getMainLooper());
            set(service,AutoFetchService.class,"countdownClock",clock);set(service,AutoFetchService.class,"forceRefresh",true);
            db.telemetryFailureKey="last_new_draw_issue";
            run.invoke(service);
            Models.Draw saved=store.getDrawByIssue(issue);
            check(saved!=null&&saved.isValid()&&issue.equals(saved.issue)&&saved.openTime==drawAt
                    &&java.util.Arrays.equals(saved.numbers,new int[]{1,2,3,4,5,6,7,8,9,10}),"ACTUAL_FETCH_PRESERVES_EXACT_PROVIDER_ISSUE_TIME_NUMBERS");
            check(issue.equals(clock.getCurrentIssue())&&AppConfig.nextIssueText(issue).equals(clock.getTargetIssue()),"DURABLE_EXACT_DRAW_STILL_ADVANCES_ITS_OWN_CLOCK_TARGET");
            check(db.telemetryFailures==1&&"1".equals(store.getState("realtime_priority","")),"NEW_DRAW_TELEMETRY_FAILURE_PRESERVES_FOLLOWING_REALTIME_DISPATCH");
            CaptureContext context=new CaptureContext();set(service,ContextWrapper.class,"mBase",context);
            set(service,AutoFetchService.class,"lastRuntimeStateKey","");db.telemetryFailureKey="service_status";
            Method update=AutoFetchService.class.getDeclaredMethod("updateRuntimeState",String.class,String.class);update.setAccessible(true);
            update.invoke(service,AppConfig.SERVICE_STATUS_RUNNING,"");
            check(db.telemetryFailures==2&&context.stateBroadcasts==1,"ATOMIC_RUNTIME_PROJECTION_FAILURE_DOES_NOT_DROP_LIVE_BROADCAST");
        }
        String json="{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_SERVICE_API_ADMISSION_BLOCKED_STATUS_SQL_SYNTHETIC_HTTP\"}\n";
        Files.writeString(Path.of(args[2]),json);System.out.println(json);
    }
}
