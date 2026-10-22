package com.ethk3.novaonepro;
import java.lang.reflect.*;import java.util.*;import java.util.concurrent.atomic.*;import java.util.concurrent.locks.*;
import android.database.sqlite.*;
/** Bypasses Android process startup only. All tested DAO, coordinator, gate and writers are product bytecode. */
public final class HostProductR974 {
 static final sun.misc.Unsafe U;
 static{try{Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(sun.misc.Unsafe)f.get(null);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 static void set(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 public static DbStore store(SQLiteDatabase db)throws Exception{
  SQLiteOpenHelper.host=db;
  DbHelper helper=(DbHelper)U.allocateInstance(DbHelper.class);
  DatabaseCoordinator coordinator=(DatabaseCoordinator)U.allocateInstance(DatabaseCoordinator.class);
  set(coordinator,"helper",helper);set(coordinator,"writeLock",new ReentrantLock(true));set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
  for(String n:new String[]{"criticalSchemaReady","runtimeSchemaReady","r811ResearchSchemaReady","linkSchemaReady"})set(coordinator,n,new AtomicBoolean(true));
  DbStore s=(DbStore)U.allocateInstance(DbStore.class);set(s,"coordinator",coordinator);set(s,"helper",helper);set(s,"columnCache",new HashMap<String,HashSet<String>>());set(s,"uiStateReadR945",new ThreadLocal<Map<String,String>>());return s;
 }
 public static void schema(SQLiteDatabase db,DbStore store)throws Exception{
  db.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_APP_STATE+"(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
  db.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_DRAWS+"(_id INTEGER PRIMARY KEY,issue TEXT UNIQUE,open_time INTEGER,fetch_time INTEGER,source_line TEXT,raw_json TEXT,dirty INTEGER DEFAULT 0,dirty_reason TEXT,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER)");
  Method m=DbStore.class.getDeclaredMethod("tradeToValues",Models.Trade.class);m.setAccessible(true);android.content.ContentValues values=(android.content.ContentValues)m.invoke(store,new Models.Trade());
  StringBuilder sql=new StringBuilder("CREATE TABLE ").append(DbHelper.TABLE_TRADES).append("(_id INTEGER PRIMARY KEY AUTOINCREMENT");
  for(java.util.Map.Entry<String,Object> e:values.valueSet())if(!e.getKey().equals("_id"))sql.append(',').append(e.getKey()).append(e.getValue() instanceof Float||e.getValue() instanceof Double?" REAL":e.getValue() instanceof Number?" INTEGER":" TEXT");
  db.execSQL(sql.append(')').toString());
  ResearchPlatformStoreR929.createSchema(db);
  db.execSQL("CREATE TABLE IF NOT EXISTS "+ResearchLifecycleR932.TABLE+"(candidate_key TEXT PRIMARY KEY,state TEXT)");
  db.execSQL("CREATE TABLE IF NOT EXISTS "+ResearchCohortR932.CHAMPION+"(family TEXT,candidate_key TEXT)");
  db.beginTransaction();try{com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952.ensure(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
 }
}
