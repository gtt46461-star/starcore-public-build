package android.database.sqlite;
import java.util.*;import android.database.Cursor;
public final class SQLiteDatabase {
 public final Map<String,Long> rows=new HashMap<>();
 public void execSQL(String sql) { if(!sql.startsWith("CREATE"))throw new UnsupportedOperationException(); }
 public void execSQL(String sql,Object[] args){if(sql.startsWith("INSERT"))rows.put(args[0]+"/"+args[2]+"/"+args[3],((Number)args[4]).longValue());else if(!sql.startsWith("DELETE"))throw new UnsupportedOperationException();}
 public Cursor rawQuery(String sql,String[] args){throw new UnsupportedOperationException("report not under test");}
}
