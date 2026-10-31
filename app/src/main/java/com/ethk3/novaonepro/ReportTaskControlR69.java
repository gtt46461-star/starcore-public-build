package com.ethk3.novaonepro;

import java.io.*;
import java.util.*;
import org.json.*;

/** Durable user report controls. No database, model, retention archive or external SAF deletion. */
public final class ReportTaskControlR69 {
    private ReportTaskControlR69() {}
    public static final int CHECKPOINT_MS_R30=100;
    private static final String BOOT_R30=readBootR30();
    static String readBootR30(){
        try(InputStream in=new FileInputStream("/proc/sys/kernel/random/boot_id")){byte[] b=new byte[64];int n=in.read(b);String value=n>0?new String(b,0,n,java.nio.charset.StandardCharsets.US_ASCII).trim():"";return value.matches("[0-9a-f-]{36}")?value:"UNKNOWN";}
        catch(IOException denied){return "UNKNOWN";}
    }
    /** Preserve the first valid request time. Wall clocks never substitute for a same-boot monotonic interval. */
    static void stampCancellationR30(JSONObject row,File previous,long requestedNanos)throws Exception{
        long first=requestedNanos;
        if(previous.isFile())try{JSONObject old=new JSONObject(ReportJobJournalCP12.readAtomic(previous,4096));
            String key=row.has("jobId")?"jobId":"requestId";long at=old.optLong("requestedNanosR30",-1);
            if(row.getString(key).equals(old.optString(key))&&BOOT_R30.equals(old.optString("bootR30"))&&at>0&&at<first)first=at;
        }catch(Exception malformed){/* An explicit retry writes a valid exact-target command; corrupt old time is not used. */}
        row.put("requestedNanosR30",first).put("bootR30",BOOT_R30);
    }
    static Long firstRequestNanosR30(File root,String request,String job)throws Exception{
        if("UNKNOWN".equals(BOOT_R30))return null;Long earliest=null;long now=System.nanoTime();
        for(String relative:new String[]{"control-r69/cancel-"+request+".json","control-r69/cancel-"+job+".json","cancel-commands/"+job+".json"}){
            File file=ReportArchiveVerifierCP12.inside(root,relative);if(!file.isFile())continue;
            JSONObject row=new JSONObject(ReportJobJournalCP12.readAtomic(file,4096));String key=row.has("jobId")?"jobId":"requestId";
            String identity=row.getString(key);if(!identity.equals(request)&&!identity.equals(job))throw new IOException("R30_CANCEL_TIME_IDENTITY");
            long at=row.optLong("requestedNanosR30",-1);if(!BOOT_R30.equals(row.optString("bootR30"))||at<=0||at>now)continue;
            if(earliest==null||at<earliest)earliest=at;
        }return earliest;
    }
    /** Sole owner calls only after its work resources have closed; this removes derived private files for this job. */
    public static void finishCancellationR30(File root,File directory,ReportJobJournalCP12.Job job)throws Exception{
        if(!"CANCELLED".equals(job.stage)||!"FULL_REPORT".equals(job.mode))return;
        ReportPublicationCP12.cleanCancelledR30(directory);
        File receipt=new File(directory,"cancel-receipt-r30.json");if(receipt.exists())return;
        Long request=firstRequestNanosR30(root,job.requestId,job.jobId);long released=System.nanoTime();Long elapsed=request==null?null:(released-request)/1_000_000L;
        JSONObject row=new JSONObject().put("jobId",job.jobId).put("resourcesReleased",true).put("privateTemporaryFilesCleaned",!new File(directory,"published.json").exists())
            .put("cancelRequestToReleaseMs",elapsed==null?JSONObject.NULL:elapsed).put("targetMs",2000).put("targetMet",elapsed==null?JSONObject.NULL:elapsed<=2000)
            .put("clock","SAME_OS_BOOT_MONOTONIC; UNKNOWN_IF_BOOT_ID_OR_REQUEST_TIME_UNAVAILABLE").put("scope","ACTUAL_CURRENT_RUNTIME; HOST_FIXTURES_DO_NOT_BECOME_PHONE_RESULTS");
        ReportJobJournalCP12.writeAtomic(receipt,row.toString());
    }
    private static File path(File root,String name)throws IOException {
        return ReportArchiveVerifierCP12.inside(root,"control-r69/"+name);
    }
    private static void id(String id)throws IOException {
        if(id==null||!id.matches("[0-9a-f]{64}"))throw new IOException("REPORT_CONTROL_ID");
    }
    /** Queue snapshot first: a claimed request is durable as a job before leaving the FIFO. */
    public static String request(File files,boolean clear)throws Exception {
        long requestedNanosR30=System.nanoTime();
        File root=ReportDirectoriesCP12.root(files);if(!root.isDirectory()&&!root.mkdirs())throw new IOException("REPORT_CONTROL_ROOT");
        Set<String> requests=new TreeSet<>();JSONArray queued=ReportRequestQueueCP12.pending(root);
        for(int i=0;i<queued.length();i++){
            JSONObject row=queued.getJSONObject(i);
            if(!"RETENTION_PAGE".equals(row.getString("mode")))requests.add(row.getString("requestId"));
        }
        File[] jobs=new File(root,"jobs").listFiles();
        if(jobs!=null)for(File directory:jobs){
            id(directory.getName());
            ReportJobJournalCP12.Job job=readJob(directory);
            if("FULL_REPORT".equals(job.mode)&&(clear||active(job.stage)))requests.add(job.requestId);
        }
        if(requests.size()>512)throw new IOException("REPORT_CONTROL_REQUEST_BOUND");
        String command=RetentionArchiveProtocolCP12.sha(("REPORT_CONTROL_R69|"+UUID.randomUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        JSONObject body=new JSONObject().put("id",command).put("clear",clear).put("requests",new JSONArray(requests))
                .put("createdWallMs",System.currentTimeMillis());
        ReportJobJournalCP12.writeAtomic(path(root,"command-"+command+".json"),body.toString());
        for(String request:requests){id(request);File marker=path(root,"cancel-"+request+".json");JSONObject cancellation=new JSONObject().put("requestId",request);stampCancellationR30(cancellation,marker,requestedNanosR30);ReportJobJournalCP12.writeAtomic(marker,cancellation.toString());}
        return command;
    }
    public static boolean cancelled(File root,String request,String transfer)throws Exception {
        id(request);id(transfer);
        for(String key:new String[]{request,transfer}){
            File marker=path(root,"cancel-"+key+".json");
            if(marker.exists()){
                if(!key.equals(new JSONObject(ReportJobJournalCP12.readAtomic(marker,4096)).getString("requestId")))throw new IOException("REPORT_CONTROL_CANCEL_IDENTITY");
                return true;
            }
        }
        return false;
    }
    public static JSONObject status(File files,String command)throws Exception {
        id(command);File root=ReportDirectoriesCP12.root(files),status=path(root,"status-"+command+".json");
        return status.exists()?new JSONObject(ReportJobJournalCP12.readAtomic(status,4096)):
                new JSONObject().put("id",command).put("stage","STOPPING");
    }
    public static boolean hasPending(File root)throws Exception {
        File[] files=path(root,"").listFiles();if(files==null)return false;
        for(File file:files)if(file.getName().matches("command-[0-9a-f]{64}\\.json"))return true;
        return false;
    }
    /** Only the sole export owner runs deletion after its previous worker released all streams. */
    public static void applyPending(ReportJobJournalCP12 journal)throws Exception {
        journal.assertOwned();File root=journal.root();ReportPublicationCP12.finishUserDeletions(root);
        File[] commands=path(root,"").listFiles();if(commands==null)return;
        Arrays.sort(commands);
        for(File command:commands){
            if(!command.getName().matches("command-[0-9a-f]{64}\\.json"))continue;
            String control=command.getName().substring(8,72);JSONObject result=new JSONObject().put("id",control);
            try {
                JSONObject body=new JSONObject(ReportJobJournalCP12.readAtomic(command,65536));
                if(!control.equals(body.getString("id")))throw new IOException("REPORT_CONTROL_COMMAND_IDENTITY");
                JSONArray list=body.getJSONArray("requests");if(list.length()>512)throw new IOException("REPORT_CONTROL_REQUEST_BOUND");
                Set<String> requests=new HashSet<>();for(int i=0;i<list.length();i++){String request=list.getString(i);id(request);requests.add(request);}
                boolean clear=body.getBoolean("clear"),waiting=false;
                for(String request:requests){
                    // Exact removal works for queued full exports and SAF transfers, not only FIFO head.
                    ReportRequestQueueCP12.remove(root,request);
                    JSONObject ack=new JSONObject().put("requestId",request).put("stage","CANCELLED").put("state","CANCELLED")
                            .put("reason",clear?"REPORT_REMOVED_BY_USER":"REPORT_USER_CANCEL");
                    ReportJobJournalCP12.writeAtomic(new File(root,"requests/"+request+".ack.json"),ack.toString());
                    File saf=new File(root,"saf/"+request+".json");
                    if(saf.exists()){
                        JSONObject transfer=new JSONObject(ReportJobJournalCP12.readAtomic(saf,65536));
                        transfer.put("state","CANCELLED").put("reason","REPORT_USER_CANCEL").put("allVolumesSaved",false);
                        ReportJobJournalCP12.writeAtomic(saf,transfer.toString());
                    }
                }
                File[] jobs=new File(root,"jobs").listFiles();
                if(jobs!=null)for(File directory:jobs){
                    id(directory.getName());ReportJobJournalCP12.Job job=readJob(directory);
                    if(!requests.contains(job.requestId)||!"FULL_REPORT".equals(job.mode))continue;
                    boolean stopPrivateR30=!clear&&active(job.stage);
                    if(clear||active(job.stage)){
                        job.stage="CANCELLED";job.lastReason=clear?"REPORT_REMOVED_BY_USER":"REPORT_USER_CANCEL";
                        job.updatedWallMs=System.currentTimeMillis();job.revision++;
                        ReportJobJournalCP12.writeAtomic(new File(directory,"job.json"),job.json().toString());journal.acknowledge(job.requestId,job);
                    }
                    if(stopPrivateR30)finishCancellationR30(root,directory,job);
                    if(clear){journal.forgetCurrent(job.jobId);if(!ReportPublicationCP12.discardByUser(directory))waiting=true;}
                }
                result.put("stage",waiting?"WAITING_FOR_READERS":"CONTROL_DONE").put("clear",clear).put("requests",requests.size());
                ReportJobJournalCP12.writeAtomic(path(root,"status-"+control+".json"),result.toString());
                if(!waiting){
                    if(!command.delete())throw new IOException("REPORT_CONTROL_COMMAND_CLEANUP");
                    for(String request:requests){File marker=path(root,"cancel-"+request+".json");if(marker.exists()&&!marker.delete())throw new IOException("REPORT_CONTROL_MARKER_CLEANUP");}
                }
            }catch(Exception failure){
                result.put("stage","CONTROL_FAILED").put("reason",failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage()));
                ReportJobJournalCP12.writeAtomic(path(root,"status-"+control+".json"),result.toString());
                if(!command.delete())throw new IOException("REPORT_CONTROL_FAILED_COMMAND_CLEANUP",failure);
            }
        }
        // Bounded small control receipts; pending commands and cancellation markers are never pruned.
        File[] files=path(root,"").listFiles();List<File> receipts=new ArrayList<>();if(files!=null)for(File file:files)
            if(file.getName().matches("status-[0-9a-f]{64}\\.json")&&!path(root,"command-"+file.getName().substring(7)).exists())receipts.add(file);
        receipts.sort((a,b)->Long.compare(b.lastModified(),a.lastModified()));
        for(int i=64;i<receipts.size();i++)if(!receipts.get(i).delete())throw new IOException("REPORT_CONTROL_RECEIPT_CLEANUP");
    }
    private static ReportJobJournalCP12.Job readJob(File directory)throws Exception {
        ReportJobJournalCP12.Job job=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(ReportArchiveVerifierCP12.inside(directory,"job.json"),65536));
        if(!directory.getName().equals(job.jobId))throw new IOException("REPORT_CONTROL_JOB_IDENTITY");return job;
    }
    private static boolean active(String stage){return !stage.equals("READY_TO_SHARE")&&!stage.equals("FAILED")&&!stage.equals("CANCELLED");}
}
