package android.database;
/** Host-only API shape used by the real Q08 read code. Never in app sources. */
public interface Cursor extends AutoCloseable {
    boolean moveToFirst();boolean moveToNext();boolean isNull(int column);
    String getString(int column);long getLong(int column);void close();
}
