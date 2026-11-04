package com.ethk3.novaonepro;
import java.io.*;import java.nio.channels.*;import java.util.*;import org.json.*;
/** Immutable published archives plus delivery pieces. No database, BET, settlement or model write authority. */
public final class ReportPublicationCP12 {
 private static final long MiB=1024L*1024L;private static final Map<String,Shared> OPEN=new HashMap<>();
 private ReportPublicationCP12(){}
 /** Includes snapshot, raw workspace, partials and cleanup records, unlike delivery size. */
 public static long jobPhysicalBytes(File job)throws IOException{return StorageBudgetR31.scopeBytes(job.getAbsoluteFile());}
 public static long allPhysicalBytes(File root)throws IOException{return StorageBudgetR31.scopeBytes(root.getAbsoluteFile());}
 private static final class Shared{final RandomAccessFile io;final FileLock lock;int refs;Shared(RandomAccessFile io,FileLock lock){this.io=io;this.lock=lock;refs=1;}}
 public static final class Lease implements Closeable{final String key;boolean closed;Lease(String key){this.key=key;}public void close()throws IOException{synchronized(OPEN){if(closed)return;closed=true;Shared state=OPEN.get(key);if(--state.refs==0){OPEN.remove(key);try{state.lock.release();}finally{state.io.close();}}}}}
 public static Lease shared(File job)throws IOException{String key=ReportArchiveVerifierCP12.inside(job,"share.lock").getPath();synchronized(OPEN){Shared state=OPEN.get(key);if(state!=null){state.refs++;return new Lease(key);}if(OPEN.size()>=8)throw new IOException("REPORT_SHARE_LEASE_LIMIT");RandomAccessFile io=new RandomAccessFile(key,"rw");try{FileLock lock=io.getChannel().tryLock(0,Long.MAX_VALUE,true);if(lock==null)throw new IOException("REPORT_SHARE_BUSY");OPEN.put(key,new Shared(io,lock));return new Lease(key);}catch(Exception bad){io.close();if(bad instanceof IOException)throw(IOException)bad;throw new IOException("REPORT_SHARE_BUSY",bad);}}}
 public static JSONObject delivery(File job)throws Exception{File file=ReportArchiveVerifierCP12.inside(job,"delivery/index.json");JSONObject index=new JSONObject(ReportJobJournalCP12.readAtomic(file,65536));ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));if(state.stage.equals("READY_TO_SHARE")){JSONObject pointer=new JSONObject(ReportJobJournalCP12.readAtomic(new File(job,"published.json"),4096));if(!state.jobId.equals(index.getString("jobId"))||!state.manifestSha.equals(index.getString("manifestSha256"))||state.evidenceComplete!=index.getBoolean("evidenceComplete")||state.checksComplete!=index.getBoolean("checksComplete")||state.allChecksPassed!=index.getBoolean("allChecksPassed")||!pointer.getString("deliveryIndexSha256").equals(ReportArchiveCP4.hash(file,()->true)))throw new IOException("REPORT_DELIVERY_INDEX_INTEGRITY");}return index;}
 public static File job(File files,String id)throws Exception{if(id==null||!id.matches("[0-9a-f]{64}"))throw new IOException("REPORT_PUBLISHED_ID");File directory=ReportArchiveVerifierCP12.inside(ReportDirectoriesCP12.root(files),"jobs/"+id);ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(directory,"job.json"),65536));if(!state.stage.equals("READY_TO_SHARE")||!state.jobId.equals(id))throw new IOException("REPORT_NOT_PUBLISHED");return directory;}
 /** Every exported item, including a large manifest segment, is <=8MiB. Full manifest stays native-verifiable in archive/. */
 public static long prepare(File job,File archive,String id,String manifestHash,boolean complete,boolean checks,boolean passed,ReportNativeCP4.Control control)throws Exception{
  File delivery=new File(job,"delivery");if(!delivery.isDirectory()&&!delivery.mkdir())throw new IOException("REPORT_DELIVERY_DIRECTORY");JSONObject index=new JSONObject();index.put("format","SCREPORT_DELIVERY_CP12_1");index.put("jobId",id);index.put("manifestSha256",manifestHash);index.put("evidenceComplete",complete);index.put("checksComplete",checks);index.put("allChecksPassed",passed);JSONArray files=new JSONArray();long total=0;
  File manifest=new File(archive,"manifest.jsonl");if(!manifestHash.equals(ReportArchiveCP4.hash(manifest,control)))throw new IOException("REPORT_DELIVERY_MANIFEST_CHANGED");
  try(InputStream in=new FileInputStream(manifest)){byte[] buffer=new byte[32768];int number=0;while(true){int first=in.read(buffer);if(first<0)break;File segment=new File(delivery,String.format(Locale.ROOT,"M%06d.manifest",++number));try(FileOutputStream fd=new FileOutputStream(segment);OutputStream out=ReportIoR31.protect(segment,fd,control)){out.write(buffer,0,first);long bytes=first;while(bytes<ReportArchiveCP4.PART_BYTES){if(!control.shouldContinue())throw new InterruptedIOException("REPORT_DELIVERY_CANCELLED");int n=in.read(buffer,0,(int)Math.min(buffer.length,ReportArchiveCP4.PART_BYTES-bytes));if(n<0)break;out.write(buffer,0,n);bytes+=n;}out.flush();fd.getFD().sync();}total=Math.addExact(total,segment.length());item(files,segment.getName(),"delivery/"+segment.getName(),segment,control,"MANIFEST_SEGMENT");}}
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
 /** R30 default: one ordinary ZIP up to 20MiB, otherwise ordered byte parts of that verified ZIP.
  * Parts are not independent ZIPs. The last-written index specifies reconstruction order and whole-ZIP SHA. */
 public static final long ZIP_LIMIT_R30=20L*1024L*1024L;
 public static final String ZIP_PART_FORMAT_R30="SCREPORT_ZIP_PARTS_R30_1";
 public static long prepareBoundedZipR30(File job,File archive,String id,String manifestHash,boolean complete,boolean checks,boolean passed,ReportNativeCP4.Control control)throws Exception{
  prepareSingleZip(job,archive,id,manifestHash,complete,checks,passed,control);
  return boundZipR30(job,control);
 }
 static void checkR30(ReportNativeCP4.Control control)throws IOException{if(!control.shouldContinue())throw new InterruptedIOException("REPORT_USER_CANCEL");}
 static long boundZipR30(File job,ReportNativeCP4.Control control)throws Exception{
  File delivery=ReportArchiveVerifierCP12.inside(job,"delivery");File indexFile=new File(delivery,"index.json");
  JSONObject index=new JSONObject(ReportJobJournalCP12.readAtomic(indexFile,65536));
  if(!"SCREPORT_SINGLE_ZIP_R992".equals(index.getString("format")))throw new IOException("R30_ZIP_SOURCE_FORMAT");
  JSONArray existing=index.getJSONArray("files");if(existing.length()!=1)throw new IOException("R30_ZIP_SOURCE_COUNT");
  JSONObject source=existing.getJSONObject(0);File zip=ReportArchiveVerifierCP12.inside(job,source.getString("path"));
  if(!"delivery/STARCORE_FULL_REPORT.zip".equals(source.getString("path"))||!zip.isFile()||zip.length()!=source.getLong("bytes"))throw new IOException("R30_ZIP_SOURCE_IDENTITY");
  long size=zip.length(),parts=(size+ZIP_LIMIT_R30-1)/ZIP_LIMIT_R30;
  if(parts>128)throw new IOException("R30_ZIP_DELIVERY_INDEX_LIMIT");
  File[] previous=DirectoryCursorR31.children(delivery,256);
  for(File file:previous)if(file.getName().matches("STARCORE_FULL_REPORT\\.zip\\.part[0-9]{2,3}(\\.partial)?")){
   checkR30(control);if(!file.getAbsoluteFile().equals(file.getCanonicalFile())||!file.isFile()||!file.delete())throw new IOException("R30_OLD_ZIP_PART_CLEANUP");
  }
  index.put("defaultZipLimitBytes",ZIP_LIMIT_R30);
  if(size<=ZIP_LIMIT_R30){index.put("expectedVolumes",1).put("exportedVolumes",1).put("omittedVolumes",0);ReportJobJournalCP12.writeAtomic(indexFile,index.toString());return size+indexFile.length();}
  JSONArray rows=new JSONArray();java.security.MessageDigest all=java.security.MessageDigest.getInstance("SHA-256");long copied=0;
  try(InputStream input=new BufferedInputStream(new FileInputStream(zip),32768)){
   byte[] buffer=new byte[32768];for(int number=1;copied<size;number++){
    checkR30(control);String name=String.format(Locale.ROOT,"STARCORE_FULL_REPORT.zip.part%02d",number);
    File target=ReportArchiveVerifierCP12.inside(delivery,name),partial=new File(target.getPath()+".partial");
    long start=copied,bytes=0;java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");
    try(FileOutputStream outputFile=new FileOutputStream(partial);OutputStream output=ReportIoR31.protect(partial,outputFile,control)){
     while(bytes<ZIP_LIMIT_R30&&copied<size){checkR30(control);int n=input.read(buffer,0,(int)Math.min(buffer.length,Math.min(ZIP_LIMIT_R30-bytes,size-copied)));if(n<0)throw new EOFException("R30_ZIP_SOURCE_TRUNCATED");output.write(buffer,0,n);hash.update(buffer,0,n);all.update(buffer,0,n);bytes+=n;copied+=n;}
     output.flush();outputFile.getFD().sync();
    }
    String sha=ReportArchiveCP4.hex(hash.digest());if(partial.length()!=bytes||!sha.equals(ReportArchiveCP4.hash(partial,control)))throw new IOException("R30_ZIP_PART_READBACK");
    if(target.exists()||!partial.renameTo(target))throw new IOException("R30_ZIP_PART_PUBLISH");
    rows.put(new JSONObject().put("name",name).put("path","delivery/"+name).put("kind","ZIP_PART").put("partNumber",number).put("offset",start).put("bytes",bytes).put("sha256",sha));
   }
   if(input.read()!=-1)throw new IOException("R30_ZIP_SOURCE_GREW");
  }
  if(copied!=size||rows.length()!=parts||!source.getString("sha256").equals(ReportArchiveCP4.hex(all.digest())))throw new IOException("R30_ZIP_SOURCE_CHANGED");
  index.put("format",ZIP_PART_FORMAT_R30).put("files",rows).put("zipBytes",size).put("zipSha256",source.getString("sha256"));
  index.put("expectedVolumes",parts).put("exportedVolumes",rows.length()).put("omittedVolumes",0);
  index.put("reassembly","按 files 数组顺序逐字节合并全部 part01..N 为 STARCORE_FULL_REPORT.zip；长度应为 zipBytes，SHA-256 应为 zipSha256。分卷不能单独作为ZIP打开。index.json 必须与全部分卷一起保留。");
  verifyPartsR30(job,index,control);checkR30(control);ReportJobJournalCP12.writeAtomic(indexFile,index.toString());
  if(!zip.delete())throw new IOException("R30_ZIP_REDUNDANT_SOURCE_CLEANUP");return size+indexFile.length();
 }
 static boolean verifyPartsR30(File job,JSONObject index,ReportNativeCP4.Control control)throws Exception{
  JSONArray files=index.getJSONArray("files");long size=index.getLong("zipBytes"),total=0;int expected=(int)((size+ZIP_LIMIT_R30-1)/ZIP_LIMIT_R30);
  if(size<=ZIP_LIMIT_R30||expected<2||expected>128||files.length()!=expected||index.getLong("expectedVolumes")!=expected||index.getLong("exportedVolumes")!=expected||index.getLong("omittedVolumes")!=0)throw new IOException("R30_ZIP_PART_COUNT");
  java.security.MessageDigest all=java.security.MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[32768];
  for(int i=0;i<files.length();i++){
   checkR30(control);JSONObject row=files.getJSONObject(i);String name=String.format(Locale.ROOT,"STARCORE_FULL_REPORT.zip.part%02d",i+1);long wanted=Math.min(ZIP_LIMIT_R30,size-total);
   if(!"ZIP_PART".equals(row.getString("kind"))||!name.equals(row.getString("name"))||!("delivery/"+name).equals(row.getString("path"))||row.getInt("partNumber")!=i+1||row.getLong("offset")!=total||row.getLong("bytes")!=wanted)throw new IOException("R30_ZIP_PART_IDENTITY");
   File file=ReportArchiveVerifierCP12.inside(job,row.getString("path"));if(!file.isFile()||file.length()!=wanted)throw new IOException("R30_ZIP_PART_SIZE");
   java.security.MessageDigest part=java.security.MessageDigest.getInstance("SHA-256");try(InputStream input=new FileInputStream(file)){int n;while((n=input.read(buffer))!=-1){checkR30(control);all.update(buffer,0,n);part.update(buffer,0,n);}}
   if(!row.getString("sha256").equals(ReportArchiveCP4.hex(part.digest())))throw new IOException("R30_ZIP_PART_HASH");total+=wanted;
  }
  if(total!=size||index.getLong("totalVolumeBytes")!=size||!index.getString("zipSha256").equals(ReportArchiveCP4.hex(all.digest())))throw new IOException("R30_ZIP_REASSEMBLY_HASH");return true;
 }
 /** Called by the sole owner after try-with-resources has released all readers, streams and input pins. */
 public static void cleanCancelledR30(File job)throws IOException{
  if(new File(job,"published.json").exists())return; // An already published immutable export is preserved.
  cleanChildren(job,new HashSet<String>(Arrays.asList("job.json","share.lock","cancel-receipt-r30.json")));
 }
 /** Delete old published reports, preserving the current report and open/shared exports. */
 public static void pruneOldReports(File root,File current,long now)throws Exception{
  File[] directories=DirectoryCursorR31.children(new File(root,"jobs"),1024);
  List<File> published=new ArrayList<>();for(File d:directories)if(!d.equals(current)&&new File(d,"published.json").isFile())published.add(d);
  published.sort((a,b)->Long.compare(b.lastModified(),a.lastModified()));
  for(int i=0;i<published.size();i++){File d=published.get(i);if(i>=2||now-d.lastModified()>7*24*60*60*1000L)deletePublished(d,now);}
 }
 public static void grant(File job,long now)throws Exception{try(Lease pin=shared(job)){ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));if(!state.stage.equals("READY_TO_SHARE"))throw new IOException("REPORT_SHARE_NOT_READY");JSONObject lease=new JSONObject();lease.put("expiresWallMs",Math.addExact(now,24*60*60*1000L));ReportJobJournalCP12.writeAtomic(new File(job,"share-until.json"),lease.toString());}}
 /** Explicit user deletion: ignore the soft 24-hour grant, honor actual open reader locks.
  * Caller holds the report-owner lock and has detached the current-job pointer. No file hashing. */
 public static boolean discardByUser(File job)throws Exception {
  if(!job.exists())return true;
  synchronized(OPEN){File lockFile=ReportArchiveVerifierCP12.inside(job,"share.lock");
  if(OPEN.containsKey(lockFile.getCanonicalPath()))return false;
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
  return true;}
 }
 /** Resume an interrupted deletion from a directory that only explicit user deletion can create. */
 public static void finishUserDeletions(File root)throws IOException {
  File trash=ReportArchiveVerifierCP12.inside(root,"deleted-reports-r69");File[] dirs=DirectoryCursorR31.children(trash,1024);
  for(File directory:dirs){if(!directory.getName().matches("[0-9a-f]{64}")||!directory.getAbsoluteFile().equals(directory.getCanonicalFile()))throw new IOException("REPORT_USER_DELETE_PATH");
   cleanChildren(directory,Collections.<String>emptySet());if(!directory.delete())throw new IOException("REPORT_USER_DELETE_FAILED");}
 }
 /** Publication owner calls before READY. Single writer lock is already held. Eight reports and aggregate Q are hard limits. */
 public static void enforceQuota(File root,File current,long quota)throws Exception{
  long physical=allPhysicalBytes(root);if(physical>ReportBudgetSU2.PHYSICAL_REPORT_ADMISSION_BYTES)throw new IOException("REPORT_PHYSICAL_QUOTA_PROTECTED_DEBT:"+physical);
  File jobs=new File(root,"jobs");File[] dirs=DirectoryCursorR31.children(jobs,1024);long total=0;List<File> published=new ArrayList<>();for(File d:dirs){if(!d.getName().matches("[0-9a-f]{64}")||!d.isDirectory())throw new IOException("REPORT_QUOTA_JOB_PATH");File ptr=new File(d,"published.json");if(ptr.isFile()||d.equals(current)){published.add(d);total=Math.addExact(total,publishedBytes(d));}}
  Collections.sort(published,(a,b)->Long.compare(a.lastModified(),b.lastModified()));for(File d:published){if(total<=quota&&published.size()<=8)break;if(d.equals(current))continue;long bytes=publishedBytes(d);if(deletePublished(d,System.currentTimeMillis())){total-=bytes;published.remove(d);break;}}
  // Repeat using a fresh bounded scan; removal changes neither caller's archive nor another held shared lease.
  if(total>quota||published.size()>8){boolean progress=false;for(File d:new ArrayList<File>(published)){if(d.equals(current))continue;long bytes=publishedBytes(d);if(deletePublished(d,System.currentTimeMillis())){total-=bytes;published.remove(d);progress=true;if(total<=quota&&published.size()<=8)break;}}if(total>quota||published.size()>8)throw new IOException("REPORT_PUBLISHED_QUOTA_PROTECTED");}
 }
 private static long publishedBytes(File job)throws Exception{long bytes=0;JSONObject index=new File(job,"delivery/index.json").isFile()?delivery(job):null;if(index!=null){JSONArray rows=index.getJSONArray("files");if(rows.length()>128)throw new IOException("REPORT_DELIVERY_BOUND");for(int i=0;i<rows.length();i++)bytes=Math.addExact(bytes,rows.getJSONObject(i).getLong("bytes"));bytes=Math.addExact(bytes,new File(job,"delivery/index.json").length());ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));if(!state.publishedDir.isEmpty())bytes=Math.addExact(bytes,new File(job,state.publishedDir+"/manifest.jsonl").length());}return bytes;}
 private static boolean deletePublished(File job,long now)throws Exception{File until=new File(job,"share-until.json");if(until.exists()&&new JSONObject(ReportJobJournalCP12.readAtomic(until,4096)).getLong("expiresWallMs")>now)return false;synchronized(OPEN){if(OPEN.containsKey(new File(job,"share.lock").getCanonicalPath()))return false;try(RandomAccessFile io=new RandomAccessFile(new File(job,"share.lock"),"rw")){FileLock lock;try{lock=io.getChannel().tryLock();}catch(OverlappingFileLockException held){return false;}if(lock==null)return false;try{cleanChildren(job,new HashSet<String>(Arrays.asList("job.json","share.lock")));ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));state.stage="FAILED";state.lastReason="REPORT_EXPIRED_BY_QUOTA";ReportJobJournalCP12.writeAtomic(new File(job,"job.json"),state.json().toString());JSONObject ack=new JSONObject();ack.put("requestId",state.requestId);ack.put("jobId",state.jobId);ack.put("stage","FAILED");ack.put("reason",state.lastReason);ReportJobJournalCP12.writeAtomic(new File(job.getParentFile().getParentFile(),"requests/"+state.requestId+".ack.json"),ack.toString());return true;}finally{lock.release();}}}}
 /** A verified, sealed single ZIP is sufficient for recovery; legacy multipart reports keep parts. */
 public static boolean verifyPublishedZip(File job,ReportNativeCP4.Control control)throws Exception {
  if(!new File(job,"delivery/index.json").isFile())return false;
  JSONObject index=delivery(job);boolean split=ZIP_PART_FORMAT_R30.equals(index.optString("format"));if(!split&&!"SCREPORT_SINGLE_ZIP_R992".equals(index.optString("format")))return false;
  ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));
  if(!"READY_TO_SHARE".equals(state.stage))throw new IOException("REPORT_ZIP_NOT_PUBLISHED");
  if(split){verifyPartsR30(job,index,control);File manifest=ReportArchiveVerifierCP12.inside(job,state.publishedDir+"/manifest.jsonl");if(!state.manifestSha.equals(ReportArchiveCP4.hash(manifest,control)))throw new IOException("REPORT_PUBLISH_HASH");return true;}
  JSONArray files=index.getJSONArray("files");if(files.length()!=1)throw new IOException("REPORT_SINGLE_ZIP_COUNT");
  JSONObject row=files.getJSONObject(0);if(!"SINGLE_ZIP".equals(row.getString("kind"))||!"delivery/STARCORE_FULL_REPORT.zip".equals(row.getString("path")))throw new IOException("REPORT_SINGLE_ZIP_IDENTITY");
  File zip=ReportArchiveVerifierCP12.inside(job,row.getString("path"));
  if(!zip.isFile()||zip.length()!=row.getLong("bytes")||!row.getString("sha256").equals(ReportArchiveCP4.hash(zip,control)))throw new IOException("REPORT_SINGLE_ZIP_HASH");
  File manifest=ReportArchiveVerifierCP12.inside(job,state.publishedDir+"/manifest.jsonl");
  if(!state.manifestSha.equals(ReportArchiveCP4.hash(manifest,control)))throw new IOException("REPORT_PUBLISH_HASH");
  return true;
 }
 public static void cleanRaw(File job,String archive)throws IOException{
  if(ReportInputsLeaseCP12.currentThreadHoldsInput())throw new IOException("REPORT_CLEANUP_INPUTS_STILL_OPEN");
  // The provider holds this lease until the actual PFD closes, including recovery exports.
  synchronized(OPEN){File lockFile=ReportArchiveVerifierCP12.inside(job,"share.lock");
   if(OPEN.containsKey(lockFile.getCanonicalPath()))throw new IOException("REPORT_CLEANUP_ACTIVE_SHARE");
   try(RandomAccessFile io=new RandomAccessFile(lockFile,"rw")){
    FileLock lock;try{lock=io.getChannel().tryLock();}catch(OverlappingFileLockException busy){throw new IOException("REPORT_CLEANUP_ACTIVE_SHARE",busy);}
    if(lock==null)throw new IOException("REPORT_CLEANUP_ACTIVE_SHARE");
    try{cleanRawOwned(job,archive);}finally{lock.release();}
   }
  }
 }
 private static void cleanRawOwned(File job,String archive)throws IOException{
  try{
   requireRawCoverageR31(job,archive);
   if(verifyPublishedZip(job,()->true)) {
    File directory=ReportArchiveVerifierCP12.inside(job,archive);File[] parts=DirectoryCursorR31.children(directory,4096);
    for(File part:parts)if(part.getName().matches("P[0-9]{6}\\.bin")){
     if(!part.getAbsoluteFile().equals(part.getCanonicalFile())||!part.isFile()||!part.delete())throw new IOException("REPORT_PART_CLEANUP_FAILED");
    }
   }
   cleanChildren(job,new HashSet<String>(Arrays.asList("job.json","published.json","delivery","share.lock","share-until.json","R30_SUCCESSFUL_SUMMARY_CANDIDATE.json","cleanup-r31.json",archive)));
  }catch(IOException error){throw error;}catch(Exception error){throw new IOException("REPORT_CLEANUP_VERIFICATION_FAILED",error);}
 }
 /** FULL_RAW can retire its private DB only when the verified archive actually contains that exact DB. */
 static void requireRawCoverageR31(File job,String archive)throws Exception{
  ReportJobJournalCP12.Job state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(job,"job.json"),65536));
  File request=new File(job.getParentFile().getParentFile(),"requests/"+state.requestId+".json");boolean raw=false,known=false;
  if(request.isFile()){JSONObject row=new JSONObject(ReportJobJournalCP12.readAtomic(request,65536));raw=row.optBoolean("includeRawEvidence",false);known=true;}
  File manifest=new File(job,archive+"/manifest.jsonl");if(manifest.length()>64L*1024*1024)throw new IOException("REPORT_CLEANUP_MANIFEST_BUDGET");
  boolean contains=false;String scope="";int linesRead=0;try(ReportArchiveVerifierCP12.LineReader lines=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(ReportArchiveVerifierCP12.inside(job,archive+"/manifest.jsonl")),8192)){byte[] bytes;while((bytes=lines.next())!=null){if(++linesRead>200000||Thread.currentThread().isInterrupted())throw new IOException("REPORT_CLEANUP_MANIFEST_SCAN_BOUND");JSONObject row=new JSONObject(new String(bytes,java.nio.charset.StandardCharsets.UTF_8));if("member".equals(row.optString("kind"))&&"snapshot.db".equals(row.optString("name")))contains=state.snapshotSha.equals(row.optString("raw_sha256"))&&state.snapshotBytes==row.optLong("raw_bytes",-1);if("footer".equals(row.optString("kind")))scope=row.optString("scope");}}
  if(!known){if(scope.equals("COMPLETE_DIAGNOSTIC_R64_RAW_PAYLOADS_EXCLUDED")){known=true;raw=false;}else if(scope.equals("FULL_REPORT_SINGLE_SNAPSHOT_AND_FROZEN_ATTACHMENTS")){known=true;raw=true;}}
  if(!known)throw new IOException("REPORT_CLEANUP_COVERAGE_UNKNOWN");if(raw&&!contains)throw new IOException("REPORT_FULL_RAW_MEMBER_MISSING_PROTECTED");
 }
 /** Retire only caller-authorized recovery copies and respect actual cross-process share readers. */
 public static boolean tryDeleteRecoveryR31(File directory)throws IOException{
  synchronized(OPEN){File lockFile=ReportArchiveVerifierCP12.inside(directory,"share.lock");if(OPEN.containsKey(lockFile.getCanonicalPath()))return false;
  try(RandomAccessFile io=new RandomAccessFile(lockFile,"rw")){
   FileLock lock;try{lock=io.getChannel().tryLock();}catch(OverlappingFileLockException busy){return false;}if(lock==null)return false;
   try{cleanChildren(directory,new HashSet<String>(Arrays.asList("share.lock")));}finally{lock.release();}
  }
  if(lockFile.exists()&&!lockFile.delete())throw new IOException("REPORT_RECOVERY_LOCK_CLEANUP");if(!directory.delete())throw new IOException("REPORT_RECOVERY_DIRECTORY_CLEANUP");return true;}
 }
 private static void cleanChildren(File directory,Set<String> keep)throws IOException{cleanChildren(directory,keep,0,new int[]{0});}
 private static void cleanChildren(File directory,Set<String> keep,int depth,int[] entries)throws IOException{
  if(depth>16)throw new IOException("REPORT_CLEANUP_DEPTH_LIMIT");
  try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(directory)){File file;while((file=cursor.next())!=null){
   if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("REPORT_CLEANUP_CANCELLED");
   if(++entries[0]>200000)throw new IOException("REPORT_CLEANUP_ENTRY_LIMIT");if(keep.contains(file.getName()))continue;
   if(!file.getAbsoluteFile().equals(file.getCanonicalFile()))throw new IOException("REPORT_CLEANUP_SYMLINK");
   if(file.isDirectory())cleanChildren(file,Collections.<String>emptySet(),depth+1,entries);
   if(!file.delete())throw new IOException("REPORT_CLEANUP_FAILED");
  }}
 }

 public static void pruneTerminal(File root,String current)throws Exception{File jobs=new File(root,"jobs");File[] dirs=DirectoryCursorR31.children(jobs,1024);
  // R13-P34: >48 job directories or one unreadable job.json used to throw here; cleanup then stopped for good and
  // every later failed full report left one more jobs/<id>/snapshot.db behind. Both cases now clean instead.
  for(File d:dirs){if(d.getName().equals(current))continue;ReportJobJournalCP12.Job state;
   try{state=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(d,"job.json"),65536));}
   catch(Exception unreadable){if(ReportBudgetSU2.abandoned(d,System.currentTimeMillis())){cleanChildren(d,Collections.<String>emptySet());if(!d.delete())throw new IOException("REPORT_TERMINAL_CLEANUP");}continue;}
   if(new File(d,"published.json").exists()){if(state.stage.equals("READY_TO_SHARE"))ReportCleanupR31.enrollReadyDirectoryR31(d,state);continue;}if(state.stage.equals("FAILED")&&new File(d,"snapshot.db").exists())continue; /* failed sole input is protected capacity debt */ if(state.stage.equals("FAILED")||state.stage.equals("CANCELLED")||state.stage.equals("RETENTION_READY")){cleanChildren(d,Collections.<String>emptySet());if(!d.delete())throw new IOException("REPORT_TERMINAL_CLEANUP");ReportJobJournalCP12.removeCancellation(root,state.jobId);}}}

 /** Expire terminal metadata, including failures before request publication. Pending work is protected. */
 public static void pruneRequestMetadata(File root,long now)throws Exception {
  ReportBudgetSU2.pruneHeavyRecovery(root,now);   // R13-P34: heavy recovery/saf parts: newest 3, at most 24h
  File requests=new File(root,"requests");File[] files=DirectoryCursorR31.children(requests,4096);
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
   boolean protectedRecovery=false;for(String kind:new String[]{"recovery","saf-parts"})if(new File(root,kind+"/"+id).isDirectory()&&!ReportBudgetSU2.retirableRecovery(root,id))protectedRecovery=true;if(protectedRecovery)continue;
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
     if(!tryDeleteRecoveryR31(generated))continue;
    }
   }
   File failure=ReportArchiveVerifierCP12.inside(root,"failures/"+id+".txt");
   if(failure.exists()&&!failure.delete())throw new IOException("REPORT_EXPIRED_FAILURE_CLEANUP");
   retained--;
  }
 }

}
