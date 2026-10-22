package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.nio.file.*;
/** Exercise actual Java installer entry points, not only their mirrored SQL files. */
public final class RetentionInstallerContractCP12 {
 static int checks;static void ok(boolean value){checks++;if(!value)throw new AssertionError("check"+checks);}
 static void reject(Runnable body){checks++;try{body.run();}catch(IllegalStateException expected){return;}throw new AssertionError("outside TX accepted");}
 static void install(SQLiteDatabase db){RetentionArtifactIdentityCP12.install(db);RetentionTaskExpiryCP12.schema(db);RetentionDurableModelQuotaCP12.schema(db);RetentionWorkingStorageQuotaCP12.schema(db);RetentionRevisionSchemaCP12.schema(db);}
 static void fk(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!c.moveToFirst());}}
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);db.execSQL("PRAGMA foreign_keys=ON");try{
   reject(()->RetentionArtifactIdentityCP12.install(db));reject(()->RetentionTaskExpiryCP12.schema(db));reject(()->RetentionDurableModelQuotaCP12.schema(db));reject(()->RetentionWorkingStorageQuotaCP12.schema(db));reject(()->RetentionRevisionSchemaCP12.schema(db));
   for(int rerun=0;rerun<2;rerun++){db.beginTransaction();try{install(db);fk(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
   try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name<>'sqlite_sequence'",null)){ok(c.moveToFirst()&&c.getInt(0)==RetentionTableRegistryCP12.ALL_INSTALLED.size());}
   try(Cursor c=db.rawQuery("SELECT bucket,byte_count FROM research_working_storage_cp12 ORDER BY bucket",null)){int count=0;while(c.moveToNext()){ok(c.getLong(1)==0);count++;}ok(count==3);}
   try(Cursor c=db.rawQuery("PRAGMA foreign_key_list(research_development_receipt_v2)",null)){boolean found=false;while(c.moveToNext())if(c.getString(2).equals("research_artifact_identity_v2"))found=true;ok(found);}
   fk(db);
   Files.writeString(Path.of(args[2]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"scope\":\"Actual five production Java installer entry points reject non-Owner TX, execute and rerun inside real hostSQLite current415schema with FK ON; exact table count and zero-source working counters preserved. Root ensure CP12 step and full fresh source DDL execution are separately root-owned.\"}\n");
  }finally{db.shutdown();}
 }
}
