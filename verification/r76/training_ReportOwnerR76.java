package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;import java.io.*;import java.nio.file.*;import java.util.*;import org.json.*;

/** Replays original report CAS identities and real Owner work, never creates Future witnesses. */
public final class training_ReportOwnerR76 {
 static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static DbStore store(SQLiteDatabase d,String[]a)throws Exception{DbStore s=HostProductR974.store(d);File root=new File(a[2]);root.mkdirs();HostProductR974.set(s,"researchAppCP12",new android.content.Context(root,new File(a[3])));HostProductR974.set(s,"researchInputRootCP11",new File(root,"research-input-v2"));return s;}
 public static void main(String[]a)throws Exception{
  try(SQLiteDatabase d=new SQLiteDatabase(a[0],a[1])){
   RealtimePressureGateR811.resetForTests();DbStore s=store(d,a);File cas=s.researchInputRootCP11();cas.mkdirs();JSONArray identities=new JSONArray();
   try(Cursor c=d.rawQuery("SELECT i.input_hash,i.byte_count,i.first_issue,i.last_issue,p.protocol_json FROM research_training_input_v2 i JOIN research_training_protocol_v2 p ON p.input_hash=i.input_hash",null)){
    while(c.moveToNext()){
     String expected=c.getString(0);List<Models.Draw> history=new ArrayList<>();
     try(Cursor rows=d.rawQuery("SELECT issue,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue>=? AND issue<=? AND dirty=0 ORDER BY issue",new String[]{c.getString(2),c.getString(3)})){
      while(rows.moveToNext()){Models.Draw draw=new Models.Draw();draw.issue=rows.getString(0);draw.openTime=rows.getLong(1);for(int p=0;p<10;p++)draw.numbers[p]=rows.getInt(p+2);history.add(draw);}
     }
     DevelopmentInputCP9.Header h=new DevelopmentInputCP9.Header(new JSONObject(c.getString(4)).getString("header"));
     DevelopmentInputCP9.Receipt receipt;try(FileOutputStream out=new FileOutputStream(new File(cas,expected+".scdev"))){receipt=DevelopmentInputCP9.write(out,h,history);}
     if(!expected.equals(receipt.inputHash)||receipt.byteCount!=c.getLong(1))throw new AssertionError("ORIGINAL_CAS_IDENTITY_MISMATCH "+expected);
     identities.put(new JSONObject().put("hash",expected).put("bytes",receipt.byteCount).put("rows",receipt.rows));
    }
   }
   Files.writeString(Path.of(a[4]+"-inputs.json"),identities.toString(2));System.out.println("INPUT_IDENTITIES "+identities.length());
   long future=n(d,"SELECT SUM(future_n) FROM r952_candidate"),cert=n(d,"SELECT COUNT(*) FROM research_certificate_r929"),real=n(d,"SELECT COUNT(*) FROM trades");
   JSONArray pulses=new JSONArray();int maximum=Integer.parseInt(a[5]);String mode=a[6];
   for(int turn=0;turn<maximum;turn++){
    s=store(d,a);long started=System.nanoTime();String outcome="OK";ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);
    try{
     if(mode.equals("continuity"))outcome=ResearchLearningContinuityR912.pulse(s,850).status;
     else ResearchTrainingHookRegistryR952.drainTrainingSlotCP11(s,System.nanoTime()+775_000_000L);
    }catch(ResearchWorkBudgetR927.Yield y){outcome=y.getMessage();}finally{ResearchWorkBudgetR927.leave(scope);}
    long elapsed=(System.nanoTime()-started)/1_000_000;long features=n(d,"SELECT COUNT(*) FROM research_training_feature_row_cp11"),receipts=n(d,"SELECT COUNT(*) FROM research_development_receipt_v2");
    JSONObject p=new JSONObject().put("turn",turn).put("ms",elapsed).put("feature_rows",features).put("receipts",receipts).put("outcome",outcome);pulses.put(p);System.out.println(p);
    if(n(d,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='FAILED_TERMINAL'")>0)throw new AssertionError("TERMINAL_MODERN_FAILURE");
    if(receipts==6)break;Thread.sleep(100);
   }
   if(future!=n(d,"SELECT SUM(future_n) FROM r952_candidate")||cert!=n(d,"SELECT COUNT(*) FROM research_certificate_r929")||real!=n(d,"SELECT COUNT(*) FROM trades"))throw new AssertionError("PREDICTION_EVIDENCE_CHANGED");
   Files.writeString(Path.of(a[4]+"-pulses.json"),pulses.toString(2));
  }
 }
}
