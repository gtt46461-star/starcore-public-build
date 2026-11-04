package android.database.sqlite;

import android.database.Cursor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** HOST shim (tools/r29/qual_host only): android.database.sqlite.SQLiteDatabase over sqlite-jdbc, binding rawQuery
 * arguments as strings exactly like Android does. Never packaged into the APK. */
public class SQLiteDatabase {
    public final Connection jdbc;
    /** HOST API parity: actually close the native JDBC reader handle. */
    public void close(){try{jdbc.close();}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
    public SQLiteDatabase(Connection jdbc) { this.jdbc = jdbc; }

    public void execSQL(String sql) { execSQL(sql, new Object[0]); }
    public void execSQL(String sql, Object[] args) {
        try (PreparedStatement p = jdbc.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i] instanceof Boolean ? (((Boolean) args[i]) ? 1 : 0) : args[i]);
            p.execute();
        } catch (java.sql.SQLException e) { throw new SQLiteException(e.getMessage()); }
    }
    public Cursor rawQuery(String sql, String[] args) {
        // SQLiteProgram.bindAllArgsAsStrings calls bindString even for an IS ?
        // predicate. Android rejects null instead of binding JDBC SQL NULL.
        if (args != null) for (int i = 0; i < args.length; i++) {
            if (args[i] == null) throw new IllegalArgumentException("the bind value at index " + (i + 1) + " is null");
        }
        try (PreparedStatement p = jdbc.prepareStatement(sql)) {
            if (args != null) for (int i = 0; i < args.length; i++) p.setString(i + 1, args[i]);
            List<Object[]> rows = new ArrayList<>(); int columns;
            try (ResultSet r = p.executeQuery()) {
                int n = r.getMetaData().getColumnCount(); columns = n;
                while (r.next()) { Object[] o = new Object[n]; for (int i = 0; i < n; i++) o[i] = r.getObject(i + 1); rows.add(o); }
            }
            return new Rows(rows, columns);
        } catch (java.sql.SQLException e) { throw new SQLiteException(e.getMessage()); }
    }
    public boolean inTransaction() { try { return !jdbc.getAutoCommit(); } catch (java.sql.SQLException e) { return false; } }
    public boolean isReadOnly() { try { return jdbc.isReadOnly(); } catch (java.sql.SQLException e) { throw new SQLiteException(e.getMessage()); } }


    private boolean successful;
    public Runnable beforeSuccessful;
    public static final int OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;
    public interface CursorFactory {}
    public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){try{return new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:file:"+path+"?mode=ro"));}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
    public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
    public void setTransactionSuccessful(){if(beforeSuccessful!=null)beforeSuccessful.run();successful=true;}
    public void endTransaction(){try{if(successful)jdbc.commit();else jdbc.rollback();}catch(java.sql.SQLException failure){try{jdbc.rollback();}catch(java.sql.SQLException ignored){}throw new SQLiteException(failure.getMessage());}finally{successful=false;try{jdbc.setAutoCommit(true);}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}}
    public String getPath(){try{return jdbc.getMetaData().getURL().substring("jdbc:sqlite:".length());}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}
    static final class Rows implements Cursor {
        final List<Object[]> rows; final int columns; int at = -1;
        Rows(List<Object[]> rows) { this(rows, rows.isEmpty() ? -1 : rows.get(0).length); }
        Rows(List<Object[]> rows, int columns) { this.rows = rows; this.columns = columns; }
        public int getColumnCount() { if(columns<0)throw new UnsupportedOperationException("HOST_CURSOR_COLUMN_METADATA_UNAVAILABLE");return columns; }
        public boolean moveToNext() { return ++at < rows.size(); }
        public boolean moveToFirst() { at = 0; return !rows.isEmpty(); }
        Object v(int i) { return rows.get(at)[i]; }
        public String getString(int i) { Object o = v(i); return o == null ? null : String.valueOf(o); }
        public long getLong(int i) { Object o = v(i); return o == null ? 0 : o instanceof Number ? ((Number) o).longValue() : Long.parseLong(o.toString()); }
        public int getInt(int i) { return (int) getLong(i); }
        public double getDouble(int i) { Object o = v(i); return o == null ? 0 : o instanceof Number ? ((Number) o).doubleValue() : Double.parseDouble(o.toString()); }
        public byte[] getBlob(int i) { return (byte[]) v(i); }
        public int getType(int i) {
            Object value = v(i);
            if (value == null) return 0;
            if (value instanceof byte[]) return 4;
            if (value instanceof Float || value instanceof Double || value instanceof java.math.BigDecimal) return 2;
            if (value instanceof Number) return 1;
            return 3;
        }
        public boolean isNull(int i) { return v(i) == null; }
        public int getCount() { return rows.size(); }
        public void close() {}
    }
}
