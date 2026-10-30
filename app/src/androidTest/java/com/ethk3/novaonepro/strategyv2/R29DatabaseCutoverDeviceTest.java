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
    static void checkpoint(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}}
    @Test public void realAndroidCutoverIndependentWritersAndReport()throws Exception{
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();assertNull(DatabaseCoordinator.peek());
        R26RuntimeDeviceTest.Sandbox sandbox=new R26RuntimeDeviceTest.Sandbox(target);
        DbStore store=new DbStore(sandbox);ResearchPoolR27 pool=new ResearchPoolR27(store,"SYNTHETIC_R29_CUTOVER",2);
        ExecutorService executor=Executors.newFixedThreadPool(2);
        SQLiteDatabase original=store.readable(),rootReader=null,researchReader=null;
        Map<String,Object> result=new LinkedHashMap<>();result.put("scope","API34_SYNTHETIC_ANDROID_SQLITE_NOT_PHYSICAL_NOT_NATURAL");
        try{
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_R29_SCHEMA",db->{
                RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);
                db.execSQL("CREATE TABLE r29_cutover_fixture(id INTEGER PRIMARY KEY,value BLOB,n TEXT)");
                for(int i=0;i<97;i++)db.execSQL("INSERT INTO r29_cutover_fixture VALUES(?,?,NULL)",new Object[]{i-48,new byte[]{0,1,(byte)i}});
                db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,cursor) VALUES('synthetic-r27','R27','fixture',9),('synthetic-legacy','P2','fixture',4)");
                return 0;
            });
            Models.Draw draw=new Models.Draw();draw.issue="202610300001";draw.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};draw.openTime=System.currentTimeMillis()-60000;draw.fetchTime=draw.openTime+1000;assertTrue(store.saveDraw(draw)>=0);
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
            CountDownLatch rootHeld=new CountDownLatch(1),releaseRoot=new CountDownLatch(1);
            Future<?> held=executor.submit(()->store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SYNTHETIC_R29_REALTIME_HELD",db->{db.execSQL("UPDATE r29_cutover_fixture SET n='root-held' WHERE id=1");rootHeld.countDown();if(!releaseRoot.await(5,TimeUnit.SECONDS))throw new IOException("synthetic release timeout");return 0;}));
            try{
                assertTrue(rootHeld.await(3,TimeUnit.SECONDS));
                Future<Integer> independent=executor.submit(()->ResearchRouteR29.write(store,"SYNTHETIC_R29_INDEPENDENT",db->{db.execSQL("UPDATE r29_cutover_fixture SET n='independent' WHERE id=2");return 1;}));
                assertEquals(Integer.valueOf(1),independent.get(1500,TimeUnit.MILLISECONDS));
            }finally{releaseRoot.countDown();held.get(5,TimeUnit.SECONDS);}
            result.put("independentResearchCommitWhileRealtimeWriterHeld",true);
            ResearchPoolR27.Worker eventWorker=pool.new Worker(researchReader,rootReader,"synthetic-events");
            ResearchEventBridgeR29.deliver(eventWorker);assertEquals(1,n(researchReader,"SELECT count(*) FROM r29_event_inbox"));assertEquals(1,n(original,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0"));
            assertEquals(0,n(researchReader,"SELECT count(*) FROM r29_learning_receipt"));result.put("deliveryWithoutInventedLearning",true);
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
