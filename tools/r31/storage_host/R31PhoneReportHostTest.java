package com.ethk3.novaonepro;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.zip.*;
import org.json.*;

/** Actual Service callbacks, OS-backed admission, JNI backup and ACK recovery.
 * Android context/time/logging and stat are explicitly adapted for HOST only. */
public final class R31PhoneReportHostTest {
    static int checks,chunks,finalCallbacks;static final long MiB=1L<<20;
    static void need(boolean yes,String why){checks++;if(!yes)throw new AssertionError(why);}
    static String id(int n){return String.format(Locale.ROOT,"%064x",n);}
    static Object get(Object value,String name)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    static void set(Object value,String name,Object field)throws Exception{Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);f.set(value,field);}
    static void enter(Object value,String stage)throws Exception{Method m=value.getClass().getDeclaredMethod("enter",String.class,long.class);m.setAccessible(true);try{m.invoke(value,stage,120000L);}catch(InvocationTargetException e){throw (Exception)e.getCause();}}
    static void release(Object value)throws Exception{Method m=value.getClass().getDeclaredMethod("releaseResourcesR31");m.setAccessible(true);m.invoke(value);}
    static Object active(ReportJobJournalCP12.Job job,File root)throws Exception{
        Class<?> u=Class.forName("sun.misc.Unsafe");Field f=u.getDeclaredField("theUnsafe");f.setAccessible(true);Object unsafe=f.get(null);
        Object service=u.getMethod("allocateInstance",Class.class).invoke(unsafe,ReportExportServiceCP4.class);set(service,"destroyed",new AtomicBoolean());
        Class<?> type=Class.forName(ReportExportServiceCP4.class.getName()+"$ActiveControl");Constructor<?> c=type.getDeclaredConstructor(ReportExportServiceCP4.class,ReportJobJournalCP12.Job.class,File.class);c.setAccessible(true);return c.newInstance(service,job,root);
    }
    public static final class Stat {public long st_blocks;Stat(long blocks){st_blocks=blocks;}}
    public static Stat stat(String path)throws Exception{
        Process p=new ProcessBuilder("stat","--format=%b",path).start();String text=new String(p.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.US_ASCII).trim();if(p.waitFor()!=0)throw new IOException("HOST_STAT_FAILED");return new Stat(Long.parseLong(text));
    }
    static void installHostStat()throws Exception{
        // Do not change product allocation semantics to make a host test pass.
        // Replace only the Android reflection transport in this test JVM with
        // actual Linux stat(st_blocks), never file length or an invented value.
        Class<?> type=Class.forName(StorageCensusSU2.class.getName()+"$Allocated");
        Class<?> u=Class.forName("sun.misc.Unsafe");Field singleton=u.getDeclaredField("theUnsafe");singleton.setAccessible(true);Object unsafe=singleton.get(null);
        for(String name:new String[]{"STAT","BLOCKS"}){Field target=type.getDeclaredField(name);Object base=u.getMethod("staticFieldBase",Field.class).invoke(unsafe,target);long offset=(long)u.getMethod("staticFieldOffset",Field.class).invoke(unsafe,target);
            Object replacement=name.equals("STAT")?R31PhoneReportHostTest.class.getMethod("stat",String.class):Stat.class.getField("st_blocks");u.getMethod("putObjectVolatile",Object.class,long.class,Object.class).invoke(unsafe,base,offset,replacement);}
    }
    static void write(File f,String text)throws Exception{f.getParentFile().mkdirs();Files.writeString(f.toPath(),text);}
    static void recovery(File root,int n,long when)throws Exception{
        String request=id(n);write(new File(root,"requests/"+request+".json"),new JSONObject().put("requestId",request).put("mode","FULL_REPORT").put("createdWallMs",when).toString());
        File directory=new File(root,"recovery/"+request);directory.mkdirs();File archive=new File(directory,"original.zip");
        try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(archive))){
            for(String name:new String[]{"EVIDENCE_BOUNDARY.json","request.json","original-failure.txt"}){zip.putNextEntry(new ZipEntry(name));String text=name.equals("EVIDENCE_BOUNDARY.json")?new JSONObject().put("scope","EXPORT_FAILURE_RECOVERY_ONLY").put("requestId",request).toString():name.equals("request.json")?new JSONObject().put("requestId",request).put("mode","FULL_REPORT").toString():"ORIGINAL_"+n+" API_KEY=phone-test-secret\nAuthorization: Bearer hidden-test-token\nhttps://user:pass@example.org/?token=url-test-secret\n";zip.write(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));zip.closeEntry();}}
        String sha=ReportArchiveCP4.hash(archive);File target=new File(directory,sha+".zip");Files.move(archive.toPath(),target.toPath());
        JSONObject receipt=new JSONObject().put("scope","EXPORT_FAILURE_RECOVERY_ONLY").put("sha256",sha).put("bytes",target.length());write(new File(directory,"receipt.json"),receipt.toString());
        write(new File(root,"requests/"+request+".ack.json"),new JSONObject().put("requestId",request).put("jobId",id(n+100)).put("mode","FULL_REPORT").put("stage","FAILED").put("reason","REPORT_PROGRESS_WRITE_FAILED").put("revision",3).put("recovery",receipt).toString());
    }
    public static void main(String[] args)throws Exception{
        File app=new File(args[0]),files=new File(app,"files"),source=new File(app,"source/"+ReleaseManifestR926.APPLICATION_ID+"/databases/"+AppConfig.DB_NAME);files.mkdirs();source.getParentFile().mkdirs();Files.move(new File(args[1]).toPath(),source.toPath());android.app.Service.filesR31=files;installHostStat();
        StorageBudgetR31 budget=new StorageBudgetR31(app,new StorageBudgetR31.Config(8L<<30,8L<<30,1024,2*MiB,"HOST_PHONE_REPORT_SYNTHETIC"),f->16L<<30);
        ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");File root=ReportDirectoriesCP12.root(files);
        try(Closeable fixture=ReportIoR31.hostFixture(app,budget);ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(files)){
            ReportJobJournalCP12.Job job=journal.recoverOrCreateR31(id(1),id(90),"FULL_REPORT");File directory=journal.directory(job),snapshot=new File(directory,"snapshot.db");
            Object active=active(job,root);ReportNativeCP4.Control actual=(ReportNativeCP4.Control)active;
            StorageBudgetR31.Reservation reservation=budget.reserve("report:"+job.jobId,job.revision,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/r981_reports/jobs/"+job.jobId,2L<<30,8L<<30,true);set(active,"storageReservationR31",reservation);set(active,"backupTargetR31",snapshot);
            try(ReportIoR31.Scope scope=ReportIoR31.bind(directory,actual)){
                try(RandomAccessFile io=new RandomAccessFile(new File(root,"control-budget.lock"),"rw");java.nio.channels.FileLock lock=io.getChannel().lock()){
                    long began=System.nanoTime();enter(active,"BACKUP");actual.onBackupProgress(100,100,0);
                    need((System.nanoTime()-began)/1000000<1000,"OPTIONAL_SAMPLES_WAITED_ON_CONTROL_LOCK");
                    long now=android.os.SystemClock.elapsedRealtime();long[] next=(long[])get(active,"nextProgressR31");need(next[0]>=now+4500&&next[1]>=now+4500,"BACKOFF_NOT_MEASURED_AFTER_FAILED_ATTEMPT");
                }
                Arrays.fill((long[])get(active,"nextProgressR31"),0);
                Files.createDirectory(new File(directory,"stage-progress.json").toPath());Files.createDirectory(new File(directory,"backup-progress.json").toPath());
                enter(active,"BACKUP");journal.transition(job,ReportJobJournalCP12.Stage.BACKUP,System.currentTimeMillis());
                ReportNativeCP4.Control traced=new ReportNativeCP4.Control(){
                    public boolean shouldContinue(){return actual.shouldContinue();}
                    public void beforeBackupChunk(long bytes)throws IOException{chunks++;actual.beforeBackupChunk(bytes);}
                    public void onBackupProgress(long total,long remaining,int code){if(code==101){finalCallbacks++;need(total==165824&&remaining==0,"LARGE_FIXTURE_OR_FINAL_CALLBACK_MISSING");try{((long[])get(active,"nextProgressR31"))[1]=0;}catch(Exception e){throw new RuntimeException(e);}}actual.onBackupProgress(total,remaining,code);}
                };
                ReportNativeCP4.backup(source,snapshot,32*MiB,traced);need(actual.shouldContinue(),"SAMPLE_ABORTED_ACTUAL_BACKUP");need(chunks>1600&&finalCallbacks==1,"LARGE_NATIVE_BACKUP_NOT_EXECUTED");
                long[] failures=(long[])get(active,"progressFailuresR31");need(failures[0]>0&&failures[1]>=2,"BOTH_SAMPLE_FAILURES_AND_FINAL_CALLBACK_REQUIRED");long count=failures[1];for(int i=0;i<100;i++)actual.onBackupProgress(165824,0,101);need(failures[1]==count,"SAMPLE_FAILURE_RETRY_STORM");
                enter(active,"SNAPSHOT_HASH");job.snapshotSha=ReportArchiveCP4.hash(snapshot,actual);job.snapshotBytes=snapshot.length();journal.transition(job,ReportJobJournalCP12.Stage.SNAPSHOT_DURABLE,System.currentTimeMillis());
                File content=new File(directory,"after-backup.json");try(OutputStream out=ReportIoR31.protect(content,new FileOutputStream(content),actual)){out.write("{\"afterFailedSamples\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
                need(content.length()>0&&ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(directory,"job.json"),65536)).stage.equals("SNAPSHOT_DURABLE"),"CONTENT_OR_AUTHORITATIVE_LOG_NOT_DURABLE");
                // Close the real grant, then invoke the production final refund.
                reservation.close();set(active,"backupChargeR31",8192L);actual.onBackupProgress(165824,0,101);
                need("REPORT_STORAGE_BUDGET_RECONCILIATION_FAILED".equals(get(active,"stopReason")),"REFUND_ERROR_OVERWRITTEN_BY_PROGRESS");Throwable first=(Throwable)get(active,"stopCauseR31");need(first.getMessage().contains("OWNER_CLOSED"),"ORIGINAL_REFUND_CAUSE_LOST");
                try{enter(active,"SNAPSHOT_HASH");throw new AssertionError("FINAL_REFUND_FAILURE_CLEARED_ON_ENTER");}catch(InterruptedIOException expected){need(expected.getCause()==first,"ORIGINAL_REFUND_CAUSE_NOT_PROPAGATED");}
                need(first==get(active,"stopCauseR31"),"FIRST_STOP_CAUSE_REPLACED");
            }finally{release(active);}
            Object cancelled=active(job,root);((AtomicBoolean)get(cancelled,"cancel")).set(true);((ReportNativeCP4.Control)cancelled).onBackupProgress(165824,0,101);
            try{enter(cancelled,"SNAPSHOT_HASH");throw new AssertionError("USER_CANCEL_IGNORED");}catch(InterruptedIOException expected){need("REPORT_USER_CANCEL".equals(expected.getMessage()),"USER_CANCEL_REASON_LOST");}finally{release(cancelled);}
            Object rejected=active(job,root);StorageBudgetR31.Reservation small=budget.reserve("undersized",99,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/r981_reports/jobs/"+job.jobId,1,4,true);set(rejected,"storageReservationR31",small);set(rejected,"backupTargetR31",new File(directory,"reject.db"));
            try{((ReportNativeCP4.Control)rejected).beforeBackupChunk(8192);throw new AssertionError("REAL_CHUNK_ADMISSION_IGNORED");}catch(IOException expected){need(!((ReportNativeCP4.Control)rejected).shouldContinue()&&expected==get(rejected,"stopCauseR31"),"ADMISSION_ORIGINAL_CAUSE_LOST");}finally{release(rejected);}
        }
        // Independent phone layout: only a retention job remains, both failed
        // FULL_REPORT ACKs and recovery ZIPs survive, failures/ never existed.
        File historyFiles=new File(app,"history/files"),historyRoot=ReportDirectoriesCP12.root(historyFiles);
        try(ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(historyFiles)){ReportJobJournalCP12.Job other=new ReportJobJournalCP12.Job(id(70),id(70),id(90),"RETENTION_PAGE",9999);other.stage="RETENTION_READY";journal.create(other);}
        recovery(historyRoot,20,200);recovery(historyRoot,21,100); // filename and mtime order deliberately disagree
        write(new File(historyRoot,"requests/"+id(60)+".ack.json"),new JSONObject().put("requestId",id(60)).put("mode","SAF_TRANSFER").put("stage","FAILED").put("createdWallMs",99999).toString());
        byte[] before=Files.readAllBytes(new File(historyRoot,"requests/"+id(20)+".ack.json").toPath());
        JSONObject latest=ReportJobJournalCP12.latestFullReportStateR31(historyRoot,false),failed=ReportJobJournalCP12.latestFullReportStateR31(historyRoot,true);
        need(id(20).equals(latest.getString("requestId"))&&id(20).equals(failed.getString("requestId")),"ACK_ONLY_ENTRY_POINTS_PICKED_WRONG_REQUEST");
        ReportJobJournalCP12.FailureDetailR31 detail=ReportJobJournalCP12.failureDetailR31(historyRoot,latest);
        need(detail.text.contains("ORIGINAL_20")&&!detail.text.contains("ORIGINAL_21")&&!detail.recoverySha.isEmpty(),"RECOVERY_ORIGINAL_WRONG_OR_MISSING");
        need(!detail.text.contains("phone-test-secret")&&!detail.text.contains("hidden-test-token")&&!detail.text.contains("url-test-secret")&&!detail.text.contains("user:pass"),"CREDENTIALS_NOT_REDACTED");
        File archive=new File(historyRoot,"recovery/"+id(20)+"/"+detail.recoverySha+".zip");byte[] bytes=Files.readAllBytes(archive.toPath());bytes[bytes.length/2]^=1;Files.write(archive.toPath(),bytes);
        detail=ReportJobJournalCP12.failureDetailR31(historyRoot,failed);need(detail.text.contains("REPORT_PROGRESS_WRITE_FAILED")&&detail.recoverySha.isEmpty()&&!detail.text.contains("ORIGINAL_20"),"CORRUPT_RECOVERY_HID_ACK_OR_WAS_READ");
        Files.delete(archive.toPath());detail=ReportJobJournalCP12.failureDetailR31(historyRoot,failed);need(detail.text.contains("REPORT_PROGRESS_WRITE_FAILED")&&detail.recoverySha.isEmpty(),"MISSING_RECOVERY_HID_ACK");
        need(Arrays.equals(before,Files.readAllBytes(new File(historyRoot,"requests/"+id(20)+".ack.json").toPath()))&&!new File(historyRoot,"jobs/"+id(120)).exists()&&!new File(historyRoot,"failures").exists(),"READ_ONLY_RECOVERY_MUTATED_EVIDENCE_OR_RECREATED_JOB");
        System.out.println("PHONE_REPORT_HOST_PASS checks="+checks+" pages=165824 chunks="+chunks+" finalCallbacks="+finalCallbacks+"; ACTUAL_PRODUCTION_CALLBACKS_AND_JNI; HOST_SYNTHETIC_NOT_ANDROID_OR_HANDSET");
    }
}
