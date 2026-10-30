package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.TimeSourceR811;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Android WAL snapshot/page-copy probe with a moving Root app_state writer. Synthetic files only;
 * this is neither an OS crash/physical-phone test nor evidence of natural model quality. */
@RunWith(AndroidJUnit4.class)
public final class R29CutoverSnapshotDeviceTest {
    static final int ROOT_ROWS=97,DEADLINE_MS=1500;
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){assertTrue(c.moveToFirst());return c.getLong(0);}}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){assertTrue(c.moveToFirst());return c.getString(0);}}
    static long deadline(){return android.os.SystemClock.elapsedRealtime()+30_000;}
    static void fixture(SQLiteDatabase db){
        assertTrue(db.enableWriteAheadLogging());db.execSQL("PRAGMA synchronous=FULL");
        db.beginTransactionNonExclusive();try{
            db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,value TEXT,updated_at INTEGER)");
            db.execSQL("CREATE TABLE r29_snapshot_fixture(id INTEGER PRIMARY KEY,value BLOB,score REAL,n TEXT)");
            for(int i=0;i<ROOT_ROWS;i++)db.execSQL("INSERT INTO app_state VALUES(?,?,1)",new Object[]{String.format(Locale.ROOT,"key%03d",i),"original-"+i});
            for(int i=0;i<65;i++)db.execSQL("INSERT INTO r29_snapshot_fixture VALUES(?,?,?,NULL)",new Object[]{i-32,new byte[]{0,(byte)i,(byte)255},i*.125});
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    static long moveRoot(ExecutorService executor,SQLiteDatabase root)throws Exception{
        Future<Long> future=executor.submit(()->{
            long start=android.os.SystemClock.elapsedRealtime();
            try(DatabaseCoordinator.WriterDeadlineScopeCP3 bound=DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs()+DEADLINE_MS);
                DatabaseCoordinator.WriterGuardCP3 guard=DatabaseCoordinator.acquireWriterGuardCP3()){
                root.beginTransactionNonExclusive();try{
                    root.execSQL("UPDATE app_state SET value='live-advanced',updated_at=2 WHERE state_key='key070'");
                    // Replacing the first already-copied key moves its rowid behind the page cursor.
                    root.execSQL("INSERT OR REPLACE INTO app_state VALUES('key000','live-replaced',2)");
                    root.execSQL("INSERT INTO app_state VALUES('live-new','live-inserted',2)");
                    root.setTransactionSuccessful();
                }finally{root.endTransaction();}
            }
            return android.os.SystemClock.elapsedRealtime()-start;
        });
        long elapsed=future.get(DEADLINE_MS,TimeUnit.MILLISECONDS);assertTrue("Root writer exceeded original 1500ms guard",elapsed<DEADLINE_MS);return elapsed;
    }
    static int[] copy(SQLiteDatabase source,SQLiteDatabase target,List<MigrationCopyR29.Table> tables,ExecutorService executor,SQLiteDatabase root,long[] writerMs)throws Exception{
        target.beginTransactionNonExclusive();try{for(MigrationCopyR29.Table table:tables)target.execSQL(table.createSql);target.setTransactionSuccessful();}finally{target.endTransaction();}
        int pages=0,appPages=0;
        for(MigrationCopyR29.Table table:tables){
            Long after=null;for(;;){
                List<MigrationCopyR29.Row> rows=ResearchCutoverR29.page(source,table,after);if(rows.isEmpty())break;
                target.beginTransactionNonExclusive();try{ResearchCutoverR29.restorePage(target,table,rows);target.setTransactionSuccessful();}finally{target.endTransaction();}
                pages++;if(table.name.equals("app_state")&&++appPages==1)writerMs[0]=moveRoot(executor,root);
                after=rows.get(rows.size()-1).rowid;
            }
        }
        return new int[]{pages,appPages};
    }
    static void originalKeysRemain(SQLiteDatabase root){
        assertEquals(ROOT_ROWS,n(root,"SELECT count(*) FROM app_state WHERE state_key GLOB 'key[0-9][0-9][0-9]'"));
        assertEquals(ROOT_ROWS+1,n(root,"SELECT count(*) FROM app_state"));
        assertEquals(65,n(root,"SELECT count(*) FROM r29_snapshot_fixture"));
    }
    static void postFailureWrite(ExecutorService executor,SQLiteDatabase root,String key)throws Exception{
        Future<Boolean> future=executor.submit(()->{
            try(DatabaseCoordinator.WriterDeadlineScopeCP3 bound=DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs()+DEADLINE_MS);
                DatabaseCoordinator.WriterGuardCP3 guard=DatabaseCoordinator.acquireWriterGuardCP3()){
                root.beginTransactionNonExclusive();try{root.execSQL("INSERT INTO app_state VALUES(?,'after-release',3)",new Object[]{key});root.setTransactionSuccessful();}finally{root.endTransaction();}
            }return true;
        });assertTrue(future.get(DEADLINE_MS,TimeUnit.MILLISECONDS));
    }
    @Test public void readOnlySnapshotPinsMovingAppStateAndReleasesOnEveryExit()throws Exception{
        File directory=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"r29-cutover-snapshot-"+UUID.randomUUID());assertTrue(directory.mkdirs());
        ExecutorService executor=Executors.newSingleThreadExecutor();Map<String,Object> receipt=new LinkedHashMap<>();
        receipt.put("scope","API34_SYNTHETIC_REAL_ANDROID_WAL_NOT_PHYSICAL_NOT_NATURAL");receipt.put("rootWriterDeadlineMs",DEADLINE_MS);
        try{
            // Exact pre-fix unpinned page/restore algorithm, with an actual committed moving writer.
            try(SQLiteDatabase root=SQLiteDatabase.openOrCreateDatabase(new File(directory,"old-root.db"),null);
                SQLiteDatabase target=SQLiteDatabase.openOrCreateDatabase(new File(directory,"old-target.db"),null)){
                fixture(root);List<MigrationCopyR29.Table> tables=ResearchCutoverR29.tables(root);boolean rejected=false;String failure="";long[] writerMs={-1};
                try{copy(root,target,tables,executor,root,writerMs);rejected=!ResearchCutoverR29.root(root,tables,deadline(),()->false).equals(ResearchCutoverR29.root(target,tables,deadline(),()->false));failure="TYPED_ROOT_MISMATCH";}
                catch(android.database.sqlite.SQLiteConstraintException duplicate){rejected=true;failure=duplicate.getClass().getSimpleName();}
                assertTrue("old unpinned algorithm must reject the real moving app_state",rejected);assertTrue(writerMs[0]>=0);originalKeysRemain(root);
                receipt.put("oldUnpinnedMovingWriterRejected",true);receipt.put("oldUnpinnedFailure",failure);receipt.put("oldRootWriterCommitMs",writerMs[0]);receipt.put("oldOriginalKeysRetained",ROOT_ROWS);
            }
            try(SQLiteDatabase root=SQLiteDatabase.openOrCreateDatabase(new File(directory,"root.db"),null);
                SQLiteDatabase target=SQLiteDatabase.openOrCreateDatabase(new File(directory,"target.db"),null)){
                fixture(root);final SQLiteDatabase[] retained={null};long[] writerMs={-1};
                String copied=ResearchCutoverR29.withSourceSnapshot(root,source->{
                    retained[0]=source;assertNotSame(root,source);assertTrue(source.isReadOnly());assertTrue(source.inTransaction());
                    assertEquals("wal",s(source,"PRAGMA journal_mode").toLowerCase(Locale.ROOT));
                    List<MigrationCopyR29.Table> tables=ResearchCutoverR29.tables(source);String before=ResearchCutoverR29.root(source,tables,deadline(),()->false);
                    int[] pages=copy(source,target,tables,executor,root,writerMs);assertTrue(pages[0]>=7);assertEquals(4,pages[1]);
                    assertTrue("live Root must advance during pinned copy",n(root,"SELECT count(*) FROM app_state")==ROOT_ROWS+1);
                    assertEquals(ROOT_ROWS,n(source,"SELECT count(*) FROM app_state"));assertEquals("original-0",s(source,"SELECT value FROM app_state WHERE state_key='key000'"));
                    assertEquals("original-70",s(source,"SELECT value FROM app_state WHERE state_key='key070'"));
                    String after=ResearchCutoverR29.root(source,tables,deadline(),()->false),targetHash=ResearchCutoverR29.root(target,tables,deadline(),()->false);
                    assertEquals(before,after);assertEquals(before,targetHash);assertNotEquals(before,ResearchCutoverR29.root(root,tables,deadline(),()->false));
                    assertEquals(ROOT_ROWS,n(target,"SELECT count(*) FROM app_state"));assertEquals(ROOT_ROWS,n(target,"SELECT count(DISTINCT state_key) FROM app_state"));assertEquals(65,n(target,"SELECT count(*) FROM r29_snapshot_fixture"));
                    receipt.put("copiedPages",pages[0]);receipt.put("appStatePages",pages[1]);receipt.put("pinnedSourceReadOnlyAndInTransaction",true);receipt.put("typedRootUnchangedAcrossLiveCommit",true);receipt.put("typedTargetRootEqualsPinnedRoot",true);receipt.put("liveRootAdvancedDuringCopy",true);return targetHash;
                });
                assertNotNull(copied);assertFalse(retained[0].isOpen());originalKeysRemain(root);receipt.put("rootWriterCommitMs",writerMs[0]);receipt.put("copiedOriginalAppStateRows",ROOT_ROWS);receipt.put("sourceClosedOnSuccess",true);
                final SQLiteDatabase[] refused={null};boolean writeDenied=false;
                try{ResearchCutoverR29.withSourceSnapshot(root,source->{refused[0]=source;source.execSQL("UPDATE app_state SET value='forbidden-readonly-write'");return null;});}
                catch(android.database.sqlite.SQLiteException expected){writeDenied=true;}
                assertTrue("OPEN_READONLY must reject persistent writes",writeDenied);assertNotNull(refused[0]);assertFalse(refused[0].isOpen());assertEquals(0,n(root,"SELECT count(*) FROM app_state WHERE value='forbidden-readonly-write'"));postFailureWrite(executor,root,"after-readonly-error");
                receipt.put("persistentWriteRejectedByReadOnlyHandle",true);receipt.put("sourceClosedAfterWriteError",true);
                final SQLiteDatabase[] cancelled={null};boolean cancel=false;
                try{ResearchCutoverR29.withSourceSnapshot(root,source->{cancelled[0]=source;ResearchCutoverR29.root(source,ResearchCutoverR29.tables(source),deadline(),()->true);return null;});}
                catch(IllegalStateException expected){assertEquals("R29_CUTOVER_CANCELLED",expected.getMessage());cancel=true;}
                assertTrue(cancel);assertNotNull(cancelled[0]);assertFalse(cancelled[0].isOpen());postFailureWrite(executor,root,"after-cancellation");receipt.put("sourceClosedAndWriterReleasedAfterCancellation",true);
                final SQLiteDatabase[] failed={null};boolean bodyError=false;
                try{ResearchCutoverR29.withSourceSnapshot(root,source->{failed[0]=source;throw new IOException("SYNTHETIC_CUTOVER_BODY_FAILURE");});}
                catch(IOException expected){assertEquals("SYNTHETIC_CUTOVER_BODY_FAILURE",expected.getMessage());bodyError=true;}
                assertTrue(bodyError);assertNotNull(failed[0]);assertFalse(failed[0].isOpen());postFailureWrite(executor,root,"after-body-error");receipt.put("sourceClosedAndWriterReleasedAfterBodyError",true);
                assertEquals(ROOT_ROWS,n(root,"SELECT count(*) FROM app_state WHERE state_key GLOB 'key[0-9][0-9][0-9]'"));assertEquals(65,n(root,"SELECT count(*) FROM r29_snapshot_fixture"));receipt.put("originalKeysAndResearchRowsRetained",true);
            }
            receipt.put("status","PASS_ANDROID_ENGINEERING_SUBSET");
            System.out.println("R29_CUTOVER_SNAPSHOT_DEVICE|PASS|scope=API34_SYNTHETIC_REAL_WAL|rootWriterDeadlineMs=1500|appStatePages=4|oldUnpinnedRejected=true");
        }finally{
            executor.shutdownNow();assertTrue(executor.awaitTermination(5,TimeUnit.SECONDS));
            File output=new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),"r29-cutover-snapshot-device.json");try(FileOutputStream stream=new FileOutputStream(output)){stream.write(JsonR27.write(receipt).getBytes(StandardCharsets.UTF_8));}
            R26RuntimeDeviceTest.remove(directory);
        }
    }
}
