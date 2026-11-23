package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.File;
import java.nio.file.Paths;
public class FieldStartup {
 public static void main(String[] args) throws Exception {
  SQLiteDatabase db=new MainChainSchemaR31Host.Owner(Paths.get(args[0])).db;
  db.execSQL("INSERT INTO r31_keep_cutover VALUES('r29_model_checkpoint','CAS_DUPLICATE_RETIREMENT','fixture',1,'fixture','fixture','fixture',1)");
  db.execSQL("INSERT OR IGNORE INTO r31_startup_checkpoint_source VALUES(1,0)");
  db.execSQL("INSERT INTO r29_model_checkpoint VALUES('fixture','fixture','fixture','1','fixture',X'030102',1)");
  StartupCheckpointIdentityR31.Prepared p=StartupCheckpointIdentityR31.prepare(db,new File(args[1]));
  if(args.length>2 && args[2].equals("dirty")) db.execSQL("UPDATE r29_model_checkpoint SET artifact=X'ff' WHERE owner='fixture'");
  if(args.length>2 && args[2].equals("shadow")) db.execSQL("CREATE TEMP TABLE r29_model_checkpoint (owner TEXT)");
  if(args.length>2 && args[2].equals("workspace")) db.execSQL("CREATE TEMP TABLE r335_extension_catalog(original_id INTEGER PRIMARY KEY,type,name,tbl_name,sql)");
  db.beginTransaction();
  try {RuntimeCoordinatorR26.startupUpgradeSpecR31("field-replay",p).transform.run(db);db.setTransactionSuccessful();}
  finally {db.endTransaction();}
  try (android.database.Cursor c=db.rawQuery("SELECT count(*) FROM r31_startup_checkpoint_receipt WHERE actual_new_archive_rows=1 AND source_rows=1", null)) {
   if (!c.moveToFirst() || c.getLong(0)!=1) throw new AssertionError("missing checkpoint receipt");
  }
  try (android.database.Cursor c=db.rawQuery("SELECT count(*) FROM r30_keep_r29_model_checkpoint WHERE owner='fixture' AND artifact=X'030102'",null)) {
   if (!c.moveToFirst() || c.getLong(0)!=1) throw new AssertionError("checkpoint bytes changed");
  }
  System.out.println("COMMITTED_CHECKPOINT_AND_RECEIPT");
 }
}
