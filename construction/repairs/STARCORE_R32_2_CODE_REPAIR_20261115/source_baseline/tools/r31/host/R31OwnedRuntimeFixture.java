package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.DbStore;
import java.io.File;
import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** HOST arrangement only. Claim/CAS/lease, immutable submit, sole writer and durable receipts are production code. */
public final class R31OwnedRuntimeFixture implements AutoCloseable {
    public final SQLiteDatabase db;
    public final DatabaseCoordinator coordinator;
    public final RuntimeR27 runtime;
    public final ResearchPoolR27.Worker worker;
    public final String owner = "host-owned-" + ProcessHandle.current().pid() + "-" + UUID.randomUUID();
    private final String task;
    private final DbHelper helper;
    private final DbStore store;
    private RuntimeCoordinatorR26.Claim claim;

    public R31OwnedRuntimeFixture(SQLiteDatabase db, String task) throws Exception {
        this.db = db; this.task = task;
        Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        coordinator = (DatabaseCoordinator) ((Unsafe) field.get(null)).allocateInstance(DatabaseCoordinator.class);
        helper = new DbHelper(db);
        android.app.Application app = new android.app.Application(new File(db.getPath()));
        Field lock = DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3"); lock.setAccessible(true);
        set("writeLock", (ReentrantLock) lock.get(null)); set("helper", helper); set("app", app);
        set("waitingWriters", new AtomicInteger()); set("criticalBusinessWaitersR914", new AtomicInteger());
        coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION, "R31_HOST_FIXTURE_SCHEMA"), tx -> {
            RuntimeSchemaR26.ensure(tx); SchemaR27.ensure(tx);
            tx.execSQL("INSERT OR REPLACE INTO r26_session(session_id,user_enabled,boot_id,state,heartbeat,actual_progress) VALUES(?,1,?,'RUNNING',?,0)", new Object[] {owner, IssueTimelineWriterR30.bootId(), System.currentTimeMillis()});
            tx.execSQL("INSERT OR IGNORE INTO r26_task(task_id,lane,mechanism,status) VALUES(?,'R27','HOST_CONTROLLED_FIXTURE','READY')", new Object[] {task});
            return 0;
        });
        store = new DbStore(coordinator, db, app);
        runtime = new RuntimeR27(store, owner);
        worker = runtime.pool().new Worker(db, "host-owned-runtime");
    }

    private void set(String name, Object value) throws Exception {
        Field field = DatabaseCoordinator.class.getDeclaredField(name); field.setAccessible(true); field.set(coordinator, value);
    }

    public <T> T owned(Callable<T> body) throws Exception {
        claim = coordinator.runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("R31_HOST_FIXTURE_CLAIM"), tx -> {
            RuntimeCoordinatorR26.Claim next = RuntimeCoordinatorR26.claimWithinR31(tx, task, owner, "R27", claim);
            if (next == null) throw new IllegalStateException("HOST_PRODUCTION_CLAIM_NOT_ACQUIRED");
            RuntimeCoordinatorR26.requireFence(tx, next); return next;
        });
        RuntimeCoordinatorR26.bindBackgroundClaim(claim);
        try { return body.call(); }
        finally { RuntimeCoordinatorR26.unbindBackgroundClaimR31(); }
    }

    /** The parent has observed this precise HOST JVM exit. Use the real fenced retry API, never edit a lease. */
    public void recoverAfterConfirmedProcessExit(long oldPid) {
        if(oldPid<=0||ProcessHandle.of(oldPid).map(ProcessHandle::isAlive).orElse(false))
            throw new IllegalStateException("HOST_PRIOR_PROCESS_NOT_CONFIRMED_DEAD");
        String prior=scalar("SELECT owner FROM r26_task WHERE task_id=?",task);
        if(prior==null||!prior.startsWith("host-owned-"+oldPid+"-"))
            throw new IllegalStateException("HOST_PRIOR_PROCESS_DOES_NOT_OWN_TASK");
        ResearchPoolR27.retry(store,task);
        System.out.println("HOST_ACTUAL_PROCESS_EXIT_AND_PRODUCTION_FENCED_RETRY pid="+oldPid+" task="+task);
    }

    /** Inject a source commit after numerical preparation but before the actual coordinator opens its transaction. */
    public void beforeNextOwnedWrite(Runnable action) {
        helper.beforeWritable = () -> {
            if (RuntimeCoordinatorR26.currentClaimR30() == null) return;
            helper.beforeWritable = null; action.run();
        };
    }

    /** Fail only after a real checkpoint SQL update, immediately before native SQLite COMMIT. */
    public void failNextCheckpoint(String modelOwner) {
        final String before = scalar("SELECT state_hash FROM r29_model_checkpoint WHERE owner=?", modelOwner);
        db.beforeSuccessful = () -> {
            if (RuntimeCoordinatorR26.currentClaimR30() != null && !java.util.Objects.equals(before, scalar("SELECT state_hash FROM r29_model_checkpoint WHERE owner=?", modelOwner))) {
                db.beforeSuccessful = null; throw new IllegalStateException("HOST_INJECTED_ROLLBACK_AFTER_CHECKPOINT_SQL");
            }
        };
    }

    private String scalar(String sql, String value) {
        try (Cursor row = db.rawQuery(sql, new String[] {value})) { return row.moveToFirst() ? row.getString(0) : null; }
    }

    @Override public void close() {
        RuntimeCoordinatorR26.unbindBackgroundClaimR31(); helper.beforeWritable = null; db.beforeSuccessful = null;
        runtime.close();
        if (!runtime.pool().awaitResourcesTerminatedR31(2000)) throw new IllegalStateException("HOST_FIXTURE_RUNTIME_DID_NOT_DRAIN");
    }
}
