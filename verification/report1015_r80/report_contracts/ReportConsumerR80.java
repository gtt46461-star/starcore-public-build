package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import java.io.*;import java.nio.file.*;import org.json.*;import java.util.*;
public class ReportConsumerR80{
 static int n; static void ok(boolean b,String s){if(!b)throw new AssertionError(s);n++;System.out.println("PASS "+s);}
 static JSONObject table(JSONObject scope,String name)throws Exception{JSONArray a=scope.getJSONArray("recentEvidence");for(int i=0;i<a.length();i++)if(name.equals(a.getJSONObject(i).getString("table")))return a.getJSONObject(i);throw new AssertionError(name);}
 public static void main(String[]args)throws Exception{File file=new File(args[1]),job=new File(args[2]);
 try(SQLiteDatabase db=new SQLiteDatabase(args[0],file.getPath())){
  DbStore store=HostProductR974.store(db);ResearchPredrawCaptureR923.Result result=ResearchPredrawCaptureR923.captureTargetWithinR927(store,"202610150001",100);
  // Existing draw means truthful rejected capture; publish records actual owner result, not synthetic success.
  String key=store.getState("r80_probe_last_status","");ok(!key.isEmpty(),"actual research owner publishes probe status");
  Map<String,String> sentinel=new LinkedHashMap<>();sentinel.put("private_test_credential","SYNTHETIC_EXCLUDED_SENTINEL");store.setStates(sentinel);
  db.makeReadOnly();String hash=ReportArchiveCP4.hash(file,()->true);
  String out=Files.readString(ReportDiagnosticEvidenceR64.write(db,job,hash,()->true).toPath());JSONObject scope=new JSONObject(out);
  ok(!out.contains("SYNTHETIC_EXCLUDED_SENTINEL"),"unrelated state excluded");ok(!scope.getBoolean("rawDatabaseIncluded"),"compact explicitly omits raw database");
  JSONArray states=scope.getJSONArray("researchSchedulerState");boolean found=false;for(int i=0;i<states.length();i++){JSONObject s=states.getJSONObject(i);if(s.getString("key").equals("r80_probe_last_status"))found=s.getString("value").equals(key)&&s.getString("status").equals("READ_OK");}ok(found,"writer persisted exact key consumed losslessly");
  String t=args[3].equals("probe")?"r80_probe_forecast":"research_bound_input_r80";JSONObject rows=table(scope,t);ok(rows.getBoolean("complete")&&rows.getJSONArray("rows").length()>0,"actual owner receipts exported with complete text "+t);
  org.json.JSONObject parsed=new JSONObject(MultiHorizonProbeR80.reportJson(db));ok(parsed.toString().equals(scope.getJSONObject("multiHorizonProbeResearch").toString()),"standalone probe consumer equals embedded report");
  ok(!parsed.getBoolean("correlatedHorizonsAreIndependentSamples")&&parsed.getString("efficacy").equals("NOT_PROVEN"),"no pooled sample or efficacy assertion");
  for(String profile:new String[]{"COMPLETE_DIAGNOSTIC_R68","COMPACT_DIAGNOSTIC_R64","FULL_RAW_EVIDENCE"}){
   try(ReportReadContextCP4 ctx=new ReportReadContextCP4(db,"fixture",hash,System.currentTimeMillis(),new ReportReadContextCP4.RuntimeObservationCP4("{}"),new ReportReadContextCP4.SourceManifestCP4(new JSONObject().put("reportProfile",profile).toString()),new android.os.CancellationSignal(),3L<<20)){
    System.out.println("PROFILE_CONSUMER "+profile+" -> "+ctx.evidenceMemberR64());if(profile.equals("COMPLETE_DIAGNOSTIC_R68")&&!ctx.compactDiagnosticR64())System.out.println("CONFIRMED_DEFECT current compact writer profile is read as raw snapshot.db");
   }
  }
  boolean cancelled=false;try{ReportDiagnosticEvidenceR64.write(db,job,hash,()->false);}catch(InterruptedIOException expected){cancelled=true;}ok(cancelled,"stream cancellation propagates without success receipt");
  JSONObject rebuilt=new JSONObject(Files.readString(ReportDiagnosticEvidenceR64.write(db,job,hash,()->true).toPath()));ok(rebuilt.getString("snapshotSha256").equals(hash),"cancelled stream rebuilt against same identity");ok(hash.equals(ReportArchiveCP4.hash(file,()->true)),"read-only snapshot unchanged");
  System.out.println("PASS REPORT_CONSUMER assertions="+n+" scope=actual_product_real_sqlite_synthetic_owner_receipts_not_device");
 }
 }
}
