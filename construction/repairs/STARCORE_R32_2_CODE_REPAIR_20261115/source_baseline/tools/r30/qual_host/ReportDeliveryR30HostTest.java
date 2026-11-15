package com.ethk3.novaonepro;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.security.*;
import java.util.zip.*;
import org.json.*;
/** Actual publication, durable cancellation, native compression and filesystem. HOST fixtures are not phone results. */
public final class ReportDeliveryR30HostTest {
    static void need(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static String h(int n){return String.format(Locale.ROOT,"%064x",n);}
    static void random(File file,int mib)throws Exception{try(FileOutputStream out=new FileOutputStream(file)){byte[] block=new byte[32768];Random r=new Random(300804);for(int n=0;n<mib*32;n++){r.nextBytes(block);out.write(block);}out.getFD().sync();}}
    static void reject(Checked work,String why)throws Exception{try{work.run();throw new AssertionError(why);}catch(IOException expected){}}
    interface Checked{void run()throws Exception;}
    static File archive(File directory,String id,File payload)throws Exception{
        File archive=new File(directory,"archive-30");need(archive.mkdir(),"ARCHIVE_DIRECTORY");
        try(ReportArchiveCP4 writer=new ReportArchiveCP4(archive,id)){writer.add("payload.bin",payload,4L<<20,()->true);writer.finish(true,true,"HOST_SYNTHETIC_DELIVERY_ONLY_NOT_NATURAL");}
        return archive;
    }
    public static void main(String[] args)throws Exception{
        File files=args.length==0?Files.createTempDirectory("r30-delivery-").toFile():new File(args[0]);files.mkdirs();
        ReportNativeCP4.loadForDiagnosticProcess("fixture:diagnostics","fixture");
        File sentinel=new File(files,"original-model-protected.bin");Files.writeString(sentinel.toPath(),"PRESERVE_ORIGINAL_PRODUCT_EVIDENCE");
        File payload=new File(files,"payload.bin");random(payload,25);
        try(ReportJobJournalCP12 owner=ReportJobJournalCP12.tryClaim(files)){
            need(owner!=null,"OWNER_LOCK");ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(h(1),h(101),h(901),"FULL_REPORT",System.currentTimeMillis());job.publishedDir="archive-30";owner.create(job);File directory=owner.directory(job),archive=archive(directory,job.jobId,payload);
            ReportArchiveVerifierCP12.Receipt proof=ReportArchiveVerifierCP12.verify(archive,job.jobId,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),()->true,null);
            job.manifestSha=proof.manifestSha256;job.evidenceComplete=true;job.checksComplete=true;job.allChecksPassed=false;
            ReportPublicationCP12.prepareBoundedZipR30(directory,archive,job.jobId,proof.manifestSha256,true,true,false,()->true);
            JSONObject index=new JSONObject(ReportJobJournalCP12.readAtomic(new File(directory,"delivery/index.json"),65536));JSONArray parts=index.getJSONArray("files");
            need(ReportPublicationCP12.ZIP_PART_FORMAT_R30.equals(index.getString("format"))&&parts.length()==2,"DEFAULT_LARGE_ZIP_NOT_PARTITIONED");
            need(!index.getBoolean("allChecksPassed")&&index.getLong("omittedVolumes")==0&&index.getInt("exportedVolumes")==index.getInt("expectedVolumes"),"INDEX_FAKE_HEALTH_OR_OMITTED_PARTS");
            File restored=new File(files,"restored-report.zip");try(OutputStream out=new FileOutputStream(restored)){byte[] b=new byte[32768];for(int i=0;i<parts.length();i++){JSONObject row=parts.getJSONObject(i);need(row.getLong("bytes")<=20L*1024*1024,"VOLUME_EXCEEDS_20MIB");try(InputStream in=new FileInputStream(new File(directory,row.getString("path")))){int n;while((n=in.read(b))!=-1)out.write(b,0,n);}}}
            need(restored.length()==index.getLong("zipBytes")&&ReportArchiveCP4.hash(restored,()->true).equals(index.getString("zipSha256")),"REASSEMBLED_ZIP_HASH_MISMATCH");
            int entries=0;try(ZipInputStream in=new ZipInputStream(new FileInputStream(restored))){ZipEntry e;byte[] b=new byte[32768];while((e=in.getNextEntry())!=null){entries++;MessageDigest hash=MessageDigest.getInstance("SHA-256");int n;while((n=in.read(b))!=-1)hash.update(b,0,n);if(e.getName().equals("payload.bin"))need(ReportArchiveCP4.hex(hash.digest()).equals(ReportArchiveCP4.hash(payload,()->true)),"ORIGINAL_LOGICAL_MEMBER_CHANGED");in.closeEntry();}}
            need(entries==2,"MEMBER_OR_MANIFEST_MISSING");
            JSONObject pointer=new JSONObject().put("jobId",job.jobId).put("relativeDir",job.publishedDir).put("deliveryIndexSha256",ReportArchiveCP4.hash(new File(directory,"delivery/index.json"),()->true));ReportJobJournalCP12.writeAtomic(new File(directory,"published.json"),pointer.toString());for(ReportJobJournalCP12.Stage phase:new ReportJobJournalCP12.Stage[]{ReportJobJournalCP12.Stage.BACKUP,ReportJobJournalCP12.Stage.SNAPSHOT_DURABLE,ReportJobJournalCP12.Stage.INDEXING,ReportJobJournalCP12.Stage.INDEX_DURABLE,ReportJobJournalCP12.Stage.COMPRESSING,ReportJobJournalCP12.Stage.VERIFYING,ReportJobJournalCP12.Stage.READY_TO_SHARE})owner.transition(job,phase,System.currentTimeMillis());
            need(ReportPublicationCP12.verifyPublishedZip(directory,()->true),"NEW_PART_FORMAT_NOT_ACCEPTED_BY_ORIGINAL_PUBLISH_OWNER");
            File first=new File(directory,parts.getJSONObject(0).getString("path"));try(RandomAccessFile io=new RandomAccessFile(first,"rw")){int before=io.read();io.seek(0);io.write(before^1);reject(()->ReportPublicationCP12.verifyPublishedZip(directory,()->true),"CORRUPTED_PART_ACCEPTED");io.seek(0);io.write(before);}
            JSONObject wrong=new JSONObject(index.toString());wrong.getJSONArray("files").getJSONObject(1).put("offset",0);reject(()->ReportPublicationCP12.verifyPartsR30(directory,wrong,()->true),"REORDERED_OFFSETS_ACCEPTED");
            File small=new File(files,"small.bin");Files.writeString(small.toPath(),"small complete report");ReportJobJournalCP12.Job smallJob=new ReportJobJournalCP12.Job(h(2),h(102),h(901),"FULL_REPORT",System.currentTimeMillis());smallJob.publishedDir="archive-30";owner.create(smallJob);File smallDir=owner.directory(smallJob),smallArchive=archive(smallDir,smallJob.jobId,small);ReportArchiveVerifierCP12.Receipt smallProof=ReportArchiveVerifierCP12.verify(smallArchive,smallJob.jobId,ReportArchiveVerifierCP12.nativeDecoder(4L<<20),()->true,null);
            ReportPublicationCP12.prepareBoundedZipR30(smallDir,smallArchive,smallJob.jobId,smallProof.manifestSha256,true,true,false,()->true);JSONObject smallIndex=new JSONObject(ReportJobJournalCP12.readAtomic(new File(smallDir,"delivery/index.json"),65536));need("SCREPORT_SINGLE_ZIP_R992".equals(smallIndex.getString("format"))&&smallIndex.getJSONArray("files").length()==1,"SMALL_ZIP_LEGACY_PATH_CHANGED");
            // Native worker uses the actual durable cancellation reader; the user command is issued concurrently.
            File large=new File(files,"native-cancel-source.bin");random(large,100);ReportJobJournalCP12.Job cancelled=new ReportJobJournalCP12.Job(h(3),h(103),h(901),"FULL_REPORT",System.currentTimeMillis());owner.create(cancelled);File cancelDir=owner.directory(cancelled);CountDownLatch active=new CountDownLatch(1);ExecutorService pool=Executors.newSingleThreadExecutor();
            Future<?> worker=pool.submit(()->{try{
                try(OutputStream out=new FileOutputStream(new File(cancelDir,"compression.partial"))){ReportNativeCP4.compress(large,out,4L<<20,()->{active.countDown();try{return !ReportTaskControlR69.cancelled(owner.root(),cancelled.requestId,cancelled.jobId);}catch(Exception failure){throw new IllegalStateException(failure);}});throw new AssertionError("NATIVE_FINISHED_WITHOUT_CANCELLATION");}
            }catch(ReportNativeCP4.Failure expected){if(expected.nativeCode!=-1001)throw new IllegalStateException(expected);try{owner.transition(cancelled,ReportJobJournalCP12.Stage.CANCELLED,System.currentTimeMillis());ReportTaskControlR69.finishCancellationR30(owner.root(),cancelDir,cancelled);}catch(Exception cleanup){throw new IllegalStateException(cleanup);}}
             catch(Exception failure){throw new IllegalStateException(failure);}});
            need(active.await(5,TimeUnit.SECONDS),"NATIVE_WORKER_DID_NOT_START");long requested=System.nanoTime();ReportTaskControlR69.request(files,false);try{worker.get(2,TimeUnit.SECONDS);}finally{pool.shutdownNow();}long ms=(System.nanoTime()-requested)/1_000_000;
            need(ms<=2000,"ACTUAL_CANCEL_EXCEEDS_2S:"+ms);need(!new File(cancelDir,"compression.partial").exists(),"CANCELLED_JOB_TEMP_LEFT_BEHIND");need(large.isFile()&&sentinel.isFile()&&new File(directory,"published.json").isFile(),"CANCEL_REMOVED_UNRELATED_OR_ORIGINAL_EVIDENCE");
            JSONObject receipt=new JSONObject(ReportJobJournalCP12.readAtomic(new File(cancelDir,"cancel-receipt-r30.json"),4096));need(receipt.getBoolean("resourcesReleased")&&receipt.getBoolean("privateTemporaryFilesCleaned"),"RELEASE_RECEIPT_FALSE");if(!receipt.isNull("cancelRequestToReleaseMs"))need(receipt.getLong("cancelRequestToReleaseMs")<=2000,"RECORDED_CANCEL_EXCEEDS_2S");
            ReportTaskControlR69.applyPending(owner);need(new File(directory,"published.json").exists()&&sentinel.isFile()&&large.isFile(),"DURABLE_COMMAND_CONSUMER_REMOVED_PUBLISHED_OR_ORIGINAL_DATA");
            System.out.println("R30_REPORT_DELIVERY_HOST|ACTUAL_NATIVE_ZIP_TO_20MIB_PARTS|RAW_MEMBER_CRC_AND_REASSEMBLY_SHA|TAMPER_AND_OFFSET_REJECTED|SMALL_ZIP_RETAINED|DURABLE_CANCEL_TO_CLOSED_STREAM_AND_TEMP_CLEAN_MS="+ms+"|ORIGINAL_AND_PUBLISHED_PROTECTED|HOST_NOT_DEVICE|PASS");
        }
    }
}
