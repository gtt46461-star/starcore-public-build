package com.ethk3.novaonepro;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import android.os.*;
import android.util.Log;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Explicitly injected Android mechanics. The ADB driver owns genuine kill/stop/restart phases. */
@RunWith(AndroidJUnit4.class)
public final class R30FaultMatrixTest {
    static final String BASE="202610310240",TARGET="202610310241";
    static final Set<String> FAULTS=new HashSet<>(Arrays.asList("fetch_timeout","draw_conflict","kill_settlement","kill_freeze","sqlite_full","clock_rollback","service_stop"));
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){assertTrue(sql,c.moveToFirst());return c.getLong(0);}}
    static long count(DbStore store,String table){return n(store.readable(),"SELECT count(*) FROM "+table+" WHERE target_issue='"+TARGET+"'");}
    static Models.Draw draw(int ordinal,long when){Models.Draw d=new Models.Draw();d.issue=String.format(Locale.ROOT,"20261031%04d",ordinal);d.openTime=when;d.fetchTime=when+1;d.sourceLine="SYNTHETIC_R30_FAULT_ONLY";for(int p=0;p<10;p++)d.numbers[p]=(p+ordinal-1)%10+1;assertTrue(d.isValid());return d;}
    static void seed(DbStore store){
        // This assertion precedes the fixture schema callback: a normal first request must be
        // prepared by DbStore's production startup owner, not rescued by this test's DDL.
        Map<String,Object> platform=ResearchPlatformStoreR929.schemaReadinessReportR31(store.readable());
        System.out.println("R31_FAULT_PLATFORM_STARTUP|db="+store.readable().getPath()+"|metadata="+JsonR27.write(platform));
        assertEquals("production startup bundle prerequisites before fault injection: "+platform,Boolean.TRUE,platform.get("ready"));
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_FAULT_FIXTURE_SCHEMA",db->{RuntimeSchemaR26.ensure(db);R31FixtureSchemaOwners.installSu1WithinWriterR31(db);StrategySchemaSU2.ensure(db);SchemaR27.ensure(db);db.execSQL("CREATE TABLE IF NOT EXISTS r30_fault_disk(id INTEGER PRIMARY KEY,payload BLOB)");return 0;});
        assertEquals("fresh routed test database",0,n(store.readable(),"SELECT count(*) FROM draws"));
        ArrayList<Models.Draw> history=new ArrayList<>();long latest=System.currentTimeMillis();for(int i=1;i<=240;i++)history.add(draw(i,latest-(240-i)*60_000L));assertEquals(240,store.saveDraws(history));
        assertEquals("default real authority remains disabled","0",store.getState(RealAuthoritySU2.USER_SWITCH,"0"));
    }
    static ArrayList<Models.Draw> history(DbStore store){ArrayList<Models.Draw> out=new ArrayList<>();try(Cursor c=store.readable().rawQuery("SELECT issue,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue<=? AND dirty=0 ORDER BY issue",new String[]{BASE})){while(c.moveToNext()){Models.Draw d=new Models.Draw();d.issue=c.getString(0);d.openTime=c.getLong(1);for(int p=0;p<10;p++)d.numbers[p]=c.getInt(p+2);out.add(d);}}assertEquals(240,out.size());return out;}
    static void freeze(DbStore store)throws Exception{
        AnalysisRequestStoreR36 requests=new AnalysisRequestStoreR36(store);AnalysisSingleFlightR36.Request request=requests.ensurePending(BASE,TARGET,TargetIssueResolver.predrawDeadlineWallR20261008(store.getDrawByIssue(BASE)),"SYNTHETIC_R30_FAULT_ONLY");
        // An interrupted process's durable lease expires by its original deadline. Never reset/steal it.
        long lease=n(store.readable(),"SELECT coalesce(lease_until,0) FROM "+DbHelper.TABLE_R36_ANALYSIS_REQUEST+" WHERE target_issue='"+TARGET+"'");
        V51RuntimeInputR36 prepared=null;long preparationMs=0;
        if(lease>System.currentTimeMillis()){
            long start=SystemClock.elapsedRealtime();
            String identity=BuildIdentity.verifyAndPersist(ApplicationProvider.getApplicationContext(),store);assertTrue(identity,BuildIdentity.isBetCompatible(identity));
            prepared=V51RuntimeInputR36.freeze(history(store),request);preparationMs=SystemClock.elapsedRealtime()-start;
            System.out.println("R30_FAULT_RECOVERY_READONLY_PREPARATION|duration_ms="+preparationMs+"|ORIGINAL_LEASE_AND_DEADLINE_UNCHANGED");
        }
        if(lease>System.currentTimeMillis())while(lease>System.currentTimeMillis()){assertTrue("original predraw deadline remains enforceable",System.currentTimeMillis()<request.deadlineAt);SystemClock.sleep(Math.max(1,Math.min(100,lease-System.currentTimeMillis())));}
        AnalysisSingleFlightR36.SubmitResult submitted=AnalysisSingleFlightR36.shared().submit(request);assertTrue(submitted.startNow);assertNotNull(submitted.lease);AnalysisSingleFlightR36.Lease owner=submitted.lease;owner.bindCurrentThread();
        RuntimeResourceAuditR927.Scope resources=null;boolean committed=false;
        try{request=requests.claim(request,owner.ownerId,System.currentTimeMillis());assertNotNull("claim original durable request",request);owner.token.claimDurablePulse(System.currentTimeMillis(),true);resources=RuntimeResourceAuditR927.begin(store,owner);if(prepared!=null)RuntimeResourceAuditR927.completedBeforeScopeR79("RECOVERY_READONLY_PREPARATION",preparationMs);V51RuntimeInputR36 input=prepared==null?V51RuntimeInputR36.freeze(history(store),request):prepared;request=requests.bindHistoryHash(request,input.historyHash,owner.ownerId);V51RuntimeOrchestrator.OutcomeR36 result=V51AutoFetchBridge.runR36(store,input,request,owner);assertNotNull(result);committed=result.commit!=null&&result.commit.success;assertTrue(result.runtime.success);assertTrue(result.commit.status,result.commit.success);assertEquals("CANONICAL_COMMITTED",requests.status(TARGET));assertEquals(1,count(store,DbHelper.TABLE_R35_SHADOW_OBSERVATION));}
        finally{try{RuntimeResourceAuditR927.finish(store,resources,committed,"SYNTHETIC_R30_FAULT_OWNER");}finally{AnalysisSingleFlightR36.shared().releaseAndTakeNext(owner);}}
    }
    static void settle(DbStore store){Models.Draw actual=store.getDrawByIssue(TARGET);assertNotNull(actual);SettlementEngine.SettlementReport result=SettlementCoordinatorR815.closeCurrentExactR906(store,actual,SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);assertEquals(result.detail,1,result.ok);assertEquals(1,count(store,DbHelper.TABLE_R35_OOS_LABEL));SettlementTaskStoreR36.Terminal terminal=SettlementTaskStoreR36.terminal(store,TARGET);assertNotNull(terminal);assertEquals(SettlementTaskStoreR36.COMPLETED,terminal.status);String original=terminal.settlementHash;SettlementEngine.SettlementReport retry=SettlementCoordinatorR815.closeCurrentExactR906(store,actual,SettlementCoordinatorR815.Reason.WATCHDOG);assertEquals(retry.detail,1,retry.ok);assertEquals(0,retry.shadowSettledCount);assertEquals(original,SettlementTaskStoreR36.terminal(store,TARGET).settlementHash);assertEquals(1,count(store,DbHelper.TABLE_R35_OOS_LABEL));}
    static void actual(DbStore store){assertEquals(1,store.saveDraws(Collections.singletonList(draw(241,System.currentTimeMillis()))));}
    static void noFalsePublicationOrReal(DbStore store){assertEquals(0,count(store,DbHelper.TABLE_TRADES));assertEquals(0,n(store.readable(),"SELECT count(*) FROM r30_issue_timeline WHERE wall_source_published IS NOT NULL"));}
    static void killAt(String operation,String target,SQLiteDatabase db){
        assertEquals(TARGET,target);assertTrue(db.inTransaction());assertEquals(1,n(db,"SELECT count(*) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE target_issue='"+TARGET+"'"));
        if(operation.equals("R36_SETTLEMENT_CORE_R911"))assertEquals(1,n(db,"SELECT count(*) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE target_issue='"+TARGET+"'"));
        Log.i("R30_FAULT_KILL_POINT",operation+"|"+target+"|ACTUAL_OWNER_BEFORE_COMMIT|pid="+android.os.Process.myPid());android.os.Process.killProcess(android.os.Process.myPid());throw new AssertionError("SIGKILL returned");
    }
    static void timeout(DbStore store)throws Exception{
        ExecutorService server=Executors.newSingleThreadExecutor();CountDownLatch release=new CountDownLatch(1);
        try(ServerSocket socket=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))){Future<?> accepted=server.submit(()->{try(Socket client=socket.accept()){release.await(10,TimeUnit.SECONDS);}catch(Exception e){throw new RuntimeException(e);}});
            long start=SystemClock.elapsedRealtime();ApiClient.RawHttpResult result=new ApiClient(store).requestGet("https://127.0.0.1:"+socket.getLocalPort()+"/SYNTHETIC_TIMEOUT_ONLY");long elapsed=SystemClock.elapsedRealtime()-start;
            assertFalse(result.ok);assertEquals(0,result.httpCode);assertTrue("actual socket read timeout "+result.errorMessage,result.errorMessage.toLowerCase(Locale.ROOT).contains("timed out")||result.errorMessage.toLowerCase(Locale.ROOT).contains("timeout"));assertTrue("original connect/read phase timeout not lowered",elapsed>=Math.min(AppConfig.HTTP_CONNECT_TIMEOUT_MS,AppConfig.HTTP_READ_TIMEOUT_MS)-250);release.countDown();accepted.get(3,TimeUnit.SECONDS);
            assertEquals(0,n(store.readable(),"SELECT count(*) FROM "+SchemaRevisionR925.TABLE_FETCH_LEDGER+" WHERE status='HTTP_OK_BODY_RECEIVED'"));
        }finally{release.countDown();server.shutdownNow();}
    }
    static void full(DbStore store){
        long[] limit={0};
        RuntimeException[] insertFailure={null};
        try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_FAULT_ACTUAL_SQLITE_FULL",db->{long pages=n(db,"PRAGMA page_count"),pageSize=n(db,"PRAGMA page_size");limit[0]=n(db,"PRAGMA max_page_count");try(Cursor c=db.rawQuery("PRAGMA max_page_count="+pages,null)){assertTrue(c.moveToFirst());assertEquals(pages,c.getLong(0));}assertEquals(pages,n(db,"PRAGMA max_page_count"));long bytes=(pages+1)*pageSize;assertTrue("isolated fixture capacity is bounded",bytes>0&&bytes<=128L*1024*1024);try{db.execSQL("INSERT INTO r30_fault_disk VALUES(1,zeroblob(?))",new Object[]{bytes});}catch(RuntimeException actual){insertFailure[0]=actual;throw actual;}return 0;});fail("real SQLITE_FULL not triggered");}
        catch(RuntimeException failure){assertNotNull("original INSERT must fail; outer="+failure+"; cause="+failure.getCause(),insertFailure[0]);assertTrue("actual INSERT must throw original Android SQLiteFullException: "+insertFailure[0],insertFailure[0] instanceof SQLiteFullException);System.out.println("R30_FAULT_ACTUAL_SQLITE_EXCEPTION|"+insertFailure[0].getClass().getSimpleName()+"|ORIGINAL_INSERT_RETHROWN|OUTER_CAUSE="+(failure.getCause()==null?"UNKNOWN":failure.getCause().getClass().getSimpleName()));}
        finally{if(limit[0]>0)store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_FAULT_RESTORE_DISK_LIMIT",db->{try(Cursor c=db.rawQuery("PRAGMA max_page_count="+limit[0],null)){assertTrue(c.moveToFirst());assertEquals(limit[0],c.getLong(0));}return 0;});}
        assertEquals(0,n(store.readable(),"SELECT count(*) FROM r30_fault_disk"));
    }
    static void rollbackClock(DbStore store){
        long wall=System.currentTimeMillis(),mono=SystemClock.elapsedRealtime();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_FAULT_CLOCK_BASELINE",db->{IssueTimelineWriterR30.markAt(db,TARGET,IssueTimelineR30.Stage.FIRST_OBSERVED,wall,mono);return 0;});
        marker("R30_FAULT_CLOCK_ARMED|wall_ms="+wall+"|mono_ms="+mono);
        long by=SystemClock.elapsedRealtime()+15_000;
        while(System.currentTimeMillis()>=wall-55_000&&SystemClock.elapsedRealtime()<by)SystemClock.sleep(25);
        long rolledWall=System.currentTimeMillis(),rolledMono=SystemClock.elapsedRealtime();
        assertTrue("ADB must apply actual OS wall-clock rollback",rolledWall<wall-55_000);assertTrue(rolledMono>=mono);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_FAULT_ACTUAL_OS_WALL_ROLLBACK",db->{IssueTimelineWriterR30.markAt(db,TARGET,IssueTimelineR30.Stage.VALIDATED,rolledWall,rolledMono);return 0;});
        try(Cursor c=store.readable().rawQuery("SELECT wall_first_observed,wall_validated,mono_first_observed,mono_validated,boot_first_observed,boot_validated,wall_source_published FROM r30_issue_timeline WHERE issue=?",new String[]{TARGET})){assertTrue(c.moveToFirst());assertTrue(c.getLong(1)<c.getLong(0));assertEquals(rolledMono-mono,c.getLong(3)-c.getLong(2));assertEquals(c.getString(4),c.getString(5));assertTrue(c.isNull(6));}
        marker("R30_FAULT_CLOCK_OBSERVED|wall_ms="+rolledWall+"|mono_ms="+rolledMono);
        by=SystemClock.elapsedRealtime()+15_000;
        while(System.currentTimeMillis()<wall-1_000&&SystemClock.elapsedRealtime()<by)SystemClock.sleep(25);
        assertTrue("OS clock must be restored before original-deadline recovery",System.currentTimeMillis()>=wall-1_000);
    }
    static Activity visibility(String mode){Instrumentation instrument=InstrumentationRegistry.getInstrumentation();Context app=ApplicationProvider.getApplicationContext();Activity activity=instrument.startActivitySync(new Intent(app,ChartsActivityR83.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));if(mode.equals("background"))instrument.runOnMainSync(()->activity.moveTaskToBack(true));instrument.waitForIdleSync();SystemClock.sleep(250);assertEquals("actual Activity visibility",mode.equals("foreground"),activity.hasWindowFocus());return activity;}
    static void marker(String text){System.out.println(text);Bundle result=new Bundle();result.putString("stream",text+"\n");InstrumentationRegistry.getInstrumentation().sendStatus(0,result);}
    @Test public void faultCell()throws Exception{
        Bundle args=InstrumentationRegistry.getArguments();String fault=args.getString("r30Fault"),mode=args.getString("r30Visibility"),phase=args.getString("r30Phase","run");assertTrue("explicit seven-fault choice",FAULTS.contains(fault));assertTrue(Arrays.asList("foreground","background").contains(mode));assertTrue(InstrumentationRegistry.getInstrumentation() instanceof R30FaultAndroidRunner);
        Context app=ApplicationProvider.getApplicationContext();DbStore store=new DbStore(app);boolean recover=phase.equals("recover");if(!recover)seed(store);Activity activity=visibility(mode);
        try{
            if(fault.equals("service_stop")){
                if(!recover){freeze(store);new ServiceHeartbeatStore(app).setUserEnabled(true);assertTrue(BootReceiver.startAutoFetchService(app,"R30_SYNTHETIC_SERVICE_STOP"));Log.i("R30_FAULT_SERVICE_ARMED",TARGET+"|durable_frozen=1|manual_reopen_required_after_force_stop");marker("R30_FAULT_ARMED|service_stop|"+mode);long stopBy=SystemClock.elapsedRealtime()+60_000;while(SystemClock.elapsedRealtime()<stopBy)SystemClock.sleep(100);fail("external force-stop did not arrive");}
                assertEquals(1,count(store,DbHelper.TABLE_R35_SHADOW_OBSERVATION));actual(store);new ServiceHeartbeatStore(app).setUserEnabled(true);assertTrue(BootReceiver.startAutoFetchService(app,"R30_EXPLICIT_REOPEN_AFTER_FORCE_STOP"));long by=SystemClock.elapsedRealtime()+30_000;while(count(store,DbHelper.TABLE_R35_OOS_LABEL)==0&&SystemClock.elapsedRealtime()<by)SystemClock.sleep(100);assertEquals("original service startup must recover durable settlement",1,count(store,DbHelper.TABLE_R35_OOS_LABEL));new ServiceHeartbeatStore(app).setUserEnabled(false);app.stopService(new Intent(app,AutoFetchService.class));settle(store);
            }else if(fault.equals("kill_freeze")){
                if(!recover){DatabaseCoordinator.beforeCommitFaultR30.set((operation,target,db)->{if(operation.equals("R36_CANONICAL")&&target.equals(TARGET))killAt(operation,target,db);});freeze(store);fail("freeze kill point not reached");}
                assertEquals("no half frozen row",0,count(store,DbHelper.TABLE_R35_SHADOW_OBSERVATION));assertEquals(0,count(store,DbHelper.TABLE_R35_OOS_LABEL));freeze(store);actual(store);settle(store);
            }else if(fault.equals("kill_settlement")){
                if(!recover){freeze(store);actual(store);DatabaseCoordinator.beforeCommitFaultR30.set((operation,target,db)->{if(operation.equals("R36_SETTLEMENT_CORE_R911")&&target.equals(TARGET))killAt(operation,target,db);});settle(store);fail("settlement kill point not reached");}
                assertEquals(1,count(store,DbHelper.TABLE_R35_SHADOW_OBSERVATION));assertEquals("uncommitted label rolled back",0,count(store,DbHelper.TABLE_R35_OOS_LABEL));assertNull(SettlementTaskStoreR36.terminal(store,TARGET));settle(store);
            }else if(fault.equals("draw_conflict")){
                freeze(store);actual(store);Models.Draw old=store.getDrawByIssue(TARGET),conflict=draw(241,old.openTime);int swap=conflict.numbers[0];conflict.numbers[0]=conflict.numbers[1];conflict.numbers[1]=swap;conflict.sourceLine="SYNTHETIC_CONFLICT_OTHER_SOURCE";store.saveDraws(Collections.singletonList(conflict));
                try(Cursor row=store.readable().rawQuery("SELECT status FROM "+DbHelper.TABLE_R811_DRAW_CANONICAL+" WHERE target_issue=?",new String[]{TARGET})){assertTrue(row.moveToFirst());assertEquals("DRAW_CONFLICT_HARD_BLOCK",row.getString(0));}assertEquals(1,n(store.readable(),"SELECT count(*) FROM draws WHERE issue='"+TARGET+"'"));
                SettlementCoordinatorR815.closeCurrentExactR906(store,old,SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);assertEquals("conflict must remain blocked",0,count(store,DbHelper.TABLE_R35_OOS_LABEL));System.out.println("R30_FAULT_RECOVERY|draw_conflict|BLOCKED_AWAITING_VERIFIED_CANONICAL|NO_AUTOMATIC_UNBLOCK");
            }else{
                if(fault.equals("fetch_timeout"))timeout(store);else if(fault.equals("sqlite_full"))full(store);else if(fault.equals("clock_rollback"))rollbackClock(store);
                assertEquals(0,count(store,DbHelper.TABLE_R35_SHADOW_OBSERVATION));assertEquals(0,count(store,DbHelper.TABLE_R35_OOS_LABEL));freeze(store);actual(store);settle(store);
            }
            noFalsePublicationOrReal(store);marker("R30_FAULT_CELL|"+fault+"|"+mode+"|ACTUAL_ANDROID_MECHANICS|NO_HALF_ROWS|NO_DUPLICATE_SETTLEMENT|NO_FAKE_PUBLISHED_TIME|SYNTHETIC_NOT_NATURAL_OR_PHYSICAL|PASS");
        }finally{DatabaseCoordinator.beforeCommitFaultR30.remove();InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);store.close();}
    }
}
