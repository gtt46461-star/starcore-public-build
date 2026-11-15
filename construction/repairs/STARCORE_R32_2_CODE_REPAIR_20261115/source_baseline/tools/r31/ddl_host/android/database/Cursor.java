package android.database;
/** HOST native-SQLite/JDBC result, not Android lifecycle evidence. */
public interface Cursor extends java.io.Closeable {
    boolean moveToNext();boolean moveToFirst();String getString(int i);long getLong(int i);int getInt(int i);double getDouble(int i);byte[] getBlob(int i);int getType(int i);boolean isNull(int i);int getCount();int getColumnIndex(String name);int getColumnCount();String[] getColumnNames();default String getColumnName(int column){return getColumnNames()[column];}void close();
}
