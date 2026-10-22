package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import android.database.Cursor;import java.nio.file.*;
public final class RetentionTaskExpiryContractCP12 {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("check"+checks);}static long count(SQLiteDatabase d,String table){try(Cursor c=d.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 public static void main(String[] args)throws Exception{SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);db.execSQL("PRAGMA foreign_keys=ON");try{
  final String old="202601010001",cutoff="202609230001",hash="a".repeat(64);String id;
  db.beginTransaction();try{id=ResearchTaskQueueR967.enqueue(db,"POST_TRAIN","candidate-1",old,"receipt-1",hash,hash);for(String table:new String[]{"r952_research_outbox","r952_candidate_predraw","draws","v57_settlement_task_r36"})ResearchTaskQueueR967.pin(db,id,table,old);db.setTransactionSuccessful();}finally{db.endTransaction();}
  ok(count(db,"v57_research_input_pin_r967")==4);
  db.beginTransaction();try{ok(ResearchTaskQueueR967.expirePostTrainBeforeCP12(db,cutoff,Long.MAX_VALUE)==1);ok(count(db,"v57_research_input_pin_r967")==0);}finally{db.endTransaction();}
  ok(count(db,"v57_research_input_pin_r967")==4);ok(count(db,"research_task_retention_expiry_cp12")==0);
  db.beginTransaction();try{ok(ResearchTaskQueueR967.expirePostTrainBeforeCP12(db,cutoff,Long.MAX_VALUE)==1);db.setTransactionSuccessful();}finally{db.endTransaction();}
  ok(count(db,"v57_research_input_pin_r967")==0);ok(count(db,"research_task_retention_expiry_cp12")==1);
  try(Cursor c=db.rawQuery("SELECT status,generation,last_error FROM v57_research_task_r967 WHERE task_id=?",new String[]{id})){ok(c.moveToFirst()&&c.getString(0).equals("EXPIRED")&&c.getLong(1)==1&&c.getString(2).equals("POST_TRAIN_INPUT_EXPIRED_WITHOUT_REVIEW"));}
  db.beginTransaction();try{ok(ResearchTaskQueueR967.expirePostTrainBeforeCP12(db,cutoff,Long.MAX_VALUE)==0);db.setTransactionSuccessful();}finally{db.endTransaction();}
  String liveId;ResearchTaskQueueR967.Lease live;
  db.beginTransaction();try{liveId=ResearchTaskQueueR967.enqueue(db,"POST_TRAIN","candidate-2",old,"receipt-2",hash,hash);ResearchTaskQueueR967.pin(db,liveId,"draws",old);live=ResearchTaskQueueR967.claimFor(db,"POST_TRAIN","candidate-2",System.currentTimeMillis());ok(live!=null);db.setTransactionSuccessful();}finally{db.endTransaction();}
  db.beginTransaction();try{ok(ResearchTaskQueueR967.expirePostTrainBeforeCP12(db,cutoff,Long.MAX_VALUE)==0);ok(ResearchTaskQueueR967.owns(db,live));ResearchTaskQueueR967.yield(db,live,live.cursor,"test",0);db.setTransactionSuccessful();}finally{db.endTransaction();}
  db.beginTransaction();try{ok(ResearchTaskQueueR967.expirePostTrainBeforeCP12(db,cutoff,Long.MAX_VALUE)==1);db.setTransactionSuccessful();}finally{db.endTransaction();}
  ok(count(db,"research_task_retention_expiry_cp12")==2);try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!c.moveToFirst());}
  Files.writeString(Path.of(args[2]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"scope\":\"Actual Java Queue Owner expiry, live monotonic lease protection, immutable compact reason receipt, rollback, pins release and rerun on hostSQLite. No training success manufactured.\"}\n");
 }finally{db.shutdown();}}
}
