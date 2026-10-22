package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import java.io.*;import java.nio.file.*;import org.json.*;
/** Compact report evidence on a copied DB; observed readiness is an explicit test input. */
public final class ReportEvidenceR65 {
 static void need(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS|"+s);}
 public static void main(String[]a)throws Exception{
  File job=new File(a[3]);job.mkdirs();File file=new File(job,"snapshot.db");Files.copy(Path.of(a[1]),file.toPath());
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],file.getPath())){
   db.execSQL("INSERT OR REPLACE INTO app_state VALUES('r65_director_last_slot','IMPORT',1)");
   db.execSQL("INSERT OR REPLACE INTO app_state VALUES('private_test_credential','MUST_NEVER_APPEAR_IN_DIAGNOSTIC_SCOPE',1)");
  }
  String hash=ReportArchiveCP4.hash(file,()->true);
  try(SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
   JSONObject observation=new JSONObject(Files.readString(Path.of(a[2],"runtime-observation.json"))).put("researchSchemaReady",true);
   String source=Files.readString(Path.of(a[2],"source-manifest.json"));
   try(ReportReadContextCP4 ctx=new ReportReadContextCP4(db,"host-r65-foundation-test",hash,System.currentTimeMillis(),new ReportReadContextCP4.RuntimeObservationCP4(observation.toString()),new ReportReadContextCP4.SourceManifestCP4(source),new android.os.CancellationSignal(),3L<<20)){
    ReportCatalogCP4.Result results=ReportCatalogCP4.write(ctx,job,(phase,n)->{});ctx.finishArtifacts();
    need(results.checks==96&&results.queryErrors==0&&results.checksComplete,"96_CHECKS_EXECUTED_WITHOUT_QUERY_ERROR");need(!results.allChecksPassed,"UNSTARTED_RESEARCH_NOT_ALL_PASS");
    boolean pending=false;for(File page:results.indexFiles)for(String line:Files.readAllLines(page.toPath())){JSONObject row=new JSONObject(line);if("check_result".equals(row.optString("kind"))&&"RSEC-078".equals(row.optString("id"))){System.out.println("FOUNDATION_HEALTH|"+row.optString("health")+"|"+row.optString("state"));pending=!"PASS".equals(row.optString("health"))&&row.optString("evidence").contains("researchReadiness=RESEARCH_FOUNDATION_PENDING");}}
    need(pending,"READY_SCHEMA_WITH_EMPTY_RESEARCH_EXPLICITLY_PENDING");
   }
   File evidence=ReportDiagnosticEvidenceR64.write(db,job,hash,()->true);String text=Files.readString(evidence.toPath());JSONObject scope=new JSONObject(text);
   need(!text.contains("MUST_NEVER_APPEAR_IN_DIAGNOSTIC_SCOPE"),"STATE_ALLOWLIST_EXCLUDES_UNRELATED_CREDENTIALS");
   boolean cursor=false,notRecorded=false;JSONArray states=scope.getJSONArray("researchSchedulerState");for(int i=0;i<states.length();i++){JSONObject row=states.getJSONObject(i);if("r65_director_last_slot".equals(row.getString("key")))cursor="IMPORT".equals(row.getString("value"));if("NOT_RECORDED".equals(row.getString("status")))notRecorded=true;}
   need(cursor&&notRecorded,"SCHEDULER_EVIDENCE_DISTINGUISHES_RECORDED_AND_MISSING");
   JSONArray rows=scope.getJSONArray("recentEvidence");boolean importSeen=false,laneSeen=false;for(int i=0;i<rows.length();i++){String t=rows.getJSONObject(i).getString("table");importSeen|=t.equals("research_atom_catalog_import_cp12");laneSeen|=t.equals("v57_research_lane_cursor_r967");}
   need(importSeen&&laneSeen,"IMPORT_AND_LANE_METADATA_EXPORTED");
   need(text.contains("next_index")&&text.contains("through_issue")&&text.contains("development_sample_n"),"ACTUAL_FEATURE_MATRIX_AND_TRAINING_RECEIPT_COLUMNS_COVERED");
  }
  need(hash.equals(ReportArchiveCP4.hash(file,()->true)),"REPORT_READONLY_SNAPSHOT_UNCHANGED");
  System.out.println("R65_REPORT_EVIDENCE_PASS|scope=HOST_COPIED_DATABASE_AND_EXPLICIT_OBSERVATION_INPUTS_NOT_PHONE");
 }
}
