package com.ethk3.novaonepro;
import java.io.*;
/** Synthetic durable-journal fixture: retention must not hide a complete or failed full report. */
public final class ReportRecoveryLookupR982 {
 static int checks;static void ok(boolean v){checks++;if(!v)throw new AssertionError("lookup:"+checks);}
 static String h(String s){return V51ChecksumEngine.sha256("SYNTHETIC_LOOKUP:"+s);}
 public static void main(String[] args)throws Exception{
  File files=new File(args[0]);files.mkdirs();File root=new File(files,"r981_reports");
  try(ReportJobJournalCP12 owner=ReportJobJournalCP12.tryClaim(files)){
   ok(ReportJobJournalCP12.latestFullReport(root)==null);
   ReportJobJournalCP12.Job first=new ReportJobJournalCP12.Job(h("full"),h("request1"),h("release"),"FULL_REPORT",100);
   owner.create(first);owner.transition(first,ReportJobJournalCP12.Stage.FAILED,110);
   ReportJobJournalCP12.Job retention=new ReportJobJournalCP12.Job(h("retention"),h("request2"),h("release"),"RETENTION_PAGE",200);owner.create(retention);
   ok(owner.current().jobId.equals(retention.jobId));
   ReportJobJournalCP12.Job recovered=ReportJobJournalCP12.latestFullReport(root);ok(recovered.jobId.equals(first.jobId));ok(recovered.stage.equals("FAILED"));
   ReportJobJournalCP12.Job latest=new ReportJobJournalCP12.Job(h("latest"),h("request3"),h("release"),"FULL_REPORT",300);owner.create(latest);
   ok(ReportJobJournalCP12.latestFullReport(root).jobId.equals(latest.jobId));
  }
  try(ReportJobJournalCP12 restart=ReportJobJournalCP12.tryClaim(files)){ok(restart.current().jobId.equals(h("latest")));ok(ReportJobJournalCP12.latestFullReport(root).jobId.equals(h("latest")));}
  File bad=new File(root,"jobs/"+h("wrong"));bad.mkdirs();ReportJobJournalCP12.Job mismatch=new ReportJobJournalCP12.Job(h("different"),h("request4"),h("release"),"FULL_REPORT",400);ReportJobJournalCP12.writeAtomic(new File(bad,"job.json"),mismatch.json().toString());
  boolean rejected=false;try{ReportJobJournalCP12.latestFullReport(root);}catch(IOException e){rejected=e.getMessage().equals("REPORT_RECOVERY_JOB_IDENTITY");}ok(rejected);
  System.out.println("PASS|assertions="+checks+"|scope=synthetic durable full-report lookup; retention separation, restart, identity rejection;DEVICE_NOT_VERIFIED");
 }
}
