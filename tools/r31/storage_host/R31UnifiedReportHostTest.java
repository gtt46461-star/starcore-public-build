package com.ethk3.novaonepro;

import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.json.*;

/** Calls the real private Service.publish, actual native archive/ZIP and journal.
 * Framework logging/file/time transports are explicitly adapted on HOST; no ART,
 * 96-check business success, device or natural-quality claim is made. */
public final class R31UnifiedReportHostTest {
    static int assertions;static final long MiB=1L<<20;
    static void need(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    static String id(String s){return StatsR27.sha256(s);}
    static Map<String,Object> map(Object... fields){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<fields.length;i+=2)m.put((String)fields[i],fields[i+1]);return m;}
    static void set(Object owner,String field,Object value)throws Exception{Field f=owner.getClass().getDeclaredField(field);f.setAccessible(true);f.set(owner,value);}
    static void write(File file,String text)throws Exception{file.getParentFile().mkdirs();Files.write(file.toPath(),text.getBytes(StandardCharsets.UTF_8));}
    static ReportCatalogCP4.Result catalog(File directory)throws Exception {
        File page=new File(directory,"catalog/catalog-0001.jsonl");StringBuilder text=new StringBuilder();
        for(CheckMetricCrosswalkR31.Check check:CheckMetricCrosswalkR31.checks())text.append(new JSONObject().put("kind","check_result").put("id",check.id).put("execution_state","COMPUTED").put("domain_outcome","INSUFFICIENT").put("health","UNKNOWN").put("completes_contract",false)).append('\n');
        write(page,text.toString());
        // Only the transport result envelope is constructed here. Each synthetic
        // row is explicitly insufficient, never a claim of executed product checks.
        Constructor<ReportCatalogCP4.Result> first=ReportCatalogCP4.Result.class.getDeclaredConstructor(long.class,long.class,long.class,long.class,long.class,long.class,boolean.class,boolean.class,boolean.class);first.setAccessible(true);
        ReportCatalogCP4.Result empty=first.newInstance(0L,0L,0L,96L,96L,0L,true,false,false);
        Constructor<ReportCatalogCP4.Result> with=ReportCatalogCP4.Result.class.getDeclaredConstructor(ReportCatalogCP4.Result.class,List.class);with.setAccessible(true);return with.newInstance(empty,Collections.singletonList(page));
    }
    static Map<String,Object> document(ReportJobJournalCP12.Job job){
        Map<String,Object> watermark=map("reportJobId",job.jobId,"sourceCommit",id("HOST_SYNTHETIC_SOURCE"),"sourceManifest",map("snapshotSha256",job.snapshotSha));
        List<Object> caps=new ArrayList<>(),tables=new ArrayList<>();for(CapabilityRegistryR30.Definition d:CapabilityRegistryR30.all())caps.add(d.json());for(String t:ResearchTableAdaptersR30R1.ids())tables.add(map("id",t,"status","NOT_RUN_HOST_TRANSPORT_FIXTURE"));
        Map<String,Object> data=map("capabilities",caps,"researchTablesR31",tables);data.put("objectIndex",ReportObjectIndexR30.buildProductionR31(map(),watermark,data));
        return ReportR29.document(ReportR29.fromSnapshotR30(new ReportR27.R30Input(map(),watermark,data,Collections.emptyList())));
    }
    static Object service()throws Exception{
        Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field f=unsafeType.getDeclaredField("theUnsafe");f.setAccessible(true);Object unsafe=f.get(null);
        Object service=unsafeType.getMethod("allocateInstance",Class.class).invoke(unsafe,ReportExportServiceCP4.class);
        set(service,"destroyed",new AtomicBoolean());return service;
    }
    static void phases(ReportJobJournalCP12 journal,ReportJobJournalCP12.Job job)throws Exception{
        for(ReportJobJournalCP12.Stage s:new ReportJobJournalCP12.Stage[]{ReportJobJournalCP12.Stage.BACKUP,ReportJobJournalCP12.Stage.SNAPSHOT_DURABLE,ReportJobJournalCP12.Stage.INDEXING,ReportJobJournalCP12.Stage.INDEX_DURABLE,ReportJobJournalCP12.Stage.COMPRESSING,ReportJobJournalCP12.Stage.VERIFYING})journal.transition(job,s,System.currentTimeMillis());
    }
    static void heldAdmissionChild(File app)throws Exception{
        File lock=new File(app,"files/r31-storage/admission.lock");lock.getParentFile().mkdirs();try(RandomAccessFile io=new RandomAccessFile(lock,"rw");FileLock lease=io.getChannel().lock()){
            System.out.println("ADMISSION_HELD");System.out.flush();System.in.read();
        }
    }
    public static void main(String[] args)throws Exception {
        File app=new File(args[0]);if(args.length>1&&args[1].equals("hold")){heldAdmissionChild(app);return;}
        File files=new File(app,"files");files.mkdirs();android.app.Service.filesR31=files;
        StorageBudgetR31 budget=new StorageBudgetR31(app,new StorageBudgetR31.Config(256*MiB,256*MiB,1024,2*MiB,"HOST_UNIFIED_REPORT_FIXTURE"),f->2L<<30);
        ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");
        String request=id("durable-request"),release=id("release");File directory;String zipSha;ReportJobJournalCP12.Job job;
        try(Closeable fixture=ReportIoR31.hostFixture(app,budget)){
            File root=ReportDirectoriesCP12.root(files);
            Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),R31UnifiedReportHostTest.class.getName(),app.getPath(),"hold").redirectError(ProcessBuilder.Redirect.INHERIT).start();
            try{
                need("ADMISSION_HELD".equals(new BufferedReader(new InputStreamReader(child.getInputStream())).readLine()),"ADMISSION_CHILD");long began=System.nanoTime();
                ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+request+".json"),new JSONObject().put("requestId",request).put("mode","FULL_REPORT").put("releaseHash",release).toString());
                ReportRequestQueueCP12.enqueue(root,request,"FULL_REPORT");need((System.nanoTime()-began)/1_000_000<4000,"CONTROL_INTENT_WAITED_FOR_BULK_CENSUS");
            }finally{child.getOutputStream().close();need(child.waitFor(5,java.util.concurrent.TimeUnit.SECONDS),"CONTROL_CHILD_RELEASE");child.destroyForcibly();}
            ReportRequestQueueCP12.remove(root,request);ReportRequestQueueCP12.reconcileRequestsR31(root);need(request.equals(ReportRequestQueueCP12.head(root)),"DURABLE_INTENT_NOT_RECOVERED_WITHOUT_FIFO");
            Field generationField=ReportExportServiceCP4.class.getDeclaredField("USER_REQUEST_GENERATION");generationField.setAccessible(true);
            AtomicLong generation=(AtomicLong)generationField.get(null);long superseded=generation.getAndIncrement();
            Method enqueue=ReportExportServiceCP4.class.getDeclaredMethod("enqueueCommittedRequestR31",File.class,String.class,String.class,long.class);enqueue.setAccessible(true);
            String cancelledRequest=id("superseded-before-enqueue");
            try{enqueue.invoke(null,root,cancelledRequest,"FULL_REPORT",superseded);throw new AssertionError("STOP_RACE_REQUEUED_OLD_GENERATION");}
            catch(InvocationTargetException expected){need(expected.getCause() instanceof IOException&&expected.getCause().getMessage().contains("SUPERSEDED"),"STOP_RACE_LOST_CAUSE");}
            need(!ReportRequestQueueCP12.pendingIds(root).contains(cancelledRequest),"STOP_RACE_MUTATED_FIFO");
            try(ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(files)){
                job=journal.recoverOrCreateR31(request,release,"FULL_REPORT");directory=journal.directory(job);job.snapshotSha=id("HOST_DIAGNOSTIC_NO_RAW_DB");job.publishedDir="archive-1";job.publishedQuotaBytes=128*MiB;
                StorageBudgetR31.Reservation reservation=budget.reserve("report:"+job.jobId,job.revision,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/r981_reports/jobs/"+job.jobId,64*MiB,256*MiB,true);
                Object service=service();Class<?> activeType=Class.forName(ReportExportServiceCP4.class.getName()+"$ActiveControl");Constructor<?> ctor=activeType.getDeclaredConstructor(ReportExportServiceCP4.class,ReportJobJournalCP12.Job.class,File.class);ctor.setAccessible(true);Object active=ctor.newInstance(service,job,journal.root());set(active,"storageReservationR31",reservation);
                ReportNativeCP4.Control control=(ReportNativeCP4.Control)active;
                try(ReportIoR31.Scope scope=ReportIoR31.bind(directory,control)){
                    ReportCatalogCP4.Result catalog=catalog(directory);Map<String,Object> document=document(job);
                    write(new File(directory,"R30_REPORT.json"),JsonR27.write(document));write(new File(directory,"R30_REPORT.md"),"HOST: insufficient evidence\n");write(new File(directory,"REPORT_DETAILS.txt"),"HOST synthetic transport, no executed product-check claim\n");write(new File(directory,"DIAGNOSTIC_SCOPE.json"),"{\"scope\":\"HOST_SYNTHETIC_NO_RAW_DB\"}");
                    for(String name:new String[]{"R26_REPORT.json","R26_FROZEN_SURFACE.json","R26_POLICY_IDENTITIES.jsonl"})write(new File(directory,name),"{\"scope\":\"HOST_SYNTHETIC_TRANSPORT_ONLY\",\"status\":\"NOT_RUN\"}\n");
                    for(String name:new String[]{"R27_REPORT.md","STORAGE_CENSUS.txt"})write(new File(directory,name),"HOST synthetic transport fixture; business evidence NOT_RUN\n");
                    ReportSemanticVerifierR31.writeReceipt(directory,document,catalog,job,false,control);job.semanticVerificationR31="REQUIRES_ZIP_READBACK";phases(journal,job);
                    File archive=new File(directory,job.publishedDir);archive.mkdir();try(ReportArchiveCP4 writer=new ReportArchiveCP4(archive,job.jobId,control)){
                        SortedMap<String,File> members=new TreeMap<>();for(String name:new String[]{"R30_REPORT.json","R30_REPORT.md","REPORT_DETAILS.txt","DIAGNOSTIC_SCOPE.json","R26_REPORT.json","R26_FROZEN_SURFACE.json","R26_POLICY_IDENTITIES.jsonl","R27_REPORT.md","STORAGE_CENSUS.txt",ReportSemanticVerifierR31.RECEIPT})members.put(name,new File(directory,name));
                        for(File page:catalog.indexFiles)members.put(page.getName(),page);for(Map.Entry<String,File> member:members.entrySet())writer.add(member.getKey(),member.getValue(),4*MiB,control);writer.finish(true,false,"HOST_SYNTHETIC_TRANSPORT_NOT_BUSINESS_HEALTH");
                    }
                    File ack=new File(root,"requests/"+request+".ack.json");Files.delete(ack.toPath());Files.createDirectory(ack.toPath());AtomicBoolean immediateGrant=new AtomicBoolean();AtomicReference<Throwable> readerError=new AtomicReference<>();
                    android.util.Log.ackHookR31=()->{
                        if(!"READY_TO_SHARE".equals(job.stage))return;
                        Thread reader=new Thread(()->{try{ReportPublicationCP12.grant(directory,System.currentTimeMillis());need(ReportPublicationCP12.verifyPublishedZip(directory,()->true),"IMMEDIATE_READER_ZIP");immediateGrant.set(true);}catch(Throwable error){readerError.set(error);}},"host-immediate-ready-consumer");
                        reader.start();try{reader.join(6000);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new RuntimeException(interrupted);}need(!reader.isAlive(),"IMMEDIATE_CONSUMER_BLOCKED");
                    };
                    Method publish=ReportExportServiceCP4.class.getDeclaredMethod("publish",ReportJobJournalCP12.Job.class,ReportJobJournalCP12.class,File.class,File.class,activeType);publish.setAccessible(true);
                    try{publish.invoke(service,job,journal,directory,archive,active);}catch(InvocationTargetException error){throw new RuntimeException("ACTUAL_PRODUCTION_PUBLISH_FAILED",error.getCause());}finally{android.util.Log.ackHookR31=null;}
                    need(readerError.get()==null,"READY_GRANT_RACE:"+readerError.get());need(immediateGrant.get(),"REAL_PUBLISH_DID_NOT_EXPOSE_READY_BEFORE_RETURN");need("ZIP_READBACK_VERIFIED".equals(job.semanticVerificationR31),"SEMANTIC_RECEIPT_NOT_COMMITTED");need(((List<?>)budget.snapshot().get("owners")).isEmpty(),"READY_PRECEDED_WORKSPACE_RESOURCE_RELEASE");
                    need("READY_TO_SHARE".equals(ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(directory,"job.json"),65536)).stage),"ACK_FAILURE_DEMOTED_READY");Files.delete(ack.toPath());
                }
                zipSha=ReportArchiveCP4.hash(new File(directory,"delivery/STARCORE_FULL_REPORT.zip"));ReportRequestQueueCP12.remove(root,request);
            }
            try(ReportJobJournalCP12 restarted=ReportJobJournalCP12.tryClaim(files)){
                ReportJobJournalCP12.Job recovered=restarted.recoverOrCreateR31(request,release,"FULL_REPORT");need(job.jobId.equals(recovered.jobId),"SECOND_REPORT_AFTER_LOST_ACK");restarted.reconcileAcknowledgementsR31();
                File ack=new File(root,"requests/"+request+".ack.json");need("READY_TO_SHARE".equals(new JSONObject(ReportJobJournalCP12.readAtomic(ack,8192)).getString("stage")),"ACK_OUTBOX_NOT_REPLAYED");
                write(ack,"{BROKEN_ACK");restarted.reconcileAcknowledgementsR31();need("READY_TO_SHARE".equals(new JSONObject(ReportJobJournalCP12.readAtomic(ack,8192)).getString("stage")),"CORRUPT_ACK_DID_NOT_RECOVER");
                need(DirectoryCursorR31.children(new File(root,"quarantine"),10).length==1,"CORRUPT_ACK_ORIGINAL_NOT_PRESERVED");need(zipSha.equals(ReportArchiveCP4.hash(new File(directory,"delivery/STARCORE_FULL_REPORT.zip"))),"RECOVERY_CHANGED_PUBLISHED_ZIP");
                ReportSemanticVerifierR31.verifyDelivery(directory,recovered,()->true);
                File receipt=new File(directory,ReportSemanticVerifierR31.RECEIPT);String original=ReportJobJournalCP12.readAtomic(receipt,65536);JSONObject tampered=new JSONObject(original);tampered.put("catalogHashXor",id("incorrect"));write(receipt,tampered.toString());
                try{ReportSemanticVerifierR31.verifyDelivery(directory,recovered,()->true);throw new AssertionError("TAMPERED_SEMANTIC_RECEIPT_ACCEPTED");}catch(IOException expected){need(expected.getMessage().contains("SEMANTIC"),"ORIGINAL_SEMANTIC_FAILURE_LOST");}write(receipt,original);
                ReportSemanticVerifierR31.verifyDelivery(directory,recovered,()->true);need(DirectoryCursorR31.children(new File(root,"jobs"),10).length==1,"ACK_RECOVERY_DUPLICATED_BACKUP_JOB");
            }
        }
        System.out.println("UNIFIED_PRODUCTION_PUBLISH_HOST_PASS assertions="+assertions+" sameZip="+zipSha+" REAL_NATIVE_ARCHIVE_ZIP_ACK_OUTBOX_AND_IMMEDIATE_READY_CONSUMER; HOST_FRAMEWORK_TRANSPORT_ADAPTED; DEVICE_NOT_RUN; SYNTHETIC_CHECK_INPUTS_NOT_96_EXECUTED_BUSINESS_CHECKS");
    }
}
