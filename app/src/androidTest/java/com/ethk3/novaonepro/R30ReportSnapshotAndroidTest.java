package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Android read sessions and WAL writers. Synthetic mechanics do not establish natural or phone quality. */
@RunWith(AndroidJUnit4.class)
public final class R30ReportSnapshotAndroidTest {
    @Test public void twelveChaptersKeepOneViewWhileTheWriterAdvances()throws Exception{
        File file=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"r30-report-synthetic-"+UUID.randomUUID()+".db");ExecutorService writer=Executors.newSingleThreadExecutor();
        try(SQLiteDatabase write=SQLiteDatabase.openOrCreateDatabase(file,null)){
            assertTrue(write.enableWriteAheadLogging());
            write.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INT)");write.execSQL("INSERT INTO draws VALUES('202610310001',0)");
            write.execSQL("CREATE TABLE r27_live_shadow(policy_hash TEXT PRIMARY KEY,status TEXT)");write.execSQL("INSERT INTO r27_live_shadow VALUES('policy','ACTIVE')");
            write.execSQL("CREATE TABLE r27_eprocess(policy_hash TEXT PRIMARY KEY,global_index INT,state TEXT,n INT,acts INT,hits INT)");write.execSQL("INSERT INTO r27_eprocess VALUES('policy',1,'ACCUMULATING',1,1,1)");
            write.execSQL("CREATE TABLE r29_qual_receipt(experiment_id TEXT,target_issue TEXT,act INT,hit INT)");write.execSQL("INSERT INTO r29_qual_receipt VALUES('policy','202610310001',1,1)");
            write.execSQL("CREATE TABLE su2_storage_budget(category TEXT PRIMARY KEY,cap_bytes INT,measured_bytes INT,measured_at INT,action TEXT) WITHOUT ROWID");write.execSQL("INSERT INTO su2_storage_budget VALUES('CACHE',1000,1,1,'KEEP')");
            try(SQLiteDatabase read=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
                ReportR27.R30Input before=ReportSnapshotR30.capture(read);assertTrue(before.errors.toString(),before.errors.isEmpty());assertFalse("Read transaction must end before chapter formatting",read.inTransaction());assertEquals(1L,((Number)before.facts.get("futurePeriods")).longValue());
                Future<?> committed=writer.submit(()->{write.beginTransaction();try{write.execSQL("INSERT INTO draws VALUES('202610310002',0)");write.execSQL("INSERT INTO r29_qual_receipt VALUES('policy','202610310002',1,0)");write.execSQL("UPDATE r27_eprocess SET n=2,acts=2");write.setTransactionSuccessful();}finally{write.endTransaction();}});committed.get(2,TimeUnit.SECONDS);
                List<Map<String,Object>> chapters=ReportR27.chaptersR30(before);assertEquals(12,chapters.size());for(Map<String,Object> chapter:chapters){assertEquals(1L,((Number)((Map<?,?>)chapter.get("commonMetrics")).get("futurePeriods")).longValue());assertEquals("202610310001",((Map<?,?>)chapter.get("snapshotWatermark")).get("latestIssue"));assertEquals(0L,chapter.get("reportConsistencyConflicts"));}
                ReportR27.R30Input after=ReportSnapshotR30.capture(read);assertTrue(after.errors.toString(),after.errors.isEmpty());assertEquals(2L,((Number)after.facts.get("futurePeriods")).longValue());assertEquals("202610310002",after.watermark.get("latestIssue"));
                Map<?,?> watermark=(Map<?,?>)((Map<?,?>)after.watermark.get("tables")).get("su2_storage_budget");assertNull(watermark.get("maxRowid"));assertEquals("CACHE",((Map<?,?>)watermark.get("maxPrimaryKeyByColumn")).get("category"));
                System.out.println("R30_ANDROID_REPORT_SNAPSHOT|12_CHAPTERS|WAL_WRITER_NOT_HELD_DURING_FORMATTING|COMMON_METRICS_AND_WATERMARKS|NEXT_VIEW_ADVANCES|READ_MODE="+after.watermark.get("readMode")+"|ANDROID_TEST_ENVIRONMENT_NOT_PHYSICAL_ACCEPTANCE|PASS");
            }
        }finally{writer.shutdownNow();SQLiteDatabase.deleteDatabase(file);}
    }
}
