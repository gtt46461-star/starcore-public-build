package com.ethk3.novaonepro;
import java.io.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.*;import org.json.*;
import android.net.Uri;import android.content.ContentResolver;
/** Actual Service/queue/publication/copy owners; isolated host provider port only. */
public final class ReportStorageFailureR991 {
 static int checks;static void need(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static String hash(int i){return String.format(Locale.ROOT,"%064x",i);}
 static final class Provider extends ContentResolver {
  final File root;final String fault;int outputOpens,inputOpens,creates;final Map<String,File> docs=new HashMap<>();
  Provider(File r,String f){root=r;root.mkdirs();fault=f;}
  public Uri createForHost(Uri parent,String mime,String name)throws IOException{Uri u=Uri.parse("content://isolated/document/"+(++creates));File f=new File(root,name);if(mime.equals("vnd.android.document/directory"))f.mkdirs();docs.put(u.toString(),f);return u;}
  IOException failure(){return new IOException("provider write failed",new android.system.ErrnoException("openDocument",fault.equals("transient")?android.system.OsConstants.EIO:android.system.OsConstants.ENOSPC));}
  public OutputStream openOutputStream(Uri u,String mode)throws IOException{outputOpens++;need("wt".equals(mode),"real product truncating-provider mode");if(fault.equals("storage")||fault.equals("transient")||(fault.equals("late")&&outputOpens==2))throw failure();File f=docs.get(u.toString());if(f==null){f=new File(root,"single.bin");docs.put(u.toString(),f);}final OutputStream real=new FileOutputStream(f,false);return new FilterOutputStream(real){public void write(byte[] b,int off,int len)throws IOException{if(fault.equals("write"))throw failure();out.write(b,off,len);}public void close()throws IOException{super.close();if(fault.equals("close"))throw failure();}};}
  public InputStream openInputStream(Uri u)throws IOException{inputOpens++;return new FileInputStream(docs.get(u.toString()));}
 }
 static final class ServiceHarness {
  final File files;final Provider provider;final android.app.AlarmManager alarms=new android.app.AlarmManager();
  final ReportExportServiceCP4 instance; ServiceHarness(File f,String fault){files=f;files.mkdirs();provider=new Provider(new File(files,"provider"),fault);android.content.Context.HOST_FILES=files;android.content.Context.HOST_RESOLVER=provider;android.content.Context.HOST_ALARMS=alarms;instance=new ReportExportServiceCP4();}
  public File getFilesDir(){return files;}public ContentResolver getContentResolver(){return provider;}
  public Object getSystemService(String name){return name.equals("alarm")?alarms:new android.app.NotificationManager();}
  public String getPackageName(){return "com.ethk3.novaonepro";}
 }
 static File publication(File files,String id)throws Exception {
  File job=new File(files,"r981_reports/jobs/"+id),archive=new File(job,"archive-1");archive.mkdirs();
  Files.write(new File(archive,"manifest.jsonl").toPath(),"ISOLATED_SYNTHETIC_R991_PUBLICATION_TRANSPORT\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
  Files.write(new File(archive,"P000001.bin").toPath(),new byte[]{11,22,33,44});String sha=ReportArchiveCP4.hash(new File(archive,"manifest.jsonl"),()->true);
  ReportJobJournalCP12.Job state=new ReportJobJournalCP12.Job(id,hash(1001),ReleaseManifestR926.MANIFEST_SHA256,"FULL_REPORT",1);state.stage="READY_TO_SHARE";state.publishedDir="archive-1";state.manifestSha=sha;state.evidenceComplete=true;
  ReportJobJournalCP12.writeAtomic(new File(job,"job.json"),state.json().toString());ReportPublicationCP12.prepare(job,archive,id,sha,true,false,false,()->true);
  JSONObject pointer=new JSONObject();pointer.put("deliveryIndexSha256",ReportArchiveCP4.hash(new File(job,"delivery/index.json"),()->true));ReportJobJournalCP12.writeAtomic(new File(job,"published.json"),pointer.toString());return job;
 }
 static void invoke(ServiceHarness service,JSONObject request,File root)throws Exception {
  Method method=ReportExportServiceCP4.class.getDeclaredMethod("runSafRequest",JSONObject.class,File.class);method.setAccessible(true);
  try{method.invoke(service.instance,request,root);}catch(InvocationTargetException failure){throw (Exception)failure.getCause();}
 }
 public static void main(String[]args)throws Exception {
  File base=new File(args[0]);base.mkdirs();String mode=args.length>1?args[1]:"storage";boolean normal=mode.equals("normal"),transientIo=mode.equals("transient"),late=mode.equals("late");
  for(boolean tree:late?new boolean[]{true}:new boolean[]{false,true}){
   File files=new File(base,tree?"tree":"single");ServiceHarness service=new ServiceHarness(files,mode);String id=hash(tree?9912:9911),transfer=hash(tree?9922:9921),unrelated=hash(9990);File job=publication(files,id),root=new File(files,"r981_reports");
   JSONObject req=new JSONObject();req.put("requestId",transfer);req.put("mode","SAF_TRANSFER");req.put("jobId",id);req.put("volumeName",tree?"*":"P000001.bin");req.put("destination",tree?"content://isolated/tree/target":"content://isolated/document/single");req.put("state","PENDING");req.put("attempt",0);
   ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+transfer+".json"),req.toString());ReportRequestQueueCP12.enqueue(root,transfer,"SAF_TRANSFER");ReportRequestQueueCP12.enqueue(root,unrelated,"FULL_REPORT");
   invoke(service,req,root);JSONObject persisted=new JSONObject(ReportJobJournalCP12.readAtomic(new File(root,"saf/"+transfer+".json"),65536)),ack=new JSONObject(ReportJobJournalCP12.readAtomic(new File(root,"requests/"+transfer+".ack.json"),65536));
   if(normal){need(persisted.getString("state").equals("VERIFIED_READBACK"),"valid real Service final readback state");need(service.provider.outputOpens>0&&service.provider.inputOpens>0,"actual writer and readback consumers");if(tree)need(ack.getBoolean("allVolumesSaved"),"every actual delivery piece and final index verified");}
   else if(transientIo){need(persisted.getString("state").equals("PAUSED"),"ordinary provider EIO retains retry");need(persisted.getString("reason").equals("IOException"),"EIO is not classified as storage full");need(service.alarms.schedules==1,"actual resume scheduler invoked once");need(transfer.equals(ReportRequestQueueCP12.head(root)),"retry retains exact queue head");need(!ack.optBoolean("allVolumesSaved")&&!ack.optBoolean("localReceiptDurable"),"retry has no success receipt");continue;}
   else {need(persisted.getString("state").equals("FAILED"),"STORAGE_FULL_MUST_NOT_PAUSE_AUTOMATIC_RETRY:"+persisted);need(persisted.getString("reason").equals("REPORT_STORAGE_FULL"),"typed provider errno preserved as exact reason");need(persisted.getString("failureDetail").contains("ENOSPC"),"original typed provider cause retained");need(persisted.getString("recovery").equals("FREE_DESTINATION_SPACE_AND_START_NEW_SAF_TRANSFER"),"actual missing-condition recovery path");need(!ack.optBoolean("allVolumesSaved")&&!ack.optBoolean("localReceiptDurable"),"no false success receipt");need(service.provider.outputOpens==(late?2:1)&&service.provider.inputOpens==(late?1:0),"failed provider cannot read back failed volume");if(late){need(persisted.getInt("verifiedVolumes")==1,"prior verified volume retained accurately");need(!new File(service.provider.root,"index.json").exists(),"partial set cannot publish final index");}}
   need(ack.getString("state").equals(persisted.getString("state")),"actual durable ack consumed exact final state");need(service.alarms.schedules==0,"terminal result schedules no automatic resume");need(unrelated.equals(ReportRequestQueueCP12.head(root)),"real queue consumer removes only exact completed request");String source=ReportArchiveCP4.hash(new File(job,"delivery/index.json"),()->true);invoke(service,req,root);need(source.equals(ReportArchiveCP4.hash(new File(job,"delivery/index.json"),()->true)),"source publication untouched by retry");need(service.provider.outputOpens==(normal?(tree?3:1):(late?2:1)),"terminal replay does not recopy or create more provider data");
  }
  System.out.println("PASS ACTUAL_REPORT_SERVICE_STORAGE_OWNER checks="+checks+" mode="+mode+" SYNTHETIC_HOST_PROVIDER DEVICE_NOT_VERIFIED");
 }
}
