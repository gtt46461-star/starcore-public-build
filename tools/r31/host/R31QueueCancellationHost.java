package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.util.concurrent.atomic.AtomicBoolean;

/** The real queue's final cancellation guard must roll back model + cursor and release its token. */
public final class R31QueueCancellationHost {
    private static long samples(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT samples FROM model",null)){if(!c.moveToFirst())throw new AssertionError("missing model");return c.getLong(0);}}
    public static void verify(DatabaseCoordinator coordinator,SQLiteDatabase db){
        AtomicBoolean cancelled=new AtomicBoolean();
        ResearchWriteQueueR27 queue=new ResearchWriteQueueR27((operation,batch)->coordinator.runAtomicWriteOrThrowF01(
                DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,operation),tx->{ResearchWriteQueueR27.execute(tx,batch,operation);return 0;}),()->false);
        long old=samples(db);
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch().add("UPDATE model SET samples=samples+1,settlements=settlements+1,parameter=parameter+7");
        batch.guardAfter(tx->{cancelled.set(true);return 0;});
        try{queue.submit("R31_HOST_CANCEL_AFTER_SQL",batch,cancelled::get);throw new AssertionError("late cancellation committed");}
        catch(DatabaseCoordinator.WriteRejectedR31 expected){if(!"CANCELLED".equals(expected.category)||expected.commitOutcome!=DatabaseCoordinator.CommitOutcome.ROLLED_BACK)throw new AssertionError("late cancellation misclassified",expected);}
        finally{Thread.interrupted();}
        if(samples(db)!=old||db.inTransaction())throw new AssertionError("cancelled model/cursor must roll back before releasing resources");
        cancelled.set(false);
        if(!queue.submit("R31_HOST_CANCEL_TOKEN_CLEARED",new ResearchWriteQueueR27.Batch().add("UPDATE model SET samples=samples+1,settlements=settlements+1,parameter=parameter+7"),()->false)||samples(db)!=old+1)
            throw new AssertionError("cancel token leaked into next submission");
        System.out.println("R31_QUEUE_LATE_CANCEL_HOST_PASS SQL_and_cursor_rollback=true cancellation_token_released=true next_submit_resumes=true");
    }
}
