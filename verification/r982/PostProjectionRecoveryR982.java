package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.lang.reflect.Method;
import com.ethk3.novaonepro.r952.ResearchDirectorR952;
/** Explicit synthetic incomplete-old-row fixture; exercises the actual POST repair consumer. */
public final class PostProjectionRecoveryR982 {
 static int n;static void check(boolean v){n++;if(!v)throw new AssertionError("post:"+n);}
 public static void main(String[] args)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);try {
   DbStore store=HostProductR974.store(db);String hash=V51ChecksumEngine.sha256("synthetic-post-identity");
   for(String issue:new String[]{"202609230001","202609230002"}){
    db.execSQL("INSERT INTO v57_settlement_task_r36(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash) VALUES(?,?,'COMPLETED',1,2,300,?,?)",new Object[]{issue,issue,hash,hash});
    db.execSQL("INSERT INTO r952_candidate_predraw(target_issue,candidate_id,prediction_mask,position,snapshot_hash,structure_hash,model_hash,code_hash,data_hash,captured_at,input_hash) VALUES(?,'synthetic-rejected-proof',7,1,?,?,?,?,?,250,?)",new Object[]{issue,hash,hash,hash,hash,hash,hash});
   }
   db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES('202609230002',200,201,201,0,1,2,3,4,5,6,7,8,9,10)");
   Method method=ResearchDirectorR952.class.getDeclaredMethod("drainPostSlotCP12",DbStore.class,long.class);method.setAccessible(true);
   check(Boolean.TRUE.equals(method.invoke(null,store,System.nanoTime()+30000000000L)));
   try(Cursor c=db.rawQuery("SELECT target_issue,proof_error,settled FROM r952_candidate_predraw ORDER BY target_issue",null)){
    check(c.moveToNext()&&c.getString(0).equals("202609230001")&&c.getString(1).isEmpty()&&c.getInt(2)==0);
    check(c.moveToNext()&&c.getString(0).equals("202609230002")&&c.getString(1).equals("R958_STRICT_FUTURE_PROOF_REJECTED")&&c.getInt(2)==0);
   }
   try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM r952_candidate_predraw WHERE settled<>0",null)){check(c.moveToFirst()&&c.getLong(0)==0);}
   System.out.println("PASS|assertions="+n+"|missing old draw retained; next clean terminal projection consumed; invalid capture rejected; no manufactured Future labels");
  }finally{db.shutdown();}
 }
}
