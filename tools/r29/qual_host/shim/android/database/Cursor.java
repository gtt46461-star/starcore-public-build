package android.database;

/** HOST shim (tools/r29/qual_host only): the subset of android.database.Cursor the R29 qualification path calls,
 * backed by a materialized JDBC result. Never packaged into the APK. */
public interface Cursor extends java.io.Closeable {
    int FIELD_TYPE_NULL=0, FIELD_TYPE_INTEGER=1, FIELD_TYPE_FLOAT=2, FIELD_TYPE_STRING=3, FIELD_TYPE_BLOB=4;
    boolean moveToNext();
    boolean moveToFirst();
    String getString(int i);
    long getLong(int i);
    int getInt(int i);
    double getDouble(int i);
    byte[] getBlob(int i);
    boolean isNull(int i);
    int getCount();
    int getType(int i);
    void close();
}
