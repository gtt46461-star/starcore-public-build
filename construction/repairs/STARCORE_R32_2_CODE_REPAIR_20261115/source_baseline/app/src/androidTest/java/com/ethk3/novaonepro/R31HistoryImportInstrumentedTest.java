package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.system.Os;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real Android SQLite/kernel/default budgets in R30FaultAndroidRunner's explicit sandbox; no natural input. */
@RunWith(AndroidJUnit4.class)
public final class R31HistoryImportInstrumentedTest {
    private static long number(SQLiteDatabase db,String sql,String... args){try(Cursor c=db.rawQuery(sql,args)){assertTrue(sql,c.moveToFirst());return c.getLong(0);}}
    private static String value(SQLiteDatabase db,String sql,String... args){try(Cursor c=db.rawQuery(sql,args)){assertTrue(sql,c.moveToFirst());return c.getString(0);}}
    private static DbStore open(){
        Context context=ApplicationProvider.getApplicationContext();
        assertTrue("explicit routed sandbox required",context.getFilesDir().getPath().contains("r30-instrumentation"));
        DbStore store=new DbStore(context);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"R31_ANDROID_HISTORY_STARTUP"),db->{RuntimeSchemaR26.ensure(db);SchemaR27.ensure(db);return 0;});
        assertTrue(HistoryImportR27.publicationSchemaReadyReadOnlyR31(store.readable()));
        assertTrue(ResearchSourceJournalR31.readySchemaR31(store.readable()));
        return store;
    }
    private static void noImportOwners(StorageBudgetR31 budget)throws Exception{
        for(Object row:(List<?>)budget.snapshot().get("owners"))assertFalse(String.valueOf(((Map<?,?>)row).get("owner")).startsWith("HISTORY_"));
    }
    @Test public void defaultStorageUsesActualAndroidAllocatedBlocksAndReleasesOwnedFiles()throws Exception{
        Context context=ApplicationProvider.getApplicationContext();assertTrue(context.getFilesDir().getPath().contains("r30-instrumentation"));
        File root=context.getFilesDir().getCanonicalFile().getParentFile();String scope="files/r31-history-kernel-test-"+UUID.randomUUID();File directory=new File(root,scope),file=new File(directory,"actual.bin");
        StorageBudgetR31 budget=new StorageBudgetR31(root);assertTrue(StorageBudgetR31.Config.production().requireAllocated);
        try{
            try(StorageBudgetR31.Reservation owner=budget.reserve("HISTORY_ANDROID_KERNEL_TEST",0,StorageBudgetR31.Category.INPUT_RESEARCH,scope,128L<<10,false)){
                assertTrue(directory.mkdirs());byte[] bytes=new byte[65536];
                owner.beforeChunk(bytes.length);
                try(FileOutputStream output=new FileOutputStream(file)){output.write(bytes);output.getFD().sync();}
                owner.reconcile();long blocks=Math.multiplyExact(Os.stat(file.getPath()).st_blocks,512L);assertEquals(blocks,StorageCensusSU2.allocatedBytes(file));
                StorageCensusSU2.Census census=StorageCensusSU2.measure(root,Collections.singleton(scope));
                assertFalse(census.incompleteReasons.toString(),census.truncated);assertTrue(census.allocatedBytesKnown);assertEquals(65536L,census.scopeBytes.get(scope).longValue());assertTrue(census.allocatedScopeBytes.get(scope)>=blocks);
            }
            noImportOwners(budget);assertTrue("closing permit is not deleting data",file.isFile());
        }finally{assertTrue(!file.exists()||file.delete());assertTrue(!directory.exists()||directory.delete());}
        StorageCensusSU2.Census released=StorageCensusSU2.measure(root,Collections.singleton(scope));assertTrue(released.allocatedBytesKnown);assertEquals(0L,released.scopeBytes.get(scope).longValue());assertEquals(0L,released.allocatedScopeBytes.get(scope).longValue());
        System.out.println("R31_ANDROID_HISTORY|REAL_OS_STAT_DEFAULT_ADMISSION|STREAM_EXIT_THEN_PERMISSION_CLOSE|OWNED_FILES_ACTUALLY_DELETED|INJECTED_NOT_NATURAL|PASS");
    }
    @Test public void actualPartialCommitCancelResumeAndPostcommitFaultPublishOnce()throws Exception{
        DbStore store=open();Context context=ApplicationProvider.getApplicationContext();StorageBudgetR31 budget=StorageBudgetR31.forFiles(context.getFilesDir());
        StringBuilder csv=new StringBuilder("period,open_time_utc_ms,open_time_local,open_number,block,hash,valid,fetch_time\n");String nonce=UUID.randomUUID().toString();
        for(int row=1;row<=40;row++)csv.append(String.format(Locale.ROOT,"20200101%04d",row)).append(",0,,\"1,2,3,4,5,6,7,8,9,10\",ANDROID_SYNTHETIC_").append(nonce).append(",TEST,1,0\n");
        byte[] bytes=csv.toString().getBytes(StandardCharsets.UTF_8);String job=StatsR27.sha256(HistoryImportR27.R31_IMPORT_ABI+"|"+StatsR27.sha256(csv.toString()));
        SQLiteDatabase db=store.readable();long canonicalBefore=number(db,"SELECT count(*) FROM draws");
        String publishedPrefix="SELECT count(*) FROM "+HistoryImportR27.publishedRowsSqlR31()+" p WHERE p.issue BETWEEN '202001010001' AND '202001010040'";
        long visibleBefore=number(db,publishedPrefix);AtomicBoolean cancel=new AtomicBoolean();AtomicInteger faults=new AtomicInteger();
        try{
            DatabaseCoordinator.diagnosticFaultR31.set(phase->{if("AFTER_COMMIT".equals(phase)&&number(db,"SELECT COALESCE(MAX(cursor_line),0) FROM r31_history_import_run WHERE job_id=?",job)>=16)cancel.set(true);});
            try{HistoryImportR27.importCsv(store,new ByteArrayInputStream(bytes),cancel::get);fail("cancel ignored");}catch(InterruptedException expected){assertTrue(cancel.get());}finally{DatabaseCoordinator.diagnosticFaultR31.remove();}
            assertEquals(16,number(db,"SELECT cursor_line FROM r31_history_import_run WHERE job_id=?",job));assertEquals("STAGING",value(db,"SELECT status FROM r31_history_import_run WHERE job_id=?",job));
            assertEquals(visibleBefore,number(db,publishedPrefix));
            String path=value(db,"SELECT source_path FROM r31_history_import_run WHERE job_id=?",job);File original=new File(context.getFilesDir().getParentFile(),path);assertTrue(original.isFile());noImportOwners(budget);
            DatabaseCoordinator.diagnosticFaultR31.set(phase->{if("AFTER_COMMIT".equals(phase)){faults.incrementAndGet();throw new IllegalStateException("INJECTED_HISTORY_AFTER_REAL_COMMIT");}});
            try{assertEquals(job,HistoryImportR27.pendingJobIdR31(store));HistoryImportR27.resumeSavedR31(store,job,()->false);HistoryImportR27.importCsv(store,new ByteArrayInputStream(bytes));}finally{DatabaseCoordinator.diagnosticFaultR31.remove();}
            assertTrue(faults.get()>0);assertEquals("COMPLETE",value(db,"SELECT status FROM r31_history_import_run WHERE job_id=?",job));assertEquals(40,number(db,"SELECT row_count FROM r31_history_import_run WHERE job_id=?",job));
            assertEquals(1,number(db,"SELECT count(*) FROM r31_history_import_run WHERE job_id=?",job));assertEquals(40,number(db,"SELECT count(*) FROM r31_history_import_row WHERE job_id=?",job));assertEquals(path,value(db,"SELECT source_path FROM r31_history_import_run WHERE job_id=?",job));assertTrue(original.isFile());
            assertEquals(40,number(db,publishedPrefix));
            assertEquals(canonicalBefore,number(db,"SELECT count(*) FROM draws"));noImportOwners(budget);
            System.out.println("R31_ANDROID_HISTORY|ACTUAL_PARTIAL_COMMIT_INVISIBLE|CANCEL_THEN_SAME_SOURCE_RESUME|POSTCOMMIT_FAULT_NO_REPEAT|DEFAULT_STORAGE|INJECTED_NOT_NATURAL|PASS");
        }finally{DatabaseCoordinator.diagnosticFaultR31.remove();store.close();}
    }
}
