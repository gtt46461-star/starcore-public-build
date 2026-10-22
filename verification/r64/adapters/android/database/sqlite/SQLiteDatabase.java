package android.database.sqlite;
import android.database.Cursor;
import java.io.*;import java.util.*;import java.lang.reflect.*;
/** Test adapter only: executes the unmodified production SQL in real SQLite, not a canned cursor. */
public class SQLiteDatabase implements java.io.Closeable {
    public interface CursorFactory {}
public long insert(String t,String n,android.content.ContentValues v){return insertWithOnConflict(t,n,v,0);}
public void setForeignKeyConstraintsEnabled(boolean b){execSQL("PRAGMA foreign_keys="+(b?"ON":"OFF"));}
    public Cursor query(String table,String[] columns,String selection,String[] args,String group,String having,String order,String limit){
        String sql="SELECT "+(columns==null?"*":String.join(",",columns))+" FROM "+table;
        if(selection!=null&&!selection.isEmpty())sql+=" WHERE "+selection;
        if(group!=null)sql+=" GROUP BY "+group;if(having!=null)sql+=" HAVING "+having;
        if(order!=null)sql+=" ORDER BY "+order;if(limit!=null)sql+=" LIMIT "+limit;return rawQuery(sql,args);
    }
    public Cursor query(String table,String[] columns,String selection,String[] args,String group,String having,String order){return query(table,columns,selection,args,group,having,order,null);}
    public int getVersion(){try(Cursor c=rawQuery("PRAGMA user_version",null)){c.moveToFirst();return c.getInt(0);}}
    public void setVersion(int v){execSQL("PRAGMA user_version="+v);}public String getPath(){return openedPath;}
    public void beginTransactionNonExclusive(){beginTransaction();}

    private final Process process;private final BufferedReader in;private final PrintWriter out;
    public SQLiteDatabase(String bridge)throws Exception{this(bridge,null);}
    private static String bridgePath;private final String openedPath;private final boolean readOnly;
    public static final int OPEN_READONLY=1,NO_LOCALIZED_COLLATORS=16;
    public SQLiteDatabase(String bridge,String copy)throws Exception{this(bridge,copy,false);}
    private SQLiteDatabase(String bridge,String copy,boolean readonly)throws Exception{
      bridgePath=bridge;openedPath=copy==null?":memory:":copy;readOnly=readonly;
      ProcessBuilder pb=copy==null?new ProcessBuilder("python",bridge):readonly?new ProcessBuilder("python",bridge,copy,"--readonly"):new ProcessBuilder("python",bridge,copy);
      process=pb.start();in=new BufferedReader(new InputStreamReader(process.getInputStream()));out=new PrintWriter(process.getOutputStream(),true);
    }
    public static SQLiteDatabase openDatabase(String path,CursorFactory factory,int flags){try{return new SQLiteDatabase(bridgePath,path,(flags&OPEN_READONLY)!=0);}catch(Exception e){throw new IllegalStateException(e);}}
    public boolean isReadOnly(){return readOnly;}

    static String en(String s){return Base64.getEncoder().encodeToString(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    static String de(String s){return new String(Base64.getDecoder().decode(s),java.nio.charset.StandardCharsets.UTF_8);}
    public synchronized Cursor rawQuery(String sql,String[] args){return call(sql,args);}
    public synchronized void execSQL(String sql){rejectRowPragma(sql);call(sql,null);}
    public synchronized void execSQL(String sql,Object[] args){rejectRowPragma(sql);call(sql,args);}
    /** Host bridge owns one connection. This is API adaptation only; propagation
     * across Android's SQLite connection pool requires the instrumentation test. */
    public synchronized void execPerConnectionSQL(String sql,Object[] args){call(sql,args);}
    private static void rejectRowPragma(String sql){String q=sql.trim().toLowerCase(java.util.Locale.ROOT);if(q.startsWith("pragma incremental_vacuum")||q.startsWith("pragma wal_checkpoint"))throw new IllegalStateException("Android execSQL cannot execute row-producing PRAGMA");}
    public static final int CONFLICT_NONE=0,CONFLICT_ABORT=2,CONFLICT_IGNORE=4,CONFLICT_REPLACE=5;
    private boolean transaction,successful;
    public android.os.CancellationSignal lastSignal;
    public synchronized Cursor rawQuery(String sql,String[] args,android.os.CancellationSignal signal){
        lastSignal=signal;
        if (sql.equals("BLOCK_UNTIL_CANCEL")) {
            while(!signal.isCanceled())try{Thread.sleep(1);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
            throw new IllegalStateException("SQL_NATIVE_CANCEL_SIGNAL");
        }
        return call(sql,args);
    }
    public boolean inTransaction(){return transaction;}
    private final java.util.ArrayDeque<Boolean> transactionFrames=new java.util.ArrayDeque<>(); private boolean childFailed; public void beginTransaction(){if(transactionFrames.isEmpty()){execSQL("BEGIN IMMEDIATE");childFailed=false;}transactionFrames.push(false);transaction=true;successful=false;}
    public void setTransactionSuccessful(){if(transactionFrames.isEmpty()||transactionFrames.peek())throw new IllegalStateException("HOST_TRANSACTION_SUCCESS_STATE");transactionFrames.pop();transactionFrames.push(true);successful=true;}
    public void endTransaction(){if(transactionFrames.isEmpty())throw new IllegalStateException("HOST_TRANSACTION_MISSING");boolean done=transactionFrames.pop();if(!done)childFailed=true;if(!transactionFrames.isEmpty())return;try{execSQL(done&&!childFailed?"COMMIT":"ROLLBACK");}finally{transaction=false;successful=false;childFailed=false;}}
    public long insertWithOnConflict(String table,String nullColumn,android.content.ContentValues values,int conflict){
        java.util.ArrayList<Object> args=new java.util.ArrayList<Object>();StringBuilder cols=new StringBuilder(),marks=new StringBuilder();
        for(java.util.Map.Entry<String,Object> e:values.valueSet()){if(cols.length()>0){cols.append(',');marks.append(',');}cols.append(e.getKey());marks.append('?');args.add(e.getValue());}
        execSQL((conflict==0?"INSERT":"INSERT OR "+(conflict==CONFLICT_REPLACE?"REPLACE":conflict==CONFLICT_ABORT?"ABORT":"IGNORE"))+" INTO "+table+"("+cols+") VALUES("+marks+")",args.toArray());
        Cursor c=rawQuery("SELECT changes(),last_insert_rowid()",null);try{c.moveToFirst();return c.getInt(0)==0?-1:c.getLong(1);}finally{c.close();}
    }
    public long insertOrThrow(String table,String nullColumn,android.content.ContentValues values){return insertWithOnConflict(table,nullColumn,values,0);}
    public int update(String table,android.content.ContentValues values,String where,String[] predicates){
        java.util.ArrayList<Object> args=new java.util.ArrayList<Object>();StringBuilder set=new StringBuilder();
        for(java.util.Map.Entry<String,Object> e:values.valueSet()){if(set.length()>0)set.append(',');set.append(e.getKey()).append("=?");args.add(e.getValue());}
        if(predicates!=null)java.util.Collections.addAll(args,(Object[])predicates);
        execSQL("UPDATE "+table+" SET "+set+(where==null?"":" WHERE "+where),args.toArray());Cursor c=rawQuery("SELECT changes()",null);try{c.moveToFirst();return c.getInt(0);}finally{c.close();}
    }
    public boolean isOpen(){return true;}
    public int delete(String table,String where,String[] predicates){
        execSQL("DELETE FROM "+table+(where==null?"":" WHERE "+where),predicates);
        try(Cursor c=rawQuery("SELECT changes()",null)){c.moveToFirst();return c.getInt(0);}
    }
    private Cursor call(String sql,Object[] args){try{

        StringBuilder b=new StringBuilder("SQL\t").append(en(sql));if(args!=null)for(Object v:args)b.append('\t').append(v==null?"N":v instanceof byte[]?"B"+Base64.getEncoder().encodeToString((byte[])v):"S"+en(v.toString()));out.println(b);
        String[] header=in.readLine().split("\t");if(header[0].equals("ERROR"))throw new IllegalStateException(de(header[1])+" | "+sql);
        int count=Integer.parseInt(header[1]),columns=Integer.parseInt(header[2]);String[] names=new String[columns];for(int i=0;i<columns;i++)names[i]=de(header[i+3]);
        Object[][] rows=new Object[count][columns];for(int r=0;r<count;r++){String[] a=in.readLine().split("\t",-1);for(int k=0;k<columns;k++)rows[r][k]=a[k].equals("N")?null:a[k].startsWith("I")?Long.valueOf(a[k].substring(1)):a[k].startsWith("F")?Double.valueOf(a[k].substring(1)):a[k].startsWith("B")?Base64.getDecoder().decode(a[k].substring(1)):de(a[k].substring(1));}
        final int[] pos={-1};
        return (Cursor)Proxy.newProxyInstance(Cursor.class.getClassLoader(),new Class[]{Cursor.class},(o,m,a)->{
            String n=m.getName();if(n.equals("close"))return null;if(n.equals("moveToFirst")){pos[0]=0;return count>0;}if(n.equals("moveToNext"))return ++pos[0]<count;
            if(n.equals("getColumnNames"))return names;
            if(n.equals("getCount"))return count;if(n.equals("getColumnCount"))return columns;if(n.equals("getColumnName"))return names[(Integer)a[0]];
            if(n.equals("getColumnIndex")||n.equals("getColumnIndexOrThrow")){for(int i=0;i<columns;i++)if(names[i].equals(a[0]))return i;return -1;}
            Object v=rows[pos[0]][(Integer)a[0]];if(n.equals("getType"))return v==null?0:v instanceof byte[]?4:v instanceof Long?1:v instanceof Double?2:3;if(n.equals("isNull"))return v==null;if(n.equals("getString"))return v==null?null:v.toString();if(n.equals("getBlob"))return v;
            if(n.equals("getInt"))return v==null?0:Integer.parseInt(v.toString());if(n.equals("getLong"))return v==null?0L:Long.parseLong(v.toString());if(n.equals("getDouble"))return v==null?0d:Double.parseDouble(v.toString());throw new UnsupportedOperationException(n);
        });
    }catch(IOException e){throw new IllegalStateException(e);}}
    public void close(){shutdown();}
    public void shutdown(){
        // EOF lets the real SQLite connection close/checkpoint before a durable snapshot is used.
        // SIGTERM during WAL checkpoint can corrupt a copied database and hide import prerequisites.
        out.close();
        try {
            if(!process.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)){
                process.destroyForcibly();throw new IllegalStateException("HOST_SQLITE_CLOSE_TIMEOUT");
            }
            if(process.exitValue()!=0)throw new IllegalStateException("HOST_SQLITE_CLOSE_FAILED:"+process.exitValue());
        }catch(InterruptedException e){Thread.currentThread().interrupt();process.destroyForcibly();throw new IllegalStateException("HOST_SQLITE_CLOSE_INTERRUPTED",e);}
    }
}
