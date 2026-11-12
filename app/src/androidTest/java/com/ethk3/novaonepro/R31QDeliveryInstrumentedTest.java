package com.ethk3.novaonepro;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.security.MessageDigest;
import java.util.UUID;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real disposable Android SQLite + main/diagnostics Service + provider readback.
 * Invoke only with -e r31qRealService true on a clean disposable emulator. */
@RunWith(AndroidJUnit4.class)
public final class R31QDeliveryInstrumentedTest {
    private static Context app(){
        assertEquals("true",InstrumentationRegistry.getArguments().getString("r31qRealService"));
        return ApplicationProvider.getApplicationContext();
    }
    private static void ready(DbStore store)throws Exception{
        long until=SystemClock.elapsedRealtime()+180000L;
        do{store.requestLegacySchemaMigrationR31();if(store.coordinatorR36().isLegacyRuntimeSchemaReady())return;Thread.sleep(100);}
        while(SystemClock.elapsedRealtime()<until);
        fail("LEGACY_NOT_READY: "+store.coordinatorR36().startupSchemaSummary());
    }
    @Test public void firstSchemaProbeFailureStopsRemainingColumns()throws Exception{
        Context app=app();DbStore store=new DbStore(app());ready(store);
        java.lang.reflect.Field auditField=DbStore.class.getDeclaredField("legacyColumnAuditR31");auditField.setAccessible(true);
        java.lang.reflect.Field failureField=DbStore.class.getDeclaredField("legacyColumnFailureR31");failureField.setAccessible(true);
        @SuppressWarnings("unchecked") ThreadLocal<Boolean> audit=(ThreadLocal<Boolean>)auditField.get(store);
        @SuppressWarnings("unchecked") ThreadLocal<Throwable> failures=(ThreadLocal<Throwable>)failureField.get(store);
        java.lang.reflect.Method add=DbStore.class.getDeclaredMethod("addColumnIfMissing",android.database.sqlite.SQLiteDatabase.class,String.class,String.class,String.class);add.setAccessible(true);
        android.database.sqlite.SQLiteDatabase closed=android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(new File(app.getCacheDir(),"r31q_closed_"+UUID.randomUUID()+".db"),null);closed.close();
        audit.set(Boolean.TRUE);failures.remove();
        try{
            try{add.invoke(store,closed,"r31q_closed","probe","INTEGER");fail("first failing probe was silently accepted");}
            catch(java.lang.reflect.InvocationTargetException expected){assertTrue(expected.getCause().getMessage().contains("LEGACY_COLUMN_MIGRATION_FAILED"));}
            assertNotNull(failures.get());
            for(int i=0;i<20;i++)try{
                add.invoke(store,store.writable(),DbHelper.TABLE_APP_STATE,"r31q_must_not_be_added","INTEGER");fail("later column entered SQL after failed unit");
            }catch(java.lang.reflect.InvocationTargetException expected){assertTrue(expected.getCause().getMessage().contains("LEGACY_COLUMN_MIGRATION_FAILED"));}
            try(Cursor c=store.readable().rawQuery("PRAGMA table_info("+DbHelper.TABLE_APP_STATE+")",null)){
                while(c.moveToNext())assertNotEquals("r31q_must_not_be_added",c.getString(1));
            }
            System.out.println("R31Q_ANDROID|FAILED_PROBE_NOT_SWALLOWED|REMAINING_COLUMNS_NOT_EXECUTED|NO_REPEATED_BUSY_WAITS|PASS");
        }finally{audit.remove();failures.remove();store.close();}
    }
    @Test public void oldReadyStampCannotSkipTheNewAuditedMigrationPass()throws Exception{
        DbStore store=new DbStore(app());ready(store);
        SchemaDomainCoordinatorR926.Contract contract=SchemaDomainCoordinatorR926.legacyContract();
        try{
            // Disposable fixture only: remove new-version unit receipts to
            // reproduce an old READY database, retaining both old markers.
            store.coordinatorR36().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31Q_OLD_READY_FIXTURE"),db->{
                db.execSQL("DELETE FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" WHERE domain=? AND revision=? AND (step_id LIKE 'R31Q21_%' OR step_id=?)",new Object[]{contract.domain,contract.revision,LegacySchemaCoordinatorR926.AUDITED_RUNTIME_STEP_R31Q21});return 1;
            });
            assertTrue(SchemaDomainCoordinatorR926.matches(store.readable(),contract));
            assertTrue(SchemaDomainCoordinatorR926.stepComplete(store.readable(),contract,"LEGACY_RUNTIME"));
            assertFalse(SchemaDomainCoordinatorR926.stepComplete(store.readable(),contract,LegacySchemaCoordinatorR926.AUDITED_RUNTIME_STEP_R31Q21));
            store.coordinatorR36().invalidateLegacyRuntimeSchemaR31("R31Q_OLD_READY_FIXTURE");ready(store);
            assertTrue("old READY incorrectly bypassed audited pass",SchemaDomainCoordinatorR926.stepComplete(store.readable(),contract,LegacySchemaCoordinatorR926.AUDITED_RUNTIME_STEP_R31Q21));
            System.out.println("R31Q_ANDROID|OLD_READY_REAUDITED|LEGACY_MARKERS_PRESERVED|NEW_UNITS_DURABLE|PASS");
        }finally{store.close();}
    }
    @Test public void blockedOptionalObserversStayBoundedAndDoNotBlockTheNextRequest()throws Exception{
        File root=ReportDirectoriesCP12.root(app().getFilesDir());String id=String.format(java.util.Locale.ROOT,"%064x",2106);
        java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(2),release=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicReference<Throwable> error=new java.util.concurrent.atomic.AtomicReference<>();
        Runnable caller=()->{try{ReportJobJournalCP12.collectObservation(root,id,new JSONObject(),"fixture",()->{entered.countDown();release.await();return "OBSERVED_FIXTURE";});}catch(Throwable failure){error.compareAndSet(null,failure);}};
        Thread a=new Thread(caller),b=new Thread(caller);a.start();b.start();
        try{
            assertTrue("two actual observer slots did not start",entered.await(3,java.util.concurrent.TimeUnit.SECONDS));
            for(int i=0;i<12;i++){
                JSONObject observation=new JSONObject();ReportJobJournalCP12.collectObservation(root,id,observation,"extra",()->"SHOULD_NOT_RUN");
                assertEquals("OBSERVATION_CAPACITY_WAIT",observation.getString("extra"));
            }
            int workers=0;for(Thread t:Thread.getAllStackTraces().keySet())if(t.isAlive()&&t.getName().equals("report-observer-r31q"))workers++;
            assertEquals(2,workers);
        }finally{release.countDown();a.join(10000);b.join(10000);}
        assertFalse(a.isAlive());assertFalse(b.isAlive());assertNull(String.valueOf(error.get()),error.get());
        System.out.println("R31Q_ANDROID|OPTIONAL_OBSERVERS_BOUNDED_AT_TWO|TWELVE_ADDITIONAL_REQUESTS_EXPLICIT_UNKNOWN|PASS");
    }
    @Test public void legacyMigrationResumesAndAllowsCriticalCommitsBetweenUnits()throws Exception{
        DbStore store=new DbStore(app());String key=UUID.randomUUID().toString();int committed=0,during=0;
        try{
            long until=SystemClock.elapsedRealtime()+180000L;
            do{
                store.requestLegacySchemaMigrationR31();
                boolean pending=!store.coordinatorR36().isLegacyRuntimeSchemaReady();
                try{
                    store.coordinatorR36().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.LIVE_FREEZE,"R31Q_INJECTED_CRITICAL_PROBE"),db->{
                        db.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{"r31q_probe_"+key,"original",System.currentTimeMillis()});return 1;
                    });committed++;if(pending)during++;
                }catch(DatabaseCoordinator.WriteRejectedR31 busy){assertTrue("unexpected failure "+busy,busy.getMessage().contains("DEPENDENCY")||busy.getMessage().contains("LOCK_BUSY"));}
                if(store.coordinatorR36().isLegacyRuntimeSchemaReady())break;
                Thread.sleep(50);
            }while(SystemClock.elapsedRealtime()<until);
            assertTrue(store.coordinatorR36().startupSchemaSummary(),store.coordinatorR36().isLegacyRuntimeSchemaReady());
            assertTrue("no independent critical commit",committed>0);
            // A preexisting READY database need not migrate in this invocation.
            try(Cursor c=store.readable().rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=?",new String[]{"r31q_probe_"+key})){
                assertTrue(c.moveToFirst());assertEquals("original",c.getString(0));
            }
            LegacySchemaCoordinatorR926.requireMainChainClosedLoopReadyR31(store.readable());
            System.out.println("R31Q_ANDROID|LEGACY_READY|CRITICAL_COMMITS="+committed+"|WHILE_PENDING="+during+"|INJECTED_NOT_NATURAL|PASS");
        }finally{store.close();}
    }
    @Test public void fullReportReachesPublishedZipAndProviderReadback()throws Exception{
        Context app=app();DbStore store=new DbStore(app());ready(store);
        new ServiceHeartbeatStore(app).setUserEnabled(false);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->app.startActivity(new Intent(app,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        Thread.sleep(1000);
        String request=ReportExportServiceCP4.requestExport(app,ReportExportServiceCP4.ExportMode.FULL_REPORT,false);
        File ack=new File(app.getFilesDir(),"r981_reports/requests/"+request+".ack.json");
        JSONObject state=null;long until=SystemClock.elapsedRealtime()+360000L;
        do{
            if(ack.isFile()){
                state=new JSONObject(ReportJobJournalCP12.readAtomic(ack,16384));String stage=state.optString("stage");
                assertFalse(state.toString(),stage.equals("FAILED")||stage.equals("CANCELLED"));
                if(stage.equals("READY_TO_SHARE"))break;
            }else assertNull(ReportRequestStatusR31.failure(request),ReportRequestStatusR31.failure(request));
            Thread.sleep(250);
        }while(SystemClock.elapsedRealtime()<until);
        assertNotNull("REPORT_ACK_NEVER_WRITTEN",state);assertEquals(state.toString(),"READY_TO_SHARE",state.optString("stage"));
        String jobId=state.getString("jobId");File job=ReportPublicationCP12.job(app.getFilesDir(),jobId);
        assertTrue("published ZIP invalid",ReportPublicationCP12.verifyPublishedZip(job,()->true));
        JSONObject index=ReportPublicationCP12.delivery(job);ReportPublicationCP12.grant(job,System.currentTimeMillis());
        JSONArray files=index.getJSONArray("files");assertTrue(files.length()>0);
        long bytes=0;byte[] buffer=new byte[32768];
        for(int i=0;i<files.length();i++){
            JSONObject member=files.getJSONObject(i);Uri uri=new Uri.Builder().scheme("content").authority(app.getPackageName()+".diagnostic.reports").appendPath("r981").appendPath(jobId).appendPath(member.getString("name")).build();
            MessageDigest sha=MessageDigest.getInstance("SHA-256");long read=0;
            try(InputStream input=app.getContentResolver().openInputStream(uri)){
                assertNotNull(input);int n;while((n=input.read(buffer))!=-1){sha.update(buffer,0,n);read+=n;}
            }
            assertEquals(member.getLong("bytes"),read);assertEquals(member.getString("sha256"),ReportArchiveCP4.hex(sha.digest()));bytes+=read;
        }
        System.out.println("R31Q_ANDROID|FULL_REPORT_PUBLISHED|PROVIDER_READBACK_HASH_PASS|BYTES="+bytes+"|CHECKS_COMPLETE="+index.optBoolean("checksComplete")+"|HEALTH="+index.optBoolean("allChecksPassed")+"|NO_QUALITY_CLAIM|PASS");
        store.close();
    }

    @Test public void liveServiceFetchesFreezesAndSettlesNewFutureObservations()throws Exception{
        Context app=app();DbStore store=new DbStore(app());ready(store);
        ServiceHeartbeatStore heartbeat=new ServiceHeartbeatStore(app);long started=System.currentTimeMillis();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->app.startActivity(new Intent(app,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        heartbeat.setUserEnabled(true);BootReceiver.persistServiceUserEnabled(app,true);
        Intent start=new Intent(app,AutoFetchService.class).setAction(AppConfig.ACTION_SERVICE_START);
        if(android.os.Build.VERSION.SDK_INT>=26)app.startForegroundService(start);else app.startService(start);
        long frozen=0,settled=0;long until=SystemClock.elapsedRealtime()+360000L;
        try{
            do{
                try(Cursor rows=store.readable().rawQuery("SELECT count(*),COALESCE(SUM(CASE WHEN settled=1 THEN 1 ELSE 0 END),0) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE commit_at>? AND pre_draw_frozen=1",new String[]{String.valueOf(started)})){
                    assertTrue(rows.moveToFirst());frozen=rows.getLong(0);settled=rows.getLong(1);
                }
                if(heartbeat.getLastFetchSuccessAt()>started&&frozen>0&&settled>0)break;
                Thread.sleep(500);
            }while(SystemClock.elapsedRealtime()<until);
            assertTrue("NO_FRESH_API_FETCH: "+heartbeat.detailedSummary(),heartbeat.getLastFetchSuccessAt()>started);
            assertTrue("NO_NEW_FUTURE_FREEZE: "+heartbeat.detailedSummary(),frozen>0);
            assertTrue("NO_NEW_FROZEN_SETTLEMENT: "+heartbeat.detailedSummary(),settled>0);
            try(Cursor rows=store.readable().rawQuery("SELECT count(*) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE commit_at>? AND commit_at>=deadline_at",new String[]{String.valueOf(started)})){
                assertTrue(rows.moveToFirst());assertEquals("late records must never masquerade as pre-draw",0,rows.getLong(0));
            }
            System.out.println("R31Q_ANDROID|REAL_API_FETCH_AND_NEW_FREEZE_SETTLEMENT|FROZEN="+frozen+"|SETTLED="+settled+"|PIT_DEADLINE_VALID|NO_WIN_RATE_CLAIM|PASS");
        }finally{
            heartbeat.setUserEnabled(false);BootReceiver.persistServiceUserEnabled(app,false);
            app.stopService(new Intent(app,AutoFetchService.class));store.close();
        }
    }
}
