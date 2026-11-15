package com.ethk3.novaonepro.strategyv2;
import android.content.Context;
import android.os.SystemClock;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
/** Uses the actual legacy writer and a real parked numerical worker on a disposable SYNTHETIC DB. */
@RunWith(AndroidJUnit4.class)
public final class R26WriterDeviceTest {
    @Test public void legacyWriterReleasesBeforeContinuationCanPark()throws Exception{
        Context original=InstrumentationRegistry.getInstrumentation().getTargetContext();assertNull(DatabaseCoordinator.peek());R26RuntimeDeviceTest.Sandbox app=new R26RuntimeDeviceTest.Sandbox(original);DbStore store=new DbStore(app);String owner="SYNTHETIC_WRITER_"+UUID.randomUUID();AtomicBoolean released=new AtomicBoolean();CooperativeContinuationR26<Long> work=null;
        try{DatabaseCoordinator writer=store.coordinatorForResearchR952();work=new CooperativeContinuationR26<>("starcore-r26-synthetic-writer",()->{
                writer.runWriteOrThrowF01("SYNTHETIC_LEGACY_NONATOMIC_WRITER",db->{db.execSQL("CREATE TABLE IF NOT EXISTS synthetic_writer_probe(value INTEGER)");long end=System.nanoTime()+50_000_000L;while(System.nanoTime()<end)ExecutionBudgetR26.poll();return 1;});released.set(true);
                long end=System.nanoTime()+1_000_000_000L;while(System.nanoTime()<end)ExecutionBudgetR26.poll();return 1L;
            },()->false,null,()->SystemClock.currentThreadTimeMillis()*1000000L,owner);
            CooperativeContinuationR26.Slice<Long> slice=work.slice(10);assertFalse("outside-writer numerical work must still cooperate",slice.done);assertTrue("continuation parked while holding the legacy writer",released.get());
            assertEquals(Integer.valueOf(1),writer.runAtomicWriteOrThrowF01("SYNTHETIC_FOLLOWING_WRITER",db->{db.execSQL("INSERT INTO synthetic_writer_probe VALUES(1)");return 1;}));
        }finally{if(work!=null)work.close();assertTrue("synthetic worker did not drain",CooperativeContinuationR26.awaitOwnerStopped(owner,2000));store.close();R26RuntimeDeviceTest.remove(app.root);}
    }
}
