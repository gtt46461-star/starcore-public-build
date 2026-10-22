package android.database.sqlite;

import android.content.Context;

/** Compile-time API-28 surface. Never package this class into the APK. */
public abstract class SQLiteOpenHelper {
    public SQLiteOpenHelper(
            Context context,
            String name,
            SQLiteDatabase.CursorFactory factory,
            int version) {}

    public abstract void onCreate(SQLiteDatabase db);
    public abstract void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new SQLiteException("downgrade");
    }

    public void onConfigure(SQLiteDatabase db) {}
    public void onOpen(SQLiteDatabase db) {}
    public void setWriteAheadLoggingEnabled(boolean enabled) {}
    public SQLiteDatabase getWritableDatabase() { return new SQLiteDatabase(); }
    public SQLiteDatabase getReadableDatabase() { return new SQLiteDatabase(); }
    public synchronized void close() {}
}
