package com.ethk3.novaonepro;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.zip.*;import org.json.*;
/** Whole current product capture with actual APK assets and complete DDL; Android API adapters only. */
public final class ReportPhoneSnapshotR997 {
 static int checks;static void need(boolean b,String reason){checks++;if(!b)throw new AssertionError(reason);}
 public static void main(String[]args)throws Exception {
  File root=new File(args[0]),apk=new File(args[1]),schema=new File(args[2]);root.mkdirs();
  String id=String.format("%064x",997);File job=new File(root,"r981_reports/jobs/"+id);job.mkdirs();
  File snapshot=new File(job,"snapshot.db");Files.copy(schema.toPath(),snapshot.toPath());
  JSONObject request=new JSONObject().put("requestId",id).put("releaseHash",ReleaseManifestR926.MANIFEST_SHA256).put("observation",new JSONObject());
  File memberInput=new File(job,"members.jsonl");ReportNativeCP4.loadForDiagnosticProcess("fixture:diagnostics","fixture");
  ReportCatalogCP4.Result catalog;
  try(android.database.sqlite.SQLiteDatabase copy=new android.database.sqlite.SQLiteDatabase(args[3],snapshot.getPath());Writer members=new FileWriter(memberInput)) {
   ReportAttachmentClosureCP12.capture(new android.content.Context(root,apk),copy,job,request,()->true,members);
   try(ReportReadContextCP4 ctx=new ReportReadContextCP4(copy,id,ReportArchiveCP4.hash(snapshot,()->true),System.currentTimeMillis(),
       new ReportReadContextCP4.RuntimeObservationCP4(ReportJobJournalCP12.readAtomic(new File(job,"runtime-observation.json"),65536)),
       new ReportReadContextCP4.SourceManifestCP4(ReportJobJournalCP12.readAtomic(new File(job,"source-manifest.json"),65536)),new android.os.CancellationSignal(),3L<<20)) {
    catalog=ReportCatalogCP4.write(ctx,job,(phase,count)->{});ctx.finishArtifacts();
    for(File page:catalog.indexFiles)ReportMemberPlanCP12.append(members,page.getName(),"catalog/"+page.getName(),ReportArchiveCP4.hash(page,()->true),page.length());
    File summary=ReportCatalogCP4.writeReadableSummary(job,catalog,()->true);ReportMemberPlanCP12.append(members,summary.getName(),summary.getName(),ReportArchiveCP4.hash(summary,()->true),summary.length());
   }
   File renders=new File(job,"render-artifacts.jsonl");if(renders.isFile())members.write(Files.readString(renders.toPath()));
  }
  JSONObject source=new JSONObject(ReportJobJournalCP12.readAtomic(new File(job,"source-manifest.json"),65536));
  need(source.getLong("seedRegistryRows")==2,"both real R996 seed rows retained");need(source.getLong("seedIdentityMismatches")>=1,"historical mismatch not blessed");
  need(Files.readString(new File(job,"seed-bundle-observations.jsonl").toPath()).contains("SEED_IDENTITY_MISMATCH"),"exact evidence");
  need(ReportAttachmentClosureCP12.isComplete(job,ReportArchiveCP4.hash(snapshot,()->true),()->true),"capture closure complete");
  int results=0,readErrors=0;
  for(File page:catalog.indexFiles)try(BufferedReader rows=new BufferedReader(new FileReader(page))){String line;while((line=rows.readLine())!=null){JSONObject row=new JSONObject(line);if("check_result".equals(row.optString("kind"))){results++;if("READ_ERROR".equals(row.optString("execution_state")))readErrors++;}}}
  need(results==96,"every canonical result retained");need(readErrors>0&&!catalog.checksComplete,"missing observation retained as incomplete verification");
  // Run all handlers on a real old phone snapshot; new-phone runtime observations are absent.
  File archive=new File(job,"archive");archive.mkdir();
  try(Writer members=new FileWriter(memberInput,true)){ReportMemberPlanCP12.append(members,"snapshot.db","snapshot.db",ReportArchiveCP4.hash(snapshot,()->true),snapshot.length());}
  File sorted=ReportMemberPlanCP12.sort(job,memberInput,()->true);
  try(ReportArchiveCP4 writer=new ReportArchiveCP4(archive,id);BufferedReader rows=new BufferedReader(new FileReader(sorted))) {
   String line;while((line=rows.readLine())!=null){JSONObject r=new JSONObject(line);writer.add(r.getString("name"),new File(job,r.getString("path")),4L<<20,()->true);}
   writer.finish(catalog.evidenceCatalogComplete,catalog.checksComplete,"R997_HOST_EXPORT_R996_REAL_PHONE_SNAPSHOT_NEW_APK_NOT_PHONE_RUNTIME");
  }
  ReportArchiveVerifierCP12.Receipt proof=ReportArchiveVerifierCP12.verify(archive,id,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),()->true,null);
  need(proof.evidenceComplete&&!catalog.allChecksPassed,"evidence and diagnostic verification distinct");
  ReportPublicationCP12.prepareSingleZip(job,archive,id,proof.manifestSha256,proof.evidenceComplete,proof.checksComplete,catalog.allChecksPassed,()->true);
  JSONObject delivery=new JSONObject(ReportJobJournalCP12.readAtomic(new File(job,"delivery/index.json"),65536));
  File zipFile=new File(job,delivery.getJSONArray("files").getJSONObject(0).getString("path"));
  try(ZipFile zip=new ZipFile(zipFile)) {
   need(zip.getEntry("snapshot.db")!=null,"full database exported");need(zip.getEntry("seed-bundle-observations.jsonl")!=null,"seed mismatch evidence exported");
   need(zip.getEntry("installed-app.apk")!=null,"actual R997 test APK exported");
   JSONObject assets=source.getJSONObject("assets");
   for(String asset:new String[]{"REQUIREMENTS_1832.csv","CARDS_299.csv","SUBITEMS.csv","IDENTITY.json"}) {
    JSONObject ref=assets.getJSONObject("diagnostics/construction-current-"+asset);
    need(zip.getEntry(ref.getString("member"))!=null,"complete current ledger exported: "+asset);
   }
  }
  System.out.println("PASS|checks="+checks+"|zipBytes="+zipFile.length()+"|scope=actual current product capture, actual APK assets, real SQLite, R996 actual phone snapshot, R997 APK;NEW_PHONE_RUNTIME_NOT_VERIFIED");
 }
}
