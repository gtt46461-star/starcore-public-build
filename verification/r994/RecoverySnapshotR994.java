package com.ethk3.novaonepro;
import java.io.*;import java.nio.file.*;import java.util.zip.*;import org.json.*;
public final class RecoverySnapshotR994 {
 static void need(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[]args)throws Exception {
  File root=new File(args[0]);root.mkdirs();String id=String.format("%064x",994),jid=String.format("%064x",1994);
  File snapshot=new File(root,"jobs/"+jid+"/snapshot.db");snapshot.getParentFile().mkdirs();Files.copy(Path.of(args[1]),snapshot.toPath());
  ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(jid,id,String.format("%064x",1),"FULL_REPORT",1);
  job.snapshotSha=ReportArchiveCP4.hash(snapshot,()->true);job.snapshotBytes=snapshot.length();
  JSONObject receipt=ReportJobJournalCP12.writeFailureArtifact(root,id,new IOException("fixture-original-error"),job);
  need(receipt.getBoolean("databaseIncluded"));
  try(ZipFile zip=new ZipFile(new File(root,"recovery/"+id+"/"+receipt.getString("sha256")+".zip"))) {
   need(zip.getEntry("snapshot.db").getSize()==snapshot.length());JSONObject boundary=new JSONObject(new String(zip.getInputStream(zip.getEntry("EVIDENCE_BOUNDARY.json")).readAllBytes(),"UTF-8"));
   need(boundary.getBoolean("databaseIncluded")&&!boundary.getBoolean("checksComplete")&&!boundary.getBoolean("allChecksPassed"));
  }
  Files.writeString(snapshot.toPath(),"changed");receipt=ReportJobJournalCP12.writeFailureArtifact(root,id,new IOException("different-error"),job);need(!receipt.getBoolean("databaseIncluded"));
  job.requestId.toString();boolean rejected=false;try{ReportJobJournalCP12.writeFailureArtifact(root,String.format("%064x",2),new IOException(),job);}catch(IOException expected){rejected=true;}need(rejected);
  System.out.println("PASS|verified snapshot retained; altered snapshot excluded; request identity enforced; failed health retained");
 }
}
