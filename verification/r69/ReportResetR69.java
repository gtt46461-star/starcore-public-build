package com.ethk3.novaonepro;
import java.io.*;import java.nio.file.*;import java.util.*;import java.lang.reflect.*;import java.util.concurrent.atomic.AtomicLong;import org.json.*;
/** Actual report control/queue/journal/publication owners on a real filesystem, not phone UI. */
public final class ReportResetR69 {
 static int checks;static String h(int n){return String.format(Locale.ROOT,"%064x",n);}
 static void need(boolean b,String why){if(!b)throw new AssertionError(why);checks++;System.out.println("PASS|"+why);}
 static void request(File root,int n,String mode)throws Exception{
  ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+h(n)+".json"),new JSONObject().put("requestId",h(n)).put("mode",mode).toString());ReportRequestQueueCP12.enqueue(root,h(n),mode);
 }
 static ReportJobJournalCP12.Job job(ReportJobJournalCP12 owner,int n,String stage)throws Exception{
  ReportJobJournalCP12.Job j=new ReportJobJournalCP12.Job(h(n),h(n+100),ReleaseManifestR926.MANIFEST_SHA256,"FULL_REPORT",System.currentTimeMillis());j.stage=stage;
  owner.create(j);owner.acknowledge(j.requestId,j);return j;
 }
 public static void main(String[] args)throws Exception{
  need(TrainingContinuationR973.compatible("6bf2b2699723abef499c8305eacb4079ad0bbb348864588551255faa6211b91c"),"R68_NUMERIC_LEARNERS_CONTINUE");
  File files=new File(args[0]);files.mkdirs();File root=new File(files,"r981_reports");
  File model=new File(files,"model-weights.bin"),draws=new File(files,"draws.db"),external=new File(files,"already-saved-user-report.zip");
  for(File f:new File[]{model,draws,external})Files.writeString(f.toPath(),"USER_DATA_SENTINEL");
  ReportJobJournalCP12 owner=ReportJobJournalCP12.tryClaim(files);
  try{
   need(owner!=null&&ReportJobJournalCP12.tryClaim(files)==null,"SINGLE_OWNER_LOCK_ENFORCED");
   ReportJobJournalCP12.Job old=job(owner,1,"READY_TO_SHARE");File oldDir=owner.directory(old),large=new File(oldDir,"old-650MiB.zip");
   try(RandomAccessFile f=new RandomAccessFile(large,"rw")){f.setLength(650L*1024*1024);}
   ReportPublicationCP12.grant(oldDir,System.currentTimeMillis());
   request(root,200,"RETENTION_PAGE");request(root,201,"FULL_REPORT");request(root,202,"SAF_TRANSFER");
   ReportJobJournalCP12.writeAtomic(new File(root,"saf/"+h(202)+".json"),new JSONObject().put("state","COPYING").put("allVolumesSaved",false).toString());
   String command=ReportTaskControlR69.request(files,true);
   need(ReportTaskControlR69.hasPending(root),"CONTROL_PUBLISHED_OUTSIDE_OLD_WORKER_QUEUE");
   need(ReportTaskControlR69.cancelled(root,old.requestId,old.jobId),"OLD_REPORT_GETS_DURABLE_STOP_SIGNAL");
   need(ReportTaskControlR69.cancelled(root,old.requestId,h(202)),"SAF_TRANSFER_GETS_STOP_SIGNAL");
   need(!ReportTaskControlR69.cancelled(root,h(200),h(200)),"RETENTION_REQUEST_NOT_CANCELLED");
   need(large.length()==650L*1024*1024,"NO_DELETION_BEFORE_OWNER_ACKNOWLEDGEMENT");
   request(root,203,"FULL_REPORT");
   try(ReportPublicationCP12.Lease reader=ReportPublicationCP12.shared(oldDir)){
    ReportTaskControlR69.applyPending(owner);
    need("WAITING_FOR_READERS".equals(ReportTaskControlR69.status(files,command).getString("stage")),"REAL_OPEN_READER_DEFERS_DELETION");
    need(large.exists(),"OPEN_READER_INPUT_PRESERVED");
    need(owner.current()==null,"OLD_CURRENT_POINTER_DETACHED");
   }
   ReportTaskControlR69.applyPending(owner);
   need("CONTROL_DONE".equals(ReportTaskControlR69.status(files,command).getString("stage")),"DELETE_COMPLETES_AFTER_READER_CLOSES");
   need(!oldDir.exists(),"650MIB_OLD_REPORT_DELETED_DESPITE_24H_SOFT_GRANT");
   need(!ReportTaskControlR69.hasPending(root),"COMPLETED_CONTROL_NOT_RESTARTED");
   Set<String> pending=ReportRequestQueueCP12.pendingIds(root);
   need(pending.size()==2&&pending.contains(h(200))&&pending.contains(h(203)),"EXACT_CANCEL_REMOVES_OLD_FULL_AND_SAF_ONLY");
   need(h(203).equals(ReportRequestQueueCP12.interactiveHead(root))&&ReportRequestQueueCP12.hasInteractive(root),"NEW_USER_REPORT_HAS_PRIORITY_OVER_RETENTION");
   need("CANCELLED".equals(new JSONObject(ReportJobJournalCP12.readAtomic(new File(root,"saf/"+h(202)+".json"),4096)).getString("state")),"OLD_SAF_RETRY_IS_TERMINAL");
   ReportJobJournalCP12.Job fresh=job(owner,3,"BACKUP");ReportRequestQueueCP12.remove(root,h(203));
   ReportTaskControlR69.applyPending(owner);need(owner.current().jobId.equals(fresh.jobId),"NEW_REPORT_CREATES_AFTER_RESET_AND_SURVIVES_OLD_CONTROL_REPLAY");
   need(!ReportRequestQueueCP12.hasInteractive(root)&&h(200).equals(ReportRequestQueueCP12.head(root)),"RETENTION_STAYS_DURABLE_FOR_LATER_RESUME");
   ReportJobJournalCP12.Job finished=job(owner,4,"READY_TO_SHARE");File finishedDir=owner.directory(finished);Files.writeString(new File(finishedDir,"complete.zip").toPath(),"keep");
   String stop=ReportTaskControlR69.request(files,false);ReportTaskControlR69.applyPending(owner);
   need("CONTROL_DONE".equals(ReportTaskControlR69.status(files,stop).getString("stage")),"CANCEL_ONLY_COMPLETES");
   need("CANCELLED".equals(ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(root,"jobs/"+fresh.jobId+"/job.json"),65536)).stage),"ACTIVE_BACKUP_CANCELLED");
   need("READY_TO_SHARE".equals(owner.current().stage)&&new File(finishedDir,"complete.zip").exists(),"CANCEL_ONLY_DOES_NOT_DELETE_COMPLETED_REPORT");
   for(int i=0;i<3;i++)job(owner,20+i,new String[]{"INDEXING","VERIFYING","PAUSED"}[i]);
   String clear=ReportTaskControlR69.request(files,true);ReportTaskControlR69.applyPending(owner);
   need("CONTROL_DONE".equals(ReportTaskControlR69.status(files,clear).getString("stage"))&&new File(root,"jobs").listFiles().length==0,"ALL_OLD_PHASES_CLEAR_WITHOUT_REEXPORTING_OR_HASHING_LARGE_FILES");
   File detached=new File(root,"deleted-reports-r69/"+h(90));detached.mkdirs();Files.writeString(new File(detached,"interrupted-delete.bin").toPath(),"trash");
   ReportTaskControlR69.applyPending(owner);need(!detached.exists(),"CRASH_INTERRUPTED_DELETION_RESUMES");
   for(File f:new File[]{model,draws,external})need("USER_DATA_SENTINEL".equals(Files.readString(f.toPath())),"PRESERVED_"+f.getName());
   // Exercise the actual in-memory request generation fence, without constructing an Android Service.
   Field generation=ReportExportServiceCP4.class.getDeclaredField("USER_REQUEST_GENERATION");generation.setAccessible(true);AtomicLong g=(AtomicLong)generation.get(null);
   Method enqueue=ReportExportServiceCP4.class.getDeclaredMethod("enqueueUserRequest",File.class,String.class,String.class,JSONObject.class,long.class);enqueue.setAccessible(true);
   long prior=g.getAndIncrement();boolean rejected=false;
   try{enqueue.invoke(null,root,h(300),"FULL_REPORT",new JSONObject().put("requestId",h(300)),prior);}catch(InvocationTargetException e){rejected=e.getCause() instanceof IOException&&e.getCause().getMessage().equals("REPORT_SUPERSEDED_BY_USER");}
   need(rejected&&!ReportRequestQueueCP12.pendingIds(root).contains(h(300)),"PRE_RESET_IN_MEMORY_REQUEST_CANNOT_REAPPEAR");
   enqueue.invoke(null,root,h(301),"FULL_REPORT",new JSONObject().put("requestId",h(301)),g.get());
   need(ReportRequestQueueCP12.pendingIds(root).contains(h(301)),"POST_RESET_REQUEST_IS_ADMITTED");
  }finally{owner.close();}
  boolean closed=false;try{ReportTaskControlR69.applyPending(owner);}catch(IOException expected){closed=true;}need(closed,"DELETION_WITHOUT_OWNER_LEASE_REJECTED");
  System.out.println("R69_REPORT_CONTROL_PASS|checks="+checks+"|HOST_SYNTHETIC_NOT_PHONE");
 }
}
