package com.ethk3.novaonepro;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Actual report sinks, descriptor copy, external sort, renderer and recovery ZIP. No Android VM/SQL-check claim. */
public final class R31ReportSinkHostTest {
    static final long MiB=1024L*1024L;
    static void need(boolean value,String why){if(!value)throw new AssertionError(why);}
    static String id(int n){return String.format(Locale.ROOT,"%064x",n);}
    static final class Owner implements ReportNativeCP4.Control {
        final StorageBudgetR31.Reservation reservation;int calls;long bytes;long denyAfter=Long.MAX_VALUE;
        Owner(StorageBudgetR31.Reservation reservation){this.reservation=reservation;}
        public boolean shouldContinue(){return true;}public boolean hasDiskAdmissionR31(){return true;}
        public void beforeOutputBytesR31(File file,long count)throws IOException{reservation.beforeChunk(count);calls++;bytes+=count;}
        public void reserveFutureBytesR31(long bytes)throws IOException{reservation.ensureAdditional(bytes,ReportPhaseBudgetR31.MAX_WRITTEN,true);}
        public OutputStream protectOutput(File file,OutputStream raw)throws IOException{
            OutputStream sink=reservation.wrap(file,raw);return new FilterOutputStream(sink){
                public void write(int value)throws IOException{write(new byte[]{(byte)value},0,1);}
                public void write(byte[] value,int offset,int count)throws IOException{calls++;if(count>denyAfter-bytes)throw new IOException("HOST_INJECTED_SINK_DENIAL");bytes+=count;out.write(value,offset,count);}
            };
        }
    }
    static final class RenderContext implements ReportTextCP12.Context {
        final File directory;int held,recorded;RenderContext(File directory){this.directory=directory;}
        public void checkpoint(){}public File jobDirectory(){return directory;}public void trackRenderer(ReportTextCP12 text){held++;}public void releaseRenderer(ReportTextCP12 text){held--;}
        public void recordTextArtifact(File file,String hash,long bytes)throws IOException{try{need(hash.equals(ReportArchiveCP4.hash(file)),"RENDER_SHA");}catch(Exception error){throw new IOException(error);}recorded++;}
    }
    public static void main(String[] args)throws Exception{
        File app=new File(args[0]);app.mkdirs();StorageBudgetR31 budget=new StorageBudgetR31(app,new StorageBudgetR31.Config(128*MiB,128*MiB,1024,65536,"HOST_REPORT_SINK_CAPACITY_FIXTURE"),f->1024*MiB);
        try(Closeable host=ReportIoR31.hostFixture(app,budget);ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(new File(app,"files"))){
            ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(id(31),id(131),id(931),"FULL_REPORT",System.currentTimeMillis());
            // Hold the actual bulk-admission OS lease. Initial job/control
            // durability must use the precharged, separately bounded metadata
            // path rather than a racing whole-tree census before admission.
            try(RandomAccessFile lockFile=new RandomAccessFile(new File(app,"files/r31-storage/admission.lock"),"rw");java.nio.channels.FileLock bulkLease=lockFile.getChannel().lock()){
                journal.create(job);journal.checkpoint(job,System.currentTimeMillis());journal.acknowledge(job.requestId,job);
                need(new File(journal.directory(job),"job.json").isFile(),"JOB_HEADER_NOT_DURABLE_WITH_BULK_COORDINATOR_BUSY");
                need(!ReportControlBudgetR31.accepts(journal.root(),new File(journal.directory(job),"bulk.json")),"BULK_PAYLOAD_ENTERED_CONTROL_HEADROOM");
                try(ReportIoR31.AtomicPermit denied=ReportIoR31.beforeAtomic(new File(journal.directory(job),"job.json"),65537)){throw new AssertionError("OVERSIZED_JOB_CONTROL_RECORD_ADMITTED");}
                catch(IOException expected){need(expected.getMessage().equals("REPORT_CONTROL_RECORD_BOUND"),"ORIGINAL_CONTROL_RECORD_BOUND_LOST");}
            }
            File directory=journal.directory(job);
            File original=new File(app,"original-input.bin");try(OutputStream out=new FileOutputStream(original)){byte[] buffer=new byte[32768];new Random(31).nextBytes(buffer);for(int n=0;n<20;n++)out.write(buffer);}String inputSha=ReportArchiveCP4.hash(original);
            try(StorageBudgetR31.Reservation reservation=budget.reserve("sink-owner",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/r981_reports/jobs/"+job.jobId,4*MiB,64*MiB,true)){
                Owner owner=new Owner(reservation);try(ReportIoR31.Scope scope=ReportIoR31.bind(directory,owner)){
                    ReportJobJournalCP12.writeAtomic(new File(directory,"proof.json"),"{\"role\":\"HOST_REPORT_SINK\"}");
                    Method copy=ReportAttachmentClosureCP12.class.getDeclaredMethod("copy",InputStream.class,File.class,long.class,String.class,ReportNativeCP4.Control.class);copy.setAccessible(true);File frozen=new File(directory,"attachment.bin");try(InputStream in=new FileInputStream(original)){copy.invoke(null,in,frozen,original.length(),inputSha,owner);}need(inputSha.equals(ReportArchiveCP4.hash(frozen)),"FROZEN_COPY_SHA");
                    File plan=new File(directory,"plan.jsonl");try(FileOutputStream fd=new FileOutputStream(plan);Writer rows=new OutputStreamWriter(owner.protectOutput(plan,fd),StandardCharsets.UTF_8)){for(int n=127;n>=0;n--)ReportMemberPlanCP12.append(rows,String.format(Locale.ROOT,"n%04d",n),"attachment.bin",inputSha,original.length());}
                    File sorted=ReportMemberPlanCP12.sort(directory,plan,owner);int count=0;String previous="";try(ReportArchiveVerifierCP12.LineReader rows=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(sorted),8192)){byte[] row;while((row=rows.next())!=null){String name=new JSONObject(new String(row,StandardCharsets.UTF_8)).getString("name");need(previous.compareTo(name)<0,"SORT_ORDER");previous=name;count++;}}need(count==128,"EXTERNAL_SORT_DROPPED_RECORDS");
                    Class<?> pagesType=Class.forName("com.ethk3.novaonepro.ReportCatalogCP4$FilePages");Constructor<?> ctor=pagesType.getDeclaredConstructor(File.class);ctor.setAccessible(true);ReportCatalogCP4.Pages pages=(ReportCatalogCP4.Pages)ctor.newInstance(directory);try(Writer page=pages.open(1)){page.write("{\"kind\":\"HOST_SINK_ONLY_NOT_96_CHECKS\"}\n");}pages.committed(1,45,1);need(new File(directory,"catalog/catalog-0001.jsonl").isFile(),"CATALOG_PAGE_NOT_COMMITTED");
                    RenderContext render=new RenderContext(directory);ReportTextCP12 text=new ReportTextCP12(render);for(int n=0;n<50000;n++)text.append('x');String reference=text.toString();need(reference.startsWith(ReportTextCP12.REF_BEGIN)&&render.held==0&&render.recorded==1,"RENDERER_SCOPE_OR_REFERENCE");
                    File inventory=ReportDiagnosticEvidenceR64.writeFileInventory(app,directory,owner);need(inventory.isFile(),"STREAM_INVENTORY_MISSING");
                    int before=owner.calls;long charged=owner.bytes;owner.denyAfter=charged;File denied=new File(directory,"denied-attachment.bin");try(InputStream in=new FileInputStream(original)){try{copy.invoke(null,in,denied,original.length(),inputSha,owner);throw new AssertionError("DENIED_ATTACHMENT_PUBLISHED");}catch(InvocationTargetException expected){need(expected.getCause() instanceof IOException,"ORIGINAL_DENIAL_LOST");}}need(!denied.exists()&&inputSha.equals(ReportArchiveCP4.hash(original)),"DENIAL_DAMAGED_ORIGINAL_OR_PUBLISHED_PARTIAL");owner.denyAfter=Long.MAX_VALUE;
                    need(owner.calls>before&&owner.bytes>=charged,"ACTUAL_SINKS_DID_NOT_CONSUME_ADMISSION");
                    long old=reservation.maximumGrowthBytes();reservation.ensureAdditional(8*MiB,64*MiB,true);need(reservation.maximumGrowthBytes()>old,"MEASURED_PHASE_DID_NOT_EXTEND");
                    try{reservation.ensureAdditional(128*MiB,ReportPhaseBudgetR31.MAX_WRITTEN,true);throw new AssertionError("PHASE_CAP_OVERRUN_ACCEPTED");}catch(StorageBudgetR31.Rejected expected){need(expected.reason.equals("BLOCKED_PROTECTED_DEBT"),expected.toString());}
                }need(!ReportIoR31.currentThreadHasScope(),"THREAD_LOCAL_OWNER_LEAKED");
            }
            ReportJobJournalCP12.writeAtomic(new File(journal.root(),"requests/"+job.requestId+".json"),"{\"includeRawEvidence\":false}");JSONObject recovery=ReportJobJournalCP12.writeFailureArtifact(journal.root(),job.requestId,new IOException("HOST_ORIGINAL_FAILURE"),job);need(!recovery.getBoolean("databaseIncluded"),"DIAGNOSTIC_RECOVERY_CLAIMED_DB");
            File recoveryDir=new File(journal.root(),"recovery/"+job.requestId),zip=new File(recoveryDir,recovery.getString("sha256")+".zip");need(zip.isFile()&&recovery.getString("sha256").equals(ReportArchiveCP4.hash(zip)),"RECOVERY_SHA");
            job.lastReason="HOST_ORIGINAL_FAILURE";journal.transition(job,ReportJobJournalCP12.Stage.FAILED,System.currentTimeMillis());
            File ack=new File(journal.root(),"requests/"+job.requestId+".ack.json");need(ack.delete(),"FAILURE_ACK_DELETE");
            JSONObject authority=ReportJobJournalCP12.authoritativeStateR31(journal.root(),job.requestId);
            need("FAILED".equals(authority.getString("stage"))&&"HOST_ORIGINAL_FAILURE".equals(authority.getString("reason")),"AUTHORITY_LOST_ORIGINAL_FAILURE");
            need(recovery.getString("sha256").equals(authority.getJSONObject("recovery").getString("sha256")),"AUTHORITY_HID_RECOVERY_WITHOUT_ACK");
            File recoveryReceipt=new File(recoveryDir,"receipt.json");String originalReceipt=ReportJobJournalCP12.readAtomic(recoveryReceipt,4096);
            Files.write(recoveryReceipt.toPath(),"{BROKEN".getBytes(StandardCharsets.UTF_8));
            JSONObject damaged=ReportJobJournalCP12.authoritativeStateR31(journal.root(),job.requestId);
            need("FAILED".equals(damaged.getString("stage"))&&damaged.has("recoveryFailure")&&!damaged.has("recovery"),"DAMAGED_OPTIONAL_RECOVERY_HID_JOB_FAILURE");
            Files.write(recoveryReceipt.toPath(),originalReceipt.getBytes(StandardCharsets.UTF_8));
            String pending=id(132);File pendingIntent=new File(journal.root(),"requests/"+pending+".json");
            ReportJobJournalCP12.writeAtomic(pendingIntent,new JSONObject().put("requestId",pending).put("mode","FULL_REPORT").put("createdWallMs",System.currentTimeMillis()).toString());
            File orphanIntent=new File(journal.root(),"requests/"+id(133)+".json");ReportJobJournalCP12.writeAtomic(orphanIntent,"{BROKEN");
            String stop=ReportTaskControlR69.request(new File(app,"files"),false);
            need(ReportTaskControlR69.cancelled(journal.root(),pending,pending),"STOP_MISSED_DURABLE_INTENT_WITHOUT_FIFO_OR_JOB");
            ReportTaskControlR69.applyPending(journal);
            need("CONTROL_DONE".equals(ReportTaskControlR69.status(new File(app,"files"),stop).getString("stage")),"UNCLAIMED_INTENT_STOP_NOT_COMMITTED");
            need(ReportTaskControlR69.status(new File(app,"files"),stop).getInt("unreadableIntentsPreserved")==1&&orphanIntent.isFile(),"CANCEL_REMOVED_OR_HID_UNREADABLE_INTENT");
            ReportRequestQueueCP12.reconcileRequestsR31(journal.root());
            need(!ReportRequestQueueCP12.pendingIds(journal.root()).contains(pending),"STOPPED_INTENT_REQUEUED_AFTER_RECONCILE");
            try(ReportIoR31.Artifact artifact=ReportIoR31.reserveArtifact(recoveryDir,MiB)){need(!ReportPublicationCP12.tryDeleteRecoveryR31(recoveryDir),"ACTIVE_RECOVERY_WRITER_NOT_LEASE_PROTECTED");}
            need(!ReportIoR31.currentThreadHasScope(),"RECOVERY_THREAD_LOCAL_OWNER_LEAKED");need(((List<?>)budget.snapshot().get("owners")).isEmpty(),"OUTPUT_GRANT_LEAKED_AFTER_FD_CLOSED");
            System.out.println("R31_REPORT_SINK_HOST|ATOMIC_DESCRIPTOR_COPY_CATALOG_PAGE_EXTERNAL_MERGE_RENDER_INVENTORY_ALL_CONSUME_REAL_DISK_ADMISSION|DENIAL_PRESERVES_ORIGINAL|EXACT_PHASE_GROWTH_CAP_DENIED|RECOVERY_ZIP_CRC_SHA_AND_ACTIVE_WRITER_PIN|SCOPES_CLOSED|HOST_SINK_CONTRACT_NOT_ANDROID_OR_96_CHECK_BUSINESS_PASS|PASS");
        }
    }
}
