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
    public static final int MAX_JSON=65536,MAX_FULL_OBSERVATIONS_R31=24;
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
            File[] commands=DirectoryCursorR31.children(directory,1024);
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
        public long acknowledgedRevision=-1L;
        public String semanticVerificationR31="LEGACY_NO_SEMANTIC_RECEIPT";
        public Job(String id,String request,String release,String mode,long now) {
            hash(id);hash(request);hash(release);if(!"FULL_REPORT".equals(mode)&&!"RETENTION_PAGE".equals(mode))throw new IllegalArgumentException("REPORT_MODE");
            jobId=id;requestId=request;releaseHash=release;this.mode=mode;createdWallMs=updatedWallMs=now;
        }
        JSONObject json()throws Exception {
            JSONObject o=new JSONObject();o.put("revision_name","REPORT_JOB_CP12_1");o.put("jobId",jobId);o.put("requestId",requestId);o.put("releaseHash",releaseHash);o.put("mode",mode);
            o.put("stage",stage);o.put("resumeStage",resumeStage);o.put("lastReason",lastReason);o.put("snapshotSha",snapshotSha);o.put("publishedDir",publishedDir);o.put("manifestSha",manifestSha);
            o.put("revision",revision);o.put("attempt",attempt);o.put("createdWallMs",createdWallMs);o.put("updatedWallMs",updatedWallMs);o.put("snapshotWallMs",snapshotWallMs);o.put("snapshotBytes",snapshotBytes);
            o.put("temporaryReservationBytes",temporaryReservationBytes);o.put("publishedQuotaBytes",publishedQuotaBytes);o.put("compressionAttempt",compressionAttempt);o.put("preemptions",preemptions);o.put("failureAttempts",failureAttempts);
            o.put("evidenceComplete",evidenceComplete);o.put("checksComplete",checksComplete);o.put("allChecksPassed",allChecksPassed);o.put("acknowledgedRevision",acknowledgedRevision);o.put("semanticVerificationR31",semanticVerificationR31);return o;
        }
        public static Job parse(String json)throws Exception {
          try {
            JSONObject o=new JSONObject(json);if(!"REPORT_JOB_CP12_1".equals(o.getString("revision_name")))throw new IOException("REPORT_JOB_REVISION");
            Job j=new Job(o.getString("jobId"),o.getString("requestId"),o.getString("releaseHash"),o.getString("mode"),o.getLong("createdWallMs"));
            j.stage=o.getString("stage");j.resumeStage=o.getString("resumeStage");Stage.valueOf(j.stage);Stage.valueOf(j.resumeStage);
            j.lastReason=o.getString("lastReason");j.snapshotSha=o.getString("snapshotSha");j.publishedDir=o.getString("publishedDir");j.manifestSha=o.getString("manifestSha");
            if(j.lastReason.length()>512||(!j.snapshotSha.isEmpty()&&!j.snapshotSha.matches("[0-9a-f]{64}"))||(!j.manifestSha.isEmpty()&&!j.manifestSha.matches("[0-9a-f]{64}"))
               ||(!j.publishedDir.isEmpty()&&!j.publishedDir.matches("archive-[0-9]{1,9}")))throw new IOException("REPORT_JOB_FIELD");
            j.revision=o.getLong("revision");j.attempt=o.getLong("attempt");j.updatedWallMs=o.getLong("updatedWallMs");j.snapshotWallMs=o.getLong("snapshotWallMs");j.snapshotBytes=o.getLong("snapshotBytes");
            j.temporaryReservationBytes=o.getLong("temporaryReservationBytes");j.publishedQuotaBytes=o.getLong("publishedQuotaBytes");j.compressionAttempt=o.getInt("compressionAttempt");j.preemptions=o.getInt("preemptions");j.failureAttempts=o.optInt("failureAttempts",0);
            j.evidenceComplete=o.getBoolean("evidenceComplete");j.checksComplete=o.getBoolean("checksComplete");j.allChecksPassed=o.getBoolean("allChecksPassed");
            j.acknowledgedRevision=o.optLong("acknowledgedRevision",-1L);
            j.semanticVerificationR31=o.optString("semanticVerificationR31","LEGACY_NO_SEMANTIC_RECEIPT");
            if(!java.util.Arrays.asList("LEGACY_NO_SEMANTIC_RECEIPT","REQUIRES_ZIP_READBACK","ZIP_READBACK_VERIFIED").contains(j.semanticVerificationR31))throw new IOException("REPORT_JOB_SEMANTIC_STATE");
            if(j.revision<0||j.attempt<0||j.compressionAttempt<0||j.preemptions<0||j.failureAttempts<0||j.snapshotBytes<0||j.temporaryReservationBytes<0||j.publishedQuotaBytes<0)throw new IOException("REPORT_JOB_RANGE");
            return j;
          } catch(IllegalArgumentException malformed) {
            // Journal identities and enum names are persisted input. A damaged
            // unrelated entry must not hide another valid immutable report.
            throw new IOException("REPORT_JOB_FORMAT",malformed);
          }
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
    public Job current()throws Exception {
        check();File current=new File(root,"current-job.json");if(!current.exists())return null;
        String original=readAtomic(current,4096);
        try {
            JSONObject pointer=new JSONObject(original);String id=pointer.getString("jobId");hash(id);
            Job job=Job.parse(readAtomic(ReportArchiveVerifierCP12.inside(root,"jobs/"+id+"/job.json"),MAX_JSON));
            if(!id.equals(job.jobId))throw new IOException("REPORT_JOB_POINTER");return job;
        } catch(Exception malformed) {
            String quarantine=StorageBudgetR31.sha((original+"|"+UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));
            // Preserve the exact pointer; the suspect job directory is untouched.
            writeAtomic(new File(root,"quarantine/"+quarantine+".json"),new JSONObject().put("originalPointer",original).put("reason",malformed.getClass().getSimpleName()+":"+String.valueOf(malformed.getMessage())).toString());
            new AtomicFile(current).delete();if(current.exists())throw new IOException("REPORT_ORPHAN_POINTER_QUARANTINE_PENDING",malformed);
            android.util.Log.w("ReportJournalCP12","Orphan current pointer quarantined; other durable requests can continue",malformed);return null;
        }
    }
    /** Read-only UI recovery lookup: retention jobs cannot hide the latest full export. */
    public static Job latestFullReport(File root)throws Exception {
        File jobs=ReportArchiveVerifierCP12.inside(root,"jobs");File[] directories=DirectoryCursorR31.children(jobs,1024);
        if(directories==null)return null;if(directories.length>1024)throw new IOException("REPORT_RECOVERY_JOB_LIMIT");
        Job latest=null;
        for(File directory:directories){
          try {
            if(!directory.getName().matches("[0-9a-f]{64}"))throw new IOException("REPORT_RECOVERY_JOB_PATH");
            File journal=ReportArchiveVerifierCP12.inside(root,"jobs/"+directory.getName()+"/job.json");
            Job job=Job.parse(readAtomic(journal,MAX_JSON));
            if(!directory.getName().equals(job.jobId))throw new IOException("REPORT_RECOVERY_JOB_IDENTITY");
            if(job.mode.equals("FULL_REPORT")&&(latest==null||job.createdWallMs>latest.createdWallMs
                    ||job.createdWallMs==latest.createdWallMs&&job.jobId.compareTo(latest.jobId)>0))latest=job;
          } catch(Exception invalid) { android.util.Log.w("ReportJournalCP12","Unrelated orphan or damaged journal preserved: "+directory.getName(),invalid); }
        }
        return latest;
    }
    public void create(Job j)throws Exception {check();File d=directory(j);File journal=new File(d,"job.json");if(journal.exists())throw new IOException("REPORT_JOB_EXISTS");
        writeAtomic(journal,j.json().toString());JSONObject pointer=new JSONObject();pointer.put("jobId",j.jobId);pointer.put("requestId",j.requestId);writeAtomic(new File(root,"current-job.json"),pointer.toString());}
    public void transition(Job j,Stage next,long now)throws Exception {check();Stage prior=Stage.valueOf(j.stage);if(!allowed(prior,next))throw new IOException("REPORT_STAGE_TRANSITION|"+prior+"|"+next);
        // Persist a detached copy first. The in-memory state must not claim success if fsync fails.
        Job copy=Job.parse(j.json().toString());copy.stage=next.name();copy.updatedWallMs=now;copy.revision=Math.addExact(copy.revision,1);
        writeAtomic(new File(directory(j),"job.json"),copy.json().toString());j.stage=copy.stage;j.updatedWallMs=copy.updatedWallMs;j.revision=copy.revision;
        try{acknowledge(j.requestId,j);}catch(IOException retry){android.util.Log.w("ReportJournalCP12","Durable ACK outbox pending for "+j.requestId+"@"+j.revision,retry);}
    }
    public void checkpoint(Job j,long now)throws Exception {check();Job copy=Job.parse(j.json().toString());copy.updatedWallMs=now;copy.revision=Math.addExact(copy.revision,1);
        writeAtomic(new File(directory(j),"job.json"),copy.json().toString());j.updatedWallMs=copy.updatedWallMs;j.revision=copy.revision;}
    /** Job revision is the outbox sequence. Persisting READY never depends on ACK I/O. */
    public void acknowledge(String requestId,Job j)throws Exception {
        check();hash(requestId);if(!j.requestId.equals(requestId))throw new IOException("REPORT_ACK_IDENTITY");
        File target=new File(root,"requests/"+requestId+".ack.json");
        if(target.exists()) {
            String original=readAtomic(target,8192);
            try {
                JSONObject prior=new JSONObject(original);
                if(!requestId.equals(prior.getString("requestId"))||!j.jobId.equals(prior.getString("jobId")))throw new org.json.JSONException("ACK_IDENTITY");
                if(prior.optLong("revision",-1)>j.revision)return; // never regress an observer
            }catch(org.json.JSONException corrupt){
                String id=StorageBudgetR31.sha((target.getName()+"|"+original).getBytes(StandardCharsets.UTF_8));
                writeAtomic(new File(root,"quarantine/"+id+".json"),new JSONObject().put("originalAck",original).put("reason","ACK_INVALID:"+corrupt.getMessage()).toString());
            }
        }
        JSONObject ack=new JSONObject();ack.put("requestId",requestId);ack.put("jobId",j.jobId);ack.put("mode",j.mode);ack.put("stage",j.stage);ack.put("revision",j.revision);ack.put("reason",j.lastReason);ack.put("evidenceComplete",j.evidenceComplete);ack.put("checksComplete",j.checksComplete);ack.put("allChecksPassed",j.allChecksPassed);
        ack.put("semanticVerificationR31",j.semanticVerificationR31);
        addRecoveryReceipt(root,requestId,ack);writeAtomic(target,ack.toString());
        // Crash after ACK and before this receipt only causes an idempotent resend.
        j.acknowledgedRevision=j.revision;writeAtomic(new File(directory(j),"job.json"),j.json().toString());
    }
    public static Job findRequestR31(File root,String request)throws Exception {
        hash(request);File jobs=new File(root,"jobs");if(!jobs.isDirectory())return null;
        Job found=null;boolean conflict=false;
        for(File directory:DirectoryCursorR31.children(jobs,1024)) {
            try {
                Job job=Job.parse(readAtomic(new File(directory,"job.json"),MAX_JSON));
                if(!directory.getName().equals(job.jobId))throw new IOException("REPORT_JOB_IDENTITY");
                if(request.equals(job.requestId)) {
                    if(found!=null&&!found.jobId.equals(job.jobId))conflict=true;
                    found=job;
                }
            }catch(IOException|org.json.JSONException damaged){android.util.Log.w("ReportJournalCP12","Orphan journal preserved: "+directory.getName(),damaged);}
        }
        if(conflict)throw new IOException("REPORT_DUPLICATE_REQUEST_JOBS");return found;
    }
    public Job recoverOrCreateR31(String request,String release,String mode)throws Exception {
        check();Job existing=findRequestR31(root,request);
        if(existing==null){existing=new Job(request,request,release,mode,System.currentTimeMillis());create(existing);return existing;}
        if(!existing.releaseHash.equals(release)||!existing.mode.equals(mode))throw new IOException("REPORT_RECOVERY_IDENTITY");
        JSONObject pointer=new JSONObject();pointer.put("jobId",existing.jobId);pointer.put("requestId",request);writeAtomic(new File(root,"current-job.json"),pointer.toString());return existing;
    }
    public void reconcileAcknowledgementsR31()throws Exception {
        check();File jobs=new File(root,"jobs");if(!jobs.isDirectory())return;
        for(File directory:DirectoryCursorR31.children(jobs,1024))try {
            Job job=Job.parse(readAtomic(new File(directory,"job.json"),MAX_JSON));
            if(!directory.getName().equals(job.jobId))throw new IOException("REPORT_JOB_IDENTITY");
            if(needsAcknowledgementR31(job))acknowledge(job.requestId,job);
        }catch(Exception retry){android.util.Log.w("ReportJournalCP12","ACK reconciliation pending: "+directory.getName(),retry);}
    }
    private boolean needsAcknowledgementR31(Job job)throws Exception {
        if(job.acknowledgedRevision<job.revision)return true;
        File ack=new File(root,"requests/"+job.requestId+".ack.json");if(!ack.exists())return true;
        try{JSONObject row=new JSONObject(readAtomic(ack,8192));return !job.requestId.equals(row.optString("requestId"))||!job.jobId.equals(row.optString("jobId"))||row.optLong("revision",-1)!=job.revision||!job.stage.equals(row.optString("stage"));}
        catch(org.json.JSONException malformed){return true;}
    }
    /** Read-only reattachment to the authoritative job also recovers a missing ACK.
     * Consumers still verify published.json, manifest and ZIP before opening it. */
    public static JSONObject authoritativeStateR31(File root,String request)throws Exception {
        Job job=findRequestR31(root,request);if(job==null)return null;
        JSONObject value=new JSONObject();value.put("requestId",request);value.put("jobId",job.jobId);value.put("mode",job.mode);value.put("stage",job.stage);value.put("revision",job.revision);value.put("reason",job.lastReason);value.put("evidenceComplete",job.evidenceComplete);value.put("checksComplete",job.checksComplete);value.put("allChecksPassed",job.allChecksPassed);value.put("semanticVerificationR31",job.semanticVerificationR31);
        // The UI prefers this authority over ACK. Preserve the independently
        // durable recovery receipt even when both ACK writes were lost.
        if(Stage.FAILED.name().equals(job.stage)) {
            try { addRecoveryReceipt(root,request,value); }
            catch(IOException|org.json.JSONException damaged) {
                // A damaged optional recovery record must not hide the job's
                // original failure or turn reattachment into a polling loop.
                value.put("recoveryFailure",damaged.getClass().getSimpleName()+":"+String.valueOf(damaged.getMessage()));
            }
        }
        return value;
    }
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
        if(!file.getCanonicalFile().equals(file.getAbsoluteFile()))throw new IOException("REPORT_JSON_PATH");
        try(ReportIoR31.AtomicPermit permit=ReportIoR31.beforeAtomic(file,bytes.length)){
            AtomicFile atomic=new AtomicFile(file);FileOutputStream out=null;
            try{out=atomic.startWrite();OutputStream admitted=permit.stream(out);admitted.write(bytes);admitted.flush();out.getFD().sync();atomic.finishWrite(out);out=null;}finally{if(out!=null)atomic.failWrite(out);}
        }
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
        org.json.JSONArray existing=observation.optJSONArray("fullObservations");if(existing!=null&&existing.length()>=MAX_FULL_OBSERVATIONS_R31)throw new IOException("REPORT_OBSERVATION_REFERENCE_BOUND");
        try(ReportIoR31.AtomicPermit permit=ReportIoR31.beforeAtomic(file,ReportIoR31.utf8Bytes(value))){
            AtomicFile atomic=new AtomicFile(file);FileOutputStream fd=atomic.startWrite();
            try{Writer writer=new BufferedWriter(new OutputStreamWriter(permit.stream(fd),StandardCharsets.UTF_8),8192);writer.write(value);writer.flush();fd.getFD().sync();atomic.finishWrite(fd);fd=null;}finally{if(fd!=null)atomic.failWrite(fd);}
        }
        JSONObject ref=new JSONObject();ref.put("key",key);ref.put("path","observations/"+requestId+"/"+key+".txt");ref.put("bytes",file.length());ref.put("sha256",ReportArchiveCP4.hash(file,()->true));
        org.json.JSONArray refs=observation.optJSONArray("fullObservations");if(refs==null){refs=new org.json.JSONArray();observation.put("fullObservations",refs);}refs.put(ref);
        observation.put(key,"FULL_OBSERVATION_MEMBER:"+key+"|bytes="+file.length()+"|sha256="+ref.getString("sha256"));
    }
    private static final long OBSERVATION_TIMEOUT_MS_R31Q=5000L;
    // A timed-out SQLite observer may ignore interruption. It still occupies its
    // slot until it really exits; repeated exports must not create new threads.
    private static final java.util.concurrent.ExecutorService OBSERVERS_R31Q=new java.util.concurrent.ThreadPoolExecutor(
            0,2,30L,java.util.concurrent.TimeUnit.SECONDS,new java.util.concurrent.SynchronousQueue<Runnable>(),
            r->{Thread t=new Thread(r,"report-observer-r31q");t.setDaemon(true);return t;},new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
    /** Optional observers may fail without preventing the immutable DB snapshot or other observations. */
    public static void collectObservation(File root,String requestId,JSONObject observation,String key,
            java.util.concurrent.Callable<String> observer)throws Exception {
        String value;
        // R31Q: an observer that blocks (on a lock held by a stalled business thread) must not hold the
        // report request forever. After the limit the observation records where its reader is blocked.
        final java.util.concurrent.atomic.AtomicReference<Thread> reader=new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.Future<String> pending;
        try{pending=OBSERVERS_R31Q.submit(()->{reader.set(Thread.currentThread());return observer.call();});}
        catch(java.util.concurrent.RejectedExecutionException full){
            observation.put(key,"OBSERVATION_CAPACITY_WAIT");
            observation.put(key+"Error","Two optional observers are still active; not sampled. No unbounded worker or queue was created.");
            return;
        }
        try { value=pending.get(OBSERVATION_TIMEOUT_MS_R31Q,java.util.concurrent.TimeUnit.MILLISECONDS); }
        catch(InterruptedException cancelled){pending.cancel(true);Thread.currentThread().interrupt();throw new IOException("REPORT_OBSERVATION_CANCELLED",cancelled);}
        catch(java.util.concurrent.TimeoutException blocked) {
            pending.cancel(true);
            observation.put(key,"OBSERVATION_TIMEOUT");
            StringBuilder frames=new StringBuilder("OBSERVATION_TIMEOUT_AFTER_MS="+OBSERVATION_TIMEOUT_MS_R31Q+"\n");
            Thread t=reader.get();
            if(t!=null){frames.append(t.getName()).append(' ').append(t.getState()).append('\n');for(StackTraceElement e:t.getStackTrace())frames.append("  at ").append(e).append('\n');}
            putObservation(root,requestId,observation,key+"Error",frames.toString());
            return;
        }
        catch(java.util.concurrent.ExecutionException failed) {
            Throwable cause=failed.getCause()==null?failed:failed.getCause();
            if(cause instanceof Error&&!(cause instanceof LinkageError))throw (Error)cause;
            observation.put(key,"OBSERVATION_READ_ERROR");
            StringWriter original=new StringWriter();PrintWriter writer=new PrintWriter(original);
            cause.printStackTrace(writer);writer.flush();
            putObservation(root,requestId,observation,key+"Error",original.toString());
            return;
        }
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
    public static JSONObject writeFailureArtifact(File root,String requestId,Throwable error,Job job)throws Exception{return writeFailureArtifact(root,requestId,error,job,()->false);}
    public static JSONObject writeFailureArtifact(File root,String requestId,Throwable error,Job job,java.util.function.BooleanSupplier cancelled)throws Exception {
        java.util.Objects.requireNonNull(cancelled);hash(requestId);
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
        File requestInput=ReportArchiveVerifierCP12.inside(root,"requests/"+requestId+".json"),observations=ReportArchiveVerifierCP12.inside(root,"observations/"+requestId);
        File[] originals=DirectoryCursorR31.children(observations,MAX_FULL_OBSERVATIONS_R31);
        long rawBytes=(snapshot==null?0:snapshot.length())+(requestInput.isFile()?requestInput.length():0);for(File file:originals){if(!file.getName().matches("[A-Za-z][A-Za-z0-9]{0,63}\\.txt"))throw new IOException("REPORT_RECOVERY_OBSERVATION_PATH");rawBytes=Math.addExact(rawBytes,file.length());}
        long recoveryBound=Math.addExact(ReportPhaseBudgetR31.zipUpper(rawBytes,originals.length+4),8L<<20);
        try(ReportIoR31.Artifact admitted=ReportIoR31.reserveArtifact(directory,recoveryBound,cancelled)){
        File partial=File.createTempFile("recovery-",".partial",directory);
        JSONObject boundary=new JSONObject();boundary.put("scope","EXPORT_FAILURE_RECOVERY_ONLY");
        boundary.put("requestId",requestId);boundary.put("evidenceComplete",false);
        boundary.put("checksComplete",false);boundary.put("allChecksPassed",false);
        boundary.put("databaseIncluded",snapshot!=null);boundary.put("reason","Full report failed; available request observations, original exception and any identity-verified durable snapshot are retained. Diagnostic checks are incomplete.");
        if(snapshot!=null){boundary.put("snapshotSha256",job.snapshotSha);boundary.put("snapshotBytes",job.snapshotBytes);}
        try {
            try(FileOutputStream fd=new FileOutputStream(partial);
                java.util.zip.ZipOutputStream zip=new java.util.zip.ZipOutputStream(new BufferedOutputStream(ReportIoR31.protect(partial,fd,admitted),32768))) {
                admitted.checkpoint();zip.putNextEntry(new java.util.zip.ZipEntry("EVIDENCE_BOUNDARY.json"));
                zip.write(boundary.toString().getBytes(StandardCharsets.UTF_8));zip.closeEntry();
                zip.putNextEntry(new java.util.zip.ZipEntry("original-failure.txt"));
                PrintWriter stack=new PrintWriter(new OutputStreamWriter(zip,StandardCharsets.UTF_8));
                error.printStackTrace(stack);stack.flush();if(stack.checkError())throw new IOException("REPORT_RECOVERY_STACK_WRITE");zip.closeEntry();
                File request=ReportArchiveVerifierCP12.inside(root,"requests/"+requestId+".json");
                if(request.isFile())copyRecoveryMember(zip,request,"request.json",admitted);
                if(snapshot!=null)copyRecoveryMember(zip,snapshot,"snapshot.db",admitted);
                for(File file:originals){
                    if(!file.getName().matches("[A-Za-z][A-Za-z0-9]{0,63}\\.txt"))throw new IOException("REPORT_RECOVERY_OBSERVATION_PATH");
                    copyRecoveryMember(zip,ReportArchiveVerifierCP12.inside(observations,file.getName()),"observations/"+file.getName(),admitted);
                }
                zip.finish();zip.flush();fd.getFD().sync();
            }
            // Reopen every member: a partial ZIP is never offered to the provider.
            try(java.util.zip.ZipFile read=new java.util.zip.ZipFile(partial)){
                byte[] buffer=new byte[32768];Enumeration<? extends java.util.zip.ZipEntry> entries=read.entries();
                while(entries.hasMoreElements()){admitted.checkpoint();java.util.zip.ZipEntry entry=entries.nextElement();java.util.zip.CRC32 crc=new java.util.zip.CRC32();java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");long bytes=0;try(InputStream in=read.getInputStream(entry)){int n;while((n=in.read(buffer))!=-1){admitted.checkpoint();crc.update(buffer,0,n);digest.update(buffer,0,n);bytes=Math.addExact(bytes,n);}}if(bytes!=entry.getSize()||crc.getValue()!=entry.getCrc())throw new IOException("REPORT_RECOVERY_MEMBER_CRC");if(entry.getName().equals("snapshot.db")&&(job==null||bytes!=job.snapshotBytes||!job.snapshotSha.equals(ReportArchiveCP4.hex(digest.digest()))))throw new IOException("REPORT_RECOVERY_SNAPSHOT_HASH");}
            }
            admitted.checkpoint();String sha=ReportArchiveCP4.hash(partial,admitted);admitted.checkpoint();File archive=new File(directory,sha+".zip");
            if(archive.exists()){if(archive.length()!=partial.length()||!sha.equals(ReportArchiveCP4.hash(archive,()->true)))throw new IOException("REPORT_RECOVERY_HASH_CONFLICT");if(!partial.delete())throw new IOException("REPORT_RECOVERY_TEMP_REMOVE");}
            else if(!partial.renameTo(archive))throw new IOException("REPORT_RECOVERY_PUBLISH");
            JSONObject receipt=new JSONObject();receipt.put("sha256",sha);receipt.put("bytes",archive.length());receipt.put("scope","EXPORT_FAILURE_RECOVERY_ONLY");
            receipt.put("databaseIncluded",snapshot!=null);
            writeAtomic(new File(directory,"receipt.json"),receipt.toString());return receipt;
        }finally{if(partial.exists()&&!partial.delete())throw new IOException("REPORT_RECOVERY_TEMP_REMOVE");}
        }
    }
    private static void copyRecoveryMember(java.util.zip.ZipOutputStream zip,File source,String name,ReportIoR31.Artifact admitted)throws IOException {
        admitted.checkpoint();zip.putNextEntry(new java.util.zip.ZipEntry(name));try(InputStream in=new FileInputStream(source)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){admitted.checkpoint();zip.write(buffer,0,n);}}zip.closeEntry();
    }
    public static void addRecoveryReceipt(File root,String requestId,JSONObject ack)throws Exception {
        hash(requestId);File receipt=ReportArchiveVerifierCP12.inside(root,"recovery/"+requestId+"/receipt.json");
        if(receipt.isFile())ack.put("recovery",new JSONObject(readAtomic(receipt,4096)));
    }
    private static void hash(String s){if(s==null||!s.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("REPORT_HASH");}
}
