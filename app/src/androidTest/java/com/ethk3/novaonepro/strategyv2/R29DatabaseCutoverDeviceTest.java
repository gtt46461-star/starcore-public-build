package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Isolated SYNTHETIC engineering test: Android SQLite routing, independent writers and paired report SQL.
 * This does not grant certificates or claim natural/physical-phone acceptance. */
@RunWith(AndroidJUnit4.class)
public final class R29DatabaseCutoverDeviceTest {
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static <T>T independent(Future<T> future)throws Exception{
        try{return future.get(1500,TimeUnit.MILLISECONDS);}catch(TimeoutException failure){
            for(Map.Entry<Thread,StackTraceElement[]> entry:Thread.getAllStackTraces().entrySet())if(entry.getKey().getName().startsWith("pool-")){
                System.out.println("R29_INDEPENDENT_WRITE_TIMEOUT_THREAD="+entry.getKey().getName()+"|"+entry.getKey().getState());for(StackTraceElement frame:entry.getValue())System.out.println("  "+frame);
            }throw failure;
        }
    }
    static boolean mirrorBusy(Throwable failure){for(int i=0;failure!=null&&i<16;i++,failure=failure.getCause()){String message=failure.getMessage();if(message!=null&&message.startsWith("CP3_DB_BUSY_R29_CRITICAL_INPUT_BOOTSTRAP_OR_CATCHUP"))return true;}return false;}
    /** This deliberately exercises the public DCo wrapper, not direct capture/apply. A busy response may
     * follow a separately committed mirror page; the requested business transaction must not run early. */
    static void warmup(DbStore store,SQLiteDatabase research,Map<String,Object> result,String name)throws Exception{
        long deadline=android.os.SystemClock.elapsedRealtime()+30_000;int busy=0,bodies=0;boolean done=false;List<Map<String,Object>> pages=new ArrayList<>();
        while(!done&&android.os.SystemClock.elapsedRealtime()<deadline&&pages.size()<256){
            CriticalInputMirrorR29.State before=CriticalInputMirrorR29.state(research);String previous=before==null?null:before.identity();boolean[] entered={false};
            try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){
                try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_SYNTHETIC_SCOPE_WARMUP",db->{entered[0]=true;db.execSQL("UPDATE r26_task SET cursor=5 WHERE task_id='synthetic-legacy'");return 0;});done=true;}
                catch(RuntimeException failure){if(!mirrorBusy(failure))throw failure;assertTrue("must exercise the public DCo failure wrapper",String.valueOf(failure.getMessage()).startsWith("F01_WRITE_FAILED"));assertFalse("business body entered before all input pages committed",entered[0]);busy++;}
            }
            if(entered[0])bodies++;
            CriticalInputMirrorR29.State after=CriticalInputMirrorR29.state(research);assertNotNull("mirror state must be durable even after wrapped busy",after);
            Map<String,Object> page=new LinkedHashMap<>();page.put("phase",after.phase);page.put("sourceEpoch",after.epoch);page.put("sourceSeq",after.seq);page.put("baseSeq",after.base);page.put("tableIndex",after.index);page.put("key",Arrays.asList(after.key));page.put("businessBodyEntered",entered[0]);pages.add(page);result.put(name+"Pages",pages);
            if(!done)assertFalse("wrapped busy returned without a durable cursor step: "+JsonR27.write(page),Objects.equals(previous,after.identity()));
        }
        assertTrue("input warmup exceeded existing 30s bound: "+JsonR27.write(pages),done);assertEquals(1,bodies);assertTrue("fixture must exercise multiple committed busy pages",busy>=2);assertTrue(CriticalInputMirrorR29.canonicalReady(research));result.put(name+"ActualWrappedDCoMultipageCommit",true);
    }
    static void checkpoint(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}}
    @Test public void realAndroidCutoverIndependentWritersAndReport()throws Exception{
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertNull(DatabaseCoordinator.peek());
        R26RuntimeDeviceTest.Sandbox sandbox=new R26RuntimeDeviceTest.Sandbox(target);
        DbStore store=new DbStore(sandbox);ResearchPoolR27 pool=new ResearchPoolR27(store,"SYNTHETIC_R29_CUTOVER",2);
        ExecutorService executor=Executors.newFixedThreadPool(2);
        SQLiteDatabase original=store.readable(),rootReader=null,researchReader=null;
        Map<String,Object> result=new LinkedHashMap<>();result.put("scope","API34_SYNTHETIC_ANDROID_SQLITE_NOT_PHYSICAL_NOT_NATURAL");
        try{
            DatabaseCoordinator coordinator=store.coordinatorForResearchR952();
            SQLiteDatabase observation=coordinator.realtimeReadOnlyR29();assertNotSame(original,observation);assertTrue(observation.isReadOnly());assertEquals(1,n(observation,"PRAGMA query_only"));
            boolean forbidden=false;try{observation.execSQL("CREATE TABLE synthetic_observer_must_not_write(k INTEGER)");}catch(android.database.sqlite.SQLiteException expected){forbidden=true;}assertTrue(forbidden);
            java.lang.reflect.Method reset=DatabaseCoordinator.class.getDeclaredMethod("closeRealtimeReadOnlyPoolR29");reset.setAccessible(true);reset.invoke(coordinator);assertFalse(observation.isOpen());observation=coordinator.realtimeReadOnlyR29();assertTrue(observation.isOpen());
            java.lang.reflect.Method select=DatabaseCoordinator.class.getDeclaredMethod("realtimeReadOnlyR29",SQLiteDatabase.class);select.setAccessible(true);
            try(SQLiteDatabase memory=SQLiteDatabase.create(null)){assertSame(memory,select.invoke(coordinator,memory));}
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_R29_SCHEMA",db->{
                RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);
                for(int i=1;i<=130;i++)CriticalInputProductionTriggerFixtureR29.oos(db,String.format(Locale.ROOT,"20261029%04d",i),1);
                db.execSQL("CREATE TABLE r29_cutover_fixture(id INTEGER PRIMARY KEY,value BLOB,n TEXT)");
                for(int i=0;i<97;i++)db.execSQL("INSERT INTO r29_cutover_fixture VALUES(?,?,NULL)",new Object[]{i-48,new byte[]{0,1,(byte)i}});
                assertSame("transaction observes its own owner",db,coordinator.realtimeReadOnlyR29());assertEquals(97,n(coordinator.realtimeReadOnlyR29(),"SELECT count(*) FROM r29_cutover_fixture"));
                db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,cursor) VALUES('synthetic-r27','R27','fixture',9),('synthetic-legacy','P2','fixture',4)");
                return 0;
            });
            Models.Draw draw=new Models.Draw();draw.issue="202610300001";draw.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};draw.openTime=System.currentTimeMillis();draw.fetchTime=draw.openTime+1000;assertTrue(store.saveDraw(draw)>=0);
            rootReader=SQLiteDatabase.openDatabase(original.getPath(),null,SQLiteDatabase.OPEN_READONLY);
            SQLiteDatabase oldResearchReader=ResearchRouteR29.openWorkerReader(sandbox);
            ResearchPoolR27.Worker migrationWorker=pool.new Worker(oldResearchReader,rootReader,"synthetic-migration");
            long start=System.nanoTime();String phase;
            try{phase=ResearchCutoverR29.run(store,migrationWorker);}finally{oldResearchReader.close();}
            result.put("cutoverMillis",(System.nanoTime()-start)/1000000);result.put("cutoverPhase",phase);
            assertEquals("ACTIVE_INDEPENDENT_RESEARCH_WRITER",phase);assertTrue(ResearchRouteR29.active(original));
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_R29_LEGACY_OWNER",db->{LegacyCapabilityAdapterR29.initialize(db,"synthetic-owner");return 0;});
            assertNull(LegacyCapabilityAdapterR29.enterContinuity(store,false));
            try{
                Future<Boolean> blocked=executor.submit(()->{try{LegacyCapabilityAdapterR29.publish(store,"synthetic-owner");return false;}catch(IllegalStateException expected){return expected.getMessage().contains("CONTINUITY_DRAIN");}});
                assertTrue("handoff must wait for the old 22-lane pulse to drain",blocked.get(2,TimeUnit.SECONDS));
            }finally{LegacyCapabilityAdapterR29.leaveContinuity();}
            LegacyCapabilityAdapterR29.publish(store,"synthetic-owner");
            assertTrue(LegacyCapabilityAdapterR29.enterContinuity(store,false).contains("OWNED_BY_RESEARCH_POOL"));
            assertNull(LegacyCapabilityAdapterR29.enterContinuity(store,true));
            try{assertTrue(LegacyCapabilityAdapterR29.onPoolContinuityThread());}finally{LegacyCapabilityAdapterR29.leaveContinuity();}
            result.put("original22LaneEntryDrainedAndFenced",true);
            researchReader=ResearchRouteR29.openWorkerReader(sandbox);
            assertEquals(1,n(researchReader,"PRAGMA query_only"));
            boolean readOnlyEnforced=false;try{researchReader.execSQL("UPDATE main.r29_cutover_fixture SET n='forbidden'");}catch(android.database.sqlite.SQLiteException expected){readOnlyEnforced=true;}assertTrue("query_only must reject writes",readOnlyEnforced);
            assertEquals(97,n(researchReader,"SELECT count(*) FROM main.r29_cutover_fixture"));assertEquals(97,n(original,"SELECT count(*) FROM r29_cutover_fixture"));
            assertEquals(1,n(researchReader,"SELECT count(*) FROM r26_task WHERE task_id='synthetic-legacy'"));assertEquals(1,n(researchReader,"SELECT count(*) FROM r26_task WHERE task_id='synthetic-r27'"));
            ResearchRouteR29.write(store,"SYNTHETIC_R29_ONLY_RESEARCH",db->{assertEquals(1,n(db,"SELECT count(*) FROM pragma_database_list"));db.execSQL("UPDATE r29_cutover_fixture SET n='research-only' WHERE id=0");return 0;});
            assertEquals(0,n(original,"SELECT count(*) FROM r29_cutover_fixture WHERE n='research-only'"));assertEquals(1,n(researchReader,"SELECT count(*) FROM r29_cutover_fixture WHERE n='research-only'"));
            warmup(store,researchReader,result,"criticalBootstrap");
            long epoch=CriticalInputMirrorR29.sourceEpoch(coordinator.realtimeReadOnlyR29());
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_R29_MULTIPAGE_REAL_ROOT_DELTA",db->{for(int i=1;i<=193;i++)CriticalInputProductionTriggerFixtureR29.oos(db,String.format(Locale.ROOT,"20261028%04d",i),1);return 0;});
            warmup(store,researchReader,result,"criticalCatchupWithAck");
            assertEquals("repeated scope refresh must not reinstall/increment Root schema epoch",epoch,CriticalInputMirrorR29.sourceEpoch(coordinator.realtimeReadOnlyR29()));
            assertEquals(323,n(researchReader,"SELECT count(*) FROM "+DbHelper.TABLE_R35_OOS_LABEL));
            result.put("criticalInputAckDoesNotResetCatchupBootstrap",true);
            CountDownLatch rootHeld=new CountDownLatch(1),releaseRoot=new CountDownLatch(1);
            Future<?> held=executor.submit(()->store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_R29_REALTIME_HELD",db->{db.execSQL("UPDATE r29_cutover_fixture SET n='root-held' WHERE id=1");rootHeld.countDown();if(!releaseRoot.await(5,TimeUnit.SECONDS))throw new IOException("synthetic release timeout");return 0;}));
            try{
                assertTrue(rootHeld.await(3,TimeUnit.SECONDS));
                Future<Long> observationDuringWriter=executor.submit(()->n(coordinator.realtimeReadOnlyR29(),"SELECT count(*) FROM r29_cutover_fixture"));assertEquals(Long.valueOf(97),independent(observationDuringWriter));
                Future<Integer> independent=executor.submit(()->ResearchRouteR29.write(store,"SYNTHETIC_R29_INDEPENDENT",db->{db.execSQL("UPDATE r29_cutover_fixture SET n='independent' WHERE id=2");return 1;}));
                assertEquals(Integer.valueOf(1),independent(independent));
                Future<Integer> legacyIndependent=executor.submit(()->{try(LegacyResearchScopeR29.Scope scope=LegacyResearchScopeR29.enter(store)){
                    return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_SYNTHETIC_INDEPENDENT_SCOPE",db->{
                        assertEquals(sandbox.getDatabasePath(ResearchRouteR29.FILE_NAME).getCanonicalPath(),new File(db.getPath()).getCanonicalPath());
                        assertEquals(1,n(db,"SELECT count(*) FROM pragma_database_list WHERE name NOT IN ('temp')"));
                        db.execSQL("UPDATE r26_task SET cursor=6 WHERE task_id='synthetic-legacy'");return 1;});
                }});
                assertEquals(Integer.valueOf(1),independent(legacyIndependent));
            }finally{releaseRoot.countDown();held.get(5,TimeUnit.SECONDS);}
            result.put("independentResearchCommitWhileRealtimeWriterHeld",true);
            result.put("explicitRootObservationReadOnlyPoolAndOwnWriteFallback",true);result.put("rootObserverReadWhileWriterHeld",true);
            assertEquals(4,n(original,"SELECT cursor FROM r26_task WHERE task_id='synthetic-legacy'"));
            assertEquals(6,n(researchReader,"SELECT cursor FROM r26_task WHERE task_id='synthetic-legacy'"));
            result.put("legacyScopedWriteIndependentAndOriginalRowsPreserved",true);
            ResearchPoolR27.Worker eventWorker=pool.new Worker(researchReader,rootReader,"synthetic-events");
            ResearchEventBridgeR29.deliver(eventWorker);assertEquals(1,n(researchReader,"SELECT count(*) FROM r29_event_inbox"));assertEquals(1,n(original,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0"));
            assertEquals(0,n(researchReader,"SELECT count(*) FROM r29_learning_receipt"));result.put("deliveryWithoutInventedLearning",true);
            // A real Android commit/fsync must return before a complete prepared frame can become eligible.
            IssueCommitR29.Fence fence=IssueCommitR29.capture(rootReader,draw.issue,TargetIssueResolver.sequenceOrdinalSU1(draw.issue),draw.openTime+60000,"SYNTHETIC_ANDROID_FREEZE");
            PreparedForecastR29.Frame frame=new PreparedForecastR29.Frame(fence,StatsR27.sha256("isolated synthetic draw input"),true);
            PreparedForecastR29.Forecast forecast=new PreparedForecastR29.Forecast();forecast.policy="synthetic-android-only";forecast.position=1;double[] p=new double[10];Arrays.fill(p,.1);forecast.probabilities=StoreR27.pack(p);forecast.n1=1;forecast.n2=2;forecast.n3=3;forecast.act=1;forecast.status="FROZEN";forecast.frozenAt=System.currentTimeMillis();frame.forecasts.add(forecast);
            assertEquals(1,PreparedForecastR29.publish(eventWorker,frame));
            assertEquals(1,n(researchReader,"SELECT count(*) FROM r29_freeze_manifest WHERE phase='USABLE' AND first_committed_wall>=prepared_wall AND first_committed_mono>=prepared_mono"));
            assertEquals(1,n(researchReader,"SELECT count(*) FROM r27_live_frozen WHERE policy_hash='synthetic-android-only' AND status='FROZEN' AND commit_verified_at>0 AND commit_verified_mono>0"));
            assertEquals(0,n(researchReader,"SELECT count(*) FROM r29_learning_receipt"));
            result.put("preparedFrameWithActualAndroidPostCommitReceipt",true);
            File report=new File(sandbox.getCacheDir(),"paired-report");assertTrue(report.mkdirs());
            checkpoint(original);File realtimeCopy=new File(report,"snapshot.db"),researchCopy=new File(report,"research-snapshot.db");
            try(SQLiteDatabase rw=SQLiteDatabase.openDatabase(sandbox.getDatabasePath(ResearchRouteR29.FILE_NAME).getPath(),null,SQLiteDatabase.OPEN_READWRITE)){checkpoint(rw);}
            Files.copy(sandbox.getDatabasePath(AppConfig.DB_NAME).toPath(),realtimeCopy.toPath());Files.copy(sandbox.getDatabasePath(ResearchRouteR29.FILE_NAME).toPath(),researchCopy.toPath());
            try(SQLiteDatabase merged=SQLiteDatabase.openDatabase(realtimeCopy.getPath(),null,SQLiteDatabase.OPEN_READWRITE)){
                merged.execSQL("ATTACH DATABASE ? AS research",new Object[]{researchCopy.getPath()});ResearchReportSnapshotR29.mergeAttachedCopies(merged,new LinkedHashMap<>(),()->true);
                assertEquals(1,n(merged,"SELECT count(*) FROM main.r29_cutover_fixture WHERE n='research-only'"));assertEquals(1,n(merged,"SELECT count(*) FROM main.r29_report_frame"));assertEquals(1,n(merged,"SELECT count(*) FROM draws"));
            }
            result.put("pairedReportSeesResearchAndRealtime",true);result.put("originalRowsPreserved",97);
            result.put("status","PASS_ANDROID_ENGINEERING_SUBSET");
        }finally{
            executor.shutdownNow();pool.close();if(researchReader!=null)researchReader.close();if(rootReader!=null)rootReader.close();store.close();
            File output=new File(target.getFilesDir(),"r29-cutover-device.json");try(FileOutputStream out=new FileOutputStream(output)){out.write(JsonR27.write(result).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            // Only the isolated synthetic directory is removed; no product database is touched.
            R26RuntimeDeviceTest.remove(sandbox.root);
        }
    }
}
