package android.database.sqlite;
import android.database.Cursor;
import java.sql.*;
import java.util.*;
/** Host-only Android read adapter over actual native SQLite JDBC. String
 * bindings mirror Android rawQuery. It exposes no write method to production. */
public final class SQLiteDatabase {
    final Connection connection;
    public final List<String> queries=new ArrayList<>();
    public SQLiteDatabase(Connection connection){this.connection=connection;}
    public Cursor rawQuery(String sql,String[] args,android.os.CancellationSignal signal){
        if(signal!=null)signal.throwIfCanceled();return rawQuery(sql,args);
    }
    public Cursor rawQuery(String sql,String[] args){
        queries.add(sql);
        try(PreparedStatement p=connection.prepareStatement(sql)){
            if(args!=null)for(int i=0;i<args.length;i++){if(args[i]==null)throw new IllegalArgumentException("NULL_STRING_BIND");p.setString(i+1,args[i]);}
            List<Object[]> rows=new ArrayList<>();
            try(ResultSet r=p.executeQuery()){int columns=r.getMetaData().getColumnCount();while(r.next()){Object[] row=new Object[columns];for(int i=0;i<columns;i++)row[i]=r.getObject(i+1);rows.add(row);}}
            return new Cursor(){int at=-1;Object value(int i){return rows.get(at)[i];}
                public boolean moveToFirst(){at=0;return !rows.isEmpty();}public boolean moveToNext(){return ++at<rows.size();}
                public boolean isNull(int i){return value(i)==null;}public String getString(int i){return value(i)==null?null:String.valueOf(value(i));}
                public long getLong(int i){Object x=value(i);return x instanceof Number?((Number)x).longValue():Long.parseLong(String.valueOf(x));}public void close(){}
            };
        }catch(SQLException failure){throw new IllegalStateException("HOST_NATIVE_SQLITE_QUERY_FAILED",failure);}
    }
}
