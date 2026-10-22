package android.database.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

public class SQLiteDatabase {
    /** Exact nested type required by SQLiteOpenHelper's public constructor. */
    public interface CursorFactory {}

    public static final int CONFLICT_NONE = 0;
    public static final int CONFLICT_ABORT = 2;
    public static final int OPEN_READONLY = 1;
    public static final int CONFLICT_IGNORE = 4;
    public static final int CONFLICT_REPLACE = 5;

    public static SQLiteDatabase openDatabase(String path, CursorFactory factory, int flags){ return new SQLiteDatabase(); }
    public void execSQL(String sql) {}
    public void execSQL(String sql, Object[] bindArgs) {}
    public long insert(String table, String nullColumnHack, ContentValues values) { return 0; }
    public long insertOrThrow(String table, String nullColumnHack, ContentValues values) { return 0; }
    public long insertWithOnConflict(String table, String nullColumnHack, ContentValues values, int conflictAlgorithm) { return 0; }
    public int update(String table, ContentValues values, String whereClause, String[] whereArgs) { return 0; }
    public int delete(String table, String whereClause, String[] whereArgs) { return 0; }
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy) { return null; }
    public Cursor query(String table, String[] columns, String selection, String[] selectionArgs,
                        String groupBy, String having, String orderBy, String limit) { return null; }
    public Cursor rawQuery(String sql, String[] selectionArgs) { return null; }
    public Cursor rawQuery(String sql, String[] selectionArgs, android.os.CancellationSignal signal) { return null; }
    public SQLiteStatement compileStatement(String sql) { return new SQLiteStatement(); }
    public void beginTransaction() {}
    public void beginTransactionNonExclusive() {}
    public void setTransactionSuccessful() {}
    public void endTransaction() {}
    public boolean inTransaction() { return false; }
    public boolean isOpen() { return true; }
    public boolean isReadOnly() { return false; }
    public String getPath() { return "stub.db"; }
    public int getVersion() { return 0; }
    public void setVersion(int version) {}
    public void setForeignKeyConstraintsEnabled(boolean enable) {}
    public void close() {}
}
