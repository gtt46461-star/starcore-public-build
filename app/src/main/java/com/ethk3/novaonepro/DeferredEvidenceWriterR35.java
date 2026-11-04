package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Single-thread writer for the 100-number/1200-action/full proof ledger. */
public final class DeferredEvidenceWriterR35 {
    private static final ConcurrentHashMap<String, V51RuntimeResult> SNAPSHOTS =
            new ConcurrentHashMap<String, V51RuntimeResult>();
    private static final ThreadPoolExecutor EXECUTOR =
            new ThreadPoolExecutor(
                    1,
                    1,
                    30L,
                    TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(AppConfig.R35_DEFERRED_QUEUE_CAPACITY),
                    new ThreadFactory() {
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "starcore_r35_deferred_evidence");
                            t.setDaemon(true);
                            try {
                                t.setPriority(Thread.MIN_PRIORITY);
                            } catch (Throwable ignore) {
                            }
                            return t;
                        }
                    },
                    new ThreadPoolExecutor.AbortPolicy());

    static {
        try {
            EXECUTOR.allowCoreThreadTimeOut(true);
        } catch (Throwable ignore) {
        }
    }

    private DeferredEvidenceWriterR35() {}

    public static EnqueueResult enqueue(final DbStore store, final V51RuntimeResult result) {
        if (store == null || result == null || !result.success || result.context == null) {
            return new EnqueueResult(false, "", "INVALID_INPUT");
        }
        final String target = safe(result.context.targetIssue);
        if (target.length() == 0) return new EnqueueResult(false, target, "TARGET_EMPTY");
        final String bundle = safe(result.checksum);
        upsertJob(store, target, bundle, "PENDING", "", false);
        SNAPSHOTS.put(target, result);
        try {
            EXECUTOR.execute(
                    new Runnable() {
                        public void run() {
                            writeOne(store, target, bundle);
                        }
                    });
            store.setState("r35_deferred_queue_depth", String.valueOf(EXECUTOR.getQueue().size()));
            return new EnqueueResult(true, target, "QUEUED_SINGLE_THREAD");
        } catch (Throwable e) {
            SNAPSHOTS.remove(target);
            upsertJob(
                    store,
                    target,
                    bundle,
                    "RETRY_REQUIRED_QUEUE_FULL",
                    e.getClass().getSimpleName(),
                    true);
            RuntimeHealthLedgerR35.record(
                    store,
                    "DEFERRED_EVIDENCE",
                    target,
                    "QUEUE_REJECTED",
                    0L,
                    0,
                    1,
                    0,
                    e.getClass().getSimpleName());
            return new EnqueueResult(false, target, "QUEUE_REJECTED");
        }
    }

    private static void writeOne(DbStore store, String target, String bundle) {
        long started = System.currentTimeMillis();
        V51RuntimeResult result = SNAPSHOTS.remove(target);
        if (result == null) {
            upsertJob(
                    store,
                    target,
                    bundle,
                    "RETRY_REQUIRED_SNAPSHOT_MISSING",
                    "IN_MEMORY_SNAPSHOT_NOT_AVAILABLE",
                    true);
            return;
        }
        upsertJob(store, target, bundle, "RUNNING", "", true);
        try {
            V51Repository.persist(store, result);
            upsertJob(store, target, bundle, "COMPLETED", "", false);
            RuntimeHealthLedgerR35.record(
                    store,
                    "DEFERRED_EVIDENCE",
                    target,
                    "SUCCESS",
                    System.currentTimeMillis() - started,
                    0,
                    1,
                    0,
                    "100_NUMBER_AND_1200_ACTION_LEDGER_COMMITTED");
            store.setState("r35_deferred_last_completed_target", target);
        } catch (OutOfMemoryError oom) {
            upsertJob(store, target, bundle, "RETRY_REQUIRED_OOM", "OOM_CAUGHT", true);
            RuntimeHealthLedgerR35.recordOom(store, "DEFERRED_EVIDENCE", target, oom);
        } catch (Throwable e) {
            upsertJob(
                    store,
                    target,
                    bundle,
                    "RETRY_REQUIRED_WRITE_ERROR",
                    e.getClass().getSimpleName() + ":" + safe(e.getMessage()),
                    true);
            RuntimeHealthLedgerR35.record(
                    store,
                    "DEFERRED_EVIDENCE",
                    target,
                    "WRITE_ERROR",
                    System.currentTimeMillis() - started,
                    0,
                    1,
                    0,
                    e.getClass().getSimpleName());
        }
    }

    public static int pendingCount(DbStore store) {
        if (store == null) return 0;
        Cursor c = null;
        try {
            SQLiteDatabase db = store.readable();
            requireJobSchemaR31(db);
            c =
                    db.rawQuery(
                            "SELECT COUNT(1) FROM "
                                    + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                    + " WHERE status<>'COMPLETED'",
                            null);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } catch (DatabaseCoordinator.SchemaPendingR31 pending) {
            throw pending;
        } catch (Throwable e) {
            Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(e);if(fatal!=null)throw fatal;
            throw new IllegalStateException("R35_DEFERRED_PENDING_UNAVAILABLE",e);
        } finally {
            if (c != null) c.close();
        }
    }

    public static void markProcessRestartRecoveryState(DbStore store) {
        if (store == null) return;
        int pending = pendingCount(store);
        store.setState("r35_deferred_restart_pending", String.valueOf(pending));
        if (pending > 0) {
            store.setState(
                    "r35_deferred_restart_policy",
                    "CANONICAL_SHADOW_PRESERVED;HEAVY_EVIDENCE_REQUIRES_NEW_PRE_DRAW_SNAPSHOT;NO_POST_DRAW_MODEL_REPLAY");
        }
    }

    private static void upsertJob(
            DbStore store,
            String target,
            String bundle,
            String status,
            String error,
            boolean incrementAttempt) {
        if (store == null) return;
        store.coordinatorR36().runAtomicWriteOrThrowF01(
                DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R35_DEFERRED_JOB_STATUS"),db->{
            requireJobSchemaR31(db);
            int attempts = 0;
            long created = System.currentTimeMillis();
            boolean existing=false;
            try(Cursor c = db.rawQuery(
                            "SELECT attempts,created_at,bundle_hash,job_type FROM "
                                    + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                                    + " WHERE target_issue=?",
                            new String[] {target})){
            if (c.moveToFirst()) {
                existing=true;
                if(!bundle.equals(c.getString(2)))throw new IllegalStateException("R31_STALE_DEFERRED_BUNDLE");
                if(!"V51_FULL_EVIDENCE".equals(c.getString(3)))throw new IllegalStateException("R31_STALE_DEFERRED_JOB_PROTOCOL_OWNER");
                attempts = c.getInt(0);
                created = c.getLong(1);
            }
            }
            if (incrementAttempt) attempts++;
            ContentValues cv = new ContentValues();
            cv.put("status", status);
            cv.put("attempts", attempts);
            cv.put("updated_at", System.currentTimeMillis());
            cv.put("last_error", trim(error, 700));
            if(!existing){
                cv.put("target_issue",target);cv.put("job_type","V51_FULL_EVIDENCE");
                cv.put("bundle_hash",bundle);cv.put("created_at",created);
                cv.put(
                    "record_hash",
                    V51ChecksumEngine.sha256("R35_DEFERRED|" + target + "|" + bundle));
            }
            // The R36 owner stores immutable snapshot/lease columns in this same durable row.
            // REPLACE would silently erase them on an R35 status/error update.
            if(existing){if(db.update(DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,cv,"target_issue=? AND bundle_hash=?",new String[]{target,bundle})!=1)throw new IllegalStateException("R31_STALE_DEFERRED_JOB_STATUS");}
            else if(db.insertWithOnConflict(DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,null,cv,SQLiteDatabase.CONFLICT_IGNORE)<0)throw new IllegalStateException("R35_DEFERRED_JOB_INSERT_FAILED");
            return 0;
        });
    }

    private static void requireJobSchemaR31(SQLiteDatabase db){
        DatabaseCoordinator.requireSchemaObjectsR31(db,"R35_DEFERRED_JOB",new String[][]{{DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB,
                "target_issue,job_type,status,attempts,bundle_hash,created_at,updated_at,last_error,record_hash"}},new String[][]{{"index","idx_r35_deferred_status"}});
    }

    private static String safe(String x) {
        return x == null ? "" : x;
    }

    private static String trim(String x, int max) {
        x = safe(x);
        return x.length() <= max ? x : x.substring(0, max);
    }

    public static final class EnqueueResult {
        public final boolean success;
        public final String targetIssue, status;

        EnqueueResult(boolean success, String target, String status) {
            this.success = success;
            targetIssue = safe(target);
            this.status = safe(status);
        }
    }
}
