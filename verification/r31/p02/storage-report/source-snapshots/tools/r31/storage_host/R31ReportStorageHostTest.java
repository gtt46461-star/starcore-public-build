package com.ethk3.novaonepro;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.json.*;

/** Original production JNI and report owners. Synthetic SQLite/files only, never natural or phone evidence. */
public final class R31ReportStorageHostTest {
    static final long MiB=1024L*1024L;
    static void need(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    static String id(int n){return String.format(Locale.ROOT,"%064x",n);}
    static StorageBudgetR31 budget(File root)throws Exception{return new StorageBudgetR31(root,new StorageBudgetR31.Config(512*MiB,512*MiB,1024,65536,"HOST_NATIVE_REPORT_CAPACITY_FIXTURE"),f->1024*MiB);}
    static final class DiskControl implements ReportNativeCP4.Control {
        final StorageBudgetR31.Reservation reservation;final File target;int chunks;long charged,before,admitted;int failAfter=-1;IOException failure;
        DiskControl(StorageBudgetR31.Reservation reservation,File target){this.reservation=reservation;this.target=target;}
        long physical()throws IOException{
            if(target==null)return 0;long bytes=0;
            for(File file:new File[]{target,new File(target.getPath()+"-journal")})if(file.exists()){
                Process stat=new ProcessBuilder("stat","--format=%b",file.getPath()).start();byte[] result;
                try(InputStream input=stat.getInputStream()){result=input.readNBytes(128);}
                try{if(stat.waitFor()!=0)throw new IOException("HOST_STAT_FAILED");}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new InterruptedIOException("HOST_STAT_INTERRUPTED");}
                bytes=Math.addExact(bytes,Math.multiplyExact(Long.parseLong(new String(result,java.nio.charset.StandardCharsets.US_ASCII).trim()),512L));
            }return bytes;
        }
        void settle()throws IOException{if(charged>0){long growth=Math.max(0,physical()-before);need(growth<=charged,"NATIVE_ACTUAL_GROWTH_EXCEEDED_PREWRITE_ADMISSION:"+growth+">"+charged);reservation.refundUnwritten(charged,growth);charged=0;}}
        public boolean shouldContinue(){return failure==null;}
        public boolean hasDiskAdmissionR31(){return true;}
        public void beforeOutputBytesR31(File file,long bytes)throws IOException{reservation.beforeChunk(bytes);}
        public void reserveFutureBytesR31(long bytes)throws IOException{reservation.ensureAdditional(bytes,ReportPhaseBudgetR31.MAX_WRITTEN,true);}
        public void beforeBackupChunk(long maximum)throws IOException{
            settle();if(failAfter>=0&&chunks>=failAfter)throw new IOException("HOST_INJECTED_CHUNK_ADMISSION_REJECTION");
            chunks++;before=physical();reservation.beforeChunk(maximum);charged=maximum;admitted+=maximum;
        }
        public void onBackupProgress(long total,long remaining,int code){try{settle();}catch(IOException error){failure=error;}}
        public OutputStream protectOutput(File file,OutputStream sink)throws IOException{return reservation.wrap(file,sink);}
    }
    static void phases(ReportJobJournalCP12 owner,ReportJobJournalCP12.Job job)throws Exception{
        for(ReportJobJournalCP12.Stage stage:new ReportJobJournalCP12.Stage[]{ReportJobJournalCP12.Stage.BACKUP,ReportJobJournalCP12.Stage.SNAPSHOT_DURABLE,ReportJobJournalCP12.Stage.INDEXING,ReportJobJournalCP12.Stage.INDEX_DURABLE,ReportJobJournalCP12.Stage.COMPRESSING,ReportJobJournalCP12.Stage.VERIFYING,ReportJobJournalCP12.Stage.READY_TO_SHARE})owner.transition(job,stage,System.currentTimeMillis());
    }
    static ReportJobJournalCP12.Job publish(ReportJobJournalCP12 owner,StorageBudgetR31 budget,File source,int n,boolean member)throws Exception{
        ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(id(n),id(100+n),id(900),"FULL_REPORT",System.currentTimeMillis());job.publishedDir="archive-31";owner.create(job);File directory=owner.directory(job),snapshot=new File(directory,"snapshot.db");
        ReportJobJournalCP12.writeAtomic(new File(owner.root(),"requests/"+job.requestId+".json"),new JSONObject().put("includeRawEvidence",true).toString());
        try(StorageBudgetR31.Reservation r=budget.reserve("report:"+job.jobId,job.revision,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/r981_reports/jobs/"+job.jobId,32*MiB,128*MiB,true)){
            DiskControl control=new DiskControl(r,snapshot);try(ReportIoR31.Scope io=ReportIoR31.bind(directory,control)){ReportNativeCP4.backup(source,snapshot,32*MiB,control);need(control.chunks>2&&control.failure==null,"BACKUP_NOT_CHUNKED_OR_ADMITTED");
            job.snapshotSha=ReportArchiveCP4.hash(snapshot);job.snapshotBytes=snapshot.length();File archive=new File(directory,job.publishedDir);need(archive.mkdir(),"ARCHIVE_DIRECTORY");File facts=new File(directory,"facts.json");Files.writeString(facts.toPath(),"{\"role\":\"HOST_STORAGE_FIXTURE_NOT_NATURAL\"}");
            control.reserveFutureBytesR31(ReportPhaseBudgetR31.archiveFuture(facts.length()+(member?snapshot.length():0),member?2:1));
            try(ReportArchiveCP4 writer=new ReportArchiveCP4(archive,job.jobId,control)){
                writer.add("facts.json",facts,4*MiB,control);if(member)writer.add("snapshot.db",snapshot,4*MiB,control);writer.finish(true,true,"FULL_REPORT_SINGLE_SNAPSHOT_AND_FROZEN_ATTACHMENTS");
            }
            ReportArchiveVerifierCP12.Receipt proof=ReportArchiveVerifierCP12.verify(archive,job.jobId,ReportArchiveVerifierCP12.nativeDecoder(4*MiB),control,null);job.manifestSha=proof.manifestSha256;job.evidenceComplete=true;job.checksComplete=true;job.allChecksPassed=false;
            ReportPublicationCP12.prepareBoundedZipR30(directory,archive,job.jobId,job.manifestSha,true,true,false,control);
            ReportJobJournalCP12.writeAtomic(new File(directory,"published.json"),new JSONObject().put("jobId",job.jobId).put("relativeDir",job.publishedDir).put("manifestSha256",job.manifestSha).put("deliveryIndexSha256",ReportArchiveCP4.hash(new File(directory,"delivery/index.json"))).toString());
            phases(owner,job);owner.acknowledge(job.requestId,job);ReportCleanupR31.enqueue(owner,job,"HOST_RESOURCE_SCOPE_CLOSED_BEFORE_CLEANUP");
            need(((List<?>)budget.snapshot().get("owners")).size()==1,"DISK_LEASE_RELEASED_BEFORE_ACTUAL_SCOPE_EXIT");
        }}return job;
    }
    static void forceDue(File directory)throws Exception{
        File record=new File(directory,"cleanup-r31.json");JSONObject row=new JSONObject(ReportJobJournalCP12.readAtomic(record,16384));row.put("nextMonotonicNanos",0).put("nextWallMs",0).put("fixture","CONTROLLED_RETRY_ONLY_NOT_ELAPSED_SOAK");ReportJobJournalCP12.writeAtomic(record,row.toString());
    }
    public static void main(String[] args)throws Exception{
        File app=new File(args[0]),source=new File(app,"databases/"+AppConfig.DB_NAME),files=new File(app,"files");files.mkdirs();String sourceSha=ReportArchiveCP4.hash(source);StorageBudgetR31 budget=budget(app);ReportNativeCP4.loadForDiagnosticProcess("fixture:diagnostics","fixture");
        try(Closeable fixture=ReportIoR31.hostFixture(app,budget)){
        File rejected=new File(files,"native-rejected/snapshot.db");rejected.getParentFile().mkdirs();
        try(StorageBudgetR31.Reservation r=budget.reserve("native-reject",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/native-rejected",8192,64*MiB,true)){
            DiskControl c=new DiskControl(r,rejected);try{ReportNativeCP4.backup(source,rejected,32*MiB,c);throw new AssertionError("UNDERSIZED_GRANT_ACCEPTED");}catch(IOException expected){need(expected.getMessage().contains("PHYSICAL_GROWTH_BOUND"),expected.toString());}
            need(!rejected.exists()&&!new File(rejected+"-journal").exists(),"REJECTED_NATIVE_OUTPUT_OR_JOURNAL_LEFT");need(((List<?>)budget.snapshot().get("owners")).size()==1,"JNI_EXCEPTION_RELEASED_OWNER_EARLY");
        }
        File interrupted=new File(files,"native-interrupted/snapshot.db");interrupted.getParentFile().mkdirs();
        try(StorageBudgetR31.Reservation r=budget.reserve("native-interrupt",2,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/native-interrupted",16*MiB,true)){
            DiskControl c=new DiskControl(r,interrupted);c.failAfter=3;try{ReportNativeCP4.backup(source,interrupted,32*MiB,c);throw new AssertionError("INJECTED_CHUNK_FAILURE_IGNORED");}catch(IOException expected){need(expected.getMessage().contains("HOST_INJECTED"),expected.toString());}
            need(c.chunks==3&&!interrupted.exists()&&!new File(interrupted+"-journal").exists(),"MID_BACKUP_FAILURE_DID_NOT_CLOSE_AND_UNLINK");
        }
        need(sourceSha.equals(ReportArchiveCP4.hash(source)),"READONLY_SOURCE_CHANGED_BY_REJECTED_BACKUPS");
        try(ReportJobJournalCP12 owner=ReportJobJournalCP12.tryClaim(files)){
            need(owner!=null,"REPORT_OWNER_BUSY");ReportJobJournalCP12.Job job=publish(owner,budget,source,1,true);File directory=owner.directory(job),snapshot=new File(directory,"snapshot.db");long before=StorageBudgetR31.scopeBytes(directory);
            try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){
                try{ReportCleanupR31.runDue(owner,System.currentTimeMillis(),2);throw new AssertionError("CLEANUP_ACCEPTED_OPEN_INPUTS");}catch(IOException expected){need(expected.getMessage().equals("REPORT_CLEANUP_INPUTS_STILL_OPEN"),expected.toString());}need(snapshot.exists(),"INPUT_PINNED_SNAPSHOT_REMOVED");
            }
            try(ReportPublicationCP12.Lease reader=ReportPublicationCP12.shared(directory)){
                ReportCleanupR31.runDue(owner,System.currentTimeMillis(),2);JSONObject pending=new JSONObject(ReportJobJournalCP12.readAtomic(new File(directory,"cleanup-r31.json"),16384));need(pending.getString("state").equals("CLEANUP_PENDING")&&snapshot.exists(),"ACTIVE_SHARE_NOT_RETAINED");need(ReportCleanupR31.nextDelayMs(owner.root())>=1000,"NO_NEW_EXPORT_RETRY_NOT_SCHEDULED");
            }
            forceDue(directory);ReportCleanupR31.runDue(owner,System.currentTimeMillis(),2);JSONObject receipt=new JSONObject(ReportJobJournalCP12.readAtomic(new File(directory,"cleanup-r31.json"),16384));
            need(receipt.getString("state").equals("CLEANUP_COMPLETE")&&!snapshot.exists(),"CLOSED_OWNER_CLEANUP_NOT_COMPLETE");need(receipt.getLong("reclaimedBytes")>0&&StorageBudgetR31.scopeBytes(directory)<before,"PRIVATE_BYTES_NOT_ACTUALLY_RELEASED");need(ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(directory,"job.json"),65536)).stage.equals("READY_TO_SHARE"),"CLEANUP_CHANGED_READY_COMMIT");need(ReportPublicationCP12.verifyPublishedZip(directory,()->true),"SEALED_DELIVERY_NOT_PRESERVED");Files.copy(new File(directory,"cleanup-r31.json").toPath(),new File(new File(args[1]).getParentFile(),"cleanup-complete.json").toPath());
            File zip=new File(directory,"delivery/STARCORE_FULL_REPORT.zip"),restored=new File(args[1]);boolean found=false;
            try(ZipInputStream in=new ZipInputStream(new FileInputStream(zip))){ZipEntry entry;byte[] buffer=new byte[32768];while((entry=in.getNextEntry())!=null){if(entry.getName().equals("snapshot.db")){try(OutputStream out=new FileOutputStream(restored)){int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}found=true;}else while(in.read(buffer)!=-1){}in.closeEntry();}}
            need(found&&job.snapshotSha.equals(ReportArchiveCP4.hash(restored)),"RESTORED_RAW_DB_HASH_OR_ZIP_CRC");
            ReportJobJournalCP12.Job incomplete=publish(owner,budget,source,2,false);File protectedDir=owner.directory(incomplete);Files.writeString(new File(directory,"cleanup-r31.json").toPath(),"{BAD_CLEANUP_RECORD");ReportCleanupR31.runDue(owner,System.currentTimeMillis(),4);
            JSONObject protectedReceipt=new JSONObject(ReportJobJournalCP12.readAtomic(new File(protectedDir,"cleanup-r31.json"),16384));need(protectedReceipt.getString("state").equals("CLEANUP_PENDING")&&protectedReceipt.getString("reason").contains("FULL_RAW_MEMBER_MISSING")&&new File(protectedDir,"snapshot.db").isFile(),"UNVERIFIED_UNIQUE_RAW_NOT_PROTECTED");
            JSONObject repaired=new JSONObject(ReportJobJournalCP12.readAtomic(new File(directory,"cleanup-r31.json"),16384));need(repaired.getString("reason").contains("CLEANUP_RECORD_CORRUPT"),"CORRUPT_RECORD_NOT_ISOLATED");need(ReportPublicationCP12.verifyPublishedZip(directory,()->true),"CORRUPT_CLEANUP_RECORD_DAMAGED_PUBLISHED_RESULT");Files.copy(new File(protectedDir,"cleanup-r31.json").toPath(),new File(new File(args[1]).getParentFile(),"cleanup-protected.json").toPath());
            System.out.println("R31_REPORT_STORAGE_HOST|PREWRITE_NATIVE_ADMISSION|JNI_EXCEPTION_CLOSES_DEST_AND_JOURNAL|READONLY_SOURCE_HASH_UNCHANGED|ARCHIVE_ZIP_SINKS_ADMITTED|INPUT_AND_SHARE_PINS_PROTECTED|READY_WITH_CLEANUP_PENDING_RETRY|ACTUAL_PRIVATE_BYTES_REMOVED|RAW_DB_ZIP_CRC_AND_SHA_RESTORED|MISSING_RAW_MEMBER_PROTECTED|CORRUPT_CLEANUP_ISOLATED|HOST_NOT_ANDROID_NOT_NATURAL|PASS");
        }
        need(((List<?>)budget.snapshot().get("owners")).isEmpty(),"NATIVE_OR_REPORT_DISK_OWNER_LEAKED");
        }
    }
}
