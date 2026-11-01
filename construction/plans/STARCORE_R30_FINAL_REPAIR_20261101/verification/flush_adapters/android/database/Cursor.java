package android.database;
public interface Cursor extends AutoCloseable { boolean moveToFirst();long getLong(int i);void close(); }
