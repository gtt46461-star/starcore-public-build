package com.ethk3.novaonepro;

import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.channels.*;
import java.util.*;
import org.json.JSONObject;

/** The file lock owns this journal. It grants no business database write authority. */
public final class ReportJobJournalCP12 implements Closeable {
    public enum Stage { CLAIMED,BACKUP,SNAPSHOT_DURABLE,INDEXING,INDEX_DURABLE,COMPRESSING,VERIFYING,READY_TO_SHARE,RETENTION_ENCODING,RETENTION_RECEIPT_DURABLE,RETENTION_READY,PAUSED,FAILED,CANCELLED }
    public enum Recovery { BACKUP,REBUILD_INDEX,RECOMPRESS,VERIFY_AND_PUBLISH,READ_PUBLISHED,WAIT_FOR_KICK,TERMINAL }
    public static final int MAX_JSON=65536;
    private static final Object CANCEL_COMMANDS_LOCK=new Object();
    private interface CancelWork<T>{T run()throws Exception;}
    /** Independent durable commands: cancelling one task cannot overwrite another task's command. */
    public static void requestCancellation(File root,String id)throws Exception {
        long requestedNanosR30=System.nanoTime();
        hash(id);withCancellationLock(root,()->{
            File job=ReportArchiveVerifierCP12.inside(root,"jobs/"+id+"/job.json");
            if(job.exists()) {
                if(!id.equals(Job.parse(readAtomic(job,MAX_JSON)).jobId))throw new IOException("REPORT_CANCEL_IDENTITY");
            } else {
                File request=ReportArchiveVerifierCP12.inside(root,"requests/"+id+".json");
                if(!request.exists())throw new IOException("REPORT_CANCEL_TASK_MISSING");
                JSONObject row=new JSONObject(readAtomic(request,MAX_JSON));
                if(!id.equals(row.getString("requestId"))||!"SAF_TRANSFER".equals(row.getString("mode")))throw new IOException("REPORT_CANCEL_IDENTITY");
            }
            File directory=ReportArchiveVerifierCP12.inside(root,"cancel-commands");
            if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("REPORT_CANCEL_DIRECTORY");
            File[] commands=directory.listFiles();if(commands==null)throw new IOException("REPORT_CANCEL_DIRECTORY_READ");
            File destination=ReportArchiveVerifierCP12.inside(root,"cancel-commands/"+id+".json");
            if(!destination.exists()&&commands.length>=1024)throw new IOException("REPORT_CANCEL_COMMAND_LIMIT");
            JSONObject command=new JSONObject();command.put("jobId",id);command.put("cancel",true);command.put("createdWallMs",System.currentTimeMillis());
            ReportTaskControlR69.stampCancellationR30(command,destination,requestedNanosR30);
            writeAtomic(destination,command.toString());return null;
        });
    }
    public static boolean isCancellationRequested(File root,String id)throws Exception {
        hash(id);return withCancellationLock(root,()->{
            File command=ReportArchiveVerifierCP12.inside(root,"cancel-commands/"+id+".json");
            if(command.exists()) {
                try{JSONObject row=new JSONObject(readAtomic(command,4096));
                    if(!id.equals(row.getString("jobId"))||!row.getBoolean("cancel"))throw new IOException("REPORT_CANCEL_IDENTITY");
                }catch(org.json.JSONException malformed){throw new IOException("REPORT_CANCEL_FORMAT",malformed);}
                return true;
            }
            // Preserve commands written by releases that used the original single-command file.
            File legacy=ReportArchiveVerifierCP12.inside(root,"cancel.json");if(!legacy.exists())return false;
            JSONObject row=new JSONObject(readAtomic(legacy,4096));return id.equals(row.optString("jobId"))&&row.optBoolean("cancel");
        });
    }
    public static void removeCancellation(File root,String id)throws Exception {
        hash(id);withCancellationLock(root,()->{File command=ReportArchiveVerifierCP12.inside(root,"cancel-commands/"+id+".json");
            if(command.exists())new AtomicFile(command).delete();if(command.exists())throw new IOException("REPORT_CANCEL_CLEANUP");return null;});
    }
    private static <T>T withCancellationLock(File root,CancelWork<T> work)throws Exception {
        synchronized(CANCEL_COMMANDS_LOCK){if(!root.isDirectory())throw new IOException("REPORT_CANCEL_ROOT");
            try(RandomAccessFile io=new RandomAccessFile(ReportArchiveVerifierCP12.inside(root,"cancel-command.lock"),"rw");FileLock lock=io.getChannel().lock()){return work.run();}}
    }
    public static final class Job {
        public final String jobId,requestId,releaseHash,mode;
        public String stage="CLAIMED",resumeStage="BACKUP",lastReason="",snapshotSha="",publishedDir="",manifestSha="";
        public long revision,attempt,createdWallMs,updatedWallMs,snapshotWallMs,snapshotBytes,temporaryReservationBytes,publishedQuotaBytes;
        public int compressionAttempt,preemptions,failureAttempts;
        public boolean evidenceComplete,checksComplete,allChecksPassed;
        public Job(String id,String request,String release,String mode,long now) {
            hash(id);hash(request);hash(release);if(!"FULL_REPORT".equals(mode)&&!"RETENTION_PAGE".equals(mode))throw new IllegalArgumentException("REPORT_MODE");
            jobId=id;requestId=request;releaseHash=release;this.mode=mode;createdWallMs=updatedWallMs=now;
        }
        JSONObject json()throws Exception {
            JSONObject o=new JSONObject();o.put("revision_name","REPORT_JOB_CP12_1");o.put("jobId",jobId);o.put("requestId",requestId);o.put("releaseHash",releaseHash);o.put("mode",mode);
            o.put("stage",stage);o.put("resumeStage",resumeStage);o.put("lastReason",lastReason);o.put("snapshotSha",snapshotSha);o.put("publishedDir",publishedDir);o.put("manifestSha",manifestSha);
            o.put("revision",revision);o.put("attempt",attempt);o.put("createdWallMs",createdWallMs);o.put("updatedWallMs",updatedWallMs);o.put("snapshotWallMs",snapshotWallMs);o.put("snapshotBytes",snapshotBytes);
            o.put("temporaryReservationBytes",temporaryReservationBytes);o.put("publishedQuotaBytes",publishedQuotaBytes);o.put("compressionAttempt",compressionAttempt);o.put("preemptions",preemptions);o.put("failureAttempts",failureAttempts);
            o.put("evidenceComplete",evidenceComplete);o.put("checksComplete",checksComplete);o.put("allChecksPassed",allChecksPassed);return o;
        }
        public static Job parse(String json)throws Exception {
            JSONObject o=new JSONObject(json);if(!"REPORT_JOB_CP12_1".equals(o.getString("revision_name")))throw new IOException("REPORT_JOB_REVISION");
            Job j=new Job(o.getString("jobId"),o.getString("requestId"),o.getString("releaseHash"),o.getString("mode"),o.getLong("createdWallMs"));
            j.stage=o.getString("stage");j.resumeStage=o.getString("resumeStage");Stage.valueOf(j.stage);Stage.valueOf(j.resumeStage);
            j.lastReason=o.getString("lastReason");j.snapshotSha=o.getString("snapshotSha");j.publishedDir=o.getString("publishedDir");j.manifestSha=o.getString("manifestSha");
            if(j.lastReason.length()>512||(!j.snapshotSha.isEmpty()&&!j.snapshotSha.matches("[0-9a-f]{64}"))||(!j.manifestSha.isEmpty()&&!j.manifestSha.matches("[0-9a-f]{64}"))
               ||(!j.publishedDir.isEmpty()&&!j.publishedDir.matches("archive-[0-9]{1,9}")))throw new IOException("REPORT_JOB_FIELD");
            j.revision=o.getLong("revision");j.attempt=o.getLong("attempt");j.updatedWallMs=o.getLong("updatedWallMs");j.snapshotWallMs=o.getLong("snapshotWallMs");j.snapshotBytes=o.getLong("snapshotBytes");
            j.temporaryReservationBytes=o.getLong("temporaryReservationBytes");j.publishedQuotaBytes=o.getLong("publishedQuotaBytes");j.compressionAttempt=o.getInt("compressionAttempt");j.preemptions=o.getInt("preemptions");j.failureAttempts=o.optInt("failureAttempts",0);
            j.evidenceComplete=o.getBoolean("evidenceComplete");j.checksComplete=o.getBoolean("checksComplete");j.allChecksPassed=o.getBoolean("allChecksPassed");
            if(j.revision<0||j.attempt<0||j.compressionAttempt<0||j.preemptions<0||j.failureAttempts<0||j.snapshotBytes<0||j.temporaryReservationBytes<0||j.publishedQuotaBytes<0)throw new IOException("REPORT_JOB_RANGE");
            return j;
        }
    }
    private final File root;private final RandomAccessFile lock;private final FileLock lease;private boolean closed;
    private ReportJobJournalCP12(File root,RandomAccessFile lock,FileLock lease){this.root=root;this.lock=lock;this.lease=lease;}
    public static ReportJobJournalCP12 tryClaim(File files)throws IOException {
        File root=ReportDirectoriesCP12.root(files);
        if(!root.getAbsoluteFile().equals(root.getCanonicalFile()))throw new IOException("REPORT_ROOT_SYMLINK");
        RandomAccessFile lock=new RandomAccessFile(new File(root,"job.lock"),"rw");FileLock lease=null;
        try{try{lease=lock.getChannel().tryLock();}catch(OverlappingFileLockException busy){return null;}
            if(lease==null)return null;return new ReportJobJournalCP12(root,lock,lease);
        }finally{if(lease==null)lock.close();}
    }
    public File root(){return root;}
    public void assertOwned()throws IOException {check();}
    public void forgetCurrent(String id)throws Exception {check();hash(id);File pointer=new File(root,"current-job.json");
        if(pointer.exists()&&id.equals(new JSONObject(readAtomic(pointer,4096)).getString("jobId"))){new AtomicFile(pointer).delete();if(pointer.exists())throw new IOException("REPORT_CURRENT_POINTER_DELETE");}}
    public File directory(Job j)throws IOException {check();File directory=ReportArchiveVerifierCP12.inside(root,"jobs/"+j.jobId);
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("REPORT_JOB_CREATE");return directory;}
    public Job current()throws Exception {check();File current=new File(root,"current-job.json");if(!current.exists())return null;
        JSONObject pointer=new JSONObject(readAtomic(current,4096));String id=pointer.getString("jobId");hash(id);
        Job j=Job.parse(readAtomic(ReportArchiveVerifierCP12.inside(root,"jobs/"+id+"/job.json"),MAX_JSON));
        if(!id.equals(j.jobId))throw new IOException("REPORT_JOB_POINTER");return j;}
    /** Read-only UI recovery lookup: retention jobs cannot hide the latest full export. */
    public static Job latestFullReport(File root)throws Exception {
        File jobs=ReportArchiveVerifierCP12.inside(root,"jobs");File[] directories=jobs.listFiles();
        if(directories==null)return null;if(directories.length>1024)throw new IOException("REPORT_RECOVERY_JOB_LIMIT");
        Job latest=null;
        for(File directory:directories){
            if(!directory.getName().matches("[0-9a-f]{64}"))throw new IOException("REPORT_RECOVERY_JOB_PATH");
            File journal=ReportArchiveVerifierCP12.inside(root,"jobs/"+directory.getName()+"/job.json");
            Job job=Job.parse(readAtomic(journal,MAX_JSON));
            if(!directory.getName().equals(job.jobId))throw new IOException("REPORT_RECOVERY_JOB_IDENTITY");
            if(job.mode.equals("FULL_REPORT")&&(latest==null||job.createdWallMs>latest.createdWallMs
                    ||job.createdWallMs==latest.createdWallMs&&job.jobId.compareTo(latest.jobId)>0))latest=job;
        }
        return latest;
    }
    public void create(Job j)throws Exception {check();File d=directory(j);File journal=new File(d,"job.json");if(journal.exists())throw new IOException("REPORT_JOB_EXISTS");
        writeAtomic(journal,j.json().toString());JSONObject pointer=new JSONObject();pointer.put("jobId",j.jobId);pointer.put("requestId",j.requestId);writeAtomic(new File(root,"current-job.json"),pointer.toString());}
    public void transition(Job j,Stage next,long now)throws Exception {check();Stage prior=Stage.valueOf(j.stage);if(!allowed(prior,next))throw new IOException("REPORT_STAGE_TRANSITION|"+prior+"|"+next);
        // Persist a detached copy first. The in-memory state must not claim success if fsync fails.
        Job copy=Job.parse(j.json().toString());copy.stage=next.name();copy.updatedWallMs=now;copy.revision=Math.addExact(copy.revision,1);
        writeAtomic(new File(directory(j),"job.json"),copy.json().toString());j.stage=copy.stage;j.updatedWallMs=copy.updatedWallMs;j.revision=copy.revision;acknowledge(j.requestId,j);
    }
    public void checkpoint(Job j,long now)throws Exception {check();Job copy=Job.parse(j.json().toString());copy.updatedWallMs=now;copy.revision=Math.addExact(copy.revision,1);
        writeAtomic(new File(directory(j),"job.json"),copy.json().toString());j.updatedWallMs=copy.updatedWallMs;j.revision=copy.revision;}
    public void acknowledge(String requestId,Job j)throws Exception {hash(requestId);JSONObject ack=new JSONObject();ack.put("requestId",requestId);ack.put("jobId",j.jobId);ack.put("mode",j.mode);ack.put("stage",j.stage);ack.put("revision",j.revision);ack.put("reason",j.lastReason);ack.put("evidenceComplete",j.evidenceComplete);ack.put("checksComplete",j.checksComplete);ack.put("allChecksPassed",j.allChecksPassed);addRecoveryReceipt(root,requestId,ack);writeAtomic(new File(root,"requests/"+requestId+".ack.json"),ack.toString());}
    public static Recovery recovery(Job j,boolean snapshotValid,boolean finishedManifest,boolean publishedValid) {
        Stage stage=Stage.valueOf(j.stage);
        if(stage==Stage.READY_TO_SHARE||stage==Stage.RETENTION_READY)return publishedValid?Recovery.READ_PUBLISHED:Recovery.TERMINAL;
        if(stage==Stage.CANCELLED)return Recovery.TERMINAL;
        if(stage==Stage.PAUSED)return Recovery.WAIT_FOR_KICK;
        if(stage==Stage.VERIFYING&&finishedManifest)return Recovery.VERIFY_AND_PUBLISH; // raw temporaries may already have been safely deleted.
        if(stage==Stage.FAILED)return Recovery.TERMINAL;
        if(!snapshotValid||stage==Stage.CLAIMED||stage==Stage.BACKUP)return Recovery.BACKUP;
        if(stage==Stage.SNAPSHOT_DURABLE||stage==Stage.INDEXING)return Recovery.REBUILD_INDEX;
        return Recovery.RECOMPRESS;
    }
    public static boolean allowed(Stage from,Stage to) {
        if(from==to)return true;if(from==Stage.READY_TO_SHARE||from==Stage.RETENTION_READY||from==Stage.CANCELLED)return false;
        if(to==Stage.FAILED||to==Stage.CANCELLED||to==Stage.PAUSED)return true;
        if(to==Stage.BACKUP)return from!=Stage.READY_TO_SHARE;
        if(from==Stage.PAUSED||from==Stage.FAILED)return to==Stage.INDEXING||to==Stage.COMPRESSING||to==Stage.VERIFYING||to==Stage.RETENTION_ENCODING;
        switch(from){case CLAIMED:return to==Stage.BACKUP;case BACKUP:return to==Stage.SNAPSHOT_DURABLE;
            case SNAPSHOT_DURABLE:return to==Stage.INDEXING||to==Stage.RETENTION_ENCODING;case INDEXING:return to==Stage.INDEX_DURABLE;
            case INDEX_DURABLE:return to==Stage.COMPRESSING||to==Stage.INDEXING;case COMPRESSING:return to==Stage.VERIFYING||to==Stage.INDEXING;
            case RETENTION_ENCODING:return to==Stage.RETENTION_RECEIPT_DURABLE;case RETENTION_RECEIPT_DURABLE:return to==Stage.RETENTION_READY||to==Stage.RETENTION_ENCODING;
            case VERIFYING:return to==Stage.READY_TO_SHARE||to==Stage.COMPRESSING||to==Stage.INDEXING;default:return false;}
    }
    private void check()throws IOException{if(closed||!lease.isValid())throw new IOException("REPORT_LEASE_NOT_OWNED");}
    public void close()throws IOException{if(!closed){closed=true;try{lease.release();}finally{lock.close();}}}
    public static String readAtomic(File file,int maximum)throws IOException {
        try(InputStream in=new AtomicFile(file).openRead()){ByteArrayOutputStream out=new ByteArrayOutputStream(Math.min(4096,maximum));byte[] b=new byte[4096];int n;
            while((n=in.read(b))!=-1){if(n>maximum-out.size())throw new IOException("REPORT_JSON_BOUND");out.write(b,0,n);}return new String(out.toByteArray(),StandardCharsets.UTF_8);}
    }
    public static void writeAtomic(File file,String json)throws IOException {
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_JSON)throw new IOException("REPORT_JSON_BOUND");File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("REPORT_JSON_PARENT");
        if(!file.getCanonicalFile().equals(file.getAbsoluteFile()))throw new IOException("REPORT_JSON_PATH");AtomicFile atomic=new AtomicFile(file);FileOutputStream out=null;
        try{out=atomic.startWrite();out.write(bytes);out.flush();out.getFD().sync();atomic.finishWrite(out);out=null;}finally{if(out!=null)atomic.failWrite(out);}
    }
    /** Full observations never prevent an export merely because a bounded request JSON is small. */
    public static void putObservation(File root,String requestId,JSONObject observation,String key,String value)throws Exception {
        hash(requestId);
        if(key==null||!key.matches("[A-Za-z][A-Za-z0-9]{0,63}"))throw new IOException("REPORT_OBSERVATION_KEY");
        if(value==null){observation.put(key,"OBSERVATION_UNAVAILABLE");return;}
        // Bound inline fields collectively; longer originals are immutable report members.
        if(value.length()<=1024&&value.getBytes(StandardCharsets.UTF_8).length<=4096){observation.put(key,value);return;}
        File file=ReportArchiveVerifierCP12.inside(root,"observations/"+requestId+"/"+key+".txt");
        File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("REPORT_OBSERVATION_PARENT");
        AtomicFile atomic=new AtomicFile(file);FileOutputStream fd=atomic.startWrite();
        try{Writer writer=new BufferedWriter(new OutputStreamWriter(fd,StandardCharsets.UTF_8),8192);writer.write(value);writer.flush();fd.getFD().sync();atomic.finishWrite(fd);fd=null;}finally{if(fd!=null)atomic.failWrite(fd);}
        JSONObject ref=new JSONObject();ref.put("key",key);ref.put("path","observations/"+requestId+"/"+key+".txt");ref.put("bytes",file.length());ref.put("sha256",ReportArchiveCP4.hash(file,()->true));
        org.json.JSONArray refs=observation.optJSONArray("fullObservations");if(refs==null){refs=new org.json.JSONArray();observation.put("fullObservations",refs);}refs.put(ref);
        observation.put(key,"FULL_OBSERVATION_MEMBER:"+key+"|bytes="+file.length()+"|sha256="+ref.getString("sha256"));
    }
    /** Optional observers may fail without preventing the immutable DB snapshot or other observations. */
    public static void collectObservation(File root,String requestId,JSONObject observation,String key,
            java.util.concurrent.Callable<String> observer)throws Exception {
        String value;
        try { value=observer.call(); }
        catch(Exception | LinkageError unavailable) {
            observation.put(key,"OBSERVATION_READ_ERROR");
            StringWriter original=new StringWriter();PrintWriter writer=new PrintWriter(original);
            unavailable.printStackTrace(writer);writer.flush();
            // Preserve the cause in a separate immutable member; never turn the failed observer into PASS.
            putObservation(root,requestId,observation,key+"Error",original.toString());
            return;
        }
        // Disk/identity/publication failures are still fatal: no successful receipt without durable evidence.
        putObservation(root,requestId,observation,key,value);
    }
    /** Recovery evidence remains shareable even if SQLite, JNI or a report check cannot run. */
    public static JSONObject writeFailureArtifact(File root,String requestId,Throwable error)throws Exception {
        return writeFailureArtifact(root,requestId,error,null);
    }
    public static JSONObject writeFailureArtifact(File root,String requestId,Throwable error,Job job)throws Exception {
        hash(requestId);
        File snapshot=null;
        if(job!=null) {
            if(!requestId.equals(job.requestId)||!"FULL_REPORT".equals(job.mode))throw new IOException("REPORT_RECOVERY_JOB_IDENTITY");
            hash(job.jobId);File candidate=ReportArchiveVerifierCP12.inside(root,"jobs/"+job.jobId+"/snapshot.db");
            if(!job.snapshotSha.isEmpty()&&candidate.isFile()&&candidate.length()==job.snapshotBytes
                    &&job.snapshotSha.equals(ReportArchiveCP4.hash(candidate,()->true)))snapshot=candidate;
        }
        // R13-P34: a recovery package never carries the whole database unless raw evidence was requested and it is small
        if(snapshot!=null&&!ReportBudgetSU2.recoveryMayIncludeDatabase(ReportArchiveVerifierCP12.inside(root,"requests/"+requestId+".json"),snapshot.length()))snapshot=null;
        File directory=ReportArchiveVerifierCP12.inside(root,"recovery/"+requestId);
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("REPORT_RECOVERY_DIRECTORY");
        File partial=File.createTempFile("recovery-",".partial",directory);
        JSONObject boundary=new JSONObject();boundary.put("scope","EXPORT_FAILURE_RECOVERY_ONLY");
        boundary.put("requestId",requestId);boundary.put("evidenceComplete",false);
        boundary.put("checksComplete",false);boundary.put("allChecksPassed",false);
        boundary.put("databaseIncluded",snapshot!=null);boundary.put("reason","Full report failed; available request observations, original exception and any identity-verified durable snapshot are retained. Diagnostic checks are incomplete.");
        if(snapshot!=null){boundary.put("snapshotSha256",job.snapshotSha);boundary.put("snapshotBytes",job.snapshotBytes);}
        try {
            try(FileOutputStream fd=new FileOutputStream(partial);
                java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(fd)) {
                zip.putNextEntry(new java.util.zip.ZipEntry("EVIDENCE_BOUNDARY.json"));
                zip.write(boundary.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
                zip.putNextEntry(new java.util.zip.ZipEntry("original-failure.txt"));
                PrintWriter stack=new PrintWriter(new OutputStreamWriter(zip,StandardCharsets.UTF_8));
                error.printStackTrace(stack);stack.flush();if(stack.checkError())throw new IOException("REPORT_RECOVERY_STACK_WRITE");zip.closeEntry();
                File request=ReportArchiveVerifierCP12.inside(root,"requests/"+requestId+".json");
                if(request.isFile())copyRecoveryMember(zip,request,"request.json");
                if(snapshot!=null)copyRecoveryMember(zip,snapshot,"snapshot.db");
                File observations=ReportArchiveVerifierCP12.inside(root,"observations/"+requestId);
                File[] originals=observations.listFiles();
                if(originals!=null)for(File file:originals){
                    if(!file.getName().matches("[A-Za-z][A-Za-z0-9]{0,63}\\.txt"))throw new IOException("REPORT_RECOVERY_OBSERVATION_PATH");
                    copyRecoveryMember(zip,ReportArchiveVerifierCP12.inside(observations,file.getName()),"observations/"+file.getName());
                }
                zip.finish();zip.flush();fd.getFD().sync();
            }
            // Reopen every member: a partial ZIP is never offered to the provider.
            try(java.util.zip.ZipFile read=new java.util.zip.ZipFile(partial)){
                byte[] buffer=new byte[32768];Enumeration<? extends java.util.zip.ZipEntry> entries=read.entries();
                while(entries.hasMoreElements()){java.util.zip.ZipEntry entry=entries.nextElement();java.util.zip.CRC32 crc=new java.util.zip.CRC32();java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");long bytes=0;try(InputStream in=read.getInputStream(entry)){int n;while((n=in.read(buffer))!=-1){crc.update(buffer,0,n);digest.update(buffer,0,n);bytes=Math.addExact(bytes,n);}}if(bytes!=entry.getSize()||crc.getValue()!=entry.getCrc())throw new IOException("REPORT_RECOVERY_MEMBER_CRC");if(entry.getName().equals("snapshot.db")&&(job==null||bytes!=job.snapshotBytes||!job.snapshotSha.equals(ReportArchiveCP4.hex(digest.digest()))))throw new IOException("REPORT_RECOVERY_SNAPSHOT_HASH");}
            }
            String sha=ReportArchiveCP4.hash(partial,()->true);File archive=new File(directory,sha+".zip");
            if(archive.exists()){if(archive.length()!=partial.length()||!sha.equals(ReportArchiveCP4.hash(archive,()->true)))throw new IOException("REPORT_RECOVERY_HASH_CONFLICT");if(!partial.delete())throw new IOException("REPORT_RECOVERY_TEMP_REMOVE");}
            else if(!partial.renameTo(archive))throw new IOException("REPORT_RECOVERY_PUBLISH");
            JSONObject receipt=new JSONObject();receipt.put("sha256",sha);receipt.put("bytes",archive.length());receipt.put("scope","EXPORT_FAILURE_RECOVERY_ONLY");
            receipt.put("databaseIncluded",snapshot!=null);
            writeAtomic(new File(directory,"receipt.json"),receipt.toString());return receipt;
        }finally{if(partial.exists()&&!partial.delete())throw new IOException("REPORT_RECOVERY_TEMP_REMOVE");}
    }
    private static void copyRecoveryMember(java.util.zip.ZipOutputStream zip,File source,String name)throws IOException {
        zip.putNextEntry(new java.util.zip.ZipEntry(name));try(InputStream in=new FileInputStream(source)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1)zip.write(buffer,0,n);}zip.closeEntry();
    }
    public static void addRecoveryReceipt(File root,String requestId,JSONObject ack)throws Exception {
        hash(requestId);File receipt=ReportArchiveVerifierCP12.inside(root,"recovery/"+requestId+"/receipt.json");
        if(receipt.isFile())ack.put("recovery",new JSONObject(readAtomic(receipt,4096)));
    }
    private static void hash(String s){if(s==null||!s.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("REPORT_HASH");}
}
