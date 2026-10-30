package android.database.sqlite;

import android.database.Cursor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** HOST shim (tools/r29/qual_host only): android.database.sqlite.SQLiteDatabase over sqlite-jdbc, binding rawQuery
 * arguments as strings exactly like Android does. Never packaged into the APK. */
public class SQLiteDatabase implements java.io.Closeable {
    public final Connection jdbc;
    public SQLiteDatabase(Connection jdbc) { this.jdbc = jdbc; }
    public static final int CONFLICT_NONE=0,CONFLICT_IGNORE=4,CONFLICT_REPLACE=5;
    public long insertWithOnConflict(String table,String nullColumnHack,android.content.ContentValues values,int conflict){
        StringBuilder cols=new StringBuilder(),binds=new StringBuilder();List<Object> args=new ArrayList<>();
        for(java.util.Map.Entry<String,Object> e:values.valueSet()){if(cols.length()>0){cols.append(',');binds.append(',');}cols.append('"').append(e.getKey().replace("\"","\"\"")).append('"');binds.append('?');args.add(e.getValue());}
        String sql="INSERT "+(conflict==CONFLICT_IGNORE?"OR IGNORE ":conflict==CONFLICT_REPLACE?"OR REPLACE ":"")+"INTO \""+table.replace("\"","\"\"")+"\"("+cols+") VALUES("+binds+")";
        execSQL(sql,args.toArray());try(Cursor c=rawQuery("SELECT changes(),last_insert_rowid()",null)){return c.moveToFirst()&&c.getLong(0)>0?c.getLong(1):-1;}
    }
    public int update(String table,android.content.ContentValues values,String where,String[] whereArgs){
        StringBuilder set=new StringBuilder();List<Object> args=new ArrayList<>();for(java.util.Map.Entry<String,Object> e:values.valueSet()){if(set.length()>0)set.append(',');set.append('"').append(e.getKey().replace("\"","\"\"")).append("\"=?");args.add(e.getValue());}if(whereArgs!=null)java.util.Collections.addAll(args,(Object[])whereArgs);
        execSQL("UPDATE \""+table.replace("\"","\"\"")+"\" SET "+set+(where==null||where.isEmpty()?"":" WHERE "+where),args.toArray());try(Cursor c=rawQuery("SELECT changes()",null)){return c.moveToFirst()?c.getInt(0):0;}
    }

    public void execSQL(String sql) { execSQL(sql, new Object[0]); }
    public void execSQL(String sql, Object[] args) {
        try (PreparedStatement p = jdbc.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i] instanceof Boolean ? (((Boolean) args[i]) ? 1 : 0) : args[i]);
            p.execute();
        } catch (java.sql.SQLException e) { throw new SQLiteException(e.getMessage()); }
    }
    public Cursor rawQuery(String sql, String[] args) {
        try (PreparedStatement p = jdbc.prepareStatement(sql)) {
            if (args != null) for (int i = 0; i < args.length; i++) p.setString(i + 1, args[i]);
            List<Object[]> rows = new ArrayList<>();String[] names;
            try (ResultSet r = p.executeQuery()) {
                int n = r.getMetaData().getColumnCount();names=new String[n];for(int i=0;i<n;i++)names[i]=r.getMetaData().getColumnLabel(i+1);
                while (r.next()) { Object[] o = new Object[n]; for (int i = 0; i < n; i++) o[i] = r.getObject(i + 1); rows.add(o); }
            }
            return new Rows(rows,names);
        } catch (java.sql.SQLException e) { throw new SQLiteException(e.getMessage()); }
    }
    public boolean inTransaction() { try { return !jdbc.getAutoCommit(); } catch (java.sql.SQLException e) { return false; } }
    public String getPath(){try{return jdbc.getMetaData().getURL().substring("jdbc:sqlite:".length());}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    private boolean successful;
    public void beginTransactionNonExclusive(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void setTransactionSuccessful(){successful=true;}
    public void endTransaction(){try{if(successful)jdbc.commit();else jdbc.rollback();jdbc.setAutoCommit(true);}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void close(){try{jdbc.close();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}

    static final class Rows implements Cursor {
        final List<Object[]> rows;final String[] names; int at = -1;
        Rows(List<Object[]> rows,String[] names) { this.rows = rows;this.names=names; }
        public int getColumnIndex(String name){if(name==null)return -1;int dot=name.lastIndexOf('.');if(dot>=0)name=name.substring(dot+1);for(int i=0;i<names.length;i++)if(names[i].equalsIgnoreCase(name))return i;return -1;}
        public boolean moveToNext() { return ++at < rows.size(); }
        public boolean moveToFirst() { at = 0; return !rows.isEmpty(); }
        Object v(int i) { return rows.get(at)[i]; }
        public String getString(int i) { Object o = v(i); return o == null ? null : String.valueOf(o); }
        public long getLong(int i) { Object o = v(i); return o == null ? 0 : o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString()); }
        public int getInt(int i) { return (int) getLong(i); }
        public double getDouble(int i) { Object o = v(i); return o == null ? 0 : o instanceof Number ? ((Number) o).doubleValue() : Double.parseDouble(o.toString()); }
        public byte[] getBlob(int i) { return (byte[]) v(i); }
        public boolean isNull(int i) { return v(i) == null; }
        public int getCount() { return rows.size(); }
        public int getType(int i) {
            Object o=v(i);
            if(o==null)return Cursor.FIELD_TYPE_NULL;
            if(o instanceof byte[])return Cursor.FIELD_TYPE_BLOB;
            if(o instanceof Float || o instanceof Double)return Cursor.FIELD_TYPE_FLOAT;
            if(o instanceof Number)return Cursor.FIELD_TYPE_INTEGER;
            return Cursor.FIELD_TYPE_STRING;
        }
        public void close() {}
    }
}
