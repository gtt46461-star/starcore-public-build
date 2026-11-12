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
}
