package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
/** Native SQLite engineering fixture. Actual 128-candidate source; never natural evidence. */
public final class EvolutionPublicationR31Host {
 static void need(boolean value,String reason){if(!value)throw new AssertionError(reason);}
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT count(*) FROM "+table,null)){need(c.moveToFirst(),table);return c.getLong(0);}}
 static long policies(SQLiteDatabase db){return count(db,"r27_policy");}
 public static void main(String[] args)throws Exception{
  ReplayDatasetR27 data=ReplayDatasetR27.synthetic(5000,3185,new int[]{700,1900});
  EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.seed=3185;cfg.births=128;cfg.nullControl=false;
  StoreR27.EvolutionSink gate=null;EvolutionLoopR27 loop=null;
  try(FeatureCacheR27 cache=new FeatureCacheR27(data)){
   loop=new EvolutionLoopR27(cfg,cache,null,System::currentTimeMillis);
   for(int phase=1;phase<=3;phase++){StoreR27.EvolutionSink sink=new StoreR27.EvolutionSink("HOST_ENGINEERING",data.issues[data.size()-1]);loop.setSink(sink);loop.step();gate=sink;}
   Map<?,?> snapshot=(Map<?,?>)JsonR27.parse(loop.snapshot());need(((List<?>)snapshot.get("pending")).size()==128,"original 128 candidates retained");
   Map<String,Integer> states=new TreeMap<>();for(Object o:(List<?>)snapshot.get("pending")){Map<?,?> p=(Map<?,?>)o;String state=String.valueOf(p.get("state"));states.put(state,states.getOrDefault(state,0)+1);}
   ResearchWriteQueueR27.Batch batch=gate.batch;String text=loop.snapshot();batch.add("INSERT OR REPLACE INTO r27_evolution_cursor(loop,generation,step,dataset_hash,rng_state,snapshot,updated_at) VALUES(?,?,?,?,?,?,?)","HOST_ENGINEERING",loop.generation,loop.step,data.datasetHash,loop.rng.state,text,System.currentTimeMillis());
   ResearchWriteQueueR27.ImmutableBatch prepared=batch.prepareR31("REPRO_EVOLUTION","repro","r27-evolution","ENGINEERING",1,data.datasetHash,0,"REPRO_ONLY");
   Map<String,Object> out=new LinkedHashMap<>();out.put("scope","ACTUAL_DEFAULT_128_SOURCE_AND_NATIVE_SQLITE_ENGINEERING_ONLY_NOT_ANDROID_OR_QUALITY");out.put("rows",data.size());out.put("pendingCandidates",128);out.put("actualCandidateStates",states);out.put("ops",prepared.opCount);out.put("bindBytes",prepared.bindBytes);out.put("overInitialBudget",prepared.overInitialBudget);out.put("snapshotBytes",text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);out.put("rngState",loop.rng.state);
   Class.forName("org.sqlite.JDBC");Path path=Files.createTempFile("r31-evolution-baseline-",".db");
   try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+path);Connection independent=DriverManager.getConnection("jdbc:sqlite:"+path)){
    SQLiteDatabase db=new SQLiteDatabase(connection),reader=new SQLiteDatabase(independent);
    db.execSQL("PRAGMA journal_mode=WAL");for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);for(String ddl:RuntimeSchemaR26.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
    batch.guardAfter(tx->{need(policies(reader)==0,"an independent reader cannot see uncommitted rows");throw new IllegalStateException("INJECTED_BEFORE_COMMIT_AFTER_ALL_ORIGINAL_SQL");});
    connection.setAutoCommit(false);boolean rolledBack=false;try{ResearchWriteQueueR27.execute(db,batch,"HOST_ENGINEERING");connection.commit();}catch(Exception injected){connection.rollback();rolledBack=injected.toString().contains("INJECTED_BEFORE_COMMIT");}finally{connection.setAutoCommit(true);}need(rolledBack,"real native transaction rollback injection fired");need(policies(db)==0&&count(db,"r26_evolution")==0&&count(db,"r27_evolution_cursor")==0,"old full batch is atomic on rollback");batch.after=null;
    long start=System.nanoTime();connection.setAutoCommit(false);try{ResearchWriteQueueR27.execute(db,batch,"HOST_ENGINEERING");connection.commit();}finally{connection.setAutoCommit(true);}out.put("actualOldAtomicCommitNanos",System.nanoTime()-start);out.put("persistedPolicyRows",policies(db));out.put("persistedAttemptRows",count(db,"r26_evolution"));out.put("persistedLineageRows",count(db,"r27_lineage"));out.put("oldAtomicRollbackVerified",true);out.put("oldPartialInvisibleBeforeCommit",true);
    try(Cursor cursor=db.rawQuery("SELECT snapshot FROM r27_evolution_cursor",null)){need(cursor.moveToFirst()&&text.equals(cursor.getString(0)),"exact original checkpoint published with outputs");}
   }finally{Files.deleteIfExists(path);Files.deleteIfExists(Path.of(path+"-wal"));Files.deleteIfExists(Path.of(path+"-shm"));}
   System.out.println(JsonR27.write(out));
  }finally{if(loop!=null)loop.close();}
 }
}
