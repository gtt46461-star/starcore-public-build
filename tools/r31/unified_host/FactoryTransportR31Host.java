package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.sql.*;import java.util.*;
/** Native SQLite + actual Store/Batch; only the CursorWindow size is a HOST boundary model.
 * No experimental trials are executed or credited by this transport fixture. */
public final class FactoryTransportR31Host {
 static int assertions;static void check(boolean b,String s){assertions++;if(!b)throw new AssertionError(s);}
 static String scalar(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
 static void execute(Connection c,SQLiteDatabase db,ResearchWriteQueueR27.Batch b)throws Exception{c.setAutoCommit(false);try{ResearchWriteQueueR27.exec(db,b);c.commit();}catch(Exception|Error e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
 @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
  Class.forName("org.sqlite.JDBC");try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
   SQLiteDatabase db=new SQLiteDatabase(connection);db.execSQL("CREATE TABLE r27_chart_cache(panel_id TEXT,key TEXT,payload_json TEXT,updated_at INTEGER,PRIMARY KEY(panel_id,key))");db.execSQL("CREATE TABLE sentinel(value INTEGER)");db.execSQL("INSERT INTO sentinel VALUES(0)");
   String source="HOST_TRANSPORT_FIXTURE_NOT_EXPERIMENTAL_EVIDENCE";
   ResearchWriteQueueR27.Batch registration=new ResearchWriteQueueR27.Batch();FactoryCapabilityStoreR31.advance(db,registration,source,1,false);execute(connection,db,registration);
   EvidenceContractStoreR31.Registration registered=EvidenceContractStoreR31.beforeFactory(db,new ResearchWriteQueueR27.Batch(),source,2);check(registered!=null,"TRANSPORT_FIXTURE_USES_ACTUAL_PREREGISTERED_NAMESPACE");String key=registered.factoryKey();
   Map<String,Object> state;try(FactoryCapabilityExperimentR31 runner=new FactoryCapabilityExperimentR31(source)){state=(Map<String,Object>)JsonR27.parse(runner.snapshot());}
   List<Object> events=new ArrayList<>();StringBuilder text=new StringBuilder();for(int i=0;i<300;i++)text.append("ORIGINAL_STORAGE_FIXTURE_");
   for(int i=0;i<600;i++){Map<String,Object> event=new LinkedHashMap<>();event.put("kind","TRANSPORT_FIXTURE_ONLY");event.put("data",Arrays.asList(i,text.toString()));events.add(event);}
   Map<String,Object> ledger=(Map<String,Object>)state.get("ledger");ledger.put("events",events);String raw=JsonR27.write(state);check(raw.length()>2*1024*1024,"FIXTURE_EXCEEDS_ANDROID_WINDOW");
   db.execSQL("INSERT INTO r27_chart_cache VALUES('r31-factory',?,?,1)",new Object[]{key,raw});boolean window=false;try{scalar(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id='r31-factory'");}catch(RuntimeException expected){window=String.valueOf(expected.getMessage()).contains("HOST_CURSOR_WINDOW");}check(window,"OLD_INLINE_READ_REPRODUCES_REAL_LIMIT_MODEL");
   ResearchWriteQueueR27.Batch wait=new ResearchWriteQueueR27.Batch();wait.add("UPDATE sentinel SET value=value+1");FactoryCapabilityStoreR31.advanceIsolated(db,wait,source,2,false);execute(connection,db,wait);check("1".equals(scalar(db,"SELECT value FROM sentinel")),"OTHER_HEALTH_WORK_COMMITS");check("INCOMPLETE_RESOURCE_WAIT".equals(((Map<?,?>)JsonR27.parse(scalar(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id='r30'"))).get("status")),"RESOURCE_WAIT_EXPLICIT_NOT_COMPLETE");
   check(Integer.parseInt(scalar(db,"SELECT length(payload_json) FROM r27_chart_cache WHERE panel_id='r31-factory'"))==raw.length(),"OVERSIZED_ORIGINAL_NOT_DELETED_OR_TRUNCATED");
   String encoded=FactoryStateCodecR31.encode(raw);db.execSQL("UPDATE r27_chart_cache SET payload_json=? WHERE panel_id='r31-factory'",new Object[]{encoded});ResearchWriteQueueR27.Batch next=new ResearchWriteQueueR27.Batch();FactoryCapabilityStoreR31.advanceIsolated(db,next,source,3,false);execute(connection,db,next);
   String saved=scalar(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id='r31-factory'");check(saved.length()<=FactoryStateCodecR31.MAX_CELL_BYTES,"SQL_CELL_BOUNDED");check(raw.equals(FactoryStateCodecR31.decode(saved)),"ALL_ORIGINAL_LEDGER_BYTES_SURVIVE_ACTUAL_STORE");
   Map<?,?> summary=(Map<?,?>)JsonR27.parse(scalar(db,"SELECT payload_json FROM r27_chart_cache WHERE panel_id='r30'"));check(Boolean.FALSE.equals(summary.get("realPermission")),"NO_REAL_AUTHORITY");check(!"COMPLETE_MEASURED_REQUIRES_REGISTERED_POWER_ASSESSMENT".equals(summary.get("status")),"TRANSPORT_IS_NOT_EXPERIMENT_COMPLETION");
   ResearchWriteQueueR27.Batch stale=new ResearchWriteQueueR27.Batch();FactoryCapabilityStoreR31.advance(db,stale,source,4,false);db.execSQL("UPDATE r27_chart_cache SET payload_json='{}' WHERE panel_id='r31-factory'");check(stale.before.call(db)<0,"PREDECESSOR_CAS_REJECTS_CHANGED_ROW_WITHOUT_INFLATE_IN_WRITER");
   ResearchWriteQueueR27.Batch corrupt=new ResearchWriteQueueR27.Batch();corrupt.add("UPDATE sentinel SET value=value+1");FactoryCapabilityStoreR31.advanceIsolated(db,corrupt,source,5,false);execute(connection,db,corrupt);check("2".equals(scalar(db,"SELECT value FROM sentinel")),"CORRUPT_OPTIONAL_EXPERIMENT_DOES_NOT_ROLL_BACK_OTHER_HEALTH");
  }
  System.out.println("FACTORY_TRANSPORT_NATIVE_HOST_PASS assertions="+assertions+" actualStore=YES actualBatch=YES cursorWindow=EXPLICIT_2_MIB_HOST_MODEL experimentEvidence=NONE naturalQuality=NOT_PROVEN");
 }
}
