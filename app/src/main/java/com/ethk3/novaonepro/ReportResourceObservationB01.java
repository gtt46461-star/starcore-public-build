package com.ethk3.novaonepro;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/** Optional bounded last-rejection evidence. The same typed rejection supplies every admission fact. */
public final class ReportResourceObservationB01 {
    public static final String NAME="resource-observation-b01.json";
    private ReportResourceObservationB01(){}
    public static void record(File root,ReportJobJournalCP12.Job job,long rejectedRevision,String failedPhase,Throwable error){
        try{
            Map<String,Object> admission=new LinkedHashMap<>();StorageBudgetR31.Rejected original=null;
            for(int n=0;error!=null&&n<16;n++){if(error instanceof StorageBudgetR31.Rejected){original=(StorageBudgetR31.Rejected)error;break;}
                Throwable next=error.getCause();if(next==error)break;error=next;}
            admission.put("status",original==null?"NO_TYPED_ADMISSION_TICKET":"REJECTED_EXACT_TICKET");
            if(original!=null){admission.putAll(original.admissionFactsB01);admission.put("reason",original.reason);
                admission.put("owner",original.owner);admission.put("fence",original.fence);admission.put("operation",original.admissionOperation);}
            admission.put("jobRevisionAtRejection",rejectedRevision);admission.put("clockKind","JAVA_SYSTEM_NANOTIME");
            if(original==null||original.admissionFactsB01.isEmpty())admission.put("unavailableReason","LOCK_OR_METADATA_ADMISSION_HAS_NO_CENSUS_TICKET");
            Map<String,Object> report=new LinkedHashMap<>();report.put("stage",job.stage);report.put("failedPhase",failedPhase);
            report.put("resumeStage",job.resumeStage);report.put("lastReason",job.lastReason);report.put("functionalFailureAttempts",job.failureAttempts);
            report.put("waitAttempt",job.resourceWaitR335.attempt);report.put("nextRetryElapsedMs",job.resourceWaitR335.nextElapsedMs);
            report.put("retryBoot",job.resourceWaitR335.boot);report.put("snapshotBytes",job.snapshotBytes);report.put("snapshotSha256",job.snapshotSha);
            Map<String,Object> body=new LinkedHashMap<>();body.put("format","STARCORE_REPORT_RESOURCE_B01_V2");
            body.put("scope","LAST_REJECTION_FILESYSTEM_INTERVAL_NOT_SQL_SNAPSHOT");body.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);
            body.put("releaseSha256",job.releaseHash);body.put("apkSha256",null);body.put("apkUnavailableReason","NOT_MEASURED_AT_REJECTION");
            body.put("jobId",job.jobId);body.put("requestId",job.requestId);body.put("publishedJobRevision",job.revision);
            body.put("observedWallMs",System.currentTimeMillis());body.put("observedElapsedMs",android.os.SystemClock.elapsedRealtime());
            body.put("bootId",ReportTaskControlR69.readBootR30());body.put("pid",android.os.Process.myPid());
            body.put("observationClockKind","ANDROID_ELAPSED_REALTIME");body.put("report",report);body.put("admission",admission);
            Map<String,Object> unknown=new LinkedHashMap<>();unknown.put("state","UNKNOWN_NOT_SAMPLED_AT_REJECTION");
            body.put("cleanup",unknown);body.put("checkpoint",unknown);
            String bytes=com.ethk3.novaonepro.strategyv2.JsonR27.write(body);
            if(ReportIoR31.utf8Bytes(bytes)>16384)throw new java.io.IOException("REPORT_RESOURCE_OBSERVATION_BOUND");
            ReportJobJournalCP12.writeAtomic(ReportArchiveVerifierCP12.inside(root,"jobs/"+job.jobId+"/"+NAME),bytes);
        }catch(Exception unavailable){android.util.Log.w("ReportResourceB01","Optional last-rejection evidence unavailable",unavailable);}
    }
    /** Only same-job/build earlier revisions are published; absence and stale identity stay explicit. */
    public static Map<String,Object> readForReport(File root,ReportJobJournalCP12.Job job){
        Map<String,Object> result=new LinkedHashMap<>();result.put("state","NO_REJECTION_OBSERVATION_IN_THIS_JOB");
        try{
            File file=ReportArchiveVerifierCP12.inside(root,"jobs/"+job.jobId+"/"+NAME);if(!file.isFile())return result;
            String original=ReportJobJournalCP12.readAtomic(file,16384);JSONObject checked=new JSONObject(original);
            if(!job.jobId.equals(checked.getString("jobId"))||!job.requestId.equals(checked.getString("requestId"))
                    ||!job.releaseHash.equals(checked.getString("releaseSha256"))||!BuildConfig.SOURCE_COMMIT_R26.equals(checked.getString("sourceCommit"))
                    ||checked.getLong("publishedJobRevision")>job.revision)throw new java.io.IOException("REPORT_RESOURCE_OBSERVATION_IDENTITY");
            result.put("state","LAST_REJECTION_OBSERVATION_NOT_CURRENT_ADMISSION");
            result.put("sha256",StorageBudgetR31.sha(original.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            result.put("original",com.ethk3.novaonepro.strategyv2.JsonR27.parse(original));
        }catch(Exception invalid){result.clear();result.put("state","RESOURCE_OBSERVATION_UNAVAILABLE");result.put("reason",invalid.getClass().getSimpleName());}
        return result;
    }
}
