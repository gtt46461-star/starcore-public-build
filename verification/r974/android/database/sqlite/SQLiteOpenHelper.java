package android.database.sqlite;
/** Host-only database handle adapter. Product DbStore and DatabaseCoordinator remain unmodified. */
public abstract class SQLiteOpenHelper {
 public static SQLiteDatabase host;
 public SQLiteOpenHelper(android.content.Context c,String name,SQLiteDatabase.CursorFactory f,int v){}
 public SQLiteDatabase getReadableDatabase(){return host;}public SQLiteDatabase getWritableDatabase(){return host;}
 public void setWriteAheadLoggingEnabled(boolean e){}public void close(){}
 public abstract void onCreate(SQLiteDatabase d);public abstract void onUpgrade(SQLiteDatabase d,int a,int b);
 public void onConfigure(SQLiteDatabase d){}public void onOpen(SQLiteDatabase d){}public void onDowngrade(SQLiteDatabase d,int a,int b){}
}
