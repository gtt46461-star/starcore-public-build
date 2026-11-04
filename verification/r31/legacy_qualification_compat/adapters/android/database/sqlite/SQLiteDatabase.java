package android.database.sqlite;

import android.content.ContentValues;
import android.database.Cursor;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.sql.*;
import java.util.*;

/** Native SQLite/JDBC adapter. Counts every production API SQL call and bind
 * inside each real transaction, including CAS, fence, progress and receipts. */
public class SQLiteDatabase implements AutoCloseable {
    public static final int CONFLICT_NONE=0,CONFLICT_IGNORE=4,CONFLICT_REPLACE=5,OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;
    public interface CursorFactory{}
    public final Connection jdbc;private final boolean readOnly;
    public boolean receiptInsertGuardInstalledR31;
    public boolean trackVmStepsR31;
    public static String scenario="BOOTSTRAP";
    public static final List<Map<String,Object>> frames=new ArrayList<>();
    public static final List<String> outsideDdl=new ArrayList<>();
    public static int failTransaction=-1,interruptTransaction=-1;
    public static String failSqlPrefix="INSERT INTO r29_qual_receipt";
    public static int txCounter;
    private Frame frame;private boolean successful;
    static final class Frame {
        final int id=++txCounter;final String label=scenario;final long start=System.nanoTime();
        long binds,rows,vmTicks;int reads,writes,fences,receipts,receiptGuardReads;boolean faulted;
        final List<Object> sql=new ArrayList<>();
    }
    public SQLiteDatabase(Connection c){this(c,false);}
    private SQLiteDatabase(Connection c,boolean ro){jdbc=c;readOnly=ro;}
    private static long bytes(Object[] args){long n=0;if(args!=null)for(Object a:args)n+=a instanceof byte[]?((byte[])a).length:a instanceof String?((String)a).getBytes(java.nio.charset.StandardCharsets.UTF_8).length:8;return n;}
    private void log(String api,String sql,Object[] args){
        String canonical=sql.trim().toUpperCase(Locale.ROOT);
        if(frame==null){if(canonical.matches("(?s)^(CREATE|ALTER|DROP|REINDEX|VACUUM|ATTACH|DETACH)\\b.*"))outsideDdl.add(scenario+"|"+sql);return;}
        long bound=bytes(args);frame.binds+=bound;
        boolean read=api.equals("read");if(read)frame.reads++;else frame.writes++;
        if(sql.contains("r26_task t JOIN r26_session"))frame.fences++;
        if(sql.contains("r31_commit_receipt"))frame.receipts++;
        Map<String,Object> row=new LinkedHashMap<>();row.put("api",api);row.put("sql",sql);row.put("bindBytes",bound);frame.sql.add(row);
        if(!frame.faulted&&frame.id==interruptTransaction&&!read){frame.faulted=true;Thread.currentThread().interrupt();}
        if(!frame.faulted&&frame.id==failTransaction&&sql.startsWith(failSqlPrefix)){
            frame.faulted=true;throw new SQLiteException("HOST_INJECTED_SQLITE_IOERR: transaction="+frame.id);
        }
    }
    private PreparedStatement prepare(String sql,Object[] args)throws SQLException{
        PreparedStatement p=jdbc.prepareStatement(sql);if(args!=null)for(int i=0;i<args.length;i++)p.setObject(i+1,args[i] instanceof Boolean?((Boolean)args[i]?1:0):args[i]);return p;
    }
    private boolean protectedInsertReadR31(String sql){boolean protectedInsert=frame!=null&&receiptInsertGuardInstalledR31&&sql.trim().matches("(?is)^INSERT(?:\\s+OR\\s+\\w+)?\\s+INTO\\s+\"?r29_qual_receipt\"?(?:\\s|\\().*");if(protectedInsert)frame.receiptGuardReads++;return protectedInsert;}
    private void protectedDuplicateReadR31(boolean protectedInsert){if(protectedInsert&&frame!=null)frame.receiptGuardReads++;}
    public void execSQL(String sql){execSQL(sql,new Object[0]);}
    public void execSQL(String sql,Object[] args){log("write",sql,args);boolean protectedInsert=protectedInsertReadR31(sql);try(PreparedStatement p=prepare(sql,args)){p.execute();if(protectedInsert&&p.getUpdateCount()==0)protectedDuplicateReadR31(true);if(frame!=null)frame.rows+=Math.max(0,p.getUpdateCount());}catch(SQLException e){if(String.valueOf(e.getMessage()).contains("R31_QUAL_RECEIPT_IMMUTABLE"))protectedDuplicateReadR31(protectedInsert);throw new SQLiteException(e.getMessage());}}
    public Cursor rawQuery(String sql,String[] args){log("read",sql,args);try(PreparedStatement p=prepare(sql,args);ResultSet r=p.executeQuery()){
        List<Object[]> rows=new ArrayList<>();List<String> names=new ArrayList<>();int n=r.getMetaData().getColumnCount();for(int i=1;i<=n;i++)names.add(r.getMetaData().getColumnLabel(i));
        while(r.next()){Object[] row=new Object[n];for(int i=0;i<n;i++)row[i]=r.getObject(i+1);rows.add(row);}return new Rows(rows,names);
    }catch(SQLException e){throw new SQLiteException(e.getMessage());}}
    public long insertWithOnConflict(String table,String nullColumn,ContentValues values,int conflict){
        StringJoiner columns=new StringJoiner(","),slots=new StringJoiner(",");List<Object> args=new ArrayList<>();for(Map.Entry<String,Object> v:values.valueSet()){columns.add('"'+v.getKey()+'"');slots.add("?");args.add(v.getValue());}
        String sql="INSERT "+(conflict==CONFLICT_IGNORE?"OR IGNORE ":conflict==CONFLICT_REPLACE?"OR REPLACE ":"")+"INTO "+table+"("+columns+") VALUES("+slots+")";log("write",sql,args.toArray());
        boolean protectedInsert=protectedInsertReadR31(sql);try(PreparedStatement p=prepare(sql,args.toArray())){int changed=p.executeUpdate();if(protectedInsert&&changed==0)protectedDuplicateReadR31(true);if(frame!=null)frame.rows+=changed;if(changed==0)return -1;log("read","SELECT last_insert_rowid()",null);try(Statement s=jdbc.createStatement();ResultSet r=s.executeQuery("SELECT last_insert_rowid()")){if(!r.next())throw new SQLException("MISSING_LAST_INSERT_ROWID");return r.getLong(1);}}catch(SQLException e){if(String.valueOf(e.getMessage()).contains("R31_QUAL_RECEIPT_IMMUTABLE"))protectedDuplicateReadR31(protectedInsert);throw new SQLiteException(e.getMessage());}
    }
    public long insert(String table,String nullColumn,ContentValues values){return insertWithOnConflict(table,nullColumn,values,CONFLICT_NONE);}
    public long insertOrThrow(String table,String nullColumn,ContentValues values){return insert(table,nullColumn,values);}
    public int update(String table,ContentValues values,String where,String[] conditions){StringJoiner sets=new StringJoiner(",");List<Object> args=new ArrayList<>();for(Map.Entry<String,Object> v:values.valueSet()){sets.add('"'+v.getKey()+"\"=?");args.add(v.getValue());}if(conditions!=null)Collections.addAll(args,(Object[])conditions);String sql="UPDATE "+table+" SET "+sets+(where==null?"":" WHERE "+where);log("write",sql,args.toArray());try(PreparedStatement p=prepare(sql,args.toArray())){int n=p.executeUpdate();if(frame!=null)frame.rows+=n;return n;}catch(SQLException e){throw new SQLiteException(e.getMessage());}}
    public int delete(String table,String where,String[] args){String sql="DELETE FROM "+table+(where==null?"":" WHERE "+where);log("write",sql,args);try(PreparedStatement p=prepare(sql,args)){int n=p.executeUpdate();if(frame!=null)frame.rows+=n;return n;}catch(SQLException e){throw new SQLiteException(e.getMessage());}}
    public boolean inTransaction(){try{return !jdbc.getAutoCommit();}catch(SQLException e){throw new SQLiteException(e.getMessage());}}
    public void beginTransaction(){if(frame!=null)throw new SQLiteException("NESTED_HOST_TRANSACTION");Frame starting=new Frame();try{jdbc.setAutoCommit(false);successful=false;frame=starting;if(trackVmStepsR31)org.sqlite.ProgressHandler.setHandler(jdbc,100,new org.sqlite.ProgressHandler(){protected int progress(){if(frame!=null)frame.vmTicks++;return 0;}});}catch(SQLException e){throw new SQLiteException(e.getMessage());}}
    public void setTransactionSuccessful(){successful=true;}
    public void endTransaction(){boolean committed=false;Frame finished=frame;try{
        if(successful){jdbc.setAutoCommit(true);committed=true;}
        else {try(Statement control=jdbc.createStatement()){control.execute("ROLLBACK");}((org.sqlite.SQLiteConnection)jdbc).getConnectionConfig().setAutoCommit(true);}
        // JDBC commit()/rollback() automatically BEGIN another transaction.
        // End the actual outer transaction once, without an untraced empty one.
    }catch(SQLException e){throw new SQLiteException(e.getMessage());}finally{
        long elapsed=finished==null?0:System.nanoTime()-finished.start;successful=false;frame=null;if(trackVmStepsR31)try{org.sqlite.ProgressHandler.clearHandler(jdbc);}catch(SQLException e){throw new SQLiteException(e.getMessage());}
        if(finished!=null){Map<String,Object> row=new LinkedHashMap<>();row.put("id",finished.id);row.put("scenario",finished.label);row.put("committed",committed);row.put("sqlCalls",finished.reads+finished.writes);row.put("transactionControlCalls",2);row.put("protectedReceiptInsertIndexedReads",finished.receiptGuardReads);row.put("allSqlIncludingBeginEnd",finished.reads+finished.writes+finished.receiptGuardReads+2);row.put("nativeVmStepLowerBound",finished.vmTicks*100);row.put("nativeVmProgressInterval",trackVmStepsR31?100:0);row.put("readCostBasis","API reads plus one indexed EXISTS per protected INSERT; duplicate IGNORE/ABORT conservatively adds its second indexed EXISTS; actual SQL and native EXPLAIN retained");row.put("reads",finished.reads);row.put("writes",finished.writes);row.put("fenceReads",finished.fences);row.put("commitReceiptCalls",finished.receipts);row.put("bindBytes",finished.binds);row.put("directChangedRowsIncludingAttemptedRollback",finished.rows);row.put("sqlTransactionNanos",elapsed);row.put("scope","HOST_NATIVE_SQL_TRANSACTION_NOT_ANDROID_WRITER_SLO");row.put("sql",finished.sql);frames.add(row);System.out.println("FRAME "+JsonR27.write(row));System.out.flush();}
    }}
    public boolean isReadOnly(){return readOnly;}public boolean isOpen(){try{return !jdbc.isClosed();}catch(SQLException e){return false;}}
    public String getPath(){try{return jdbc.getMetaData().getURL().substring("jdbc:sqlite:".length());}catch(SQLException e){throw new SQLiteException(e.getMessage());}}
    public void close(){try{jdbc.close();}catch(SQLException e){throw new SQLiteException(e.getMessage());}}
    public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){try{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+((flags&OPEN_READONLY)!=0?"file:"+path+"?mode=ro":path)),(flags&OPEN_READONLY)!=0);}catch(SQLException e){throw new SQLiteException(e.getMessage());}}
    static final class Rows implements Cursor {
        final List<Object[]> rows;final List<String> names;int at=-1;Rows(List<Object[]> rows,List<String> names){this.rows=rows;this.names=names;}Object value(int i){return rows.get(at)[i];}
        public boolean moveToNext(){return ++at<rows.size();}public boolean moveToFirst(){at=0;return !rows.isEmpty();}public String getString(int i){return value(i)==null?null:String.valueOf(value(i));}public long getLong(int i){Object v=value(i);return v==null?0:v instanceof Number?((Number)v).longValue():Long.parseLong(v.toString());}public int getInt(int i){return(int)getLong(i);}public double getDouble(int i){Object v=value(i);return v==null?0:v instanceof Number?((Number)v).doubleValue():Double.parseDouble(v.toString());}public byte[] getBlob(int i){return(byte[])value(i);}public int getType(int i){Object v=value(i);return v==null?0:v instanceof byte[]?4:v instanceof Double||v instanceof Float?2:v instanceof Number?1:3;}public boolean isNull(int i){return value(i)==null;}public int getCount(){return rows.size();}public int getColumnIndex(String name){for(int i=0;i<names.size();i++)if(name.equalsIgnoreCase(names.get(i)))return i;return -1;}public int getColumnCount(){return names.size();}public String[] getColumnNames(){return names.toArray(new String[0]);}public void close(){}
    }
}
