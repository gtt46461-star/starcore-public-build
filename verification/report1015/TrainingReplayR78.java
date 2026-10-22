package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;import java.util.*;import java.io.*;
/** Original immutable report CAS inputs, replayed on a separate host fixture.
 * Report omits partial feature blobs: this reconstructs features from row zero,
 * not a claim to have replayed the original on-phone checkpoint or scheduler. */
public final class TrainingReplayR78 {
 static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 public static void main(String[]a)throws Exception{
  try(SQLiteDatabase d=new SQLiteDatabase(a[0],a[1])){
   DbStore s=HostProductR974.store(d);File files=new File(a[2]);files.mkdirs();
   android.content.Context context=new android.app.Application(files,new File(a[3]));
   HostProductR974.set(s,"researchAppCP12",context);HostProductR974.set(s.coordinatorForResearchR952(),"app",context);
   File cas=new File(files,"research-input-v2");cas.mkdirs();HostProductR974.set(s,"researchInputRootCP11",cas);
   int inputs=0;try(Cursor c=d.rawQuery("SELECT i.input_hash,i.byte_count,i.first_issue,i.last_issue,p.protocol_json FROM research_training_input_v2 i JOIN research_training_protocol_v2 p ON p.input_hash=i.input_hash",null)){
    while(c.moveToNext()){
     List<Models.Draw> history=new ArrayList<>();
     try(Cursor rows=d.rawQuery("SELECT issue,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue>=? AND issue<=? AND dirty=0 ORDER BY issue",new String[]{c.getString(2),c.getString(3)})){
      while(rows.moveToNext()){Models.Draw row=new Models.Draw();row.issue=rows.getString(0);row.openTime=rows.getLong(1);for(int p=0;p<10;p++)row.numbers[p]=rows.getInt(p+2);history.add(row);}
     }
     DevelopmentInputCP9.Header header=new DevelopmentInputCP9.Header(new org.json.JSONObject(c.getString(4)).getString("header"));
     DevelopmentInputCP9.Receipt receipt;try(FileOutputStream out=new FileOutputStream(new File(cas,c.getString(0)+".scdev"))){receipt=DevelopmentInputCP9.write(out,header,history);}
     if(!receipt.inputHash.equals(c.getString(0))||receipt.byteCount!=c.getLong(1))throw new AssertionError("ORIGINAL_CAS_IDENTITY_MISMATCH");inputs++;
    }
   }
   System.out.println("EXACT_ORIGINAL_INPUTS="+inputs);RealtimePressureGateR811.resetForTests();
   for(int turn=0;turn<600;turn++){
    long before=n(d,"SELECT COUNT(*) FROM research_training_feature_row_cp11");
    ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);
    try{ResearchTrainingHookRegistryR952.drainTrainingSlotCP11(s,System.nanoTime()+775_000_000L);}catch(ResearchWorkBudgetR927.Yield expected){}finally{ResearchWorkBudgetR927.leave(scope);}
    long rows=n(d,"SELECT COUNT(*) FROM research_training_feature_row_cp11"),done=n(d,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='COMPLETED'"),failed=n(d,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE status='FAILED_TERMINAL'");
    if(turn%10==0||done==inputs||failed>0)System.out.println("TURN="+turn+" rows="+rows+" advanced="+(rows-before)+" completed="+done+" failed="+failed);
    if(failed>0&&a.length<5){try(Cursor c=d.rawQuery("SELECT last_error FROM v57_research_task_r967 WHERE status='FAILED_TERMINAL'",null)){while(c.moveToNext())System.out.println("FAIL_REASON "+c.getString(0));}throw new AssertionError("TERMINAL_TRAIN_FAILURE");}
    if(done+failed==inputs){System.out.println("PASS ORIGINAL_REPORT_TRAINING_REPLAY completed="+done+" rejected="+failed+" features="+rows+" receipts="+n(d,"SELECT COUNT(*) FROM research_development_receipt_v2")+" scope=COMPONENT_HOST_REPLAY_NOT_PHONE_CONTINUITY");return;}
    Thread.sleep(40);
   }
   throw new AssertionError("TRAIN_REPLAY_NOT_COMPLETED_WITHIN_BOUNDED_RUN");
  }
 }
}
