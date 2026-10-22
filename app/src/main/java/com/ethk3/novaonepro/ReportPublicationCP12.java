package com.ethk3.novaonepro;
import java.io.*;import java.nio.channels.*;import java.util.*;import org.json.*;
/** Immutable published archives plus delivery pieces. No database, BET, settlement or model write authority. */
public final class ReportPublicationCP12 {
 private static final long MiB=1024L*1024L;private static final Map<String,Shared> OPEN=new HashMap<>();
 private ReportPublicationCP12(){}
 private static final class Shared{final RandomAccessFile io;final FileLock lock;int refs;Shared(RandomAccessFile io,FileLock lock){this.io=io;this.lock=lock;refs=1;}}
 public static final class Lease implements Closeable{final String key;boolean closed;Lease(String key){this.key=key;}public void close()throws IOException{synchronized(OPEN){if(closed)return;closed=true;Shared state=OPEN.get(key);if(--state.refs==0){OPEN.remove(key);try{state.lock.release();}finally{state.io.close();}}}}}
 public static Lease shared(File job)throws IOException{String key=ReportArchiveVerifierCP12.inside(job,"share.lock").getPath();synchronized(OPEN){Shared state=OPEN.get(key);if(state!=null){state.refs++;return new Lease(key);}if(OPEN.size()>=8)throw new IOException("REPORT_SHARE_LEASE_LIMIT");RandomAccessFile io=new RandomAccessFile(key,"rw");try{FileLock lock=io.getChannel().tryLock(0,Long.MAX_VALUE,true);if(lock==null)throw new IOException("REPORT_SHARE_BUSY");OPEN.put(key,new Shared(io,lock));return new Lease(key);}catch(Exception bad){io.close();if(bad instanceof IOException)throw(IOException)bad;throw new IOException("REPORT_SHARE_BUSY",bad);}}}
 public static JSONObject delivery(File job)throws Exception{File file=ReportArchiveVerifierCP12.inside(job,"delivery/index.json");JSONObject index=new JSONObject(ReportJobJournalCP12.readAtomic(file,65536));ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));if(state.stage.equals("READY_TO_SHARE")){JSONObject pointer=new JSONObject(ReportJobJournalCP12.readAtomic(new File(job,"published.json"),4096));if(!state.jobId.equals(index.getString("jobId"))||!state.manifestSha.equals(index.getString("manifestSha256"))||state.evidenceComplete!=index.getBoolean("evidenceComplete")||state.checksComplete!=index.getBoolean("checksComplete")||state.allChecksPassed!=index.getBoolean("allChecksPassed")||!pointer.getString("deliveryIndexSha256").equals(ReportArchiveCP4.hash(file,()->true)))throw new IOException("REPORT_DELIVERY_INDEX_INTEGRITY");}return index;}
 public static File job(File files,String id)throws Exception{if(id==null||!id.matches("[0-9a-f]{64}"))throw new IOException("REPORT_PUBLISHED_ID");File directory=ReportArchiveVerifierCP12.inside(new File(files,"r981_reports"),"jobs/"+id);ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(directory,"job.json"),65536));if(!state.stage.equals("READY_TO_SHARE")||!state.jobId.equals(id))throw new IOException("REPORT_NOT_PUBLISHED");return directory;}
 /** Every exported item, including a large manifest segment, is <=8MiB. Full manifest stays native-verifiable in archive/. */
 public static long prepare(File job,File archive,String id,String manifestHash,boolean complete,boolean checks,boolean passed,ReportNativeCP4.Control control)throws Exception{
  File delivery=new File(job,"delivery");if(!delivery.isDirectory()&&!delivery.mkdir())throw new IOException("REPORT_DELIVERY_DIRECTORY");JSONObject index=new JSONObject();index.put("format","SCREPORT_DELIVERY_CP12_1");index.put("jobId",id);index.put("manifestSha256",manifestHash);index.put("evidenceComplete",complete);index.put("checksComplete",checks);index.put("allChecksPassed",passed);JSONArray files=new JSONArray();long total=0;
  File manifest=new File(archive,"manifest.jsonl");if(!manifestHash.equals(ReportArchiveCP4.hash(manifest,control)))throw new IOException("REPORT_DELIVERY_MANIFEST_CHANGED");
  try(InputStream in=new FileInputStream(manifest)){byte[] buffer=new byte[32768];int number=0;while(true){int first=in.read(buffer);if(first<0)break;File segment=new File(delivery,String.format(Locale.ROOT,"M%06d.manifest",++number));try(FileOutputStream out=new FileOutputStream(segment)){out.write(buffer,0,first);long bytes=first;while(bytes<ReportArchiveCP4.PART_BYTES){if(!control.shouldContinue())throw new InterruptedIOException("REPORT_DELIVERY_CANCELLED");int n=in.read(buffer,0,(int)Math.min(buffer.length,ReportArchiveCP4.PART_BYTES-bytes));if(n<0)break;out.write(buffer,0,n);bytes+=n;}out.flush();out.getFD().sync();}total=Math.addExact(total,segment.length());item(files,segment.getName(),"delivery/"+segment.getName(),segment,control,"MANIFEST_SEGMENT");}}
  int part=0;while(true){File file=new File(archive,String.format(Locale.ROOT,"P%06d.bin",++part));if(!file.exists())break;item(files,file.getName(),archive.getName()+"/"+file.getName(),file,control,"PART");total=Math.addExact(total,file.length());}
  if(files.length()==0||files.length()>128)throw new IOException("REPORT_DELIVERY_FILES_LIMIT");index.put("files",files);index.put("manifestBytes",manifest.length());index.put("totalVolumeBytes",total);ReportJobJournalCP12.writeAtomic(new File(delivery,"index.json"),index.toString());return Math.addExact(total,new File(delivery,"index.json").length());
 }
 private static void item(JSONArray rows,String name,String path,File file,ReportNativeCP4.Control control,String kind)throws Exception{if(file.length()<1||file.length()>ReportArchiveCP4.PART_BYTES)throw new IOException("REPORT_DELIVERY_VOLUME_SIZE");JSONObject row=new JSONObject();row.put("name",name);row.put("path",path);row.put("bytes",file.length());row.put("sha256",ReportArchiveCP4.hash(file,control));row.put("kind",kind);rows.put(row);}
 /** Current delivery is one standard ZIP. The complete size is limited only by available storage. */
 public static long prepareSingleZip(File job,File archive,String id,String manifestHash,boolean complete,boolean checks,boolean passed,ReportNativeCP4.Control control)throws Exception{
  if(!manifestHash.equals(ReportArchiveCP4.hash(new File(archive,"manifest.jsonl"),control)))throw new IOException("REPORT_DELIVERY_MANIFEST_CHANGED");
  File delivery=new File(job,"delivery");if(!delivery.isDirectory()&&!delivery.mkdir())throw new IOException("REPORT_DELIVERY_DIRECTORY");
  File zip=new File(delivery,"STARCORE_FULL_REPORT.zip");ReportArchiveVerifierCP12.writeZip(archive,zip,ReportArchiveVerifierCP12.nativeDecoder(4*MiB),control);
  JSONObject index=new JSONObject();index.put("format","SCREPORT_SINGLE_ZIP_R992");index.put("jobId",id);index.put("manifestSha256",manifestHash);index.put("evidenceComplete",complete);index.put("checksComplete",checks);index.put("allChecksPassed",passed);
  JSONObject row=new JSONObject();row.put("name",zip.getName());row.put("path","delivery/"+zip.getName());row.put("bytes",zip.length());row.put("sha256",ReportArchiveCP4.hash(zip,control));row.put("kind","SINGLE_ZIP");JSONArray files=new JSONArray();files.put(row);index.put("files",files);index.put("totalVolumeBytes",zip.length());
  ReportJobJournalCP12.writeAtomic(new File(delivery,"index.json"),index.toString());return zip.length();
 }
 /** Delete old published reports, preserving the current report and open/shared exports. */
 public static void pruneOldReports(File root,File current,long now)throws Exception{
  File[] directories=new File(root,"jobs").listFiles();if(directories==null)return;
  List<File> published=new ArrayList<>();for(File d:directories)if(!d.equals(current)&&new File(d,"published.json").isFile())published.add(d);
  published.sort((a,b)->Long.compare(b.lastModified(),a.lastModified()));
  for(int i=0;i<published.size();i++){File d=published.get(i);if(i>=2||now-d.lastModified()>7*24*60*60*1000L)deletePublished(d,now);}
 }
 public static void grant(File job,long now)throws Exception{try(Lease pin=shared(job)){ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));if(!state.stage.equals("READY_TO_SHARE"))throw new IOException("REPORT_SHARE_NOT_READY");JSONObject lease=new JSONObject();lease.put("expiresWallMs",Math.addExact(now,24*60*60*1000L));ReportJobJournalCP12.writeAtomic(new File(job,"share-until.json"),lease.toString());}}
 /** Explicit user deletion: ignore the soft 24-hour grant, honor actual open reader locks.
  * Caller holds the report-owner lock and has detached the current-job pointer. No file hashing. */
 public static boolean discardByUser(File job)throws Exception {
  if(!job.exists())return true;
  File lockFile=ReportArchiveVerifierCP12.inside(job,"share.lock");
  synchronized(OPEN){if(OPEN.containsKey(lockFile.getCanonicalPath()))return false;}
  try(RandomAccessFile io=new RandomAccessFile(lockFile,"rw")){
   FileLock lock;try{lock=io.getChannel().tryLock();}catch(OverlappingFileLockException held){return false;}
   if(lock==null)return false;
   try{
    File trash=ReportArchiveVerifierCP12.inside(job.getParentFile().getParentFile(),"deleted-reports-r69");
    if(!trash.isDirectory()&&!trash.mkdir())throw new IOException("REPORT_USER_DELETE_DIRECTORY");
    File detached=ReportArchiveVerifierCP12.inside(trash,job.getName());
    if(detached.exists()||!job.renameTo(detached))throw new IOException("REPORT_USER_DELETE_DETACH");
    cleanChildren(detached,Collections.<String>emptySet());if(!detached.delete())throw new IOException("REPORT_USER_DELETE_FAILED");
   }finally{lock.release();}
  }
  return true;
 }
 /** Resume an interrupted deletion from a directory that only explicit user deletion can create. */
 public static void finishUserDeletions(File root)throws IOException {
  File trash=ReportArchiveVerifierCP12.inside(root,"deleted-reports-r69");File[] dirs=trash.listFiles();if(dirs==null)return;
  for(File directory:dirs){if(!directory.getName().matches("[0-9a-f]{64}")||!directory.getAbsoluteFile().equals(directory.getCanonicalFile()))throw new IOException("REPORT_USER_DELETE_PATH");
   cleanChildren(directory,Collections.<String>emptySet());if(!directory.delete())throw new IOException("REPORT_USER_DELETE_FAILED");}
 }
 /** Publication owner calls before READY. Single writer lock is already held. Eight reports and aggregate Q are hard limits. */
 public static void enforceQuota(File root,File current,long quota)throws Exception{
  File jobs=new File(root,"jobs");File[] dirs=jobs.listFiles();if(dirs==null)throw new IOException("REPORT_QUOTA_LIST");if(dirs.length>48)throw new IOException("REPORT_JOB_DIRECTORY_LIMIT");long total=0;List<File> published=new ArrayList<>();for(File d:dirs){if(!d.getName().matches("[0-9a-f]{64}")||!d.isDirectory())throw new IOException("REPORT_QUOTA_JOB_PATH");File ptr=new File(d,"published.json");if(ptr.isFile()||d.equals(current)){published.add(d);total=Math.addExact(total,publishedBytes(d));}}
  Collections.sort(published,(a,b)->Long.compare(a.lastModified(),b.lastModified()));for(File d:published){if(total<=quota&&published.size()<=8)break;if(d.equals(current))continue;long bytes=publishedBytes(d);if(deletePublished(d,System.currentTimeMillis())){total-=bytes;published.remove(d);break;}}
  // Repeat using a fresh bounded scan; removal changes neither caller's archive nor another held shared lease.
  if(total>quota||published.size()>8){boolean progress=false;for(File d:new ArrayList<File>(published)){if(d.equals(current))continue;long bytes=publishedBytes(d);if(deletePublished(d,System.currentTimeMillis())){total-=bytes;published.remove(d);progress=true;if(total<=quota&&published.size()<=8)break;}}if(total>quota||published.size()>8)throw new IOException("REPORT_PUBLISHED_QUOTA_PROTECTED");}
 }
 private static long publishedBytes(File job)throws Exception{long bytes=0;JSONObject index=new File(job,"delivery/index.json").isFile()?delivery(job):null;if(index!=null){JSONArray rows=index.getJSONArray("files");if(rows.length()>128)throw new IOException("REPORT_DELIVERY_BOUND");for(int i=0;i<rows.length();i++)bytes=Math.addExact(bytes,rows.getJSONObject(i).getLong("bytes"));bytes=Math.addExact(bytes,new File(job,"delivery/index.json").length());ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));if(!state.publishedDir.isEmpty())bytes=Math.addExact(bytes,new File(job,state.publishedDir+"/manifest.jsonl").length());}return bytes;}
 private static boolean deletePublished(File job,long now)throws Exception{File until=new File(job,"share-until.json");if(until.exists()&&new JSONObject(ReportJobJournalCP12.readAtomic(until,4096)).getLong("expiresWallMs")>now)return false;synchronized(OPEN){if(OPEN.containsKey(new File(job,"share.lock").getCanonicalPath()))return false;}try(RandomAccessFile io=new RandomAccessFile(new File(job,"share.lock"),"rw")){FileLock lock;try{lock=io.getChannel().tryLock();}catch(OverlappingFileLockException held){return false;}if(lock==null)return false;try{cleanChildren(job,new HashSet<String>(Arrays.asList("job.json","share.lock")));ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));state.stage="FAILED";state.lastReason="REPORT_EXPIRED_BY_QUOTA";ReportJobJournalCP12.writeAtomic(new File(job,"job.json"),state.json().toString());JSONObject ack=new JSONObject();ack.put("requestId",state.requestId);ack.put("jobId",state.jobId);ack.put("stage","FAILED");ack.put("reason",state.lastReason);ReportJobJournalCP12.writeAtomic(new File(job.getParentFile().getParentFile(),"requests/"+state.requestId+".ack.json"),ack.toString());return true;}finally{lock.release();}}}
 /** A verified, sealed single ZIP is sufficient for recovery; legacy multipart reports keep parts. */
 public static boolean verifyPublishedZip(File job,ReportNativeCP4.Control control)throws Exception {
  if(!new File(job,"delivery/index.json").isFile())return false;
  JSONObject index=delivery(job);if(!"SCREPORT_SINGLE_ZIP_R992".equals(index.optString("format")))return false;
  ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));
  if(!"READY_TO_SHARE".equals(state.stage))throw new IOException("REPORT_ZIP_NOT_PUBLISHED");
  JSONArray files=index.getJSONArray("files");if(files.length()!=1)throw new IOException("REPORT_SINGLE_ZIP_COUNT");
  JSONObject row=files.getJSONObject(0);if(!"SINGLE_ZIP".equals(row.getString("kind"))||!"delivery/STARCORE_FULL_REPORT.zip".equals(row.getString("path")))throw new IOException("REPORT_SINGLE_ZIP_IDENTITY");
  File zip=ReportArchiveVerifierCP12.inside(job,row.getString("path"));
  if(!zip.isFile()||zip.length()!=row.getLong("bytes")||!row.getString("sha256").equals(ReportArchiveCP4.hash(zip,control)))throw new IOException("REPORT_SINGLE_ZIP_HASH");
  File manifest=ReportArchiveVerifierCP12.inside(job,state.publishedDir+"/manifest.jsonl");
  if(!state.manifestSha.equals(ReportArchiveCP4.hash(manifest,control)))throw new IOException("REPORT_PUBLISH_HASH");
  return true;
 }
 public static void cleanRaw(File job,String archive)throws IOException{
  try{
   if(verifyPublishedZip(job,()->true)) {
    File directory=ReportArchiveVerifierCP12.inside(job,archive);File[] parts=directory.listFiles();if(parts==null)throw new IOException("REPORT_CLEANUP_LIST");
    for(File part:parts)if(part.getName().matches("P[0-9]{6}\\.bin")){
     if(!part.getAbsoluteFile().equals(part.getCanonicalFile())||!part.isFile()||!part.delete())throw new IOException("REPORT_PART_CLEANUP_FAILED");
    }
   }
   cleanChildren(job,new HashSet<String>(Arrays.asList("job.json","published.json","delivery","share.lock","share-until.json",archive)));
  }catch(IOException error){throw error;}catch(Exception error){throw new IOException("REPORT_CLEANUP_VERIFICATION_FAILED",error);}
 }
 private static void cleanChildren(File directory,Set<String> keep)throws IOException{File[] files=directory.listFiles();if(files==null)throw new IOException("REPORT_CLEANUP_LIST");if(files.length>4096)throw new IOException("REPORT_CLEANUP_DIRECTORY_LIMIT");for(File file:files){if(keep.contains(file.getName()))continue;if(!file.getAbsoluteFile().equals(file.getCanonicalFile()))throw new IOException("REPORT_CLEANUP_SYMLINK");if(file.isDirectory())cleanChildren(file,Collections.<String>emptySet());if(!file.delete())throw new IOException("REPORT_CLEANUP_FAILED");}}

 public static void pruneTerminal(File root,String current)throws Exception{File jobs=new File(root,"jobs");File[] dirs=jobs.listFiles();if(dirs==null)return;if(dirs.length>48)throw new IOException("REPORT_JOB_DIRECTORY_LIMIT");for(File d:dirs){if(d.getName().equals(current))continue;ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(d,"job.json"),65536));if(new File(d,"published.json").exists()){if(state.stage.equals("READY_TO_SHARE"))try{cleanRaw(d,state.publishedDir);}catch(IOException cleanup){ReportJobJournalCP12.writeAtomic(new File(d,"cleanup-pending.json"),new JSONObject().put("state","UNVERIFIED_DELIVERY_PRESERVED").put("reason",String.valueOf(cleanup.getMessage())).toString());}continue;}if(state.stage.equals("FAILED")||state.stage.equals("CANCELLED")||state.stage.equals("RETENTION_READY")){cleanChildren(d,Collections.<String>emptySet());if(!d.delete())throw new IOException("REPORT_TERMINAL_CLEANUP");ReportJobJournalCP12.removeCancellation(root,state.jobId);}}}

 /** Expire terminal metadata, including failures before request publication. Pending work is protected. */
 public static void pruneRequestMetadata(File root,long now)throws Exception {
  File requests=new File(root,"requests");File[] files=requests.listFiles();if(files==null)return;
  // Clean old installations before enforcing retention. A historical backlog is not an export failure.
  Set<String> pending=ReportRequestQueueCP12.pendingIds(root);List<File> terminal=new ArrayList<>();
  for(File file:files){
   if(!file.getName().matches("[0-9a-f]{64}\\.ack\\.json"))continue;
   String id=file.getName().substring(0,64);if(pending.contains(id))continue;
   JSONObject ack=new JSONObject(ReportJobJournalCP12.readAtomic(file,8192));
   String stage=ack.optString("stage",ack.optString("state",""));
   if(!stage.equals("READY_TO_SHARE")&&!stage.equals("FAILED")&&!stage.equals("CANCELLED")&&!stage.equals("VERIFIED_READBACK"))continue;
   File request=new File(requests,id+".json");
   if(request.exists()){
    JSONObject body=new JSONObject(ReportJobJournalCP12.readAtomic(request,65536));
    if(body.optString("mode","").equals("RETENTION_PAGE"))continue;
   }
   terminal.add(file);
  }
  Collections.sort(terminal,(a,b)->Long.compare(a.lastModified(),b.lastModified()));int retained=terminal.size();
  for(File ack:terminal){
   if(retained<=256&&now-ack.lastModified()<7*24*60*60*1000L)continue;
   String id=ack.getName().substring(0,64);
   String cancelJob=new JSONObject(ReportJobJournalCP12.readAtomic(ack,8192)).optString("jobId","");
   File request=new File(requests,id+".json");
   if(request.exists()&&!request.delete())throw new IOException("REPORT_REQUEST_METADATA_CLEANUP");
   if(!ack.delete())throw new IOException("REPORT_REQUEST_METADATA_CLEANUP");
   ReportJobJournalCP12.removeCancellation(root,id);
   if(cancelJob.matches("[0-9a-f]{64}"))ReportJobJournalCP12.removeCancellation(root,cancelJob);
   File receipt=ReportArchiveVerifierCP12.inside(root,"saf/"+id+".json");
   if(receipt.exists()&&!receipt.delete())throw new IOException("REPORT_REQUEST_METADATA_CLEANUP");
   for(String relative:new String[]{"observations/"+id,"recovery/"+id,"saf-parts/"+id}){
    File generated=ReportArchiveVerifierCP12.inside(root,relative);
    if(generated.isDirectory()){
     cleanChildren(generated,Collections.<String>emptySet());
     if(!generated.delete())throw new IOException("REPORT_EXPIRED_RECOVERY_CLEANUP");
    }
   }
   File failure=ReportArchiveVerifierCP12.inside(root,"failures/"+id+".txt");
   if(failure.exists()&&!failure.delete())throw new IOException("REPORT_EXPIRED_FAILURE_CLEANUP");
   retained--;
  }
 }

}
