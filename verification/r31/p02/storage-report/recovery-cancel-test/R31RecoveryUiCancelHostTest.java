package com.ethk3.novaonepro;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.json.*;

/** Actual recovery ZIP and OS descriptors. UI BooleanSupplier signal, deliberately no thread interruption. */
public final class R31RecoveryUiCancelHostTest {
    static void need(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    static String id(int n){return String.format(Locale.ROOT,"%064x",n);}
    static int openDescriptors(File root)throws Exception{
        int count=0;try(java.util.stream.Stream<Path> paths=Files.list(Path.of("/proc/self/fd"))){for(Iterator<Path> it=paths.iterator();it.hasNext();){Path path=it.next();try{if(Files.readSymbolicLink(path).toString().startsWith(root.getCanonicalPath()+File.separator))count++;}catch(IOException gone){}}}return count;
    }
    public static void main(String[] args)throws Exception{
        File root=new File(args[0]),source=new File(args[1]);root.mkdirs();String sourceHash=ReportArchiveCP4.hash(source);
        StorageBudgetR31 budget=new StorageBudgetR31(root,new StorageBudgetR31.Config(128L<<20,128L<<20,1024,65536,"HOST_RECOVERY_UI_CANCEL_FIXTURE"),f->1024L<<20);
        try(Closeable fixture=ReportIoR31.hostFixture(root,budget);ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(new File(root,"files"))){
            need(journal!=null,"JOURNAL_OWNER");ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(id(44),id(144),id(944),"FULL_REPORT",System.currentTimeMillis());journal.create(job);
            File snapshot=new File(journal.directory(job),"snapshot.db");Files.copy(source.toPath(),snapshot.toPath());job.snapshotSha=ReportArchiveCP4.hash(snapshot);job.snapshotBytes=snapshot.length();need(sourceHash.equals(job.snapshotSha),"ORIGINAL_VALID_SQLITE_COPY");
            ReportJobJournalCP12.writeAtomic(new File(journal.root(),"requests/"+job.requestId+".json"),"{\"includeRawEvidence\":true}");
            File recovery=new File(journal.root(),"recovery/"+job.requestId);AtomicBoolean uiCancelled=new AtomicBoolean(),announced=new AtomicBoolean(),interrupted=new AtomicBoolean();CountDownLatch wroteBytes=new CountDownLatch(1),applySignal=new CountDownLatch(1);
            ExecutorService worker=Executors.newSingleThreadExecutor();
            Future<Throwable> done=worker.submit(()->{
                try{ReportJobJournalCP12.writeFailureArtifact(journal.root(),job.requestId,new IOException("HOST_ORIGINAL_REPORT_FAILURE"),job,()->{
                    try{if(!announced.get()&&recovery.isDirectory())for(File file:DirectoryCursorR31.children(recovery,8))if(file.getName().endsWith(".partial")&&file.length()>=65536&&announced.compareAndSet(false,true)){wroteBytes.countDown();if(!applySignal.await(5,TimeUnit.SECONDS))throw new IllegalStateException("UI_SIGNAL_TIMEOUT");break;}}
                    catch(IOException|InterruptedException failure){throw new IllegalStateException(failure);}return uiCancelled.get();
                });return new AssertionError("CANCELLED_RECOVERY_PUBLISHED");}
                catch(Throwable failure){return failure;}finally{interrupted.set(Thread.currentThread().isInterrupted());}
            });
            long elapsed;
            try{
                need(wroteBytes.await(8,TimeUnit.SECONDS),"REAL_PARTIAL_BYTES_NOT_OBSERVED");need(!done.isDone(),"WORKER_EXITED_BEFORE_UI_SIGNAL");need(!((List<?>)budget.snapshot().get("owners")).isEmpty(),"PERMIT_RELEASED_WHILE_STREAM_HELD");need(!ReportPublicationCP12.tryDeleteRecoveryR31(recovery),"ACTIVE_RECOVERY_WRITER_NOT_PINNED");need(openDescriptors(journal.directory(job))>0&&openDescriptors(recovery)>0,"INPUT_OUTPUT_HANDLES_NOT_ACTUALLY_HELD_AT_UI_CANCEL");
                long start=System.nanoTime();uiCancelled.set(true);applySignal.countDown();Throwable failure=done.get(5,TimeUnit.SECONDS);elapsed=(System.nanoTime()-start)/1000000L;
                need(failure instanceof StorageBudgetR31.Rejected&&com.ethk3.novaonepro.strategyv2.FailureDispositionR31.classify(failure).disposition==com.ethk3.novaonepro.strategyv2.FailureDispositionR31.Disposition.CANCELLED,"CANCEL_FLAG_NOT_TYPED:"+failure);
                need(elapsed<=2000&&!interrupted.get(),"UI_FLAG_RELIED_ON_INTERRUPT_OR_EXIT_TOO_SLOW:"+elapsed);
            }finally{applySignal.countDown();worker.shutdown();need(worker.awaitTermination(5,TimeUnit.SECONDS),"WORKER_NOT_ACTUALLY_TERMINATED");}
            for(File file:DirectoryCursorR31.children(recovery,8))need(!file.getName().endsWith(".partial")&&!file.getName().endsWith(".zip")&&!file.getName().equals("receipt.json"),"HALF_ZIP_OR_SUCCESS_RECEIPT_PUBLISHED");
            need(((List<?>)budget.snapshot().get("owners")).isEmpty(),"DISK_OWNER_NOT_RELEASED_AFTER_EXIT");need(openDescriptors(recovery)==0&&openDescriptors(journal.directory(job))==0,"INPUT_OR_OUTPUT_DESCRIPTOR_RETAINED_AFTER_EXIT");need(sourceHash.equals(ReportArchiveCP4.hash(source))&&job.snapshotSha.equals(ReportArchiveCP4.hash(snapshot)),"CANCEL_DAMAGED_ORIGINAL_INPUT");
            uiCancelled.set(false);JSONObject retry=ReportJobJournalCP12.writeFailureArtifact(journal.root(),job.requestId,new IOException("HOST_ORIGINAL_REPORT_FAILURE"),job,uiCancelled::get);File zip=new File(recovery,retry.getString("sha256")+".zip");need(retry.getBoolean("databaseIncluded")&&zip.isFile()&&retry.getString("sha256").equals(ReportArchiveCP4.hash(zip)),"SAME_JOB_RETRY_NOT_VERIFIED");
            need(((List<?>)budget.snapshot().get("owners")).isEmpty()&&openDescriptors(recovery)==0&&openDescriptors(journal.directory(job))==0&&!ReportIoR31.currentThreadHasScope(),"RETRY_RESOURCE_OR_SCOPE_LEAK");
            System.out.println("R31_RECOVERY_UI_CANCEL_HOST|REAL_PARTIAL_GE_64K|BOOLEAN_UI_FLAG_WITHOUT_INTERRUPT|ACTUAL_WORKER_EXIT_MS="+elapsed+"|INPUT_OUTPUT_OS_FDS_CLOSED|NO_PARTIAL_ZIP_OR_READY_RECEIPT|SOURCE_HASH_PRESERVED|SAME_JOB_RETRY_CRC_SHA_VERIFIED|HOST_NOT_ANDROID_OR_DEVICE_DURATION|PASS");
        }
    }
}
