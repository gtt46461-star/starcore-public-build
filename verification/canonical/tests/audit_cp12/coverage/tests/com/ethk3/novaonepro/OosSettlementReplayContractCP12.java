package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.lang.reflect.*;import java.nio.file.*;
public final class OosSettlementReplayContractCP12 {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("check"+n);}
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 public static void main(String[] a)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(a[0],a[1]);try{
   Models.Draw d=new Models.Draw();d.issue="202609230002";d.openTime=200;for(int i=0;i<10;i++)d.numbers[i]=i+1;String H=V51ChecksumEngine.sha256("identity");
   db.execSQL("INSERT INTO draws(issue,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10)VALUES(?,200,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{d.issue});
   db.execSQL("INSERT INTO v57_shadow_observation_r35(target_issue,game_code,base_issue,position,n1,n2,n3,lane,decision_action,reject_code,bundle_hash,contract_hash,authorization_hash,record_hash,created_at,commit_at,pre_draw_frozen,history_hash,snapshot_hash,action_space_hash,history_max_issue,selected_hit_p)VALUES(?,'Binance1-PK10','202609230001',1,1,2,3,'SHADOW','NO_BET','',?,?,?, ?,100,100,1,?,?,?,'202609230001',0.3)",new Object[]{d.issue,H,H,H,H,H,H,H});
   Constructor<SettlementEngine.OwnerToken> ctor=SettlementEngine.OwnerToken.class.getDeclaredConstructor(String.class,long.class);ctor.setAccessible(true);SettlementEngine.OwnerToken owner=ctor.newInstance(d.issue,1L);
   db.beginTransaction();try{n++;try{OosAfterDrawWriterR36.settleFrozenWithinTransaction(db,null,d);throw new AssertionError("owner bypass");}catch(SecurityException expected){}}finally{db.endTransaction();}
   db.beginTransaction();try{ok(OosAfterDrawWriterR36.settleFrozenWithinTransaction(db,owner,d).success);}finally{db.endTransaction();}ok(count(db,DbHelper.TABLE_R35_OOS_LABEL)==0);
   db.beginTransaction();try{ok(OosAfterDrawWriterR36.settleFrozenWithinTransaction(db,owner,d).success);db.setTransactionSuccessful();}finally{db.endTransaction();}
   db.beginTransaction();try{OosAfterDrawWriterR36.LabelResult replay=OosAfterDrawWriterR36.settleFrozenWithinTransaction(db,owner,d);ok(replay.success&&replay.replay);db.setTransactionSuccessful();}finally{db.endTransaction();}ok(count(db,DbHelper.TABLE_R35_OOS_LABEL)==1);
   db.execSQL("UPDATE v57_shadow_observation_r35 SET snapshot_hash=? WHERE target_issue=?",new Object[]{V51ChecksumEngine.sha256("different"),d.issue});
   db.beginTransaction();try{n++;try{OosAfterDrawWriterR36.settleFrozenWithinTransaction(db,owner,d);throw new AssertionError("hash bypass");}catch(IllegalStateException expected){}}finally{db.endTransaction();}
   db.execSQL("UPDATE v57_shadow_observation_r35 SET selected_hit_p=2 WHERE target_issue=?",new Object[]{d.issue});db.beginTransaction();try{OosAfterDrawWriterR36.LabelResult invalid=OosAfterDrawWriterR36.settleFrozenWithinTransaction(db,owner,d);ok(!invalid.success&&invalid.status.equals("FROZEN_PROBABILITY_INVALID"));}finally{db.endTransaction();}
   ok(count(db,DbHelper.TABLE_TRADES)==0);Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"assertions\":"+n+",\"scope\":\"Actual package OOS handoff executes only with test-created private OwnerToken; SQLite owner rejection, prediction transaction rollback, exact replay, provenance drift rejection and invalid probability rejection. Token reflection is test fixture only, no production reflective caller.\"}\n");
  }finally{db.shutdown();}
 }
}
