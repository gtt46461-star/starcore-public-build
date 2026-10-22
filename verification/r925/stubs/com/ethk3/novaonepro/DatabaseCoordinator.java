package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

/** Synchronous host transaction boundary backed by the real SQLite bridge. */
public final class DatabaseCoordinator {
    private final SQLiteDatabase db;

    public DatabaseCoordinator(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("HOST_COORDINATOR_DB_NULL");
        this.db = db;
    }

    public boolean isR811ResearchSchemaReady(){return ResearchSchemaCoordinatorR926.summary().startsWith("READY|");}

    public SQLiteDatabase writable() { return db; }

    public interface WriteRunnable { void run(SQLiteDatabase db); }
    public interface WriteCallable<T> { T call(SQLiteDatabase db); }

    public <T> T runWrite(String tag, WriteCallable<T> work, T fallback) {
        if (work == null) return fallback;
        try { return work.call(db); }
        catch (RuntimeResourceAuditR927.BudgetExceeded deadline) { throw deadline; }
        catch (ResearchWorkBudgetR927.Yield yield) { throw yield; }
        catch (Throwable failure) { return fallback; }
    }
    public boolean runWrite(String tag, final WriteRunnable work) {
        if (work == null) return false;
        return Boolean.TRUE.equals(runWrite(tag, new WriteCallable<Boolean>() {
            public Boolean call(SQLiteDatabase db) { work.run(db); return true; }
        }, Boolean.FALSE));
    }

    public <T> T runAtomicWrite(String tag, final WriteCallable<T> work, T fallback) {
        if (work == null) return fallback;
        return runWrite(tag, new WriteCallable<T>() {
            public T call(SQLiteDatabase db) {
                boolean owns = !db.inTransaction();
                if (owns) db.beginTransaction();
                TransactionSchemaCacheR941.Scope schemaScope = TransactionSchemaCacheR941.enter(db);
                try {
                    T result = work.call(db);
                    if (owns) db.setTransactionSuccessful();
                    return result;
                } finally { try { if (owns) db.endTransaction(); } finally { TransactionSchemaCacheR941.leave(schemaScope); } }
            }
        }, fallback);
    }

    public static final class TransactionContext { public final String transactionHash=V51ChecksumEngine.sha256("HOST_REAL_SQLITE_TRANSACTION"); }
    public interface TransactionCallable<T> {
        T call(SQLiteDatabase db, TransactionContext context) throws Exception;
    }
    public static final class TransactionResult<T> {
        public final boolean replay=false;
        public final String transactionHash=V51ChecksumEngine.sha256("HOST_REAL_SQLITE_TRANSACTION");
        public final boolean success;
        public final T value;
        public final String code;
        TransactionResult(boolean success, T value, String code) {
            this.success = success;
            this.value = value;
            this.code = code;
        }
    }

    public <T> TransactionResult<T> runTransaction(String operation,String target,String key,TransactionCallable<T> task,T fallback) {
        return runDeferredTransactionR36(target,key,task,fallback);
    }

    public <T> TransactionResult<T> runDeferredTransactionR36(
            String target, String key, TransactionCallable<T> task, T fallback) {
        db.beginTransaction();
        try {
            T value = task.call(db, new TransactionContext());
            db.setTransactionSuccessful();
            return new TransactionResult<T>(true, value, "OK");
        } catch (Exception error) {
            return new TransactionResult<T>(false, fallback, error.toString());
        } finally {
            db.endTransaction();
        }
    }

    public boolean runAtomicWrite(String tag, final WriteRunnable work) {
        if (work == null) return false;
        return Boolean.TRUE.equals(runAtomicWrite(tag, new WriteCallable<Boolean>() {
            public Boolean call(SQLiteDatabase db) { work.run(db); return true; }
        }, Boolean.FALSE));
    }
}
