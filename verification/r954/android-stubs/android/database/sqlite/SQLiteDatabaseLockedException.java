package android.database.sqlite;
public class SQLiteDatabaseLockedException extends SQLiteException {
    public SQLiteDatabaseLockedException() { super(); }
    public SQLiteDatabaseLockedException(String message) { super(message); }
}
