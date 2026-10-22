package com.ethk3.novaonepro;
import java.io.*;import java.util.*;import org.json.*;
/** Current expiry policy on actual filesystem/lease owners; synthetic published jobs. */
public final class ReportExpiryR992 {
 static int checks;static void need(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static String h(int i){return String.format(Locale.ROOT,"%064x",i);}
 static File job(File root,int id,long modified)throws Exception {
  File d=new File(root,"jobs/"+h(id));d.mkdirs();ReportJobJournalCP12.Job state=new ReportJobJournalCP12.Job(h(id),h(id+100),h(900),"FULL_REPORT",modified);state.stage="READY_TO_SHARE";state.publishedDir="archive-1";
  ReportJobJournalCP12.writeAtomic(new File(d,"job.json"),state.json().toString());ReportJobJournalCP12.writeAtomic(new File(d,"published.json"),"{}");
  File tmp=new File(d,"archive-1");tmp.mkdir();try(FileOutputStream out=new FileOutputStream(new File(tmp,"large-fixture.bin"))){out.write(1);}d.setLastModified(modified);return d;
 }
 public static void main(String[]args)throws Exception {
  File root=new File(args[0],"r981_reports");new File(root,"requests").mkdirs();long now=System.currentTimeMillis(),day=86400000L;
  File current=job(root,1,now-10*day),recent1=job(root,2,now-1000),recent2=job(root,3,now-2000),extra=job(root,4,now-3000),expired=job(root,5,now-8*day),open=job(root,6,now-9*day),shared=job(root,7,now-10*day);
  JSONObject grant=new JSONObject();grant.put("expiresWallMs",now+day);ReportJobJournalCP12.writeAtomic(new File(shared,"share-until.json"),grant.toString());shared.setLastModified(now-10*day);
  try(ReportPublicationCP12.Lease lease=ReportPublicationCP12.shared(open)){open.setLastModified(now-9*day);ReportPublicationCP12.pruneOldReports(root,current,now);
   need(new File(current,"published.json").exists(),"currentProtected");need(new File(recent1,"published.json").exists(),"recent1");need(new File(recent2,"published.json").exists(),"recent2");
   need(!new File(extra,"published.json").exists(),"extraRemoved");need(!new File(expired,"published.json").exists(),"expiredRemoved");need(new File(open,"published.json").exists(),"openProtected");need(new File(shared,"published.json").exists(),"shareLeaseProtected");}
  ReportPublicationCP12.pruneOldReports(root,current,now+2*day);need(!new File(open,"published.json").exists(),"closedOldRemoved");need(!new File(shared,"published.json").exists(),"expiredShareRemoved");
  String id=h(999);JSONObject request=new JSONObject();request.put("mode","FULL_REPORT");JSONObject ack=new JSONObject();ack.put("stage","FAILED");ack.put("jobId",h(888));
  ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+id+".json"),request.toString());File ackFile=new File(root,"requests/"+id+".ack.json");ReportJobJournalCP12.writeAtomic(ackFile,ack.toString());ackFile.setLastModified(now-8*day);
  for(String p:new String[]{"recovery/"+id,"saf-parts/"+id,"observations/"+id}){File d=new File(root,p);d.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(d,"trash"))){out.write(1);}}
  File failure=new File(root,"failures/"+id+".txt");failure.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(failure)){out.write(1);}
  ReportPublicationCP12.pruneRequestMetadata(root,now);need(!new File(root,"recovery/"+id).exists(),"recoveryRemoved");need(!new File(root,"saf-parts/"+id).exists(),"safResidueRemoved");need(!failure.exists(),"failureRemoved");need(!ackFile.exists(),"oldMetadataRemoved");
  System.out.println("PASS R992_EXPIRED_REPORT_AND_RESIDUE checks="+checks+" scope=Actual filesystem and lease owners; synthetic jobs; not Android provider");
 }
}
