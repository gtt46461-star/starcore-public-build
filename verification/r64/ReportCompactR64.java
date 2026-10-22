package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import java.io.*;import java.nio.file.*;import java.util.zip.*;import org.json.*;
/** Original user snapshot repaired by the product migration; original observation is kept as historical evidence. */
public final class ReportCompactR64 {
 static void need(boolean yes,String what){if(!yes)throw new AssertionError(what);System.out.println("PASS "+what);}
 public static void main(String[]a)throws Exception{
  File root=new File(a[0]).getCanonicalFile(),apk=new File(a[1]),schema=new File(a[2]),original=new File(a[4]);root.mkdirs();
  JSONObject observation=new JSONObject(Files.readString(new File(original,"runtime-observation.json").toPath()));
  JSONArray refs=observation.getJSONArray("fullObservations");String id=refs.getJSONObject(0).getString("path").split("/")[1];
  for(int i=0;i<refs.length();i++){JSONObject ref=refs.getJSONObject(i);File dest=new File(root,"r981_reports/"+ref.getString("path"));dest.getParentFile().mkdirs();Files.copy(new File(original,"runtime-observation-"+ref.getString("key")+".txt").toPath(),dest.toPath());}
  File job=new File(root,"r981_reports/jobs/"+id);job.mkdirs();File snapshot=new File(job,"snapshot.db");Files.copy(schema.toPath(),snapshot.toPath());
  JSONObject request=new JSONObject().put("requestId",id).put("releaseHash",ReleaseManifestR926.MANIFEST_SHA256).put("observation",observation);
  File plan=new File(job,"members.jsonl");ReportNativeCP4.loadForDiagnosticProcess("fixture:diagnostics","fixture");
  // Initialize the transport bridge, then reopen under an enforced OS-level read-only SQLite connection.
  new SQLiteDatabase(a[3],snapshot.getPath()).close();ReportCatalogCP4.Result catalog;String hash=ReportArchiveCP4.hash(snapshot,()->true);
  try(SQLiteDatabase copy=SQLiteDatabase.openDatabase(snapshot.getPath(),null,SQLiteDatabase.OPEN_READONLY);Writer members=new FileWriter(plan)){
   ReportAttachmentClosureCP12.capture(new android.content.Context(root,apk),copy,job,request,()->true,members);
   try(ReportReadContextCP4 ctx=new ReportReadContextCP4(copy,id,hash,System.currentTimeMillis(),new ReportReadContextCP4.RuntimeObservationCP4(observation.toString()),new ReportReadContextCP4.SourceManifestCP4(Files.readString(new File(job,"source-manifest.json").toPath())),new android.os.CancellationSignal(),3L<<20)){
    catalog=ReportCatalogCP4.write(ctx,job,(phase,count)->{});ctx.finishArtifacts();
    for(File page:catalog.indexFiles)ReportMemberPlanCP12.append(members,page.getName(),"catalog/"+page.getName(),ReportArchiveCP4.hash(page,()->true),page.length());
    File summary=ReportCatalogCP4.writeReadableSummary(job,catalog,()->true);ReportMemberPlanCP12.append(members,summary.getName(),summary.getName(),ReportArchiveCP4.hash(summary,()->true),summary.length());
   }
   File scope=ReportDiagnosticEvidenceR64.write(copy,job,hash,()->true);ReportMemberPlanCP12.append(members,scope.getName(),scope.getName(),ReportArchiveCP4.hash(scope,()->true),scope.length());
   File renders=new File(job,"render-artifacts.jsonl");if(renders.isFile())members.write(Files.readString(renders.toPath()));
  }
  need(catalog.checks==96&&catalog.queryErrors==0&&catalog.checksComplete,"96_CHECKS_NO_QUERY_ERRORS");need(!catalog.allChecksPassed,"UNMET_FUNCTIONS_REMAIN_UNMET");
  need(hash.equals(ReportArchiveCP4.hash(snapshot,()->true)),"REPORT_NEVER_MODIFIES_SNAPSHOT");
  need(ReportAttachmentClosureCP12.isComplete(job,hash,()->true),"ATTACHMENTS_VERIFIED");
  File archive=new File(job,"archive");archive.mkdir();File sorted=ReportMemberPlanCP12.sort(job,plan,()->true);
  try(ReportArchiveCP4 writer=new ReportArchiveCP4(archive,id);BufferedReader rows=new BufferedReader(new FileReader(sorted))){String line;while((line=rows.readLine())!=null){JSONObject r=new JSONObject(line);writer.add(r.getString("name"),new File(job,r.getString("path")),4L<<20,()->true);}writer.finish(catalog.evidenceCatalogComplete,catalog.checksComplete,"R64_COMPACT_USER_REPORT_REPLAY_NOT_PHONE");}
  ReportArchiveVerifierCP12.Receipt proof=ReportArchiveVerifierCP12.verify(archive,id,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),()->true,null);
  ReportPublicationCP12.prepareSingleZip(job,archive,id,proof.manifestSha256,proof.evidenceComplete,proof.checksComplete,catalog.allChecksPassed,()->true);
  JSONObject delivery=new JSONObject(ReportJobJournalCP12.readAtomic(new File(job,"delivery/index.json"),65536));need(delivery.getJSONArray("files").length()==1,"SINGLE_ZIP_DELIVERY");
  File zipFile=new File(job,delivery.getJSONArray("files").getJSONObject(0).getString("path"));
  try(ZipFile zip=new ZipFile(zipFile)){need(zip.getEntry("snapshot.db")==null&&zip.getEntry("installed-app.apk")==null,"RAW_PAYLOADS_OMITTED");need(zip.getEntry("DIAGNOSTIC_SCOPE.json")!=null&&zip.getEntry("REPORT_DETAILS.txt")!=null,"READABLE_AND_STRUCTURED_DIAGNOSTICS_PRESENT");}
  need(zipFile.length()<10L*1024*1024,"COMPACT_UNDER_10_MIB_FOR_USER_FIXTURE");
  System.out.println("R64_COMPACT_REPORT_PASS|zipBytes="+zipFile.length()+"|zip="+zipFile+"|scope=PRODUCTION_JNI_AND_APK_ASSETS_HOST_REPLAY_NOT_DEVICE");
 }
}
