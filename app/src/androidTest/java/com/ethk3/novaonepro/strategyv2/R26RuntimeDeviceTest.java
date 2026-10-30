package com.ethk3.novaonepro.strategyv2;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual production coordinator/writer on an isolated SYNTHETIC database, never natural evidence. */
@RunWith(AndroidJUnit4.class)
public final class R26RuntimeDeviceTest {
    private static final int HISTORY=4200,WRITE_PROBES=1000;
    static final class Sandbox extends Application {
        final File root;final String prefix;
        Sandbox(Context base)throws IOException {this(base,"r26-synthetic-"+UUID.randomUUID());}
        Sandbox(Context base,String name)throws IOException {attachBaseContext(base);if(!name.matches("r26-synthetic-[A-Za-z0-9-]{1,120}"))throw new IOException("sandbox name");prefix=name;root=new File(base.getCacheDir(),prefix).getCanonicalFile();if(!root.isDirectory()&&!root.mkdirs())throw new IOException("sandbox directory");}
        @Override public Context getApplicationContext(){return this;}
        @Override public Context createDeviceProtectedStorageContext(){return this;}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences(prefix+"-"+name,mode);}
        File directory(String name){File f=new File(root,name);if(!f.isDirectory()&&!f.mkdirs())throw new IllegalStateException("sandbox directory");return f;}
        @Override public File getFilesDir(){return directory("files");}
        @Override public File getCacheDir(){return directory("cache");}
        @Override public File getDatabasePath(String name){return new File(directory("databases"),name);}
        @Override public ApplicationInfo getApplicationInfo(){ApplicationInfo info=new ApplicationInfo(super.getApplicationInfo());info.dataDir=root.getPath();return info;}
        @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory);}
        @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,DatabaseErrorHandler errors){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),factory,errors);}
        @Override public boolean deleteDatabase(String name){return SQLiteDatabase.deleteDatabase(getDatabasePath(name));}
    }
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):0;}}
    private static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    private static Models.Draw draw(long first,int index,long opened){Models.Draw d=new Models.Draw();d.issue=TargetIssueResolver.fromSequenceOrdinalSU1(first+index);d.numbers=new int[10];for(int p=0;p<10;p++)d.numbers[p]=(p+index%7)%10+1;d.openTime=opened+index*60000L;d.fetchTime=d.openTime+1000;return d;}
    private static void waitStopped(SQLiteDatabase db)throws Exception {long end=System.nanoTime()+10_000_000_000L;while(System.nanoTime()<end){if(scalar(db,"SELECT count(*) FROM r26_session WHERE user_enabled=1")==0)return;Thread.sleep(20);}fail("coordinator did not persist stop");}
    private static void drain(RuntimeCoordinatorR26 runtime)throws Exception {java.lang.reflect.Field field=RuntimeCoordinatorR26.class.getDeclaredField("worker");field.setAccessible(true);Thread worker=(Thread)field.get(runtime);worker.join(10000);assertFalse("coordinator thread did not drain",worker.isAlive());assertFalse("stopped coordinator retained its CPU lease",runtime.isCpuLeaseHeld());}
    static void remove(File f){File[] children=f.listFiles();if(children!=null)for(File child:children)remove(child);if(f.exists())assertTrue(f.delete());}

    @Test public void allMechanismsAdvanceWithRealWriterPressureAndRestartWithoutStaleCommit()throws Exception {
        Context original=InstrumentationRegistry.getInstrumentation().getTargetContext();assertNull("Fresh instrumentation process must not already own another database",DatabaseCoordinator.peek());Sandbox app=new Sandbox(original);DbStore store=null;RuntimeCoordinatorR26 runtime=null;JSONObject result=new JSONObject().put("scope","SYNTHETIC_ISOLATED_PRODUCTION_COORDINATOR; NOT_NATURAL_QUALITY; NOT_USER_PHONE_ACCEPTANCE").put("api",android.os.Build.VERSION.SDK_INT).put("engineeringWriteProbes",1000).put("fullPredrawOpportunitySloClaimed",false).put("restartKind","RUNTIME_COORDINATOR_REPLACEMENT_IN_SAME_PROCESS").put("javaHeapLimitAtStart",Runtime.getRuntime().maxMemory()).put("targetLargeHeapFlag",(original.getApplicationInfo().flags&ApplicationInfo.FLAG_LARGE_HEAP)!=0);
        try{
            store=new DbStore(app);SQLiteDatabase db=store.readable();assertEquals(app.getDatabasePath(AppConfig.DB_NAME).getCanonicalPath(),new File(db.getPath()).getCanonicalPath());assertFalse(new File(db.getPath()).getCanonicalPath().equals(original.getDatabasePath(AppConfig.DB_NAME).getCanonicalPath()));
            long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001"),opened=System.currentTimeMillis()-(HISTORY+WRITE_PROBES+1L)*60000;List<Models.Draw> history=new ArrayList<>();for(int i=0;i<HISTORY;i++)history.add(draw(first,i,opened));assertEquals(HISTORY,store.saveDraws(history));
            new ServiceHeartbeatStore(app).setUserEnabled(true);runtime=new RuntimeCoordinatorR26(store);long start=System.nanoTime(),end=start+240_000_000_000L;int families=0,experts=0;
            while(System.nanoTime()<end){
                if(table(db,"r26_capability")){families=(int)scalar(db,"SELECT count(*) FROM r26_capability WHERE kind='FAMILY' AND last_progress>0");experts=(int)scalar(db,"SELECT count(DISTINCT expert_id) FROM (SELECT expert_id FROM r26_expert_experiment WHERE cursor>0 UNION SELECT expert_id FROM r26_expert_audit)");if(families==24&&experts==16)break;}
                assertTrue("research coordinator exited",runtime.isRunning());Thread.sleep(100);
            }
            result.put("familyMechanismsWithCommittedProgress",families).put("expertsWithActualReceipts",experts).put("allMechanismsServiceMillis",(System.nanoTime()-start)/1000000);
            long readyAt=scalar(db,"SELECT last_progress FROM r26_task WHERE task_id='schema'");long readyService=readyAt>0?Math.max(0,System.currentTimeMillis()-readyAt):-1;result.put("strategyReadyToAllMechanismsWallMs",readyService<0?JSONObject.NULL:readyService).put("firstServiceTargetMs",60000).put("firstServiceTargetObservedWithin",readyService<0?JSONObject.NULL:readyService<=60000);
            assertEquals("Each family must commit real numerical work",24,families);assertEquals("Each expert must complete a pair sample or an audit",16,experts);assertTrue("enabled research has no CPU lease",runtime.isCpuLeaseHeld());result.put("cpuLeaseHeldBeforePressure",true).put("cpuLeaseMaxHoldMs",RuntimePowerLeaseR26.MAX_HOLD_MS).put("cpuLeaseRenewAfterMs",RuntimePowerLeaseR26.RENEW_AFTER_MS);
            long handoffWait=System.nanoTime()+60_000_000_000L;
            while(System.nanoTime()<handoffWait&&(!ResearchRouteR29.active(db)||scalar(db,"SELECT count(*) FROM r29_scheduler_owner WHERE singleton=1 AND dispatcher='RESEARCH_POOL'")==0)){
                assertTrue("runtime stopped during verified migration/handoff",runtime.isRunning());Thread.sleep(100);
            }
            assertTrue("R29 research migration did not publish within the explicit handoff budget",ResearchRouteR29.active(db));
            assertEquals("Legacy tasks must be dispatched by the unified pool",1,scalar(db,"SELECT count(*) FROM r29_scheduler_owner WHERE singleton=1 AND dispatcher='RESEARCH_POOL'"));
            result.put("r29IndependentResearchActive",true).put("legacyDispatchOwner","RESEARCH_POOL");
            assertEquals("consecutive canonical history was rejected by the original foundry",0,scalar(db,"SELECT count(*) FROM r26_progress WHERE blocked_reason LIKE '%SU1_HISTORY_NOT_CAUSAL_CONSECUTIVE%'"));
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_PROTECTED_PARENT_FIXTURE",writer->{writer.execSQL("INSERT OR IGNORE INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,state,created_at) VALUES(?,?,'SYNTHETIC','F15','INCUMBENT','SYNTHETIC',0,'LEAGUE',1)",new Object[]{IncumbentLoopSU2.KEY,XiIncumbentSU2.CANONICAL});writer.execSQL("UPDATE su2_model_genome SET val_fitness=99 WHERE genome_key=?",new Object[]{IncumbentLoopSU2.KEY});return 0;});
            long[] latency=new long[1000];Handler ui=new Handler(Looper.getMainLooper());
            for(int i=0;i<1000;i++){
                RealtimePressureGateR811.beginSettlement();long t=System.nanoTime();try{assertTrue("canonical probe write rejected",store.saveDraw(draw(first,HISTORY+i,opened))>=0);}finally{latency[i]=(System.nanoTime()-t)/1000000;RealtimePressureGateR811.endSettlement();}
                if(i%25==0){CountDownLatch responsive=new CountDownLatch(1);assertTrue(ui.post(responsive::countDown));assertTrue("main looper stalled under research pressure",responsive.await(2,TimeUnit.SECONDS));Thread.sleep(10);}RuntimeCoordinatorR26.wake();
            }
            Arrays.sort(latency);result.put("canonicalDrawWriteP50Ms",latency[499]).put("canonicalDrawWriteP95Ms",latency[949]).put("canonicalDrawWriteP99Ms",latency[989]).put("canonicalDrawWriteMaxMs",latency[999]);assertTrue("writer p99 exceeded critical owner's 2s envelope",latency[989]<2000);
            long cut=first+HISTORY+WRITE_PROBES-5000,renewEnd=System.nanoTime()+120_000_000_000L;
            while(System.nanoTime()<renewEnd&&scalar(db,"SELECT count(*) FROM r26_foundry f JOIN r26_candidate c ON c.campaign_id=f.campaign_id WHERE f.status='RUNNING' AND f.source_first>="+cut+" AND c.cursor>0")==0)Thread.sleep(100);
            assertTrue("foundry did not renew and learn past raw retention with the protected incumbent present",scalar(db,"SELECT count(*) FROM r26_foundry f JOIN r26_candidate c ON c.campaign_id=f.campaign_id WHERE f.status='RUNNING' AND f.source_first>="+cut+" AND c.cursor>0")>0);result.put("retentionBoundaryRenewed",true);
            endurance(app,store,db,first,result,runtime);
            long before=scalar(db,"SELECT coalesce(cursor,0) FROM r26_task WHERE task_id='foundry-su2'"),progress=scalar(db,"SELECT coalesce(max(actual_progress),0) FROM r26_session");assertTrue("no committed numerical sample before restart",before>0);result.put("committedCandidateSamplesBeforeRestart",before).put("actualProgressBeforeRestart",progress);
            RuntimeCoordinatorR26.Claim stale=null;end=System.nanoTime()+2_000_000_000L;while(stale==null&&System.nanoTime()<end){try(Cursor c=db.rawQuery("SELECT task_id,owner,fence,cursor FROM r26_task WHERE status='RUNNING' AND owner IS NOT NULL LIMIT 1",null)){if(c.moveToFirst())stale=new RuntimeCoordinatorR26.Claim(c.getString(0),c.getString(1),c.getLong(2),c.getLong(3));}if(stale==null)Thread.sleep(5);}
            runtime.close();drain(runtime);waitStopped(db);if(stale!=null)try{RuntimeCoordinatorR26.requireFence(db,stale);fail("stopped owner could commit");}catch(IllegalStateException expected){}
            runtime=new RuntimeCoordinatorR26(store);end=System.nanoTime()+30_000_000_000L;while(System.nanoTime()<end&&scalar(db,"SELECT coalesce(cursor,0) FROM r26_task WHERE task_id='foundry-su2'")<=before)Thread.sleep(50);
            long after=scalar(db,"SELECT coalesce(cursor,0) FROM r26_task WHERE task_id='foundry-su2'");assertTrue("restart lost or stopped numerical progress",after>before);result.put("committedCandidateSamplesAfterRestart",after).put("restartResumed",true).put("staleOwnerRejected",stale!=null);
            SQLiteDatabase research=ResearchRouteR29.reader(db);long newErrors=scalar(research,"SELECT count(*) FROM r27_task_error WHERE task_id LIKE 'r29-%'");result.put("r29TaskErrors",newErrors).put("retainedLegacyStateExports",scalar(research,"SELECT count(*) FROM r29_legacy_capability_snapshot"));assertEquals("New R29 owner/cutover/export tasks must retain and repair actual failures",0,newErrors);
            runtime.close();drain(runtime);waitStopped(db);long capacityFailures=ServiceTotalsR26.failures(db);result.put("committedServiceCapacityFailures",capacityFailures).put("capacityFailReceipts",scalar(db,"SELECT count(*) FROM r26_progress WHERE blocked_reason LIKE 'CAPACITY_FAIL:%'")).put("checkpointFileBytes",CheckpointArtifactsR26.forStore(store).bytes());assertEquals("Committed capacity failures remain failures after diagnostic-ring pruning or restart",0,capacityFailures);result.put("result","PASS_WITH_STATED_ENGINEERING_SCOPE");
        }catch(Throwable failure){result.put("result","FAIL").put("error",failure.toString());
            JSONArray threads=new JSONArray();for(Map.Entry<Thread,StackTraceElement[]> entry:Thread.getAllStackTraces().entrySet())if(entry.getKey().getName().contains("starcore-r26")){JSONArray stack=new JSONArray();for(StackTraceElement frame:entry.getValue())stack.put(frame.toString());threads.put(new JSONObject().put("name",entry.getKey().getName()).put("state",entry.getKey().getState().toString()).put("stack",stack));}result.put("researchThreadsAtFailure",threads);
            if(store!=null&&table(store.readable(),"r26_task")){JSONArray tasks=new JSONArray();try(Cursor c=store.readable().rawQuery("SELECT task_id,cursor,status,blocked_reason,last_progress,last_cpu_ms FROM r26_task ORDER BY task_id",null)){while(c.moveToNext())tasks.put(new JSONObject().put("task",c.getString(0)).put("cursor",c.getLong(1)).put("status",c.getString(2)).put("reason",c.isNull(3)?JSONObject.NULL:c.getString(3)).put("lastProgress",c.getLong(4)).put("cpuMs",c.getLong(5)));}result.put("tasksAtFailure",tasks).put("legacySchema",ResearchSchemaCoordinatorR926.summary());}
            System.out.println("R26_RUNTIME_FAILURE_RECEIPT="+result.toString());throw failure;}
        finally{
            if(runtime!=null){new ServiceHeartbeatStore(app).setUserEnabled(false);runtime.close();drain(runtime);if(store!=null&&table(store.readable(),"r26_session"))waitStopped(store.readable());}
            if(store!=null&&RuntimeSchemaR26.ready(store.readable()))try{SQLiteDatabase evidence=store.readable();result.put("committedResearchService",ServiceTotalsR26.snapshot(evidence));JSONArray capacity=new JSONArray();try(Cursor c=evidence.rawQuery("SELECT task_id,cpu_ms,blocked_reason,recorded_at FROM r26_progress WHERE blocked_reason LIKE 'CAPACITY_FAIL:%' ORDER BY receipt_id DESC LIMIT 32",null)){while(c.moveToNext())capacity.put(new JSONObject().put("task",c.getString(0)).put("cpuMs",c.getLong(1)).put("reason",c.getString(2)).put("recordedAt",c.getLong(3)));}result.put("retainedCapacityFailureDetails",capacity).put("legacySchemaAtCompletion",ResearchSchemaCoordinatorR926.summary());JSONArray units=new JSONArray();try(Cursor c=evidence.rawQuery("SELECT step_id,completed_at-started_at FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" WHERE completed_at>0 ORDER BY completed_at-started_at DESC,step_id LIMIT 16",null)){while(c.moveToNext())units.put(new JSONObject().put("step",c.getString(0)).put("journalElapsedMs",c.getLong(1)));}result.put("slowestLegacyMigrationUnits",units);}catch(Exception capture){result.put("serviceTelemetryCaptureError",capture.toString());}
            if(store!=null)store.close();
            File receipt=new File(original.getCacheDir(),"r26-runtime-synthetic.json");try(FileOutputStream output=new FileOutputStream(receipt)){output.write(result.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));output.getFD().sync();}
            remove(app.root);
        }
    }
    /** Optional one-hour emulator engineering scenario. It never changes the standard test deadline. */
    private static void endurance(Sandbox app,DbStore store,SQLiteDatabase db,long first,JSONObject result,RuntimeCoordinatorR26 runtime)throws Exception {
        int seconds=Integer.parseInt(InstrumentationRegistry.getArguments().getString("r26EnduranceSeconds","0"));
        assertTrue("endurance must be disabled or a full 60 minutes",seconds==0||seconds==3600);if(seconds==0)return;
        String scenario=InstrumentationRegistry.getArguments().getString("r26EnduranceScreen","BRIGHT");assertTrue(scenario.equals("BRIGHT")||scenario.equals("LOCKED"));
        JSONObject receipt=new JSONObject().put("scope","EMULATOR_SYNTHETIC_ENGINEERING_ONLY; NOT_USER_PHONE_OR_NATURAL_FORWARD_QUALITY").put("requestedSeconds",seconds).put("screen",scenario).put("chargingRequiredForResearch",false);
        JSONArray samples=new JSONArray();receipt.put("samples",samples);result.put("endurance",receipt);
        long suspendedAtStart=android.os.SystemClock.elapsedRealtime()-android.os.SystemClock.uptimeMillis();receipt.put("cpuLeasePolicy","USER_ENABLED_RESEARCH_SESSION_OS_TIMED_PARTIAL_WAKE_LOCK; NO_SCREEN_OR_CHARGING_ELIGIBILITY");
        long start=android.os.SystemClock.elapsedRealtime(),finish=start+seconds*1000L,nextSample=start+60000,nextDraw=start;long lastProgress=scalar(db,"SELECT coalesce(max(actual_progress),0) FROM r26_session"),maxWrite=0;int labels=0;Handler ui=new Handler(Looper.getMainLooper());
        while(android.os.SystemClock.elapsedRealtime()<finish){
            long now=android.os.SystemClock.elapsedRealtime();
            if(now>=nextDraw){Models.Draw label=draw(first,HISTORY+WRITE_PROBES+labels,0);label.openTime=System.currentTimeMillis()-2000;label.fetchTime=label.openTime+500;RealtimePressureGateR811.beginSettlement();long entered=System.nanoTime();try{assertTrue(store.saveDraw(label)>=0);}finally{maxWrite=Math.max(maxWrite,(System.nanoTime()-entered)/1000000);RealtimePressureGateR811.endSettlement();}labels++;RuntimeCoordinatorR26.wake();nextDraw=now+5000;}
            CountDownLatch responsive=new CountDownLatch(1);assertTrue(ui.post(responsive::countDown));assertTrue("main looper stalled during endurance",responsive.await(2,TimeUnit.SECONDS));
            if(now>=nextSample){
                android.content.Intent battery=app.registerReceiver(null,new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED));assertNotNull("battery telemetry unavailable",battery);int plugged=battery.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED,-1);assertEquals("unplugged scenario changed",0,plugged);
                android.os.PowerManager power=(android.os.PowerManager)app.getSystemService(Context.POWER_SERVICE);assertNotNull(power);boolean interactive=power.isInteractive();assertEquals("requested screen scenario changed",scenario.equals("BRIGHT"),interactive);
                assertTrue("enabled research CPU lease expired during endurance",runtime.isCpuLeaseHeld());
                long progress=scalar(db,"SELECT coalesce(max(actual_progress),0) FROM r26_session");assertTrue("research stopped advancing for a complete minute",progress>lastProgress);
                JSONObject sample=new JSONObject().put("elapsedMs",now-start).put("syntheticLabels",labels).put("actualProgress",progress).put("progressDelta",progress-lastProgress).put("pssKb",android.os.Debug.getPss()).put("javaHeapUsedBytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()).put("javaHeapLimitBytes",Runtime.getRuntime().maxMemory()).put("checkpointBytes",CheckpointArtifactsR26.forStore(store).bytes()).put("maxCanonicalWriteMsSoFar",maxWrite).put("pluggedTelemetry",plugged).put("interactive",interactive).put("capacityFailReceipts",scalar(db,"SELECT count(*) FROM r26_progress WHERE blocked_reason LIKE 'CAPACITY_FAIL:%'")).put("committedServiceCapacityFailures",ServiceTotalsR26.failures(db));
                android.app.ActivityManager.RunningAppProcessInfo process=new android.app.ActivityManager.RunningAppProcessInfo();android.app.ActivityManager.getMyMemoryState(process);sample.put("cpuLeaseHeld",runtime.isCpuLeaseHeld()).put("cpuLeaseRenewals",runtime.cpuLeaseRenewals()).put("systemDeepSleepSinceStartMs",Math.max(0,(android.os.SystemClock.elapsedRealtime()-android.os.SystemClock.uptimeMillis())-suspendedAtStart)).put("processImportance",process.importance).put("ignoresBatteryOptimizations",android.os.Build.VERSION.SDK_INT>=23?power.isIgnoringBatteryOptimizations(app.getPackageName()):JSONObject.NULL);
                JSONArray tasks=new JSONArray();try(Cursor c=db.rawQuery("SELECT task_id,cursor,last_progress,last_cpu_ms,blocked_reason FROM r26_task ORDER BY task_id",null)){while(c.moveToNext())tasks.put(new JSONObject().put("task",c.getString(0)).put("cursor",c.getLong(1)).put("lastActualProgress",c.getLong(2)).put("lastCpuMs",c.getLong(3)).put("blocker",c.isNull(4)?JSONObject.NULL:c.getString(4)));}sample.put("tasks",tasks);samples.put(sample);lastProgress=progress;nextSample=now+60000;
                System.out.println("R26_ENDURANCE_MINUTE="+sample.toString());
            }
            Thread.sleep(250);
        }
        receipt.put("elapsedMs",android.os.SystemClock.elapsedRealtime()-start).put("syntheticLabelsAdded",labels).put("result","COMPLETED_EMULATOR_ENGINEERING_SCENARIO");assertTrue(receipt.getLong("elapsedMs")>=3600000);assertTrue(samples.length()>=59);
    }
}
