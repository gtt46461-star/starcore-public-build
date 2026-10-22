package com.ethk3.novaonepro;
import java.io.*;import java.util.*;import java.lang.reflect.*;import android.content.Context;import org.json.*;
public final class ReportCancellationIsolationR987 {
 static int assertions;static void need(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why+" assertion="+assertions);}
 static String hash(int n){return String.format(Locale.ROOT,"%064x",n);}
 static boolean cancelled(File root,String id)throws Exception{
  try{return (Boolean)ReportJobJournalCP12.class.getMethod("isCancellationRequested",File.class,String.class).invoke(null,root,id);}
  catch(NoSuchMethodException before){File command=new File(root,"cancel.json");if(!command.exists())return false;JSONObject row=new JSONObject(ReportJobJournalCP12.readAtomic(command,4096));return id.equals(row.optString("jobId"))&&row.optBoolean("cancel");}
  catch(InvocationTargetException rejected){throw (Exception)rejected.getCause();}
 }
 static void reject(RunnableThrow work,String why)throws Exception{assertions++;try{work.run();throw new AssertionError(why);}catch(IOException expected){}}
 interface RunnableThrow{void run()throws Exception;}
 public static void main(String[]args)throws Exception{
  File files=new File(args[0]);files.mkdirs();File root=new File(files,"r981_reports");Context context=new Context(files);
  String a=hash(1),b=hash(2),transfer=hash(3),other=hash(4);
  if(args.length>1){if(args[1].startsWith("write-")){String key=args[1].equals("write-a")?a:b;for(int i=0;i<8;i++)ReportJobJournalCP12.requestCancellation(root,key);}need(cancelled(root,a)&&cancelled(root,b)&&cancelled(root,transfer),"independent fresh JVM consumes all exact durable commands");System.out.println("PASS FRESH_JVM_CANCEL_RECOVERY "+args[1]);return;}
  try(ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(files)){
   journal.create(new ReportJobJournalCP12.Job(a,hash(101),hash(900),"FULL_REPORT",10));
   journal.create(new ReportJobJournalCP12.Job(b,hash(102),hash(900),"FULL_REPORT",20));
  }
  JSONObject request=new JSONObject();request.put("requestId",transfer);request.put("mode","SAF_TRANSFER");ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+transfer+".json"),request.toString());
  ReportExportServiceCP4.requestCancel(context,a);need(context.starts.size()==1,"first actual Service kick");need(cancelled(root,a),"first cancellation durable");
  ReportExportServiceCP4.requestCancel(context,b);need(context.starts.size()==2,"second actual Service kick");need(cancelled(root,b),"second cancellation durable");
  need(cancelled(root,a),"later cancellation must not erase earlier report cancellation");
  ReportExportServiceCP4.requestCancel(context,transfer);need(cancelled(root,transfer),"SAF transfer cancellation consumed");need(cancelled(root,a)&&cancelled(root,b),"SAF cannot erase report commands");need(!cancelled(root,other),"unrelated task unaffected");
  ReportExportServiceCP4.requestCancel(context,a);need(cancelled(root,a)&&cancelled(root,b)&&cancelled(root,transfer),"repeat cancellation idempotent");
  need(context.starts.get(0).getStringExtra("id").equals(a)&&context.starts.get(0).getAction().equals(ReportExportServiceCP4.ACTION_CANCEL),"kick exact original target");
  reject(()->ReportExportServiceCP4.requestCancel(context,other),"unknown task cannot create a cancellation tombstone");
  need(!cancelled(root,other),"unknown task remains unaffected");
  JSONObject legacy=new JSONObject();legacy.put("jobId",other);legacy.put("cancel",true);ReportJobJournalCP12.writeAtomic(new File(root,"cancel.json"),legacy.toString());
  need(cancelled(root,other),"previous version legacy command still consumed");need(cancelled(root,a)&&cancelled(root,b),"legacy command cannot erase new durable commands");
  String java=new File(System.getProperty("java.home"),"bin/java").getPath();Process pa=new ProcessBuilder(java,"-Xmx32m","-cp",System.getProperty("java.class.path"),ReportCancellationIsolationR987.class.getName(),files.getPath(),"write-a").inheritIO().start();Process pb=new ProcessBuilder(java,"-Xmx32m","-cp",System.getProperty("java.class.path"),ReportCancellationIsolationR987.class.getName(),files.getPath(),"write-b").inheritIO().start();need(pa.waitFor()==0&&pb.waitFor()==0,"concurrent process writers preserve all independent commands");
  Process restart=new ProcessBuilder(java,"-Xmx32m","-cp",System.getProperty("java.class.path"),ReportCancellationIsolationR987.class.getName(),files.getPath(),"restart").inheritIO().start();need(restart.waitFor()==0,"fresh process durable recovery");
  JSONObject corrupt=new JSONObject();corrupt.put("jobId",b);corrupt.put("cancel",true);ReportJobJournalCP12.writeAtomic(new File(root,"cancel-commands/"+a+".json"),corrupt.toString());reject(()->cancelled(root,a),"mismatched cancellation identity fails closed");
  ReportJobJournalCP12.writeAtomic(new File(root,"cancel-commands/"+a+".json"),"not-json");reject(()->cancelled(root,a),"malformed command has typed failure and cannot claim cancellation");
  ReportExportServiceCP4.requestCancel(context,a);need(cancelled(root,a),"explicit original Owner retry restores exact identity");
  ReportJobJournalCP12.removeCancellation(root,a);need(!cancelled(root,a)&&cancelled(root,b)&&cancelled(root,transfer),"terminal metadata cleanup removes only exact command");
  ReportExportServiceCP4.requestCancel(context,a);JSONObject full=new JSONObject();full.put("requestId",hash(101));full.put("mode","FULL_REPORT");ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+hash(101)+".json"),full.toString());
  try(ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(files)){ReportJobJournalCP12.Job done=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(root,"jobs/"+a+"/job.json"),65536));journal.transition(done,ReportJobJournalCP12.Stage.CANCELLED,30);journal.acknowledge(done.requestId,done);}
  JSONObject saved=new JSONObject();saved.put("requestId",transfer);saved.put("state","VERIFIED_READBACK");ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+transfer+".ack.json"),saved.toString());
  ReportPublicationCP12.pruneRequestMetadata(root,System.currentTimeMillis()+8L*24*60*60*1000);
  need(!cancelled(root,a)&&!cancelled(root,transfer)&&cancelled(root,b),"actual publication metadata consumer cleans exact terminal job and transfer commands");need(!new File(root,"requests/"+hash(101)+".ack.json").exists()&&!new File(root,"requests/"+transfer+".ack.json").exists(),"actual expired acknowledgments removed");
  try(ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(files)){ReportJobJournalCP12.Job retention=new ReportJobJournalCP12.Job(hash(5),hash(105),hash(900),"RETENTION_PAGE",40);journal.create(retention);ReportExportServiceCP4.requestCancel(context,retention.jobId);journal.transition(retention,ReportJobJournalCP12.Stage.CANCELLED,41);journal.create(new ReportJobJournalCP12.Job(hash(6),hash(106),hash(900),"FULL_REPORT",50));}
  ReportPublicationCP12.pruneTerminal(root,hash(6));need(!cancelled(root,hash(5))&&cancelled(root,b),"terminal retention pruning cannot accumulate orphan cancellation commands");need(!new File(root,"jobs/"+hash(5)).exists(),"actual terminal retention directory removed");
  System.out.println("PASS REAL_SERVICE_CANCEL_DURABLE_ISOLATION assertions="+assertions+" scope=SYNTHETIC_HOST_TRANSPORT DEVICE_NOT_VERIFIED");
 }
}
