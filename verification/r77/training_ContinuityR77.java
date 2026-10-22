package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;import java.io.*;import java.nio.file.*;import java.util.*;import org.json.*;

/** Replays original report CAS identities and real Owner work, never creates Future witnesses. */
public final class training_ContinuityR77 {
 static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static DbStore store(SQLiteDatabase d,String[]a)throws Exception{DbStore s=HostProductR974.store(d);File root=new File(a[2]);root.mkdirs();android.content.Context context=new android.app.Application(root,new File(a[3]));HostProductR974.set(s,"researchAppCP12",context);HostProductR974.set(s.coordinatorForResearchR952(),"app",context);HostProductR974.set(s,"researchInputRootCP11",new File(root,"research-input-v2"));return s;}
 static final class ProbedDb extends SQLiteDatabase {
  final String mode;int turn,writeFailures,slowReads;boolean armed;java.util.Map<String,Integer> owners=new java.util.TreeMap<>();
  ProbedDb(String bridge,String path,String mode)throws Exception{super(bridge,path);this.mode=mode;}
  @Override public synchronized Cursor rawQuery(String q,String[] args){
   if(mode.equals("slow")&&q.startsWith("SELECT c.ast,p.protocol_json"))try{slowReads++;Thread.sleep(40);}catch(InterruptedException x){Thread.currentThread().interrupt();}
   long started=System.nanoTime();Cursor result=super.rawQuery(q,args);
   if(mode.equals("profile")&&(q.startsWith("SELECT row_index")||q.startsWith("SELECT length(partial_payload)")))System.out.println("PROFILE query_ms="+(System.nanoTime()-started)/1000000+" remaining_ms="+ResearchWorkBudgetR927.remainingMs()+" sql="+q.substring(0,Math.min(95,q.length())));
   return result;
  }
  @Override public synchronized void execSQL(String q,Object[] args){
   if(q.contains("r65_director_last_slot',")&&args!=null&&args.length>0)owners.merge(String.valueOf(args[0]),1,Integer::sum);
   if(mode.equals("write")&&armed&&q.startsWith("INSERT INTO research_training_feature_row_cp11")){armed=false;writeFailures++;throw new IllegalStateException("R77_INJECTED_WRITE_DEFER");}
   super.execSQL(q,args);
  }
 }
 static String value(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getString(0):"";}}
 static void requireRealHostClock(){long wall=System.nanoTime(),mono=TimeSourceR811.monoNowMs();for(int i=0;i<10000;i++)TimeSourceR811.monoNowMs();long drift=(TimeSourceR811.monoNowMs()-mono)-(System.nanoTime()-wall)/1000000;if(Math.abs(drift)>50)throw new AssertionError("HOST_CLOCK_NOT_ELAPSED_REALTIME:"+drift);}
 public static void main(String[]a)throws Exception{
  requireRealHostClock();
  try(ProbedDb d=new ProbedDb(a[0],a[1],a[6])){
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
    s=store(d,a);d.turn=turn;d.armed=d.armed||turn%17==0;long oldFeatures=n(d,"SELECT COUNT(*) FROM research_training_feature_row_cp11");long oldOnline=n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='COMPLETED'");
    boolean pressure=mode.equals("p0")&&turn%11==0;if(pressure)RealtimePressureGateR811.beginAnalysis();
    long started=System.nanoTime();String outcome="OK";ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);
    try{
     outcome=ResearchLearningContinuityR912.pulse(s,850).status;
    }catch(ResearchWorkBudgetR927.Yield y){outcome=y.getMessage();}finally{ResearchWorkBudgetR927.leave(scope);if(pressure)RealtimePressureGateR811.endAnalysis();}
    long elapsed=(System.nanoTime()-started)/1_000_000;long features=n(d,"SELECT COUNT(*) FROM research_training_feature_row_cp11"),receipts=n(d,"SELECT COUNT(*) FROM research_development_receipt_v2");
    JSONObject p=new JSONObject().put("turn",turn).put("ms",elapsed).put("feature_rows",features).put("receipts",receipts).put("outcome",outcome).put("online_completed",n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='COMPLETED'")).put("online_cursor",value(d,"SELECT MAX(l.trained_until_issue) FROM r952_learning_cursor l JOIN r952_candidate c ON l.consumer_id='R952:'||c.candidate_id WHERE c.ast NOT LIKE 'AST3:%'")).put("owners",new JSONObject(d.owners)).put("write_failures",d.writeFailures).put("slow_reads",d.slowReads);pulses.put(p);System.out.println(p);
    if(n(d,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='FAILED_TERMINAL'")>0)throw new AssertionError("TERMINAL_MODERN_FAILURE");
    if(pressure&&(features!=oldFeatures||oldOnline!=n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='COMPLETED'")))throw new AssertionError("P0_PUBLISHED_TRAINING");
    Thread.sleep(100);
   }
   if(mode.equals("write")&&d.writeFailures==0)throw new AssertionError("WRITE_FAULT_NOT_EXERCISED");
   if(mode.equals("slow")&&d.slowReads==0)throw new AssertionError("SLOW_READ_NOT_EXERCISED");
   if(future!=n(d,"SELECT SUM(future_n) FROM r952_candidate")||cert!=n(d,"SELECT COUNT(*) FROM research_certificate_r929")||real!=n(d,"SELECT COUNT(*) FROM trades"))throw new AssertionError("PREDICTION_EVIDENCE_CHANGED");
   Files.writeString(Path.of(a[4]+"-pulses.json"),pulses.toString(2));
  }
 }
}
