package android.database;
/** Real host-SQLite scalar adapter; no canned product result. */
public final class DatabaseUtils {
 public static long longForQuery(android.database.sqlite.SQLiteDatabase db,String sql,String[] args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new IllegalStateException("NO_SCALAR");return c.getLong(0);}}
 public static String stringForQuery(android.database.sqlite.SQLiteDatabase db,String sql,String[] args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new IllegalStateException("NO_SCALAR");return c.getString(0);}}
 public static long queryNumEntries(android.database.sqlite.SQLiteDatabase d,String t){return longForQuery(d,"SELECT COUNT(*) FROM "+t,null);}
}
