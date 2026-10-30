package android.database.sqlite;

import android.database.Cursor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Functional HOST-only framework shim (never packaged into APK): android.database.sqlite.SQLiteDatabase over sqlite-jdbc, binding rawQuery
 * arguments as strings exactly like Android does. Never packaged into the APK. */
public class SQLiteDatabase implements java.io.Closeable {
    public final Connection jdbc;
    /** Explicit HOST fault injection at the framework SQL boundary; never part of the APK. */
    public static volatile java.util.function.BiConsumer<SQLiteDatabase,String> hostBeforeQuery;
    private boolean hostReadOnly;
    public SQLiteDatabase(Connection jdbc) { this.jdbc = jdbc; }
    public static final int OPEN_READWRITE=0,OPEN_READONLY=1,CREATE_IF_NECESSARY=0x10000000,NO_LOCALIZED_COLLATORS=0x10;
    public interface CursorFactory {}
    public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){
        if(path==null)throw new IllegalArgumentException("HOST_SQLITE_PATH_REQUIRED");
        boolean readOnly=(flags&OPEN_READONLY)!=0,create=(flags&CREATE_IF_NECESSARY)!=0;
        if(!create&&!":memory:".equals(path)&&!new java.io.File(path).isFile())throw new SQLiteException("HOST_DATABASE_MISSING:"+path);
        try{
            java.util.Properties properties=new java.util.Properties();properties.setProperty("open_mode",readOnly?"1":create?"6":"2");
            SQLiteDatabase db=new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:"+path,properties));db.hostReadOnly=readOnly;
            db.execSQL("PRAGMA busy_timeout=1000");if(readOnly)try(Cursor c=db.rawQuery("PRAGMA query_only=1",null)){c.moveToFirst();}
            return db;
        }catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}
    }
    public static SQLiteDatabase openOrCreateDatabase(java.io.File path,CursorFactory factory){return openDatabase(path.getPath(),factory,CREATE_IF_NECESSARY);}
    public static SQLiteDatabase openOrCreateDatabase(String path,CursorFactory factory){return openDatabase(path,factory,CREATE_IF_NECESSARY);}
    public static SQLiteDatabase create(CursorFactory factory){return openDatabase(":memory:",factory,CREATE_IF_NECESSARY);}
    public boolean isOpen(){try{return !jdbc.isClosed();}catch(java.sql.SQLException e){return false;}}
    public boolean isReadOnly(){return hostReadOnly;}
    public boolean enableWriteAheadLogging(){if(hostReadOnly)return false;try(Cursor c=rawQuery("PRAGMA journal_mode=WAL",null)){return c.moveToFirst()&&("wal".equalsIgnoreCase(c.getString(0))||":memory:".equals(getPath()));}}
    public void disableWriteAheadLogging(){try(Cursor c=rawQuery("PRAGMA journal_mode=DELETE",null)){c.moveToFirst();}}
    public int getVersion(){try(Cursor c=rawQuery("PRAGMA user_version",null)){return c.moveToFirst()?c.getInt(0):0;}}
    public void setVersion(int version){if(version<0)throw new IllegalArgumentException("HOST_VERSION");execSQL("PRAGMA user_version="+version);}
    public boolean isWriteAheadLoggingEnabled(){try(Cursor c=rawQuery("PRAGMA journal_mode",null)){return c.moveToFirst()&&"wal".equalsIgnoreCase(c.getString(0));}}


    public void execSQL(String sql) { execSQL(sql, new Object[0]); }
    public void execSQL(String sql, Object[] args) {
        try (PreparedStatement p = jdbc.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i] instanceof Boolean ? (((Boolean) args[i]) ? 1 : 0) : args[i]);
            p.execute();
        } catch (java.sql.SQLException e) { throw new SQLiteException(e.getMessage()); }
    }
    public Cursor rawQuery(String sql, String[] args) {
        java.util.function.BiConsumer<SQLiteDatabase,String> hook=hostBeforeQuery;if(hook!=null)hook.accept(this,sql);
        try (PreparedStatement p = jdbc.prepareStatement(sql)) {
            if (args != null) for (int i = 0; i < args.length; i++) p.setString(i + 1, args[i]);
            List<Object[]> rows = new ArrayList<>(); String[]names;
            if(!p.execute())return new Rows(rows,new String[0]);
            try (ResultSet r = p.getResultSet()) {
                int n = r.getMetaData().getColumnCount(); names=new String[n];for(int i=0;i<n;i++)names[i]=r.getMetaData().getColumnLabel(i+1);
                while (r.next()) { Object[] o = new Object[n]; for (int i = 0; i < n; i++) o[i] = r.getObject(i + 1); rows.add(o); }
            }
            return new Rows(rows,names);
        } catch (java.sql.SQLException e) { throw new SQLiteException(e.getMessage()); }
    }
    public boolean inTransaction() { try { return !jdbc.getAutoCommit(); } catch (java.sql.SQLException e) { return false; } }
    public String getPath(){try{return jdbc.getMetaData().getURL().substring("jdbc:sqlite:".length());}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    private final java.util.ArrayDeque<Boolean> tx=new java.util.ArrayDeque<>(); private boolean failed;
    public void beginTransaction(){beginTransactionNonExclusive();}
    public void beginTransactionNonExclusive(){try{if(tx.isEmpty()){jdbc.setAutoCommit(false);failed=false;}tx.push(false);}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void setTransactionSuccessful(){if(tx.isEmpty()||tx.peek())throw new IllegalStateException("HOST_INVALID_TRANSACTION_SUCCESS");tx.pop();tx.push(true);}
    public void endTransaction(){if(tx.isEmpty())throw new IllegalStateException("HOST_NO_TRANSACTION");if(!tx.pop())failed=true;if(!tx.isEmpty())return;try{if(failed)jdbc.rollback();else jdbc.commit();jdbc.setAutoCommit(true);}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public static final int CONFLICT_NONE=0,CONFLICT_ROLLBACK=1,CONFLICT_ABORT=2,CONFLICT_FAIL=3,CONFLICT_IGNORE=4,CONFLICT_REPLACE=5;
    public long insertOrThrow(String table,String nullColumnHack,android.content.ContentValues v){return insertWithOnConflict(table,nullColumnHack,v,0);}
    public long insert(String table,String nullColumnHack,android.content.ContentValues v){try{return insertWithOnConflict(table,nullColumnHack,v,0);}catch(SQLiteException e){return -1;}}
    public long replace(String table,String nullColumnHack,android.content.ContentValues v){return insertWithOnConflict(table,nullColumnHack,v,5);}
    public long insertWithOnConflict(String table,String nullColumnHack,android.content.ContentValues v,int conflict){
      StringBuilder cols=new StringBuilder(),marks=new StringBuilder();List<Object>args=new ArrayList<>();
      for(java.util.Map.Entry<String,Object> e:v.valueSet()){if(cols.length()>0){cols.append(',');marks.append(',');}cols.append('"').append(e.getKey().replace("\"","\"\"")).append('"');marks.append('?');args.add(e.getValue());}
      String op=conflict==5?" OR REPLACE":conflict==4?" OR IGNORE":"";
      int changed=executeUpdate("INSERT"+op+" INTO "+table+"("+cols+") VALUES("+marks+")",args.toArray());
      if(changed==0)return -1;try(Cursor c=rawQuery("SELECT last_insert_rowid()",null)){return c.moveToFirst()?c.getLong(0):-1;}
    }
    public int delete(String table,String where,String[]whereArgs){return executeUpdate("DELETE FROM "+table+(where==null?"":" WHERE "+where),whereArgs==null?new Object[0]:whereArgs);}
    public int update(String table,android.content.ContentValues v,String where,String[]whereArgs){StringBuilder set=new StringBuilder();List<Object>args=new ArrayList<>();for(java.util.Map.Entry<String,Object>e:v.valueSet()){if(set.length()>0)set.append(',');set.append(e.getKey()).append("=?");args.add(e.getValue());}if(whereArgs!=null)java.util.Collections.addAll(args,whereArgs);return executeUpdate("UPDATE "+table+" SET "+set+(where==null?"":" WHERE "+where),args.toArray());}
    private int executeUpdate(String sql,Object[]args){try(PreparedStatement p=jdbc.prepareStatement(sql)){for(int i=0;i<args.length;i++)p.setObject(i+1,args[i] instanceof Boolean?((Boolean)args[i]?1:0):args[i]);return p.executeUpdate();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void close(){try{jdbc.close();}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}

    static final class Rows implements Cursor {
        final List<Object[]> rows; final String[]names; int at = -1;
        Rows(List<Object[]> rows,String[]names) { this.rows = rows; this.names=names; }
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
        public int getColumnIndex(String name){for(int i=0;i<names.length;i++)if(names[i].equalsIgnoreCase(name))return i;return -1;}
        public int getColumnIndexOrThrow(String name){int i=getColumnIndex(name);if(i<0)throw new IllegalArgumentException(name);return i;}
        public String[]getColumnNames(){return names.clone();} public int getColumnCount(){return names.length;} public int getPosition(){return at;} public boolean moveToPosition(int position){at=position;return at>=0&&at<rows.size();}
        public void close() {}
    }
}
