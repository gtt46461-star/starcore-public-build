package com.ethk3.novaonepro;

import java.io.*;
import java.util.*;
import org.json.*;

/** Durable user report controls. No database, model, retention archive or external SAF deletion. */
public final class ReportTaskControlR69 {
    private ReportTaskControlR69() {}
    private static File path(File root,String name)throws IOException {
        return ReportArchiveVerifierCP12.inside(root,"control-r69/"+name);
    }
    private static void id(String id)throws IOException {
        if(id==null||!id.matches("[0-9a-f]{64}"))throw new IOException("REPORT_CONTROL_ID");
    }
    /** Queue snapshot first: a claimed request is durable as a job before leaving the FIFO. */
    public static String request(File files,boolean clear)throws Exception {
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
        for(String request:requests){id(request);ReportJobJournalCP12.writeAtomic(path(root,"cancel-"+request+".json"),new JSONObject().put("requestId",request).toString());}
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
                    if(clear||active(job.stage)){
                        job.stage="CANCELLED";job.lastReason=clear?"REPORT_REMOVED_BY_USER":"REPORT_USER_CANCEL";
                        job.updatedWallMs=System.currentTimeMillis();job.revision++;
                        ReportJobJournalCP12.writeAtomic(new File(directory,"job.json"),job.json().toString());journal.acknowledge(job.requestId,job);
                    }
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
