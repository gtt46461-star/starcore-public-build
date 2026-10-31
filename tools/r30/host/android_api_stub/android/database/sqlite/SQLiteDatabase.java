package android.database.sqlite;
public final class SQLiteDatabase implements java.io.Closeable { public static final int OPEN_READONLY=1;
 public static SQLiteDatabase openDatabase(String p, Object f, int flags){return null;} public static SQLiteDatabase openOrCreateDatabase(java.io.File f, Object x){return null;}
 public android.database.Cursor rawQuery(String sql, String[] a){return null;} public void execSQL(String s){} public void execSQL(String s, Object[] a){}
 public boolean enableWriteAheadLogging(){return true;} public void beginTransactionNonExclusive(){} public void beginTransaction(){} public void setTransactionSuccessful(){} public void endTransaction(){} public boolean inTransaction(){return false;} public String getPath(){return "";} public void close(){} }
