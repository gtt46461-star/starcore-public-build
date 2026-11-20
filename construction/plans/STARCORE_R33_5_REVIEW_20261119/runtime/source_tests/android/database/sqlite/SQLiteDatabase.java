package android.database.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

/** Host boundary. The probe supplies row values and records write calls. */
public abstract class SQLiteDatabase {
    public abstract boolean inTransaction();
    public abstract Cursor rawQuery(String sql, String[] args);
    public abstract long insertWithOnConflict(String table,String empty,ContentValues values,int conflict);
    public abstract int update(String table,ContentValues values,String predicate,String[] args);
}
