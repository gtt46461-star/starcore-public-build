package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.nio.file.*;
public final class ArtifactAnchorContractCP12 {
 static int checks;static void ok(boolean v){checks++;if(!v)throw new AssertionError("check"+checks);}
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);db.execSQL("PRAGMA foreign_keys=ON");try{
   ok(count(db,"research_development_receipt_v2")==1);String artifact;try(Cursor c=db.rawQuery("SELECT artifact_hash FROM research_development_receipt_v2",null)){c.moveToFirst();artifact=c.getString(0);}
   db.beginTransaction();try{RetentionArtifactIdentityCP12.install(db);ok(count(db,"research_artifact_identity_v2")==1);}finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("PRAGMA foreign_key_list(research_development_receipt_v2)",null)){boolean old=false;while(c.moveToNext())if(c.getString(2).equals("r952_artifact"))old=true;ok(old);}
   db.beginTransaction();try{RetentionArtifactIdentityCP12.install(db);RetentionArtifactIdentityCP12.register(db,artifact);db.setTransactionSuccessful();}finally{db.endTransaction();}
   ok(count(db,"research_artifact_identity_v2")==1);ok(count(db,"research_development_receipt_v2")==1);
   db.beginTransaction();try{RetentionArtifactIdentityCP12.install(db);db.setTransactionSuccessful();}finally{db.endTransaction();}ok(count(db,"research_artifact_identity_v2")==1);
   try(Cursor c=db.rawQuery("PRAGMA foreign_key_list(research_development_receipt_v2)",null)){boolean anchor=false,old=false;while(c.moveToNext()){if(c.getString(2).equals("research_artifact_identity_v2"))anchor=true;if(c.getString(2).equals("r952_artifact"))old=true;}ok(anchor&&!old);}
   try{db.execSQL("DELETE FROM research_artifact_identity_v2");throw new AssertionError("anchor deletion allowed");}catch(IllegalStateException expected){checks++;}
   try{db.execSQL("UPDATE research_development_receipt_v2 SET receipt_json='{}'");throw new AssertionError("receipt changed");}catch(IllegalStateException expected){checks++;}
   db.beginTransaction();try{
    db.execSQL("DELETE FROM r952_learning_cursor");db.execSQL("UPDATE v57_research_task_r967 SET status='EXPIRED',last_error='HOST_CONTRACT_EXPIRE_FREEZE' WHERE kind='FREEZE_CANDIDATE'");
    db.execSQL("DELETE FROM r952_artifact WHERE artifact_hash=?",new Object[]{artifact});ok(count(db,"r952_artifact")==0);ok(count(db,"research_development_receipt_v2")==1);ok(count(db,"research_artifact_identity_v2")==1);
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!c.moveToFirst());}
   Files.writeString(Path.of(args[2]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"scope\":\"ActualJava installer oldreceiptFK→smallanchor migration, rollback, rerun, immutableguards and rawartifact deletion retaining permanentreceipt on realhostSQLite.\"}\n");
  }finally{db.shutdown();}
 }
}
