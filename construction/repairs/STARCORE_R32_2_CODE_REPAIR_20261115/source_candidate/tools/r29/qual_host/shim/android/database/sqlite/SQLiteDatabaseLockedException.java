package android.database.sqlite;

/** Android exception type used by the production queue; HOST-only, never packaged. */
public class SQLiteDatabaseLockedException extends SQLiteException {
    public SQLiteDatabaseLockedException(String message) { super(message); }
}
