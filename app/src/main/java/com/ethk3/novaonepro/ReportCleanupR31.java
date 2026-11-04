package com.ethk3.novaonepro;

import java.io.*;
import java.util.*;
import org.json.*;

/** Same report journal owner, separate cleanup state. READY never means raw cleanup succeeded. */
public final class ReportCleanupR31 {
    private ReportCleanupR31(){}
    private static final String RECORD="cleanup-r31.json";
    private static final long MAX_RETRY=600000L;
    public static void enqueue(ReportJobJournalCP12 journal,ReportJobJournalCP12.Job job,String reason)throws Exception{
        journal.assertOwned();if(!"FULL_REPORT".equals(job.mode)||!"READY_TO_SHARE".equals(job.stage))return;
        enrollReadyDirectoryR31(journal.directory(job),job,reason);
    }
    static void enrollReadyDirectoryR31(File directory,ReportJobJournalCP12.Job job)throws Exception{enrollReadyDirectoryR31(directory,job,"LEGACY_READY_CLEANUP_ENROLLED");}
    private static void enrollReadyDirectoryR31(File directory,ReportJobJournalCP12.Job job,String reason)throws Exception{
        if(!"FULL_REPORT".equals(job.mode)||!"READY_TO_SHARE".equals(job.stage)||!directory.getName().equals(job.jobId))return;
        File record=new File(directory,RECORD);if(record.isFile())return;
        ReportJobJournalCP12.writeAtomic(record,pending(job.jobId,reason).toString());
    }
    private static JSONObject pending(String id,String reason)throws JSONException{
        return new JSONObject().put("format","R31_REPORT_CLEANUP_V1").put("jobId",id).put("state","CLEANUP_PENDING")
                .put("attempts",0).put("nextWallMs",0).put("nextMonotonicNanos",0).put("boot",StorageBudgetR31.boot())
                .put("reason",bounded(reason)).put("retainedWorkspaceBytes",JSONObject.NULL).put("reclaimedBytes",0)
                .put("measurement","REGULAR_FILE_LENGTH; ALLOCATED_BLOCKS_UNKNOWN; RECEIPT_WRITE_EXCLUDED")
                .put("resourcesClosed",false);
    }
    /** Invoke only before opening work inputs or after the outer resource scope has closed. */
    public static int runDue(ReportJobJournalCP12 journal,long nowWall,int maximum)throws Exception{
        journal.assertOwned();if(maximum<1||maximum>4)throw new IllegalArgumentException("REPORT_CLEANUP_BOUND");
        if(ReportInputsLeaseCP12.currentThreadHoldsInput())throw new IOException("REPORT_CLEANUP_INPUTS_STILL_OPEN");
        File[] jobs=DirectoryCursorR31.children(new File(journal.root(),"jobs"),1024);Arrays.sort(jobs);int handled=0;
        for(File directory:jobs){
            if(handled>=maximum)break;if(!directory.isDirectory()||!directory.getName().matches("[0-9a-f]{64}")||!directory.getAbsoluteFile().equals(directory.getCanonicalFile()))continue;
            File record=new File(directory,RECORD);if(!record.isFile())continue;
            JSONObject row;
            try{row=new JSONObject(ReportJobJournalCP12.readAtomic(record,16384));
                if(!directory.getName().equals(row.getString("jobId")))throw new IOException("REPORT_CLEANUP_IDENTITY");
            }catch(Exception corrupt){
                // This metadata is reconstructible. Preserve every payload; isolate a bad row from other jobs.
                row=pending(directory.getName(),"CLEANUP_RECORD_CORRUPT_PAYLOADS_PROTECTED:"+corrupt.getClass().getSimpleName());
                defer(row,nowWall);ReportJobJournalCP12.writeAtomic(record,row.toString());handled++;continue;
            }
            if(!"CLEANUP_PENDING".equals(row.optString("state")))continue;
            if(delay(row,nowWall,System.nanoTime())>0)continue;
            handled++;row.put("attempts",Math.min(1000000,row.optInt("attempts",0)+1)).put("resourcesClosed",true).put("attemptWallMs",nowWall);
            long before=-1;
            try{
                before=ReportPublicationCP12.jobPhysicalBytes(directory);row.put("beforePhysicalBytes",before);
                ReportJobJournalCP12.Job job=ReportJobJournalCP12.Job.parse(ReportJobJournalCP12.readAtomic(new File(directory,"job.json"),65536));
                if(!"READY_TO_SHARE".equals(job.stage)||!new File(directory,"published.json").isFile())throw new IOException("REPORT_CLEANUP_UNPUBLISHED_PROTECTED");
                ReportPublicationCP12.cleanRaw(directory,job.publishedDir);
                long after=ReportPublicationCP12.jobPhysicalBytes(directory);
                row.put("state","CLEANUP_COMPLETE").put("afterPhysicalBytes",after).put("reclaimedBytes",Math.max(0,before-after))
                        .put("retainedWorkspaceBytes",after).put("reason","VERIFIED_DELIVERY_PRESERVED_PRIVATE_WORKSPACE_RELEASED");
            }catch(Exception failure){
                try{row.put("retainedWorkspaceBytes",ReportPublicationCP12.jobPhysicalBytes(directory));}catch(IOException unknown){row.put("retainedWorkspaceBytes",JSONObject.NULL);}
                row.put("reason",bounded(failure.getClass().getSimpleName()+":"+failure.getMessage()));defer(row,nowWall);
            }
            // A receipt failure is a cleanup failure; callers must never demote the already-published job.
            ReportJobJournalCP12.writeAtomic(record,row.toString());
        }return handled;
    }
    private static void defer(JSONObject row,long wall)throws JSONException{
        long wait=Math.min(MAX_RETRY,1000L<<Math.min(9,Math.max(0,row.optInt("attempts",0))));
        row.put("state","CLEANUP_PENDING").put("nextWallMs",wall+wait).put("nextMonotonicNanos",System.nanoTime()+wait*1000000L).put("boot",StorageBudgetR31.boot());
    }
    private static long delay(JSONObject row,long wall,long monotonic){
        String boot=StorageBudgetR31.boot();
        if(!boot.equals("UNKNOWN")&&boot.equals(row.optString("boot")))return (row.optLong("nextMonotonicNanos",0)-monotonic)/1000000L;
        return Math.min(MAX_RETRY,row.optLong("nextWallMs",0)-wall);
    }
    public static long nextDelayMs(File root)throws Exception{
        File[] jobs=DirectoryCursorR31.children(new File(root,"jobs"),1024);long wait=Long.MAX_VALUE;
        for(File directory:jobs){File file=new File(directory,RECORD);if(!file.isFile())continue;
            try{JSONObject row=new JSONObject(ReportJobJournalCP12.readAtomic(file,16384));if("CLEANUP_PENDING".equals(row.optString("state")))wait=Math.min(wait,Math.max(1000,Math.min(MAX_RETRY,delay(row,System.currentTimeMillis(),System.nanoTime()))));}
            catch(Exception corrupt){wait=Math.min(wait,MAX_RETRY);}
        }return wait==Long.MAX_VALUE?-1:wait;
    }
    private static String bounded(String reason){String value=reason==null?"UNKNOWN":reason;return value.substring(0,Math.min(512,value.length()));}
}
