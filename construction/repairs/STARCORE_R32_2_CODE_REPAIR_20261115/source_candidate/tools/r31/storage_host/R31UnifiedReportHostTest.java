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
    static void observationWakeControl(File files)throws Exception{
        files.mkdirs();File root=ReportDirectoriesCP12.root(files);String request=id("HOST_OBSERVER_WAKE_WITH_NO_OS_ALARM");
        JSONObject intent=new JSONObject().put("requestId",request).put("mode","FULL_REPORT").put("releaseHash",id("release")).put("createdWallMs",System.currentTimeMillis()-4500).put("observationState","UNKNOWN_NOT_COLLECTED");
        ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+request+".json"),intent.toString());ReportRequestQueueCP12.reconcileRequestsR31(root);
        try(ReportJobJournalCP12 first=ReportJobJournalCP12.tryClaim(files)){need(first!=null,"PREPARATION_OWNER_MISSING");need(first.current()==null,"PREPARATION_ALREADY_CREATED_JOB");}
        ReportExportServiceCP4 owner=(ReportExportServiceCP4)service();AtomicBoolean destroyed=new AtomicBoolean(),queued=new AtomicBoolean();
        java.util.concurrent.atomic.AtomicInteger submissions=new java.util.concurrent.atomic.AtomicInteger();
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.CountDownLatch resumed=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ScheduledThreadPoolExecutor timer=new java.util.concurrent.ScheduledThreadPoolExecutor(1);
        java.util.concurrent.ThreadPoolExecutor worker=new java.util.concurrent.ThreadPoolExecutor(1,1,0,java.util.concurrent.TimeUnit.MILLISECONDS,new java.util.concurrent.ArrayBlockingQueue<>(1)){
            @Override public java.util.concurrent.Future<?> submit(Runnable originalDrain){
                // HOST transport substitutes only the Android drain invocation.
                // Reclaim the actual released file owner and its durable FIFO;
                // this is wake dispatch, never a synthetic report acceptance.
                submissions.incrementAndGet();java.util.concurrent.CompletableFuture<Void> done=new java.util.concurrent.CompletableFuture<>();
                try(ReportJobJournalCP12 next=ReportJobJournalCP12.tryClaim(files)){
                    need(next!=null,"OBSERVER_TIMER_RETAINED_JOURNAL_LEASE");need(request.equals(ReportRequestQueueCP12.head(root)),"WAKE_LOST_OR_REPLACED_DURABLE_REQUEST");
                    ReportJobJournalCP12.Job job=next.recoverOrCreateR31(request,id("release"),"FULL_REPORT");next.acknowledge(request,job);
                    need("UNKNOWN_NOT_COLLECTED".equals(new JSONObject(ReportJobJournalCP12.readAtomic(new File(root,"requests/"+request+".json"),65536)).getString("observationState")),"WAKE_MANUFACTURED_OBSERVATION");
                    done.complete(null);
                }catch(Throwable error){failure.set(error);done.completeExceptionally(error);}finally{resumed.countDown();}
                return done;
            }
        };
        set(owner,"destroyed",destroyed);set(owner,"drainQueued",queued);set(owner,"worker",worker);set(owner,"controlTimer",timer);
        try{
            for(int i=0;i<100;i++){need(owner.scheduleObservationRetryR31(500),"LIVE_PREPARATION_RETRY_NOT_ADMITTED");need(timer.getQueue().size()<=1,"OBSERVATION_TIMER_QUEUE_UNBOUNDED");}
            need(resumed.await(3,java.util.concurrent.TimeUnit.SECONDS),"DURABLE_INTENT_WAITED_FOR_UNDELIVERED_OS_ALARM");
            need(failure.get()==null,"WAKE_JOURNAL_RECLAIM_FAILED:"+failure.get());need(submissions.get()==1,"PREPARATION_WAKE_SUBMITTED_MULTIPLE_DRAINS");
            destroyed.set(true);need(!owner.scheduleObservationRetryR31(1),"DESTROYED_REPORT_OWNER_RESTARTED_WORK");
            System.out.println("R31_OBSERVER_WAKE_HOST_CONTROL_PASS actual_control_timer=true no_OS_alarm_delivery=true released_journal_reclaimed=true one_pending_timer=true same_durable_request=true unknown_observation_preserved=true scope=HOST_DISPATCH_NOT_ANDROID_SERVICE_OR_REPORT_ACCEPTANCE");
        }finally{destroyed.set(true);timer.shutdownNow();worker.shutdownNow();}
    }
    public static void main(String[] args)throws Exception {
        JSONObject recoveredObservation=ReportAttachmentClosureCP12.requestObservationR31(new JSONObject());
        need("UNKNOWN_NOT_COLLECTED".equals(recoveredObservation.getString("observationState")),"LOST_OPTIONAL_OBSERVER_NOT_UNKNOWN");
        need(!recoveredObservation.has("researchSchemaReady")&&!recoveredObservation.has("dbWaitMs"),"MISSING_OBSERVER_FABRICATED_HEALTH_OR_ZERO");
        JSONObject originalObservation=new JSONObject().put("researchSchemaReady",false).put("dbWaitMs",37);
        JSONObject frozenObservation=ReportAttachmentClosureCP12.requestObservationR31(new JSONObject().put("observation",originalObservation));
        originalObservation.put("dbWaitMs",99);
        need(!frozenObservation.getBoolean("researchSchemaReady")&&frozenObservation.getLong("dbWaitMs")==37,"OPTIONAL_OBSERVATION_NOT_FROZEN_BY_VALUE");
        boolean malformedRefused=false;try{ReportAttachmentClosureCP12.requestObservationR31(new JSONObject().put("observation","damaged"));}catch(IOException expected){malformedRefused="REPORT_REQUEST_OBSERVATION_TYPE".equals(expected.getMessage());}
        need(malformedRefused,"CORRUPT_OPTIONAL_OBSERVATION_ACCEPTED");
        JSONObject preparing=new JSONObject().put("mode","FULL_REPORT").put("observationState","UNKNOWN_NOT_COLLECTED").put("createdWallMs",10000);
        need(ReportExportServiceCP4.observationPreparationPendingR31(preparing,10000)&&ReportExportServiceCP4.observationPreparationPendingR31(preparing,14999),"OBSERVER_GRACE_NOT_BOUNDED");
        need(!ReportExportServiceCP4.observationPreparationPendingR31(preparing,15000)&&!ReportExportServiceCP4.observationPreparationPendingR31(preparing,9999),"OBSERVER_DEATH_OR_CLOCK_ROLLBACK_DEADLOCK");
        preparing.put("observationState","COLLECTED_OUTSIDE_SQL_WATERMARK");
        need(!ReportExportServiceCP4.observationPreparationPendingR31(preparing,10000),"COLLECTED_OBSERVATION_UNNECESSARILY_WAITED");
        File app=new File(args[0]);if(args.length>1&&args[1].equals("hold")){heldAdmissionChild(app);return;}
        File files=new File(app,"files");files.mkdirs();android.app.Service.filesR31=files;
        observationWakeControl(new File(app,"observer-wake/files"));
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
                zipSha=ReportArchiveCP4.hash(new File(directory,"delivery/STARCORE_FULL_REPORT.zip"));
                try(RandomAccessFile cleanup=new RandomAccessFile(new File(directory,"share.lock"),"rw");java.nio.channels.FileLock exclusive=cleanup.getChannel().lock()){
                    try{ReportPublicationCP12.grant(directory,System.currentTimeMillis());throw new AssertionError("EXCLUSIVE_CLEANUP_GRANTED_READER");}
                    catch(ReportPublicationCP12.ShareBusyR31 expected){need(expected.getCause() instanceof java.nio.channels.OverlappingFileLockException,"SHARE_CONFLICT_CAUSE_LOST");}
                }
                ReportPublicationCP12.grant(directory,System.currentTimeMillis());
                need(zipSha.equals(ReportArchiveCP4.hash(new File(directory,"delivery/STARCORE_FULL_REPORT.zip"))),"TRANSIENT_LEASE_RETRY_CHANGED_PUBLISHED_ZIP");
                ReportRequestQueueCP12.remove(root,request);
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
            String clear=ReportTaskControlR69.request(files,true);
            try(ReportJobJournalCP12 clearing=ReportJobJournalCP12.tryClaim(files)){
                ReportTaskControlR69.applyPending(clearing);
                need("CONTROL_DONE".equals(ReportTaskControlR69.status(files,clear).getString("stage")),"EXPLICIT_CLEAR_DID_NOT_FINISH");
                String next=id("new-export-after-explicit-clear");
                ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+next+".json"),new JSONObject().put("requestId",next).put("mode","FULL_REPORT").put("releaseHash",release).toString());
                ReportRequestQueueCP12.enqueue(root,next,"FULL_REPORT");
                clearing.reconcileAcknowledgementsR31();ReportRequestQueueCP12.reconcileRequestsR31(root);ReportTaskControlR69.applyPending(clearing);
                ReportJobJournalCP12.Job fresh=clearing.recoverOrCreateR31(next,release,"FULL_REPORT");
                need(next.equals(fresh.requestId)&&"FULL_REPORT".equals(fresh.mode)&&!job.jobId.equals(fresh.jobId),"CLEAR_RESTART_REUSED_OLD_OR_MALFORMED_JOB");
                clearing.acknowledge(next,fresh);
            }
        }
        System.out.println("UNIFIED_PRODUCTION_PUBLISH_HOST_PASS assertions="+assertions+" sameZip="+zipSha+" REAL_NATIVE_ARCHIVE_ZIP_ACK_OUTBOX_AND_IMMEDIATE_READY_CONSUMER; HOST_FRAMEWORK_TRANSPORT_ADAPTED; DEVICE_NOT_RUN; SYNTHETIC_CHECK_INPUTS_NOT_96_EXECUTED_BUSINESS_CHECKS");
    }
}
