package com.ethk3.novaonepro;
import java.io.*;import java.util.*;import org.json.*;
/** Exercises failure-before-request publication and old installations already above the former scan limit. */
public final class ReportMetadataRecoveryR993 {
 static int checks;static void need(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static String h(int n){return String.format(Locale.ROOT,"%064x",n);}
 static File failure(File root,int n,long at,boolean request)throws Exception{
  String id=h(n);if(request){JSONObject body=new JSONObject();body.put("mode","FULL_REPORT");ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+id+".json"),body.toString());new File(root,"requests/"+id+".json").setLastModified(at);}
  JSONObject ack=new JSONObject();ack.put("requestId",id);ack.put("stage","FAILED");
  File a=new File(root,"requests/"+id+".ack.json");ReportJobJournalCP12.writeAtomic(a,ack.toString());a.setLastModified(at);
  for(String path:new String[]{"observations/","recovery/","saf-parts/"}){File d=new File(root,path+id);d.mkdirs();try(FileOutputStream out=new FileOutputStream(new File(d,"fixture"))){out.write(1);}}
  File f=new File(root,"failures/"+id+".txt");f.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(f)){out.write(1);}return a;
 }
 public static void main(String[]args)throws Exception{
  File root=new File(args[0],"r981_reports");new File(root,"requests").mkdirs();long now=System.currentTimeMillis(),old=now-8L*86400000L;
  if(args[1].equals("orphan")){
   File expired=failure(root,1,old,false),fresh=failure(root,2,now,false),queued=failure(root,3,old,true);
   ReportRequestQueueCP12.enqueue(root,h(3),"FULL_REPORT");ReportPublicationCP12.pruneRequestMetadata(root,now);
   need(!expired.exists(),"orphanAckMustExpire");need(!new File(root,"recovery/"+h(1)).exists(),"orphanRecoveryMustExpire");need(!new File(root,"observations/"+h(1)).exists(),"orphanObservationsMustExpire");need(!new File(root,"failures/"+h(1)+".txt").exists(),"orphanStackMustExpire");need(fresh.exists(),"recentFailureRemainsReadable");need(queued.exists(),"queuedRequestProtected");need(h(3).equals(ReportRequestQueueCP12.head(root)),"queueIdentityPreserved");
  }else{
   for(int i=1;i<=520;i++)failure(root,i,old,true);
   need(new File(root,"requests").list().length>1024,"oldInstallationAboveFormerLimit");
   ReportPublicationCP12.pruneRequestMetadata(root,now);need(new File(root,"requests").list().length==0,"cleanupMustRunBeforeRejectingOldInstallation");
   need(new File(root,"observations").list().length==0,"allOldObservationsRemoved");need(new File(root,"recovery").list().length==0,"allOldRecoveryRemoved");
  }
  System.out.println("PASS REPORT_METADATA_RECOVERY mode="+args[1]+" checks="+checks+" scope=Actual filesystem/queue owners, synthetic failure fixtures, not device");
 }
}
