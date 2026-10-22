package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.JSONObject;
/** Actual product cleanup against the complete installed trigger schema. */
public final class RetentionRevisionR75 {
 static int checks;static int tableIndex;static final String TABLE="diagnostic_log",KEY="retention_cp12_revision_after_"+TABLE;
 static void ok(boolean v,String m){if(!v)throw new AssertionError(m);checks++;System.out.println("PASS "+m);}
 static long num(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
 static String state(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):"";}}
 static void set(SQLiteDatabase db,String key,String value){db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,1)",new Object[]{key,value});}
 static int sweep(SQLiteDatabase db,boolean commit){db.beginTransaction();try{set(db,"retention_cp12_revision_cursor",Integer.toString(tableIndex));int n=DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(db,TimeSourceR811.monoNowMs()+5000);if(commit)db.setTransactionSuccessful();return n;}finally{db.endTransaction();}}
 public static void main(String[] args)throws Exception{
  for(int i=0;i<RetentionTableRegistryCP12.ALL_INSTALLED.size();i++)if(RetentionTableRegistryCP12.ALL_INSTALLED.get(i).table.equals(TABLE))tableIndex=i;
  RealtimePressureGateR811.resetForTests();
  try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){
   db.setForeignKeyConstraintsEnabled(true);
   ok(num(db,"SELECT COUNT(*) FROM sqlite_master WHERE type='trigger'")>=1300,"full installed schema");
   ok(sweep(db,true)==0,"first bounded page preserves existing 128 rows");
   ok(state(db,KEY).equals("128"),"page cursor advances over live rows");
   ok(state(db,"retention_cp12_revision_last").startsWith("examined=128|"),"at most 128 candidates per cleanup");
   ok(state(db,"retention_cp12_revision_cursor").equals(Integer.toString(tableIndex+1)),"table cursor gives next table a turn");
   ok(sweep(db,true)==0&&state(db,KEY).equals("256"),"second call resumes after persisted key");
   ok(sweep(db,false)==84,"rollback fixture actually attempts orphan deletions");
   ok(state(db,KEY).equals("256")&&num(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'")==560,"rollback restores cursor and revisions atomically");
   ok(sweep(db,true)==84,"third page deletes only 84 known missing rowids");
   ok(sweep(db,true)==128,"next page handles 128 orphans");
   ok(sweep(db,true)==48,"last page finishes orphan tail");
   ok(state(db,KEY).equals(Long.toString(Long.MIN_VALUE)),"page end wraps for newly inserted low rowids");
   ok(num(db,"SELECT COUNT(*) FROM diagnostic_log")==300&&num(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'")==300,"all live rows and their current generations retained");
   db.execSQL("INSERT INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnostic_log',-7)");
   ok(sweep(db,true)==1,"wrapped cursor finds newly inserted low rowid orphan");
   db.execSQL("UPDATE diagnostic_log SET detail='changed' WHERE rowid=150");
   long generation=num(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=150");
   ok(sweep(db,true)==0&&num(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=150")==generation,"updated live generation survives cleanup");
   // A genuine second SQLite connection waits behind the cleanup transaction.
   AtomicReference<Throwable> error=new AtomicReference<Throwable>();CountDownLatch ready=new CountDownLatch(1);AtomicBoolean done=new AtomicBoolean();
   try(SQLiteDatabase other=new SQLiteDatabase(args[0],args[1])){
    db.beginTransaction();Thread writer=new Thread(()->{ready.countDown();try{other.execSQL("INSERT INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES('concurrent',1,'INFO','R75','HOST','new','')");other.execSQL("INSERT OR REPLACE INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES('concurrent',1,'INFO','R75','HOST','replaced','')");done.set(true);}catch(Throwable t){error.set(t);}});
    try{writer.start();ready.await();set(db,"retention_cp12_revision_cursor",Integer.toString(tableIndex));DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(db,TimeSourceR811.monoNowMs()+5000);db.setTransactionSuccessful();}finally{db.endTransaction();}
    writer.join(10000);ok(!writer.isAlive()&&error.get()==null&&done.get(),"concurrent writer resumes after transaction without lost work");
   }
   ok(num(db,"SELECT COUNT(*) FROM diagnostic_log WHERE log_key='concurrent'")==1,"concurrent replacement leaves exactly one live row");
   set(db,KEY,Long.toString(Long.MIN_VALUE));for(int i=0;i<3;i++)sweep(db,true);
   ok(num(db,"SELECT COUNT(*) FROM research_row_revision_v2 r JOIN diagnostic_log d ON d.rowid=r.source_rowid WHERE r.source_table='diagnostic_log'")==301,"concurrent row generation remains valid after full sweep");
   long before=num(db,"SELECT COUNT(*) FROM research_row_revision_v2");db.beginTransaction();try{ok(DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(db,0)==0,"expired deadline does no scanning");db.setTransactionSuccessful();}finally{db.endTransaction();}
   ok(num(db,"SELECT COUNT(*) FROM research_row_revision_v2")==before,"expired slice does not remove revisions");
  }
  Files.writeString(Path.of(args[2]),new JSONObject().put("status","PASS").put("checks",checks).put("scope","ACTUAL_PRODUCT_FULL_SCHEMA_REAL_SQLITE_SYNTHETIC_INPUT_NOT_DEVICE").toString()+"\n");
 }
}
