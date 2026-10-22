package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import com.ethk3.novaonepro.r952.*;import java.io.*;import java.nio.file.*;import java.lang.reflect.*;
public final class ModernSeedRegistrationContractCP12 {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("check"+n);}
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);try{
   DbStore store=HostProductR974.store(db);Field root=DbStore.class.getDeclaredField("researchInputRootCP11");root.setAccessible(true);root.set(store,new File(args[2]));
   db.beginTransaction();try{DevelopmentProductionCP12.schema(db);RetentionArtifactIdentityCP12.install(db);ModernSeedFactoryCP12.schema(db);db.execSQL("CREATE TABLE IF NOT EXISTS research_action_input_boundary_cp12(action_id TEXT PRIMARY KEY REFERENCES research_action_v2(action_id),source_issue TEXT NOT NULL,selection_boundary TEXT NOT NULL,evidence_hash TEXT NOT NULL,boundary_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL)");db.setTransactionSuccessful();}finally{db.endTransaction();}
   String result=MolecularToolWorldR947.importSeedBatchCP4(store,System.nanoTime()+10_000_000_000L);ok(result!=null&&!result.isEmpty());
   ok(count(db,"r952_candidate")==1);ok(count(db,"research_training_protocol_v2")==1);ok(count(db,"research_training_input_v2")==1);ok(count(db,ResearchTaskQueueR967.TABLE)==1);ok(count(db,"research_action_input_boundary_cp12")==1);
   try(Cursor c=db.rawQuery("SELECT ast,future_n,may_create_real,state FROM r952_candidate",null)){ok(c.moveToFirst()&&c.getString(0).startsWith("AST3:")&&c.getInt(1)==0&&c.getInt(2)==0&&c.getString(3).equals("NURSERY"));}
   try(Cursor c=db.rawQuery("SELECT state FROM research_molecule_use_cp12",null)){ok(c.moveToFirst()&&c.getString(0).equals("REGISTERED"));}
   try(Cursor c=db.rawQuery("SELECT kind,status FROM v57_research_task_r967",null)){ok(c.moveToFirst()&&c.getString(0).equals("TRAIN_CANDIDATE")&&c.getString(1).equals("PENDING"));}
   try(Cursor action=db.rawQuery("SELECT planned_endpoint,parent_hashes_json,operator FROM research_action_v2",null)){ok(action.moveToFirst()&&action.getInt(0)==360&&action.getString(1).equals("[]")&&action.getString(2).equals("CREATE"));}
   ok(count(db,DbHelper.TABLE_TRADES)==0);ok(count(db,CandidateCalibrationR932.OBS)==0);ok(count(db,"r967_frozen_evaluation_v2")==0);
   Files.writeString(Path.of(args[3]),"{\"status\":\"PASS\",\"assertions\":"+n+",\"actual_public_seed_registration\":true,\"scope\":\"Actual public Molecular IMPORT adapter creates compact finding/action/boundary, streams SCDEV1 CAS prepare outside writer, then sole Director commit with training protocol/task/lifecycle/seed-use. Android startup bypassed only; Future0 and noREAL asserted.\"}\n");
  }finally{db.shutdown();}
 }
}
