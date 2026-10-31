package com.ethk3.novaonepro;
import android.database.Cursor;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.strategyv2.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
/** Actual existing writer deadline scopes, unchanged freeze exception, durable exactly-once test receipt. */
@RunWith(AndroidJUnit4.class)
public final class R30WriteQueueAndroidTest {
    static long n(DbStore store,String key){try(Cursor c=store.readable().rawQuery("SELECT count(*) FROM r30_wait_probe WHERE id=?",new String[]{key})){assertTrue(c.moveToFirst());return c.getLong(0);}}
    static DbStore open(){DbStore store=new DbStore(ApplicationProvider.getApplicationContext());store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_WAIT_SCHEMA",db->{SchemaR27.ensure(db);db.execSQL("CREATE TABLE IF NOT EXISTS r30_wait_probe(id TEXT PRIMARY KEY,value INT NOT NULL)");return 0;});return store;}
    @Test public void tighterCallerDeadlineRejectsBeforeBodyThenSamePendingBatchCommitsOnce(){
        DbStore store=open();try{ResearchWriteQueueR27 queue=new ResearchWriteQueueR27(store);ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch().add("INSERT OR IGNORE INTO r30_wait_probe VALUES('deadline',1)");
            try(DatabaseCoordinator.WriterDeadlineScopeCP3 outer=DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs()-1)){assertFalse(queue.submit("WAIT_PROBE",batch,()->false));}
            assertEquals(0,n(store,"deadline"));assertEquals(1L,queue.waitSnapshotR30().get("rejectedOrRequeued"));assertTrue(queue.submit("WAIT_PROBE",batch,()->false));assertTrue(queue.submit("WAIT_PROBE",batch,()->false));assertEquals(1,n(store,"deadline"));assertEquals(1,batch.ops.size());assertEquals(200,ResearchWriteQueueR27.MAX_WAIT_MS);
            System.out.println("R30_ANDROID_WAIT|TIGHTER_ORIGINAL_SCOPE_PRESERVED_AND_RESTORED|REJECTED_BODY_NOT_RUN|EXACT_PENDING_BATCH_RETRY|ONE_DURABLE_RECEIPT|PASS");
        }finally{store.close();}
    }
    @Test public void guardRejectsBackgroundAndOriginalLiveFreezeExceptionStillCommits(){
        DbStore store=open();try{ResearchWriteQueueR27 queue=new ResearchWriteQueueR27(store);queue.setNextDeadline(System.currentTimeMillis()+1000);ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch().add("INSERT OR IGNORE INTO r30_wait_probe VALUES('freeze',1)");assertFalse(queue.submit("WAIT_PROBE",batch,()->false));assertEquals(0,n(store,"freeze"));assertTrue(queue.submit("LIVE_FREEZE",batch,()->false,true));assertEquals(1,n(store,"freeze"));assertEquals(1L,queue.waitSnapshotR30().get("rejectedOrRequeued"));
            System.out.println("R30_ANDROID_WAIT|DEADLINE_GUARD_UNCHANGED|LIVE_FREEZE_EXCEPTION_RETAINED|MAX_WAIT_200_UNCHANGED|PASS");
        }finally{store.close();}
    }
}
