package com.ethk3.novaonepro.strategyv2;

import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DatabaseCoordinator.CommitOutcome;
import com.ethk3.novaonepro.DatabaseCoordinator.WriteRejectedR31;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public final class R335PreparedWriterRetryTest {
    private static final String WAIT = "R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN";
    private static final String OPERATION = "EVOLUTION_STAGING_2_0";

    private static WriteRejectedR31 failure(String category, String phase, CommitOutcome outcome, String reason) {
        return new WriteRejectedR31(category, "R27_" + OPERATION, phase, outcome, new IllegalStateException(reason));
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void mustPropagate(WriteRejectedR31 rejected) {
        ResearchWriteQueueR27 queue = new ResearchWriteQueueR27((name,batch) -> { throw rejected; }, () -> false);
        try {
            queue.submit(OPERATION, new ResearchWriteQueueR27.Batch().add("SELECT 1"), () -> false);
        } catch (WriteRejectedR31 actual) {
            check(actual == rejected, "Wrong rejected transaction");
            return;
        }
        throw new AssertionError("Unsafe retry " + rejected.phase + " " + rejected.commitOutcome);
    }

    @Test public void deferredPreparedWriterCanResumeOnce() { exercise(); }

    public static void main(String[] args) {
        try {
            exercise();
            System.out.println("PREPARED_WRITER_RETRY_OK retained=1 attempts=3 committed=1 protectedRefusals=5");
        } catch (Throwable failure) {
            failure.printStackTrace(System.err);
            System.err.flush();
            System.exit(2);
        }
    }

    private static void exercise() {
        try (SQLiteDatabase db = SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE staged_slice (identity TEXT PRIMARY KEY)");
            ResearchWriteQueueR27.Batch original = new ResearchWriteQueueR27.Batch()
                    .add("INSERT INTO staged_slice VALUES(?)", "same-pinned-input-slice");
            AtomicInteger attempts = new AtomicInteger();
            ResearchWriteQueueR27 queue = new ResearchWriteQueueR27((name,batch) -> {
                check(name.equals(OPERATION) && batch == original, "Prepared slice changed");
                if (attempts.incrementAndGet() < 3) {
                    throw failure("DEPENDENCY", "ADMISSION", CommitOutcome.ROLLED_BACK, WAIT);
                }
                db.beginTransaction();
                try {
                    for (ResearchWriteQueueR27.Op op : batch.ops) db.execSQL(op.sql, op.args);
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
            }, () -> false);
            check(!queue.submit(OPERATION, original, () -> false), "First writer backpressure must yield");
            check(!queue.submit(OPERATION, original, () -> false), "Repeated writer backpressure must yield");
            check(DatabaseUtils.longForQuery(db, "SELECT count(*) FROM staged_slice", null) == 0, "Partial write");
            check(queue.submit(OPERATION, original, () -> false), "Prepared slice did not resume");
            check(attempts.get() == 3 && DatabaseUtils.longForQuery(db, "SELECT count(*) FROM staged_slice", null) == 1, "Lost or duplicated slice");
        }
        mustPropagate(failure("DEPENDENCY","ADMISSION",CommitOutcome.UNKNOWN,WAIT));
        mustPropagate(failure("DEPENDENCY","ADMISSION",CommitOutcome.COMMITTED,WAIT));
        mustPropagate(failure("DEPENDENCY","BUSINESS",CommitOutcome.ROLLED_BACK,WAIT));
        mustPropagate(failure("INTEGRITY","ADMISSION",CommitOutcome.ROLLED_BACK,WAIT));
        mustPropagate(failure("DEPENDENCY","ADMISSION",CommitOutcome.ROLLED_BACK,"R31_OWNER_FENCE_INVALID"));
    }
}
