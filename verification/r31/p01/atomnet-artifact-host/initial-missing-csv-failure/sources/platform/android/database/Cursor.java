package android.database;

/** HOST shim (tools/r29/qual_host only): the subset of android.database.Cursor the R29 qualification path calls,
 * backed by a materialized JDBC result. Never packaged into the APK. */
public interface Cursor extends java.io.Closeable {
    boolean moveToNext();
    boolean moveToFirst();
    String getString(int i);
    long getLong(int i);
    int getInt(int i);
    double getDouble(int i);
    byte[] getBlob(int i);
    int getType(int i);
    boolean isNull(int i);
    int getCount();
    default int getColumnCount() { throw new UnsupportedOperationException("HOST_CURSOR_COLUMN_METADATA_UNAVAILABLE"); }
    void close();
}
