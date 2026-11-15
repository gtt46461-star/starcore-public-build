package android.database.sqlite;
/** HOST connection ownership adapter; migrations are invoked explicitly by the harness. */
public abstract class SQLiteOpenHelper {
    public static SQLiteDatabase hostDatabase;
    public SQLiteOpenHelper(android.content.Context context,String name,SQLiteDatabase.CursorFactory factory,int version){}
    public SQLiteDatabase getWritableDatabase(){return hostDatabase;}public SQLiteDatabase getReadableDatabase(){return hostDatabase;}public void close(){}public void setWriteAheadLoggingEnabled(boolean enabled){}public void onConfigure(SQLiteDatabase db){}public void onOpen(SQLiteDatabase db){}public abstract void onCreate(SQLiteDatabase db);public abstract void onUpgrade(SQLiteDatabase db,int before,int after);public void onDowngrade(SQLiteDatabase db,int before,int after){}
}
