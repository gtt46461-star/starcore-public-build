package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
/** Test transport only: real SQLite transactions, no Android lock/scheduler claim. */
public final class DatabaseCoordinator {
    public interface WriteCallable<T>{T call(SQLiteDatabase db);}
    private final SQLiteDatabase db;
    public DatabaseCoordinator(SQLiteDatabase db){this.db=db;}
    public <T>T runAtomicWriteOrThrowF01(String tag,WriteCallable<T> work){
        db.beginTransaction();try{T result=work.call(db);db.setTransactionSuccessful();return result;}finally{db.endTransaction();}
    }
}
